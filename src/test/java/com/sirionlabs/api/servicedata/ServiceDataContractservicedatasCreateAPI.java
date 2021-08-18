package com.sirionlabs.api.servicedata;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author manoj.upreti
 * @implNote This class is to implement the contractservicedatas API to create single service data for a contract
 */
public class ServiceDataContractservicedatasCreateAPI extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(ServiceDataContractservicedatasCreateAPI.class);
	String contractServiceDataCreateResponseStr = null;

	public HttpResponse hitContractServiceDatasCreateAPI() throws IOException {
		HttpResponse httpResponse = null;
		String queryString = "/contractservicedatas/create";
		logger.debug("Query string url formed is {}", queryString);

		HttpPost httpPostRequest = new HttpPost(queryString);
		httpPostRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		httpPostRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		httpPostRequest.addHeader("Accept-Encoding", "gzip, deflate");

		String contractServiceDataCreatePayload = "";

		httpResponse = super.postRequest(httpPostRequest, contractServiceDataCreatePayload);

		contractServiceDataCreateResponseStr = EntityUtils.toString(httpResponse.getEntity());
		logger.debug("The Response is : [ {} ]", httpResponse);

		logger.debug("response json is: {}", contractServiceDataCreateResponseStr);

		Header[] headers = httpResponse.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Response header {}", headers[i].toString());
		}
		logger.debug("API Status Code is : {}", httpResponse.getStatusLine().toString());
		return httpResponse;
	}

	public String getContractServiceDataCreateResponseStr() {
		return contractServiceDataCreateResponseStr;
	}

	private String generatePayloadForContractServiceDataCreate() {
		logger.debug("Generating the Payload for Contract Service Data Create API");
		int offset = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("offset"));
		int size = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("pagesize"));
		String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
		String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");

		JSONObject payload = new JSONObject();

		JSONObject filterMap = new JSONObject();
		filterMap.put("entityTypeId", JSONObject.NULL);
		filterMap.put("offset", offset);
		filterMap.put("size", size);
		filterMap.put("orderByColumnName", orderByColumnName);
		filterMap.put("orderDirection", orderDirection);

		JSONObject filterJson = new JSONObject();
		filterMap.put("filterJson", filterJson);

		payload.put("filterMap", filterMap);

		logger.debug("The Payload is generated for tabListData API, returning : [ {} ]", payload);
		return payload.toString();
	}
}
