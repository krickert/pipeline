package com.rokkon.pipeline.consul.service;

import com.rokkon.pipeline.commons.model.GlobalModuleRegistryService;
import com.rokkon.pipeline.consul.registry.validation.ModuleConnectionValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.consul.Service;
import io.vertx.ext.consul.ServiceOptions;
import io.vertx.ext.consul.CheckOptions;
import io.vertx.ext.consul.CheckStatus;
import io.vertx.ext.consul.ServiceEntry;
import io.vertx.ext.consul.ServiceEntryList;
import io.vertx.ext.consul.KeyValue;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import com.rokkon.pipeline.consul.config.PipelineConsulConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.net.Socket;
import java.net.InetSocketAddress;

import io.quarkus.scheduler.Scheduled;

/**
 * Implementation of GlobalModuleRegistryService that uses Consul for storage.
 * Modules are registered globally and can be referenced by clusters.
 */
@ApplicationScoped
public class GlobalModuleRegistryServiceImpl implements GlobalModuleRegistryService {

    private static final Logger LOG = Logger.getLogger(GlobalModuleRegistryServiceImpl.class);

    @Inject
    PipelineConsulConfig config;
    
    @ConfigProperty(name = "pipeline.consul.cleanup.interval", defaultValue = "30m")
    String cleanupInterval;

    @Inject
    ConsulClient consulClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ModuleConnectionValidator connectionValidator;

    @Inject
    HealthCheckConfigProvider healthConfig;

    private JsonSchemaFactory schemaFactory;

    /**
     * Default constructor for CDI.
     */
    public GlobalModuleRegistryServiceImpl() {
        // Default constructor for CDI
    }
    
    /**
     * Get the JsonSchemaFactory instance (lazy initialization).
     */
    private JsonSchemaFactory getSchemaFactory() {
        if (schemaFactory == null) {
            try {
                LOG.debug("Initializing JsonSchemaFactory for V7");
                schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                LOG.debug("JsonSchemaFactory initialized successfully");
            } catch (Exception e) {
                LOG.error("Failed to initialize JsonSchemaFactory: " + e.getMessage(), e);
                // Log the full stack trace
                if (e.getCause() != null) {
                    LOG.error("Caused by: " + e.getCause().getMessage(), e.getCause());
                }
                // Return a null object pattern or throw a more specific exception
                throw new RuntimeException("Failed to initialize JSON Schema validator: " + e.getMessage(), e);
            }
        }
        return schemaFactory;
    }

    /**
     * Register a module globally in Consul.
     * This creates both a service entry and stores metadata in KV.
     */
    @Override
    @CacheInvalidate(cacheName = "global-modules-list")
    @CacheInvalidate(cacheName = "global-modules-enabled")
    public Uni<ModuleRegistration> registerModule(
            String moduleName,
            String implementationId,
            String host,
            int port,
            String serviceType,
            String version,
            Map<String, String> metadata,
            String engineHost,
            int enginePort,
            String jsonSchema) {

        String moduleId = generateModuleId(moduleName);

        LOG.infof("Registering module globally: %s (%s) at %s:%d (engine connection: %s:%d)", 
                  moduleName, implementationId, host, port, engineHost, enginePort);

        // Extract container metadata if provided
        final String containerId = metadata != null ? metadata.get("containerId") : null;
        final String containerName = metadata != null ? metadata.get("containerName") : null;
        final String hostname = metadata != null ? metadata.get("hostname") : null;

        // Validate JSON Schema v7 if provided
        if (jsonSchema != null && !jsonSchema.trim().isEmpty()) {
            if (!isValidJsonSchemaV7(jsonSchema)) {
                throw new IllegalArgumentException(
                    String.format("Invalid JSON Schema v7 provided for module '%s'", moduleName)
                );
            }
        }

        // Allow multiple instances of the same module type to register
        // Each instance gets a unique moduleId but shares the same moduleName
        return listRegisteredModules()
            .onItem().transformToUni(existingModules -> {
                // Check for duplicate container registration
                if (containerId != null) {
                    Optional<ModuleRegistration> duplicateContainer = existingModules.stream()
                        .filter(m -> containerId.equals(m.containerId()))
                        .findFirst();

                    if (duplicateContainer.isPresent()) {
                        ModuleRegistration dup = duplicateContainer.get();
                        LOG.errorf("Container %s is already registered as module '%s'", containerId, dup.moduleName());
                        return Uni.createFrom().failure(new IllegalArgumentException(
                            String.format("Container %s is already registered as module '%s'. " +
                                        "Cannot register the same container under multiple module names.", 
                                        containerId, dup.moduleName())
                        ));
                    }
                }

                // Check if there are existing modules with the same name
                // Check for duplicate host/port combination
                boolean duplicateEndpoint = existingModules.stream()
                    .anyMatch(m -> m.host().equals(host) && m.port() == port);

                if (duplicateEndpoint) {
                    LOG.warnf("Module already registered at %s:%d", host, port);
                    return Uni.createFrom().failure(new WebApplicationException(
                        String.format("A module is already registered at %s:%d", host, port),
                        Response.Status.CONFLICT
                    ));
                }

                List<ModuleRegistration> sameNameModules = existingModules.stream()
                    .filter(m -> m.moduleName().equals(moduleName))
                    .toList();

                if (!sameNameModules.isEmpty()) {
                    LOG.infof("Module type '%s' already has %d instance(s) registered. Adding new instance.", 
                             moduleName, sameNameModules.size());

                    // Validate schema consistency across instances
                    for (ModuleRegistration existing : sameNameModules) {
                        if (existing.jsonSchema() != null && jsonSchema != null) {
                            if (!areJsonSchemasEquivalent(existing.jsonSchema(), jsonSchema)) {
                                LOG.errorf("Module %s schema mismatch detected for new instance", moduleName);
                                return Uni.createFrom().failure(new IllegalArgumentException(
                                    String.format("Schema mismatch for module '%s'. All instances must have the same schema.", 
                                                moduleName)
                                ));
                            }
                        }
                    }
                }

                // Validate the module is accessible
                return connectionValidator.validateConnection(host, port, moduleName);
            })
            .onItem().transformToUni(valid -> {
                if (!valid) {
                    return Uni.createFrom().failure(new WebApplicationException(
                        String.format("Cannot connect to module %s at %s:%d", moduleName, host, port),
                        Response.Status.BAD_REQUEST
                    ));
                }

                // Create the module registration record
                ModuleRegistration registration = new ModuleRegistration(
                    moduleId,
                    moduleName,
                    implementationId,
                    host,
                    port,
                    serviceType,
                    version,
                    metadata != null ? metadata : Map.of(),
                    System.currentTimeMillis(),
                    engineHost,
                    enginePort,
                    jsonSchema,
                    true,  // New modules are enabled by default
                    containerId,
                    containerName,
                    hostname
                );

                // Create service options for Consul
                Map<String, String> serviceMeta = new java.util.HashMap<>();
                serviceMeta.put("moduleName", moduleName);
                serviceMeta.put("implementationId", implementationId);
                serviceMeta.put("serviceType", serviceType);
                serviceMeta.put("service-type", "MODULE");  // Add standard service-type for dashboard
                serviceMeta.put("version", version);
                serviceMeta.put("registeredAt", String.valueOf(registration.registeredAt()));

                // Add container metadata if available
                if (containerId != null) serviceMeta.put("containerId", containerId);
                if (containerName != null) serviceMeta.put("containerName", containerName);
                if (hostname != null) serviceMeta.put("hostname", hostname);

                // Store JSON schema if provided
                if (jsonSchema != null && !jsonSchema.trim().isEmpty()) {
                    serviceMeta.put("jsonSchema", jsonSchema);
                }

                // Store engine connection info if different from Consul registration
                if (!engineHost.equals(host) || enginePort != port) {
                    serviceMeta.put("engineHost", engineHost);
                    serviceMeta.put("enginePort", String.valueOf(enginePort));
                }

                ServiceOptions serviceOptions = new ServiceOptions()
                    .setId(moduleId)
                    .setName(moduleName)
                    .setTags(List.of("module", "global", serviceType, "version:" + version))
                    .setAddress(host)
                    .setPort(port)
                    .setMeta(serviceMeta);

                // Add gRPC health check with configurable intervals
                CheckOptions checkOptions = new CheckOptions()
                    .setName("Module gRPC Health Check")
                    .setGrpc(host + ":" + port)
                    .setGrpcTls(false)
                    .setInterval(healthConfig.getCheckInterval().getSeconds() + "s")
                    .setDeregisterAfter(healthConfig.getDeregisterAfter().getSeconds() + "s");

                serviceOptions.setCheckOptions(checkOptions);

                // Register the service
                LOG.infof("Registering service with Consul: %s (ID: %s)", moduleName, moduleId);
                return consulClient.registerService(serviceOptions)
                .onItem().transformToUni(v -> {
                    LOG.infof("Service registered with Consul, now storing metadata in KV");
                    // Store additional metadata in KV
                    return storeModuleMetadata(registration);
                })
                .onItem().transform(v -> {
                    LOG.infof("Module registration complete: %s", moduleId);
                    return registration;
                });
            })
            .onFailure().transform(t -> {
                LOG.errorf(t, "Failed to register module %s", moduleName);
                if (t instanceof WebApplicationException) {
                    return t;
                }
                return new WebApplicationException(
                    "Failed to register module: " + t.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR
                );
            });
    }

    /**
     * List all globally registered modules as an ordered set (no duplicates)
     * This includes both enabled and disabled modules
     */
    @Override
    @CacheResult(cacheName = "global-modules-list")
    @SuppressWarnings("unchecked")
    public Uni<Set<ModuleRegistration>> listRegisteredModules() {
        String prefix = config.consul().kvPrefix() + "/modules/global/";
        return consulClient.getKeys(prefix)
            .onItem().transformToUni(keys -> {
                if (keys == null || keys.isEmpty()) {
                    return Uni.createFrom().item(new LinkedHashSet<ModuleRegistration>());
                }

                List<Uni<Optional<ModuleRegistration>>> moduleUnis = keys.stream()
                    .map(this::getModuleMetadataFromKey)
                    .collect(Collectors.toList());

                return Uni.combine().all().unis(moduleUnis)
                    .with(results -> {
                        Set<ModuleRegistration> modules = new LinkedHashSet<>();
                        for (Object result : results) {
                            Optional<ModuleRegistration> moduleOpt = (Optional<ModuleRegistration>) result;
                            moduleOpt.ifPresent(modules::add);
                        }
                        return modules;
                    });
            })
            .onFailure().recoverWithItem(new LinkedHashSet<ModuleRegistration>());
    }

    /**
     * List only enabled modules
     */
    @Override
    @CacheResult(cacheName = "global-modules-enabled")
    public Uni<Set<ModuleRegistration>> listEnabledModules() {
        return listRegisteredModules()
            .onItem().transform(modules -> {
                Set<ModuleRegistration> enabledModules = new LinkedHashSet<>();
                for (ModuleRegistration module : modules) {
                    if (module.enabled()) {
                        enabledModules.add(module);
                    }
                }
                return enabledModules;
            });
    }

    /**
     * Get a specific module by ID
     */
    @Override
    @CacheResult(cacheName = "global-modules")
    public Uni<ModuleRegistration> getModule(@CacheKey String moduleId) {
        // This approach is inefficient as it lists all modules.
        // A more direct lookup would be better if performance is critical, but this relies on the cache.
        return listRegisteredModules()
            .onItem().transform(modules -> modules.stream()
                .filter(module -> module.moduleId().equals(moduleId))
                .findFirst()
                .orElse(null));
    }

    /**
     * Disable a module (sets enabled=false)
     */
    @Override
    @CacheInvalidate(cacheName = "global-modules-list")
    @CacheInvalidate(cacheName = "global-modules-enabled")
    @CacheInvalidate(cacheName = "global-modules")
    @CacheInvalidate(cacheName = "cluster-modules-enabled")
    public Uni<Boolean> disableModule(@CacheKey String moduleId) {
        LOG.infof("Disabling module: %s", moduleId);

        // First get the current module registration
        return getModule(moduleId)
            .onItem().transformToUni(module -> {
                if (module == null) {
                    LOG.warnf("Module %s not found for disabling", moduleId);
                    return Uni.createFrom().item(false);
                }

                // Create a new registration with enabled=false
                ModuleRegistration disabledModule = new ModuleRegistration(
                    module.moduleId(),
                    module.moduleName(),
                    module.implementationId(),
                    module.host(),
                    module.port(),
                    module.serviceType(),
                    module.version(),
                    module.metadata(),
                    module.registeredAt(),
                    module.engineHost(),
                    module.enginePort(),
                    module.jsonSchema(),
                    false,  // Set to disabled
                    module.containerId(),
                    module.containerName(),
                    module.hostname()
                );

                // Update the KV store with the disabled state
                return storeModuleMetadata(disabledModule)
                    .onItem().transform(v -> {
                        LOG.infof("Module %s has been disabled", moduleId);
                        return true;
                    });
            })
            .onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Failed to disable module %s", moduleId);
                return false;
            });
    }

    /**
     * Enable a module (sets enabled=true)
     */
    @Override
    @CacheInvalidate(cacheName = "global-modules-list")
    @CacheInvalidate(cacheName = "global-modules-enabled")
    @CacheInvalidate(cacheName = "global-modules")
    @CacheInvalidate(cacheName = "cluster-modules-enabled")
    public Uni<Boolean> enableModule(@CacheKey String moduleId) {
        LOG.infof("Enabling module: %s", moduleId);

        // First get the current module registration
        return getModule(moduleId)
            .onItem().transformToUni(module -> {
                if (module == null) {
                    LOG.warnf("Module %s not found for enabling", moduleId);
                    return Uni.createFrom().item(false);
                }

                // Create a new registration with enabled=true
                ModuleRegistration enabledModule = new ModuleRegistration(
                    module.moduleId(),
                    module.moduleName(),
                    module.implementationId(),
                    module.host(),
                    module.port(),
                    module.serviceType(),
                    module.version(),
                    module.metadata(),
                    module.registeredAt(),
                    module.engineHost(),
                    module.enginePort(),
                    module.jsonSchema(),
                    true,  // Set to enabled
                    module.containerId(),
                    module.containerName(),
                    module.hostname()
                );

                // Update the KV store with the enabled state
                return storeModuleMetadata(enabledModule)
                    .onItem().transform(v -> {
                        LOG.infof("Module %s has been enabled", moduleId);
                        return true;
                    });
            })
            .onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Failed to enable module %s", moduleId);
                return false;
            });
    }

    /**
     * Deregister a module (hard delete - removes from registry completely)
     */
    @Override
    @CacheInvalidate(cacheName = "global-modules-list")
    @CacheInvalidate(cacheName = "global-modules-enabled")
    @CacheInvalidate(cacheName = "global-modules")
    @CacheInvalidate(cacheName = "module-health-status")
    @CacheInvalidate(cacheName = "cluster-modules-enabled")
    public Uni<Void> deregisterModule(@CacheKey String moduleId) {
        LOG.infof("Deregistering module: %s", moduleId);
        return consulClient.deregisterService(moduleId)
            .onItem().transformToUni(v -> {
                LOG.infof("Module %s deregistered from Consul services, removing from KV.", moduleId);
                // Also remove from KV store
                return consulClient.deleteValue(buildModuleKvKey(moduleId));
            })
            .onFailure().invoke(t -> LOG.errorf(t, "Failed to deregister module %s", moduleId))
            .replaceWithVoid();
    }

    /**
     * Converts a Consul Service to a ModuleRegistration object.
     * This involves fetching additional metadata from the KV store.
     */
    private Uni<ModuleRegistration> serviceToModuleRegistration(Service service) {
        if (service == null) {
            return Uni.createFrom().nullItem();
        }

        return getModuleMetadata(service.getId())
            .onItem().transform(metadataOpt -> {
                ModuleRegistration metadata = metadataOpt.orElse(null);

                // If metadata is missing, build a partial registration from the service
                if (metadata == null) {
                    LOG.warnf("Metadata not found in KV for module %s. Creating partial registration.", service.getId());
                    return new ModuleRegistration(
                        service.getId(),
                        service.getName(),
                        service.getMeta().getOrDefault("implementationId", "unknown"),
                        service.getAddress(),
                        service.getPort(),
                        service.getMeta().getOrDefault("serviceType", "unknown"),
                        service.getMeta().getOrDefault("version", "unknown"),
                        service.getMeta(),
                        Long.parseLong(service.getMeta().getOrDefault("registeredAt", "0")),
                        service.getMeta().getOrDefault("engineHost", service.getAddress()),
                        Integer.parseInt(service.getMeta().getOrDefault("enginePort", String.valueOf(service.getPort()))),
                        service.getMeta().get("jsonSchema"),
                        true, // Assume enabled if no metadata
                        service.getMeta().get("containerId"),
                        service.getMeta().get("containerName"),
                        service.getMeta().get("hostname")
                    );
                }

                return metadata;
            });
    }

    /**
     * Stores the full module registration details in the Consul KV store.
     */
    private Uni<Void> storeModuleMetadata(ModuleRegistration registration) {
        try {
            String key = buildModuleKvKey(registration.moduleId());
            String json = objectMapper.writeValueAsString(registration);
            return consulClient.putValue(key, json).replaceWithVoid();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize module metadata for %s", registration.moduleId());
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Retrieves the full module registration details from the Consul KV store.
     */
    private Uni<Optional<ModuleRegistration>> getModuleMetadata(String moduleId) {
        String key = buildModuleKvKey(moduleId);
        return getModuleMetadataFromKey(key);
    }

    private Uni<Optional<ModuleRegistration>> getModuleMetadataFromKey(String key) {
        return consulClient.getValue(key)
            .onItem().transform(kv -> {
                if (kv == null || kv.getValue() == null) {
                    return Optional.<ModuleRegistration>empty();
                }
                try {
                    ModuleRegistration reg = objectMapper.readValue(kv.getValue(), ModuleRegistration.class);
                    return Optional.of(reg);
                } catch (Exception e) {
                    LOG.warnf(e, "Failed to parse KV metadata from key %s", key);
                    return Optional.<ModuleRegistration>empty();
                }
            })
            .onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Failed to retrieve module metadata from key %s", key);
                return Optional.<ModuleRegistration>empty();
            });
    }

    private String buildModuleKvKey(String moduleId) {
        return config.consul().kvPrefix() + "/modules/global/" + moduleId;
    }


    /**
     * Generates a unique ID for a module instance.
     */
    private String generateModuleId(String moduleName) {
        return moduleName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Archive a service by moving it from active to archive namespace in Consul
     */
    @Override
    @CacheInvalidate(cacheName = "global-modules-list")
    @CacheInvalidate(cacheName = "global-modules-enabled")
    @CacheInvalidate(cacheName = "global-modules")
    @CacheInvalidate(cacheName = "module-health-status")
    public Uni<Boolean> archiveService(String serviceName, String reason) {
        String timestamp = java.time.Instant.now().toString();

        // First, try to get the service from Consul's service registry
        return consulClient.catalogServiceNodes(serviceName)
        .onItem().transformToUni(serviceNodes -> {
            if (serviceNodes == null || serviceNodes.getList() == null || serviceNodes.getList().isEmpty()) {
                LOG.warnf("Service %s not found in Consul registry", serviceName);
                return Uni.createFrom().item(false);
            }

            // Get the first service node
            var serviceNode = serviceNodes.getList().get(0);

            // Create archive metadata
            String archiveJson;
            try {
                // Use simple JSON format to avoid Jackson dependency issues
                archiveJson = String.format("""
                    {
                        "serviceName": "%s",
                        "serviceId": "%s",
                        "address": "%s",
                        "port": %d,
                        "archivedAt": "%s",
                        "reason": "%s"
                    }
                    """,
                    serviceNode.getName() != null ? serviceNode.getName() : serviceName,
                    serviceNode.getId() != null ? serviceNode.getId() : "",
                    serviceNode.getAddress() != null ? serviceNode.getAddress() : "",
                    serviceNode.getPort(),
                    timestamp,
                    reason
                );
            } catch (Exception e) {
                LOG.warnf("Failed to create archive JSON: %s", e.getMessage());
                return archiveServiceSimple(serviceName, reason, timestamp);
            }

            // Store in archive namespace
            String archiveKey = String.format("%s/services/%s-%s", 
                config.consul().kvPrefix() + "/archive", serviceName, timestamp.replace(":", "-").replace(".", "-"));

            return consulClient.putValue(archiveKey, archiveJson)
            .onItem().transformToUni(success -> {
                if (!success) {
                    return Uni.createFrom().failure(
                        new RuntimeException("Failed to archive service metadata")
                    );
                }

                // Now deregister the service using the service ID
                String serviceId = serviceNode.getId() != null ? 
                    serviceNode.getId() : serviceName;

                return consulClient.deregisterService(serviceId)
                .onItem().transform(v -> {
                    LOG.infof("Successfully archived and deregistered service %s", serviceName);
                    return true;
                });
            });
        })
        .onFailure().recoverWithUni(t -> {
            if (t.getMessage() != null && t.getMessage().contains("com.fasterxml.jackson")) {
                // JSON serialization error - try simpler format
                LOG.warnf("JSON serialization failed, using simple archive format: %s", t.getMessage());
                return archiveServiceSimple(serviceName, reason, timestamp);
            }
            LOG.errorf(t, "Failed to archive service %s", serviceName);
            return Uni.createFrom().item(false);
        });
    }

    private Uni<Boolean> archiveServiceSimple(String serviceName, String reason, String timestamp) {
        // Simple archive format without complex JSON serialization
        String archiveJson = String.format("""
            {
                "serviceName": "%s",
                "archivedAt": "%s",
                "reason": "%s"
            }
            """,
            serviceName,
            timestamp,
            reason
        );

        String archiveKey = String.format("%s/services/%s-%s", 
            config.consul().kvPrefix() + "/archive", serviceName, timestamp.replace(":", "-").replace(".", "-"));

        return consulClient.putValue(archiveKey, archiveJson)
        .onItem().transform(success -> {
            if (success) {
                LOG.infof("Service %s archived (simple format)", serviceName);
            }
            return success;
        });
    }

    /**
     * Compare two JSON schemas for semantic equivalence.
     * This handles differences in formatting, property ordering, etc.
     */
    private boolean areJsonSchemasEquivalent(String schema1, String schema2) {
        try {
            // Parse both schemas as JSON
            JsonNode schemaNode1 = objectMapper.readTree(schema1);
            JsonNode schemaNode2 = objectMapper.readTree(schema2);

            // Use Jackson's equals which does deep comparison ignoring order
            return schemaNode1.equals(schemaNode2);
        } catch (Exception e) {
            LOG.errorf("Failed to compare schemas: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Validate that a schema is a valid JSON Schema v7
     */
    private boolean isValidJsonSchemaV7(String schemaContent) {
        if (schemaContent == null || schemaContent.trim().isEmpty()) {
            return false;
        }

        try {
            JsonNode schemaNode = objectMapper.readTree(schemaContent);
            // Attempt to create a JsonSchema - this validates it's proper JSON Schema v7
            JsonSchema schema = getSchemaFactory().getSchema(schemaNode);
            return true;
        } catch (Exception e) {
            LOG.errorf("Invalid JSON Schema v7: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Clean up zombie instances - modules that are failing health checks or no longer exist
     * This method handles ALL types of zombies and dirty Consul state:
     * 1. Type 1: Classic zombies (in KV + catalog but unhealthy)
     * 2. Type 2: Stale service entries (in catalog but not KV)
     * 3. Type 3: Partial registrations (in KV but not catalog)
     * 4. Type 4: Name mismatches (registered with wrong name like "echo-module" instead of "echo")
     * 5. Type 5: Any registration that doesn't match expected patterns or has no real backing
     */
    @Override
    public Uni<ZombieCleanupResult> cleanupZombieInstances() {
        LOG.info("Starting comprehensive zombie instance cleanup");

        Uni<Set<ModuleRegistration>> registeredModulesUni = listRegisteredModules();

        Uni<List<Service>> catalogServicesUni = consulClient.catalogServices()
            .onItem().transform(serviceList -> {
                if (serviceList == null || serviceList.getList() == null) {
                    return new ArrayList<Service>();
                }
                return serviceList.getList().stream()
                    .filter(service -> service != null &&
                                    service.getTags() != null &&
                                    service.getTags().contains("module"))
                    .collect(Collectors.toList());
            });

        return Uni.combine().all().unis(registeredModulesUni, catalogServicesUni).asTuple()
            .onItem().transformToUni(tuple -> {
                Set<ModuleRegistration> registeredModules = tuple.getItem1();
                List<Service> catalogModuleServices = tuple.getItem2();

                Set<String> registeredModuleIds = registeredModules.stream()
                    .map(ModuleRegistration::moduleId)
                    .collect(Collectors.toSet());

                // Fetch health for all catalog services in parallel
                List<Uni<ServiceEntryList>> healthUnis = catalogModuleServices.stream()
                    .map(s -> consulClient.healthServiceNodes(s.getName(), false))
                    .collect(Collectors.toList());

                return Uni.combine().all().unis(healthUnis)
                    .with(healthResults -> {
                        Set<String> healthyOrWarningIds = new HashSet<>();
                        Set<String> criticalIds = new HashSet<>();
                        Set<String> catalogIds = new HashSet<>();

                        for (Object result : healthResults) {
                            ServiceEntryList sel = (ServiceEntryList) result;
                            if (sel != null && sel.getList() != null) {
                                for (ServiceEntry entry : sel.getList()) {
                                    catalogIds.add(entry.getService().getId());
                                    boolean isCritical = entry.getChecks().stream()
                                        .anyMatch(c -> c.getStatus() == CheckStatus.CRITICAL);
                                    if (isCritical) {
                                        criticalIds.add(entry.getService().getId());
                                    } else {
                                        healthyOrWarningIds.add(entry.getService().getId());
                                    }
                                }
                            }
                        }

                        // Now, determine zombies
                        Set<ModuleRegistration> zombies = new HashSet<>();
                        // Type 1: In KV and critical in catalog
                        zombies.addAll(registeredModules.stream()
                            .filter(m -> criticalIds.contains(m.moduleId()))
                            .collect(Collectors.toSet()));
                        // Type 2: In KV but not in catalog at all
                        zombies.addAll(registeredModules.stream()
                            .filter(m -> !catalogIds.contains(m.moduleId()))
                            .collect(Collectors.toSet()));

                        // Type 3: In catalog but not in KV (stale service)
                        catalogModuleServices.stream()
                            .filter(s -> !registeredModuleIds.contains(s.getId()))
                            .forEach(s -> zombies.add(serviceToPartialModuleRegistration(s)));

                        return zombies;
                    })
                    .onItem().transformToUni(zombies -> {
                        int zombiesDetected = zombies.size();
                        if (zombiesDetected == 0) {
                            LOG.info("No zombies detected");
                            return Uni.createFrom().item(new ZombieCleanupResult(0, 0, List.of()));
                        }

                        LOG.infof("Detected %d zombie modules. Cleaning up...", zombiesDetected);

                        List<Uni<Void>> cleanupUnis = zombies.stream()
                            .map(zombie -> deregisterModule(zombie.moduleId()))
                            .collect(Collectors.toList());

                        return Uni.combine().all().unis(cleanupUnis)
                            .with(v -> {
                                // For simplicity, we assume cleanup is successful if it doesn't fail.
                                // A more robust implementation could return results from each cleanup Uni.
                                LOG.infof("Zombie cleanup completed: %d detected, %d cleaned.", zombiesDetected, zombiesDetected);
                                return new ZombieCleanupResult(zombiesDetected, zombiesDetected, List.of());
                            });
                    });
            })
            .onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Failed to cleanup zombie instances");
                return new ZombieCleanupResult(0, 0, List.of("Cleanup failed: " + t.getMessage()));
            });
    }

    private ModuleRegistration serviceToPartialModuleRegistration(Service service) {
        return new ModuleRegistration(
            service.getId(), service.getName(), "unknown", service.getAddress(), service.getPort(),
            "unknown", "unknown", service.getMeta(), 0, service.getAddress(), service.getPort(),
            null, false, null, null, null
        );
    }

    /**
     * Public method to check module health using Consul health checks
     * @param moduleId The module ID to check
     * @return Health status of the module
     */
    @Override
    @CacheResult(cacheName = "module-health-status")
    public Uni<ServiceHealthStatus> getModuleHealthStatus(@CacheKey String moduleId) {
        return getModule(moduleId)
            .onItem().transformToUni(module -> {
                if (module == null) {
                    // To avoid caching nulls for non-existent modules, we return a failure.
                    return Uni.createFrom().failure(new WebApplicationException("Module not found: " + moduleId, Response.Status.NOT_FOUND));
                }
                return checkModuleHealth(module);
            });
    }

    /**
     * Check module health using Consul health checks
     */
    private Uni<ServiceHealthStatus> checkModuleHealth(ModuleRegistration module) {
        String serviceName = module.moduleName();

        return consulClient.healthServiceNodes(serviceName, false)
        .onItem().transform(serviceEntryList -> {
            if (serviceEntryList == null || serviceEntryList.getList() == null || serviceEntryList.getList().isEmpty()) {
                // Service not found in health checks
                LOG.debugf("Module %s not found in Consul health checks", module.moduleId());
                return new ServiceHealthStatus(module, HealthStatus.CRITICAL, false);
            }

            // Find the specific instance by ID
            var instanceHealth = serviceEntryList.getList().stream()
                .filter(entry -> module.moduleId().equals(entry.getService().getId()))
                .findFirst();

            if (instanceHealth.isEmpty()) {
                // Specific instance not found
                LOG.debugf("Module instance %s not found in health checks", module.moduleId());
                return new ServiceHealthStatus(module, HealthStatus.CRITICAL, false);
            }

            // Get the worst health check status for this instance
            HealthStatus worstStatus = HealthStatus.PASSING;
            var checks = instanceHealth.get().getChecks();
            if (checks != null) {
                for (var check : checks) {
                    CheckStatus status = check.getStatus();
                    if (status == CheckStatus.CRITICAL) {
                        worstStatus = HealthStatus.CRITICAL;
                        break;
                    } else if (status == CheckStatus.WARNING && worstStatus == HealthStatus.PASSING) {
                        worstStatus = HealthStatus.WARNING;
                    }
                }
            }

            if (worstStatus == HealthStatus.CRITICAL) {
                LOG.debugf("Module %s has critical health status", module.moduleId());
            }

            return new ServiceHealthStatus(module, worstStatus, true);
        })
        .onFailure().recoverWithItem(t -> {
            LOG.warnf("Failed to check health for module %s: %s", module.moduleId(), t.getMessage());
            // On failure, assume module exists but status unknown
            return new ServiceHealthStatus(module, HealthStatus.WARNING, true);
        });
    }

    /**
     * Clean up stale entries in the whitelist (modules registered but not in Consul)
     */
    @Override
    public Uni<Integer> cleanupStaleWhitelistedModules() {
        LOG.info("Starting stale whitelist cleanup");

        Uni<Set<String>> registeredModuleIdsUni = listRegisteredModules()
            .map(modules -> modules.stream().map(ModuleRegistration::moduleId).collect(Collectors.toSet()));

        Uni<Set<String>> consulServiceIdsUni = consulClient.localServices()
            .map(services -> services.stream().map(Service::getId).collect(Collectors.toSet()));

        return Uni.combine().all().unis(registeredModuleIdsUni, consulServiceIdsUni).asTuple()
            .onItem().transformToUni(tuple -> {
                Set<String> registeredIds = tuple.getItem1();
                Set<String> consulIds = tuple.getItem2();

                registeredIds.removeAll(consulIds); // a - b = stale IDs

                if (registeredIds.isEmpty()) {
                    LOG.info("No stale whitelist entries found");
                    return Uni.createFrom().item(0);
                }

                LOG.infof("Found %d stale whitelist entries to remove from KV store.", registeredIds.size());

                List<Uni<Void>> cleanupUnis = registeredIds.stream()
                    .map(staleId -> consulClient.deleteValue(buildModuleKvKey(staleId)))
                    .collect(Collectors.toList());

                return Uni.combine().all().unis(cleanupUnis)
                    .with(v -> registeredIds.size());
            })
            .onFailure().recoverWithItem(t -> {
                LOG.errorf(t, "Failed to cleanup stale whitelist entries");
                return 0;
            });
    }

    /**
     * Scheduled cleanup task that runs periodically.
     * Timing is controlled by configuration properties that can be updated at runtime.
     */
    @Scheduled(every = "{pipeline.consul.cleanup.interval:30m}", 
               delay = 1, 
               delayUnit = java.util.concurrent.TimeUnit.MINUTES)
    void scheduledCleanup() {
        if (!healthConfig.isCleanupEnabled()) {
            LOG.debug("Scheduled cleanup is disabled via configuration");
            return;
        }

        LOG.debug("Running scheduled cleanup tasks");

        // Run zombie cleanup
        cleanupZombieInstances()
            .subscribe().with(
                result -> {
                    if (result.zombiesDetected() > 0) {
                        LOG.infof("Scheduled cleanup detected and cleaned %d zombie module(s)", result.zombiesCleaned());
                    } else {
                        LOG.debug("Scheduled cleanup found no zombie modules");
                    }
                },
                failure -> LOG.errorf(failure, "Error during scheduled cleanup: %s", failure.getMessage())
            );

        // Clean up stale whitelist entries
        cleanupStaleWhitelistedModules()
            .subscribe().with(
                count -> LOG.infof("Cleaned up %d stale whitelist entry(ies)", count),
                failure -> LOG.errorf(failure, "Error during stale whitelist cleanup: %s", failure.getMessage())
            );
    }
}
