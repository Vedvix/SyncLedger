package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for user session information (for session management UI).
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDTO {

    /**
     * Unique session ID.
     */
    private Long id;

    /**
     * Device name (parsed from user agent).
     */
    private String deviceName;

    /**
     * IP address of the session.
     */
    private String ipAddress;

    /**
     * Location (derived from IP, if available).
     */
    private String location;

    /**
     * Full user agent string.
     */
    private String userAgent;

    /**
     * When the session was created.
     */
    private LocalDateTime createdAt;

    /**
     * When the session was last active.
     */
    private LocalDateTime lastUsedAt;

    /**
     * When the session expires.
     */
    private LocalDateTime expiresAt;

    /**
     * Whether this is the current session making the request.
     */
    private Boolean isCurrent;
}
