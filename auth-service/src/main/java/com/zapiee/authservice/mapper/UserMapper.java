package com.zapiee.authservice.mapper;

import com.zapiee.authservice.dto.response.UserInfo;
import com.zapiee.authservice.entity.Role;
import com.zapiee.authservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserInfo mapToDto(User user);

    default String map(Role role) {
        return role.getName();
    }
}
