package com.Zapiee.staff_service.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
public class RestaurantResponse {

    private Long id;
    private String name;
    private String status;

}
