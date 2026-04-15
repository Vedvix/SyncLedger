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
import java.util.Map;

/**
 * Oracle Fusion Cloud ERP connector — uses REST API with basic auth.
 * Creates AP Invoices via POST /fscmRestApi/resources/latest/invoices
 *
 * @author vedvix
 */
@Service
@Slf4j
public class OracleConnector implements ErpConnector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    public OracleConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(90))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.ORACLE;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        OracleCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/fscmRestApi/resources/latest/invoices?limit=1";

        HttpHeaders headers = buildHeaders(creds);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "ORACLE_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Oracle Fusion: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        OracleCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/fscmRestApi/resources/latest/invoices";
        String requestJson = null;

        try {
            ObjectNode payload = buildInvoicePayload(invoice, creds);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("Oracle Fusion: Creating AP Invoice [host={}, invoiceNo={}]",
                    creds.baseUrl, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();
            String invoiceId = extractInvoiceId(responseBody);

            log.info("Oracle Fusion: AP Invoice created [InvoiceId={}, invoiceNo={}]",
                    invoiceId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(invoiceId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("Oracle Fusion error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "ORACLE_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (Exception e) {
            log.error("Oracle Fusion connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Oracle Fusion: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildInvoicePayload(Invoice invoice, OracleCredentials creds) {
        ObjectNode inv = MAPPER.createObjectNode();

        // Invoice type (Standard)
        inv.put("InvoiceType", "Standard");

        // Supplier (vendor)
        if (invoice.getSageVendorId() != null) {
            inv.put("Supplier", invoice.getSageVendorId());
        } else if (invoice.getVendorName() != null) {
            inv.put("Supplier", invoice.getVendorName());
        }

        // Invoice number
        if (invoice.getInvoiceNumber() != null) {
            inv.put("InvoiceNumber", invoice.getInvoiceNumber());
        }

        // Dates
        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        inv.put("InvoiceDate", invDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            inv.put("PaymentDueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Amount
        if (invoice.getTotalAmount() != null) {
            inv.put("InvoiceAmount", invoice.getTotalAmount());
        }

        // Currency
        if (invoice.getCurrency() != null) {
            inv.put("InvoiceCurrencyCode", invoice.getCurrency());
        }

        // Business Unit
        if (creds.businessUnit != null && !creds.businessUnit.isBlank()) {
            inv.put("BusinessUnit", creds.businessUnit);
        }

        // Description
        if (invoice.getVendorName() != null) {
            inv.put("Description", "Invoice from " + invoice.getVendorName());
        }

        // PO number
        if (invoice.getPoNumber() != null) {
            inv.put("PONumber", invoice.getPoNumber());
        }

        // Distribution lines
        ArrayNode lines = MAPPER.createArrayNode();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            int lineNum = 1;
            for (InvoiceLineItem li : invoice.getLineItems()) {
                lines.add(buildDistributionLine(li, lineNum++));
            }
        } else {
            lines.add(buildSingleDistributionLine(invoice));
        }
        inv.set("invoiceLines", lines);

        return inv;
    }

    private ObjectNode buildDistributionLine(InvoiceLineItem li, int lineNum) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("LineNumber", lineNum);
        line.put("LineType", "Item");

        if (li.getLineTotal() != null) {
            line.put("Amount", li.getLineTotal());
        }
        if (li.getDescription() != null) {
            line.put("Description", li.getDescription());
        }
        if (li.getGlAccountCode() != null) {
            line.put("DistributionAccount", li.getGlAccountCode());
        }
        if (li.getQuantity() != null) {
            line.put("Quantity", li.getQuantity());
        }
        if (li.getUnitPrice() != null) {
            line.put("UnitPrice", li.getUnitPrice());
        }

        return line;
    }

    private ObjectNode buildSingleDistributionLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("LineNumber", 1);
        line.put("LineType", "Item");
        line.put("Amount", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        line.put("Description", "Invoice " + invoice.getInvoiceNumber());
        if (invoice.getGlAccount() != null) {
            line.put("DistributionAccount", invoice.getGlAccount());
        }
        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractInvoiceId(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("InvoiceId")) {
                return root.path("InvoiceId").asText(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse Oracle invoice response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("title")) {
                return root.path("detail").asText(root.path("title").asText("Unknown Oracle error"));
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown Oracle Fusion error";
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private record OracleCredentials(String baseUrl, String username, String password,
                                      String businessUnit) {}

    private OracleCredentials resolveCredentials(Map<String, String> props) {
        String baseUrl = props.get("base_url");
        String username = props.get("username");
        String password = props.get("password");
        String businessUnit = props.getOrDefault("business_unit", "");

        if (baseUrl == null || baseUrl.isBlank())
            throw new BadRequestException("Oracle Fusion Cloud Instance URL is not configured.");
        if (username == null || username.isBlank())
            throw new BadRequestException("Oracle Fusion Username is not configured.");
        if (password == null || password.isBlank())
            throw new BadRequestException("Oracle Fusion Password is not configured.");

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return new OracleCredentials(baseUrl, username, password, businessUnit);
    }

    private HttpHeaders buildHeaders(OracleCredentials creds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(creds.username, creds.password);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
