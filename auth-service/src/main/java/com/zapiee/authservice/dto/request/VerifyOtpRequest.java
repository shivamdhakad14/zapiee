package com.zapiee.authservice.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyOtpRequest {

    @NotBlank(message = "email is required")
    @Size(max = 50, message = "email must not exceed 50 characters")
    private String email;

    @NotBlank(message = "otp is required")
    @Size(max = 6, message = "otp must not exceed 6 characters")
    private String otp;
}
