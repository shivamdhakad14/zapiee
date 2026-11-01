package com.Zapiee.staff_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Current Pin is required")
    private String currentPin;

    @NotBlank(message = "New password is required")
    @Size(min = 4 , max = 6)
    @Pattern(
            regexp = "^[0-9]{4,6}$",
            message = "PIN must contain 4 to 6 digits only"
    )
    private String newPin;

    @NotBlank(message = "Confirm pin is required")
    private String confirmPin;
}