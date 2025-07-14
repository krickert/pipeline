package com.rokkon.pipeline.validation;

import com.rokkon.pipeline.config.model.PipelineConfig;
import com.rokkon.pipeline.engine.validation.CompositeValidator;
import com.rokkon.pipeline.engine.validation.validators.ProcessorInfoValidator;
import com.rokkon.pipeline.engine.validation.validators.OutputRoutingValidator;
import com.rokkon.pipeline.engine.validation.validators.RequiredFieldsValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ValidationModeTestBase {
    
    protected abstract CompositeValidator<PipelineConfig> getValidator();
    
    @Test
    void testValidatorSupportsModes() {
        // Test that validators can declare supported modes
        ProcessorInfoValidator processorValidator = new ProcessorInfoValidator();
        assertThat(processorValidator.supportedModes()).containsExactly(ValidationMode.PRODUCTION);
        
        OutputRoutingValidator routingValidator = new OutputRoutingValidator();
        assertThat(routingValidator.supportedModes()).containsExactly(ValidationMode.PRODUCTION);
        
        RequiredFieldsValidator requiredValidator = new RequiredFieldsValidator();
        assertThat(requiredValidator.supportedModes()).containsExactlyInAnyOrder(
            ValidationMode.PRODUCTION, ValidationMode.DESIGN, ValidationMode.TESTING);
    }
    
    @Test
    void testDesignModeSkipsProductionOnlyValidators() {
        PipelineConfig config = new PipelineConfig("test", null);
        
        // Design mode should only run the all-modes validator
        ValidationResult designResult = getValidator().validate(config, ValidationMode.DESIGN);
        assertThat(designResult.valid()).isTrue();
        assertThat(designResult.hasErrors()).isFalse();
        
        // Production mode should run both validators
        ValidationResult productionResult = getValidator().validate(config, ValidationMode.PRODUCTION);
        assertThat(productionResult.valid()).isFalse();
        assertThat(productionResult.hasErrors()).isTrue();
        assertThat(productionResult.errors()).contains("Production-only check failed");
    }
    
    @Test
    void testTestingModeSkipsProductionOnlyValidators() {
        PipelineConfig config = new PipelineConfig("test", null);
        
        // Testing mode should only run the all-modes validator
        ValidationResult testingResult = getValidator().validate(config, ValidationMode.TESTING);
        assertThat(testingResult.valid()).isTrue();
        assertThat(testingResult.hasErrors()).isFalse();
    }
    
    @Test
    void testDefaultModeIsProduction() {
        PipelineConfig config = new PipelineConfig("test", null);
        
        // Default validate() should use PRODUCTION mode
        ValidationResult defaultResult = getValidator().validate(config);
        ValidationResult productionResult = getValidator().validate(config, ValidationMode.PRODUCTION);
        
        assertThat(defaultResult.valid()).isEqualTo(productionResult.valid());
        assertThat(defaultResult.errors()).isEqualTo(productionResult.errors());
    }
}