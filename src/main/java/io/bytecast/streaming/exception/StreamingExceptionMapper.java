package io.bytecast.streaming.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Provider
public class StreamingExceptionMapper implements ExceptionMapper<StreamingException> {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingExceptionMapper.class);

    @Override
    public Response toResponse(StreamingException e) {
        StreamingErrorCode code = e.getErrorCode();

        LOG.warn("[{}] resource={} | {}", code.code(), e.getResourceId(), e.getMessage(),
                e.getCause() != null ? e.getCause() : null);

        Map<String, Object> body = Map.of(
                "error", code.code(),
                "status", code.httpStatus(),
                "resource", e.getResourceId(),
                "message", code.defaultMessage(),
                "detail", e.getDetail() != null ? e.getDetail() : ""
        );

        Response.ResponseBuilder builder = Response.status(code.httpStatus())
                .entity(body)
                .type(MediaType.APPLICATION_JSON);

        for (Map.Entry<String, String> h : e.getHeaders().entrySet()) {
            builder.header(h.getKey(), h.getValue());
        }

        return builder.build();
    }
}