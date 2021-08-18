package com.sirionlabs.api.searchLayout;

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
public class SearchLayoutEntityTypes extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(SearchLayoutEntityTypes.class);

	public static List<Map<String, String>> getAttachmentEntityTypes(String jsonStr) {
		return setTypes(jsonStr, "attachmentSearchEntityTypes");
	}

	public static List<Map<String, String>> getMetadataEntityTypes(String jsonStr) {
		return setTypes(jsonStr, "searchEntityTypes");
	}

	private static List<Map<String, String>> setTypes(String jsonStr, String jsonArrKey) {
		List<Map<String, String>> searchEntityTypes = new ArrayList<>();
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONArray(jsonArrKey);

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);
				Map<String, String> entityTypesMap = new HashMap<>();
				entityTypesMap.put("id", Integer.toString(jsonObj.getInt("id")));
				entityTypesMap.put("name", jsonObj.getString("name"));
				searchEntityTypes.add(entityTypesMap);
			}
		} catch (Exception e) {
			logger.error("Exception while setting Types for JsonArr Key {}. {}", jsonArrKey, e.getStackTrace());
		}
		return searchEntityTypes;
	}

	public static int getAttachmentEntityTypeIdFromEntityName(String jsonStr, String name) {
		return getEntityTypeIdFromEntityName(jsonStr, name, "attachmentSearchEntityTypes");
	}

	public static int getMetadataEntityTypeIdFromEntityName(String jsonStr, String name) {
		return getEntityTypeIdFromEntityName(jsonStr, name, "searchEntityTypes");
	}

	private static int getEntityTypeIdFromEntityName(String jsonStr, String name, String jsonArrKey) {
		int entityId = -1;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONArray jsonArr = jsonObj.getJSONArray(jsonArrKey);
			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = jsonArr.getJSONObject(i);
				if (jsonObj.getString("name").trim().equalsIgnoreCase(name)) {
					entityId = jsonObj.getInt("id");
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Entity Type Id of {} in SearchLayoutEntityType. {}", name, e.getMessage());
		}
		return entityId;
	}

	public String hitSearchLayoutEntityTypes() {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/searchLayout/entityTypes";
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.getRequest(getRequest, false);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Search Entity Types response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Search Entity Types Api. {}", e.getMessage());
		}
		return response;
	}
}
