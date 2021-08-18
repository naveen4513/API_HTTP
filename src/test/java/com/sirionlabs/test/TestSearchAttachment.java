package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.search.SearchAttachment;
import com.sirionlabs.api.search.SearchAttachmentDownload;
import com.sirionlabs.api.searchFilter.SearchFilterData;
import com.sirionlabs.api.searchLayout.SearchLayoutEntityTypes;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.search.SearchAttachmentHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestSearchAttachment {
    private final static Logger logger = LoggerFactory.getLogger(TestSearchAttachment.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static int limit = 5;
    private static int offset = 0;
    private static int maxRecordsToValidate = 5;
    private static boolean applyRandomization = true;
    private static int maxFiltersToValidate = 3;
    private static int maxFilterOptionsToValidate = 3;
    private static String downloadFile = null;
    private static Boolean verifyDownloadResults = true;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SearchAttachmentConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SearchAttachmentConfigFileName");
        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization").trim().equalsIgnoreCase("false"))
            applyRandomization = false;

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "resultslimit");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            limit = Integer.parseInt(temp);
        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            offset = Integer.parseInt(temp);
        if (applyRandomization) {
            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxnoofrecordstovalidate");
            if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
                maxRecordsToValidate = Integer.parseInt(temp);
            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxnooffilterstovalidate");
            if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
                maxFiltersToValidate = Integer.parseInt(temp);
            temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxnooffilteroptionstovalidate");
            if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
                maxFilterOptionsToValidate = Integer.parseInt(temp);
        }

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "verifyDownloadResults");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            verifyDownloadResults = false;

        String downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
        String downloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilename");
        downloadFile = downloadFilePath + "/" + downloadFileName + ".xlsx";
    }

    @DataProvider(parallel = true)
    public Object[][] dataProviderForSearchAttachment() {
        List<Map<String, String>> allEntitiesToTest = setAllEntitiesToTest();
        List<Object[]> allTestData = new ArrayList<>();
        for (Map<String, String> entityToTest : allEntitiesToTest) {
            String queryText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityquerytextmapping", entityToTest.get("name").trim());
            if (queryText != null && !queryText.trim().equalsIgnoreCase("")) {
                String[] allKeywords = queryText.split(Pattern.quote(","));
                for (String keyword : allKeywords) {
                    if (!keyword.trim().equalsIgnoreCase(""))
                        allTestData.add(new Object[]{entityToTest.get("name").trim(), Integer.parseInt(entityToTest.get("id").trim()), keyword.trim()});
                }
            } else {
                String defaultQueryText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityquerytextmapping", "default");
                allTestData.add(new Object[]{entityToTest.get("name").trim(), Integer.parseInt(entityToTest.get("id").trim()), defaultQueryText});
            }
        }
        logger.info("Total Keywords : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForSearchAttachment")
    public void testSearchAttachment(String entityLabel, int entityTypeId, String keyword) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Verifying Search Attachment for Entity {} and Query Text {}.", entityLabel, keyword);
            logger.info("Hitting SearchAttachment Api for Entity {} and Keyword [{}]", entityLabel, keyword);
            SearchAttachment attachObj = new SearchAttachment();
            attachObj.hitAttachment(keyword, entityTypeId, limit, offset);
            String attachmentJsonStr = attachObj.getAttachmentJsonStr();

            if (!ParseJsonResponse.validJsonResponse(attachmentJsonStr)) {
                csAssert.assertTrue(false, "Search Attachment Response for Entity Label [" + entityLabel + "] and Keyword [" + keyword +
                        "] is not valid JSON.");
            } else {
                JSONObject jsonObj = new JSONObject(attachmentJsonStr);

                if (jsonObj.has("searchResults")) {
                    SearchAttachmentHelper attachmentHelperObj = new SearchAttachmentHelper();

                    if (attachmentHelperObj.getFilteredCount(attachmentJsonStr) > 0) {
                        JSONArray jsonArr = jsonObj.getJSONArray("searchResults");
                        logger.info("Total Results found for Entity {} and Keyword [{}] are: {}", entityLabel, keyword, jsonArr.length());
                        if (jsonArr.length() > 0) {
                            if (verifyDownloadResults) {
                                boolean fileDownloaded = verifyAttachmentDownload(keyword, entityTypeId);
                                csAssert.assertTrue(fileDownloaded, "Attachment File Download failed for QueryText " + keyword);
                            }

                            int[] randomNumbersForResults;
                            if (applyRandomization) {
                                logger.info("Maximum No of Results to be Validated: {}", maxRecordsToValidate);
                                randomNumbersForResults = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, jsonArr.length() - 1, maxRecordsToValidate);
                            } else {
                                randomNumbersForResults = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, jsonArr.length() - 1, jsonArr.length());
                            }
                            for (int randomNumber : randomNumbersForResults) {
                                String documentName = jsonArr.getJSONObject(randomNumber).getString("documentName");
                                verifyResponse(jsonArr.getJSONObject(randomNumber).toString(), entityLabel, entityTypeId, keyword, documentName, csAssert);
                            }
                            verifyFilters(entityLabel, entityTypeId, keyword);
                        }
                    }
                } else {
                    throw new SkipException("JSONArray SearchResults not found for Entity " + entityLabel + " and Keyword [" + keyword + "]");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Search Attachment. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Verifying Search Attachment. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifyResponse(String jsonStr, String entityLabel, int entityTypeId, String keyword, String documentName, CustomAssert csAssert) {
        try {
            logger.info("Verifying Response for Entity {} and Keyword [{}]", entityLabel, keyword);
            JSONObject jsonObj = new JSONObject(jsonStr);
            csAssert.assertTrue(jsonObj.getInt("entityTypeId") == entityTypeId, "Actual EntityTypeId: " + jsonObj.getInt("entityTypeId") +
                    " and Expected EntityTypeId: " + entityTypeId + " for Entity " + entityLabel + " and Keyword [" + keyword + "].");
            JSONArray snippetsArr = jsonObj.getJSONArray("highlightedSnippets");
            for (int i = 0; i < snippetsArr.length(); i++) {
                logger.info("Verifying Snippet #{} of Record having Document Name [{}] for Entity {} and Keyword [{}]", (i + 1), documentName, entityLabel, keyword);
                boolean casePass = false;
                String snippetStr = snippetsArr.getString(i).trim().toLowerCase();
                if (keyword.startsWith("\"") && keyword.endsWith("\"")) {
                    if (snippetStr.contains("<b>" + keyword.substring(1, keyword.length() - 1).toLowerCase() + "</b>"))
                        casePass = true;
                } else {
                    String[] keywordTokens = keyword.split(Pattern.quote(" "));
                    for (String token : keywordTokens) {
                        if (snippetStr.contains("<b>" + token.trim().toLowerCase() + "</b>")) {
                            casePass = true;
                            break;
                        }
                    }
                }
                if (!casePass) {
                    csAssert.assertTrue(false, "Highlighted Snippet #" + (i + 1) + " of Record having Document Name [" + documentName +
                            "] doesn't contain Keyword [" + keyword + "]");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Response for Entity {} and Keyword [{}]. {}", entityLabel, keyword, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Response for Entity " + entityLabel + " and Keyword " + keyword + ". " + e.getMessage());
        }
    }

    private void verifyFilters(String entityLabel, int entityTypeId, String keyword) {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("******************************************************");
            logger.info("Verifying Filters for Entity {} and Keyword [{}]", entityLabel, keyword);
            SearchFilterData dataObj = new SearchFilterData();
            dataObj.hitSearchFilterData();
            String dataJsonStr = dataObj.getSearchFilterDataJsonStr();
            List<Map<String, String>> allFilterMetadatas = SearchFilterData.getAllFilterMetadatas(dataJsonStr);
            logger.info("Total Filters available are: {}", allFilterMetadatas.size());
            int[] randomNumbersForFilters;
            if (applyRandomization) {
                logger.info("Max No of Filters to be Validated for Entity {} and Keyword [{}] are: {}", entityLabel, keyword, maxFiltersToValidate);
                randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allFilterMetadatas.size() - 1, maxFiltersToValidate);
            } else {
                randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allFilterMetadatas.size() - 1, allFilterMetadatas.size());
            }
            for (int randomFilterNo : randomNumbersForFilters) {
                Map<String, String> filterMetadata = allFilterMetadatas.get(randomFilterNo);
                logger.info("Verifying Filter [{}] for Entity {} and Keyword [{}]", filterMetadata.get("name"), entityLabel, keyword);
                String filterQueryName = filterMetadata.get("queryName");
                String filterType = filterMetadata.get("uiType");
                if (SearchFilterData.isFilterAutoComplete(dataJsonStr, Integer.parseInt(filterMetadata.get("id"))))
                    filterType = "autoComplete";

                String showPageObjectName = null;
                if (!filterQueryName.trim().equalsIgnoreCase("supplier") && !filterQueryName.trim().equalsIgnoreCase("contract")) {
                    logger.info("Getting Show Page Object Name mapping for Filter {}", filterMetadata.get("name"));
                    showPageObjectName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filternameshowpageobjectmapping",
                            filterMetadata.get("queryName"));
                }

                if (filterType.trim().equalsIgnoreCase("MultiSelect") || filterType.trim().equalsIgnoreCase("Select")) {
                    logger.info("Getting All Options of Filter [{}] for Entity {} and Keyword [{}]", filterMetadata.get("name"), entityLabel, keyword);
                    List<Map<String, String>> allOptions = SearchFilterData.getAllFilterDataOptionsFromFilterId(dataJsonStr, Integer.parseInt(filterMetadata.get("id")));
                    logger.info("Total Options available of Filter [{}] for Entity {} and Keyword [{}] are : {}", filterMetadata.get("name"), entityLabel, keyword,
                            allOptions.size());
                    int[] randomNumbersForOptions;
                    if (applyRandomization) {
                        logger.info("Max No of Options to be Validated of Filter [{}] for Entity {} and Keyword [{}] are: {}", filterMetadata.get("name"), entityLabel, keyword,
                                maxFilterOptionsToValidate);
                        randomNumbersForOptions = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, maxFilterOptionsToValidate);
                    } else {
                        randomNumbersForOptions = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, allOptions.size());
                    }

                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    List<FutureTask<Boolean>> taskList = new ArrayList<>();
                    for (int randomOptionNo : randomNumbersForOptions) {
                        String finalShowPageObjectName = showPageObjectName;

                        FutureTask<Boolean> result = new FutureTask<>(() -> {
                            verifyFiltersResponse(entityLabel, entityTypeId, keyword, filterMetadata, allOptions.get(randomOptionNo), filterQueryName,
                                    csAssert, finalShowPageObjectName);
                            return true;
                        }
                        );
                        taskList.add(result);
                        executor.execute(result);
                    }
                    for (FutureTask<Boolean> task : taskList)
                        task.get();

                } else if (filterType.trim().equalsIgnoreCase("autoComplete")) {
                    String optionsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFilePath");
                    String optionsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName");
                    int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropdowntype",
                            filterQueryName));
                    Map<String, String> optionsParams = new HashMap<>();
                    String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pagetype", "documentTree");
                    optionsParams.put("pageType", pageType);
                    optionsParams.put("entityTpeId", Integer.toString(entityTypeId));
                    String query = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultfiltertext");
                    optionsParams.put("query", query);
                    Options optionObj = new Options();
                    logger.info("Hitting Options Api for Query Text: {}", query);
                    optionObj.hitOptions(dropDownType, optionsParams);
                    String id = optionObj.getIds();
                    if (id.contains(",")) {
                        String[] allIds = id.split(Pattern.quote(","));
                        logger.info("Multiple data available for Query Text: {}. Considering first record i.e. {}", query, allIds[0].trim());
                        id = allIds[0].trim();
                    }
                    Map<String, String> optionsMap = new HashMap<>();
                    optionsMap.put("name", Options.getNameFromId(optionObj.getOptionsJsonStr(), Integer.parseInt(id)));
                    optionsMap.put("id", id);
                    verifyFiltersResponse(entityLabel, entityTypeId, keyword, filterMetadata, optionsMap, filterQueryName, csAssert, showPageObjectName);
                } else {
                    logger.info("Currently Filters of Type other than Select/MultiSelect and AutoComplete are not supported");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Filters for Keyword [{}]. {}", keyword, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Filters for Keyword [" + keyword + "]. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyFiltersResponse(String entityLabel, int entityTypeId, String keyword, Map<String, String> filterMetadata, Map<String, String> optionsMap,
                                       String filterQueryName, CustomAssert csAssert, String showPageObjectName) {
        logger.info("Verifying Filter Response for Entity {}, Keyword [{}], Filter [{}], Value {}", entityLabel, keyword, filterMetadata.get("name"), optionsMap.get("name"));
        try {
            String optionId = optionsMap.get("id");
            logger.info("Proceeding with Option {} for Filter [{}], Entity {} and Keyword [{}]", optionsMap.get("name"), filterMetadata.get("name"), entityLabel, keyword);
            Map<String, String> filtersPayloadMap = new HashMap<>();
            String payloadKeyName;
            if (filterQueryName.endsWith("s"))
                payloadKeyName = filterQueryName.trim().substring(0, filterQueryName.length() - 1) + "Ids";
            else
                payloadKeyName = filterQueryName.trim() + "Ids";
            filtersPayloadMap.put(payloadKeyName, optionId);
            logger.info("Hitting Attachment Api for Option {}, Filter [{}], Entity {} and Keyword [{}]", optionsMap.get("name"), filterMetadata.get("name"), entityLabel,
                    keyword);
            SearchAttachment attachObj = new SearchAttachment();
            attachObj.hitAttachment(keyword, entityTypeId, limit, offset, filtersPayloadMap);
            String attachmentJsonStr = attachObj.getAttachmentJsonStr();

            JSONObject jsonObj = new JSONObject(attachmentJsonStr);

            if (jsonObj.has("searchResults")) {
                JSONArray resultsArr = jsonObj.getJSONArray("searchResults");
                if (filterQueryName.trim().equalsIgnoreCase("supplier") || filterQueryName.trim().equalsIgnoreCase("contract")) {
                    logger.info("Total Results found for Option {}, Filter [{}], Entity {} and Keyword [{}] are : {}", optionsMap.get("name"), filterMetadata.get("name"),
                            entityLabel, keyword, resultsArr.length());

                    if (resultsArr.length() > 0) {
                        if (verifyDownloadResults) {
                            boolean fileDownloaded = verifyAttachmentDownload(keyword, entityTypeId, payloadKeyName, optionId);
                            csAssert.assertTrue(fileDownloaded, "Attachment File Download failed for QueryText " + keyword + ", PayloadKey " + payloadKeyName +
                                    " and PayloadValue " + optionId);
                        }
                    }

                    if (filterQueryName.trim().equalsIgnoreCase("supplier")) {
                        for (int i = 0; i < resultsArr.length(); i++) {
                            logger.info("Verifying Result #{} for Option Id {}, Filter [{}], Entity {} and Keyword [{}]", (i + 1), optionId, filterMetadata.get("name"),
                                    entityLabel, keyword);
                            jsonObj = resultsArr.getJSONObject(i);
                            if (jsonObj.has("relationId") && !jsonObj.get("relationId").toString().trim().equalsIgnoreCase("null")) {
                                int actualSupplierId = jsonObj.getInt("relationId");
                                boolean supplierIdPass = Integer.toString(actualSupplierId).trim().equalsIgnoreCase(optionId);
                                if (!supplierIdPass) {
                                    csAssert.assertTrue(false, "Actual Supplier Id: " + actualSupplierId + " and Expected Supplier Id: " + optionId +
                                            " for Result #" + (i + 1) + ", Option " + optionsMap.get("name") + ", Filter [" + filterMetadata.get("name") + "], Entity " +
                                            entityLabel + " and Keyword [" + keyword + "]");
                                }
                            }
                        }
                        logger.info("******************************************************");
                    } else if (filterQueryName.trim().equalsIgnoreCase("contract")) {
                        for (int j = 0; j < resultsArr.length(); j++) {
                            logger.info("Verifying Result #{} for Option Id {}, Filter [{}], Entity {} and Keyword [{}]", (j + 1), optionId, filterMetadata.get("name"),
                                    entityLabel, keyword);
                            jsonObj = resultsArr.getJSONObject(j);
                            int actualContractId = jsonObj.getJSONArray("contractId").getInt(0);
                            boolean contractIdPass = Integer.toString(actualContractId).trim().equalsIgnoreCase(optionId);
                            if (!contractIdPass) {
                                csAssert.assertTrue(false, "Actual Contract Id: " + actualContractId + " and Expected Contract Id: " + optionId +
                                        " for Result #" + (j + 1) + ", Option " + optionsMap.get("name") + ", Filter [" + filterMetadata.get("name") + "], Entity " + entityLabel +
                                        " and Keyword [" + keyword + "]");
                            }
                        }
                        logger.info("******************************************************");
                    }
                } else {
                    logger.info("Total Results found for Option {}, Filter [{}], Entity {} and Keyword [{}] are : {}", optionsMap.get("name"), filterMetadata.get("name"),
                            entityLabel, keyword, resultsArr.length());

                    if (resultsArr.length() > 0) {
                        if (verifyDownloadResults) {
                            boolean fileDownloaded = verifyAttachmentDownload(keyword, entityTypeId, payloadKeyName, optionId);
                            csAssert.assertTrue(fileDownloaded, "Attachment File Download failed for QueryText " + keyword + ", PayloadKey " + payloadKeyName +
                                    " and PayloadValue " + optionId);
                        }
                    }

                    for (int i = 0; i < resultsArr.length(); i++) {
                        jsonObj = resultsArr.getJSONObject(i);
                        Integer entityId = jsonObj.getInt("entityId");

                        logger.info("Verifying Result #{} for Option {}, Entity Id {}, Filter [{}], Entity {} and Keyword [{}]", (i + 1), optionsMap.get("name"),
                                entityId, filterMetadata.get("name"), entityLabel, keyword);

                        Show showObj = new Show();
                        logger.info("Hitting Show Api for Result #{} having Entity Id {}, Value {}, Filter [{}], Entity {} and Keyword [{}]", (i + 1),
                                entityId, optionsMap.get("name"), filterMetadata.get("name"), entityLabel, keyword);
                        showObj.hitShow(entityTypeId, entityId);
                        String showJsonStr = showObj.getShowJsonStr();
                        boolean result;

                        if (showPageObjectName != null && !showPageObjectName.trim().equalsIgnoreCase("")) {
                            if (ParseJsonResponse.hasPermissionError(showJsonStr)) {
                                logger.info("Doesn't have Permission to Access Show Page for Result #{} having Entity Id {}, Value {}, Filter [{}], Entity {} and Keyword [{}]. " +
                                                "Hence Skipping validation.",
                                        (i + 1), optionsMap.get("name"), entityId, filterMetadata.get("name"), entityLabel, keyword);
                            } else {
                                result = showObj.verifyShowField(showJsonStr, showPageObjectName, optionsMap.get("name"), entityTypeId, "text");
                                if (!result) {
                                    csAssert.assertTrue(false, "Data Validation for Filter " + filterMetadata.get("name") +
                                            " having Value " + optionsMap.get("name") + ", Entity Id " + entityId + " and Keyword [" + keyword +
                                            "] failed on Show Page.");
                                }
                            }
                        } else {
                            logger.error("Show Page Object Name mapping not found for Filter {} and Keyword [{}]. Hence skipping Validation on Show " +
                                    "Page", filterMetadata.get("name"), keyword);
                            csAssert.assertTrue(false, "Show Page Object Name mapping not found for Filter " +
                                    filterMetadata.get("name") + " and Keyword [" + keyword + "]");
                        }
                    }
                }
            } else {
                throw new SkipException("JSONArray SearchResults not found for Option " + optionsMap.get("name") + ", Filter " + filterMetadata.get("name") +
                        ", Entity " + entityLabel + " and Keyword [" + keyword + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Option {} for Filter [{}], Entity {} and Keyword [{}]. {}", optionsMap.get("name"), filterMetadata.get("name"), entityLabel,
                    keyword, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Option " + optionsMap.get("name") + " for Filter " +
                    filterMetadata.get("name") + ", Entity " + entityLabel + " and Keyword [" + keyword + "]. " + e.getMessage());
        }
        logger.info("******************************************************");
    }

    private List<Map<String, String>> setAllEntitiesToTest() {
        List<Map<String, String>> allEntities = new ArrayList<>();
        logger.info("Setting all Entities to Test.");
        try {
            SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
            logger.info("Hitting Search Layout Entity Types Api.");
            String entityTypesJsonStr = entityTypesObj.hitSearchLayoutEntityTypes();

            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllEntities");
            if (temp != null && temp.trim().equalsIgnoreCase("false")) {
                String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiesToTest").split(Pattern.quote(","));
                for (String entity : entitiesToTest) {
                    Map<String, String> entityMap = new HashMap<>();
                    entityMap.put("name", entity.trim());
                    entityMap.put("id", Integer.toString(SearchLayoutEntityTypes.getAttachmentEntityTypeIdFromEntityName(entityTypesJsonStr, entity.trim())));
                    allEntities.add(entityMap);
                }
            } else {
                allEntities = SearchLayoutEntityTypes.getAttachmentEntityTypes(entityTypesJsonStr);
            }
        } catch (Exception e) {
            logger.error("Exception while Setting all Entities to Test. {}", e.getMessage());
        }
        return allEntities;
    }

    private boolean verifyAttachmentDownload(String queryText, int entityTypeId) {
        return verifyAttachmentDownload(queryText, entityTypeId, null, null);
    }

    private boolean verifyAttachmentDownload(String queryText, int entityTypeId, String filterPayloadKey, String filterPayloadValue) {
        boolean downloadPass = false;
        try {
            SearchAttachmentDownload downloadObj = new SearchAttachmentDownload();
            HttpResponse downloadResponse;
            if (filterPayloadKey != null && filterPayloadValue != null) {
                Map<String, String> filtersMap = new HashMap<>();
                filtersMap.put(filterPayloadKey, filterPayloadValue);
                downloadResponse = downloadObj.downloadAttachmentResultsFile(downloadFile, queryText, entityTypeId, limit, filtersMap);
            } else {
                downloadResponse = downloadObj.downloadAttachmentResultsFile(downloadFile, queryText, entityTypeId, limit);
            }
            if (downloadResponse != null) {
                File downloadedFile = new File(downloadFile);
                if (downloadedFile.exists())
                    downloadPass = true;
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Attachment Download for Keyword [{}]. PayloadKey: {} and PayloadValue: {}. {}", queryText, filterPayloadKey,
                    filterPayloadValue, e.getStackTrace());
        }
        return downloadPass;
    }
}
