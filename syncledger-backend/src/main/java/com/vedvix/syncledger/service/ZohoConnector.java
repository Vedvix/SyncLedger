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
 * Zoho Books connector — uses REST API with OAuth2.
 * Creates AP Bills via POST /books/v3/bills?organization_id={orgId}
 *
 * Zoho data centers:
 *   US → zohoapis.com
 *   EU → zohoapis.eu
 *   IN → zohoapis.in
 *   AU → zohoapis.com.au
 *   JP → zohoapis.jp
 *
 * @author vedvix
 */
@Service
@Slf4j
public class ZohoConnector implements ErpConnector {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> REGION_DOMAINS = Map.of(
            "US", "https://www.zohoapis.com",
            "EU", "https://www.zohoapis.eu",
            "IN", "https://www.zohoapis.in",
            "AU", "https://www.zohoapis.com.au",
            "JP", "https://www.zohoapis.jp"
    );

    private final RestTemplate restTemplate;

    public ZohoConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.ZOHO;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        ZohoCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/books/v3/organizations";

        HttpHeaders headers = buildHeaders(creds.accessToken);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "ZOHO_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Zoho Books: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        ZohoCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/books/v3/bills?organization_id=" + creds.organizationId;
        String requestJson = null;

        try {
            ObjectNode payload = buildBillPayload(invoice, creds);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("Zoho Books: Creating Bill [orgId={}, invoiceNo={}]",
                    creds.organizationId, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds.accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();

            String billId = extractBillId(responseBody);

            log.info("Zoho Books: Bill created [billId={}, invoiceNo={}]",
                    billId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(billId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("Zoho Books error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "ZOHO_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (Exception e) {
            log.error("Zoho Books connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Zoho Books: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildBillPayload(Invoice invoice, ZohoCredentials creds) {
        ObjectNode bill = MAPPER.createObjectNode();

        // Vendor (required) — use vendor ID if available, otherwise vendor name lookup
        if (invoice.getSageVendorId() != null) {
            bill.put("vendor_id", invoice.getSageVendorId());
        } else if (invoice.getVendorName() != null) {
            // Zoho requires vendor_id; if only name is available, it must be pre-created
            bill.put("vendor_id", invoice.getVendorName());
        }

        // Bill number (required)
        if (invoice.getInvoiceNumber() != null) {
            bill.put("bill_number", invoice.getInvoiceNumber());
        }

        // Dates
        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        bill.put("date", invDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            bill.put("due_date", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Reference / PO number
        if (invoice.getPoNumber() != null) {
            bill.put("reference_number", invoice.getPoNumber());
        }

        // Currency
        if (invoice.getCurrency() != null && !"USD".equals(invoice.getCurrency())) {
            bill.put("currency_code", invoice.getCurrency());
        }

        // Notes
        if (invoice.getVendorName() != null) {
            bill.put("notes", "Invoice from " + invoice.getVendorName());
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
        bill.set("line_items", lineItems);

        return bill;
    }

    private ObjectNode buildLineItem(InvoiceLineItem li) {
        ObjectNode line = MAPPER.createObjectNode();

        if (li.getGlAccountCode() != null) {
            line.put("account_id", li.getGlAccountCode());
        }
        if (li.getDescription() != null) {
            line.put("description", li.getDescription());
        }
        if (li.getUnitPrice() != null) {
            line.put("rate", li.getUnitPrice());
        }
        if (li.getQuantity() != null) {
            line.put("quantity", li.getQuantity());
        }
        if (li.getItemCode() != null) {
            line.put("item_id", li.getItemCode());
            line.put("name", li.getItemCode());
        }
        if (li.getUnit() != null) {
            line.put("unit", li.getUnit());
        }

        return line;
    }

    private ObjectNode buildSingleLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        if (invoice.getGlAccount() != null) {
            line.put("account_id", invoice.getGlAccount());
        }
        line.put("description", "Invoice " + invoice.getInvoiceNumber());
        line.put("rate", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        line.put("quantity", 1);
        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractBillId(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode bill = root.path("bill");
            if (!bill.isMissingNode()) {
                return bill.path("bill_id").asText(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse Zoho bill response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("message")) {
                return root.path("message").asText("Unknown Zoho Books error");
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown Zoho Books error";
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private record ZohoCredentials(String accessToken, String organizationId, String baseUrl) {}

    private static final Map<String, String> REGION_ACCOUNTS = Map.of(
            "US", "https://accounts.zoho.com",
            "EU", "https://accounts.zoho.eu",
            "IN", "https://accounts.zoho.in",
            "AU", "https://accounts.zoho.com.au",
            "JP", "https://accounts.zoho.jp"
    );

    private ZohoCredentials resolveCredentials(Map<String, String> props) {
        String organizationId = props.get("organization_id");
        String refreshToken = props.get("refresh_token");
        String clientId = props.get("client_id");
        String clientSecret = props.get("client_secret");
        String region = props.getOrDefault("region", "US").toUpperCase();

        if (organizationId == null || organizationId.isBlank())
            throw new BadRequestException("Zoho Books Organization ID is not configured.");
        if (refreshToken == null || refreshToken.isBlank())
            throw new BadRequestException("Zoho Books Refresh Token is not configured. Complete the OAuth2 authorization flow.");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new BadRequestException("Zoho Books OAuth2 client credentials are not configured.");

        String baseUrl = REGION_DOMAINS.getOrDefault(region, REGION_DOMAINS.get("US"));
        String accountsUrl = REGION_ACCOUNTS.getOrDefault(region, REGION_ACCOUNTS.get("US"));

        String accessToken = refreshZohoAccessToken(accountsUrl, clientId, clientSecret, refreshToken);

        return new ZohoCredentials(accessToken, organizationId, baseUrl);
    }

    private String refreshZohoAccessToken(String accountsUrl, String clientId, String clientSecret, String refreshToken) {
        try {
            String tokenUrl = accountsUrl + "/oauth/v2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "grant_type=refresh_token"
                    + "&refresh_token=" + refreshToken
                    + "&client_id=" + clientId
                    + "&client_secret=" + clientSecret;

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode token = MAPPER.readTree(response.getBody());
            String accessToken = token.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                String error = token.path("error").asText("unknown");
                throw new BadRequestException("Zoho token refresh failed: " + error);
            }
            return accessToken;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Zoho token refresh failed: {}", e.getResponseBodyAsString());
            throw new BadRequestException("Zoho Books OAuth2 token refresh failed. Please re-authorize.");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoho token refresh error", e);
            throw new BadRequestException("Zoho Books OAuth2 token refresh failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
