"""
AI extraction data models and response schemas.

Defines the structured JSON schema that GPT-4o Vision and Text modes
return, plus validation/cross-check result models.

Author: vedvix
"""

from datetime import date
from decimal import Decimal
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class ExtractionTier(str, Enum):
    """Extraction method tier used."""
    VISION = "gpt4o_vision"       # Tier 2: PDF → Image → GPT-4o Vision
    TEXT_LLM = "gpt4o_text"       # Tier 1: Text → GPT-4o
    REGEX = "regex_parser"        # Tier 0: Legacy regex FieldParser
    VISION_WITH_VALIDATION = "gpt4o_vision_validated"
    TEXT_LLM_WITH_VALIDATION = "gpt4o_text_validated"


class AILineItem(BaseModel):
    """Line item as extracted by AI."""
    line_number: int = Field(default=1)
    description: Optional[str] = None
    item_code: Optional[str] = None
    quantity: Optional[float] = None
    unit: Optional[str] = None
    unit_price: Optional[float] = None
    tax_rate: Optional[float] = None
    tax_amount: Optional[float] = None
    discount_amount: Optional[float] = None
    line_total: Optional[float] = None
    gl_account_code: Optional[str] = None
    cost_center: Optional[str] = None


class AIVendorInfo(BaseModel):
    """Vendor info as extracted by AI."""
    name: Optional[str] = None
    address: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    tax_id: Optional[str] = None
    website: Optional[str] = None


class AIInvoiceExtraction(BaseModel):
    """
    The structured JSON schema returned by GPT-4o.
    
    This is the schema we instruct the LLM to produce.
    Field names match our InvoiceData model for easy mapping.
    """
    # Identification
    invoice_number: Optional[str] = None
    po_number: Optional[str] = None
    
    # Vendor
    vendor: AIVendorInfo = Field(default_factory=AIVendorInfo)
    
    # Dates (ISO format strings from LLM)
    invoice_date: Optional[str] = None
    due_date: Optional[str] = None
    
    # Financial
    subtotal: Optional[float] = None
    tax_amount: Optional[float] = None
    discount_amount: Optional[float] = None
    shipping_amount: Optional[float] = None
    total_amount: Optional[float] = None
    currency: str = "USD"
    
    # Line items
    line_items: List[AILineItem] = Field(default_factory=list)
    
    # Payment
    payment_terms: Optional[str] = None
    bank_details: Optional[str] = None
    
    # Additional metadata fields
    project_number: Optional[str] = None
    sale_number: Optional[str] = None
    opportunity_number: Optional[str] = None
    gl_account: Optional[str] = None
    cost_center: Optional[str] = None
    
    # AI confidence (self-reported by the LLM)
    ai_confidence: Optional[float] = Field(default=None, description="AI self-reported confidence 0-1")
    ai_notes: Optional[str] = Field(default=None, description="AI notes about extraction quality")


class FieldValidation(BaseModel):
    """Validation result for a single field."""
    field_name: str
    ai_value: Optional[str] = None
    regex_value: Optional[str] = None
    match: bool = False
    confidence_adjustment: float = 0.0
    note: Optional[str] = None


class CrossValidationResult(BaseModel):
    """Result of cross-validating AI extraction against regex extraction."""
    total_fields_compared: int = 0
    matching_fields: int = 0
    mismatched_fields: int = 0
    ai_only_fields: int = 0
    regex_only_fields: int = 0
    validation_score: float = 0.0  # 0-1 agreement score
    field_validations: List[FieldValidation] = Field(default_factory=list)
    final_confidence: float = 0.0
    recommended_review: bool = False
    notes: List[str] = Field(default_factory=list)


class AIExtractionResult(BaseModel):
    """Complete result from the AI extraction pipeline."""
    success: bool = True
    tier_used: ExtractionTier = ExtractionTier.REGEX
    ai_extraction: Optional[AIInvoiceExtraction] = None
    regex_extraction: Optional[Dict[str, Any]] = None
    cross_validation: Optional[CrossValidationResult] = None
    final_confidence: float = 0.0
    processing_time_ms: int = 0
    token_usage: Optional[Dict[str, int]] = None
    error: Optional[str] = None
    fallback_reason: Optional[str] = None
    
    # Cost tracking
    estimated_cost_usd: float = 0.0


# ── The prompt schema (for instructing GPT-4o) ─────────────────────────────

INVOICE_JSON_SCHEMA = {
    "type": "object",
    "properties": {
        "invoice_number": {"type": ["string", "null"], "description": "Invoice number, order number, or PO number"},
        "po_number": {"type": ["string", "null"], "description": "Purchase order number if separate from invoice number"},
        "vendor": {
            "type": "object",
            "properties": {
                "name": {"type": ["string", "null"], "description": "Vendor/supplier company name"},
                "address": {"type": ["string", "null"], "description": "Vendor full address"},
                "email": {"type": ["string", "null"], "description": "Vendor email address"},
                "phone": {"type": ["string", "null"], "description": "Vendor phone number"},
                "tax_id": {"type": ["string", "null"], "description": "Vendor tax ID, EIN, or VAT number"},
                "website": {"type": ["string", "null"], "description": "Vendor website URL"},
            }
        },
        "invoice_date": {"type": ["string", "null"], "description": "Invoice date in YYYY-MM-DD format"},
        "due_date": {"type": ["string", "null"], "description": "Payment due date in YYYY-MM-DD format"},
        "subtotal": {"type": ["number", "null"], "description": "Subtotal before tax and discounts"},
        "tax_amount": {"type": ["number", "null"], "description": "Total tax amount"},
        "discount_amount": {"type": ["number", "null"], "description": "Total discount amount"},
        "shipping_amount": {"type": ["number", "null"], "description": "Shipping/freight charges"},
        "total_amount": {"type": ["number", "null"], "description": "Total invoice amount due"},
        "currency": {"type": "string", "description": "Three-letter currency code (e.g., USD, EUR, GBP)", "default": "USD"},
        "line_items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "line_number": {"type": "integer"},
                    "description": {"type": ["string", "null"]},
                    "item_code": {"type": ["string", "null"]},
                    "quantity": {"type": ["number", "null"]},
                    "unit": {"type": ["string", "null"]},
                    "unit_price": {"type": ["number", "null"]},
                    "tax_rate": {"type": ["number", "null"]},
                    "tax_amount": {"type": ["number", "null"]},
                    "discount_amount": {"type": ["number", "null"]},
                    "line_total": {"type": ["number", "null"]},
                    "gl_account_code": {"type": ["string", "null"]},
                    "cost_center": {"type": ["string", "null"]},
                }
            }
        },
        "payment_terms": {"type": ["string", "null"], "description": "Payment terms (e.g., Net 30, Due on Receipt)"},
        "bank_details": {"type": ["string", "null"], "description": "Bank account or payment details"},
        "project_number": {"type": ["string", "null"]},
        "sale_number": {"type": ["string", "null"]},
        "opportunity_number": {"type": ["string", "null"]},
        "gl_account": {"type": ["string", "null"]},
        "cost_center": {"type": ["string", "null"]},
        "ai_confidence": {"type": "number", "description": "Your confidence in the extraction accuracy from 0.0 to 1.0"},
        "ai_notes": {"type": ["string", "null"], "description": "Any notes about extraction quality or ambiguities"},
    },
    "required": ["invoice_number", "vendor", "total_amount", "line_items", "ai_confidence"]
}


VISION_SYSTEM_PROMPT = """You are an expert invoice data extraction AI. You analyze invoice images and extract structured data with high accuracy.

INSTRUCTIONS:
1. Carefully examine the invoice image(s) provided.
2. Extract ALL visible fields into the JSON schema below.
3. For dates, convert to YYYY-MM-DD format regardless of the original format.
4. For amounts, extract as plain numbers (no currency symbols, no commas). Use negative for credits.
5. Extract every line item visible on the invoice.
6. If a field is not visible or cannot be determined, use null.
7. Set ai_confidence between 0.0 and 1.0 based on how confident you are in the extraction accuracy.
8. In ai_notes, mention any ambiguities, unclear text, or fields you are uncertain about.

IMPORTANT RULES:
- Invoice number: Look for "Invoice #", "Invoice No.", "INV-", "Order #", etc.
- PO number: Look for "PO #", "Purchase Order", "P.O." — this is SEPARATE from invoice number.
- Vendor: The company ISSUING the invoice (seller), not the buyer.
- Total: The final amount due. Look for "Total Due", "Amount Due", "Balance Due", "Grand Total".
- Line items: Each product/service row. Include description, quantity, unit price, and line total.
- Dates: Look for invoice date, order date, and due date separately.

Return ONLY valid JSON matching the schema. No markdown, no code blocks, no explanation."""


TEXT_SYSTEM_PROMPT = """You are an expert invoice data extraction AI. You analyze raw text extracted from invoice PDFs and convert it into structured data.

INSTRUCTIONS:
1. The text below was extracted from an invoice PDF. It may have formatting issues, missing spaces, or jumbled layout.
2. Extract ALL identifiable fields into the JSON schema below.
3. For dates, convert to YYYY-MM-DD format regardless of the original format.
4. For amounts, extract as plain numbers (no currency symbols, no commas). Use negative for credits.
5. Extract every line item you can identify.
6. If a field is not identifiable, use null.
7. Set ai_confidence between 0.0 and 1.0 based on how confident you are given the text quality.
8. In ai_notes, mention any parsing difficulties or ambiguities.

IMPORTANT:
- The text may come from OCR and contain errors. Do your best to interpret correctly.
- Look for patterns like "Invoice #:", "Total:", "Vendor:", "Date:" etc.
- Line items are often in tabular format - look for rows with description, qty, price, total.
- The vendor is the company ISSUING the invoice, usually at the top.

Return ONLY valid JSON matching the schema. No markdown, no code blocks, no explanation."""
