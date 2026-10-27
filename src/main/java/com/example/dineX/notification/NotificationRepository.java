package com.example.dineX.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByHotelIdOrderByIdDesc(Long hotelId);
    long countByHotelIdAndReadFlagFalse(Long hotelId);
    List<Notification> findByHotelIdAndReadFlagFalse(Long hotelId);
    Optional<Notification> findByIdAndHotelId(Long id, Long hotelId);
}
