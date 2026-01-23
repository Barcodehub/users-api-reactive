package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.JwtPayload;
import com.example.resilient_api.domain.model.LoginRequest;
import com.example.resilient_api.domain.model.LoginResponse;
import reactor.core.publisher.Mono;

public interface AuthServicePort {
    Mono<LoginResponse> login(LoginRequest loginRequest, String messageId);
    Mono<JwtPayload> validateToken(String token);
}
