package com.example.resilient_api.application.config;

import com.example.resilient_api.domain.api.AuthServicePort;
import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.api.PasswordEncoderPort;
import com.example.resilient_api.domain.api.UserServicePort;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import com.example.resilient_api.domain.usecase.AuthUseCase;
import com.example.resilient_api.domain.usecase.UserUseCase;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.UserPersistenceAdapter;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper.UserEntityMapper;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UseCasesConfig {
    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    @Bean
    public UserPersistencePort userPersistencePort() {
        return new UserPersistenceAdapter(userRepository, userEntityMapper);
    }

    @Bean
    public UserServicePort userServicePort(UserPersistencePort userPersistencePort, PasswordEncoderPort passwordEncoderPort) {
        return new UserUseCase(userPersistencePort, passwordEncoderPort);
    }

    @Bean
    public AuthServicePort authServicePort(UserPersistencePort userPersistencePort,
                                          PasswordEncoderPort passwordEncoderPort,
                                          JwtPort jwtPort) {
        return new AuthUseCase(userPersistencePort, passwordEncoderPort, jwtPort);
    }
}
