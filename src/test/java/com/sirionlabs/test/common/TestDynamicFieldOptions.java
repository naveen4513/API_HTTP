package com.sirionlabs.test.common;

import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicFieldOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TestDynamicFieldOptions {

    private final static Logger logger = LoggerFactory.getLogger(TestDynamicFieldOptions.class);

    @Test
    public void testDynamicFieldOptions() {
        try {
            String payload = "{\"fieldId\":100801,\"options\":{";

            for (int i = 1000; i < 4000; i++) {
                payload = payload.concat("\"-" + i + "\":{\"name\":\"Option " + i + "\",\"orderSeq\":" + i + ",\"active\":true},");
            }

            payload = payload.substring(0, payload.length() - 1).concat("}}");

            String responseBody = DynamicFieldOptions.getDynamicFieldOptionsResponse(payload).getResponseBody();
            logger.info("Response: {}", responseBody);
        } catch (Exception e) {
            logger.error("Exception while Creating Dynamic Field Options. " + e.getMessage());
        }
    }
}