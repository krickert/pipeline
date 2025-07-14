
package com.rokkon.testing.server.consul;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;

public class ConsulIntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "rokkon.test.consul.container-name", "rokkon-integration-test-consul"
        );
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(ConsulTestResource.class));
    }
}
