package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.PasswordEncoderPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.api.UserServicePort;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserUseCase implements UserServicePort {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 150;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoderPort passwordEncoderPort;

    public UserUseCase(UserPersistencePort userPersistencePort, PasswordEncoderPort passwordEncoderPort) {
        this.userPersistencePort = userPersistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    @Override
    public Mono<User> registerUser(User user, String messageId) {
        return validateUser(user)
                .then(userPersistencePort.existsByEmail(user.email()))
                .filter(exists -> !exists)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.USER_ALREADY_EXISTS)))
                .map(exists -> {
                    String encodedPassword = passwordEncoderPort.encode(user.password());
                    return new User(user.id(), user.name(), user.email(), encodedPassword, user.isAdmin());
                })
                .flatMap(userPersistencePort::save);
    }

    @Override
    public Mono<User> getUserById(Long id, String messageId) {
        if (id == null) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_ID_REQUIRED));
        }

        return userPersistencePort.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.USER_NOT_FOUND)));
    }

    @Override
    public Mono<Map<Long, Boolean>> checkUsersExist(List<Long> ids, String messageId) {
        if (ids == null || ids.isEmpty()) {
            return Mono.just(Map.of());
        }

        return userPersistencePort.findExistingIdsByIds(ids)
                .collect(Collectors.toSet())
                .map(existingIds -> ids.stream()
                        .collect(Collectors.toMap(
                                id -> id,
                                existingIds::contains
                        ))
                );
    }

    @Override
    public Flux<User> getUsersByIds(List<Long> ids, String messageId) {
        if (ids == null || ids.isEmpty()) {
            return Flux.empty();
        }

        return userPersistencePort.findAllByIdIn(ids);
    }

    private Mono<Void> validateUser(User user) {
        if (user.name() == null || user.name().trim().isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_NAME_REQUIRED));
        }
        if (user.email() == null || user.email().trim().isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_EMAIL_REQUIRED));
        }
        if (user.password() == null || user.password().trim().isEmpty()) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_PASSWORD_REQUIRED));
        }
        if (user.name().length() > MAX_NAME_LENGTH) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_NAME_TOO_LONG));
        }
        if (user.email().length() > MAX_EMAIL_LENGTH) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_EMAIL_TOO_LONG));
        }
        if (!EMAIL_PATTERN.matcher(user.email()).matches()) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_EMAIL_INVALID));
        }
        if (user.isAdmin() == null) {
            return Mono.error(new BusinessException(TechnicalMessage.USER_ROLE_REQUIRED));
        }
        return Mono.empty();
    }
}


