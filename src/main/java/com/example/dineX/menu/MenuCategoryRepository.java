package com.example.dineX.menu;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {
    List<MenuCategory> findByHotelIdAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(Long hotelId);
    List<MenuCategory> findByHotelIdAndEnabledTrueAndDeletedFlagFalseOrderByDisplayPriorityAscNameAsc(Long hotelId);
    Optional<MenuCategory> findByIdAndHotelIdAndDeletedFlagFalse(Long id, Long hotelId);
    Optional<MenuCategory> findByHotelIdAndNameIgnoreCase(Long hotelId, String name);
}
