package io.quarkiverse.embedded.postgresql.deployment;

import static io.quarkiverse.embedded.postgresql.EmbeddedPostgreSQLConfigUtils.*;
import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Map;

import io.quarkiverse.embedded.postgresql.EmbeddedPostgreSQLConnectionConfigurer;
import io.quarkiverse.embedded.postgresql.EmbeddedPostgreSQLRecorder;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class EmbeddedPostgreSQLProcessor {

    private static final String FEATURE = "embedded-postgres";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsDevelopment.class)
    @Record(RUNTIME_INIT)
    ServiceStartBuildItem startService(EmbeddedPostgreSQLRecorder recorder, ShutdownContextBuildItem shutdown,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> configProducer) {
        final int port = getPort();
        Map<String, String> dbNames = getDBNames(dataSourcesBuildTimeConfig);
        recorder.startPostgres(shutdown, port, dbNames);
        getConfig(port, dbNames).forEach((k, v) -> configProducer.produce(new RunTimeConfigurationDefaultBuildItem(k, v)));
        return new ServiceStartBuildItem(FEATURE);
    }

    @BuildStep
    void configureAgroalConnection(BuildProducer<AdditionalBeanBuildItem> additionalBeans, Capabilities capabilities) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            additionalBeans
                    .produce(new AdditionalBeanBuildItem.Builder().addBeanClass(EmbeddedPostgreSQLConnectionConfigurer.class)
                            .setDefaultScope(BuiltinScope.APPLICATION.getName())
                            .setUnremovable()
                            .build());
        }
    }

    @BuildStep
    void registerDriver(BuildProducer<JdbcDriverBuildItem> jdbcDriver) {
        jdbcDriver.produce(new JdbcDriverBuildItem(DatabaseKind.POSTGRESQL, "org.postgresql.Driver",
                "org.postgresql.xa.PGXADataSource"));
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(new IndexDependencyBuildItem("io.zonky.test",
                "embedded-postgres"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void nativeResources(BuildProducer<NativeImageResourcePatternsBuildItem> resource) {
        resource.produce(NativeImageResourcePatternsBuildItem.builder().includeGlob("postgres-*.txz").build());
    }
}
