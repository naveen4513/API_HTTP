package com.sirionlabs.test.bulkEdit;

import com.sirionlabs.api.bulkedit.BulkeditCreate;
import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestBulkEdit {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkEdit.class);
    private static String configAutoExtractionFilePath;
    private static String configAutoExtractionFileName;
    private static int listDataOffset;
    private static int maxRecordsForListData;
    private static int maxRecordsForBulkEdit;
    private static int maxRecordsForShow;
    private static String schedulerWaitTimeout;
    private static String schedulerPollingTime;
    private static Boolean taskKilledFlag;
    private static Boolean waitForScheduler;
    private static Boolean checkShowPageIsBlocked;

    private static String text;
    private static String date;
    private static String stakeholder;
    private static String number;
    private static String flowsToTest;
    private static String dependentFieldsFlows;
    private static String parentChildFlows;

    @BeforeTest
    public void beforeTest(){
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("bulkEditDataConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("bulkEditDataConfigFileName");
        listDataOffset = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "listdataoffset"));
        maxRecordsForListData = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "maxrecordsforlistdata"));
        maxRecordsForBulkEdit = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "maxrecordsforbulkedit"));
        maxRecordsForShow = Integer.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "maxrecordsforshow"));
        schedulerWaitTimeout = String.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "schedulerwaittimeout"));
        schedulerPollingTime = String.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "schedulerpollingtime"));
        waitForScheduler = Boolean.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "waitforscheduler"));
        taskKilledFlag = Boolean.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "taskkilledflag"));
        checkShowPageIsBlocked = Boolean.valueOf(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "checkshowpageisblocked"));
        text = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"default field values", "text");
        date = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"default field values", "date");
        stakeholder = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"default field values", "stakeholder");
        number = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,"default field values", "number");
        flowsToTest = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "flowstotest");
        dependentFieldsFlows = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "dependentfieldsflows");
        parentChildFlows = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "parentchildflow");
    }

    @DataProvider(name = "getDataForBulkEdit")
    public Object[][] getDataForBulkEdit(){
        List<List<Object>> allDataToBeTested = new LinkedList<>();
        List<Object> dataToBeTested;
        String[] allFlowsToBeTested = flowsToTest.split(",");
        for(int i=0;i<allFlowsToBeTested.length;i++){
            dataToBeTested = new LinkedList<>();
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"entityname"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"fieldtoupdate"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"fieldtoupdatevalue"));
            allDataToBeTested.add(dataToBeTested);
        }

        int i=0;
        Object[][] groupArray = new Object[allFlowsToBeTested.length][3];
        for(List<Object> data : allDataToBeTested){
            for(int j=0;j<data.size();j++){
                groupArray[i][j] = data.get(j);
            }
            i++;
        }
        return groupArray;
    }

    @DataProvider(name = "getDataForBulkEditParentChild")
    public Object[][] getDataForBulkEditParentChild(){
        List<List<Object>> allDataToBeTested = new LinkedList<>();
        List<Object> dataToBeTested;
        String[] allFlowsToBeTested = parentChildFlows.split(",");
        for(int i=0;i<allFlowsToBeTested.length;i++){
            dataToBeTested = new LinkedList<>();
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"entityname"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"fieldtoupdate"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"fieldtoupdatevalue"));
            allDataToBeTested.add(dataToBeTested);
        }

        int i=0;
        Object[][] groupArray = new Object[allFlowsToBeTested.length][3];
        for(List<Object> data : allDataToBeTested){
            for(int j=0;j<data.size();j++){
                groupArray[i][j] = data.get(j);
            }
            i++;
        }
        return groupArray;
    }

    @DataProvider(name = "verifyDependentFields")
    public Object[][] verifyDependentFields(){
        List<List<Object>> allDataToBeTested = new LinkedList<>();
        List<Object> dataToBeTested;
        String[] allFlowsToBeTested = dependentFieldsFlows.split(",");
        for(int i=0;i<allFlowsToBeTested.length;i++){
            dataToBeTested = new LinkedList<>();
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"entityid"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"getfieldprovisingdata"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"fieldname"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"dependentfield1"));
            dataToBeTested.add(ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName,allFlowsToBeTested[i] ,"dependenttfield2"));
            allDataToBeTested.add(dataToBeTested);
        }

        int i=0;
        Object[][] groupArray = new Object[allFlowsToBeTested.length][allDataToBeTested.stream().findAny().get().size()];
        for(List<Object> data : allDataToBeTested){
            for(int j=0;j<data.size();j++){
                groupArray[i][j] = data.get(j);
            }
            i++;
        }
        return groupArray;
    }

   // @Test(dataProvider = "getDataForBulkEdit")
    public void TestBulkEdit(String entityName,String fieldToUpdate,String fieldToUpdateValue) throws ConfigurationException, ExecutionException, InterruptedException {
        CustomAssert customAssert = new CustomAssert();
        Check check = new Check();
        HttpResponse loginResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Status Code is not valid");
        bulkEntityEdit(entityName,listDataOffset,maxRecordsForListData,taskKilledFlag,maxRecordsForBulkEdit,fieldToUpdate,fieldToUpdateValue,waitForScheduler,maxRecordsForShow,schedulerWaitTimeout,schedulerPollingTime,checkShowPageIsBlocked,customAssert);
        customAssert.assertAll();
    }

    @Test(dataProvider = "getDataForBulkEditParentChild")
    public void TestBulkEditForDependentFields(String entityName,String fieldToUpdate,String fieldToUpdateValue) throws ConfigurationException, ExecutionException, InterruptedException {
        CustomAssert customAssert = new CustomAssert();
        Check check = new Check();
        HttpResponse loginResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Status Code is not valid");
        bulkEntityEditForDependentFields(entityName,listDataOffset,maxRecordsForListData,taskKilledFlag,maxRecordsForBulkEdit,fieldToUpdate,fieldToUpdateValue,waitForScheduler,maxRecordsForShow,schedulerWaitTimeout,schedulerPollingTime,checkShowPageIsBlocked,customAssert);
        customAssert.assertAll();
    }

    //@Test
    public void TestBulkEditPermissionFromClientAdmin(){
        CustomAssert customAssert = new CustomAssert();
        BulkOperationsHelper bulkOperationsHelper = new BulkOperationsHelper();
        int contractBulkEditCheckboxId = 521;
        int endUserId = 1183;
        boolean isBulkEditRemoved = bulkOperationsHelper.removeBulkEditPermission(contractBulkEditCheckboxId,endUserId);

        String entityName = "contracts";
        if(isBulkEditRemoved == true){
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        // Get List Data From Entity Show Page
        List<Map<Integer, Map<String, String>>> listData = getEntityListData(entityTypeId,listId,listDataOffset,maxRecordsForListData);

        // Filter Out List Data which are already locked from bulk edit
        listData = filterLockedData(listData,entityName);

        // Execute ListRendererDefaultUserListMetaData API to get the default column ids for bulk checkbox to extract show page ids for records to be updated
        ListRendererDefaultUserListMetaData listMetadatObj = hitListRendererDefaultUserListMetaData(listId,entityName);

        // Records to be updated show page Ids
        List<String> recordsToEdit = getEntityShowPageIds(listId,listData,maxRecordsForBulkEdit,listMetadatObj,entityName);

        // Bulk Create Edit JSON with records ids to update
        String createJsonStr = bulkEditCreate(recordsToEdit,entityTypeId);

        JSONObject jsonObject = new JSONObject(createJsonStr);
        customAssert.assertTrue(jsonObject.getJSONObject("header").getJSONObject("response")
                .get("errorMessage").toString().equals("Either you do not have the required permissions or requested page does not exist anymore.")
                ,"Permission is not restricted from Admin");
        }
        else{
            throw new SkipException("Permission for bulk Edit is not removed for entity  " + entityName + " and End User Id " + endUserId);
        }

        boolean isBulkEditAppended = bulkOperationsHelper.appendBulkEditPermission(contractBulkEditCheckboxId,endUserId);
        if(isBulkEditAppended == false){
            logger.warn("Please Append bulk edit permission for entity  " + entityName + " and End User Id " + endUserId + "manually");
        }
        customAssert.assertAll();
    }

    //@Test()
    public void TestGlobalValuesInBulkEditDropDown() throws IOException {
        CustomAssert customAssert = new CustomAssert();
        Check check = new Check();

        // Login to client Admin and get Functions Global List
        HttpResponse loginResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"), ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Status Code is not valid");

        String uri = "/mastercontracttypes/list";
        String getFunctionsFromAdmin = getFunctionsListFromAdmin(uri);
        Document document =Jsoup.parse(getFunctionsFromAdmin);
        List<String> expectedList = document.select("a .listTdDivFont").stream().map(m->m.text()).collect(Collectors.toList());

        // Login to End-User and get functions from bulk edit page
        loginResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Status Code is not valid");

        String entityName = "service levels";
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        // Get List Data From Entity Show Page
        List<Map<Integer, Map<String, String>>> listData = getEntityListData(entityTypeId,listId,listDataOffset,maxRecordsForListData);

        // Filter Out List Data which are already locked from bulk edit
        listData = filterLockedData(listData,entityName);

        // Execute ListRendererDefaultUserListMetaData API to get the default column ids for bulk checkbox to extract show page ids for records to be updated
        ListRendererDefaultUserListMetaData listMetadatObj = hitListRendererDefaultUserListMetaData(listId,entityName);

        // Records to be updated show page Ids
        List<String> recordsToEdit = getEntityShowPageIds(listId,listData,maxRecordsForBulkEdit,listMetadatObj,entityName);

        // Bulk Create Edit JSON with records ids to update
        String createJsonStr = bulkEditCreate(recordsToEdit,entityTypeId);

        JSONObject createBulkEditJsonStr = new JSONObject(createJsonStr);

        int globalFieldsLength =0;
        try{
        globalFieldsLength = createBulkEditJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("functions").getJSONObject("options").getJSONArray("data").length();
            List<String> actualValues = new LinkedList<>();
            for(int i=0;i<globalFieldsLength;i++){
                actualValues.add(createBulkEditJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("functions").getJSONObject("options").getJSONArray("data").getJSONObject(i).get("name").toString());
            }
            customAssert.assertTrue(expectedList.stream().sorted().collect(Collectors.toList()).equals(actualValues.stream().sorted().collect(Collectors.toList())),"Global functions from client admin is not coming from admin to bulk edit global functions values");
        }
        catch (Exception e){
            logger.warn("Functions and Services are not present to edit for entity " + entityName);
        }
        customAssert.assertAll();
    }

    //@Test(dataProvider = "verifyDependentFields")
    public void verifyDependentFieldInClientAdmin(String entityId,String getFieldProvisioningUrl,String fieldName,String dependentField1,String dependentField2) throws IOException {
        CustomAssert customAssert = new CustomAssert();
        Check check = new Check();
        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"), ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
        HttpGet httpGet = new HttpGet(getFieldProvisioningUrl + entityId);
        HttpResponse httpResponse = APIUtils.getRequest(httpGet);
        String fieldProvisioningResponseStr = EntityUtils.toString(httpResponse.getEntity());
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"Response Code is invalid");
        customAssert.assertTrue(JSONUtility.validjson(fieldProvisioningResponseStr),"Not a valid Json");

        JSONObject jsonObject = new JSONObject(fieldProvisioningResponseStr);
        int jsonArrayLength = jsonObject.getJSONArray("data").length();
        for(int i =0; i<jsonArrayLength;i++){
            if(jsonObject.getJSONArray("data").getJSONObject(i).get(fieldName).toString().equals(dependentField1) || jsonObject.getJSONArray("data").getJSONObject(i).get(fieldName).toString().equals(dependentField2)) {
                jsonObject.getJSONArray("data").getJSONObject(i).put("bulkEdit", false);
            }
        }

        String payload = jsonObject.getJSONArray("data").toString();
        HttpPost httpPost = new HttpPost(getFieldProvisioningUrl + entityId);
        httpPost.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        httpPost.addHeader("Content-Type","application/json; charset=UTF-8");
        httpPost.addHeader("Accept-Encoding","gzip, deflate");
        httpResponse = APIUtils.postRequest(httpPost,payload);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"Response Code is invalid");

        httpGet = new HttpGet(getFieldProvisioningUrl + entityId);
        httpResponse = APIUtils.getRequest(httpGet);
        fieldProvisioningResponseStr = EntityUtils.toString(httpResponse.getEntity());
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"Response Code is invalid");
        customAssert.assertTrue(JSONUtility.validjson(fieldProvisioningResponseStr),"Not a valid Json");

        jsonObject = new JSONObject(fieldProvisioningResponseStr);

        List<JSONObject> allDependentFieldProperties = getDependentFieldsJSON(jsonObject,fieldName,dependentField1,dependentField2);

        customAssert.assertTrue( allDependentFieldProperties.stream().map(m->(boolean)m.get("bulkEdit")).findAny().get() == false,"Values of both dependent fields are not as expected");
        customAssert.assertTrue(allDependentFieldProperties.stream().map(m->m.get("bulkEdit")).collect(Collectors.toSet()).size() ==1,"Values of both dependent fields are not same");

        jsonArrayLength = jsonObject.getJSONArray("data").length();
        for(int i =0; i<jsonArrayLength;i++){
            if(jsonObject.getJSONArray("data").getJSONObject(i).get(fieldName).toString().equals(dependentField1) || jsonObject.getJSONArray("data").getJSONObject(i).get(fieldName).toString().equals(dependentField2)) {
                jsonObject.getJSONArray("data").getJSONObject(i).put("bulkEdit", true);
            }
        }

        payload = jsonObject.getJSONArray("data").toString();
        httpPost = new HttpPost(getFieldProvisioningUrl + entityId);
        httpPost.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        httpPost.addHeader("Content-Type","application/json; charset=UTF-8");
        httpPost.addHeader("Accept-Encoding","gzip, deflate");
        httpResponse = APIUtils.postRequest(httpPost,payload);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"Response Code is invalid");

        httpGet = new HttpGet(getFieldProvisioningUrl + entityId);
        httpResponse = APIUtils.getRequest(httpGet);
        fieldProvisioningResponseStr = EntityUtils.toString(httpResponse.getEntity());
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,"Response Code is invalid");
        customAssert.assertTrue(JSONUtility.validjson(fieldProvisioningResponseStr),"Not a valid Json");

        jsonObject = new JSONObject(fieldProvisioningResponseStr);

        allDependentFieldProperties = getDependentFieldsJSON(jsonObject,fieldName,dependentField1,dependentField2);

        customAssert.assertTrue( allDependentFieldProperties.stream().map(m->(boolean)m.get("bulkEdit")).findAny().get() == true,"Values of both dependent fields are not as expected");
        customAssert.assertTrue(allDependentFieldProperties.stream().map(m->m.get("bulkEdit")).collect(Collectors.toSet()).size() ==1,"Values of both dependent fields are not same");

        customAssert.assertAll();
    }

    public String getFunctionsListFromAdmin(String uri) throws IOException {
        String getFunctionsFromAdminResponseStr = null;
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Accept","application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        try {
            HttpResponse getFunctionsFromAdminResponse = APIUtils.getRequest(httpGet);
            getFunctionsFromAdminResponseStr = EntityUtils.toString(getFunctionsFromAdminResponse.getEntity());
        }
        catch (Exception e){
            logger.error("Error in get functions from admin api " + e.getStackTrace());
        }

        return getFunctionsFromAdminResponseStr;

    }

    public void bulkEntityEdit(String entityName,int listDataOffset,int maxRecordsForListData,Boolean taskKilledFlag,int maxRecordsForBulkEdit,String fieldToUpdate,String fieldToUpdateValue,Boolean waitForScheduler,int maxRecordsForShow,String schedulerWaitTimeout,String schedulerPollingTime,Boolean checkShowPageIsBlocked,CustomAssert customAssert) throws ConfigurationException, ExecutionException, InterruptedException {
        List<Integer> tasksIdBeforeBulkEdit;
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        // Get List Data From Entity Show Page
        List<Map<Integer, Map<String, String>>> listData = getEntityListData(entityTypeId,listId,listDataOffset,maxRecordsForListData);

        // Filter Out List Data which are already locked from bulk edit
        listData = filterLockedData(listData,entityName);

        // Scheduled Task Killed Flag
        boolean tasksKilled = killAllSchedulerTasks(taskKilledFlag);

        if(tasksKilled){
            tasksIdBeforeBulkEdit = null;
        }
        else {
            tasksIdBeforeBulkEdit =fetchTasksIds();
        }

        // Execute ListRendererDefaultUserListMetaData API to get the default column ids for bulk checkbox to extract show page ids for records to be updated
        ListRendererDefaultUserListMetaData listMetadatObj = hitListRendererDefaultUserListMetaData(listId,entityName);

        // Records to be updated show page Ids
        List<String> recordsToEdit = getEntityShowPageIds(listId,listData,maxRecordsForBulkEdit,listMetadatObj,entityName);

        // Get Payload of all the records before bulk edit so that it can be reverted back after verifying bulk edit
        Map<Integer, String> recordsPayloadBeforeBulkEdit = getPayloadForRecords(entityName,recordsToEdit);

        // Bulk Create Edit JSON with records ids to update
        String createJsonStr = bulkEditCreate(recordsToEdit,entityTypeId);

        // Extract Properties of the field to be updated
        List<Map<String,String>> allFieldProperties = new LinkedList<>();
        Map<String,String> fieldProperties = null;
        String[] allFieldsToUpdate = fieldToUpdate.split("::");
        if(allFieldsToUpdate.length>1){
            for(int i=0;i<allFieldsToUpdate.length;i++){
                fieldProperties = new HashMap<>();
                fieldProperties = getPropertiesOfFieldsToBeUpdated(createJsonStr,allFieldsToUpdate[i],entityName);
                allFieldProperties.add(fieldProperties);
            }
        }
        else {
            fieldProperties = new HashMap<>();
            fieldProperties = getPropertiesOfFieldsToBeUpdated(createJsonStr,fieldToUpdate,entityName);
        }

        // Payload to be needed for bulk edit with record Ids
        String payloadForBulkEdit = null;
        if(allFieldProperties.size()>1){
            List<JSONObject> fieldsToBeUpdated = new LinkedList<>();
            String[] allFieldToUpdateValues = fieldToUpdateValue.split("::");
            for(int i=0;i<allFieldProperties.size();i++){
                payloadForBulkEdit = createPayloadForBulkEdit(allFieldProperties.get(i),createJsonStr,entityTypeId,recordsToEdit.toString(),allFieldToUpdateValues[i],entityName);
                JSONObject jsonObject = new JSONObject(payloadForBulkEdit);
                fieldsToBeUpdated.add(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(allFieldProperties.get(i).get("name")));
            }
            for(JSONObject jsonObject : fieldsToBeUpdated){
                for(int i=0;i<allFieldProperties.size();i++){
                    JSONObject payload = new JSONObject(payloadForBulkEdit);
                    if(!payload.getJSONObject("body").getJSONObject("data").has(allFieldProperties.get(i).get("name"))){
                        payload.getJSONObject("body").getJSONObject("data").put(allFieldProperties.get(i).get("name"),jsonObject);
                        if(!payload.getJSONObject("body").getJSONObject("globalData").getJSONArray("fieldIds").toList().stream().map(m->m.toString()).collect(Collectors.toList()).contains(allFieldProperties.get(i).get("id"))){
                            payload.getJSONObject("body").getJSONObject("globalData").getJSONArray("fieldIds").put(Integer.valueOf(allFieldProperties.get(i).get("id")));
                            payloadForBulkEdit = payload.toString();
                            break;
                        }
                    }
                }
            }
        }
        else {
            payloadForBulkEdit = createPayloadForBulkEdit(fieldProperties, createJsonStr, entityTypeId, recordsToEdit.toString(), fieldToUpdateValue,entityName);
        }

        // Hit Bulk Edit API
        String editJsonStr = hitCreateBulkEdit(entityTypeId,payloadForBulkEdit);

        // Verify Bulk Edit Response
        boolean isRequestCompleted = validBulkEditResponse(editJsonStr,fieldProperties,entityName,customAssert);

        //Wait for Scheduler to finish Request
        if(waitForScheduler == true){
            isRequestCompleted =waitForSchedulerToFinishBulkRequest(entityTypeId,recordsToEdit,maxRecordsForShow,tasksIdBeforeBulkEdit,schedulerWaitTimeout,schedulerPollingTime,checkShowPageIsBlocked,customAssert);
        }
        else if(isRequestCompleted == false && waitForScheduler == true){
            logger.error("Failing Test as Bulk Edit Job for Flow [{}] is not completed yet and Flag " +
                    "\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True.");
            customAssert.assertTrue(false, "Failing Test as Bulk Edit Job for Flow is not completed yet and Flag \'" +
                    "FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True");
        }
        else if(isRequestCompleted == false && waitForScheduler == false){
            throw new SkipException("Bulk Edit Job  is not completed yet and Flag " +
                    "\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to False. Hence not Checking Status on Show Page.");
        }

        //Verify Changes in Show Page
        if(isRequestCompleted) {
            String[] allFields = fieldToUpdate.split("::");
            String[] allFieldsValue = fieldToUpdateValue.split("::");
            if (allFields.length > 1) {
                for (int i = 0; i < allFields.length; i++) {
                    String[] allUpdatedValues = allFieldsValue[i].split(",");
                    if (allUpdatedValues.length > 1) {
                        for (int j = 0; j < allUpdatedValues.length; j++) {
                            verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, allFieldProperties.get(i), recordsPayloadBeforeBulkEdit, allUpdatedValues[j], customAssert);
                        }
                    } else {
                        verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, allFieldProperties.get(i), recordsPayloadBeforeBulkEdit, allUpdatedValues[i], customAssert);
                    }
                }
            } else {
                String[] allUpdatedValues = fieldToUpdateValue.split(",");
                if (allUpdatedValues.length > 1) {
                    for (int j = 0; j < allUpdatedValues.length; j++) {
                        verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, fieldProperties, recordsPayloadBeforeBulkEdit, allUpdatedValues[j], customAssert);
                    }
                } else {
                    verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, fieldProperties, recordsPayloadBeforeBulkEdit, fieldToUpdateValue, customAssert);
                }
            }
        }
        else {
            throw new SkipException("Request for Bulk is not submitted successfully. Please find field label which is not updated " + fieldProperties.get("label"));
        }

        // verify email for the bulk edit
        verifyBulkEditEmail(customAssert);

        // Restore Records to original State once bulk Edit is complete
        restoreRecords(entityName,recordsPayloadBeforeBulkEdit);
    }

    public void bulkEntityEditForDependentFields(String entityName,int listDataOffset,int maxRecordsForListData,Boolean taskKilledFlag,int maxRecordsForBulkEdit,String fieldToUpdate,String fieldToUpdateValue,Boolean waitForScheduler,int maxRecordsForShow,String schedulerWaitTimeout,String schedulerPollingTime,Boolean checkShowPageIsBlocked,CustomAssert customAssert) throws ConfigurationException, ExecutionException, InterruptedException {
        List<Integer> tasksIdBeforeBulkEdit;
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        // Get List Data From Entity Show Page
        List<Map<Integer, Map<String, String>>> listData = getEntityListData(entityTypeId,listId,listDataOffset,maxRecordsForListData);

        // Filter Out List Data which are already locked from bulk edit
        listData = filterLockedData(listData,entityName);

        // Scheduled Task Killed Flag
        boolean tasksKilled = killAllSchedulerTasks(taskKilledFlag);

        if(tasksKilled){
            tasksIdBeforeBulkEdit = null;
        }
        else {
            tasksIdBeforeBulkEdit =fetchTasksIds();
        }

        // Execute ListRendererDefaultUserListMetaData API to get the default column ids for bulk checkbox to extract show page ids for records to be updated
        ListRendererDefaultUserListMetaData listMetadatObj = hitListRendererDefaultUserListMetaData(listId,entityName);

        // Get Show Page Ids of all records
        List<String> showPageId = getEntityShowPageIds(listId,listData,listData.size(),listMetadatObj,entityName);

        // Filter Out Records which are not available for bulk edit
        List<String> editableRecords = filterListDataByParentInheritance(showPageId,ConfigureConstantFields.getUrlNameForEntity(entityName),fieldToUpdate,fieldToUpdateValue);

        if(editableRecords.size() ==0){
            throw new SkipException("No Records are present for bulk edit for " + entityName + "in total filtered count of " + maxRecordsForListData + "Please increase the size of the records to be considered.");
        }

        int[] recordsNumbers = RandomNumbers.getMultipleRandomNumbersWithinRange(0,editableRecords.size(),maxRecordsForBulkEdit);

        List<String> recordsToEdit = new LinkedList<>();

        for(int i : recordsNumbers){
            recordsToEdit.add(editableRecords.get(i));
        }

        // Get Payload of all the records before bulk edit so that it can be reverted back after verifying bulk edit
        Map<Integer, String> recordsPayloadBeforeBulkEdit = getPayloadForRecords(entityName,recordsToEdit);

        // Bulk Create Edit JSON with records ids to update
        String createJsonStr = bulkEditCreate(recordsToEdit,entityTypeId);

        // Extract Properties of the field to be updated
        List<Map<String,String>> allFieldProperties = new LinkedList<>();
        Map<String,String> fieldProperties = null;
        String[] allFieldsToUpdate = fieldToUpdate.split("::");
        if(allFieldsToUpdate.length>1){
            for(int i=0;i<allFieldsToUpdate.length;i++){
                fieldProperties = new HashMap<>();
                fieldProperties = getPropertiesOfFieldsToBeUpdated(createJsonStr,allFieldsToUpdate[i],entityName);
                allFieldProperties.add(fieldProperties);
            }
        }
        else {
            fieldProperties = new HashMap<>();
            fieldProperties = getPropertiesOfFieldsToBeUpdated(createJsonStr,fieldToUpdate,entityName);
        }

        // Payload to be needed for bulk edit with record Ids
        String payloadForBulkEdit = null;
        if(allFieldProperties.size()>1){
            List<JSONObject> fieldsToBeUpdated = new LinkedList<>();
            String[] allFieldToUpdateValues = fieldToUpdateValue.split("::");
            for(int i=0;i<allFieldProperties.size();i++){
                payloadForBulkEdit = createPayloadForBulkEdit(allFieldProperties.get(i),createJsonStr,entityTypeId,recordsToEdit.toString(),allFieldToUpdateValues[i],entityName);
                JSONObject jsonObject = new JSONObject(payloadForBulkEdit);
                fieldsToBeUpdated.add(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(allFieldProperties.get(i).get("name")));
            }
            for(JSONObject jsonObject : fieldsToBeUpdated){
                for(int i=0;i<allFieldProperties.size();i++){
                    JSONObject payload = new JSONObject(payloadForBulkEdit);
                    if(!payload.getJSONObject("body").getJSONObject("data").has(allFieldProperties.get(i).get("name"))){
                        payload.getJSONObject("body").getJSONObject("data").put(allFieldProperties.get(i).get("name"),jsonObject);
                        if(!payload.getJSONObject("body").getJSONObject("globalData").getJSONArray("fieldIds").toList().stream().map(m->m.toString()).collect(Collectors.toList()).contains(allFieldProperties.get(i).get("id"))){
                            payload.getJSONObject("body").getJSONObject("globalData").getJSONArray("fieldIds").put(Integer.valueOf(allFieldProperties.get(i).get("id")));
                            payloadForBulkEdit = payload.toString();
                            break;
                        }
                    }
                }
            }
        }
        else {
            payloadForBulkEdit = createPayloadForBulkEdit(fieldProperties, createJsonStr, entityTypeId, recordsToEdit.toString(), fieldToUpdateValue,entityName);
        }

        // Adding Parent Mapping with Child
        if(allFieldProperties.size()>1){
            Map<String,String> parentProperties = new LinkedHashMap<>();
            Map<String,String> childProperties = new LinkedHashMap<>();
            for(Map<String,String> properties : allFieldProperties){

                if(properties.get("dependentField")!=null){
                    parentProperties = properties;
                }
                else if(properties.get("groupBy")!=null){
                    childProperties = properties;
                }
            }
            if(childProperties.size()>1 && parentProperties.size()>1) {
              JSONObject childParentJson = new JSONObject(payloadForBulkEdit);
              int childValuesLength = childParentJson.getJSONObject("body").getJSONObject("data").getJSONObject(childProperties.get("name")).getJSONArray("values").length();

              for(int i=0;i<childValuesLength;i++){
                  childParentJson.getJSONObject("body").getJSONObject("data").getJSONObject(childProperties.get("name")).getJSONArray("values").getJSONObject(i).put("parentId",
                          childParentJson.getJSONObject("body").getJSONObject("data").getJSONObject(parentProperties.get("name")).getJSONArray("values").getJSONObject(i).get("id"));

                  childParentJson.getJSONObject("body").getJSONObject("data").getJSONObject(childProperties.get("name")).getJSONArray("values").getJSONObject(i).put("parentName",
                          childParentJson.getJSONObject("body").getJSONObject("data").getJSONObject(parentProperties.get("name")).getJSONArray("values").getJSONObject(i).get("name"));
              }
              payloadForBulkEdit = childParentJson.toString();
            }
        }

        // Hit Bulk Edit API
        String editJsonStr = hitCreateBulkEdit(entityTypeId,payloadForBulkEdit);

        // Verify Bulk Edit Response
        boolean isRequestCompleted = validBulkEditResponse(editJsonStr,fieldProperties,entityName,customAssert);

        //Wait for Scheduler to finish Request
        if(waitForScheduler == true){
            isRequestCompleted =waitForSchedulerToFinishBulkRequest(entityTypeId,recordsToEdit,maxRecordsForShow,tasksIdBeforeBulkEdit,schedulerWaitTimeout,schedulerPollingTime,checkShowPageIsBlocked,customAssert);
        }
        else if(isRequestCompleted == false && waitForScheduler == true){
            logger.error("Failing Test as Bulk Edit Job for Flow [{}] is not completed yet and Flag " +
                    "\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True.");
            customAssert.assertTrue(false, "Failing Test as Bulk Edit Job for Flow is not completed yet and Flag \'" +
                    "FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True");
        }
        else if(isRequestCompleted == false && waitForScheduler == false){
            throw new SkipException("Bulk Edit Job  is not completed yet and Flag " +
                    "\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to False. Hence not Checking Status on Show Page.");
        }

        //Verify Changes in Show Page
        if(isRequestCompleted) {
            String[] allFields = fieldToUpdate.split("::");
            String[] allFieldsValue = fieldToUpdateValue.split("::");
            if (allFields.length > 1) {
                for (int i = 0; i < allFields.length; i++) {
                    String[] allUpdatedValues = allFieldsValue[i].split(",");
                    if (allUpdatedValues.length > 1) {
                        for (int j = 0; j < allUpdatedValues.length; j++) {
                            verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, allFieldProperties.get(i), recordsPayloadBeforeBulkEdit, allUpdatedValues[j], customAssert);
                        }
                    } else {
                        verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, allFieldProperties.get(i), recordsPayloadBeforeBulkEdit, allUpdatedValues[0], customAssert);
                    }
                }
            } else {
                String[] allUpdatedValues = fieldToUpdateValue.split(",");
                if (allUpdatedValues.length > 1) {
                    for (int j = 0; j < allUpdatedValues.length; j++) {
                        verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, fieldProperties, recordsPayloadBeforeBulkEdit, allUpdatedValues[j], customAssert);
                    }
                } else {
                    verifyChangesInShowPage(entityTypeId, recordsToEdit, maxRecordsForShow, fieldProperties, recordsPayloadBeforeBulkEdit, fieldToUpdateValue, customAssert);
                }
            }
        }
        else {
            throw new SkipException("Request for Bulk is not submitted successfully. Please find field label which is not updated " + fieldProperties.get("label"));
        }
        // verify email for the bulk edit
        verifyBulkEditEmail(customAssert);

        // Restore Records to original State once bulk Edit is complete
        restoreRecords(entityName,recordsPayloadBeforeBulkEdit);
    }

    public List<String> filterListDataByParentInheritance(List<String> listData,String entityName,String fieldToUpdate,String fieldToUpdateValue){
        List<String> recordsToBeEdit = new LinkedList<>();
        List<Map<String,Object>> allOptionsDataParentEntity;
        List<Map<String,Object>> allSelectedValuesDataParentEntity;
        JSONObject jsonObject;

        // Field Labels of parent and child field
        String[] dependentFields = fieldToUpdate.split("::");
        String parentField = dependentFields[0].toLowerCase().trim();

        // Values to be used while bulk editing in parent and child field
        String[] dependentFieldsValue = fieldToUpdateValue.split("::");
        String parentFieldValue = dependentFieldsValue[0].trim();
        for(String data : listData) {
            allOptionsDataParentEntity = new LinkedList<>();
            allSelectedValuesDataParentEntity = new LinkedList<>();
            String showPageResponseStr = editEntityShowPage(entityName, data);
            jsonObject = new JSONObject(showPageResponseStr);

            if(JSONUtility.checkKey(jsonObject.getJSONObject("header").getJSONObject("response"),"errorMessage")){
            if (jsonObject.getJSONObject("header").getJSONObject("response").get("errorMessage")
                    .toString().contains("Either you do not have the required permissions or requested page does not exist anymore.") &&
                    jsonObject.getJSONObject("header").getJSONObject("response").get("status").toString().contains("applicationError")) {

                String entityShowPageResponseStr = hitEntityShowPage(entityName, data);
                JSONObject entityShowPageResponseJsonStr = new JSONObject(entityShowPageResponseStr);
                    logger.info("Skipping Record " + data + " As the status of this record is "+ entityShowPageResponseJsonStr.getJSONObject("body").getJSONObject("data").getJSONObject("status").getJSONObject("values").get("name").toString() + " thus we are not allowed to edit it");
                }
            }
            else{
                recordsToBeEdit.add(data);
            }
        /*    else {
                // Parent Field Options and Selected Value
                if (JSONUtility.checkKey(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(parentField), "options")) {
                    int allOptionsParent = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(parentField).getJSONObject("options").getJSONArray("data").length();
                    for (int i = 0; i < allOptionsParent; i++) {
                        allOptionsDataParentEntity.add(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(parentField).getJSONObject("options").getJSONArray("data").getJSONObject(i).toMap());
                    }
                    int selectedValuesParent = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(parentField).getJSONArray("values").length();
                    for (int i = 0; i < selectedValuesParent; i++) {
                        allSelectedValuesDataParentEntity.add(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject(parentField).getJSONArray("values").getJSONObject(i).toMap());
                    }

                    //Adding Flag if parent value to be used for bulk edit is present in options and not in selected value
                    if (allOptionsDataParentEntity.stream().map(m -> m.get("name")).collect(Collectors.toList()).contains(parentFieldValue)
                            && !allSelectedValuesDataParentEntity.stream().map(m -> m.get("name")).collect(Collectors.toList()).contains(parentFieldValue)) {
                        recordsToBeEdit.add(data);
                    }
                }
                else{
                    logger.info("Skipping Record " + data + " As the field is editable and doesn't have options to select");
                }
            }*/
        }
        return recordsToBeEdit;
    }

    public String editEntityShowPage(String entityName, String showPageId){
        String httpResponseStr = null;
        String uri = "/"+ entityName + "/edit/" + showPageId;
        HttpGet httpGet = new HttpGet(uri);

        httpGet.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");

        try{
        HttpResponse httpResponse = APIUtils.getRequest(httpGet);
        httpResponseStr = EntityUtils.toString(httpResponse.getEntity());
        }
        catch (Exception e){
            logger.error("Request Failed To execute " + e.getStackTrace());
        }
        return httpResponseStr;
    }

    public String hitEntityShowPage(String entityName, String showPageId){
        String httpResponseStr = null;
        String uri = "/"+ entityName + "/show/" + showPageId;
        HttpGet httpGet = new HttpGet(uri);

        httpGet.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
        httpGet.addHeader("Content-Type","application/json;charset=UTF-8");

        try{
            HttpResponse httpResponse = APIUtils.getRequest(httpGet);
            httpResponseStr = EntityUtils.toString(httpResponse.getEntity());
        }
        catch (Exception e){
            logger.error("Request Failed To execute " + e.getStackTrace());
        }
        return httpResponseStr;
    }

    public void verifyBulkEditEmail(CustomAssert customAssert){
        BulkOperationsHelper bulkOperationsHelper = new BulkOperationsHelper();
        String attachmentName = bulkOperationsHelper.getLatestBulkEditAttachmentName();
        String expectedName = getExpectedAttachmentName();
        customAssert.assertTrue(attachmentName.equals(expectedName),"Bulk Edit Attachment Name is not as expected");
        if(!attachmentName.equals(expectedName)){
            logger.error("Email is not triggered for Bulk Edit");
        }
    }

    private String getExpectedAttachmentName() {
        String expectedAttachmentName;
        DateFormat dateFormat = new SimpleDateFormat("MMddyyyy");
        Date date = new Date();
        String currentDate = dateFormat.format(date);

        expectedAttachmentName = "Bulk Edit Response Report - " + currentDate + ".xls";

        return expectedAttachmentName;
    }

    public List<JSONObject> getDependentFieldsJSON(JSONObject jsonObject,String fieldName,String dependentField1,String dependentField2){
        List<JSONObject> dependentFieldsData = new LinkedList<>();
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        for(int i =0; i<jsonArray.length();i++){
            if(jsonArray.getJSONObject(i).get(fieldName).toString().equals(dependentField1) || jsonArray.getJSONObject(i).get(fieldName).toString().equals(dependentField2))
                dependentFieldsData.add(jsonArray.getJSONObject(i));
        }
        return dependentFieldsData;
    }

    private void restoreRecords(String entityName, Map<Integer, String> originalPayloadMap) {
        try {
            logger.info("Restoring all records to its original state for Flow and Entity {}", entityName);

            for (Map.Entry<Integer, String> record : originalPayloadMap.entrySet()) {
                EntityOperationsHelper.restoreRecord(entityName, record.getKey(), originalPayloadMap.get(record.getKey()));
            }
        } catch (Exception e) {
            logger.error("Exception while Restoring Records for Flow and Entity {}. {}", entityName, e.getStackTrace());
        }
    }

    public boolean waitForSchedulerToFinishBulkRequest(int entityTypeId,List<String> recordsToEdit,int maxRecordsForShow,List<Integer> allTaskIds,String schedulerwaittimeout,String schedulerpollingtime,Boolean checkShowPageIsBlocked,CustomAssert csAssert){
            boolean isJobFinished = false;
            int randomNumbersForShow[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (recordsToEdit.size() - 1), maxRecordsForShow);
            int entityIdsForShow[] = new int[randomNumbersForShow.length];
            for (int j = 0; j < randomNumbersForShow.length; j++)
                entityIdsForShow[j] = Integer.parseInt(recordsToEdit.get(j));

            Fetch fetchObj = new Fetch();
            logger.info("Hitting Fetch API to Get Bulk Edit Job Task Id");
            fetchObj.hitFetch();
            logger.info("Getting Task Id of Bulk Edit Job for Flow");
            int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

            waitForScheduler(entityTypeId, entityIdsForShow, newTaskId, schedulerwaittimeout,schedulerpollingtime,checkShowPageIsBlocked,csAssert);

            logger.info("Hitting Fetch API to get Status of Bulk Edit Job");
            fetchObj.hitFetch();
            String bulkEditJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

            if (bulkEditJobStatus != null && bulkEditJobStatus.trim().equalsIgnoreCase("Completed")) {
                isJobFinished = true;
            }
            else{
                isJobFinished = false;
            }

            return isJobFinished;
    }

    public void verifyChangesInShowPage(int entityTypeId,List<String> recordsToEdit,int maxRecordsForShow,Map<String,String> field,Map<Integer,String> recordsPayloadBeforeBulkEdit,String fieldToUpdateValue,CustomAssert csAssert) throws ExecutionException, InterruptedException {
        //Validate Show Page
        int randomNumbersForShow[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (recordsToEdit.size() - 1), maxRecordsForShow);
        int entityIdsForShow[] = new int[randomNumbersForShow.length];
        for (int j = 0; j < randomNumbersForShow.length; j++)
            entityIdsForShow[j] = Integer.parseInt(recordsToEdit.get(j));

         this.verifyDataOnShowPage(entityTypeId, entityIdsForShow, field.get("name"), fieldToUpdateValue, field.get("type"), Boolean.parseBoolean(field.get("multiple")),
                        Boolean.parseBoolean(field.get("dynamicField")), recordsPayloadBeforeBulkEdit, field.get("model"),csAssert);

    }

    public boolean validBulkEditResponse(String editJsonStr,Map<String,String> field,String entityName,CustomAssert csAssert)  {
        boolean isRequestSubmittedSuccessfully = false;
        boolean isValidJson = ParseJsonResponse.validJsonResponse(editJsonStr);
        if (isValidJson) {
            //Verify Response
            JSONObject jsonObj = new JSONObject(editJsonStr);
            String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().toLowerCase();
            csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Status: success and Actual Status: " + status);

            csAssert.assertTrue(jsonObj.toString().contains("Your request has been successfully submitted"),"Request has not been submitted successfully for bulk edit");
            if (status.equalsIgnoreCase("success")) {

                if (!jsonObj.toString().contains("Your request has been successfully submitted")) {
                    isRequestSubmittedSuccessfully = false;
                    csAssert.assertTrue(false, "Request has not been submitted successfully for bulk edit");
                }
                else if(jsonObj.toString().contains("Your request has been successfully submitted")){
                    isRequestSubmittedSuccessfully = true;
                }
            } else {
                isRequestSubmittedSuccessfully =false;
                csAssert.assertTrue(false, "BulkEdit Response for Field " + field.get("label") + " and Entity " + entityName + " having Value " +
                        field.get("name") + " is unsuccessful.");
            }
        }
        return isRequestSubmittedSuccessfully;
    }

    private void waitForScheduler(int entityTypeId, int entityIdsForShow[], int newTaskId,String schedulerwaittimeout,String schedulerpollingtime,Boolean checkShowPageIsBlocked,CustomAssert csAssert) {
        logger.info("Waiting for Scheduler to Complete for Flow.");
        try {
            long timeOut = 1200000;
            String temp = schedulerwaittimeout;
            if (org.apache.commons.lang3.math.NumberUtils.isParsable(temp))
                timeOut = Long.parseLong(temp);

            logger.info("Time Out for Scheduler is {} milliseconds", timeOut);
            long timeSpent = 0;

            if (newTaskId != -1) {
                logger.info("Checking if Bulk Edit Task has completed or not for Flow");
                long pollingTime = 5000;
                temp = schedulerpollingtime;
                if (NumberUtils.isParsable(temp))
                    pollingTime = Long.parseLong(temp);

                while (timeSpent < timeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();

                    logger.info("Getting Status of Bulk Edit Task for Flow");
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        logger.info("Bulk Edit Task Completed for Flow");
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Bulk Edit Task is not finished yet for Flow");
                    }

                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
                        if (!UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId) &&
                                !UserTasksHelper.anyRecordProcessedInTask(fetchObj.getFetchJsonStr(), newTaskId)) {

                            //Verify that Show Page is not accessible for entities
                            if (checkShowPageIsBlocked)
                                this.checkShowPageIsBlocked(entityTypeId, entityIdsForShow, csAssert);
                        } else {
                            logger.info("Bulk Edit Task for Flow [{}] is In Progress but At-least One record has been processed or failed. " +
                                    "Hence Not Checking if Show Page is Blocked or not.");
                        }
                    } else {
                        logger.info("Bulk Edit Task for Flow has not been picked by Scheduler yet.");
                    }
                }
            } else {
                logger.info("Couldn't get Bulk Edit Task Job Id for Flow. Hence waiting for Task Time Out i.e. {}", timeOut);
                Thread.sleep(timeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish for Flow. {}", e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow" + e.getMessage());
        }
    }

    private void checkShowPageIsBlocked(int entityTypeId, int entityIds[], CustomAssert csAssert) {
        try {
            logger.info("Verifying that Show Page is blocked for Flow, Entity Type Id {} and Entity Ids {}", entityTypeId, Arrays.toString(entityIds));
            ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
            List<FutureTask<Boolean>> taskList = new ArrayList<>();

            logger.info("Total Records for EntityTypeId {} are {}", entityTypeId, entityIds.length);
            for (int i = 0; i < entityIds.length; i++) {
                final int index = i;

                FutureTask<Boolean> result = new FutureTask<>(() -> {
                    Show showObj = new Show();
                    logger.info("Hitting Show Api for Record #{} for Flow  having EntityTypeId {} and Id {}", (index + 1), entityTypeId, entityIds[index]);
                    showObj.hitShow(entityTypeId, entityIds[index]);
                    String showJsonStr = showObj.getShowJsonStr();
                    boolean showPageBlocked = showObj.isShowPageBlockedForBulkAction(showJsonStr);
                    if (!showPageBlocked) {
                        logger.error("Show Page is accessible for Record #{} for Flow having EntityTypeId {} and Id {}", (index + 1), entityTypeId,
                                entityIds[index]);
                        csAssert.assertTrue(false, "Show Page is accessible for Record #" + (index + 1) + " for Flow  having EntityTypeId " + entityTypeId + " and Id " + entityIds[index]);
                    }
                    return true;
                });
                taskList.add(result);
                executor.execute(result);
            }
            for (FutureTask<Boolean> task : taskList)
                task.get();
        } catch (Exception e) {
            logger.error("Exception while Checking if Show Page is Blocked for Flow. {}", e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Checking if Show Page is Blocked for Flow." + e.getMessage());
        }
    }

    private void verifyDataOnShowPage(int entityTypeId, int recordIds[], String fieldName, String fieldValue, String fieldType, boolean isMultiple,
                                      Boolean isDynamic, Map<Integer, String> originalPayloadMap,String model, CustomAssert csAssert) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
        List<FutureTask<Boolean>> taskList = new ArrayList<>();

        logger.info("Total Records to validate for Flow and EntityTypeId {} : {}", entityTypeId, recordIds.length);

        for (int j = 0; j < recordIds.length; j++) {
            final int index = j;
            logger.info("Verifying that Show Page is Accessible for Flow, EntityTypeId {} and EntityId {}", entityTypeId, recordIds[index]);

            /*FutureTask<Boolean> result = new FutureTask<>(() -> {
                try {*/
					/*
					Special Handling for Select type fields. Verify Data on Show Page only if that Record has the field option on Edit Page.
					Otherwise the field will get updated but wouldn't fail in Bulk Edit Feature.
					 */

                    if (fieldType.trim().equalsIgnoreCase("select")) {
                        String editGetResponse = originalPayloadMap.get(recordIds[index]);
                        List<Map<String, String>> allOptions = ParseJsonResponse.getAllOptionsForField(editGetResponse, fieldName, isDynamic);

                        Boolean optionPresentOnEditPage = false;
                        if (allOptions != null) {
                            for (Map<String, String> optionMap : allOptions) {
                                if (optionMap.get("name").trim().equalsIgnoreCase(fieldValue.trim())) {
                                    optionPresentOnEditPage = true;
                                    break;
                                }
                            }
                        }

                        if (!optionPresentOnEditPage) {
                            logger.info("Record having Id {} doesn't have the Option {} for Field {} on Edit Page. Hence no need to validate data on Show Page.",
                                    recordIds[index], fieldValue, fieldName);
                            //return true;
                        }
                    }

                    Show showObj = new Show();
                    logger.info("Hitting Show Api for Record #{} for Flow  having EntityTypeId {} and Id {}", (index + 1), entityTypeId, recordIds[index]);
                    showObj.hitShow(entityTypeId, recordIds[index]);
                    String showJsonStr = showObj.getShowJsonStr();
                    boolean isShowPageAccessible = showObj.isShowPageAccessibleForBulkEdit(showJsonStr);

                    if (isShowPageAccessible) {
                        //Verify Field Value
                        if(model!=null){
                        if(model.equals("stakeHolders.values")){
                            ShowHelper.verifyShowField(showJsonStr,"stakeholders","stakeholders",fieldValue,entityTypeId,recordIds[index],csAssert);
                            }
                        }
                        else {
                            logger.info("Verifying Field {} for Record #{} for Flow", fieldName, (index + 1));
                            boolean casePass;
                            casePass = showObj.verifyShowField(showJsonStr, fieldName, fieldValue, entityTypeId, fieldType, isMultiple, isDynamic);

                            if(!casePass){
                                casePass = verifyWhetherRecordIsEditable(entityTypeId,recordIds[index],fieldName,csAssert);

                                if(casePass == true){
                                    logger.info(fieldName + " is not editable for record " + recordIds[index] + ". Thus, ignoring this scenario and considering it as pass.");
                                }
                            }
                            if (!casePass) {
                                logger.error("Record #{} for Flow having EntityTypeId {} and Id {} failed on Show Page for Field {}", (index + 1),
                                        entityTypeId, recordIds[index], fieldName);
                                csAssert.assertTrue(false, "Record #" + (index + 1) + " for Flow having EntityTypeId " +
                                        entityTypeId + " and Id " + recordIds[index] + " failed on Show Page for Field " + fieldName);
                            }
                        }
                    } else {
                        logger.error("Show Page is not accessible for Record #{} for Flow  having EntityTypeId {} and Id {}", (index + 1),
                                entityTypeId, recordIds[index]);
                        csAssert.assertTrue(false, "Show Page is not accessible for Record #" + (index + 1) + " for Flow  having EntityTypeId " + entityTypeId + " and Id " + recordIds[index]);
                    }
                } /*catch (Exception e) {
                    logger.error("Exception while Verifying Show Page for Record for Flow having EntityTypeId {} and Id {}. {}", entityTypeId,
                            recordIds[index], e.getStackTrace());
                    csAssert.assertTrue(false, "Exception while Verifying Show Page for Record having EntityTypeId " + entityTypeId + " and Id " +
                            recordIds[index] + ". " + e.getMessage());
                }*/
           /*     return true;
            });
            taskList.add(result);
            executor.execute(result);
        }*/

        for (FutureTask<Boolean> task : taskList)
            task.get();
    }

    public boolean verifyWhetherRecordIsEditable(int entityTypeId, int recordId,String fieldToUpdate,CustomAssert customAssert){
        boolean casePass = false;
        String urlName = ConfigureConstantFields.getUrlNameForEntity(ConfigureConstantFields.getEntityNameById(entityTypeId));
        String editShowPageStrResponse = editEntityShowPage(urlName,String.valueOf(recordId));
        JSONObject jsonObject = new JSONObject(editShowPageStrResponse);
        Map<String,String> fieldProperties =getPropertiesOfFieldsToBeUpdated(editShowPageStrResponse,fieldToUpdate,ConfigureConstantFields.getEntityNameById(entityTypeId));

        if(jsonObject.getJSONObject("header").getJSONObject("response").get("status").toString().contains("applicationError")){
            if(JSONUtility.checkKey(jsonObject.getJSONObject("header").getJSONObject("response"),"errorMessage")){
                if (jsonObject.getJSONObject("header").getJSONObject("response").get("errorMessage")
                        .toString().contains("Either you do not have the required permissions or requested page does not exist anymore.")) {
                    logger.info("Record " + recordId + " is not editable as it is throwing error message " + jsonObject.getJSONObject("header").getJSONObject("response").get("errorMessage")
                            .toString() + " Thus, we are not considering it for bulk edit");
                    casePass = true;
                }
            }
        }
        else if(fieldProperties.get("displayMode").equals("editable")){
            BulkOperationsHelper bulkOperationsHelper = new BulkOperationsHelper();
            String requestId = bulkOperationsHelper.getLatestBulkEditRequestId();
            String errorMessage = bulkOperationsHelper.getErrorMessagesForBulkEditRequestIdAndEntityId(requestId,recordId);
            logger.warn(fieldToUpdate + " is editable for record " + recordId + ". Then also it is not able to update");
            logger.warn("Error Message Reflected in DB is " + errorMessage);

            if(errorMessage.contains("Can not use . These Function(s) are not present in the parent Entity.\",\"Can not use L. These Service(s) are not present in the parent Entity.")){
                logger.warn("For Record Id " + recordId + " functions are not present in parent entity thus it is restricted in here for edit");
                casePass = true;
            }
            else {
                casePass = false;
            }
        }
        else if(!fieldProperties.get("displayMode").equals("editable")){
            BulkOperationsHelper bulkOperationsHelper = new BulkOperationsHelper();
            String requestId = bulkOperationsHelper.getLatestBulkEditRequestId();
            String errorMessage = bulkOperationsHelper.getErrorMessagesForBulkEditRequestIdAndEntityId(requestId,recordId);
            logger.warn("Database error message as record " + recordId + " is not editable " + errorMessage);
            customAssert.assertTrue(errorMessage.contains("Functions cannot be edited (Non Editable field)"),"Error message in not valid");
            logger.warn(fieldToUpdate + " is editable for record " + recordId + ". Then also it is not able to update");
            casePass = true;
        }
        return casePass;
    }

    public String hitCreateBulkEdit(int entityTypeId,String payloadForEdit){
        BulkeditEdit editObj = new BulkeditEdit();
        editObj.hitBulkeditEdit(entityTypeId, payloadForEdit);
        String editJsonStr = editObj.getBulkeditEditJsonStr();
        return editJsonStr;
    }

    public Map<String,String> getPropertiesOfFieldsToBeUpdated(String createJsonStr,String testField,String entityName){
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(createJsonStr, testField);
        if (field.size() == 0) {
            throw new SkipException("Couldn't find Details for Field " + testField + ". Hence skipping Validation for Entity " + entityName);
        }
        return field;
    }

    public String createPayloadForBulkEdit(Map<String,String> field,String createJsonStr,int entityTypeId,String recordIds,String fieldToUpdateValue,String entityName) throws ConfigurationException {

        String fieldValue = null;
        String payloadForEdit = null;
        String fieldType = null;

        if (field.containsKey("type") && field.get("type") != null)
            fieldType = field.get("type").trim().toLowerCase();

        String fieldLabel = field.get("label");
        String fieldName = field.get("name");

        String fieldIdsForPayload = PayloadUtils.getFieldIdsForBulkEditPayload(createJsonStr, field.get("id"));
        if (fieldIdsForPayload.trim().equalsIgnoreCase("")) {
            throw new SkipException("Couldn't get Field Ids for Bulk Edit Payload. Hence skipping validation.");
        }

        if (fieldType != null) {
            switch (fieldType) {
                default:
                    if (field.get("model") != null && field.get("model").equalsIgnoreCase("stakeHolders.values")) {
                        fieldValue = getValueForField(fieldLabel, "stakeholder",fieldToUpdateValue);
                        int stakeholderId = -1;

                        logger.info("Hitting Options API to get Id for Stakeholder {}.", fieldValue);
                        Options optionObj = new Options();
                        int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                                ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", "stakeholders"));
                        Map<String, String> params = new HashMap<>();
                        params.put("pageType", ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                                ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata"));
                        params.put("query", fieldValue);
                        optionObj.hitOptions(dropDownType, params);
                        String ids = optionObj.getIds();

                        if (ids == null) {
                            throw new SkipException("Couldn't get Stakeholder Id for Stakeholder " + fieldValue + ". Hence skipping further validation.");
                        }

                        if (ids.trim().contains(",")) {
                            String allIds[] = ids.split(Pattern.quote(","));
                            for (String id : allIds) {
                                stakeholderId = Integer.parseInt(id.trim());
                                if (Options.getNameFromId(optionObj.getOptionsJsonStr(), stakeholderId).trim().equalsIgnoreCase(fieldValue)) {
                                    break;
                                }
                            }
                        } else {
                            stakeholderId = Integer.parseInt(ids);
                        }

                        payloadForEdit = getPayloadForEdit(fieldValue, entityTypeId, fieldName, stakeholderId, recordIds, fieldIdsForPayload);
                        fieldName = "stakeholders";
                        fieldType = "stakeholder";
                    } else {
                        fieldValue = getValueForField(fieldLabel, fieldType,fieldToUpdateValue);
                        payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, fieldIdsForPayload);
                    }
                    break;

                case "select":
                    //Get all available options for Field
                    List<Map<String, String>> allOptions = ParseJsonResponse.getAllOptionsForField(createJsonStr, fieldName,
                            Boolean.valueOf(field.get("dynamicField")));

                    String value;
                    String[] fieldsArray =null;
                    List<String> multipleSelectedData = new LinkedList<>();
                    if(fieldToUpdateValue != null && !fieldToUpdateValue.equalsIgnoreCase("")) {
                        if (fieldToUpdateValue.contains(",")) {
                            fieldsArray = fieldToUpdateValue.split(",");
                            for (int i = 0; i < fieldsArray.length; i++) {
                                value = fieldsArray[i].trim();
                                if (value != null && !value.equalsIgnoreCase("")) {
                                    fieldValue = value.trim();
                                    for (Map<String, String> optionsMap : allOptions) {
                                        if (optionsMap.get("name").trim().equalsIgnoreCase(fieldValue)) {
                                            multipleSelectedData.add(selectMultipleValues(field, fieldValue, optionsMap.get("id")));
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            value = fieldToUpdateValue;
                            if (value != null && !value.equalsIgnoreCase("")) {
                                fieldValue = value.trim();

                                for (Map<String, String> optionsMap : allOptions) {
                                    if (optionsMap.get("name").trim().equalsIgnoreCase(fieldValue)) {
                                        payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, optionsMap.get("id"), fieldIdsForPayload);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        if(multipleSelectedData.size()>0){
                            String multiSelectData =String.join(",",multipleSelectedData);
                            payloadForEdit = getPayloadForEdit(field,entityTypeId, recordIds, multiSelectData, fieldIdsForPayload);
                            break;
                        }
                    }

                    logger.info("Total Available Options for Field {} are {}", fieldLabel, allOptions.size());

                    if (allOptions.size() > 0) {
                        int randomNumbersForSelect = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                        fieldValue = allOptions.get(randomNumbersForSelect).get("name");
                        payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, allOptions.get(randomNumbersForSelect).get("id"),
                                fieldIdsForPayload);
                        logger.info("Verifying BulkEdit For Field {} having Value {}", fieldLabel, fieldValue);
                    } else
                        logger.info("No Option found for Field {} and Entity {}", fieldLabel, entityName);
                    break;

                case "checkbox":
                    fieldValue = getValueForField(fieldLabel, fieldType,fieldToUpdateValue);
                    payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, fieldIdsForPayload);
                    break;
            }
        } else {
            throw new SkipException("Null Field Type for Field " + fieldLabel + " of Entity " + entityName + ". Hence Skipping Validation.");
        }
        return payloadForEdit;
    }


    private String getValueForField(String fieldLabel, String fieldType,String fieldToUpdateValue) throws ConfigurationException {
        String fieldValue = null;

            String value = fieldToUpdateValue;
            if (value != null && !value.equalsIgnoreCase(""))
                return value;


        switch (fieldType) {
            case "text":
            case "textarea":
                String basePrefix = text;
                String currTime = Long.toString(System.currentTimeMillis());
                currTime = currTime.substring(currTime.length() - 3, currTime.length());
                fieldValue = basePrefix + currTime;
                break;

            case "date":
                fieldValue = date;
                break;

            case "stakeholder":
                fieldValue = stakeholder;
                break;

            case "number":
                fieldValue = number;
                break;

            default:
                logger.warn("Field Type {} not defined. Hence returning Null Value", fieldType);
                break;
        }
        if (fieldValue == null)
            logger.warn("Couldn't get Value for Field {}", fieldLabel);
        return fieldValue;
    }

    private String getPayloadForEdit(Map<String, String> field, String expectedValue, int entityTypeId, String entityIds, String fieldIdsForPayload)
            throws ConfigurationException {
        return getPayloadForEdit(field, expectedValue, entityTypeId, entityIds, null, fieldIdsForPayload);
    }

    private String getPayloadForEdit(Map<String, String> field, String expectedValue, int entityTypeId, String entityIds, String fieldId, String fieldIdsForPayload)
            throws ConfigurationException {
        String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":0}},";
        String temp;

        switch (field.get("type").trim().toLowerCase()) {
            default:
                temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":\"" + expectedValue + "\"}";
                break;

            case "select":
                if (field.get("multiple").equalsIgnoreCase("true"))
                    temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":[{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}]}";
                else
                    temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}}";

                break;

            case "checkbox":
                temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":\" + Boolean.valueOf(expectedValue) + \"}";
                break;

        }
        if (field.get("dynamicField").equalsIgnoreCase("true"))
            payload += "\"dynamicMetadata\":{" + temp + "}";
        else
            payload += temp;
        payload += "},\"globalData\":{\"entityIds\":" + entityIds + ",\"fieldIds\": [" + fieldIdsForPayload + "],\"isGlobalBulk\":true}}}";

        return payload;
    }

    //For Select Field in case of multiple fields
    private String getPayloadForEdit(Map<String,String> field,int entityTypeId, String entityIds, String multiSelectedData,String fieldIdsForPayload) {
        String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":0}},";

        String temp;
        if (field.get("multiple").equalsIgnoreCase("true"))
            temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":["+ multiSelectedData +"]}";
        else
            temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":"+ multiSelectedData +"}";

        if (field.get("dynamicField").equalsIgnoreCase("true"))
            payload += "\"dynamicMetadata\":{" + temp + "}";
        else
            payload += temp;
        payload += "},\"globalData\":{\"entityIds\":" + entityIds + ",\"fieldIds\": [" + fieldIdsForPayload + "],\"isGlobalBulk\":true}}}";

        return payload;
    }

    public String selectMultipleValues(Map<String, String> field,String expectedValue,String fieldId){
        String temp = null;
        if (field.get("multiple").equalsIgnoreCase("true"))
            temp = "{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}";
        return temp;
    }

    //For Stakeholder Field
    private String getPayloadForEdit(String expectedValue, int entityTypeId, String roleGroup, int id, String entityIds, String fieldIdsForPayload) {
        String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
                + "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":0}},";
        String temp = "\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"" + roleGroup + "\":{\"values\":[{\"id\":" + id + ",\"name\":\"" + expectedValue + "\"}]}}}";
        payload += temp + "},\"globalData\":{\"entityIds\":" + entityIds + ",\"fieldIds\": [" + fieldIdsForPayload + "],\"isGlobalBulk\":true}}}";
        return payload;
    }


    public String bulkEditCreate(List<String> recordIds,int entityTypeId){
        String payloadForCreate = BulkeditCreate.getPayload(recordIds);
        BulkeditCreate createObj = new BulkeditCreate();
        String createJsonStr = createObj.hitBulkeditCreate(entityTypeId, payloadForCreate);
        return createJsonStr;
    }

    private Map<Integer, String> getPayloadForRecords(String entityName, List<String> recordIds) {
        Map<Integer, String> recordsPayloadMap = new HashMap<>();

        try {
            Edit editObj = new Edit();

            for (String id : recordIds) {
                Integer recordId = Integer.parseInt(id);

                logger.info("Hitting Edit Get API for Record Id {}, Entity {}", recordId, entityName);
                String editGetResponse = editObj.hitEdit(entityName, recordId);

                if (!ParseJsonResponse.validJsonResponse(editGetResponse))
                    throw new SkipException("Edit Get API Response for Record Id " + recordId + ", Entity " + entityName + "  is an Invalid JSON.");

                recordsPayloadMap.put(recordId, editGetResponse);
            }
        } catch (Exception e) {
            throw new SkipException("Exception occurred while creating Original Payload Map for Records for " + entityName + " \nError trace " + e.getMessage());
        }
        return recordsPayloadMap;
    }


    public List<String> getEntityShowPageIds(int listId, List<Map<Integer, Map<String, String>>> filteredRecords, int maxRecordsForListData, ListRendererDefaultUserListMetaData listMetadatObj,String entityName){
        List<String> idsForShow = null;
        logger.info("Getting Random Records from List Data for Entity {}", entityName);
        int[] randomNumbersForBulkEdit = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (filteredRecords.size() - 1), maxRecordsForListData);
        logger.info("Total Records selected from List Data {} for Entity {}", randomNumbersForBulkEdit.length, entityName);
        logger.info("Getting Ids for the Selected Records from List Data for Entity {}", entityName);
        String recordIds = this.setRecordIds(filteredRecords, randomNumbersForBulkEdit, listMetadatObj.getIdFromQueryName("id"));
        logger.info("Ids for the Selected  Records from List Data for Entity {} are : {}", entityName, recordIds);

        String[] allRecordIds = recordIds.split(Pattern.quote(","));
        idsForShow = Arrays.asList(allRecordIds);

        return idsForShow;
    }

    public ListRendererDefaultUserListMetaData hitListRendererDefaultUserListMetaData(int listId,String entityName){
        ListRendererDefaultUserListMetaData listMetadatObj = new ListRendererDefaultUserListMetaData();
        logger.info("Hitting ListRendererDefaultUserListMetaData Api for Entity {}", entityName);
        listMetadatObj.hitListRendererDefaultUserListMetadata(listId, null, "{}");
        listMetadatObj.setFilterMetadatas(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());
        listMetadatObj.setColumns(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());
        return listMetadatObj;
    }

    private String setRecordIds(List<Map<Integer, Map<String, String>>> listData, int[] indexArray, int columnId) {
        String entityIds = "";
        boolean first = true;
        try {
            for (int index : indexArray) {
                if (first) {
                    entityIds = entityIds.concat(listData.get(index).get(columnId).get("valueId"));
                    first = false;
                } else
                    entityIds = entityIds.concat("," + listData.get(index).get(columnId).get("valueId"));
            }
        } catch (Exception e) {
            logger.error("Exception while setting Entity Ids in TestBulkEdit. {}", e.getMessage());
        }
        return entityIds;
    }


    public List<Integer> fetchTasksIds(){
        logger.info("Hitting Fetch API.");
        Fetch fetchObj = new Fetch();
        fetchObj.hitFetch();
        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

        return allTaskIds;
    }

    public boolean killAllSchedulerTasks(boolean killAlreadyScheduledTasks){
        boolean tasksKilled;
        if(killAlreadyScheduledTasks){
            UserTasksHelper.removeAllTasks();
            tasksKilled = true;
        }
        else{
            tasksKilled = false;
            logger.info("Already Scheduled tasks are not killed if there are tasks lined up then it may take some time to accomplish bulk edit");
        }
        return tasksKilled;
    }

    public List<Map<Integer, Map<String, String>>> getEntityListData(int entityTypeId,int listId,int listDataOffset,int maxRecordsForListData){
        List<Map<Integer, Map<String, String>>> listData = null;
        // Payload for Entities Listing Page Data
        String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + maxRecordsForListData +
                    ",\"orderByColumnName\":\"id\"," + "\"orderDirection\":\"desc\",\"filterJson\":{}}}";

        ListRendererListData listDataObj = new ListRendererListData();
        logger.info("Hitting ListRendererListData Api for EntityTypeId {}", entityTypeId);
        listDataObj.hitListRendererListData(listId, false, payloadForListData, null);
        listDataObj.setListData(listDataObj.getListDataJsonStr());
        listData = listDataObj.getListData();

        return listData;
    }

    public List<Map<Integer, Map<String, String>>> filterLockedData(List<Map<Integer, Map<String, String>>> listData,String entityName){
        List<Map<Integer, Map<String, String>>> filteredRecords = null;
        if(listData.size()>0){
        logger.info("Getting Column Id for BulkCheckBox Column");
        int columnIdForBulkCheckBox = Integer.valueOf(listData.stream().findFirst().get().values().stream().filter(m->m.get("columnName").equals("bulkcheckbox")).findFirst().get().get("id"));
        logger.info("Getting Column Id for Status Column");
        int columnIdForStatus = Integer.valueOf(listData.stream().findFirst().get().values().stream().filter(m->m.get("columnName").equals("status")).findFirst().get().get("id"));
        logger.info("Filtering List Data Records. i.e. Removing records which are already locked for Bulk Action.");
        filteredRecords = this.filterListDataRecords(listData, columnIdForBulkCheckBox, columnIdForStatus);
        }
        else{
            throw new SkipException("No Data in entity listing " + entityName + " bulk edit can't be performed");
        }

        return filteredRecords;
    }

    private List<Map<Integer, Map<String, String>>> filterListDataRecords(List<Map<Integer, Map<String, String>>> listDataRecords, int columnIdForBulkCheckBox,
                                                                          int columnIdForStatus) {
        List<Map<Integer, Map<String, String>>> filteredRecords = new ArrayList<>();
        filteredRecords.addAll(listDataRecords);

        try {
            for (int i = 0; i < filteredRecords.size(); ) {
                Map<Integer, Map<String, String>> record = filteredRecords.get(i);
                if (record.get(columnIdForBulkCheckBox).get("value").toLowerCase().contains("true") ||
                        record.get(columnIdForStatus).get("value").trim().equalsIgnoreCase("Archived"))
                    filteredRecords.remove(i);
                else
                    i++;
            }
        } catch (Exception e) {
            logger.error("Exception while filtering List Data Records. {}", e.getMessage());
        }
        return filteredRecords;
    }

}
