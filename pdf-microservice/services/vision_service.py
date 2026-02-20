"""
Vision-based extraction service using GPT-4o Vision API.

Converts PDF pages to images and sends them to GPT-4o Vision
for structured invoice data extraction.

Tier 2 (highest accuracy) in the extraction pipeline.

Author: vedvix
"""

import base64
import io
import json
import os
import time
from typing import List, Optional, Tuple

import structlog
from PIL import Image
from pdf2image import convert_from_path

from services.ai_models import (
    AIInvoiceExtraction,
    INVOICE_JSON_SCHEMA,
    VISION_SYSTEM_PROMPT,
)

logger = structlog.get_logger(__name__)

# GPT-4o Vision pricing (per token)
INPUT_COST_PER_1K = 0.0025   # $2.50 per 1M input tokens
OUTPUT_COST_PER_1K = 0.01    # $10.00 per 1M output tokens

# Image token estimation for GPT-4o Vision
# A 1024x1024 image ≈ 765 tokens in "high" detail mode
# Invoice at 300 DPI letter size (2550x3300) ≈ 1700-2000 tokens per page


class VisionExtractionService:
    """
    Extracts invoice data by sending PDF page images to GPT-4o Vision.
    
    This is the highest accuracy extraction method, capable of understanding
    any invoice layout, table structure, handwritten text, and multi-language content.
    """
    
    # Image conversion settings
    PDF_DPI = 200          # Balance between quality and token cost
    MAX_IMAGE_WIDTH = 2048  # GPT-4o Vision max useful width
    MAX_IMAGE_HEIGHT = 2048
    MAX_PAGES = 5           # Max pages to send (most invoices are 1-3 pages)
    JPEG_QUALITY = 85       # JPEG quality for base64 encoding
    
    def __init__(self, client=None):
        """
        Initialize with an OpenAI client.
        
        Args:
            client: OpenAI async client instance (injected by AIExtractionService)
        """
        self.client = client
        self.model = os.getenv("OPENAI_VISION_MODEL", "gpt-4o")
        logger.info("VisionExtractionService initialized", model=self.model)
    
    def set_client(self, client):
        """Set the OpenAI client (allows lazy initialization)."""
        self.client = client
    
    async def extract(self, pdf_path: str) -> Tuple[Optional[AIInvoiceExtraction], dict]:
        """
        Extract invoice data from PDF using GPT-4o Vision.
        
        Args:
            pdf_path: Path to the PDF file
            
        Returns:
            Tuple of (AIInvoiceExtraction or None, metadata dict with tokens/cost)
        """
        start_time = time.time()
        metadata = {
            "tokens_input": 0,
            "tokens_output": 0,
            "estimated_cost": 0.0,
            "pages_sent": 0,
            "model": self.model,
        }
        
        if not self.client:
            raise RuntimeError("OpenAI client not initialized")
        
        try:
            # Step 1: Convert PDF pages to images
            images = self._convert_pdf_to_images(pdf_path)
            pages_to_send = min(len(images), self.MAX_PAGES)
            metadata["pages_sent"] = pages_to_send
            
            logger.info(
                "Converting PDF for vision extraction",
                total_pages=len(images),
                pages_to_send=pages_to_send,
            )
            
            # Step 2: Encode images as base64
            image_contents = []
            for i in range(pages_to_send):
                base64_image = self._encode_image(images[i])
                image_contents.append({
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{base64_image}",
                        "detail": "high",  # High detail for invoice text
                    }
                })
            
            # Step 3: Build the message with images + schema prompt
            schema_text = json.dumps(INVOICE_JSON_SCHEMA, indent=2)
            user_content = [
                {
                    "type": "text",
                    "text": (
                        f"Extract all invoice data from the following {pages_to_send} page(s) "
                        f"into this exact JSON schema:\n\n{schema_text}\n\n"
                        "Return ONLY the JSON object. No markdown, no code fences."
                    ),
                },
                *image_contents,
            ]
            
            # Step 4: Call GPT-4o Vision
            logger.info("Calling GPT-4o Vision API", pages=pages_to_send)
            
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": VISION_SYSTEM_PROMPT},
                    {"role": "user", "content": user_content},
                ],
                max_tokens=4096,
                temperature=0.1,  # Low temperature for accurate extraction
                response_format={"type": "json_object"},
            )
            
            # Step 5: Parse response
            raw_response = response.choices[0].message.content
            usage = response.usage
            
            metadata["tokens_input"] = usage.prompt_tokens if usage else 0
            metadata["tokens_output"] = usage.completion_tokens if usage else 0
            metadata["estimated_cost"] = self._estimate_cost(
                metadata["tokens_input"], metadata["tokens_output"]
            )
            
            processing_time = int((time.time() - start_time) * 1000)
            metadata["processing_time_ms"] = processing_time
            
            logger.info(
                "GPT-4o Vision response received",
                input_tokens=metadata["tokens_input"],
                output_tokens=metadata["tokens_output"],
                cost_usd=f"${metadata['estimated_cost']:.4f}",
                time_ms=processing_time,
            )
            
            # Step 6: Parse JSON into our model
            extraction = self._parse_response(raw_response)
            
            return extraction, metadata
            
        except Exception as e:
            logger.exception("Vision extraction failed", error=str(e))
            metadata["error"] = str(e)
            return None, metadata
    
    def _convert_pdf_to_images(self, pdf_path: str) -> List[Image.Image]:
        """Convert PDF pages to PIL images optimized for vision API."""
        try:
            images = convert_from_path(
                pdf_path,
                dpi=self.PDF_DPI,
                fmt='jpeg',
            )
            
            # Resize if needed
            resized = []
            for img in images:
                if img.width > self.MAX_IMAGE_WIDTH or img.height > self.MAX_IMAGE_HEIGHT:
                    img.thumbnail(
                        (self.MAX_IMAGE_WIDTH, self.MAX_IMAGE_HEIGHT),
                        Image.Resampling.LANCZOS
                    )
                resized.append(img)
            
            return resized
            
        except Exception as e:
            logger.exception("Failed to convert PDF to images", error=str(e))
            raise
    
    def _encode_image(self, image: Image.Image) -> str:
        """Encode a PIL image as base64 JPEG string."""
        buffer = io.BytesIO()
        
        # Convert to RGB if necessary (RGBA, CMYK etc.)
        if image.mode not in ('RGB', 'L'):
            image = image.convert('RGB')
        
        image.save(buffer, format='JPEG', quality=self.JPEG_QUALITY)
        buffer.seek(0)
        return base64.b64encode(buffer.read()).decode('utf-8')
    
    def _parse_response(self, raw_json: str) -> Optional[AIInvoiceExtraction]:
        """Parse GPT-4o response into AIInvoiceExtraction model."""
        try:
            # Clean response - remove markdown fences if present
            cleaned = raw_json.strip()
            if cleaned.startswith("```"):
                cleaned = cleaned.split("\n", 1)[1]  # Remove first line
                if cleaned.endswith("```"):
                    cleaned = cleaned[:-3]
                cleaned = cleaned.strip()
            
            data = json.loads(cleaned)
            
            # Parse into our model
            extraction = AIInvoiceExtraction(**data)
            
            logger.info(
                "Vision extraction parsed",
                invoice_number=extraction.invoice_number,
                vendor=extraction.vendor.name,
                total=extraction.total_amount,
                line_items=len(extraction.line_items),
                ai_confidence=extraction.ai_confidence,
            )
            
            return extraction
            
        except json.JSONDecodeError as e:
            logger.error("Failed to parse vision response as JSON", error=str(e), raw=raw_json[:500])
            return None
        except Exception as e:
            logger.error("Failed to create AIInvoiceExtraction from response", error=str(e))
            return None
    
    def _estimate_cost(self, input_tokens: int, output_tokens: int) -> float:
        """Estimate the API cost in USD."""
        input_cost = (input_tokens / 1000) * INPUT_COST_PER_1K
        output_cost = (output_tokens / 1000) * OUTPUT_COST_PER_1K
        return round(input_cost + output_cost, 6)
