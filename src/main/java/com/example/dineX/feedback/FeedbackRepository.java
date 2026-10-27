package com.example.dineX.feedback;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    boolean existsBySessionId(Long sessionId);
    List<Feedback> findByHotelIdOrderByIdDesc(Long hotelId);
}
