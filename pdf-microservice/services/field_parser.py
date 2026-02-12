"""
Enhanced field parser for Purchase Order / Invoice extraction.
Optimized for MGD Construction Services format with configurable mapping support.

Extracts raw fields from PDF text and outputs them as a dictionary for the
mapping engine to transform into the target system model.

Author: vedvix
"""

import re
from datetime import date
from decimal import Decimal, InvalidOperation
from typing import Any, Dict, List, Optional, Tuple

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
    
    Returns both a raw_fields dict (for mapping engine) and a direct InvoiceData.
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
        'approved_date': [
            r'Approved\s+Date\s*[\n\r]+\s*(\d{1,2}/\d{1,2}/\d{2,4})',
            r'Approved\s+Date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
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
        # Address patterns
        'address': [
            # Multi-line address with city, state, zip
            r'(?:address|location|ship\s*to|bill\s*to)\s*:?\s*\n?\s*(.+(?:\n.+){0,3}?\s*\d{5}(?:-\d{4})?)',
            # Street address pattern — require word-boundary on suffix to avoid matching "Construction"
            r'(\d+\s+[A-Za-z][\w\s]+\b(?:St|Street|Ave|Avenue|Blvd|Dr|Drive|Rd|Road|Ln|Lane|Way|Ct|Court|Pl|Place|Cir|Hwy)\b\.?\s*(?:,\s*[A-Za-z\s]+,?\s*[A-Z]{2}\s*\d{5})?)',
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
        
        This method does direct extraction for backward compatibility.
        For configurable mapping, use parse_raw_fields() + MappingEngine.
        
        Args:
            text: Raw extracted text from PDF
            
        Returns:
            InvoiceData with parsed fields
        """
        raw_fields = self.parse_raw_fields(text)
        line_items = self._extract_line_items(text)
        
        # Build InvoiceData directly (backward compatible)
        po_number = raw_fields.get("po_number")
        invoice_number = raw_fields.get("invoice_number")
        
        if not invoice_number and po_number:
            invoice_number = f"PO-{po_number}"
        
        invoice_date = raw_fields.get("order_date") or raw_fields.get("invoice_date")
        due_date = raw_fields.get("due_date")
        
        total = raw_fields.get("total")
        subtotal = raw_fields.get("subtotal")
        tax = raw_fields.get("tax_amount")
        
        if not subtotal and total and not tax:
            subtotal = total
        
        vendor_name = raw_fields.get("vendor_name")
        vendor_email = raw_fields.get("vendor_email")
        vendor_phone = raw_fields.get("vendor_phone")
        vendor_address = raw_fields.get("vendor_address")
        
        if not total and line_items:
            total = sum(item.line_total for item in line_items if item.line_total)
        
        confidence = self._calculate_confidence(
            invoice_number=invoice_number,
            invoice_date=invoice_date,
            total=total,
            vendor_name=vendor_name,
            line_items=line_items
        )
        
        requires_review = confidence < 0.7 or invoice_number is None or total is None
        
        logger.info(
            "Parsing completed",
            po_number=po_number,
            invoice_number=invoice_number,
            vendor=vendor_name,
            total=str(total) if total else None,
            line_items_count=len(line_items),
            confidence=confidence,
            requires_review=requires_review
        )
        
        return InvoiceData(
            invoice_number=invoice_number,
            po_number=po_number,
            vendor=VendorInfo(
                name=vendor_name,
                address=vendor_address,
                email=vendor_email,
                phone=vendor_phone,
            ),
            invoice_date=invoice_date,
            due_date=due_date,
            subtotal=subtotal,
            tax_amount=tax,
            total_amount=total,
            line_items=line_items,
            gl_account=raw_fields.get("gl_account"),
            project=raw_fields.get("opportunity_number") or raw_fields.get("project_number"),
            item_category=raw_fields.get("product_category"),
            location=vendor_address,
            confidence_score=confidence,
            requires_manual_review=requires_review,
            raw_text=text
        )
    
    def parse_raw_fields(self, text: str) -> Dict[str, Any]:
        """
        Extract all raw fields from text into a flat dictionary.
        
        This is the primary method used by the mapping engine.
        Keys correspond to MappingSourceField enum values.
        
        Args:
            text: Raw extracted text from PDF
            
        Returns:
            Dict of field_name -> extracted_value
        """
        logger.info("Extracting raw fields", text_length=len(text))
        
        fields: Dict[str, Any] = {}
        
        # ── Identification ──────────────────────────────────────────────
        fields["po_number"] = self._extract_po_number(text)
        fields["invoice_number"] = self._extract_pattern(text, self.PATTERNS['invoice_number'])
        
        # ── Dates ───────────────────────────────────────────────────────
        fields["order_date"] = self._extract_date(text, self.PATTERNS['order_date'])
        fields["invoice_date"] = (
            fields["order_date"]
            or self._extract_date(text, self.PATTERNS['date'])
        )
        fields["due_date"] = self._extract_date(text, self.PATTERNS['due_date'])
        fields["approved_date"] = self._extract_date(text, self.PATTERNS['approved_date'])
        
        # ── Financial ───────────────────────────────────────────────────
        fields["total"] = self._extract_amount(text, self.PATTERNS['total'])
        fields["subtotal"] = self._extract_amount(text, self.PATTERNS['subtotal'])
        fields["tax_amount"] = self._extract_amount(text, self.PATTERNS['tax'])
        
        # ── Vendor Info ─────────────────────────────────────────────────
        vendor_info = self._extract_vendor_info(text)
        fields["vendor_name"] = vendor_info.name
        fields["vendor_address"] = vendor_info.address or self._extract_address(text)
        fields["vendor_email"] = vendor_info.email
        fields["vendor_phone"] = vendor_info.phone
        
        # ── Project Metadata ───────────────────────────────────────────
        fields["project_number"] = self._extract_pattern(text, self.PATTERNS['project_number'])
        fields["sale_number"] = self._extract_pattern(text, self.PATTERNS['sale_number'])
        fields["opportunity_number"] = self._extract_pattern(text, self.PATTERNS['opportunity_number'])
        fields["market_segment"] = self._extract_pattern(text, self.PATTERNS['market_segment'])
        fields["product_category"] = self._extract_pattern(text, self.PATTERNS['product_category'])
        fields["created_by"] = self._extract_pattern(text, self.PATTERNS['created_by'])
        
        # Store raw text for downstream use
        fields["raw_text"] = text
        
        # Clean up None values in dict (keep them but log)
        populated = {k: v for k, v in fields.items() if v is not None and k != "raw_text"}
        logger.info("Raw fields extracted", field_count=len(populated), fields=list(populated.keys()))
        
        return fields
    
    def parse_with_line_items(self, text: str) -> Tuple[Dict[str, Any], List[LineItem]]:
        """
        Extract both raw fields and line items from text.
        
        Convenience method that returns both pieces needed by the mapping engine.
        
        Returns:
            Tuple of (raw_fields dict, list of LineItem)
        """
        raw_fields = self.parse_raw_fields(text)
        line_items = self._extract_line_items(text)
        
        # Calculate confidence and store in raw_fields
        po_number = raw_fields.get("po_number")
        invoice_number = raw_fields.get("invoice_number")
        if not invoice_number and po_number:
            invoice_number = f"PO-{po_number}"
        
        total = raw_fields.get("total")
        
        if not total and line_items:
            total = sum(item.line_total for item in line_items if item.line_total)
            raw_fields["total"] = total
        
        confidence = self._calculate_confidence(
            invoice_number=invoice_number,
            invoice_date=raw_fields.get("order_date") or raw_fields.get("invoice_date"),
            total=total,
            vendor_name=raw_fields.get("vendor_name"),
            line_items=line_items,
        )
        
        raw_fields["confidence_score"] = confidence
        raw_fields["requires_manual_review"] = (
            confidence < 0.7 or invoice_number is None or total is None
        )
        
        return raw_fields, line_items
    
    # ─── Private extraction methods ─────────────────────────────────────────
    
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
    
    def _extract_address(self, text: str) -> Optional[str]:
        """Extract address from text using multiple strategies."""
        # Valid US state abbreviations for strict matching
        US_STATES = {
            'AL','AK','AZ','AR','CA','CO','CT','DE','FL','GA','HI','ID','IL','IN',
            'IA','KS','KY','LA','ME','MD','MA','MI','MN','MS','MO','MT','NE','NV',
            'NH','NJ','NM','NY','NC','ND','OH','OK','OR','PA','RI','SC','SD','TN',
            'TX','UT','VT','VA','WA','WV','WI','WY','DC',
        }
        
        # Try explicit address patterns first
        for pattern in self.PATTERNS['address']:
            match = re.search(pattern, text, re.IGNORECASE | re.MULTILINE)
            if match:
                addr = match.group(1).strip()
                if len(addr) > 10:
                    return addr
        
        # Look for lines with valid state abbreviation + zip code
        lines = text.split('\n')
        for i, line in enumerate(lines):
            stripped = line.strip()
            state_zip_match = re.search(r'\b([A-Z]{2})\s+(\d{5})\b', stripped)
            if state_zip_match:
                state = state_zip_match.group(1)
                if state not in US_STATES:
                    continue  # Skip false positives like "er 00068"
                # Collect preceding lines as address
                addr_parts = []
                for j in range(max(0, i - 2), i + 1):
                    part = lines[j].strip()
                    if part and len(part) > 3:
                        addr_parts.append(part)
                if addr_parts:
                    return ', '.join(addr_parts)
        
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
            match = re.search(
                r'Approved\s+Date\s*[\n\r]+([A-Za-z][A-Za-z\s\']+(?:Inc|Corp|LLC|Services?|Construction)?[A-Za-z\s]*)',
                text, re.IGNORECASE
            )
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
        
        # Extract address
        address = self._extract_address(text)
        
        return VendorInfo(
            name=vendor_name,
            address=address,
            email=email,
            phone=phone
        )
    
    def _extract_line_items(self, text: str) -> List[LineItem]:
        """
        Extract line items from PO/invoice text.
        
        Handles TWO formats:
        1. Inline (pdfplumber): description text qty $unit_price $total_price on same/adjacent lines
        2. Stacked (PyMuPDF): each value on its own line ($unit_price then $total_price)
        
        Strategy: Find all lines containing the numeric triple (qty $price $total),
        collect description text from preceding non-numeric lines and any text
        prefix before the numbers on the same line.
        """
        line_items = []
        lines = text.split('\n')
        
        # ── Locate the line-item section ───────────────────────────────
        start_idx = 0
        end_idx = len(lines)
        
        for i, line in enumerate(lines):
            stripped = line.strip()
            # Header row: "Product Name", "Quantity", "Price", etc.
            if re.search(r'Product\s+Name|Description\s+Price|Line\s+Unit\s+Total', stripped, re.IGNORECASE):
                start_idx = i + 1
            # Skip sub-header lines like "Quantity", "Price", "Description"
            if start_idx > 0 and i == start_idx and re.match(
                r'^(Price|Quantity|Description|Product\s+Name)\s*$', stripped, re.IGNORECASE
            ):
                start_idx = i + 1
                continue
            # Stop at "Total:" row
            if re.match(r'^\s*Total\s*:', stripped, re.IGNORECASE):
                end_idx = i
                break
        
        # Skip any remaining sub-header lines
        while start_idx < end_idx:
            stripped = lines[start_idx].strip()
            if re.match(r'^(Price|Quantity|Description|Product\s+Name)\s*$', stripped, re.IGNORECASE):
                start_idx += 1
            else:
                break
        
        # ── Pattern: qty $unit_price $total_price (inline on one line) ─
        # Matches: "13.16 $130.00 $1,710.80" or "Some desc text 7.20 $170.00 $1,224.00"
        INLINE_PATTERN = re.compile(
            r'([\d,]+\.?\d*)\s+\$([\d,]+\.?\d*)\s+\$([\d,]+\.?\d*)'
        )
        
        line_number = 0
        desc_buffer: List[str] = []
        
        for i in range(start_idx, end_idx):
            line = lines[i].strip()
            if not line:
                continue
            
            # Skip notes/instructions section
            if re.search(r'notes|special\s+instructions', line, re.IGNORECASE):
                continue
            
            match = INLINE_PATTERN.search(line)
            if match:
                # Text before the numeric triple is part of the description
                prefix = line[:match.start()].strip()
                if prefix:
                    desc_buffer.append(prefix)
                
                try:
                    qty = Decimal(match.group(1).replace(',', ''))
                    unit_price = Decimal(match.group(2).replace(',', ''))
                    total_price = Decimal(match.group(3).replace(',', ''))
                    
                    line_number += 1
                    description = ' '.join(desc_buffer).strip()
                    
                    line_items.append(LineItem(
                        line_number=line_number,
                        description=description if description else None,
                        quantity=qty,
                        unit_price=unit_price,
                        line_total=total_price,
                    ))
                    
                    # Text after the numeric triple starts next item's description
                    suffix = line[match.end():].strip()
                    desc_buffer = [suffix] if suffix else []
                except (InvalidOperation, ValueError):
                    desc_buffer.append(line)
            else:
                # Non-numeric line → accumulate as description
                desc_buffer.append(line)
        
        # Leftover description lines belong to the last item
        if desc_buffer and line_items:
            extra_desc = ' '.join(desc_buffer).strip()
            if extra_desc:
                last = line_items[-1]
                if last.description:
                    last.description += ' ' + extra_desc
                else:
                    last.description = extra_desc
        
        # ── Fallback: try stacked (PyMuPDF) format ─────────────────────
        if not line_items:
            line_items = self._extract_line_items_stacked(text)
        
        if not line_items:
            line_items = self._extract_line_items_pattern_based(text)
        
        # Post-process: fix description bleed-over from continuation lines
        line_items = self._fix_description_continuations(line_items)
        
        return line_items
    
    def _fix_description_continuations(self, items: List[LineItem]) -> List[LineItem]:
        """
        Fix descriptions where continuation text from item N appears at the
        start of item N+1's description.
        
        pdfplumber sometimes places wrapped cell text after the numeric values,
        so continuation lines (e.g. "- 6/12") end up prepended to the next item.
        
        Heuristic: if item's description starts with a fragment that begins with
        "-" or lowercase, move it to the previous item.
        """
        for i in range(1, len(items)):
            desc = items[i].description or ""
            if not desc:
                continue
            
            # Case 1: Description starts with "- " continuation fragment (numeric/slash content)
            # e.g. "- 6/12 Mansard Roof / 10/12 above" → move "- 6/12" to prev item
            cont_match = re.match(r'^(-\s*[\d/\.\-\s]+?)\s+([A-Z][A-Za-z].*)$', desc, re.DOTALL)
            if cont_match:
                continuation = cont_match.group(1).strip()
                real_desc = cont_match.group(2).strip()
                prev = items[i - 1]
                if prev.description:
                    prev.description += ' ' + continuation
                else:
                    prev.description = continuation
                items[i].description = real_desc
                continue
            
            # Case 2: Description starts with lowercase fragment before a capitalized phrase
            # e.g. "above extra detail Truss Repair Labor Only"
            cont_match2 = re.match(r'^([a-z][^.]+?)\s+([A-Z][A-Za-z].+)$', desc, re.DOTALL)
            if cont_match2:
                continuation = cont_match2.group(1).strip()
                real_desc = cont_match2.group(2).strip()
                prev = items[i - 1]
                if prev.description:
                    prev.description += ' ' + continuation
                else:
                    prev.description = continuation
                items[i].description = real_desc
        
        return items
    
    def _extract_line_items_stacked(self, text: str) -> List[LineItem]:
        """
        Extract line items in stacked format where each value is on its own line:
          Description (one or more lines)
          quantity (number)
          $unit_price
          $total_price
        
        Used when PyMuPDF extracts the PDF with values on separate lines.
        """
        line_items = []
        lines = text.split('\n')
        
        # Find header and total boundaries
        header_idx = -1
        end_idx = len(lines)
        
        for i, line in enumerate(lines):
            if 'Product Name' in line or ('Total' in line and 'Price' in line):
                header_idx = i
            if re.match(r'\s*Total\s*:', line.strip()):
                end_idx = i
                break
        
        if header_idx == -1:
            return []
        
        # Skip header rows
        start_idx = header_idx + 1
        while start_idx < end_idx:
            if re.match(r'^(Price|Quantity|Description|Product\s+Name)$', lines[start_idx].strip(), re.IGNORECASE):
                start_idx += 1
            else:
                break
        
        line_number = 0
        desc_buffer: List[str] = []
        i = start_idx
        
        while i < end_idx:
            line = lines[i].strip()
            if not line:
                i += 1
                continue
            
            # Look for $price $total on consecutive lines after a quantity line
            if line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', line):
                if i + 1 < end_idx:
                    next_line = lines[i + 1].strip()
                    if next_line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', next_line):
                        # Walk backwards for quantity
                        qty_idx = i - 1
                        qty_line = lines[qty_idx].strip() if qty_idx >= start_idx else ""
                        while qty_idx >= start_idx and not re.match(r'^[\d,]+\.?\d*$', qty_line):
                            qty_idx -= 1
                            qty_line = lines[qty_idx].strip() if qty_idx >= start_idx else ""
                        
                        if re.match(r'^[\d,]+\.?\d*$', qty_line):
                            try:
                                quantity = Decimal(qty_line.replace(',', ''))
                                unit_price = Decimal(line.replace('$', '').replace(',', ''))
                                total_price = Decimal(next_line.replace('$', '').replace(',', ''))
                                
                                line_number += 1
                                description = ' '.join(desc_buffer).strip()
                                
                                line_items.append(LineItem(
                                    line_number=line_number,
                                    description=description if description else None,
                                    quantity=quantity,
                                    unit_price=unit_price,
                                    line_total=total_price
                                ))
                                
                                desc_buffer = []
                                i += 2
                                continue
                            except (InvalidOperation, ValueError):
                                pass
                i += 1
                continue
            
            # Skip standalone qty numbers
            if re.match(r'^[\d,]+\.?\d*$', line):
                i += 1
                continue
            
            if not any(skip in line.lower() for skip in ['notes', 'special', 'instructions']):
                desc_buffer.append(line)
            
            i += 1
        
        return line_items
    
    def _extract_line_items_pattern_based(self, text: str) -> List[LineItem]:
        """
        Extract line items using pattern matching on the full text.
        
        Works by finding $total patterns and looking backwards for the structure.
        """
        line_items = []
        lines = [l.strip() for l in text.split('\n') if l.strip()]
        
        line_number = 0
        i = 0
        while i < len(lines):
            line = lines[i]
            
            if line.startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', line):
                if i > 0 and lines[i-1].startswith('$') and re.match(r'^\$[\d,]+\.?\d*$', lines[i-1]):
                    if i > 1 and re.match(r'^[\d,]+\.?\d*$', lines[i-2]):
                        qty_line = lines[i-2]
                        unit_price_line = lines[i-1]
                        total_price_line = line
                        
                        desc_lines = []
                        j = i - 3
                        while j >= 0:
                            prev_line = lines[j]
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
        """Fallback line item extraction using regex patterns."""
        line_items = []
        
        pattern = r'([A-Za-z][^\n]{5,50}?)\s+([\d,]+\.?\d*)\s+\$?([\d,]+\.?\d+)\s+\$?([\d,]+\.?\d+)'
        matches = re.findall(pattern, text, re.MULTILINE)
        
        for i, match in enumerate(matches, 1):
            try:
                description, qty_str, price_str, total_str = match
                
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
            complete_items = sum(1 for item in line_items if item.line_total and item.quantity)
            item_score = min(complete_items / max(len(line_items), 1), 1.0)
            score += weights['line_items'] * item_score
        
        if total and line_items:
            calculated_total = sum(item.line_total for item in line_items if item.line_total)
            if calculated_total and abs(float(total) - float(calculated_total)) < 0.01:
                score += 0.05
        
        return round(min(score, 1.0), 2)
