package com.example.dineX.support;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByHotelIdOrderByIdDesc(Long hotelId);
    List<SupportTicket> findAllByOrderByIdDesc();
    List<SupportTicket> findByStatusOrderByIdDesc(SupportTicket.Status status);
}
