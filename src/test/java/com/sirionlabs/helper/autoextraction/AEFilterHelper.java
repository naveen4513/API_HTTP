package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.userGroups.UserGroupsUsers;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.test.autoExtraction.AETestFilters;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class AEFilterHelper {

    private final static Logger logger = LoggerFactory.getLogger(AEFilterHelper.class);
    AutoExtractionHelper autoExtractionHelper = new AutoExtractionHelper();


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


    public static void verifyResultsOnShowPage(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterPayload, String listDataResponse, String filterName,
                                               String expectedValue, String showPageObjectName, int entityTypeId, String filterType, Map<String, String> optionMap,
                                               CustomAssert csAssert) {
        try {
            switch (filterType) {
                default:
                    verifyResults(allRecordsToValidate, filterPayload, listDataResponse, filterName, expectedValue, showPageObjectName, entityTypeId, csAssert, filterType);
                    break;

                case "stakeholder":
                    int listId = 432;
                    String roleGroupId = optionMap.get("roleGroupId");
                    String optionId = optionMap.get("id");
                    AEFilterHelper.testDownloadListingData(filterPayload,filterName);
                    hitListDataAndVerifyResultsForStakeholdersForAE(listId, filterName, showPageObjectName, optionId, filterPayload,
                            entityTypeId, csAssert, roleGroupId, true, allRecordsToValidate.size(), expectedValue);
                    break;

                case "inputtext":
                    listId = 432;
                    String textValue=getValueForInputTextType(filterName);

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


    private static void hitListDataAndVerifyResultsOnShowPage(String filterName, String filterPayload, String expectedValue, String entityName, int entityTypeId,
                                                              String filterType, Map<String, String> optionMap, String showPageObjectName, Boolean applyRandomization,
                                                              int maxNoOfRecordsToValidate, CustomAssert csAssert) throws IOException {
        List<String> filterNamesList = new ArrayList<>();
        filterNamesList.add(filterName);

        List<String> expectedValuesList = new ArrayList<>();
        expectedValuesList.add(expectedValue);

        List<String> filterTypesList = new ArrayList<>();
        filterTypesList.add(filterType);

        List<Map<String, String>> optionsMapList = new ArrayList<>();
        optionsMapList.add(optionMap);

        List<String> showPageObjectNamesList = new ArrayList<>();
        showPageObjectNamesList.add(showPageObjectName);

        hitListDataAndVerifyResultsOnShowPage(filterNamesList, filterPayload, expectedValuesList, entityName, entityTypeId, filterTypesList, optionsMapList,
                showPageObjectNamesList, applyRandomization, maxNoOfRecordsToValidate, csAssert);
    }

    public static void validateFilter(String filterDataResponse, String filterName, Integer filterId, String entityName, int entityTypeId, int listDataOffset,
                                      int listDataSize, String startDate, String endDate, String minSliderValue, String maxSliderValue, Boolean applyRandomization,
                                      int maxFilterOptionsToValidate, int maxNoOfRecordsToValidate, String showPageObjectName, CustomAssert csAssert) {

        String filterType = getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();
        String expectedValue;

        switch (filterType) {

            case "date":
                expectedValue = startDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + endDate;
                break;

            case "inputtext":
                expectedValue= getValueForInputTextType(filterName);
                break;

            default:
                expectedValue = null;
                break;
        }

        validateFilter(filterDataResponse, filterName, filterId, entityName, entityTypeId, listDataOffset, listDataSize, expectedValue, applyRandomization,
                maxFilterOptionsToValidate, maxNoOfRecordsToValidate, showPageObjectName, csAssert);
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

    private static void verifyResultsForDateTypeListingPage(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterName, String expectedValue, CustomAssert csAssert) {

        String actualValue;
        String columnNameToCheck = "";

        if (filterName.equals("createdDate")) {
            columnNameToCheck = "datecreated";
        } else if (filterName.equals("modifiedDate")) {
            columnNameToCheck = "datemodified";
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

    private static void verifyResultsOthersListingPage(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterName, String expectedValue, CustomAssert csAssert) {

        String actualValue;
        String columnNameToCheck = "";

        if (filterName.equals("createdby")) {
            columnNameToCheck = "createdby";
        } else if (filterName.equals("lastmodifiedby")) {
            columnNameToCheck = "lastmodifiedby";
        }

        for (Map<Integer, Map<String, String>> singleRecord : allRecordsToValidate) {

            singleRecordLoop:
            for (Map.Entry<Integer, Map<String, String>> singleRecordColumnData : singleRecord.entrySet()) {

                if (singleRecordColumnData.getValue().get("columnName").equals(columnNameToCheck)) {
                    actualValue = singleRecordColumnData.getValue().get("value");
                    if (!actualValue.equals(expectedValue)) {
                        csAssert.assertTrue(false, "Actual value and Expected value mismatch for filter " + filterName);
                    }
                    break singleRecordLoop;
                }
            }
        }
    }


    private static void hitListDataAndVerifyResultsOnShowPage(List<String> filterNamesList, String filterPayload, List<String> expectedValuesList, String entityName,
                                                              int entityTypeId, List<String> filterTypesList, List<Map<String, String>> optionMapsList,
                                                              List<String> showPageObjectNamesList, Boolean applyRandomization, int maxNoOfRecordsToValidate,
                                                              CustomAssert csAssert) throws IOException {
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

            for (int i = 0; i < filterNamesList.size(); i++) {

                filterName = filterNamesList.get(i);

                List<Map<Integer, Map<String, String>>> allRecordsToValidate = AEFilterHelper.getListDataResultsToValidate(listDataResponse,
                        filterNamesList.get(i), expectedValuesList.get(i), entityTypeId, csAssert, applyRandomization, maxNoOfRecordsToValidate);


                if(filterName.equalsIgnoreCase("pageSimilarityScore"))
                {
                    logger.info("validating page similarity score filter results " );
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "maxpagesimilarityscore");
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalData = listObj.getJSONArray("data").length();
                    for(int j=0;j<totalData;j++)
                    {
                        String pageSimilarityScoreValue =  listObj.getJSONArray("data").getJSONObject(j).getJSONObject(Integer.toString(columnId)).getString("value");
                        JSONArray pageSimilarityDataList=new JSONArray(pageSimilarityScoreValue);
                        for(int k=0;k<pageSimilarityDataList.length();k++) {
                            String id=pageSimilarityDataList.getJSONObject(k).get("id").toString();
                            String score=pageSimilarityDataList.getJSONObject(k).get("maxPageScoreData").toString();
                            logger.info("checking if filter result's page similarity score is greater than or equal to input value i.e 100.0 ");
                            csAssert.assertTrue(Float.parseFloat(score)>=100.0," Page similarity score is not greater than or equal to expected value to the document id "+id);
                        }

                    }

                }

                else if(filterName.equalsIgnoreCase("documentSimilarityScore"))
                {
                    {
                        logger.info("validating document similarity score filter results " );
                        int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "maxsimilarityscore");
                        JSONObject listObj = new JSONObject(listDataResponse);
                        int totalData = listObj.getJSONArray("data").length();
                        for(int j=0;j<totalData;j++)
                        {
                            String maxSimilarityScoreValue =  listObj.getJSONArray("data").getJSONObject(j).getJSONObject(Integer.toString(columnId)).getString("value");
                            JSONArray maxSimilarityDataList=new JSONArray(maxSimilarityScoreValue);
                            for(int k=0;k<maxSimilarityDataList.length();k++) {
                                String id=maxSimilarityDataList.getJSONObject(k).get("id").toString();
                                String score=maxSimilarityDataList.getJSONObject(k).get("similarityScore").toString();
                                logger.info("checking if filter result's max similarity score is greater than or equal to input value i.e 100.0 ");
                                csAssert.assertTrue(Float.parseFloat(score)>=100.0," max similarity score is not greater than or equal to expected value to the document id "+id);
                            }

                        }

                    }

                }
                else if (filterName.equals("duplicatedocs")) {

                    logger.info("Started the verification for duplicate Data Filter ");
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "documentname");
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalData = listObj.getJSONArray("data").length();
                    int actualCount = allRecordsToValidate.size();
                    Set<String> documentNameSet = new HashSet<String>();
                    for (int j = 0; j < totalData; j++) {
                        documentNameSet.add(listObj.getJSONArray("data").getJSONObject(j).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[0]);
                    }
                    logger.info("Set of document Names when there is no duplicate doc:" + documentNameSet);
                    int expectedCount = documentNameSet.size();
                    csAssert.assertTrue(actualCount > expectedCount, "After Applying duplicate data filter it is still not showing duplicate docs" + filterName);

                }
                else if (filterName.equals("clusters")) {
                    int clusterColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "clusters");
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalDocs = listObj.getJSONArray("data").length();
                    for (i = 0; i < totalDocs; i++) {
                        String[] clusterValue = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(clusterColumnId)).get("value").toString().split(",");
                        if (clusterValue.length > 1) {
                            boolean flag = false;
                            for (int j = 0; j < clusterValue.length; j++) {

                                if (clusterValue[j].trim().equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                    flag = true;
                                    break;
                                }

                            }
                            csAssert.assertTrue(flag, "Cluster value for comma separated and option are different");


                        } else {
                            csAssert.assertEquals(clusterValue[0].trim(), optionMapsList.get(0).get("name"), "Cluster value for normal cluster and option are different");

                        }

                    }
                } else if (filterName.equals("doctag1ids") || filterName.equals("doctag2ids") ||
                        filterName.equals("doctagids") || filterName.equals("groupids") || filterName.equals("batchids")) {
                    String columnNameToCheck = "";


                    if (filterName.equals("doctag1ids")) {
                        columnNameToCheck = "doctag1";
                    } else if (filterName.equals("doctag2ids")) {
                        columnNameToCheck = "doctag2";
                    } else if (filterName.equals("doctagids")) {
                        columnNameToCheck = "doctags";
                    } else if (filterName.equals("groupids")) {
                        columnNameToCheck = "groups";
                    } else if (filterName.equals("batchids")) {
                        columnNameToCheck = "batch";
                    }

                    logger.info("Started Verification for FilterName:" + filterName);
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, columnNameToCheck);
                    List<String> groupOfTags = new LinkedList<>();
                    List<String> labelNamesWithSeparator = new LinkedList<>();
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalDocs = listObj.getJSONArray("data").length();
                    for ( i = 0; i < totalDocs; i++) {
                        groupOfTags.add(listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString());
                    }
                    for ( i = 0; i < groupOfTags.size(); i++) {
                        if (groupOfTags.get(i).contains(":;")) {
                            String[] firstIndexOfName = groupOfTags.get(i).split(":;");
                            int size = firstIndexOfName.length;
                            for (int j = 0; j < size; j++) {
                                if (firstIndexOfName[j].contains("::")) {
                                    String[] finalOptionNames = firstIndexOfName[j].split("::");
                                    if (finalOptionNames[1].equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                        labelNamesWithSeparator.add(finalOptionNames[1]);

                                    }
                                } else if (firstIndexOfName[j].equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                    labelNamesWithSeparator.add(firstIndexOfName[j]);
                                }
                            }
                        }
                    }
                    logger.info("Starting the verification of Cluster Filter on AE listing Page");
                    for (String recordsToValidate : labelNamesWithSeparator) {

                        csAssert.assertTrue(recordsToValidate.equalsIgnoreCase(optionMapsList.get(0).get("name")), "Value of" + optionMapsList.get(0).get("name") + " " + "option applied in filter is not equal to the values in column" + filterName);
                    }


                } else if (filterName.equals("unassigneddocs")) {
                    logger.info("Started Verification for Filter Name" + filterName);

                    for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {

                        int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                        int recordEntityTypeId = entityTypeId;
                        int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                        if (optionMapsList.get(0).get("name").equals("Assigned")) {
                            try {
                                HttpResponse showPageResponse = AutoExtractionHelper.docShowAPI(recordId);
                                csAssert.assertTrue(showPageResponse.getStatusLine().getStatusCode() == 200, "Response code of AE show API is invalid");
                                String showPageStr = EntityUtils.toString(showPageResponse.getEntity());
                                JSONObject showPageObj = new JSONObject(showPageStr);
                                List<String> layoutGroups = new ArrayList<>();
                                int layoutGroupCount = showPageObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0).getJSONArray("fields").length();
                                JSONArray layoutGroupArr = showPageObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0).getJSONArray("fields");

                                for (i = 0; i < layoutGroupCount; i++) {
                                    layoutGroups.add(String.valueOf(layoutGroupArr.getJSONObject(i).get("label")));

                                }
                                logger.info("Validating Assigned for:" + recordId);
                                csAssert.assertTrue(layoutGroups.contains("Stakeholders"), "Assigned Documents are not having the Sections other than Basic Information");

                            } catch (Exception e) {
                                csAssert.assertTrue(false, "AE Show page API is not working because of :  :" + e.getStackTrace());
                            }
                        } else if (optionMapsList.get(0).get("name").equals("Unassigned")) {
                            try {
                                logger.info("Validating UnAssigned for:" + recordId);
                                HttpResponse generalTabResponse = AutoExtractionHelper.hitGeneralTabAPI(recordId);
                                csAssert.assertTrue(generalTabResponse.getStatusLine().getStatusCode() == 200, "Response Code of General Tab API is Invalid");
                                String generalTabStr = EntityUtils.toString(generalTabResponse.getEntity());
                                JSONObject generalTabObj = new JSONObject(generalTabStr);
                                boolean response = (boolean) generalTabObj.get("success");
                                csAssert.assertTrue(response == true, " Failed to verify Unassigned Document as response is not success");
                            } catch (Exception e) {
                                csAssert.assertTrue(false, "General Tab API is not working because of :" + e.getStackTrace());

                            }
                        }
                    }

                } else if (filterName.equalsIgnoreCase("projectids")) {
                    logger.info("Validating filter result for filter name " + filterName + " and " + optionMapsList.get(0).get("name"));
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalData = listObj.getJSONArray("data").length();
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "projects");
                    if (totalData > 0) {
                        for (i = 0; i < totalData; i++) {
                            String[] projectValue = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split("::");
                            if (projectValue.length > 1) {
                                List<String> allProjects = new ArrayList<>();
                                for (int j = 0; j < projectValue.length; j++) {
                                    String[] projectNames = projectValue[i].trim().split(":;");
                                    for (String project : projectNames) {
                                        if (project.equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                            allProjects.add(project);
                                            logger.info("Project name " + optionMapsList.get(0).get("name") + " found in Multi Projects Document.");
                                        }
                                    }
                                }
                                csAssert.assertEquals(allProjects.get(0), optionMapsList.get(0).get("name"), "List Page validation failed for" +
                                        " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + " having Id " + listId);
                            } else {
                                String[] Value = listObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                                String projectName = Value[0].trim();
                                csAssert.assertEquals(projectName, optionMapsList.get(0).get("name"), "List Page validation failed for" +
                                        " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + " having Id " + listId);
                            }
                        }
                    }
                } else if (filterName.equalsIgnoreCase("statusId")) {
                    JSONObject listObj = new JSONObject(listDataResponse);
                    JSONArray listObjArr = listObj.getJSONArray("data");
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "status");
                    for (i = 0; i < listObjArr.length(); i++) {
                        String[] extractionStatusValue = listObjArr.getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                        String status = extractionStatusValue[0];
                        csAssert.assertEquals(status, optionMapsList.get(0).get("name"), "List Page validation failed for Record Id " +
                                " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + "having Id " + listId);
                    }
                } else if (filterName.equalsIgnoreCase("status")) {
                    JSONObject listObj = new JSONObject(listDataResponse);
                    JSONArray listObjArr = listObj.getJSONArray("data");
                    int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "workflowstatus");
                    for (i = 0; i < listObjArr.length(); i++) {
                        String workFlowStatusValue = listObjArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString();
                        csAssert.assertEquals(workFlowStatusValue, optionMapsList.get(0).get("name"), "List Page validation failed for Record Id " +
                                " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + " having Id " + listId);
                    }
                } else if (filterName.equalsIgnoreCase("categoryId")) {

                    for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                        int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                        int recordEntityTypeId = entityTypeId;
                        int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                        logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionMapsList.get(0).get("name"));
                        logger.info("Validating Record Id " + recordId + "for Filter name " + filterName + " and " + optionMapsList.get(0).get("name"));
                        String payloadForClause = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\"categoryId\"},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":" + recordId + "}";
                        HttpResponse clauseListResponse = AutoExtractionHelper.getTabData(payloadForClause, 493);
                        String clauseListResponseStr = EntityUtils.toString(clauseListResponse.getEntity());

                        boolean result = false;
                        if (ParseJsonResponse.validJsonResponse(clauseListResponseStr)) {
                            int columnId = ListDataHelper.getColumnIdFromColumnName(clauseListResponseStr, "name");
                            JSONObject obj = new JSONObject(clauseListResponseStr);
                            JSONArray arrObj = obj.getJSONArray("data");
                            for (i = 0; i < arrObj.length(); i++) {
                                String[] temp = arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                                if ((temp[0].trim()).equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                    result = true;
                                    logger.info("Category id Filter name  is " + optionMapsList.get(0).get("name") + " and Clause list value is " + temp[0]);
                                }
                            }
                            csAssert.assertTrue(result, "Show Page validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + " having Id " + listId);
                        } else {
                            logger.error("Clause Tab List API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionMapsList.get(0).get("name"));
                            csAssert.assertTrue(false, "Clause Tab List API Response for Record Id " + recordId + " of Filter " +
                                    filterName + " and Option " + optionMapsList.get(0).get("name") + " is an Invalid JSON.");
                        }

                    }
                } else if (filterName.equalsIgnoreCase("fieldId")) {
                    for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                        int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                        int recordEntityTypeId = entityTypeId;
                        int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                        logger.info("Validating filter name " + filterName + " for " + optionMapsList.get(0).get("name"));
                        String payloadForMetadata = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":" + recordId + "}";
                        HttpResponse metadataTabListResponse = AutoExtractionHelper.getTabData(payloadForMetadata, 433);
                        String metadataTabListResponseStr = EntityUtils.toString(metadataTabListResponse.getEntity());
                        boolean result = false;
                        if (ParseJsonResponse.validJsonResponse(metadataTabListResponseStr)) {
                            int columnId = ListDataHelper.getColumnIdFromColumnName(metadataTabListResponseStr, "fieldname");
                            JSONObject obj = new JSONObject(metadataTabListResponseStr);
                            JSONArray arrObj = obj.getJSONArray("data");
                            for (i = 0; i < arrObj.length(); i++) {
                                String[] temp = arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().trim().split(":;");
                                if ((temp[0].trim()).equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                    result = true;
                                } else if (temp[0].trim().contains("(")) {
                                    String[] temp1 = temp[0].split("\\(");
                                    String value = temp1[0].trim() + "(" + temp1[1].trim();
                                    if (value.equalsIgnoreCase(optionMapsList.get(0).get("name"))) {
                                        result = true;
                                    }
                                }
                            }
                            csAssert.assertTrue(result, "Metadata tab list API validation failed for Record Id " + recordId +
                                    " of Filter " + filterName + " and Option " + optionMapsList.get(0).get("name") + " of Entity Name  " + entityName + " of Report having Id " + listId);
                        } else {
                            logger.error("Metadata tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionMapsList.get(0).get("name"));
                            csAssert.assertTrue(false, "Metadata tab list API Response for Record Id " + recordId + " of Filter " +
                                    filterName + " and Option " + optionMapsList.get(0).get("name") + " is an Invalid JSON.");
                        }
                    }
                }
                else if (filterName.equals("contract")) {
                    logger.info("Started the verification for Contract Filter ");
                    int contractNameColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "contractname");
                    int contractIdColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse,"contractid");
                    List<String> contractName = new LinkedList<>();
                    List<String> contractID = new LinkedList<>();
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int totalData = listObj.getJSONArray("data").length();
                    if(totalData>0) {
                        for (int x = 0; x < totalData; x++) {
                            contractName.add(listObj.getJSONArray("data").getJSONObject(x).getJSONObject(String.valueOf(contractNameColumnId)).get("value").toString().split(":;")[0]);
                            contractID.add(listObj.getJSONArray("data").getJSONObject(x).getJSONObject(String.valueOf(contractIdColumnId)).get("value").toString().split(":;")[1]);
                        }
                        String actualContractOptionValue = optionMapsList.get(0).get("name");
                        String actualContractId = optionMapsList.get(0).get("id");
                        csAssert.assertTrue(contractName.stream().allMatch(actualContractOptionValue::equalsIgnoreCase),"Contract Filter is not working" + "Actual Option Value: "+actualContractOptionValue);
                        csAssert.assertTrue(contractID.stream().allMatch(actualContractId::equalsIgnoreCase),"Contract Filter is not working" + "Actual Option Value: "+actualContractOptionValue);
                    }

                }
                else if (filterName.equals("parentreference"))
                {
                    /* C153905: Verify that Parent Reference filter is working fine*/
                    JSONObject listObj = new JSONObject(listDataResponse);
                    int dataCount = listObj.getJSONArray("data").length();
                    List<String> parentReferenceValue = new LinkedList<>();
                    if(dataCount>0) {
                        logger.info("Verify that Parent Reference Filter is working fine");
                        int contractNameColumnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "parentreference");
                        if (optionMapsList.get(0).get("name").equals("Exists")) {
                            for(int y=0;y<dataCount;y++)
                            {
                                parentReferenceValue.add((String) listObj.getJSONArray("data").getJSONObject(y).getJSONObject(String.valueOf(contractNameColumnId)).get("value"));

                            }
                            csAssert.assertEquals(parentReferenceValue.size(),dataCount,"Data does not exists in Parent Reference column after applying Parent reference: Exists Filter");
                        }
                        else {
                            for (int m = 0; m < dataCount; m++) {
                                String doesNotExistsValue = String.valueOf(listObj.getJSONArray("data").getJSONObject(m).getJSONObject(String.valueOf(contractNameColumnId)).get("value"));
                                csAssert.assertEquals(doesNotExistsValue,"null","Value exists for parent reference after applying filter of parent reference does not exists, value :" +parentReferenceValue );
                            }
                        }
                    }
                    else
                    {
                        throw new SkipException("There is no data in AE listing to validate Parent Reference Filter");

                    }
                }

                else {
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
                case  "inputtext":
                    filterPayload=getPayloadForInputTextType(filterName,filterId,entityTypeId,listDataOffset,listDataSize);
                    hitListDataAndVerifyResultsOnShowPage(filterName, filterPayload, expectedValue, entityName, entityTypeId, filterType, null,
                            showPageObjectName, applyRandomization, maxNoOfRecordsToValidate, csAssert);
                    break;

                default:
                    List<Map<String, String>> allOptions = getAllOptionsOfFilter(filterDataResponse, filterName, filterType, entityTypeId);
                    List<Map<String, String>> allOptionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                    for (Map<String, String> optionToTest : allOptionsToTest) {
                        expectedValue = optionToTest.get("name");
                        filterPayload = getFiltersPayload(filterName, filterId, filterType, entityTypeId, listDataOffset, listDataSize, optionToTest, expectedValue);

                        AEFilterHelper.testDownloadListingData(filterPayload,filterName);
                        hitListDataAndVerifyResultsOnShowPage(filterName, filterPayload, expectedValue, entityName, entityTypeId, filterType, optionToTest,
                                showPageObjectName, applyRandomization, maxNoOfRecordsToValidate, csAssert);
                    }
                    break;
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Filter " + filterName + " of Entity " + entityName + ". " + e.getMessage());
        }
    }

    private static void verifyResults(List<Map<Integer, Map<String, String>>> allRecordsToValidate, String filterPayload, String listDataResponse, String filterName,
                                      String expectedValue, String showPageObjectName, int entityTypeId, CustomAssert csAssert, String filterType) {
        try {
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, expectedValue, allRecordsToValidate.size());

            int a = ConfigureConstantFields.getEntityIdByName("vendors");
            int obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

            Show showObj = new Show();
            String showResponse;

            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

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
                List<String> bulkEntitiesToCheck = Arrays.asList("contracts", "obligations", "child obligations", "service levels", "child service levels", "invoices", "service data", "consumptions", "actions", "issues", "disputes");

            }
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }


    public static void hitListDataAndVerifyResultsForStakeholdersForAE(int listId, String filterName, String optionName, String optionId, String payload,
                                                                       int entityTypeId, CustomAssert csAssert, String roleGroupId, Boolean applyRandomization,
                                                                       int maxRecordsToValidate, String expectedValue) throws Exception {
        try {
            logger.info("Hitting List Data API for Filter {}, Role Group Id {} and Option {}", filterName, roleGroupId, optionName);
            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(listId, payload);
            String listDataResponse = listDataObj.getListDataJsonStr();

            List<Map<Integer, Map<String, String>>> allRecordsToValidate = getListDataResultsToValidate(listDataResponse, filterName, expectedValue, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, allRecordsToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : allRecordsToValidate) {
                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }

                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                HttpResponse showApiRespone = AutoExtractionHelper.docShowAPI(recordId);
                String showApiResponseStr = EntityUtils.toString(showApiRespone.getEntity());
                if (ParseJsonResponse.validJsonResponse(showApiResponseStr)) {
                    JSONObject obj = new JSONObject(showApiResponseStr);
                    String stakeHolderJsonStr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").toString();
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
                                                ", Entity Type Id " + entityTypeId + " and Record Id " + recordId + " is an Invalid JSON.");
                                    }
                                } else if (stakeHolderJsonArr.getJSONObject(i).get("id").toString().trim().equalsIgnoreCase(optionId.trim())) {
                                    result = true;
                                }
                            }
                            if (!result) {
                                logger.error("Show Page validation failed for Record Id {} of Filter {}, Role Group {} and Option {}", recordId, filterName, roleGroupId,
                                        optionName);
                                csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                        " of Filter " + filterName + ", Role Group " + roleGroupId + " and Option " + optionName);
                            }

                        } else {
                            logger.error("Couldn't find Role Group [{}] in Show Response for Record Id {} of Filter {} and Option {}", "rg_" + roleGroupId, recordId,
                                    filterName, optionName);
                            csAssert.assertTrue(false, "Couldn't find Role Group [rg_" + roleGroupId + "] in Show Response for Record Id " +
                                    recordId + " of Filter " + filterName + " and Option " + optionName);
                        }

                    } else {
                        logger.error("Couldn't get Stakeholders Json from Show Response for Record Id {} of Filter {} and Option {}", recordId, filterName,
                                optionName);
                        csAssert.assertTrue(false, "Couldn't get Stakeholders Json from Show Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName);
                    }

                }

            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

    public static void hitListDataAndVerifyResults(int listId, String filterName, String optionName, String payload, String showPageObjectName,
                                                   int entityTypeId, String entityName, CustomAssert csAssert, String filterType, Boolean applyRandomization, int maxRecordsToValidate, String expectedValue) throws IOException {

        logger.info("Hitting Report List Data API for Filter {} and Option {} of Report [{}] having Id {}", filterName, optionName);
        ListRendererListData listDataObj = new ListRendererListData();
        listDataObj.hitListRendererListData(listId, payload);
        String listDataResponse = listDataObj.getListDataJsonStr();

        try{
            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(listDataResponse, filterName, expectedValue, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName,
                    listDataToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }
                if (filterType.equalsIgnoreCase("inputtext") && filterName.equalsIgnoreCase("folder")) {
                    int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                    logger.info("Hitting General Tab API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                    HttpResponse generalTabResponse = AutoExtractionHelper.hitGeneralTabAPI(recordId);
                    String generalTabResponseStr = EntityUtils.toString(generalTabResponse.getEntity());
                    boolean result = false;
                    if (ParseJsonResponse.validJsonResponse(generalTabResponseStr)) {
                        JSONObject obj = new JSONObject(generalTabResponseStr);
                        if (obj.getJSONObject("response").get("relativePath").toString().equalsIgnoreCase(optionName)) {
                            result = true;

                        }
                        logger.info("folder Name to validate is " + optionName);
                        csAssert.assertTrue(result, "General Tab validation failed for Record Id " + recordId +
                                " of Filter " + filterName + " and Option " + optionName + " of Report [" + "] having Id " + listId);
                    }
                    else
                    {
                        logger.error("General tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                        csAssert.assertTrue(false, "General tab list API Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName + " is an Invalid JSON.");
                    }
                }
                else if (filterType.equalsIgnoreCase("inputtext") && filterName.equalsIgnoreCase("metadatavalue")) {
                    int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
                    logger.info("Hitting metadata Tab API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                    String payloadforMetadata="{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"min\":\"0\"}}},\"entityId\":"+recordId+"}";
                    HttpResponse metadataTabListResponse = AutoExtractionHelper.getTabData(payloadforMetadata,433);
                    String metadataTabListResponseStr = EntityUtils.toString(metadataTabListResponse.getEntity());
                    boolean result = false;
                    if (ParseJsonResponse.validJsonResponse(metadataTabListResponseStr)) {
                        int columnId=ListDataHelper.getColumnIdFromColumnName(metadataTabListResponseStr, "extractedtext");
                        JSONObject obj = new JSONObject(metadataTabListResponseStr);
                        JSONArray arrObj=obj.getJSONArray("data");
                        for(int i=0;i<arrObj.length();i++)
                        {
                            if(arrObj.getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().equalsIgnoreCase(optionName))
                            {
                                result=true;
                            }

                        }
                        logger.info("metadata Filter value  is " + optionName);
                        csAssert.assertTrue(result, "Metadata tab list API validation failed for Record Id " + recordId +
                                " of Filter " + filterName + " and Option " + optionName + " of List having Id "+listId);
                    }
                    else
                    {
                        logger.error("Metadata tab list API  Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                        csAssert.assertTrue(false, "Metadata tab list API Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName + " is an Invalid JSON.");
                    }
                }
                else {
                    throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
                }
            }
        }
        catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
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

    public static String getValueForInputTextType(String filterName) {
        String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("AEListFiltersConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("AEListFiltersConfigFileName");
        String textValue = null;
        try {


            if (ParseConfigFile.hasPropertyCaseSensitive(configFilePath, configFileName, "inputtext", filterName)) {
                textValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "inputtext", filterName);
            } else {
                logger.info("Text value is not available for Filter {} ", filterName);
            }
        } catch (Exception e) {
            logger.error("Exception while getting text Value for Filter {}. {}", filterName, e.getMessage());
        }
        return textValue;
    }


    public static String getFiltersPayloadPrefix(int entityTypeId, int listDataOffset, int listDataSize) {
        return "{\"selectedColumns\": [],\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{";
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

    public static String getPayloadForInputTextType(String filterName, int filterId, int entityTypeId, int listDataOffset,
                                                    int listDataSize) {
        String payload = null;
        String textValue = AEFilterHelper.getValueForInputTextType(filterName);

        logger.info("Creating Payload for Filter {} and Option {}.", filterName, textValue);
        payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
                "\",\"filterName\":\"" + filterName + "\",\"min\":\"" + textValue + "\"}}},\"selectedColumns\":[]}";
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

                case "inputtext":
                    payload= getPayloadForInputTextType(filterName,filterId,entityTypeId,0,20);
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

    public static void testDownloadListingData(String payload, String filterName)
    {
        String outputFileName = "Autoextraction Doc Listing.xlsx";
        String outputFilePath="src/test/output";
        String aeDataEntity = "auto extraction";
        int entityUrlId=432;
        CustomAssert csAssert=new CustomAssert();
        try {
            logger.info("Hitting List Data API for Auto-extraction");
            HttpResponse listDataResponse = AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
            String listDataStr = EntityUtils.toString(listDataResponse.getEntity());
            JSONObject listDataObj = new JSONObject(listDataStr);
            int dataInListing = listDataObj.getJSONArray("data").length();
            csAssert.assertTrue(dataInListing > 0, "There is no data in AE listing");
            if (dataInListing > 0) {
                logger.info("Downloading Auto Extraction Listing Data");
                DownloadListWithData DownloadListWithDataObj = new DownloadListWithData();
                Map<String, String> formParam = new HashMap<>();
                formParam.put("_csrf_token", "null");
                formParam.put("jsonData", payload);

                HttpResponse downloadResponse = DownloadListWithDataObj.hitDownloadListWithData(formParam, entityUrlId);
                logger.info("Checking if Auto extraction listing is downloaded");
                String downloadStatus =DownloadListWithDataObj.dumpDownloadListWithDataResponseIntoFile(downloadResponse, outputFilePath, "ListDataDownloadOutput",aeDataEntity,"Autoextraction Doc Listing");
                if (downloadStatus == null) {
                    csAssert.assertTrue(false, "Auto Extraction List Download is unsuccessful"+filterName);
                }
                else {
                    logger.info("Excel has been downloaded successfully for FilterName:"+ filterName);
                }
            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating AE listing due to: "+e.getMessage());
        }
        finally {
            logger.info("Deleting downloaded file from location from:"+outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            FileUtils.deleteFile(outputFilePath + "/" + "ListDataDownloadOutput"+"/"+aeDataEntity + "/" + outputFileName);
            logger.info(outputFileName+" Downloaded sheet Deleted successfully.");
        }
        csAssert.assertAll();
    }

}

