"""
File type detection utilities for PDF and image support.

Author: vedvix
"""

import os
from typing import Tuple

from PIL import Image

# Supported image extensions
IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.tiff', '.tif', '.bmp'}

# All supported file extensions
SUPPORTED_EXTENSIONS = {'.pdf'} | IMAGE_EXTENSIONS


def is_image_file(file_path: str) -> bool:
    """Check if a file is a supported image based on extension."""
    ext = os.path.splitext(file_path)[1].lower()
    return ext in IMAGE_EXTENSIONS


def is_pdf_file(file_path: str) -> bool:
    """Check if a file is a PDF based on extension."""
    return os.path.splitext(file_path)[1].lower() == '.pdf'


def is_supported_file(filename: str) -> bool:
    """Check if a filename has a supported extension (PDF or image)."""
    ext = os.path.splitext(filename)[1].lower()
    return ext in SUPPORTED_EXTENSIONS


def get_file_suffix(filename: str) -> str:
    """Get the file extension (suffix) for temp file creation."""
    ext = os.path.splitext(filename)[1].lower()
    return ext if ext else '.pdf'


def load_image(file_path: str) -> Image.Image:
    """Load an image file as a PIL Image, converting to RGB if needed."""
    img = Image.open(file_path)
    if img.mode not in ('RGB', 'L'):
        img = img.convert('RGB')
    return img
