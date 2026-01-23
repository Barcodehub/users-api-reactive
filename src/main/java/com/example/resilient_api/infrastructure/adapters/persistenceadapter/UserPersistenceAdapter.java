package com.example.resilient_api.infrastructure.adapters.persistenceadapter;

import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.UserEntity;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.UserEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.UserRepository;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {
    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    @Override
    public Mono<User> save(User user) {
        return userRepository.save(userEntityMapper.toEntity(user))
                .map(userEntityMapper::toModel);
    }

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .map(userEntityMapper::toModel);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userEntityMapper::toModel);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.findByEmail(email)
                .hasElement();
    }

    @Override
    public Flux<Long> findExistingIdsByIds(List<Long> ids) {
        return userRepository.findAllByIdIn(ids)
                .map(UserEntity::getId);
    }

    @Override
    public Flux<User> findAllByIdIn(List<Long> ids) {
        return userRepository.findAllByIdIn(ids)
                .map(userEntityMapper::toModel);
    }
}
