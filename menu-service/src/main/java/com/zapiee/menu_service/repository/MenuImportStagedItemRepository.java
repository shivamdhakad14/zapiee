package com.zapiee.menu_service.repository;

import com.zapiee.menu_service.entity.MenuImportStagedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuImportStagedItemRepository extends JpaRepository<MenuImportStagedItem, Long> {
    List<MenuImportStagedItem> findByImportJobId(Long importJobId);
    Page<MenuImportStagedItem> findByImportJobId(Long importJobId, Pageable pageable);
    long countByImportJobId(Long importJobId);
    long countByImportJobIdAndReviewStatus(Long importJobId, MenuImportStagedItem.ReviewStatus status);
}