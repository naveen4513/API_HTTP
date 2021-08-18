package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.servicedata.TblauditlogsFieldHistory;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

public class TestServiceLevelExtraTestCases {

    private final static Logger logger = LoggerFactory.getLogger(TestChildServiceLevelAutomation.class);

    private String slCreationConfigFilePath = null;
    private String slCreationConfigFileName = null;
    private String extraFieldsConfigFilePath = null;
    private String extraFieldsConfigFileName = null;
    private String configFilePath = null;
    private String configFileName = null;

    private String slEntity = "service levels";
    private String cslEntity = "child service levels";
    private String uploadFilePath;
    private String downloadFilePath;
    private String bulkUpdateFilename;

    private String rawDataFileName;
    private String auditLogUser;
    private String adminUser;
    private String rawDataFileValidMsg;
    private Integer slEntityTypeId;
    private Integer cslEntityTypeId;
    private Integer contractEntityTypeId;
    private Integer supplierEntityTypeId;
    private int serviceLevelId;
    private int cslBulkUpdateTemplateId;
    private int slMetaDataUploadTemplateId;
    private int uploadIdSL_PerformanceDataTab;
    private final int childServiceLevelTabId = 7;

    private ArrayList<String> childServiceLevelIds = new ArrayList<>();
    private ArrayList<String> childServiceLevelIdsForFlowDownOfValues = new ArrayList<>();

    private String slTemplateFileName;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    PostgreSQLJDBC postgreSQLJDBC;

    private String currentDateIn_MM_DD_YYYY_Format;
    private String previousDateIn_MM_DD_YYYY_Format;
//    private String nextDateIn_MM_DD_YYYY_Format;

    private String currentDateIn_MMM_DD_YYYY_Format;
    private String previousDateIn_MMM_DD_YYYY_Format;
    private String nextDateIn_MMM_DD_YYYY_Format;

    private String[] workFlowActionToPerformCSLComputePerformance;
    private List<List<String>> currentTimeStampForSLTemplateUploadCSLCreation;

    static int randomFileNumber = 1;

    @BeforeClass(groups = {"sanity"})
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        slCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");

        extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");
        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");
        contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
        supplierEntityTypeId = ConfigureConstantFields.getEntityIdByName("suppliers");

        uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadfilepath");
        downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
        bulkUpdateFilename = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkupdatefilename");
        cslBulkUpdateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslbulkupdatetemplateid"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));
        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
        rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
        auditLogUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
        adminUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminuser");
        rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilesuccessmsg");
        slTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

        postgreSQLJDBC = new PostgreSQLJDBC();

        try {
            postgreSQLJDBC.deleteDBEntry("delete from system_emails");
        } catch (Exception e) {
            logger.error("Exception while deleting from system_emails table");
        }

        currentDateIn_MM_DD_YYYY_Format = DateUtils.getCurrentDateInMM_DD_YYYY();
        previousDateIn_MM_DD_YYYY_Format = DateUtils.getPreviousDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);
//        nextDateIn_MM_DD_YYYY_Format = DateUtils.getNextDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);

        currentDateIn_MMM_DD_YYYY_Format = DateUtils.getCurrentDateInMMM_DD_YYYY();
        previousDateIn_MMM_DD_YYYY_Format = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);
//        nextDateIn_MMM_DD_YYYY_Format = DateUtils.getNextDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);

        workFlowActionToPerformCSLComputePerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslperformancecalulation").split("->");

    }

    //C10772 C10773 C10776
    @Test(groups = {"sanity"},enabled = true,priority = 0)          //Validated 24 Sept
    public void TestBulkUploadRawDataCSL() {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        Download download = new Download();

        String unzipFilePath = downloadFilePath + "/" + "BulkUploadRawDataFilesUnZipped";

        String flowToTest = "sl automation flow";
        int numberOfCSLForBulkUpload = 5;
        int csl;
        String showPageResponse;
        String shortCodeId;
        String description;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        int serviceLevelBulkUpload;
        String BulkUploadFileNameExcel = "BulkUploadRawData.xlsx";

        try {

            serviceLevelBulkUpload = getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

            slToDelete.add(serviceLevelBulkUpload);

            if (serviceLevelBulkUpload != -1) {
//
                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
//				//Performing workflow Actions till Active
//
                if (!performWorkFlowActions(slEntityTypeId, serviceLevelBulkUpload, workFlowSteps, auditLogUser,false, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelBulkUpload);
                    customAssert.assertAll();
                }


                ArrayList<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelBulkUpload, customAssert);
                if (childServiceLevelIds.size() == 0) {
                    customAssert.assertTrue(false, "Child Service Level not created ");
                    customAssert.assertAll();
                }

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenamebulkupload");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

                currentTimeStampForSLTemplateUploadCSLCreation = getCurrentTimeStamp();

                if (!uploadPerformanceDataFormat(serviceLevelBulkUpload, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelBulkUpload, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                }


                addCSLToDelete(childServiceLevelIds);

                List<String> childServiceLevelIdsForBulkUpload = new ArrayList<>();
                List<String> childServiceLevelShortCodeIdsList = new ArrayList<>();
                List<String> childServiceLevelIdsForNonBulkUpload = new ArrayList<>();
                List<String> childServiceLevelDescList = new ArrayList<>();

                if (childServiceLevelIds.size() == 0) {

                    customAssert.assertTrue(false, "Child Service Level Ids are not created with in the expected time frame for SL ID " + serviceLevelBulkUpload);
                    customAssert.assertAll();
                }

                for (int i = 0; i < childServiceLevelIds.size(); i++) {

                    if (i < numberOfCSLForBulkUpload) {
                        csl = Integer.parseInt(childServiceLevelIds.get(i));

                        childServiceLevelIdsForBulkUpload.add(childServiceLevelIds.get(i));

                        show.hitShowVersion2(cslEntityTypeId, csl);
                        showPageResponse = show.getShowJsonStr();

                        shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);
                        description = ShowHelper.getValueOfField("description", showPageResponse);

                        childServiceLevelShortCodeIdsList.add(shortCodeId);
                        childServiceLevelDescList.add(description);


                    } else {
                        childServiceLevelIdsForNonBulkUpload.add(childServiceLevelIds.get(i));
                    }

                }

                String bulkUploadRawDataZipFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadrawdatazipfilename");
                String currDate = DateUtils.getCurrentDateInAnyFormat("dd-MMM-yyyy");
                String bulkUploadRawDataZipDir = bulkUploadRawDataZipFileName.replace("date", currDate);

                bulkUploadRawDataZipFileName = bulkUploadRawDataZipFileName + ".zip";

                String downloadFilePathBulkUpload = downloadFilePath + "/" + bulkUploadRawDataZipFileName;

                download.downloadBulkTemplateRawData(downloadFilePathBulkUpload, childServiceLevelIdsForBulkUpload);
                FileUtils.clearDirectory(unzipFilePath);
                FileUtils.unzip(downloadFilePathBulkUpload, unzipFilePath);

                if (!validateDownloadedBulkTemplateRawDataWithUploadedSlTemplateRawData(unzipFilePath, bulkUploadRawDataZipDir,performanceDataFormatFileName, childServiceLevelIdsForBulkUpload,
                        childServiceLevelShortCodeIdsList, childServiceLevelDescList, customAssert)) {
                    customAssert.assertTrue(false, "validate Downloaded BulkTemplate RawData With Uploaded SlTemplate RawData failed");
                }

                HashMap<String, String> renamedFilesMap = new HashMap<>();


                String newFileNameAfterRename = "BulkUploadRawData.xlsx";

                renamedFilesMap = renameFolderAndFiles(unzipFilePath, renamedFilesMap, newFileNameAfterRename, customAssert);

                randomFileNumber = 1;

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"rawdatafilename_bulkuploadrawdata");

                for (Map.Entry<String, String> entry : renamedFilesMap.entrySet()) {

                    try {
                        XLSUtils rawDataExcel = new XLSUtils(uploadFilePath, rawDataFileName);

                        rawDataExcel.copySheetFromOneExcelToAnother("Sheet1", "Data Sheet",
                                entry.getKey(), entry.getValue());

                    } catch (Exception e) {

                        logger.error("Exception while copying single Raw Data Upload File To Bulk Upload Raw Data Files");
                        customAssert.assertTrue(false, "Exception while copying single Raw Data Upload File To Bulk Upload Raw Data Files " + e.getMessage());
                    }

                }

//				XLSUtils rawDataExcelBulkUpload = new XLSUtils(uploadFilePath, rawDataFileName);
//				rawDataExcelBulkUpload.copySheetFromOneExcelToAnother("Sheet1", "Sheet1", uploadFilePath,
//						BulkUploadFileNameExcel);


                File unzipFilePathDir = new File(unzipFilePath);
                String zipFileName = "abc" + ".zip";
                FileUtils.zipDir(unzipFilePath + "\\" + unzipFilePathDir.list()[0], downloadFilePath + "\\" + zipFileName);

                Thread.sleep(3000);

                UploadRawData uploadRawData = new UploadRawData();
                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("entityTypeId", String.valueOf(cslEntityTypeId));
                payloadMap.put("upload", "Submit");

                uploadRawData.hitBulkUploadRawData(downloadFilePath, zipFileName, payloadMap);

                if (!uploadRawData.getUploadRawDataJsonStr().contains("200")) {
                    customAssert.assertTrue(false, "Bulk Upload Raw Data File Uploaded Unsuccessfully");

                }
                Thread.sleep(300000);

//                C10776
                int cslID;
                int k =1;
                String rawDataFileToCheck = BulkUploadFileNameExcel.split(".xlsx")[0];

                for (String cslIdForBulkUploadValidation : childServiceLevelIdsForBulkUpload) {

                    cslID = Integer.parseInt(cslIdForBulkUploadValidation);
                    String rawDataFile = rawDataFileToCheck + k + ".xlsx";

                    if (!validateStructuredPerformanceDataCSL(cslID, rawDataFile, "Done", "View Structured Data", auditLogUser, customAssert)) {
                        customAssert.assertTrue(false, "Performance Data Tab Validated Unsuccessfully for CSL ID " + cslIdForBulkUploadValidation);
                    }

                    if (!validateRawDataTab(cslID, 11, uploadFilePath, BulkUploadFileNameExcel, "Sheet1", customAssert)) {
                        customAssert.assertTrue(false, "Raw Data tab validated unsuccessfully for CSL ID " + cslIdForBulkUploadValidation);
                    }

//                    C10777
                    if (!verifyAuditLog(cslEntityTypeId, cslID, "Performance data uploaded", adminUser, customAssert)) {
                        customAssert.assertTrue(false, "Audit Log validated unsuccessfully after bulk upload of raw data");
                    }

                    k++;
                }

                for (String cslIdForNonBulkUploadValid : childServiceLevelIdsForNonBulkUpload) {

                    if (!validateStructuredPerformanceDataCSL(Integer.parseInt(cslIdForNonBulkUploadValid), "",
                            "", "", "", customAssert)) {
                        customAssert.assertTrue(false, "Structure Performance Data Tab Validated unsuccessfully for CSL ID "
                                + cslIdForNonBulkUploadValid);
                    }
                }

                String[] invalidFileTypesBulkUpload = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadrawdatafileinvalidtypes").split(",");

                for (String invalidFileName : invalidFileTypesBulkUpload) {
//                    C10772
                    //Checking the non supported file type scenario
                    uploadRawData.hitBulkUploadRawData(downloadFilePath, invalidFileName, payloadMap);

                    if (!uploadRawData.getUploadRawDataJsonStr().contains("500")) {
                        customAssert.assertTrue(false, "While uploading invalid file types 500 error is not there");

                    }
                }

                //Checking ignore file scenario
                cslID = Integer.parseInt(childServiceLevelIdsForBulkUpload.get(0));
                String timeStamp = getCurrentTimeStamp().get(0).get(0);
                if (!validateIgnoreFileScenario(cslID, customAssert)) {
                    customAssert.assertTrue(false, "Ignore File Scenario validated unsuccessfully for CSL ID " + childServiceLevelIdsForBulkUpload.get(0));
                } else {
                    show.hitShowVersion2(cslEntityTypeId, cslID);
                    showPageResponse = show.getShowJsonStr();

                    //C13573
                    shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);

//                  C13573 C10613 C13573
                    String subjectLine = "SL Computation (" + shortCodeId + ") -  ignore raw data file request response";
                    List<String> expectedSentencesInBody = new ArrayList<>();
                    expectedSentencesInBody.add("To calculate the updated scores, please re-compute the performance.");
                    expectedSentencesInBody.add("Your ignore raw data file request has been completed.");

                    List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

                    if (recordFromSystemEmailTable.size() == 0) {
                        customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
                    }

                    if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
                        customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                    }

//					if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//						customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for subjectLine " + subjectLine);
//					}

                    if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                        customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating SL Met Status " + e.getMessage());
        }

        customAssert.assertAll();

    }

    //C10440  C10484 C13529 C10775 C10423
    @Test(enabled = true, priority = 0)
    public void TestCSLCompStatus_ErrorInComputation() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow error in computation";
        Show show = new Show();
        int serviceLevelIdErrorInComputation = -1;

        ArrayList<String> childServiceLevelIdsErrorInComputation = null;
        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        try {
            serviceLevelIdErrorInComputation = getActiveServiceLevelId(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

            Thread.sleep(5000);

            slToDelete.add(serviceLevelIdErrorInComputation);

            if (serviceLevelIdErrorInComputation != -1) {

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
                if (!uploadPerformanceDataFormat(serviceLevelIdErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelIdErrorInComputation, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
                }

                childServiceLevelIdsErrorInComputation = checkIfCSLCreatedOnServiceLevel(serviceLevelIdErrorInComputation, customAssert);

                if (childServiceLevelIdsErrorInComputation.size() == 0) {
                    customAssert.assertTrue(false, "Child Service Level not created ");
//					customAssert.assertAll();
                }

                addCSLToDelete(childServiceLevelIdsErrorInComputation);

                int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                if (childServiceLevelIdsErrorInComputation.size() != numberOfChildServiceLevel) {

                    customAssert.assertTrue(false, "For Service Level Id " + serviceLevelIdErrorInComputation + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIds.size());
                    customAssert.assertAll();
                }

                int childServiceLevelIdErrorInComputation = -1;

                childServiceLevelIdErrorInComputation = Integer.parseInt(childServiceLevelIdsErrorInComputation.get(0));

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

                if (childServiceLevelIdErrorInComputation != -1) {

                    if (!uploadRawDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
                        customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childServiceLevelIdErrorInComputation);
//						customAssert.assertAll();
                    }

                    if (!validateStructuredPerformanceDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
                        customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childServiceLevelIdErrorInComputation);
                    }

                    List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));

                    String timeStamp = getCurrentTimeStamp().get(0).get(0);

                    //Performing workflow Actions till Error in computation
                    if (!performWorkFlowActions(cslEntityTypeId, childServiceLevelIdErrorInComputation, workFlowSteps, auditLogUser, customAssert)) {
                        customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelIdErrorInComputation);
                        customAssert.assertAll();
                    }
                    Thread.sleep(15000);
                    show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                    String showPageResponse = show.getShowJsonStr();

                    String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                    if (!computationStatus.equalsIgnoreCase("Error in Computation")) {

                        customAssert.assertTrue(false, "Computation Status Expected \"Error in Computation\" Actual Computation Status " + computationStatus);

                    } else {
                        customAssert.assertTrue(true, "Computation Status validated successfully");
                    }

                    show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                    String showResponse = show.getShowJsonStr();

                    String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
                    String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C10483
                    String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + description + ")";

                    List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
                    if (recordFromSystemEmailTable.size() == 0) {
                        customAssert.assertTrue(false, "No entry in system email table for Error in performance computation for CSL ID" + shortCodeId);
                    }

                    if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
                        customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
                    }

//					if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//						customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for CSL Raw Data Metadata Upload for SL " + shortCodeId);
//					}

                } else {
                    customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelIdErrorInComputation);
                }

            } else {
                customAssert.assertTrue(false, "Unable to create service level id ");
            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for CSL ID " + serviceLevelIdErrorInComputation + "and CSL ID " + childServiceLevelIdsErrorInComputation + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C10423
    @Test(enabled = true)    //Completed
    public void TestCSLStatus_DCQInError() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow error in computation";
        Show show = new Show();
        int serviceLevelIdErrorInComputation = -1;

        ArrayList<String> childServiceLevelIdsErrorInComputation = null;
        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query123\": {\"bool\": {\"must\": [{\"match\": {\"childslaId12345\": \"childSLAId\"}}]}}}";

        try {
            serviceLevelIdErrorInComputation = getActiveServiceLevelId(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
            Thread.sleep(5000);
            if (serviceLevelIdErrorInComputation != -1) {

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
                if (!uploadPerformanceDataFormat(serviceLevelIdErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
                }

                if (!validatePerformanceDataFormatTab(serviceLevelIdErrorInComputation, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
                }

                childServiceLevelIdsErrorInComputation = checkIfCSLCreatedOnServiceLevel(serviceLevelIdErrorInComputation, customAssert);

                if (childServiceLevelIdsErrorInComputation.size() == 0) {
                    customAssert.assertTrue(false, "Child Service Level not created ");
                    customAssert.assertAll();
                }

                addCSLToDelete(childServiceLevelIdsErrorInComputation);

                int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                if (childServiceLevelIdsErrorInComputation.size() != numberOfChildServiceLevel) {

                    customAssert.assertTrue(false, "For Service Level Id " + serviceLevelIdErrorInComputation + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIds.size());
                    customAssert.assertAll();
                }

                int childServiceLevelIdErrorInComputation = -1;

                childServiceLevelIdErrorInComputation = Integer.parseInt(childServiceLevelIdsErrorInComputation.get(0));

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

                String timeStamp = getCurrentTimeStamp().get(0).get(0);
                Thread.sleep(5000);
                if (childServiceLevelIdErrorInComputation != -1) {

                    if (!uploadRawDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
                        customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childServiceLevelIdErrorInComputation);
//						customAssert.assertAll();
                    }

                    if (!validateStructuredPerformanceDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, "Done With Error", null, auditLogUser, customAssert)) {
                        customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childServiceLevelIdErrorInComputation);
                    }

                    show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                    String showResponse = show.getShowJsonStr();

                    String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
                    String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C13535 C13536 C13531
                    String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

                    List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
                    if (recordFromSystemEmailTable.size() == 0) {
                        customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
                    }

                    if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
                        customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
                    }

//                    if(!validateSentSuccessfullyFlag(recordFromSystemEmailTable,customAssert)){
//                        customAssert.assertTrue(false,"Sent Successfully Flag validated unsuccessfully in system emails table for CSL Raw Data Metadata Upload for SL " + shortCodeId);
//                    }

//					List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));
//
//					//Performing workflow Actions till Error in computation
//					if (!performWorkFlowActions(cslEntityTypeId, childServiceLevelIdErrorInComputation, workFlowSteps, auditLogUser, customAssert)) {
//						customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelIdErrorInComputation);
//						customAssert.assertAll();
//					}

                    performComputationCSL(childServiceLevelIdErrorInComputation,customAssert);

                    show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                    String showPageResponse = show.getShowJsonStr();

                    String computationStatus = ShowHelper.getValueOfField("performancestatus", showPageResponse);

                    if (!computationStatus.equalsIgnoreCase("Computation Approved")) {

                        customAssert.assertTrue(false, "Computation Status Expected \"Computation Approved\" Actual Computation Status " + computationStatus);

                    } else {
                        customAssert.assertTrue(true, "Computation Status validated successfully");
                    }

                } else {
                    customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelIdErrorInComputation);
                }

            } else {
                customAssert.assertTrue(false, "Service level id equals to -1");
            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for CSL ID " + serviceLevelIdErrorInComputation + "and CSL ID " + childServiceLevelIdsErrorInComputation + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C10544		Workflow to be corrected for BA1 Env  1 oct
    @Test(enabled = false)
    public void TestESQueryUpdateWhenCompStatusErrorInComputation() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow error in computation";
        Show show = new Show();
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        String showPageResponse;

        int serviceLevelIdErrorInComputation = -1;

        ArrayList<String> childServiceLevelIdsErrorInComputation = null;
        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        try{

            serviceLevelIdErrorInComputation = getActiveServiceLevelId(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

            if (serviceLevelIdErrorInComputation != -1) {

                slToDelete.add(serviceLevelIdErrorInComputation);
                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

                if (!uploadPerformanceDataFormat(serviceLevelIdErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
                }

                if (!validatePerformanceDataFormatTab(serviceLevelIdErrorInComputation, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId);
                }

                childServiceLevelIdsErrorInComputation = checkIfCSLCreatedOnServiceLevel(serviceLevelIdErrorInComputation, customAssert);

                if (childServiceLevelIdsErrorInComputation.size() == 0) {
                    customAssert.assertTrue(false, "Child Service Level not created ");
                    customAssert.assertAll();
                }

                addCSLToDelete(childServiceLevelIdsErrorInComputation);

                int childServiceLevelIdErrorInComputation = -1;

                childServiceLevelIdErrorInComputation = Integer.parseInt(childServiceLevelIdsErrorInComputation.get(0));

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

                if (childServiceLevelIdErrorInComputation != -1) {

                    if (!uploadRawDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
                        customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childServiceLevelIdErrorInComputation);
//						customAssert.assertAll();
                    }

                    if (!validateStructuredPerformanceDataCSL(childServiceLevelIdErrorInComputation, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
                        customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childServiceLevelIdErrorInComputation);
                    }

                    List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));

                    //Performing workflow Actions till Error in computation
                    if (!performComputationCSL(childServiceLevelIdErrorInComputation,customAssert)) {
                        customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelIdErrorInComputation);
//						customAssert.assertAll();
                    }

                    Thread.sleep(55000);

                    show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                    showPageResponse = show.getShowJsonStr();

                    String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                    if (!computationStatus.equalsIgnoreCase("Error in Computation")) {

                        customAssert.assertTrue(false, "Computation Status Expected \"Error in Computation\" Actual Computation Status " + computationStatus);
//						customAssert.assertAll();
                    }else {

                        logger.info("Checking DCQ PCQ UDC when CSL in Error in Computation " + childServiceLevelIdErrorInComputation);

                        //After The status Data Marked for Computation updating the PCQ
                        String PCQUpdated = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':0, 'integer':'abcd']; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 4;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                        if (updatePCQonMSL(serviceLevelIdErrorInComputation, PCQUpdated, customAssert)) {

                            if (!checkDCQPCQUDCOnCSL(childServiceLevelIdErrorInComputation, DCQ, PCQUpdated, "Number", customAssert)) {
                                customAssert.assertTrue(false, "DCQ PCQ validated unsuccessfully on CSL ID " + childServiceLevelIdErrorInComputation);
                            }
                        }else {
                            customAssert.assertTrue(false,"PCQ updated unsuccessfully on SL ID " + serviceLevelIdErrorInComputation);
                        }

                        String workFlowActionToPerform = "ReComputePerformance";
                        if (!workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, childServiceLevelIdErrorInComputation, workFlowActionToPerform,customAssert)) {
                            customAssert.assertTrue(false, "Unable to perform recompute performance for the entity " + childServiceLevelIdErrorInComputation);

                        }

                        Thread.sleep(55000);

                        show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                        showPageResponse = show.getShowJsonStr();

                        computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                        if (!computationStatus.equals("Data Marked for Computation")) {
                            customAssert.assertTrue(false, "Expected status is Data Marked for Computation after PCQ update after 55000 ms");
                        }

                        workFlowActionToPerform = "ApproveComputation";
                        if (!workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, childServiceLevelIdErrorInComputation, workFlowActionToPerform,customAssert)) {
                            customAssert.assertTrue(false, "Unable to perform recompute performance for the entity " + childServiceLevelIdErrorInComputation);
                            customAssert.assertAll();
                        }
                        Thread.sleep(55000);

                        show.hitShowVersion2(cslEntityTypeId, childServiceLevelIdErrorInComputation);
                        showPageResponse = show.getShowJsonStr();

                        computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                        if (!computationStatus.equals("Computation Completed Successfully")) {

                            customAssert.assertTrue(false, "Expected status is Computation Completed Successfully after PCQ update");
                        }else {
                            PCQUpdated = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':7]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 8;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                            if (updatePCQonMSL(serviceLevelIdErrorInComputation, PCQUpdated, customAssert)) {

                                if (!checkDCQPCQUDCOnCSL(childServiceLevelIdErrorInComputation, DCQ, PCQUpdated, "Number", customAssert)) {
                                    customAssert.assertTrue(false, "DCQ PCQ validated unsuccessfully on CSL ID " + childServiceLevelIdErrorInComputation);
                                }else {
                                    logger.info("DCQ PCQ UDC validated successfully on CSL " + childServiceLevelIdErrorInComputation + " after Computation Completed Successfully");
                                }
                            }else {
                                logger.error("PCQ updated unsuccessfully on SL ID " + serviceLevelIdErrorInComputation);
                                customAssert.assertTrue(false,"PCQ updated unsuccessfully on SL ID " + serviceLevelIdErrorInComputation);
                            }
                        }
                    }

                }else {
                    logger.error("Child Service Level ID = -1 ");
                    customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelIdErrorInComputation);
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating ESQuery Update When CompStatus ErrorInComputation " + e.getMessage());
            customAssert.assertTrue(false,"Exception while validating ESQuery Update When CompStatus ErrorInComputation " + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(enabled = true)
    public void TestReadMetaDataViaEsQuery(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "sl automation flow";

        String flowToFindInConfigFile = "read metadata via es query";

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}},\"script_fields\":{\"Service Module Name Check\":{\"script\":\"if(params['_source']['Service Module Name Test'] == '${Title}'){return 'Supported'}else{return 'NA'}\"}}}";

        int serviceLevelId;

        try {
            serviceLevelId = 8393;
//            serviceLevelId = getActiveServiceLevelId(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
////			serviceLevelId = 8105;
//            if(serviceLevelId ==- 1){
//                customAssert.assertTrue(false,"Unable to create Active Service Level Id");
//                customAssert.assertAll();
//            }

            ArrayList<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

            if(childServiceLevelIds.size() == 0){

                logger.error("Child Service level size equal to zero");
                customAssert.assertTrue(false,"Child Service level size equal to zero");
            }

            Thread.sleep(5000);
            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToFindInConfigFile,"sltemplatefile");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

            String fileUploadPath = uploadFilePath + "/SIR_1088";

            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, fileUploadPath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }


            int childServiceLevelId = Integer.parseInt(childServiceLevelIds.get(1));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToFindInConfigFile,"rawdatafile");

            Thread.sleep(5000);
            if (!uploadRawDataCSL(childServiceLevelId,fileUploadPath, rawDataFileName, rawDataFileValidMsg, customAssert)) {
                customAssert.assertTrue(false, "Raw Data upload unsuccessful on CSL ID " + childServiceLevelId);
            }

            String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
            if (!validateStructuredPerformanceDataCSL(childServiceLevelId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

            Thread.sleep(5000);

            String structurePerformanceDataResponse = serviceLevelHelper.getStructuredPerformanceData(childServiceLevelId);
            String documentId = serviceLevelHelper.getDocumentIdFromCSLRawDataTab(structurePerformanceDataResponse);

            String payload = "{\"documentId\":" + documentId + ",\"offset\":0,\"size\":20,\"childSlaId\":" + childServiceLevelId +"}";

            SLDetails slDetails = new SLDetails();
            slDetails.hitSLDetailsList(payload);
            String slDetailsResponse = slDetails.getSLDetailsResponseStr();

            if(JSONUtility.validjson(slDetailsResponse)){

                JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
                JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
                JSONObject indRow;
                JSONArray indRowArray;
                String columnName;
                String columnValue = "";

                for(int i =0;i<2;i++){

                    try {
                        indRow = dataArray.getJSONObject(i);

                        indRowArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);

                        //This logic is build in such a way that Raw data file must contain 2 rows only
                        // and 1st row should have "supported" text for "Check" fields and "NA" for 2 nd Row
                        for(int j =0;j<indRowArray.length();j++) {

                            columnName = indRowArray.getJSONObject(j).get("columnName").toString();

                            if (columnName.contains("Check")) {
                                columnValue = indRowArray.getJSONObject(j).get("columnValue").toString();

                                if (i == 0) {
                                    if (!columnValue.equalsIgnoreCase("Supported")) {
                                        customAssert.assertTrue(false, "Expected values of in row 1 for calculated values should be Supported " +
                                                "Either you have uploaded the wrong raw data file for row 1 or there is a bug");
                                    }
                                } else if (i == 1) {
                                    if (!columnValue.equalsIgnoreCase("NA")) {
                                        customAssert.assertTrue(false, "Expected values of in row 2 for calculated values should be NA " +
                                                "Either you have uploaded the wrong raw data file for row 2 or there is a bug");
                                    }
                                }
                            }
                        }

                    }catch (Exception e){
                        logger.warn("Number of data while opening view structure data is less than 2");
                        customAssert.assertTrue(false,"Number of data while opening view structure data is less than 2");
                    }
                }


            }else {
                logger.error("SL Details Response is not a valid json");
                customAssert.assertTrue(false,"SL Details Response is not a valid json");
            }

        }catch (Exception e){
            logger.error("Exception while Read MetaData Via Es Query " + e.getMessage());
        }

        customAssert.assertAll();
    }


    @AfterClass(groups = {"sanity"})
    public void afterClass() {

        postgreSQLJDBC.closeConnection();

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
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
                    actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "auditlogactioname", workFlowStepToBePerformed);
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

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user,Boolean checkAuditLogTab, CustomAssert customAssert) {

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
                    if(checkAuditLogTab) {
                        actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "auditlogactioname", workFlowStepToBePerformed);
                        if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, customAssert)) {
                            customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    //    C10565 C10686 C44282
    private Boolean uploadPerformanceDataFormat(int entityId, int uploadId, int templateId, String performanceDataFormatFilePath, String performanceDataFormatFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Performance Data Format on " + entityId);
        UploadBulkData uploadBulkData = new UploadBulkData();

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("parentEntityId", String.valueOf(entityId));
        payloadMap.put("parentEntityTypeId", String.valueOf(slEntityTypeId));

        uploadBulkData.hitUploadBulkData(uploadId, templateId, performanceDataFormatFilePath, performanceDataFormatFileName, payloadMap);

        String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

        if (!fileUploadResponse.contains("200:;")) {

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
                        customAssert.assertTrue(false, "Expected File Name : " + uploadFileName + " Actual File Name : " + columnValue);
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

                    String createdBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
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

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childServiceLevelIds = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

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

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childServiceLevelIds = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childServiceLevelIds.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + " " + e.getMessage());
        }

        return childServiceLevelIds;
    }

    //    C10636  C10638
    private Boolean uploadRawDataCSL(int cslId, String rawDataFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));

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

    private Boolean uploadRawDataCSL(int cslId,String uploadFilePath, String rawDataFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));

            UploadRawData uploadRawData = new UploadRawData();
            uploadRawData.hitUploadRawData(uploadFilePath, rawDataFileName, payloadMap);
            String uploadRawDataString = uploadRawData.getUploadRawDataJsonStr();

//            if (uploadRawDataString.contains("200:;basic:;Your request has been successfully submitted")) {
            if (uploadRawDataString.contains("200:;")) {
                customAssert.assertTrue(true, "Raw data uploaded successfully on Child Service Level " + cslId);
                uploadRawDataStatus = true;
            } else {
                customAssert.assertTrue(false, "Raw data uploaded unsuccessfully on Child Service Level " + cslId);
                uploadRawDataStatus = false;
                return uploadRawDataStatus;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while uploading raw data on Child Service Level Id " + cslId + " " + e.getMessage());
            uploadRawDataStatus = false;
        }
        return uploadRawDataStatus;
    }

    //    C10638
    private Boolean validateStructuredPerformanceDataCSL(int CSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert customAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 120000L;
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

            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
            tabListResponse = tabListData.getTabListDataResponseStr();
            tabListResponseJson = new JSONObject(tabListResponse);
            dataArray = tabListResponseJson.getJSONArray("data");

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

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

//        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '" + subjectLine + "' AND date(date_created) >= "  + "'" + currentTimeStamp + "'"
//                + "order by id desc";

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        String sqlQuery = "select subject,attachment,sent_succesfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
                + "order by id desc";
        List<List<String>> queryResult = null;
        try {
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);

        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }finally {
//            postgreSQLJDBC.closeConnection();
        }
        return queryResult;

    }

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        try {
            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }
        return currentTimeStamp;
    }

    private int getActiveServiceLevelId(String flowToTest, String PCQ, String DCQ, String user, CustomAssert customAssert) {

        int serviceLevelId = -1;
        try {

            serviceLevelId = getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

            if (serviceLevelId != -1) {
                List<String> workFlowSteps;
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                if (!performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, user,false, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    return -1;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
        }
        return serviceLevelId;
    }

    private Boolean checkDCQPCQUDCOnCSL(int childServiceLevelId, String DCQExpected, String PCQExpected, String UDCExpected, CustomAssert customAssert) {

        Boolean validationStatus = true;
        Show show = new Show();

        try {
            show.hitShowVersion2(cslEntityTypeId, childServiceLevelId);
            String showResponse = show.getShowJsonStr();

            String DCQActual = ShowHelper.getValueOfField("performancedatacalculationquery", showResponse);
            String PCQActual = ShowHelper.getValueOfField("performancecomputationcalculationquery", showResponse);
            String UDCActual = ShowHelper.getValueOfField("udc", showResponse);

            if (!DCQActual.equalsIgnoreCase(DCQExpected)) {

                customAssert.assertTrue(false, "DCQ Expected and Actual are not equal");
                //customAssert.assertTrue(false, "DCQ Expected : " + DCQExpected + "DCQ Actual : " + DCQActual);
                validationStatus = false;
            }

            if (!PCQActual.equalsIgnoreCase(PCQExpected)) {

                customAssert.assertTrue(false, "PCQ Expected and Actual are not equal");
                //customAssert.assertTrue(false, "PCQ Expected : " + PCQExpected + "PCQ Actual : " + PCQActual);
                validationStatus = false;
            }

            if (!UDCActual.equalsIgnoreCase(UDCExpected)) {

                customAssert.assertTrue(false, "UDC Expected and Actual are not equal");
                //customAssert.assertTrue(false, "UDC Expected : " + UDCExpected + "UDC Actual : " + UDCActual);
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating DCQ and  PCQ on child SL " + childServiceLevelId);
            validationStatus = false;
        }

        return validationStatus;
    }

    private int getServiceLevelId(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

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



    private HashMap<String, String> renameFolderAndFiles(String filePath, HashMap<String, String> filePathFileNameMap,String newFileNameAfterRename, CustomAssert customAssert) {

        try {
            File file = new File(filePath);
            File[] listFiles = file.listFiles();
            String[] list = file.list();

            for (int i = 0; i < listFiles.length; i++) {

                //newFilePath = listFiles[i].toString();
                //File newFile = new File(newFilePath);
                if (listFiles[i].isDirectory()) {
                    filePathFileNameMap = renameFolderAndFiles(listFiles[i].toString(), filePathFileNameMap,newFileNameAfterRename, customAssert);
                } else if (listFiles[i].isFile()) {

                    for (int j = 0; j < list.length; j++) {

                        File beforeRename = listFiles[i];

                        String newFileNameAfterRename1 = newFileNameAfterRename.split(".xlsx")[0] + randomFileNumber + ".xlsx";

                        randomFileNumber = randomFileNumber + 1;

                        File newFileAfterRename1 = new File(listFiles[i].toString().replace(list[j], newFileNameAfterRename1));

                        beforeRename.renameTo(newFileAfterRename1);
                        filePathFileNameMap.put(filePath, newFileNameAfterRename1);
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Exception while renaming files");
            customAssert.assertTrue(false, "Exception while renaming files");
        }

        return filePathFileNameMap;
    }

    private Boolean validateRawDataTab(int cSLId, int rawDataLineItemSizeExcel, String excelFilePath, String excelFileName, String sheetName, CustomAssert customAssert) {

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

            HashMap<Integer, HashMap<String, String>> rawDataExcelRowValuesMap = createExcelRowValuesMap(excelFilePath, excelFileName, sheetName, customAssert);
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

                    columnValuesMapExcel = rawDataExcelRowValuesMap.get(i + 2);
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

    private HashMap<Integer, HashMap<String, String>> createRawDataLineItemRowValuesMap(int cSLId, int rawDataLineItemSizeExcel, CustomAssert customAssert) {

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

    private HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap(String excelFilePath, String excelFileName, String sheetName, CustomAssert customAssert) {

        HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap = new HashMap<>();
        HashMap<String, String> columnNameValueMap;

        try {
            int excelNumberOfRows = Integer.parseInt(XLSUtils.getNoOfRows(excelFilePath, excelFileName, sheetName).toString());
            List<String> excelColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, 1);
            List<String> excelDataRowData;
            for (int excelRowNum = 2; excelRowNum <= excelNumberOfRows; excelRowNum++) {

                excelDataRowData = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, excelRowNum);

                if (excelColumnNames.size() == excelDataRowData.size()) {
                    columnNameValueMap = new HashMap<>();
                    for (int columnCount = 0; columnCount < excelColumnNames.size(); columnCount++) {

                        columnNameValueMap.put(excelColumnNames.get(columnCount).toUpperCase(), excelDataRowData.get(columnCount));
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


    private String getStructuredPerformanceData(int cSLId) {

        logger.info("Getting Structured Performance data for CSL ID " + cSLId);
        int structuredPerformanceDataTabId = 207;

        TabListData tabListData = new TabListData();
        String payload = "{\"filterMap\":{\"entityTypeId\":" + cSLId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId, payload);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        return tabListResponse;

    }

    //    C10615 C10616 C10634 C10643
    private Boolean validateIgnoreFileScenario(int cSLId, CustomAssert customAssert) {

        logger.info("Validating Ignore Files Scenario");

        Boolean validationStatus = true;
        Long waitForIgnoreTimeOut = 120000L;
        try {
            String structurePerformanceData = getStructuredPerformanceData(cSLId);

            if (JSONUtility.validjson(structurePerformanceData)) {

                JSONObject structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                JSONArray dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);
                String columnName;
                String documentId = "";
                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("filename")) {
                        documentId = dataArray.getJSONObject(i).get("value").toString().split(":;")[1];
                        break;
                    }
                }

                SLDetails slDetails = new SLDetails();
                String payload = "{\"documentIds\":[" + documentId + "],\"childSlaId\":" + cSLId + "}";
                slDetails.hitIgnorePerformanceDataFiles(payload);

                //Checking Ignore in Progress Case
                structurePerformanceData = getStructuredPerformanceData(cSLId);
                structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);
                String statusValue = "";
                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("status")) {
                        statusValue = dataArray.getJSONObject(i).get("value").toString();
                        if (!(statusValue.equalsIgnoreCase("Ignore in Progress:;false") || statusValue.contains("Ignored"))) {
                            customAssert.assertTrue(false, "Status For File Ignore Scenario is not as Expected for CSL ID " + cSLId);
                            customAssert.assertTrue(false, "Expected : " + "Ignore in Progress:;false" + " Actual : " + statusValue);
                            validationStatus = false;
                        }
                        break;
                    }
                }

                Thread.sleep(300000);
                //Checking Ignored Case
                structurePerformanceData = getStructuredPerformanceData(cSLId);
                structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);

                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("status")) {
                        statusValue = dataArray.getJSONObject(i).get("value").toString();
                        if (!statusValue.equalsIgnoreCase("Ignored:;false")) {
                            customAssert.assertTrue(false, "Status For File Ignore Scenario is not as Expected for CSL ID " + cSLId);
                            customAssert.assertTrue(false, "Expected : " + "Ignored:;false" + " Actual : " + statusValue);
                            validationStatus = false;
                        }
                        break;
                    }
                }
                //Verifying Audit Log After Ignoring Files
//				if (!(verifyAuditLog(cslEntityTypeId, cSLId, "Performance Data Ignored", auditLogUser, customAssert))) {
//					customAssert.assertTrue(false, "Audit Log validated unsuccessfully after Ignore Files Functionality");
//				}

                //Verifying Raw Data Tab when all files are ignored
                //************************************************************************************************************************************************
                payload = "{\"offset\":0,\"size\":1,\"childSlaId\":" + cSLId + "}";
                slDetails.hitSLDetailsGlobalList(payload);
                String sLDetailsResponse = slDetails.getSLDetailsResponseStr();

                dataArray = new JSONObject(sLDetailsResponse).getJSONArray("data");
                if (dataArray.length() != 0) {
                    customAssert.assertTrue(false, "Raw Data Tab contains data when all files have been ignored for CSL ID " + cSLId);
                }
                //************************************************************************************************************************************************


            } else {
                customAssert.assertTrue(false, "Structured Performance Data Response is not a valid Json for CSL Id " + cSLId);
                validationStatus = false;
            }

        } catch (Exception e) {
            logger.error("Exception while validating Ignore Files Scenario");
            customAssert.assertTrue(false, "Exception while validating Ignore Files Scenario");
            validationStatus = false;
        }
        return validationStatus;

    }


    //C10772
    private Boolean validateDownloadedBulkTemplateRawDataWithUploadedSlTemplateRawData(String unzipFilePath, String bulkUploadRawDataZipDir,
                                                                                       String slTemplateFileName,
                                                                                       List<String> childServiceLevelIdsForBulkUpload,
                                                                                       List<String> childServiceLevelShortCodeIdsList,
                                                                                       List<String> childServiceLevelDescList,
                                                                                       CustomAssert customAssert) {

        Boolean validationStatus = true;

        String unzippedFilesLocation = unzipFilePath + "/" + bulkUploadRawDataZipDir;
        String expectedFolder;
        String expectedFileName;
        String folderToGoInside;
        int numberOfRowsInUnzippedExcel;

        try {
            int numberOfRowsInPerformanceDataFileUploadedOnSL = Integer.parseInt(XLSUtils.getNoOfRows(uploadFilePath, slTemplateFileName, "Format Sheet").toString());

            List<String> columnNameListTemplateExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(uploadFilePath, slTemplateFileName, "Format Sheet", 0, 2, numberOfRowsInPerformanceDataFileUploadedOnSL - 2);
            List<String> columnTypeListTemplateExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(uploadFilePath, slTemplateFileName, "Format Sheet", 1, 2, numberOfRowsInPerformanceDataFileUploadedOnSL - 2);
            List<String> rowHeadersForExcel;

            String insideFilesPrefix = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkuploadrawdatazipfilenameiinerfilesprefix");
            for (int i = 0; i < childServiceLevelIdsForBulkUpload.size(); i++) {

                expectedFolder = childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i);
                folderToGoInside = unzippedFilesLocation + "/" + expectedFolder;

                if(insideFilesPrefix == null) {
                    expectedFileName = childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i) + ".xlsx";
                }else {
                    expectedFileName = insideFilesPrefix + childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i) + ".xlsx";
                }
                if (FileUtils.fileExists(folderToGoInside, expectedFileName)) {

                    rowHeadersForExcel = XLSUtils.getExcelDataOfOneRow(folderToGoInside, expectedFileName, "Data Sheet", 1);

                    numberOfRowsInUnzippedExcel = Integer.parseInt(XLSUtils.getNoOfRows(folderToGoInside, expectedFileName, "Format Sheet").toString());

                    List<String> columnNameListUnZipExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(folderToGoInside, expectedFileName, "Format Sheet", 0, 1, numberOfRowsInUnzippedExcel - 1);
                    List<String> columnTypeListUnZipExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(folderToGoInside, expectedFileName, "Format Sheet", 1, 1, numberOfRowsInUnzippedExcel - 1);

                    if (columnNameListTemplateExcel.size() == columnNameListUnZipExcel.size()) {

                        for (int j = 0; j < columnNameListTemplateExcel.size(); j++) {
                            if (!columnNameListTemplateExcel.get(j).equalsIgnoreCase(columnNameListUnZipExcel.get(j))) {
                                customAssert.assertTrue(false, "Expected and Actual value for row number " + j + " is not equal for unzipped excel " + expectedFileName
                                        + " and template excel");
                                validationStatus = false;
                            }
                        }

                    } else {
                        customAssert.assertTrue(false, "Number of rows for Column Name in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
                        validationStatus = false;
                    }

                    if (columnTypeListTemplateExcel.size() == columnTypeListUnZipExcel.size()) {
                        for (int j = 0; j < columnTypeListTemplateExcel.size(); j++) {
                            if (!columnTypeListTemplateExcel.get(j).equalsIgnoreCase(columnTypeListUnZipExcel.get(j))) {
                                customAssert.assertTrue(false, "Expected and Actual type for row number " + j + " is not equal for unzipped excel " + expectedFileName
                                        + " and template excel");
                                validationStatus = false;
                            }
                        }
                    } else {
                        customAssert.assertTrue(false, "Number of rows for Column Type in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
                        validationStatus = false;
                    }

                    if (columnNameListTemplateExcel.size() == rowHeadersForExcel.size()) {

                        for (int j = 0; j < columnNameListTemplateExcel.size(); j++) {
                            if (!columnNameListTemplateExcel.get(j).equalsIgnoreCase(rowHeadersForExcel.get(j))) {
                                customAssert.assertTrue(false, "Expected and Actual value for row number " + j + " is not equal for unzipped excel " + expectedFileName
                                        + " and template excel");
                                validationStatus = false;
                            }
                        }

                    } else {
                        customAssert.assertTrue(false, "Number of columns for Header Name in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
                        validationStatus = false;
                    }

                } else {
                    customAssert.assertTrue(false, "File does not exists " + folderToGoInside + "/" + expectedFileName);
                    validationStatus = false;

                }

            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Downloaded BulkTemplate RawData With Uploaded SlTemplate RawData " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean validateAttachmentName(List<List<String>> recordFromSystemEmailTable, String expectedAttachment, CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {
            String actualAttachment = recordFromSystemEmailTable.get(0).get(1);

            if (expectedAttachment == null && actualAttachment == null) {
                return true;
            } else if (expectedAttachment == null && actualAttachment != null) {
                return false;
            }

            if (actualAttachment.equalsIgnoreCase(expectedAttachment)) {

                customAssert.assertTrue(false, "Attachment name validated unsuccessfully from system emails table");
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Attachment Name from system email table with the expected attachment name");
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean validateBodyOfEmail(List<List<String>> recordFromSystemEmailTable, List<String> expectedStringInBody, CustomAssert customAssert) {

        Boolean validationStatus = true;

        String actualBodyHtml;
        try {

            for (int i = 0; i < recordFromSystemEmailTable.size(); i++) {
                actualBodyHtml = recordFromSystemEmailTable.get(i).get(3);

                for (int j = 0; j < expectedStringInBody.size(); j++) {

                    if (!actualBodyHtml.contains(expectedStringInBody.get(j))) {
                        customAssert.assertTrue(false, "Body Html does not contain the expected String " + expectedStringInBody.get(j));
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

    private boolean updatePCQonMSL(int serviceLevelId,String PCQUpdated,CustomAssert customAssert){

        Boolean updationStatus = true;

        try{
            Edit edit = new Edit();
            String editResponse = edit.hitEdit("service levels",serviceLevelId);
            JSONObject editResponseJson = new JSONObject(editResponse);
            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");
            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",PCQUpdated);

            editResponse = edit.hitEdit("service levels",editResponseJson.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Error while updating PCQ on SL ID " + serviceLevelId);
                updationStatus = false;
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while updating PCQ on SL " + serviceLevelId);
            updationStatus = false;
        }
        return updationStatus;
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

    private Boolean performComputationCSL(int cSLId,CustomAssert customAssert) {

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        Boolean workFlowStatus = true;
        try {
            Thread.sleep(2000);
        }catch (Exception e){

        }
        for (String workFlowActionToPerform : workFlowActionToPerformCSLComputePerformance) {
            workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, cSLId, workFlowActionToPerform,customAssert);


            if (!workFlowStatus) {
                customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " on CSL Id " + cSLId);
                workFlowStatus = false;
            }
        }

        return workFlowStatus;
    }

}
