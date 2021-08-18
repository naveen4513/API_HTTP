package com.sirionlabs.api.integration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteRoute extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DeleteRoute.class);
	private String deleteRouteJsonStr = null;
	public String apiStatusCode;

	public HttpResponse hitDeleteRoute(String clientId, String entityTypeId) {
		HttpResponse response = null;
		try {
			HttpDelete deleteRequest;

			String queryString = "/integration/route/"+clientId+"/"+entityTypeId;
			logger.debug("Query string url formed is {}", queryString);

			deleteRequest = new HttpDelete(queryString);
			deleteRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			deleteRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			deleteRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.deleteRequestWithoutAuth(deleteRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			this.deleteRouteJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				logger.debug("DeleteRoute response header {}", header.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting DeleteRoute Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDeleteRouteJsonStr() {
		return deleteRouteJsonStr;
	}

	public String getDeleteRouteResponseStatusCode() {
		return apiStatusCode;
	}
}
