package io.bytecast.streaming.service;

import io.bytecast.streaming.exception.StreamingErrorCode;
import io.bytecast.streaming.exception.StreamingException;
import io.bytecast.streaming.storage.HlsFile;
import io.bytecast.streaming.storage.VideoData;
import io.bytecast.streaming.storage.VideoMetadata;
import io.bytecast.streaming.storage.VideoStorage;
import io.bytecast.streaming.storage.VideoStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Slf4j
public class VideoStreamService {

    private static final Pattern RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");
    private static final long DEFAULT_CHUNK_SIZE = 1024 * 1024 * 8;

    @Inject
    VideoStorage videoStorage;

    public HlsFile getMasterM3u8(String videoId) throws Exception {
        VideoMetadata metadata = videoStorage.getMetadataHlsMaster(videoId);

        long fileSize = metadata.size();
        String contentType = metadata.contentType();
        log.debug("MinIO stat | videoId={}, fileSize={}, contentType={}", videoId, fileSize, contentType);

        return videoStorage.downloadMasterM3u8(videoId);
    }

    public VideoData getSegment(String videoId, String rangeHeader) throws Exception {
        if (rangeHeader == null) {
            log.debug("No Range header present, returning 200 with Accept-Ranges");
            throw new StreamingException(
                    StreamingErrorCode.RANGE_MALFORMED,
                    videoId,
                    "header=" + null
            );
        }

        VideoMetadata metadata = videoStorage.getMetadata(videoId);
        long fileSize = metadata.size();
        String contentType = metadata.contentType();
        log.debug("MinIO stat | videoId={}, fileSize={}, contentType={}", videoId, fileSize, contentType);

        long start = 0;
        long end = 0;

        Matcher matcher = RANGE_PATTERN.matcher(rangeHeader);
        if (matcher.matches()) {
            String startStr = matcher.group(1);
            String endStr = matcher.group(2);

            if (!startStr.isBlank()) {
                start = Long.parseLong(startStr);
                log.debug("Parsed range start | start={}", start);
            }

            if (!endStr.isBlank()) {
                end = Long.parseLong(endStr);
                log.debug("Parsed range end | end={}", end);
            }
            else
                end = Math.min(start + DEFAULT_CHUNK_SIZE - 1, fileSize - 1);
        } else {
            log.warn("Range header did not match expected pattern | rangeHeader={}, default values", rangeHeader);
        }

        if (start < 0 || start > end || start >= fileSize) {
            log.warn("Invalid range requested | start={}, end={}, fileSize={}", start, end, fileSize);
            throw new StreamingException(
                    StreamingErrorCode.RANGE_UNSATISFIABLE,
                    videoId,
                    "start=" + start + ", end=" + end + ", size=" + fileSize,
                    Map.of("Content-Range", "bytes */" + fileSize)
            );
        }

        long finalStart = start;
        long contentLength = end - finalStart + 1;

        log.debug("Serving chunk | videoId={}, range={}-{}, contentLength={}, fileSize={}",
                videoId, finalStart, end, contentLength, fileSize);

        VideoStream videoStream = videoStorage.downloadSegment(videoId, start, contentLength);
        return new VideoData(videoStream.stream(), videoStream.transferred(), finalStart, end, contentLength, fileSize, contentType);
    }
}
