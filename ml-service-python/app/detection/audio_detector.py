import librosa
import numpy as np
import io
import time
import hashlib


def extract_audio_features(audio_bytes: bytes) -> dict:
    """Extract MFCC and spectral features using librosa"""
    try:
        audio_file = io.BytesIO(audio_bytes)
        y, sr = librosa.load(audio_file, sr=None, duration=30)

        mfccs = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=13)
        mfcc_mean = np.mean(mfccs, axis=1)
        mfcc_std = np.std(mfccs, axis=1)

        spectral_centroid = np.mean(librosa.feature.spectral_centroid(y=y, sr=sr))
        spectral_rolloff = np.mean(librosa.feature.spectral_rolloff(y=y, sr=sr))
        spectral_bandwidth = np.mean(librosa.feature.spectral_bandwidth(y=y, sr=sr))
        zero_crossing_rate = np.mean(librosa.feature.zero_crossing_rate(y))
        rms = np.mean(librosa.feature.rms(y=y))

        return {
            "mfcc_mean": mfcc_mean.tolist(),
            "mfcc_std": mfcc_std.tolist(),
            "spectral_centroid": float(spectral_centroid),
            "spectral_rolloff": float(spectral_rolloff),
            "spectral_bandwidth": float(spectral_bandwidth),
            "zero_crossing_rate": float(zero_crossing_rate),
            "rms_energy": float(rms),
            "sample_rate": sr,
            "duration": float(len(y) / sr)
        }

    except Exception:
        return None


def analyze_audio_anomalies(features: dict) -> float:
    """Score audio for deepfake indicators based on feature anomalies"""
    if features is None:
        return 0.5

    anomaly_score = 0.0
    checks = 0

    # Check 1: MFCC variance — synthetic voices are too uniform
    mfcc_std_mean = np.mean(features["mfcc_std"])
    if mfcc_std_mean < 8.0:
        anomaly_score += 0.7
    elif mfcc_std_mean < 15.0:
        anomaly_score += 0.3
    checks += 1

    # Check 2: Zero crossing rate — TTS often too regular
    zcr = features["zero_crossing_rate"]
    if zcr < 0.02 or zcr > 0.25:
        anomaly_score += 0.5
    checks += 1

    # Check 3: RMS energy consistency
    rms = features["rms_energy"]
    if rms < 0.001:
        anomaly_score += 0.6
    checks += 1

    # Check 4: Spectral bandwidth (TTS has narrow bandwidth)
    bandwidth = features["spectral_bandwidth"]
    normalized_bw = bandwidth / (features["sample_rate"] / 2)
    if normalized_bw < 0.15:
        anomaly_score += 0.5
    checks += 1

    fake_probability = min(anomaly_score / checks, 1.0)
    return round(float(fake_probability), 4)


def detect_audio(audio_bytes: bytes, filename: str) -> dict:
    """Main audio detection function"""
    start_time = time.time()

    features = extract_audio_features(audio_bytes)
    fake_probability = analyze_audio_anomalies(features)
    fake_probability = max(0.05, min(fake_probability, 0.95))
    real_probability = round(1.0 - fake_probability, 4)

    prediction = "fake" if fake_probability > 0.5 else "real"
    confidence = round(max(fake_probability, real_probability), 4)

    processing_time = round(time.time() - start_time, 3)
    file_hash = hashlib.md5(audio_bytes).hexdigest()

    duration = features["duration"] if features else 0.0
    sample_rate = features["sample_rate"] if features else 0

    return {
        "prediction": prediction,
        "confidence": confidence,
        "fake_probability": fake_probability,
        "real_probability": real_probability,
        "processing_time": processing_time,
        "file_info": {
            "filename": filename,
            "size": len(audio_bytes),
            "duration_seconds": round(duration, 2),
            "sample_rate": sample_rate,
            "hash": file_hash,
            "analysis_methods": ["MFCC_Analysis", "Spectral_Anomaly_Detection"]
        }
    }
