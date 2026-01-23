package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.JwtPayload;
import reactor.core.publisher.Mono;

public interface JwtPort {
    String generateToken(JwtPayload payload);
    Mono<JwtPayload> validateAndExtractPayload(String token);
}
