"""
OCR service using Tesseract for scanned PDF documents.

Author: vedvix
"""

import os
import tempfile
import time
from typing import List

import structlog
from pdf2image import convert_from_path
from PIL import Image
import pytesseract

from models.invoice_data import ExtractionResult

logger = structlog.get_logger(__name__)


class OCRService:
    """
    OCR extraction service using Tesseract.
    
    Used for scanned PDFs where direct text extraction fails.
    """
    
    # Tesseract configuration
    TESSERACT_CONFIG = '--oem 3 --psm 6'
    
    # DPI for PDF to image conversion (higher = better quality, slower)
    PDF_DPI = 300
    
    def __init__(self):
        """Initialize OCR service."""
        # Check if Tesseract is available
        try:
            pytesseract.get_tesseract_version()
            logger.info("Tesseract OCR initialized")
        except Exception as e:
            logger.warning("Tesseract not found, OCR may not work", error=str(e))
    
    async def extract(self, pdf_path: str) -> ExtractionResult:
        """
        Extract text from a scanned PDF using OCR.
        
        Args:
            pdf_path: Path to the PDF file
            
        Returns:
            ExtractionResult with extracted text and metadata
        """
        start_time = time.time()
        
        try:
            logger.info("Starting OCR extraction", pdf_path=pdf_path)
            
            # Convert PDF to images
            images = self._convert_pdf_to_images(pdf_path)
            page_count = len(images)
            
            logger.info("PDF converted to images", pages=page_count)
            
            # Extract text from each image
            all_text = []
            for i, image in enumerate(images):
                logger.debug("Processing page with OCR", page=i + 1)
                text = self._extract_text_from_image(image)
                all_text.append(text)
            
            full_text = "\n\n".join(all_text)
            processing_time = int((time.time() - start_time) * 1000)
            
            logger.info(
                "OCR extraction completed",
                text_length=len(full_text),
                pages=page_count,
                time_ms=processing_time
            )
            
            return ExtractionResult(
                text=full_text,
                method="tesseract_ocr",
                page_count=page_count,
                needs_ocr=False,  # OCR was performed
                processing_time_ms=processing_time
            )
            
        except Exception as e:
            logger.exception("Error during OCR extraction", error=str(e))
            raise
    
    def _convert_pdf_to_images(self, pdf_path: str) -> List[Image.Image]:
        """
        Convert PDF pages to PIL images.
        
        Args:
            pdf_path: Path to the PDF file
            
        Returns:
            List of PIL Image objects
        """
        try:
            images = convert_from_path(
                pdf_path,
                dpi=self.PDF_DPI,
                fmt='png'
            )
            return images
        except Exception as e:
            logger.exception("Error converting PDF to images", error=str(e))
            raise
    
    def _extract_text_from_image(self, image: Image.Image) -> str:
        """
        Extract text from a single image using Tesseract.
        
        Args:
            image: PIL Image object
            
        Returns:
            Extracted text string
        """
        try:
            # Preprocess image for better OCR results
            processed_image = self._preprocess_image(image)
            
            # Run Tesseract OCR
            text = pytesseract.image_to_string(
                processed_image,
                config=self.TESSERACT_CONFIG
            )
            
            return text.strip()
        except Exception as e:
            logger.exception("Error extracting text from image", error=str(e))
            return ""
    
    def _preprocess_image(self, image: Image.Image) -> Image.Image:
        """
        Preprocess image to improve OCR accuracy.
        
        Args:
            image: Original PIL Image
            
        Returns:
            Preprocessed PIL Image
        """
        # Convert to grayscale
        if image.mode != 'L':
            image = image.convert('L')
        
        # Simple threshold to improve contrast
        # For production, consider more advanced preprocessing
        
        return image
    
    async def extract_from_image(self, image_path: str) -> str:
        """
        Extract text from a single image file.
        
        Args:
            image_path: Path to the image file
            
        Returns:
            Extracted text string
        """
        try:
            image = Image.open(image_path)
            return self._extract_text_from_image(image)
        except Exception as e:
            logger.exception("Error processing image", error=str(e))
            return ""
