package io.bytecast.streaming.job;

import io.bytecast.streaming.service.HlsRendition;

import java.nio.file.Path;
import java.util.List;

public record HlsOutput(Path playlist, List<HlsRendition> segments) {
}