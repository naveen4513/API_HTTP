package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.API.GlobalUpload.globalUploadAPI;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.autoExtraction.SaveTrainingInputData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.AutoExtraction.GlobalUpload.GlobalUploadDTO;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.TestContractDocumentUpload;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomString;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import static com.sirionlabs.test.autoExtraction.TestAutoExtractionAPIs.clientId;

public class AutoExtractionSmoke extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionSmoke.class);
    private static String autoExtractionServiceHostUrl;
    private static String autoExtractionUsProdServiceHostUrl;
    private static String autoExtractionVFProdServiceHostUrl;
    private static String autoExtractionEUProdServiceHostUrl;
    private static String autoExtractionUKProdServiceHostUrl;
    private static String autoExtractionAUSProdServiceHostUrl;
    private static String autoExtractionVFSandboxServiceHostUrl;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    int newlyCreatedProjectId;
    String drsFlag;
    CustomAssert csAssert = new CustomAssert();



    @BeforeClass
    public void beforeClass() {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");

        /*During VPC Migration AE service has been moved to Jump Server, so currently we are unable to do a Health check of AE service as IP is not accessible from VPN
        commented all the Host URL*/

        autoExtractionUsProdServiceHostUrl = "ssh -i ~/.ssh/id_rsa naveen.gupta@"+ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae us production", "jumpserver")+"" +
                "\"curl -s http://"+ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae us production", "pulsarserver")+":8080" +
                "/admin/v2/persistent/public/default/"+ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae vf production", "topicname")+"/stats " +
                "| jq '.subscriptions | .[] | .msgBacklog'\"";

        /*autoExtractionVFProdServiceHostUrl = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae vf production", "scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae vf production", "hostname") + ":" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "ae vf production", "port");*/

        /*autoExtractionEUProdServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae eu production","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae eu production","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae eu production","port");*/

        /*autoExtractionUKProdServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae uk production","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae uk production","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae uk production","port");*/

        /*autoExtractionAUSProdServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae aus production","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae aus production","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae aus production","port");*/

        /*autoExtractionServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae environment","port");*/

        /*autoExtractionVFSandboxServiceHostUrl =ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae vf sandbox","scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae vf sandbox","hostname") + ":"+
                ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath,configAutoExtractionFileName,"ae vf sandbox","port");*/
    }
    @BeforeTest
    public void checkDRSFlagValue() throws IOException {
        AutoExtractionSmoke obj = new AutoExtractionSmoke();
         drsFlag = obj.checkDRSFlag();
    }

    @Parameters("Environment")
    @Test(priority = 1)
    public void TestServiceCheckAPI(String environment) throws IOException
    {
        /*try{
        *//*if (environment.equals("Prod/AEUS")) {
            APIValidator response = executor.get(autoExtractionUsProdServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");

        }
        else if (environment.equals("Prod/AEVF")) {
            APIValidator response = executor.get(autoExtractionVFProdServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");*//*
        }
        *//*else if (environment.equals("autoextraction_sandbox"))
        {
            APIValidator response = executor.get(autoExtractionServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");
        }*//*
        *//*else if (environment.equals("Prod/AEUK"))
        {
            APIValidator response = executor.get(autoExtractionUKProdServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");
        }*//*
        *//*else if (environment.equals("Prod/AEEU"))
        {
            APIValidator response = executor.get(autoExtractionEUProdServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");
        }*//*
        *//*else if (environment.equals("Prod/AEAUS"))
        {
            APIValidator response = executor.get(autoExtractionAUSProdServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");
        }*//*
        *//*else if (environment.equals("Sandbox/AEVF"))
        {
            APIValidator response = executor.get(autoExtractionVFSandboxServiceHostUrl, "/autoExtraction", null);
            response.validateResponseCode(200, csAssert);
            csAssert.assertTrue(response.getResponse().getResponseCode() == 200,
                    "AutoExtraction Service is not up and running");
        }*//*
        }
        catch (Exception e)
        {
            logger.info("Error while validating AE service is up or not");
            csAssert.assertTrue(false, e.getMessage());
        }

        csAssert.assertAll();*/

    }

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
            String errors = jsonObj.getString("errors");
            String isSuccess = jsonObj.getString("success");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");

            dtoObject = new GlobalUploadDTO(testCaseId, description, useJsonParam, key, extension, name, numberOfFiles, projectIds, groupIds, tagIds, errors, isSuccess, expectedStatusCode);
        } catch (Exception e) {
            logger.error("Exception while Getting GlobalUpload DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    public Map<String, String> fileUpload() {
        String filePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
        String fileName = "KAMADALTD_DRSADraftR_3252013.docx";
        Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUpload(filePath, fileName);
        return uploadedFileProperty;
    }

    public Map<String,String> fileUploadDRS()
    {
        String filePath = "src/test/resources/TestConfig/AutoExtraction/UploadFiles";
        String fileName = "KAMADALTD_DRSADraftR_3252013.docx";
        Map<String, String> uploadedFileProperty = TestContractDocumentUpload.fileUploadForDRS(filePath, fileName);
        return uploadedFileProperty;
    }


    public String getPayloadForMandateFields(GlobalUploadDTO globalUploadDTO) throws IOException {
        String payload;
        boolean useJsonParam = globalUploadDTO.getUseJsonParam();
        List<JSONObject> filesToUploadJson = new LinkedList<>();
        int filesToUpload = globalUploadDTO.getNumberOfFiles();
        Map<String, String> fileProperties;
        String key = null;
        int pageNo = 0;
        for (int i = 0; i < filesToUpload; i++) {
            if(drsFlag.equalsIgnoreCase("true"))
            {
                fileProperties = fileUploadDRS();
                String keyAndPages = fileProperties.get("filePathOnServer");
                JSONObject keyJson = new JSONObject(keyAndPages);
                key = (String) keyJson.get("documentId");
                pageNo = (int) keyJson.get("noOfPages");
            }
            else {
                fileProperties = fileUpload();
            }
            if (useJsonParam == false && drsFlag.equalsIgnoreCase("false")) {

                filesToUploadJson.add(globalUploadAPI.createJson(fileProperties.get("extension"), fileProperties.get("key"), fileProperties.get("name"), globalUploadDTO.getProjectIds()));

            }
            else if(useJsonParam == false && drsFlag.equalsIgnoreCase("true"))
            {
                filesToUploadJson.add(globalUploadAPI.createJson(fileProperties.get("extension"),key,fileProperties.get("name"), pageNo,globalUploadDTO.getProjectIds()));

            }
            else {
                filesToUploadJson.add(globalUploadAPI.createJson(globalUploadDTO.getExtension(), globalUploadDTO.getKey(), globalUploadDTO.getName()));
            }
        }
        payload = globalUploadAPI.getPayload(filesToUploadJson);
        return payload;
    }

    public String getPayloadForAllFields(GlobalUploadDTO globalUploadDTO) throws IOException {
        String payload;
        boolean useJsonParam = globalUploadDTO.getUseJsonParam();
        List<JSONObject> filesToUploadJson = new LinkedList<>();
        int filesToUpload = globalUploadDTO.getNumberOfFiles();
        Map<String, String> fileProperties;
        for (int i = 0; i < filesToUpload; i++) {

            if(drsFlag.equalsIgnoreCase("true")) {
                fileProperties = fileUpload();
            }
            else{
                fileProperties = fileUploadDRS();

            }
            if (useJsonParam == false) {
                filesToUploadJson.add(globalUploadAPI.createJson(fileProperties.get("extension"), fileProperties.get("filePathOnServer"), fileProperties.get("name"), globalUploadDTO.getProjectIds(), globalUploadDTO.getGroupIds(), globalUploadDTO.getTagIds()));
            } else {
                filesToUploadJson.add(globalUploadAPI.createJson(globalUploadDTO.getExtension(), globalUploadDTO.getKey(), globalUploadDTO.getName(), globalUploadDTO.getProjectIds(), globalUploadDTO.getGroupIds(), globalUploadDTO.getTagIds()));
            }
        }
        payload = globalUploadAPI.getPayload(filesToUploadJson);
        return payload;
    }

    public String checkDRSFlag() throws IOException {
        HttpResponse sideLayoutResponse = AutoExtractionHelper.sideLayoutAPI();
        csAssert.assertTrue(sideLayoutResponse.getStatusLine().getStatusCode()==200,"Response code is Invalid");
        String sideLayoutStr = EntityUtils.toString(sideLayoutResponse.getEntity());
        JSONObject sideLayoutJson = new JSONObject(sideLayoutStr);
        String drsFlagValue = (String) sideLayoutJson.getJSONObject("data").getJSONObject("rightSideBar").getJSONObject("springProperties").get("drsEnabled");
        return drsFlagValue;
    }

    @Test(dataProvider = "globalUploadDataProvider",dependsOnMethods = {"TestServiceCheckAPI"},enabled = true,priority = 3)
    public void TestGlobalUploadAPI(GlobalUploadDTO globalUploadDTO) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = globalUploadDTO.getTestCaseId();
        String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(18);
        String payload;
        Map<String,String> uploadedFileProperty;
        try {
            String description = globalUploadDTO.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            AutoExtractionSmoke uploadObj = new AutoExtractionSmoke();
            if(drsFlag.equalsIgnoreCase("true"))
            {
                uploadedFileProperty = uploadObj.fileUploadDRS();
            }
            else {
                uploadedFileProperty = uploadObj.fileUpload();
            }
            // Verifying with mandatory parameters
            if(testCaseId.equalsIgnoreCase("1")||testCaseId.equalsIgnoreCase("2")) {
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    String keyAndPages = uploadedFileProperty.get("filePathOnServer");
                    JSONObject keyJson = new JSONObject(keyAndPages);
                    String key = (String) keyJson.get("documentId");
                    int pageNo = (int) keyJson.get("noOfPages");
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + key + "\",\"name\": \"" + uploadedFileProperty.get("name") +"\",\"noOfPages\":\""+pageNo+ "\" ,\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
            }
            else {
                payload = getPayloadForMandateFields(globalUploadDTO);
            }

            HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
            String globalUploadResponseStr = EntityUtils.toString(httpResponse.getEntity());
            csAssert.assertTrue(JSONUtility.validjson(globalUploadResponseStr), "Not a valid Json");
            JSONObject globalUploadJsonObject = new JSONObject(globalUploadResponseStr.trim());
            csAssert.assertTrue(globalUploadJsonObject.get("responseCode").equals(globalUploadDTO.getExpectedStatusCode()), "Response Code is not Valid");

            if (globalUploadDTO.getErrors().equals("null")) {
                csAssert.assertTrue(globalUploadJsonObject.get("errors").toString().equals(globalUploadDTO.getErrors()), "There are errors while global upload");
            } else {
                csAssert.assertTrue(globalUploadJsonObject.getJSONArray("errors").toString().contains(globalUploadDTO.getErrors()), "There are errors while global upload");
            }

            csAssert.assertTrue(globalUploadJsonObject.get("success").toString().equals(globalUploadDTO.getIsGlobalUploadSuccess()), "Global Upload is not successful");

            AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
            // Verifying with all parameters
            if(testCaseId.equalsIgnoreCase("1")||testCaseId.equalsIgnoreCase("2")) {
                if(drsFlag.equalsIgnoreCase("true"))
                {
                    String keyAndPages = uploadedFileProperty.get("filePathOnServer");
                    JSONObject keyJson = new JSONObject(keyAndPages);
                    String key = (String) keyJson.get("documentId");
                    int pageNo = (int) keyJson.get("noOfPages");
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + key + "\",\"name\": \"" + uploadedFileProperty.get("name") +"\",\"noOfPages\":\""+pageNo+ "\" ,\"projectIds\":[" + newlyCreatedProjectId + "]}]";
                }
                else {
                    payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";
                }
            }
            else {
                payload = getPayloadForMandateFields(globalUploadDTO);
            }
            httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
            globalUploadResponseStr = EntityUtils.toString(httpResponse.getEntity());
            csAssert.assertTrue(JSONUtility.validjson(globalUploadResponseStr), "Not a valid Json");
            globalUploadJsonObject = new JSONObject(globalUploadResponseStr.trim());
            csAssert.assertTrue(globalUploadJsonObject.get("responseCode").equals(globalUploadDTO.getExpectedStatusCode()), "Response Code is not Valid");

            if (globalUploadDTO.getErrors().equals("null")) {
                csAssert.assertTrue(globalUploadJsonObject.get("errors").toString().equals(globalUploadDTO.getErrors()), "There are errors while global upload");
            } else {
                csAssert.assertTrue(globalUploadJsonObject.getJSONArray("errors").toString().contains(globalUploadDTO.getErrors()), "There are errors while global upload");
            }
            csAssert.assertTrue(globalUploadJsonObject.get("success").toString().equals(globalUploadDTO.getIsGlobalUploadSuccess()), "Global Upload is not successful");
            AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true,priority = 4)
    public void ValidateExtractionStatus() throws IOException, InterruptedException
    {
        try {
            CustomAssert csAssert = new CustomAssert();
            boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
            logger.info("Extraction Completed:"+isExtractionCompleted);
            csAssert.assertTrue(isExtractionCompleted=true, "Extraction not Completed for the documents");
        }

        catch (Exception e)
        {
            logger.info("Exception occured while hitting validateExtractionStatus API",e.getStackTrace());
            csAssert.assertTrue(false,e.getMessage());
        }

        csAssert.assertAll();

    }
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true,priority = 5)
    public void TestNonSearchablePDF()throws IOException {
        logger.info("Testing Global Upload for Searchable PDF");
        // File Upload API to get the key of file uploaded
        logger.info("File Uplaod API to get the key of file that has been uploaded");
        try {
            CustomAssert csAssert = new CustomAssert();
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "non searchable pdf document", "fileuploadpath");
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "non searchable pdf document", "fileuploadname");
            Map<String, String> uploadedFileProperty = null;
            String payload="";
            if (drsFlag.equalsIgnoreCase("true")){
                uploadedFileProperty = TestContractDocumentUpload.fileUploadForDRS(templateFilePath,templateFileName);
                String keyAndPages = uploadedFileProperty.get("filePathOnServer");
                JSONObject keyJson = new JSONObject(keyAndPages);
                String key = (String) keyJson.get("documentId");
                int pageNo = (int) keyJson.get("noOfPages");
                payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + key + "\",\"name\": \"" + uploadedFileProperty.get("name") +"\",\"noOfPages\":\""+pageNo+ "\" ,\"projectIds\":[" + newlyCreatedProjectId + "]}]";
            }
            else {
                uploadedFileProperty= TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);
                payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";

            }

            try {
                // Hit Global Upload API
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                boolean isExtractionCompletedForNonSearchablePDF = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForNonSearchablePDF,"Non-Searchable PDF are not getting completed ");
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Global Upload API is not working because:"+ e.getStackTrace());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because :" + e.getStackTrace());
        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true,priority = 6)
    public void TestSearchablePDF()throws IOException {
        logger.info("Testing Global Upload for Searchable PDF");
        // File Upload API to get the key of file uploaded
        logger.info("File Uplaod API to get the key of file that has been uploaded");
        try {
            CustomAssert csAssert = new CustomAssert();
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"searchable pdf document", "fileuploadpath");
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "searchable pdf document","fileuploadname");
            Map<String, String> uploadedFileProperty = null;
            String payload="";
            if (drsFlag.equalsIgnoreCase("true")){
                uploadedFileProperty = TestContractDocumentUpload.fileUploadForDRS(templateFilePath,templateFileName);
                String keyAndPages = uploadedFileProperty.get("filePathOnServer");
                JSONObject keyJson = new JSONObject(keyAndPages);
                String key = (String) keyJson.get("documentId");
                int pageNo = (int) keyJson.get("noOfPages");
                payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + key + "\",\"name\": \"" + uploadedFileProperty.get("name") +"\",\"noOfPages\":\""+pageNo+ "\" ,\"projectIds\":[" + newlyCreatedProjectId + "]}]";
            }
            else {
                uploadedFileProperty= TestContractDocumentUpload.fileUpload(templateFilePath, templateFileName);
                payload = "[{\"extension\": \"" + uploadedFileProperty.get("extension") + "\",\"key\": \"" + uploadedFileProperty.get("key") + "\",\"name\": \"" + uploadedFileProperty.get("name") + "\",\"projectIds\":["+newlyCreatedProjectId+"]}]";

            }

            try {
                // Hit Global Upload API
                HttpResponse httpResponse = globalUploadAPI.hitGlobalUpload(globalUploadAPI.getApiPath(), payload);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code for global Upload API is not valid");
                AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                boolean isExtractionCompletedForSearchablePDF = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForSearchablePDF,"Searchable PDF are not getting completed ");
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Global Upload API is not working beacuse:"+ e.getStackTrace());
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "File Upload API is not working because :" + e.getStackTrace());
        }
        csAssert.assertAll();
    }

    //Automation Listing Page
    @Test(dependsOnMethods = {"TestServiceCheckAPI"},priority = 7)
    public void automationListing() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Testing Automation Listing");
            HttpResponse automationListResponse = AutoExtractionHelper.getListDataForEntities("432", "{\"filterMap\":{}}");
            csAssert.assertTrue(automationListResponse.getStatusLine().getStatusCode() == 200, "Automation List Data Response Code is not valid");
            String automationListStr = EntityUtils.toString(automationListResponse.getEntity());
            JSONObject automationJsonObj = new JSONObject(automationListStr);
            JSONArray jsonArr = automationJsonObj.getJSONArray("data");
            int dataInListingCount = automationJsonObj.getJSONArray("data").length();
            csAssert.assertTrue(dataInListingCount>0,"There is no Data in Automation Listing");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occured on AutoExtraction listing because:" + e.getMessage());
        }
        csAssert.assertAll();
    }


    @Test(dependsOnMethods = {"TestServiceCheckAPI"},enabled = true,priority = 2)
    public void testProjectCreation() throws IOException
    {
        //Creating a New Project from Project Listing Page
        // To Map a Project with fields, Get All the Meta Data Fields in Project to extract
        try {
            HttpResponse metadataFieldResponse = AutoExtractionHelper.getAllMetadataFields();
            csAssert.assertTrue(metadataFieldResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
            String metadataFieldResponseStr = EntityUtils.toString(metadataFieldResponse.getEntity());

            JSONObject metadataFieldResponseJsonStr = new JSONObject(metadataFieldResponseStr);
            int metadataFieldsLength = metadataFieldResponseJsonStr.getJSONArray("response").length();
            HashMap<Integer, String> metadataFields = new LinkedHashMap<>();
            for (int i = 0; i < metadataFieldsLength; i++) {
                metadataFields.put(Integer.valueOf(metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("id").toString()), metadataFieldResponseJsonStr.getJSONArray("response").getJSONObject(i).get("name").toString());
            }

            if (metadataFields.size() < 1) {
                throw new SkipException("Metadata Fields are not there to map it with a Project");
            }

            //Creating a New Project from the list of fields
            try {
                logger.info("Creating a new Project");
                APIResponse projectCreateResponse = ProjectCreationAPI.projectCreateAPIResponse(ProjectCreationAPI.getAPIPath(), ProjectCreationAPI.getHeaders(), ProjectCreationAPI.getPayload());
                Integer projectResponseCode = projectCreateResponse.getResponseCode();
                String projectCreateStr = projectCreateResponse.getResponseBody();
                csAssert.assertTrue(projectResponseCode == 200, "Response Code is Invalid");
                JSONObject projectCreateJson = new JSONObject(projectCreateStr);
                csAssert.assertTrue(projectCreateJson.get("success").toString().equals("true"), "Project is not created successfully");
                newlyCreatedProjectId = Integer.valueOf(projectCreateJson.getJSONObject("response").get("id").toString());
                logger.info("Newly created project is " + newlyCreatedProjectId);

                //Test Case to validate show page of a Project
                try {
                    HttpResponse projectShowResponse = AutoExtractionHelper.testProjectShowpage(newlyCreatedProjectId);
                    csAssert.assertTrue(projectShowResponse.getStatusLine().getStatusCode() == 200, "Project Showpage Response Code is not Valid");
                    String projectShowStr = EntityUtils.toString(projectShowResponse.getEntity());
                    JSONObject projectShowJson = new JSONObject(projectShowStr);
                    int projectIdOnShowpage = (int) projectShowJson.getJSONObject("response").get("id");
                    csAssert.assertTrue(projectIdOnShowpage==newlyCreatedProjectId,"Project Showpage is not opening");

                    //Updating an existing Project
                    try {
                        String allMetadataFields = ProjectCreationAPI.getAllMetadataFields();
                        String projectUpdate = "Update Automation" + RandomString.getRandomAlphaNumericString(10);
                        String updateProjectPayload = "{\"name\":\"" + projectUpdate + "\",\"description\":\" Update Automation Project\",\"projectLinkedFieldIds\":["+allMetadataFields+"],\"id\":\""+newlyCreatedProjectId+"\",\"clientId\":" + clientId +"}";
                        HttpResponse projectUpdateResponse = AutoExtractionHelper.projectUpdateAPI(updateProjectPayload);
                        csAssert.assertTrue(projectUpdateResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
                        String projectUpdateStr = EntityUtils.toString(projectUpdateResponse.getEntity());
                        JSONObject projectUpdateJson = new JSONObject(projectUpdateStr);
                        String updatedName = (String) projectUpdateJson.getJSONObject("response").get("name");
                        csAssert.assertTrue(updatedName.equals(projectUpdate),"Project Name is not getting updated");

                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false,"Exception occured while updating a Project" + e.getMessage());
                    }
                }

                catch (Exception e)
                {
                    csAssert.assertTrue(false,"Exception occured on project showpage" +e.getMessage());
                }
            }

            catch (Exception e)
            {
                csAssert.assertTrue(false,"Exception occured while creating a Project" + e.getMessage());
            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occured while fetching all the metadata fields for the client " + e.getMessage());
        }
        csAssert.assertAll();

    }



    @BeforeTest
    public void beforeTestMethod() {
        String dataFilePath = "src/test/resources/TestConfig/APITestData/AutoExtraction/GlobalUpload/";
        String dataFileName = "globalDataAPI.json";

        try {
            FileOutputStream fos = new FileOutputStream(dataFilePath + dataFileName);

            JSONArray arrFromJsonFile = globalUploadAPI.testCreateJsonFileData();
            String fileCon = arrFromJsonFile.toString();
            fos.write(fileCon.getBytes());

        } catch (FileNotFoundException ffe) {
            ffe.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }

    }
}

