package com.vedvix.syncledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for processing invoices through the PDF extraction microservice.
 * Handles queuing and result processing.
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceProcessingService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final S3Service s3Service;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${pdf-service.url:http://localhost:8001}")
    private String pdfServiceUrl;

    /**
     * Queue invoice for processing.
     */
    public Invoice queueForProcessing(Organization org, String s3Key, String fileName,
                                       String emailId, String emailFrom, String emailSubject,
                                       LocalDateTime emailReceivedAt) {
        // Create invoice record in PENDING state
        Invoice invoice = Invoice.builder()
                .organization(org)
                .invoiceNumber("PENDING-" + System.currentTimeMillis())
                .vendorName("Pending Extraction")
                .subtotal(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .invoiceDate(LocalDate.now())
                .status(InvoiceStatus.PENDING)
                .originalFileName(fileName)
                .s3Key(s3Key)
                .sourceEmailId(emailId)
                .sourceEmailFrom(emailFrom)
                .sourceEmailSubject(emailSubject)
                .sourceEmailReceivedAt(emailReceivedAt)
                .receivedDate(LocalDate.now())
                .build();
        
        invoiceRepository.save(invoice);
        log.info("Created pending invoice record: {}", invoice.getId());
        
        // Trigger async extraction
        processInvoiceAsync(invoice.getId());
        
        return invoice;
    }

    /**
     * Process invoice through PDF extraction service.
     */
    public void processInvoiceAsync(Long invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            // Get presigned URL for PDF service to download
            String presignedUrl = s3Service.generatePresignedUrl(invoice.getS3Key());
            
            // Call PDF extraction service
            String extractionUrl = pdfServiceUrl + "/api/v1/extract";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String requestBody = objectMapper.writeValueAsString(new ExtractionRequest(
                presignedUrl,
                invoice.getOriginalFileName(),
                invoice.getOrganization().getId(),
                invoiceId
            ));
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                extractionUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse response and update invoice
                JsonNode result = objectMapper.readTree(response.getBody());
                updateInvoiceFromExtraction(invoice, result);
                log.info("Successfully processed invoice: {}", invoiceId);
            } else {
                markInvoiceAsFailedExtraction(invoice, "Extraction service returned: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error processing invoice {}: {}", invoiceId, e.getMessage());
            invoiceRepository.findById(invoiceId).ifPresent(inv -> 
                markInvoiceAsFailedExtraction(inv, e.getMessage())
            );
        }
    }

    /**
     * Update invoice with extraction results.
     */
    private void updateInvoiceFromExtraction(Invoice invoice, JsonNode result) {
        try {
            JsonNode data = result.get("data");
            
            // Update invoice fields from extraction
            if (data.has("invoice_number") && !data.get("invoice_number").isNull()) {
                invoice.setInvoiceNumber(data.get("invoice_number").asText());
            }
            if (data.has("vendor_name") && !data.get("vendor_name").isNull()) {
                invoice.setVendorName(data.get("vendor_name").asText());
            }
            if (data.has("vendor_address") && !data.get("vendor_address").isNull()) {
                invoice.setVendorAddress(data.get("vendor_address").asText());
            }
            if (data.has("subtotal") && !data.get("subtotal").isNull()) {
                invoice.setSubtotal(new BigDecimal(data.get("subtotal").asText()));
            }
            if (data.has("tax_amount") && !data.get("tax_amount").isNull()) {
                invoice.setTaxAmount(new BigDecimal(data.get("tax_amount").asText()));
            }
            if (data.has("total_amount") && !data.get("total_amount").isNull()) {
                invoice.setTotalAmount(new BigDecimal(data.get("total_amount").asText()));
            }
            if (data.has("invoice_date") && !data.get("invoice_date").isNull()) {
                invoice.setInvoiceDate(LocalDate.parse(data.get("invoice_date").asText()));
            }
            if (data.has("due_date") && !data.get("due_date").isNull()) {
                invoice.setDueDate(LocalDate.parse(data.get("due_date").asText()));
            }
            if (data.has("po_number") && !data.get("po_number").isNull()) {
                invoice.setPoNumber(data.get("po_number").asText());
            }
            if (data.has("currency") && !data.get("currency").isNull()) {
                invoice.setCurrency(data.get("currency").asText());
            }
            
            // Confidence and review flags
            if (data.has("confidence_score") && !data.get("confidence_score").isNull()) {
                BigDecimal confidence = new BigDecimal(data.get("confidence_score").asText());
                invoice.setConfidenceScore(confidence);
                // Flag for manual review if confidence is low
                invoice.setRequiresManualReview(confidence.compareTo(new BigDecimal("0.8")) < 0);
            }
            
            // Extraction metadata
            invoice.setExtractionMethod(data.has("extraction_method") 
                ? data.get("extraction_method").asText() : "ocr");
            invoice.setExtractedAt(LocalDateTime.now());
            
            // Set status based on confidence
            if (invoice.getRequiresManualReview()) {
                invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
            } else {
                invoice.setStatus(InvoiceStatus.PENDING);
            }
            
            // Store raw extracted data
            invoice.setRawExtractedData(result.toString());
            
            invoiceRepository.save(invoice);
            log.info("Updated invoice {} with extraction results", invoice.getId());
            
        } catch (Exception e) {
            log.error("Error updating invoice from extraction: {}", e.getMessage());
            markInvoiceAsFailedExtraction(invoice, "Failed to parse extraction results: " + e.getMessage());
        }
    }

    /**
     * Mark invoice as failed extraction.
     */
    private void markInvoiceAsFailedExtraction(Invoice invoice, String errorMessage) {
        invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
        invoice.setRequiresManualReview(true);
        invoice.setReviewNotes("Extraction failed: " + errorMessage);
        invoiceRepository.save(invoice);
    }

    // Inner class for extraction request
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ExtractionRequest {
        private String fileUrl;
        private String fileName;
        private Long organizationId;
        private Long invoiceId;
    }
}
