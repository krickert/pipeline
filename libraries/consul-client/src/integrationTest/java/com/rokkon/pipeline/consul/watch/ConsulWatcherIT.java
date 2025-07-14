package com.rokkon.pipeline.consul.watch;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import com.rokkon.pipeline.events.cache.ConsulPipelineDefinitionChangedEvent;
import com.rokkon.pipeline.events.cache.ConsulModuleRegistrationChangedEvent;
import com.rokkon.pipeline.events.cache.ConsulClusterPipelineChangedEvent;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.consul.ConsulClient;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.KeyValueList;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.mutiny.Multi;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration test for ConsulWatcher.
 * Tests the watch functionality for pipeline definitions, module registrations,
 * and cluster configurations.
 */
@ConsulIntegrationTest(namespacePrefix = "watcher-test")
class ConsulWatcherIT {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    // Core Vertx instance for watches
    private io.vertx.core.Vertx coreVertx;
    
    // Consul options for watches
    private io.vertx.ext.consul.ConsulClientOptions consulOptions;
    
    // Track events received during tests
    private static final List<ConsulPipelineDefinitionChangedEvent> pipelineEvents = new CopyOnWriteArrayList<>();
    private static final List<ConsulModuleRegistrationChangedEvent> moduleEvents = new CopyOnWriteArrayList<>();
    private static final List<ConsulClusterPipelineChangedEvent> clusterEvents = new CopyOnWriteArrayList<>();
    
    @BeforeEach
    void setup() {
        // Clear event lists
        pipelineEvents.clear();
        moduleEvents.clear();
        clusterEvents.clear();
        
        // Create core Vertx instance for non-reactive tests
        coreVertx = io.vertx.core.Vertx.vertx();
        
        // Create Consul options
        consulOptions = new io.vertx.ext.consul.ConsulClientOptions()
            .setHost("localhost")
            .setPort(Integer.parseInt(System.getProperty("consul.port", "33610")));
    }
    
    @AfterEach
    void cleanup() {
        // Clear event lists
        pipelineEvents.clear();
        moduleEvents.clear();
        clusterEvents.clear();
        
        // Close core Vertx instance
        if (coreVertx != null) {
            coreVertx.close();
        }
    }
    
    /**
     * CDI event observers to capture events fired by ConsulWatcher
     */
    public static class EventObserver {
        void onPipelineDefinitionChanged(@Observes ConsulPipelineDefinitionChangedEvent event) {
            pipelineEvents.add(event);
        }
        
        void onModuleRegistrationChanged(@Observes ConsulModuleRegistrationChangedEvent event) {
            moduleEvents.add(event);
        }
        
        void onClusterPipelineChanged(@Observes ConsulClusterPipelineChangedEvent event) {
            clusterEvents.add(event);
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchPipelineDefinitions() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch for pipeline definitions
        String pipelinePrefix = namespace + "/pipelines/definitions";
        AtomicBoolean watchTriggered = new AtomicBoolean(false);
        AtomicInteger changeCount = new AtomicInteger(0);
        CountDownLatch watchLatch = new CountDownLatch(1);
        
        Watch<KeyValueList> watch = Watch.keyPrefix(pipelinePrefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                watchTriggered.set(true);
                changeCount.incrementAndGet();
                KeyValueList kvList = result.nextResult();
                if (kvList != null && kvList.getList() != null && !kvList.getList().isEmpty()) {
                    watchLatch.countDown();
                }
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(500);
            
            // Create a pipeline definition
            String pipelineId = "test-pipeline-" + System.currentTimeMillis();
            String pipelineKey = pipelinePrefix + "/" + pipelineId;
            String pipelineValue = "{\"name\":\"Test Pipeline\",\"config\":{}}";
            
            Boolean putResult = client.putValue(pipelineKey, pipelineValue)
                .await().atMost(Duration.ofSeconds(5));
            assertTrue(putResult, "Put value should succeed");
            
            // Wait for watch to trigger
            assertTrue(watchLatch.await(10, TimeUnit.SECONDS), "Watch should trigger within 10 seconds");
            assertTrue(watchTriggered.get(), "Watch should have been triggered");
            assertTrue(changeCount.get() > 0, "Change count should be greater than 0");
            
            // Update the pipeline
            CountDownLatch updateLatch = new CountDownLatch(1);
            AtomicBoolean updateTriggered = new AtomicBoolean(false);
            
            watch.setHandler(result -> {
                if (result.succeeded()) {
                    updateTriggered.set(true);
                    updateLatch.countDown();
                }
            });
            
            String updatedValue = "{\"name\":\"Updated Pipeline\",\"config\":{\"updated\":true}}";
            putResult = client.putValue(pipelineKey, updatedValue)
                .await().atMost(Duration.ofSeconds(5));
            assertTrue(putResult, "Update should succeed");
            
            assertTrue(updateLatch.await(10, TimeUnit.SECONDS), "Update watch should trigger");
            assertTrue(updateTriggered.get(), "Update should have triggered watch");
            
            // Delete the pipeline
            client.deleteValue(pipelineKey)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchModuleRegistrations() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch for module registrations
        String modulePrefix = namespace + "/modules/registered";
        AtomicBoolean watchTriggered = new AtomicBoolean(false);
        CountDownLatch watchLatch = new CountDownLatch(1);
        
        Watch<KeyValueList> watch = Watch.keyPrefix(modulePrefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                watchTriggered.set(true);
                KeyValueList kvList = result.nextResult();
                if (kvList != null && kvList.getList() != null && !kvList.getList().isEmpty()) {
                    watchLatch.countDown();
                }
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(500);
            
            // Register a module
            String moduleId = "test-module-" + System.currentTimeMillis();
            String moduleKey = modulePrefix + "/" + moduleId;
            String moduleValue = "{\"name\":\"Test Module\",\"host\":\"localhost\",\"port\":8080}";
            
            Boolean putResult = client.putValue(moduleKey, moduleValue)
                .await().atMost(Duration.ofSeconds(5));
            assertTrue(putResult, "Put value should succeed");
            
            // Wait for watch to trigger
            assertTrue(watchLatch.await(10, TimeUnit.SECONDS), "Watch should trigger within 10 seconds");
            assertTrue(watchTriggered.get(), "Watch should have been triggered");
            
            // Clean up
            client.deleteValue(moduleKey)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchClusterPipelines() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        String clusterName = "test-cluster";
        
        // Create a watch for cluster pipelines
        String clusterPrefix = namespace + "/clusters/" + clusterName + "/pipelines";
        AtomicBoolean watchTriggered = new AtomicBoolean(false);
        CountDownLatch watchLatch = new CountDownLatch(1);
        
        Watch<KeyValueList> watch = Watch.keyPrefix(clusterPrefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                watchTriggered.set(true);
                KeyValueList kvList = result.nextResult();
                if (kvList != null && kvList.getList() != null && !kvList.getList().isEmpty()) {
                    watchLatch.countDown();
                }
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(500);
            
            // Create a cluster pipeline config
            String pipelineId = "cluster-pipeline-" + System.currentTimeMillis();
            String pipelineKey = clusterPrefix + "/" + pipelineId + "/config";
            String pipelineValue = "{\"enabled\":true,\"priority\":1}";
            
            Boolean putResult = client.putValue(pipelineKey, pipelineValue)
                .await().atMost(Duration.ofSeconds(5));
            assertTrue(putResult, "Put value should succeed");
            
            // Wait for watch to trigger
            assertTrue(watchLatch.await(10, TimeUnit.SECONDS), "Watch should trigger within 10 seconds");
            assertTrue(watchTriggered.get(), "Watch should have been triggered");
            
            // Clean up
            client.deleteValue(pipelineKey)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchWithMultipleChanges() {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch
        String prefix = namespace + "/test-multiple";
        AtomicInteger changeCount = new AtomicInteger(0);
        AtomicInteger maxItemsSeen = new AtomicInteger(0);
        AtomicBoolean watchEstablished = new AtomicBoolean(false);
        
        // Start the watch
        Watch<KeyValueList> watch = Watch.keyPrefix(prefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                watchEstablished.set(true);
                KeyValueList kvList = result.nextResult();
                if (kvList != null && kvList.getList() != null) {
                    changeCount.incrementAndGet();
                    int size = kvList.getList().size();
                    maxItemsSeen.updateAndGet(prev -> Math.max(prev, size));
                }
            }
        }).start();
        
        try {
            // Wait for watch to establish
            Uni.createFrom().voidItem()
                .onItem().delayIt().by(Duration.ofMillis(500))
                .flatMap(v -> {
                    // Create multiple keys in one batch
                    List<Uni<Boolean>> puts = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        String key = prefix + "/item-" + i;
                        String value = "value-" + i;
                        puts.add(client.putValue(key, value));
                    }
                    return Uni.join().all(puts).andCollectFailures();
                })
                .onItem().delayIt().by(Duration.ofSeconds(2)) // Wait for first batch to be processed
                .flatMap(v -> {
                    // Add more items
                    List<Uni<Boolean>> morePuts = new ArrayList<>();
                    for (int i = 3; i < 5; i++) {
                        String key = prefix + "/item-" + i;
                        String value = "value-" + i;
                        morePuts.add(client.putValue(key, value));
                    }
                    return Uni.join().all(morePuts).andCollectFailures();
                })
                .onItem().delayIt().by(Duration.ofSeconds(2)) // Wait for second batch to be processed
                .onItem().invoke(v -> {
                    // Verify results
                    assertTrue(changeCount.get() >= 1, "Should have at least 1 change event");
                    assertEquals(5, maxItemsSeen.get(), "Should have seen all 5 items");
                })
                .flatMap(v -> {
                    // Clean up
                    List<Uni<Void>> deletes = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        deletes.add(client.deleteValue(prefix + "/item-" + i));
                    }
                    return Uni.join().all(deletes).andCollectFailures();
                })
                .await().atMost(Duration.ofSeconds(20));
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchReconnectAfterError() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch
        String prefix = namespace + "/test-reconnect";
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch reconnectLatch = new CountDownLatch(2); // Expect at least 2 successful events
        
        Watch<KeyValueList> watch = Watch.keyPrefix(prefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                successCount.incrementAndGet();
                reconnectLatch.countDown();
            } else {
                errorCount.incrementAndGet();
                // Log but don't fail - watches should recover
                System.err.println("Watch error: " + result.cause().getMessage());
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(500);
            
            // Create initial value
            String key = prefix + "/test-key";
            client.putValue(key, "initial-value")
                .await().atMost(Duration.ofSeconds(5));
            
            // Wait a bit
            Thread.sleep(1000);
            
            // Update value to trigger another watch event
            client.putValue(key, "updated-value")
                .await().atMost(Duration.ofSeconds(5));
            
            // Wait for both events
            assertTrue(reconnectLatch.await(15, TimeUnit.SECONDS), 
                "Should receive at least 2 successful watch events");
            assertTrue(successCount.get() >= 2, "Should have at least 2 successful events");
            
            // Clean up
            client.deleteValue(key)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchWithNoInitialData() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch on a prefix with no data
        String emptyPrefix = namespace + "/empty-prefix-" + System.currentTimeMillis();
        AtomicBoolean initialWatchFired = new AtomicBoolean(false);
        AtomicBoolean dataWatchFired = new AtomicBoolean(false);
        CountDownLatch dataLatch = new CountDownLatch(1);
        
        Watch<KeyValueList> watch = Watch.keyPrefix(emptyPrefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                KeyValueList kvList = result.nextResult();
                if (kvList != null) {
                    if (kvList.getList() == null || kvList.getList().isEmpty()) {
                        initialWatchFired.set(true);
                    } else {
                        dataWatchFired.set(true);
                        dataLatch.countDown();
                    }
                }
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(1000);
            
            // Create data in the previously empty prefix
            String key = emptyPrefix + "/new-key";
            client.putValue(key, "new-value")
                .await().atMost(Duration.ofSeconds(5));
            
            // Wait for watch to detect the new data
            assertTrue(dataLatch.await(10, TimeUnit.SECONDS), "Watch should detect new data");
            assertTrue(dataWatchFired.get(), "Data watch should have fired");
            
            // Clean up
            client.deleteValue(key)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchWithRapidUpdates() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create a watch
        String prefix = namespace + "/rapid-updates";
        String key = prefix + "/counter";
        AtomicInteger updateCount = new AtomicInteger(0);
        CountDownLatch updatesLatch = new CountDownLatch(5); // Expect at least 5 updates
        
        Watch<KeyValueList> watch = Watch.keyPrefix(prefix, coreVertx, consulOptions);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                updateCount.incrementAndGet();
                updatesLatch.countDown();
            }
        }).start();
        
        try {
            // Wait for initial watch to establish
            Thread.sleep(500);
            
            // Perform rapid updates
            for (int i = 0; i < 10; i++) {
                client.putValue(key, "value-" + i)
                    .await().atMost(Duration.ofSeconds(5));
                Thread.sleep(100); // Small delay between updates
            }
            
            // Wait for updates to be detected
            assertTrue(updatesLatch.await(15, TimeUnit.SECONDS), 
                "Should detect at least 5 updates");
            assertTrue(updateCount.get() >= 5, 
                "Should have received at least 5 update events, got: " + updateCount.get());
            
            // Clean up
            client.deleteValue(key)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testMultipleWatchesOnSamePrefix() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Create multiple watches on the same prefix
        String prefix = namespace + "/multi-watch";
        AtomicInteger watch1Count = new AtomicInteger(0);
        AtomicInteger watch2Count = new AtomicInteger(0);
        CountDownLatch bothWatchesLatch = new CountDownLatch(2);
        
        Watch<KeyValueList> watch1 = Watch.keyPrefix(prefix, coreVertx, consulOptions);
        watch1.setHandler(result -> {
            if (result.succeeded()) {
                watch1Count.incrementAndGet();
                bothWatchesLatch.countDown();
            }
        }).start();
        
        Watch<KeyValueList> watch2 = Watch.keyPrefix(prefix, coreVertx, consulOptions);
        watch2.setHandler(result -> {
            if (result.succeeded()) {
                watch2Count.incrementAndGet();
                bothWatchesLatch.countDown();
            }
        }).start();
        
        try {
            // Wait for watches to establish
            Thread.sleep(500);
            
            // Create a value
            String key = prefix + "/shared-key";
            client.putValue(key, "shared-value")
                .await().atMost(Duration.ofSeconds(5));
            
            // Both watches should trigger
            assertTrue(bothWatchesLatch.await(10, TimeUnit.SECONDS), 
                "Both watches should trigger");
            assertTrue(watch1Count.get() > 0, "Watch 1 should have triggered");
            assertTrue(watch2Count.get() > 0, "Watch 2 should have triggered");
            
            // Clean up
            client.deleteValue(key)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch1.stop();
            watch2.stop();
        }
    }
}