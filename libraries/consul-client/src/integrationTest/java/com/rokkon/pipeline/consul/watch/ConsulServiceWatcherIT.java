package com.rokkon.pipeline.consul.watch;

import com.rokkon.pipeline.consul.test.ConsulIntegrationTest;
import com.rokkon.pipeline.consul.test.ConsulTest;
import com.rokkon.pipeline.consul.test.ConsulTestContext;
import com.rokkon.pipeline.events.cache.ConsulModuleHealthChanged;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.vertx.mutiny.ext.consul.ConsulClient;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.CheckStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ConsulServiceWatcher.
 * Tests the service health monitoring functionality.
 */
@ConsulIntegrationTest(namespacePrefix = "service-watcher-test")
class ConsulServiceWatcherIT {
    
    @ConsulTest
    private ConsulTestContext consul;
    
    private io.vertx.core.Vertx vertx;
    private ConsulClientOptions options;
    
    // Track events that would be fired
    private final List<ConsulModuleHealthChanged> healthEvents = new CopyOnWriteArrayList<>();
    
    @BeforeEach
    void setup() {
        vertx = io.vertx.core.Vertx.vertx();
        healthEvents.clear();
        
        // Get consul configuration from system properties
        String host = System.getProperty("consul.host", "localhost");
        int port = Integer.parseInt(System.getProperty("consul.port", "33610"));
        
        options = new ConsulClientOptions()
            .setHost(host)
            .setPort(port)
            .setTimeout(10000);
            
        System.out.println("Service watcher test setup - host: " + host + ", port: " + port);
    }
    
    @AfterEach
    void cleanup() {
        healthEvents.clear();
        if (vertx != null) {
            vertx.close();
        }
    }
    
    @Test
    @Timeout(30)
    void testServiceRegistrationAndWatch() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Register a module service (without health check for now)
        String serviceId = namespace + "-test-module-1";
        String serviceName = "test-module";
        
        ServiceOptions service = new ServiceOptions()
            .setId(serviceId)
            .setName(serviceName)
            .setTags(List.of("module", "test"))
            .setAddress("localhost")
            .setPort(8080);
        
        // Register the service
        client.registerService(service)
            .await().atMost(Duration.ofSeconds(5));
        
        System.out.println("Service registered: " + serviceId);
        
        // Create a watch for the service
        AtomicBoolean watchFired = new AtomicBoolean(false);
        AtomicBoolean moduleTagFound = new AtomicBoolean(false);
        CountDownLatch serviceLatch = new CountDownLatch(1);
        
        // Watch service endpoint for the service
        Watch<ServiceEntryList> watch = Watch.service(serviceName, vertx, options);
        
        watch.setHandler(result -> {
            System.out.println("Service watch handler called - succeeded: " + result.succeeded());
            
            if (result.succeeded()) {
                ServiceEntryList entryList = result.nextResult();
                if (entryList != null && entryList.getList() != null) {
                    System.out.println("Service entries count: " + entryList.getList().size());
                    
                    for (ServiceEntry entry : entryList.getList()) {
                        if (entry.getService().getId().equals(serviceId)) {
                            System.out.println("Found our service entry");
                            watchFired.set(true);
                            
                            // Check if it has the module tag
                            if (entry.getService().getTags() != null && 
                                entry.getService().getTags().contains("module")) {
                                System.out.println("Service has module tag");
                                moduleTagFound.set(true);
                                serviceLatch.countDown();
                            }
                        }
                    }
                }
            } else {
                System.err.println("Watch error: " + result.cause().getMessage());
            }
        });
        
        watch.start();
        System.out.println("Service watch started");
        
        try {
            // Wait for watch to detect the service
            assertTrue(serviceLatch.await(15, TimeUnit.SECONDS), "Watch should detect service");
            assertTrue(watchFired.get(), "Watch should have fired");
            assertTrue(moduleTagFound.get(), "Module tag should be found");
            
        } finally {
            // Clean up
            watch.stop();
            client.deregisterService(serviceId)
                .await().atMost(Duration.ofSeconds(5));
            System.out.println("Service deregistered and watch stopped");
        }
    }
    
    @Test
    @Timeout(30)
    void testMultipleModuleServices() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        // Register multiple module services
        List<String> serviceIds = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String serviceId = namespace + "-test-module-" + i;
            serviceIds.add(serviceId);
            
            ServiceOptions service = new ServiceOptions()
                .setId(serviceId)
                .setName("test-module-group")
                .setTags(List.of("module", "test"))
                .setAddress("localhost")
                .setPort(8080 + i);
            
            client.registerService(service)
                .await().atMost(Duration.ofSeconds(5));
        }
        
        System.out.println("Registered " + serviceIds.size() + " services");
        
        // Watch the service group
        AtomicInteger moduleCount = new AtomicInteger(0);
        CountDownLatch allServicesLatch = new CountDownLatch(1);
        
        Watch<ServiceEntryList> watch = Watch.service("test-module-group", vertx, options);
        
        watch.setHandler(result -> {
            if (result.succeeded()) {
                ServiceEntryList entryList = result.nextResult();
                if (entryList != null && entryList.getList() != null) {
                    long count = entryList.getList().stream()
                        .filter(entry -> entry.getService().getTags() != null &&
                                        entry.getService().getTags().contains("module"))
                        .count();
                    
                    System.out.println("Found " + count + " module services");
                    moduleCount.set((int) count);
                    
                    if (count >= 3) {
                        allServicesLatch.countDown();
                    }
                }
            }
        });
        
        watch.start();
        
        try {
            // Wait for all services to be detected
            assertTrue(allServicesLatch.await(15, TimeUnit.SECONDS), 
                "Should detect all module services");
            assertEquals(3, moduleCount.get(), "Should find 3 module services");
            
        } finally {
            // Clean up
            watch.stop();
            for (String serviceId : serviceIds) {
                client.deregisterService(serviceId)
                    .await().atMost(Duration.ofSeconds(5));
            }
        }
    }
    
    @Test
    @Timeout(30)
    void testServiceRemoval() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        String serviceId = namespace + "-removal-test";
        String serviceName = "removal-test-module";
        
        ServiceOptions service = new ServiceOptions()
            .setId(serviceId)
            .setName(serviceName)
            .setTags(List.of("module"))
            .setAddress("localhost")
            .setPort(9090);
        
        // Register service
        client.registerService(service)
            .await().atMost(Duration.ofSeconds(5));
        
        // Watch for the service
        AtomicBoolean serviceDetected = new AtomicBoolean(false);
        AtomicBoolean serviceRemoved = new AtomicBoolean(false);
        CountDownLatch detectedLatch = new CountDownLatch(1);
        CountDownLatch removedLatch = new CountDownLatch(1);
        
        Watch<ServiceEntryList> watch = Watch.service(serviceName, vertx, options);
        
        watch.setHandler(result -> {
            if (result.succeeded()) {
                ServiceEntryList entryList = result.nextResult();
                if (entryList != null && entryList.getList() != null) {
                    boolean found = entryList.getList().stream()
                        .anyMatch(entry -> entry.getService().getId().equals(serviceId));
                    
                    if (found && !serviceDetected.get()) {
                        serviceDetected.set(true);
                        detectedLatch.countDown();
                        System.out.println("Service detected");
                    } else if (!found && serviceDetected.get()) {
                        serviceRemoved.set(true);
                        removedLatch.countDown();
                        System.out.println("Service removed");
                    }
                }
            }
        });
        
        watch.start();
        
        try {
            // Wait for service to be detected
            assertTrue(detectedLatch.await(10, TimeUnit.SECONDS), 
                "Service should be detected");
            assertTrue(serviceDetected.get(), "Service detected flag should be true");
            
            // Deregister the service
            client.deregisterService(serviceId)
                .await().atMost(Duration.ofSeconds(5));
            
            // Wait for removal to be detected
            assertTrue(removedLatch.await(15, TimeUnit.SECONDS), 
                "Service removal should be detected");
            assertTrue(serviceRemoved.get(), "Service removed flag should be true");
            
        } finally {
            watch.stop();
        }
    }
    
    @Test
    @Timeout(30)
    void testWatchMultipleServiceUpdates() throws InterruptedException {
        ConsulClient client = consul.consulClient();
        String namespace = consul.namespace();
        
        String serviceName = "update-test-module";
        
        // Track updates
        AtomicInteger updateCount = new AtomicInteger(0);
        CountDownLatch firstUpdateLatch = new CountDownLatch(1);
        CountDownLatch secondUpdateLatch = new CountDownLatch(1);
        
        Watch<ServiceEntryList> watch = Watch.service(serviceName, vertx, options);
        
        watch.setHandler(result -> {
            if (result.succeeded()) {
                ServiceEntryList entryList = result.nextResult();
                if (entryList != null && entryList.getList() != null) {
                    System.out.println("Watch update - services: " + entryList.getList().size());
                    
                    if (!entryList.getList().isEmpty()) {
                        int count = updateCount.incrementAndGet();
                        System.out.println("Update count: " + count);
                        
                        if (count == 1) {
                            firstUpdateLatch.countDown();
                        }
                        if (entryList.getList().size() >= 2) {
                            secondUpdateLatch.countDown();
                        }
                    }
                }
            } else {
                System.err.println("Watch error: " + result.cause().getMessage());
            }
        });
        
        watch.start();
        System.out.println("Watch started for service: " + serviceName);
        
        try {
            // Wait a bit
            Thread.sleep(1000);
            
            // Register first service
            String serviceId1 = namespace + "-update-1";
            ServiceOptions service1 = new ServiceOptions()
                .setId(serviceId1)
                .setName(serviceName)
                .setTags(List.of("module"))
                .setAddress("localhost")
                .setPort(9191);
            
            System.out.println("Registering first service: " + serviceId1);
            client.registerService(service1)
                .await().atMost(Duration.ofSeconds(5));
            System.out.println("First service registered");
            
            assertTrue(firstUpdateLatch.await(10, TimeUnit.SECONDS), "Should see first update");
            
            // Register second service
            String serviceId2 = namespace + "-update-2";
            ServiceOptions service2 = new ServiceOptions()
                .setId(serviceId2)
                .setName(serviceName)
                .setTags(List.of("module"))
                .setAddress("localhost")
                .setPort(9192);
                
            client.registerService(service2)
                .await().atMost(Duration.ofSeconds(5));
            
            assertTrue(secondUpdateLatch.await(10, TimeUnit.SECONDS), "Should see second update");
            
            // Verify we saw updates
            assertTrue(updateCount.get() >= 2, 
                "Should have at least 2 updates recorded");
            
            // Clean up
            client.deregisterService(serviceId1)
                .await().atMost(Duration.ofSeconds(5));
            client.deregisterService(serviceId2)
                .await().atMost(Duration.ofSeconds(5));
            
        } finally {
            watch.stop();
        }
    }
}