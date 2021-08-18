package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.clientSetup.reportRenderer.ReportRendererListJson;
import com.sirionlabs.api.commonAPI.DeleteLink;
import com.sirionlabs.api.commonAPI.LinkEntity;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCDRTrackerReport {

    private final static Logger logger = LoggerFactory.getLogger(TestCDRTrackerReport.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/CDRTrackerReport";
    private String configFileName = "TestCDRTrackerReport.cfg";

    private String downloadReportPath = "src/test";
    private String downloadReportName = "CDR Tracker Report.xlsx";

    private LinkEntity linkEntityObj = new LinkEntity();
    private DownloadGraphWithData downloadObj = new DownloadGraphWithData();
    private AdminHelper adminHelperObj = new AdminHelper();
    private ReportsDefaultUserListMetadataHelper metadataObj = new ReportsDefaultUserListMetadataHelper();
    private FieldRenaming fieldRenamingObj = new FieldRenaming();
    private UpdateAccount updateAccountObj = new UpdateAccount();

    @AfterClass
    public void afterClass() {
        if (FileUtils.fileExists(downloadReportPath, downloadReportName)) {
            FileUtils.deleteFile(downloadReportPath, downloadReportName);
        }
    }

    private int createCDR() {
        return createCDR("cdr creation");
    }

    private int createCDR(String sectionName) {
        String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, false);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    private int createContract() {
        String sectionName = "contract creation";
        String createResponse = Contract.createContract(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, true);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    /*
    TC-C89058: Verify that "Related contract" column is present in the downloaded excel data and is displayed in the desired format.
    TC-C89063: Verify that only the linked and related entities are displayed
     */
    @Test
    public void testC89058() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();
        int contractId = createContract();

        try {
            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence couldn't validate further.");
            }

            //Link CDR and Contract
            String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + contractId + ",\"entityTypeId\":61}]}";
            String linkResponse = linkEntityObj.hitLinkEntity(linkEntitiesPayload);

            if (new JSONObject(linkResponse).isNull("error")) {
                //Download report with Selected Columns
                logger.info("Downloading CDR Tracker Report with Selected Columns");
                boolean downloadReport = downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

                String cdrShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
                String contractShowResponse = ShowHelper.getShowResponseVersion2(61, contractId);

                if (downloadReport) {
                    //Validate data in CDR Tracker Report
                    validateRelatedContractsDataInReport(cdrShowResponse, 61, contractShowResponse, csAssert, "[C89058. Selected Columns.]");
                    //Validate C89090 with Selected Columns
                    validateC89090(csAssert, "Selected Columns");
                } else {
                    csAssert.assertFalse(true, "C89058. CDR Tracker Report Download failed with Selected Columns");
                }

                //Download Report with All Columns
                downloadReport = downloadCDRTrackerReport(getAllColumnsFilterJson());

                if (downloadReport) {
                    //Validate data in CDR Tracker Report
                    validateRelatedContractsDataInReport(cdrShowResponse, 61, contractShowResponse, csAssert, "[C89058. All Columns.]");
                    //Validate C89090 with All Columns
                    validateC89090(csAssert, "All Columns");
                } else {
                    csAssert.assertFalse(true, "C89058. CDR Tracker Report Download failed with All Columns");
                }

                //Validate C89128
                validateC89128(cdrId, contractId, csAssert);
            } else {
                csAssert.assertFalse(true, "Link Entity failed.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89058. " + e.getMessage());
        } finally {
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
        }

        csAssert.assertAll();
    }

    private boolean downloadCDRTrackerReport(String filterJson) {
        Map<String, String> params = new HashMap<>();
        params.put("jsonData", filterJson);
        HttpResponse response = downloadObj.hitDownloadGraphWithData(params, 324);

        FileUtils fileUtil = new FileUtils();
        return fileUtil.writeResponseIntoFile(response, downloadReportPath + "/" + downloadReportName);
    }

    private void validateRelatedContractsDataInReport(String recordOneShowResponse, int recordTwoEntityTypeId, String recordTwoShowResponse,
                                                      CustomAssert csAssert, String additionalInfo) {
        try {
            List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);
            List<String> allHeadersInLowerCase = new ArrayList<>();

            for (String header : allHeaders) {
                allHeadersInLowerCase.add(header.toLowerCase());
            }

            if (allHeadersInLowerCase.contains("related contract")) {
                List<String> data = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 5);

                //Validate CDR Id
                String actualValue = data.get(allHeadersInLowerCase.indexOf("id"));
                String expectedValue = ShowHelper.getValueOfField(160, "short code id", recordOneShowResponse);
                csAssert.assertEquals(actualValue, expectedValue, "CDR Id Column Validation failed in Downloaded CDR Tracker Report. Expected Id Value: " +
                        expectedValue + " and Actual Value: " + actualValue + ". " + additionalInfo);

                //Validate CDR Title
                actualValue = data.get(allHeadersInLowerCase.indexOf("title"));
                expectedValue = ShowHelper.getValueOfField(160, "title", recordOneShowResponse);
                csAssert.assertEquals(actualValue, expectedValue, "CDR Title Column Validation failed in Downloaded CDR Tracker Report. Expected Title Value: " +
                        expectedValue + " and Actual Value: " + actualValue + ". " + additionalInfo);

                //Validate Related Contract
                actualValue = data.get(allHeadersInLowerCase.indexOf("related contract"));
                expectedValue = ShowHelper.getValueOfField(recordTwoEntityTypeId, "short code id", recordTwoShowResponse) + ":" +
                        ShowHelper.getValueOfField(recordTwoEntityTypeId, "title", recordTwoShowResponse);
                csAssert.assertEquals(actualValue, expectedValue, "Related Contract Column Validation failed in Downloaded CDR Tracker Report. Expected Value: " +
                        expectedValue + " and Actual Value: " + actualValue + ". " + additionalInfo);

            } else {
                csAssert.assertFalse(true, "Downloaded Report doesn't contain Related Contract Column.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Data in CDR Tracker Report. " + additionalInfo + ". " + e.getMessage());
        }
    }

    private String getRelatedContractsSelectedColumnsFilterJson() {
        return "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{\"344\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1040\",\"name\":\"Richline Group Inc\"}]},\"filterId\":344," +
                "\"filterName\":\"customer\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                "\"selectedColumns\":[{\"columnId\":12664,\"columnQueryName\":\"id\"},{\"columnId\":18817,\"columnQueryName\":\"related_contracts\"}," +
                "{\"columnId\":12666,\"columnQueryName\":\"title\"}]}";
    }

    private String getAllColumnsFilterJson() {
        return "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{\"344\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1040\",\"name\":\"Richline Group Inc\"}]},\"filterId\":344," +
                "\"filterName\":\"customer\"}}}}";
    }

    /*
    TC-C89090: Verify that same data is not duplicated across all the rows
     */
    private void validateC89090(CustomAssert csAssert, String additionalInfo) {
        try {
            logger.info("Starting Test TC-C89090: Verify that same data is not duplicated across all the rows.");
            List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);
            int index = -1;

            for (int i = 0; i < allHeaders.size(); i++) {
                if (allHeaders.get(i).trim().toLowerCase().equalsIgnoreCase("id")) {
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                throw new Exception("Couldn't locate Id Column in Downloaded CDR Tracker Report. " + additionalInfo);
            }

            List<String> data = XLSUtils.getOneColumnDataFromMultipleRows(downloadReportPath, downloadReportName, "Data", index,
                    4, XLSUtils.getNoOfRows(downloadReportPath, downloadReportName, "Data").intValue() - 6);

            int count = 0;
            String cdrIdValue = data.get(0);

            for (String value : data) {
                if (value.equalsIgnoreCase(cdrIdValue)) {
                    count++;
                }
            }

            if (count > 1) {
                csAssert.assertFalse(true, "CDR having Id " + cdrIdValue + " is coming more than once in Downloaded CDR Tracker Report. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89090. " + additionalInfo + ". " + e.getMessage());
        }
    }

    /*
    TC-C89128: Verify if a linked contract is de-linked and another contract is linked, then the results should be updated in the CDR Tracker Report.
     */
    private void validateC89128(int cdrId, int oldContractId, CustomAssert csAssert) {
        int newContractId = createContract();

        try {
            logger.info("Starting Test TC-C89128: Verify if a linked contract is de-linked and another contract is linked, " +
                    "then the results should be updated in the CDR Tracker Report.");

            //Remove Link
            String payload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\":" + oldContractId + ",\"entityTypeId\":61}]}";
            String deleteLinkResponse = DeleteLink.getDeleteLinkResponse(payload);

            if (new JSONObject(deleteLinkResponse).isNull("error")) {
                //Link CDR with New Contract
                String linkEntitiesPayload = "{\"entityId\":" + cdrId + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + newContractId + ",\"entityTypeId\":61}]}";
                linkEntityObj.hitLinkEntity(linkEntitiesPayload);

                //Download report with Selected Columns
                logger.info("Downloading CDR Tracker Report with Selected Columns");
                boolean downloadReport = downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

                String cdrShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
                String contractShowResponse = ShowHelper.getShowResponseVersion2(61, newContractId);

                if (downloadReport) {
                    //Validate data in CDR Tracker Report
                    validateRelatedContractsDataInReport(cdrShowResponse, 61, contractShowResponse, csAssert, "[C89128. Selected Columns.]");
                } else {
                    csAssert.assertFalse(true, "C89128. CDR Tracker Report Download failed with Selected Columns");
                }

                //Download Report with All Columns
                downloadReport = downloadCDRTrackerReport(getAllColumnsFilterJson());

                if (downloadReport) {
                    //Validate data in CDR Tracker Report
                    validateRelatedContractsDataInReport(cdrShowResponse, 61, contractShowResponse, csAssert, "[C89128. All Columns.]");
                } else {
                    csAssert.assertFalse(true, "C89128. CDR Tracker Report Download failed with All Columns");
                }
            } else {
                csAssert.assertFalse(true, "Delete Link API failed.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89128. " + e.getMessage());
        } finally {
            EntityOperationsHelper.deleteEntityRecord("contracts", newContractId);
        }
    }

    /*
    TC-C89057: Verify if a linked CDR is deleted then it should not be displayed in the related contracts column.
     */
    @Test(priority = 1)
    public void testC89057() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId1 = createCDR();
        int cdrId2 = createCDR("cdr2 creation");

        try {
            logger.info("Starting Test TC-C89057: Verify if a linked CDR is deleted then it should not be displayed in the related contracts column.");

            //Link CDR with CDR.
            String linkEntitiesPayload = "{\"entityId\":" + cdrId1 + ",\"entityTypeId\":160,\"linkEntities\":[{\"entityId\": " + cdrId2 + ",\"entityTypeId\":160}]}";
            linkEntityObj.hitLinkEntity(linkEntitiesPayload);

            logger.info("Downloading CDR Tracker Report with Selected Columns");
            boolean downloadReport = downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

            String cdrOneShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId1);
            String cdrTwoShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId2);

            if (downloadReport) {
                //Validate data in CDR Tracker Report
                validateRelatedContractsDataInReport(cdrOneShowResponse, 160, cdrTwoShowResponse, csAssert, "[C89057. Selected Columns.]");

                //Delete Linked CDR
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId2);

                downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

                //Validate data in CDR Tracker Report
                List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);
                List<String> allHeadersInLowerCase = new ArrayList<>();

                for (String header : allHeaders) {
                    allHeadersInLowerCase.add(header.toLowerCase());
                }

                if (allHeadersInLowerCase.contains("related contract")) {
                    List<String> data = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 5);

                    //Validate CDR Id
                    String actualValue = data.get(allHeadersInLowerCase.indexOf("id"));
                    String expectedValue = ShowHelper.getValueOfField(160, "short code id", cdrOneShowResponse);
                    csAssert.assertEquals(actualValue, expectedValue, "CDR Id Column Validation failed in Downloaded CDR Tracker Report. Expected Id Value: " +
                            expectedValue + " and Actual Value: " + actualValue);

                    //Validate CDR Title
                    actualValue = data.get(allHeadersInLowerCase.indexOf("title"));
                    expectedValue = ShowHelper.getValueOfField(160, "title", cdrOneShowResponse);
                    csAssert.assertEquals(actualValue, expectedValue, "CDR Title Column Validation failed in Downloaded CDR Tracker Report. Expected Title Value: " +
                            expectedValue + " and Actual Value: " + actualValue);

                    //Validate Related Contract
                    actualValue = data.get(allHeadersInLowerCase.indexOf("related contract"));
                    csAssert.assertEquals(actualValue, "-", "Related Contract Column Validation failed in Downloaded CDR Tracker Report. Expected Value: " +
                            expectedValue + " and Actual Value: " + actualValue);

                } else {
                    csAssert.assertFalse(true, "Downloaded Report doesn't contain Related Contract Column.");
                }
            } else {
                csAssert.assertFalse(true, "C89057. CDR Tracker Report Download failed with Selected Columns");
            }

            //Validate C89083
            validateC89083(csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89057. " + e.getMessage());
        } finally {
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId1);
        }

        csAssert.assertAll();
    }

    /*
    TC-C89083: Verify internationalization support for Related Contract Field in CDR Tracker Report.
     */
    private void validateC89083(CustomAssert csAssert) {
        try {
            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1000);

            adminHelperObj.loginWithClientAdminUser();
            String fieldLabelResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, 850);
            adminHelperObj.loginWithEndUser();

            String expectedValue = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, "Report : Contract Draft Request- Tracker",
                    "Related Contract");

            //Validate Related Contract Value in DefaultUserListMetadata API for Report
            String defaultUserListResponse = metadataObj.hitDefaultUserListMetadataAPIForReportId(324);
            String actualValue = metadataObj.getColumnPropertyValueFromQueryName(defaultUserListResponse, "related_contracts", "name");

            boolean matchLabels = StringUtils.matchRussianCharacters(expectedValue, actualValue);
            csAssert.assertTrue(matchLabels, "Internationalization Validation failed in Report DefaultUserListMetadata API Response. Expected Value: " +
                    expectedValue + " and Actual Value: " + actualValue);

            //Validate Related Contract Value in Downloaded Report.
            boolean downloadReport = downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

            if (downloadReport) {
                //Validate data in CDR Tracker Report
                List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);

                boolean result = false;
                for (String header : allHeaders) {
                    if (StringUtils.matchRussianCharacters(expectedValue.toLowerCase(), header.toLowerCase())) {
                        result = true;
                        break;
                    }
                }

                csAssert.assertTrue(result, "C89083. Downloaded Report doesn't contain Column [" + expectedValue + "].");
            } else {
                csAssert.assertFalse(true, "C89083. CDR Tracker Report Download failed with Selected Columns");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89083. " + e.getMessage());
        } finally {
            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1);
        }
    }

    /*
    TC-C89127: Verify that "Related Contract" column is added at client admin inside reports section
     */
    @Test(priority = 2)
    public void testC89127() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();
        String reportConfigureListResponse = ReportRendererListConfigure.getReportListConfigureResponse(324);

        try {
            logger.info("Starting Test TC-C89127: Verify that Related Contract column is added at client admin inside reports section");
            //Disable Related Contract Column for Excel.
            JSONObject jsonObj = new JSONObject(reportConfigureListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("ecxelColumns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.equalsIgnoreCase("related_contracts")) {
                    jsonArr.getJSONObject(i).put("deleted", true);
                    break;
                }
            }

            jsonObj.put("ecxelColumns", new JSONArray(jsonArr.toString()));
            String payload = jsonObj.toString();
            ReportRendererListConfigure.updateReportListConfigureResponse(324, payload);

            downloadCDRTrackerReport(getRelatedContractsSelectedColumnsFilterJson());

            //Validate data in CDR Tracker Report
            List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);

            for (String header : allHeaders) {
                if (header.toLowerCase().equalsIgnoreCase("Related Contract")) {
                    csAssert.assertFalse(true,
                            "Related Contract Column is still coming in Downloaded CDR Tracker Report after disabling this column at Client Admin.");
                    break;
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89127. " + e.getMessage());
        } finally {
            ReportRendererListConfigure.updateReportListConfigureResponse(324, reportConfigureListResponse);
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
        }

        csAssert.assertAll();
    }

    /*
    TC-C88888: Verify that at Client setup admin a new column will get added inside filters, columns and excel columns
     */
    @Test(priority = 3)
    public void testC88888() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C88888: Verify that at Client setup admin a new column will get added inside filters, columns and excel columns");
            String reportListJsonResponse = ReportRendererListJson.getListJsonResponse(324, 1002);

            //Verify Filter
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasFilterMetadata(reportListJsonResponse, "documentmovementstatus"),
                    "Contract Porting Status filter not found for CDR Tracker Report on Client Setup Admin.");

            //Verify Column
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasColumn(reportListJsonResponse, "document_movement_status"),
                    "Contract Porting Status Column not found for CDR Tracker Report on Client Setup Admin.");

            //Verify Excel Column
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasExcelColumn(reportListJsonResponse, "document_movement_status"),
                    "Contract Porting Status Excel Column not found for CDR Tracker Report on Client Setup Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88888. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C88885: Verify that at client admin, a new column will get inside filters, columns and excel columns
     */
    @Test(priority = 4)
    public void testC88885() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C88885: Verify that at client admin, a new column will get inside filters, columns and excel columns");
            String reportConfigureResponse = ReportRendererListConfigure.getReportListConfigureResponse(324);

            //Verify Filter
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasFilterMetadata(reportConfigureResponse, "documentmovementstatus"),
                    "Contract Porting Status filter not found for CDR Tracker Report on Client Admin.");

            //Verify Column
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasColumn(reportConfigureResponse, "document_movement_status"),
                    "Contract Porting Status Column not found for CDR Tracker Report on Client Admin.");

            //Verify Excel Column
            csAssert.assertTrue(DefaultUserListMetadataHelper.hasExcelColumn(reportConfigureResponse, "document_movement_status"),
                    "Contract Porting Status Excel Column not found for CDR Tracker Report on Client Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88885. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    TC-C88886: Verify that Contract Porting Status can be enabled and disabled from client admin
     */
    @Test(priority = 5)
    public void testC88886() {
        CustomAssert csAssert = new CustomAssert();
        String reportConfigureListResponse = ReportRendererListConfigure.getReportListConfigureResponse(324);

        try {
            logger.info("Starting Test TC-C88886: Verify that Contract Porting Status can be enabled and disabled from client admin");
            JSONObject jsonObj = new JSONObject(reportConfigureListResponse);

            //Disable Excel Column
            JSONArray jsonArr = jsonObj.getJSONArray("ecxelColumns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.equalsIgnoreCase("document_movement_status")) {
                    jsonArr.getJSONObject(i).put("deleted", true);
                    break;
                }
            }

            jsonObj.put("ecxelColumns", new JSONArray(jsonArr.toString()));

            //Disable Column
            jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.equalsIgnoreCase("document_movement_status")) {
                    jsonArr.getJSONObject(i).put("deleted", true);
                    break;
                }
            }

            jsonObj.put("columns", new JSONArray(jsonArr.toString()));

            //Disable Filter
            jsonArr = jsonObj.getJSONArray("filterMetadatas");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");

                if (queryName.equalsIgnoreCase("documentmovementstatus")) {
                    jsonArr.getJSONObject(i).put("deleted", true);
                    break;
                }
            }

            jsonObj.put("filterMetadatas", new JSONArray(jsonArr.toString()));

            String payload = jsonObj.toString();
            ReportRendererListConfigure.updateReportListConfigureResponse(324, payload);

            String defaultUserListMetadataResponse = metadataObj.hitDefaultUserListMetadataAPIForReportId(324);

            //Verify Filter
            csAssert.assertFalse(DefaultUserListMetadataHelper.hasFilterMetadata(defaultUserListMetadataResponse, "documentmovementstatus"),
                    "Contract Porting Status filter present at End User for CDR Tracker Report after disabling it on Client Admin.");

            //Verify Column
            csAssert.assertFalse(DefaultUserListMetadataHelper.hasColumn(defaultUserListMetadataResponse, "document_movement_status"),
                    "Contract Porting Status Column present at End User for CDR Tracker Report after disabling it on Client Admin.");

            //Verify Excel Column
            csAssert.assertFalse(DefaultUserListMetadataHelper.hasExcelColumn(defaultUserListMetadataResponse, "document_movement_status"),
                    "Contract Porting Status Excel Column present at End User for CDR Tracker Report after disabling it on Client Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88886. " + e.getMessage());
        } finally {
            ReportRendererListConfigure.updateReportListConfigureResponse(324, reportConfigureListResponse);
        }

        csAssert.assertAll();
    }

    /*
    TC-C88880: Verify that Contract Porting Status is added inside last column on CDR tracker report
    TC-C88882
     */
    @Test(priority = 6)
    public void testC88880() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C88880: Verify that Contract Porting Status is added inside last column on CDR tracker report");
            //Download report with Selected Columns
            logger.info("Downloading CDR Tracker Report with Selected Columns");
            boolean downloadReport = downloadCDRTrackerReport(getContractPortingStatusSelectedColumnsFilterJson());

            String cdrShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId);

            if (downloadReport) {
                //Validate data in CDR Tracker Report
                validateContractPortingStatusDataInReport(cdrId, cdrShowResponse, csAssert, "[C88880. Selected Columns.]");
            } else {
                csAssert.assertFalse(true, "C88880. CDR Tracker Report Download failed with Selected Columns");
            }

            //Download Report with All Columns
            downloadReport = downloadCDRTrackerReport(getAllColumnsFilterJson());

            if (downloadReport) {
                //Validate data in CDR Tracker Report
                validateContractPortingStatusDataInReport(cdrId, cdrShowResponse, csAssert, "[C88880. All Columns.]");
            } else {
                csAssert.assertFalse(true, "C88880. CDR Tracker Report Download failed with All Columns");
            }

            validateC88919(csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C88880. " + e.getMessage());
        } finally {
            EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
        }

        csAssert.assertAll();
    }

    private String getContractPortingStatusSelectedColumnsFilterJson() {
        return "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                "\"filterJson\":{\"344\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1040\",\"name\":\"Richline Group Inc\"}]},\"filterId\":344," +
                "\"filterName\":\"customer\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                "\"selectedColumns\":[{\"columnId\":12664,\"columnQueryName\":\"id\"},{\"columnId\":18774,\"columnQueryName\":\"document_movement_status\"}," +
                "{\"columnId\":12666,\"columnQueryName\":\"title\"}]}";
    }

    private void validateContractPortingStatusDataInReport(int cdrId, String cdrShowResponse, CustomAssert csAssert, String additionalInfo) {
        try {
            List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);
            List<String> allHeadersInLowerCase = new ArrayList<>();

            for (String header : allHeaders) {
                allHeadersInLowerCase.add(header.toLowerCase());
            }

            if (allHeadersInLowerCase.contains("contract porting status")) {
                List<String> data = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 5);

                //Validate CDR Id
                String actualValue = data.get(allHeadersInLowerCase.indexOf("id"));
                String expectedValue = ShowHelper.getValueOfField(cdrId, "short code id", cdrShowResponse);
                csAssert.assertEquals(actualValue, expectedValue, "CDR Id Column Validation failed in Downloaded CDR Tracker Report. Expected Id Value: " +
                        expectedValue + " and Actual Value: " + actualValue + ". " + additionalInfo);

                //Validate CDR Title
                actualValue = data.get(allHeadersInLowerCase.indexOf("title"));
                expectedValue = ShowHelper.getValueOfField(cdrId, "title", cdrShowResponse);
                csAssert.assertEquals(actualValue, expectedValue, "CDR Title Column Validation failed in Downloaded CDR Tracker Report. Expected Title Value: " +
                        expectedValue + " and Actual Value: " + actualValue + ". " + additionalInfo);

                //Validate Contract Porting Status
                actualValue = data.get(allHeadersInLowerCase.indexOf("contract porting status"));
                csAssert.assertEquals(actualValue, "Pending Action",
                        "CDR Contract Porting Status Column Validation failed in Downloaded CDR Tracker Report. Expected Value: Pending Action and Actual Value: " +
                                actualValue);
            } else {
                csAssert.assertFalse(true, "Downloaded Report doesn't contain Contract Porting Status Column.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Contract Porting Status Data in CDR Tracker Report. " + additionalInfo +
                    ". " + e.getMessage());
        }
    }

    private void validateC88919(CustomAssert csAssert) {
        try {
            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1000);

            adminHelperObj.loginWithClientAdminUser();
            String fieldLabelResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, 850);
            adminHelperObj.loginWithEndUser();

            String expectedValue = fieldRenamingObj.getClientFieldNameFromName(fieldLabelResponse, "Metadata", "Contract Porting Status");

            //Validate Contract Porting Status Value in DefaultUserListMetadata API for Report
            String defaultUserListResponse = metadataObj.hitDefaultUserListMetadataAPIForReportId(324);
            String actualValue = metadataObj.getColumnPropertyValueFromQueryName(defaultUserListResponse, "document_movement_status", "name");

            boolean matchLabels = StringUtils.matchRussianCharacters(expectedValue, actualValue);
            csAssert.assertTrue(matchLabels, "Internationalization Validation failed in Report DefaultUserListMetadata API Response. Expected Value: " +
                    expectedValue + " and Actual Value: " + actualValue);

            //Validate Related Contract Value in Downloaded Report.
            boolean downloadReport = downloadCDRTrackerReport(getContractPortingStatusSelectedColumnsFilterJson());

            if (downloadReport) {
                //Validate data in CDR Tracker Report
                List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(downloadReportPath, downloadReportName, "Data", 4);

                boolean result = false;
                for (String header : allHeaders) {
                    if (StringUtils.matchRussianCharacters(expectedValue.toLowerCase(), header.toLowerCase())) {
                        result = true;
                        break;
                    }
                }

                csAssert.assertTrue(result, "C88919. Downloaded Report doesn't contain Column [" + expectedValue + "].");
            } else {
                csAssert.assertFalse(true, "C88919. CDR Tracker Report Download failed with Selected Columns");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89083. " + e.getMessage());
        } finally {
            updateAccountObj.updateUserLanguage(ConfigureEnvironment.getEndUserLoginId(), 1002, 1);
        }
    }
}