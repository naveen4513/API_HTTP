package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Options extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Options.class);
	private String ids;
	private String optionsJsonStr = null;

	public static String getNameFromId(String jsonStr, int id) {
		String name = null;
		JSONObject jsonObj = new JSONObject(jsonStr);
		JSONArray jsonArr = jsonObj.getJSONArray("data");
		for (int i = 0; i < jsonArr.length(); i++) {
			jsonObj = jsonArr.getJSONObject(i);
			if (jsonObj.has("id") && jsonObj.getInt("id") == id) {
				name = jsonObj.getString("name");
				break;
			}
		}
		return name;
	}

	public HttpResponse hitOptions(int dropDownType, Map<String, String> params) {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/options/" + dropDownType;
			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "?" + urlParams;
			}
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = APIUtils.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.optionsJsonStr = EntityUtils.toString(response.getEntity());
			this.setIds(optionsJsonStr);

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Options response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			ids = null;
			logger.error("Exception while hitting Options Api. {}", e.getMessage());
		}
		return response;
	}

	public String getIds() {
		return this.ids;
	}

	private void setIds(String jsonStr) {
		ids = null;
		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);
			JSONArray jsonArray = json.getArrayJsonValue("data");

			if (jsonArray.length() != 0) {
				boolean first = true;

				for (int i = 0; i < jsonArray.length(); i++) {
					jsonObj = new JSONObject(jsonArray.get(i).toString());
					json = new JSONUtility(jsonObj);

					if (first) {
						this.ids = Integer.toString(json.getIntegerJsonValue("id"));
						first = false;
					} else
						this.ids = ids.concat("," + Integer.toString(json.getIntegerJsonValue("id")));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while setting Ids in Options. {}", e.getMessage());
		}
	}

	public String getOptionsJsonStr() {
		return this.optionsJsonStr;
	}
}
