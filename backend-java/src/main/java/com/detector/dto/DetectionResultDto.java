package com.detector.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class DetectionResultDto {

    private String scanId;
    private String prediction;
    private Double confidence;

    @JsonProperty("fake_probability")
    private Double fakeProbability;

    @JsonProperty("real_probability")
    private Double realProbability;

    @JsonProperty("processing_time")
    private Double processingTime;

    @JsonProperty("file_info")
    private Map<String, Object> fileInfo;

    // ── Getters & Setters ─────────────────────────────────────

    public String getScanId() { return scanId; }
    public void setScanId(String scanId) { this.scanId = scanId; }

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Double getFakeProbability() { return fakeProbability; }
    public void setFakeProbability(Double fakeProbability) { this.fakeProbability = fakeProbability; }

    public Double getRealProbability() { return realProbability; }
    public void setRealProbability(Double realProbability) { this.realProbability = realProbability; }

    public Double getProcessingTime() { return processingTime; }
    public void setProcessingTime(Double processingTime) { this.processingTime = processingTime; }

    public Map<String, Object> getFileInfo() { return fileInfo; }
    public void setFileInfo(Map<String, Object> fileInfo) { this.fileInfo = fileInfo; }

    // Helper: extract filename from file_info map
    public String extractFilename() {
        if (fileInfo != null && fileInfo.containsKey("filename")) {
            return fileInfo.get("filename").toString();
        }
        return "unknown";
    }

    // Helper: extract file size from file_info map
    public Long extractFileSize() {
        if (fileInfo != null && fileInfo.containsKey("size")) {
            return Long.parseLong(fileInfo.get("size").toString());
        }
        return 0L;
    }
}
