package com.vedvix.syncledger.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vedvix.syncledger.model.*;
import com.vedvix.syncledger.repository.AiUsageLogRepository;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final AiUsageLogRepository aiUsageLogRepository;
    private final StorageService storageService;
    private final VendorService vendorService;
    private final InvoiceAuditService invoiceAuditService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RuntimeConfigService runtimeConfigService;

    /**
     * Queue invoice for processing.
     */
    @Transactional
    public Invoice queueForProcessing(Organization org, String s3Key, String fileName,
                                       String emailId, String emailFrom, String emailSubject,
                                       LocalDateTime emailReceivedAt) {
        // Generate the URL for serving this file
        String s3Url = storageService.generatePresignedUrl(s3Key);
        
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
                .s3Url(s3Url)
                .sourceEmailId(emailId)
                .sourceEmailFrom(emailFrom)
                .sourceEmailSubject(emailSubject)
                .sourceEmailReceivedAt(emailReceivedAt)
                .receivedDate(LocalDate.now())
                .build();
        
        invoiceRepository.save(invoice);
        log.info("Created pending invoice record: {}", invoice.getId());

        // Audit: received via email
        invoiceAuditService.logReceivedViaEmail(invoice, emailFrom, emailSubject);
        
        // Trigger async extraction
        processInvoiceAsync(invoice.getId());
        
        return invoice;
    }

    /**
     * Upload a PDF, store it, create invoice record, and trigger extraction.
     */
    @Transactional
    public Invoice uploadAndProcess(Organization org, MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.pdf";
        
        // Upload to storage (S3 or local)
        String s3Key = storageService.uploadInvoiceFile(
                org, fileName, file.getInputStream(), file.getContentType(), file.getSize());
        String s3Url = storageService.generatePresignedUrl(s3Key);
        
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
                .s3Url(s3Url)
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .receivedDate(LocalDate.now())
                .build();
        
        invoiceRepository.save(invoice);
        log.info("Created invoice from upload: {}, s3Key: {}", invoice.getId(), s3Key);

        // Audit: received via upload
        invoiceAuditService.logReceivedViaUpload(invoice, null);
        
        // Trigger extraction
        processInvoiceAsync(invoice.getId());
        
        return invoice;
    }

    /**
     * Process invoice through PDF extraction service.
     */
    @Transactional
    public void processInvoiceAsync(Long invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

            log.info("Processing invoice {} through PDF service, s3Key: {}", invoiceId, invoice.getS3Key());

            // Audit: extraction started
            invoiceAuditService.logExtractionStarted(invoice);
            
            // Get URL for PDF service to download (presigned for S3, direct for local)
            String fileUrl = storageService.generatePresignedUrl(invoice.getS3Key());
            log.debug("Generated file URL for PDF service: {}", fileUrl);
            
            // Call PDF extraction service
            String pdfServiceUrl = runtimeConfigService.getString("pdf-service.url", "http://localhost:8001");
            String extractionUrl = pdfServiceUrl + "/api/v1/extract";
            log.info("Calling PDF service at: {}", extractionUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Build AI config from runtime configs
            AiConfigPayload aiConfig = buildAiConfig();
            
            String requestBody = objectMapper.writeValueAsString(new ExtractionRequest(
                fileUrl,
                invoice.getOriginalFileName(),
                invoice.getOrganization().getId(),
                invoiceId,
                aiConfig
            ));
            
            log.debug("PDF service request body: {}", requestBody);
            
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                extractionUrl,
                HttpMethod.POST,
                request,
                String.class
            );
            
            log.info("PDF service response status: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                // Parse response and update invoice
                JsonNode result = objectMapper.readTree(response.getBody());
                updateInvoiceFromExtraction(invoice, result);
                log.info("Successfully processed invoice: {}", invoiceId);
            } else {
                log.error("PDF service returned error: {}, body: {}", response.getStatusCode(), response.getBody());
                markInvoiceAsFailedExtraction(invoice, "Extraction service returned: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error processing invoice {}: {}", invoiceId, e.getMessage(), e);
            invoiceRepository.findById(invoiceId).ifPresent(inv -> 
                markInvoiceAsFailedExtraction(inv, e.getMessage())
            );
        }
    }

    /**
     * Update invoice with extraction results.
     * 
     * Python microservice response format:
     * {
     *   "data": {
     *     "invoice_number": "...",
     *     "po_number": "...",
     *     "vendor": { "name": "...", "address": "...", "email": "...", "phone": "..." },
     *     "invoice_date": "2025-11-24",
     *     "due_date": "2025-11-28",
     *     "subtotal": 3654.60,
     *     "tax_amount": 0,
     *     "total_amount": 3654.60,
     *     "gl_account": "5100",
     *     "project": "O049425",
     *     "item_category": "Roofing",
     *     "location": "...",
     *     "cost_center": "...",
     *     "mapping_profile_id": "default-subcontractor",
     *     "confidence_score": 1.0,
     *     "line_items": [ { "line_number": 1, "description": "...", "quantity": 13.16, ... } ]
     *   },
     *   "extraction_method": "pdfplumber",
     *   "page_count": 2,
     *   "mapping_info": { "gl_account": "5100", "project": "O049425", "item": "Roofing", ... }
     * }
     */
    private void updateInvoiceFromExtraction(Invoice invoice, JsonNode result) {
        try {
            JsonNode data = result.get("data");
            
            // ── Invoice identification ────────────────────────────────────
            setIfPresent(data, "invoice_number", v -> invoice.setInvoiceNumber(v.asText()));
            setIfPresent(data, "po_number", v -> invoice.setPoNumber(v.asText()));
            
            // ── Vendor info (nested under data.vendor) ────────────────────
            JsonNode vendor = data.get("vendor");
            if (vendor != null && !vendor.isNull()) {
                setIfPresent(vendor, "name", v -> invoice.setVendorName(v.asText()));
                setIfPresent(vendor, "address", v -> invoice.setVendorAddress(v.asText()));
                setIfPresent(vendor, "email", v -> invoice.setVendorEmail(v.asText()));
                setIfPresent(vendor, "phone", v -> invoice.setVendorPhone(v.asText()));
                setIfPresent(vendor, "tax_id", v -> invoice.setVendorTaxId(v.asText()));
            }
            
            // ── Auto-link to Vendor entity ────────────────────────────────
            try {
                com.vedvix.syncledger.model.Vendor matchedVendor = vendorService.findOrCreateVendor(
                        invoice.getOrganizationId(),
                        invoice.getVendorName(),
                        invoice.getVendorAddress(),
                        invoice.getVendorEmail(),
                        invoice.getVendorPhone(),
                        invoice.getVendorTaxId()
                );
                if (matchedVendor != null) {
                    invoice.setVendor(matchedVendor);
                    log.info("Linked invoice {} to vendor {} (id={})", 
                            invoice.getId(), matchedVendor.getName(), matchedVendor.getId());
                    invoiceAuditService.logVendorLinked(invoice, matchedVendor.getName(), matchedVendor.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to auto-link vendor for invoice {}: {}", invoice.getId(), e.getMessage());
            }
            
            // ── Financial ─────────────────────────────────────────────────
            setIfPresent(data, "subtotal", v -> invoice.setSubtotal(toBigDecimal(v)));
            setIfPresent(data, "tax_amount", v -> invoice.setTaxAmount(toBigDecimal(v)));
            setIfPresent(data, "total_amount", v -> invoice.setTotalAmount(toBigDecimal(v)));
            setIfPresent(data, "currency", v -> invoice.setCurrency(v.asText()));
            
            // ── Dates ─────────────────────────────────────────────────────
            setIfPresent(data, "invoice_date", v -> invoice.setInvoiceDate(LocalDate.parse(v.asText())));
            setIfPresent(data, "due_date", v -> invoice.setDueDate(LocalDate.parse(v.asText())));
            
            // ── Mapping fields (gl_account, project, item, etc.) ──────────
            setIfPresent(data, "gl_account", v -> invoice.setGlAccount(v.asText()));
            setIfPresent(data, "project", v -> invoice.setProject(v.asText()));
            setIfPresent(data, "item_category", v -> invoice.setItemCategory(v.asText()));
            setIfPresent(data, "location", v -> invoice.setLocation(v.asText()));
            setIfPresent(data, "cost_center", v -> invoice.setCostCenter(v.asText()));
            setIfPresent(data, "mapping_profile_id", v -> invoice.setMappingProfileId(v.asText()));
            
            // Also check mapping_info at result level for fallback
            JsonNode mappingInfo = result.get("mapping_info");
            if (mappingInfo != null && !mappingInfo.isNull()) {
                if (invoice.getGlAccount() == null)
                    setIfPresent(mappingInfo, "gl_account", v -> invoice.setGlAccount(v.asText()));
                if (invoice.getProject() == null)
                    setIfPresent(mappingInfo, "project", v -> invoice.setProject(v.asText()));
                if (invoice.getItemCategory() == null)
                    setIfPresent(mappingInfo, "item", v -> invoice.setItemCategory(v.asText()));
                
                // Save field mapping trace for the reviewer UI
                JsonNode fieldMappingsNode = mappingInfo.get("field_mappings");
                if (fieldMappingsNode != null && fieldMappingsNode.isArray()) {
                    invoice.setFieldMappings(fieldMappingsNode.toString());
                }
            }
            
            // ── Confidence and review flags ───────────────────────────────
            if (data.has("confidence_score") && !data.get("confidence_score").isNull()) {
                BigDecimal confidence = toBigDecimal(data.get("confidence_score"));
                BigDecimal autoApprovalConfidenceThreshold = runtimeConfigService.getDecimal(
                        "invoice.auto-approval.confidence-threshold", new BigDecimal("0.87"));
                invoice.setConfidenceScore(confidence);
                invoice.setRequiresManualReview(confidence.compareTo(autoApprovalConfidenceThreshold) < 0);
                log.debug("Invoice confidence: {}, threshold: {}, requires review: {}", 
                    confidence, autoApprovalConfidenceThreshold, invoice.getRequiresManualReview());
            }
            
            // ── Line items ────────────────────────────────────────────────
            JsonNode lineItemsNode = data.get("line_items");
            if (lineItemsNode != null && lineItemsNode.isArray() && lineItemsNode.size() > 0) {
                // Clear existing line items
                invoice.getLineItems().clear();
                
                for (JsonNode itemNode : lineItemsNode) {
                    InvoiceLineItem lineItem = InvoiceLineItem.builder()
                            .lineNumber(itemNode.has("line_number") ? itemNode.get("line_number").asInt() : 0)
                            .description(getTextOrNull(itemNode, "description"))
                            .itemCode(getTextOrNull(itemNode, "item_code"))
                            .unit(getTextOrNull(itemNode, "unit"))
                            .quantity(itemNode.has("quantity") && !itemNode.get("quantity").isNull()
                                    ? toBigDecimal(itemNode.get("quantity")) : null)
                            .unitPrice(itemNode.has("unit_price") && !itemNode.get("unit_price").isNull()
                                    ? toBigDecimal(itemNode.get("unit_price")) : null)
                            .lineTotal(itemNode.has("line_total") && !itemNode.get("line_total").isNull()
                                    ? toBigDecimal(itemNode.get("line_total")) : BigDecimal.ZERO)
                            .taxAmount(itemNode.has("tax_amount") && !itemNode.get("tax_amount").isNull()
                                    ? toBigDecimal(itemNode.get("tax_amount")) : null)
                            .glAccountCode(getTextOrNull(itemNode, "gl_account_code"))
                            .costCenter(getTextOrNull(itemNode, "cost_center"))
                            .build();
                    invoice.addLineItem(lineItem);
                }
                log.info("Saved {} line items for invoice {}", invoice.getLineItems().size(), invoice.getId());
            }
            
            // ── Extraction metadata (from top-level result, not data) ─────
            if (result.has("extraction_method") && !result.get("extraction_method").isNull()) {
                invoice.setExtractionMethod(result.get("extraction_method").asText());
            } else {
                invoice.setExtractionMethod("ocr");
            }
            if (result.has("page_count") && !result.get("page_count").isNull()) {
                invoice.setPageCount(result.get("page_count").asInt());
            }
            invoice.setExtractedAt(LocalDateTime.now());
            
            // ── AI usage metadata (from top-level result) ─────────────────
            String aiTier = null;
            Integer inputTokens = null;
            Integer outputTokens = null;
            Integer totalTokens = null;
            BigDecimal aiCost = null;

            if (result.has("ai_tier_used") && !result.get("ai_tier_used").isNull()) {
                aiTier = result.get("ai_tier_used").asText();
                invoice.setAiTierUsed(aiTier);
            }
            if (result.has("ai_model_name") && !result.get("ai_model_name").isNull()) {
                invoice.setAiModelName(result.get("ai_model_name").asText());
            }
            if (result.has("ai_cost_usd") && !result.get("ai_cost_usd").isNull()) {
                aiCost = toBigDecimal(result.get("ai_cost_usd"));
                invoice.setAiCostUsd(aiCost);
            }
            JsonNode tokenUsage = result.get("ai_token_usage");
            if (tokenUsage != null && !tokenUsage.isNull()) {
                if (tokenUsage.has("input") && !tokenUsage.get("input").isNull()) {
                    inputTokens = tokenUsage.get("input").asInt();
                    invoice.setAiInputTokens(inputTokens);
                }
                if (tokenUsage.has("output") && !tokenUsage.get("output").isNull()) {
                    outputTokens = tokenUsage.get("output").asInt();
                    invoice.setAiOutputTokens(outputTokens);
                }
                totalTokens = (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
                invoice.setAiTotalTokens(totalTokens);
            }

            // Persist AI usage log for billing/metering
            try {
                Integer processingTimeMs = null;
                if (result.has("processing_time_ms") && !result.get("processing_time_ms").isNull()) {
                    processingTimeMs = result.get("processing_time_ms").asInt();
                }
                AiUsageLog usageLog = AiUsageLog.builder()
                        .organization(invoice.getOrganization())
                        .invoice(invoice)
                        .aiTier(aiTier != null ? aiTier : "unknown")
                        .modelName(invoice.getAiModelName())
                        .inputTokens(inputTokens != null ? inputTokens : 0)
                        .outputTokens(outputTokens != null ? outputTokens : 0)
                        .totalTokens(totalTokens != null ? totalTokens : 0)
                        .estimatedCostUsd(aiCost != null ? aiCost : BigDecimal.ZERO)
                        .processingTimeMs(processingTimeMs)
                        .success(true)
                        .build();
                aiUsageLogRepository.save(usageLog);
                log.info("Saved AI usage log for invoice {} — tier={}, tokens={}, cost={}",
                        invoice.getId(), aiTier, totalTokens, aiCost);
            } catch (Exception ex) {
                log.warn("Failed to save AI usage log for invoice {}: {}", invoice.getId(), ex.getMessage());
            }

            // ── Status based on confidence ────────────────────────────────
            if (invoice.getRequiresManualReview() != null && invoice.getRequiresManualReview()) {
                invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
            } else {
                invoice.setStatus(InvoiceStatus.PENDING);
            }
            
            // Store raw extracted data for debugging
            invoice.setRawExtractedData(result.toString());
            
            invoiceRepository.save(invoice);
            log.info("Updated invoice {} with extraction results — vendor={}, total={}, confidence={}, lineItems={}", 
                    invoice.getId(), invoice.getVendorName(), invoice.getTotalAmount(), 
                    invoice.getConfidenceScore(), invoice.getLineItems().size());

            // Audit: extraction completed
            invoiceAuditService.logExtractionCompleted(invoice,
                    invoice.getConfidenceScore(),
                    invoice.getExtractionMethod(),
                    invoice.getLineItems() != null ? invoice.getLineItems().size() : 0);
            
        } catch (Exception e) {
            log.error("Error updating invoice from extraction: {}", e.getMessage(), e);
            markInvoiceAsFailedExtraction(invoice, "Failed to parse extraction results: " + e.getMessage());
        }
    }
    
    /** Helper: set field if JSON node has non-null value */
    private void setIfPresent(JsonNode node, String field, java.util.function.Consumer<JsonNode> setter) {
        if (node.has(field) && !node.get(field).isNull()) {
            setter.accept(node.get(field));
        }
    }
    
    /** Helper: convert JSON number/string to BigDecimal */
    private BigDecimal toBigDecimal(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue();
        }
        return new BigDecimal(node.asText().replace(",", "").replace("$", ""));
    }
    
    /** Helper: get text or null */
    private String getTextOrNull(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            String text = node.get(field).asText();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    /**
     * Mark invoice as failed extraction.
     */
    private void markInvoiceAsFailedExtraction(Invoice invoice, String errorMessage) {
        invoice.setStatus(InvoiceStatus.UNDER_REVIEW);
        invoice.setRequiresManualReview(true);
        invoice.setReviewNotes("Extraction failed: " + errorMessage);
        invoiceRepository.save(invoice);

        // Audit: extraction failed
        invoiceAuditService.logExtractionFailed(invoice, errorMessage);
    }

    /**
     * Build AI config payload from runtime configs.
     * Only includes non-empty values so the PDF service falls back to its env vars for unset keys.
     */
    private AiConfigPayload buildAiConfig() {
        AiConfigPayload config = new AiConfigPayload();
        
        String provider = runtimeConfigService.getString("ai.provider", "");
        if (!provider.isEmpty()) config.setProvider(provider);
        
        String apiKey = runtimeConfigService.getString("ai.openai.api-key", "");
        if (!apiKey.isEmpty()) config.setApiKey(apiKey);
        
        String visionModel = runtimeConfigService.getString("ai.openai.vision-model", "");
        if (!visionModel.isEmpty()) config.setVisionModel(visionModel);
        
        String textModel = runtimeConfigService.getString("ai.openai.text-model", "");
        if (!textModel.isEmpty()) config.setTextModel(textModel);
        
        config.setEnableVision(runtimeConfigService.getBoolean("ai.enable-vision", true));
        config.setEnableTextLlm(runtimeConfigService.getBoolean("ai.enable-text-llm", true));
        config.setEnableCrossValidation(runtimeConfigService.getBoolean("ai.enable-cross-validation", true));
        
        return config;
    }

    // Inner class for extraction request - uses snake_case for Python API
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ExtractionRequest {
        @JsonProperty("file_url")
        private String fileUrl;
        
        @JsonProperty("file_name")
        private String fileName;
        
        @JsonProperty("organization_id")
        private Long organizationId;
        
        @JsonProperty("invoice_id")
        private Long invoiceId;
        
        @JsonProperty("ai_config")
        private AiConfigPayload aiConfig;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class AiConfigPayload {
        private String provider;
        @JsonProperty("api_key")
        private String apiKey;
        @JsonProperty("vision_model")
        private String visionModel;
        @JsonProperty("text_model")
        private String textModel;
        @JsonProperty("enable_vision")
        private Boolean enableVision;
        @JsonProperty("enable_text_llm")
        private Boolean enableTextLlm;
        @JsonProperty("enable_cross_validation")
        private Boolean enableCrossValidation;
    }
}
