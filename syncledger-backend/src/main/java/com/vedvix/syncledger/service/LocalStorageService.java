package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.Organization;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Local file storage service for development/testing.
 * Stores files on the local filesystem instead of S3.
 * 
 * Activated when: storage.type=local (default for local profile)
 * 
 * @author vedvix
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${storage.local.base-url:http://localhost:8080}")
    private String baseUrl;

    private Path uploadsDir;

    @PostConstruct
    public void init() {
        try {
            uploadsDir = Paths.get(basePath).toAbsolutePath();
            Files.createDirectories(uploadsDir);
            log.info("Local storage initialized at: {}", uploadsDir);
        } catch (IOException e) {
            log.error("Failed to create uploads directory: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize local storage", e);
        }
    }

    @Override
    public String uploadInvoiceFile(Organization org, String fileName, 
                                     InputStream inputStream, String contentType, long size) {
        String storageKey = generateStorageKey(org, fileName);
        Path filePath = uploadsDir.resolve(storageKey);
        
        try {
            // Create parent directories if needed
            Files.createDirectories(filePath.getParent());
            
            // Copy file content
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Uploaded file {} to local storage: {}", fileName, storageKey);
            return storageKey;
            
        } catch (IOException e) {
            log.error("Error uploading file to local storage: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to local storage", e);
        }
    }

    @Override
    public String generatePresignedUrl(String storageKey) {
        // For local storage, return a URL that our FileController can serve
        // contextPath already includes /api, so we use /files/ directly
        String url = baseUrl + contextPath + "/files/" + storageKey;
        log.debug("Generated local file URL: {}", url);
        return url;
    }

    @Override
    public InputStream downloadFile(String storageKey) {
        Path filePath = uploadsDir.resolve(storageKey);
        
        try {
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File not found: " + storageKey);
            }
            return new FileInputStream(filePath.toFile());
        } catch (IOException e) {
            log.error("Error downloading file from local storage: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from local storage", e);
        }
    }

    @Override
    public void deleteFile(String storageKey) {
        Path filePath = uploadsDir.resolve(storageKey);
        
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted file from local storage: {}", storageKey);
        } catch (IOException e) {
            log.error("Error deleting file from local storage: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from local storage", e);
        }
    }

    @Override
    public boolean fileExists(String storageKey) {
        Path filePath = uploadsDir.resolve(storageKey);
        return Files.exists(filePath);
    }

    /**
     * Get the absolute path for a storage key.
     * Used by FileController to serve files.
     */
    public Path getFilePath(String storageKey) {
        return uploadsDir.resolve(storageKey);
    }

    /**
     * Generate organization-specific storage key.
     * Format: {org-slug}/files/{yyyy}/{MM}/{dd}/{uuid}_{filename}
     * Example: evolotek/files/2026/02/09/abc12345_invoice.pdf
     */
    private String generateStorageKey(Organization org, String fileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitizedFileName = sanitizeFileName(fileName);
        
        // Format: {slug}/files/{yyyy}/{MM}/{dd}/{uuid}_{filename}
        return String.format("%s/files/%s/%s_%s",
                org.getSlug(), timestamp, uuid, sanitizedFileName);
    }

    /**
     * Sanitize filename for safe storage.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "unknown.pdf";
        }
        // Remove path separators and special characters
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
