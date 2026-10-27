package com.example.dineX.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningSessionRepository extends JpaRepository<DiningSession, Long> {
    Optional<DiningSession> findBySessionToken(String sessionToken);
    Optional<DiningSession> findFirstByTableIdAndStatusNotOrderByIdDesc(Long tableId, DiningSession.Status status);
    boolean existsByTableIdAndStatusNot(Long tableId, DiningSession.Status status);
    List<DiningSession> findByHotelIdAndStatusNotOrderByIdDesc(Long hotelId, DiningSession.Status status);
    Optional<DiningSession> findByIdAndHotelId(Long id, Long hotelId);
}
