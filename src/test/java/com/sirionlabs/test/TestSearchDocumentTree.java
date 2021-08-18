package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.documentViewer.DocumentViewerShow;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.search.ContractDocDownload;
import com.sirionlabs.api.search.SearchContractDoc;
import com.sirionlabs.api.searchFilter.SearchFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestSearchDocumentTree {
    private final static Logger logger = LoggerFactory.getLogger(TestSearchDocumentTree.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static int limit = 5;
    private static int offset = 0;
    private static int maxRecordsToValidate = 5;
    private static int accessListId;
    private static int contractEntityTypeId;
    private static int accessLimit = 20;
    private static int accessOffset = 0;
    private static boolean applyRandomization = true;
    private static int maxFiltersToValidate = 3;
    private static int maxFilterOptionsToValidate = 3;
    private static String authTokenPrefix;
    private static String userIdObjectKey;
    private static String downloadFile = null;
    private static Boolean verifyDownloadResults = true;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SearchDocumentConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SearchDocumentConfigFileName");
        accessListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "contracts access user", "entity_url_id"));
        contractEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), "contracts"));
        logger.info("Setting AuthToken Prefix to be used to Verify Access on Contracts Show Page.");
        String authToken = Check.getAuthorization();
		String[] tokens = authToken.split(Pattern.quote(":"));
        authTokenPrefix = tokens[0].trim();
        setUserIdObjectKey();
        if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization").trim().equalsIgnoreCase("false"))
            applyRandomization = false;

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "resultslimit");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            limit = Integer.parseInt(temp);
        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            offset = Integer.parseInt(temp);
        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "accessdatalimit");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            accessLimit = Integer.parseInt(temp);
        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "accessdataoffset");
        if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
            accessOffset = Integer.parseInt(temp);
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
    public Object[][] keywordsToSearch() {
        logger.info("Setting all Keywords/Strings to be Searched.");
        String delimiterForValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delimiterforvalues").trim();
        String queryText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "queryText");
        List<Object[]> allTestData = new ArrayList<>();
        if (queryText != null && !queryText.trim().equalsIgnoreCase("")) {
            String[] allKeywords = queryText.split(Pattern.quote(delimiterForValues));
            for (String keyword : allKeywords) {
                if (!keyword.trim().equalsIgnoreCase(""))
                    allTestData.add(new Object[]{keyword.trim()});
            }
        } else
            allTestData.add(new Object[]{"Test"});
        logger.info("Total Keywords : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "keywordsToSearch")
    public void testSearchDocTree(String keyword) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Keyword to Search: [{}]", keyword);
            logger.info("Hitting SearchContractDoc Api for Keyword [{}]", keyword);
            SearchContractDoc docTreeObj = new SearchContractDoc();
            docTreeObj.hitSearchContractDoc(keyword, limit, offset);
            String docTreeJsonStr = docTreeObj.getContractDocJsonStr();
            JSONObject jsonObj = new JSONObject(docTreeJsonStr);

            if (jsonObj.has("searchResults")) {
                JSONArray jsonArr = jsonObj.getJSONArray("searchResults");
                logger.info("Total Results found for Keyword [{}] are: {}", keyword, jsonArr.length());
                boolean fileDownloaded;
                if (jsonArr.length() > 0) {
                    if (verifyDownloadResults) {
                        fileDownloaded = verifySearchDownload(keyword);
                        csAssert.assertTrue(fileDownloaded, "ContractDoc File Download failed for QueryText " + keyword);
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
//						int contractId = jsonArr.getJSONObject(randomNumber).getJSONArray("contractId").getInt(0);
                        verifyResponse(jsonArr.getJSONObject(randomNumber).toString(), keyword, documentName, csAssert);
//					verifyAccess(contractId, csAssert);
                    }
//						verifyFilters(keyword);
                }
            } else {
                throw new SkipException("JSONArray SearchResults not found for Keyword " + keyword);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Testing Document Tree. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Testing Document Tree. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifyResponse(String jsonStr, String keyword, String documentName, CustomAssert csAssert) {
        try {
            logger.info("Verifying Response for Keyword [{}]", keyword);
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray snippetsArr = jsonObj.getJSONArray("highlightedSnippets");
            for (int i = 0; i < snippetsArr.length(); i++) {
                logger.info("Verifying Snippet #{} of Record having Document Name [{}] for Keyword [{}]", (i + 1), documentName, keyword);
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
            logger.error("Exception while Verifying Response for Keyword [{}]. {}", keyword, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Response for Keyword " + keyword + ". " + e.getMessage());
        }
    }

    private void verifyAccess(int contractId, CustomAssert csAssert) {
        try {
            logger.info("Verifying User Access for Contract Id {}", contractId);
            boolean accessPass = false;
            ListRendererTabListData tabListDataObj = new ListRendererTabListData();
            String payload = "{\"filterMap\":{\"offset\":" + accessOffset + ",\"size\":" + accessLimit + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                    "\"filterJson\":{}}}";
            logger.info("Hitting ListRendererTabListData Api for Contract Id {}", contractId);
            tabListDataObj.hitListRendererTabListData(accessListId, contractEntityTypeId, contractId, payload);
            String tabListJsonStr = tabListDataObj.getTabListDataJsonStr();
            JSONObject jsonObj = new JSONObject(tabListJsonStr);
            JSONArray jsonArr = jsonObj.getJSONArray("data");
            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getJSONObject(userIdObjectKey).getString("value").trim().contains(authTokenPrefix)) {
                    accessPass = true;
                    break;
                }
            }
            if (!accessPass) {
                csAssert.assertTrue(false, "Access is not provided to User having AuthTokenPrefix [" + authTokenPrefix + "] for Contract Id " +
                        contractId);
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying Access of User on Contracts Show Page having ContractId {}. {}", contractId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Access of User on Contracts Show Page having ContractId" + contractId + ". " +
                    e.getMessage());
        }
    }

    private void setUserIdObjectKey() {
        try {
            logger.info("Setting UserIdObjectKey to be used to Verify Access at Contract Show Page");
            ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
            Map<String, String> params = new HashMap<>();
            params.put("entityTypeId", Integer.toString(contractEntityTypeId));
            logger.info("Hitting ListRendererDefaultUserListMetadata Api.");
            defaultUserListObj.hitListRendererDefaultUserListMetadata(accessListId, params);
            defaultUserListObj.setColumns(defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr());
            userIdObjectKey = Integer.toString(defaultUserListObj.getIdFromQueryName("userid"));
        } catch (Exception e) {
            logger.error("Exception while setting UserIdObjectKey. {}", e.getMessage());
        }
    }

    private void verifyFilters(String keyword) {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("******************************************************");
            logger.info("Verifying Filters for Keyword [{}]", keyword);
            SearchFilterData dataObj = new SearchFilterData();
            dataObj.hitSearchFilterData();
            String dataJsonStr = dataObj.getSearchFilterDataJsonStr();
            List<Map<String, String>> allFilterMetadatas = SearchFilterData.getAllFilterMetadatas(dataJsonStr);
            logger.info("Total Filters available are: {}", allFilterMetadatas.size());
            int[] randomNumbersForFilters;
            if (applyRandomization) {
                logger.info("Max No of Filters to be Validated for Keyword [{}] are: {}", keyword, maxFiltersToValidate);
                randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allFilterMetadatas.size() - 1, maxFiltersToValidate);
            } else {
                randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allFilterMetadatas.size() - 1, allFilterMetadatas.size());
            }
            for (int randomFilterNo : randomNumbersForFilters) {
                Map<String, String> filterMetadata = allFilterMetadatas.get(randomFilterNo);
                logger.info("Verifying Filter [{}] for Keyword [{}]", filterMetadata.get("name"), keyword);
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
                    logger.info("Getting All Options for Filter [{}] and Keyword [{}]", filterMetadata.get("name"), keyword);
                    List<Map<String, String>> allOptions = SearchFilterData.getAllFilterDataOptionsFromFilterId(dataJsonStr, Integer.parseInt(filterMetadata.get("id")));
                    logger.info("Total Options available for Filter [{}] and Keyword [{}] are : {}", filterMetadata.get("name"), keyword, allOptions.size());
                    int[] randomNumbersForOptions;
                    if (applyRandomization) {
                        logger.info("Max No of Options to be Validated for Filter [{}] and Keyword [{}] are: {}", filterMetadata.get("name"), keyword,
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
                            verifyFiltersResponse(keyword, filterMetadata, allOptions.get(randomOptionNo), filterQueryName, csAssert, finalShowPageObjectName);
                            return true;
                        });
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
                    String entityTypeId;
                    if (filterQueryName.trim().equalsIgnoreCase("supplier") || filterQueryName.trim().equalsIgnoreCase("suppliers"))
                        entityTypeId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                                ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), "suppliers");
                    else if (filterQueryName.trim().equalsIgnoreCase("contract") || filterQueryName.trim().equalsIgnoreCase("contracts"))
                        entityTypeId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                                ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), "contracts");
                    else
                        entityTypeId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
                                ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), filterQueryName.trim());
                    if (entityTypeId == null) {
                        logger.error("Entity Type Id not defined for Filter Query Name: {}. Hence Skipping validation", filterQueryName.trim());
                        csAssert.assertTrue(false, "Entity Type Id not defined for Filter Query Name: " + filterQueryName.trim() + ". Hence Skipping validation.");
                        continue;
                    }
                    optionsParams.put("entityTpeId", entityTypeId);
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
                    verifyFiltersResponse(keyword, filterMetadata, optionsMap, filterQueryName, csAssert, showPageObjectName);
                } else {
                    logger.info("Currently Filters of Type other than Select/MultiSelect and AutoComplete are not supported");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Filters for Keyword [{}]. {}", keyword, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Filters for Keyword [" + keyword + "]. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void verifyFiltersResponse(String keyword, Map<String, String> filterMetadata, Map<String, String> optionsMap, String filterQueryName,
                                       CustomAssert csAssert, String showPageObjectName) {
        String optionName = optionsMap.get("name");
        logger.info("Verifying Filter Response for Keyword [{}], Filter [{}], Value {}", keyword, filterMetadata.get("name"), optionName);

        try {
            String optionId = optionsMap.get("id");
            logger.info("Proceeding with Option {} for Filter [{}] and Keyword [{}]", optionName, filterMetadata.get("name"), keyword);

            Map<String, String> filtersPayloadMap = new HashMap<>();
            String payloadKeyName;

            if (filterQueryName.endsWith("s"))
                payloadKeyName = filterQueryName.trim().substring(0, filterQueryName.length() - 1) + "Ids";
            else
                payloadKeyName = filterQueryName.trim() + "Ids";

            filtersPayloadMap.put(payloadKeyName, optionId);
            logger.info("Hitting ContractDoc Api for Option {}, Filter [{}] and Keyword [{}]", optionName, filterMetadata.get("name"), keyword);

            SearchContractDoc docTreeObj = new SearchContractDoc();
            docTreeObj.hitSearchContractDoc(keyword, limit, offset, filtersPayloadMap);
            String docTreeJsonStr = docTreeObj.getContractDocJsonStr();
            JSONObject jsonObj = new JSONObject(docTreeJsonStr);

            if (jsonObj.has("searchResults")) {
                JSONArray resultsArr = jsonObj.getJSONArray("searchResults");

                if (filterQueryName.trim().equalsIgnoreCase("supplier") || filterQueryName.trim().equalsIgnoreCase("contract")) {
                    logger.info("Total Results found for Option {}, Filter [{}] and Keyword [{}] are : {}", optionName,
                            filterMetadata.get("name"), keyword, resultsArr.length());

                    if (resultsArr.length() > 0) {
                        if (verifyDownloadResults) {
                            boolean fileDownloaded = verifySearchDownload(keyword, payloadKeyName, optionId);
                            csAssert.assertTrue(fileDownloaded, "ContractDoc File Download failed for QueryText " + keyword + ", PayloadKey " +
                                    payloadKeyName + " and PayloadValue " + optionId);
                        }
                    }

                    if (filterQueryName.trim().equalsIgnoreCase("supplier")) {
                        for (int i = 0; i < resultsArr.length(); i++) {
                            logger.info("Verifying Result #{} for Option Id {}, Filter [{}] and Keyword [{}]", (i + 1), optionId,
                                    filterMetadata.get("name"), keyword);
                            jsonObj = resultsArr.getJSONObject(i);
                            int actualSupplierId = jsonObj.getInt("relationId");
                            boolean supplierIdPass = Integer.toString(actualSupplierId).trim().equalsIgnoreCase(optionId);
                            if (!supplierIdPass) {
                                csAssert.assertTrue(false, "Actual Supplier Id: " +
                                        actualSupplierId + " and Expected Supplier Id: " + optionId + " for Result #" + (i + 1) + ", Option " +
                                        optionName + ", Filter [" + filterMetadata.get("name") + "] and Keyword [" + keyword + "]");
                            }
                        }
                        logger.info("******************************************************");
                    } else if (filterQueryName.trim().equalsIgnoreCase("contract")) {
                        for (int j = 0; j < resultsArr.length(); j++) {
                            logger.info("Verifying Result #{} for Option Id {}, Filter [{}] and Keyword [{}]", (j + 1), optionId,
                                    filterMetadata.get("name"), keyword);
                            jsonObj = resultsArr.getJSONObject(j);
                            int actualContractId = jsonObj.getJSONArray("contractId").getInt(0);
                            boolean contractIdPass = Integer.toString(actualContractId).trim().equalsIgnoreCase(optionId);
                            if (!contractIdPass) {
                                csAssert.assertTrue(false, "Actual Contract Id: " +
                                        actualContractId + " and Expected Contract Id: " + optionId + " for Result #" + (j + 1) + ", Option " +
                                        optionName + ", Filter [" + filterMetadata.get("name") + "] and Keyword [" + keyword + "]");
                            }
                        }
                        logger.info("******************************************************");
                    }
                } else {
                    //Special handling for Document Tags Filter
                    if (filterQueryName.equalsIgnoreCase("documentTags")) {
                        for (int k = 0; k < resultsArr.length(); k++) {
                            int documentId = resultsArr.getJSONObject(k).getInt("documentId");
                            String documentViewerShowResponse = DocumentViewerShow.getDocumentViewerShowResponse(documentId);

                            if (ParseJsonResponse.validJsonResponse(documentViewerShowResponse)) {
                                if (ParseJsonResponse.getStatusFromResponse(documentViewerShowResponse).equalsIgnoreCase("applicationError")) {
                                    continue;
                                }

                                JSONObject docShowJsonObj = new JSONObject(documentViewerShowResponse);
                                docShowJsonObj = docShowJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("documentTags").getJSONObject("options");
                                JSONArray optionsArr = docShowJsonObj.getJSONArray("data");

                                boolean tagFound = false;

                                for (int l = 0; l < optionsArr.length(); l++) {
                                    docShowJsonObj = optionsArr.getJSONObject(l);

                                    if (docShowJsonObj.getString("name").equalsIgnoreCase(optionName)) {
                                        tagFound = true;
                                        break;
                                    }
                                }

                                csAssert.assertTrue(tagFound, "Document Tag [" + optionName + "] not found in Document Viewer Show Response for Document Id " + documentId);
                            } else {
                                csAssert.assertTrue(false, "DocumentViewer Show Response for Document Id " + documentId + " is an Invalid JSON.");
                            }
                        }


                    } else {

                        Map<Integer, Boolean> uniqueContracts = new HashMap<>();

                        int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields
                                .getConstantFieldsProperty("EntityIdConfigFilePath"), ConfigureConstantFields
                                .getConstantFieldsProperty("EntityIdConfigFileName"), "contracts"));
                        logger.info("Total Results found for Option {}, Filter [{}] and Keyword [{}] are : {}", optionName,
                                filterMetadata.get("name"), keyword, resultsArr.length());

                        if (resultsArr.length() > 0) {
                            if (verifyDownloadResults) {
                                boolean fileDownloaded = verifySearchDownload(keyword, payloadKeyName, optionId);
                                csAssert.assertTrue(fileDownloaded, "ContractDoc File Download failed for QueryText " + keyword + ", PayloadKey " +
                                        payloadKeyName + " and PayloadValue " + optionId);
                            }
                        }

                        for (int i = 0; i < resultsArr.length(); i++) {
                            jsonObj = resultsArr.getJSONObject(i);
                            Integer contractId = jsonObj.getJSONArray("contractId").getInt(0);
                            logger.info("Verifying Result #{} for Option {}, Contract Id {}, Filter [{}] and Keyword [{}]", (i + 1), optionName,
                                    contractId,
                                    filterMetadata.get("name"), keyword);
                            if (!uniqueContracts.containsKey(contractId)) {
                                Show showObj = new Show();
                                logger.info("Hitting Show Api for Result #{} having Contract Id {}, Value {}, Filter [{}] and Keyword [{}]", (i + 1),
                                        contractId, optionName, filterMetadata.get("name"), keyword);
                                showObj.hitShow(entityTypeId, contractId);
                                String showJsonStr = showObj.getShowJsonStr();
                                boolean result = false;

                                if (showPageObjectName != null && !showPageObjectName.trim().equalsIgnoreCase("")) {
                                    result = showObj.verifyShowField(showJsonStr, showPageObjectName, optionName, entityTypeId);
                                    if (!result) {
                                        csAssert.assertTrue(false, "Data Validation for Filter " + filterMetadata.get("name") +
                                                " having Value " + optionName + ", Contract Id " + contractId + " and Keyword [" + keyword +
                                                "] failed on Show Page.");
                                    }
                                } else {
                                    logger.error("Show Page Object Name mapping not found for Filter {} and Keyword [{}]. Hence skipping Validation on Show " +
                                            "Page", filterMetadata.get("name"), keyword);
                                    csAssert.assertTrue(false, "Show Page Object Name mapping not found for Filter " +
                                            filterMetadata.get("name") + " and Keyword [" + keyword + "]");
                                }
                                uniqueContracts.put(contractId, result);
                            } else {
                                logger.info("Not Hitting Show Api for Result #{} having Contract Id {} and Value {} as already verified earlier.", (i + 1),
                                        contractId, optionName);
                                if (!uniqueContracts.get(contractId)) {
                                    csAssert.assertTrue(false, "Data Validation for Filter " + filterMetadata.get("name") +
                                            " having Value " + optionName + ", Contract Id " + contractId + " and Keyword [" + keyword +
                                            "] failed on Show Page.");
                                }
                            }
                        }
                    }
                }
            } else {
                throw new SkipException("JSONArray SearchResults not found for Option " + optionName + ", Filter [" + filterMetadata.get("name") +
                        "] and Keyword [" + keyword + "]");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Verifying Option {} for Filter [{}] and Keyword [{}]. {}", optionName, filterMetadata.get("name"), keyword,
                    e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Verifying Option " + optionName + " for Filter " +
                    filterMetadata.get("name") + " and Keyword [" + keyword + "]. " + e.getMessage());
        }
        logger.info("******************************************************");
    }

    private boolean verifySearchDownload(String queryText) {
        return verifySearchDownload(queryText, null, null);
    }

    private boolean verifySearchDownload(String queryText, String filterPayloadKey, String filterPayloadValue) {
        boolean downloadPass = false;
        try {
            ContractDocDownload downloadObj = new ContractDocDownload();
            HttpResponse downloadResponse;
            if (filterPayloadKey != null && filterPayloadValue != null) {
                Map<String, String> filtersMap = new HashMap<>();
                filtersMap.put(filterPayloadKey, filterPayloadValue);
                downloadResponse = downloadObj.downloadContractDocResultsFile(downloadFile, queryText, limit, filtersMap);
            } else {
                downloadResponse = downloadObj.downloadContractDocResultsFile(downloadFile, queryText, limit);
            }
            if (downloadResponse != null) {
                File downloadedFile = new File(downloadFile);
                if (downloadedFile.exists())
                    downloadPass = true;
            }
        } catch (Exception e) {
            logger.error("Exception while Verifying ContractDoc Download for Keyword [{}]. PayloadKey: {} and PayloadValue: {}. {}", queryText, filterPayloadKey,
                    filterPayloadValue, e.getStackTrace());
        }
        return downloadPass;
    }
}
