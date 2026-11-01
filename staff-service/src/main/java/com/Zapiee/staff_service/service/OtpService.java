package com.Zapiee.staff_service.service;

import com.Zapiee.staff_service.dto.OtpData;
import com.Zapiee.staff_service.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    @Autowired
    private StaffRepository repository;

    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    private static final long OTP_EXPIRY_SECONDS = 120; // 2 minutes
    private static final int MAX_ATTEMPTS = 5;

    // -------- REQUEST OTP --------
    public String requestOtp(String identifier) {

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        OtpData otpData = new OtpData(
                otp,
                System.currentTimeMillis(),
                0
        );

        otpStorage.put(identifier, otpData);

        System.out.println("Generated OTP: " + otp);

        return "OTP Sent Successfully";
    }

    // -------- VERIFY OTP --------
    public String verifyOtp(String identifier, String otp) {

        OtpData otpData = otpStorage.get(identifier);

        if (otpData == null) {
            throw new RuntimeException("OTP not found");
        }

        // Check expiry
        long currentTime = System.currentTimeMillis();
        long otpTime = otpData.getCreatedTime();

        if ((currentTime - otpTime) > OTP_EXPIRY_SECONDS * 1000) {
            otpStorage.remove(identifier);
            throw new RuntimeException("OTP expired");
        }

        // Check attempts
        if (otpData.getAttempts() >= MAX_ATTEMPTS) {
            throw new RuntimeException("Too many attempts");
        }

        // Validate OTP
        if (!otpData.getOtp().equals(otp)) {
            otpData.incrementAttempts();
            throw new RuntimeException("Invalid OTP");
        }

        // Success → Remove OTP
        otpStorage.remove(identifier);

        return "Login Successful";
    }
}
