package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.ApprovalAction;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for approving or rejecting an invoice.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    @NotNull(message = "Action is required")
    private ApprovalAction action;

    @Size(max = 1000, message = "Comments must be less than 1000 characters")
    private String comments;

    @Size(max = 500, message = "Rejection reason must be less than 500 characters")
    private String rejectionReason;
}
