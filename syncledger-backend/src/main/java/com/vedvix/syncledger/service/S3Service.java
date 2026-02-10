package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * AWS S3 service for file storage.
 * Manages invoice file uploads with organization-specific paths.
 * 
 * Activated when: storage.type=s3
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3Service implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry:3600}")
    private int presignedUrlExpirySeconds;

    /**
     * Upload invoice file to S3 with organization-specific path.
     */
    @Override
    public String uploadInvoiceFile(Organization org, String fileName, 
                                     InputStream inputStream, String contentType, long size) {
        String s3Key = generateS3Key(org, fileName);
        
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .metadata(java.util.Map.of(
                        "organization-id", org.getId().toString(),
                        "organization-slug", org.getSlug(),
                        "original-filename", fileName,
                        "uploaded-at", LocalDateTime.now().toString()
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
            
            log.info("Uploaded file {} to S3 key: {}", fileName, s3Key);
            return s3Key;
            
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Generate presigned URL for file download.
     */
    @Override
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(presignedUrlExpirySeconds))
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build())
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Error generating presigned URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Download file from S3.
     */
    @Override
    public InputStream downloadFile(String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(request);
        } catch (Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    /**
     * Delete file from S3.
     */
    @Override
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Deleted file from S3: {}", s3Key);
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    /**
     * Check if file exists in S3.
     */
    @Override
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Generate organization-specific S3 key.
     * Format: {org-slug}/files/{yyyy}/{MM}/{dd}/{uuid}_{filename}
     * Example: evolotek/files/2026/02/09/abc12345_invoice.pdf
     * 
     * Falls back to org.s3FolderPath if set (for custom bucket structures)
     */
    private String generateS3Key(Organization org, String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitizedFileName = sanitizeFileName(fileName);
        
        // Use s3FolderPath if set, otherwise use standard format
        String basePath = (org.getS3FolderPath() != null && !org.getS3FolderPath().isBlank()) 
                ? org.getS3FolderPath() 
                : org.getSlug() + "/files";
        
        return String.format("%s/%s/%s_%s", 
            basePath,
            timestamp,
            uuid,
            sanitizedFileName
        );
    }

    /**
     * Sanitize filename for S3.
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
