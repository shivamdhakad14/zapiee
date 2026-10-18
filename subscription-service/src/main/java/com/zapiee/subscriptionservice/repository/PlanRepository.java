package com.zapiee.subscriptionservice.repository;

import com.zapiee.subscriptionservice.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, String> {
    List<Plan> findByActiveTrueOrderByAmountPaiseAsc();
    Optional<Plan> findByRazorpayPlanId(String razorpayPlanId);
    boolean existsByRazorpayPlanId(String razorpayPlanId);
}
