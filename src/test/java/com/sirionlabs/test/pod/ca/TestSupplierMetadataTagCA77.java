package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataCreate;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.presignature.FindMappedTags;
import com.sirionlabs.api.presignature.TagValidation;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Clause;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.ContractTemplate;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestSupplierMetadataTagCA77 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSupplierMetadataTagCA77.class);

    private static String configFilePath;
    private static String configFileName;
    private String extraFieldsConfigFileName;

    private static int clauseIdWithSupplierMetadataTag = -1;
    private static int newClauseTagId = -1;
    private static String newCustomFieldName = "AutomationClauseTag";
    private static int newCustomFieldId = -1;
    private UpdateAccount updateAccount = new UpdateAccount();

    private Options optionsObj = new Options();
    private Check checkObj = new Check();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSupplierMetadataTagFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSupplierMetadataTagFileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";

    }

    @AfterClass
    public void afterClass() {
        deleteClauseAndCustomField();
    }

    /*
    TC-C89074: Create Tag on Clause create page.
     */
    @Test(enabled = false)
    public void testC89074() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-CC89074: Create Tag on Clause Create page.");
            //Check tag in Options API.

            Map<String, String> optionParams = new HashMap<>();
            optionParams.put("pageType", "6");
            optionParams.put("entityTpeId", "206");
            optionParams.put("pageEntityTypeId", "206");
            optionParams.put("query", "Status");
            optionParams.put("languageType", "1");

            optionsObj.hitOptions(18, optionParams);
            String optionsResponse = optionsObj.getOptionsJsonStr();

            JSONObject jsonObj = new JSONObject(optionsResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            boolean tagFound = false;

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                String tagName = jsonObj.getString("name");
                jsonObj = jsonObj.getJSONObject("customData");

                if (tagName.equalsIgnoreCase("status")) {
                    tagFound = true;

                    //Validate Tag Information
                    String apiName = jsonObj.getString("apiName");
                    csAssert.assertEquals(apiName, "status",
                            "Tag Details Validation failed in Options API Response. Expected API Name: Status and Actual API Name: " + apiName);

                    int entityTypeId = jsonObj.getJSONObject("entityType").getInt("id");
                    csAssert.assertEquals(entityTypeId, 1,
                            "Tag Details Validation failed in Options API Response. Expected EntityTypeId: 1 and Actual EntityTypeId: " + entityTypeId);

                    String fieldType = jsonObj.getString("type");
                    csAssert.assertTrue(fieldType.equalsIgnoreCase("Text Field"),
                            "Tag Details Validation failed in Options API Response. Expected Type: Text Field and Actual Type: " + fieldType);
                    break;
                }
            }

            csAssert.assertTrue(tagFound, "Status Field not found in Options API Response");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-CC89074. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C89075: Verify Supplier Metadata can be used as Tag inside Clause for Custom Field type.
     */
    @Test(enabled = false)
    public void testC89075() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89075: Verify Supplier Metadata can be used as Tag inside Clause for Custom Field type.");
            AdminHelper adminObj = new AdminHelper();
            String lastUserName = Check.lastLoggedInUserName;
            String lastUserPassword = Check.lastLoggedInUserPassword;

            adminObj.loginWithClientAdminUser();

            HashMap<String, String> params = DynamicMetadataCreate.getParameters(newCustomFieldName, newCustomFieldName, 77665, 1,
                    505, 19, 1, 1);

            params.put("createClauseTag", "true");
            params.put("dynamicFieldValidations[0].validationType", "MAX");
            params.put("dynamicFieldValidations[0].message", "AutomationClauseTag Validation");
            params.put("dynamicFieldValidations[0].parameter", "1");
            params.put("_linkedParent", "on");
            params.put("_linkedChild", "on");

            logger.info("Hitting Create Dynamic Field API for Field Name {} and EntityTypeId {}", newCustomFieldName, 1);
            Integer responseCode = executor.postMultiPartFormData(DynamicMetadataCreate.getAPIPath(), DynamicMetadataCreate.getHeaders(),
                    params).getResponse().getResponseCode();

            adminObj.loginWithUser(lastUserName, lastUserPassword);

            if (responseCode == 302) {
                newCustomFieldId = DynamicMetadataCreate.getFieldId(newCustomFieldName);

                //Check tag in Options API.
                Map<String, String> optionParams = new HashMap<>();
                optionParams.put("pageType", "6");
                optionParams.put("entityTpeId", "206");
                optionParams.put("pageEntityTypeId", "206");
                optionParams.put("query", newCustomFieldName);
                optionParams.put("languageType", "1");

                optionsObj.hitOptions(18, optionParams);
                String optionsResponse = optionsObj.getOptionsJsonStr();

                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    String tagName = jsonObj.getString("name");
                    jsonObj = jsonObj.getJSONObject("customData");
                    int tagId = jsonObj.getInt("id");

                    if (tagName.equalsIgnoreCase(newCustomFieldName) && tagId == newCustomFieldId) {
                        tagFound = true;
                        newClauseTagId = jsonArr.getJSONObject(i).getInt("id");

                        //Validate Tag Information
                        int entityTypeId = jsonObj.getJSONObject("entityType").getInt("id");
                        csAssert.assertEquals(entityTypeId, 1,
                                "Tag Details Validation failed in Options API Response. Expected EntityTypeId: 1 and Actual EntityTypeId: " + entityTypeId);

                        String fieldType = jsonObj.getString("type");
                        csAssert.assertTrue(fieldType.equalsIgnoreCase("Currency"),
                                "Tag Details Validation failed in Options API Response. Expected Type: Currency and Actual Type: " + fieldType);
                        break;
                    }
                }

                csAssert.assertTrue(tagFound, "Newly Created Custom Field " + newCustomFieldName + " not found in Options API Response");

                //Create Clause with new Tag.
                String createSection = "supplier metadata flow 1";
                UpdateFile.updateConfigFileProperty(configFilePath, "ExtraFields.cfg", createSection, "clauseTags",
                        "newClauseTagId", String.valueOf(newClauseTagId));

                String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                        createSection, false);

                try {
                    String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);

                    if (status.equalsIgnoreCase("success")) {
                        clauseIdWithSupplierMetadataTag = CreateEntity.getNewEntityId(clauseCreateResponse);

                        //Validate Clause Tag on Show Page.
                        String showResponse = ShowHelper.getShowResponseVersion2(138, clauseIdWithSupplierMetadataTag);
                        JSONObject clauseTagsObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                                .getJSONArray("values").getJSONObject(0);

                        csAssert.assertTrue(clauseTagsObj.getString("name").equalsIgnoreCase(newCustomFieldName),
                                "Clause Show Page Validation. Expected Tag Name: " + newCustomFieldName + " and Actual Name: " + clauseTagsObj.getString("name"));
                        csAssert.assertTrue(clauseTagsObj.getInt("id") == newClauseTagId, "Clause Show Page Validation. Expected Tag Id: " +
                                newClauseTagId + " and Actual Id: " + clauseTagsObj.getInt("id"));

                        clauseTagsObj = clauseTagsObj.getJSONObject("tagHTMLType");
                        csAssert.assertTrue(clauseTagsObj.getInt("id") == 19,
                                "Clause Show Page Validation. Expected TagHtmlType Id: 19 and Actual Id: " + clauseTagsObj.getInt("id"));
                        csAssert.assertTrue(clauseTagsObj.getString("name").equalsIgnoreCase("currency"),
                                "Clause Show Page Validation. Expected TagHtmlType Name: currency and Actual Name: " + clauseTagsObj.getString("name"));
                    } else {
                        csAssert.assertFalse(true, "Couldn't create Clause due to " + status);
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while Validating Tag in Clause. " + e.getMessage());
                }

                UpdateFile.updateConfigFileProperty(configFilePath, "ExtraFields.cfg", createSection, "clauseTags",
                        String.valueOf(newClauseTagId), "newClauseTagId");

            } else {
                csAssert.assertFalse(true, "Couldn't create Custom Field.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89075. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private boolean performActionOnClause(int clauseId, String actionName) {
        try {
            String actionsResponse = Actions.getActionsV3Response(138, clauseId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(138, clauseId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";

            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }


    /*
    TC-C89079: Verify that clause having Supplier Metadata tags is getting consumed inside template and CDR Wizard
     */
    @Test (enabled = false)
    public void testC89079() {
        CustomAssert csAssert = new CustomAssert();
        int ctId = -1;
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C89079: Verify that Clause having Supplier Metadata Tag is getting consumed inside Template and CDR Wizard.");
            logger.info("Creating Template with Clause having Supplier Metadata Tag.");

            String ctCreateResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                    "c89079 template creation", false);
            String ctResult = ParseJsonResponse.getStatusFromResponse(ctCreateResponse);

            if (ctResult.equalsIgnoreCase("success")) {
                ctId = CreateEntity.getNewEntityId(ctCreateResponse);

                logger.info("Creating CDR with CT having Clause that contains Supplier Metadata Tag.");
                String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                        "c89079 cdr creation", true);
                String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

                if (cdrResult.equalsIgnoreCase("success")) {
                    cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

                    UpdateFile.updateConfigFileProperty(configFilePath, configFileName, "c89079 cdr edit", "mappedContractTemplates",
                            "ct id", String.valueOf(ctId));

                    logger.info("Adding Template to Newly Created CDR.");
                    Edit editObj = new Edit();

                    String editGetResponse = editObj.hitEdit("contract draft request", cdrId);
                    Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                            "c89079 cdr edit");

                    String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

                    JSONObject jsonObj = new JSONObject(editGetResponse);
                    jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                    jsonObj.put("mappedContractTemplates", new JSONObject(mappedTemplatePayload));
                    String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
                    String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
                    String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

                    UpdateFile.updateConfigFileProperty(configFilePath, configFileName, "c89079 cdr edit", "mappedContractTemplates",
                            String.valueOf(ctId), "ct id");

                    if (updateResult.equalsIgnoreCase("success")) {

                    } else {
                        csAssert.assertFalse(true, "Couldn't Update CDR with CT having Clause that contains Supplier Metadata Tag due to: " + updateResult);
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't Create CDR with CT having Clause that contains Supplier Metadata Tag due to: " + cdrResult);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create CT with Clause having Supplier Metadata Tag due to: " + ctResult);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89079: " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
        }

        csAssert.assertAll();
    }

    /*
   TC-C89080: Verify that if CDR is having single supplier, tag values will get pre-filled for supplier metadata.
   TC -C89082
    */
    @Test(enabled = true)
    public void testC89080() {
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
                logger.info("Starting Test TC-CC89080: Create Tag on Clause Create page.");
                //Check tag in Options API.

                Map<String, String> optionParams = new HashMap<>();
                optionParams.put("pageType", "6");
                optionParams.put("entityTpeId", "206");
                optionParams.put("pageEntityTypeId", "206");
                optionParams.put("query", "Name");
                optionParams.put("languageType", "1");

                optionsObj.hitOptions(18, optionParams);
                String optionsResponse = optionsObj.getOptionsJsonStr();

                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    String tagName = jsonObj.getString("name");
                    jsonObj = jsonObj.getJSONObject("customData");

                    if (tagName.equalsIgnoreCase("name")) {
                        tagFound = true;

                        //Validate Tag Information
                        String apiName = jsonObj.getString("apiName");
                        csAssert.assertEquals(apiName, "name",
                                "Tag Details Validation failed in Options API Response. Expected API Name: Name and Actual API Name: " + apiName);
                    }
                }
                String newClauseTagId = jsonArr.getJSONObject(0).get("id").toString();
                // create clause with metadata Tag supplier tag
                String clauseCreateSection = "c89080 single supplier tag clause creation";
                UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, clauseCreateSection, "clauseTags",
                        "newClauseTagId", String.valueOf(newClauseTagId));
                try {
                    String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                            clauseCreateSection, false);
                    String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);
                    if (status.equalsIgnoreCase("success")) {
                        csAssert.assertEquals(status, "success", " Create clause failed when supplier Metadata Tag is used");
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
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, clauseCreateSection, "clauseTags",
                                String.valueOf(newClauseTagId), "newClauseTagId");
                        if (clauseTagsObj.get("name") == "Name") {
                            csAssert.assertTrue(false, "Creating Clause that contains single Supplier Metadata Tag failed");
                        }
                    } else {
                        logger.info("Clause creation with tag Name having single supplier, tag is not successful because: " + status);
                    }
                    try {
                        //Creating CT using above clause
                        String createSectionCT = "c89080 single supplier tag clause to ct";
                        // update the clause id
                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSectionCT, "clauses",
                                "clauseId", String.valueOf(clauseId));
                        String ctCreateResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                                createSectionCT, false);
                        String createStatus = ParseJsonResponse.getStatusFromResponse(ctCreateResponse);
                        if (createStatus.equalsIgnoreCase("success"))
                            csAssert.assertEquals(createStatus, "success", " Creating CT failed with Clause that contains single Supplier Metadata Tag");

                        ctId = CreateEntity.getNewEntityId(ctCreateResponse);
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
                        if (ctClauseTagObj.get("name") == "Name") {
                            csAssert.assertTrue(false, " Created CT tag doesn't match as of clause supplier single Metadata tag i.e Name");
                        }
                        String ctResult = ParseJsonResponse.getStatusFromResponse(ctCreateResponse);

                        // cdr creation from CT with Supplier single supplier Tag
                        if (ctResult.equalsIgnoreCase("success")) {
                            try {
                                logger.info("Creating CDR with CT having Clause that contains single Supplier Metadata Tag.");
                                String cdrEdit = "c89080 cdr single supplier edit";
                                String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                                        "c89079 cdr creation", true);
                                String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);
                                String findMappedTagCreateSection = "c89080 cdr findmapped tags";

                                // update find mapped template id
                                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, findMappedTagCreateSection, "templateid",
                                        "ct id", String.valueOf(ctId));

                                Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, findMappedTagCreateSection);

                                if (cdrResult.equalsIgnoreCase("success")) {
                                    cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);
                                    // edit api
                                    //Update Tag Values
                                    logger.info("Updating Tag Values");
                                    String tagValidationResponse = TagValidation.getTagValidationResponse(properties.get("tagvalidationpayload"));
                                    boolean isValidationSuccessful = TagValidation.isTagValidationSuccessful(tagValidationResponse);
                                    if (isValidationSuccessful) {
                                        // update CT id
                                        UpdateFile.updateConfigFileProperty(configFilePath, configFileName, cdrEdit, "mappedContractTemplates",
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

                                        // Revert back the CT id update
                                        UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, cdrEdit, "mappedContractTemplates",
                                                String.valueOf(ctId), "ct id");
                                        if (updateResult.equalsIgnoreCase("success")) {
                                            logger.info("Hitting FindMappedTags API for CDR");
                                        }

                                        String findMappedTagsResponse = FindMappedTags.getFindMappedTagsResponse(cdrId, "[" + properties.get("templateid") + "]");
                                        cdrjsonObj = new JSONObject(findMappedTagsResponse).getJSONObject("data").getJSONObject("mappedTags")
                                                .getJSONObject(properties.get("templateid")).getJSONObject("mappedTags");
                                        JSONArray cdrJsonArr = new JSONArray(properties.get("tagvalidationpayload"));
                                        for (int i = 0; i < cdrJsonArr.length(); i++) {
                                            int expectedTagId = cdrJsonArr.getJSONObject(i).getInt("id");
                                            String expectedTagName = cdrJsonArr.getJSONObject(i).getString("name");
                                            String expectedDefaultValue = cdrJsonArr.getJSONObject(i).get("defaultValue").toString();

                                            if (cdrjsonObj.has(String.valueOf(expectedTagId))) {
                                                JSONObject tagJsonObj = cdrjsonObj.getJSONObject(String.valueOf(expectedTagId));

                                                String actualTagName = tagJsonObj.getString("name");
                                                String actualDefaultValue = tagJsonObj.get("defaultValue").toString();

                                                csAssert.assertEquals(expectedTagName, actualTagName, "FindMappedTags API validation failed. Expected Tag Name: " +
                                                        expectedTagName + " and Actual Tag Name: " + actualTagName);

                                                csAssert.assertEquals(expectedDefaultValue, actualDefaultValue, "FindMappedTags API validation failed. Expected Default Value: " +
                                                        expectedDefaultValue + " and Actual Default Value: " + actualDefaultValue);
                                            } else {
                                                csAssert.assertFalse(true, "FindMappedTags API doesn't contain Tag having Id " + expectedTagId);
                                            }
                                        }
                                    }
                                }
                                // Revert back find mapped template id
                                UpdateFile.updateConfigFileProperty(configFilePath, configFileName, findMappedTagCreateSection, "templateid",
                                        String.valueOf(ctId), "ct id");
                                // tablistdata to check the CDR document Tab

                                int entityTypeId = 160;
                                String payload= "{\"filterMap\":{\"entityTypeId\":"+entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                                try{
                                    String CDRDocumentTab = TabListDataHelper.hitTabListDataAPIForCDRContractDocumentTab(7516, payload);
                                    JSONObject cdrDocumentTabjsonObj = new JSONObject(CDRDocumentTab);
                                    String documentName = cdrDocumentTabjsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(14388)).get("value").toString();
                                    csAssert.assertEquals(documentName,"19848:;CT from supplier metadata tag clause:;docx:;160:;16121","CDR document Tab file is not as expected file i.e 19848:;CT from supplier metadata tag clause:;docx:;160:;16121");

                                }catch (Exception e){
                                    csAssert.assertFalse(true, "CDR Contract document tab tablistdata render list of documnets uploaded failed because: " + e.getMessage());
                                }

                            } catch (Exception e) {
                                csAssert.assertFalse(true, "Creating CDR failed with CT having Clause that contains single Supplier Metadata Tag because: " + e.getMessage());

                            }
                        }

                    } catch (Exception e) {
                        csAssert.assertFalse(true, "Creating CT failed using Clause that contains single Supplier Metadata Tag because: " + e.getMessage());
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Clause creation failed with supplier tag Name because: " + e.getMessage());

                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-CC89080. " + e.getMessage());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Language update failed: " + e.getMessage());
        }
        finally {
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

    private void deleteClauseAndCustomField() {
        try {
            //Delete Clause
            if (clauseIdWithSupplierMetadataTag != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseIdWithSupplierMetadataTag);
            }

            //Delete Custom Field
            new PostgreSQLJDBC().deleteDBEntry("delete from clause_tag where id = " + newClauseTagId);
            DynamicMetadataCreate.deleteDynamicField(newCustomFieldName, newCustomFieldId);
        } catch (Exception e) {
            logger.error("Exception while Deleting Tag and Custom Field. " + e.getMessage());
        }
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