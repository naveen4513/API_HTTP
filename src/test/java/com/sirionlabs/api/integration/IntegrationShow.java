package com.sirionlabs.api.integration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationShow extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(IntegrationShow.class);

	public String hitIntegrationShow(int entityTypeId, String entityId) throws Exception {
		String responseStr = null;
		try {
			HttpGet getRequest;
			String queryString = "/integration/show/" + entityId + "/" + entityTypeId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

			HttpResponse response = super.getRequest(getRequest, false);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Integration Show response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Integration Show Api. {}", e.getMessage());
		}
		return responseStr;
	}
}
