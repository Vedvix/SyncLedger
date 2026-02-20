"""
AIExtractionService — Unified AI-powered invoice extraction pipeline.

Orchestrates a 3-tier extraction cascade:
  Tier 2 (Vision):   PDF → Image → GPT-4o Vision → Structured JSON  (best accuracy)
  Tier 1 (Text+LLM): Text → GPT-4o → Structured JSON                (fallback)
  Tier 0 (Regex):    Text → FieldParser regex → Structured JSON      (last resort)

All tiers are cross-validated against the regex parser for confidence scoring.

Architecture:
  ┌─────────────────────────────────────────────────────────────────────┐
  │                    AIExtractionService                              │
  │                                                                     │
  │  PDF Input ──► VisionService (GPT-4o Vision)                       │
  │                    │                                                │
  │                    ├─ Success ──► Cross-validate vs Regex ──► Done  │
  │                    │                                                │
  │                    └─ Fail ──► TextLLMService (GPT-4o Text)        │
  │                                    │                               │
  │                                    ├─ Success ──► Cross-validate   │
  │                                    │                               │
  │                                    └─ Fail ──► FieldParser (Regex) │
  │                                                    │               │
  │                                                    └──► Done       │
  └─────────────────────────────────────────────────────────────────────┘

Author: vedvix
"""

import os
import time
from datetime import date
from decimal import Decimal, InvalidOperation
from typing import Any, Dict, List, Optional, Tuple

import structlog

from models.invoice_data import InvoiceData, LineItem, VendorInfo
from services.ai_models import (
    AIExtractionResult,
    AIInvoiceExtraction,
    CrossValidationResult,
    ExtractionTier,
)
from services.field_parser import FieldParser
from services.text_llm_service import TextLLMExtractionService
from services.validation_service import CrossValidationService
from services.vision_service import VisionExtractionService

logger = structlog.get_logger(__name__)


class AIExtractionService:
    """
    Unified AI extraction service that orchestrates Vision, Text+LLM, and Regex extraction.
    
    Usage:
        ai_service = AIExtractionService()
        await ai_service.initialize()  # Initializes OpenAI client
        
        result = await ai_service.extract(pdf_path, raw_text)
        # result.tier_used tells you which method succeeded
        # result.final_confidence is the cross-validated confidence
        # result.ai_extraction has the structured data
    """
    
    def __init__(self):
        """Initialize sub-services (OpenAI client is lazy-initialized)."""
        self._openai_client = None
        self._initialized = False
        
        self.vision_service = VisionExtractionService()
        self.text_llm_service = TextLLMExtractionService()
        self.validation_service = CrossValidationService()
        
        # Configuration
        self.enable_vision = os.getenv("AI_ENABLE_VISION", "true").lower() == "true"
        self.enable_text_llm = os.getenv("AI_ENABLE_TEXT_LLM", "true").lower() == "true"
        self.enable_cross_validation = os.getenv("AI_ENABLE_VALIDATION", "true").lower() == "true"
        
        # Cost tracking
        self._total_cost = 0.0
        self._total_extractions = 0
        
        logger.info(
            "AIExtractionService created",
            vision_enabled=self.enable_vision,
            text_llm_enabled=self.enable_text_llm,
            cross_validation=self.enable_cross_validation,
        )
    
    async def initialize(self):
        """
        Initialize OpenAI client. Must be called before extract().
        
        Reads OPENAI_API_KEY from environment.
        """
        if self._initialized:
            return
        
        api_key = os.getenv("OPENAI_API_KEY")
        if not api_key:
            logger.warning(
                "OPENAI_API_KEY not set — AI extraction disabled, falling back to regex only"
            )
            self.enable_vision = False
            self.enable_text_llm = False
            self._initialized = True
            return
        
        try:
            from openai import AsyncOpenAI
            
            self._openai_client = AsyncOpenAI(
                api_key=api_key,
                timeout=120.0,  # 2 minute timeout for vision
                max_retries=2,
            )
            
            # Inject client into sub-services
            self.vision_service.set_client(self._openai_client)
            self.text_llm_service.set_client(self._openai_client)
            
            self._initialized = True
            logger.info("OpenAI client initialized for AI extraction")
            
        except ImportError:
            logger.error("openai package not installed — AI extraction disabled")
            self.enable_vision = False
            self.enable_text_llm = False
            self._initialized = True
        except Exception as e:
            logger.error("Failed to initialize OpenAI client", error=str(e))
            self.enable_vision = False
            self.enable_text_llm = False
            self._initialized = True
    
    async def extract(
        self,
        pdf_path: str,
        raw_text: str,
        field_parser: FieldParser,
    ) -> AIExtractionResult:
        """
        Extract invoice data using the 3-tier cascade.
        
        Args:
            pdf_path: Path to the PDF file (for Vision extraction)
            raw_text: Pre-extracted text from pdfplumber/OCR (for Text+LLM and Regex)
            field_parser: FieldParser instance for regex extraction and cross-validation
            
        Returns:
            AIExtractionResult with the best extraction and metadata
        """
        if not self._initialized:
            await self.initialize()
        
        start_time = time.time()
        result = AIExtractionResult()
        
        # Always run regex extraction (for fallback + cross-validation)
        regex_fields, regex_line_items = field_parser.parse_with_line_items(raw_text)
        result.regex_extraction = {
            k: str(v) if v is not None else None 
            for k, v in regex_fields.items() 
            if k != "raw_text"
        }
        
        # ── Tier 2: Vision Extraction ──────────────────────────────────
        if self.enable_vision:
            logger.info("Attempting Tier 2: GPT-4o Vision extraction")
            try:
                ai_extraction, vision_meta = await self.vision_service.extract(pdf_path)
                
                if ai_extraction and self._is_valid_extraction(ai_extraction):
                    result.ai_extraction = ai_extraction
                    result.tier_used = ExtractionTier.VISION
                    result.token_usage = {
                        "input": vision_meta.get("tokens_input", 0),
                        "output": vision_meta.get("tokens_output", 0),
                    }
                    result.estimated_cost_usd = vision_meta.get("estimated_cost", 0.0)
                    
                    # Cross-validate against regex
                    if self.enable_cross_validation:
                        result.cross_validation = self.validation_service.validate(
                            ai_extraction, regex_fields, regex_line_items
                        )
                        result.final_confidence = result.cross_validation.final_confidence
                        result.tier_used = ExtractionTier.VISION_WITH_VALIDATION
                    else:
                        result.final_confidence = ai_extraction.ai_confidence or 0.8
                    
                    logger.info(
                        "Tier 2 Vision extraction succeeded",
                        confidence=result.final_confidence,
                        cost=f"${result.estimated_cost_usd:.4f}",
                    )
                else:
                    result.fallback_reason = vision_meta.get("error", "Vision returned invalid/empty extraction")
                    logger.warning("Tier 2 Vision extraction returned invalid result, falling back")
                    
            except Exception as e:
                result.fallback_reason = f"Vision error: {str(e)}"
                logger.warning("Tier 2 Vision extraction failed", error=str(e))
        
        # ── Tier 1: Text + LLM Extraction (fallback) ──────────────────
        if result.ai_extraction is None and self.enable_text_llm and raw_text.strip():
            logger.info("Attempting Tier 1: GPT-4o Text extraction (fallback)")
            try:
                ai_extraction, text_meta = await self.text_llm_service.extract(raw_text)
                
                if ai_extraction and self._is_valid_extraction(ai_extraction):
                    result.ai_extraction = ai_extraction
                    result.tier_used = ExtractionTier.TEXT_LLM
                    result.token_usage = {
                        "input": text_meta.get("tokens_input", 0),
                        "output": text_meta.get("tokens_output", 0),
                    }
                    result.estimated_cost_usd = text_meta.get("estimated_cost", 0.0)
                    
                    # Cross-validate against regex
                    if self.enable_cross_validation:
                        result.cross_validation = self.validation_service.validate(
                            ai_extraction, regex_fields, regex_line_items
                        )
                        result.final_confidence = result.cross_validation.final_confidence
                        result.tier_used = ExtractionTier.TEXT_LLM_WITH_VALIDATION
                    else:
                        result.final_confidence = ai_extraction.ai_confidence or 0.7
                    
                    logger.info(
                        "Tier 1 Text+LLM extraction succeeded",
                        confidence=result.final_confidence,
                        cost=f"${result.estimated_cost_usd:.4f}",
                    )
                else:
                    reason = text_meta.get("error", "Text LLM returned invalid/empty extraction")
                    result.fallback_reason = (result.fallback_reason or "") + f" | Text LLM: {reason}"
                    logger.warning("Tier 1 Text+LLM extraction returned invalid result")
                    
            except Exception as e:
                result.fallback_reason = (result.fallback_reason or "") + f" | Text LLM error: {str(e)}"
                logger.warning("Tier 1 Text+LLM extraction failed", error=str(e))
        
        # ── Tier 0: Regex Fallback ─────────────────────────────────────
        if result.ai_extraction is None:
            logger.info("Using Tier 0: Regex FieldParser (final fallback)")
            result.tier_used = ExtractionTier.REGEX
            result.final_confidence = regex_fields.get("confidence_score", 0.5)
            # The regex result is already in result.regex_extraction
            # The caller will use field_parser results directly
        
        # Finalize
        result.processing_time_ms = int((time.time() - start_time) * 1000)
        result.success = True
        
        # Track cost
        self._total_cost += result.estimated_cost_usd
        self._total_extractions += 1
        
        logger.info(
            "AI extraction pipeline complete",
            tier=result.tier_used.value,
            confidence=result.final_confidence,
            cost_usd=f"${result.estimated_cost_usd:.4f}",
            time_ms=result.processing_time_ms,
            total_cost_session=f"${self._total_cost:.4f}",
            total_extractions=self._total_extractions,
        )
        
        return result
    
    def ai_to_invoice_data(
        self,
        ai_result: AIExtractionResult,
        raw_text: str,
    ) -> InvoiceData:
        """
        Convert an AIExtractionResult to the standard InvoiceData model.
        
        This merges AI extraction data into the existing InvoiceData format
        used by the rest of the pipeline (mapping engine, database, etc.).
        
        Args:
            ai_result: The AI extraction result
            raw_text: Original raw text for the raw_text field
            
        Returns:
            InvoiceData populated from AI extraction
        """
        if ai_result.ai_extraction is None:
            # No AI result — caller should use regex pipeline instead
            raise ValueError("No AI extraction available — use regex pipeline")
        
        ai = ai_result.ai_extraction
        
        # Convert line items
        line_items = []
        for i, ai_item in enumerate(ai.line_items or []):
            line_items.append(LineItem(
                line_number=ai_item.line_number or (i + 1),
                description=ai_item.description,
                item_code=ai_item.item_code,
                quantity=self._to_decimal(ai_item.quantity),
                unit=ai_item.unit,
                unit_price=self._to_decimal(ai_item.unit_price),
                tax_rate=self._to_decimal(ai_item.tax_rate),
                tax_amount=self._to_decimal(ai_item.tax_amount),
                discount_amount=self._to_decimal(ai_item.discount_amount),
                line_total=self._to_decimal(ai_item.line_total),
                gl_account_code=ai_item.gl_account_code,
                cost_center=ai_item.cost_center,
            ))
        
        # Parse dates
        invoice_date = self._parse_date(ai.invoice_date)
        due_date = self._parse_date(ai.due_date)
        
        # Build InvoiceData
        return InvoiceData(
            invoice_number=ai.invoice_number,
            po_number=ai.po_number,
            vendor=VendorInfo(
                name=ai.vendor.name if ai.vendor else None,
                address=ai.vendor.address if ai.vendor else None,
                email=ai.vendor.email if ai.vendor else None,
                phone=ai.vendor.phone if ai.vendor else None,
                tax_id=ai.vendor.tax_id if ai.vendor else None,
                website=ai.vendor.website if ai.vendor else None,
            ),
            invoice_date=invoice_date,
            due_date=due_date,
            subtotal=self._to_decimal(ai.subtotal),
            tax_amount=self._to_decimal(ai.tax_amount),
            discount_amount=self._to_decimal(ai.discount_amount),
            shipping_amount=self._to_decimal(ai.shipping_amount),
            total_amount=self._to_decimal(ai.total_amount),
            currency=ai.currency or "USD",
            line_items=line_items,
            payment_terms=ai.payment_terms,
            bank_details=ai.bank_details,
            gl_account=ai.gl_account,
            project=ai.project_number or ai.opportunity_number,
            item_category=None,
            location=ai.vendor.address if ai.vendor else None,
            cost_center=ai.cost_center,
            confidence_score=ai_result.final_confidence,
            requires_manual_review=(
                ai_result.final_confidence < 0.7
                or (ai_result.cross_validation and ai_result.cross_validation.recommended_review)
            ),
            raw_text=raw_text,
        )
    
    def _is_valid_extraction(self, extraction: AIInvoiceExtraction) -> bool:
        """Check if an AI extraction has minimum required fields."""
        has_invoice_id = bool(extraction.invoice_number or extraction.po_number)
        has_total = extraction.total_amount is not None
        has_vendor = bool(extraction.vendor and extraction.vendor.name)
        
        # Must have at least invoice number/PO AND (total OR vendor)
        if has_invoice_id and (has_total or has_vendor):
            return True
        
        # Or must have total AND vendor
        if has_total and has_vendor:
            return True
        
        logger.warning(
            "AI extraction validation failed",
            has_invoice_id=has_invoice_id,
            has_total=has_total,
            has_vendor=has_vendor,
        )
        return False
    
    @staticmethod
    def _to_decimal(value: Any) -> Optional[Decimal]:
        """Convert a value to Decimal safely."""
        if value is None:
            return None
        try:
            return Decimal(str(value))
        except (InvalidOperation, ValueError, TypeError):
            return None
    
    @staticmethod
    def _parse_date(value: Optional[str]) -> Optional[date]:
        """Parse a date string (ISO or common formats)."""
        if not value:
            return None
        try:
            from dateutil import parser as date_parser
            return date_parser.parse(value).date()
        except Exception:
            return None
    
    @property
    def is_ai_enabled(self) -> bool:
        """Check if any AI extraction tier is enabled."""
        return self.enable_vision or self.enable_text_llm
    
    @property
    def stats(self) -> dict:
        """Get extraction statistics."""
        return {
            "total_extractions": self._total_extractions,
            "total_cost_usd": round(self._total_cost, 4),
            "avg_cost_per_extraction": round(
                self._total_cost / max(self._total_extractions, 1), 4
            ),
            "vision_enabled": self.enable_vision,
            "text_llm_enabled": self.enable_text_llm,
            "cross_validation_enabled": self.enable_cross_validation,
            "initialized": self._initialized,
        }
