# Integration Test App

This application serves as the integration test environment for the pipeline project. It provides a running Quarkus application for testing extensions and libraries without requiring the full engine infrastructure.

## Purpose

- **Integration Testing**: Primary location for @QuarkusIntegrationTest tests that verify packaged JAR behavior
- **Development Mode**: Instant dev mode environment for testing without the full engine
- **Extension Testing**: Validates extensions like consul-devservices work correctly
- **Template**: Serves as a template for creating integration test scenarios

## Running

### Development Mode
```bash
./gradlew :integration-test-app:quarkusDev
```

### Run Integration Tests
```bash
./gradlew :integration-test-app:quarkusIntTest
```

### Build Application
```bash
./gradlew :integration-test-app:build
```

## Test Structure

Integration tests are located in `src/integrationTest/java` and use the `@ConsulQuarkusIntegrationTest` annotation for Consul-based tests.

Example:
```java
@ConsulQuarkusIntegrationTest
public class MyIntegrationIT {
    @ConsulTest
    ConsulTestContext consulContext;
    
    @Test
    public void testFeature() {
        // Test against packaged application
    }
}
```

Note: @QuarkusIntegrationTest does not support CDI injection, but the @ConsulTest field injection works via the JUnit extension.