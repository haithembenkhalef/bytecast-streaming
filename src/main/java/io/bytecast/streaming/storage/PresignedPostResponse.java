package io.bytecast.streaming.storage;

import io.quarkus.arc.All;
import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class PresignedPostResponse {
    public String url;
    public Map<String, String> formData;
}