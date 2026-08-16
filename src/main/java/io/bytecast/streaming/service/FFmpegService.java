package io.bytecast.streaming.service;

import io.bytecast.streaming.job.HlsOutput;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ApplicationScoped
@Slf4j
public class FFmpegService {

    private static final List<Rendition> LADDER = List.of(
            new Rendition("360p", 640, 360, "800k", "128k"),
            //new Rendition("480p", 842, 480, "1400k", "128k"),
            //new Rendition("720p", 1280, 720, "2800k", "128k"),
            new Rendition("1080p", 1920, 1080, "5000k", "192k")
    );

    public HlsOutput generateHls(Path inputFile, Path outputDirectory) throws Exception {

        // Create one subdirectory per quality: outputDirectory/360p, /480p, ...
        for (Rendition r : LADDER) {
            Files.createDirectories(outputDirectory.resolve(r.name()));
        }

        Path masterPlaylist = outputDirectory.resolve("master.m3u8");

        List<String> command = buildCommand(inputFile, outputDirectory, masterPlaylist);

        log.info("Starting FFmpeg HLS multi-quality job | input={}, output={}",
                inputFile, outputDirectory);

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        // Drain FFmpeg's output so the process doesn't block on a full pipe buffer,
        // and so you actually get logs if something fails.
        try (var reader = process.inputReader()) {
            reader.lines().forEach(line -> log.debug("ffmpeg: " + line));
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        List<HlsRendition> renditions = new ArrayList<>();
        for (Rendition r : LADDER) {
            Path qualityDir = outputDirectory.resolve(r.name());
            Path variantPlaylist = qualityDir.resolve("playlist.m3u8");

            List<Path> segments;
            try (Stream<Path> files = Files.list(qualityDir)) {
                segments = files
                        .filter(p -> p.toString().endsWith(".ts"))
                        .sorted()
                        .toList();
            }

            renditions.add(new HlsRendition(r.name(), variantPlaylist, segments));
        }

        log.info("FFmpeg HLS job complete | renditions={}", renditions.size());

        return new HlsOutput(masterPlaylist, renditions);
    }

    private List<String> buildCommand(
            Path inputFile,
            Path outputDirectory,
            Path masterPlaylist
    ) {
        List<String> command = new ArrayList<>();

        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile.toString());

        StringBuilder filterComplex = new StringBuilder();

        filterComplex.append("[0:v]split=")
                .append(LADDER.size());

        for (int i = 0; i < LADDER.size(); i++) {
            filterComplex.append("[v").append(i + 1).append("]");
        }

        filterComplex.append(";");

        for (int i = 0; i < LADDER.size(); i++) {
            Rendition r = LADDER.get(i);

            filterComplex.append("[v")
                    .append(i + 1)
                    .append("]scale=w=")
                    .append(r.width())
                    .append(":h=")
                    .append(r.height())
                    .append("[v")
                    .append(i + 1)
                    .append("out]");

            if (i < LADDER.size() - 1) {
                filterComplex.append(";");
            }
        }

        command.add("-filter_complex");
        command.add(filterComplex.toString());

        for (int i = 0; i < LADDER.size(); i++) {
            Rendition r = LADDER.get(i);

            command.add("-map");
            command.add("[v" + (i + 1) + "out]");

            command.add("-c:v:" + i);
            command.add("libx264");

            command.add("-b:v:" + i);
            command.add(r.videoBitrate());
        }

        command.add("-f");
        command.add("hls");

        command.add("-hls_time");
        command.add("6");

        command.add("-hls_playlist_type");
        command.add("vod");

        command.add("-hls_flags");
        command.add("independent_segments");

        command.add("-master_pl_name");
        command.add(masterPlaylist.getFileName().toString());

        StringBuilder varStreamMap = new StringBuilder();

        for (int i = 0; i < LADDER.size(); i++) {
            if (i > 0) {
                varStreamMap.append(" ");
            }

            varStreamMap.append("v:")
                    .append(i)
                    .append(",name:")
                    .append(LADDER.get(i).name());
        }

        command.add("-var_stream_map");
        command.add(varStreamMap.toString());

        command.add("-hls_segment_filename");
        command.add(
                outputDirectory
                        .resolve("%v")
                        .resolve("segment_%03d.ts")
                        .toString()
        );

        command.add(
                outputDirectory
                        .resolve("%v")
                        .resolve("playlist.m3u8")
                        .toString()
        );

        return command;
    }

    /*private List<String> buildCommand(Path inputFile, Path outputDirectory, Path masterPlaylist) {
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(inputFile.toString());

        // Build filter_complex: split video into N copies, scale each
        StringBuilder split = new StringBuilder("[0:v]split=" + LADDER.size());
        for (int i = 0; i < LADDER.size(); i++) {
            split.append("[v").append(i + 1).append("]");
        }
        split.append("; ");

        for (int i = 0; i < LADDER.size(); i++) {
            Rendition r = LADDER.get(i);
            split.append("[v").append(i + 1).append("]scale=w=")
                    .append(r.width()).append(":h=").append(r.height())
                    .append("[v").append(i + 1).append("out]");
            if (i < LADDER.size() - 1) split.append("; ");
        }

        command.add("-filter_complex");
        command.add(split.toString());

        // Map each scaled video output + bitrate
        for (int i = 0; i < LADDER.size(); i++) {
            Rendition r = LADDER.get(i);
            command.add("-map");
            command.add("[v" + (i + 1) + "out]");
            command.add("-c:v:" + i);
            command.add("libx264");
            command.add("-b:v:" + i);
            command.add(r.videoBitrate());
        }

        // Map audio once per rendition (same source audio track each time)
        for (int i = 0; i < LADDER.size(); i++) {
            Rendition r = LADDER.get(i);
            command.add("-map");
            command.add("a:0");
            command.add("-c:a:" + i);
            command.add("aac");
            command.add("-b:a:" + i);
            command.add(r.audioBitrate());
        }

        command.add("-f");
        command.add("hls");
        command.add("-hls_time");
        command.add("6");
        command.add("-hls_playlist_type");
        command.add("vod");
        command.add("-hls_flags");
        command.add("independent_segments");
        command.add("-master_pl_name");
        command.add(masterPlaylist.getFileName().toString());

        // var_stream_map ties video/audio pairs to named variants, e.g.:
        // "v:0,a:0,name:360p v:1,a:1,name:480p ..."
        StringBuilder varStreamMap = new StringBuilder();
        for (int i = 0; i < LADDER.size(); i++) {
            if (i > 0) varStreamMap.append(" ");
            varStreamMap.append("v:").append(i).append(",a:").append(i)
                    .append(",name:").append(LADDER.get(i).name());
        }
        command.add("-var_stream_map");
        command.add(varStreamMap.toString());

        command.add("-hls_segment_filename");
        command.add(outputDirectory.resolve("%v").resolve("segment_%03d.ts").toString());

        // Output variant playlists go into per-quality folders: %v is substituted
        // with the "name" from var_stream_map (360p, 480p, ...)
        command.add(outputDirectory.resolve("%v").resolve("playlist.m3u8").toString());

        return command;
    }*/

    private record Rendition(String name, int width, int height, String videoBitrate, String audioBitrate) {}

}
