package com.sirionlabs.api.searchFilter;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
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

public class SearchFilterData extends APIUtils {
    private final static Logger logger = LoggerFactory.getLogger(SearchFilterData.class);
    private String searchFilterDataJsonStr = null;

    public static List<Map<String, String>> getAllFilterMetadatas(String jsonStr) {
        List<Map<String, String>> allFilterMetadataMaps = new ArrayList<Map<String, String>>();
        logger.info("Setting All FilterMetadatas.");
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArr = jsonObj.getJSONArray("filterMetadatas");
            for (int i = 0; i < jsonArr.length(); i++) {
                jsonObj = jsonArr.getJSONObject(i);

                if (jsonObj.getString("displayFormat").equalsIgnoreCase("{}")) {
                    continue;
                }

                Map<String, String> filterMetadataMap = new HashMap<String, String>();
                filterMetadataMap.put("id", Integer.toString(jsonObj.getInt("id")));
                filterMetadataMap.put("name", jsonObj.getString("name"));
                filterMetadataMap.put("queryName", jsonObj.getString("queryName"));
                filterMetadataMap.put("uiType", jsonObj.getString("uiType"));
                allFilterMetadataMaps.add(filterMetadataMap);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All FilterMetadatas. {}", e.getMessage());
        }
        return allFilterMetadataMaps;
    }

    public static List<Map<String, String>> getAllFilterDataOptionsFromFilterId(String jsonStr, int filterId) {
        List<Map<String, String>> allOptionsData = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("filterDataMap");
            for (String key : JSONObject.getNames(jsonObj)) {
                JSONObject filterDataObj = jsonObj.getJSONObject(key);
                if (filterDataObj.getInt("filterId") == filterId) {
                    filterDataObj = filterDataObj.getJSONObject("multiselectValues").getJSONObject("OPTIONS");
                    JSONArray optionsArr;
                    if (filterDataObj.has("data"))
                        optionsArr = filterDataObj.getJSONArray("data");
                    else
                        optionsArr = filterDataObj.getJSONArray("DATA");
                    for (int j = 0; j < optionsArr.length(); j++) {
                        Map<String, String> optionsDataMap = new HashMap<>();
                        optionsDataMap.put("id", Integer.toString(optionsArr.getJSONObject(j).getInt("id")));
                        optionsDataMap.put("name", optionsArr.getJSONObject(j).getString("name"));
                        allOptionsData.add(optionsDataMap);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All FilterData Options from Filter Id: {}. {}", filterId, e.getStackTrace());
        }
        return allOptionsData;
    }

    public static boolean isFilterAutoComplete(String jsonStr, int filterId) {
        boolean isAutoComplete = false;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            jsonObj = jsonObj.getJSONObject("filterDataMap");
            for (String key : JSONObject.getNames(jsonObj)) {
                JSONObject filterDataObj = jsonObj.getJSONObject(key);
                if (filterDataObj.getInt("filterId") == filterId) {
                    filterDataObj = filterDataObj.getJSONObject("multiselectValues").getJSONObject("OPTIONS");
                    if (filterDataObj.has("autoComplete"))
                        isAutoComplete = filterDataObj.getBoolean("autoComplete");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking if Filter having Id {} is AutoComplete or not. {}", filterId, e.getMessage());
        }
        return isAutoComplete;
    }

    public HttpResponse hitSearchFilterData() throws Exception {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/searchfilter/data";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = super.getRequest(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.searchFilterDataJsonStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("SearchFilterData response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Calendar Filter Data Api. {}", e.getMessage());
        }
        return response;
    }

    public String getSearchFilterDataJsonStr() {
        return searchFilterDataJsonStr;
    }

}
