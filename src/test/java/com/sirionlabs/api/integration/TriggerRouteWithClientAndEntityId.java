package com.sirionlabs.api.integration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerRouteWithClientAndEntityId extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(TriggerRouteWithClientAndEntityId.class);

	String triggerRouteJsonStr = null;
	String apiStatusCode = null;

	public HttpResponse hitTriggerRouteWithClientAndEntityId(String clientId, String entityTypeId) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/integration/task/" + clientId + "/" + entityTypeId;

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "*/*");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequestWithoutAuth(postRequest,"");
			logger.debug("Response status is {}", response.getStatusLine().toString());
			triggerRouteJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", triggerRouteJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("triggerRoute response header {}", headers[i].toString());
			}

			logger.debug("Trigger route API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

		} catch (Exception e) {
			logger.error("Exception while hitting Trigger route Api. {}", e.getMessage());
		}
		return response;
	}

	public String getTriggerRouteJsonStr() {
		return this.triggerRouteJsonStr;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}
}