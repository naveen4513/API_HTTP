package com.sirionlabs.test.api.AutoExtraction.ContractDocumentGetEntities;

import com.sirionlabs.api.autoExtraction.API.ContractDocumentGetEntities.ContractDocumentGetEntitiesAPI;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.dto.AutoExtraction.ContractDocumentGetEntities.ContractDocumentGetEntitiesDTO;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.test.autoExtraction.TestContractCreationAPI;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ContractDocumentGetEntitiesAPITest {

    private final static Logger logger = LoggerFactory.getLogger(ContractDocumentGetEntitiesAPITest.class);
    private static String configAutoExtractionFilePath;
    private static String configAutoExtractionFileName;
    private static String templateFilePath;
    private static String templateFileName;
    private static String contractCreationConfigFilePath;
    private static String contractCreationConfigFileName;
    private static String relationId;
    private static String entity;
    private static String postgresHost;
    private static String postgresPort;
    private static String postgresDbName;
    private static String postgresDbUsername;
    private static String postgresDbPassword;

    @BeforeClass
    public void beforeClass(){
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"contract document get entities docx document", "fileuploadpath");
        templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "contract document get entities docx document","fileuploadname");
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
        relationId = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "contracts","sourceid");
        entity = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        postgresHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "host");
        postgresPort = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "port");
        postgresDbName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "dbname");
        postgresDbUsername = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "username");
        postgresDbPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"postres sirion db details", "password");
    }

    @DataProvider(name = "contractDocumentGetEntitiesDataProvider")
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/APITestData/AutoExtraction/ContractDocumentGetEntities";
        String dataFileName = "contractDocumentGetEntitiesAPI.json";

        List<ContractDocumentGetEntitiesDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);
                ContractDocumentGetEntitiesDTO dtoObject = getUpdateDTOObjectFromJson(jsonObj);

                if (dtoObject != null) {
                    dtoObjectList.add(dtoObject);
                }
        }

        for (ContractDocumentGetEntitiesDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private ContractDocumentGetEntitiesDTO getUpdateDTOObjectFromJson(JSONObject jsonObj) {
        ContractDocumentGetEntitiesDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            boolean validAuthorization = jsonObj.getBoolean("validAuthorization");
            String acceptHeader = jsonObj.getString("acceptHeader");
            boolean validAcceptHeader = jsonObj.getBoolean("validAcceptHeader");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            dtoObject = new ContractDocumentGetEntitiesDTO(testCaseId, description,validAuthorization, acceptHeader,validAcceptHeader,expectedStatusCode);
        } catch (Exception e) {
            logger.error("Exception while Getting GlobalUpload DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "contractDocumentGetEntitiesDataProvider")
    public void TestContractDocumentGetEntitiesAPI(ContractDocumentGetEntitiesDTO contractDocumentGetEntitiesDTO){
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = contractDocumentGetEntitiesDTO.getTestCaseId();

        try {
            String description = contractDocumentGetEntitiesDTO.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            int contractTreeDocId = hitContractDocumentUploadAPI();
            String apiPath =ContractDocumentGetEntitiesAPI.getApiPath(contractTreeDocId,1,"V1");
            String authorization;
            if(contractDocumentGetEntitiesDTO.getAuthorization() == true && contractDocumentGetEntitiesDTO.getValidAcceptHeader() == true) {
                authorization = Check.getAuthorization();
                int automationListingDocId =waitForExtractionToComplete(contractTreeDocId,apiPath,authorization,contractDocumentGetEntitiesDTO,csAssert);

                HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                HashMap<String, String> contractDocumentGetEntitiesResponseMap = ContractDocumentGetEntitiesAPI.getContractDocumentGetEntitiesResponse(httpResponse);
                csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("active").equals("true"), "Document Link Should be active");
                csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("type").equals("URL"), "Document Type Should be URL");
                csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("url").equals("/show/autoExtractionDocuments/" + automationListingDocId), "Document URL Should not be empty");
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(), "Contract Document Get Entities Response Code is not valid");
            }
            else if(contractDocumentGetEntitiesDTO.getAuthorization() == false && contractDocumentGetEntitiesDTO.getValidAcceptHeader() == true){
                authorization = Check.getAuthorization() + RandomString.getRandomAlphaNumericString(10);
                HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(),"Response Code should be " + contractDocumentGetEntitiesDTO.getExpectedStatusCode() + " as Authorization is invalid");

                // waiting for extraction to complete
                authorization = Check.getAuthorization();
                waitForExtractionToComplete(contractTreeDocId,apiPath,authorization,contractDocumentGetEntitiesDTO,csAssert);
            }
            else if(contractDocumentGetEntitiesDTO.getAuthorization() == true && contractDocumentGetEntitiesDTO.getValidAcceptHeader() == false){
                authorization = Check.getAuthorization();
                HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(),"Response Code should be " + contractDocumentGetEntitiesDTO.getExpectedStatusCode() + " as Accept Header is invalid");

                // waiting for extraction to complete
                authorization = Check.getAuthorization();
                waitForExtractionToComplete(contractTreeDocId,apiPath,authorization,contractDocumentGetEntitiesDTO,csAssert);
            }
        }
        catch (Exception e){
            logger.info("Contract Document Get Entities API is getting Failed " + e.getStackTrace());

        }
        csAssert.assertAll();
    }

    public int waitForExtractionToComplete(int contractTreeDocId,String apiPath,String authorization,ContractDocumentGetEntitiesDTO contractDocumentGetEntitiesDTO,CustomAssert csAssert) throws SQLException, IOException {

        int automationListingDocId = 0;
        if(ConfigureEnvironment.environment.contains("sandbox")){
            try {
                AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query,payload);
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson =  new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            for (String key : keys) {
                if(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("documentname")){
                    automationListingDocId = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1].trim());
                    break;
                }
            }
        }
        else {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            // Verify Scheduler status and get automation listing document id from scheduler table
            String query = "SELECT  id,status_id FROM autoextraction_document_request WHERE document_id = " + contractTreeDocId + ";";
            List<List<String>> schedulerData = postgreSQLJDBC.doSelect(query);
            int statusId = Integer.valueOf(schedulerData.get(0).get(1));
            automationListingDocId = Integer.valueOf(schedulerData.get(0).get(0));

            // Check Whether Scheduler has picked the document for extraction or not
            LocalTime initialTime = LocalTime.now();
            while (statusId != 2) {
                logger.info("Scheduler hasn't picked yet " + statusId);
                schedulerData = postgreSQLJDBC.doSelect(query);
                statusId = Integer.valueOf(schedulerData.get(0).get(1));

                if (statusId == 3) {
                    throw new SkipException("Document will not be picked by scheduler as task is failed");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Scheduler to pick the document for extraction = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Waited for 10 minutes for Scheduler to pick the document." +
                            "Please look manually whether their is problem in Scheduler to pick the document." +
                            "For document id " + automationListingDocId + " in Automation Listing and document id " + contractTreeDocId + " in Contract Tree");
                }
            }

            // Get extraction status from extraction status table
            query = "SELECT ads.document_id, min(extraction_status) as status  FROM  autoextraction_document_status ads where ads.document_id=" + automationListingDocId + " GROUP BY ads.document_id";
            List<List<String>> documentStatusData = postgreSQLJDBC.doSelect(query);

            int documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));

            initialTime = LocalTime.now();
            while (documentExtractionStatus != 4) {
                documentStatusData = postgreSQLJDBC.doSelect(query);

                documentExtractionStatus = Integer.valueOf(documentStatusData.get(0).get(1));
                if (documentExtractionStatus == 1) {
                    logger.info("Document is submitted for Auto-Extraction");
                    HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                    HashMap<String, String> contractDocumentGetEntitiesResponseMap = ContractDocumentGetEntitiesAPI.getContractDocumentGetEntitiesResponse(httpResponse);
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("active").equals("false"), "Document Link Should not be active");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("type").equals("URL"), "Document Type Should be URL");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("url").equals(""), "Document URL Should not be empty");
                    csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(), "Contract Document Get Entities Response Code is not valid");
                } else if (documentExtractionStatus == 2) {
                    logger.info("Document is in pre-processing stage");
                    HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                    HashMap<String, String> contractDocumentGetEntitiesResponseMap = ContractDocumentGetEntitiesAPI.getContractDocumentGetEntitiesResponse(httpResponse);
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("active").equals("false"), "Document Link Should not be active");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("type").equals("URL"), "Document Type Should be URL");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("url").equals(""), "Document URL Should not be empty");
                    csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(), "Contract Document Get Entities Response Code is not valid");
                } else if (documentExtractionStatus == 3) {
                    logger.info("Document is in post-processing stage");
                    HttpResponse httpResponse = ContractDocumentGetEntitiesAPI.hitContractDocumentGetEntitiesAPI(apiPath, authorization, contractDocumentGetEntitiesDTO.getAcceptHeader());
                    HashMap<String, String> contractDocumentGetEntitiesResponseMap = ContractDocumentGetEntitiesAPI.getContractDocumentGetEntitiesResponse(httpResponse);
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("active").equals("false"), "Document Link Should not be active");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("type").equals("URL"), "Document Type Should be URL");
                    csAssert.assertTrue(contractDocumentGetEntitiesResponseMap.get("url").equals(""), "Document URL Should not be empty");
                    csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == contractDocumentGetEntitiesDTO.getExpectedStatusCode(), "Contract Document Get Entities Response Code is not valid");
                }
                LocalTime finalTime = LocalTime.now();
                Duration duration = Duration.between(initialTime, finalTime);
                logger.info("Waiting for Extraction to complete Wait Time = " + duration.getSeconds());

                if (duration.getSeconds() > 600) {
                    throw new SkipException("Extraction is working slow already waited for 10 minutes." +
                            "Please look manually whether their is problem in extraction or services are working slow." +
                            "For document id " + automationListingDocId);
                }
            }
        }
        return automationListingDocId;
    }

    public int  hitContractDocumentUploadAPI() throws IOException {
        int docId =0;
        try {
            int contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileName, entity, templateFilePath, templateFileName, relationId, true);
            String apiPath = ContractShow.getAPIPath();
            apiPath = String.format(apiPath + contractId);
            HttpGet httpGet = new HttpGet(apiPath);
            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
            httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            HttpResponse newlyCreatedContractShowResponse = APIUtils.getRequest(httpGet);
            String newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());
            JSONObject jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);

            docId = (int) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("contractDocuments").getJSONArray("values").getJSONObject(0).get("id");
        }
        catch (Exception e){
            logger.info("Exception while hitting contract document upload api " + e.getStackTrace());
        }
        return docId;
    }

}
