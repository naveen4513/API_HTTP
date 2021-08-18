package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsFilterHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AutoExtractionTestReportListFilters {

    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionTestReportListFilters.class);
    public static ReportsDefaultUserListMetadataHelper defaultUserListObj1 = new ReportsDefaultUserListMetadataHelper();
    private String configFilePath = null;
    private String configFileName = null;
    private Boolean testAllFilters = true;
    private Boolean applyRandomization = false;
    private Integer maxFiltersToValidate = 3;
    private Integer maxNoOfFilterOptionsToValidate = 3;
    private Integer maxNoOfRecordsToValidate = 3;
    private Integer listDataSize = 20;
    private Integer listDataOffset = 0;
    private String defaultMinSliderValue;
    private String defaultMaxSliderValue;
    private List<String> entitiesToTest = new ArrayList<>();

    private ReportsDefaultUserListMetadataHelper defaultUserListObj = new ReportsDefaultUserListMetadataHelper();
    private ReportsListHelper listHelperObj = new ReportsListHelper();
    private ReportRendererFilterData filterObj = new ReportRendererFilterData();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestReportFiltersConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestReportFiltersConfigFileName");

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

                defaultMinSliderValue = defaultPropertiesMap.get("defaultminslidervalue");
                defaultMaxSliderValue = defaultPropertiesMap.get("defaultmaxslidervalue");

                String entityName = defaultPropertiesMap.get("entitiestotest");
        }
    }

    @DataProvider(parallel = true)
    public Object[][] dataProviderForFilters() {
        logger.info("Setting All Reports to Test Filters");
        List<Object[]> allTestData = new ArrayList<>();
        String reportName="AutoExtraction - Tracker";
        int entityTypeId=316;
        int reportId=520;
        String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
        Map<String,Integer> allReportsListOfEntity=new HashMap<>();
        allReportsListOfEntity.put(entityName,reportId);

        Boolean isListingAvailable = defaultUserListObj.isReportListingAvailable(reportName, reportId);
        if (isListingAvailable == null) {
            allTestData.add(new Object[]{null, reportId, reportName, entityTypeId, entityName, isListingAvailable});
        }
        else if (isListingAvailable)
        {
            List<String> filtersToTest = getFiltersToTestOfReport(reportName, String.valueOf(reportId).trim());
            if (filtersToTest != null) {
                for (String filter : filtersToTest) {
                    allTestData.add(new Object[]{filter.trim(), reportId, reportName, entityTypeId, entityName, isListingAvailable});
                }
            } else {
                logger.error("Some Exception Occurred while Getting Filters to Test for Report Name [{}]", reportName);
            }
        }
        return allTestData.toArray(new Object[0][]);
    }

    private List<String> getFiltersToTestOfReport(String reportName, String reportId) {
        List<String> filtersToTest = new ArrayList<>();

        try {
            logger.info("Setting All Filters to Test for Report [{}] having Id {}", reportName, reportId);

            if (testAllFilters) {
                logger.info("Hitting Report Filter Data Api for Report [{}] having Id {}", reportName, reportId);
                filterObj.hitReportRendererFilterData(Integer.parseInt(reportId));
                String filterResponse = filterObj.getReportRendererFilterDataJsonStr();

                if (ParseJsonResponse.validJsonResponse(filterResponse)) {
                    List<String> allFilterNames = ListRendererFilterDataHelper.getAllStaticFilterNames(filterResponse);

                    if (allFilterNames.size() > 0) {
                        logger.info("Total Static Filters found for Report [{}]: {}", reportName, allFilterNames.size());
                        logger.info("Removing Filters (if any) that are to be Ignored for Report [{}] having Id {}.", reportName, reportId);

                        //Adding temporary code to exclude filters 'lastmodifiedby', 'createdby', 'createdDate', 'modifiedDate'
                        List<String> eligibleFilters = new ArrayList<>(allFilterNames);

                        List<String> filtersToIgnoreList = getFiltersToIgnoreList("Auto Extraction- Tracker", "520");

                        if (filtersToIgnoreList != null) {
                            for (String filterToIgnore : filtersToIgnoreList) {
                                if (allFilterNames.contains(filterToIgnore))
                                    eligibleFilters.remove(filterToIgnore);
                            }
                        } else {
                            logger.error("Some Exception Occurred while Getting Filters to Ignore for Report [{}] having Id {}", reportName, reportId);
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
                            logger.error("No Eligible Filter found for Report [{}] having Id {}.", reportName, reportId);
                        }
                    } else {
                        logger.error("No Filter found for Report [{}] having Id {}.", reportName, reportId);
                    }
                }
            } else {
                Map<String, String> filtersToTestMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filters to test");
                if (!filtersToTestMap.isEmpty()) {
                    if (filtersToTestMap.containsKey(reportId) &&
                            !filtersToTestMap.get(reportId).trim().equalsIgnoreCase("")) {
                        String[] filtersToTestArr = filtersToTestMap.get(reportId).trim().split(Pattern.quote(","));

                        for (String filter : filtersToTestArr) {
                            filtersToTest.add(filter.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Filters to Test for Report [{}] having Id {}. {}", reportName, reportId, e.getStackTrace());
            return null;
        }
        return filtersToTest;
    }

    @Test(dataProvider = "dataProviderForFilters")
    public void testReportFilters(String filterName, int reportId, String reportName, int entityTypeId, String entityName, Boolean isListingAvailable) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("************************************************");

            if (isListingAvailable == null) {
                csAssert.assertTrue(false, "Couldn't check if Listing is Available for Report [" + reportName +
                        "] or not. Check DefaultUserListMetadata API Response.");
                csAssert.assertAll();
            }

            logger.info("Hitting Filter Data API for Report [{}] having Id {} of Entity {} and Filter Name {}", reportName, reportId, entityName, filterName);
            filterObj.hitReportRendererFilterData(reportId);
            String filterDataResponse = filterObj.getReportRendererFilterDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                logger.info("Verifying Filter: {} of Report [{}] having Id {} of Entity {}", filterName, reportName, reportId, entityName);
                logger.info("Getting Id for Filter: {} of Report [{}] having Id {} of Entity {}", filterName, reportName, reportId, entityName);
                Integer filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);

                if (filterId != -1) {
                   logger.info("Getting Show Page Object Name Mapping of Filter {} of Entity {}", filterName, entityName);
                    String showPageObjectName = getShowPageObjectNameMapping(entityName, filterName);

                    if ((showPageObjectName != null && !showPageObjectName.trim().equalsIgnoreCase(""))) {

                        logger.info("Getting Type of Filter {} of Entity {}", filterName, entityName);
                        String filterType = ListRendererFilterDataHelper.getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();

                        switch (filterType) {
                            case "stakeholder":
                                ReportsFilterHelper.validateStakeHolderFilter(reportName, reportId, filterDataResponse, filterName, filterId, entityTypeId, listDataOffset,
                                        listDataSize, applyRandomization, maxNoOfFilterOptionsToValidate, maxNoOfRecordsToValidate, csAssert);
                                break;

                            case "slider":
                                ReportsFilterHelper.validateFiltersOfSliderType(reportName, reportId, filterName, filterId, showPageObjectName, entityTypeId, entityName,
                                        applyRandomization, maxNoOfRecordsToValidate, listDataOffset, listDataSize, defaultMinSliderValue, defaultMaxSliderValue,
                                        csAssert);
                                break;

                            case "inputtext":
                                String textValue=getValueForInputTextType(filterName);
                                ReportsFilterHelper.validateFiltersOfInputTextType(reportName, reportId, filterDataResponse, filterName, filterId, showPageObjectName,textValue,
                                        entityTypeId, entityName, applyRandomization, maxNoOfFilterOptionsToValidate, maxNoOfRecordsToValidate, listDataOffset, listDataSize, csAssert);
                                break;

                            default:
                                ReportsFilterHelper.validateFiltersOfSelectType(reportName, reportId, filterDataResponse, filterName, filterId, showPageObjectName,
                                        entityTypeId, entityName, applyRandomization, maxNoOfFilterOptionsToValidate, maxNoOfRecordsToValidate, listDataOffset, listDataSize,
                                        csAssert);

                        }
                    } else {
                        logger.warn("Couldn't get Show Page Object Name of Filter {} of Entity {}. Hence skipping test", filterName, entityName);
                        throw new SkipException("Couldn't get Show Page Object Name of Filter " + filterName + " of Entity " + entityName + ". Hence skipping test.");
                    }
                } else {
                    throw new SkipException("Couldn't get Id for Filter " + filterName + " of Entity " + entityName + ". Hence skipping test.");
                }
            } else {
                csAssert.assertTrue(false, "Filter Data API Response for Entity " + entityName + " is an Invalid JSON.");

                FileUtils.saveResponseInFile("Filter " + filterName + " EntityTypeId " + entityTypeId + " FilterData API HTML.txt", filterDataResponse);
            }
        } catch (SkipException e) {
            logger.warn(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {} of Report [{}] having Id {} of Entity {}. {}", filterName, reportName, reportId, entityName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Report [" + reportName + "] having Id " + reportId +
                    " of Entity " + entityName + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private String getValueForInputTextType(String filterName)
    {
        String textValue = null;
        try {
            if(ParseConfigFile.hasPropertyCaseSensitive(configFilePath, configFileName,"inputtext",filterName)) {
                textValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "inputtext", filterName);
            }
            else
            {
                logger.info("Text value is not available for Filter {} ", filterName);
            }
        }catch(Exception e)
        {
            logger.error("Exception while getting text Value for Filter {}. {}", filterName, e.getMessage());
        }
        return textValue;
    }

    //TC: C153030 Verify the AE Tracker listing is Successfully Downloading after applying Filter.

    public static void testDownloadReportData(String payload, String filterName)
    {
        logger.info("Start test: Download list data after applying filter");
        String outputFileName = "AE-Tracker Report with Filter.xlsx";
        String outputFilePath="src/test/output";
        int reportId=520;
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("Hitting report DefaultUserListMetadata API");
            String reportsDefaultUserListMetadata = defaultUserListObj1.hitDefaultUserListMetadataAPIForReportId(reportId);
            JSONObject reportsDefaultUserListMetadataJsonObj = new JSONObject(reportsDefaultUserListMetadata);
            String queryName = reportsDefaultUserListMetadataJsonObj.get("queryName").toString();
            csAssert.assertEquals(queryName, "autoExtraction.selectAllDocsTracker", "AE Tracker Report is nor present in report Section");
            logger.info("Downloading Auto Extraction Tracker Report Data after applying filter "+filterName);
            DownloadReportWithData downloadReportWithDataObj=new DownloadReportWithData();
            Map<String,String> formParam = new HashMap<>();
            formParam.put("_csrf_token","null");
            formParam.put("jsonData",payload);

            HttpResponse downloadResponse =  downloadReportWithDataObj.hitDownloadReportWithData(formParam,reportId);
            logger.info("Checking if Auto extraction tracker report is downloaded");
            Boolean downloadStatus = downloadReportWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath,outputFileName);
            if(!(downloadStatus == true)){
                csAssert.assertTrue(false,"Auto Extraction Report Download is unsuccessful");
            }
            else {
                logger.info("Auto Extraction Report download after applying filter "+filterName+" is successful");
            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating AE Tracker Report due to "+e.getMessage());
        }
        finally {
            logger.info("Deleting downloaded AE Tracker report from location "+outputFilePath);
            FileUtils.deleteFile(outputFilePath + "/" + outputFileName);
            logger.info(outputFileName+" AE Tracker report Deleted successfully.");
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

    private List<String> getFiltersToIgnoreList(String reportName, String reportId) {
        List<String> filtersToIgnore = new ArrayList<>();

        try {
            Map<String, String> filtersToIgnoreMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "filters to ignore");

            if (!filtersToIgnoreMap.isEmpty()) {
                if (filtersToIgnoreMap.containsKey(reportId) && !filtersToIgnoreMap.get(reportId).trim().equalsIgnoreCase("")) {
                    String[] filtersToIgnoreArr = filtersToIgnoreMap.get(reportId).trim().split(Pattern.quote(","));

                    for (String filter : filtersToIgnoreArr) {
                        filtersToIgnore.add(filter.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Filters to Ignore List for Report [{}] having Id {}. {}", reportName, reportId, e.getStackTrace());
            return null;
        }
        return filtersToIgnore;
    }
}