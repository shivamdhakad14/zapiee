package com.Zapiee.staff_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StaffInfo {
    private Long id;
    private String email;
    private String name;
    private String mobile;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private Long restaurantId;
}
