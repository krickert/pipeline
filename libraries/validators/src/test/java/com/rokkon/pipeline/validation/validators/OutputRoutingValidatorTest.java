package com.rokkon.pipeline.validation.validators;

import com.rokkon.pipeline.engine.validation.validators.OutputRoutingValidator;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class OutputRoutingValidatorTest extends OutputRoutingValidatorTestBase {

    @Inject
    OutputRoutingValidator validator;

    @Override
    protected OutputRoutingValidator getValidator() {
        return validator;
    }
}