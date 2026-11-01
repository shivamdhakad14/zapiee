package com.zapiee.tableservice.controller;

import com.zapiee.tableservice.dto.ApiResponse;
import com.zapiee.tableservice.dto.request.TableResolveRequest;
import com.zapiee.tableservice.dto.response.GenerateTablesResponse;
import com.zapiee.tableservice.dto.response.TableQrResponse;
import com.zapiee.tableservice.dto.response.TableResolveResponse;
import com.zapiee.tableservice.dto.response.TableResponse;
import com.zapiee.tableservice.service.TableInfoService;
import com.zapiee.tableservice.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Table Management",
        description = "APIs for managing restaurant tables and QR codes."
)
public class TableController {


    private static final String RESTAURANT_ID_HEADER = "X-Restaurant-Id";

    private final TableService tableService;
    private final TableInfoService tableInfoService;

    @Operation(summary = "Create Tables By Count",
            description = "Creates tables from table number 1 up to the requested count."
    )
    @PostMapping("/generate/{tableCount}")
    public ResponseEntity<ApiResponse<GenerateTablesResponse>> createTablesByCount(
            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableCount) {

        GenerateTablesResponse response = tableService.createTablesByCount(restaurantId, tableCount);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                                response,
                        "Tables created successfully."));
    }


    @Operation(summary = "Create Single Table",
            description = "Creates a single table for the restaurant."
    )
    @PostMapping("/generate/single-table/{tableNumber}")
    public ResponseEntity<ApiResponse<GenerateTablesResponse>> createSingleTable(
            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableNumber) {

        GenerateTablesResponse response = tableService.createSingleTable(restaurantId, tableNumber);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                                response,
                        "Table created successfully."));
    }


    @Operation(
            summary = "Get Table By Id",
            description = "Returns table details by table ID."
    )
    @GetMapping("/{tableId}")
    public ResponseEntity<ApiResponse<TableResponse>> getTableById(@PathVariable Long tableId) {

        TableResponse response = tableService.getTableById(tableId);

        return ResponseEntity.ok(ApiResponse.success(
                        response, "Table fetched successfully."));
    }


    @Operation(
            summary = "Get Table By Number",
            description = "Returns a table using its table number.")
    @GetMapping("/tableNumber/{tableNumber}")
    public ResponseEntity<ApiResponse<TableResponse>> getTableByNumber(
            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableNumber) {

        TableResponse response = tableService.getTableByNumber(restaurantId, tableNumber);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                "Table fetched successfully."));
    }

    @Operation(
            summary = "Get All Tables",
            description = " Returns all tables belonging to the restaurant with pagination.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TableResponse>>> getAllTables(
            @RequestHeader("X-Restaurant-Id") Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TableResponse> response = tableService.getAllTables(restaurantId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                "Tables fetched successfully."));
    }

    @PostMapping("/resolve")
    public ResponseEntity<TableResolveResponse> resolveTable(@Valid @RequestBody TableResolveRequest request) {
        TableResolveResponse response = tableInfoService.resolveTable(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get Active Tables",
            description = "Returns only active tables belonging to the restaurant.")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<TableResponse>>> getActiveTables(
            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TableResponse> response = tableService.getActiveTables(restaurantId, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                "Active tables fetched successfully."));
    }

    @Operation(
            summary = "Update Table Active Status",
            description = "Activates or deactivates a restaurant table.")
    @PatchMapping("/table/{tableNumber}/active")
    public ResponseEntity<ApiResponse<TableResponse>> updateTableActiveStatus(

            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableNumber,
            @RequestParam Boolean active) {

        TableResponse response = tableService.updateTableActiveStatus(restaurantId, tableNumber, active);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                        active ? "Table activated successfully." : "Table deactivated successfully."));
    }

    @Operation(
            summary = "Delete Table",
            description = "Deletes a restaurant table and its associated QR code."
    )
    @DeleteMapping("/table/{tableNumber}")
    public ResponseEntity<ApiResponse<Void>> deleteTable(

            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableNumber) {

        tableService.deleteTable(restaurantId, tableNumber);

        return ResponseEntity.ok(ApiResponse.success(
                        null,
                        "Table deleted successfully."));
    }



    @Operation(
            summary = "Regenerate Table QR Code",
            description = " Regenerates the QR code for a specific table by tableNumber"
    )
    @PostMapping("/table/{tableNumber}/qr/regenerate")
    public ResponseEntity<ApiResponse<TableQrResponse>> regenerateQr(
            @RequestHeader(RESTAURANT_ID_HEADER) Long restaurantId,
            @PathVariable Long tableNumber) {

        TableQrResponse response = tableService.regenerateQr(restaurantId, tableNumber);

        return ResponseEntity.ok(ApiResponse.success(
                        response,
                "QR code regenerated successfully."));
    }
}

