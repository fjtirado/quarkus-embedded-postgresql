package io.quarkiverse.embedded.postgresql;

import static io.quarkiverse.embedded.postgresql.EmbeddedPostgreSQLConfigSourceFactory.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres.Builder;

@Recorder
public class EmbeddedPostgreSQLRecorder {

    private static final Logger logger = Logger.getLogger(EmbeddedPostgreSQLRecorder.class);

    public void startPostgres(ShutdownContext shutdownContext, int port, Map<String, String> dbNames) {
        Builder builder = EmbeddedPostgres.builder();
        Config config = ConfigProvider.getConfig();
        builder.setPort(port);
        builder.setConnectConfig("stringtype",
                config.getOptionalValue("quarkus.embedded.postgresql.stringtype", String.class).orElse("unspecified"));

        config.getOptionalValue("quarkus.embedded.postgresql.startup.wait", Long.class).ifPresent(
                timeout -> {
                    logger.infov("PG startup timeout set to {0}", timeout);
                    builder.setPGStartupWait(Duration.ofMillis(timeout));
                });

        config.getOptionalValue("quarkus.embedded.postgresql.data.dir", String.class).ifPresent(path -> {
            logger.infov("Setting embedded postgresql data dir to {0}", path);
            builder.setDataDirectory(path);
            builder.setCleanDataDirectory(false);
        });

        EmbeddedPostgres pg;
        try {
            pg = builder.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        logger.infov(
                "Embedded Postgres started at port \"{0,number,#}\" with database \"{1}\", user \"{2}\" and password \"{3}\"",
                pg.getPort(), DEFAULT_DATABASE, DEFAULT_USERNAME, DEFAULT_PASSWORD);
        shutdownContext.addShutdownTask(() -> {
            try {
                pg.close();
            } catch (IOException e) {
                logger.warn("Error shutting down embedded postgres", e);
            }
        });
        createDatabases(pg, dbNames.values(), DEFAULT_USERNAME);
    }

    private void createDatabases(EmbeddedPostgres pg, Collection<String> dbNames,
            String userName) {
        pg.getDatabase(DEFAULT_USERNAME, DEFAULT_DATABASE);
        dbNames.forEach(ds -> createDatabase(pg.getPostgresDatabase(), ds, userName));
    }

    private void createDatabase(final DataSource dataSource, final String sanitizedDbName, final String userName) {
        Objects.requireNonNull(userName);
        String createDbStatement = String.format(
                "SELECT 'CREATE DATABASE %s OWNER %s' as createQuery WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '%s')",
                sanitizedDbName, userName, sanitizedDbName);
        try (Connection connection = dataSource.getConnection();
                Statement stmt = connection.createStatement()) {
            ResultSet result = stmt.executeQuery(createDbStatement);
            if (result.next()) {
                stmt.executeUpdate(result.getString("createQuery"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error creating DB " + sanitizedDbName, e);
        }
    }
}
