package com.sirionlabs.api.insights;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vijay.thakur on 4/09/2018.
 */
public class EntityList extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(EntityList.class);
	String entityListJsonStr = null;

	public HttpResponse hitInsightsEntityList() {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/insights/entity-list";
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "*/*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.entityListJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("insights entity-list response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting insights entity-list Api. {}", e.getMessage());
		}
		return response;
	}

	public String getInsightsEntityListJsonStr() {
		return this.entityListJsonStr;
	}
}
