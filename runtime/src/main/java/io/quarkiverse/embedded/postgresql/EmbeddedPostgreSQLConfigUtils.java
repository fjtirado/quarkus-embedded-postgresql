package io.quarkiverse.embedded.postgresql;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;

public class EmbeddedPostgreSQLConfigUtils {

    static final String QUARKUS_DATASOURCE_REACTIVE_URL = "quarkus.datasource.reactive.url";
    static final String QUARKUS_DATASOURCE_JDBC_URL = "quarkus.datasource.jdbc.url";
    static final String QUARKUS_NAMED_DATASOURCE_REACTIVE_URL = "quarkus.datasource.\"%s\".reactive.url";
    static final String QUARKUS_NAMED_DATASOURCE_JDBC_URL = "quarkus.datasource.\"%s\".jdbc.url";
    static final String QUARKUS_DATASOURCE_USERNAME = "quarkus.datasource.username";
    static final String QUARKUS_DATASOURCE_PASSWORD = "quarkus.datasource.password";
    static final String QUARKUS_NAMED_DATASOURCE_USERNAME = "quarkus.datasource.\"%s\".username";
    static final String QUARKUS_NAMED_DATASOURCE_PASSWORD = "quarkus.datasource.\"%s\".password";
    static final String DEFAULT_DATABASE = "postgres";
    static final String DEFAULT_REACTIVE_URL = "postgresql://localhost:%d/%s?stringtype=unspecified";
    static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:%d/%s?stringtype=unspecified";
    static final String DEFAULT_USERNAME = "postgres";
    static final String DEFAULT_PASSWORD = "postgres";

    static final String PORT_PROPERTY = "quarkus.embedded.postgresql.port";

    private static final int START_PORT = 5432;

    public static Map<String, String> getConfig(int port, Map<String, String> dbNames) {
        Map<String, String> allConfigs = new HashMap<>();

        dbNames.forEach((key, value) -> {
            allConfigs.put(format(QUARKUS_NAMED_DATASOURCE_REACTIVE_URL, key),
                    format(DEFAULT_REACTIVE_URL, port, value));
            allConfigs.put(format(QUARKUS_NAMED_DATASOURCE_JDBC_URL, key),
                    format(DEFAULT_JDBC_URL, port, value));
            allConfigs.put(format(QUARKUS_NAMED_DATASOURCE_USERNAME, key), DEFAULT_USERNAME);
            allConfigs.put(format(QUARKUS_NAMED_DATASOURCE_PASSWORD, key), DEFAULT_PASSWORD);
        });

        allConfigs.put(QUARKUS_DATASOURCE_REACTIVE_URL, format(DEFAULT_REACTIVE_URL, port, DEFAULT_DATABASE));
        allConfigs.put(QUARKUS_DATASOURCE_JDBC_URL, format(DEFAULT_JDBC_URL, port, DEFAULT_DATABASE));
        allConfigs.put(QUARKUS_DATASOURCE_USERNAME, DEFAULT_USERNAME);
        allConfigs.put(QUARKUS_DATASOURCE_PASSWORD, DEFAULT_PASSWORD);

        return allConfigs;
    }

    public static int getPort() {
        return ConfigProvider.getConfig().getOptionalValue(PORT_PROPERTY, Integer.class)
                .orElseGet(EmbeddedPostgreSQLConfigUtils::getDefaultPort);
    }

    private static Integer getDefaultPort() {
        return (int) Math.round(Math.random() * 10) + START_PORT;
    }

    public static Map<String, String> getDBNames(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return dataSourcesBuildTimeConfig.dataSources().entrySet().stream()
                .filter(ds -> ds.getValue().dbKind().filter(kind -> kind.equals("postgresql")).isPresent())
                .map(Entry::getKey)
                .collect(Collectors.toMap(e -> e, PostgreSQLSyntaxUtils::sanitizeDbName));
    }
}
