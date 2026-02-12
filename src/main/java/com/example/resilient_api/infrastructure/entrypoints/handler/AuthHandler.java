package com.example.resilient_api.infrastructure.entrypoints.handler;

import com.example.resilient_api.domain.api.AuthServicePort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.exceptions.TechnicalException;
import com.example.resilient_api.infrastructure.entrypoints.dto.LoginRequestDTO;
import com.example.resilient_api.infrastructure.entrypoints.mapper.AuthMapper;
import com.example.resilient_api.infrastructure.entrypoints.util.APIResponse;
import com.example.resilient_api.infrastructure.entrypoints.util.ErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
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
public class AuthHandler {

    private final AuthServicePort authServicePort;
    private final AuthMapper authMapper;

    @Operation(
        operationId = "login",
        summary = "Login de usuario",
        description = "Autentica un usuario y devuelve un token JWT (endpoint público)",
        tags = {"Autenticación"},
        responses = {
            @ApiResponse(responseCode = "200", description = "Login exitoso, retorna token JWT"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
        }
    )
    public Mono<ServerResponse> login(ServerRequest request) {
        String messageId = getMessageId(request);
        return request.bodyToMono(LoginRequestDTO.class)
                .flatMap(loginRequestDTO -> authServicePort.login(
                        authMapper.loginRequestDTOToLoginRequest(loginRequestDTO), messageId)
                        .doOnSuccess(response -> log.info("Login successful for user: {} with messageId: {}",
                                response.userId(), messageId))
                )
                .flatMap(loginResponse -> ServerResponse.status(HttpStatus.OK)
                        .bodyValue(authMapper.loginResponseToLoginResponseDTO(loginResponse)))
                .contextWrite(Context.of(X_MESSAGE_ID, messageId))
                .doOnError(ex -> log.error("Error on Login - [ERROR]", ex))
                .onErrorResume(BusinessException.class, ex -> handleBusinessException(ex, messageId))
                .onErrorResume(TechnicalException.class, ex -> handleTechnicalException(ex, messageId))
                .onErrorResume(ex -> handleUnexpectedException(ex, messageId));
    }

    private Mono<ServerResponse> handleBusinessException(BusinessException ex, String messageId) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                messageId,
                ex.getTechnicalMessage(),
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
