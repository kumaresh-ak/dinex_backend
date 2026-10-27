package com.example.dineX.menu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(Long hotelId);
    List<MenuItem> findByHotelIdAndVisibleTrueAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(Long hotelId);
    List<MenuItem> findByHotelIdAndIdInAndDeletedFlagFalse(Long hotelId, Collection<Long> ids);
    Optional<MenuItem> findByIdAndHotelIdAndDeletedFlagFalse(Long id, Long hotelId);
    long countByHotelIdAndCategoryIdAndDeletedFlagFalse(Long hotelId, Long categoryId);
    boolean existsByHotelIdAndDeletedFlagFalse(Long hotelId);
}
