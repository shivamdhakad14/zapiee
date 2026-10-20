package com.zapiee.restaurantservice.dto.Response;

import com.zapiee.restaurantservice.enums.AccountStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private Long id;
    private String email;
    private String name;
    private Set<String> roles;
    private String phoneNumber;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private AccountStatus accountStatus;

}