package io.bytecast.streaming.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
public class VideoProcessingJob {
    String id;
    String videoId;
    JobStatus status;
    Instant createdAt;
    Instant startedAt;
    Instant finishedAt;
    String errorMessage;
}