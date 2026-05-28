import cv2
import numpy as np
from PIL import Image, ImageChops, ImageEnhance
import io
import hashlib
import time

def run_ela_analysis(image_bytes: bytes) -> float:
    """Error Level Analysis to detect pixel manipulation"""
    try:
        original = Image.open(io.BytesIO(image_bytes)).convert("RGB")

        buffer = io.BytesIO()
        original.save(buffer, format="JPEG", quality=90)
        buffer.seek(0)
        compressed = Image.open(buffer).convert("RGB")

        ela_image = ImageChops.difference(original, compressed)
        enhancer = ImageEnhance.Brightness(ela_image)
        ela_enhanced = enhancer.enhance(10)
        ela_enhanced_array = np.array(ela_enhanced)

        ela_mean = np.mean(ela_enhanced_array)
        ela_std = np.std(ela_enhanced_array)

        manipulation_score = min((ela_mean / 255.0) * 2.5 + (ela_std / 255.0) * 1.5, 1.0)
        return float(manipulation_score)

    except Exception:
        return 0.5


def run_opencv_analysis(image_bytes: bytes) -> float:
    """OpenCV-based structural analysis"""
    try:
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            return 0.5

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

        laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
        edges = cv2.Canny(gray, 100, 200)
        edge_density = np.sum(edges > 0) / edges.size

        noise_score = 1.0 - min(laplacian_var / 500.0, 1.0)
        edge_score = abs(edge_density - 0.08) * 5.0

        structural_score = (noise_score * 0.6 + min(edge_score, 1.0) * 0.4)
        return float(min(structural_score, 1.0))

    except Exception:
        return 0.5


def detect_image(image_bytes: bytes, filename: str) -> dict:
    """Main image detection function"""
    start_time = time.time()

    ela_score = run_ela_analysis(image_bytes)
    opencv_score = run_opencv_analysis(image_bytes)

    fake_probability = round((ela_score * 0.6 + opencv_score * 0.4), 4)
    fake_probability = max(0.05, min(fake_probability, 0.95))
    real_probability = round(1.0 - fake_probability, 4)

    prediction = "fake" if fake_probability > 0.5 else "real"
    confidence = round(max(fake_probability, real_probability), 4)

    try:
        img = Image.open(io.BytesIO(image_bytes))
        dimensions = list(img.size)
    except Exception:
        dimensions = [0, 0]

    processing_time = round(time.time() - start_time, 3)
    file_hash = hashlib.md5(image_bytes).hexdigest()

    return {
        "prediction": prediction,
        "confidence": confidence,
        "fake_probability": fake_probability,
        "real_probability": real_probability,
        "processing_time": processing_time,
        "file_info": {
            "filename": filename,
            "size": len(image_bytes),
            "dimensions": dimensions,
            "hash": file_hash,
            "analysis_methods": ["ELA", "OpenCV_Structural"]
        }
    }
