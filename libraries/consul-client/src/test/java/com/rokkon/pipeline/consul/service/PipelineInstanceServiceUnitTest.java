package com.rokkon.pipeline.consul.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rokkon.pipeline.config.model.PipelineConfig;
import com.rokkon.pipeline.config.service.PipelineDefinitionService;
import com.rokkon.pipeline.config.service.PipelineInstanceService;
import com.rokkon.pipeline.testing.util.UnifiedTestProfile;
import com.rokkon.pipeline.validation.impl.EmptyValidationResult;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for PipelineInstanceService that uses mocked dependencies.
 * Tests service logic without requiring real Consul.
 */
@QuarkusTest
@TestProfile(UnifiedTestProfile.class)
class PipelineInstanceServiceUnitTest extends PipelineInstanceServiceTestBase {
    
    @Inject
    PipelineInstanceService pipelineInstanceServiceImpl;
    
    @Inject
    ObjectMapper objectMapper;
    
    @InjectMock
    ConsulClient consulClient;
    
    @InjectMock
    PipelineDefinitionService pipelineDefinitionServiceImpl;
    
    // Track stored values to simulate Consul KV store
    private final Map<String, String> kvStore = new HashMap<>();
    
    @Override
    void setupDependencies() {
        this.pipelineInstanceService = pipelineInstanceServiceImpl;
        this.pipelineDefinitionService = pipelineDefinitionServiceImpl;
    }
    
    @BeforeEach
    void setupMocks() {
        // Clear KV store before each test
        kvStore.clear();
        
        // Mock ConsulClient for getKeys
        Mockito.when(consulClient.getKeys(Mockito.anyString()))
            .thenAnswer(invocation -> {
                String prefix = invocation.getArgument(0);
                List<String> matchingKeys = kvStore.keySet().stream()
                    .filter(key -> key.startsWith(prefix))
                    .toList();
                return Uni.createFrom().item(matchingKeys);
            });
        
        // Mock ConsulClient for getValue
        Mockito.when(consulClient.getValue(Mockito.anyString()))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = kvStore.get(key);
                
                if (value != null) {
                    var keyValue = new io.vertx.ext.consul.KeyValue();
                    keyValue.setKey(key);
                    keyValue.setValue(value);
                    return Uni.createFrom().item(keyValue);
                }
                
                return Uni.createFrom().nullItem();
            });
        
        // Mock ConsulClient for putValue
        Mockito.when(consulClient.putValue(Mockito.anyString(), Mockito.anyString()))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String value = invocation.getArgument(1);
                kvStore.put(key, value);
                return Uni.createFrom().item(true);
            });
        
        // Mock ConsulClient for deleteValue
        Mockito.when(consulClient.deleteValue(Mockito.anyString()))
            .thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                kvStore.remove(key);
                return Uni.createFrom().voidItem();
            });
        
        // Mock PipelineDefinitionService
        Mockito.when(pipelineDefinitionServiceImpl.getDefinition(Mockito.anyString()))
            .thenAnswer(invocation -> {
                String defId = invocation.getArgument(0);
                
                // Check if it's one of our test definitions
                if (testDefinitionIds.contains(defId)) {
                    return Uni.createFrom().item(new PipelineConfig(
                        "Test Pipeline",
                        Map.of()
                    ));
                }
                
                return Uni.createFrom().nullItem();
            });
        
        Mockito.when(pipelineDefinitionServiceImpl.createDefinition(Mockito.anyString(), Mockito.any(PipelineConfig.class)))
            .thenReturn(Uni.createFrom().item(EmptyValidationResult.instance()));
        
        Mockito.when(pipelineDefinitionServiceImpl.deleteDefinition(Mockito.anyString()))
            .thenReturn(Uni.createFrom().item(EmptyValidationResult.instance()));
    }
}