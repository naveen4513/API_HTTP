package com.sirionlabs.helper.Reports;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.userGroups.UserGroupsUsers;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionReportFilterHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class ReportsFilterHelper {

    private final static Logger logger = LoggerFactory.getLogger(ReportsFilterHelper.class);

    private static Boolean isFilterOfAutoCompleteType(String filterDataResponse, String filterName) {
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

    private static List<Map<String, String>> getAllOptionsOfAutoCompleteFilter(String filterName, int entityTypeId) {
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

    public static void validateFiltersOfCheckBoxType(String reportName, Integer reportId, String filterDataResponse, String filterName, Integer filterId,
                                                     String showPageObjectName, int entityTypeId, String entityName, Boolean applyRandomization, int maxFilterOptionsToValidate,
                                                     int maxRecordsToValidate, int listDataOffset, int listDataSize, CustomAssert csAssert) {
        try {
            List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

            if (allOptions.size() > 0) {
                List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                for (Map<String, String> filterOption : optionsToTest) {
                    String optionName = filterOption.get("name");
                    String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {
                        optionName = optionName.trim().equalsIgnoreCase("yes") ? "true" : optionName;
                        optionName = optionName.trim().equalsIgnoreCase("no") ? "false" : optionName;

                        hitListDataAndVerifyResults(reportName, reportId, filterName, optionName, payload, showPageObjectName, entityTypeId, entityName, csAssert, "checkbox", applyRandomization,
                                maxRecordsToValidate);
                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
                    }
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

    public static void validateFiltersOfAutoCompleteType(String reportName, Integer reportId, String filterName, Integer filterId, String showPageObjectName, int entityTypeId,
                                                         String entityName, Boolean applyRandomization, int maxFilterOptionsToValidate, int maxRecordsToValidate, int listDataOffset,
                                                         int listDataSize, CustomAssert csAssert) {
        try {
            List<Map<String, String>> allOptions = getAllOptionsOfAutoCompleteFilter(filterName, entityTypeId);

            if (allOptions.size() > 0) {
                List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                for (Map<String, String> filterOption : optionsToTest) {
                    String optionName = filterOption.get("name");

                    String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {

                        hitListDataAndVerifyResults(reportName, reportId, filterName, optionName, payload, showPageObjectName, entityTypeId, entityName, csAssert,
                                "select", applyRandomization, maxRecordsToValidate);
                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
                    }
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

    public static void validateFiltersOfSelectType(String reportName, Integer reportId, String filterDataResponse, String filterName, Integer filterId,
                                                   String showPageObjectName, int entityTypeId, String entityName, Boolean applyRandomization, int maxFilterOptionsToValidate,
                                                   int maxRecordsToValidate, int listDataOffset, int listDataSize, CustomAssert csAssert) {
        try {
            List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

            if (allOptions.size() > 0) {
                List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                for (Map<String, String> filterOption : optionsToTest) {
                    String optionName = filterOption.get("name");

                    String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {
                        if(entityTypeId==316) {

                            AutoExtractionReportFilterHelper reportFilterHelper = new AutoExtractionReportFilterHelper();

                            reportFilterHelper.autoExtractionhitListDataAndVerifyResults(reportName, reportId, filterName, optionName, payload, showPageObjectName, entityTypeId, entityName, csAssert, applyRandomization,
                                    maxRecordsToValidate);
                        }
                        else {
                            hitListDataAndVerifyResults(reportName, reportId, filterName, optionName, payload, showPageObjectName, entityTypeId, entityName, csAssert,
                                    "select", applyRandomization,
                                    maxRecordsToValidate);
                        }

                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
                    }
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

    public static void validateFiltersOfDateType(String reportName, Integer reportId, String filterName, Integer filterId, String showPageObjectName, int entityTypeId, String entityName, Boolean applyRandomization,
                                                 int maxRecordsToValidate, int listDataOffset, int listDataSize, String startDate, String endDate, CustomAssert csAssert) {
        try {
            String payload = getPayloadForDateType(filterName, filterId, entityTypeId, listDataOffset, listDataSize, startDate, endDate);

            if (payload != null) {
                hitListDataAndVerifyResultsForDateType(reportName, reportId, filterName, filterId, startDate, endDate, payload, showPageObjectName, entityTypeId, entityName, listDataOffset, listDataSize, csAssert, applyRandomization,
                        maxRecordsToValidate);
            } else {
                throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Date Range " + startDate + " - " + endDate + ". Hence skipping test");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Date. " + e.getMessage());
        }
    }

    public static void validateFiltersOfSliderType(String reportName, Integer reportId, String filterName, Integer filterId, String showPageObjectName, int entityTypeId,
                                                   String entityName, Boolean applyRandomization, int maxRecordsToValidate, int listDataOffset, int listDataSize, String minSliderValue,
                                                   String maxSliderValue, CustomAssert csAssert) {
        try {
            String payload = getPayloadForSliderType(filterName, filterId, entityTypeId, listDataOffset, listDataSize, minSliderValue, maxSliderValue);
            String expectedValue = minSliderValue + ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter") + maxSliderValue;

            if (payload != null) {
                hitListDataAndVerifyResults(reportName, reportId, filterName, expectedValue, payload, showPageObjectName, entityTypeId, entityName, csAssert, "slider",
                        applyRandomization, maxRecordsToValidate);
            } else {
                throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Slider Range " + expectedValue + ". Hence skipping test");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Slider. " + e.getMessage());
        }
    }

    public static void validateStakeHolderFilter(String reportName, Integer reportId, String filterDataResponse, String filterName, Integer filterId, int entityTypeId, int listDataOffset, int listDataSize,
                                                 Boolean applyRandomization, int maxFilterOptionsToValidate, int maxRecordsToValidate,
                                                 CustomAssert csAssert) {
        try {
            List<Map<String, String>> allStakeHolderOptions = getAllOptionsOfStakeHolder(filterDataResponse, filterName);

            if (allStakeHolderOptions.size() > 0) {
                List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allStakeHolderOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                for (Map<String, String> filterOption : optionsToTest) {
                    String optionName = filterOption.get("name");
                    String roleGroupId = filterOption.get("roleGroupId");
                    String optionId = filterOption.get("id");
                    String payload = getPayloadForStakeHolder(filterName, filterId, filterOption, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {
                        if(entityTypeId==316){
                            AutoExtractionReportFilterHelper.hitListDataAndVerifyResultsForStakeholdersForAE(reportName, reportId, filterName, optionName, optionId, payload, entityTypeId, csAssert, roleGroupId,
                                    applyRandomization, maxRecordsToValidate);
                        }
                        else {
                            hitListDataAndVerifyResultsForStakeholders(reportName, reportId, filterName, optionName, optionId, payload, entityTypeId, csAssert, roleGroupId,
                                    applyRandomization, maxRecordsToValidate);
                        }
                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
                    }
                }
            } else {
                throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Stakeholder Filter " + filterName + ". " + e.getMessage());
        }
    }

    public static void validateFiltersOfInputTextType(String reportName, Integer reportId, String filterDataResponse, String filterName, Integer filterId,
                                                     String showPageObjectName,String textValue, int entityTypeId, String entityName, Boolean applyRandomization, int maxFilterOptionsToValidate,
                                                     int maxRecordsToValidate, int listDataOffset, int listDataSize, CustomAssert csAssert) throws IOException {

        logger.info("validating filter for "+reportName +" and "+filterName);

                    String optionName = textValue;

                    String payload = getPayloadForInputTextType(filterName, filterId, optionName, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {

                            AutoExtractionReportFilterHelper reportFilterHelper = new AutoExtractionReportFilterHelper();

                            reportFilterHelper.hitListDataAndVerifyResults(reportName, reportId, filterName, optionName, payload, showPageObjectName, entityTypeId, entityName, csAssert,
                                    "inputtext", applyRandomization,
                                    maxRecordsToValidate);


                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + textValue + ". Hence skipping test");

                }

    }

    private static void validateFiltersOfSelectTypeWithAuditLog(String reportName, Integer reportId, String filterDataResponse, String filterName, Integer filterId, int entityTypeId, Boolean applyRandomization,
                                                                int maxFilterOptionsToValidate, int maxRecordsToValidate, int listDataOffset, int listDataSize,
                                                                CustomAssert csAssert) {
        try {
            List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

            if (allOptions.size() > 0) {
                List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName, applyRandomization, maxFilterOptionsToValidate);

                for (Map<String, String> filterOption : optionsToTest) {
                    String optionName = filterOption.get("name");
                    String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId, listDataOffset, listDataSize);

                    if (payload != null) {
                        hitListDataAndVerifyResultsWithAuditLog(reportName, reportId, filterName, optionName, payload, entityTypeId, csAssert, applyRandomization,
                                maxRecordsToValidate, null, null);
                    } else {
                        throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
                    }
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

    private static void validateFiltersOfDateTypeWithAuditLog(String reportName, Integer reportId, String filterName, Integer filterId, int entityTypeId, Boolean applyRandomization,
                                                              int maxRecordsToValidate, int listDataOffset, int listDataSize, String startDate, String endDate,
                                                              String expectedDateFormat, String auditLogDateFormat, CustomAssert csAssert) {
        try {
            String payload = getPayloadForDateType(filterName, filterId, entityTypeId, listDataOffset, listDataSize, startDate, endDate);

            String fromDate = startDate;
            Date date = new SimpleDateFormat("MM-dd-yyyy").parse(fromDate);
            fromDate = new SimpleDateFormat(expectedDateFormat).format(date);

            String toDate = endDate;
            date = new SimpleDateFormat("MM-dd-yyyy").parse(toDate);
            toDate = new SimpleDateFormat(expectedDateFormat).format(date);

            String expectedDateRange = fromDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + toDate;

            if (payload != null) {
                hitListDataAndVerifyResultsWithAuditLog(reportName, reportId, filterName, expectedDateRange, payload, entityTypeId, csAssert, applyRandomization,
                        maxRecordsToValidate, expectedDateFormat, auditLogDateFormat);
            } else {
                throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Date Range " + expectedDateRange + ". Hence skipping test");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Date. " + e.getMessage());
        }
    }

    private static String getPayloadForSelectType(String filterName, Integer filterId, Map<String, String> optionMap, int entityTypeId, int listDataOffset,
                                                  int listDataSize) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {} and Option {}.", filterName, optionMap.get("name"));
           payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
                    "\",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionMap.get("id") +
                    "\",\"name\":\"" + optionMap.get("name") + "\"}]}}}}}";

           } catch (Exception e) {
            logger.error("Exception while creating Payload for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForStakeHolder(String filterName, Integer filterId, Map<String, String> optionMap, int entityTypeId, int listDataOffset,
                                                   int listDataSize) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {} and Option {}.", filterName, optionMap.get("name"));

            String userName = optionMap.get("name");
            String userId = optionMap.get("id");
            String roleGroupId = optionMap.get("roleGroupId");
            String roleGroupName = optionMap.get("roleGroupName");
            String idType = optionMap.get("idType");

            payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":" + filterId +
                    ",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + roleGroupId +
                    "\",\"name\":\"" + roleGroupName + "\",\"group\":[{\"name\":\"" + userName + "\",\"id\":" + userId + ",\"idType\":" + idType +
                    ",\"selected\":true}],\"type\":" + optionMap.get("typeId") + "},{\"name\":\"" + userName + "\",\"id\":" + userId + ",\"idType\":" + idType +
                    ",\"selected\":true}]},\"uitype\":\"STAKEHOLDER\"}}}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForInputTextType(String filterName, Integer filterId, String textValue, int entityTypeId, int listDataOffset,
                                                     int listDataSize) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {} and Option {}.", filterName, textValue);

            payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
                    "\",\"filterName\":\"" + filterName + "\",\"min\":\""+textValue+"\"}}},\"selectedColumns\":[]}";


        } catch (Exception e) {
            logger.error("Exception while creating Payload for Filter {} and Option {}. {}", filterName, textValue, e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForDateType(String filterName, Integer filterId, int entityTypeId, int listDataOffset, int listDataSize, String startDate,
                                                String endDate) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {}", filterName);

            payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
                    "\",\"filterName\":\"" + filterName + "\",\"start\":\"" + startDate + "\",\"end\":\"" + endDate +
                    "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
        }
        return payload;
    }

    private static String getPayloadForSliderType(String filterName, Integer filterId, int entityTypeId, int listDataOffset, int listDataSize, String minSliderValue,
                                                  String maxSliderValue) {
        String payload = null;

        try {
            logger.info("Creating Payload for Filter {}", filterName);

            payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize +
                    ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
                    "\",\"filterName\":\"" + filterName + "\",\"min\":\"" + minSliderValue + "\",\"max\":\"" + maxSliderValue + "\"}}}}";
        } catch (Exception e) {
            logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
        }
        return payload;
    }

    private static void hitListDataAndVerifyResultsForStakeholders(String reportName, Integer reportId, String filterName, String optionName, String optionId, String payload,
                                                                   int entityTypeId, CustomAssert csAssert, String roleGroupId, Boolean applyRandomization,
                                                                   int maxRecordsToValidate) {
        try {
            logger.info("Hitting List Data API for Filter {}, Role Group Id {} and Option {}", filterName, roleGroupId, optionName);
            ReportRendererListData listDataObj = new ReportRendererListData();

            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, filterName, optionName,
                    payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
                int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);

                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }

                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
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
                                                    ", Entity Type Id " + entityTypeId + " and Record Id " + recordId + " is an Invalid JSON.");
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
                    } else {
                        logger.warn("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
                        /*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName +
                                " and Option " + optionName);*/
                    }
                } else {
                    logger.error("Show API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
                            filterName + " and Option " + optionName + " is an Invalid JSON.");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

    private static void hitListDataAndVerifyResultsForDateType(String reportName, Integer reportId, String filterName, Integer filterId, String startDate, String endDate, String payload, String showPageObjectName,
                                                               int entityTypeId, String entityName, int listDataOffset, int listDataSize, CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) {
        try {
           String  expectedValue = startDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + endDate;
            logger.info("Hitting List Data API for Filter {} and Option {}", filterName, expectedValue);
            ReportRendererListData listDataObj = new ReportRendererListData();
            listDataObj.hitReportRendererListData(reportId, payload);
            String reportListDataResponse = listDataObj.getListDataJsonStr();
            List<Map<Integer, Map<String, String>>> allRecordsToValidate = ReportsFilterHelper.getListDataResultsToValidate(reportListDataResponse,
                    filterName,expectedValue, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);

            if (filterName.contains("Date")||filterName.equals("createdDate") || filterName.equals("modifiedDate") || filterName.equals("activeDate") ||filterName.equalsIgnoreCase("reviewDate")) {

                verifyResultsForDateTypeListingPage(allRecordsToValidate, filterName,expectedValue, csAssert);

            }


//            if (filterName.equalsIgnoreCase("createdDate") || filterName.equalsIgnoreCase("modifiedDate") ) {
//                List<String> reportListDataIds = ListDataHelper.getColumnIds(reportListDataResponse);
//
//                String expectedDateValue = startDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + endDate;
//
//                String completeFilterPayload = ListRendererFilterDataHelper.getFiltersPayloadPrefix(entityTypeId, listDataOffset, listDataSize);
//
//                String listFilterPayload = ListRendererFilterDataHelper.getPayloadForSingleFilter(filterName, filterId, "date", entityTypeId, null,
//                        expectedDateValue);
//
//                if (listFilterPayload != null) {
//                    completeFilterPayload = completeFilterPayload.concat(listFilterPayload);
//                }
//
//                completeFilterPayload = completeFilterPayload.substring(0, completeFilterPayload.length() - 1);
//                completeFilterPayload = completeFilterPayload.concat(ListRendererFilterDataHelper.getFiltersPayloadSuffix());
//
//                ListRendererListData listRendererListData = new ListRendererListData();
//                listRendererListData.hitListRendererListData(ConfigureConstantFields.getListIdForEntity(entityName), completeFilterPayload);
//                String listDataResponse = listRendererListData.getListDataJsonStr();
//
//                List<String> listDataIds = ListDataHelper.getColumnIds(listDataResponse);
//
//                if (!validateListIds(reportListDataIds, listDataIds, csAssert)) {
//                    logger.error("Filter " + filterName + " validated unsuccessfully");
//                    csAssert.assertTrue(false, "Filter " + filterName + " validated unsuccessfully");
//                }
//                return;
//            }
//
//            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, filterName, optionName, payload,
//                    entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
//            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());
//
//            String expectedDateRange = null;
//            String expectedDateFormat = null;
//
//            String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageObjectName, entityTypeId);
//            String lastObjectName = ShowHelper.getLastObjectNameFromHierarchy(fieldHierarchy);
//
//            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
//                int idColumnNo = getIdColumnNoForReport(reportListDataResponse, reportId);
//
//                if (idColumnNo == -1) {
//                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
//                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
//                }
//
//                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));
//
//                logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
//                Show showObj = new Show();
//                showObj.hitShow(entityTypeId, recordId);
//                String showResponse = showObj.getShowJsonStr();
//
//                if (ParseJsonResponse.validJsonResponse(showResponse)) {
//                    boolean result = false;
//
//                    JSONObject jsonObj = new JSONObject(showResponse);
//                    String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
//
//                    if (showStatus.trim().equalsIgnoreCase("success")) {
//                        if (expectedDateFormat == null) {
//                            expectedDateFormat = ShowHelper.getExpectedDateFormat(showResponse, lastObjectName, fieldHierarchy);
//                        }
//
//                        if (expectedDateFormat != null) {
//
//                            if (expectedDateRange == null) {
//                                String fromDate = startDate;
//                                Date date = new SimpleDateFormat("MM-dd-yyyy").parse(fromDate);
//                                fromDate = new SimpleDateFormat(expectedDateFormat).format(date);
//
//                                String toDate = endDate;
//                                date = new SimpleDateFormat("MM-dd-yyyy").parse(toDate);
//                                toDate = new SimpleDateFormat(expectedDateFormat).format(date);
//
//                                expectedDateRange = fromDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + toDate;
//                            }
//
//                            result = showObj.verifyShowField(showResponse, showPageObjectName, expectedDateRange, entityTypeId, "date", expectedDateFormat);
//                        } else {
//                            logger.error("Couldn't get Expected Date Format for Filter {}, Option {} and Record Id {}", filterName, optionName, recordId);
//                        }
//
//                        if (!result) {
//                            logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
//                            csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
//                                    " of Filter " + filterName + " and Option " + optionName);
//                        }
//                    } else {
//                        logger.warn("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
//                        /*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName +
//                                " and Option " + optionName);*/
//                    }
//                } else {
//                    logger.error("Show API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", recordId, filterName, optionName);
//                    csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
//                            filterName + " and Option " + optionName + " is an Invalid JSON.");
//                }
//            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
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

    private static void hitListDataAndVerifyResults(String reportName, Integer reportId, String filterName, String optionName, String payload, String showPageObjectName,
                                                    int entityTypeId, String entityName, CustomAssert csAssert, String filterType, Boolean applyRandomization, int maxRecordsToValidate) {
        try {
            logger.info("Hitting Report List Data API for Filter {} and Option {} of Report [{}] having Id {}", filterName, optionName, reportName, reportId);
            ReportRendererListData listDataObj = new ReportRendererListData();
            listDataObj.hitReportRendererListData(reportId, payload);
            String listDataResponse = listDataObj.getListDataJsonStr();

            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, listDataResponse, filterName,
                    optionName, payload, entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName, reportName, reportId,
                    listDataToValidate.size());

            int vendorEntityTypeId = ConfigureConstantFields.getEntityIdByName("vendors");
            int obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

            if (entityTypeId != vendorEntityTypeId && filterName.equalsIgnoreCase("vendorHierarchy")) {
                validateVendorHierarchyFilter(reportName, reportId, listDataToValidate, listDataResponse, filterName, optionName, showPageObjectName,
                        entityTypeId, csAssert, filterType);
                return;
            }

            if (filterName.equalsIgnoreCase("source")) {
                validateSourceFilter(reportName, reportId, payload, listDataToValidate, listDataResponse, filterName, optionName, entityTypeId, csAssert, filterType);
                return;
            }

            if (filterName.equalsIgnoreCase("lastmodifiedby") || filterName.equalsIgnoreCase("createdby")) {
                listDataObj.hitReportRendererListData(reportId, payload);
                String reportListDataResponse = listDataObj.getListDataJsonStr();

                List<String> reportListDataIds = ListDataHelper.getColumnIds(reportListDataResponse);

                ListRendererListData listRendererListData = new ListRendererListData();
                listRendererListData.hitListRendererListData(ConfigureConstantFields.getListIdForEntity(entityName), payload);
                listDataResponse = listRendererListData.getListDataJsonStr();

                List<String> listDataIds = ListDataHelper.getColumnIds(listDataResponse);

                if (!validateListIds(reportListDataIds, listDataIds, csAssert)) {
                    logger.error("Filter " + filterName + " validated unsuccessfully");
                    csAssert.assertTrue(false, "Filter " + filterName + " validated unsuccessfully");
                }
                return;
            }

            //Special handling for Filter Service Data of Entity Consumption/Service Data
            if (filterName.equalsIgnoreCase("serviceData") && (entityTypeId == 176 || entityTypeId == 64)) {
                String[] temp = optionName.split(Pattern.quote("("));
                optionName = temp[0].trim();
            }

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
                int recordEntityTypeId = entityTypeId;
                Show showObj = new Show();
                String showResponse;
                int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);

                if (idColumnNo == -1) {
                    logger.warn("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                    throw new SkipException("Couldn't find Id of Column ID in ListData Response. Hence skipping test.");
                }

                int recordId = Integer.parseInt(recordToValidate.get(idColumnNo).get("valueId"));

                try {
                    if (entityTypeId == ConfigureConstantFields.getEntityIdByName("invoice line item") &&
                            filterName.equalsIgnoreCase("invoiceFilter")) {
                        logger.info("Hitting Show API for Record Id {} of Invoice Line Item.", recordId);
                        showObj.hitShowVersion2(entityTypeId, recordId);
                        showResponse = showObj.getShowJsonStr();

                        if (ShowHelper.isShowPageAccessible(showResponse)) {
                            JSONObject jsonObj = new JSONObject(showResponse);
                            recordId = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("parentShortCodeId").getJSONObject("values").getInt("id");
                            recordEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
                            showPageObjectName = "name";
                        }
                    }

                    logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);

                    showObj.hitShowVersion2(recordEntityTypeId, recordId);
                    showResponse = showObj.getShowJsonStr();

                    if (!ShowHelper.isShowPageAccessible(showResponse)) {
//                        csAssert.assertTrue(false, "Show Page is Not Accessible for Record Id " + recordId + " of Entity Type Id " + entityTypeId);
                        return;
                    }

                    //Special Handling for Multi Supplier Filter
                    if (filterName.equalsIgnoreCase("multisuppliercontracts")) {
                        validateMultiSupplierContractFilter(reportId, reportName, entityName, entityTypeId, recordId, filterName, showResponse, optionName, csAssert);
                        return;
                    }

                    //added by gaurav bhadani on 24 july
                    if (entityTypeId == obligationEntityTypeId && filterName.equalsIgnoreCase("documentType")) {
                        int parentrecordEntityTypeId = ShowHelper.getParentEntityTypeId(showResponse);
                        int parentrecordId = ShowHelper.getParentEntityId(showResponse);
                        showObj.hitShowVersion2(parentrecordEntityTypeId, parentrecordId);
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

                    //Special Handling for Status filter of GBM
                    if (entityTypeId == 87 && filterName.equalsIgnoreCase("status")) {
                        int parentRecordId = ShowHelper.getParentEntityId(showResponse);
                        showResponse = ShowHelper.getShowResponse(86, parentRecordId);
                    }

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        boolean result;

                        if (ShowHelper.isShowPageAccessible(showResponse)) {
                            result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, recordEntityTypeId, filterType.trim());

                            if (!result) {
                                //Special code for Regions field.
                                if (showPageObjectName.trim().equalsIgnoreCase("globalRegions") ||
                                        showPageObjectName.trim().equalsIgnoreCase("contractRegions")) {
                                    showPageObjectName = showPageObjectName.trim().equalsIgnoreCase("globalRegions") ? "contractregions" : "globalregions";

                                    result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, recordEntityTypeId, filterType.trim());
                                } else if (showPageObjectName.trim().equalsIgnoreCase("aging")) {
                                    //Special code for Aging field.
                                    showPageObjectName = "agingvalue";
                                    result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, recordEntityTypeId, filterType.trim());
                                }
                            }

                            if (!result) {
                                logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {} of Report [{}] having Id {}", recordId, filterName,
                                        optionName, reportName, reportId);
                                csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                                        " of Filter " + filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId);
                            }
                        } else {
                            logger.warn("Show Page Not Accessible for Record Id {} of Filter {} and Option {} of Report [{}] having Id {}", recordId, filterName, optionName,
                                    reportName, reportId);
                        /*csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName + " and Option "
                                + optionName + " of Report [" + reportName + "] having Id " + reportId);*/
                        }
                    } else {
                        logger.error("Show API Response for Record Id {} of Filter {} and Option {} of Report [{}] having Id {} is an Invalid JSON.", recordId, filterName,
                                optionName, reportName, reportId);
                        csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
                                filterName + " and Option " + optionName + " of Report [" + reportName + "] having Id " + reportId + " is an Invalid JSON.");

                        FileUtils.saveResponseInFile("Filter " + filterName + " EntityTypeId " + entityTypeId + " Record Id " + recordId + " Show API HTML.txt",
                                showResponse);
                    }
                } catch (Exception e) {
                    csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + ", Option " + optionName + ", EntityTypeId " +
                            recordEntityTypeId + ", Record Id " + recordId + ". " + e.getMessage());
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
        }
    }

    private static void hitListDataAndVerifyResultsWithAuditLog(String reportName, Integer reportId, String filterName, String optionName, String payload, int entityTypeId, CustomAssert csAssert,
                                                                Boolean applyRandomization, int maxRecordsToValidate, String expectedDateFormat, String auditLogDateFormat) {
        try {
            logger.info("Hitting List Data API for Filter {} and Option {}", filterName, optionName);
            ReportRendererListData listDataObj = new ReportRendererListData();

            List<Map<Integer, Map<String, String>>> listDataToValidate = getListDataResultsToValidate(reportName, reportId, listDataObj, filterName, optionName, payload,
                    entityTypeId, csAssert, applyRandomization, maxRecordsToValidate);
            logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());

            for (Map<Integer, Map<String, String>> recordToValidate : listDataToValidate) {
                int idColumnNo = getIdColumnNoForReport(listDataObj.getListDataJsonStr(), reportId);
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
                        boolean result;

                        if (filterName.equalsIgnoreCase("createdDate") || filterName.equalsIgnoreCase("modifiedDate")) {
                            expectedValueColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogTabListResponse, "audit_log_user_date");

                            if (expectedValueColumnId != null) {
                                actualValueInAuditLogTab = auditLogListData.get(0).get(Integer.parseInt(expectedValueColumnId.trim())).get("value");

                                if (!actualValueInAuditLogTab.trim().contains("AoE")) {
                                    String[] temp = optionName.split(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter"));
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
    }

    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String reportName, Integer reportId, ReportRendererListData listDataObj,
                                                                                        String filterName, String optionName, String payload, int entityTypeId,
                                                                                        CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) {
        return getListDataResultsToValidate(reportName, reportId, listDataObj, null, filterName, optionName, payload, entityTypeId,
                csAssert, applyRandomization, maxRecordsToValidate);
    }

    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String reportName, Integer reportId, ReportRendererListData listDataObj,
                                                                                        String listDataResponse, String filterName, String optionName, String payload,
                                                                                        int entityTypeId, CustomAssert csAssert, Boolean applyRandomization,
                                                                                        int maxRecordsToValidate) {
        List<Map<Integer, Map<String, String>>> listDataResults = new ArrayList<>();

        try {
            if (listDataResponse == null) {
                listDataObj.hitReportRendererListData(reportId, payload);
                listDataResponse = listDataObj.getListDataJsonStr();
            }

            filterName = filterName.trim();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                listDataObj.setListData(listDataResponse);
                List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
                logger.info("Total Results found for Filter {} and Option {} of Report [{}] having Id {}: {}", filterName, optionName, reportName, reportId, listData.size());

                if (listData.size() > 0) {
                    listDataResults = filterListDataResultsToValidate(listData, applyRandomization, maxRecordsToValidate);
                }
            } else {
                logger.error("List Data API Response for Filter {} and Option {} of Report [{}] having Id {} is an Invalid JSON.", filterName, optionName, reportName,
                        reportId);
                csAssert.assertTrue(false, "List Data API Response for Filter " + filterName + " and Option " + optionName + " of Report [" + reportName +
                        "] having Id " + reportId + " is an Invalid JSON.");
                FileUtils.saveResponseInFile("ListData Filter " + filterName + " Report Id " + reportId + ".txt", listDataResponse);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting List Data Results to Validate for Filter {} and Option {} of Entity Type Id {}. {}", filterName, optionName,
                    entityTypeId, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Getting List Data Results to Validate for Filter " + filterName + ", Option " + optionName +
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

    private static Boolean validateListIds(List<String> reportListDataIds, List<String> listDataIds, CustomAssert csAssert) {

        boolean validationStatus = true;

        if (!(reportListDataIds.size() == listDataIds.size())) {
            validationStatus = false;
            csAssert.assertTrue(false, "Total Report List Data Size and List Data Response size are not equal");
        }
        for (String reportListId : reportListDataIds) {

            if (listDataIds.contains(reportListId)) {

                csAssert.assertTrue(true, "Report List Id " + reportListId + " not found in list data response");

            } else {
                logger.error("Report List Id " + reportListId + " not found in list data response");
                csAssert.assertTrue(false, "Report List Id " + reportListId + " not found in list data response");
                validationStatus = false;
            }
        }
        return validationStatus;
    }

    private static int getIdColumnNoForReport(String listDataResponse, int reportId) {
        String columnName;

        switch (reportId) {
            case 222:
            case 223:
            case 224:
            case 270:
                columnName = "sirion_id";
                break;

            case 444:
                columnName = "servicedataid";
                break;

            default:
                columnName = "id";
                break;
        }

        return ListDataHelper.getColumnIdFromColumnName(listDataResponse, columnName);
    }

    private static void validateMultiSupplierContractFilter(int reportId, String reportName, String entityName, int entityTypeId, int recordId, String filterName,
                                                            String showResponse, String expectedValue, CustomAssert csAssert) {
        try {
            boolean expectedMultipleSuppliers = (expectedValue.equalsIgnoreCase("yes") || expectedValue.equalsIgnoreCase("true"));
            String fieldHierarchy = ShowHelper.getShowFieldHierarchy("relations", entityTypeId);
            List<String> allValues = ShowHelper.getAllSelectValuesOfField(showResponse, "relations", fieldHierarchy, recordId, entityTypeId);

            if (allValues == null) {
                csAssert.assertFalse(true, "Couldn't get All Relation Values from Show API Response for Record Id " + recordId + " of Entity " +
                        entityName);
            } else {
                boolean actualMultipleSuppliers = allValues.size() > 1;

                csAssert.assertEquals(expectedMultipleSuppliers, actualMultipleSuppliers, "Expected Multiple Suppliers Value: " + expectedMultipleSuppliers +
                        " and Actual Multiple Suppliers Value: " + actualMultipleSuppliers);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + " for Report [" + reportName + "] having Id " +
                    reportId + ", Entity " + entityName + ", Record Id " + recordId + " and Expected Value: " + expectedValue + ". " + e.getMessage());
        }
    }

    private static void validateVendorHierarchyFilter(String reportName, int reportId, List<Map<Integer, Map<String, String>>> allRecordsToValidate,
                                                      String listDataResponse, String filterName, String expectedValue, String showPageObjectName,
                                                      int entityTypeId, CustomAssert csAssert, String filterType) {
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
            csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + " of Report [" + reportName + "] having Id " +
                    reportId + ". " + e.getMessage());
        }
    }

    private static void validateSourceFilter(String reportName, int reportId, String listDataPayload, List<Map<Integer, Map<String, String>>> allRecordsToValidate,
                                             String listDataResponse, String filterName, String expectedValue, int entityTypeId, CustomAssert csAssert, String filterType) {
        try {
            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
            JSONObject jsonObj = new JSONObject(listDataPayload).getJSONObject("filterMap").getJSONObject("filterJson");
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
                    logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {} of Report [{}] having Id {}", recordId, filterName,
                            expectedValue, reportName, reportId);
                    csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
                            " of Filter " + filterName + " and Option " + expectedValue + " of Report [" + reportName + "] having Id " + reportId);
                }

            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Filter " + filterName + " of Report [" + reportName + "] having Id " +
                    reportId + " and Option: " + expectedValue + ". " + e.getMessage());
        }
    }

    private static List<Map<Integer, Map<String, String>>> getListDataResultsToValidate(String listDataResponse, String filterName, String expectedValue, int entityTypeId,
                                                                                        CustomAssert csAssert, Boolean applyRandomization, int maxRecordsToValidate) {
        List<Map<Integer, Map<String, String>>> listDataResults = new ArrayList<>();

        try {
            filterName = filterName.trim();
            ReportRendererListData listDataObj = new ReportRendererListData ();

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

}

