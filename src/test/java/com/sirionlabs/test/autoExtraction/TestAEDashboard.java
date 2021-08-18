package com.sirionlabs.test.autoExtraction;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestAEDashboard {
    private final static Logger logger = LoggerFactory.getLogger(TestAEDashboard.class);


    @DataProvider
    public Object[][] dataProviderForDashboardCreation() {
        List<Object[]> allDataToTest = new ArrayList<>();

        String chartName="API Automation Chart Bar Type";
        String chartType ="bar2d";
        allDataToTest.add(new Object[]{chartName, chartType});

        chartName="API Automation Chart Line Type";
        chartType="scrollLine2d";
        allDataToTest.add(new Object[]{chartName, chartType});

        chartName="API Automation Chart Pie Type";
        chartType="pie2d";
        allDataToTest.add(new Object[]{chartName, chartType});

        chartName="API Automation Chart Doughnut Type";
        chartType="doughnut2d";
        allDataToTest.add(new Object[]{chartName, chartType});

        logger.info("Total Flows to Test : {}", allDataToTest.size());
        return allDataToTest.toArray(new Object[0][]);
    }

    @Test(dataProvider="dataProviderForDashboardCreation")
    public void createDashboard(String chartName,String chartType) throws IOException {
        CustomAssert customAssert=new CustomAssert();
        try {
            String savedDashboardStr = TestAEDashboard.savedDashboard();
            JSONArray arrList = new JSONArray(savedDashboardStr);
            int totalCount = arrList.length();
            if (totalCount > 0) {
                String chartId = arrList.getJSONObject(0).get("id").toString();
                deleteDashboard(chartId);
                logger.info("one chart data from Saved Dashboard list is deleted successfully i.e " + chartId);
            }
            logger.info("Hitting create chart API");
            HttpResponse createChartResponse = AutoExtractionHelper.createChart();
            customAssert.assertTrue(createChartResponse.getStatusLine().getStatusCode() == 200, "Create chart API response code is invalid");
            logger.info("Starting Test:Start creating dashboard for chart Type "+chartType);
            saveChart(chartName, chartType);
            Thread.sleep(5000);
            String savedDashboardStrNew = savedDashboard();
            JSONArray jsonArr = new JSONArray(savedDashboardStrNew);
            int newCount = jsonArr.length();
            List<String> allDashboards=new ArrayList<>();
            for(int i=0;i<newCount;i++) {
                String chartData = jsonArr.getJSONObject(i).get("chartData").toString();
                JSONObject chartDataObj = new JSONObject(chartData);
                String actualChartName = chartDataObj.getJSONObject("chartData").get("chartName").toString().trim();
                allDashboards.add(actualChartName);
            }
            if (totalCount == 0) {
                customAssert.assertTrue((totalCount + 1) == newCount, "Dashboard is not created successfully.");
                customAssert.assertEquals(allDashboards.get(0), chartName, "Chart is not created for chart Type " + chartType + " and chart Name " + chartName);

            } else {
                customAssert.assertTrue(totalCount == newCount, "Dashboard is not created successfully.");
                customAssert.assertTrue(allDashboards.contains(chartName),"Chart is not created for chart Type " + chartType + " and chart Name " + chartName);
            }
            allDashboards.clear();
        }
        catch (Exception e)
        {
            logger.info("Exception while validating create Chart because of "+e.getMessage());
        }
        customAssert.assertAll();
    }

    public static void deleteDashboard(String chartId) {
        CustomAssert customAssert = new CustomAssert();
        logger.info("Deleting one saved chart from saved chart list");
        HttpResponse deleteChartResponse = AutoExtractionHelper.deleteChart(chartId);
        customAssert.assertTrue(deleteChartResponse.getStatusLine().getStatusCode() == 200, "Delete Chart API Response is invalid");
    }

    public static void saveChart(String chartName,String chartType) throws IOException {
        CustomAssert customAssert = new CustomAssert();
        String payload ="{\"chartData\":{\"chartName\":\""+chartName+"\",\"chartType\":\""+chartType+"\",\"xaxisLevel\":[\"tags\"],\"yaxisLevel\":[\"count\"],\"xaxisDisplay\":\"DOCUMENT TAGS\",\"yaxisDisplay\":\"Count\",\"sqlQuery\":\"fetchDashBoardDataForTags\"}}";
        HttpResponse saveDashboardResponse=AutoExtractionHelper.saveDashboard(payload);
        customAssert.assertTrue(saveDashboardResponse.getStatusLine().getStatusCode()==200,"Save Dashboard AI Response is invalid");
        String saveDashboardResponseStr=EntityUtils.toString(saveDashboardResponse.getEntity());
        JSONObject obj=new JSONObject(saveDashboardResponseStr);
        String value=obj.get("success").toString();
        customAssert.assertEquals(value,"true","Dashboard with name "+chartName+" has not been created successfully");
        logger.info("Chart is created successfully for chart Name "+chartName);
    }

    public static String savedDashboard() throws IOException {
        CustomAssert customAssert = new CustomAssert();
        logger.info("Getting all data saved on Dashboard");
        HttpResponse savedDashboard = AutoExtractionHelper.savedDashboard();
        customAssert.assertTrue(savedDashboard.getStatusLine().getStatusCode() == 200, "Saved Dashboard API Response code is not valid");
        String savedDashboardStr = EntityUtils.toString(savedDashboard.getEntity());
        return savedDashboardStr;
    }
}