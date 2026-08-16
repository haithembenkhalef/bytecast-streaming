package io.bytecast.streaming.service;

import io.bytecast.streaming.job.HlsGenerationProcessor;
import io.bytecast.streaming.job.JobStatus;
import io.bytecast.streaming.job.VideoProcessingJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class VideoProcessingService {

    @Inject
    HlsGenerationProcessor processor;

    @Inject
    VideoProcessingWorker worker;

    public VideoProcessingJob submitHlsGeneration(String videoId, Map<String, Object> context) {
        String jobId = UUID.randomUUID().toString();
        VideoProcessingJob processingJob = new VideoProcessingJob(jobId, videoId, JobStatus.PENDING, Instant.now(), null, null, null);
        worker.execute(() -> processor.process(videoId, context));
        return processingJob;
    }
}