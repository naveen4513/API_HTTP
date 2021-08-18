package com.sirionlabs.test.SL_Stories.slif;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.mongodb.MongoDBConnection;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.*;

public class test_After_Scheduler_UploadRawData extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(test_After_Scheduler_UploadRawData.class);

    String configFilePath;
    String configFileName;
    String adminUserName;
    String adminPassword;

    String dbHostName;
    String dbPortName;

    String dbName;
    String dbUserName;
    String dbPassowrd;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSLIF2ConfigFileName");

        Check check = new Check();

        adminUserName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
        adminPassword = ConfigureEnvironment.getEnvironmentProperty("clientUserPassword");
        check.hitCheck(adminUserName,adminPassword);

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassowrd = ConfigureEnvironment.getEnvironmentProperty("dbPassword");
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

    @Test(dataProvider = "flowsToTest", enabled = true, priority = 0)
    public void TestCheckMails(String flowToTest){

        CustomAssert customAssert = new CustomAssert();
        String name = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"name")+DateUtils.getCurrentDateInMM_DD_YYYY();
        String subjectLine = "Data fetch request for  " +name+ " completed";

        try {
            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for destination " + name);
            }

            List<String> expectedString = new ArrayList<>();
            expectedString.add("Entities requested to update");
            expectedString.add("Entities successfully updated");
            expectedString.add("Entities failed to update");
            validateBodyOfEmail(recordFromSystemEmailTable, expectedString, customAssert);
        }catch (Exception e){
            customAssert.assertFalse(false,"Exception while validating Success Mails" + e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "flowsToTest", enabled = false, priority = 0)
    public void TestCheckRawDataTab(String flowToTest){
        CustomAssert customAssert = new CustomAssert();
        try {

            int destinationId = fetchDestinationIdFromTable(flowToTest);
            String hostAddress = "192.168.2.235";
            int port = 27017;
            String dbName = "slif_raw_data";
            String collectionName = "SourceRawData";
            String fieldName = "destinationConfigId";

            MongoDBConnection mongoDBConnection = new MongoDBConnection(hostAddress,port);
            List<Document> response = mongoDBConnection.getDBResponse(dbName,collectionName,fieldName,destinationId);
            String newResponse = response.get(response.size()-1).toJson();

            JSONObject jsonObject = new JSONObject(newResponse);
            JSONArray mongoData = jsonObject.getJSONArray("result");

            int cslId = 366711;

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            JSONArray cslRawData = serviceLevelHelper.getJsonRawDataCSLTab(cslId,mongoData.length(),customAssert);
            HashMap<String, String> newHashMap = new HashMap<>();

//            for(int k =0;k<newData.length();k++){
////                Map<String, Object> newMap = newData.getJSONObject(k).toMap();
//                String number = newData.getJSONObject(i).get("number").toString();
//                String opened_at = newData.getJSONObject(i).get("opened_at").toString();
//                String state = newData.getJSONObject(i).get("state").toString();
//                String priority = newData.getJSONObject(i).get("priority").toString();
//                if(newHashMap.containsKey(number){
//                    if()
//
//                }
//            }

            for(int i=0;i<mongoData.length();i++) {
                JSONObject cslDataObject = cslRawData.getJSONObject(i);
                String number = mongoData.getJSONObject(i).get("number").toString();
                String opened_at = mongoData.getJSONObject(i).get("opened_at").toString();
                String state = mongoData.getJSONObject(i).get("state").toString();
                String priority = mongoData.getJSONObject(i).get("priority").toString();

                for (int j = 1; j < cslDataObject.length(); j++) {
                    if(cslDataObject.getJSONObject(String.valueOf(j)).getString("columnName").equalsIgnoreCase("number")){
                        customAssert.assertTrue(cslDataObject.getJSONObject(String.valueOf(j)).getString("columnValue").equalsIgnoreCase(number),"SLIF columns are all presnt in CSL Raw Data Tab");
                    }
                    else if (cslDataObject.getJSONObject(String.valueOf(j)).getString("columnName").equalsIgnoreCase("opened_at")){
                        customAssert.assertTrue(cslDataObject.getJSONObject(String.valueOf(j)).getString("columnValue").equalsIgnoreCase(opened_at),"SLIF columns are all presnt in CSL Raw Data Tab");
                    }
                    else if (cslDataObject.getJSONObject(String.valueOf(j)).getString("columnName").equalsIgnoreCase("state")){
                        customAssert.assertTrue(cslDataObject.getJSONObject(String.valueOf(j)).getString("columnValue").equalsIgnoreCase(state),"SLIF columns are all presnt in CSL Raw Data Tab");
                    }
                    else if (cslDataObject.getJSONObject(String.valueOf(j)).getString("columnName").equalsIgnoreCase("priority")){
                        customAssert.assertTrue(cslDataObject.getJSONObject(String.valueOf(j)).getString("columnValue").equalsIgnoreCase(priority),"SLIF columns are all presnt in CSL Raw Data Tab");
                    }
                }
            }

        }catch (Exception e) {
            logger.error("Exception while validation Raw Data Tab on CSL" + e.getMessage());
            customAssert.assertTrue(false,"Exception while validation Raw Data Tab on CSL" + e.getMessage());
        }
    }

    @Test(enabled = false, priority = 2)
    public void deleteMongoData(){
        String hostAddress = "192.168.2.235";
        int port = 27017;
        String dbName = "slif_raw_data";
        String collectionName = "SourceRawData";
        String fieldName = "destinationConfigId";
        int fieldValue = 558;
//        int fieldValue = fetchDestinationIdFromTable(name);
        MongoDBConnection mongoDBConnection = new MongoDBConnection(hostAddress,port);
        mongoDBConnection.deleteDocumentFromDB(dbName,collectionName,fieldName,fieldValue);
    }

    @Test(dataProvider = "flowToTest", enabled = false)
    public void getRecordFromSystemEmailTableFailure(String flowToTest){

        String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();
        String destinationId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"name") + DateUtils.getPreviousDateInMM_DD_YYYY(currentDate);

        String subjectLine = "Data fetch request for  " +destinationId+ " completed";
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

    }

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine) {

        String sqlQuery = "select body from system_emails where subject ilike '%" + subjectLine + "%'";
        List<List<String>> queryResult = null;
        PostgreSQLJDBC postgreSQLJDBC;
        dbName = "letterbox-sl";

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);

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

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);
        try {


            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
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
//            startDate = currentYear+"-"+currentMonthString+"-"+"01";
            end = getEndDate(currentMonth,currentYear);
//            endDate = currentYear+"-"+currentMonthString+"-"+end;
            endDate =currentMonthString+"-"+end+"-"+currentYear;
//            endDate = currentYear+"-"+currentMonthString+"-"+end;
            dates.add(startDate);
            dates.add(endDate);
        } catch (Exception e) {

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

    private List<Integer> getIDCSLDataFromListingPage(String[] contract, String[] performanceStatus, String[] slaItem, String[] computationFrequency, String startDate, String endDate){

        List<Integer> cslList=new ArrayList<>();

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
                Integer columnValue;
                while(keys.hasNext()) {
                    String key = keys.next();
                    columnName = indRowDataJson.getJSONObject(key).get("columnName").toString();
                    if(columnName.equals("id")){
                        columnValue = Integer.parseInt(indRowDataJson.getJSONObject(key).get("value").toString().split(":;")[1]);
//                        columnValue = "CSL"+columnValue;
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

    private Boolean validateRawDataTabFromRawDataExcelDownloaded(int cslId, int destinationId, CustomAssert customAssert){

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

    private String createPayloadStringFromArray(String[] list) {
        String payload = "";
        for (int i = 0; i < list.length; i++) {
            payload += "{\"id\": \"" + list[i] + "\"},";
        }
        payload = payload.substring(0,payload.length()-1);
        return payload;
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

    private int fetchDestinationIdFromTable(String name){
        int destinationId = -1;

        try {
            String dbHostAddress = "192.168.2.157";
            String dbPortName = "5432";
            String dbName = "slif";
            String dbUserName = "postgres";
            String dbPassword = "postgres";

            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> dbCurrentDate = postgreSQLJDBC.doSelect("select id from destination_integration_config where name ilike '%"+name+"%';");
            destinationId = Integer.parseInt(dbCurrentDate.get(0).get(0));
            postgreSQLJDBC.closeConnection();
        }catch (Exception e){
            logger.debug("Exception while getting destination id from table destination_integration_config");
        }
        return destinationId;
    }

    private Boolean validateBodyOfEmail(List<List<String>> recordFromSystemEmailTable, List<String> expectedStringInBody, CustomAssert customAssert) {

        Boolean validationStatus = true;

        String actualBodyHtml;
        try {

            for (int i = 0; i < recordFromSystemEmailTable.size(); i++) {
                actualBodyHtml = recordFromSystemEmailTable.get(i).get(3);

                for (int j = 0; j < expectedStringInBody.size(); j++) {

                    if (!actualBodyHtml.contains(expectedStringInBody.get(j))) {
                        customAssert.assertTrue(false, "While validating email Body Html does not contain the expected String " + expectedStringInBody.get(j));
                        validationStatus = false;
                    }
                }


            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating body of Email");
            validationStatus = false;
        }

        return validationStatus;
    }



}