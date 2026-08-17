    package io.bytecast.streaming.exception;

public enum StreamingErrorCode {
    JOB_NOT_FOUND("JOB.NOT_FOUND", 404, "Job does not exist"),
    VIDEO_NOT_FOUND("VID.NOT_FOUND", 404, "Video does not exist in storage"),
    BUCKET_NOT_FOUND("BKT.NOT_FOUND", 404, "Storage bucket does not exist"),
    RANGE_UNSATISFIABLE("RNG.UNSATISFIABLE", 416, "Requested byte range exceeds file bounds"),
    RANGE_MALFORMED("RNG.MALFORMED", 400, "Range header syntax is invalid"),
    STORAGE_UNAVAILABLE("STG.UNAVAILABLE", 503, "Cannot reach storage backend"),
    STORAGE_TIMEOUT("STG.TIMEOUT", 504, "Storage backend timed out"),
    STORAGE_ERROR_UNKNOWN("STG.UNKNOWN", 500, "Unknown storage error"),
    STREAM_INTERRUPTED("STRM.INTERRUPTED", 500, "Client stream was interrupted"),
    UNAUTHORIZED("AUTH.FORBIDDEN", 403, "Access denied to this resource"),
    UNKNOWN("ERR.UNKNOWN", 500, "Unexpected streaming error");

    private final String code;
    private final int httpStatus;
    private final String defaultMessage;

    StreamingErrorCode(String code, int httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }
    public int httpStatus() { return httpStatus; }
    public String defaultMessage() { return defaultMessage; }
}