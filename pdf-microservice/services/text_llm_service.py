"""
Text + LLM extraction service using GPT-4o.

Takes raw text extracted from PDF (via pdfplumber or OCR) and sends it 
to GPT-4o for structured invoice data extraction.

Tier 1 (fallback from Vision) in the extraction pipeline.

Author: vedvix
"""

import json
import os
import time
from typing import Optional, Tuple

import structlog

from services.ai_models import (
    AIInvoiceExtraction,
    INVOICE_JSON_SCHEMA,
    TEXT_SYSTEM_PROMPT,
)

logger = structlog.get_logger(__name__)

# GPT-4o text pricing (per token)
INPUT_COST_PER_1K = 0.0025   # $2.50 per 1M input tokens
OUTPUT_COST_PER_1K = 0.01    # $10.00 per 1M output tokens


class TextLLMExtractionService:
    """
    Extracts invoice data by sending raw text to GPT-4o.
    
    This is the Tier 1 fallback when Vision extraction fails.
    It works with text extracted by pdfplumber or Tesseract OCR.
    Lower cost than Vision (no image tokens) but slightly less accurate
    for complex layouts.
    """
    
    # Maximum text length to send (GPT-4o supports 128K context)
    MAX_TEXT_LENGTH = 32000  # Keep it reasonable for cost control
    
    def __init__(self, client=None):
        """
        Initialize with an OpenAI client.
        
        Args:
            client: OpenAI async client instance (injected by AIExtractionService)
        """
        self.client = client
        self.model = os.getenv("OPENAI_TEXT_MODEL", "gpt-4o")
        logger.info("TextLLMExtractionService initialized", model=self.model)
    
    def set_client(self, client):
        """Set the OpenAI client (allows lazy initialization)."""
        self.client = client
    
    async def extract(self, raw_text: str) -> Tuple[Optional[AIInvoiceExtraction], dict]:
        """
        Extract invoice data from raw text using GPT-4o.
        
        Args:
            raw_text: Raw text extracted from PDF (via pdfplumber or OCR)
            
        Returns:
            Tuple of (AIInvoiceExtraction or None, metadata dict with tokens/cost)
        """
        start_time = time.time()
        metadata = {
            "tokens_input": 0,
            "tokens_output": 0,
            "estimated_cost": 0.0,
            "text_length": len(raw_text),
            "model": self.model,
        }
        
        if not self.client:
            raise RuntimeError("OpenAI client not initialized")
        
        try:
            # Truncate text if needed
            text_to_send = raw_text
            if len(text_to_send) > self.MAX_TEXT_LENGTH:
                text_to_send = text_to_send[:self.MAX_TEXT_LENGTH]
                logger.warning(
                    "Truncated text for LLM",
                    original_length=len(raw_text),
                    truncated_to=self.MAX_TEXT_LENGTH,
                )
            
            # Build the prompt
            schema_text = json.dumps(INVOICE_JSON_SCHEMA, indent=2)
            user_message = (
                f"Extract all invoice data from the following text into this exact JSON schema:\n\n"
                f"{schema_text}\n\n"
                f"--- INVOICE TEXT START ---\n"
                f"{text_to_send}\n"
                f"--- INVOICE TEXT END ---\n\n"
                f"Return ONLY the JSON object. No markdown, no code fences."
            )
            
            # Call GPT-4o
            logger.info("Calling GPT-4o Text API", text_length=len(text_to_send))
            
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": TEXT_SYSTEM_PROMPT},
                    {"role": "user", "content": user_message},
                ],
                max_tokens=4096,
                temperature=0.1,
                response_format={"type": "json_object"},
            )
            
            # Parse response
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
                "GPT-4o Text response received",
                input_tokens=metadata["tokens_input"],
                output_tokens=metadata["tokens_output"],
                cost_usd=f"${metadata['estimated_cost']:.4f}",
                time_ms=processing_time,
            )
            
            # Parse JSON response
            extraction = self._parse_response(raw_response)
            
            return extraction, metadata
            
        except Exception as e:
            logger.exception("Text LLM extraction failed", error=str(e))
            metadata["error"] = str(e)
            return None, metadata
    
    def _parse_response(self, raw_json: str) -> Optional[AIInvoiceExtraction]:
        """Parse GPT-4o response into AIInvoiceExtraction model."""
        try:
            # Clean response
            cleaned = raw_json.strip()
            if cleaned.startswith("```"):
                cleaned = cleaned.split("\n", 1)[1]
                if cleaned.endswith("```"):
                    cleaned = cleaned[:-3]
                cleaned = cleaned.strip()
            
            data = json.loads(cleaned)
            extraction = AIInvoiceExtraction(**data)
            
            logger.info(
                "Text LLM extraction parsed",
                invoice_number=extraction.invoice_number,
                vendor=extraction.vendor.name,
                total=extraction.total_amount,
                line_items=len(extraction.line_items),
                ai_confidence=extraction.ai_confidence,
            )
            
            return extraction
            
        except json.JSONDecodeError as e:
            logger.error("Failed to parse text LLM response as JSON", error=str(e), raw=raw_json[:500])
            return None
        except Exception as e:
            logger.error("Failed to create AIInvoiceExtraction from text response", error=str(e))
            return None
    
    def _estimate_cost(self, input_tokens: int, output_tokens: int) -> float:
        """Estimate the API cost in USD."""
        input_cost = (input_tokens / 1000) * INPUT_COST_PER_1K
        output_cost = (output_tokens / 1000) * OUTPUT_COST_PER_1K
        return round(input_cost + output_cost, 6)
