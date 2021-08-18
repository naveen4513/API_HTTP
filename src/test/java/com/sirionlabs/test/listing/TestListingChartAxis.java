package com.sirionlabs.test.listing;


import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListingChartAxis;
import com.sirionlabs.api.listRenderer.ListingChartData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.ListChartsDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestListingChartAxis {
    private final static Logger logger = LoggerFactory.getLogger(TestListingChartAxis.class);

    private String userLanguageId = "1";
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingChartDataFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingChartDataFileName");
    }

        @DataProvider(parallel = true)
        public Object[][] dataProviderForListingChart(){
            logger.info("Fetching all the list ids to Validate");
            List<Object[]> allTestData = new ArrayList<>();
          //  String[] allListIDsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"listidtotestlistchart").split(",");
            String[] allEntityNames = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entitynameforlistchart").split(",");


            for (String entityName : allEntityNames) {
             Integer listID=   Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"listidtotestlistchart",entityName));

                allTestData.add(new Object[]{entityName,listID});
            }

            return allTestData.toArray(new Object[0][]);
        }


    public ArrayList<List<String>> getChartColumns(Integer listId,CustomAssert customAssert) {
        Integer clientId=new AdminHelper().getClientId();

        ArrayList<List<String>> chartColumns = new ArrayList<>();

        try {
            ListingChartAxis listingChart = new ListingChartAxis();
            String chartResponse = listingChart.hitListingChart(listId);

            JSONArray jsonarr = new JSONArray(chartResponse);


            for (int i = 0; i < jsonarr.length(); i++) {
                List<String> tempData = new ArrayList<>();

                JSONObject job = jsonarr.getJSONObject(i);

                String xaxisDisplay = job.getString("xaxisDisplay");
                String xaxislevel = job.getString("xaxislevel");
                tempData.add(xaxisDisplay);
                tempData.add(xaxislevel);

                chartColumns.add(tempData);


            }


            // validate database and API response
            List<List<String>> dbChartsData = ListChartsDbHelper.getAllChartColumns(clientId, listId, userLanguageId);

            if (!dbChartsData.isEmpty()) {

                //Validate total number or records in database and API response
                customAssert.assertTrue(dbChartsData.size() == chartColumns.size(), "Total column count in database and API matches");

                for (List<String> data : dbChartsData) {

                    if (!chartColumns.contains(data)) {
                        customAssert.assertTrue(false, "Mismatch in Database Xaxis records and API response");
                    }

                }


            }


        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting chart columns");

        }

        return chartColumns;


    }

    @Test(dataProvider = "dataProviderForListingChart",priority = -1)
    public void testChartListData(String entityName, Integer listId) {

        CustomAssert customAssert = new CustomAssert();
        int chartType = 1;
      // String entityName="obligations";


        try {

            int entityTypeId= ConfigureConstantFields.getEntityIdByName(entityName);
            ArrayList<List<String>> chartColumns = getChartColumns(listId,customAssert);
            HashMap<String, String> filterMapFromListAPI = createFilterMapFromListAPI(listId, customAssert);


            for (int i = 0; i < chartColumns.size(); i++) {


                String xaxis = chartColumns.get(i).get(1);

                if (xaxis.equals("currency")) {
                    logger.info("Skipping currency column as Filters cannot be applied on currency");
                } else {
                   String payload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"}}";

                    ListingChartData listingChartData = new ListingChartData();
                    String chartListResponse = listingChartData.hitListChartData(listId, xaxis, chartType, payload);

                    HashMap<String, String> filterMapFromChartAPI = createFilterMapFromChartAPI(chartListResponse, listId, chartType, customAssert);

                    for (String key : filterMapFromListAPI.keySet()) {

                        if ((filterMapFromListAPI.get(key).equals("0")) && filterMapFromChartAPI.containsKey(key)) {
                            customAssert.assertTrue(false, "Listing response has zero value for Filter and option  " + key + " but chart Response contains total Count as zero");
                        }


                        else if ((filterMapFromListAPI.get(key).equals(filterMapFromChartAPI.get(key)))) {
                            customAssert.assertTrue( false,"Chart Response value " +filterMapFromChartAPI.get(key)+"  for Filter and options " +key+  "  does not match the count in List API response ");
                            System.out.println();
                        }

                    }

                }
            }


        } catch (Exception e) {

            customAssert.assertTrue(false, "Exception while hitting listdata API");

        }


    }

    public HashMap<String, String> createFilterMapFromChartAPI(String chartListResponse, int listId, int chartType,CustomAssert customAssert) {
        HashMap<String, String> filterMapFromChartAPI = new HashMap<>();
         String filterId;


        try {

            if (!JSONUtility.validjson(chartListResponse)) {
                customAssert.assertTrue(false, "Chart list response is an invalid json for listid " + listId + " charttype " + chartType);
            } else {
                System.out.println();

                JSONObject jsonobj = new JSONObject(chartListResponse);
                JSONArray responseArr = jsonobj.getJSONArray("response");

                for (int j = 0; j < responseArr.length(); j++) {

                    JSONObject individualCount = responseArr.getJSONObject(j);


                    String link = individualCount.get("link").toString();
                    String label = individualCount.get("label").toString();
                    String value = individualCount.get("value").toString();
                    if (link == null) {
                        //customAssert
                        continue;
                    }
                    if (label == null) {
                        continue;
                    }

                    if (value == null) {
                        continue;
                    }
                    String[] linkData = link.split("JavaScript: showListAndDrill");
                    String linkDataPayload = linkData[1];
                    linkDataPayload = linkDataPayload.substring(1, linkDataPayload.length() - 1);

                    JSONObject linkDataPayloadJson = new JSONObject(linkDataPayload);
                    filterId = linkDataPayloadJson.get("filterId").toString();
                    String optionId = linkDataPayloadJson.get("optionId").toString();
                    List<String> optionIdsInResponse = new ArrayList<>();
                    optionIdsInResponse.add(optionId);

                    String optionName = linkDataPayloadJson.get("optionName").toString();
                    String key = filterId + "+" + optionId;
                    filterMapFromChartAPI.put(key, value);

                    System.out.println();

                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while creating FilterMap from chart API");
        }


        return filterMapFromChartAPI;

    }

    HashMap<String, String> createFilterMapFromListAPI( int listId,CustomAssert customAssert) {
        HashMap<String, String> filterMapFromListAPI = new HashMap<>();

        String filterId="";

        try {


            //Validating filterdata API
            ListRendererFilterData filterobject = new ListRendererFilterData();
            filterobject.hitListRendererFilterData(listId);
            String getFilterResponse = filterobject.getListRendererFilterDataJsonStr();
            System.out.println();
            if (!JSONUtility.validjson(getFilterResponse)) {
                customAssert.assertTrue(false, "Filter Data response is invalid for  listid " + listId);
            } else {
                JSONObject jobjfilterResponseJson = new JSONObject(getFilterResponse);

                Iterator<String> keysFromFilterIds = jobjfilterResponseJson.keys();


                while (keysFromFilterIds.hasNext()) {
                    filterId = keysFromFilterIds.next();
                    String filterName = jobjfilterResponseJson.getJSONObject(filterId).get("filterName").toString();
                    if (filterId.equals("343")) {
                        System.out.println();
                    }

                    JSONObject option = new JSONObject();
                    try {
                        option = jobjfilterResponseJson.getJSONObject(filterId).getJSONObject("multiselectValues").getJSONObject("OPTIONS");
                    } catch (Exception e) {
                        continue;
                    }
                    String autoComplete = "false";
                    try {
                        autoComplete = option.get("autoComplete").toString();

                    } catch (Exception e) {
                        continue;
                    }


                    if (autoComplete.equals("false")) {
                        JSONArray totalOptions = new JSONArray();
                        try {
                            totalOptions = option.getJSONArray("DATA");
                        } catch (Exception e) {
                            try {
                                totalOptions = option.getJSONArray("data");
                            } catch (Exception e1) {
                                continue;

                            }

                        }
                        for (int k = 0; k < totalOptions.length(); k++) {


                            JSONObject individualOptions = totalOptions.getJSONObject(k);
                            String optionIdFromFilterData = individualOptions.get("id").toString();
                            String optionNameFromFilterData = individualOptions.get("name").toString();


                            String listingPayload = "{\"filterMap\":{\"entityTypeId\":12,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                                    "\"filterJson\":{\"" + filterId + " \":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionIdFromFilterData + "\",\"name\":\"" + optionNameFromFilterData + "\"}]}," +
                                    "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"" +
                                    "selectedColumns\":[{\"columnId\":276,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":56,\"columnQueryName\":\"id\"}]}";
                            ListRendererListData listRenderListData = new ListRendererListData();
                            listRenderListData.hitListRendererListDataV2(listId, listingPayload);
                            String listingResponse = listRenderListData.getListDataJsonStr();

                            if (!JSONUtility.validjson(listingResponse)) {
                                System.out.println("Not a valid Json");

                            } else {

                                JSONObject listingResponseJson = new JSONObject(listingResponse);

                                String filteredCountListRender = listingResponseJson.get("filteredCount").toString();
                                filterMapFromListAPI.put(filterId + "+" + optionIdFromFilterData, filteredCountListRender);
                            }
                        }
                    }
                }

            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Creating FilterMap from List API" +filterId);
        }
        return filterMapFromListAPI;

    }


}