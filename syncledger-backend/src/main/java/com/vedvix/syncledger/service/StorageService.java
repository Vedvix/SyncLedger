package com.vedvix.syncledger.service;

import com.vedvix.syncledger.model.Organization;

import java.io.InputStream;

/**
 * Storage service interface for file operations.
 * Implementations: S3Service (production), LocalStorageService (development)
 * 
 * @author vedvix
 */
public interface StorageService {

    /**
     * Upload invoice file with organization-specific path.
     * 
     * @param org The organization
     * @param fileName Original file name
     * @param inputStream File content
     * @param contentType MIME type
     * @param size File size in bytes
     * @return Storage key/path for the file
     */
    String uploadInvoiceFile(Organization org, String fileName, 
                             InputStream inputStream, String contentType, long size);

    /**
     * Generate URL for file download/access.
     * For S3: returns presigned URL
     * For Local: returns relative path served by controller
     * 
     * @param storageKey The storage key/path
     * @return URL to access the file
     */
    String generatePresignedUrl(String storageKey);

    /**
     * Download file from storage.
     * 
     * @param storageKey The storage key/path
     * @return InputStream of file content
     */
    InputStream downloadFile(String storageKey);

    /**
     * Delete file from storage.
     * 
     * @param storageKey The storage key/path
     */
    void deleteFile(String storageKey);

    /**
     * Check if file exists in storage.
     * 
     * @param storageKey The storage key/path
     * @return true if file exists
     */
    boolean fileExists(String storageKey);
}
