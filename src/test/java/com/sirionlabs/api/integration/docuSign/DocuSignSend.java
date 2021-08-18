package com.sirionlabs.api.integration.docuSign;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DocuSignSend extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DocuSignSend.class);
	String docuSignSendJsonStr = null;
	String apiStatusCode = null;
	String apiResponseTime = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponseTime() {
		return apiResponseTime;
	}

	public HttpResponse hitDocuSignSend(String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/docusign/send";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			docuSignSendJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", docuSignSendJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("docuSignSend response header {}", headers[i].toString());
			}

			logger.debug("API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());
			apiResponseTime = super.getApiResponseTime();

		} catch (Exception e) {
			logger.error("Exception while hitting docuSignSend Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDocuSignSendJsonStr() {
		return this.docuSignSendJsonStr;
	}
}