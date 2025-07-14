package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.NamingConventionValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

@QuarkusIntegrationTest
public class NamingConventionValidatorIT extends NamingConventionValidatorTestBase {

    private NamingConventionValidator validator;

    @BeforeEach
    void setup() {
        validator = new NamingConventionValidator();
    }

    @Override
    protected NamingConventionValidator getValidator() {
        return validator;
    }
}