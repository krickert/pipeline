package com.rokkon.pipeline.engine.validation.validators;

import com.rokkon.pipeline.validation.validators.KafkaTopicNamingValidatorTestBase;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.BeforeEach;

/**
 * Integration test for KafkaTopicNamingValidator using @QuarkusIntegrationTest.
 * Tests the validator in production mode with a fully deployed Quarkus application.
 */
@QuarkusIntegrationTest
public class KafkaTopicNamingValidatorIT extends KafkaTopicNamingValidatorTestBase {
    
    private KafkaTopicNamingValidator validator;
    
    @BeforeEach
    void setup() {
        // In integration test, create validator manually since we're testing 
        // the production-built application
        validator = new KafkaTopicNamingValidator();
    }
    
    @Override
    protected KafkaTopicNamingValidator getValidator() {
        return validator;
    }
}