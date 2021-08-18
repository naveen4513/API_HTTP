package com.sirionlabs.test.reports;

import com.sirionlabs.api.clientAdmin.listingParam.CreateForm;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestReportDownload {
    private final static Logger logger = LoggerFactory.getLogger(TestReportDownload.class);
    private List<String> entitiesToTest = new ArrayList<>();
    private ReportsListHelper reportsListHelper = new ReportsListHelper();
    private Boolean testAllReport = true;
    private String configFilePath = null;
    private String configFileName = null;
    private List<String> reportsToIgnore = new ArrayList<>();

    private ReportsDefaultUserListMetadataHelper reportsDefaultUserListMetadataHelper = new ReportsDefaultUserListMetadataHelper();

    @BeforeClass
    public void entitiesToTest() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportDownloadConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportDownloadConfigFileName");
        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "testallreports");
        if (temp != null && temp.trim().equalsIgnoreCase("false")) {
            testAllReport = false;
        }
        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "reportstoignore");
        if (temp != null && !temp.trim().equalsIgnoreCase("")) {
            String[] reportsArr = temp.split(",");
            for (String report : reportsArr) {
                reportsToIgnore.add(report.trim());

            }
        }
        if (testAllReport) {
            Map<String, String> defaultPropertiesMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "default");
            if (!defaultPropertiesMap.isEmpty()) {
                String[] entitiesArr = defaultPropertiesMap.get("entitiestotest").split(",");
                for (String entity : entitiesArr) {
                    entitiesToTest.add(entity.trim());
                }
            }
        }

    }

    @DataProvider
    public Object[][] allDataForTestCase() {
        List<Object[]> allTestData = new ArrayList<>();
        try {
            String reportListJsonResponse = reportsListHelper.getReportListJsonResponse();
            if (!entitiesToTest.isEmpty()) {
                for (String entityName : entitiesToTest) {
                    if (ParseJsonResponse.validJsonResponse(reportListJsonResponse)) {
                        List<Map<String, String>> reportMetaData = reportsListHelper.getAllReportsOfEntity(reportListJsonResponse, entityName.trim());
                        if (!reportMetaData.isEmpty()) {
                            for (Map<String, String> reportData : reportMetaData) {
                                String reportName = reportData.get("name").trim();
                                String reportId = reportData.get("id").trim();
                                if (reportsToIgnore.contains(reportId)) {
                                    continue;
                                }
                                String isManualReport = reportData.get("isManualReport").trim();
                                if (isManualReport.equalsIgnoreCase("false")) {
                                    allTestData.add(new Object[]{entityName, reportName, reportId});
                                }
                            }
                        } else {
                            logger.error("Report Meta Data is Empty[" + "]" + entityName);
                        }
                    } else {
                        logger.error("ReportListJson API Response for Report is an Invalid JSON.");
                    }
                }
            } else {
                Map<String, String> testSpecificReport = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "report to test");
                if (!testSpecificReport.isEmpty()) {
                    for (Map.Entry<String, String> entry : testSpecificReport.entrySet()) {
                        String entityName = entry.getKey().toLowerCase();
                        String[] reportNames = entry.getValue().split(",");
                        List<String> reportToTest = new ArrayList<>();
                        for (String report : reportNames) {
                            reportToTest.add(report.trim().toLowerCase());
                        }
                        if (ParseJsonResponse.validJsonResponse(reportListJsonResponse)) {
                            List<Map<String, String>> reportMetaData = reportsListHelper.getAllReportsOfEntity(reportListJsonResponse, entityName.trim());
                            if (!reportMetaData.isEmpty()) {
                                for (Map<String, String> reportData : reportMetaData) {
                                    String reportName = reportData.get("name").trim();
                                    if (reportToTest.contains(reportName.toLowerCase())) {
                                        String reportId = reportData.get("id").trim();
                                        if (reportsToIgnore.contains(reportId)) {
                                            continue;
                                        }
                                        String isManualReport = reportData.get("isManualReport").trim();
                                        if (isManualReport.equalsIgnoreCase("false")) {
                                            allTestData.add(new Object[]{entityName, reportName, reportId});
                                        }
                                    }
                                }
                            } else {
                                logger.error("Report Meta Data is Empty[" + "]" + entityName);
                            }
                        } else {
                            logger.error("ReportListJson API Response for Report is an Invalid JSON.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in dataProvider {}", e.getMessage());
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "allDataForTestCase")
    public void validateReportDownload(String entityName, String reportName, String reportId) {
        CustomAssert csAssert = new CustomAssert();

        try {
            String defaultUserListMetadataResponse = reportsDefaultUserListMetadataHelper.hitDefaultUserListMetadataAPIForReportId(Integer.parseInt(reportId));
            Boolean isDownload = reportsDefaultUserListMetadataHelper.isReportDownloadAble(defaultUserListMetadataResponse, reportId, reportName);
            Boolean isListing = reportsDefaultUserListMetadataHelper.isReportListingAvailable(defaultUserListMetadataResponse, reportName, Integer.parseInt(reportId));

            validateNoDataForStatusNotIncludedInReportContentControl(entityName, reportName, reportId, isListing, isDownload, csAssert);
            validateReportDownloadForSelectedColumns(entityName, reportName, reportId, defaultUserListMetadataResponse, isDownload, isListing, csAssert);
           // validateStatusFromReportContentControlInExcel(entityName, reportName, reportId, isDownload,isListing, csAssert);
        } catch (SkipException e) {
            logger.error("SkipException: {}", e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Report Download. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void validateStatusFromReportContentControlInExcel(String entityName, String reportName, String reportId, Boolean isDownload,Boolean isListing, CustomAssert csAssert) {
        try {
            //report default user list meta data
            // check isDownload flag is null or true or false

            if (!(isDownload == null) && isDownload) {
                if (!(isListing==null)&&isListing) {
                    Boolean isReportWithinDownloadLimit= reportsListHelper.isReportWithinDownloadLimit(Integer.parseInt(reportId));
                    if (isReportWithinDownloadLimit==null||isReportWithinDownloadLimit==false)
                    {
                        throw new SkipException("We Can't Download Report Because limitExceeded Flag Max For Report {" + reportName + "} And Report Id{" + reportId + "}");
                    }
                }
                    //download the report
                    DownloadGraphWithData downloadObj = new DownloadGraphWithData();
                    Map<String, String> formParam = new HashMap<>();
                    int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                    formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
                    formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                    HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, Integer.parseInt(reportId));
                    String outputFilePath = "src/test/output";
                    String outputFileName = (reportName + ".xlsx").replace("/", " ");
                    FileUtils fileUtil = new FileUtils();
                    Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
                    if (!fileDownloaded) {
                        throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
                    }
                    try {
                        statusCheck(outputFilePath, outputFileName, reportId, csAssert, reportName);
                    } catch (SkipException e) {
                        throw new SkipException(e.getMessage());
                    } finally {
                        //delete the downloaded file
                        FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
                    }

            } else {
                throw new SkipException("We Can't Download Report Because isDownload Flag False For Report {" + reportName + "} And Report Id{" + reportId + "}");
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception While Verifying All Status  In Report " + reportName + " and Report Id " + reportId + ". " + e.getMessage());
        }
    }

    private void validateNoDataForStatusNotIncludedInReportContentControl(String entityName, String reportName, String reportId, Boolean isListing, Boolean isDownload, CustomAssert csAssert) {
        try {
            //report default user list meta data class object
            if (!(isListing == null) && isListing) {
                // calling function getPayloadAfterApplyingFilter
                String payload = getPayloadAfterApplyingFilter(entityName, reportId, reportName, csAssert);
                if (payload != null) {
                    logger.info("Hitting Report Renderer List Data API");
                    ReportRendererListData reportRendererListData = new ReportRendererListData();
                    reportRendererListData.hitReportRendererListData(Integer.parseInt(reportId), payload);
                    //Now  Check The length Of Data In ReportRendererListData API Response
                    int lengthOfData = new JSONObject(reportRendererListData.getListDataJsonStr()).getJSONArray("data").length();

                    if (!(lengthOfData == 0)) {
                        csAssert.assertTrue(false, "ReportRendererListData API Response Data Is Not Found Null For Report {" + reportName + "} And Report Id{" + reportId + "}");
                    }
                }
            }
            if (!(isDownload == null) && isDownload) {
                // calling function getPayloadAfterApplyingFilter
                String payload = getPayloadAfterApplyingFilter(entityName, reportId, reportName, csAssert);
                //Download the File
                if (payload != null) {
                    DownloadGraphWithData downloadObj = new DownloadGraphWithData();
                    Map<String, String> formParam = new HashMap<>();
                    formParam.put("jsonData", payload);
                    formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                    HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, Integer.parseInt(reportId));
                    String outputFilePath = "src/test/output";
                    String outputFileName = (reportName + ".xlsx").replace("/", " ");
                    FileUtils fileUtil = new FileUtils();
                    Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
                    if (!fileDownloaded) {
                        throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
                    }
                    //calculate number of rows in Excel Data Sheet
                    Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
                    //  read all record data from excel file
                    List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 4,
                            noOfRows.intValue() - 5);
                    if (allRecordsData.size() > 1) {
                        csAssert.assertTrue(false, "Data Found IN Downloaded Excel File {" + reportName + "}");
                    }
                    FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

                }
            }
        } catch (SkipException e) {
            logger.error("Exception While Verifying Filter {}", e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Status Filter For Report [{}] Report Id [{}] {}", reportName, reportId, e.getStackTrace());
            csAssert.assertTrue(false, e.getMessage());
        }
    }

    private void statusCheck(String outputFilePath, String outputFileName, String reportId, CustomAssert csAssert, String reportName) {
        try {
            List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
            logger.info("Hitting Create From Response API");
            String createFormResponse = CreateForm.getCreateFormResponse(Integer.parseInt(reportId));
            Long noOfRows = XLSUtils.getNoOfRows(outputFilePath, outputFileName, "Data");
            List<List<String>> allRecordsData = XLSUtils.getExcelDataOfMultipleRows(outputFilePath, outputFileName, "Data", 4,
                    noOfRows.intValue() - 5);
            if (allRecordsData == null) {
                throw new SkipException("Couldn't get All Records Data from Data Sheet.");
            }
            List<String> allSelectedPerformanceStatus = CreateForm.getAllSelectedStatus(createFormResponse, Integer.parseInt(reportId));
            if (allSelectedPerformanceStatus.isEmpty()) {
                throw new SkipException("Couldn't get All Selected Performance Status for Report [" + reportName + "] having Id " + reportId);
            }

            int columnNoOfPerformanceStatus = 0;
            switch (reportName) {
                case "child obligations":
                    if (allHeadersInExcelDataSheet.contains("PERFORMANCE STATUS")) {
                        columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf("PERFORMANCE STATUS");
                        break;
                    }
                case "contract draft request":
                    if (allHeadersInExcelDataSheet.contains("CURRENT STATUS")) {
                        columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf("CURRENT STATUS");
                        break;
                    }
                default:
                    if (allHeadersInExcelDataSheet.contains("STATUS")) {
                        columnNoOfPerformanceStatus = allHeadersInExcelDataSheet.indexOf("STATUS");
                        break;
                    }

            }
            boolean isActualStatus = false;
            if (!(columnNoOfPerformanceStatus == 0)) {
                for (int i = 0; i < allRecordsData.size(); i++) {
                    String actualPerformanceStatus = allRecordsData.get(i).get(columnNoOfPerformanceStatus);
                    if (!actualPerformanceStatus.equalsIgnoreCase("")) {
                        isActualStatus = true;
                        if (!allSelectedPerformanceStatus.contains(actualPerformanceStatus)) {
                            csAssert.assertTrue(false, "Performance Status for Record #" + (i + 1) + ": [" + actualPerformanceStatus +
                                    "] not found in Selected Performance Status List.");
                        }
                    }
                }
                if (!isActualStatus) {
                    throw new SkipException("No Data Found In Report {" + reportName + "} And Report Id {" + reportId + "}");
                }
            } else {
                csAssert.assertTrue(false, "Status Filed Not Found In Excel Sheet");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception While Verifying All  Status Field In Download Report {} {}", reportName, reportId);
            csAssert.assertTrue(false, e.toString());
        }
    }

    private String getPayloadAfterApplyingFilter(String entityName, String reportId, String reportName, CustomAssert csAssert) {
        DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
        List<String> statusFilterData = new ArrayList<>();
        Map<String, String> statusFilterDtaWithId = new HashMap<>();
        String filterName = null;
        boolean isStatusFilterFound = false;
        int filterId = 0;
        try {
            logger.info("Hitting Report Renderer Filter Data API");
            downloadReportWithData.hitReportRendererFilterData(Integer.parseInt(reportId));
            String responseReportRenderFilterData = downloadReportWithData.getReportRendererFilterDataJsonStr();
            //Check Valid Json Response
            if (ParseJsonResponse.validJsonResponse(responseReportRenderFilterData)) {
                JSONObject jsonObject = new JSONObject(responseReportRenderFilterData);
                String[] allFilterObjectNames = JSONObject.getNames(jsonObject);
                for (String filterObjectName : allFilterObjectNames) {
                    if (reportId.equalsIgnoreCase("264")) {
                        if (jsonObject.getJSONObject(filterObjectName).getString("filterName").equalsIgnoreCase("performanceStatus")) {
                            filterId = jsonObject.getJSONObject(filterObjectName).getInt("filterId");
                            filterName = jsonObject.getJSONObject(filterObjectName).getString("filterName");
                            isStatusFilterFound = true;
                            JSONArray jsonArray = jsonObject.getJSONObject(filterObjectName).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                            for (int j = 0; j < jsonArray.length(); j++) {
                                statusFilterData.add(jsonArray.getJSONObject(j).getString("name"));
                                statusFilterDtaWithId.put(jsonArray.getJSONObject(j).getString("name"), jsonArray.getJSONObject(j).getString("id"));
                            }
                            break;
                        }
                    } else {
                    if (jsonObject.getJSONObject(filterObjectName).getString("filterName").equalsIgnoreCase("status")) {
                        filterId = jsonObject.getJSONObject(filterObjectName).getInt("filterId");
                        filterName = jsonObject.getJSONObject(filterObjectName).getString("filterName");
                        isStatusFilterFound = true;
                        JSONArray jsonArray = jsonObject.getJSONObject(filterObjectName).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                        for (int j = 0; j < jsonArray.length(); j++) {
                            statusFilterData.add(jsonArray.getJSONObject(j).getString("name"));
                            statusFilterDtaWithId.put(jsonArray.getJSONObject(j).getString("name"), jsonArray.getJSONObject(j).getString("id"));
                        }
                        break;
                    }
                }
                }
            }
            if (!isStatusFilterFound) {
                throw new SkipException("Status Filter Not Present In Filter Option");
            }
            String createFormResponse = CreateForm.getCreateFormResponse(Integer.parseInt(reportId));
            List<String> allSelectedPerformanceStatus = CreateForm.getAllSelectedStatus(createFormResponse, Integer.parseInt(reportId));
            if (!allSelectedPerformanceStatus.isEmpty()) {
                statusFilterData.removeAll(allSelectedPerformanceStatus);
            } else {
                csAssert.assertTrue(false, "No Status Found In Create Form Response");
                csAssert.assertAll();
            }
            if (!(statusFilterData.size() == 0)) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"" +
                        ",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":" +
                        "{\"SELECTEDDATA\":[{\"id\":\"" + statusFilterDtaWithId.get(statusFilterData.get(0)) + "\",\"name\":\"" + statusFilterData.get(0) + "\"}]}" +
                        ",\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType" +
                        "\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception {}", e.getMessage());
            csAssert.assertTrue(false, "Exception While Verifying  Status For Report" + reportName + "  Report Id" + reportId);
        }
        return null;
    }

    private void validateReportDownloadForSelectedColumns(String entityName, String reportName, String reportId, String defaultUserListMetadataResponse, Boolean isDownload, Boolean isListing, CustomAssert csAssert) {
        try {
            List<Integer> columnId = new ArrayList<>();
            List<String> columnName = new ArrayList<>();
            Map<Integer, String> columnQueryNameById = new HashMap<>();
            // check isDownload flag is null or true or false
            if (!(isDownload == null) && isDownload) {
                if (!(isListing == null) && isListing) {
                    Boolean isReportWithinDownloadLimit = reportsListHelper.isReportWithinDownloadLimit(Integer.parseInt(reportId));
                    //check is report download flag true or false
                    if (!(isReportWithinDownloadLimit == null) && isReportWithinDownloadLimit) {
                        JSONArray jsonArray = new JSONObject(defaultUserListMetadataResponse).getJSONArray("columns");
                        for (int i = 0; i < 3; i++) {
                            int randomValue = (int) (Math.random() * jsonArray.length());
                            if (jsonArray.getJSONObject(randomValue).getString("queryName").contains("dyn")) {
                                   i--;
                                   continue;
                            }
                            columnId.add(jsonArray.getJSONObject(randomValue).getInt("id"));
                            columnName.add(jsonArray.getJSONObject(randomValue).getString("name").toUpperCase());
                            columnQueryNameById.put(jsonArray.getJSONObject(randomValue).getInt("id"), jsonArray.getJSONObject(randomValue).getString("queryName"));
                        }
                        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                        String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                                "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":" + columnId.get(0) + ",\"" +
                                "columnQueryName\":\"" + columnQueryNameById.get(columnId.get(0)) + "\"},{\"columnId\":" + columnId.get(1) + ",\"" +
                                "columnQueryName\":\"" + columnQueryNameById.get(columnId.get(1)) + "\"},{\"columnId\":" + columnId.get(2) + ",\"" +
                                "columnQueryName\":\"" + columnQueryNameById.get(columnId.get(2)) + "\"}]}";
                        DownloadGraphWithData downloadObj = new DownloadGraphWithData();
                        Map<String, String> formParam = new HashMap<>();
                        formParam.put("jsonData", payload);
                        formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
                        HttpResponse response = downloadObj.hitDownloadGraphWithData(formParam, Integer.parseInt(reportId));
                        String outputFilePath = "src/test/output";
                        String outputFileName = (reportName + ".xlsx").replaceAll("/", " ");
                        FileUtils fileUtil = new FileUtils();
                        Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
                        if (!fileDownloaded) {
                            throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
                        }
                        List<String> allHeadersInExcelDataSheet = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, "Data", 4);
                        FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
                        for (String excelHeader : columnName) {
                            if (!allHeadersInExcelDataSheet.contains(excelHeader)) {
                                csAssert.assertTrue(false, "Column Name Not Found In Excel Sheet {" + excelHeader + "}"+"::::: Payload:::::"+payload);
                            }
                        }

                    }
                }
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception While Verifying All Selected Columns In Downloaded Excel Sheet For Report {} And Report Id{}{}", reportName, reportId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception While Verifying All Selected Columns In Downloaded Excel Sheet");
        }
    }
}
