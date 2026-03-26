package com.vedvix.syncledger.service;

import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceLineItem;
import com.vedvix.syncledger.model.Organization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class SageIntacctService {

    private static final String DEFAULT_GATEWAY = "https://api.intacct.com/ia/xml/xmlgw.phtml";

    @Value("${sage.intacct.sender-id:}")
    private String appSenderId;

    @Value("${sage.intacct.sender-password:}")
    private String appSenderPassword;

    private final EncryptionService encryptionService;
    private final RestTemplate restTemplate;

    public SageIntacctService(EncryptionService encryptionService, RestTemplateBuilder restTemplateBuilder) {
        this.encryptionService = encryptionService;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    /**
     * Result of a Sage Intacct API call.
     */
    public record SageResponse(
            boolean success,
            String recordNo,
            int httpStatusCode,
            String errorCode,
            String errorMessage,
            String requestPayload,
            String responsePayload
    ) {
        static SageResponse ok(String recordNo, int httpStatus, String request, String response) {
            return new SageResponse(true, recordNo, httpStatus, null, null, request, response);
        }

        static SageResponse fail(String recordNo, int httpStatus, String code, String msg, String request, String response) {
            return new SageResponse(false, recordNo, httpStatus, code, msg, request, response);
        }
    }

    /**
     * Create an AP Bill in Sage Intacct for the given invoice.
     */
    public SageResponse createBill(Invoice invoice, Organization org) {
        // --- Resolve sender credentials (app-level override → org-level) ---
        String senderId = nonBlank(appSenderId) != null ? appSenderId : org.getErpTenantId();
        String senderPassword = nonBlank(appSenderPassword) != null
                ? appSenderPassword : decryptSafe(org.getErpApiKeyEncrypted());

        // --- Login credentials always from org ---
        String userId = org.getErpTenantId();
        String companyId = org.getErpCompanyId();
        String userPassword = decryptSafe(org.getErpApiKeyEncrypted());

        if (nonBlank(senderId) == null)
            throw new BadRequestException("Sage Sender ID is not configured. Set it in ERP settings (User ID field).");
        if (nonBlank(companyId) == null)
            throw new BadRequestException("Sage Company ID is not configured for this organization.");
        if (nonBlank(userPassword) == null)
            throw new BadRequestException("Sage password is not configured for this organization.");

        // --- Build XML ---
        String controlId = UUID.randomUUID().toString();
        String requestXml = buildCreateBillXml(invoice, senderId, senderPassword,
                userId, companyId, userPassword, controlId);

        // Mask credentials for logging / sync log
        String maskedXml = requestXml
                .replaceAll("<password>[^<]*</password>", "<password>***</password>");

        // --- Resolve gateway URL ---
        String gatewayUrl = resolveGatewayUrl(org.getErpApiEndpoint());

        log.info("Sage Intacct: Creating AP Bill [gateway={}, company={}, invoiceNo={}]",
                gatewayUrl, companyId, invoice.getInvoiceNumber());

        // --- Send HTTP ---
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> entity = new HttpEntity<>(requestXml, headers);

        String responseBody;
        int httpStatus;
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    gatewayUrl, HttpMethod.POST, entity, String.class);
            responseBody = response.getBody();
            httpStatus = response.getStatusCode().value();
        } catch (Exception e) {
            log.error("Sage Intacct HTTP error: {}", e.getMessage());
            return SageResponse.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Sage Intacct: " + e.getMessage(), maskedXml, null);
        }

        // --- Parse response ---
        SageResponse result = parseResponse(responseBody, httpStatus, maskedXml);

        if (result.success()) {
            log.info("Sage Intacct: AP Bill created [RECORDNO={}, invoiceNo={}]",
                    result.recordNo(), invoice.getInvoiceNumber());
        } else {
            log.warn("Sage Intacct error [invoiceNo={}, code={}, msg={}]",
                    invoice.getInvoiceNumber(), result.errorCode(), result.errorMessage());
        }
        return result;
    }

    // ─── XML Builder ─────────────────────────────────────────────────────────────

    private String buildCreateBillXml(Invoice invoice, String senderId, String senderPwd,
                                      String userId, String companyId, String userPwd,
                                      String controlId) {
        StringBuilder items = new StringBuilder();
        if (invoice.getLineItems() != null && !invoice.getLineItems().isEmpty()) {
            for (InvoiceLineItem li : invoice.getLineItems()) {
                items.append("<APBILLITEM>");
                tag(items, "GLACCOUNTNO", li.getGlAccountCode() != null ? li.getGlAccountCode() : "5000");
                tag(items, "TRX_AMOUNT", li.getLineTotal() != null ? li.getLineTotal().toPlainString() : "0");
                if (li.getDescription() != null) tag(items, "MEMO", trunc(li.getDescription(), 1000));
                if (li.getItemCode() != null) tag(items, "ITEMID", li.getItemCode());
                if (li.getQuantity() != null) tag(items, "QUANTITY", li.getQuantity().toPlainString());
                if (li.getUnitPrice() != null) tag(items, "PRICE", li.getUnitPrice().toPlainString());
                if (li.getCostCenter() != null) tag(items, "DEPARTMENTID", li.getCostCenter());
                items.append("</APBILLITEM>\n");
            }
        } else {
            items.append("<APBILLITEM>");
            tag(items, "GLACCOUNTNO", invoice.getGlAccount() != null ? invoice.getGlAccount() : "5000");
            tag(items, "TRX_AMOUNT", invoice.getTotalAmount() != null ? invoice.getTotalAmount().toPlainString() : "0");
            tag(items, "MEMO", "Invoice " + esc(invoice.getInvoiceNumber()));
            items.append("</APBILLITEM>\n");
        }

        String vendorId = invoice.getSageVendorId() != null
                ? invoice.getSageVendorId()
                : (invoice.getVendorName() != null ? invoice.getVendorName() : "");

        LocalDate invDate = invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        LocalDate dueDate = invoice.getDueDate() != null ? invoice.getDueDate() : invDate.plusDays(30);
        String ccy = invoice.getCurrency() != null ? invoice.getCurrency() : "USD";

        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<request>\n");

        // Control block
        x.append("<control>\n");
        tag(x, "senderid", senderId);
        tag(x, "password", senderPwd);
        tag(x, "controlid", controlId);
        x.append("<uniqueid>false</uniqueid>\n");
        x.append("<dtdversion>3.0</dtdversion>\n");
        x.append("</control>\n");

        // Operation block
        x.append("<operation>\n<authentication>\n<login>\n");
        tag(x, "userid", userId);
        tag(x, "companyid", companyId);
        tag(x, "password", userPwd);
        x.append("</login>\n</authentication>\n<content>\n");

        // Function: create AP Bill
        x.append("<function controlid=\"create_bill_").append(invoice.getId()).append("\">\n");
        x.append("<create>\n<APBILL>\n");
        tag(x, "VENDORID", vendorId);
        appendDate(x, "DATECREATED", invDate);
        appendDate(x, "DATEPOSTED", invDate);
        appendDate(x, "DATEDUE", dueDate);
        tag(x, "DOCNUMBER", invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "");
        if (invoice.getPoNumber() != null) tag(x, "PONUMBER", invoice.getPoNumber());
        tag(x, "BASECURR", ccy);
        tag(x, "CURRENCY", ccy);
        if (invoice.getVendorName() != null) {
            tag(x, "DESCRIPTION", "Invoice from " + invoice.getVendorName());
        }
        x.append("<APBILLITEMS>\n").append(items).append("</APBILLITEMS>\n");
        x.append("</APBILL>\n</create>\n</function>\n");
        x.append("</content>\n</operation>\n</request>");
        return x.toString();
    }

    private void appendDate(StringBuilder sb, String tag, LocalDate d) {
        sb.append("<").append(tag).append(">");
        sb.append("<YEAR>").append(d.getYear()).append("</YEAR>");
        sb.append("<MONTH>").append(String.format("%02d", d.getMonthValue())).append("</MONTH>");
        sb.append("<DAY>").append(String.format("%02d", d.getDayOfMonth())).append("</DAY>");
        sb.append("</").append(tag).append(">\n");
    }

    // ─── Response Parser ─────────────────────────────────────────────────────────

    private SageResponse parseResponse(String body, int httpStatus, String maskedReq) {
        if (body == null || body.isBlank()) {
            return SageResponse.fail(null, httpStatus, "EMPTY_RESPONSE",
                    "Empty response from Sage Intacct", maskedReq, null);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(body)));

            // 1. Check <control><status>
            String ctlStatus = childText(doc.getDocumentElement(), "control", "status");
            if ("failure".equalsIgnoreCase(ctlStatus)) {
                return extractError(doc.getDocumentElement(), httpStatus, maskedReq, body);
            }

            // 2. Check <operation><result><status>
            NodeList results = doc.getElementsByTagName("result");
            if (results.getLength() > 0) {
                Element result = (Element) results.item(0);
                String resultStatus = directChild(result, "status");

                if ("failure".equalsIgnoreCase(resultStatus)) {
                    return extractResultError(result, httpStatus, maskedReq, body);
                }

                // Success — extract RECORDNO
                String recordNo = null;
                NodeList apbills = doc.getElementsByTagName("APBILL");
                if (apbills.getLength() > 0)
                    recordNo = directChild((Element) apbills.item(0), "RECORDNO");
                if (recordNo == null) {
                    NodeList keys = doc.getElementsByTagName("key");
                    if (keys.getLength() > 0) recordNo = keys.item(0).getTextContent();
                }
                return SageResponse.ok(recordNo, httpStatus, maskedReq, body);
            }

            return SageResponse.ok(null, httpStatus, maskedReq, body);

        } catch (Exception e) {
            log.error("Failed to parse Sage response: {}", e.getMessage());
            return SageResponse.fail(null, httpStatus, "PARSE_ERROR",
                    "Failed to parse Sage response: " + e.getMessage(), maskedReq, body);
        }
    }

    private SageResponse extractError(Element root, int http, String req, String resp) {
        NodeList errors = root.getElementsByTagName("error");
        if (errors.getLength() > 0) {
            Element err = (Element) errors.item(0);
            return SageResponse.fail(null, http,
                    directChild(err, "errorno"),
                    errorMsg(err), req, resp);
        }
        return SageResponse.fail(null, http, "CONTROL_FAILURE", "Sage Intacct control failure", req, resp);
    }

    private SageResponse extractResultError(Element result, int http, String req, String resp) {
        NodeList errMsgs = result.getElementsByTagName("errormessage");
        if (errMsgs.getLength() > 0) {
            NodeList errors = ((Element) errMsgs.item(0)).getElementsByTagName("error");
            if (errors.getLength() > 0) {
                Element err = (Element) errors.item(0);
                return SageResponse.fail(null, http,
                        directChild(err, "errorno"),
                        errorMsg(err), req, resp);
            }
        }
        return SageResponse.fail(null, http, "RESULT_FAILURE", "Sage Intacct operation failed", req, resp);
    }

    private String errorMsg(Element err) {
        StringBuilder sb = new StringBuilder();
        String d1 = directChild(err, "description");
        String d2 = directChild(err, "description2");
        String fix = directChild(err, "correction");
        if (d1 != null) sb.append(d1);
        if (d2 != null) { if (!sb.isEmpty()) sb.append(" — "); sb.append(d2); }
        if (fix != null && !fix.isBlank()) { if (!sb.isEmpty()) sb.append(" [Fix: "); sb.append(fix).append("]"); }
        return !sb.isEmpty() ? sb.toString() : "Unknown Sage error";
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String resolveGatewayUrl(String orgEndpoint) {
        if (orgEndpoint != null && orgEndpoint.contains("/ia/xml/")) return orgEndpoint;
        return DEFAULT_GATEWAY;
    }

    private String decryptSafe(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) return null;
        try {
            return encryptionService.decrypt(encrypted);
        } catch (Exception e) {
            log.debug("Decryption failed, trying raw value");
            return encrypted;
        }
    }

    private static String nonBlank(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    private void tag(StringBuilder sb, String name, String value) {
        sb.append("<").append(name).append(">").append(esc(value)).append("</").append(name).append(">");
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String trunc(String t, int max) {
        if (t == null) return "";
        return t.length() <= max ? t : t.substring(0, max);
    }

    private String childText(Element root, String parent, String child) {
        NodeList parents = root.getElementsByTagName(parent);
        if (parents.getLength() > 0) return directChild((Element) parents.item(0), child);
        return null;
    }

    private String directChild(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }
}
