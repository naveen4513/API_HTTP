package com.sirionlabs.api.integration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vijay.thakur on 5/3/2018
 */
public class GetRoute extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(GetRoute.class);
	String getRouteJsonStr = null;
	String statusCode = null;

	public HttpResponse hitGetRoute(String clientId, String entityTypeId) {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/integration/route/"+clientId+"/"+entityTypeId;
			getRequest = new HttpGet(queryString);

			getRequest.addHeader("Accept", "*/*");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.getRequestWithoutAuthorization(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.getRouteJsonStr = EntityUtils.toString(response.getEntity());
			this.statusCode = String.valueOf(response.getStatusLine().getStatusCode());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("GET route response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting GET route Api. {}", e.getMessage());
		}
		return response;
	}

	public String getRouteJsonStr() {
		return this.getRouteJsonStr;
	}

	public String getRouteStatusCode() {
		return this.statusCode;
	}
}
