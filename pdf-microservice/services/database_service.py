"""
Database service for PostgreSQL operations.

Author: vedvix
"""

import os
from contextlib import asynccontextmanager
from datetime import datetime
from decimal import Decimal
from typing import AsyncGenerator, List, Optional

import structlog
from sqlalchemy import select, update, delete
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.orm import selectinload

from models.db_models import Base, Invoice, InvoiceLineItem, EmailLog
from models.invoice_data import InvoiceData, LineItem

logger = structlog.get_logger(__name__)


class DatabaseService:
    """
    Async database service for PostgreSQL using SQLAlchemy.
    
    Handles all database operations for invoices and related entities.
    """
    
    def __init__(self, database_url: Optional[str] = None):
        """
        Initialize database service.
        
        Args:
            database_url: PostgreSQL connection URL. If not provided, uses env var.
        """
        self.database_url = database_url or os.getenv(
            "DATABASE_URL",
            "postgresql+asyncpg://syncledger:syncledger123@localhost:5432/syncledger"
        )
        
        # Convert postgres:// to postgresql+asyncpg:// if needed
        if self.database_url.startswith("postgres://"):
            self.database_url = self.database_url.replace("postgres://", "postgresql+asyncpg://", 1)
        elif self.database_url.startswith("postgresql://") and "+asyncpg" not in self.database_url:
            self.database_url = self.database_url.replace("postgresql://", "postgresql+asyncpg://", 1)
        
        self.engine = create_async_engine(
            self.database_url,
            echo=os.getenv("DB_ECHO", "false").lower() == "true",
            pool_size=5,
            max_overflow=10,
            pool_pre_ping=True
        )
        
        self.async_session = async_sessionmaker(
            self.engine,
            class_=AsyncSession,
            expire_on_commit=False
        )
        
        logger.info("Database service initialized", url=self.database_url.split("@")[-1])
    
    async def init_db(self):
        """Initialize database tables (for development only)."""
        async with self.engine.begin() as conn:
            # Note: In production, use Flyway migrations instead
            await conn.run_sync(Base.metadata.create_all)
        logger.info("Database tables initialized")
    
    @asynccontextmanager
    async def get_session(self) -> AsyncGenerator[AsyncSession, None]:
        """Get an async database session."""
        async with self.async_session() as session:
            try:
                yield session
                await session.commit()
            except Exception:
                await session.rollback()
                raise
    
    async def save_invoice(
        self,
        invoice_data: InvoiceData,
        original_filename: str,
        s3_key: str,
        s3_url: Optional[str] = None,
        file_size: Optional[int] = None,
        page_count: Optional[int] = None,
        extraction_method: str = "pymupdf",
        extraction_duration_ms: Optional[int] = None,
        source_email_id: Optional[str] = None,
        source_email_from: Optional[str] = None,
        source_email_subject: Optional[str] = None
    ) -> int:
        """
        Save extracted invoice data to database.
        
        Args:
            invoice_data: Parsed invoice data from PDF
            original_filename: Original PDF filename
            s3_key: S3 storage key
            s3_url: S3 presigned URL (optional)
            file_size: File size in bytes
            page_count: Number of pages in PDF
            extraction_method: Method used for extraction
            extraction_duration_ms: Extraction processing time
            source_email_id: Microsoft Graph email ID (optional)
            source_email_from: Email sender address
            source_email_subject: Email subject
            
        Returns:
            Created invoice ID
        """
        async with self.get_session() as session:
            # Create invoice record
            invoice = Invoice(
                invoice_number=invoice_data.invoice_number or f"INV-{datetime.now().strftime('%Y%m%d%H%M%S')}",
                po_number=invoice_data.po_number,
                vendor_name=invoice_data.vendor.name or "Unknown Vendor",
                vendor_address=invoice_data.vendor.address,
                vendor_email=invoice_data.vendor.email,
                vendor_phone=invoice_data.vendor.phone,
                vendor_tax_id=invoice_data.vendor.tax_id,
                subtotal=invoice_data.subtotal or invoice_data.total_amount or Decimal("0"),
                tax_amount=invoice_data.tax_amount or Decimal("0"),
                discount_amount=invoice_data.discount_amount or Decimal("0"),
                shipping_amount=invoice_data.shipping_amount or Decimal("0"),
                total_amount=invoice_data.total_amount or Decimal("0"),
                currency=invoice_data.currency,
                invoice_date=invoice_data.invoice_date or datetime.now().date(),
                due_date=invoice_data.due_date,
                received_date=datetime.now().date(),
                status="PENDING",
                confidence_score=Decimal(str(invoice_data.confidence_score)),
                requires_manual_review=invoice_data.requires_manual_review,
                original_file_name=original_filename,
                s3_key=s3_key,
                s3_url=s3_url,
                file_size_bytes=file_size,
                mime_type="application/pdf",
                page_count=page_count,
                source_email_id=source_email_id,
                source_email_from=source_email_from,
                source_email_subject=source_email_subject,
                extraction_method=extraction_method,
                extracted_at=datetime.now(),
                extraction_duration_ms=extraction_duration_ms,
                raw_extracted_data=invoice_data.raw_text,
                # Mapped fields
                gl_account=invoice_data.gl_account,
                project=invoice_data.project,
                item_category=invoice_data.item_category,
                location=invoice_data.location,
                cost_center=invoice_data.cost_center,
                mapping_profile_id=invoice_data.mapping_profile_id,
            )
            
            session.add(invoice)
            await session.flush()  # Get the ID
            
            # Add line items
            for item in invoice_data.line_items:
                line_item = InvoiceLineItem(
                    invoice_id=invoice.id,
                    line_number=item.line_number,
                    description=item.description,
                    item_code=item.item_code,
                    unit=item.unit,
                    quantity=item.quantity,
                    unit_price=item.unit_price,
                    tax_rate=item.tax_rate,
                    tax_amount=item.tax_amount,
                    discount_amount=item.discount_amount,
                    line_total=item.line_total or Decimal("0"),
                    gl_account_code=item.gl_account_code,
                    cost_center=item.cost_center,
                )
                session.add(line_item)
            
            await session.commit()
            
            logger.info(
                "Invoice saved to database",
                invoice_id=invoice.id,
                invoice_number=invoice.invoice_number,
                vendor=invoice.vendor_name,
                total=str(invoice.total_amount),
                line_items=len(invoice_data.line_items)
            )
            
            return invoice.id
    
    async def get_invoice(self, invoice_id: int) -> Optional[Invoice]:
        """Get invoice by ID with line items."""
        async with self.get_session() as session:
            result = await session.execute(
                select(Invoice)
                .options(selectinload(Invoice.line_items))
                .where(Invoice.id == invoice_id)
            )
            return result.scalar_one_or_none()
    
    async def get_invoice_by_number(self, invoice_number: str) -> Optional[Invoice]:
        """Get invoice by invoice number."""
        async with self.get_session() as session:
            result = await session.execute(
                select(Invoice)
                .options(selectinload(Invoice.line_items))
                .where(Invoice.invoice_number == invoice_number)
            )
            return result.scalar_one_or_none()
    
    async def get_invoices(
        self,
        status: Optional[str] = None,
        vendor_name: Optional[str] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[Invoice]:
        """Get invoices with optional filtering."""
        async with self.get_session() as session:
            query = select(Invoice).options(selectinload(Invoice.line_items))
            
            if status:
                query = query.where(Invoice.status == status)
            if vendor_name:
                query = query.where(Invoice.vendor_name.ilike(f"%{vendor_name}%"))
            
            query = query.order_by(Invoice.created_at.desc()).limit(limit).offset(offset)
            
            result = await session.execute(query)
            return list(result.scalars().all())
    
    async def update_invoice_status(self, invoice_id: int, status: str, notes: Optional[str] = None) -> bool:
        """Update invoice status."""
        async with self.get_session() as session:
            result = await session.execute(
                update(Invoice)
                .where(Invoice.id == invoice_id)
                .values(status=status, review_notes=notes, updated_at=datetime.now())
            )
            return result.rowcount > 0
    
    async def check_duplicate(self, invoice_number: str, vendor_name: str) -> bool:
        """Check if invoice already exists (duplicate detection)."""
        async with self.get_session() as session:
            result = await session.execute(
                select(Invoice.id)
                .where(Invoice.invoice_number == invoice_number)
                .where(Invoice.vendor_name.ilike(f"%{vendor_name}%"))
            )
            return result.scalar_one_or_none() is not None
    
    async def save_email_log(
        self,
        message_id: str,
        from_address: str,
        subject: str,
        received_at: datetime,
        has_attachments: bool = False,
        attachment_count: int = 0,
        attachment_names: Optional[str] = None
    ) -> int:
        """Save email processing log."""
        async with self.get_session() as session:
            email_log = EmailLog(
                message_id=message_id,
                from_address=from_address,
                subject=subject,
                received_at=received_at,
                has_attachments=has_attachments,
                attachment_count=attachment_count,
                attachment_names=attachment_names
            )
            session.add(email_log)
            await session.flush()
            return email_log.id
    
    async def update_email_log(
        self,
        message_id: str,
        is_processed: bool = True,
        invoices_extracted: int = 0,
        has_error: bool = False,
        error_message: Optional[str] = None
    ):
        """Update email log after processing."""
        async with self.get_session() as session:
            await session.execute(
                update(EmailLog)
                .where(EmailLog.message_id == message_id)
                .values(
                    is_processed=is_processed,
                    processed_at=datetime.now(),
                    invoices_extracted=invoices_extracted,
                    has_error=has_error,
                    error_message=error_message
                )
            )
    
    async def is_email_processed(self, message_id: str) -> bool:
        """Check if email has already been processed."""
        async with self.get_session() as session:
            result = await session.execute(
                select(EmailLog.is_processed)
                .where(EmailLog.message_id == message_id)
            )
            row = result.scalar_one_or_none()
            return row is True
    
    async def close(self):
        """Close database connections."""
        await self.engine.dispose()
        logger.info("Database connections closed")


# Singleton instance
_db_service: Optional[DatabaseService] = None


def get_db_service() -> DatabaseService:
    """Get or create database service singleton."""
    global _db_service
    if _db_service is None:
        _db_service = DatabaseService()
    return _db_service


async def init_db_service() -> DatabaseService:
    """Initialize and return database service."""
    global _db_service
    _db_service = DatabaseService()
    return _db_service
