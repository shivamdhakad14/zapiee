package com.zapiee.menu_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class ApproveStagedItemsRequest {
    @NotEmpty(message = "At least one staged item ID is required")
    private List<Long> stagedItemIds;
}
