package com.zapiee.restaurantservice.controller;

import com.zapiee.restaurantservice.dto.ApiResponse;
import com.zapiee.restaurantservice.dto.Request.RestaurantRegistrationRequest;
import com.zapiee.restaurantservice.dto.Response.RestaurantInfoResponse;
import com.zapiee.restaurantservice.dto.Response.RestaurantResponse;
import com.zapiee.restaurantservice.dto.Response.RestaurantStatusInfo;
import com.zapiee.restaurantservice.enums.DocumentType;
import com.zapiee.restaurantservice.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/restaurants")
@Tag(
        name = "Restaurant Management",
        description = "Restaurant registration and profile management APIs."
)
@SecurityRequirement(name = "Bearer Authentication")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @Operation(
            summary = "Register Restaurant",
            description = "Registers a new restaurant for the authenticated owner.")
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RestaurantResponse>> registerRestaurant(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RestaurantRegistrationRequest request)  {

        log.info("Restaurant registration request received.");
        RestaurantResponse response = restaurantService.registerRestaurant(userId, request);

        log.info("Restaurant registered successfully for ownerId={}", userId);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Restaurant register successfully."));
    }


    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadOwnerDocument(
            @RequestHeader("X-User-Id") Long ownerId,
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestPart("file") MultipartFile file) {

        String documentUrl = restaurantService.uploadOwnerDocument(
                ownerId, restaurantId,
                documentType, file);

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Owner document uploaded successfully.")
                        .data(documentUrl)
                        .build()
        );
    }


    @Operation(
            summary = "Get Restaurant",
            description = "Returns restaurant details of the authenticated owner.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantByOwnerId(@RequestHeader("X-User-Id") Long ownerId) {
        RestaurantResponse response = restaurantService.getRestaurantByOwnerId(ownerId);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Restaurant fetched successfully."));
    }


    @Operation(
            summary = "Get Restaurant By Id",
            description = "Returns restaurant details by restaurant id."
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurantById(@RequestHeader("X-Restaurant-Id") Long restaurantId) {
        RestaurantResponse response = restaurantService.getRestaurantById(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Restaurant fetched successfully."));
    }

    @Operation(
            summary = "Get Restaurant Status",
            description = "Returns the current status of a restaurant by restaurant ID."
    )
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<RestaurantStatusInfo>> getRestaurantStatus(@RequestHeader("X-Restaurant-Id") Long restaurantId) {
        RestaurantStatusInfo response = restaurantService.getRestaurantStatus(restaurantId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        response, "Restaurant status fetched successfully."
                ));
    }


    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantInfoResponse> restaurantById(@PathVariable("restaurantId") Long restaurantId) {
        RestaurantInfoResponse response = restaurantService.restaurantById(restaurantId);
        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Get Active Restaurants",
            description = "Returns all active restaurants."
    )
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getActiveRestaurants() {
        List<RestaurantResponse> response = restaurantService.getAllActiveRestaurants();
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Active restaurants fetched successfully."));
    }


    @Operation(
            summary = "Get Inactive Restaurants",
            description = "Returns all inactive restaurants."
    )
    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getInactiveRestaurants() {
        List<RestaurantResponse> response = restaurantService.getAllInactiveRestaurants();
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Active restaurants fetched successfully."));
    }



    @Operation(
            summary = "Get All Restaurants",
            description = "Returns all restaurants."
    )
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getAllRestaurants() {
        List<RestaurantResponse> response = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "All restaurants fetched successfully."));
    }



    @Operation(
            summary = "Activate Restaurant",
            description = "Activates the specified restaurant."
    )
    @PatchMapping("/status/activate")
    public ResponseEntity<ApiResponse<Void>> activateRestaurant(@RequestHeader("X-Restaurant-Id") Long restaurantId) {
        restaurantService.activateRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Restaurant activated successfully."));
    }



    @Operation(
            summary = "Deactivate Restaurant",
            description = "Deactivates the specified restaurant."
    )
    @PatchMapping("/status/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateRestaurant(@RequestHeader("X-Restaurant-Id") Long restaurantId) {
        restaurantService.deactivateRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Restaurant deactivated successfully."));
    }



//    @Operation(
//            summary = "Activate Subscription",
//            description = "Activates the first subscription for a restaurant."
//    )
//    @PatchMapping("/{restaurantId}/subscription/activate")
//    public ResponseEntity<ApiResponse<Void>> activateSubscription(
//            @PathVariable Long restaurantId,
//            @Valid @RequestBody SubscriptionRequest request) {
//
//        restaurantService.updateSubscriptionStatus(restaurantId, request);
//        return ApiResponse.success(
//                HttpStatus.OK,
//                "Subscription activated successfully.",
//                null
//        );
//    }


    @Operation(
            summary = "Update Razorpay Customer",
            description = "Updates the Razorpay customer id."
    )
    @PatchMapping("/razorpay/customer")
    public ResponseEntity<ApiResponse<Void>> updateRazorpayCustomer(@RequestHeader("X-Restaurant-Id") Long restaurantId, @RequestParam String razorpayCustomerId) {
        restaurantService.updateRazorpayCustomerId(restaurantId, razorpayCustomerId);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Razorpay customer updated successfully."));
    }

}