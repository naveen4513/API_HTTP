package com.sirionlabs.test.filters;

import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
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
public class TestFilters {

    private final static Logger logger = LoggerFactory.getLogger(TestFilters.class);
    private String configFilePath = null;
    private String configFileName = null;
    private Boolean testAllFilters = true;
    private Boolean applyRandomization = false;
    private Integer maxFiltersToValidate = 3;
    private Integer maxNoOfFilterOptionsToValidate = 3;
    private Integer maxNoOfRecordsToValidate = 3;
    private Integer listDataSize = 20;
    private Integer listDataOffset = 0;
    private String defaultStartDate;
    private String defaultEndDate;
    private String defaultMinSliderValue;
    private String defaultMaxSliderValue;
    private Integer numOfMultipleFiltersToValidate;
    private String allEntitiesToTestStr;

    private ListRendererFilterData filterObj = new ListRendererFilterData();

    private Map<String, String> filterDataResponseMap = new HashMap<>();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("FiltersConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("FiltersConfigFileName");

        String entities = System.getProperty("Entities");

        if (entities != null && !entities.equalsIgnoreCase("")) {
            allEntitiesToTestStr = entities;
        }

        Map<String, String> defaultPropertiesMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "default");
        if (!defaultPropertiesMap.isEmpty()) {
            if (defaultPropertiesMap.containsKey("testallfilters") && defaultPropertiesMap.get("testallfilters").trim().equalsIgnoreCase("false"))
                testAllFilters = false;

            if (defaultPropertiesMap.containsKey("applyrandomization") && defaultPropertiesMap.get("applyrandomization").trim().equalsIgnoreCase("true"))
                applyRandomization = true;

            if (applyRandomization) {
                if (defaultPropertiesMap.containsKey("maxnooffilterstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxnooffilterstovalidate")))
                    maxFiltersToValidate = Integer.parseInt(defaultPropertiesMap.get("maxnooffilterstovalidate"));

                if (defaultPropertiesMap.containsKey("maxnooffilteroptionstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxnooffilteroptionstovalidate")))
                    maxNoOfFilterOptionsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxnooffilteroptionstovalidate"));

                if (defaultPropertiesMap.containsKey("maxrecordstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxrecordstovalidate")))
                    maxNoOfRecordsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxrecordstovalidate"));
            }

            if (defaultPropertiesMap.containsKey("offset") && NumberUtils.isParsable(defaultPropertiesMap.get("offset")))
                listDataOffset = Integer.parseInt(defaultPropertiesMap.get("offset"));

            if (defaultPropertiesMap.containsKey("size") && NumberUtils.isParsable(defaultPropertiesMap.get("size")))
                listDataSize = Integer.parseInt(defaultPropertiesMap.get("size"));

            defaultStartDate = defaultPropertiesMap.get("defaultstartdate");
            //defaultEndDate = defaultPropertiesMap.get("defaultenddate");;
            defaultEndDate= DateUtils.getCurrentDateInMM_DD_YYYY();
            defaultMinSliderValue = defaultPropertiesMap.get("defaultminslidervalue");
            defaultMaxSliderValue = defaultPropertiesMap.get("defaultmaxslidervalue");
            numOfMultipleFiltersToValidate = Integer.parseInt(defaultPropertiesMap.get("numofmultiplefilterstovalidate"));
        }
    }

    @DataProvider(parallel = true)
    public Object[][] dataProviderForFilters() {
        logger.info("Setting All Entities to Test Filters");
        List<Object[]> allTestData = new ArrayList<>();

        String[] allEntitiesToTestArr;

        if (allEntitiesToTestStr == null || allEntitiesToTestStr.trim().equalsIgnoreCase("")) {
            allEntitiesToTestArr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
                    "entitiesToTest").split(Pattern.quote(","));
        } else {
            allEntitiesToTestArr = allEntitiesToTestStr.split(Pattern.quote(","));
        }

        List<String> allEntitiesToTestList = new ArrayList<>(Arrays.asList(allEntitiesToTestArr));

        for (String entity : allEntitiesToTestList) {
            entity = entity.trim();
            List<String> filtersToTest = getFiltersToTest(entity);

            if (filtersToTest != null) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entity);

                for (String filter : filtersToTest) {
                    allTestData.add(new Object[]{entity, filter.trim(), entityTypeId});
                }
            } else {
                logger.error("Some Exception Occurred while Getting Filters to Test for Entity {}", entity);
            }
        }

        logger.info("All Entities To Test: " + allEntitiesToTestList);

        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForMultipleFilters() {
        logger.info("Setting All Entities to Test Filters");
        List<Object[]> allTestData = new ArrayList<>();

        String[] allEntitiesToTestArr;

        if (allEntitiesToTestStr == null || allEntitiesToTestStr.trim().equalsIgnoreCase("")) {
            allEntitiesToTestArr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
                    "entitiesToTest").split(Pattern.quote(","));
        } else {
            allEntitiesToTestArr = allEntitiesToTestStr.split(Pattern.quote(","));
        }

        List<String> entitiesToTest = new ArrayList<>(Arrays.asList(allEntitiesToTestArr));
        ArrayList<String> filterList = new ArrayList<>();
        for (String entity : entitiesToTest) {
            entity = entity.trim();
            List<String> filtersToTest = getFiltersToTest(entity);

            if (filtersToTest != null) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entity);
                int numberOfFiltersAddedInList = 0;
                for (String filter : filtersToTest) {
                    try {
                        Integer.parseInt(filter);
                        logger.info("Filter is of type numeric so not adding to list of multiple filters");
                        continue;

                    } catch (Exception e) {
                        logger.info("Filter is of type other than numeric so adding to list of multiple filters");
                    }

                    filterList.add(filter.trim());
                    numberOfFiltersAddedInList += 1;

                    if (numberOfFiltersAddedInList == numOfMultipleFiltersToValidate) {
                        allTestData.add(new Object[]{entity, filterList, entityTypeId});
                        numberOfFiltersAddedInList = 0;
                        filterList = new ArrayList<>();
                    }

                }
            } else {
                logger.error("Some Exception Occurred while Getting Filters to Test for Entity {}", entity);
            }
        }

        return allTestData.toArray(new Object[0][]);
    }

    private List<String> getFiltersToTest(String entityName) {
        List<String> filtersToTest = new ArrayList<>();

        try {
            if (testAllFilters) {
                logger.info("Setting All Filters to Test for Entity {}", entityName);
                logger.info("Hitting Filter Data Api for Entity {}", entityName);
                int listId = ConfigureConstantFields.getListIdForEntity(entityName);
                filterObj.hitListRendererFilterData(listId);
                String filterResponse = filterObj.getListRendererFilterDataJsonStr();

                filterDataResponseMap.put(entityName, filterResponse);

                if (ParseJsonResponse.validJsonResponse(filterResponse)) {
                    List<String> allFilterNames = ListRendererFilterDataHelper.getAllStaticFilterNames(filterResponse);

                    if (allFilterNames.size() > 0) {
                        logger.info("Total Filters found for Entity {}: {}", entityName, allFilterNames.size());
                        logger.info("Removing Filters (if any) that are to be Ignored for Entity {}.", entityName);

                        //Adding temporary code to exclude filters 'lastmodifiedby', 'createdby', 'createdDate', 'modifiedDate'
                        List<String> eligibleFilters = new ArrayList<>(allFilterNames);
                       // eligibleFilters.remove("lastmodifiedby");
                       // eligibleFilters.remove("createdby");
                       // eligibleFilters.remove("createdDate");
                       // eligibleFilters.remove("modifiedDate");
                        if (entityName.equalsIgnoreCase("governance body meetings"))
                            eligibleFilters.remove("status");

                        List<String> filtersToIgnoreList = getFiltersToIgnoreList(entityName);

                        if (filtersToIgnoreList != null) {
                            for (String filterToIgnore : filtersToIgnoreList) {
                                if (allFilterNames.contains(filterToIgnore))
                                    eligibleFilters.remove(filterToIgnore);
                            }
                        } else {
                            logger.error("Some Exception Occurred while Getting Filters to Ignore for Entity {}", entityName);
                        }

                        if (eligibleFilters.size() > 0) {
                            if (applyRandomization) {
                                logger.info("Maximum No of Filters to Validate: {}", maxFiltersToValidate);
                                int[] randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, eligibleFilters.size() - 1,
                                        maxFiltersToValidate);
                                for (int randomNumber : randomNumbersForFilters) {
                                    filtersToTest.add(eligibleFilters.get(randomNumber));
                                }
                            } else {
                                filtersToTest.addAll(eligibleFilters);
                            }
                        } else {
                            logger.error("No Eligible Filter found for Entity {}.", entityName);
                        }
                    } else {
                        logger.error("No Filter found for Entity {}.", entityName);
                    }
                }
            } else {
                Map<String, String> filtersToTestMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filters to test");
                if (!filtersToTestMap.isEmpty()) {
                    if (filtersToTestMap.containsKey(entityName.trim()) && !filtersToTestMap.get(entityName.trim()).trim().equalsIgnoreCase("")) {
                        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
                        filterObj.hitListRendererFilterData(listId);
                        String filterResponse = filterObj.getListRendererFilterDataJsonStr();
                        filterDataResponseMap.put(entityName, filterResponse);

                        String[] filtersToTestArr = filtersToTestMap.get(entityName.trim()).trim().split(Pattern.quote(","));

                        for (String filter : filtersToTestArr) {
                            filtersToTest.add(filter.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Filters to Test for Entity {}. {}", entityName, e.getStackTrace());
            return null;
        }
        return filtersToTest;
    }

    private List<String> getFiltersToIgnoreList(String entityName) {
        List<String> filtersToIgnore = new ArrayList<>();

        try {
            Map<String, String> filtersToIgnoreMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filters to ignore");

            if (!filtersToIgnoreMap.isEmpty()) {
                if (filtersToIgnoreMap.containsKey(entityName.trim()) && !filtersToIgnoreMap.get(entityName.trim()).trim().equalsIgnoreCase("")) {
                    String[] filtersToIgnoreArr = filtersToIgnoreMap.get(entityName.trim()).trim().split(Pattern.quote(","));

                    for (String filter : filtersToIgnoreArr) {
                        filtersToIgnore.add(filter.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Filters to Ignore List for Entity {}. {}", entityName, e.getStackTrace());
            return null;
        }
        return filtersToIgnore;
    }

    @Test(dataProvider = "dataProviderForFilters")
    public void testFilters(String entityName, String filterName, int entityTypeId) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("************************************************");
            logger.info("Hitting Filter Data API for Entity {} and Filter Name {}", entityName, filterName);
//            filterObj.hitListRendererFilterData(listId);
            String filterDataResponse = filterDataResponseMap.get(entityName);

            if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                logger.info("Verifying Filter: {} of Entity {}", filterName, entityName);
                logger.info("Getting Id for Filter: {} of Entity {}", filterName, entityName);
                Integer filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);

                if (filterId != -1) {
                    logger.info("Getting Show Page Object Name Mapping of Filter {} of Entity {}", filterName, entityName);
                    String showPageObjectName = getShowPageObjectNameMapping(entityName, filterName);

                    if (showPageObjectName != null && !showPageObjectName.trim().equalsIgnoreCase("")) {
                        if (filterName.equalsIgnoreCase("stakeholder")) {
                            ListRendererFilterDataHelper.validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset,
                                    listDataSize, defaultStartDate, defaultEndDate, defaultMinSliderValue, defaultMaxSliderValue, false,
                                    maxNoOfFilterOptionsToValidate, maxNoOfRecordsToValidate, showPageObjectName, csAssert);
                        } else {
                            ListRendererFilterDataHelper.validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset,
                                    listDataSize, defaultStartDate, defaultEndDate, defaultMinSliderValue, defaultMaxSliderValue, applyRandomization,
                                    maxNoOfFilterOptionsToValidate, maxNoOfRecordsToValidate, showPageObjectName, csAssert);
                        }
                    } else {
                        logger.warn("Couldn't get Show Page Object Name of Filter {} of Entity {}. Hence skipping test.", filterName, entityName);
                        throw new SkipException("Couldn't get Show Page Object Name of Filter " + filterName + " of Entity " + entityName + ". Hence skipping test.");
                    }
                } else {
                    throw new SkipException("Couldn't get Id for Filter " + filterName + " of Entity " + entityName + ". Hence skipping test.");
                }
            } else {
                String htmlReason = ParseJsonResponse.getHTMLResponseReason(filterDataResponse);
                csAssert.assertTrue(false, "Filter Data API Response for Entity " + entityName + " is an Invalid JSON. " + htmlReason);
            }
        } catch (SkipException e) {
            logger.info(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {} of Entity {}. {}", filterName, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(dataProvider = "dataProviderForMultipleFilters", enabled = false)
    public void testMultipleFilters(String entityName, List<String> filterNameList, int entityTypeId) {
        CustomAssert csAssert = new CustomAssert();

        Integer filterId;
        ArrayList<Integer> filterIdList = new ArrayList<>();
        ArrayList<String> showPageObjectNameList = new ArrayList<>();
        String showPageObjectName;
        try {
            logger.info("************************************************");
            logger.info("Hitting Filter Data API for Entity {} and Filter Name {}", entityName, filterNameList);
//            filterObj.hitListRendererFilterData(listId);
            String filterDataResponse = filterDataResponseMap.get(entityName);

            if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                logger.info("Verifying Filter: {} of Entity {}", filterNameList, entityName);
                logger.info("Getting Id for Filter: {} of Entity {}", filterNameList, entityName);

                for (String filterNameFromList : filterNameList) {
                    filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterNameFromList);

                    if (filterId != -1) {
                        logger.info("Getting Show Page Object Name Mapping of Filter {} of Entity {}", filterNameFromList, entityName);
                        filterIdList.add(filterId);
                        showPageObjectName = getShowPageObjectNameMapping(entityName, filterNameFromList);
                        showPageObjectNameList.add(showPageObjectName);
                    }
                }

                if (!showPageObjectNameList.contains(null) || filterNameList.contains("createdBy") || filterNameList.contains("lastmodifiedby") ||
                        filterNameList.contains("createdDate") || filterNameList.contains("modifiedDate")) {

                    List<String> expectedValueList = new ArrayList<>();

                    ListRendererFilterDataHelper.validateMultipleFilters(filterDataResponse, filterNameList, filterIdList, entityName,
                            entityTypeId, listDataOffset, listDataSize, expectedValueList, applyRandomization, 1, maxNoOfRecordsToValidate, showPageObjectNameList, defaultStartDate, defaultEndDate, defaultMinSliderValue, defaultMaxSliderValue, csAssert);

                } else {

                    logger.warn("Couldn't get Show Page Object Name of Filter {} of Entity {}. Hence skipping test.", filterNameList, entityName);
                    throw new SkipException("Couldn't get Show Page Object Name of Filter { " + filterNameList + " } of Entity { " + entityName + " }. Hence skipping test.");
                }
            } else {
                String htmlResponse = ParseJsonResponse.getHTMLResponseReason(filterDataResponse);
                csAssert.assertFalse(true, "FilterData API Response for Entity " + entityName + " is an invalid JSON. " + htmlResponse);
            }
        } catch (SkipException e) {
            logger.info(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {} of Entity {}. {}", filterNameList, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterNameList + " of Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();

    }

    private String getShowPageObjectNameMapping(String entityName, String filterName) {
        String showPageObjectName = null;

        try {
            if (ParseConfigFile.hasPropertyCaseSensitive(configFilePath, configFileName, entityName.trim() + " filter name show page object mapping", filterName)) {
                showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                        entityName.trim() + " filter name show page object mapping", filterName);
            } else if (ParseConfigFile.hasPropertyCaseSensitive(configFilePath, configFileName, "default filter name show page object mapping", filterName)) {
                showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                        "default filter name show page object mapping", filterName);
            } else {
                logger.info("Show Page Object Mapping not available for Filter {} of Entity {}", filterName, entityName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting Show Page Object Name Mapping of Filter {}. {}", filterName, e.getStackTrace());
        }
        return showPageObjectName;
    }
}