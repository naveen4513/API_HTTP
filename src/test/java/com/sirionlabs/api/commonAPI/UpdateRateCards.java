package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateRateCards extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(UpdateRateCards.class);
	private String updateRateCardsJsonStr;

	public HttpResponse hitUpdateRateCards(String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;

			String queryString = "/updateRateCards";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.updateRateCardsJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Update Rate Cards response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Update Rate Cards Api. {}", e.getMessage());
		}
		return response;
	}

	public String getUpdateRateCardsJsonStr() {
		return updateRateCardsJsonStr;
	}
}
