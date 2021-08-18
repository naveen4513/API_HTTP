package com.sirionlabs.api.metadataSearch;

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

/**
 * Created by akshay.rohilla on 6/20/2017.
 */
public class MetadataSearch extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(MetadataSearch.class);

	public static List<String> getAllFieldLabels(String jsonStr) {
		List<String> allFieldLabels = new ArrayList<>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
			JSONArray jsonArr = jsonObj.getJSONArray("fields");
			allFieldLabels = setAllFieldLabels(allFieldLabels, jsonArr.get(0).toString());
		} catch (Exception e) {
			logger.error("Exception while getting All Field Labels. {}", e.getMessage());
		}
		return allFieldLabels;
	}

	private static List<String> setAllFieldLabels(List<String> allFieldLabels, String jsonStr) {
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);
				if (jsonObj.has("fields"))
					allFieldLabels = setAllFieldLabels(allFieldLabels, jsonArr.get(i).toString());
				else {
					if (jsonObj.has("label")) {
						allFieldLabels.add(jsonObj.getString("label"));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while setting All Field Labels in MetadataSearch. {}", e.getMessage());
		}
		return allFieldLabels;
	}

	public static List<Map<String, String>> getAvailableOptionsForField(String jsonStr, String fieldName, boolean isDynamicField) {
		List<Map<String, String>> availableOptions = new ArrayList<>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
			JSONArray jsonArr;

			if (!isDynamicField)
				jsonArr = jsonObj.getJSONObject(fieldName).getJSONObject("options").getJSONArray("data");

			else
				jsonArr = jsonObj.getJSONObject("dynamicMetadata").getJSONObject(fieldName).getJSONObject("options").getJSONArray("data");

			for (int i = 0; i < jsonArr.length(); i++) {
				Map<String, String> availableOptionMap = new HashMap<>();
				jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
				availableOptionMap.put("id", Integer.toString(jsonObj.getInt("id")));
				availableOptionMap.put("name", jsonObj.getString("name"));
				availableOptions.add(availableOptionMap);
			}
		} catch (Exception e) {
			logger.error("Exception while fetching All Available options for Field {} in MetadataSearch. {}", fieldName, e.getMessage());
		}
		return availableOptions;
	}

	public String hitMetadataSearch(int entityTypeId) {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/metadatasearch/" + entityTypeId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.getRequest(getRequest);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("MetaDataSearch response header {}", oneHeader);
			}
		} catch (Exception e) {
			logger.error("Exception while hitting MetaDataSearch Api. {}", e.getMessage());
		}
		return response;
	}

}
