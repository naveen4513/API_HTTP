package com.sirionlabs.test.listing;

import com.sirionlabs.api.listRenderer.FetchSavedListingCharts;
import com.sirionlabs.api.listRenderer.ListingChartAxis;
import com.sirionlabs.api.listRenderer.ListingSaveChartData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.ListChartsDbHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestListingChartSave {
    private final static Logger logger = LoggerFactory.getLogger(TestListingChartSave.class);
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingChartDataFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingChartDataFileName");
    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForListingChartSave() {
        logger.info("Fetching all the list ids to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        String[] allListIDsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listidtotestsavechart").split(",");
        for (String listID : allListIDsToTest) {
            allTestData.add(new Object[]{listID});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForListingChartSave")
    public void Test_ChartSaveAPI(String listId) {

        CustomAssert customAssert = new CustomAssert();

        try {
            Integer clientId = new AdminHelper().getClientId();
            Integer userId = new AccountInfo().getUserId();
            List<String> getChartsDataFromPayload = new ArrayList<>();
            List<List<String>> chartColumns = getChartColumns(Integer.parseInt(listId), customAssert);
            ListingSaveChartData listingSaveChartData = new ListingSaveChartData();
            List<String> columnsOnChart = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "columnstotest").split(","));


            for (int index = 0; index < chartColumns.size(); index++) {
                String xAxisDisplay = chartColumns.get(index).get(0);
                String xAxisLevel = chartColumns.get(index).get(1);
                if (xAxisDisplay == null) {
                    logger.debug("Skipping due to null value in xAxisDisplay");
                    continue;
                } else if (columnsOnChart.contains(xAxisDisplay)) {

                    Integer totalDBCountBeforeAPIHit = Integer.parseInt(ListChartsDbHelper.getTotalSavedData(Integer.parseInt(listId), clientId, userId).get(0).get(0));
                    String chartNameValue = DateUtils.getCurrentTimeStamp();
                    chartNameValue = chartNameValue.replaceAll("_", "");
                    chartNameValue = chartNameValue.replaceAll(" ", "");
                    chartNameValue = "Automation" + chartNameValue;

                    String payloadToSave = "{\"chartData\":{\"chartName\":\"" + chartNameValue + "\",\"chartType\":\"scrollColumn2D\",\"xaxisLevel\":[\"" + xAxisLevel + "\"],\"yaxisLevel\":[\"count\"],\"xaxisDisplay\":\"" + xAxisDisplay + "\",\"yaxisDisplay\":\"Count\"}}";
                    String response = listingSaveChartData.hitListChartData(Integer.parseInt(listId), payloadToSave);
                    if (!JSONUtility.validjson(response)) {
                        logger.error("Listing Save Chart Data API response is not a valid JSON");
                        customAssert.assertTrue(false, "Listing Save Chart Data API response is not a valid JSON");
                    } else {
                        JSONObject responseJSON = new JSONObject(response);
                        String successStatus = responseJSON.get("success").toString();
                        if (successStatus == null) {
                            logger.error("Success Status is null");
                            customAssert.assertTrue(false, "Success Status is null");
                        } else if (successStatus.equalsIgnoreCase("true")) {

                            Integer expectedTotalCount = totalDBCountBeforeAPIHit + 1;
                            Integer actualTotalCount = Integer.parseInt(ListChartsDbHelper.getTotalSavedData(Integer.parseInt(listId), clientId, userId).get(0).get(0));
                            Integer actualLatestId = Integer.parseInt(ListChartsDbHelper.getListingChartSavedData(Integer.parseInt(listId), clientId).get(0).get(0));

                            getChartsDataFromPayload.add(chartNameValue);
                            getChartsDataFromPayload.add(xAxisDisplay);
                            getChartsDataFromPayload.add(xAxisLevel);
                            getChartsDataFromPayload.add(Integer.toString(actualLatestId));
                            getChartsDataFromPayload.add(Integer.toString(actualTotalCount));


                            List<String> dataFromResponse = fetchSavedData(Integer.parseInt(listId), customAssert);

                            if (!getChartsDataFromPayload.containsAll(dataFromResponse)) {
                                customAssert.assertTrue(false, "Chart Data in response mismatches the Chart Data in Payload/Database  ");
                            } else if (expectedTotalCount.equals(actualTotalCount)) {

                                logger.info("Chart data is saved for column : {}", xAxisDisplay);
                                customAssert.assertTrue(true, "Chart data is saved for column : " + xAxisDisplay);
                                ListChartsDbHelper.deleteLatestId(actualLatestId);
                                logger.info("Chart data is deleted for Latest ID ", actualLatestId);


                            } else {
                                customAssert.assertTrue(false, "Total count and latestID mismatches in databases");
                            }


                        } else {
                            logger.error("Chart data is not saved for column : {}", xAxisDisplay);
                            customAssert.assertTrue(false, "Chart data is not saved for column : " + xAxisDisplay);
                        }


                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Validating the scenario {}", e.getMessage());
            customAssert.assertTrue(false, "Exception while Validating the scenario");
        }
        customAssert.assertAll();
    }

    public List<List<String>> getChartColumns(int listId, CustomAssert customAssert) {
        List<List<String>> chartColumns = new ArrayList<>();
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
        } catch (Exception e) {
            logger.error("Exception while getting chart values");
            customAssert.assertTrue(false, "Exception while getting chart values");
        }
        return chartColumns;


    }


    public List<String> fetchSavedData(int listId, CustomAssert customAssert) {
        List<String> getchartsDataFromResponse = new ArrayList<>();
        try {


            FetchSavedListingCharts fetchcharts = new FetchSavedListingCharts();
            String fetchchartsresponse = fetchcharts.hitFetchSavedCharts(listId);
            if (!ParseJsonResponse.validJsonResponse(fetchchartsresponse)) {
                logger.error("Fetch Saved Dashboard API response is not a valid JSON");
                customAssert.assertTrue(false, "Fetch Saved Dashboard API response is not a valid JSON");
            } else {

                JSONArray jsonarr = new JSONArray(fetchchartsresponse);
                int length = jsonarr.length();
                String totalCountInJson = Integer.toString(length);

                JSONObject job = jsonarr.getJSONObject(0);

                String chartData = job.get("chartData").toString();
                JSONObject chartDataObj = new JSONObject(chartData);
                String actualChartName = chartDataObj.getJSONObject("chartData").get("chartName").toString().trim();
                String xaxisLevelfromAPI = chartDataObj.getJSONObject("chartData").get("xaxisLevel").toString().replace("[", "").replace("]", "").replace("\"", " ").trim();
                String xaxisDisplayfromAPI = chartDataObj.getJSONObject("chartData").get("xaxisDisplay").toString().trim();
                String latestIDInJson = job.get("id").toString();
                getchartsDataFromResponse.add(actualChartName);
                getchartsDataFromResponse.add(xaxisDisplayfromAPI);
                getchartsDataFromResponse.add(xaxisLevelfromAPI);
                getchartsDataFromResponse.add(latestIDInJson);
                getchartsDataFromResponse.add(totalCountInJson);

            }
        } catch (Exception e) {
            logger.error("Exception while getting Saved Data");
            customAssert.assertTrue(false, "Exception while getting Saved data");
        }
        return getchartsDataFromResponse;


    }

}