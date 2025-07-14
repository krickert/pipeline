package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.InterPipelineLoopValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@QuarkusIntegrationTest
public class InterPipelineLoopValidatorIT extends InterPipelineLoopValidatorTestBase {
    
    private InterPipelineLoopValidator validator;
    
    @BeforeEach
    void setup() {
        validator = new InterPipelineLoopValidator();
    }
    
    @Override
    protected InterPipelineLoopValidator getValidator() {
        return validator;
    }
}