package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.api.workflowLayout.WorkflowLayoutCreateForm;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;

public class TestWorkflowLayoutCreateFormAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutCreateFormAPI.class);

    @Test
    public void testWorkflowLayoutCreateFormAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating WorkflowLayout Create Form API.");
            APIResponse response = WorkflowLayoutCreateForm.getCreateFormResponse(WorkflowLayoutCreateForm.getApiPath(), WorkflowLayoutCreateForm.getHeaders());

            csAssert.assertTrue(response.getResponseCode() == 200, "Expected Response Code: 200 and Actual Response Code: " + response.getResponseCode());
            String responseBody = response.getResponseBody();

            if (ParseJsonResponse.validJsonResponse(responseBody)) {
                Integer[] expectedEntityTypeIds = {18, 63, 13, 15, 138, 176, 160, 61, 140, 139, 28, 86, 87, 16, 165, 67, 17, 12, 181, 64, 14, 1, 3, 80};
                List<Integer> actualEntityTypeIds = WorkflowLayoutCreateForm.getAllEntityTypeIdsFromResponse(responseBody);

                if (actualEntityTypeIds == null || actualEntityTypeIds.isEmpty()) {
                    throw new SkipException("Couldn't get EntityTypeIds List from Workflow Layout CreateForm API Response.");
                }

                for (Integer entityTypeId : expectedEntityTypeIds) {
                    csAssert.assertTrue(actualEntityTypeIds.contains(entityTypeId), "Entity Id: " + entityTypeId +
                            " is not present in Workflow Layout CreateForm API Response");
                }
            } else {
                csAssert.assertTrue(false, "Workflow Layout Create Form API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Workflow Layout Create Form API. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}