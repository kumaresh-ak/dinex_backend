package com.example.dineX.security;

public record AuthUser(Long id, String username, Long hotelId, UserRole role) {
    public boolean isSuperAdmin() { return role == UserRole.SUPER_ADMIN; }
}
