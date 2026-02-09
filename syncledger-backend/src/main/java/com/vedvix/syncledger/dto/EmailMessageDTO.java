package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing an email message from Microsoft Graph.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageDTO {

    private String messageId;
    private String internetMessageId;
    private String fromAddress;
    private String fromName;
    private List<String> toAddresses;
    private String subject;
    private String bodyPreview;
    private String bodyContent;
    private LocalDateTime receivedAt;
    private Boolean hasAttachments;
    private Integer attachmentCount;
    private List<EmailAttachmentDTO> attachments;
    private Boolean isRead;
    
    /**
     * Get display name for sender.
     */
    public String getFromDisplay() {
        if (fromName != null && !fromName.isBlank()) {
            return fromName + " <" + fromAddress + ">";
        }
        return fromAddress;
    }
}
