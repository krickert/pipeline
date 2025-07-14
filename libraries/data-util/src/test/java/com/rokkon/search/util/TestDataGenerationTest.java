package com.rokkon.search.util;

import com.rokkon.search.model.ActionType;
import com.rokkon.search.model.PipeDoc;
import com.rokkon.search.model.PipeStream;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test demonstrates how to properly load protobuf classes in Quarkus.
 * It uses reflection to load the classes, which is required due to Quarkus's
 * strict classloading mechanism.
 * 
 * It also tests the TestDataBuffer class, which is a replacement for ProcessingBuffer
 * from commons:util to avoid circular dependencies.
 */
@QuarkusTest
public class TestDataGenerationTest {
    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerationTest.class);

    @Inject
    @Named("outputBuffer")
    TestDataBuffer<PipeDoc> outputBuffer;

    @Inject
    @Named("inputBuffer")
    TestDataBuffer<PipeStream> inputBuffer;

    @Test
    void testProtobufClassLoading() {
        try {
            // !!!! CRITICAL - DO NOT DELETE THIS FORCE LOADING !!!!
            // Force load the classes using the current thread's context classloader
            // This is REQUIRED due to Quarkus classloading timing issues in tests
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class<?> pipeDocClass = cl.loadClass("com.rokkon.search.model.PipeDoc");
            Class<?> pipeStreamClass = cl.loadClass("com.rokkon.search.model.PipeStream");

            // Verify that the classes were loaded successfully
            assertThat(pipeDocClass).isNotNull();
            assertThat(pipeStreamClass).isNotNull();

            // Log success
            logger.info("Successfully loaded PipeDoc and PipeStream classes using reflection");

            // This test will pass if the classes are loaded successfully
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load protobuf classes", e);
            throw new RuntimeException("Failed to load protobuf classes", e);
        }
    }

    @Test
    void testDataBuffers() {
        // Verify that the buffers were injected successfully
        assertThat(outputBuffer).isNotNull();
        assertThat(inputBuffer).isNotNull();

        // Verify that the buffers are empty (since we haven't added anything)
        assertThat(outputBuffer.size()).isEqualTo(0);
        assertThat(inputBuffer.size()).isEqualTo(0);

        logger.info("Successfully injected and verified TestDataBuffer instances");
    }

    @Test
    void testProtobufCreationAndUsage() {
        // Create a sample PipeDoc
        PipeDoc doc = PipeDoc.newBuilder()
                .setId("test-doc-1")
                .setTitle("Test Document")
                .setBody("This is a test document body")
                .setSourceUri("http://example.com/doc1")
                .build();

        // Verify the protobuf was created correctly
        assertThat(doc).isNotNull();
        assertThat(doc.getId()).isEqualTo("test-doc-1");
        assertThat(doc.getTitle()).isEqualTo("Test Document");
        assertThat(doc.getBody()).isEqualTo("This is a test document body");
        assertThat(doc.getSourceUri()).isEqualTo("http://example.com/doc1");

        // Add it to the output buffer
        outputBuffer.add(doc);
        assertThat(outputBuffer.size()).isEqualTo(1);

        // Create a sample PipeStream
        PipeStream stream = PipeStream.newBuilder()
                .setStreamId("test-stream-1")
                .setDocument(doc)
                .setCurrentPipelineName("test-pipeline")
                .setTargetStepName("test-step")
                .setCurrentHopNumber(1)
                .setActionType(ActionType.CREATE)
                .build();

        // Verify the protobuf was created correctly
        assertThat(stream).isNotNull();
        assertThat(stream.getStreamId()).isEqualTo("test-stream-1");
        assertThat(stream.getDocument()).isEqualTo(doc);
        assertThat(stream.getCurrentPipelineName()).isEqualTo("test-pipeline");
        assertThat(stream.getTargetStepName()).isEqualTo("test-step");
        assertThat(stream.getCurrentHopNumber()).isEqualTo(1);
        assertThat(stream.getActionType()).isEqualTo(ActionType.CREATE);

        // Add it to the input buffer
        inputBuffer.add(stream);
        assertThat(inputBuffer.size()).isEqualTo(1);

        // Verify we can get snapshots
        var docSnapshot = outputBuffer.snapshot();
        assertThat(docSnapshot).hasSize(1);
        assertThat(docSnapshot.get(0).getId()).isEqualTo("test-doc-1");

        var streamSnapshot = inputBuffer.snapshot();
        assertThat(streamSnapshot).hasSize(1);
        assertThat(streamSnapshot.get(0).getStreamId()).isEqualTo("test-stream-1");

        logger.info("Successfully created and used PipeDoc and PipeStream protobuf objects");
    }
}
