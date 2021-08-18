package com.sirionlabs.test.api.workflowPod.workflowButtons;

import com.sirionlabs.api.workflowButtons.WorkflowButtonCreateForm;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class TestWorkflowButtonsCreateFormAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowButtonsCreateFormAPI.class);


    @Test
    public void testWorkflowButtonsCreateFormAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating WorkflowButtons Create Form API.");
            String createFormResponse = WorkflowButtonCreateForm.getCreateFormResponse(WorkflowButtonCreateForm.getApiPath(),
                    WorkflowButtonCreateForm.getHeaders()).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(createFormResponse)) {
                JSONObject jsonObj = new JSONObject(createFormResponse).getJSONObject("body").getJSONObject("data");

                //Validate Active field
                boolean activeValue = jsonObj.getJSONObject("active").getBoolean("values");
                csAssert.assertTrue(!activeValue, "Expected Active value: false and Actual Value: " + activeValue);

                //Validate other fields like name, description, color and id are present
                csAssert.assertTrue(jsonObj.has("color"), "Color field not present in CreateForm API Response");
                csAssert.assertTrue(jsonObj.has("description"), "Description field not present in CreateForm API Response");
                csAssert.assertTrue(jsonObj.has("name"), "Name field not present in CreateForm API Response");
                csAssert.assertTrue(jsonObj.has("id"), "Id field not present in CreateForm API Response");
            } else {
                csAssert.assertTrue(false, "Workflow Buttons Create Form API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Workflow Buttons Create Form API. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}