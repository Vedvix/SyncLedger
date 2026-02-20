import pdfplumber
import os

STATIC_DIR = r"D:\Projects\SyncLedger\static"

GROUP_A = [
    "011326-Stock-MD.pdf",
    "011326-Stock.pdf",
    "1822587.pdf",
    "1824575.pdf",
    "1831873.pdf",
    "1839919.pdf",
    "1843466.pdf",
    "1911407-00.pdf",
    "1913023-00.pdf",
    "1914974-00.pdf",
    "1960010-00.pdf",
    "1960483-00.pdf",
]

GROUP_B = [
    "0047460967-001.pdf",
    "0047487532-001.pdf",
    "0639243.PDF",
    "14374042.pdf",
    "14441911.pdf",
    "231007.pdf",
    "526010-R.pdf",
    "84765.pdf",
    "001117657.pdf",
]

def extract_first_page(pdf_path):
    try:
        with pdfplumber.open(pdf_path) as pdf:
            if len(pdf.pages) == 0:
                return "[NO PAGES]"
            text = pdf.pages[0].extract_text()
            if text is None:
                return "[extract_text() returned None - likely scanned/image PDF]"
            if text.strip() == "":
                return "[extract_text() returned empty string]"
            return text
    except Exception as e:
        return f"[ERROR: {e}]"

def extract_all_pages(pdf_path):
    """Fallback: extract all pages if first page is empty."""
    try:
        with pdfplumber.open(pdf_path) as pdf:
            all_text = []
            for i, page in enumerate(pdf.pages):
                t = page.extract_text()
                if t and t.strip():
                    all_text.append(f"--- Page {i+1} ---\n{t}")
            if not all_text:
                return None
            return "\n".join(all_text)
    except:
        return None

print("=" * 80)
print("GROUP A - Invoice number returning empty string")
print("=" * 80)

for fname in GROUP_A:
    path = os.path.join(STATIC_DIR, fname)
    print(f"\n{'─' * 70}")
    print(f"FILE: {fname}")
    print(f"EXISTS: {os.path.exists(path)}")
    print(f"{'─' * 70}")
    if not os.path.exists(path):
        print("[FILE NOT FOUND]")
        continue
    text = extract_first_page(path)
    print(text)
    # If first page was empty, try all pages
    if text.startswith("[extract_text() returned"):
        all_text = extract_all_pages(path)
        if all_text:
            print("\n[FALLBACK - other pages had text:]")
            print(all_text)

print("\n\n")
print("=" * 80)
print("GROUP B - Total amount missing")
print("=" * 80)

for fname in GROUP_B:
    path = os.path.join(STATIC_DIR, fname)
    print(f"\n{'─' * 70}")
    print(f"FILE: {fname}")
    print(f"EXISTS: {os.path.exists(path)}")
    print(f"{'─' * 70}")
    if not os.path.exists(path):
        print("[FILE NOT FOUND]")
        continue
    text = extract_first_page(path)
    print(text)
    if text.startswith("[extract_text() returned"):
        all_text = extract_all_pages(path)
        if all_text:
            print("\n[FALLBACK - other pages had text:]")
            print(all_text)
