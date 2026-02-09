package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.EmailAttachmentDTO;
import com.vedvix.syncledger.dto.EmailMessageDTO;
import com.vedvix.syncledger.model.EmailLog;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.model.OrganizationStatus;
import com.vedvix.syncledger.repository.EmailLogRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Email polling service that orchestrates the entire email processing pipeline.
 * 
 * Flow:
 * 1. Poll emails from all active organizations' mailboxes
 * 2. Extract PDF attachments from unread emails
 * 3. Upload PDFs to S3 with organization-specific paths
 * 4. Trigger PDF extraction service
 * 5. Create invoice records with extracted data
 * 6. Mark emails as processed
 * 
 * Multi-tenant isolation is enforced at every step.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.polling.enabled", havingValue = "true")
public class EmailPollingService {

    private final MicrosoftGraphService graphService;
    private final OrganizationRepository organizationRepository;
    private final EmailLogRepository emailLogRepository;
    private final S3Service s3Service;
    private final InvoiceProcessingService invoiceProcessingService;

    @Value("${email.polling.max-emails-per-batch:50}")
    private int maxEmailsPerBatch;

    @Value("${email.polling.enabled:true}")
    private boolean pollingEnabled;

    // Prevent concurrent polls
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    /**
     * Scheduled email polling task.
     * Runs every 5 minutes by default (configurable via email.polling.interval).
     */
    @Scheduled(fixedDelayString = "${email.polling.interval:300000}")
    public void scheduledPoll() {
        if (!pollingEnabled) {
            log.debug("Email polling is disabled");
            return;
        }

        if (!isPolling.compareAndSet(false, true)) {
            log.info("Email polling already in progress, skipping this cycle");
            return;
        }

        try {
            log.info("Starting scheduled email polling cycle");
            long startTime = System.currentTimeMillis();
            
            int totalEmailsProcessed = pollAllOrganizations();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Email polling cycle completed in {}ms, processed {} emails", duration, totalEmailsProcessed);
            
        } catch (Exception e) {
            log.error("Error during scheduled email polling: {}", e.getMessage(), e);
        } finally {
            isPolling.set(false);
        }
    }

    /**
     * Poll emails for all active organizations.
     * 
     * @return Total number of emails processed
     */
    @Transactional
    public int pollAllOrganizations() {
        List<Organization> activeOrgs = organizationRepository.findOrganizationsForEmailSync();
        
        log.info("Found {} active organizations with email configured for polling", activeOrgs.size());
        
        int totalProcessed = 0;
        
        for (Organization org : activeOrgs) {
            try {
                int processed = pollOrganizationEmails(org);
                totalProcessed += processed;
            } catch (Exception e) {
                log.error("Error polling emails for organization {}: {}", org.getSlug(), e.getMessage());
                // Continue with other organizations
            }
        }
        
        return totalProcessed;
    }

    /**
     * Poll emails for a specific organization.
     * 
     * @param organization The organization to poll
     * @return Number of emails processed
     */
    @Transactional
    public int pollOrganizationEmails(Organization organization) {
        String mailbox = organization.getEmailAddress();
        
        if (mailbox == null || mailbox.isBlank()) {
            log.warn("Organization {} has no email address configured", organization.getSlug());
            return 0;
        }

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            log.debug("Skipping non-active organization: {}", organization.getSlug());
            return 0;
        }

        log.info("Polling emails for organization: {} ({})", organization.getName(), mailbox);
        
        try {
            // Get unread emails with attachments
            List<EmailMessageDTO> emails = graphService.getUnreadEmails(mailbox, maxEmailsPerBatch);
            
            if (emails.isEmpty()) {
                log.debug("No new emails for organization: {}", organization.getSlug());
                return 0;
            }

            log.info("Found {} new emails for {}", emails.size(), organization.getSlug());
            
            int processedCount = 0;
            
            for (EmailMessageDTO email : emails) {
                try {
                    if (processEmail(organization, email, mailbox)) {
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing email {} for org {}: {}", 
                            email.getMessageId(), organization.getSlug(), e.getMessage());
                    // Log the failed email
                    logFailedEmail(organization, email, e.getMessage());
                }
            }

            log.info("Processed {}/{} emails for {}", processedCount, emails.size(), organization.getSlug());
            return processedCount;
            
        } catch (Exception e) {
            log.error("Error polling emails for {}: {}", organization.getSlug(), e.getMessage());
            throw new RuntimeException("Email polling failed for " + organization.getSlug(), e);
        }
    }

    /**
     * Process a single email for an organization.
     * 
     * @param organization The organization
     * @param email The email message
     * @param mailbox The mailbox email address
     * @return true if processed successfully
     */
    private boolean processEmail(Organization organization, EmailMessageDTO email, String mailbox) {
        String messageId = email.getMessageId();
        
        // Check if already processed
        if (emailLogRepository.existsByMessageId(messageId)) {
            log.debug("Email {} already processed, skipping", messageId);
            // Still mark as read
            graphService.markAsRead(mailbox, messageId);
            return false;
        }

        log.info("Processing email: '{}' from {} for org {}", 
                email.getSubject(), email.getFromAddress(), organization.getSlug());
        
        long startTime = System.currentTimeMillis();
        
        // Create email log entry
        EmailLog emailLog = createEmailLog(organization, email);
        emailLogRepository.save(emailLog);
        
        try {
            // Get attachments
            List<EmailAttachmentDTO> attachments = graphService.getEmailAttachments(mailbox, messageId);
            
            // Filter to processable attachments (PDFs, images)
            List<EmailAttachmentDTO> pdfAttachments = attachments.stream()
                    .filter(EmailAttachmentDTO::isProcessable)
                    .collect(Collectors.toList());
            
            if (pdfAttachments.isEmpty()) {
                log.info("No processable attachments in email: {}", email.getSubject());
                emailLog.markProcessed(0);
                emailLogRepository.save(emailLog);
                graphService.markAsRead(mailbox, messageId);
                return true;
            }

            log.info("Found {} processable attachments in email", pdfAttachments.size());
            
            // Update email log with attachment info
            emailLog.setAttachmentCount(pdfAttachments.size());
            emailLog.setAttachmentNames(pdfAttachments.stream()
                    .map(EmailAttachmentDTO::getName)
                    .collect(Collectors.joining(", ")));
            
            // Process each attachment
            int invoicesCreated = 0;
            for (EmailAttachmentDTO attachment : pdfAttachments) {
                try {
                    Invoice invoice = processAttachment(organization, email, attachment);
                    if (invoice != null) {
                        invoicesCreated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing attachment {}: {}", attachment.getName(), e.getMessage());
                }
            }

            // Update email log
            long duration = System.currentTimeMillis() - startTime;
            emailLog.setProcessingDurationMs((int) duration);
            emailLog.markProcessed(invoicesCreated);
            emailLogRepository.save(emailLog);

            // Mark email as read and move to processed folder
            graphService.markAsRead(mailbox, messageId);
            graphService.moveToProcessedFolder(mailbox, messageId);

            log.info("Successfully processed email {} with {} invoices in {}ms", 
                    messageId, invoicesCreated, duration);
            
            return true;

        } catch (Exception e) {
            log.error("Error processing email {}: {}", messageId, e.getMessage());
            emailLog.markFailed(e.getMessage());
            emailLogRepository.save(emailLog);
            throw e;
        }
    }

    /**
     * Process a single attachment and create an invoice.
     * 
     * @param organization The organization
     * @param email The source email
     * @param attachment The attachment to process
     * @return Created invoice or null if failed
     */
    private Invoice processAttachment(Organization organization, EmailMessageDTO email, 
                                       EmailAttachmentDTO attachment) {
        log.info("Processing attachment: {} ({} bytes)", attachment.getName(), attachment.getSize());
        
        if (attachment.getContentBytes() == null || attachment.getContentBytes().length == 0) {
            log.warn("Attachment {} has no content", attachment.getName());
            return null;
        }

        try {
            // Upload to S3 with organization-specific path
            String s3Key = s3Service.uploadInvoiceFile(
                    organization,
                    attachment.getName(),
                    new ByteArrayInputStream(attachment.getContentBytes()),
                    attachment.getContentType(),
                    attachment.getContentBytes().length
            );

            log.info("Uploaded attachment to S3: {}", s3Key);

            // Queue for processing through PDF service
            Invoice invoice = invoiceProcessingService.queueForProcessing(
                    organization,
                    s3Key,
                    attachment.getName(),
                    email.getMessageId(),
                    email.getFromAddress(),
                    email.getSubject(),
                    email.getReceivedAt()
            );

            log.info("Created invoice {} for attachment {}", invoice.getId(), attachment.getName());
            return invoice;

        } catch (Exception e) {
            log.error("Failed to process attachment {}: {}", attachment.getName(), e.getMessage());
            throw new RuntimeException("Failed to process attachment: " + attachment.getName(), e);
        }
    }

    /**
     * Create EmailLog entry from email DTO.
     */
    private EmailLog createEmailLog(Organization organization, EmailMessageDTO email) {
        return EmailLog.builder()
                .organization(organization)
                .messageId(email.getMessageId())
                .internetMessageId(email.getInternetMessageId())
                .fromAddress(email.getFromAddress())
                .fromName(email.getFromName())
                .toAddresses(email.getToAddresses() != null 
                        ? String.join(", ", email.getToAddresses()) : null)
                .subject(email.getSubject())
                .bodyPreview(email.getBodyPreview())
                .receivedAt(email.getReceivedAt() != null ? email.getReceivedAt() : LocalDateTime.now())
                .hasAttachments(email.getHasAttachments())
                .isProcessed(false)
                .hasError(false)
                .build();
    }

    /**
     * Log a failed email processing attempt.
     */
    private void logFailedEmail(Organization organization, EmailMessageDTO email, String errorMessage) {
        try {
            if (!emailLogRepository.existsByMessageId(email.getMessageId())) {
                EmailLog emailLog = createEmailLog(organization, email);
                emailLog.markFailed(errorMessage);
                emailLogRepository.save(emailLog);
            }
        } catch (Exception e) {
            log.error("Failed to log email error: {}", e.getMessage());
        }
    }

    /**
     * Manually trigger polling for a specific organization.
     * Used for testing or on-demand sync.
     * 
     * @param organizationId The organization ID
     * @return Number of emails processed
     */
    @Transactional
    public int triggerPollForOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        
        return pollOrganizationEmails(org);
    }

    /**
     * Test email connection for an organization.
     * 
     * @param organizationId The organization ID
     * @return true if connection successful
     */
    public boolean testOrganizationEmailConnection(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
        
        if (org.getEmailAddress() == null || org.getEmailAddress().isBlank()) {
            return false;
        }
        
        return graphService.testMailboxConnection(org.getEmailAddress());
    }

    /**
     * Get email polling status.
     */
    public EmailPollingStatus getStatus() {
        return EmailPollingStatus.builder()
                .enabled(pollingEnabled)
                .currentlyPolling(isPolling.get())
                .totalOrganizations(organizationRepository.countByStatus(OrganizationStatus.ACTIVE))
                .organizationsWithEmail(organizationRepository.findOrganizationsForEmailSync().size())
                .pendingEmails(emailLogRepository.countByIsProcessedFalse())
                .failedEmails(emailLogRepository.countByHasErrorTrue())
                .build();
    }

    /**
     * Email polling status DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class EmailPollingStatus {
        private boolean enabled;
        private boolean currentlyPolling;
        private long totalOrganizations;
        private int organizationsWithEmail;
        private long pendingEmails;
        private long failedEmails;
    }
}
