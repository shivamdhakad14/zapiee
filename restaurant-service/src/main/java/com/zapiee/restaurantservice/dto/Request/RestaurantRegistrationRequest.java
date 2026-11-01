package com.zapiee.restaurantservice.dto.Request;

import com.zapiee.restaurantservice.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "Restaurant Registration Request",
        description = "Request payload used to register a new restaurant.")
public class RestaurantRegistrationRequest {


    @NotBlank(message = "Business type is required.")
    @Size(max = 100, message = "Business type cannot exceed 100 characters.")
    private String businessType;


    @NotBlank(message = "Business name is required.")
    @Size(max = 150, message = "Business name cannot exceed 150 characters.")
    private String businessName;


    @NotBlank(message = "Owner name is required.")
    @Size(max = 100, message = "Owner name cannot exceed 100 characters.")
    private String ownerName;


    @NotBlank(message = "Business contact is required.")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Please enter a valid 10-digit Indian mobile number.")
    private String businessContact;


    @NotBlank(message = "Business email is required.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 150, message = "Email cannot exceed 150 characters.")
    private String businessEmail;

    private String foodCategory;


    @NotBlank(message = "Country is required.")
    private String country;


    @NotBlank(message = "State is required.")
    private String state;


    @NotBlank(message = "City is required.")
    private String city;

    @NotBlank(message = "Area is required.")
    private String area;


    @NotBlank(message = "PinCode is required.")
    @Pattern(
            regexp = "^[1-9][0-9]{5}$",
            message = "Please enter a valid 6-digit PIN code.")
    private String pincode;

    @NotNull(message = "Latitude is required.")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @NotNull(message = "Geofence radius is required.")
    private Integer geofenceRadiusMeters;

}