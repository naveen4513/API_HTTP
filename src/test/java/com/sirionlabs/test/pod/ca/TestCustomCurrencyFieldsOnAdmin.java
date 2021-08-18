package com.sirionlabs.test.pod.ca;


import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.listRenderer.ListRendererConfigure;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestCustomCurrencyFieldsOnAdmin {
    private final static Logger logger = LoggerFactory.getLogger(TestCustomCurrencyFieldsOnAdmin.class);
    private String configFilePath;
    private String configFileName;
    private String entityURLId;
    private String customField = "Automation Currency";

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
    }

    @DataProvider
    public Object[][] dataProviderQuickView() {
        List<Object[]> listOfArrays = new ArrayList<>();
        List<String> listOfEntities = new ArrayList<>();
        listOfEntities.add("contract draft request");
        listOfEntities.add("contracts");
        for (String entity : listOfEntities) {
            listOfArrays.add(new Object[]{entity});
        }
        return listOfArrays.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderQuickView")
    public void testC89992(String entityName) {
        String endUserEntityName = entityName;
        CustomAssert customAssert = new CustomAssert();
        AdminHelper adminHelper = new AdminHelper();
        boolean stepOne = false;
        String reportType = "Lead Time";
        try {
            if (adminHelper.loginWithClientAdminUser()) {
                String configResponse = getQuickViewConfigResponse(entityName, customAssert);
                boolean columnAndFilterAtQuickView = getColumnAndFilterStatus(configResponse, false, customAssert);
                if (columnAndFilterAtQuickView) {
                    stepOne = true;
                    logger.info("Column and Filter for {} are available for Entity {} in Listing", customField, entityName);
                } else {
                    logger.error("Column and Filter for {} are not available for Entity {} in Listing", customField, entityName);
                    customAssert.assertTrue(false, "Column and Filter for " + customField + " are not available for Entity " + entityName + " in Listing");
                }
                int reportId = -1;
                if (adminHelper.loginWithEndUser()) {
                    reportId = getReportId(endUserEntityName, reportType, customAssert);
                    if (adminHelper.loginWithClientAdminUser()) {
                        boolean columnAndFilterAtReport = getColumnAndFilterStatus(getReportConfigResponse(reportId, customAssert), true, customAssert);
                        if (columnAndFilterAtReport) {
                            logger.info("Column and Filter for {} are available for Entity {} in Report {}", customField, entityName, reportType);
                        } else {
                            logger.info("Column and Filter for {} are not available for Entity {} in Report {}", customField, entityName, reportType);
                            customAssert.assertTrue(false, "Column and Filter for " + customField + " are not available for Entity " + entityName + " in Report " + reportType);
                        }
                    }
                }
            } else {
                logger.error("Login attempt at Admin side is failed");
                customAssert.assertTrue(false, "Login attempt at Admin side is failed");
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while checking the filters and columns on Admin side", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while checking the filters and columns on Admin side");
        } finally {
            if (adminHelper.loginWithEndUser()) {
                logger.info("Logged In with the end user credentials.");
            } else {
                logger.error("Could not log In with the end user credentials.");
                customAssert.assertTrue(false, "Could not log In with the end user credentials.");
            }
        }
        customAssert.assertAll();
    }

    public String getQuickViewConfigResponse(String entityName, CustomAssert customAssert) {
        entityURLId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id");
        String getConfigResponse = null;
        try {
            ListRendererConfigure listRendererConfigure = new ListRendererConfigure();
            listRendererConfigure.hitListRendererConfigure(entityURLId, customAssert);
            getConfigResponse = listRendererConfigure.getListRendererConfigureJsonStr();
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching configure response", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching configure response");
        }
        return getConfigResponse;
    }

    public boolean getColumnAndFilterStatus(String configureListResponse, boolean reportCheck, CustomAssert customAssert) {
        boolean availabilityStatus = false;
        boolean filterDeleteStatus = true;
        boolean columnDeleteStatus = true;
        boolean excelDeleteStatus = true;
        try {
            JSONObject configureResponseJSON = new JSONObject(configureListResponse);

            JSONArray filterMetadatasArray = configureResponseJSON.getJSONArray("filterMetadatas");
            for (int index = 0; index < filterMetadatasArray.length(); index++) {
                String filterName = filterMetadatasArray.getJSONObject(index).getString("name");
                if (filterName.equalsIgnoreCase("Automation Currency")) {
                    filterDeleteStatus = filterMetadatasArray.getJSONObject(index).getBoolean("deleted");
                }
            }

            JSONArray columnsArray = configureResponseJSON.getJSONArray("columns");
            for (int index = 0; index < columnsArray.length(); index++) {
                String columnName = columnsArray.getJSONObject(index).getString("name");
                if (columnName.equalsIgnoreCase("Automation Currency")) {
                    columnDeleteStatus = columnsArray.getJSONObject(index).getBoolean("deleted");
                }
            }

            if (reportCheck) {
                JSONArray excelArray = configureResponseJSON.getJSONArray("ecxelColumns");
                for (int index = 0; index < excelArray.length(); index++) {
                    String columnName = excelArray.getJSONObject(index).getString("name");
                    if (columnName.equalsIgnoreCase("Automation Currency")) {
                        excelDeleteStatus = excelArray.getJSONObject(index).getBoolean("deleted");
                    }
                }
            }

            if (!reportCheck & !filterDeleteStatus & !columnDeleteStatus) {
                availabilityStatus = true;
            } else if (reportCheck & !filterDeleteStatus & !columnDeleteStatus & !excelDeleteStatus) {
                availabilityStatus = true;
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while checking the status of Column and Filter", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while checking the status of Column and Filter");
        }
        return availabilityStatus;
    }

    public int getReportId(String entityName, String reportType, CustomAssert customAssert) {
        if (entityName.equalsIgnoreCase("contract draft request")) {
            entityName = "Contract Requests";
        }
        int reportId = 0;
        try {
            ReportRendererListData reportRendererListData = new ReportRendererListData();
            String listingResponse = reportRendererListData.getReportListReportJSON("");
            if (listingResponse != null) {
                JSONArray jsonArray = new JSONArray(listingResponse);
                String listMetaDataJson = null;
                for (int index = 0; index < jsonArray.length(); index++) {
                    JSONObject job = jsonArray.getJSONObject(index);
                    if (entityName.equalsIgnoreCase(job.getString("name"))) {
                        listMetaDataJson = job.get("listMetaDataJsons").toString();
                        break;
                    }
                }
                JSONArray jsonArrayMetaData = new JSONArray(listMetaDataJson);
                for (int index = 0; index < jsonArrayMetaData.length(); index++) {
                    JSONObject job = jsonArrayMetaData.getJSONObject(index);
                    String entityReportName = job.getString("name");
                    if (entityReportName != null) {
                        if (entityReportName.contains(reportType)) {
                            reportId = job.getInt("id");
                            logger.info("Report for {} is {}", entityReportName, reportId);
                            break;
                        }
                    } else {
                        logger.error("Report Name of the entity is null");
                        customAssert.assertTrue(false, "Report Name of the entity is null");
                    }
                }
            } else {
                logger.error("Listing Response on Report Listing is null");
                customAssert.assertTrue(false, "Listing Response on Report Listing is null");
            }
        } catch (Exception e) {
            logger.error("Exception occurred while fetching ReportId from Report listing {}", e.getMessage());
            customAssert.assertTrue(false, "Exception occurred while fetching ReportId from Report listing " + e.getMessage());
        }
        return reportId;
    }

    public String getReportConfigResponse(int reportId, CustomAssert customAssert) {
        String reportConfigResponse = null;
        try {
            reportConfigResponse = new ReportRendererListConfigure().getReportListConfigureResponse(reportId);
        } catch (Exception e) {
            logger.error("Exception {} occurred while fetching Report Config Response", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while fetching Report Config Response");
        }
        return reportConfigResponse;
    }
}