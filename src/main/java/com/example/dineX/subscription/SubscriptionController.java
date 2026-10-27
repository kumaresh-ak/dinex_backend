package com.example.dineX.subscription;

import com.example.dineX.security.AuthUser;
import com.example.dineX.storage.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

public class SubscriptionController {
    private SubscriptionController() { }

    public record RenewalRequest(@NotNull Long planId, @NotBlank String paymentReference,
                                 @NotNull LocalDate paymentDate) { }

    public record RejectRequest(@NotBlank String reason) { }

    @RestController
    @RequestMapping("/api/v1/public/subscription-plans")
    public static class PublicPlanController {
        private final SubscriptionService service;

        public PublicPlanController(SubscriptionService service) {
            this.service = service;
        }

        @GetMapping
        public List<SubscriptionService.PlanView> plans() {
            return service.listPlans();
        }
    }

    @RestController
    @RequestMapping("/api/v1/hotel/subscription")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('HOTEL_ADMIN')")
    public static class HotelSubscriptionController {
        private final SubscriptionService service;
        private final FileStorageService storage;

        public HotelSubscriptionController(SubscriptionService service, FileStorageService storage) {
            this.service = service;
            this.storage = storage;
        }

        @GetMapping
        public SubscriptionService.CurrentSubscription current(@AuthenticationPrincipal AuthUser user) {
            return service.current(user.hotelId());
        }

        @PostMapping(path = "/requests", consumes = "multipart/form-data")
        public ResponseEntity<SubscriptionService.SubscriptionView> request(
                @AuthenticationPrincipal AuthUser user,
                @Valid @RequestPart("data") RenewalRequest data,
                @RequestPart(value = "proof", required = false) MultipartFile proof) {
            String key = proof == null || proof.isEmpty() ? null : storage.store(proof, "proofs");
            return ResponseEntity.status(HttpStatus.CREATED).body(service.requestRenewal(
                    user.hotelId(), data.planId(), data.paymentReference(), data.paymentDate(), key));
        }
    }

    @RestController
    @RequestMapping("/api/v1/admin/subscriptions")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN')")
    public static class AdminSubscriptionController {
        private final SubscriptionService service;

        public AdminSubscriptionController(SubscriptionService service) {
            this.service = service;
        }

        @GetMapping
        public List<SubscriptionService.SubscriptionView> list(
                @RequestParam(required = false) HotelSubscription.Status status) {
            return service.list(status);
        }

        @PostMapping("/{id}/approve")
        public SubscriptionService.SubscriptionView approve(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
            return service.approve(id, user.id());
        }

        @PostMapping("/{id}/reject")
        public SubscriptionService.SubscriptionView reject(@PathVariable Long id, @Valid @RequestBody RejectRequest body) {
            return service.reject(id, body.reason());
        }
    }
}
