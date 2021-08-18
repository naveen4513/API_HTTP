package com.sirionlabs.helper.servicelevel;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ServiceLevelHelper {

    private final static Logger logger = LoggerFactory.getLogger(ServiceLevelHelper.class);

    private String slEntity = "service levels";
    private int  slEntityTypeId = 14;
    private  int cslEntityTypeId = 15;

    private String cslEntity = "child service levels";
    private String contract = "contracts";

    private String slCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
    private String slCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");

    private String extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
    private String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");

    private String contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractFilePath");
    private String contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractFileName");

    private String contractExtraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractFilePath");
    private String contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractExtraFieldsFileName");


    private String slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
    private String slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

    public int getContractId(String flowToTest, CustomAssert softAssert) {

        int contractId = -1;

        CreateEntity createEntity = new CreateEntity(contractCreationConfigFilePath, contractCreationConfigFileName,
                contractExtraFieldsConfigFilePath, contractExtraFieldsConfigFileName, flowToTest);

        String createPayload = createEntity.getCreatePayload(contract, true, false);
        //Updating payload according to PCQ
        if (!JSONUtility.validjson(createPayload)) {
            throw new SkipException("Couldn't get Create Payload as valid Json for Flow [" + flowToTest + "] Thus Skipping the test");
        }
        JSONObject createPayloadJson = new JSONObject(createPayload);
        createPayload = createPayloadJson.toString();

        String createResponse = null;

        if (createPayload != null) {
            logger.info("Hitting Create Api for Entity {}.", contract);
            Create createObj = new Create();
            createObj.hitCreate(contract, createPayload);
            createResponse = createObj.getCreateJsonStr();

            if (!ParseJsonResponse.validJsonResponse(createResponse)) {
                FileUtils.saveResponseInFile(slEntity + " Create API HTML.txt", createResponse);
            }
        }

        if (createResponse == null) {
            throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "] Thus Skipping the test");
        }

        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

            logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                contractId = CreateEntity.getNewEntityId(createResponse, contract);

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return contractId;
    }

    public int getServiceLevelId(String flowToTest, String PCQ, String DCQ, CustomAssert softAssert) {

        int serviceLevelId = -1;

        CreateEntity createEntity = new CreateEntity(slCreationConfigFilePath, slCreationConfigFileName,
                extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

        String createPayload = createEntity.getCreatePayload("service levels", true, false);
        //Updating payload according to PCQ
        if (!JSONUtility.validjson(createPayload)) {
            throw new SkipException("Couldn't get Create Payload as valid Json for Flow [" + flowToTest + "] Thus Skipping the test");
        }
        JSONObject createPayloadJson = new JSONObject(createPayload);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values", DCQ);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values", PCQ);
        createPayload = createPayloadJson.toString();

        String createResponse = null;

        if (createPayload != null) {
            logger.info("Hitting Create Api for Entity {}.", slEntity);
            Create createObj = new Create();
            createObj.hitCreate(slEntity, createPayload);
            createResponse = createObj.getCreateJsonStr();

            if (!ParseJsonResponse.validJsonResponse(createResponse)) {
                FileUtils.saveResponseInFile(slEntity + " Create API HTML.txt", createResponse);
            }
        }

        if (createResponse == null) {
            throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "] Thus Skipping the test");
        }

        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

            logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                serviceLevelId = CreateEntity.getNewEntityId(createResponse, "service levels");

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return serviceLevelId;
    }

    public Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user, CustomAssert softAssert) {

        Boolean workFlowStepActionStatus;
        String actionNameAuditLog;
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                workFlowStepActionStatus = workflowActionsHelper.performWorkflowAction(entityTypeId, entityId, workFlowStepToBePerformed);
                if (!workFlowStepActionStatus) {

                    softAssert.assertTrue(false, "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId);
                    return false;
                } else {
//                    actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(slConfigFilePath, slConfigFileName, "auditlogactioname", workFlowStepToBePerformed);
//                    if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, softAssert)) {
//                        softAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
//                    }
                }
            }

        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getStackTrace());
            return false;

        }
        return true;
    }

    private Boolean verifyAuditLog(int entityTypeId, int entityId, String actionNameExpected, String user, CustomAssert softAssert) {

        logger.info("Validating Audit Log Tab for entity type id " + entityTypeId + " and entity id " + entityId);

        int AuditLogTabId = 61;

        Boolean validationStatus = true;
        int expectedValidationChecksOnAuditLogTab = 5;
        int actualValidationChecksOnAuditLogTab = 0;

        TabListData tabListData = new TabListData();

        JSONObject latestActionRow;
        JSONObject tabListDataResponseJson;
        JSONArray dataArray;
        JSONArray latestActionRowJsonArray;

        String tabListDataResponse;
        String columnName;
        String columnValue;

        try {

            String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();
            String previousDate = DateUtils.getPreviousDateInMM_DD_YYYY(currentDate);
            String nextDate = DateUtils.getNextDateInMM_DD_YYYY(currentDate);

            tabListData.hitTabListData(AuditLogTabId, entityTypeId, entityId);
            tabListDataResponse = tabListData.getTabListDataResponseStr();

            if (APIUtils.validJsonResponse(tabListDataResponse)) {

                tabListDataResponseJson = new JSONObject(tabListDataResponse);
                dataArray = tabListDataResponseJson.getJSONArray("data");
                latestActionRow = dataArray.getJSONObject(0);
//                latestActionRow = dataArray.getJSONObject(dataArray.length() - 1);
                latestActionRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(latestActionRow);

                for (int i = 0; i < latestActionRowJsonArray.length(); i++) {

                    columnName = latestActionRowJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = latestActionRowJsonArray.getJSONObject(i).get("value").toString();

                    switch (columnName) {

                        case "action_name":

                            if (!columnValue.equalsIgnoreCase(actionNameExpected)) {
                                softAssert.assertTrue(false, "Under Audit Log Tab action_name is validated unsuccessfully for entity id " + entityId);
                                softAssert.assertTrue(false, "Expected action_name : " + actionNameExpected + " Actual action_name : " + columnValue);
                                validationStatus = false;
                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "requested_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                softAssert.assertTrue(false, "Under Audit Log Tab requested_by is validated unsuccessfully for entity id " + entityId);
                                softAssert.assertTrue(false, "Expected requested_by : " + actionNameExpected + " Actual requested_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "completed_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                softAssert.assertTrue(false, "Under Audit Log Tab completed_by is validated unsuccessfully for entity id " + entityId);
                                softAssert.assertTrue(false, "Expected completed_by : " + actionNameExpected + " Actual completed_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "audit_log_date_created":

                            if (!(columnValue.contains(currentDate) || columnValue.contains(previousDate) || columnValue.contains(nextDate))) {

                                softAssert.assertTrue(false, "Under Audit Log Tab audit_log_date_created is validated unsuccessfully for entity id " + entityId);
                                softAssert.assertTrue(false, "Expected audit_log_date_created : " + currentDate + " OR " + previousDate
                                        + " OR " + nextDate + " Actual audit_log_date_created : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "audit_log_user_date":

                            if (!(columnValue.contains(currentDate) || columnValue.contains(previousDate) || columnValue.contains(nextDate))) {

                                softAssert.assertTrue(false, "Under Audit Log Tab audit_log_user_date is validated unsuccessfully for entity id " + entityId);
                                softAssert.assertTrue(false, "Expected audit_log_user_date : " + currentDate + " OR " + previousDate
                                        + " OR " + nextDate + " Actual audit_log_user_date : " + columnValue);
                                validationStatus = false;
                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;
                    }

                }

                if (actualValidationChecksOnAuditLogTab == expectedValidationChecksOnAuditLogTab) {
                    softAssert.assertTrue(true, "All validation checks passed successfully");
                } else {
                    softAssert.assertTrue(false, "Validation check count not equal to " + expectedValidationChecksOnAuditLogTab);
                    validationStatus = false;
                }
            } else {
                softAssert.assertTrue(false, "Audit Log Tab Response is not a valid json for entity id " + entityId);
                validationStatus = false;
            }


        } catch (Exception e) {
            logger.error("Exception while validating tab list response " + e.getStackTrace());
            softAssert.assertTrue(false, "Exception while validating tab list response " + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;
    }

    public ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId, CustomAssert softAssert) {

        logger.info("Checking if CSL created on service level");

        int childServiceLevelTabId = 7;

        long timeSpent = 0;
        long cSLCreationTimeOut = 1500000L;
        long pollingTime = 5000L;
        ArrayList<String> childServiceLevelIds = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray;

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        softAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        softAssert.assertTrue(true, "Child Service Level created successfully ");

                        childServiceLevelIds = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childServiceLevelIds.size() == 0) {
                    softAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId);
                }

            } else {
                softAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + e.getStackTrace());
        }

        return childServiceLevelIds;
    }

    public List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try {
            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp");
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
    }

    public Boolean uploadPerformanceDataFormat(int entityId, int uploadId, int templateId, String performanceDataFormatFilePath, String performanceDataFormatFileName, String expectedMsg, CustomAssert softAssert) {

        logger.info("Uploading Performance Data Format on " + entityId);
        UploadBulkData uploadBulkData = new UploadBulkData();

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("parentEntityId", String.valueOf(entityId));
        payloadMap.put("parentEntityTypeId", String.valueOf(slEntityTypeId));

        uploadBulkData.hitUploadBulkData(uploadId, templateId, performanceDataFormatFilePath, performanceDataFormatFileName, payloadMap);

        String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

        if (!fileUploadResponse.contains(expectedMsg)) {

            softAssert.assertTrue(false, "Error while performance data format upload");
            return false;
        }
        return true;
    }

    public Boolean validatePerformanceDataFormatTab(int entityId, String uploadFileName, CustomAssert softAssert) {

        JSONObject fileUploadDetailsJson;
        JSONObject tabListDataResponseJson;

        JSONArray indRowData;
        JSONArray dataArray = new JSONArray();

        String columnName;
        String columnValue;
        String tabListDataResponse;

        int performanceDataFormatTabId = 331;

        long timeSpent = 0L;
        long cSLPerformanceDataFormatTabTimeOut = 60000L;
        long pollingTime = 5000L;

        Boolean validationStatus = true;
        TabListData tabListData = new TabListData();

        try {
            Thread.sleep(10000);
            while (timeSpent < cSLPerformanceDataFormatTabTimeOut) {

                tabListData.hitTabListData(performanceDataFormatTabId, slEntityTypeId, entityId);
                tabListDataResponse = tabListData.getTabListDataResponseStr();

                if (JSONUtility.validjson(tabListDataResponse)) {

                    tabListDataResponseJson = new JSONObject(tabListDataResponse);
                    dataArray = tabListDataResponseJson.getJSONArray("data");

                    if (dataArray.length() >= 1) {
                        break;

                    }
                } else {
                    softAssert.assertTrue(false, "Performance Data Format Tab list Response is not a valid Json");
                    return false;
                }
                Thread.sleep(pollingTime);
                timeSpent = timeSpent + pollingTime;
            }
            //C10739
            if (uploadFileName.equalsIgnoreCase("")) {
                if (dataArray.length() != 0) {
                    softAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 0");
                    return false;
                } else return true;
            }

            if (dataArray.length() <1) {
                softAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 1");
                return false;
            }

            fileUploadDetailsJson = dataArray.getJSONObject(0);

            indRowData = JSONUtility.convertJsonOnjectToJsonArray(fileUploadDetailsJson);

            for (int i = 0; i < indRowData.length(); i++) {

                columnName = indRowData.getJSONObject(i).get("columnName").toString();

                if (columnName.equalsIgnoreCase("filename")) {
                    columnValue = indRowData.getJSONObject(i).get("value").toString().split(":;")[0];
                    if (!columnValue.equalsIgnoreCase(uploadFileName)) {
                        softAssert.assertTrue(false, "Performance Data Format Tab Upload File Name Expected and Actual values mismatch");
                        softAssert.assertTrue(false, "Expected File Name : " + uploadFileName + " Actual File Name : " + columnValue);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("createddate")) {

//                    columnValue = indRowData.getJSONObject(i).get("value").toString();
//                    String currentDate = DateUtils.getCurrentDateInMMM_DD_YYYY();
//                    String previousDate = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDate);
//
//                    if (!(columnValue.equalsIgnoreCase(currentDate) || columnValue.equalsIgnoreCase(previousDate))) {
//
//                        softAssert.assertTrue(false, "Performance Data Format Tab Date Expected and Actual values mismatch");
//                        softAssert.assertTrue(false, "Expected Date : " + currentDate + " OR " + previousDate + " Actual Date : " + columnValue);
//                        validationStatus = false;
//                    }
                }

                if (columnName.equalsIgnoreCase("createdby")) {

                    columnValue = indRowData.getJSONObject(i).get("value").toString();
                    String createdBy = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
                    if (!columnValue.equalsIgnoreCase(createdBy)) {

                        softAssert.assertTrue(false, "Performance Data Format Tab createdby field Expected and Actual values mismatch");
                        softAssert.assertTrue(false, "Expected createdby : " + createdBy + " Actual createdby : " + columnValue);
                        validationStatus = false;
                    }
                }
            }
        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while validating performance data format tab for SL ID " + entityId);
            validationStatus = false;
        }
        return validationStatus;
    }

    public Boolean uploadRawDataCSL(int cslId,String uploadFilePath,String rawDataFileName, String expectedMsg, CustomAssert softAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));

            UploadRawData uploadRawData = new UploadRawData();
            uploadRawData.hitUploadRawData(uploadFilePath, rawDataFileName, payloadMap);
            String uploadRawDataString = uploadRawData.getUploadRawDataJsonStr();

            if (uploadRawDataString.contains(expectedMsg)) {
                softAssert.assertTrue(true, "Raw data uploaded successfully on Child Service Level " + cslId);
                uploadRawDataStatus = true;
            } else {
                softAssert.assertTrue(false, "Raw data uploaded unsuccessfully on Child Service Level " + cslId);
                uploadRawDataStatus = false;
                return uploadRawDataStatus;
            }

        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while uploading raw data on Child Service Level Id " + cslId + e.getStackTrace());
            uploadRawDataStatus = false;
        }
        return uploadRawDataStatus;
    }

    public Boolean validateStructuredPerformanceDataCSL(int cSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert softAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + cSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 120000L;
        long pollingTime = 5000L;
        int structuredPerformanceDataTabId = 207;
        JSONArray dataArray = new JSONArray();

        try {
            JSONObject tabListResponseJson;

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < fileUploadTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        softAssert.assertTrue(false, "Structured Performance Data tab in Child Service Level has invalid Json Response for child service level id " + cSLId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    //Case when bulk upload is not done and no entry is expected
                    if (expectedFileName.equalsIgnoreCase("")) {
                        if (dataArray.length() == 0) {

                            softAssert.assertTrue(true, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    cSLId + "Actual : Row Doesn't exists");
                        } else {
                            softAssert.assertTrue(false, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    cSLId + "Actual : Row exists");
                            validationStatus = false;
                        }
                        return validationStatus;
                    }

                    if (dataArray.length() > 0) {

                        softAssert.assertTrue(true, "Raw Data File Upload row created");
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Raw Data File not Uploaded yet");
                    }
                }
                if (dataArray.length() == 0) {
                    softAssert.assertTrue(false, "Raw Data File not Uploaded in " + fileUploadTimeOut + " milli seconds");
                    validationStatus = false;
                    return validationStatus;
                }
            } else {
                softAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for child service level id " + cSLId);
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(15000);
            JSONObject individualRowData = dataArray.getJSONObject(0);

            JSONArray individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
            JSONObject individualColumnJson;
            String columnName;
            String columnValue;
            for (int i = 0; i < individualRowDataJsonArray.length(); i++) {

                individualColumnJson = individualRowDataJsonArray.getJSONObject(i);

                columnName = individualColumnJson.get("columnName").toString();
                columnValue = individualColumnJson.get("value").toString();

                if (columnName.equalsIgnoreCase("filename")) {

                    String fileName = columnValue.split(":;")[0];

                    if (!fileName.equalsIgnoreCase(expectedFileName)) {
                        softAssert.assertTrue(false, "Structure Performance Data File Name Expected : " + expectedFileName + " Actual File Name : " + fileName);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("status")) {

                    String status = columnValue.split(":;")[0];

                    if (!status.equalsIgnoreCase(computationStatus)) {
                        softAssert.assertTrue(false, "Structure Performance Data Status Expected : " + computationStatus + " Actual Status : " + status);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("performancedata")) {

                    if (expectedPerformanceData == null) {

                    } else {
                        String performanceData = columnValue.split(":;")[0];

                        if (!performanceData.equalsIgnoreCase(expectedPerformanceData)) {
                            softAssert.assertTrue(false, "Structure Performance Data Performance Data Expected : " + expectedPerformanceData + " Actual Status : " + performanceData);
                            validationStatus = false;
                        }
                    }
                }

                if (columnName.equalsIgnoreCase("completedby")) {

                    String completedBy = columnValue.split(":;")[0];

                    if (!completedBy.equalsIgnoreCase(expectedCompletedBy)) {
                        softAssert.assertTrue(false, "Structure Performance Data CompletedBy : " + expectedCompletedBy + " Actual completedBy : " + completedBy);
                        validationStatus = false;
                    }
                }

//                if (columnName.equalsIgnoreCase("timeofaction")) {
//
//                    String timeOfAction = columnValue.split(":;")[0];
//                    String currentDate = DateUtils.getCurrentDateInMMM_DD_YYYY();
//                    String previousDate = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDate);
//
//                    if (!(timeOfAction.contains(currentDate) || timeOfAction.contains(previousDate))) {
//                        softAssert.assertTrue(false, "Structure Performance Data TimeOfAction Expected value : " + currentDate + " Actual timeOfAction : " + timeOfAction);
//                        validationStatus = false;
//                    }
//                }
            }

        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while validating Structured Performance Data on CSL " + cSLId + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;

    }

    public Boolean validateStructuredPerformanceDataCSL(int cSLId, String expectedFileName, String computationStatus, CustomAssert softAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + cSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 120000L;
        String actualStatus = "Not Done";
        long statusTime = 8000L;
        long pollingTime = 5000L;
        int structuredPerformanceDataTabId = 207;
        String status = null;
        JSONArray dataArray = new JSONArray();

        try {
            JSONObject tabListResponseJson;

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < fileUploadTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        softAssert.assertTrue(false, "Structured Performance Data tab in Child Service Level has invalid Json Response for child service level id " + cSLId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    //Case when bulk upload is not done and no entry is expected
                    if (expectedFileName.equalsIgnoreCase("")) {
                        if (dataArray.length() == 0) {

                            softAssert.assertTrue(true, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    cSLId + "Actual : Row Doesn't exists");
                        } else {
                            softAssert.assertTrue(false, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    cSLId + "Actual : Row exists");
                            validationStatus = false;
                        }
                        return validationStatus;
                    }

                    if (dataArray.length() > 0) {
                        // false, need to change message.
                        softAssert.assertTrue(true, "Raw Data File Upload row created");
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Raw Data File not Uploaded yet");
                    }
                }
                if (dataArray.length() == 0) {
                    softAssert.assertTrue(false, "Raw Data File not Uploaded in " + fileUploadTimeOut + " milli seconds");
                    validationStatus = false;
                    return validationStatus;
                }
            } else {
                softAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for child service level id " + cSLId);
                validationStatus = false;
                return validationStatus;
            }

            while (timeSpent < fileUploadTimeOut) {

                logger.info("Putting Thread on Sleep for {} milliseconds.", statusTime);
                Thread.sleep(statusTime);

                tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId);
                tabListResponse = tabListData.getTabListDataResponseStr();
                tabListResponseJson = new JSONObject(tabListResponse);
                dataArray = tabListResponseJson.getJSONArray("data");

                JSONObject individualRowData = dataArray.getJSONObject(dataArray.length() - 1);
                JSONArray individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
                JSONObject individualColumnJson;
                String columnName;
                String columnValue;
                for (int i = 0; i < individualRowDataJsonArray.length(); i++) {

                    individualColumnJson = individualRowDataJsonArray.getJSONObject(i);

                    columnName = individualColumnJson.get("columnName").toString();
                    columnValue = individualColumnJson.get("value").toString();

                    if (columnName.equalsIgnoreCase("status")) {

                        status = columnValue.split(":;")[0];

                        if(status.equalsIgnoreCase(actualStatus)){
                            timeSpent += pollingTime;
                        }
                        else{
                            break;
                        }
                    }
                }
                if(status.equalsIgnoreCase(computationStatus)){
                    break;
                }
            }

        } catch (Exception e) {
            softAssert.assertTrue(false, "Exception while validating Structured Performance Data on CSL " + cSLId + e.getStackTrace());
            validationStatus = false;
        }

        return validationStatus;

    }

    public Boolean uploadWorkflow(){

        Boolean workflowUploadStatus = true;



        return workflowUploadStatus;

    }

    public String getStructuredPerformanceData(int cSLId) {

        logger.info("Getting Structured Performance data for CSL ID " + cSLId);
        int structuredPerformanceDataTabId = 207;

        TabListData tabListData = new TabListData();
        String payload = "{\"filterMap\":{\"entityTypeId\":" + cSLId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId, payload);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        return tabListResponse;

    }

    public String getDocumentIdFromCSLRawDataTab(String structurePerformanceData){

        String documentId = "";
        try {
            if (JSONUtility.validjson(structurePerformanceData)) {

                JSONArray structuredPerformanceDataFileDetailsJsonArray = new JSONObject(structurePerformanceData).getJSONArray("data");
                JSONArray dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJsonArray.getJSONObject(structuredPerformanceDataFileDetailsJsonArray.length() -1));
                String columnName;

                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("filename")) {
                        documentId = dataArray.getJSONObject(i).get("value").toString().split(":;")[1];
                        break;
                    }
                }
            }
        }catch (Exception e){
            logger.error("Error while getting document id " + e.getMessage());
        }
        return documentId;
    }

    public String getCSLForCurrMonth(String currentMonth, int serviceLevelId, CustomAssert customAssert){

        String cslForCurrMonth = "-1";

        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();

            int childServiceLevelTabId = 7;
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            HashMap<String,String> monthCSLIDMap = new HashMap<>();

            if (JSONUtility.validjson(tabListResponse)) {


                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);

                    }else {

                        tabListResponseJson = new JSONObject(tabListResponse);
                        dataArray = tabListResponseJson.getJSONArray("data");
                        JSONArray indRowDataJsonArray;
                        String columnName;
                        String columnValueId = "";
                        String columnValueMonth = "";

                        for(int i =0;i<dataArray.length();i++){

                            indRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

                            for(int j =0;j< indRowDataJsonArray.length();j++){

                                columnName = indRowDataJsonArray.getJSONObject(j).get("columnName").toString();

                                if(columnName.equalsIgnoreCase("reporting_date")){
                                    columnValueMonth = indRowDataJsonArray.getJSONObject(j).get("value").toString().split("-")[0];
                                }

                                if(columnName.equalsIgnoreCase("id")){
                                    columnValueId = indRowDataJsonArray.getJSONObject(j).get("value").toString().split(":;")[1];
                                }
                            }

                            monthCSLIDMap.put(columnValueMonth,columnValueId);
                        }

                        cslForCurrMonth = monthCSLIDMap.get(currentMonth);

                    }


            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + " " + e.getMessage());
        }

        return cslForCurrMonth;
    }

    public Boolean editPCQDCQUDC(int slId,String PCQ,String DCQ,String UDC,CustomAssert customAssert){

        Boolean editStatus = true;
        Edit edit = new Edit();
        String editResponse;
        try {

            Show show = new Show();
            editResponse = edit.hitEdit(slEntity,slId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values",UDC);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values",DCQ);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",PCQ);

            editResponse = edit.hitEdit(slEntity,editResponseJson.toString());

            if(editResponse.contains("success")){

                show.hitShowVersion2(slEntityTypeId,slId);

                String showResponse = show.getShowJsonStr();

                String udcAfter = ShowHelper.getValueOfField("udc",showResponse);
                String dcqAfter = ShowHelper.getValueOfField("performancedatacalculationquery",showResponse);
                String pcqAfter = ShowHelper.getValueOfField("performancecomputationcalculationquery",showResponse);

                if(!udcAfter.equalsIgnoreCase(UDC)){
                    customAssert.assertTrue(false,"UDC is not updated after SL Edit");
                    editStatus = false;
                }

                if(!dcqAfter.equalsIgnoreCase(DCQ)){
                    customAssert.assertTrue(false,"DCQ is not updated after SL Edit");
                    editStatus = false;
                }

                if(!pcqAfter.equalsIgnoreCase(PCQ)){
                    customAssert.assertTrue(false,"PCQ is not updated after SL Edit");
                    editStatus = false;
                }

            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully ");
                editStatus = false;
            }

        }catch (Exception e){
            logger.error("Exception while validating the Editing PCQ DCQ UDC  " + e.getMessage());
            editStatus = false;
        }


        return editStatus;
    }

    public Boolean validateRawDataTab(int cSLId, int rawDataLineItemSizeExcel, String excelFilePath, String excelFileName, String sheetName,int excelStartRow,int excelEndRow,int excelColumnNameColNumber, CustomAssert customAssert) {

        logger.info("Validating RawData Tab On CSL " + cSLId);
        Boolean validationStatus = true;

        try {
            HashMap<String, String> columnValuesMapExcel;
            HashMap<String, String> columnValuesMapApp;


            HashMap<Integer, HashMap<String, String>> rawDataLineItemRowValuesMap = createRawDataLineItemRowValuesMap(cSLId, rawDataLineItemSizeExcel, customAssert);
            if (rawDataLineItemRowValuesMap.size() == 0) {

                customAssert.assertTrue(false, "Either no row exists on Raw Data Tab of" +
                        "CSL or Error while  creating rawDataLineItemRowValuesMap for CSL Id " + cSLId);
                validationStatus = false;
                return validationStatus;
            }

            HashMap<Integer, HashMap<String, String>> rawDataExcelRowValuesMap = createExcelRowValuesMap(excelStartRow,excelEndRow,excelColumnNameColNumber,excelFilePath, excelFileName, sheetName, customAssert);
            if (rawDataLineItemRowValuesMap.size() == 0) {

                customAssert.assertTrue(false, "Either no row exists in Raw Data Uploaded Excel" +
                        "or Error while creating rawDataExcelRowValuesMap from Excel file " + excelFileName + "at " + excelFilePath);
                validationStatus = false;
                return validationStatus;
            }

            HashMap<String, String> columnContainingExtraValues = new HashMap<>();
            columnContainingExtraValues.put("ID", "false");
            columnContainingExtraValues.put("Active", "Yes");
            columnContainingExtraValues.put("Exception", "No");

            if (rawDataExcelRowValuesMap.size() == rawDataLineItemRowValuesMap.size()) {

                for (int i = 0; i < rawDataExcelRowValuesMap.size(); i++) {

                    columnValuesMapExcel = rawDataExcelRowValuesMap.get(i + excelStartRow);
                    columnValuesMapApp = rawDataLineItemRowValuesMap.get(i);

                    validateMapKeyValuePairs(columnValuesMapExcel, columnValuesMapApp, columnContainingExtraValues, customAssert);


                }

            } else {
                customAssert.assertTrue(false, "Number of line Items on Raw Data Tab and Excel sheet are not equal for CSL ID " + cSLId);
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Raw Data Tab on CSL ID " + cSLId);
            validationStatus = false;
        }
        return validationStatus;
    }

    private HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap(int excelRowStart,int excelRowEnd,int excelColumnNameColNumber,String excelFilePath, String excelFileName, String sheetName, CustomAssert customAssert) {

        HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap = new HashMap<>();
        HashMap<String, String> columnNameValueMap;

        try {

            List<String> excelColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, excelColumnNameColNumber);
            List<String> excelDataRowData;
            String columnName;
            for (int excelRowNum = excelRowStart; excelRowNum <= excelRowEnd; excelRowNum++) {

                excelDataRowData = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, excelRowNum);

                if (excelColumnNames.size() == excelDataRowData.size()) {
                    columnNameValueMap = new HashMap<>();
                    for (int columnCount = 0; columnCount < excelColumnNames.size(); columnCount++) {
                        columnName = excelColumnNames.get(columnCount).toUpperCase();
                        if(columnName.equals("")) {

                        }else {
                            columnNameValueMap.put(excelColumnNames.get(columnCount).toUpperCase(), excelDataRowData.get(columnCount));
                        }
                    }

                    createExcelRowValuesMap.put(excelRowNum, columnNameValueMap);
                } else {
                    customAssert.assertTrue(false, "Excel Column Name Count Different from excel row data");
                }

            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while creating excel Row Values Map " + e.getMessage());
            createExcelRowValuesMap.clear();
        }
        return createExcelRowValuesMap;
    }

    public HashMap<Integer, HashMap<String, String>> createRawDataLineItemRowValuesMap(int cSLId, int rawDataLineItemSizeExcel, CustomAssert customAssert) {

        HashMap<Integer, HashMap<String, String>> rawDataLineItemRowValuesMap = new HashMap();

        try {
            String payload = "{\"offset\":0,\"size\":" + rawDataLineItemSizeExcel + ",\"childSlaId\":" + cSLId + "}";

            SLDetails slDetails = new SLDetails();
            slDetails.hitSLDetailsGlobalList(payload);
            String SideDetailsGlobalListResponse = slDetails.getSLDetailsResponseStr();

            if (!JSONUtility.validjson(SideDetailsGlobalListResponse)) {
                customAssert.assertTrue(false, "Side Details Global List Response is not valid Json");
                rawDataLineItemRowValuesMap.clear();
                return rawDataLineItemRowValuesMap;
            }

            JSONObject SideDetailsGlobalListResponseJson = new JSONObject(SideDetailsGlobalListResponse);
            JSONArray dataArray = SideDetailsGlobalListResponseJson.getJSONArray("data");
            JSONObject rawDataLineItemJson;
            JSONArray rawDataLineItemJsonArray;
            String columnName;
            String columnValue;

            HashMap<String, String> columnNameValueMap;

            //Creating a map of raw data tab values from screen
            for (int i = 0; i < dataArray.length(); i++) {

                rawDataLineItemJson = dataArray.getJSONObject(i);

                rawDataLineItemJsonArray = JSONUtility.convertJsonOnjectToJsonArray(rawDataLineItemJson);

                columnNameValueMap = new HashMap();
                for (int j = 0; j < rawDataLineItemJsonArray.length(); j++) {

                    columnName = rawDataLineItemJsonArray.getJSONObject(j).get("columnName").toString().toUpperCase();
                    columnValue = rawDataLineItemJsonArray.getJSONObject(j).get("columnValue").toString();
                    columnNameValueMap.put(columnName, columnValue);
                }
                rawDataLineItemRowValuesMap.put(i, columnNameValueMap);
            }

        } catch (Exception e) {

            rawDataLineItemRowValuesMap.clear();
            return rawDataLineItemRowValuesMap;
        }
        return rawDataLineItemRowValuesMap;
    }

    private Boolean validateMapKeyValuePairs(HashMap<String, String> columnValuesLargerMap, HashMap<String, String> columnValuesMapSmallerMap,
                                             HashMap<String, String> extraValuesMap, CustomAssert customAssert) {

        Boolean validationStatus = true;

        String keyFromLargerMap;
        String valueFromSmallerMap;
        String valueFromLargerMap;
        String valueFromExtraValuesMap;

        try {
            for (Map.Entry<String, String> entry : columnValuesLargerMap.entrySet()) {

                keyFromLargerMap = entry.getKey();
                valueFromLargerMap = entry.getValue();

                if (columnValuesMapSmallerMap.containsKey(keyFromLargerMap)) {
                    valueFromSmallerMap = columnValuesMapSmallerMap.get(keyFromLargerMap);

                    if (!valueFromLargerMap.equalsIgnoreCase(valueFromSmallerMap)) {
                        customAssert.assertTrue(false, "Expected value of " + keyFromLargerMap + " is not equal to Actual Value");
                        customAssert.assertTrue(false, "Value from largerMap " + valueFromLargerMap + " Value from SmallerMap " + valueFromSmallerMap);
                        validationStatus = false;
                    }

                } else if (extraValuesMap.containsKey(keyFromLargerMap)) {

                    valueFromExtraValuesMap = extraValuesMap.get(keyFromLargerMap);

                    if (!valueFromLargerMap.equalsIgnoreCase(valueFromExtraValuesMap)) {
                        customAssert.assertTrue(false, "Expected value of " + keyFromLargerMap + " is not equal to Actual Value");
                        customAssert.assertTrue(false, "Value from largerMap " + valueFromLargerMap + " Value from ExtraFields " + valueFromExtraValuesMap);
                        validationStatus = false;
                    }
                } else {
                    customAssert.assertTrue(false, "Key " + keyFromLargerMap + " Not found in Excel Column Values Map Or Extra Values Map");
                }

            }
        } catch (Exception e) {
            validationStatus = false;
        }

        return validationStatus;
    }

    public JSONArray getJsonRawDataCSLTab(int cSLId, int mongoDataLength, CustomAssert customAssert) {

        HashMap<Integer, HashMap<String, String>> rawDataLineItemRowValuesMap = new HashMap();
        JSONArray dataArray = new JSONArray();

        try {
            String payload = "{\"offset\":0,\"size\":" + mongoDataLength + ",\"childSlaId\":" + cSLId + "}";

            SLDetails slDetails = new SLDetails();
            slDetails.hitSLDetailsGlobalList(payload);
            String SideDetailsGlobalListResponse = slDetails.getSLDetailsResponseStr();

            if (!JSONUtility.validjson(SideDetailsGlobalListResponse)) {
                customAssert.assertTrue(false, "Side Details Global List Response is not valid Json");
                rawDataLineItemRowValuesMap.clear();
            }

            JSONObject SideDetailsGlobalListResponseJson = new JSONObject(SideDetailsGlobalListResponse);
            dataArray = SideDetailsGlobalListResponseJson.getJSONArray("data");

        } catch (Exception e) {
            rawDataLineItemRowValuesMap.clear();
        }
        return dataArray;
    }

}
