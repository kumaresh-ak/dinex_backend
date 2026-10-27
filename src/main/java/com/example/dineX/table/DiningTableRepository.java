package com.example.dineX.table;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findByHotelIdAndDeletedFlagFalseOrderByTableNumberAsc(Long hotelId);
    Optional<DiningTable> findByIdAndHotelIdAndDeletedFlagFalse(Long id, Long hotelId);
    Optional<DiningTable> findByQrTokenAndEnabledTrueAndDeletedFlagFalse(String qrToken);
    boolean existsByHotelIdAndTableNumber(Long hotelId, String tableNumber);
}
