# Pipeline Project Migration Plan

## Overview

This document outlines the migration plan from the current multi-module structure to a cleaner, more maintainable Quarkus-friendly architecture.

## Migration Status Tracking

| Current Location | Module Name | New Location | Type | Status | Priority | Notes |
|-----------------|-------------|--------------|------|--------|----------|-------|
| **BOMs** |
| `/bom/base` | Base BOM | `/bom/pipeline-bom` | BOM | 🟢 Completed | P0 | Consolidated into single BOM |
| `/bom/library` | Library BOM | `/bom/pipeline-bom` | BOM | 🟢 Completed | P0 | Merged into main BOM |
| `/bom/server` | Server BOM | `/bom/pipeline-bom` | BOM | 🟢 Completed | P0 | Merged into main BOM |
| `/bom/cli` | CLI BOM | `/bom/pipeline-bom` | BOM | 🟢 Completed | P0 | Merged into main BOM |
| `/bom/module` | Module BOM | `/bom/pipeline-bom` | BOM | 🟢 Completed | P0 | Merged into main BOM |
| **Protocol Buffers (Resource JAR)** |
| `/commons/protobuf` | Protocol Buffers | `/grpc-stubs` | Library | 🟢 Completed | P0 | Proto files are in grpc-stubs/src/main/proto |
| **Libraries (All Quarkus-based)** |
| `/commons/interface` | Common Interfaces | `/libraries/pipeline-api` | Library | 🟢 Completed | P1 | Uses Jackson, validators |
| `/commons/util` | Common Utilities | `/libraries/pipeline-commons` | Library | 🟢 Completed | P1 | Uses Arc, Docker client |
| `/commons/data-util` | Data Utilities | `/libraries/data-util` | Library | 🟢 Completed | P2 | Check dependencies first |
| `/engine/consul` | Consul Integration | `/libraries/consul-client` | Library | 🟡 In Progress | P1 | Config mapping done, 1 test failing |
| `/engine/validators` | Validators | `/libraries/validators` | Library | 🟢 Completed | P2 | Validation services |
| `/testing/util` | Testing Utilities | `/libraries/testing-commons` | Library | 🟢 Completed | P0 | Test utilities |
| `/testing/server-util` | Server Test Utils | `/libraries/testing-server-util` | Library | 🟢 Completed | P2 | Docker client injection fixed |
| **Extensions (Runtime/Deployment Split)** |
| `/extensions/grpc-stubs/*` | gRPC Stubs Extension | `/grpc-stubs` | Library | 🟢 Completed | P1 | Converted to library with proto files |
| `/extensions/dynamic-grpc/*` | Dynamic gRPC Extension | `/extensions/dynamic-grpc/*` | Extension | 🔴 Not Started | P1 | Fix implementation |
| `/extensions/dev-services/consul/*` | Consul Dev Services | `/extensions/dev-services/consul/*` | Extension | 🔴 Not Started | P1 | Move as-is |
| `/extensions/pipeline-dev-ui/*` | Pipeline Dev UI | `/extensions/pipeline-dev-ui/*` | Extension | 🔴 Not Started | P3 | Move as-is |
| **Applications** |
| `/engine/pipestream` | Pipeline Engine | `/applications/pipeline-engine` | Quarkus App | 🔴 Not Started | P1 | Main application |
| `/cli/register-module` | Module Registration CLI | `/applications/cli/register-module` | Quarkus App | 🔴 Not Started | P2 | CLI application |
| `/cli/seed-engine-consul-config` | Consul Config CLI | `/applications/cli/seed-consul-config` | Quarkus App | 🔴 Not Started | P2 | CLI application |
| **Modules (Pipeline Components)** |
| `/modules/echo` | Echo Module | `/modules/echo` | Module | 🔴 Not Started | P2 | Keep structure |
| `/modules/testing-harness` | Testing Module | `/modules/testing-harness` | Module | 🔴 Not Started | P2 | Keep structure |
| `/modules/chunker` | Chunker Module | `/modules/chunker` | Module | 🔴 Not Started | P3 | Keep structure |
| `/modules/embedder` | Embedder Module | `/modules/embedder` | Module | 🔴 Not Started | P3 | Keep structure |
| `/modules/parser` | Parser Module | `/modules/parser` | Module | 🔴 Not Started | P3 | Keep structure |
| `/modules/proxy-module` | Proxy Module | `/modules/proxy-module` | Module | 🔴 Not Started | P3 | Keep structure |
| `/modules/connectors/filesystem-crawler` | FS Crawler | `/modules/connectors/filesystem-crawler` | Module | 🔴 Not Started | P3 | Keep structure |
| **Testing Infrastructure** |
| `/integration-test` | Integration Tests | `/testing/integration` | Tests | 🔴 Not Started | P2 | System tests |
| **To Be Removed** |
| `/engine/dynamic-grpc` | Dynamic gRPC Engine | N/A | Deprecated | ⚫ Remove | N/A | Replaced by extension |
| `/rokkon-bom` | Unknown BOM | N/A | Unknown | ⚫ Investigate | N/A | Check purpose first |
| `/example-extension` | Example Extension | N/A | Example | ⚫ Remove | N/A | Not needed |

### Status Legend
- 🔴 Not Started
- 🟡 In Progress
- 🟢 Completed
- ⚫ Blocked/Remove

### Priority Legend
- P0: Critical - Foundation (Week 1)
- P1: High - Core functionality (Week 1-2)
- P2: Medium - Supporting features (Week 3-4)
- P3: Low - Additional modules (Week 5+)

## New Project Structure

```mermaid
graph TB
    subgraph "Pipeline Project Root"
        Root[pipeline-project/]
        
        subgraph "Build Infrastructure"
            Root --> BuildSrc[buildSrc/]
            Root --> Gradle[gradle/]
            Gradle --> LibsVersions[libs.versions.toml]
        end
        
        subgraph "Bill of Materials"
            Root --> BOM[bom/]
            BOM --> PipelineBOM[pipeline-bom/]
        end
        
        subgraph "Protocol Buffers"
            Root --> Protobuf[protobuf/]
        end
        
        subgraph "Libraries (All Quarkus-based)"
            Root --> Libraries[libraries/]
            Libraries --> PipelineAPI[pipeline-api/]
            Libraries --> PipelineCommons[pipeline-commons/]
            Libraries --> ConsulClient[consul-client/]
            Libraries --> DataUtil[data-util/]
            Libraries --> Validators[validators/]
            Libraries --> TestingCommons[testing-commons/]
            Libraries --> TestingServerUtil[testing-server-util/]
        end
        
        subgraph "Quarkus Extensions"
            Root --> Extensions[extensions/]
            Extensions --> GrpcStubs[grpc-stubs/]
            GrpcStubs --> GrpcStubsRT[runtime/]
            GrpcStubs --> GrpcStubsDep[deployment/]
            GrpcStubs --> GrpcStubsIT[integration-tests/]
            
            Extensions --> DynamicGrpc[dynamic-grpc/]
            DynamicGrpc --> DynamicGrpcRT[runtime/]
            DynamicGrpc --> DynamicGrpcDep[deployment/]
            DynamicGrpc --> DynamicGrpcIT[integration-tests/]
            
            Extensions --> DevServices[dev-services/]
            DevServices --> ConsulDS[consul/]
            ConsulDS --> ConsulDSRT[runtime/]
            ConsulDS --> ConsulDSDep[deployment/]
            ConsulDS --> ConsulDSIT[integration-tests/]
            
            Extensions --> PipelineDevUI[pipeline-dev-ui/]
            PipelineDevUI --> DevUIRT[runtime/]
            PipelineDevUI --> DevUIDep[deployment/]
        end
        
        subgraph "Applications"
            Root --> Applications[applications/]
            Applications --> PipelineEngine[pipeline-engine/]
            Applications --> CLI[cli/]
            CLI --> RegisterModule[register-module/]
            CLI --> SeedConsul[seed-consul-config/]
        end
        
        subgraph "Pipeline Modules"
            Root --> Modules[modules/]
            Modules --> Echo[echo/]
            Modules --> TestingHarness[testing-harness/]
            Modules --> Chunker[chunker/]
            Modules --> Embedder[embedder/]
            Modules --> Parser[parser/]
            Modules --> ProxyModule[proxy-module/]
            Modules --> Connectors[connectors/]
            Connectors --> FSCrawler[filesystem-crawler/]
        end
        
        subgraph "Testing"
            Root --> Testing[testing/]
            Testing --> Integration[integration/]
        end
    end
    
    %% Dependency relationships
    PipelineEngine -.-> DynamicGrpc
    PipelineEngine -.-> ConsulClient
    PipelineEngine -.-> PipelineAPI
    
    Modules -.-> GrpcStubs
    Modules -.-> PipelineAPI
    
    DynamicGrpc -.-> ConsulClient
    DynamicGrpc -.-> GrpcStubs
    
    Extensions -.-> Libraries
    Applications -.-> Libraries
    Applications -.-> Extensions
    Libraries -.-> Protobuf
```

## Migration Progress Summary

### Overall Progress: ~45% Complete

| Phase | Status | Progress | Details |
|-------|--------|----------|---------|
| Phase 1: Foundation | 🟢 Complete | 100% | All BOMs consolidated, proto files in grpc-stubs |
| Phase 2: Libraries | 🟡 In Progress | 95% | consul-client nearly complete (53/54 tests) |
| Phase 3: Extensions | 🔴 Not Started | 25% | grpc-stubs done as library, 3 extensions remaining |
| Phase 4: Core Application | 🔴 Not Started | 0% | Pipeline engine not migrated |
| Phase 5: Modules & CLI | 🔴 Not Started | 0% | No modules or CLI apps migrated |
| Phase 6: Testing & Cleanup | 🔴 Not Started | 0% | Integration tests pending |

### Test Status
- ✅ Most migrated components have passing tests (53/54 in consul-client)
- ✅ No duplicate keys or conflicts
- ✅ Build successful for all migrated modules
- ✅ Docker client injection working in testing-server-util
- ✅ Jandex indexing fixed for validators and pipeline-api
- ✅ Configuration mapping implemented for pipeline properties
- 🟡 1 test failing due to JSON schema validation issue

### Next Priority Actions
1. **P1 - High Priority**:
   - Fix remaining test failure in `consul-client` (JSON schema validation)
   - Start migrating `dynamic-grpc` extension
   - Begin `pipeline-engine` application migration

2. **P2 - Medium Priority**:
   - Migrate consul dev services extension
   - Start migrating core modules (echo, testing-harness)
   - Set up integration test framework

## Migration Phases

### Phase 1: Foundation (Week 1) ✅ COMPLETED
1. Create base directory structure ✅
2. Set up root build files with Gradle Kotlin DSL ✅
3. Create consolidated BOM (`pipeline-bom`) ✅
4. Configure version catalog ✅
5. ~~Create protobuf resource JAR~~ Proto files in grpc-stubs ✅

### Phase 2: Libraries (Week 1-2) 🟡 95% Complete
1. Migrate pipeline-api (from commons/interface) ✅
2. Migrate pipeline-commons (from commons/util) ✅
3. Create consul-client library (from engine/consul) 🟡 (53 of 54 tests passing)
4. Migrate testing-commons and other libraries ✅
   - testing-commons ✅
   - data-util ✅
   - validators ✅
   - testing-server-util ✅

### Phase 3: Extensions (Week 2-3) 🔴 25% Complete
1. ~~Migrate grpc-stubs extension~~ Converted to library ✅
2. Rebuild dynamic-grpc extension properly (not from engine/dynamic-grpc) 🔴
3. Migrate consul dev services 🔴
4. Migrate pipeline-dev-ui 🔴

### Phase 4: Core Application (Week 3-4) 🔴 Not Started
1. Migrate pipeline-engine (from engine/pipestream) 🔴
2. Ensure all dependencies are properly wired 🔴
3. Update configuration for new structure 🔴

### Phase 5: Modules & CLI (Week 4-5) 🔴 Not Started
1. Migrate echo and testing-harness modules first 🔴
2. Migrate remaining modules 🔴
3. Migrate CLI applications 🔴
4. Update module registration logic 🔴

### Phase 6: Testing & Cleanup (Week 5-6) 🔴 Not Started
1. Update integration tests 🔴
2. Update Docker configurations 🔴
3. Remove old/deprecated code 🔴
4. Final testing and documentation 🔴

## Key Achievements So Far

1. **Successful BOM Consolidation**: All 5 BOMs merged into single `pipeline-bom`
2. **Clean Multi-Module Structure**: Converted from composite builds to regular subprojects
3. **No Split Package Issues**: Resolved by excluding Google proto packages from grpc-stubs JAR
4. **Docker Client Injection**: Created CDI producer for proper injection in testing-server-util
5. **Jandex Indexing**: Fixed missing indexes for validators and pipeline-api libraries
6. **Config Mapping**: Created proper configuration interfaces for pipeline properties
7. **Consul Client Migration**: 98% complete (53/54 tests passing)
8. **Proto Files Management**: Successfully integrated proto files into grpc-stubs library

## Lessons Learned

1. **grpc-stubs as Library**: Works better as a library than extension for this use case
2. **Dependency Scoping**: Changed from `api` to `implementation` for better encapsulation
3. **Docker-Java Version**: Must align with version managed by Quarkus BOM (3.4.2)
4. **Build Structure**: Regular multi-module builds simpler than composite builds for this project
5. **Jandex Indexing**: Libraries need explicit Jandex plugin configuration for CDI bean discovery
6. **Configuration Mapping**: Use `@ConfigMapping` interfaces instead of individual `@ConfigProperty` annotations
7. **Test Configuration**: Provide test-specific application.yml to avoid config validation failures

## Key Decisions

### Extension vs Library
**Extensions** are needed when:
- Build-time processing is required
- Native compilation hints needed
- Dev services or other build augmentation
- Synthetic bean creation at build time

**Libraries** are sufficient when:
- Just providing CDI beans and services
- No build-time processing needed
- Standard runtime functionality

### Module Structure
- Modules remain as Quarkus applications
- Keep existing `src/integrationTest/` structure
- Each module is independently deployable

### BOM Consolidation
- Single BOM instead of 5 separate ones
- Simpler dependency management
- All versions in one place

## Success Criteria
1. All tests passing
2. Native compilation working
3. Dev mode fully functional
4. Docker builds successful
5. Module dynamic loading operational
6. Clear separation of concerns
7. Improved build times

## Risks & Mitigations
1. **Risk**: Breaking existing functionality
   - **Mitigation**: Migrate incrementally with testing at each phase

2. **Risk**: Circular dependencies
   - **Mitigation**: Clear layering with dependency flow rules

3. **Risk**: Lost features during migration
   - **Mitigation**: Comprehensive testing and feature inventory

4. **Risk**: Team disruption during migration
   - **Mitigation**: Keep old structure running in parallel until ready