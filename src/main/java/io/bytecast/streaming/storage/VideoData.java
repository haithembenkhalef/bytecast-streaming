package io.bytecast.streaming.storage;


import jakarta.ws.rs.core.StreamingOutput;

public record VideoData(StreamingOutput stream, long transferred, long start, long end, long length, long size, String contentType) {
}
