package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for organization self-signup.
 * Creates both an organization and its first admin user.
 * 
 * @author vedvix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSignupRequest {

    // Organization details
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255, message = "Organization name must be between 2 and 255 characters")
    private String organizationName;

    @NotBlank(message = "Organization email is required")
    @Email(message = "Invalid organization email address")
    private String organizationEmail;

    // Admin user details
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
    )
    private String password;

    // Optional contact info
    private String phone;
    private String companyWebsite;
}
