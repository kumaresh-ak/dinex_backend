package com.example.dineX.subscription;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    List<SubscriptionPlan> findByEnabledTrueOrderByPriceAsc();
}
