package com.sirionlabs.api.IntegrationListing;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by shivashish on 6/12/17.
 */
public class IntegrationListingListData extends APIUtils {


	private final static Logger logger = LoggerFactory.getLogger(IntegrationListingListData.class);

	String apiStatusCode = null;
	String apiResponseTime = null;
	String integrationListingListDataJsonStr = null;

	public String getApiResponse() {
		return integrationListingListDataJsonStr;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitIntegrationListingListData(String listId) throws Exception {
		return hitIntegrationListingListData(listId, null, "{}");
	}

	public HttpResponse hitIntegrationListingListData(String listId, String payload) throws Exception {
		return hitIntegrationListingListData(listId, null, payload);
	}

	public HttpResponse hitIntegrationListingListData(String listId, Map<String, String> params, String payload) throws Exception {
		HttpResponse response = null;
		String queryString = "/integrationlisting/getListData/" + listId;
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
		this.integrationListingListDataJsonStr = EntityUtils.toString(response.getEntity());
		logger.debug("response json is: {}", integrationListingListDataJsonStr);

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Integration Listing List Data API response header {}", headers[i].toString());
		}

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();
		apiResponseTime = super.getApiResponseTime();

		return response;
	}
}
