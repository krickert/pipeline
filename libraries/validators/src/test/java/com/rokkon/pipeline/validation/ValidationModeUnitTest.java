package com.rokkon.pipeline.validation;

import com.rokkon.pipeline.api.validation.ConfigValidator;
import com.rokkon.pipeline.api.validation.ValidationMode;
import com.rokkon.pipeline.api.validation.ValidationResult;
import com.rokkon.pipeline.config.model.PipelineConfig;
import com.rokkon.pipeline.engine.validation.CompositeValidator;
import com.rokkon.pipeline.testing.util.UnifiedTestProfile;
import com.rokkon.pipeline.commons.validation.ValidationResultFactory;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

@QuarkusTest
@TestProfile(UnifiedTestProfile.class)
class ValidationModeUnitTest extends ValidationModeTestBase {

    private CompositeValidator<PipelineConfig> validator;

    @BeforeEach
    void setup() {
        // Create a simple validator that supports all modes
        ConfigValidator<PipelineConfig> allModesValidator = new ConfigValidator<PipelineConfig>() {
            @Override
            public ValidationResult validate(PipelineConfig config) {
                if (config == null) {
                    return ValidationResultFactory.failure("Config is null");
                }
                return ValidationResultFactory.success();
            }

            @Override
            public String getValidatorName() {
                return "AllModesValidator";
            }
        };

        // Create a validator that only runs in PRODUCTION mode
        ConfigValidator<PipelineConfig> productionOnlyValidator = new ConfigValidator<PipelineConfig>() {
            @Override
            public ValidationResult validate(PipelineConfig config) {
                return ValidationResultFactory.failure("Production-only check failed");
            }

            @Override
            public String getValidatorName() {
                return "ProductionOnlyValidator";
            }

            @Override
            public Set<ValidationMode> supportedModes() {
                return Set.of(ValidationMode.PRODUCTION);
            }
        };

        // Create composite validator with both
        validator = new CompositeValidator<>("TestComposite",
            List.of(allModesValidator, productionOnlyValidator));
    }

    @Override
    protected CompositeValidator<PipelineConfig> getValidator() {
        return validator;
    }
}