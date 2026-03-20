"""
Comprehensive OCR field alias/variation registry.

Maps all known label variations that appear on vendor invoices to standard field names.
Used by the FieldParser to recognise fields regardless of label wording.
Organizations can override or extend these aliases via their mapping profile.

Author: vedvix
"""

from typing import Dict, List

# ─── Standard Field → OCR Label Variations ────────────────────────────────────
# Keys are the canonical MappingSourceField values.
# Values are lists of regex-ready label patterns (case-insensitive).
# Patterns must NOT include a capture group — the parser will append one.

DEFAULT_FIELD_ALIASES: Dict[str, List[str]] = {
    # ── 1. INVOICE NUMBER ─────────────────────────────────────────────────
    "invoice_number": [
        r"invoice\s*#",
        r"invoice\s+no\.?",
        r"invoice\s+number",
        r"inv\s*#",
        r"inv\s+no\.?",
        r"inv\.\s*no\.?",
        r"invoice\s*#",
        r"invoiceno",
        r"inv#",
        r"invno",
        r"bill\s*#",
        r"bill\s+no\.?",
        r"bill\s+number",
        r"billing\s*#",
        r"billing\s+no\.?",
        r"document\s*#",
        r"document\s+no\.?",
        r"doc\s*#",
        r"doc\s+no\.?",
        r"invoice\s+id",
        r"inv\s+id",
        r"billing\s+id",
        r"statement\s*#",
        r"our\s+invoice",
        r"your\s+invoice",
        r"invoice\s+ref",
        # International
        r"facture",
        r"factura",
        r"rechnung",
        r"nota\s+fiscal",
    ],

    # ── 2. PURCHASE ORDER NUMBER ──────────────────────────────────────────
    "po_number": [
        r"po\s*#",
        r"po\s+no\.?",
        r"po\s+number",
        r"p\.o\.\s*#",
        r"p\.o\.\s+no\.?",
        r"p\.o\.\s+number",
        r"po#",
        r"pono",
        r"p\.o#",
        r"order\s*#",
        r"order\s+no\.?",
        r"order\s+number",
        r"purchase\s+order\s*#",
        r"purchase\s+order\s+no\.?",
        r"purchase\s+order\s+number",
        r"customer\s+po",
        r"cust\s+po",
        r"client\s+po",
        r"buyer\s+po",
        r"po\s+ref",
        r"po\s+reference",
        r"purchase\s+order\s+ref",
        r"your\s+po",
        r"our\s+po",
        r"po\s+id",
        r"purchase\s+order\s+id",
    ],

    # ── 3. VENDOR NAME ────────────────────────────────────────────────────
    "vendor_name": [
        r"vendor",
        r"vendor\s+name",
        r"supplier",
        r"supplier\s+name",
        r"from",
        r"sold\s+by",
        r"remit\s+to",
        r"seller",
        r"provider",
        r"company\s+name",
        r"business\s+name",
        r"merchant",
        r"bill\s+from",
        r"invoice\s+from",
        r"supplied\s+by",
    ],

    # ── 4. VENDOR ADDRESS ─────────────────────────────────────────────────
    "vendor_address": [
        r"vendor\s+address",
        r"supplier\s+address",
        r"from\s+address",
        r"remit\s+to\s+address",
        r"seller\s+address",
        r"company\s+address",
        r"business\s+address",
        r"return\s+address",
        r"mailing\s+address",
        r"registered\s+address",
        r"principal\s+address",
    ],

    # ── 5. VENDOR PHONE ───────────────────────────────────────────────────
    "vendor_phone": [
        r"phone\s*#?",
        r"phone\s+no\.?",
        r"telephone",
        r"tel\s*:?",
        r"contact\s*#",
        r"contact\s+number",
        r"vendor\s+phone",
        r"supplier\s+phone",
        r"office\s+phone",
        r"main\s+phone",
    ],

    # ── 6. VENDOR EMAIL ───────────────────────────────────────────────────
    "vendor_email": [
        r"e?\-?mail(?:\s+address)?",
        r"contact\s+email",
        r"vendor\s+email",
        r"supplier\s+email",
        r"correspondence\s+email",
        r"billing\s+email",
        r"accounts\s+email",
    ],

    # ── 7. VENDOR TAX ID ──────────────────────────────────────────────────
    "vendor_tax_id": [
        r"tax\s+id\s*#?",
        r"tax\s+id\s+no\.?",
        r"tax\s+identification",
        r"ein\s*#?",
        r"federal\s+id",
        r"federal\s+tax\s+id",
        r"vat\s*#",
        r"vat\s+no\.?",
        r"vat\s+number",
        r"vat\s+id",
        r"tin",
        r"fein",
        r"employer\s+id",
        r"gst\s*#",
        r"gst\s+no\.?",
        r"business\s*#",
        r"business\s+number",
        r"tax\s+registration",
        r"fiscal\s+id",
        r"abn",  # Australia
        r"pan",  # India
    ],

    # ── 8. BILL-TO NAME ───────────────────────────────────────────────────
    "bill_to_name": [
        r"bill\s+to",
        r"billed\s+to",
        r"customer(?:\s+name)?",
        r"sold\s+to",
        r"buyer",
        r"client",
        r"account\s+name",
        r"billing\s+name",
        r"invoice\s+to",
        r"to\s*:",
        r"recipient",
    ],

    # ── 9. BILL-TO ADDRESS ────────────────────────────────────────────────
    "bill_to_address": [
        r"bill\s+to\s+address",
        r"billing\s+address",
        r"customer\s+address",
        r"sold\s+to\s+address",
        r"buyer\s+address",
        r"client\s+address",
        r"invoice\s+address",
        r"account\s+address",
    ],

    # ── 10. SHIP-TO NAME ──────────────────────────────────────────────────
    "ship_to_name": [
        r"ship\s+to(?:\s+name)?",
        r"shipping\s+name",
        r"deliver\s+to",
        r"delivery\s+name",
        r"recipient\s+name",
        r"shipping\s+contact",
        r"consignee",
        r"destination",
    ],

    # ── 11. SHIP-TO ADDRESS ───────────────────────────────────────────────
    "ship_to_address": [
        r"ship\s+to\s+address",
        r"shipping\s+address",
        r"delivery\s+address",
        r"ship\s+address",
        r"destination\s+address",
        r"consignee\s+address",
        r"delivery\s+location",
        r"shipping\s+location",
    ],

    # ── 12. INVOICE DATE ──────────────────────────────────────────────────
    "invoice_date": [
        r"invoice\s+date",
        r"inv\s+date",
        r"bill\s+date",
        r"billing\s+date",
        r"document\s+date",
        r"issue\s+date",
        r"date\s+of\s+invoice",
        r"invoice\s+dt",
        r"dated",
        r"created\s+date",
        r"statement\s+date",
    ],

    # ── 13. DUE DATE ──────────────────────────────────────────────────────
    "due_date": [
        r"due\s+date",
        r"payment\s+due",
        r"payable\s+by",
        r"payment\s+date",
        r"pay\s+by\s+date",
        r"payment\s+deadline",
        r"due\s+by",
        r"maturity\s+date",
        r"expiry\s+date",
        r"please\s+pay\s+by",
        r"payment\s+due\s+date",
    ],

    # ── 14. SHIP DATE ─────────────────────────────────────────────────────
    "ship_date": [
        r"ship\s+date",
        r"shipping\s+date",
        r"shipped(?:\s+date)?",
        r"date\s+shipped",
        r"dispatch\s+date",
        r"sent\s+date",
        r"departure\s+date",
        r"shipment\s+date",
    ],

    # ── 15. DELIVERY DATE ─────────────────────────────────────────────────
    "delivery_date": [
        r"delivery\s+date",
        r"delivered(?:\s+date)?",
        r"date\s+delivered",
        r"arrival\s+date",
        r"received\s+date",
        r"del\s+date",
        r"expected\s+delivery",
    ],

    # ── 16. SUBTOTAL ──────────────────────────────────────────────────────
    "subtotal": [
        r"subtotal",
        r"sub\s*-?\s*total",
        r"net\s+total",
        r"net\s+amount",
        r"amount\s+before\s+tax",
        r"pre\s*-?\s*tax\s+amount",
        r"merchandise\s+total",
        r"goods\s+total",
        r"items?\s+total",
        r"sub\s+amount",
        r"taxable\s+amount",
    ],

    # ── 17. TAX AMOUNT ────────────────────────────────────────────────────
    "tax_amount": [
        r"(?:sales\s+)?tax(?:\s+amount)?",
        r"vat(?:\s+amount)?",
        r"gst(?:\s+amount)?",
        r"hst",
        r"total\s+tax",
        r"tax\s+total",
        r"taxes",
        r"tax\s+charged",
        r"state\s+tax",
        r"local\s+tax",
        r"applicable\s+tax",
        r"tax\s+due",
        r"cgst",
        r"sgst",
        r"igst",
        r"pst",
    ],

    # ── 18. SHIPPING AMOUNT ───────────────────────────────────────────────
    "shipping_amount": [
        r"shipping(?:\s+amount)?",
        r"shipping\s+cost",
        r"freight(?:\s+charges)?",
        r"delivery\s+charge",
        r"delivery\s+fee",
        r"shipping\s+&?\s*handling",
        r"s\s*&\s*h",
        r"postage",
        r"carriage",
        r"transportation",
        r"shipping\s+total",
    ],

    # ── 19. DISCOUNT AMOUNT ───────────────────────────────────────────────
    "discount_amount": [
        r"discount(?:\s+amount)?",
        r"less\s+discount",
        r"deduction",
        r"rebate",
        r"allowance",
        r"price\s+reduction",
        r"savings",
        r"promotional\s+discount",
        r"trade\s+discount",
        r"cash\s+discount",
    ],

    # ── 20. TOTAL AMOUNT ──────────────────────────────────────────────────
    "total": [
        r"total(?:\s+amount)?",
        r"grand\s+total",
        r"invoice\s+total",
        r"balance",
        r"total\s+due",
        r"final\s+amount",
        r"sum\s+total",
        r"overall\s+total",
        r"net\s+payable",
        r"total\s+charges",
        r"invoice\s+amount",
        r"bill\s+total",
    ],

    # ── 21. AMOUNT DUE ────────────────────────────────────────────────────
    "amount_due": [
        r"amount\s+due",
        r"balance\s+due",
        r"total\s+due",
        r"payable",
        r"payment\s+amount",
        r"amount\s+payable",
        r"outstanding",
        r"amount\s+owing",
        r"pay\s+amount",
        r"remittance\s+amount",
        r"please\s+pay",
        r"payment\s+required",
    ],

    # ── 22. CUSTOMER / ACCOUNT NUMBER ─────────────────────────────────────
    "customer_number": [
        r"customer\s*#",
        r"customer\s+no\.?",
        r"customer\s+number",
        r"customer\s+id",
        r"account\s*#",
        r"account\s+no\.?",
        r"account\s+number",
        r"acct\s*#",
        r"acct\s+no\.?",
        r"client\s*#",
        r"client\s+no\.?",
        r"client\s+id",
        r"cust\s*#",
        r"cust\s+no\.?",
        r"customer\s+code",
        r"account\s+code",
        r"client\s+code",
        r"account\s+id",
    ],

    # ── 23. REFERENCE NUMBER ──────────────────────────────────────────────
    "reference_number": [
        r"reference\s*#?",
        r"reference\s+no\.?",
        r"ref\s*#",
        r"ref\s+no\.?",
        r"reference\s+number",
        r"your\s+ref",
        r"our\s+ref",
        r"internal\s+ref",
        r"transaction\s+ref",
        r"doc\s+ref",
    ],

    # ── 24. ORDER NUMBER ──────────────────────────────────────────────────
    "order_number": [
        r"order\s*#",
        r"order\s+no\.?",
        r"order\s+number",
        r"sales\s+order(?:\s*#)?",
        r"so\s*#",
        r"so\s+no\.?",
        r"order\s+id",
        r"work\s+order(?:\s*#)?",
        r"wo\s*#",
        r"order\s+ref",
    ],

    # ── 25. JOB / PROJECT NUMBER ──────────────────────────────────────────
    "project_number": [
        r"job\s*#",
        r"job\s+no\.?",
        r"job\s+number",
        r"job\s+id",
        r"job\s+code",
        r"job\s+name",
        r"project\s+job",
        r"work\s*#",
        r"work\s+no\.?",
        r"project\s*#",
        r"project\s+no\.?",
        r"project\s+number",
        r"project\s+id",
        r"project\s+code",
        r"project\s+name",
        r"project\s+ref",
    ],

    # ── 26. PAYMENT TERMS ─────────────────────────────────────────────────
    "payment_terms": [
        r"payment\s+terms",
        r"terms",
        r"payment\s+conditions",
        r"credit\s+terms",
        r"pay\s+terms",
        r"payment\s+method",
        r"payment\s+info",
        r"net\s+terms",
        r"days\s+terms",
        r"terms\s+of\s+payment",
    ],

    # ── 27. CURRENCY ──────────────────────────────────────────────────────
    "currency": [
        r"currency",
        r"curr",
        r"payment\s+currency",
        r"invoice\s+currency",
    ],

    # ── 28. GL ACCOUNT ────────────────────────────────────────────────────
    "gl_account": [
        r"g/?l\s+account\s*#?",
        r"gl\s+code",
        r"account\s+code",
        r"general\s+ledger",
        r"ledger\s+account",
        r"chart\s+of\s+accounts",
        r"coa\s+code",
    ],

    # ── 29. COST CENTER ───────────────────────────────────────────────────
    "cost_center": [
        r"cost\s+cent(?:er|re)\s*:?",
        r"department",
        r"dept\s+code",
        r"division",
        r"profit\s+cent(?:er|re)",
        r"responsibility\s+cent(?:er|re)",
    ],
}


# ─── Line-Item Field Aliases ─────────────────────────────────────────────────
LINE_ITEM_ALIASES: Dict[str, List[str]] = {
    "description": [
        r"description",
        r"item\s+description",
        r"product\s+description",
        r"service\s+description",
        r"details",
        r"item",
        r"product",
        r"goods",
        r"service",
    ],
    "quantity": [
        r"qty",
        r"quantity",
        r"units",
        r"count",
        r"number\s+of\s+units",
    ],
    "unit_price": [
        r"unit\s+price",
        r"price",
        r"rate",
        r"unit\s+cost",
        r"price\s+each",
        r"ea\.?",
        r"each",
        r"price\s*/\s*unit",
        r"cost\s+per\s+unit",
    ],
    "line_total": [
        r"extended\s+price",
        r"line\s+total",
        r"amount",
        r"extension",
        r"line\s+amount",
    ],
    "item_code": [
        r"item\s*#",
        r"item\s+no\.?",
        r"item\s+code",
        r"product\s*#",
        r"product\s+code",
        r"sku",
        r"part\s*#",
        r"part\s+no\.?",
        r"catalog\s*#",
        r"stock\s*#",
    ],
    "uom": [
        r"uom",
        r"unit(?:\s+of\s+measure)?",
        r"um",
    ],
}


# ─── Field Priority (when same data appears under multiple labels) ────────────
FIELD_PRIORITY: Dict[str, List[str]] = {
    "invoice_number": [
        "invoice_number",
        "inv_number",
        "bill_number",
        "document_number",
    ],
    "po_number": [
        "po_number",
        "purchase_order_number",
        "order_number",
        "customer_po",
    ],
    "total": [
        "total_amount",
        "invoice_total",
        "grand_total",
        "amount_due",
        "balance_due",
    ],
}


# ─── Vendor-Specific Invoice Patterns ────────────────────────────────────────
# Maps vendor name regex → known quirks of their invoices.
VENDOR_INVOICE_PATTERNS: Dict[str, Dict] = {
    # Bath Suppliers
    r"(?i)aqua\s*grip": {"invoice_format": "numeric"},
    r"(?i)arizona\s+shower": {"invoice_format": "IN-prefix"},
    r"(?i)bath\s+concepts|bci": {"invoice_format": "numeric-IN-suffix"},
    r"(?i)ferguson": {"invoice_format": "alpha-prefix", "prefixes": ["WT", "WX"]},
    # Roof Suppliers
    r"(?i)abc\s+supply": {"invoice_format": "long-numeric-dash"},
    r"(?i)lansing": {"invoice_format": "8-digit-suffix"},
    r"(?i)qxo|beacon": {"invoice_format": "2-letter-numeric"},
    r"(?i)srs\s+distribution": {"invoice_format": "long-numeric-dash"},
    # Window Suppliers
    r"(?i)custom\s+window": {"invoice_format": "7-digit"},
    r"(?i)guida": {"invoice_format": "8-digit-leading-zeros"},
    r"(?i)kensington": {"invoice_format": "numeric-dash-suffix"},
    r"(?i)soft\s*-?\s*lite": {"invoice_format": "numeric-dash-suffix"},
}


def build_alias_regex(field_name: str, aliases: Dict[str, List[str]] = None) -> str:
    """
    Build a single combined regex for all aliases of a given field.

    Returns a pattern like ``(?:alias1|alias2|alias3)`` (case-insensitive).
    The caller is responsible for adding the capture group for the value.
    """
    source = aliases or DEFAULT_FIELD_ALIASES
    patterns = source.get(field_name, [])
    if not patterns:
        return ""
    return "(?:" + "|".join(patterns) + ")"
