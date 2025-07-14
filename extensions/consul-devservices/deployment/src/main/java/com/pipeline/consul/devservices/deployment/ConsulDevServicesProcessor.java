package com.pipeline.consul.devservices.deployment;

import com.pipeline.consul.devservices.ConsulDevServicesConfig;
import com.pipeline.consul.devservices.ConsulDevServicesProvider;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.*;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.runtime.configuration.ConfigUtils;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Build steps for Consul DevServices.
 */
public class ConsulDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(ConsulDevServicesProcessor.class);
    private static final String CONSUL_HOST_PROP = "quarkus.consul.host";
    private static final String FEATURE = "consul-devservices";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    DevServicesResultBuildItem startConsulDevServices(
            ConsulDevServicesConfig consulConfig,
            LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem) {

        // Don't start if Docker is not available
        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOG.warn("Docker not available, cannot start Consul DevServices");
            return null;
        }

        // Don't start if user has already configured Consul
        if (ConfigUtils.isPropertyPresent(CONSUL_HOST_PROP)) {
            LOG.debugf("Not starting Consul DevServices as %s is configured", CONSUL_HOST_PROP);
            return null;
        }

        // Create a log compressor to avoid cluttering the console
        try (StartupLogCompressor compressor = new StartupLogCompressor(
                "(Consul DevServices)",
                consoleInstalledBuildItem,
                loggingSetupBuildItem
        )) {
            Optional<Map<String, String>> properties = ConsulDevServicesProvider
                    .startConsulContainer(consulConfig, launchMode.getLaunchMode());

            if (properties.isPresent()) {
                Map<String, String> config = properties.get();

                // Register shutdown handler
                closeBuildItem.addCloseTask(ConsulDevServicesProvider::stopConsulContainer, true);

                // Log success
                String consulUrl = String.format("http://%s:%s",
                        config.get("quarkus.consul.host"),
                        config.get("quarkus.consul.port"));

                LOG.infof("Consul DevServices started at %s", consulUrl);
                LOG.infof("Consul UI available at %s/ui", consulUrl);

                // Return the result
                return new DevServicesResultBuildItem(
                        FEATURE,
                        null, // We'll enhance this later to track container IDs
                        config
                );
            }
        }

        return null;
    }

    @BuildStep
    void addDependencies(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        // Index Consul and TestContainers classes
        indexDependency.produce(new IndexDependencyBuildItem("org.testcontainers", "consul"));
    }
}