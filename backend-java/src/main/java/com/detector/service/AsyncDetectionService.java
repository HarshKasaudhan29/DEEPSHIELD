package com.detector.service;

import com.detector.dto.DetectionResultDto;
import com.detector.dto.TaskStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AsyncDetectionService.class);

    // WebSocket template — broadcasts messages to subscribers
    private final SimpMessagingTemplate messagingTemplate;
    private final DetectionHistoryService historyService;
    private final WebClient webClient;

    // ML service endpoint map
    private static final String IMAGE_ENDPOINT = "/api/detect/image";
    private static final String AUDIO_ENDPOINT = "/api/detect/audio";
    private static final String VIDEO_ENDPOINT = "/api/detect/video";

    public AsyncDetectionService(SimpMessagingTemplate messagingTemplate,
                                 DetectionHistoryService historyService,
                                 WebClient.Builder webClientBuilder,
                                 @Value("${ml.service.url}") String mlServiceUrl) {
        this.messagingTemplate = messagingTemplate;
        this.historyService    = historyService;
        this.webClient         = webClientBuilder
                .baseUrl(mlServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(200 * 1024 * 1024)) // 200MB
                .build();
    }

    // ── Public entry point ────────────────────────────────────

    /**
     * Async task: processes file, broadcasts progress via WebSocket,
     * saves result to MongoDB.
     *
     * @param taskId   unique task identifier (returned immediately to client)
     * @param userId   authenticated user
     * @param fileType IMAGE | AUDIO | VIDEO
     * @param file     uploaded multipart file
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> processFileAsync(String taskId,
                                                     String userId,
                                                     String fileType,
                                                     MultipartFile file) {
        String channel = "/user/" + userId + "/queue/task-progress";

        try {
            // Step 1 — Notify: started
            broadcast(channel, TaskStatusDto.started(taskId, file.getOriginalFilename()));

            // Step 2 — Notify: ML running
            broadcast(channel, TaskStatusDto.mlRunning(taskId));

            // Step 3 — Call FastAPI ML service
            String mlEndpoint = resolveEndpoint(fileType);
            DetectionResultDto result = callMlService(mlEndpoint, file);

            // Step 4 — Notify: saving
            broadcast(channel, TaskStatusDto.saving(taskId));

            // Step 5 — Async save to MongoDB
            historyService.saveScanResult(userId, fileType, result);

            // Step 6 — Notify: completed with full result
            broadcast(channel, TaskStatusDto.completed(taskId, result));

            log.info("Task {} completed for user {} — prediction={}",
                    taskId, userId, result.getPrediction());

        } catch (Exception e) {
            log.error("Task {} failed for user {}: {}", taskId, userId, e.getMessage());
            broadcast(channel, TaskStatusDto.failed(taskId, e.getMessage()));
            return CompletableFuture.failedFuture(e);
        }

        return CompletableFuture.completedFuture(null);
    }

    // ── Private helpers ───────────────────────────────────────

    private void broadcast(String channel, TaskStatusDto status) {
        try {
            messagingTemplate.convertAndSend(channel, status);
            log.debug("WS broadcast → {} : {}", channel, status.getStatus());
        } catch (Exception e) {
            log.warn("WS broadcast failed: {}", e.getMessage());
        }
    }

    private DetectionResultDto callMlService(String endpoint, MultipartFile file) {
        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            }).contentType(MediaType.APPLICATION_OCTET_STREAM);

            return webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(DetectionResultDto.class)
                    .block();

        } catch (Exception e) {
            throw new RuntimeException("ML service call failed: " + e.getMessage(), e);
        }
    }

    private String resolveEndpoint(String fileType) {
        return switch (fileType.toUpperCase()) {
            case "IMAGE" -> IMAGE_ENDPOINT;
            case "AUDIO" -> AUDIO_ENDPOINT;
            case "VIDEO" -> VIDEO_ENDPOINT;
            default -> throw new IllegalArgumentException("Unknown fileType: " + fileType);
        };
    }
}
