package com.sirionlabs.helper.Reports;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsListHelper {

    private final static Logger logger = LoggerFactory.getLogger(ReportsListHelper.class);

    private ReportRendererListData listDataObj = new ReportRendererListData();

    public String hitListDataAPIForReportId(int reportId) {
        logger.info("Hitting ListData API for Report Id {}", reportId);

        listDataObj.hitReportRendererListData(reportId);
        return listDataObj.getListDataJsonStr();
    }

    public String hitListDataAPIForReportId(int reportId, String payload) {
        logger.info("Hitting ListData API for Report Id {}", reportId);

        listDataObj.hitReportRendererListData(reportId, payload);
        return listDataObj.getListDataJsonStr();
    }

    public Integer getReportId(int entityTypeId, String reportName) {
        String reportListJsonResponse = getReportListJsonResponse();

        return getReportId(reportListJsonResponse, entityTypeId, reportName);
    }

    private Integer getReportId(String listReportJsonResponse, int entityTypeId, String reportName) {
        try {
            if (ParseJsonResponse.validJsonResponse(listReportJsonResponse)) {
                JSONArray jsonArr = new JSONArray(listReportJsonResponse);

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonArr.getJSONObject(i).getInt("entityTypeId") == entityTypeId) {
                        JSONArray metadataJsonArr = jsonArr.getJSONObject(i).getJSONArray("listMetaDataJsons");

                        for (int j = 0; j < metadataJsonArr.length(); j++) {
                            if (metadataJsonArr.getJSONObject(j).getString("name").trim().equalsIgnoreCase(reportName.trim())) {
                                return metadataJsonArr.getJSONObject(j).getInt("id");
                            }
                        }
                        break;
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Exception while Getting Report Id for Report [{}] of EntityTypeId {}. {}", reportName, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public String getReportListJsonResponse() {
        ReportRenderListReportJson reportListObj = new ReportRenderListReportJson();
        reportListObj.hitReportRender();
        return reportListObj.getReportRendorJsonStr();
    }

    public List<Map<String, String>> getAllReportsOfEntity(String entityName) {
        String reportListJsonResponse = getReportListJsonResponse();
        return getAllReportsOfEntity(reportListJsonResponse, entityName);
    }

    public List<Map<String, String>> getAllReportsOfEntity(String reportListJsonResponse, String entityName) {
        List<Map<String, String>> allReports = new ArrayList<>();

        try {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            if (ParseJsonResponse.validJsonResponse(reportListJsonResponse)) {
                JSONArray jsonArr = new JSONArray(reportListJsonResponse);

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                    int actualEntityTypeId = jsonObj.getInt("entityTypeId");

                    if (actualEntityTypeId == entityTypeId) {
                        JSONArray reportsJsonArr = jsonObj.getJSONArray("listMetaDataJsons");

                        for (int j = 0; j < reportsJsonArr.length(); j++) {
                            Map<String, String> reportMap = new HashMap<>();

                            int reportId = reportsJsonArr.getJSONObject(j).getInt("id");
                            String reportName = reportsJsonArr.getJSONObject(j).getString("name");
                            boolean isManualReport = reportsJsonArr.getJSONObject(j).getBoolean("isManualReport");

                            reportMap.put("id", String.valueOf(reportId));
                            reportMap.put("name", reportName);
                            reportMap.put("isManualReport", String.valueOf(isManualReport));
                            allReports.add(reportMap);
                        }
                    }
                }
            } else {
                logger.error("ListReportJson API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Reports of Entity {}. {}", entityName, e.getMessage());
        }

        return allReports;
    }

    public Map<Integer, List<Map<String, String>>> getAllEntityWiseReportsMap() {
        String reportListJsonResponse = getReportListJsonResponse();
        return getAllEntityWiseReportsMap(reportListJsonResponse);
    }

    public Map<Integer, List<Map<String, String>>> getAllEntityWiseReportsMap(String reportListJsonResponse) {
        try {
            Map<Integer, List<Map<String, String>>> allReportsMap = new HashMap<>();

            if (ParseJsonResponse.validJsonResponse(reportListJsonResponse)) {
                JSONArray jsonArr = new JSONArray(reportListJsonResponse);

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject jsonObj = jsonArr.getJSONObject(i);
                    Integer entityTypeId = jsonObj.getInt("entityTypeId");
                    JSONArray reportsJsonArr = jsonObj.getJSONArray("listMetaDataJsons");

                    List<Map<String, String>> reportsList = new ArrayList<>();

                    for (int j = 0; j < reportsJsonArr.length(); j++) {
                        Map<String, String> reportMap = new HashMap<>();

                        Integer reportId = reportsJsonArr.getJSONObject(j).getInt("id");
                        String reportName = reportsJsonArr.getJSONObject(j).getString("name");
                        Boolean isManualReport = reportsJsonArr.getJSONObject(j).getBoolean("isManualReport");

                        reportMap.put("id", reportId.toString());
                        reportMap.put("name", reportName);
                        reportMap.put("isManualReport", isManualReport.toString());
                        reportsList.add(reportMap);
                    }

                    allReportsMap.put(entityTypeId, reportsList);
                }

                return allReportsMap;
            } else {
                logger.error("ListReportJson API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Reports Map. {}", e.getMessage());
        }

        return null;
    }

    public Boolean isColumnPresentInListDataAPIResponse(String listDataResponse, String columnName) {
        try {
            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                JSONArray jsonArr = jsonObj.names();

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("columnName").trim().equalsIgnoreCase(columnName.trim())) {
                        return true;
                    }
                }

                return false;
            } else {
                logger.error("ListData API Response is an Invalid JSON. Hence couldn't check Column [{}]", columnName);
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Column [{}] is Present in ListData API Response or not. {}", columnName, e.getStackTrace());
        }
        return null;
    }

    public Boolean downloadGraphWithDataAllColumns(String outputFilePath, String outputFileName, String reportName, int entityTypeId, int reportId, int recordsSize) {
        try {
            logger.info("Hitting ReportRenderer Download API for Report [{}] having Id {}", reportName, reportId);
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            Map<String, String> params = new HashMap<>();
            params.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + recordsSize + ",\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}");

            HttpResponse downloadResponse = downloadObj.hitDownloadGraphWithData(params, reportId);

            FileUtils fileUtil = new FileUtils();
            return fileUtil.writeResponseIntoFile(downloadResponse, outputFilePath + "/" + outputFileName);
        } catch (Exception e) {
            logger.error("Exception while Downloading Graph with Data and All Columns for Report [{}] having Id {} at Location [{}]. {}", reportName, reportId,
                    outputFilePath + "/" + outputFileName, e.getStackTrace());
        }

        return null;
    }

    public Boolean downloadGraphWithDataAllColumnsWithFilterJson(String outputFilePath, String outputFileName, String reportName, int entityTypeId, int reportId, int recordsSize) {
        try {
            logger.info("Hitting ReportRenderer Download API for Report [{}] having Id {}", reportName, reportId);
            DownloadGraphWithData downloadObj = new DownloadGraphWithData();
            Map<String, String> params = new HashMap<>();
            params.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + recordsSize + ",\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"1\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"2530\",\"name\":\"Akshay_Chawla\"}]},\"filterId\":1,\"filterName\":\"supplier\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}}");

            HttpResponse downloadResponse = downloadObj.hitDownloadGraphWithData(params, reportId);

            FileUtils fileUtil = new FileUtils();
            return fileUtil.writeResponseIntoFile(downloadResponse, outputFilePath + "/" + outputFileName);
        } catch (Exception e) {
            logger.error("Exception while Downloading Graph with Data and All Columns for Report [{}] having Id {} at Location [{}]. {}", reportName, reportId,
                    outputFilePath + "/" + outputFileName, e.getStackTrace());
        }

        return null;
    }

    public Boolean isFieldPresentInReportListDataDownloadExcel(String excelFilePath, String excelFileName, String columnName) {
        try {
            logger.info("Getting All Headers from Excel File Located at [{}] and Sheet Data", excelFilePath + "/" + excelFileName);
            List<String> allHeaders = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Data", 4);

            return allHeaders.contains(columnName.trim());
        } catch (Exception e) {
            logger.error("Exception while Checking if Field [{}] is Present or not in Report Download Excel at Location [{}]. {}", columnName,
                    excelFilePath + "/" + excelFileName, e.getStackTrace());
        }

        return null;
    }

    public void verifyListingRecordsData(int entityTypeId, int listId, int listDataOffset, int listDataSize, int maxNoOfRecordsToValidate,
                                         List<String> fieldsToVerify, Map<String, String> fieldsShowPageObjectMap, String showPageExpectedDateFormat,
                                         String listDataExpectedDateFormat, CustomAssert csAssert) {

        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
        int totalRecordsAvailable = -1;
        int noOfRecordsValidated = 0;
        int idColumnNo = -1;
        Map<String, Integer> fieldToVerifyColumnNoMap = new HashMap<>();
        int listDataSizeToHit = maxNoOfRecordsToValidate < listDataSize ? maxNoOfRecordsToValidate : listDataSize;
        int recordNo = 0;

        try {
            logger.info("Validating Listing Records Data for Entity {}", entityName);
            logger.info("Maximum No of Records to Validate for Entity {} are: {}", entityName, maxNoOfRecordsToValidate);

            Map<String, String> fieldTypeMap = ShowHelper.getShowPageObjectTypesMap();

            do {
                logger.info("Hitting List Data API for Entity {} with Offset {} and Size {}.", entityName, listDataOffset, listDataSizeToHit);
                ReportRendererListData listDataObj = new ReportRendererListData();
                String payload = getPayloadForListDataValidation(entityTypeId, listId, listDataSizeToHit, fieldsToVerify, listDataOffset + noOfRecordsValidated);
                listDataObj.hitReportRendererListData(listId, payload);
                String listDataResponse = listDataObj.getListDataJsonStr();

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    if (totalRecordsAvailable == -1) {
                        totalRecordsAvailable = ListDataHelper.getTotalListDataCount(listDataResponse);
                        logger.info("Total No of Records Available in Listing for Entity {} are: {}", entityName, totalRecordsAvailable);
                    }

                    List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListDataToValidate(listDataResponse, fieldsToVerify);

                    if (listData.size() > 0) {
                        noOfRecordsValidated += listData.size();

                        if (idColumnNo == -1) {
                            idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                        }

                        for (Map<Integer, Map<String, String>> listDataRecordMap : listData) {
                            logger.info("***************************************************************************");
                            int recordId = Integer.parseInt(listDataRecordMap.get(idColumnNo).get("valueId"));

                            logger.info("Validating Record #{} having Id {} for Entity {}", ++recordNo, recordId, entityName);
                            logger.info("Hitting Show API for Entity {} and Record Id {}", entityName, recordId);
                            Show showObj = new Show();
                            showObj.hitShow(entityTypeId, recordId);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (ShowHelper.isShowPageAccessible(showResponse)) {
                                    for (String field : fieldsToVerify) {
                                        try {
                                            logger.info("Verifying List Data Field {} for Record #{} having Id {} of Entity {}", field, recordNo, recordId, entityName);
                                            if (!fieldToVerifyColumnNoMap.containsKey(field.trim())) {
                                                fieldToVerifyColumnNoMap.put(field.trim(), ListDataHelper.getColumnIdFromColumnName(listDataResponse, field));
                                            }

                                            int fieldColumnNo = fieldToVerifyColumnNoMap.get(field.trim());
                                            if (fieldColumnNo == -1) {
                                                logger.warn("Couldn't get Column No of Field {} from ListData Response. Hence couldn't validate data", field);
                                                continue;
                                            }

                                            String expectedValue;
                                            expectedValue = field.trim().equalsIgnoreCase("id") ? listDataRecordMap.get(fieldColumnNo).get("valueId") :
                                                    listDataRecordMap.get(fieldColumnNo).get("value");

                                            //Special handling required for SourceId field.
                                            if (field.trim().equalsIgnoreCase("sourceId")) {
                                                ListDataHelper.validateSourceIdField(entityName, listDataRecordMap.get(fieldColumnNo).get("value"), recordId, csAssert);
                                                continue;
                                            }

                                            if (field.trim().equalsIgnoreCase("parentDocumentId") && expectedValue != null) {
                                                expectedValue = String.valueOf(ListDataHelper.getRecordIdFromValue(expectedValue));
                                            }

                                            String showPageObjectName = fieldsShowPageObjectMap.get(field).trim();

                                            if (!fieldTypeMap.containsKey(showPageObjectName)) {
                                                fieldTypeMap.put(showPageObjectName,
                                                        ShowHelper.getTypeOfShowField(showResponse, showPageObjectName, recordId, entityTypeId));
                                            }

                                            String fieldType = fieldTypeMap.get(showPageObjectName);

                                            if (fieldType == null) {
                                                csAssert.assertTrue(false, "Couldn't get Type of Field " + field + " having Show Page Object Name [" +
                                                        showPageObjectName + "] from Show API Response for Record Id " + recordId + " and Entity " + entityName +
                                                        ". Hence couldn't validate data");
                                            } else {
                                                /* Below check is to handle inconsistent behaviour of listData api response for Definition entity*/
                                                if (entityTypeId == 138 && expectedValue != null && (field.trim().equalsIgnoreCase("status") ||
                                                        field.trim().equalsIgnoreCase("company_position"))) {
                                                    expectedValue = expectedValue.trim().split(":;")[0].trim();
                                                }

                                                ListDataHelper.validateListingRecordsDataOnShowPage(showResponse, entityTypeId, entityName, showPageObjectName, expectedValue,
                                                        field, fieldType, recordId, csAssert);
                                            }
                                        } catch (Exception e) {
                                            logger.error("Exception while verifying Listing Data Field {} for Record Id {} of Entity {}. {}", field, recordId,
                                                    entityName, e.getStackTrace());
                                            csAssert.assertTrue(false, "Exception while Verifying Listing Data Field " + field + " for Record Id " +
                                                    recordId + " of Entity " + entityName + ". " + e.getMessage());
                                        }
                                    }
                                } else {
                                    logger.warn("Show Page for Entity {} and Record Id {} is not accessible.", entityName, recordId);
                                }
                            } else {
                                csAssert.assertTrue(false, "Show API Response for Entity " + entityName + " and Record Id " + recordId +
                                        " is an Invalid JSON.");
                            }
                            logger.info("***************************************************************************");
                        }
                    } else {
                        if (ListDataHelper.getFilteredListDataCount(listDataResponse) < 1) {
                            logger.info("No Record present in List Data Response for Entity {} with ListDataSize {} and ListDataOffset {}", entityName, listDataSizeToHit,
                                    (listDataOffset + noOfRecordsValidated));
                            break;
                        } else
                            csAssert.assertTrue(false, "Couldn't create List Data Map List. Issue in code.");
                    }
                } else {
                    csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " with Offset " + listDataOffset + " and Size " +
                            listDataSize + " is an Invalid JSON.");

                    FileUtils.saveResponseInFile(entityName + " ListData API HTML.txt", listDataResponse);
                    break;
                }
                listDataSizeToHit = (maxNoOfRecordsToValidate - noOfRecordsValidated) < listDataSize ? (maxNoOfRecordsToValidate - noOfRecordsValidated) : listDataSize;
            }
            while (noOfRecordsValidated < maxNoOfRecordsToValidate && (listDataOffset + noOfRecordsValidated) < totalRecordsAvailable);

        } catch (Exception e) {
            logger.error("Exception while verifying Listing Records for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Listing Records for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private String getPayloadForListDataValidation(int entityTypeId, int listId, int listDataSize, List<String> fieldsToVerify, int offset) throws Exception {
        String selectedColumnsJson = "[";

        ReportRendererDefaultUserListMetaData defaultObj = new ReportRendererDefaultUserListMetaData();
        defaultObj.hitReportRendererDefaultUserListMetadata(listId);
        String defaultUserListResponse = defaultObj.getReportRendererDefaultUserListMetaDataJsonStr();

        if (!ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            logger.error("DefaultUserListMetaData API Response for List Id {} and EntityTypeId {} is an Invalid JSON.", listId, entityTypeId);
            return null;
        }

        JSONObject defaultJsonObj = new JSONObject(defaultUserListResponse);
        JSONArray jsonArr = defaultJsonObj.getJSONArray("columns");

        for (int i = 0; i < jsonArr.length(); i++) {
            String queryName = jsonArr.getJSONObject(i).getString("queryName").trim();

            if (fieldsToVerify.contains(queryName)) {
                selectedColumnsJson = selectedColumnsJson.concat("{\"columnId\":" + jsonArr.getJSONObject(i).getInt("id") + ",\"columnQueryName\":\"" +
                        queryName + "\"},");
            }
        }

        selectedColumnsJson = selectedColumnsJson.substring(0, selectedColumnsJson.length() - 1);
        selectedColumnsJson = selectedColumnsJson.concat("]");

        return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":" + selectedColumnsJson + "}";
    }

    public Boolean isReportWithinDownloadLimit(int reportId) {
        ReportRendererListData listDataObj = new ReportRendererListData();
        listDataObj.hitReportRendererListData(reportId);
        return isReportWithinDownloadLimit(listDataObj.getListDataJsonStr());
    }

    public Boolean isReportWithinDownloadLimit(String listDataResponse) {
        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
            JSONObject jsonObj = new JSONObject(listDataResponse);
            String limitExceeded = jsonObj.getString("limitExceeded");

            return limitExceeded.equalsIgnoreCase("none");
        }

        return null;
    }

    public String getReportName(int entityTypeId, int reportId) {
        String reportListJsonResponse = getReportListJsonResponse();

        return getReportName(reportListJsonResponse, entityTypeId, reportId);
    }

    private String getReportName(String listReportJsonResponse, int entityTypeId, int reportId) {
        try {
            if (ParseJsonResponse.validJsonResponse(listReportJsonResponse)) {
                JSONArray jsonArr = new JSONArray(listReportJsonResponse);

                for (int i = 0; i < jsonArr.length(); i++) {
                    if (jsonArr.getJSONObject(i).getInt("entityTypeId") == entityTypeId) {
                        JSONArray metadataJsonArr = jsonArr.getJSONObject(i).getJSONArray("listMetaDataJsons");

                        for (int j = 0; j < metadataJsonArr.length(); j++) {
                            if (metadataJsonArr.getJSONObject(j).getInt("id") == reportId) {
                                return metadataJsonArr.getJSONObject(j).getString("name");
                            }
                        }
                        break;
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Exception while Getting Report Name for Report Id {} of EntityTypeId {}. {}", reportId, entityTypeId, e.getStackTrace());
        }

        return null;
    }
}