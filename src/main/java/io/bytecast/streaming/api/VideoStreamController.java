package io.bytecast.streaming.api;

import io.bytecast.streaming.job.VideoProcessingJob;
import io.bytecast.streaming.service.VideoProcessingService;
import io.bytecast.streaming.service.VideoStreamService;
import io.bytecast.streaming.storage.HlsFile;
import io.bytecast.streaming.storage.PresignedPostResponse;
import io.bytecast.streaming.storage.VideoData;
import io.bytecast.streaming.storage.VideoStorage;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.resteasy.reactive.RestResponse;
import java.io.InputStream;
import java.util.Map;

@Path("/api/videos")
@Slf4j
public class VideoStreamController {

    static final String APPLICATION_MPEGURL = "application/vnd.apple.mpegurl";
    static final String VIDEO_MP2T = "video/mp2t";

    @Inject
    VideoStreamService videoStreamService;

    @Inject
    VideoProcessingService processingService;

    @Inject
    VideoStorage videoStorage;

    @GET
    @Path("/{videoId}/upload")
    public RestResponse<PresignedPostResponse> generatePreSignedUrl(@PathParam("videoId") String videoId) {
        PresignedPostResponse preSignedUrl = videoStorage.preSignedUrl(videoId);
        return RestResponse.ok(preSignedUrl);
    }

    @GET
    @Path("/{videoId}/stream")
    public Response stream(@PathParam("videoId") String videoId, @HeaderParam("Range") String rangeHeader) throws Exception {
        log.info("Stream request received | videoId={}, rangeHeader={}", videoId, rangeHeader);

        VideoData segment = videoStreamService.getSegment(videoId, rangeHeader);

        log.info("Returning partial content (206) | videoId={}, Content-Range={}-/{}/{}", videoId, segment.start(), segment.end(), segment.size());

        return Response.status(206)
                .entity(segment.stream())
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes " + segment.start() + "-" + segment.end() + "/" + segment.size())
                .header("Content-Type", segment.contentType())
                .header("Content-Length", segment.length())
                .build();
    }

    @GET
    @Path("/{videoId}/hls/master.m3u8")
    @Produces("application/vnd.apple.mpegurl")
    public Response getMasterPlaylist(@PathParam("videoId") String videoId) throws Exception {
        log.info("Stream Hls mnaster.m3u8 request received | videoId={}", videoId);
        HlsFile masterM3u8 = videoStreamService.getMasterM3u8(videoId);
        StreamingOutput out = output -> {
            try (InputStream in = masterM3u8.stream()) {
                in.transferTo(output);
            }
        };
        Response res = Response.ok(out).header("Content-Type", "application/vnd.apple.mpegurl").build();
        log.info("Returning OK (200) | videoId={}", videoId);
        return res;
    }

    @GET
    @Path("/{videoId}/hls/{quality}/{segment}")
    public Response getSegment(@PathParam("videoId") String videoId, @PathParam("quality") String quality, @PathParam("segment") String segment) throws Exception {
        log.info("Stream segment file request received | videoId={}, quality={}, segment={}", videoId, quality, segment);
        HlsFile hlsObject = videoStorage.getHlsObject(videoId, quality, segment);
        StreamingOutput out = output -> {
            try (InputStream in = hlsObject.stream()) {
                in.transferTo(output);
            }
        };
        String contentType = segment.endsWith(".m3u8")
                ? APPLICATION_MPEGURL
                : VIDEO_MP2T;
        return Response.ok(out).type(contentType).build();
    }

    @GET
    @Path("/{videoId}/hls/{segment}")
    public Response getSegment(@PathParam("videoId") String videoId, @PathParam("segment") String segment) throws Exception {
        log.info("Stream segment file request received | videoId={}, segment={}", videoId, segment);
        HlsFile hlsObject = videoStorage.getHlsObject(videoId, null, segment);
        StreamingOutput out = output -> {
            try (InputStream in = hlsObject.stream()) {
                in.transferTo(output);
            }
        };
        String contentType = segment.endsWith(".m3u8")
                ? APPLICATION_MPEGURL
                : VIDEO_MP2T;
        return Response.ok(out).type(contentType).build();
    }

    @POST
    @Path("/{videoId}/HlsGenerationJob")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public RestResponse<VideoProcessingJob> process(@PathParam("videoId") String videoId, @RequestBody Map<String, Object> context) {

        VideoProcessingJob job = processingService.submitHlsGeneration(videoId, context);

        return RestResponse.accepted(job);
    }
}