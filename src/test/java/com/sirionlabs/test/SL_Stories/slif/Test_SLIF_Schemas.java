package com.sirionlabs.test.SL_Stories.slif;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.ServiceLevel.SLIF_Schemas;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.velocity.runtime.directive.Parse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

public class Test_SLIF_Schemas extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(Test_SLIF_Schemas.class);

    String configFilePath;
    String configFileName;
    String slConfigFilePath;
    String slConfigFileName;

    String adminUserName;
    String adminPassword;

    private String slifDictionaryConfigFilePath;
    private String slifDictionaryConfigFileName;

    String name;
    String description;
    String active;
    String[] email;
    String toolId;
    String authId;
    String url;
    String columns;
    String UDC;
    String NDC;
    String userName;
    String password;
    String epochTime;
    int destinationId;
    String dbHostAddress;
    String dbName;
    String dbPortName;
    String dbUserName;
    String dbPassword;
    PostgreSQLJDBC postgreSQLJDBC;

    @BeforeClass
    public void BeforeClass() {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");
        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        Check check = new Check();
        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        check.hitCheck(adminUserName,adminPassword);
        slifDictionaryConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLIFDictionaryConfigFilePath");
        slifDictionaryConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLIFDictionaryConfigFileName");

        description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","description");
        active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","active");
        email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email").split(",");

        dbHostAddress = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress,dbPortName,dbName,dbUserName,dbPassword);

    }

    @DataProvider(name = "flowsToTest", parallel = false)
    public Object[][] flowsToTest() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotest").split(",");

        for(String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "flowsToTestDaily", parallel = false)
    public Object[][] flowsToTestDaily() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowToTestDaily = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotestdaily").split(",");

        for(String flowToTest : flowToTestDaily) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "flowsToSource", parallel = false)
    public Object[][] flowsToSource() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowToTestDaily = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstosource").split(",");

        for(String flowToTest : flowToTestDaily) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    //C153499,C153500,C153501,C153590
    @Test(dataProvider = "flowsToSource" ,enabled = true, priority = 0)  // working now
    public void TestSourceCreationServiceNow(String flowToSource){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = -1;

        try{

            UDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
            name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"name")+" "+DateUtils.getCurrentDateInMM_DD_YYYY();
            toolId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"tool");
            columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"columns");
            url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"url");
            NDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"ndc");
            authId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"auth");
            userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"username");
            password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToSource,"password");

            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)) {
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false, "Create Source API Response is not a valid Json");
            }

            APIResponse schemaResponse = SLIF_Schemas.hitGetSchemaDetails(executor, toolId);
            String schemaBody  = schemaResponse.getResponseBody();
            if(!APIUtils.validJsonResponse(schemaBody)){
                logger.error("Request Schema API Response is not a valid Json");
                customAssert.assertTrue(false,"Request Schema API Response is not a valid Json");
            }

            APIResponse authResponse = SLIF_Schemas.hitGetAuthDetails(executor, authId);
            String authBody = authResponse.getResponseBody();
            if(!APIUtils.validJsonResponse(authBody)){
                logger.error("Auth Schema API Response is not a valid Json");
                customAssert.assertTrue(false,"Auth Schema API Response is not a valid Json");
            }

            JSONObject getResponse = new JSONObject(schemaBody);
            JSONObject getAuthResponse = new JSONObject(authBody);

            JSONObject sourceSchemas = getResponse.getJSONObject("sourceUiSchema");
            JSONObject authSchemas = getAuthResponse.getJSONObject("authUiSchema");

            sourceSchemas.put("url",url);
            sourceSchemas.put("newDataCriteria",NDC);
            sourceSchemas.put("columns",columns);
            authSchemas.put("username",userName);
            authSchemas.put("password",password);

            String[] auth = authSchemas.toString().split("\\{");
            String[] source  = sourceSchemas.toString().split("\\}");

            String newSchema = source[0]+","+auth[1];

            String payload = createPayloadForSource(name,toolId,newSchema, authId);

            response = SLIF_Schemas.hitPostCreateSource(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create Source API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create Source API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }
            else {
                customAssert.assertTrue(true, "Source ID created successfully");
                logger.info("Source has been created successfully "+sourceId+"");
            }

        }catch (Exception e){
            customAssert.assertFalse(false,"Exception while validating Create Source  API " + e.getMessage());
        }finally {
            deleteSource(sourceId);
        }
        customAssert.assertAll();
    }

    //C152071,C141320,C141321,C141226,C141280
    @Test(dataProvider = "flowsToTest", enabled = true, priority = 1)
    public void TestDestinationCreationServiceNow(String flowToTest){
        CustomAssert customAssert = new CustomAssert();
        destinationId = -1;

        try{
            String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"name")+getDate();
            String[] slaId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"sla").split(",");
            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"sourceid"));
            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));
            String query = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"query");
            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest, "frequency"));
            String[] slaSubCategory = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"slasubcategory").split(",");
            String[] contract = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract").split(",");
            int reportId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","report id"));
            String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance status").split(",");
            String[] computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"computation frequency").split(",");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","entity type id"));

            destinationId = createDestination(name,sourceId, frequencyId, slaId, slaSubCategory,computationFrequency,entityTypeId,query,customAssert);

            if(destinationId == -1){
                customAssert.assertFalse(true,"Destination ID not created successfully Service Now");
            }
            List<String> listingData = new ArrayList<>();

            //Frequency Id 2 means monthly
            if(frequencyId == 2) {
                String startDate = getLastMonthDueDate().get(0);
                String endDate = getLastMonthDueDate().get(1);
                listingData = getCSLDataFromListingPage(customAssert,contract,performanceStatus,slaId,computationFrequency,startDate,endDate);

            }//Frequency Id 3 means quarterly
            else if(frequencyId == 3) {
                String startDate = getQuarterlyMonthDueDate().get(0);
                String endDate = getQuarterlyMonthDueDate().get(1);
                listingData = getCSLDataFromListingPage(customAssert,contract,performanceStatus,slaId,computationFrequency,startDate,endDate);

            }

            //Validating Test CSL
            String filePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";

            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filename");

            Boolean downLoadCSLDataStatus= downloadCSLDataFromDestination(computationFrequency,frequencyId,supplierId,slaId,contract,performanceStatus,filePath,fileName,reportId,customAssert);

            if(downLoadCSLDataStatus) {
                List<String> excelData = getCSLDataFromExcelSheet(customAssert,filePath, fileName);

                if(excelData.isEmpty()){
                    customAssert.assertTrue(false, "There is NO CSL corresponding to this destination");
                    customAssert.assertAll();
                }

                Boolean result = compareExcelAndListingData(customAssert,listingData, excelData);
                if (result)
                    customAssert.assertTrue(true, "Eligible CSL has validated successfully");
            }

//          Validate ServiceNowIds
            String serviceNowUrl = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","serviceNowUrl");
            String tableName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","tableName");
            String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","newDataCriteria");
            String startTime = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","starttime");
            String endTime = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","endtime");
            String systemParamFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","systemparamfields");

            List<Map<String,String>> serviceNowData = getServiceNowData(frequencyId, serviceNowUrl, tableName, newDataCriteria, startTime, endTime, systemParamFields,customAssert);

            if(serviceNowData.size()<2000) {

                String excelFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String excelFileName = "Raw Data From Destination.xlsx";

                Boolean downloadTestDataStatus = destinationDownload(destinationId, excelFilePath, excelFileName, customAssert);

                if (downloadTestDataStatus) {
                    compareTestDataWithIncFetchedFromSnow(serviceNowData, excelFilePath, excelFileName, customAssert);
                }
            }else{
                customAssert.assertTrue(true, "Can't Compare Test Data with Service Now Data as data count is greater that 2000");
            }

            Boolean templateOnSL = uploadTemplateOnSL(supplierId,contract,slaId,customAssert);
            if(templateOnSL.equals(true)){
                logger.debug("Template upload successfully on SL");
            }
            else{
                logger.debug("Getting error while uploading template on SL");
            }

        }catch (Exception e){
            logger.error("Exception while validating TestSLIFPositiveFlow " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating TestSLIFPositiveFlow in the main Test Method " + e.getMessage());
        }
        customAssert.assertAll();
    }

    //C152071,C141320,C141321,C141287
    @Test(dataProvider = "flowsToTestDaily", enabled = false)
    public void TestDestinationCreationDaily(String flowToTest){
        CustomAssert customAssert = new CustomAssert();
        try {
            String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"name") + DateUtils.getCurrentDateInMM_DD_YYYY();
            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "source ids", "positive flow"));;
            int reportId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","report id"));

            String[] contract = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "contract").split(",");
            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "supplier"));
            String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "performance status").split(",");
            String query = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"query");
            String[] slaId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sla").split(",");
            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "frequency"));
            String[] slaSubCategoryId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "slasubcategory").split(",");
            String[] computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "computation frequency").split(",");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","entity type id"));

            String payload = createPayloadForDestinationDaily(name,sourceId, frequencyId, slaId, slaSubCategoryId, computationFrequency,entityTypeId,query);

            destinationId = createDestinationDaily(payload, customAssert);

            List<String> listingData = new ArrayList<>();
            // 1006 means computation frequency is monthly

            if (Arrays.asList(computationFrequency).contains("1003")) {
                String startDate = getDailyMonthDueDate().get(0);
                String endDate = getDailyMonthDueDate().get(1);

                listingData = getCSLDataFromListingPage(customAssert,contract, performanceStatus, slaId, computationFrequency, startDate, endDate);
            }
            // 1006 means computation frequency is quarterly
            else if (Arrays.asList(computationFrequency).contains("1006")) {
                String startDate = getDailyMonthDueDateQuarterly().get(0);
                String endDate = getDailyMonthDueDateQuarterly().get(1);

                listingData = getCSLDataFromListingPage(customAssert,contract, performanceStatus, slaId, computationFrequency, startDate, endDate);
            }

            String filePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filename");

            Boolean downloadStatus = downloadCSLDataFromDestination(computationFrequency, frequencyId, supplierId, slaId, contract, performanceStatus, filePath, fileName, reportId,customAssert);

            if (downloadStatus) {
                List<String> excelData = getCSLDataFromExcelSheet(customAssert,filePath, fileName);

                if(excelData.isEmpty()){
                    customAssert.assertTrue(false, "There is NO CSL corresponding to this destination");
                    customAssert.assertAll();
                }

                Boolean result = compareExcelAndListingData(customAssert,listingData, excelData);
                if (result)
                    customAssert.assertTrue(true, "Eligible CSL has validated successfully");
            }

            //Validate ServiceNowIds
            String serviceNowUrl = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","serviceNowUrl");
            String tableName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","tableName");
            String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","newDataCriteria");

            String startTime = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","starttime");
            String endTime = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","endtime");
            String systemParamFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","systemparamfields");

            List<Map<String,String>> serviceNowData = getServiceNowDataDaily(serviceNowUrl, tableName, newDataCriteria, startTime, endTime, systemParamFields,customAssert);

            if(serviceNowData.size()>2000) {
                String excelFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
                String excelFileName = "Raw Data From Destination.xlsx";

                Boolean downloadTestDataStatus = destinationDownload(destinationId, excelFilePath, excelFileName, customAssert);

                if (downloadTestDataStatus) {
                    compareTestDataWithIncFetchedFromSnow(serviceNowData, excelFilePath, excelFileName, customAssert);
                }
            }else {
                customAssert.assertTrue(true, "Can't Compare Test Data with Service Now Data as data count is greater that 2000");
            }

            Boolean templateOnSL = uploadTemplateOnSL(supplierId,contract,slaId,customAssert);
            if(templateOnSL.equals(true)){
                logger.debug("Template upload successfully on SL");
            }
            else{
                logger.debug("Getting error while uploading template on SL");
            }

        }catch (Exception e){
            logger.error("Exception while Testing Destination Daily Flow in the main Main Method" + e.getMessage());
            customAssert.assertTrue(false,"Exception while Testing Destination Daily Flow in the main Main Method" + e.getMessage());
        }
    }

    private String createPayloadForSource(String name,String toolId,String newSchema, String authId){

        String payload;

        payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\":false," +
                  "\"values\":"+newSchema+"},\"authMetadata\":{\"name\":\"authMetadata\",\"id\":12635," +
                  "\"multiEntitySupport\":false,\"values\":{\"id\":"+authId+"}},\"canSupplierBeParent\":true," +
                  "\"requestMetadata\":{\"name\":\"requestMetadata\",\"id\":12629,\"multiEntitySupport\":false,\"values\":" +
                  "{\"id\":"+toolId+"}},\"active\":{\"name\":\"active\",\"id\":12625,\"values\":"+active+",\"multiEntitySupport" +
                  "\":false},\"name\":{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\""+name+"\"}," +
                  "\"uniqueDataCriteria\":{\"name\":\"uniqueDataCriteria\",\"id\":12665,\"multiEntitySupport\":false," +
                  "\"values\":\""+UDC+"\"},\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false," +
                  "\"values\":\""+description+"\"},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false,\"values\"" +
                  ":["+createPayloadStringFromArray(email)+"]}}}}";

        return payload;

    }

    public static int getSourceId(String createJsonStr, String entityName,String notification) {
        int newEntityId = -1;
        try {
            if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
                JSONObject jsonObj = new JSONObject(createJsonStr);
                if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
                    if (jsonObj.getJSONObject("header").getJSONObject("response").has("entityId")) {
                        return jsonObj.getJSONObject("header").getJSONObject("response").getInt("entityId");
                    }

                    String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");

                    String temp[] = notificationStr.trim().split(Pattern.quote(notification + "/"));
                    if (temp.length > 1) {
                        String temp2 = temp[1];
                        String temp3[] = temp2.trim().split(Pattern.quote("\""));
                        if (temp3.length > 1) {
                            String temp4 = temp3[0];
                            String temp5[] = temp4.trim().split(Pattern.quote("/"));
                            if (temp5.length > 1)
                                newEntityId = Integer.parseInt(temp5[1]);
                        }
                    }
                } else {
                    logger.error("New Entity {} not created. ", entityName);
                }
            } else {
                logger.error("Create Response for Entity {} is not valid JSON.", entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getStackTrace());
        }
        return newEntityId;
    }

    public int createDestination(String name,int sourceId,int frequencyId,String[] slaId, String[] slaSubCategory,String[] computationFrequency ,int entityTypeId,String query,CustomAssert customAssert){


        int destinationId = -1;
        try{
            String payload = createPayloadForDestination(name,sourceId,frequencyId,slaId,slaSubCategory,computationFrequency,entityTypeId,query);

            APIResponse response = SLIF_Schemas.hitPostCreateDestination(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create destination API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create destination API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Destination API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Destination API Response is not a valid Json");

            }else {
                destinationId = getSourceId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully.");
                    customAssert.assertAll();
                }
                else{
                    logger.info("Destination ID created successfully.");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating Destination Create API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Destination Create API " + e.getMessage());
        }

        return destinationId;

    }

    private String createPayloadForDestination(String name,int sourceId, int frequencyId,String[] slaId,String[] subCategory,String[] computationFrequency,int entityTypeId,String query){

        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","active");
        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","email").split(",");
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

        String timeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","time zone");
        String datePattern = getDate();
        String[] contractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract").split(",");
        String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance status").split(",");
        String[] performanceParentId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance parent id").split(",");
        int emailID = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","emailid"));

        String payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\"" +
                ":false,\"values\":{\"snowQuery\":\""+query+"\"}},\"frequency\":{\"name\":\"frequency\",\"id\":12649," +
                "\"multiEntitySupport\":false,\"values\":{\"id\":"+frequencyId+"}},\"canSupplierBeParent\":true," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\""+datePattern+"\"," +
                "\"displayValues\":\"11-19-2020\"},\"sourceIntegrationConfig\":{\"name\":\"sourceIntegrationConfig\"," +
                "\"id\":12641,\"multiEntitySupport\":false,\"values\":{\"id\":"+sourceId+"}},\"contractIds\":" +
                "{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(contractId)+"]}," +
                "\"active\":{\"name\":\"active\",\"id\":12640,\"values\":"+active+",\"multiEntitySupport\":false}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":12648,\"multiEntitySupport\":false,\"values\":" +
                "["+createPayloadSLAStringFromArray(slaId,subCategory)+"]},\"name\":{\"name\":\"name\",\"id\":" +
                "12638,\"multiEntitySupport\":false,\"values\":\""+name+"\"},\"supplierId\":{\"name\":\"supplierId\"," +
                "\"id\":12646,\"multiEntitySupport\":false,\"values\":{\"id\":"+supplierId+"}},\"configTimeZone\":" +
                "{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{\"id\":"+timeZone+"}}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false,\"values\":\""+description+"\"}," +
                "\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Destination Integration Config\",\"id\":346}," +
                "\"multiEntitySupport\":false},\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":["+createPayloadSLAStringFromArray(performanceStatus,performanceParentId)+"]}," +
                "\"email\":{\"name\":\"email\",\"id\":"+emailID+",\"multiEntitySupport\":false,\"values\":["+createPayloadStringFromArray(email)+"]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":["+createPayloadStringFromArray(computationFrequency)+"]},\"entityTypeId\":{\"name\":" +
                "\"entityTypeId\",\"values\":"+entityTypeId+",\"multiEntitySupport\":false}}}}";

        return payload;
    }

    private List<String> getLastMonthDueDate() {

        List<String> dates = new ArrayList<>();
        String endDate=null;
        int end=0;
        String startDate = null;
        String currentDateDb;
        try {

            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select now();");

            currentDateDb = dbCurrentDate.toString();
            String currentMonthString="";
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            if(currentMonth == 1){
                currentMonth = 12;
                currentYear = currentYear - 1;
            }else {
                currentMonth = currentMonth -1;
            }

            if(currentMonth <=9) {
                currentMonthString = 0 + String.valueOf(currentMonth);
            }else {
                currentMonthString = String.valueOf(currentMonth);
            }

            startDate =currentMonthString+"-"+"01"+"-"+currentYear;
            end = getEndDate(currentMonth,currentYear);
            endDate =currentMonthString+"-"+end+"-"+currentYear;
            dates.add(startDate);
            dates.add(endDate);
        } catch (Exception e) {
            logger.error("Dates are not fetching successfully");
        }
        return dates;
    }

    private List<String> getCSLDataFromListingPage(CustomAssert customAssert,String[] contract, String[] performanceStatus, String[] slaItem, String[] computationFrequency, String startDate, String endDate){

        List<String> cslList=new ArrayList<>();

        Check check = new Check();
        check.hitCheck();

        int listId = 265;

        String response;

        String payload = createPayloadForCSLListingPage(contract,performanceStatus,slaItem,computationFrequency,startDate,endDate);
        try {
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId, payload);
            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);
            JSONArray dataArray = listDataResponseJSON.getJSONArray("data");
            JSONObject indRowDataJson;
            for(int i=0;i < dataArray.length();i++) {
                indRowDataJson = dataArray.getJSONObject(i);
                Iterator<String> keys = indRowDataJson.keys();
                String columnName;
                String columnValue;
                while(keys.hasNext()) {
                    String key = keys.next();
                    columnName = indRowDataJson.getJSONObject(key).get("columnName").toString();
                    if(columnName.equals("id")){
                        columnValue = indRowDataJson.getJSONObject(key).get("value").toString().split(":;")[0];
                        columnValue = "CSL"+columnValue;
                        cslList.add(columnValue);
                    }
                }
            }
        }catch(Exception e){
            customAssert.assertFalse(false,"Error in fetching CSL data from listing Page");
        }
        finally {
            check.hitCheck(adminUserName,adminPassword);
        }
        return cslList;
    }

    private List<String> getQuarterlyMonthDueDate() {
        List<String> dates = new ArrayList<>();
        String endDate=null;
        String startDate = null;
        int end = 0;
        String currentDateDb;
        try {

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("Select now();");
            postgreSQLJDBC.closeConnection();
            currentDateDb = dbCurrentDate.toString();
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            List<String> quarterMonth = expectedPreviousQuarterMonths(currentMonth);
            String quarterFirstMonth = quarterMonth.get(0);
            String quarterLastMonth =  quarterMonth.get(2);

            if(currentMonth == 1 || currentMonth == 2 || currentMonth ==3){
                currentYear = currentYear-1;
            }

            if(Integer.parseInt(quarterFirstMonth) <= 9)
                quarterFirstMonth = 0+quarterFirstMonth;
            if(Integer.parseInt(quarterLastMonth) <= 9)
                quarterLastMonth = 0+quarterLastMonth;

//            startDate = currentYear+ "-" +quarterFirstMonthString+ "-" +"01";
            startDate = quarterFirstMonth+"-"+"01"+"-"+currentYear;
            end = getEndDate(Integer.parseInt(quarterLastMonth),currentYear);
//            endDate = currentYear+"-"+quarterLastMonthString+"-"+end;
            endDate = quarterLastMonth+"-"+end+"-"+currentYear;

            dates.add(startDate);
            dates.add(endDate);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return dates;
    }

    Boolean downloadCSLDataFromDestination(String[] computationFrequency,int frequency,int supplierId,String[] slaId,String[] contractId,String[] performanceStatus, String filePath, String fileName,int reportId,CustomAssert customAssert){

        try{
            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            Map<String, String> formParam = getFormParamDownloadList(computationFrequency,frequency,supplierId,slaId,contractId,performanceStatus);

            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam,reportId);
            if (response.getStatusLine().toString().contains("200")) {

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,filePath,fileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Report for frequency " + frequency);
                }

            } else {
                logger.error("Error while downloading Report ");
                customAssert.assertTrue(false,"Error while downloading Report ");
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        return true;
    }

    private List<String> getCSLDataFromExcelSheet(CustomAssert customAssert,String filePath, String fileName){
        List<String> cSLList = new ArrayList<>();
        try{
            String sheetName = "Data QA";
            int columnNo = 0;
            int startingRowNo = 4;
            int getRows = XLSUtils.getNoOfRows(filePath,fileName,sheetName).intValue();
            int noOfRows = getRows-6;

            cSLList = XLSUtils.getOneColumnDataFromMultipleRows(filePath,fileName,sheetName,columnNo,startingRowNo,noOfRows);

        }catch(Exception e){
            customAssert.assertFalse(false,"Error in fetching CSL list from Excel");
        }
        return cSLList;
    }

    private Boolean compareExcelAndListingData(CustomAssert customAssert,List<String> listingData, List<String> excelData){
        Boolean comparisonStatus = true;
        try{
            if(excelData.size()==listingData.size())
            {
                for(int i=0;i<excelData.size();i++)
                {
                    if(!excelData.get(i).equals(listingData.get(i)))
                    {
                        comparisonStatus = false;
                        customAssert.assertTrue(false,"CSLId from Listing Page and Excel Sheet are not equal Excel Sheet List "+ excelData +"Listing Sheet List"+listingData);
                        customAssert.assertAll();
                        break;
                    }
                }
            }
            else
            {
                comparisonStatus = false;
                customAssert.assertTrue(false,"CSLId from Listing Page and Excel Sheet are not equal Excel Sheet List "+ excelData +"Listing Sheet List"+listingData);
                customAssert.assertAll();
            }
        }catch(Exception e){
            customAssert.assertFalse(false, "Error in comparing Excel and Listing CSL");
        }
        return comparisonStatus;
    }

    private List<Map<String,String>> getServiceNowData(int frequencyId,String serviceNowUrl,String tableName,String newDataCriteria,
                                                       String startTime,String endTime,String systemParamFields,CustomAssert customAssert) {

        List<Map<String,String>> serviceNowData = new ArrayList<>();

        int portNumber = 443;
        String protocolScheme = "https";
        String startDate = "";
        String endDate = "";
        String newStartDate = "";
        String newEndDate = "";

        if (frequencyId ==2){
            startDate = getLastMonthDueDateForServiceNow().get(0);
            endDate = getLastMonthDueDateForServiceNow().get(1);
        }

        if (frequencyId ==3){
            startDate = getQuarterlyMonthDueDateForServiceNow().get(0);
            endDate = getQuarterlyMonthDueDateForServiceNow().get(1);
        }

        newStartDate = startDate+""+startTime;
        newEndDate = endDate+""+endTime;

        String jS = "javascript:gs.dateGenerate(%27";
        String operator = "BETWEEN";
        String order_by = "%5EORDERBYopened_at";

        String apiUrl = "/api/now/table/" + tableName + "?sysparm_query=" + newDataCriteria +
                operator + jS + newStartDate + "%27)@" + jS + newEndDate + "%27)"+order_by+"&sysparm_fields=" + systemParamFields + "&sysparm_display_value=true&sysparm_limit=2000";

//      String apiUrl = "/api/now/table/incident?sysparm_query=opened_at>=javascript:gs.dateGenerate('2020-05-01','05:30:01')^opened_at<=javascript:gs.dateGenerate('2020-06-01','05:29:59')^ORDERBYopened_at&sysparm_fields=number,state,priority,opened_at";

        try {
            HttpGet httpGetRequest = new HttpGet(apiUrl);

            httpGetRequest.addHeader("Accept", "application/json");
            httpGetRequest.addHeader("Authorization", "Basic YWRtaW46aFhhaUE1U1ZCcmI3");
            httpGetRequest.addHeader("Cookie", "glide_user_route=glide.bdfedf64b9e9d707bc156f6d44dce6c5; BIGipServerpool_dev68841=2441107466.55102.0000; glide_user_activity=U0N2Mzp2N1gxVGtMSEJwRWw5WXVQNWxSeXNqZTIyNHhsZHlVMTpaNG0xei9YbHV0MjNYS3J5RXE5dE5HMlJkYTRBMGluS2R4ME04TzJPaVJrPQ==; JSESSIONID=B5CDDA60333B04BFDEA9433AAB26B7E9; glide_session_store=72ED703ADB8150108BB3A455CA961921");
            HttpClient httpClient;
            APIUtils oAPIUtils = new APIUtils();

            httpClient = oAPIUtils.getHttpsClient();

            HttpHost target = new HttpHost(serviceNowUrl, portNumber, protocolScheme);

            HttpResponse response = httpClient.execute(target, httpGetRequest);

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONObject responseBodyJson = new JSONObject(responseBody);

            JSONArray resultArray = responseBodyJson.getJSONArray("result");
            Map<String,String> excelMap;
            for(int i=0;i<resultArray.length();i++){

                Iterator<String> keys = resultArray.getJSONObject(i).keys();
                excelMap = new HashMap<>();

                while(keys.hasNext()) {
                    String key = keys.next();
                    String columnValue = "null";
                    try {
                        columnValue = resultArray.getJSONObject(i).get(key).toString();
                    }catch (Exception e){
                        customAssert.assertFalse(false,"Exception while fetching service now data");
                    }
                    excelMap.put(key,columnValue);
                }
                serviceNowData.add(excelMap);
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creating service now data ");
        }

        return serviceNowData;
    }

    public Boolean destinationDownload(int destinationId,String outputFilePath,String outputFileName,CustomAssert customAssert){

        Boolean destinationDownloadStatus = true;
        try{

            String apiUrl = "/slintegration/destination/download/" + destinationId;

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            HttpResponse response = executor.GetHttpResponseForGetAPI(apiUrl, headers);

            if (response.getStatusLine().toString().contains("200")) {

                Boolean downLoadStatus = dumpDownloadListWithDataResponseIntoFile(response,outputFilePath,outputFileName );

                if(!downLoadStatus){
                    customAssert.assertTrue(false,"Error while downloading Raw Data From Destination");
                    destinationDownloadStatus = false;
                }

            } else {
                logger.error("Error while downloading Raw ");
                customAssert.assertTrue(false,"Error while downloading Raw Data From Destination");
                destinationDownloadStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
            destinationDownloadStatus = false;
        }

        return destinationDownloadStatus;
    }

    private Boolean compareTestDataWithIncFetchedFromSnow(List<Map<String,String>> serviceNowData,
                                                          String excelFilePath,String excelFileName,CustomAssert customAssert){

        Boolean compareStatus = true;
        try{

            String sheetName = "Data QA";
            int rawDataHeaderRowNum = 4;

            int startingRow = 4;
            int noOfRows = XLSUtils.getNoOfRows(excelFilePath,excelFileName,sheetName).intValue() -6;

            List<String> headerRawData = XLSUtils.getExcelDataOfOneRow(excelFilePath,excelFileName,sheetName,rawDataHeaderRowNum);
            Map<String,Integer> headerDataColumnNum = new HashMap<>();

            for(int i =0;i<headerRawData.size();i++){
                headerDataColumnNum.put(headerRawData.get(i),i);
            }

            List<List<String>> excelData = XLSUtils.getExcelDataOfMultipleRows(excelFilePath,excelFileName,sheetName,startingRow,noOfRows);
            Map<String,String> serviceNowDataValues;
            String serviceNowColumnName;
            String serviceNowColumnValue;

            for(int i=0;i<serviceNowData.size();i++){

                serviceNowDataValues = serviceNowData.get(i);

                if(serviceNowDataValues.size()== headerDataColumnNum.size()) {

                    for (Map.Entry<String, String> entry : serviceNowDataValues.entrySet()) {

                        serviceNowColumnName = entry.getKey();
                        serviceNowColumnValue = entry.getValue();
                        serviceNowColumnValue = getValueFromDictionary(serviceNowColumnName, serviceNowColumnValue);
                        int columnNum = headerDataColumnNum.get(serviceNowColumnName);

                        String excelColumnValue = excelData.get(i).get(columnNum);

                        if (!serviceNowColumnValue.equals(excelColumnValue)) {
                            compareStatus = false;
                            customAssert.assertTrue(false, "TestData Download comparison failed with the Data Fetched From Service Now");
                            customAssert.assertAll();
                            break;
                        }

                    }
                }else {
                    customAssert.assertTrue(false, "Mismatch in Test Data And Service Now Data");
                    customAssert.assertAll();
                }
            }

        }catch (Exception e){

            customAssert.assertTrue(false,"Exception while comparing TestData Downloaded With Inc Fetched From Service Now");
            compareStatus = false;
        }

        return compareStatus;
    }

    private Boolean uploadTemplateOnSL(int supplierId, String[] contract, String[] slaId, CustomAssert customAssert) {
        Boolean templateUploadOnSL = true;
        List<Integer> slIds;
        int uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));;
        int slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));;
        slIds = getSLIdFromListingPage(supplierId,contract,slaId);
        String[] columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl template upload","column name").split(",");
        String[] columnType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl template upload","column type").split(",");
        String[] format = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl template upload","format").split(",");
        String[] type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl template upload","type").split(",");
        String[] sirionFunction = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"sl template upload","sirion function").split(",");

        int startRowNum = 2;
        String sheetName = "Format Sheet";
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        String uploadFilePath = "src\\test\\resources\\TestConfig\\SLIF";
        String performanceDataFormatFileName = "SLTemplate.xlsm";
        String expectedMsg = "200";
        try{
            for(int i= startRowNum;i<columns.length+2;i++) {
                XLSUtils.updateColumnValue(uploadFilePath, performanceDataFormatFileName, sheetName, i, 0, columns[i-2].trim());
                XLSUtils.updateColumnValue(uploadFilePath, performanceDataFormatFileName, sheetName, i, 1, columnType[i-2].trim());
                XLSUtils.updateColumnValue(uploadFilePath, performanceDataFormatFileName, sheetName, i, 2, format[i-2].trim());
                XLSUtils.updateColumnValue(uploadFilePath, performanceDataFormatFileName, sheetName, i, 3, type[i-2].trim());
                XLSUtils.updateColumnValue(uploadFilePath, performanceDataFormatFileName, sheetName, i, 4, sirionFunction[i-2].trim());
            }}catch(Exception e){
            logger.error("Exception while updating Excel",e.getStackTrace());
        }
        for(int i=0;i<slIds.size();i++) {
            int serviceLevelId = slIds.get(i);
            Boolean uploadSLTemplate = serviceLevelHelper.uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

            if (!uploadSLTemplate) {
                templateUploadOnSL = false;
                customAssert.assertFalse(false, "Error while uploading SL Template");
            }

            Boolean fileUploadStatus = serviceLevelHelper.validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert);

            if (!fileUploadStatus) {
                templateUploadOnSL = false;
                customAssert.assertFalse(false, "SL Template not uploaded successfully");
            }
        }
        return templateUploadOnSL;
    }

    private int getEndDate(int month,int year){
        int endDate=0;
        if(month == 1 || month ==3 || month ==5 || month == 7 || month == 8|| month ==10 || month == 12)
            endDate =31;
        else if(month == 4 || month== 6 || month ==9 || month ==11)
            endDate = 30;
        else if(month == 2)
        {
            if((year % 4 == 0) && !(year % 100 == 0) || (year % 400 == 0))
                endDate = 29;
            else
                endDate = 28;
        }

        return endDate;
    }

    private String createPayloadForCSLListingPage(String[] contract, String[] performanceStatus, String[] slaItem, String[] computationFrequency, String startDate, String endDate){
        String payload=null;

        try {
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":2000,\"orderByColumnName\":" +
                    "\"id\",\"orderDirection\":\"asc nulls last\",\"filterJson\":" +
                    "{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":["+createPayloadStringFromArray(contract)+"]},\"filterId\":2," +
                    "\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"24\":{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null," +
                    "\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\""+startDate+"\",\"end\":\""+endDate+"\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"26\":{\"multiselectValues\":{\"SELECTEDDATA\":["+createPayloadStringFromArray(performanceStatus)+"]}," +
                    "\"filterId\":26,\"filterName\":\"performanceStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"29\":{\"multiselectValues\":{\"SELECTEDDATA\":["+createPayloadStringFromArray(computationFrequency)+"]}," +
                    "\"filterId\":29,\"filterName\":\"frequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":["+createPayloadStringFromArray(slaItem)+"]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null," +"\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }catch(Exception e){
            logger.debug("Exception while creating payload");
        }

        return payload;
    }

    private List<String> expectedPreviousQuarterMonths(int month){

        List<String> previousMonths = new ArrayList<>();

        if(month == 1 || month == 2 || month == 3){
            previousMonths = Arrays.asList("10","11","12");

        }
        else if(month == 4 || month == 5 || month == 6){
            previousMonths = Arrays.asList("1","2","3");
        }
        else if(month == 7 || month == 8 || month == 9){
            previousMonths = Arrays.asList("4","5","6");
        }
        else if(month == 10 || month == 11 || month == 12) {
            previousMonths = Arrays.asList("7", "8", "9");
        }
        return previousMonths;

    }

    private Map<String, String> getFormParamDownloadList(String[] computationFrequency,int frequency,int supplierId,String[] slaId,String[] contractId,String[] performanceStatus) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        jsonData = "{\"filterMap\":{\"filterJson\":{},\"supplierId\":"+supplierId+",\"contractIds\":["+createPayloadForReport(contractId)+"]," +
                "\"slaIds\":["+createPayloadForReport(slaId)+"],\"computationFrequency\":["+createPayloadForReport(computationFrequency)+"]," +
                "\"frequency\":"+frequency+",\"performanceStatus\":["+createPayloadForReport(performanceStatus)+"]}}\n";

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String outputFileName) {

        Boolean status;
        String outputFile;
        FileUtils fileUtil = new FileUtils();

        try {
            outputFile = outputFilePath + "/" + outputFileName;
            status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status) {
                logger.info("DownloadListWithData file generated at {}", outputFile);
                status = true;
            }
        }catch (Exception e){
            status = false;
        }
        return status;
    }

    private List<String> getLastMonthDueDateForServiceNow() {

        List<String> dates = new ArrayList<>();
        String endDate=null;
        int end=0;
        String startDate = null;
        String currentDateDb;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select now();");
            postgreSQLJDBC.closeConnection();

            currentDateDb = dbCurrentDate.toString();
            String currentMonthString="";
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));
            String month = String.valueOf(currentMonth);

            if(currentMonth == 1){
                currentMonth = 12;
                currentYear = currentYear - 1;
            }else {
                currentMonth = currentMonth -1;
            }

            if(currentMonth <=9) {
                currentMonthString = "0" + currentMonth;
            }

            startDate = currentYear+"-"+currentMonth+"-"+"01";
            endDate = currentYear+"-"+month+"-"+"01";

            dates.add(startDate);
            dates.add(endDate);
        } catch (Exception e) {
            logger.debug("Error while fetching last month due date");
        }
        return dates;
    }

    private List<String> getQuarterlyMonthDueDateForServiceNow() {
        List<String> dates = new ArrayList<>();
        String endDate=null;
        String startDate = null;
        int end = 0;
        String currentDateDb;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("Select now();");
            postgreSQLJDBC.closeConnection();
            currentDateDb = dbCurrentDate.toString();
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            if(currentYear == 1 || currentYear == 2 || currentYear == 3)
                currentYear = currentYear-1;

            List<String> quarterMonth = expectedPreviousQuarterMonths(currentMonth);
            String quarterFirstMonth = quarterMonth.get(0);
            String  quarterLastMonth =  quarterMonth.get(2);

            if(Integer.parseInt(quarterFirstMonth) <= 9)
                quarterFirstMonth = 0+quarterFirstMonth;
            if(Integer.parseInt(quarterLastMonth) <= 9)
                quarterLastMonth = 0+quarterLastMonth;

//            startDate = currentYear+ "-" +quarterFirstMonthString+ "-" +"01";
            startDate = quarterFirstMonth+"-"+"01"+"-"+currentYear;
            end = getEndDate(Integer.parseInt(quarterLastMonth),currentYear);
//            endDate = currentYear+"-"+quarterLastMonthString+"-"+end;
            endDate = quarterLastMonth+"-"+end+"-"+currentYear;

            dates.add(startDate);
            dates.add(endDate);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return dates;
    }

    private String getValueFromDictionary(String serviceNowColumnName,String serviceNowColumnValue){

        String dictionaryValue = serviceNowColumnValue;
        try{
            dictionaryValue = ParseConfigFile.getValueFromConfigFile(slifDictionaryConfigFilePath,slifDictionaryConfigFileName,serviceNowColumnName,serviceNowColumnValue);

            if(dictionaryValue == null){
                dictionaryValue = serviceNowColumnValue;
            }else if(dictionaryValue.equals("")){
                dictionaryValue = serviceNowColumnValue;
            }

        }catch (Exception e){
            dictionaryValue = serviceNowColumnValue;
        }

        return dictionaryValue;
    }

    private List<Integer> getSLIdFromListingPage(int supplierId, String[] contract, String[] slaId){
        List<Integer> slLists = new ArrayList<>();
        String response;
        int listId = 6;
        String payload = createPayloadForSLListingPage(supplierId,contract,slaId);
        try{
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);
            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);
            JSONArray dataArray = new JSONArray();
            dataArray = listDataResponseJSON.getJSONArray("data");
            JSONObject indRowDataJson;
            for(int i=0;i<dataArray.length();i++){
                indRowDataJson = dataArray.getJSONObject(i);
                Iterator<String> keys = indRowDataJson.keys();
                String columnName;
                Integer columnValue;
                while(keys.hasNext()){
                    String key = keys.next();
                    columnName = indRowDataJson.getJSONObject(key).get("columnName").toString();
                    if(columnName.equals("id")){
                        columnValue = Integer.parseInt(indRowDataJson.getJSONObject(key).get("value").toString().split(":;")[1]);
                        slLists.add(columnValue);
                    }
                }

            }
        }catch(Exception e){
            logger.debug("Exception while getting SL Ids from Sirion App");
        }
        return slLists;
    }

    private String createPayloadStringFromArray(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"id\": \"" + list[i] + "\"},";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

    private String createPayloadForReport(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += ""+ list[i] +",";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

    private String createPayloadForSLListingPage(int supplierId, String[] contract, String[] slaId){
        String payload = null;
        try{

            payload = "{ \"filterMap\": { \"entityTypeId\": 14, \"offset\": 0, \"size\": 2000, " +
                    "\"orderByColumnName\": \"id\", \"orderDirection\": \"desc nulls last\", " +
                    "\"filterJson\": { \"1\": { \"multiselectValues\": { \"SELECTEDDATA\": " +
                    "[ { \"id\": \""+supplierId+"\" } ] }, \"filterId\": 1, \"filterName\": \"supplier\", " +
                    "\"entityFieldHtmlType\": null, \"entityFieldId\": null }, \"2\": " +
                    "{ \"multiselectValues\": { \"SELECTEDDATA\": [ "+createPayloadStringFromArray(contract)+" ] }, " +
                    "\"filterId\": 2, \"filterName\": \"contract\", \"entityFieldHtmlType\": null, " +
                    "\"entityFieldId\": null }, \"124\": { \"multiselectValues\": { \"SELECTEDDATA\": " +
                    "[ "+createPayloadStringFromArray(slaId)+" ] }, " +
                    "\"filterId\": 124, \"filterName\": \"slItem\", \"entityFieldHtmlType\": null, " +
                    "\"entityFieldId\": null }, \"242\": { \"filterId\": 242, \"listId\": null, " +
                    "\"filterName\": \"calendarViewType\", \"filterShowName\": null, \"minValue\": null, " +
                    "\"maxValue\": null, \"min\": null, \"max\": null, \"multiselectValues\": " +
                    "{ \"SELECTEDDATA\": [ { \"id\": \"1001\", \"name\": \"Gregorian\", \"group\": null, " +
                    "\"type\": null, \"$$hashKey\": \"object:288\" } ] } } } } }";
        }catch(Exception e){
            logger.debug("Exception while creating payload for SL Listing Page");
        }
        return payload;
    }

    private String createPayloadForDestinationDaily(String name,int sourceId, int frequencyId,String[] slaId,String[] subCategory,String[] computationFrequency,int entityTypeId,String query){

        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","active");
        String[] email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","email").split(",");
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

        String timeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","time zone");
        int emailID = 81;
        String[] contractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract").split(",");
        String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance status").split(",");
        String[] performanceParentId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance parent id").split(",");

        String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":true," +
                "\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638," +
                "\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false," +
                "\"values\":\"" + name + "\"},\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Destination Integration Config\",\"id\":" + entityTypeId + "},\"multiEntitySupport\":false}," +
                "\"email\":{\"name\":\"email\",\"id\":" + emailID + ",\"multiEntitySupport\":false," +
                "\"values\":["+createPayloadStringFromArray(email)+"]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId + ",\"multiEntitySupport\":false}," +
                "\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + frequencyId + ",\"name\":\"Monthly\",\"parentId\":null}},\"canSupplierBeParent\":true," +
                "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\"\"}," +
                "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"sourceIntegrationConfig\":" +
                "{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + sourceId + ",\"name\":\"API AUTOMATION\",\"parentId\":null}}," +
                "\"query\":{\"name\":\"query\",\"id\":12651,\"multiEntitySupport\":false,\"values\":\""+query+"\"}," +
                "\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false," +
                "\"values\":["+createPayloadStringFromArray(contractId)+"]}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":" + 12648 +",\"multiEntitySupport\":false,\"values\":" +
                "["+createPayloadSLAStringFromArray(slaId,subCategory)+"]}," +
                "\"supplierId\":{\"name\":\"supplierId\",\"id\":12646,\"multiEntitySupport\":false," +
                "\"values\":{\"name\":\"Berkshire Hathaway - test 1\",\"id\":" + supplierId + "}}," +
                "\"configTimeZone\":{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + timeZone + ",\"name\":\"Asia/Kolkata (GMT +05:30)\",\"parentId\":null}}," +
                "\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":["+createPayloadSLAStringFromArray(performanceStatus,performanceParentId)+"]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":["+createPayloadStringFromArray(computationFrequency)+"]}}}}";

        return payload;
    }

    private int createDestinationDaily(String payload,CustomAssert customAssert){

        int destinationId = -1;
        try{
            APIResponse response = SLIF_Schemas.hitPostCreateDestination(executor, payload);

            if(response.getResponseCode() !=200){
                logger.error("Create destination API Response Code is not equal to 200");
                customAssert.assertTrue(false,"Create destination API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Destination API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Destination API Response is not a valid Json");

            }else {
                destinationId = getSourceId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully.");
                }
                else{
                    customAssert.assertTrue(false, "Destination ID not created successfully.");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating Destination Create API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Destination Create API " + e.getMessage());
        }

        return destinationId;
    }

    private List<String> getDailyMonthDueDate() {
        List<String> dates = new ArrayList<>();
        String endDate;
        String startDate;
        int end =0;
        String currentDateDb;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("Select now();");
            String currentMonthString = "";
            currentDateDb = dbCurrentDate.toString();
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            end = getEndDate(currentMonth,currentYear);

            if(currentMonth <=9)
                currentMonthString = 0+String.valueOf(currentMonth);
//            startDate = currentYear+ "-" +currentMonth+ "-" +01;
            startDate = currentMonthString+"-"+"01"+"-"+currentYear;
//            endDate = currentYear+"-"+currentMonth+"-"+end;
            endDate = currentMonthString+"-"+end+"-"+currentYear;
            dates.add(startDate);
            dates.add(endDate);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return dates;
    }

    private String createPayloadSLAStringFromArray(String[] slaItem, String[] slaCategory) {
        String payload = "";
        for (int i = 0; i < slaItem.length; i++) {
            payload += "{ \"id\": "+slaItem[i]+", \"parentId\": "+slaCategory[i]+" },";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
    }

    private List<String> getDailyMonthDueDateQuarterly() {
        List<String> dates = new ArrayList<>();
        String endDate=null;
        String startDate = null;
        int end = 0;
        String currentDateDb;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("Select now();");
            postgreSQLJDBC.closeConnection();
            currentDateDb = dbCurrentDate.toString();
            String quarterFirstMonthString = "";
            String quarterLastMonthString = "";
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            List<String> quarterMonth = expectedPresentQuarterMonths(currentMonth);
            int quarterFirstMonth = Integer.parseInt(quarterMonth.get(0));
            int quarterLastMonth =  Integer.parseInt(quarterMonth.get(2));

            if(quarterFirstMonth <= 9)
                quarterFirstMonthString = 0+String.valueOf(quarterFirstMonth);
            if(quarterLastMonth <= 9)
                quarterLastMonthString = 0+String.valueOf(quarterLastMonth);

//            startDate = currentYear+ "-" +quarterFirstMonthString+ "-" +"01";
            startDate = quarterFirstMonthString+"-"+"01"+"-"+currentYear;
            end = getEndDate(quarterLastMonth,currentYear);
//            endDate = currentYear+"-"+quarterLastMonthString+"-"+end;
            endDate = quarterLastMonthString+"-"+end+"-"+currentYear;

            dates.add(startDate);
            dates.add(endDate);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return dates;
    }

    private List<Map<String,String>> getServiceNowDataDaily(String serviceNowUrl,String tableName,String newDataCriteria,
                                                            String startTime,String endTime,String systemParamFields,CustomAssert customAssert) {

        List<Map<String,String>> serviceNowData = new ArrayList<>();

        int portNumber = 443;
        String protocolScheme = "https";
        String startDate = "";
        String endDate = "";
        String newStartDate = "";
        String newEndDate = "";

        startDate = getPreviousDate();
        endDate = getTodayDate();
        newStartDate = startDate+""+startTime;
        newEndDate = endDate+""+endTime;

        String jS = "javascript:gs.dateGenerate(%27";
        String operator = "BETWEEN";
        String order_by = "%5EORDERBYopened_at";

        String apiUrl = "/api/now/table/" + tableName + "?sysparm_query=" + newDataCriteria +
                operator + jS + newStartDate + "%27)@" + jS + newEndDate + "%27)"+order_by+"&sysparm_fields=" + systemParamFields + "&sysparm_display_value=true&sysparm_limit=2000";

//        String apiUrl = "/api/now/table/incident?sysparm_query=opened_at>=javascript:gs.dateGenerate('2020-05-01','05:30:01')^opened_at<=javascript:gs.dateGenerate('2020-06-01','05:29:59')^ORDERBYopened_at&sysparm_fields=number,state,priority,opened_at";

        try {
            HttpGet httpGetRequest = new HttpGet(apiUrl);

            httpGetRequest.addHeader("Accept", "application/json");
            httpGetRequest.addHeader("Authorization", "Basic YWRtaW46aFhhaUE1U1ZCcmI3");
            httpGetRequest.addHeader("Cookie", "glide_user_route=glide.bdfedf64b9e9d707bc156f6d44dce6c5; BIGipServerpool_dev68841=2441107466.55102.0000; glide_user_activity=U0N2Mzp2N1gxVGtMSEJwRWw5WXVQNWxSeXNqZTIyNHhsZHlVMTpaNG0xei9YbHV0MjNYS3J5RXE5dE5HMlJkYTRBMGluS2R4ME04TzJPaVJrPQ==; JSESSIONID=B5CDDA60333B04BFDEA9433AAB26B7E9; glide_session_store=72ED703ADB8150108BB3A455CA961921");
            HttpClient httpClient;
            APIUtils oAPIUtils = new APIUtils();

            httpClient = oAPIUtils.getHttpsClient();

            HttpHost target = new HttpHost(serviceNowUrl, portNumber, protocolScheme);

            HttpResponse response = httpClient.execute(target, httpGetRequest);

            String responseBody = EntityUtils.toString(response.getEntity());

            JSONObject responseBodyJson = new JSONObject(responseBody);

            JSONArray resultArray = responseBodyJson.getJSONArray("result");
            Map<String,String> excelMap;
            for(int i=0;i<resultArray.length();i++){

                Iterator<String> keys = resultArray.getJSONObject(i).keys();
                excelMap = new HashMap<>();

                while(keys.hasNext()) {
                    String key = keys.next();
                    String columnValue = "null";
                    try {
                        columnValue = resultArray.getJSONObject(i).get(key).toString();
                    }catch (Exception e){
                        customAssert.assertFalse(false,"Error while fetching service now data");
                    }
                    excelMap.put(key,columnValue);
                }
                serviceNowData.add(excelMap);
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while creating service now data ");
        }

        return serviceNowData;
    }

    private String getPreviousDate() {

        String previousDate = null;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select now();");
            postgreSQLJDBC.closeConnection();

            String currentDateDb = dbCurrentDate.get(0).toString();

            int currentMonth = Integer.parseInt(currentDateDb.substring(6,8));
            int currentYear =  Integer.parseInt(currentDateDb.substring(1,5));
            int currentDate =  Integer.parseInt(currentDateDb.substring(9,11));

            if(currentMonth == 1 && currentDate == 1){

                currentYear = currentYear -1;
                previousDate = currentYear + "-12-31";
            }else if(currentYear %2  == 0 && currentMonth == 3 && currentDate == 1){
                previousDate = currentYear + "-02-29";
            }else if(currentYear %2  != 0 && currentMonth == 3 && currentDate == 1){
                previousDate = currentYear + "-02-28";
            }else if((currentMonth == 2 || currentMonth == 4 || currentMonth == 6 || currentMonth == 8 || currentMonth == 9 || currentMonth == 11) && currentDate == 1){
                currentMonth = currentMonth -1;

                if(currentMonth<10) {
                    previousDate = currentYear + "-0" + currentMonth + "-31";
                }else{
                    previousDate = currentYear + currentMonth + "-31";
                }
            }else if((currentMonth == 5 || currentMonth == 7 || currentMonth == 10 || currentMonth == 12) && currentDate == 1){
                currentMonth = currentMonth -1;

                if(currentMonth<10) {
                    previousDate = currentYear + "-0" + currentMonth + "-30";
                }else{
                    previousDate = currentYear + currentMonth + "-30";
                }
            }else {
                if(currentMonth<10) {
                    currentDate = currentDate - 1;
                    if(currentDate<10) {
                        previousDate = currentYear + "-0" + currentMonth + "-" + "0" + currentDate;
                    }else{
                        previousDate = currentYear + "-0" + currentMonth + "-" + currentDate;
                    }
                }else{
                    currentDate = currentDate - 1;
                    if(currentDate<10) {
                        previousDate = currentYear + "-" + currentMonth + "-" + "0" + currentDate;
                    }else{
                        previousDate = currentYear + "-" + currentMonth + "-" + currentDate;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting previous Date");
        }
        return previousDate;
    }

    private List<String> expectedPresentQuarterMonths(int month){

        List<String> previousMonths = new ArrayList<>();

        if(month == 1 || month == 2 || month == 3){
            previousMonths = Arrays.asList("1","2","3");

        }
        else if(month == 4 || month == 5 || month == 6){
            previousMonths = Arrays.asList("4","5","6");
        }
        else if(month == 7 || month == 8 || month == 9){
            previousMonths = Arrays.asList("7","8","9");
        }
        else if(month == 10 || month == 11 || month == 12) {
            previousMonths = Arrays.asList("10", "11", "12");
        }
        return previousMonths;

    }

    private String getTodayDate() {

        String todayDate = "";
        String endDate=null;
        int end=0;
        String startDate = null;
        String currentDateDb;
        try {
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select now();");
            postgreSQLJDBC.closeConnection();

            currentDateDb = dbCurrentDate.toString();
            String currentMonthString="";
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));
            int currentDate =  Integer.parseInt(currentDateDb.substring(10,12));

            todayDate = currentYear+"-"+currentMonth+"-"+currentDate;
        } catch (Exception e) {
            logger.error("Exception while getting current Date");
        }
        return todayDate;
    }

    private boolean destinationDelete(int destinationId){

        Boolean destinationDelete=true;

        dbName = "slif";
        try{
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> results = postgreSQLJDBC.doSelect("select id from destination_integration_config where id='" +destinationId+ "';");

            int rest_id = Integer.parseInt(results.get(0).get(0));

            Boolean deleteDestination =deleteDest(rest_id);

            if(deleteDestination)
            {
                postgreSQLJDBC.deleteDBEntry("delete from destination_integration_config where id='" +destinationId+ " ';");
            }
        }catch (Exception e){
            destinationDelete = false;
        }

        return destinationDelete;
    }

    private Boolean deleteDest(int rest_id){
        Boolean updateDb=true;
        dbName = "slif";

        try{
            postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.deleteDBEntry("Update destination_integration_config set active='false' where id = "+rest_id+ "';");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private boolean deleteSource(int sourceId){
        Boolean updateDb=true;
        String dbName = "slif";

        try{
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.deleteDBEntry("Delete from source_integration_config where id=" +sourceId+ ";");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            logger.error("Error while updating db");
            updateDb = false;
        }
        return updateDb;
    }

    private String getDate(){
        String newDate = null;
        int newDay = 0;
        String currentDay = null;
        String currentMonth = null;
        try{

            String date = DateUtils.getCurrentDateInDD_MM_YYYY();
            int newMonth = Integer.parseInt(date.substring(3,5));
            int newYear = Integer.parseInt(date.substring(6,10));
            newDay = Integer.parseInt(date.substring(0, 2));

            int endDate = getEndDate(newMonth,newYear);
            if(newDay == endDate){
                newDay = 01;
                newMonth = newMonth +1;
            } else {
                newDay = newDay + 1;
            }

            if(newDay <=9) {
                currentDay = 0 + String.valueOf(newDay);
            }else {
                currentDay = String.valueOf(newDay);
            }

            if(newMonth <=9) {
                currentMonth = 0 + String.valueOf(newMonth);
            }else {
                currentMonth = String.valueOf(newMonth);
            }

            newDate = currentMonth+"-"+ currentDay +"-"+newYear;

        }catch (Exception e){
            logger.error("Dates expected is wrong");
        }
        return newDate;
    }

}
