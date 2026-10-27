package com.example.dineX.billing;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstBySessionIdOrderByIdDesc(Long sessionId);
    List<Payment> findByBillIdOrderByIdDesc(Long billId);
    List<Payment> findByHotelIdOrderByIdDesc(Long hotelId);
    Optional<Payment> findFirstByBillIdAndStatusOrderByIdDesc(Long billId, Payment.Status status);
}
