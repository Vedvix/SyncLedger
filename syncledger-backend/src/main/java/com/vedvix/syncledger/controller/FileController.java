package com.vedvix.syncledger.controller;

import com.vedvix.syncledger.service.LocalStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for serving local files in development mode.
 * Only active when storage.type=local.
 * 
 * @author vedvix
 */
@Slf4j
@RestController
@RequestMapping("/files")
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class FileController {

    @Autowired
    private LocalStorageService localStorageService;

    /**
     * Serve file from local storage.
     * Handles paths like: /api/files/organizations/evolotek/invoices/2026/02/09/abc123_invoice.pdf
     */
    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extract the file path from the request URI
        String requestUri = org.springframework.web.context.request.RequestContextHolder
                .currentRequestAttributes()
                .toString();
        
        // This is a bit of a workaround to get the full path after /api/files/
        jakarta.servlet.http.HttpServletRequest request = 
            ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes())
                .getRequest();
        
        String fullPath = request.getRequestURI();
        String basePath = "/api/files/";
        
        if (!fullPath.startsWith(basePath)) {
            return ResponseEntity.notFound().build();
        }
        
        String storageKey = fullPath.substring(basePath.length());
        
        return serveFileByKey(storageKey);
    }

    /**
     * Alternative endpoint with path as request parameter.
     */
    @GetMapping
    public ResponseEntity<Resource> serveFileByParam(@RequestParam("path") String storageKey) {
        return serveFileByKey(storageKey);
    }

    private ResponseEntity<Resource> serveFileByKey(String storageKey) {
        try {
            Path filePath = localStorageService.getFilePath(storageKey);
            
            if (!Files.exists(filePath)) {
                log.warn("File not found: {}", storageKey);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String fileName = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error serving file {}: {}", storageKey, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
