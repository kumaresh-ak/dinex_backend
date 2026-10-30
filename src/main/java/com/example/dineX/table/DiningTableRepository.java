package com.example.dineX.table;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findByHotelIdAndDeletedFlagFalseOrderByTableNumberAsc(Long hotelId);
    Optional<DiningTable> findByIdAndHotelIdAndDeletedFlagFalse(Long id, Long hotelId);
    Optional<DiningTable> findByQrTokenAndEnabledTrueAndDeletedFlagFalse(String qrToken);

    /** Locks the table row so concurrent scans of the same table cannot each open a session. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.qrToken = :qrToken and t.enabled = true and t.deletedFlag = false")
    Optional<DiningTable> lockByQrToken(@Param("qrToken") String qrToken);
    boolean existsByHotelIdAndTableNumber(Long hotelId, String tableNumber);
}
