package com.innowise.orderservice.config;

public record RequestAuthContext(
        Long userId,
        RequesterRole role,
        String serviceName
) {
    public boolean isAdmin() {
        return role == RequesterRole.ADMIN;
    }

    public boolean isEndUser() {
        return userId != null && role != null;
    }
}
