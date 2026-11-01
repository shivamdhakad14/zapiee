package com.zapiee.menu_service.controller;

import com.zapiee.menu_service.dto.request.TableResolveRequest;
import com.zapiee.menu_service.dto.response.ApiResponse;
import com.zapiee.menu_service.dto.response.PublicMenuResponse;
import com.zapiee.menu_service.service.PublicMenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/public-menu")
@RequiredArgsConstructor
public class CustomerMenuController {

    private final PublicMenuItemService publicMenuItemService;

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<PublicMenuResponse>> getMenu(@Valid @RequestBody TableResolveRequest request) {
        log.info("request received for customer menu fetch for restaurant by id : {}" , request);
        PublicMenuResponse publicMenu = publicMenuItemService.getPublicMenu(request);
        return ResponseEntity.ok(ApiResponse.success(publicMenu));
    }

}
