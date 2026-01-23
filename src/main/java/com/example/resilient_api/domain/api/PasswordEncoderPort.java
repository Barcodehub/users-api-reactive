package com.example.resilient_api.domain.api;

import reactor.core.publisher.Mono;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
    Mono<Boolean> matches(String rawPassword, String encodedPassword);
}
