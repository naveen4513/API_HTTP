package com.sirionlabs.helper.Reports;

import com.sirionlabs.api.reportRenderer.*;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.test.reports.TestReportsDataDownload;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReportsDownloadHelper {

    private final static Logger logger = LoggerFactory.getLogger(ReportsDownloadHelper.class);
    private String outputFilePath = "src/test/output";
    private String outputFile;


    private String getReportRendererChartDataPayload(Integer entityTypeId) {
        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
                ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
    }

    private String getMetaDataResponseStr(Integer reportId, String entityName) {
        String responseStr = null;
        try {
            ReportRendererDefaultUserListMetaData metaDataObj = new ReportRendererDefaultUserListMetaData();
            metaDataObj.hitReportRendererDefaultUserListMetadata(reportId);
            responseStr = metaDataObj.getReportRendererDefaultUserListMetaDataJsonStr();
        } catch (Exception e) {
            logger.error("Exception occurred while hitting metaData API for entity {}", entityName);
        }
        return responseStr;
    }

    private Map<String, String> getDownloadGraphPayload(String reportRendererChartDataResponseStr) {
        Map<String, String> formParam = new HashMap<>();

        formParam.put("chartJson", reportRendererChartDataResponseStr);
        formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

        return formParam;
    }

    private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName,
                                                           String reportName, CustomAssert csAssert) {
        this.dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, featureName, entityName, reportName, null, csAssert);
    }

    private String removeSpecialCharactersFromFileName(String fileName) {
        String newFileName;
        logger.info("fileName before removing special character : {}", fileName);
        newFileName = fileName.replaceAll("[+^/*<:>?|]*", "");
        newFileName = newFileName.replaceAll("\\\\", "");
        newFileName = newFileName.replaceAll("\"", "");
        logger.info("fileName after removing special character : {}", newFileName);

        return newFileName;
    }

    private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName,
                                                           String reportName, String columnStatus, CustomAssert csAssert) {

        String outputImageFormatForDownloadGraph = ".png";
        String outputFileFormatForDownloadGraphWithData = ".xlsx";
        reportName = removeSpecialCharactersFromFileName(reportName); //replacing special characters
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            if (columnStatus != null)
                outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + "_" + columnStatus + outputFileFormatForDownloadGraphWithData;
            else
                outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + outputImageFormatForDownloadGraph;

            Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);

            if (status)
                logger.info("DownloadGraphWithData file generated at {}", outputFile);
            else {
                csAssert.assertTrue(false, "File not generated at [" + outputFile + "] for Report [" + reportName + "]");
            }
        }
    }

    public String reportGraphDownload(Integer reportId, String reportName, int entityTypeId, String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Downloading Graph for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
            String reportRendererChartDataPayload = getReportRendererChartDataPayload(entityTypeId);
            String metaDataResponseStr = getMetaDataResponseStr(reportId, entityName);

            if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
                Boolean isChartAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isChart");

                if (isChartAvailable) {
                    ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
                    reportRendererListDataObj.hitReportRendererListData(reportId);
                    String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
                    if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
                        JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
                        if (reportListDataJson.getInt("totalCount") == 0)
                            logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraph image file for this report.", reportName);

                        else {
                            ReportRendererChartData chartDataObj = new ReportRendererChartData();
                            chartDataObj.hitReportRendererChartData(reportRendererChartDataPayload, reportId);
                            String reportRendererChartDataResponseStr = chartDataObj.getReportRendererChartDataJsonStr();

                            if (APIUtils.validJsonResponse(reportRendererChartDataResponseStr, "[reportRendererChartDataResponse]")) {

                                Map<String, String> formParam = getDownloadGraphPayload(reportRendererChartDataResponseStr);
                                DownloadGraph downloadGraphObj = new DownloadGraph();
                                HttpResponse response = downloadGraphObj.hitDownloadGraph(formParam);

                                dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraph", entityName, reportName,
                                        csAssert);
                            } else {
                                csAssert.assertTrue(false, "reportRendererChartDataResponseStr is not valid json for report = {}" + reportName);
                                logger.error("reportRendererChartDataResponseStr is not valid json for entity = {} and report = {}. Hence skipping download graph validation.",
                                        entityName, reportName);
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "ReportRendererListData is not valid json for report = {}" + reportName);
                        logger.error("ReportRendererListData response is not valid json for report = {}", reportName);
                    }
                } else
                    logger.warn("Chart is not available(isChart = false) for entity {} , report {}. Hence skipping download graph for this report.", entityName,
                            reportName);
            } else {
                csAssert.assertTrue(false, "MetaData response is not valid json for report = {}" + reportName);
                logger.error("Meta Data response for report = {} is not valid json. downloadGraph validation skipped.", reportName);
            }
        } catch (Exception e) {
            logger.error("Exception while Downloading Graph for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Downloading Graph for Report [" + reportName + "] having Id " + reportId + " of Entity " +
                    entityName);
        }
        //csAssert.assertAll();
        return outputFile;
    }

    public String reportGraphDownloadWithData(Integer reportId, String reportName, int entityTypeId, String entityName, String filterJson, boolean selectedCol) {
        return reportGraphDownloadWithData(reportId,reportName,entityTypeId,entityName,filterJson,selectedCol,false);
    }

        public String reportGraphDownloadWithData(Integer reportId, String reportName, int entityTypeId, String entityName, String filterJson, boolean selectedCol, boolean selectRandomColumn) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Downloading Graph with Data for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
            String metaDataResponseStr = getMetaDataResponseStr(reportId, entityName);

            if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
                Boolean isListingAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isListing");
                Boolean isDownloadAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isDownload");

                if (isListingAvailable && isDownloadAvailable) {
                    ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
                    reportRendererListDataObj.hitReportRendererListData(reportId);
                    String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
                    if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
                        JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
                        if (reportListDataJson.getInt("totalCount") == 0)
                            logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraphWithData file for this report.",
                                    reportName);

                        else {
                            if(selectedCol){
                                if(selectRandomColumn)
                                    downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, reportId, reportName, true,
                                        csAssert,filterJson);
                                else
                                    downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, reportId, reportName, false,
                                        csAssert,filterJson);
                            }
                            else
                                downloadGraphDataForAllColumns(entityTypeId, entityName, reportId, reportName, csAssert,filterJson);

                        }
                    } else {
                        csAssert.assertTrue(false, "reportListData is not valid json for report = " + reportName);
                        logger.error("reportListData is not valid json for report = {}. Hence generation of downloadGraphWithData file is skipped.", reportName);
                    }
                } else {
                    logger.warn("download is not available(isDownload = false) for entity {} and report {}. Hence skipping download data for this report.",
                            entityName, reportName);
                }
            } else {
                csAssert.assertTrue(false, "Meta Data response is not valid json for report = " + reportName);
                logger.error("MetaData response is not valid json for report = {}. DownloadGraphWithData file generation skipped.", reportName);
            }
        } catch (Exception e) {
            logger.error("Exception while Downloading Graph with Data for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Downloading Graph with Data for Report [" + reportName + "] having Id " + reportId + " of Entity " +
                    entityName);
        }
        //csAssert.assertAll();
        return outputFile;
    }

    private void downloadGraphDataForAllColumns(Integer entityTypeId, String entityName, Integer reportId, String reportName, CustomAssert csAssert,String filterJson) {
        Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId,filterJson);

        DownloadGraphWithData downloadGraphWithDataObj = new DownloadGraphWithData();
        logger.info("Hitting DownloadGraphWithData for entity {} [id = {}] and reportName {} [id = {}]", entityName, entityTypeId, reportName, reportId);
        HttpResponse response = downloadGraphWithDataObj.hitDownloadGraphWithData(formParam, reportId);

        /*
         * dumping response into file
         * */
        dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraphWithData", entityName, reportName, "AllColumn", csAssert);
    }

    private void downloadGraphDataForSelectedColumns(String metaDataResponseStr, Integer entityTypeId, String entityName, Integer reportId, String reportName, Boolean isRandomizationRequiredOnColumn, CustomAssert csAssert,String filterJson) throws Exception {

        ReportRendererDefaultUserListMetaData metaDataObj = new ReportRendererDefaultUserListMetaData();
        Map<Integer, String> columnIdNameMap = metaDataObj.getColumnIdNameMap(metaDataResponseStr, isRandomizationRequiredOnColumn, 5);

        Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId, columnIdNameMap,filterJson);
        DownloadGraphWithData downloadGraphWithDataObj = new DownloadGraphWithData();
        logger.info("Hitting DownloadGraphWithData for entity {} [id = {}] and reportName {} [id = {}]", entityName, entityTypeId, reportName, reportId);
        HttpResponse response = downloadGraphWithDataObj.hitDownloadGraphWithData(formParam, reportId);

        String columnStatus = "DefaultColumn";
        if (isRandomizationRequiredOnColumn)
            columnStatus = "RandomizedColumn";

        /*
         * dumping response into file
         * */
        dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraphWithData", entityName, reportName, columnStatus, csAssert);
    }


    private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId,String filterJson) {
        return this.getDownloadGraphWithDataPayload(entityTypeId, null,filterJson);
    }

    private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap,String filterJson) {
        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{"+filterJson+"}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{"+filterJson+"}}," + selectedColumnArray + "}";

        }

        logger.info("json payload formed is {}",jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

        return formParam;
    }

    public String checkCellColor(int colNum){
        String color = "" ;
        try{
            File file = new File(outputFile);
            FileInputStream fis = new FileInputStream(file);
            ZipSecureFile.setMinInflateRatio(-1.0d);
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sh = wb.getSheet("Data");
            int rowCount = sh.getPhysicalNumberOfRows();
            logger.info("Row Count : "+rowCount);
            XSSFCellStyle cs;
            for(int rowIndex=4;rowIndex<rowCount-2;rowIndex++){
                cs = sh.getRow(rowIndex).getCell(colNum).getCellStyle();
                color = cs.getFillForegroundColorColor().getARGBHex();
                logger.info("Color: "+color);
            }
        }
        catch(Exception e)
        {
            logger.debug("Exception found while reading cell color "+e.toString());
        }
        return color;
    }
}
