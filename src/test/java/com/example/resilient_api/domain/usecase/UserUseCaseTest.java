package com.example.resilient_api.domain.usecase;

import com.example.resilient_api.domain.api.PasswordEncoderPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.domain.spi.UserPersistencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserPersistencePort userPersistencePort;

    @Mock
    private PasswordEncoderPort passwordEncoderPort;

    @InjectMocks
    private UserUseCase userUseCase;

    private User validUser;
    private String messageId;

    @BeforeEach
    void setUp() {
        validUser = new User(null, "John Doe", "john@example.com", "password123", false);
        messageId = "test-message-id-123";
    }

    @Test
    void registerUser_WithValidData_ShouldReturnSavedUser() {
        // Arrange
        User savedUser = new User(1L, "John Doe", "john@example.com", "encodedPassword", false);
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
        when(userPersistencePort.save(any(User.class))).thenReturn(Mono.just(savedUser));

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(validUser, messageId))
                .expectNext(savedUser)
                .verifyComplete();

        verify(userPersistencePort).existsByEmail("john@example.com");
        verify(passwordEncoderPort).encode("password123");
        verify(userPersistencePort).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldThrowBusinessException() {
        // Arrange
        when(userPersistencePort.existsByEmail(anyString())).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(validUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_ALREADY_EXISTS)
                .verify();

        verify(userPersistencePort).existsByEmail("john@example.com");
        verify(userPersistencePort, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithNullName_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, null, "john@example.com", "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_NAME_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).existsByEmail(anyString());
    }

    @Test
    void registerUser_WithEmptyName_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "   ", "john@example.com", "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_NAME_REQUIRED)
                .verify();
    }

    @Test
    void registerUser_WithTooLongName_ShouldThrowBusinessException() {
        // Arrange
        String longName = "a".repeat(101);
        User invalidUser = new User(null, longName, "john@example.com", "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_NAME_TOO_LONG)
                .verify();
    }

    @Test
    void registerUser_WithNullEmail_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", null, "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_REQUIRED)
                .verify();
    }

    @Test
    void registerUser_WithEmptyEmail_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", "   ", "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_REQUIRED)
                .verify();
    }

    @Test
    void registerUser_WithInvalidEmailFormat_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", "invalid-email", "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_INVALID)
                .verify();
    }

    @Test
    void registerUser_WithTooLongEmail_ShouldThrowBusinessException() {
        // Arrange
        String longEmail = "a".repeat(140) + "@example.com"; // > 150 chars
        User invalidUser = new User(null, "John Doe", longEmail, "password123", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_EMAIL_TOO_LONG)
                .verify();
    }

    @Test
    void registerUser_WithNullPassword_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", "john@example.com", null, false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_PASSWORD_REQUIRED)
                .verify();
    }

    @Test
    void registerUser_WithEmptyPassword_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", "john@example.com", "   ", false);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_PASSWORD_REQUIRED)
                .verify();
    }

    @Test
    void registerUser_WithNullIsAdmin_ShouldThrowBusinessException() {
        // Arrange
        User invalidUser = new User(null, "John Doe", "john@example.com", "password123", null);

        // Act & Assert
        StepVerifier.create(userUseCase.registerUser(invalidUser, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_ROLE_REQUIRED)
                .verify();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Arrange
        Long userId = 1L;
        User user = new User(userId, "John Doe", "john@example.com", "encodedPassword", false);
        when(userPersistencePort.findById(userId)).thenReturn(Mono.just(user));

        // Act & Assert
        StepVerifier.create(userUseCase.getUserById(userId, messageId))
                .expectNext(user)
                .verifyComplete();

        verify(userPersistencePort).findById(userId);
    }

    @Test
    void getUserById_WithNullId_ShouldThrowBusinessException() {
        // Act & Assert
        StepVerifier.create(userUseCase.getUserById(null, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_ID_REQUIRED)
                .verify();

        verify(userPersistencePort, never()).findById(any());
    }

    @Test
    void getUserById_WithNonExistingId_ShouldThrowBusinessException() {
        // Arrange
        Long userId = 999L;
        when(userPersistencePort.findById(userId)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userUseCase.getUserById(userId, messageId))
                .expectErrorMatches(throwable -> throwable instanceof BusinessException &&
                        ((BusinessException) throwable).getTechnicalMessage() == TechnicalMessage.USER_NOT_FOUND)
                .verify();

        verify(userPersistencePort).findById(userId);
    }

    @Test
    void checkUsersExist_WithExistingIds_ShouldReturnAllTrue() {
        // Arrange
        List<Long> ids = List.of(1L, 2L, 3L);
        when(userPersistencePort.findExistingIdsByIds(ids)).thenReturn(Flux.just(1L, 2L, 3L));

        // Act & Assert
        StepVerifier.create(userUseCase.checkUsersExist(ids, messageId))
                .expectNextMatches(result ->
                        result.size() == 3 &&
                        result.get(1L) &&
                        result.get(2L) &&
                        result.get(3L)
                )
                .verifyComplete();

        verify(userPersistencePort).findExistingIdsByIds(ids);
    }

    @Test
    void checkUsersExist_WithNonExistingIds_ShouldReturnFalse() {
        // Arrange
        List<Long> ids = List.of(1L, 999L);
        when(userPersistencePort.findExistingIdsByIds(ids)).thenReturn(Flux.just(1L));

        // Act & Assert
        StepVerifier.create(userUseCase.checkUsersExist(ids, messageId))
                .expectNextMatches(result ->
                        result.size() == 2 &&
                        result.get(1L) &&
                        !result.get(999L)
                )
                .verifyComplete();
    }

    @Test
    void checkUsersExist_WithNullIds_ShouldReturnEmptyMap() {
        // Act & Assert
        StepVerifier.create(userUseCase.checkUsersExist(null, messageId))
                .expectNextMatches(result -> result.isEmpty())
                .verifyComplete();

        verify(userPersistencePort, never()).findExistingIdsByIds(any());
    }

    @Test
    void checkUsersExist_WithEmptyIds_ShouldReturnEmptyMap() {
        // Act & Assert
        StepVerifier.create(userUseCase.checkUsersExist(List.of(), messageId))
                .expectNextMatches(result -> result.isEmpty())
                .verifyComplete();

        verify(userPersistencePort, never()).findExistingIdsByIds(any());
    }

    @Test
    void getUsersByIds_WithValidIds_ShouldReturnUsers() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        User user1 = new User(1L, "John Doe", "john@example.com", "pass", false);
        User user2 = new User(2L, "Jane Doe", "jane@example.com", "pass", false);

        when(userPersistencePort.findAllByIdIn(ids)).thenReturn(Flux.just(user1, user2));

        // Act & Assert
        StepVerifier.create(userUseCase.getUsersByIds(ids, messageId))
                .expectNext(user1)
                .expectNext(user2)
                .verifyComplete();

        verify(userPersistencePort).findAllByIdIn(ids);
    }

    @Test
    void getUsersByIds_WithNullIds_ShouldReturnEmpty() {
        // Act & Assert
        StepVerifier.create(userUseCase.getUsersByIds(null, messageId))
                .verifyComplete();

        verify(userPersistencePort, never()).findAllByIdIn(any());
    }

    @Test
    void getUsersByIds_WithEmptyIds_ShouldReturnEmpty() {
        // Act & Assert
        StepVerifier.create(userUseCase.getUsersByIds(List.of(), messageId))
                .verifyComplete();

        verify(userPersistencePort, never()).findAllByIdIn(any());
    }
}
