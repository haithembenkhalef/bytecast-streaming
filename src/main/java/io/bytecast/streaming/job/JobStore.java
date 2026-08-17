package io.bytecast.streaming.job;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class JobStore {

    private final Map<String, VideoProcessingJob> jobs = new ConcurrentHashMap<>();

    public void save(VideoProcessingJob job) {
        jobs.put(job.getId(), job);
    }

    public Optional<VideoProcessingJob> findById(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public void updateStatus(String jobId, JobStatus status) {
        VideoProcessingJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(status);
            job.setUpdatedAt(Instant.now());
        }
    }

    public void complete(String jobId, Object result) {
        VideoProcessingJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(JobStatus.COMPLETED);
            job.setResult(result);
            job.setUpdatedAt(Instant.now());
        }
    }

    public void fail(String jobId, String errorMessage) {
        VideoProcessingJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setUpdatedAt(Instant.now());
        }
    }
}