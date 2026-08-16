package io.bytecast.streaming.api;

import io.bytecast.streaming.job.VideoProcessingJob;
import io.bytecast.streaming.service.VideoProcessingService;
import io.bytecast.streaming.service.VideoStreamService;
import io.bytecast.streaming.storage.PresignedPostResponse;
import io.bytecast.streaming.storage.VideoData;
import io.bytecast.streaming.storage.VideoStorage;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.resteasy.reactive.RestResponse;
import java.util.Map;

@Path("/api/videos")
@Slf4j
public class VideoStreamController {

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
    public Response stream(@PathParam("videoId") String videoId,
                           @HeaderParam("Range") String rangeHeader) throws Exception {
        log.info("Stream request received | videoId={}, rangeHeader={}", videoId, rangeHeader);

        VideoData segment = videoStreamService.getSegment(videoId, rangeHeader);

        log.info("Returning partial content (206) | videoId={}, Content-Range={}-/{}/{}",
                videoId, segment.start(), segment.end(), segment.size());

        return Response.status(206)
                .entity(segment.stream())
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", "bytes " + segment.start() + "-" + segment.end() + "/" + segment.size())
                .header("Content-Type", segment.contentType())
                .header("Content-Length", segment.size())
                .build();
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