package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Organization statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationStatsDTO {
    private Long organizationId;
    private String organizationName;
    private long totalUsers;
    private long activeUsers;
    private long totalInvoices;
    private String status;
    private LocalDateTime createdAt;
}
