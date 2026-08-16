package io.bytecast.streaming.job;

import java.nio.file.Path;
import java.util.List;

public record HlsOutput(Path playlist, List<Path> segments) {
}