package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.dto.ApiResponseDto;
import com.vedvix.syncledger.dto.PagedResponse;
import com.vedvix.syncledger.model.EmailLog;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.repository.EmailLogRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.service.EmailPollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for email polling operations.
 * Super Admin can manage platform-wide email operations.
 * Organization Admins can view their own email logs.
 * 
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/v1/emails")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Email polling and processing operations")
@ConditionalOnProperty(name = "email.polling.enabled", havingValue = "true", matchIfMissing = true)
public class EmailController {

    private final EmailPollingService emailPollingService;
    private final EmailLogRepository emailLogRepository;
    private final OrganizationRepository organizationRepository;

    // ==================== SUPER ADMIN ENDPOINTS ====================

    /**
     * Get email polling status (Super Admin only).
     */
    @Operation(summary = "Get email polling status", description = "Get current status of email polling service")
    @GetMapping("/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<EmailPollingService.EmailPollingStatus>> getPollingStatus(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.info("Getting email polling status for super admin: {}", currentUser.getEmail());
        
        EmailPollingService.EmailPollingStatus status = emailPollingService.getStatus();
        return ResponseEntity.ok(ApiResponseDto.success("Email polling status retrieved", status));
    }

    /**
     * Trigger email polling for all organizations (Super Admin only).
     */
    @Operation(summary = "Trigger email polling", description = "Manually trigger email polling for all organizations")
    @PostMapping("/poll")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Integer>> triggerPoll(@AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.info("Manual email poll triggered by super admin: {}", currentUser.getEmail());
        
        int processed = emailPollingService.pollAllOrganizations();
        return ResponseEntity.ok(ApiResponseDto.success(
                "Email polling completed. Processed " + processed + " emails.", processed));
    }

    /**
     * Trigger email polling for a specific organization (Super Admin only).
     */
    @Operation(summary = "Trigger polling for organization", description = "Manually trigger email polling for a specific organization")
    @PostMapping("/poll/organization/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Integer>> triggerOrganizationPoll(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.info("Manual poll for org {} triggered by super admin: {}", orgId, currentUser.getEmail());
        
        int processed = emailPollingService.triggerPollForOrganization(orgId);
        return ResponseEntity.ok(ApiResponseDto.success(
                "Organization email polling completed. Processed " + processed + " emails.", processed));
    }

    /**
     * Test email connection for an organization (Super Admin only).
     */
    @Operation(summary = "Test email connection", description = "Test email connection for an organization's mailbox")
    @GetMapping("/test-connection/organization/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<Boolean>> testEmailConnection(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.info("Testing email connection for org {} by super admin: {}", orgId, currentUser.getEmail());
        
        boolean connected = emailPollingService.testOrganizationEmailConnection(orgId);
        
        String message = connected 
                ? "Email connection successful" 
                : "Email connection failed - check mailbox configuration and Azure AD permissions";
                
        return ResponseEntity.ok(ApiResponseDto.success(message, connected));
    }

    /**
     * Get all email logs with pagination (Super Admin only).
     */
    @Operation(summary = "Get all email logs", description = "Get paginated list of all email logs")
    @GetMapping("/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<PagedResponse<EmailLogDTO>>> getAllEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(required = false) Boolean hasError,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.debug("Getting email logs for super admin, page: {}, size: {}", page, size);
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<EmailLog> logs;
        
        if (processed != null && processed) {
            logs = emailLogRepository.findByIsProcessedTrue(pageRequest);
        } else if (hasError != null && hasError) {
            logs = emailLogRepository.findByHasErrorTrue(pageRequest);
        } else {
            logs = emailLogRepository.findAll(pageRequest);
        }
        
        Page<EmailLogDTO> dtoPage = logs.map(this::mapToDTO);
        return ResponseEntity.ok(ApiResponseDto.success("Email logs retrieved", PagedResponse.from(dtoPage)));
    }

    // ==================== ORGANIZATION ADMIN ENDPOINTS ====================

    /**
     * Get email logs for current user's organization (Admin only).
     */
    @Operation(summary = "Get organization email logs", description = "Get email logs for the current user's organization")
    @GetMapping("/logs/my-organization")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<PagedResponse<EmailLogDTO>>> getOrganizationEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        // Super admin needs to specify org, or we return all
        if (currentUser.isSuperAdmin()) {
            return getAllEmailLogs(page, size, null, null, currentUser);
        }
        
        Long orgId = currentUser.getOrganizationId();
        if (orgId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("User not associated with an organization"));
        }
        
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<EmailLog> logs = emailLogRepository.findByOrganization(org, pageRequest);
        
        Page<EmailLogDTO> dtoPage = logs.map(this::mapToDTO);
        return ResponseEntity.ok(ApiResponseDto.success("Email logs retrieved", PagedResponse.from(dtoPage)));
    }

    /**
     * Get email processing stats for organization.
     */
    @Operation(summary = "Get email stats", description = "Get email processing statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<EmailStatsDTO>> getEmailStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        
        Object[] stats;
        
        if (currentUser.isSuperAdmin()) {
            stats = emailLogRepository.getEmailProcessingStats();
        } else {
            Long orgId = currentUser.getOrganizationId();
            if (orgId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("User not associated with an organization"));
            }
            Organization org = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            stats = emailLogRepository.getEmailProcessingStatsByOrganization(org);
        }
        
        EmailStatsDTO dto = mapStatsToDTO(stats);
        return ResponseEntity.ok(ApiResponseDto.success("Email stats retrieved", dto));
    }

    /**
     * Get recent email logs.
     */
    @Operation(summary = "Get recent emails", description = "Get 20 most recent email logs")
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponseDto<List<EmailLogDTO>>> getRecentEmails(@AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<EmailLog> logs;
        
        if (currentUser.isSuperAdmin()) {
            logs = emailLogRepository.findTop20ByOrderByReceivedAtDesc();
        } else {
            Long orgId = currentUser.getOrganizationId();
            if (orgId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("User not associated with an organization"));
            }
            Organization org = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new RuntimeException("Organization not found"));
            logs = emailLogRepository.findTop20ByOrganizationOrderByReceivedAtDesc(org);
        }
        
        List<EmailLogDTO> dtos = logs.stream().map(this::mapToDTO).toList();
        return ResponseEntity.ok(ApiResponseDto.success("Recent emails retrieved", dtos));
    }

    // ==================== HELPER METHODS ====================

    private EmailLogDTO mapToDTO(EmailLog log) {
        return EmailLogDTO.builder()
                .id(log.getId())
                .organizationId(log.getOrganization() != null ? log.getOrganization().getId() : null)
                .organizationName(log.getOrganization() != null ? log.getOrganization().getName() : null)
                .messageId(log.getMessageId())
                .fromAddress(log.getFromAddress())
                .fromName(log.getFromName())
                .subject(log.getSubject())
                .bodyPreview(log.getBodyPreview())
                .receivedAt(log.getReceivedAt())
                .hasAttachments(log.getHasAttachments())
                .attachmentCount(log.getAttachmentCount())
                .attachmentNames(log.getAttachmentNames())
                .isProcessed(log.getIsProcessed())
                .processedAt(log.getProcessedAt())
                .invoicesExtracted(log.getInvoicesExtracted())
                .hasError(log.getHasError())
                .errorMessage(log.getErrorMessage())
                .processingDurationMs(log.getProcessingDurationMs())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private EmailStatsDTO mapStatsToDTO(Object[] stats) {
        if (stats == null || stats.length == 0 || stats[0] == null) {
            return EmailStatsDTO.builder()
                    .totalEmails(0L)
                    .processedEmails(0L)
                    .failedEmails(0L)
                    .totalInvoicesExtracted(0L)
                    .build();
        }
        
        return EmailStatsDTO.builder()
                .totalEmails(stats[0] != null ? ((Number) stats[0]).longValue() : 0L)
                .processedEmails(stats[1] != null ? ((Number) stats[1]).longValue() : 0L)
                .failedEmails(stats[2] != null ? ((Number) stats[2]).longValue() : 0L)
                .totalInvoicesExtracted(stats[3] != null ? ((Number) stats[3]).longValue() : 0L)
                .build();
    }

    // ==================== INNER DTOs ====================

    @lombok.Data
    @lombok.Builder
    public static class EmailLogDTO {
        private Long id;
        private Long organizationId;
        private String organizationName;
        private String messageId;
        private String fromAddress;
        private String fromName;
        private String subject;
        private String bodyPreview;
        private java.time.LocalDateTime receivedAt;
        private Boolean hasAttachments;
        private Integer attachmentCount;
        private String attachmentNames;
        private Boolean isProcessed;
        private java.time.LocalDateTime processedAt;
        private Integer invoicesExtracted;
        private Boolean hasError;
        private String errorMessage;
        private Integer processingDurationMs;
        private java.time.LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class EmailStatsDTO {
        private Long totalEmails;
        private Long processedEmails;
        private Long failedEmails;
        private Long totalInvoicesExtracted;
    }
}
