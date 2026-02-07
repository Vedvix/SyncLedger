"""
Pydantic models for invoice data extraction.

Author: vedvix
"""

from datetime import date
from decimal import Decimal
from typing import List, Optional

from pydantic import BaseModel, Field


class LineItem(BaseModel):
    """Invoice line item data."""
    
    line_number: int = Field(..., description="Line item number")
    description: Optional[str] = Field(None, description="Item description")
    item_code: Optional[str] = Field(None, description="Product/service code")
    quantity: Optional[Decimal] = Field(None, description="Quantity")
    unit: Optional[str] = Field(None, description="Unit of measure")
    unit_price: Optional[Decimal] = Field(None, description="Price per unit")
    tax_rate: Optional[Decimal] = Field(None, description="Tax rate percentage")
    tax_amount: Optional[Decimal] = Field(None, description="Tax amount")
    discount_amount: Optional[Decimal] = Field(None, description="Discount amount")
    line_total: Optional[Decimal] = Field(None, description="Total for this line")


class VendorInfo(BaseModel):
    """Vendor information extracted from invoice."""
    
    name: Optional[str] = Field(None, description="Vendor/supplier name")
    address: Optional[str] = Field(None, description="Vendor address")
    email: Optional[str] = Field(None, description="Vendor email")
    phone: Optional[str] = Field(None, description="Vendor phone number")
    tax_id: Optional[str] = Field(None, description="Vendor tax ID / VAT number")
    website: Optional[str] = Field(None, description="Vendor website")


class InvoiceData(BaseModel):
    """Extracted invoice data."""
    
    # Invoice identification
    invoice_number: Optional[str] = Field(None, description="Invoice number")
    po_number: Optional[str] = Field(None, description="Purchase order number")
    
    # Vendor information
    vendor: VendorInfo = Field(default_factory=VendorInfo, description="Vendor details")
    
    # Dates
    invoice_date: Optional[date] = Field(None, description="Invoice date")
    due_date: Optional[date] = Field(None, description="Payment due date")
    
    # Financial details
    subtotal: Optional[Decimal] = Field(None, description="Subtotal before tax")
    tax_amount: Optional[Decimal] = Field(None, description="Total tax amount")
    discount_amount: Optional[Decimal] = Field(None, description="Total discount")
    shipping_amount: Optional[Decimal] = Field(None, description="Shipping/freight charges")
    total_amount: Optional[Decimal] = Field(None, description="Total invoice amount")
    currency: str = Field(default="USD", description="Currency code")
    
    # Line items
    line_items: List[LineItem] = Field(default_factory=list, description="Invoice line items")
    
    # Payment information
    payment_terms: Optional[str] = Field(None, description="Payment terms")
    bank_details: Optional[str] = Field(None, description="Bank account details")
    
    # Extraction metadata
    confidence_score: float = Field(default=0.0, ge=0.0, le=1.0, description="Extraction confidence 0-1")
    requires_manual_review: bool = Field(default=False, description="Needs human review")
    raw_text: Optional[str] = Field(None, description="Raw extracted text")
    
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


class ExtractFromUrlRequest(BaseModel):
    """Request to extract invoice from URL (multi-tenant)."""
    
    file_url: str = Field(..., description="Presigned URL to download PDF")
    file_name: str = Field(..., description="Original filename")
    organization_id: int = Field(..., description="Organization ID for multi-tenant context")
    invoice_id: Optional[int] = Field(None, description="Existing invoice ID to update")


class HealthResponse(BaseModel):
    """Health check response."""
    
    status: str = Field(..., description="Service status")
    service: str = Field(..., description="Service name")
    version: str = Field(..., description="Service version")
    database: str = Field(default="unknown", description="Database connection status")
