package com.example.dineX.hotel;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Optional<Hotel> findByIdAndDeletedFlagFalse(Long id);
    Optional<Hotel> findByMenuQrTokenAndDeletedFlagFalse(String menuQrToken);
    Page<Hotel> findByDeletedFlagFalse(Pageable pageable);
    boolean existsByEmail(String email);
}
