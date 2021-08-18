package com.sirionlabs.api.usertasks;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remove extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Remove.class);
	private String removeJsonStr;

	public HttpResponse hitRemove(String payload) throws Exception {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/usertasks/remove";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "text/html, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.removeJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Remove response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Remove Api. {}", e.getMessage());
		}
		return response;
	}

	public String getRemoveJsonStr() {
		return removeJsonStr;
	}
}
