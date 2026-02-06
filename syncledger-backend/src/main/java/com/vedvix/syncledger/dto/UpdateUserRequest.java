package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.UserRole;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for updating an existing user.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {

    @Size(max = 100, message = "First name must be less than 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be less than 100 characters")
    private String lastName;

    private UserRole role;

    private Boolean isActive;

    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    @Size(max = 100, message = "Department must be less than 100 characters")
    private String department;

    @Size(max = 200, message = "Job title must be less than 200 characters")
    private String jobTitle;
}
