package com.zapiee.tableservice.service;

import com.zapiee.tableservice.client.RestaurantClient;
import com.zapiee.tableservice.config.LocationProperties;
import com.zapiee.tableservice.dto.request.LocationValidationRequest;
import com.zapiee.tableservice.dto.response.LocationValidationResponse;
import com.zapiee.tableservice.dto.response.RestaurantInfoResponse;
import com.zapiee.tableservice.enums.QrStatus;
import com.zapiee.tableservice.exception.BadRequestException;
import com.zapiee.tableservice.exception.QrNotFoundException;
import com.zapiee.tableservice.exception.TableInactiveException;
import com.zapiee.tableservice.model.RestaurantTable;
import com.zapiee.tableservice.model.TableQr;
import com.zapiee.tableservice.repository.RestaurantTableRepository;
import com.zapiee.tableservice.repository.TableQrRepository;
import com.zapiee.tableservice.util.GeoLocationUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationValidationService {

    private final TableQrRepository tableQrRepository;
    private final RestaurantTableRepository tableRepository;
    private final RestaurantClient restaurantClient;
    private final GeoLocationUtil geoLocationUtil;
    private final LocationProperties locationProperties;


    @Transactional
    public LocationValidationResponse validateCustomerLocation(LocationValidationRequest request) {

        validateRequest(request);

        TableQr qr = tableQrRepository.findByQrTokenAndStatus(request.getQrToken(), QrStatus.ACTIVE)
                .orElseThrow(() -> new QrNotFoundException("Active QR code not found."));

        RestaurantTable table = qr.getRestaurantTable();

        if (table == null) {
            throw new QrNotFoundException("Table associated with QR code was not found.");
        }

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new TableInactiveException("This table is currently inactive.");
        }

        Long restaurantId = table.getRestaurantId();

        if (restaurantId == null) {
            throw new BadRequestException("Restaurant ID is missing for this table.");
        }

        RestaurantInfoResponse restaurant = restaurantClient.restaurantById(restaurantId);

        if (restaurant == null) {
            throw new BadRequestException("Restaurant information could not be retrieved.");
        }

        validateRestaurantLocation(restaurant);

        BigDecimal distanceMeters = geoLocationUtil.calculateDistanceMeters(
                        request.getLatitude(),
                        request.getLongitude(),
                        restaurant.getLatitude(),
                        restaurant.getLongitude());

        BigDecimal allowedRadius = locationProperties.getAllowedRadiusMeters();

        boolean allowed = distanceMeters.compareTo(allowedRadius) <= 0;


        log.info("Location validation completed. " +
                        "restaurantId={}, tableId={}, distanceMeters={}, allowed={}",
                restaurantId, table.getId(),
                distanceMeters, allowed);

        if (allowed) {
            return LocationValidationResponse
                    .builder()
                    .allowed(true)
                    .distanceMeters(distanceMeters)
                    .allowedRadiusMeters(allowedRadius)
                    .message("Customer is within the restaurant area.")
                    .build();
        }


        return LocationValidationResponse
                .builder()
                .allowed(false)
                .distanceMeters(distanceMeters)
                .allowedRadiusMeters(allowedRadius)
                .message("You must be near the restaurant " + "to access the menu.")
                .build();
    }

    private void validateRequest(LocationValidationRequest request) {

        if (request == null) {
            throw new BadRequestException("Location validation request is required.");
        }

        if (request.getQrToken() == null || request.getQrToken().isBlank()) {
            throw new BadRequestException("QR token is required.");
        }

        if (request.getLatitude() == null) {
            throw new BadRequestException("Customer latitude is required.");
        }

        if (request.getLongitude() == null) {
            throw new BadRequestException("Customer longitude is required.");
        }

        if (request.getAccuracy() == null) {
            throw new BadRequestException("GPS accuracy is required.");
        }

        if (request.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 ||
                request.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Latitude must be between -90 and 90.");
        }

        if (request.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 ||
                request.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Longitude must be between -180 and 180.");
        }

        if (!Double.isFinite(request.getAccuracy())) {
            throw new BadRequestException("GPS accuracy must be a valid number.");
        }

        if (request.getAccuracy() <= 0) {
            throw new BadRequestException("GPS accuracy must be greater than zero.");
        }

        Double maxAccuracy = locationProperties.getMaxGpsAccuracyMeters();

        if (request.getAccuracy() > maxAccuracy) {
            throw new BadRequestException(
                    "GPS accuracy is too low. " + "Please enable precise location and try again.");
        }
    }


    private void validateRestaurantLocation(RestaurantInfoResponse location) {

        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new BadRequestException("Restaurant location is not configured.");
        }

        if (location.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0 ||
                location.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Restaurant latitude is invalid.");
        }

        if (location.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0 ||
                location.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Restaurant longitude is invalid.");
        }
    }

}