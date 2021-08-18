package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.api.clientAdmin.masterslacategorys.CreateClientSLACategory;
import com.sirionlabs.api.clientAdmin.masterslacategorys.CreateClientSLAItem;
import com.sirionlabs.api.clientAdmin.masterslacategorys.CreateClientSLASubCategory;
import com.sirionlabs.api.clientAdmin.masterslacategorys.SLASubCategories;
import com.sirionlabs.api.clientSetup.masterslacategorys.Masterslacategorys;
import com.sirionlabs.api.clientSetup.masterslacategorys.MasterslacategorysList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.elasticSearch.ElasticSearch;
import com.sirionlabs.api.listRenderer.ListRendererListData;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;

import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.sql.SQLException;

import java.util.*;
import java.util.regex.Pattern;

import com.sirionlabs.helper.EntityOperationsHelper;


public class test_SLIF_EndUser extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_SLIF_EndUser.class);
    int sourceId;
    int sourceIdForDestination;
    int destinationId;

    private int slEntityTypeId;
    private int cslEntityTypeId;

    private String slConfigFilePath;
    private String slConfigFileName;

    private String auditLogUser;

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();
    private String slEntity = "service levels";

    int slaCategory;
    int clientSLACategory;
    int slaSubCategory;
    int slaItem;

    String adminUserName;
    String adminPassword;

    String epochTime;

    int frequencyId;        //dailyfreq=1,lastmonthfreq=2, quarterlymonthfreq=3
    String computationFrequency = "1003";        //Monthly Date
    int contractId = 136866;
    int entityTypeId = 346;
    int reportId = 514;

    String configFilePath;
    String configFileName;

    private String slifDictionaryConfigFilePath;
    private String slifDictionaryConfigFileName;

    private String dbHostName;
    private String dbPortName;
    private String dbName;
    private String slifDBName;
    private String dbUserName;
    private String dbPassword;

    @BeforeClass
    public void BeforeClass(){


        slifDictionaryConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLIFDictionaryConfigFilePath");
        slifDictionaryConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLIFDictionaryConfigFileName");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");

        Check check = new Check();

        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        check.hitCheck(adminUserName,adminPassword);

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        slifDBName = "slif";
    }

    @DataProvider(name = "flowsToTest", parallel = false)
    public Object[][] flowsToTest() {

        List<Object[]> allTestData = new ArrayList<>();

//        int frequencyDaily = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination frequency daily","frequency daily"));
//        int slaDaily = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination frequency daily","sla daily"));
//        int slaCategoryDaily = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination frequency daily","slasubcategory daily"));

        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flowstotest").split(",");

        for(String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "flowsToTest", enabled = true)
    public void TestSLIFPositiveFlow(String flowToTest){
        CustomAssert customAssert = new CustomAssert();

        int destinationId = -1;

        try{

            int slaId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"sla"));
            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","positive flow"));
            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest, "frequency"));
            int slaSubCategory = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"slasubcategory"));
            int contract = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract"));

            String performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","performance status");
            String computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"computation frequency");

            destinationId = createDestination(sourceId, frequencyId, slaId, slaSubCategory,computationFrequency,customAssert);

            List<String> listingData = new ArrayList<>();

            //Frequency Id 2 means monthly
            if(frequencyId == 2) {
                String startDate = getLastMonthDueDate().get(0);
                String endDate = getLastMonthDueDate().get(1);
                ArrayList<Integer> cslDBIdList = new ArrayList<>();
                listingData = getCSLDataFromListingPage(contract,performanceStatus,slaId,computationFrequency,startDate,endDate,cslDBIdList);

                deleteDataAtIndex(cslDBIdList,customAssert);


            }//Frequency Id 3 means quarterly
            else if(frequencyId == 3) {
                String startDate = getQuarterlyMonthDueDate().get(0);
                String endDate = getQuarterlyMonthDueDate().get(1);

                ArrayList<Integer> cslDBIdList = new ArrayList<>();
                listingData = getCSLDataFromListingPage(contract,performanceStatus,slaId,computationFrequency,startDate,endDate,cslDBIdList);

            }

            String filePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";

            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filename");

            Boolean downLoadCSLDataStatus= downloadCSLDataFromDestination(computationFrequency,frequencyId,supplierId,slaId,contract,performanceStatus,filePath,fileName,customAssert);

            if(downLoadCSLDataStatus) {
                List<String> excelData = getCSLDataFromExcelSheet(filePath, fileName);

                Boolean result = compareExcelAndListingData(listingData, excelData, customAssert);
                if (result)
                    customAssert.assertTrue(true, "Eligible CSL has validated successfully");
            }

            //Validate ServiceNowIds
            String serviceNowUrl = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","serviceNowUrl");
            String tableName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","tableName");
            String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","newDataCriteria");
            String startDate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","startDate");
            String endDate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","endDate");
            String systemParamFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","systemparamfields");

            List<Map<String,String>> serviceNowData = getServiceNowData(serviceNowUrl, tableName, newDataCriteria, startDate, endDate, systemParamFields,customAssert);

            String excelFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
            String excelFileName = "Raw Data From Destination.xlsx";

            compareTestDataWithIncFetchedFromSnow(serviceNowData,excelFilePath,excelFileName,customAssert);



        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating TestSLIFPositiveFlow in the main Test Method " + e.getMessage());
        }finally {
//            destinationDelete(destinationId);
        }

    }

    @Test(enabled = false)
    public void TestDestinationDailyFlow(){
        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "frequency daily";
        try {

            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "source ids", "positive flow"));;

            int contract = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "contract"));
            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "supplier"));
            String performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "performance status");

            int slaId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sla"));
            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "frequency"));
            int slaSubCategoryId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "slasubcategory"));
            String computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "computation frequency");

            String payload = createPayloadForDestinationDaily(sourceId, frequencyId, slaId, slaSubCategoryId, computationFrequency);

            int destinationId = createDestination(payload, customAssert);

            List<String> listingData = new ArrayList<>();
            if (frequencyId == 1) {
                String startDate = getDailyMonthDueDate().get(0);
                String endDate = getDailyMonthDueDate().get(1);

                ArrayList<Integer> cslDBIdList = new ArrayList<>();
                listingData = getCSLDataFromListingPage(contract,performanceStatus,slaId,computationFrequency,startDate,endDate,cslDBIdList);
            }


            String filePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filename");

            Boolean downloadStatus = downloadCSLDataFromDestination(computationFrequency, frequencyId, supplierId, slaId, contractId, performanceStatus, filePath, fileName, customAssert);

            if (downloadStatus) {
                List<String> excelData = getCSLDataFromExcelSheet(filePath, fileName);

                Boolean result = compareExcelAndListingData(listingData, excelData, customAssert);
                if (result)
                    customAssert.assertTrue(true, "Eligible CSL has validated successfully");
            }

            //Validate ServiceNowIds
            String serviceNowUrl = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","serviceNowUrl");
            String tableName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","tableName");
            String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","newDataCriteria");

            String startDate = getPreviousDate() + "%27%2C%2700:00:00";
            String endDate = getPreviousDate() + "%27%2C%2723:59:59";

            String systemParamFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service now data fetch","systemparamfields");

            List<Map<String,String>> serviceNowData = getServiceNowData(serviceNowUrl, tableName, newDataCriteria, startDate, endDate, systemParamFields,customAssert);

            String excelFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";
            String excelFileName = "Raw Data From Destination.xlsx";

            compareTestDataWithIncFetchedFromSnow(serviceNowData,excelFilePath,excelFileName,customAssert);


        }catch (Exception e){
            logger.error("Exception while Testing Destination Daily Flow in the main Main Method" + e.getMessage());
            customAssert.assertTrue(false,"Exception while Testing Destination Daily Flow in the main Main Method" + e.getMessage());
        }
    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioInvalidURLProvided(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String invalidServiceNowUrl = "https://dev6800011111111.service-now.com/api/now/table/incident";
        String expectedErrorMsg = "Source Validation Failed - source with configured URL doesn't exist";

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",invalidServiceNowUrl);

            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioInvalidCredentials(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = "https://dev68841.service-now.com/api/now/table/incident";

        String expectedErrorMsg = "Source Validation Failed - authentication failed";
        String invalidPassword = "hXa";
        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",invalidPassword);

            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioUniqueDataCriteriaDoesNotExist(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = "https://dev68841.service-now.com/api/now/table/incident";

        String expectedErrorMsg = "Source Validation Failed - Unique Data Criteria doesn't exist.";
        String password = "hXaiA5SVBrb7";
        String uniqueDataCriteria = "number1";

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);


            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioNewDataCriteriaDoesNotExist(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","url");

        String expectedErrorMsg = "Source Validation Failed - New Data Criteria doesn't exist.";
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","password");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String newDataCriteria = "opened_at1";

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("newDataCriteria",newDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);


            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioColDoesNotExist(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","url");

        String expectedErrorMsg = "Source Validation Failed - Following Columns doesn't exist: number1, priority1, state1.";
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","password");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","new data criteria");

        String columns = "number1,state1,priority1";

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("newDataCriteria",newDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("columns",columns);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);

            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioColDoesNoEligibleDataFound(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = "https://dev72696.service-now.com/api/now/table/incident";

        String expectedErrorMsg = "Source Validation Failed - No eligible data found.";


        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","password");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","new data criteria");
        String columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","columns");

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("newDataCriteria",newDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("columns",columns);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);

            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceNegativeScenarioSourceUnReachableDown(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow"));
        String url = "https://dev87648.service-now.com/api/now/table/incident";

        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","password");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","new data criteria");
        String columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","columns");

        try{
            String editResponse =  editSource(sourceId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("newDataCriteria",newDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("columns",columns);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);

            String editResponseNegativeScenario = editSource(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestDestNegScenarioInvalidURLProvidedUpdate(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidURLInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";

        try{
            String editResponse =  editDestination(destinationId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            String editResponseNegativeScenario = editDestination(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario in the main method");
        }finally {
            resetSource(sourceId,customAssert);
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestDestNegScenarioNewDataCriteriaDoesNotExistUpdate(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidNewDataCriteriaInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - New Data Criteria doesn't exist.";

        try{
            String editResponse =  editDestination(destinationId);
            JSONObject editResponseJson = new JSONObject(editResponse);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);
            String editResponseNegativeScenario = editDestination(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }
        finally {
            resetSource(sourceId,customAssert);
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestDestNegScenarioColumnsDoesNotExistUpdate(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidColumnsInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - Following Columns doesn't exist: number1, priority1, state1.";

        try{
            String editResponse =  editDestination(destinationId);
            JSONObject editResponseJson = new JSONObject(editResponse);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
//            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("password").put("values",password);


            String editResponseNegativeScenario = editDestination(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }
        finally {
            resetSource(sourceId,customAssert);
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestDestNegScenarioUniqueDataCriteriaDoesNotExistUpdate(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidUniqueDataCriteriaInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - Unique Data Criteria doesn't exist.";

        try{
            String editResponse =  editDestination(destinationId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            String editResponseNegativeScenario = editDestination(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }
        finally {
            resetSource(sourceId,customAssert);
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestDestNegSceenarioInvalidCredentialsUpdate(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidCredentialsInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - authentication failed";

        try{
            String editResponse =  editDestination(destinationId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            String editResponseNegativeScenario = editDestination(editResponseJson.toString());

            JSONObject editResponseJsonNegSce = new JSONObject(editResponseNegativeScenario);

            String errorMsgActual = editResponseJsonNegSce.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).get("message").toString();

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidCredentials in the main method");
        }
        finally {
            resetSourceCredentials(sourceId);
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestDestNegScenarioInvalidURLProvided_TestData(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidURLInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";

        try{
            String errorMsgActual =  checkErrorMsgOnTestData(destinationId);

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }finally {
            resetSource(sourceId,customAssert);
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestDestNegScenarioInvalidUniqueDataCriteriaProvided_TestData(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidUniqueDataCriteriaInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";

        try{
            String errorMsgActual =  checkErrorMsgOnTestData(destinationId);

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }finally {
            resetSource(sourceId,customAssert);
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestDestNegScenarioInvalidNewDataCriteriaProvided_TestData(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));
        sourceUpdateInvalidNewDataCriteriaInDB(sourceId);

        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";

        try{
            String errorMsgActual =  checkErrorMsgOnTestData(destinationId);

            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }finally {
            resetSource(sourceId,customAssert);
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestDestNegScenarioInvalidColumnsProvided_TestData(){

        CustomAssert customAssert = new CustomAssert();
        int destinationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination ids","negative flow destination scenario"));
        int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source ids","negative flow destination scenario"));

        sourceUpdateInvalidColumnsInDB(sourceId);
        String expectedErrorMsg = "Source Validation Failed - source is unreachable/down";

        try{
            String errorMsgActual =  checkErrorMsgOnTestData(destinationId);

            if(errorMsgActual == null){
                customAssert.assertTrue(false,"Expected Error Actual Msg is Null");
            }
            customAssert.assertEquals(errorMsgActual,expectedErrorMsg);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating NegativeScenarioInvalidURLProvided in the main method");
        }finally {
            resetSource(sourceId,customAssert);
        }
        customAssert.assertAll();
    }

    @Test(priority = 0,enabled = false)
    public void TestCreateSource1(){

        CustomAssert customAssert = new CustomAssert();

        int sourceId = -1;
        String newApiUrl = "/slintegration/source/new";

        String apiUrl = "/slintegration/source/create";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(newApiUrl,headers).getResponse();
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("New API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }
            String payload= createPayloadForSource1();
            response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }
    }

    @Test(priority = 1,dependsOnMethods = "TestCreateSource",enabled = false)
    public void TestEditSource(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/source/edit";
        String editUrlGet = apiUrl + "/" + sourceId;
        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(editUrlGet,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }


            String payload;

            payload = "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1005,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"multiEntitySupport\":false},\"password\":{\"name\":\"password\",\"values\":\"AC8S9RokpqaV\",\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"values\":" + sourceId + ",\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"multiEntitySupport\":false},\"active\":{\"name\":\"active\",\"values\":" + true + ",\"multiEntitySupport\":false},\"dynamicMetadata\":{},\"creationDate\":{\"name\":\"creationDate\",\"id\":12626,\"values\":\"03-13-2020\",\"displayValues\":\"03-13-2020\",\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"sourceType\":{\"name\":\"sourceType\",\"values\":1,\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"values\":\"Service Now 2\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"restEndPointSchema\":{\"name\":\"restEndPointSchema\",\"values\":{\"id\":1,\"clientId\":null,\"name\":\"Service Now\",\"sourceTypeId\":null,\"jsonSchema\":{\"url\":null,\"attributes\":{\"sysparam_query\":{\"title\":\"Filter Query\",\"description\":\"Encoded query used to filter the result set.\",\"type\":\"string\",\"default\":null},\"sysparam_fields\":{\"title\":\"Response Fields\",\"description\":\"Comma-separated list of field names to return in the response.\",\"type\":\"string\",\"default\":null}},\"dataCriteria\":null}},\"multiEntitySupport\":false},\"description\":{\"name\":\"description\",\"values\":\"updated value demo 123\",\"multiEntitySupport\":false},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Clients\",\"id\":2},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Client\",\"id\":2},{\"name\":\"Sirion\",\"id\":1},{\"name\":\"Non-User\",\"id\":3},{\"name\":\"Supplier \",\"id\":4}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"multiEntitySupport\":false},\"restEndPoint\":{\"name\":\"restEndPoint\",\"values\":{\"id\":null,\"clientId\":null,\"url\":\"https://dev96838.service-now.com/api/now/table/incident\",\"attributes\":{\"offset_query\":\"offset\",\"sysparm_query\":\"limit\"},\"dataCriteria\":\"name\",\"columns\":\"some column\"},\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"values\":\"a@b.com,c@d.com, g@h.com\",\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":2,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"modificationDate\":{\"name\":\"modificationDate\",\"id\":12627,\"values\":\"03-15-2020\",\"displayValues\":\"03-15-2020\",\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":338,\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Authority Notification\",\"id\":1658},{\"name\":\"Business Case\",\"id\":1394},{\"name\":\"Other\",\"id\":1922},{\"name\":\"Outsourcing Committee Approval\",\"id\":1526},{\"name\":\"Outsourcing Materiality Assessment\",\"id\":1130},{\"name\":\"Performance Review\",\"id\":1790},{\"name\":\"Risk Assessment\",\"id\":1262}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":50,\"enableListView\":false,\"filterName\":\"Change Request\"},\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"name\":\"commentDocuments\",\"multiEntitySupport\":false}},\"authenticationType\":{\"name\":\"authenticationType\",\"values\":{\"name\":\"Basic Authentication\",\"id\":1},\"multiEntitySupport\":false},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"user\":{\"name\":\"user\",\"values\":\"admin\",\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"multiEntitySupport\":false}}}}";

            response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json for scenario 2");
            }

            if(!responseBody.contains("success")){
                customAssert.assertTrue(false,"Edit Done Unsuccessfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 2,dependsOnMethods = "TestEditSource",enabled = false)
    public void TestShowSource(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/source/show/"  + sourceId;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONObject responseBodyJson = new JSONObject(responseBody);

                String activeStatus = responseBodyJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").get("values").toString();

                if (!activeStatus.equalsIgnoreCase("true")) {
                    customAssert.assertTrue(false, "Active status expected as true " + "Actual Status " + activeStatus);
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 0,enabled = false)
    public void TestCreateSource(){

        CustomAssert customAssert = new CustomAssert();

        String newApiUrl = "/slintegration/source/new";

        String apiUrl = "/slintegration/source/create";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(newApiUrl,headers).getResponse();
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("New API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            String payload;

            payload = "{\"body\":{\"data\":{\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1005,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"multiEntitySupport\":false},\"password\":{\"name\":\"password\",\"id\":12637,\"values\":\"AC8S9RokpqaV\",\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"values\":1,\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"multiEntitySupport\":false},\"active\":{\"name\":\"active\",\"id\":12625,\"values\":true,\"multiEntitySupport\":false},\"dynamicMetadata\":{},\"creationDate\":{\"name\":\"creationDate\",\"id\":12626,\"values\":\"04-13-2020\",\"displayValues\":\"04-13-2020\",\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"sourceType\":{\"name\":\"sourceType\",\"values\":1,\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12623,\"values\":\"Source 1\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"restEndPointSchema\":{\"name\":\"restEndPointSchema\",\"values\":{\"id\":1,\"clientId\":null,\"name\":\"Service Now\",\"sourceTypeId\":null,\"jsonSchema\":{\"attributes\":{\"sysparam_query\":{\"name\":\"sysparam_query\",\"title\":\"Filter Query\",\"description\":\"Encoded query used to filter the result set.\",\"type\":\"string\",\"default\":null},\"sysparam_fields\":{\"name\":\"sysparam_fields\",\"title\":\"Response Fields\",\"description\":\"Comma-separated list of field names to return in the response.\",\"type\":\"string\",\"default\":null}}}},\"multiEntitySupport\":false},\"description\":{\"name\":\"description\",\"id\":12624,\"values\":\"Description 1\",\"multiEntitySupport\":false},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Clients\",\"id\":2},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Client\",\"id\":2},{\"name\":\"Sirion\",\"id\":1},{\"name\":\"Non-User\",\"id\":3},{\"name\":\"Supplier \",\"id\":4}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"multiEntitySupport\":false},\"restEndPoint\":{\"name\":\"restEndPoint\",\"values\":{\"id\":null,\"clientId\":null,\"url\":\"https://dev96838.service-now.com/api/now/table/incident\",\"attributes\":{\"limit\":100},\"newDataCriteria\":\"resolved_at\",\"uniqueDataCriteria\":\"resolved_at\",\"columns\":\"resolved_by, resolved_at\"},\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"values\":\"abc@ghi.com\",\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":2,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"modificationDate\":{\"name\":\"modificationDate\",\"id\":12627,\"values\":\"04-14-2020\",\"displayValues\":\"04-14-2020\",\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":338,\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Authority Notification\",\"id\":1658},{\"name\":\"Business Case\",\"id\":1394},{\"name\":\"Other\",\"id\":1922},{\"name\":\"Outsourcing Committee Approval\",\"id\":1526},{\"name\":\"Outsourcing Materiality Assessment\",\"id\":1130},{\"name\":\"Performance Review\",\"id\":1790},{\"name\":\"Risk Assessment\",\"id\":1262}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":50,\"enableListView\":false,\"filterName\":\"Change Request\"},\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"name\":\"commentDocuments\",\"multiEntitySupport\":false}},\"authenticationType\":{\"name\":\"authenticationType\",\"id\":12635,\"values\":{\"name\":\"Basic Authentication\",\"id\":1},\"multiEntitySupport\":false},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"user\":{\"name\":\"user\",\"values\":\"admin\",\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"multiEntitySupport\":false}}}}";
            response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 3,enabled = false)
    public void TestCreateDestination(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/destination/create";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            String payload;

            int frequencyId = 1;
            int computationFrequency = 1003;        //Monthly Date
            int contractId = 136866;
            int slaId= 1233;
            int slaSubCategory= 1142;
            payload = createPayloadForDestination(sourceIdForDestination,contractId,slaSubCategory,slaId,frequencyId,computationFrequency);

            try {


                PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, dbName, dbUserName, dbPassword);

                postgreSQLJDBC.updateDBEntry("update source_integration_config set active = true where id = " + sourceIdForDestination);
                postgreSQLJDBC.closeConnection();

            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while updating DB");
            }

            APIResponse response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                destinationId = getSourceId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }


        customAssert.assertAll();

    }

    @Test(priority = 4,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestEditDestination(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/destination/edit";

        String editUrlGet = apiUrl + "/" + destinationId;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(editUrlGet,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200 for scenario 1");
                customAssert.assertTrue(false,"API Response Code is not equal to 200 for scenario 1");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response Code is not equal to 200 for scenario 2");
            }

            String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":false,\"multiEntitySupport\":false},\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"values\":{\"id\":2},\"multiEntitySupport\":false},\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"values\":\"02-23-2020 08:40:47\",\"multiEntitySupport\":false},\"supplierId\":{\"name\":\"supplier\",\"id\":12646,\"values\":{\"name\":\"Berkshire Hathaway\",\"id\":1174},\"multiEntitySupport\":false},\"sourceIntegrationConfig\":{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"values\":{\"id\":" + sourceIdForDestination + "},\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"values\":" + destinationId + ",\"multiEntitySupport\":false},\"query\":{\"name\":\"query\",\"id\":12651,\"values\":\"abcdef\",\"multiEntitySupport\":false},\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"values\":[{\"name\":\"Walmart\",\"id\":1173}],\"multiEntitySupport\":false},\"configTimeZone\":{\"name\":\"timeZone\",\"id\":12652,\"values\":{\"name\":\"Europe/Paris (GMT +01:00)\",\"id\":1},\"multiEntitySupport\":false},\"slaIds\":{\"name\":\"slaIds\",\"id\":12648,\"values\":[{\"name\":\"Abandoned Call Rate (Europe)\",\"id\":1173,\"parentId\":1076,\"parentName\":\"Change/Release Management | Aeronautics1\",\"group\":\"Change/Release Management | Aeronautics1\"},{\"name\":\"CSAT Overall Post Ticket Closure User Dissatisfaction (North America)\",\"id\":1172,\"parentId\":1076,\"parentName\":\"Change/Release Management | Aeronautics1\",\"group\":\"Change/Release Management | Aeronautics1\"}],\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638,\"values\":\"service now demo testing\",\"multiEntitySupport\":false},\"description\":{\"name\":\"description\",\"id\":12639,\"values\":\"this is test config for demo\",\"multiEntitySupport\":false},\"creationDate\":{\"name\":\"creationDate\",\"id\":12642,\"values\":\"02-23-2020 08:40:47\",\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"values\":\"kush.saluja@sirionlabs.com,avnish@sirionlabs.com,kamal.negi@sirionlabs.com\",\"multiEntitySupport\":false},\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"values\":1005,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"canSupplierBeParent\":true,\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"contractingHubs\":{\"name\":\"contractingHubs\",\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"dynamicMetadata\":{},\"lastSyncDate\":{\"name\":\"lastSyncDate\",\"id\":12644,\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"projectLevels\":{\"name\":\"projectLevels\",\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"Clients\",\"id\":2},\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Client\",\"id\":2},{\"name\":\"Sirion\",\"id\":1},{\"name\":\"Non-User\",\"id\":3},{\"name\":\"Supplier \",\"id\":4}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false}},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"multiEntitySupport\":false},\"dateModified\":{\"name\":\"dateModified\",\"id\":12643,\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"values\":2,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":346,\"multiEntitySupport\":false},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":10,\"enableListView\":true,\"filterName\":\"User\"},\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":{\"api\":null,\"autoComplete\":false,\"data\":[{\"name\":\"Authority Notification\",\"id\":1658},{\"name\":\"Business Case\",\"id\":1394},{\"name\":\"Other\",\"id\":1922},{\"name\":\"Outsourcing Committee Approval\",\"id\":1526},{\"name\":\"Outsourcing Materiality Assessment\",\"id\":1130},{\"name\":\"Performance Review\",\"id\":1790},{\"name\":\"Risk Assessment\",\"id\":1262}],\"size\":null,\"sizeLimit\":null,\"enableListView\":false,\"filterName\":null},\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":50,\"enableListView\":false,\"filterName\":\"Change Request\"},\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"name\":\"commentDocuments\",\"multiEntitySupport\":false}},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"multiEntitySupport\":false}}}}";

            response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200 for scenario 2");
                customAssert.assertTrue(false,"API Response Code is not equal to 200 for scenario 2");
            }

            responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json for scenario 2");
            }

            if(!responseBody.contains("success")){
                customAssert.assertTrue(false,"Edit Done Unsuccessfully");
            }


        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 5,dependsOnMethods = "TestEditDestination",enabled = false)
    public void TestShowDestination(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/destination/show/"  + destinationId;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONObject responseBodyJson = new JSONObject(responseBody);

                String activeStatus = responseBodyJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").get("values").toString();

                if (activeStatus.equalsIgnoreCase("true")) {
                    customAssert.assertTrue(false, "Active status expected as true " + "Actual Status " + activeStatus);
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 6,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestSupplier(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/supplier";


        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");


            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray supplierJsonArray = new JSONArray(responseBody);

                if (supplierJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "Suppliers array size is equal to zero");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 7,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestSLA(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/SLA/" + 1124;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray slJsonArray = new JSONArray(responseBody);

                if (slJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "Suppliers array size is equal to zero");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 8,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestContract(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/contract/" + 1124;

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray slJsonArray = new JSONArray(responseBody);

                if (slJsonArray.length() == 0) {
                    customAssert.assertTrue(false, "Contract array size is equal to zero");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 9,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestFrequency(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/frequency";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");


            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray responseBodyArray = new JSONArray(responseBody);

                if (responseBodyArray.length() == 0) {
                    customAssert.assertTrue(false, "Frequency Response Size is equal To Zero");
                }

                if (!(responseBody.contains("Daily") && responseBody.contains("Monthly") && responseBody.contains("Quarterly"))) {
                    customAssert.assertTrue(false, "One of the frequency Daily Monthly or Quarterly is not present");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(priority = 10,dependsOnMethods = "TestCreateDestination",enabled = false)
    public void TestTimeZone(){

        CustomAssert customAssert = new CustomAssert();

        String apiUrl = "/slintegration/timezone";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");


            APIResponse response = executor.get(apiUrl,headers).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                JSONArray responseBodyArray = new JSONArray(responseBody);

                if (responseBodyArray.length() == 0) {
                    customAssert.assertTrue(false, "Timezone Response Size is equal To Zero");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        customAssert.assertAll();

    }

    @Test(enabled = false)
    public void TestSourceFailure(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        try{

            slaCategory = createSLACategory(customAssert);
            if(slaCategory == -1){
                customAssert.assertTrue(false,"SlaCategory not created");
                customAssert.assertAll();
                return;
            }

            clientSLACategory = createClientSLACategory(slaCategory,customAssert);
            if(clientSLACategory == -1){
                customAssert.assertTrue(false,"ClientSlaCategory not created");
                customAssert.assertAll();
                return;
            }

            slaSubCategory = createClientSLASubCategory(clientSLACategory,customAssert);
            if(slaSubCategory == -1){
                customAssert.assertTrue(false,"SlaSubCategory not created");
                customAssert.assertAll();
                return;
            }

            slaItem = createClientSLAItem(slaSubCategory,customAssert);
            if(slaItem == -1){
                customAssert.assertTrue(false,"SlaItem not created");
                customAssert.assertAll();
                return;
            }

            //Login With End USer
            Check check = new Check();
            check.hitCheck();

            String flowToTest = "sl automation flow";

            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
            int serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest,PCQ,DCQ,customAssert);

            slToDelete.add(serviceLevelId);
            if(serviceLevelId != 1){
                Boolean editStatus = editSLAItems(serviceLevelId,clientSLACategory,slaSubCategory,slaItem,customAssert);

                if(!editStatus){
                    customAssert.assertTrue(false,"Error while editing SLA Items");
                }

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active
                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                ArrayList<String> childServiceLevelIdList = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                addCSLToDelete(childServiceLevelIdList);
            }else {

                customAssert.assertTrue(false,"Service Level Id not created");
                customAssert.assertAll();
                return;
            }

            check.hitCheck(adminUserName,adminPassword);
            String url1 = "https://dev68841.service-now.com/api/now/table/incident";
            String userName1 = "admin";
            String password1 = "hXaiA5SVBrb7";
            sourceId = createSourceForDestination(url1, userName1, password1);
            destinationId = createDestination(sourceId,slaSubCategory,slaItem,frequencyId,computationFrequency,customAssert);

            Boolean negative=sourceIsNotAuthenticated(sourceId);

            epochTime = getEpochTime(120);
            Boolean dbUpdateStatus = updateTriggerTimeInSLIFQartzDB(epochTime,destinationId);

            List<List<String>> currentTimeStamp = getCurrentTimeStamp();
            if(negative){
                String subjectLine = "Data fetch request for  " +destinationId+ " completed";
                List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTableFailure(subjectLine, currentTimeStamp.get(0).get(0));
            }
        }
        catch(Exception e)
        {
            customAssert.assertAll();
        }
    }

//    @AfterClass
//    public void afterClass(){
//        Check check = new Check();
//        check.hitCheck();
//
//        logger.debug("Number CSL To Delete " + cslToDelete.size());
//        EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);
//
//		logger.debug("Number SL To Delete " + slToDelete.size());
//		EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);
//
//        try {
//            String dbHostAddress = "192.168.2.157";
//            String dbPortName = "5432";
//            String dbName = "SL-Automation";
//            String dbUserName = "postgres";
//            String dbPassword = "postgres";
//
//            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
//
//            postgreSQLJDBC.updateDBEntry("update sla set active = false where id = " + slaCategory);
//            postgreSQLJDBC.updateDBEntry("update sla_category set active = false where id = " + clientSLACategory);
//            postgreSQLJDBC.updateDBEntry("update slasub_category set active = false where id = " + slaSubCategory);
//            postgreSQLJDBC.updateDBEntry("update sla_item set active = false where id = " + slaItem);
//            postgreSQLJDBC.closeConnection();
//
//        }catch (Exception e){
//            logger.error("Exception while updating DB");
//        }
//    }

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

    private String createPayloadForSource(String url, String userName, String password){
        String payload;
        String name = "Gaurav";
        String emailId = "1232";
        String toolId = "1";  // For Service Now;

        String column = "number,priority,state";
        String newDataCriteria = "opened_at";
        String uniqueDataCriteria = "number";
        String active="false";

        String authenticationType = "1";

        payload = "{\"body\":{\"data\":{\"password\":{\"name\":\"" +
                "password\",\"id\":12637,\"multiEntitySupport\":false,\"values\":\"" + password + "\"}," +
                "\"active\":{\"name\":\"active\",\"id\":12625,\"values\":" +active+ ",\"multiEntitySupport\":false}," +
                "\"name\":{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"restEndPointSchema\":{\"name\":\"restEndPointSchema\",\"id\":12629,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" +toolId + ",\"clientId\":null,\"name\":\"Service Now\",\"sourceType\":null,\"jsonSchema\":null}}," +
                "\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Source Integration Config\",\"id\":338},\"multiEntitySupport\":false}," +
                "\"restEndPoint\":{\"name\":\"restEndPoint\",\"multiEntitySupport\":false,\"values\":" +
                "{\"newDataCriteria\":\"" + newDataCriteria + "\",\"uniqueDataCriteria\":\"" + uniqueDataCriteria + "\"," +
                "\"url\":\"" + url + "\",\"columns\":\"" + column + "\"}}," +
                "\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false,\"" +
                "values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":" + emailId + "}]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":338,\"multiEntitySupport\":false}," +
                "\"authenticationType\":{\"name\":\"authenticationType\",\"id\":12635,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + authenticationType + ",\"name\":\"Basic Authentication\",\"parentId\":null}}," +
                "\"user\":{\"name\":\"user\",\"multiEntitySupport\":false,\"values\":\"" + userName + "\"}}}}";

        return payload;
    }

    private String createPayloadForDestination(int sourceId,int contractId,int subCategory,int slaId,int frequencyId,int computationFrequency){

        String name = "Gaurav" + DateUtils.getCurrentTimeStamp();
        int entityTypeId = 346;
//        int frequencyId = 2;
        int performanceStatus = 4;              //Overdue
//        int computationFrequency = 1003;        //Monthly Date
        int timeZone = 8;
        int email = 81;
//        int slaId = 12648;
//        int contractId = 1110;
        int supplierId = 1050;
        String datePattern = "05-27-2020";

        String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":true," +
                "\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638," +
                "\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false," +
                "\"values\":\"" + name + "\"},\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Destination Integration Config\",\"id\":" + entityTypeId + "},\"multiEntitySupport\":false}," +
                "\"email\":{\"name\":\"email\",\"id\":" + email + ",\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":1232}]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId + ",\"multiEntitySupport\":false}," +
                "\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + frequencyId + ",\"name\":\"Monthly\",\"parentId\":null}},\"canSupplierBeParent\":true," +
                "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\"" + datePattern + "\"}," +
                "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"sourceIntegrationConfig\":" +
                "{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + sourceId + ",\"name\":\"API AUTOMATION\",\"parentId\":null}}," +
                "\"query\":{\"name\":\"query\",\"id\":12651,\"multiEntitySupport\":false,\"values\":\"test\"}," +
                "\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Sirion Performance Contracts ( CO01035 )\",\"id\":" + contractId + "}]}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":" + 12648 +",\"multiEntitySupport\":false,\"values\":" +
                "[{\"name\":\"QuantasSLITEM\",\"id\":" + slaId + ",\"parentId\":" + subCategory + ",\"parentName\":" +
                "\"Quantasubcat2 | QuantasSL\",\"group\":\"Quantasubcat2 | QuantasSL\"}]}," +
                "\"supplierId\":{\"name\":\"supplierId\",\"id\":12646,\"multiEntitySupport\":false," +
                "\"values\":{\"name\":\"Berkshire Hathaway - test 1\",\"id\":" + supplierId + "}}," +
                "\"configTimeZone\":{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + timeZone + ",\"name\":\"Asia/Kolkata (GMT +05:30)\",\"parentId\":null}}," +
                "\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":[{\"name\":\"Overdue\",\"id\":" + performanceStatus + "," +
                "\"parentId\":1,\"parentName\":\"Default\",\"group\":\"Default\"}]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Monthly (Date)\",\"id\":" + computationFrequency + ",\"parentId\":1005}]}}}}";

//        String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":true," +
//                "\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638," +
//                "\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
//                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false," +
//                "\"values\":\"" + name + "\"},\"parentEntityType\":{\"name\":\"parentEntityType\"," +
//                "\"values\":{\"name\":\"Destination Integration Config\",\"id\":" + entityTypeId + "},\"multiEntitySupport\":false}," +
//                "\"email\":{\"name\":\"email\",\"id\":" + email + ",\"multiEntitySupport\":false," +
//                "\"values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":1232}]}," +
//                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId + ",\"multiEntitySupport\":false}," +
//                "\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"multiEntitySupport\":false,\"values\":{" +
//                "\"id\":" + frequencyId + ",\"name\":\"Monthly\",\"parentId\":null}},\"canSupplierBeParent\":true," +
//                "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
//                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\"" + "\"}," +
//                "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"sourceIntegrationConfig\":" +
//                "{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"multiEntitySupport\":false," +
//                "\"values\":{\"id\":" + sourceId + ",\"name\":\"API AUTOMATION\",\"parentId\":null}}," +
//                "\"query\":{\"name\":\"query\",\"id\":12651,\"multiEntitySupport\":false,\"values\":\"test\"}," +
//                "\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false," +
//                "\"values\":[{\"name\":\"Sirion Performance Contracts ( CO01035 )\",\"id\":" + contractId + "}]}," +
//                "\"slaIds\":{\"name\":\"slaIds\",\"id\":" + 12648 +",\"multiEntitySupport\":false,\"values\":" +
//                "[{\"name\":\"QuantasSLITEM\",\"id\":" + slaId + ",\"parentId\":" + subCategory + ",\"parentName\":" +
//                "\"Quantasubcat2 | QuantasSL\",\"group\":\"Quantasubcat2 | QuantasSL\"}]}," +
//                "\"supplierId\":{\"name\":\"supplierId\",\"id\":12646,\"multiEntitySupport\":false," +
//                "\"values\":{\"name\":\"Berkshire Hathaway - test 1\",\"id\":" + supplierId + "}}," +
//                "\"configTimeZone\":{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{" +
//                "\"id\":" + timeZone + ",\"name\":\"Asia/Kolkata (GMT +05:30)\",\"parentId\":null}}," +
//                "\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
//                "\"multiEntitySupport\":false,\"values\":[{\"name\":\"Overdue\",\"id\":" + performanceStatus + "," +
//                "\"parentId\":1,\"parentName\":\"Default\",\"group\":\"Default\"}]}," +
//                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
//                "\"values\":[{\"name\":\"Monthly (Date)\",\"id\":" + computationFrequency + ",\"parentId\":1005}]}}}}";


        return payload;
    }

    private Boolean validateRawDataTabFromRawDataExcelDownloaded(int cslId,int destinationId,CustomAssert customAssert){

        Boolean validationStatus;

        String rawDateFilePath = "src\\test\\resources\\TestConfig\\SLIF";
        String excelFileName = "Raw Data.xlsx";

//        destinationDownload(destinationId,rawDateFilePath,excelFileName,customAssert);

        String sheetName = "Data QA";

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try {


            int startRow = 5;
            int excelNumberOfRows = XLSUtils.getNoOfRows(rawDateFilePath, excelFileName, sheetName).intValue();
            int rawDataLineItemSizeExcel = excelNumberOfRows-6;
            int endRowNumber = excelNumberOfRows - 2;
            int excelColumnNameColNumber = 4;


            validationStatus = serviceLevelHelper.validateRawDataTab(cslId, rawDataLineItemSizeExcel, rawDateFilePath, excelFileName, sheetName, startRow, endRowNumber, excelColumnNameColNumber, customAssert);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in function validateRawTabFromRawDataExcelDownloaded");
            validationStatus = false;
        }

        return validationStatus;
    }

    public int createDestination(int sourceId,int frequencyId,int slaId, int slaSubCategory,String computationFrequency ,CustomAssert customAssert){

        String apiUrl = "/slintegration/destination/create";
        int destinationId = -1;
        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            String payload = createPayloadForDestination1(sourceId,frequencyId,slaId,slaSubCategory,computationFrequency);

            APIResponse response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");

//                if(responseBody.contains("Validation Error")){
//                    String subjectLine = "Error in validating Source";
//
//                    List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp.get(0).get(0));
//                }
            }else {

                destinationId = getSourceId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully");
                }
            }



        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        return destinationId;

    }

    private int createSLACategory(CustomAssert customAssert){

        int sLACategory = -1;

        Check check = new Check();
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientSetupUserName");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientSetupUserPassword");

            check.hitCheckForClientSetup(adminUserName,adminPassword);

            Map<String,String> parameters = new HashMap<>();
            parameters.put("name","API AUTO " + RandomNumbers.getRandomNumberWithinRangeIndex(10,10000));
//            parameters.put("_csrf_token","7be133b7-70d5-4b33-bf32-2c2cedf84a3c");
            parameters.put("ajax","true");
            parameters.put("history","{}");

            String hostUrl = ConfigureEnvironment.getEnvironmentProperty("clientsetupurl");

            int responseCode = Masterslacategorys.postMasterSlaCategorys(hostUrl,parameters);

            if(responseCode != 302){
                customAssert.assertTrue(false,"Unable to create SLA form sirion admin");
            }else{

                String responseBody = MasterslacategorysList.getMasterSlaCategorysList(hostUrl);

                sLACategory = getSLAId(responseBody);

            }



        }catch (Exception e){

        }finally {

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);
        }

        return sLACategory;
    }

    private int createClientSLACategory(int slaId,CustomAssert customAssert){

        Check check = new Check();
        int clientSLACategory = -1;
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);

            Map<String,String> parameters = new HashMap<>();
            parameters.put("_method","");
            parameters.put("id",String.valueOf(slaId));
            parameters.put("name","Created By API Automation " + RandomNumbers.getRandomNumberWithinRangeIndex(10,1000));
            parameters.put("active","true");
            parameters.put("_active","on");
            parameters.put("_csrf_token","fd8b3a8d-8906-4f07-b9f1-0a4f1e63d684");
            parameters.put("history","{}");
            parameters.put("ajax","true");

            int responseCode = CreateClientSLACategory.postCreateClientSLACategory(parameters);

            if(responseCode != 302){
                customAssert.assertTrue(false,"Unable to create SLA form sirion admin");
            }else{

                String responseBody = MasterslacategorysList.getMasterSlaCategorysList();

                clientSLACategory = getSLAId(responseBody);

            }

        }catch (Exception e){

        }finally {


            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
            check.hitCheck(adminUserName,adminPassword);
        }
        return clientSLACategory;
    }

    private int createClientSLASubCategory(int clientSlaId,CustomAssert customAssert){

        int clientSLASubCategory = -1;
        Check check = new Check();
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);

            Map<String,String> parameters = new HashMap<>();
            parameters.put("_method","");
            parameters.put("category.name","Created By API Automation " + RandomNumbers.getRandomNumberWithinRangeIndex(10,10000));
            parameters.put("category.id",String.valueOf(clientSlaId));
            parameters.put("id","1050");
            parameters.put("name","Change/Release Management");
            parameters.put("active","true");
            parameters.put("_active","on");
//            parameters.put("_csrf_token","fd8b3a8d-8906-4f07-b9f1-0a4f1e63d684");
            parameters.put("history","{}");
            parameters.put("ajax","true");

            int responseCode = CreateClientSLASubCategory.postCreateClientSLASubCategory(parameters);

            if(responseCode != 302){
                customAssert.assertTrue(false,"Unable to create SLA form sirion admin");
            }else{

                String responseBody = SLASubCategories.getMasterSlaSubCategorysList(clientSlaId);

                clientSLASubCategory = getSLASubCategoryId(responseBody);

            }

        }catch (Exception e){

        }finally {


            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
            check.hitCheck(adminUserName,adminPassword);
        }

        return clientSLASubCategory;
    }

    private int createClientSLAItem(int subCategoryId,CustomAssert customAssert){

        int slaItem = -1;
        Check check = new Check();
        try{

            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");

            check.hitCheck(adminUserName,adminPassword);

            Map<String,String> parameters = new HashMap<>();
            parameters.put("_method","");
            parameters.put("subCategory.name","Automation");
            parameters.put("subCategory.id",String.valueOf(subCategoryId));
            parameters.put("id","1174");
            parameters.put("name","Automation " + RandomNumbers.getRandomNumberWithinRangeIndex(10,1000));
            parameters.put("active","true");
            parameters.put("_active","on");
            parameters.put("_csrf_token","fd8b3a8d-8906-4f07-b9f1-0a4f1e63d684");
            parameters.put("history","{}");
            parameters.put("ajax","true");

            int responseCode = CreateClientSLAItem.postCreateClientSLAItem(parameters);
//            int responseCode = 302;
            if(responseCode != 302){
                customAssert.assertTrue(false,"Unable to create SLA form sirion admin");
            }else{

                String responseBody = SLASubCategories.getMasterSlaSubCategorysListClient(subCategoryId);

                slaItem = getSLAItemId(responseBody);

            }

        }catch (Exception e){

        }finally {


            String adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
            String adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
            check.hitCheck(adminUserName,adminPassword);
        }

        return slaItem;
    }

    public static int getSLAId(String response) {

        int slaItem = -1;
        Document html = Jsoup.parse(response);
        Elements allElements = html.getElementById("_title_pl_com_sirionlabs_model_MasterSlacategory_id").child(1).children();

        int elementSize = allElements.get(1).getAllElements().get(0).childNodes().size();

        String id = "";
        TreeSet idSet = new TreeSet();
        TreeSet idSetDescending;
        String childElement;
        for(int i=0;i<elementSize;i++){

            try{
                childElement = allElements.get(1).getAllElements().get(0).childNodes().get(i).toString();
                Document html1 = Jsoup.parse(childElement);
                id = html1.getElementsByAttribute("href").toString().split("/masterslacategorys/show/")[1].split(">")[0];

                idSet.add(id);

            }catch (Exception e1){
                logger.error("Error while parsing HTML");
            }
        }

        idSetDescending = (TreeSet)idSet.descendingSet();
        String expectedId = String.valueOf(idSetDescending.first()).split("\"")[0];

        slaItem = Integer.parseInt(expectedId);
        return slaItem;
    }

    public static int getSLASubCategoryId(String response) {

        int slaItem = -1;
        Document html = Jsoup.parse(response);
        Elements allElements = html.getElementById("_title_pl_com_sirionlabs_model_MasterSlasubCategory_id").child(1).children();

        int elementSize = allElements.get(1).getAllElements().get(0).childNodes().size();

        String id = "";
        TreeSet idSet = new TreeSet();
        TreeSet idSetDescending;
        String childElement;
        for(int i=0;i<elementSize;i++){

            try{
                childElement = allElements.get(1).getAllElements().get(0).childNodes().get(i).toString();
                Document html1 = Jsoup.parse(childElement);
                id = html1.getElementsByAttribute("href").toString().split("/masterslasubcategorys/show/")[1].split(">")[0];

                idSet.add(id);

            }catch (Exception e1){
                logger.error("Error while parsing HTML");
            }
        }

        idSetDescending = (TreeSet)idSet.descendingSet();
        String expectedId = String.valueOf(idSetDescending.first()).split("\"")[0];

        slaItem = Integer.parseInt(expectedId);
        return slaItem;
    }

    public static int getSLAItemId(String response) {

        int slaItem = -1;
        Document html = Jsoup.parse(response);
        Elements allElements = html.getElementById("_title_pl_com_sirionlabs_model_MasterSlaitem_id").child(1).children();

        int elementSize = allElements.get(1).getAllElements().get(0).childNodes().size();

        String id = "";
        TreeSet idSet = new TreeSet();
        TreeSet idSetDescending;
        String childElement;
        for(int i=0;i<elementSize;i++){

            try{
                childElement = allElements.get(1).getAllElements().get(0).childNodes().get(i).toString();
                Document html1 = Jsoup.parse(childElement);
                id = html1.getElementsByAttribute("href").toString().split("/masterslaitems/show/")[1].split(">")[0];

                idSet.add(id);

            }catch (Exception e1){
                logger.error("Error while parsing HTML");
            }
        }

        idSetDescending = (TreeSet)idSet.descendingSet();
        String expectedId = String.valueOf(idSetDescending.first()).split("\"")[0];

        slaItem = Integer.parseInt(expectedId);
        return slaItem;
    }

    private void addCSLToDelete(ArrayList<String> cslToDeleteList){

        try {
            for (String cslIDToDelete : cslToDeleteList) {
                cslToDelete.add(Integer.parseInt(cslIDToDelete));
            }
        }catch (Exception e){
            logger.error("Error while adding child service level to deleted list " + e.getMessage());
        }
    }

    private int getDailyCSL(ArrayList<String> childServiceLevelIdList){

        int dailyCSL = -1;
        Show show = new Show();
        String showResponse;


        String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();

        int currentMonth = Integer.parseInt(currentDate.split("-")[0]);
        int currentYear =  Integer.parseInt(currentDate.split("-")[2]);

        String dueDate;
        String convertDate;
        int serviceStartMonth;
        int serviceStartYear;
        try {
            for (String childServiceLevel : childServiceLevelIdList) {
                show.hitShowVersion2(cslEntityTypeId,Integer.parseInt(childServiceLevel));
                showResponse = show.getShowJsonStr();

                JSONObject showResponseJson = new JSONObject(showResponse);
                dueDate = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dueDate").get("displayValues").toString();

                serviceStartMonth = Integer.parseInt(dueDate.split("-")[1]);
                serviceStartYear = Integer.parseInt(dueDate.split("-")[0]);

                if((serviceStartMonth == currentMonth) && (serviceStartYear == currentYear)){
                    dailyCSL = Integer.parseInt(childServiceLevel);
                    break;
                }

            }
        }catch (Exception e){
            logger.error("Exception while getting last month CSL");
        }

        return dailyCSL;
    }

    private int getLastMonthCSL(ArrayList<String> childServiceLevelIdList){

        int lastMonthCSL = -1;
        Show show = new Show();
        String showResponse;


        String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();

        int currentMonth = Integer.parseInt(currentDate.split("-")[0]);
        int currentYear =  Integer.parseInt(currentDate.split("-")[2]);

        if(currentMonth == 1){
            currentMonth = 12;
            currentYear = currentYear - 1;
        }else {
            currentMonth = currentMonth -1;
        }

//        if()
        String dueDate;
        String convertDate;
        int serviceStartMonth;
        int serviceStartYear;
        try {
            for (String childServiceLevel : childServiceLevelIdList) {
                show.hitShowVersion2(cslEntityTypeId,Integer.parseInt(childServiceLevel));
                showResponse = show.getShowJsonStr();

                JSONObject showResponseJson = new JSONObject(showResponse);
                dueDate = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dueDate").get("displayValues").toString();

//                SimpleDateFormat newDate=new SimpleDateFormat("MM-dd-yyyy");
//                convertDate=new SimpleDateFormat("MM-DD-YYYY").format(dueDate);
//                 convertDate = newDate.format(dueDate);
//                String strDate=


                serviceStartMonth = Integer.parseInt(dueDate.split("-")[1]);
                serviceStartYear = Integer.parseInt(dueDate.split("-")[0]);

                if((serviceStartMonth == currentMonth) && (serviceStartYear == currentYear)){
                    lastMonthCSL = Integer.parseInt(childServiceLevel);
                    break;
                }

            }
        }catch (Exception e){
            logger.error("Exception while getting last month CSL");
        }

        return lastMonthCSL;
    }

    private ArrayList<Integer> getQuarterlyMonthCSL(ArrayList<String> childServiceLevelIdList) {
        ArrayList<Integer> quarterlyMonthCSL=new ArrayList<>();
        Show show = new Show();
        String showResponse;

        String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();

        int currentMonth = Integer.parseInt(currentDate.split("-")[0]);
        int currentYear =  Integer.parseInt(currentDate.split("-")[2]);

        List<String> quarterMonth = expectedPreviousQuarterMonths(currentMonth);
        int quarterFirstMonth = Integer.parseInt(quarterMonth.get(0));
        int quarterLastMonth =  Integer.parseInt(quarterMonth.get(2));

        String dueDate;
        String convertDate;
        int serviceStartMonth;

        int serviceStartYear;
        try {
            for (String childServiceLevel : childServiceLevelIdList) {
                show.hitShowVersion2(cslEntityTypeId,Integer.parseInt(childServiceLevel));
                showResponse = show.getShowJsonStr();

                JSONObject showResponseJson = new JSONObject(showResponse);
                dueDate = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dueDate").get("displayValues").toString();

//                SimpleDateFormat newDate=new SimpleDateFormat("MM-dd-yyyy");
//                convertDate=new SimpleDateFormat("MM-DD-YYYY").format(dueDate);
//                convertDate = newDate.format(dueDate);
//                String strDate=

                serviceStartMonth = Integer.parseInt(dueDate.split("-")[1]);

                serviceStartYear = Integer.parseInt(dueDate.split("-")[2]);

                if((serviceStartMonth>= quarterFirstMonth) && (serviceStartMonth <=quarterLastMonth) && (currentYear==serviceStartYear)){
                    quarterlyMonthCSL.add(Integer.parseInt(childServiceLevel));
//                    break;
                }

            }
        }catch (Exception e){
            logger.error("Exception while getting quarterly month CSL");
        }

        return quarterlyMonthCSL;
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

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

//        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
//                + "order by id desc";
        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '%" + subjectLine + "%'";
        List<List<String>> queryResult = null;
        PostgreSQLJDBC postgreSQLJDBC;
        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassword);

        try {
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);

        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return queryResult;

    }

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC;
//
        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassword);
        try {


            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
    }

    private Boolean editSLAItems(int slId,int slCategory,int subCategory,int SLAItem,CustomAssert customAssert){

        Boolean editStatus = true;
        Edit edit = new Edit();
        String editResponse;
        try {


            editResponse = edit.hitEdit(slEntity,slId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("category").getJSONObject("values").put("id",slCategory);
            if(editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("subCategory").has("values")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("subCategory").getJSONObject("values").put("id",subCategory);
            }else {
                JSONObject valuesJSon = new JSONObject();
                valuesJSon.put("id",subCategory);
                valuesJSon.put("parentId",slCategory);
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("subCategory").put("values",valuesJSon);


            }

            if(editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slaItem").has("values")){
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slaItem").getJSONObject("values").put("id",subCategory);
            }else {
                JSONObject valuesJSon = new JSONObject();
                valuesJSon.put("id",SLAItem);
                valuesJSon.put("parentId",subCategory);
                editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slaItem").put("values",valuesJSon);
            }

            editResponse = edit.hitEdit(slEntity,editResponseJson.toString());

            if(!editResponse.contains("success")){

                customAssert.assertTrue(false,"Edit done unsuccessfully ");
                editStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while Editing SLA Items  " + e.getMessage());
            editStatus = false;
        }


        return editStatus;
    }

    public int createSourceForDestination(String url,String userName,String password){

        CustomAssert customAssert = new CustomAssert();

        int sourceId = -1;
        String newApiUrl = "/slintegration/source/new";

        String apiUrl = "/slintegration/source/create";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(newApiUrl,headers).getResponse();
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("New API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }


            String payload= createPayloadForSource(url, userName, password);

            response = executor.post(apiUrl,headers,payload.toString()).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }

            sourceId =  getSourceId(responseBody,"source","source");

            if(sourceId == -1){
                customAssert.assertTrue(false,"Source ID not created successfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        return sourceId;

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

    private String getEpochTime(long afterSeconds){

        Long epochTime = System.currentTimeMillis() + afterSeconds;

        return epochTime.toString();
    }

    private Boolean updateTriggerTimeInSLIFQartzDB(String epochTime,int destinationId){

        Boolean updateDb = true;
        try {
            String dbName = "slif_quartz";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, dbName, dbUserName, dbPassword);

            postgreSQLJDBC.updateDBEntry("update slif_qrtz_triggers set next_fire_time='" + epochTime + "' where slif_qrtz_triggers.trigger_name='" + destinationId + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private Boolean sourceIsNotAuthenticated(int sourceId){
        Boolean updateDb=true;
//        String pass="abcd";
        List<List<String>> results;
        int rest_id;
        try{
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "slif";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            results = postgreSQLJDBC.doSelect("Select rest_endpoint_data_id from source_integration_config where id='" +sourceId+ " ';");
            rest_id = Integer.parseInt(results.get(0).get(0));
            postgreSQLJDBC.updateDBEntry("update rest_endpoint_data set url='https://dev68846.service-now.com/api/now/table/incident' where id='" + rest_id + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;

    }

    private List<List<String>> getRecordFromSystemEmailTableFailure(String subjectLine, String currentTimeStamp){

//        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
//                + "order by id desc";
        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '%" + subjectLine + "%'";
        List<List<String>> queryResult = null;

        String dbHostAddress = "192.168.2.157";
        String dbPortName = "5432";
        String dbName = "letterbox";
        String dbUserName = "postgres";
        String dbPassword = "postgres";

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
        try{
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);
        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }
        return queryResult;
    }

    private Boolean downloadCSLDataFromDestination(String computationFrequency,int frequency,int supplierId,int slaId,int contractId,String performanceStatus, String filePath, String fileName,CustomAssert customAssert){

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

    private Map<String, String> getFormParamDownloadList(String computationFrequency,int frequency,int supplierId,int slaId,int contractId,String performanceStatus) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;
        String csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");

        jsonData = "{\"filterMap\":{\"filterJson\":{},\"supplierId\":"+supplierId+",\"contractIds\":["+contractId+"],\"slaIds\":["+slaId+"],\"computationFrequency\":["+computationFrequency+"],\"frequency\":"+frequency+",\"performanceStatus\":["+performanceStatus+"]}}\n";

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private List<String> expectedPreviousMonths(int month){

        List<String> previousMonths = new ArrayList<>();

        if(month == 1){
            previousMonths = Arrays.asList("12");

        } else if(month > 1 && month < 13){
            String previousMonth = String.valueOf(month -1);
            previousMonths = Arrays.asList(previousMonth);
        }

        return previousMonths;

    }

    private String createPayloadForSource1(){
        String payload;

        String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","name");
        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","active");
        String email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email");
        String toolId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","tool");
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","url");
        String columns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","columns");
        String newDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","new data criteria");
        String uniqueDataCriteria = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","unique data criteria");
        String authenticationType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","authentication type");
        String userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","username");
        String password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","password");


        payload = "{\"body\":{\"data\":{\"password\":{\"name\":\"" +
                "password\",\"id\":12637,\"multiEntitySupport\":false,\"values\":\"" + password + "\"}," +
                "\"active\":{\"name\":\"active\",\"id\":12625,\"values\":" +active+ ",\"multiEntitySupport\":false}," +
                "\"name\":{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"restEndPointSchema\":{\"name\":\"restEndPointSchema\",\"id\":12629,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" +toolId + ",\"clientId\":null,\"name\":\"Service Now\",\"sourceType\":null,\"jsonSchema\":null}}," +
                "\"description\":{\"name\":\"description\",\"id\":12624,\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Source Integration Config\",\"id\":338},\"multiEntitySupport\":false}," +
                "\"restEndPoint\":{\"name\":\"restEndPoint\",\"multiEntitySupport\":false,\"values\":" +
                "{\"newDataCriteria\":\"" + newDataCriteria + "\",\"uniqueDataCriteria\":\"" + uniqueDataCriteria + "\"," +
                "\"url\":\"" + url + "\",\"columns\":\"" + columns + "\"}}," +
                "\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false,\"" +
                "values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":" + email + "}]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":338,\"multiEntitySupport\":false}," +
                "\"authenticationType\":{\"name\":\"authenticationType\",\"id\":12635,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + authenticationType + ",\"name\":\"Basic Authentication\",\"parentId\":null}}," +
                "\"user\":{\"name\":\"user\",\"multiEntitySupport\":false,\"values\":\"" + userName + "\"}}}}";

        return payload;
    }

    private String createPayloadForDestination1(int sourceId, int frequencyId,int slaId,int subCategory,String computationFrequency){

        String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","name");
        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","active");
        String email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","email");
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

        String timeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","time zone");
//        computationFrequency = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","computation frequency"));
        String datePattern = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","date pattern");
        int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract"));
        String query = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","query");
        String performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"Destination config details","performance status");

        if(name.isEmpty() && name.length()>200){
            logger.error("Name field can't be empty and can't be greater than 200 characters.");
        }

        String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":true," +
                "\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638," +
                "\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false," +
                "\"values\":\"" + name + "\"},\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Destination Integration Config\",\"id\":" + entityTypeId + "},\"multiEntitySupport\":false}," +
                "\"email\":{\"name\":\"email\",\"id\":" + email + ",\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":1232}]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId + ",\"multiEntitySupport\":false}," +
                "\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + frequencyId + ",\"name\":\"Monthly\",\"parentId\":null}},\"canSupplierBeParent\":true," +
                "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\"" + datePattern + "\"}," +
                "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"sourceIntegrationConfig\":" +
                "{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + sourceId + ",\"name\":\"API AUTOMATION\",\"parentId\":null}}," +
                "\"query\":{\"name\":\"query\",\"id\":12651,\"multiEntitySupport\":false,\"values\":\"test\"}," +
                "\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Sirion Performance Contracts ( CO01035 )\",\"id\":" + contractId + "}]}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":" + 12648 +",\"multiEntitySupport\":false,\"values\":" +
                "[{\"name\":\"QuantasSLITEM\",\"id\":" + slaId + ",\"parentId\":" + subCategory + ",\"parentName\":" +
                "\"Quantasubcat2 | QuantasSL\",\"group\":\"Quantasubcat2 | QuantasSL\"}]}," +
                "\"supplierId\":{\"name\":\"supplierId\",\"id\":12646,\"multiEntitySupport\":false," +
                "\"values\":{\"name\":\"Berkshire Hathaway - test 1\",\"id\":" + supplierId + "}}," +
                "\"configTimeZone\":{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + timeZone + ",\"name\":\"Asia/Kolkata (GMT +05:30)\",\"parentId\":null}}," +
                "\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":[{\"name\":\"Overdue\",\"id\":" + performanceStatus + "," +
                "\"parentId\":1,\"parentName\":\"Default\",\"group\":\"Default\"}]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Monthly (Date)\",\"id\":" + computationFrequency + ",\"parentId\":1005}]}}}}";

        return payload;
    }

    private String createPayloadForDestinationDaily(int sourceId, int frequencyId,int slaId,int subCategory,String computationFrequency){

        String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","name");
        String description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","description");
        String active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","active");
        String email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","email");
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","supplier"));

        String timeZone = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","time zone");

        int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","contract"));
        String query = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"destination config details","query");
        String performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"Destination config details","performance status");

        String payload = "{\"body\":{\"data\":{\"active\":{\"name\":\"active\",\"id\":12640,\"values\":true," +active+
                "\"multiEntitySupport\":false},\"name\":{\"name\":\"name\",\"id\":12638," +
                "\"multiEntitySupport\":false,\"values\":\"" + name + "\"}," +
                "\"description\":{\"name\":\"description\",\"id\":12639,\"multiEntitySupport\":false," +
                "\"values\":\"" + name + "\"},\"parentEntityType\":{\"name\":\"parentEntityType\"," +
                "\"values\":{\"name\":\"Destination Integration Config\",\"id\":" + entityTypeId + "},\"multiEntitySupport\":false}," +
                "\"email\":{\"name\":\"email\",\"id\":" + email + ",\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"bhadani gaurav (bhadani@sirionqa.office)\",\"id\":1232}]}," +
                "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId + ",\"multiEntitySupport\":false}," +
                "\"frequency\":{\"name\":\"frequency\",\"id\":12649,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + frequencyId + ",\"name\":\"Monthly\",\"parentId\":null}},\"canSupplierBeParent\":true," +
                "\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false}," +
                "\"datePattern\":{\"name\":\"datePattern\",\"id\":12650,\"multiEntitySupport\":false,\"values\":\"\"}," +
                "\"initiatives\":{\"name\":\"initiatives\",\"multiEntitySupport\":false},\"sourceIntegrationConfig\":" +
                "{\"name\":\"sourceIntegrationConfig\",\"id\":12641,\"multiEntitySupport\":false," +
                "\"values\":{\"id\":" + sourceId + ",\"name\":\"API AUTOMATION\",\"parentId\":null}}," +
                "\"query\":{\"name\":\"query\",\"id\":12651,\"multiEntitySupport\":false,\"values\":\"test\"}," +
                "\"contractIds\":{\"name\":\"contractIds\",\"id\":12647,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Sirion Performance Contracts ( CO01035 )\",\"id\":" + contractId + "}]}," +
                "\"slaIds\":{\"name\":\"slaIds\",\"id\":" + 12648 +",\"multiEntitySupport\":false,\"values\":" +
                "[{\"name\":\"QuantasSLITEM\",\"id\":" + slaId + ",\"parentId\":" + subCategory + ",\"parentName\":" +
                "\"Quantasubcat2 | QuantasSL\",\"group\":\"Quantasubcat2 | QuantasSL\"}]}," +
                "\"supplierId\":{\"name\":\"supplierId\",\"id\":12646,\"multiEntitySupport\":false," +
                "\"values\":{\"name\":\"Berkshire Hathaway - test 1\",\"id\":" + supplierId + "}}," +
                "\"configTimeZone\":{\"name\":\"configTimeZone\",\"id\":12652,\"multiEntitySupport\":false,\"values\":{" +
                "\"id\":" + timeZone + ",\"name\":\"Asia/Kolkata (GMT +05:30)\",\"parentId\":null}}," +
                "\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":12671," +
                "\"multiEntitySupport\":false,\"values\":[{\"name\":\"Overdue\",\"id\":" + performanceStatus + "," +
                "\"parentId\":1,\"parentName\":\"Default\",\"group\":\"Default\"}]}," +
                "\"computationFrequency\":{\"name\":\"computationFrequency\",\"id\":12661,\"multiEntitySupport\":false," +
                "\"values\":[{\"name\":\"Monthly (Date)\",\"id\":" + computationFrequency + ",\"parentId\":1005}]}}}}";

        return payload;
    }

    private Boolean deleteDest(int rest_id){
        Boolean updateDb=true;
        String dbHostAddress = "192.168.2.157";
        String dbPortName = "5432";
        String dbName = "slif_quartz";

        String dbUserName = "postgres";
        String dbPassword = "postgres";
        try{
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.deleteDBEntry("Delete from slif_qrtz_cron_triggers where trigger_name='" +rest_id+ "';");
            postgreSQLJDBC.deleteDBEntry("Delete from slif_qrtz_triggers where trigger_name='" +rest_id+ "';");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private List<String> getCSLDataFromListingPage(int contract, String performanceStatus, int slaItem, String computationFrequency, String startDate, String endDate,ArrayList<Integer> cslDBIdList){

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
            JSONArray dataArray = new JSONArray();
            dataArray = listDataResponseJSON.getJSONArray("data");
            JSONObject indRowDataJson;
            for(int i=0;i < dataArray.length();i++)
            {
                indRowDataJson = dataArray.getJSONObject(i);
                Iterator<String> keys = indRowDataJson.keys();
                String columnName;
                String columnValue;
                while(keys.hasNext()) {
                    String key = keys.next();
                    columnName = indRowDataJson.getJSONObject(key).get("columnName").toString();
                    if(columnName.equals("id")){
                        columnValue = indRowDataJson.getJSONObject(key).get("value").toString();
                        cslDBIdList.add(Integer.parseInt(columnValue.split(":;")[0]));
                        columnValue = "CSL"+columnValue.split(":;")[1];
                        cslList.add(columnValue);
                    }
                }
            }
//           jsonObject.getString(response);
        }catch(Exception e){}
        finally {
            check.hitCheck(adminUserName,adminPassword);
        }
        return cslList;
    }

    private String createPayloadForCSLListingPage(int contract, String performanceStatus, int slaItem, String computationFrequency, String startDate, String endDate){
        String payload=null;

        try {
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":2000,\"orderByColumnName\":" +
                    "\"id\",\"orderDirection\":\"asc nulls last\",\"filterJson\":" +
                    "{\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[" +
                    "{\"id\":\""+contract+"\",\"name\":\"Contract Flow Down Test\"}]},\"filterId\":2," +
                    "\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"24\":{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null," +
                    "\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null,\"start\":\""+startDate+"\",\"end\":\""+endDate+"\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"26\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+performanceStatus+"\",\"name\":\"Overdue\"}]}," +
                    "\"filterId\":26,\"filterName\":\"performanceStatus\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"29\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+computationFrequency+"\",\"name\":\"Monthly (Date)\"}]}," +
                    "\"filterId\":29,\"filterName\":\"frequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\""+slaItem+"\",\"name\":\"Automation 201\"}]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null," +"\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }catch(Exception e){
            logger.debug("Exception while creating payload");
        }

        return payload;
    }

    private List<String> getCSLDataFromExcelSheet(String filePath, String fileName){
        List<String> cSLList = new ArrayList<>();
        try{
            String sheetName = "Data QA";
            int columnNo = 0;
            int startingRowNo = 4;
            int getRows = XLSUtils.getNoOfRows(filePath,fileName,sheetName).intValue();
            int noOfRows = getRows-6;

            cSLList = XLSUtils.getOneColumnDataFromMultipleRows(filePath,fileName,sheetName,columnNo,startingRowNo,noOfRows);
        }catch(Exception e){}
        return cSLList;
    }

    private Boolean compareExcelAndListingData(List<String> excelData, List<String> listingData ,CustomAssert customAssert){
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
                        break;
                    }
                }
            }
            else
            {
                comparisonStatus = false;
            }
        }catch(Exception e){}
        return comparisonStatus;
    }

    private List<String> getRawDataFromDestination(String filePath,String fileName){
        List<String> rawData = new ArrayList<>();
        try{
            String sheetName = "Data QA";
            int columnNo = 0;
            int startingRowNo = 4;
            int noOfRows = XLSUtils.getNoOfRows(filePath,fileName,sheetName).intValue()-2;

            rawData = XLSUtils.getOneColumnDataFromMultipleRows(filePath,fileName,sheetName,columnNo,startingRowNo,noOfRows);
        }catch(Exception e){

        }
        return rawData;
    }

    private List<String> getLastMonthDueDate() {

        List<String> dates = new ArrayList<>();
        String endDate=null;
        int end=0;
        String startDate = null;
        String currentDateDb;
        try {
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "SL-Automation";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select now();");
            postgreSQLJDBC.closeConnection();

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

            if(currentMonth <=9)
                currentMonthString = 0+String.valueOf(currentMonth);

//            startDate = currentYear+ "-" +currentMonthString+ "-" +"01";
            startDate =currentMonthString+"-"+"01"+"-"+currentYear;
            end = getEndDate(currentMonth,currentYear);
//            endDate = currentYear+"-"+currentMonthString+"-"+end;
            endDate =currentMonthString+"-"+end+"-"+currentYear;
            dates.add(startDate);
            dates.add(endDate);
        } catch (Exception e) {

        }
        return dates;
    }

    private String getPreviousDate() {

        String previousDate = null;
        try {
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "SL-Automation";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
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

            System.out.println();

        } catch (Exception e) {

        }
        return previousDate;
    }

    private List<String> getDailyMonthDueDate() {
        List<String> dates = new ArrayList<>();
        String endDate;
        String startDate;
        int end =0;
        String currentDateDb;
        try {
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "SL-Automation";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
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

    private List<String> getQuarterlyMonthDueDate() {
        List<String> dates = new ArrayList<>();
        String endDate=null;
        String startDate = null;
        int end = 0;
        String currentDateDb;
        try {
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "SL-Automation";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("Select now();");
            postgreSQLJDBC.closeConnection();
            currentDateDb = dbCurrentDate.toString();
            String quarterFirstMonthString = "";
            String quarterLastMonthString = "";
            int currentMonth = Integer.parseInt(currentDateDb.substring(7,9));
            int currentYear =  Integer.parseInt(currentDateDb.substring(2,6));

            List<String> quarterMonth = expectedPreviousQuarterMonths(currentMonth);
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

    private int getEndDate(int month,int year){
        int endDate=0;
        if(month == 1 || month ==3 || month ==5 || month == 7 || month == 8|| month ==10)
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

    private List<String> getListingData(int supplierId,List<String> contractId,String startDate,String endDate,int slaId,int frequency){

        String payload = null;
        if(frequency == 6){
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"Berkshire Hathaway - test 1\"}]}," +
                    "\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId.get(0) + "\",\"name\":\"Independent Contractor Agreement - Berkshire Hathaway\"}]}," +
                    "\"filterId\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"24\":{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"dayOffset\":null,\"duration\":null," +
                    "\"start\":\"" + startDate + "\",\"end\":\"" + endDate + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"29\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1006\",\"name\":\"Quarterly\"}," +
                    "{\"id\":\"1003\",\"name\":\"Monthly (Date)\"},{\"id\":\"1004\",\"name\":\"Monthly (Day)\"}," +
                    "{\"id\":\"1013\",\"name\":\"Monthly first nth working day\"}," +
                    "{\"id\":\"1012\",\"name\":\"Monthly last nth working day\"}]}," +
                    "\"filterId\":29,\"filterName\":\"frequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + slaId + "\",\"name\":\"Abandoned ; Call Rate (Europe)\"}]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"400\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + frequency + "\",\"name\":\"Quarterly\"}]}," +
                    "\"filterId\":400,\"filterName\":\"datafrequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }else {
            payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"Berkshire Hathaway - test 1\"}]}," +
                    "\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"2\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractId.get(0) + "\",\"name\":\"Independent Contractor Agreement - Berkshire Hathaway\"}," +
                    "{\"id\":\"" + contractId.get(1) + "\",\"name\":\"Sirion Performance Contracts\"}]},\"filterId" +
                    "\":2,\"filterName\":\"contract\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"24\":" +
                    "{\"filterId\":\"24\",\"filterName\":\"dueDate\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"dayOffset\":null,\"duration\":null,\"start\":\"" + startDate + "\",\"end\":\"" + endDate + "\"," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}," +
                    "\"124\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + slaId + "\",\"name\":\"Abandoned ; Call Rate (Europe)\"}]}," +
                    "\"filterId\":124,\"filterName\":\"slItem\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}," +
                    "\"400\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + frequency + "\",\"name\":\"Monthly\"}]}," +
                    "\"filterId\":400,\"filterName\":\"datafrequency\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":12148,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":12142,\"columnQueryName\":\"id\"}]}";
        }

        ListRendererListData listRendererListData = new ListRendererListData();
        listRendererListData.hitListRendererListData(265,payload);
        String listingResponse = listRendererListData.getListDataJsonStr();

        JSONObject listingResponseJson = new JSONObject(listingResponse);

        JSONArray dataArray = listingResponseJson.getJSONArray("data");
        JSONObject indData;
        JSONArray indDataArray;
        String columnName;
        String columnValue;

        List<String> cslIdList = new ArrayList<>();
        for(int i =0;i < dataArray.length();i++ ){

            indData = dataArray.getJSONObject(i);

            indDataArray = JSONUtility.convertJsonOnjectToJsonArray(indData);

            innerLoop:
            for(int j=0;j<indDataArray.length();j++){

                columnName = indDataArray.getJSONObject(j).get("columnName").toString();

                if(columnName.equals("id")){
                    columnValue = indDataArray.getJSONObject(j).get("value").toString().split(":;")[0];
                    cslIdList.add("CSL" + columnValue);
                    break innerLoop;
                }

            }
        }

        return cslIdList;
    }

    private boolean destinationDelete(int destinationId){

        Boolean destinationDelete=true;

        String dbHostAddress = "192.168.2.157";
        String dbPortName = "5432";
        String dbName = "slif";

        String dbUserName = "postgres";
        String dbPassword = "postgres";
        try{
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
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

    private List<Map<String,String>> getServiceNowData(String serviceNowUrl,String tableName,String newDataCriteria,
                                                       String startDate,String endDate,String systemParamFields,CustomAssert customAssert) {

        List<Map<String,String>> serviceNowData = new ArrayList<>();

        int portNumber = 443;
        String protocolScheme = "https";

        String jS = "javascript:gs.dateGenerate(%27";
        String operator = "BETWEEN";

        String apiUrl = "/api/now/table/" + tableName + "?sysparm_query=" + newDataCriteria +
                operator + jS + startDate + "%27)@" + jS + endDate + "%27)&sysparm_fields=" + systemParamFields + "&sysparm_limit=2000";

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

    private Boolean compareTestDataWithIncFetchedFromSnow(List<Map<String,String>> serviceNowData,
                                                          String excelFilePath,String excelFileName,CustomAssert customAssert){

        Boolean compareStatus = true;
        try{

            String sheetName = "Data QA";
            int rawDataHeaderRowNum = 4;

            int startingRow = 5;
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

                for (Map.Entry<String,String> entry : serviceNowDataValues.entrySet()){

                    serviceNowColumnName = entry.getKey();
                    serviceNowColumnValue = entry.getValue();
                    serviceNowColumnValue = getValueFromDictionary(serviceNowColumnName,serviceNowColumnValue);
                    int columnNum =  headerDataColumnNum.get(serviceNowColumnName);

                    String excelColumnValue = excelData.get(i).get(columnNum);

                    if(!serviceNowColumnValue.equals(excelColumnValue)){
                        compareStatus = false;
                    }

                }
            }

            if(!compareStatus){
                customAssert.assertTrue(false,"TestData Download comparison failed with the Data Fetched From Service Now");
            }

        }catch (Exception e){

            customAssert.assertTrue(false,"Exception while comparing TestData Downloaded With Inc Fetched From Service Now");
            compareStatus = false;
        }

        return compareStatus;
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

    private int createDestination(String payload,CustomAssert customAssert){

        int destinationId = -1;
        String apiUrl = "/slintegration/destination/create";

        try{
            Map<String,String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.post(apiUrl,headers,payload).getResponse();

            if(response.getResponseCode() !=200){
                logger.error("API Response Code is not equal to 200");
                customAssert.assertTrue(false,"API Response Code is not equal to 200");
            }

            String responseBody = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false,"API Response is not a valid Json");
            }else {

                destinationId = getSourceId(responseBody, "destination", "destination");

                if (destinationId == -1) {
                    customAssert.assertTrue(false, "Destination ID not created successfully");
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating API " + e.getMessage());
        }

        return destinationId;
    }

    private String editSource(int sourceId){

        String apiUrl = "/slintegration/source/edit";
        String editUrlGet = apiUrl + "/" + sourceId;

        String responseBody = null;
        JSONObject editResponseJson = new JSONObject();

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(editUrlGet, headers).getResponse();

            if (response.getResponseCode() != 200) {
                logger.error("API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            editResponseJson = new JSONObject(responseBody);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

        }catch (Exception e){
        }
        return editResponseJson.toString();
    }

    private String editSource(String payload){

        String apiUrl = "/slintegration/source/edit";

        try{

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.post(apiUrl, headers,payload).getResponse();

            return  response.getResponseBody();


        }catch (Exception e){

        }

        return "";
    }

    private String editDestination(int destinationId){

        String apiUrl = "/slintegration/destination/edit";
        String editUrlGet = apiUrl + "/" + destinationId;

        String responseBody = null;
        JSONObject editResponseJson = new JSONObject();

        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(editUrlGet, headers).getResponse();

            if (response.getResponseCode() != 200) {
                logger.error("API Response Code is not equal to 200");
            }

            responseBody = response.getResponseBody();

            editResponseJson = new JSONObject(responseBody);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

        }catch (Exception e){
        }
        return editResponseJson.toString();
    }

    private String editDestination(String payload){

        String apiUrl = "/slintegration/destination/edit";

        try{

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.post(apiUrl, headers,payload).getResponse();

            return  response.getResponseBody();


        }catch (Exception e){

        }

        return "";
    }

    private String checkErrorMsgOnTestData(int destinationId){

        String apiUrl = "/slintegration/destination/download/";
        String editUrl = apiUrl +"/" +destinationId;
        String responseBody;
        JSONObject editResponseJson;
        String msg = null;

        try{
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type","application/json");

            APIResponse response = executor.get(editUrl,headers).getResponse();

            if(response.getResponseCode()!=200){
                logger.error("API response code is not equal to 200.");
            }
            responseBody = response.getResponseBody();
            if(responseBody==null){
                logger.error("Response Body is null.");
            }
            editResponseJson = new JSONObject(responseBody);

            msg = editResponseJson.getString("message");

        }catch (Exception e){
            logger.error("Exception while validating Test Data");
        }
        return msg;
    }

    private Boolean sourceUpdateInvalidURLInDB(int sourceId){
        Boolean updateDb=true;
        List<List<String>> results;
        int rest_id;
        try{
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "slif";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            results = postgreSQLJDBC.doSelect("Select rest_endpoint_data_id from source_integration_config where id='" +sourceId+ " ';");
            rest_id = Integer.parseInt(results.get(0).get(0));
            postgreSQLJDBC.updateDBEntry("update rest_endpoint_data set url='https://dev68846.service-now.com/api/now/table/incident' where id='" + rest_id + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;

    }

    private Boolean sourceUpdateInvalidUniqueDataCriteriaInDB(int sourceId){
        Boolean updateDb=true;
        List<List<String>> results;
        int rest_id;
        try{

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, slifDBName, dbUserName, dbPassword);
            results = postgreSQLJDBC.doSelect("Select rest_endpoint_data_id from source_integration_config where id='" +sourceId+ " ';");
            rest_id = Integer.parseInt(results.get(0).get(0));
            postgreSQLJDBC.updateDBEntry("update rest_endpoint_data set unique_data_criteria='number1' where id='" + rest_id + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private Boolean sourceUpdateInvalidNewDataCriteriaInDB(int sourceId){
        Boolean updateDb=true;
        List<List<String>> results;
        int rest_id;
        try{

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, slifDBName, dbUserName, dbPassword);
            results = postgreSQLJDBC.doSelect("Select rest_endpoint_data_id from source_integration_config where id='" +sourceId+ " ';");
            rest_id = Integer.parseInt(results.get(0).get(0));
            postgreSQLJDBC.updateDBEntry("update rest_endpoint_data set new_data_criteria='open_at' where id='" + rest_id + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private Boolean sourceUpdateInvalidColumnsInDB(int sourceId){
        Boolean updateDb=true;
        List<List<String>> results;
        int rest_id;
        try{

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, slifDBName, dbUserName, dbPassword);
            results = postgreSQLJDBC.doSelect("Select rest_endpoint_data_id from source_integration_config where id='" +sourceId+ " ';");
            rest_id = Integer.parseInt(results.get(0).get(0));
            postgreSQLJDBC.updateDBEntry("update rest_endpoint_data set columns='number1,state1,priority1' where id='" + rest_id + "';");
            postgreSQLJDBC.closeConnection();


        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private Boolean sourceUpdateInvalidCredentialsInDB(int sourceId){
        Boolean updateDb=true;
        try{

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, slifDBName, dbUserName, dbPassword);
            postgreSQLJDBC.updateDBEntry("update source_integration_config set user_name ='admin123' where id='" + sourceId + "';");
            postgreSQLJDBC.closeConnection();
        }catch (Exception e){
            updateDb = false;
        }
        return updateDb;
    }

    private void resetSource(int sourceId,CustomAssert customAssert){

        String url = "https://dev68841.service-now.com/api/now/table/incident";
        String newDataCriteria = "opened_at";
        String uniqueDataCriteria = "number";
        String columns = "number,state,priority";

        try{
            String editResponse = editSource(sourceId);

            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("active").put("values",true);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("url",url);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("uniqueDataCriteria",uniqueDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("newDataCriteria",newDataCriteria);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("restEndPoint").getJSONObject("values").put("columns",columns);

            editResponse = editSource(editResponseJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Error while updating source");
            }

        }catch (Exception e){
            logger.error("Exception while updating Source");
            customAssert.assertTrue(false,"Exception while updating Source");
        }

    }

    private Boolean resetSourceCredentials(int sourceId){
        Boolean updateDbStatus = true;
        try{

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName, dbPortName, dbName, dbUserName, dbPassword);
            postgreSQLJDBC.updateDBEntry("update source_integration_config set user_name ='admin' where id='" + sourceId + "';");
            postgreSQLJDBC.closeConnection();

        }catch (Exception e){
            updateDbStatus = false;
        }
        return updateDbStatus;
    }

    private boolean deleteDataAtIndex(ArrayList<Integer> childSlLList,CustomAssert customAssert){

        Boolean validationStaus = true;
        try{
            ArrayList<String> templateIdList = new ArrayList<>();

            ElasticSearch elasticSearch = new ElasticSearch();

            String templateId;
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

            String payload;
            for(int i=0;i<childSlLList.size();i++) {

                payload = "{\"query\":{\"match\":{\"childslaId\":\"" + childSlLList.get(i) + "\"}}}";

                templateId = postgreSQLJDBC.doSelect("select raw_data_template_id from child_sla where id=" + childSlLList.get(i) +"").get(0).get(0);

                elasticSearch.deleteIndexData(templateId,payload);

                templateIdList.add(templateId);

            }
            postgreSQLJDBC.closeConnection();
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while deleting Data At Index");
            validationStaus = false;
        }
        return validationStaus;
    }
}
