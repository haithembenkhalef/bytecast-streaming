package io.bytecast.streaming.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "bytecast.storage")
public interface Config {

    /**
     *
     * @return Storage bucket name
     */
    @WithName("bucket")
    String bucket();
}
