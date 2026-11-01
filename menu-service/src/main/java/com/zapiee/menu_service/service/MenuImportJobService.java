package com.zapiee.menu_service.service;

import com.zapiee.menu_service.client.FileUploadClient;
import com.zapiee.menu_service.dto.response.MenuImagesUploadResponse;
import com.zapiee.menu_service.dto.response.MenuImportJobResponse;
import com.zapiee.menu_service.entity.MenuImportJob;
import com.zapiee.menu_service.entity.MenuImportJobPage;
import com.zapiee.menu_service.repository.MenuImportJobPageRepository;
import com.zapiee.menu_service.repository.MenuImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuImportJobService {

    private final MenuImportJobRepository jobRepository;
    private final MenuImportJobPageRepository pageRepository;
    private final FileUploadClient fileStorageClient;
    private final MenuPageExtractionWorker pageExtractionWorker;

    @Transactional
    public MenuImportJobResponse createJob(Long restaurantId, List<MultipartFile> images, Long requestedById) {

        MenuImportJob job = MenuImportJob.builder()
                .restaurantId(restaurantId)
                .status(MenuImportJob.Status.QUEUED)
                .totalPages(images.size())
                .requestedById(requestedById)
                .build();

        MenuImportJob save = jobRepository.save(job);

        for (int i = 0; i < images.size(); i++) {
            MenuImagesUploadResponse url = fileStorageClient.uploadFullMenuImage(save.getId(), restaurantId, images.get(i));
            pageRepository.save(MenuImportJobPage.builder()
                    .importJobId(save.getId())
                    .pageNumber(i + 1)
                    .imageUrl(url.getUrl())
                    .status(MenuImportJobPage.PageStatus.QUEUED)
                    .build());
        }

        pageExtractionWorker.processAllPages(save.getId());

        return MenuImportJobResponse.builder()
                .jobId(save.getId())
                .status(MenuImportJob.Status.QUEUED)
                .totalPages(images.size())
                .pagesCompleted(0)
                .pagesFailed(0)
                .build();
    }

    public MenuImportJobResponse getStatus(Long jobId, Long restaurantId) {
        MenuImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.zapiee.menu_service.exception.ResourceNotFoundException("Import job not found"));

        if (!job.getRestaurantId().equals(restaurantId)) {
            throw new com.zapiee.menu_service.exception.BusinessRuleViolationException(
                    "Import job does not belong to this restaurant", "UNAUTHORIZED_ACCESS");
        }

        return MenuImportJobResponse.builder()
                .jobId(job.getId())
                .status(job.getStatus())
                .totalPages(job.getTotalPages())
                .pagesCompleted(job.getPagesCompleted())
                .pagesFailed(job.getPagesFailed())
                .totalItemsExtracted(job.getTotalItemsExtracted())
                .failureReason(job.getFailureReason())
                .build();
    }
}
