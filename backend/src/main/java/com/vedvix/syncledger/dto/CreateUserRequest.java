package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.UserRole;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for creating a new user.
 * Super Admin creates users - no self-registration.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be less than 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be less than 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be less than 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Password must contain uppercase, lowercase, number, and special character")
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    @Size(max = 20, message = "Phone must be less than 20 characters")
    private String phone;

    @Size(max = 100, message = "Department must be less than 100 characters")
    private String department;

    @Size(max = 200, message = "Job title must be less than 200 characters")
    private String jobTitle;
}
