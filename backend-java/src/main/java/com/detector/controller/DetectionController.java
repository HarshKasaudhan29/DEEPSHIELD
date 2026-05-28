package com.detector.controller;

import com.detector.dto.TaskStatusDto;
import com.detector.service.AsyncDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/detect")
public class DetectionController {

    private final AsyncDetectionService asyncDetectionService;

    public DetectionController(AsyncDetectionService asyncDetectionService) {
        this.asyncDetectionService = asyncDetectionService;
    }

    // ── Image Upload ──────────────────────────────────────────

    @PostMapping("/image")
    public ResponseEntity<?> detectImage(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return handleUpload(file, "IMAGE", auth);
    }

    // ── Audio Upload ──────────────────────────────────────────

    @PostMapping("/audio")
    public ResponseEntity<?> detectAudio(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return handleUpload(file, "AUDIO", auth);
    }

    // ── Video Upload ──────────────────────────────────────────

    @PostMapping("/video")
    public ResponseEntity<?> detectVideo(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return handleUpload(file, "VIDEO", auth);
    }

    // ── Shared handler ────────────────────────────────────────

    /**
     * 1. Generate unique taskId
     * 2. Return PROCESSING_STARTED immediately (non-blocking)
     * 3. Hand off to async thread — result delivered via WebSocket
     */
    private ResponseEntity<?> handleUpload(MultipartFile file,
                                            String fileType,
                                            Authentication auth) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No file provided"));
        }

        String userId = (auth != null) ? auth.getName() : "anonymous";
        String taskId = UUID.randomUUID().toString();

        // Fire-and-forget: async thread handles ML + WebSocket broadcast
        asyncDetectionService.processFileAsync(taskId, userId, fileType, file);

        // Return immediately — frontend listens on WebSocket for progress
        return ResponseEntity.accepted().body(Map.of(
                "taskId",   taskId,
                "status",   TaskStatusDto.PROCESSING_STARTED,
                "message",  "File accepted. Connect to WebSocket for real-time updates.",
                "wsChannel", "/user/" + userId + "/queue/task-progress"
        ));
    }
}
