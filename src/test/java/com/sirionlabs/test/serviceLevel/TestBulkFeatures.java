package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestBulkFeatures {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkFeatures.class);

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
    private String completedBy;
    private String adminUser;
    private String rawDataFileValidMsg;
    private Integer slEntityTypeId;
    private Integer cslEntityTypeId;
    private Integer contractEntityTypeId;
    private Integer supplierEntityTypeId;

    private int serviceLevelId1;
    private int serviceLevelId12;

    private int cslBulkUpdateTemplateId;
    private int slMetaDataUploadTemplateId;
    private int uploadIdSL_PerformanceDataTab;
    private final int childServiceLevelTabId = 7;

    private ArrayList<String> childserviceLevelId1s = new ArrayList<>();
    private ArrayList<String> childserviceLevelId1s1 = new ArrayList<>();
    private ArrayList<String> childserviceLevelId1sForFlowDownOfValues = new ArrayList<>();

    private String slTemplateFileName;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

//	PostgreSQLJDBC postgreSQLJDBC;

    private String currentDateIn_MM_DD_YYYY_Format;
    private String previousDateIn_MM_DD_YYYY_Format;
    private String nextDateIn_MM_DD_YYYY_Format;

    private String currentDateIn_MMM_DD_YYYY_Format;
    private String previousDateIn_MMM_DD_YYYY_Format;
    private String nextDateIn_MMM_DD_YYYY_Format;

    private String[] workFlowActionToPerformCSLComputePerformance;
    private List<List<String>> currentTimeStampForSLTemplateUploadCSLCreation;

    static int randomFileNumber = 1;

    String dbHostName;
    String dbPortName;
    String dbName;
    String dbUserName;
    String dbPassowrd;


    @BeforeClass(groups = {"sanity","sprint"})
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
        auditLogUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "auditloguser");
        completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "completedby");

        adminUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminuser");
        rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilesuccessmsg");
        slTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

        currentDateIn_MM_DD_YYYY_Format = DateUtils.getCurrentDateInMM_DD_YYYY();
        previousDateIn_MM_DD_YYYY_Format = DateUtils.getPreviousDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);
//		nextDateIn_MM_DD_YYYY_Format = DateUtils.getNextDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);

        currentDateIn_MMM_DD_YYYY_Format = DateUtils.getCurrentDateInMMM_DD_YYYY();
        previousDateIn_MMM_DD_YYYY_Format = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);
//		nextDateIn_MMM_DD_YYYY_Format = DateUtils.getNextDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);

        workFlowActionToPerformCSLComputePerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslperformancecalulation").split("->");

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassowrd = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);

        try {
//			postgreSQLJDBC.deleteDBEntry("delete from system_emails");
        } catch (Exception e) {
            logger.error("Exception while deleting from system_emails table");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

    }

    //Always set parallel to false else test fails
    @DataProvider(name = "bulkCreateFlows", parallel = true)
    public Object[][] bulkCreateFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"bulk create", "flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(groups = {"sanity","PCQ DCQ UDC Update1","PCQ DCQ UDC Update2"},enabled = true)
    public void TestCSLCreation() {

        CustomAssert customAssert = new CustomAssert();

        try {

//			executeParallelServiceToGetActiveServiceLevelId();

            serviceLevelId1 = 20918;
            serviceLevelId12 = 20919;
            slToDelete.add(serviceLevelId1);
            slToDelete.add(serviceLevelId12);

            if (serviceLevelId1 != -1) {

                childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);
                childserviceLevelId1s1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId12, customAssert);
//
//                addCSLToDelete(childserviceLevelId1s);
//                addCSLToDelete(childserviceLevelId1s1);

//				int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));
//
//				if (!(childserviceLevelId1s.size() >= numberOfChildServiceLevel)) {
//
//					customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId1 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s.size());
//					customAssert.assertAll();
//				}
//
//				if (!(childserviceLevelId1s1.size() >= numberOfChildServiceLevel)) {
//
//					customAssert.assertTrue(false, "For Service Level Id 2 " + serviceLevelId12 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s1.size());
//					customAssert.assertAll();
//				}
//
//				//Validating there should not be any data on view template on structure performance data when no template is uploaded
//				if (!validateViewTemplate(Integer.parseInt(childserviceLevelId1s.get(0)), null, customAssert)) {
//					customAssert.assertTrue(false, "View Template validated unsuccessfully when no template uploaded on service level");
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId1, "", customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
//				}
//
//				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
//				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
////
//				currentTimeStampForSLTemplateUploadCSLCreation = getCurrentTimeStamp();
//
//				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//					customAssert.assertTrue(false, "Error while performance data file upload");
////					customAssert.assertAll();
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
//				}
//
//				if (!validateDownloadFileFromPerformanceTab(serviceLevelId1, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//					customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for SL ID " + serviceLevelId1);
//				}
//
//				if (!uploadPerformanceDataFormat(serviceLevelId12, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//					customAssert.assertTrue(false, "Error while performance data file upload for Service Level 2");
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId12, performanceDataFormatFileName, customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for Service Level 2");
//				}
//
//				if (!validateDownloadFileFromPerformanceTab(serviceLevelId12, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//					customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for service level  2" );
//				}



            }

        } catch (Exception e) {
            logger.error("Exception while creation of CSL");
            customAssert.assertTrue(false, "Exception while creation of CSL" + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C13773  April 2020 Validated
    @Test(groups = {"sanity","sprint"},dataProvider = "bulkCreateFlows",enabled = false)
    public void TestBulkCreateSL(String bulkCreateFlow) {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);
        String bulkCreateSLFile = null;

        try {
            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

            String expectedMaxExpected1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "expectedmax1");
            String expectedMaxExpected2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "expectedmax2");
            String maxExpected1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "max1");
            String maxExpected2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "max2");
            String sigMaxExpected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "sigmax");

            String expectedMinExpected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "expectedmin");
            String minExpected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "min");
            String sigMinExpected1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "sigmin1");
            String sigMinExpected2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create", "sigmin2");

            String currencyFieldValue = "-10";
            UploadBulkData uploadBulkData = new UploadBulkData();

            Map<String, String> payloadMap = new HashMap<>();
            int entityIdContract = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create","bulkcreatesloncontract"));
            payloadMap.put("parentEntityId", String.valueOf(entityIdContract));
            payloadMap.put("parentEntityTypeId", String.valueOf(contractEntityTypeId));

            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slbulkcreatetemplateid"));

            bulkCreateSLFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreateslfilename");

            int startRowNum = 6;
            int numberOfSlToCreate = 1;

            int bulkCreateExcelRowToUpdateTill = startRowNum + numberOfSlToCreate;

            String desc = "Automation Bulk Create SL";
            Boolean templateDownloadStatus = BulkTemplate.downloadBulkCreateTemplate(downloadFilePath, bulkCreateSLFile, 1015, contractEntityTypeId,entityIdContract);

            if(!templateDownloadStatus){
                customAssert.assertTrue(false,"SL Bulk Create Template download unsuccessful from contract id " + entityIdContract);
            }

            String sheetName = "Sla";

            List<String> rowsToUpdate = XLSUtils.getExcelDataOfOneRow(downloadFilePath,bulkCreateSLFile,sheetName,2);

            int supplierDBId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create","supplierdbid"));

            String supplierName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create","suppliername");
            String supplierId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create","supplierid");


			String contractName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create","contractname");
            String contract = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create","contractid");

            Map<String,String> excelToUpdate = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"bulk create");

            int j=0;
            int k=1;
            for (int i = startRowNum; i < bulkCreateExcelRowToUpdateTill; i++) {
                for(j=0;j<rowsToUpdate.size();j++){
                    if(excelToUpdate.containsKey(rowsToUpdate.get(j))){
                        XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile,sheetName,i,j,excelToUpdate.get(rowsToUpdate.get(j)));
                        if(excelToUpdate.containsKey(243) || excelToUpdate.containsKey(244)){
                            System.out.println(excelToUpdate.values());
                        }
                    }
                }

                XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 0, String.valueOf(k));
                k=k+1;


//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 4, desc);
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 5, desc);
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 6, bulkCreateFlow);
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 48, "80");
                XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 1, supplierName + " ( " + supplierId + " )");
                XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 2, contractName + " ( " + contract + " )");
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 12, "Asia/Kolkata (GMT +05:30)");
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 15, "United States Dollar (USD)");
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 94, "Yes");


//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 18, PCQ);
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 19, DCQ);
//				XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 23, Integer.parseInt(currencyFieldValue));

				if(bulkCreateFlow.equalsIgnoreCase("sl max level 1")){

					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Maximum - 1 level");


				}else if(bulkCreateFlow.equalsIgnoreCase("sl max level 2")){
					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Maximum - 2 level");
					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 49, expectedMinExpected);

				}
//				else if(bulkCreateFlow.equalsIgnoreCase("sl max level 3")){
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Maximum - 3 level");
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 49, expectedMinExpected);
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 50, sigMinExpected1);
//
//				}else if(bulkCreateFlow.equalsIgnoreCase("sl min level 1")){
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Minimum - 1 level");
//				}else if(bulkCreateFlow.equalsIgnoreCase("sl min level 2")){
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Minimum - 2 level");
//				}else if(bulkCreateFlow.equalsIgnoreCase("sl min level 3")){
//
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 47, "Minimum - 3 level");
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 48, expectedMaxExpected2);
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 49, maxExpected1);
//					XLSUtils.updateColumnValue(uploadFilePath, bulkCreateSLFile, "Sla", i, 50, sigMinExpected2);
//				}
            }
            String timeStamp = getCurrentTimeStamp().get(0).get(0);
            uploadBulkData.hitUploadBulkData(slEntityTypeId, templateId, uploadFilePath, bulkCreateSLFile, payloadMap);

            String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

            if (!fileUploadResponse.contains("200:;")) {

                customAssert.assertTrue(false, "Error while Bulk Create Service Level");

            } else {
                logger.info("Waiting for 15 seconds");
                Thread.sleep(15000);

                String sqlQuery = "select id,sla_id from sla where name ilike '%" + desc + "%'" + "AND date_created > '" + timeStamp + "'";

                List<List<String>> sqlQueryResults = postgreSQLJDBC.doSelect(sqlQuery);
                if (sqlQueryResults.size() != numberOfSlToCreate) {
                    customAssert.assertTrue(false, "Actual number of SL created not according to expected number of " + numberOfSlToCreate);
                }
                List<String> rowList;

                int slCreatedId = 0;

                String sla_id;
                String showResponse;
                String threshold;
                String expected;
                String minimum;
                String sigMinMax;

                if (sqlQueryResults.size() == 0) {
                    customAssert.assertTrue(false, "After Bulk Create entities created not found in the DB");
                }
                for (int i = 0; i < sqlQueryResults.size(); i++) {

                    rowList = sqlQueryResults.get(i);
                    slCreatedId = Integer.parseInt(rowList.get(0));

                    slToDelete.add(slCreatedId);

                    sla_id = rowList.get(1);

                    show.hitShowVersion2(slEntityTypeId, slCreatedId);
                    showResponse = show.getShowJsonStr();

                    threshold = ShowHelper.getValueOfField("threshold", showResponse);
                    expected = ShowHelper.getValueOfField("expected", showResponse);

                    if (sla_id.equalsIgnoreCase("sl max level 3")) {

                        minimum = ShowHelper.getValueOfField("minimum", showResponse);
                        sigMinMax = ShowHelper.getValueOfField("sigminmax", showResponse);

                        if (!threshold.equalsIgnoreCase("Maximum - 3 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Maximum - 3 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected1)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected1 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }

                        if (!minimum.equalsIgnoreCase(expectedMinExpected)) {
                            customAssert.assertTrue(false, "Expected Minimum : " + maxExpected1 + "Actual minimum : " + minimum + " for SL ID " + slCreatedId);
                        }

                        if (!sigMinMax.equalsIgnoreCase(sigMinExpected1)) {
                            customAssert.assertTrue(false, "Expected Sig Min/Max : " + sigMaxExpected + "Actual Sig Min/Max : " + sigMinMax + " for SL ID " + slCreatedId);
                        }

                        int dynamicFieldSlId = 102363;
                        String dynamicFieldSl = "dyn" + dynamicFieldSlId;

                        validateFieldsOnShowPage(slEntityTypeId,slCreatedId,dynamicFieldSl,currencyFieldValue,customAssert);

                    } else if (sla_id.equalsIgnoreCase("sl max level 2")) {

                        minimum = ShowHelper.getValueOfField("minimum", showResponse);

                        if (!threshold.equalsIgnoreCase("Maximum - 2 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Maximum - 2 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected1)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected1 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }

                        if (!minimum.equalsIgnoreCase(expectedMinExpected)) {
                            customAssert.assertTrue(false, "Expected Minimum : " + maxExpected1 + "Actual minimum : " + minimum + " for SL ID " + slCreatedId);
                        }

                    } else if (sla_id.equalsIgnoreCase("sl max level 1")) {

                        if (!threshold.equalsIgnoreCase("Maximum - 1 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Maximum - 1 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected1)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected1 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }

                    } else if (sla_id.equalsIgnoreCase("sl min level 3")) {

                        minimum = ShowHelper.getValueOfField("minimum", showResponse);
                        sigMinMax = ShowHelper.getValueOfField("sigminmax", showResponse);

                        if (!threshold.equalsIgnoreCase("Minimum - 3 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Minimum - 3 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected2)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected2 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }

                        if (!minimum.equalsIgnoreCase(maxExpected1)) {
                            customAssert.assertTrue(false, "Expected Minimum : " + maxExpected1 + "Actual minimum : " + minimum + " for SL ID " + slCreatedId);
                        }

                        if (!sigMinMax.equalsIgnoreCase(sigMinExpected2)) {
                            customAssert.assertTrue(false, "Expected Sig Min/Max : " + sigMinExpected1 + "Actual Sig Min/Max : " + sigMinMax + " for SL ID " + slCreatedId);
                        }
                    } else if (sla_id.equalsIgnoreCase("sl min level 2")) {

                        minimum = ShowHelper.getValueOfField("minimum", showResponse);

                        if (!threshold.equalsIgnoreCase("Minimum - 2 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Minimum - 3 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected2)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected2 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }

                        if (!minimum.equalsIgnoreCase(minExpected)) {
                            customAssert.assertTrue(false, "Expected Minimum : " + maxExpected2 + "Actual minimum : " + minimum + " for SL ID " + slCreatedId);
                        }

                    } else if (sla_id.equalsIgnoreCase("sl min level 1")) {
                        if (!threshold.equalsIgnoreCase("Minimum - 1 level")) {
                            customAssert.assertTrue(false, "Expected Threshold : Minimum - 3 level Actual Threshold : " + threshold + " for SL ID " + slCreatedId);
                        }

                        if (!expected.equalsIgnoreCase(expectedMaxExpected2)) {
                            customAssert.assertTrue(false, "Expected expected : " + expectedMaxExpected2 + " Actual expected : " + expected + " for SL ID " + slCreatedId);
                        }
                    }

                }

                slToDelete.add(slCreatedId);

//                C13772 Email Validation
                String subjectLine = "Bulk create request response";
                show.hitShowVersion2(contractEntityTypeId, entityIdContract);
                showResponse = show.getShowJsonStr();

                String contractTitle = ShowHelper.getValueOfField("title", showResponse);


                show.hitShowVersion2(supplierEntityTypeId, supplierDBId);

                String showResponseSupplier = show.getShowJsonStr();
                String shortCodeIdSupplier = ShowHelper.getValueOfField("short code id", showResponseSupplier);

                List<String> expectedSentencesInBody = new ArrayList<>();
				expectedSentencesInBody.add("Your Service Levels bulk create request has been completed.");
                expectedSentencesInBody.add("Supplier : " + supplierName + " (" + shortCodeIdSupplier + " )");

                expectedSentencesInBody.add("Entity type : Service Levels");
                expectedSentencesInBody.add("Entities requested to create: " + numberOfSlToCreate);
                expectedSentencesInBody.add("Entities failed to create : 0");

                List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

                if (recordFromSystemEmailTable.size() == 0) {
                    customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
                }

                if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
					customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for subjectLine " + subjectLine);
				}

                if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                    customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while bulk create SL " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();
    }

    //C13519 C46296 C44281 C13290 C13594
//	@Test(groups = {"sanity"},dataProvider = "slMetStatusValidationFlows", enabled = false)
    @Test(groups = {"sanity","PCQ DCQ UDC Update1" },dependsOnMethods = "TestCSLCreation", enabled = false)
    public void TestBulkUpdateCSL() {

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "sl max level 3";

        UploadBulkData uploadBulkData = new UploadBulkData();
        Show show = new Show();

        Map<String, String> payloadMap = new HashMap<>();
        String BulkUploadResponse;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        //String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        int serviceLevelId1SLMetStatus = serviceLevelId1;

        try {

            Thread.sleep(5000);

            if (!validate_Max_SigMax_AbsentSL(serviceLevelId1SLMetStatus, flowToTest, customAssert)) {
                customAssert.assertTrue(false, "Max Or SigMax Fields are not present or absent on the SL ID " + serviceLevelId1SLMetStatus
                        + " for the flow " + flowToTest);
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
            if (!uploadPerformanceDataFormat(serviceLevelId1SLMetStatus, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId1SLMetStatus, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }


            String[] slMetStatusToValidate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "slmetstatustovalidate").split(",");
            int totalStatusToValidate = slMetStatusToValidate.length;

            String[] finalNumValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "finalnumvalue").split(",");

            String[] finalDenValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "finaldenominavalue").split(",");

            String[] colorCodeValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "colorcode").split(",");

            if ((totalStatusToValidate != finalNumValues.length)
                    || (totalStatusToValidate != finalDenValues.length)
                    || (totalStatusToValidate != colorCodeValues.length)) {

                customAssert.assertTrue(false, "Error in configuration file " +
                        configFileName + " for the flow " + flowToTest);
//					customAssert.assertAll();
            }

            Float finalPerformance;

            int cSLIdForSlMetStatusValidation = Integer.parseInt(childserviceLevelId1s.get(10));
            String showResponse;

            if (!BulkTemplate.downloadBulkUpdateTemplate(downloadFilePath, bulkUpdateFilename, cslBulkUpdateTemplateId, cslEntityTypeId, childserviceLevelId1s.get(10))) {
                customAssert.assertTrue(false, "Unable to download bulk template for CSL Id " + cSLIdForSlMetStatusValidation);
//					customAssert.assertAll();
            }

            payloadMap.put("entityTypeId", Integer.toString(cslEntityTypeId));

            for (int i = 0; i < totalStatusToValidate; i++) {

                logger.info("Validating the SL MET status flow " + slMetStatusToValidate[i]);

                finalPerformance = (Float.parseFloat(finalNumValues[i]) / Float.parseFloat(finalDenValues[i])) * 100;
                XLSUtils.updateColumnValue(downloadFilePath, bulkUpdateFilename, "Child Sla", 6, 25, finalNumValues[i]);
                XLSUtils.updateColumnValue(downloadFilePath, bulkUpdateFilename, "Child Sla", 6, 26, finalDenValues[i]);
//					XLSUtils.updateColumnValue(downloadFilePath, bulkUpdateFilename, "Child Sla", 6, 36, finalDenValues[i]);
//					XLSUtils.updateColumnValue(downloadFilePath, bulkUpdateFilename, "Child Sla", 6, 38, slMetStatusToValidate[i]);

                uploadBulkData.hitUploadBulkData(cslEntityTypeId, cslBulkUpdateTemplateId, downloadFilePath, bulkUpdateFilename, payloadMap);
                BulkUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

                if (!BulkUploadResponse.contains("200")) {
                    customAssert.assertTrue(false, "BulkUpload done unsuccessfully for the flow " + slMetStatusToValidate[i]);
                }
                Thread.sleep(60000);
                show.hitShowVersion2(cslEntityTypeId, cSLIdForSlMetStatusValidation);
                showResponse = show.getShowJsonStr();

                if (!validateSLMETStatus(showResponse, finalPerformance, slMetStatusToValidate[i], colorCodeValues[i], customAssert)) {
                    customAssert.assertTrue(false, "SLMet Status validated unsuccessfully for the flow " + flowToTest + " and SLMET Status " + slMetStatusToValidate[i]);
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating SL  Met Status " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C13761 C13760 C13762
    @Test(groups = {"sanity","sprint","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false)
    public void TestBulkEditCSL() {

        CustomAssert customAssert = new CustomAssert();

        try {

            String entityIdToTest;

            int cslID1 = Integer.parseInt(childserviceLevelId1s.get(8));

            int cslID2 = Integer.parseInt(childserviceLevelId1s.get(9));

            entityIdToTest = cslID1 + "," + cslID2;
//			Thread.sleep(5000);
            String bulkEditFieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkedit", "fieldids");

            String expected = "90.0";
            String minMax = "100.0";
//			String minMax = "110.0";
            String sigMinMax = "110.0";
//			String sigMinMax = "120.0";
            String supplierNumerator = "100.0";
            String supplierDenominator = "100.0";
            String finalNumerator = "100.0";
            String finalDenominator = "100.0";
            String actualNumerator = "100.0";
            String actualDenominator = "100.0";
            String supplierCalculation = "100.0";
//			String actualPerformance = "100.0";
            String finalPerformance = "100.0";
            String discrepancy = "100.0";

//            String bulkEditPayload = "{\"body\": {\"data\": {" +
//                    "\"finalPerformance\": {\"name\": \"finalPerformance\",\"id\": 1103,\"values\": \"" + finalPerformance + "\"}," +
//                    "\"discrepancy\": {\"name\": \"discrepancy\",\"id\": 1104,\"values\": \"" + discrepancy + "\"}," +
////					"\"actualPerformance\": {\"name\": \"actualPerformance\",\"id\": 1102,\"values\": \"" + actualPerformance + "\"}," +
//                    "\"supplierCalculation\": {\"name\": \"supplierCalculation\",\"id\": 1101,\"values\": \"" + supplierCalculation + "\"}," +
//                    "\"expected\": {\"name\": \"expected\",\"id\": 1148,\"values\": \"" + expected + "\"}," +
//                    "\"minMax\": {\"name\": \"minMax\",\"id\": 1147,\"values\": \"" + minMax + "\"}," +
//                    "\"sigMinMax\": {\"name\": \"sigMinMax\",\"id\": 1149,\"values\": \"" + sigMinMax + "\"}," +
//                    "\"supplierNumerator\": {\"name\": \"supplierNumerator\",\"id\": 1106,\"values\": \"" + supplierNumerator + "\"}," +
//                    "\"supplierDenominator\": {\"name\": \"supplierDenominator\",\"id\": 1107,\"values\": \"" + supplierDenominator + "\"}," +
//                    "\"finalNumerator\": {\"name\": \"finalNumerator\",\"id\": 1110,\"values\": \"" + finalNumerator + "\"}," +
//                    "\"finalDenominator\": {\"name\": \"finalDenominator\",\"id\": 1111,\"values\": \"" + finalDenominator + "\"}," +
//                    "\"actualNumerator\": {\"name\": \"actualNumerator\",\"id\": 1108,\"values\": \"" + actualNumerator + "\"}," +
//                    "\"actualDenominator\": {\"name\": \"actualDenominator\",\"id\": 1109,\"values\": \"" + actualDenominator + "\"}}," +
//                    "\"globalData\": {\"entityIds\": [" + entityIdToTest + "],\"fieldIds\": [" + bulkEditFieldIds + "],\"isGlobalBulk\": true}}}";


            String bulkEditPayload = "{\"body\":{\"data\":{\"acceptanceDate\":{\"name\":\"acceptanceDate\",\"id\":1208,\"multiEntitySupport\":false},\"parentShortCodeId\":{\"name\":\"parentShortCodeId\",\"id\":1217,\"multiEntitySupport\":false},\"functions\":{\"name\":\"functions\",\"id\":1160,\"multiEntitySupport\":false},\"references\":{\"name\":\"references\",\"multiEntitySupport\":false},\"clauses\":{\"name\":\"clauses\",\"multiEntitySupport\":false},\"discrepancyCreditEarnback\":{\"name\":\"discrepancyCreditEarnback\",\"id\":1114,\"multiEntitySupport\":false},\"parentEntityId\":{\"name\":\"parentEntityId\",\"multiEntitySupport\":false},\"slKpi\":{\"name\":\"slKpi\",\"id\":11705,\"options\":null,\"multiEntitySupport\":false},\"supplierAccess\":{\"name\":\"supplierAccess\",\"id\":1136,\"multiEntitySupport\":false},\"performanceComputationCalculationQuery\":{\"name\":\"performanceComputationCalculationQuery\",\"id\":1221,\"multiEntitySupport\":false},\"initiatives\":{\"name\":\"initiatives\",\"id\":4916,\"multiEntitySupport\":false},\"state\":{\"name\":\"state\",\"multiEntitySupport\":false},\"supplierDenominator\":{\"name\":\"supplierDenominator\",\"id\":1107,\"multiEntitySupport\":false,\"values\":\"75\"},\"finalNumerator\":{\"name\":\"finalNumerator\",\"id\":1110,\"multiEntitySupport\":false,\"values\":\"75\"},\"contractingHubs\":{\"name\":\"contractingHubs\",\"id\":11253,\"multiEntitySupport\":false},\"cycleTime\":{\"name\":\"cycleTime\",\"multiEntitySupport\":false},\"clientCurrency\":{\"name\":\"clientCurrency\",\"multiEntitySupport\":false},\"expected\":{\"name\":\"expected\",\"id\":1148,\"multiEntitySupport\":false,\"values\":\"90\"},\"earnbacksTerm\":{\"name\":\"earnbacksTerm\",\"id\":1182,\"multiEntitySupport\":false},\"stagingPrimaryKey\":{\"name\":\"stagingPrimaryKey\",\"multiEntitySupport\":false},\"timeZone\":{\"name\":\"timeZone\",\"id\":1162,\"multiEntitySupport\":false},\"creditApplicable\":{\"name\":\"creditApplicable\",\"id\":12060,\"multiEntitySupport\":false},\"earnbackClauseName\":{\"name\":\"earnbackClauseName\",\"id\":12065,\"multiEntitySupport\":false},\"leadTimes\":{\"name\":\"leadTimes\",\"multiEntitySupport\":false},\"clientHoliday\":{\"name\":\"clientHoliday\",\"multiEntitySupport\":false},\"supplierCalculation\":{\"name\":\"supplierCalculation\",\"id\":1101,\"multiEntitySupport\":false,\"values\":\"75\"},\"discrepancyValue\":{\"name\":\"discrepancyValue\",\"id\":11136,\"displayValues\":\"\",\"multiEntitySupport\":false},\"creditApplicableDate\":{\"name\":\"creditApplicableDate\",\"id\":1174,\"multiEntitySupport\":false},\"uniqueDataCriteria\":{\"name\":\"uniqueDataCriteria\",\"id\":7152,\"multiEntitySupport\":false},\"startDate\":{\"name\":\"startDate\",\"id\":1156,\"multiEntitySupport\":false},\"contractingMarkets\":{\"name\":\"contractingMarkets\",\"id\":11254,\"multiEntitySupport\":false},\"discrepancyResolutionStatus\":{\"name\":\"discrepancyResolutionStatus\",\"id\":1105,\"multiEntitySupport\":false,\"values\":null},\"slMet\":{\"name\":\"slMet\",\"id\":1151,\"options\":null,\"multiEntitySupport\":false,\"values\":null},\"clientFinalEarnbackAmount\":{\"name\":\"clientFinalEarnbackAmount\",\"multiEntitySupport\":false},\"minMax\":{\"name\":\"minMax\",\"id\":1147,\"multiEntitySupport\":false,\"values\":\"100\"},\"responsibility\":{\"name\":\"responsibility\",\"id\":1168,\"multiEntitySupport\":false},\"vendorContractingParty\":{\"name\":\"vendorContractingParty\",\"id\":4747,\"multiEntitySupport\":false},\"currency\":{\"name\":\"currency\",\"id\":1135,\"multiEntitySupport\":false},\"clause\":{\"name\":\"clause\",\"id\":1140,\"multiEntitySupport\":false},\"earnbackModeAvailable\":{\"name\":\"earnbackModeAvailable\",\"values\":false,\"multiEntitySupport\":false},\"ragApplicable\":{\"name\":\"ragApplicable\",\"id\":11997,\"options\":null,\"multiEntitySupport\":false},\"newSigMinMax\":{\"name\":\"newSigMinMax\",\"id\":1191,\"multiEntitySupport\":false},\"allowComputationDuplicates\":{\"name\":\"allowComputationDuplicates\",\"id\":7153,\"values\":false,\"multiEntitySupport\":false},\"measurementWindow\":{\"name\":\"measurementWindow\",\"id\":1150,\"multiEntitySupport\":false},\"services\":{\"name\":\"services\",\"id\":1161,\"options\":null,\"multiEntitySupport\":false},\"creditFrequency\":{\"name\":\"creditFrequency\",\"id\":1180,\"multiEntitySupport\":false},\"earnbackFrequency\":{\"name\":\"earnbackFrequency\",\"id\":1186,\"multiEntitySupport\":false},\"oldSystemId\":{\"name\":\"oldSystemId\",\"multiEntitySupport\":false},\"lineItemEarnbackPercentage\":{\"name\":\"lineItemEarnbackPercentage\",\"id\":1185,\"multiEntitySupport\":false},\"creditImpactClause\":{\"name\":\"creditImpactClause\",\"id\":1205,\"multiEntitySupport\":false},\"performanceDataCalculationQuery\":{\"name\":\"performanceDataCalculationQuery\",\"id\":1222,\"multiEntitySupport\":false},\"ytdAverage\":{\"name\":\"ytdAverage\",\"id\":1146,\"multiEntitySupport\":false},\"creditEarnbackApplied\":{\"name\":\"creditEarnbackApplied\",\"id\":12068,\"multiEntitySupport\":false},\"clientCreditAmountPaid\":{\"name\":\"clientCreditAmountPaid\",\"multiEntitySupport\":false},\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":15,\"multiEntitySupport\":false},\"clientCreditImpactValue\":{\"name\":\"clientCreditImpactValue\",\"multiEntitySupport\":false},\"invoiceCreditPercentage\":{\"name\":\"invoiceCreditPercentage\",\"id\":1176,\"multiEntitySupport\":false},\"relations\":{\"name\":\"relations\",\"multiEntitySupport\":false},\"creditAmountBalance\":{\"name\":\"creditAmountBalance\",\"id\":12072,\"displayValues\":\"\",\"multiEntitySupport\":false},\"pageNumber\":{\"name\":\"pageNumber\",\"multiEntitySupport\":false},\"dueDate\":{\"name\":\"dueDate\",\"id\":1158,\"multiEntitySupport\":false},\"creditImpactApplicable\":{\"name\":\"creditImpactApplicable\",\"id\":1224,\"multiEntitySupport\":false},\"creditCalculationSubmitted\":{\"name\":\"creditCalculationSubmitted\",\"values\":false,\"multiEntitySupport\":false},\"clientFinalCreditAmount\":{\"name\":\"clientFinalCreditAmount\",\"multiEntitySupport\":false},\"scopeOfServiceTwo\":{\"name\":\"scopeOfServiceTwo\",\"id\":11715,\"multiEntitySupport\":false},\"serviceCategory\":{\"name\":\"serviceCategory\",\"id\":11470,\"multiEntitySupport\":false},\"expDate\":{\"name\":\"expDate\",\"id\":1157,\"multiEntitySupport\":false},\"frequency\":{\"name\":\"frequency\",\"id\":1153,\"multiEntitySupport\":false},\"actualPerformance\":{\"name\":\"actualPerformance\",\"id\":1102,\"multiEntitySupport\":false,\"values\":\"75\"},\"canSupplierBeParent\":true,\"scopeOfServiceOne\":{\"name\":\"scopeOfServiceOne\",\"id\":11714,\"multiEntitySupport\":false},\"clientEarnbackAmountPaid\":{\"name\":\"clientEarnbackAmountPaid\",\"multiEntitySupport\":false},\"actualParentEntityTypeId\":{\"name\":\"actualParentEntityTypeId\",\"multiEntitySupport\":false},\"creditValue\":{\"name\":\"creditValue\",\"id\":12364,\"multiEntitySupport\":false},\"actualDenominator\":{\"name\":\"actualDenominator\",\"id\":1109,\"multiEntitySupport\":false,\"values\":\"75\"},\"earnbackApplicableDate\":{\"name\":\"earnbackApplicableDate\",\"id\":1181,\"multiEntitySupport\":false},\"useStartDateParams\":{\"name\":\"useStartDateParams\",\"multiEntitySupport\":false},\"deliveryCountries\":{\"name\":\"deliveryCountries\",\"id\":1163,\"multiEntitySupport\":false},\"finalSigMinMax\":{\"name\":\"finalSigMinMax\",\"id\":1194,\"multiEntitySupport\":false},\"supplierNumerator\":{\"name\":\"supplierNumerator\",\"id\":1106,\"multiEntitySupport\":false,\"values\":\"75\"},\"name\":{\"name\":\"name\",\"id\":1127,\"multiEntitySupport\":false},\"rootInfo\":{\"name\":\"rootInfo\",\"multiEntitySupport\":false},\"projectId\":{\"name\":\"projectId\",\"id\":8157,\"multiEntitySupport\":false},\"calculatedEarnbackAmount\":{\"name\":\"calculatedEarnbackAmount\",\"id\":12063,\"displayValues\":\"\",\"multiEntitySupport\":false},\"excludeWeekends\":{\"name\":\"excludeWeekends\",\"values\":false,\"multiEntitySupport\":false},\"creditClauseApplied\":{\"name\":\"creditClauseApplied\",\"values\":false,\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":0,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}},\"slaCategory\":{\"name\":\"slaCategory\",\"id\":1131,\"multiEntitySupport\":false},\"lineItemsForEarnbackCalculation\":{\"name\":\"lineItemsForEarnbackCalculation\",\"id\":12142,\"multiEntitySupport\":false},\"impactDays\":{\"name\":\"impactDays\",\"id\":1117,\"multiEntitySupport\":false},\"lineItemsForCreditCalculation\":{\"name\":\"lineItemsForCreditCalculation\",\"id\":12141,\"multiEntitySupport\":false},\"description\":{\"name\":\"description\",\"id\":12083,\"multiEntitySupport\":false},\"parentEntityType\":{\"name\":\"parentEntityType\",\"multiEntitySupport\":false},\"newMinMax\":{\"name\":\"newMinMax\",\"id\":1189,\"multiEntitySupport\":false},\"adhocUser\":{\"firstName\":{\"name\":\"firstName\",\"id\":78,\"multiEntitySupport\":false},\"lastName\":{\"name\":\"lastName\",\"id\":79,\"multiEntitySupport\":false},\"loginId\":{\"name\":\"loginId\",\"id\":80,\"multiEntitySupport\":false},\"userType\":{\"name\":\"userType\",\"id\":83,\"options\":null,\"multiEntitySupport\":false},\"uniqueLoginId\":{\"name\":\"uniqueLoginId\",\"id\":82,\"multiEntitySupport\":false},\"email\":{\"name\":\"email\",\"id\":81,\"multiEntitySupport\":false}},\"performanceStatus\":{\"name\":\"performanceStatus\",\"id\":1134,\"multiEntitySupport\":false},\"clientCreditAmount\":{\"name\":\"clientCreditAmount\",\"multiEntitySupport\":false},\"draftEntity\":{\"name\":\"draftEntity\",\"values\":false,\"multiEntitySupport\":false},\"slaId\":{\"name\":\"slaId\",\"id\":1126,\"multiEntitySupport\":false},\"dataFrequency\":{\"name\":\"dataFrequency\",\"id\":1154,\"multiEntitySupport\":false},\"contractCountries\":{\"name\":\"contractCountries\",\"id\":1167,\"options\":null,\"multiEntitySupport\":false},\"measurementUnit\":{\"name\":\"measurementUnit\",\"id\":1144,\"multiEntitySupport\":false},\"recipientCompanyCodes\":{\"name\":\"recipientCompanyCodes\",\"id\":4895,\"options\":null,\"multiEntitySupport\":false},\"shortCodeId\":{\"name\":\"shortCodeId\",\"id\":1172,\"multiEntitySupport\":false},\"subCategoryEarnbackPercentage\":{\"name\":\"subCategoryEarnbackPercentage\",\"id\":1184,\"multiEntitySupport\":false},\"excludeFromHoliday\":{\"name\":\"excludeFromHoliday\",\"values\":false,\"multiEntitySupport\":false},\"sourceEntityId\":{\"name\":\"sourceEntityId\",\"multiEntitySupport\":false},\"rejectionDate\":{\"name\":\"rejectionDate\",\"id\":1209,\"multiEntitySupport\":false},\"finalPerformance\":{\"name\":\"finalPerformance\",\"id\":1103,\"multiEntitySupport\":false,\"values\":75},\"comment\":{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null,\"multiEntitySupport\":false},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false,\"values\":\"\"},\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false},\"invoiceCopy\":{\"name\":\"invoiceCopy\",\"values\":false,\"multiEntitySupport\":false},\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null,\"multiEntitySupport\":false},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}},\"applicableCredit\":{\"name\":\"applicableCredit\",\"id\":1112,\"multiEntitySupport\":false},\"contractingCompanyCodes\":{\"name\":\"contractingCompanyCodes\",\"id\":11256,\"options\":null,\"multiEntitySupport\":false},\"applications\":{\"name\":\"applications\",\"id\":1142,\"multiEntitySupport\":false},\"integrationSystem\":{\"name\":\"integrationSystem\",\"multiEntitySupport\":false},\"financialImpactClause\":{\"name\":\"financialImpactClause\",\"id\":1202,\"multiEntitySupport\":false},\"globalRegions\":{\"name\":\"globalRegions\",\"id\":1164,\"multiEntitySupport\":false},\"globalCountries\":{\"name\":\"globalCountries\",\"id\":1165,\"options\":null,\"multiEntitySupport\":false},\"financialImpactCurrencyValue\":{\"name\":\"financialImpactCurrencyValue\",\"id\":1201,\"displayValues\":\"\",\"multiEntitySupport\":false},\"computationStatus\":{\"name\":\"computationStatus\",\"id\":7158,\"multiEntitySupport\":false},\"clientEarnbackAmount\":{\"name\":\"clientEarnbackAmount\",\"multiEntitySupport\":false},\"categoryCreditPercentage\":{\"name\":\"categoryCreditPercentage\",\"id\":1177,\"multiEntitySupport\":false},\"regionType\":{\"name\":\"regionType\",\"id\":11696,\"options\":null,\"multiEntitySupport\":false},\"signatureAllowed\":{\"name\":\"signatureAllowed\",\"values\":false,\"multiEntitySupport\":false},\"supplier\":{\"name\":\"supplier\",\"id\":1129,\"values\":{\"url\":\"/tblrelations/show/null\",\"entityTypeId\":1,\"active\":false,\"blocked\":false,\"createdFromListPage\":false,\"summaryGroupData\":false,\"bulkOperation\":false,\"blockedForBulk\":false,\"autoExtracted\":false,\"dynamicFieldsEncrypted\":false,\"systemAdmin\":false,\"canOverdue\":false,\"autoCreate\":false,\"draftEntity\":false,\"validationError\":false,\"isReject\":false,\"parentHalting\":false,\"autoTaskFailed\":false,\"compareHistory\":false,\"flagForClone\":false,\"createStakeHolder\":false,\"escapeValueUpdateTask\":false,\"excludeFromHoliday\":false,\"excludeWeekends\":false,\"datetimeEnabled\":false,\"uploadAllowed\":false,\"downloadAllowed\":false,\"signatureAllowed\":false,\"saveCommentDocOnValueUpdate\":false,\"forAuditLog\":false,\"sourceOfAction\":0,\"savedAsDraft\":false,\"performedInMonth\":false,\"multiplier\":1,\"entityFinancialData\":{\"id\":null,\"entityId\":null,\"entityTypeId\":null,\"totalDirectAcv\":0,\"totalDirectTcv\":0,\"totalDirectFycv\":0,\"totalIndirectAcv\":0,\"totalIndirectTcv\":0,\"totalIndirectFycv\":0,\"dateCreated\":null,\"dateModified\":null,\"deleted\":false},\"deleteProcessed\":false,\"financialParamsUpdated\":false,\"globalActive\":false,\"dynamicMap\":{},\"overdue\":false,\"autoTask\":false},\"multiEntitySupport\":false},\"id\":{\"name\":\"id\",\"multiEntitySupport\":false},\"parentEntityIds\":{\"name\":\"parentEntityIds\",\"multiEntitySupport\":false},\"financialImpactApplicable\":{\"name\":\"financialImpactApplicable\",\"id\":1223,\"multiEntitySupport\":false},\"finalDenominator\":{\"name\":\"finalDenominator\",\"id\":1111,\"multiEntitySupport\":false,\"values\":\"75\"},\"applicationGroups\":{\"name\":\"applicationGroups\",\"id\":1141,\"multiEntitySupport\":false},\"contract\":{\"name\":\"contract\",\"id\":1130,\"values\":{\"url\":\"/tblcontracts/show/null\",\"entityTypeId\":61,\"active\":false,\"blocked\":false,\"createdFromListPage\":false,\"summaryGroupData\":false,\"bulkOperation\":false,\"blockedForBulk\":false,\"autoExtracted\":false,\"dynamicFieldsEncrypted\":false,\"systemAdmin\":false,\"canOverdue\":false,\"autoCreate\":false,\"draftEntity\":false,\"validationError\":false,\"isReject\":false,\"parentHalting\":false,\"autoTaskFailed\":false,\"compareHistory\":false,\"flagForClone\":false,\"createStakeHolder\":false,\"escapeValueUpdateTask\":false,\"excludeFromHoliday\":false,\"excludeWeekends\":false,\"datetimeEnabled\":false,\"uploadAllowed\":false,\"downloadAllowed\":false,\"signatureAllowed\":false,\"saveCommentDocOnValueUpdate\":false,\"forAuditLog\":false,\"sourceOfAction\":0,\"savedAsDraft\":false,\"performedInMonth\":false,\"multiplier\":1,\"entityFinancialData\":{\"id\":null,\"entityId\":null,\"entityTypeId\":null,\"totalDirectAcv\":0,\"totalDirectTcv\":0,\"totalDirectFycv\":0,\"totalIndirectAcv\":0,\"totalIndirectTcv\":0,\"totalIndirectFycv\":0,\"dateCreated\":null,\"dateModified\":null,\"deleted\":false},\"deleteProcessed\":false,\"financialParamsUpdated\":false,\"dynamicMap\":{},\"globalActive\":false,\"accessible\":false,\"sendForBulk\":false,\"globalContract\":false,\"leafContract\":false,\"dnoSlaBlockedForContract\":false,\"calendarType\":{\"id\":1001},\"autoextractedDocuments\":false,\"overdue\":false,\"autoTask\":false},\"multiEntitySupport\":false},\"creditModeAvailable\":{\"name\":\"creditModeAvailable\",\"values\":false,\"multiEntitySupport\":false},\"actualNumerator\":{\"name\":\"actualNumerator\",\"id\":1108,\"multiEntitySupport\":false,\"values\":\"75\"},\"priority\":{\"name\":\"priority\",\"id\":1137,\"multiEntitySupport\":false},\"creditAmountPaid\":{\"name\":\"creditAmountPaid\",\"id\":12070,\"displayValues\":\"\",\"multiEntitySupport\":false},\"discrepancy\":{\"name\":\"discrepancy\",\"id\":1104,\"multiEntitySupport\":false,\"values\":\"75\"},\"projectLevels\":{\"name\":\"projectLevels\",\"id\":4936,\"multiEntitySupport\":false},\"reportingDate\":{\"name\":\"reportingDate\",\"id\":11701,\"multiEntitySupport\":false},\"status\":{\"name\":\"status\",\"id\":1125,\"multiEntitySupport\":false},\"lineItemCreditPercentage\":{\"name\":\"lineItemCreditPercentage\",\"id\":1179,\"multiEntitySupport\":false},\"discrepancyStatus\":{\"name\":\"discrepancyStatus\",\"id\":1115,\"multiEntitySupport\":false},\"recipientClientEntities\":{\"name\":\"recipientClientEntities\",\"id\":4894,\"multiEntitySupport\":false},\"creditClauses\":{\"name\":\"creditClauses\",\"options\":null,\"multiEntitySupport\":false},\"ytdStartDate\":{\"name\":\"ytdStartDate\",\"id\":1145,\"multiEntitySupport\":false},\"applicableEarnback\":{\"name\":\"applicableEarnback\",\"id\":1113,\"multiEntitySupport\":false},\"creditTerm\":{\"name\":\"creditTerm\",\"id\":1175,\"multiEntitySupport\":false},\"referenceNo\":{\"name\":\"referenceNo\",\"multiEntitySupport\":false},\"actualParentEntityId\":{\"name\":\"actualParentEntityId\",\"multiEntitySupport\":false},\"recipientMarkets\":{\"name\":\"recipientMarkets\",\"id\":4893,\"multiEntitySupport\":false},\"systemCalculatedSlMet\":{\"name\":\"systemCalculatedSlMet\",\"multiEntitySupport\":false,\"values\":null,\"options\":null},\"slaItem\":{\"name\":\"slaItem\",\"id\":1133,\"options\":null,\"multiEntitySupport\":false},\"annualAverage\":{\"name\":\"annualAverage\",\"id\":1188,\"multiEntitySupport\":false},\"parentEntityTypeId\":{\"name\":\"parentEntityTypeId\",\"multiEntitySupport\":false},\"earnbackClauses\":{\"name\":\"earnbackClauses\",\"options\":null,\"multiEntitySupport\":false},\"subCategoryCreditPercentage\":{\"name\":\"subCategoryCreditPercentage\",\"id\":1178,\"multiEntitySupport\":false},\"contractRegions\":{\"name\":\"contractRegions\",\"id\":1166,\"multiEntitySupport\":false},\"earnbackClauseApplied\":{\"name\":\"earnbackClauseApplied\",\"values\":false,\"multiEntitySupport\":false},\"earnbackAmountPaid\":{\"name\":\"earnbackAmountPaid\",\"id\":12071,\"displayValues\":\"\",\"multiEntitySupport\":false},\"contractingClientEntities\":{\"name\":\"contractingClientEntities\",\"id\":11255,\"multiEntitySupport\":false},\"earnbackValue\":{\"name\":\"earnbackValue\",\"id\":12365,\"multiEntitySupport\":false},\"threshold\":{\"name\":\"threshold\",\"id\":1143,\"multiEntitySupport\":false},\"recipientHubs\":{\"name\":\"recipientHubs\",\"id\":4892,\"multiEntitySupport\":false},\"pageReference\":{\"name\":\"pageReference\",\"id\":1138,\"multiEntitySupport\":false},\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"rg_2054\":{\"values\":[],\"name\":\"rg_2054\",\"label\":\"Child Service Levels Manager\",\"userType\":[2,1,3,4],\"options\":null}},\"options\":null,\"multiEntitySupport\":false},\"creditImpactCurrencyValue\":{\"name\":\"creditImpactCurrencyValue\",\"id\":1204,\"displayValues\":\"\",\"multiEntitySupport\":false},\"invoiceNo\":{\"name\":\"invoiceNo\",\"id\":1116,\"multiEntitySupport\":false},\"clientFinancialImpactValue\":{\"name\":\"clientFinancialImpactValue\",\"multiEntitySupport\":false},\"parentHalting\":{\"name\":\"parentHalting\",\"values\":false,\"multiEntitySupport\":false},\"weekType\":{\"name\":\"weekType\",\"multiEntitySupport\":false},\"dynamicMetadata\":{\"dyn102097\":{\"name\":\"dyn102097\",\"id\":102097,\"multiEntitySupport\":false},\"dyn102592\":{\"name\":\"dyn102592\",\"id\":102592,\"options\":null,\"multiEntitySupport\":false},\"dyn102552\":{\"name\":\"dyn102552\",\"id\":102552,\"multiEntitySupport\":false},\"dyn102178\":{\"name\":\"dyn102178\",\"id\":102178,\"options\":null,\"multiEntitySupport\":false},\"dyn102533\":{\"name\":\"dyn102533\",\"id\":102533,\"multiEntitySupport\":false,\"values\":\"\"},\"dyn102599\":{\"name\":\"dyn102599\",\"id\":102599,\"multiEntitySupport\":false},\"dyn102535\":{\"name\":\"dyn102535\",\"id\":102535,\"options\":null,\"multiEntitySupport\":false},\"dyn102536\":{\"name\":\"dyn102536\",\"id\":102536,\"options\":null,\"multiEntitySupport\":false},\"dyn102635\":{\"name\":\"dyn102635\",\"id\":102635,\"options\":null,\"multiEntitySupport\":false},\"dyn102537\":{\"name\":\"dyn102537\",\"id\":102537,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102636\":{\"name\":\"dyn102636\",\"id\":102636,\"options\":null,\"multiEntitySupport\":false},\"dyn101999\":{\"name\":\"dyn101999\",\"id\":101999,\"multiEntitySupport\":false},\"dyn102381\":{\"name\":\"dyn102381\",\"id\":102381,\"multiEntitySupport\":false},\"dyn102382\":{\"name\":\"dyn102382\",\"id\":102382,\"multiEntitySupport\":false},\"dyn102540\":{\"name\":\"dyn102540\",\"id\":102540,\"multiEntitySupport\":false},\"dyn102221\":{\"name\":\"dyn102221\",\"id\":102221,\"options\":null,\"multiEntitySupport\":false},\"dyn102122\":{\"name\":\"dyn102122\",\"id\":102122,\"multiEntitySupport\":false},\"dyn102541\":{\"name\":\"dyn102541\",\"id\":102541,\"multiEntitySupport\":false},\"dyn102189\":{\"name\":\"dyn102189\",\"id\":102189,\"options\":null,\"multiEntitySupport\":false},\"dyn102222\":{\"name\":\"dyn102222\",\"id\":102222,\"options\":null,\"multiEntitySupport\":false},\"dyn102542\":{\"name\":\"dyn102542\",\"id\":102542,\"multiEntitySupport\":false,\"values\":false},\"dyn102543\":{\"name\":\"dyn102543\",\"id\":102543,\"multiEntitySupport\":false},\"dyn102642\":{\"name\":\"dyn102642\",\"id\":102642,\"options\":null,\"multiEntitySupport\":false},\"dyn102544\":{\"name\":\"dyn102544\",\"id\":102544,\"multiEntitySupport\":false,\"values\":\"\"},\"dyn102621\":{\"name\":\"dyn102621\",\"id\":102621,\"multiEntitySupport\":false},\"dyn102545\":{\"name\":\"dyn102545\",\"id\":102545,\"multiEntitySupport\":false},\"dyn102589\":{\"name\":\"dyn102589\",\"id\":102589,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102622\":{\"name\":\"dyn102622\",\"id\":102622,\"multiEntitySupport\":false},\"dyn102546\":{\"name\":\"dyn102546\",\"id\":102546,\"options\":null,\"multiEntitySupport\":false},\"dyn102623\":{\"name\":\"dyn102623\",\"id\":102623,\"multiEntitySupport\":false},\"dyn102547\":{\"name\":\"dyn102547\",\"id\":102547,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102624\":{\"name\":\"dyn102624\",\"id\":102624,\"multiEntitySupport\":false,\"values\":false},\"dyn101998\":{\"name\":\"dyn101998\",\"id\":101998,\"multiEntitySupport\":false},\"dyn102516\":{\"name\":\"dyn102516\",\"id\":102516,\"multiEntitySupport\":false},\"dyn102538\":{\"name\":\"dyn102538\",\"id\":102538,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102539\":{\"name\":\"dyn102539\",\"id\":102539,\"multiEntitySupport\":false}},\"creditClauseName\":{\"name\":\"creditClauseName\",\"id\":12064,\"multiEntitySupport\":false},\"categoryEarnbackPercentage\":{\"name\":\"categoryEarnbackPercentage\",\"id\":1183,\"multiEntitySupport\":false},\"finalMinMax\":{\"name\":\"finalMinMax\",\"id\":1192,\"multiEntitySupport\":false},\"computeSLMet\":{\"name\":\"computeSLMet\",\"values\":false,\"multiEntitySupport\":false},\"finalCreditAmount\":{\"name\":\"finalCreditAmount\",\"id\":12066,\"displayValues\":\"\",\"multiEntitySupport\":false},\"computationDoneWithDuplicates\":{\"name\":\"computationDoneWithDuplicates\",\"id\":7154,\"values\":false,\"multiEntitySupport\":false},\"earnbackApplicable\":{\"name\":\"earnbackApplicable\",\"id\":12073,\"multiEntitySupport\":false},\"clientCreditAmountBalance\":{\"name\":\"clientCreditAmountBalance\",\"multiEntitySupport\":false},\"clientEarnbackAmountBalance\":{\"name\":\"clientEarnbackAmountBalance\",\"multiEntitySupport\":false},\"earnbackAmountBalance\":{\"name\":\"earnbackAmountBalance\",\"id\":12078,\"displayValues\":\"\",\"multiEntitySupport\":false},\"reportingFrequency\":{\"name\":\"reportingFrequency\",\"id\":11706,\"multiEntitySupport\":false},\"earnbackCalculationSubmitted\":{\"name\":\"earnbackCalculationSubmitted\",\"values\":false,\"multiEntitySupport\":false},\"contracts\":{\"name\":\"contracts\",\"multiEntitySupport\":false},\"finalExpected\":{\"name\":\"finalExpected\",\"id\":1193,\"multiEntitySupport\":false},\"tier\":{\"name\":\"tier\",\"id\":1206,\"multiEntitySupport\":false},\"reviewDate\":{\"name\":\"reviewDate\",\"id\":1225,\"multiEntitySupport\":false},\"finalEarnbackAmount\":{\"name\":\"finalEarnbackAmount\",\"id\":12067,\"displayValues\":\"\",\"multiEntitySupport\":false},\"ageing\":{\"name\":\"ageing\",\"multiEntitySupport\":false},\"sourceEntityTypeId\":{\"name\":\"sourceEntityTypeId\",\"multiEntitySupport\":false},\"slaSubCategory\":{\"name\":\"slaSubCategory\",\"id\":1132,\"options\":null,\"multiEntitySupport\":false},\"submissionDate\":{\"name\":\"submissionDate\",\"id\":1207,\"multiEntitySupport\":false},\"calculatedCreditAmount\":{\"name\":\"calculatedCreditAmount\",\"id\":12062,\"displayValues\":\"\",\"multiEntitySupport\":false},\"sigMinMax\":{\"name\":\"sigMinMax\",\"id\":1149,\"multiEntitySupport\":false,\"values\":\"130\"},\"impactTypes\":{\"name\":\"impactTypes\",\"id\":1118,\"multiEntitySupport\":false},\"continuousImprovementComment\":{\"name\":\"continuousImprovementComment\",\"id\":1187,\"multiEntitySupport\":false},\"newExpected\":{\"name\":\"newExpected\",\"id\":1190,\"multiEntitySupport\":false},\"dyn102097\":{\"name\":\"dyn102097\",\"id\":102097,\"multiEntitySupport\":false},\"dyn102592\":{\"name\":\"dyn102592\",\"id\":102592,\"options\":null,\"multiEntitySupport\":false},\"dyn102552\":{\"name\":\"dyn102552\",\"id\":102552,\"multiEntitySupport\":false},\"dyn102178\":{\"name\":\"dyn102178\",\"id\":102178,\"options\":null,\"multiEntitySupport\":false},\"dyn102533\":{\"name\":\"dyn102533\",\"id\":102533,\"multiEntitySupport\":false,\"values\":\"\"},\"dyn102599\":{\"name\":\"dyn102599\",\"id\":102599,\"multiEntitySupport\":false},\"dyn102535\":{\"name\":\"dyn102535\",\"id\":102535,\"options\":null,\"multiEntitySupport\":false},\"dyn102536\":{\"name\":\"dyn102536\",\"id\":102536,\"options\":null,\"multiEntitySupport\":false},\"dyn102635\":{\"name\":\"dyn102635\",\"id\":102635,\"options\":null,\"multiEntitySupport\":false},\"dyn102537\":{\"name\":\"dyn102537\",\"id\":102537,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102636\":{\"name\":\"dyn102636\",\"id\":102636,\"options\":null,\"multiEntitySupport\":false},\"dyn101999\":{\"name\":\"dyn101999\",\"id\":101999,\"multiEntitySupport\":false},\"dyn102381\":{\"name\":\"dyn102381\",\"id\":102381,\"multiEntitySupport\":false},\"dyn102382\":{\"name\":\"dyn102382\",\"id\":102382,\"multiEntitySupport\":false},\"dyn102540\":{\"name\":\"dyn102540\",\"id\":102540,\"multiEntitySupport\":false},\"dyn102221\":{\"name\":\"dyn102221\",\"id\":102221,\"options\":null,\"multiEntitySupport\":false},\"dyn102122\":{\"name\":\"dyn102122\",\"id\":102122,\"multiEntitySupport\":false},\"dyn102541\":{\"name\":\"dyn102541\",\"id\":102541,\"multiEntitySupport\":false},\"dyn102189\":{\"name\":\"dyn102189\",\"id\":102189,\"options\":null,\"multiEntitySupport\":false},\"dyn102222\":{\"name\":\"dyn102222\",\"id\":102222,\"options\":null,\"multiEntitySupport\":false},\"dyn102542\":{\"name\":\"dyn102542\",\"id\":102542,\"multiEntitySupport\":false,\"values\":false},\"dyn102543\":{\"name\":\"dyn102543\",\"id\":102543,\"multiEntitySupport\":false},\"dyn102642\":{\"name\":\"dyn102642\",\"id\":102642,\"options\":null,\"multiEntitySupport\":false},\"dyn102544\":{\"name\":\"dyn102544\",\"id\":102544,\"multiEntitySupport\":false,\"values\":\"\"},\"dyn102621\":{\"name\":\"dyn102621\",\"id\":102621,\"multiEntitySupport\":false},\"dyn102545\":{\"name\":\"dyn102545\",\"id\":102545,\"multiEntitySupport\":false},\"dyn102589\":{\"name\":\"dyn102589\",\"id\":102589,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102622\":{\"name\":\"dyn102622\",\"id\":102622,\"multiEntitySupport\":false},\"dyn102546\":{\"name\":\"dyn102546\",\"id\":102546,\"options\":null,\"multiEntitySupport\":false},\"dyn102623\":{\"name\":\"dyn102623\",\"id\":102623,\"multiEntitySupport\":false},\"dyn102547\":{\"name\":\"dyn102547\",\"id\":102547,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102624\":{\"name\":\"dyn102624\",\"id\":102624,\"multiEntitySupport\":false,\"values\":false},\"dyn101998\":{\"name\":\"dyn101998\",\"id\":101998,\"multiEntitySupport\":false},\"dyn102516\":{\"name\":\"dyn102516\",\"id\":102516,\"multiEntitySupport\":false},\"dyn102538\":{\"name\":\"dyn102538\",\"id\":102538,\"options\":null,\"multiEntitySupport\":false,\"values\":[]},\"dyn102539\":{\"name\":\"dyn102539\",\"id\":102539,\"multiEntitySupport\":false}},\"globalData\":{\"entityIds\":["+entityIdToTest+"],\"fieldIds\":[1148,1147,1149,1106,1108,1107,1109,1101,1102,1104,1110,1111,1103],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(cslEntityTypeId, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }

            Thread.sleep(6000);

            if (!verifyAuditLog(cslEntityTypeId, cslID1, "Updated (Bulk)",completedBy , customAssert)) {
                customAssert.assertTrue(false, "Audit Log Tab Verified successfully after Bulk Edit");
            }

            if (!verifyAuditLog(cslEntityTypeId, cslID2, "Updated (Bulk)", completedBy, customAssert)) {
                customAssert.assertTrue(false, "Audit Log Tab Verified successfully after Bulk Edit");
            }

            //Validating target field values on show page after bulk edit
            HashMap<String, String> showPageFieldValuesMap = new HashMap<>();
            showPageFieldValuesMap.put("expected", expected);
            showPageFieldValuesMap.put("minmax", minMax);
            showPageFieldValuesMap.put("sigminmax", sigMinMax);
            showPageFieldValuesMap.put("suppliernumerator", supplierNumerator);
            showPageFieldValuesMap.put("supplierdenominator", supplierDenominator);
            showPageFieldValuesMap.put("finalnumerator", finalNumerator);
            showPageFieldValuesMap.put("finaldenominator", finalDenominator);
            showPageFieldValuesMap.put("actualnumerator", actualNumerator);
            showPageFieldValuesMap.put("actualdenominator", actualDenominator);
            showPageFieldValuesMap.put("suppliercalculation", supplierCalculation);
//			showPageFieldValuesMap.put("actualperformance", actualPerformance);
            showPageFieldValuesMap.put("finalperformance", finalPerformance);
            showPageFieldValuesMap.put("discrepancy", discrepancy);

            if (!validateFieldValuesOnShowPage(cslEntityTypeId, cslID1, showPageFieldValuesMap, customAssert)) {
                customAssert.assertTrue(false, "Field values validated unsuccessfully on show page");
            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Bulk Edit Scenario");
        }

        customAssert.assertAll();
    }

    //C44283
    @Test(groups = {"sanity"},enabled = false)
    public void TestBulkUploadPerformanceDataCSL() {

        logger.info("Validating Performance Data Bulk Upload functionality");
        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        String flowToTest = "sl automation flow";

        try {
            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

//            int serviceLevelId1 = 20864;
            int serviceLevelId1 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

            slToDelete.add(serviceLevelId1);

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

            if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
//				customAssert.assertAll();

            }

            if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }

            ArrayList<String> childserviceLevelId1sBulkUpload = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);
            addCSLToDelete(childserviceLevelId1sBulkUpload);

            String entityIds = "";
            String bulkUploadPerformanceDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadperformancedatafilename");

            //Child service level for bulk upload Performance Data to start from previously created CSL from the dependent test
            int initialCSLToStart = 1;
//			int numberOfCSLToUpdate = 4;
            int numberOfCSLToUpdate = 2;
            int totalCSLToUpdate = initialCSLToStart + numberOfCSLToUpdate;
            int staringExcelRowNum = 2;
            int excelRowToUpdateTill = staringExcelRowNum + numberOfCSLToUpdate;
            int cslId;

            String showResponse;

            //Utilising above created child Service Level Ids
            for (int i = initialCSLToStart; i < totalCSLToUpdate; i++) {

                entityIds = entityIds + childserviceLevelId1sBulkUpload.get(i) + ",";
            }

            entityIds = entityIds.substring(0, entityIds.length() - 1);

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("contractId", "");
            payloadMap.put("relationId", "");
            payloadMap.put("entityIds", entityIds);
            payloadMap.put("_csrf_token", "");

            Download download = new Download();

            download.downloadBulkPerformanceData(downloadFilePath + "/" + bulkUploadPerformanceDataFileName, payloadMap);

            Map<Integer, Object> columnDataMap = new HashMap<>();

            String[] excelUpdatedValuesFromConfigFile;
            String[] excelUpdatedColumnsFromConfigFile;
            String[] targetFieldNames = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadperformancedata", "columnnames").split(",");

            Map<Integer, List<String>> excelRowAndColMap = new HashMap<>();

            List<String> columnValues;

            for (int excelRowNum = staringExcelRowNum; excelRowNum < excelRowToUpdateTill; excelRowNum++) {

                excelUpdatedColumnsFromConfigFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadperformancedata", "rowcolumnnumbers" + excelRowNum).split(",");
                excelUpdatedValuesFromConfigFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadperformancedata", "rowcolumnvalues" + excelRowNum).split(",");

                if (excelUpdatedColumnsFromConfigFile.length != excelUpdatedValuesFromConfigFile.length) {
                    customAssert.assertTrue(false, "Configuration incorrect under header bulk upload performance data as number of column numbers column values are incorrect for row " + excelRowNum);
                    break;
                }
                columnValues = Arrays.asList(excelUpdatedValuesFromConfigFile);

                excelRowAndColMap.put(excelRowNum, columnValues);

                for (int i = 0; i < excelUpdatedValuesFromConfigFile.length; i++) {
                    try{

                        Double columnValue = Double.parseDouble(excelUpdatedValuesFromConfigFile[i]);
                        columnDataMap.put(Integer.parseInt(excelUpdatedColumnsFromConfigFile[i]), columnValue);

                    }catch (NumberFormatException e){
                        columnDataMap.put(Integer.parseInt(excelUpdatedColumnsFromConfigFile[i]), excelUpdatedValuesFromConfigFile[i]);

                    }catch (Exception e){
                        columnDataMap.put(Integer.parseInt(excelUpdatedColumnsFromConfigFile[i]), excelUpdatedValuesFromConfigFile[i]);
                    }

                }

                XLSUtils.editRowData(downloadFilePath, bulkUploadPerformanceDataFileName, "data", excelRowNum, columnDataMap);
                columnDataMap.clear();
            }
            payloadMap.clear();
            payloadMap.put("entityTypeId", String.valueOf(cslEntityTypeId));
            payloadMap.put("upload", "Submit");
            payloadMap.put("_csrf_token", "");

            UploadBulkData uploadBulkData = new UploadBulkData();
            uploadBulkData.hitBulkUploadUpload(downloadFilePath, bulkUploadPerformanceDataFileName, payloadMap);
            String uploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

            if (!uploadResponse.contains("200")) {
                customAssert.assertTrue(false, "Bulk Upload Performance Data uploaded unsuccessfully for file " + bulkUploadPerformanceDataFileName);
//				customAssert.assertAll();
            }

            int excelRowNum = staringExcelRowNum;
            List<String> excelRow;
            String fieldValueFromScreen;
            String fieldValueFromExcel;
            //waitForScheduler();
            Thread.sleep(120000);
//            C13601
            for (int i = totalCSLToUpdate - 1; i >= initialCSLToStart; i--) {

                cslId = Integer.parseInt(childserviceLevelId1sBulkUpload.get(i));
                show.hitShowVersion2(cslEntityTypeId, cslId);
                showResponse = show.getShowJsonStr();


                excelRow = excelRowAndColMap.get(excelRowNum);

                for (int j = 0; j < excelRow.size(); j++) {
                    fieldValueFromScreen = ShowHelper.getValueOfField(targetFieldNames[j], showResponse);
                    fieldValueFromExcel = excelRow.get(j);
                    if (targetFieldNames[j].equalsIgnoreCase("slmetval")) {

                        if (!fieldValueFromExcel.equalsIgnoreCase(fieldValueFromScreen)) {
                            customAssert.assertTrue(false, "SLMET value expected and actual mismatch for CSL ID " + cslId);
                        }
                    } else {
                        if (!(String.valueOf(Double.parseDouble(fieldValueFromExcel)).equalsIgnoreCase(fieldValueFromScreen))) {
                            customAssert.assertTrue(false, "Expected and Actual Value for the field " + targetFieldNames[j] +
                                    " is not equal for the CSL ID " + cslId);
                        }
                    }
                }
                excelRowNum++;
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Performance Data Bulk Upload functionality " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C10772 C10773 C10776
    @Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = true,priority = 0)          //Validated 24 Sept
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
            serviceLevelBulkUpload = serviceLevelId1;

            slToDelete.add(serviceLevelBulkUpload);

            if (serviceLevelBulkUpload != -1) {
//
                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenamebulkupload");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

                currentTimeStampForSLTemplateUploadCSLCreation = getCurrentTimeStamp();

                if (!uploadPerformanceDataFormat(serviceLevelBulkUpload, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelBulkUpload, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                }


                List<String> childserviceLevelId1sForBulkUpload = new ArrayList<>();
                List<String> childServiceLevelShortCodeIdsList = new ArrayList<>();
                List<String> childserviceLevelId1sForNonBulkUpload = new ArrayList<>();
                List<String> childServiceLevelDescList = new ArrayList<>();

                for (int i = 11; i < 16; i++) {

                    if (i < 14) {
                        csl = Integer.parseInt(childserviceLevelId1s.get(i));

                        childserviceLevelId1sForBulkUpload.add(childserviceLevelId1s.get(i));

                        show.hitShowVersion2(cslEntityTypeId, csl);
                        showPageResponse = show.getShowJsonStr();

                        shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);
                        description = ShowHelper.getValueOfField("description", showPageResponse);

                        childServiceLevelShortCodeIdsList.add(shortCodeId);
                        childServiceLevelDescList.add(description);


                    } else {
                        childserviceLevelId1sForNonBulkUpload.add(childserviceLevelId1s.get(i));
                    }

                }

                String bulkUploadRawDataZipFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkuploadrawdatazipfilename");
                String currDate = DateUtils.getCurrentDateInAnyFormat("dd-MMM-yyyy");
                String bulkUploadRawDataZipDir = bulkUploadRawDataZipFileName.replace("date", currDate);

                bulkUploadRawDataZipFileName = bulkUploadRawDataZipFileName + ".zip";

                String downloadFilePathBulkUpload = downloadFilePath + "/" + bulkUploadRawDataZipFileName;

                download.downloadBulkTemplateRawData(downloadFilePathBulkUpload, childserviceLevelId1sForBulkUpload);
                FileUtils.clearDirectory(unzipFilePath);
                FileUtils.unzip(downloadFilePathBulkUpload, unzipFilePath);

                if (!validateDownloadedBulkTemplateRawDataWithUploadedSlTemplateRawData(unzipFilePath, bulkUploadRawDataZipDir, performanceDataFormatFileName, childserviceLevelId1sForBulkUpload,
                        childServiceLevelShortCodeIdsList, childServiceLevelDescList, customAssert)) {
                    customAssert.assertTrue(false, "validate Downloaded BulkTemplate RawData With Uploaded SlTemplate RawData failed");
                }

                HashMap<String, String> renamedFilesMap = new HashMap<>();


                String newFileNameAfterRename = "BulkUploadRawData.xlsx";

                renamedFilesMap = renameFolderAndFiles(unzipFilePath, renamedFilesMap, newFileNameAfterRename, customAssert);

                randomFileNumber = 1;

                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename_bulkuploadrawdata");
                XLSUtils.updateColumnValue(uploadFilePath,rawDataFileName,"Sheet1",100,100,"");
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
                Thread.sleep(10000);

//                C10776
                int cslID;
                int k = 1;
                String rawDataFileToCheck = BulkUploadFileNameExcel.split(".xlsx")[0];

                for (String cslIdForBulkUploadValidation : childserviceLevelId1sForBulkUpload) {

                    cslID = Integer.parseInt(cslIdForBulkUploadValidation);
                    String rawDataFile = rawDataFileToCheck + k + ".xlsx";

                    if (!validateStructuredPerformanceDataCSL(cslID, rawDataFile, "Done", "View Structured Data", completedBy, customAssert)) {
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

                for (String cslIdForNonBulkUploadValid : childserviceLevelId1sForNonBulkUpload) {

                    if (!validateStructuredPerformanceDataCSL(Integer.parseInt(cslIdForNonBulkUploadValid), "",
                            "", "", completedBy, customAssert)) {
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

            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Validating the scenario " + e.getMessage());
        }

        customAssert.assertAll();

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

    private synchronized Boolean validateFieldsOnShowPage(int entityTypeId,int entityId,String dynamicField,String customFieldValue,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            Show show = new Show();
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String customCurrencyFieldValue = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicField).get("values").toString();

            if (!customCurrencyFieldValue.equalsIgnoreCase(customFieldValue)) {
                customAssert.assertTrue(false, "Expected and Actual Value Of custom field didn't match");
                validationStatus = false;

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating values on show page " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

        String sqlQuery = "select subject,sent_successfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
                + "order by id desc";
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
                actualBodyHtml = recordFromSystemEmailTable.get(i).get(2);

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

    private Boolean validateSentSuccessfullyFlag(List<List<String>> recordFromSystemEmailTable, CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {

            if (!recordFromSystemEmailTable.get(0).get(1).equalsIgnoreCase("t")) {
                customAssert.assertTrue(false, "Sent Successfully flag is not true");
                validationStatus = false;
            }

        } catch (Exception e) {

            customAssert.assertTrue(false, "Exception while validating Sent Successfully Flag from system email table with the expected attachment name");
            validationStatus = false;

        }
        return validationStatus;
    }

    private Boolean validate_Max_SigMax_AbsentSL(int slID, String flow, CustomAssert customAssert) {

        logger.info("Validating the target values such as maximum or sig Max are Absent for different minimum/maximum levels");
        Boolean validationStatus = true;
        String showResponse;
        try {
            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId, slID);
            showResponse = show.getShowJsonStr();
            JSONObject minimumJson = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("minimum");
            JSONObject sigMinMaxJson = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("sigMinMax");

            if (flow.equalsIgnoreCase("sl max level 1") || flow.equalsIgnoreCase("sl min level 1")) {

                if (minimumJson.has("values")) {
                    customAssert.assertTrue(false, "Maximum / Minimum is not expected as field for SL " + slID + " for the flow " + flow);
                }
                if (sigMinMaxJson.has("values")) {
                    customAssert.assertTrue(false, "Sig Maximum / Minimum is not expected as field for SL " + slID);
                }
            } else if (flow.equalsIgnoreCase("sl max level 2") || flow.equalsIgnoreCase("sl min level 2")) {

                if (!minimumJson.has("values")) {
                    customAssert.assertTrue(false, "Maximum / Minimum is expected as field for SL " + slID + " for the flow " + flow);
                }
                if (sigMinMaxJson.has("values")) {
                    customAssert.assertTrue(false, "Sig Maximum / Minimum is not expected as field for SL " + slID + " for the flow " + flow);
                }
            } else if (flow.equalsIgnoreCase("sl max level 3") || flow.equalsIgnoreCase("sl min level 3")) {

                if (!minimumJson.has("values")) {
                    customAssert.assertTrue(false, "Maximum / Minimum is expected as field for SL " + slID + " for the flow " + flow);
                }
                if (!sigMinMaxJson.has("values")) {
                    customAssert.assertTrue(false, "Sig Maximum / Minimum is expected as field for SL " + slID + flow);
                }
            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating target fields absence for different minimum/maximum levels for SL " + slID);
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

    private Boolean validateSLMETStatus(String showResponse, Float finalPerformanceExpected, String slMetStatusToValidate, String expectedColorCode, CustomAssert customAssert) {

        logger.info("Validating SLMet Status " + slMetStatusToValidate);
        Boolean validationStatus = true;
        try {

            JSONObject showResponseJson = new JSONObject(showResponse);
            String actualSlMetStatus;
            String actualColorCode;
            String actualFinalPerformance;

            try{
                actualSlMetStatus = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slMet").getJSONObject("values").get("name").toString();
            }catch (Exception e){
                actualSlMetStatus = "Exception Occured / Value Not Present";
            }

            try{
                actualColorCode = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slMet").getJSONObject("values").get("colorCode").toString();
            }catch (Exception e){
                actualColorCode = "Exception Occured / Value Not Present";
            }

            try{
                actualFinalPerformance = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalPerformance").get("values").toString();
            }catch (Exception e){
                actualFinalPerformance = "Exception Occured / Value Not Present";
            }

            if (!slMetStatusToValidate.equalsIgnoreCase(actualSlMetStatus)) {
                customAssert.assertTrue(false, "SLMET Status expected and actual mismatch, Expected SLMET Status " + slMetStatusToValidate + " Actual SLMET Status " + actualSlMetStatus);
                validationStatus = false;
            }

            if (!expectedColorCode.equalsIgnoreCase(actualColorCode)) {
                customAssert.assertTrue(false, "Color code expected and actual mismatch, Expected Color code " + expectedColorCode + " Actual Color code " + actualColorCode);
                validationStatus = false;
            }

            if (!finalPerformanceExpected.toString().equals(actualFinalPerformance)) {
                customAssert.assertTrue(false, "Final Performance expected and actual mismatch, Expected Final Performance " + finalPerformanceExpected + " Actual Final Performance " + actualFinalPerformance);
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating SL MET Status " + slMetStatusToValidate + e.getMessage()) ;
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

    private boolean validateFieldValuesOnShowPage(int entityTypeId, int entityId,
                                                  HashMap<String, String> showPageFieldValuesMap, CustomAssert customAssert) {

        Boolean validationStatus = true;
        Show show = new Show();

        try {
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();
            String fieldName;
            String expectedFieldValue;
            String actualFieldValue;

            for (Map.Entry<String, String> entry : showPageFieldValuesMap.entrySet()) {

                fieldName = entry.getKey();
                expectedFieldValue = entry.getValue();
                actualFieldValue = ShowHelper.getValueOfField(fieldName, showResponse);

                if (!expectedFieldValue.equalsIgnoreCase(actualFieldValue)) {
                    customAssert.assertTrue(false, "Expected and Actual Field Value did not match for Field " + fieldName +
                            " and  entity ID " + entityId);
                    validationStatus = false;
                }
            }

        } catch (Exception e) {

            customAssert.assertTrue(false, "Exception while validating field values on show page " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;

    }

//    @AfterClass(groups = {"sanity","sprint"})
//	public void afterClass() {
//
//		logger.debug("Number CSL To Delete " + cslToDelete.size());
//		EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);
//
//		logger.debug("Number SL To Delete " + slToDelete.size());
//		EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);
//
//	}

    private int getActiveserviceLevelId1(String flowToTest, String PCQ, String DCQ, String user, CustomAssert customAssert) {

        int serviceLevelId1 = -1;
        try {

            serviceLevelId1 = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);

            if (serviceLevelId1 != -1) {
                List<String> workFlowSteps;
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                if (!performWorkFlowActions(slEntityTypeId, serviceLevelId1, workFlowSteps, user,false, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1);
                    return -1;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
        }
        return serviceLevelId1;
    }

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId1, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childserviceLevelId1s = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId1,50);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId1);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId1);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childserviceLevelId1s = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childserviceLevelId1s.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId1);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId1);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId1 + " " + e.getMessage());
        }

        return childserviceLevelId1s;
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

    private int getserviceLevelId1(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId1 = -1;

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
                serviceLevelId1 = CreateEntity.getNewEntityId(createResponse, "service levels");

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return serviceLevelId1;
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

    //C10772
    private Boolean validateDownloadedBulkTemplateRawDataWithUploadedSlTemplateRawData(String unzipFilePath, String bulkUploadRawDataZipDir,
                                                                                       String slTemplateFileName,
                                                                                       List<String> childserviceLevelId1sForBulkUpload,
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
            for (int i = 0; i < childserviceLevelId1sForBulkUpload.size(); i++) {

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

    //    C10638
    private Boolean validateStructuredPerformanceDataCSL(int CSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert customAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 60000L;
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
                customAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for service level id " + serviceLevelId1);
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(15000);

            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
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

    public void executeParallelServiceToGetActiveServiceLevelId() throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<Integer> future1 = executorService.submit(this::getActiveServiceLevelId);

        Future<Integer> future2 = executorService.submit(this::getActiveServiceLevelId);

        // wait until result will be ready
        serviceLevelId1 = future1.get();

        // wait only certain timeout otherwise throw an exception
        serviceLevelId12 = future2.get(1, TimeUnit.SECONDS);

        executorService.shutdown();
    }

    private int getActiveServiceLevelId(){

//		int serviceLevelId = serviceLevelId1 + 1;
        int serviceLevelId = -1;
        String flowToTest = "sl automation flow";
//
        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        String user = auditLogUser;
//
        CustomAssert customAssert = new CustomAssert();
        try {

            serviceLevelId = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);

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

}

