package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.UserServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.UserDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.UserIdsRequest;
import com.example.resilient_api.infrastructure.entrypoints.mapper.UserMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.List;

import static com.example.resilient_api.infrastructure.entrypoints.util.Constants.X_MESSAGE_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserHandlerImpl {

    private final UserServicePort userServicePort;
    private final UserMapper userMapper;

    @Operation(
        operationId = "createUser",
        summary = "Registrar usuario",
        description = "Crea un nuevo usuario (endpoint público)",
        tags = {"Usuarios"},
        responses = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o email duplicado")
        }
    )
    public Mono<ServerResponse> createUser(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(UserDTO.class)
                .flatMap(userDTO -> userServicePort.registerUser(
                        userMapper.userDTOToUser(userDTO), messageId)
                        .doOnSuccess(savedUser -> log.info("User created successfully with messageId: {}", messageId))
                )
                .flatMap(savedUser -> ServerResponse.status(HttpStatus.CREATED)
                        .bodyValue(userMapper.userToUserDTO(savedUser)))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error on User - [ERROR]", ex))
                .onErrorResume(BusinessException.class, ex -> handleBusinessException(ex, messageId))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    @Operation(
        operationId = "getUserById",
        summary = "Obtener usuario por ID",
        description = "Obtiene un usuario por su ID (endpoint interno)",
        tags = {"Usuarios"},
        parameters = @Parameter(name = "id", in = ParameterIn.PATH, description = "ID del usuario")
    )
    public Mono<ServerResponse> getUserById(ServerRequest request) {
        String messageId = getMessageId(request);
        try {
            Long userId = Long.parseLong(request.pathVariable("id"));
            return userServicePort.getUserById(userId, messageId)
                    .doOnSuccess(user -> log.info("User retrieved successfully with messageId: {}", messageId))
                    .flatMap(user -> ServerResponse.status(HttpStatus.OK)
                            .bodyValue(userMapper.userToUserDTO(user)))
                    .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                    .doOnError(ex -> log.error("Error getting user by id for messageId: {}", messageId, ex))
                    .onErrorResume(BusinessException.class, ex -> handleBusinessException(ex, messageId))
                    .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                    .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
        } catch (NumberFormatException e) {
            log.error("Invalid user ID format for messageId: {}", messageId, e);
            return handleBusinessException(
                    new BusinessException(TechnicalMessage.USER_ID_REQUIRED), messageId);
        }
    }

    @Operation(
        operationId = "checkUsersExist",
        summary = "Verificar existencia de usuarios",
        description = "Verifica si los usuarios existen (endpoint interno)",
        tags = {"Usuarios"}
    )
    public Mono<ServerResponse> checkUsersExist(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(UserIdsRequest.class)
                .flatMap(idsRequest -> {
                    List<Long> ids = idsRequest.getIds() != null ? idsRequest.getIds() : List.of();
                    return userServicePort.checkUsersExist(ids, messageId)
                            .doOnSuccess(result -> log.info("Users existence checked successfully with messageId: {}", messageId));
                })
                .flatMap(result -> ServerResponse.status(HttpStatus.OK).bodyValue(result))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error checking users existence for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    @Operation(
        operationId = "getUsersByIds",
        summary = "Obtener usuarios por IDs",
        description = "Obtiene usuarios por sus IDs (endpoint interno)",
        tags = {"Usuarios"}
    )
    public Mono<ServerResponse> getUsersByIds(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(UserIdsRequest.class)
                .flatMapMany(idsRequest -> {
                    List<Long> ids = idsRequest.getIds() != null ? idsRequest.getIds() : List.of();
                    return userServicePort.getUsersByIds(ids, messageId)
                            .map(userMapper::userToUserDTO)
                            .doOnComplete(() -> log.info("Users retrieved successfully with messageId: {}", messageId));
                })
                .collectList()
                .flatMap(users -> ServerResponse.status(HttpStatus.OK).bodyValue(users))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error getting users by ids for messageId: {}", messageId, ex))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    private Mono<ServerResponse> handleBusinessException(BusinessException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                messageId,
                TechnicalMessage.INVALID_PARAMETERS,
                List.of(buildErrorDTO(ex.getTechnicalMessage())));
    }

    private Mono<ServerResponse> handleTechnicalException(TechnicalException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(buildErrorDTO(ex.getTechnicalMessage())));
    }

    private Mono<ServerResponse> handleUnexpectedException(Throwable ex, String messageId) {
        log.error("Unexpected error occurred for messageId: {}", messageId, ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                messageId,
                TechnicalMessage.INTERNAL_ERROR,
                List.of(ErrorDTO.builder()
                        .code(TechnicalMessage.INTERNAL_ERROR.getCode())
                        .message(TechnicalMessage.INTERNAL_ERROR.getMessage())
                        .build()));
    }

    private ErrorDTO buildErrorDTO(TechnicalMessage technicalMessage) {
        return ErrorDTO.builder()
                .code(technicalMessage.getCode())
                .message(technicalMessage.getMessage())
                .param(technicalMessage.getParam())
                .build();
    }

    private Mono<ServerResponse> buildErrorResponse(HttpStatus httpStatus, String identifier, TechnicalMessage error,
                                                    List<ErrorDTO> errors) {
        return Mono.defer(() -> {
            APIResponse apiErrorResponse = APIResponse
                    .builder()
                    .code(error.getCode())
                    .message(error.getMessage())
                    .identifier(identifier)
                    .date(Instant.now().toString())
                    .errors(errors)
                    .build();
            return ServerResponse.status(httpStatus)
                    .bodyValue(apiErrorResponse);
        });
    }

    private String getMessageId(ServerRequest serverRequest) {
        return serverRequest.headers().firstHeader(X_MESSAGE_ID);
    }
}
