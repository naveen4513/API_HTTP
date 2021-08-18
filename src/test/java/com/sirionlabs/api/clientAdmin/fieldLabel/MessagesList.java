package com.sirionlabs.api.clientAdmin.fieldLabel;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessagesList extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(MessagesList.class);

	public String hitFieldLabelMessagesList(String payload) throws Exception {
		String response = null;
		try {
			HttpPost postRequest;
			String queryString = "/fieldlabel/messages/list";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/plain, */*");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Messages List response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Field Label Messages List API. {}", e.getMessage());
		}
		return response;
	}
}