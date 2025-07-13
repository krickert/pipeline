package com.pipeline.extension.grpc.stubs.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;

class GrpcStubsProcessor {

    private static final String FEATURE = "grpc-stubs";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    IndexDependencyBuildItem indexProtobufModule() {
        // Index our protobuf module so generated classes can be discovered
        return new IndexDependencyBuildItem("com.pipeline", "protobuf");
    }

    @BuildStep
    IndexDependencyBuildItem indexRuntimeModule() {
        // Index the runtime module for CDI discovery
        return new IndexDependencyBuildItem("com.pipeline", "grpc-stubs");
    }
}