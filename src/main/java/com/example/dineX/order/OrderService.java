package com.example.dineX.order;

import static com.example.dineX.order.FoodOrder.Status.ACCEPTED;
import static com.example.dineX.order.FoodOrder.Status.CANCELLED;
import static com.example.dineX.order.FoodOrder.Status.COOKING;
import static com.example.dineX.order.FoodOrder.Status.ORDERED;
import static com.example.dineX.order.FoodOrder.Status.READY;
import static com.example.dineX.order.FoodOrder.Status.SERVED;

import com.example.dineX.billing.Bill;
import com.example.dineX.billing.BillRepository;
import com.example.dineX.billing.Payment;
import com.example.dineX.billing.PaymentRepository;
import com.example.dineX.common.ApiException;
import com.example.dineX.config.AppProperties;
import com.example.dineX.feedback.FeedbackRepository;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.menu.MenuCategoryRepository;
import com.example.dineX.menu.MenuItem;
import com.example.dineX.menu.MenuItemRepository;
import com.example.dineX.notification.NotificationService;
import com.example.dineX.order.OrderDtos.CategoryBlock;
import com.example.dineX.order.OrderDtos.CustomerRequest;
import com.example.dineX.order.OrderDtos.LineRequest;
import com.example.dineX.order.OrderDtos.MenuItemBlock;
import com.example.dineX.order.OrderDtos.OrderItemView;
import com.example.dineX.order.OrderDtos.OrderView;
import com.example.dineX.order.OrderDtos.PlaceOrderRequest;
import com.example.dineX.order.OrderDtos.SessionStart;
import com.example.dineX.order.OrderDtos.SessionSummary;
import com.example.dineX.order.OrderDtos.SessionView;
import com.example.dineX.order.OrderDtos.TableMenuView;
import com.example.dineX.realtime.RealtimePublisher;
import com.example.dineX.subscription.SubscriptionService;
import com.example.dineX.table.DiningTable;
import com.example.dineX.table.DiningTableRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final Set<FoodOrder.Status> ACTIVE_STATES = EnumSet.of(ORDERED, ACCEPTED, COOKING, READY);
    private static final Map<FoodOrder.Status, Set<FoodOrder.Status>> TRANSITIONS = Map.of(
            ORDERED, EnumSet.of(ACCEPTED, CANCELLED),
            ACCEPTED, EnumSet.of(COOKING, CANCELLED),
            COOKING, EnumSet.of(READY),
            READY, EnumSet.of(SERVED),
            SERVED, EnumSet.noneOf(FoodOrder.Status.class),
            CANCELLED, EnumSet.noneOf(FoodOrder.Status.class));
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Totals(BigDecimal subtotal, BigDecimal sgst, BigDecimal cgst, BigDecimal tipAmount,
                         BigDecimal grandTotal) { }

    private final DiningTableRepository tables;
    private final MenuCategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final DiningSessionRepository sessions;
    private final FoodOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CustomerRepository customers;
    private final HotelRepository hotels;
    private final SubscriptionService subscriptions;
    private final BillRepository bills;
    private final PaymentRepository payments;
    private final FeedbackRepository feedback;
    private final NotificationService notifications;
    private final RealtimePublisher realtime;
    private final AppProperties properties;

    public OrderService(DiningTableRepository tables, MenuCategoryRepository categories, MenuItemRepository menuItems,
                        DiningSessionRepository sessions, FoodOrderRepository orders, OrderItemRepository orderItems,
                        CustomerRepository customers, HotelRepository hotels, SubscriptionService subscriptions,
                        BillRepository bills, PaymentRepository payments, FeedbackRepository feedback,
                        NotificationService notifications, RealtimePublisher realtime, AppProperties properties) {
        this.tables = tables;
        this.categories = categories;
        this.menuItems = menuItems;
        this.sessions = sessions;
        this.orders = orders;
        this.orderItems = orderItems;
        this.customers = customers;
        this.hotels = hotels;
        this.subscriptions = subscriptions;
        this.bills = bills;
        this.payments = payments;
        this.feedback = feedback;
        this.notifications = notifications;
        this.realtime = realtime;
        this.properties = properties;
    }

    // ----- Guest (public QR flow) -----

    @Transactional(readOnly = true)
    public TableMenuView menuForTable(String qrToken) {
        DiningTable table = tableByQr(qrToken);
        Hotel hotel = enabledHotel(table.getHotelId());
        Map<Long, List<MenuItem>> byCategory = menuItems
                .findByHotelIdAndVisibleTrueAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotel.getId())
                .stream()
                .collect(Collectors.groupingBy(MenuItem::getCategoryId, LinkedHashMap::new, Collectors.toList()));
        List<CategoryBlock> blocks = categories
                .findByHotelIdAndEnabledTrueAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(hotel.getId())
                .stream()
                .filter(c -> byCategory.containsKey(c.getId()))
                .map(c -> new CategoryBlock(c.getId(), c.getName(),
                        byCategory.get(c.getId()).stream().map(OrderService::menuBlock).toList()))
                .toList();
        return new TableMenuView(hotel.getHotelName(), hotel.getLogoUrl(),
                subscriptions.hasActiveSubscription(hotel.getId()), table.getTableNumber(), table.getTableName(),
                blocks);
    }

    @Transactional
    public SessionStart startSession(String qrToken) {
        DiningTable table = tableByQr(qrToken);
        enabledHotel(table.getHotelId());
        DiningSession session = sessions
                .findFirstByTableIdAndStatusNotOrderByIdDesc(table.getId(), DiningSession.Status.CLOSED)
                .orElseGet(() -> newSession(table));
        return new SessionStart(session.getSessionToken(), table.getTableNumber(), session.getStatus().name());
    }

    @Transactional(readOnly = true)
    public SessionView sessionViewByToken(String token) {
        return sessionView(requireSession(token));
    }

    @Transactional
    public SessionView identifyCustomer(String token, CustomerRequest request) {
        DiningSession session = requireSession(token);
        if (session.getStatus() != DiningSession.Status.OPEN) {
            throw ApiException.conflict("Customer details can only be updated while the session is open");
        }
        String mobile = request.mobileNumber().replaceAll("[\\s-]", "");
        Customer customer = customers.findByHotelIdAndMobileNumber(session.getHotelId(), mobile)
                .orElseGet(Customer::new);
        customer.setHotelId(session.getHotelId());
        customer.setMobileNumber(mobile);
        customer.setCustomerName(request.customerName().trim());
        customers.save(customer);
        session.setCustomerId(customer.getId());
        session.setCustomerName(customer.getCustomerName());
        session.setCustomerMobile(mobile);
        sessions.save(session);
        SessionView view = sessionView(session);
        realtime.publish(session.getHotelId(), session.getSessionToken(), "SESSION_UPDATED", view);
        return view;
    }

    @Transactional
    public OrderView placeOrder(String token, PlaceOrderRequest request) {
        DiningSession session = requireSession(token);
        if (session.getStatus() != DiningSession.Status.OPEN) {
            throw ApiException.conflict("This session is no longer accepting orders");
        }
        if (session.getCustomerId() == null) {
            throw ApiException.badRequest("Enter your name, mobile number and table number before ordering");
        }
        Hotel hotel = enabledHotel(session.getHotelId());
        if (!subscriptions.hasActiveSubscription(hotel.getId())) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "SUBSCRIPTION_REQUIRED",
                    "This restaurant cannot accept orders until its subscription is renewed");
        }
        Set<Long> ids = request.items().stream().map(LineRequest::menuItemId).collect(Collectors.toSet());
        Map<Long, MenuItem> menu = menuItems.findByHotelIdAndIdInAndDeletedFlagFalse(hotel.getId(), ids)
                .stream()
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        List<OrderItem> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineRequest line : request.items()) {
            MenuItem item = menu.get(line.menuItemId());
            if (item == null) {
                throw ApiException.badRequest("One or more menu items are no longer on the menu");
            }
            if (!item.isAvailable() || !item.isVisible()) {
                throw ApiException.conflict(item.getName() + " is currently unavailable");
            }
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(line.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            OrderItem orderItem = new OrderItem();
            orderItem.setHotelId(hotel.getId());
            orderItem.setMenuItemId(item.getId());
            orderItem.setItemName(item.getName());
            orderItem.setQuantity(line.quantity());
            orderItem.setUnitPrice(item.getPrice());
            orderItem.setTotalPrice(lineTotal);
            orderItem.setSpecialInstructions(blankToNull(line.specialInstructions()));
            lines.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sgst = percent(subtotal, properties.tax().sgstPercent());
        BigDecimal cgst = percent(subtotal, properties.tax().cgstPercent());

        FoodOrder order = new FoodOrder();
        order.setHotelId(hotel.getId());
        order.setSessionId(session.getId());
        order.setOrderNumber(newCode("ORD"));
        order.setNotes(blankToNull(request.notes()));
        order.setStatus(ORDERED);
        order.setSubtotal(subtotal);
        order.setSgst(sgst);
        order.setCgst(cgst);
        order.setGrandTotal(subtotal.add(sgst).add(cgst));
        orders.save(order);

        lines.forEach(line -> line.setOrderId(order.getId()));
        orderItems.saveAll(lines);

        OrderView view = toViews(List.of(order)).get(0);
        notifications.create(hotel.getId(), "New order " + order.getOrderNumber(),
                tableNumber(session) + " placed " + lines.size() + " item line(s)");
        realtime.publish(hotel.getId(), session.getSessionToken(), "ORDER_PLACED", view);
        return view;
    }

    @Transactional
    public void requestAssistance(String token) {
        DiningSession session = requireSession(token);
        if (session.getStatus() == DiningSession.Status.CLOSED) {
            throw ApiException.conflict("This session has been closed");
        }
        notifications.create(session.getHotelId(), "Assistance requested", tableNumber(session) + " needs assistance");
    }

    // ----- Hotel and kitchen -----

    @Transactional(readOnly = true)
    public List<OrderView> activeOrders(Long hotelId, Collection<FoodOrder.Status> statuses) {
        Collection<FoodOrder.Status> filter = statuses == null || statuses.isEmpty() ? ACTIVE_STATES : statuses;
        return toViews(orders.findByHotelIdAndStatusInOrderByIdAsc(hotelId, filter));
    }

    @Transactional
    public OrderView changeStatus(Long hotelId, Long orderId, FoodOrder.Status target, boolean kitchen) {
        FoodOrder order = orders.findByIdAndHotelId(orderId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Order"));
        if (kitchen && target == CANCELLED) {
            throw ApiException.forbidden("Kitchen staff cannot cancel orders");
        }
        if (!TRANSITIONS.get(order.getStatus()).contains(target)) {
            throw ApiException.conflict("Order cannot move from " + order.getStatus() + " to " + target);
        }
        order.setStatus(target);
        DiningSession session = sessions.findById(order.getSessionId())
                .orElseThrow(() -> ApiException.notFound("Session"));
        OrderView view = toViews(List.of(order)).get(0);
        realtime.publish(hotelId, session.getSessionToken(), "ORDER_STATUS", view);
        return view;
    }

    @Transactional(readOnly = true)
    public List<SessionSummary> activeSessions(Long hotelId) {
        return sessions.findByHotelIdAndStatusNotOrderByIdDesc(hotelId, DiningSession.Status.CLOSED).stream()
                .map(s -> {
                    List<FoodOrder> rows = orders.findBySessionIdOrderByIdAsc(s.getId());
                    Totals totals = totals(rows);
                    int count = (int) rows.stream().filter(o -> o.getStatus() != CANCELLED).count();
                    return new SessionSummary(s.getId(), s.getSessionToken(), s.getSessionNumber(), s.getStatus().name(),
                            tableNumber(s), s.getCustomerName(), count, totals.grandTotal(), s.getOpenedAt());
                })
                .toList();
    }

    // ----- Shared helpers (used by billing and other modules) -----

    @Transactional(readOnly = true)
    public DiningSession requireSession(String token) {
        return sessions.findBySessionToken(token).orElseThrow(() -> ApiException.notFound("Session"));
    }

    @Transactional(readOnly = true)
    public SessionView sessionView(DiningSession session) {
        Hotel hotel = hotels.findById(session.getHotelId()).orElseThrow(() -> ApiException.notFound("Restaurant"));
        List<FoodOrder> rows = orders.findBySessionIdOrderByIdAsc(session.getId());
        Optional<Bill> bill = bills.findBySessionId(session.getId());
        Totals totals = bill
                .map(b -> new Totals(b.getSubtotal(), b.getSgst(), b.getCgst(), b.getTipAmount(), b.getGrandTotal()))
                .orElseGet(() -> totals(rows));
        Optional<Payment> payment = payments.findFirstBySessionIdOrderByIdDesc(session.getId());
        return new SessionView(session.getSessionToken(), session.getSessionNumber(), session.getStatus().name(),
                tableNumber(session), hotel.getHotelName(), hotel.getLogoUrl(), hotel.getUpiId(),
                hotel.getQrImageUrl(), session.getCustomerName(), session.getCustomerMobile(),
                toViews(rows), totals.subtotal(), totals.sgst(), totals.cgst(), totals.tipAmount(),
                totals.grandTotal(), bill.map(Bill::getId).orElse(null),
                bill.map(b -> b.getStatus().name()).orElse(null),
                payment.map(p -> p.getPaymentMethod().name()).orElse(null),
                payment.map(p -> p.getStatus().name()).orElse(null),
                feedback.existsBySessionId(session.getId()));
    }

    /** Pushes the current session view to guests and hotel staff. */
    public void broadcastSession(DiningSession session) {
        realtime.publish(session.getHotelId(), session.getSessionToken(), "SESSION_UPDATED", sessionView(session));
    }

    /** Totals over the non-cancelled orders of a session. */
    public Totals totals(List<FoodOrder> rows) {
        List<FoodOrder> live = rows.stream().filter(o -> o.getStatus() != CANCELLED).toList();
        BigDecimal subtotal = sum(live, FoodOrder::getSubtotal);
        BigDecimal sgst = sum(live, FoodOrder::getSgst);
        BigDecimal cgst = sum(live, FoodOrder::getCgst);
        return new Totals(subtotal, sgst, cgst, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                subtotal.add(sgst).add(cgst));
    }

    public List<OrderView> toViews(List<FoodOrder> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = rows.stream().map(FoodOrder::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = orderItems.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        Set<Long> sessionIds = rows.stream().map(FoodOrder::getSessionId).collect(Collectors.toSet());
        Map<Long, DiningSession> sessionMap = sessions.findAllById(sessionIds).stream()
                .collect(Collectors.toMap(DiningSession::getId, Function.identity()));
        Set<Long> tableIds = sessionMap.values().stream().map(DiningSession::getTableId).collect(Collectors.toSet());
        Map<Long, String> tableNumbers = tables.findAllById(tableIds).stream()
                .collect(Collectors.toMap(DiningTable::getId, DiningTable::getTableNumber));
        return rows.stream().map(o -> {
            DiningSession session = sessionMap.get(o.getSessionId());
            List<OrderItemView> items = itemsByOrder.getOrDefault(o.getId(), List.of()).stream()
                    .map(i -> new OrderItemView(i.getMenuItemId(), i.getItemName(), i.getQuantity(),
                            i.getUnitPrice(), i.getTotalPrice(), i.getSpecialInstructions()))
                    .toList();
            return new OrderView(o.getId(), o.getOrderNumber(), tableNumbers.get(session.getTableId()),
                    session.getSessionToken(), o.getStatus().name(), o.getNotes(), o.getCreatedDate(),
                    o.getSubtotal(), o.getSgst(), o.getCgst(), o.getGrandTotal(), items);
        }).toList();
    }

    public String tableNumber(DiningSession session) {
        return tables.findById(session.getTableId()).map(DiningTable::getTableNumber).orElse(null);
    }

    public Hotel enabledHotel(Long hotelId) {
        Hotel hotel = hotels.findByIdAndDeletedFlagFalse(hotelId)
                .orElseThrow(() -> ApiException.notFound("Restaurant"));
        if (!hotel.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "HOTEL_DISABLED",
                    "This restaurant is currently not accepting guests");
        }
        return hotel;
    }

    public static BigDecimal percent(BigDecimal amount, BigDecimal pct) {
        return amount.multiply(pct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static String newCode(String prefix) {
        byte[] bytes = new byte[5];
        RANDOM.nextBytes(bytes);
        return prefix + "-" + HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private DiningTable tableByQr(String qrToken) {
        return tables.findByQrTokenAndEnabledTrueAndDeletedFlagFalse(qrToken)
                .orElseThrow(() -> ApiException.notFound("Table"));
    }

    private DiningSession newSession(DiningTable table) {
        DiningSession session = new DiningSession();
        session.setHotelId(table.getHotelId());
        session.setTableId(table.getId());
        session.setSessionNumber(newCode("SES"));
        session.setSessionToken(newToken());
        session.setStatus(DiningSession.Status.OPEN);
        return sessions.save(session);
    }

    private static BigDecimal sum(List<FoodOrder> rows, Function<FoodOrder, BigDecimal> field) {
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static MenuItemBlock menuBlock(MenuItem item) {
        return new MenuItemBlock(item.getId(), item.getName(), item.getDescription(), item.getPrice(),
                item.getImageUrl(), item.getPreparationMinutes(), item.getFoodType().name(), item.isAvailable());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
