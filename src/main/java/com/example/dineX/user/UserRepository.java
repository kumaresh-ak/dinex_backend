package com.example.dineX.user;

import com.example.dineX.security.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndEnabledTrueAndDeletedFlagFalse(String username);
    Optional<User> findByUsernameAndDeletedFlagFalse(String username);
    Optional<User> findByIdAndDeletedFlagFalse(Long id);
    List<User> findByHotelIdAndRoleAndEnabledTrueAndDeletedFlagFalse(Long hotelId, UserRole role);
    List<User> findByHotelIdAndDeletedFlagFalse(Long hotelId);
    Optional<User> findFirstByHotelIdAndRoleAndDeletedFlagFalseOrderByIdAsc(Long hotelId, UserRole role);
    boolean existsByUsername(String username);
}
