package com.zapiee.restaurantservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant_link_accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RestaurantLinkAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Razorpay Route linked account ID.
     * Format: acc_xxxxxxxxxx
     * Created once via POST /v1/accounts (Razorpay Route API).
     * Used by Payment Service to route customer payments directly here.
     * NULL = restaurant has not completed linked account setup.
     */
    @Column(name = "razorpay_linked_account_id")
    private String razorpayLinkedAccountId;

    /**
     * KYC/activation status of the linked account.
     * PENDING             = account created, KYC email sent, awaiting owner action
     * ACTIVATED           = KYC complete, can receive Route transfers
     * NEEDS_CLARIFICATION = Razorpay requested more documents
     * SUSPENDED           = account suspended by Razorpay
     */
    @Column(name = "linked_account_kyc_status")
    @Builder.Default
    private String linkedAccountKycStatus = "NOT_CREATED";

    /** Razorpay stakeholder ID (KYC individual) */
    @Column(name = "razorpay_stakeholder_id")
    private String razorpayStakeholderId;

    @Builder.Default
    @Column(name = "platform_commission_percent", precision = 5, scale = 2)
    private BigDecimal platformCommissionPercent = BigDecimal.valueOf(0.00);

    /** Masked bank account number for display: XXXX1234 */
    @Column(name = "bank_account_last4")
    private String bankAccountLast4;

    @Column(name = "linked_account_created_at")
    private LocalDateTime linkedAccountCreatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
