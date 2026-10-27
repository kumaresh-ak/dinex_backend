package com.example.dineX.user;

import com.example.dineX.security.UserRole;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_id")
    private Long hotelId;

    @Column(nullable = false, unique = true, length = 190)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "deleted_flag", nullable = false)
    private boolean deletedFlag = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    protected User() { }

    public User(Long hotelId, String username, String passwordHash, String displayName, String mobileNumber, UserRole role) {
        this.hotelId = hotelId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.mobileNumber = mobileNumber;
        this.role = role;
    }

    public void changePasswordHash(String newHash) { this.passwordHash = newHash; }
    public void recordLogin() { this.lastLoginAt = LocalDateTime.now(); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDeletedFlag(boolean deletedFlag) { this.deletedFlag = deletedFlag; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }

    public Long getId() { return id; }
    public Long getHotelId() { return hotelId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getMobileNumber() { return mobileNumber; }
    public UserRole getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public boolean isDeletedFlag() { return deletedFlag; }
}
