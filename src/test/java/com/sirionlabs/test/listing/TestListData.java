package com.sirionlabs.test.listing;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestListData {

    private final static Logger logger = LoggerFactory.getLogger(TestListData.class);
    private String configFilePath = null;
    private String configFileName = null;
    private int listDataOffset = 0;
    private int listDataSize = 20;
    private int maxNoOfRecordsToValidate = 200;
    private String listDataExpectedDateFormat;

    private List<String> entitiesToTest = new ArrayList<>();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListDataTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ListDataTestConfigFileName");

        Map<String, String> defaultPropertiesMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "default");

        if (!defaultPropertiesMap.isEmpty()) {
            if (defaultPropertiesMap.containsKey("offset") && NumberUtils.isParsable(defaultPropertiesMap.get("offset")))
                listDataOffset = Integer.parseInt(defaultPropertiesMap.get("offset"));

            if (defaultPropertiesMap.containsKey("size") && NumberUtils.isParsable(defaultPropertiesMap.get("size")))
                listDataSize = Integer.parseInt(defaultPropertiesMap.get("size"));

            if (defaultPropertiesMap.containsKey("maxrecordstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxrecordstovalidate")))
                maxNoOfRecordsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxrecordstovalidate"));

            listDataExpectedDateFormat = defaultPropertiesMap.get("listdataexpecteddateformat");
        } else {
            logger.error("Couldn't get Default Properties from Config File [{}]", configFilePath + "/" + configFileName);
        }

        String[] allEntities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiesToTest").split(Pattern.quote(","));

        for(String entityName: allEntities) {
        	entitiesToTest.add(entityName.trim());
		}
    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForListingData() {
        logger.info("Setting All Entities to Test Listing Data");

        List<Object[]> allTestData = new ArrayList<>();

        for (String entity : entitiesToTest) {
            allTestData.add(new Object[]{entity});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForListingData")
    public void testListData(String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Listing for Entity {}", entityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName.trim());
            int listId = ConfigureConstantFields.getListIdForEntity(entityName.trim());

            //Validate Listing Records Data
            testListingRecordsData(entityName, entityTypeId, listId, csAssert);

        } catch (Exception e) {
            logger.error("Exception while Validating Listing Data for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Listing Data for Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void testListingRecordsData(String entityName, int entityTypeId, int listId, CustomAssert csAssert) {
        try {
            //Get Fields to Test for Entity
            List<String> fieldsToTest = getFieldsToTestForEntity(entityName);

            if (fieldsToTest.isEmpty()) {
                csAssert.assertTrue(false, "Couldn't get Fields to Test for Entity " + entityName);
            } else {
                Map<String, String> fieldsShowPageObjectMap = getFieldShowPageObjectMapForEntity(entityName, fieldsToTest);

                if (fieldsToTest.isEmpty() || fieldsShowPageObjectMap.isEmpty()) {
                    csAssert.assertTrue(false, "Couldn't get Fields to Test with Show Page Object Mapping for Entity " + entityName);
                } else {
                    String showPageExpectedDateFormat = getShowPageExpectedDateFormatForEntity(entityName);

                    ListDataHelper.verifyListingRecordsData(entityTypeId, listId, listDataOffset, listDataSize, maxNoOfRecordsToValidate, fieldsToTest, fieldsShowPageObjectMap,
                            showPageExpectedDateFormat, listDataExpectedDateFormat, csAssert);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating Listing Records Data for Entity {}. {}", entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Listing Records Data for Entity " + entityName + ". " + e.getMessage());
        }
    }

    private List<String> getFieldsToTestForEntity(String entityName) {
        String entityFieldsToTestMappingSectionName = "entity fields to test mapping";
        List<String> fieldsToTest = new ArrayList<>();

        try {
            logger.info("Getting Fields to Test for Entity {}", entityName);

            if (ParseConfigFile.hasProperty(configFilePath, configFileName, entityFieldsToTestMappingSectionName, entityName)) {
                String[] allFields = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityFieldsToTestMappingSectionName,
                        entityName).trim().split(Pattern.quote(","));

                for (String field : allFields) {
                    fieldsToTest.add(field.trim());
                }

                if (!fieldsToTest.contains("id")) {
                    fieldsToTest.add("id");
                }
            } else {
                logger.info("Couldn't get find Fields to Test Mapping for Entity {} in Config Section [{}]", entityName, entityFieldsToTestMappingSectionName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting List Data Fields to test. {}", e.getMessage());
        }
        return fieldsToTest;
    }

    private Map<String, String> getFieldShowPageObjectMapForEntity(String entityName, List<String> fieldsToTest) {
        String masterEntityShowPageObjectSectionMapName = "entity show page object section mapping";
        String defaultFieldsShowPageObjectMappingSectionName = "default fields show page object mapping section";
        Map<String, String> fieldShowPageObjectMap = new HashMap<>();

        try {
            logger.info("Creating Field and Show Page Object Mapping for Entity {}", entityName);

            List<String> allFieldsToTest = new ArrayList<>(fieldsToTest);

            String entityFieldsShowPageObjectMappingSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                    masterEntityShowPageObjectSectionMapName, entityName);

            if (entityFieldsShowPageObjectMappingSectionName == null || entityFieldsShowPageObjectMappingSectionName.trim().equalsIgnoreCase("")) {
                logger.warn("Couldn't find Mapping of Fields Show Page Object Section for Entity {}. Proceeding with Default Mapping", entityName);
                entityFieldsShowPageObjectMappingSectionName = defaultFieldsShowPageObjectMappingSectionName;
            }

            Map<String, String> entityShowPageObjectMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                    entityFieldsShowPageObjectMappingSectionName);
            Map<String, String> defaultShowPageObjectMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                    defaultFieldsShowPageObjectMappingSectionName);

            for (String field : allFieldsToTest) {
                if (entityShowPageObjectMap.containsKey(field.trim())) {
                    fieldShowPageObjectMap.put(field, entityShowPageObjectMap.get(field.trim()));
                } else if (defaultShowPageObjectMap.containsKey(field.trim())) {
                    logger.warn("Couldn't find Show Page Object Mapping for Field {} of Entity {} in Section [{}]. Hence Proceeding with Default Mapping.", field.trim(),
                            entityName, entityFieldsShowPageObjectMappingSectionName);
                    fieldShowPageObjectMap.put(field, defaultShowPageObjectMap.get(field.trim()));
                } else {
                    if (!field.trim().equalsIgnoreCase("sourceId")) {
                        logger.warn("Couldn't find Show Page Object Mapping for Field {} of Entity {}. Hence removing it.", field, entityName);
                        fieldsToTest.remove(field);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting List Data Fields and Show Page Object Map. {}", e.getMessage());
        }
        return fieldShowPageObjectMap;
    }

    private String getShowPageExpectedDateFormatForEntity(String entityName) {
        String showPageExpectedDateFormat = null;
        String showPageExpectedDateFormatMappingSectionName = "entity show page expected date format mapping";

        try {
            logger.info("Getting Show Page Expected Date Format for Entity {}", entityName);
            Map<String, String> allShowPageExpectedDateFormatsMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName,
                    showPageExpectedDateFormatMappingSectionName);

            if (!allShowPageExpectedDateFormatsMap.isEmpty()) {
                if (allShowPageExpectedDateFormatsMap.containsKey(entityName.trim())) {
                    showPageExpectedDateFormat = allShowPageExpectedDateFormatsMap.get(entityName.trim());
                } else {
                    logger.warn("Show Page Expected Date Format Mapping not available for Entity {} under Section [{}]. Hence considering Default Date Format", entityName,
                            showPageExpectedDateFormatMappingSectionName);
                    showPageExpectedDateFormat = allShowPageExpectedDateFormatsMap.get("default");
                }
            } else {
                logger.error("Couldn't get Show Page Expected Date Format Map from Config File [{}]", configFilePath + "/" + configFileName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Show Page Expected Date Format for Entity {}. {}", entityName, e.getStackTrace());
        }
        return showPageExpectedDateFormat;
    }

    /*
    Below test verifies No of Records on Listing Page for all Entities is same on UX 2.0 as on UX 1.0
     */
    @Test(dataProvider = "dataProviderForListingData")
    public void testListingDataCountOnUX2AndUX1(String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test: Verify that the count on listing page should be same on UX 2.0 as on UX 1.0 for Entity {}.", entityName);
            String listDataV1Response = ListDataHelper.getListDataResponse(entityName);
            int totalCountInV1Response = ListDataHelper.getTotalListDataCount(listDataV1Response);

            String listDataV2Response = ListDataHelper.getListDataResponseVersion2(entityName);
            int totalCountInV2Response = ListDataHelper.getTotalListDataCount(listDataV2Response);

            csAssert.assertTrue(totalCountInV1Response == totalCountInV2Response, "Entity " + entityName +
                    ". Total Records Count in List Data V1 Response: " + totalCountInV1Response + " and Total Records Count in List Data V2 Response: " +
                    totalCountInV2Response);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Listing Data Count on UX2 and UX1 for Entity " + entityName + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForListingData", enabled = false)
    public void testEntityListingPagination(String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Listing Pagination for Entity {}.", entityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":[]}";

            String listDataV2Response = ListDataHelper.getListDataResponseVersion2(entityName, payloadForListData, true);
            int[] limitArr = {20, 50, 200};

            int totalRecords = ListDataHelper.getFilteredListDataCount(listDataV2Response);

            if (totalRecords == -1) {
                throw new SkipException("Couldn't get Total No of Records in Listing of Entity " + entityName);
            }

            if (totalRecords == 0) {
                logger.info("No Record found in Listing of Entity {}", entityName);
                return;
            }

            logger.info("Total Records present for Entity {} are: {}", entityName, totalRecords);

            for (int size : limitArr) {
                int offset = 0;

                do {
                    logger.info("Hitting ListData API for Entity {} with Size: {} and Offset: {}", entityName, size, offset);

                    payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size +
                            ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}},\"selectedColumns\":[]}";

                    listDataV2Response = ListDataHelper.getListDataResponseVersion2(entityName, payloadForListData, true);
                    totalRecords = ListDataHelper.getFilteredListDataCount(listDataV2Response);

                    if (ParseJsonResponse.validJsonResponse(listDataV2Response)) {
                        JSONObject jsonObj = new JSONObject(listDataV2Response);
                        JSONArray jsonArr = jsonObj.getJSONArray("data");

                        int noOfData = jsonArr.length();
                        int expectedNoOfData = (size + offset) > totalRecords ? (totalRecords % size) : size;

                        if (expectedNoOfData != noOfData) {
                            csAssert.assertTrue(false, "Pagination failed for Listing of Entity " + entityName +
                                    ", size " + size + " and offset " + offset + ". Expected No of Records: " + expectedNoOfData + " and Actual No of Records: " + noOfData);
                        }
                    } else {
                        csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + ",size " + size + " and offset " +
                                offset + " is an Invalid JSON.");
                    }

                    offset += size;
                } while (offset <= totalRecords);
            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing Pagination for Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }
}