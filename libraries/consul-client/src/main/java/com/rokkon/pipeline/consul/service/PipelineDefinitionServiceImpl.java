package com.rokkon.pipeline.consul.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rokkon.pipeline.config.model.PipelineConfig;
import com.rokkon.pipeline.config.model.PipelineInstance;
import com.rokkon.pipeline.config.service.PipelineDefinitionService;
import com.rokkon.pipeline.config.service.PipelineInstanceService;
import com.rokkon.pipeline.config.model.PipelineDefinitionSummary;
import com.rokkon.pipeline.engine.validation.CompositeValidator;
import com.rokkon.pipeline.api.validation.ValidationResult;
import com.rokkon.pipeline.commons.validation.ValidationResultFactory;
import com.rokkon.pipeline.api.validation.ValidationMode;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PipelineDefinitionService for managing global pipeline definitions in Consul.
 */
@ApplicationScoped
public class PipelineDefinitionServiceImpl implements PipelineDefinitionService {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineDefinitionServiceImpl.class);
    private static final String PIPELINE_METADATA_SUFFIX = "/metadata";
    private static final String PIPELINES_DEFINITIONS_PATH = "/pipelines/definitions/";
    private static final String CREATED_AT_KEY = "createdAt";
    private static final String MODIFIED_AT_KEY = "modifiedAt";
    private static final String CREATED_BY_KEY = "createdBy";
    private static final String MODIFIED_BY_KEY = "modifiedBy";
    private static final String VALIDATION_MODE_KEY = "validationMode";
    private static final String HAS_WARNINGS_KEY = "hasWarnings";
    private static final String SYSTEM_USER = "system";
    private static final String NO_DESCRIPTION = "No description";
    private static final String NO_STEPS_DEFINED = "No steps defined";

    // Cache names
    private static final String CACHE_PIPELINE_DEFINITIONS_LIST = "pipeline-definitions-list";
    private static final String CACHE_PIPELINE_DEFINITIONS = "pipeline-definitions";
    private static final String CACHE_PIPELINE_DEFINITIONS_EXISTS = "pipeline-definitions-exists";
    private static final String CACHE_PIPELINE_METADATA = "pipeline-metadata";
    private static final String CACHE_PIPELINE_ACTIVE_INSTANCES = "pipeline-active-instances";

    @ConfigProperty(name = "pipeline.consul.kv-prefix", defaultValue = "pipeline")
    String kvPrefix;

    @Inject
    ConsulClient consulClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    CompositeValidator<PipelineConfig> pipelineValidator;

    @Inject
    PipelineInstanceService pipelineInstanceService;

    /**
     * Default constructor for CDI.
     */
    public PipelineDefinitionServiceImpl() {
        // Default constructor for CDI
    }

    @Override
    @CacheResult(cacheName = CACHE_PIPELINE_DEFINITIONS_LIST)
    public Uni<List<PipelineDefinitionSummary>> listDefinitions() {
        return consulClient.getKeys(kvPrefix + PIPELINES_DEFINITIONS_PATH)
            .flatMap(keys -> {

                if (keys == null || keys.isEmpty()) {
                    return Uni.createFrom().item(Collections.emptyList());
                }

                // Filter out metadata keys and get unique pipeline IDs
                Set<String> pipelineIds = keys.stream()
                    .filter(key -> !key.endsWith(PIPELINE_METADATA_SUFFIX))
                    .map(key -> key.substring((kvPrefix + PIPELINES_DEFINITIONS_PATH).length()))
                    .filter(id -> !id.contains("/"))  // Skip nested keys
                    .collect(Collectors.toSet());

                // Create list of Unis for parallel fetching
                List<Uni<PipelineDefinitionSummary>> summaryUnis = pipelineIds.stream()
                    .map(pipelineId -> 
                        // Fetch definition, metadata, and instance count in parallel
                        Uni.combine().all().unis(
                            getDefinition(pipelineId),
                            getMetadata(pipelineId),
                            getActiveInstanceCount(pipelineId)
                        ).asTuple()
                        .map(tuple -> {
                            PipelineConfig definition = tuple.getItem1();
                            Map<String, String> metadata = tuple.getItem2();
                            Integer instanceCount = tuple.getItem3();

                            if (definition != null) {
                                return new PipelineDefinitionSummary(
                                    pipelineId,
                                    definition.name(),
                                    getFirstStepDescription(definition),
                                    definition.pipelineSteps().size(),
                                    metadata.getOrDefault(CREATED_AT_KEY, ""),
                                    metadata.getOrDefault(MODIFIED_AT_KEY, ""),
                                    instanceCount
                                );
                            }
                            return null;
                        })
                        .onFailure().recoverWithItem(error -> {
                            LOG.warn("Failed to load pipeline definition summary for '{}'", pipelineId, error);
                            return null;
                        })
                    )
                    .toList();

                // Combine all Unis and filter out nulls
                return Uni.combine().all().unis(summaryUnis)
                    .with(list -> list.stream()
                        .filter(Objects::nonNull)
                        .map(obj -> (PipelineDefinitionSummary) obj)
                        .collect(Collectors.toList())
                    );
            });
    }

    private Uni<Map<String, String>> getMetadata(String pipelineId) {
        String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId + PIPELINE_METADATA_SUFFIX;
        return consulClient.getValue(key)
            .map(keyValue -> {
                if (keyValue != null && keyValue.getValue() != null) {
                    try {
                        String json = keyValue.getValue();
                        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                    } catch (Exception e) {
                        LOG.warn("Failed to parse metadata for pipeline definition '{}'", pipelineId, e);
                        return new HashMap<String, String>();
                    }
                }
                return new HashMap<String, String>();
            })
            .onFailure().recoverWithItem(error -> {
                LOG.warn("Failed to get metadata for pipeline definition '{}'", pipelineId, error);
                return new HashMap<String, String>();
            });
    }

    @Override
    @CacheResult(cacheName = CACHE_PIPELINE_DEFINITIONS)
    public Uni<PipelineConfig> getDefinition(@CacheKey String pipelineId) {
        String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId;
        return consulClient.getValue(key)
            .map(keyValue -> {
                if (keyValue != null && keyValue.getValue() != null) {
                    try {
                        String json = keyValue.getValue();
                        return objectMapper.readValue(json, PipelineConfig.class);
                    } catch (Exception e) {
                        LOG.error("Failed to parse pipeline definition '{}'", pipelineId, e);
                        return null;
                    }
                }
                return null;
            });
    }

    @Override
    public Uni<ValidationResult> createDefinition(String pipelineId, PipelineConfig definition) {
        // Use production validation by default
        return createDefinition(pipelineId, definition, ValidationMode.PRODUCTION);
    }

    @Override
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS_LIST)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS_EXISTS)
    public Uni<ValidationResult> createDefinition(@CacheKey String pipelineId, PipelineConfig definition, ValidationMode validationMode) {
        // Check if already exists
        return definitionExists(pipelineId)
            .flatMap(exists -> {
                if (exists) {
                    return Uni.createFrom().item(ValidationResultFactory.failure("Pipeline definition '" + pipelineId + "' already exists"));
                }

                // Validate the pipeline configuration with the specified mode
                ValidationResult validationResult = pipelineValidator.validate(definition, validationMode);
                // Check validation results based on mode
                if (validationMode == ValidationMode.PRODUCTION && !validationResult.valid()) {
                    // Production mode requires no errors
                    return Uni.createFrom().item(validationResult);
                } else if ((validationMode == ValidationMode.DESIGN || validationMode == ValidationMode.TESTING) && validationResult.hasErrors()) {
                    // Design and Testing modes only fail on errors, allow warnings
                    return Uni.createFrom().item(validationResult);
                }

                try {
                    // Store in Consul
                    String json = objectMapper.writeValueAsString(definition);
                    String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId;

                    return consulClient.putValue(key, json)
                        .flatMap(success -> {
                            if (!success) {
                                return Uni.createFrom().item(ValidationResultFactory.failure("Failed to store pipeline definition in Consul"));
                            }

                            // Store metadata
                            Map<String, String> metadata = new HashMap<>();
                            metadata.put(CREATED_AT_KEY, Instant.now().toString());
                            metadata.put(MODIFIED_AT_KEY, Instant.now().toString());
                            metadata.put(CREATED_BY_KEY, SYSTEM_USER); // TODO: Add user context
                            metadata.put(VALIDATION_MODE_KEY, validationMode.toString());
                            metadata.put(HAS_WARNINGS_KEY, String.valueOf(validationResult.hasWarnings()));

                            try {
                                String metadataKey = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId + PIPELINE_METADATA_SUFFIX;
                                String metadataJson = objectMapper.writeValueAsString(metadata);

                                return consulClient.putValue(metadataKey, metadataJson)
                                    .map(metaSuccess -> {
                                        if (metaSuccess) {
                                            LOG.info("Created pipeline definition '{}' in Consul", pipelineId);
                                            // Preserve warnings from original validation
                                            return validationResult.hasWarnings() ? 
                                                ValidationResultFactory.successWithWarnings(validationResult.warnings()) :
                                                ValidationResultFactory.success();
                                        } else {
                                            return ValidationResultFactory.failure("Failed to store metadata");
                                        }
                                    });
                            } catch (JsonProcessingException e) {
                                LOG.error("Failed to serialize metadata", e);
                                return Uni.createFrom().item(ValidationResultFactory.failure("Failed to serialize metadata: " + e.getMessage()));
                            }
                        });

                } catch (JsonProcessingException e) {
                    LOG.error("Failed to serialize pipeline definition", e);
                    return Uni.createFrom().item(ValidationResultFactory.failure("Failed to serialize pipeline definition: " + e.getMessage()));
                }
            });
    }

    @Override
    public Uni<ValidationResult> updateDefinition(String pipelineId, PipelineConfig definition) {
        // Use production validation by default
        return updateDefinition(pipelineId, definition, ValidationMode.PRODUCTION);
    }

    @Override
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS_LIST)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_METADATA)
    public Uni<ValidationResult> updateDefinition(@CacheKey String pipelineId, PipelineConfig definition, ValidationMode validationMode) {
        // Check if exists
        return definitionExists(pipelineId)
            .flatMap(exists -> {
                if (!exists) {
                    return Uni.createFrom().item(ValidationResultFactory.failure("Pipeline definition '" + pipelineId + "' not found"));
                }

                // Validate the pipeline configuration with the specified mode
                ValidationResult validationResult = pipelineValidator.validate(definition, validationMode);
                // Check validation results based on mode
                if (validationMode == ValidationMode.PRODUCTION && !validationResult.valid()) {
                    // Production mode requires no errors
                    return Uni.createFrom().item(validationResult);
                } else if ((validationMode == ValidationMode.DESIGN || validationMode == ValidationMode.TESTING) && validationResult.hasErrors()) {
                    // Design and Testing modes only fail on errors, allow warnings
                    return Uni.createFrom().item(validationResult);
                }

                try {
                    // Update in Consul
                    String json = objectMapper.writeValueAsString(definition);
                    String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId;

                    return consulClient.putValue(key, json)
                        .flatMap(success -> {
                            if (!success) {
                                return Uni.createFrom().item(ValidationResultFactory.failure("Failed to update pipeline definition in Consul"));
                            }

                            // Update metadata reactively
                            return getMetadata(pipelineId)
                                .onItem().transformToUni(metadata -> {
                                    metadata.put(MODIFIED_AT_KEY, Instant.now().toString());
                                    metadata.put(MODIFIED_BY_KEY, SYSTEM_USER); // TODO: Add user context
                                    metadata.put(VALIDATION_MODE_KEY, validationMode.toString());
                                    metadata.put(HAS_WARNINGS_KEY, String.valueOf(validationResult.hasWarnings()));

                                    try {
                                        String metadataKey = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId + PIPELINE_METADATA_SUFFIX;
                                        String metadataJson = objectMapper.writeValueAsString(metadata);

                                        return consulClient.putValue(metadataKey, metadataJson)
                                            .map(metaSuccess -> {
                                                if (metaSuccess) {
                                                    LOG.info("Updated pipeline definition '{}' in Consul", pipelineId);
                                                    // Preserve warnings from original validation
                                                    return validationResult.hasWarnings() ? 
                                                        ValidationResultFactory.successWithWarnings(validationResult.warnings()) :
                                                        ValidationResultFactory.success();
                                                } else {
                                                    return ValidationResultFactory.failure("Failed to update metadata");
                                                }
                                            });
                                    } catch (JsonProcessingException e) {
                                        LOG.error("Failed to serialize metadata", e);
                                        return Uni.createFrom().item(ValidationResultFactory.failure("Failed to serialize metadata: " + e.getMessage()));
                                    }
                                });
                        });
                } catch (JsonProcessingException e) {
                    LOG.error("Failed to serialize pipeline definition", e);
                    return Uni.createFrom().item(ValidationResultFactory.failure("Failed to serialize pipeline definition: " + e.getMessage()));
                }
            });
    }

    @Override
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS_LIST)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_DEFINITIONS_EXISTS)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_METADATA)
    @CacheInvalidate(cacheName = CACHE_PIPELINE_ACTIVE_INSTANCES)
    public Uni<ValidationResult> deleteDefinition(@CacheKey String pipelineId) {
        // Check if exists
        return definitionExists(pipelineId)
            .flatMap(exists -> {
                if (!exists) {
                    return Uni.createFrom().item(ValidationResultFactory.failure("Pipeline definition '" + pipelineId + "' not found"));
                }

                // Check for active instances
                return getActiveInstanceCount(pipelineId)
                    .flatMap(instanceCount -> {
                        if (instanceCount > 0) {
                            return Uni.createFrom().item(ValidationResultFactory.failure("Cannot delete pipeline definition with " + instanceCount + " active instances"));
                        }

                        // Delete from Consul
                        String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId;
                        String metadataKey = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId + PIPELINE_METADATA_SUFFIX;

                        return consulClient.deleteValue(key)
                            .flatMap(success -> consulClient.deleteValue(metadataKey))
                            .map(metaSuccess -> {
                                LOG.info("Deleted pipeline definition '{}' from Consul", pipelineId);
                                return ValidationResultFactory.success();
                            });
                    });
            });
    }

    @Override
    @CacheResult(cacheName = CACHE_PIPELINE_DEFINITIONS_EXISTS)
    public Uni<Boolean> definitionExists(@CacheKey String pipelineId) {
        String key = kvPrefix + PIPELINES_DEFINITIONS_PATH + pipelineId;
        return consulClient.getValue(key)
            .map(keyValue -> keyValue != null && keyValue.getValue() != null);
    }

    @Override
    @CacheResult(cacheName = CACHE_PIPELINE_ACTIVE_INSTANCES)
    public Uni<Integer> getActiveInstanceCount(@CacheKey String pipelineId) {
        return pipelineInstanceService.listInstancesByDefinition(pipelineId)
            .map(instances -> (int) instances.stream()
                .filter(instance -> instance.status() == PipelineInstance.PipelineInstanceStatus.RUNNING 
                    || instance.status() == PipelineInstance.PipelineInstanceStatus.STARTING)
                .count()
            )
            .onFailure().recoverWithItem(error -> {
                LOG.warn("Failed to get active instance count for pipeline '{}'", pipelineId, error);
                return 0;
            });
    }


    private String getFirstStepDescription(PipelineConfig config) {
        if (config.pipelineSteps() != null && !config.pipelineSteps().isEmpty()) {
            return config.pipelineSteps().values().stream()
                .findFirst()
                .map(step -> step.description() != null ? step.description() : NO_DESCRIPTION)
                .orElse(NO_STEPS_DEFINED);
        }
        return NO_STEPS_DEFINED;
    }
}
