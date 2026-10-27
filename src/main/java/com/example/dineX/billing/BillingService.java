package com.example.dineX.billing;

import com.example.dineX.audit.AuditService;
import com.example.dineX.billing.BillingDtos.BillView;
import com.example.dineX.billing.BillingDtos.PaymentView;
import com.example.dineX.common.ApiException;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.notification.NotificationService;
import com.example.dineX.order.DiningSession;
import com.example.dineX.order.DiningSessionRepository;
import com.example.dineX.order.FoodOrder;
import com.example.dineX.order.FoodOrderRepository;
import com.example.dineX.order.OrderDtos.OrderView;
import com.example.dineX.order.OrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private static final BigDecimal MAX_TIP = new BigDecimal("100000");
    private static final Set<FoodOrder.Status> IN_PROGRESS = EnumSet.of(
            FoodOrder.Status.ORDERED, FoodOrder.Status.ACCEPTED, FoodOrder.Status.COOKING, FoodOrder.Status.READY);

    private final BillRepository bills;
    private final PaymentRepository payments;
    private final DiningSessionRepository sessions;
    private final FoodOrderRepository orders;
    private final HotelRepository hotels;
    private final OrderService orderService;
    private final NotificationService notifications;
    private final BillPdfService pdfService;
    private final AuditService audit;

    public BillingService(BillRepository bills, PaymentRepository payments, DiningSessionRepository sessions,
                          FoodOrderRepository orders, HotelRepository hotels, OrderService orderService,
                          NotificationService notifications, BillPdfService pdfService, AuditService audit) {
        this.bills = bills;
        this.payments = payments;
        this.sessions = sessions;
        this.orders = orders;
        this.hotels = hotels;
        this.orderService = orderService;
        this.notifications = notifications;
        this.pdfService = pdfService;
        this.audit = audit;
    }

    @Transactional
    public BillView generateForHotel(Long hotelId, Long sessionId) {
        DiningSession session = sessions.findByIdAndHotelId(sessionId, hotelId)
                .orElseThrow(() -> ApiException.notFound("Session"));
        return generate(session);
    }

    @Transactional
    public BillView generateForGuest(String token) {
        return generate(orderService.requireSession(token));
    }

    @Transactional
    public BillView setTip(String token, BigDecimal tip) {
        DiningSession session = orderService.requireSession(token);
        Bill bill = bills.findBySessionId(session.getId())
                .orElseThrow(() -> ApiException.conflict("Request the bill before adding a tip"));
        if (bill.getStatus() != Bill.Status.UNPAID) {
            throw ApiException.conflict("The tip can no longer be changed");
        }
        if (tip.compareTo(MAX_TIP) > 0) {
            throw ApiException.badRequest("Tip exceeds the allowed limit");
        }
        BigDecimal amount = tip.setScale(2, RoundingMode.HALF_UP);
        bill.setTipAmount(amount);
        bill.setGrandTotal(bill.getSubtotal().add(bill.getSgst()).add(bill.getCgst()).add(amount));
        bills.save(bill);
        orderService.broadcastSession(session);
        return view(bill, session);
    }

    @Transactional
    public BillView initiatePayment(String token, Payment.Method method) {
        DiningSession session = orderService.requireSession(token);
        Bill bill = bills.findBySessionId(session.getId())
                .orElseThrow(() -> ApiException.conflict("The bill has not been generated"));
        if (bill.getStatus() == Bill.Status.PAID) {
            throw ApiException.conflict("This bill is already paid");
        }
        Payment payment = payments.findFirstByBillIdAndStatusOrderByIdDesc(bill.getId(), Payment.Status.PENDING)
                .orElseGet(Payment::new);
        payment.setHotelId(bill.getHotelId());
        payment.setSessionId(session.getId());
        payment.setBillId(bill.getId());
        payment.setAmount(bill.getGrandTotal());
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.Status.PENDING);
        payments.save(payment);

        bill.setStatus(Bill.Status.PAYMENT_PENDING);
        bills.save(bill);
        session.setStatus(DiningSession.Status.PAYMENT_PENDING);
        sessions.save(session);

        notifications.create(bill.getHotelId(), "Payment pending " + bill.getBillNumber(),
                orderService.tableNumber(session) + " will pay by " + method.name());
        orderService.broadcastSession(session);
        return view(bill, session);
    }

    @Transactional
    public BillView settle(Long hotelId, Long billId, Payment.Method method, Long staffId) {
        Bill bill = bills.findByIdAndHotelId(billId, hotelId).orElseThrow(() -> ApiException.notFound("Bill"));
        if (bill.getStatus() == Bill.Status.PAID) {
            throw ApiException.conflict("This bill is already settled");
        }
        DiningSession session = sessions.findById(bill.getSessionId())
                .orElseThrow(() -> ApiException.notFound("Session"));
        LocalDateTime now = LocalDateTime.now();

        Payment payment = payments.findFirstByBillIdAndStatusOrderByIdDesc(bill.getId(), Payment.Status.PENDING)
                .orElseGet(Payment::new);
        payment.setHotelId(hotelId);
        payment.setSessionId(session.getId());
        payment.setBillId(bill.getId());
        payment.setAmount(bill.getGrandTotal());
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.Status.PAID);
        payment.setPaidAt(now);
        payment.setVerifiedBy(staffId);
        payments.save(payment);

        bill.setStatus(Bill.Status.PAID);
        bill.setPaidAt(now);
        bills.save(bill);
        session.setStatus(DiningSession.Status.CLOSED);
        session.setClosedAt(now);
        sessions.save(session);

        audit.record(hotelId, "BILL_SETTLED", "Bill", bill.getId(),
                method.name() + " " + bill.getGrandTotal().toPlainString());
        notifications.create(hotelId, "Bill settled " + bill.getBillNumber(),
                orderService.tableNumber(session) + " paid " + bill.getGrandTotal().toPlainString()
                        + " by " + method.name());
        orderService.broadcastSession(session);
        return view(bill, session);
    }

    @Transactional(readOnly = true)
    public List<BillView> list(Long hotelId, Bill.Status status) {
        List<Bill> rows = status == null
                ? bills.findByHotelIdOrderByIdDesc(hotelId)
                : bills.findByHotelIdAndStatusInOrderByIdDesc(hotelId, List.of(status));
        return rows.stream().map(b -> view(b, sessionOf(b))).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments(Long hotelId) {
        List<Payment> rows = payments.findByHotelIdOrderByIdDesc(hotelId);
        Map<Long, String> billNumbers = bills.findAllById(
                rows.stream().map(Payment::getBillId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Bill::getId, Bill::getBillNumber));
        return rows.stream().map(p -> new PaymentView(p.getId(), p.getBillId(),
                billNumbers.get(p.getBillId()), p.getPaymentMethod().name(), p.getAmount(), p.getStatus().name(),
                p.getPaidAt(), p.getCreatedDate())).toList();
    }

    @Transactional(readOnly = true)
    public byte[] pdfForHotel(Long hotelId, Long billId) {
        Bill bill = bills.findByIdAndHotelId(billId, hotelId).orElseThrow(() -> ApiException.notFound("Bill"));
        return render(bill);
    }

    @Transactional(readOnly = true)
    public byte[] pdfForGuest(String token) {
        DiningSession session = orderService.requireSession(token);
        Bill bill = bills.findBySessionId(session.getId())
                .orElseThrow(() -> ApiException.notFound("Bill"));
        return render(bill);
    }

    private BillView view(Bill bill, DiningSession session) {
        return new BillView(bill.getId(), bill.getBillNumber(), session.getId(), session.getSessionNumber(),
                orderService.tableNumber(session), bill.getStatus().name(), bill.getSubtotal(), bill.getSgst(),
                bill.getCgst(), bill.getTipAmount(), bill.getGrandTotal(), bill.getGeneratedAt(), bill.getPaidAt());
    }

    private DiningSession sessionOf(Bill bill) {
        return sessions.findById(bill.getSessionId()).orElseThrow(() -> ApiException.notFound("Session"));
    }

    private BillView generate(DiningSession session) {
        if (session.getStatus() != DiningSession.Status.OPEN) {
            throw ApiException.conflict("A bill has already been generated for this session");
        }
        if (orders.existsBySessionIdAndStatusIn(session.getId(), IN_PROGRESS)) {
            throw ApiException.conflict("Orders are still in progress. Serve or cancel them before billing");
        }
        List<FoodOrder> rows = orders.findBySessionIdOrderByIdAsc(session.getId());
        if (rows.stream().noneMatch(o -> o.getStatus() == FoodOrder.Status.SERVED)) {
            throw ApiException.conflict("There are no served orders to bill");
        }
        OrderService.Totals totals = orderService.totals(rows);

        Bill bill = new Bill();
        bill.setHotelId(session.getHotelId());
        bill.setSessionId(session.getId());
        bill.setBillNumber(OrderService.newCode("BILL"));
        bill.setSubtotal(totals.subtotal());
        bill.setSgst(totals.sgst());
        bill.setCgst(totals.cgst());
        bill.setTipAmount(BigDecimal.ZERO);
        bill.setGrandTotal(totals.grandTotal());
        bill.setStatus(Bill.Status.UNPAID);
        bills.save(bill);

        session.setStatus(DiningSession.Status.BILL_GENERATED);
        sessions.save(session);
        audit.record(session.getHotelId(), "BILL_GENERATED", "Bill", bill.getId(), bill.getBillNumber());
        orderService.broadcastSession(session);
        return view(bill, session);
    }

    private byte[] render(Bill bill) {
        DiningSession session = sessionOf(bill);
        Hotel hotel = hotels.findById(bill.getHotelId()).orElseThrow(() -> ApiException.notFound("Restaurant"));
        List<OrderView> served = orderService.toViews(orders.findBySessionIdOrderByIdAsc(session.getId())).stream()
                .filter(o -> !"CANCELLED".equals(o.status()))
                .toList();
        return pdfService.render(hotel, bill, session, orderService.tableNumber(session), served);
    }
}
