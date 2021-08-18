package com.sirionlabs.test.common;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.WeekTimeForEntities;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ActionIssuesFromCDRAndWeekType extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(ActionIssuesFromCDRAndWeekType.class);
    CustomAssert customAssert = new CustomAssert();
    SoftAssert softAssert;
    private static String configFilePath = null;
    private static String configFileName = null;
    private static String clientAdminUserName = null;
    private static String clientAdminUserPassword = null;
    private static String endUserName = null;
    private static String endUserPassword = null;
    private static String supplierTypeUserName = null;
    private static String supplierTypeUserPassword = null;
    APIValidator validator;
    JSONObject jsonObject;
    private static String hostUrl;


    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName");
        clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client admin user details", "username");
        clientAdminUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client admin user details", "password");
        endUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "end user details", "username");
        endUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "end user details", "password");
        supplierTypeUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type end user details cdr", "username");
        supplierTypeUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type end user details cdr", "password");
        hostUrl = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "hosturl");
    }

    ////////////////////////CDR Week Type, Ageing,Cycle Time, Lead Time ///////////////////////////

   // @Test(dataProvider = "checkCDRWeekTypeData")
    public void checkCDRWeekTime(String username,String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName, String session,String showAPIPath){
        softAssert = new SoftAssert();
        loginWithEndUser(username,password);
        String response = ContractDraftRequest.createCDR(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        // Perform send for client review action
        String sendForClientReview = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        APIValidator validator = performActionsOnCreatedEntity("contract draft request",sendForClientReview,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        // Perform Test Internationalization Action
        String testInternationalization = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntity("contract draft request",testInternationalization,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        String requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("values"));

        String weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));

        float days = diffBetweenDays(weekType,requestedDate + " 12:00:00", DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy") + " 00:00:00");

        Double ageing = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("ageing").get("values");

        // Verify Ageing and Week Type
        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        String expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        // Perform Reject Action
        String reject = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values","Test API");
        String payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

        validator = executor.post(hostUrl, reject, WeekTimeForEntities.getHeaders(),
                payload);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        // Perform Send For Client Review
        sendForClientReview = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntity("contract draft request",sendForClientReview,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        //Perform Approve Action
        String approve = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntity("contract draft request",approve,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        //Perform Publish Action
        String publish = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");

        JSONObject publishPayload = new JSONObject();
        publishPayload.put("name","End User License Agreement");
        publishPayload.put("id","1003");

        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("agreementType").put("values",publishPayload);

        payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

        validator = executor.post(hostUrl, publish, WeekTimeForEntities.getHeaders(),
                payload);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("values"));

        weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));

        days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"));

        ageing = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("Aging").get("values").toString());

        Double cycleTime = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("CycleTime").get("values").toString());

        // Verify Ageing and Week Type
        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        softAssert.assertTrue((days-0.8)< cycleTime && (days+0.8)>ageing,
                "Cycle time is not as expected");

        expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        softAssert.assertAll();
    }

  // @Test(dataProvider = "checkCDRWeekTypeDataWithActualDate")
    public void checkCDRWeekTimeWithActualTime(String username,String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName, String session,String showAPIPath){
        softAssert = new SoftAssert();
        loginWithEndUser(username,password);
        String response =ContractDraftRequest.createCDR(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        // Perform send for client review action
        String sendForClientReview = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        APIValidator validator = performActionsOnCreatedEntityWithActualDate("contract draft request","04-12-2019",sendForClientReview,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        // Perform Test Internationalization Action
        String testInternationalization = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntityWithActualDate("contract draft request","04-12-2019",testInternationalization,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        String requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("values"));

        String weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));

        float days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"));

        Double ageing = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("ageing").get("values");

        // Verify Ageing and Week Type
        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        String expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        // Perform Reject Action
        String reject = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values","Test API");
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values","04-20-2019");
        String payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

        validator = executor.post(hostUrl, reject, WeekTimeForEntities.getHeaders(),
                payload);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("values"));

        weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));

        days = diffBetweenDays(weekType,requestedDate,"04-20-2019");

        ageing = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("Aging").get("values").toString());

        Double leadTime1 = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("LeadTime1").get("values").toString());

        // Verify Ageing and Lead Time 1
        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        float leadTime1Days =diffBetweenDays(weekType,"04-12-2019","04-20-2019");

        softAssert.assertTrue((leadTime1Days-0.8)< leadTime1 && (leadTime1Days+0.8)>leadTime1,
                "Lead Time 1 is not as expected");

        // Perform Send For Client Review
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values","04-25-2019");

        sendForClientReview = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntity("contract draft request",sendForClientReview,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        Double leadTime2 = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("LeadTime2").get("values").toString());

        // Verify Lead Time 2
        float leadTime2Days =diffBetweenDays(weekType,"04-20-2019","04-25-2019");

        softAssert.assertTrue((leadTime2Days-0.8)< leadTime2 && (leadTime2Days+0.8)>leadTime1,
                "Lead time 2 is not as expected");

        //Perform Approve Action
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values","05-02-2019");
        String approve = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        validator = performActionsOnCreatedEntity("contract draft request",approve,headers);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        leadTime1 = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("LeadTime1").get("values").toString());
        leadTime1Days = leadTime1Days + diffBetweenDays(weekType,"04-25-2019","05-02-2019");

        softAssert.assertTrue((leadTime1Days-0.8)< leadTime1 && (leadTime1Days+0.8)>leadTime1,
                "Ageing is not as expected");

        //Perform Publish Action
        String publish = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");

        JSONObject publishPayload = new JSONObject();
        publishPayload.put("name","End User License Agreement");
        publishPayload.put("id","1003");

        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values","05-10-2019");
        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("agreementType").put("values",publishPayload);

        payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

        validator = executor.post(hostUrl, publish, WeekTimeForEntities.getHeaders(),
                payload);

        validator.validateResponseCode(200,customAssert);

        // Hit Show CDR API
        jsonObject =hitShowEntity(apiPath,headers);

        requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("effectiveDate").get("values"));

        weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));

        days = diffBetweenDays(weekType,requestedDate,"05-10-2019");

        ageing = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("Aging").get("values").toString());

        Double cycleTime = Double.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("leadTimes").getJSONObject("values").getJSONObject("CycleTime").get("values").toString());

        // Verify Ageing and Week Type
        softAssert.assertTrue((days-0.8)< cycleTime && (days+0.8)>ageing,
                "Cycle time is not as expected");

        expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        softAssert.assertAll();
    }

    @Test(dataProvider = "CheckWeekTypeInDownloadedFileData")
    public void checkWeekTypeInDownloadedFile(String username,String password,String filterJson,int entityTypeId,String entityName, int listId) throws FileNotFoundException {
        softAssert = new SoftAssert();
        loginWithEndUser(username, password);
        // Download All Columns
        String outputFilePath = ListDataHelper.downloadListDataForAllColumns(filterJson,entityTypeId,entityName,listId,customAssert);

        List<String> AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/AllColumn.xlsx","").trim(),"AllColumn.xlsx","Data");
        softAssert.assertTrue(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is not present");

        List<String> data=XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath.replace("/AllColumn.xlsx","").trim(),"AllColumn.xlsx","Data",87,5,10);
        softAssert.assertTrue(data.stream().findFirst().get().contains("Day"),"Different Week Types are not present in downloaded sheet");

        // Download Selected Columns With Week Type
        Map<Integer,String> selectedColumns = new LinkedHashMap<>();
        selectedColumns.put(12260,"title");
        selectedColumns.put(12259,"id");
        selectedColumns.put(18215,"customer");
        selectedColumns.put(12268,"completion_date");
        selectedColumns.put(18673,"weektype");


        outputFilePath = ListDataHelper.downloadListDataForSelectedColumns(filterJson,entityTypeId,entityName,listId,selectedColumns,softAssert);

        AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data");
        softAssert.assertTrue(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is not present");

        data=XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data",4,5,10);
        softAssert.assertTrue(data.stream().findFirst().get().contains("Day"),"Different Week Types are not present in downloaded sheet");

        // Download Selected Columns WithOut Week Type
        selectedColumns = new LinkedHashMap<>();
        selectedColumns.put(12260,"title");
        selectedColumns.put(12259,"id");
        selectedColumns.put(18215,"customer");
        selectedColumns.put(12268,"completion_date");

        outputFilePath = ListDataHelper.downloadListDataForSelectedColumns(filterJson,entityTypeId,entityName,listId,selectedColumns,softAssert);

        AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data");
        softAssert.assertFalse(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is present");

        softAssert.assertAll();
    }


    @DataProvider(name = "CheckWeekTypeInDownloadedFileData")
    public Object[][] getDataForWeekTypeInDownloadedFile(){

        return new Object[][] {
                {
                        endUserName,endUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}" ,
                        160,"cdr",279
                },
        };
    }

    @DataProvider(name = "checkCDRWeekTypeData")
    public Object[][] dataForCDRWeekType(){
        return new Object[][]{
                {
                        endUserName,endUserPassword, ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "cdr week type", "/cdr/show/"
                },
        };
    }


    @DataProvider(name = "checkCDRWeekTypeDataWithActualDate")
    public Object[][] dataForCDRWeekTypeWithActualDate(){
        return new Object[][]{
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "cdr week type with actual time", "/cdr/show/"
                },
        };
    }

    private Boolean loginWithEndUser(String username,String password) {
        logger.info("Logging with UserName [{}] and Password [{}]", username, password);
        Check checkObj = new Check();
        checkObj.hitCheck(username, password);

        return (Check.getAuthorization() != null);
    }

    public JSONObject hitShowEntity(String apiPath,Map<String,String> headers){

        validator=executor.get(hostUrl,apiPath,headers);
        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        return jsonObject;
    }

    public APIValidator performActionsOnCreatedEntity(String entity,String query,Map<String,String> headers){
        validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");

        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");

        return validator;
    }

    public APIValidator performActionsOnCreatedEntityWithActualDate(String entity,String actualDate,String query,Map<String,String> headers){

        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values",
                    actualDate);
        validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");

        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");

        return validator;
    }

    public float diffBetweenDays(String weekType,String startDate,String endDate){
        int notToIncludeDays;
        float diffDays = 0;
        DateTime jodastarttime = null;
        DateTime jodaendtime = null;
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        Date d1 = null;
        Date d2 = null;

        if(startDate.contains(":")){
            DateTimeFormatter dtf = DateTimeFormat.forPattern("MM-dd-yyyy HH:mm:ss");
            jodastarttime = dtf.parseDateTime(startDate);
            jodaendtime = dtf.parseDateTime(endDate);}
        else{
            DateTimeFormatter dtf = DateTimeFormat.forPattern("MM-dd-yyyy");
            jodastarttime = dtf.parseDateTime(startDate);
            jodaendtime = dtf.parseDateTime(endDate);
        }

        try {
            d1 = format.parse(startDate);
            d2 = format.parse(endDate);

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();

            diffDays = (diff / (24 * 60 * 60 * 1000));

            if(weekType.equals("Five Days")){
                notToIncludeDays = weekendCount5Days(jodastarttime,jodaendtime);
                return diffDays - ((float) notToIncludeDays);
            }
            else if(weekType.equals("Six Days")){
                notToIncludeDays = weekendCount6Days(jodastarttime,jodaendtime);
                return diffDays - ((float) notToIncludeDays);
            }
            else if(weekType.equals("Seven Days")){
                diffDays = diffDays / 7;
                diffDays = diffDays * 7;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return diffDays;
    }

    public static int weekendCount5Days(DateTime start, DateTime end) {
        int weekendDays = 0;
        try{
            while(end.compareTo(start) > 0){
                int day = start.getDayOfWeek();
                if ((day == DateTimeConstants.SUNDAY) || (day == DateTimeConstants.SATURDAY))
                    weekendDays++;
                start = start.plusDays(1);
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return weekendDays;
    }

    public static int weekendCount6Days(DateTime start, DateTime end) {
        int weekendDays = 0;
        try{
            while(end.compareTo(start) > 0){
                int day = start.getDayOfWeek();
                if ((day == DateTimeConstants.SUNDAY))
                    weekendDays++;
                start = start.plusDays(1);
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return weekendDays;
    }
}
