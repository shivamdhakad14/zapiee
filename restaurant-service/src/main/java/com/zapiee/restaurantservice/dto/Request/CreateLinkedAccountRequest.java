package com.zapiee.restaurantservice.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLinkedAccountRequest {

    // ── Business details ─────────────────────────────────────────
    @NotBlank @Size(max = 100)
    private String legalBusinessName;       // e.g. "Spice Garden Pvt Ltd"

    @NotBlank
    private String businessType;            // "route" (required for Route)

    @NotBlank
    private String businessCategory;        // "food_and_beverage" | "restaurant" | "other"

    private String businessSubCategory;

    // ── Contact person (must be KYC verified) ────────────────────
    @NotBlank @Size(max = 100)
    private String contactName;

    @NotBlank @Email
    private String contactEmail;            // KYC link sent here

    @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$")
    private String contactPhone;

    // ── Bank account for settlements ─────────────────────────────
    @NotBlank @Size(min = 9, max = 18)
    private String bankAccountNumber;

    @NotBlank @Size(min = 11, max = 11)
    private String ifscCode;

    @NotBlank
    private String beneficiaryName;         // Name on the bank account

    // ── PAN (required for KYC) ───────────────────────────────────
    @NotBlank @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}")
    private String panNumber;

    // ── Business address ─────────────────────────────────────────
    @NotBlank
    private String addressLine1;

    private String addressLine2;

    @NotBlank
    private String city;

    @NotBlank @Size(min = 6, max = 6)
    private String pincode;

    @NotBlank
    private String state;

    @Builder.Default
    private String country = "IN";
}
