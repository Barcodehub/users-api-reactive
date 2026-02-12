package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.AuthServicePort;
import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.api.PasswordEncoderPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.JwtPayload;
import com.example.resilient_api.domain.model.LoginRequest;
import com.example.resilient_api.domain.model.LoginResponse;
import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class AuthUseCase implements AuthServicePort {

    private final UserPersistencePort userPersistencePort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final JwtPort jwtPort;

    @Override
    public Mono<LoginResponse> login(LoginRequest loginRequest, String messageId) {
        log.info("Starting login process for email: {} with messageId: {}", loginRequest.email(), messageId);

        return Mono.defer(() -> {
                    try {
                        validateLoginRequestSync(loginRequest);
                        return userPersistencePort.findByEmail(loginRequest.email());
                    } catch (BusinessException e) {
                        return Mono.error(e);
                    }
                })
                .switchIfEmpty(Mono.error(new BusinessException(TechnicalMessage.INVALID_CREDENTIALS)))
                .flatMap(user -> validatePassword(loginRequest.password(), user))
                .map(this::buildJwtPayload)
                .map(payload -> buildLoginResponse(payload, jwtPort.generateToken(payload)))
                .doOnSuccess(response -> log.info("Login successful for user: {} with messageId: {}",
                        response.userId(), messageId))
                .doOnError(ex -> log.error("Login failed with messageId: {}", messageId, ex));
    }

    @Override
    public Mono<JwtPayload> validateToken(String token) {
        return jwtPort.validateAndExtractPayload(token);
    }

    private void validateLoginRequestSync(LoginRequest loginRequest) {
        // Verificar null primero, luego blank
        if (loginRequest.email() == null) {
            throw new BusinessException(TechnicalMessage.USER_EMAIL_REQUIRED);
        }
        if (loginRequest.email().isBlank()) {
            throw new BusinessException(TechnicalMessage.USER_EMAIL_REQUIRED);
        }
        if (loginRequest.password() == null) {
            throw new BusinessException(TechnicalMessage.USER_PASSWORD_REQUIRED);
        }
        if (loginRequest.password().isBlank()) {
            throw new BusinessException(TechnicalMessage.USER_PASSWORD_REQUIRED);
        }
    }

    private Mono<User> validatePassword(String rawPassword, User user) {
        return passwordEncoderPort.matches(rawPassword, user.password())
                .flatMap(matches -> {
                    if (Boolean.TRUE.equals(matches)) {
                        return Mono.just(user);
                    }
                    return Mono.error(new BusinessException(TechnicalMessage.INVALID_CREDENTIALS));
                });
    }

    private JwtPayload buildJwtPayload(User user) {
        return new JwtPayload(user.id(), user.email(), user.isAdmin());
    }

    private LoginResponse buildLoginResponse(JwtPayload payload, String token) {
        return new LoginResponse(token, payload.userId(), payload.email(), payload.isAdmin());
    }
}
