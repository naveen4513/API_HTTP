package com.sirionlabs.api.IntegrationListing;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by shivashish on 8/12/17.
 */
public class IntegrationListingGetTypes extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(IntegrationListingGetTypes.class);

	String apiStatusCode = null;
	String apiResponseTime = null;
	String integrationListingGetTypesJsonStr = null;

	public String getApiResponse() {
		return integrationListingGetTypesJsonStr;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitIntegrationListingGetTypes(String listId) throws Exception {
		return hitIntegrationListingGetTypes(listId, null);
	}

	public HttpResponse hitIntegrationListingGetTypes(String listId, Map<String, String> params) throws Exception {
		HttpResponse response = null;
		String queryString = "/integrationlisting/getListTypes/" + listId;
		if (params != null) {
			String urlParams = UrlEncodedString.getUrlEncodedString(params);
			queryString += "?" + urlParams;
		}
		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
		getRequest.addHeader("Accept-Encoding", "gzip, deflate");

		response = super.getRequest(getRequest);
		logger.debug(response.getStatusLine().toString());
		this.integrationListingGetTypesJsonStr = EntityUtils.toString(response.getEntity());
		logger.debug("response json is: {}", integrationListingGetTypesJsonStr);

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug("Integration Listing Get Types API response header {}", headers[i].toString());
		}

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();
		apiResponseTime = super.getApiResponseTime();

		return response;
	}

}
