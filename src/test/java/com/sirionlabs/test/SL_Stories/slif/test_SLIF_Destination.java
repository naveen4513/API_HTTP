package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class test_SLIF_Destination extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_SLIF_Destination.class);

    private String configFilePath;
    private String configFileName;
    private String hostURL;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIFConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIFConfigFileName");

        hostURL = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"scheme") + "://" +
                ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"hostip") + ":" +
                ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"hostport") +
                "/slif";
    }

    //Completed
    @Test(priority = 0)
    public void Test_IntegrationConfiguration_APIs(){

        CustomAssert customAssert = new CustomAssert();

        int clientId = 1005;
        String apiUrl = "/integration-configurations/destination/" + clientId;
        Boolean validationStatus;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            String sourceIntegrationConfigurationJsonString;

            String currentDate = DateUtils.getCurrentDateInAnyFormat("dd-MM-yyyy");
            String prevDate = DateUtils.getPreviousDateInDDMMYYYY(currentDate);

            String prevDateIn_YYYY_MM_DD = "";

            String [] prevDateArray = prevDate.split("-");

            for(int i = prevDateArray.length -1;i>=0;i--){
                prevDateIn_YYYY_MM_DD  += prevDateArray[i] + "-";
            }
            prevDateIn_YYYY_MM_DD = prevDateIn_YYYY_MM_DD.substring(0,prevDateIn_YYYY_MM_DD.length() -1);

            sourceIntegrationConfigurationJsonString = "{\"description\":\"api_automation test\",\"email\":\"kushmeet.saluja@sirionlabs.com\",\"frequency\":2,\"frequencyType\":1,\"lastSyncDate\":\"" + prevDateIn_YYYY_MM_DD + "T08:13:14.553458Z\",\"name\":\"api_automation test\",\"password\":\"h7xhVCvy9EDX\",\"restEndPoint\":{\"limitParam\":\"sysparm_limit\",\"newDataParam\":\"sys_updated_on\",\"offsetParam\":\"sysparm_offset\",\"requestParams\":{\"fields\":\"priority,state,number,assigned_to\",\"dateFilter\":\"sys_updated_on\",\"displayValue\":\"true\",\"sysparm_query\":\"state=7\",\"excludeReferenceLink\":true},\"uniqueDataParam\":\"number\",\"url\":\"https://dev67347.service-now.com/api/now/table/incident\"},\"restEndPointSchema\":{\"id\":1,\"jsonSchema\":{\"limitParam\":{\"default\":\"sysparam_limit\",\"description\":\"Request parameter for defining response page limit\",\"title\":\"Limit Parameter\",\"type\":\"string\"},\"newDataParam\":{\"default\":\"sys_updated_on\",\"description\":\"Column for filtering new data records.\",\"title\":\"New Data Criteria\",\"type\":\"string\"},\"offsetParam\":{\"default\":\"sysparam_offset\",\"description\":\"Request parameter for defining response page offset.\",\"title\":\"Offset Parameter\",\"type\":\"string\"},\"requestParams\":{\"sysparam_query\":{\"title\":\"Filter Query\",\"description\":\"Encoded query used to filter the result set.\",\"type\":\"string\"},\"sysparam_fields\":{\"title\":\"Response Fields\",\"description\":\"Comma-separated list of field names to return in the response.\",\"type\":\"string\"}},\"uniqueDataParam\":{\"default\":\"number\",\"description\":\"Column for uniquely identifying a record.\",\"title\":\"Unique Data Criteria\",\"type\":\"string\"},\"url\":{\"description\":\"ServiceNow Rest API request URL.\",\"title\":\"URL\",\"type\":\"string\"}},\"name\":\"service now\",\"sourceTypeId\":1},\"startSyncDate\":\"" +  prevDateIn_YYYY_MM_DD + "T08:13:14.553458Z\",\"status\":3,\"user\":\"admin\"}";

            logger.info("Testing the API Url for POST Method");
            APIResponse response = executor.post(hostURL,apiUrl,headers,sourceIntegrationConfigurationJsonString).getResponse();

            if(response.getResponseCode() !=201){
                logger.error("API Response Code is not equal to 200");
            }
            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            JSONObject responseJson = new JSONObject(responseBody);

            int configIdCreated = Integer.parseInt(responseJson.get("configId").toString());

            String newApiUrl = apiUrl + "/" + configIdCreated;
            response = executor.get(hostURL,newApiUrl,headers).getResponse();
            responseJson = new JSONObject(response.getResponseBody());

            String status = responseJson.get("status").toString();

            if(!status.equalsIgnoreCase("1")){
                logger.error("Expected status after source creation : 1 but actual status " + status);
                customAssert.assertTrue(false,"Expected status after source creation : 1 but actual status " + status);
            }

            logger.info("Completed Testing the API Url for POST Method");

            validationStatus = validateValidateAPI(newApiUrl,customAssert);
            if(!validationStatus){
                logger.error("Validation of validate API unsuccessful");
                customAssert.assertTrue(false,"Validation of validate API unsuccessful");
            }

//            ******************************************************************************************************888
            validationStatus = validateGetAPIForAllSourceId(apiUrl,configIdCreated,customAssert);

            if(!validationStatus){
                logger.error("Get API Validation unsuccessful for All Source ID");
                customAssert.assertTrue(false,"Get API Validation unsuccessful for All Source ID");
            }

//            ******************************************************************************************************888

            logger.info("Testing the API for Specific Source Id");
            newApiUrl = apiUrl + "/" + configIdCreated;
            validationStatus = validateGetAPIForSpecificSourceId(newApiUrl,customAssert);

            if(!validationStatus){
                logger.error("Get API Validation unsuccessful for specific Source ID");
                customAssert.assertTrue(false,"Get API Validation unsuccessful for specific Source ID");
            }

            validationStatus = validatePutAPI(newApiUrl,customAssert);

            if(!validationStatus){
                logger.error("Put API Validation unsuccessful");
                customAssert.assertTrue(false,"Put API Validation unsuccessful");
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test
    public void Test_DestinationCount(){

        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            headers.put("accept","*/*");

            int clientId = 1005;

            String apiUrl = "integration-configurations/destination/count/" + clientId;

            APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();

            System.out.println("abc");


        }catch (Exception e){

        }

        customAssert.assertAll();
    }

    @Test
    public void Test_DestinationTimeZone(){

        CustomAssert customAssert = new CustomAssert();

        try{

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            headers.put("accept","*/*");

            String apiUrl = "/integration-configurations/destination/timezone";

            APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();

            System.out.println("");

        }catch (Exception e){

        }

        customAssert.assertAll();
    }

    private Boolean validateGetAPIForAllSourceId(String apiUrl,int configIdCreated,CustomAssert customAssert){

        logger.info("Testing the API URL for Get Method All Config Ids");

        Boolean validationStatus = true;

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");

        APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();

        if(response.getResponseCode() !=200){
            logger.error("API Response Code is not equal to 200");
            validationStatus = false;
        }
        String responseBody = response.getResponseBody();

        if(!APIUtils.validJsonResponse(responseBody)){
            logger.error("API Response is not a valid Json");
            customAssert.assertTrue(false,"API Response is not a valid Json");
            validationStatus = false;
        }

        JSONArray responseBodyJsonArray = new JSONArray(responseBody);
        int actualId;
        Boolean fetchAllSourceIdContainsNewlyCreatedId = false;

        for(int i=0;i<responseBodyJsonArray.length();i++){

            actualId = Integer.parseInt(responseBodyJsonArray.getJSONObject(i).get("id").toString());

            if(actualId == configIdCreated){
                fetchAllSourceIdContainsNewlyCreatedId = true;
                break;
            }

        }

        if(!fetchAllSourceIdContainsNewlyCreatedId){
            logger.error("API doesn't Contains newly created id for the particular client id");
            customAssert.assertTrue(false,"API doesn't Contains newly created id for the particular client id");
            validationStatus = false;
        }

        logger.info("Completed Testing the API Url for Get Method For All Source Id for particular Client ID");

        return validationStatus;
    }

    private Boolean validateValidateAPI(String apiUrl,CustomAssert customAssert){

        Boolean validationStatus = true;

        logger.info("Validating the validate API");

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");

        APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();

        if(response.getResponseCode() !=200){
            logger.error("API Response Code is not equal to 200");
            customAssert.assertTrue(false,"API Response Code is not equal to 200");
            validationStatus = false;
        }

        logger.info("Completed Testing the Validate API Url for Get Method For All Particular Source Id for particular Client ID");

        return validationStatus;

    }

    private Boolean validateGetAPIForSpecificSourceId(String newApiUrl,CustomAssert customAssert){

        logger.info("Testing the API for Specific Source Id");

        Boolean validationStatus = true;

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");
        APIResponse response = executor.get(hostURL,newApiUrl,headers).getResponse();

        if(!APIUtils.validJsonResponse(response.getResponseBody())){
            logger.error("API Response is not a valid Json for Specific Source Id");
            customAssert.assertTrue(false,"API Response is not a valid Json for Specific Source Id");
            validationStatus = false;
        }
        logger.info("Completed Testing the API Url for Get Method For Specific Source Id for particular Client ID");

        return validationStatus;

    }

    private Boolean validatePutAPI(String newApiUrl,CustomAssert customAssert){

        logger.info("Testing the API Url for Put Method For Specific Source Id for particular Client ID for updating the source");

        Boolean validationStatus = true;

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");

        String putAPIPayload = executor.get(hostURL,newApiUrl,headers).getResponse().getResponseBody();

        JSONObject putAPIPayloadJson = new JSONObject(putAPIPayload);
        putAPIPayloadJson.remove("dateCreated");
        putAPIPayloadJson.remove("dateModified");
        String updatedName = "updated value";
        String updatedDescription = "updated value";

        putAPIPayloadJson.put("name",updatedName);
        putAPIPayloadJson.put("description",updatedDescription);

        putAPIPayload = putAPIPayloadJson.toString();

        APIResponse response = executor.put(hostURL,newApiUrl,headers,putAPIPayload).getResponse();

        if(response.getResponseCode() != 200){
            logger.error("Update Response is not equal to 200");
            customAssert.assertTrue(false,"Update Response is not equal to 200");
            validationStatus = false;
        }

        response = executor.get(hostURL,newApiUrl,headers).getResponse();

        String responseBody = response.getResponseBody();

        JSONObject responseJson = new JSONObject(responseBody);

        String actualName = responseJson.get("name").toString();
        String actualDescription = responseJson.get("description").toString();

        if(!actualName.equalsIgnoreCase(updatedName)){
            logger.error("Name value for source id not updated");
            customAssert.assertTrue(false,"Name value for source id not updated");
            validationStatus = false;
        }

        if(!actualDescription.equalsIgnoreCase(updatedDescription)){
            logger.error("Description value for source id not updated");
            customAssert.assertTrue(false,"Description value for source id is not updated");
            validationStatus = false;
        }

        logger.info("Completed Testing the API Url for Put Method For Specific Source Id for particular Client ID");

        return validationStatus;

    }



}
