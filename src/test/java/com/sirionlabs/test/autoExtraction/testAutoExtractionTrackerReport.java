package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.scheduleReport.CreateScheduleReport;
import com.sirionlabs.api.scheduleReport.CreateScheduleReportForm;
import com.sirionlabs.api.scheduleReport.ScheduleByMeReportAPI;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class testAutoExtractionTrackerReport {
    private final static Logger logger = LoggerFactory.getLogger(testAutoExtractionTrackerReport.class);
    private ReportsDefaultUserListMetadataHelper defaultUserListObj = new ReportsDefaultUserListMetadataHelper();
    /*TC C152166: Verify the AE Tracker Report in Reports
      TC C152168: Verify the AE Tracker listing is Successfully Downloading.
     */
    @Test
    public void testDownloadReportData()
    {
        String outputFileName = "AE-Tracker Report.xlsx";
        String outputFilePath="src/test/output";
        int reportId=520;
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("Hitting report DefaultUserListMetadata API");
            String reportsDefaultUserListMetadata = defaultUserListObj.hitDefaultUserListMetadataAPIForReportId(reportId);
            JSONObject reportsDefaultUserListMetadataJsonObj = new JSONObject(reportsDefaultUserListMetadata);
            String queryName = reportsDefaultUserListMetadataJsonObj.get("queryName").toString();
            csAssert.assertEquals(queryName, "autoExtraction.selectAllDocsTracker", "AE Tracker Report is nor present in report Section");
            logger.info("Downloading Auto Extraction Tracker Report Data");
            DownloadReportWithData downloadReportWithDataObj=new DownloadReportWithData();
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
                    "\"filterJson\":{\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                    "\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}}," +
                    "\"selectedColumns\":[{\"columnId\":17298,\"columnQueryName\":\"id\"},{\"columnId\":17304,\"columnQueryName\":\"projects\"}," +
                    "{\"columnId\":17316,\"columnQueryName\":\"status\"},{\"columnId\":17320,\"columnQueryName\":\"workflowstatus\"}," +
                    "{\"columnId\":17310,\"columnQueryName\":\"directorypath\"},{\"columnId\":17312,\"columnQueryName\":\"folder\"}," +
                    "{\"columnId\":17300,\"columnQueryName\":\"documentname\"},{\"columnId\":17333,\"columnQueryName\":\"clusters\"}," +
                    "{\"columnId\":17338,\"columnQueryName\":\"duplicatecount\"},{\"columnId\":17302,\"columnQueryName\":\"contracttype\"}]}";
            Map<String,String> formParam = new HashMap<>();
            formParam.put("_csrf_token","null");
            formParam.put("jsonData",payload);

            HttpResponse downloadResponse =  downloadReportWithDataObj.hitDownloadReportWithData(formParam,reportId);
            logger.info("Checking if Auto extraction tracker report is downloaded");
            Boolean downloadStatus = downloadReportWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath,outputFileName);
            if(!(downloadStatus == true)){
                csAssert.assertTrue(false,"Auto Extraction Report Download is unsuccessful");
            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating AE Tracker Report due to "+e.getMessage());
        }
        finally {
            logger.info("Deleting downloaded AE Tracker report from location "+outputFilePath);
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
            logger.info(outputFileName+" AE Tracker report Deleted successfully.");
        }
        csAssert.assertAll();
    }

    /*
    TC :C152170 Verify that Schedules are Creating successfully
    */
    @Test
    public void testC152170() throws IOException,Exception {
        CustomAssert csAssert=new CustomAssert();
        int reportId=520;
        try {
            CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
            HashMap<String, String> queryStringParams = new HashMap<>();
            queryStringParams.put("id", String.valueOf(reportId));

            logger.info("Hitting Create Report Form API");
            HttpResponse responseCreateReportFormAPI = createScheduleReportForm.hitCreateReportFormAPI(queryStringParams);
            logger.info("Checking if response code for Create Form API is valid");
            csAssert.assertTrue(responseCreateReportFormAPI.getStatusLine().getStatusCode() == 200, "API status code is not correct While hitting CreateScheduleReportForm API for Report Id " + reportId);
            String responseCreateReportFormAPIStr = createScheduleReportForm.getResponseCreateReportFormAPI();

            logger.info("Hitting Create Schedule Report API");

            CreateScheduleReport createScheduleReport = new CreateScheduleReport(responseCreateReportFormAPIStr, 316, reportId);
            createScheduleReport.hitCreateScheduleReportAPI();
            logger.info("Checking if response code for Create Schedule Report API is valid");
            csAssert.assertTrue(createScheduleReport.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReport API for Report Id " + reportId);

            // Hitting the ScheduleByMeReport API
            logger.info("Hitting Schedule By Me Report API");
            ScheduleByMeReportAPI scheduleByMeReportAPI = new ScheduleByMeReportAPI(316, reportId);
            scheduleByMeReportAPI.hitScheduleByMeReportAPI();

            logger.info("Checking if response code for Schedule By Me Report API is valid");
            csAssert.assertTrue(scheduleByMeReportAPI.getApiStatusCode().contains("200"), "API status code is not correct While hitting ScheduleByMeReport API for Report Id " + reportId);
            csAssert.assertTrue(APIUtils.validJsonResponse(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI()), "ScheduleByMeReport API Response in not a valid Json");

            logger.info("Validating entry in schedule report by Me.");
            csAssert.assertTrue(createScheduleReport.validateScheduleByMeReportAPIReponse(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI()), "ScheduleByMeReport API Response don't have any entry having earlier created schedule Report for report id : " + reportId);
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating AE Tracker Create Schedule Report due to "+e.getMessage());
        }
        csAssert.assertAll();
 }

}
