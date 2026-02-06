"""
SyncLedger PDF Extraction Microservice
FastAPI application for extracting invoice data from PDF documents.

Author: vedvix
"""

import os
import tempfile
from contextlib import asynccontextmanager
from typing import Optional

import structlog
from fastapi import FastAPI, File, HTTPException, UploadFile, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import ORJSONResponse

from models.invoice_data import ExtractionResponse, HealthResponse
from services.pdf_extractor import PDFExtractor
from services.ocr_service import OCRService
from services.field_parser import FieldParser

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


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager."""
    global pdf_extractor, ocr_service, field_parser
    
    logger.info("Starting SyncLedger PDF Extraction Service")
    
    # Initialize services
    pdf_extractor = PDFExtractor()
    ocr_service = OCRService()
    field_parser = FieldParser()
    
    logger.info("Services initialized successfully")
    
    yield
    
    # Cleanup
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
    return HealthResponse(
        status="healthy",
        service="pdf-extraction-service",
        version="1.0.0"
    )


@app.post("/extract", response_model=ExtractionResponse, tags=["Extraction"])
async def extract_invoice_data(
    file: UploadFile = File(..., description="PDF file to extract data from")
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
            
            logger.info(
                "Extraction completed",
                filename=file.filename,
                confidence=invoice_data.confidence_score,
                method=extraction_result.method
            )
            
            return ExtractionResponse(
                success=True,
                data=invoice_data,
                extraction_method=extraction_result.method,
                page_count=extraction_result.page_count,
                processing_time_ms=extraction_result.processing_time_ms
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


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", 8001)),
        reload=os.getenv("ENV", "development") == "development"
    )
