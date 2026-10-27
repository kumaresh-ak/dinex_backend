package com.example.dineX.subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelSubscriptionRepository extends JpaRepository<HotelSubscription, Long> {
    List<HotelSubscription> findByHotelIdOrderByIdDesc(Long hotelId);
    List<HotelSubscription> findByStatusOrderByIdDesc(HotelSubscription.Status status);
    List<HotelSubscription> findByStatusAndEndDateBefore(HotelSubscription.Status status, LocalDate date);
    boolean existsByHotelIdAndStatus(Long hotelId, HotelSubscription.Status status);
    Optional<HotelSubscription> findFirstByHotelIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
            Long hotelId, HotelSubscription.Status status, LocalDate date);
    boolean existsByHotelIdAndStatusAndEndDateGreaterThanEqual(Long hotelId, HotelSubscription.Status status, LocalDate date);
}
