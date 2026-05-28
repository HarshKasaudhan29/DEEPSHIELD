"""Synthetic Media Detector FastAPI ML service."""

from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import logging
import time

from app.detection.image_detector import detect_image
from app.detection.audio_detector import detect_audio
from app.detection.video_detector import detect_video_async

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Synthetic Media Detector ML Service",
    description="AI-powered deepfake detection for images, audio, and video.",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def startup_event():
    logger.info("Synthetic Media Detector ML Service started on port 8000")

@app.on_event("shutdown")
async def shutdown_event():
    logger.info("ML Service shutting down...")

@app.get("/health")
async def health_check():
    return {
        "status":  "UP",
        "service": "ml-detection-service",
        "version": "1.0.0",
        "timestamp": time.time()
    }

@app.get("/")
async def root():
    return {
        "message": "Synthetic Media Detector ML Service",
        "docs":    "/docs",
        "health":  "/health"
    }

@app.post("/api/detect/image")
async def detect_image_endpoint(file: UploadFile = File(...)):
    """
    Detect AI-generated or manipulated images.
    Uses ELA (Error Level Analysis) + OpenCV structural analysis.
    """
    # Validate file type
    allowed = {"image/jpeg", "image/png", "image/gif", "image/webp"}
    if file.content_type and file.content_type not in allowed:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported file type: {file.content_type}. Allowed: JPEG, PNG, GIF, WEBP"
        )

    contents = await file.read()

    if len(contents) == 0:
        raise HTTPException(status_code=400, detail="Empty file received.")

    if len(contents) > 100 * 1024 * 1024:  # 100MB
        raise HTTPException(status_code=413, detail="File too large. Max 100MB.")

    logger.info(f"Image detection: {file.filename} ({len(contents)} bytes)")

    result = detect_image(contents, file.filename or "unknown.jpg")

    logger.info(f"Image result: prediction={result['prediction']} confidence={result['confidence']}")
    return result

@app.post("/api/detect/audio")
async def detect_audio_endpoint(file: UploadFile = File(...)):
    """
    Detect synthetic or AI-generated audio/voice.
    Uses Librosa MFCC and spectral anomaly analysis.
    """
    allowed = {"audio/mpeg", "audio/wav", "audio/flac", "audio/mp4",
               "audio/x-wav", "audio/x-flac", "audio/m4a"}
    if file.content_type and file.content_type not in allowed:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported file type: {file.content_type}. Allowed: MP3, WAV, FLAC, M4A"
        )

    contents = await file.read()

    if len(contents) == 0:
        raise HTTPException(status_code=400, detail="Empty file received.")

    if len(contents) > 100 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large. Max 100MB.")

    logger.info(f"Audio detection: {file.filename} ({len(contents)} bytes)")

    result = detect_audio(contents, file.filename or "unknown.mp3")

    logger.info(f"Audio result: prediction={result['prediction']} confidence={result['confidence']}")
    return result

@app.post("/api/detect/video")
async def detect_video_endpoint(file: UploadFile = File(...)):
    """
    Detect deepfake or AI-generated video.
    Samples 8 frames, runs image analysis + temporal consistency check.
    """
    allowed = {"video/mp4", "video/avi", "video/quicktime",
               "video/x-matroska", "video/x-msvideo"}
    if file.content_type and file.content_type not in allowed:
        raise HTTPException(
            status_code=415,
            detail=f"Unsupported file type: {file.content_type}. Allowed: MP4, AVI, MOV, MKV"
        )

    contents = await file.read()

    if len(contents) == 0:
        raise HTTPException(status_code=400, detail="Empty file received.")

    if len(contents) > 100 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="File too large. Max 100MB.")

    logger.info(f"Video detection: {file.filename} ({len(contents)} bytes)")

    result = await detect_video_async(contents, file.filename or "unknown.mp4")

    logger.info(f"Video result: prediction={result['prediction']} confidence={result['confidence']}")
    return result

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=False,
        workers=2,
        log_level="info"
    )
