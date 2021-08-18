package com.sirionlabs.api.listRenderer;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

import static org.jsoup.helper.StringUtil.isNumeric;

public class ListRendererListData extends APIUtils {
	final static String valueDelimiter = ":;";
	private final static Logger logger = LoggerFactory.getLogger(ListRendererListData.class);
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

	public HttpResponse hitListRendererTabListData(int listId, int tablistId, int id, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/tablistdata/" + tablistId + "/" + id;

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRenderer Tab List Data Api. {}", e.getMessage());
		}
		return response;

	}

	public String listDataResponse(int entityTypeId, String entitySection) {

		int defaultNumberOfRecrodsToGet = 20;
		return listDataResponse(entityTypeId, entitySection, defaultNumberOfRecrodsToGet);
	}

	public String listDataResponse(int entityTypeId, String entitySection, int maxRecordsToGet) {
		String listDataJsonStr = "";
		try {
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

			String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + maxRecordsToGet + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";


			logger.info("Hitting ListRendererListData");

			hitListRendererListData(urlId, true, listDataPayload, null);

			listDataJsonStr = getListDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception While Fetching url Id for entityName : [{}] , Exception : [{}]", entitySection, e.getLocalizedMessage());
		}
		return listDataJsonStr;
	}

	public HttpResponse hitListRendererListData(int listId, boolean firstCall) {
		return hitListRendererListData(listId, firstCall, "{\"filterMap\":{}}", null);
	}

	public HttpResponse hitListRendererListData(int listId, String payload) {
		return hitListRendererListData(listId, false, payload, null);
	}

	public HttpResponse hitListRendererListData(int listId) {
		return hitListRendererListData(listId, true, "{\"filterMap\":{}}", null);
	}

	public HttpResponse hitListRendererListData(int entityTypeId, int offset, int size, String orderByColumnName, String orderDirection, int listId) throws Exception {

		String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
				"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";

		logger.debug("Hitting ListRendererListData");
		logger.debug("entityTypeId : {}", entityTypeId);
		logger.debug("offset : {}", offset);
		logger.debug("size : {}", size);
		logger.debug("with orderByColumnName : {}", orderByColumnName);
		logger.debug("and orderDirection : {}", orderDirection);
		logger.debug("listId : {}", listId);

		return hitListRendererListData(listId, false, listDataPayload, null);
	}

	public HttpResponse hitListRendererListData(int listId, String payload, Map<String, String> params) {
		return hitListRendererListData(listId, false, payload, params);
	}

	public HttpResponse hitListRendererListData(int listId, boolean firstCall, String payload, Map<String, String> params) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?isFirstCall=" + firstCall;
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

	public String hitListRendererListDataV2(int listId, String payload, Map<String, String> params) {
		String response = null;

		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0";

			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "&" + urlParams;
			}

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			apiStatusCode = String.valueOf(httpResponse.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRendererListData Api. {}", e.getMessage());
		}
		return response;
	}

	public String hitListRendererInsight(int listId,int computationId, String payload) {
		String response = null;

		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?contractId=&relationId=&vendorId=&am=true&insightComputationId=" + computationId + "&version=2.0";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			postRequest.addHeader("x-requested-with", "XMLHttpRequest");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

		} catch (Exception e) {
			logger.error("Exception while hitting ListRendererListData Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitListRendererListDataV2isFirstCall(int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0&isFirstCall=true";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

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

	public HttpResponse hitListRendererListDataV2amTrue(int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?am=true&version=2.0";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

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

	public List<Map<Integer, Map<String, String>>> getListData() {
		return this.listData;
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
						if (columnJson.getStringJsonValue("value").contains("::")) {
							String []arr=columnJson.getStringJsonValue("value").split("::");
							String value="";
							for (int ar=0; ar<arr.length; ar++) {
								if (arr[ar].contains(":;")){
									   value=value+arr[ar].split(":;")[0]+", ";
								}
							}
							columnDataMap.put("value", value.substring(0,value.length()-2));
						}
						else {
							String values[] = columnJson.getStringJsonValue("value").split(valueDelimiter);
							columnDataMap.put("value", values[0]);
							if (values.length > 1)
								columnDataMap.put("valueId", values[1]);
						}
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
			List<Integer> recordKeySet = new ArrayList<>(oneRecord.keySet());

			for (Integer recordKey : recordKeySet) {
				Map<String, String> column = oneRecord.get(recordKey);

				if (column.get("columnName").equalsIgnoreCase(columnName.trim())) {
					id = Integer.parseInt(column.get("id"));
					break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Column Id of {} in ListRendererListData. {}", columnName, e.getMessage());
		}
		return id;
	}


	// this function will return the total count of records in Entity List Page
	public int getFilteredCount() {

		int noOfRecords = -1;
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		if (listDataResponseObj.has("filteredCount"))
			noOfRecords = listDataResponseObj.getInt("filteredCount");


		return noOfRecords;

	}

	public List<Integer> getAllRecordDbId(int columnId, String listDataJsonStr) {
		List<Integer> dbId = new ArrayList<Integer>();
		try {
			JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
			int noOfRecords = listDataResponseObj.getJSONArray("data").length();

			for (int i = 0; i < noOfRecords; i++) {
				JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
				String uidAndDbid = recordObj.getJSONObject(Integer.toString(columnId)).getString("value");
				String[] splitDbId = uidAndDbid.split(":;");
				dbId.add(Integer.parseInt(splitDbId[1]));
			}
		} catch (Exception e) {
			logger.error("Exception While Fetching All Records Db Id [{}]", e.getLocalizedMessage());
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
				String record = (String) recordObj.getJSONObject(Integer.toString(columnID)).get("value");
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

	public List<String> getAllRecordForParticularColumns(int columnID, String listDataJsonStr) {
		List<String> allRecords = new ArrayList<>();
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);
			if (recordObj.getJSONObject(Integer.toString(columnID)).get("value").equals(JSONObject.NULL)) {
				allRecords.add(null);
			} else {
				String record = (String) recordObj.getJSONObject(Integer.toString(columnID)).get("value");
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

	// this function will verify whether records is sorted properly based of sortingType (ex : asc or desc)
	public boolean isRecordsSortedProperly(List<String> allRecords, String type, String columnName, String sortingType, PostgreSQLJDBC jDBCUtils) throws SQLException {


		logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
		boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )

		if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
		{
			strictAscOrDescCheck = true;

		}

		String[] allRecordsArray = allRecords.toArray(new String[allRecords.size()]);
		int[] checkForConsecutivityOfNull = new int[allRecordsArray.length]; // since it's either null first or null last type to sorting this will return the index of comparable record
		int index = 0;
		int firstIndexToTest = 0;
		int lastIndexToTest = allRecordsArray.length;
		boolean allRecordsNotNull = false;

		for (String record : allRecordsArray) {
			if (record == null)
				checkForConsecutivityOfNull[index++] = 0;
			else {
				checkForConsecutivityOfNull[index++] = 1;
				allRecordsNotNull = true;
			}
		}

		if (!allRecordsNotNull) {

			logger.info("All Records Are Null Nothing to Verify ");
			return true;
		}

		if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
			for (int i = 0; i < allRecordsArray.length; i++) {
				if (checkForConsecutivityOfNull[i] == 1) {
					firstIndexToTest = i;
					lastIndexToTest = allRecords.size();
					break;
				} else
					continue;
			}

			for (int j = firstIndexToTest; j < allRecordsArray.length; j++) {
				if (checkForConsecutivityOfNull[j] == 0) // again null should not occur since it's asc nulls first
				{
					logger.info("Error:Since It's Asc nulls first type of sorting So null value should occur at beginning only");
					return false;
				}

			}

		}

		if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

			for (int i = 0; i < allRecordsArray.length; i++) {
				if (checkForConsecutivityOfNull[i] == 0) {
					firstIndexToTest = 0;
					lastIndexToTest = i;
					break;
				} else
					continue;
			}


			for (int j = lastIndexToTest; j < allRecordsArray.length; j++) {
				if (checkForConsecutivityOfNull[j] == 1) // again null should not occur since it's  desc nulls first
				{
					logger.info("Error:Since It's Desc nulls last type of sorting So null value should occur at last only");
					return false;
				}

			}

		}

		List<String> allRecordsExceptNullValues = allRecords.subList(firstIndexToTest, lastIndexToTest);

		//removed system admin as it is not sortable in audit log
		if(columnName.toLowerCase().equals("completed by") || columnName.toLowerCase().equals("requested by")){
			allRecordsExceptNullValues.remove("System Admin");
		}


		if (type.contentEquals("TEXT"))  //if Records Type is Text
		{
//			PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();

			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

				boolean sorted = true;
				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {
					if (!jDBCUtils.compareTwoRecordsForAscOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in asc null first where " + allRecordsExceptNullValues.get(j - 1) + " should come after " + allRecordsExceptNullValues.get(j));
						sorted = false;
						//Added by gaurav bhadani as this gives wrong result on checking 2 strings eg. '105' and '98'
						try {
							if (isNumeric(allRecordsExceptNullValues.get(j - 1)) && isNumeric(allRecordsExceptNullValues.get(j))) {
								if (!jDBCUtils.compareTwoRecordsForAscOrEqualNumeric(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
									sorted = false;
								} else {
									sorted = true;
								}
							}
						} catch (Exception e) {
							logger.error("Exception while comparing records");
						}
					}

				}

				return sorted;
			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				boolean sorted = true;
				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {
					if (!jDBCUtils.compareTwoRecordsForDescOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in desc null first where " + allRecordsExceptNullValues.get(j) + " should come before " + allRecordsExceptNullValues.get(j - 1));
						sorted = false;

						//Added by gaurav bhadani as this gives wrong result on checking 2 strings eg. '105' and '98'
						try {
							if (isNumeric(allRecordsExceptNullValues.get(j - 1)) && isNumeric(allRecordsExceptNullValues.get(j))) {
								if (!jDBCUtils.compareTwoRecordsForAscOrEqualNumeric(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
									sorted = true;
								} else {
									sorted = false;
								}
							}
						} catch (Exception e) {
							logger.error("Exception while comparing records");
						}
					}

				}
//				jDBCUtils.closeConnection();
				return sorted;
			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
//			jDBCUtils.closeConnection();
			return true;

		}

		if (type.contentEquals("NUMBER")) //if Records Type is number
		{

			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

				boolean sorted = true;
				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {

					try {
						Float previousValue = Float.parseFloat(allRecordsExceptNullValues.get(j - 1));
						Float nextValue = Float.parseFloat(allRecordsExceptNullValues.get(j));
						if (strictAscOrDescCheck) {
							if (nextValue <= previousValue) {
								logger.info("Verifying Sorted Results for Column name : {} ", columnName);
								logger.error("Not Sorting Properly in asc null first where " + nextValue + " should come after " + previousValue);
								sorted = false;
								break;
							}
						} else {
							if (nextValue < previousValue) {
								logger.info("Verifying Sorted Results for Column name : {} ", columnName);
								logger.error("Not Sorting Properly in asc null first where " + nextValue + " should come after " + previousValue);
								sorted = false;
								break;
							}
						}
					} catch (Exception e) {
						logger.error("Fatal Error : Exception occured while converting the number formate data in Float : {}", e.getMessage());
						return false;
					}


				}

				return sorted;

			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {


				boolean sorted = true;
				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {

					try {
						Float previousValue = Float.parseFloat(allRecordsExceptNullValues.get(j - 1));
						Float nextValue = Float.parseFloat(allRecordsExceptNullValues.get(j));

						if (strictAscOrDescCheck) {
							if (nextValue >= previousValue) {
								logger.info("Verifying Sorted Results for Column name : {} ", columnName);
								logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
								sorted = false;
								break;
							}
						} else {
							if (nextValue > previousValue) {
								logger.info("Verifying Sorted Results for Column name : {} ", columnName);
								logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
								sorted = false;
								break;
							}
						}
					} catch (Exception e) {
						logger.error("Fatal Error : Exception occured while converting the number formate data in Float : {}", e.getMessage());
						return false;
					}
				}

				return sorted;

			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
			return true;

		}

		if (type.contentEquals("DATE")) //todo laters since there is no standard formate
		{
			return true; // hack as of now

//			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {
//
//
//			}
//			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
//
//
//			}
//
//			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
//			return true;

		}


		logger.info("Column DataType of Records is not matching with any of the above-mentioned condition");
		return true;

	}


	// this function will verify whether last value of prev page is less than or equal to the first Value of Current Page in case of asc sorting or vice versa
	public boolean isPaginationCorrect(String lastRecordOfPrevPage, String firstRecordOfCurrentPage, String columnName, String type, String sortingType, PostgreSQLJDBC jDBCUtils) throws SQLException {


		logger.debug("Verifying whether lastRecord of Prev Page is less than of equal to the first Value of Current Page in case of asc sorting or vice versa ");
		logger.debug("lastRecordOfPrevPage :{}", lastRecordOfPrevPage);
		logger.debug("firstRecordOfCurrentPage :{}", firstRecordOfCurrentPage);

		boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )

		if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
		{
			strictAscOrDescCheck = true;

		}

		if (lastRecordOfPrevPage == null || firstRecordOfCurrentPage == null) {
			logger.info("either of lastRecordOfPrevPage or firstRecordOfCurrentPage is null , can't compare");
			return true;
		}


		if (type.contentEquals("TEXT"))  //if Records Type is Text
		{

//			PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();
			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
				boolean sorted = true;
				if (!jDBCUtils.compareTwoRecordsForAscOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
					sorted = false;
				}

				return sorted;
			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				boolean sorted = true;
				if (!jDBCUtils.compareTwoRecordsForDescOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
					sorted = false;
				}

				return sorted;
			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
			return true;

		}

		if (type.contentEquals("NUMBER")) //if Records Type is number
		{

			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

				boolean sorted = true;

				Float previousValue = Float.parseFloat(lastRecordOfPrevPage);
				Float nextValue = Float.parseFloat(firstRecordOfCurrentPage);
				if (strictAscOrDescCheck) {
					if (nextValue <= previousValue) {
						sorted = false;

					}
				} else {
					if (nextValue < previousValue) {
						sorted = false;

					}
				}


				return sorted;

			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {


				boolean sorted = true;

				Float previousValue = Float.parseFloat(lastRecordOfPrevPage);
				Float nextValue = Float.parseFloat(firstRecordOfCurrentPage);
				if (strictAscOrDescCheck) {
					if (nextValue >= previousValue) {
						sorted = false;

					}
				} else {
					if (nextValue > previousValue) {
						sorted = false;

					}
				}


				return sorted;

			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
			return true;

		}

		if (type.contentEquals("DATE")) //todo laters since there is no standard formate
		{
			return true; // hack as of now

//			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {
//
//
//			}
//			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
//
//
//			}
//
//			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
//			return true;

		}


		logger.info("Column DataType of Records is not matching with any of the above-mentioned condition");
		return true;

	}


	public Map<Integer, String> getListColumnIdNameMap(String ListRenderListListJson) {
		Map<Integer, String> ListColumnIdNameMap = new HashMap<Integer, String>();
		Integer ColumnId = null;
		String ColumnName = null;
		try {
			JSONArray ListJsonArray = new JSONObject(ListRenderListListJson).getJSONArray("data");
			for (int i = 0; i < ListJsonArray.length(); i++) {
				JSONObject jsonObj = ListJsonArray.getJSONObject(i);
				String[] columnIds = JSONObject.getNames(jsonObj);
				for (String columnId : columnIds) {
					ColumnId = jsonObj.getJSONObject(columnId).getInt("columnId");
					ColumnName = jsonObj.getJSONObject(columnId).getString("columnName");
					ListColumnIdNameMap.put(ColumnId, ColumnName);
				}
				break;

			}
		} catch (Exception e) {
			logger.error("Exception while getting List Column Id-Name Map. {}", e.getMessage());
		}
		return ListColumnIdNameMap;
	}

	//Added by gaurav bhadani on 13 july 2018 for hitting liste renderer filter data
	public HttpResponse hitListRendererFilterData(int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/filterData";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer Filter response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRenderer Filter Data Api. {}", e.getMessage());
		}
		return response;
	}

	//Added by gaurav bhadani on 26 july 2018 for hitting liste renderer filter data
	public HttpResponse hitListRendererFilterDataComputation(int listId, int computationid) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
//			String queryString = "/listRenderer/list/" + listId + "/filterData";
			String queryString = "/listRenderer/list/" + listId + "/filterData?relationId=&contractId=&am=true&insightComputationId=" + computationid;

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, "{}");
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer Filter response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRenderer Filter Data Api. {}", e.getMessage());
		}
		return response;
	}

	//Added by gaurav bhadani on 1 august 2018 as part of downloading list data
	//Method takes additional filterjson as a parameter and gives results based on the filter json
	public HttpResponse hitListRendererListData(int entityTypeId, int offset, int size, String orderByColumnName, String orderDirection, int listId, String filterjson) throws Exception {

		String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
				"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterjson + "}}}";

		logger.debug("Hitting ListRendererListData");
		logger.debug("entityTypeId : {}", entityTypeId);
		logger.debug("offset : {}", offset);
		logger.debug("size : {}", size);
		logger.debug("with orderByColumnName : {}", orderByColumnName);
		logger.debug("and orderDirection : {}", orderDirection);
		logger.debug("listId : {}", listId);

		return hitListRendererListData(listId, false, listDataPayload, null);
	}

	public HttpResponse hitListRendererListDataV2(int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
//			String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0";
			String queryString = "/listRenderer/list/" + listId + "/listdata?am=true&version=2.0";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

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

	public String hitListRendV2(int listId, String payload) {
		HttpResponse response = null;
		String listDataJsonStr = "";
		try {
			HttpPost postRequest;
//			String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0";
			String queryString = "/listRenderer/list/" + listId + "/listdata?am=true&version=2.0";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);



		} catch (Exception e) {
			logger.error("Exception while hitting ListRendererListData Api. {}", e.getMessage());
		}
		return listDataJsonStr;
	}

	public String listDataResponseV2(int entityTypeId, String entitySection, int maxRecordsToGet, Map<String, String> params) {
		try {
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

			String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + maxRecordsToGet + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";

			logger.info("Hitting ListRendererListData");

			return hitListRendererListDataV2(urlId, listDataPayload, params);
		} catch (Exception e) {
			logger.error("Exception While Fetching url Id for entityName : [{}] , Exception : [{}]", entitySection, e.getLocalizedMessage());
		}
		return null;
	}

	public String listDataResponseV2OrderDirection(int entityTypeId, String entitySection, int maxRecordsToGet, String orderDirection, Map<String, String> params) {
		try {
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

			String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + maxRecordsToGet + ",\"orderByColumnName\":\"id\",\"orderDirection\":\""+orderDirection+"\",\"filterJson\":{}}}";

			logger.info("Hitting ListRendererListData");

			return hitListRendererListDataV2(urlId, listDataPayload, params);
		} catch (Exception e) {
			logger.error("Exception While Fetching url Id for entityName : [{}] , Exception : [{}]", entitySection, e.getLocalizedMessage());
		}
		return null;
	}

	public HttpResponse hitListRendererSlaSpecificGraph(int listId, int slID, int calendarType) {

		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/listRenderer/list/" + listId + "/slaSpecificGraph/" + slID + "?calendarType=" + calendarType;

			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRenderer slaSpecificGraph Api. {}", e.getMessage());
		}
		return response;

	}

	public HttpResponse hitListRendererListData(int listId, String parameterString, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata" + parameterString;

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ListRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting ListRenderer Tab List Data Api. {}", e.getMessage());
		}
		return response;

	}

	public HttpResponse hitListRendererListDataV2WithOutParams(String listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

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

	public HashMap<String,String> getColValOfPartRecord(String listDataResponse,String recordId){

		HashMap<String,String> columnValOfPartRecord = new HashMap<>();
		try{
			Boolean recordFound = false;
			String columnName;
			String columnValue;
			JSONObject listDataRespJson = new JSONObject(listDataResponse);

			JSONArray dataArray = listDataRespJson.getJSONArray("data");

			outerLoop:
			for(int i =0;i<dataArray.length();i++){

				JSONObject indRowJson = dataArray.getJSONObject(i);

				Iterator<String> keys = dataArray.getJSONObject(i).keys();
				columnValOfPartRecord = new HashMap<>();
				while (keys.hasNext()){

					String key = keys.next();

					columnName = indRowJson.getJSONObject(key).get("columnName").toString();
					columnValue = indRowJson.getJSONObject(key).get("value").toString();
					columnValOfPartRecord.put(columnName,columnValue);
					if(columnName.equals("id")){
						String idColumn = columnValue.split(":;")[1];

						if(idColumn.equals(recordId)){
							recordFound = true;

							columnValOfPartRecord.put(columnName,columnValue);
						}

					}
				}

				if(recordFound == true){
					break outerLoop;
				}
				columnValOfPartRecord.clear();
			}

		}catch (Exception e){
			logger.error("Exception while getting column values of a particular record");
		}
		return columnValOfPartRecord;
	}

	public int getRandRecordIdFromListing(String entityName,String listResponse,CustomAssert customAssert){

		int recordId = -1;
		if(JSONUtility.validjson(listDataJsonStr)){

			JSONObject listResponseJson = new JSONObject(listResponse);
			JSONObject firstRowJson = listResponseJson.getJSONArray("data").getJSONObject(0);

			Iterator<String> keys = firstRowJson.keys();
			String columnName;
			String key;
			while (keys.hasNext()){
				key = keys.next();
				columnName = firstRowJson.getJSONObject(key).get("columnName").toString();

				if(columnName.equals("id")){
					recordId = Integer.parseInt(firstRowJson.getJSONObject(key).get("value").toString().split(":;")[1]);
					break;
				}
			}

		}else {
			customAssert.assertTrue(false,"List Response is not a valid json for entity " + entityName);
		}
		return recordId;
	}

	public Map<String, String> getListColumnNameValueMap(String listRenderListListJson) {
		Map<String, String> listColumnIdNameMap = new HashMap<String, String>();

		String columnName = null;
		String columnValue = null;
		try {
			JSONArray ListJsonArray = new JSONObject(listRenderListListJson).getJSONArray("data");
			for (int i = 0; i < ListJsonArray.length(); i++) {
				JSONObject jsonObj = ListJsonArray.getJSONObject(i);
				String[] columnIds = JSONObject.getNames(jsonObj);
				for (String columnId : columnIds) {
					columnName = jsonObj.getJSONObject(columnId).getString("columnName");
					columnValue = jsonObj.getJSONObject(columnId).get("value").toString();

					listColumnIdNameMap.put(columnName, columnValue);
				}
				break;

			}
		} catch (Exception e) {
			logger.error("Exception while getting List Column Name Value Map. {}", e.getMessage());
		}
		return listColumnIdNameMap;
	}

	public Map<Integer,Map<String, String>> getListColumnIdValueMap(String listRenderListListJson) {

		Map<Integer,Map<String, String>> listColIdValueMap = new HashMap<>();
		Map<String, String> listColumnIdNameMap = new HashMap<>();

		String colId = null;
		String columnValue = null;
		try {
			JSONArray ListJsonArray = new JSONObject(listRenderListListJson).getJSONArray("data");
			for (int i = 0; i < ListJsonArray.length(); i++) {
				JSONObject jsonObj = ListJsonArray.getJSONObject(i);
				String[] columnIds = JSONObject.getNames(jsonObj);
				listColumnIdNameMap = new HashMap<>();
				for (String columnId : columnIds) {
					colId = jsonObj.getJSONObject(columnId).get("columnId").toString();
					columnValue = jsonObj.getJSONObject(columnId).get("value").toString();

					listColumnIdNameMap.put(colId, columnValue);
				}
				listColIdValueMap.put(i,listColumnIdNameMap);
			}
		} catch (Exception e) {
			logger.error("Exception while getting List Column Id Value Map. {}", e.getMessage());
		}
		return listColIdValueMap;
	}
	/*
	* This method will try to return listing records max 1
	* based on Numeric type Custom Field Filter which is supposed
	* */
	public Map<String,String> getListRespToChkPartRecIsPresent(int entityTypeId,String filterId,String filterName,
															   String min,String max,
															   String payload,
															   CustomAssert customAssert) {

		Map<String, String> listColumnIdNameMap = new HashMap<String, String>();

		try {
			Thread.sleep(1000);
			String listPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId +
					",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId + "\"," +
					"\"filterName\":\"" + filterName + "\",\"entityFieldId\":" + filterName + "," +
					"\"entityFieldHtmlType\":18,\"min\":\"" + min + "\",\"max\":\"" + max + "\",\"suffix\":null}}},\"selectedColumns\":[" +
					payload
					+ "]}";

			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			int listId = ConfigureConstantFields.getListIdForEntity(entityName);

			String listResponse = hitListRendV2(listId, listPayload);

			if (JSONUtility.validjson(listResponse)) {

				JSONObject listDataJson = new JSONObject(listResponse);
				JSONArray dataArray = listDataJson.getJSONArray("data");
				if (dataArray.length() == 0) {

				} else if (dataArray.length() > 1) {
					customAssert.assertTrue(false, "More than one record returned when applied filter id " + filterId + " for entity " + entityName + " Expected one record should return in listing response");
				} else {
					listColumnIdNameMap = getListColumnNameValueMap(listResponse);
				}
			} else {
				customAssert.assertTrue(false, "List Response is not a valid json");
			}

		} catch (Exception e) {
			logger.error("Exception while getting Listing Response To Check if Particular Record Is Present on Listing");
		}
		return listColumnIdNameMap;
	}

	public Map<String,String> getListRespToChkPartRecIsPresent(int entityTypeId,String filterId,String filterName,
																int filterIdValue, String payload,CustomAssert customAssert){

		Map<String, String> listColumnIdNameMap = new HashMap<>();

		try{

			String listPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId +
					",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":" +
					"{\"SELECTEDDATA\":[{\"id\":\"" + filterIdValue + "\",\"name\":\"\"}]}," +
					"\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\"," +
					"\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[" + payload + "]}";

			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			int listId = ConfigureConstantFields.getListIdForEntity(entityName);

			String listResponse = hitListRendV2(listId,listPayload);

			if(JSONUtility.validjson(listResponse)){

				JSONObject listDataJson = new JSONObject(listResponse);
				JSONArray dataArray = listDataJson.getJSONArray("data");
				if(dataArray.length() == 0){

				} else {
					listColumnIdNameMap = getListColumnNameValueMap(listResponse);
				}
			}else {
				customAssert.assertTrue(false,"List Response is not a valid json");
			}

		}catch (Exception e){
			logger.error("Exception while getting Listing Response To Check if Particular Record Is Present on Listing");
		}
		return listColumnIdNameMap;
	}

	public String createPayloadForColStr(String payload,String ignoredColumns) {

		String colPayload = payload;
		try {

			if (ignoredColumns == null) {
				return payload;
			} else {

				List<String> ignoredColumnsList = Arrays.asList(ignoredColumns.split(","));

				JSONArray payloadJsonArray = new JSONArray(payload);

				for (int i = 0; i < payloadJsonArray.length(); i++) {

					for (int j = 0; j < ignoredColumnsList.size(); j++) {

						if (payloadJsonArray.getJSONObject(i).toString().contains(ignoredColumnsList.get(j))) {
							payloadJsonArray.remove(i);
						}
					}


				}
				//Replacing curly brackets
				colPayload = colPayload.replaceAll("\\[", "");
				colPayload = colPayload.replaceAll("\\]","");

			}
		} catch (Exception e) {
			logger.error("Exception while creating payload from Columns String");
		}
		return colPayload;

	}

	public String chkPartRecIsPresentForDiffUser(int entityTypeId,String filterId,String filterName,
															   int filterIdValue, String payload,CustomAssert customAssert){

		Map<String, String> listColumnIdNameMap = new HashMap<>();

		String recordPresent = "Yes";
		Check check = new Check();

		try{

			String userName = ConfigureEnvironment.getEnvironmentProperty("second_username");
			String password = ConfigureEnvironment.getEnvironmentProperty("second_username_password");

			check.hitCheck(userName,password);

			String listPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId +
					",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":" +
					"{\"SELECTEDDATA\":[{\"id\":\"" + filterIdValue + "\",\"name\":\"\"}]}," +
					"\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\"," +
					"\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[" + payload + "]}";

			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			int listId = ConfigureConstantFields.getListIdForEntity(entityName);

			String listResponse = hitListRendV2(listId,listPayload);

			if(JSONUtility.validjson(listResponse)){

				JSONObject listDataJson = new JSONObject(listResponse);
				JSONArray dataArray = listDataJson.getJSONArray("data");
				if(dataArray.length() == 0){

				} else {
					listColumnIdNameMap = getListColumnNameValueMap(listResponse);
				}
			}else {
				customAssert.assertTrue(false,"List Response is not a valid json");
			}

		}catch (Exception e){
			logger.error("Exception while getting Listing Response To Check if Particular Record Is Present on Listing");
		}finally {
			check.hitCheck();
		}

		if(listColumnIdNameMap.size() == 0){
			recordPresent = "No";
		}

		return recordPresent;
	}

	public String chkPartRecIsPresentForDiffUser(int entityTypeId,String filterId,String filterName,
															   String min,String max,
															   String payload,
															   CustomAssert customAssert){

		Map<String, String> listColumnIdNameMap = new HashMap<>();
		Check check = new Check();
		String recordPresent = "Yes";

		try{

			String userName = ConfigureEnvironment.getEnvironmentProperty("second_username");
			String password = ConfigureEnvironment.getEnvironmentProperty("second_username_password");

			if(userName == null){
				return recordPresent;
			}
			check.hitCheck(userName,password);

			String listPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId +
					",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\""+ filterId + "\":{\"filterId\":\"" + filterId + "\"," +
					"\"filterName\":\"" + filterName + "\",\"entityFieldId\":" + filterName + "," +
					"\"entityFieldHtmlType\":18,\"min\":\"" + min + "\",\"max\":\"" + max + "\",\"suffix\":null}}},\"selectedColumns\":[" +
					payload
					+ "]}";

			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			int listId = ConfigureConstantFields.getListIdForEntity(entityName);

			String listResponse = hitListRendV2(listId,listPayload);

			if(JSONUtility.validjson(listResponse)){

				JSONObject listDataJson = new JSONObject(listResponse);
				JSONArray dataArray = listDataJson.getJSONArray("data");
				if(dataArray.length() == 0){

				} else {
					listColumnIdNameMap = getListColumnNameValueMap(listResponse);
				}
			}else {
				customAssert.assertTrue(false,"List Response is not a valid json");
			}

		}catch (Exception e){
			logger.error("Exception while getting Listing Response To Check if Particular Record Is Present on Listing");
		}finally {
			check.hitCheck();

		}

		if(listColumnIdNameMap.size() == 0){
			recordPresent = "No";
		}
		return recordPresent;
	}

	public Boolean checkRecFoundOnListPage(int listId,int idColId,int expEntityId,String generalPayload,CustomAssert customAssert){

		Boolean recFoundOnListDefPayload = false;
		try{

			hitListRendererListDataV2(listId,generalPayload);

			String listResponse= getListDataJsonStr();

			if(JSONUtility.validjson(listResponse)){
				JSONArray dataArray = new JSONObject(listResponse).getJSONArray("data");
				outerLoop:
				for(int i =0;i<dataArray.length();i++){
					JSONObject singleRow = dataArray.getJSONObject(i);
					Iterator<String> keys = singleRow.keys();

					while (keys.hasNext()){
						String key = keys.next();
						if(key.equals(String.valueOf(idColId))){
							String actualContractId = singleRow.getJSONObject(key).get("value").toString().split(":;")[1];

							if(actualContractId.equalsIgnoreCase(String.valueOf(expEntityId))){
								recFoundOnListDefPayload = true;
								break outerLoop;
							}
						}
					}
				}

				if(recFoundOnListDefPayload){
					logger.info("Record found on listing page with default payload");
				}else {
					logger.error("Record not found on listing page with default payload");
					customAssert.assertTrue(false,"Record not found on listing page with default payload");
				}
			}else {
				customAssert.assertTrue(false,"List Response is not valid json");
				recFoundOnListDefPayload = false;
			}
		}catch (Exception e){
			recFoundOnListDefPayload = false;
		}
		return recFoundOnListDefPayload;
	}

}
