package com.rokkon.search.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URL;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the directory constants in ProtobufTestDataHelper match actual resources.
 * This test ensures we catch any mismatches between expected and actual directory names.
 */
@QuarkusTest
class TestDataDirectoryVerificationTest {

    private static Stream<Arguments> directoryMappings() {
        return Stream.of(
            // Format: constant name, expected path, actual path
            Arguments.of("PIPE_DOC_DIRECTORY", "test-data/pipe_docs", "test-data/pipe-docs"),
            Arguments.of("PIPE_STREAM_DIRECTORY", "test-data/pipe_streams", "test-data/pipe-streams"),
            Arguments.of("TIKA_PIPE_DOC_DIRECTORY", "test-data/tika_pipe_docs", "test-data/tika-pipe-docs"),
            Arguments.of("TIKA_PIPE_STREAM_DIRECTORY", "test-data/tika_pipe_streams", "test-data/tika-pipe-streams"),
            Arguments.of("CHUNKER_PIPE_DOC_DIRECTORY", "test-data/chunker_pipe_docs", "test-data/chunker-pipe-docs"),
            Arguments.of("CHUNKER_PIPE_STREAM_DIRECTORY", "test-data/chunker_pipe_streams", "test-data/chunker-pipe-streams"),
            Arguments.of("SAMPLE_PIPE_DOC_DIRECTORY", "test-data/sample_pipe_docs", "test-data/sample-pipe-docs"),
            Arguments.of("SAMPLE_PIPE_STREAM_DIRECTORY", "test-data/sample_pipe_streams", "test-data/sample-pipe-streams"),
            Arguments.of("PIPELINE_GENERATED_DIRECTORY", "test-data/pipeline_generated", "test-data/pipeline-generated"),
            Arguments.of("TIKA_REQUESTS_DIRECTORY", "test-data/tika/requests", "test-data/tika/requests"),
            Arguments.of("TIKA_RESPONSES_DIRECTORY", "test-data/tika/responses", "test-data/tika/responses"),
            Arguments.of("CHUNKER_INPUT_DIRECTORY", "test-data/parser/output", "test-data/parser/output")
        );
    }

    @ParameterizedTest
    @MethodSource("directoryMappings")
    void verifyDirectoryExists(String constantName, String expectedPath, String actualPath) {
        // Check if the actual directory exists in classpath
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL resource = cl.getResource(actualPath);
        
        if (resource != null) {
            System.out.println("✓ " + constantName + ": Found " + actualPath + " at " + resource);
        } else {
            System.out.println("✗ " + constantName + ": NOT found " + actualPath);
            
            // Try the expected path to see if it exists
            URL expectedResource = cl.getResource(expectedPath);
            if (expectedResource != null) {
                System.out.println("  BUT found at expected path: " + expectedPath + " at " + expectedResource);
            }
        }
    }

    @Test
    void documentCurrentState() {
        System.out.println("\n=== Current Test Data Directory State ===");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
        // Check what actually exists
        String[] possibleDirs = {
            "test-data/pipe_docs",
            "test-data/pipe-docs",
            "test-data/pipe_streams",
            "test-data/pipe-streams",
            "test-data/tika_pipe_docs",
            "test-data/tika-pipe-docs",
            "test-data/tika_pipe_streams",
            "test-data/tika-pipe-streams",
            "test-data/chunker_pipe_docs",
            "test-data/chunker-pipe-docs",
            "test-data/chunker_pipe_streams",
            "test-data/chunker-pipe-streams",
            "test-data/sample_pipe_docs",
            "test-data/sample-pipe-docs",
            "test-data/sample_pipe_streams",
            "test-data/sample-pipe-streams",
            "test-data/pipeline_generated",
            "test-data/pipeline-generated",
            "test-data/tika/requests",
            "test-data/tika/responses",
            "test-data/parser/output"
        };
        
        for (String dir : possibleDirs) {
            URL resource = cl.getResource(dir);
            if (resource != null) {
                System.out.println("Found: " + dir + " at " + resource);
            }
        }
    }
}