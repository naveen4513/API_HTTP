package com.sirionlabs.test.api.workflowPod.wor340;

import com.sirionlabs.api.clientAdmin.specialEntityField.SpecialEntityFieldCreateForm;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

public class TestSpecialEntityFieldCreateFormAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestSpecialEntityFieldCreateFormAPI.class);

    @Test
    public void testSpecialEntityFieldCreateFormAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating SpecialEntityField Create Form API.");
            APIResponse response = SpecialEntityFieldCreateForm.getCreateFormResponse();

            csAssert.assertTrue(response.getResponseCode() == 200, "Expected Response Code: 200 and Actual Response Code: " + response.getResponseCode());
            String responseBody = response.getResponseBody();

            if (ParseJsonResponse.validJsonResponse(responseBody)) {
                //Validate Primary Entity Options
                Integer[] expectedPrimaryEntityTypeIds = {18, 63, 13, 15, 138, 176, 160, 61, 140, 139, 28, 86, 87, 16, 165, 67, 17, 12, 181, 64, 14, 1, 3, 80};
                List<Integer> actualPrimaryEntityTypeIds = SpecialEntityFieldCreateForm.getAllPrimaryEntityTypeIdsFromResponse(responseBody);

                if (actualPrimaryEntityTypeIds == null || actualPrimaryEntityTypeIds.isEmpty()) {
                    throw new SkipException("Couldn't get Primary EntityTypeIds List from SpecialEntityField CreateForm API Response.");
                }

                for (Integer entityTypeId : expectedPrimaryEntityTypeIds) {
                    csAssert.assertTrue(actualPrimaryEntityTypeIds.contains(entityTypeId), "Primary Entity Id: " + entityTypeId +
                            " is not present in SpecialEntityField CreateForm API Response");
                }

                //Validate Secondary Entity Options
                List<Integer> actualSecondaryEntityTypeIds = SpecialEntityFieldCreateForm.getAllSecondaryEntityTypeIdsFromResponse(responseBody);
                Integer[] expectedSecondaryEntityTypeIds = {1, 61};

                if (actualSecondaryEntityTypeIds == null || actualSecondaryEntityTypeIds.isEmpty()) {
                    throw new SkipException("Couldn't get Secondary EntityTypeIds List from SpecialEntityField CreateForm API Response.");
                }

                for (Integer entityTypeId : expectedSecondaryEntityTypeIds) {
                    csAssert.assertTrue(actualSecondaryEntityTypeIds.contains(entityTypeId), "Secondary Entity Id: " + entityTypeId +
                            " is not present in SpecialEntityField CreateForm API Response");
                }
            } else {
                csAssert.assertTrue(false, "SpecialEntityField Create Form API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating SpecialEntityField Create Form API. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}