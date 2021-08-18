package com.sirionlabs.test.api.workflowPod.workflowButtons;

import com.sirionlabs.api.workflowButtons.WorkflowButtonCreate;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowButtons.WorkflowButtonsCreateDTO;
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
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWorkflowButtonsCreateAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowButtonsCreateAPI.class);

    private String testingType;
    private List<Integer> newlyCreatedButtonIds = new ArrayList<>();

    private int clientId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;

        clientId = new AdminHelper().getClientId();
    }

    @AfterClass
    public void afterClass() {
        //Delete all newly created Buttons in DB.
        for (Integer buttonId : newlyCreatedButtonIds) {
            WorkflowButtonsDbHelper.deleteButtonDataInDb(buttonId);
        }
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowButtons";
        String dataFileName = "workflowButtonsCreateAPIData.json";

        List<WorkflowButtonsCreateDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowButtonsCreateDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowButtonsCreateDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowButtonsCreateDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowButtonsCreateDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String buttonName = (jsonObj.has("buttonName") && !jsonObj.isNull("buttonName")) ? jsonObj.getString("buttonName") : null;
            String color = (jsonObj.has("color") && !jsonObj.isNull("color")) ? jsonObj.getString("color") : null;
            String buttonDescription = jsonObj.getString("buttonDescription");
            Boolean active = jsonObj.getBoolean("active");
            int entityTypeId = jsonObj.getInt("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowButtonsCreateDTO(testCaseId, description, buttonName, color, buttonDescription, active, entityTypeId, expectedStatusCode,
                    expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowButtonsCreate DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowButtonsCreateAPI(WorkflowButtonsCreateDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            int lastButtonIdInDb = WorkflowButtonsDbHelper.getLatestButtonIdFromDB();

            if (lastButtonIdInDb == -1) {
                throw new SkipException("Couldn't get Last Workflow Button Id from DB.");
            }

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 328);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String payload = WorkflowButtonCreate.getPayload(dtoObject.getButtonName(), dtoObject.getColor(), dtoObject.getButtonDescription(), dtoObject.getActive(),
                    dtoObject.getEntityTypeId());

            APIResponse response = WorkflowButtonCreate.getCreateResponse(WorkflowButtonCreate.getApiPath(), WorkflowButtonCreate.getHeaders(), payload);

            String responseBody = response.getResponseBody();
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseBody), "Create API Response is an Invalid JSON.");

            int actualResponseCode = response.getResponseCode();

            csAssert.assertTrue(expectedResponseCode == actualResponseCode, "Expected Response Code: " + expectedResponseCode +
                    " and Actual Response Code: " + actualResponseCode);

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    //Validate Response Body.
                    JSONObject jsonObj = new JSONObject(responseBody).getJSONObject("header").getJSONObject("response");

                    if (jsonObj.has("entityId") && !jsonObj.isNull("entityId")) {
                        //Validate Button Id
                        int newlyCreatedButtonIdInResponse = jsonObj.getInt("entityId");

                        //Validate API Response and Data in DB.
                        validateCreateAPIResponseAndData(lastButtonIdInDb, newlyCreatedButtonIdInResponse, dtoObject, csAssert);

                        //Validate Audit Log
                        validateAuditLogData(lastAuditLogId, newlyCreatedButtonIdInResponse, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Create API Response doesn't contain valid Id of newly created workflow button.");
                    }
                }
            } else {
                int latestButtonIdInDb = WorkflowButtonsDbHelper.getLatestButtonIdFromDB();

                //Validate that new entry is not created in DB.
                if (lastButtonIdInDb != latestButtonIdInDb) {
                    csAssert.assertTrue(false, "New Entry created in DB.");
                    newlyCreatedButtonIds.add(latestButtonIdInDb);
                }

                //Validate Error Message
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

                //Validate no new entry created in Audit Log.
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

    private void validateCreateAPIResponseAndData(int lastButtonId, int newlyCreatedButtonIdInResponse, WorkflowButtonsCreateDTO dtoObject, CustomAssert csAssert) {
        try {
            List<String> latestButtonData = WorkflowButtonsDbHelper.getLatestButtonCreateDataFromDB();

            if (latestButtonData == null || latestButtonData.isEmpty()) {
                throw new SkipException("Couldn't get Data of Workflow Button from DB.");
            }

            //Validate Button Id
            int newlyCreatedButtonIdInDB = Integer.parseInt(latestButtonData.get(0));

            //Validate that new Id is generated in DB.
            if (newlyCreatedButtonIdInDB == lastButtonId) {
                csAssert.assertTrue(false, "New entry not created in DB.");
            } else {
                newlyCreatedButtonIds.add(newlyCreatedButtonIdInDB);
            }

            csAssert.assertTrue(newlyCreatedButtonIdInDB == newlyCreatedButtonIdInResponse, "Button Id in DB: " +
                    newlyCreatedButtonIdInDB + " and Button Id in API Response: " + newlyCreatedButtonIdInResponse);

            //Validate Button Name
            String buttonNameInDB = latestButtonData.get(1);
            csAssert.assertTrue(buttonNameInDB.equalsIgnoreCase(dtoObject.getButtonName()), "Button Name passed in API Payload: " +
                    dtoObject.getButtonName() + " and Button Name in DB: " + buttonNameInDB);

            //Validate Active
            String activeValueInDb = latestButtonData.get(2);
            String expectedActiveValue = (dtoObject.getActive() != null && dtoObject.getActive()) ? "true" : "false";
            expectedActiveValue = expectedActiveValue.equalsIgnoreCase("true") ? "t" : "f";

            csAssert.assertTrue(activeValueInDb.equalsIgnoreCase(expectedActiveValue), "Active Value passed in API Payload: " +
                    expectedActiveValue + " and Active Value in DB: " + activeValueInDb);

            //Validate Color
            String colorValueInDb = latestButtonData.get(3);
            csAssert.assertTrue(dtoObject.getColor().equalsIgnoreCase(colorValueInDb), "Color Value passed in API Payload: " +
                    dtoObject.getColor() + " and Color Value in DB: " + colorValueInDb);

            //Validate Description
            String descriptionValueInDb = latestButtonData.get(4);
            csAssert.assertTrue(dtoObject.getButtonDescription().equalsIgnoreCase(descriptionValueInDb), "Description Value passed in API Payload: " +
                    dtoObject.getButtonDescription() + " and Description Value in DB: " + descriptionValueInDb);

            //Validate Date Created field
                       /* String createdDateInDB = latestButtonData.get(7);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                        String dateInDBInRequiredFormat = dateFormat.parse(createdDateInDB).toString();
                        Date currentDate = new Date();
                        String currentDateInRequiredFormat = dateFormat.format(currentDate);*/

            //Validate Date Modified field
            String modifiedDateInDB = latestButtonData.get(8);
            csAssert.assertTrue(modifiedDateInDB == null, "Modified Date is not null in DB.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Create API Response and Data. " + e.getMessage());
        }
    }

    private void validateAuditLogData(int lastAuditLogId, int newlyCreatedButtonId, CustomAssert csAssert) {
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
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(newlyCreatedButtonId)), "Expected EntityId in Audit Log: " +
                        newlyCreatedButtonId + " and Actual EntityId: " + entityIdInDb);

                //Validate ActionId
                String actionIdInDb = auditLogData.get(2);
                csAssert.assertTrue(actionIdInDb.equalsIgnoreCase("1"), "Expected ActionId in Audit Log: 1 and Actual ActionId: " + actionIdInDb);

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
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Button Id: " + newlyCreatedButtonId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log Data. " + e.getMessage());
        }
    }
}