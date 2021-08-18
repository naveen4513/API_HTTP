package com.sirionlabs.test.CommonColumnFilterAndListing;

import com.monitorjbl.xlsx.StreamingReader;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestFilterColumnListing {
    private final static Logger logger = LoggerFactory.getLogger(TestFilterColumnListing.class);
    private String columnConfigFilePath;
    private String columnConfigFileName;
    private String filterConfigFilePath;
    private String filterConfigFileName;

    @BeforeClass
    public void beforeClass() {
        columnConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestColumnConfigFilePath");
        columnConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestColumnConfigFileName");
        filterConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestFilterConfigFilePath");
        filterConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestFilterConfigFileName");
    }

    @DataProvider
    public Object[][] dataProviderForColumnName() {
        List<Object[]> allTestData = new ArrayList<>();
        if (columnConfigFileName != null && columnConfigFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(columnConfigFilePath, columnConfigFileName, "column");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String[] key = entity.getKey().split("/");
                    String sectionName = key[0];
                    String entityName = key[1];
                    String subEntityName = key[2];
                    String listID = ParseConfigFile.getValueFromConfigFile(columnConfigFilePath, columnConfigFileName, sectionName, entityName + "/" + subEntityName);
                    String columns = entity.getValue();
                    allTestData.add(new Object[]{sectionName, entityName, subEntityName, listID, columns});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderForFilterName() {
        List<Object[]> allTestData = new ArrayList<>();
        if (filterConfigFileName != null && filterConfigFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(filterConfigFilePath, filterConfigFileName, "filter");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String[] key = entity.getKey().split("/");
                    String sectionName = key[0];
                    String entityName = key[1];
                    String subEntityName = key[2];
                    String listID = ParseConfigFile.getValueFromConfigFile(filterConfigFilePath, filterConfigFileName, sectionName, entityName + "/" + subEntityName);
                    String filters = entity.getValue();
                    allTestData.add(new Object[]{sectionName, entityName, subEntityName, listID, filters});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }
    @DataProvider
    public Object[][] dataProviderForExcelDownload() {
        List<Object[]> allTestData = new ArrayList<>();
        if (columnConfigFileName != null && columnConfigFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(columnConfigFilePath, columnConfigFileName, "excel");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String[] key = entity.getKey().split("/");
                    String sectionName = key[0];
                    String entityName = key[1];
                    String subEntityName = key[2];
                    String listID = ParseConfigFile.getValueFromConfigFile(columnConfigFilePath, columnConfigFileName, sectionName, entityName + "/" + subEntityName);
                    String filters = entity.getValue();
                    allTestData.add(new Object[]{sectionName, entityName, subEntityName, listID, filters});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForExcelDownload")
    public void testExcel(String sectionName, String entityName, String subEntityName, String listID, String columns) {
        logger.info("check column in {} {} tab", entityName, subEntityName);
        CustomAssert customAssert = new CustomAssert();
        try {
            List<String> allColumns = getAllColumnNameCheckInColumnSection(columns);
            if (sectionName.equalsIgnoreCase("report")) {
                excelReportDownload(entityName, subEntityName, listID, customAssert, allColumns);
            } else {
                excelListDownload(entityName, subEntityName, listID, customAssert, allColumns);
            }

        } catch (Exception e) {
            logger.error("Exception while verifying column present in column section {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying column present in column section");
        }
        customAssert.assertAll();
    }
    @Test(dataProvider = "dataProviderForColumnName")
    public void testColumnName(String sectionName, String entityName, String subEntityName, String listID, String columns) {
        logger.info("check column in {} {} tab", entityName, subEntityName);
        CustomAssert customAssert = new CustomAssert();
        try {
            List<String> allColumns = getAllColumnNameCheckInColumnSection(columns);
            String defaultUserListMetaDataResponse;
            switch (sectionName) {
                case "list":
                    ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
                    listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(Integer.parseInt(listID));
                    defaultUserListMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
                    break;
                case "report":
                    ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                    reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(listID));
                    defaultUserListMetaDataResponse = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + sectionName);
            }

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetaDataResponse)) {
                DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
                List<String> allColumnQueryNames = defaultUserListMetadataHelper.getAllColumnQueryNames(defaultUserListMetaDataResponse);

                for (String columnName : allColumns) {
                    if (!allColumnQueryNames.contains(columnName))
                        customAssert.assertTrue(false, "column {" + columnName + "} not found in column section for entity name {" + entityName + "} {" + subEntityName + "} tab");
                }

            }

        } catch (Exception e) {
            logger.error("Exception while verifying column present in column section {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying column present in column section");
        }
        customAssert.assertAll();
    }

    private void excelListDownload(String entityName, String subEntityName, String listID, CustomAssert customAssert, List<String> allColumns) {
        try {
            //download the report

            logger.info("excel file Download for listing");
            DownloadListWithData downloadListWithData = new DownloadListWithData();
            Map<String, String> formParam = new HashMap<>();
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            formParam.put("jsonData", "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
            formParam.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

            HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, Integer.parseInt(listID));
            String outputFilePath = "src/test/output";
            String outputFileName = (entityName + ".xlsx").replace("/", " ");
            FileUtils fileUtil = new FileUtils();
            Boolean fileDownloaded = fileUtil.writeResponseIntoFile(response, outputFilePath + "/" + outputFileName);
            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data for Entity [" + entityName + "] having Id " + entityTypeId);
            }
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
            for (String column : allColumns) {
                if (!allHeadersInExcelDataSheet.contains(column))
                    customAssert.assertTrue(false, "Column name " + column + " not found in excel listing download for " + entityName);
            }
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception While Verifying All Status  In Report excel" + entityName + " for " + subEntityName + ". " + e.getMessage());
        }

    }

    public void excelReportDownload(String entityName, String reportName, String reportId, CustomAssert customAssert, List<String> allColumns) {
        try {
            logger.info("report download for report ");
            Boolean isReportWithinDownloadLimit = new ReportsListHelper().isReportWithinDownloadLimit(Integer.parseInt(reportId));
            if (!isReportWithinDownloadLimit)
                throw new SkipException("We Can't Download Report Because limitExceeded Flag Max For Report {" + reportName + "} And Report Id{" + reportId + "}");

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
                customAssert.assertTrue(false,"Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
                throw new SkipException("Couldn't Download Data for Report [" + reportName + "] having Id " + reportId);
            }
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
            for (String column : allColumns) {
                if (!allHeadersInExcelDataSheet.contains(column))
                    customAssert.assertTrue(false, "Column name" + column + "not found in excel download Report for" + reportName);
            }
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception While Verifying All Status  In Report excel" + reportName + " and Report Id " + reportId + ". " + e.getMessage());
        }
    }

    private List<String> getAllColumnNameCheckInColumnSection(String columns) {

        List<String> allColumnsName = new ArrayList<>();
        String[] columnName = columns.split(",");
        for (String column : columnName) allColumnsName.add(column.toLowerCase().trim());
        return allColumnsName;
    }

    @Test(dataProvider = "dataProviderForFilterName")
    public void testFilterName(String sectionName, String entityName, String subEntityName, String listID, String filters) {

        logger.info("check filter in {} {} tab", entityName, subEntityName);
        List<String> allFilters = getAllFilterNameCheckInFilterSection(filters);
        CustomAssert customAssert = new CustomAssert();
        try {
            String defaultUserListMetaDataResponse;
            switch (sectionName) {
                case "list":
                    ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
                    listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(Integer.parseInt(listID));
                    defaultUserListMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
                    break;
                case "report":
                    ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                    reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(listID));
                    defaultUserListMetaDataResponse = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + sectionName);
            }

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetaDataResponse)) {
                DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
                List<String> allFilterQueryNames = getAllFilterQueryNames(defaultUserListMetaDataResponse);

                for (String filterName : allFilters) {
                    if (!allFilterQueryNames.contains(filterName))
                        customAssert.assertTrue(false, "filter {" + filterName + "} not found in filter section for entity name {" + entityName + "} {" + subEntityName + "} tab");
                }

            }

        } catch (Exception e) {
            logger.error("Exception while verifying filter present in filter section {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while verifying filter present in filter section");
        }
        customAssert.assertAll();
    }

    public List<String> getAllFilterQueryNames(String defaultUserListResponse) {
        List<String> allFilterMetadataNames = new ArrayList<>();

        if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");

            for (int i = 0; i < jsonArr.length(); i++) {
                allFilterMetadataNames.add(jsonArr.getJSONObject(i).getString("queryName").toLowerCase());
            }
        }

        return allFilterMetadataNames;
    }
    private List<String> getAllFilterNameCheckInFilterSection(String filters) {
        List<String> allFiltersName = new ArrayList<>();
        String[] filterNames = filters.split(",");
        for (String filter : filterNames) allFiltersName.add(filter.toLowerCase().trim());
        return allFiltersName;
    }

}
