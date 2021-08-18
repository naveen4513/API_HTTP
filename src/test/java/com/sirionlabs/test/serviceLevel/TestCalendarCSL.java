package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestCalendarCSL extends TestAPIBase {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestCalendarCSL.class);

    @Test(enabled = true)
    public void TestCalendarCSLData(){

        String calendarA = "false";
        CustomAssert customAssert = new CustomAssert();
        int month = 7;
        int year = 2020;
        String response =  null;
        try{

            CalendarData calendarData = new CalendarData();
            calendarData.hitCalendarData(month, year, calendarA);
            response =  calendarData.getCalendarDataJsonStr();

            JSONObject caleJsonData = new JSONObject(response);
            JSONArray caleData = caleJsonData.getJSONArray("array");
            List<Integer> id = new ArrayList<>();
            String todayDate = DateUtils.getCurrentDateInMM_DD_YYYY();
            String date = null;

            for(int i=0;i<caleData.length();i++){
                date = caleData.getJSONObject(i).getString("start");
                long unix_seconds = Long.parseLong(date);
                Date date1 = new Date(unix_seconds * 1000 );

                SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
                if(date.equalsIgnoreCase(todayDate));
                id.add(caleData.getJSONObject(i).getInt("id"));
                break;
            }

            checkReportingDate(id,customAssert);

        }catch (Exception e){
            customAssert.assertFalse(false,"Calendar CSl has not validated successfully");
        }
    }

    public void checkReportingDate(List<Integer> id,CustomAssert customAssert){
        JSONArray responseJson;
        for(int i=0;i<id.size();i++){
            String apiUrl ="/v2/summarydata/15/"+id+"/";

            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                APIResponse response = executor.get(apiUrl, headers).getResponse();
                String responseBody = response.getResponseBody();

                if (!APIUtils.validJsonResponse(responseBody)) {
                    logger.error("API Response is not a valid Json");
                    customAssert.assertTrue(false, "API Response is not a valid Json");
                }

                responseBody = response.getResponseBody();
                responseJson = new JSONArray(responseBody);

            }catch (Exception e){
                customAssert.assertFalse(false,"Dates has not validated successfully");
            }
        }
    }
}