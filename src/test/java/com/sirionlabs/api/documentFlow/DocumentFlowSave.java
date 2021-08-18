package com.sirionlabs.api.documentFlow;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentFlowSave extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(DocumentFlowSave.class);
	String documentFlowSaveJsonStr = null;

	public HttpResponse hitDocumentFlowSave(String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;

			String queryString = "/documentFlow/save";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.documentFlowSaveJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("documentFlowSave response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting documentFlowSave Api. {}", e.getMessage());
		}
		return response;
	}

	public String getDocumentFlowSaveJsonStr() {
		return documentFlowSaveJsonStr;
	}
}
