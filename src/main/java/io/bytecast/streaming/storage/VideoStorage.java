package io.bytecast.streaming.storage;

import io.bytecast.streaming.config.Config;
import io.bytecast.streaming.exception.StreamingErrorCode;
import io.bytecast.streaming.exception.StreamingException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static io.bytecast.streaming.exception.StreamingErrorCode.STORAGE_ERROR_UNKNOWN;

@ApplicationScoped
@Slf4j
public class VideoStorage {

    private static final String VIDEO_DIRECTORY = "videos";
    private static final String ORIGINAL = "original";

    @Inject
    MinioClient minioClient;

    @Inject
    private Config config;

    public PresignedPostResponse preSignedUrl(String videoId) {
        String bucket = config.bucket();
        String objectName = buildVideoObjectKey(videoId);

        PostPolicy policy = new PostPolicy(bucket, ZonedDateTime.now().plusMinutes(15));
        policy.addContentLengthRangeCondition(1024, 10L * 1024 * 1024 * 1024); // 1KB–10GB
        policy.addStartsWithCondition("key", objectName);
       // policy.addStartsWithCondition("type", "video/");
        try {
            Map<String, String> formData = minioClient.getPresignedPostFormData(policy);
            //String uploadUrl = config.minioEndpoint() + "/" + bucket;
            return new PresignedPostResponse("", formData);
        } catch (Exception e) {
            throw new StreamingException(STORAGE_ERROR_UNKNOWN, String.format("Unable to generate upload URL for videoId=%s", videoId));
        }
    }

    public InputStream download(String objectKey) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(config.bucket())
                        .object(objectKey)
                        .build()
        );
    }

    public VideoStream downloadSegment(String videoId, long start, long length) {
        String bucket = config.bucket();
        AtomicLong transferred = new AtomicLong();
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucket)
                .object(videoId)
                .offset(start)
                .length(length)
                .build();

        log.debug("Opening MinIO stream | videoId={}, offset={}, length={}",
                videoId, start, length);
        StreamingOutput stream = output -> {
            try (InputStream in = minioClient.getObject(args)) {
                transferred.set(in.transferTo(output));
                log.debug("Stream complete | bytesTransferred={}", transferred);
            } catch (Exception e) {
                log.error("Stream error | videoId={}, offset={}, error={}",
                        videoId, start, e.getMessage(), e);
                throw new RuntimeException("Stream error", e);
            }
        };
        return new VideoStream(stream, transferred.get());
    }

    public String uploadHls(String videoId, Path hlsDirectory) throws Exception {

        String prefix = "videos/" + videoId + "/hls/";

        try (Stream<Path> files = Files.list(hlsDirectory)) {

            for (Path file : files.toList()) {

                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String objectKey = prefix + file.getFileName();

                minioClient.uploadObject(
                        UploadObjectArgs.builder()
                                .bucket(config.bucket())
                                .object(objectKey)
                                .filename(file.toString())
                                .build());
            }
        }
        return prefix;
    }

    @CacheResult(cacheName = "video-metadata")
    public VideoMetadata getMetadata(String videoId) throws Exception {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(config.bucket()).object(videoId).build()
            );
            return new VideoMetadata(stat.size(), stat.contentType());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new StreamingException(StreamingErrorCode.VIDEO_NOT_FOUND, videoId);
            }
            throw e;
        }
    }

    public static String buildVideoObjectKey(String videoId) {
        return String.format("%s/%s/%s", VIDEO_DIRECTORY, videoId, ORIGINAL);
    }
}
