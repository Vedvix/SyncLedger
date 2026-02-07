package com.vedvix.syncledger.dto;

import lombok.*;

/**
 * DTO for Platform-wide statistics (Super Admin).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDTO {
    private long totalOrganizations;
    private long activeOrganizations;
    private long totalUsers;
    private long totalInvoices;
}
