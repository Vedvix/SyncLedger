"""
Batch OCR Test Script for SyncLedger
Tests all sample PDFs and generates a comprehensive extraction report.

Usage: python batch_ocr_test.py
"""

import asyncio
import glob
import json
import os
import sys
import time
from collections import defaultdict
from datetime import datetime

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from services.pdf_extractor import PDFExtractor
from services.ocr_service import OCRService
from services.field_parser import FieldParser
from services.mapping_engine import MappingEngine


STATIC_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "static")


async def process_single_pdf(pdf_path: str, pdf_extractor: PDFExtractor, 
                              ocr_service: OCRService, field_parser: FieldParser,
                              mapping_engine: MappingEngine) -> dict:
    """Process a single PDF and return extraction results."""
    filename = os.path.basename(pdf_path)
    start_time = time.time()
    
    result = {
        "filename": filename,
        "success": False,
        "error": None,
        "extraction_method": None,
        "page_count": 0,
        "processing_time_ms": 0,
        "confidence_score": 0.0,
        "fields_extracted": {},
        "fields_missing": [],
        "line_items_count": 0,
        "mapping_profile": None,
    }
    
    try:
        # Extract text
        extraction_result = await pdf_extractor.extract(pdf_path)
        
        if extraction_result.needs_ocr:
            extraction_result = await ocr_service.extract(pdf_path)
        
        result["extraction_method"] = extraction_result.method
        result["page_count"] = extraction_result.page_count
        
        # Parse fields
        raw_fields, line_items = field_parser.parse_with_line_items(extraction_result.text)
        
        # Apply mapping
        mapping_result = mapping_engine.apply_mapping(
            raw_fields=raw_fields,
            line_items=line_items,
        )
        
        invoice_data = mapping_result.invoice_data
        
        # Collect results
        result["success"] = True
        result["confidence_score"] = invoice_data.confidence_score or 0.0
        result["mapping_profile"] = mapping_result.profile_used.id if mapping_result.profile_used else None
        result["line_items_count"] = len(invoice_data.line_items) if invoice_data.line_items else 0
        
        # Track which fields were extracted
        all_fields = [
            "invoice_number", "po_number", "vendor_name", "vendor_address",
            "vendor_email", "vendor_phone", "invoice_date", "due_date",
            "subtotal", "total_amount", "tax_amount",
            "gl_account", "project", "item_category"
        ]
        
        extracted = {}
        missing = []
        
        # Invoice number
        inv_num = invoice_data.invoice_number
        if inv_num and inv_num != "UNKNOWN" and not inv_num.startswith("PENDING"):
            extracted["invoice_number"] = str(inv_num)
        else:
            missing.append("invoice_number")
        
        # PO number
        if invoice_data.po_number:
            extracted["po_number"] = str(invoice_data.po_number)
        else:
            missing.append("po_number")
        
        # Vendor
        if invoice_data.vendor and invoice_data.vendor.name and invoice_data.vendor.name != "Unknown":
            extracted["vendor_name"] = invoice_data.vendor.name
        else:
            missing.append("vendor_name")
        
        if invoice_data.vendor and invoice_data.vendor.address:
            extracted["vendor_address"] = invoice_data.vendor.address[:60]
        else:
            missing.append("vendor_address")
        
        if invoice_data.vendor and invoice_data.vendor.email:
            extracted["vendor_email"] = invoice_data.vendor.email
        else:
            missing.append("vendor_email")
        
        if invoice_data.vendor and invoice_data.vendor.phone:
            extracted["vendor_phone"] = invoice_data.vendor.phone
        else:
            missing.append("vendor_phone")
        
        # Dates
        if invoice_data.invoice_date:
            extracted["invoice_date"] = str(invoice_data.invoice_date)
        else:
            missing.append("invoice_date")
        
        if invoice_data.due_date:
            extracted["due_date"] = str(invoice_data.due_date)
        else:
            missing.append("due_date")
        
        # Financial
        if invoice_data.subtotal is not None:
            extracted["subtotal"] = str(invoice_data.subtotal)
        else:
            missing.append("subtotal")
        
        if invoice_data.total_amount is not None:
            extracted["total_amount"] = str(invoice_data.total_amount)
        else:
            missing.append("total_amount")
        
        if invoice_data.tax_amount is not None and float(invoice_data.tax_amount) > 0:
            extracted["tax_amount"] = str(invoice_data.tax_amount)
        else:
            missing.append("tax_amount")
        
        # Mapping fields
        if mapping_result.gl_account:
            extracted["gl_account"] = mapping_result.gl_account
        else:
            missing.append("gl_account")
        
        if mapping_result.project:
            extracted["project"] = mapping_result.project
        else:
            missing.append("project")
        
        if mapping_result.item:
            extracted["item_category"] = mapping_result.item
        else:
            missing.append("item_category")
        
        result["fields_extracted"] = extracted
        result["fields_missing"] = missing
        
    except Exception as e:
        result["error"] = str(e)
    
    result["processing_time_ms"] = int((time.time() - start_time) * 1000)
    return result


async def main():
    print("=" * 80)
    print("SyncLedger OCR Batch Test Report")
    print(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Sample Directory: {STATIC_DIR}")
    print("=" * 80)
    
    # Find all PDFs
    pdf_files = sorted(glob.glob(os.path.join(STATIC_DIR, "*.pdf")) + 
                       glob.glob(os.path.join(STATIC_DIR, "*.PDF")))
    
    if not pdf_files:
        print(f"No PDF files found in {STATIC_DIR}")
        return
    
    print(f"\nTotal PDFs found: {len(pdf_files)}")
    print("-" * 80)
    
    # Initialize services
    pdf_extractor = PDFExtractor()
    ocr_service = OCRService()
    field_parser = FieldParser()
    mapping_engine = MappingEngine()
    
    # Process all PDFs
    results = []
    for i, pdf_path in enumerate(pdf_files, 1):
        filename = os.path.basename(pdf_path)
        print(f"[{i}/{len(pdf_files)}] Processing: {filename}...", end=" ", flush=True)
        
        result = await process_single_pdf(pdf_path, pdf_extractor, ocr_service, field_parser, mapping_engine)
        results.append(result)
        
        if result["success"]:
            conf = result["confidence_score"]
            extracted_count = len(result["fields_extracted"])
            total_fields = extracted_count + len(result["fields_missing"])
            print(f"OK  confidence={conf:.0%}  fields={extracted_count}/{total_fields}  "
                  f"lines={result['line_items_count']}  time={result['processing_time_ms']}ms")
        else:
            print(f"FAIL  error={result['error'][:60]}")
    
    # ── Generate Summary Report ──────────────────────────────────────────
    
    print("\n" + "=" * 80)
    print("EXTRACTION SUMMARY REPORT")
    print("=" * 80)
    
    total = len(results)
    successful = [r for r in results if r["success"]]
    failed = [r for r in results if not r["success"]]
    
    print(f"\n{'Total PDFs:':<30} {total}")
    print(f"{'Successful extractions:':<30} {len(successful)} ({len(successful)/total*100:.1f}%)")
    print(f"{'Failed extractions:':<30} {len(failed)} ({len(failed)/total*100:.1f}%)")
    
    if successful:
        confidences = [r["confidence_score"] for r in successful]
        avg_conf = sum(confidences) / len(confidences)
        high_conf = len([c for c in confidences if c >= 0.87])
        med_conf = len([c for c in confidences if 0.5 <= c < 0.87])
        low_conf = len([c for c in confidences if c < 0.5])
        
        print(f"\n{'--- Confidence Scores ---':}")
        print(f"{'Average confidence:':<30} {avg_conf:.1%}")
        print(f"{'High (>=87%):':<30} {high_conf} ({high_conf/len(successful)*100:.1f}%)")
        print(f"{'Medium (50-86%):':<30} {med_conf} ({med_conf/len(successful)*100:.1f}%)")
        print(f"{'Low (<50%):':<30} {low_conf} ({low_conf/len(successful)*100:.1f}%)")
        print(f"{'Min confidence:':<30} {min(confidences):.1%}")
        print(f"{'Max confidence:':<30} {max(confidences):.1%}")
    
    # Field extraction rates
    if successful:
        print(f"\n{'--- Field Extraction Rates ---':}")
        field_names = [
            "invoice_number", "po_number", "vendor_name", "vendor_address",
            "vendor_email", "vendor_phone", "invoice_date", "due_date",
            "subtotal", "total_amount", "tax_amount",
            "gl_account", "project", "item_category"
        ]
        
        for field in field_names:
            count = sum(1 for r in successful if field in r["fields_extracted"])
            pct = count / len(successful) * 100
            bar = "█" * int(pct / 5) + "░" * (20 - int(pct / 5))
            print(f"  {field:<20} {bar} {count:>3}/{len(successful)} ({pct:.1f}%)")
    
    # Line items stats
    if successful:
        with_lines = [r for r in successful if r["line_items_count"] > 0]
        total_lines = sum(r["line_items_count"] for r in successful)
        print(f"\n{'--- Line Items ---':}")
        print(f"{'PDFs with line items:':<30} {len(with_lines)} ({len(with_lines)/len(successful)*100:.1f}%)")
        print(f"{'Total line items extracted:':<30} {total_lines}")
        if with_lines:
            avg_lines = total_lines / len(with_lines)
            print(f"{'Avg lines per PDF (when >0):':<30} {avg_lines:.1f}")
    
    # Extraction method breakdown
    if successful:
        methods = defaultdict(int)
        for r in successful:
            methods[r["extraction_method"] or "unknown"] += 1
        print(f"\n{'--- Extraction Methods ---':}")
        for method, count in sorted(methods.items(), key=lambda x: -x[1]):
            print(f"  {method:<25} {count:>3} ({count/len(successful)*100:.1f}%)")
    
    # Mapping profile breakdown
    if successful:
        profiles = defaultdict(int)
        for r in successful:
            profiles[r["mapping_profile"] or "none"] += 1
        print(f"\n{'--- Mapping Profiles Used ---':}")
        for profile, count in sorted(profiles.items(), key=lambda x: -x[1]):
            print(f"  {profile:<30} {count:>3} ({count/len(successful)*100:.1f}%)")
    
    # Processing time
    if successful:
        times = [r["processing_time_ms"] for r in successful]
        print(f"\n{'--- Processing Time ---':}")
        print(f"{'Average:':<30} {sum(times)/len(times):.0f}ms")
        print(f"{'Min:':<30} {min(times)}ms")
        print(f"{'Max:':<30} {max(times)}ms")
        print(f"{'Total:':<30} {sum(times)/1000:.1f}s")
    
    # Failed files
    if failed:
        print(f"\n{'--- Failed Files ---':}")
        for r in failed:
            print(f"  {r['filename']:<40} {r['error'][:60]}")
    
    # Vendor breakdown (unique vendors found)
    if successful:
        vendors = defaultdict(int)
        for r in successful:
            v = r["fields_extracted"].get("vendor_name", "Unknown")
            vendors[v] += 1
        print(f"\n{'--- Unique Vendors Detected ---':} {len(vendors)}")
        for vendor, count in sorted(vendors.items(), key=lambda x: -x[1])[:15]:
            print(f"  {vendor:<45} {count:>3} invoices")
        if len(vendors) > 15:
            print(f"  ... and {len(vendors) - 15} more vendors")
    
    print("\n" + "=" * 80)
    print("END OF REPORT")
    print("=" * 80)
    
    # Save JSON report
    report_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ocr_report.json")
    report_data = {
        "timestamp": datetime.now().isoformat(),
        "total_files": total,
        "successful": len(successful),
        "failed": len(failed),
        "avg_confidence": avg_conf if successful else 0,
        "results": results,
    }
    with open(report_path, "w") as f:
        json.dump(report_data, f, indent=2, default=str)
    print(f"\nDetailed JSON report saved to: {report_path}")


if __name__ == "__main__":
    asyncio.run(main())
