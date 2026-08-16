package io.bytecast.streaming.service;

import io.bytecast.streaming.job.HlsOutput;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
public class FFmpegService {

    public HlsOutput generateHls(Path inputFile, Path outputDirectory)
            throws Exception {

        Path playlist = outputDirectory.resolve("master.m3u8");

        List<String> command = List.of(
                "ffmpeg",
                "-i", inputFile.toString(),
                "-codec:v", "libx264",
                "-codec:a", "aac",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename",
                outputDirectory.resolve("segment_%03d.ts").toString(),
                playlist.toString()
        );

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        List<Path> segments;

        try (Stream<Path> files = Files.list(outputDirectory)) {
            segments = files
                    .filter(path -> path.toString().endsWith(".ts"))
                    .sorted()
                    .toList();
        }

        return new HlsOutput(
                playlist,
                segments
        );
    }
}
