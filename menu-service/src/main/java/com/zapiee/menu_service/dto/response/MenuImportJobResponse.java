package com.zapiee.menu_service.dto.response;


import com.zapiee.menu_service.entity.MenuImportJob;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MenuImportJobResponse {
    private Long jobId;
    private MenuImportJob.Status status;
    private Integer totalPages;
    private Integer pagesCompleted;
    private Integer pagesFailed;
    private Integer totalItemsExtracted;
    private String failureReason;
}