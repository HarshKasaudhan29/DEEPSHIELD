import cv2
import numpy as np
import tempfile
import os
import time
import hashlib
import asyncio
from concurrent.futures import ThreadPoolExecutor
from .image_detector import detect_image

executor = ThreadPoolExecutor(max_workers=4)


def sample_video_frames(video_bytes: bytes, num_frames: int = 8):
    """Sample frames from video using OpenCV"""
    frames = []

    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=False) as tmp:
        tmp.write(video_bytes)
        tmp_path = tmp.name

    try:
        cap = cv2.VideoCapture(tmp_path)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        fps = cap.get(cv2.CAP_PROP_FPS)
        duration = total_frames / fps if fps > 0 else 0

        if total_frames == 0:
            return frames, 0, 0

        frame_indices = np.linspace(0, total_frames - 1, num_frames, dtype=int)

        for idx in frame_indices:
            cap.set(cv2.CAP_PROP_POS_FRAMES, idx)
            ret, frame = cap.read()
            if ret:
                _, buffer = cv2.imencode(".jpg", frame)
                frames.append(buffer.tobytes())

        cap.release()
        return frames, fps, duration

    except Exception:
        return [], 0, 0

    finally:
        os.unlink(tmp_path)


def analyze_temporal_consistency(frames_bytes: list) -> float:
    """Check frame-to-frame consistency — deepfakes often have temporal glitches"""
    if len(frames_bytes) < 2:
        return 0.5

    try:
        prev_frame = None
        diffs = []

        for frame_bytes in frames_bytes:
            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_GRAYSCALE)

            if frame is None:
                continue

            frame = cv2.resize(frame, (128, 128))

            if prev_frame is not None:
                diff = np.mean(np.abs(frame.astype(float) - prev_frame.astype(float)))
                diffs.append(diff)

            prev_frame = frame

        if not diffs:
            return 0.5

        diff_variance = np.std(diffs)
        diff_mean = np.mean(diffs)

        if diff_mean < 2.0:
            temporal_score = 0.7
        elif diff_variance > 20.0:
            temporal_score = 0.6
        else:
            temporal_score = 0.2

        return float(temporal_score)

    except Exception:
        return 0.5


async def detect_video_async(video_bytes: bytes, filename: str) -> dict:
    """Async video detection with frame sampling"""
    start_time = time.time()

    loop = asyncio.get_running_loop()

    frames, fps, duration = await loop.run_in_executor(
        executor, sample_video_frames, video_bytes, 8
    )

    if not frames:
        return {
            "prediction": "unknown",
            "confidence": 0.5,
            "fake_probability": 0.5,
            "real_probability": 0.5,
            "processing_time": round(time.time() - start_time, 3),
            "file_info": {
                "filename": filename,
                "size": len(video_bytes),
                "error": "Could not extract frames"
            }
        }

    frame_tasks = [
        loop.run_in_executor(executor, detect_image, frame, f"frame_{i}.jpg")
        for i, frame in enumerate(frames)
    ]
    frame_results = await asyncio.gather(*frame_tasks)

    temporal_score = await loop.run_in_executor(
        executor, analyze_temporal_consistency, frames
    )

    frame_fake_probs = [r["fake_probability"] for r in frame_results]
    avg_frame_score = np.mean(frame_fake_probs)

    fake_probability = round((avg_frame_score * 0.7 + temporal_score * 0.3), 4)
    fake_probability = max(0.05, min(fake_probability, 0.95))
    real_probability = round(1.0 - fake_probability, 4)

    prediction = "fake" if fake_probability > 0.5 else "real"
    confidence = round(max(fake_probability, real_probability), 4)

    processing_time = round(time.time() - start_time, 3)
    file_hash = hashlib.md5(video_bytes[:1024]).hexdigest()

    return {
        "prediction": prediction,
        "confidence": confidence,
        "fake_probability": fake_probability,
        "real_probability": real_probability,
        "processing_time": processing_time,
        "file_info": {
            "filename": filename,
            "size": len(video_bytes),
            "fps": round(fps, 2),
            "duration_seconds": round(duration, 2),
            "frames_analyzed": len(frames),
            "hash": file_hash,
            "analysis_methods": ["Frame_ELA", "OpenCV_Structural", "Temporal_Consistency"]
        }
    }
