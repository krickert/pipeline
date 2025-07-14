# Consul Dev Services Extension

This document outlines the features and architecture of the Consul Dev Services extension.

## Overview

This extension provides a comprehensive Consul development and testing environment within Quarkus. It goes beyond a simple Consul container, offering a production-like sidecar architecture with fixed networking to simplify the development of distributed systems that rely on Consul for service discovery and configuration.

## Core Features

*   **Automatic Consul Instance:** Starts a Consul container automatically for development and testing if no `quarkus.consul.host` is configured.
*   **Two-Container Sidecar Pattern:** Deploys a central Consul **server** and a Consul **agent** sidecar, mirroring a realistic production topology. This allows the application to interact with a local agent, just as it would in production.
*   **Configuration Seeding:** Pre-populates the Consul KV store with configuration data on startup via the `quarkus.consul.devservices.seed-data` property. This is ideal for setting up initial application configuration.
*   **Fixed IP Allocation:** Creates a dedicated Docker network (defaulting to subnet `10.5.0.0/24`) and assigns predictable, fixed IP addresses to the Consul server, agent, and any module sidecars. This ensures stable communication between services during development.
*   **Container Reuse:** Reuses the Consul containers between runs by default (`quarkus.consul.devservices.reuse=true`) to significantly shorten feedback cycles during development and testing.
*   **Seamless Test Integration:** Includes a `QuarkusTestResourceLifecycleManager` (`ConsulDevServicesTestResource`) that guarantees the Consul instance is running *before* the Quarkus application starts during integration tests. This is critical for services that fetch configuration from Consul at boot time.
*   **Dynamic Module Sidecar Support:** The `ModuleSidecarProvider` allows pipeline modules to be launched with their own dedicated Consul agent sidecar, sharing the same network namespace. This enables modules to seamlessly register with Consul and discover other services.
*   **Automatic Test Mode Detection:** The `TestMode` enum intelligently detects the execution context (`DEVELOPMENT`, `INTEGRATION_TEST`, or `UNIT_TEST`) and configures the dev service's behavior accordingly.

## Architecture

The extension is built around a few key components:

1.  **`ConsulDevServicesProcessor` (Deployment Module):**
    *   This is the main entry point for the extension at build time.
    *   It checks if the dev service should be started (e.g., Docker is available, `quarkus.consul.host` is not set).
    *   It initiates the startup of the Consul containers via the `ConsulDevServicesProvider`.
    *   It injects the necessary configuration properties (`quarkus.consul.host`, `quarkus.consul.port`, etc.) into the application.

2.  **`ConsulDevServicesProvider` (Runtime Module):**
    *   Manages the lifecycle of the Docker containers.
    *   Creates the custom Docker network with a fixed subnet.
    *   Starts the Consul server and agent containers with fixed IPs.
    *   Handles container reuse by searching for existing containers with specific labels.
    *   Provides the `IPAllocator` for other parts of the system to use.

3.  **`IPAllocator` (Runtime Module):**
    *   A utility class that manages IP address allocation from the defined subnet.
    *   Ensures that each container gets a unique and predictable IP address.

4.  **`ModuleSidecarProvider` (Runtime Module):**
    *   Provides a factory for creating Consul agent sidecars for pipeline modules.
    *   This is a key component for building a microservices architecture, as it allows each module to have its own agent for service registration and discovery.

5.  **`ConsulDevServicesTestResource` (Runtime Module):**
    *   A `QuarkusTestResourceLifecycleManager` that is responsible for starting the Consul dev service *before* the application in integration tests.
    *   It uses static initialization to ensure that Consul is available when the Quarkus application is being built and started, which is essential for extensions like `quarkus-consul-config`.

## Configuration

The extension can be configured via `application.properties` with the following properties under the `quarkus.consul.devservices` prefix:

| Property | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable or disable the dev service. |
| `image-name` | `hashicorp/consul:1.21` | The Docker image to use for Consul. |
| `port` | (random) | A fixed host port for the Consul HTTP API. |
| `reuse` | `true` | Whether to reuse the Consul container between runs. |
| `consul-args` | (none) | Additional arguments for the `consul agent` command. |
| `network-alias` | `consul` | The network alias for the Consul container. |
| `seed-data` | (none) | A map of KV paths to JSON values to seed into Consul. |
| `log-level` | `INFO` | The log level for the Consul container. |
| `startup-timeout` | `60` | The timeout in seconds for the container to start. |
| `network-subnet` | `10.5.0.0/24` | The subnet for the Docker network. |

