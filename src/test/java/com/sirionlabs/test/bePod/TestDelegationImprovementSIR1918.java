package com.sirionlabs.test.bePod;

import com.sirionlabs.api.delegation.DelegationCreateForm;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.*;

public class TestDelegationImprovementSIR1918 {

    private final static Logger logger = LoggerFactory.getLogger(TestDelegationImprovementSIR1918.class);

    private DefaultUserListMetadataHelper metadataHelperObj = new DefaultUserListMetadataHelper();
    private ListRendererListData listDataObj = new ListRendererListData();
    private ListRendererFilterData filterObj = new ListRendererFilterData();

    private String layoutURL;
    private String dataURL;
    private String filterURL;


    //TC-C89547
    @Test
    public void testCreateFormAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Create Form V3 API");
            APIResponse createFormResponse = DelegationCreateForm.getCreateFormV3Response();

            int responseCode = createFormResponse.getResponseCode();
            csAssert.assertTrue(responseCode == 200, "Expected Response Code: 200 and Actual Response Code: " + responseCode);

            String responseBody = createFormResponse.getResponseBody();

            if (ParseJsonResponse.validJsonResponse(responseBody) ) {
                String status = (String) JSONUtility.parseJson(responseBody,"$.header.response.status");
                if(status.equals("success")) {
                    List<String> allTabNames = ParseJsonResponse.getAllTabLabels(responseBody);

                    csAssert.assertTrue(allTabNames.size() == 2, "Expected No of Tabs: 2 and Actual No of Tabs: " + allTabNames.size());

                    csAssert.assertTrue(allTabNames.contains("Select Task"), "Tab [Select Task] not found in CreateForm V3 Response");
                    csAssert.assertTrue(allTabNames.contains("Select Stakeholders"), "Tab [Select Stakeholders] not found in CreateForm V3 Response");

                    layoutURL = (String) JSONUtility.parseJson(responseBody, "$.body.layoutInfo.layoutComponent.fields[0].fields[0].fields[0].layoutURL");
                    csAssert.assertEquals(layoutURL,"/listRenderer/list/488/defaultUserListMetaData","layoutURL is not correct: "+ layoutURL);
                    dataURL = (String) JSONUtility.parseJson(responseBody, "$.body.layoutInfo.layoutComponent.fields[0].fields[0].fields[0].dataURL");
                    csAssert.assertEquals(dataURL,"/listRenderer/list/488/listdata","dataURL is not correct: "+ dataURL);
                    filterURL = (String) JSONUtility.parseJson(responseBody, "$.body.layoutInfo.layoutComponent.fields[0].fields[0].fields[0].filterURL");
                    csAssert.assertEquals(filterURL,"/listRenderer/list/488/filterData","filterURL is not correct: "+ filterURL);

                }else{
                    csAssert.assertTrue(false,"CreateForm V3 Response status is: "+ status);
                }
            } else {
                csAssert.assertTrue(false, "CreateForm V3 Response is an Invalid JSON.");
            }
        } catch (SkipException e) {
            logger.warn("Skipping Case: " + e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Create Form API. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //TC-C89567
    @Test
    public void testDefaultUserListMetadataAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating DefaultUserListMetadata API.");
            logger.info("Hitting DefaultUserListMetadata API for List Id: 488");

            Map<String, String> params = new HashMap<>();
            params.put("entityTypeId", "314");

            String defaultUserListMetadataResponse = metadataHelperObj.getDefaultUserListMetadataResponse(488, params);

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                List<String> allFilterMetadataQueryNames = metadataHelperObj.getAllEnabledFilterMetadataQueryNames(defaultUserListMetadataResponse);

                csAssert.assertTrue(allFilterMetadataQueryNames.contains("entityType"),
                        "Entity Type Filter Metadata not present in DefaultUserListMetadata API Response for List Id 488");
                csAssert.assertTrue(allFilterMetadataQueryNames.contains("roleGroup"),
                        "Role Group Filter Metadata not present in DefaultUsertListMetadata API Response for List Id 488");
                csAssert.assertTrue(allFilterMetadataQueryNames.contains("supplier"),
                        "supplier Filter Metadata not present in DefaultUsertListMetadata API Response for List Id 488");
                csAssert.assertTrue(allFilterMetadataQueryNames.contains("contract"),
                        "contract Filter Metadata not present in DefaultUsertListMetadata API Response for List Id 488");

            } else {
                csAssert.assertTrue(false, "DefaultUserListMetadata API Response for ListId 488 is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating DefaultUserListMetadata API. " + e.getMessage());
        }

        csAssert.assertAll();
    }


    //TC-C89562
    @Test
    public void testFilterDataAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Filter Data API for List Id 488");

            logger.info("Hitting FilterData API for List Id 488");
            filterObj.hitListRendererFilterData(488);
            String filterDataResponse = filterObj.getListRendererFilterDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterDataResponse);

                //Validate total 4 filters
                csAssert.assertTrue(allFilterNames.size() == 4, "Expected No of Filters: 4 and Actual No of Filters: " + allFilterNames.size());

                String[] expectedFilters = {"entityType", "roleGroup", "supplier", "contract"};

                for (String expectedFilterName : expectedFilters) {
                    csAssert.assertTrue(allFilterNames.contains(expectedFilterName), "Filter " + expectedFilterName + " is not present in FilterData API Response.");
                }
            } else {
                csAssert.assertTrue(false, "FilterData API Response for List Id 488 is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Filter Data API for List Id 488. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    //TC-C89568
    @Test
    public void testListDataAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating ListData API for List Id 488");
            logger.info("Hitting ListData API for List Id 488");

            Map<String, String> params = new HashMap<>();
            params.put("version", "2.0");

            String payload = "{\"filterMap\":{\"entityTypeId\":314,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(488, payload, false, params);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                if (ListDataHelper.getFilteredListDataCount(listDataResponse) > 0) {

                    //validate
                    net.minidev.json.JSONArray data = (net.minidev.json.JSONArray) JSONUtility.parseJson(listDataResponse, "$.data");
                    csAssert.assertEquals(data.size(),20,"data in list api 488 is not as per size in payload ");

                    //Validate All columns
                    logger.info("Validating All Columns in ListData API Response.");
                    List<String> allColumns = ListDataHelper.getAllColumnName(listDataResponse);

                    logger.info("Hitting DefaultUserListMetadata API");
                    params = new HashMap<>();
                    params.put("entityTypeId", "314");

                    String defaultUserListMetadataResponse = metadataHelperObj.getDefaultUserListMetadataResponse(488, params);
                    List<String> allExpectedColumns = metadataHelperObj.getAllColumnQueryNames(defaultUserListMetadataResponse);

                    if (allColumns.size() == allExpectedColumns.size()) {
                        for (String expectedColumnName : allExpectedColumns) {
                            csAssert.assertTrue(allColumns.contains(expectedColumnName), "Expected Column " + expectedColumnName +
                                    " not found in ListData API Response");
                        }
                    } else {
                        csAssert.assertTrue(false, "Data Mismatch in DefaultUserListMetadata API and ListData API. No of Columns in " +
                                "DefaultUserListMetadata API: " + allExpectedColumns.size() + " and No of Columns in List Data API: " + allColumns.size());
                    }
                }else{
                    throw new SkipException("FilteredListDataCount is zero");
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for List Id 488 is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating List Data API. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    @Test
    public void testFilters() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Validating Filters for Delegation Create");
        logger.info("Hitting FilterData API for List Id 488");
        filterObj.hitListRendererFilterData(488);
        String filterDataResponse = filterObj.getListRendererFilterDataJsonStr();

        if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
            JSONObject jsonObj = new JSONObject(filterDataResponse);
            String[] allFilterObjects = JSONObject.getNames(jsonObj);

            for (String filter : allFilterObjects) {
                JSONObject filterJsonObj = jsonObj.getJSONObject(filter);
                String filterName = filterJsonObj.getString("filterName");

                try {
                   if (!filterName.equalsIgnoreCase("entityType") && !filterName.equalsIgnoreCase("roleGroup")) {
                        continue;
                    }

                    Boolean filterOfAutoCompleteType = ListRendererFilterDataHelper.isFilterOfAutoCompleteType(filterDataResponse, filterName);
                    if (filterOfAutoCompleteType == null) {
                        throw new SkipException("Couldn't check if Filter " + filterName + " is of AutoComplete Type or not.");
                    }

                    if (filterOfAutoCompleteType) {
                        logger.warn("Currently do not support filters of Auto Complete Type.");
                        continue;
                    }

                    List<Map<String, String>> allFilterOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
                    if (allFilterOptions == null) {
                        throw new SkipException("Couldn't get All Options of Filter " + filterName);
                    }

                    logger.info("Validating Filter {}", filterName);

                    int filterId = filterJsonObj.getInt("filterId");
                    String listDataColumnName = filterName.equalsIgnoreCase("entityType") ? "entityid" : "rolegroupname";

                    String columnId = null;

                    for (Map<String, String> filterOption : allFilterOptions) {
                        String selectedOptionId = filterOption.get("id");
                        String selectedOptionName = filterOption.get("name");
                        String groupName = filterOption.get("group");

                        String expectedValue = filterName.equalsIgnoreCase("entityType") ? selectedOptionId : getExpectedNameForRoleGroup(selectedOptionId);

                        String payload = "{\"filterMap\":{\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{\"" + filterId +
                                "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + selectedOptionId + "\"}]},\"filterId\":" + filterId +
                                ",\"filterName\":\"" + filterName + "\"}}}}";

                        logger.info("Hitting ListData API for Filter {} and Option {}", filterName, selectedOptionName);

                        listDataObj.hitListRendererListDataV2(488, payload);
                        String listDataResponse = listDataObj.getListDataJsonStr();

                        if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                            int filteredCount = ListDataHelper.getFilteredListDataCount(listDataResponse);
                            JSONArray resultsArr = new JSONObject(listDataResponse).getJSONArray("data");

                            int listDataSize = resultsArr.length();
                            if(listDataSize>30) listDataSize = 30;

                            if (filteredCount == 0) {
                                throw new SkipException("List Data failure for Filter " + filterName + " and Option " +
                                        selectedOptionName + " and Group "+groupName+". FilteredCount = 0 and Data Size: " + listDataSize);
                            }

                            if (columnId == null) {
                                columnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, listDataColumnName);
                            }

                            for (int i = 0; i < listDataSize; i++) {
                                if (!resultsArr.getJSONObject(i).getJSONObject(columnId).isNull("value")) {
                                    String actualValue = resultsArr.getJSONObject(i).getJSONObject(columnId).getString("value");

                                    if(listDataColumnName.equals("entityid"))
                                        csAssert.assertTrue(actualValue.split(":;")[2].contains(expectedValue.toLowerCase()), "List Data Mismatch. Expected Value: " +
                                                expectedValue + " and Actual Value: " + actualValue + " for Filter " + filterName+" and Group "+ groupName);
                                    else
                                        csAssert.assertTrue(actualValue.toLowerCase().trim().contains(expectedValue.toLowerCase().trim()), "List Data Mismatch. Expected Value: " +
                                                expectedValue + " and Actual Value: " + actualValue + " for Filter " + filterName+" and Group "+ groupName);
                                } else {
                                    csAssert.assertFalse(true, "List Data API Response for Filter " + filterName + " and Option " +
                                            selectedOptionName  +" and Group "+ groupName+". Value is Null");
                                }
                            }
                        } else {
                            csAssert.assertTrue(false, "ListData API Response for Filter " + filterName + " and Selected Option " +
                                    selectedOptionName + " is an Invalid JSON.");
                        }
                    }
                }
                catch (Exception e) {
                    if(e instanceof  SkipException){
                        throw new SkipException("List Data failure for Filter " + filterName);

                    }else
                    csAssert.assertTrue(false, "Exception while Validating Filter " + filterName + ". " + e.getMessage());
                }
            }
        } else {
            csAssert.assertTrue(false, "Filter Data API Response is an Invalid JSON.");
        }

        csAssert.assertAll();
    }

    private String getExpectedNameForRoleGroup(String roleGroupId) throws SQLException {
        String name = null;

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<List<String>> results = sqlObj.doSelect("select description from role_group where id = " + roleGroupId);

        if (!results.isEmpty()) {
            name = results.get(0).get(0);
        }

        sqlObj.closeConnection();

        return name;
    }
}