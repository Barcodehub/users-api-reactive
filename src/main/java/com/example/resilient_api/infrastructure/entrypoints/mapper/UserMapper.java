package com.example.resilient_api.infrastructure.entrypoints.mapper;

import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.infrastructure.entrypoints.dto.UserDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User userDTOToUser(UserDTO userDTO);
    UserDTO userToUserDTO(User user);
}
