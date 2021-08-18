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

public class TabDefaultUserListMetaData extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(TabDefaultUserListMetaData.class);
	String tabDefaultUserListMetaDataJsonStr = null;
	List<Map<String, String>> columns = new ArrayList<>();
	List<Map<String, String>> allColumnQueryName = new ArrayList<>();
	String apiStatusCode = null;
	String apiResponseTime = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitTabDefaultUserListMetadata(int tabId) {
		return hitTabDefaultUserListMetadata(tabId, null, "{}");
	}

	public HttpResponse hitTabDefaultUserListMetadata(int tabId, Map<String, String> params) {
		return hitTabDefaultUserListMetadata(tabId, params, "{}");
	}

	public HttpResponse hitTabDefaultUserListMetadata(int tabId, Map<String, String> params, String payload) {
		HttpResponse response = null;
		try {
			String queryString = "/listRenderer/list/" + tabId + "/defaultUserListMetaData";
			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "?" + urlParams;
			}
			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug(response.getStatusLine().toString());
			this.tabDefaultUserListMetaDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", tabDefaultUserListMetaDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = response.getStatusLine().toString();
			apiResponseTime = super.getApiResponseTime();
		} catch (Exception e) {
			logger.error("Exception while hitting TabDefaultUserListMetadata API for tabId [{}] , Exception is [{}]", tabId, e.getLocalizedMessage());
		}

		return response;
	}


	public List<Map<String, String>> getColumns() {
		return this.columns;
	}

	public void setColumns(String jsonStr) {
		columns.clear();
		Map<String, String> columnsMap = new HashMap<String, String>();

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);

			if (jsonObj.has("body") && json.getStringJsonValue("body") == null)
				logger.info("ListRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");

			else {
				JSONArray jsonArr = new JSONArray(json.getArrayJsonValue("columns").toString());

				for (int i = 0; i < jsonArr.length(); i++) {
					jsonObj = new JSONObject(jsonArr.get(i).toString());
					json = new JSONUtility(jsonObj);

					columnsMap = new HashMap<String, String>();
					columnsMap.put("id", Integer.toString(json.getIntegerJsonValue("id")));
					columnsMap.put("name", json.getStringJsonValue("name"));
					columnsMap.put("defaultName", json.getStringJsonValue("defaultName"));
					columnsMap.put("queryName", json.getStringJsonValue("queryName"));
					columnsMap.put("type", json.getStringJsonValue("type"));
					columnsMap.put("displayFormat", json.getStringJsonValue("displayFormat"));

					columns.add(columnsMap);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while setting Columns in TabDefaultUserListMetaData. {}", e.getMessage());
		}
	}


	// this will return the list of Map of  <all the query (column name) , is_sortableFlag , columntype, id >
	public List<Map<String, String>> getAllQueryName() {
		setColumns(tabDefaultUserListMetaDataJsonStr);
		try {

			for (Map<String, String> column : columns) {

				Map<String, String> columnsMap = new HashMap<String, String>();
				columnsMap.put("queryName", column.get("queryName"));
				columnsMap.put("type", column.get("type"));
				columnsMap.put("id", column.get("id"));
				columnsMap.put("name", column.get("name"));
				columnsMap.put("displayFormat", column.get("displayFormat"));

				// if displayFormat flag has sortable flag false or prefix flag is true (for currency)
				if (column.get("displayFormat").contains("\"sortable\":false") || column.get("displayFormat").contains("\"prefix\":true")) {
					columnsMap.put("isSortable", "false");
				} else
					columnsMap.put("isSortable", "true");

				logger.debug("column Map is : {}", columnsMap);

				allColumnQueryName.add(columnsMap);

			}
		} catch (Exception e) {
			logger.error("Exception while fetching queryName of in TabDefaultUserListMetaData. {}", e.getMessage());
		}
		return allColumnQueryName;
	}

	public String getTabDefaultUserListMetaDataJsonStr() {
		return this.tabDefaultUserListMetaDataJsonStr;
	}


}





