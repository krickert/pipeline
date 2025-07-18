package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.config.model.PipelineConfig;
import com.rokkon.pipeline.api.validation.PipelineConfigValidator;
import com.rokkon.pipeline.api.validation.PipelineConfigValidatable;
import com.rokkon.pipeline.api.validation.ValidationResult;
import com.rokkon.pipeline.commons.validation.ValidationResultFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that there are no circular dependencies within a pipeline.
 * TODO: Implement full loop detection logic.
 */
@ApplicationScoped
public class IntraPipelineLoopValidator implements PipelineConfigValidator {
    
    @Override
    public ValidationResult validate(PipelineConfigValidatable validatable) {
        PipelineConfig config = (PipelineConfig) validatable;
        if (config == null || config.pipelineSteps() == null || config.pipelineSteps().isEmpty()) {
            return ValidationResultFactory.success();
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // TODO: Implement comprehensive loop detection
        // For now, just add a warning that this validation is not yet implemented
        // The old system had sophisticated graph traversal logic
        warnings.add("Intra-pipeline loop detection is not yet implemented");
        
        return errors.isEmpty() ? ValidationResultFactory.successWithWarnings(warnings) : ValidationResultFactory.failure(errors, warnings);
    }
    
    @Override
    public int getPriority() {
        return 600; // Run after reference validation
    }
    
    @Override
    public String getValidatorName() {
        return "IntraPipelineLoopValidator";
    }
}