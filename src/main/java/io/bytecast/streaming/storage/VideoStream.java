package io.bytecast.streaming.storage;

import jakarta.ws.rs.core.StreamingOutput;

public record VideoStream(StreamingOutput stream, long transferred) {
}


