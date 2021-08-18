package com.sirionlabs.test.internationalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirionlabs.api.dashboard.DashboardCharts;
import com.sirionlabs.api.dashboard.DashboardLocalFilters;
import com.sirionlabs.api.dashboard.DashboardRecords;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PayloadUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class FieldLabelDashboard extends TestDisputeInternationalization {
    private final static Logger logger = LoggerFactory.getLogger(FieldLabelDashboard.class);

    public void testDashboardGlobalFilters() {
        CustomAssert csAssert = new CustomAssert();


        for (Integer chartId:getAllDashboardIds()){
            try {
                logger.info("********** Validating Global Filters for Chart Id {} ************", chartId);

                String payloadForRecords = PayloadUtils.getPayloadForDashboardRecords(chartId);
                logger.info("Hitting Dashboard Records Api (headers only) for Chart Id: {}", chartId);
                DashboardRecords recordsObj = new DashboardRecords();
                recordsObj.hitDashboardRecords(true, payloadForRecords);
                String recordsJsonStr = recordsObj.getDashboardRecordsJsonStr();

                JSONObject jsonObj = new JSONObject(recordsJsonStr);
                Map<String,Object> filterMap = jsonObj.getJSONObject("chartListHeader").toMap();
                String filters[] = Arrays.stream(new Collection[]{filterMap.values()}).map(Object::toString).toArray(String[]::new);

                //FilterMetadata Response - Dashboard
                logger.info("Getting All Attribute Names for Chart Id: {}", chartId);
                String dashboardLocalFiltersResponseStr;
                String dashboardLocalFiltersPayload = "{\"chartId\":" + chartId + "}";
                DashboardLocalFilters dashboardLocalFiltersObj = new DashboardLocalFilters();
                dashboardLocalFiltersObj.hitDashboardLocalFilters(dashboardLocalFiltersPayload);
                String dashboardLocalFiltersRes = dashboardLocalFiltersObj.getDashboardLocalFiltersJsonStr();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> dashboardMap = objectMapper.readValue(dashboardLocalFiltersRes,Map.class);
                Map<String, Object> attributeMap = (Map<String, Object>) dashboardMap.get("attributes");
                Set<String> columnAtrributes = attributeMap.keySet();

                for ( String columnName : columnAtrributes ) {
                    if (columnName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + columnName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing columns of "+chartId +" Dashboard");
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + columnName + "] does not contain: [" + expectedPostFix + "] under listing columns of "+chartId +" Dashboard");
                    }
                }
                for ( String filterName : filters) {
                    if (filterName.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + filterName.toLowerCase() + "] contain: [" + expectedPostFix.toLowerCase() + "] under listing filters of "+chartId+" Dashboard");
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + filterName.toLowerCase() + "] does not contain: [" + expectedPostFix.toLowerCase() + "] under listing filters of "+chartId+" Dashboard");
                    }
                }

            } catch (Exception e) {
                csAssert.assertTrue(false, "TestDashboard Global filter Exception\n" + e.getStackTrace());
                logger.error("Exception occurred while testing global filters on dashboard for chartId {}", chartId);
            }
        }

    }

    public List<Integer> getAllDashboardIds() {
        logger.info("Hitting Dashboard Charts Api for All DashboardCharts");
        List<Integer> allChartsId = new ArrayList<>();
        DashboardCharts chartObj = new DashboardCharts();
        chartObj.hitDashboardCharts();
        String chartResponse = chartObj.getDashboardChartsJsonStr();
        logger.debug("chart response {}", chartResponse);
        allChartsId = chartObj.getAllChartIdsExcludingManualCharts(chartResponse);

        List<Integer> allTestData = new ArrayList<>();
        for (int i = 0; i < allChartsId.size(); i++) {
            allTestData.add(allChartsId.get(i));
        }
        return allTestData;
    }
}
