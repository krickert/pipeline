package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.ProcessorInfoValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@QuarkusIntegrationTest
public class ProcessorInfoValidatorIT extends ProcessorInfoValidatorTestBase {
    
    private ProcessorInfoValidator validator;
    
    @BeforeEach
    void setup() {
        validator = new ProcessorInfoValidator();
    }
    
    @Override
    protected ProcessorInfoValidator getValidator() {
        return validator;
    }
}