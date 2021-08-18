package com.sirionlabs.test.common;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.commonAPI.WeekTimeForEntities;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
public class TestWeekTimeForEntities extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWeekTimeForEntities.class);
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
        supplierTypeUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type end user details", "username");
        supplierTypeUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type end user details", "password");
        hostUrl = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "hosturl");
    }

    //C46386 , C46361
    //@Test(dataProvider = "CheckEntityWeekTypeData")
    public void checkEntityWeekTime(String username,String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName, String session,String showAPIPath){
        softAssert = new SoftAssert();
        loginWithEndUser(username,password);
        String response = null;

        if(username.equals(supplierTypeUserName) && showAPIPath.contains("action")){
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("action")) {
           response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if (showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if (showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        jsonObject =hitShowAction(apiPath,headers);

        String submitToOwnerQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        String requestedDate = null;
        if(showAPIPath.contains("action")){
            requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("requestedOn").get("values"));}
        else if(showAPIPath.contains("issue") || showAPIPath.contains("dispute")){
            requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("issueDate").get("values"));}

        float days = 0;
        String weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));
        if(showAPIPath.contains("action") || showAPIPath.contains("issue")){
            days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"));}
        else if(showAPIPath.contains("dispute")){
            days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"));}

        Double ageing = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("ageing").get("values");

        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        String expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        if(showAPIPath.contains("action")){
        performActionsOnCreatedEntity("action",submitToOwnerQueryString,headers);}
        else if(showAPIPath.contains("issue") ||  showAPIPath.contains("dispute")){
            performActionsOnCreatedEntity("issue-work1",submitToOwnerQueryString,headers);}

        jsonObject =hitShowAction(apiPath,headers);

        String submitActionQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");

        if(showAPIPath.contains("action")){
        performActionsOnCreatedEntity("action",submitActionQueryString,headers);}
        else if(showAPIPath.contains("issue")  ||  showAPIPath.contains("dispute")){
            performActionsOnCreatedEntity("issue-work2",submitActionQueryString,headers);}

        jsonObject =hitShowAction(apiPath,headers);
        String approveQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");

        validator = performActionsOnCreatedEntity("action",approveQueryString,headers);

        jsonObject =hitShowAction(apiPath,headers);
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        Double cycleTime = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("cycleTime").get("values");

        softAssert.assertTrue((days-0.8)< cycleTime && (days+0.8)>cycleTime,
                "Cycle Time is not as expected");

        softAssert.assertAll();
    }

   //@Test(dataProvider = "CheckEntityWeekTypeDataWithActualTime")
    public void checkEntityWeekTimeWithActualTime(String username,String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName, String session,String showAPIPath){
        softAssert = new SoftAssert();
        loginWithEndUser(username,password);
        String response = null;

        if(username.equals(supplierTypeUserName) && showAPIPath.contains("action")){
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("action")) {
           response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if (showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if (showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        jsonObject =hitShowAction(apiPath,headers);

        String submitToOwnerQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        String requestedDate = null;
        if(showAPIPath.contains("action")){
            requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("requestedOn").get("values"));}
        else if(showAPIPath.contains("issue") || showAPIPath.contains("dispute")){
            requestedDate = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("issueDate").get("values"));}

        String weekType = String.valueOf(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType").getJSONObject("values").get("name"));
        float days =0;
        if(showAPIPath.contains("action") || showAPIPath.contains("issue")){
            days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy HH:mm:ss"));}
        else if(showAPIPath.contains("dispute")){
            days = diffBetweenDays(weekType,requestedDate,DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy"));}

        Double ageing = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("ageing").get("values");

        softAssert.assertTrue((days-0.8)< ageing && (days+0.8)>ageing,
                "Ageing is not as expected");

        String expectedWeekType = ParseConfigFile.getValueFromConfigFile(filePath, fileName, session,"expectedweektype");

        softAssert.assertTrue(weekType.equals(expectedWeekType),
                "Week Type is not correct");

        if(showAPIPath.contains("action")){
            performActionsOnCreatedEntityWithActualDate("action","04-12-2019 00:00:00",submitToOwnerQueryString,headers);}
        else if(showAPIPath.contains("issue")){
            performActionsOnCreatedEntityWithActualDate("issue-work1","04-12-2019 00:00:00",submitToOwnerQueryString,headers);}
        else if( showAPIPath.contains("dispute")){
            performActionsOnCreatedEntityWithActualDate("issue-work1","04-12-2019",submitToOwnerQueryString,headers);}

        jsonObject =hitShowAction(apiPath,headers);

        String submitActionQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");

        if(showAPIPath.contains("action")){
            performActionsOnCreatedEntityWithActualDate("action","04-18-2019 00:00:00",submitActionQueryString,headers);}
        else if(showAPIPath.contains("issue")){
            performActionsOnCreatedEntityWithActualDate("issue-work2","04-18-2019 00:00:00",submitActionQueryString,headers);}
        else if(showAPIPath.contains("dispute")){
            performActionsOnCreatedEntityWithActualDate("issue-work2","04-18-2019",submitActionQueryString,headers);}


        jsonObject =hitShowAction(apiPath,headers);
        String approveQueryString = (String) jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions").getJSONObject(0).get("api");
        String actualDate = null;
        if(showAPIPath.contains("dispute")){
            actualDate ="04-20-2019";}
        else{ actualDate ="04-20-2019 00:00:00";}
        validator = performActionsOnCreatedEntityWithActualDate("action",actualDate,approveQueryString,headers);

        jsonObject =hitShowAction(apiPath,headers);
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        Double cycleTime = (Double) jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("cycleTime").get("values");

        float cycleDays = diffBetweenDays(weekType,requestedDate,actualDate);
        softAssert.assertTrue(ageing!=cycleTime,"cycle is not recalculated");
        softAssert.assertTrue((cycleDays-0.8)< cycleTime && (cycleDays+0.8)>cycleTime,
                "Cycle Time is not as expected");

        softAssert.assertAll();
    }


    //C46363, C46365 , C46364 week type column value not displayed
    //@Test(dataProvider = "checkWeekTypeFilterData")
    public void checkWeekTypeFilter(String username,String password,String filterDataUrl,String listDataApiUrl,String fiveDaysPayload,String SixDaysPayload,String SevenDaysPayload){
        softAssert = new SoftAssert();
        loginWithEndUser(username, password);
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();
        validator = executor.post(hostUrl,filterDataUrl,headers, "{}");
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        List<String> filters = ListRendererFilterDataHelper.getAllFilterNames(jsonObject.toString());
        softAssert.assertTrue(filters.contains("weektypes"), "Week Type filter is not present");
        List<Map<String,String>> weekTypeFilterOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(jsonObject.toString(),"weektypes");

        softAssert.assertTrue(weekTypeFilterOptions.stream().map(m->m.get("name")).collect(Collectors.toList()).contains("Five Day") &&
                        weekTypeFilterOptions.stream().map(m->m.get("name")).collect(Collectors.toList()).contains("Six Day") &&
                        weekTypeFilterOptions.stream().map(m->m.get("name")).collect(Collectors.toList()).contains("Seven Day"),
                "Week Types are not present");

                //First Filter - 5 Week Type
        String payload  = fiveDaysPayload;
        hitFilterWithDifferentWeekDays(listDataApiUrl,payload,headers,"Five Day");

        //Clear Filter
        clearWeekTypeFilter(listDataApiUrl,headers);

        //Second Filter - 6 Week Type
        payload  = SixDaysPayload;

        hitFilterWithDifferentWeekDays(listDataApiUrl,payload,headers,"Six Day");

        //Clear Filter
        clearWeekTypeFilter(listDataApiUrl,headers);

        //Third Filter - 7 Week Type
        payload  = SevenDaysPayload;
        hitFilterWithDifferentWeekDays(listDataApiUrl,payload,headers,"Seven Day");

        //Clear Filter
        clearWeekTypeFilter(listDataApiUrl,headers);
        softAssert.assertAll();
    }

    //C46366
   // @Test(dataProvider = "CheckWeekTypeInDownloadedFileData")
    public void checkWeekTypeInDownloadedFile(String username,String password,String filterJson,int entityTypeId,String entityName, int listId) throws FileNotFoundException {
        softAssert = new SoftAssert();
        loginWithEndUser(username, password);
        // Download All Columns
        String outputFilePath = ListDataHelper.downloadListDataForAllColumns(filterJson,entityTypeId,entityName,listId,customAssert);

        List<String> AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/AllColumn.xlsx","").trim(),"AllColumn.xlsx","Data");
        softAssert.assertTrue(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is not present");

        List<String> data=XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath.replace("/AllColumn.xlsx","").trim(),"AllColumn.xlsx","Data",35,5,10);
        softAssert.assertTrue(data.stream().findFirst().get().contains("Day"),"Different Week Types are not present in downloaded sheet");

        // Download Selected Columns With Week Type
        Map<Integer,String> selectedColumns = new LinkedHashMap<>();
        if(entityName.equals("actions")){
        selectedColumns.put(181,"contract");
        selectedColumns.put(178,"id");
        selectedColumns.put(182,"managementtype");
        selectedColumns.put(184,"plannedcompetitiondate");
        selectedColumns.put(18335,"weektype");}
        else if(entityName.equals("issues")){
            selectedColumns.put(158,"contract");
            selectedColumns.put(155,"id");
            selectedColumns.put(159,"managementtype");
            selectedColumns.put(161,"plannedcompetitiondate");
            selectedColumns.put(18334,"weektype");}
        else if(entityName.equals("disputes")){
            selectedColumns.put(13177,"contract");
            selectedColumns.put(13174,"id");
            selectedColumns.put(13178,"managementtype");
            selectedColumns.put(13179,"plannedcompetitiondate");
            selectedColumns.put(18336,"weektype");}


        outputFilePath = ListDataHelper.downloadListDataForSelectedColumns(filterJson,entityTypeId,entityName,listId,selectedColumns,softAssert);

        AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data");
        softAssert.assertTrue(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is not present");

        data=XLSUtils.getOneColumnDataFromMultipleRows(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data",4,5,10);
        softAssert.assertTrue(data.stream().findFirst().get().contains("Day"),"Different Week Types are not present in downloaded sheet");

        // Download Selected Columns WithOut Week Type
        selectedColumns = new LinkedHashMap<>();
        if(entityName.equals("actions")){
        selectedColumns.put(181,"contract");
        selectedColumns.put(178,"id");
        selectedColumns.put(182,"managementtype");
        selectedColumns.put(184,"plannedcompetitiondate");}
        else if(entityName.equals("issues")){
            selectedColumns.put(158,"contract");
            selectedColumns.put(155,"id");
            selectedColumns.put(159,"managementtype");
            selectedColumns.put(161,"plannedcompetitiondate"); }
        else if(entityName.equals("disputes")){
            selectedColumns.put(13177,"contract");
            selectedColumns.put(13174,"id");
            selectedColumns.put(13178,"managementtype");
            selectedColumns.put(13179,"plannedcompetitiondate"); }

        outputFilePath = ListDataHelper.downloadListDataForSelectedColumns(filterJson,entityTypeId,entityName,listId,selectedColumns,softAssert);

        AllColumnsHeader = XLSUtils.getOffSetHeaders(outputFilePath.replace("/SelectedColumn.xlsx",""),"SelectedColumn.xlsx","Data");
        softAssert.assertFalse(AllColumnsHeader.contains("WEEK TYPE"),
                "Week Type is present");

        softAssert.assertAll();
    }

   // C46362   edit button in dispute workflow, week type field editable
   //@Test(dataProvider = "AuditLogAndFieldHistoryData")
    public void verifyChangesInAuditLogAndFieldHistory(String username, String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName,
                                                       String session,String showAPIPath,String tableListDataPayload,String tableListDataAPIPath,
                                                       String editAPIPath)
    {
        softAssert = new SoftAssert();
        loginWithEndUser(username, password);
        String response = null;
        if(username.equals(supplierTypeUserName) && showAPIPath.contains("action")){
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("action")) {
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("issue")) {
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("dispute")) {
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        jsonObject =hitShowAction(apiPath,headers);

        String payload = tableListDataPayload;
        validator = executor.post(hostUrl,tableListDataAPIPath + createdEntityId,headers, payload);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        softAssert.assertTrue((Integer) jsonObject.get("filteredCount") == 1,
                "Entity is not newly Created");

        validator = executor.get(hostUrl,editAPIPath + createdEntityId,headers);
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType")
                .getJSONObject("values").put("name","Six Day");

        jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType")
                .getJSONObject("values").put("id",2);

        payload = "{ \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
        validator = executor.post(hostUrl,editAPIPath,headers,payload);

        payload = tableListDataPayload;
        validator = executor.post(hostUrl,tableListDataAPIPath + createdEntityId,headers, payload);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        softAssert.assertTrue((Integer) jsonObject.get("filteredCount") == 2,
                "Entity is not updated Successfully");

        List<HashMap<String,String>> historyColumnValues = allColumnValuesFromList(jsonObject.toString(),"history");
        validator = executor.get(hostUrl,historyColumnValues.get(0).get("history"),headers);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        softAssert.assertTrue(jsonObject.has("value"),
                "History does not key value it means it is not updated");
        softAssert.assertTrue(jsonObject.getJSONArray("value").toString().contains("Six Day"),
                "History is not getting Updated");
        softAssert.assertAll();
    }

    // C46371, C46358
    //@Test(dataProvider = "weekTypeInternationalisation")
    public void weekTypeInternationalisation(String username,String password,String grupAndLangAPIPath,String weekTypePayload,String weekTypePayloadReplacement,int listId,String filterDataAPI) throws Exception {
        softAssert = new SoftAssert();
        loginWithClientAdminUser();
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();
        validator = executor.get(hostUrl,grupAndLangAPIPath,headers);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        int fieldArrayLength = jsonObject.getJSONArray("childGroups").length();
        for(int i=0;i<fieldArrayLength;i++){
            if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").toString().equals("Metadata")){
                int fieldLabelsLength = jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
                for(int j =0;j<fieldLabelsLength;j++){
                    if(jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("name").equals("Week Type")){
                        jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",weekTypePayloadReplacement);
                        break;
                    }
                }
            }
        }
        String payload = jsonObject.toString();

        validator = executor.post(hostUrl,"/fieldlabel/update",headers,payload);

        loginWithEndUser(username,password);

        UpdateAccount updateAccount = new UpdateAccount();
        Map<String,String> params = generateFormDataMap("1000");
        Integer statusCode = updateAccount.hitUpdateAccount(params);
        softAssert.assertTrue(statusCode == 302,
                "Update API is not updating the parameters");

        ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
        HttpResponse response = listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listId);

        String listdata = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "API Status Code didn't Matched");

        jsonObject = new JSONObject(listdata);

        softAssert.assertTrue(getAllColumnNames(jsonObject).contains(weekTypePayloadReplacement),
                "Week Type Option is not changed to other Language");

       params = generateFormDataMap("1");
       statusCode = updateAccount.hitUpdateAccount(params);
       softAssert.assertTrue(statusCode == 302,
                "Update API is not updating the parameters");

        listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
        response = listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listId);

        listdata = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
        softAssert.assertTrue(response.getStatusLine().getStatusCode() == 200,
                "API Status Code didn't Matched");

        jsonObject = new JSONObject(listdata);

//        softAssert.assertFalse(getAllColumnNames(jsonObject).contains("Неделю Typeo"), "Other language instead of English is still present");

        // Renaming Week Types From Client Admin
        loginWithClientAdminUser();
        headers = WeekTimeForEntities.getHeaders();
        validator = executor.get(hostUrl,"/fieldlabel/findLabelsByGroupIdAndLanguageId/1/2132",headers);

        String[] newWeekTypeOptions = {"5 Day","6 Day","7 Day"};
        List<String> weekTypeOptions = new LinkedList<>();
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        fieldArrayLength = jsonObject.getJSONArray("childGroups").length();
        for(int i=0;i<fieldArrayLength;i++){
            if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").equals("Week Type")) {
                int fieldLabelsLength = jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
                for (int j = 0; j < fieldLabelsLength; j++) {
                    weekTypeOptions.add((String) jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("clientFieldName"));
                    jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",newWeekTypeOptions[j]);
                }
            }
        }

        payload = jsonObject.toString();

        validator = executor.post(hostUrl,"/fieldlabel/update",headers,payload);

        loginWithEndUser(username,password);
        headers = WeekTimeForEntities.getHeaders();
        validator = executor.post(hostUrl,filterDataAPI,headers, "{}");
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        List<String> filters = ListRendererFilterDataHelper.getAllFilterNames(jsonObject.toString());
        softAssert.assertTrue(filters.contains("weektypes"), "Week Type filter is not present");
        List<Map<String,String>> weekTypeFilterOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(jsonObject.toString(),"weektypes");

        Object[] weekTypeFromAPIResponse = weekTypeFilterOptions.stream().map(m->m.get("name")).toArray();

        softAssert.assertTrue(newWeekTypeOptions.length == weekTypeFromAPIResponse.length,
                "Week Type Options are not same in count");

        if(newWeekTypeOptions.length == weekTypeFromAPIResponse.length) {
            for (int i = 0; i < weekTypeFromAPIResponse.length; i++) {
                softAssert.assertTrue(newWeekTypeOptions[i].toString().equals(weekTypeFromAPIResponse[i].toString()),
                        "Week Type is not Updated");
            }
        }

        // Resetting Week Type in Client Admin
        loginWithClientAdminUser();
        headers = WeekTimeForEntities.getHeaders();
        validator = executor.get(hostUrl,"/fieldlabel/findLabelsByGroupIdAndLanguageId/1/2132",headers);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        fieldArrayLength = jsonObject.getJSONArray("childGroups").length();
        for(int i=0;i<fieldArrayLength;i++){
            if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").equals("Week Type")) {
                int fieldLabelsLength = jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
                for (int j = 0; j < fieldLabelsLength; j++) {
                    weekTypeOptions.add((String) jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("clientFieldName"));
                    jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",weekTypeOptions.get(j));
                }
            }
        }

        payload = jsonObject.toString();

        validator = executor.post(hostUrl,"/fieldlabel/update",headers,payload);

        // Resetting language change
        validator = executor.get(hostUrl,grupAndLangAPIPath,headers);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        fieldArrayLength = jsonObject.getJSONArray("childGroups").length();
        for(int i=0;i<fieldArrayLength;i++){
            if(jsonObject.getJSONArray("childGroups").getJSONObject(i).get("name").toString().equals("Metadata")){
                int fieldLabelsLength = jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").length();
                for(int j =0;j<fieldLabelsLength;j++){
                    if(jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).get("name").equals("Week Type")){
                        jsonObject.getJSONArray("childGroups").getJSONObject(i).getJSONArray("fieldLabels").getJSONObject(j).put("clientFieldName",weekTypePayload);
                        break;
                    }
                }
            }
        }
        payload = jsonObject.toString();

        validator = executor.post(hostUrl,"/fieldlabel/update",headers,payload);

        softAssert.assertAll();
    }

    // C46355
    //@Test(dataProvider = "CheckEntityWeekTypeData")
    public void verifyWeekTypeIsHiddenByDefault(String username,String password,String filePath,String fileName,String extraFieldFilePath,String extraFieldFileName, String session,String showAPIPath){
        softAssert = new SoftAssert();
        String response = null;

        loginWithEndUser(username, password);


        if(username.equals(supplierTypeUserName) && showAPIPath.contains("action")){
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("action")) {
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("issue")) {
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }
        else if(showAPIPath.contains("dispute")) {
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, session, false);
        }

        int createdEntityId = CreateEntity.getNewEntityId(response);
        String apiPath = showAPIPath + createdEntityId;
        HashMap<String, String> headers = WeekTimeForEntities.getHeaders();

        jsonObject =hitShowAction(apiPath,headers);
        softAssert.assertTrue(JSONUtility.checkKey(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType"),"values"),
                "Week Type is not present");

        if(username.equals(supplierTypeUserName) && showAPIPath.contains("action")){
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, "supplier action flow 1", false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, "supplier issue flow 1", false);
        }
        else if(username.equals(supplierTypeUserName) && showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, "supplier dispute flow 1", false);
        }
        else if(showAPIPath.contains("action")) {
            response = Action.createAction(filePath, fileName, extraFieldFilePath, extraFieldFileName, "flow 1", false);
        }
        else if(showAPIPath.contains("issue")){
            response = Issue.createIssue(filePath, fileName, extraFieldFilePath, extraFieldFileName, "issue and dispute flow 1", false);
        }
        else if(showAPIPath.contains("dispute")){
            response = Dispute.createDispute(filePath, fileName, extraFieldFilePath, extraFieldFileName, "issue and dispute flow 1", false);
        }

        createdEntityId = CreateEntity.getNewEntityId(response);
        apiPath = showAPIPath + createdEntityId;

        jsonObject =hitShowAction(apiPath,headers);

        softAssert.assertFalse(JSONUtility.checkKey(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("weekType"),"values"),
                "Week Type is present");

        softAssert.assertAll();
    }

    @DataProvider(name = "checkWeekTypeFilterData")
    public Object[][] WeekTypeFilterData(){

        return  new Object[][]{
                {
                        endUserName,endUserPassword,
                        "/listRenderer/list/9/filterData",
                        "/listRenderer/list/9/listdata",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}"
                } ,

                {
                        endUserName,endUserPassword,"/listRenderer/list/8/filterData",
                        "/listRenderer/list/8/listdata",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}"
                },

                {
                        endUserName,endUserPassword,"/listRenderer/list/286/filterData",
                        "/listRenderer/list/286/listdata",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}"
                },
                {
                        endUserName,endUserPassword,"/listRenderer/list/286/filterData",
                        "/listRenderer/list/279/listdata",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}"
                },
                { supplierTypeUserName,supplierTypeUserPassword,
                        "/listRenderer/list/9/filterData",
                        "/listRenderer/list/9/listdata",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14487,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":178,\"columnQueryName\":\"id\"},{\"columnId\":179,\"columnQueryName\":\"name\"},{\"columnId\":180,\"columnQueryName\":\"relation\"},{\"columnId\":181,\"columnQueryName\":\"contract\"},{\"columnId\":182,\"columnQueryName\":\"managementtype\"},{\"columnId\":18310,\"columnQueryName\":\"dependententity\"},{\"columnId\":184,\"columnQueryName\":\"plannedcompetitiondate\"},{\"columnId\":18335,\"columnQueryName\":\"weektype\"}]}"
                } ,
                { supplierTypeUserName,supplierTypeUserPassword,"/listRenderer/list/8/filterData",
                        "/listRenderer/list/8/listdata",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1552\",\"name\":\"Approved\"},{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"1553\",\"name\":\"Assigned\"},{\"id\":\"1557\",\"name\":\"Closed\"},{\"id\":\"1555\",\"name\":\"Dispute Resolved\"},{\"id\":\"1554\",\"name\":\"Escalated\"},{\"id\":\"1551\",\"name\":\"Newly Created\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1556\",\"name\":\"Rejected\"},{\"id\":\"1691\",\"name\":\"Active\"},{\"id\":\"1689\",\"name\":\"Approved\"},{\"id\":\"1690\",\"name\":\"Awaiting Approval\"},{\"id\":\"1688\",\"name\":\"Dispute Resolved\"},{\"id\":\"1687\",\"name\":\"Escalated To Dispute\"},{\"id\":\"1685\",\"name\":\"Newly Created\"},{\"id\":\"1686\",\"name\":\"Rejected\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14486,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":155,\"columnQueryName\":\"id\"},{\"columnId\":156,\"columnQueryName\":\"name\"},{\"columnId\":157,\"columnQueryName\":\"relation\"},{\"columnId\":158,\"columnQueryName\":\"contract\"},{\"columnId\":159,\"columnQueryName\":\"managementtype\"},{\"columnId\":18312,\"columnQueryName\":\"dependententity\"},{\"columnId\":161,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18334,\"columnQueryName\":\"weektype\"}]}"
                },
                { supplierTypeUserName,supplierTypeUserPassword,"/listRenderer/list/286/filterData",
                        "/listRenderer/list/286/listdata",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2\",\"name\":\"Six Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"},{\"id\":\"4\",\"name\":\"Overdue\"},{\"id\":\"1715\",\"name\":\"Agreement Approved\"},{\"id\":\"1716\",\"name\":\"Agreement Rejected\"},{\"id\":\"1706\",\"name\":\"Approved\"},{\"id\":\"1722\",\"name\":\"Awaiting Approval on Settlement Agreement\"},{\"id\":\"1728\",\"name\":\"Awaiting Finance Review\"},{\"id\":\"1720\",\"name\":\"Awaiting Legal Review\"},{\"id\":\"1719\",\"name\":\"Claim Acknowledged\"},{\"id\":\"1727\",\"name\":\"Claim Received\"},{\"id\":\"1726\",\"name\":\"Client Signature Received\"},{\"id\":\"1705\",\"name\":\"Dispute Acknowledged\"},{\"id\":\"1707\",\"name\":\"Dispute Assigned\"},{\"id\":\"1718\",\"name\":\"Dispute Logs Updated\"},{\"id\":\"1708\",\"name\":\"Dispute Resolved\"},{\"id\":\"1711\",\"name\":\"Escalated to Dispute\"},{\"id\":\"1712\",\"name\":\"Impact Analysis Submitted\"},{\"id\":\"1721\",\"name\":\"Legal Review Complete\"},{\"id\":\"1704\",\"name\":\"Newly Created\"},{\"id\":\"1717\",\"name\":\"Outcome Received\"},{\"id\":\"1709\",\"name\":\"Rejected\"},{\"id\":\"1710\",\"name\":\"Resubmitted\"},{\"id\":\"1723\",\"name\":\"Settlement Agreement Approved\"},{\"id\":\"1713\",\"name\":\"Settlement Agreement Created\"},{\"id\":\"1714\",\"name\":\"Settlement Agreement Negotiated\"},{\"id\":\"1724\",\"name\":\"Settlement Agreement Rejected\"},{\"id\":\"1725\",\"name\":\"Supplier Signature Received\"}]}},\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"3\",\"name\":\"Seven Day\"}]}}}},\"selectedColumns\":[{\"columnId\":14491,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":13174,\"columnQueryName\":\"id\"},{\"columnId\":13175,\"columnQueryName\":\"name\"},{\"columnId\":13176,\"columnQueryName\":\"relation\"},{\"columnId\":13177,\"columnQueryName\":\"contract\"},{\"columnId\":13178,\"columnQueryName\":\"managementtype\"},{\"columnId\":18314,\"columnQueryName\":\"dependententity\"},{\"columnId\":13179,\"columnQueryName\":\"plannedcompletiondate\"},{\"columnId\":18336,\"columnQueryName\":\"weektype\"}]}"
                },
        };
    }

    @DataProvider(name = "CheckWeekTypeInDownloadedFileData")
    public Object[][] getDataForWeekTypeInDownloadedFile(){

        return new Object[][] {
                {
                        endUserName,endUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}" ,
                        18,"actions",9
                },
                {
                        endUserName,endUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}",
                        17,"issues",8
                },
                {
                        endUserName,endUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}",
                        28,"disputes",286
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}",
                        18,"actions",9
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}",
                        17,"issues",8
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "\"83\":{\"filterId\":\"83\",\"filterName\":\"weektypes\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Five Day\"}]}}",
                        28,"disputes",286
                },

        };
    }

    @DataProvider(name = "AuditLogAndFieldHistoryData")
    public Object[][] getChangesInAuditLogAndFieldHistoryData(){
        return new Object[][]{
                {
                        endUserName,endUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "default", "/actionitemmgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/18/",
                        "/actionitemmgmts/edit/"
                },
                {
                        endUserName,endUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "issue week type", "/issuemgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/17/",
                        "/issuemgmts/edit/"
                },
                {
                        endUserName,endUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "dispute week type", "/disputemgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/28/",
                        "/disputemgmts/edit/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier action default flow", "/actionitemmgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":18,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/18/",
                        "/actionitemmgmts/edit/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier issue default flow", "/issuemgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":17,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/17/",
                        "/issuemgmts/edit/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier dispute default flow", "/disputemgmts/show/",
                        "{\"filterMap\":{\"entityTypeId\":28,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}",
                        "/listRenderer/list/61/tablistdata/28/",
                        "/disputemgmts/edit/"
                },
        };
    }


    //@DataProvider(name = "weekTypeInternationalisation")
    /*public Object[][] getWeekTypeInternationalisationData(){
        return new Object[][]{
                {
                    endUserName,endUserPassword,
                    "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/21",
                    "Week Type",
                    "Неделю Typeo",
                        9, "/listRenderer/list/9/filterData"
                },
                {
                        endUserName,endUserPassword,
                       "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/31",
                        "Week Type",
                        "Неделю Typeo",
                        8, "/listRenderer/list/8/filterData"
                },
                {
                        endUserName,endUserPassword,
                       "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/908",
                        "Week Type",
                        "Неделю Typeo",
                        286, "/listRenderer/list/286/filterData"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/21",
                        "Week Type",
                        "Неделю Typeo",
                        9, "/listRenderer/list/9/filterData"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/31",
                        "Week Type",
                        "Неделю Typeo",
                        8, "/listRenderer/list/8/filterData"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,
                        "/fieldlabel/findLabelsByGroupIdAndLanguageId/1000/908",
                        "Week Type",
                        "Неделю Typeo",
                        286, "/listRenderer/list/286/filterData"
                },
        };

    }*/

    @DataProvider(name = "CheckEntityWeekTypeDataWithActualTime")
    public Object[][] dataForCheckEntityWeekTypeWithActualTime(){
        return new Object[][]{
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "client action flow with actual time", "/actionitemmgmts/show/"
                },
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "client issue flow with actual time", "/issuemgmts/show/"
                },
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "client dispute flow with actual time", "/disputemgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier action flow with actual time", "/actionitemmgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier issue flow with actual time", "/issuemgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier dispute default flow", "/disputemgmts/show/"
                },

        };
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

            if(weekType.equals("Five Day")){
                notToIncludeDays = weekendCount5Days(jodastarttime,jodaendtime);
                return diffDays - ((float) notToIncludeDays);
            }
            else if(weekType.equals("Six Day")){
                notToIncludeDays = weekendCount6Days(jodastarttime,jodaendtime);
                return diffDays - ((float) notToIncludeDays);
            }
            else if(weekType.equals("Seven Day")){
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

    @DataProvider(name = "CheckEntityWeekTypeData")
    public Object[][] dataForCheckEntityWeekType(){
        return new Object[][]{
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "default", "/actionitemmgmts/show/"
                },
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "issue week type", "/issuemgmts/show/"
                },
                {
                        endUserName,endUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "dispute week type", "/disputemgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier action default flow", "/actionitemmgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier issue default flow", "/issuemgmts/show/"
                },
                {
                        supplierTypeUserName,supplierTypeUserPassword,ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("WorkTimeConfigFileName"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFilePath"),
                        ConfigureConstantFields.getConstantFieldsProperty("ExtraFieldWorkTimeConfigFileName"),
                        "supplier dispute default flow", "/disputemgmts/show/"
                },

        };
    }

    private Boolean loginWithEndUser(String username,String password) {
        logger.info("Logging with UserName [{}] and Password [{}]", username, password);
        Check checkObj = new Check();
        checkObj.hitCheck(username, password);

        return (Check.getAuthorization() != null);
    }

    public JSONObject hitShowAction(String apiPath, Map<String,String> headers){
        validator=executor.get(hostUrl,apiPath,headers);
        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");
        jsonObject = new JSONObject(validator.getResponse().getResponseBody());

        return jsonObject;
    }

    public List<HashMap<String ,String>> allColumnValuesFromList(String response, String... columnNames){
        List<HashMap<String,String>> jsonValues = new LinkedList<>();
        HashMap<String,String> addColumnValues = null;
        JSONObject jsonObj = new JSONObject(response);
        Integer columnId;

        for (int i = 0; i < jsonObj.getJSONArray("data").length(); i++) {
            addColumnValues = new LinkedHashMap<>();
            for (int j = 0; j < columnNames.length; j++) {
                columnId=ListDataHelper.getColumnIdFromColumnName(response,columnNames[j]);
                addColumnValues.put(columnNames[j], jsonObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString());
            }
            jsonValues.add(addColumnValues);
        }
        return jsonValues;
    }

    public APIValidator performActionsOnCreatedEntity(String entity,String query,Map<String,String> headers){
        String payload = null;
        if(entity.contains("action")) {
            validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");
        }
        else if (entity.contains("issue-work1")){

            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("resolutionRemarks").put("values","Test API");
            payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
            validator = executor.post(hostUrl, query, headers,
                    payload);
        }
        else if (entity.contains("issue-work2")){
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("processAreaImpacted").put("values","Test API");
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("actionTaken").put("values","Test API");
            payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

            validator = executor.post(hostUrl, query, WeekTimeForEntities.getHeaders(),
                    payload);
        }
        else{
            validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");
        }
        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");

        return validator;
    }

    public APIValidator performActionsOnCreatedEntityWithActualDate(String entity,String actualDate,String query,Map<String,String> headers){
        String payload = null;
        if(entity.contains("action")) {
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values",
                    actualDate);
            validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");
        }
        else if (entity.contains("issue-work1")){

            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values",
                    actualDate);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("resolutionRemarks").put("values","Test API");
            payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";
            validator = executor.post(hostUrl, query, headers,
                    payload);
        }
        else if (entity.contains("issue-work2")){
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values",
                    actualDate);
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("processAreaImpacted").put("values","Test API");
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("actionTaken").put("values","Test API");
            payload = " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}";

            validator = executor.post(hostUrl, query, WeekTimeForEntities.getHeaders(),
                    payload);
        }
        else{
            jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("actualDate").put("values",
                    actualDate);
            validator = executor.post(hostUrl, query, headers,
                    " { \"body\" : " + "{\"data\" : " + jsonObject.getJSONObject("body").getJSONObject("data").toString() + "}}");
        }

        validator.validateResponseCode(200,customAssert);

        softAssert.assertTrue(JSONUtility.validjson(validator.getResponse().getResponseBody()),
                "Not a valid Json Response");

        return validator;
    }


    public APIValidator clearWeekTypeFilter(String listDataAPIUrl,Map<String,String> headers){
        validator = executor.post(hostUrl,listDataAPIUrl + "?isFirstCall=true",headers, "{\"filterMap\":{}}");
        return validator;
    }

    public void hitFilterWithDifferentWeekDays(String listDataAPIUrl,String payload,Map<String,String> headers,String weekDays){
        validator = executor.post(hostUrl,listDataAPIUrl,headers, payload);

        jsonObject = new JSONObject(validator.getResponse().getResponseBody());
        List<HashMap<String, String>> allColumnsFromListData = allColumnValuesFromList(jsonObject.toString(),"weektype");

        List<String> weekTypes = new LinkedList<>();
        for(HashMap<String,String> h : allColumnsFromListData){
            weekTypes.add(h.get("weektype"));
        }

        softAssert.assertTrue(jsonObject.getJSONArray("data").length() == weekTypes.size(),
                "Not all Filtered Data has week type column");

        weekTypes =weekTypes.stream().filter(m->m.equals(weekDays)).collect(Collectors.toList());

        softAssert.assertTrue(jsonObject.getJSONArray("data").length() == weekTypes.size(),
                "Data is not getting filtered properly");
    }

    private Map<String, String> generateFormDataMap(String lanugageId) {
        Map<String, String> formDataMap = new LinkedHashMap<>();
        formDataMap.put("firstName","Akshay");
        formDataMap.put("lastName","User");
        formDataMap.put("language.id",lanugageId);
        formDataMap.put("timeZone.id","8");
        formDataMap.put("id","1047");

        return formDataMap;
    }

    private Boolean loginWithClientAdminUser() {
        logger.info("Logging with Client Admin UserName [{}] and Password [{}]", clientAdminUserName, clientAdminUserPassword);
        Check checkObj = new Check();
        checkObj.hitCheck(clientAdminUserName, clientAdminUserPassword);

        return (Check.getAuthorization() != null);
    }

    public List<Object> getAllColumnNames(JSONObject jsonObject1){
        List<Object> allColumns = new LinkedList<>();
        for(int i=0;i<jsonObject1.getJSONArray("columns").length();i++){
            allColumns.add(jsonObject1.getJSONArray("columns").getJSONObject(i).get("name"));
        }
        return allColumns;
    }

}
