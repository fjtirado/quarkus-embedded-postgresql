package io.quarkiverse.embedded.postgresql;

import java.util.Map;

import io.smallrye.config.common.MapBackedConfigSource;

public class EmbeddedPostgreSQLConfigSource extends MapBackedConfigSource {

    public EmbeddedPostgreSQLConfigSource(Map<String, String> propertyMap) {
        super("EmbeddedPostGresSQL", propertyMap);
    }

}
