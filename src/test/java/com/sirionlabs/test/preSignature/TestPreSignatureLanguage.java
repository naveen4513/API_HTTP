package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Clause;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.ContractTemplate;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.UpdateFile;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestPreSignatureLanguage extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestPreSignatureLanguage.class);

    private String configFilePath;
    private String configFileNme;
    private String extraFieldsConfigFileName;

    private FieldRenaming fieldRenamingObj = new FieldRenaming();
    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private Options optionObj = new Options();
    private WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
    private UpdateAccount updateAccount = new UpdateAccount();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestPreSigLanguageConfigFilePath");
        configFileNme = ConfigureConstantFields.getConstantFieldsProperty("TestPreSigLanguageConfigFileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";
    }

    /*
    TC-C13795: Verify Tags in CT should come in same language as selected for CT.
     */
    @Test
    public void testC13795() {
        CustomAssert csAssert = new CustomAssert();
        int ctId = -1;

        try {
            logger.info("Starting Test TC-C13795.");

            //Creating CT using already existing Clause which has Supplier Metadata Tag Supplier.
            String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                    "c13795 ct creation", false);
            String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (createStatus.equalsIgnoreCase("success")) {
                ctId = CreateEntity.getNewEntityId(createResponse);
                String templatePageDataResponse = TemplatePageData.getTemplatePageDataResponse(ctId);

                JSONObject jsonObj = new JSONObject(templatePageDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("clauseTags");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (!jsonObj.getJSONObject("entityField").isNull("label")) {
                        tagFound = true;

                        String actualTagName = jsonObj.getString("name");

                        adminHelperObj.loginWithClientAdminUser();
                        String fieldLabelResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1);
                        String expectedTagName = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, "Metadata", "Status");

                        if (!actualTagName.equalsIgnoreCase(expectedTagName)) {
                            csAssert.assertTrue(false, "Expected Tag Name: " + expectedTagName + " and Actual Tag Name: " + actualTagName +
                                    " in CT Template Viewer.");
                        }

                        break;
                    }
                }

                if (!tagFound) {
                    csAssert.assertTrue(false, "Status Tag not found in Template Viewer of CT Id " + ctId);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create CT. Hence couldn't validate further steps.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C13795: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
        }

        csAssert.assertAll();
    }

    //Steps performed
    // update the language of the user
    // Create tag
    // check Tag on Show page - Confirm the Tag
    // Move Clause to CT
    // Link CT to CDR
    // change the user language again
    // delete the tag created.
    /* C4566 Verify the tags appear in the same language in clause content as selected in the language metadata of clause in Create page
      C4567 Verify the viewer and the tag menu shows the tags in same language as that selected in clause metadata
    */
    @Test
    public void testc4566() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;
        int ctId = -1;
        int cdrId = -1;
        boolean languageUpdate = false;

        // update the language of the user to English i.e language_id = 1
        try {
            languageUpdate = updateAccount.updateUserLanguage("anay_user", 1002, 1);
            if (languageUpdate == true) {
                logger.info("User language changed to English sucessfully");
            }

            try {
                logger.info("Starting Test TC-C4566.");
                // Options API
                HashMap<String, String> map = new HashMap<>();
                map.put("pageType", "6");
                map.put("entityTpeId", "206");
                map.put("pageEntityTypeId", "206");
                map.put("query", "नाम");
                map.put("languageType", "1");

                optionObj.hitOptions(18, map);
                String optionsResponse = optionObj.getOptionsJsonStr();
                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                String tagName = String.valueOf(jsonObj.getJSONArray("data").getJSONObject(0).get("name"));
                csAssert.assertEquals(tagName, "नाम",
                        "Tag Details Validation failed in Options API Response. Expected name as: नाम in hindi language and Actual name is: " + tagName);
                String newClauseTagId = jsonObj.getJSONArray("data").getJSONObject(0).get("id").toString();

                // Clause create API
                String createSection = "language ct and cdr c4566";
                UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                        "newClauseTagId", String.valueOf(newClauseTagId));

                String clauseCreateResponse = Clause.createClause(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                        createSection, false);
                try {
                    String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);
                    if (status.equalsIgnoreCase("success")) {
                        csAssert.assertEquals(status, "success", " Create clause failed when language is hindi and name is नाम");
                        // move the clause to correct state (Approve the Clause)
                        clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);
                        String[] actions = {"SendForClientReview", "Approve", "Publish"};
                        boolean actionPerformed = true;
                        String actionFailed = null;
                        for (String actionName : actions) {

                            actionPerformed = performAction(clauseId, actionName, 138);

                            if (!actionPerformed) {
                                actionFailed = actionName;
                                break;
                            }
                        }
                        //Validate Clause Tag on Show Page.
                        String showResponse = ShowHelper.getShowResponseVersion2(138, clauseId);
                        JSONObject clauseTagsObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                                .getJSONArray("values").getJSONObject(0);
                        // revert back the changes in clause
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                                String.valueOf(newClauseTagId), "newClauseTagId");

                        csAssert.assertTrue(clauseTagsObj.get("name") == "नाम", "Clause Show Page Validation. Expected Tag Name is नाम and actual is: "+ clauseTagsObj.get("name") +" Expected Tag Id: " +
                                newClauseTagId + " and Actual Id: " + clauseTagsObj.getInt("id"));
                        if (clauseTagsObj.get("name") == "नाम") {
                            csAssert.assertTrue(false, "Created clause Tag text is not as per expected i.e नाम tag using hindi नाम");
                        }
                    } else {
                        logger.info("Clause creation with tag नाम is not successful because: " + status);
                    }

                    try {
                        //Creating CT using above clause
                        String createSectionCT = "language clause to ct c4566";
                        // update the clause id
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSectionCT, "clauses",
                                "clauseId", String.valueOf(clauseId));
                        String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                                createSectionCT, false);

                        String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);
                        if (createStatus.equalsIgnoreCase("success"))
                            csAssert.assertEquals(createStatus, "success", " Create CT failed from clause when language is hindi and name is नाम");

                        ctId = CreateEntity.getNewEntityId(createResponse);
                        // revert back the changes
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSectionCT, "clauses",
                                String.valueOf(clauseId), "clauseId");

                        // Move CT to correct state (Approve CT)
                        String[] ctActions = {"SendForApproval", "Approve"};
                        boolean ctActionPerformed = true;
                        String ctActionFailed = null;
                        for (String actionName : ctActions) {

                            ctActionPerformed = performAction(ctId, actionName, 140);

                            if (!ctActionPerformed) {
                                ctActionFailed = actionName;
                                break;
                            }
                        }
                        String templatePageDataResponse = TemplatePageData.getTemplatePageDataResponse(ctId);
                        JSONObject ctClauseTagObj = new JSONObject(templatePageDataResponse).getJSONArray("clauseTags").getJSONObject(0);

                        if (ctClauseTagObj.get("name") == "नाम") {
                            csAssert.assertTrue(false, " Created CT tag doesn't match as of clause tag i.e नाम");
                        }

                        try {
                            // Create CDR from CT
                            String createSectionCDR = "c4566 cdr creation";
                            String cdrEdit = "c4566 cdr edit";
                            logger.info("Creating CDR with CT having ClauseTag नाम.");
                            String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                                    createSectionCDR, false);

                            String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);
                            if (cdrResult.equalsIgnoreCase("success")) {
                                cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

                                UpdateFile.updateConfigFileProperty(configFilePath, configFileNme, cdrEdit, "mappedContractTemplates",
                                        "ct id", String.valueOf(ctId));

                                logger.info("Adding Contract Template to Newly Created CDR.");
                                Edit editObj = new Edit();

                                String editGetResponse = editObj.hitEdit("contract draft request", cdrId);
                                Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, extraFieldsConfigFileName,
                                        cdrEdit);

                                String mappedTemplatePayload = editProperties.get("mappedContractTemplates");
                                JSONObject cdrjsonObj = new JSONObject(editGetResponse);
                                cdrjsonObj = cdrjsonObj.getJSONObject("body").getJSONObject("data");

                                jsonObj.put("mappedContractTemplates", new JSONObject(mappedTemplatePayload));
                                String updatePayload = "{\"body\":{\"data\":" + cdrjsonObj.toString() + "}}";
                                String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
                                String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

                                UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, cdrEdit, "mappedContractTemplates",
                                        String.valueOf(ctId), "ct id");
                                if (updateResult.equalsIgnoreCase("success")) {

                                } else {
                                    csAssert.assertFalse(true, "Couldn't Update CDR with CT having Clause tag i.e नाम: " + updateResult);
                                }
                            }
                        } catch (Exception e) {
                            csAssert.assertFalse(true, "CDR creation failed using CT, which had clause tag नाम because: " + e.getMessage());

                        }
                    } catch (Exception e) {
                        csAssert.assertFalse(true, "Contract Template creation failed using clause tag name नाम because: " + e.getMessage());
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Clause creation failed with tag name नाम because: " + e.getMessage());
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C4566: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Language update failed: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }

/*
    C4568 - Verify if the clause is edited with another language, the tags in viewer and tag menu changes according to the language
*/
    @Test
    public void testc4568() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;
        boolean languageUpdate = false;

        // update the language of the user to English i.e language_id = 1
        try {
            languageUpdate = updateAccount.updateUserLanguage("anay_user", 1002, 1);
            if (languageUpdate == true) {
                logger.info("User language changed to English sucessfully");
            }

            try {
                logger.info("Starting Test TC-C4566.");
                // Options API
                HashMap<String, String> map = new HashMap<>();
                map.put("pageType", "6");
                map.put("entityTpeId", "206");
                map.put("pageEntityTypeId", "206");
                map.put("query", "नाम");
                map.put("languageType", "1");

                optionObj.hitOptions(18, map);
                String optionsResponse = optionObj.getOptionsJsonStr();
                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                String newClauseTagId = null;
                if(jsonArr.length()==0){
                    logger.info("New Tag had to be created. No pre-existing value is been assigned to it.");
                    String clauseTagCreatePayload = "{\"name\":\"नाम\",\"tagHTMLType\":{\"id\":1}}";
                    HttpResponse createTagResponse = PreSignatureHelper.createTag(clauseTagCreatePayload);
                    csAssert.assertFalse(new JSONObject(createTagResponse).getJSONObject("data").getString("message").equalsIgnoreCase("Tag created successfully"),"Tag could not be created.");
                    newClauseTagId = new JSONObject(createTagResponse).getJSONObject("data").getJSONObject("id").toString();
                }else {
                    String tagName = String.valueOf(jsonObj.getJSONArray("data").getJSONObject(0).get("name"));
                    csAssert.assertEquals(tagName, "नाम",
                            "Tag Details Validation failed in Options API Response. Expected name as: नाम in hindi language and Actual name is: " + tagName);
                    newClauseTagId = jsonObj.getJSONArray("data").getJSONObject(0).get("id").toString();
                }

                // Clause create API
                String createSection = "language ct and cdr c4566";
                UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                        "newClauseTagId", String.valueOf(newClauseTagId));

                String clauseCreateResponse = Clause.createClause(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                        createSection, false);
                try {
                    String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);
                    clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);

                    if (status.equalsIgnoreCase("success")) {
                        csAssert.assertEquals(status, "success", " Create clause failed when language is hindi and name is नाम");

                        //Validate Clause Tag on Show Page.
                        String showResponse = ShowHelper.getShowResponseVersion2(138, clauseId);
                        JSONObject clauseTagsObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                                .getJSONArray("values").getJSONObject(0);


                        // revert back the changes in clause
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                                String.valueOf(newClauseTagId), "newClauseTagId");

                        if (clauseTagsObj.get("name") == "नाम") {
                            csAssert.assertTrue(false, "Created clause Tag text is not as per expected i.e नाम tag using hindi नाम");
                        }
                        // update the language to "English - United States" and "id": 11
                        try {
                            String updateClauseSection = "language clause edit c4568";
                            Edit editObj = new Edit();
                            String editGetResponse = editObj.hitEdit("clauses", clauseId);
                            Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileNme,
                                    updateClauseSection);

                            String languageTypePayload = editProperties.get("languageType");
                            JSONObject clauseLangUpdateJsonObj = new JSONObject(editGetResponse);
                            clauseLangUpdateJsonObj = clauseLangUpdateJsonObj.getJSONObject("body").getJSONObject("data");

                            clauseLangUpdateJsonObj.put("languageType", new JSONObject(languageTypePayload));
                            String updatePayload = "{\"body\":{\"data\":" + clauseLangUpdateJsonObj.toString() + "}}";
                            String updateResponse = editObj.hitEdit("clauses", updatePayload);
                            try {
                                String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);
                                if (updateResult.equalsIgnoreCase("success")) {
                                    logger.info("Updated The clause language to English - United States");
                                }
                            } catch (Exception e) {
                                csAssert.assertFalse(true, "Update clause language to English - United States failed because: " + e.getMessage());
                            }
                        } catch (Exception e) {
                            csAssert.assertFalse(true, "Edit language of Clause failed because: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Clause creation failed with tag name नाम because: " + e.getMessage());
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C4566: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Language update failed: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }
        csAssert.assertAll();
    }

    /*
    C4573 Verify the choose clause section should show the tags in its card of same language that selected in clause entity
     */
    @Test
    public void testc4573() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;
        int ctId = -1;
        int cdrId = -1;
        boolean languageUpdate = false;

        // update the language of the user to English i.e language_id = 1
        try {
            languageUpdate = updateAccount.updateUserLanguage("anay_user", 1002, 1);
            if (languageUpdate == true) {
                logger.info("User language changed to English sucessfully");
            }

            try {
                logger.info("Starting Test TC-C4566.");
                // Options API
                HashMap<String, String> map = new HashMap<>();
                map.put("pageType", "6");
                map.put("entityTpeId", "206");
                map.put("pageEntityTypeId", "206");
                map.put("query", "नाम");
                map.put("languageType", "1");

                optionObj.hitOptions(18, map);
                String optionsResponse = optionObj.getOptionsJsonStr();
                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                String tagName = String.valueOf(jsonObj.getJSONArray("data").getJSONObject(0).get("name"));
                csAssert.assertEquals(tagName, "नाम",
                        "Tag Details Validation failed in Options API Response. Expected name as: नाम in hindi language and Actual name is: " + tagName);
                String newClauseTagId = jsonObj.getJSONArray("data").getJSONObject(0).get("id").toString();

                // Clause create API
                String createSection = "language ct and cdr c4566";
                UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                        "newClauseTagId", String.valueOf(newClauseTagId));

                String clauseCreateResponse = Clause.createClause(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                        createSection, false);
                try {
                    String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);
                    if (status.equalsIgnoreCase("success")) {
                        csAssert.assertEquals(status, "success", " Create clause failed when language is hindi and name is नाम");
                        // move the clause to correct state (Approve the Clause)
                        clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);
                        String[] actions = {"SendForClientReview", "Approve", "Publish"};
                        boolean actionPerformed = true;
                        String actionFailed = null;
                        for (String actionName : actions) {

                            actionPerformed = performAction(clauseId, actionName, 138);

                            if (!actionPerformed) {
                                actionFailed = actionName;
                                break;
                            }
                        }
                        //Validate Clause Tag on Show Page.
                        String showResponse = ShowHelper.getShowResponseVersion2(138, clauseId);
                        JSONObject clauseTagsObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                                .getJSONArray("values").getJSONObject(0);
                        // revert back the changes in clause
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                                String.valueOf(newClauseTagId), "newClauseTagId");

                        if (clauseTagsObj.get("name") == "नाम") {
                            csAssert.assertTrue(false, "Created clause Tag text is not as per expected i.e नाम tag using hindi नाम");
                        }
                    } else {
                        logger.info("Clause creation with tag नाम is not successful because: " + status);
                    }

                    try {
                        //Creating CT using above clause
                        String createSectionCT = "clause ct different language c4573";
                        // update the clause id
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSectionCT, "clauses",
                                "clauseId", String.valueOf(clauseId));
                        String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                                createSectionCT, false);

                        String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);
                        if (createStatus.equalsIgnoreCase("success"))
                            csAssert.assertEquals(createStatus, "success", " Create CT failed from clause when language is hindi and name is नाम");

                        ctId = CreateEntity.getNewEntityId(createResponse);
                        // revert back the changes
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSectionCT, "clauses",
                                String.valueOf(clauseId), "clauseId");

                        String templatePageDataResponse = TemplatePageData.getTemplatePageDataResponse(ctId);
                        JSONObject ctClauseTagObj = new JSONObject(templatePageDataResponse).getJSONArray("clauseTags").getJSONObject(0);

                        if (ctClauseTagObj.get("name") == "नाम") {
                            csAssert.assertTrue(false, " Created CT tag doesn't match as of clause tag i.e नाम");
                        }
                        // check language for CT, go to show page of CT and open the CT and check the language
                        try {
                            String showResponse = ShowHelper.getShowResponseVersion2(140, ctId);
                            String ctlanguage = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("languageType").getJSONObject("values").get("name").toString();
                            if (ctlanguage == "English - United States") {
                                csAssert.assertTrue(false, "CT language is not English - United States as set in Ct creation");
                            }
                        } catch (Exception e) {
                            csAssert.assertTrue(false, "Show API for CT failed because:" + e.getMessage());
                        }

                    } catch (Exception e) {
                        csAssert.assertFalse(true, "Contract Template creation failed using clause tag name नाम because: " + e.getMessage());
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Clause creation failed with tag name नाम because: " + e.getMessage());
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C4566: " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Language update failed: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }


    @Test(enabled = false)
    public void testc4580() {
        CustomAssert csAssert = new CustomAssert();
        int ctId = -1;

        try {
            logger.info("Starting Test TC-C13795.");

            //Creating CT using already existing Clause which has Supplier Metadata Tag Supplier.
            String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                    "c13795 ct creation", false);
            String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (createStatus.equalsIgnoreCase("success")) {
                ctId = CreateEntity.getNewEntityId(createResponse);
                String templatePageDataResponse = TemplatePageData.getTemplatePageDataResponse(ctId);

                JSONObject jsonObj = new JSONObject(templatePageDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("clauseTags");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (jsonObj.getJSONObject("entityField").getString("label").equalsIgnoreCase("Status")) {
                        tagFound = true;

                        String actualTagName = jsonObj.getString("name");

                        adminHelperObj.loginWithClientAdminUser();
                        String fieldLabelResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1);
                        String expectedTagName = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, "Metadata", "Status");

                        if (!actualTagName.equalsIgnoreCase(expectedTagName)) {
                            csAssert.assertTrue(false, "Expected Tag Name: " + expectedTagName + " and Actual Tag Name: " + actualTagName +
                                    " in CT Template Viewer.");
                        }

                        break;
                    }
                }

                if (!tagFound) {
                    csAssert.assertTrue(false, "Status Tag not found in Template Viewer of CT Id " + ctId);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create CT. Hence couldn't validate further steps.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C13795: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
        }

        csAssert.assertAll();
    }

    private int getNewClauseId(int newClauseTagId) {
        try {
            String createSection = "c140993";
            UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                    "newClauseTagId", String.valueOf(newClauseTagId));

            String clauseCreateResponse = Clause.createClause(configFilePath, configFileNme, configFilePath, extraFieldsConfigFileName,
                    createSection, false);

            String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);

            UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                    String.valueOf(newClauseTagId), "newClauseTagId");

            if (status.equalsIgnoreCase("success"))
                return CreateEntity.getNewEntityId(clauseCreateResponse);
        } catch (Exception e) {
            logger.error("Exception while Creating Clause. " + e.getMessage());
        }

        return -1;
    }

    public boolean performAction(int entityCreatedId, String actionName, int entityTypeId) {
        try {
            String actionsResponse = Actions.getActionsV3Response(entityTypeId, entityCreatedId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, entityCreatedId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";

            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }
}