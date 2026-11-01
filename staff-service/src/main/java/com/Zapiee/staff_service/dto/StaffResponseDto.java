package com.Zapiee.staff_service.dto;

import com.Zapiee.staff_service.enums.WorkingStaffRole;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StaffResponseDto {
    private Long id;
    private String name;
    private String mobile;
    private BigDecimal salary;
    private WorkingStaffRole workingStaffRole;
    private String status;
    private String shift;
    private Long restaurantId;
    private LocalDateTime createdAt;
}
