package com.sirionlabs.api.integration;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vijay.thakur on 5/31/2018
 */
public class ContractPortingForm extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ContractPortingForm.class);
	String contractPortJsonStr = null;
	String statusCode = null;

	public HttpResponse hitGetContractPortingForm(String contractStagingId) {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/contracts/staging/"+contractStagingId;
			getRequest = new HttpGet(queryString);

			getRequest.addHeader("Accept", "*/*");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.contractPortJsonStr = EntityUtils.toString(response.getEntity());
			this.statusCode = String.valueOf(response.getStatusLine().getStatusCode());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("GET route response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting GET contract port Api. {}", e.getMessage());
		}
		return response;
	}

	public String getContractPortingFormJsonStr() {
		return this.contractPortJsonStr;
	}

	public String getContractPortingFormStatusCode() {
		return this.statusCode;
	}
}
