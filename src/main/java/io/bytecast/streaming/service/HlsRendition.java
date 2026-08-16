package io.bytecast.streaming.service;

import java.nio.file.Path;
import java.util.List;

public record HlsRendition(
        String quality,        // e.g. "360p", "480p", "720p", "1080p"
        Path playlist,          // path to this quality's playlist.m3u8
        List<Path> segments     // this quality's .ts segments
) {}