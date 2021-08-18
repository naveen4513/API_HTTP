package com.sirionlabs.test.cdr;

import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestClassificationDocumentUploadCDR {

    private static Logger logger = LoggerFactory.getLogger(TestClassificationDocumentUploadCDR.class);
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public  void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ClassificationFunctionalityCDRConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ClassificationFunctionalityCDRConfigFileName");

    }


    @DataProvider(name = "getDataForClassificationFuncTest", parallel = false)
	public Object[][] dataProviderForClassificationFuncTest() {

		List<Object[]> allTestData = new ArrayList<>();

		logger.info("Setting Entities For Classification Functionality Test");


        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows to test").split(",");

        String fileType;
        String classificationType;
        String templateType;
        String documentStatus;
        String shareWithSupplier;
        String privateFlag;

        for(String flowToTest : flowsToTest){

            fileType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"file type");
            classificationType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"classification type");
            templateType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"template type");
            documentStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"document status");
            shareWithSupplier = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"share with supplier");
            privateFlag = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"private");

            allTestData.add(new Object[]{fileType, classificationType,templateType,documentStatus,shareWithSupplier,privateFlag});
        }

        return allTestData.toArray(new Object[0][]);

	}

    @Test(dataProvider = "getDataForClassificationFuncTest")
    public void TestDocumentUploadCDR(String extension,String classificationType,String templateType, String documentStatus, String shareWithSupplier, String privateFlag) {
        CustomAssert csAssert = new CustomAssert();

        try {


            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contracts");
            String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

            String idValue = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");
            int contractId = ListDataHelper.getRecordIdFromValue(idValue);

            logger.info("Uploading Document on Contract Document tab of Contract Id {}", contractId);
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);

            String templatePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload filepath");

            String templateName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"upload filename " + extension);

            String tabListPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, tabListPayload);

            int noOfDocsBeforeUpload = new JSONObject(tabListResponse).getJSONArray("data").length();

            Map<String, String> queryParameters = setPostParams(templateName, contractId, randomKeyForFileUpload,extension);

            FileUploadDraft fileUploadDraft = new FileUploadDraft();
            String uploadResponse = fileUploadDraft.hitFileUpload(templatePath, templateName, queryParameters);

            if (ParseJsonResponse.validJsonResponse(uploadResponse)) {
                tabListResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, tabListPayload);

                int noOfDocsAfterUpload = new JSONObject(tabListResponse).getJSONArray("data").length();

                if (noOfDocsAfterUpload <= noOfDocsBeforeUpload) {
                    csAssert.assertFalse(true, "Document not appearing in Contract Document Tab of Contract Id " + contractId);
                }
            } else {
                csAssert.assertFalse(true, "Doc Upload Response is an Invalid JSON.");
            }

            int cdrID = 4114;
            PreSignatureHelper preSignatureHelper = new PreSignatureHelper();
            String editResponse = preSignatureHelper.getContractDraftRequestEditPageResponseString(cdrID);
            JSONObject contractDraftRequestJson  =new JSONObject(editResponse);

            int templateId = -1;
            int documentStatusId = 1;

            String documentFileId = null;
            int documentSize = 8665;

            Boolean legalFlag = false;
            Boolean financialFlag = false;
            Boolean businessCaseFlag = false;

            String[] classificationTypeArray = classificationType.split(",");
            List<String> classificationTypeList = Arrays.asList(classificationTypeArray);

            if(classificationTypeList.contains("legal")){
                legalFlag = true;
            }

            if(classificationTypeList.contains("financial")){
                financialFlag = true;
            }
            if(classificationTypeList.contains("business case")){
                businessCaseFlag = true;
            }

            if(templateType.equalsIgnoreCase("Main Template")){
                templateId = 1001;
            }else if(templateType.equalsIgnoreCase("Attachment")){
                templateId = 1002;
            }else if(templateType.equalsIgnoreCase("Service Data Bulk Template")){
                templateId = 900;
            }

            if(documentStatus.equalsIgnoreCase("Draft")){
                documentStatusId = 1;
            }else if(documentStatus.equalsIgnoreCase("Final")){
                documentStatusId = 2;
            }

            JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[{\"templateTypeId\":" + templateId + ",\"documentFileId\":" + documentFileId + ",\"documentSize\":" + documentSize + ",\"key\":\"vAry4vNnihfB\",\"documentStatusId\":" + documentStatusId + ",\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":" + shareWithSupplier + ",\"legal\":" + legalFlag + ",\"financial\":" + financialFlag + ",\"businessCase\":" + businessCaseFlag + "}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentChangeStatusJsonArray);

            if(documentStatus.equalsIgnoreCase("Draft")){
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("draft").put("values", true);
            }

            if(privateFlag.equalsIgnoreCase("true")){
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", true);
            }else {
                contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("privateCommunication").put("values", "null");
            }

            String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            String submitResponse = preSignatureHelper.submitFileDraftWithResponse(submitDraftPayload);

            System.currentTimeMillis();



        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Contract Document Doc Upload. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private Map<String, String> setPostParams(String templateName, int entityId, String randomKeyForFileUpload,String extension) {

        Map<String, String> map = new HashMap<>();
        if (entityId == -1)
            return map;

        map.put("name", templateName.split("\\.")[0]);
        map.put("extension", extension);
        map.put("entityTypeId", String.valueOf(61));
        map.put("entityId", String.valueOf(entityId));
        map.put("key", randomKeyForFileUpload);

        return map;
    }

}
