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
 * QuickBooks Online connector — uses REST API with OAuth2.
 * Creates AP Bills via POST /v3/company/{realmId}/bill
 *
 * @author vedvix
 */
@Service
@Slf4j
public class QuickBooksConnector implements ErpConnector {

    private static final String SANDBOX_BASE = "https://sandbox-quickbooks.api.intuit.com";
    private static final String PRODUCTION_BASE = "https://quickbooks.api.intuit.com";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;

    public QuickBooksConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.QUICKBOOKS;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        QBCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/v3/company/" + creds.realmId + "/companyinfo/" + creds.realmId;

        HttpHeaders headers = buildHeaders(creds.accessToken);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "QB_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to QuickBooks: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        QBCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/v3/company/" + creds.realmId + "/bill";
        String requestJson = null;

        try {
            ObjectNode bill = buildBillPayload(invoice);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(bill);

            log.info("QuickBooks: Creating Bill [realmId={}, invoiceNo={}]",
                    creds.realmId, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds.accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();

            // Extract Bill.Id from response
            String billId = extractBillId(responseBody);

            log.info("QuickBooks: Bill created [Id={}, invoiceNo={}]", billId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(billId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("QuickBooks error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "QB_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (Exception e) {
            log.error("QuickBooks connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to QuickBooks: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildBillPayload(Invoice invoice) {
        ObjectNode bill = MAPPER.createObjectNode();

        // Vendor reference (required)
        ObjectNode vendorRef = MAPPER.createObjectNode();
        vendorRef.put("value", invoice.getSageVendorId() != null
                ? invoice.getSageVendorId() : invoice.getVendorName());
        bill.set("VendorRef", vendorRef);

        // Dates
        LocalDate txnDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        bill.put("TxnDate", txnDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (invoice.getDueDate() != null) {
            bill.put("DueDate", invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // Document number
        if (invoice.getInvoiceNumber() != null) {
            bill.put("DocNumber", invoice.getInvoiceNumber());
        }

        // PO number as private note
        if (invoice.getPoNumber() != null) {
            bill.put("PrivateNote", "PO: " + invoice.getPoNumber());
        }

        // Currency
        if (invoice.getCurrency() != null && !"USD".equals(invoice.getCurrency())) {
            ObjectNode currRef = MAPPER.createObjectNode();
            currRef.put("value", invoice.getCurrency());
            bill.set("CurrencyRef", currRef);
        }

        // Line items
        ArrayNode lines = MAPPER.createArrayNode();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            int lineNum = 1;
            for (InvoiceLineItem li : invoice.getLineItems()) {
                lines.add(buildLineItem(li, lineNum++));
            }
        } else {
            // Single line from header
            lines.add(buildSingleLine(invoice));
        }
        bill.set("Line", lines);

        return bill;
    }

    private ObjectNode buildLineItem(InvoiceLineItem li, int lineNum) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("DetailType", "AccountBasedExpenseLineDetail");

        BigDecimal amount = li.getLineTotal() != null ? li.getLineTotal() : BigDecimal.ZERO;
        line.put("Amount", amount);

        if (li.getDescription() != null) {
            line.put("Description", li.getDescription());
        }

        ObjectNode detail = MAPPER.createObjectNode();
        ObjectNode accountRef = MAPPER.createObjectNode();
        accountRef.put("value", li.getGlAccountCode() != null ? li.getGlAccountCode() : "5000");
        detail.set("AccountRef", accountRef);
        line.set("AccountBasedExpenseLineDetail", detail);

        return line;
    }

    private ObjectNode buildSingleLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        line.put("DetailType", "AccountBasedExpenseLineDetail");
        line.put("Amount", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        line.put("Description", "Invoice " + invoice.getInvoiceNumber());

        ObjectNode detail = MAPPER.createObjectNode();
        ObjectNode accountRef = MAPPER.createObjectNode();
        accountRef.put("value", invoice.getGlAccount() != null ? invoice.getGlAccount() : "5000");
        detail.set("AccountRef", accountRef);
        line.set("AccountBasedExpenseLineDetail", detail);

        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractBillId(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode bill = root.path("Bill");
            if (!bill.isMissingNode()) {
                return bill.path("Id").asText(null);
            }
        } catch (Exception e) {
            log.warn("Failed to parse QuickBooks bill response: {}", e.getMessage());
        }
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode fault = root.path("Fault");
            if (!fault.isMissingNode()) {
                JsonNode errors = fault.path("Error");
                if (errors.isArray() && !errors.isEmpty()) {
                    return errors.get(0).path("Detail").asText(
                            errors.get(0).path("Message").asText("Unknown QuickBooks error"));
                }
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown QuickBooks error";
    }

    // ─── Credentials ─────────────────────────────────────────────────────────────

    private static final String TOKEN_URL = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";

    private record QBCredentials(String accessToken, String realmId, String baseUrl) {}

    private QBCredentials resolveCredentials(Map<String, String> props) {
        String realmId = props.get("realm_id");
        String refreshToken = props.get("refresh_token");
        String clientId = props.get("client_id");
        String clientSecret = props.get("client_secret");
        String environment = props.getOrDefault("environment", "production");

        if (realmId == null || realmId.isBlank())
            throw new BadRequestException("QuickBooks Realm ID (Company ID) is not configured.");
        if (refreshToken == null || refreshToken.isBlank())
            throw new BadRequestException("QuickBooks Refresh Token is not configured. Complete the OAuth2 authorization flow.");
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())
            throw new BadRequestException("QuickBooks OAuth2 client credentials are not configured.");

        String baseUrl = "sandbox".equalsIgnoreCase(environment) ? SANDBOX_BASE : PRODUCTION_BASE;

        String accessToken = refreshAccessToken(clientId, clientSecret, refreshToken);

        return new QBCredentials(accessToken, realmId, baseUrl);
    }

    private String refreshAccessToken(String clientId, String clientSecret, String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, clientSecret);

            String body = "grant_type=refresh_token&refresh_token=" + refreshToken;

            ResponseEntity<String> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode token = MAPPER.readTree(response.getBody());
            String accessToken = token.path("access_token").asText(null);
            if (accessToken == null || accessToken.isBlank()) {
                throw new BadRequestException("QuickBooks token refresh returned empty access_token");
            }
            return accessToken;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("QuickBooks token refresh failed: {}", e.getResponseBodyAsString());
            throw new BadRequestException("QuickBooks OAuth2 token refresh failed. Please re-authorize.");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("QuickBooks token refresh error", e);
            throw new BadRequestException("QuickBooks OAuth2 token refresh failed: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
