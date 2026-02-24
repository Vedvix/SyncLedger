package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Organization data transfer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private Long id;
    private String name;
    private String slug;
    private String emailAddress;
    private String status;
    private String sageApiEndpoint;
    private String erpType;
    private String erpApiEndpoint;
    private String erpTenantId;
    private String erpCompanyId;
    private Boolean erpAutoSync;
    private Boolean erpConfigured;
    private String s3FolderPath;
    private String sqsQueueName;

    // Contact
    private String contactName;
    private String contactEmail;
    private String contactPhone;

    // Subscription summary
    private String subscriptionPlan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionExpiresAt;
    private Long remainingTrialDays;
    private Boolean hasAccess;

    // MS Config (summary only - never expose secrets)
    private Boolean msCredentialsConfigured;
    private Boolean msCredentialsVerified;
    private String msClientId;
    private String msTenantId;
    private String msMailboxEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
