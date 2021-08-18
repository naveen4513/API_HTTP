package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldLabel.MessagesList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.presignature.ClausePageData;
import com.sirionlabs.api.presignature.FindMappedContractTemplate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Clause;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestCDRAdditionalRequiredFields {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRAdditionalRequiredFields.class);

    private String configFilePath;
    private String configFileName;

    private Check checkObj = new Check();
    private AdminHelper adminHelperObj = new AdminHelper();
    private ListRendererConfigure configureObj = new ListRendererConfigure();
    private DefaultUserListMetadataHelper defaultHelperObj = new DefaultUserListMetadataHelper();
    private OptionsHelper optionsHelperObj = new OptionsHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestCDRAdditionalFieldsConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestCDRAdditionalFieldsConfigFileName");
    }

    private void loginWithEndUser() {
        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    /*
    TC-C4641: Verify fields are present in Quick Link of Client Admin and can be sorted.
     */
    @Test
    public void testC4641() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4641: Verify fields are present in Quick Link of Client Admin and can be sorted.");
            adminHelperObj.loginWithClientAdminUser();

            String[] expectedFieldQueryNames = {"contractinghubs", "contractingmarkets", "internalcontractingparties", "contractingcompanycodes", "hubs", "recpmarkets",
                    "companycodes", "spendtype", "createdby", "createdfor", "vendorcontractingparty", "customer"};

            configureObj.hitListRendererConfigure("279");
            String configureResponse = configureObj.getListRendererConfigureJsonStr();
            if (ParseJsonResponse.validJsonResponse(configureResponse)) {
                List<String> allColumnQuerNames = defaultHelperObj.getAllColumnQueryNames(configureResponse);

                for (String expectedFieldName : expectedFieldQueryNames) {
                    csAssert.assertTrue(allColumnQuerNames.contains(expectedFieldName), "Expected Field Query Name " + expectedFieldName +
                            " not found in Configure API Response.");
                }
            } else {
                csAssert.assertFalse(true, "Configure API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4641. " + e.getMessage());
        } finally {
            loginWithEndUser();
        }

        csAssert.assertAll();
    }

    /*
    TC-C4651: Verify Internationalization of CDR Fields.
     */
    @Test
    public void testC4651() {
        CustomAssert csAssert = new CustomAssert();
        UpdateAccount updateAccountObj = new UpdateAccount();
        String endUserLoginId = ConfigureEnvironment.getEndUserLoginId();
        int clientId = adminHelperObj.getClientId();

        try {
            logger.info("Starting Test TC-C4651: Verify Internationalization of CDR Fields");
            String[] allFields = {"Contracting Hub", "Contracting Market", "Internal Contracting Party", "Spend Type", "Created By", "Created For",
                    "Vendor Contracting Party", "Customer"};

            logger.info("Changing User Language to Russian.");
            updateAccountObj.updateUserLanguage(endUserLoginId, clientId, 1000);

            FieldRenaming fieldRenamingObj = new FieldRenaming();

            adminHelperObj.loginWithClientAdminUser();
            String fieldLabelResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 850);

            MessagesList messagesListObj = new MessagesList();
            String messagesListPayload = "[";

            Map<Integer, String> allFieldsMap = new HashMap<>();

            for (String field : allFields) {
                String groupName = getFieldGroupName(field);
                String fieldId = fieldRenamingObj.getFieldAttribute(fieldLabelResponse, field, groupName, "id");
                allFieldsMap.put(Integer.parseInt(fieldId), field);

                messagesListPayload = messagesListPayload.concat(fieldId) + ",";
            }

            messagesListPayload = messagesListPayload.substring(0, messagesListPayload.length() - 1) + "]";

            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);
            JSONObject jsonObj = new JSONObject(messagesListResponse);

            for (Map.Entry<Integer, String> entry : allFieldsMap.entrySet()) {
                String groupName = getFieldGroupName(entry.getValue());
                String expectedFieldName = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, groupName, entry.getValue());
                String actualFieldName = jsonObj.getJSONObject(entry.getKey().toString()).getString("name");

                matchLabels(expectedFieldName, actualFieldName, csAssert, "Internationalization Validation for Field " + entry.getValue());
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4651: " + e.getMessage());
        } finally {
            logger.info("Changing User Language back to English.");
            updateAccountObj.updateUserLanguage(endUserLoginId, clientId, 1);
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    private String getFieldGroupName(String fieldName) {
        if (fieldName.equalsIgnoreCase("CREATED BY")) {
            return "List Specific Columns";
        }

        return "Metadata";
    }

    private void matchLabels(String expectedFieldLabel, String actualFieldLabel, CustomAssert csAssert, String additionalInfo) {
        boolean matchLabels = StringUtils.matchRussianCharacters(expectedFieldLabel, actualFieldLabel);
        csAssert.assertTrue(matchLabels, "Expected " + expectedFieldLabel + " Label: " + expectedFieldLabel + " and Actual Label: " + actualFieldLabel + ". " +
                additionalInfo);
    }

    /*
    TC-C4649: Verify Fields are present as tag in Clause Create Page and can be used as tag.
     */
    @Test
    public void testC4649() {
        CustomAssert csAssert = new CustomAssert();
        int clauseId = -1;

        try {
            logger.info("Starting Test TC-C4649: Verify Fields are present as tag on Clause Create Page and can be used as tag.");

            String optionsResponse = optionsHelperObj.hitOptionsAPIForTags("Spend Type", "1");
            JSONObject jsonObj = new JSONObject(optionsResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("data");

            boolean tagFound = false;
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i).getJSONObject("customData");

                if (jsonObj.has("apiName") && jsonObj.getString("apiName").equalsIgnoreCase("spendType")) {
                    if (jsonObj.has("entityType") && jsonObj.getJSONObject("entityType").getInt("id") == 160) {
                        tagFound = true;
                        break;
                    }
                }
            }

            if (tagFound) {
                //Create Clause with Spend Type Tag.
                String clauseCreateResponse = Clause.createClause(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                        "c4649 clause creation", false);
                String status = ParseJsonResponse.getStatusFromResponse(clauseCreateResponse);

                if (status.equalsIgnoreCase("success")) {
                    clauseId = CreateEntity.getNewEntityId(clauseCreateResponse);

                    //Validate Tag in Clause Page Data Response.
                    ClausePageData clausePageDataObj = new ClausePageData();
                    clausePageDataObj.hitClausePageData(clauseId);
                    String clausePageDataResponse = clausePageDataObj.getClausePageDataResponseStr();

                    jsonObj = new JSONObject(clausePageDataResponse).getJSONArray("clauseTags").getJSONObject(0).getJSONObject("entityField");

                    String prefixMsg = "ClausePageData API Response Validation failed. ";
                    csAssert.assertEquals(jsonObj.getString("apiName"), "spendType", prefixMsg + "Expected APIName: spendType and Actual APIName: " +
                            jsonObj.getString("apiName"));

                    csAssert.assertEquals(jsonObj.getJSONObject("entityType").getInt("id"), 160, prefixMsg + "Expected Id: 160 and Actual Id: " +
                            jsonObj.getJSONObject("entityType").getInt("id"));
                } else {
                    csAssert.assertFalse(true, "Couldn't create Clause due to " + status);
                }
            } else {
                csAssert.assertFalse(true, "Spend Type Tag not found in Options API Response.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4649. " + e.getMessage());
        } finally {
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C4654: Verify Customer field Tag in CDR Wizard.
     */
    @Test
    public void testC4654() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C4654: Verify Customer field Tag in CDR Wizard.");

            String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                    "c4654 cdr creation", false);
            String status = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

            if (status.equalsIgnoreCase("success")) {
                cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

                logger.info("Adding Template to Newly Created CDR.");
                Edit editObj = new Edit();

                String editGetResponse = editObj.getEditPayload("contract draft request", cdrId);
                Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                        "c4654 cdr edit");

                String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

                JSONObject jsonObj = new JSONObject(editGetResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                Set<String> dataKeys = jsonObj.keySet();
                for(String key : dataKeys){
                    try{
                        if(!jsonObj.getJSONObject(key).isNull("options")){
                            jsonObj.getJSONObject(key).remove("options");
                        }
                    }catch(Exception e){
                        continue;
                    }
                }

                jsonObj.getJSONObject("mappedContractTemplates").put("values", new JSONObject(mappedTemplatePayload).getJSONArray("values"));
                String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
                String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
                String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

                if (updateResult.equalsIgnoreCase("success")) {
                    logger.info("Hitting FindMappedContractTemplate API for CDR");
                    String findMappedContractTemplateResponse = FindMappedContractTemplate.getFindMappedContractTemplateResponse(cdrId);

                    jsonObj = new JSONObject(findMappedContractTemplateResponse).getJSONObject("data").getJSONArray("mappedContractTemplates")
                            .getJSONObject(0).getJSONObject("mappedTags");

                    String[] objNames = JSONObject.getNames(jsonObj);
                    boolean tagFound = false;

                    for (String objName : objNames) {
                        JSONObject tagJsonObj = jsonObj.getJSONObject(objName);

                        if (tagJsonObj.has("entityField") && tagJsonObj.getJSONObject("entityField").getString("apiName").equalsIgnoreCase("customer")) {
                            tagFound = true;

                            String actualDefaultValue = tagJsonObj.get("defaultValue").toString();
                            csAssert.assertTrue(actualDefaultValue.equalsIgnoreCase("Apple Inc"),
                                    "FindMappedContractTemplate API Validation failed. Expected Default Value: Apple Inc and Actual Value: " + actualDefaultValue);
                        }
                    }

                    csAssert.assertTrue(tagFound, "Couldn't find Customer Tag in FindMappedContractTemplate API Response.");
                } else {
                    csAssert.assertFalse(true,
                            "Couldn't Update CDR with CT having Clause that contains Single/Multi Select Field Tag due to: " + updateResult);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create CDR due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4654. " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }
}