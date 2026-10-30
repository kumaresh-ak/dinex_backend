package com.example.dineX.subscription;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.hotel.Hotel;
import com.example.dineX.hotel.HotelRepository;
import com.example.dineX.security.TenantStatusCache;
import com.example.dineX.settings.PlatformSettingsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {
    private final SubscriptionPlanRepository plans;
    private final HotelSubscriptionRepository subscriptions;
    private final HotelRepository hotels;
    private final AuditService audit;
    private final PlatformSettingsService settings;
    private final TenantStatusCache tenantStatus;

    public SubscriptionService(SubscriptionPlanRepository plans, HotelSubscriptionRepository subscriptions,
                               HotelRepository hotels, AuditService audit, PlatformSettingsService settings,
                               TenantStatusCache tenantStatus) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.hotels = hotels;
        this.audit = audit;
        this.settings = settings;
        this.tenantStatus = tenantStatus;
    }

    public record PlanView(Long id, String planCode, String planName, BigDecimal price, int durationMonths) { }

    public record SubscriptionView(Long id, Long hotelId, String hotelName, String planName, BigDecimal amount,
                                   String status, LocalDate paymentDate, String paymentReference,
                                   String paymentProofKey, LocalDate startDate, LocalDate endDate) { }

    public record CurrentSubscription(String hotelStatus, boolean active, long remainingDays,
                                      SubscriptionView activeSubscription, SubscriptionView pendingRequest,
                                      List<SubscriptionView> history, String platformUpiId) { }

    @Transactional(readOnly = true)
    public List<PlanView> listPlans() {
        return plans.findByEnabledTrueOrderByPriceAsc().stream()
                .map(p -> new PlanView(p.getId(), p.getPlanCode(), p.getPlanName(), p.getPrice(), p.getDurationMonths()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long hotelId) {
        return subscriptions.existsByHotelIdAndStatusAndEndDateGreaterThanEqual(
                hotelId, HotelSubscription.Status.ACTIVE, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public CurrentSubscription current(Long hotelId) {
        Hotel hotel = hotels.findByIdAndDeletedFlagFalse(hotelId).orElseThrow(() -> ApiException.notFound("Hotel"));
        LocalDate today = LocalDate.now();
        Optional<HotelSubscription> active = subscriptions
                .findFirstByHotelIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
                        hotelId, HotelSubscription.Status.ACTIVE, today);
        List<HotelSubscription> history = subscriptions.findByHotelIdOrderByIdDesc(hotelId);
        Map<Long, SubscriptionPlan> planMap = planMap();
        SubscriptionView activeView = active.map(s -> view(s, planMap, Map.of(hotelId, hotel.getHotelName()))).orElse(null);
        SubscriptionView pending = history.stream()
                .filter(s -> s.getStatus() == HotelSubscription.Status.PENDING_APPROVAL)
                .findFirst()
                .map(s -> view(s, planMap, Map.of(hotelId, hotel.getHotelName())))
                .orElse(null);
        long remaining = active.map(s -> Math.max(0, ChronoUnit.DAYS.between(today, s.getEndDate()) + 1)).orElse(0L);
        return new CurrentSubscription(hotel.getSubscriptionStatus().name(), active.isPresent(), remaining,
                activeView, pending,
                history.stream().map(s -> view(s, planMap, Map.of(hotelId, hotel.getHotelName()))).toList(),
                settings.value(PlatformSettingsService.Key.PAYMENT_UPI_ID));
    }

    @Transactional
    public SubscriptionView requestRenewal(Long hotelId, Long planId, String paymentReference, LocalDate paymentDate,
                                           String paymentProofKey) {
        SubscriptionPlan plan = plans.findById(planId)
                .filter(SubscriptionPlan::isEnabled)
                .orElseThrow(() -> ApiException.notFound("Subscription plan"));
        if (subscriptions.existsByHotelIdAndStatus(hotelId, HotelSubscription.Status.PENDING_APPROVAL)) {
            throw ApiException.conflict("A subscription request is already awaiting approval");
        }
        HotelSubscription request = new HotelSubscription();
        request.setHotelId(hotelId);
        request.setPlanId(plan.getId());
        request.setPaymentReference(paymentReference);
        request.setPaymentDate(paymentDate);
        request.setPaymentProofUrl(paymentProofKey);
        request.setStatus(HotelSubscription.Status.PENDING_APPROVAL);
        HotelSubscription saved = subscriptions.save(request);
        audit.record(hotelId, "SUBSCRIPTION_REQUESTED", "HotelSubscription", saved.getId(), plan.getPlanCode());
        return view(saved, planMap(), hotelNames(List.of(hotelId)));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionView> list(HotelSubscription.Status status) {
        List<HotelSubscription> rows = status == null
                ? subscriptions.findAll(Sort.by(Sort.Direction.DESC, "id"))
                : subscriptions.findByStatusOrderByIdDesc(status);
        Map<Long, SubscriptionPlan> planMap = planMap();
        Map<Long, String> names = hotelNames(rows.stream().map(HotelSubscription::getHotelId).distinct().toList());
        return rows.stream().map(s -> view(s, planMap, names)).toList();
    }

    @Transactional
    public SubscriptionView approve(Long id, Long approverId) {
        HotelSubscription request = find(id);
        requirePending(request);
        SubscriptionPlan plan = plans.findById(request.getPlanId()).orElseThrow(() -> ApiException.notFound("Plan"));
        LocalDate today = LocalDate.now();
        LocalDate start = subscriptions
                .findFirstByHotelIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
                        request.getHotelId(), HotelSubscription.Status.ACTIVE, today)
                .map(current -> current.getEndDate().plusDays(1))
                .orElse(today);
        request.setStartDate(start);
        request.setEndDate(start.plusMonths(plan.getDurationMonths()).minusDays(1));
        request.setStatus(HotelSubscription.Status.ACTIVE);
        request.setApprovedBy(approverId);
        request.setApprovedDate(LocalDateTime.now());
        subscriptions.save(request);

        Hotel hotel = hotels.findById(request.getHotelId()).orElseThrow(() -> ApiException.notFound("Hotel"));
        hotel.setSubscriptionStatus(Hotel.SubscriptionStatus.ACTIVE);
        hotels.save(hotel);
        tenantStatus.evict(hotel.getId());
        audit.record(hotel.getId(), "SUBSCRIPTION_APPROVED", "HotelSubscription", id, plan.getPlanCode());
        return view(request, planMap(), hotelNames(List.of(hotel.getId())));
    }

    @Transactional
    public SubscriptionView reject(Long id, String reason) {
        HotelSubscription request = find(id);
        requirePending(request);
        request.setStatus(HotelSubscription.Status.REJECTED);
        subscriptions.save(request);
        audit.record(request.getHotelId(), "SUBSCRIPTION_REJECTED", "HotelSubscription", id, reason);
        return view(request, planMap(), hotelNames(List.of(request.getHotelId())));
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void expireOverdue() {
        LocalDate today = LocalDate.now();
        for (HotelSubscription overdue : subscriptions.findByStatusAndEndDateBefore(HotelSubscription.Status.ACTIVE, today)) {
            overdue.setStatus(HotelSubscription.Status.EXPIRED);
            subscriptions.save(overdue);
        }
        for (Hotel hotel : hotels.findAll()) {
            if (hotel.getSubscriptionStatus() == Hotel.SubscriptionStatus.ACTIVE
                    && !subscriptions.existsByHotelIdAndStatusAndEndDateGreaterThanEqual(
                    hotel.getId(), HotelSubscription.Status.ACTIVE, today)) {
                hotel.setSubscriptionStatus(Hotel.SubscriptionStatus.EXPIRED);
                hotels.save(hotel);
                audit.record(hotel.getId(), "SUBSCRIPTION_EXPIRED", "Hotel", hotel.getId(), null);
            }
        }
        tenantStatus.evictAll();
    }

    private HotelSubscription find(Long id) {
        return subscriptions.findById(id).orElseThrow(() -> ApiException.notFound("Subscription request"));
    }

    private void requirePending(HotelSubscription request) {
        if (request.getStatus() != HotelSubscription.Status.PENDING_APPROVAL) {
            throw ApiException.conflict("Only pending requests can be processed");
        }
    }

    private Map<Long, SubscriptionPlan> planMap() {
        return plans.findAll().stream().collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity()));
    }

    private Map<Long, String> hotelNames(List<Long> hotelIds) {
        return hotels.findAllById(hotelIds).stream().collect(Collectors.toMap(Hotel::getId, Hotel::getHotelName));
    }

    private SubscriptionView view(HotelSubscription s, Map<Long, SubscriptionPlan> planMap, Map<Long, String> names) {
        SubscriptionPlan plan = planMap.get(s.getPlanId());
        return new SubscriptionView(s.getId(), s.getHotelId(), names.get(s.getHotelId()),
                plan == null ? null : plan.getPlanName(), plan == null ? null : plan.getPrice(),
                s.getStatus().name(), s.getPaymentDate(), s.getPaymentReference(), s.getPaymentProofUrl(),
                s.getStartDate(), s.getEndDate());
    }
}
