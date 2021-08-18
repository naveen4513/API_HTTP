package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestTextAreaTypeTags extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestTextAreaTypeTags.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestTextAreaTypeTagPath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestTextAreaTypeTagName");
        extraFieldsConfigFileName = "TestTagExtraFields.cfg";
    }

    /*
    TC-C88424: Verify Character limit for Custom Created Text Area Type Tag.
     */
    @Test
    public void testC88424() {
        CustomAssert csAssert = new CustomAssert();

        try {
            Map<String, String> properties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "c88424");
            String tagId = properties.get("tagId");
            String tagName = properties.get("tagName");

            //Update Tag Value with less than 2048 chars.
            String updatedTagValue = "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit Test Char limit " +
                    "Test Char limit Test Char limit Test Char limit";
            String tagUpdatePayload = "[{\"id\":" + tagId + ",\"name\":\"" + tagName + "\",\"defaultValue\":\"" + updatedTagValue + "\"}]";

            String response = EntityUtils.toString(PreSignatureHelper.updateTagValue(tagUpdatePayload).getEntity());
            JSONObject jsonObj = new JSONObject(response).getJSONObject("response").getJSONObject(tagId);
            boolean isErrorNull = jsonObj.isNull("validationErrorMessages");

            csAssert.assertEquals(isErrorNull, true, "Tag Value Update failed when less than 2048 chars used.");

            //Update Tag Value with more than 2048 chars
            updatedTagValue = updatedTagValue.concat("Test Char limit Test Char limit Test Char limit");
            tagUpdatePayload = "[{\"id\":" + tagId + ",\"name\":\"" + tagName + "\",\"defaultValue\":\"" + updatedTagValue + "\"}]";

            response = EntityUtils.toString(PreSignatureHelper.updateTagValue(tagUpdatePayload).getEntity());
            jsonObj = new JSONObject(response).getJSONObject("response").getJSONObject(tagId);
            isErrorNull = jsonObj.isNull("validationErrorMessages");

            if (!isErrorNull) {
                //Verify error message
                JSONArray jsonArr = jsonObj.getJSONArray("validationErrorMessages");
                boolean errorMatched = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonArr.getJSONObject(i).getString("errorMessage").toLowerCase().contains("exceeding maximum length of 2048")) {
                        errorMatched = true;
                        break;
                    }
                }

                csAssert.assertEquals(errorMatched, true,
                        "Tag Value Updated Validation failed when more than 2048 chars used. Couldn't find error message [Exceeding maximum length of 2048]");
            } else {
                csAssert.assertFalse(true, "Tag Value Update failed when more than 2048 chars used.");
            }

            //Update Tag Value with Original Value
            updatedTagValue = properties.get("originalTagValue");
            tagUpdatePayload = "[{\"id\":" + tagId + ",\"name\":\"" + tagName + "\",\"defaultValue\":\"" + updatedTagValue + "\"}]";

            PreSignatureHelper.updateTagValue(tagUpdatePayload);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88424: " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C88427: Verify when document is downloaded and tag values are updated.
     */
    @Test
    public void testC88427() {
        CustomAssert csAssert = new CustomAssert();
        int newCdrId = -1;

        try {
            logger.info("Starting Test TC-C88427: Verify Tag Value Validation when document is downloaded and then uploaded with updated value.");
            String response = executor.get("/tblcdr/create/rest?version=2.0", ApiHeaders.getDefaultLegacyHeaders()).getResponse().getResponseBody();

            if (ParseJsonResponse.validJsonResponse(response)) {
                String createSection = "c88427 creation";
                String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, createSection,
                        true);

                if (ParseJsonResponse.validJsonResponse(createResponse) && ParseJsonResponse.getStatusFromResponse(createResponse).equalsIgnoreCase("success")) {
                    newCdrId = CreateEntity.getNewEntityId(createResponse);

                    Edit editObj = new Edit();

                    String editGetResponse = editObj.getEditPayload("contract draft request", newCdrId);
                    Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                            "c88427 edit payload");

                    String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

                    JSONObject jsonObj = new JSONObject(editGetResponse);
                    jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                    jsonObj.getJSONObject("mappedContractTemplates").put("values", new JSONObject(mappedTemplatePayload).getJSONArray("values"));
                    String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
                    String updateResponse = editObj.hitEdit("contract draft request", updatePayload);

                    if (ParseJsonResponse.getStatusFromResponse(updateResponse).equalsIgnoreCase("success")) {
                        String tabListResponse = TabListDataHelper.getTabListDataResponse(160, newCdrId, 367);
                        String documentNameId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");
                        String documentNameValue = new JSONObject(tabListResponse).getJSONArray("data")
                                .getJSONObject(0).getJSONObject(documentNameId).getString("value");
                        String expectedTemplateName = editProperties.get("expectedTemplateName");

                        if (!documentNameValue.contains(expectedTemplateName + ":;")) {
                            csAssert.assertFalse(true, "Document having name " + expectedTemplateName + " not found in Contract Document Tab of CDR Id " +
                                    newCdrId);
                        } else {
                            FileUtils.copyFile(configFilePath, "Tag Doc.docx", configFilePath, expectedTemplateName + ".docx");

                            //Upload doc with updated tag value (exceeding 2048 chars)
                            String docId = documentNameValue.split(":;")[0];
                            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
                            FileUploadDraft fileUploadDraft = new FileUploadDraft();
                            Map<String, String> queryParameters = setPostParams(expectedTemplateName + ".docx", newCdrId, docId, randomKeyForFileUpload);

                            fileUploadDraft.hitFileUpload(configFilePath, expectedTemplateName + ".docx", queryParameters);

                            String payload = getPayloadForSubmitDraft(newCdrId, docId, randomKeyForFileUpload);
                            SubmitDraft submitDraft = new SubmitDraft();
                            submitDraft.hitSubmitDraft(payload);
                            String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

                            if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                                tabListResponse = TabListDataHelper.getTabListDataResponse(160, newCdrId, 367);
                                int noOfDocs = new JSONObject(tabListResponse).getJSONArray("data").length();

                                csAssert.assertEquals(noOfDocs, 2, "Document Upload with Updated Tag Value failed.");
                            } else {
                                csAssert.assertFalse(true, "Document Submit Draft with Updated Tag Value failed.");
                            }

                            FileUtils.deleteFile(configFilePath, expectedTemplateName + ".docx");
                        }
                    } else {
                        csAssert.assertFalse(true, "CDR Update failed. Hence couldn't validate further.");
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't create CDR.");
                }
            } else {
                csAssert.assertFalse(true, "CDR Create/New API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88427: " + e.getMessage());
        } finally {
            if (newCdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", newCdrId);
            }
        }

        csAssert.assertAll();
    }

    private Map<String, String> setPostParams(String templateName, int entityId, String docId, String randomKeyForFileUpload) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", templateName.split("\\.")[0]);
        map.put("extension", "docx");
        map.put("entityTypeId", String.valueOf(160));
        map.put("entityId", String.valueOf(entityId));
        map.put("key", randomKeyForFileUpload);
        map.put("DocumentId", docId);

        return map;
    }

    private String getPayloadForSubmitDraft(int cdrId, String documentFileId, String documentKey) {
        String payload = null;

        try {
            Show show = new Show();
            show.hitShow(160, cdrId);
            String showPageResponse = show.getShowJsonStr();

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1002,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":1,\"permissions\":{\"financial\":false," +
                        "\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false," + "\"shareWithSupplierFlag\":false}]}";

                commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                jsonData.put("comment", commentJsonObj);

                JSONObject finalPayload = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("data", jsonData);
                finalPayload.put("body", body);

                payload = finalPayload.toString();
            }

        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return payload;
    }
}