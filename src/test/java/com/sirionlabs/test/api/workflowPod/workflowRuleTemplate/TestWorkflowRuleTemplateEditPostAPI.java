package com.sirionlabs.test.api.workflowPod.workflowRuleTemplate;

import com.sirionlabs.api.workflowRuleTemplate.WorkflowRuleTemplateEditPost;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowRuleTemplate.WorkflowRuleTemplateEditPostDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.AuditLogsDbHelper;
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

public class TestWorkflowRuleTemplateEditPostAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRuleTemplateEditPostAPI.class);

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

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowRuleTemplate";
        String dataFileName = "workflowRuleTemplateEditPostAPIData.json";

        List<WorkflowRuleTemplateEditPostDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowRuleTemplateEditPostDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowRuleTemplateEditPostDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowRuleTemplateEditPostDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowRuleTemplateEditPostDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            int workflowRuleTemplateId = jsonObj.getInt("workflowRuleTemplateId");
            String workflowRuleTemplateName = jsonObj.getString("workflowRuleTemplateName");
            String entityName = jsonObj.getString("entityName");
            boolean active = jsonObj.getBoolean("active");
            String workflowRuleTemplateJson = jsonObj.getString("workflowRuleTemplateJson");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowRuleTemplateEditPostDTO(testCaseId, description, workflowRuleTemplateId, workflowRuleTemplateName, entityName, active,
                    workflowRuleTemplateJson, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowRuleTemplateEditPost DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowRuleTemplateEditPostAPI(WorkflowRuleTemplateEditPostDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            String selectedColumns = "id, rule_template_name, active, entity_type_id, rule_template_json";
            List<String> ruleTemplateDataBeforeEditCall = WorkflowRuleTemplateDbHelper.getRuleTemplateDataFromDB(selectedColumns, dtoObject.getWorkflowRuleTemplateId());

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 331);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            int entityTypeId = ConfigureConstantFields.getEntityIdByName(dtoObject.getEntityName());

            String payload = WorkflowRuleTemplateEditPost.getPayload(dtoObject.getWorkflowRuleTemplateId(), dtoObject.getWorkflowRuleTemplateName(),
                    dtoObject.getWorkflowRuleTemplateJson(), dtoObject.getActive(), entityTypeId);

            APIResponse response = WorkflowRuleTemplateEditPost.getEditPostResponse(payload);

            String responseBody = response.getResponseBody();
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(responseBody), "Edit Post API Response is an Invalid JSON.");

            int actualResponseCode = response.getResponseCode();

            csAssert.assertTrue(expectedResponseCode == actualResponseCode, "Expected Response Code: " + expectedResponseCode +
                    " and Actual Response Code: " + actualResponseCode);

            List<String> ruleTemplateDataAfterEditCall = WorkflowRuleTemplateDbHelper.getRuleTemplateDataFromDB(selectedColumns, dtoObject.getWorkflowRuleTemplateId());

            if (ruleTemplateDataAfterEditCall == null || ruleTemplateDataAfterEditCall.isEmpty()) {
                throw new SkipException("Couldn't get Data of Workflow Rule Template from DB.");
            }

            if (expectedErrorMessage == null) {
                if (ParseJsonResponse.validJsonResponse(responseBody)) {
                    //Validate Rule Template Data
                    validateRuleTemplateData(ruleTemplateDataAfterEditCall, dtoObject, csAssert);

                    //Validate Audit Log
                    validateAuditLogData(lastAuditLogId, dtoObject.getWorkflowRuleTemplateId(), csAssert);

                    //Revert data in DB.
                    revertDataInDB(ruleTemplateDataBeforeEditCall, dtoObject.getWorkflowRuleTemplateId());
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

                    //Validate Rule Template Name is unchanged.
                    csAssert.assertTrue(ruleTemplateDataAfterEditCall.get(1).equalsIgnoreCase(ruleTemplateDataBeforeEditCall.get(1)),
                            "Rule Template name changed in DB.");

                    //Validate Active value is unchanged.
                    csAssert.assertTrue(ruleTemplateDataAfterEditCall.get(2).equalsIgnoreCase(ruleTemplateDataBeforeEditCall.get(2)),
                            "Active Value changed in DB.");

                    //Validate Rule Template Json is unchanged.
                    csAssert.assertTrue(ruleTemplateDataAfterEditCall.get(4).equalsIgnoreCase(ruleTemplateDataBeforeEditCall.get(4)),
                            "Rule Template Json Value changed in DB.");

                } else {
                    csAssert.assertTrue(false, "Expected Status: ValidationError and Actual Status: " + status);

                    //Revert data back.
                    revertDataInDB(ruleTemplateDataBeforeEditCall, dtoObject.getWorkflowRuleTemplateId());
                }

                //Validate no new entry created in Audit Log.
                int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 331);

                csAssert.assertTrue(lastAuditLogId == latestAuditLogId, "New Audit Log Entry Created in DB.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateRuleTemplateData(List<String> ruleTemplateDataInDb, WorkflowRuleTemplateEditPostDTO dtoObject, CustomAssert csAssert) {
        try {
            //Validate Rule Template Name
            String ruleTemplateNameInDB = ruleTemplateDataInDb.get(1).trim();
            csAssert.assertTrue(ruleTemplateNameInDB.equalsIgnoreCase(dtoObject.getWorkflowRuleTemplateName()), "Rule Template Name passed in API Payload: " +
                    dtoObject.getWorkflowRuleTemplateName() + " and Rule Template Name in DB: " + ruleTemplateNameInDB);

            //Validate Active Value
            boolean activeValueInDb = (ruleTemplateDataInDb.get(2).equalsIgnoreCase("t"));
            csAssert.assertTrue(dtoObject.getActive().equals(activeValueInDb), "Active Value passed in API Payload: " +
                    dtoObject.getActive() + " and Active Value in DB: " + activeValueInDb);

            //Validate Rule Template Json
            String ruleTemplateJsonValueInDb = ruleTemplateDataInDb.get(4);
            csAssert.assertTrue(ruleTemplateJsonValueInDb.equalsIgnoreCase(dtoObject.getWorkflowRuleTemplateJson()),
                    "Rule Template Json Value passed in API Payload: [" + dtoObject.getWorkflowRuleTemplateJson() +
                            "] and Value in DB: [" + ruleTemplateJsonValueInDb + "]");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit Post API Response and Data. " + e.getMessage());
        }
    }

    private void validateAuditLogData(int lastAuditLogId, int ruleTemplateId, CustomAssert csAssert) {
        try {
            int latestAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 331);

            //Validate new entry in Audit Log Table.
            if (latestAuditLogId > lastAuditLogId) {
                String selectedColumns = "entity_type_id, entity_id, action_id, requested_by, completed_by, client_id";
                List<String> auditLogData = AuditLogsDbHelper.getOneAuditLogDataFromId(selectedColumns, latestAuditLogId);

                if (auditLogData.isEmpty()) {
                    throw new SkipException("Couldn't get Audit Log Data for Log Id " + latestAuditLogId);
                }

                //Validate EntityTypeId
                String entityTypeIdInDb = auditLogData.get(0);
                csAssert.assertTrue(entityTypeIdInDb.equalsIgnoreCase("331"), "Expected EntityTypeId in Audit Log: 331 and Actual EntityTypeId: " +
                        entityTypeIdInDb);

                //Validate EntityId
                String entityIdInDb = auditLogData.get(1);
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(ruleTemplateId)), "Expected EntityId in Audit Log: " +
                        ruleTemplateId + " and Actual EntityId: " + entityIdInDb);

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
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Rule Template Id: " + ruleTemplateId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log Data. " + e.getMessage());
        }
    }

    private void revertDataInDB(List<String> ruleTemplateData, int ruleTemplateId) {
        String payload = WorkflowRuleTemplateEditPost.getPayload(ruleTemplateId, ruleTemplateData.get(1), ruleTemplateData.get(4),
                Boolean.parseBoolean(ruleTemplateData.get(2)), Integer.parseInt(ruleTemplateData.get(3)));

        WorkflowRuleTemplateEditPost.getEditPostResponse(payload);
    }
}