package com.sirionlabs.api.metadataSearch;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by akshay.rohilla on 6/20/2017.
 */
public class Search extends APIUtils {

    private final static String valueDelimiter = ":;";
    private final static Logger logger = LoggerFactory.getLogger(Search.class);

    public static List<Map<Integer, Map<String, String>>> getSearchData(String jsonStr) {
        List<Map<Integer, Map<String, String>>> searchData = new ArrayList<>();

        Map<Integer, Map<String, String>> columnData;
        Map<String, String> columnDataMap;
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            if (jsonObj.has("body")) {
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values");
                JSONArray searchDataArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < searchDataArr.length(); i++) {
                    columnData = new HashMap<>();
                    jsonObj = new JSONObject(searchDataArr.get(i).toString());

                    for (String column : JSONObject.getNames(jsonObj)) {
                        columnDataMap = new HashMap<>();
                        JSONObject columnJsonObj = jsonObj.getJSONObject(column);
                        columnDataMap.put("id", Integer.toString(columnJsonObj.getInt("columnId")));
                        columnDataMap.put("columnName", columnJsonObj.getString("columnName"));

                        if (columnJsonObj.has("value") && !columnJsonObj.isNull("value")) {
                            if (!columnJsonObj.get("value").toString().startsWith("[")) {
                                String[] values = columnJsonObj.getString("value").split(valueDelimiter);
                                columnDataMap.put("value", values[0]);

                                if (values.length > 1)
                                    columnDataMap.put("valueId", values[1]);
                            }
                        } else {
                            columnDataMap.put("value", "null");
                        }

                        columnData.put(Integer.parseInt(column), columnDataMap);
                    }
                    searchData.add(columnData);
                }
            } else {
                logger.error("Search Response doesn't have Body Object. Application Error.");
            }
        } catch (Exception e) {
            logger.error("Exception while setting Search Data. {}", e.getMessage());
        }
        return searchData;
    }

    public String hitSearch(int entityId, String payload) {
        try {
            HttpPost postRequest;
            String queryString = "/metadatasearch/search/" + entityId + "?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            HttpResponse response = postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            return EntityUtils.toString(response.getEntity());
        } catch (Exception e) {
            logger.error("Exception while hitting Search MetaData Api. {}", e.getMessage());
        }

        return null;
    }

    public String hitSearch(int entityTypeId) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;

            String queryString = "/metadatasearch/" + entityTypeId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = super.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            return EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            logger.error("Exception while hitting Show Api. {}", e.getMessage());
        }
        return null ;
    }

}
