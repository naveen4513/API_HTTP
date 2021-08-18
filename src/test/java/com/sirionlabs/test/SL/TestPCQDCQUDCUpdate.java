package com.sirionlabs.test.SL;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;


//workflow open true and  workflow close true PCQ DCQ update
//workflow open false and  workflow close false update everything
//workflow open true and  workflow close false PCQ DCQ notupdate


//workflow open false workflow close false Flow hogi
//workflow open true workflow close false Flow hogi

//workflow open true workflow close true Flow nhi hogi
public class TestPCQDCQUDCUpdate {

    private final static Logger logger = LoggerFactory.getLogger(TestPCQDCQUDCUpdate.class);

    private int slEntityTypeId = 14;
    private int cslEntityTypeId = 15;
    private String slConfigFilePath;
    private String slConfigFileName;

    int uploadIdSL_PerformanceDataTab;
    int slMetaDataUploadTemplateId;

    int serviceLevelId;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    SCPUtils scpUtils;
    ArrayList<String> childServiceLevelIds;

    String serviceLevel = "service levels";

    String dbHostName;
    String dbPortName;
    String dbName;
    String dbUserName;
    String dbPassword;

    String uploadFilePath;
    String expectedMsg;

    @BeforeClass
    public void beforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));

        String schedulerHost = ConfigureEnvironment.getEnvironmentProperty("schedulerHost");
        String schedulerUserName = ConfigureEnvironment.getEnvironmentProperty("schedulerUserName");
        String schedulerPassword = ConfigureEnvironment.getEnvironmentProperty("schedulerPassword");
        int port = 22;
        SCPUtils scpUtils = new SCPUtils(schedulerHost,schedulerUserName,schedulerPassword,port);

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassword = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));
        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));

        uploadFilePath = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadfilepath");
        expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
    }

    @Test
    public void TestCSLCreation(){

        CustomAssert customAssert = new CustomAssert();

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedataformatfilename");

        String flowToTest = "sl automation flow";

        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

        if (serviceLevelId == -1) {
            customAssert.assertTrue(false, "Unable to get active service level id");
            customAssert.assertAll();
        }


        List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath,slConfigFileName,"slactiveworkflowsteps").split("->"));
        boolean slActiveStatus = serviceLevelHelper.performWorkFlowActions(slEntityTypeId,serviceLevelId,workFlowSteps,"",customAssert);

        if(!slActiveStatus){
            customAssert.assertTrue(false,"Error while activating SL");
            customAssert.assertAll();
        }

        childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

        if (childServiceLevelIds.size() == 0) {
            customAssert.assertTrue(false, "Child Service Level not created ");
            customAssert.assertAll();
        }

        slToDelete.add(serviceLevelId);
        addCSLToDelete(childServiceLevelIds);

        if (childServiceLevelIds.size() == 0) {
            customAssert.assertTrue(false, "Child Service level not created under Service Level Id " + serviceLevelId);
            customAssert.assertAll();
        }

        if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
            customAssert.assertTrue(false, "Error while performance data file upload for Service Level 2");
        }

        if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

            customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for Service Level 2");
        }

        customAssert.assertAll();

    }

//    C10433 C10435
//workflow open true and  workflow close false PCQ DCQ update
    @Test(dependsOnMethods = "TestCSLCreation",priority = 1,enabled = true)
    public void TestUDCDCQUpdateOnSLWithWorkFlowFlagCloseFalse(){

        CustomAssert customAssert = new CustomAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassword);

        try {

            int cslId = Integer.parseInt(childServiceLevelIds.get(1));

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,serviceLevelId);

            String showResponse = show.getShowJsonStr();

            String udcBefore = ShowHelper.getValueOfField("udc",showResponse);

            String dcqBefore = ShowHelper.getValueOfField("performancedatacalculationquery",showResponse);
            String pcqBefore = ShowHelper.getValueOfField("performancecomputationcalculationquery",showResponse);

            postgreSQLJDBC.updateDBEntry("update child_sla SET workflow_open = true ,workflow_close = false where id = " + cslId + ";");

            Edit edit = new Edit();

            String editResponse = edit.hitEdit(serviceLevel,serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            String udcNow = "test12345";
            String dcqNow = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed1'){return 'No'}else{return 'Yes'}\"}}}";
            String pcqNow = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 288000){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':29]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 101; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";


            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values",udcNow);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values",dcqNow);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",pcqNow);

            editResponse = edit.hitEdit(serviceLevel,editResponseJson.toString());

            if(editResponse.contains("success")){

                show.hitShowVersion2(cslEntityTypeId,cslId);

                showResponse = show.getShowJsonStr();

                String udcAfter = ShowHelper.getValueOfField("udc",showResponse);
                String dcqAfter = ShowHelper.getValueOfField("performancedatacalculationquery",showResponse);
                String pcqAfter = ShowHelper.getValueOfField("performancecomputationcalculationquery",showResponse);

                if(udcAfter.equalsIgnoreCase(udcBefore)){
                    customAssert.assertTrue(false,"UDC is not updated even after workflow close flag is set to false");
                }

                if(dcqAfter.equalsIgnoreCase(dcqBefore)){
                    customAssert.assertTrue(false,"DCQ is not updated even after workflow close flag is set to false");
                }

                if(pcqAfter.equalsIgnoreCase(pcqBefore)){
                    customAssert.assertTrue(false,"PCQ is not updated even after workflow close flag is set to false");
                }

            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully ");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }

//    workflow open true and  workflow close true PCQ DCQ notupdate
    @Test(dependsOnMethods = "TestCSLCreation",priority = 1,enabled = true)
    public void TestUDCDCQUpdateOnSLWithWorkFlowFlagCloseTrue(){

        CustomAssert customAssert = new CustomAssert();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassword);
        try {

            int cslId = Integer.parseInt(childServiceLevelIds.get(2));

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,serviceLevelId);

            String showResponse = show.getShowJsonStr();

            String udcBefore = ShowHelper.getValueOfField("udc",showResponse);

            String dcqBefore = ShowHelper.getValueOfField("performancedatacalculationquery",showResponse);
            String pcqBefore = ShowHelper.getValueOfField("performancecomputationcalculationquery",showResponse);

            postgreSQLJDBC.updateDBEntry("update child_sla SET workflow_open = true, workflow_close = true where id = " + cslId + ";");

            Edit edit = new Edit();

            String editResponse = edit.hitEdit(serviceLevel,serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            String udcNow = "test1234567";
            String dcqNow = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed3'){return 'No'}else{return 'Yes'}\"}}}";
            String pcqNow = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 288000){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':31]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 101; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";


            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values",udcNow);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values",dcqNow);
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",pcqNow);

            editResponse = edit.hitEdit(serviceLevel,editResponseJson.toString());

            if(editResponse.contains("success")){

                show.hitShowVersion2(cslEntityTypeId,cslId);

                showResponse = show.getShowJsonStr();

                String udcAfter = ShowHelper.getValueOfField("udc",showResponse);
                String dcqAfter = ShowHelper.getValueOfField("performancedatacalculationquery",showResponse);
                String pcqAfter = ShowHelper.getValueOfField("performancecomputationcalculationquery",showResponse);

                if(!udcAfter.equalsIgnoreCase(udcBefore)){
                    customAssert.assertTrue(false,"UDC is not updated even after workflow close flag is set to false");
                }

                if(!dcqAfter.equalsIgnoreCase(dcqBefore)){
                    customAssert.assertTrue(false,"DCQ is not updated even after workflow close flag is set to false");
                }

                if(!pcqAfter.equalsIgnoreCase(pcqBefore)){
                    customAssert.assertTrue(false,"PCQ is not updated even after workflow close flag is set to false");
                }

            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully ");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
            logger.error("Exception while validating the scenario " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }


    @Test(dependsOnMethods = "TestCSLCreation",priority = 1,enabled = true)
    public void TestUDCUpdateOnSLReIndexesCSL(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassword);
        try {

            int cslId = Integer.parseInt(childServiceLevelIds.get(2));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilename");
            String completedBy = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
            String rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilesuccessmsg");

            if (!serviceLevelHelper.uploadRawDataCSL(cslId, uploadFilePath,rawDataFileName, rawDataFileValidMsg, customAssert)) {

                customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cslId);

            } else {
                if (!serviceLevelHelper.validateStructuredPerformanceDataCSL(cslId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                    customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

                }
            }

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,serviceLevelId);

            String showResponse = show.getShowJsonStr();

            String udcBefore = ShowHelper.getValueOfField("udc",showResponse);

            postgreSQLJDBC.updateDBEntry("update child_sla SET workflow_close = false where id = " + cslId + ";");

            Edit edit = new Edit();

            String editResponse = edit.hitEdit(serviceLevel,serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            String udcNow = "test12345";
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values",udcNow);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            String sqlQuery = "select id from task where entity_type_id =15 and entity_id = " + cslId + " and date_modified > " + timeStamp;

            editResponse = edit.hitEdit(serviceLevel,editResponseJson.toString());

            if(editResponse.contains("success")){

                Thread.sleep(5000);
                List<List<String>> sqlResult = postgreSQLJDBC.doSelect(sqlQuery);

                if(sqlResult.size() == 0){
                    customAssert.assertTrue(false,"No entry in task table after UDC update for reindexing");
                }

            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully ");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }


//    C10550
    @Test(dependsOnMethods = "TestCSLCreation",priority = 1,enabled = true)
    public void TestUDCUpdateHasNoChangeOnComputationStatusDataNotUploaded(){

        CustomAssert customAssert = new CustomAssert();
        try {

            int cslId = Integer.parseInt(childServiceLevelIds.get(3));

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,serviceLevelId);

            String showResponse = show.getShowJsonStr();

            String udcBefore = ShowHelper.getValueOfField("udc",showResponse);

            Edit edit = new Edit();

            String editResponse = edit.hitEdit(serviceLevel,serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            String udcNow = "test12341";
            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values",udcNow);

            editResponse = edit.hitEdit(serviceLevel,editResponseJson.toString());

            if(editResponse.contains("success")){

                show.hitShowVersion2(cslEntityTypeId,cslId);

                showResponse = show.getShowJsonStr();

                String computationStatus =ShowHelper.getValueOfField("computationstatus",showResponse);

                if(!computationStatus.equalsIgnoreCase("Data Not Uploaded")){
                    customAssert.assertTrue(true,"Computation status should be Data Not Uploaded");
                }


            }else {
                customAssert.assertTrue(false,"Edit done unsuccessfully ");
            }

        }catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();
    }

    private void addCSLToDelete(ArrayList<String> cslToDeleteList){

        try {
            for (String cslIDToDelete : cslToDeleteList) {
                cslToDelete.add(Integer.parseInt(cslIDToDelete));
            }
        }catch (Exception e){
            logger.error("Error while adding child service level to deleted list");
        }
    }

    @AfterClass
    public void AfterClass(){

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }

    private Boolean uploadPerformanceDataFormat(int entityId, int uploadId, int templateId, String performanceDataFormatFilePath, String performanceDataFormatFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Performance Data Format on " + entityId);
        UploadBulkData uploadBulkData = new UploadBulkData();

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("parentEntityId", String.valueOf(entityId));
        payloadMap.put("parentEntityTypeId", String.valueOf(slEntityTypeId));

        uploadBulkData.hitUploadBulkData(uploadId, templateId, performanceDataFormatFilePath, performanceDataFormatFileName, payloadMap);

        String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

        if (!fileUploadResponse.contains(expectedMsg)) {

            customAssert.assertTrue(false, "Error while performance data format upload or SL Template Upload message has been changed");
            return false;
        }
        return true;
    }

    //    C10565 C10638
    private Boolean validatePerformanceDataFormatTab(int entityId, String uploadFileName, CustomAssert customAssert) {

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
            Thread.sleep(3000);
            while (timeSpent < cSLPerformanceDataFormatTabTimeOut) {

                tabListData.hitTabListData(performanceDataFormatTabId, slEntityTypeId, entityId,0,50,"id","asc");

                tabListDataResponse = tabListData.getTabListDataResponseStr();

                if (JSONUtility.validjson(tabListDataResponse)) {

                    tabListDataResponseJson = new JSONObject(tabListDataResponse);
                    dataArray = tabListDataResponseJson.getJSONArray("data");

                    if (dataArray.length() >= 1) {
                        break;

                    }
                } else {
                    customAssert.assertTrue(false, "Performance Data Format Tab list Response is not a valid Json");
                    return false;
                }
                Thread.sleep(pollingTime);
                timeSpent = timeSpent + pollingTime;
            }
            //C10739
            if (uploadFileName.equalsIgnoreCase("")) {
                if (dataArray.length() != 0) {
                    customAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 0");
                    return false;
                } else return true;
            }

//			if (dataArray.length() != 1) {
//				customAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 1");
//				return false;
//			}

            fileUploadDetailsJson = dataArray.getJSONObject(0);

            indRowData = JSONUtility.convertJsonOnjectToJsonArray(fileUploadDetailsJson);

            for (int i = 0; i < indRowData.length(); i++) {

                columnName = indRowData.getJSONObject(i).get("columnName").toString();

                if (columnName.equalsIgnoreCase("filename")) {
                    columnValue = indRowData.getJSONObject(i).get("value").toString().split(":;")[0];
                    if (!columnValue.equalsIgnoreCase(uploadFileName)) {

                        customAssert.assertTrue(false, "Performance Data Format Tab Upload File Name Expected and Actual values mismatch");
                        customAssert.assertTrue(false, "Expected File Name : " + uploadFileName + " Actual File Name : " + columnValue);validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("createddate")) {

                    columnValue = indRowData.getJSONObject(i).get("value").toString();

//					if (!(columnValue.equalsIgnoreCase(currentDateIn_MMM_DD_YYYY_Format) || columnValue.equalsIgnoreCase(previousDateIn_MMM_DD_YYYY_Format)
//							|| columnValue.contains(nextDateIn_MMM_DD_YYYY_Format))) {
//
//						customAssert.assertTrue(false, "Performance Data Format Tab Date Expected and Actual values mismatch");
//						customAssert.assertTrue(false, "Expected Date : " + currentDateIn_MMM_DD_YYYY_Format + " OR " + previousDateIn_MMM_DD_YYYY_Format
//								+ " OR " + nextDateIn_MMM_DD_YYYY_Format + " Actual Date : " + columnValue);
//						validationStatus = false;
//					}
                }

            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating performance data format tab for SL ID " + entityId + " " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC;

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
}
