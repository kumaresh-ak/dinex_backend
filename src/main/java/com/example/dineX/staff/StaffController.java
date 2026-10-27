package com.example.dineX.staff;

import com.example.dineX.security.AuthUser;
import com.example.dineX.staff.StaffDtos.CreateStaffRequest;
import com.example.dineX.staff.StaffDtos.StaffView;
import com.example.dineX.staff.StaffDtos.StatusRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hotel/staff")
public class StaffController {
    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public List<StaffView> list(@AuthenticationPrincipal AuthUser user) {
        return staffService.list(user.hotelId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffView create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody CreateStaffRequest request) {
        return staffService.create(user.hotelId(), request);
    }

    @PatchMapping("/{id}/status")
    public StaffView status(@AuthenticationPrincipal AuthUser user, @PathVariable Long id,
                            @Valid @RequestBody StatusRequest request) {
        return staffService.setEnabled(user.hotelId(), id, request.enabled());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        staffService.delete(user.hotelId(), id);
    }
}
