package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.api.workflowLayout.WorkflowLayoutEditPost;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowLayout.WorkflowLayoutEditPostDTO;
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

public class TestWorkflowLayoutEditPostAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutEditPostAPI.class);

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

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowLayout";
        String dataFileName = "workflowLayoutEditPostAPIData.json";

        List<WorkflowLayoutEditPostDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowLayoutEditPostDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowLayoutEditPostDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowLayoutEditPostDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowLayoutEditPostDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int layoutId = jsonObj.getInt("workflowLayoutId");
            String workflowLayoutGroupName = jsonObj.isNull("workflowLayoutGroupName") ? null : jsonObj.getString("workflowLayoutGroupName");
            String editableFieldsShowPage = jsonObj.isNull("editableFieldsShowPage") ? null : jsonObj.getString("editableFieldsShowPage");
            String editableFieldsEditPage = jsonObj.isNull("editableFieldsEditPage") ? null : jsonObj.getString("editableFieldsEditPage");
            String editPageTabs = jsonObj.isNull("editPageTabs") ? null : jsonObj.getString("editPageTabs");
            String showPageTabs = jsonObj.isNull("showPageTabs") ? null : jsonObj.getString("showPageTabs");
            int entityTypeId = jsonObj.getInt("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowLayoutEditPostDTO(testCaseId, description, layoutId, workflowLayoutGroupName, editableFieldsShowPage, editableFieldsEditPage,
                    editPageTabs, showPageTabs, entityTypeId, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowLayoutEditPost DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowLayoutEditPostAPI(WorkflowLayoutEditPostDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String selectedColumns = "id, name, edit_page_tabs, show_page_tabs, editable_fields_show_page, editable_fields_edit_page, entity_type_id";
            List<String> layoutDataBeforeEditCall = WorkflowLayoutDbHelper.getLayoutDataFromDB(selectedColumns, dtoObject.getWorkflowLayoutId());

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 329);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String payload = WorkflowLayoutEditPost.getPayload(dtoObject.getWorkflowLayoutId(), dtoObject.getWorkflowLayoutGroupName(),
                    dtoObject.getEditableFieldsShowPage(), dtoObject.getEditableFieldsEditPage(), dtoObject.getEditPageTabs(), dtoObject.getShowPageTabs(),
                    dtoObject.getEntityTypeId());

            APIResponse response = WorkflowLayoutEditPost.getEditPostResponse(WorkflowLayoutEditPost.getApiPath(), WorkflowLayoutEditPost.getHeaders(), payload);

            String responseBody = response.getResponseBody();
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseBody), "Edit Post API Response is an Invalid JSON.");

            int actualResponseCode = response.getResponseCode();

            csAssert.assertTrue(expectedResponseCode == actualResponseCode, "Expected Response Code: " + expectedResponseCode +
                    " and Actual Response Code: " + actualResponseCode);

            List<String> layoutDataAfterEditCall = WorkflowLayoutDbHelper.getLayoutDataFromDB(selectedColumns, dtoObject.getWorkflowLayoutId());

            if (layoutDataAfterEditCall == null || layoutDataAfterEditCall.isEmpty()) {
                throw new SkipException("Couldn't get Data of Workflow Layout from DB.");
            }

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    //Validate Layout Data
                    validateLayoutData(layoutDataAfterEditCall, dtoObject, csAssert);

                    //Validate Audit Log
                    validateAuditLogData(lastAuditLogId, dtoObject.getWorkflowLayoutId(), csAssert);

                    //Revert data in DB.
                    revertDataInDB(layoutDataBeforeEditCall, dtoObject.getWorkflowLayoutId());
                }
            } else {
                //Validate Error Message
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

                    //Validate Layout Name is unchanged.
                    csAssert.assertTrue(layoutDataAfterEditCall.get(1).equalsIgnoreCase(layoutDataBeforeEditCall.get(1)), "Layout name changed in DB.");

                    //Validate Edit Page Tabs Value is unchanged.
                    csAssert.assertTrue(layoutDataAfterEditCall.get(2).equalsIgnoreCase(layoutDataBeforeEditCall.get(2)), "Edit Page Tabs Value changed in DB.");

                    //Validate Show Page Tabs Value is unchanged.
                    csAssert.assertTrue(layoutDataAfterEditCall.get(3).equalsIgnoreCase(layoutDataBeforeEditCall.get(3)), "Show Page Tabs Value changed in DB.");

                    //Validate Editable Fields Show Page Value is unchanged.
                    csAssert.assertTrue(layoutDataAfterEditCall.get(4).equalsIgnoreCase(layoutDataBeforeEditCall.get(4)),
                            "Editable Fields Show Page Value changed in DB.");

                    //Validate Editable Fields Edit Page Value is unchanged.
                    csAssert.assertTrue(layoutDataAfterEditCall.get(5).equalsIgnoreCase(layoutDataBeforeEditCall.get(5)),
                            "Editable Fields Edit Page Value changed in DB.");
                } else {
                    csAssert.assertTrue(false, "Expected Status: ValidationError and Actual Status: " + status);

                    //Revert data back.
                    revertDataInDB(layoutDataBeforeEditCall, dtoObject.getWorkflowLayoutId());
                }

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

    private void validateLayoutData(List<String> layoutDataInDb, WorkflowLayoutEditPostDTO dtoObject, CustomAssert csAssert) {
        try {
            //Validate Layout Name
            String layoutNameInDB = layoutDataInDb.get(1).trim();
            csAssert.assertTrue(layoutNameInDB.equalsIgnoreCase(dtoObject.getWorkflowLayoutGroupName()), "Layout Name passed in API Payload: " +
                    dtoObject.getWorkflowLayoutGroupName() + " and Layout Name in DB: " + layoutNameInDB);

            //Validate Edit Page Tabs
            validateTabsValue(layoutDataInDb.get(2), dtoObject.getEditPageTabs(), "Edit Page", csAssert);

            //Validate Show Page Tabs
            validateTabsValue(layoutDataInDb.get(3), dtoObject.getShowPageTabs(), "Show Page", csAssert);

            //Validate Editable Fields Show Page
            validateTabsValue(layoutDataInDb.get(4), dtoObject.getEditableFieldsShowPage(), "Editable Fields Show Page", csAssert);

            //Validate Editable Fields Edit Page
            validateTabsValue(layoutDataInDb.get(5), dtoObject.getEditableFieldsEditPage(), "Editable Fields Edit Page", csAssert);

            //Validate EntityTypeId
            String entityTypeIdValueInDb = layoutDataInDb.get(6);
            csAssert.assertTrue(entityTypeIdValueInDb.equalsIgnoreCase(String.valueOf(dtoObject.getEntityTypeId())),
                    "EntityTypeId Value passed in API Payload: " + dtoObject.getEntityTypeId() + " and Actual Value in DB: " + entityTypeIdValueInDb);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit Post API Response and Data. " + e.getMessage());
        }
    }

    private void validateAuditLogData(int lastAuditLogId, int layoutId, CustomAssert csAssert) {
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
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(layoutId)), "Expected EntityId in Audit Log: " +
                        layoutId + " and Actual EntityId: " + entityIdInDb);

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
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Layout Id: " + layoutId);
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

    private void revertDataInDB(List<String> layoutData, int layoutId) {
        String payload = WorkflowLayoutEditPost.getPayload(layoutId, layoutData.get(1),
                layoutData.get(4).replace("{", "").replace("}", ""),
                layoutData.get(5).replace("{", "").replace("}", ""),
                layoutData.get(2).replace("{", "").replace("}", ""),
                layoutData.get(3).replace("{", "").replace("}", ""), Integer.parseInt(layoutData.get(6)));

        WorkflowLayoutEditPost.getEditPostResponse(WorkflowLayoutEditPost.getApiPath(), WorkflowLayoutEditPost.getHeaders(), payload);
    }
}