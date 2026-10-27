package com.example.dineX.billing;

import com.example.dineX.billing.BillingDtos.BillView;
import com.example.dineX.billing.BillingDtos.PaymentView;
import com.example.dineX.billing.BillingDtos.SettleRequest;
import com.example.dineX.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/billing")
public class BillingController {
    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/sessions/{sessionId}/bill")
    public BillView generate(@AuthenticationPrincipal AuthUser user, @PathVariable Long sessionId) {
        return billingService.generateForHotel(user.hotelId(), sessionId);
    }

    @GetMapping("/bills")
    public List<BillView> bills(@AuthenticationPrincipal AuthUser user,
                                @RequestParam(required = false) Bill.Status status) {
        return billingService.list(user.hotelId(), status);
    }

    @PostMapping("/bills/{id}/settle")
    public BillView settle(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                           @Valid @RequestBody SettleRequest request) {
        return billingService.settle(user.hotelId(), id, request.paymentMethod(), user.id());
    }

    @GetMapping(value = "/bills/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"bill-" + id + ".pdf\"")
                .body(billingService.pdfForHotel(user.hotelId(), id));
    }

    @GetMapping("/payments")
    public List<PaymentView> payments(@AuthenticationPrincipal AuthUser user) {
        return billingService.payments(user.hotelId());
    }
}
