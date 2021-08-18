package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.documentFlow.MoveToTreeSave;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestContractFromCDR {
    private final static Logger logger = LoggerFactory.getLogger(TestContractFromCDR.class);
    private String configFilePath;
    private String configFileName;
    private String entityTypeId;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFilePath;
    private String cdrExtraFieldsConfigFileName;
    private Map<String, String> defaultProperties;
    private HashMap<String, Integer> deleteEntities = new HashMap<>();
    private String cdr = "contract draft request";
    private String contracts = "contracts";

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFileName");
        defaultProperties = ParseConfigFile.getAllProperties(cdrConfigFilePath, cdrConfigFileName);

        cdrExtraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        cdrExtraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");
    }

    //@Test
    public void testC153261() {
        CustomAssert customAssert = new CustomAssert();
        int cdrId = -1;
        int contractId = -1;
        List<Integer> templateIds = new ArrayList<>();
        boolean editStatus = false;
        try {
            cdrId = createCDR(customAssert);
            boolean addTemplate = addTheTemplateToCDR(cdrId, cdr, chooseTemplate(customAssert), customAssert);
            if (addTemplate) {
                List<HashMap<String, Integer>> contractDocumentListData = getContractDocumentListData(cdrId, cdr, customAssert);
                for (HashMap<String, Integer> contractDocumentData : contractDocumentListData) {
                    templateIds.add(contractDocumentData.get("Id"));
                    editStatus = editContractDocumentStatus(cdrId, cdr, contractDocumentData, customAssert);

                    if (!editStatus)
                        break;
                }
                if (editStatus) {
                    List<String> templateOnCDR = getShowPageTemplates(cdrId, cdr, customAssert);
                    contractId = createContractFromCDR(cdrId, cdr, templateIds, customAssert);
                    List<String> templateOnContract = getShowPageTemplates(contractId, contracts, customAssert);
                    customAssert.assertTrue(templateOnCDR.equals(templateOnContract), "At Creation Level,TC - C153261 failed.");
                    if (templateOnCDR.equals(templateOnContract)) {
                        boolean moveToTreeStatus = moveToTree(contractId, contracts, templateIds, customAssert);
                        customAssert.assertTrue(moveToTreeStatus, "At Move to Tree Level,TC - C153261 failed.");
                    }
                } else {
                    logger.error("Status of document could not be changed. So not proceeding further.");
                    customAssert.assertTrue(false, "Status of document could not be changed. So not proceeding further.");
                }
            } else {
                logger.error("Template could not be added.");
                customAssert.assertTrue(false, "Template could not be added.");
            }

        } catch (Exception e) {
            logger.error("Exception {} occurred while testing TC - C153261.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while testing TC - C153261.");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(cdr, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
            EntityOperationsHelper.deleteEntityRecord(contracts, contractId);
            logger.info("Contract with entityId id {} is deleted.", contractId);
        }
        customAssert.assertAll();
    }

    //To be edited
    @Test(enabled = false)
    public void testC153260() {
        CustomAssert customAssert = new CustomAssert();
        int cdrId = -1;
        int contractId = -1;
        String templateShowId = "";
        try {
            cdrId = createCDR(customAssert);
            HashMap<String, Boolean> templateLayout = getShowPageTemplatesLayout(cdrId, cdr, customAssert);
            if (templateLayout.get("Multiple") & !templateLayout.get("Editable")) {
                if (getShowPageTemplates(cdrId, cdr, customAssert).isEmpty()) {
                    if (addTheTemplateToCDR(cdrId, cdr, chooseTemplate(customAssert), customAssert)) {
                        List<String> templateDetails = getShowPageTemplates(cdrId, cdr, customAssert);
                        if (!templateDetails.isEmpty()) {
                            for (String template : templateDetails) {
                                templateShowId = template.split(" ")[template.split(" ").length - 1].replaceAll("[^a-zA-Z0-9]", "");
                            }
                        } else {
                            logger.error("Contract Documents tab is empty whereas it should have the added document.");
                            customAssert.assertTrue(false, "Contract Documents tab is empty whereas it should have the added document.");
                        }
                    } else {
                        logger.error("Template could not be added to the CDR.");
                        customAssert.assertTrue(false, "Template could not be added to the CDR.");
                    }
                } else {
                    logger.error("CDR has some contract documents whereas it should have been empty.");
                    customAssert.assertTrue(false, "CDR has some contract documents whereas it should have been empty.");
                }

            } else {
                logger.error("Templates field is either not multiselect or editable or both.");
                customAssert.assertTrue(false, "Templates field is either not multiselect or editable or both.");
            }

        } catch (Exception e) {
            logger.error("Exception {} occurred while automating TC-C153260.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while automating TC-C153260.");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(cdr, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
            EntityOperationsHelper.deleteEntityRecord(contracts, contractId);
            logger.info("Contract with entityId id {} is deleted.", contractId);
        }
        customAssert.assertAll();
    }

    //@Test
    public void testC153232() {
        CustomAssert customAssert = new CustomAssert();
        int cdrId = -1;
        try {
            cdrId = createCDR(customAssert);
            if (addTheTemplateToCDR(cdrId, cdr, chooseTemplate(customAssert), customAssert)) {
                Thread.sleep(120000);
                List<String> columnsName = getContractClauseTabColumns(cdrId, cdr, getContractDocumentListData(cdrId, cdr, customAssert), customAssert);
                customAssert.assertTrue(columnsName.contains("companyposition"), "TC-C153232 failed as column \"Company Postion\" does not exist in Contract Clause tab.");
            } else {
                logger.error("Could not add the template to the CDR.");
                customAssert.assertTrue(false, "Could not add the template to the CDR.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while automating TC-C153232.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while automating TC-C153232.");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(cdr, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152660() {
        CustomAssert customAssert = new CustomAssert();
        List<Integer> templateIds = new ArrayList<>();
        int cdrId = -1;
        int contractId = -1;
        try {
            cdrId = createCDR(customAssert);
            HashMap<String, String> showPageDataCDR = getShowPageNames(cdrId, cdr, customAssert);
            String cdrName = showPageDataCDR.get("Title");
            String cdrShortCode = showPageDataCDR.get("ShortCodeId");
            if (addTheTemplateToCDR(cdrId, cdr, chooseTemplate(customAssert), customAssert)) {
                List<HashMap<String, Integer>> contractDocumentListData = getContractDocumentListData(cdrId, cdr, customAssert);
                for (HashMap<String, Integer> contractDocumentData : contractDocumentListData) {
                    templateIds.add(contractDocumentData.get("Id"));
                }
                contractId = createContractFromCDR(cdrId, cdr, templateIds, customAssert);
                HashMap<String, String> showPageDataContract = getShowPageNames(contractId,contracts,customAssert);
                String sourceTitle = showPageDataContract.get("SourceTitle");
                customAssert.assertTrue(sourceTitle.contains(cdrName)&sourceTitle.contains(cdrShortCode),"TC-C152660");
            } else {
                logger.error("Template could not be added to the CDR.");
                customAssert.assertTrue(false, "Template could not be added to the CDR.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while automating TC-C152660", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while automating TC-C152660");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(cdr, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
            EntityOperationsHelper.deleteEntityRecord(contracts, contractId);
            logger.info("Contract with entityId id {} is deleted.", contractId);
        }
        customAssert.assertAll();
    }

    private int createCDR(CustomAssert customAssert) {
        int entityId = -1;
        String createResponse;
        try {
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrExtraFieldsConfigFilePath, cdrExtraFieldsConfigFileName, "cdr creation", true);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObject = new JSONObject(createResponse);
                String createStatus = jsonObject.getJSONObject("header").getJSONObject("response").getString("status").trim();
                if (createStatus.equalsIgnoreCase("success")) {
                    entityId = CreateEntity.getNewEntityId(createResponse, "contract draft request");
                    logger.info("Id of the Entity Created is : {}", entityId);
                } else {
                    logger.error("CDR creation is unsuccessful.");
                    customAssert.assertTrue(false, "CDR creation is unsuccessful.");
                }
            } else {
                logger.error("Create response of CDR is not a valid JSON Response");
                customAssert.assertTrue(false, "Create response of CDR is not a valid JSON Response");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while creating the CDR", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while creating the CDR");
        }
        return entityId;
    }

    private HashMap<String, String> chooseTemplate(CustomAssert customAssert) {
        HashMap<String, String> templateDetails = new HashMap<>();
        try {
//Choose Template
            String templatePayload = "{\"filterMap\":{\"entityTypeId\":140,\"offset\":20,\"isApprovedTemplates\":true,\"size\":20,\"orderByColumnName\":\"name\",\"orderDirection\":\"asc\",\"filterJson\":{\"101\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1004\",\"name\":\"Non Competition Agreement\"}]},\"filterId\":101,\"filterName\":\"agreementType\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"266\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1005\",\"name\":\"Main Template\"}]},\"filterId\":266,\"filterName\":\"templatetype\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}";
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2WithOutParams("364", templatePayload);
            String selectTemplateResponse = listRendererListData.getListDataJsonStr();
//Select template
            JSONObject templateJSON = null;
            JSONArray data = new JSONObject(selectTemplateResponse).getJSONArray("data");
            Set<String> dataKeys = data.getJSONObject(0).keySet();
            String dataKey = null;
            for (String newKey : dataKeys) {
                if (data.getJSONObject(0).getJSONObject(newKey).getString("columnName").equalsIgnoreCase("name")) {
                    dataKey = newKey;
                    break;
                }
            }
            for (int index = 0; index < data.length(); index++) {
                if (data.getJSONObject(index).getJSONObject(dataKey).getString("value").equalsIgnoreCase("AUTOMATION PURPOSE FOR DEVIATION FEATURE (DO NOT USE)")) {
                    templateJSON = data.getJSONObject(index);
                    break;
                }
            }
            Set<String> keys = templateJSON.keySet();
            for (String key : keys) {
                if (templateJSON.getJSONObject(key).getString("columnName").equalsIgnoreCase("id")) {
                    templateDetails.put("Id", templateJSON.getJSONObject(key).getString("value"));
                } else if (templateJSON.getJSONObject(key).getString("columnName").equalsIgnoreCase("name")) {
                    templateDetails.put("Name", templateJSON.getJSONObject(key).getString("value"));
                } else if (templateJSON.getJSONObject(key).getString("columnName").equalsIgnoreCase("hasChildren")) {
                    templateDetails.put("HasChildren", templateJSON.getJSONObject(key).getString("value"));
                } else if (templateJSON.getJSONObject(key).getString("columnName").equalsIgnoreCase("templateTypeId")) {
                    templateDetails.put("TemplateTypeId", templateJSON.getJSONObject(key).getString("value"));
                }
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while selecting the template.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while selecting the template.");
        }
        return templateDetails;
    }

    private boolean addTheTemplateToCDR(int entityId, String entityName, HashMap<String, String> templateDetails, CustomAssert customAssert) {
        Edit edit = new Edit();
        String editGetResponse;
        boolean status = false;
        try {
            editGetResponse = edit.getEditPayload(entityName, entityId);
            String tempId = templateDetails.get("Id");
            String tempName = templateDetails.get("Name");
            String tempHasChildren = templateDetails.get("HasChildren");
            String tempTemplateTypeId = templateDetails.get("TemplateTypeId");

            String mappedContractTemplatesValue = "{\"id\":\"" + tempId + "\",\"name\":\"" + tempName + "\",\"hasChildren\":\"" + tempHasChildren + "\",\"templateTypeId\":\"" + tempTemplateTypeId + "\",\"checked\":1,\"mappedContractTemplates\":null,\"uniqueIdentifier\":\"" + tempId + "78028499879\",\"$$hashKey\":\"object:442\",\"tagsCount\":1,\"mappedTags\":{\"2584\":{\"name\":\"are\",\"id\":2584,\"identifier\":\"are\",\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1},\"orderSeq\":1,\"tagTypeId\":2,\"$$hashKey\":\"object:459\"}}}";

            if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
                JSONObject editGetJSON = new JSONObject(editGetResponse);
                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").append("values", new JSONObject(mappedContractTemplatesValue));
                String editPostPayload = editGetJSON.toString();
                try {
                    String editPostResponse = edit.hitEdit(entityName, editPostPayload);
                    if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                        if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                            status = true;
                        } else {
                            logger.error("POST EDIT API is not successful");
                            customAssert.assertTrue(false, "POST EDIT API is not successful");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception {} occurred while hitting POST Edit API", e.getMessage());
                    customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while hitting POST Edit API");
                }
            } else {
                logger.error("Get response of Edit API is not a valid JSON");
                customAssert.assertTrue(false, "Get response of Edit API is not a valid JSON");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while editing the Entity.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while editing the Entity.");
        }
        return status;
    }

    private List<HashMap<String, Integer>> getContractDocumentListData(int entityId, String entityName, CustomAssert customAssert) {
        TabListData tabListData = new TabListData();
        List<HashMap<String, Integer>> contractDocuments = new ArrayList<>();
        int tabId = 367;
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        try {
            String contractDocumentsListData = tabListData.hitTabListDataV2(tabId, Integer.parseInt(entityTypeId), entityId);
            if (ParseJsonResponse.validJsonResponse(contractDocumentsListData)) {
                JSONArray dataArray = new JSONObject(contractDocumentsListData).getJSONArray("data");
                for (int index = 0; index < dataArray.length(); index++) {
                    HashMap<String, Integer> contractDocumentDetails = new HashMap<>();
                    Set<String> keys = dataArray.getJSONObject(index).keySet();
                    for (String key : keys)
                        if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentname")) {
                            int id = Integer.parseInt(dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[4]);
                            contractDocumentDetails.put("Id", id);
                        } else if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("template_type_id")) {
                            int templateTypeId = Integer.parseInt(dataArray.getJSONObject(index).getJSONObject(key).get("value").toString());
                            contractDocumentDetails.put("DocumentTemplateTypeId", templateTypeId);
                        }
                    contractDocuments.add(contractDocumentDetails);
                }
            } else {
                logger.error("Response of Contract Documents Tab API is not a valid JSON");
                customAssert.assertTrue(false, "Response of Contract Documents Tab API is not a valid JSON");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching the list of Contract Documents.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching the list of Contract Documents.");
        }
        return contractDocuments;
    }

    private boolean editContractDocumentStatus(int entityId, String entityName, HashMap<String, Integer> templateDetail, CustomAssert customAssert) {
        boolean statusChange = false;
        Edit edit = new Edit();
        int documentId = templateDetail.get("Id");
        int templateTypeId = templateDetail.get("DocumentTemplateTypeId");
        String finalStatusPayload = "[{\"shareWithSupplierFlag\":false,\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":" + templateTypeId + ",\"documentStatus\":{\"id\":2,\"name\":\"Final\"},\"documentFileId\":" + documentId + "}]";

        try {
            String editGetPayload = edit.getEditPayload(entityName, entityId);
            if (ParseJsonResponse.validJsonResponse(editGetPayload)) {
                JSONObject editGetJSON = new JSONObject(editGetPayload);
                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("documentEditable").put("values", true);

                editGetJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", new JSONArray(finalStatusPayload));

                String editPostPayload = editGetJSON.toString();
                String editPostResponse = edit.hitEdit(entityName, editPostPayload);
                if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
                    if (new JSONObject(editPostResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                        statusChange = true;
                    }
                } else {
                    logger.error("Edit POST Response is not a valid JSON.");
                    customAssert.assertTrue(false, "Edit POST Response is not a valid JSON.");
                }
            } else {
                logger.error("Edit GET API response is not a valid JSON.");
                customAssert.assertTrue(false, "Edit GET API response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while changing the document status.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while changing the document status.");
        }
        return statusChange;
    }

    private int createContractFromCDR(int cdrID, String parentEntity, List<Integer> templateIds, CustomAssert customAssert) {
        int contractID = -1;
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, parentEntity, "entity_type_id");
        try {
            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"actualParentEntity\":{\"entityIds\":[],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + cdrID + "],\"entityTypeId\":160}}";
            JSONObject newPayloadJSON = new JSONObject(newPayload);

            ContractFreeCreate contractFreeCreate = new ContractFreeCreate();
            contractFreeCreate.hitContractFreeCreate(cdrID, 160, 4);
            String freeCreateResponse = contractFreeCreate.getFreeCreateJsonStr();

            if (ParseJsonResponse.validJsonResponse(freeCreateResponse)) {
                JSONObject freeCreateJSON = new JSONObject(freeCreateResponse);
                JSONArray dataArray = freeCreateJSON.getJSONObject("body").getJSONObject("data").getJSONObject("supplier").getJSONObject("options").getJSONArray("data");
                for (int index = 0; index < dataArray.length(); index++) {
                    int sourceId = Integer.parseInt(dataArray.getJSONObject(index).get("id").toString());
                    newPayloadJSON.getJSONObject("parentEntity").append("entityIds", sourceId);
                    newPayloadJSON.getJSONObject("actualParentEntity").append("entityIds", sourceId);
                }
                newPayload = newPayloadJSON.toString();
                New newObj = new New();
                newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
                String newResponse = newObj.getNewJsonStr();
                if (ParseJsonResponse.validJsonResponse(newResponse)) {
                    JSONObject newJSON = new JSONObject(newResponse);
                    if (newJSON.getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                        newJSON.remove("header");
                        newJSON.remove("session");
                        newJSON.remove("actions");
                        newJSON.remove("createLinks");
                        newJSON.getJSONObject("body").remove("layoutInfo");
                        newJSON.getJSONObject("body").remove("globalData");
                        newJSON.getJSONObject("body").remove("errors");

                        Set<String> keys = newJSON.getJSONObject("body").getJSONObject("data").keySet();
                        String options = null;
                        for (String key : keys) {
                            try {
                                if (!newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).isNull("options")) {
                                    newJSON.getJSONObject("body").getJSONObject("data").getJSONObject(key).put("options", options);
                                }
                            } catch (Exception e) {
                                continue;
                            }
                        }

                        JSONObject dynamicMetaData = newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                        Set<String> metaKeys = dynamicMetaData.keySet();
                        for (String metaKey : metaKeys) {
                            try {
                                dynamicMetaData.getJSONObject(metaKey).put("options", options);
                                newJSON.getJSONObject("body").put(metaKey, dynamicMetaData.getJSONObject(metaKey));
                            } catch (Exception e) {
                                newJSON.getJSONObject("body").put(metaKey, dynamicMetaData.getString(metaKey));
                            }
                        }

                        for (int templateId : templateIds) {
                            String auditLogDoc = "{\"auditLogDocFileId\":\"" + templateId + "\"}";
                            newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").append("values", new JSONObject(auditLogDoc));
                        }

                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values", "AUTOMATION PURPOSE (PLEASE DO NOT USE)");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("title").put("values", "AUTOMATION PURPOSE (PLEASE DO NOT USE)");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("deliveryCountries").append("values", new JSONObject("{\"name\":\"India\",\"id\":111}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("tier").put("values", new JSONObject("{\"name\":\"Tier - 2\",\"id\":1007}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").put("values", "10-01-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("expirationDate").put("values", "10-31-2020");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("currency").put("values", new JSONObject("{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\",\"additionalOption\":true}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("contractCurrencies").append("values", new JSONObject("{\"name\":\"Indian Rupee (INR)\",\"id\":8,\"shortName\":\"INR\",\"additionalOption\":true}"));
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardFromDate").put("values", "11-01-2018");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardToDate").put("values", "11-30-2018");
                        newJSON.getJSONObject("body").getJSONObject("data").getJSONObject("rateCardsApplicable").put("values", new JSONObject("{\"name\":\"automation conv factor 3\",\"id\":1017}"));
                    } else {
                        logger.error("New API hit is unsuccessful.");
                        customAssert.assertTrue(false, "New API hit is unsuccessful.");
                    }

                    String createPayload = newJSON.toString();
                    if (ParseJsonResponse.validJsonResponse(createPayload)) {
                        Create create = new Create();
                        create.hitCreate("contracts", createPayload);
                        String createResponse = create.getCreateJsonStr();
                        if (ParseJsonResponse.validJsonResponse(createResponse)) {
                            JSONObject createJSON = new JSONObject(createResponse).getJSONObject("header").getJSONObject("response");
                            if (createJSON.getString("status").equalsIgnoreCase("success")) {
                                contractID = Integer.parseInt(createJSON.get("entityId").toString());
                                deleteEntities.put("contracts", contractID);
                            } else {
                                logger.error("Contract creation is unsuccessful.");
                                customAssert.assertTrue(false, "Contract creation is unsuccessful.");
                            }
                        } else {
                            logger.error("MSA Create Response is not a valid JSON");
                            customAssert.assertTrue(false, "MSA Create Response is not a valid JSON");
                        }
                    } else {
                        logger.error("Payload created for Create API is not a valid JSON");
                        customAssert.assertTrue(false, "Payload created for Create API is not a valid JSON");
                    }
                } else {
                    logger.error("New API Response for MSA is not a valid JSON");
                    customAssert.assertTrue(false, "New API Response for MSA is not a valid JSON");
                }
            } else {
                logger.error("Contract Free Create response is not a valid JSON");
                customAssert.assertTrue(false, "Contract Free Create response is not a valid JSON");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while creating MSA from CDR", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while creating MSA from CDR");
        }
        return contractID;
    }

    private List<String> getShowPageTemplates(int entityId, String entityName, CustomAssert customAssert) {
        List<String> showPageTemplates = new ArrayList<>();
        Show show = new Show();
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        try {
            show.hitShow(Integer.parseInt(entityTypeId), entityId);
            String showPageResponse = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showPageResponse)) {
                JSONObject showPageJSON = new JSONObject(showPageResponse);
                JSONArray templatesValue = showPageJSON.getJSONObject("body").getJSONObject("data").getJSONObject("templates").getJSONArray("values");
                for (int index = 0; index < templatesValue.length(); index++) {
                    showPageTemplates.add(templatesValue.getJSONObject(index).getString("name"));
                }
            } else {
                logger.error("Show Page Response is not a valid JSON.");
                customAssert.assertTrue(false, "Show Page Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching show page details of {}.", e.getMessage(), entityName);
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching show page details of " + entityName + ".");
        }
        return showPageTemplates;
    }

    private HashMap<String, Boolean> getShowPageTemplatesLayout(int entityId, String entityName, CustomAssert customAssert) {
        Show show = new Show();
        HashMap<String, Boolean> templateLayoutComponent = new HashMap<>();
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        try {
            show.hitShow(Integer.parseInt(entityTypeId), entityId);
            String showPageResponse = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showPageResponse)) {
                JSONArray layoutFieldsArray = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");
                for (int index = 0; index < layoutFieldsArray.length(); index++) {
                    if (layoutFieldsArray.getJSONObject(index).getString("label").equalsIgnoreCase("General")) {
                        JSONArray generalFieldsArray = layoutFieldsArray.getJSONObject(index).getJSONArray("fields");
                        for (int fIndex = 0; fIndex < generalFieldsArray.length(); fIndex++) {
                            if (generalFieldsArray.getJSONObject(fIndex).getString("label").equalsIgnoreCase("Basic Information")) {
                                JSONArray biFieldArray = generalFieldsArray.getJSONObject(fIndex).getJSONArray("fields");
                                for (int i = 0; i < biFieldArray.length(); i++) {
                                    try {
                                        if (biFieldArray.getJSONObject(i).getString("name").equalsIgnoreCase("templates")) {
                                            if (biFieldArray.getJSONObject(i).getString("displayMode").equalsIgnoreCase("display")) {
                                                templateLayoutComponent.put("Editable", false);
                                            } else if (biFieldArray.getJSONObject(i).getString("displayMode").equalsIgnoreCase("editable")) {
                                                templateLayoutComponent.put("Editable", true);
                                            }

                                            if (biFieldArray.getJSONObject(i).getJSONObject("properties").get("multiple").toString().equalsIgnoreCase("true")) {
                                                templateLayoutComponent.put("Multiple", true);
                                            } else if (biFieldArray.getJSONObject(i).getJSONObject("properties").get("multiple").toString().equalsIgnoreCase("false")) {
                                                templateLayoutComponent.put("Multiple", false);
                                            }
                                        }
                                    } catch (Exception e) {
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                logger.error("Show Page Response is not a valid JSON.");
                customAssert.assertTrue(false, "Show Page Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching show page layout.", e.getMessage());
            customAssert.assertTrue(false, "Exception {} occurred while fetching show page layout.");
        }
        return templateLayoutComponent;
    }

    private boolean moveToTree(int entityId, String entityName, List<Integer> templateIds, CustomAssert customAssert) {
        MoveToTreeSave moveToTreeSave = new MoveToTreeSave();
        boolean moveToTreeFlag = false;
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        int typeId = Integer.parseInt(entityTypeId);
        try {
            String moveToTreePayload = "{\"baseEntityId\":" + entityId + ",\"baseEntityTypeId\":" + typeId + ",\"sourceEntityTypeId\":" + typeId + ",\"sourceEntityId\":" + entityId + ",\"entityTypeId\":" + typeId + ",\"entityId\":" + entityId + ",\"auditLogDocTreeFlowDocs\":[],\"sourceTabId\":2,\"statusId\":1}";
            JSONObject moveToTreeJSON = new JSONObject(moveToTreePayload);
            for (int templateId : templateIds) {
                moveToTreeJSON.append("auditLogDocTreeFlowDocs", new JSONObject("{\"auditLogDocFileId\":\"" + templateId + "\"}"));
            }

            moveToTreePayload = moveToTreeJSON.toString();

            moveToTreeSave.hitMoveToTreeSave(moveToTreePayload);
            String moveToTreeResponse = moveToTreeSave.getMoveToTreeSaveJsonStr();
            if (ParseJsonResponse.validJsonResponse(moveToTreeResponse)) {
                moveToTreeFlag = new JSONObject(moveToTreeResponse).getBoolean("success");
            } else {
                logger.error("Move To Tree Response");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while performing the action \"Move To Tree\".", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while performing the action \"Move To Tree\".");
        }
        return moveToTreeFlag;
    }

    private List<String> getContractClauseTabColumns(int entityId, String entityName, List<HashMap<String, Integer>> contractDocuments, CustomAssert customAssert) {
        List<String> columnsName = new ArrayList<>();
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        String listId = "492";
        try {
            ListRendererListData listRendererListData = new ListRendererListData();
            for (HashMap<String, Integer> contractDocument : contractDocuments) {
                int documentFieldId = contractDocument.get("Id");
                String payload = "{\"filterMap\":{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"entityTypeId\":" + Integer.parseInt(entityTypeId) + ",\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":null,\"documentFileId\":\"" + documentFieldId + "\",\"entityId\":" + entityId + "}},\"filterJson\":{}}}";
                listRendererListData.hitListRendererListDataV2WithOutParams(listId, payload);
                String listResponse = listRendererListData.getListDataJsonStr();
                if (ParseJsonResponse.validJsonResponse(listResponse)) {
                    JSONArray dataArray = new JSONObject(listResponse).getJSONArray("data");
                    for (int index = 0; index < dataArray.length(); index++) {
                        for (String key : dataArray.getJSONObject(index).keySet()) {
                            columnsName.add(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").toLowerCase());
                        }
                    }
                } else {
                    logger.error("Listing Response is not a valid JSON.");
                    customAssert.assertTrue(false, "Listing Response is not a valid JSON.");
                }
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching the list of the columns under Contract Clauses Tab.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching the list of the columns under Contract Clauses Tab.");
        }
        return columnsName;
    }

    private HashMap<String, String> getShowPageNames(int entityId, String entityName, CustomAssert customAssert) {
        HashMap<String, String> showPageData = new HashMap<>();
        Show show = new Show();
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");
        try {
            show.hitShow(Integer.parseInt(entityTypeId), entityId);
            String showPageResponse = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showPageResponse)) {
                JSONObject showPageDataJSON = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");

                if (entityName.equalsIgnoreCase(cdr)) {
                    showPageData.put("Title", showPageDataJSON.getJSONObject("title").getString("values"));
                    showPageData.put("ShortCodeId", showPageDataJSON.getJSONObject("shortCodeId").getString("values"));
                } else if (entityName.equalsIgnoreCase(contracts)) {
                    showPageData.put("SourceTitle", showPageDataJSON.getJSONObject("sourceTitle").getJSONObject("values").getString("name"));
                }
            } else {
                logger.error("Show Page Response is not a valid JSON.");
                customAssert.assertTrue(false, "Show Page Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching show page details of {}.", e.getMessage(), entityName);
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching show page details of " + entityName + ".");
        }
        return showPageData;
    }
}