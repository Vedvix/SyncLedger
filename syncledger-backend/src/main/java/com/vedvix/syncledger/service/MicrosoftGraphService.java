package com.vedvix.syncledger.service;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredential;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedvix.syncledger.dto.EmailAttachmentDTO;
import com.vedvix.syncledger.dto.EmailMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Microsoft Graph API service for email operations using REST API.
 * Handles reading emails from organization mailboxes.
 * 
 * Uses application permissions (not user delegation):
 * - Mail.Read: Read mail in all mailboxes
 * - Mail.ReadWrite: Read and write mail (for marking as read, moving)
 * 
 * @author vedvix
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "email.polling.enabled", havingValue = "true")
public class MicrosoftGraphService {

    private static final String GRAPH_API_BASE = "https://graph.microsoft.com/v1.0";
    private static final String SCOPE = "https://graph.microsoft.com/.default";

    private final ClientSecretCredential credential;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MicrosoftGraphService(
            ClientSecretCredential credential,
            @Qualifier("graphRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.credential = credential;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Get access token for Graph API.
     */
    private String getAccessToken() {
        try {
            TokenRequestContext context = new TokenRequestContext();
            context.addScopes(SCOPE);
            String token = credential.getToken(context).block().getToken();
            return token;
        } catch (Exception e) {
            log.error("Failed to get access token: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate with Azure AD", e);
        }
    }

    /**
     * Create HTTP headers with bearer token.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Get unread emails from a specific mailbox.
     * 
     * @param mailboxEmail The email address of the mailbox to read
     * @param maxResults Maximum number of emails to retrieve
     * @return List of email DTOs
     */
    public List<EmailMessageDTO> getUnreadEmails(String mailboxEmail, int maxResults) {
        log.info("Fetching unread emails from mailbox: {}", mailboxEmail);
        
        try {
            // Note: Removed $orderby to avoid "InefficientFilter" error on some mailboxes
            // The results will be sorted in memory if needed
            String url = String.format(
                "%s/users/%s/mailFolders/inbox/messages?$filter=isRead eq false&$top=%d&$select=id,internetMessageId,from,toRecipients,subject,bodyPreview,receivedDateTime,hasAttachments,isRead",
                GRAPH_API_BASE, mailboxEmail, maxResults
            );

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Graph API returned error: {}", response.getStatusCode());
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode messages = root.get("value");
            
            if (messages == null || !messages.isArray()) {
                log.info("No unread emails found in {}", mailboxEmail);
                return new ArrayList<>();
            }

            List<EmailMessageDTO> result = new ArrayList<>();
            for (JsonNode message : messages) {
                // Filter for messages with attachments in memory (to avoid complex OData filter)
                boolean hasAttachments = message.has("hasAttachments") && message.get("hasAttachments").asBoolean();
                if (hasAttachments) {
                    result.add(convertToDTO(message));
                }
            }
            
            // Sort by received date descending (newest first)
            result.sort((a, b) -> {
                if (a.getReceivedAt() == null) return 1;
                if (b.getReceivedAt() == null) return -1;
                return b.getReceivedAt().compareTo(a.getReceivedAt());
            });

            log.info("Found {} unread emails with attachments in {}", result.size(), mailboxEmail);
            return result;

        } catch (Exception e) {
            log.error("Error fetching emails from {}: {}", mailboxEmail, e.getMessage());
            throw new RuntimeException("Failed to fetch emails from " + mailboxEmail, e);
        }
    }

    /**
     * Get attachments for a specific email.
     * 
     * @param mailboxEmail The mailbox email
     * @param messageId The message ID
     * @return List of attachment DTOs with content
     */
    public List<EmailAttachmentDTO> getEmailAttachments(String mailboxEmail, String messageId) {
        log.debug("Fetching attachments for message {} in {}", messageId, mailboxEmail);
        
        try {
            // Get list of attachments
            String listUrl = String.format(
                "%s/users/%s/messages/%s/attachments",
                GRAPH_API_BASE, mailboxEmail, messageId
            );

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(listUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("Failed to get attachments: {}", response.getStatusCode());
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode attachments = root.get("value");
            
            if (attachments == null || !attachments.isArray()) {
                log.info("No attachments array in response for message {}", messageId);
                return new ArrayList<>();
            }

            log.debug("Found {} attachment entries in message {}", attachments.size(), messageId);
            
            List<EmailAttachmentDTO> result = new ArrayList<>();
            
            for (JsonNode attachment : attachments) {
                String type = attachment.has("@odata.type") ? attachment.get("@odata.type").asText() : "";
                String name = attachment.has("name") ? attachment.get("name").asText() : "unknown";
                log.debug("Processing attachment: name={}, type={}", name, type);
                
                // Only process file attachments
                if (type.contains("fileAttachment")) {
                    String attachmentId = attachment.get("id").asText();
                    
                    // Get full attachment with content
                    String attachmentUrl = String.format(
                        "%s/users/%s/messages/%s/attachments/%s",
                        GRAPH_API_BASE, mailboxEmail, messageId, attachmentId
                    );
                    
                    ResponseEntity<String> attachmentResponse = restTemplate.exchange(
                        attachmentUrl, HttpMethod.GET, entity, String.class
                    );
                    
                    if (attachmentResponse.getStatusCode() == HttpStatus.OK) {
                        JsonNode fullAttachment = objectMapper.readTree(attachmentResponse.getBody());
                        
                        EmailAttachmentDTO dto = EmailAttachmentDTO.builder()
                                .id(fullAttachment.get("id").asText())
                                .name(fullAttachment.has("name") ? fullAttachment.get("name").asText() : "unknown")
                                .contentType(fullAttachment.has("contentType") ? fullAttachment.get("contentType").asText() : "application/octet-stream")
                                .size(fullAttachment.has("size") ? fullAttachment.get("size").asLong() : 0L)
                                .isInline(fullAttachment.has("isInline") && fullAttachment.get("isInline").asBoolean())
                                .build();
                        
                        // Decode base64 content
                        if (fullAttachment.has("contentBytes")) {
                            String base64Content = fullAttachment.get("contentBytes").asText();
                            dto.setContentBytes(Base64.getDecoder().decode(base64Content));
                        }
                        
                        result.add(dto);
                    }
                }
            }

            log.info("Retrieved {} attachments for message {}", result.size(), messageId);
            return result;

        } catch (Exception e) {
            log.error("Error fetching attachments for message {}: {}", messageId, e.getMessage());
            throw new RuntimeException("Failed to fetch attachments", e);
        }
    }

    /**
     * Mark an email as read.
     * 
     * @param mailboxEmail The mailbox email
     * @param messageId The message ID
     */
    public void markAsRead(String mailboxEmail, String messageId) {
        log.debug("Marking message {} as read in {}", messageId, mailboxEmail);
        
        try {
            String url = String.format(
                "%s/users/%s/messages/%s",
                GRAPH_API_BASE, mailboxEmail, messageId
            );

            String body = "{\"isRead\": true}";
            HttpEntity<String> entity = new HttpEntity<>(body, createHeaders());
            
            restTemplate.exchange(url, HttpMethod.PATCH, entity, String.class);
            log.info("Marked message {} as read", messageId);

        } catch (Exception e) {
            log.error("Error marking message {} as read: {}", messageId, e.getMessage());
            // Don't throw - this is not critical
        }
    }

    /**
     * Move email to processed folder (creates folder if needed).
     * 
     * @param mailboxEmail The mailbox email
     * @param messageId The message ID
     */
    public void moveToProcessedFolder(String mailboxEmail, String messageId) {
        log.debug("Moving message {} to Processed folder in {}", messageId, mailboxEmail);
        
        try {
            // First, find or create the Processed folder
            String processedFolderId = getOrCreateFolder(mailboxEmail, "Processed Invoices");
            
            if (processedFolderId != null) {
                // Move the message
                String url = String.format(
                    "%s/users/%s/messages/%s/move",
                    GRAPH_API_BASE, mailboxEmail, messageId
                );
                
                String body = String.format("{\"destinationId\": \"%s\"}", processedFolderId);
                HttpEntity<String> entity = new HttpEntity<>(body, createHeaders());
                
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                log.info("Moved message {} to Processed Invoices folder", messageId);
            }
        } catch (Exception e) {
            log.warn("Could not move message {} to processed folder: {}", messageId, e.getMessage());
            // Not critical - just mark as read
        }
    }

    /**
     * Get or create a mail folder.
     */
    private String getOrCreateFolder(String mailboxEmail, String folderName) {
        try {
            // Try to find existing folder
            String searchUrl = String.format(
                "%s/users/%s/mailFolders?$filter=displayName eq '%s'",
                GRAPH_API_BASE, mailboxEmail, folderName
            );

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode folders = root.get("value");
            
            if (folders != null && folders.isArray() && folders.size() > 0) {
                return folders.get(0).get("id").asText();
            }

            // Create folder if not found
            String createUrl = String.format(
                "%s/users/%s/mailFolders",
                GRAPH_API_BASE, mailboxEmail
            );
            
            String body = String.format("{\"displayName\": \"%s\"}", folderName);
            HttpEntity<String> createEntity = new HttpEntity<>(body, createHeaders());
            
            ResponseEntity<String> createResponse = restTemplate.exchange(
                createUrl, HttpMethod.POST, createEntity, String.class
            );
            
            if (createResponse.getStatusCode() == HttpStatus.CREATED) {
                JsonNode createdFolder = objectMapper.readTree(createResponse.getBody());
                return createdFolder.get("id").asText();
            }
            
            return null;

        } catch (Exception e) {
            log.warn("Could not get/create folder {}: {}", folderName, e.getMessage());
            return null;
        }
    }

    /**
     * Convert JSON message to EmailMessageDTO.
     */
    private EmailMessageDTO convertToDTO(JsonNode message) {
        EmailMessageDTO dto = EmailMessageDTO.builder()
                .messageId(message.get("id").asText())
                .internetMessageId(message.has("internetMessageId") ? message.get("internetMessageId").asText() : null)
                .subject(message.has("subject") ? message.get("subject").asText() : "(No Subject)")
                .bodyPreview(message.has("bodyPreview") ? message.get("bodyPreview").asText() : null)
                .hasAttachments(message.has("hasAttachments") && message.get("hasAttachments").asBoolean())
                .isRead(message.has("isRead") && message.get("isRead").asBoolean())
                .build();

        // Extract from address
        if (message.has("from") && message.get("from").has("emailAddress")) {
            JsonNode emailAddress = message.get("from").get("emailAddress");
            dto.setFromAddress(emailAddress.has("address") ? emailAddress.get("address").asText() : null);
            dto.setFromName(emailAddress.has("name") ? emailAddress.get("name").asText() : null);
        }

        // Extract to addresses
        if (message.has("toRecipients") && message.get("toRecipients").isArray()) {
            List<String> toAddresses = new ArrayList<>();
            for (JsonNode recipient : message.get("toRecipients")) {
                if (recipient.has("emailAddress") && recipient.get("emailAddress").has("address")) {
                    toAddresses.add(recipient.get("emailAddress").get("address").asText());
                }
            }
            dto.setToAddresses(toAddresses);
        }

        // Convert received date
        if (message.has("receivedDateTime")) {
            try {
                OffsetDateTime odt = OffsetDateTime.parse(message.get("receivedDateTime").asText());
                dto.setReceivedAt(odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
            } catch (Exception e) {
                dto.setReceivedAt(LocalDateTime.now());
            }
        }

        return dto;
    }

    /**
     * Test connection to mailbox.
     * 
     * @param mailboxEmail The mailbox to test
     * @return true if connection successful
     */
    public boolean testMailboxConnection(String mailboxEmail) {
        try {
            String url = String.format(
                "%s/users/%s/mailFolders?$top=1",
                GRAPH_API_BASE, mailboxEmail
            );

            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Mailbox connection test failed for {}: {}", mailboxEmail, e.getMessage());
            return false;
        }
    }
}
