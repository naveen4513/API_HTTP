package com.sirionlabs.api.TaskLibraryAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CreateTaskAPI {
    private static final String CreateGeneralTaskAPIFilePath;
    private static final String CreateGeneralTaskAPIFileName;
    private static final String CreateEditTaskAPIFileName;
    private static final String CreateEditTaskAPIFilePath;
    private static final String CreateStateTaskAPIFilePath;
    private static final String CreateStateTaskAPIFileName;
    private static final String CreateEmailTaskAPIFilePath;
    private static final String CreateEmailTaskAPIFileName;
    private static final String CreateLeadTimeTaskAPIFilePath;
    private static final String CreateLeadTimeTaskAPIFileName;
    private static final String CreateValueUpdateTaskAPIFilePath;
    private static final String CreateValueUpdateTaskAPIFileName;
    private static final String CreateAutoCreateTaskAPIFilePath;
    private static final String CreateAutoCreateTaskAPIFileName;
    private static final String DeleteTaskAPIFilePath;
    private static final String DeleteTaskAPIFileName;
    private static final String FetchTaskAPIFilePath;
    private static final String FetchTaskAPIFileName;
    private static final String UpdateTaskAPIFilePath;
    private static final String UpdateTaskAPIFileName;

    private static Map<String, String> map;

    static {
        CreateGeneralTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateGeneralTaskAPIFilePath");
        CreateGeneralTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateGeneralTaskAPIFileName");
        CreateEditTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateEditTaskAPIFileName");
        CreateEditTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateEditTaskAPIFilePath");
        CreateStateTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateStateTaskAPIFilePath");
        CreateStateTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateStateTaskAPIFileName");
        CreateEmailTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateEmailTaskAPIFilePath");
        CreateEmailTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateEmailTaskAPIFileName");
        CreateLeadTimeTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateLeadTimeTaskAPIFilePath");
        CreateLeadTimeTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateLeadTimeTaskAPIFileName");
        CreateValueUpdateTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateValueUpdateTaskAPIFilePath");
        CreateValueUpdateTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateValueUpdateTaskAPIFileName");
        CreateAutoCreateTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreateAutoCreateTaskAPIFilePath");
        CreateAutoCreateTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("CreateAutoCreateTaskAPIFileName");
        DeleteTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("DeleteTaskAPIFilePath");
        DeleteTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("DeleteTaskAPIFileName");
        FetchTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("FetchTaskAPIFilePath");
        FetchTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("FetchTaskAPIFileName");
        UpdateTaskAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("UpdateTaskAPIFilePath");
        UpdateTaskAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("UpdateTaskAPIFileName");
    }

    public String getPayloadCreateGeneralTask(String status, String taskName, String entityTypeId, String entityName) {

        return "{\n" +
                "        \"task\": {\n" +
                "            \"type\": \"GENERAL\",\n" +
                "            \"entityType\": {\n" +
                "                \"name\": \"" + entityName + "\",\n" +
                "                \"id\":" + entityTypeId + "\n" +
                "            },\n" +
                "            \"id\": null,\n" +
                "            \"name\": \"" + taskName + "\",\n" +
                "            \"description\": null,\n" +
                "            \"status\": {\n" +
                "                \"id\": 2457,\n" +
                "                \"name\": \"" + status + "\"\n" +
                "            },\n" +
                "            \"button\": {\n" +
                "                \"id\": 1011,\n" +
                "                \"name\": \"Manager Review\"\n" +
                "            },\n" +
                "            \"requiredFields\": null,\n" +
                "            \"editableFields\": null,\n" +
                "            \"hiddenFields\": null,\n" +
                "            \"tabs\": null,\n" +
                "            \"actions\": null,\n" +
                "            \"confirmMsg\": null,\n" +
                "            \"createSecondaryEntity\": null,\n" +
                "            \"ownerRoleGroups\": null,\n" +
                "            \"ownerUserRoleGroups\": null,\n" +
                "            \"wfGeneralTaskValidationList\": [\n" +
                "                {\n" +
                "                    \"id\": null,\n" +
                "                    \"fieldId\": null,\n" +
                "                    \"message\": null,\n" +
                "                    \"rule\": null\n" +
                "                }\n" +
                "            ],\n" +
                "            \"wfTaskType\": {\n" +
                "                \"id\": 1,\n" +
                "                \"name\": \"General Task\"\n" +
                "            }\n" +
                "        }\n" +
                "    }";
    }

    public String getPayloadCreateEditTask(String wfTaskTypeId, String wfTaskTypeName, String taskName, String entityTypeId, String entityName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"EDIT\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":null,\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":" + wfTaskTypeId + ",\n" +
                "\"name\":\"" + wfTaskTypeName + "\"\n" +
                "},\n" +
                "\"editableFields\":null,\n" +
                "\"hiddenFields\":null,\n" +
                "\"tabs\":null,\n" +
                "\"requiredFields\":null,\n" +
                "\"wfEditTaskValidationList\":[\n" +
                "]\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateStateTask(String stateId, String stateName, String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"STATE\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityTypeName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":\"Test State task\",\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":4,\n" +
                "\"name\":\"State Task\"\n" +
                "},\n" +
                "\"state\":{\n" +
                "\"id\":" + stateId + ",\n" +
                "\"name\":\"" + stateName + "\"\n" +
                "}\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateEmailTask(String emailTemplateId, String emailTemplateName, String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"EMAIL\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityTypeName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":2,\n" +
                "\"name\":\"Email Task\"\n" +
                "},\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":\"Test Email Task\",\n" +
                "\"toUserRoleGroups\":[\n" +
                "{\n" +
                "\"id\":1383,\n" +
                "\"name\":\"test032020\"\n" +
                "}\n" +
                "],\n" +
                "\"toRoleGroups\":null,\n" +
                "\"ccUserRoleGroups\":null,\n" +
                "\"ccRoleGroups\":null,\n" +
                "\"bccUserRoleGroups\":null,\n" +
                "\"bccRoleGroups\":null,\n" +
                "\"emailTemplate\":{\n" +
                "\"id\":" + emailTemplateId + ",\n" +
                "\"name\":\"" + emailTemplateName + "\"\n" +
                "}\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateLeadTimeTask(String leadTimeFieldId, String leadTimeFieldName, String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"LEAD_TIME\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\" " + entityTypeName + " \",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + " \",\n" +
                "\"description\":null,\n" +
                "\"leadTimeField\":{\n" +
                "\"id\":" + leadTimeFieldId + ",\n" +
                "\"name\":\"" + leadTimeFieldName + "\"\n" +
                "},\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":3,\n" +
                "\"name\":\"Lead Time Task\"\n" +
                "},\n" +
                "\"rule\":\"{\\\"ruleType\\\":\\\"ADD\\\",\\\"chain\\\":[{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"},{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"20\\\"}]}\"\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateValueUpdateTask(String fieldId, String fieldName, String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"VALUE_UPDATE\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityTypeName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":\"Value Update Test Task\",\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":6,\n" +
                "\"name\":\"Value Update Task\"\n" +
                "},\n" +
                "\"valueUpdateTaskDataList\":[\n" +
                "{\n" +
                "\"id\":null,\n" +
                "\"field\":{\n" +
                "\"id\":" + fieldId + ",\n" +
                "\"name\":\"" + fieldName + "\"\n" +
                "},\n" +
                "\"rule\":\"{\\\"ruleType\\\":\\\"ADD\\\",\\\"chain\\\":[{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"},{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"20\\\"}]}\"\n" +
                "}\n" +
                "]\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateValueUpdateLargeChainingValTask(String fieldId, String fieldName, String taskName, String entityTypeId, String entityTypeName, String rule) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"VALUE_UPDATE\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityTypeName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":\"Value Update Test Task\",\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":6,\n" +
                "\"name\":\"Value Update Task\"\n" +
                "},\n" +
                "\"valueUpdateTaskDataList\":[\n" +
                "{\n" +
                "\"id\":null,\n" +
                "\"field\":{\n" +
                "\"id\":" + fieldId + ",\n" +
                "\"name\":\"" + fieldName + "\"\n" +
                "},\n" +
                "\"rule\":\"" + rule + "\"\n" +
                "}\n" +
                "]\n" +
                "}\n" +
                "}";
    }

    public String getPayloadCreateValueUpdateLargeValTask(String taskName, String entityTypeId, String entityTypeName, String rule) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"VALUE_UPDATE\",\n" +
                "\"entityType\":{\n" +
                "\"name\":\"" + entityTypeName + "\",\n" +
                "\"id\":" + entityTypeId + "\n" +
                "},\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + "\",\n" +
                "\"description\":null,\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":6,\n" +
                "\"name\":\"Value Update Task\"\n" +
                "},\n" +
                "\"valueUpdateTaskDataList\":[\n" +
                rule +
                "]\n" +
                "}\n" +
                "}";
    }


    public String getPayloadCreateAutoCreateTask(String entityToCreateId, String entityToCreateName, String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "\"task\":{\n" +
                "\"type\":\"AUTO_CREATE\",\n" +
                "\"id\":null,\n" +
                "\"name\":\"" + taskName + " \",\n" +
                "\"description\":\"This is AutoCreate task\",\n" +
                "\"entityType\":{\n" +
                "\"id\":" + entityTypeId + " ,\n" +
                "\"name\":\"" + entityTypeName + " \"\n" +
                "},\n" +
                "\"wfTaskType\":{\n" +
                "\"id\":8,\n" +
                "\"name\":\"AutoCreate\"\n" +
                "},\n" +
                "\"entityToCreate\":{\n" +
                "\"id\":" + entityToCreateId + ",\n" +
                "\"name\":\" " + entityToCreateName + " \"\n" +
                "}\n" +
                "}\n" +
                "}";
    }

    public String getPayloadForUpdateTask(String taskName, String entityTypeId, String entityTypeName) {

        return "{\n" +
                "    \"task\": {\n" +
                "        \"createdBy\": {\n" +
                "            \"id\": 1160,\n" +
                "            \"name\": \"Anay Admin\"\n" +
                "        },\n" +
                "        \"dateCreated\": \"09-23-2020 11:23:47\",\n" +
                "        \"dateUpdated\": \"09-23-2020 11:23:47\",\n" +
                "        \"description\": null,\n" +
                "        \"entityType\": {\n" +
                "            \"id\":" + entityTypeId + ",\n" +
                "            \"name\": \"" + entityTypeName + "\"\n" +
                "        },\n" +
                "        \"id\": 300,\n" +
                "        \"modifiedBy\": {\n" +
                "            \"id\": 1160,\n" +
                "            \"name\": \"Anay Admin\"\n" +
                "        },\n" +
                "        \"name\": \"" + taskName + "\",\n" +
                "        \"type\": \"VALUE_UPDATE\",\n" +
                "        \"valueUpdateTaskDataList\": [\n" +
                "            {\n" +
                "                \"field\": {\n" +
                "                    \"apiName\": \"additionalACV\",\n" +
                "                    \"dataType\": null,\n" +
                "                    \"id\": 33,\n" +
                "                    \"name\": \"Additional ACV Value\"\n" +
                "                },\n" +
                "                \"id\": null,\n" +
                "                \"rule\": \"{\\\"ruleType\\\":\\\"ADD\\\",\\\"chain\\\":[{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"},{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"}]}\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"wfTaskType\": {\n" +
                "            \"id\": 6,\n" +
                "            \"name\": \"ValueUpdate\"\n" +
                "        }\n" +
                "    }\n" +
                "}";
    }

    public APIValidator hitPostCreateTaskAPICall(APIExecutor executor, String domain, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-task/save-entity";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public APIValidator hitDeleteTaskAPICall(APIExecutor executor, String domain, String taskIdToBeDeleted) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-task/delete/" + taskIdToBeDeleted;
        return executor.post(domain, queryString, headers, null, null);
    }

    public APIValidator hitFetchTaskAPICall(APIExecutor executor, String domain, String taskIdToBeFetched) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-task/fetch/" + taskIdToBeFetched;
        return executor.get(domain, queryString, headers);
    }

    public APIValidator hitUpdateTaskAPICall(APIExecutor executor, String domain, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        String queryString = "/wf-task/update-entity";
        return executor.post(domain, queryString, headers, payload, null);
    }

    public int randomNumberGenerator() {
        Random rand = new Random();
        return rand.nextInt(99999);
    }

    public String createRuleForValueUpdateTask(int numberofRules) {
        String rule = "{\n" +
                "\"id\":null,\n" +
                "\"field\":{\n" +
                "\"id\":4906,\n" +
                "\"name\":\"Contract Type\"\n" +
                "},\n" +
                "\"rule\":\"{\\\"ruleType\\\":\\\"ADD\\\",\\\"chain\\\":[{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"},{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"}]}\"\n" +
                "},";

        String repeated = new String(new char[numberofRules]).replace("\0", rule);
        repeated = repeated.substring(0, repeated.length() - 1);
        return repeated;
    }

    public String createRuleForValueUpdateChainingTask(int numberofRules) {
        String s1 = "{\\\"ruleType\\\":\\\"AND\\\",\\\"chain\\\":[";
        String s2 = "{\\\"ruleType\\\":\\\"ADD\\\",\\\"chain\\\":[{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"},{\\\"ruleType\\\":\\\"LITERAL\\\",\\\"value\\\":\\\"10\\\"}]},";
        String s3 = "]}";

        String repeated = new String(new char[numberofRules]).replace("\0", s2);
        repeated = repeated.substring(0, repeated.length() - 1);
        return s1 + repeated + s3;
    }

    public static Map<String, String> getAllConfigForCreateGeneralTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateGeneralTaskAPIFilePath, CreateGeneralTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateEditTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateEditTaskAPIFilePath, CreateEditTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateStateTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateStateTaskAPIFilePath, CreateStateTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateEmailTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateEmailTaskAPIFilePath, CreateEmailTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateLeadTimeTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateLeadTimeTaskAPIFilePath, CreateLeadTimeTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateValueUpdateTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateValueUpdateTaskAPIFilePath, CreateValueUpdateTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForCreateAutoCreateTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(CreateAutoCreateTaskAPIFilePath, CreateAutoCreateTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForDeleteTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(DeleteTaskAPIFilePath, DeleteTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForFetchTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(FetchTaskAPIFilePath, FetchTaskAPIFileName, section_name);
        return map;
    }

    public static Map<String, String> getAllConfigForUpdateTaskAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(UpdateTaskAPIFilePath, UpdateTaskAPIFileName, section_name);
        return map;
    }
}