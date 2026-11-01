package com.zapiee.menu_service.controller;

import com.zapiee.menu_service.dto.request.ApproveStagedItemsRequest;
import com.zapiee.menu_service.dto.request.UpdateStagedItemRequest;
import com.zapiee.menu_service.dto.response.MenuImportJobResponse;
import com.zapiee.menu_service.dto.response.StagedItemResponse;
import com.zapiee.menu_service.service.MenuImportJobService;
import com.zapiee.menu_service.service.MenuImportReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu/bulk-import")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MenuBulkImportController {

    private final MenuImportJobService jobService;
    private final MenuImportReviewService reviewService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MenuImportJobResponse> upload(
            @RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
            @RequestHeader(name = "X-User-Id") Long userId,
            @RequestParam("images") List<MultipartFile> images) {

        MenuImportJobResponse response = jobService.createJob(restaurantId, images, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<MenuImportJobResponse> getStatus(@RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
                                                           @PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.getStatus(jobId, restaurantId));
    }

    @GetMapping("/{jobId}/staged-items")
    public ResponseEntity<Page<StagedItemResponse>> getStagedItems(
            @RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(reviewService.getStagedItems(jobId, restaurantId, pageable));
    }

    @PatchMapping("/{jobId}/staged-items/{stagedItemId}")
    public ResponseEntity<StagedItemResponse> updateStagedItem(
            @RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
            @PathVariable Long jobId,
            @PathVariable Long stagedItemId,
            @Valid @RequestBody UpdateStagedItemRequest request) {

        return ResponseEntity.ok(reviewService.updateStagedItem(stagedItemId, jobId, restaurantId, request));
    }

    @PostMapping("/{jobId}/approve")
    public ResponseEntity<Void> approve(
            @RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
            @RequestHeader(name = "X-User-Id") Long userId,
            @PathVariable Long jobId,
            @Valid @RequestBody ApproveStagedItemsRequest request) {

        reviewService.approveItems(jobId, restaurantId, request.getStagedItemIds(), userId, "");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{jobId}/reject")
    public ResponseEntity<Void> reject(
            @RequestHeader(name = "X-Restaurant-Id") Long restaurantId,
            @PathVariable Long jobId,
            @Valid @RequestBody ApproveStagedItemsRequest request) {

        reviewService.rejectItems(jobId, restaurantId, request.getStagedItemIds());
        return ResponseEntity.ok().build();
    }
}