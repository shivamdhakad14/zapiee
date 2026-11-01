package com.zapiee.menu_service.service;

import com.zapiee.menu_service.dto.request.CreateCategoryRequest;
import com.zapiee.menu_service.dto.request.CreateMenuItemRequest;
import com.zapiee.menu_service.dto.request.UpdateStagedItemRequest;
import com.zapiee.menu_service.dto.response.StagedItemResponse;
import com.zapiee.menu_service.entity.*;
import com.zapiee.menu_service.exception.BusinessRuleViolationException;
import com.zapiee.menu_service.exception.ResourceNotFoundException;
import com.zapiee.menu_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuImportReviewService {

    private final MenuImportJobRepository jobRepository;
    private final MenuImportStagedItemRepository stagedItemRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final MenuItemService menuItemService;

    private static final double MIN_AUTO_APPROVABLE_CONFIDENCE = 0.6;

    public Page<StagedItemResponse> getStagedItems(Long jobId, Long restaurantId, Pageable pageable) {
        validateJobOwnership(jobId, restaurantId);
        return stagedItemRepository.findByImportJobId(jobId, pageable).map(this::toResponse);
    }

    @Transactional
    public StagedItemResponse updateStagedItem(Long stagedItemId, Long jobId, Long restaurantId,
                                               UpdateStagedItemRequest request) {
        validateJobOwnership(jobId, restaurantId);

        MenuImportStagedItem item = stagedItemRepository.findById(stagedItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Staged item not found"));

        if (!item.getImportJobId().equals(jobId)) {
            throw new BusinessRuleViolationException("Staged item does not belong to this job", "INVALID_STAGED_ITEM");
        }

        if (request.getCategoryName() != null) item.setFinalCategoryName(request.getCategoryName().trim());
        if (request.getItemName() != null) item.setFinalItemName(request.getItemName().trim());
        if (request.getDescription() != null) item.setFinalDescription(request.getDescription().trim());
        if (request.getPrice() != null) item.setFinalPrice(request.getPrice());
        if (request.getIsVeg() != null) item.setFinalIsVeg(request.getIsVeg());

        item.setReviewStatus(MenuImportStagedItem.ReviewStatus.EDITED);
        return toResponse(stagedItemRepository.save(item));
    }

    @Transactional
    public void rejectItems(Long jobId, Long restaurantId, List<Long> stagedItemIds) {
        validateJobOwnership(jobId, restaurantId);
        List<MenuImportStagedItem> items = stagedItemRepository.findAllById(stagedItemIds);
        for (MenuImportStagedItem item : items) {
            item.setReviewStatus(MenuImportStagedItem.ReviewStatus.REJECTED);
        }
        stagedItemRepository.saveAll(items);
        refreshJobCompletion(jobId);
    }

    @Transactional
    public void approveItems(Long jobId, Long restaurantId, List<Long> stagedItemIds,
                             Long actorId, String actorName) {

        MenuImportJob job = validateJobOwnership(jobId, restaurantId);

        Map<String, Long> categoryNameToId = new HashMap<>();
        List<MenuImportStagedItem> items = stagedItemRepository.findAllById(stagedItemIds);

        for (MenuImportStagedItem staged : items) {
            if (!staged.getImportJobId().equals(jobId)) {
                continue; // silently skip cross-job IDs rather than fail the whole batch
            }
            if (staged.getReviewStatus() == MenuImportStagedItem.ReviewStatus.APPROVED
                    || staged.getReviewStatus() == MenuImportStagedItem.ReviewStatus.REJECTED) {
                continue; // already finalized
            }

            try {
                validateApprovable(staged);

                Long categoryId = resolveOrCreateCategory(
                        staged.getFinalCategoryName(), restaurantId, categoryNameToId, actorId, actorName);

                CreateMenuItemRequest request = new CreateMenuItemRequest();
                request.setCategoryId(categoryId);
                request.setName(staged.getFinalItemName());
                request.setDescription(staged.getFinalDescription());
                request.setPrice(staged.getFinalPrice());
                request.setTaxRate(java.math.BigDecimal.ZERO);
                request.setIsVeg(staged.getFinalIsVeg() != null ? staged.getFinalIsVeg() : true);

                menuItemService.createMenuItem(request, restaurantId);
                staged.setReviewStatus(MenuImportStagedItem.ReviewStatus.APPROVED);

            } catch (Exception ex) {
                staged.setReviewStatus(MenuImportStagedItem.ReviewStatus.REJECTED);
            }

            stagedItemRepository.save(staged);
        }

        refreshJobCompletion(jobId);
    }

    private void validateApprovable(MenuImportStagedItem staged) {
        if (staged.getConfidenceScore() != null && staged.getConfidenceScore() < MIN_AUTO_APPROVABLE_CONFIDENCE) {
            throw new BusinessRuleViolationException(
                    "Low-confidence extraction requires manual confirmation before approval",
                    "LOW_CONFIDENCE_REQUIRES_CONFIRMATION");
        }
        if (staged.getCrossPageDuplicateOfStagedItemId() != null) {
            throw new BusinessRuleViolationException(
                    "This item appears to duplicate another extracted item — resolve the duplicate first",
                    "UNRESOLVED_DUPLICATE");
        }
    }

    private Long resolveOrCreateCategory(String categoryName, Long restaurantId,
                                         Map<String, Long> cache, Long actorId, String actorName) {
        String key = categoryName.trim().toLowerCase();
        if (cache.containsKey(key)) return cache.get(key);

        Long existingId = categoryRepository
                .findByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(restaurantId, categoryName)
                .map(Category::getId)
                .orElse(null);

        Long categoryId;
        if (existingId != null) {
            categoryId = existingId;
        } else {
            CreateCategoryRequest request = new CreateCategoryRequest();
            request.setName(categoryName);
            categoryId = categoryService.createCategory(request, restaurantId).getId();
        }
        cache.put(key, categoryId);
        return categoryId;
    }

    private void refreshJobCompletion(Long jobId) {
        MenuImportJob job = jobRepository.findById(jobId).orElseThrow();

        long approved = stagedItemRepository.countByImportJobIdAndReviewStatus(jobId, MenuImportStagedItem.ReviewStatus.APPROVED);
        long rejected = stagedItemRepository.countByImportJobIdAndReviewStatus(jobId, MenuImportStagedItem.ReviewStatus.REJECTED);
        long total = stagedItemRepository.countByImportJobId(jobId);

        job.setItemsApproved((int) approved);

        if (approved + rejected >= total) {
            job.setStatus(approved > 0 ? MenuImportJob.Status.COMPLETED : MenuImportJob.Status.FAILED);
            job.setCompletedAt(java.time.LocalDateTime.now());
        } else if (approved > 0) {
            job.setStatus(MenuImportJob.Status.PARTIALLY_APPROVED);
        }

        jobRepository.save(job);
    }

    private MenuImportJob validateJobOwnership(Long jobId, Long restaurantId) {
        MenuImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found"));
        if (!job.getRestaurantId().equals(restaurantId)) {
            throw new BusinessRuleViolationException("Import job does not belong to this restaurant", "UNAUTHORIZED_ACCESS");
        }
        return job;
    }

    private StagedItemResponse toResponse(MenuImportStagedItem item) {
        return StagedItemResponse.builder()
                .id(item.getId())
                .sourcePageNumber(item.getSourcePageNumber())
                .categoryName(item.getFinalCategoryName())
                .itemName(item.getFinalItemName())
                .description(item.getFinalDescription())
                .price(item.getFinalPrice())
                .isVeg(item.getFinalIsVeg())
                .confidenceScore(item.getConfidenceScore())
                .lowConfidence(item.getConfidenceScore() != null && item.getConfidenceScore() < MIN_AUTO_APPROVABLE_CONFIDENCE)
                .possibleDuplicateOfItemId(item.getPossibleDuplicateOfItemId())
                .crossPageDuplicateOfStagedItemId(item.getCrossPageDuplicateOfStagedItemId())
                .reviewStatus(item.getReviewStatus())
                .build();
    }
}

