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
 * Xero connector — uses REST API with OAuth2.
 * Creates AP Invoices (ACCPAY type) via PUT /api.xro/2.0/Invoices
 *
 * @author vedvix
 */
@Service
@Slf4j
public class XeroConnector implements ErpConnector {

    private static final String XERO_API_BASE = "https://api.xero.com/api.xro/2.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    public XeroConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.XERO;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        XeroCredentials creds = resolveCredentials(properties);
        String url = XERO_API_BASE + "/Organisation";

        HttpHeaders headers = buildHeaders(creds);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "XERO_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Xero: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        XeroCredentials creds = resolveCredentials(properties);
        String url = XERO_API_BASE + "/Invoices";
        String requestJson = null;

        try {
            ObjectNode payload = buildInvoicePayload(invoice);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("Xero: Creating AP Invoice [tenantId={}, invoiceNo={}]",
                    creds.tenantId, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();

            String invoiceId = extractInvoiceId(responseBody);

            log.info("Xero: AP Invoice created [InvoiceID={}, invoiceNo={}]",
                    invoiceId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(invoiceId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("Xero error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "XERO_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (Exception e) {
            log.error("Xero connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Xero: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildInvoicePayload(Invoice invoice) {
        ObjectNode inv = MAPPER.createObjectNode();

        // Type = ACCPAY for accounts payable (vendor bills)
        inv.put("Type", "ACCPAY");
        inv.put("Status", "AUTHORISED");

        // Contact (vendor)
        ObjectNode contact = MAPPER.createObjectNode();
        if (invoice.getSageVendorId() != null) {
            contact.put("ContactID", invoice.getSageVendorId());
        } else if (invoice.getVendorName() != null) {
            contact.put("Name", invoice.getVendorName());
        }
        inv.set("Contact", contact);

        // Dates
        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        inv.put("Date", invDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            inv.put("DueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Invoice number & reference
        if (invoice.getInvoiceNumber() != null) {
            inv.put("InvoiceNumber", invoice.getInvoiceNumber());
        }
        if (invoice.getPoNumber() != null) {
            inv.put("Reference", invoice.getPoNumber());
        }

        // Currency
        if (invoice.getCurrency() != null) {
            inv.put("CurrencyCode", invoice.getCurrency());
        }

        // Line items
        ArrayNode lineItems = MAPPER.createArrayNode();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            for (InvoiceLineItem li : invoice.getLineItems()) {
                lineItems.add(buildLineItem(li));
            }
        } else {
            lineItems.add(buildSingleLine(invoice));
        }
        inv.set("LineItems", lineItems);

        return inv;
    }

    private ObjectNode buildLineItem(InvoiceLineItem li) {
        ObjectNode line = MAPPER.createObjectNode();

        if (li.getDescription() != null) {
            line.put("Description", li.getDescription());
        }
        if (li.getQuantity() != null) {
            line.put("Quantity", li.getQuantity());
        }
        if (li.getUnitPrice() != null) {
            line.put("UnitAmount", li.getUnitPrice());
        }
        if (li.getGlAccountCode() != null) {
            line.put("AccountCode", li.getGlAccountCode());
        }
        if (li.getItemCode() != null) {
            line.put("ItemCode", li.getItemCode());
        }

        return line;
    }

    private ObjectNode buildSingleLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("Description", "Invoice " + invoice.getInvoiceNumber());
        line.put("Quantity", 1);
        line.put("UnitAmount", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        if (invoice.getGlAccount() != null) {
            line.put("AccountCode", invoice.getGlAccount());
        }
        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractInvoiceId(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode invoices = root.path("Invoices");
            if (invoices.isArray() && !invoices.isEmpty()) {
                return invoices.get(0).path("InvoiceID").asText(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse Xero invoice response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("Message")) {
                return root.path("Message").asText("Unknown Xero error");
            }
            JsonNode elements = root.path("Elements");
            if (elements.isArray() && !elements.isEmpty()) {
                JsonNode validationErrors = elements.get(0).path("ValidationErrors");
                if (validationErrors.isArray() && !validationErrors.isEmpty()) {
                    return validationErrors.get(0).path("Message").asText("Xero validation error");
                }
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown Xero error";
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private static final String XERO_TOKEN_URL = "https://identity.xero.com/connect/token";

    private record XeroCredentials(String accessToken, String tenantId) {}

    private XeroCredentials resolveCredentials(Map<String, String> props) {
        String tenantId = props.get("tenant_id");
        String refreshToken = props.get("refresh_token");
        String clientId = props.get("client_id");
        String clientSecret = props.get("client_secret");

        if (tenantId == null || tenantId.isBlank())
            throw new BadRequestException("Xero Tenant ID is not configured.");
        if (refreshToken == null || refreshToken.isBlank())
            throw new BadRequestException("Xero Refresh Token is not configured. Complete the OAuth2 authorization flow.");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new BadRequestException("Xero OAuth2 client credentials are not configured.");

        String accessToken = refreshXeroAccessToken(clientId, clientSecret, refreshToken);

        return new XeroCredentials(accessToken, tenantId);
    }

    private String refreshXeroAccessToken(String clientId, String clientSecret, String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=refresh_token&refresh_token=" + refreshToken;

            ResponseEntity<String> response = restTemplate.exchange(
                    XERO_TOKEN_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode token = MAPPER.readTree(response.getBody());
            String accessToken = token.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new BadRequestException("Xero token refresh returned empty access_token");
            }
            return accessToken;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Xero token refresh failed: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Xero OAuth2 token refresh failed. Please re-authorize.");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Xero token refresh error", e);
            throw new BadRequestException("Xero OAuth2 token refresh failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(XeroCredentials creds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(creds.accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Xero-tenant-id", creds.tenantId);
        return headers;
    }
}
