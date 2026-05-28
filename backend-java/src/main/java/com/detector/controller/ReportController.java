package com.detector.controller;

import com.detector.entity.DetectionHistory;
import com.detector.service.DetectionHistoryService;
import com.detector.service.ReportGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportGenerationService reportService;
    private final DetectionHistoryService historyService;

    public ReportController(ReportGenerationService reportService,
                            DetectionHistoryService historyService) {
        this.reportService  = reportService;
        this.historyService = historyService;
    }

    // ── GET /api/reports/download/{id} ────────────────────────

    /**
     * Fetch detection record from MongoDB, generate PDF on-the-fly,
     * stream it back as a downloadable attachment.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> downloadReport(
            @PathVariable String id,
            Authentication auth) {

        // 1. Fetch record from MongoDB
        Optional<DetectionHistory> opt = historyService.getScanById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Report not found for id: " + id));
        }

        DetectionHistory record = opt.get();

        // 2. Security: ensure user can only download their own reports
        if (auth != null && !auth.getName().equals(record.getUserId())
                && !hasAdminRole(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        // 3. Generate PDF bytes
        byte[] pdfBytes;
        try {
            pdfBytes = reportService.generateReport(record);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "PDF generation failed: " + e.getMessage()));
        }

        // 4. Build download filename
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("SMD_Report_%s_%s.pdf",
                sanitizeFilename(record.getFilename()), timestamp);

        // 5. Stream PDF as attachment
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header("X-Report-Id", record.getId())
                .header("X-Prediction", record.getPrediction())
                .body(pdfBytes);
    }

    // ── GET /api/reports/preview/{id} ─────────────────────────

    /**
     * Same as download but opens inline in browser (no attachment header).
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<?> previewReport(
            @PathVariable String id,
            Authentication auth) {

        Optional<DetectionHistory> opt = historyService.getScanById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DetectionHistory record = opt.get();

        if (auth != null && !auth.getName().equals(record.getUserId())
                && !hasAdminRole(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            byte[] pdfBytes = reportService.generateReport(record);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private boolean hasAdminRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "unknown";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                       .replaceAll("\\.[^.]+$", ""); // remove extension
    }
}
