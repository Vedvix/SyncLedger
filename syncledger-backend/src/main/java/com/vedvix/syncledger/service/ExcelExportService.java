package com.vedvix.syncledger.service;

import com.vedvix.syncledger.dto.ExportRequest;
import com.vedvix.syncledger.model.Invoice;
import com.vedvix.syncledger.model.InvoiceLineItem;
import com.vedvix.syncledger.repository.InvoiceRepository;
import com.vedvix.syncledger.security.UserPrincipal;
import com.vedvix.syncledger.specification.InvoiceSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for exporting invoice data to Excel (.xlsx) format.
 * Generates professionally formatted Excel workbooks with:
 * - Invoices sheet with all extracted fields
 * - Line Items sheet (optional)
 * - Summary sheet with pivot-style analysis (optional)
 * 
 * @author vedvix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final InvoiceRepository invoiceRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // All available columns for export
    private static final LinkedHashMap<String, String> ALL_COLUMNS = new LinkedHashMap<>() {{
        put("invoiceNumber", "Invoice Number");
        put("poNumber", "PO Number");
        put("vendorName", "Vendor Name");
        put("vendorAddress", "Vendor Address");
        put("vendorEmail", "Vendor Email");
        put("vendorPhone", "Vendor Phone");
        put("vendorTaxId", "Vendor Tax ID");
        put("invoiceDate", "Invoice Date");
        put("dueDate", "Due Date");
        put("receivedDate", "Received Date");
        put("subtotal", "Subtotal");
        put("taxAmount", "Tax Amount");
        put("discountAmount", "Discount");
        put("shippingAmount", "Shipping");
        put("totalAmount", "Total Amount");
        put("currency", "Currency");
        put("status", "Status");
        put("confidenceScore", "Confidence %");
        put("requiresManualReview", "Manual Review");
        put("reviewNotes", "Review Notes");
        put("glAccount", "GL Account");
        put("costCenter", "Cost Center");
        put("project", "Project");
        put("itemCategory", "Item Category");
        put("location", "Location");
        put("originalFileName", "File Name");
        put("sourceEmailFrom", "Email From");
        put("sourceEmailSubject", "Email Subject");
        put("extractionMethod", "Extraction Method");
        put("sageInvoiceId", "Sage Invoice ID");
        put("syncStatus", "Sync Status");
        put("syncErrorMessage", "Sync Error");
        put("assignedTo", "Assigned To");
        put("createdAt", "Date Imported");
        put("updatedAt", "Last Updated");
        put("isOverdue", "Overdue");
        put("daysUntilDue", "Days Until Due");
    }};

    /**
     * Export invoices to Excel with advanced filtering.
     * 
     * @param request Export criteria and options
     * @param currentUser The authenticated user (for org scoping)
     * @return Byte array of the .xlsx file
     */
    @Transactional(readOnly = true)
    public byte[] exportToExcel(ExportRequest request, UserPrincipal currentUser) throws IOException {
        // Determine organization scope
        Long orgId = currentUser.isSuperAdmin() ? null : currentUser.getOrganizationId();

        // Build dynamic query specification
        Specification<Invoice> spec = InvoiceSpecification.fromExportRequest(request, orgId);

        // Determine sort
        Sort sort = buildSort(request);

        // Execute query
        List<Invoice> invoices = invoiceRepository.findAll(spec, sort);

        log.info("Exporting {} invoices to Excel for user {}", invoices.size(), currentUser.getEmail());

        // Determine which columns to export
        List<String> columns = request.getColumns() != null && !request.getColumns().isEmpty()
                ? request.getColumns()
                : new ArrayList<>(ALL_COLUMNS.keySet());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Create styles
            Map<String, CellStyle> styles = createStyles(workbook);

            // Sheet 1: Invoices
            createInvoicesSheet(workbook, styles, invoices, columns);

            // Sheet 2: Line Items (optional)
            if (Boolean.TRUE.equals(request.getIncludeLineItems())) {
                createLineItemsSheet(workbook, styles, invoices);
            }

            // Sheet 3: Summary (optional, defaults to true)
            if (request.getIncludeSummary() == null || Boolean.TRUE.equals(request.getIncludeSummary())) {
                createSummarySheet(workbook, styles, invoices);
            }

            // Write to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Get all available export columns.
     */
    public Map<String, String> getAvailableColumns() {
        return Collections.unmodifiableMap(ALL_COLUMNS);
    }

    // ==================== Sheet Creation ====================

    private void createInvoicesSheet(XSSFWorkbook workbook, Map<String, CellStyle> styles,
                                      List<Invoice> invoices, List<String> columns) {
        XSSFSheet sheet = workbook.createSheet("Invoices");

        // Header row
        Row headerRow = sheet.createRow(0);
        int colIdx = 0;
        for (String col : columns) {
            String label = ALL_COLUMNS.getOrDefault(col, col);
            Cell cell = headerRow.createCell(colIdx);
            cell.setCellValue(label);
            cell.setCellStyle(styles.get("header"));
            colIdx++;
        }

        // Data rows
        int rowIdx = 1;
        for (Invoice invoice : invoices) {
            Row row = sheet.createRow(rowIdx);
            colIdx = 0;
            for (String col : columns) {
                Cell cell = row.createCell(colIdx);
                setCellValue(cell, invoice, col, styles);
                colIdx++;
            }
            rowIdx++;
        }

        // Auto-size columns (with max width)
        for (int i = 0; i < columns.size(); i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
            if (currentWidth < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        // Freeze header row
        sheet.createFreezePane(0, 1);

        // Auto-filter
        if (!invoices.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columns.size() - 1));
        }
    }

    private void createLineItemsSheet(XSSFWorkbook workbook, Map<String, CellStyle> styles,
                                       List<Invoice> invoices) {
        XSSFSheet sheet = workbook.createSheet("Line Items");

        // Headers
        String[] headers = {
                "Invoice Number", "Vendor Name", "Line #", "Description",
                "Item Code", "Unit", "Quantity", "Unit Price",
                "Tax Rate", "Tax Amount", "Discount", "Line Total",
                "GL Account", "Cost Center"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
        }

        // Data
        int rowIdx = 1;
        for (Invoice invoice : invoices) {
            if (invoice.getLineItems() == null) continue;
            for (InvoiceLineItem item : invoice.getLineItems()) {
                Row row = sheet.createRow(rowIdx);
                int c = 0;
                row.createCell(c++).setCellValue(invoice.getInvoiceNumber());
                row.createCell(c++).setCellValue(invoice.getVendorName());
                row.createCell(c++).setCellValue(item.getLineNumber() != null ? item.getLineNumber() : 0);
                row.createCell(c++).setCellValue(nullSafe(item.getDescription()));
                row.createCell(c++).setCellValue(nullSafe(item.getItemCode()));
                row.createCell(c++).setCellValue(nullSafe(item.getUnit()));
                setCurrencyCell(row.createCell(c++), item.getQuantity(), styles.get("number"));
                setCurrencyCell(row.createCell(c++), item.getUnitPrice(), styles.get("currency"));
                setCurrencyCell(row.createCell(c++), item.getTaxRate(), styles.get("percent"));
                setCurrencyCell(row.createCell(c++), item.getTaxAmount(), styles.get("currency"));
                setCurrencyCell(row.createCell(c++), item.getDiscountAmount(), styles.get("currency"));
                setCurrencyCell(row.createCell(c++), item.getLineTotal(), styles.get("currency"));
                row.createCell(c++).setCellValue(nullSafe(item.getGlAccountCode()));
                row.createCell(c++).setCellValue(nullSafe(item.getCostCenter()));
                rowIdx++;
            }
        }

        // Formatting
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        sheet.createFreezePane(0, 1);
        if (rowIdx > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook, Map<String, CellStyle> styles,
                                     List<Invoice> invoices) {
        XSSFSheet sheet = workbook.createSheet("Summary");
        int rowIdx = 0;

        // ── Overview Section ──
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Export Summary");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row dateRow = sheet.createRow(rowIdx++);
        dateRow.createCell(0).setCellValue("Generated:");
        dateRow.createCell(1).setCellValue(LocalDateTime.now().format(DATETIME_FMT));

        Row countRow = sheet.createRow(rowIdx++);
        countRow.createCell(0).setCellValue("Total Invoices:");
        countRow.createCell(1).setCellValue(invoices.size());

        BigDecimal totalAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Row totalRow = sheet.createRow(rowIdx++);
        totalRow.createCell(0).setCellValue("Total Amount:");
        Cell totalCell = totalRow.createCell(1);
        totalCell.setCellValue(totalAmount.doubleValue());
        totalCell.setCellStyle(styles.get("currency"));

        rowIdx++; // blank row

        // ── Status Breakdown ──
        Row statusTitleRow = sheet.createRow(rowIdx++);
        Cell statusTitle = statusTitleRow.createCell(0);
        statusTitle.setCellValue("Status Breakdown");
        statusTitle.setCellStyle(styles.get("sectionHeader"));

        Row statusHeaderRow = sheet.createRow(rowIdx++);
        Cell sh1 = statusHeaderRow.createCell(0);
        sh1.setCellValue("Status");
        sh1.setCellStyle(styles.get("header"));
        Cell sh2 = statusHeaderRow.createCell(1);
        sh2.setCellValue("Count");
        sh2.setCellStyle(styles.get("header"));
        Cell sh3 = statusHeaderRow.createCell(2);
        sh3.setCellValue("Total Amount");
        sh3.setCellStyle(styles.get("header"));
        Cell sh4 = statusHeaderRow.createCell(3);
        sh4.setCellValue("% of Total");
        sh4.setCellStyle(styles.get("header"));

        Map<String, List<Invoice>> byStatus = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            byStatus.computeIfAbsent(inv.getStatus().name(), k -> new ArrayList<>()).add(inv);
        }
        for (Map.Entry<String, List<Invoice>> entry : byStatus.entrySet()) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(entry.getKey());
            r.createCell(1).setCellValue(entry.getValue().size());
            BigDecimal amt = entry.getValue().stream()
                    .map(Invoice::getTotalAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Cell amtCell = r.createCell(2);
            amtCell.setCellValue(amt.doubleValue());
            amtCell.setCellStyle(styles.get("currency"));
            Cell pctCell = r.createCell(3);
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                pctCell.setCellValue(amt.multiply(BigDecimal.valueOf(100))
                        .divide(totalAmount, 1, java.math.RoundingMode.HALF_UP).doubleValue());
            }
            pctCell.setCellStyle(styles.get("percent"));
        }

        rowIdx++; // blank row

        // ── Top Vendors ──
        Row vendorTitleRow = sheet.createRow(rowIdx++);
        Cell vendorTitle = vendorTitleRow.createCell(0);
        vendorTitle.setCellValue("Top Vendors (by Amount)");
        vendorTitle.setCellStyle(styles.get("sectionHeader"));

        Row vendorHeaderRow = sheet.createRow(rowIdx++);
        Cell vh1 = vendorHeaderRow.createCell(0);
        vh1.setCellValue("Vendor");
        vh1.setCellStyle(styles.get("header"));
        Cell vh2 = vendorHeaderRow.createCell(1);
        vh2.setCellValue("Invoice Count");
        vh2.setCellStyle(styles.get("header"));
        Cell vh3 = vendorHeaderRow.createCell(2);
        vh3.setCellValue("Total Amount");
        vh3.setCellStyle(styles.get("header"));
        Cell vh4 = vendorHeaderRow.createCell(3);
        vh4.setCellValue("Avg Amount");
        vh4.setCellStyle(styles.get("header"));

        Map<String, List<Invoice>> byVendor = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            byVendor.computeIfAbsent(inv.getVendorName(), k -> new ArrayList<>()).add(inv);
        }
        // Sort by total amount desc
        byVendor.entrySet().stream()
                .sorted((a, b) -> {
                    BigDecimal aAmt = a.getValue().stream().map(Invoice::getTotalAmount)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal bAmt = b.getValue().stream().map(Invoice::getTotalAmount)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return bAmt.compareTo(aAmt);
                })
                .limit(20)
                .forEach(entry -> {
                    int rIdx = sheet.getLastRowNum() + 1;
                    Row r = sheet.createRow(rIdx);
                    r.createCell(0).setCellValue(entry.getKey());
                    r.createCell(1).setCellValue(entry.getValue().size());
                    BigDecimal amt = entry.getValue().stream()
                            .map(Invoice::getTotalAmount).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Cell amtCell = r.createCell(2);
                    amtCell.setCellValue(amt.doubleValue());
                    amtCell.setCellStyle(styles.get("currency"));
                    Cell avgCell = r.createCell(3);
                    if (!entry.getValue().isEmpty()) {
                        avgCell.setCellValue(amt.divide(BigDecimal.valueOf(entry.getValue().size()),
                                2, java.math.RoundingMode.HALF_UP).doubleValue());
                    }
                    avgCell.setCellStyle(styles.get("currency"));
                });

        rowIdx = sheet.getLastRowNum() + 2;

        // ── Monthly Breakdown ──
        Row monthlyTitleRow = sheet.createRow(rowIdx++);
        Cell monthlyTitle = monthlyTitleRow.createCell(0);
        monthlyTitle.setCellValue("Monthly Breakdown");
        monthlyTitle.setCellStyle(styles.get("sectionHeader"));

        Row monthHeaderRow = sheet.createRow(rowIdx++);
        Cell mh1 = monthHeaderRow.createCell(0);
        mh1.setCellValue("Month");
        mh1.setCellStyle(styles.get("header"));
        Cell mh2 = monthHeaderRow.createCell(1);
        mh2.setCellValue("Count");
        mh2.setCellStyle(styles.get("header"));
        Cell mh3 = monthHeaderRow.createCell(2);
        mh3.setCellValue("Total Amount");
        mh3.setCellStyle(styles.get("header"));

        Map<String, List<Invoice>> byMonth = new TreeMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Invoice inv : invoices) {
            if (inv.getInvoiceDate() != null) {
                String month = inv.getInvoiceDate().format(monthFmt);
                byMonth.computeIfAbsent(month, k -> new ArrayList<>()).add(inv);
            }
        }
        for (Map.Entry<String, List<Invoice>> entry : byMonth.entrySet()) {
            Row r = sheet.createRow(rowIdx++);
            r.createCell(0).setCellValue(entry.getKey());
            r.createCell(1).setCellValue(entry.getValue().size());
            BigDecimal amt = entry.getValue().stream()
                    .map(Invoice::getTotalAmount).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Cell amtCell = r.createCell(2);
            amtCell.setCellValue(amt.doubleValue());
            amtCell.setCellStyle(styles.get("currency"));
        }

        // Auto-size
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ==================== Cell Value Mapping ====================

    private void setCellValue(Cell cell, Invoice invoice, String field, Map<String, CellStyle> styles) {
        switch (field) {
            case "invoiceNumber" -> cell.setCellValue(nullSafe(invoice.getInvoiceNumber()));
            case "poNumber" -> cell.setCellValue(nullSafe(invoice.getPoNumber()));
            case "vendorName" -> cell.setCellValue(nullSafe(invoice.getVendorName()));
            case "vendorAddress" -> cell.setCellValue(nullSafe(invoice.getVendorAddress()));
            case "vendorEmail" -> cell.setCellValue(nullSafe(invoice.getVendorEmail()));
            case "vendorPhone" -> cell.setCellValue(nullSafe(invoice.getVendorPhone()));
            case "vendorTaxId" -> cell.setCellValue(nullSafe(invoice.getVendorTaxId()));
            case "invoiceDate" -> cell.setCellValue(formatDate(invoice.getInvoiceDate()));
            case "dueDate" -> cell.setCellValue(formatDate(invoice.getDueDate()));
            case "receivedDate" -> cell.setCellValue(formatDate(invoice.getReceivedDate()));
            case "subtotal" -> setCurrencyCell(cell, invoice.getSubtotal(), styles.get("currency"));
            case "taxAmount" -> setCurrencyCell(cell, invoice.getTaxAmount(), styles.get("currency"));
            case "discountAmount" -> setCurrencyCell(cell, invoice.getDiscountAmount(), styles.get("currency"));
            case "shippingAmount" -> setCurrencyCell(cell, invoice.getShippingAmount(), styles.get("currency"));
            case "totalAmount" -> setCurrencyCell(cell, invoice.getTotalAmount(), styles.get("currency"));
            case "currency" -> cell.setCellValue(nullSafe(invoice.getCurrency()));
            case "status" -> cell.setCellValue(invoice.getStatus() != null ? invoice.getStatus().name() : "");
            case "confidenceScore" -> {
                if (invoice.getConfidenceScore() != null) {
                    cell.setCellValue(invoice.getConfidenceScore().doubleValue());
                    cell.setCellStyle(styles.get("percent"));
                }
            }
            case "requiresManualReview" -> cell.setCellValue(
                    Boolean.TRUE.equals(invoice.getRequiresManualReview()) ? "Yes" : "No");
            case "reviewNotes" -> cell.setCellValue(nullSafe(invoice.getReviewNotes()));
            case "glAccount" -> cell.setCellValue(nullSafe(invoice.getGlAccount()));
            case "costCenter" -> cell.setCellValue(nullSafe(invoice.getCostCenter()));
            case "project" -> cell.setCellValue(nullSafe(invoice.getProject()));
            case "itemCategory" -> cell.setCellValue(nullSafe(invoice.getItemCategory()));
            case "location" -> cell.setCellValue(nullSafe(invoice.getLocation()));
            case "originalFileName" -> cell.setCellValue(nullSafe(invoice.getOriginalFileName()));
            case "sourceEmailFrom" -> cell.setCellValue(nullSafe(invoice.getSourceEmailFrom()));
            case "sourceEmailSubject" -> cell.setCellValue(nullSafe(invoice.getSourceEmailSubject()));
            case "extractionMethod" -> cell.setCellValue(nullSafe(invoice.getExtractionMethod()));
            case "sageInvoiceId" -> cell.setCellValue(nullSafe(invoice.getSageInvoiceId()));
            case "syncStatus" -> cell.setCellValue(
                    invoice.getSyncStatus() != null ? invoice.getSyncStatus().name() : "");
            case "syncErrorMessage" -> cell.setCellValue(nullSafe(invoice.getSyncErrorMessage()));
            case "assignedTo" -> cell.setCellValue(
                    invoice.getAssignedTo() != null 
                            ? invoice.getAssignedTo().getFirstName() + " " + invoice.getAssignedTo().getLastName()
                            : "");
            case "createdAt" -> cell.setCellValue(formatDateTime(invoice.getCreatedAt()));
            case "updatedAt" -> cell.setCellValue(formatDateTime(invoice.getUpdatedAt()));
            case "isOverdue" -> cell.setCellValue(invoice.isOverdue() ? "Yes" : "No");
            case "daysUntilDue" -> {
                Long days = invoice.getDaysUntilDue();
                if (days != null) {
                    cell.setCellValue(days);
                }
            }
            default -> cell.setCellValue("");
        }
    }

    // ==================== Styles ====================

    private Map<String, CellStyle> createStyles(XSSFWorkbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // Title style
        XSSFCellStyle titleStyle = workbook.createCellStyle();
        XSSFFont titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(new XSSFColor(new byte[]{0x1E, 0x3A, 0x5F}, null));
        titleStyle.setFont(titleFont);
        styles.put("title", titleStyle);

        // Section header style
        XSSFCellStyle sectionStyle = workbook.createCellStyle();
        XSSFFont sectionFont = workbook.createFont();
        sectionFont.setBold(true);
        sectionFont.setFontHeightInPoints((short) 13);
        sectionFont.setColor(new XSSFColor(new byte[]{0x2D, 0x5F, (byte) 0x8A}, null));
        sectionStyle.setFont(sectionFont);
        styles.put("sectionHeader", sectionStyle);

        // Header style
        XSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{0x2D, 0x5F, (byte) 0x8A}, null));
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("header", headerStyle);

        // Currency style
        XSSFCellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        styles.put("currency", currencyStyle);

        // Number style
        XSSFCellStyle numberStyle = workbook.createCellStyle();
        numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.##"));
        styles.put("number", numberStyle);

        // Percent style
        XSSFCellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
        styles.put("percent", percentStyle);

        return styles;
    }

    // ==================== Helpers ====================

    private Sort buildSort(ExportRequest request) {
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private void setCurrencyCell(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            if (style != null) cell.setCellStyle(style);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FMT) : "";
    }
}
