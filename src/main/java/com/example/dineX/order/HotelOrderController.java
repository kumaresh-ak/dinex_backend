package com.example.dineX.order;

import com.example.dineX.security.AuthUser;
import com.example.dineX.order.OrderDtos.OrderView;
import com.example.dineX.order.OrderDtos.SessionSummary;
import com.example.dineX.order.OrderDtos.StatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel")
public class HotelOrderController {
    private final OrderService orderService;

    public HotelOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public List<OrderView> orders(@AuthenticationPrincipal AuthUser user,
                                  @RequestParam(required = false) List<FoodOrder.Status> status) {
        return orderService.activeOrders(user.hotelId(), status);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderView changeStatus(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                                  @Valid @RequestBody StatusRequest request) {
        return orderService.changeStatus(user.hotelId(), id, request.status());
    }

    @GetMapping("/sessions")
    public List<SessionSummary> sessions(@AuthenticationPrincipal AuthUser user) {
        return orderService.activeSessions(user.hotelId());
    }
}
