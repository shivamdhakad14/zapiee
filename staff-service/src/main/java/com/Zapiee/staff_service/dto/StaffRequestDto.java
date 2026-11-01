package com.Zapiee.staff_service.dto;

import com.Zapiee.staff_service.enums.StaffShift;
import com.Zapiee.staff_service.enums.StaffStatus;
import com.Zapiee.staff_service.enums.WorkingStaffRole;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StaffRequestDto {

    @NotBlank(message = "Staff name is required")
    @Size(min = 2, max = 100, message = "Staff name must be between 2 and 100 characters")
    @Pattern(
            regexp = "^[\\p{L}][\\p{L} .'-]*$",
            message = "Staff name can contain letters, spaces, periods, apostrophes, and hyphens"
    )
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number"
    )
    private String mobile;

    @NotNull(message = "Salary is required")
    @DecimalMin(value = "0.01", message = "Salary must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Salary must have at most 10 integer digits and 2 decimal places")
    private BigDecimal salary;

    @NotBlank(message = "PIN is required")
    @Pattern(
            regexp = "^\\d{4,6}$",
            message = "PIN must contain 4 to 6 digits"
    )
    private String pin;

    @NotBlank(message = "Confirm PIN is required")
    @Pattern(
            regexp = "^\\d{4,6}$",
            message = "Confirm PIN must contain 4 to 6 digits"
    )
    private String confirmPin;

    @NotNull(message = "Staff status is required")
    private StaffStatus status;

    @NotNull(message = "Staff shift is required")
    private StaffShift shift;

    @NotNull(message = "Restaurant ID is required")
    @Positive(message = "Restaurant ID must be greater than zero")
    private Long restaurantId;

    @NotNull(message = "Working staff role is required")
    private WorkingStaffRole workingStaffRole;
}