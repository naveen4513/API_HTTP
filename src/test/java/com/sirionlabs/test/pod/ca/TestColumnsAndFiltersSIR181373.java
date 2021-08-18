package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.fieldLabel.CreateForm;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientAdmin.FieldLabelHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestColumnsAndFiltersSIR181373 extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestColumnsAndFiltersSIR181373.class);

    private static String endUserName = null;
    private static String endUserPassword = null;
    private FieldLabelHelper fieldLabelHelperObj = new FieldLabelHelper();
    private ReportsListHelper reportListHelperObj = new ReportsListHelper();
    private ReportsDefaultUserListMetadataHelper defaultUserListHelperObj = new ReportsDefaultUserListMetadataHelper();
    private FieldRenaming fieldRenamingObj = new FieldRenaming();
    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        endUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

        testCasesMap = getTestCasesMapping();
    }

    private Boolean loginWithEndUser(String userName, String userPassword) {
        logger.info("Logging with UserName [{}] and Password [{}]", userName, userPassword);
        Check checkObj = new Check();
        checkObj.hitCheck(userName, userPassword);

        return (Check.getAuthorization() != null);
    }

    @AfterClass
    public void afterClass() {
        //Login with Original User for Rest of the Automation Suite
        logger.info("Logging back with Original Environment Configuration.");

        if (!loginWithEndUser(endUserName, endUserPassword)) {
            logger.info("Couldn't Login back with Original UserName [{}] and Password [{}]. Hence aborting Automation Suite.", endUserName, endUserPassword);
            System.exit(0);
        }
    }

    //TC-C13442: To Verify State - Field and Filter in Listing & Excel Download under Contract Lead Time Report and CDR Lead Time Report
    @Test
    public void testC13442() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify State - Field and Filter in Listing & Excel Download under Contract Lead Time Report and CDR Lead Time Report.");

            String reportName = "Contracts - Lead Time";
            int reportId = 1000;

            String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "state", csAssert);

            String listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "state", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 61, reportId, "STATE", csAssert);

            reportName = "Contract Draft Request - Lead Time";
            reportId = 1011;

            defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "state", csAssert);

            listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "state", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 160, reportId, "STATE", csAssert);
        } catch (SkipException e) {
            addTestResultAsSkip(getTestCaseIdForMethodName("testC13442"), csAssert);
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Test C13442. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test C13442. " + e.getMessage());
        }

        addTestResult(getTestCaseIdForMethodName("testC13442"), csAssert);
        csAssert.assertAll();
    }

    private void verifyIfFieldIsPresentInDefaultUserListMetadataAPI(String defaultUserListResponse, String reportName, int reportId, String fieldName, CustomAssert csAssert) {
        logger.info("Verifying Field {} in DefaultUserListMetadata API Response for Report [{}] having Id {}", fieldName, reportName, reportId);

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            Boolean isStateFieldPresent = defaultUserListHelperObj.isFieldPresentInDefaultUserListMetadataAPIResponse(defaultUserListResponse, fieldName);

            if (isStateFieldPresent == null) {
                logger.warn("Couldn't Check if {} Field is Present or not in DefaultUserListMetadata API Response for Report [{}] having Id {}. Hence skipping test.",
                        fieldName, reportName, reportId);
                throw new SkipException("Couldn't Check if " + fieldName + " Field is Present or not in DefaultUserListMetadata API Response for Report [" + reportName +
                        "] having Id " + reportId + ". Hence skipping test.");
            }

            csAssert.assertTrue(isStateFieldPresent, fieldName + " Field is not Present in DefaultUserListMetadata API Response for Report [" + reportName +
                    "] having Id " + reportId);
        } else {
            csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Report [" + reportName + "] having Id " + reportId +
                    " is an Invalid JSON.");
        }
    }

    private void verifyIfFieldIsPresentInListDataAPI(String listDataAPIResponse, String reportName, int reportId, String fieldName, CustomAssert csAssert) {
        logger.info("Verifying Field {} in ListData API Response for Report [{}] having Id {}", fieldName, reportName, reportId);

        if (ParseJsonResponse.validJsonResponse(listDataAPIResponse)) {
            if (ListDataHelper.getFilteredListDataCount(listDataAPIResponse) != 0) {
                Boolean isStatePresent = reportListHelperObj.isColumnPresentInListDataAPIResponse(listDataAPIResponse, fieldName);

                if (isStatePresent == null) {
                    logger.warn("Couldn't Check if {} Column is Present or not in ListData API Response for Report [{}] having Id {}. Hence skipping test.",
                            fieldName, reportName, reportId);
                    throw new SkipException("Couldn't Check if " + fieldName + " Column is Present or not in ListData API Response for Report [" + reportName + "] having Id " +
                            reportId + ". Hence skipping test.");
                }

                csAssert.assertTrue(isStatePresent, fieldName + " Field is not Present in ListData API Response for Report [" + reportName + "] having Id " + reportId);
            } else {
                logger.warn("Couldn't get Records in ListData API for Report [{}] having Id {}", reportName, reportId);
            }
        } else {
            csAssert.assertTrue(false, "ListData API Response for Report [" + reportName + "] having Id " + reportId + " is an Invalid JSON.");
        }
    }

    private void verifyIfFieldIsPresentInListDownloadExcel(String reportName, int entityTypeId, int reportId, String fieldName, CustomAssert csAssert) {
        Boolean fileDownloaded = reportListHelperObj.downloadGraphWithDataAllColumnsWithFilterJson("src/test", reportName + ".xlsx",
                reportName, entityTypeId, reportId, 3);

        if (fileDownloaded) {
            Boolean isStateFieldPresent = reportListHelperObj.isFieldPresentInReportListDataDownloadExcel("src/test", reportName + ".xlsx",
                    fieldName);

            if (isStateFieldPresent == null) {
                logger.warn("Couldn't Check if Column {} is Present or not in ListData Download Excel for Report [{}] having Id {} and located at [{}]. " +
                        "Hence skipping test", fieldName, reportName, reportId, "src/test/" + reportName + ".xlsx");
                throw new SkipException("Couldn't Check if Column " + fieldName + " is Present or not in ListData Download Excel for Report [" + reportName + "] having Id " +
                        reportId + " and located at [src/test/" + reportName + ".xlsx]. Hence skipping test.");
            }

            csAssert.assertTrue(isStateFieldPresent, fieldName + " Field is not Present in ListData Download Excel for Report [" + reportName + "] having Id " +
                    reportId + " and located at [src/test/" + reportName + ".xlsx].");

            FileUtils.deleteFile("src/test/" + reportName + ".xlsx");
        } else {
            logger.warn("Couldn't Download Report with Data for Report [{}] having Id {}. Hence skipping test.", reportName, reportId);
            throw new SkipException("Couldn't Download Report with Data for Report [" + reportName + "] having Id " + reportId + ". Hence skipping test.");
        }
    }

    //TC-C13443: To Verify Source Name/Title Field in Listing & Excel Download under Contract Lead Time Report
    @Test
    public void testC13443() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify Source Name/Title Field in Listing & Excel Download under Contract Lead Time Report.");

            String reportName = "Contracts - Lead Time";
            int reportId = 1000;

            String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "sourcename", csAssert);

            String listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "sourcename", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 61, reportId, "SOURCE NAME/TITLE", csAssert);
        } catch (SkipException e) {
            addTestResultAsSkip(getTestCaseIdForMethodName("testC13443"), csAssert);
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Test C13443. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test C13443. " + e.getMessage());
        }

        addTestResult(getTestCaseIdForMethodName("testC13443"), csAssert);
        csAssert.assertAll();
    }

    //TC-C13444: To Verify Supplier Field in CDR Tracker Report
    @Test
    public void testC13444() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify Supplier Field and Filter in Listing & Excel Download under CDR Tracker Report.");

            String reportName = "Contract Draft Request - Tracker";
            int reportId = 324;

            String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "supplier", csAssert);

            String listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "supplier", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 160, reportId, "SUPPLIERS", csAssert);
        } catch (SkipException e) {
            addTestResultAsSkip(getTestCaseIdForMethodName("testC13444"), csAssert);
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Test C13444. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test C13444. " + e.getMessage());
        }

        addTestResult(getTestCaseIdForMethodName("testC13444"), csAssert);
        csAssert.assertAll();
    }

    //TC-C13446: To Verify Source Id - Listing and Excel download should be added in Contract Listing, Contract Tracker Report and Contract Lead Time Report.
    @Test
    public void testC13446() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify Source Id Field in Listing & Excel Download under Contract Tracker Report and Contract Lead Time Report.");

            String reportName = "Contracts - Lead Time";
            int reportId = 1000;

            String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "sourceid", csAssert);

            String listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "sourceid", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 61, reportId, "SOURCE ID", csAssert);

            reportName = "Contract - Pipeline Report";
            reportId = 222;

            defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "sourceid", csAssert);

            listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "sourceid", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 61, reportId, "SOURCE ID", csAssert);
        } catch (SkipException e) {
            addTestResultAsSkip(getTestCaseIdForMethodName("testC13446"), csAssert);
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Test C13446. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test C13446. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC-C13452: To Verify Parent Document ID Field in Listing & Excel Download under Contract Tracker Report
    @Test
    public void testC13452() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify Parent Document ID Field in Listing & Excel Download under Contract Tracker Report.");

            String reportName = "Contracts - Tracker";
            int reportId = 222;

            String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            verifyIfFieldIsPresentInDefaultUserListMetadataAPI(defaultUserListResponse, reportName, reportId, "parentdocumentid", csAssert);

            String listDataResponse = reportListHelperObj.hitListDataAPIForReportId(reportId);
            verifyIfFieldIsPresentInListDataAPI(listDataResponse, reportName, reportId, "parentdocumentid", csAssert);
            verifyIfFieldIsPresentInListDownloadExcel(reportName, 61, reportId, "PARENT DOCUMENT ID", csAssert);
        } catch (SkipException e) {
            addTestResultAsSkip(getTestCaseIdForMethodName("testC13452"), csAssert);
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating Test C13452. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Test C13452. " + e.getMessage());
        }

        addTestResult(getTestCaseIdForMethodName("testC13452"), csAssert);
        csAssert.assertAll();
    }

    //TC-C13456: To Verify renaming of Fields in Listing, Filters and Reports for Contract and CDR.
    @Test
    public void testC13456() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: To Verify Renaming of Fields in Listing, Filters and Reports for Contract and CDR.");

            if (!adminHelperObj.loginWithClientAdminUser()) {
                logger.error("Couldn't login with Client Admin User. Hence skipping test");
                throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
            }

            logger.info("Hitting FieldLabel CreateForm API.");
            CreateForm createFormObj = new CreateForm();
            String createFormResponse = createFormObj.hitFieldLabelCreateForm();

            Map<String, String> contractFieldsMap = new HashMap<>();
            contractFieldsMap.put("State", "State2");
            contractFieldsMap.put("Source Id", "Source Id2");
            contractFieldsMap.put("ID", "ID2");
            contractFieldsMap.put("Parent Document Id", "Parent Document Id2");

            testC13456ForContracts(createFormResponse, contractFieldsMap, csAssert);

            Map<String, String> cdrFieldsMap = new HashMap<>();
            cdrFieldsMap.put("State", "State2");
            cdrFieldsMap.put("Suppliers", "Suppliers2");

            testC13456ForCDR(createFormResponse, cdrFieldsMap, csAssert);

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test C13456. " + e.getMessage());
        } finally {
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    private void testC13456ForContracts(String createFormResponse, Map<String, String> contractFieldsMap, CustomAssert CustomAssert) {
        logger.info("Validating Test C13456 for Contracts Entity.");
        logger.info("Getting GroupId Value for Contract");
        Integer groupIdValue = fieldLabelHelperObj.getFieldLabelGroupValueFromCreateFormAPI(createFormResponse, "Entity", "Contract");

        if (groupIdValue == null) {
            logger.error("Couldn't get GroupIdValue for Group [Contract] from CreateForm API Response. Hence skipping test.");
            throw new SkipException("Couldn't get GroupIdValue for Group [Contract] from CreateForm API Response. Hence skipping test.");
        }

        logger.info("Hitting FieldRenaming API for Language Id 1 and Group Id {}", groupIdValue);
        String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1, groupIdValue);

        if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
            String payloadForFieldRenamingUpdate = getPayloadForFieldRenamingUpdate(fieldRenamingResponse, contractFieldsMap, "Contracts");

            if (payloadForFieldRenamingUpdate == null) {
                logger.warn("Couldn't get Payload for Field Renaming Update API for Contracts. Hence skipping test.");
                throw new SkipException("Couldn't get Payload for Field Renaming Update API for Contracts. Hence skipping test.");
            }

            try {
                logger.info("Hitting Field Renaming Update API.");
                String fieldRenamingUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

                if (ParseJsonResponse.validJsonResponse(fieldRenamingUpdateResponse)) {
                    JSONObject updateJsonObj = new JSONObject(fieldRenamingUpdateResponse);

                    if (updateJsonObj.getBoolean("isSuccess")) {
                        if (!loginWithEndUser(endUserName, endUserPassword)) {
                            logger.error("Couldn't login with End User [{}] and Password [{}]. Hence skipping test", endUserName, endUserPassword);
                            throw new SkipException("Couldn't login with End User [" + endUserName + "] and Password [" + endUserPassword + "]. Hence skipping test.");
                        }

                        verifyFieldLabelsInEntityListing("Contracts", contractFieldsMap, CustomAssert);

                        verifyFieldLabelsInEntityReport("Contracts", "Contracts - Lead Time", contractFieldsMap, CustomAssert);

                    } else {
                        logger.error("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
                        throw new SkipException("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
                    }
                } else {
                    CustomAssert.assertTrue(false, "Field Renaming Update API Response is an Invalid JSON.");
                }

                revertFieldLabels(fieldRenamingResponse, "Contracts");
            } catch (SkipException e) {
                revertFieldLabels(fieldRenamingResponse, "Contracts");
                throw new SkipException(e.getMessage());
            }
        } else {
            CustomAssert.assertTrue(false, "Field Renaming API Response for Language Id 1 and Group Id " + groupIdValue +
                    " is an Invalid JSON.");
            FileUtils.saveResponseInFile("FieldRenamingAPI for Language Id 1 and GroupId " + groupIdValue + ".txt", fieldRenamingResponse);
        }
    }

    private void testC13456ForCDR(String createFormResponse, Map<String, String> cdrFieldsMap, CustomAssert CustomAssert) {
        logger.info("Validating Test C13456 for CDR Entity.");
        logger.info("Getting GroupId Value for CDR");
        Integer groupIdValue = fieldLabelHelperObj.getFieldLabelGroupValueFromCreateFormAPI(createFormResponse, "Entity", "Contract Draft Request");

        if (groupIdValue == null) {
            logger.error("Couldn't get GroupIdValue for Group [Contract Draft Request] from CreateForm API Response. Hence skipping test.");
            throw new SkipException("Couldn't get GroupIdValue for Group [Contract Draft Request] from CreateForm API Response. Hence skipping test.");
        }

        adminHelperObj.loginWithClientAdminUser();

        logger.info("Hitting FieldRenaming API for Language Id 1 and Group Id {}", groupIdValue);
        String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1, groupIdValue);

        if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
            String payloadForFieldRenamingUpdate = getPayloadForFieldRenamingUpdate(fieldRenamingResponse, cdrFieldsMap, "Contract Draft Request");

            if (payloadForFieldRenamingUpdate == null) {
                logger.warn("Couldn't get Payload for Field Renaming Update API for Contract Draft Request. Hence skipping test.");
                throw new SkipException("Couldn't get Payload for Field Renaming Update API for Contract Draft Request. Hence skipping test.");
            }

            try {
                logger.info("Hitting Field Renaming Update API.");
                String fieldRenamingUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

                if (ParseJsonResponse.validJsonResponse(fieldRenamingUpdateResponse)) {
                    JSONObject updateJsonObj = new JSONObject(fieldRenamingUpdateResponse);

                    if (updateJsonObj.getBoolean("isSuccess")) {
                        if (!loginWithEndUser(endUserName, endUserPassword)) {
                            logger.error("Couldn't login with End User [{}] and Password [{}]. Hence skipping test", endUserName, endUserPassword);
                            throw new SkipException("Couldn't login with End User [" + endUserName + "] and Password [" + endUserPassword + "]. Hence skipping test.");
                        }

                        verifyFieldLabelsInEntityListing("Contract Draft Request", cdrFieldsMap, CustomAssert);

                        verifyFieldLabelsInEntityReport("Contract Draft Request", "Contract Requests - Lead Time", cdrFieldsMap, CustomAssert);

                        verifyFieldLabelsInEntityReport("Contract Draft Request", "Contract Requests - Tracker", cdrFieldsMap, CustomAssert);

                    } else {
                        logger.error("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
                        throw new SkipException("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
                    }
                } else {
                    CustomAssert.assertTrue(false, "Field Renaming Update API Response is an Invalid JSON.");
                }

                revertFieldLabels(fieldRenamingResponse, "Contract Draft Request");
            } catch (SkipException e) {
                revertFieldLabels(fieldRenamingResponse, "Contract Draft Request");
                throw new SkipException(e.getMessage());
            }
        } else {
            CustomAssert.assertTrue(false, "Field Renaming API Response for Language Id 1 and Group Id " + groupIdValue +
                    " is an Invalid JSON.");
            FileUtils.saveResponseInFile("FieldRenamingAPI for Language Id 1 and GroupId " + groupIdValue + ".txt", fieldRenamingResponse);
        }
    }

    private void verifyFieldLabelsInEntityListing(String entityName, Map<String, String> entityFieldsMap, CustomAssert CustomAssert) {
        try {
            logger.info("Verifying Field Labels in {} Listing.", entityName);
            logger.info("Hitting DefaultUserListMetaData API for {} Listing", entityName);
            ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
            int entityListId = ConfigureConstantFields.getListIdForEntity(entityName);
            defaultUserListObj.hitListRendererDefaultUserListMetadata(entityListId);
            String defaultUserListResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
                JSONObject defaultUserListJsonObj = new JSONObject(defaultUserListResponse);
                JSONArray metadataJsonArr = defaultUserListJsonObj.getJSONArray("filterMetadatas");

                for (int k = 0; k < metadataJsonArr.length(); k++) {
                    String defaultName = metadataJsonArr.getJSONObject(k).getString("defaultName").trim();

                    for (Map.Entry<String, String> fieldsMap : entityFieldsMap.entrySet()) {
                        if (fieldsMap.getKey().trim().toLowerCase().equalsIgnoreCase(defaultName)) {
                            String expectedValue = fieldsMap.getValue().trim();
                            String actualValue = metadataJsonArr.getJSONObject(k).getString("name").trim();

                            if (!expectedValue.equalsIgnoreCase(actualValue)) {
                                logger.error("Expected Field Label: [{}] and Actual Field Label: [{}] in {} DefaultUserListMetadata API for Listing",
                                        expectedValue, actualValue, entityName);
                                CustomAssert.assertTrue(false, "Expected Field Label: [" + expectedValue + "] and Actual Field Label: [" +
                                        actualValue + "] in " + entityName + " DefaultUserListMetadata API for Listing.");
                            }
                            break;
                        }
                    }
                }
            } else {
                CustomAssert.assertTrue(false, "DefaultUserListMetaData API Response for " + entityName + " Listing is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Field Labels in Listing for Entity {}. {}", entityName, e.getStackTrace());
            CustomAssert.assertTrue(false, "Exception while Verifying Field Labels in Listing for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private void verifyFieldLabelsInEntityReport(String entityName, String reportName, Map<String, String> entityFieldsMap, CustomAssert CustomAssert) {
        try {
            logger.info("Verifying Field Labels in {} Report for Entity {}", reportName, entityName);
            Integer reportId = reportListHelperObj.getReportId(ConfigureConstantFields.getEntityIdByName(entityName), reportName);

            if (reportId != null) {
                logger.info("Hitting DefaultUserListMetadata API for Report {}", reportName);
                ReportRendererDefaultUserListMetaData reportDefaultObj = new ReportRendererDefaultUserListMetaData();
                reportDefaultObj.hitReportRendererDefaultUserListMetadata(reportId);
                String reportDefaultUserListResponse = reportDefaultObj.getReportRendererDefaultUserListMetaDataJsonStr();

                if (ParseJsonResponse.validJsonResponse(reportDefaultUserListResponse)) {
                    JSONObject defaultUserListJsonObj = new JSONObject(reportDefaultUserListResponse);
                    JSONArray metadataJsonArr = defaultUserListJsonObj.getJSONArray("filterMetadatas");

                    for (int k = 0; k < metadataJsonArr.length(); k++) {
                        String defaultName = metadataJsonArr.getJSONObject(k).getString("defaultName").trim();

                        for (Map.Entry<String, String> fieldsMap : entityFieldsMap.entrySet()) {
                            if (fieldsMap.getKey().trim().toLowerCase().equalsIgnoreCase(defaultName)) {
                                String expectedValue = fieldsMap.getValue().trim();
                                String actualValue = metadataJsonArr.getJSONObject(k).getString("name").trim();

                                if (!expectedValue.equalsIgnoreCase(actualValue)) {
                                    logger.error("Expected Field Label: [{}] and Actual Field Label: [{}] in DefaultUserListMetadata API for Report {}",
                                            expectedValue, actualValue, reportName);
                                    CustomAssert.assertTrue(false, "Expected Field Label: [" + expectedValue +
                                            "] and Actual Field Label: [" + actualValue + "] in DefaultUserListMetadata API for Report " + reportName);
                                }
                                break;
                            }
                        }
                    }
                } else {
                    CustomAssert.assertTrue(false,
                            "DefaultUserListMetadata API Response for Report " + reportName + " is an Invalid JSON.");
                }
            } else {
                logger.warn("Couldn't get Id for Report {}. Hence skipping test.", reportName);
                throw new SkipException("Couldn't get Id for Report " + reportName + ". Hence skipping test.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Field Labels in Report {}. {}", reportName, e.getStackTrace());
            CustomAssert.assertTrue(false, "Exception while Verifying Field Labels in Report " + reportName + ". " + e.getMessage());
        }
    }

    private String getPayloadForFieldRenamingUpdate(String fieldRenamingResponse, Map<String, String> entityFieldsMap, String entityName) {
        try {
            String payloadForFieldRenamingUpdate = fieldRenamingResponse;
            JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("childGroups");

            String originalFieldLabelValue;

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getString("name").trim().equalsIgnoreCase("Metadata")) {
                    JSONArray fieldLabelsArr = jsonArr.getJSONObject(i).getJSONArray("fieldLabels");

                    for (int j = 0; j < fieldLabelsArr.length(); j++) {
                        String metadataFieldName = fieldLabelsArr.getJSONObject(j).getString("name").trim();

                        if (entityFieldsMap.containsKey(metadataFieldName)) {
                            originalFieldLabelValue = fieldLabelsArr.getJSONObject(j).getString("clientFieldName").trim();
                            payloadForFieldRenamingUpdate = payloadForFieldRenamingUpdate.replace("\"clientFieldName\":\"" + originalFieldLabelValue + "\"",
                                    "\"clientFieldName\":\"" + entityFieldsMap.get(metadataFieldName) + "\"");
                        }
                    }
                    break;
                }
            }

            return payloadForFieldRenamingUpdate;
        } catch (Exception e) {
            logger.error("Exception while Getting Payload for Field Renaming Update API for Entity {}. {}", entityName, e.getStackTrace());
        }

        return null;
    }

    private void revertFieldLabels(String fieldRenamingResponse, String entityName) {
        logger.info("Reverting Field Labels to Original Values for Entity {}", entityName);

        if (!adminHelperObj.loginWithClientAdminUser()) {
            logger.error("Couldn't login with Client Admin User. Hence skipping test");
            throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
        }

        fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);

        logger.info("Logging Back with End User");
        loginWithEndUser(endUserName, endUserPassword);
    }
}