package io.bytecast.streaming.job;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoProcessingJob {
    String id;
    String videoId;
    JobStatus status;
    Instant createdAt;
    Instant updatedAt;
    Instant startedAt;
    Instant finishedAt;
    String errorMessage;
    Object result;

    public VideoProcessingJob(String id, String videoId, JobStatus status, Instant createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.status = status;
        this.createdAt = createdAt;
    }
}