package com.vedvix.syncledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing an email attachment from Microsoft Graph.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachmentDTO {

    private String id;
    private String name;
    private String contentType;
    private Long size;
    private byte[] contentBytes;
    private Boolean isInline;
    
    /**
     * Check if attachment is a PDF file.
     */
    public boolean isPdf() {
        if (contentType != null && contentType.toLowerCase().contains("pdf")) {
            return true;
        }
        if (name != null && name.toLowerCase().endsWith(".pdf")) {
            return true;
        }
        return false;
    }
    
    /**
     * Check if attachment is processable (PDF or image).
     */
    public boolean isProcessable() {
        if (isPdf()) {
            return true;
        }
        if (contentType != null) {
            String ct = contentType.toLowerCase();
            return ct.contains("image/jpeg") || ct.contains("image/png") || 
                   ct.contains("image/tiff") || ct.contains("image/bmp");
        }
        if (name != null) {
            String n = name.toLowerCase();
            return n.endsWith(".jpg") || n.endsWith(".jpeg") || 
                   n.endsWith(".png") || n.endsWith(".tiff") || n.endsWith(".bmp");
        }
        return false;
    }
    
    /**
     * Get file extension.
     */
    public String getExtension() {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }
}
