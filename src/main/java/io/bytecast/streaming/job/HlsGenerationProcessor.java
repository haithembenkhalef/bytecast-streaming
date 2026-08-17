package io.bytecast.streaming.job;

import io.bytecast.streaming.service.FFmpegService;
import io.bytecast.streaming.storage.VideoStorage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class HlsGenerationProcessor implements VideoProcessor<HlsGenerationResult> {

    public static String TEMP_VIDEO_NAME = "video";
    public static String TEMP_HLS_DIRECTORY = "hls-%s";


    @Inject
    VideoStorage storage;

    @Inject
    FFmpegService ffmpegService;

    @Override
    public HlsGenerationResult process(String videoId, Map<String, Object> context) {

        Path workingDirectory = null;

        String objectName = VideoStorage.buildVideoObjectKey(videoId);

        try {
            workingDirectory = Files.createTempDirectory(String.format(TEMP_HLS_DIRECTORY, videoId));

            Path inputFile = workingDirectory.resolve(TEMP_VIDEO_NAME);

            // Download original video
            try (InputStream in = storage.download(objectName)) {
                Files.copy(in, inputFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Generate HLS
            HlsOutput output = ffmpegService.generateHls(inputFile, workingDirectory);

            // Upload playlist and segments
            String playlistObjectKey = storage.uploadHls(videoId, workingDirectory);

            List<String> segmentKeys = output.segments().stream()
                    .flatMap(rendition -> rendition.segments().stream())
                    .map(Path::toString)
                    .toList();

            return new HlsGenerationResult(
                    playlistObjectKey,
                    segmentKeys,
                    output.segments().size());

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HLS for video " + videoId, e);

        } catch (Exception e) {
            throw new RuntimeException(e);

        } finally {
            if (workingDirectory != null) {
                FileUtils.deleteQuietly(workingDirectory.toFile());
            }
        }
    }
}