package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.commonAPI.DeviationSummary;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.http.HttpResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class TestCDRDeviations {
    private final static Logger logger = LoggerFactory.getLogger(TestCDRDeviations.class);

    private String configFilePath;
    private String configFileName;
    private String entityURLId;
    private String entityTypeId;
    private String entityName = "contract draft request";

    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFilePath;
    private String cdrExtraFieldsConfigFileName;
    private Map<String, String> defaultProperties;

    @BeforeClass
    public void beforeClass() throws Exception {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id");
        entityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_type_id");

        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierCDRCreationFileName");
        defaultProperties = ParseConfigFile.getAllProperties(cdrConfigFilePath, cdrConfigFileName);

        cdrExtraFieldsConfigFilePath = defaultProperties.get("extrafieldsconfigfilepath");
        cdrExtraFieldsConfigFileName = defaultProperties.get("extrafieldsconfigfilename");
    }

    @Test
    public void testC90183() {
        CustomAssert customAssert = new CustomAssert();
        String displayMode = getDisplayMode(Integer.parseInt(entityURLId), customAssert).get("DisplayMode");
        if (displayMode.equalsIgnoreCase("display")) {
            customAssert.assertTrue(true, "Total Deviations Field is non-editale.");
            logger.info("Total Deviations Field is non-editable.");
        } else if (displayMode.equalsIgnoreCase("editable")) {
            customAssert.assertTrue(false, "Total Deviations Field is editable.");
            logger.error("Total Deviations Field is editable.");
        }
        customAssert.assertAll();
    }

    @Test
    public void testC90212() {
        CustomAssert customAssert = new CustomAssert();
        if (downloadCDRListing(Integer.parseInt(entityURLId), customAssert)) {
            logger.info("Column - Total Deviations exists in the downloaded sheet");
            customAssert.assertTrue(true, "Column - Total Deviations exists in the downloaded sheet");
        } else {
            logger.error("Column - Total Deviations does not exist in the downloaded sheet");
            customAssert.assertTrue(true, "Column - Total Deviations does not exist in the downloaded sheet");
        }
        customAssert.assertAll();
    }

    @Test
    public void testC90217() {
        CustomAssert customAssert = new CustomAssert();
        int entityId = -1;
        boolean flag = false;
        try {
            //CDR Creation
            entityId = createCDR(customAssert);
            //Fetch Template details
            HashMap<String, String> templateDetails = chooseTemplate(customAssert);
            //Add Template
            if (addTheTemplateToCDR(entityId, templateDetails, customAssert)) {
                Thread.sleep(120000);
//Get Template Listing
                List<HashMap<String, String>> contractDocuments = getContractDocumentListData(entityId, customAssert);
                int clausesCount = getClauseCountBeforeDeviation(entityId, contractDocuments, customAssert);
//Download new template document
                HashMap<String, String> contractDocument = downloadTemplate(contractDocuments.get(0), customAssert);
                if (contractDocument.get("DownloadStatus").equalsIgnoreCase("Downloaded")) {
//Upload the updated doc
                    flag = uploadTheDocument(contractDocument, entityId, false, customAssert);
                    Thread.sleep(120000);
                    if (flag) {
//Get Show page response
                        Show show = new Show();
                        show.hitShow(Integer.parseInt(entityTypeId), entityId, true);
                        String showPageResponse = show.getShowJsonStr();
                        if (ParseJsonResponse.validJsonResponse(showPageResponse)) {
                            JSONObject showJSON = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                            double deviationPercent = Double.parseDouble(showJSON.getJSONObject("deviationPercentage").getJSONObject("values").get("deviationPercent").toString());
                            double deviation = Double.parseDouble(showJSON.getJSONObject("deviationPercentage").getJSONObject("values").get("deviation").toString());
                            if (deviationPercent == (deviation / clausesCount) * 100) {
                                logger.info("Deviation percentage is correct as per the formula.");
                                String actionName = "SendForClientReview";
                                if (performActionOnCDR(entityId, actionName, customAssert)) {
                                    logger.info("CDR has been sent for Review.");
                                    HashMap<String, HashMap<String, String>> baseAndLatestDocs = getContractDocumentIds(entityId, customAssert);
                                    HashMap<String, String> latestDocs = baseAndLatestDocs.get("LatestDocs");
                                    HashMap<String, String> baseDocs = baseAndLatestDocs.get("BaseDocs");

                                    double deviationOnContractClauseTab = 0.0;
                                    for (String clusterId : latestDocs.keySet()) {
                                        deviationOnContractClauseTab = deviationOnContractClauseTab + getDeviationSummary(entityId, latestDocs.get(clusterId), customAssert);
                                    }
                                    if (deviationOnContractClauseTab == deviation) {
                                        for (String clusterId : latestDocs.keySet()) {
                                            List<String> contentControlIds = getClauseToReview(entityId, latestDocs.get(clusterId), customAssert);
                                            for (String contentControlId : contentControlIds) {
                                                HashMap<String, List<String>> comparedText = compareClauseText(contentControlId, baseDocs.get(clusterId), latestDocs.get(clusterId), clusterId, customAssert);
                                                for (String str : comparedText.keySet()) {
                                                    String comparisonText = "";
                                                    for (String text : comparedText.get(str)) {
                                                        comparisonText = text + " " + comparisonText;
                                                    }
                                                    logger.info("Different text found in {} is : {}", str, comparisonText.trim());
                                                }
                                            }
                                        }
                                    } else {
                                        logger.error("TC-C90216 is failed.");
                                        customAssert.assertTrue(false, "TC-C90216 is failed.");
                                    }
                                } else {
                                    logger.error("Could not perform Send For Review.");
                                    customAssert.assertTrue(false, "Could not perform Send For Review.");
                                }
                            } else {
                                logger.error("Deviation percentage is incorrect as per the formula.");
                                customAssert.assertTrue(false, "Deviation percentage is incorrect as per the formula.");
                            }
                        } else {
                            logger.error("Show page Response after Uploading the Documents is not a valid JSON.");
                            customAssert.assertTrue(false, "Show page Response after Uploading the Documents is not a valid JSON.");
                        }
                    }
                } else {
                    logger.error("Document is not updated. Hence no deviation will be shown for document {}", contractDocument.get("DocumentName"));
                    customAssert.assertTrue(false, "Document is not updated. Hence no deviation will be shown for document " + contractDocument.get("DocumentName"));
                }
            } else {
                logger.error("Template is not added in the CDR");
                customAssert.assertTrue(false, "Template is not added in the CDR");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while verifying the percentage calculation of deviation", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while verifying the percentage calculation of deviation");
        } finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, entityId);
            logger.info("CDR with entityId id {} is deleted.", entityId);
        }
        customAssert.assertAll();
    }

    @Test
    public void testC152740() {
        CustomAssert customAssert = new CustomAssert();
        int cdrId = -1;
        try {
            cdrId = createCDR(customAssert);
            if (addTheTemplateToCDR(cdrId, chooseTemplate(customAssert), customAssert)) {
//                Thread.sleep(120000);
                HashMap<String, String> contractDocument = downloadTemplate(getContractDocumentListData(cdrId, customAssert).get(0), customAssert);
                if (contractDocument.get("DownloadStatus").equalsIgnoreCase("Downloaded")){
                    if(uploadTheDocument(contractDocument, cdrId, false, customAssert)){
                        String actionName = "SendForClientReview";
                        if (performActionOnCDR(cdrId, actionName, customAssert)){
                            List<HashMap<String, String>> contractDocumentListData = getContractDocumentListData(cdrId, customAssert);
                            for(HashMap<String, String> contractDocumentList : contractDocumentListData){
                                try{
                                    if(contractDocumentList.get("DocumentLockedStatus").contains("true")){
                                        logger.info("TC-C152740 is passed.");
                                        //Add review task
                                    }else{
                                        logger.error("Document Lock Status is not changed.");
                                        customAssert.assertTrue(false,"Document Lock Status is not changed.");
                                    }
                                    HashMap<String, HashMap<String, String>> baseAndLatestDocs = getContractDocumentIds(cdrId, customAssert);
                                    HashMap<String, String> latestDocs = baseAndLatestDocs.get("LatestDocs");
                                    for (String clusterId : latestDocs.keySet()) {
                                        List<String> contentControlIds = getClauseToReview(cdrId, latestDocs.get(clusterId), customAssert);
                                        for (String contentControlId : contentControlIds){

                                        }
                                    }

                                }catch (Exception e){
                                    continue;
                                }
                            }
                        }else{
                            logger.error("Could not perform the action \"{}\".",actionName);
                            customAssert.assertTrue(false,"Could not perform the action \""+actionName+"\".");
                        }
                    }else{
                        logger.error("Template with changes could not be uploaded to the CDR.");
                        customAssert.assertTrue(false,"Template with changes could not be uploaded to the CDR.");
                    }
                }else{
                    logger.error("Template with changes could not be downloaded.");
                    customAssert.assertTrue(false,"Template with changes could not be downloaded.");
                }
            } else {
                logger.error("Template could not be added to the CDR.");
                customAssert.assertTrue(false, "Template could not be added to the CDR.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while automating TC-C152740.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while automating TC-C152740.");
        }/*finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
        }*/
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
                    /*               deleteEntities.put("contract draft request", entityId);*/
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

    private boolean addTheTemplateToCDR(int entityId, HashMap<String, String> templateDetails, CustomAssert customAssert) {
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

    private List<HashMap<String, String>> getContractDocumentListData(int entityId, CustomAssert customAssert) {
        TabListData tabListData = new TabListData();
        List<HashMap<String, String>> contractDocuments = new ArrayList<>();
        int tabId = 367;
        try {
            String contractDocumentsListData = tabListData.hitTabListDataV2(tabId, Integer.parseInt(entityTypeId), entityId);
            if (ParseJsonResponse.validJsonResponse(contractDocumentsListData)) {
                JSONArray dataArray = new JSONObject(contractDocumentsListData).getJSONArray("data");
                for (int index = 0; index < dataArray.length(); index++) {
                    HashMap<String, String> contractDocumentDetails = new HashMap<>();
                    Set<String> keys = dataArray.getJSONObject(index).keySet();
                    for (String key : keys)
                        if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentname")) {
                            String documentId = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[0];
                            String documentName = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[1];
                            String documentExtension = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[2];
                            String id = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[4];
                            contractDocumentDetails.put("DocumentId", documentId);
                            contractDocumentDetails.put("DocumentName", documentName);
                            contractDocumentDetails.put("DocumentExtension", documentExtension);
                            contractDocumentDetails.put("Id", id);
                        } else if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("type")) {
                            String documentType = dataArray.getJSONObject(index).getJSONObject(key).getString("value");
                            contractDocumentDetails.put("DocumentType", documentType);
                        } else if(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("locked_status")){
                            try{
                                contractDocumentDetails.put("DocumentLockedStatus", dataArray.getJSONObject(index).getJSONObject(key).getString("value"));
                            }catch (Exception e){
                                continue;
                            }
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

    private int getClauseCountBeforeDeviation(int entityId, List<HashMap<String, String>> contractDocuments, CustomAssert customAssert) {
        List<String> clauseCategory = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        try {
            for (HashMap<String, String> contractDocument : contractDocuments) {
                ids.add(contractDocument.get("Id"));
            }

            for (String id : ids) {
                ListRendererListData listRendererListData = new ListRendererListData();
                String payload = "{\"filterMap\":{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"entityTypeId\":160,\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":null,\"documentFileId\":\"" + id + "\",\"entityId\":" + entityId + "}},\"filterJson\":{}}}";
                listRendererListData.hitListRendererListDataV2WithOutParams("492", payload);
                String contractDocumentsListData = listRendererListData.getListDataJsonStr();
                if (ParseJsonResponse.validJsonResponse(contractDocumentsListData)) {
                    JSONArray data = new JSONObject(contractDocumentsListData).getJSONArray("data");
                    for (int index = 0; index < data.length(); index++) {
                        Set<String> keys = data.getJSONObject(index).keySet();
                        for (String key : keys) {
                            if (data.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("clauseCategory")) {
                                String value = data.getJSONObject(index).getJSONObject(key).getString("value");
                                if (!clauseCategory.contains(value)) {
                                    clauseCategory.add(value);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    logger.error("Contract Clause List Response is not a valid response");
                    customAssert.assertTrue(false, "Contract Clause List Response is not a valid response");
                }
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while getting the count of the clauses before deviation.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while getting the count of the clauses before deviation.");
        }
        return clauseCategory.size();
    }

    private HashMap<String, String> downloadTemplate(HashMap<String, String> downloadedDocument, CustomAssert customAssert) {
        Download download = new Download();
        String fileName = "AUTOMATION PURPOSE (PLEASE DO NOT USE).docx";
        String downloadPath = System.getProperty("user.dir") + "/src/test/java/com/sirionlabs/test/pod/ContractDocuments";
        try {
            String queryString = "/contracttemplate/download/5349?";
            if (download.hitDownload(downloadPath, fileName, queryString)) {
                downloadedDocument.put("DocumentName", fileName);
                downloadedDocument.put("DownloadPath", downloadPath);
                downloadedDocument.put("DownloadStatus", "Downloaded");
            }
        } catch (Exception e) {
            logger.error("Execption {} occurred while downloading the seprate template", e.getMessage());
            customAssert.assertTrue(false, "Execption " + e.getMessage() + " occurred while downloading the seprate template");
        }
        return downloadedDocument;
    }

    private boolean uploadTheDocument(HashMap<String, String> downloadedDocument, int entityId, boolean generalUpload, CustomAssert customAssert) {
        boolean flag = false;
        try {
            String id = downloadedDocument.get("Id");
            String extension = downloadedDocument.get("DocumentExtension");
            String documentId = downloadedDocument.get("DocumentId");
            String name = downloadedDocument.get("DocumentName").split("." + extension)[0];
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(downloadedDocument.get("DownloadPath") + "/" + downloadedDocument.get("DocumentName"));
            String uploadDraftResponse = null;
            if (generalUpload)
                uploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument(name, extension, randomKeyForFileUpload, entityTypeId, "" + entityId, fileToUpload);
            else {
                uploadDraftResponse = PreSignatureHelper.fileUploadDraft(name, extension, randomKeyForFileUpload, entityTypeId, "" + entityId, documentId, fileToUpload);
            }

            String commentDocumentsValue = null;
            if (ParseJsonResponse.validJsonResponse(uploadDraftResponse)) {
                JSONObject uploadDraftJSON = new JSONObject(uploadDraftResponse);
                int templateTypeId = Integer.parseInt(uploadDraftJSON.get("templateTypeId").toString());
                int documentSize = Integer.parseInt(uploadDraftJSON.get("documentSize").toString());
                String key = uploadDraftJSON.getString("key");
                int documentStatusId = Integer.parseInt(uploadDraftJSON.get("documentStatusId").toString());
                if (generalUpload) {
                    commentDocumentsValue = "{\"templateTypeId\":" + templateTypeId + ",\"documentFileId\":null,\"documentTags\":null,\"documentSize\":" + documentSize + ",\"key\":\"" + key + "\",\"documentStatusId\":" + documentStatusId + ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false}";
                } else {
                    commentDocumentsValue = "{\"templateTypeId\":" + templateTypeId + ",\"documentFileId\":" + id + ",\"documentTags\":[],\"documentSize\":" + documentSize + ",\"key\":\"" + key + "\",\"documentStatusId\":" + documentStatusId + ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false,\"documentId\":" + documentId + "}";
                }
                Edit edit = new Edit();
                String editResponsePayload = edit.getEditPayload(entityName, entityId);

                if (ParseJsonResponse.validJsonResponse(editResponsePayload)) {
                    JSONObject editJSON = new JSONObject(editResponsePayload);

                    Set<String> elements = editJSON.getJSONObject("body").getJSONObject("data").keySet();
                    String options = null;
                    for (String element : elements) {
                        try {
                            if (!editJSON.getJSONObject("body").getJSONObject("data").getJSONObject(element).isNull("options")) {
                                editJSON.getJSONObject("body").getJSONObject("data").getJSONObject(element).put("options", options);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                    }

                    JSONObject dynamicMetaDataJSON = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                    Set<String> keys = dynamicMetaDataJSON.keySet();
                    for (String dynamicKey : keys) {
                        try {
                            if (!dynamicMetaDataJSON.getJSONObject(dynamicKey).isNull("options")) {
                                dynamicMetaDataJSON.getJSONObject(dynamicKey).put("options", options);
                            }
                        } finally {
                            editJSON.getJSONObject("body").getJSONObject("data").put(dynamicKey, dynamicMetaDataJSON.getJSONObject(dynamicKey));
                        }
                    }

                    Set<String> commentKeys = editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").keySet();
                    for (String commentKey : commentKeys) {
                        if (!editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject(commentKey).isNull("options")) {
                            editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject(commentKey).put("options", options);
                        }
                    }

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").append("values", new JSONObject(commentDocumentsValue));

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);

                    editJSON.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", "null");

                    String updatePayload = editJSON.toString();
                    PreSignatureHelper preSignatureHelper = new PreSignatureHelper();
                    String uploadResponse = preSignatureHelper.submitFileDraftWithResponse(updatePayload);

                    if (ParseJsonResponse.validJsonResponse(uploadResponse)) {
                        if (new JSONObject(uploadResponse).getJSONObject("header").getJSONObject("response").getString("status").equalsIgnoreCase("success")) {
                            flag = true;
                        } else {
                            logger.error("Could not upload the document.");
                            customAssert.assertTrue(false, "Could not upload the document.");
                        }
                    } else {
                        logger.error("Upload API Response is not a valid JSON response.");
                        customAssert.assertTrue(false, "Upload API Response is not a valid JSON response.");
                    }
                } else {
                    logger.error("Error occurred while getting the upload payload.");
                    customAssert.assertTrue(false, "Error occurred while getting the upload payload.");
                }
            } else {
                logger.error("Upload Draft Response is not a valid JSON Response.");
                customAssert.assertTrue(false, "Upload Draft Response is not a valid JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while uploading the Contract Document.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while uploading the Contract Document.");
        }
        return flag;
    }

    private int getShowPageIdOfCDR(int listId, CustomAssert customAssert) {
        ListRendererListData listDataObj = new ListRendererListData();
        int showID = 0;
        try {
            String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1656\",\"name\":\"Active\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":12259,\"columnQueryName\":\"id\"},{\"columnId\":12260,\"columnQueryName\":\"title\"}]}";
            listDataObj.hitListRendererListData(listId, payload);
            String listDataResponse = listDataObj.getListDataJsonStr();
            if (listDataResponse != null) {
                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    String data = new JSONObject(listDataResponse).get("data").toString();
                    JSONArray dataArray = new JSONArray(data);
                    String dataArrayKeys = dataArray.getJSONObject(0).toString();
                    Set<String> dataArrayKeySet = new JSONObject(dataArrayKeys).keySet();
                    for (String dataArrayKey : dataArrayKeySet) {
                        String valueToKey = new JSONObject(dataArrayKeys).get(dataArrayKey).toString();
                        JSONObject jo = new JSONObject(valueToKey);
                        String value = jo.get("value").toString();
                        String columnName = jo.get("columnName").toString();
                        if (!value.equalsIgnoreCase("null") & !columnName.equalsIgnoreCase("null")) {
                            if (columnName.equalsIgnoreCase("id")) {
                                showID = Integer.parseInt(value.split(";")[1]);
                                break;
                            } else
                                continue;
                        }
                    }
                } else {
                    logger.error("Listing response of CDR is not a valid JSON");
                    customAssert.assertTrue(false, "Listing response of CDR is not a valid JSON");
                }
            } else {
                logger.error("Listing response is null");
                customAssert.assertTrue(false, "Listing response is null");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while fetching the list data of CDR");
            customAssert.assertTrue(false, "Exception occurred while fetching the list data of CDR");
        }
        return showID;
    }

    private HashMap<String, String> getDisplayMode(int listId, CustomAssert customAssert) {
        Edit edit = new Edit();
        HashMap<String, String> editResponseData = new HashMap<>();
        String displayMode = null;
        String editResponse;
        boolean flag = false;
        try {
            editResponse = edit.hitEdit("Contract Draft Request", getShowPageIdOfCDR(listId, customAssert));
            if (editResponse != null) {
                if (ParseJsonResponse.validJsonResponse(editResponse)) {
                    editResponseData.put("EditGetPayload", editResponse);
                    JSONArray fieldArray = new JSONObject(editResponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");
                    for (int index = 0; index < fieldArray.length(); index++) {
                        JSONArray fieldsArray = fieldArray.getJSONObject(index).getJSONArray("fields");
                        for (int pointer = 0; pointer < fieldsArray.length(); pointer++) {
                            JSONArray basicFieldsArray = fieldsArray.getJSONObject(pointer).getJSONArray("fields");
                            for (int finalIndex = 0; finalIndex < basicFieldsArray.length(); finalIndex++) {
                                JSONObject finalJsonFields = basicFieldsArray.getJSONObject(finalIndex);
                                if (finalJsonFields.getString("label").equalsIgnoreCase("Total Deviations")) {
                                    displayMode = finalJsonFields.getString("displayMode");
                                    editResponseData.put("DisplayMode", displayMode);
                                    flag = true;
                                    break;
                                }
                            }
                            if (flag)
                                break;
                        }
                        if (flag)
                            break;
                    }
                } else {
                    logger.error("Show page Edit Response is not a valid JSON");
                    customAssert.assertTrue(false, "Show page Edit Response is not a valid JSON");
                }

            } else {
                logger.error("Show page Edit Response is null");
                customAssert.assertTrue(false, "Show page Edit Response is null");
            }

        } catch (Exception e) {
            logger.error("Exception occurred while editing the Entity : {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while editing the Entity : " + e.getMessage());
        }
        return editResponseData;
    }

    private boolean downloadCDRListing(int listId, CustomAssert customAssert) {
        DownloadListWithData downloadListWithData = new DownloadListWithData();
        boolean downloadStatus;

        boolean columnStatus = false;
        try {
            HashMap<String, String> formParams = new HashMap<>();
            formParams.put("_csrf_token", "null");
            formParams.put("jsonData", "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":6,\"listId\":null,\"filterName\":\"status\",\"filterShowName\":null,\"minValue\":null,\"maxValue\":null,\"min\":null,\"max\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1656\",\"name\":\"Active\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1655\",\"name\":\"Approved\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1653\",\"name\":\"Awaiting Client Review\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1657\",\"name\":\"Inactivated\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1652\",\"name\":\"Newly Created\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1654\",\"name\":\"Rejected\",\"group\":\"Contract Draft Request\",\"type\":null},{\"id\":\"1\",\"name\":\"Archived\",\"group\":\"Default\",\"type\":null},{\"id\":\"2\",\"name\":\"On Hold\",\"group\":\"Default\",\"type\":null}]},\"startDate\":null,\"endDate\":null,\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"start\":null,\"end\":null,\"dayOffset\":null,\"monthType\":null,\"duration\":null,\"dayType\":null,\"operator\":null,\"filterDisabled\":false,\"uitype\":\"MULTISELECT\",\"primary\":false}}}}");
            HttpResponse downloadResponse = downloadListWithData.hitDownloadListWithData(formParams, listId);
            if (downloadResponse != null) {
                String outputFilePath = "src/test/java/com/sirionlabs/test/pod/CDRListingData";
                String outputFileName = "CDRListingData.xlsx";
                downloadStatus = downloadListWithData.dumpDownloadListIntoFile(downloadResponse, outputFilePath, outputFileName);
                if (downloadStatus) {
                    File file = new File(outputFilePath + "/" + outputFileName);
                    FileInputStream fis = new FileInputStream(file);
                    Workbook workbook = new XSSFWorkbook(fis);
                    Sheet sheet = workbook.getSheetAt(0);
                    int totalRows = sheet.getLastRowNum();
                    int totalCols = sheet.getRow(0).getLastCellNum();
                    DataFormatter dataFormatter = new DataFormatter();
                    Object[][] dataOfExcelSheet = new Object[totalRows][totalCols];
                    int rowId = 0;

                    for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                        for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                            try {
                                Cell cell = sheet.getRow(rowIndex).getCell(colIndex);
                                dataOfExcelSheet[rowIndex][colIndex] = dataFormatter.formatCellValue(cell);
                            } catch (Exception ex) {
                                dataOfExcelSheet[rowIndex][colIndex] = null;
                            }
                        }
                    }

                    for (int index = 0; index < totalRows - 1; index++) {
                        try {
                            if (dataOfExcelSheet[index][0].toString().equalsIgnoreCase("id")) {
                                rowId = index;
                                break;
                            }
                        } catch (Exception ex) {
                            continue;
                        }
                    }

                    for (int rowIndex = rowId; rowIndex < totalRows - 1; rowIndex++) {
                        for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                            String value = dataOfExcelSheet[rowIndex][colIndex].toString();
                            if (value.equalsIgnoreCase("TOTAL DEVIATIONS")) {
                                columnStatus = true;
                                break;
                            }
                        }
                        if (columnStatus)
                            break;
                    }
                } else {
                    logger.error("Response of CDR Listing could not be downloaded");
                    customAssert.assertTrue(false, "Response of CDR Listing could not be downloaded");
                }
            } else {
                logger.error("Download Response of CDR Listing Download is null");
                customAssert.assertTrue(false, "Download Response of CDR Listing Download is null");
            }

        } catch (Exception e) {
            logger.error("Exception while downloading CDR listing data : {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while downloading CDR listing data :" + e.getMessage());
        }
        return columnStatus;
    }

    private boolean performActionOnCDR(int entityId, String actionName, CustomAssert customAssert) {
        try {
            boolean actionFlag = new WorkflowActionsHelper().performWorkflowAction(Integer.parseInt(entityTypeId), entityId, actionName);
            return actionFlag;
        } catch (Exception e) {
            logger.error("Exception {} occurred while performing action {} on CDR.", e.getMessage(), actionName.toUpperCase());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while performing action " + actionName.toUpperCase() + " on CDR.");
            return false;
        }
    }

    private HashMap<String, HashMap<String, String>> getContractDocumentIds(int entityId, CustomAssert customAssert) {
        String value = "";
        String clusterId = "";
        HashMap<String, List<Double>> clusterIds = new HashMap<>();
        HashMap<String, String> latestDocs = new HashMap<>();
        HashMap<String, String> baseDocs = new HashMap<>();
        HashMap<String, HashMap<String, String>> baseAndLatestDocs = new HashMap<>();
        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        try {
            String contractDocumentPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            listRendererTabListData.hitListRendererTabListData(367, Integer.parseInt(entityTypeId), entityId, contractDocumentPayload);
            String listingResponse = listRendererTabListData.getTabListDataJsonStr();
            if (ParseJsonResponse.validJsonResponse(listingResponse)) {
                JSONArray dataArray = new JSONObject(listingResponse).getJSONArray("data");
                for (int index = 0; index < dataArray.length(); index++) {
                    double version = 0.0;
                    for (String key : dataArray.getJSONObject(index).keySet()) {
                        if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("version")) {
                            version = Double.parseDouble(dataArray.getJSONObject(index).getJSONObject(key).getString("value"));
                        } else if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentname")) {
                            value = dataArray.getJSONObject(index).getJSONObject(key).getString("value");
                            clusterId = value.split(":;")[0];
                        }
                    }
                    try {
                        List<Double> exisistingVersion = clusterIds.get(clusterId);
                        exisistingVersion.add(version);
                        clusterIds.put(clusterId, exisistingVersion);
                    } catch (Exception e) {
                        List<Double> versions = new ArrayList<>();
                        versions.add(version);
                        clusterIds.put(clusterId, versions);
                    }
                }
                for (String cId : clusterIds.keySet()) {
                    double latestId = getLatestVersion(clusterIds.get(cId));
                    latestDocs.put(cId, getDocIds(dataArray, latestId, cId));

                    double baseId = getBaseVersion(clusterIds.get(cId));
                    baseDocs.put(cId, getDocIds(dataArray, baseId, cId));
                }
                baseAndLatestDocs.put("LatestDocs", latestDocs);
                baseAndLatestDocs.put("BaseDocs", baseDocs);
            } else {
                logger.error("Contract Document Tab Listing Response is not a valid JSON.");
                customAssert.assertTrue(false, "Contract Document Tab Listing Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching the document IDs under Contract Document Tab.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching the document IDs under Contract Document Tab.");
        }
        return baseAndLatestDocs;
    }

    private double getLatestVersion(List<Double> versions) {
        double maxVersion = versions.get(0);
        for (double version : versions) {
            if (version > maxVersion) {
                maxVersion = version;
            }
        }
        return maxVersion;
    }

    private double getBaseVersion(List<Double> versions) {
        double baseVersion = versions.get(0);
        for (double version : versions) {
            if (version < baseVersion) {
                baseVersion = version;
            }
        }
        return baseVersion;
    }

    private String getDocIds(JSONArray data, double version, String clusterId) {
        String latestDocId = "";
        boolean flag = false;
        for (int index = 0; index < data.length(); index++) {
            double dataVersion = 0.0;
            String value = "";
            String dataClusterId = "";
            for (String key : data.getJSONObject(index).keySet()) {
                if (data.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("version")) {
                    dataVersion = Double.parseDouble(data.getJSONObject(index).getJSONObject(key).getString("value"));
                } else if (data.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentname")) {
                    value = data.getJSONObject(index).getJSONObject(key).getString("value");
                    dataClusterId = value.split(":;")[0];
                }
                if (dataVersion == version & dataClusterId.equals(clusterId)) {
                    latestDocId = value.split(":;")[4];
                    flag = true;
                    break;
                }
            }
            if (flag) {
                break;
            }
        }
        return latestDocId;
    }

    private List<String> getClauseToReview(int entityId, String documentId, CustomAssert customAssert) {
        List<String> contentControlId = new ArrayList<>();
        ListRendererListData listRendererListData = new ListRendererListData();
        try {
            String payload = "{\"filterMap\":{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"entityTypeId\":" + entityTypeId + ",\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":5,\"documentFileId\":\"" + documentId + "\",\"entityId\":" + entityId + "}},\"filterJson\":{}}}";
            listRendererListData.hitListRendererListDataV2WithOutParams("492", payload);
            String listingResponse = listRendererListData.getListDataJsonStr();
            if (ParseJsonResponse.validJsonResponse(listingResponse)) {
                JSONArray dataArray = new JSONObject(listingResponse).getJSONArray("data");
                for (int index = 0; index < dataArray.length(); index++) {
                    Set<String> keys = dataArray.getJSONObject(index).keySet();
                    for (String key : keys) {
                        if (dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("contentControlId")) {
                            contentControlId.add(dataArray.getJSONObject(index).getJSONObject(key).getString("value"));
                            break;
                        }
                    }
                }
            } else {
                logger.error("Contract Clause Tab Listing Response is not a valid JSON.");
                customAssert.assertTrue(false, "Contract Clause Tab Listing Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching clauses to review.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching clauses to review.");
        }
        return contentControlId;
    }

    private HashMap<String, List<String>> compareClauseText(String contentControlId, String baseDocumentId, String latestDocumentId, String clusterId, CustomAssert customAssert) {
        HashMap<String, List<String>> comparedData = new HashMap<>();
        List<String> baseDocText = new ArrayList<>();
        List<String> latestDocText = new ArrayList<>();
        List<String> tempLatestDocText = new ArrayList<>();
        try {
            String payload = "{\"contentControlId\":\"" + contentControlId + "\",\"sourceDocumentFileId\":\"" + baseDocumentId + "\",\"compareDocumentFileiId\":\"" + latestDocumentId + "\",\"documentId\":" + Integer.parseInt(clusterId) + "}";
            String response = executor.post("/tblcdr/deviation/clause/compare", ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            if (ParseJsonResponse.validJsonResponse(response)) {
                JSONObject responseJSON = new JSONObject(response).getJSONObject("clauseInfo");
                Set<String> keys = responseJSON.keySet();
                if (keys.size() == 2) {
                    for (String str : responseJSON.getJSONObject(baseDocumentId).getString("text").split(" ")) {
                        baseDocText.add(str.trim());
                    }
                    for (String str : responseJSON.getJSONObject(latestDocumentId).getString("text").split(" ")) {
                        latestDocText.add(str);
                        tempLatestDocText.add(str.trim());
                    }
                    int baseTextsize = baseDocText.size();
                    int latestTextsize = latestDocText.size();

                    if (baseTextsize == latestTextsize) {
                        logger.error("Clause has not been deviated.");
                        customAssert.assertTrue(false, "Clause has not been deviated.");
                    } else {
                        for (String str : baseDocText) {
                            latestDocText.remove(str);
                        }
                        for (String str : tempLatestDocText) {
                            baseDocText.remove(str);
                        }
                    }
                } else if (keys.size() == 1) {
                    logger.info("Clause is added new. So it's text cannot be compared. ");
                    baseDocText.add("---NewAddedClauseSoNoBaseDoc---");

                    for (String str : responseJSON.getJSONObject(latestDocumentId).getString("text").split(" ")) {
                        latestDocText.add(str.trim());
                    }
                }
                comparedData.put("UpdatedDocText", latestDocText);
                comparedData.put("BaseDocText", baseDocText);
            } else {
                logger.error("Compare Clause Response is not a valid JSON.");
                customAssert.assertTrue(false, "Compare Clause Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while comparing the texts of the clauses.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while comparing the texts of the clauses.");
        }
        return comparedData;
    }

    private double getDeviationSummary(int cdrId, String documentId, CustomAssert customAssert) {
        double deviationCount = 0.0;
        try {
            String deviationSummaryResponse = DeviationSummary.getDeviationSummaryResponse("contract draft request", cdrId, documentId);
            if (ParseJsonResponse.validJsonResponse(deviationSummaryResponse)) {
                JSONArray deviationSummariesArray = new JSONObject(deviationSummaryResponse).getJSONArray("deviationSummaries");
                for (int index = 0; index < deviationSummariesArray.length(); index++) {
                    if (!deviationSummariesArray.getJSONObject(index).getString("name").equalsIgnoreCase("review pending")) {
                        deviationCount = deviationCount + Double.parseDouble(deviationSummariesArray.getJSONObject(index).get("count").toString());
                    }
                }
            } else {
                logger.error("Deviation Summary Response is not a valid JSON.");
                customAssert.assertTrue(false, "Deviation Summary Response is not a valid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching deviation summary from the CDR.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching deviation summary from the CDR.");
        }
        return deviationCount;
    }

    private void reviewClauses(String contentControlId, int documentId, CustomAssert customAssert){
        String payload = "";
        try{
            payload = "{\"contentControlId\":\""+contentControlId+"\",\"documentFileId\":\""+documentId+"\"}";
            String response = executor.post("/tblcdr/review/clauseText", ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();

        }catch (Exception e){
            logger.error("Exception {} occurred while reviewing the clauses.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while reviewing the clauses.");
        }
    }
}