package com.sirionlabs.test.api.AutoExtraction.GlobalUpload;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.AutoExtraction.GlobalUpload.GlobalUploadDTO;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class GlobalUploadAPI {

    private final static Logger logger = LoggerFactory.getLogger(GlobalUploadAPI.class);

    @DataProvider(name = "globalUploadDataProvider")
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/AutoExtraction/GlobalUpload";
        String dataFileName = "globalDataAPI.json";

        List<GlobalUploadDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                    GlobalUploadDTO dtoObject = getUpdateDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
            }
        }

        for (GlobalUploadDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private GlobalUploadDTO getUpdateDTOObjectFromJson(JSONObject jsonObj) {
        GlobalUploadDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            boolean useJsonParam = jsonObj.getBoolean("useJsonParams");
            String key = jsonObj.getString("key");
            String name = jsonObj.getString("name");
            String extension = jsonObj.getString("extension");
            int numberOfFiles = jsonObj.getInt("numberOfFiles");
            JSONArray projectIds = jsonObj.getJSONArray("projectIds");
            JSONArray groupIds = jsonObj.getJSONArray("groupIds");
            JSONArray tagIds = jsonObj.getJSONArray("tagIds");
            String errors  = jsonObj.getString("errors");
            String isSuccess  = jsonObj.getString("success");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");

            dtoObject = new GlobalUploadDTO(testCaseId, description, useJsonParam,key,extension,name, numberOfFiles,projectIds,groupIds,tagIds,errors,isSuccess,expectedStatusCode);
        } catch (Exception e) {
            logger.error("Exception while Getting GlobalUpload DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    public Map<String,String> fileUpload(){
        String filePath =  "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
        String fileName = "KAMADALTD_DRSADraftR_3252013.docx";
        Map<String,String> uploadedFileProperty =TestContractDocumentUpload.fileUpload(filePath,fileName);
        return uploadedFileProperty;
    }

    public String getPayloadForMandateFields(GlobalUploadDTO globalUploadDTO){
        String payload;
        boolean useJsonParam =globalUploadDTO.getUseJsonParam();
        List<JSONObject> filesToUploadJson = new LinkedList<>();
        int filesToUpload = globalUploadDTO.getNumberOfFiles();
        for(int i=0;i<filesToUpload;i++){
            Map<String,String> fileProperties  = fileUpload();
            if(useJsonParam == false){
            filesToUploadJson.add(globalUploadAPI.createJson(fileProperties.get("extension"),fileProperties.get("key"),fileProperties.get("name")));}
            else{
                filesToUploadJson.add(globalUploadAPI.createJson(globalUploadDTO.getExtension(),globalUploadDTO.getKey(),globalUploadDTO.getName()));
            }
        }
        payload = globalUploadAPI.getPayload(filesToUploadJson);
        return payload;
    }

    public String getPayloadForAllFields(GlobalUploadDTO globalUploadDTO){
        String payload;
        boolean useJsonParam =globalUploadDTO.getUseJsonParam();
        List<JSONObject> filesToUploadJson = new LinkedList<>();
        int filesToUpload = globalUploadDTO.getNumberOfFiles();
        for(int i=0;i<filesToUpload;i++){
            Map<String,String> fileProperties  = fileUpload();
            if(useJsonParam == false) {
                filesToUploadJson.add(globalUploadAPI.createJson(fileProperties.get("extension"), fileProperties.get("key"), fileProperties.get("name"), globalUploadDTO.getProjectIds(), globalUploadDTO.getGroupIds(), globalUploadDTO.getTagIds()));
            }
            else{
                filesToUploadJson.add(globalUploadAPI.createJson(globalUploadDTO.getExtension(),globalUploadDTO.getKey(),globalUploadDTO.getName(), globalUploadDTO.getProjectIds(), globalUploadDTO.getGroupIds(), globalUploadDTO.getTagIds()));
            }
        }
        payload = globalUploadAPI.getPayload(filesToUploadJson);
        return payload;
    }

    @Test(dataProvider = "globalUploadDataProvider")
    public void TestGlobalUploadAPI(GlobalUploadDTO globalUploadDTO){
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = globalUploadDTO.getTestCaseId();
        String payload;
        try {
            String description = globalUploadDTO.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);

            // Verifying with mandatory parameters
            payload = getPayloadForMandateFields(globalUploadDTO);
            HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(),payload);
            String globalUploadResponseStr = EntityUtils.toString(httpResponse.getEntity());
            csAssert.assertTrue(JSONUtility.validjson(globalUploadResponseStr),"Not a valid Json");
            JSONObject globalUploadJsonObject = new JSONObject(globalUploadResponseStr.trim());
            csAssert.assertTrue(globalUploadJsonObject.get("responseCode").equals(globalUploadDTO.getExpectedStatusCode()),"Response Code is not Valid");

            if(globalUploadDTO.getErrors().equals("null")){
            csAssert.assertTrue(globalUploadJsonObject.get("errors").toString().equals(globalUploadDTO.getErrors()),"There are errors while global upload");}
            else{
                csAssert.assertTrue(globalUploadJsonObject.getJSONArray("errors").toString().contains(globalUploadDTO.getErrors()),"There are errors while global upload");
            }

            csAssert.assertTrue(globalUploadJsonObject.get("success").toString().equals(globalUploadDTO.getIsGlobalUploadSuccess()),"Global Upload is not successful");

            AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password"));
            // Verifying with all parameters
            payload = getPayloadForAllFields(globalUploadDTO);
            httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(),payload);
            globalUploadResponseStr = EntityUtils.toString(httpResponse.getEntity());
            csAssert.assertTrue(JSONUtility.validjson(globalUploadResponseStr),"Not a valid Json");
            globalUploadJsonObject = new JSONObject(globalUploadResponseStr.trim());
            String statusCodeInResponse = (String) globalUploadJsonObject.get("responseCode");
            String statusCodeExpected = globalUploadDTO.getExpectedStatusCode();

            csAssert.assertTrue(globalUploadJsonObject.get("responseCode").equals(globalUploadDTO.getExpectedStatusCode()),"Response Code is not Valid");

            if(globalUploadDTO.getErrors().equals("null")){
            csAssert.assertTrue(globalUploadJsonObject.get("errors").toString().equals(globalUploadDTO.getErrors()),"There are errors while global upload");}
            else{
                csAssert.assertTrue(globalUploadJsonObject.getJSONArray("errors").toString().contains(globalUploadDTO.getErrors()),"There are errors while global upload");
            }
            csAssert.assertTrue(globalUploadJsonObject.get("success").toString().equals(globalUploadDTO.getIsGlobalUploadSuccess()),"Global Upload is not successful");
            AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password"));
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @BeforeTest
    public void beforeTestMethod()
    {
        String dataFilePath = "src/test/resources/TestConfig/APITestData/AutoExtraction/GlobalUpload/";
        String dataFileName = "globalDataAPI.json";

        try
        {
            FileOutputStream fos = new FileOutputStream(dataFilePath+dataFileName);

            JSONArray arrFromJsonFile = globalUploadAPI.testCreateJsonFileData();
            String fileCon = arrFromJsonFile.toString();
            fos.write(fileCon.getBytes());

        } catch(FileNotFoundException ffe) {
            ffe.printStackTrace();
        }
        catch (IOException ie){
            ie.printStackTrace();
        }

    }




}
