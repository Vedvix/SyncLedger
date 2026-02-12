"""
Enhanced field parser for Purchase Order / Invoice extraction.
Optimized for MGD Construction Services format.

Author: vedvix
"""

import re
from datetime import date
from decimal import Decimal, InvalidOperation
from typing import List, Optional, Tuple, Dict, Any

import structlog
from dateutil import parser as date_parser

from models.invoice_data import InvoiceData, LineItem, VendorInfo

logger = structlog.get_logger(__name__)


class FieldParser:
    """
    Parse extracted text into structured invoice/purchase order data.
    
    Optimized for Purchase Order format with:
    - PO Number at top
    - Vendor name
    - Order date
    - Project/Sale/Opportunity numbers
    - Line items with Product Name, Description, Quantity, Unit Price, Total
    - Grand Total
    """
    
    # Regex patterns for PO format field extraction
    PATTERNS = {
        # PO/Invoice Number - usually at the very top
        'po_number': [
            r'^\s*(\d{8})\s*$',  # 8-digit PO number at start
            r'PO\s*#?\s*:?\s*(\d+)',
            r'Purchase\s+Order\s*#?\s*:?\s*(\d+)',
            r'Order\s*#?\s*:?\s*(\d+)',
        ],
        'invoice_number': [
            r'invoice\s*#?\s*:?\s*([A-Z0-9\-]+)',
            r'inv\s*#?\s*:?\s*([A-Z0-9\-]+)',
            r'invoice\s+number\s*:?\s*([A-Z0-9\-]+)',
        ],
        # Project/Sale/Opportunity Numbers
        'project_number': [
            r'Project\s+Number\s*:?\s*([A-Z]?\d+)',
        ],
        'sale_number': [
            r'Sale\s+Number\s*:?\s*([A-Z]?\d+)',
        ],
        'opportunity_number': [
            r'Opportunity\s+Number\s*:?\s*([A-Z]?\d+)',
        ],
        # Dates
        'order_date': [
            r'Order\s+Date\s*[\n\r]+\s*(\d{1,2}/\d{1,2}/\d{2,4})',
            r'Order\s+Date\s*:?\s*(\d{1,2}/\d{1,2}/\d{2,4})',
        ],
        'date': [
            r'date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'invoice\s+date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
        ],
        'due_date': [
            r'due\s+date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'Approved\s+Date\s*[\n\r]+\s*(\d{1,2}/\d{1,2}/\d{2,4})',
            r'payment\s+due\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
        ],
        # Totals
        'total': [
            r'Total\s*:?\s*\$?([\d,]+\.?\d*)',
            r'Grand\s+Total\s*:?\s*\$?([\d,]+\.?\d*)',
            r'Amount\s+Due\s*:?\s*\$?([\d,]+\.?\d*)',
            r'Balance\s+Due\s*:?\s*\$?([\d,]+\.?\d*)',
        ],
        'subtotal': [
            r'subtotal\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'sub\s*-?\s*total\s*:?\s*\$?\s*([\d,]+\.?\d*)',
        ],
        'tax': [
            r'tax\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'vat\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'sales\s+tax\s*:?\s*\$?\s*([\d,]+\.?\d*)',
        ],
        # Contact info
        'email': [
            r'([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})',
        ],
        'phone': [
            r'(?:phone|tel|fax)\s*:?\s*([\d\-\(\)\s\+]+)',
            r'(\+?1?\s*[\(\-]?\d{3}[\)\-\s]?\d{3}[\-\s]?\d{4})',
        ],
        'created_by': [
            r'Created\s+By\s*:?\s*([A-Za-z\s]+?)(?:\n|$)',
        ],
        'market_segment': [
            r'Market\s+Segment\s*:?\s*([A-Za-z\s]+?)(?:\n|$)',
        ],
        'product_category': [
            r'Product\s+Category\s*:?\s*([A-Za-z\s]+?)(?:\n|$)',
        ],
    }
    
    # Known vendor names for matching
    KNOWN_VENDORS = [
        'MGD Construction Services',
        'Master Gutters Installation Service',
        "Mayan's Construction Corp",
    ]
    
    async def parse(self, text: str) -> InvoiceData:
        """
        Parse extracted text into structured invoice data.
        
        Args:
            text: Raw extracted text from PDF
            
        Returns:
            InvoiceData with parsed fields
        """
        logger.info("Parsing invoice/PO text", text_length=len(text))
        
        # Extract PO number (from first line typically)
        po_number = self._extract_po_number(text)
        invoice_number = self._extract_pattern(text, self.PATTERNS['invoice_number'])
        
        # Use PO number as invoice number if no specific invoice number found
        if not invoice_number and po_number:
            invoice_number = f"PO-{po_number}"
        
        # Extract dates
        invoice_date = self._extract_date(text, self.PATTERNS['order_date']) or \
                       self._extract_date(text, self.PATTERNS['date'])
        due_date = self._extract_date(text, self.PATTERNS['due_date'])
        
        # Extract totals
        total = self._extract_amount(text, self.PATTERNS['total'])
        subtotal = self._extract_amount(text, self.PATTERNS['subtotal'])
        tax = self._extract_amount(text, self.PATTERNS['tax'])
        
        # If no subtotal but we have total, use total as subtotal (common in POs without tax)
        if not subtotal and total and not tax:
            subtotal = total
        
        # Extract vendor info
        vendor = self._extract_vendor_info(text)
        
        # Extract additional metadata
        project_number = self._extract_pattern(text, self.PATTERNS['project_number'])
        
        # Extract line items
        line_items = self._extract_line_items(text)
        
        # If we didn't get total from pattern, calculate from line items
        if not total and line_items:
            total = sum(item.line_total for item in line_items if item.line_total)
        
        # Calculate confidence score
        confidence = self._calculate_confidence(
            invoice_number=invoice_number,
            invoice_date=invoice_date,
            total=total,
            vendor_name=vendor.name,
            line_items=line_items
        )
        
        requires_review = confidence < 0.7 or invoice_number is None or total is None
        
        logger.info(
            "Parsing completed",
            po_number=po_number,
            invoice_number=invoice_number,
            vendor=vendor.name,
            total=str(total) if total else None,
            line_items_count=len(line_items),
            confidence=confidence,
            requires_review=requires_review
        )
        
        return InvoiceData(
            invoice_number=invoice_number,
            po_number=po_number,
            vendor=vendor,
            invoice_date=invoice_date,
            due_date=due_date,
            subtotal=subtotal,
            tax_amount=tax,
            total_amount=total,
            line_items=line_items,
            confidence_score=confidence,
            requires_manual_review=requires_review,
            raw_text=text
        )
    
    def _extract_po_number(self, text: str) -> Optional[str]:
        """Extract PO number from text, checking first line first."""
        lines = text.strip().split('\n')
        
        # Check first few lines for standalone PO number
        for line in lines[:5]:
            line = line.strip()
            # 8-digit number standalone
            if re.match(r'^\d{8}$', line):
                return line
        
        # Try other patterns
        return self._extract_pattern(text, self.PATTERNS['po_number'])
    
    def _extract_pattern(self, text: str, patterns: List[str]) -> Optional[str]:
        """Extract first matching pattern from text."""
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE | re.MULTILINE)
            if match:
                return match.group(1).strip()
        return None
    
    def _extract_date(self, text: str, patterns: List[str]) -> Optional[date]:
        """Extract and parse date from text."""
        date_str = self._extract_pattern(text, patterns)
        if date_str:
            try:
                parsed = date_parser.parse(date_str, fuzzy=True)
                return parsed.date()
            except Exception:
                pass
        return None
    
    def _extract_amount(self, text: str, patterns: List[str]) -> Optional[Decimal]:
        """Extract and parse monetary amount from text."""
        amount_str = self._extract_pattern(text, patterns)
        if amount_str:
            try:
                # Remove commas, dollar signs, and whitespace
                cleaned = amount_str.replace(',', '').replace('$', '').strip()
                return Decimal(cleaned)
            except InvalidOperation:
                pass
        return None
    
    def _extract_vendor_info(self, text: str) -> VendorInfo:
        """Extract vendor information from text."""
        # Extract individual fields
        email = self._extract_pattern(text, self.PATTERNS['email'])
        phone = self._extract_pattern(text, self.PATTERNS['phone'])
        
        # Try to find known vendor names first
        vendor_name = None
        text_lower = text.lower()
        
        for known_vendor in self.KNOWN_VENDORS:
            if known_vendor.lower() in text_lower:
                vendor_name = known_vendor
                break
        
        # If no known vendor, look for vendor name after approved date line
        if not vendor_name:
            # Pattern: After "Approved Date" line, the next significant line is vendor
            match = re.search(r'Approved\s+Date\s*[\n\r]+([A-Za-z][A-Za-z\s\']+(?:Inc|Corp|LLC|Services?|Construction)?[A-Za-z\s]*)', text, re.IGNORECASE)
            if match:
                vendor_name = match.group(1).strip()
        
        # Fallback: look for company indicators in first 15 lines
        if not vendor_name:
            lines = [l.strip() for l in text.split('\n') if l.strip()]
            for line in lines[:15]:
                if any(ind in line for ind in ['Inc', 'Corp', 'LLC', 'Services', 'Construction', 'Company']):
                    if not any(skip in line.lower() for skip in ['purchase order', 'invoice', 'project', 'sale']):
                        vendor_name = line
                        break
        
        return VendorInfo(
            name=vendor_name,
            email=email,
            phone=phone
        )
    
    def _extract_line_items(self, text: str) -> List[LineItem]:
        """
        Extract line items from PO/invoice text.
        
        Handles multi-line format where items span several lines:
        - Description line(s)
        - Quantity
        - $Unit Price
        - $Total Price
        """
        line_items = []
        lines = text.split('\n')
        
        # Find the header row to know where line items start
        header_idx = -1
        for i, line in enumerate(lines):
            if 'Product Name' in line or 'Total' in line and 'Price' in line:
                header_idx = i
                break
        
        # Also check for "Price" alone as the header might be split
        if header_idx == -1:
            for i, line in enumerate(lines):
                if line.strip() == 'Price' and i > 0:
                    # Check if previous lines have Quantity
                    for j in range(max(0, i-5), i):
                        if 'Quantity' in lines[j]:
                            header_idx = i
                            break
                    if header_idx != -1:
                        break
        
        if header_idx == -1:
            return self._extract_line_items_pattern_based(text)
        
        # Process lines after header - collect items by looking for price patterns
        # In this format: Description lines -> Quantity -> $Price -> $Total
        line_number = 0
        description_buffer = []
        i = header_idx + 1
        
        while i < len(lines):
            line = lines[i].strip()
            
            # Skip empty lines
            if not line:
                i += 1
                continue
            
            # Check for end of items
            if 'Total:' in line:
                break
            
            # Check if this line is a dollar amount (price)
            if line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', line):
                # This is likely a unit price or total price
                # Look ahead to see if next line is also a price (total)
                if i + 1 < len(lines):
                    next_line = lines[i + 1].strip()
                    if next_line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', next_line):
                        # We have unit price and total
                        # Look back for quantity
                        qty_idx = i - 1
                        qty_line = lines[qty_idx].strip() if qty_idx >= 0 else ""
                        
                        # Skip if qty_line is a description
                        while qty_idx >= header_idx and not re.match(r'^[\d,]+\.?\d*$', qty_line):
                            qty_idx -= 1
                            qty_line = lines[qty_idx].strip() if qty_idx >= header_idx else ""
                        
                        if re.match(r'^[\d,]+\.?\d*$', qty_line):
                            try:
                                quantity = Decimal(qty_line.replace(',', ''))
                                unit_price = Decimal(line.replace('$', '').replace(',', ''))
                                total_price = Decimal(next_line.replace('$', '').replace(',', ''))
                                
                                line_number += 1
                                description = ' '.join(description_buffer).strip()
                                
                                line_items.append(LineItem(
                                    line_number=line_number,
                                    description=description if description else None,
                                    quantity=quantity,
                                    unit_price=unit_price,
                                    line_total=total_price
                                ))
                                
                                description_buffer = []
                                i += 2  # Skip both price lines
                                continue
                            except (InvalidOperation, ValueError):
                                pass
                
                i += 1
                continue
            
            # Check if this is a standalone quantity (number without $)
            if re.match(r'^[\d,]+\.?\d*$', line):
                # This might be quantity, don't add to description
                i += 1
                continue
            
            # Otherwise, this is description text
            if line and not any(skip in line.lower() for skip in ['notes', 'special', 'instructions']):
                description_buffer.append(line)
            
            i += 1
        
        # If no items found, try pattern-based extraction
        if not line_items:
            return self._extract_line_items_pattern_based(text)
        
        return line_items
    
    def _extract_line_items_pattern_based(self, text: str) -> List[LineItem]:
        """
        Extract line items using pattern matching on the full text.
        
        Handles the Purchase Order format where items span multiple lines:
        - Description line(s)  
        - Quantity (number without $)
        - $Unit Price (starts with $)
        - $Total Price (starts with $)
        
        Works by finding $total patterns and looking backwards for the structure.
        """
        line_items = []
        lines = [l.strip() for l in text.split('\n') if l.strip()]
        
        # Find all total price patterns ($XXX.XX) and work backwards
        line_number = 0
        i = 0
        while i < len(lines):
            line = lines[i]
            
            # Check for total price (starts with $, standalone dollar amount)
            if line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', line):
                # Look back for unit price (also starts with $)
                if i > 0 and lines[i-1].startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', lines[i-1]):
                    # Look back for quantity (number without $)
                    if i > 1 and re.match(r'^[\d,]+\.?\d*$', lines[i-2]):
                        qty_line = lines[i-2]
                        unit_price_line = lines[i-1]
                        total_price_line = line
                        
                        # Collect description lines going backwards from quantity
                        desc_lines = []
                        j = i - 3
                        while j >= 0:
                            prev_line = lines[j]
                            # Stop at: another total ($), another qty (number w/o $), or header keywords
                            if re.match(r'^\$[\d,]+\.?\d*$', prev_line):
                                break
                            if re.match(r'^[\d,]+\.?\d*$', prev_line):
                                break
                            if any(kw in prev_line for kw in ['Product Name', 'Quantity', 'Price', 'Total:', 'Notes', 'Total']):
                                break
                            desc_lines.insert(0, prev_line)
                            j -= 1
                        
                        try:
                            line_number += 1
                            line_items.append(LineItem(
                                line_number=line_number,
                                description=' '.join(desc_lines) if desc_lines else None,
                                quantity=Decimal(qty_line.replace(',', '')),
                                unit_price=Decimal(unit_price_line.replace('$', '').replace(',', '')),
                                line_total=Decimal(total_price_line.replace('$', '').replace(',', ''))
                            ))
                        except (InvalidOperation, ValueError):
                            pass
            
            i += 1
        
        return line_items
    
    def _extract_line_items_fallback(self, text: str) -> List[LineItem]:
        """
        Fallback line item extraction using regex patterns.
        """
        line_items = []
        
        # Pattern: description followed by quantity, unit price, total
        # Handles formats like: "description 13.16 $130.00 $1,710.80"
        pattern = r'([A-Za-z][^\n]{5,50}?)\s+([\d,]+\.?\d*)\s+\$?([\d,]+\.?\d+)\s+\$?([\d,]+\.?\d+)'
        
        matches = re.findall(pattern, text, re.MULTILINE)
        
        for i, match in enumerate(matches, 1):
            try:
                description, qty_str, price_str, total_str = match
                
                # Skip header-like rows
                if 'quantity' in description.lower() or 'price' in description.lower():
                    continue
                
                quantity = Decimal(qty_str.replace(',', ''))
                unit_price = Decimal(price_str.replace(',', ''))
                line_total = Decimal(total_str.replace(',', ''))
                
                line_items.append(LineItem(
                    line_number=i,
                    description=description.strip(),
                    quantity=quantity,
                    unit_price=unit_price,
                    line_total=line_total
                ))
            except (InvalidOperation, ValueError):
                continue
        
        return line_items
    
    def _calculate_confidence(
        self,
        invoice_number: Optional[str],
        invoice_date: Optional[date],
        total: Optional[Decimal],
        vendor_name: Optional[str],
        line_items: List[LineItem]
    ) -> float:
        """
        Calculate confidence score based on extracted fields.
        
        Returns a score between 0 and 1.
        """
        score = 0.0
        weights = {
            'invoice_number': 0.25,
            'invoice_date': 0.15,
            'total': 0.25,
            'vendor_name': 0.15,
            'line_items': 0.20,
        }
        
        if invoice_number:
            score += weights['invoice_number']
        if invoice_date:
            score += weights['invoice_date']
        if total:
            score += weights['total']
        if vendor_name:
            score += weights['vendor_name']
        if line_items:
            # Partial credit based on number of line items with complete data
            complete_items = sum(1 for item in line_items if item.line_total and item.quantity)
            item_score = min(complete_items / max(len(line_items), 1), 1.0)
            score += weights['line_items'] * item_score
        
        # Verify total matches sum of line items (if applicable)
        if total and line_items:
            calculated_total = sum(item.line_total for item in line_items if item.line_total)
            if calculated_total and abs(float(total) - float(calculated_total)) < 0.01:
                score += 0.05  # Bonus for matching totals
        
        return round(min(score, 1.0), 2)
