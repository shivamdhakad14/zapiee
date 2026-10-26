package com.zapiee.authservice.service;

import com.zapiee.authservice.dto.response.UserInfo;
import com.zapiee.authservice.entity.Role;
import com.zapiee.authservice.entity.User;
import com.zapiee.authservice.exception.BusinessRuleViolationException;
import com.zapiee.authservice.exception.ResourceNotFoundException;
import com.zapiee.authservice.mapper.UserMapper;
import com.zapiee.authservice.repository.RoleRepository;
import com.zapiee.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;

    public UserInfo getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        return userMapper.mapToDto(user);
    }

    public void assignRole(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        Role userRole = roleRepository.findByName("ROLE_OWNER")
                .orElseThrow(() -> new BusinessRuleViolationException("Default role not found",
                        HttpStatus.INTERNAL_SERVER_ERROR.toString()));

        user.addRole(userRole);
        userRepository.save(user);
    }

    public UserInfo updateUserRole(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        Role userRole = roleRepository.findByName("ROLE_OWNER")
                .orElseThrow(() -> new BusinessRuleViolationException("role not found",
                        HttpStatus.INTERNAL_SERVER_ERROR.toString()));

        user.addRole(userRole);
        User save = userRepository.save(user);
        return userMapper.mapToDto(save);
    }

}
