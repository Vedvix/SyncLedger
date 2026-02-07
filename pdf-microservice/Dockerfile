# =============================================================================
# SYNCLEDGER PDF MICROSERVICE - DOCKERFILE
# Python FastAPI service for PDF extraction
# =============================================================================

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies for OCR and PDF processing
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    poppler-utils \
    curl \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -g 1001 syncledger && \
    useradd -u 1001 -g syncledger -m syncledger

# Copy requirements first for caching
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create temp directory for PDF processing
RUN mkdir -p /tmp/pdf_processing && \
    chown -R syncledger:syncledger /app /tmp/pdf_processing

USER syncledger

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Run the application
EXPOSE 8001
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
