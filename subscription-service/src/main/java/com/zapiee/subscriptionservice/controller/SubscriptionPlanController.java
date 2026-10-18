package com.zapiee.subscriptionservice.controller;

import com.zapiee.subscriptionservice.dto.request.CreatePlanRequest;
import com.zapiee.subscriptionservice.dto.response.ApiResponse;
import com.zapiee.subscriptionservice.dto.response.PlanResponse;
import com.zapiee.subscriptionservice.service.SubscriptionPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getAllPlans() {
        return ResponseEntity.ok(ApiResponse.success(subscriptionPlanService.getAllPlansIncludingInactive()));
    }

    @PostMapping("/create-plan")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(
            @Valid @RequestBody CreatePlanRequest request) {
        log.info("Create plan api hit");
        PlanResponse plan = subscriptionPlanService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(plan, "Plan created and synced to Razorpay"));
    }

    @PutMapping("/plans/{planId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivatePlan(@PathVariable String planId) {
        subscriptionPlanService.deactivatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success(null, "Plan deactivated. Existing subscribers keep access."));
    }

    @PutMapping("/plans/{planId}/reactivate")
    public ResponseEntity<ApiResponse<Void>> reactivatePlan(@PathVariable String planId) {
        subscriptionPlanService.reactivatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success(null, "Plan reactivated."));
    }

}
