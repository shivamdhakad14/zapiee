package com.zapiee.menu_service.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanFeaturesResponse {

    private String planId;

    // ---- Menu/Catalog quotas ----
    private int maxCategories;
    private int maxMenuItemsPerCategory;
    private int maxMenuItemsTotal;
    private boolean bulkMenuImportEnabled;

    // ---- Table/QR quotas ----
    private int maxTables;
    private boolean qrRotationEnabled;
    private int geofenceMinRadiusMeters;

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
