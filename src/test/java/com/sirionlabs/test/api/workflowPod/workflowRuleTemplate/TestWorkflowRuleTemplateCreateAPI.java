package com.sirionlabs.test.api.workflowPod.workflowRuleTemplate;

import com.sirionlabs.api.workflowRuleTemplate.WorkflowRuleTemplateCreate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.workflowRuleTemplate.WorkflowRuleTemplateCreateDTO;
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
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestWorkflowRuleTemplateCreateAPI {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRuleTemplateCreateAPI.class);

    private String testingType;
    private List<Integer> newlyCreatedTemplateIds = new ArrayList<>();

    private int clientId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;

        clientId = new AdminHelper().getClientId();
    }

    @AfterClass
    public void afterClass() {
        //Delete all newly created Rule Templates in DB.
        for (Integer ruleTemplateId : newlyCreatedTemplateIds) {
            WorkflowRuleTemplateDbHelper.deleteRuleTemplateDataInDb(ruleTemplateId);
        }
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/WorkflowRuleTemplate";
        String dataFileName = "workflowRuleTemplateCreateAPIData.json";

        List<WorkflowRuleTemplateCreateDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WorkflowRuleTemplateCreateDTO dtoObject = getCreateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WorkflowRuleTemplateCreateDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WorkflowRuleTemplateCreateDTO getCreateDTOObjectFromJson(JSONObject jsonObj) {
        WorkflowRuleTemplateCreateDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");

            String workflowRuleTemplateName = jsonObj.isNull("workflowRuleTemplateName") ? null : jsonObj.getString("workflowRuleTemplateName");
            String entityName = jsonObj.getString("entityName");
            Boolean active = jsonObj.getBoolean("active");
            String workflowRuleTemplateJson = jsonObj.isNull("workflowRuleTemplateJson") ? null : jsonObj.getString("workflowRuleTemplateJson");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String expectedErrorMessage = (jsonObj.has("expectedErrorMessage") && !jsonObj.isNull("expectedErrorMessage")) ?
                    jsonObj.getString("expectedErrorMessage") : null;

            dtoObject = new WorkflowRuleTemplateCreateDTO(testCaseId, description, workflowRuleTemplateName, entityName, active,
                    workflowRuleTemplateJson, expectedStatusCode, expectedErrorMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting WorkflowRuleTemplateCreate DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testWorkflowRuleTemplateCreateAPI(WorkflowRuleTemplateCreateDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();

        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            int lastRuleTemplateIdInDb = WorkflowRuleTemplateDbHelper.getLatestRuleTemplateIdFromDB();

            if (lastRuleTemplateIdInDb == -1) {
                throw new SkipException("Couldn't get Last Workflow Rule Template Id from DB.");
            }

            int lastAuditLogId = AuditLogsDbHelper.getLatestAuditLogIdForClientAndEntityTypeIdFromDb(clientId, 331);

            int expectedResponseCode = dtoObject.getExpectedStatusCode();
            String expectedErrorMessage = dtoObject.getExpectedErrorMessage();

            String payload = WorkflowRuleTemplateCreate.getPayload(dtoObject.getWorkflowRuleTemplateName(), dtoObject.getWorkflowRuleTemplateJson(),
                    dtoObject.getActive(), dtoObject.getEntityName());

            APIResponse response = WorkflowRuleTemplateCreate.getCreateResponse(payload);

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
                        //Validate Rule Template Id
                        int newlyCreatedRuleTemplateIdInResponse = jsonObj.getInt("entityId");

                        //Validate API Response and Data in DB.
                        validateCreateAPIResponseAndData(lastRuleTemplateIdInDb, newlyCreatedRuleTemplateIdInResponse, dtoObject, csAssert);

                        //Validate Audit Log
                        validateAuditLogData(lastAuditLogId, newlyCreatedRuleTemplateIdInResponse, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Create API Response doesn't contain valid Id of newly created workflow rule template.");
                    }
                }
            } else {
                int latestRuleTemplateIdInDb = WorkflowRuleTemplateDbHelper.getLatestRuleTemplateIdFromDB();

                //Validate that new entry is not created in DB.
                if (lastRuleTemplateIdInDb != latestRuleTemplateIdInDb) {
                    csAssert.assertTrue(false, "New Entry created in DB.");
                    newlyCreatedTemplateIds.add(latestRuleTemplateIdInDb);
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

    private void validateCreateAPIResponseAndData(int lastRuleTemplateId, int newlyCreatedRuleTemplateIdInResponse, WorkflowRuleTemplateCreateDTO dtoObject,
                                                  CustomAssert csAssert) {
        try {
            String selectedColumns = "id, rule_template_name, active, entity_type_id, rule_template_json, date_modified";
            List<String> latestRuleTemplateData = WorkflowRuleTemplateDbHelper.getLatestRuleTemplateDataFromDB(selectedColumns);

            if (latestRuleTemplateData == null || latestRuleTemplateData.isEmpty()) {
                throw new SkipException("Couldn't get Data of Workflow Rule Template from DB.");
            }

            //Validate Rule Template Id
            int newlyCreatedRuleTemplateIdInDB = Integer.parseInt(latestRuleTemplateData.get(0));

            //Validate that new Id is generated in DB.
            if (newlyCreatedRuleTemplateIdInDB == lastRuleTemplateId) {
                csAssert.assertTrue(false, "New entry not created in DB.");
            } else {
                newlyCreatedTemplateIds.add(newlyCreatedRuleTemplateIdInDB);
            }

            csAssert.assertTrue(newlyCreatedRuleTemplateIdInDB == newlyCreatedRuleTemplateIdInResponse, "Rule Template Id in DB: " +
                    newlyCreatedRuleTemplateIdInDB + " and Rule Template Id in API Response: " + newlyCreatedRuleTemplateIdInResponse);

            //Validate Rule Template Name
            String ruleTemplateNameInDB = latestRuleTemplateData.get(1).trim();
            csAssert.assertTrue(ruleTemplateNameInDB.equalsIgnoreCase(dtoObject.getWorkflowRuleTemplateName()), "Rule Template Name passed in API Payload: " +
                    dtoObject.getWorkflowRuleTemplateName() + " and Rule Template Name in DB: " + ruleTemplateNameInDB);

            //Validate Active Value
            boolean activeValueInDB = latestRuleTemplateData.get(2).trim().equalsIgnoreCase("t");
            csAssert.assertTrue(dtoObject.getActive().equals(activeValueInDB), "Active Value passed in API Payload: " + dtoObject.getActive() +
                    " and Active Value in DB: " + activeValueInDB);

            //Validate Entity Type
            int expectedEntityTypeId = ConfigureConstantFields.getEntityIdByName(dtoObject.getEntityName());
            String entityTypeIdValueInDB = latestRuleTemplateData.get(3);
            csAssert.assertTrue(entityTypeIdValueInDB.equalsIgnoreCase(String.valueOf(expectedEntityTypeId)), "EntityTypeId Passed in API Payload: " +
                    expectedEntityTypeId + " and EntityTypeId in DB: " + entityTypeIdValueInDB);

            String ruleJsonInDB = latestRuleTemplateData.get(4);
            csAssert.assertTrue(ruleJsonInDB.equalsIgnoreCase(dtoObject.getWorkflowRuleTemplateJson()), "Rule Json Passed in API Payload: [" +
                    dtoObject.getWorkflowRuleTemplateJson() + "] and Json Value in DB: [" + ruleJsonInDB + "]");

            //Validate Date Modified field
            String modifiedDateInDB = latestRuleTemplateData.get(5);
            csAssert.assertTrue(modifiedDateInDB == null, "Modified Date is not null in DB.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Create API Response and Data. " + e.getMessage());
        }
    }

    private void validateAuditLogData(int lastAuditLogId, int newlyCreatedRuleTemplateId, CustomAssert csAssert) {
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
                csAssert.assertTrue(entityIdInDb.equalsIgnoreCase(String.valueOf(newlyCreatedRuleTemplateId)), "Expected EntityId in Audit Log: " +
                        newlyCreatedRuleTemplateId + " and Actual EntityId: " + entityIdInDb);

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
                csAssert.assertTrue(false, "New Entry not created in Audit Log for Workflow Rule Template Id: " + newlyCreatedRuleTemplateId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log Data. " + e.getMessage());
        }
    }
}