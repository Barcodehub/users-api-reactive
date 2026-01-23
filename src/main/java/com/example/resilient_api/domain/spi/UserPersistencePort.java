package com.example.resilient_api.domain.spi;

import com.example.resilient_api.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserPersistencePort {
    Mono<User> save(User user);
    Mono<User> findById(Long id);
    Mono<User> findByEmail(String email);
    Mono<Boolean> existsByEmail(String email);
    Flux<Long> findExistingIdsByIds(List<Long> ids);
    Flux<User> findAllByIdIn(List<Long> ids);
}

