package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class test_SLIF_ClientAdmin_1889 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_SLIF_ClientAdmin_1889.class);

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
        String apiUrl = "/integration-configurations/source/" + clientId;
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

            String dateCreated = "2020-03-04T06:54:07.696Z";
            String dateModified = "2020-03-04T06:54:07.696Z";

//            sourceIntegrationConfigurationJsonString = "{\"description\":\"api_automation test\",\"email\":\"kushmeet.saluja@sirionlabs.com\",\"frequency\":2,\"frequencyType\":1,\"lastSyncDate\":\"" + prevDateIn_YYYY_MM_DD + "T08:13:14.553458Z\",\"name\":\"api_automation test\",\"password\":\"h7xhVCvy9EDX\",\"restEndPoint\":{\"limitParam\":\"sysparm_limit\",\"newDataParam\":\"sys_updated_on\",\"offsetParam\":\"sysparm_offset\",\"requestParams\":{\"fields\":\"priority,state,number,assigned_to\",\"dateFilter\":\"sys_updated_on\",\"displayValue\":\"true\",\"sysparm_query\":\"state=7\",\"excludeReferenceLink\":true},\"uniqueDataParam\":\"number\",\"url\":\"https://dev67347.service-now.com/api/now/table/incident\"},\"restEndPointSchema\":{\"id\":1,\"jsonSchema\":{\"limitParam\":{\"default\":\"sysparam_limit\",\"description\":\"Request parameter for defining response page limit\",\"title\":\"Limit Parameter\",\"type\":\"string\"},\"newDataParam\":{\"default\":\"sys_updated_on\",\"description\":\"Column for filtering new data records.\",\"title\":\"New Data Criteria\",\"type\":\"string\"},\"offsetParam\":{\"default\":\"sysparam_offset\",\"description\":\"Request parameter for defining response page offset.\",\"title\":\"Offset Parameter\",\"type\":\"string\"},\"requestParams\":{\"sysparam_query\":{\"title\":\"Filter Query\",\"description\":\"Encoded query used to filter the result set.\",\"type\":\"string\"},\"sysparam_fields\":{\"title\":\"Response Fields\",\"description\":\"Comma-separated list of field names to return in the response.\",\"type\":\"string\"}},\"uniqueDataParam\":{\"default\":\"number\",\"description\":\"Column for uniquely identifying a record.\",\"title\":\"Unique Data Criteria\",\"type\":\"string\"},\"url\":{\"description\":\"ServiceNow Rest API request URL.\",\"title\":\"URL\",\"type\":\"string\"}},\"name\":\"service now\",\"sourceTypeId\":1},\"startSyncDate\":\"" +  prevDateIn_YYYY_MM_DD + "T08:13:14.553458Z\",\"status\":3,\"user\":\"admin\"}";
            String name = "Service Now New";
            String password = "AC8S9RokpqaV";
            sourceIntegrationConfigurationJsonString = "{\"name\":\"" + name + "\",\"user\":\"admin\",\"password\":\"" + password + "\",\"email\":\"a@b.com,c@d.com\",\"authenticationType\":1,\"active\":\"false\",\"restEndPoint\":{\"url\":\"https://dev96838.service-now.com/api/now/table/incident\",\"attributes\":{\"sysparm_query\":\"limit\",\"offset_query\":\"offset\"},\"uniqueDataParam\":\"name\",\"newDataParam\":\"name\"},\"restEndPointSchema\":{\"id\":1}}";


            logger.info("Testing the API Url for POST Method");
            APIResponse response = executor.post(hostURL,apiUrl,headers,sourceIntegrationConfigurationJsonString).getResponse();

            if(response.getResponseCode() !=201){
                logger.error("API Response Code is not equal to 201");
                customAssert.assertTrue(false,"API Response Code is not equal to 201");
            }
            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            JSONObject responseJson = new JSONObject(responseBody);

            String configIdCreated = responseJson.get("configId").toString();

            String newApiUrl = apiUrl + "/" + configIdCreated;

            logger.info("Completed Testing the API Url for Get and Post Method");

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
            validationStatus = validateGetAPIForSpecificSourceId(configIdCreated,newApiUrl,customAssert);

            if(!validationStatus){
                logger.error("Get API Validation unsuccessful for specific Source ID");
                customAssert.assertTrue(false,"Get API Validation unsuccessful for specific Source ID");
            }


            validationStatus = validatePatchAPI(newApiUrl,false,customAssert);

            if(!validationStatus){
                logger.error("Delete API Validation unsuccessful");
                customAssert.assertTrue(false,"Patch API Validation unsuccessful");
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

    //Completed
    @Test(enabled = false)
    public void Test_IntegrationConfiguration_GetSourceFrequency_API_Get(){

        CustomAssert customAssert = new CustomAssert();
        String apiUrl = "/integration-configurations/source/frequency";

        try{

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            APIResponse apiResponse = executor.get(hostURL,apiUrl,headers).getResponse();

            int responseCode = apiResponse.getResponseCode();

            if( responseCode != 200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Expected Response Code : 200 Actual Response Code : " + responseCode);
            }

            String responseBody = apiResponse.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {
                int totalNumberOfStatusToBeChecked = 3;
                JSONArray responseArray = new JSONArray(responseBody);

                if(responseArray.length() != totalNumberOfStatusToBeChecked){
                    logger.error("Number of status to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of status present " + responseArray.length());
                    customAssert.assertTrue(false,"Number of frequency to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of frequency present " + responseArray.length());
                }

                int statusChecksPassed = 0;
                String statusName;
                for(int i = 0;i<responseArray.length();i++){
                    statusName = responseArray.getJSONObject(i).get("name").toString();
                    if(statusName.equalsIgnoreCase("Daily") ||
                            statusName.equalsIgnoreCase("Weekly") ||
                            statusName.equalsIgnoreCase("Monthly")){

                        statusChecksPassed =statusChecksPassed + 1;
                    }
                }
                if(statusChecksPassed != totalNumberOfStatusToBeChecked){
                    logger.error("Number of frequency to checked are " + statusChecksPassed + " Actual number Of frequency present " + totalNumberOfStatusToBeChecked);
                    customAssert.assertTrue(false,"Expected Checks " + totalNumberOfStatusToBeChecked + " Actual Checks " + statusChecksPassed);
                }
            }



        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //Completed
    @Test(enabled = false)
    public void Test_IntegrationConfiguration_SourceType_API_Get(){

        CustomAssert customAssert = new CustomAssert();
        int clientId = 1005;
        String apiUrl = "/integration-configurations/source/type/" + clientId;

        try{

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();

            int responseCode = response.getResponseCode();

            if(responseCode != 200){
                customAssert.assertTrue(false,"Expected Response Code : 200 Actual Response Code : " + responseCode);
            }
            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {
                JSONArray responseArray = new JSONArray(responseBody);
                JSONObject actualJsonObject =  responseArray.getJSONObject(0);

                try {
                    String actualId = actualJsonObject.get("id").toString();
                    if (!actualId.equalsIgnoreCase("1")) {
                        customAssert.assertTrue(false,"Expected ID Value : 1 but Actual Value : " + actualId);
                    }
                }catch (Exception e){
                    logger.error("Exception while parsing response Json for key id " + e.getMessage());
                    customAssert.assertTrue(false,"Exception while parsing response Json for key id " + e.getMessage());
                }

                try {
                    String actualName = actualJsonObject.get("name").toString();
                    if (!actualName.equalsIgnoreCase("service now")) {
                        customAssert.assertTrue(false,"Expected name Value : service now but Actual Value : " + actualName);
                    }
                }catch (Exception e){
                    logger.error("Exception while parsing response Json for key name " + e.getMessage());
                    customAssert.assertTrue(false,"Exception while parsing response Json for key name " + e.getMessage());
                }

                try {

                    String actualSourceTypeId = actualJsonObject.get("sourceTypeId").toString();

                    if (!actualSourceTypeId.equalsIgnoreCase("1")) {
                        customAssert.assertTrue(false,"Expected sourceTypeId Value : 1 but Actual Value : " + actualSourceTypeId);
                    }

                }catch (Exception e){
                    logger.error("Exception while parsing response Json for key sourceTypeId " + e.getMessage());
                    customAssert.assertTrue(false,"Exception while parsing response Json for key sourceTypeId " + e.getMessage());
                }

            }



        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //Completed
    @Test(enabled = false)
    public void Test_IntegrationConfiguration_GetSourceSchema_API_Get(){

        CustomAssert customAssert = new CustomAssert();
        int clientId = 1005;
        int schemaId = 0;
        String apiUrl = "/integration-configurations/source/schema/" + clientId + "/" + schemaId;

        try{

            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");
            executor.get(hostURL,apiUrl,headers).getResponse();

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();
    }

//    //Completed It is not in 2.0
//    @Test(enabled = false)
//    public void Test_IntegrationConfiguration_GetSourceFrequencyTypes_API_Get(){
//
//        CustomAssert customAssert = new CustomAssert();
//        String apiUrl = "/integration-configurations/source/frequency-types";
//
//        try{
//            Map<String,String> headers = new HashMap<>();
//            headers.put("Content-Type","application/json");
//            APIResponse apiResponse = executor.get(hostURL,apiUrl,headers).getResponse();
//
//            int responseCode = apiResponse.getResponseCode();
//
//            if( responseCode != 200){
//                logger.error("API Response Code is not equal to 200");
//                customAssert.assertTrue(false,"Expected Response Code : 200 Actual Response Code : " + responseCode);
//            }
//
//            String responseBody = apiResponse.getResponseBody();
//
//            if(!APIUtils.validJsonResponse(responseBody)){
//                customAssert.assertTrue(false,"API Response is not a valid Json");
//            }else {
//                int totalNumberOfStatusToBeChecked = 2;
//                JSONArray responseArray = new JSONArray(responseBody);
//
//                if(responseArray.length() != totalNumberOfStatusToBeChecked){
//                    logger.error("Number of status to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of status present " + responseArray.length());
//                    customAssert.assertTrue(false,"Number of frequency types to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of frequency types present " + responseArray.length());
//                }
//
//                int statusChecksPassed = 0;
//                String statusName;
//                for(int i = 0;i<responseArray.length();i++){
//                    statusName = responseArray.getJSONObject(i).get("name").toString();
//                    if(statusName.equalsIgnoreCase("Regular") ||
//                            statusName.equalsIgnoreCase("Manual")){
//
//                        statusChecksPassed =statusChecksPassed + 1;
//                    }
//                }
//                if(statusChecksPassed != totalNumberOfStatusToBeChecked){
//                    logger.error("Number of frequency types to checked are " + statusChecksPassed + " Actual number Of frequency types present " + totalNumberOfStatusToBeChecked);
//                    customAssert.assertTrue(false,"Expected Checks " + totalNumberOfStatusToBeChecked + " Actual Checks " + statusChecksPassed);
//                }
//            }
//
//        }catch (Exception e){
//            logger.error("Exception while validating API " + e.getMessage());
//            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
//        }
//
//        customAssert.assertAll();
//    }

    //Completed It is not in 2.0
//    @Test
//    public void Test_IntegrationConfiguration_GetSourceStatus_API_Get(){
//
//        CustomAssert customAssert = new CustomAssert();
//        String apiUrl = "/integration-configurations/source/status";
//
//        Map<String,String> headers = new HashMap<>();
//        headers.put("Content-Type","application/json");
//
//        try{
//
//            APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();
//            int responseCode = response.getResponseCode();
//
//            if( responseCode != 200){
//                logger.error("API Response Code is not equal to 200");
//                customAssert.assertTrue(false,"Expected Response Code : 200 Actual Response Code : " + responseCode);
//            }
//
//            String responseBody = response.getResponseBody();
//
//            if(!APIUtils.validJsonResponse(responseBody)){
//                customAssert.assertTrue(false,"API Response is not a valid Json");
//            }else {
//                int totalNumberOfStatusToBeChecked = 4;
//                JSONArray responseArray = new JSONArray(responseBody);
//
//                if(responseArray.length() != totalNumberOfStatusToBeChecked){
//                    logger.error("Number of status to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of status present " + responseArray.length());
//                    customAssert.assertTrue(false,"Number of status to be checked are " + totalNumberOfStatusToBeChecked + " Actual number Of status present " + responseArray.length());
//                }
//
//                int statusChecksPassed = 0;
//                String statusName;
//                for(int i = 0;i<responseArray.length();i++){
//                    statusName = responseArray.getJSONObject(i).get("name").toString();
//                    if(statusName.equalsIgnoreCase("Draft") ||
//                            statusName.equalsIgnoreCase("In Review") ||
//                            statusName.equalsIgnoreCase("Active") ||
//                            statusName.equalsIgnoreCase("Inactive")){
//
//                        statusChecksPassed =statusChecksPassed + 1;
//                    }
//                }
//                if(statusChecksPassed != totalNumberOfStatusToBeChecked){
//                    logger.error("Number of status to checked are " + statusChecksPassed + " Actual number Of status present " + totalNumberOfStatusToBeChecked);
//                    customAssert.assertTrue(false,"Expected Checks " + totalNumberOfStatusToBeChecked + " Actual Checks " + statusChecksPassed);
//                }
//            }
//
//        }catch (Exception e){
//            logger.error("Exception while validating API " + e.getMessage());
//            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
//        }
//
//        customAssert.assertAll();
//    }

    private Boolean validateDeleteAPI(String newApiUrl,CustomAssert customAssert ){

        Boolean validationStatus = true;

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");

        APIResponse response = executor.delete(hostURL,newApiUrl,headers).getResponse();

        if(response.getResponseCode() != 204){
            logger.error("Expected and Actual Response Code Didn't match for Delete API");
            customAssert.assertTrue(false,"Expected and Actual Response Code Didn't match for Delete API");
            validationStatus = false;
        }

        response = executor.get(hostURL,newApiUrl,headers).getResponse();

        if(response.getResponseCode() != 404){
            logger.error("Expected and Actual Response Code Didn't match for Get API for No source Found");
            customAssert.assertTrue(false,"Expected and Actual Response Code Didn't match for Get API for No source Found");
            validationStatus = false;
        }

        if(!response.getResponseBody().contains("Source Integration configuration not found")){
            logger.error("Response Body doesn't contain Source Integration configuration not found");
            customAssert.assertTrue(false,"Response Body doesn't contain Source Integration configuration not found");
            validationStatus = false;
        }

        logger.info("Completed Testing the API Url for Delete Method For Specific Source Id for particular Client ID");

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

    private Boolean validateGetAPIForSpecificSourceId(String idExpected,String newApiUrl,CustomAssert customAssert){

        logger.info("Testing the API for Specific Source Id");

        Boolean validationStatus = true;

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            APIResponse response = executor.get(hostURL, newApiUrl, headers).getResponse();

            if (!APIUtils.validJsonResponse(response.getResponseBody())) {
                logger.error("API Response is not a valid Json for Specific Source Id");
                customAssert.assertTrue(false, "API Response is not a valid Json for Specific Source Id");
                validationStatus = false;
            }

            JSONObject responseJson = new JSONObject(response);

            String idActual = responseJson.get("id").toString();

            if(idActual.equalsIgnoreCase(idExpected)){

                customAssert.assertTrue(false,"Id value didn't match Specific Source Id");
            }

            logger.info("Completed Testing the API Url for Get Method For Specific Source Id for particular Client ID");

        }catch (Exception e){
            validationStatus = false;
        }
        return validationStatus;

    }

    private Boolean validateGetAPIForAllSourceId(String apiUrl, String configIdCreated, CustomAssert customAssert){

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
        String actualId;
        Boolean fetchAllSourceIdContainsNewlyCreatedId = false;

        for(int i=0;i<responseBodyJsonArray.length();i++){

            actualId = responseBodyJsonArray.getJSONObject(i).get("id").toString();

            if(actualId.equalsIgnoreCase(configIdCreated)){
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

    private Boolean validatePatchAPI(String apiUrl,Boolean falseToBeChecked,CustomAssert customAssert){

        Boolean validationStatus = true;

        Map<String,String> headers = new HashMap<>();
        headers.put("Content-Type","application/json");
        String newAPIUrl =  apiUrl + "?active=true";

        APIResponse response = executor.patch(hostURL,newAPIUrl,headers).getResponse();

        if(response.getResponseCode() != 200){
            logger.error("Expected Response code : 200 Actual Response Code : " + response.getResponseCode());
            customAssert.assertTrue(false,"Expected Response code : 200 Actual Response Code : " + response.getResponseCode());
            validationStatus = false;
        }

        if(falseToBeChecked) {
            newAPIUrl = apiUrl + "?active=false";

            response = executor.patch(hostURL, newAPIUrl, headers).getResponse();

            if (response.getResponseCode() != 200) {
                logger.error("Expected Response code : 200 Actual Response Code : " + response.getResponseCode());
                customAssert.assertTrue(false, "Expected Response code : 200 Actual Response Code : " + response.getResponseCode());
                validationStatus = false;
            }
        }
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

//    private Boolean validateSourceSync(int clientId,int sourceId,CustomAssert customAssert){
//
//        logger.info("Validating the source Sync API");
//
//        Boolean validationStatus = true;
//
//        String apiUrl = "/integration-configurations/source/sync/" + clientId + "/" + sourceId;
//
//        logger.info("Validating the validate API");
//
//        Map<String,String> headers = new HashMap<>();
//        headers.put("Content-Type","application/json");
//
//        APIResponse response = executor.get(hostURL,apiUrl,headers).getResponse();
//
//        if(response.getResponseCode()!=202){
//            logger.error("SourceSync API validation unsuccessful ");
//            customAssert.assertTrue(false,"SourceSync API validation unsuccessful");
//            validationStatus = false;
//
//        }
//        return validationStatus;
//    }

}
