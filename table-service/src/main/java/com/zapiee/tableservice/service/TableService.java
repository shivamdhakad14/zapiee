package com.zapiee.tableservice.service;

import com.zapiee.tableservice.client.RestaurantClient;
import com.zapiee.tableservice.dto.response.*;
import com.zapiee.tableservice.enums.QrStatus;
import com.zapiee.tableservice.enums.TableStatus;
import com.zapiee.tableservice.exception.BadRequestException;
import com.zapiee.tableservice.exception.InvalidTableException;
import com.zapiee.tableservice.exception.QrAlreadyExistsException;
import com.zapiee.tableservice.exception.QrNotFoundException;
import com.zapiee.tableservice.exception.ServiceUnavailableException;
import com.zapiee.tableservice.exception.TableAlreadyExistsException;
import com.zapiee.tableservice.exception.TableInactiveException;
import com.zapiee.tableservice.exception.TableNotFoundException;
import com.zapiee.tableservice.model.RestaurantTable;
import com.zapiee.tableservice.model.TableQr;
import com.zapiee.tableservice.repository.RestaurantTableRepository;
import com.zapiee.tableservice.repository.TableQrRepository;
import com.zapiee.tableservice.util.QrGeneratorUtil;
import com.zapiee.tableservice.util.QrPayloadUtil;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {

    private static final long MAX_TABLE_COUNT_PER_REQUEST = 100;
    private static final long MAX_TABLE_NUMBER = 100_000;

    private final RestaurantTableRepository tableRepository;
    private final TableQrRepository tableQrRepository;
    private final RestaurantClient restaurantClient;
    private final QrPayloadUtil qrPayloadUtil;
    private final QrGeneratorUtil qrGeneratorUtil;

    private final SecureRandom secureRandom = new SecureRandom();


    @Transactional
    public GenerateTablesResponse createTablesByCount(Long restaurantId, Long tableCount) {

        validateRestaurantId(restaurantId);
        validateTableCount(tableCount);

        log.info("Creating tables. restaurantId={}, requestedCount={}", restaurantId, tableCount);

        Long lastTableNumber = tableRepository.findMaxTableNumberByRestaurantId(restaurantId);

        long startTableNumber = lastTableNumber == null ? 1 : lastTableNumber + 1;

        List<TableWithQrResponse> tables = new ArrayList<>();

        for (long i = 0; i < tableCount; i++) {

            long tableNumber = startTableNumber + i;

            validateTableNumber(tableNumber);
            TableWithQrResponse tableResponse = createTableWithQr(restaurantId, tableNumber);

            tables.add(tableResponse);
        }

        log.info("Tables created successfully. restaurantId={}, createdCount={}", restaurantId, tables.size());

        return GenerateTablesResponse.builder()
                .tables(tables)
                .build();
    }


    @Transactional
    public GenerateTablesResponse createSingleTable(Long restaurantId, Long tableNumber) {

        validateRestaurantId(restaurantId);
        validateTableNumber(tableNumber);

        log.info("Creating single table. restaurantId={}, tableNumber={}", restaurantId, tableNumber);

        boolean exists = tableRepository.existsByRestaurantIdAndTableNumber(restaurantId, tableNumber);

        if (exists) {
            throw new TableAlreadyExistsException(
                    "Table number " + tableNumber + " already exists for restaurant " + restaurantId);
        }

        TableWithQrResponse table = createTableWithQr(restaurantId, tableNumber);

        return GenerateTablesResponse.builder()
                .tables(List.of(table))
                .build();
    }


    private TableWithQrResponse createTableWithQr(Long restaurantId, Long tableNumber) {

        RestaurantTable table = RestaurantTable.builder()
                .restaurantId(restaurantId)
                .tableNumber(tableNumber)
                .status(TableStatus.AVAILABLE)
                .active(true)
                .build();

        table = tableRepository.save(table);

        log.info("Table saved. tableId={}, restaurantId={}, tableNumber={}", table.getId(),
                restaurantId, tableNumber);

        TableQr qr = createQrForTable(table);

        return mapToTableWithQrResponse(table, qr);
    }


    private TableQr createQrForTable(RestaurantTable table) {

        if (table == null || table.getId() == null) {
            throw new InvalidTableException("Valid table is required to create QR code.");
        }

        boolean qrExists = tableQrRepository.existsByRestaurantTableId(table.getId());

        if (qrExists) {
            throw new QrAlreadyExistsException("QR code already exists for table " + table.getTableNumber());
        }

        String qrToken = generateUniqueToken();

        String qrPayload = qrPayloadUtil.buildPayload(qrToken);

        String qrImageBase64 = qrGeneratorUtil.generateQrBase64(qrPayload);

        TableQr qr = TableQr.builder()
                .restaurantTable(table)
                .qrToken(qrToken)
                .qrPayload(qrPayload)
                .qrImageBase64(qrImageBase64)
                .status(QrStatus.ACTIVE)
                .generatedAt(LocalDateTime.now())
                .build();

        qr = tableQrRepository.save(qr);

        log.info("QR created successfully. qrId={}, tableId={}, tableNumber={}",
                qr.getId(), table.getId(), table.getTableNumber());

        return qr;
    }


    @Transactional(readOnly = true)
    public TableResponse getTableById(Long tableId) {

        validateId(tableId, "Table ID");

        RestaurantTable table = tableRepository.findById(tableId)
                        .orElseThrow(() -> new TableNotFoundException(
                                "Table not found with id: " + tableId));

        TableQr qr = tableQrRepository
                .findByRestaurantTableId(tableId)
                .orElseThrow(() -> new QrNotFoundException(
                        "QR code not found for table id: " + tableId));

        return mapToTableResponse(table, qr);
    }


    @Transactional(readOnly = true)
    public TableResponse getTableByNumber(Long restaurantId, Long tableNumber) {

        validateRestaurantId(restaurantId);
        validateTableNumber(tableNumber);

        RestaurantTable table = tableRepository
                        .findByRestaurantIdAndTableNumber(restaurantId, tableNumber)
                        .orElseThrow(() -> new TableNotFoundException(
                                "Table number " + tableNumber +
                                        " not found for restaurant " + restaurantId));

        TableQr qr = tableQrRepository .findByRestaurantTableId(table.getId())
                .orElseThrow(() -> new QrNotFoundException( "QR code not found for table " + tableNumber));

        return mapToTableResponse(table, qr);
    }


    @Transactional(readOnly = true)
    public Page<TableResponse> getAllTables(Long restaurantId, Pageable pageable) {

        validateRestaurantId(restaurantId);
        validatePageable(pageable);

        return tableRepository .findAllByRestaurantId(restaurantId, pageable)
                .map(table -> {
                    TableQr qr = tableQrRepository .findByRestaurantTableId(table.getId())
                            .orElse(null);
                    return mapToTableResponse(table, qr); });
    }




    @Transactional(readOnly = true)
    public Page<TableResponse> getActiveTables(Long restaurantId,
            Pageable pageable) {

        validateRestaurantId(restaurantId);
        validatePageable(pageable);

        return tableRepository .findAllByRestaurantIdAndActiveTrue( restaurantId, pageable)
                .map(table -> {
                    TableQr qr = tableQrRepository .findByRestaurantTableId(table.getId())
                            .orElse(null); return mapToTableResponse(table, qr); });
    }



    @Transactional
    public TableResponse updateTableActiveStatus(Long restaurantId, Long tableNumber, Boolean active) {

        validateRestaurantId(restaurantId);
        validateTableNumber(tableNumber);

        if (active == null) {
            throw new BadRequestException("Active status is required.");
        }

        RestaurantTable table = tableRepository
                        .findByRestaurantIdAndTableNumber(restaurantId, tableNumber)
                        .orElseThrow(() -> new TableNotFoundException(
                                "Table number " + tableNumber +
                                        " not found for restaurant " + restaurantId));

        table.setActive(active);

        if (Boolean.TRUE.equals(active)) {
            log.info("Table activated. restaurantId={}, tableNumber={}",
                    restaurantId, tableNumber);

        } else {
            log.info("Table deactivated. restaurantId={}, tableNumber={}",
                    restaurantId, tableNumber);
        }

        RestaurantTable savedTable = tableRepository.save(table);

        TableQr qr = tableQrRepository .findByRestaurantTableId(savedTable.getId())
                .orElseThrow(() -> new QrNotFoundException( "QR code not found for table " + tableNumber));

        return mapToTableResponse(savedTable, qr);
    }


    @Transactional
    public void deleteTable(Long restaurantId, Long tableNumber) {

        validateRestaurantId(restaurantId);
        validateTableNumber(tableNumber);

        RestaurantTable table = tableRepository
                        .findByRestaurantIdAndTableNumber(restaurantId, tableNumber)
                        .orElseThrow(() -> new TableNotFoundException(
                                "Table number " + tableNumber +
                                        " not found for restaurant " + restaurantId));

        tableQrRepository.deleteByRestaurantTableId(table.getId());
        tableRepository.delete(table);

        log.info("Table deleted successfully. restaurantId={}, tableNumber={}",
                restaurantId, tableNumber);
    }



    @Transactional(readOnly = true)
    public TableQrResponse getQrByTableId(Long tableId) {

        validateId(tableId, "Table ID");

        TableQr qr = tableQrRepository
                        .findByRestaurantTableId(tableId)
                        .orElseThrow(() -> new QrNotFoundException(
                                "QR code not found for table id: " + tableId));

        return mapToTableQrResponse(qr);
    }



    @Transactional(readOnly = true)
    public TableQr getQrByToken(String qrToken) {

        if (qrToken == null || qrToken.isBlank()) {
            throw new BadRequestException("QR token is required.");
        }

        TableQr qr = tableQrRepository
                        .findByQrTokenAndStatus(qrToken, QrStatus.ACTIVE)
                        .orElseThrow(() -> new QrNotFoundException(
                                        "Active QR code not found."));

        RestaurantTable table = qr.getRestaurantTable();

        if (table == null) {
            throw new QrNotFoundException("Table associated with QR code was not found.");
        }

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new TableInactiveException("This table is currently inactive.");
        }

        return qr;
    }



    @Transactional
    public TableQrResponse regenerateQr(Long restaurantId, Long tableNumber) {

        validateRestaurantId(restaurantId);
        validateTableNumber(tableNumber);

        RestaurantTable table = tableRepository
                        .findByRestaurantIdAndTableNumber(restaurantId, tableNumber)
                        .orElseThrow(() -> new TableNotFoundException(
                                "Table number " + tableNumber +
                                        " not found for restaurant " + restaurantId));

        if (!Boolean.TRUE.equals(table.getActive())) {
            throw new TableInactiveException(
                    "Cannot generate QR for an inactive table."
            );
        }


        TableQr existingQr = tableQrRepository
                        .findByRestaurantTableId(table.getId())
                        .orElseThrow(() -> new QrNotFoundException(
                                "QR code not found for table " + tableNumber));

        String newQrToken = generateUniqueToken();

        String newQrPayload = qrPayloadUtil.buildPayload(newQrToken);

        String newQrImageBase64 = qrGeneratorUtil.generateQrBase64(newQrPayload);

        existingQr.setQrToken(newQrToken);
        existingQr.setQrPayload(newQrPayload);
        existingQr.setQrImageBase64(newQrImageBase64);
        existingQr.setStatus(QrStatus.ACTIVE);
        existingQr.setGeneratedAt(LocalDateTime.now());
        existingQr.setRevokedAt(null);

        TableQr savedQr = tableQrRepository.save(existingQr);

        log.info("QR regenerated successfully. restaurantId={}, tableId={}, tableNumber={}, qrId={}",
                restaurantId, table.getId(), tableNumber, savedQr.getId());

        return mapToTableQrResponse(savedQr);
    }



    private TableResponse mapToTableResponse(RestaurantTable table, TableQr qr) {

        return TableResponse.builder()
                .tableId(table.getId())
                .restaurantId(table.getRestaurantId())
                .tableNumber(table.getTableNumber())
                .tableStatus(table.getStatus())
                .active(table.getActive())
                .qr(mapToTableQrResponse(qr))
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .build();
    }



    private TableWithQrResponse mapToTableWithQrResponse(RestaurantTable table, TableQr qr) {

        return TableWithQrResponse.builder()
                .tableId(table.getId())
                .restaurantId(table.getRestaurantId())
                .tableNumber(table.getTableNumber())
                .tableStatus(table.getStatus())
                .active(table.getActive())
                .qr(mapToTableQrResponse(qr))
                .build();
    }


    private TableQrResponse mapToTableQrResponse(TableQr qr) {

        return TableQrResponse.builder()
                .qrId(qr.getId())
                .qrToken(qr.getQrToken())
                .qrPayload(qr.getQrPayload())
                .qrImageBase64(qr.getQrImageBase64())
                .status(qr.getStatus())
                .build();
    }



    private void validateRestaurantId(Long restaurantId) {

        if (restaurantId == null) {
            throw new BadRequestException("Restaurant ID is required.");
        }

        if (restaurantId <= 0) {
            throw new BadRequestException("Restaurant ID must be greater than 0.");
        }

        try {

            RestaurantInfoResponse response = restaurantClient.restaurantById(restaurantId);

            if (response == null) {
                throw new ServiceUnavailableException(
                        "Restaurant Service returned an empty response.");
            }

            log.debug("Restaurant validated successfully. restaurantId={}", restaurantId);

        } catch (FeignException.NotFound ex) {

            log.warn("Restaurant not found. restaurantId={}", restaurantId);

            throw new BadRequestException("Restaurant not found with id: " + restaurantId);

        } catch (FeignException.ServiceUnavailable ex) {

            log.error("Restaurant Service unavailable. restaurantId={}",
                    restaurantId, ex);

            throw new ServiceUnavailableException("Restaurant Service is currently unavailable.");

        } catch (FeignException ex) {

            log.error("Failed to validate restaurant. restaurantId={}",
                    restaurantId, ex);

            throw new ServiceUnavailableException("Unable to validate restaurant at the moment.");
        }
    }


    private void validateTableNumber(Long tableNumber) {

        if (tableNumber == null) {
            throw new BadRequestException("Table number is required.");
        }

        if (tableNumber <= 0) {
            throw new BadRequestException("Table number must be greater than 0.");
        }

        if (tableNumber > MAX_TABLE_NUMBER) {
            throw new BadRequestException("Table number cannot be greater than " +
                            MAX_TABLE_NUMBER + ".");
        }
    }


    private void validateTableCount(Long tableCount) {

        if (tableCount == null) {
            throw new BadRequestException("Table count is required.");
        }

        if (tableCount <= 0) {
            throw new BadRequestException("Table count must be greater than 0.");
        }

        if (tableCount > MAX_TABLE_COUNT_PER_REQUEST) {
            throw new BadRequestException(
                    "You can generate maximum " + MAX_TABLE_COUNT_PER_REQUEST + " tables at a time.");
        }
    }


    private void validateId(Long id, String fieldName) {

        if (id == null) {
            throw new BadRequestException(fieldName + " is required.");
        }

        if (id <= 0) {
            throw new BadRequestException(fieldName + " must be greater than 0.");
        }
    }




    private void validatePageable(Pageable pageable) {

        if (pageable == null) {
            throw new BadRequestException("Pagination information is required.");
        }
    }


    private String generateUniqueToken() {

        for (int attempt = 1; attempt <= 10; attempt++) {

            byte[] randomBytes = new byte[24];
            secureRandom.nextBytes(randomBytes);

            String token = Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(randomBytes);

            boolean exists = tableQrRepository.existsByQrToken(token);

            if (!exists) {
                return token;
            }

            log.warn("QR token collision detected. attempt={}", attempt);
        }

        throw new InvalidTableException("Unable to generate a unique QR token. Please try again.");
    }
}