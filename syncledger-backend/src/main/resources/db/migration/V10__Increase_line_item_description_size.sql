-- V10: Increase description column size in invoice_line_items to avoid truncation errors from OCR-extracted text
ALTER TABLE invoice_line_items ALTER COLUMN description TYPE TEXT;
