package com.example.resilient_api.infrastructure.adapters.persistenceadapter.mapper;

import com.example.resilient_api.domain.model.User;
import com.example.resilient_api.infrastructure.adapters.persistenceadapter.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {
    User toModel(UserEntity entity);
    UserEntity toEntity(User user);
}

