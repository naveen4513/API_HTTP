package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.api.ServiceLevel.SLIF_Schemas;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class test_Message_Scenarios extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_Message_Scenarios.class);


    private String slConfigFilePath;
    private String slConfigFileName;

    String adminUserName;
    String adminPassword;

    String configFilePath;
    String configFileName;

    String UDC;
    String userName;
    String password;
    String columns;
    String url;
    String expectedMsg;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIFBPConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIFBPConfigFileName");

        Check check = new Check();

        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        check.hitCheck(adminUserName,adminPassword);
    }

    @DataProvider(name = "flowstotestinvalid", parallel = false)
    public Object[][] flowsToTest() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotestinvalid").split(",");

        for(String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "flowstotestinvalidBP", parallel = false)
    public Object[][] flowsToTestBP() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"flowstotestinvalid").split(",");

        for(String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "flowsToTest",enabled = true,priority = 0)
    public void TestSourceNegativeScenario(String flowToTest){

        CustomAssert customAssert = new CustomAssert();
        url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"url");
        columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"columns");
        UDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"udc");
        userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"username");
        password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"password");
        expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expectedmsg");

        try{
            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)) {
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false, "Create Source API Response is not a valid Json");
            }

            String payload = createPayloadForSource(url,columns,UDC,userName,password);

            response = SLIF_Schemas.hitPostCreateSource(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create Source API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create Source API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();
            JSONObject jsonObject = new JSONObject(responseBody);

            String message = jsonObject.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            Assert.assertTrue(message.equalsIgnoreCase(expectedMsg),"Error in validating negative scenarios");

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }
    }

    @Test(dataProvider = "flowsToTestBP",enabled = true,priority = 0)
    public void TestBPSourceNegativeScenario(String flowToTestBP){

        CustomAssert customAssert = new CustomAssert();
        url = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"url");
        columns = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"columns");
        UDC = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"udc");
        userName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"username");
        password = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"password");
        expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,flowToTestBP,"expectedmsg");

        try{
            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)) {
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false, "Create Source API Response is not a valid Json");
            }

            String payload = createPayloadForBPSource(url,columns,UDC,userName,password);

            response = SLIF_Schemas.hitPostCreateSource(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create Source API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create Source API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();
            JSONObject jsonObject = new JSONObject(responseBody);

            String message = jsonObject.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            Assert.assertTrue(message.equalsIgnoreCase(expectedMsg),"Error in validating negative scenarios");

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }
    }

    private String createPayloadForSource(String url, String columns, String UDC, String userName, String password){

        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email").split(",");
        String emailId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","emailid");

        String payload;

        payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\":false,\"values\":" +
                "{\"url\":\""+url+"\",\"columns\":\""+columns+"\"," +
                "\"username\":\""+userName+"\",\"password\":\""+password+"\"}},\"authMetadata\":{\"name\":\"authMetadata\",\"id\":12635," +
                "\"multiEntitySupport\":false,\"values\":{\"id\":1,\"name\":\"Basic Authentication\"}},\"canSupplierBeParent\":true," +
                "\"requestMetadata\":{\"name\":\"requestMetadata\",\"id\":12629,\"multiEntitySupport\":false,\"values\":{\"id\":2}}," +
                "\"active\":{\"name\":\"active\",\"id\":12625,\"values\":true,\"multiEntitySupport\":false},\"name\":" +
                "{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\"Error Message Scenarios\"}," +
                "\"uniqueDataCriteria\":{\"name\":\"uniqueDataCriteria\",\"id\":12665,\"multiEntitySupport\":false,\"values\":\""+UDC+"\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false,\"values\":\"Error Scenarios\"}," +
                "\"email\":{\"name\":\"email\",\"id\":"+emailId+",\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(email)+"]}}}}";

        return payload;
    }

    private String createPayloadForBPSource(String url, String columns, String UDC, String userName, String password){

        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email").split(",");
        String emailId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","emailid");

        String payload;

        payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\":false,\"values\":" +
                "{\"url\":\""+url+"\",\"columns\":\""+columns+"\"," +
                "\"username\":\""+userName+"\",\"password\":\""+password+"\"}},\"authMetadata\":{\"name\":\"authMetadata\",\"id\":12635," +
                "\"multiEntitySupport\":false,\"values\":{\"id\":1,\"name\":\"Basic Authentication\"}},\"canSupplierBeParent\":true," +
                "\"requestMetadata\":{\"name\":\"requestMetadata\",\"id\":12629,\"multiEntitySupport\":false,\"values\":{\"id\":7}}," +
                "\"active\":{\"name\":\"active\",\"id\":12625,\"values\":true,\"multiEntitySupport\":false},\"name\":" +
                "{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\"Error Message Scenarios\"}," +
                "\"uniqueDataCriteria\":{\"name\":\"uniqueDataCriteria\",\"id\":12665,\"multiEntitySupport\":false,\"values\":\""+UDC+"\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false,\"values\":\"Error Scenarios\"}," +
                "\"email\":{\"name\":\"email\",\"id\":"+emailId+",\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(email)+"]}}}}";

        return payload;
    }

    private String createPayloadStringFromArray(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"id\": \"" + list[i] + "\"},";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

}

