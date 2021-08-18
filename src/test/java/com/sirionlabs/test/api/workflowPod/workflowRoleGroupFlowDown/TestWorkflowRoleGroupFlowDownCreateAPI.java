package com.sirionlabs.test.api.workflowPod.workflowRoleGroupFlowDown;

import com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate;
import com.sirionlabs.dto.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreateDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.RoleGroupFlowDownDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWorkflowRoleGroupFlowDownCreateAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRoleGroupFlowDownCreateAPI.class);

    private String testingType;
    private List<Integer> newlyCreatedRoleGroupFlowDownIds = new ArrayList<>();

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @AfterClass
    public void afterClass() {
        //Delete all newly created Role Group Flow Down in DB.
        for (Integer roleGroupFlowDownId : newlyCreatedRoleGroupFlowDownIds) {
            RoleGroupFlowDownDbHelper.deleteRoleGroupFlowDownDataInDb(roleGroupFlowDownId);
        }
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowRoleGroupFlowDown";
        String dataFileName = "workflowRoleGroupFlowDownCreateAPIData.json";

        List<WorkflowRoleGroupFlowDownCreateDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowRoleGroupFlowDownCreateDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowRoleGroupFlowDownCreateDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowRoleGroupFlowDownCreateDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowRoleGroupFlowDownCreateDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String parentEntityTypeId = jsonObj.getString("parentEntityTypeId");
            String childEntityTypeId = jsonObj.getString("childEntityTypeId");
            String roleGroupId = jsonObj.getString("roleGroupId");
            String clientId = jsonObj.getString("clientId");
            String deleted = jsonObj.getString("deleted");

            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowRoleGroupFlowDownCreateDTO(testCaseId, description, parentEntityTypeId, childEntityTypeId, roleGroupId, clientId, deleted,
                    expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowRoleGroupFlowDownCreate DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowRoleGroupFlowDownCreateAPI(WorkflowRoleGroupFlowDownCreateDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String[] parentEntityTypeIdArr = dtoObject.getParentEntityTypeId().split(",");
            String[] childEntityTypeIdArr = dtoObject.getChildEntityTypeId().split(",");
            String[] roleGroupIdArr = dtoObject.getRoleGroupId().split(",");
            String[] clientIdArr = dtoObject.getClientId().split(",");
            String[] deletedArr = dtoObject.getDeleted().split(",");

            String[] expectedStatusArr = new String[parentEntityTypeIdArr.length];

            for (int i = 0; i < parentEntityTypeIdArr.length; i++) {
                boolean isEntryPresent = RoleGroupFlowDownDbHelper.isRoleGroupFlowDownPresentInDb(parentEntityTypeIdArr[i], childEntityTypeIdArr[i],
                        roleGroupIdArr[i], clientIdArr[i]);

                String expectedStatus = isEntryPresent ? "update" : "create";
                expectedStatusArr[i] = expectedStatus;
            }

            String payload = WorkflowRoleGroupFlowDownCreate.getPayload(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, clientIdArr, deletedArr);

            APIResponse response = WorkflowRoleGroupFlowDownCreate.getCreateResponse(payload);

            String responseBody = response.getResponseBody();
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseBody), "Create API Response is an Invalid JSON.");

            int actualResponseCode = response.getResponseCode();

            csAssert.assertTrue(expectedResponseCode == actualResponseCode, "Expected Response Code: " + expectedResponseCode +
                    " and Actual Response Code: " + actualResponseCode);

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    //Validate Response
                    boolean actualStatus = new JSONObject(responseBody).getBoolean("success");
                    csAssert.assertTrue(actualStatus, "API Response Validation failed. Expected Result: Success and Actual Result: Failure");

                    //Validate Data in DB.
                    validateDataInDb(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, clientIdArr, deletedArr, expectedStatusArr, csAssert);
                }
            } else {
                //Validate Error Message
                JSONArray errorMessagesArr = new JSONObject(responseBody).getJSONArray("errorMessages");

                boolean errorMessageFound = false;
                for (int i = 0; i < errorMessagesArr.length(); i++) {
                    String message = errorMessagesArr.getJSONObject(i).getString("errorMessage").toLowerCase();

                    if (message.contains(expectedErrorMessage.toLowerCase())) {
                        errorMessageFound = true;
                        break;
                    }
                }

                csAssert.assertTrue(errorMessageFound, "Expected Error Message: [" + expectedErrorMessage + "] not found in API Response.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateDataInDb(String[] parentEntityTypeIdArr, String[] childEntityTypeIdArr, String[] roleGroupIdArr, String[] clientIdArr,
                                  String[] deletedArr, String[] expectedStatusArr, CustomAssert csAssert) {
        try {
            String selectedColumns = "id, deleted, date_modified";

            for (int i = 0; i < parentEntityTypeIdArr.length; i++) {
                List<String> dataInDb = RoleGroupFlowDownDbHelper.getRoleGroupFlowDownDataFromDB(selectedColumns, parentEntityTypeIdArr[i], childEntityTypeIdArr[i],
                        roleGroupIdArr[i], clientIdArr[i]);

                if (!dataInDb.isEmpty()) {
                    if (!newlyCreatedRoleGroupFlowDownIds.contains(Integer.parseInt(dataInDb.get(0)))) {
                        newlyCreatedRoleGroupFlowDownIds.add(Integer.parseInt(dataInDb.get(0)));
                    }

                    //Verify Deleted value
                    String deletedValueInDb = dataInDb.get(1);
                    deletedValueInDb = deletedValueInDb.equalsIgnoreCase("t") ? "true" : "false";
                    csAssert.assertTrue(deletedValueInDb.equalsIgnoreCase(deletedArr[i].trim()), "Data Validation in DB failed. Expected Deleted Value: " +
                            deletedArr[i] + " and Actual Value in DB: " + deletedValueInDb);

                    //Verify Modified Date
                    if (expectedStatusArr[i].equalsIgnoreCase("create")) {
                        csAssert.assertTrue(dataInDb.get(2) == null, "Modified Date is not null in DB.");
                    } else {
                        csAssert.assertTrue(dataInDb.get(2) != null, "Modified Date is null in DB.");
                    }
                } else {
                    csAssert.assertTrue(false, "Entry not found in DB for Role Group Record Id " + (i + 1) + ". ParentEntityTypeId " +
                            parentEntityTypeIdArr[i] + ", ChildEntityTypeId " + childEntityTypeIdArr[i] + ", RoleGroupId " + roleGroupIdArr[i] + ", Client Id " +
                            clientIdArr[i] + " and Deleted " + deletedArr[i]);
                }
            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Create API Response and Data. " + e.getMessage());
        }
    }
}