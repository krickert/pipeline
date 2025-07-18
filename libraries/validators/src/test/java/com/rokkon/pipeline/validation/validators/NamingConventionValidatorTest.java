package com.rokkon.pipeline.validation.validators;

import com.rokkon.pipeline.engine.validation.validators.NamingConventionValidator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class NamingConventionValidatorTest extends NamingConventionValidatorTestBase {

    @Inject
    NamingConventionValidator validator;

    @Override
    protected NamingConventionValidator getValidator() {
        return validator;
    }
}