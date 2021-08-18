package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shivashish on 25/7/17.
 */
public class ReportRendererDefaultUserListMetaData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ReportRendererDefaultUserListMetaData.class);
	String reportRendererDefaultUserListMetaDataJsonStr = null;
	List<Map<String, String>> columns = new ArrayList<>();
	List<Map<String, String>> allColumnQueryName = new ArrayList<>();


	public HttpResponse hitReportRendererDefaultUserListMetadata(int listId) throws Exception {
		return hitReportRendererDefaultUserListMetaData(listId, null, "{}");
	}

	public HttpResponse hitReportRendererDefaultUserListMetadata(int listId, Map<String, String> params) throws Exception {
		return hitReportRendererDefaultUserListMetaData(listId, params, "{}");
	}

	public HttpResponse hitReportRendererDefaultUserListMetaData(int reportId, Map<String, String> params, String payload) throws Exception {
		HttpResponse response = null;
		String queryString = "/reportRenderer/list/" + reportId + "/defaultUserListMetaData";
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
		this.reportRendererDefaultUserListMetaDataJsonStr = EntityUtils.toString(response.getEntity());
		logger.debug("response json is: {}", reportRendererDefaultUserListMetaDataJsonStr);

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("ReportRenderer response header {}", headers[i].toString());
		}
		return response;
	}

	public String getReportRendererDefaultUserListMetaDataJsonStr() {
		return this.reportRendererDefaultUserListMetaDataJsonStr;
	}

	public Map<Integer, String> getColumnIdNameMap(String metaDataResponseStr, Boolean isRandomizationOnColumnRequired, Integer maxRandomOptions) {
		Map<Integer, String> allColumnIdNameMap = new HashMap<>();
		Map<Integer, Integer> allColumnIdOrderMap = new HashMap<>();
		Map<Integer, String> requiredColumnIdNameMap = new HashMap<>();

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
				logger.info("Applying Randomization on Columns");
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
			for (Map.Entry<Integer, Integer> entry : allColumnIdOrderMap.entrySet()) {
				if (entry.getValue() <= maxNumberOfColumns) {
					requiredColumnIdNameMap.put(entry.getKey(), allColumnIdNameMap.get(entry.getKey()));
				}
			}
		}

		return requiredColumnIdNameMap;
	}

	// this will return the list of Map of  <all the query (column name) , is_sortableFlag , columntype, id >
	public List<Map<String, String>> getAllQueryName() {
		setColumns(reportRendererDefaultUserListMetaDataJsonStr);
		try {

			for (Map<String, String> column : columns) {

				Map<String, String> columnsMap = new HashMap<String, String>();
				columnsMap.put("queryName", column.get("queryName"));
				columnsMap.put("type", column.get("type"));
				columnsMap.put("id", column.get("id"));
				columnsMap.put("name", column.get("name"));
				columnsMap.put("displayFormat", column.get("displayFormat"));
				columnsMap.put("order", column.get("order"));

				// if displayFormat flag has sortable flag false or prefix flag is true (for currency)
				if (column.get("displayFormat").contains("\"sortable\":false") || column.get("displayFormat").contains("\"prefix\":true")) {
					columnsMap.put("isSortable", "false");
				} else
					columnsMap.put("isSortable", "true");

				logger.debug("column Map is : {}", columnsMap);

				allColumnQueryName.add(columnsMap);

			}
		} catch (Exception e) {
			logger.error("Exception while fetching queryName of in ReportRendererDefaultUserListMetaData. {}", e.getMessage());
		}
		return allColumnQueryName;
	}

	public void setColumns(String jsonStr) {
		columns.clear();
		Map<String, String> columnsMap = new HashMap<String, String>();

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);

			if (jsonObj.has("body") && json.getStringJsonValue("body") == null)
				logger.info("ReportRenderer Default UserList MetaData Response Body is null. Hence couldn't set Columns");

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
					columnsMap.put("order", Integer.toString(json.getIntegerJsonValue("order")));

					columns.add(columnsMap);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while setting Columns in ReportRendererDefaultUserListMetaData. {}", e.getMessage());
		}
	}

	public String getDefaultUserListMetadataResponse(int listId) {


		logger.info("Hitting DefaultUserListMetadata API for Entity {}", listId);
		ListRendererDefaultUserListMetaData metadataObj = new ListRendererDefaultUserListMetaData();
		metadataObj.hitListRendererDefaultUserListMetadata(listId);
		return metadataObj.getListRendererDefaultUserListMetaDataJsonStr();
	}

}
