package com.example.dineX.menu;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.config.AppProperties;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.menu.MenuQrDtos.MenuQrSession;
import com.example.dineX.menu.MenuQrDtos.MenuQrView;
import com.example.dineX.menu.MenuQrDtos.PublicMenuView;
import com.example.dineX.order.OrderDtos.SessionStart;
import com.example.dineX.order.OrderService;
import com.example.dineX.table.DiningTable;
import com.example.dineX.table.DiningTableRepository;
import com.example.dineX.table.TableService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One common QR code per hotel: guests scan it, pick their table and view the menu. */
@Service
public class MenuQrService {
    private final HotelRepository hotels;
    private final MenuItemRepository menuItems;
    private final DiningTableRepository tables;
    private final OrderService orders;
    private final AuditService audit;
    private final AppProperties properties;

    public MenuQrService(HotelRepository hotels, MenuItemRepository menuItems, DiningTableRepository tables,
                         OrderService orders, AuditService audit, AppProperties properties) {
        this.hotels = hotels;
        this.menuItems = menuItems;
        this.tables = tables;
        this.orders = orders;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public MenuQrView current(Long hotelId) {
        return view(hotel(hotelId));
    }

    @Transactional
    public MenuQrView generate(Long hotelId) {
        Hotel hotel = hotel(hotelId);
        if (!menuItems.existsByHotelIdAndDeletedFlagFalse(hotelId)) {
            throw ApiException.conflict("Add menu items before generating the menu QR code");
        }
        hotel.setMenuQrToken(UUID.randomUUID().toString().replace("-", ""));
        hotels.save(hotel);
        audit.record(hotelId, "MENU_QR_GENERATED", "Hotel", hotelId, hotel.getHotelName());
        return view(hotel);
    }

    @Transactional(readOnly = true)
    public byte[] png(Long hotelId) {
        Hotel hotel = hotel(hotelId);
        if (hotel.getMenuQrToken() == null) {
            throw ApiException.notFound("Menu QR code");
        }
        return TableService.render(menuUrl(hotel.getMenuQrToken()));
    }

    @Transactional(readOnly = true)
    public PublicMenuView tablesFor(String token) {
        Hotel hotel = publicHotel(token);
        List<String> numbers = enabledTables(hotel.getId()).stream()
                .map(DiningTable::getTableNumber)
                .toList();
        return new PublicMenuView(hotel.getHotelName(), hotel.getLogoUrl(), numbers);
    }

    @Transactional
    public MenuQrSession startSession(String token, String tableNumber) {
        Hotel hotel = publicHotel(token);
        String wanted = tableNumber.trim();
        DiningTable table = enabledTables(hotel.getId()).stream()
                .filter(t -> t.getTableNumber().equals(wanted))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Table"));
        SessionStart start = orders.startSession(table.getQrToken());
        return new MenuQrSession(start.sessionToken(), start.tableNumber(), start.status(), table.getQrToken());
    }

    private List<DiningTable> enabledTables(Long hotelId) {
        return tables.findByHotelIdAndDeletedFlagFalseOrderByTableNumberAsc(hotelId).stream()
                .filter(t -> t.isEnabled() && t.getQrToken() != null)
                .toList();
    }

    private Hotel hotel(Long hotelId) {
        return hotels.findByIdAndDeletedFlagFalse(hotelId)
                .orElseThrow(() -> ApiException.notFound("Hotel"));
    }

    private Hotel publicHotel(String token) {
        return hotels.findByMenuQrTokenAndDeletedFlagFalse(token)
                .filter(Hotel::isEnabled)
                .orElseThrow(() -> ApiException.notFound("Menu"));
    }

    private MenuQrView view(Hotel hotel) {
        if (hotel.getMenuQrToken() == null) {
            return new MenuQrView(false, null);
        }
        return new MenuQrView(true, menuUrl(hotel.getMenuQrToken()));
    }

    private String menuUrl(String token) {
        return properties.publicBaseUrl().replaceAll("/+$", "") + "/menu/" + token;
    }
}
