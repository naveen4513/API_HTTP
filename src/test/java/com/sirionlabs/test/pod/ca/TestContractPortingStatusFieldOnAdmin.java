package com.sirionlabs.test.pod.ca;


import com.google.common.collect.Ordering;
import com.sirionlabs.api.clientAdmin.report.ReportRendererListConfigure;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestContractPortingStatusFieldOnAdmin {
    private final static Logger logger = LoggerFactory.getLogger(TestContractPortingStatusFieldOnAdmin.class);
    private String configFilePath;
    private String configFileName;
    private int entityURLId;
    private String field = "Contract Porting Status";
    private String entityName = "Contract Draft Request";

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        entityURLId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id"));
    }

    @Test
    public void testC88887() {
        String endUserEntityName = "contract requests";
        CustomAssert customAssert = new CustomAssert();
        AdminHelper adminHelper = new AdminHelper();
        String reportType = "Tracker";
        try {
            if (adminHelper.loginWithClientAdminUser()) {
                int reportId = -1;
                if (adminHelper.loginWithEndUser()) {
                    reportId = getReportId(endUserEntityName, reportType, customAssert);
                    if (adminHelper.loginWithClientAdminUser()) {
                        boolean columnAndFilterAtReport = getColumnAndFilterStatus(getReportConfigResponse(reportId, customAssert), customAssert);
                        if (columnAndFilterAtReport) {
                            logger.info("Field for {} are available for Entity {} in Report {}", field, entityName, reportType);
                            //Sorting Part
                            if (adminHelper.loginWithEndUser()) {
                                logger.info("Logged In with the end user credentials.");
                                if(verifySorting(customAssert)){
                                    logger.info("Sorting is working fine.");
                                }else{
                                    logger.error("Sorting is not working fine");
                                    customAssert.assertTrue(false,"Sorting is not working fine");
                                }
                            } else {
                                logger.error("Could not log In with the end user credentials.");
                                customAssert.assertTrue(false, "Could not log In with the end user credentials.");
                            }
                        } else {
                            logger.info("Field for {} are not available for Entity {} in Report {}", field, entityName, reportType);
                            customAssert.assertTrue(false, "Field for " + field + " are not available for Entity " + entityName + " in Report " + reportType);
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
        }
        customAssert.assertAll();
    }

    public int getReportId(String entityName, String reportType, CustomAssert customAssert) {
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

    public boolean getColumnAndFilterStatus(String configureListResponse, CustomAssert customAssert) {
        boolean availabilityStatus = false;
        boolean filterDeleteStatus = true;
        boolean columnDeleteStatus = true;
        boolean excelDeleteStatus = true;
        try {
            JSONObject configureResponseJSON = new JSONObject(configureListResponse);

            JSONArray filterMetadatasArray = configureResponseJSON.getJSONArray("filterMetadatas");
            for (int index = 0; index < filterMetadatasArray.length(); index++) {
                String filterName = filterMetadatasArray.getJSONObject(index).getString("name");
                if (filterName.equalsIgnoreCase(field)) {
                    filterDeleteStatus = filterMetadatasArray.getJSONObject(index).getBoolean("deleted");
                }
            }

            JSONArray columnsArray = configureResponseJSON.getJSONArray("columns");
            for (int index = 0; index < columnsArray.length(); index++) {
                String columnName = columnsArray.getJSONObject(index).getString("name");
                if (columnName.equalsIgnoreCase(field)) {
                    columnDeleteStatus = columnsArray.getJSONObject(index).getBoolean("deleted");
                }
            }

            JSONArray excelArray = configureResponseJSON.getJSONArray("ecxelColumns");
            for (int index = 0; index < excelArray.length(); index++) {
                String columnName = excelArray.getJSONObject(index).getString("name");
                if (columnName.equalsIgnoreCase(field)) {
                    excelDeleteStatus = excelArray.getJSONObject(index).getBoolean("deleted");
                }
            }

            if (!filterDeleteStatus & !columnDeleteStatus & !excelDeleteStatus) {
                availabilityStatus = true;
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while checking the status of Column and Filter", e.getMessage());
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while checking the status of Column and Filter");
        }
        return availabilityStatus;
    }

    public boolean verifySorting(CustomAssert customAssert) {
        //String sortingPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":100,\"orderByColumnName\":\"document_movement_status\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"342\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1000\",\"name\":\"Russian (русский)\"}]},\"filterId\":342,\"filterName\":\"language\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":12259,\"columnQueryName\":\"id\"},{\"columnId\":12260,\"columnQueryName\":\"title\"},{\"columnId\":18041,\"columnQueryName\":\"document_movement_status\"}]}";
        ListRendererListData listRendererListData = new ListRendererListData();
        List<String> ascNullFirstSortedData = new ArrayList<>();
        List<String> descNullLastSortedData = new ArrayList<>();
        try {
            //Get AcsNullFirstSorted Data
            //listRendererListData.hitListRendererListDataV2amTrue(entityURLId, sortingPayload);
            String sortingResponse = listRendererListData.getListDataJsonStr();
            JSONArray data = null;
            String columnId = null;
            if (ParseJsonResponse.validJsonResponse(sortingResponse)) {
                data = new JSONObject(sortingResponse).getJSONArray("data");
                Set<String> keys = data.getJSONObject(0).keySet();
                for (String key : keys) {
                    if (data.getJSONObject(0).getJSONObject(key).getString("columnName").equalsIgnoreCase("document_movement_status")) {
                        columnId = key;
                        break;
                    }
                }
                for (int index = 0; index < data.length(); index++) {
                    try {
                        ascNullFirstSortedData.add(data.getJSONObject(index).getJSONObject(columnId).getString("value").toUpperCase().split(":;")[0]);
                    } catch (Exception e) {
                        ascNullFirstSortedData.add("A");
                    }
                }
            } else {
                logger.error("AscNullFirst Sorting Response is not a valid JSON");
                customAssert.assertTrue(false, "AscNullFirst Sorting Response is not a valid JSON");
            }
            //Get DescNullLastSorted Data
            String orderDirection = "desc nulls last";
            //JSONObject sortingPayloadJSON = new JSONObject(sortingPayload);
            //sortingPayloadJSON.getJSONObject("filterMap").put("orderDirection", orderDirection);
            //listRendererListData.hitListRendererListDataV2amTrue(entityURLId, sortingPayloadJSON.toString());
            sortingResponse = listRendererListData.getListDataJsonStr();
            if (ParseJsonResponse.validJsonResponse(sortingResponse)) {
                data = new JSONObject(sortingResponse).getJSONArray("data");
                Set<String> keys = data.getJSONObject(0).keySet();
                for (String key : keys) {
                    if (data.getJSONObject(0).getJSONObject(key).getString("columnName").equalsIgnoreCase("document_movement_status")) {
                        columnId = key;
                        break;
                    }
                }
                for (int index = 0; index < data.length(); index++) {
                    try {
                        descNullLastSortedData.add(data.getJSONObject(index).getJSONObject(columnId).getString("value").toUpperCase().split(":;")[0]);
                    } catch (Exception e) {
                        descNullLastSortedData.add("A");
                    }
                }
            } else {
                logger.error("DescNullLast Sorting Response is not a valid JSON");
                customAssert.assertTrue(false, "DescNullLast Sorting Response is not a valid JSON");
            }
            
            //Check if the lists are sorted
            boolean ascSorting = verifyAscSorting(ascNullFirstSortedData,ascNullFirstSortedData.size(),customAssert);
            boolean descSorting = verifyDescSorting(descNullLastSortedData,descNullLastSortedData.size(),customAssert);
            if(ascSorting & descSorting){
                return true;
            }else{
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception {} occurred while sorting the column {}", e.getMessage(), field);
            customAssert.assertTrue(false, "Exception " + e.getMessage() + " occurred while sorting the column " + field);
            return false;
        }
    }

    public boolean verifyAscSorting(List<String> listOfStrings, int index, CustomAssert customAssert){
        try{
            if (index < 2) {
                return true;
            } else if (listOfStrings.get(index - 2).compareTo(listOfStrings.get(index - 1)) > 0) {
                return false;
            } else {
                return verifyAscSorting(listOfStrings, index - 1,customAssert);
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while verifying the sorting of Asc Null First Sorted Data",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while verifying the sorting of Asc Null First Sorted Data");
            return false;
        }

    }

    public boolean verifyDescSorting(List<String> listOfStrings, int index, CustomAssert customAssert){
        try{
            if (index < 2) {
                return true;
            } else if (listOfStrings.get(index - 2).compareTo(listOfStrings.get(index - 1)) < 0) {
                return false;
            } else {
                return verifyDescSorting(listOfStrings, index - 1,customAssert);
            }
        }catch (Exception e){
            logger.error("Exception {} occurred while verifying the sorting of Desc Null Last Sorted Data",e.getMessage());
            customAssert.assertTrue(false,"Exception "+e.getMessage()+" occurred while verifying the sorting of Desc Null Last Sorted Data");
            return false;
        }

    }
}