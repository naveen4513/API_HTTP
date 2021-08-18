package com.sirionlabs.api.massEmail;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateMassEmail extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(CreateMassEmail.class);

	public String hitCreateMassEmail(String payload) throws Exception {
		String responseStr = null;
		try {
			HttpPost postRequest;
			String queryString = "/massEmail/create";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse response = super.postRequest(postRequest, payload);

			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Mass Email Create response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Mass Email Create Api. {}", e.getMessage());
		}
		return responseStr;
	}
}
