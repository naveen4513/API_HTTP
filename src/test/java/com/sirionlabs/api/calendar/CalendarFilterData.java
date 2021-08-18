package com.sirionlabs.api.calendar;

import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarFilterData extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(CalendarFilterData.class);
    private String calendarFilterDataJsonStr = null;

    public static List<Map<String, String>> getAllFilterMetadata(String jsonStr) {
        List<Map<String, String>> allFilterMetadata = new ArrayList<>();
        try {
            if (ParseJsonResponse.validJsonResponse(jsonStr)) {
                JSONObject jsonObj = new JSONObject(jsonStr);
                JSONArray jsonArr = jsonObj.getJSONArray("filterMetadata");
                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    Map<String, String> filterMetadataMap = new HashMap<>();
                    filterMetadataMap.put("id", Integer.toString(jsonObj.getInt("id")));
                    filterMetadataMap.put("name", jsonObj.getString("name"));
                    filterMetadataMap.put("queryName", jsonObj.getString("queryName"));
                    allFilterMetadata.add(filterMetadataMap);
                }
            } else {
                logger.error("Invalid JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Filter Metadata. {}", e.getMessage());
        }

        return allFilterMetadata;
    }

    public static List<Map<String, String>> getAllFilterOptionsFromQueryName(String jsonStr, String queryName) {
        List<Map<String, String>> allFilterOptions = new ArrayList<>();
        try {
            if (ParseJsonResponse.validJsonResponse(jsonStr)) {
                JSONObject jsonObj = new JSONObject(jsonStr);
                jsonObj = jsonObj.getJSONObject(queryName);
                JSONArray jsonArr;
                if (jsonObj.has("DATA"))
                    jsonArr = jsonObj.getJSONArray("DATA");
                else
                    jsonArr = jsonObj.getJSONArray("data");
                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    Map<String, String> optionsMap = new HashMap<>();
                    optionsMap.put("id", Integer.toString(jsonObj.getInt("id")));
                    optionsMap.put("name", jsonObj.getString("name"));
                    allFilterOptions.add(optionsMap);
                }
            } else {
                logger.error("Invalid JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Options for Query Name {}. {}", queryName, e.getStackTrace());
        }
        return allFilterOptions;
    }

    public static List<Map<String, String>> getAllOptionsOfSupplierAutoCompleteFilter() {
        return getAllOptionsOfAutoCompleteFilter("relations", 1, "");
    }

    private static List<Map<String, String>> getAllOptionsOfAutoCompleteFilter(String filterName, int entityTypeId, String query) {
        List<Map<String, String>> allOptionsOfFilter = new ArrayList<>();

        try {
            OptionsHelper opHelperObj = new OptionsHelper();
            String optionsResponse = opHelperObj.hitOptionsForCalendarFilter(filterName, entityTypeId, query);

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

    public static boolean isFilterAutoComplete(String jsonStr, String filterName, String filterQueryName) {
        boolean autoComplete = false;
        try {
            if (ParseJsonResponse.validJsonResponse(jsonStr)) {
                JSONObject jsonObj = new JSONObject(jsonStr);
                jsonObj = jsonObj.getJSONObject(filterQueryName);
                if (jsonObj.has("autoComplete"))
                    autoComplete = jsonObj.getBoolean("autoComplete");
            } else {
                logger.error("Invalid JSON Response.");
            }
        } catch (Exception e) {
            logger.error("Exception while Checking if Filter {} is AutoComplete or not. {}", filterName, e.getStackTrace());
        }
        return autoComplete;
    }

    public void hitCalendarFilterData() {
        HttpResponse response;
        try {
            HttpGet getRequest;
            String queryString = "/calendar/filterData";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.calendarFilterDataJsonStr = EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting Calendar Filter Data Api. {}", e.getMessage());
        }
    }

    public String getCalendarFilterDataJsonStr() {
        return this.calendarFilterDataJsonStr;
    }
}
