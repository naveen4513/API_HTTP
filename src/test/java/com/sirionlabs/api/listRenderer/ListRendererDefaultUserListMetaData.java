package com.sirionlabs.api.listRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ListRendererDefaultUserListMetaData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ListRendererDefaultUserListMetaData.class);
	String listRendererDefaultUserListMetaDataJsonStr = null;
	List<Map<String, String>> filterMetadatas = new ArrayList<>();
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

	public HttpResponse hitListRendererDefaultUserListMetadata(int listId) {
		return hitListRendererDefaultUserListMetadata(listId, null, "{}");
	}

	public HttpResponse hitListRendererDefaultUserListMetadata(int listId, Map<String, String> params) {
		return hitListRendererDefaultUserListMetadata(listId, params, "{}");
	}

	public HttpResponse hitListRendererDefaultUserListMetadata(int listId, Map<String, String> params, String payload) {
		HttpResponse response = null;

		try {
			String queryString = "/listRenderer/list/" + listId + "/defaultUserListMetaData";
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
			this.listRendererDefaultUserListMetaDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listRendererDefaultUserListMetaDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = response.getStatusLine().toString();
			apiResponseTime = super.getApiResponseTime();
		} catch (Exception e) {
			logger.error("Exception while Hitting ListRenderer DefaultUserListMetadata API for ListId {}. {}", listId, e.getStackTrace());
		}

		return response;
	}

	public List<Map<String, String>> getFilterMetadatas() {
		return this.filterMetadatas;
	}

	public void setFilterMetadatas(String jsonStr) {
		filterMetadatas.clear();
		Map<String, String> filterMetadatasMap = new HashMap<String, String>();

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);

			if (jsonObj.has("body") && json.getStringJsonValue("body") == null)
				logger.info("ListRenderer Default UserList MetaData Response Body is null. Hence couldn't set FilterMetadata");

			else {
				JSONArray jsonArr = new JSONArray(json.getArrayJsonValue("filterMetadatas").toString());

				for (int i = 0; i < jsonArr.length(); i++) {
					jsonObj = new JSONObject(jsonArr.get(i).toString());
					json = new JSONUtility(jsonObj);

					filterMetadatasMap = new HashMap<String, String>();
					filterMetadatasMap.put("id", Integer.toString(json.getIntegerJsonValue("id")));
					filterMetadatasMap.put("name", json.getStringJsonValue("name"));
					filterMetadatasMap.put("defaultName", json.getStringJsonValue("defaultName"));
					filterMetadatasMap.put("queryName", json.getStringJsonValue("queryName"));
					filterMetadatasMap.put("type", json.getStringJsonValue("type"));

					filterMetadatas.add(filterMetadatasMap);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while setting FilterMetadatas in ListRendererDefaultUserListMetaData. {}", e.getMessage());
		}
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
			logger.error("Exception while setting Columns in ListRendererDefaultUserListMetaData. {}", e.getMessage());
		}
	}

	public int getIdFromQueryName(String queryName) {
		int id = -1;
		try {
			for (Map<String, String> column : columns) {
				if (column.get("queryName").equalsIgnoreCase(queryName)) {
					id = Integer.parseInt(column.get("id"));
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Id of {} in ListRendererDefaultUserListMetaData. {}", queryName, e.getMessage());
		}
		return id;
	}

	// this will return the list of Map of  <all the query (column name) , is_sortableFlag , columntype, id >
	public List<Map<String, String>> getAllQueryName() {
		setColumns(listRendererDefaultUserListMetaDataJsonStr);
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
			logger.error("Exception while fetching queryName of in ListRendererDefaultUserListMetaData. {}", e.getMessage());
		}
		return allColumnQueryName;
	}

	public String getListRendererDefaultUserListMetaDataJsonStr() {
		return this.listRendererDefaultUserListMetaDataJsonStr;
	}

	public String getFieldTypeFromQueryName(String queryName) {
		String fieldType = null;
		try {
			for (Map<String, String> column : columns) {
				if (column.get("queryName").equalsIgnoreCase(queryName)) {
					fieldType = column.get("type");
					break;
				}
			}

			if (fieldType == null) {
				for (Map<String, String> filterMetaData : filterMetadatas) {
					if (filterMetaData.get("queryName").equalsIgnoreCase(queryName)) {
						fieldType = filterMetaData.get("type");
						break;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Field Type of {} in ListRendererDefaultUserListMetaData. {}", queryName, e.getMessage());
		}

		return fieldType;
	}


	/**
	 * this function will generate listColumnID-NameMap of size maxRandomOptions if isRandomizationOnColumnRequired true
	 *
	 * @param metaDataResponseStr
	 * @param isRandomizationOnColumnRequired
	 * @param maxRandomOptions
	 * @return
	 */
	public Map<Integer, String> getListColumnIdNameMap(String metaDataResponseStr, Boolean isRandomizationOnColumnRequired, Integer maxRandomOptions) {
		Map<Integer, String> allColumnIdNameMap = new HashMap<>();
		Map<Integer, Integer> allColumnIdOrderMap = new HashMap<>();
		Map<Integer, String> requiredColumnIdNameMap = new HashMap<>();
		int columnOrderMultiplier = 10;

		JSONObject responseJson = new JSONObject(metaDataResponseStr);
		Integer maxNumberOfColumns = responseJson.getInt("maxNumberOfColumns");
		JSONArray columnArray = responseJson.getJSONArray("columns");

		for (int i = 0; i < columnArray.length(); i++) {
			Integer columnId = columnArray.getJSONObject(i).getInt("id");
			String columnName = columnArray.getJSONObject(i).getString("queryName");
			Integer columnOrder = columnArray.getJSONObject(i).getInt("order");
			allColumnIdNameMap.put(columnId, columnName);
			allColumnIdOrderMap.put(columnId, columnOrder);
		}

		if (isRandomizationOnColumnRequired) {

			List<Map<Integer, String>> columnNameIdList = new ArrayList<>();
			if (allColumnIdNameMap.size() > 0) {
				for (Map.Entry<Integer, String> entry : allColumnIdNameMap.entrySet()) {
					Map<Integer, String> tempMap = new HashMap<>();
					tempMap.put(entry.getKey(), entry.getValue());
					columnNameIdList.add(tempMap);
				}

				List<Integer> randomColumnId = new ArrayList<>();
				List<String> randomColumnName = new ArrayList<>();
				logger.debug("Applying Randomization on Columns");
				if (maxRandomOptions >= allColumnIdNameMap.size())
					maxRandomOptions = allColumnIdNameMap.size() - 1;
				int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allColumnIdNameMap.size() - 1, maxRandomOptions);
				for (int i = 0; i < randomNumbers.length; i++) {

					Map<Integer, String> innerMap = columnNameIdList.get(i);
					for (Map.Entry<Integer, String> entry : innerMap.entrySet()) {
						randomColumnId.add(entry.getKey());
						randomColumnName.add(entry.getValue());
					}
				}

				for (int i = 0; i < randomColumnId.size(); i++) {
					requiredColumnIdNameMap.put(randomColumnId.get(i), randomColumnName.get(i));
				}
			}
		} else {

			int[] columnOrders = new int[allColumnIdOrderMap.size()];
			int i = 0;
			for (Map.Entry<Integer, Integer> entry : allColumnIdOrderMap.entrySet()) {
				columnOrders[i++] = entry.getValue();
			}
			Arrays.sort(columnOrders);
			logger.info("ColumnOrders : [{}]", columnOrders);

			for (Map.Entry<Integer, Integer> entry : allColumnIdOrderMap.entrySet()) {
				if (entry.getValue() < columnOrders[maxNumberOfColumns]) {
					requiredColumnIdNameMap.put(entry.getKey(), allColumnIdNameMap.get(entry.getKey()));
				}
			}


		}

		logger.debug("requiredColumnIdNameMap is : [{}]", requiredColumnIdNameMap);

		return requiredColumnIdNameMap;
	}


}