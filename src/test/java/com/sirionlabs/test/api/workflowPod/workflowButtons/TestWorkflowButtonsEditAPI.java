package com.sirionlabs.test.api.workflowPod.workflowButtons;

import com.sirionlabs.api.workflowButtons.WorkflowButtonEdit;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowButtons.WorkflowButtonsEditDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.AuditLogsDbHelper;
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

public class TestWorkflowButtonsEditAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowButtonsEditAPI.class);

    private String testingType;
    private int clientId;


    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;

        clientId = new AdminHelper().getClientId();
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowButtons";
        String dataFileName = "workflowButtonsEditAPIData.json";

        List<WorkflowButtonsEditDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowButtonsEditDTO dtoObject = getUpdateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowButtonsEditDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowButtonsEditDTO getUpdateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowButtonsEditDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int buttonId = jsonObj.getInt("buttonId");
            String buttonName = (jsonObj.has("buttonName") && !jsonObj.isNull("buttonName")) ? jsonObj.getString("buttonName") : null;
            String color = (jsonObj.has("color") && !jsonObj.isNull("color")) ? jsonObj.getString("color") : null;
            String buttonDescription = jsonObj.getString("buttonDescription");
            Boolean active = jsonObj.getBoolean("active");
            int entityTypeId = jsonObj.getInt("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowButtonsEditDTO(testCaseId, description, buttonId, buttonName, color, buttonDescription, active, entityTypeId, expectedStatusCode,
                    expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowButtonsEdit DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowButtonsEditAPI(WorkflowButtonsEditDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            int buttonId = dtoObject.getButtonId();
            List<String> buttonDataBeforeEditAPICall = WorkflowButtonsDbHelper.getButtonDataFromDB(buttonId);

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 328);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String payload = WorkflowButtonEdit.getPayload(buttonId, dtoObject.getButtonName(), dtoObject.getColor(), dtoObject.getButtonDescription(),
                    dtoObject.getActive(), dtoObject.getEntityTypeId());

            APIResponse response = WorkflowButtonEdit.getUpdateResponse(WorkflowButtonEdit.getApiPath(), WorkflowButtonEdit.getHeaders(), payload);

            String responseBody = response.getResponseBody();
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseBody), "Edit API Response is an Invalid JSON.");

            int actualResponseCode = response.getResponseCode();

            csAssert.assertTrue(expectedResponseCode == actualResponseCode, "Expected Response Code: " + expectedResponseCode +
                    " and Actual Response Code: " + actualResponseCode);

            List<String> buttonDataAfterEditAPICall = WorkflowButtonsDbHelper.getButtonDataFromDB(buttonId);

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    //Validate Response status.
                    String status = ParseJsonResponse.getStatusFromResponse(responseBody);
                    csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Response Status: Success and Actual Response Status: " + status);

                    //Validate Button Name
                    String buttonNameInDB = buttonDataAfterEditAPICall.get(1);
                    matchFieldValue(buttonNameInDB, dtoObject.getButtonName(), "Button Name", csAssert);

                    //Validate Active
                    String activeValueInDb = buttonDataAfterEditAPICall.get(2);
                    String expectedActiveValue = (dtoObject.getActive() != null && dtoObject.getActive()) ? "true" : "false";
                    expectedActiveValue = expectedActiveValue.equalsIgnoreCase("true") ? "t" : "f";

                    matchFieldValue(activeValueInDb, expectedActiveValue, "Active Value", csAssert);

                    //Validate Color
                    String colorValueInDb = buttonDataAfterEditAPICall.get(3);
                    matchFieldValue(colorValueInDb, dtoObject.getColor(), "Color Value", csAssert);

                    //Validate Description
                    String descriptionValueInDb = buttonDataAfterEditAPICall.get(4);
                    matchFieldValue(descriptionValueInDb, dtoObject.getButtonDescription(), "Description Value", csAssert);

                    //Validate Audit log data.
                    validateAuditLogData(lastAuditLogId, buttonId, csAssert);

                    //Revert data as before.
                    revertDataInDB(buttonDataBeforeEditAPICall, buttonId);
                }
            } else {
                //Validate Validation Error in Response.
                String status = ParseJsonResponse.getStatusFromResponse(responseBody);

                if (status.equalsIgnoreCase("validationError")) {
                    JSONArray genericErrorsArr = new JSONObject(responseBody).getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors");

                    boolean errorMessageFound = false;
                    for (int i = 0; i < genericErrorsArr.length(); i++) {
                        String message = genericErrorsArr.getJSONObject(i).getString("message").toLowerCase();

                        if (message.contains(expectedErrorMessage.toLowerCase())) {
                            errorMessageFound = true;
                            break;
                        }
                    }

                    csAssert.assertTrue(errorMessageFound, "Expected Error Message: [" + expectedErrorMessage + "] not found in API Response.");

                    //Validate Button Name is unchanged.
                    matchFieldValue(buttonDataAfterEditAPICall.get(1), buttonDataBeforeEditAPICall.get(1), "Button Name", csAssert);

                    //Validate Active Field is unchanged.
                    matchFieldValue(buttonDataAfterEditAPICall.get(3), buttonDataBeforeEditAPICall.get(3), "Active Value", csAssert);

                    //Validate Color Field is unchanged.
                    matchFieldValue(buttonDataAfterEditAPICall.get(2), buttonDataBeforeEditAPICall.get(2), "Color Value", csAssert);

                    //Validate Description is unchanged.
                    matchFieldValue(buttonDataAfterEditAPICall.get(4), buttonDataBeforeEditAPICall.get(4), "Description Value", csAssert);
                } else {
                    csAssert.assertTrue(false, "Expected Status: ValidationError and Actual Status: " + status);

                    //Revert data back.
                    revertDataInDB(buttonDataBeforeEditAPICall, buttonId);
                }

                //Validate no new entry created in Audit Log Table.
                int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 328);

                csAssert.assertTrue(lastAuditLogId == latestAuditLogId, "New Audit Log Entry Created in DB.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void matchFieldValue(String actualValue, String expectedValue, String fieldInfo, CustomAssert csAssert) {
        csAssert.assertTrue(actualValue.equalsIgnoreCase(expectedValue), fieldInfo + " passed in API Payload: " +
                expectedValue + " and " + fieldInfo + " in DB: " + actualValue);
    }

    private void revertDataInDB(List<String> buttonData, int buttonId) {
        String payload = WorkflowButtonEdit.getPayload(buttonId, buttonData.get(1), buttonData.get(3),
                buttonData.get(4), Boolean.parseBoolean(buttonData.get(2)), 328);
        WorkflowButtonEdit.getUpdateResponse(WorkflowButtonEdit.getApiPath(), WorkflowButtonEdit.getHeaders(), payload);
    }

    private void validateAuditLogData(int lastAuditLogId, int buttonId, CustomAssert csAssert) {
        try {
            int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 328);

            //Validate new entry in Audit Log Table.
            if (latestAuditLogId > lastAuditLogId) {
                String selectedColumns = "entity_type_id, entity_id, action_id, requested_by, completed_by, client_id";
                List<String> auditLogData = AuditLogsDbHelper.getOneAuditLogDataFromId(selectedColumns, latestAuditLogId);

                if (auditLogData.isEmpty()) {
                    throw new SkipException("Couldn't get Audit Log Data for Log Id " + latestAuditLogId);
                }

                //Validate EntityTypeId
                String entityTypeIdInDb = auditLogData.get(0);
                csAssert.assertTrue(entityTypeIdInDb.equalsIgnoreCase("328"), "Expected EntityTypeId in Audit Log: 328 and Actual EntityTypeId: " +
                        entityTypeIdInDb);

                //Validate EntityId
                String entityIdInDb = auditLogData.get(1);
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(buttonId)), "Expected EntityId in Audit Log: " +
                        buttonId + " and Actual EntityId: " + entityIdInDb);

                //Validate ActionId
                String actionIdInDb = auditLogData.get(2);
                csAssert.assertTrue(actionIdInDb.equalsIgnoreCase("2"), "Expected ActionId in Audit Log: 2 and Actual ActionId: " + actionIdInDb);

                //Validate RequestedBy
                String requestedByIdInDb = auditLogData.get(3);
                int expectedRequestedById = AppUserDbHelper.getUserIdFromLoginIdAndClientId(ConfigureEnvironment.getClientAdminUser(), clientId);

                csAssert.assertTrue(requestedByIdInDb.contains(String.valueOf(expectedRequestedById)), "Expected RequestedBy Id in Audit Log: " +
                        expectedRequestedById + " and Actual RequestedBy Id: " + requestedByIdInDb);

                //Validate CompletedBy
                String completedByIdInDb = auditLogData.get(4);
                int expectedCompletedById = AppUserDbHelper.getUserIdFromLoginIdAndClientId(ConfigureEnvironment.getClientAdminUser(), clientId);

                csAssert.assertTrue(completedByIdInDb.contains(String.valueOf(expectedCompletedById)), "Expected CompletedBy Id in Audit Log: " +
                        expectedCompletedById + " and Actual CompletedBy Id: " + completedByIdInDb);

                //Validate ClientId
                String clientIdInDb = auditLogData.get(5);
                csAssert.assertTrue(clientIdInDb.contains(String.valueOf(clientId)), "Expected ClientId in Audit Log: " + clientId +
                        " and Actual Client Id: " + clientIdInDb);
            } else {
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Button Id: " + buttonId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log Data. " + e.getMessage());
        }
    }
}