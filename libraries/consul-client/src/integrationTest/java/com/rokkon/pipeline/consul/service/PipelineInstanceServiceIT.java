package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.api.validation.ConfigValidatable;
import com.rokkon.pipeline.api.validation.ValidationMode;
import com.rokkon.pipeline.api.validation.ValidationResult;
import com.rokkon.pipeline.commons.validation.EmptyValidationResult;
import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;

import java.time.Duration;

/**
 * Integration test for PipelineInstanceService using real Consul.
 */
@ConsulIntegrationTest(namespacePrefix = "instance-test")
class PipelineInstanceServiceIT extends PipelineInstanceServiceTestBase {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    @Override
    void setupDependencies() {
        // Create a stub validator for the integration test
        var stubValidator = new com.rokkon.pipeline.engine.validation.CompositeValidator() {
            @Override
            public ValidationResult validate(
                    ConfigValidatable config,
                    ValidationMode mode) {
                // Always return success for integration testing
                return EmptyValidationResult.instance();
            }
        };
        
        // Create real services using ConsulTestContext
        this.pipelineDefinitionService = consul.createService(PipelineDefinitionServiceImpl.class)
            .withKvPrefix(consul.namespace())
            .withDependency("pipelineValidator", stubValidator)
            .build();
        
        this.pipelineInstanceService = consul.createService(PipelineInstanceServiceImpl.class)
            .withKvPrefix(consul.namespace())
            .withDependency("pipelineDefinitionService", pipelineDefinitionService)
            .build();
    }
    
    @Override
    void cleanup() {
        super.cleanup();
        
        // Clean up test definitions
        for (String defId : testDefinitionIds) {
            try {
                pipelineDefinitionService.deleteDefinition(defId)
                    .await().atMost(Duration.ofSeconds(5));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}