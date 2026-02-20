"""
Cross-validation service for comparing AI extraction results with regex extraction.

Compares field-by-field between AI (GPT-4o) and regex (FieldParser) results
to calculate a final confidence score and flag discrepancies for review.

Author: vedvix
"""

import re
from datetime import date
from decimal import Decimal, InvalidOperation
from typing import Any, Dict, List, Optional

import structlog

from models.invoice_data import InvoiceData, LineItem
from services.ai_models import (
    AIInvoiceExtraction,
    CrossValidationResult,
    FieldValidation,
)

logger = structlog.get_logger(__name__)


class CrossValidationService:
    """
    Cross-validates AI extraction results against regex-based extraction.
    
    The validation process:
    1. Compare critical fields (invoice_number, total, vendor, dates)
    2. Compare financial fields (subtotal, tax, line item totals)
    3. Calculate agreement score
    4. Adjust AI confidence based on agreement/disagreement
    5. Flag items for manual review when discrepancies are significant
    """
    
    # Fields to compare and their weights for confidence scoring
    CRITICAL_FIELDS = {
        "invoice_number": 0.20,    # Most important
        "total_amount": 0.25,      # Financial accuracy critical
        "vendor_name": 0.15,       # Vendor identification
        "invoice_date": 0.10,      # Date accuracy
    }
    
    IMPORTANT_FIELDS = {
        "subtotal": 0.08,
        "tax_amount": 0.05,
        "due_date": 0.05,
        "po_number": 0.05,
        "vendor_email": 0.03,
        "vendor_phone": 0.02,
        "vendor_address": 0.02,
    }
    
    # Amount tolerance for numerical comparisons (e.g., $100.00 vs $100.01)
    AMOUNT_TOLERANCE = 0.02  # $0.02
    AMOUNT_TOLERANCE_PCT = 0.005  # 0.5% relative tolerance
    
    def validate(
        self,
        ai_result: AIInvoiceExtraction,
        regex_fields: Dict[str, Any],
        regex_line_items: List[LineItem],
    ) -> CrossValidationResult:
        """
        Cross-validate AI extraction against regex extraction.
        
        Args:
            ai_result: Structured data from GPT-4o
            regex_fields: Raw fields dict from FieldParser.parse_raw_fields()
            regex_line_items: Line items from FieldParser._extract_line_items()
            
        Returns:
            CrossValidationResult with detailed comparison
        """
        validations: List[FieldValidation] = []
        notes: List[str] = []
        
        # Convert AI result to comparable dict
        ai_fields = self._ai_to_comparable(ai_result)
        regex_comparable = self._regex_to_comparable(regex_fields)
        
        # Compare critical fields
        for field, weight in self.CRITICAL_FIELDS.items():
            validation = self._compare_field(
                field, ai_fields.get(field), regex_comparable.get(field), weight
            )
            validations.append(validation)
        
        # Compare important fields
        for field, weight in self.IMPORTANT_FIELDS.items():
            validation = self._compare_field(
                field, ai_fields.get(field), regex_comparable.get(field), weight
            )
            validations.append(validation)
        
        # Compare line item count
        ai_line_count = len(ai_result.line_items) if ai_result.line_items else 0
        regex_line_count = len(regex_line_items)
        if ai_line_count > 0 and regex_line_count > 0:
            if ai_line_count == regex_line_count:
                notes.append(f"Line item count matches: {ai_line_count}")
            else:
                notes.append(
                    f"Line item count mismatch: AI={ai_line_count}, Regex={regex_line_count}"
                )
        elif ai_line_count > 0:
            notes.append(f"AI found {ai_line_count} line items, regex found none")
        elif regex_line_count > 0:
            notes.append(f"Regex found {regex_line_count} line items, AI found none")
        
        # Validate line item totals consistency
        if ai_result.line_items and ai_result.total_amount:
            ai_line_sum = sum(
                item.line_total for item in ai_result.line_items 
                if item.line_total is not None
            )
            if ai_line_sum > 0:
                diff = abs(ai_line_sum - ai_result.total_amount)
                subtotal = ai_result.subtotal or ai_result.total_amount
                tax = ai_result.tax_amount or 0
                expected_total = subtotal + tax - (ai_result.discount_amount or 0)
                if diff < self.AMOUNT_TOLERANCE or (ai_result.tax_amount and abs(ai_line_sum - subtotal) < self.AMOUNT_TOLERANCE):
                    notes.append("AI line items sum matches total/subtotal")
                else:
                    notes.append(
                        f"AI line items sum (${ai_line_sum:.2f}) differs from total (${ai_result.total_amount:.2f})"
                    )
        
        # Calculate aggregate scores
        total_compared = len([v for v in validations if v.ai_value or v.regex_value])
        matching = len([v for v in validations if v.match])
        mismatched = len([v for v in validations if not v.match and v.ai_value and v.regex_value])
        ai_only = len([v for v in validations if v.ai_value and not v.regex_value])
        regex_only = len([v for v in validations if v.regex_value and not v.ai_value])
        
        # Weighted validation score
        total_weight = sum(v.confidence_adjustment for v in validations if v.match)
        max_weight = sum(
            self.CRITICAL_FIELDS.get(v.field_name, 0) + self.IMPORTANT_FIELDS.get(v.field_name, 0)
            for v in validations
            if v.ai_value or v.regex_value
        )
        validation_score = total_weight / max_weight if max_weight > 0 else 0.5
        
        # Calculate final confidence
        ai_confidence = ai_result.ai_confidence or 0.7
        final_confidence = self._calculate_final_confidence(
            ai_confidence, validation_score, matching, mismatched, ai_only, total_compared
        )
        
        # Determine if review is needed
        recommended_review = (
            final_confidence < 0.7
            or any(
                not v.match and v.field_name in self.CRITICAL_FIELDS
                and v.ai_value and v.regex_value
                for v in validations
            )
        )
        
        if recommended_review:
            # Identify which critical fields disagree
            disagreeing = [
                v.field_name for v in validations
                if not v.match and v.field_name in self.CRITICAL_FIELDS
                and v.ai_value and v.regex_value
            ]
            if disagreeing:
                notes.append(f"Manual review recommended: critical field disagreements on {', '.join(disagreeing)}")
        
        result = CrossValidationResult(
            total_fields_compared=total_compared,
            matching_fields=matching,
            mismatched_fields=mismatched,
            ai_only_fields=ai_only,
            regex_only_fields=regex_only,
            validation_score=round(validation_score, 3),
            field_validations=validations,
            final_confidence=round(final_confidence, 3),
            recommended_review=recommended_review,
            notes=notes,
        )
        
        logger.info(
            "Cross-validation complete",
            compared=total_compared,
            matching=matching,
            mismatched=mismatched,
            ai_only=ai_only,
            validation_score=f"{validation_score:.2f}",
            final_confidence=f"{final_confidence:.2f}",
            review_needed=recommended_review,
        )
        
        return result
    
    def _compare_field(
        self,
        field_name: str,
        ai_value: Optional[str],
        regex_value: Optional[str],
        weight: float,
    ) -> FieldValidation:
        """Compare a single field between AI and regex extraction."""
        
        # Both null = no comparison possible
        if ai_value is None and regex_value is None:
            return FieldValidation(
                field_name=field_name,
                match=True,
                confidence_adjustment=0,
                note="Both sources returned null",
            )
        
        # One is null
        if ai_value is None:
            return FieldValidation(
                field_name=field_name,
                regex_value=regex_value,
                match=False,
                confidence_adjustment=0,
                note="AI returned null, regex found value",
            )
        if regex_value is None:
            return FieldValidation(
                field_name=field_name,
                ai_value=ai_value,
                match=False,
                confidence_adjustment=weight * 0.5,  # Partial credit for AI-only
                note="AI found value, regex returned null (AI possibly better)",
            )
        
        # Both have values — compare
        is_match = self._values_match(field_name, ai_value, regex_value)
        
        return FieldValidation(
            field_name=field_name,
            ai_value=ai_value,
            regex_value=regex_value,
            match=is_match,
            confidence_adjustment=weight if is_match else -weight * 0.5,
            note="Values match" if is_match else f"Mismatch: AI='{ai_value}' vs Regex='{regex_value}'",
        )
    
    def _values_match(self, field_name: str, ai_val: str, regex_val: str) -> bool:
        """
        Check if two field values match, using appropriate comparison logic.
        
        - Amounts: numerical comparison with tolerance
        - Dates: date comparison
        - Strings: normalized string comparison
        """
        if ai_val is None or regex_val is None:
            return ai_val == regex_val
        
        # Amount fields
        if field_name in ("total_amount", "subtotal", "tax_amount", "discount_amount", "shipping_amount"):
            return self._amounts_match(ai_val, regex_val)
        
        # Date fields
        if field_name in ("invoice_date", "due_date"):
            return self._dates_match(ai_val, regex_val)
        
        # String fields - normalize and compare
        return self._strings_match(ai_val, regex_val)
    
    def _amounts_match(self, a: str, b: str) -> bool:
        """Compare two amount strings with tolerance."""
        try:
            val_a = float(re.sub(r'[,$]', '', str(a)))
            val_b = float(re.sub(r'[,$]', '', str(b)))
            
            # Absolute tolerance
            if abs(val_a - val_b) <= self.AMOUNT_TOLERANCE:
                return True
            
            # Relative tolerance
            max_val = max(abs(val_a), abs(val_b))
            if max_val > 0 and abs(val_a - val_b) / max_val <= self.AMOUNT_TOLERANCE_PCT:
                return True
            
            return False
        except (ValueError, TypeError):
            return str(a).strip() == str(b).strip()
    
    def _dates_match(self, a: str, b: str) -> bool:
        """Compare two date values."""
        try:
            from dateutil import parser as date_parser
            
            date_a = date_parser.parse(str(a)).date() if not isinstance(a, date) else a
            date_b = date_parser.parse(str(b)).date() if not isinstance(b, date) else b
            
            return date_a == date_b
        except Exception:
            # Fall back to string comparison
            return self._strings_match(str(a), str(b))
    
    def _strings_match(self, a: str, b: str) -> bool:
        """Compare two strings with normalization."""
        # Normalize: lowercase, strip whitespace, remove extra spaces
        norm_a = re.sub(r'\s+', ' ', str(a).lower().strip())
        norm_b = re.sub(r'\s+', ' ', str(b).lower().strip())
        
        if norm_a == norm_b:
            return True
        
        # Check if one contains the other (partial match for vendor names etc.)
        if len(norm_a) > 3 and len(norm_b) > 3:
            if norm_a in norm_b or norm_b in norm_a:
                return True
        
        # Check for common abbreviation patterns
        # e.g., "MGD Construction Services" vs "MGD Construction Services Inc."
        if norm_a.replace('.', '').replace(',', '') == norm_b.replace('.', '').replace(',', ''):
            return True
        
        return False
    
    def _ai_to_comparable(self, ai_result: AIInvoiceExtraction) -> Dict[str, Optional[str]]:
        """Convert AI extraction to comparable dict of strings."""
        return {
            "invoice_number": ai_result.invoice_number,
            "po_number": ai_result.po_number,
            "vendor_name": ai_result.vendor.name if ai_result.vendor else None,
            "vendor_email": ai_result.vendor.email if ai_result.vendor else None,
            "vendor_phone": ai_result.vendor.phone if ai_result.vendor else None,
            "vendor_address": ai_result.vendor.address if ai_result.vendor else None,
            "invoice_date": ai_result.invoice_date,
            "due_date": ai_result.due_date,
            "total_amount": str(ai_result.total_amount) if ai_result.total_amount is not None else None,
            "subtotal": str(ai_result.subtotal) if ai_result.subtotal is not None else None,
            "tax_amount": str(ai_result.tax_amount) if ai_result.tax_amount is not None else None,
        }
    
    def _regex_to_comparable(self, regex_fields: Dict[str, Any]) -> Dict[str, Optional[str]]:
        """Convert regex raw_fields to comparable dict of strings."""
        def to_str(val: Any) -> Optional[str]:
            if val is None:
                return None
            if isinstance(val, date):
                return val.isoformat()
            if isinstance(val, Decimal):
                return str(float(val))
            return str(val)
        
        return {
            "invoice_number": to_str(regex_fields.get("invoice_number")),
            "po_number": to_str(regex_fields.get("po_number")),
            "vendor_name": to_str(regex_fields.get("vendor_name")),
            "vendor_email": to_str(regex_fields.get("vendor_email")),
            "vendor_phone": to_str(regex_fields.get("vendor_phone")),
            "vendor_address": to_str(regex_fields.get("vendor_address")),
            "invoice_date": to_str(
                regex_fields.get("invoice_date") or regex_fields.get("order_date")
            ),
            "due_date": to_str(regex_fields.get("due_date")),
            "total_amount": to_str(regex_fields.get("total")),
            "subtotal": to_str(regex_fields.get("subtotal")),
            "tax_amount": to_str(regex_fields.get("tax_amount")),
        }
    
    def _calculate_final_confidence(
        self,
        ai_confidence: float,
        validation_score: float,
        matching: int,
        mismatched: int,
        ai_only: int,
        total_compared: int,
    ) -> float:
        """
        Calculate the final confidence score combining AI self-confidence and validation.
        
        Formula:
        - Base: weighted average of AI confidence (60%) and validation score (40%)
        - Bonus: +5% if all critical fields match
        - Penalty: -10% per critical field mismatch
        - AI-only bonus: +2% for fields only AI found (it may be better)
        """
        if total_compared == 0:
            return ai_confidence
        
        # Weighted average
        base = (ai_confidence * 0.6) + (validation_score * 0.4)
        
        # Adjustments
        if mismatched == 0 and matching > 0:
            base += 0.05  # All-match bonus
        
        # AI-only fields slight bonus (AI found more)
        if ai_only > 0 and total_compared > 0:
            base += min(ai_only * 0.02, 0.06)
        
        # Clamp to 0-1
        return max(0.0, min(1.0, base))
