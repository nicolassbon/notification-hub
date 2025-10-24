package com.notificationhub.mapper;

import com.notificationhub.dto.response.UserResponse;
import com.notificationhub.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);
}
