package com.example.dineX.staff;

import com.example.dineX.audit.AuditService;
import com.example.dineX.common.ApiException;
import com.example.dineX.security.UserRole;
import com.example.dineX.staff.StaffDtos.CreateStaffRequest;
import com.example.dineX.staff.StaffDtos.StaffView;
import com.example.dineX.user.User;
import com.example.dineX.user.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public StaffService(UserRepository users, PasswordEncoder passwordEncoder, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<StaffView> list(Long hotelId) {
        return users.findByHotelIdAndDeletedFlagFalse(hotelId).stream()
                .filter(u -> u.getRole() == UserRole.CHEF)
                .map(this::view)
                .toList();
    }

    @Transactional
    public StaffView create(Long hotelId, CreateStaffRequest request) {
        String username = request.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw ApiException.conflict("A user with this email already exists");
        }
        User chef = new User(hotelId, username, passwordEncoder.encode(request.password()),
                request.displayName().trim(), request.mobileNumber(), UserRole.CHEF);
        users.save(chef);
        audit.record(hotelId, "STAFF_CREATED", "User", chef.getId(), username);
        return view(chef);
    }

    @Transactional
    public StaffView setEnabled(Long hotelId, Long id, boolean enabled) {
        User chef = findChef(hotelId, id);
        chef.setEnabled(enabled);
        users.save(chef);
        audit.record(hotelId, enabled ? "STAFF_ENABLED" : "STAFF_DISABLED", "User", id, chef.getUsername());
        return view(chef);
    }

    @Transactional
    public void delete(Long hotelId, Long id) {
        User chef = findChef(hotelId, id);
        chef.setDeletedFlag(true);
        chef.setEnabled(false);
        users.save(chef);
        audit.record(hotelId, "STAFF_DELETED", "User", id, chef.getUsername());
    }

    private User findChef(Long hotelId, Long id) {
        return users.findByIdAndDeletedFlagFalse(id)
                .filter(u -> hotelId.equals(u.getHotelId()) && u.getRole() == UserRole.CHEF)
                .orElseThrow(() -> ApiException.notFound("Staff member"));
    }

    private StaffView view(User u) {
        return new StaffView(u.getId(), u.getDisplayName(), u.getUsername(), u.getMobileNumber(), u.isEnabled(),
                u.getLastLoginAt());
    }
}
