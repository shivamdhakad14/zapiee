package com.Zapiee.staff_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginIdentifierRequestDto {

    @NotBlank(message = "Email or mobile number is required")
    @Size(max = 254, message = "Email or mobile number cannot exceed 254 characters")
    @Pattern(
            regexp = "^(?:[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+|[6-9]\\d{9})$",
            message = "Enter a valid email address or 10-digit Indian mobile number"
    )
    private String emailOrMobile;

    @NotBlank(message = "PIN is required")
    @Pattern(
            regexp = "^\\d{4,6}$",
            message = "PIN must contain 4 to 6 digits"
    )
    private String pin;
}