package com.Zapiee.staff_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class OtpData {

    private String otp;
    private long createdTime;
    private int attempts;

    public void incrementAttempts() {
        this.attempts++;
    }
}
