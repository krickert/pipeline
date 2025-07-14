package com.rokkon.search.util;

import com.rokkon.search.model.PipeDoc;
import com.rokkon.search.model.PipeStream;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test class for verifying ProtobufTestDataHelper loads data correctly.
 * This tests both from filesystem (unit test) and from JAR (integration test).
 */
public abstract class TestDataLoadingTestBase {

    /**
     * Get the ProtobufTestDataHelper instance.
     * Unit tests can inject it, integration tests should create it directly.
     */
    protected abstract ProtobufTestDataHelper getTestDataHelper();

    //@Test
    //@Test
    void testLoadSamplePipeDocuments() {
        Collection<PipeDoc> docs = getTestDataHelper().getSamplePipeDocuments();
        
        assertThat(docs)
            .as("Sample pipe documents should be loaded")
            .isNotEmpty()
            .hasSizeGreaterThan(10);
        
        // Verify document structure
        docs.stream().limit(5).forEach(doc -> {
            assertThat(doc.getId()).isNotEmpty();
            assertThat(doc.hasBody()).isTrue();
            assertThat(doc.getBody()).isNotEmpty();
        });
        
        System.out.println("Loaded " + docs.size() + " sample pipe documents");
    }

    @Test
    void testLoadTikaPipeDocuments() {
        Collection<PipeDoc> docs = getTestDataHelper().getTikaPipeDocuments();
        
        assertThat(docs)
            .as("Tika pipe documents should be loaded")
            .isNotEmpty()
            .hasSizeGreaterThan(10);
        
        // Tika docs should have body content
        docs.stream().limit(5).forEach(doc -> {
            assertThat(doc.getId()).isNotEmpty();
            assertThat(doc.hasBody()).isTrue();
            assertThat(doc.getBody()).isNotEmpty();
        });
        
        System.out.println("Loaded " + docs.size() + " tika pipe documents");
    }

    @Test
    void testLoadPipeStreams() {
        Collection<PipeStream> streams = getTestDataHelper().getPipeStreams();
        
        assertThat(streams)
            .as("Pipe streams should be loaded")
            .isNotEmpty()
            .hasSizeGreaterThan(10);
        
        // Verify stream structure
        streams.stream().limit(5).forEach(stream -> {
            assertThat(stream.getStreamId()).isNotEmpty();
            assertThat(stream.hasDocument()).isTrue();
            assertThat(stream.getDocument().getId()).isNotEmpty();
        });
        
        System.out.println("Loaded " + streams.size() + " pipe streams");
    }

    @Test
    void testLoadTikaRequests() {
        Collection<PipeStream> requests = getTestDataHelper().getTikaRequestStreams();
        
        assertThat(requests)
            .as("Tika request streams should be loaded")
            .isNotEmpty();
        
        // Tika requests should have documents with blobs
        requests.stream().limit(5).forEach(stream -> {
            assertThat(stream.hasDocument()).isTrue();
            assertThat(stream.getDocument().hasBlob()).isTrue();
        });
        
        System.out.println("Loaded " + requests.size() + " tika request streams");
    }

    @Test
    void testLoadTikaResponses() {
        Collection<PipeDoc> responses = getTestDataHelper().getTikaResponseDocuments();
        
        assertThat(responses)
            .as("Tika response documents should be loaded")
            .isNotEmpty();
        
        // Tika responses should have extracted text
        responses.stream().limit(5).forEach(doc -> {
            assertThat(doc.hasBody()).isTrue();
            assertThat(doc.getBody()).isNotEmpty();
        });
        
        System.out.println("Loaded " + responses.size() + " tika response documents");
    }

    @Test
    void testLoadChunkerDocuments() {
        Collection<PipeDoc> chunks = getTestDataHelper().getChunkerPipeDocuments();
        
        assertThat(chunks)
            .as("Chunker documents should be loaded")
            .isNotEmpty();
        
        System.out.println("Loaded " + chunks.size() + " chunker documents");
    }

    @Test
    void testCachingBehavior() {
        // First call
        Collection<PipeDoc> firstCall = getTestDataHelper().getSamplePipeDocuments();
        
        // Second call should return cached instance
        Collection<PipeDoc> secondCall = getTestDataHelper().getSamplePipeDocuments();
        
        assertThat(firstCall)
            .as("Cached collections should be the same instance")
            .isSameAs(secondCall);
    }

    @Test
    void testMapsArePopulated() {
        // Load documents
        Collection<PipeDoc> docs = getTestDataHelper().getSamplePipeDocuments();
        
        // Get map
        var docsMap = getTestDataHelper().getSamplePipeDocumentsMap();
        
        assertThat(docsMap)
            .as("Document map should contain all documents")
            .hasSize(docs.size());
        
        // Verify mapping
        docs.stream().limit(5).forEach(doc -> {
            assertThat(docsMap)
                .containsKey(doc.getId())
                .containsEntry(doc.getId(), doc);
        });
    }
}