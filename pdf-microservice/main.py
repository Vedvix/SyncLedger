"""
SyncLedger PDF Extraction Microservice
FastAPI application for extracting invoice data from PDF documents.

Author: vedvix
"""

import os
import tempfile
import time
from contextlib import asynccontextmanager
from typing import List, Optional

import structlog
from fastapi import FastAPI, File, HTTPException, UploadFile, status, Query, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import ORJSONResponse

from models.invoice_data import (
    ExtractionResponse, HealthResponse, InvoiceData,
    SaveInvoiceRequest, SaveInvoiceResponse,
    BatchProcessRequest, BatchProcessResponse,
    ExtractFromUrlRequest
)
from models.mapping_config import (
    InvoiceMappingProfile,
    MappingProfileListResponse,
    MappingProfileCreateRequest,
    MappingProfileUpdateRequest,
    AvailableFieldsResponse,
    MappingSourceField,
    MappingTargetField,
    DateTransform,
    FieldMappingRule,
)
from services.pdf_extractor import PDFExtractor
from services.ocr_service import OCRService
from services.field_parser import FieldParser
from services.mapping_engine import MappingEngine
from services.database_service import DatabaseService, init_db_service
from services.ai_extraction_service import AIExtractionService
from services.ai_models import ExtractionTier

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger(__name__)

# Service instances
pdf_extractor: Optional[PDFExtractor] = None
ocr_service: Optional[OCRService] = None
field_parser: Optional[FieldParser] = None
mapping_engine: Optional[MappingEngine] = None
db_service: Optional[DatabaseService] = None
ai_service: Optional[AIExtractionService] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global pdf_extractor, ocr_service, field_parser, mapping_engine, db_service, ai_service
    
    logger.info("Starting SyncLedger PDF Extraction Service")
    
    # Initialize services
    pdf_extractor = PDFExtractor()
    ocr_service = OCRService()
    field_parser = FieldParser()
    mapping_engine = MappingEngine()
    
    # Initialize AI extraction service
    ai_service = AIExtractionService()
    try:
        await ai_service.initialize()
        logger.info("AI extraction service initialized", ai_enabled=ai_service.is_ai_enabled)
    except Exception as e:
        logger.warning("AI extraction service initialization failed — using regex only", error=str(e))
    
    # Initialize database service
    try:
        db_service = await init_db_service()
        logger.info("Database service initialized")
        
        # Connect mapping engine to DB and load persisted profiles
        mapping_engine.set_db_service(db_service)
        await mapping_engine.load_profiles_from_db()
    except Exception as e:
        logger.warning("Database service initialization failed - running without DB", error=str(e))
        db_service = None
    
    logger.info("Services initialized successfully")
    
    yield
    
    # Cleanup
    if db_service:
        await db_service.close()
    logger.info("Shutting down PDF Extraction Service")


# Create FastAPI application
app = FastAPI(
    title="SyncLedger PDF Extraction Service",
    description="Microservice for extracting invoice data from PDF documents using PyMuPDF and Tesseract OCR",
    version="1.0.0",
    default_response_class=ORJSONResponse,
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "http://localhost:3000,http://localhost:8080").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint."""
    db_status = "disconnected"
    if db_service:
        try:
            # Quick check - just verify we can get a session
            async with db_service.get_session():
                db_status = "connected"
        except Exception:
            db_status = "error"
    
    return HealthResponse(
        status="healthy",
        service="pdf-extraction-service",
        version="1.0.0",
        database=db_status
    )


async def _extract_with_ai_pipeline(
    tmp_path: str,
    extraction_result,
    organization_id: Optional[int] = None,
    profile_id: Optional[str] = None,
) -> ExtractionResponse:
    """
    Core extraction helper that runs the AI pipeline with fallback.
    
    Flow:
    1. Run AI extraction (Vision → Text+LLM → Regex cascade)
    2. If AI succeeded: convert to InvoiceData, then apply mapping engine
    3. If AI failed: fall back to regex + mapping engine (original pipeline)
    4. Return ExtractionResponse with AI metadata
    """
    from models.invoice_data import ExtractionResponse
    
    raw_text = extraction_result.text
    ai_result = None
    ai_response_fields = {}
    
    # ── Run AI Pipeline ───────────────────────────────────────────────
    if ai_service and ai_service.is_ai_enabled:
        try:
            ai_result = await ai_service.extract(
                pdf_path=tmp_path,
                raw_text=raw_text,
                field_parser=field_parser,
            )
            
            ai_response_fields = {
                "ai_tier_used": ai_result.tier_used.value,
                "ai_confidence": ai_result.final_confidence,
                "ai_cost_usd": ai_result.estimated_cost_usd,
                "ai_token_usage": ai_result.token_usage,
            }
            
            if ai_result.cross_validation:
                ai_response_fields["ai_validation"] = {
                    "fields_compared": ai_result.cross_validation.total_fields_compared,
                    "matching": ai_result.cross_validation.matching_fields,
                    "mismatched": ai_result.cross_validation.mismatched_fields,
                    "validation_score": ai_result.cross_validation.validation_score,
                    "review_recommended": ai_result.cross_validation.recommended_review,
                    "notes": ai_result.cross_validation.notes,
                }
            
        except Exception as e:
            logger.error("AI pipeline error, falling back to regex", error=str(e))
            ai_result = None
    
    # ── Build InvoiceData ─────────────────────────────────────────────
    if ai_result and ai_result.ai_extraction and ai_result.tier_used != ExtractionTier.REGEX:
        # AI succeeded — convert to InvoiceData
        invoice_data = ai_service.ai_to_invoice_data(ai_result, raw_text)
        extraction_method = ai_result.tier_used.value
        
        # Still apply mapping engine for GL account, project, etc.
        raw_fields, line_items = field_parser.parse_with_line_items(raw_text)
        # Override raw_fields with AI-extracted values where available
        ai_ext = ai_result.ai_extraction
        if ai_ext.invoice_number:
            raw_fields["invoice_number"] = ai_ext.invoice_number
        if ai_ext.vendor and ai_ext.vendor.name:
            raw_fields["vendor_name"] = ai_ext.vendor.name
        if ai_ext.total_amount is not None:
            raw_fields["total"] = ai_ext.total_amount
        
        mapping_result = mapping_engine.apply_mapping(
            raw_fields=raw_fields,
            line_items=invoice_data.line_items,
            organization_id=organization_id,
            profile_id=profile_id,
        )
        
        # Apply mapped fields (GL, project, etc.) to invoice_data
        if mapping_result.gl_account:
            invoice_data.gl_account = mapping_result.gl_account
        if mapping_result.project:
            invoice_data.project = mapping_result.project
        if mapping_result.location:
            invoice_data.location = mapping_result.location
        if mapping_result.cost_center:
            invoice_data.cost_center = mapping_result.cost_center
        invoice_data.mapping_profile_id = mapping_result.profile_used.id
        
        # Apply GL to line items
        if mapping_result.gl_account:
            for item in invoice_data.line_items:
                if not item.gl_account_code:
                    item.gl_account_code = mapping_result.gl_account
        
    else:
        # Regex fallback — original pipeline
        extraction_method = extraction_result.method
        raw_fields, line_items = field_parser.parse_with_line_items(raw_text)
        mapping_result = mapping_engine.apply_mapping(
            raw_fields=raw_fields,
            line_items=line_items,
            organization_id=organization_id,
            profile_id=profile_id,
        )
        invoice_data = mapping_result.invoice_data
        invoice_data.mapping_profile_id = mapping_result.profile_used.id
    
    return invoice_data, extraction_method, mapping_result, ai_response_fields


@app.post("/extract", response_model=ExtractionResponse, tags=["Extraction"])
async def extract_invoice_data(
    file: UploadFile = File(..., description="PDF file to extract data from"),
    save_to_db: bool = Query(default=False, description="Whether to save extracted data to database")
):
    """
    Extract invoice data from uploaded PDF file.
    
    This endpoint accepts a PDF file and extracts:
    - Invoice number
    - Vendor information (name, address, email, phone)
    - Invoice date and due date
    - Line items (description, quantity, unit price, total)
    - Tax and total amounts
    
    Extraction uses a 3-tier AI cascade:
    1. GPT-4o Vision (best accuracy)
    2. GPT-4o Text+LLM (fallback)
    3. Regex FieldParser (last resort)
    Results are cross-validated for confidence scoring.
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PDF files are accepted"
        )
    
    logger.info("Received PDF for extraction", filename=file.filename)
    
    try:
        # Read file content
        content = await file.read()
        
        if len(content) == 0:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty file uploaded"
            )
        
        if len(content) > 50 * 1024 * 1024:  # 50MB limit
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail="File size exceeds 50MB limit"
            )
        
        # Save to temporary file for processing
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
            tmp_file.write(content)
            tmp_path = tmp_file.name
        
        try:
            # Extract text from PDF
            extraction_result = await pdf_extractor.extract(tmp_path)
            
            # If text extraction failed or returned minimal text, use OCR
            if extraction_result.needs_ocr:
                logger.info("Using OCR for scanned document", filename=file.filename)
                extraction_result = await ocr_service.extract(tmp_path)
            
            # Run AI extraction pipeline with fallback
            invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
                tmp_path=tmp_path,
                extraction_result=extraction_result,
            )
            
            # Optionally save to database
            invoice_id = None
            if save_to_db and db_service:
                try:
                    invoice_id = await db_service.save_invoice(
                        invoice_data=invoice_data,
                        original_filename=file.filename,
                        s3_key=f"uploads/{file.filename}",
                        page_count=extraction_result.page_count,
                        extraction_method=extraction_method,
                        extraction_duration_ms=extraction_result.processing_time_ms
                    )
                    logger.info("Invoice saved to database", invoice_id=invoice_id)
                except Exception as e:
                    logger.error("Failed to save invoice to database", error=str(e))
            
            logger.info(
                "Extraction completed",
                filename=file.filename,
                confidence=invoice_data.confidence_score,
                method=extraction_method,
                mapping_profile=mapping_result.profile_used.name,
                gl_account=mapping_result.gl_account,
                project=mapping_result.project,
                saved_to_db=invoice_id is not None
            )
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_method,
                page_count=extraction_result.page_count,
                processing_time_ms=extraction_result.processing_time_ms,
                invoice_id=invoice_id,
                mapping_info=mapping_result.to_dict(),
                **ai_meta,
            )
            
        finally:
            # Cleanup temporary file
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)
                
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Error extracting invoice data", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing PDF: {str(e)}"
        )


@app.post("/extract/url", response_model=ExtractionResponse, tags=["Extraction"])
async def extract_from_url(url: str):
    """
    Extract invoice data from a PDF at the given URL (e.g., S3 presigned URL).
    Simple endpoint for quick extraction without organization context.
    """
    import httpx
    
    logger.info("Extracting from URL", url=url[:100])
    
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(url, timeout=30.0)
            response.raise_for_status()
            content = response.content
        
        # Save to temporary file
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
            tmp_file.write(content)
            tmp_path = tmp_file.name
        
        try:
            extraction_result = await pdf_extractor.extract(tmp_path)
            
            if extraction_result.needs_ocr:
                extraction_result = await ocr_service.extract(tmp_path)
            
            # Run AI extraction pipeline
            invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
                tmp_path=tmp_path,
                extraction_result=extraction_result,
            )
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_method,
                page_count=extraction_result.page_count,
                processing_time_ms=extraction_result.processing_time_ms,
                mapping_info=mapping_result.to_dict(),
                **ai_meta,
            )
            
        finally:
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)
                
    except httpx.HTTPError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to download PDF from URL: {str(e)}"
        )
    except Exception as e:
        logger.exception("Error extracting from URL", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing PDF: {str(e)}"
        )


@app.post("/api/v1/extract", response_model=ExtractionResponse, tags=["Extraction"])
async def extract_invoice_from_url(request: ExtractFromUrlRequest):
    """
    Extract invoice data from URL with organization context.
    This is the primary endpoint called by the Java backend.
    
    Args:
        request: Contains file_url, file_name, organization_id, and optional invoice_id
    
    Returns:
        ExtractionResponse with extracted invoice data
    """
    import httpx
    
    logger.info(
        "Extracting invoice for organization",
        organization_id=request.organization_id,
        invoice_id=request.invoice_id,
        filename=request.file_name
    )
    
    start_time = time.time()
    
    try:
        # Download PDF from URL
        async with httpx.AsyncClient() as client:
            response = await client.get(request.file_url, timeout=60.0)
            response.raise_for_status()
            content = response.content
        
        # Save to temporary file
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
            tmp_file.write(content)
            tmp_path = tmp_file.name
        
        try:
            # Extract text from PDF
            extraction_result = await pdf_extractor.extract(tmp_path)
            
            # Use OCR if needed
            if extraction_result.needs_ocr:
                logger.info("Using OCR for scanned document", filename=request.file_name)
                extraction_result = await ocr_service.extract(tmp_path)
            
            # Run AI extraction pipeline with organization context
            invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
                tmp_path=tmp_path,
                extraction_result=extraction_result,
                organization_id=request.organization_id,
            )
            
            processing_time = int((time.time() - start_time) * 1000)
            
            logger.info(
                "Extraction completed",
                organization_id=request.organization_id,
                invoice_id=request.invoice_id,
                confidence=invoice_data.confidence_score,
                method=extraction_method,
                ai_tier=ai_meta.get("ai_tier_used"),
                mapping_profile=mapping_result.profile_used.name,
                gl_account=mapping_result.gl_account,
                project=mapping_result.project,
                processing_time_ms=processing_time
            )
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_method,
                page_count=extraction_result.page_count,
                processing_time_ms=processing_time,
                mapping_info=mapping_result.to_dict(),
                **ai_meta,
            )
            
        finally:
            if os.path.exists(tmp_path):
                os.unlink(tmp_path)
                
    except httpx.HTTPError as e:
        logger.error("Failed to download PDF", error=str(e), url=request.file_url[:100])
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to download PDF from URL: {str(e)}"
        )
    except Exception as e:
        logger.exception(
            "Error extracting invoice",
            organization_id=request.organization_id,
            error=str(e)
        )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing PDF: {str(e)}"
        )


@app.post("/invoices/save", response_model=SaveInvoiceResponse, tags=["Database"])
async def save_invoice(request: SaveInvoiceRequest):
    """
    Save extracted invoice data to database.
    
    Use this endpoint when you've already extracted the data and want to save it.
    """
    if not db_service:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database service not available"
        )
    
    try:
        invoice_id = await db_service.save_invoice(
            invoice_data=request.invoice_data,
            original_filename=request.original_filename,
            s3_key=request.s3_key,
            s3_url=request.s3_url,
            file_size=request.file_size,
            page_count=request.page_count,
            extraction_method=request.extraction_method,
            extraction_duration_ms=request.extraction_duration_ms,
            source_email_id=request.source_email_id,
            source_email_from=request.source_email_from,
            source_email_subject=request.source_email_subject
        )
        
        return SaveInvoiceResponse(
            success=True,
            invoice_id=invoice_id,
            invoice_number=request.invoice_data.invoice_number
        )
    except Exception as e:
        logger.exception("Error saving invoice", error=str(e))
        return SaveInvoiceResponse(
            success=False,
            error=str(e)
        )


@app.get("/invoices", tags=["Database"])
async def get_invoices(
    status_filter: Optional[str] = Query(None, alias="status", description="Filter by status"),
    vendor: Optional[str] = Query(None, description="Filter by vendor name"),
    limit: int = Query(default=100, le=500, description="Maximum results"),
    offset: int = Query(default=0, ge=0, description="Offset for pagination")
):
    """
    Get invoices from database with optional filtering.
    """
    if not db_service:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database service not available"
        )
    
    try:
        invoices = await db_service.get_invoices(
            status=status_filter,
            vendor_name=vendor,
            limit=limit,
            offset=offset
        )
        
        return {
            "success": True,
            "count": len(invoices),
            "invoices": [
                {
                    "id": inv.id,
                    "invoice_number": inv.invoice_number,
                    "po_number": inv.po_number,
                    "vendor_name": inv.vendor_name,
                    "total_amount": float(inv.total_amount) if inv.total_amount else None,
                    "invoice_date": inv.invoice_date.isoformat() if inv.invoice_date else None,
                    "status": inv.status,
                    "confidence_score": float(inv.confidence_score) if inv.confidence_score else None,
                    "requires_manual_review": inv.requires_manual_review,
                    "line_items_count": len(inv.line_items),
                    "created_at": inv.created_at.isoformat() if inv.created_at else None
                }
                for inv in invoices
            ]
        }
    except Exception as e:
        logger.exception("Error fetching invoices", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.get("/invoices/{invoice_id}", tags=["Database"])
async def get_invoice(invoice_id: int):
    """
    Get invoice by ID with full details including line items.
    """
    if not db_service:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Database service not available"
        )
    
    try:
        invoice = await db_service.get_invoice(invoice_id)
        
        if not invoice:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Invoice {invoice_id} not found"
            )
        
        return {
            "success": True,
            "invoice": {
                "id": invoice.id,
                "invoice_number": invoice.invoice_number,
                "po_number": invoice.po_number,
                "vendor_name": invoice.vendor_name,
                "vendor_email": invoice.vendor_email,
                "vendor_phone": invoice.vendor_phone,
                "subtotal": float(invoice.subtotal) if invoice.subtotal else None,
                "tax_amount": float(invoice.tax_amount) if invoice.tax_amount else None,
                "total_amount": float(invoice.total_amount) if invoice.total_amount else None,
                "currency": invoice.currency,
                "invoice_date": invoice.invoice_date.isoformat() if invoice.invoice_date else None,
                "due_date": invoice.due_date.isoformat() if invoice.due_date else None,
                "status": invoice.status,
                "confidence_score": float(invoice.confidence_score) if invoice.confidence_score else None,
                "requires_manual_review": invoice.requires_manual_review,
                "original_file_name": invoice.original_file_name,
                "s3_key": invoice.s3_key,
                "extraction_method": invoice.extraction_method,
                "created_at": invoice.created_at.isoformat() if invoice.created_at else None,
                "line_items": [
                    {
                        "line_number": item.line_number,
                        "description": item.description,
                        "quantity": float(item.quantity) if item.quantity else None,
                        "unit_price": float(item.unit_price) if item.unit_price else None,
                        "line_total": float(item.line_total) if item.line_total else None
                    }
                    for item in invoice.line_items
                ]
            }
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("Error fetching invoice", error=str(e))
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )


@app.post("/extract/batch", response_model=BatchProcessResponse, tags=["Extraction"])
async def batch_extract(
    files: List[UploadFile] = File(..., description="PDF files to process"),
    save_to_db: bool = Query(default=True, description="Save to database")
):
    """
    Process multiple PDF files in batch.
    """
    results: List[ExtractionResponse] = []
    successful = 0
    failed = 0
    
    for file in files:
        try:
            if not file.filename.lower().endswith('.pdf'):
                results.append(ExtractionResponse(
                    success=False,
                    data=None,
                    extraction_method="none",
                    page_count=0,
                    processing_time_ms=0,
                    error=f"Not a PDF file: {file.filename}"
                ))
                failed += 1
                continue
            
            content = await file.read()
            
            with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
                tmp_file.write(content)
                tmp_path = tmp_file.name
            
            try:
                start_time = time.time()
                extraction_result = await pdf_extractor.extract(tmp_path)
                
                if extraction_result.needs_ocr:
                    extraction_result = await ocr_service.extract(tmp_path)
                
                # Run AI extraction pipeline
                invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
                    tmp_path=tmp_path,
                    extraction_result=extraction_result,
                )
                
                invoice_id = None
                if save_to_db and db_service:
                    try:
                        invoice_id = await db_service.save_invoice(
                            invoice_data=invoice_data,
                            original_filename=file.filename,
                            s3_key=f"batch/{file.filename}",
                            page_count=extraction_result.page_count,
                            extraction_method=extraction_method,
                            extraction_duration_ms=extraction_result.processing_time_ms
                        )
                    except Exception as e:
                        logger.error("Failed to save invoice", filename=file.filename, error=str(e))
                
                results.append(ExtractionResponse(
                    success=True,
                    data=invoice_data,
                    extraction_method=extraction_method,
                    page_count=extraction_result.page_count,
                    processing_time_ms=extraction_result.processing_time_ms,
                    invoice_id=invoice_id,
                    mapping_info=mapping_result.to_dict(),
                    **ai_meta,
                ))
                successful += 1
                
            finally:
                if os.path.exists(tmp_path):
                    os.unlink(tmp_path)
                    
        except Exception as e:
            logger.exception("Error processing file", filename=file.filename, error=str(e))
            results.append(ExtractionResponse(
                success=False,
                data=None,
                extraction_method="none",
                page_count=0,
                processing_time_ms=0,
                error=str(e)
            ))
            failed += 1
    
    return BatchProcessResponse(
        success=failed == 0,
        total_files=len(files),
        successful=successful,
        failed=failed,
        results=results
    )


@app.post("/extract/folder", tags=["Extraction"])
async def extract_from_folder(
    folder_path: str = Body(..., embed=True, description="Local folder path containing PDFs"),
    save_to_db: bool = Body(default=True, embed=True, description="Save to database")
):
    """
    Extract data from all PDFs in a local folder.
    Useful for processing sample invoices or batch imports.
    """
    import glob
    
    if not os.path.isdir(folder_path):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Folder not found: {folder_path}"
        )
    
    pdf_files = glob.glob(os.path.join(folder_path, "*.pdf"))
    
    if not pdf_files:
        return {
            "success": True,
            "message": "No PDF files found in folder",
            "processed": 0
        }
    
    results = []
    for pdf_path in pdf_files:
        try:
            extraction_result = await pdf_extractor.extract(pdf_path)
            
            if extraction_result.needs_ocr:
                extraction_result = await ocr_service.extract(pdf_path)
            
            # Run AI extraction pipeline
            invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
                tmp_path=pdf_path,
                extraction_result=extraction_result,
            )
            
            invoice_id = None
            if save_to_db and db_service:
                invoice_id = await db_service.save_invoice(
                    invoice_data=invoice_data,
                    original_filename=os.path.basename(pdf_path),
                    s3_key=f"local/{os.path.basename(pdf_path)}",
                    page_count=extraction_result.page_count,
                    extraction_method=extraction_method,
                    extraction_duration_ms=extraction_result.processing_time_ms
                )
            
            results.append({
                "filename": os.path.basename(pdf_path),
                "success": True,
                "invoice_number": invoice_data.invoice_number,
                "vendor": invoice_data.vendor.name,
                "total": float(invoice_data.total_amount) if invoice_data.total_amount else None,
                "confidence": invoice_data.confidence_score,
                "invoice_id": invoice_id,
                "ai_tier": ai_meta.get("ai_tier_used"),
            })
            
        except Exception as e:
            logger.error("Error processing file", path=pdf_path, error=str(e))
            results.append({
                "filename": os.path.basename(pdf_path),
                "success": False,
                "error": str(e)
            })
    
    return {
        "success": True,
        "folder": folder_path,
        "total_files": len(pdf_files),
        "processed": len([r for r in results if r.get("success")]),
        "results": results
    }


# ─── Mapping Profile Management API ────────────────────────────────────────

@app.get("/api/v1/mapping/profiles", tags=["Mapping"])
async def list_mapping_profiles(
    organization_id: Optional[int] = Query(None, description="Filter by organization")
):
    """
    List all available mapping profiles.
    
    Returns built-in and custom mapping profiles for the given organization context.
    """
    profiles = mapping_engine.list_profiles(organization_id=organization_id)
    return {
        "success": True,
        "profiles": [p.dict() for p in profiles],
        "total": len(profiles)
    }


@app.get("/api/v1/mapping/profiles/{profile_id}", tags=["Mapping"])
async def get_mapping_profile(profile_id: str):
    """Get a specific mapping profile by ID."""
    profile = mapping_engine.get_profile(profile_id)
    if not profile:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Mapping profile '{profile_id}' not found"
        )
    return {"success": True, "profile": profile.dict()}


@app.post("/api/v1/mapping/profiles", tags=["Mapping"])
async def create_mapping_profile(request: MappingProfileCreateRequest):
    """
    Create a new mapping profile.
    
    Custom mapping profiles allow organizations to define how extracted invoice
    fields map to the target system model for different vendor invoice formats.
    Profiles are persisted in the database and scoped per organization.
    """
    import uuid
    
    profile = InvoiceMappingProfile(
        id=f"custom-{uuid.uuid4().hex[:8]}",
        name=request.name,
        description=request.description,
        vendor_pattern=request.vendor_pattern,
        is_default=request.is_default,
        organization_id=request.organization_id,
        rules=request.rules,
    )
    
    mapping_engine.register_profile(profile)
    
    # Persist to database
    await mapping_engine.save_profile_to_db(profile)
    
    logger.info("Mapping profile created", profile_id=profile.id, name=profile.name)
    return {"success": True, "profile": profile.dict()}


@app.put("/api/v1/mapping/profiles/{profile_id}", tags=["Mapping"])
async def update_mapping_profile(profile_id: str, request: MappingProfileUpdateRequest):
    """Update an existing mapping profile."""
    existing = mapping_engine.get_profile(profile_id)
    if not existing:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Mapping profile '{profile_id}' not found"
        )
    
    # Apply updates
    if request.name is not None:
        existing.name = request.name
    if request.description is not None:
        existing.description = request.description
    if request.vendor_pattern is not None:
        existing.vendor_pattern = request.vendor_pattern
    if request.is_default is not None:
        existing.is_default = request.is_default
    if request.rules is not None:
        existing.rules = request.rules
    
    mapping_engine.register_profile(existing)
    
    # Persist to database
    await mapping_engine.save_profile_to_db(existing)
    
    logger.info("Mapping profile updated", profile_id=profile_id)
    return {"success": True, "profile": existing.dict()}


@app.delete("/api/v1/mapping/profiles/{profile_id}", tags=["Mapping"])
async def delete_mapping_profile(profile_id: str):
    """Delete a mapping profile."""
    # Prevent deletion of built-in profiles
    if profile_id.startswith("default-") or profile_id.startswith("standard-"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot delete built-in mapping profiles"
        )
    
    if not mapping_engine.remove_profile(profile_id):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Mapping profile '{profile_id}' not found"
        )
    
    # Remove from database
    await mapping_engine.delete_profile_from_db(profile_id)
    
    return {"success": True, "message": f"Profile '{profile_id}' deleted"}


@app.get("/api/v1/mapping/fields", tags=["Mapping"])
async def get_available_fields():
    """
    Get all available source fields, target fields, and date transforms.
    
    This endpoint helps the frontend build the mapping profile configuration UI.
    """
    source_fields = [
        {"value": f.value, "label": f.value.replace("_", " ").title()}
        for f in MappingSourceField
    ]
    target_fields = [
        {"value": f.value, "label": f.value.replace("_", " ").title()}
        for f in MappingTargetField
    ]
    date_transforms = [
        {"value": t.value, "label": t.value.replace("_", " ").title()}
        for t in DateTransform
    ]
    
    return {
        "success": True,
        "source_fields": source_fields,
        "target_fields": target_fields,
        "date_transforms": date_transforms,
    }


@app.post("/api/v1/mapping/preview", tags=["Mapping"])
async def preview_mapping(
    file: UploadFile = File(..., description="PDF file to preview mapping for"),
    profile_id: Optional[str] = Query(None, description="Mapping profile to use"),
    organization_id: Optional[int] = Query(None, description="Organization context"),
):
    """
    Preview the result of applying a mapping profile to a PDF.
    
    Extracts fields from the PDF and shows both the raw extracted data and
    the mapped result, without saving to the database.
    """
    if not file.filename.lower().endswith('.pdf'):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only PDF files are accepted"
        )
    
    content = await file.read()
    
    with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
        tmp_file.write(content)
        tmp_path = tmp_file.name
    
    try:
        extraction_result = await pdf_extractor.extract(tmp_path)
        
        if extraction_result.needs_ocr:
            extraction_result = await ocr_service.extract(tmp_path)
        
        # Always get regex fields for raw preview
        raw_fields, line_items = field_parser.parse_with_line_items(extraction_result.text)
        
        # Run AI pipeline for the mapped result
        invoice_data, extraction_method, mapping_result, ai_meta = await _extract_with_ai_pipeline(
            tmp_path=tmp_path,
            extraction_result=extraction_result,
            profile_id=profile_id,
            organization_id=organization_id,
        )
        
        # Build serializable raw_fields (remove raw_text for preview)
        preview_raw = {
            k: (str(v) if v is not None else None)
            for k, v in raw_fields.items()
            if k != "raw_text"
        }
        
        return {
            "success": True,
            "raw_extracted_fields": preview_raw,
            "mapped_result": {
                "invoice_data": invoice_data.dict(),
                **mapping_result.to_dict(),
            },
            "line_items": [item.dict() for item in line_items],
            "extraction_method": extraction_method,
            "page_count": extraction_result.page_count,
            "ai_tier_used": ai_meta.get("ai_tier_used"),
            "ai_confidence": ai_meta.get("ai_confidence"),
        }
    finally:
        if os.path.exists(tmp_path):
            os.unlink(tmp_path)


# ─── AI Stats & Management ─────────────────────────────────────────────────

@app.get("/ai/stats", tags=["AI"])
async def get_ai_stats():
    """
    Get AI extraction service statistics.
    
    Returns usage stats, cost tracking, and configuration status.
    """
    if not ai_service:
        return {
            "enabled": False,
            "message": "AI extraction service not initialized (missing OPENAI_API_KEY)"
        }
    
    stats = ai_service.stats
    return {
        "enabled": ai_service.is_ai_enabled,
        "vision_enabled": ai_service.enable_vision,
        "text_llm_enabled": ai_service.enable_text_llm,
        "validation_enabled": ai_service.enable_cross_validation,
        "stats": stats,
    }


@app.post("/ai/reset-stats", tags=["AI"])
async def reset_ai_stats():
    """Reset AI extraction statistics counters."""
    if not ai_service:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="AI extraction service not available"
        )
    
    ai_service._total_extractions = 0
    ai_service._total_cost = 0.0
    return {"success": True, "message": "AI stats reset"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8001)),
        reload=os.getenv("ENV", "development") == "development"
    )
