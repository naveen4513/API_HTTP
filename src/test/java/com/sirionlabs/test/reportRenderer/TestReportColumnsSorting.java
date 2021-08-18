package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by shivashish on 25/9/17.
 */
public class TestReportColumnsSorting {


    private final static Logger logger = LoggerFactory.getLogger(TestReportColumnsSorting.class);
    static String reportColumnSortingConfigFilePath;
    static String reportColumnSortingConfigFileName;
    static int size;
    static int offset;
    static String defaultSize;
    static String orderDirectionDesc;
    static String orderDirectionAsc;
    String reportRendererConfigFilePath;
    String reportRendererConfigFileName;
    String baseFilePath;
    String entityIdMappingFileName;
    String entityIdConfigFilePath;
    String dateFormat;
    String testFiltersRandomData;
    String filtersRandomDataSize;

    List<String> allEntitySection;
    List<String> skipReportIdsList = new ArrayList<>();
    List<String> trendReportIdsList = new ArrayList<>();
    String testForAllColumnsGlobal;

    @BeforeClass
    @Parameters({"MultiLingual", "TestForAllColumns"})
    public void beforeClass(String multiLingual, String testForAllColumns) throws IOException, ConfigurationException {
        logger.info("In Before Class method");
        getReportRenderConfigData();
        new PostgreSQLJDBC().updateDBEntry("update client SET multilanguage_supported = '" + multiLingual + "' where id = 1002");
        testForAllColumnsGlobal = testForAllColumns;
    }

    public void getReportRenderConfigData() throws ParseException, ConfigurationException {
        logger.debug("Initializing Test Data for Report Render");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
        reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");

        dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");

        testFiltersRandomData = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "testrandondata");
        filtersRandomDataSize = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "randomcount");

        // reportColumn Sortin Config File Properties
        reportColumnSortingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
        reportColumnSortingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportColumnSortingConfigFileName");
        allEntitySection = ParseConfigFile.getAllSectionNames(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName);

        String skipReportIds = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, "skipreportids");
        skipReportIdsList = Arrays.asList(skipReportIds.trim().split(Pattern.quote(",")));

        size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName,
                "size"));
        offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName,
                "offset"));
        defaultSize = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName,
                "defaultsize");
        orderDirectionDesc = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName,
                "orderdirectiondesc");
        orderDirectionAsc = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName,
                "orderdirectionasc");
        String trendReportIds = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, "trendreportids");
        //trendReportIdsList = Arrays.asList(trendReportIds.trim().split(Pattern.quote(",")));

    }

    public Map<Integer, List<String>> getdataProviderMapIfConfigHasMoreEntries(JSONArray reportJsonArrayForEntities) {
        logger.info("Creating Data Provider Map as Config has more entries than API Response");
        Map<Integer, List<String>> dataProvidermap = new HashMap<>();
        for (String entityNameInConfig : allEntitySection) {
            int entityIdInConfig = ConfigureConstantFields.getEntityIdByName(entityNameInConfig);
            for (int count = 0; count < reportJsonArrayForEntities.length(); count++) {
                JSONObject obj = reportJsonArrayForEntities.getJSONObject(count);
                JSONUtility jsonUtilObj = new JSONUtility(obj);
                int entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
                if (entityTypeId == entityIdInConfig) {
                    logger.debug("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityNameInConfig, entityTypeId);
                    JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
                    int sizeOfSubReports = subReportsJsonArray.length();
                    List<String> reportsIdsArray = new ArrayList<>();
                    for (int sc = 0; sc < sizeOfSubReports; sc++) {
                        JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
                        JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
                        String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
                        String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
                        if (isManualReport.equalsIgnoreCase("false")) {
                            reportsIdsArray.add(reportId);
                        }
                    }
                    dataProvidermap.put(entityTypeId, reportsIdsArray);
                }
            }
        }
        return dataProvidermap;
    }

    public Map<Integer, List<String>> getdataProviderMapIfAPIResponseHasMoreEntries(JSONArray reportJsonArrayForEntities) {
        logger.info("Creating Data Provider Map as API Response has more entries than Config");
        Map<Integer, List<String>> dataProvidermap = new HashMap<>();
        for (int rc = 0; rc < reportJsonArrayForEntities.length(); rc++) {
            JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
            JSONUtility jsonUtilObj = new JSONUtility(obj);
            Integer entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
            String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
            if (entityName != null) {
                if (allEntitySection.contains(entityName.toLowerCase())) {
                    logger.info("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityName, entityTypeId);
                    JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
                    int sizeOfSubReports = subReportsJsonArray.length();
                    List<String> reportsIdsArray = new ArrayList<>();
                    for (int sc = 0; sc < sizeOfSubReports; sc++) {
                        JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
                        JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
                        String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
                        String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
                        if (isManualReport.equalsIgnoreCase("false")) {
                            reportsIdsArray.add(reportId);
                        }
                    }
                    dataProvidermap.put(entityTypeId, reportsIdsArray);
                } else {
                    logger.warn("The Entity Name [ {} ] is not available in the Config file.", entityName);
                }
            }
        }
        return dataProvidermap;
    }

    public Map<Integer, List<String>> getDefaultReportsList() {
        ReportRenderListReportJson reportRenderListReportJsonObj = new ReportRenderListReportJson();
        try {
            reportRenderListReportJsonObj.hitReportRender();
            String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
            logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
            JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
            int noOfEntitiesInReport = reportJsonArrayForEntities.length();
            logger.info("Number of Records [ {} ]", noOfEntitiesInReport);
            if (allEntitySection.size() > noOfEntitiesInReport) {
                logger.debug("The entries in Config are more than the API Response");
                return getdataProviderMapIfConfigHasMoreEntries(reportJsonArrayForEntities);
            } else {
                logger.debug("The entries in the API Response are more than Config");
                return getdataProviderMapIfAPIResponseHasMoreEntries(reportJsonArrayForEntities);
            }
        } catch (Exception e) {
            logger.error("Got Exception while creating Data Provider for All Reports , Cause : [ {} ] , Stack : [ {} ]", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
        }

        return null;
    }

    @DataProvider(name = "TestReportRendererData", parallel = true)
    public Object[][] getTestReportRendererData() throws ConfigurationException {

        logger.debug("In the Data Provider");

        String testReportsForAllEntitiesAllReports = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, "testreportsforallentitiesallreports");
        List<Object[]> allTestData = new ArrayList<>();

        if (testReportsForAllEntitiesAllReports.equalsIgnoreCase("true")) {
            Map<Integer, List<String>> allEntityWithReport = getDefaultReportsList();
            for (Map.Entry<Integer, List<String>> entityWithReport : allEntityWithReport.entrySet()) {
                Integer entity = entityWithReport.getKey();
                List<String> report = entityWithReport.getValue();
                for (String s : report) {
                    if (skipReportIdsList.contains(s)) {
                        logger.info("No need to test Sorting for this report : [ {} ] , as it is mentioned in Config file", s);
                        continue;
                    }
                    ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();

                    try {
                        reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(s));
                        String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                        if (!ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr)) {
                            allTestData.add(new Object[]{entity, s, -2});
                            continue;
                        }
                        JSONObject jsonObj = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
                        JSONUtility json = new JSONUtility(jsonObj);
                        if (jsonObj.has("body") && json.getStringJsonValue("body") == null) {
                            logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                            allTestData.add(new Object[]{entity, s, -2});
                            continue;
                        }
                        JSONObject reportMetadataJson = json.getJsonObject("reportMetadataJson");
                        boolean isListing = reportMetadataJson.getBoolean("isListing");
                        if (isListing) {
                            int getTotalRecordsOfEntity = getTotalRecordsCountForReportId(Integer.parseInt(s));
                            if (getTotalRecordsOfEntity == 0) {
                                continue;
                            }
                            allTestData.add(new Object[]{entity, s, getTotalRecordsOfEntity});
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            String testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, "testreportsforallentities");
            if (testReportsForAllEntities.equalsIgnoreCase("true")) {
                logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
            } else {
                String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, "entitiestotest").split(",");
                logger.debug("entitiesToTest :{} , entitiesToTest.size() : {}", entitiesToTest, entitiesToTest.length);
                allEntitySection = Arrays.asList(entitiesToTest);
            }
            for (String entitySection : allEntitySection) {
                logger.debug("entitySection :{}", entitySection);
                Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
                List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, entitySection);
                for (String entitySpecificProperty : allProperties) {
                    if (entitySpecificProperty.contentEquals("test")) {
                        String reportsIds = ParseConfigFile.getValueFromConfigFile(reportColumnSortingConfigFilePath, reportColumnSortingConfigFileName, entitySection, entitySpecificProperty);
                        logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIds);
                        String[] reportsIdsArray = reportsIds.split(",");
                        for (String s : reportsIdsArray) {
                            if (skipReportIdsList.contains(s)) {
                                logger.info("No need to test Sorting for this report : [ {} ] , as it is mentioned in Config file", s);
                                continue;
                            }
                            ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                            try {
                                reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(Integer.parseInt(s));
                                String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                                if (!ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr)) {
                                    allTestData.add(new Object[]{entityTypeId, s, -2});
                                    continue;
                                }
                                JSONObject jsonObj = new JSONObject(reportRendererDefaultUserListMetaDataJsonStr);
                                JSONUtility json = new JSONUtility(jsonObj);
                                if (jsonObj.has("body") && json.getStringJsonValue("body") == null) {
                                    logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                                    allTestData.add(new Object[]{entityTypeId, s, -2});
                                    continue;
                                }
                                JSONObject reportMetadataJson = json.getJsonObject("reportMetadataJson");
                                boolean isListing = reportMetadataJson.getBoolean("isListing");
                                if (isListing) {
                                    int getTotalRecordsOfEntity = getTotalRecordsCountForReportId(Integer.parseInt(s));
                                    if (getTotalRecordsOfEntity == 0) {
                                        continue;
                                    }
                                    allTestData.add(new Object[]{entityTypeId, s, getTotalRecordsOfEntity});
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            logger.debug("Formed the parameter Array to be tested");
        }
        return allTestData.toArray(new Object[0][]);
    }


    /* helper method of getting total number of Records from reportRenderer API Response and verify the API reportRendered API */
    public int getTotalRecordsCountForReportId(Integer reportId) {

        int numberOfRecords = -1;
        ReportRendererListData reportRendererListData = new ReportRendererListData();
        reportRendererListData.hitReportRendererListData(reportId, true);
        if (!reportRendererListData.getApiStatusCode().contains("200")) {
            logger.error("reportRendererListData Response Code is incorrect for reportId : {} ", reportId);
        } else {
            if (APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr())) {

                if (reportRendererListData.getFilteredCount() == -1) {
                    numberOfRecords = 0;
                    logger.warn("There is no record for reportId : {} ", reportId);
                } else {
                    numberOfRecords = reportRendererListData.getFilteredCount();
                }
            } else {
                logger.error("reportListData API Response is not valid Json for reportId :  " + reportId);

            }
        }
        return numberOfRecords;

    }

    /* helper method of getting total number of Pages*/
    public int getNumberOfPages(int numberOfRecords, int size){

        int numberOfPages;
        if (numberOfRecords % size == 0)
            numberOfPages = numberOfRecords / size;
        else
            numberOfPages = ((numberOfRecords / size) + 1);

        return numberOfPages;

    }

    @Test(dataProvider = "TestReportRendererData")
    public void testReportRenderData(Integer entityTypeId, String reportId, int getTotalRecordsOfEntity) {

        CustomAssert csAssertion = new CustomAssert();
        String entitySection = ConfigureConstantFields.getEntityNameById(entityTypeId);
        ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
        ReportRendererListData reportRendererListData = new ReportRendererListData();
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        boolean statusCodeVerification;
        boolean isResponseJson;
        boolean isTrendReport = false;
        List<Map<String, String>> allColumnQueryName;
        logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entitySection);

        if (skipReportIdsList.contains(reportId)) {
            logger.info("No need to test Sorting for this report : [ {} ] , as it is mentioned in Config file", reportId);
            return;
        }

        try {
            if (getTotalRecordsOfEntity == -2) {
                csAssertion.assertTrue(false, "ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
                throw new SkipException("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");
            }
            logger.info("###################################################:Tests Starting for ReportId:{}##################################################################", reportId);
            if (trendReportIdsList.contains(reportId)) {
                isTrendReport = true;
            }

            if (verifyReportRendererDefaultUserListMetaDataAPIResponse(Integer.parseInt(reportId.trim()), reportRendererDefaultUserListMetaData)) {
                allColumnQueryName = reportRendererDefaultUserListMetaData.getAllQueryName();
                if (allColumnQueryName.isEmpty()) {
                    csAssertion.assertTrue(false, "Error : Default User List Data API is failing not being able to get all the column name " + entitySection);
                }
                logger.info("allColumnQueryName is : [{}] --> size is [{}] ", allColumnQueryName, allColumnQueryName.size());

                if (getTotalRecordsOfEntity == -1) {
                    csAssertion.assertTrue(false, "Report Renderer API Response for Report Id  : " + reportId + " in not correct so skipping it");
                    throw new SkipException("Report Renderer API Response for Report Id  : " + reportId + " in not correct so skipping it");

                }
                int numberOfPages = getNumberOfPages(getTotalRecordsOfEntity, size);
                logger.debug("getTotalRecordsOfEntity is {}", getTotalRecordsOfEntity);
                logger.debug("numberOfPages is {}", numberOfPages);

                int entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
                logger.debug("entitySectionTypeId is :{}", entitySectionTypeId);
                if (allColumnQueryName.size() > 0) {
                    logger.debug("Total Column for ReportId : {} is : {}", reportId, allColumnQueryName.size());
                    int columnsChecked = 0;
                    int columnsNumberTobeChecked = 3;
                    for (Map<String, String> columnQueryNameHashMap : allColumnQueryName) {
                        columnsChecked = columnsChecked + 1;

                        if (testForAllColumnsGlobal.equalsIgnoreCase("false")) {
                            if (columnsChecked == columnsNumberTobeChecked) {
                                break;
                            }
                        }

                        if (columnQueryNameHashMap.get("isSortable").toLowerCase().contentEquals("true") &&
                                !columnQueryNameHashMap.get("queryName").toLowerCase().contains("dyn")) {
                            logger.debug("columnQueryNameHashMap is : {}", columnQueryNameHashMap);
                            logger.debug("Sorting Verification Started for Column name : {} ", columnQueryNameHashMap.get("name"));
                            if (testForAllColumnsGlobal.equalsIgnoreCase("false")) {
                                if (numberOfPages > 2) {
                                    numberOfPages = 1;
                                }
                               }
                            else {
                                if (numberOfPages >5) {
                                    numberOfPages = 4;
                                }
                            }
                            for (int i = 0; i < numberOfPages; i++) {
                                int offsetForAPICall = i * size;
                                logger.info("Sorting Verification Started for Column name : {} , and offset : {} ", columnQueryNameHashMap.get("name"), offsetForAPICall);

                                // Sorting Column by Asc Order
                                reportRendererListData.hitReportRendererListData(entitySectionTypeId, offsetForAPICall, size, columnQueryNameHashMap.get("queryName"), orderDirectionAsc, Integer.parseInt(reportId));
                                statusCodeVerification = reportRendererListData.getApiStatusCode().contains("200");
                                isResponseJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());


                                if (!statusCodeVerification) {
                                    logger.error("reportRendererListData API Response Code is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
                                    csAssertion.assertTrue(false, "reportRendererListData API Response Code for Entity: " + entitySection + " is incorrect " + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction " + " for report Id " + reportId);
                                    break;
                                } else if (!isResponseJson) {
                                    logger.error("reportRendererListData API Response is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
                                    csAssertion.assertTrue(false, "reportRendererListData API Response for Entity: " + entitySection + " is not valid Json" + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction " + " for report Id " + reportId);
                                  break;
                                } else {
                                    logger.debug("reportRendererListData API Response Code and Response is proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
                                    List<String> allRecords = reportRendererListData.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")));
                                    logger.debug("All Records List is : --> {}", allRecords);
                                    if (allRecords.isEmpty()) {
                                        csAssertion.assertTrue(false, "RECORDS EMPTY For Entity::" + entitySection + " While Sorting with Column::" + columnQueryNameHashMap.get("name") + " In Asc Direction ");
                                    }
                                    boolean isRecordsSorted = reportRendererListData.isRecordsSortedProperly(allRecords, columnQueryNameHashMap.get("type"), columnQueryNameHashMap.get("name"), orderDirectionAsc, isTrendReport, postgreSQLJDBC);
                                    if (!isRecordsSorted) {
                                        csAssertion.assertTrue(false, "Records are not Properly Sorted even after applying sorting (asc nulls first) for column " + columnQueryNameHashMap.get("queryName") + " for report Id " + reportId);
                                       break;
                                    }
                                }
                                // Sorting Column by Desc Order
                                reportRendererListData.hitReportRendererListData(entitySectionTypeId, offsetForAPICall, size, columnQueryNameHashMap.get("queryName"), orderDirectionDesc, Integer.parseInt(reportId));
                                statusCodeVerification = reportRendererListData.getApiStatusCode().contains("200");
                                isResponseJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());

                                if (!statusCodeVerification) {
                                    logger.error("reportRendererListData API Response Code is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
                                    csAssertion.assertTrue(false, "reportRendererListData API Response Code for Entity: " + entitySection + " is incorrect " + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction " + " for report Id " + reportId);
                                    break;
                                } else if (!isResponseJson) {
                                    logger.error("reportRendererListData API Response is not proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
                                    csAssertion.assertTrue(false, "reportRendererListData API Response for Entity: " + entitySection + " is not valid Json" + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction " + " for report Id " + reportId);
                                   break;
                                } else {
                                    logger.debug("reportRendererListData API Response Code and Response is proper for Entity: " + entitySection + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
                                    List<String> allRecords = reportRendererListData.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")));
                                    if (allRecords.isEmpty()) {
                                        csAssertion.assertTrue(false, "RECORDS EMPTY For Entity::" + entitySection + " While Sorting with Column::" + columnQueryNameHashMap.get("name") + " In Desc Direction ");
                                    }
                                    logger.debug("All Records List is : --> {}", allRecords);
                                    boolean isRecordsSorted = reportRendererListData.isRecordsSortedProperly(allRecords, columnQueryNameHashMap.get("type"), columnQueryNameHashMap.get("name"), orderDirectionDesc, isTrendReport, postgreSQLJDBC);
                                    if (!isRecordsSorted) {
                                        csAssertion.assertTrue(false, "Records are not Properly Sorted even after applying sorting (desc nulls first) for column " + columnQueryNameHashMap.get("queryName") + " for report Id " + reportId);
                                         break;
                                    }
                                }
                                logger.debug("Sorting Verification Ended for Column name : {} ", columnQueryNameHashMap.get("name"));
                            }

                        }
                    }

                } else {
                    csAssertion.assertTrue(false, "Error: default User List Meta Data API Response for  : " + reportId + " doesn't have column node to get columns detail ");
                }

            } else {
                csAssertion.assertTrue(false, "Report Renderer Default User List MetaData API Response validated unsuccessfully");
            }
        } catch (Exception e) {
            logger.error("Got Exception while Testing the report : [ {} ], for entity : [ {} ], Cause : [ {} ], Exception : [ {} ]", reportId, entitySection, e.getMessage(), e.getStackTrace());
            csAssertion.assertTrue(false, "Got Exception while Testing the report Cause " + e.getMessage() + e.toString());
            e.printStackTrace();
        }
//		}

        logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entitySection);
        csAssertion.assertAll();
    }


    //Verification of List Rederer Default List Meta Data API Response
    public boolean verifyReportRendererDefaultUserListMetaDataAPIResponse(Integer reportId, ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData) throws Exception {

        CustomAssert csAssertion = new CustomAssert();
        HttpResponse response = reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportId);

        if (reportRendererDefaultUserListMetaData.getStatusCodeFrom(response).contains("200")) {

            if (reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr().contains("errorMessage")) {
                csAssertion.assertTrue(false, "reportRendererDefaultUserListMetaData Response is not correct for reportId : " + reportId + " It Contains Error Message");
                csAssertion.assertAll();
                return false;
            } else
                return true;


        } else {
            csAssertion.assertTrue(false, "reportRendererDefaultUserListMetaData Status Code is not correct for reportId : " + reportId);
            csAssertion.assertAll();
            return false;
        }
    }
}
