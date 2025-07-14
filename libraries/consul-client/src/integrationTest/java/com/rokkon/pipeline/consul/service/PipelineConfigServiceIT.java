package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.config.service.ClusterService;
import com.rokkon.pipeline.config.service.PipelineConfigService;
import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import com.rokkon.pipeline.engine.validation.CompositeValidator;
import com.rokkon.pipeline.engine.validation.validators.*;
import com.rokkon.pipeline.validation.PipelineConfigValidatable;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration test for PipelineConfigService running in prod mode.
 * Extends PipelineConfigServiceTestBase to reuse common test logic.
 * 
 * This follows the pattern described in TESTING_STRATEGY.md where integration tests
 * use @ConsulIntegrationTest annotation and create services directly.
 */
@ConsulIntegrationTest(namespacePrefix = "pipeline-config-test")
class PipelineConfigServiceIT extends PipelineConfigServiceTestBase {

    @ConsulTest
    private ConsulTestContext consul;
    
    private PipelineConfigService pipelineConfigService;
    private ClusterService clusterService;

    @BeforeEach
    @Override
    void setup() {
        // Initialize services first
        String consulHost = System.getProperty("consul.host", "localhost");
        String consulPort = System.getProperty("consul.port", "8500");

        // Create ClusterService first as it's a dependency
        ClusterServiceImpl clusterServiceImpl = consul.createService(ClusterServiceImpl.class)
            .withDependency("consulHost", consulHost)
            .withDependency("consulPort", consulPort)
            .build();
        this.clusterService = clusterServiceImpl;

        // Create validators for pipeline config service
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

        // Create PipelineConfigService
        PipelineConfigServiceImpl pipelineConfigServiceImpl = consul.createService(PipelineConfigServiceImpl.class)
            .withDependency("consulHost", consulHost)
            .withDependency("consulPort", consulPort)
            .withDependency("clusterService", clusterServiceImpl)
            .withDependency("validator", validator)
            .build();
        
        this.pipelineConfigService = pipelineConfigServiceImpl;
        
        // Now call parent setup
        super.setup();
    }

    @Override
    protected PipelineConfigService getPipelineConfigService() {
        return pipelineConfigService;
    }

    @Override
    protected ClusterService getClusterService() {
        return clusterService;
    }
}