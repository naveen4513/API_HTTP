package com.sirionlabs.test.serviceData;

import com.google.inject.internal.cglib.core.$Customizer;
import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.DownloadTemplates.DownloadServiceDataTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.localmongo.LocalMongoHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestServiceDataBulkCreate {

    private final static Logger logger = LoggerFactory.getLogger(TestServiceDataBulkCreate.class);
    private static int serviceDataEntityTypeId = -1;
    private static int contractEntityTypeId = -1;
    private static int serviceDataListId = -1;
    private static Long schedulerJobTimeOut = 600000L;
    private static Long schedulerJobPollingTime = 5000L;
    private static int auditLogTabId = -1;
    private static String auditLogExpectedRequestedBy = null;
    private static String auditLogExpectedCompletedBy = null;
    private static String auditLogActionNameColumnId = null;
    private static String auditLogCompletedByColumnId = null;
    private static String auditLogRequestedByColumnId = null;
    private static String auditLogCommentColumnId = null;
    private static String auditLogHistoryColumnId = null;
    private static String auditLogDocumentColumnId = null;
    private String configFilePath = null;
    private String configFileName = null;
    private String bulkCreateTemplateFilePath = null;
    private Integer bulkCreateTemplateId = -1;

    private String serviceData = "service data";

    PostgreSQLJDBC postgreSQLJDBC;
    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataBulkCreateTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataBulkCreateTestConfigFileName");
        bulkCreateTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateFilePath");
        bulkCreateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateId"));
        serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
        contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobTimeOut");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            schedulerJobTimeOut = Long.parseLong(temp.trim());

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobPollingTime");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            schedulerJobPollingTime = Long.parseLong(temp.trim());

        serviceDataListId = ConfigureConstantFields.getListIdForEntity("service data");
        auditLogTabId = TabListDataHelper.getIdForTab("audit log");
        auditLogExpectedCompletedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log validation",
                "expectedCompletedBy");
        auditLogExpectedRequestedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "audit log validation",
                "expectedRequestedBy");

        postgreSQLJDBC = new PostgreSQLJDBC();
    }

    //TC-99870, TC-99871
    @Test(enabled = true)
    public void validateBulkCreateTemplateNameAndExtension() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Service Data Bulk Create Template");
            String sectionName = "download template validation";
            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "contractid"));

            logger.info("Hitting Download API.");
            Download downloadObj = new Download();
            HttpResponse downloadResponse = downloadObj.hitDownload(bulkCreateTemplateId, contractEntityTypeId, contractId);

            if (downloadResponse == null)
                throw new SkipException("Unexpected Error at Downloading Service Bulk Create Template. Hence skipping test.");

            String expectedTemplateName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "expectedTemplateName");

            Header[] headers = downloadResponse.getAllHeaders();
            for (Header oneHeader : headers) {
                if (oneHeader.toString().trim().contains("Content-Disposition")) {
                    String[] temp = oneHeader.toString().trim().split(Pattern.quote("filename=\""));
                    String actualFileName = temp[1].trim();
                    actualFileName = actualFileName.substring(0, actualFileName.length() - 1);

                    if (!oneHeader.toString().trim().contains(expectedTemplateName)) {
                        csAssert.assertTrue(false, "Expected Template Name: [" + expectedTemplateName + "] and Actual Template Name: [" +
                                actualFileName + "]");
                    }

                    if (!actualFileName.endsWith(".xlsm")) {
                        csAssert.assertTrue(false, "Service Data Bulk Create Template doesn't have Extension as xlsm.");
                    }
                    break;
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Service Data Bulk Create Template Name & Extension Validation. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC-99872
    @Test(priority = 1, enabled = true)
    public void validateSheetsInBulkCreateTemplate() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String sectionName = "download template validation";
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "templateFileName");
            int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "contractid"));
            logger.info("Downloading Service Data Bulk Create Template.");
            Boolean templateDownloaded = BulkTemplate.downloadBulkCreateTemplate(bulkCreateTemplateFilePath, templateFileName, bulkCreateTemplateId,
                    contractEntityTypeId, contractId);

            if (!templateDownloaded)
                throw new SkipException("Couldn't download Service Data Bulk Create Template. Hence Skipping test.");

            XLSUtils xlsObj = new XLSUtils(bulkCreateTemplateFilePath, templateFileName);

            logger.info("Getting all Sheet Names in Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);
            List<String> allSheetNames = xlsObj.getSheetNames();

            if (allSheetNames.isEmpty()) {
                throw new SkipException("Couldn't get any sheet in Service Data Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" +
                        templateFileName + "]");
            }

            String[] expectedSheetNamesArr = {"Instructions", "Information", "Service Data"};
            List<String> allExpectedSheetNames = new ArrayList<>();
            allExpectedSheetNames.addAll(Arrays.asList(expectedSheetNamesArr));

            for (String expectedSheetName : allExpectedSheetNames) {
                if (!allSheetNames.contains(expectedSheetName.trim())) {
                    csAssert.assertTrue(false, "Service Data Bulk Create Template doesn't contain Sheet " + expectedSheetName);
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Sheets in Service Data Bulk Create Template. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC-99873, TC-99881, TC-99887, TC-99879, TC-99885, TC-99882
    @Test(priority = 2,enabled = true)
    public void validateDataSheet() {
        CustomAssert csAssert = new CustomAssert();

        try {
            //Validate Fields in Data Sheet
            String sectionName = "data sheet validation";
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
                    "templateFileName");
            logger.info("Validating Fields in Data Sheet of Service Data Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);

            if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
                throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
                        "]. Hence skipping test");
            }

            String expectedDataFields = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "expectedFieldsInDataSheet");
            String[] expectedFieldsInDataSheet = expectedDataFields.split(Pattern.quote(","));
            List<String> allExpectedFields = new ArrayList<>(Arrays.asList(expectedFieldsInDataSheet));

            List<String> allHeadersTemp = XLSUtils.getHeaders(bulkCreateTemplateFilePath, templateFileName, "Service Data");
            List<String> allHeaders = new ArrayList<>();
            for (String string : allHeadersTemp) {
                allHeaders.add(string.toLowerCase());
            }

            if (allHeaders.isEmpty())
                throw new SkipException("Couldn't get Headers in Data Sheet of Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" + templateFileName + "]");

            for (String expectedField : allExpectedFields) {
                if (!allHeaders.contains(expectedField.trim().toLowerCase())) {
                    csAssert.assertTrue(false, "Data Sheet doesn't contain field [" + expectedField + "].");
                }
            }

            //Validate All Validation Rule
            logger.info("Validating that all Validation Rules are present in Data Sheet of Bulk Create Template.");
            List<String> allFields = XLSUtils.getExcelDataOfOneRow(bulkCreateTemplateFilePath, templateFileName, "Service Data", 5);
            if (allFields.isEmpty())
                throw new SkipException("Couldn't get Data at Row No 5 in Data Sheet of Bulk Create Template. Hence skipping test.");

            String[] expectedValidationRules = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
                    "expectedRulesInDataSheet").split(Pattern.quote(","));
            List<String> uniqueValidationRules = new ArrayList<>();

            for (String validationRule : allFields) {
                if (!uniqueValidationRules.contains(validationRule.trim()))
                    uniqueValidationRules.add(validationRule.trim());
            }

            for (String expectedRule : expectedValidationRules) {
                if (!uniqueValidationRules.contains(expectedRule.trim())) {
                    csAssert.assertTrue(false, "Row No 5 in Data Sheet of Bulk Create Template doesn't contain Validation Rule " + expectedRule);
                }
            }

            //Validate Mandatory Fields
            logger.info("Validating that Mandatory rule is Present for all Mandatory Fields.");
            String[] expectedMandatoryFields = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
                    "expectedMandatoryFields").split(Pattern.quote(","));
            List<String> allExpectedMandatoryFieldsInDataSheet = new ArrayList<>();
            allExpectedMandatoryFieldsInDataSheet.addAll(Arrays.asList(expectedMandatoryFields));

            for (String expectedMandatoryField : allExpectedMandatoryFieldsInDataSheet) {
                expectedMandatoryField = expectedMandatoryField.trim();

                if (!allHeaders.contains(expectedMandatoryField.toLowerCase())) {
                    csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not present in Data Sheet of Bulk Create Template");
                } else {
                    if (!allFields.get(allHeaders.indexOf(expectedMandatoryField.toLowerCase())).trim().contains("Mandatory")) {
                        csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not marked as Mandatory in Data Sheet of Bulk Create Template");
                    }
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Data Sheet of Service Data Bulk Create Template. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC-99875
    @Test(priority = 3,enabled = true)
    public void validateInstructionsSheet() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
                    "templateFileName");
            logger.info("Validating Instructions Sheet of Service Data Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);

            if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
                throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
                        "]. Hence skipping test");
            }

            List<String> allInstructionKeys = XLSUtils.getOneColumnDataFromMultipleRows(bulkCreateTemplateFilePath, templateFileName, "Instructions",
                    5, 0, 3);
            List<String> allInstructionValues = XLSUtils.getOneColumnDataFromMultipleRows(bulkCreateTemplateFilePath, templateFileName, "Instructions",
                    6, 0, 3);

            if (allInstructionKeys.isEmpty() || allInstructionValues.isEmpty()) {
                throw new SkipException("Couldn't find all the Expected Instruction Keys and Values in Instructions Sheet of Bulk Create Template. Hence skipping test.");
            }

            String[] expectedKeys = {"Template Type", "Template Version", "Feature Instructions"};
            for (int i = 0; i < allInstructionKeys.size(); i++) {
                if (!allInstructionKeys.get(i).trim().equalsIgnoreCase(expectedKeys[i].trim())) {
                    csAssert.assertTrue(false, "Expected Instruction Key at Row #" + (i + 1) + ": [" + expectedKeys[i].trim() +
                            "] and Actual Key found is: " + allInstructionKeys.get(i).trim());
                }
            }

            String[] expectedValues = {"Bulk Create", "1.2", "1. The template is used to create entities in bulk.\n" +
                    "\n" +
                    "2. It is advisable that user always downloads latest template and use.\n" +
                    "\n" +
                    "3. A bulk create template consists atleast 3 sheets 1) Instructions 2) Information 3) Entity data sheet(1 or more). Entity data sheets contain data related to the Entity, Child Entities, Tables part of the Entity and/or Child Entity.\n" +
                    "\n" +
                    "4. \"Information\" sheet captured details of the template e.g. Download date, Downloaded by etc.\n" +
                    "\n" +
                    "5. Content of Entity data sheets is dynamic and depends on the configuration set by admin.\n" +
                    "\n" +
                    "6. Entity data sheet has following initial columns 1) Header (1st row) 2) Instructions(5th row) 3) Reference Data(6th row). User should populate data from 7th row ownwards.\n" +
                    "\n" +
                    "7. Header: Contains name of metadata.\n" +
                    "8. Instruction: Captures information related to metadata validation e.g. limit restrictions, mandatory or not etc.\n" +
                    "\n" +
                    "9. Reference Data: Contains information related to master data available against the metadata e.g. master list of functions & services.\n" +
                    "\n"};

            for (int i = 0; i < allInstructionValues.size(); i++) {
                if (!allInstructionValues.get(i).trim().equalsIgnoreCase(expectedValues[i].trim())) {
                    csAssert.assertTrue(false, "Expected Instruction Value at Row #" + (i + 1) + ": [" + expectedValues[i].trim() +
                            "] and Actual Value found is: " + allInstructionValues.get(i).trim());
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Instructions Sheet of Service Data Bulk Create Template. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC-99876, TC-99877
    @Test(priority = 4,enabled = true)
    public void validateInformationSheet() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
                    "templateFileName");
            logger.info("Validating Information Sheet of Service Data Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" + templateFileName + "]");

            if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
                throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
                        "]. Hence skipping test");
            }

            List<String> allInformationKeys = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(bulkCreateTemplateFilePath, templateFileName,
                    "Information", 0, 0, 8);

            if (allInformationKeys.isEmpty()) {
                throw new SkipException("Couldn't get all Keys from Information Sheet of Bulk Create Template. Hence skipping test.");
            }

            String[] expectedKeys = {"Bulk Create - Service Data", "", "Downloaded By", "Download date & time", "Parent entity", "",
                    "CONFIDENTIALITY AND DISCLAIMER\r\n" +
                            "The information in this document is proprietary and confidential and is provided upon the recipient's promise to keep such information " +
                            "confidential. In no event may this information be supplied to third parties without <Client's Name>'s prior written consent.\r\n" +
                            "The following notice shall be reproduced on any copies permitted to be made:\r\n" +
                            "<Client's Name> Confidential & Proprietary. All rights reserved.", ""};

            for (int i = 0; i < allInformationKeys.size(); i++) {
                if (!allInformationKeys.get(i).trim().contains(expectedKeys[i].trim())) {
                    csAssert.assertTrue(false, "Expected Information Key at Row #" + (i + 1) + ": [" + expectedKeys[i].trim() +
                            "] and Actual Key found is: " + allInformationKeys.get(i).trim());
                }
            }

            List<String> allInformationValues = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(bulkCreateTemplateFilePath, templateFileName,
                    "Information", 1, 0, 7);
            if (allInformationValues.isEmpty()) {
                throw new SkipException("Couldn't get all Values from Information Sheet of Bulk Create Template. Hence skipping test.");
            }

            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "information sheet validation");

            String expectedDownloadedBy = properties.get("expecteddownloadedby").trim();
            String expectedSupplier = properties.get("expectedsupplier").trim();
            String expectedParentEntity = properties.get("expectedparententity").trim();

            if (!allInformationValues.get(2).trim().equalsIgnoreCase(expectedDownloadedBy)) {
                csAssert.assertTrue(false, "Expected Downloaded By: " + expectedDownloadedBy + " and Actual Downloaded By: " +
                        allInformationValues.get(2));
            }

//			if (!allInformationValues.get(4).trim().equalsIgnoreCase(expectedSupplier)) {
//				csAssert.assertTrue(false, "Expected Supplier: " + expectedSupplier + " and Actual Supplier: " + allInformationValues.get(4));
//			}

            if (!allInformationValues.get(4).trim().equalsIgnoreCase(expectedParentEntity)) {
                csAssert.assertTrue(false, "Expected Parent Entity: " + expectedParentEntity + " and Actual Parent Entity: " +
                        allInformationValues.get(4));
            }

            //Deleting Bulk Create Template
            new File(bulkCreateTemplateFilePath + "/" + templateFileName).delete();
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Information Sheet of Service Data Bulk Create Template. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderForBulkCreateTemplateUpload() throws ConfigurationException {
        logger.info("Setting all Bulk Create Upload Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();

        String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadFlowsToValidate").split(Pattern.quote(","));
        for (String flow : allFlows) {
            if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                flowsToTest.add(flow.trim());
            } else {
                logger.info("Flow having name [{}] not found in Service Data Bulk Create Config File.", flow.trim());
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForBulkCreateTemplateUpload", priority = 5, enabled = true)
    public void testBulkCreateUpload(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Service Data Bulk Create Template Upload Flow [{}]", flowToTest);
            logger.info("Uploading Service Data Bulk Create Template for Flow [{}]", flowToTest);
            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            String bulkCreateTemplateFileName = properties.get("bulkcreatetemplatefilename");

            if (!(new File(bulkCreateTemplateFilePath + "/" + bulkCreateTemplateFileName).exists())) {
                throw new SkipException("Couldn't find Bulk Create Template File at Location: " + bulkCreateTemplateFilePath + "/" + bulkCreateTemplateFileName);
            }

            int contractId = Integer.parseInt(properties.get("contractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, bulkCreateTemplateFileName, contractEntityTypeId, contractId,
                    serviceDataEntityTypeId, bulkCreateTemplateId);
            String expectedMessage = properties.get("expectedmessage").trim();

            logger.info("Actual Bulk Create Template Upload API Response: {} and Expected Result: {}", uploadResponse, expectedMessage);
            if (uploadResponse == null || !uploadResponse.trim().toLowerCase().contains(expectedMessage.toLowerCase())) {
                csAssert.assertTrue(false, "Bulk Create Template Upload Response doesn't match with Expected Response for Flow [" + flowToTest + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Service Data Bulk Create Template Upload for Flow [" + flowToTest +
                    "]. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderForBulkCreateTemplateProcessing() throws ConfigurationException {
        logger.info("Setting all Bulk Create Template Processing Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();
        List<ExcelData> filesList = new ArrayList<>();

        String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "processingFlowsToValidate").split(Pattern.quote(","));
        for (String flow : allFlows) {
            if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                filesList.add(new ExcelData(Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "contractid")), ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "bulkcreatetemplatefilename")));
                flowsToTest.add(flow.trim());
            } else {
                logger.info("Flow having name [{}] not found in Service Data Bulk Create Template Bulk Upload Config File.", flow.trim());
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        LocalMongoHelper localMongoHelper = new LocalMongoHelper();
        List<String> response = localMongoHelper.getExcelProperties("service data bulk create");

        for (String str : response) {
            if (ParseJsonResponse.validJsonResponse(str)) {
                JSONObject jsonObject = new JSONObject(str);

                String fileName = jsonObject.getString("_id");
                if (filesList.contains(new ExcelData(-1, fileName))) {

                    Download download = new Download();
                    download.hitDownload(bulkCreateTemplateFilePath, fileName, 1001, contractEntityTypeId, findContractIdFromExcelData(fileName, filesList));

                    Set<String> set = jsonObject.getJSONObject("Service Data").keySet();
                    for (String rowNumber : set) {

                        List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));
                        List shouldSetRandomColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "shouldsetrandomcolumnids").split(","));
                        Map<String, Object> dataMap;

                        dataMap = new HashMap<>();

                        //for (Map.Entry<String, String> entry : map.entrySet()) {
                        for (String internalKey : jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).keySet()) {
                            if (NumberUtils.isParsable(jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).getString(internalKey))) {
                                if (dateColumnIds.contains(internalKey)) {
                                    dataMap.put(internalKey, DateUtil.getJavaDate(Double.parseDouble(jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).getString(internalKey))));
                                } else
                                    dataMap.put(internalKey, Double.parseDouble(jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).getString(internalKey)));
                            } else {
                                if(shouldSetRandomColumnIds.contains(internalKey))
                                    dataMap.put(internalKey, jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).getString(internalKey)+ RandomNumbers.getRandomNumberWithinRangeIndex(1000000,9999999));
                                else
                                    dataMap.put(internalKey, jsonObject.getJSONObject("Service Data").getJSONObject(rowNumber).getString(internalKey));
                            }
                        }

                        boolean editDone = XLSUtils.editRowDataUsingColumnId(bulkCreateTemplateFilePath, fileName, "Service Data", Integer.parseInt(rowNumber), dataMap);
                        System.out.println(editDone);

                    }
                }


            }
        }


        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForBulkCreateTemplateProcessing", priority = 6,enabled = true)//6
    public void testBulkCreateTemplateProcessing(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Service Data Bulk Create Template Processing Flow [{}]", flowToTest);
            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
            String templateFileName = properties.get("bulkcreatetemplatefilename");
            UserTasksHelper.removeAllTasks();

            logger.info("Uploading Bulk Create Template for Flow [{}]", flowToTest);
            int contractId = Integer.parseInt(properties.get("contractid"));

            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, templateFileName, contractEntityTypeId, contractId,
                    serviceDataEntityTypeId, bulkCreateTemplateId);

            if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                String fetchResponse = fetchObj.getFetchJsonStr();

                if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

                    if (newTaskId == -1) {
                        throw new SkipException("Couldn't get Task Id for Bulk Create Template Scheduler Job for Flow [" + flowToTest + "]");
                    }

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);
                    String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
                    logger.info("Expected Result of Bulk Create Template Processing for Flow [{}] is {}", flowToTest, expectedResult);
                    String jobStatus = schedulerJob.get("jobPassed").trim();

                    if (jobStatus.equalsIgnoreCase("skip")) {
                        throw new SkipException(schedulerJob.get("errorMessage") + ". Hence skipping test.");
                    }

                    if (expectedResult.equalsIgnoreCase("success")) {
                        if (jobStatus.equalsIgnoreCase("false")) {
                            csAssert.assertTrue(false, "Bulk Create Template Processing failed for Flow [" + flowToTest +
                                    "] whereas it was expected to process successfully");
                        } else {
                            logger.info("Bulk Create Template Processed successfully for Flow [" + flowToTest + "]");

                            //Verify Audit Logs
                            verifyAuditLogsForProcessingFlows(flowToTest, contractId, templateFileName, csAssert);
                        }
                    } else {
                        if (jobStatus.equalsIgnoreCase("true")) {
                            csAssert.assertTrue(false, "Bulk Create Template Processed successfully for Flow [" + flowToTest +
                                    "] whereas it was expected to fail.");
                        } else {
                            logger.info("Bulk Create Template Processing failed for Flow [" + flowToTest + "]");
                        }
                    }

                    if (jobStatus.equalsIgnoreCase("true")) {
                        //Delete Newly Created Service Data
                        deleteServiceDataOfContract(flowToTest, contractId, csAssert);
                    }
                } else {
                    csAssert.assertTrue(false, "Fetch API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
                }
            } else {
                throw new SkipException("Couldn't upload Bulk Create Template Successfully for Flow [" + flowToTest + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Service Data Bulk Create Template Processing for Flow [" + flowToTest + "]. " +
                    e.getMessage());
        }
        csAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderForBulkCreateTemplateValueUpdate() throws ConfigurationException {
        logger.info("Setting all Bulk Create Template Value Update to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> flowsToTest = new ArrayList<>();
        List<ExcelData> filesList = new ArrayList<>();

        String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "processingflowstovalidatevalueupdate").split(Pattern.quote(","));
        for (String flow : allFlows) {
            if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                filesList.add(new ExcelData(Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "contractid")), ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "bulkcreatetemplatefilename")));
                flowsToTest.add(flow.trim());
            } else {
                logger.info("Flow having name [{}] not found in Service Data Bulk Create Template Bulk Upload Config File.", flow.trim());
            }
        }

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }
//    C89312
    @Test(dataProvider = "dataProviderForBulkCreateTemplateValueUpdate",enabled = true)//6
    public void testBulkCreateValueUpdate(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        String desc = "test invoicing type";

        DateUtils dateUtils = new DateUtils();
        int serviceDataId = -1;
        try {
            logger.info("Validating Service Data Bulk Create Template Processing Flow [{}]", flowToTest);
            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
            String templateFileName = properties.get("bulkcreatetemplatefilename");
            UserTasksHelper.removeAllTasks();

            logger.info("Uploading Bulk Create Template for Flow [{}]", templateFileName);
            int contractId = Integer.parseInt(properties.get("contractid"));

            String timeStamp = dateUtils.getCurrentTimeStampDB().get(0).get(0);
            String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, templateFileName, contractEntityTypeId, contractId,
                    serviceDataEntityTypeId, bulkCreateTemplateId);

            if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                String fetchResponse = fetchObj.getFetchJsonStr();

                if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

                    if (newTaskId == -1) {
                        throw new SkipException("Couldn't get Task Id for Bulk Create Template Scheduler Job for Flow [" + flowToTest + "]");
                    }

                    Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);
                    String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
                    logger.info("Expected Result of Bulk Create Template Processing for Flow [{}] is {}", flowToTest, expectedResult);
                    String jobStatus = schedulerJob.get("jobPassed").trim();

                    if (jobStatus.equalsIgnoreCase("skip")) {
                        throw new SkipException(schedulerJob.get("errorMessage") + ". Hence skipping test.");
                    }

                    if (expectedResult.equalsIgnoreCase("success")) {
                        if (jobStatus.equalsIgnoreCase("false")) {
                            csAssert.assertTrue(false, "Bulk Create Template Processing failed for Flow [" + flowToTest +
                                    "] whereas it was expected to process successfully");
                        } else {
                            logger.info("Bulk Create Template Processed successfully for Flow [" + flowToTest + "]");
                            String sqlQuery = "select id from contract_service_data where display_name ilike '%" + desc + "%'" + "AND date_created > '" + timeStamp + "'";

                            List<List<String>> sqlQueryResults = postgreSQLJDBC.doSelect(sqlQuery);

                            if(sqlQueryResults.size() == 0){
                                csAssert.assertTrue(false,"Newly Created Service Date Id not found in DB");
                            }else {

                                serviceDataId = Integer.parseInt(sqlQueryResults.get(0).get(0));

                                String expected_invoicing_type = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expected_invoicing_type");
                                String expected_invoicing_billing_period = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"expected_invoicing_billing_period");

                                Show show = new Show();
                                show.hitShowVersion2(serviceDataEntityTypeId,serviceDataId);
                                String showResponse = show.getShowJsonStr();

                                String actualBillingPeriod = ShowHelper.getValueOfField("billing period",showResponse);
                                String actualInvoicingType = ShowHelper.getValueOfField("invoicingtype",showResponse);

                                if(!actualBillingPeriod.equalsIgnoreCase(expected_invoicing_billing_period)){
                                    csAssert.assertTrue(false,"Expected and Actual Billing Period Mismatch");
                                }

                                if(!actualInvoicingType.equalsIgnoreCase(expected_invoicing_type)){
                                    csAssert.assertTrue(false,"Expected and Actual Invoicing Type Mismatch");
                                }
                            }
                        }
                    } else {
                        if (jobStatus.equalsIgnoreCase("true")) {
                            csAssert.assertTrue(false, "Bulk Create Template Processed successfully for Flow [" + flowToTest +
                                    "] whereas it was expected to fail.");
                        } else {
                            logger.info("Bulk Create Template Processing failed for Flow [" + flowToTest + "]");
                        }
                    }

                    if (jobStatus.equalsIgnoreCase("true")) {
                        //Delete Newly Created Service Data
                        EntityOperationsHelper.deleteEntityRecord(serviceData,serviceDataId);
                    }
                } else {
                    csAssert.assertTrue(false, "Fetch API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
                }
            } else {
                throw new SkipException("Couldn't upload Bulk Create Template Successfully for Flow [" + flowToTest + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Service Data Bulk Create Template Processing for Flow [" + flowToTest + "]. " +
                    e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyAuditLogsForProcessingFlows(String flowToTest, int contractId, String templateFileName, CustomAssert csAssert) {
        try {
            logger.info("Validating Contract Audit Logs for Contract Id {} and Flow [{}]", contractId, flowToTest);
            logger.info("Hitting TabListData API for Contract Id {}, Audit Log Tab and Flow [{}]", contractId, flowToTest);
            TabListData tabListObj = new TabListData();
            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String tabListResponse = tabListObj.hitTabListData(auditLogTabId, contractEntityTypeId, contractId, payload);

            if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
                List<Map<Integer, Map<String, String>>> tabListData = ListDataHelper.getListData(tabListResponse);

                if (tabListData.isEmpty()) {
                    throw new SkipException("Couldn't get data from TabListData API Response for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");
                }

                //Validate Action Taken Column
                if (auditLogActionNameColumnId == null)
                    auditLogActionNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "action_name");
                if (auditLogActionNameColumnId == null)
                    throw new SkipException("Couldn't get Id for Column Action Taken for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String actualActionTaken = tabListData.get(0).get(Integer.parseInt(auditLogActionNameColumnId)).get("value");
                if (actualActionTaken == null || !actualActionTaken.trim().equalsIgnoreCase("Data Uploaded")) {
                    csAssert.assertTrue(false, "Expected Action Taken Value: Data Uploaded and Actual Action Taken Value: " + actualActionTaken);
                }

                //Validate Requested By Column
                if (auditLogRequestedByColumnId == null)
                    auditLogRequestedByColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "requested_by");
                if (auditLogRequestedByColumnId == null)
                    throw new SkipException("Couldn't get Id for Column Requested By for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String actualRequestedBy = tabListData.get(0).get(Integer.parseInt(auditLogRequestedByColumnId)).get("value");
                if (actualRequestedBy == null || !actualRequestedBy.trim().equalsIgnoreCase(auditLogExpectedRequestedBy.trim())) {
                    csAssert.assertTrue(false, "Expected Requested By Value: " + auditLogExpectedRequestedBy + " and Actual Requested By Value: " +
                            actualRequestedBy);
                }

                //Validate Completed By Column
                if (auditLogCompletedByColumnId == null)
                    auditLogCompletedByColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "completed_by");
                if (auditLogCompletedByColumnId == null)
                    throw new SkipException("Couldn't get Id for Column Completed By for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String actualCompletedBy = tabListData.get(0).get(Integer.parseInt(auditLogCompletedByColumnId)).get("value");
                if (actualCompletedBy == null || !actualCompletedBy.trim().equalsIgnoreCase(auditLogExpectedCompletedBy.trim())) {
                    csAssert.assertTrue(false, "Expected Completed By Value: " + auditLogExpectedCompletedBy + " and Actual Completed By Value: " +
                            actualCompletedBy);
                }

                //Validate Comment Column
                if (auditLogCommentColumnId == null)
                    auditLogCommentColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "comment");
                if (auditLogCommentColumnId == null)
                    throw new SkipException("Couldn't get Id for Column Comment for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String actualCommentValue = tabListData.get(0).get(Integer.parseInt(auditLogCommentColumnId)).get("value");
                if (actualCommentValue == null || !actualCommentValue.trim().equalsIgnoreCase("No")) {
                    csAssert.assertTrue(false, "Expected Comment Value: No and Actual Comment Value: " + actualCommentValue);
                }

                //Validate Document Column
                if (auditLogDocumentColumnId == null)
                    auditLogDocumentColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "document");
                if (auditLogDocumentColumnId == null)
                    throw new SkipException("Couldn't get Id for Column Document for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String actualDocumentValue = tabListData.get(0).get(Integer.parseInt(auditLogDocumentColumnId)).get("value");
                if (actualDocumentValue == null || !actualDocumentValue.trim().equalsIgnoreCase("No")) {
                    csAssert.assertTrue(false, "Expected Document Value: No and Actual Document Value: " + actualDocumentValue);
                }

                //Validate History Column
                if (auditLogHistoryColumnId == null)
                    auditLogHistoryColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "history");
                if (auditLogHistoryColumnId == null)
                    throw new SkipException("Couldn't get Id for Column History for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                String historyValue = tabListData.get(0).get(Integer.parseInt(auditLogHistoryColumnId)).get("value");
                Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

                if (historyId == -1)
                    throw new SkipException("Couldn't get History Id for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest + "]");

                logger.info("Hitting Field History API for Contract Id {} and Flow [{}]", contractId, flowToTest);
                FieldHistory historyObj = new FieldHistory();
                String historyResponse = historyObj.hitFieldHistory(historyId, contractEntityTypeId);

                if (ParseJsonResponse.validJsonResponse(historyResponse)) {
                    JSONObject jsonObj = new JSONObject(historyResponse);
                    JSONArray jsonArr = jsonObj.getJSONArray("value");

                    if (jsonArr.length() == 0) {
                        throw new SkipException("Couldn't get Data from Field History API Response for History Id " + historyId + " and Contract Id " + contractId);
                    }

                    //Validate History Action
                    String actionValue = jsonArr.getJSONObject(0).getString("state");
                    if (actionValue == null || !actionValue.trim().equalsIgnoreCase("ADDED")) {
                        csAssert.assertTrue(false, "Expected History Action Value: ADDED and Actual History Action Value: " + actionValue);
                    }

                    //Validate History Field Name
                    String fieldNameValue = jsonArr.getJSONObject(0).getString("property");
                    if (fieldNameValue == null || !fieldNameValue.trim().equalsIgnoreCase("Service Data Uploaded")) {
                        csAssert.assertTrue(false, "Expected History Field Name Value: Service Data Uploaded and Actual History Field Name Value: " +
                                fieldNameValue);
                    }

                    //Validate History New Value
                    String newValue = jsonArr.getJSONObject(0).getString("newValue");
                    if (newValue == null || !newValue.trim().equalsIgnoreCase(templateFileName.trim())) {
                        csAssert.assertTrue(false, "Expected History New Value: " + templateFileName.trim() + " and Actual History New Value: " +
                                newValue);
                    }
                } else {
                    csAssert.assertTrue(false, "Field History API Response for History Id " + historyId + " and Contract Id " + contractId +
                            " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "TabListData API Response for Contract Id " + contractId + ", Audit Log Tab and Flow [" + flowToTest +
                        "] is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Audit Logs for Contract Id " + contractId + " and Flow [" + flowToTest + "]. " +
                    e.getMessage());
        }
    }

    private void deleteServiceDataOfContract(String flowToTest, int contractId, CustomAssert csAssert) {
        try {
            logger.info("Hitting ListRendererListData API for Service Data");
            ListRendererListData listDataObj = new ListRendererListData();
            String payload = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
            Map<String, String> params = new HashMap<>();
            params.put("contractId", String.valueOf(contractId));

            listDataObj.hitListRendererListData(serviceDataListId, payload, params);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(listDataResponse);

                if (listData.size() > 0) {
                    int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

                    if (idColumnNo != -1) {
                        for (Map<Integer, Map<String, String>> oneListData : listData) {
                            int serviceDataId = Integer.parseInt(oneListData.get(idColumnNo).get("valueId"));
                            logger.info("Deleting Service Data Id {}", serviceDataId);
                            EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId);
                        }
                    } else {
                        throw new SkipException("Couldn't get Column No for Id and Flow [" + flowToTest + "]");
                    }
                } else {
                    throw new SkipException("Couldn't get List Data from ListDataAPI Response for Flow [" + flowToTest + "]");
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Service Data is an Invalid JSON.");
            }
        } catch (Exception e) {
            throw new SkipException("Exception while deleting Service Data(s) of Contract Id " + contractId + ". [" + e.getMessage() + "]");
        }
    }

    private int findContractIdFromExcelData(String find, List<ExcelData> list) {

        for (ExcelData excelData : list) {
            if (excelData.equals(new ExcelData(-1, find))) {
                return excelData.getContractId();
            }
        }
        return -1;
    }


    class ExcelData {

        private String filename;
        private int contractId;

        ExcelData(int contractId, String fileName) {
            this.contractId = contractId;
            this.filename = fileName;
        }

        public String getFilename() {
            return filename;
        }

        public int getContractId() {
            return contractId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExcelData excelData = (ExcelData) o;
            return filename.equals(excelData.getFilename());
        }

        @Override
        public int hashCode() {
            return Objects.hash(filename);
        }
    }
}
