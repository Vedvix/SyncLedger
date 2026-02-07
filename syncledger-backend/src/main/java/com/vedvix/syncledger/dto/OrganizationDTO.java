package com.vedvix.syncledger.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for Organization data transfer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private Long id;
    private String name;
    private String slug;
    private String emailAddress;
    private String status;
    private String sageApiEndpoint;
    private String s3FolderPath;
    private String sqsQueueName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
