package com.sirionlabs.test;

import com.sirionlabs.api.reportRenderer.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDownloadGraph extends TestRailBase {
    private final static Logger logger = LoggerFactory.getLogger(TestDownloadGraph.class);
    static String csrfToken;
    static String outputFilePath;
    static String outputImageFormatForDownloadGraph;
    static String outputFileFormatForDownloadGraphWithData;
    static String reportRendererConfigFileName;
    static String reportRendererConfigFilePath;
    static String entityIdMappingFileName;
    static String entityIdConfigFilePath;
    static List<String> allEntitySection;
    static int maxRandomOptions = 5;
    static Integer size = 20;
    static int offset = 0;
    static String orderByColumnName = "id";
    static String orderDirection = "desc";


    @BeforeClass
    public void setStaticFields() throws ConfigurationException {
        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadReportGraphFilePath");
        outputImageFormatForDownloadGraph = ConfigureConstantFields.getConstantFieldsProperty("DownloadReportGraphImageFormat");
        outputFileFormatForDownloadGraphWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadReportGraphWithDataFileFormat");

        reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
        reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");

        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");

        allEntitySection = ParseConfigFile.getAllSectionNames(reportRendererConfigFilePath, reportRendererConfigFileName);

        if (!ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                "maxrandomoptions").trim().equalsIgnoreCase(""))
            maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                    "maxrandomoptions").trim());

        if (!ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                "size").trim().equalsIgnoreCase(""))
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "size"));

        if (!ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                "offset").trim().equalsIgnoreCase(""))
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "offset"));

        if (!ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                "orderbycolumnname").trim().equalsIgnoreCase(""))
            orderByColumnName = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "orderByColumnName");

        if (!ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,
                "orderdirection").trim().equalsIgnoreCase(""))
            orderDirection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "orderDirection");
        testCasesMap = getTestCasesMapping();
    }

    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[2];
            //Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection, "entity_type_id"));
            Integer entitySectionTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            i++;
        }

        return groupArray;
    }

    @Test(dataProvider = "getAllEntitySection", priority = 1)
    public void testDownloadGraph(String entityName, Integer entityTypeId) {
        CustomAssert cAssert = new CustomAssert();
        logger.info("***************** Validating download graph for entity {} ************", entityName);
        try {
            if (entityTypeId != 0) {
                ReportRenderListReportJson reportObj = new ReportRenderListReportJson();
                reportObj.hitReportRender();
                String reportRendererJsonStr = reportObj.getReportRendorJsonStr();
                if (APIUtils.validJsonResponse(reportRendererJsonStr, "[reportRendererJson response]")) {

                    String reportRendererChartDataPayload = getReportRendererChartDataPayload(entityTypeId);
                    Map<Integer, String> reportIdNameMap = reportObj.getReportIdNameMapExcludingManualReports(reportRendererJsonStr, entityTypeId);

                    for (Map.Entry<Integer, String> entryMap : reportIdNameMap.entrySet()) {

                        String metaDataResponseStr = getMetaDataResponseStr(entryMap.getKey(), entityName);
                        if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
                            boolean isChartAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isChart");

                            if (isChartAvailable) {
                                ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
                                reportRendererListDataObj.hitReportRendererListData(entryMap.getKey());
                                String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
                                if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
                                    JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
                                    if (reportListDataJson.getInt("totalCount") == 0)
                                        logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraph image file for this report.", entryMap.getValue());

                                    else {
                                        ReportRendererChartData chartDataObj = new ReportRendererChartData();
                                        chartDataObj.hitReportRendererChartData(reportRendererChartDataPayload, entryMap.getKey());
                                        String reportRendererChartDataResponseStr = chartDataObj.getReportRendererChartDataJsonStr();

                                        if (APIUtils.validJsonResponse(reportRendererChartDataResponseStr, "[reportRendererChartDataResponse]")) {

                                            Map<String, String> formParam = getDownloadGraphPayload(reportRendererChartDataResponseStr);
                                            DownloadGraph downloadGraphObj = new DownloadGraph();
                                            HttpResponse response = downloadGraphObj.hitDownloadGraphOld(formParam);

                                            cAssert.assertTrue(response.getStatusLine().getStatusCode()==200,"DownloadGraph API Invalid Status Code ::"+response.getStatusLine().getStatusCode()+"for entity :: "+entityName+" and report Id :: "+entryMap.getValue());

                                            /*
                                             * dumping response into file
                                             * */
                                            dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraph", entityName, entryMap.getValue(), cAssert);
                                        } else {
                                            cAssert.assertTrue(false, "reportRendererChartDataResponseStr is not valid json for report = {}" + entryMap.getValue());
                                            logger.error("reportRendererChartDataResponseStr is not valid json for entity = {} and report = {}. Hence skipping download graph validation.", entityName, entryMap.getValue());
                                        }
                                    }
                                } else {
                                    cAssert.assertTrue(false, "ReportRendererListData is not valid json for report = {}" + entryMap.getValue());
                                    logger.error("ReportRendererListData response is not valid json for report = {}", entryMap.getValue());
                                }
                            } else
                                logger.warn("Chart is not available(isChart = false) for entity {} , report {}. Hence skipping download graph for this report.", entityName, entryMap.getValue());
                        } else {
                            cAssert.assertTrue(false, "MetaData response is not valid json for report = {}" + entryMap.getValue());
                            logger.error("Meta Data response for report = {} is not valid json. downloadGraph validation skipped.", entryMap.getValue());
                        }
                    }
                } else {
                    cAssert.assertTrue(false, "reportRendererJson response is not valid json.");
                    logger.error("reportRendererJson is not valid json for {}. Hence skipping download graph validation.", entityName);
                }
            }
        } catch (Exception e) {
            cAssert.assertTrue(false, "Exception occurred while validating download graph for reports." + e.getMessage());
            logger.error("Exception occurred while validating download graph for reports. {}", e.getMessage());
        } finally {
            addTestResult(getTestCaseIdForMethodName("testDownloadGraph"), cAssert);
        }
        cAssert.assertAll();
    }

    @Test(dataProvider = "getAllEntitySection", priority = 2)
    public void testDownloadGraphWithData(String entityName, Integer entityTypeId) {
        CustomAssert customAssert = new CustomAssert();

        try {
            logger.info("***************** Validating download graph with data for entity {} ************", entityName);

            if (entityTypeId != 0) {
                ReportRenderListReportJson reportObj = new ReportRenderListReportJson();
                reportObj.hitReportRender();
                String reportRendererJsonStr = reportObj.getReportRendorJsonStr();
                if (APIUtils.validJsonResponse(reportRendererJsonStr, "[reportRendererJson response]")) {
                    Map<Integer, String> reportIdNameMap = reportObj.getReportIdNameMapExcludingManualReports(reportRendererJsonStr, entityTypeId);

                    for (Map.Entry<Integer, String> entryMap : reportIdNameMap.entrySet()) {
                        String metaDataResponseStr = getMetaDataResponseStr(entryMap.getKey(), entityName);

                        if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
                            Boolean isListingAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isListing");
                            Boolean isDownloadAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isDownload");

                            if (isListingAvailable && isDownloadAvailable) {
                                ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
                                reportRendererListDataObj.hitReportRendererListData(entryMap.getKey());
                                String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
                                if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
                                    JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
                                    if (reportListDataJson.getInt("totalCount") == 0)
                                        logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraphWithData file for this report.", entryMap.getValue());

                                    else {
                                        downloadGraphDataForAllColumns(entityTypeId, entityName, entryMap.getKey(), entryMap.getValue(), customAssert);
                                        downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, entryMap.getKey(), entryMap.getValue(), true, customAssert);
                                        downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, entryMap.getKey(), entryMap.getValue(), false, customAssert);
                                    }
                                } else {
                                    customAssert.assertTrue(false, "reportListData is not valid json for report = " + entryMap.getValue());
                                    logger.error("reportListData is not valid json for report = {}. Hence generation of downloadGraphWithData file is skipped.", entryMap.getValue());
                                }
                            } else {
                                logger.warn("download is not available(isDownload = false) for entity {} and report {}. Hence skipping download data for this report.", entityName, entryMap.getValue());
                            }
                        } else {
                            customAssert.assertTrue(false, "Meta Data response is not valid json for report = " + entryMap.getValue());
                            logger.error("MetaData response is not valid json for report = {}. DownloadGraphWithData file generation skipped.", entryMap.getValue());
                        }
                    }
                }
            } else
                logger.warn("Entity Id not found for the entity {}", entityName);
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception occurred in testDownloadGraphWithData. " + e.getMessage());
            logger.error("Exception occurred in testDownloadGraphWithData. {} ", e.getMessage());
        }
        addTestResult(getTestCaseIdForMethodName("testDownloadGraphWithData"), customAssert);
        customAssert.assertAll();
    }


    private Map<String, String> getDownloadGraphPayload(String reportRendererChartDataResponseStr) {
        Map<String, String> formParam = new HashMap<>();

        formParam.put("chartJson", reportRendererChartDataResponseStr);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId) {
        return this.getDownloadGraphWithDataPayload(entityTypeId, null);
    }

    private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap) {
        Map<String, String> formParam = new HashMap<>();
        String jsonData;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";
        } else {
            StringBuilder selectedColumnArray = new StringBuilder("\"selectedColumns\":[");
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray.append("{\"columnId\":").append(entryMap.getKey()).append(",\"columnQueryName\":\"").append(entryMap.getValue()).append("\"},");
            }
            selectedColumnArray = new StringBuilder(selectedColumnArray.substring(0, selectedColumnArray.length() - 1));
            selectedColumnArray.append("]");

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private String getReportRendererChartDataPayload(Integer entityTypeId) {
        String payload;

        payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";

        return payload;
    }

    private void downloadGraphDataForAllColumns(Integer entityTypeId, String entityName, Integer reportId, String reportName, CustomAssert csAssert) {
        Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId);

        DownloadGraphWithData downloadGraphWithDataObj = new DownloadGraphWithData();
        logger.info("Hitting DownloadGraphWithData for entity {} [id = {}] and reportName {} [id = {}]", entityName, entityTypeId, reportName, reportId);
        HttpResponse response = downloadGraphWithDataObj.hitDownloadGraphWithData(formParam, reportId);

        /*
         * dumping response into file
         * */
        dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraphWithData", entityName, reportName, "AllColumn", csAssert);
    }

    private void downloadGraphDataForSelectedColumns(String metaDataResponseStr, Integer entityTypeId, String entityName, Integer reportId, String reportName, Boolean isRandomizationRequiredOnColumn, CustomAssert csAssert) throws Exception {

        ReportRendererDefaultUserListMetaData metaDataObj = new ReportRendererDefaultUserListMetaData();
        Map<Integer, String> columnIdNameMap = metaDataObj.getColumnIdNameMap(metaDataResponseStr, isRandomizationRequiredOnColumn, maxRandomOptions);

        Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId, columnIdNameMap);
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

    /*
     * Below method is for dumping the DownloadGraphWithData response into file(default format : xlsx). Column status is appended in the file name to differentiate among :
     * 1. All Columns
     * 2. Default Selected Columns
     * 3. Randomized selected Columns
     *
     * featureName parameter is for creating new folder in the output directory. This will help to easily analyse the downloaded files separately for downloadGraph and downloadGraphWithData feature.
     * featureName = DownloadGraph or DownloadGraphWithData
     * */
    private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String reportName, String columnStatus, CustomAssert csAssert) {
        String outputFile = null;
      try {
          reportName = removeSpecialCharactersFromFileName(reportName); //replacing special characters
          FileUtils fileUtil = new FileUtils();
          Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
          Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
          if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
              if (columnStatus != null)
                  outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + "_" + columnStatus + outputFileFormatForDownloadGraphWithData;
              else
                  outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + outputImageFormatForDownloadGraph;

              Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFile);

              // Commenting this part as of now : 30th May 2018 -> shiv
//			if (columnStatus == null && (ImageReader.isImageHasChartTypeNotSupported(outputFile) || ImageReader.isImageHasNotDataToDisplay(outputFile))) {
//				if (outputFile.contains("Contract Draft Request- Lead Time")) {
//					logger.warn("Since no test data is available. Hence skipping image validation of Contract Draft Request- Lead Time downloaded graph.");
//				} else
//					csAssert.assertTrue(false, "Image is Not a Valid One ,  Since it's either has no Data to Display or Chart Type Type Not Supported. output file = " + outputFile);
//			}

              if (!fileDownloaded) {
                  csAssert.assertTrue(false, "Couldn't Download Graph for Report [" + reportName + "] and Entity Name " + entityName);
              } else logger.info("DownloadGraphWithData file generated at {}", outputFile);
          }
      }catch(Exception e) {
          csAssert.assertTrue(false,"Exception while Downloading Graph for entity and report name ::"+ entityName+"::"+reportName+":::::"+e.getMessage());
      }finally {
          FileUtils.deleteFile(outputFile);
      }

    }

    private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String reportName, CustomAssert csAssert) {
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

}
