package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration tests for ClusterService using real Consul.
 * Uses the new @ConsulIntegrationTest annotation for cleaner setup.
 */
@ConsulIntegrationTest(namespacePrefix = "cluster-test")
class ClusterServiceIT extends ClusterServiceTestBase {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Override
    @BeforeEach
    void setupDependencies() {
        // Create the service using the test context
        ClusterServiceImpl clusterServiceImpl = consul.createService(ClusterServiceImpl.class)
            .build();
        
        this.clusterService = clusterServiceImpl;
    }
}