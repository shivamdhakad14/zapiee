package com.zapiee.restaurantservice.dto.Response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserInfo {

    private Long id;
    private String email;
    private String mobil;

}