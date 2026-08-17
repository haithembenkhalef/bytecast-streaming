package io.bytecast.streaming.service;

import io.bytecast.streaming.exception.StreamingException;
import io.bytecast.streaming.job.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.bytecast.streaming.exception.StreamingErrorCode.JOB_NOT_FOUND;

@ApplicationScoped
public class VideoProcessingService {

    @Inject
    HlsGenerationProcessor processor;

    @Inject
    JobStore jobStore;

    @Inject
    VideoProcessingWorker worker;

    public VideoProcessingJob submitHlsGeneration(String videoId, Map<String, Object> context) {
        String jobId = UUID.randomUUID().toString();
        VideoProcessingJob processingJob = new VideoProcessingJob(jobId, videoId, JobStatus.PENDING, Instant.now());
        jobStore.save(processingJob);
        worker.execute(() -> runJob(jobId, videoId, context));
        return processingJob;
    }

    public VideoProcessingJob getJob(String jobId) {
        return jobStore.findById(jobId).orElseThrow(() -> new StreamingException(JOB_NOT_FOUND, jobId));
    }

    private void runJob(String jobId, String videoId, Map<String, Object> context) {
        jobStore.updateStatus(jobId, JobStatus.RUNNING);

        try {
            HlsGenerationResult result = processor.process(videoId, context);
            jobStore.complete(jobId, result);
        } catch (Exception e) {
            jobStore.fail(jobId, e.getMessage());
        }
    }
}