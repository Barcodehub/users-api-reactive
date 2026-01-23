package com.example.resilient_api.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum TechnicalMessage {

    INTERNAL_ERROR("500","Something went wrong, please try again", ""),
    INVALID_REQUEST("400", "Bad Request, please verify data", ""),
    INVALID_PARAMETERS(INVALID_REQUEST.getCode(), "Bad Parameters, please verify data", ""),
    UNSUPPORTED_OPERATION("501", "Method not supported, please try again", ""),
    USER_CREATED("201", "User created successfully", ""),
    USER_ALREADY_EXISTS("400", "User with this email already exists", "email"),
    USER_NOT_FOUND("404", "User not found", "id"),
    USER_NAME_REQUIRED("400", "User name is required", "name"),
    USER_EMAIL_REQUIRED("400", "User email is required", "email"),
    USER_NAME_TOO_LONG("400", "User name cannot exceed 100 characters", "name"),
    USER_EMAIL_TOO_LONG("400", "User email cannot exceed 150 characters", "email"),
    USER_EMAIL_INVALID("400", "User email format is invalid", "email"),
    USER_ROLE_REQUIRED("400", "User role (isAdmin) is required", "isAdmin"),
    USER_ID_REQUIRED("400", "User ID is required", "id"),
    USER_PASSWORD_REQUIRED("400", "User password is required", "password"),
    INVALID_CREDENTIALS("401", "Invalid email or password", "credentials"),
    TOKEN_EXPIRED("401", "Token has expired", "token"),
    TOKEN_INVALID("401", "Token is invalid", "token"),
    TOKEN_MISSING("401", "Authentication token is missing", "token"),
    UNAUTHORIZED("401", "Unauthorized access", "")
    ;

    private final String code;
    private final String message;
    private final String param;
}