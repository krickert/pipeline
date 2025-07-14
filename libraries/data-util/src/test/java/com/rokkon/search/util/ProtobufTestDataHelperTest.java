package com.rokkon.search.util;

import com.rokkon.search.util.ProtobufTestDataHelper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProtobufTestDataHelperTest extends ProtobufTestDataHelperTestBase {

    @Inject
    ProtobufTestDataHelper protobufTestDataHelper;

    @Override
    protected ProtobufTestDataHelper getProtobufTestDataHelper() {
        return protobufTestDataHelper;
    }
}