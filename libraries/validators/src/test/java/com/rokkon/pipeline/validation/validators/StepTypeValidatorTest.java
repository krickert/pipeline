package com.rokkon.pipeline.validation.validators;

import com.rokkon.pipeline.engine.validation.validators.StepTypeValidator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class StepTypeValidatorTest extends StepTypeValidatorTestBase {
    
    @Inject
    StepTypeValidator validator;
    
    @Override
    protected StepTypeValidator getValidator() {
        return validator;
    }
}