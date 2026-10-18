package com.zapiee.subscriptionservice.service;

import com.zapiee.subscriptionservice.dto.PlanFeatures;
import com.zapiee.subscriptionservice.dto.request.CreatePlanRequest;
import com.zapiee.subscriptionservice.dto.response.PlanResponse;
import com.zapiee.subscriptionservice.exception.ResourceNotFoundException;
import com.zapiee.subscriptionservice.model.Plan;
import com.zapiee.subscriptionservice.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPlanService {

    private final PlanRepository planRepository;
    private final PlanService planService;

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public PlanResponse createPlan(CreatePlanRequest request) {
        if (planRepository.existsById(request.getId())) {
            throw new RuntimeException("Plan with id '" + request.getId() + "' already exists" +
                    "PLAN_EXISTS");
        }

        PlanFeatures features = request.getPlanFeatures();

        Plan plan = Plan.builder()
                .id(request.getId())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .amountPaise(request.getAmountPaise())
                .billingInterval(request.getInterval())
                .billingIntervalCount(request.getIntervalCount())
                .trialDays(request.getTrialDays())
                .features(features)
                .build();

        planService.createPlanInRazorpayAndSave(plan);
        log.info("Admin created new plan: {}", request);
        return planService.toPlanResponse(plan);
    }

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public void deactivatePlan(String planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        plan.setActive(false);
        planRepository.save(plan);
        log.info("Admin deactivated plan: {}", planId);
    }

    @Transactional
    @CacheEvict(value = "plans", allEntries = true)
    public void reactivatePlan(String planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));
        plan.setActive(true);
        planRepository.save(plan);
        log.info("Admin reactivated plan: {}", planId);
    }

    public List<PlanResponse> getAllPlansIncludingInactive() {
        return planRepository.findAll().stream()
                .map(planService::toPlanResponse)
                .toList();
    }

}