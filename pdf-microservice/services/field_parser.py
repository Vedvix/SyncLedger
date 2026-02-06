"""
Field parser for extracting structured data from invoice text.

Author: vedvix
"""

import re
from datetime import date
from decimal import Decimal, InvalidOperation
from typing import List, Optional, Tuple

import structlog
from dateutil import parser as date_parser

from models.invoice_data import InvoiceData, LineItem, VendorInfo

logger = structlog.get_logger(__name__)


class FieldParser:
    """
    Parse extracted text into structured invoice data.
    
    Uses regex patterns and heuristics to identify invoice fields.
    """
    
    # Regex patterns for field extraction
    PATTERNS = {
        'invoice_number': [
            r'invoice\s*#?\s*:?\s*([A-Z0-9\-]+)',
            r'inv\s*#?\s*:?\s*([A-Z0-9\-]+)',
            r'invoice\s+number\s*:?\s*([A-Z0-9\-]+)',
            r'bill\s*#?\s*:?\s*([A-Z0-9\-]+)',
        ],
        'po_number': [
            r'p\.?o\.?\s*#?\s*:?\s*([A-Z0-9\-]+)',
            r'purchase\s+order\s*#?\s*:?\s*([A-Z0-9\-]+)',
        ],
        'date': [
            r'date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'invoice\s+date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'([A-Za-z]+\s+\d{1,2},?\s+\d{4})',
        ],
        'due_date': [
            r'due\s+date\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'payment\s+due\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
            r'due\s*:?\s*(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4})',
        ],
        'total': [
            r'total\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'amount\s+due\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'grand\s+total\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'balance\s+due\s*:?\s*\$?\s*([\d,]+\.?\d*)',
        ],
        'subtotal': [
            r'subtotal\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'sub\s*-?\s*total\s*:?\s*\$?\s*([\d,]+\.?\d*)',
        ],
        'tax': [
            r'tax\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'vat\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'sales\s+tax\s*:?\s*\$?\s*([\d,]+\.?\d*)',
            r'gst\s*:?\s*\$?\s*([\d,]+\.?\d*)',
        ],
        'email': [
            r'([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})',
        ],
        'phone': [
            r'(?:phone|tel|fax)\s*:?\s*([\d\-\(\)\s\+]+)',
            r'(\+?1?\s*[\(\-]?\d{3}[\)\-\s]?\d{3}[\-\s]?\d{4})',
        ],
        'tax_id': [
            r'tax\s*id\s*:?\s*([A-Z0-9\-]+)',
            r'vat\s*(?:no|number|#)?\s*:?\s*([A-Z0-9\-]+)',
            r'ein\s*:?\s*(\d{2}-\d{7})',
        ],
    }
    
    async def parse(self, text: str) -> InvoiceData:
        """
        Parse extracted text into structured invoice data.
        
        Args:
            text: Raw extracted text from PDF
            
        Returns:
            InvoiceData with parsed fields
        """
        logger.info("Parsing invoice text", text_length=len(text))
        
        text_lower = text.lower()
        
        # Extract fields using patterns
        invoice_number = self._extract_pattern(text, self.PATTERNS['invoice_number'])
        po_number = self._extract_pattern(text, self.PATTERNS['po_number'])
        
        invoice_date = self._extract_date(text, self.PATTERNS['date'])
        due_date = self._extract_date(text, self.PATTERNS['due_date'])
        
        total = self._extract_amount(text, self.PATTERNS['total'])
        subtotal = self._extract_amount(text, self.PATTERNS['subtotal'])
        tax = self._extract_amount(text, self.PATTERNS['tax'])
        
        # Extract vendor info
        vendor = self._extract_vendor_info(text)
        
        # Extract line items
        line_items = self._extract_line_items(text)
        
        # Calculate confidence score
        confidence = self._calculate_confidence(
            invoice_number=invoice_number,
            invoice_date=invoice_date,
            total=total,
            vendor_name=vendor.name
        )
        
        requires_review = confidence < 0.7 or invoice_number is None or total is None
        
        logger.info(
            "Parsing completed",
            invoice_number=invoice_number,
            total=total,
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
    
    def _extract_pattern(self, text: str, patterns: List[str]) -> Optional[str]:
        """Extract first matching pattern from text."""
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
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
                # Remove commas and convert to Decimal
                cleaned = amount_str.replace(',', '')
                return Decimal(cleaned)
            except InvalidOperation:
                pass
        return None
    
    def _extract_vendor_info(self, text: str) -> VendorInfo:
        """Extract vendor information from text."""
        # Extract individual fields
        email = self._extract_pattern(text, self.PATTERNS['email'])
        phone = self._extract_pattern(text, self.PATTERNS['phone'])
        tax_id = self._extract_pattern(text, self.PATTERNS['tax_id'])
        
        # Try to extract vendor name from first few lines
        lines = [l.strip() for l in text.split('\n') if l.strip()]
        vendor_name = None
        
        # Heuristic: Look for company name in first 10 lines
        for line in lines[:10]:
            # Skip lines that look like addresses or dates
            if re.match(r'^\d', line):
                continue
            if '@' in line or 'invoice' in line.lower():
                continue
            if len(line) > 5 and len(line) < 100:
                # Likely a company name
                vendor_name = line
                break
        
        return VendorInfo(
            name=vendor_name,
            email=email,
            phone=phone,
            tax_id=tax_id
        )
    
    def _extract_line_items(self, text: str) -> List[LineItem]:
        """
        Extract line items from invoice text.
        
        This is a simplified implementation. Production code would need
        more sophisticated table detection and parsing.
        """
        line_items = []
        
        # Pattern to match line items: description followed by quantity, price, total
        line_pattern = r'(.{10,50})\s+(\d+(?:\.\d+)?)\s+\$?([\d,]+\.?\d*)\s+\$?([\d,]+\.?\d*)'
        
        matches = re.findall(line_pattern, text, re.MULTILINE)
        
        for i, match in enumerate(matches, 1):
            try:
                description, qty_str, price_str, total_str = match
                
                quantity = Decimal(qty_str)
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
        vendor_name: Optional[str]
    ) -> float:
        """
        Calculate confidence score based on extracted fields.
        
        Returns a score between 0 and 1.
        """
        score = 0.0
        max_score = 4.0
        
        if invoice_number:
            score += 1.0
        if invoice_date:
            score += 1.0
        if total:
            score += 1.0
        if vendor_name:
            score += 1.0
        
        return round(score / max_score, 2)
