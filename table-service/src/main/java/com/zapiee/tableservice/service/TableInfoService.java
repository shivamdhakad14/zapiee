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

        Double distanceMeters = geoLocationUtil.calculateDistanceMeters(
                        request.getLatitude(), request.getLongitude(),
                        restaurant.getLatitude(), restaurant.getLongitude());

        Double allowedRadius = locationProperties.getAllowedRadiusMeters();

        if (allowedRadius == null || allowedRadius.compareTo(distanceMeters) <= 0) {
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
                restaurantId, table.getId(), distanceMeters, sessionResponse.getExpireAt());

        return TableResolveResponse.builder()
                .restaurantId(restaurantId)
                .tableId(table.getId())
                .sessionToken(sessionResponse.getSessionToken())
                .expireAt(sessionResponse.getExpireAt())
                .build();
    }

}
