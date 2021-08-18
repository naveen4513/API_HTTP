package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.api.workflowLayout.WorkflowLayoutShow;
import com.sirionlabs.dto.workflowLayout.WorkflowLayoutShowDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.WorkflowLayoutDbHelper;
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

public class TestWorkflowLayoutShowAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutShowAPI.class);

    private String testingType;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowLayout";
        String dataFileName = "workflowLayoutShowAPIData.json";

        List<WorkflowLayoutShowDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowLayoutShowDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowLayoutShowDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowLayoutShowDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowLayoutShowDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int layoutId = jsonObj.getInt("workflowLayoutId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowLayoutShowDTO(testCaseId, description, layoutId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowLayoutShow DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowLayoutShowAPI(WorkflowLayoutShowDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String apiPath = WorkflowLayoutShow.getApiPath(dtoObject.getWorkflowLayoutId());
            APIResponse response = WorkflowLayoutShow.getWorkflowLayoutShowResponse(apiPath, WorkflowLayoutShow.getHeaders());

            int actualStatusCode = response.getResponseCode();
            csAssert.assertTrue(actualStatusCode == dtoObject.getExpectedStatusCode(), "Expected Status Code: " +
                    dtoObject.getExpectedStatusCode() + " and Actual Status Code: " + actualStatusCode);

            String responseBody = response.getResponseBody();

            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            if (expectedErrorMessage == null) {
                //Validate data from DB.
                String selectedColumns = "id, name, edit_page_tabs, show_page_tabs, editable_fields_show_page, editable_fields_edit_page, entity_type_id";
                List<String> allDataForLayoutFromDb = WorkflowLayoutDbHelper.getLayoutDataFromDB(selectedColumns, dtoObject.getWorkflowLayoutId());

                if (allDataForLayoutFromDb == null || allDataForLayoutFromDb.isEmpty()) {
                    throw new SkipException("Couldn't get Data from DB for Layout Id: " + dtoObject.getWorkflowLayoutId());
                }

                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    JSONObject jsonObj = new JSONObject(responseBody).getJSONObject("body").getJSONObject("data");

                    //Validate Status in response
                    String status = ParseJsonResponse.getStatusFromResponse(responseBody);
                    csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Status: Success and Actual Status: " + status);

                    //Validate Layout Id
                    int actualLayoutId = jsonObj.getJSONObject("id").getInt("values");
                    csAssert.assertTrue(actualLayoutId == dtoObject.getWorkflowLayoutId(), "Expected Layout Id: " + dtoObject.getWorkflowLayoutId() +
                            " and Actual Layout Id: " + actualLayoutId);

                    //Validate Layout Name
                    String expectedLayoutName = allDataForLayoutFromDb.get(1);
                    String actualLayoutName = jsonObj.getJSONObject("workflowLayoutGroup").getString("values");
                    csAssert.assertTrue(expectedLayoutName.equalsIgnoreCase(actualLayoutName), "Expected Layout Name: " + expectedLayoutName +
                            " and Actual Layout Name: " + actualLayoutName);

                    //Validate Edit Page Tabs
                    List<Integer> allIdsInAPIResponse = WorkflowLayoutShow.getAllEditPageTabIds(responseBody);
                    validateTabAndFieldIds(allDataForLayoutFromDb.get(2), allIdsInAPIResponse, "Edit Page Tabs", csAssert);

                    //Validate Show Page Tabs
                    allIdsInAPIResponse = WorkflowLayoutShow.getAllShowPageTabIds(responseBody);
                    validateTabAndFieldIds(allDataForLayoutFromDb.get(3), allIdsInAPIResponse, "Show Page Tabs", csAssert);

                    //Validate Editable Fields Show Page
                    allIdsInAPIResponse = WorkflowLayoutShow.getAllEditableFieldsShowPageIds(responseBody);
                    validateTabAndFieldIds(allDataForLayoutFromDb.get(4), allIdsInAPIResponse, "Editable Fields Show Page Field", csAssert);

                    //Validate Editable Fields Edit Page
                    allIdsInAPIResponse = WorkflowLayoutShow.getAllEditableFieldsEditPageIds(responseBody);
                    validateTabAndFieldIds(allDataForLayoutFromDb.get(5), allIdsInAPIResponse, "Editable Fields Edit Page Field", csAssert);

                    //Validate EntityTypeId
                    String expectedEntityTypeId = allDataForLayoutFromDb.get(6);
                    int actualEntityTypeId = jsonObj.getJSONObject("entityTypeReq").getJSONObject("values").getInt("id");
                    csAssert.assertTrue(expectedEntityTypeId.contains(String.valueOf(actualEntityTypeId)), "Expected EntityTypeId: " +
                            expectedEntityTypeId + " and Actual EntityTypeId: " + actualEntityTypeId);
                } else {
                    csAssert.assertTrue(false, "Workflow Layout Show API Response for Layout Id: " + dtoObject.getWorkflowLayoutId() +
                            " is an Invalid JSON");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateTabAndFieldIds(String tabValueInDb, List<Integer> allIdsInAPIResponse, String fieldInfo, CustomAssert csAssert) {
        try {
            logger.info("{} Value in DB: {}", fieldInfo, tabValueInDb);

            for (Integer id : allIdsInAPIResponse) {
                csAssert.assertTrue(tabValueInDb.contains(id.toString()), fieldInfo + " Value having Id: " +
                        id + " is not present in DB");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Value of " + fieldInfo + ". " + e.getMessage());
        }
    }
}