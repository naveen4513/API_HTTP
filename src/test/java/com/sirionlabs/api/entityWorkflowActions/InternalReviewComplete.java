package com.sirionlabs.api.entityWorkflowActions;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 18/4/18.
 */
public class InternalReviewComplete extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(InternalReviewComplete.class);
	private String internalReviewCompleteJsonStr = null;

	public void hitInternalReviewComplete(String entityName, String uriName, String payload) throws Exception {
		HttpResponse response = null;
		try {
			HttpPost postRequest;

			if (uriName != null) {
				String queryString = "/" + uriName + "/work-7015";
				logger.debug("Query string url formed is {}", queryString);
				postRequest = new HttpPost(queryString);
				postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
				postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
				postRequest.addHeader("Accept-Encoding", "gzip, deflate");
				response = super.postRequest(postRequest, payload);
				logger.debug("Response status is {}", response.getStatusLine().toString());
				this.internalReviewCompleteJsonStr = EntityUtils.toString(response.getEntity());

				Header[] headers = response.getAllHeaders();
				for (Header oneHeader : headers) {
					logger.debug("InternalReviewComplete response header {}", oneHeader.toString());
				}
			} else {
				logger.error("Couldn't get URI Name for Entity {}. Hence not hitting InternalReviewComplete (work-7015) API", entityName);
			}
		} catch (Exception e) {
			logger.error("Exception while hitting InternalReviewComplete (work-7015) API for Entity {}. {}", entityName, e.getStackTrace());
		}
	}

	public String getInternalReviewCompleteJsonStr() {
		return this.internalReviewCompleteJsonStr;
	}
}
