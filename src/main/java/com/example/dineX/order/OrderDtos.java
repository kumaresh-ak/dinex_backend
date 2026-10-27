package com.example.dineX.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() { }

    public record LineRequest(
            @NotNull Long menuItemId,
            @Min(1) @Max(50) int quantity,
            @Size(max = 500) String specialInstructions) { }

    public record PlaceOrderRequest(
            @NotEmpty @Valid List<LineRequest> items,
            @Size(max = 500) String notes) { }

    public record CustomerRequest(
            @NotBlank @Size(max = 160) String customerName,
            @NotBlank @Pattern(regexp = "^[0-9+ \\-]{7,30}$") String mobileNumber) { }

    public record StatusRequest(@NotNull FoodOrder.Status status) { }

    public record TipRequest(@NotNull @DecimalMin("0.00") BigDecimal tipAmount) { }

    public record OrderItemView(Long menuItemId, String itemName, int quantity, BigDecimal unitPrice,
                                BigDecimal totalPrice, String specialInstructions) { }

    public record OrderView(Long id, String orderNumber, String tableNumber, String sessionToken, String status,
                            String notes, LocalDateTime createdAt, BigDecimal subtotal, BigDecimal sgst,
                            BigDecimal cgst, BigDecimal grandTotal, List<OrderItemView> items) { }

    public record SessionSummary(Long sessionId, String sessionToken, String sessionNumber, String status, String tableNumber,
                                 String customerName, int orderCount, BigDecimal grandTotal,
                                 LocalDateTime openedAt) { }

    public record SessionStart(String sessionToken, String tableNumber, String status) { }

    public record MenuItemBlock(Long id, String name, String description, BigDecimal price, String imageUrl,
                                int preparationMinutes, String foodType, boolean available) { }

    public record CategoryBlock(Long id, String name, List<MenuItemBlock> items) { }

    public record TableMenuView(String hotelName, String logoUrl, boolean ordersAccepting, String tableNumber,
                                String tableName, List<CategoryBlock> categories) { }

    public record SessionView(String sessionToken, String sessionNumber, String status, String tableNumber,
                              String hotelName, String logoUrl, String upiId, String qrImageUrl,
                              String customerName, String customerMobile, List<OrderView> orders,
                              BigDecimal subtotal, BigDecimal sgst, BigDecimal cgst, BigDecimal tipAmount,
                              BigDecimal grandTotal, Long billId, String billStatus, String paymentMethod,
                              String paymentStatus, boolean feedbackSubmitted) { }
}
