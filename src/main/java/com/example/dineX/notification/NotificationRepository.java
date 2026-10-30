package com.example.dineX.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop100ByHotelIdOrderByIdDesc(Long hotelId);
    long countByHotelIdAndReadFlagFalse(Long hotelId);
    Optional<Notification> findByIdAndHotelId(Long id, Long hotelId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.hotelId = :hotelId and n.readFlag = false")
    int markAllReadByHotelId(@Param("hotelId") Long hotelId);
}
