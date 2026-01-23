package com.example.resilient_api.domain.model;

public record LoginResponse(String token, Long userId, String email, Boolean isAdmin) {
}
