package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration tests for ModuleWhitelistService using real Consul container.
 * Uses the new @ConsulIntegrationTest annotation for cleaner setup.
 */
@ConsulIntegrationTest(namespacePrefix = "whitelist-test")
class ModuleWhitelistServiceIT extends ModuleWhitelistServiceTestBase {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Override
    @BeforeEach
    void setupDependencies() {
        // Create the cluster service first (dependency for whitelist service)
        ClusterServiceImpl clusterServiceImpl = consul.createService(ClusterServiceImpl.class)
            .build();
        
        // Create the module whitelist service with its dependencies
        ModuleWhitelistServiceImpl moduleWhitelistServiceImpl = consul.createService(ModuleWhitelistServiceImpl.class)
            .withDependency("clusterService", clusterServiceImpl)
            .withDependency("pipelineConfigService", new StubPipelineConfigService())
            .build();
        
        // Set test base class fields
        this.clusterService = clusterServiceImpl;
        this.moduleWhitelistService = moduleWhitelistServiceImpl;
    }
}