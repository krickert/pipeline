package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.StepTypeValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@QuarkusIntegrationTest
public class StepTypeValidatorIT extends StepTypeValidatorTestBase {
    
    private StepTypeValidator validator;
    
    @BeforeEach
    void setup() {
        validator = new StepTypeValidator();
    }
    
    @Override
    protected StepTypeValidator getValidator() {
        return validator;
    }
}