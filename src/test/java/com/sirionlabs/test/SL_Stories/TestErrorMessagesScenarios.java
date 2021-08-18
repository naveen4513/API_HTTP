package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestErrorMessagesScenarios {

    private final static Logger logger = LoggerFactory.getLogger(TestErrorMessagesScenarios.class);
    private String slConfigFilePath;
    private String slConfigFileName;
    int slEntityTypeId = 14;
    int cslEntityTypeId = 15;
    int uploadIdSL_PerformanceDataTab;
    int slMetaDataUploadTemplateId;
    SCPUtils scpUtils;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();
    ArrayList<String> childServiceLevelIds;
    private String uploadFilePath;
    private String completedBy;
    private String rawDataFileValidMsg;

    int serviceLevelId;

    String dbHostAddress;
    String dbName;
    String dbPort;
    String dbUserName;
    String dbPassword;

    @BeforeClass
    public void beforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");
        uploadFilePath = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadfilepath");
        completedBy = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "completedby");
        rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilesuccessmsg");

        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));

        String schedulerHost = ConfigureEnvironment.getEnvironmentProperty("schedulerHost");
        String schedulerUserName = ConfigureEnvironment.getEnvironmentProperty("schedulerUserName");
        String schedulerPassword = ConfigureEnvironment.getEnvironmentProperty("schedulerPassword");
        int port = 22;
        scpUtils = new SCPUtils(schedulerHost,schedulerUserName,schedulerPassword,port);

        dbHostAddress = "192.168.2.157";
        dbPort = "5432";
        dbName = "letterbox-sl";
        dbUserName = "postgres";
        dbPassword = "postgres";
    }

    @Test(enabled = true)
    public void TestCSLCreation() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly

//		BA1
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"@SUM(state.map.total, doc['Time Taken (Seconds)']);@SUMIF(state.map.totalif, doc['Time Taken (Seconds)'], gt(doc['Time Taken (Seconds)'], 1));\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0, 'total':0, 'totalif':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator': 0, 'Actual_Denominator': 0, 'Supplier_Numerator': 0, 'Supplier_Denominator': 0, 'Final_Numerator': 0, 'Final_Denominator': 0];@FOREACH(a, states, SUM(params.result.Final_Denominator, a.map.totalif), SUM(params.result.Final_Numerator, a.map.total));if (params.result.Final_Denominator <= 100) {params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;}return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue \": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";

        try {

          serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
          slToDelete.add(serviceLevelId);

            if (serviceLevelId != -1) {

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if(!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps,"", customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                }

                childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                addCSLToDelete(childServiceLevelIds);
                slToDelete.add(serviceLevelId);
            }

        } catch (Exception e) {
            logger.error("Exception while creation of CSL");
            customAssert.assertTrue(false, "Exception while creation of CSL" + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 0, description = "MetaData Template Duplicate Headers")
    public void TestMetaDataTemplateDuplicateHeaderColumns(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try {

            int slId = serviceLevelId;
            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "Scenario1.xlsm";
            int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(1,1000);
            String copiedFileName = "Scenario1_" + randomNumber + ".xlsm";

            FileUtils.copyFile(uploadFilePath,slTemplateFileName,uploadFilePath,copiedFileName);


            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,copiedFileName,"200",customAssert
                    );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }
            Thread.sleep(5000);
            String remoteFilePath = "/data/temp-session";

            String localDir = uploadFilePath + "\\";

            String localFileName = copiedFileName;

            Boolean scpStatus = scpUtils.getFileFromRemoteServerToLocalServer(remoteFilePath,copiedFileName,localDir,localFileName);

            if(scpStatus){
                try {
                    XLSUtils xlsUtils = new XLSUtils(uploadFilePath, localFileName);
                    String sheetName = "Format Sheet";
                    String errorMsg = xlsUtils.getCellData(sheetName, 5, 6);

                    if (!errorMsg.equalsIgnoreCase("Duplicate 'Column Name'")) {
                        customAssert.assertTrue(false, "Expected Error Message Duplicate header found " +
                                "Actual Error Message " + errorMsg + " are not same");
                    }
                }catch (Exception e){
                    customAssert.assertTrue(false,"Exception while validating excel");
                }finally {
                    FileUtils.deleteFile(uploadFilePath, localFileName);
                }
            }else {
                customAssert.assertTrue(false,"File SCP Done unsuccessfully");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating MetaDataTemplate Duplicate Header Columns");
        }

        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 1, description = "Computed Column missing in DCQ C13565")
    public void TestComputedColumnMissingORMismatchInDCQ(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try {

            int slId = serviceLevelId;
            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "Scenario2SLTemplate.xlsm";
            int randomNumbers = RandomNumbers.getRandomNumberWithinRangeIndex(1,10000);

            String copiedFileName = "Scenario2SLTemplate_" + randomNumbers + ".xlsm";


            FileUtils.copyFile(uploadFilePath,slTemplateFileName,uploadFilePath,copiedFileName);


            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,copiedFileName,"200",customAssert
            );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }

            Boolean templateUploadOnSL = serviceLevelHelper.validatePerformanceDataFormatTab(slId,copiedFileName,customAssert);

            if(!templateUploadOnSL){
                customAssert.assertTrue(false,"SL Template uploaded unsuccessfully");
            }

            FileUtils.deleteFile(uploadFilePath,copiedFileName);
            int cslId = Integer.parseInt(childServiceLevelIds.get(1));

            String slRawDataFileName = "Scenario2RawDataFile.xlsx";
            randomNumbers = RandomNumbers.getRandomNumberWithinRangeIndex(1,10000);
            copiedFileName = "Scenario2RawDataFile_" + randomNumbers + ".xlsx";

            FileUtils.copyFile(uploadFilePath,slRawDataFileName,uploadFilePath,copiedFileName);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean rawDataFileUploadStatus = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,copiedFileName,"200",customAssert);

            if(rawDataFileUploadStatus){
                Boolean structureFormatDataStatus = serviceLevelHelper.validateStructuredPerformanceDataCSL(cslId,copiedFileName,"Done With Error",customAssert);

                if(!structureFormatDataStatus){
                    customAssert.assertTrue(false,"Structured Performance Data Tab On CSL Computed unsuccessfully");
                }
            }else {
                customAssert.assertTrue(false,"Raw Data File Not Uploaded");
            }

            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C13535 C13536 C13531
            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("DCQ script for Computed Column 'Valid Issue' not found.");
            expectedSentencesInBody.add("Error Details: Error in executing script to compute 'Valid Issue'");
            expectedSentencesInBody.add("Error Type: Processing Error");

            String body = recordFromSystemEmailTable.get(0).get(2);

            for(String expectedSentence : expectedSentencesInBody ){

                if(!body.contains(expectedSentence)){
                    customAssert.assertTrue(false,expectedSentence + "not found in the error email ");
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Computed Column Missing or Mismatch");
        }
        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 2, description = "Date Format Mismatch In Computed Column C13560 C13561")
    public void TestDateFormatMismatchInComputedColumnAndRawData(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        int slId = serviceLevelId;

        String dcqNew = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Status\": {\"script\": \"def newFormat = new SimpleDateFormat('yyyy/MM/dd');def oldFormat = new SimpleDateFormat(\\\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\\\");def date = oldFormat.parse(doc['Created'].value.toString());return newFormat.format(date);\"}}}";

        if(!editFields("performanceDataCalculationQuery",dcqNew)){
            customAssert.assertTrue(false,"DCQ edited unsuccessfully");
        }
        try{

            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "Scenario3SLTemplate.xlsm";

            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,slTemplateFileName,"200",customAssert
            );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }

            int cslId = Integer.parseInt(childServiceLevelIds.get(3));

            String slRawDataFileName = "Scenario3RawDataFile.xlsx";

            int randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);

            String copiedFileName = "Scenario3RawDataFile" + randomNumber + ".xlsx";

            FileUtils.copyFile(uploadFilePath,slRawDataFileName,uploadFilePath,copiedFileName);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean rawDataFileUploadStatus = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,copiedFileName,"200",customAssert);

            if(rawDataFileUploadStatus){
                Boolean structureFormatDataStatus = serviceLevelHelper.validateStructuredPerformanceDataCSL(cslId,copiedFileName,"Done With Error",customAssert);

                if(!structureFormatDataStatus){
                    customAssert.assertTrue(false,"Structured Performance Data Tab On CSL Computed unsuccessfully");
                }

            }else {
                customAssert.assertTrue(false,"Raw Data File Not Uploaded");
            }
            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);
            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Either DateTime format mismatch in script vs. template or data type mismatch for Computed Column");

            String body = recordFromSystemEmailTable.get(0).get(2);

            for (String expectedSentence : expectedSentencesInBody) {

                if (!body.contains(expectedSentence)) {
                    customAssert.assertTrue(false, expectedSentence + "not found in the error email ");
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Date Format Mismatch In Computed Column And RawData");
        }finally {
            String dcqOld = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Status\": {\"script\": \"def newFormat = new SimpleDateFormat('yyyy/MM/dd');def oldFormat = new SimpleDateFormat(\\\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\\\");def date = oldFormat.parse(doc['Created'].value.toString());return newFormat.format(date);\"}}}";

            if(!editFields("performanceDataCalculationQuery",dcqOld)){
                customAssert.assertTrue(false,"DCQ edited unsuccessfully");
            }
        }
        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 3, description = "Data Type Mismatch C13560 C13561 ")
    public void TestDataTypeMismatchForComputedColumnsInTemplateAndDCQ(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try{
            String dcqNew = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Created\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";

            if(!editFields("performanceDataCalculationQuery",dcqNew)){
                customAssert.assertTrue(false,"DCQ edited unsuccessfully");
            }

            int slId = serviceLevelId;
            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "Scenario4SLTemplate.xlsm";

            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,slTemplateFileName,"200",customAssert
            );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }

            int cslId = Integer.parseInt(childServiceLevelIds.get(4));
            String slRawDataFileName = "Scenario4RawDataFile.xlsx";
            int randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);

            String copiedFileName = "Scenario4RawDataFile_" + randomNumber + ".xlsx";

            FileUtils.copyFile(uploadFilePath,slRawDataFileName,uploadFilePath,copiedFileName);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);
            Boolean rawDataFileUploadStatus = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,copiedFileName,"200",customAssert);
            FileUtils.deleteFile(uploadFilePath,copiedFileName);
            if(rawDataFileUploadStatus){
                Boolean structureFormatDataStatus = serviceLevelHelper.validateStructuredPerformanceDataCSL(cslId, copiedFileName, "Done With Error", customAssert);

                if (!structureFormatDataStatus) {
                    customAssert.assertTrue(false, "Structured Performance Data Tab On CSL Computed unsuccessfully");
                }
            }else {
                customAssert.assertTrue(false,"Raw Data File Not Uploaded");
            }

            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);
            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }else {

                List<String> expectedSentencesInBody = new ArrayList<>();
                expectedSentencesInBody.add("Either DateTime format mismatch in script vs. template or data type mismatch for Computed Column");

                String body = recordFromSystemEmailTable.get(0).get(2);

                for (String expectedSentence : expectedSentencesInBody) {

                    if (!body.contains(expectedSentence)) {
                        customAssert.assertTrue(false, expectedSentence + "not found in the error email ");
                    }
                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Data Type Mismatch In Raw Data Template");
        }finally {
            String dcqOld = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue \": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";
            if(!editFields("performanceDataCalculationQuery",dcqOld)){
                customAssert.assertTrue(false,"DCQ edited unsuccessfully");
            }
        }

        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 4, description = "C13538 Exception in Sirion Computed Column")
    public void TestExceptionInSirionComputedColumnProcessing(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try{
            int slId = serviceLevelId;

            String dcqNew = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Status\": {\"script\": \"def newFormat = new SimpleDateFormat('yyyy/MM/dd');def oldFormat = new SimpleDateFormat(\\\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\\\");def date = oldFormat.parse(doc['Created'].value.toString());return newFormat.format(date);\"}}}";

            if(!editFields("performanceDataCalculationQuery",dcqNew)){
                customAssert.assertTrue(false,"DCQ edited unsuccessfully");
            }

            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "Scenario6SLTemplate.xlsm";

            int randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);

            String copiedFileName = "Scenario6SLTemplate_" + randomNumber + ".xlsm";

            FileUtils.copyFile(uploadFilePath,slTemplateFileName,uploadFilePath,copiedFileName);


            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,copiedFileName,"200",customAssert
            );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }

            FileUtils.deleteFile(uploadFilePath,copiedFileName);
            int cslId = Integer.parseInt(childServiceLevelIds.get(6));

            String slRawDataFileName = "Scenario6RawDataFile.xlsx";

            randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);
            copiedFileName = "Scenario6RawDataFile_" + randomNumber + ".xlsx";

            FileUtils.copyFile(uploadFilePath,slRawDataFileName,uploadFilePath,copiedFileName);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean rawDataFileUploadStatus = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,copiedFileName,"200",customAssert);

            if (rawDataFileUploadStatus) {
                Boolean structureFormatDataStatus = serviceLevelHelper.validateStructuredPerformanceDataCSL(cslId, copiedFileName, "Done With Error", customAssert);

                if (!structureFormatDataStatus) {
                    customAssert.assertTrue(false, "Structured Performance Data Tab On CSL Computed unsuccessfully");
                }


            } else {
                customAssert.assertTrue(false, "Raw Data File Not Uploaded");
            }

            FileUtils.deleteFile(uploadFilePath,copiedFileName);

            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);
            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }else {


                List<String> expectedSentencesInBody = new ArrayList<>();
                expectedSentencesInBody.add("Column Working Hours Does Not Exist.");
                expectedSentencesInBody.add("Column Time Zone Does Not Exist.");
                expectedSentencesInBody.add("Error Details: Error in Sirion Computed Function column 'Time Taken (Seconds)':");
                expectedSentencesInBody.add("Column Week Type Does Not Exist.");
                expectedSentencesInBody.add("Error Type: Processing Error");

                String body = recordFromSystemEmailTable.get(0).get(2);

                for (String expectedSentence : expectedSentencesInBody) {

                    if (!body.contains(expectedSentence)) {
                        customAssert.assertTrue(false, expectedSentence + "not found in the error email ");
                    }
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Processing Error in Sirion Computed Columns");
        }

        customAssert.assertAll();
    }

    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 5, description = "C13563 Raw Data File Hidden Sheet")
    public void TestRawDataFileHiddenSheet(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        try{
            int slId = serviceLevelId;

            String dcqNew = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Status\": {\"script\": \"def newFormat = new SimpleDateFormat('yyyy/MM/dd');def oldFormat = new SimpleDateFormat(\\\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\\\");def date = oldFormat.parse(doc['Created'].value.toString());return newFormat.format(date);\"}}}";

            if(!editFields("performanceDataCalculationQuery",dcqNew)){
                customAssert.assertTrue(false,"DCQ edited unsuccessfully");
            }

            String uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\ErrorMessagesScenarios";
            String slTemplateFileName = "PerformanceDataFormat.xlsm";

            int randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);

            String copiedFileName = "PerformanceDataFormat_" + randomNumber + ".xlsm";

            FileUtils.copyFile(uploadFilePath,slTemplateFileName,uploadFilePath,copiedFileName);

            Boolean uploadStatus = serviceLevelHelper.uploadPerformanceDataFormat(slId,
                    uploadIdSL_PerformanceDataTab,slMetaDataUploadTemplateId,
                    uploadFilePath,copiedFileName,"200",customAssert
            );

            if(!uploadStatus){
                customAssert.assertTrue(false,"Upload Performed unsuccessfully of sl template");
            }

            FileUtils.deleteFile(uploadFilePath,copiedFileName);
            int cslId = Integer.parseInt(childServiceLevelIds.get(7));

            String slRawDataFileName = "RawDataFileHiddenSheet.xlsx";

            randomNumber = RandomNumbers.getRandomNumberWithinRange(1,10000);
            copiedFileName = "RawDataFileHiddenSheet_" + randomNumber + ".xlsx";

            FileUtils.copyFile(uploadFilePath,slRawDataFileName,uploadFilePath,copiedFileName);

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            Boolean rawDataFileUploadStatus = serviceLevelHelper.uploadRawDataCSL(cslId,uploadFilePath,copiedFileName,"200",customAssert);

            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cslId);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);
            String subjectLine = "Error in pre-processing raw data file (" +shortCodeId+ " - " + description+ ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
            }else {


                List<String> expectedSentencesInBody = new ArrayList<>();
                expectedSentencesInBody.add("Raw Data Upload Error - file has irrelevant sheets");

                String body = recordFromSystemEmailTable.get(0).get(2);

                for (String expectedSentence : expectedSentencesInBody) {

                    if (!body.contains(expectedSentence)) {
                        customAssert.assertTrue(false, expectedSentence + "not found in the error email ");
                    }
                }

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Raw Data File Hidden Scenario.");
        }

        customAssert.assertAll();
    }

//    C10440  C10484 C13529 C10775 C10423
    @Test(groups = {"sanity","PCQ DCQ UDC Update1"}, dependsOnMethods = "TestCSLCreation",enabled = true,priority = 6, description = "PCQ Error and Error Email C10484")
    public void TestCSLCompStatus_ErrorInComputation() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow error in computation";
        Show show = new Show();

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
        String UDC = "Incident ID";

        int childserviceLevelId1ErrorInComputation = -1;

        try {

//			serviceLevelId1ErrorInComputation = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedataformatfilename");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
            }

//				childserviceLevelId1ErrorInComputation = Integer.parseInt(childserviceLevelId1sErrorInComputation.get(1));
            childserviceLevelId1ErrorInComputation = Integer.parseInt(childServiceLevelIds.get(8));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilename");

            if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
                customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
            }

            if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
            }

            List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "cslerrorincomputationworkflowsteps").split("->"));

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            //Performing workflow Actions till Error in computation
            if (!performWorkFlowActions(cslEntityTypeId, childserviceLevelId1ErrorInComputation, workFlowSteps, completedBy, customAssert)) {
                customAssert.assertTrue(false, "Error while performing workflow actions on CSL ID " + childserviceLevelId1ErrorInComputation);
            }

            Thread.sleep(15000);
            long timeSpent = 0;
            long fileUploadTimeOut = 120000L;
            long pollingTime = 5000L;

            String computationStatus;
            String showPageResponse;

            while (timeSpent < fileUploadTimeOut) {
                logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                Thread.sleep(pollingTime);

                show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
                showPageResponse = show.getShowJsonStr();
                computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                if (!computationStatus.equalsIgnoreCase("Error in Computation")) {
                    timeSpent += pollingTime;
                } else {
                    customAssert.assertTrue(true, "Computation Status validated successfully");
                    break;
                }

            }

            show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423
            String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for Error in performance computation for CSLID " + shortCodeId);
            }

            if (!validateSubjectLine(recordFromSystemEmailTable, subjectLine, customAssert)) {
                customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

//					if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//						customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for CSL Raw Data Metadata Upload for SL " + shortCodeId);
//					}

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for SL ID " + serviceLevelId + "and CSL ID " + childserviceLevelId1ErrorInComputation + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(groups = {"sanity","PCQ DCQ UDC Update1"}, dependsOnMethods = "TestCSLCreation",enabled = true,priority = 7, description = "PCQ Error and Error Email C10484")
    public void TestCSLCompStatus_DCQSyntaxError() {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"matcheee\": {\"childslaId\": \"childSLAId\"}}]}}}";
        String UDC = "Incident ID";

        int childserviceLevelId1ErrorInComputation = -1;

        try {

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedataformatfilename");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
            }

            childserviceLevelId1ErrorInComputation = Integer.parseInt(childServiceLevelIds.get(9));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilename");

            String timeStamp = getCurrentTimeStamp().get(0).get(0);

            if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {

                customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
            }

            if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done With Error", null, completedBy, customAssert)) {
                customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
            }

            Thread.sleep(15000);
            show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);

            show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
            String showResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
            String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423
            String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for Error in performance computation for CSLID " + shortCodeId);
            }

            List<String> expectedSentencesInBody = new ArrayList<>();
            expectedSentencesInBody.add("Error Type: Syntax Error");

            String body = recordFromSystemEmailTable.get(0).get(2);

            for (String expectedSentence : expectedSentencesInBody) {

                if (!body.contains(expectedSentence)) {
                    customAssert.assertTrue(false, expectedSentence + "not found in the error email ");
                }
            }

            if (!validateSubjectLine(recordFromSystemEmailTable, subjectLine, customAssert)) {
                customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for SL ID " + serviceLevelId + "and CSL ID " + childserviceLevelId1ErrorInComputation + e.getMessage());
        }

        customAssert.assertAll();
    }

//    C10423
    @Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = true, priority = 8, description = "DCQ Error and Error Email C10423")    //Completed
    public void TestCSLStatus_DCQInError() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow error in computation";
        Show show = new Show();
        int serviceLevelId1ErrorInComputation = serviceLevelId;
        int childserviceLevelId1ErrorInComputation = -1;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query123\": {\"bool\": {\"must\": [{\"match\": {\"childslaId12345\": \"childSLAId\"}}]}}}";

        try {

            String UDC = "Incident ID";
            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            if (serviceLevelId1ErrorInComputation != -1) {

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
                if (!uploadPerformanceDataFormat(serviceLevelId1ErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelId1ErrorInComputation, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
                }

                childserviceLevelId1ErrorInComputation = Integer.parseInt(childServiceLevelIds.get(10));

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilename");

                String timeStamp = getCurrentTimeStamp().get(0).get(0);
                Thread.sleep(5000);
                if (childserviceLevelId1ErrorInComputation != -1) {

                    if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
                        customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
                    }

                    if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done With Error", null, completedBy, customAssert)) {
                        customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
                    }

                    show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
                    String showResponse = show.getShowJsonStr();

                    String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
                    String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C13535 C13536 C13531
                    String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

                    List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
                    if (recordFromSystemEmailTable.size() == 0) {
                        customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
                    }

                    if (!validateSubjectLine(recordFromSystemEmailTable, subjectLine, customAssert)) {
                        customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
                    }

                } else {
                    customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelId1ErrorInComputation);
                }

            } else {
                customAssert.assertTrue(false, "Service level id equals to -1");
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for SL ID " + serviceLevelId1ErrorInComputation + "and CSL ID " + childserviceLevelId1ErrorInComputation + e.getMessage());
        }

        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
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

    private boolean editFields(String fieldName,String fieldValue){

        Edit edit = new Edit();
        Boolean editStatus = true;
        try {

            String serviceLevel = "service levels";
            String editResponse = edit.hitEdit(serviceLevel, serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).put("values", fieldValue);


            editResponse = edit.hitEdit(serviceLevel, editResponseJson.toString());

            if(!editResponse.contains("success")){
                editStatus = false;
            }
        }catch (Exception e){
            editStatus = false;
        }

        return editStatus;
    }

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

//        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '" + subjectLine + "' AND date(date_created) >= "  + "'" + currentTimeStamp + "'"
//                + "order by id desc";

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress,dbPort,dbName,dbUserName,dbPassword);
        String sqlQuery = "select subject,sent_successfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
                + "order by id desc";
        List<List<String>> queryResult = null;
        try {
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);

        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }finally {
//			postgreSQLJDBC.closeConnection();
        }
        return queryResult;

    }

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try {
            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
    }

    private Boolean validateSubjectLine(List<List<String>> recordFromSystemEmailTable, String expectedSubjectLine, CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {
            String actualSubjectLine = recordFromSystemEmailTable.get(0).get(0);

            if (expectedSubjectLine == null && actualSubjectLine == null) {
                return true;
            } else if (expectedSubjectLine == null && actualSubjectLine != null) {
                return false;
            }

            if (!actualSubjectLine.equalsIgnoreCase(expectedSubjectLine)) {

                customAssert.assertTrue(false, "Subject Line validated unsuccessfully from system emails table");
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Subject Line from system email table with the expected attachment name");
            validationStatus = false;
        }

        return validationStatus;
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
            Thread.sleep(10000);
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

                if (columnName.equalsIgnoreCase("createdby")) {

                    columnValue = indRowData.getJSONObject(i).get("value").toString();

                    String createdBy = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
                    if (!columnValue.equalsIgnoreCase(createdBy)) {

                        customAssert.assertTrue(false, "Performance Data Format Tab created by field Expected and Actual values mismatch");
                        customAssert.assertTrue(false, "Expected createdby : " + createdBy + " Actual createdby : " + columnValue);
                        validationStatus = false;
                    }
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating performance data format tab for SL ID " + entityId + " " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user, CustomAssert customAssert) {

        Boolean workFlowStepActionStatus;
        String actionNameAuditLog;
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                //workFlowStepActionStatus = workflowActionsHelper.performWorkflowAction(entityTypeId, entityId, workFlowStepToBePerformed);
                workFlowStepActionStatus = workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStepToBePerformed,customAssert);

                if (!workFlowStepActionStatus) {

                    customAssert.assertTrue(false, "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId);
                    return false;
                } else {
                    actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(slConfigFilePath, slConfigFileName, "auditlogactioname", workFlowStepToBePerformed);
                    if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, customAssert)) {
                        customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    private Boolean verifyAuditLog(int entityTypeId, int entityId, String actionNameExpected, String user, CustomAssert customAssert) {

        logger.info("Validating Audit Log Tab for entity type id " + entityTypeId + " and entity id " + entityId);

        int AuditLogTabId = 61;

        Boolean validationStatus = true;
//		int expectedValidationChecksOnAuditLogTab = 5;
        int expectedValidationChecksOnAuditLogTab = 3;
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

            tabListData.hitTabListData(AuditLogTabId, entityTypeId, entityId);
            tabListDataResponse = tabListData.getTabListDataResponseStr();

            if (APIUtils.validJsonResponse(tabListDataResponse)) {

                tabListDataResponseJson = new JSONObject(tabListDataResponse);
                dataArray = tabListDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"No entry exists in Audit Log Tab for entity id " + entityId + " entity type id " + entityTypeId);
                    return false;
                }

//				latestActionRow = dataArray.getJSONObject(0);
                latestActionRow = dataArray.getJSONObject(dataArray.length() - 1);
                latestActionRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(latestActionRow);

                for (int i = 0; i < latestActionRowJsonArray.length(); i++) {

                    columnName = latestActionRowJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = latestActionRowJsonArray.getJSONObject(i).get("value").toString();

                    switch (columnName) {

                        case "action_name":

                            if (!columnValue.equalsIgnoreCase(actionNameExpected)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab action_name is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected action_name : " + actionNameExpected + " Actual action_name : " + columnValue);
                                validationStatus = false;
                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "requested_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab requested_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected requested_by : " + actionNameExpected + " Actual requested_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "completed_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab completed_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected completed_by : " + actionNameExpected + " Actual completed_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

//						case "audit_log_date_created":
//
//							if (!(columnValue.contains(currentDateIn_MM_DD_YYYY_Format) || columnValue.contains(previousDateIn_MM_DD_YYYY_Format) || columnValue.contains(nextDateIn_MM_DD_YYYY_Format))) {
//
//								customAssert.assertTrue(false, "Under Audit Log Tab audit_log_date_created is validated unsuccessfully for entity id " + entityId);
//								customAssert.assertTrue(false, "Expected audit_log_date_created : " + currentDateIn_MM_DD_YYYY_Format + " OR " + previousDateIn_MM_DD_YYYY_Format
//										+ " OR " + nextDateIn_MM_DD_YYYY_Format + " Actual audit_log_date_created : " + columnValue);
//								validationStatus = false;
//
//							} else {
//								actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
//							}
//							break;
//
//						case "audit_log_user_date":
//
//							if (!(columnValue.contains(currentDateIn_MM_DD_YYYY_Format) || columnValue.contains(previousDateIn_MM_DD_YYYY_Format) || columnValue.contains(nextDateIn_MM_DD_YYYY_Format))) {
//
//								customAssert.assertTrue(false, "Under Audit Log Tab audit_log_user_date is validated unsuccessfully for entity id " + entityId);
//								customAssert.assertTrue(false, "Expected audit_log_user_date : " + currentDateIn_MM_DD_YYYY_Format + " OR " + previousDateIn_MM_DD_YYYY_Format
//										+ " OR " + nextDateIn_MM_DD_YYYY_Format + " Actual audit_log_user_date : " + columnValue);
//								validationStatus = false;
//							} else {
//								actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
//							}
//							break;
                    }

                }

                if (actualValidationChecksOnAuditLogTab == expectedValidationChecksOnAuditLogTab) {
                    customAssert.assertTrue(true, "All validation checks passed successfully");
                } else {
                    customAssert.assertTrue(false, "Validation check count not equal to " + expectedValidationChecksOnAuditLogTab);
                    validationStatus = false;
                }
            } else {
                customAssert.assertTrue(false, "Audit Log Tab Response is not a valid json for entity id " + entityId);
                validationStatus = false;
            }


        } catch (Exception e) {
            logger.error("Exception while validating tab list response " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating tab list response " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean uploadRawDataCSL(int cslId, String rawDataFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));
            payloadMap.put("_csrf_token", "9e2897d6-7c08-493d-bfb4-31ab887e5ce8");

            XLSUtils.updateColumnValue(uploadFilePath,rawDataFileName,"Sheet1",100,200,"");


            UploadRawData uploadRawData = new UploadRawData();
            uploadRawData.hitUploadRawData(uploadFilePath, rawDataFileName, payloadMap);
            String uploadRawDataString = uploadRawData.getUploadRawDataJsonStr();

//            if (uploadRawDataString.contains("200:;basic:;Your request has been successfully submitted")) {
            if (uploadRawDataString.contains("200:;")) {
                customAssert.assertTrue(true, "Raw data uploaded successfully on Child Service Level " + cslId);
                uploadRawDataStatus = true;
            } else {
                customAssert.assertTrue(false, "Raw data uploaded unsuccessfully on Child Service Level " + cslId + "");
                uploadRawDataStatus = false;
                return uploadRawDataStatus;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while uploading raw data on Child Service Level Id " + cslId + " " + e.getMessage());
            uploadRawDataStatus = false;
        }
        return uploadRawDataStatus;
    }

    private Boolean validateStructuredPerformanceDataCSL(int CSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert customAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 80000L;
        long pollingTime = 5000L;
        int structuredPerformanceDataTabId = 207;
        JSONArray dataArray = new JSONArray();

        try {
            JSONObject tabListResponseJson;

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < fileUploadTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Structured Performance Data tab in Child Service Level has invalid Json Response for child service level id " + CSLId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    //Case when bulk upload is not done and no entry is expected
                    if (expectedFileName.equalsIgnoreCase("")) {
                        if (dataArray.length() == 0) {

                            customAssert.assertTrue(true, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    CSLId + "Actual : Row Doesn't exists");
                        } else {
                            customAssert.assertTrue(false, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    CSLId + "Actual : Row exists");
                            validationStatus = false;
                        }
                        return validationStatus;
                    }

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Raw Data File Upload row created");
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Raw Data File not Uploaded yet");
                    }
                }
                if (dataArray.length() == 0) {
                    customAssert.assertTrue(false, "Raw Data File not Uploaded in " + fileUploadTimeOut + " milli seconds");
                    validationStatus = false;
                    return validationStatus;
                }
            } else {
                customAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for service level id " + serviceLevelId);
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(15000);
            JSONArray individualRowDataJsonArray = null;
            JSONObject individualColumnJson;
            String columnName;
            String columnValue;

            while (timeSpent < fileUploadTimeOut) {

                logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                Thread.sleep(pollingTime);

                tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
                tabListResponse = tabListData.getTabListDataResponseStr();
                tabListResponseJson = new JSONObject(tabListResponse);
                dataArray = tabListResponseJson.getJSONArray("data");
                JSONObject individualRowData = dataArray.getJSONObject(dataArray.length() - 1);
                individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
                columnValue = individualRowDataJsonArray.getJSONObject(0).getString("value").split(":;")[0];
                if(columnValue.equalsIgnoreCase(computationStatus)){
                    break;
                }else {
                    timeSpent += pollingTime;
                }
            }

            for (int i = 0; i < individualRowDataJsonArray.length(); i++) {

                individualColumnJson = individualRowDataJsonArray.getJSONObject(i);

                columnName = individualColumnJson.get("columnName").toString();
                columnValue = individualColumnJson.get("value").toString();


                if (columnName.equalsIgnoreCase("filename")) {

                    if (expectedFileName.equalsIgnoreCase("Check for Snow File")) {

                        String fileName = columnValue.split(":;")[0];

                        if (fileName ==null) {

                            customAssert.assertTrue(false, "Structure Performance Data File Name Expected : " + expectedFileName + " Actual File Name : " + fileName);
                            validationStatus = false;
                        }

                    } else {

                        String fileName = columnValue.split(":;")[0];

                        if (!fileName.equalsIgnoreCase(expectedFileName)) {
                            customAssert.assertTrue(false, "Structure Performance Data File Name Expected : " + expectedFileName + " Actual File Name : " + fileName);
                            validationStatus = false;
                        }
                    }
                }

                if (columnName.equalsIgnoreCase("status")) {

                    String status = columnValue.split(":;")[0];

                    if (!status.equalsIgnoreCase(computationStatus)) {
                        customAssert.assertTrue(false, "Structure Performance Data Status Expected : " + computationStatus + " Actual Status : " + status);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("performancedata")) {

                    if (expectedPerformanceData == null) {

                    } else {
                        String performanceData = columnValue.split(":;")[0];

                        if (!performanceData.equalsIgnoreCase(expectedPerformanceData)) {
                            customAssert.assertTrue(false, "Structure Performance Data Performance Data Expected : " + expectedPerformanceData + " Actual Status : " + performanceData);
                            validationStatus = false;
                        }
                    }
                }

                if (columnName.equalsIgnoreCase("completedby")) {

                    String completedBy = columnValue.split(":;")[0];

                    if (!completedBy.equalsIgnoreCase(expectedCompletedBy)) {
                        customAssert.assertTrue(false, "Structure Performance Data CompletedBy : " + expectedCompletedBy + " Actual completedBy : " + completedBy);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("timeofaction")) {

                    String timeOfAction = columnValue.split(":;")[0];
//					String currentDate = DateUtils.getCurrentDateInMMM_DD_YYYY();
//					String previousDate = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDate);
//					String nextDate = DateUtils.getNextDateInDDMMYYYY(currentDate);

//					if (!(timeOfAction.contains(currentDateIn_MMM_DD_YYYY_Format) || timeOfAction.contains(previousDateIn_MMM_DD_YYYY_Format) || timeOfAction.contains(nextDateIn_MMM_DD_YYYY_Format))) {
//						customAssert.assertTrue(false, "Structure Performance Data TimeOfAction Expected value : " + currentDateIn_MMM_DD_YYYY_Format + " Actual timeOfAction : " + timeOfAction);
//						validationStatus = false;
//					}
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Structured Performance Data on CSL " + CSLId + " " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;
    }
}
