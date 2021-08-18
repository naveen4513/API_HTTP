package com.sirionlabs.test.listRenderer;

import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.reportRenderer.FilterData;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by vijay.thakur on 7/6/2017.
 */
//@Listeners(value = MyTestListenerAdapter.class)
public class TestListRendererListData {

    private final static Logger logger = LoggerFactory.getLogger(TestListRendererListData.class);

    static int size;
    static int offset;
    static String listDataConfigFilePath;
    static String listDataConfigFileName;
    static String orderByColumnName;
    static String orderDirection;
    static String listDataCSVFilePath;
    static String listDataCSVFileName;
    static List<Integer> entitiesToSkip = new ArrayList<Integer>();
    String entityIdMappingFileName;
    String entityIdConfigFilePath;
    Boolean useRandomizationOnListingFilters = true;
    Integer maxRandomOptions = 3;
    String dateFormat;
    List<String> allEntitySection;
    DumpResultsIntoCSV dumpResultsObj;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws IOException {
        logger.info("In Before Class method");
        try {
            entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
            entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
            //allEntitySection = ParseConfigFile.getAllSectionNames(entityIdConfigFilePath, entityIdMappingFileName);
            dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");
            getListDataConfigData();

            //setting csv generation
            listDataCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("listDataCSVFilePath");
            listDataCSVFileName = ConfigureConstantFields.getConstantFieldsProperty("listDataCSVFileName");
            dumpResultsObj = new DumpResultsIntoCSV(listDataCSVFilePath, listDataCSVFileName, setHeadersInCSVFile());

        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }

    public void getListDataConfigData() throws ParseException, IOException, ConfigurationException {
        logger.info("Getting Test Data for listData api");
        TestListRendererListData.listDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFilePath");
        TestListRendererListData.listDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListDataConfigFileName");
        TestListRendererListData.size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                "size"));
        TestListRendererListData.offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                "offset"));
        TestListRendererListData.orderByColumnName = ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                "orderByColumnName");
        TestListRendererListData.orderDirection = ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                "orderDirection");
        allEntitySection = Arrays.asList((ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                "allentitytotest")).split(","));
        useRandomizationOnListingFilters = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "useRandomizationOnListingFilters"));
        maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "maxfilteroptionstobevalidated"));
        entitiesToSkip = this.getEntityIdsToSkip("entitiestoskip");

    }

    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size() - entitiesToSkip.size()][];

        for (String entitySection : allEntitySection) {

            Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
            if (entitiesToSkip.contains(entitySectionTypeId)) {
                continue;
            }
            groupArray[i] = new Object[2];
            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            i++;
        }

        return groupArray;
    }

    @Test(groups = "regression", dataProvider = "getAllEntitySection", enabled = true)
    public void verifyListDataResponseForAllPages(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        try {
            int actualTotalCount = 0;
            logger.info("**************************** validating entity {} ***********************", entitySection);
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                    "offset"));
            // Iterate over each entitySection
            do {
                Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));
                String listDataJsonStr = listDataResponse(offset, entityTypeId, urlId);

                logger.info("List Data Response : offset={} , entity={} , response={}", offset, entitySection, listDataJsonStr);
                //Assertion for valid JSON response,permission denied and application error
                boolean isListDataValidJson = APIUtils.validJsonResponse(listDataJsonStr, "[list data response]");
                csAssertion.assertTrue(isListDataValidJson, "Response is not a valid JSON for entity {}" + entitySection);
                if (isListDataValidJson) {
                    boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataJsonStr);
                    boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataJsonStr);
                    csAssertion.assertFalse(isListDataApplicationError, "Application error found while hitting API = listData  for entity = " + entitySection);
                    csAssertion.assertFalse(isListDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                    if (isListDataApplicationError || isListDataPermissionDenied) {
                        logger.error("Error in list data response for entity = {} and offset = {}. response ={}", entitySection, offset, listDataJsonStr);
                    } else {
                        JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
                        int noOfRecords = listDataResponseObj.getJSONArray("data").length();
                        actualTotalCount = listDataResponseObj.getInt("filteredCount");
                        logger.info("total count = {}", actualTotalCount);
                        // Assertion for number of records on subsequent pages
                        csAssertion.assertTrue(validateAssertion(noOfRecords, actualTotalCount), "Number of records fetched is incorrect. offset =" + offset + ", totalRecords=" + actualTotalCount + ",size=" + size);
                    }
                } else {
                    logger.error(" list data response is not valid json for entity = {} and offset = {}. response = {}", entitySection, offset, listDataJsonStr);
                }
                offset = offset + size;
            } while (offset < actualTotalCount);


        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestListRendererListData Exception\n" + errors.toString());
        }

        csAssertion.assertAll();
    }

	@Test(groups = "smoke", dataProvider = "getAllEntitySection", enabled = true, timeOut = 1000000)
	public void verifyListDataResponseForFirstPage(String entitySection, Integer entityTypeId) {
		CustomAssert csAssertion = new CustomAssert();
		Map<String, String> listDataReportMap = new HashMap<>();
		try {
			int actualTotalCount = 0;
			// Iterate over each entitySection
			logger.info("***************************** validating entity {} ******************************************", entitySection);
			offset = 0;
			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            listDataReportMap.put("EntityId", entityTypeId.toString());
            listDataReportMap.put("EntityName", entitySection);
            listDataReportMap.put("APIName", "ListData[firstPage]");

//            String listDataJsonStr = listDataResponse(offset, entityTypeId, urlId);
            String listDataPayload = "";
            String columnId =ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath,listDataConfigFileName,entitySection,"columnid");
            String columnName = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath,listDataConfigFileName,entitySection,"columnname");

            if(columnId == null){
                listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
                        "\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}},\"selectedColumns\":[]}";
            }else {
                String columnPayload = "{\"columnId\": " + columnId + ",\"columnQueryName\": \"" + columnName + "\"}";
                listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
                        "\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}},\"selectedColumns\":[" + columnPayload + "]}";
            }



//            String listDataPayload = "{\"filterMap\":{}}";
            logger.info("Hitting ListRendererListData");
            ListRendererListData listDataObj = new ListRendererListData();
            listDataReportMap.put("Payload", listDataPayload);
            listDataObj.hitListRendererListDataV2(urlId, listDataPayload);
            String apiResponseTime = listDataObj.getApiResponseTime();
            listDataReportMap.put("ResponseTime(sec)", apiResponseTime);
            String listDataJsonStr = listDataObj.getListDataJsonStr();
            logger.info("List Data Response : offset={} , entity={} ", offset, entitySection);
            //Assertion for valid JSON response,permission denied and application error
            Boolean isListDataValidJson = APIUtils.validJsonResponse(listDataJsonStr, "[list data response]");

            csAssertion.assertTrue(isListDataValidJson, "Response is not a valid JSON for entity {}" + entitySection);
            listDataReportMap.put("isApiResponseValidJson", isListDataValidJson.toString());

            if (isListDataValidJson) {

                Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataJsonStr);
                Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataJsonStr);

                listDataReportMap.put("isApplicationError", isListDataApplicationError.toString());
                listDataReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

                csAssertion.assertFalse(isListDataApplicationError, "Application error found while hitting API = listData  for entity = " + entitySection);
                csAssertion.assertFalse(isListDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                if (isListDataApplicationError || isListDataPermissionDenied) {
                    logger.error("Error in list data response for entity = {} and offset = {}. response ={}", entitySection, offset, listDataJsonStr);
                    listDataReportMap.put("TestStatus", "Failed");
                    dumpResultsIntoCSV(listDataReportMap);
                } else {
                    listDataReportMap.put("TestStatus", "Passed");
                    dumpResultsIntoCSV(listDataReportMap);

                    JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
                    int noOfRecords = listDataResponseObj.getJSONArray("data").length();
                    actualTotalCount = listDataResponseObj.getInt("filteredCount");
                    logger.info("total count = {} ", actualTotalCount);
                    // Assertion for number of records on subsequent pages
                    csAssertion.assertTrue(validateAssertion(noOfRecords, actualTotalCount), "Number of records fetched is incorrect. offset =" + offset + ", totalRecords=" + actualTotalCount + ",size=" + size);
                }
            } else {
                logger.error("list data response is not valid json for entity = {} and offset = {}. response ={}", entitySection, offset, listDataJsonStr);
                listDataReportMap.put("TestStatus", "Failed");

				FileUtils.saveResponseInFile(entitySection + " listDataFirstPage", listDataJsonStr);
                dumpResultsIntoCSV(listDataReportMap);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestListRendererListData Exception\n" + errors.toString());
        }
        csAssertion.assertAll();

    }

    @Test(groups = {"smoke", "regression"}, dataProvider = "getAllEntitySection", enabled = true, timeOut = 1000000)
    public void verifyListDataResponseForFirstCall(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        Map<String, String> listDataReportMap = new HashMap<>();
        try {
            int actualTotalCount = 0;
            // Iterate over each entitySection
            logger.info("***************************** validating entity {} ******************************************", entitySection);
            offset = 0;
            Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            listDataReportMap.put("EntityId", entityTypeId.toString());
            listDataReportMap.put("EntityName", entitySection);
            listDataReportMap.put("APIName", "ListData[firstCall]");

            ListRendererListData listDataObj = new ListRendererListData();
            //for the first call empty payload is passed
            String payload = "{\"filterMap\":{}}";
            listDataReportMap.put("Payload", payload);
            listDataObj.hitListRendererListDataV2isFirstCall(urlId, payload);
            String apiResponseTime = listDataObj.getApiResponseTime();
            listDataReportMap.put("ResponseTime(sec)", apiResponseTime);
            String listDataJsonStr = listDataObj.getListDataJsonStr();

            logger.info("List Data Response : offset={} , entity={} ", offset, entitySection);
            //Assertion for valid JSON response,permission denied and application error
            Boolean isListDataValidJson = APIUtils.validJsonResponse(listDataJsonStr, "[list data response]");
            listDataReportMap.put("isApiResponseValidJson", isListDataValidJson.toString());
            csAssertion.assertTrue(isListDataValidJson, "Response is not a valid JSON for entity {}" + entitySection);
            if (isListDataValidJson) {
                Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataJsonStr);
                Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataJsonStr);

                listDataReportMap.put("isApplicationError", isListDataApplicationError.toString());
                listDataReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

                csAssertion.assertFalse(isListDataApplicationError, "Application error found while hitting API = listData  for entity = " + entitySection);
                csAssertion.assertFalse(isListDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                if (isListDataApplicationError || isListDataPermissionDenied) {
                    logger.error("Error in list data response for entity = {} and offset = {}. response ={}", entitySection, offset, listDataJsonStr);
                    listDataReportMap.put("TestStatus", "Failed");
                    dumpResultsIntoCSV(listDataReportMap);
                } else {
                    listDataReportMap.put("TestStatus", "Passed");
                    dumpResultsIntoCSV(listDataReportMap);

                    JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
                    int noOfRecords = listDataResponseObj.getJSONArray("data").length();
                    actualTotalCount = listDataResponseObj.getInt("filteredCount");
                    logger.info("total count = {} ", actualTotalCount);
                    // Assertion for first call. API should return the no. of records of default size
                    int defaultSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestListRendererListData.listDataConfigFilePath, TestListRendererListData.listDataConfigFileName,
                            "defaultsize"));
                    if (noOfRecords == defaultSize || noOfRecords == actualTotalCount)
                        logger.info("First Call validation passed for entity ={}", entitySection);
                    else
                        csAssertion.assertTrue(false, "Number of records fetched is incorrect for first call. recordsFetched =" + noOfRecords + "offset =" + offset + ", totalRecords=" + actualTotalCount + ",size(default)=" + size);
                }
            } else {
                logger.error("List Data response is not valid json for entity = {} and offset = {}. response ={}", entitySection, offset, listDataJsonStr);
                listDataReportMap.put("TestStatus", "Failed");
				FileUtils.saveResponseInFile(entitySection + " listDataFirstCall", listDataJsonStr);

                dumpResultsIntoCSV(listDataReportMap);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestListRendererListData Exception\n" + errors.toString());
        }
        csAssertion.assertAll();
    }

    @Test(groups = {"regression"}, dataProvider = "getAllEntitySection", enabled = false)
    public void verifyListDataResponseWithFilters(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        Map<String, String> listDataWithFiltersReportMap = new HashMap<>();
        try {
            listDataWithFiltersReportMap.put("EntityId", entityTypeId.toString());
            listDataWithFiltersReportMap.put("EntityName", entitySection);
            listDataWithFiltersReportMap.put("APIName", "ListData[Filters Validation]");

            Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
            metaDataObj.hitListRendererDefaultUserListMetadata(urlId);
            String defaultMetadata = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();
            FilterUtils filterUtils = new FilterUtils();
            Map<Integer, Map<String, String>> filters = filterUtils.getFilters(defaultMetadata);

            ListRendererFilterData filterDataObj = new ListRendererFilterData();
            filterDataObj.hitListRendererFilterData(urlId);
            String filterDataJsonStr = filterDataObj.getListRendererFilterDataJsonStr();

            List<FilterData> filteredDataList = filterUtils.getFiltersData(filterDataJsonStr, urlId.toString(), entityTypeId.toString());


            //TC-97086:Verify that when invoice listing page, the supplier. Contract, function, service
            if (entitySection.toLowerCase().contentEquals("invoices")) {
                int count = 0;
                for (FilterData filterDataClass : filteredDataList) {
                    if (filterDataClass.getFilterName().toLowerCase().contentEquals("supplier")) {
                        count++;
                    }
                    if (filterDataClass.getFilterName().toLowerCase().contentEquals("contract")) {
                        count++;
                    }
                    if (filterDataClass.getFilterName().toLowerCase().contentEquals("functions")) {
                        count++;
                    }
                    if (filterDataClass.getFilterName().toLowerCase().contentEquals("services")) {
                        count++;
                    }

                }

                if (count == 4) {
                    logger.info("All the Fitlers mentioned in TC-97086 have been found ");
                } else {

                    csAssertion.assertTrue(false, "Error : expected Filters in TC-97086 are not there ");
                }

            }


            for (FilterData filterDataClass : filteredDataList) {
                if (filters.containsKey(filterDataClass.getFilterId())) {
                    String filterUiType = filters.get(filterDataClass.getFilterId()).get("uiType");
                    Integer filterId = filterDataClass.getFilterId();
                    String filterQueryName = filters.get(filterDataClass.getFilterId()).get("queryName");
                    logger.debug("The Filter UI Type is [ {} ]", filterUiType);
                    logger.debug("The Filter filterId is [ {} ]", filterId);
                    logger.debug("The filterQueryName is [ {} ]", filterQueryName);

                    listDataWithFiltersReportMap.put("FilterName", filterQueryName);

                    if (filterUiType.equalsIgnoreCase("STATUS") || filterUiType.equalsIgnoreCase("MULTISELECT")) {
                        boolean testResultForSTATUSFilter = testFilter(entitySection, entityTypeId, urlId, filterId, filterUiType, filterQueryName, filterDataClass, listDataWithFiltersReportMap);
                        csAssertion.assertTrue(testResultForSTATUSFilter, "Testing STATUS Filter failed ,for entityName : [" + entitySection + "] ,entityTypeId : [" + entityTypeId + "] ,filterQueryName : [" + filterQueryName + "] . \n\n");
                    } else if (filterUiType.equalsIgnoreCase("DATE")) {
                        boolean testResultForDATEFilter = testDATEFilter(entitySection, urlId, filterId, filterUiType, filterQueryName, listDataWithFiltersReportMap);
                        csAssertion.assertTrue(testResultForDATEFilter, "Testing DATE Filter failed , for entityName : [" + entitySection + "] ,entityTypeId : [" + entityTypeId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
                    } else if (filterUiType.equalsIgnoreCase("SLIDER")) {
                        boolean testResultForSLIDERFilter = testSLIDERFilter(entitySection, urlId, filterId, filterUiType, filterQueryName, listDataWithFiltersReportMap);
                        csAssertion.assertTrue(testResultForSLIDERFilter, "Testing SLIDER Filter failed , for entityName : [" + entitySection + "] ,entityTypeId : [" + entityTypeId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
                    } else if (filterUiType.equalsIgnoreCase("STAKEHOLDER")) {
                        boolean testResultForSTAKEHOLDERFilter = testSTAKEHOLDERFilter(entitySection, urlId, filterId, filterUiType, filterQueryName, filterDataClass, listDataWithFiltersReportMap);
                        csAssertion.assertTrue(testResultForSTAKEHOLDERFilter, "Testing STAKEHOLDER Filter failed ,for entityName : [" + entitySection + "] ,entityTypeId : [" + entityTypeId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
                    }
                } else {
                    logger.error("No FIlter Found in Metadata API Response for [ {} ] and , ID : [ {} ]", filterDataClass.getFilterName(), filterDataClass.getFilterId());
                }

            }
        } catch (Exception e) {
            logger.error("Exception occurred while verifying listData response when filter is applied. {}", e.getMessage());
            csAssertion.assertTrue(false, "Exception occurred while verifying listData response when filter is applied. " + e.getMessage());
        }
    }

    @Test(groups = {"smoke", "regression"}, dataProvider = "getAllEntitySection", enabled = true, timeOut = 1000000)
    public void testFilterDataResponse(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        Map<String, String> reportMap = new HashMap<>();

        try {
            // Iterate over each entitySection
            logger.info("***************************** validating filter data response for entity {} ******************************************", entitySection);
            Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            reportMap.put("EntityId", entityTypeId.toString());
            reportMap.put("EntityName", entitySection);
            reportMap.put("APIName", "FilterData");

            ListRendererFilterData filterDataObj = new ListRendererFilterData();
            filterDataObj.hitListRendererFilterData(urlId);
            String apiResponseTime = filterDataObj.getApiResponseTime();

            logger.info("filterData api response time for the entity = {} is {} milliSeconds", entitySection, apiResponseTime);
            reportMap.put("ResponseTime(sec)", apiResponseTime);

            String filterDataJsonStr = filterDataObj.getListRendererFilterDataJsonStr();

            //Assertion for valid JSON response,permission denied and application error
            Boolean isFilterDataValidJson = APIUtils.validJsonResponse(filterDataJsonStr, "[filter data response]");
            reportMap.put("isApiResponseValidJson", isFilterDataValidJson.toString());
            csAssertion.assertTrue(isFilterDataValidJson, "Response is not a valid JSON for entity {}" + entitySection);
            if (isFilterDataValidJson) {
                logger.info(" Filter Data Response for entity {} is valid json.", entitySection);
                Boolean isFilterDataApplicationError = APIUtils.isApplicationErrorInResponse(filterDataJsonStr);
                Boolean isFilterDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(filterDataJsonStr);

                reportMap.put("isApplicationError", isFilterDataApplicationError.toString());
                reportMap.put("isPermissionDenied", isFilterDataPermissionDenied.toString());

                csAssertion.assertFalse(isFilterDataApplicationError, "Application error found while hitting API = filterData  for entity = " + entitySection);
                csAssertion.assertFalse(isFilterDataPermissionDenied, "Permission Denied error found while hitting listData API  for entity = " + entitySection);
                if (isFilterDataApplicationError || isFilterDataPermissionDenied) {
                    logger.error("Error in filter data response for entity = {} , response ={}", entitySection, filterDataJsonStr);
                    reportMap.put("TestStatus", "failed");
                    dumpResultsIntoCSV(reportMap);
                } else {
                    reportMap.put("TestStatus", "passed");
                    dumpResultsIntoCSV(reportMap);
                }
            } else {
                logger.error("filter data response is not valid json for entity = {} ", entitySection);
                reportMap.put("TestStatus", "failed");
                dumpResultsIntoCSV(reportMap);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestFilterData Exception\n" + errors.toString());
            reportMap.put("TestStatus", "failed[Exception : " + e.getMessage());
            dumpResultsIntoCSV(reportMap);
        }
        csAssertion.assertAll();
    }

    @Test(groups = {"smoke", "regression"}, dataProvider = "getAllEntitySection", enabled = true, timeOut = 1000000)
    public void testMetaDataResponse(String entitySection, Integer entityTypeId) {
        CustomAssert csAssertion = new CustomAssert();
        Map<String, String> reportMap = new HashMap<>();
        try {
            // Iterate over each entitySection
            logger.info("***************************** validating defaultMetaData response for entity {} ******************************************", entitySection);
            Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            reportMap.put("EntityId", entityTypeId.toString());
            reportMap.put("EntityName", entitySection);
            reportMap.put("APIName", "MetaData");

            ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
            metaDataObj.hitListRendererDefaultUserListMetadata(urlId);
            String apiResponseTime = metaDataObj.getApiResponseTime();

            logger.info("metaData api response time for the entity = {} is {} millSeconds", entitySection, apiResponseTime);
            reportMap.put("ResponseTime(sec)", apiResponseTime);

            String metaDataJsonStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

            //Assertion for valid JSON response,permission denied and application error
            Boolean isMetaDataValidJson = APIUtils.validJsonResponse(metaDataJsonStr, "[meta data response]");
            csAssertion.assertTrue(isMetaDataValidJson, "Response is not a valid JSON for entity {}" + entitySection);
            reportMap.put("isApiResponseValidJson", isMetaDataValidJson.toString());

            if (isMetaDataValidJson) {
                logger.info(" meta Data Response for entity {} is valid json ", entitySection);
                Boolean isMetaDataApplicationError = APIUtils.isApplicationErrorInResponse(metaDataJsonStr);
                Boolean isMetaDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(metaDataJsonStr);

                reportMap.put("isApplicationError", isMetaDataApplicationError.toString());
                reportMap.put("isPermissionDenied", isMetaDataPermissionDenied.toString());

                csAssertion.assertFalse(isMetaDataApplicationError, "Application error found while hitting API = metaData  for entity = " + entitySection);
                csAssertion.assertFalse(isMetaDataPermissionDenied, "Permission Denied error found while hitting metaData API  for entity = " + entitySection);
                if (isMetaDataApplicationError || isMetaDataPermissionDenied) {
                    logger.error("Error in Meta data response for entity = {} , response ={}", entitySection, metaDataJsonStr);
                    reportMap.put("TestStatus", "failed");
                    dumpResultsIntoCSV(reportMap);
                } else {
                    reportMap.put("TestStatus", "passed");
                    dumpResultsIntoCSV(reportMap);
                }
            } else {
                logger.error("Meta data response is not valid json for entity = {} ", entitySection);
                reportMap.put("TestStatus", "failed");
                dumpResultsIntoCSV(reportMap);
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            csAssertion.assertTrue(false, "TestFilterData Exception\n" + errors.toString());
            reportMap.put("TestStatus", "failed[Exception : " + e.getMessage());
            dumpResultsIntoCSV(reportMap);
        }
        csAssertion.assertAll();

    }

    public String listDataResponse(int offset, int entityTypeId, int urlId) throws Exception {

        String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
                offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
                "\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";

        logger.info("Hitting ListRendererListData");
        ListRendererListData listDataObj = new ListRendererListData();
        HttpResponse response = listDataObj.hitListRendererListDataV2(urlId, listDataPayload);
        return listDataObj.getListDataJsonStr();

    }

    public boolean validateAssertion(int noOfRecords, int totalCount) {
        boolean result = false;
        if (size < totalCount) {
            if (offset <= totalCount) {
                if (offset <= totalCount - size && noOfRecords == size)
                    result = true;

                else if (noOfRecords == totalCount - offset)
                    result = true;
            }
        } else {
            if (noOfRecords == totalCount)
                result = true;
        }
        return result;
    }

    private boolean testFilter(String entityName, Integer entityTypeId, Integer urlId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass, Map<String, String> listDataReportMap) {
        boolean testFilterResponse = true;
        if (filterDataClass.getDataClassList().size() > 0) {

            List<FilterData.DataClass> dataClassList;
            if (useRandomizationOnListingFilters.toString().equalsIgnoreCase("true")) {
                dataClassList = getNRandomList(filterDataClass.getDataClassList(), Integer.parseInt(maxRandomOptions.toString()));
            } else {
                dataClassList = filterDataClass.getDataClassList();
            }

            for (FilterData.DataClass dataClassObj : dataClassList) {
                Map<String, String> listDataLocalReportMap = new HashMap<>(listDataReportMap);
                listDataLocalReportMap.put("FilterOption", dataClassObj.getDataValue());
                FilterUtils filterUtils = new FilterUtils();
                String listDataPayload = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
                listDataLocalReportMap.put("Payload", listDataPayload);
                logger.debug(" The  payload is [ {} ] for urlId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", listDataPayload, urlId, filterUiType, filterQueryName, filterId);
                ListRendererListData listDataObj = new ListRendererListData();
                listDataObj.hitListRendererListDataV2(urlId, listDataPayload);
                String apiResponseTime = listDataObj.getApiResponseTime();
                logger.debug(" The response is : [ {} ], for payload : [ {} ] of urlId : [ {} ]", listDataObj.getListDataJsonStr(), listDataPayload, urlId);
                Boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr());
                listDataLocalReportMap.put("isApiResponseValidJson", isResponseValidJson.toString());
                listDataLocalReportMap.put("ResponseTime(sec)", apiResponseTime);
                if (isResponseValidJson) {

                    Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataObj.getListDataJsonStr());
                    Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataObj.getListDataJsonStr());

                    listDataLocalReportMap.put("isApplicationError", isListDataApplicationError.toString());
                    listDataLocalReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

                    if (isListDataApplicationError || isListDataPermissionDenied) {
                        listDataLocalReportMap.put("TestStatus", "Failed");
                        dumpResultsIntoCSV(listDataLocalReportMap);
                    } else {
                        listDataLocalReportMap.put("TestStatus", "Passed");
                        dumpResultsIntoCSV(listDataLocalReportMap);
                    }
                } else {
                    testFilterResponse = false;
                    logger.error("The Json response is not a valid Json for entity [ {} ] , urlId [ {} ] , filterUiType [ {} ], filterQueryName : [ {} ],  payLoad [ {} ] \n\n", entityName, urlId, filterUiType, filterQueryName, listDataPayload);

                    listDataLocalReportMap.put("TestStatus", "Failed");
                    dumpResultsIntoCSV(listDataLocalReportMap);
                }
            }
        } else {
            logger.warn("The filterData List Size is 0 for Entity [ {} ], UrlId [ {} ] , filterName [ {} ]", entityName, urlId, filterQueryName);
        }
        logger.debug("The Response for Entity [ {} ], UrlId [ {} ] , filterName [ {} ] is [ {} ]", entityName, urlId, filterQueryName, testFilterResponse);
        return testFilterResponse;
    }

    private Map<String, String> getRandomMap(Map<String, String> originalMap, int randomDataCounts) {
        if (originalMap.size() <= randomDataCounts) {
            return originalMap;
        } else {
            Map<String, String> randomDataMap = new HashMap<>();
            int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, originalMap.keySet().size() - 1, randomDataCounts);
            List<Object> keySetList = Arrays.asList(originalMap.keySet().toArray());
            for (int i = 0; i < randomNumbers.length; i++) {
                String key = (String) keySetList.get(randomNumbers[i]);
                String value = originalMap.get(key);

                randomDataMap.put(key, value);

            }
            return randomDataMap;
        }
    }

    private List<FilterData.DataClass> getNRandomList(List<FilterData.DataClass> originalList, int randomDataCounts) {
        if (originalList.size() <= randomDataCounts) {
            return originalList;
        } else {
            List<FilterData.DataClass> copy = new ArrayList<>(originalList);
            Collections.shuffle(copy);
            return copy.subList(0, randomDataCounts);
        }
    }

    private boolean testDATEFilter(String entityName, Integer urlId, Integer filterId, String filterUiType, String filterQueryName, Map<String, String> listDataReportMap) {

        boolean testDATEFilterResponse = true;
        Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        String date_from = "";
        String date_to = "";
        String startDate = "";
        String endDate = "";
        try {
            String duedate_fromEntitySection = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, entityName, "date_from");
            if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
                date_from = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "date_from");
            } else {
                date_from = duedate_fromEntitySection;
            }

            String duedate_toEntitySection = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, entityName, "date_to");
            if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
                date_to = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "date_to");
            } else {
                date_to = duedate_toEntitySection;
            }


            String actualDate = DateUtils.getDateFromEpoch(System.currentTimeMillis(), dateFormat);
            startDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_from), dateFormat);
            endDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_to), dateFormat);

        } catch (Exception e) {
            logger.error("Got Exception while fetching Date Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Cause : [ {} ],  Exception is [ {} ]", entityName, urlId, filterQueryName, e.getMessage(), e.getStackTrace());
            e.printStackTrace();
            return false;
        }

        FilterData.DataClass dataClassObj = new FilterData.DataClass();
        dataClassObj.setDataName(startDate);
        dataClassObj.setDataValue(endDate);

        Map<String, String> listDataLocalReportMap = new HashMap<>(listDataReportMap);
        listDataLocalReportMap.put("FilterOption", dataClassObj.getDataName() + " To " + dataClassObj.getDataValue());

        FilterUtils filterUtils = new FilterUtils();
        String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
        listDataLocalReportMap.put("Payload", payLoad);
        logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
        ListRendererListData listDataObj = new ListRendererListData();
        listDataObj.hitListRendererListDataV2(urlId, payLoad);
        String apiResponseTime = listDataObj.getApiResponseTime();
        listDataLocalReportMap.put("ResponseTime(sec)", apiResponseTime);
        logger.debug(" The response is : [ {} ], for payload : [ {} ] of UrlId : [ {} ]", listDataObj.getListDataJsonStr(), payLoad, urlId);
        Boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr());
        listDataLocalReportMap.put("isApiResponseValidJson", isResponseValidJson.toString());

        if (isResponseValidJson) {

            Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataObj.getListDataJsonStr());
            Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataObj.getListDataJsonStr());

            listDataLocalReportMap.put("isApplicationError", isListDataApplicationError.toString());
            listDataLocalReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

            if (isListDataApplicationError || isListDataPermissionDenied) {
                listDataLocalReportMap.put("TestStatus", "Failed");
                dumpResultsIntoCSV(listDataLocalReportMap);
            } else {
                listDataLocalReportMap.put("TestStatus", "Passed");
                dumpResultsIntoCSV(listDataLocalReportMap);
            }
        } else {
            testDATEFilterResponse = false;
            logger.error("The DATE filter Json response is not a valid Json for entity [ {} ] , urlId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, urlId, filterQueryName, payLoad);

            listDataLocalReportMap.put("TestStatus", "Failed");
            dumpResultsIntoCSV(listDataLocalReportMap);
        }
        logger.debug("The Response for Entity [ {} ], UrlId [ {} ] , filterName [ {} ] is [ {} ]", entityName, urlId, filterQueryName, testDATEFilterResponse);
        return testDATEFilterResponse;
    }

    private boolean testSLIDERFilter(String entityName, Integer urlId, Integer filterId, String filterUiType, String filterQueryName, Map<String, String> listDataReportMap) {

        boolean testSLIDERFilterResponse = true;
        Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        String slider_min = "";
        String slider_max = "";
        try {
            String slider_min_fromEntitySection = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, entityName, "slider_min");
            if (slider_min_fromEntitySection == null || slider_min_fromEntitySection.equalsIgnoreCase("")) {
                slider_min = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "slider_min");
            } else {
                slider_min = slider_min_fromEntitySection;
            }

            String slider_max_toEntitySection = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, entityName, "slider_max");
            if (slider_max_toEntitySection == null || slider_max_toEntitySection.equalsIgnoreCase("")) {
                slider_max = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "slider_max");
            } else {
                slider_max = slider_max_toEntitySection;
            }
        } catch (Exception e) {
            logger.error("Got Exception while fetching Slider Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, urlId, filterQueryName, e.getMessage());
            e.printStackTrace();
            return false;
        }


        FilterData.DataClass dataClassObj = new FilterData.DataClass();
        dataClassObj.setDataName(slider_min);
        dataClassObj.setDataValue(slider_max);

        Map<String, String> listDataLocalReportMap = new HashMap<>(listDataReportMap);
        listDataLocalReportMap.put("FilterOption", dataClassObj.getDataName() + " To " + dataClassObj.getDataValue());

        FilterUtils filterUtils = new FilterUtils();
        String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
        listDataLocalReportMap.put("Payload", payLoad);
        logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
        ListRendererListData listDataObj = new ListRendererListData();
        listDataObj.hitListRendererListDataV2(urlId, payLoad);
        String apiResponseTime = listDataObj.getApiResponseTime();
        listDataLocalReportMap.put("ResponseTime(sec)", apiResponseTime);
        logger.debug(" The response is : [ {} ], for payload : [ {} ] of urlId : [ {} ]", listDataObj.getListDataJsonStr(), payLoad, urlId);
        Boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr());
        listDataLocalReportMap.put("isApiResponseValidJson", isResponseValidJson.toString());

        if (isResponseValidJson) {

            Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataObj.getListDataJsonStr());
            Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataObj.getListDataJsonStr());

            listDataLocalReportMap.put("isApplicationError", isListDataApplicationError.toString());
            listDataLocalReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

            if (isListDataApplicationError || isListDataPermissionDenied) {
                listDataLocalReportMap.put("TestStatus", "Failed");
                dumpResultsIntoCSV(listDataLocalReportMap);
            } else {
                listDataLocalReportMap.put("TestStatus", "Passed");
                dumpResultsIntoCSV(listDataLocalReportMap);
            }
        } else {
            testSLIDERFilterResponse = false;
            logger.error("The SLIDER filter Json response is not a valid Json for entity [ {} ] , urlId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, urlId, filterQueryName, payLoad);

            listDataLocalReportMap.put("TestStatus", "Failed");
            dumpResultsIntoCSV(listDataLocalReportMap);
        }

        logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, urlId, filterQueryName, testSLIDERFilterResponse);
        return testSLIDERFilterResponse;
    }

    private boolean testSTAKEHOLDERFilter(String entityName, Integer urlId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass, Map<String, String> listDataReportMap) {

        boolean testSTAKEHOLDERFilterResponse = true;
        Integer entityTypeId = -1;
        try {
            entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String stakeHolder_Config_Vaues = "";
            Map<String, List<String>> stakeGroupsMapFromConfig = new HashMap<>();
            String StakeHolderGroupsDelemeter = "";
            String StakeHolderOneGroupDelemeter = "";
            String StakeHolderGroupEntryID = "";

            try {
                stakeHolder_Config_Vaues = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, entityName, "stakeholders");
                StakeHolderGroupsDelemeter = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "stakeholdergroupsdelemeter");
                StakeHolderOneGroupDelemeter = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "stakeholderonegroupdelemeter");
                StakeHolderGroupEntryID = ParseConfigFile.getValueFromConfigFile(listDataConfigFilePath, listDataConfigFileName, "default", "stakeholdergroupentityid");

            } catch (Exception e) {
                logger.error("Got exception while getting config values for Entity : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityName, e.getMessage(), e.getStackTrace());
                e.printStackTrace();
                return false;
            }
            if ((stakeHolder_Config_Vaues == null || stakeHolder_Config_Vaues.equalsIgnoreCase(""))) {
                logger.debug("No Specification found in the TestReports.cfg file , testing for all Data");
                if (filterDataClass.getDataClassList().size() > 0) {

                    List<FilterData.DataClass> dataClassList;
                    if (useRandomizationOnListingFilters.toString().equalsIgnoreCase("true")) {
                        dataClassList = getNRandomList(filterDataClass.getDataClassList(), maxRandomOptions);
                    } else {
                        dataClassList = filterDataClass.getDataClassList();
                    }


                    for (FilterData.DataClass dataClassObj : dataClassList) {

                        Map<String, String> stakeHoldersMapToBetested;
                        if (useRandomizationOnListingFilters.toString().equalsIgnoreCase("true")) {
                            stakeHoldersMapToBetested = getRandomMap(dataClassObj.getMapOfData(), maxRandomOptions);
                        } else {
                            stakeHoldersMapToBetested = dataClassObj.getMapOfData();
                        }

                        for (Map.Entry<String, String> StakeHolderDataToBeSelected : stakeHoldersMapToBetested.entrySet()) {
                            Map<String, String> listDataLocalReportMap = new HashMap<>(listDataReportMap);
                            listDataLocalReportMap.put("FilterOption", StakeHolderDataToBeSelected.getValue());
                            FilterUtils filterUtils = new FilterUtils();
                            String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, StakeHolderDataToBeSelected.getKey());
                            listDataLocalReportMap.put("Payload", payLoad);
                            logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ] , filterQueryName : [ {} ]", payLoad, entityTypeId, filterQueryName);
                            ListRendererListData listDataObj = new ListRendererListData();
                            listDataObj.hitListRendererListDataV2(urlId, payLoad);
                            String apiResponseTime = listDataObj.getApiResponseTime();
                            listDataLocalReportMap.put("ResponseTime(sec)", apiResponseTime);
                            logger.debug(" The response is [ {} ] , for payload [ {} ] of urlId : [ {} ] ", listDataObj.getListDataJsonStr(), payLoad, urlId);
                            Boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr());
                            listDataLocalReportMap.put("isApiResponseValidJson", isResponseValidJson.toString());

                            if (isResponseValidJson) {

                                Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataObj.getListDataJsonStr());
                                Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataObj.getListDataJsonStr());

                                listDataLocalReportMap.put("isApplicationError", isListDataApplicationError.toString());
                                listDataLocalReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

                                if (isListDataApplicationError || isListDataPermissionDenied) {
                                    listDataLocalReportMap.put("TestStatus", "Failed");
                                    dumpResultsIntoCSV(listDataLocalReportMap);
                                } else {
                                    listDataLocalReportMap.put("TestStatus", "Passed");
                                    dumpResultsIntoCSV(listDataLocalReportMap);
                                }
                            } else {
                                testSTAKEHOLDERFilterResponse = false;
                                logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , urlId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, urlId, filterQueryName, payLoad);

                                listDataLocalReportMap.put("TestStatus", "Failed");
                                dumpResultsIntoCSV(listDataLocalReportMap);
                            }
                        }
                    }

                } else {
                    logger.warn("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, urlId, filterQueryName);
                }
            } else {
                String stakeGroups[] = stakeHolder_Config_Vaues.split(Pattern.quote(StakeHolderGroupsDelemeter.trim()));
                for (String group : stakeGroups) {
                    String groupEntry[] = group.split(Pattern.quote(StakeHolderOneGroupDelemeter.trim()));
                    String groupIDs[] = groupEntry[1].split(Pattern.quote(StakeHolderGroupEntryID.trim()));
                    List<String> groupIDsList = new ArrayList<>();
                    for (String id : groupIDs) {
                        groupIDsList.add(id);
                    }
                    stakeGroupsMapFromConfig.put(groupEntry[0], groupIDsList);
                }


                if (filterDataClass.getDataClassList().size() > 0) {

                    List<FilterData.DataClass> dataClassList;
                    if (useRandomizationOnListingFilters.toString().equalsIgnoreCase("true")) {
                        dataClassList = getNRandomList(filterDataClass.getDataClassList(), maxRandomOptions);
                    } else {
                        dataClassList = filterDataClass.getDataClassList();
                    }

                    for (FilterData.DataClass dataClassObj : dataClassList) {
                        if (stakeGroupsMapFromConfig.containsKey(dataClassObj.getDataName())) {
                            for (String stakeHolderGroupIDToBeSelected : stakeGroupsMapFromConfig.get(dataClassObj.getDataName())) {
                                Map<String, String> listDataLocalReportMap = new HashMap<>(listDataReportMap);
                                listDataLocalReportMap.put("FilterOption", stakeHolderGroupIDToBeSelected);
                                FilterUtils filterUtils = new FilterUtils();
                                String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, stakeHolderGroupIDToBeSelected);
                                listDataLocalReportMap.put("Payload", payLoad);
                                logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
                                ListRendererListData listDataObj = new ListRendererListData();
                                listDataObj.hitListRendererListDataV2(urlId, payLoad);
                                String apiResponseTime = listDataObj.getApiResponseTime();
                                listDataLocalReportMap.put("ResponseTime(sec)", apiResponseTime);
                                logger.debug(" The response is : [ {} ], for payload : [ {} ] of urlId : [ {} ]", listDataObj.getListDataJsonStr(), payLoad, urlId);
                                Boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr());

                                if (isResponseValidJson) {

                                    Boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(listDataObj.getListDataJsonStr());
                                    Boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(listDataObj.getListDataJsonStr());

                                    listDataLocalReportMap.put("isApplicationError", isListDataApplicationError.toString());
                                    listDataLocalReportMap.put("isPermissionDenied", isListDataPermissionDenied.toString());

                                    if (isListDataApplicationError || isListDataPermissionDenied) {
                                        listDataLocalReportMap.put("TestStatus", "Failed");
                                        dumpResultsIntoCSV(listDataLocalReportMap);
                                    } else {
                                        listDataLocalReportMap.put("TestStatus", "Passed");
                                        dumpResultsIntoCSV(listDataLocalReportMap);
                                    }
                                } else {
                                    testSTAKEHOLDERFilterResponse = false;
                                    logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , urlId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, urlId, filterQueryName, payLoad);

                                    listDataLocalReportMap.put("TestStatus", "Failed");
                                    dumpResultsIntoCSV(listDataLocalReportMap);
                                }
                            }
                        } else {
                            logger.debug("The Group ID is not available in Config File , [ {} ]", dataClassObj.getDataName());
                        }

                    }
                } else {
                    logger.warn("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, urlId, filterQueryName);
                }
            }
        } catch (Exception e) {
            logger.error("Got Exception while testing StakeHolder for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Cause : [ {} ], Exception is : [ {} ]", entityName, urlId, filterQueryName, e.getMessage(), e.getMessage());
            e.printStackTrace();
            return false;
        }
        logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, urlId, filterQueryName, testSTAKEHOLDERFilterResponse);
        return testSTAKEHOLDERFilterResponse;
    }

    private List<String> setHeadersInCSVFile() {
        List<String> headers = new ArrayList<String>();
        String allColumns[] = {"EntityId", "EntityName", "APIName", "FilterName", "FilterOption", "Payload", "isApiResponseValidJson", "isApplicationError", "isPermissionDenied",
                "ResponseTime(sec)", "TestStatus"};
        for (String columnName : allColumns)
            headers.add(columnName);
        return headers;
    }

    private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
        String allColumns[] = {"EntityId", "EntityName", "APIName", "FilterName", "FilterOption", "Payload", "isApiResponseValidJson", "isApplicationError", "isPermissionDenied",
                "ResponseTime(sec)", "TestStatus"};
        for (String column : allColumns) {
            if (!resultsMap.containsKey(column))
                resultsMap.put(column, "null");
        }
        dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
    }

    private List<Integer> getEntityIdsToSkip(String propertyName) throws ConfigurationException {
        String value = ParseConfigFile.getValueFromConfigFile(this.listDataConfigFilePath, this.listDataConfigFileName, propertyName);
        List<Integer> idList = new ArrayList<Integer>();

        if (!value.trim().equalsIgnoreCase("")) {
            String entityIds[] = ParseConfigFile.getValueFromConfigFile(this.listDataConfigFilePath, this.listDataConfigFileName, propertyName).split(",");

            for (int i = 0; i < entityIds.length; i++)
                idList.add(Integer.parseInt(entityIds[i].trim()));
        }
        return idList;
    }
}
