package com.detector.dto;

import java.time.LocalDateTime;

public class TaskStatusDto {

    // Status constants
    public static final String PROCESSING_STARTED   = "PROCESSING_STARTED";
    public static final String FRAME_EXTRACTED      = "FRAME_EXTRACTED";
    public static final String ML_ANALYSIS_RUNNING  = "ML_ANALYSIS_RUNNING";
    public static final String SAVING_RESULT        = "SAVING_RESULT";
    public static final String COMPLETED            = "COMPLETED";
    public static final String FAILED               = "FAILED";

    private String taskId;
    private String status;
    private String message;
    private Integer progressPercent;   // 0-100
    private Object result;             // final DetectionResultDto when COMPLETED
    private String errorMessage;       // when FAILED
    private LocalDateTime timestamp;

    // ── Constructors ──────────────────────────────────────────

    public TaskStatusDto() {}

    public TaskStatusDto(String taskId, String status, String message, int progress) {
        this.taskId          = taskId;
        this.status          = status;
        this.message         = message;
        this.progressPercent = progress;
        this.timestamp       = LocalDateTime.now();
    }

    // ── Static factory helpers ────────────────────────────────

    public static TaskStatusDto started(String taskId, String filename) {
        return new TaskStatusDto(taskId, PROCESSING_STARTED,
                "Started processing: " + filename, 5);
    }

    public static TaskStatusDto mlRunning(String taskId) {
        return new TaskStatusDto(taskId, ML_ANALYSIS_RUNNING,
                "Running ML analysis...", 50);
    }

    public static TaskStatusDto saving(String taskId) {
        return new TaskStatusDto(taskId, SAVING_RESULT,
                "Saving result to history...", 85);
    }

    public static TaskStatusDto completed(String taskId, Object result) {
        TaskStatusDto dto = new TaskStatusDto(taskId, COMPLETED,
                "Analysis complete", 100);
        dto.setResult(result);
        return dto;
    }

    public static TaskStatusDto failed(String taskId, String error) {
        TaskStatusDto dto = new TaskStatusDto(taskId, FAILED,
                "Processing failed", 0);
        dto.setErrorMessage(error);
        return dto;
    }

    // ── Getters & Setters ─────────────────────────────────────

    public String getTaskId()               { return taskId; }
    public void   setTaskId(String t)       { this.taskId = t; }

    public String getStatus()               { return status; }
    public void   setStatus(String s)       { this.status = s; }

    public String getMessage()              { return message; }
    public void   setMessage(String m)      { this.message = m; }

    public Integer getProgressPercent()     { return progressPercent; }
    public void    setProgressPercent(Integer p) { this.progressPercent = p; }

    public Object getResult()               { return result; }
    public void   setResult(Object r)       { this.result = r; }

    public String getErrorMessage()         { return errorMessage; }
    public void   setErrorMessage(String e) { this.errorMessage = e; }

    public LocalDateTime getTimestamp()     { return timestamp; }
    public void          setTimestamp(LocalDateTime t) { this.timestamp = t; }
}
