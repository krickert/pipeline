package com.rokkon.pipeline.engine.validation;

import com.rokkon.pipeline.engine.validation.validators.*;
import com.rokkon.pipeline.validation.Composite;
import com.rokkon.pipeline.validation.ConfigValidator;
import com.rokkon.pipeline.validation.PipelineConfigValidatable;
import com.rokkon.pipeline.validation.PipelineConfigValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;

/**
 * Producer for the composite PipelineConfigValidator that aggregates all individual validators.
 */
@ApplicationScoped
public class PipelineValidatorProducer {
    
    @Inject RequiredFieldsValidator requiredFieldsValidator;
    @Inject
    NamingConventionValidator namingConventionValidator;
    @Inject StepReferenceValidator stepReferenceValidator;
    @Inject
    ProcessorInfoValidator processorInfoValidator;
    @Inject RetryConfigValidator retryConfigValidator;
    @Inject TransportConfigValidator transportConfigValidator;
    @Inject
    OutputRoutingValidator outputRoutingValidator;
    @Inject
    KafkaTopicNamingValidator kafkaTopicNamingValidator;
    @Inject
    IntraPipelineLoopValidator intraPipelineLoopValidator;
    @Inject StepTypeValidator stepTypeValidator;
    
    @Produces
    @Composite
    @Named("compositePipelineValidator")
    @ApplicationScoped
    public PipelineConfigValidator producePipelineConfigValidator() {
        List<ConfigValidator<PipelineConfigValidatable>> validatorList = new ArrayList<>();
        
        // Add all validators explicitly to avoid circular dependency issues
        validatorList.add(requiredFieldsValidator);
        validatorList.add(namingConventionValidator);
        validatorList.add(stepReferenceValidator);
        validatorList.add(processorInfoValidator);
        validatorList.add(retryConfigValidator);
        validatorList.add(transportConfigValidator);
        validatorList.add(outputRoutingValidator);
        validatorList.add(kafkaTopicNamingValidator);
        validatorList.add(intraPipelineLoopValidator);
        validatorList.add(stepTypeValidator);
        
        // Create and return a concrete implementation
        return new CompositePipelineConfigValidator(validatorList);
    }
}