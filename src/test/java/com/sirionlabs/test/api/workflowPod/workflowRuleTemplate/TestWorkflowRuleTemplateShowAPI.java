package com.sirionlabs.test.api.workflowPod.workflowRuleTemplate;

import com.sirionlabs.api.workflowRuleTemplate.WorkflowRuleTemplateShow;
import com.sirionlabs.dto.workflowRuleTemplate.WorkflowRuleTemplateShowDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.WorkflowRuleTemplateDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWorkflowRuleTemplateShowAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRuleTemplateShowAPI.class);

    private String testingType;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowRuleTemplate";
        String dataFileName = "workflowRuleTemplateShowAPIData.json";

        List<WorkflowRuleTemplateShowDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowRuleTemplateShowDTO dtoObject = getShowDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowRuleTemplateShowDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowRuleTemplateShowDTO getShowDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowRuleTemplateShowDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int ruleTemplateId = jsonObj.getInt("workflowRuleTemplateId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowRuleTemplateShowDTO(testCaseId, description, ruleTemplateId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowRuleTemplateShow DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowRuleTemplateShowAPI(WorkflowRuleTemplateShowDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            APIResponse response = WorkflowRuleTemplateShow.getWorkflowRuleTemplateShowResponse(dtoObject.getWorkflowRuleTemplateId());

            int actualStatusCode = response.getResponseCode();
            csAssert.assertTrue(actualStatusCode == dtoObject.getExpectedStatusCode(), "Expected Status Code: " +
                    dtoObject.getExpectedStatusCode() + " and Actual Status Code: " + actualStatusCode);

            String responseBody = response.getResponseBody();

            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            if (expectedErrorMessage == null) {
                //Validate data from DB.
                String selectedColumns = "id, rule_template_name, active, entity_type_id, rule_template_json";
                List<String> allDataForRuleTemplateFromDb = WorkflowRuleTemplateDbHelper.getRuleTemplateDataFromDB(selectedColumns, dtoObject.getWorkflowRuleTemplateId());

                if (allDataForRuleTemplateFromDb == null || allDataForRuleTemplateFromDb.isEmpty()) {
                    throw new SkipException("Couldn't get Data from DB for Rule Template Id: " + dtoObject.getWorkflowRuleTemplateId());
                }

                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    JSONObject jsonObj = new JSONObject(responseBody).getJSONObject("body").getJSONObject("data");

                    //Validate Status in response
                    String status = ParseJsonResponse.getStatusFromResponse(responseBody);
                    csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Status: Success and Actual Status: " + status);

                    //Validate Rule Template Id
                    int actualRuleTemplateId = jsonObj.getJSONObject("id").getInt("values");
                    csAssert.assertTrue(actualRuleTemplateId == dtoObject.getWorkflowRuleTemplateId(), "Expected Rule Template Id: " +
                            dtoObject.getWorkflowRuleTemplateId() + " and Actual Rule Template Id: " + actualRuleTemplateId);

                    //Validate Rule Template Name
                    String expectedRuleTemplateName = allDataForRuleTemplateFromDb.get(1);
                    String actualRuleTemplateName = jsonObj.getJSONObject("workflowRuleTemplateName").getString("values");
                    csAssert.assertTrue(expectedRuleTemplateName.equalsIgnoreCase(actualRuleTemplateName), "Expected Rule Template Name: " +
                            expectedRuleTemplateName + " and Actual Rule Template Name: " + actualRuleTemplateName);

                    //Validate Active Value
                    String expectedActiveValue = allDataForRuleTemplateFromDb.get(2);
                    expectedActiveValue = expectedActiveValue.equalsIgnoreCase("t") ? "true" : "false";
                    boolean actualActiveValue = jsonObj.getJSONObject("active").getBoolean("values");
                    csAssert.assertTrue(expectedActiveValue.equalsIgnoreCase(String.valueOf(actualActiveValue)), "Expected Active Value: " +
                            expectedActiveValue + " and Actual Active Value: " + actualActiveValue);

                    //Validate EntityTypeId
                    String expectedEntityTypeId = allDataForRuleTemplateFromDb.get(3);
                    int actualEntityTypeId = jsonObj.getJSONObject("entityType").getJSONObject("values").getInt("id");
                    csAssert.assertTrue(expectedEntityTypeId.contains(String.valueOf(actualEntityTypeId)), "Expected EntityTypeId: " +
                            expectedEntityTypeId + " and Actual EntityTypeId: " + actualEntityTypeId);

                    //Validate Rule Template Json Value
                    String expectedJsonValue = allDataForRuleTemplateFromDb.get(4);
                    String actualJsonValue = jsonObj.getJSONObject("workflowRuleTemplateJson").getString("values");
                    csAssert.assertTrue(expectedJsonValue.equalsIgnoreCase(actualJsonValue), "Expected Json Value: " + expectedJsonValue +
                            " and Actual Json Value: " + actualJsonValue);
                } else {
                    csAssert.assertTrue(false, "Workflow Rule Template Show API Response for Rule Template Id: " +
                            dtoObject.getWorkflowRuleTemplateId() + " is an Invalid JSON");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }
}