package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.consul.registry.validation.TestModuleConnectionValidator;
import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration tests for GlobalModuleRegistryService using real Consul.
 * Uses the new @ConsulIntegrationTest annotation for cleaner setup.
 * Uses TestModuleConnectionValidator instead of mocking for true integration testing.
 */
@ConsulIntegrationTest(namespacePrefix = "global-registry-test")
class GlobalModuleRegistryServiceIT extends GlobalModuleRegistryServiceTestBase {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Override
    @BeforeEach
    void setupDependencies() {
        // Create dummy implementations for non-Consul dependencies
        TestModuleConnectionValidator testValidator = new TestModuleConnectionValidator();
        HealthCheckConfigProvider testHealthConfig = new TestHealthCheckConfigProvider();
        
        // Create the service with test implementations
        GlobalModuleRegistryServiceImpl serviceImpl = consul.createService(GlobalModuleRegistryServiceImpl.class)
            .withDependency("connectionValidator", testValidator)
            .withDependency("healthConfig", testHealthConfig)
            .build();
        
        this.globalModuleRegistryService = serviceImpl;
    }
    
    /**
     * Test implementation of HealthCheckConfigProvider
     */
    private static class TestHealthCheckConfigProvider extends HealthCheckConfigProvider {
        @Override
        public java.time.Duration getCheckInterval() {
            return java.time.Duration.ofSeconds(10);
        }
        
        @Override
        public java.time.Duration getDeregisterAfter() {
            return java.time.Duration.ofSeconds(60);
        }
        
        @Override
        public boolean isCleanupEnabled() {
            return false;
        }
    }
}