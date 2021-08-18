package com.sirionlabs.test.reports;

import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCGBTrackerReportSIR219945 {

    private final static Logger logger = LoggerFactory.getLogger(TestCGBTrackerReportSIR219945.class);

    private int cgbTrackerReportId = 264;
    private String gbIdQueryName = "gbid";
    private String excelColumnName = null;
    private String configFilePath;
    private String configFileName;

    /*
    TC-C88334: Verify Governance Body Id in CGB Tracker Report.
     */

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportStatusConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportStatusConfigFileName");
    }
    @Test
    public void testC88334() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C88334: Verify Governance Body Id in CGB Tracker Report.");

            //Verify GB Id Field in DefaultUserListMetadata API Response.
            verifyGBIdInDefaultUserListMetadataResponse(csAssert);

            //Verify GB Id Field in ListData API Response.
            verifyGBIdInListDataResponse(csAssert);

            //Verify GB Id Field in Downloaded Excel.
            verifyGBIdInDownloadedExcel(csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C88334. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyGBIdInDefaultUserListMetadataResponse(CustomAssert csAssert) {
        ReportsDefaultUserListMetadataHelper defaultUserListMetadataHelperObj = new ReportsDefaultUserListMetadataHelper();
        String defaultUserListResponse = defaultUserListMetadataHelperObj.hitDefaultUserListMetadataAPIForReportId(cgbTrackerReportId);

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            csAssert.assertTrue(defaultUserListMetadataHelperObj.isFieldPresentInDefaultUserListMetadataAPIResponse(defaultUserListResponse, gbIdQueryName),
                    "Governance Body Id Field is not present in DefaultUserListMetadata API Response of CGB Tracker Report.");

            excelColumnName = defaultUserListMetadataHelperObj.getColumnPropertyValueFromQueryName(defaultUserListResponse, gbIdQueryName, "name");
        } else {
            csAssert.assertTrue(false, "DefaultUserListMetadata API Response for CGB Tracker Report is an Invalid JSON.");
        }
    }

    private void verifyGBIdInListDataResponse(CustomAssert csAssert) {
        ReportsListHelper reportsListHelperObj = new ReportsListHelper();
        String listDataResponse = reportsListHelperObj.hitListDataAPIForReportId(cgbTrackerReportId);


        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
            int gbIdColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, gbIdQueryName);
            int idColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

            if (gbIdColumnId != -1) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String gbIdValue = jsonObj.getJSONObject(String.valueOf(gbIdColumnId)).getString("value");

                    if (gbIdValue == null || !gbIdValue.contains(":;")) {
                        String recordValue = jsonObj.getJSONObject(String.valueOf(idColumnId)).getString("value");
                        int recordId = ListDataHelper.getRecordIdFromValue(recordValue);

                        csAssert.assertTrue(false, "Parent GB Id value not found in ListData API Response for Record Id: " + recordId);
                    }
                }
            } else {
                csAssert.assertTrue(false, "Couldn't find Governance Body Id Field in ListData API Response of CGB Tracker Report.");
            }
        } else {
            csAssert.assertTrue(false, "ListData API Response for CGB Tracker Report is an Invalid JSON.");
        }
    }

    private void verifyGBIdInDownloadedExcel(CustomAssert csAssert) {
        try {
            if (excelColumnName == null) {
                throw new SkipException("Couldn't set Excel Column Name from DefaultUserListMetadata API Response for CGB Tracker Report.");
            }

            DownloadReportWithData downloadObj = new DownloadReportWithData();
            Map<String, String> params = new HashMap<>();

            Map<String, String> allConstantProperties= ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "testcgbtrackerreportsir219945");

//            String payload = "{\"filterMap\":{\"entityTypeId\":87,\"offset\":0,\"size\":20,\"orderByColumnName\":\"status\",\"orderDirection\":\"asc nulls first\"," +
//                    "\"filterJson\":{\"1\":{\"filterId\":\"1\",\"filterName\":\"supplier\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
//                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1041\",\"name\":\"Dell Technologies\"}]}}}}}";

            ReportsListHelper reportsListHelperObj = new ReportsListHelper();
            String listDataResponse = reportsListHelperObj.hitListDataAPIForReportId(cgbTrackerReportId, allConstantProperties.get("payload"));

            Boolean isReportWithinDownloadLimit = reportsListHelperObj.isReportWithinDownloadLimit(listDataResponse);

            if (isReportWithinDownloadLimit == null) {
                throw new SkipException("Couldn't check whether download limit exceeded or not.");
            }

            if (!isReportWithinDownloadLimit) {
                throw new SkipException("Download Limit Exceeded for CGB Tracker Report. Hence skipping Excel Validation.");
            }

            params.put("jsonData", allConstantProperties.get("payload"));
            HttpResponse response = downloadObj.hitDownloadReportWithData(params, cgbTrackerReportId);

            if (response.getStatusLine().toString().contains("200")) {
                FileUtils fileUtil = new FileUtils();
                String filePath = "src/test/output";
                String fileName = "CGBReport.xlsx";
                String outputFile = filePath + "/" + fileName;

                boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFile);
                if (fileDownloaded) {
                    List<String> allHeaders = XLSUtils.getExcelDataOfOneRow("src/test/output", fileName, "Data", 4);
                    List<String> allHeadersInLowerCase = new ArrayList<>();

                    if (allHeaders == null || allHeaders.isEmpty()) {
                        throw new SkipException("Couldn't Get All Headers from CGB Tracker Report.");
                    }

                    for (String header : allHeaders) {
                        allHeadersInLowerCase.add(header.toLowerCase());
                    }

                    if (allHeadersInLowerCase.contains(excelColumnName.toLowerCase())) {
                        int index = allHeadersInLowerCase.indexOf(excelColumnName.toLowerCase());
                        Long noOfRows = XLSUtils.getNoOfRows(filePath, fileName, "Data");

                        List<String> allGbIdValues = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(filePath, fileName, "Data", index, 4,
                                noOfRows.intValue() - 6);

                        if (allGbIdValues == null || allGbIdValues.isEmpty()) {
                            throw new SkipException("Couldn't get All GB Id Values from Excel.");
                        }

                        if (allGbIdValues.contains("")) {
                            csAssert.assertTrue(false, "Downloaded CGB Tracker Report contains Empty GB Id Value.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't find GB Id Field named [" + excelColumnName + "] in CGB Tracker Report.");
                    }

                    FileUtils.deleteFile(outputFile);
                } else {
                    csAssert.assertTrue(false, "Couldn't dump CGB Tracker Report Download Response into file.");
                }
            } else {
                csAssert.assertTrue(false, "CGB Tracker Report Download failed.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating GB Id in Downloaded Excel of CGB Tracker Report. " + e.getMessage());
        }
    }
}
