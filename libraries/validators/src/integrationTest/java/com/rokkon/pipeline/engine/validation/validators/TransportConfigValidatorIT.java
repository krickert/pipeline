package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.TransportConfigValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@QuarkusIntegrationTest
public class TransportConfigValidatorIT extends TransportConfigValidatorTestBase {
    
    private TransportConfigValidator validator;
    
    @BeforeEach
    void setup() {
        validator = new TransportConfigValidator();
    }
    
    @Override
    protected TransportConfigValidator getValidator() {
        return validator;
    }
}