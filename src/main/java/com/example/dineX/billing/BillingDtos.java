package com.example.dineX.billing;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class BillingDtos {
    private BillingDtos() { }

    public record BillView(Long id, String billNumber, Long sessionId, String sessionNumber, String tableNumber,
                           String status, BigDecimal subtotal, BigDecimal sgst, BigDecimal cgst,
                           BigDecimal tipAmount, BigDecimal grandTotal, LocalDateTime generatedAt,
                           LocalDateTime paidAt) { }

    public record PaymentView(Long id, Long billId, String billNumber, String paymentMethod, BigDecimal amount,
                              String status, LocalDateTime paidAt, LocalDateTime createdAt) { }

    public record SettleRequest(@NotNull Payment.Method paymentMethod) { }

    public record PaymentRequest(@NotNull Payment.Method paymentMethod) { }
}
