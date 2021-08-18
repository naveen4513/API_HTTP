package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.api.workflowLayout.WorkflowLayoutCreate;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowLayout.WorkflowLayoutCreateDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.AuditLogsDbHelper;
import com.sirionlabs.helper.dbHelper.WorkflowLayoutDbHelper;
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

public class TestWorkflowLayoutCreateAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutCreateAPI.class);

    private String testingType;
    private List<Integer> newlyCreatedLayoutIds = new ArrayList<>();

    private int clientId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;

        clientId = new AdminHelper().getClientId();
    }

    @AfterClass
    public void afterClass() {
        //Delete all newly created Layouts in DB.
        for (Integer layoutId : newlyCreatedLayoutIds) {
            WorkflowLayoutDbHelper.deleteLayoutDataInDb(layoutId);
        }
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowLayout";
        String dataFileName = "workflowLayoutCreateAPIData.json";

        List<WorkflowLayoutCreateDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowLayoutCreateDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowLayoutCreateDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowLayoutCreateDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowLayoutCreateDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String workflowLayoutGroupName = jsonObj.isNull("workflowLayoutGroupName") ? null : jsonObj.getString("workflowLayoutGroupName");
            String editableFieldsShowPage = jsonObj.isNull("editableFieldsShowPage") ? null : jsonObj.getString("editableFieldsShowPage");
            String editableFieldsEditPage = jsonObj.isNull("editableFieldsEditPage") ? null : jsonObj.getString("editableFieldsEditPage");
            String editPageTabs = jsonObj.isNull("editPageTabs") ? null : jsonObj.getString("editPageTabs");
            String showPageTabs = jsonObj.isNull("showPageTabs") ? null : jsonObj.getString("showPageTabs");
            int entityTypeId = jsonObj.getInt("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowLayoutCreateDTO(testCaseId, description, workflowLayoutGroupName, editableFieldsShowPage, editableFieldsEditPage,
                    editPageTabs, showPageTabs, entityTypeId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowLayoutCreate DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowLayoutCreateAPI(WorkflowLayoutCreateDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            int lastLayoutIdInDb = WorkflowLayoutDbHelper.getLatestLayoutIdFromDB();

            if (lastLayoutIdInDb == -1) {
                throw new SkipException("Couldn't get Last Workflow Layout Id from DB.");
            }

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 329);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String payload = WorkflowLayoutCreate.getPayload(dtoObject.getWorkflowLayoutGroupName(), dtoObject.getEditableFieldsShowPage(),
                    dtoObject.getEditableFieldsEditPage(), dtoObject.getEditPageTabs(), dtoObject.getShowPageTabs(), dtoObject.getEntityTypeId());

            APIResponse response = WorkflowLayoutCreate.getCreateResponse(WorkflowLayoutCreate.getApiPath(), WorkflowLayoutCreate.getHeaders(), payload);

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
                        //Validate Layout Id
                        int newlyCreatedLayoutIdInResponse = jsonObj.getInt("entityId");

                        //Validate API Response and Data in DB.
                        validateCreateAPIResponseAndData(lastLayoutIdInDb, newlyCreatedLayoutIdInResponse, dtoObject, csAssert);

                        //Validate Audit Log
                        validateAuditLogData(lastAuditLogId, newlyCreatedLayoutIdInResponse, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Create API Response doesn't contain valid Id of newly created workflow layout.");
                    }
                }
            } else {
                int latestLayoutIdInDb = WorkflowLayoutDbHelper.getLatestLayoutIdFromDB();

                //Validate that new entry is not created in DB.
                if (lastLayoutIdInDb != latestLayoutIdInDb) {
                    csAssert.assertTrue(false, "New Entry created in DB.");
                    newlyCreatedLayoutIds.add(latestLayoutIdInDb);
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
                int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 329);

                csAssert.assertTrue(lastAuditLogId == latestAuditLogId, "New Audit Log Entry Created in DB.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateCreateAPIResponseAndData(int lastLayoutId, int newlyCreatedLayoutIdInResponse, WorkflowLayoutCreateDTO dtoObject, CustomAssert csAssert) {
        try {
            String selectedColumns = "id, name, edit_page_tabs, show_page_tabs, editable_fields_show_page, editable_fields_edit_page, entity_type_id, date_modified";
            List<String> latestLayoutData = WorkflowLayoutDbHelper.getLatestLayoutDataFromDB(selectedColumns);

            if (latestLayoutData == null || latestLayoutData.isEmpty()) {
                throw new SkipException("Couldn't get Data of Workflow Layout from DB.");
            }

            //Validate Layout Id
            int newlyCreatedLayoutIdInDB = Integer.parseInt(latestLayoutData.get(0));

            //Validate that new Id is generated in DB.
            if (newlyCreatedLayoutIdInDB == lastLayoutId) {
                csAssert.assertTrue(false, "New entry not created in DB.");
            } else {
                newlyCreatedLayoutIds.add(newlyCreatedLayoutIdInDB);
            }

            csAssert.assertTrue(newlyCreatedLayoutIdInDB == newlyCreatedLayoutIdInResponse, "Layout Id in DB: " +
                    newlyCreatedLayoutIdInDB + " and Layout Id in API Response: " + newlyCreatedLayoutIdInResponse);

            //Validate Layout Name
            String layoutNameInDB = latestLayoutData.get(1).trim();
            csAssert.assertTrue(layoutNameInDB.equalsIgnoreCase(dtoObject.getWorkflowLayoutGroupName()), "Layout Name passed in API Payload: " +
                    dtoObject.getWorkflowLayoutGroupName() + " and Layout Name in DB: " + layoutNameInDB);

            //Validate Edit Page Tabs
            validateTabsValue(latestLayoutData.get(2), dtoObject.getEditPageTabs(), "Edit Page", csAssert);

            //Validate Show Page Tabs
            validateTabsValue(latestLayoutData.get(3), dtoObject.getShowPageTabs(), "Show Page", csAssert);

            //Validate Editable Fields Show Page
            validateTabsValue(latestLayoutData.get(4), dtoObject.getEditableFieldsShowPage(), "Editable Fields Show Page", csAssert);

            //Validate Editable Fields Edit Page
            validateTabsValue(latestLayoutData.get(5), dtoObject.getEditableFieldsEditPage(), "Editable Fields Edit Page", csAssert);

            //Validate EntityTypeId
            String entityTypeIdValueInDb = latestLayoutData.get(6);
            csAssert.assertTrue(entityTypeIdValueInDb.equalsIgnoreCase(String.valueOf(dtoObject.getEntityTypeId())),
                    "EntityTypeId Value passed in API Payload: " + dtoObject.getEntityTypeId() + " and Actual Value in DB: " + entityTypeIdValueInDb);

            //Validate Date Modified field
            String modifiedDateInDB = latestLayoutData.get(7);
            csAssert.assertTrue(modifiedDateInDB == null, "Modified Date is not null in DB.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Create API Response and Data. " + e.getMessage());
        }
    }

    private void validateAuditLogData(int lastAuditLogId, int newlyCreatedLayoutId, CustomAssert csAssert) {
        try {
            int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 329);

            //Validate new entry in Audit Log Table.
            if (latestAuditLogId > lastAuditLogId) {
                String selectedColumns = "entity_type_id, entity_id, action_id, requested_by, completed_by, client_id";
                List<String> auditLogData = AuditLogsDbHelper.getOneAuditLogDataFromId(selectedColumns, latestAuditLogId);

                if (auditLogData.isEmpty()) {
                    throw new SkipException("Couldn't get Audit Log Data for Log Id " + latestAuditLogId);
                }

                //Validate EntityTypeId
                String entityTypeIdInDb = auditLogData.get(0);
                csAssert.assertTrue(entityTypeIdInDb.equalsIgnoreCase("329"), "Expected EntityTypeId in Audit Log: 329 and Actual EntityTypeId: " +
                        entityTypeIdInDb);

                //Validate EntityId
                String entityIdInDb = auditLogData.get(1);
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(newlyCreatedLayoutId)), "Expected EntityId in Audit Log: " +
                        newlyCreatedLayoutId + " and Actual EntityId: " + entityIdInDb);

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
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Layout Id: " + newlyCreatedLayoutId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log Data. " + e.getMessage());
        }
    }

    private void validateTabsValue(String tabValueInDb, String expectedTabValue, String fieldInfo, CustomAssert csAssert) {
        try {
            if (expectedTabValue == null) {
                csAssert.assertTrue(tabValueInDb.equalsIgnoreCase("{}"), "Expected " + fieldInfo + " Value: {} in DB and Actual Value: " +
                        tabValueInDb);
            } else {
                String[] expectedValuesArr = expectedTabValue.split(",");
                logger.info("{} Tabs Value in DB: {}", fieldInfo, tabValueInDb);

                for (String expectedValue : expectedValuesArr) {
                    csAssert.assertTrue(tabValueInDb.contains(expectedValue.trim()), fieldInfo + " Tabs Value passed in API Payload: " +
                            expectedValue + " is not present in DB");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Tabs Value of " + fieldInfo + ". " + e.getMessage());
        }
    }
}