"""
Pydantic models for invoice data extraction.

Author: vedvix
"""

from datetime import date
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field, field_validator


class LineItem(BaseModel):
    """Invoice line item data."""
    
    line_number: int = Field(..., description="Line item number", ge=1)
    description: Optional[str] = Field(None, description="Item description", max_length=1000)
    item_code: Optional[str] = Field(None, description="Product/service code", max_length=100)
    quantity: Optional[Decimal] = Field(None, description="Quantity", ge=0, le=Decimal("999999999.9999"))
    unit: Optional[str] = Field(None, description="Unit of measure", max_length=50)
    unit_price: Optional[Decimal] = Field(None, description="Price per unit", ge=0, le=Decimal("999999999.9999"))
    tax_rate: Optional[Decimal] = Field(None, description="Tax rate percentage", ge=0, le=Decimal("100"))
    tax_amount: Optional[Decimal] = Field(None, description="Tax amount", ge=0, le=Decimal("999999999.99"))
    discount_amount: Optional[Decimal] = Field(None, description="Discount amount", ge=0, le=Decimal("999999999.99"))
    line_total: Optional[Decimal] = Field(None, description="Total for this line", ge=0, le=Decimal("999999999.99"))
    gl_account_code: Optional[str] = Field(None, description="GL account code", max_length=50)
    cost_center: Optional[str] = Field(None, description="Cost center code", max_length=100)


class VendorInfo(BaseModel):
    """Vendor information extracted from invoice."""
    
    name: Optional[str] = Field(None, description="Vendor/supplier name", max_length=500)
    address: Optional[str] = Field(None, description="Vendor address", max_length=1000)
    email: Optional[str] = Field(None, description="Vendor email", max_length=254)
    phone: Optional[str] = Field(None, description="Vendor phone number", max_length=50)
    tax_id: Optional[str] = Field(None, description="Vendor tax ID / VAT number", max_length=50)
    website: Optional[str] = Field(None, description="Vendor website", max_length=500)


class InvoiceData(BaseModel):
    """Extracted invoice data."""
    
    # Invoice identification
    invoice_number: Optional[str] = Field(None, description="Invoice number", max_length=100)
    po_number: Optional[str] = Field(None, description="Purchase order number", max_length=100)
    
    # Vendor information
    vendor: VendorInfo = Field(default_factory=VendorInfo, description="Vendor details")
    
    # Dates
    invoice_date: Optional[date] = Field(None, description="Invoice date")
    due_date: Optional[date] = Field(None, description="Payment due date")
    
    # Financial details
    subtotal: Optional[Decimal] = Field(None, description="Subtotal before tax", ge=0, le=Decimal("999999999.99"))
    tax_amount: Optional[Decimal] = Field(None, description="Total tax amount", ge=0, le=Decimal("999999999.99"))
    discount_amount: Optional[Decimal] = Field(None, description="Total discount", ge=0, le=Decimal("999999999.99"))
    shipping_amount: Optional[Decimal] = Field(None, description="Shipping/freight charges", ge=0, le=Decimal("999999999.99"))
    total_amount: Optional[Decimal] = Field(None, description="Total invoice amount", ge=0, le=Decimal("999999999.99"))
    currency: str = Field(default="USD", description="Currency code", min_length=3, max_length=3)
    
    # Line items
    line_items: List[LineItem] = Field(default_factory=list, description="Invoice line items")
    
    # Payment information
    payment_terms: Optional[str] = Field(None, description="Payment terms", max_length=200)
    bank_details: Optional[str] = Field(None, description="Bank account details", max_length=500)
    
    # Mapped fields (populated by mapping engine)
    gl_account: Optional[str] = Field(None, description="GL account code from mapping", max_length=50)
    project: Optional[str] = Field(None, description="Project/opportunity number from mapping", max_length=100)
    item_category: Optional[str] = Field(None, description="Item/product category from mapping", max_length=100)
    location: Optional[str] = Field(None, description="Location/address from mapping", max_length=500)
    cost_center: Optional[str] = Field(None, description="Cost center from mapping", max_length=100)
    mapping_profile_id: Optional[str] = Field(None, description="ID of mapping profile used", max_length=100)
    
    # Extraction metadata
    confidence_score: float = Field(default=0.0, ge=0.0, le=1.0, description="Extraction confidence 0-1")
    requires_manual_review: bool = Field(default=False, description="Needs human review")
    raw_text: Optional[str] = Field(None, description="Raw extracted text")
    
    @field_validator("due_date")
    @classmethod
    def validate_due_date(cls, v, info):
        if v and info.data.get("invoice_date") and v < info.data["invoice_date"]:
            # Allow but flag — some invoices genuinely have past-due dates
            pass
        return v
    
    class Config:
        json_encoders = {
            Decimal: lambda v: float(v) if v else None,
            date: lambda v: v.isoformat() if v else None,
        }


class ExtractionResult(BaseModel):
    """Result from PDF text extraction."""
    
    text: str = Field(..., description="Extracted text content")
    method: str = Field(..., description="Extraction method used")
    page_count: int = Field(..., description="Number of pages")
    needs_ocr: bool = Field(default=False, description="Whether OCR is needed")
    processing_time_ms: int = Field(..., description="Processing time in milliseconds")


class ExtractionResponse(BaseModel):
    """API response for extraction endpoint."""
    
    success: bool = Field(..., description="Whether extraction succeeded")
    data: Optional[InvoiceData] = Field(None, description="Extracted invoice data")
    extraction_method: str = Field(..., description="Method used for extraction")
    page_count: int = Field(..., description="Number of pages in PDF")
    processing_time_ms: int = Field(..., description="Total processing time")
    error: Optional[str] = Field(None, description="Error message if failed")
    invoice_id: Optional[int] = Field(None, description="Database ID if saved")
    mapping_info: Optional[dict] = Field(None, description="Mapping profile info and results")
    
    # AI extraction metadata
    ai_tier_used: Optional[str] = Field(None, description="AI extraction tier used (gpt4o_vision, gpt4o_text, regex_parser)")
    ai_confidence: Optional[float] = Field(None, description="AI cross-validated confidence score")
    ai_cost_usd: Optional[float] = Field(None, description="Estimated AI API cost for this extraction")
    ai_token_usage: Optional[dict] = Field(None, description="Token usage breakdown")
    ai_validation: Optional[dict] = Field(None, description="Cross-validation results summary")


class SaveInvoiceRequest(BaseModel):
    """Request to save extracted invoice to database."""
    
    invoice_data: InvoiceData = Field(..., description="Extracted invoice data")
    original_filename: str = Field(..., description="Original PDF filename")
    s3_key: str = Field(..., description="S3 storage key")
    s3_url: Optional[str] = Field(None, description="S3 presigned URL")
    file_size: Optional[int] = Field(None, description="File size in bytes")
    page_count: Optional[int] = Field(None, description="Number of pages")
    extraction_method: str = Field(default="pymupdf", description="Extraction method")
    extraction_duration_ms: Optional[int] = Field(None, description="Processing time")
    source_email_id: Optional[str] = Field(None, description="Source email ID")
    source_email_from: Optional[str] = Field(None, description="Source email sender")
    source_email_subject: Optional[str] = Field(None, description="Source email subject")


class SaveInvoiceResponse(BaseModel):
    """Response after saving invoice."""
    
    success: bool = Field(..., description="Whether save succeeded")
    invoice_id: Optional[int] = Field(None, description="Created invoice ID")
    invoice_number: Optional[str] = Field(None, description="Invoice number")
    error: Optional[str] = Field(None, description="Error message if failed")


class BatchProcessRequest(BaseModel):
    """Request to process multiple PDF files."""
    
    files: List[str] = Field(..., description="List of file paths or S3 URLs")
    save_to_db: bool = Field(default=True, description="Whether to save to database")
    source_email_id: Optional[str] = Field(None, description="Common source email ID")


class BatchProcessResponse(BaseModel):
    """Response from batch processing."""
    
    success: bool = Field(..., description="Overall success status")
    total_files: int = Field(..., description="Total files processed")
    successful: int = Field(..., description="Successfully processed count")
    failed: int = Field(..., description="Failed count")
    results: List[ExtractionResponse] = Field(..., description="Individual results")


class AiConfig(BaseModel):
    """Optional AI configuration overrides passed per-request from runtime config."""
    
    provider: Optional[str] = Field(None, description="AI provider (openai)")
    api_key: Optional[str] = Field(None, description="AI provider API key")
    vision_model: Optional[str] = Field(None, description="Model for vision extraction")
    text_model: Optional[str] = Field(None, description="Model for text extraction")
    enable_vision: Optional[bool] = Field(None, description="Enable vision extraction")
    enable_text_llm: Optional[bool] = Field(None, description="Enable text LLM extraction")
    enable_cross_validation: Optional[bool] = Field(None, description="Enable cross-validation")


class ExtractFromUrlRequest(BaseModel):
    """Request to extract invoice from URL (multi-tenant)."""
    
    file_url: str = Field(..., description="Presigned URL to download PDF")
    file_name: str = Field(..., description="Original filename")
    organization_id: int = Field(..., description="Organization ID for multi-tenant context")
    invoice_id: Optional[int] = Field(None, description="Existing invoice ID to update")
    ai_config: Optional[AiConfig] = Field(None, description="AI configuration overrides from runtime config")


class HealthResponse(BaseModel):
    """Health check response."""
    
    status: str = Field(..., description="Service status")
    service: str = Field(..., description="Service name")
    version: str = Field(..., description="Service version")
    database: str = Field(default="unknown", description="Database connection status")
    ai_status: str = Field(default="unknown", description="AI extraction service status")
