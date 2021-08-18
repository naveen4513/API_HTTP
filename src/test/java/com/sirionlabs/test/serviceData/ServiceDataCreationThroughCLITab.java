package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.rawdata.RawDataApi;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class ServiceDataCreationThroughCLITab {

        public static final Logger logger = LoggerFactory.getLogger(ServiceDataCreationThroughCLITab.class);

        private String configFilePath;
        private String configFileName;

        private int cdrEntityTypeId;
        private String cdrName;
        String clientId;
        String supplierid;
        int newCdrId;

        private String contractConfigFilePath = "src/test/resources/Helper/EntityCreation/Contract", contractExtraFieldsConfigFileName = "contractExtraFields.cfg", contractConfigFileName = "contract.cfg";

        @BeforeClass
        public void beforeClass(){

            configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SDCreationThroughCLITabConfigFilePath");
            configFileName = ConfigureConstantFields.getConstantFieldsProperty("SDCreationThroughCLITabConfigFileName");

            cdrEntityTypeId = 160;

            cdrName = "contract draft request";
            clientId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"client id");

            supplierid = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplierid");

        }

        @Test
        public void Test_ServiceDataCreationThroughCLITab(){

            CustomAssert customAssert = new CustomAssert();

            try{

                RawDataApi rawDataApi = new RawDataApi();
                String id = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"id");

                String templateId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"template id");

                String payload = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"raw data create payload");

                JSONArray rawDataPayloadArray = new JSONArray(payload);
                String timeStamp = DateUtils.getCurrentTimeStamp();
                String clientIdSd = "new Client_" + timeStamp;
                String supplierIdSd = "new Supplier_" + timeStamp;
                rawDataPayloadArray.getJSONObject(0).put("Client id",clientIdSd);
                rawDataPayloadArray.getJSONObject(0).put("Supplier id",supplierIdSd);

                int cdrIdToClone = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"cdr id to clone"));
                Clone clone = new Clone();

                newCdrId =  clone.createEntityFromClone(cdrName,cdrIdToClone);

                if(newCdrId == -1){
                    failLogging(customAssert,"Unable to create new CDR from clone of CDR ID " + cdrIdToClone);
                    customAssert.assertAll();
                    return;
                }
                String response = rawDataApi.rawDataCreate(id,templateId,cdrEntityTypeId,newCdrId,rawDataPayloadArray.toString());

                validateCliTab(id,templateId,newCdrId,clientIdSd,supplierIdSd,customAssert);

                validateIsDocUpdated(newCdrId,"false",customAssert);

                //Generate Document
                validateGenDocument(newCdrId,true,false,customAssert);

                int contractId = contractDocFinalCreateContract(newCdrId,customAssert);

                if(contractId == -1){
                    failLogging(customAssert,"Error creating contract from CDR");
                }else {

                    checkServiceDataOnListing(contractId,customAssert);
                }

            }catch (Exception e){
                failLogging(customAssert,"Exception while validating the flow");
            }
            customAssert.assertAll();

        }

        @AfterClass
        public void afterClass(){

            EntityOperationsHelper.deleteEntityRecord(cdrName,newCdrId);

        }

        private int createContract(int cdrId,String documentIdNew,CustomAssert customAssert){

            int contractId = -1;
            try {
                JSONObject documentsToBeSubmittedForContractCreationJson = new JSONObject();
                JSONObject documentPayloadForCdr = new JSONObject();
                documentPayloadForCdr.put("auditLogDocFileId", documentIdNew);
                documentsToBeSubmittedForContractCreationJson.put("values", new JSONArray().put(0, documentPayloadForCdr));
                documentsToBeSubmittedForContractCreationJson.put("name", "contractDocuments");
                documentsToBeSubmittedForContractCreationJson.put("multiEntitySupport", false);

                UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "contractDocuments", documentsToBeSubmittedForContractCreationJson.toString());
                UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "sourceEntityId", "{\"values\":" + cdrId + "}");
                UpdateFile.addPropertyToConfigFile(contractConfigFilePath, contractExtraFieldsConfigFileName, "fixed fee flow 1", "sourceEntityTypeId", "{\"values\":" + cdrEntityTypeId + "}");

                assert supplierid != null : "Supplier id is found null";

                UpdateFile.updateConfigFileProperty(contractConfigFilePath, contractConfigFileName, "fixed fee flow 1", "sourceid", supplierid);

                contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");

                logger.info("Contract Id is : {}", contractId);
            }catch (Exception e){
                failLogging(customAssert,"Exception while creating contract from CDR");
            }
            return contractId;
        }

        private void failLogging(CustomAssert customAssert,String message){

            logger.error(message);
            customAssert.assertFalse(true,message);

        }

        private void validateGenDocument(int cdrId,Boolean excelGen,Boolean wordGen,CustomAssert customAssert){
            try{

                Edit edit = new Edit();
                String editPayload = edit.getEditPayload(cdrName,cdrId);
                JSONObject editPayloadJson = new JSONObject(editPayload);
                JSONObject valuesJson = new JSONObject("{\"excelDocument\":" + excelGen + ",\"wordDocument\":" + wordGen + "}");

                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("cdrLineItem").put("values",valuesJson);
                editPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("lineItemDocument").put("values",true);
                String editResponse = edit.hitEdit(cdrName,editPayloadJson.toString());

                if(!editResponse.contains("success")){
                    failLogging(customAssert,"During Generate Document Edit Response does not contain success Document no generated properly");
                }

            }catch (Exception e){
                failLogging(customAssert,"");
            }
        }

        private void validateIsDocUpdated(int newCdrId,String isDocGen,CustomAssert customAssert){
            try {
                RawDataApi rawDataApi = new RawDataApi();

                String docUploadResp = rawDataApi.lineItemIsDocUpdated(cdrEntityTypeId, newCdrId);

                if (!JSONUtility.validjson(docUploadResp)) {
                    failLogging(customAssert, "Is Document Updated Response is an invalid json");
                } else {
                    JSONObject docUploadRespJson = new JSONObject(docUploadResp);
                    String isDocGenAct = docUploadRespJson.get("generateDocument").toString();
                    if(!isDocGenAct.equals(isDocGen)){
                        failLogging(customAssert,"In validate IsDocumentUpdated method Value of generateDocument Expected : " + isDocGen + " Actual value : " + isDocGenAct);
                    }

                }
            }catch (Exception e){
                failLogging(customAssert,"Exception while validating is Doc updated API ");
            }
        }

        private void validateCliTab(String id,String templateId,int newCdrId,String clientIdSd,String supplierIdSd,CustomAssert customAssert) {
            try {
                RawDataApi rawDataApi = new RawDataApi();

                String globalListPayload = "{\"clientId\":" + clientId + ",\"parentEntityTypeId\":" + id + ",\"parentEntityId\":" + templateId + ",\"entityTypeId\":" + cdrEntityTypeId + ",\"entityId\":" + newCdrId + ",\"size\":9999999,\"offset\":0}";

                String globalListResponse = rawDataApi.rawDataGlobalList(globalListPayload);

                if(!JSONUtility.validjson(globalListResponse)){
                    failLogging(customAssert,"On Contract Line Item tab Global List Response is not a valid json");
                }else {

                    String columnNameValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cli tab column name values");

                    String[] columnNames = columnNameValues.split(",");

                    HashMap<String, String> expColValuesMap = new HashMap<>();
                    String colName;
                    String colValue;
                    for (int i = 0; i < columnNames.length; i++) {

                        colName = columnNames[i].split("->")[0];
                        colValue = columnNames[i].split("->")[1];
                        expColValuesMap.put(colName, colValue);
                    }
                    expColValuesMap.put("Client id", clientIdSd);
                    expColValuesMap.put("Supplier id", supplierIdSd);

                    JSONObject globalListResponseJson = new JSONObject(globalListResponse);

                    JSONArray jsonDataArray = globalListResponseJson.getJSONArray("data");
                    JSONObject dataJson;
                    JSONArray columnArray;
                    String actualColName;
                    String actualColValue;
                    for (int i = 0; i < jsonDataArray.length(); i++) {
                        dataJson = jsonDataArray.getJSONObject(0);

                        columnArray = JSONUtility.convertJsonOnjectToJsonArray(dataJson);

                        for (int j = 0; j < columnArray.length(); j++) {
                            actualColName = columnArray.getJSONObject(j).get("columnName").toString();
                            actualColValue = columnArray.getJSONObject(j).get("columnValue").toString();

                            if (expColValuesMap.containsKey(actualColName)) {
                                String expValue = expColValuesMap.get(actualColName);
                                if (!actualColValue.equals(expValue)) {
                                    failLogging(customAssert, "In the CLI Tab for CDR ID " + newCdrId + " Expected Value : " + expValue + " Actual Value : " + actualColValue);
                                }
                            } else {
                                logger.debug("Column Name " + actualColName + " not found in CLI Tab Listing ");
                            }
                        }

                    }
                }
            }catch (Exception e){
                failLogging(customAssert,"Exception while validating CLI tab");
            }
        }

        private int contractDocFinalCreateContract(int cdrId,CustomAssert customAssert){
            int contractId = -1;
            try{

                TabListData cdrTabListData = new TabListData();
                String tabListResponse;

                boolean found = false;
                int columnIdStatus = 15939, columnIdName = 14388;
                String validationSuccessId = "1";
                ParseJsonResponse parseJsonResponse1;
                int timeOutForValidation = 240000;
                int interval = 5000, timeElapsed = 0;
                String clusterId = null;

    //            documentUFileId=String.valueOf(Integer.parseInt(documentFileId)+1);

                tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

                JSONObject tabListJson = new JSONObject(tabListResponse);
                JSONArray tabListJsonArray = tabListJson.getJSONArray("data");
                String documentFileId = null, documentSize = null;

                for (Object jsonObject : tabListJsonArray) {
                    if (jsonObject instanceof JSONObject) {
                        parseJsonResponse1 = new ParseJsonResponse();
                        parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdStatus);
                        Object value = parseJsonResponse1.getJsonNodeValue();
                        if (value instanceof String) {
                            String stringValue = (String) value;
    //                        if (stringValue.contains(documentFileId)) {
                            documentFileId = stringValue.split(":;")[1];
                            parseJsonResponse1 = new ParseJsonResponse();
                            parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdName);
                            value = parseJsonResponse1.getJsonNodeValue();

                            if (value instanceof String) {
                                clusterId = ((String) value).split(":;")[0];
                                break;
                            }
    //                        }
                        }
                    }
                }

                if (clusterId == null) {
                    logger.error("Cannot find cluster id");
                    customAssert.assertTrue(false, "Cannot find cluster id");
                    return -1;
                }

                String documentIdNew = documentFileId;

                while (!found && timeElapsed < timeOutForValidation) {
                    tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

                    tabListJson = new JSONObject(tabListResponse);
                    tabListJsonArray = tabListJson.getJSONArray("data");

                    for (Object jsonObject : tabListJsonArray) {
                        if (jsonObject instanceof JSONObject) {

                            parseJsonResponse1 = new ParseJsonResponse();
                            parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdName);
                            Object value = parseJsonResponse1.getJsonNodeValue();


                            if (value instanceof String) {
                                String stringValue = (String) value;
                                if (stringValue.contains(clusterId)) {

                                    if (Integer.parseInt(((String) value).split(":;")[4]) >= Integer.parseInt(documentIdNew)) {
                                        documentIdNew = ((String) value).split(":;")[4];

                                        parseJsonResponse1 = new ParseJsonResponse();
                                        parseJsonResponse1.getNodeFromJsonWithValue((JSONObject) jsonObject, Collections.singletonList("columnId"), columnIdStatus);
                                        value = parseJsonResponse1.getJsonNodeValue();

                                        if (value instanceof String) {
                                            stringValue = (String) value;
                                            String[] statusAndId = stringValue.split(":;");
                                            if (statusAndId[0].equals(validationSuccessId)) {
                                                found = true;
                                            } else {
                                                logger.error("Successful validation value not found in document {}", stringValue);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Thread.sleep(interval);
                    timeElapsed += interval;
                }

                if (!found) {
                    customAssert.assertTrue(false, "Successful validation value not found in document id [" + documentIdNew + "]");

                    return -1;

                }

                HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
                JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", new JSONArray().put(0, new JSONObject().put("documentFileId", documentIdNew)));
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("editable", true);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("editableDocumentType", true);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("shareWithSupplierFlag", false);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("templateTypeId", 900);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).put("documentStatus", new JSONObject().put("id", 2)); //todo
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").getJSONArray("values").getJSONObject(0).getJSONObject("documentStatus").put("name", "Final");
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", "Document property updated");
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", false);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("shareWithSupplier").put("values", false);
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("documentEditable").put("values", true);

                //contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);

                String submitFinalPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

                Edit editCdr = new Edit();
                String editResponse = editCdr.hitEdit(cdrName, submitFinalPayload);

                if(!editResponse.contains("success")){
                    failLogging(customAssert,"Error while editing cdr during making document as final ");
                }else {

                    contractId = createContract(cdrId,documentIdNew,customAssert);
                }

            }catch (Exception e){

                failLogging(customAssert,"Exception while making contract document as Final");
            }
            return contractId;
        }

        private void checkServiceDataOnListing(int contractId,CustomAssert customAssert){

            try{

                ListRendererListData listRendererListData = new ListRendererListData();

                Map<String, String> params = new HashMap<>();
                params.put("contractId", String.valueOf(contractId));
                params.put("relationId", supplierid);
                params.put("vendorId", "");

                String listingServiceData = "";
                JSONObject jsonObjectForServiceDataListing = new JSONObject();

                int haltTime = 5000;
                int checkCount = 20;
                String listPayload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId + "\",\"name\":\"\"}]},\"filterId\":2,\"filterName\":\"contract\"}}},\"selectedColumns\":[{\"columnId\":14219,\"columnQueryName\":\"id\"}]}";
                while (checkCount > 0) {
                    logger.info("Checking if service data is created for contract {}", contractId);
                    listRendererListData.hitListRendererListDataV2(352, listPayload);
                    listingServiceData = listRendererListData.getListDataJsonStr();
                    jsonObjectForServiceDataListing = new JSONObject(listingServiceData);
                    if (jsonObjectForServiceDataListing.getJSONArray("data").length() > 0)
                        break;
                    checkCount--;
                    logger.info("Halting 5 seconds to check if service data is created for contract {}, remaining count {}", contractId, checkCount);
                    Thread.sleep(haltTime);
                }


                jsonObjectForServiceDataListing = new JSONObject(listingServiceData);
                if (jsonObjectForServiceDataListing.has("data")) {
                    if (jsonObjectForServiceDataListing.getJSONArray("data").length() == 0) {

                        failLogging(customAssert,"No service data found in the listing after filter applied for the created contract");

                    }
                }

            }catch (Exception e){
                failLogging(customAssert,"Exception while checking contract on listing page");
            }
        }

}
