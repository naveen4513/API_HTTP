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
import com.sirionlabs.helper.clientAdmin.AdminHelper;
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
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class TestCDRDeleteDocument{
    private final static Logger logger = LoggerFactory.getLogger(TestCDRDeleteDocument.class);

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

    private String documentPathName = System.getProperty("user.dir") + "/src/test";
    private String documentFileName = "AUTOMATION PURPOSE (PLEASE DO NOT USE).docx";

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
    public void testC153889() {
        CustomAssert customAssert = new CustomAssert();
        AdminHelper adminHelper = new AdminHelper();
        try{
            Set<String> permission = adminHelper.getAllPermissionsForUser("anay_user", 1002);
            customAssert.assertTrue(permission.contains("925") & permission.contains("926") ,"TC-C153889 failed.");
        }catch (Exception e){
            logger.error("Exception {} occurred while executing TC-C153889.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while executing TC-C153889.");
        }
        customAssert.assertAll();
    }

    @Test
    public void testC153890(){
        CustomAssert customAssert = new CustomAssert();
        int cdrId = -1;
        try{
            cdrId = createCDR(customAssert);
            if(uploadTheDocument(downloadTemplate(customAssert),cdrId,1, customAssert)){
                HashMap<String,List<Boolean>> deleteDocumentColumnAtDraft = getContractDocumentColumns(cdrId, customAssert);
                for(String status : deleteDocumentColumnAtDraft.keySet()){
                    if(status.equalsIgnoreCase("draft")){
                        customAssert.assertTrue(deleteDocumentColumnAtDraft.get(status).contains(true),"TC-C153890 is failed as Delete option is disabled at Draft level.");
                    }else{
                        customAssert.assertTrue(deleteDocumentColumnAtDraft.get(status).contains(false),"TC-C153890 is failed as Delete option is enabled at "+status+" level.");
                    }
                }

                if(uploadTheDocument(downloadTemplate(customAssert),cdrId,2, customAssert)){
                    HashMap<String,List<Boolean>> deleteDocumentColumnAtFinal = getContractDocumentColumns(cdrId, customAssert);
                    for(String status : deleteDocumentColumnAtFinal.keySet()){
                        customAssert.assertTrue(deleteDocumentColumnAtFinal.get(status).contains(false),"TC-C153890 is failed as Delete option is not disabled at "+status+" level.");
                    }
                }else{
                    logger.error("Could not upload the document as other than Draft.");
                    customAssert.assertTrue(false,"Could not upload the document as other than Draft");
                }
            }else{
                logger.error("Could not upload the document as a Draft.");
                customAssert.assertTrue(false,"Could not upload the document as a Draft.");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while executing TC-C153890().",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while executing TC-C153890().");
        }finally {
            EntityOperationsHelper.deleteEntityRecord(entityName, cdrId);
            logger.info("CDR with entityId id {} is deleted.", cdrId);
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

    private HashMap<String,List<Boolean>> getContractDocumentColumns(int entityId, CustomAssert customAssert){
        HashMap<String,List<Boolean>> deleteDocument = new HashMap<>();
        String documentStatus = "";
        boolean deletePermission = false;
        TabListData tabListData = new TabListData();
        int tabId = 367;
        try{
            String contractDocumentsListData = tabListData.hitTabListDataV2(tabId, Integer.parseInt(entityTypeId), entityId);
            if (ParseJsonResponse.validJsonResponse(contractDocumentsListData)){
                JSONArray dataArray = new JSONObject(contractDocumentsListData).getJSONArray("data");
                for(int index=0; index<dataArray.length();index++){
                    for(String key : dataArray.getJSONObject(index).keySet()){
                        try{
                            if(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentstatus")){
                                if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[0].equalsIgnoreCase("1")){
                                    documentStatus = "draft";
                                }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[0].equalsIgnoreCase("2")){
                                    documentStatus = "final";
                                }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[0].equalsIgnoreCase("3")){
                                    documentStatus = "executed";
                                }
                            }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("enable_delete_comment")){
                                if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").equalsIgnoreCase("true")) {
                                    deletePermission=true;
                                }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").equalsIgnoreCase("false")) {
                                    deletePermission=false;
                                }
                            }
                        }catch (Exception e){
                            continue;
                        }
                    }
                    try{
                        List<Boolean> earlierPermissionList = deleteDocument.get(documentStatus);
                        earlierPermissionList.add(deletePermission);
                        deleteDocument.put(documentStatus,earlierPermissionList);
                    }catch (Exception e){
                        List<Boolean> earlierPermissionList = new ArrayList<>();
                        earlierPermissionList.add(deletePermission);
                        deleteDocument.put(documentStatus,earlierPermissionList);
                    }
                }
            }else {
                logger.error("Response of Contract Documents Tab API is not a valid JSON");
                customAssert.assertTrue(false, "Response of Contract Documents Tab API is not a valid JSON");
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while fetching the list of Contract Documents.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching the list of Contract Documents.");
        }
        return deleteDocument;
    }

    private HashMap<String,HashMap<String,Boolean>> getClusterAndDeviation(int entityId, CustomAssert customAssert){
        HashMap<String,HashMap<String,Boolean>> clusterDocDeviation = new HashMap<>();
        boolean deviationFlag = false;
        TabListData tabListData = new TabListData();
        int tabId = 367;
        try{
            String contractDocumentsListData = tabListData.hitTabListDataV2(tabId, Integer.parseInt(entityTypeId), entityId);
            if (ParseJsonResponse.validJsonResponse(contractDocumentsListData)) {
                JSONArray dataArray = new JSONObject(contractDocumentsListData).getJSONArray("data");
                String clusterId = "";
                for(int index=0; index<dataArray.length();index++){
                    String docId = "";
                    for(String key : dataArray.getJSONObject(index).keySet()){
                        try{
                            if(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("documentname")){
                                String tempClusterId = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[0];
                                if(!tempClusterId.equalsIgnoreCase(clusterId)){
                                    clusterId = tempClusterId;
                                }
                                docId = dataArray.getJSONObject(index).getJSONObject(key).getString("value").split(":;")[4];
                            }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("columnName").equalsIgnoreCase("triggerdeviation")){
                                if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").equalsIgnoreCase("yes")){
                                    deviationFlag = true;
                                }else if(dataArray.getJSONObject(index).getJSONObject(key).getString("value").equalsIgnoreCase("no")){
                                    deviationFlag = false;
                                }
                            }
                        }catch (Exception e){
                            continue;
                        }
                    }
                    try{
                        HashMap<String,Boolean> deviationMap = clusterDocDeviation.get(clusterId);
                        deviationMap.put(docId,deviationFlag);
                        clusterDocDeviation.put(clusterId,deviationMap);
                    }catch (Exception e){
                        HashMap<String,Boolean> triggerDeviation = new HashMap<>();
                        triggerDeviation.put(docId,deviationFlag);
                        clusterDocDeviation.put(clusterId,triggerDeviation);
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while fetching the list of Contract Documents.", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching the list of Contract Documents.");
        }
        return clusterDocDeviation;
    }

    private String getGeneralUploadDraftResponse(HashMap<String, String> documentDetails, int entityId, CustomAssert customAssert){
        String extension = documentDetails.get("DocumentExtension");
        String name = documentDetails.get("DocumentName").split("." + extension)[0];
        String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
        File fileToUpload = new File(documentDetails.get("DownloadPath") + "/" + documentDetails.get("DocumentName"));
        try{
            return PreSignatureHelper.fileUploadDraftWithNewDocument(name, extension, randomKeyForFileUpload, entityTypeId, "" + entityId, fileToUpload);
        }catch (Exception e){
            logger.error("Draft API was not executed successfully. Exception {} has occurred.",e.getMessage());
            customAssert.assertTrue(false,"Draft API was not executed successfully. Exception "+e.getMessage()+" has occurred.");
            return null;
        }
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

    private HashMap<String, String> downloadTemplate(CustomAssert customAssert) {
        Download download = new Download();
        HashMap<String, String> downloadedDocument = new HashMap<>();
        try {
            String queryString = "/contracttemplate/download/5349?";
            if (download.hitDownload(documentPathName, documentFileName, queryString)) {
                downloadedDocument.put("DocumentExtension","docx");
                downloadedDocument.put("DocumentName",documentFileName);
                downloadedDocument.put("DownloadPath",documentPathName);
            }
        } catch (Exception e) {
            logger.error("Execption {} occurred while downloading the seprate template", e.getMessage());
            customAssert.assertTrue(false, "Execption " + e.getMessage() + " occurred while downloading the seprate template");
        }
        return downloadedDocument;
    }

    private boolean uploadTheDocument(HashMap<String, String> downloadedDocument, int entityId, int documentStatusId, CustomAssert customAssert) {
        boolean flag = false;
        try {
            String extension = downloadedDocument.get("DocumentExtension");
            String name = downloadedDocument.get("DocumentName").split("." + extension)[0];
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(downloadedDocument.get("DownloadPath") + "/" + downloadedDocument.get("DocumentName"));
            String uploadDraftResponse = null;
            uploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument(name, extension, randomKeyForFileUpload, entityTypeId, "" + entityId, fileToUpload);
            String commentDocumentsValue = null;
            if (ParseJsonResponse.validJsonResponse(uploadDraftResponse)) {
                JSONObject uploadDraftJSON = new JSONObject(uploadDraftResponse);
                int templateTypeId = Integer.parseInt(uploadDraftJSON.get("templateTypeId").toString());
                int documentSize = Integer.parseInt(uploadDraftJSON.get("documentSize").toString());
                String key = uploadDraftJSON.getString("key");
//                int documentStatusId = Integer.parseInt(uploadDraftJSON.get("documentStatusId").toString());
                commentDocumentsValue = "{\"templateTypeId\":" + templateTypeId + ",\"documentFileId\":null,\"documentTags\":null,\"triggerDeviation\":false,\"documentSize\":" + documentSize + ",\"key\":\"" + key + "\",\"documentStatusId\":" + documentStatusId + ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false}";
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

    private HashMap<String, String> createDocument(CustomAssert customAssert){
        HashMap<String, String> createdDocument = new HashMap<>();
        try{
            XWPFDocument document = new XWPFDocument();
            File file = new File(documentPathName+"/"+documentFileName);
            FileOutputStream out = new FileOutputStream(file);
            document.write(out);
            out.close();

            createdDocument.put("DocumentExtension","docx");
            createdDocument.put("DocumentName",documentFileName);
            createdDocument.put("DownloadPath",documentPathName);

        }catch (Exception e){
            logger.error("Exception {} occurred while creating Docx file.",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while creating Docx file.");
        }
        return createdDocument;
    }
}