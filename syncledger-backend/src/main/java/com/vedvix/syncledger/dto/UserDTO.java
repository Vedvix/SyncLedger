package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.UserRole;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for User entity.
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
    private UserRole role;
    private Boolean isActive;
    private String profilePictureUrl;
    private String phone;
    private String department;
    private String jobTitle;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String createdByEmail;

    /**
     * Get full name.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
