package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class TestCA16 extends TestAPIBase {

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCA16FilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCA16FileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";
    }

    /*
    TC-C88444: Verify that 'No Touch Contract' metadata field is present in CDR.
     */
    @Test
    public void testC88444() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String response = executor.get("/tblcdr/create/rest?version=2.0", ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();

            if (ParseJsonResponse.validJsonResponse(response)) {
                Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(response, "noTouchContract");

                if (fieldMap == null || fieldMap.isEmpty()) {
                    csAssert.assertFalse(true, "Couldn't find Field 'No Touch Contract' in Create/New API Response for CDR.");
                } else {
                    //Validate Type
                    csAssert.assertEquals(fieldMap.get("type"), "checkbox",
                            "No Touch Contract Field Validation field failed. Expected Type: checkbox and Actual Type: " + fieldMap.get("type"));

                    //Validate Default Value
                    JSONObject jsonObj = new JSONObject(response).getJSONObject("body").getJSONObject("data").getJSONObject("noTouchContract");
                    if (jsonObj.has("values") && jsonObj.getBoolean("values")) {
                        csAssert.assertFalse(true, "No Touch Contract Field doesn't have default value as false in Create/New API Response.");
                    }
                }
            } else {
                csAssert.assertFalse(true, "CDR Create/New API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88444. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C88448: Verify behavior when 'No Touch Contract' flag value is added from workflow inside value update task.
    Considering that it is defined in Workflow that after Creation Perform below workflow actions to make No Touch Contract is auto-updated to True value.

    Workflow Steps: SendForClientReview -> TestInternationalization -> Reject (with comment) -> SendForClientReview -> Approve
     */
    @Test(dependsOnMethods = "testC88444")
    public void testC88448() {
        CustomAssert csAssert = new CustomAssert();
        int newCdrId = -1;

        try {
            String createSection = "no touch contract";
            String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, createSection, true);

            if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.getStatusFromResponse(createResponse).equalsIgnoreCase("success")) {
                newCdrId = CreateEntity.getNewEntityId(createResponse);
                String showResponse = ShowHelper.getShowResponseVersion2(160, newCdrId);

                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                    //Perform Workflow Actions
                    String[] actions = {"SendForClientReview"};
                    boolean actionPerformed = true;
                    String actionFailed = null;

                    for (String actionName : actions) {
                        actionPerformed = performAction(newCdrId, actionName);

                        if (!actionPerformed) {
                            actionFailed = actionName;
                            break;
                        }
                    }

                    if (!actionPerformed) {
                        csAssert.assertFalse(true, "Action " + actionFailed + " failed. Hence skipping further validation.");
                    } else {
                        showResponse = ShowHelper.getShowResponseVersion2(160, newCdrId);
                        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("noTouchContract");

                        csAssert.assertEquals(jsonObj.getBoolean("values"), true,
                                "No Touch Contract field Validation failed. Expected Value: True and Actual Value: " + jsonObj.getBoolean("values"));
                    }
                } else {
                    csAssert.assertFalse(true, "Show Page Response for CDR Id " + newCdrId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't create CDR.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88448. " + e.getMessage());
        }

        if (newCdrId != -1) {
            EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
        }

        csAssert.assertAll();
    }

    private boolean performAction(int cdrId, String actionName) {
        try {
            String actionsResponse = Actions.getActionsV3Response(160, cdrId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";

            if (actionName.equalsIgnoreCase("Reject")) {
                JSONObject commentsJsonObj = new JSONObject(payload).getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments");
                commentsJsonObj.put("values", "test");

                JSONObject commentJsonObj = new JSONObject(payload).getJSONObject("body").getJSONObject("data").getJSONObject("comment").put("comments", commentsJsonObj);
                JSONObject jsonObj = new JSONObject(payload);
                jsonObj.getJSONObject("body").getJSONObject("data").put("comment", commentJsonObj);

                payload = jsonObj.toString();
            }

            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }

    /*
    TC-CC88454: Verify Internationalization for 'No Touch Contract' field.
     */
    @Test
    public void testC88454() {
        CustomAssert csAssert = new CustomAssert();
        UpdateAccount updateAccObj = new UpdateAccount();

        try {
            updateAccObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1000);

            new AdminHelper().loginWithClientAdminUser();

            FieldRenaming fieldRenamingObj = new FieldRenaming();
            String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, 850);

            if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
                String expectedLabel = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, "No Touch Contract");
                new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

                String response = executor.get("/tblcdr/create/rest?version=2.0", ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();

                Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(response, "noTouchContract");

                if (fieldMap == null || fieldMap.isEmpty()) {
                    csAssert.assertFalse(true, "Couldn't find Field 'No Touch Contract' in Create/New API Response for CDR.");
                } else {
                    String label = fieldMap.get("label");

                    if (!label.equalsIgnoreCase(expectedLabel)) {
                        csAssert.assertFalse(true, "Internationalization failed for No Touch Contract Field. Expected Label: " + expectedLabel +
                                " and Actual Label: " + label);
                    }
                }
            } else {
                csAssert.assertFalse(true, "Field Renaming Response for language id 1000 and group id 850 is an invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88454. " + e.getMessage());
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        } finally {
            updateAccObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1);
        }

        csAssert.assertAll();
    }

    /*
    TC-C88800: Verify that if 'No Touch Contract' is true and template is updated, PDF version of Template will get generated.
     */
    @Test
    public void testC88800() {
        CustomAssert csAssert = new CustomAssert();
        int newCdrId = -1;

        try {
            String createSection = "c88800";
            String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, createSection, true);

            if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.getStatusFromResponse(createResponse).equalsIgnoreCase("success")) {
                newCdrId = CreateEntity.getNewEntityId(createResponse);

                validateTemplateGeneration(newCdrId, createSection, "pdf", csAssert);
            } else {
                csAssert.assertFalse(true, "Couldn't create CDR.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88800. " + e.getMessage());
        }

        if (newCdrId != -1) {
            EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
        }

        csAssert.assertAll();
    }

    private void validateTemplateGeneration(int cdrId, String flowName, String expectedVersion, CustomAssert csAssert) throws Exception {
        Edit editObj = new Edit();

        String editGetResponse = editObj.getEditPayload("contract draft request", cdrId);
        Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowName + " edit payload");

        String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

        JSONObject jsonObj = new JSONObject(editGetResponse);
        jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

        jsonObj.getJSONObject("mappedContractTemplates").put("values",new JSONObject(mappedTemplatePayload).getJSONArray("values"));

        String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
        String updateResponse = editObj.hitEdit("contract draft request", updatePayload);

        if (ParseJsonResponse.getStatusFromResponse(updateResponse).equalsIgnoreCase("success")) {
            String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367);
            String documentNameId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");
            String documentNameValue = new JSONObject(tabListResponse).getJSONArray("data")
                    .getJSONObject(0).getJSONObject(documentNameId).getString("value");
            String expectedTemplateName = editProperties.get("expectedTemplateName");

            if (!documentNameValue.contains(expectedTemplateName + ":;" + expectedVersion)) {
                csAssert.assertFalse(true, expectedVersion + " Version of Template not generated.");
            }
        } else {
            csAssert.assertFalse(true, "CDR Update failed. Hence couldn't validate further.");
        }
    }

    /*
    TC-C88801: Verify that if 'No Touch Contract' value is false and template is updated, Word version of template will get generated.
     */
    @Test
    public void testC88801() {
        CustomAssert csAssert = new CustomAssert();

        int newCdrId = -1;

        try {
            String createSection = "c88801";
            String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, createSection, true);

            if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.getStatusFromResponse(createResponse).equalsIgnoreCase("success")) {
                newCdrId = CreateEntity.getNewEntityId(createResponse);

                validateTemplateGeneration(newCdrId, createSection, "docx", csAssert);
            } else {
                csAssert.assertFalse(true, "Couldn't create CDR.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88801. " + e.getMessage());
        }

        if (newCdrId != -1) {
            EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
        }


        csAssert.assertAll();
    }
}