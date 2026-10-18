package com.zapiee.tableservice.service;

import com.zapiee.tableservice.client.RestaurantClient;
import com.zapiee.tableservice.config.LocationProperties;
import com.zapiee.tableservice.dto.request.TableResolveRequest;
import com.zapiee.tableservice.dto.response.RestaurantInfoResponse;
import com.zapiee.tableservice.dto.response.TableResolveResponse;
import com.zapiee.tableservice.enums.QrStatus;
import com.zapiee.tableservice.exception.BadRequestException;
import com.zapiee.tableservice.exception.QrNotFoundException;
import com.zapiee.tableservice.exception.TableInactiveException;
import com.zapiee.tableservice.exception.TableNotFoundException;
import com.zapiee.tableservice.model.RestaurantTable;
import com.zapiee.tableservice.model.TableQr;
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
public class TableInfoService {

    private final TableQrRepository tableQrRepository;
    private final TableSessionService  tableSessionService;
    private final RestaurantClient restaurantClient;
    private final GeoLocationUtil geoLocationUtil;
    private final LocationProperties locationProperties;

    @Transactional
    public TableResolveResponse resolveTable(TableResolveRequest request) {

        validateResolveRequest(request);

        TableQr qr = tableQrRepository.findByQrTokenAndStatus(request.getQrToken(), QrStatus.ACTIVE)
                .orElseThrow(() -> new QrNotFoundException("Active QR code not found."));

        RestaurantTable table = qr.getRestaurantTable();

        if (table == null) {
            throw new TableNotFoundException("Table associated with QR code was not found.");
        }

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new TableInactiveException("This table is currently inactive.");
        }

        Long restaurantId = table.getRestaurantId();

        if (restaurantId == null || restaurantId <= 0) {
            throw new BadRequestException("Restaurant ID is missing or invalid for this table.");
        }

        RestaurantInfoResponse restaurant = restaurantClient.restaurantById(restaurantId);

        if (restaurant == null) {
            throw new BadRequestException("Restaurant information could not be retrieved.");
        }

        validateRestaurantLocation(restaurant);

        BigDecimal distanceMeters = geoLocationUtil.calculateDistanceMeters(
                        request.getLatitude(), request.getLongitude(),
                        restaurant.getLatitude(), restaurant.getLongitude());

        BigDecimal allowedRadius = locationProperties.getAllowedRadiusMeters();

        if (allowedRadius == null || allowedRadius.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Location validation radius is not configured correctly.");
        }

        if (distanceMeters.compareTo(allowedRadius) > 0) {

            log.warn("Table resolve rejected due to customer location. " +
                            "restaurantId={}, tableId={}, distanceMeters={}, allowedRadius={}",
                    restaurantId, table.getId(), distanceMeters, allowedRadius);

            throw new BadRequestException("You must be near the restaurant to access this table.");
        }

        TableResolveResponse sessionResponse = tableSessionService.createSession(restaurantId, table.getId(), table.getTableNumber());

        if (sessionResponse == null || sessionResponse.getSessionToken() == null
                || sessionResponse.getSessionToken().isBlank()) {
            throw new BadRequestException("Unable to create or resolve table session.");
        }

        log.info("Table resolved successfully. " + "restaurantId={}, tableId={}, distanceMeters={}, sessionExpiresAt={}",
                restaurantId, table.getId(), distanceMeters, sessionResponse.getSessionExpiresAt());

        return TableResolveResponse.builder()
                .restaurantId(restaurantId)
                .tableId(table.getId())
                .sessionToken(sessionResponse.getSessionToken())
                .sessionExpiresAt(sessionResponse.getSessionExpiresAt())
                .build();
    }



    private void validateResolveRequest(TableResolveRequest request) {

        if (request == null) {
            throw new BadRequestException("Table resolve request is required.");
        }

        if (request.getQrToken() == null || request.getQrToken().isBlank()) {
            throw new BadRequestException("QR token is required.");
        }

        if (request.getLatitude() == null) {
            throw new BadRequestException("Latitude is required.");
        }

        if (request.getLongitude() == null) {
            throw new BadRequestException("Longitude is required.");
        }

        if (request.getAccuracyMeters() == null) {
            throw new BadRequestException("Accuracy is required.");
        }

        if (request.getAccuracyMeters() <= 0) {
            throw new BadRequestException("Accuracy must be greater than zero.");
        }

        if (request.getLatitude().compareTo(BigDecimal.valueOf(-90)) < 0
                || request.getLatitude().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new BadRequestException("Latitude must be between -90 and 90.");
        }

        if (request.getLongitude().compareTo(BigDecimal.valueOf(-180)) < 0
                || request.getLongitude().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BadRequestException("Longitude must be between -180 and 180.");
        }

        Double maxAccuracy = locationProperties.getMaxGpsAccuracyMeters();

        if (maxAccuracy != null && request.getAccuracyMeters() > maxAccuracy) {
            throw new BadRequestException("GPS accuracy is too low. " +
                            "Please enable precise location and try again.");
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
