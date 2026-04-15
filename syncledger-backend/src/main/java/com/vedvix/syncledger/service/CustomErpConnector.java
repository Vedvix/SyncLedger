package com.vedvix.syncledger.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.ErpType;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceLineItem;
import com.vedvix.syncledger.service.erp.ErpConnector;
import com.vedvix.syncledger.service.erp.ErpSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Custom ERP connector — sends invoice data to any REST API endpoint.
 * The API URL, auth method, and bill endpoint are all configurable via properties.
 *
 * Supports authentication types: bearer_token, api_key, basic, oauth2
 *
 * @author vedvix
 */
@Service
@Slf4j
public class CustomErpConnector implements ErpConnector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    public CustomErpConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.CUSTOM;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        CustomCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl;

        HttpHeaders headers = buildHeaders(creds);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // 401/403 means the endpoint exists but auth failed — still a valid connection test result
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "CUSTOM_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to custom ERP: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        CustomCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + creds.createBillPath;
        String requestJson = null;

        try {
            ObjectNode payload = buildGenericBillPayload(invoice);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("Custom ERP: Creating Bill [url={}, invoiceNo={}]",
                    url, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();

            // Try to extract an ID from the response (best-effort)
            String recordId = extractRecordId(responseBody);

            log.info("Custom ERP: Bill created [id={}, invoiceNo={}]",
                    recordId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(recordId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.warn("Custom ERP error [invoiceNo={}, status={}, body={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorBody);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "CUSTOM_API_ERROR",
                    errorBody, requestJson, errorBody);
        } catch (Exception e) {
            log.error("Custom ERP connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to custom ERP: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Generic Bill Payload ────────────────────────────────────────────────────

    /**
     * Builds a generic, self-describing JSON payload that covers common AP bill fields.
     * The receiving API should be able to map these standard fields to its own schema.
     */
    private ObjectNode buildGenericBillPayload(Invoice invoice) {
        ObjectNode bill = MAPPER.createObjectNode();

        // Vendor
        ObjectNode vendor = MAPPER.createObjectNode();
        if (invoice.getSageVendorId() != null) vendor.put("id", invoice.getSageVendorId());
        if (invoice.getVendorName() != null) vendor.put("name", invoice.getVendorName());
        bill.set("vendor", vendor);

        // Header
        if (invoice.getInvoiceNumber() != null) bill.put("invoiceNumber", invoice.getInvoiceNumber());

        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        bill.put("invoiceDate", invDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            bill.put("dueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (invoice.getPoNumber() != null) bill.put("poNumber", invoice.getPoNumber());
        if (invoice.getCurrency() != null) bill.put("currency", invoice.getCurrency());
        if (invoice.getTotalAmount() != null) bill.put("totalAmount", invoice.getTotalAmount());
        if (invoice.getSubtotal() != null) bill.put("subtotalAmount", invoice.getSubtotal());
        if (invoice.getTaxAmount() != null) bill.put("taxAmount", invoice.getTaxAmount());
        if (invoice.getGlAccount() != null) bill.put("glAccount", invoice.getGlAccount());
        if (invoice.getCostCenter() != null) bill.put("costCenter", invoice.getCostCenter());

        // Line items
        ArrayNode lineItems = MAPPER.createArrayNode();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            for (InvoiceLineItem li : invoice.getLineItems()) {
                ObjectNode line = MAPPER.createObjectNode();
                line.put("lineNumber", li.getLineNumber());
                if (li.getDescription() != null) line.put("description", li.getDescription());
                if (li.getItemCode() != null) line.put("itemCode", li.getItemCode());
                if (li.getUnit() != null) line.put("unit", li.getUnit());
                if (li.getQuantity() != null) line.put("quantity", li.getQuantity());
                if (li.getUnitPrice() != null) line.put("unitPrice", li.getUnitPrice());
                if (li.getTaxRate() != null) line.put("taxRate", li.getTaxRate());
                if (li.getTaxAmount() != null) line.put("taxAmount", li.getTaxAmount());
                if (li.getDiscountAmount() != null) line.put("discountAmount", li.getDiscountAmount());
                if (li.getLineTotal() != null) line.put("lineTotal", li.getLineTotal());
                if (li.getGlAccountCode() != null) line.put("glAccountCode", li.getGlAccountCode());
                if (li.getCostCenter() != null) line.put("costCenter", li.getCostCenter());
                lineItems.add(line);
            }
        }
        bill.set("lineItems", lineItems);

        return bill;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractRecordId(String responseBody) {
        if (responseBody == null) return null;
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            // Try common ID field names
            for (String field : new String[]{"id", "Id", "ID", "record_id", "recordId",
                    "bill_id", "billId", "invoice_id", "invoiceId", "docId"}) {
                if (root.has(field)) {
                    return root.path(field).asText(null);
                }
            }
            // Try nested under "data"
            JsonNode data = root.path("data");
            if (!data.isMissingNode()) {
                for (String field : new String[]{"id", "Id", "ID", "record_id"}) {
                    if (data.has(field)) {
                        return data.path(field).asText(null);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private record CustomCredentials(String baseUrl, String createBillPath,
                                      String authType, String apiKey,
                                      String username, String password) {}

    private CustomCredentials resolveCredentials(Map<String, String> props) {
        String baseUrl = props.get("base_url");
        String createBillPath = props.getOrDefault("create_bill_path", "/api/v1/bills");
        String authType = props.getOrDefault("auth_type", "bearer_token");
        String apiKey = props.getOrDefault("api_key", "");
        String username = props.getOrDefault("username", "");
        String password = props.getOrDefault("password", "");

        if (baseUrl == null || baseUrl.isBlank())
            throw new BadRequestException("Custom ERP API Base URL is not configured.");

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return new CustomCredentials(baseUrl, createBillPath, authType, apiKey, username, password);
    }

    private HttpHeaders buildHeaders(CustomCredentials creds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        switch (creds.authType.toLowerCase()) {
            case "bearer_token" -> {
                if (creds.apiKey != null && !creds.apiKey.isBlank()) {
                    headers.setBearerAuth(creds.apiKey);
                }
            }
            case "api_key" -> {
                if (creds.apiKey != null && !creds.apiKey.isBlank()) {
                    headers.set("X-API-Key", creds.apiKey);
                }
            }
            case "basic" -> {
                if (creds.username != null && !creds.username.isBlank()) {
                    headers.setBasicAuth(creds.username,
                            creds.password != null ? creds.password : "");
                }
            }
            case "oauth2" -> {
                // For OAuth2, api_key holds the access token
                if (creds.apiKey != null && !creds.apiKey.isBlank()) {
                    headers.setBearerAuth(creds.apiKey);
                }
            }
            default -> {
                if (creds.apiKey != null && !creds.apiKey.isBlank()) {
                    headers.setBearerAuth(creds.apiKey);
                }
            }
        }

        return headers;
    }
}
