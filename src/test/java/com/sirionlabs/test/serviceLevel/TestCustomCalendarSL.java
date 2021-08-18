package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.HashMap;

import java.util.Map;

public class TestCustomCalendarSL extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestCustomCalendarSL.class);

    @Test(enabled = true)
    public void TestCustomCalendar(){
        CustomAssert customAssert = new CustomAssert();
        int listId = 6;
        String response;
        int childId = -1;
        String payload = "{\"filterMap\":{\"entityTypeId\":14,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"6\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"5\"}]},\"filterId\":6,\"filterName\":\"status\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"242\":{\"filterId\":242,\"listId\":null,\"filterName\":\"calendarViewType\",\"filterShowName\":null,\"minValue\":null,\"maxValue\":null,\"min\":null,\"max\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1001\",\"name\":\"Gregorian\",\"group\":null,\"type\":null,\"$$hashKey\":\"object:242\"}],\"OPTIONS\":{\"autoComplete\":false,\"DATA\":[{\"id\":\"1002\",\"name\":\"13 Period\",\"group\":null,\"type\":null,\"$$hashKey\":\"object:239\"},{\"id\":\"1003\",\"name\":\"445-Calendar\",\"group\":null,\"type\":null,\"$$hashKey\":\"object:240\"},{\"id\":\"1005\",\"name\":\"544-Calendar\",\"group\":null,\"type\":null,\"$$hashKey\":\"object:241\"},{\"id\":\"1001\",\"name\":\"Gregorian\",\"group\":null,\"type\":null,\"$$hashKey\":\"object:242\"}],\"optionApi\":null}},\"startDate\":null,\"endDate\":null,\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"start\":null,\"end\":null,\"dayOffset\":null,\"monthType\":null,\"duration\":null,\"dayType\":null,\"operator\":null,\"filterDisabled\":false,\"primary\":false,\"uitype\":\"SINGLESELECT\"}}},\"selectedColumns\":[{\"columnId\":277,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":110,\"columnQueryName\":\"id\"},{\"columnId\":111,\"columnQueryName\":\"slaid\"},{\"columnId\":112,\"columnQueryName\":\"name\"},{\"columnId\":113,\"columnQueryName\":\"suppliername\"},{\"columnId\":114,\"columnQueryName\":\"expected\"},{\"columnId\":115,\"columnQueryName\":\"minimum\"},{\"columnId\":-1,\"columnQueryName\":\"Aug-20\"},{\"columnId\":-2,\"columnQueryName\":\"Jul-20\"},{\"columnId\":-3,\"columnQueryName\":\"Jun-20\"}]}";
        try{
            ListRendererListData listRendererListData = new ListRendererListData();

            listRendererListData.hitListRendererListDataV2amTrue(listId, payload);
            response = listRendererListData.getListDataJsonStr();
            JSONObject listDataResponseJSON = new JSONObject(response);

            JSONArray listData = listDataResponseJSON.getJSONArray("data");
            for (int i=0;i<listData.length();i++){
                JSONArray arrayData =  listData.getJSONObject(i).names();
                for(int j=0;j<arrayData.length();j++){
                    if(listData.getJSONObject(i).getJSONObject(arrayData.getString(j)).getString("columnName").equalsIgnoreCase("Jun-20")){
                        Object obj =  listData.getJSONObject(i).getJSONObject(arrayData.getString(j)).get("value");
                        if(obj instanceof String && !obj.equals("NA")){
                            childId = Integer.parseInt(listData.getJSONObject(i).getJSONObject(arrayData.getString(j)).getString("value").split(":;:;:;:;")[1]);
                            checkDueDate(childId,customAssert);
                        }

                    }
                }
            }

        }catch(Exception e){
            logger.error("Gregorian Calendar is not validated successfully");
            customAssert.assertFalse(false,"Gregorian Calendar is not validated successfully");
        }
    }

    public boolean checkDueDate(int childId, CustomAssert customAssert){
        Boolean checkDate = true;
        String api = "/v2/summarydata/15/"+childId+"/show?";
        int checkMonth = 6;
        JSONObject response2;
        try{
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            APIResponse response = executor.get(api, headers).getResponse();
            String responseBody = response.getResponseBody();

            if (!APIUtils.validJsonResponse(responseBody)) {
                logger.error("API Response is not a valid Json");
                customAssert.assertTrue(false, "API Response is not a valid Json");
            }

            responseBody = response.getResponseBody();
            response2 = new JSONObject(responseBody);

            String date = response2.getJSONObject("body").getJSONObject("data").getJSONObject("dueDate").getString("values");
            int month = Integer.parseInt(date.split("-")[0]);
            if (checkMonth!=month){
                checkDate = false;
                logger.error("Dates are not equal");
            }
        }catch(Exception e){
            logger.error("Dates are not validated successfully");
            customAssert.assertFalse(false,"Dates are not validated successfully");
        }
        return checkDate;
    }
}