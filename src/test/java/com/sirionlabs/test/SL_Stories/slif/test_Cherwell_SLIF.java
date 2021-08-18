package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.api.ServiceLevel.SLIF_Schemas;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

public class test_Cherwell_SLIF extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_Cherwell_SLIF.class);

    String configFilePath;
    String configFileName;

    String name;
    String description;
    String active;
    String[] email;
    String url;
    String associationId;
    String scopeId;
    String searchId;
    String authId;
    String oauthUrl;
    String clientKey;
    String userName;
    String password;
    String UDC;
    String dbHostAddress;
    String dbName;
    String dbPortName;
    String dbUserName;
    String dbPassword;
    PostgreSQLJDBC postgreSQLJDBC;
    int destinationId;
    String adminUserName;
    String adminPassword;

    @BeforeClass()
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");
        description = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","description");
        active = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","active");
        email = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source config details","email").split(",");

        Check check = new Check();
        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        check.hitCheck(adminUserName,adminPassword);

        dbHostAddress = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress,dbPortName,dbName,dbUserName,dbPassword);
    }

    //C153588
    @Test(enabled = false)  // working now
    public void TestSourceCreationCherwell(){

        CustomAssert customAssert = new CustomAssert();
        int sourceId = -1;

        try{

            name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","name")+" "+ DateUtils.getCurrentDateInMM_DD_YYYY();
            UDC = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","udc");
            url = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","url");
            associationId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","associationid");
            scopeId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","scopeid");
            searchId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","searchid");
            oauthUrl = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","oauthurl");
            clientKey = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","clientkey");
            userName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","username");
            password = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"source cherwell detail","password");

            APIResponse response = SLIF_Schemas.hitGetCreateSource(executor);
            String responseBody  = response.getResponseBody();

            if(!APIUtils.validJsonResponse(responseBody)){
                logger.error("Create Source API Response is not a valid Json");
                customAssert.assertTrue(false,"Create Source API Response is not a valid Json");
            }

            String payload= createPayloadForSourceCherwell();
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
                customAssert.assertTrue(true,"Source has been created successfully");
            }

        }catch (Exception e){
            logger.error("Exception while validating Create Source API " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating Create Source  API " + e.getMessage());
        }finally {
            deleteSource(sourceId);
        }
        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestCherwellDestination(){
        CustomAssert customAssert = new CustomAssert();
        destinationId = -1;

        try {
            String name = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "name") + DateUtils.getCurrentDateInMM_DD_YYYY();
            String[] slaId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "sla").split(",");
            int sourceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "sourceid"));
            int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "supplier"));
            String query = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "query");
            int frequencyId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "frequency"));
            String[] slaSubCategory = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "slasubcategory").split(",");
            String[] contract = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "contract").split(",");
            int reportId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "report id"));
            String[] performanceStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "performance status").split(",");
            String[] computationFrequency = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "computation frequency").split(",");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "destination config details", "entity type id"));

            destinationId = createDestination(name, sourceId, frequencyId, slaId, slaSubCategory, computationFrequency, entityTypeId, query, customAssert);

            List<String> listingData = new ArrayList<>();

            //Frequency Id 2 means monthly
            if (frequencyId == 2) {
                String startDate = getLastMonthDueDate().get(0);
                String endDate = getLastMonthDueDate().get(1);
                listingData = getCSLDataFromListingPage(customAssert, contract, performanceStatus, slaId, computationFrequency, startDate, endDate);

            }

            // Validating Test CSL
            String filePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLIF";

            String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"cherwell dest detail", "filename");

            Boolean downLoadCSLDataStatus = downloadCSLDataFromDestination(computationFrequency, frequencyId, supplierId, slaId, contract, performanceStatus, filePath, fileName, reportId, customAssert);

            if (downLoadCSLDataStatus) {
                List<String> excelData = getCSLDataFromExcelSheet(customAssert, filePath, fileName);

                Boolean result = compareExcelAndListingData(customAssert, listingData, excelData);
                if (result)
                    customAssert.assertTrue(true, "Eligible CSL has validated successfully");
            }
        }catch (Exception e){
            customAssert.assertFalse(false,"Error while testing cherwell destination");
        }
        customAssert.assertAll();
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

    private String createPayloadForSourceCherwell(){

        String payload;

        payload = "{\"body\":{\"data\":{\"dynamicData\":{\"name\":\"dynamicData\",\"multiEntitySupport\"" +
                ":false,\"values\":{\"url\":\""+url+"\",\"associationId\":\""+associationId+"\"," +
                "\"scopeOwnerId\":\""+scopeId+"\",\"searchId\":\""+searchId+"\",\"oauthUrl\":" +
                "\""+oauthUrl+"\",\"clientId\":\""+clientKey+"\",\"username\":\""+userName+"\"," +
                "\"password\":\""+password+"\"}},\"authMetadata\":{\"name\":\"authMetadata\",\"id\":12635," +
                "\"multiEntitySupport\":false,\"values\":{\"id\":5}},\"canSupplierBeParent\":true," +
                "\"requestMetadata\":{\"name\":\"requestMetadata\",\"id\":12629,\"multiEntitySupport" +
                "\":false,\"values\":{\"id\":4,\"name\":\"Cherwell\"}},\"active\":{\"name\":\"active\"," +
                "\"id\":12625,\"values\":"+active+",\"multiEntitySupport\":false},\"name\":" +
                "{\"name\":\"name\",\"id\":12623,\"multiEntitySupport\":false,\"values\":\""+name+"\"}," +
                "\"uniqueDataCriteria\":{\"name\":\"uniqueDataCriteria\",\"id\":12665,\"multiEntitySupport\"" +
                ":false,\"values\":\""+UDC+"\"},\"description\":{\"name\":\"description\",\"id\":12624," +
                "\"multiEntitySupport\":false,\"values\":\""+description+"\"},\"parentEntityType\":{\"name\"" +
                ":\"parentEntityType\",\"values\":{\"name\":\"Source Integration Config\",\"id\":338}," +
                "\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\"" +
                ":false,\"values\":["+createPayloadStringFromArray(email)+"]},\"entityTypeId\":{\"name\"" +
                ":\"entityTypeId\",\"values\":338,\"multiEntitySupport\":false}}}}";

        return payload;

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

    private String createPayloadStringFromArray(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"id\": \"" + list[i] + "\"},";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
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

    private String createPayloadSLAStringFromArray(String[] slaItem, String[] slaCategory) {
        String payload = "";
        for (int i = 0; i < slaItem.length; i++) {
            payload += "{ \"id\": "+slaItem[i]+", \"parentId\": "+slaCategory[i]+" },";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
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

    private Boolean compareExcelAndListingData(CustomAssert customAssert,List<String> excelData, List<String> listingData){
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
        }catch(Exception e){
            customAssert.assertFalse(false, "Error in comparing Excel and Listing CSL");
        }
        return comparisonStatus;
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

    private String createPayloadForReport(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += ""+ list[i] +",";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
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
