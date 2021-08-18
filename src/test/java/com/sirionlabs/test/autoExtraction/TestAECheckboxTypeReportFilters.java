package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.*;

public class TestAECheckboxTypeReportFilters extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestAECheckboxTypeReportFilters.class);

    public int filterId = -1;
    String filterName;
    Map<Integer, String> allBlankFilters = new HashMap<>();
    List<String> allFilterType;
    static String filterDataResponse;

    @BeforeClass
    public void beforeClass() {
        allFilterType = getFilterTypeToTest();
        filterDataResponse=getFilterDataResponse();
    }

    public List<String> getFilterTypeToTest() {
        String[] filterType = {"blank","does not have"};
        return Arrays.asList(filterType);
    }

    public String getFilterDataResponse()
    {
        logger.info("Hitting Filter Data API for AE Tracker Report listing");
        ReportRendererFilterData filterDataObj = new ReportRendererFilterData();
        filterDataObj.hitReportRendererFilterData(520);
        return filterDataObj.getReportRendererFilterDataJsonStr();
    }

    public Map<Integer, String> getFilterIdAndName(String filterType) throws IOException {
        CustomAssert csAssert = new CustomAssert();
        try {
            JSONObject filterDataJson = new JSONObject(filterDataResponse);
            String[] allJsonObj = JSONObject.getNames(filterDataJson);

            for (String objName : allJsonObj) {
                filterName = filterDataJson.getJSONObject(objName).getString("filterName");
                if (filterType.equalsIgnoreCase("blank") && (filterName.equalsIgnoreCase("status") || filterName.equalsIgnoreCase("doctagids") || filterName.equalsIgnoreCase("doctag1ids") ||
                        filterName.equalsIgnoreCase("doctag2ids") || filterName.equalsIgnoreCase("batchids"))) {
                    filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);
                    allBlankFilters.put(filterId, filterName);
                } else if (filterType.equalsIgnoreCase("does not have") && (filterName.equalsIgnoreCase("doctagids") || filterName.equalsIgnoreCase("doctag1ids") ||
                        filterName.equalsIgnoreCase("doctag2ids")|| (filterName.equalsIgnoreCase("categoryId"))|| filterName.equalsIgnoreCase("fieldId"))) {
                    filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);
                    allBlankFilters.put(filterId, filterName);
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Filter Data API is not working due to: " + e.getMessage());

        }
        return allBlankFilters;
    }

    //C153265: Verify that Blank Filter functionality should be working fine
    //C153266: Verify that Does not have filter functionality is working fine
    @Test
    public void blankAndExcludeFiltersForAETrackerReport() throws IOException {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Start Test: Validating checkbox Type filter for AE Tracker Report");
        for (String filterType : allFilterType) {
            try {
                logger.info("Validating filter for filter type " + filterType);
                TestAECheckboxTypeReportFilters obj = new TestAECheckboxTypeReportFilters();
                allBlankFilters = obj.getFilterIdAndName(filterType);
                for (Map.Entry<Integer, String> entry : allBlankFilters.entrySet()) {
                    filterName = entry.getValue();
                    logger.info("Applying Filter for" + filterType + " AE Tracker Report for filterName " + filterName);
                    HttpResponse blankFiltersResponse = AutoExtractionHelper.blankFiltersForReport(payloadForFilterType(filterType, filterName));
                    String blankFilterStr = EntityUtils.toString(blankFiltersResponse.getEntity());
                    JSONObject blankFilterObj = new JSONObject(blankFilterStr);
                    int dataLength = blankFilterObj.getJSONArray("data").length();
                    int documentId = ListDataHelper.getColumnIdFromColumnName(blankFilterStr, "id");
                    String columnNameToCheck = "";
                    if (filterName.equals("doctag1ids")) {
                        columnNameToCheck = "doctag1";
                    } else if (filterName.equals("doctag2ids")) {
                        columnNameToCheck = "doctag2";
                    } else if (filterName.equals("doctagids")) {
                        columnNameToCheck = "doctags";
                    } else if (filterName.equals("status")) {
                        columnNameToCheck = "workflowstatus";
                    } else if (filterName.equals("batchids")) {
                        columnNameToCheck = "batch";
                    }
                    if (dataLength > 1) {
                        if (filterName.equalsIgnoreCase("fieldId") || filterName.equalsIgnoreCase("categoryId")) {
                            for(int i=0;i<dataLength;i++)
                            {
                                int columnId=-1;
                                int listId=-1;
                                if(filterName.equalsIgnoreCase("fieldId"))
                                {
                                    listId=433;
                                }
                                else if(filterName.equalsIgnoreCase("categoryId"))
                                {
                                    listId=493;
                                }
                                List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
                                String optionName = allOptions.get(0).get("name");
                                String[] idValue = blankFilterObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(documentId)).get("value").toString().split(":;");
                                logger.info("Validating Record Id " + idValue[1] + "for Filter name " + filterName + " and " + optionName);
                                String payloadForClause = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366,\"filterName\":\""+filterName+"\"},\""+filterId+"\":{\""+filterId+"\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":" + idValue[1] + "}";
                                HttpResponse tabListResponse = AutoExtractionHelper.getTabData(payloadForClause, listId);
                                String tabListResponseStr = EntityUtils.toString(tabListResponse.getEntity());
                                List<String> tabListData=new ArrayList<>();
                                if (ParseJsonResponse.validJsonResponse(tabListResponseStr)) {
                                    if(listId==493) {
                                        columnId = ListDataHelper.getColumnIdFromColumnName(tabListResponseStr, "name");
                                    }
                                    else if(listId==433) {
                                        columnId = ListDataHelper.getColumnIdFromColumnName(tabListResponseStr, "fieldname");
                                    }
                                    JSONObject obj1 = new JSONObject(tabListResponseStr);
                                    JSONArray arrObj = obj1.getJSONArray("data");
                                    for (int k = 0; k < arrObj.length(); k++) {

                                        String[] temp = arrObj.getJSONObject(k).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;");
                                        tabListData.add(temp[0]);
                                    }
                                    csAssert.assertTrue(!(tabListData.stream().anyMatch(optionName::equalsIgnoreCase)), "Show Page validation failed for Record Id " + idValue[1] +
                                            " of Filter " + filterName + " and Option " + optionName );
                                } else {
                                    logger.error("Tab List API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", idValue[1], filterName, optionName);
                                    csAssert.assertTrue(false, "Tab List API Response for Record Id " + idValue[1] + " of Filter " +
                                            filterName + " and Option " + optionName + " is an Invalid JSON.");
                                }


                            }


                        } else {
                            int columnId = ListDataHelper.getColumnIdFromColumnName(blankFilterStr, columnNameToCheck);
                            if (!filterType.equals("does not have")) {
                                for (int i = 0; i < dataLength; i++) {
                                    String value = blankFilterObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString();
                                    String[] idValue = blankFilterObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(documentId)).get("value").toString().split(":;");
                                    csAssert.assertTrue(value.equalsIgnoreCase("null"), "Blank Filter is not working for : " + filterName + " for document id " + idValue[1]);
                                }
                            } else {
                                List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
                                String optionName = allOptions.get(0).get("name");
                                for (int i = 0; i < dataLength; i++) {
                                    String[] idValue = blankFilterObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(documentId)).get("value").toString().split(":;");
                                    String[] value = blankFilterObj.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnId)).get("value").toString().split("::");

                                    if (value.length > 1) {
                                        ArrayList<String> valueSet = new ArrayList<>();
                                        for (int j = 0; j < value.length; j++) {
                                            valueSet.add(value[j]);
                                        }
                                        csAssert.assertTrue(!valueSet.contains(optionName), "Does not have filter is not working for option " + optionName + " for document Id " + idValue[1]);

                                    } else {
                                        csAssert.assertTrue(!value.equals(optionName), "Does not have filter is not working for option " + optionName + " for document Id " + idValue[1]);
                                    }
                                }

                            }

                            logger.info("Validating filter result on AE Tracker report listing page for filter name " + filterName);
                        }
                    }else {
                        logger.warn("There is no enough data present to test for column " + entry.getValue());
                    }
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "AE tracker Report List Data API is not working for filter Type " + filterType + " Filters due to:" + e.getMessage());
            }
        }
        csAssert.assertAll();

    }


    public String payloadForFilterType(String filterType, String filterName) {
        Integer filterIdToValidate = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);
        String payload = " ";
        switch (filterType) {
            case "blank":
                payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":0,\"name\":\"blank\"}]}," +
                        "\"filterId\":\"" + filterId + "\",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";

                break;

            case "does not have":
                logger.info("Hitting Filter Data API for AE Tracker Report listing");
                List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
                payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterIdToValidate + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + allOptions.get(0).get("id") + "\",\"name\":\"" + allOptions.get(0).get("name") + "\"}]}," +
                        "\"filterId\":\"" + filterIdToValidate + "\",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"operator\":\"not\"}}},\"selectedColumns\":[]}";
                break;
        }
        return payload;

    }

}
