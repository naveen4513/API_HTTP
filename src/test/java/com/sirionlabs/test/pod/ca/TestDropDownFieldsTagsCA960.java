package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataCreate;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.TemplatePageData;
import com.sirionlabs.api.presignature.ClausePageData;
import com.sirionlabs.api.presignature.FindMappedTags;
import com.sirionlabs.api.presignature.TagValidation;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestDropDownFieldsTagsCA960 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDropDownFieldsTagsCA960.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    private OptionsHelper optionsHelperObj = new OptionsHelper();
    private ClausePageData clausePageDataObj = new ClausePageData();

    private String functionsInRussian;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCA960ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCA960ConfigFileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";

        new AdminHelper().loginWithClientAdminUser();
        FieldRenaming fieldRenamingObj = new FieldRenaming();
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 850);
        functionsInRussian = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, "Metadata", "functions");

        new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    /*
    TC-C140992: Verify 'Available as Clause Tag' Flag for Single and Multi Select Custom fields on Client Admin
    TC-C140993: Verify Newly Created Tag is available at Clause Creation.
     */
    @Test(enabled = false)
    public void testC140992() {
        CustomAssert csAssert = new CustomAssert();
        String newCustomFieldName = null;
        int newCustomFieldId = -1;
        int clauseId = -1;

        try {
            logger.info("Starting Test TC-C140992: Verify 'Available as Clause Tag' Flag for Single/Multi Select Custom Fields on Client Admin.");
            AdminHelper adminObj = new AdminHelper();
            String lastUserName = Check.lastLoggedInUserName;
            String lastUserPassword = Check.lastLoggedInUserPassword;

            adminObj.loginWithClientAdminUser();

            newCustomFieldName = "TestCDRSingleSelectFieldTag";
            HashMap<String, String> params = DynamicMetadataCreate.getParameters(newCustomFieldName, newCustomFieldName, 87432, 160,
                    3507, 3, 1, 1);

            params.put("createClauseTag", "true");
            params.put("_linkedParent", "on");
            params.put("_linkedChild", "on");
            params.put("dynamicFieldOptionValues[0].name", "Option1");
            params.put("dynamicFieldOptionValues[0].orderSeq", "87433");
            params.put("dynamicFieldOptionValues[0].active", "on");

            logger.info("Hitting Create Dynamic Field API for Field Name {} and EntityTypeId {}", newCustomFieldName, 1);
            Integer responseCode = executor.postMultiPartFormData(DynamicMetadataCreate.getAPIPath(), DynamicMetadataCreate.getHeaders(),
                    params).getResponse().getResponseCode();

            adminObj.loginWithUser(lastUserName, lastUserPassword);

            if (responseCode == 302) {
                newCustomFieldId = DynamicMetadataCreate.getFieldId(newCustomFieldName);
                int newClauseTagId = -1;

                //Check tag in Options API.
                String optionsResponse = optionsHelperObj.hitOptionsAPIForTags(newCustomFieldName, "1");

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
                        csAssert.assertEquals(entityTypeId, 160,
                                "Tag Details Validation failed in Options API Response. Expected EntityTypeId: 160 and Actual EntityTypeId: " + entityTypeId);

                        String fieldType = jsonObj.getString("type");
                        csAssert.assertTrue(fieldType.equalsIgnoreCase("Single Select"),
                                "Tag Details Validation failed in Options API Response. Expected Type: Single Select and Actual Type: " + fieldType);
                        break;
                    }
                }

                csAssert.assertTrue(tagFound, "Newly Created Custom Field " + newCustomFieldName + " not found in Options API Response");

                //Create Clause with new Tag.
                clauseId = getNewClauseId(newClauseTagId);

                try {
                    if (clauseId != -1) {
                        //Validate Clause Tag on Show Page.
                        String showResponse = ShowHelper.getShowResponseVersion2(138, clauseId);
                        JSONObject clauseTagsObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                                .getJSONArray("values").getJSONObject(0);

                        csAssert.assertTrue(clauseTagsObj.getString("name").equalsIgnoreCase(newCustomFieldName),
                                "Clause Show Page Validation. Expected Tag Name: " + newCustomFieldName + " and Actual Name: " + clauseTagsObj.getString("name"));
                        csAssert.assertTrue(clauseTagsObj.getInt("id") == newClauseTagId, "Clause Show Page Validation. Expected Tag Id: " +
                                newClauseTagId + " and Actual Id: " + clauseTagsObj.getInt("id"));

                        clauseTagsObj = clauseTagsObj.getJSONObject("tagHTMLType");
                        csAssert.assertTrue(clauseTagsObj.getString("name").equalsIgnoreCase("Single Select"),
                                "Clause Show Page Validation. Expected TagHtmlType Name: Single Select and Actual Name: " + clauseTagsObj.getString("name"));
                    } else {
                        csAssert.assertFalse(true, "Couldn't create Clause using Drop Down Select Field Tag.");
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while Validating Tag in Clause. " + e.getMessage());
                }
            } else {
                csAssert.assertFalse(true, "Couldn't create Custom Field.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140992: " + e.getMessage());
        } finally {
            //Delete Custom Field
            if (newCustomFieldName != null) {
                deleteCustomField(newCustomFieldName, newCustomFieldId);
            }

            //Delete Clause
            if (clauseId == -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }

    private int getNewClauseId(int newClauseTagId) {
        try {
            String createSection = "c140993";
            UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, createSection, "clauseTags",
                    "newClauseTagId", String.valueOf(newClauseTagId));

            String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
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

    private void deleteCustomField(String newCustomFieldName, int newCustomFieldId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();

            String query = "delete from tag_metadata_mapping where tag_id = (select id from clause_tag where name ='" + newCustomFieldName + "')";
            postgresObj.deleteDBEntry(query);

            query = "delete from clause_tag where name ='" + newCustomFieldName + "'";
            postgresObj.deleteDBEntry(query);

            postgresObj.closeConnection();

            DynamicMetadataCreate.deleteDynamicField(newCustomFieldName, newCustomFieldId);
        } catch (Exception e) {
            logger.error("Exception while Deleting Custom Field.");
        }
    }

    /*
    TC-C140995: Verify all Static Single & Multi Select Fields of CDR are available to be used as Tags.
     */
    @Test(enabled = false)
    public void testC140995() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C140995: Verify all Static Single & Multi Select Fields of CDR are available to be used as Tags");
        String[] allFieldNames = {"Agreement Type", "Suppliers", "Functions", "Services", "Recipient Market", "Created For", "Term Type", "Business Units",
                "Regions", "Countries", "Currency", "Client Contracting Entity", "Customer", "Language", "Vendor Contracting Party", "Spend Type",
                "Recipient Client Entity", "Contracting Client Entity"};

        Map<String, String> fieldsMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "fields mapping");

        for (String fieldName : allFieldNames) {
            try {
                String optionsResponse = optionsHelperObj.hitOptionsAPIForTags(fieldName, "1");

                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    jsonObj = jsonObj.getJSONObject("customData");

                    String actualApiName = null;
                    if (jsonObj.has("entityType")) {
                        actualApiName = jsonObj.getString("apiName");
                    }

                    if (actualApiName != null && actualApiName.equalsIgnoreCase(fieldsMap.get(fieldName))) {
                        String prefixMsg = "Tag Details Validation failed in Options API Response for Field " + fieldName + ". ";

                        tagFound = true;

                        //Validate Tag Information
                        int entityTypeId = jsonObj.getJSONObject("entityType").getInt("id");
                        csAssert.assertEquals(entityTypeId, 160,
                                prefixMsg + "Expected EntityTypeId: 160 and Actual EntityTypeId: " + entityTypeId);

                        String fieldType = jsonObj.getString("type");
                        csAssert.assertTrue((fieldType.equalsIgnoreCase("Single Select") || fieldType.equalsIgnoreCase("Multi Select")),
                                prefixMsg + "Expected Type: Single Select and Actual Type: " + fieldType);
                        break;
                    }
                }

                csAssert.assertTrue(tagFound, "Field " + fieldName + " not found in Options API Response");
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C140995 for Field " + fieldName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    @Test(enabled = false)
    public void testC141002() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;

        try {
            String sectionName = "c141002";
            String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, false);

            String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);

            if (status.equalsIgnoreCase("success")) {
                clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);

                //Validate Tag information in ClausePageData API.
                clausePageDataObj.hitClausePageData(clauseId);
                String clausePageDataResponse = clausePageDataObj.getClausePageDataResponseStr();

                JSONObject jsonObj = new JSONObject(clausePageDataResponse).getJSONArray("clauseTags").getJSONObject(0);
                String prefixMsg = "Multi-Select Field Services Tag Validation failed in ClausePageData API Response. ";

                //Validate identifier
                csAssert.assertTrue(jsonObj.getString("identifier").equalsIgnoreCase("services"),
                        prefixMsg + "Expected Identifier: Services and Actual Identifier: " + jsonObj.getString("identifier"));

                //Validate ApiName
                jsonObj = jsonObj.getJSONObject("entityField");
                csAssert.assertTrue(jsonObj.getString("apiName").equalsIgnoreCase("services"),
                        prefixMsg + "Expected APIName: Services and Actual APIName: " + jsonObj.getString("apiName"));

                //Validate EntityTypeId
                csAssert.assertTrue(jsonObj.getJSONObject("entityType").getInt("id") == 160,
                        prefixMsg + "Expected EntityTypeId: 160 and Actual EntityTypeId: " + jsonObj.getJSONObject("entityType").getInt("id"));
            } else {
                csAssert.assertFalse(true, "Couldn't Create Clause due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C141002: " + e.getMessage());
        } finally {
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C141003
    TC-C141004
    TC-C141012
    TC-C89081
     */
    @Test
    public void testC141003() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C141003.");

            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "c141003");
            String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                    "c141003 cdr creation", false);
            String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

            if (cdrResult.equalsIgnoreCase("success")) {
                cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

                //Update Tag Values
                logger.info("Updating Tag Values");

                String tagValidationResponse = TagValidation.getTagValidationResponse(properties.get("tagvalidationpayload"));
                boolean isValidationSuccessful = TagValidation.isTagValidationSuccessful(tagValidationResponse);

                if (isValidationSuccessful) {
                    logger.info("Adding Template to Newly Created CDR.");
                    Edit editObj = new Edit();

                    String editGetResponse = editObj.getEditPayload("contract draft request", cdrId);
                    Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                            "c141003 cdr edit");

                    String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

                    JSONObject jsonObj = new JSONObject(editGetResponse);
                    jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                    jsonObj.getJSONObject("mappedContractTemplates").put("values", new JSONObject(mappedTemplatePayload).getJSONArray("values"));
                    String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
                    String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
                    String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

                    if (updateResult.equalsIgnoreCase("success")) {
                        logger.info("Hitting FindMappedTags API for CDR");
                        String findMappedTagsResponse = FindMappedTags.getFindMappedTagsResponse(cdrId, "[" + properties.get("templateid") + "]");

                        jsonObj = new JSONObject(findMappedTagsResponse).getJSONObject("data").getJSONObject("mappedTags")
                                .getJSONObject(properties.get("templateid")).getJSONObject("mappedTags");

                        JSONArray jsonArr = new JSONArray(properties.get("tagvalidationpayload"));
                        for (int i = 0; i < jsonArr.length(); i++) {
                            int expectedTagId = jsonArr.getJSONObject(i).getInt("id");
                            String expectedTagName = jsonArr.getJSONObject(i).getString("name");
                            String expectedDefaultValue = jsonArr.getJSONObject(i).getString("defaultValue");

                            if (jsonObj.has(String.valueOf(expectedTagId))) {
                                JSONObject tagJsonObj = jsonObj.getJSONObject(String.valueOf(expectedTagId));

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
                    } else {
                        csAssert.assertFalse(true,
                                "Couldn't Update CDR with CT having Clause that contains Single/Multi Select Field Tag due to: " + updateResult);
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't Update Tag Values on CDR Wizard.");
                }
            } else {
                csAssert.assertFalse(true,
                        "Couldn't Create CDR with CT having Clause that contains Single/Multi Select Field Tag due to: " + cdrResult);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C141003. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C141018: Verify Single/Multi Select Tags will be visible on clause viewer based on Clause language
     */
    @Test(enabled = false)
    public void testC141018() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;

        try {
            logger.info("Starting Test TC-C141018: Verify Single/Multi Select Tags will be visible on Clause Viewer based on Clause Language");
            logger.info("Validating in Options API Response.");

            //Validate Negative Case. Tag should not come in Options API Response for Query Functions and Language Russian
            String optionsResponse = optionsHelperObj.hitOptionsAPIForTags("Functions", "1000");
            JSONObject jsonObj = new JSONObject(optionsResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i).getJSONObject("customData");

                if (jsonObj.has("apiName") && jsonObj.getString("apiName").equalsIgnoreCase("functions")) {
                    csAssert.assertFalse(true, "Functions Tag Visible in Options API Response for Query Functions and Language 1000");
                    break;
                }
            }

            UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, "c141018",
                    "clauseTags", "expected functions name", functionsInRussian);

            //Validate Positive Case. Tag should come in Options API Response for Query функции and Language Russian
            optionsResponse = optionsHelperObj.hitOptionsAPIForTags(functionsInRussian, "1000");
            jsonObj = new JSONObject(optionsResponse);
            jsonArr = jsonObj.getJSONArray("data");

            boolean tagFound = false;
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i).getJSONObject("customData");

                if (jsonObj.has("apiName") && jsonObj.getString("apiName").equalsIgnoreCase("functions")) {
                    tagFound = true;
                    break;
                }
            }

            csAssert.assertTrue(tagFound, "Functions Tag not found in Options API Response for Query функции and Language 1000");

            //Create Clause with Functions tag and validate value in ClausePageData API
            String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                    "c141018", false);

            UpdateFile.updateConfigFileProperty(configFilePath, extraFieldsConfigFileName, "c141018",
                    "clauseTags", functionsInRussian, "expected functions name");

            String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);

            if (status.equalsIgnoreCase("success")) {
                clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);

                clausePageDataObj.hitClausePageData(clauseId);
                String clausePageDataResponse = clausePageDataObj.getClausePageDataResponseStr();

                jsonObj = new JSONObject(clausePageDataResponse).getJSONArray("clauseTags").getJSONObject(0);
                String prefixMsg = "Multi-Select Field Functions Tag Validation failed in ClausePageData API Response. ";

                //Validate identifier
                csAssert.assertTrue(jsonObj.getString("identifier").equalsIgnoreCase("functions"),
                        prefixMsg + "Expected Identifier: Functions and Actual Identifier: " + jsonObj.getString("identifier"));

                //Validate ApiName
                jsonObj = jsonObj.getJSONObject("entityField");
                csAssert.assertTrue(jsonObj.getString("apiName").equalsIgnoreCase("functions"),
                        prefixMsg + "Expected APIName: Functions and Actual APIName: " + jsonObj.getString("apiName"));

                //Validate EntityTypeId
                csAssert.assertTrue(jsonObj.getJSONObject("entityType").getInt("id") == 160,
                        prefixMsg + "Expected EntityTypeId: 160 and Actual EntityTypeId: " + jsonObj.getJSONObject("entityType").getInt("id"));
            } else {
                csAssert.assertFalse(true, "Couldn't Create Clause due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C141018: " + e.getMessage());
        } finally {
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C141020: Verify Single/Multi Select Tags will be visible on template viewer based on CT Language
     */
    @Test(enabled = false)
    public void testC141020() {
        CustomAssert csAssert = new CustomAssert();
        int ctId = -1;

        try {
            logger.info("Starting Test TC-C141020: Verify Single/Multi Select Tags will be visible on Template Viewer based on CT Language");
            String createResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                    "c141020 template creation", false);
            String createStatus = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (createStatus.equalsIgnoreCase("success")) {
                ctId = CreateEntity.getNewEntityId(createResponse);
                String templatePageDataResponse = TemplatePageData.getTemplatePageDataResponse(ctId);

                JSONObject jsonObj = new JSONObject(templatePageDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("clauseTags");

                boolean tagFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);

                    if (jsonObj.has("entityField") && jsonObj.getJSONObject("entityField").has("apiName") &&
                            jsonObj.getJSONObject("entityField").getString("apiName").equalsIgnoreCase("Functions")) {
                        tagFound = true;

                        String actualTagName = jsonObj.getString("name");

                        if (!actualTagName.equalsIgnoreCase(functionsInRussian)) {
                            csAssert.assertTrue(false, "Expected Tag Name: " + functionsInRussian + " and Actual Tag Name: " + actualTagName +
                                    " in CT Template Viewer.");
                        }

                        break;
                    }
                }

                if (!tagFound) {
                    csAssert.assertTrue(false, "Functions Tag not found in Template Viewer of CT Id " + ctId);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create CT. Hence couldn't validate further steps.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C141020. " + e.getMessage());
        } finally {
            if (ctId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", ctId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C141019: Verify Single/Multi Select Tags will be visible on document viewer based on CDR Language
     */
    @Test(enabled = false)
    public void testC141019() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C141019: Verify Single/Multi Select Tags will be visible on document viewer based on CDR Language");

            String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                    "c141019 cdr creation", false);
            String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

            if (cdrResult.equalsIgnoreCase("success")) {
                cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

                logger.info("Hitting FindMappedTags API for CDR");
                String templateId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141019 cdr creation", "ctid");
                String findMappedTagsResponse = FindMappedTags.getFindMappedTagsResponse(cdrId, "[" + templateId + "]");

                JSONObject jsonObj = new JSONObject(findMappedTagsResponse).getJSONObject("data").getJSONObject("mappedTags")
                        .getJSONObject(templateId).getJSONObject("mappedTags");

                String[] objNames = JSONObject.getNames(jsonObj);

                boolean tagFound = false;
                for (String obj : objNames) {
                    JSONObject tagJsonObj = jsonObj.getJSONObject(obj).getJSONObject("entityField");

                    if (tagJsonObj.has("apiName") && tagJsonObj.getString("apiName").equalsIgnoreCase("functions")) {
                        tagFound = true;

                        String actualTagName = jsonObj.getJSONObject(obj).getString("name");
                        csAssert.assertTrue(actualTagName.equalsIgnoreCase(functionsInRussian), "FindMappedTags API validation failed. Expected Tag Name: " +
                                functionsInRussian + " and Actual Tag Name: " + actualTagName);

                        break;
                    }
                }

                csAssert.assertTrue(tagFound, "Functions Tag not found in FindMappedTags API for CDR.");
            } else {
                csAssert.assertFalse(true,
                        "Couldn't Create CDR with CT having Clause that contains Single/Multi Select Field Tag due to: " + cdrResult);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C141019. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }
}