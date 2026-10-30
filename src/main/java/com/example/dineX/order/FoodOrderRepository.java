package com.example.dineX.order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    List<FoodOrder> findBySessionIdOrderByIdAsc(Long sessionId);
    List<FoodOrder> findBySessionIdInOrderByIdAsc(Collection<Long> sessionIds);
    List<FoodOrder> findByHotelIdAndStatusInOrderByIdAsc(Long hotelId, Collection<FoodOrder.Status> statuses);
    Optional<FoodOrder> findByIdAndHotelId(Long id, Long hotelId);
    boolean existsBySessionIdAndStatusIn(Long sessionId, Collection<FoodOrder.Status> statuses);
}
