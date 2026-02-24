package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User entity.
 * Includes organization context for multi-tenant support.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Boolean isActive;
    private String profilePictureUrl;
    private String phone;
    private String department;
    private String jobTitle;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String createdByEmail;
    
    // Organization context
    private Long organizationId;
    private String organizationSlug;
    private String organizationName;
    private String organizationStatus;

    /**
     * Get full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
