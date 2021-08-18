package com.sirionlabs.api.bulkedit;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BulkeditCreate extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(BulkeditCreate.class);

	public static String getPayload(String recordIds) {
		return "{\"entityIds\":[" + recordIds + "]}";
	}

	public static String getPayload(List<String> recordIds) {
		return "{\"entityIds\":" + recordIds + "}";
	}

	public String hitBulkeditCreate(int entityTypeId, String payload) {
		String responseStr = null;
		try {
			HttpPost postRequest;
			String queryString = "/bulkedit/create/" + entityTypeId;
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Create response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Bulkedit Create Api. {}", e.getMessage());
		}
		return responseStr;
	}
}
