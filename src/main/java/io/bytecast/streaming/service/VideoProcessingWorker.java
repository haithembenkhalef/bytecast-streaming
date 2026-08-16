package io.bytecast.streaming.service;

import io.bytecast.streaming.job.VideoProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

@ApplicationScoped
public class VideoProcessingWorker {

    @Inject
    ManagedExecutor executor;

    public void execute(Runnable processor) {
        executor.runAsync(processor);
    }
}