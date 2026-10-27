package com.example.dineX.order;

import com.example.dineX.billing.BillingDtos.BillView;
import com.example.dineX.billing.BillingDtos.PaymentRequest;
import com.example.dineX.billing.BillingService;
import com.example.dineX.feedback.FeedbackDtos.FeedbackRequest;
import com.example.dineX.feedback.FeedbackService;
import com.example.dineX.order.OrderDtos.CustomerRequest;
import com.example.dineX.order.OrderDtos.OrderView;
import com.example.dineX.order.OrderDtos.PlaceOrderRequest;
import com.example.dineX.order.OrderDtos.SessionStart;
import com.example.dineX.order.OrderDtos.SessionView;
import com.example.dineX.order.OrderDtos.TableMenuView;
import com.example.dineX.order.OrderDtos.TipRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Guest-facing endpoints reached through the table QR code. No authentication required. */
@RestController
@RequestMapping("/api/v1/public")
public class PublicOrderController {
    private final OrderService orderService;
    private final BillingService billingService;
    private final FeedbackService feedbackService;

    public PublicOrderController(OrderService orderService, BillingService billingService,
                                 FeedbackService feedbackService) {
        this.orderService = orderService;
        this.billingService = billingService;
        this.feedbackService = feedbackService;
    }

    @GetMapping("/tables/{qrToken}/menu")
    public TableMenuView menu(@PathVariable String qrToken) {
        return orderService.menuForTable(qrToken);
    }

    @PostMapping("/tables/{qrToken}/session")
    public SessionStart startSession(@PathVariable String qrToken) {
        return orderService.startSession(qrToken);
    }

    @GetMapping("/sessions/{sessionToken}")
    public SessionView session(@PathVariable String sessionToken) {
        return orderService.sessionViewByToken(sessionToken);
    }

    @PutMapping("/sessions/{sessionToken}/customer")
    public SessionView identify(@PathVariable String sessionToken, @Valid @RequestBody CustomerRequest request) {
        return orderService.identifyCustomer(sessionToken, request);
    }

    @PostMapping("/sessions/{sessionToken}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView placeOrder(@PathVariable String sessionToken, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(sessionToken, request);
    }

    @PostMapping("/sessions/{sessionToken}/assistance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assistance(@PathVariable String sessionToken) {
        orderService.requestAssistance(sessionToken);
    }

    @PostMapping("/sessions/{sessionToken}/bill")
    public BillView requestBill(@PathVariable String sessionToken) {
        return billingService.generateForGuest(sessionToken);
    }

    @PutMapping("/sessions/{sessionToken}/tip")
    public BillView tip(@PathVariable String sessionToken, @Valid @RequestBody TipRequest request) {
        return billingService.setTip(sessionToken, request.tipAmount());
    }

    @PostMapping("/sessions/{sessionToken}/payment")
    public BillView payment(@PathVariable String sessionToken, @Valid @RequestBody PaymentRequest request) {
        return billingService.initiatePayment(sessionToken, request.paymentMethod());
    }

    @GetMapping(value = "/sessions/{sessionToken}/bill/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> billPdf(@PathVariable String sessionToken) {
        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"bill.pdf\"")
                .body(billingService.pdfForGuest(sessionToken));
    }

    @PostMapping("/sessions/{sessionToken}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public void feedback(@PathVariable String sessionToken, @Valid @RequestBody FeedbackRequest request) {
        feedbackService.submit(sessionToken, request);
    }
}
