package com.vedvix.syncledger.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for user login.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
