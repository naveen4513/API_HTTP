package com.sirionlabs.api.moveToTree;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Save extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Save.class);

	public String hitSave(String payload) {
		String response = null;

		try {
			String queryString = "/moveToTree/save";
			logger.debug("Query string url formed is {}", queryString);

			HttpPost postRequest = new HttpPost(queryString);
			postRequest.addHeader("Accept", "application/json, text/plain, */*");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			response = EntityUtils.toString(httpResponse.getEntity());

		} catch (Exception e) {
			logger.error("Exception while hitting MoveToTree Save API. {}", e.getMessage());
		}

		return response;
	}
}