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
public class IntegrationListingListMetaData extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(IntegrationListingListMetaData.class);

	String apiStatusCode = null;
	String apiResponseTime = null;
	String integrationListingListMetaDataJsonStr = null;

	public String getApiResponse() {
		return integrationListingListMetaDataJsonStr;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitIntegrationListingListMetaData(String listId) throws Exception {
		return hitIntegrationListingListMetaData(listId, null, "{}");
	}

	public HttpResponse hitIntegrationListingListMetaData(String listId, Map<String, String> params, String payload) throws Exception {
		HttpResponse response = null;
		String queryString = "/integrationlisting/getListMetadata/" + listId;
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
		this.integrationListingListMetaDataJsonStr = EntityUtils.toString(response.getEntity());
		logger.debug("response json is: {}", integrationListingListMetaDataJsonStr);

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Integration Listing List Meta Data response header {}", headers[i].toString());
		}

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();
		apiResponseTime = super.getApiResponseTime();

		return response;
	}

}
