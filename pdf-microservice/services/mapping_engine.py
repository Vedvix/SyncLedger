"""
Field Mapping Engine — applies configurable mapping profiles to extracted invoice data.

Transforms raw extracted fields into the target system model based on the active
mapping profile. Supports date transformations, default values, fallback sources,
and vendor-pattern-based auto-profile selection.

Author: vedvix
"""

import re
from datetime import date, timedelta
from decimal import Decimal, InvalidOperation
from typing import Any, Dict, List, Optional

import structlog

from models.invoice_data import InvoiceData, LineItem, VendorInfo
from models.mapping_config import (
    DateTransform,
    FieldMappingRule,
    InvoiceMappingProfile,
    MappingSourceField,
    MappingTargetField,
    get_default_subcontractor_profile,
    get_standard_invoice_profile,
)

logger = structlog.get_logger(__name__)


class MappingEngine:
    """
    Applies a mapping profile to raw extracted fields, producing a final InvoiceData.
    
    The engine:
    1. Receives raw extracted fields (dict of source field → value)
    2. Matches the best mapping profile (by vendor pattern or explicit selection)
    3. Applies each mapping rule: source lookup → fallback → default → transform
    4. Returns a mapped InvoiceData plus any additional mapped fields
    """
    
    def __init__(self):
        """Initialize with built-in profiles."""
        self._profiles: Dict[str, InvoiceMappingProfile] = {}
        self._load_builtin_profiles()
    
    def _load_builtin_profiles(self):
        """Load the built-in mapping profiles."""
        for profile in [
            get_default_subcontractor_profile(),
            get_standard_invoice_profile(),
        ]:
            if profile.id:
                self._profiles[profile.id] = profile
    
    # ── Profile Management ──────────────────────────────────────────────────
    
    def register_profile(self, profile: InvoiceMappingProfile) -> None:
        """Register a mapping profile."""
        if profile.id:
            self._profiles[profile.id] = profile
            logger.info("Mapping profile registered", profile_id=profile.id, name=profile.name)
    
    def get_profile(self, profile_id: str) -> Optional[InvoiceMappingProfile]:
        """Get a profile by ID."""
        return self._profiles.get(profile_id)
    
    def list_profiles(self, organization_id: Optional[int] = None) -> List[InvoiceMappingProfile]:
        """List all profiles, optionally filtered by organization."""
        profiles = list(self._profiles.values())
        if organization_id is not None:
            profiles = [
                p for p in profiles
                if p.organization_id is None or p.organization_id == organization_id
            ]
        return profiles
    
    def remove_profile(self, profile_id: str) -> bool:
        """Remove a profile by ID."""
        if profile_id in self._profiles:
            del self._profiles[profile_id]
            return True
        return False
    
    def select_profile(
        self,
        vendor_name: Optional[str] = None,
        profile_id: Optional[str] = None,
        organization_id: Optional[int] = None,
    ) -> InvoiceMappingProfile:
        """
        Select the best mapping profile for the given context.
        
        Priority:
        1. Explicit profile_id
        2. Vendor pattern match (organization-specific first, then global)
        3. Organization default
        4. Global default
        """
        # 1. Explicit profile
        if profile_id and profile_id in self._profiles:
            return self._profiles[profile_id]
        
        # 2. Vendor pattern match
        if vendor_name:
            # Organization-specific profiles first
            for profile in self._profiles.values():
                if (
                    profile.vendor_pattern
                    and profile.organization_id == organization_id
                    and re.search(profile.vendor_pattern, vendor_name, re.IGNORECASE)
                ):
                    logger.info(
                        "Auto-matched vendor profile",
                        profile=profile.name,
                        vendor=vendor_name,
                    )
                    return profile
            
            # Global profiles
            for profile in self._profiles.values():
                if (
                    profile.vendor_pattern
                    and profile.organization_id is None
                    and re.search(profile.vendor_pattern, vendor_name, re.IGNORECASE)
                ):
                    return profile
        
        # 3/4. Default profile
        for profile in self._profiles.values():
            if profile.is_default:
                if organization_id and profile.organization_id == organization_id:
                    return profile
        
        for profile in self._profiles.values():
            if profile.is_default and profile.organization_id is None:
                return profile
        
        # Ultimate fallback: subcontractor profile
        return get_default_subcontractor_profile()
    
    # ── Mapping Execution ───────────────────────────────────────────────────
    
    def apply_mapping(
        self,
        raw_fields: Dict[str, Any],
        line_items: List[LineItem],
        profile: Optional[InvoiceMappingProfile] = None,
        profile_id: Optional[str] = None,
        organization_id: Optional[int] = None,
    ) -> "MappedInvoiceResult":
        """
        Apply a mapping profile to raw extracted fields.
        
        Args:
            raw_fields: Dict of source field name → extracted value
            line_items: Extracted line items
            profile: Explicit profile to use
            profile_id: Profile ID to look up
            organization_id: Org context for profile selection
            
        Returns:
            MappedInvoiceResult with the mapped InvoiceData and extra fields
        """
        # Resolve profile
        vendor_name = raw_fields.get("vendor_name")
        if profile is None:
            profile = self.select_profile(
                vendor_name=vendor_name,
                profile_id=profile_id,
                organization_id=organization_id,
            )
        
        logger.info(
            "Applying mapping profile",
            profile=profile.name,
            rules_count=len(profile.rules),
        )
        
        mapped: Dict[str, Any] = {}
        unmapped_targets: List[str] = []
        field_mappings: List[Dict[str, Any]] = []
        
        for rule in profile.rules:
            value, actual_source = self._resolve_value_traced(rule, raw_fields)
            if value is not None:
                mapped[rule.target_field] = value
                field_mappings.append({
                    "target": rule.target_field,
                    "source": actual_source,
                    "value": str(value),
                    "rule": rule.description or f"{rule.target_field} ← {actual_source}",
                })
            elif rule.is_required:
                unmapped_targets.append(rule.target_field)
                logger.warning(
                    "Required field not mapped",
                    target=rule.target_field,
                    source=rule.source_field,
                )
        
        # Apply GL account to line items if set
        gl_account = mapped.get(MappingTargetField.GL_ACCOUNT.value) or mapped.get("gl_account")
        if gl_account and line_items:
            for item in line_items:
                if not item.gl_account_code:
                    item.gl_account_code = gl_account
        
        # Build InvoiceData from mapped fields
        invoice_data = self._build_invoice_data(mapped, line_items, raw_fields)
        
        return MappedInvoiceResult(
            invoice_data=invoice_data,
            profile_used=profile,
            mapped_fields=mapped,
            unmapped_required=unmapped_targets,
            field_mappings=field_mappings,
            gl_account=gl_account,
            project=mapped.get(MappingTargetField.PROJECT.value) or mapped.get("project"),
            item=mapped.get(MappingTargetField.ITEM.value) or mapped.get("item"),
            location=mapped.get(MappingTargetField.LOCATION.value) or mapped.get("location"),
            cost_center=mapped.get(MappingTargetField.COST_CENTER.value) or mapped.get("cost_center"),
        )
    
    def _resolve_value_traced(
        self, rule: FieldMappingRule, raw_fields: Dict[str, Any]
    ) -> tuple:
        """Resolve the value for a mapping rule and return (value, actual_source)."""
        value = None
        actual_source = None
        
        # 1. Try primary source
        if rule.source_field:
            value = raw_fields.get(rule.source_field)
            if value is not None:
                actual_source = rule.source_field
        
        # 2. Try fallback source
        if value is None and rule.fallback_source:
            value = raw_fields.get(rule.fallback_source)
            if value is not None:
                actual_source = rule.fallback_source
        
        # 3. Apply date transform
        if rule.date_transform and rule.date_transform != DateTransform.NONE.value:
            src = rule.date_transform_source or actual_source
            value = self._apply_date_transform(
                value, rule.date_transform, rule.date_transform_source, raw_fields
            )
            actual_source = f"{src} → {rule.date_transform}" if src else rule.date_transform
        
        # 4. Apply default value
        if value is None and rule.default_value is not None:
            value = rule.default_value
            actual_source = f"default ({rule.default_value})"
        
        return value, actual_source

    def _resolve_value(
        self, rule: FieldMappingRule, raw_fields: Dict[str, Any]
    ) -> Any:
        """Resolve the value for a mapping rule."""
        value = None
        
        # 1. Try primary source
        if rule.source_field:
            value = raw_fields.get(rule.source_field)
        
        # 2. Try fallback source
        if value is None and rule.fallback_source:
            value = raw_fields.get(rule.fallback_source)
        
        # 3. Apply date transform
        if rule.date_transform and rule.date_transform != DateTransform.NONE.value:
            value = self._apply_date_transform(
                value, rule.date_transform, rule.date_transform_source, raw_fields
            )
        
        # 4. Apply default value
        if value is None and rule.default_value is not None:
            value = rule.default_value
        
        return value
    
    def _apply_date_transform(
        self,
        current_value: Any,
        transform: str,
        transform_source: Optional[str],
        raw_fields: Dict[str, Any],
    ) -> Optional[date]:
        """Apply a date transformation."""
        # Determine the base date
        base_date = None
        
        if transform_source:
            base_date = raw_fields.get(transform_source)
        
        if base_date is None and current_value is not None:
            base_date = current_value
        
        if base_date is None:
            # Fall back to today
            base_date = date.today()
        
        if isinstance(base_date, str):
            try:
                from dateutil import parser as date_parser
                base_date = date_parser.parse(base_date).date()
            except Exception:
                base_date = date.today()
        
        if not isinstance(base_date, date):
            base_date = date.today()
        
        if transform == DateTransform.NEXT_FRIDAY.value:
            return self._next_weekday(base_date, 4)  # 4 = Friday
        elif transform == DateTransform.NEXT_MONDAY.value:
            return self._next_weekday(base_date, 0)  # 0 = Monday
        elif transform == DateTransform.ADD_30_DAYS.value:
            return base_date + timedelta(days=30)
        elif transform == DateTransform.ADD_60_DAYS.value:
            return base_date + timedelta(days=60)
        elif transform == DateTransform.ADD_90_DAYS.value:
            return base_date + timedelta(days=90)
        elif transform == DateTransform.END_OF_MONTH.value:
            import calendar
            last_day = calendar.monthrange(base_date.year, base_date.month)[1]
            return base_date.replace(day=last_day)
        
        return current_value if isinstance(current_value, date) else None
    
    @staticmethod
    def _next_weekday(d: date, weekday: int) -> date:
        """
        Get the next occurrence of a weekday from the given date.
        
        Args:
            d: Base date
            weekday: 0=Monday, 4=Friday, 6=Sunday
            
        Returns:
            The next occurrence (always in the future, never the same day)
        """
        days_ahead = weekday - d.weekday()
        if days_ahead <= 0:
            days_ahead += 7
        return d + timedelta(days=days_ahead)
    
    def _build_invoice_data(
        self,
        mapped: Dict[str, Any],
        line_items: List[LineItem],
        raw_fields: Dict[str, Any],
    ) -> InvoiceData:
        """Build InvoiceData from mapped fields."""
        
        def get_decimal(key: str) -> Optional[Decimal]:
            val = mapped.get(key)
            if val is None:
                return None
            if isinstance(val, Decimal):
                return val
            try:
                return Decimal(str(val).replace(",", "").replace("$", ""))
            except (InvalidOperation, ValueError):
                return None
        
        def get_date(key: str) -> Optional[date]:
            val = mapped.get(key)
            if isinstance(val, date):
                return val
            if isinstance(val, str):
                try:
                    from dateutil import parser as date_parser
                    return date_parser.parse(val).date()
                except Exception:
                    return None
            return None
        
        def get_str(key: str) -> Optional[str]:
            val = mapped.get(key)
            return str(val) if val is not None else None
        
        total = get_decimal(MappingTargetField.TOTAL_AMOUNT.value)
        subtotal = get_decimal(MappingTargetField.SUBTOTAL.value)
        tax = get_decimal(MappingTargetField.TAX_AMOUNT.value)
        
        # Fallback: if no subtotal, use total
        if subtotal is None and total is not None and (tax is None or tax == Decimal("0")):
            subtotal = total
        
        return InvoiceData(
            invoice_number=get_str(MappingTargetField.INVOICE_NUMBER.value),
            po_number=get_str(MappingTargetField.PO_NUMBER.value),
            vendor=VendorInfo(
                name=get_str(MappingTargetField.VENDOR_NAME.value),
                address=get_str(MappingTargetField.VENDOR_ADDRESS.value),
                email=get_str(MappingTargetField.VENDOR_EMAIL.value),
                phone=get_str(MappingTargetField.VENDOR_PHONE.value),
            ),
            invoice_date=get_date(MappingTargetField.INVOICE_DATE.value),
            due_date=get_date(MappingTargetField.DUE_DATE.value),
            subtotal=subtotal,
            tax_amount=tax,
            total_amount=total,
            line_items=line_items,
            # Additional mapped fields stored in metadata
            gl_account=get_str(MappingTargetField.GL_ACCOUNT.value),
            project=get_str(MappingTargetField.PROJECT.value),
            item_category=get_str(MappingTargetField.ITEM.value),
            location=get_str(MappingTargetField.LOCATION.value),
            confidence_score=raw_fields.get("confidence_score", 0.0),
            requires_manual_review=raw_fields.get("requires_manual_review", False),
            raw_text=raw_fields.get("raw_text"),
        )


class MappedInvoiceResult:
    """Result of applying a mapping profile to extracted data."""
    
    def __init__(
        self,
        invoice_data: InvoiceData,
        profile_used: InvoiceMappingProfile,
        mapped_fields: Dict[str, Any],
        unmapped_required: List[str],
        field_mappings: List[Dict[str, Any]] = None,
        gl_account: Optional[str] = None,
        project: Optional[str] = None,
        item: Optional[str] = None,
        location: Optional[str] = None,
        cost_center: Optional[str] = None,
    ):
        self.invoice_data = invoice_data
        self.profile_used = profile_used
        self.mapped_fields = mapped_fields
        self.unmapped_required = unmapped_required
        self.field_mappings = field_mappings or []
        self.gl_account = gl_account
        self.project = project
        self.item = item
        self.location = location
        self.cost_center = cost_center
    
    @property
    def has_unmapped_required(self) -> bool:
        return len(self.unmapped_required) > 0
    
    def to_dict(self) -> Dict[str, Any]:
        """Serialize the result for API response."""
        return {
            "profile_id": self.profile_used.id,
            "profile_name": self.profile_used.name,
            "gl_account": self.gl_account,
            "project": self.project,
            "item": self.item,
            "location": self.location,
            "cost_center": self.cost_center,
            "unmapped_required_fields": self.unmapped_required,
            "field_mappings": self.field_mappings,
        }
