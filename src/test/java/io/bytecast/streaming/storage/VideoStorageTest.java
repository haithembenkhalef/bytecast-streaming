package io.bytecast.streaming.storage;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.bytecast.streaming.job.HlsGenerationProcessor.TEMP_VIDEO_NAME;
import static io.bytecast.streaming.storage.VideoStorage.shouldSkip;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VideoStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsTempVideoFile() throws IOException {
        Path tempVideo = tempDir.resolve("hls-123/");
        Path file = tempVideo.resolve(TEMP_VIDEO_NAME);
        Files.createDirectory(tempVideo);
        Files.createFile(file);

        assertTrue(shouldSkip(file.getFileName()));
    }

    @Test
    void doesNotSkipRegularHlsFile() throws IOException {
        Path playlist = tempDir.resolve("index.m3u8");
        Files.createFile(playlist);

        assertFalse(shouldSkip(playlist));
    }
}
