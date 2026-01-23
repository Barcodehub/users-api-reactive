package com.example.resilient_api.infrastructure.entrypoints.mapper;

import com.example.resilient_api.domain.model.LoginRequest;
import com.example.resilient_api.domain.model.LoginResponse;
import com.example.resilient_api.infrastructure.entrypoints.dto.LoginRequestDTO;
import com.example.resilient_api.infrastructure.entrypoints.dto.LoginResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    LoginRequest loginRequestDTOToLoginRequest(LoginRequestDTO loginRequestDTO);

    LoginResponseDTO loginResponseToLoginResponseDTO(LoginResponse loginResponse);
}
