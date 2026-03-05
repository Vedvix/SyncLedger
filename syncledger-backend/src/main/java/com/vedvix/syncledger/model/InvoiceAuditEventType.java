package com.vedvix.syncledger.model;

/**
 * Types of events tracked in the invoice audit trail.
 * Covers the full lifecycle from receipt through ERP sync.
 *
 * @author vedvix
 */
public enum InvoiceAuditEventType {

    // ── Receipt ──────────────────────────────────────────────────────────
    RECEIVED_VIA_EMAIL("Received via Email", "Invoice received from email attachment"),
    RECEIVED_VIA_UPLOAD("Received via Upload", "Invoice manually uploaded"),

    // ── AI Extraction ────────────────────────────────────────────────────
    EXTRACTION_STARTED("Extraction Started", "AI extraction initiated"),
    EXTRACTION_COMPLETED("Extraction Completed", "AI extraction finished successfully"),
    EXTRACTION_FAILED("Extraction Failed", "AI extraction encountered an error"),

    // ── Status Changes ───────────────────────────────────────────────────
    STATUS_CHANGED("Status Changed", "Invoice status changed"),

    // ── Review & Edit ────────────────────────────────────────────────────
    FIELD_UPDATED("Field Updated", "Invoice fields were updated"),
    SUBMITTED_FOR_REVIEW("Submitted for Review", "Invoice submitted for manual review"),

    // ── Approval Workflow ────────────────────────────────────────────────
    APPROVED("Approved", "Invoice approved"),
    REJECTED("Rejected", "Invoice rejected"),

    // ── Sync to ERP ──────────────────────────────────────────────────────
    SYNC_STARTED("Sync Started", "ERP synchronization initiated"),
    SYNC_COMPLETED("Sync Completed", "Successfully synced to ERP"),
    SYNC_FAILED("Sync Failed", "ERP synchronization failed"),

    // ── Assignment ───────────────────────────────────────────────────────
    ASSIGNED("Assigned", "Invoice assigned to a user"),

    // ── Other ────────────────────────────────────────────────────────────
    EXPORTED("Exported", "Invoice exported"),
    ARCHIVED("Archived", "Invoice archived"),
    NOTE_ADDED("Note Added", "Review notes updated"),
    VENDOR_LINKED("Vendor Linked", "Vendor auto-linked from extraction");

    private final String displayName;
    private final String description;

    InvoiceAuditEventType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
