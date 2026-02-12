package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.api.PasswordEncoderPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.JwtPayload;
import com.example.resilient_api.domain.model.LoginRequest;
import com.example.resilient_api.domain.model.LoginResponse;
import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @Mock
    private JwtPort jwtPort;

    @InjectMocks
    private AuthUseCase authUseCase;

    private LoginRequest validLoginRequest;
    private User validUser;
    private String messageId;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("john@example.com", "password123");
        validUser = new User(1L, "John Doe", "john@example.com", "encodedPassword", false);
        messageId = "test-message-id-123";
    }

    @Test
    void login_WithValidCredentials_ShouldReturnLoginResponse() {
        // Arrange
        JwtPayload payload = new JwtPayload(1L, "john@example.com", false);
        String token = "generated-jwt-token";

        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.just(validUser));
        when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(Mono.just(true));
        when(jwtPort.generateToken(any(JwtPayload.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectNextMatches(response ->
                        response.token().equals(token) &&
                        response.userId().equals(1L) &&
                        response.email().equals("john@example.com") &&
                        response.isAdmin().equals(false)
                )
                .verifyComplete();

        verify(userPersistencePort).findByEmail("john@example.com");
        verify(passwordEncoderPort).matches("password123", "encodedPassword");
        verify(jwtPort).generateToken(any(JwtPayload.class));
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowBusinessException() {
        // Arrange
        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.just(validUser));
        when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.INVALID_CREDENTIALS)
                .verify();

        verify(userPersistencePort).findByEmail("john@example.com");
        verify(passwordEncoderPort).matches("password123", "encodedPassword");
        verify(jwtPort, never()).generateToken(any());
    }

    @Test
    void login_WithNonExistingEmail_ShouldThrowBusinessException() {
        // Arrange
        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.INVALID_CREDENTIALS)
                .verify();

        verify(userPersistencePort).findByEmail("john@example.com");
        verify(passwordEncoderPort, never()).matches(anyString(), anyString());
        verify(jwtPort, never()).generateToken(any());
    }

    @Test
    void login_WithNullEmail_ShouldThrowBusinessException() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest(null, "password123");

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).findByEmail(anyString());
    }

    @Test
    void login_WithEmptyEmail_ShouldThrowBusinessException() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest("   ", "password123");

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).findByEmail(anyString());
    }

    @Test
    void login_WithNullPassword_ShouldThrowBusinessException() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest("john@example.com", null);

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_PASSWORD_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).findByEmail(anyString());
    }

    @Test
    void login_WithEmptyPassword_ShouldThrowBusinessException() {
        // Arrange
        LoginRequest invalidRequest = new LoginRequest("john@example.com", "   ");

        // Act & Assert
        StepVerifier.create(authUseCase.login(invalidRequest, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_PASSWORD_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).findByEmail(anyString());
    }

    @Test
    void login_WithAdminUser_ShouldReturnLoginResponseWithAdminTrue() {
        // Arrange
        User adminUser = new User(1L, "Admin User", "admin@example.com", "encodedPassword", true);
        String token = "admin-jwt-token";

        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.just(adminUser));
        when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(Mono.just(true));
        when(jwtPort.generateToken(any(JwtPayload.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectNextMatches(response ->
                        response.isAdmin().equals(true) &&
                        response.userId().equals(1L)
                )
                .verifyComplete();
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnJwtPayload() {
        // Arrange
        String token = "valid-jwt-token";
        JwtPayload expectedPayload = new JwtPayload(1L, "john@example.com", false);

        when(jwtPort.validateAndExtractPayload(token)).thenReturn(Mono.just(expectedPayload));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(token))
                .expectNext(expectedPayload)
                .verifyComplete();

        verify(jwtPort).validateAndExtractPayload(token);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldPropagateError() {
        // Arrange
        String invalidToken = "invalid-jwt-token";
        BusinessException expectedException = new BusinessException(TechnicalMessage.TOKEN_INVALID);

        when(jwtPort.validateAndExtractPayload(invalidToken)).thenReturn(Mono.error(expectedException));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(invalidToken))
                .expectError(BusinessException.class)
                .verify();

        verify(jwtPort).validateAndExtractPayload(invalidToken);
    }

    @Test
    void login_WithAdminUser_ShouldGenerateTokenWithAdminRole() {
        // Arrange
        User adminUser = new User(1L, "Admin User", "admin@example.com", "encodedPassword", true);
        String token = "admin-jwt-token";
        JwtPayload expectedPayload = new JwtPayload(1L, "admin@example.com", true);

        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.just(adminUser));
        when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(Mono.just(true));
        when(jwtPort.generateToken(any(JwtPayload.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectNextMatches(response ->
                        response.isAdmin().equals(true) &&
                        response.token().equals(token) &&
                        response.userId().equals(1L) &&
                        response.email().equals("admin@example.com")
                )
                .verifyComplete();

        verify(jwtPort).generateToken(argThat(payload ->
                payload.isAdmin() != null &&
                payload.isAdmin() &&
                payload.userId().equals(1L)
        ));
    }

    @Test
    void login_WithNonAdminUser_ShouldGenerateTokenWithoutAdminRole() {
        // Arrange
        User regularUser = new User(2L, "Regular User", "user@example.com", "encodedPassword", false);
        String token = "user-jwt-token";

        when(userPersistencePort.findByEmail(anyString())).thenReturn(Mono.just(regularUser));
        when(passwordEncoderPort.matches(anyString(), anyString())).thenReturn(Mono.just(true));
        when(jwtPort.generateToken(any(JwtPayload.class))).thenReturn(token);

        // Act & Assert
        StepVerifier.create(authUseCase.login(validLoginRequest, messageId))
                .expectNextMatches(response ->
                        response.isAdmin().equals(false) &&
                        response.userId().equals(2L)
                )
                .verifyComplete();

        verify(jwtPort).generateToken(argThat(payload ->
                payload.isAdmin() != null &&
                !payload.isAdmin() &&
                payload.userId().equals(2L)
        ));
    }

    @Test
    void validateToken_WithAdminToken_ShouldReturnAdminPayload() {
        // Arrange
        String adminToken = "admin-jwt-token";
        JwtPayload adminPayload = new JwtPayload(1L, "admin@example.com", true);

        when(jwtPort.validateAndExtractPayload(adminToken)).thenReturn(Mono.just(adminPayload));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(adminToken))
                .expectNextMatches(payload ->
                        payload.isAdmin() &&
                        payload.userId().equals(1L) &&
                        payload.email().equals("admin@example.com")
                )
                .verifyComplete();

        verify(jwtPort).validateAndExtractPayload(adminToken);
    }

    @Test
    void validateToken_WithNonAdminToken_ShouldReturnNonAdminPayload() {
        // Arrange
        String userToken = "user-jwt-token";
        JwtPayload userPayload = new JwtPayload(2L, "user@example.com", false);

        when(jwtPort.validateAndExtractPayload(userToken)).thenReturn(Mono.just(userPayload));

        // Act & Assert
        StepVerifier.create(authUseCase.validateToken(userToken))
                .expectNextMatches(payload ->
                        !payload.isAdmin() &&
                        payload.userId().equals(2L) &&
                        payload.email().equals("user@example.com")
                )
                .verifyComplete();

        verify(jwtPort).validateAndExtractPayload(userToken);
    }
}
