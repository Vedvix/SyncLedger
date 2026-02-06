package com.vedvix.syncledger.dto;

import com.vedvix.syncledger.model.ApprovalAction;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Approval entity.
 * 
 * @author vedvix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalDTO {

    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private Long approverId;
    private String approverName;
    private String approverEmail;
    private ApprovalAction action;
    private String comments;
    private String rejectionReason;
    private Integer approvalLevel;
    private LocalDateTime createdAt;
}
