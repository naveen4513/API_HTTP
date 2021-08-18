package com.sirionlabs.test.reports;

import com.sirionlabs.api.clientSetup.reportRenderer.ReportRendererListJson;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestReportMetaDataOnClientSetupAdmin {
    private final static Logger logger = LoggerFactory.getLogger(TestReportMetaDataOnClientSetupAdmin.class);
    private String configFilePath;
    private String configFileName;
    private Integer clientId;

    @BeforeClass
    public void beforeClass() {
        logger.info("read config file path and config file name");

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportClientSetupAdminConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportClientSetupAdminConfigFileName");

        logger.info("config file path {}", configFilePath);
        logger.info("config file name {}", configFileName);

        clientId = new AdminHelper().getClientId();
        logger.info("client id {}", clientId);
    }

    @DataProvider
    public Object[][] dataProviderColumnsData() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "column");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String reportName = entity.getKey().trim().toLowerCase();
                    int  listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", reportName));
                    String[] Columns = entity.getValue().split(",");
                    List<String> expectedColumnsName = new ArrayList<>();
                    for (String column : Columns) expectedColumnsName.add(column.toLowerCase().trim());
                    allTestData.add(new Object[]{reportName, listId, expectedColumnsName});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderFiltersData() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filter");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String reportName = entity.getKey().trim().toLowerCase();
                    int  listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", reportName));
                    String[] filters = entity.getValue().split(",");
                    List<String> expectedFiltersName = new ArrayList<>();
                    for (String column : filters) expectedFiltersName.add(column.toLowerCase().trim());
                    allTestData.add(new Object[]{reportName, listId, expectedFiltersName});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider
    public Object[][] dataProviderExcelColumnsData() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "excelcolumn");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String reportName = entity.getKey().trim().toLowerCase();
                    int  listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", reportName));
                    String[] excelColumns = entity.getValue().split(",");
                    List<String> expectedExcelColumnsName = new ArrayList<>();
                    for (String column : excelColumns) expectedExcelColumnsName.add(column.toLowerCase().trim());
                    allTestData.add(new Object[]{reportName, listId, expectedExcelColumnsName});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderColumnsData")
    public void testColumn(String reportName, int reportId, List<String> expectedColumns) {
        logger.info("Report Name {}", reportName);
        logger.info("Report Id {}", reportId);
        logger.info("Expected Column to verify {}", expectedColumns);
        List<String> actualColumnsName = new ArrayList<>();
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("Hitting reportRenderer/list/{reportId}/listJson?clientId API");
            String listJsonResponse = ReportRendererListJson.getListJsonResponse(reportId, clientId);
            if (ParseJsonResponse.validJsonResponse(listJsonResponse)) {
                JSONArray filterMetadata = new JSONObject(listJsonResponse).getJSONArray("columns");
                for (int i = 0; i < filterMetadata.length(); i++) {
                    actualColumnsName.add(filterMetadata.getJSONObject(i).getString("queryName").trim().toLowerCase());
                }
                logger.info("actual column name {}",actualColumnsName);
                for (String column : expectedColumns) {
                    if (!actualColumnsName.contains(column))
                        customAssert.assertTrue(false, "Column Name { " + column + " } not found in report { " + reportName + " }");
                }
            } else {
                logger.error("Invalid JSon Response {}", listJsonResponse);
                throw new Exception();
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
            logger.error("Exception while verifying column in report {}", reportName);
        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderFiltersData")
    public void testFilter(String reportName, int reportId, List<String> expectedFilters) {
        logger.info("Report Name {}", reportName);
        logger.info("Report Id {}", reportId);
        logger.info("Expected filter to verify {}", expectedFilters);
        List<String> actualFiltersName = new ArrayList<>();
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("Hitting reportRenderer/list/{reportId}/listJson?clientId API");
            String listJsonResponse = ReportRendererListJson.getListJsonResponse(reportId, clientId);
            if (ParseJsonResponse.validJsonResponse(listJsonResponse)) {
                JSONArray filterMetadata = new JSONObject(listJsonResponse).getJSONArray("filterMetadatas");
                for (int i = 0; i < filterMetadata.length(); i++) {
                    actualFiltersName.add(filterMetadata.getJSONObject(i).getString("queryName").trim().toLowerCase());
                }
                logger.info("actual filter name {}",actualFiltersName);
                for (String column : expectedFilters) {
                    if (!actualFiltersName.contains(column))
                        customAssert.assertTrue(false, "filter Name { " + column + " } not found in report { " + reportName + " }");
                }
            } else {
                logger.error("Invalid JSon Response {}", listJsonResponse);
                throw new Exception();
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
            logger.error("Exception while verifying filter in report {}", reportName);
        }
        customAssert.assertAll();

    }

    @Test(dataProvider = "dataProviderExcelColumnsData")
    public void testExcelColumn(String reportName, int reportId, List<String> expectedExcelColumns) {
        logger.info("Report Name {}", reportName);
        logger.info("Report Id {}", reportId);
        logger.info("Expected Excel Column to verify {}", expectedExcelColumns);
        List<String> actualExcelColumnsName = new ArrayList<>();
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("Hitting reportRenderer/list/{reportId}/listJson?clientId API");
            String listJsonResponse = ReportRendererListJson.getListJsonResponse(reportId, clientId);
            if (ParseJsonResponse.validJsonResponse(listJsonResponse)) {
                JSONArray filterMetadata = new JSONObject(listJsonResponse).getJSONArray("ecxelColumns");
                for (int i = 0; i < filterMetadata.length(); i++) {
                    actualExcelColumnsName.add(filterMetadata.getJSONObject(i).getString("queryName").trim().toLowerCase());
                }
                logger.info("actual excel column name {}",actualExcelColumnsName);
                for (String column : expectedExcelColumns) {
                    if (!actualExcelColumnsName.contains(column))
                        customAssert.assertTrue(false, "Excel Column Name { " + column + " } not found in report { " + reportName + " }");
                }
            } else {
                logger.error("Invalid JSon Response {}", listJsonResponse);
                throw new Exception();
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
            logger.error("Exception while verifying Excel column in report {}", reportName);
        }
        customAssert.assertAll();
    }

    @DataProvider
    public Object[][] dataProviderForC89286() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filterc89286");
            if (!allConstantProperties.isEmpty()) {
                for (Map.Entry<String, String> entity : allConstantProperties.entrySet()) {
                    String reportName = entity.getKey().trim().toLowerCase();
                    int  listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", reportName));
                    String[] filters = entity.getValue().split(",");
                    List<String> expectedFiltersName = new ArrayList<>();
                    for (String column : filters) expectedFiltersName.add(column.toLowerCase().trim());
                    allTestData.add(new Object[]{reportName, listId, expectedFiltersName});
                }

            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForC89286")
    public void testC89286(String reportName, int reportId, List<String> expectedFilters)
    {
        logger.info("Report Name {}", reportName);
        logger.info("Report Id {}", reportId);
        logger.info("Expected filter to verify {}", expectedFilters);
        List<String> actualFiltersName = new ArrayList<>();
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("Hitting reportRenderer/list/{reportId}/listJson?clientId API");
            String listJsonResponse = ReportRendererListJson.getListJsonResponse(reportId, clientId);
            if (ParseJsonResponse.validJsonResponse(listJsonResponse)) {
                JSONArray filterMetadata = new JSONObject(listJsonResponse).getJSONArray("filterMetadatas");
                for (int i = 0; i < filterMetadata.length(); i++) {
                    actualFiltersName.add(filterMetadata.getJSONObject(i).getString("queryName").trim().toLowerCase());
                }
                logger.info("actual filter name {}",actualFiltersName);
                for (String column : expectedFilters) {
                    if (actualFiltersName.contains(column))
                        customAssert.assertTrue(false, "filter Name { " + column + " } found in report { " + reportName + " }");
                }
            } else {
                logger.error("Invalid JSon Response {}", listJsonResponse);
                throw new Exception();
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
            logger.error("Exception while verifying filter in report {}", reportName);
        }
        customAssert.assertAll();
    }
}
