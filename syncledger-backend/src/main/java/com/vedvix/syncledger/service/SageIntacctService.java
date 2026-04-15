package com.vedvix.syncledger.service;

import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.ErpType;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceLineItem;
import com.vedvix.syncledger.model.Organization;
import com.vedvix.syncledger.service.erp.ErpConnector;
import com.vedvix.syncledger.service.erp.ErpSyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SageIntacctService implements ErpConnector {

    private static final String DEFAULT_GATEWAY = "https://api.intacct.com/ia/xml/xmlgw.phtml";

    @Value("${sage.intacct.sender-id:}")
    private String appSenderId;

    @Value("${sage.intacct.sender-password:}")
    private String appSenderPassword;

    private final RestTemplate restTemplate;

    public SageIntacctService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
    }

    @Override
    public ErpType getErpType() {
        return ErpType.SAGE;
    }

    /**
     * Test connectivity to Sage Intacct using the org's ERP credentials.
     */
    @Override
    public ErpSyncResult testConnection(Map<String, String> properties) {
        SageCredentials creds = resolveCredentials(properties);

        String controlId = UUID.randomUUID().toString();
        String requestXml = buildTestConnectionXml(creds, controlId);
        String maskedXml = requestXml.replaceAll("<password>[^<]*</password>", "<password>***</password>");
        String gatewayUrl = resolveGatewayUrl(properties.get("gateway_url"));

        log.info("Sage Intacct: Testing connection [gateway={}, senderId={}, company={}]",
                gatewayUrl, creds.senderId, creds.companyId);

        return executeRequest(requestXml, maskedXml, gatewayUrl);
    }

    /**
     * Create an AP Bill in Sage Intacct for the given invoice.
     */
    @Override
    public ErpSyncResult createBill(Invoice invoice, Map<String, String> properties) {
        SageCredentials creds = resolveCredentials(properties);

        String controlId = UUID.randomUUID().toString();
        String requestXml = buildCreateBillXml(invoice, creds, controlId);
        String maskedXml = requestXml.replaceAll("<password>[^<]*</password>", "<password>***</password>");
        String gatewayUrl = resolveGatewayUrl(properties.get("gateway_url"));

        log.info("Sage Intacct: Creating AP Bill [gateway={}, senderId={}, company={}, invoiceNo={}]",
                gatewayUrl, creds.senderId, creds.companyId, invoice.getInvoiceNumber());

        ErpSyncResult result = executeRequest(requestXml, maskedXml, gatewayUrl);

        if (result.success()) {
            log.info("Sage Intacct: AP Bill created [RECORDNO={}, invoiceNo={}]",
                    result.remoteRecordId(), invoice.getInvoiceNumber());
        } else {
            log.warn("Sage Intacct error [invoiceNo={}, code={}, msg={}]",
                    invoice.getInvoiceNumber(), result.errorCode(), result.errorMessage());
        }
        return result;
    }

    // ─── Shared infrastructure ────────────────────────────────────────────────────

    private record SageCredentials(String senderId, String senderPassword,
                                    String userId, String companyId, String userPassword) {}

    private SageCredentials resolveCredentials(Map<String, String> props) {
        // Sender credentials: property → app-level env var → FAIL
        String senderId = nonBlank(props.get("sender_id")) != null
                ? props.get("sender_id") : nonBlank(appSenderId);
        String senderPassword = nonBlank(props.get("sender_password")) != null
                ? props.get("sender_password") : nonBlank(appSenderPassword);

        String userId = props.get("user_id");
        String companyId = props.get("company_id");
        String userPassword = props.get("user_password");

        if (nonBlank(senderId) == null)
            throw new BadRequestException(
                    "Sage Sender ID is not configured. "
                    + "Set it in ERP properties or as the SAGE_SENDER_ID environment variable. "
                    + "Register at https://developer.intacct.com to obtain one.");
        if (nonBlank(senderPassword) == null)
            throw new BadRequestException(
                    "Sage Sender Password is not configured. "
                    + "Set it in ERP properties or as the SAGE_SENDER_PASSWORD environment variable.");
        if (nonBlank(userId) == null)
            throw new BadRequestException("Sage User ID is not configured in ERP properties.");
        if (nonBlank(companyId) == null)
            throw new BadRequestException("Sage Company ID is not configured in ERP properties.");
        if (nonBlank(userPassword) == null)
            throw new BadRequestException("Sage User Password is not configured in ERP properties.");

        return new SageCredentials(senderId, senderPassword, userId, companyId, userPassword);
    }

    private ErpSyncResult executeRequest(String requestXml, String maskedXml, String gatewayUrl) {
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
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            responseBody = e.getResponseBodyAsString();
            httpStatus = e.getStatusCode().value();
            log.warn("Sage Intacct HTTP {}: {}", httpStatus, responseBody);
        } catch (Exception e) {
            log.error("Sage Intacct connection error: {}", e.getMessage());
            return ErpSyncResult.fail(null, 0, "CONNECTION_ERROR",
                    "Failed to connect to Sage Intacct: " + e.getMessage(), maskedXml, null);
        }

        return parseResponse(responseBody, httpStatus, maskedXml);
    }

    // ─── XML Builders ─────────────────────────────────────────────────────────────

    private void appendControlAndLogin(StringBuilder x, SageCredentials creds, String controlId) {
        x.append("<control>\n");
        tag(x, "senderid", creds.senderId);
        tag(x, "password", creds.senderPassword);
        tag(x, "controlid", controlId);
        x.append("<uniqueid>false</uniqueid>\n");
        x.append("<dtdversion>3.0</dtdversion>\n");
        x.append("</control>\n");
        x.append("<operation>\n<authentication>\n<login>\n");
        tag(x, "userid", creds.userId);
        tag(x, "companyid", creds.companyId);
        tag(x, "password", creds.userPassword);
        x.append("</login>\n</authentication>\n<content>\n");
    }

    private String buildTestConnectionXml(SageCredentials creds, String controlId) {
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<request>\n");
        appendControlAndLogin(x, creds, controlId);
        x.append("<function controlid=\"test_connection\">\n");
        x.append("<getAPISession />\n");
        x.append("</function>\n");
        x.append("</content>\n</operation>\n</request>");
        return x.toString();
    }

    private String buildCreateBillXml(Invoice invoice, SageCredentials creds, String controlId) {
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
        appendControlAndLogin(x, creds, controlId);

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

    private ErpSyncResult parseResponse(String body, int httpStatus, String maskedReq) {
        if (body == null || body.isBlank()) {
            return ErpSyncResult.fail(null, httpStatus, "EMPTY_RESPONSE",
                    "Empty response from Sage Intacct", maskedReq, null);
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // Prevent XXE attacks: fully disable external entities and DTDs
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
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
                return ErpSyncResult.ok(recordNo, httpStatus, maskedReq, body);
            }

            return ErpSyncResult.ok(null, httpStatus, maskedReq, body);

        } catch (Exception e) {
            log.error("Failed to parse Sage response: {}", e.getMessage());
            return ErpSyncResult.fail(null, httpStatus, "PARSE_ERROR",
                    "Failed to parse Sage response: " + e.getMessage(), maskedReq, body);
        }
    }

    private ErpSyncResult extractError(Element root, int http, String req, String resp) {
        NodeList errors = root.getElementsByTagName("error");
        if (errors.getLength() > 0) {
            Element err = (Element) errors.item(0);
            return ErpSyncResult.fail(null, http,
                    directChild(err, "errorno"),
                    errorMsg(err), req, resp);
        }
        return ErpSyncResult.fail(null, http, "CONTROL_FAILURE", "Sage Intacct control failure", req, resp);
    }

    private ErpSyncResult extractResultError(Element result, int http, String req, String resp) {
        NodeList errMsgs = result.getElementsByTagName("errormessage");
        if (errMsgs.getLength() > 0) {
            NodeList errors = ((Element) errMsgs.item(0)).getElementsByTagName("error");
            if (errors.getLength() > 0) {
                Element err = (Element) errors.item(0);
                return ErpSyncResult.fail(null, http,
                        directChild(err, "errorno"),
                        errorMsg(err), req, resp);
            }
        }
        return ErpSyncResult.fail(null, http, "RESULT_FAILURE", "Sage Intacct operation failed", req, resp);
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

    private String resolveGatewayUrl(String endpoint) {
        if (endpoint != null && endpoint.contains("/ia/xml/")) return endpoint;
        return DEFAULT_GATEWAY;
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
