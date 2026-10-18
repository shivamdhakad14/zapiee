package com.zapiee.tableservice.controller;

import com.zapiee.tableservice.dto.ApiResponse;
import com.zapiee.tableservice.dto.request.LocationValidationRequest;
import com.zapiee.tableservice.dto.response.LocationValidationResponse;
import com.zapiee.tableservice.service.LocationValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
@Tag(
        name = "Table Location",
        description = "Customer location validation for restaurant QR access."
)
public class TableLocationController {

    private final LocationValidationService locationValidationService;

    @Operation(
            summary = "Validate Customer Location",
            description = "Validates whether the customer is within the allowedcdistance of the restaurant associated with the scanned QR code."
    )
    @PostMapping("/validate-location")
    public ResponseEntity<ApiResponse<LocationValidationResponse>> validateCustomerLocation
            (@Valid @RequestBody LocationValidationRequest request) {

        LocationValidationResponse response = locationValidationService.validateCustomerLocation(request);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                        response.getAllowed()
                                ? "Customer location validated successfully."
                                : "Customer is outside the restaurant area."));
    }
}