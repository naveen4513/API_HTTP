package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
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

public class ListRendererFilterData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ListRendererFilterData.class);
	private String listRendererFilterDataJsonStr = null;
	private String apiStatusCode = null;
	private String apiResponseTime = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitListRendererFilterData(int listId) {
		return hitListRendererFilterData(listId, "{}", null);
	}

	public HttpResponse hitListRendererFilterData(int listId, String payload) {
		return hitListRendererFilterData(listId, payload, null);
	}

	public HttpResponse hitListRendererFilterData(int listId, Map<String, String> params) {
		return hitListRendererFilterData(listId, "{}", params);
	}

	public HttpResponse hitListRendererFilterData(int listId, String payload, Map<String, String> params) {
		HttpResponse response = null;

		try {
			String queryString = "/listRenderer/list/" + listId + "/filterData";
			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "?" + urlParams;
			}
			logger.debug("Query string url formed is {}", queryString);
			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.listRendererFilterDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listRendererFilterDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("ListRenderer response header {}", oneHeader.toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = response.getStatusLine().toString();
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRendererFilterData Api. {}", e.getMessage());
		}
		return response;
	}

	public List<Map<String, String>> setFilter(String jsonStr, String filterName) {
		boolean filterFound = false;
		List<Map<String, String>> filter = new ArrayList<>();
		Map<String, String> filterMap;

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);

			for (String objectId : JSONObject.getNames(jsonObj)) {
				JSONObject objectJson = jsonObj.getJSONObject(objectId);

				if (objectJson.getString("filterName").equalsIgnoreCase(filterName)) {
					filterFound = true;
					objectJson = objectJson.getJSONObject("multiselectValues").getJSONObject("OPTIONS");

					String dataName = null;

					if (objectJson.has("data"))
						dataName = "data";

					else if (objectJson.has("DATA"))
						dataName = "DATA";

					if (!objectJson.getString(dataName).trim().equalsIgnoreCase("null")) {
						JSONArray jsonArr = objectJson.getJSONArray(dataName);

						//For Handling cases like Stakeholders
						if (jsonArr.length() > 0) {
							JSONObject tempJsonObj = new JSONObject(jsonArr.get(0).toString());

							if (tempJsonObj.has("group")) {
								String groupValue = tempJsonObj.getString("group");

								if (groupValue.startsWith("[") && groupValue.endsWith("]"))
									jsonArr = tempJsonObj.getJSONArray("group");
							}

							for (int i = 0; i < jsonArr.length(); i++) {
								JSONObject entityJsonObj = new JSONObject(jsonArr.get(i).toString());
								JSONUtility entityJson = new JSONUtility(entityJsonObj);

								filterMap = new HashMap<>();
								filterMap.put("id", entityJson.getStringJsonValue("id"));
								filterMap.put("name", entityJson.getStringJsonValue("name"));

								filter.add(filterMap);
							}
							break;
						} else {
							logger.info("No Data options available for Filter {}", filterName);
							break;
						}
					} else {
						logger.info("No Data options available for Filter {}", filterName);
						break;
					}
				}
			}

			if (!filterFound)
				logger.info("No Such Filter {} Found", filterName);
		} catch (Exception e) {
			logger.error("Exception while setting Filter {} in ListRendererFilterData. {}", filterName, e.getMessage());
		}

		return filter;
	}

	public String getListRendererFilterDataJsonStr() {
		return this.listRendererFilterDataJsonStr;
	}

	public boolean isFilterAutoComplete(String jsonStr, String filterName) {
		boolean autoComplete = false;
		boolean filterFound = false;

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);

			for (String objectId : JSONObject.getNames(jsonObj)) {
				JSONObject objectJson = jsonObj.getJSONObject(objectId);

				if (objectJson.getString("filterName").equalsIgnoreCase(filterName)) {
					filterFound = true;
					objectJson = objectJson.getJSONObject("multiselectValues").getJSONObject("OPTIONS");

					if (objectJson.has("autoComplete")) {
						autoComplete = objectJson.getBoolean("autoComplete");
						break;
					}
				}
			}

			if (!filterFound)
				logger.info("No Such Filter {} found", filterName);
		} catch (Exception e) {
			logger.error("Exception while fetching autoComplete of {} in ListRendererFilterData. {}", filterName, e.getMessage());
		}
		return autoComplete;
	}

	public int getFilterId(String jsonStr, String filterName) {
		int fieldId = -1;
		boolean filterFound = false;

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);

			for (String objectId : JSONObject.getNames(jsonObj)) {
				JSONObject objectJson = jsonObj.getJSONObject(objectId);
				JSONUtility jsonUtil = new JSONUtility(objectJson);

				if (jsonUtil.getStringJsonValue("filterName").equalsIgnoreCase(filterName)) {
					filterFound = true;

					if (objectJson.has("filterId")) {
						fieldId = jsonUtil.getIntegerJsonValue("filterId");
						break;
					}
				}
			}

			if (!filterFound)
				logger.info("No Such Filter {} found", filterName);
		} catch (Exception e) {
			logger.error("Exception while fetching Filter Id of {} in ListRendererFilterData. {}", filterName, e.getMessage());
		}

		return fieldId;
	}
}