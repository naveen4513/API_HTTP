package com.sirionlabs.api.bulkaction;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class BulkActionSave extends APIUtils {

	public static Logger logger = LoggerFactory.getLogger(BulkActionSave.class);

	public static String getApiPath() {
		return "/bulkaction/save";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultLegacyHeaders();
	}

	public static String getPayloadForSave(String createJsonStr,String entityIds, int entityTypeId, int listId, String fromStatus, String toStatus) {
		String payloadForSave = null;
		try {
			logger.info("Hitting Bulk Action Create API for EntityTypeId {}", entityTypeId);
			
			String commentStr = "\"comment\": {";

			if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
				JSONObject jsonObj = new JSONObject(createJsonStr);
				jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

				JSONObject commentObj = jsonObj.getJSONObject("comment");
				JSONArray jsonArr = commentObj.names();

				//Remove options from Child Objects
				int i;
				for (i = 0; i < jsonArr.length() - 1; i++) {
					jsonObj = commentObj.getJSONObject(jsonArr.get(i).toString().trim());
					if (jsonObj.has("options"))
						jsonObj.remove("options");
					commentStr = commentStr.concat("\"" + jsonArr.get(i) + "\": " + jsonObj.toString() + ",");
				}

				jsonObj = commentObj.getJSONObject(jsonArr.get(i).toString().trim());
				if (jsonObj.has("options"))
					jsonObj.remove("options");
				commentStr += "\"" + jsonArr.get(i) + "\": " + jsonObj.toString() + "}";

				payloadForSave = "{\"entityIds\": [" + entityIds + "],\"entityTypeId\": " + entityTypeId + ",\"listId\": \"" + listId + "\",\"fromTask\": \"" +
						fromStatus + "\",\"toTask\": \"" + toStatus + "\",\"toBeIgnoredEntityIds\": []," + commentStr + ",\"isGlobalBulk\": true, \"isSelectAll\": false, " +
						"\"filterMap\": {\"currentTask\": \"" + fromStatus + "\",\"nextTaskForBulk\": \"" + toStatus + "\"}}";
			} else {
				logger.error("Bulk Action Create Response is not a valid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Payload for Bulk Action Save. {}", e.getMessage());
		}
		return payloadForSave;
	}
}
