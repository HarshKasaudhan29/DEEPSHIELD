package com.detector.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "detection_history")
public class DetectionHistory {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String filename;
    private String fileType;         // IMAGE, AUDIO, VIDEO
    private String prediction;       // real / fake
    private Double confidenceScore;
    private Double fakeProbability;
    private Double realProbability;
    private Double processingTime;
    private Long fileSize;

    private LocalDateTime timestamp;
    private String jsonReportUrl;    // optional: path/URL to stored JSON

    // ── Constructors ──────────────────────────────────────────

    public DetectionHistory() {}

    public DetectionHistory(String userId, String filename, String fileType,
                            String prediction, Double confidenceScore,
                            Double fakeProbability, Double realProbability,
                            Double processingTime, Long fileSize) {
        this.userId = userId;
        this.filename = filename;
        this.fileType = fileType;
        this.prediction = prediction;
        this.confidenceScore = confidenceScore;
        this.fakeProbability = fakeProbability;
        this.realProbability = realProbability;
        this.processingTime = processingTime;
        this.fileSize = fileSize;
        this.timestamp = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getPrediction() { return prediction; }
    public void setPrediction(String prediction) { this.prediction = prediction; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Double getFakeProbability() { return fakeProbability; }
    public void setFakeProbability(Double fakeProbability) { this.fakeProbability = fakeProbability; }

    public Double getRealProbability() { return realProbability; }
    public void setRealProbability(Double realProbability) { this.realProbability = realProbability; }

    public Double getProcessingTime() { return processingTime; }
    public void setProcessingTime(Double processingTime) { this.processingTime = processingTime; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getJsonReportUrl() { return jsonReportUrl; }
    public void setJsonReportUrl(String jsonReportUrl) { this.jsonReportUrl = jsonReportUrl; }
}
