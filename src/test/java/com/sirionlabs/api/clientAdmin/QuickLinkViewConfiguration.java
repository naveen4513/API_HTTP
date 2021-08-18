package com.sirionlabs.api.clientAdmin;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickLinkViewConfiguration extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(QuickLinkViewConfiguration.class);
	public String configureDataJsonStr = null;

	public HttpResponse hitQuickLinkViewConfigure(int listId) throws Exception {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/listRenderer/list/" + listId + "/configure";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, "");
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.configureDataJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Configure Data response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting list data configure Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitlistConfigureUpdate(int listId, String payload) throws Exception {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "listRenderer/list/" + listId + "/listConfigureUpdate?reportName=undefined&_t=1529923373630";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.configureDataJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Configure Data response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting list data configure Api. {}", e.getMessage());
		}
		return response;
	}

	public String getConfigureDataJsonStr() {
		return configureDataJsonStr;
	}
}
