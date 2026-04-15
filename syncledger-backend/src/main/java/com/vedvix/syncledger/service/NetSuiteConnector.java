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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Oracle NetSuite connector — uses SuiteTalk REST API with Token-Based Authentication (TBA).
 * Creates Vendor Bills via POST /services/rest/record/v1/vendorBill
 *
 * @author vedvix
 */
@Service
@Slf4j
public class NetSuiteConnector implements ErpConnector {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter NS_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final RestTemplate restTemplate;

    public NetSuiteConnector(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(90))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.NETSUITE;
    }

    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        NSCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/services/rest/record/v1/metadata-catalog/vendorBill";

        HttpHeaders headers = buildHeaders(creds, "GET", url);
        String requestDesc = "GET " + url;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return ErpSyncResult.ok(null, response.getStatusCode().value(), requestDesc, response.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "NS_HTTP_ERROR",
                    e.getResponseBodyAsString(), requestDesc, e.getResponseBodyAsString());
        } catch (Exception e) {
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to NetSuite: " + e.getMessage(), requestDesc, null);
        }
    }

    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        NSCredentials creds = resolveCredentials(properties);
        String url = creds.baseUrl + "/services/rest/record/v1/vendorBill";
        String requestJson = null;

        try {
            ObjectNode payload = buildVendorBillPayload(invoice);
            requestJson = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(payload);

            log.info("NetSuite: Creating Vendor Bill [accountId={}, invoiceNo={}]",
                    creds.accountId, invoice.getInvoiceNumber());

            HttpHeaders headers = buildHeaders(creds, "POST", url);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            int httpStatus = response.getStatusCode().value();

            // NetSuite returns the record ID in the Location header or response body
            String recordId = extractRecordId(response.getHeaders(), responseBody);

            log.info("NetSuite: Vendor Bill created [id={}, invoiceNo={}]",
                    recordId, invoice.getInvoiceNumber());
            return ErpSyncResult.ok(recordId, httpStatus, requestJson, responseBody);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            String errorMsg = extractErrorMessage(errorBody);
            log.warn("NetSuite error [invoiceNo={}, status={}, msg={}]",
                    invoice.getInvoiceNumber(), e.getStatusCode().value(), errorMsg);
            return ErpSyncResult.fail(null, e.getStatusCode().value(), "NS_API_ERROR",
                    errorMsg, requestJson, errorBody);
        } catch (Exception e) {
            log.error("NetSuite connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to NetSuite: " + e.getMessage(), requestJson, null);
        }
    }

    // ─── Payload Builder ─────────────────────────────────────────────────────────

    private ObjectNode buildVendorBillPayload(Invoice invoice) {
        ObjectNode bill = MAPPER.createObjectNode();

        // Entity (vendor) reference
        if (invoice.getSageVendorId() != null) {
            ObjectNode entity = MAPPER.createObjectNode();
            entity.put("id", invoice.getSageVendorId());
            bill.set("entity", entity);
        }

        // Dates
        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        bill.put("tranDate", invDate.format(NS_DATE));
        if (invoice.getDueDate() != null) {
            bill.put("dueDate", invoice.getDueDate().format(NS_DATE));
        }

        // Transaction ID / document number
        if (invoice.getInvoiceNumber() != null) {
            bill.put("tranId", invoice.getInvoiceNumber());
        }

        // PO reference
        if (invoice.getPoNumber() != null) {
            bill.put("otherRefNum", invoice.getPoNumber());
        }

        // Memo
        if (invoice.getVendorName() != null) {
            bill.put("memo", "Invoice from " + invoice.getVendorName());
        }

        // Currency
        if (invoice.getCurrency() != null) {
            ObjectNode currency = MAPPER.createObjectNode();
            currency.put("refName", invoice.getCurrency());
            bill.set("currency", currency);
        }

        // Expense lines (item sublist)
        ObjectNode expense = MAPPER.createObjectNode();
        ArrayNode items = MAPPER.createArrayNode();

        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            for (InvoiceLineItem li : invoice.getLineItems()) {
                items.add(buildExpenseLine(li));
            }
        } else {
            items.add(buildSingleExpenseLine(invoice));
        }
        expense.set("items", items);
        bill.set("expense", expense);

        return bill;
    }

    private ObjectNode buildExpenseLine(InvoiceLineItem li) {
        ObjectNode line = MAPPER.createObjectNode();

        // Account reference
        if (li.getGlAccountCode() != null) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("id", li.getGlAccountCode());
            line.set("account", account);
        }

        // Amount
        line.put("amount", li.getLineTotal() != null ? li.getLineTotal() : BigDecimal.ZERO);

        // Memo
        if (li.getDescription() != null) {
            line.put("memo", li.getDescription());
        }

        // Department
        if (li.getCostCenter() != null) {
            ObjectNode dept = MAPPER.createObjectNode();
            dept.put("refName", li.getCostCenter());
            line.set("department", dept);
        }

        return line;
    }

    private ObjectNode buildSingleExpenseLine(Invoice invoice) {
        ObjectNode line = MAPPER.createObjectNode();
        if (invoice.getGlAccount() != null) {
            ObjectNode account = MAPPER.createObjectNode();
            account.put("id", invoice.getGlAccount());
            line.set("account", account);
        }
        line.put("amount", invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO);
        line.put("memo", "Invoice " + invoice.getInvoiceNumber());
        return line;
    }

    // ─── Response Parsing ────────────────────────────────────────────────────────

    private String extractRecordId(HttpHeaders responseHeaders, String responseBody) {
        // NetSuite returns Location header with the record URL
        String location = responseHeaders.getFirst("Location");
        if (location != null) {
            int lastSlash = location.lastIndexOf('/');
            if (lastSlash >= 0) {
                return location.substring(lastSlash + 1);
            }
        }
        // Fallback: parse response body
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("id")) {
                return root.path("id").asText(null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.has("o:errorDetails")) {
                JsonNode details = root.path("o:errorDetails");
                if (details.isArray() && !details.isEmpty()) {
                    return details.get(0).path("detail").asText(
                            details.get(0).path("message").asText("Unknown NetSuite error"));
                }
            }
            if (root.has("title")) {
                return root.path("title").asText("Unknown NetSuite error");
            }
        } catch (Exception ignored) {}
        return responseBody != null ? responseBody : "Unknown NetSuite error";
    }

    // ─── OAuth1 / TBA Authentication ─────────────────────────────────────────────

    private record NSCredentials(String accountId, String consumerKey, String consumerSecret,
                                  String tokenId, String tokenSecret, String baseUrl) {}

    private NSCredentials resolveCredentials(Map<String, String> props) {
        String accountId = props.get("account_id");
        String consumerKey = props.get("consumer_key");
        String consumerSecret = props.get("consumer_secret");
        String tokenId = props.get("token_id");
        String tokenSecret = props.get("token_secret");
        String baseUrl = props.get("base_url");

        if (accountId == null || accountId.isBlank())
            throw new BadRequestException("NetSuite Account ID is not configured.");
        if (consumerKey == null || consumerKey.isBlank())
            throw new BadRequestException("NetSuite Consumer Key is not configured.");
        if (tokenId == null || tokenId.isBlank())
            throw new BadRequestException("NetSuite Token ID is not configured.");

        // Build base URL from account ID if not provided
        if (baseUrl == null || baseUrl.isBlank()) {
            String accountSlug = accountId.toLowerCase().replace('_', '-');
            baseUrl = "https://" + accountSlug + ".suitetalk.api.netsuite.com";
        }

        return new NSCredentials(accountId, consumerKey,
                consumerSecret != null ? consumerSecret : "",
                tokenId,
                tokenSecret != null ? tokenSecret : "",
                baseUrl);
    }

    private HttpHeaders buildHeaders(NSCredentials creds, String httpMethod, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        // Build OAuth1 Authorization header (Token-Based Authentication)
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        String authHeader = buildOAuthHeader(creds, httpMethod, url, nonce, timestamp);
        headers.set("Authorization", authHeader);

        return headers;
    }

    private String buildOAuthHeader(NSCredentials creds, String method, String url,
                                     String nonce, String timestamp) {
        // Signature base string
        String baseString = method.toUpperCase() + "&"
                + percentEncode(url) + "&"
                + percentEncode("oauth_consumer_key=" + creds.consumerKey
                + "&oauth_nonce=" + nonce
                + "&oauth_signature_method=HMAC-SHA256"
                + "&oauth_timestamp=" + timestamp
                + "&oauth_token=" + creds.tokenId
                + "&oauth_version=1.0");

        String signingKey = percentEncode(creds.consumerSecret) + "&" + percentEncode(creds.tokenSecret);
        String signature = hmacSha256(signingKey, baseString);

        return "OAuth realm=\"" + creds.accountId + "\","
                + "oauth_consumer_key=\"" + creds.consumerKey + "\","
                + "oauth_token=\"" + creds.tokenId + "\","
                + "oauth_signature_method=\"HMAC-SHA256\","
                + "oauth_timestamp=\"" + timestamp + "\","
                + "oauth_nonce=\"" + nonce + "\","
                + "oauth_version=\"1.0\","
                + "oauth_signature=\"" + percentEncode(signature) + "\"";
    }

    private static String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new BadRequestException("Failed to compute HMAC-SHA256 signature: " + e.getMessage());
        }
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
