package com.rokkon.pipeline.consul.config;

import com.rokkon.pipeline.consul.test.config.TestEngineConfig;
import com.rokkon.pipeline.consul.test.config.TestConsulConfig;
import com.rokkon.pipeline.consul.test.config.TestModulesConfig;
import com.rokkon.pipeline.consul.test.config.TestDefaultClusterConfig;

/**
 * Test implementation of ConsulConfigSource for integration tests.
 * Provides sensible defaults for all configuration values.
 */
public class TestConsulConfigSource implements ConsulConfigSource {
    private final EngineConfig engineConfig;
    private final ConsulConfig consulConfig;
    private final ModulesConfig modulesConfig;
    private final DefaultClusterConfig defaultClusterConfig;
    
    public TestConsulConfigSource() {
        this.engineConfig = new TestEngineConfig();
        this.consulConfig = new TestConsulConfig();
        this.modulesConfig = new TestModulesConfig();
        this.defaultClusterConfig = new TestDefaultClusterConfig();
    }
    
    @Override
    public EngineConfig engine() {
        return engineConfig;
    }
    
    @Override
    public ConsulConfig consul() {
        return consulConfig;
    }
    
    @Override
    public ModulesConfig modules() {
        return modulesConfig;
    }
    
    @Override
    public DefaultClusterConfig defaultCluster() {
        return defaultClusterConfig;
    }
}