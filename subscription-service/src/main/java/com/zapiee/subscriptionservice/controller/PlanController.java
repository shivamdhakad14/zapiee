package com.zapiee.subscriptionservice.controller;

import com.zapiee.subscriptionservice.dto.request.CreatePlanRequest;
import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import com.zapiee.subscriptionservice.dto.response.PlanResponse;
import com.zapiee.subscriptionservice.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success(planService.getActivePlans()));
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlan(@PathVariable String planId) {
        return ResponseEntity.ok(ApiResponse.success(planService.toPlanResponse(planService.getPlanById(planId))));
    }
}
