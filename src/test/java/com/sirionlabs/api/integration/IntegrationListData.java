package com.sirionlabs.api.integration;

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

import java.util.*;

public class IntegrationListData extends APIUtils {
	final static String valueDelimiter = ":;";
	private final static Logger logger = LoggerFactory.getLogger(IntegrationListData.class);
	String listDataJsonStr = null;
	List<Map<Integer, Map<String, String>>> listData = new ArrayList<Map<Integer, Map<String, String>>>();
	String apiStatusCode = null;
	String apiResponseTime = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitIntegrationListData(String listId, String payload) {
		return hitIntegrationListData(listId, payload, null);
	}

	public HttpResponse hitIntegrationListData(String listId, String payload, Map<String, String> params) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/integrationlisting/getListData/" + listId;
			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "&" + urlParams;
			}
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			if (APIUtils.validJsonResponse(listDataJsonStr)) {
				this.setListData(listDataJsonStr);
			}

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRendererListData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getListDataJsonStr() {
		return this.listDataJsonStr;
	}

	public void setListData(String jsonStr) {
		listData.clear();
		Map<Integer, Map<String, String>> columnData;
		Map<String, String> columnDataMap;

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);
			JSONArray listDataArr = new JSONArray(json.getArrayJsonValue("data").toString());

			for (int i = 0; i < listDataArr.length(); i++) {
				columnData = new HashMap<Integer, Map<String, String>>();
				jsonObj = new JSONObject(listDataArr.get(i).toString());

				for (String column : JSONObject.getNames(jsonObj)) {
					columnDataMap = new HashMap<String, String>();
					JSONObject columnJsonObj = jsonObj.getJSONObject(column);
					JSONUtility columnJson = new JSONUtility(columnJsonObj);
					columnDataMap.put("id", Integer.toString(columnJson.getIntegerJsonValue("columnId")));
					String columnName = columnJson.getStringJsonValue("columnName");
					columnDataMap.put("columnName", columnName);
					if (columnName.trim().equalsIgnoreCase("bulkcheckbox")) {
						columnDataMap.put("value", columnJson.getStringJsonValue("value"));
					} else {
						String values[] = columnJson.getStringJsonValue("value").split(valueDelimiter);
						columnDataMap.put("value", values[0]);
						if (values.length > 1)
							columnDataMap.put("valueId", values[1]);
					}
					columnData.put(Integer.parseInt(column), columnDataMap);
				}
				listData.add(columnData);
			}
		} catch (Exception e) {
			logger.error("Exception while setting List Data in ListRendererListData. {}", e.getMessage());
		}
	}

	public int getColumnIdFromColumnName(String columnName) {
		int id = -1;
		try {
			Map<Integer, Map<String, String>> oneRecord = listData.get(0);
			List recordKeySet = new ArrayList(oneRecord.keySet());

			for (int i = 0; i < recordKeySet.size(); i++) {
				Map<String, String> column = oneRecord.get(recordKeySet.get(i));

				if (column.get("columnName").equals(columnName)) {
					id = Integer.parseInt(column.get("id"));
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Column Id of {} in ListRendererListData. {}", columnName, e.getMessage());
		}
		return id;
	}

	public List<Integer> getAllRecordDbId(int columnId, String listDataJsonStr) throws Exception {
		List<Integer> dbId = new ArrayList<Integer>();
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
			String uidAndDbid = recordObj.getJSONObject(Integer.toString(columnId)).getString("value");
			String[] splitDbId = uidAndDbid.split(":;");
			dbId.add(Integer.parseInt(splitDbId[1]));
		}
		return dbId;
	}

	public List<String> getAllRecordForParticularColumns(int columnID) {
		List<String> allRecords = new ArrayList<>();
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
			if (recordObj.getJSONObject(Integer.toString(columnID)).get("value").equals(JSONObject.NULL)) {
				allRecords.add(null);
			} else {
				String record = recordObj.getJSONObject(Integer.toString(columnID)).get("value").toString();
				// if record is not text
				if (record.contains(":;")) {
					String[] splitDbId = record.split(":;");
					allRecords.add(splitDbId[0]);
				} else
					allRecords.add(record);
			}

		}

		logger.debug("All Records is : {}", allRecords);
		return allRecords;

	}

	public Map<String, String> getContractClientPrimaryKeyAndStagingIdMap(String integrationListingResponse) {

		Map<String, String> map = new LinkedHashMap<>();

		Integer clientPrimaryKeyColumnId = getColumnIdFromColumnName("stagingClientPrimaryKey");
		JSONObject listDataResponseObj = new JSONObject(integrationListingResponse);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
			if (!recordObj.getJSONObject(Integer.toString(clientPrimaryKeyColumnId)).get("value").equals(JSONObject.NULL)) {
				String record = recordObj.getJSONObject(Integer.toString(clientPrimaryKeyColumnId)).get("value").toString();
				// if record is not text
				if (record.contains(":;")) {
					String[] splitDbId = record.split(":;");
					map.put(splitDbId[0], splitDbId[1]);
				}
			}
		}
		return map;
	}

}