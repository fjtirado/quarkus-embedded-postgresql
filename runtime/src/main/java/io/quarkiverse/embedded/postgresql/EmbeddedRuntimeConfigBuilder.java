package io.quarkiverse.embedded.postgresql;

import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

public class EmbeddedRuntimeConfigBuilder implements ConfigBuilder {

    private static final Logger logger = Logger.getLogger(EmbeddedRuntimeConfigBuilder.class);

    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        logger.info("PUTO BUILDER");
        return builder.withSources(new EmbeddedPostgreSQLConfigSourceFactory());
    }

}
