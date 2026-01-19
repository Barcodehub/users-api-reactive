package com.example.resilient_api.domain.api;

import com.example.resilient_api.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface UserServicePort {
    Mono<User> registerUser(User user, String messageId);
    Mono<User> getUserById(Long id, String messageId);
    Mono<Map<Long, Boolean>> checkUsersExist(List<Long> ids, String messageId);
    Flux<User> getUsersByIds(List<Long> ids, String messageId);
}
