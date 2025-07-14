package com.rokkon.search.util;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Unit test for ProtobufTestDataHelper that loads data from filesystem.
 * Uses CDI injection to get the helper instance.
 */
@QuarkusTest
class TestDataLoadingTest extends TestDataLoadingTestBase {

    @Inject
    ProtobufTestDataHelper testDataHelper;

    @Override
    protected ProtobufTestDataHelper getTestDataHelper() {
        return testDataHelper;
    }
}