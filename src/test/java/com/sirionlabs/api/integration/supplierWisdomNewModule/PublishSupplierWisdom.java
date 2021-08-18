package com.sirionlabs.api.integration.supplierWisdomNewModule;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishSupplierWisdom extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(PublishSupplierWisdom.class);

	String publishSupplierWisdomJsonStr = null;
	String apiStatusCode = null;

	public HttpResponse hitPublishSupplierWisdom(String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/integrationService/v1/publish";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "*/*");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			postRequest.addHeader("Content-Type", "application/json");

			response = super.postRequestWithoutAuth(postRequest,payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			publishSupplierWisdomJsonStr = EntityUtils.toString(response.getEntity());
			logger.debug("response json is: {}", publishSupplierWisdomJsonStr);

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("publishSupplierWisdom response header {}", headers[i].toString());
			}

			logger.debug("publishSupplierWisdom API Status Code is : {}", response.getStatusLine().toString());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

		} catch (Exception e) {
			logger.error("Exception while hitting publishSupplierWisdom Api. {}", e.getMessage());
		}
		return response;
	}

	public String getPublishSupplierWisdomJsonStr() {
		return this.publishSupplierWisdomJsonStr;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}
}