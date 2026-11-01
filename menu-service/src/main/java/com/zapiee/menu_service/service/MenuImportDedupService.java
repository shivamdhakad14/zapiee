package com.zapiee.menu_service.service;

import com.zapiee.menu_service.entity.MenuImportStagedItem;
import com.zapiee.menu_service.repository.MenuImportStagedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuImportDedupService {

    private final MenuImportStagedItemRepository stagedItemRepository;

    @Transactional
    public void flagCrossPageDuplicates(Long jobId) {
        List<MenuImportStagedItem> items = stagedItemRepository.findByImportJobId(jobId);

        for (int i = 0; i < items.size(); i++) {
            MenuImportStagedItem a = items.get(i);
            if (a.getCrossPageDuplicateOfStagedItemId() != null) continue;

            for (int j = i + 1; j < items.size(); j++) {
                MenuImportStagedItem b = items.get(j);
                if (b.getCrossPageDuplicateOfStagedItemId() != null) continue;

                if (isLikelyDuplicate(a, b)) {
                    b.setCrossPageDuplicateOfStagedItemId(a.getId());
                    stagedItemRepository.save(b);
                }
            }
        }
    }

    private boolean isLikelyDuplicate(MenuImportStagedItem a, MenuImportStagedItem b) {
        boolean sameName = normalize(a.getRawItemName()).equals(normalize(b.getRawItemName()));
        boolean samePrice = a.getRawPrice() != null && b.getRawPrice() != null
                && a.getRawPrice().subtract(b.getRawPrice()).abs().doubleValue() < 1.0;
        return sameName && samePrice;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
