package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.documentFlow.DocumentFlowSave;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestDocumentUploadCDR {


    private static Logger logger = LoggerFactory.getLogger(TestDocumentUploadCDR.class);
    private int cdrEntityTypeId = 160,contractTypeId=61;
    private int cdrListId = 279;
    private int columnIdForIDCDR = 12259, templateId = 1001,cdrId;
    private String fileDownloadFilePath, fileName;
    private int draftStatusId =2;
    private String configFilePath,configFileName;
    private String contractConfigFilePath,
            contractExtraFieldsConfigFileName,
            contractConfigFileName;

    @BeforeClass
    public void beforeClass(){

        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DocumentUploadCDRConfigFileName");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DocumentUploadCDRConfigFilePath");
        fileDownloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templatedownloadfilepath");
        fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templatename");

        contractConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractconfigfilename");
        contractConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractconfigfilepath");
        contractExtraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractextrafieldsconfigfilename");
    }

    @DataProvider
    public Object[][] DataProviderForTestDocumentUpload() {

        Object[][] object = new Object[2][];

        object[0] = new Object[]{ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserName"),ConfigureEnvironment.getEnvironmentProperty("supplierTypeUserPassword")};
        object[1] = new Object[]{ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password")};


        return object;
    }


    @Test(dataProvider = "DataProviderForTestDocumentUpload",priority = 1,enabled = true)
    public void testDocumentUpload(String user_name,String password) {
        CustomAssert customAssert = new CustomAssert();
        try {

            File f = new File(fileDownloadFilePath +"/"+ fileName);
            if(!f.exists()) {
                logger.error("File to be uploaded cannot be found");
                customAssert.assertTrue(false, "File to be uploaded cannot be found");
                customAssert.assertAll();
            }

            new Check().hitCheck(user_name,password);

            ListRendererListData cdrListRendererListData = new ListRendererListData();
            cdrListRendererListData.hitListRendererListData(cdrEntityTypeId, 10, 10, "id", "desc", cdrListId);
            String cdrListDataResponse = cdrListRendererListData.getListDataJsonStr(); //Calling dateColumnIds API for CDR

            JSONObject cdrListRenderResponseJson = new JSONObject(cdrListDataResponse);

            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
            try {
                parseJsonResponse.getNodeFromJsonWithValue(cdrListRenderResponseJson, Collections.singletonList("columnId"), columnIdForIDCDR); //Extracting columnIdStatus for the first CDR in dateColumnIds
            } catch (Exception e) {
                logger.error("Exception in extracting value of the dateColumnIds data API");
                customAssert.assertTrue(false, "Cannot extract CDR from cdr listing, hence marking it failed");
                customAssert.assertAll();
            }

            cdrId = -1;
            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                cdrId = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]); //Extracting id from the column value of the CDR dateColumnIds
            else {
                logger.error("columnIdStatus could not be retrieved correctly");
                customAssert.assertTrue(false, "columnIdStatus could not be retrieved correctly");
                customAssert.assertAll();
            }

            logger.info("Using CDR id {}",cdrId);

            //cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid") == null ? cdrId : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrid"));

            TabListData cdrTabListData = new TabListData();
            String tabListResponse;

            tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

            JSONObject tabListJson = new JSONObject(tabListResponse);
            JSONArray tabListJsonArray = tabListJson.getJSONArray("data");
            int previousLength = tabListJsonArray.length();


            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

            Map<String, String> queryParameters = setPostParams(cdrId, randomKeyForFileUpload);

            if (queryParameters.isEmpty()) {
                logger.error("cannot make query parameters map");
                customAssert.assertTrue(false, "cannot make query parameters map");
                customAssert.assertAll();
            }

            FileUploadDraft fileUploadDraft = new FileUploadDraft();
            String uploadResponse = fileUploadDraft.hitFileUpload(fileDownloadFilePath, fileName, queryParameters);
            String documentFileId = null, documentSize = null;
            try {
                if (new JSONObject(uploadResponse).get("documentFileId") != JSONObject.NULL)
                    documentFileId = String.valueOf(new JSONObject(uploadResponse).getInt("documentFileId"));
                documentSize = String.valueOf(new JSONObject(uploadResponse).getInt("documentSize"));
            } catch (Exception e) {
                logger.error("cannot extract documents details for draft submit");
                customAssert.assertTrue(false, "cannot extract documents details for draft submit");
                customAssert.assertAll();
            }

            // Submit Document Draft
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[ { \"templateTypeId\": "+templateId+", \"documentFileId\": " + documentFileId + ", \"documentSize\": " + documentSize + ", \"key\": " + randomKeyForFileUpload + ", \"documentStatusId\": "+draftStatusId+", \"permissions\": { \"financial\": false, \"legal\": false, \"businessCase\": false }, \"performanceData\": false, \"searchable\": false, \"shareWithSupplierFlag\": false } ]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentChangeStatusJsonArray);
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);
            String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
            customAssert.assertTrue(submitDraftResponse.getStatusLine().getStatusCode() == 200, "Submit draft API Response is not valid");
            String responseString = EntityUtils.toString(submitDraftResponse.getEntity());
            if (!responseString.contains("success")) {
                logger.error("Submit data API response is not SUCCESS");
                customAssert.assertTrue(false, "Submit data API response is not SUCCESS");
                customAssert.assertAll();
            }

            logger.info("*************************** Excel submitted successfully ******************************");

            logger.debug("Checking for validation of the uploaded file");

            cdrTabListData = new TabListData();

            tabListResponse = cdrTabListData.hitTabListData(367, 160, cdrId);

            tabListJson = new JSONObject(tabListResponse);
            tabListJsonArray = tabListJson.getJSONArray("data");
            if(tabListJsonArray.length()<=previousLength){
                logger.error("Cannot find Document after uploading");
                customAssert.assertTrue(false, "Cannot find Document after uploading");
                customAssert.assertAll();
            }


        } catch (Exception e) {
            logger.error("Exception caught in testDocumentUpload()");
            customAssert.assertTrue(false, "Exception caught in testDocumentUpload()");
        }

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "testDocumentUpload")
    public void testDocumentInheritFromDCR() throws InterruptedException {

        CustomAssert customAssert = new CustomAssert();

        String flowToExecute = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractcreationflow");

        //UpdateFile.updateConfigFileProperty(contractConfigFilePath,contractExtraFieldsConfigFileName,flowToExecute,"contractDocuments",documentsToBeSubmittedForContractCreationJson.toString());
        UpdateFile.addPropertyToConfigFile(contractConfigFilePath,contractExtraFieldsConfigFileName,flowToExecute,"sourceEntityId","{\"values\":"+cdrId+"}");
        UpdateFile.addPropertyToConfigFile(contractConfigFilePath,contractExtraFieldsConfigFileName,flowToExecute,"sourceEntityTypeId","{\"values\":"+cdrEntityTypeId+"}");

        int contractId = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, "fixed fee flow 1");

        logger.info("Contract Id is : {}", contractId);

        int tabListIdForDocumentListingOnContract = 409;
        ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
        String payload = tabListPayload(contractTypeId,contractId);
        listRendererTabListData.hitListRendererTabListData(tabListIdForDocumentListingOnContract,cdrEntityTypeId,cdrId,payload);
        String response = listRendererTabListData.getTabListDataJsonStr();
        JSONObject jsonObject = new JSONObject(response);

        if(jsonObject.getJSONArray("data").length()==0){
            logger.error("Cannot find Final documents in the CDR");
            customAssert.assertTrue(false, "Cannot find Final documents in the CDR");
            customAssert.assertAll();
        }

        String documentId=null;
        String columnIdForExtractingDocId = "17813";
        try {
            documentId= jsonObject.getJSONArray("data").getJSONObject(0).getJSONObject(columnIdForExtractingDocId).getString("value").split(":;")[0];
        }
        catch (Exception e){
            logger.error("Exception occurred while extracting value from listing");
            customAssert.assertTrue(false, "Exception occurred while extracting value from listing");
            customAssert.assertAll();
        }

        DocumentFlowSave documentFlowSave = new DocumentFlowSave();
        documentFlowSave.hitDocumentFlowSave(saveInheritPayload(contractId,contractTypeId,documentId));
        String documentSaveResponse = documentFlowSave.getDocumentFlowSaveJsonStr();

        if(!documentSaveResponse.contains("Your request has been successfully submitted.")){
            logger.error("Document save response is not successful, got response [{}]",documentSaveResponse);
            customAssert.assertTrue(false, "Document save response is not successful");
            customAssert.assertAll();
        }

        int timeOut = 60000;

        while((timeOut-=5000)>0){
            logger.info("Firing listing api to check the inherit");
            int listId = 366;
            listRendererTabListData = new ListRendererTabListData();
            payload = listRendererTabListData.getPayload(contractTypeId,0,20,"id","asc","{}");
            listRendererTabListData.hitListRendererTabListData(listId,contractTypeId,contractId,payload);
            response = listRendererTabListData.getTabListDataJsonStr();
            jsonObject = new JSONObject(response);

            if(jsonObject.getJSONArray("data").length()!=0){
                logger.debug("Document from CDR inherited to Contract");
                break;
            }

            logger.info("Document inherit not done, Wait 5 sec.");
            Thread.sleep(5000);
        }

        if(jsonObject.getJSONArray("data").length()==0){
            logger.error("Cannot find Final documents in the contract after inherit");
            customAssert.assertTrue(false, "Cannot find Final documents in the contract after inherit");
            customAssert.assertAll();
        }

        customAssert.assertAll();

    }

    @Test(priority = 3)
    public void testCustomerFieldInheritanceInContractFromCdr(){


        CustomAssert customAssert = new CustomAssert();
        String[] cdrId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"cdridforcustomerfieldinheritance").split(",");

        String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[" + cdrId[1] + "],\"entityTypeId\":1}," +
                "\"actualParentEntity\":{\"entityIds\":[" + cdrId[1] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                cdrId[0] + "],\"entityTypeId\":160}}";

        New newContract = new New();
        newContract.hitNewV1ForMultiSupplier("contracts",payload);
        String responseString = newContract.getNewJsonStr();

        logger.info("Response : {}",responseString);

        ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
        parseJsonResponse.getNodeFromJsonWithValues(new JSONObject(responseString),Collections.singletonList("id"),12336);
        Object valueObject = parseJsonResponse.getJsonNodeValue();

        if(valueObject==null||valueObject==JSONObject.NULL){
            logger.error("Customer value not found in new api");
            customAssert.assertTrue(false, "Customer value not found in new api");
            customAssert.assertAll();
        }

        customAssert.assertAll();
    }

    private Map<String, String> setPostParams(int entityId, String randomKeyForFileUpload) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", fileName.split("\\.")[0]);
        map.put("extension", "xlsm");
        map.put("entityTypeId", String.valueOf(cdrEntityTypeId));
        map.put("entityId", String.valueOf(entityId));
        map.put("serviceDataRequest", "true");
        map.put("key", randomKeyForFileUpload);

        return map;
    }

    public String tabListPayload(int entityTypeId, int entityId) {
        return  "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}},\"defaultParameters\": { \"targetEntityTypeId\": "+entityTypeId+", \"targetEntityId\": "+entityId+", \"docFlowType\": \"inherit\" }}";
    }

    private String saveInheritPayload(int contractId,int contractTypeId,String documentId){
        return "{\"entityId\":"+contractId+",\"entityTypeId\":"+contractTypeId+",\"auditLogDocIds\":[\""+documentId+"\"],\"sourceTabId\":2,\"statusId\":1}";
    }
}
