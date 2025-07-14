package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.config.service.PipelineDefinitionService;
import com.rokkon.pipeline.config.service.PipelineInstanceService;
import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import com.rokkon.pipeline.engine.validation.CompositeValidator;
import com.rokkon.pipeline.engine.validation.validators.*;
import com.rokkon.pipeline.validation.PipelineConfigValidatable;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration test for PipelineDefinitionService running in prod mode.
 * Extends PipelineDefinitionServiceTestBase to reuse common test logic.
 * 
 * This follows the pattern described in TESTING_STRATEGY.md where integration tests
 * use @ConsulIntegrationTest annotation and create services directly.
 */
@ConsulIntegrationTest(namespacePrefix = "pipeline-definition-test")
class PipelineDefinitionServiceIT extends PipelineDefinitionServiceTestBase {

    @ConsulTest
    private ConsulTestContext consul;
    
    private PipelineDefinitionService pipelineDefinitionService;
    private PipelineInstanceService pipelineInstanceService;

    @BeforeEach
    @Override
    void setup() {
        // Initialize services first
        String kvPrefix = consul.namespace();

        // Create validators for pipeline definition service
        CompositeValidator<PipelineConfigValidatable> validator = new CompositeValidator<>("PipelineConfigValidator");
        validator.addValidator(new RequiredFieldsValidator())
                 .addValidator(new NamingConventionValidator())
                 .addValidator(new StepTypeValidator())
                 .addValidator(new IntraPipelineLoopValidator())
                 .addValidator(new StepReferenceValidator())
                 .addValidator(new ProcessorInfoValidator())
                 .addValidator(new TransportConfigValidator())
                 .addValidator(new RetryConfigValidator())
                 .addValidator(new OutputRoutingValidator())
                 .addValidator(new KafkaTopicNamingValidator());

        // Create PipelineInstanceService mock/stub (needed as dependency)
        // For integration tests, we'll create a simple stub that returns empty results
        this.pipelineInstanceService = new StubPipelineInstanceService();

        // Create PipelineDefinitionService
        PipelineDefinitionServiceImpl pipelineDefinitionServiceImpl = consul.createService(PipelineDefinitionServiceImpl.class)
            .withKvPrefix(kvPrefix)
            .withDependency("pipelineValidator", validator)
            .withDependency("pipelineInstanceService", pipelineInstanceService)
            .build();
        
        this.pipelineDefinitionService = pipelineDefinitionServiceImpl;
        
        // Now call parent setup
        super.setup();
    }

    @Override
    protected PipelineDefinitionService getPipelineDefinitionService() {
        return pipelineDefinitionService;
    }

    @Override
    protected PipelineInstanceService getPipelineInstanceService() {
        return pipelineInstanceService;
    }
    
    /**
     * Stub implementation of PipelineInstanceService for testing.
     * Returns empty results for all operations.
     */
    private static class StubPipelineInstanceService implements PipelineInstanceService {
        @Override
        public io.smallrye.mutiny.Uni<java.util.List<com.rokkon.pipeline.config.model.PipelineInstance>> listInstances(String clusterName) {
            return io.smallrye.mutiny.Uni.createFrom().item(java.util.Collections.emptyList());
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.config.model.PipelineInstance> getInstance(String clusterName, String instanceId) {
            return io.smallrye.mutiny.Uni.createFrom().nullItem();
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.validation.ValidationResult> createInstance(String clusterName, com.rokkon.pipeline.config.model.CreateInstanceRequest request) {
            return io.smallrye.mutiny.Uni.createFrom().item(com.rokkon.pipeline.validation.impl.ValidationResultFactory.success());
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.validation.ValidationResult> updateInstance(String clusterName, String instanceId, com.rokkon.pipeline.config.model.PipelineInstance instance) {
            return io.smallrye.mutiny.Uni.createFrom().item(com.rokkon.pipeline.validation.impl.ValidationResultFactory.success());
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.validation.ValidationResult> deleteInstance(String clusterName, String instanceId) {
            return io.smallrye.mutiny.Uni.createFrom().item(com.rokkon.pipeline.validation.impl.ValidationResultFactory.success());
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.validation.ValidationResult> startInstance(String clusterName, String instanceId) {
            return io.smallrye.mutiny.Uni.createFrom().item(com.rokkon.pipeline.validation.impl.ValidationResultFactory.success());
        }

        @Override
        public io.smallrye.mutiny.Uni<com.rokkon.pipeline.validation.ValidationResult> stopInstance(String clusterName, String instanceId) {
            return io.smallrye.mutiny.Uni.createFrom().item(com.rokkon.pipeline.validation.impl.ValidationResultFactory.success());
        }

        @Override
        public io.smallrye.mutiny.Uni<Boolean> instanceExists(String clusterName, String instanceId) {
            return io.smallrye.mutiny.Uni.createFrom().item(false);
        }

        @Override
        public io.smallrye.mutiny.Uni<java.util.List<com.rokkon.pipeline.config.model.PipelineInstance>> listInstancesByDefinition(String pipelineDefinitionId) {
            return io.smallrye.mutiny.Uni.createFrom().item(java.util.Collections.emptyList());
        }
    }
}