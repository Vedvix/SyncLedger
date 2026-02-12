"""
Configurable field mapping models for invoice extraction.

Supports dynamic mapping between extracted PDF fields and target system fields.
Different invoice formats can have different mapping configurations.

Author: vedvix
"""

from datetime import date, timedelta
from decimal import Decimal
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class MappingSourceField(str, Enum):
    """Available source fields from PDF extraction."""
    
    # Identification
    PO_NUMBER = "po_number"
    INVOICE_NUMBER = "invoice_number"
    
    # Dates
    ORDER_DATE = "order_date"
    INVOICE_DATE = "invoice_date"
    DUE_DATE = "due_date"
    APPROVED_DATE = "approved_date"
    
    # Financial
    TOTAL = "total"
    SUBTOTAL = "subtotal"
    TAX_AMOUNT = "tax_amount"
    
    # Vendor
    VENDOR_NAME = "vendor_name"
    VENDOR_ADDRESS = "vendor_address"
    VENDOR_EMAIL = "vendor_email"
    VENDOR_PHONE = "vendor_phone"
    
    # Project / PO metadata
    PROJECT_NUMBER = "project_number"
    SALE_NUMBER = "sale_number"
    OPPORTUNITY_NUMBER = "opportunity_number"
    MARKET_SEGMENT = "market_segment"
    PRODUCT_CATEGORY = "product_category"
    CREATED_BY = "created_by"
    
    # Line item fields
    LINE_DESCRIPTION = "line_description"
    LINE_QUANTITY = "line_quantity"
    LINE_UNIT_PRICE = "line_unit_price"
    LINE_TOTAL = "line_total"


class MappingTargetField(str, Enum):
    """Target fields in the system's invoice model."""
    
    INVOICE_NUMBER = "invoice_number"
    PO_NUMBER = "po_number"
    TOTAL_AMOUNT = "total_amount"
    SUBTOTAL = "subtotal"
    TAX_AMOUNT = "tax_amount"
    INVOICE_DATE = "invoice_date"
    DUE_DATE = "due_date"
    VENDOR_NAME = "vendor_name"
    VENDOR_ADDRESS = "vendor_address"
    VENDOR_EMAIL = "vendor_email"
    VENDOR_PHONE = "vendor_phone"
    GL_ACCOUNT = "gl_account"
    PROJECT = "project"
    ITEM = "item"
    LOCATION = "location"
    COST_CENTER = "cost_center"


class DateTransform(str, Enum):
    """Date transformation rules."""
    
    NONE = "none"
    NEXT_FRIDAY = "next_friday"
    NEXT_MONDAY = "next_monday"
    ADD_30_DAYS = "add_30_days"
    ADD_60_DAYS = "add_60_days"
    ADD_90_DAYS = "add_90_days"
    END_OF_MONTH = "end_of_month"


class FieldMappingRule(BaseModel):
    """A single field mapping rule from source to target."""
    
    target_field: MappingTargetField = Field(
        ..., description="Target field in the system model"
    )
    source_field: Optional[MappingSourceField] = Field(
        None, description="Source field from extracted data. None if using default_value."
    )
    default_value: Optional[str] = Field(
        None, description="Default value if source field is empty or not mapped"
    )
    fallback_source: Optional[MappingSourceField] = Field(
        None, description="Fallback source field if primary source is empty"
    )
    date_transform: Optional[DateTransform] = Field(
        None, description="Date transformation to apply (only for date fields)"
    )
    date_transform_source: Optional[MappingSourceField] = Field(
        None, description="Source date field for the transform (e.g., order_date for next_friday)"
    )
    is_required: bool = Field(
        default=False, description="Whether this field is required for a valid invoice"
    )
    description: Optional[str] = Field(
        None, description="Human-readable description of this mapping rule"
    )

    class Config:
        use_enum_values = True


class InvoiceMappingProfile(BaseModel):
    """
    A complete mapping profile for a specific invoice format.
    
    Each profile defines how extracted PDF fields map to the target system model.
    Organizations can have multiple profiles for different vendor invoice formats.
    """
    
    id: Optional[str] = Field(None, description="Unique profile identifier")
    name: str = Field(..., description="Profile name (e.g., 'MGD Construction PO')")
    description: Optional[str] = Field(None, description="Profile description")
    vendor_pattern: Optional[str] = Field(
        None, 
        description="Regex pattern to auto-match this profile to a vendor name"
    )
    is_default: bool = Field(
        default=False, description="Whether this is the default profile"
    )
    organization_id: Optional[int] = Field(
        None, description="Organization this profile belongs to (None = global)"
    )
    rules: List[FieldMappingRule] = Field(
        default_factory=list, description="List of field mapping rules"
    )
    
    class Config:
        use_enum_values = True


# ─── Default Mapping Profiles ───────────────────────────────────────────────

def get_default_subcontractor_profile() -> InvoiceMappingProfile:
    """
    Default mapping profile for subcontractor invoices.
    
    Mapping:
    - Invoice No → Purchase Order number from invoice
    - Total Amount → Total from invoice
    - Payment Due Date → Next Friday from the Order Date
    - GL Account → 5100 (subcontractor default)
    - Project → Opportunity Number from invoice
    - Item → Product Category from invoice
    - Location → Address present in invoice
    """
    return InvoiceMappingProfile(
        id="default-subcontractor",
        name="Subcontractor Invoice (Default)",
        description="Default mapping for subcontractor invoices. "
                    "Maps PO as invoice number, calculates next Friday for due date, "
                    "uses GL 5100, maps opportunity number to project.",
        vendor_pattern=r"(?i)MGD|Master\s+Gutters|Mayan.?s\s+Construction",
        is_default=True,
        rules=[
            FieldMappingRule(
                target_field=MappingTargetField.INVOICE_NUMBER,
                source_field=MappingSourceField.PO_NUMBER,
                fallback_source=MappingSourceField.INVOICE_NUMBER,
                is_required=True,
                description="Invoice No → Purchase Order number from the invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.TOTAL_AMOUNT,
                source_field=MappingSourceField.TOTAL,
                is_required=True,
                description="Total Amount → Total from the invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.DUE_DATE,
                source_field=MappingSourceField.DUE_DATE,
                date_transform=DateTransform.NEXT_FRIDAY,
                date_transform_source=MappingSourceField.ORDER_DATE,
                description="Payment Due Date → Next Friday from the Order Date"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.GL_ACCOUNT,
                source_field=None,
                default_value="5100",
                description="GL Account → Default 5100 for subcontractor"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.PROJECT,
                source_field=MappingSourceField.OPPORTUNITY_NUMBER,
                fallback_source=MappingSourceField.PROJECT_NUMBER,
                description="Project → Opportunity Number from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.ITEM,
                source_field=MappingSourceField.PRODUCT_CATEGORY,
                fallback_source=MappingSourceField.MARKET_SEGMENT,
                description="Item → Product Category from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.LOCATION,
                source_field=MappingSourceField.VENDOR_ADDRESS,
                description="Location → Address present in the invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.SUBTOTAL,
                source_field=MappingSourceField.SUBTOTAL,
                description="Subtotal from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.TAX_AMOUNT,
                source_field=MappingSourceField.TAX_AMOUNT,
                default_value="0",
                description="Tax amount from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.INVOICE_DATE,
                source_field=MappingSourceField.ORDER_DATE,
                fallback_source=MappingSourceField.INVOICE_DATE,
                description="Invoice date from order date"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_NAME,
                source_field=MappingSourceField.VENDOR_NAME,
                is_required=True,
                description="Vendor name"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_ADDRESS,
                source_field=MappingSourceField.VENDOR_ADDRESS,
                description="Vendor address"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_EMAIL,
                source_field=MappingSourceField.VENDOR_EMAIL,
                description="Vendor email"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_PHONE,
                source_field=MappingSourceField.VENDOR_PHONE,
                description="Vendor phone"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.PO_NUMBER,
                source_field=MappingSourceField.PO_NUMBER,
                description="Original PO number preserved"
            ),
        ]
    )


def get_standard_invoice_profile() -> InvoiceMappingProfile:
    """
    Standard mapping profile for typical vendor invoices.
    
    Uses direct field mapping without transformations.
    """
    return InvoiceMappingProfile(
        id="standard-invoice",
        name="Standard Invoice",
        description="Standard mapping for typical vendor invoices with direct field mapping.",
        is_default=False,
        rules=[
            FieldMappingRule(
                target_field=MappingTargetField.INVOICE_NUMBER,
                source_field=MappingSourceField.INVOICE_NUMBER,
                fallback_source=MappingSourceField.PO_NUMBER,
                is_required=True,
                description="Invoice number directly from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.PO_NUMBER,
                source_field=MappingSourceField.PO_NUMBER,
                description="PO number from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.TOTAL_AMOUNT,
                source_field=MappingSourceField.TOTAL,
                is_required=True,
                description="Total amount from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.SUBTOTAL,
                source_field=MappingSourceField.SUBTOTAL,
                description="Subtotal from invoice"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.TAX_AMOUNT,
                source_field=MappingSourceField.TAX_AMOUNT,
                default_value="0",
                description="Tax amount"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.DUE_DATE,
                source_field=MappingSourceField.DUE_DATE,
                date_transform=DateTransform.ADD_30_DAYS,
                date_transform_source=MappingSourceField.INVOICE_DATE,
                description="Due date, defaults to Net 30 from invoice date"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.INVOICE_DATE,
                source_field=MappingSourceField.INVOICE_DATE,
                fallback_source=MappingSourceField.ORDER_DATE,
                description="Invoice date"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_NAME,
                source_field=MappingSourceField.VENDOR_NAME,
                is_required=True,
                description="Vendor name"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_ADDRESS,
                source_field=MappingSourceField.VENDOR_ADDRESS,
                description="Vendor address"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_EMAIL,
                source_field=MappingSourceField.VENDOR_EMAIL,
                description="Vendor email"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.VENDOR_PHONE,
                source_field=MappingSourceField.VENDOR_PHONE,
                description="Vendor phone"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.GL_ACCOUNT,
                source_field=None,
                default_value="5000",
                description="GL Account default for standard vendors"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.PROJECT,
                source_field=MappingSourceField.PROJECT_NUMBER,
                fallback_source=MappingSourceField.OPPORTUNITY_NUMBER,
                description="Project number"
            ),
            FieldMappingRule(
                target_field=MappingTargetField.LOCATION,
                source_field=MappingSourceField.VENDOR_ADDRESS,
                description="Location from vendor address"
            ),
        ]
    )


# ─── API Request/Response Models ────────────────────────────────────────────

class MappingProfileListResponse(BaseModel):
    """Response listing available mapping profiles."""
    
    profiles: List[InvoiceMappingProfile]
    total: int


class MappingProfileCreateRequest(BaseModel):
    """Request to create a new mapping profile."""
    
    name: str = Field(..., description="Profile name")
    description: Optional[str] = None
    vendor_pattern: Optional[str] = None
    is_default: bool = False
    organization_id: Optional[int] = None
    rules: List[FieldMappingRule] = Field(
        default_factory=list, description="Field mapping rules"
    )

    class Config:
        use_enum_values = True


class MappingProfileUpdateRequest(BaseModel):
    """Request to update an existing mapping profile."""
    
    name: Optional[str] = None
    description: Optional[str] = None
    vendor_pattern: Optional[str] = None
    is_default: Optional[bool] = None
    rules: Optional[List[FieldMappingRule]] = None

    class Config:
        use_enum_values = True


class AvailableFieldsResponse(BaseModel):
    """Response listing all available source and target fields."""
    
    source_fields: List[Dict[str, str]]
    target_fields: List[Dict[str, str]]
    date_transforms: List[Dict[str, str]]
