"""
Database models for PostgreSQL using SQLAlchemy.

Author: vedvix
"""

from datetime import date, datetime
from decimal import Decimal
from enum import Enum
from typing import List, Optional

from sqlalchemy import (
    BigInteger, Boolean, Column, Date, DateTime, 
    ForeignKey, Index, Integer, Numeric, String, Text,
    create_engine, Enum as SQLEnum
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship
from sqlalchemy.sql import func


class Base(DeclarativeBase):
    """Base class for all models."""
    pass


class InvoiceStatus(str, Enum):
    """Invoice status enum matching Java backend."""
    PENDING = "PENDING"
    UNDER_REVIEW = "UNDER_REVIEW"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    SYNCED = "SYNCED"
    SYNC_FAILED = "SYNC_FAILED"
    ARCHIVED = "ARCHIVED"


class SyncStatus(str, Enum):
    """Sage sync status enum."""
    PENDING = "PENDING"
    IN_PROGRESS = "IN_PROGRESS"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    RETRYING = "RETRYING"


class Invoice(Base):
    """Invoice database model matching Java entity."""
    
    __tablename__ = "invoices"
    
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    
    # Invoice Identification
    invoice_number: Mapped[str] = mapped_column(String(100), nullable=False)
    po_number: Mapped[Optional[str]] = mapped_column(String(100))
    
    # Vendor Information
    vendor_name: Mapped[str] = mapped_column(String(255), nullable=False)
    vendor_address: Mapped[Optional[str]] = mapped_column(String(255))
    vendor_email: Mapped[Optional[str]] = mapped_column(String(100))
    vendor_phone: Mapped[Optional[str]] = mapped_column(String(50))
    vendor_tax_id: Mapped[Optional[str]] = mapped_column(String(50))
    
    # Financial Details
    subtotal: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False)
    tax_amount: Mapped[Decimal] = mapped_column(Numeric(15, 2), default=Decimal("0"))
    discount_amount: Mapped[Decimal] = mapped_column(Numeric(15, 2), default=Decimal("0"))
    shipping_amount: Mapped[Decimal] = mapped_column(Numeric(15, 2), default=Decimal("0"))
    total_amount: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False)
    currency: Mapped[str] = mapped_column(String(3), default="USD")
    
    # Dates
    invoice_date: Mapped[date] = mapped_column(Date, nullable=False)
    due_date: Mapped[Optional[date]] = mapped_column(Date)
    received_date: Mapped[Optional[date]] = mapped_column(Date)
    
    # Status & Processing
    status: Mapped[str] = mapped_column(String(20), default="PENDING")
    confidence_score: Mapped[Optional[Decimal]] = mapped_column(Numeric(5, 2))
    requires_manual_review: Mapped[bool] = mapped_column(Boolean, default=False)
    review_notes: Mapped[Optional[str]] = mapped_column(String(500))
    
    # File Storage
    original_file_name: Mapped[str] = mapped_column(String(500), nullable=False)
    s3_key: Mapped[str] = mapped_column(String(500), nullable=False)
    s3_url: Mapped[Optional[str]] = mapped_column(String(500))
    file_size_bytes: Mapped[Optional[int]] = mapped_column(BigInteger)
    mime_type: Mapped[Optional[str]] = mapped_column(String(50))
    page_count: Mapped[Optional[int]] = mapped_column(Integer)
    
    # Email Source
    source_email_id: Mapped[Optional[str]] = mapped_column(String(500))
    source_email_from: Mapped[Optional[str]] = mapped_column(String(255))
    source_email_subject: Mapped[Optional[str]] = mapped_column(String(500))
    source_email_received_at: Mapped[Optional[datetime]] = mapped_column(DateTime)
    
    # Extraction Metadata
    extraction_method: Mapped[Optional[str]] = mapped_column(String(50))
    extracted_at: Mapped[Optional[datetime]] = mapped_column(DateTime)
    extraction_duration_ms: Mapped[Optional[int]] = mapped_column(Integer)
    raw_extracted_data: Mapped[Optional[str]] = mapped_column(Text)
    
    # Sage Integration
    sage_invoice_id: Mapped[Optional[str]] = mapped_column(String(100))
    sage_vendor_id: Mapped[Optional[str]] = mapped_column(String(100))
    sync_status: Mapped[Optional[str]] = mapped_column(String(20))
    last_sync_attempt: Mapped[Optional[datetime]] = mapped_column(DateTime)
    sync_attempt_count: Mapped[int] = mapped_column(Integer, default=0)
    sync_error_message: Mapped[Optional[str]] = mapped_column(String(500))
    
    # Mapping Fields
    gl_account: Mapped[Optional[str]] = mapped_column(String(100))
    project: Mapped[Optional[str]] = mapped_column(String(255))
    item_category: Mapped[Optional[str]] = mapped_column(String(255))
    location: Mapped[Optional[str]] = mapped_column(String(500))
    cost_center: Mapped[Optional[str]] = mapped_column(String(100))
    mapping_profile_id: Mapped[Optional[str]] = mapped_column(String(100))
    
    # Audit Fields
    assigned_to_id: Mapped[Optional[int]] = mapped_column(BigInteger, ForeignKey("users.id"))
    processed_by_id: Mapped[Optional[int]] = mapped_column(BigInteger, ForeignKey("users.id"))
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now(), onupdate=func.now())
    
    # Relationships
    line_items: Mapped[List["InvoiceLineItem"]] = relationship(
        "InvoiceLineItem", 
        back_populates="invoice",
        cascade="all, delete-orphan"
    )
    
    __table_args__ = (
        Index("idx_invoice_number", "invoice_number"),
        Index("idx_invoice_status", "status"),
        Index("idx_invoice_vendor", "vendor_name"),
        Index("idx_invoice_date", "invoice_date"),
        Index("idx_invoice_due_date", "due_date"),
    )


class InvoiceLineItem(Base):
    """Invoice line item database model."""
    
    __tablename__ = "invoice_line_items"
    
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    invoice_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("invoices.id", ondelete="CASCADE"), nullable=False)
    
    line_number: Mapped[int] = mapped_column(Integer, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(String(500))
    item_code: Mapped[Optional[str]] = mapped_column(String(100))
    unit: Mapped[Optional[str]] = mapped_column(String(50))
    quantity: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 4))
    unit_price: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 4))
    tax_rate: Mapped[Optional[Decimal]] = mapped_column(Numeric(5, 2))
    tax_amount: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 2))
    discount_amount: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 2))
    line_total: Mapped[Decimal] = mapped_column(Numeric(15, 2), nullable=False)
    gl_account_code: Mapped[Optional[str]] = mapped_column(String(100))
    cost_center: Mapped[Optional[str]] = mapped_column(String(100))
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    
    # Relationship
    invoice: Mapped["Invoice"] = relationship("Invoice", back_populates="line_items")
    
    __table_args__ = (
        Index("idx_line_item_invoice", "invoice_id"),
    )


class User(Base):
    """User model for reference (minimal, managed by Java backend)."""
    
    __tablename__ = "users"
    
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    first_name: Mapped[str] = mapped_column(String(100), nullable=False)
    last_name: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    role: Mapped[str] = mapped_column(String(20), nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)


class EmailLog(Base):
    """Email processing log model."""
    
    __tablename__ = "email_logs"
    
    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    message_id: Mapped[str] = mapped_column(String(500), nullable=False, unique=True)
    internet_message_id: Mapped[Optional[str]] = mapped_column(String(500))
    from_address: Mapped[Optional[str]] = mapped_column(String(255))
    from_name: Mapped[Optional[str]] = mapped_column(String(255))
    to_addresses: Mapped[Optional[str]] = mapped_column(String(1000))
    subject: Mapped[Optional[str]] = mapped_column(String(500))
    body_preview: Mapped[Optional[str]] = mapped_column(Text)
    received_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    has_attachments: Mapped[bool] = mapped_column(Boolean, default=False)
    attachment_count: Mapped[Optional[int]] = mapped_column(Integer)
    attachment_names: Mapped[Optional[str]] = mapped_column(String(1000))
    is_processed: Mapped[bool] = mapped_column(Boolean, default=False)
    processed_at: Mapped[Optional[datetime]] = mapped_column(DateTime)
    invoices_extracted: Mapped[Optional[int]] = mapped_column(Integer)
    has_error: Mapped[bool] = mapped_column(Boolean, default=False)
    error_message: Mapped[Optional[str]] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
