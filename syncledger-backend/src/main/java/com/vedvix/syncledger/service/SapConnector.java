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
 * SAP connector — supports SAP Business One Service Layer REST API.
 * Creates AP Invoices via POST /b1s/v2/PurchaseInvoices
 *
 * For SAP S/4HANA Cloud, the OData V4 API would be used instead — this can be
 * extended later by checking a property flag.
 *
 * @author vedvix
 */
@Service
@Slf4j
public class SapConnector implements ErpConnector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    public SapConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.SAP;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        SapCredentials creds = resolveCredentials(properties);

        // Login to Service Layer to obtain session cookie
        String loginUrl = creds.baseUrl + "/b1s/v2/Login";
        String requestJson = null;

        try {
            ObjectNode loginPayload = MAPPER.createObjectNode();
            loginPayload.put("CompanyDB", creds.companyDb);
            loginPayload.put("UserName", creds.username);
            loginPayload.put("Password", creds.password);
            requestJson = MAPPER.writeValueAsString(loginPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    loginUrl, HttpMethod.POST, entity, String.class);

            return ErpSyncResult.ok(null, response.getStatusCode().value(),
                    "POST " + loginUrl, response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "SAP_LOGIN_ERROR",
                    e.getResponseBodyAsString(), requestJson, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to SAP: " + e.getMessage(), requestJson, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        SapCredentials creds = resolveCredentials(properties);
        String requestJson = null;

        try {
            // Step 1: Login to obtain session
            String sessionCookie = login(creds);

            // Step 2: Create PurchaseInvoice
            String url = creds.baseUrl + "/b1s/v2/PurchaseInvoices";
            ObjectNode payload = buildPurchaseInvoicePayload(invoice);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("SAP B1: Creating Purchase Invoice [company={}, invoiceNo={}]",
                    creds.companyDb, invoice.getInvoiceNumber());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "B1SESSION=" + sessionCookie);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();
            String docEntry = extractDocEntry(responseBody);

            log.info("SAP B1: Purchase Invoice created [DocEntry={}, invoiceNo={}]",
                    docEntry, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(docEntry, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("SAP B1 error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "SAP_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("SAP B1 connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to SAP: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Session Management ──────────────────────────────────────────────────────

    private String login(SapCredentials creds) {
        String loginUrl = creds.baseUrl + "/b1s/v2/Login";

        ObjectNode loginPayload = MAPPER.createObjectNode();
        loginPayload.put("CompanyDB", creds.companyDb);
        loginPayload.put("UserName", creds.username);
        loginPayload.put("Password", creds.password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String body = MAPPER.writeValueAsString(loginPayload);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    loginUrl, HttpMethod.POST, entity, String.class);

            JsonNode root = MAPPER.readTree(response.getBody());
            String sessionId = root.path("SessionId").asText(null);
            if (sessionId == null || sessionId.isBlank()) {
                throw new BadRequestException("SAP login succeeded but no SessionId returned");
            }
            return sessionId;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("SAP B1 login failed: " + e.getMessage());
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildPurchaseInvoicePayload(Invoice invoice) {
        ObjectNode doc = MAPPER.createObjectNode();

        // Vendor (CardCode)
        if (invoice.getSageVendorId() != null) {
            doc.put("CardCode", invoice.getSageVendorId());
        } else if (invoice.getVendorName() != null) {
            doc.put("CardCode", invoice.getVendorName());
        }

        // Dates
        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        doc.put("DocDate", invDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            doc.put("DocDueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Document number
        if (invoice.getInvoiceNumber() != null) {
            doc.put("NumAtCard", invoice.getInvoiceNumber());
        }

        // Comments
        if (invoice.getVendorName() != null) {
            doc.put("Comments", "Invoice from " + invoice.getVendorName());
        }

        // Currency
        if (invoice.getCurrency() != null) {
            doc.put("DocCurrency", invoice.getCurrency());
        }

        // Document lines
        ArrayNode lines = MAPPER.createArrayNode();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            int lineNum = 0;
            for (InvoiceLineItem li : invoice.getLineItems()) {
                lines.add(buildDocumentLine(li, lineNum++));
            }
        } else {
            lines.add(buildSingleDocumentLine(invoice));
        }
        doc.set("DocumentLines", lines);

        return doc;
    }

    private ObjectNode buildDocumentLine(InvoiceLineItem li, int lineNum) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("LineNum", lineNum);

        if (li.getGlAccountCode() != null) {
            line.put("AccountCode", li.getGlAccountCode());
        }
        if (li.getItemCode() != null) {
            line.put("ItemCode", li.getItemCode());
        }
        if (li.getDescription() != null) {
            line.put("ItemDescription", li.getDescription());
        }
        if (li.getQuantity() != null) {
            line.put("Quantity", li.getQuantity());
        }
        if (li.getUnitPrice() != null) {
            line.put("Price", li.getUnitPrice());
        }
        if (li.getLineTotal() != null) {
            line.put("LineTotal", li.getLineTotal());
        }
        if (li.getCostCenter() != null) {
            line.put("CostingCode", li.getCostCenter());
        }

        return line;
    }

    private ObjectNode buildSingleDocumentLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("LineNum", 0);
        if (invoice.getGlAccount() != null) {
            line.put("AccountCode", invoice.getGlAccount());
        }
        line.put("ItemDescription", "Invoice " + invoice.getInvoiceNumber());
        line.put("LineTotal", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractDocEntry(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("DocEntry")) {
                return root.path("DocEntry").asText(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse SAP response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                JsonNode msg = error.path("message");
                if (!msg.isMissingNode()) {
                    return msg.path("value").asText(msg.asText("Unknown SAP error"));
                }
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown SAP error";
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private record SapCredentials(String baseUrl, String username, String password, String companyDb) {}

    private SapCredentials resolveCredentials(Map<String, String> props) {
        String baseUrl = props.get("base_url");
        String username = props.get("username");
        String password = props.get("password");
        String companyDb = props.getOrDefault("company_db", "");

        if (baseUrl == null || baseUrl.isBlank())
            throw new BadRequestException("SAP Server URL is not configured.");
        if (username == null || username.isBlank())
            throw new BadRequestException("SAP Username is not configured.");
        if (password == null || password.isBlank())
            throw new BadRequestException("SAP Password is not configured.");

        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return new SapCredentials(baseUrl, username, password, companyDb);
    }
}
