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
    BatchProcessRequest, BatchProcessResponse
)
from services.pdf_extractor import PDFExtractor
from services.ocr_service import OCRService
from services.field_parser import FieldParser
from services.database_service import DatabaseService, init_db_service

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
db_service: Optional[DatabaseService] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global pdf_extractor, ocr_service, field_parser, db_service
    
    logger.info("Starting SyncLedger PDF Extraction Service")
    
    # Initialize services
    pdf_extractor = PDFExtractor()
    ocr_service = OCRService()
    field_parser = FieldParser()
    
    # Initialize database service
    try:
        db_service = await init_db_service()
        logger.info("Database service initialized")
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
    
    The extraction uses PyMuPDF for text extraction with Tesseract OCR fallback
    for scanned documents.
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
            
            # Parse extracted text into structured data
            invoice_data = await field_parser.parse(extraction_result.text)
            
            # Optionally save to database
            invoice_id = None
            if save_to_db and db_service:
                try:
                    invoice_id = await db_service.save_invoice(
                        invoice_data=invoice_data,
                        original_filename=file.filename,
                        s3_key=f"uploads/{file.filename}",  # Temporary key
                        page_count=extraction_result.page_count,
                        extraction_method=extraction_result.method,
                        extraction_duration_ms=extraction_result.processing_time_ms
                    )
                    logger.info("Invoice saved to database", invoice_id=invoice_id)
                except Exception as e:
                    logger.error("Failed to save invoice to database", error=str(e))
            
            logger.info(
                "Extraction completed",
                filename=file.filename,
                confidence=invoice_data.confidence_score,
                method=extraction_result.method,
                saved_to_db=invoice_id is not None
            )
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_result.method,
                page_count=extraction_result.page_count,
                processing_time_ms=extraction_result.processing_time_ms,
                invoice_id=invoice_id
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
            
            invoice_data = await field_parser.parse(extraction_result.text)
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_result.method,
                page_count=extraction_result.page_count,
                processing_time_ms=extraction_result.processing_time_ms
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
                
                invoice_data = await field_parser.parse(extraction_result.text)
                
                invoice_id = None
                if save_to_db and db_service:
                    try:
                        invoice_id = await db_service.save_invoice(
                            invoice_data=invoice_data,
                            original_filename=file.filename,
                            s3_key=f"batch/{file.filename}",
                            page_count=extraction_result.page_count,
                            extraction_method=extraction_result.method,
                            extraction_duration_ms=extraction_result.processing_time_ms
                        )
                    except Exception as e:
                        logger.error("Failed to save invoice", filename=file.filename, error=str(e))
                
                results.append(ExtractionResponse(
                    success=True,
                    data=invoice_data,
                    extraction_method=extraction_result.method,
                    page_count=extraction_result.page_count,
                    processing_time_ms=extraction_result.processing_time_ms,
                    invoice_id=invoice_id
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
            
            invoice_data = await field_parser.parse(extraction_result.text)
            
            invoice_id = None
            if save_to_db and db_service:
                invoice_id = await db_service.save_invoice(
                    invoice_data=invoice_data,
                    original_filename=os.path.basename(pdf_path),
                    s3_key=f"local/{os.path.basename(pdf_path)}",
                    page_count=extraction_result.page_count,
                    extraction_method=extraction_result.method,
                    extraction_duration_ms=extraction_result.processing_time_ms
                )
            
            results.append({
                "filename": os.path.basename(pdf_path),
                "success": True,
                "invoice_number": invoice_data.invoice_number,
                "vendor": invoice_data.vendor.name,
                "total": float(invoice_data.total_amount) if invoice_data.total_amount else None,
                "confidence": invoice_data.confidence_score,
                "invoice_id": invoice_id
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


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8001)),
        reload=os.getenv("ENV", "development") == "development"
    )
