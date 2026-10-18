package com.zapiee.subscriptionservice.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.zapiee.subscriptionservice.config.RazorpayConfig;
import com.zapiee.subscriptionservice.dto.PlanFeatures;
import com.zapiee.subscriptionservice.dto.response.PlanResponse;
import com.zapiee.subscriptionservice.model.Plan;
import com.zapiee.subscriptionservice.repository.PlanRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PlanRepository planRepository;

    @Transactional
    public void createPlanInRazorpayAndSave(Plan request) {

        try {
            JSONObject item = new JSONObject();
            item.put("name", request.getDisplayName());
            item.put("description", request.getDescription() != null ? request.getDescription() : request.getDisplayName());
            item.put("amount", request.getAmountPaise());
            item.put("currency", "INR");

            JSONObject planData = new JSONObject();
            planData.put("period", request.getBillingInterval().name().toLowerCase());
            planData.put("interval", request.getBillingIntervalCount());
            planData.put("item", item);

            // 3. Add Plan Features as Notes
            PlanFeatures features = request.getFeatures();
            JSONObject notes = new JSONObject();
            notes.put("plan_id", request.getId());
            notes.put("max_categories", features.getMaxCategories());
            notes.put("max_menu_items_per_category", features.getMaxMenuItemsPerCategory());
            notes.put("max_menu_items_total", features.getMaxMenuItemsTotal());
            notes.put("bulk_menu_import_enabled", features.isBulkMenuImportEnabled());
            notes.put("max_tables", features.getMaxTables());
            notes.put("qr_rotation_enabled", features.isQrRotationEnabled());
            notes.put("geofence_min_radius_meters", features.getGeofenceMinRadiusMeters());
            notes.put("max_monthly_orders", features.getMaxMonthlyOrders());
            notes.put("manual_staff_orders_enabled", features.isManualStaffOrdersEnabled());
            notes.put("max_staff_accounts", features.getMaxStaffAccounts());
            notes.put("sub_role_granularity_enabled", features.isSubRoleGranularityEnabled());
            notes.put("analytics_enabled", features.isAnalyticsEnabled());
            notes.put("trial_days", request.getTrialDays());
            planData.put("notes", notes);

            // 4. Create Plan in Razorpay
            com.razorpay.Plan razorpayPlan =
                    razorpayClient.plans.create(planData);

            String razorpayPlanId =
                    razorpayPlan.get("id");
            request.setRazorpayPlanId(razorpayPlanId);
            planRepository.save(request);

            log.info("Created Razorpay plan and saved in DB: {} -> {}", request.getId(), razorpayPlanId);

        } catch (RazorpayException e) {

            log.error("Failed to create Razorpay plan {}: {}", request.getId(), e.getMessage(), e);

            throw new RuntimeException("Failed to create Razorpay plan " + request.getId() + ": " + e.getMessage(), e);
        }
    }

    public List<PlanResponse> getActivePlans() {
        return planRepository.findByActiveTrueOrderByAmountPaiseAsc()
                .stream()
                .map(this::toPlanResponse)
                .toList();
    }

    public Plan getPlanById(String planId) {
        return planRepository.findById(planId)
                .filter(Plan::getActive)
                .orElseThrow(() -> new RuntimeException(
                        "Plan not found or inactive: " + planId));
    }

    public PlanResponse toPlanResponse(Plan plan) {

        return PlanResponse.builder()
                .id(plan.getId())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .amountMonthly(BigDecimal.valueOf(plan.getAmountPaise()).divide(BigDecimal.valueOf(100)))
                .interval(plan.getBillingInterval())
                .trialDays(plan.getTrialDays())
                .planFeatures(plan.getFeatures())
                .build();
    }
}
