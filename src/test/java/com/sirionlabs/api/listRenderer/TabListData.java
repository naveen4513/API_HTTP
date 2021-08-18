package com.sirionlabs.api.listRenderer;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class TabListData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(TabListData.class);

	String tabListDataResponseStr = null;
	String apiStatusCode = null;

	String[] getStructuredPerformanceExcelIds;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public HttpResponse hitTabListData(String tabSpecificID, boolean isServiceDataTab, String entityTypeId, String parentEntityTypeId, String parentEntityId) throws IOException {
		logger.debug("Hitting tablistdata API");
		HttpResponse response = null;
		String queryString = "/listRenderer/list/" + tabSpecificID + "/tablistdata/" + parentEntityTypeId + "/" + parentEntityId;
							
		logger.debug("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		postRequest.addHeader("Accept-Encoding", "gzip, deflate");

		String requestPayload = generateTabListDataPayload(entityTypeId, isServiceDataTab);
		logger.debug("Requested Payload : [ {} ]", requestPayload);
		response = super.postRequest(postRequest, requestPayload);


		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("hitTabListData response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

		tabListDataResponseStr = EntityUtils.toString(response.getEntity());
		logger.debug("The Response is : [ {} ]", tabListDataResponseStr);
		return response;
	}

	// overloaded function hitTabListData in which payload is taken as parameter
	public HttpResponse hitTabListData(String tabSpecificID, boolean isServiceDataTab, String entityTypeId, String parentEntityTypeId, String parentEntityId, String payload) throws IOException {
		logger.debug("Hitting tablistdata API");
		HttpResponse response = null;
		String queryString = "/listRenderer/list/" + tabSpecificID + "/tablistdata/" + parentEntityTypeId + "/" + parentEntityId;
		logger.debug("Query string url formed is {}", queryString);

		HttpPost postRequest = new HttpPost(queryString);
		postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		postRequest.addHeader("Accept-Encoding", "gzip, deflate");


		logger.debug("Payload is: [ {} ]", payload);
		response = super.postRequest(postRequest, payload);


		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("hitTabListData response header {}", headers[i].toString());
		}


		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		tabListDataResponseStr = EntityUtils.toString(response.getEntity());
		logger.debug("The Response is : [ {} ]", tabListDataResponseStr);
		return response;
	}

	public String getTabListDataResponseStr() {
		return tabListDataResponseStr;
	}

	private String generateTabListDataPayload(String entityTypeId, boolean isServiceDataTab) {
		logger.debug("Generating the Payload for entityTypeId : [ {} ]", entityTypeId);
		int offset = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("offset"));
		int size = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("pagesize"));
		String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
		String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");

		JSONObject payload = new JSONObject();

		JSONObject filterMap = new JSONObject();
		filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
		filterMap.put("offset", offset);
		filterMap.put("size", size);
		filterMap.put("orderByColumnName", orderByColumnName);
		filterMap.put("orderDirection", orderDirection);

		JSONObject filterJson = new JSONObject();
		filterMap.put("filterJson", filterJson);

		payload.put("filterMap", filterMap);

		if (isServiceDataTab) {
			payload.put("contractSpecific", true);
		}
		logger.debug("The Payload is generated for tabListData API, returning : [ {} ]", payload);
		return payload.toString();
	}

	public String[] getStructuredPerformanceExcelIds() {
		JSONObject tabListDataResponseObj = new JSONObject(getTabListDataResponseStr());
		int noOfRecords = tabListDataResponseObj.getJSONArray("data").length();

		if (noOfRecords >= 1) {
			getStructuredPerformanceExcelIds = new String[noOfRecords];
			for (int i = 0; i < noOfRecords; i++) {
				JSONObject jsonObject = tabListDataResponseObj.getJSONArray("data").getJSONObject(i);
				if (JSONUtility.getValueByEmbeddedKey("filename", jsonObject, "value") != null)
					getStructuredPerformanceExcelIds[i] = JSONUtility.getValueByEmbeddedKey("filename", jsonObject, "value");
				else {
					logger.error("Fatal Error : filename value is null in Json Response of ServiceLevel Performance Data Tab");
					return null;
				}

			}
			return getStructuredPerformanceExcelIds;
		} else {
			logger.error("Error:CSL Structured Performance Data Tab API Response Data Field is Empty ");
			return null;
		}
	}


	public String hitTabListData(Integer tabId, Integer entityTypeId, Integer entityId) {
		String defaultPayload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
		return hitTabListData(tabId, entityTypeId, entityId, defaultPayload);
	}

	public String hitTabListData(Integer tabId, Integer entityTypeId, Integer entityId,int offset,int size) {
		String defaultPayload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
		return hitTabListData(tabId, entityTypeId, entityId, defaultPayload);
	}

	public String hitTabListData(Integer tabId, Integer entityTypeId, Integer entityId, String payload) {
		try {
			String queryString = "/listRenderer/list/" + tabId + "/tablistdata/" + entityTypeId + "/" + entityId;
			logger.debug("Query string url formed is {}", queryString);

			logger.info("Hitting TabListData API for TabId {} of Record Id {} of EntityTypeId {}", tabId, entityId, entityTypeId);

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = APIUtils.postRequest(postRequest, payload);

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("hitTabListData response header {}", oneHeader.toString());
			}

			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

			tabListDataResponseStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting TabListData API for TabId {}, EntityTypeId {} and EntityId {}. {}", tabId, entityTypeId, entityId, e.getStackTrace());
		}
		return tabListDataResponseStr;
	}

	public String hitTabListData(Integer tabId, Integer entityTypeId, Integer entityId, Integer parentId) {
		String defaultPayload = "{\"filterMap\":{\"entityTypeId\":" + parentId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
		return hitTabListData(tabId, entityTypeId, entityId, defaultPayload);
	}


	public String hitTabListData(int tabId, int entityTypeId, int entityId, int offset, int size, String orderByColumnName, String orderDirection) {

		String tabListDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
				"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";

		logger.debug("Hitting tabListData API");
		logger.debug("entityTypeId : {}", entityTypeId);
		logger.debug("offset : {}", offset);
		logger.debug("size : {}", size);
		logger.debug("with orderByColumnName : {}", orderByColumnName);
		logger.debug("and orderDirection : {}", orderDirection);
		logger.debug("tabId : {}", tabId);

		return hitTabListData(tabId, entityTypeId, entityId, tabListDataPayload);
	}

	public String hitTabListDataV2(Integer tabId, Integer entityTypeId, Integer entityId) {
		try {
			String defaultPayload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

			String queryString = "/listRenderer/list/" + tabId + "/tablistdata/" + entityTypeId + "/" + entityId + "?version=2.0";
			logger.debug("Query string url formed is {}", queryString);

			logger.info("Hitting TabListData API for TabId {} of Record Id {} of EntityTypeId {}", tabId, entityId, entityTypeId);

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = APIUtils.postRequest(postRequest, defaultPayload);

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("hitTabListData response header {}", oneHeader.toString());
			}

			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

			tabListDataResponseStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting TabListData API for TabId {}, EntityTypeId {} and EntityId {}. {}", tabId, entityTypeId, entityId, e.getStackTrace());
		}
		return tabListDataResponseStr;
	}

	public String hitTabListDataV2(Integer tabId, Integer entityTypeId, Integer entityId,String payload) {
		try {

			String queryString = "/listRenderer/list/" + tabId + "/tablistdata/" + entityTypeId + "/" + entityId + "?version=2.0";
			logger.debug("Query string url formed is {}", queryString);

			logger.info("Hitting TabListData API for TabId {} of Record Id {} of EntityTypeId {}", tabId, entityId, entityTypeId);

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = APIUtils.postRequest(postRequest, payload);

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("hitTabListData response header {}", oneHeader.toString());
			}

			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

			tabListDataResponseStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting TabListData API for TabId {}, EntityTypeId {} and EntityId {}. {}", tabId, entityTypeId, entityId, e.getStackTrace());
		}
		return tabListDataResponseStr;
	}

	public List<Map<String,String>> getTabDataList(int tabId,int entityTypeId, int entityId,String payload, CustomAssert customAssert){

		List<Map<String,String>> chargesTabData = new ArrayList<>();

		TabListData tabListData = new TabListData();
		try{

			String tabListResponse = tabListData.hitTabListDataV2(tabId, entityTypeId, entityId,payload);

			if(JSONUtility.validjson(tabListResponse)){

				JSONObject tabListResponseJson = new JSONObject(tabListResponse);

				JSONArray dataArray = tabListResponseJson.getJSONArray("data");

				for(int i =0;i<dataArray.length();i++){

					Iterator<String> keys  = dataArray.getJSONObject(i).keys();
					Map<String,String> columnValuesMap = new HashMap<>();

					while (keys.hasNext()){
						String key = keys.next();

						String columnName = dataArray.getJSONObject(i).getJSONObject(key).get("columnName").toString();
						String columnValue = dataArray.getJSONObject(i).getJSONObject(key).get("value").toString();

						columnValuesMap.put(columnName,columnValue);
					}
					chargesTabData.add(columnValuesMap);
				}

			} else {
				logger.error("Tab List Response is not a valid json");
				customAssert.assertTrue(false,"Tab List Response is not a valid json for tab id " + tabId + " and record Id " + entityId);
			}


		}catch (Exception e){
			logger.error("Exception while validating charges tab");
			customAssert.assertTrue(false,"Exception while validating charges tab");

		}
		return chargesTabData;
	}


}
