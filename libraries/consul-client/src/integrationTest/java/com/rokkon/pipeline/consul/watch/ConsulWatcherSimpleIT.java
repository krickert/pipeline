package com.rokkon.pipeline.consul.watch;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.mutiny.ext.consul.ConsulClient;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.KeyValueList;
import io.vertx.ext.consul.ConsulClientOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified integration test for Consul watches to verify basic functionality.
 */
@ConsulIntegrationTest(namespacePrefix = "simple-watch-test")
class ConsulWatcherSimpleIT {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    private io.vertx.core.Vertx vertx;
    private ConsulClientOptions options;
    
    @BeforeEach
    void setup() {
        vertx = io.vertx.core.Vertx.vertx();
        
        // Get consul configuration from system properties
        String host = System.getProperty("consul.host", "localhost");
        int port = Integer.parseInt(System.getProperty("consul.port", "33610"));
        
        options = new ConsulClientOptions()
            .setHost(host)
            .setPort(port)
            .setTimeout(10000);
            
        System.out.println("Consul watch test setup - host: " + host + ", port: " + port);
    }
    
    @AfterEach
    void cleanup() {
        if (vertx != null) {
            vertx.close();
        }
    }
    
    @Test
    @Timeout(30)
    void testBasicWatch() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        String testKey = namespace + "/test-watch-key";
        
        System.out.println("Testing watch on key: " + testKey);
        
        // Create atomic flags and latch
        AtomicBoolean initialWatchFired = new AtomicBoolean(false);
        AtomicBoolean dataWatchFired = new AtomicBoolean(false);
        AtomicReference<String> watchedValue = new AtomicReference<>();
        CountDownLatch dataLatch = new CountDownLatch(1);
        
        // Create a simple key watch
        Watch<KeyValueList> watch = Watch.keyPrefix(testKey, vertx, options);
        
        watch.setHandler(result -> {
            System.out.println("Watch handler called - succeeded: " + result.succeeded());
            
            if (result.succeeded()) {
                KeyValueList kvList = result.nextResult();
                System.out.println("Watch result - index: " + (kvList != null ? kvList.getIndex() : "null"));
                
                if (kvList != null) {
                    if (kvList.getList() == null || kvList.getList().isEmpty()) {
                        System.out.println("Watch fired with empty list");
                        initialWatchFired.set(true);
                    } else {
                        System.out.println("Watch fired with data - size: " + kvList.getList().size());
                        kvList.getList().forEach(kv -> {
                            System.out.println("  Key: " + kv.getKey() + ", Value: " + kv.getValue());
                            if (kv.getKey().equals(testKey)) {
                                watchedValue.set(kv.getValue());
                                dataWatchFired.set(true);
                                dataLatch.countDown();
                            }
                        });
                    }
                }
            } else {
                System.err.println("Watch error: " + result.cause().getMessage());
                result.cause().printStackTrace();
            }
        });
        
        // Start the watch
        watch.start();
        System.out.println("Watch started");
        
        try {
            // Wait a bit for watch to establish
            Thread.sleep(1000);
            
            // Now put a value
            System.out.println("Putting value to key: " + testKey);
            String testValue = "test-value-" + System.currentTimeMillis();
            Boolean putResult = client.putValue(testKey, testValue)
                .await().atMost(Duration.ofSeconds(5));
            assertTrue(putResult, "Put should succeed");
            System.out.println("Value put successfully");
            
            // Wait for watch to fire
            boolean fired = dataLatch.await(15, TimeUnit.SECONDS);
            System.out.println("Watch fired: " + fired);
            
            assertTrue(fired, "Watch should fire within 15 seconds");
            assertTrue(dataWatchFired.get(), "Data watch should have fired");
            assertEquals(testValue, watchedValue.get(), "Watched value should match");
            
            // Clean up
            client.deleteValue(testKey)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
            System.out.println("Watch stopped");
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchWithExistingData() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        String testKey = namespace + "/existing-data-key";
        
        // First put some data
        String initialValue = "initial-value";
        client.putValue(testKey, initialValue)
            .await().atMost(Duration.ofSeconds(5));
        
        // Now create watch
        AtomicBoolean watchFired = new AtomicBoolean(false);
        AtomicReference<String> watchedValue = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        Watch<KeyValueList> watch = Watch.keyPrefix(testKey, vertx, options);
        watch.setHandler(result -> {
            if (result.succeeded()) {
                KeyValueList kvList = result.nextResult();
                if (kvList != null && kvList.getList() != null && !kvList.getList().isEmpty()) {
                    kvList.getList().forEach(kv -> {
                        if (kv.getKey().equals(testKey)) {
                            watchedValue.set(kv.getValue());
                            watchFired.set(true);
                            latch.countDown();
                        }
                    });
                }
            }
        }).start();
        
        try {
            // Watch should fire immediately with existing data
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Watch should fire for existing data");
            assertTrue(watchFired.get(), "Watch should have fired");
            assertEquals(initialValue, watchedValue.get(), "Should see initial value");
            
            // Clean up
            client.deleteValue(testKey)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
}