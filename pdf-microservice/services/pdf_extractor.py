"""
PDF text extraction service using pdfplumber.

Author: vedvix
"""

import time
from typing import Optional

import pdfplumber
import structlog

from models.invoice_data import ExtractionResult

logger = structlog.get_logger(__name__)


class PDFExtractor:
    """
    PDF text extraction using pdfplumber.
    
    This is the primary extraction method for digital PDFs.
    Falls back to OCR for scanned documents.
    """
    
    # Minimum text length to consider extraction successful
    MIN_TEXT_LENGTH = 100
    
    # Minimum ratio of text to pages to consider it a digital PDF
    MIN_TEXT_PER_PAGE = 50
    
    async def extract(self, pdf_path: str) -> ExtractionResult:
        """
        Extract text from a PDF file.
        
        Args:
            pdf_path: Path to the PDF file
            
        Returns:
            ExtractionResult with extracted text and metadata
        """
        start_time = time.time()
        
        try:
            with pdfplumber.open(pdf_path) as pdf:
                page_count = len(pdf.pages)
                
                logger.info("Extracting text from PDF", pages=page_count)
                
                # Extract text from all pages
                all_text = []
                for page in pdf.pages:
                    text = page.extract_text() or ""
                    all_text.append(text)
            
            full_text = "\n\n".join(all_text)
            text_length = len(full_text.strip())
            
            # Determine if OCR is needed
            needs_ocr = self._needs_ocr(text_length, page_count)
            
            processing_time = int((time.time() - start_time) * 1000)
            
            logger.info(
                "PDF extraction completed",
                text_length=text_length,
                pages=page_count,
                needs_ocr=needs_ocr,
                time_ms=processing_time
            )
            
            return ExtractionResult(
                text=full_text,
                method="pdfplumber",
                page_count=page_count,
                needs_ocr=needs_ocr,
                processing_time_ms=processing_time
            )
            
        except Exception as e:
            logger.exception("Error extracting PDF text", error=str(e))
            raise
    
    def _needs_ocr(self, text_length: int, page_count: int) -> bool:
        """
        Determine if OCR is needed based on extracted text.
        
        A scanned PDF will have very little or no extractable text.
        """
        if text_length < self.MIN_TEXT_LENGTH:
            return True
        
        # Check text per page ratio
        text_per_page = text_length / max(page_count, 1)
        if text_per_page < self.MIN_TEXT_PER_PAGE:
            return True
        
        return False
    
    async def get_page_count(self, pdf_path: str) -> int:
        """Get the number of pages in a PDF."""
        try:
            doc = fitz.open(pdf_path)
            count = len(doc)
            doc.close()
            return count
        except Exception as e:
            logger.exception("Error getting page count", error=str(e))
            return 0
    
    async def extract_page(self, pdf_path: str, page_num: int) -> Optional[str]:
        """Extract text from a specific page."""
        try:
            doc = fitz.open(pdf_path)
            if page_num < 0 or page_num >= len(doc):
                doc.close()
                return None
            
            page = doc[page_num]
            text = page.get_text("text")
            doc.close()
            return text
        except Exception as e:
            logger.exception("Error extracting page", error=str(e), page=page_num)
            return None
