package com.example.dineX.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findBySessionId(Long sessionId);
    Optional<Bill> findByIdAndHotelId(Long id, Long hotelId);
    List<Bill> findByHotelIdAndStatusInOrderByIdDesc(Long hotelId, List<Bill.Status> statuses);
    List<Bill> findByHotelIdOrderByIdDesc(Long hotelId);
}
