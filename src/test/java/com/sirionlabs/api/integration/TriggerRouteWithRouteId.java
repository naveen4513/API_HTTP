package com.sirionlabs.api.integration;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerRouteWithRouteId extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(TriggerRouteWithRouteId.class);

	String triggerRouteJsonStr = null;
	String apiStatusCode = null;

	public HttpResponse hitTriggerRouteWithRouteId(String camelRouteId) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/integration/task/" + camelRouteId;

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