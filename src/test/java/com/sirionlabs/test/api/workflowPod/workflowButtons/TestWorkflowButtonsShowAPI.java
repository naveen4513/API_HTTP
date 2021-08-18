package com.sirionlabs.test.api.workflowPod.workflowButtons;

import com.sirionlabs.api.workflowButtons.WorkflowButtonShow;
import com.sirionlabs.dto.workflowButtons.WorkflowButtonsShowDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.WorkflowButtonsDbHelper;
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

public class TestWorkflowButtonsShowAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowButtonsShowAPI.class);

    private String testingType;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowButtons";
        String dataFileName = "workflowButtonsShowAPIData.json";

        List<WorkflowButtonsShowDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowButtonsShowDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowButtonsShowDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowButtonsShowDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowButtonsShowDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int buttonId = jsonObj.getInt("buttonId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowButtonsShowDTO(testCaseId, description, buttonId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowButtonsShow DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowButtonsShowAPI(WorkflowButtonsShowDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String apiPath = WorkflowButtonShow.getApiPath(dtoObject.getButtonId());
            APIResponse response = WorkflowButtonShow.getShowResponse(apiPath, WorkflowButtonShow.getHeaders());

            int actualStatusCode = response.getResponseCode();
            csAssert.assertTrue(actualStatusCode == dtoObject.getExpectedStatusCode(), "Expected Status Code: " +
                    dtoObject.getExpectedStatusCode() + " and Actual Status Code: " + actualStatusCode);

            String responseBody = response.getResponseBody();

            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            if (expectedErrorMessage == null) {
                //Validate data from DB.
                List<String> allDataForButtonFromDb = WorkflowButtonsDbHelper.getButtonDataFromDB(dtoObject.getButtonId());

                if (allDataForButtonFromDb == null || allDataForButtonFromDb.isEmpty()) {
                    throw new SkipException("Couldn't get Data from DB for Button Id: " + dtoObject.getButtonId());
                }

                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    JSONObject jsonObj = new JSONObject(responseBody).getJSONObject("body").getJSONObject("data");

                    //Validate Status in response
                    String status = ParseJsonResponse.getStatusFromResponse(responseBody);
                    csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Status: Success and Actual Status: " + status);

                    //Validate Button Id
                    int actualButtonId = jsonObj.getJSONObject("id").getInt("values");
                    csAssert.assertTrue(actualButtonId == dtoObject.getButtonId(), "Expected Button Id: " + dtoObject.getButtonId() +
                            " and Actual Button Id: " + actualButtonId);

                    //Validate Button Name
                    String expectedButtonName = allDataForButtonFromDb.get(1);
                    String actualButtonName = jsonObj.getJSONObject("name").getString("values");
                    csAssert.assertTrue(expectedButtonName.equalsIgnoreCase(actualButtonName), "Expected Button Name: " + expectedButtonName +
                            " and Actual Button Name: " + actualButtonName);

                    //Validate Active Value
                    String expectedActiveValue = allDataForButtonFromDb.get(2);
                    expectedActiveValue = expectedActiveValue.equalsIgnoreCase("t") ? "true" : "false";
                    String actualActiveValue = String.valueOf(jsonObj.getJSONObject("active").getBoolean("values"));
                    csAssert.assertTrue(expectedActiveValue.equalsIgnoreCase(actualActiveValue), "Expected Active Value: " + expectedActiveValue +
                            " and Actual Active Value: " + actualActiveValue);

                    //Validate Color Value
                    String expectedColorValue = allDataForButtonFromDb.get(3);
                    String actualColorValue = jsonObj.getJSONObject("color").getString("values");
                    csAssert.assertTrue(expectedActiveValue.equalsIgnoreCase(actualActiveValue), "Expected Color Value: " + expectedColorValue +
                            " and Actual Color Value: " + actualColorValue);

                    //Validate Button Description
                    String expectedDescription = allDataForButtonFromDb.get(4);
                    String actualDescription = jsonObj.getJSONObject("description").getString("values");
                    csAssert.assertTrue(expectedDescription.equalsIgnoreCase(actualDescription), "Expected Button Description: " + expectedDescription +
                            " and Actual Button Description: " + actualDescription);
                } else {
                    csAssert.assertTrue(false, "Workflow Button Show API Response for Button Id: " + dtoObject.getButtonId() + " is an Invalid JSON");
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