package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by shivashish on 25/7/17.
 */
public class ReportRendererListData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ReportRendererListData.class);
	String listDataJsonStr = null;
	List<Map<Integer, Map<String, String>>> listData = new ArrayList<Map<Integer, Map<String, String>>>();

	String apiStatusCode = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public HttpResponse hitReportRendererListData(int listId, boolean firstCall) {
		return hitReportRendererListData(listId, firstCall, "{\"filterMap\":{}}", null);
	}

	public HttpResponse hitReportRendererListData(int entityTypeId, int offset, int size, String orderByColumnName, String orderDirection, int reportId) throws Exception {

		String listDataPayload;
		if (orderDirection .equalsIgnoreCase("asc"))
		listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
				"\"orderDirection\":\"asc nulls first\",\"filterJson\":{}}}";
		else
			listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
					offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
					"\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

		logger.debug("Hitting ReportRendererListData");
		logger.debug("entityTypeId : {}", entityTypeId);
		logger.debug("offset : {}", offset);
		logger.debug("size : {}", size);
		logger.debug("with orderByColumnName : {}", orderByColumnName);
		logger.debug("and orderDirection : {}", orderDirection);
		logger.debug("reportId : {}", reportId);

		return hitReportRendererListData(reportId, false, listDataPayload, null);
	}

	public HttpResponse hitReportRendererListData(int listId) {
		return hitReportRendererListData(listId, true, "{\"filterMap\":{}}", null);
	}

	public HttpResponse hitReportRendererListData(int listId, boolean firstCall, String payload, Map<String, String> params) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/list/" + listId + "/listdata?isFirstCall=" + firstCall;
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
			this.listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ReportRenderer response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

		} catch (Exception e) {
			logger.error("Exception while hitting ReportRendererListData Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitReportRendererListData(int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/list/" + listId + "/listdata?version=2.0";
			/*if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "&" + urlParams;
			}*/
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.listDataJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", listDataJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("ReportRenderer response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting ReportRendererListData Api. {}", e.getMessage());
		}
		return response;
	}

	public String getListDataJsonStr() {
		return this.listDataJsonStr;
	}

	// this function will return the total count of records in Entity List Page
	public int getFilteredCount() {

		int noOfRecords = -1;
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		if (listDataResponseObj.has("filteredCount"))
			noOfRecords = listDataResponseObj.getInt("filteredCount");


		return noOfRecords;

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

	// will return the all the Record based on the Column Name
	public List<String> getAllRecordForParticularColumns(String columnName) {
		List<String> allRecords = new ArrayList<>();
		JSONObject listDataResponseObj = new JSONObject(listDataJsonStr);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		for (int i = 0; i < noOfRecords; i++) {
			JSONObject recordObj = listDataResponseObj.getJSONArray("data").getJSONObject(i);

			Set<String> keys = recordObj.keySet();

			for(String key : keys) {

				if (recordObj.getJSONObject(key).get("columnName").toString().contentEquals(columnName)) {
					return getAllRecordForParticularColumns(Integer.parseInt(key));

				}
				else
					continue;
			}

		}
		logger.debug("All Records is : {}", allRecords);
		return allRecords;

	}

	// this function will verify whether records is sorted properly based of sortingType (ex : asc or desc)
	public boolean isRecordsSortedProperly(List<String> allRecords, String type, String columnName, String sortingType,Boolean isTrendReport,PostgreSQLJDBC jDBCUtils) throws SQLException {

		logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
		boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )
		if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
		{
			strictAscOrDescCheck = true;

		}
		if (!columnName.contentEquals("datecreated"))  // Since Duplicate Can't be allowed in this case
		{
			return true;

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

			logger.debug("All Records Are Null Nothing to Verify ");
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

		if (type.contentEquals("TEXT"))  //if Records Type is Text
		{
//			PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();

			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {

				boolean sorted = true;
				boolean sortedByCompareTo;
				boolean sortedByPostgres = true;

				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {

					if (!jDBCUtils.compareTwoRecordsForAscOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in asc null first where " + allRecordsExceptNullValues.get(j - 1) + " should come after " + allRecordsExceptNullValues.get(j));
						sortedByPostgres = false;
					}
					if(allRecordsExceptNullValues.get(j - 1).compareToIgnoreCase(allRecordsExceptNullValues.get(j)) > 0){
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in desc null first where " + allRecordsExceptNullValues.get(j) + " should come before " + allRecordsExceptNullValues.get(j - 1));
						sortedByCompareTo = false;
					}else {
						sortedByCompareTo = true;
					}
					if(sortedByCompareTo == true || sortedByPostgres == true){
						sorted = true;
					}else {
						sorted =false;
					}
                    if(isTrendReport && columnName.equals("MONTH - YEAR")){
                        String prevValue = allRecordsExceptNullValues.get(j - 1);
                        String nextValue = allRecordsExceptNullValues.get(j);

                        if(DateUtils.checkIfDateInAscending(prevValue,nextValue)){
                            sorted = true;
                        }else {
                            sorted = false;
                        }
                    }
				}
				return sorted;
			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				boolean sortedByPostgres = true;
				boolean sortedByCompareTo;
				boolean sorted = true;

				for (int j = 1; j < allRecordsExceptNullValues.size(); j++) {
					if (!jDBCUtils.compareTwoRecordsForDescOrEqual(allRecordsExceptNullValues.get(j - 1), allRecordsExceptNullValues.get(j))) {
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in desc null first where " + allRecordsExceptNullValues.get(j) + " should come before " + allRecordsExceptNullValues.get(j - 1));
						sortedByPostgres = false;
					}
					if(allRecordsExceptNullValues.get(j - 1).compareToIgnoreCase(allRecordsExceptNullValues.get(j)) < 0){
						logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
						logger.warn("Not Sorting Properly in desc null first where " + allRecordsExceptNullValues.get(j) + " should come before " + allRecordsExceptNullValues.get(j - 1));
						sortedByCompareTo = false;
					}else {
						sortedByCompareTo = true;
					}
					if(sortedByCompareTo == true || sortedByPostgres == true){
						sorted = true;
					}else {
						sorted =false;
					}
                    if(isTrendReport && columnName.equals("")){
                        String prevValue = allRecordsExceptNullValues.get(j - 1);
                        String nextValue = allRecordsExceptNullValues.get(j);

                        if(DateUtils.checkIfDateInAscending(prevValue,nextValue)){
                            sorted = false;
                        }else {
                            sorted = true;
                        }
                    }
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
					}
					catch (NumberFormatException nfe){
						String previousValue = allRecordsExceptNullValues.get(j - 1);
						String nextValue = allRecordsExceptNullValues.get(j);
						if(previousValue.compareToIgnoreCase(nextValue) > 0){
							logger.info("Verifying Sorted Results for Column name : {} ", columnName);
							logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
							sorted = false;
							break;
						}
					}
					catch (Exception e) {
						logger.error("Fatal Error : Exception occured while converting the number format data in Float : {}", e.getMessage());
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
					}catch (NumberFormatException nfe){
						String previousValue = allRecordsExceptNullValues.get(j - 1);
						String nextValue = allRecordsExceptNullValues.get(j);
						if(previousValue.compareToIgnoreCase(nextValue) < 0){
							logger.info("Verifying Sorted Results for Column name : {} ", columnName);
							logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
							sorted = false;
							break;
						}
					}
					catch (Exception e) {
						logger.error("Fatal Error : Exception occured while converting the number format data in Float : {}", e.getMessage());
						return false;
					}
				}

				return sorted;

			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
			return true;

		}

		if (type.contentEquals("DATE")) //todo laters since there is no standard format
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


		logger.debug("Column DataType of Records is not matching with any of the above-mentioned condition");
		return true;

	}


	// this function will verify whether last value of prev page is less than or equal to the first Value of Current Page in case of asc sorting or vice versa
	public boolean isPaginationCorrect(String lastRecordOfPrevPage, String firstRecordOfCurrentPage, String columnName, String type, String sortingType,PostgreSQLJDBC jDBCUtils) throws SQLException {


		logger.debug("Verifying whether lastRecord of Prev Page is less than of equal to the first Value of Current Page in case of asc sorting or vice versa ");
		logger.debug("lastRecordOfPrevPage :{}", lastRecordOfPrevPage);
		logger.debug("firstRecordOfCurrentPage :{}", firstRecordOfCurrentPage);

		boolean strictAscOrDescCheck = false;  // for verifying strict sorting for IDs (as of now )

		if (columnName.contentEquals("ID"))  // Since Duplicate Can't be allowed in this case
		{
			strictAscOrDescCheck = true;

		}

		if (lastRecordOfPrevPage == null || firstRecordOfCurrentPage == null) {
			logger.debug("either of lastRecordOfPrevPage or firstRecordOfCurrentPage is null , can't compare");
			return true;
		}


		if (type.contentEquals("TEXT"))  //if Records Type is Text
		{

//			PostgreSQLJDBC jDBCUtils = new PostgreSQLJDBC();
			if (sortingType.toLowerCase().contentEquals("asc nulls first")) {
				boolean sorted = true;
				boolean sortedByPostgres = true;
				boolean sortedByCompareTo = true;
				if (!jDBCUtils.compareTwoRecordsForAscOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
					sortedByPostgres = false;
				}
				if(lastRecordOfPrevPage.compareToIgnoreCase(firstRecordOfCurrentPage) > 0){
					logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
					logger.warn("Not Sorting Properly in asc nulls first where " + lastRecordOfPrevPage + " should come before " + firstRecordOfCurrentPage);
					sortedByCompareTo = false;
				}else {
					sortedByCompareTo = true;
				}
				if(sortedByCompareTo == true || sortedByPostgres == true){
					sorted = true;
				}else {
					sorted =false;
				}

				return sorted;
			}


			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				boolean sorted = true;
				boolean sortedByPostgres =true;
				boolean sortedByCompareTo =true;
				if (!jDBCUtils.compareTwoRecordsForDescOrEqual(lastRecordOfPrevPage, firstRecordOfCurrentPage)) {
					sortedByPostgres = false;
				}
				if(lastRecordOfPrevPage.compareToIgnoreCase(firstRecordOfCurrentPage) < 0){
					logger.debug("Verifying Sorted Results for Column name : {} ", columnName);
					logger.warn("Not Sorting Properly in desc null first where " + lastRecordOfPrevPage + " should come before " + firstRecordOfCurrentPage);
					sortedByCompareTo = false;
				}else {
					sortedByCompareTo = true;
				}
				if(sortedByCompareTo == true || sortedByPostgres == true){
					sorted = true;
				}else {
					sorted =false;
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
				try {
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
				}catch (NumberFormatException nfe){

					String previousValue = lastRecordOfPrevPage;
					String nextValue = firstRecordOfCurrentPage;
					if(previousValue.compareToIgnoreCase(nextValue) < 0){
						logger.info("Verifying Sorted Results for Column name : {} ", columnName);
						logger.error("Not Sorting Properly in asc null first where " + nextValue + " should come before " + previousValue);
						sorted = false;
					}
				}
				catch (Exception e) {
					logger.error("Fatal Error : Exception occured while converting the number format data in Float : {}", e.getMessage());
					return false;
				}
				return sorted;

			}

			if (sortingType.toLowerCase().contentEquals("desc nulls last")) {

				boolean sorted = true;
				try {
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

				}catch (NumberFormatException nfe){
					String previousValue = lastRecordOfPrevPage;
					String nextValue = firstRecordOfCurrentPage;
					if(previousValue.compareToIgnoreCase(nextValue) > 0){
						logger.info("Verifying Sorted Results for Column name : {} ", columnName);
						logger.error("Not Sorting Properly in desc null first where " + nextValue + " should come before " + previousValue);
						sorted = false;
					}
				}
				catch (Exception e) {
					logger.error("Fatal Error : Exception occured while converting the number format data in Float : {}", e.getMessage());
					return false;
				}
				return sorted;

			}


			logger.warn("sortingType Mentioned is config file is not correct : Neither desc nulls last or asc nulls first ");
			return true;

		}

		if (type.contentEquals("DATE")) //todo later since there is no standard format
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

	public void setListData(String jsonStr) {
		listData.clear();
		Map<Integer, Map<String, String>> columnData;
		Map<String, String> columnDataMap;

		try {
			JSONObject jsonObj = new JSONObject(jsonStr);
			JSONUtility json = new JSONUtility(jsonObj);
			JSONArray listDataArr = new JSONArray(json.getArrayJsonValue("data").toString());

			for (int i = 0; i < listDataArr.length(); i++) {
				columnData = new HashMap<>();
				jsonObj = new JSONObject(listDataArr.get(i).toString());

				for (String column : JSONObject.getNames(jsonObj)) {
					columnDataMap = new HashMap<>();
					JSONObject columnJsonObj = jsonObj.getJSONObject(column);
					JSONUtility columnJson = new JSONUtility(columnJsonObj);
					columnDataMap.put("id", Integer.toString(columnJson.getIntegerJsonValue("columnId")));
					String columnName = columnJson.getStringJsonValue("columnName");
					columnDataMap.put("columnName", columnName);
					if (columnName.trim().equalsIgnoreCase("bulkcheckbox")) {
						columnDataMap.put("value", columnJson.getStringJsonValue("value"));
					} else {
						String values[] = columnJson.getStringJsonValue("value").split(":;");
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

	public List<Map<Integer, Map<String, String>>> getListData() {
		return this.listData;
	}

	public String getReportListReportJSON(String payload) {
		String apiResponse = "";
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/listreportJson";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse response = super.postRequest(postRequest,payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			apiResponse = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting API {}", e.getMessage());
		}
		return apiResponse;
	}
}
