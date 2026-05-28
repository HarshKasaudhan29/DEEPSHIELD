package com.detector.service;

import com.detector.dto.DetectionResultDto;
import com.detector.entity.DetectionHistory;
import com.detector.repository.DetectionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DetectionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(DetectionHistoryService.class);

    private final DetectionHistoryRepository historyRepository;

    public DetectionHistoryService(DetectionHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    // ── Async Save ────────────────────────────────────────────

    /**
     * Asynchronously saves a scan result to MongoDB.
     * Non-blocking — controller returns response immediately.
     *
     * @param userId   authenticated user's ID (or "anonymous")
     * @param fileType IMAGE / AUDIO / VIDEO
     * @param result   DetectionResultDto from FastAPI ML service
     */
    public DetectionHistory saveScanResult(
            String userId,
            String fileType,
            DetectionResultDto result) {

        try {
            DetectionHistory history = new DetectionHistory(
                    userId,
                    result.extractFilename(),
                    fileType.toUpperCase(),
                    result.getPrediction(),
                    result.getConfidence(),
                    result.getFakeProbability(),
                    result.getRealProbability(),
                    result.getProcessingTime(),
                    result.extractFileSize()
            );

            DetectionHistory saved = historyRepository.save(history);
            log.info("Scan saved to MongoDB — id={} user={} prediction={}",
                    saved.getId(), userId, saved.getPrediction());

            return CompletableFuture.completedFuture(saved);

        } catch (Exception e) {
            log.error("Failed to save scan history for user={}: {}", userId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    // ── Read Operations ───────────────────────────────────────

    /**
     * Fetch all scan history for a user, newest first.
     */
    public List<DetectionHistory> getUserHistory(String userId) {
        return historyRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Fetch a single scan record by ID.
     */
    public Optional<DetectionHistory> getScanById(String id) {
        return historyRepository.findById(id);
    }

    /**
     * Fetch history filtered by file type.
     */
    public List<DetectionHistory> getUserHistoryByType(String userId, String fileType) {
        return historyRepository.findByUserIdAndFileTypeOrderByTimestampDesc(
                userId, fileType.toUpperCase());
    }

    /**
     * Stats summary for a user.
     */
    public ScanStats getUserStats(String userId) {
        long total = historyRepository.countByUserId(userId);
        long fakeCount = historyRepository.countByUserIdAndPrediction(userId, "fake");
        long realCount = historyRepository.countByUserIdAndPrediction(userId, "real");
        return new ScanStats(total, fakeCount, realCount);
    }

    // ── Inner Stats DTO ───────────────────────────────────────

    public record ScanStats(long total, long fakeDetected, long realDetected) {}
}
