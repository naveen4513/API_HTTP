package com.sirionlabs.helper.ListRenderer;

import com.sirionlabs.api.bulk.BulkFetch;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.userGroups.UserGroupsUsers;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class ListRendererFilterDataHelper {

    private final static Logger logger = LoggerFactory.getLogger(ListRendererFilterDataHelper.class);

    public static String getFilterDataResponse(String entityName) {
        return getFilterDataResponse(entityName, "{}");
    }

    private static String getFilterDataResponse(String entityName, String payload) {
        ListRendererFilterData filterObj = new ListRendererFilterData();
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        filterObj.hitListRendererFilterData(listId, payload);
        return filterObj.getListRendererFilterDataJsonStr();
    }

    public static List<String> getAllFilterNames(String filterDataResponse) {
        List<String> allFilterNames = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);

            for (String filterId : JSONObject.getNames(jsonObj)) {
                allFilterNames.add(jsonObj.getJSONObject(filterId).getString("filterName"));
            }
        } catch (Exception e) {
            logger.error("Exception while getting all Filter Names. {}", e.getMessage());
        }
        return allFilterNames;
    }

    public static List<String> getAllStaticFilterNames(String filterDataResponse) {
        List<String> allFilterNames = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);

            for (String filterId : JSONObject.getNames(jsonObj)) {
                if (!jsonObj.getJSONObject(filterId).getString("filterName").startsWith("10")) {
                    allFilterNames.add(jsonObj.getJSONObject(filterId).getString("filterName"));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting all Filter Names. {}", e.getMessage());
        }
        return allFilterNames;
    }

    public static Integer getFilterIdFromFilterName(String filterDataResponse, String filterName) {
        Integer filterId = -1;

        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);
            JSONArray jsonArr = jsonObj.names();

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("filterName").trim().equalsIgnoreCase(filterName)) {
                    filterId = jsonObj.getJSONObject(jsonArr.get(i).toString()).getInt("filterId");
                    return filterId;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Id of Filter {}. {}", filterName, e.getStackTrace());
        }
        return filterId;
    }

    public static String getFilterUiType(String filterDataResponse, String filterName) {
        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);
            JSONArray jsonArr = jsonObj.names();

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("filterName").trim().equalsIgnoreCase(filterName)) {
                    if (jsonObj.getJSONObject(jsonArr.get(i).toString()).isNull("uitype"))
                        return null;

                    return jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("uitype");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting UI Type of Filter {}. {}", filterName, e.getStackTrace());
        }
        return null;
    }

    public static String getFilterType(String filterDataResponse, String filterName, Integer filterId) {
        String filterUiType = getFilterUiType(filterDataResponse, filterName);
        filterUiType = filterUiType == null ? "null" : filterUiType.trim();

        if (filterUiType.equalsIgnoreCase("STAKEHOLDER")) {
            return "stakeholder";
        }

        JSONObject jsonObj = new JSONObject(filterDataResponse);
        jsonObj = jsonObj.getJSONObject(filterId.toString()).getJSONObject("multiselectValues").getJSONObject("OPTIONS");

        Boolean isAutoComplete = isFilterOfAutoCompleteType(filterDataResponse, filterName);

        if (isAutoComplete != null && isAutoComplete) {
            return "autoComplete";
        }

        if (filterUiType.equalsIgnoreCase("null")) {
            if ((jsonObj.has("data") && !jsonObj.isNull("data")) || (jsonObj.has("DATA") && !jsonObj.isNull("DATA"))) {
                return "select";
            }
        } else if (filterUiType.equalsIgnoreCase("Select") || filterUiType.equalsIgnoreCase("MultiSelect") ||
                filterUiType.equalsIgnoreCase("SingleSelect")) {
            if (jsonObj.has("data") && !jsonObj.isNull("data") && jsonObj.getJSONArray("data").length() == 2) {
                if (jsonObj.getJSONArray("data").getJSONObject(0).get("id").toString().trim().equalsIgnoreCase("true") ||
                        jsonObj.getJSONArray("data").getJSONObject(0).get("id").toString().trim().equalsIgnoreCase("false"))
                    return "checkBox";
            } else if (jsonObj.has("DATA") && !jsonObj.isNull("DATA") && jsonObj.getJSONArray("DATA").length() == 2) {
                if (jsonObj.getJSONArray("DATA").getJSONObject(0).get("id").toString().trim().equalsIgnoreCase("true") ||
                        jsonObj.getJSONArray("DATA").getJSONObject(0).get("id").toString().trim().equalsIgnoreCase("false"))
                    return "checkBox";
            }
        }

        return filterUiType;
    }

    public static List<Map<String, String>> getAllOptionsOfFilter(String filterDataResponse, String filterName) {
        List<Map<String, String>> allOptionsOfFilter = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);
            JSONArray jsonArr = jsonObj.names();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject tempObj = jsonObj.getJSONObject(jsonArr.get(i).toString());

                if (tempObj.getString("filterName").trim().equalsIgnoreCase(filterName)) {
                    tempObj = tempObj.getJSONObject("multiselectValues").getJSONObject("OPTIONS");
                    JSONArray optionsArr;

                    if (tempObj.has("data"))
                        optionsArr = tempObj.getJSONArray("data");
                    else if (tempObj.has("DATA"))
                        optionsArr = tempObj.getJSONArray("DATA");
                    else {
                        logger.error("Couldn't find Options for Filter {}", filterName);
                        return allOptionsOfFilter;
                    }

                    for (int j = 0; j < optionsArr.length(); j++) {
                        JSONObject optionObj = optionsArr.getJSONObject(j);
                        Map<String, String> optionMap = new HashMap<>();

                        optionMap.put("name", optionObj.getString("name"));
                        optionMap.put("id", optionObj.get("id").toString());

                        if (optionObj.has("group") && !optionObj.isNull("group"))
                            optionMap.put("group", optionObj.getString("group"));

                        allOptionsOfFilter.add(optionMap);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Options of Filter {}. {}", filterName, e.getStackTrace());
        }
        return allOptionsOfFilter;
    }

    public static Boolean isFilterOfAutoCompleteType(String filterDataResponse, String filterName) {
        try {
            JSONObject jsonObj = new JSONObject(filterDataResponse);
            JSONArray jsonArr = jsonObj.names();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject tempObj = jsonObj.getJSONObject(jsonArr.get(i).toString());

                if (tempObj.getString("filterName").trim().equalsIgnoreCase(filterName)) {
                    tempObj = tempObj.getJSONObject("multiselectValues").getJSONObject("OPTIONS");

                    return tempObj.getBoolean("autoComplete");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Filter {} is of AutoComplete Type or Not. {}", filterName, e.getStackTrace());
        }
        return null;
    }

    public static List<Map<String, String>> getAllOptionsOfAutoCompleteFilter(String filterName, int entityTypeId) {
        List<Map<String, String>> allOptionsOfFilter = new ArrayList<>();

        try {
            OptionsHelper opHelperObj = new OptionsHelper();
            String optionsResponse = opHelperObj.hitOptionsForAutoCompleteFilter(filterName, entityTypeId);

            if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                JSONObject jsonObj = new JSONObject(optionsResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject optionObj = jsonArr.getJSONObject(i);
                    Map<String, String> optionMap = new HashMap<>();

                    optionMap.put("name", optionObj.getString("name"));
                    optionMap.put("id", optionObj.get("id").toString());

                    if (optionObj.has("group"))
                        optionMap.put("group", optionObj.getString("group"));

                    allOptionsOfFilter.add(optionMap);
                }
            } else {
                logger.error("Options API Response for Filter Name [{}] and Entity Type Id {} is an Invalid JSON.", filterName, entityTypeId);
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Options of Filter [{}]. {}", filterName, e.getStackTrace());
        }
        return allOptionsOfFilter;
    }

    private static List<Map<String, String>> getAllOptionsOfStakeHolder(String filterDataResponse, String filterName) {
        List<Map<String, String>> allOptionsOfFilter = new ArrayList<>();

        try {
            Boolean isAutoComplete = isFilterOfAutoCompleteType(filterDataResponse, filterName);
            OptionsHelper opHelperObj = new OptionsHelper();

            JSONObject jsonObj = new JSONObject(filterDataResponse);
            JSONArray jsonArr = jsonObj.names();

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject tempObj = jsonObj.getJSONObject(jsonArr.get(i).toString());

                if (tempObj.getString("filterName").trim().equalsIgnoreCase(filterName)) {
                    tempObj = tempObj.getJSONObject("multiselectValues").getJSONObject("OPTIONS");
                    JSONArray roleGroupArr;

                    if (tempObj.has("data"))
                        roleGroupArr = tempObj.getJSONArray("data");
                    else if (tempObj.has("DATA"))
                        roleGroupArr = tempObj.getJSONArray("DATA");
                    else {
                        logger.error("Couldn't find Role Group Array for Filter {}", filterName);
                        return allOptionsOfFilter;
                    }

                    for (int j = 0; j < roleGroupArr.length(); j++) {
                        JSONObject roleGroupObj = roleGroupArr.getJSONObject(j);
                        String roleGroupId = roleGroupObj.getString("id");
                        String roleGroupName = roleGroupObj.getString("name");
                        JSONArray usersArr = roleGroupObj.getJSONArray("group");

                        if (isAutoComplete != null && isAutoComplete) {
                            String optionsResponse = opHelperObj.hitOptionsForAllStakeholdersOfAGroup(roleGroupId);

                            if (ParseJsonResponse.validJsonResponse(optionsResponse)) {
                                JSONObject optionsObj = new JSONObject(optionsResponse);
                                usersArr = optionsObj.getJSONArray("data");
                            } else {
                                logger.error("Options API Response for Stakeholder Role Group Id {} is an Invalid JSON.", roleGroupId);
                                continue;
                            }
                        }

                        for (int k = 0; k < usersArr.length(); k++) {
                            JSONObject userObj = usersArr.getJSONObject(k);

                            if (userObj.has("idType")) {
                                Map<String, String> optionMap = new HashMap<>();

                                optionMap.put("roleGroupName", roleGroupName);
                                optionMap.put("roleGroupId", roleGroupId);
                                optionMap.put("name", userObj.getString("name"));
                                optionMap.put("id", String.valueOf(userObj.getInt("id")));
                                optionMap.put("idType", String.valueOf(userObj.getInt("idType")));

                                allOptionsOfFilter.add(optionMap);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Options of StakeHolder Filter {}. {}", filterName, e.getStackTrace());
        }
        return allOptionsOfFilter;
    }

    private static List<Map<String, String>> getFilterOptionsToTest(List<Map<String, String>> allOptions, String filterName, Boolean applyRandomization,
                                                                    int maxFilterOptionsToValidate) {
        if (!applyRandomization)
            return allOptions;

        List<Map<String, String>> optionsToTest = new ArrayList<>();

        try {
            int[] randomNumbersArr = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, maxFilterOptionsToValidate);

            for (int randomNumber : randomNumbersArr) {
                optionsToTest.add(allOptions.get(randomNumber));
            }
        } catch (Exception e) {
            logger.error("Exception while getting Options to test for Filter {}. {}", filterName, e.getStackTrace());
        }
        return optionsToTest;
    }

    //For filters of Type Select, Multi-Select, AutoComplete, Stakeholder
    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                      int listDataSize, String showPageObjectName, CustomAssert csAssert) {
        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, null, null,
                null, null, showPageObjectName, csAssert);
    }

    //For filters of Type Date
    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                      int listDataSize, String startDate, String endDate, String showPageObjectName, CustomAssert csAssert) {
        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, startDate, endDate,
                null, null, showPageObjectName, csAssert);
    }

    //For filters of Type Slider
    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String minSliderValue, String maxSliderValue, String entityName,
                                      int entityTypeId, int listDataOffset, int listDataSize, String showPageObjectName, CustomAssert csAssert) {
        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, null, null,
                minSliderValue, maxSliderValue, showPageObjectName, csAssert);
    }

    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                      int listDataSize, String startDate, String endDate, String minSliderValue, String maxSliderValue, String showPageObjectName,
                                      CustomAssert csAssert) {
        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, startDate, endDate, minSliderValue, maxSliderValue,
                true, 3, 3, showPageObjectName, csAssert);
    }

    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                      int listDataSize, String startDate, String endDate, String minSliderValue, String maxSliderValue, Boolean applyRandomization,
                                      int maxFilterOptionsToValidate, int maxNoOfRecordsToValidate, String showPageObjectName, CustomAssert csAssert) {

        String filterType = getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();
        String expectedValue;

        switch (filterType) {
            case "slider":
                expectedValue = minSliderValue + ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter") + maxSliderValue;
                break;

            case "date":
                expectedValue = startDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + endDate;
                break;

            default:
                expectedValue = null;
                break;
        }

        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, expectedValue, applyRandomization,
                maxFilterOptionsToValidate, maxNoOfRecordsToValidate, showPageObjectName, csAssert);
    }


    private static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                       int listDataSize, String expectedValue, Boolean applyRandomization, int maxFilterOptionsToValidate, int maxNoOfRecordsToValidate,
                                       String showPageObjectName, CustomAssert csAssert) {
        try {
            String filterType = getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();
            String filterPayload;

            switch (filterType) {
                case "slider":
                case "date":
                    filterPayload = getFiltersPayload(filterName, filterId, filterType, entityTypeId, listDataOffset, listDataSize, null,
                            expectedValue);

                    hitListDataAndVerifyResultsOnShowPage(filterName, filterPayload, expectedValue, entityName, entityTypeId, filterType, null,
                            showPageObjectName, applyRandomization, maxNoOfRecordsToValidate, csAssert);
                    break;

                default:
                    List<Map<String, String>> allOptions = getAllOptionsOfFilter(filterDataResponse, filterName, filterType, entityTypeId);
                    List<Map<String, String>> allOptionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                    for (Map<String, String> optionToTest : allOptionsToTest) {
                        expectedValue = optionToTest.get("name");
                        filterPayload = getFiltersPayload(filterName, filterId, filterType, entityTypeId, listDataOffset, listDataSize, optionToTest, expectedValue);

                        hitListDataAndVerifyResultsOnShowPage(filterName, filterPayload, expectedValue, entityName, entityTypeId, filterType, optionToTest,
                                showPageObjectName, applyRandomization, maxNoOfRecordsToValidate, csAssert);
                    }
                    break;
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Filter " + filterName + " of Entity " + entityName + ". " + e.getMessage());
        }
    }

    private String getFilterPayload(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                       int listDataSize, String expectedValue, Boolean applyRandomization, int maxFilterOptionsToValidate, CustomAssert csAssert) {

        String filterPayload = null;
        try {
            String filterType = getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();


            switch (filterType) {
                case "slider":
                case "date":
                    filterPayload = getFiltersPayload(filterName, filterId, filterType, entityTypeId, listDataOffset, listDataSize, null,
                            expectedValue);

                    break;

                default:
                    List<Map<String, String>> allOptions = getAllOptionsOfFilter(filterDataResponse, filterName, filterType, entityTypeId);
                    List<Map<String, String>> allOptionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                    for (Map<String, String> optionToTest : allOptionsToTest) {
                        expectedValue = optionToTest.get("name");
                        filterPayload = getFiltersPayload(filterName, filterId, filterType, entityTypeId, listDataOffset, listDataSize, optionToTest, expectedValue);

                    }
                    break;
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while creating Filter Payload for Filter " + filterName + " of Entity " + entityName + ". " + e.getMessage());
        }
        return filterPayload;
    }

    public static void validateMultipleFilters(String filterDataResponse, List<String> filterNamesList, List<Integer> filterIdsList, String entityName, int entityTypeId,
                                               int listDataOffset, int listDataSize, List<String> expectedValuesList, Boolean applyRandomization,
                                               int maxFilterOptionsToValidate, int maxNoOfRecordsToValidate, List<String> showPageObjectNamesList, String startDate, String endDate, String minSliderValue, String maxSliderValue, CustomAssert csAssert) {

        try {
            String completePayload = getFiltersPayloadPrefix(entityTypeId, listDataOffset, listDataSize);
            List<String> filterTypesList = new ArrayList<>();
            List<Map<String, String>> optionMapsList = new ArrayList<>();
            String filterType;
            String filterPayload = "";
            String expectedValue = "";
            for (int i = 0; i < filterNamesList.size(); i++) {

                filterType = getFilterType(filterDataResponse, filterNamesList.get(i), filterIdsList.get(i)).trim().toLowerCase();
                filterTypesList.add(filterType);

                switch (filterType) {

                    case "slider":
                        expectedValue = minSliderValue + ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter") + maxSliderValue;
                        expectedValuesList.add(i, expectedValue);
                        filterPayload = getPayloadForSingleFilter(filterNamesList.get(i), filterIdsList.get(i), filterType, entityTypeId, null,
                                expectedValue);
                        Map<String, String> sliderOptions = new HashMap<>();
                        sliderOptions.put(filterNamesList.get(i), expectedValue);
                        optionMapsList.add(sliderOptions);
                        break;

                    case "date":
                        expectedValue = startDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + endDate;
                        expectedValuesList.add(i, expectedValue);
                        filterPayload = getPayloadForSingleFilter(filterNamesList.get(i), filterIdsList.get(i), filterType, entityTypeId, null,
                                expectedValue);
                        Map<String, String> dateOptions = new HashMap<>();
                        dateOptions.put(filterNamesList.get(i), expectedValue);
                        optionMapsList.add(dateOptions);

                        break;

                    default:
                        List<Map<String, String>> allOptions = getAllOptionsOfFilter(filterDataResponse, filterNamesList.get(i), filterType, entityTypeId);

                        if (allOptions != null && !allOptions.isEmpty()) {
                            int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
                            Map<String, String> optionToTest = allOptions.get(randomNumber);
                            optionMapsList.add(optionToTest);
                            expectedValuesList.add(i, optionToTest.get("name"));
                            filterPayload = getPayloadForSingleFilter(filterNamesList.get(i), filterIdsList.get(i), filterType, entityTypeId, optionToTest,
                                    expectedValuesList.get(i));
                        } else {
                            logger.debug("Option Value is null so skipping the test");
                            throw new SkipException("Option Value is null for filter " + filterNamesList.get(i) + " so skipping the test");
                        }
                        break;
                }

                completePayload = completePayload.concat(filterPayload);
            }

            completePayload = completePayload.substring(0, completePayload.length() - 1);
            completePayload = completePayload.concat(getFiltersPayloadSuffix());

            hitListDataAndVerifyResultsOnShowPage(filterNamesList, completePayload, expectedValuesList, entityName, entityTypeId, filterTypesList, optionMapsList,
                    showPageObjectNamesList, applyRandomization, maxNoOfRecordsToValidate, csAssert);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Multiple Filters [" + filterNamesList.toString() + "] of Entity " +
                    entityName + ". " + e.getMessage());
        }
    }

    private static void hitListDataAndVerifyResultsOnShowPage(String filterName, String filterPayload, String expectedValue, String entityName, int entityTypeId,
                                                              String filterType, Map<String, String> optionMap, String showPageObjectName, Boolean applyRandomization,
                                                              int maxNoOfRecordsToValidate, CustomAssert csAssert) {
        List<String> filterNamesList = new ArrayList<>();
        filterNamesList.add(filterName);

        List<String> expectedValuesList = new ArrayList<>();
        expectedValuesList.add(expectedValue.trim());

        List<String> filterTypesList = new ArrayList<>();
        filterTypesList.add(filterType);

        List<Map<String, String>> optionsMapList = new ArrayList<>();
        optionsMapList.add(optionMap);

        List<String> showPageObjectNamesList = new ArrayList<>();
        showPageObjectNamesList.add(showPageObjectName);

        hitListDataAndVerifyResultsOnShowPage(filterNamesList, filterPayload, expectedValuesList, entityName, entityTypeId, filterTypesList, optionsMapList,
                showPageObjectNamesList, applyRandomization, maxNoOfRecordsToValidate, csAssert);
    }

    private static void hitListDataAndVerifyResultsOnShowPage(List<String> filterNamesList, String filterPayload, List<String> expectedValuesList, String entityName,
                                                              int entityTypeId, List<String> filterTypesList, List<Map<String, String>> optionMapsList,
                                                              List<String> showPageObjectNamesList, Boolean applyRandomization, int maxNoOfRecordsToValidate,
                                                              CustomAssert csAssert) {
        if (filterPayload == null) {
            csAssert.assertTrue(false, "Couldn't create Payload for Filter " + filterNamesList.toString() + " of Entity " + entityName);
            return;
        }

        int listId = ConfigureConstantFields.getListIdForEntity(entityName);
        String filterName;
        ListRendererListData listDataObj = new ListRendererListData();

        listDataObj.hitListRendererListData(listId, filterPayload);
        String listDataResponse = listDataObj.getListDataJsonStr();


        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
        /*if (ListDataHelper.isListDownloadable(listDataResponse)) {

            if (validateListDataDownloadedExcelFilterSheet(listId, filterNamesList, expectedValuesList, filterTypesList, filterPayload, entityTypeId, entityName, csAssert)) {
                logger.info("Filter sheet validated successfully for entity " + entityName);
            } else {
                logger.error("Filter sheet validated unsuccessfully for entity " + entityName);
                csAssert.assertTrue(false, "Filter sheet validated unsuccessfully for entity " + entityName);
            }
        } else {
            logger.info("List data size is not adequate for download");
        }*/

            for (int i = 0; i < filterNamesList.size(); i++) {

                filterName = filterNamesList.get(i);

                List<Map<Integer, Map<String, String>>> allRecordsToValidate = ListRendererFilterDataHelper.getListDataResultsToValidate(listDataResponse,
                        filterNamesList.get(i), expectedValuesList.get(i), entityTypeId, csAssert, applyRandomization, maxNoOfRecordsToValidate);

                if (filterName.equals("createdDate") || filterName.equals("modifiedDate") || filterName.equals("activeDate") ||filterName.equalsIgnoreCase("reviewDate")) {

                    verifyResultsForDateTypeListingPage(allRecordsToValidate, filterName, expectedValuesList.get(i), csAssert);

                }
                else if (filterName.equals("vendorHierarchy")&&!entityName.equalsIgnoreCase("suppliers")&&!entityName.equalsIgnoreCase("vendors"))
                {
                    verifyResultsOthersListingPage(entityName,allRecordsToValidate, filterName, expectedValuesList.get(i), csAssert);
                }
                else if (filterName.equals("createdby") || filterName.equals("lastModifiedBy") || filterName.equalsIgnoreCase("multisuppliercontracts") ||(entityName.equalsIgnoreCase("child Service levels")&&filterName.equalsIgnoreCase("reportingfrequency"))) {

                    verifyResultsOthersListingPage(entityName,allRecordsToValidate, filterName, expectedValuesList.get(i), csAssert);
                } else {
                    if(filterName.equals("vendorHierarchy")&&(entityName.equalsIgnoreCase("suppliers")|| entityName.equalsIgnoreCase("vendors"))) {
                        String []arr=expectedValuesList.get(i).split(" ");
                        expectedValuesList.set(i,expectedValuesList.get(i).substring(0,expectedValuesList.get(i).length()-arr[arr.length-1].length()).trim());
                    }
                    verifyResultsOnShowPage(allRecordsToValidate, filterPayload, listDataResponse, filterNamesList.get(i), expectedValuesList.get(i), showPageObjectNamesList.get(i),
                            entityTypeId, filterTypesList.get(i), optionMapsList.get(i), csAssert);
                }
            }
        }
        else
            {
                csAssert.assertTrue(false, "listDataResponse is invalid response for entity name { " +entityName + " } and list id { " +listId+" } , applied filter payload { "+filterPayload+ " } and api name /listRenderer/list/"+listId+"/listdata?isFirstCall=false");

            }
        csAssert.assertAll();
    }

    private static Boolean validateListDataDownloadedExcelFilterSheet(int listId, List<String> filterNamesList,
                                                                      List<String> expectedValuesList, List<String> filterTypeList, String filterPayload,
                                                                      int entityTypeId, String entityName, CustomAssert csAssert) {

        boolean validationStatus = true;
        try {

            ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
            LinkedHashMap<String, String> filterNamesTobeCheckedInExcel = new LinkedHashMap<>();

            listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listId);
            String listRendererDefaultUserListMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
            String filterValue;
            int i = 0;

            DefaultUserListMetadataHelper defaultHelperObj = new DefaultUserListMetadataHelper();

            for (String filter : filterNamesList) {
                filterValue = expectedValuesList.get(i);
                filterNamesTobeCheckedInExcel.put(defaultHelperObj.getFilterName(listRendererDefaultUserListMetaDataResponse, filter), filterValue);
                i++;
            }

            JSONObject filterPayloadJson = new JSONObject(filterPayload);

            filterPayload = filterPayloadJson.getJSONObject("filterMap").getJSONObject("filterJson").toString();
            filterPayload = filterPayload.substring(1, filterPayload.length() - 1);

            String excelFileDownloadPath = ListDataHelper.downloadListDataForAllColumns(filterPayload, entityTypeId, entityName, listId, csAssert);

            if (excelFileDownloadPath == null) {
                logger.error("Unable to download the list data for entity " + entityName);
                validationStatus = false;
                return validationStatus;
            } else {
                String[] excelFilePathSplit = excelFileDownloadPath.split("/");
                int excelFilePathSplitLength = excelFilePathSplit.length;
                String excelFileName = excelFilePathSplit[excelFilePathSplitLength - 1];
                String excelFilePath = "";

                for (i = 0; i < excelFilePathSplitLength - 1; i++) {

                    if (i < (excelFilePathSplitLength - 2)) {
                        excelFilePath = excelFilePath.concat(excelFilePathSplit[i] + "/");
                    } else {
                        excelFilePath = excelFilePath.concat(excelFilePathSplit[i]);
                    }
                }

                List<String> excelFilterColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Filter", 4);
                List<String> excelFilterColumnValues = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, "Filter", 5);

                String filterColumnName;
                String filterColumnValue;
                String startDateInExcel;
                String endDateInExcel;
                String filterType;
                String[] filterColumnValueDates;
                String[] filterColumnValueSlider;
                int filterPosition;
                i = 0;

                for (Map.Entry<String, String> filterNameValue : filterNamesTobeCheckedInExcel.entrySet()) {
                    filterColumnName = filterNameValue.getKey();
                    filterColumnValue = filterNameValue.getValue();
                    filterType = filterTypeList.get(i);

                    if (filterColumnName.equalsIgnoreCase("Created On") || filterColumnName.equalsIgnoreCase("Last Modified Date")) {
                        filterColumnValueDates = filterColumnValue.split(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter"));
                        startDateInExcel = DateUtils.convertDateTo_YYYYMMMDD_From_MM_DD_YYYY(filterColumnValueDates[0]);
                        endDateInExcel = DateUtils.convertDateTo_YYYYMMMDD_From_MM_DD_YYYY(filterColumnValueDates[1]);
                        filterColumnValue = startDateInExcel + " - \n " + endDateInExcel;
                    }

                    if (filterType.equals("slider")) {
                        filterColumnValueSlider = filterColumnValue.split(ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter"));
                        filterColumnValue = filterColumnValueSlider[0] + " - \n " + filterColumnValueSlider[1];
                    }

                    if (excelFilterColumnNames.contains(filterColumnName)) {

                        filterPosition = excelFilterColumnNames.indexOf(filterColumnName);
                        if (excelFilterColumnValues.get(filterPosition).equals(filterColumnValue)) {
                            logger.info("Expected and Actual value for filter " + filterColumnName + " are equal");
                        } else {
                            logger.error("Expected and Actual value for filter " + filterColumnName + " are not equal");
                            csAssert.assertTrue(false, "Expected and Actual value for filter " + filterColumnName + " are not equal");
                            validationStatus = false;
                        }

                    } else {
                        logger.error(filterColumnName + " filter column not found in the downloaded excel");
                        csAssert.assertTrue(false, filterColumnName + " filter column not found in the downloaded excel");
                        validationStatus = false;
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating excel filter sheet ");
            csAssert.assertTrue(false, "Exception while validating excel filter sheet ");
            validationStatus = false;
        }
        return validationStatus;
    }

    private static void verifyResultsOthersListingPage(String entityName,List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterName, String expectedValue, CustomAssert csAssert) {

        String actualValue;
        String columnNameToCheck = "";

        if (filterName.equals("createdby")) {
            columnNameToCheck = "createdby";
        } else if (filterName.equals("lastmodifiedby")) {
            columnNameToCheck = "lastmodifiedby";
        }
        else if(filterName.equalsIgnoreCase("reportingfrequency"))
        {
            columnNameToCheck="reporting_frequency";
        }
        else {
            columnNameToCheck=filterName.toLowerCase();
        }

        for (Map<Integer, Map<String, String>> singleRecord : allRecordsToValidate) {

            singleRecordLoop:
            for (Map.Entry<Integer, Map<String, String>> singleRecordColumnData : singleRecord.entrySet()) {

                if (singleRecordColumnData.getValue().get("columnName").equals(columnNameToCheck)) {
                    actualValue = singleRecordColumnData.getValue().get("value");
                    if (filterName.equalsIgnoreCase("vendorHierarchy"))
                    {
                             String []arr=expectedValue.split(" ");
                             expectedValue=expectedValue.substring(0,expectedValue.length()-arr[arr.length-1].length()).trim();
                        if (!actualValue.contains(expectedValue.trim())) {
                            csAssert.assertTrue(false, "Actual value :: " + actualValue + " and Expected value :: " + expectedValue + " mismatch for filter " + filterName);
                        }
                    }
                    else {
                        if (!actualValue.equals(expectedValue.trim())) {
                            csAssert.assertTrue(false, "Actual value :: " + actualValue + " and Expected value :: " + expectedValue + " mismatch for filter " + filterName);
                        }
                    }
                    break singleRecordLoop;
                }
            }
        }
    }

    private static void verifyResultsForDateTypeListingPage(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterName, String expectedValue, CustomAssert csAssert) {

        String actualValue;
        String columnNameToCheck = "";

        if (filterName.equals("createdDate")) {
            columnNameToCheck = "datecreated";
        } else if (filterName.equals("modifiedDate")) {
            columnNameToCheck = "datemodified";
        }
        else if(filterName.equals("activeDate"))
        {
            columnNameToCheck="activedate";
        }
        else
        {
            columnNameToCheck=filterName.toLowerCase();
        }
        String dateRangeDelimiter = ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter");
        String upperDate = "";
        String lowerDate = "";

        if (expectedValue.contains(dateRangeDelimiter)) {
            String[] temp = expectedValue.split(Pattern.quote(dateRangeDelimiter));
            lowerDate = temp[0].replaceAll("-", "/");
            upperDate = temp[1].replaceAll("-", "/");
            if (lowerDate.contains("-") || upperDate.contains("-")) {
                lowerDate = DateUtils.convertDateTo_MMM_DD_YYYY_From_MM_DD_YYYY(lowerDate, "-");
                upperDate = DateUtils.convertDateTo_MMM_DD_YYYY_From_MM_DD_YYYY(upperDate, "-");
            } else if (lowerDate.contains("/") || upperDate.contains("/")) {
                lowerDate = DateUtils.convertDateTo_MMM_DD_YYYY_From_MM_DD_YYYY(lowerDate, "/");
                upperDate = DateUtils.convertDateTo_MMM_DD_YYYY_From_MM_DD_YYYY(upperDate, "/");
            }

        }
        for (Map<Integer, Map<String, String>> singleRecord : allRecordsToValidate) {

            singleRecordLoop:
            for (Map.Entry<Integer, Map<String, String>> singleRecordColumnData : singleRecord.entrySet()) {

                if (singleRecordColumnData.getValue().get("columnName").equals(columnNameToCheck)) {
                    actualValue = singleRecordColumnData.getValue().get("value");
                    try {

                        if (!DateUtils.isDateWithinRange(actualValue, lowerDate, upperDate, "MMM-DD-YYYY")) {
                            csAssert.assertTrue(false, "Date doesn't lies in the range for filter " + filterName);
                        }
                    } catch (Exception e) {
                        csAssert.assertTrue(false, "Exception while verifying filter " + filterName);
                    }
                    break singleRecordLoop;
                }
            }
        }
    }

    private static void verifyResultsOnShowPage(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterPayload, String listDataResponse, String filterName,
                                                String expectedValue, String showPageObjectName, int entityTypeId, String filterType, Map<String, String> optionMap,
                                                CustomAssert csAssert) {
        try {
            switch (filterType) {
                default:
                    verifyResults(allRecordsToValidate, filterPayload, listDataResponse, filterName, expectedValue, showPageObjectName, entityTypeId, csAssert, filterType);
                    break;

                case "stakeholder":
                    String roleGroupId = optionMap.get("roleGroupId");
                    String optionId = optionMap.get("id");
                    verifyResultsForStakeholders(allRecordsToValidate, listDataResponse, filterName, expectedValue, optionId, entityTypeId, csAssert,
                            roleGroupId);
                    break;

                case "date":
                    verifyResultsForDateType(allRecordsToValidate, listDataResponse, filterName, expectedValue, showPageObjectName, entityTypeId,
                            csAssert);
                    break;
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Results on Show Page for Filter " + filterName + " of EntityTypeId " +
                    entityTypeId + ". " + e.getMessage());
        }
    }

    private static List<Map<String, String>> getAllOptionsOfFilter(String filterDataResponse, String filterName, String filterType, int entityTypeId) {
        List<Map<String, String>> allOptions;

        try {
            switch (filterType) {
                case "select":
                case "singleselect":
                case "multiselect":
                case "checkbox":
                    allOptions = getAllOptionsOfFilter(filterDataResponse, filterName);
                    break;

                case "autocomplete":
                    allOptions = getAllOptionsOfAutoCompleteFilter(filterName, entityTypeId);
                    break;

                case "stakeholder":
                    allOptions = getAllOptionsOfStakeHolder(filterDataResponse, filterName);
                    break;

                default:
                    return null;
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Options for Filter {}. {}", filterName, e.getStackTrace());
            return null;
        }

        return allOptions;
    }

    private static String getFiltersPayload(String filterName, Integer filterId, String filterType, int entityTypeId, int listDataOffset,
                                            int listDataSize, Map<String, String> optionMap, String expectedValue) {
        List<String> filterNamesList = new ArrayList<>();
        filterNamesList.add(filterName);

        List<Integer> filterIdsList = new ArrayList<>();
        filterIdsList.add(filterId);

        List<String> filterTypesList = new ArrayList<>();
        filterTypesList.add(filterType);

        List<Map<String, String>> optionMapsList = new ArrayList<>();
        optionMapsList.add(optionMap);

        List<String> expectedValuesList = new ArrayList<>();
        expectedValuesList.add(expectedValue);

        return getFiltersPayload(filterNamesList, filterIdsList, filterTypesList, entityTypeId, listDataOffset, listDataSize, optionMapsList, expectedValuesList);
    }

    private static String getFiltersPayload(List<String> filterNamesList, List<Integer> filterIdsList, List<String> filterTypesList, int entityTypeId, int listDataOffset,
                                            int listDataSize, List<Map<String, String>> optionMapsList, List<String> expectedValuesList) {
        String completePayload = getFiltersPayloadPrefix(entityTypeId, listDataOffset, listDataSize);

        for (int i = 0; i < filterNamesList.size(); i++) {
            String payload = getPayloadForSingleFilter(filterNamesList.get(i), filterIdsList.get(i), filterTypesList.get(i), entityTypeId, optionMapsList.get(i),
                    expectedValuesList.get(i));

            if (payload == null)
                return null;

            completePayload = completePayload.concat(payload);
        }

        completePayload = completePayload.substring(0, completePayload.length() - 1);
        completePayload = completePayload.concat(getFiltersPayloadSuffix());

        return completePayload;
    }

    public static String getPayloadForSingleFilter(String filterName, Integer filterId, String filterType, int entityTypeId,
                                                   Map<String, String> optionMap, String expectedValue) {
        String payload;

        try {
            switch (filterType) {
                case "select":
                case "singleselect":
                case "multiselect":
                case "autocomplete":
                case "checkbox":
                    payload = getPayloadForSelectType(filterName, filterId, optionMap);
                    break;

                case "stakeholder":
                    payload = getPayloadForStakeHolder(filterName, filterId, optionMap);
                    break;

                case "date":
                    payload = getPayloadForDateType(filterName, filterId, expectedValue);
                    break;

                case "slider":
                    payload = getPayloadForSliderType(filterName, filterId, expectedValue);
                    break;

                default:
                    logger.warn("Filter of Type {} is not supported.", filterType);
                    return null;
            }

            if (payload != null)
                payload = payload.concat(",");
        } catch (Exception e) {
            logger.error("Exception while Creating Payload for Filter {} of EntityTypeId {}. {}", filterName, entityTypeId, e.getStackTrace());
            return null;
        }
        return payload;
    }

	/*private static void validateFiltersOfSelectTypeWithAuditLog(String filterDataResponse, String filterName, Integer filterId, int entityTypeId, Boolean applyRandomization,
	                                                            int maxFilterOptionsToValidate, int maxRecordsToValidate, int listDataOffset, int listDataSize,
	                                                            CustomAssert csAssert) {
		try {
			List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

			if (allOptions.size() > 0) {
				List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

				for (Map<String, String> filterOption : optionsToTest) {
					*//*String optionName = filterOption.get("name");
					String payload = getCompletePayloadForFilters(entityTypeId, listDataOffset, listDataSize, getPayloadForSelectType(filterName, filterId, filterOption));

					if (payload != null) {
						*//**//*hitListDataAndVerifyResultsWithAuditLog(filterName, optionName, payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate,
								null, null);*//**//*
					} else {
						throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
					}*//*
				}
			} else {
				throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
		}
	}

	private static void validateFiltersOfDateTypeWithAuditLog(String filterName, Integer filterId, int entityTypeId, Boolean applyRandomization,
	                                                          int maxRecordsToValidate, int listDataOffset, int listDataSize, String startDate, String endDate,
	                                                          String expectedDateFormat, String auditLogDateFormat, CustomAssert csAssert) {
		try {
			*//*String payload = getCompletePayloadForFilters(entityTypeId, listDataOffset, listDataSize, getPayloadForDateType(filterName, filterId, startDate, endDate));

			String fromDate = startDate;
			Date date = new SimpleDateFormat("MM-dd-yyyy").parse(fromDate);
			fromDate = new SimpleDateFormat(expectedDateFormat).format(date);

			String toDate = endDate;
			date = new SimpleDateFormat("MM-dd-yyyy").parse(toDate);
			toDate = new SimpleDateFormat(expectedDateFormat).format(date);

			String expectedDateRange = fromDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + toDate;

			if (payload != null) {
				*//**//*hitListDataAndVerifyResultsWithAuditLog(filterName, expectedDateRange, payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate,
						expectedDateFormat, auditLogDateFormat);*//**//*
			} else {
				throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Date Range " + expectedDateRange + ". Hence skipping test");
			}*//*
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Date. " + e.getMessage());
		}
	}*/

    public static String getFiltersPayloadPrefix(int entityTypeId, int listDataOffset, int listDataSize) {
        return "{\"selectedColumns\": [],\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{";
    }

    public static String getFiltersPayloadSuffix() {
        return "}}}";
    }

    private static String getPayloadForSelectType(String filterName, Integer filterId, Map<String, String> optionMap) {
        String payload;

        try {
            logger.info("Creating Payload for Filter {} and Option {}.", filterName, optionMap.get("name"));

            payload = "\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" +
                    optionMap.get("id") + "\",\"name\":\"" + optionMap.get("name") + "\"}]}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
            return null;
        }
        return payload;
    }

    private static String getPayloadForStakeHolder(String filterName, Integer filterId, Map<String, String> optionMap) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {} and Option {}.", filterName, optionMap.get("name"));

            String userName = optionMap.get("name");
            String userId = optionMap.get("id");
            String roleGroupId = optionMap.get("roleGroupId");
            String roleGroupName = optionMap.get("roleGroupName");
            String idType = optionMap.get("idType");

            payload = "\"" + filterId + "\":{\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" +
                    roleGroupId + "\",\"name\":\"" + roleGroupName + "\",\"group\":[{\"name\":\"" + userName + "\",\"id\":" + userId + ",\"idType\":" + idType +
                    ",\"selected\":true}],\"type\":" + optionMap.get("typeId") + "},{\"name\":\"" + userName + "\",\"id\":" + userId + ",\"idType\":" + idType +
                    ",\"selected\":true}]},\"uitype\":\"STAKEHOLDER\"}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForDateType(String filterName, Integer filterId, String expectedValue) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {}", filterName);

            String[] dateValues = expectedValue.split(Pattern.quote(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter")));
            String startDate = dateValues[0];
            String endDate = dateValues[1];

            payload = "\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"" + filterName + "\",\"start\":\"" + startDate + "\",\"end\":\"" + endDate +
                    "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForSliderType(String filterName, Integer filterId, String expectedValue) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {}", filterName);
            String[] sliderValues = expectedValue.split(Pattern.quote(ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter")));
            String minSliderValue = sliderValues[0];
            String maxSliderValue = sliderValues[1];

            payload = "\"" + filterId + "\":{\"filterId\":\"" + filterId + "\",\"filterName\":\"" + filterName + "\",\"min\":\"" + minSliderValue +
                    "\",\"max\":\"" + maxSliderValue + "\"}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
        }
        return payload;
    }

    private static void verifyResultsForStakeholders(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String listDataResponse, String filterName,
                                                     String expectedValue, String optionId, int entityTypeId, CustomAssert csAssert, String roleGroupId) {
        try {
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, expectedValue, allRecordsToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                Show showObj = new Show();
                showObj.hitShow(entityTypeId, recordId);
                String showResponse = showObj.getShowJsonStr();

                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                    JSONObject jsonObj = new JSONObject(showResponse);
                    String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

                    if (showStatus.trim().equalsIgnoreCase("success")) {
                        String stakeHolderJsonStr = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").toString();

                        if (stakeHolderJsonStr != null) {
                            JSONObject stakeHolderJson = new JSONObject(stakeHolderJsonStr);
                            stakeHolderJson = stakeHolderJson.getJSONObject("values");

                            if (stakeHolderJson.has("rg_" + roleGroupId)) {
                                JSONArray stakeHolderJsonArr = stakeHolderJson.getJSONObject("rg_" + roleGroupId).getJSONArray("values");
                                boolean result = false;

                                for (int i = 0; i < stakeHolderJsonArr.length(); i++) {
                                    if (stakeHolderJsonArr.getJSONObject(i).has("idTypes")) {
                                        int userGroupId = stakeHolderJsonArr.getJSONObject(i).getInt("id");

                                        logger.info("Hitting UserGroups Users API for UserGroup  Id {}, Entity Type Id {} and Record Id {}", userGroupId, entityTypeId,
                                                recordId);
                                        UserGroupsUsers userObj = new UserGroupsUsers();
                                        String userResponse = userObj.hitUserGroupsUsers(userGroupId, entityTypeId, recordId);

                                        if (ParseJsonResponse.validJsonResponse(userResponse)) {
                                            JSONArray userJsonArr = new JSONArray(userResponse);

                                            for (int j = 0; j < userJsonArr.length(); j++) {
                                                if (userJsonArr.getJSONObject(j).get("id").toString().trim().equalsIgnoreCase(optionId.trim())) {
                                                    result = true;
                                                    break;
                                                }
                                            }
                                        } else {
                                            csAssert.assertTrue(false, "UserGroups Users API Response for UserGroup Id " + userGroupId +
                                                    ", Entity Type Id " + entityTypeId + " and Record Id " + recordId + " is an Invalid JSON. " +
                                                    ParseJsonResponse.getHTMLResponseReason(userResponse));
                                        }
                                    } else if (stakeHolderJsonArr.getJSONObject(i).get("id").toString().trim().equalsIgnoreCase(optionId.trim())) {
                                        result = true;
                                    }

                                    if (result) {
                                        break;
                                    }
                                }

                                if (!result) {
                                    logger.error("Show Page validation failed for Record Id {} of Filter {}, Role Group {} and Option {}", recordId, filterName, roleGroupId,
                                            expectedValue);
                                    csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                            " of Filter " + filterName + ", Role Group " + roleGroupId + " and Option " + expectedValue);
                                }
                            } else {
                                logger.error("Couldn't find Role Group [{}] in Show Response for Record Id {} of Filter {} and Option {}", "rg_" + roleGroupId, recordId,
                                        filterName, expectedValue);
                                csAssert.assertTrue(false, "Couldn't find Role Group [rg_" + roleGroupId + "] in Show Response for Record Id " +
                                        recordId + " of Filter " + filterName + " and Option " + expectedValue);
                            }
                        } else {
                            logger.error("Couldn't get Stakeholders Json from Show Response for Record Id {} of Filter {} and Option {}", recordId, filterName,
                                    expectedValue);
                            csAssert.assertTrue(false, "Couldn't get Stakeholders Json from Show Response for Record Id " + recordId + " of Filter " +
                                    filterName + " and Option " + expectedValue);
                        }
                    } else {
                        logger.error("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
						/*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName +
								" and Option " + expectedValue);*/
                    }
                } else {
                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
                            filterName + " and Option " + expectedValue + " is an Invalid JSON. " + ParseJsonResponse.getHTMLResponseReason(showResponse));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

    private static void verifyResultsForDateType(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String listDataResponse, String filterName,
                                                 String expectedValue, String showPageObjectName, int entityTypeId, CustomAssert csAssert) {
        try {
            String[] dateValues = expectedValue.split(Pattern.quote(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter")));
            String startDate = dateValues[0];
            String endDate = dateValues[1];

            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, expectedValue, allRecordsToValidate.size());

            String expectedDateRange = null;
            String expectedDateFormat = null;

            String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageObjectName, entityTypeId);
            String lastObjectName = ShowHelper.getLastObjectNameFromHierarchy(fieldHierarchy);

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                Show showObj = new Show();
                showObj.hitShow(entityTypeId, recordId);
                String showResponse = showObj.getShowJsonStr();

                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                    boolean result = false;

                    JSONObject jsonObj = new JSONObject(showResponse);
                    String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

                    if (showStatus.trim().equalsIgnoreCase("success")) {
                        if (expectedDateFormat == null) {
                            expectedDateFormat = ShowHelper.getExpectedDateFormat(showResponse, lastObjectName, fieldHierarchy);
                        }

                        if (expectedDateFormat != null) {

                            if (expectedDateRange == null) {
                                String fromDate = startDate;
                                Date date = new SimpleDateFormat("MM-dd-yyyy").parse(fromDate);
                                fromDate = new SimpleDateFormat(expectedDateFormat).format(date);

                                String toDate = endDate;
                                date = new SimpleDateFormat("MM-dd-yyyy").parse(toDate);
                                toDate = new SimpleDateFormat(expectedDateFormat).format(date);

                                expectedDateRange = fromDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + toDate;
                            }

                            result = showObj.verifyShowField(showResponse, showPageObjectName, expectedDateRange, entityTypeId, "date", expectedDateFormat);
                        } else {
                            logger.error("Couldn't get Expected Date Format for Filter {}, Option {} and Record Id {}", filterName, expectedValue, recordId);
                        }

                        if (!result) {
                            logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                            csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + expectedValue);
                        }
                    } else {
                        logger.error("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
						/*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName +
								" and Option " + expectedValue);*/
                    }
                } else {
                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
                            filterName + " and Option " + expectedValue + " is an Invalid JSON. " + ParseJsonResponse.getHTMLResponseReason(showResponse));
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

    private static void verifyResults(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterPayload, String listDataResponse, String filterName,
                                      String expectedValue, String showPageObjectName, int entityTypeId, CustomAssert csAssert, String filterType) {
        try {
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, expectedValue, allRecordsToValidate.size());

            int vendorEntityTypeId = ConfigureConstantFields.getEntityIdByName("vendors");
            int obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

            Show showObj = new Show();
            String showResponse;

            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

            //Special case for Vendor Hierarchy Filter
            if (entityTypeId != vendorEntityTypeId && filterName.equalsIgnoreCase("vendorHierarchy")) {
                validateVendorHierarchyFilter(allRecordsToValidate, listDataResponse, filterName, expectedValue, showPageObjectName, entityTypeId, csAssert, filterType);
                return;
            }

            if (filterName.equalsIgnoreCase("source")) {
                validateSourceFilter(allRecordsToValidate, filterPayload, listDataResponse, filterName, expectedValue, entityTypeId, csAssert, filterType);
                return;
            }

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int recordEntityTypeId = entityTypeId;
                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                if (entityTypeId == ConfigureConstantFields.getEntityIdByName("invoice line item") &&
                        filterName.equalsIgnoreCase("invoiceFilter")) {
                    logger.info("Hitting Show API for Record Id {} of Invoice Line Item.", recordId);
                    showObj.hitShow(entityTypeId, recordId);
                    showResponse = showObj.getShowJsonStr();

                    if (ShowHelper.isShowPageAccessible(showResponse)) {
                        JSONObject jsonObj = new JSONObject(showResponse);
                        recordId = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("parentShortCodeId").getJSONObject("values").getInt("id");
                        recordEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
                        showPageObjectName = "name";
                    }
                }

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                showObj.hitShow(recordEntityTypeId, recordId);
                showResponse = showObj.getShowJsonStr();

                if (!ShowHelper.isShowPageAccessible(showResponse)) {
//                        csAssert.assertTrue(false, "Show Page is Not Accessible for Record Id " + recordId + " of Entity Type Id " + entityTypeId);
                    return;
                }

                //added by gaurav bhadani on 24 july
                if (entityTypeId == obligationEntityTypeId && filterName.equalsIgnoreCase("documentType")) {
                    int parentrecordEntityTypeId = ShowHelper.getParentEntityTypeId(showResponse);
                    int parentrecordId = ShowHelper.getParentEntityId(showResponse);
                    showObj.hitShow(parentrecordEntityTypeId, parentrecordId);
                    showResponse = showObj.getShowJsonStr();
                }

                //Special Handling for Filters Regions, Countries of Invoice Line Item
                if ((filterName.equalsIgnoreCase("regions") || filterName.equalsIgnoreCase("countries")) &&
                        entityTypeId == 165) {
                    int serviceDataRecordId = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data")
                            .getJSONObject("serviceIdSupplier").getJSONObject("values").getInt("id");
                    showResponse = ShowHelper.getShowResponse(64, serviceDataRecordId);
                    recordEntityTypeId = 64;
                }

                if (ParseJsonResponse.validJsonResponse(showResponse)) {
                    boolean result;

                    JSONObject jsonObj = new JSONObject(showResponse);
                    String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

                    if (showStatus.trim().equalsIgnoreCase("success")) {
                        result = showObj.verifyShowField(showResponse, showPageObjectName, expectedValue, recordEntityTypeId, filterType.trim());

                        if (!result) {
                            //Special code for Regions field.
                            if (showPageObjectName.trim().equalsIgnoreCase("globalRegions") ||
                                    showPageObjectName.trim().equalsIgnoreCase("contractRegions")) {
                                showPageObjectName = showPageObjectName.trim().equalsIgnoreCase("globalRegions") ? "contractregions" : "globalregions";

                                result = showObj.verifyShowField(showResponse, showPageObjectName, expectedValue, recordEntityTypeId, filterType.trim());
                            } else if (showPageObjectName.trim().equalsIgnoreCase("aging")) {
                                //Special code for Aging field.
                                showPageObjectName = "agingvalue";
                                result = showObj.verifyShowField(showResponse, showPageObjectName, expectedValue, recordEntityTypeId, filterType.trim());
                            }
                        }

                        if (!result) {
                            logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                            csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + expectedValue);
                        }
                    } else {
                        logger.warn("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, expectedValue);
                        /*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName + " and Option " +
                                expectedValue);*/
                    }
                } else {
                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
                            filterName + " and Option " + expectedValue + " is an Invalid JSON. " + ParseJsonResponse.getHTMLResponseReason(showResponse));

                    FileUtils.saveResponseInFile("Filter " + filterName + " EntityTypeId " + entityTypeId + " Record Id " + recordId + " Show API HTML.txt",
                            showResponse);
                }
                String entityIdName = ConfigureConstantFields.getEntityNameById(entityTypeId);
                List<String> bulkEntitiesToCheck  = Arrays.asList("contracts","obligations","child obligations","service levels","child service levels","invoices","service data","consumptions","actions","issues","disputes");

                if(bulkEntitiesToCheck.contains(entityIdName)) {
                    Boolean bulkFetchResponse = validateBulkFetchResponse(entityIdName, filterPayload, csAssert);
                    if (!bulkFetchResponse) {
                        csAssert.assertTrue(false, "Error while validating bulk fetch response for entity " + entityIdName + " Filter " + filterName + " Filter Value " + filterPayload);
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

	/*private static void hitListDataAndVerifyResultsWithAuditLog(String filterName, String optionName, String payload, int entityTypeId, CustomAssert csAssert,
	                                                            Boolean applyRandomization, int maxRecordsToValidate, String expectedDateFormat, String auditLogDateFormat) {
		try {
			logger.info("Hitting List Data API for Filter {} and Option {}", filterName, optionName);
			ListRendererListData listDataObj = new ListRendererListData();

			List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(listDataObj, filterName, optionName, payload, entityTypeId, csAssert,
					applyRandomization, maxRecordsToValidate);
			logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());

			for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
				int idColumnNo = listDataObj.getColumnIdFromColumnName("id");
				int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

				logger.info("Hitting TabListData API for Audit Log Tab.");
				TabListData tabListObj = new TabListData();
				String payloadForAuditLogTab;

				if (filterName.equalsIgnoreCase("createdDate") || filterName.equalsIgnoreCase("createdBy"))
					payloadForAuditLogTab = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
							"\"orderDirection\":\"asc\",\"filterJson\":{}}}";
				else
					payloadForAuditLogTab = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
							"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

				int auditLogTabId = TabListDataHelper.getIdForTab("audit log");
				String auditLogTabListResponse = tabListObj.hitTabListData(auditLogTabId, entityTypeId, recordId, payloadForAuditLogTab);

				if (ParseJsonResponse.validJsonResponse(auditLogTabListResponse)) {
					List<Map<Integer, Map<String, String>>> auditLogListData = ListDataHelper.getListData(auditLogTabListResponse);
					if (auditLogListData.size() > 0) {
						String actualValueInAuditLogTab;
						String expectedValueColumnId;
						Boolean result;

						if (filterName.equalsIgnoreCase("createdDate") || filterName.equalsIgnoreCase("modifiedDate")) {
							expectedValueColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabListResponse, "audit_log_user_date");

							if (expectedValueColumnId != null) {
								actualValueInAuditLogTab = auditLogListData.get(0).get(Integer.parseInt(expectedValueColumnId.trim())).get("value");

								if (!actualValueInAuditLogTab.trim().contains("AoE")) {
									String temp[] = optionName.split(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter"));
									String fromDate = temp[0];
									String toDate = temp[1];

									Date date = new SimpleDateFormat(auditLogDateFormat).parse(actualValueInAuditLogTab);
									actualValueInAuditLogTab = new SimpleDateFormat(expectedDateFormat).format(date);
									logger.info("Expected Value: {} and Actual Value: {}", optionName, actualValueInAuditLogTab);
									result = DateUtils.isDateWithinRange(actualValueInAuditLogTab, fromDate, toDate, expectedDateFormat);
								} else {
									result = true;
								}
							} else {
								logger.error("Couldn't get Id for Column [audit_log_user_date] from Audit Log List Data.");
								result = false;
							}
						} else {
							expectedValueColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabListResponse, "completed_by");

							if (expectedValueColumnId != null) {
								actualValueInAuditLogTab = auditLogListData.get(0).get(Integer.parseInt(expectedValueColumnId.trim())).get("value");

								logger.info("Expected Value: {} and Actual Value: {}", optionName, actualValueInAuditLogTab);
								result = optionName.trim().equalsIgnoreCase(actualValueInAuditLogTab.trim());
							} else {
								logger.error("Couldn't get Id for Column [completed_by] from Audit Log List Data.");
								result = false;
							}
						}

						if (!result) {
							csAssert.assertTrue(false, "Audit Log Validation failed for Filter " + filterName + " and Option " + optionName +
									" and Record Id " + recordId);
						}
					} else {
						csAssert.assertTrue(false, "Couldn't get List Data from Audit Log TabListData API for Entity Id " + recordId);
					}
				} else {
					csAssert.assertTrue(false, "Audit Log TabListData API Response for Entity Id " + recordId + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
		}
	}*/

    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String listDataResponse, String filterName, String expectedValue, int entityTypeId,
                                                                                        CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) {
        List<Map<Integer, Map<String, String>>> listDataResults = new ArrayList<>();

        try {
            filterName = filterName.trim();
            ListRendererListData listDataObj = new ListRendererListData();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                listDataObj.setListData(listDataResponse);
                List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
                logger.info("Total Results found for Filter {} and Option {}: {}", filterName, expectedValue, listData.size());

                if (listData.size() > 0) {
                    listDataResults = filterListDataResultsToValidate(listData, applyRandomization, maxRecordsToValidate);
                }
            } else {
                csAssert.assertTrue(false, "List Data API Response for Filter " + filterName + " and Option " + expectedValue + " is an Invalid JSON. " +
                        ParseJsonResponse.getHTMLResponseReason(listDataResponse));
            }
        } catch (Exception e) {
            logger.error("Exception while Getting List Data Results to Validate for Filter {} and Option {} of Entity Type Id {}. {}", filterName, expectedValue,
                    entityTypeId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Getting List Data Results to Validate for Filter " + filterName + ", Option " + expectedValue +
                    " of Entity Type Id " + entityTypeId + ". " + e.getMessage());
        }
        return listDataResults;
    }

    private static List<Map<Integer, Map<String, String>>> filterListDataResultsToValidate(List<Map<Integer, Map<String, String>>> allResults, Boolean applyRandomization,
                                                                                           int maxRecordsToValidate) {
        if (!applyRandomization)
            return allResults;

        List<Map<Integer, Map<String, String>>> listDataResultsToValidate = new ArrayList<>();

        try {
            int[] randomNumbersArr = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allResults.size() - 1, maxRecordsToValidate);

            for (int randomNumber : randomNumbersArr) {
                listDataResultsToValidate.add(allResults.get(randomNumber));
            }
        } catch (Exception e) {
            logger.error("Exception while filtering Results to Validate. {}", e.getMessage());
        }
        return listDataResultsToValidate;
    }

    public static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(List<Map<Integer, Map<String, String>>> allResults, Boolean applyRandomization,
                                                                                       int maxRecordsToValidate) {
        if (!applyRandomization)
            return allResults;

        List<Map<Integer, Map<String, String>>> listDataResultsToValidate = new ArrayList<>();

        try {
            int[] randomNumbersArr = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allResults.size() - 1, maxRecordsToValidate);

            for (int randomNumber : randomNumbersArr) {
                listDataResultsToValidate.add(allResults.get(randomNumber));
            }
        } catch (Exception e) {
            logger.error("Exception while getting Results to Validate. {}", e.getMessage());
        }
        return listDataResultsToValidate;
    }

    public static List<Map<String, String>> getAllSupplierTypeUsersFromFilterResponse(String filterDataResponse) {
        List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

        try {
            if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                int filterId = getFilterIdFromFilterName(filterDataResponse, "stakeholder");

                if (filterId == -1)
                    return allSupplierTypeUsers;

                JSONObject jsonObj = new JSONObject(filterDataResponse);
                jsonObj = jsonObj.getJSONObject(String.valueOf(filterId)).getJSONObject("multiselectValues").getJSONObject("OPTIONS");

                String dataArrayName = jsonObj.has("data") ? "data" : "DATA";
                JSONArray jsonArr = jsonObj.getJSONArray(dataArrayName);
                jsonObj = jsonArr.getJSONObject(0);

                Boolean isFilterAutoComplete = isFilterOfAutoCompleteType(filterDataResponse, "stakeholder");

                if (isFilterAutoComplete != null && isFilterAutoComplete) {
                    OptionsHelper optionObj = new OptionsHelper();
                    String optionResponse = optionObj.hitOptionsForAllStakeholders();
                    allSupplierTypeUsers = optionObj.getAllSupplierTypeUsersFromOptionsResponse(optionResponse);

                    return allSupplierTypeUsers;
                }


                jsonArr = jsonObj.getJSONArray("group");

                for (int i = 0; i < jsonArr.length(); i++) {
                    int idType = jsonArr.getJSONObject(i).getInt("idType");

                    if (idType == 4) {
                        Map<String, String> userMap = new HashMap<>();

                        String userName = jsonArr.getJSONObject(i).getString("name");
                        int userId = jsonArr.getJSONObject(i).getInt("id");

                        userMap.put("name", userName);
                        userMap.put("id", String.valueOf(userId));

                        allSupplierTypeUsers.add(userMap);
                    }
                }

                return allSupplierTypeUsers;
            } else {
                logger.error("Filter Data Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Supplier Type Users from Filter Data Response. {}", e.getMessage());
        }

        return null;
    }

    private static void validateVendorHierarchyFilter(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String listDataResponse, String filterName,
                                                      String expectedValue, String showPageObjectName, int entityTypeId, CustomAssert csAssert, String filterType) {
        try {
            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            Show showObj = new Show();

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                showObj.hitShowVersion2(entityTypeId, recordId);
                String showResponse = showObj.getShowJsonStr();

                if (!ShowHelper.isShowPageAccessible(showResponse)) {
                    logger.warn("Show Page is Not Accessible for Record Id {} of Entity Type Id {}", recordId, entityTypeId);
                    continue;
                }

                String showFieldHierarchy = ShowHelper.getShowFieldHierarchy("supplier id", entityTypeId);
                List<String> allSupplierIds;

                if (showFieldHierarchy.contains("relations")) {
                    allSupplierIds = ShowHelper.getAllSelectValuesOfField(showResponse, "supplier id",
                            showFieldHierarchy, recordId, entityTypeId);
                } else {
                    String supplierId = ShowHelper.getValueOfField(entityTypeId, "supplier id", showResponse);
                    allSupplierIds = new ArrayList<>();

                    if (supplierId != null) {
                        allSupplierIds.add(supplierId);
                    }
                }

                if (allSupplierIds == null) {
                    csAssert.assertTrue(false, "Couldn't get Supplier Id for Entity Id " + recordId);
                    continue;
                }

                boolean matchFound = false;

                for (String supplierId : allSupplierIds) {
                    logger.info("Hitting Show API for Supplier Id {}", supplierId);
                    showObj.hitShowVersion2(1, Integer.parseInt(supplierId));
                    showResponse = showObj.getShowJsonStr();

                    if (!ShowHelper.isShowPageAccessible(showResponse)) {
//                        csAssert.assertTrue(false, "Show Page is Not Accessible for Supplier Id " + supplierId);
                        continue;
                    }

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        boolean result;

                        JSONObject jsonObj = new JSONObject(showResponse);
                        String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

                        if (showStatus.trim().equalsIgnoreCase("success")) {
                            result = showObj.verifyShowField(showResponse, showPageObjectName, expectedValue, 1, filterType.trim());

                            if (result) {
                                matchFound = true;
                                break;
                            }
                        } else {
                            logger.warn("Show Page Not Accessible for Supplier Id {} ", supplierId);
                            /*csAssert.assertTrue(false, "Show Page Not Accessible for Supplier Id " + supplierId);*/
                        }
                    } else {
                        csAssert.assertTrue(false, "Show API Response for Supplier Id " + supplierId + " is an Invalid JSON. " +
                                ParseJsonResponse.getHTMLResponseReason(showResponse));
                        FileUtils.saveResponseInFile("Filter " + filterName + " Supplier Record Id " + supplierId + " Show API HTML.txt", showResponse);
                    }
                }

                if (!matchFound) {
                    logger.error("Show Page validation failed for Record Id {}, Filter {} and Option {}", recordId, filterName, expectedValue);
                    csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                            " of Filter " + filterName + " and Option " + expectedValue);
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + " of EntityTypeId " + entityTypeId + ". " + e.getMessage());
        }
    }

    private static void validateSourceFilter(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterPayload,
                                             String listDataResponse, String filterName, String expectedValue, int entityTypeId, CustomAssert csAssert, String filterType) {
        try {
            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            JSONObject jsonObj = new JSONObject(filterPayload).getJSONObject("filterMap").getJSONObject("filterJson");
            String filterId = JSONObject.getNames(jsonObj)[0];
            String optionId = jsonObj.getJSONObject(filterId).getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").getJSONObject(0).getString("id");
            Show showObj = new Show();

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                showObj.hitShowVersion2(entityTypeId, recordId);
                String showResponse = showObj.getShowJsonStr();

                if (!ShowHelper.isShowPageAccessible(showResponse)) {
                    logger.warn("Show Page is Not Accessible for Record Id {} of Entity Type Id {}", recordId, entityTypeId);
                    continue;
                }

                boolean result = showObj.verifyShowField(showResponse, "parententitytypeid", optionId, entityTypeId, filterType.trim());
                if (!result) {
                    logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {} of EntityTypeId {}", recordId, filterName,
                            expectedValue, entityTypeId);
                    csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                            " of Filter " + filterName + " and Option " + expectedValue + " of EntityTypeId " + entityTypeId);
                }

            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + " of EntityTypeId " + entityTypeId +
                    " and Option: " + expectedValue + ". " + e.getMessage());
        }
    }

    private static Boolean validateBulkFetchResponse(String entityName,String filterPayload,CustomAssert customAssert) {

        String filter = "null";
        Boolean validationStatus = true;
        try {
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListData(listId,filterPayload);
            String listResponse = listRendererListData.getListDataJsonStr();

            JSONObject listResponseJson = new JSONObject(listResponse);
            JSONArray dataArray = listResponseJson.getJSONArray("data");

            if(dataArray.length() == 0){
                return true;
            }

            JSONObject filterPayloadJson = new JSONObject(filterPayload);
            if (filterPayloadJson.has("selectedColumns")) {
                filterPayloadJson.remove("selectedColumns");
            }
            String keySet = filterPayloadJson.getJSONObject("filterMap").getJSONObject("filterJson").keySet().iterator().next();

            filter =  filterPayloadJson.getJSONObject("filterMap").getJSONObject("filterJson").getJSONObject(keySet).toString();

            String filterForBulkFetchIds = "{\"listId\":" + listId + ",\"filterMap\":{\"" + keySet + "\":" + filter + "},\"contractId\":\"\",\"relationId\":\"\",\"vendorId\":\"\",\"entityIds\":[]}";

            String bulkFetchResponse = BulkFetch.getBulkFetchResponse(entityName,filterForBulkFetchIds);

            if(bulkFetchResponse.contains("applicationError")){
//                customAssert.assertTrue(false,"Bulk Fetch Response contains applicationError " + "for entityName " + entityName );
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false,"Exception while validating bulk fetch response for entity " + entityName);
            validationStatus = false;
        }

        return validationStatus;
    }
}