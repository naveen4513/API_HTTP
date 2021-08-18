package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.workflowRuleTemplate.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.WorkflowRuleTemplateDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;


public class TestWorkflowRuleTemplateEndToEnd {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRuleTemplateEndToEnd.class);

    @BeforeClass
    public void beforeClass() {
        new AdminHelper().loginWithClientAdminUser();
    }

    @AfterClass
    public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @Test
    public void testWorkflowRuleTemplateEndToEnd() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Hitting Workflow Rule Template Create Form API.");
        String createFormResponse = WorkflowRuleTemplateCreateForm.getCreateFormResponse().getResponseBody();

        if (ParseJsonResponse.validJsonResponse(createFormResponse)) {
            List<Integer> allEntityTypeIds = WorkflowRuleTemplateCreateForm.getAllEntityTypeIdsFromResponse(createFormResponse);

            if (allEntityTypeIds == null || allEntityTypeIds.isEmpty()) {
                throw new SkipException("Couldn't get All EntityTypeIds from Workflow Rule Template Create API Response.");
            }

            for (Integer entityTypeId : allEntityTypeIds) {
                String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

                try {
                    logger.info("Validating Workflow Rule Template End to End flow for Entity {}", entityName);

                    //Create Workflow Rule Template
                    String ruleTemplateName = "API Automation Workflow Rule Template End to End " + entityName;
                    String ruleTemplateJson = "{}";

                    String ruleTemplateCreatePayload = WorkflowRuleTemplateCreate.getPayload(ruleTemplateName, ruleTemplateJson, true, entityName);

                    String ruleTemplateCreateResponse = WorkflowRuleTemplateCreate.getCreateResponse(ruleTemplateCreatePayload).getResponseBody();

                    if (ParseJsonResponse.validJsonResponse(ruleTemplateCreateResponse)) {
                        JSONObject jsonObj = new JSONObject(ruleTemplateCreateResponse).getJSONObject("header").getJSONObject("response");
                        int newlyCreatedRuleTemplateId = jsonObj.getInt("entityId");

                        //Validate Listing
                        validateRuleTemplateListing(newlyCreatedRuleTemplateId, ruleTemplateName, entityName, csAssert);

                        //Validate Show
                        validateRuleTemplateShow(newlyCreatedRuleTemplateId, ruleTemplateName, entityName, csAssert);

                        //Validate Audit log
                        validateAuditLog(newlyCreatedRuleTemplateId, "saved", entityName, csAssert);

                        //Validate Edit
                        validateRuleTemplateEdit(newlyCreatedRuleTemplateId, entityName, csAssert);

                        //Delete Rule Template
                        validateRuleTemplateDelete(newlyCreatedRuleTemplateId, entityName, csAssert);
                    } else {
                        csAssert.assertTrue(false, "Workflow Rule Template Create API Response is an Invalid JSON for Entity " + entityName);
                    }
                } catch (SkipException e) {
                    throw new SkipException(e.getMessage());
                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception while Validating Workflow Rule Template End to End Flow for Entity " + entityName +
                            ". " + e.getMessage());
                }
            }
        } else {
            csAssert.assertTrue(false, "Workflow Rule Template Create Form API Response is an Invalid JSON.");
        }

        csAssert.assertAll();
    }

    private void validateRuleTemplateListing(int ruleTemplateId, String ruleTemplateName, String entityName, CustomAssert csAssert) {
        try {
            boolean positiveTest = (ruleTemplateName != null);

            logger.info("Validating Workflow Rule Template Listing for Entity {}", entityName);
            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(484, true, ListDataHelper.getPayloadForListData(331, 20, 0), null);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");
                int idColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                boolean ruleTemplateIdFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumn)).getString("value");

                    if (value.contains(String.valueOf(ruleTemplateId))) {
                        ruleTemplateIdFound = true;

                        //Validate Rule Template Name in Listing
                        String nameColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "rulename");
                        String nameValue = jsonArr.getJSONObject(i).getJSONObject(nameColumnId).getString("value");

                        csAssert.assertTrue(nameValue.equalsIgnoreCase(ruleTemplateName), "Expected Rule Template Name: " + ruleTemplateName +
                                " and Actual Rule Template Name: " + nameValue + " in ListData Response for Entity " + entityName);

                        //Validate Rule Json
                        String jsonColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "rulejson");
                        String jsonValue = jsonArr.getJSONObject(i).getJSONObject(jsonColumnId).getString("value");

                        csAssert.assertTrue(jsonValue.equalsIgnoreCase("{}"),
                                "Expected Rule Template Json Value: {} and Actual Value: " + jsonValue + " in ListData Response for Entity " + entityName);

                        //Validate Active Value
                        String activeColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "active");
                        String activeValue = jsonArr.getJSONObject(i).getJSONObject(activeColumnId).getString("value");

                        csAssert.assertTrue(activeValue.equalsIgnoreCase("true"),
                                "Expected Rule Template Active Value: true and Actual Value: " + activeValue + " in ListData Response for Entity " + entityName);

                        break;
                    }
                }

                if (positiveTest) {
                    csAssert.assertTrue(ruleTemplateIdFound, "Workflow Rule Template having Id: " + ruleTemplateId +
                            " not found in Listing API Response for Entity " + entityName);
                } else {
                    csAssert.assertFalse(ruleTemplateIdFound, "Workflow Rule Template having Id: " + ruleTemplateId +
                            " is still present in Listing API Response for Entity " + entityName);
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Workflow Rule Template is an Invalid JSON for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing for Rule Template Id: " + ruleTemplateId + " and Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateRuleTemplateShow(int ruleTemplateId, String expectedRuleTemplateName, String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Rule Template Show for Entity {}", entityName);
            String showResponse = WorkflowRuleTemplateShow.getWorkflowRuleTemplateShowResponse(ruleTemplateId).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

                //Validate Rule Template Name
                String actualRuleTemplateName = jsonObj.getJSONObject("workflowRuleTemplateName").getString("values");
                csAssert.assertTrue(actualRuleTemplateName.equalsIgnoreCase(expectedRuleTemplateName), "Expected Rule Template Name: " + expectedRuleTemplateName +
                        " and Actual Rule Template Name: " + actualRuleTemplateName + " for Entity " + entityName);
            } else {
                csAssert.assertTrue(false, "Workflow Rule Template Show API Response for Rule Template Id " + ruleTemplateId +
                        " is an Invalid JSON for Entity " + entityName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Show of Workflow Rule Template Id: " + ruleTemplateId + " for Entity " + entityName +
                    ". " + e.getMessage());
        }
    }

    private void validateAuditLog(int ruleTemplateId, String expectedActionName, String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Rule Template Audit Logs for Entity {}", entityName);
            ListRendererTabListData tabListDataObj = new ListRendererTabListData();

            String payload = "{\"filterMap\":{\"entityTypeId\":331,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            tabListDataObj.hitListRendererTabListData(61, 331, ruleTemplateId, payload, true);
            String auditLogTabResponse = tabListDataObj.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(auditLogTabResponse)) {
                JSONArray jsonArr = new JSONObject(auditLogTabResponse).getJSONArray("data");
                String actionColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "action_name");
                String actionValue = jsonArr.getJSONObject(0).getJSONObject(actionColumnId).getString("value");

                csAssert.assertTrue(actionValue.equalsIgnoreCase(expectedActionName), "Expected Action Name: " + expectedActionName +
                        " and Actual Action Name: " + actionValue + " for Entity " + entityName);

                String historyColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabResponse, "history");
                String historyValue = jsonArr.getJSONObject(0).getJSONObject(historyColumnId).getString("value");
                Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                FieldHistory historyObj = new FieldHistory();
                String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, 331, true);

                if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
                    JSONObject jsonObj = new JSONObject(fieldHistoryResponse);
                    jsonArr = jsonObj.getJSONArray("value");

                    if (!jsonObj.isNull("errorMessage")) {
                        csAssert.assertTrue(false, "Error in Field History API Response: " + jsonObj.getString("errorMessage") + " for Entity " +
                                entityName);
                    } else {
                        if (expectedActionName.equalsIgnoreCase("saved")) {
                            csAssert.assertTrue(jsonArr.length() == 0, "Expected History Value length: 0 and Actual Value length: " +
                                    jsonArr.length() + " for Entity " + entityName);
                        } else {
                            csAssert.assertTrue(jsonArr.length() > 0, "History Value Array is empty for Entity " + entityName);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Field History API Response for Rule Template Id: " + ruleTemplateId +
                            " is an Invalid JSON for Entity " + entityName);
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Audit Log of Workflow Rule Template Id: " + ruleTemplateId +
                        " is an Invalid JSON for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Log for Workflow Rule Template Id: " + ruleTemplateId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateRuleTemplateEdit(int ruleTemplateId, String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Rule Template Edit for Entity " + entityName);
            String newRuleTemplateName = "API Automation Rule Template Edit End to End Flow " + entityName;

            String payload = WorkflowRuleTemplateEditPost.getPayload(ruleTemplateId, newRuleTemplateName, "{}", true, 331);
            APIResponse response = WorkflowRuleTemplateEditPost.getEditPostResponse(payload);

            int responseCode = response.getResponseCode();
            csAssert.assertTrue(responseCode == 200, "Edit API Expected Status Code: 200 and Actual Status Code: " + responseCode +
                    " for Entity " + entityName);

            String status = ParseJsonResponse.getStatusFromResponse(response.getResponseBody());
            if (status.equalsIgnoreCase("success")) {
                validateRuleTemplateShow(ruleTemplateId, newRuleTemplateName, entityName, csAssert);

                validateAuditLog(ruleTemplateId, "updated", entityName, csAssert);
            } else {
                csAssert.assertTrue(false, "Couldn't Edit Update Workflow Rule Template for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit of Workflow Rule Template Id: " + ruleTemplateId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private void validateRuleTemplateDelete(int ruleTemplateId, String entityName, CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow Rule Template Delete for Entity {}", entityName);
            APIResponse response = WorkflowRuleTemplateDelete.getDeleteResponse(WorkflowRuleTemplateDelete.getPayload(ruleTemplateId));
            int responseCode = response.getResponseCode();

            csAssert.assertTrue(responseCode == 200, "Delete API Expected Status Code: 200 and Actual Status Code: " + responseCode +
                    " for Entity " + entityName);

            String status = ParseJsonResponse.getStatusFromResponse(response.getResponseBody());
            csAssert.assertTrue(status.equalsIgnoreCase("success"), "Delete API Expected Status: success and Actual Status: " + status +
                    " for Entity " + entityName);

            //Validate data in DB.
            List<String> dataInDb = WorkflowRuleTemplateDbHelper.getRuleTemplateDataFromDB("deleted", ruleTemplateId);
            String deletedValueInDb = dataInDb.get(0).equalsIgnoreCase("t") ? "true" : "false";

            csAssert.assertTrue(deletedValueInDb.equalsIgnoreCase("true"), "Deleted Flag in DB is still not set to false for Rule Template Id: " +
                    ruleTemplateId + " for Entity " + entityName);

            validateRuleTemplateListing(ruleTemplateId, null, entityName, csAssert);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Delete of Workflow Rule Template Id: " + ruleTemplateId + " for Entity " +
                    entityName + ". " + e.getMessage());
        }
    }
}