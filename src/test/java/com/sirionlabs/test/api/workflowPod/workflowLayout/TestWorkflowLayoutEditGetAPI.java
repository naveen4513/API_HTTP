package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.api.workflowLayout.WorkflowLayoutEditGet;
import com.sirionlabs.dto.workflowLayout.WorkflowLayoutEditGetDTO;
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

public class TestWorkflowLayoutEditGetAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutEditGetAPI.class);

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
        String dataFileName = "workflowLayoutEditGetAPIData.json";

        List<WorkflowLayoutEditGetDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowLayoutEditGetDTO dtoObject = getUpdateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowLayoutEditGetDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowLayoutEditGetDTO getUpdateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowLayoutEditGetDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int layoutId = jsonObj.getInt("workflowLayoutId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowLayoutEditGetDTO(testCaseId, description, layoutId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowLayoutEditGet DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowLayoutEditGetAPI(WorkflowLayoutEditGetDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String apiPath = WorkflowLayoutEditGet.getApiPath(dtoObject.getWorkflowLayoutId());
            APIResponse response = WorkflowLayoutEditGet.getEditGetResponse(apiPath, WorkflowLayoutEditGet.getHeaders());

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

                    //Validate Edit Page Tabs Selected Values
                    List<Integer> allIdsInAPIResponse = WorkflowLayoutEditGet.getAllSelectedEditPageTabIds(responseBody);
                    validateSelectedTabAndFieldIds(allDataForLayoutFromDb.get(2), allIdsInAPIResponse, "Edit Page Tabs Selected", csAssert);

                    //Validate Show Page Tabs Selected Values
                    allIdsInAPIResponse = WorkflowLayoutEditGet.getAllSelectedShowPageTabIds(responseBody);
                    validateSelectedTabAndFieldIds(allDataForLayoutFromDb.get(3), allIdsInAPIResponse, "Show Page Tabs Selected", csAssert);

                    //Validate Editable Fields Show Page Selected Values
                    allIdsInAPIResponse = WorkflowLayoutEditGet.getAllSelectedEditableFieldsShowPageIds(responseBody);
                    validateSelectedTabAndFieldIds(allDataForLayoutFromDb.get(4), allIdsInAPIResponse, "Editable Fields Show Page Field Selected", csAssert);

                    //Validate Editable Fields Edit Page Selected Values
                    allIdsInAPIResponse = WorkflowLayoutEditGet.getAllSelectedEditableFieldsEditPageIds(responseBody);
                    validateSelectedTabAndFieldIds(allDataForLayoutFromDb.get(5), allIdsInAPIResponse, "Editable Fields Edit Page Field Selected", csAssert);

                    //Validate Edit Page Tabs Options
                    List<Integer> allOptionIdsInAPIResponse = WorkflowLayoutEditGet.getAllOptionsOfEditPageTabIds(responseBody);
                    csAssert.assertTrue((allOptionIdsInAPIResponse != null && !allOptionIdsInAPIResponse.isEmpty()),
                            "Edit Page Tabs Options not available in API Response.");

                    //Validate Show Page Tabs Options
                    allOptionIdsInAPIResponse = WorkflowLayoutEditGet.getAllOptionsOfShowPageTabIds(responseBody);
                    csAssert.assertTrue((allOptionIdsInAPIResponse != null && !allOptionIdsInAPIResponse.isEmpty()),
                            "Show Page Tabs Options not available in API Response.");

                    //Validate Editable Fields Show Page Options
                    allOptionIdsInAPIResponse = WorkflowLayoutEditGet.getAllOptionsOfEditableFieldsShowPageIds(responseBody);
                    csAssert.assertTrue((allOptionIdsInAPIResponse != null && !allOptionIdsInAPIResponse.isEmpty()),
                            "Editable Fields Show Page Options not available in API Response.");

                    //Validate Editable Fields Edit Page Options
                    allOptionIdsInAPIResponse = WorkflowLayoutEditGet.getAllOptionsOfEditableFieldsEditPageIds(responseBody);
                    csAssert.assertTrue((allOptionIdsInAPIResponse != null && !allOptionIdsInAPIResponse.isEmpty()),
                            "Editable Fields Edit Page Options not available in API Response.");

                    //Validate EntityTypeId
                    String expectedEntityTypeId = allDataForLayoutFromDb.get(6);
                    int actualEntityTypeId = jsonObj.getJSONObject("entityTypeReq").getJSONObject("values").getInt("id");
                    csAssert.assertTrue(expectedEntityTypeId.contains(String.valueOf(actualEntityTypeId)), "Expected EntityTypeId: " +
                            expectedEntityTypeId + " and Actual EntityTypeId: " + actualEntityTypeId);
                } else {
                    csAssert.assertTrue(false, "Workflow Layout Edit API Response for Layout Id: " + dtoObject.getWorkflowLayoutId() +
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

    private void validateSelectedTabAndFieldIds(String tabValueInDb, List<Integer> allIdsInAPIResponse, String fieldInfo, CustomAssert csAssert) {
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