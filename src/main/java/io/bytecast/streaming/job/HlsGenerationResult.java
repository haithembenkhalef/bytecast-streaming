package io.bytecast.streaming.job;

import java.util.List;

public record HlsGenerationResult(
        String playlistObjectKey,
        List<String> segmentObjectKeys,
        int segmentCount
) {
}