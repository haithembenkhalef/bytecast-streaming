package io.bytecast.streaming.job;

import java.util.Map;

public interface VideoProcessor<R> {

    R process(String videoId, Map<String, Object> context);
}