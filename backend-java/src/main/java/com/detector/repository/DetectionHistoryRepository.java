package com.detector.repository;

import com.detector.entity.DetectionHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetectionHistoryRepository extends MongoRepository<DetectionHistory, String> {

    // Fetch all scans for a user, newest first
    List<DetectionHistory> findByUserIdOrderByTimestampDesc(String userId);

    // Fetch scans by file type for a user
    List<DetectionHistory> findByUserIdAndFileTypeOrderByTimestampDesc(String userId, String fileType);

    // Fetch scans between date range
    List<DetectionHistory> findByUserIdAndTimestampBetween(
            String userId, LocalDateTime start, LocalDateTime end);

    // Count total scans by user
    long countByUserId(String userId);

    // Count fake detections by user
    long countByUserIdAndPrediction(String userId, String prediction);
}
