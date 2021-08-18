package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestPreSignatureSanity {

    private final static Logger logger = LoggerFactory.getLogger(TestPreSignatureSanity.class);
    private static String configFilePath;
    private static String configFileName;
    private static String contractCreationConfigFilePath;
    private static String contractCreationConfigFileName;
    private Check check = new Check();

    @BeforeClass
    public void before() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFileName");
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureContractCreationConfigFilePath");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureContractCreationConfigFileName");
    }

    @Test
    public void testEndToEndPreSignatureFlow() {

        logger.info("This test case starts with creating clause, definition , activating them, creating contract template" +
                "with newly created clause and template, creating contract draft request, attaching contract template to CDR");

        logger.info("E2E Pre-Signature Test Started");

        SoftAssert softAssert = new SoftAssert();
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        int clauseId = -1;
        int definitionId = -1;
        int contractTemplateId = -1;
        int cdrId = -1;
        int contractId = -1;
        try {
            // Create new Clause with tags
            String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
            clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
            logger.info("Newly Created Clause Id " + clauseId);

            if (clauseId == -1) {
                throw new SkipException("Error in Creating Clause");
            }

            // Get text for which tag has to be created

            String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
            String[] wordTags = text.split(" ");
            int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
            String wordToTag = wordTags[word];

            //Creating the new tag
            String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
            HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
            softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
            JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
            int newlyCreatedOrExistingTagId = 0;
            if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"),
                        "Tag is not created successfully");
                newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
            } else {
                if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                    softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                    .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                            "Tag is not already present");
                    HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                    softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                    JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                    int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                    for (int i = 0; i < tagsLength; i++) {
                        if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                            newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                        }
                    }
                }
            }

            // Updating clause with tag
            String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
            String[] htmlWordBag = htmlText.split(" ");
            String htmlToTag = htmlWordBag[word];

            // Replacing html with tagged html
            String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                    " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId +
                    ":</span>" + htmlToTag + "<span style=\"display:none\">}</span></span></span>";
            htmlWordBag[word] = updateTag;

            //Altering html text with newly created tag
            StringBuffer sb = new StringBuffer();
            for (String s : htmlWordBag) {
                sb.append(s + " ");
            }

            // Hitting Edit Get API to get Clause text and Clause tag to update
            HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
            softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
            JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
            // Get the json object needed to update to link newly created tag with clause
            // Updating clause tag
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
            JSONObject tagValues = new JSONObject();
            tagValues.put("id", newlyCreatedOrExistingTagId);
            tagValues.put("name", newlyCreatedOrExistingTagId);
            JSONObject tagHtmlType = new JSONObject();
            tagHtmlType.put("id", 1);
            tagHtmlType.put("name", "Text Field");
            tagValues.put("tagHTMLType", tagHtmlType);
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
            // Updating Clause text
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

            // Edit Clause with tags
            String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
            softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
            JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
            softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                    "Tag is not updated with clause");

            // Activate Newly Created Clause
            // Send for client review Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);
            // Approve Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);
            // Publish Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);

            /////////////// Definition Creation ////////////////////
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "definition fields", 1);
            String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                    "definition fields", false);
            definitionId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
            logger.info("Newly Created Definition Id " + definitionId);

            // Get Created definition
            HttpResponse getCreatedDefinitionResponse = PreSignatureHelper.getClause(definitionId);
            softAssert.assertTrue(getCreatedDefinitionResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created definition");
            JSONObject createdDefinitionJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedDefinitionResponse);

            // Activate Newly Created Definition
            // Send for client review Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);
            // Approve Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);
            // Publish Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);

            ///////////// Contract Template Creation////////////////
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates", "contract template fields", 0);

            // Setting values for clauses to be selected while creating contract template
            JSONObject clauseCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "fields", "category"));
            JSONObject definitionCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "definition fields", "definitionCategory"));
            int clauseFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
            String selectClausePayload = "{\"name\": \"clauses\",\"id\": " + clauseFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": " +
                    "{\"name\": \"" + clauseCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(clauseCategory
                    .getJSONObject("values").get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data")
                    .getJSONObject("name").get("values") + "\",\"id\": " + clauseId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 1}," +
                    "\"order\": 1,\"mandatory\": null},{\"clauseCategory\": {\"name\": \"" + definitionCategory.getJSONObject("values").get("name").toString() +
                    "\",\"id\": \"" + Integer.valueOf(definitionCategory.getJSONObject("values").get("id").toString()) + "\"},\"clause\": {\"name\": \"" +
                    createdDefinitionJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values") + "\",\"id\": " + definitionId +
                    "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 2},\"order\": 2,\"mandatory\": null}]}";

            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, "contract template fields", "clauses", selectClausePayload);

            String contractTemplateResponseString = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                    "contract template fields", false);
            contractTemplateId = PreSignatureHelper.getNewlyCreatedId(contractTemplateResponseString);
            // Get newly created contract template response
            HttpResponse contractTemplateResponse = PreSignatureHelper.getContractTemplateResponse(contractTemplateId);
            JSONObject contractTemplateJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateResponse);

            int templateTypeId = Integer.parseInt(contractTemplateJson.getJSONObject("body").getJSONObject("data")
                    .getJSONObject("templateType").getJSONObject("values").get("id").toString());
            String contractTemplateName = contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            String contractTemplateDocument = contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("uploadDocument")
                    .getJSONObject("values").get("name").toString();

            // Create new Contract Draft Request
            String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                    "contract draft request fields", false);
            cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

            // Attaching Creating Template to Created Contract Draft Request
            // Get the Created CDR edit page response
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

            JSONArray attachTemplateArray = new JSONArray("[{\"id\":\"" + contractTemplateId + "\",\"name\":\"" + contractTemplateName +
                    "\",\"hasChildren\":\"false\",\"templateTypeId\":\"" + templateTypeId + "\",\"checked\":1,\"mappedContractTemplates\":null," +
                    "\"uniqueIdentifier\":\"186899071638312\",\"$$hashKey\":\"object:1366\",\"mappedTags\":{\"" + newlyCreatedOrExistingTagId +
                    "\":{\"name\":\"" + htmlToTag + "\",\"id\":" + newlyCreatedOrExistingTagId + ",\"identifier\":\"" + htmlToTag +
                    "\",\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1},\"orderSeq\":100,\"tagTypeId\":2,\"$$hashKey\":\"object:1371\"}}}]");

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").put("values", attachTemplateArray);

            String contractDraftRequestAttachTemplatePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            // Edit contract draft request with template
            HttpResponse contractDraftRequestUpdateResponse = PreSignatureHelper.editContractDraftRequest(contractDraftRequestAttachTemplatePayload);
            JSONObject contractDraftRequestUpdateJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestUpdateResponse);
            softAssert.assertTrue(contractDraftRequestUpdateJson.getJSONObject("header").getJSONObject("response").get("status").toString().trim().equals("success"),
                    "Updating the CDR with template is not successful");

            // Verify Contract Template Section in Contract Draft Request Page
            HttpResponse getContractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestResponse(cdrId);
            softAssert.assertTrue(getContractDraftRequestResponse.getStatusLine().getStatusCode() == 200,
                    "Error in fetching created Contract Draft Request Response");
            JSONObject getContractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(getContractDraftRequestResponse);

            // Find Mapped Contract Template API to verify which contract template is linked with CDR
            HttpResponse findMappedCTAPIResponse = PreSignatureHelper.findMappedContractTemplate(cdrId);
            softAssert.assertTrue(findMappedCTAPIResponse.getStatusLine().getStatusCode() == 200, "Find Mapped CT API Response Code is not valid");
            JSONObject findMappedCTAPIJson = PreSignatureHelper.getJsonObjectForResponse(findMappedCTAPIResponse);
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).get("name").toString().trim().equals(contractTemplateName.trim()), "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data")
                            .getJSONArray("mappedContractTemplates").getJSONObject(0).get("id").toString().trim()) == contractTemplateId,
                    "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                            .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("name").toString().trim().equals(wordToTag.trim()),
                    "Created Tag Name is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("id").toString().trim())
                    == newlyCreatedOrExistingTagId, "Created Tag Id is not Linked to Contract Template Linked to CDR");

            // Edit Contract Template from CDR show page
            // Update Tag
            String updatedTagText = RandomString.getRandomAlphaNumericString(5);
            String updateTagPayload = "[{\"id\":" + newlyCreatedOrExistingTagId + ",\"name\":\"" + wordToTag + "\",\"defaultValue\":\"" + updatedTagText + "\"}]";

            HttpResponse updateTagResponse = PreSignatureHelper.updateTagValue(updateTagPayload);

            softAssert.assertTrue(updateTagResponse.getStatusLine().getStatusCode() == 200, "Updated Tag Response Code is not Valid");

            JSONObject updateTagJson = PreSignatureHelper.getJsonObjectForResponse(updateTagResponse);
            softAssert.assertTrue(updateTagJson.get("success").toString().equals("true"), "Tag Update is not successful");
            softAssert.assertTrue(updateTagJson.get("errors").toString().equals("null"), "Tag Update is not successful");
            // Link updated tag value to CDR
            getContractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").getJSONArray("values")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).put("defaultValue", updatedTagText);
            String editCDRWithTagPayload = "{\"body\":{\"data\":" + getContractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            HttpResponse editCDRWithTagResponse = PreSignatureHelper.editContractDraftRequest(editCDRWithTagPayload);
            softAssert.assertTrue(editCDRWithTagResponse.getStatusLine().getStatusCode() == 200, "CDR is not updated with tag value = " + updatedTagText);
            JSONObject editCDRWithTagJson = PreSignatureHelper.getJsonObjectForResponse(editCDRWithTagResponse);
            softAssert.assertTrue(editCDRWithTagJson.getJSONObject("header").getJSONObject("response").get("status").toString().trim().equals("success"),
                    "CDR is updated with new tag value");

            // Find Mapped Contract Template API
            findMappedCTAPIResponse = PreSignatureHelper.findMappedContractTemplate(cdrId);
            softAssert.assertTrue(findMappedCTAPIResponse.getStatusLine().getStatusCode() == 200, "Find Mapped CT API Response Code is not valid");
            findMappedCTAPIJson = PreSignatureHelper.getJsonObjectForResponse(findMappedCTAPIResponse);
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .get("name").toString().trim().equals(contractTemplateName.trim()), "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).get("id").toString().trim()) == contractTemplateId, "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                            .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("name").toString().trim().equals(wordToTag.trim()),
                    "Created Tag Name is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("id").toString().trim())
                    == newlyCreatedOrExistingTagId, "Created Tag Id is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("defaultValue").toString().trim()
                    .equals(updatedTagText.trim()), "Not able to see mapped contract template with CDR");

            // Validate Contract Template in Contract Document tab of CDR
            HttpResponse defaultUserListMetaDataResponse = PreSignatureHelper.defaultUserListMetaDataAPI("367", 160, "{}");
            softAssert.assertTrue(defaultUserListMetaDataResponse.getStatusLine().getStatusCode() == 200,
                    "Default User List Meta Data API Response Code is not valid");
            JSONObject defaultUserListMetaDataJson = PreSignatureHelper.getJsonObjectForResponse(defaultUserListMetaDataResponse);
            List<Integer> columnIds = PreSignatureHelper.getDefaultColumns(defaultUserListMetaDataJson.getJSONArray("columns"));

            String tabListDataPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse tabListDataResponse = PreSignatureHelper.tabListDataAPI("367", "160", cdrId, tabListDataPayload);
            softAssert.assertTrue(tabListDataResponse.getStatusLine().getStatusCode() == 200, "Tab List Data API Response is not valid");
            JSONObject tabListDataJson = PreSignatureHelper.getJsonObjectForResponse(tabListDataResponse);

            String documentName = null;
            String documentStatus = null;
            String documentId = null;
            String contractDocumentTemplateTypeId = null;

            for (int columnId : columnIds) {
                if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("documentname")) {
                    documentName = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("documentstatus")) {
                    documentStatus = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName").toString().equals("id")) {
                    documentId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("template_type_id")) {
                    contractDocumentTemplateTypeId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId))
                            .get("value").toString();
                }
            }

            softAssert.assertTrue(documentName.split(":;")[1].trim().equals(contractTemplateName.trim()),
                    "Contract Template document is not getting reflected in CDR contract document tab");
            softAssert.assertTrue(documentStatus.split(":;")[0].trim().equals("1"),
                    "In CDR contract document tab, Contract Template document is not in draft status");

            // Edit Contract Template to final status from Contract Document tab in CDR
            contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentStatusChangeJsonArray = new JSONArray("[{\"documentFileId\": \"" + documentStatus.split(":;")[1].trim() +
                    "\",\"editable\": true,\"shareWithSupplierFlag\": false,\"documentStatus\": {\"id\": \"2\",\"name\": \"\"}}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments")
                    .put("values", contractDocumentStatusChangeJsonArray);
            String contractDraftRequestContractDocumentStatusChangePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body")
                    .getJSONObject("data").toString() + "}}";

            HttpResponse contractDraftRequestContractDocumentStatusChangeResponse =
                    PreSignatureHelper.editContractDraftRequest(contractDraftRequestContractDocumentStatusChangePayload);
            softAssert.assertTrue(contractDraftRequestContractDocumentStatusChangeResponse.getStatusLine().getStatusCode() == 200,
                    "Response Code for Contract Document Status Change in CDR is not valid");

            // Download the Document from Contract Document tab
            Boolean isContractDocumentDownloaded = PreSignatureHelper.getTemplateFromContractDocumentTabCDR(System.getProperty("user.dir") +
                            "\\src\\test\\resources\\TestConfig\\PreSignature\\Files", contractTemplateDocument, documentId, "78", "160",
                    documentStatus.split(":;")[1].trim());
            softAssert.assertTrue(isContractDocumentDownloaded, "Contract Template from Contract Document Tab in CDR is not Downloaded");

            // Upload Document Draft and Change status to executed
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\PreSignature\\Files" + "\\" +
                    contractTemplateDocument);
            String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraft(contractTemplateDocument.split("\\.")[0],
                    contractTemplateDocument.split("\\.")[1], randomKeyForFileUpload, "160", String.valueOf(cdrId),
                    String.valueOf(documentId), fileToUpload);
            JSONObject fileUploadDraftJson = new JSONObject(fileUploadDraftResponse);

            softAssert.assertTrue(fileUploadDraftJson.get("documentFileId").toString().trim().equals(documentStatus.split(":;")[1].trim()),
                    "Document File id is not as expected");
            softAssert.assertTrue(fileUploadDraftJson.get("templateTypeId").toString().trim().equals(contractDocumentTemplateTypeId.trim()),
                    "Document template type id is not as expected");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(0).get("name").toString().equals("Final"),
                    "Document initial status is not final");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(0).get("id").toString().equals("2"),
                    "Document initial status is not final");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(1).get("name").toString().equals("Executed"),
                    "Document status is not Executed");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(1).get("id").toString().equals("3"),
                    "Document status is not Executed");
            // Submit Document Draft
            contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[{\"templateTypeId\":" + contractDocumentTemplateTypeId + ",\"documentFileId\":" +
                    documentStatus.split(":;")[1].trim() + ",\"key\":\"" + randomKeyForFileUpload + "\"," +
                    "\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"documentStatusId\":3,\"performanceData\":false," +
                    "\"searchable\":false,\"shareWithSupplierFlag\":false,\"documentId\":" + documentId + "}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values",
                    contractDocumentChangeStatusJsonArray);
            String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
            softAssert.assertTrue(submitDraftResponse.getStatusLine().getStatusCode() == 200, "Submit draft API Response is not valid");

            String sourceEntityId = "{\"name\":\"sourceEntityId\",\"values\":" + cdrId + ",\"multiEntitySupport\":false}";
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract from cdr", "sourceEntityId",
                    sourceEntityId);

            // Contract Creation from CDR
            String contractCreateSection = "contract from cdr";
            ParseConfigFile.updateValueInConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, contractCreateSection,
                    "sourceid", String.valueOf(cdrId));

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(contractCreationConfigFilePath, contractCreationConfigFileName,
                    contractCreateSection);
            String[] parentSupplierIdsArr = flowProperties.get("supplierids").split(",");
            String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) +
                    ",\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + flowProperties.get("sourceid") + "],\"entityTypeId\":160}," +
                    "\"actualParentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}}";

            String createContractResponse = createContractFromCDRResponse(payload, contractCreateSection);

            if (createContractResponse == null) {
                softAssert.assertFalse(true, "Contract Create API Response is null.");
            }

            contractId = PreSignatureHelper.getNewlyCreatedId(createContractResponse);
            JSONObject createContractJson = new JSONObject(createContractResponse);
            softAssert.assertTrue(createContractJson.getJSONObject("header").getJSONObject("response").get("status").toString().trim().equals("success"),
                    "Contract is not created from CDR successfully");

            // Created Contract Response to get Contract Name
            HttpResponse createdContractResponse = PreSignatureHelper.showCreatedContract(contractId);
            softAssert.assertTrue(createdContractResponse.getStatusLine().getStatusCode() == 200, "Created Contract Show API Response is not valid");
            JSONObject createdContractJson = PreSignatureHelper.getJsonObjectForResponse(createdContractResponse);
            String createdContractName = createdContractJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString().trim();

            // Verify Related Document tab of CDR to verify contract has been created
            HttpResponse relatedDocumentMetaDataTabResponse = PreSignatureHelper.defaultUserListMetaDataAPI("377", 160, "{}");
            softAssert.assertTrue(relatedDocumentMetaDataTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Meta Data Tab List Data API Response is not valid");
            JSONObject relatedDocumentMetaDataTabJson = PreSignatureHelper.getJsonObjectForResponse(relatedDocumentMetaDataTabResponse);
            List<Integer> relatedContractsDefaultColumns = PreSignatureHelper.getDefaultColumns(relatedDocumentMetaDataTabJson.getJSONArray("columns"));

            String relatedContractTabPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse relatedDocumentTabResponse = PreSignatureHelper.tabListDataAPI("377", "160", cdrId, relatedContractTabPayload);
            softAssert.assertTrue(relatedDocumentTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Tab List API Response is not valid");
            JSONObject relatedDocumentTabJson = PreSignatureHelper.getJsonObjectForResponse(relatedDocumentTabResponse);
            softAssert.assertTrue(Integer.parseInt(relatedDocumentTabJson.get("filteredCount").toString()) == 1,
                    "Related Contracts is not reflecting in list data");

            HashMap<String, String> relatedContractData = new HashMap<>();
            for (int relatedContractsDefaultColumn : relatedContractsDefaultColumns) {
                relatedContractData.put(relatedDocumentTabJson.getJSONArray("data").getJSONObject(0)
                                .getJSONObject(String.valueOf(relatedContractsDefaultColumn)).get("columnName").toString(),
                        relatedDocumentTabJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(relatedContractsDefaultColumn))
                                .get("value").toString());
            }

            softAssert.assertTrue(relatedContractData.get("contract_id").split(":;")[1].trim().equals(String.valueOf(contractId)),
                    "Created Contract is not reflecting on related document tab in contract draft request");
            softAssert.assertTrue(relatedContractData.get("linkedentitytype").trim().equals("Contracts"),
                    "Created Contract is not reflecting on related document tab in contract draft request");

            // Get all task id before moving contract to tree
            List<Integer> allTaskIdsBeforeSubmittingRequestToMoveToTree = PreSignatureHelper.getAllTaskIds();
            // Removing all task ids before moving contract to tree
            UserTasksHelper.removeAllTasks();

            // Move to Tree from Created Contract
            String moveToTreePayload = "{\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61,\"sourceEntityTypeId\":160,\"sourceEntityId\":" +
                    cdrId + ",\"entityTypeId\":61,\"entityId\":" + contractId + ",\"auditLogDocTreeFlowDocs\":[{\"auditLogDocFileId\":\"" +
                    documentName.split(":;")[documentName.split(":;").length - 1].trim() + "\"}],\"sourceTabId\":2,\"statusId\":1}";
            HttpResponse moveToTreeResponse = PreSignatureHelper.moveToTree(moveToTreePayload);
            softAssert.assertTrue(moveToTreeResponse.getStatusLine().getStatusCode() == 200, "Move To Tree Response is not Valid");
            JSONObject moveToTreeJson = PreSignatureHelper.getJsonObjectForResponse(moveToTreeResponse);
            softAssert.assertTrue(moveToTreeJson.toString().contains("Your request has been successfully submitted"), "Move to be tree request is not successful");

            // Get new task id
            int newTaskIdForMovingDocToContractTree = PreSignatureHelper.getNewTaskId(allTaskIdsBeforeSubmittingRequestToMoveToTree);
            // New task created for move to tree
            Map<String, String> docMovementToContractTreeJob;
            docMovementToContractTreeJob = UserTasksHelper.waitForScheduler(Long.parseLong("10000"), Long.parseLong("100"), newTaskIdForMovingDocToContractTree);

            softAssert.assertTrue(docMovementToContractTreeJob.get("jobPassed").equals("true"), "Job for move to tree is not successful");
            softAssert.assertTrue(docMovementToContractTreeJob.get("errorMessage") == null, "Error while scheduler is executing move to tree job. " +
                    docMovementToContractTreeJob.get("errorMessage"));

            // Verify Document in Contract Document tab of contract after move to tree
            HttpResponse contractDocumentMetaDataTabResponse = PreSignatureHelper.defaultUserListMetaDataAPI("366", 61, "{}");
            softAssert.assertTrue(contractDocumentMetaDataTabResponse.getStatusLine().getStatusCode() == 200,
                    "Contract Document Meta Data Tab List Data API Response is not valid");
            JSONObject contractDocumentMetaDataTabJson = PreSignatureHelper.getJsonObjectForResponse(contractDocumentMetaDataTabResponse);
            List<Integer> contractDocumentTabDefaultColumns = PreSignatureHelper.getDefaultColumns(contractDocumentMetaDataTabJson.getJSONArray("columns"));

            String contractDocumentTabPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse contractDocumentTabResponse = PreSignatureHelper.tabListDataAPI("366", "61", contractId, contractDocumentTabPayload);
            softAssert.assertTrue(relatedDocumentTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Tab List API Response is not valid");
            JSONObject contractDocumentTabJson = PreSignatureHelper.getJsonObjectForResponse(contractDocumentTabResponse);
            softAssert.assertTrue(Integer.parseInt(contractDocumentTabJson.get("filteredCount").toString()) == 1,
                    "Related Contracts is not reflecting in list data");

            HashMap<String, String> contractDocumentData = new HashMap<>();
            for (int contractDocumentTabDefaultColumn : contractDocumentTabDefaultColumns) {
                contractDocumentData.put(contractDocumentTabJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(contractDocumentTabDefaultColumn))
                        .get("columnName").toString(), contractDocumentTabJson.getJSONArray("data").getJSONObject(0)
                        .getJSONObject(String.valueOf(contractDocumentTabDefaultColumn)).get("value").toString());
            }
            softAssert.assertTrue(contractDocumentData.get("documentname").split(":;")[1].trim().equals(documentName.split(":;")[1].trim()),
                    "Created Contract is not reflecting on related document tab in contract draft request");

            HttpResponse documentOnTreeResponse = PreSignatureHelper.verifyDocumentOnTree(contractId, "{}");
            softAssert.assertTrue(documentOnTreeResponse.getStatusLine().getStatusCode() == 200, "Document On Tree API Response is not valid");
            JSONObject documentOnTreeJson = PreSignatureHelper.getJsonObjectForResponse(documentOnTreeResponse);

            boolean isDocumentOnTree = false;
            int documentOnTreeChildrenLength = documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").length();
            for (int i = 0; i < documentOnTreeChildrenLength; i++) {
                if (documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").getJSONObject(i).get("text").toString().trim()
                        .equals(createdContractName)) {
                    isDocumentOnTree = true;
                    softAssert.assertTrue(Integer.parseInt(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children")
                            .getJSONObject(i).get("numberOfChild").toString().trim()) == 1, "Document is not reflecting into tree");
                    break;
                }
            }
            softAssert.assertTrue(isDocumentOnTree, "Document should be in tree but it doesn't exist");
            logger.info("E2E Pre-Signature Test Completed");
        } catch (Exception e) {
            softAssert.assertFalse(true, "Exception in Pre Sig E2E. " + e.getMessage());
        } finally {
            //Delete all new created data.
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }

            if (definitionId != -1) {
                EntityOperationsHelper.deleteEntityRecord("definition", definitionId);
            }

            if (contractTemplateId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", contractTemplateId);
            }

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        }

        softAssert.assertAll();
    }

    private String createContractFromCDRResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, configFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        configFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }
}