package io.bytecast.streaming.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
public class StreamingException extends RuntimeException {
    private final StreamingErrorCode errorCode;
    private final String resourceId;
    private final String detail;
    private final Map<String, String> headers;

    public StreamingException(StreamingErrorCode errorCode, String resourceId) {
        this(errorCode, resourceId, null, null);
    }

    public StreamingException(StreamingErrorCode errorCode, String resourceId, String detail) {
        this(errorCode, resourceId, detail, null);
    }

    public StreamingException(StreamingErrorCode errorCode, String resourceId, String detail,
                              Map<String, String> headers) {
        super(errorCode.defaultMessage() + (detail != null ? " | " + detail : ""));
        this.errorCode = errorCode;
        this.resourceId = resourceId;
        this.detail = detail;
        this.headers = headers != null ? new HashMap<>(headers) : Collections.emptyMap();
    }

    public StreamingException(StreamingErrorCode errorCode, String resourceId, Throwable cause) {
        super(errorCode.defaultMessage(), cause);
        this.errorCode = errorCode;
        this.resourceId = resourceId;
        this.detail = cause.getMessage();
        this.headers = Collections.emptyMap();
    }
}