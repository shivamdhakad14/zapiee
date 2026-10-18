package com.zapiee.menu_service.service;

import com.zapiee.menu_service.entity.MenuImportJob;
import com.zapiee.menu_service.entity.MenuImportJobPage;
import com.zapiee.menu_service.entity.MenuImportStagedItem;
import com.zapiee.menu_service.entity.MenuItem;
import com.zapiee.menu_service.repository.MenuImportJobPageRepository;
import com.zapiee.menu_service.repository.MenuImportJobRepository;
import com.zapiee.menu_service.repository.MenuImportStagedItemRepository;
import com.zapiee.menu_service.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuPageExtractionWorker {

    private final MenuImportJobRepository jobRepository;
    private final MenuImportJobPageRepository pageRepository;
    private final MenuImportStagedItemRepository stagedItemRepository;
    private final MenuImageExtractionService extractionService;
    private final MenuImportDedupService dedupService;
    private final MenuItemRepository menuItemRepository;

    @Async
    public void processAllPages(Long jobId) {
        markJobProcessing(jobId);
        List<MenuImportJobPage> pages = pageRepository.findByImportJobId(jobId);
        for (MenuImportJobPage page : pages) {
            processSinglePage(jobId, page.getId());
        }
    }

    @Transactional
    protected void markJobProcessing(Long jobId) {
        MenuImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Menu import job not found: " + jobId
                        )
                );
        job.setStatus(MenuImportJob.Status.PROCESSING);
        jobRepository.save(job);
    }

    public void processSinglePage(Long jobId, Long pageId) {
        MenuImportJobPage page = pageRepository.findById(pageId).orElseThrow();
        page.setStatus(MenuImportJobPage.PageStatus.PROCESSING);
        pageRepository.save(page);

        boolean succeeded;
        try {
            var extracted = extractionService.extractFromImage(page.getImageUrl());

            Long restaurantId = jobRepository.findById(jobId)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Menu import job not found: " + jobId
                            ))
                    .getRestaurantId();

            for (var item : extracted) {

                Long duplicateId = menuItemRepository
                        .findFirstByRestaurantIdAndNameIgnoreCaseAndIsDeletedFalse(
                                restaurantId,
                                item.getName().trim()
                        )
                        .map(MenuItem::getId)
                        .orElse(null);

                stagedItemRepository.save(
                        MenuImportStagedItem.builder()
                                .importJobId(jobId)
                                .sourcePageNumber(page.getPageNumber())
                                .rawCategoryName(item.getCategory())
                                .rawItemName(item.getName())
                                .rawDescription(item.getDescription())
                                .rawPrice(item.getPrice())
                                .rawIsVeg(item.getIsVeg())
                                .confidenceScore(item.getConfidence())
                                .finalCategoryName(item.getCategory())
                                .finalItemName(item.getName())
                                .finalDescription(item.getDescription())
                                .finalPrice(item.getPrice())
                                .finalIsVeg(item.getIsVeg())
                                .possibleDuplicateOfItemId(duplicateId)
                                .reviewStatus(
                                        MenuImportStagedItem.ReviewStatus.PENDING
                                )
                                .build()
                );
            }

            page.setStatus(MenuImportJobPage.PageStatus.PROCESSED);
            page.setItemsExtracted(extracted.size());
            succeeded = true;

        } catch (Exception ex) {
            log.error("menu extraction failed for : " , ex);
            page.setStatus(MenuImportJobPage.PageStatus.FAILED);
            page.setFailureReason(ex.getMessage().length() > 500 ? "menu extraction failed for internal" +
                                                                   " server error you can try again later" : ex.getMessage());
            succeeded = false;
        }
        pageRepository.save(page);

        onPageComplete(jobId, succeeded);
    }

    private void onPageComplete(Long jobId, boolean succeeded) {
        int attempts = 0;
        while (attempts < 5) {
            try {
                incrementJobCounters(jobId, succeeded);
                return;
            } catch (OptimisticLockingFailureException e) {
                attempts++;
            }
        }
        log.error("Failed to update completion counters for import job {} after 5 attempts — needs manual check", jobId);
    }

    @Transactional
    protected void incrementJobCounters(Long jobId, boolean succeeded) {
        MenuImportJob job = jobRepository.findById(jobId).orElseThrow();
        if (succeeded) {
            job.setPagesCompleted(job.getPagesCompleted() + 1);
        } else {
            job.setPagesFailed(job.getPagesFailed() + 1);
        }

        int totalDone = job.getPagesCompleted() + job.getPagesFailed();
        if (totalDone >= job.getTotalPages()) {
            finalizeJob(job);
        }
        jobRepository.save(job);
    }

    private void finalizeJob(MenuImportJob job) {
        if (job.getPagesCompleted() == 0) {
            job.setStatus(MenuImportJob.Status.FAILED);
            job.setFailureReason("All pages failed extraction");
            return;
        }

        dedupService.flagCrossPageDuplicates(job.getId());

        long totalExtracted = stagedItemRepository.countByImportJobId(job.getId());
        job.setStatus(MenuImportJob.Status.READY_FOR_REVIEW);
        job.setTotalItemsExtracted((int) totalExtracted);
        job.setFailureReason(job.getPagesFailed() > 0
                ? job.getPagesFailed() + " of " + job.getTotalPages() + " page(s) failed extraction"
                : null);
    }
}
