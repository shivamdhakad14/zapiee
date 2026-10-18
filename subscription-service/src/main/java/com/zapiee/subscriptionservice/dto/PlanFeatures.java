package com.zapiee.subscriptionservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanFeatures {

    // ---- Menu/Catalog quotas ----
    private int maxCategories;
    private int maxMenuItemsPerCategory;
    private int maxMenuItemsTotal;
    private boolean bulkMenuImportEnabled;

    // ---- Table/QR quotas ----
    private int maxTables;
    private boolean qrRotationEnabled;      // scheduled auto-rotation, not just manual
    private int geofenceMinRadiusMeters;    // floor — plan can restrict how tight owners may set it

    // ---- Order quotas ----
    private int maxMonthlyOrders;
    private boolean manualStaffOrdersEnabled;

    // ---- Staff/RBAC quotas ----
    private int maxStaffAccounts;
    private boolean subRoleGranularityEnabled; // e.g. Cashier/Kitchen split vs just "Staff"

    // ---- Feature flags ----
    private boolean analyticsEnabled;
    private boolean couponsEnabled;
    private boolean multiTenantBranchesEnabled;
    private boolean customBrandingEnabled;
    private boolean prioritySupportEnabled;
    private boolean apiAccessEnabled; // public API / webhooks for enterprise integrations

    // ---- Reporting ----
    private boolean revenueExportEnabled;
    private int revenueExportHistoryMonths; // e.g. BASIC=1, PRO=12, ENTERPRISE=unlimited(-1)
}