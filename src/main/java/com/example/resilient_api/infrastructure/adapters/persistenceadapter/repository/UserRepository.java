package com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository;

import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface UserRepository extends ReactiveCrudRepository<UserEntity, Long> {
    Mono<UserEntity> findByEmail(String email);
    Flux<UserEntity> findAllByIdIn(List<Long> ids);
}

