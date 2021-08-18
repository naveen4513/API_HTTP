package com.sirionlabs.test.reports;

import com.monitorjbl.xlsx.StreamingReader;
import com.sirionlabs.api.AllCommonAPI.APIExecutorCommon;
import com.sirionlabs.api.AllCommonAPI.AllCommonAPI;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestCaseReportCA_1797 {
    private final static Logger logger = LoggerFactory.getLogger(TestCaseReportCA_1797.class);
    private String download;

    @DataProvider
    public Object[][] dataProviderForColumnName() {
        List<Object[]> allTestData = new ArrayList<>();
        String reportName="Contract - Pipeline Report - New";
        int reportId=270;
        String columns="Related Contracts,Related Requests";
        allTestData.add(new Object[]{reportName,reportId, columns});
        return allTestData.toArray(new Object[0][]);
    }

    //To verify On client admin "Related Contracts" and "Related Requests"column should be removed from column section for CDR tracker report
    @Test(dataProvider = "dataProviderForColumnName")
    public void testColumnNameC152480(String reportName,int reportId,String columns) {
        logger.info("check column in {} {} tab", reportName, reportId);
        CustomAssert customAssert = new CustomAssert();
        try {
            List<String> allColumns = getAllColumnNameCheckInColumnSection(columns);
            ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(String.valueOf(reportId)));
            String defaultUserListMetaDataResponse;  defaultUserListMetaDataResponse = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetaDataResponse)) {
                DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
                List<String> allColumnQueryNames = defaultUserListMetadataHelper.getAllColumnQueryNames(defaultUserListMetaDataResponse);
                for (String columnName : allColumns) {
                    if (allColumnQueryNames.contains(columnName))
                        customAssert.assertTrue(false, "column {" + columnName + "} found in column section for report name {" + reportName + "} {" + reportId + "} tab");
                }

            }

        } catch (Exception e) {
            logger.error("Exception while verifying column in column section {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying column in column section");
        }
        customAssert.assertAll();
    }

    private List<String> getAllColumnNameCheckInColumnSection(String columns) {
        List<String> allColumnsName = new ArrayList<>();
        String[] columnName = columns.split(",");
        for (String column : columnName) allColumnsName.add(column.toLowerCase().trim());
        return allColumnsName;
    }

    //To verify User should get an Email with Download link after an Hour of Requesting Excel Report.
    @DataProvider
    public Object[][] dataProviderScheduleReport() {
        List<Object[]> allTestData = new ArrayList<>();
        String reportName="Contract Draft Request- Tracker";
        String reportId="324";
        String payload="{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":50,\"orderByColumnName\":\"datecreated\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
        allTestData.add(new Object[]{reportName,reportId,payload});
        return allTestData.toArray(new Object[0][]);
    }
    @Test(dataProvider ="dataProviderScheduleReport")
    public void testCaseScheduleReport(String reportName,String reportId,String payload)
    {
        CustomAssert customAssert=new CustomAssert();
        logger.info("Schedule Report in {} {} tab", reportName, reportId);
        AllCommonAPI allCommonAPI=new AllCommonAPI();
        try {
             APIResponse apiResponse= new APIExecutorCommon().setApiPath(allCommonAPI.getScheduledLargeReportAPI(Integer.parseInt(reportId)))
                                     .setHeaders("Accept", "application/json, text/javascript, */*; q=0.01")
                                     .setHeaders("Content-Type", "application/json;charset=UTF-8")
                                     .setPayload(payload)
                                     .setMethodType("post")
                                     .getResponse();
            if (apiResponse.getResponseCode()==200) {
                   String response=apiResponse.getResponseBody();
                   JSONObject jsonObject=new JSONObject(response);
                   boolean success=jsonObject.getBoolean("success");
                   String message=jsonObject.getString("message");
                   String error= jsonObject.getString("errors");
                   if (!success&&!message.equalsIgnoreCase("Request submitted. You will get notification over e-mail.")&& !error.isEmpty()) {
                       customAssert.assertTrue(false,"report is not scheduling ="+message+" error="+error);
                   }
            }
        }
        catch (Exception e) {
            logger.error("Exception while verifying schedule report {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying schedule report");
        }
        customAssert.assertAll();
    }
    @Test(dependsOnMethods = "testCaseScheduleReport")
    public void verifiedScheduleReportInScheduleJob()
    {
        CustomAssert customAssert=new CustomAssert();
        logger.info("Schedule Report in tab");
        String payload="{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"datereportgenerated\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
        try {
            int timeStamp=0;
            boolean isSuccess = false;
            AllCommonAPI allCommonAPI=new AllCommonAPI();
            String reportName = null;
            String dateCreated=null;
            String dateReportGenerated=null;
            String status=null;
            a:while (timeStamp<300000) {
                APIResponse apiResponse = new APIExecutorCommon().setApiPath(allCommonAPI.getListRendererListScheduleData(225))
                        .setHeaders("Accept", "application/json, text/javascript, */*; q=0.01")
                        .setHeaders("Content-Type", "application/json;charset=UTF-8")
                        .setPayload(payload)
                        .setMethodType("post")
                        .getResponse();

                if (apiResponse.getResponseCode() == 200) {
                    String response = apiResponse.getResponseBody();
                    JSONObject jsonObject = new JSONObject(response);
                    JSONArray jsonArray = jsonObject.getJSONArray("data");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONArray name = jsonArray.getJSONObject(i).names();
                        for (int j=0; j<name.length();j++) {
                            if (jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("columnName").equalsIgnoreCase("reportname")) {
                                reportName = jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("value");
                            }
                            else if(jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("columnName").equalsIgnoreCase("datecreated")) {
                                dateCreated = jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("value");
                            }
                            else if(jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("columnName").equalsIgnoreCase("datereportgenerated")) {
                                dateReportGenerated = jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("value");
                            }
                            else if (jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("columnName").equalsIgnoreCase("status")) {
                                status = jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("value");
                            }
                            else
                            {
                                download = jsonArray.getJSONObject(i).getJSONObject(name.getString(j)).getString("value");
                            }
                        }
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDateTime now = LocalDateTime.now();
                        assert reportName != null;
                        if (reportName.equalsIgnoreCase("Contract Draft Request- Tracker")) {
                            assert status != null;
                            if (status.equalsIgnoreCase("ACTIVE")) {
                                assert dateReportGenerated != null;
                                if (dateReportGenerated.trim().equalsIgnoreCase(dtf.format(now))) {
                                    assert dateCreated != null;
                                    if (dateCreated.trim().equalsIgnoreCase(dtf.format(now))) {
                                        isSuccess = true;
                                        break a;
                                    }
                                }
                            }
                        }
                    }
                }
                Thread.sleep(10);
                timeStamp+=500;
            }

            if (isSuccess)
            {
                  String []arr=download.split(":;");
            HttpResponse httpResponse= new APIExecutorCommon().setApiPath(allCommonAPI.getScheduleLargeReportDownloadReport(arr[0]).trim())
                                        .setHeaders("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                                        .setMethodType("get")
                                        .getHttpResponseForGetAPI();
                          excelDownload(httpResponse,arr[1],customAssert);
            }
            else {
                customAssert.assertTrue(false,"report is not present in scheduled job section");
            }
        }
        catch (Exception e) {
            logger.error("Exception while verifying schedule report {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying schedule report");
        }
        customAssert.assertAll();
    }

    private void excelDownload(HttpResponse response,String entityName,CustomAssert customAssert) {
        String outputFilePath = "src/test/output";
        String outputFileName = entityName + ".xlsx";
        try {
            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
             if (!fileDownloaded) throw new SkipException("Couldn't Download Data for Entity [" + entityName + "]");
            List<String> allHeadersInExcelDataSheet = new ArrayList<>();
            InputStream is = new FileInputStream(new File(outputFilePath + "/" + outputFileName));
            Workbook reader = StreamingReader.builder()
                    .rowCacheSize(10)  // number of rows to keep in memory (defaults to 10)
                    .bufferSize(8192)     // buffer size to use when reading InputStream to file (defaults to 1024)
                    .open(is);            // InputStream or File for XLSX file (required)
            Sheet name = reader.getSheet("Data");
            Iterator<Row> row = name.rowIterator();
            int k = 1;
            while (row.hasNext()) {
                row.next();
                if (k == 3) {

                    Row row1 = row.next();
                    int f = row1.getPhysicalNumberOfCells();
                    for (int i = 0; i < f; i++)
                        allHeadersInExcelDataSheet.add(row1.getCell(i).getStringCellValue().toLowerCase());
                }
                k++;
            }
                if (!allHeadersInExcelDataSheet.contains("related contract")&&!allHeadersInExcelDataSheet.contains("related requests"))
                      customAssert.assertTrue(false, "Column name Related Contract and Related Requests not found in excel scheduled download for " + entityName);

        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
        finally {
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
        }
       customAssert.assertAll();
    }
}
