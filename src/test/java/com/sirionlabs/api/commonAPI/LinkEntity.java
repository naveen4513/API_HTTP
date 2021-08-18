package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkEntity extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(LinkEntity.class);

	public String hitLinkEntity(int sourceEntityTypeId,int sourceEntityId) {
		String responseStr = null;
		try {
			HttpGet getRequest;
			String queryString = "/linkentity/getentitytypes?sourceEntityTypeId="+sourceEntityTypeId+"&sourceEntityId="+sourceEntityId;
			logger.debug("Query string url formed is {}", queryString);

			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");

			HttpResponse response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			responseStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("LinkEntity Get response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting LinkEntity Get Api. {}", e.getMessage());
		}
		return responseStr;
	}

	public String hitLinkEntity(String payload) throws Exception {
		String responseStr = null;
		try {
			HttpPost postRequest;
			String queryString = "/linkentity/link";
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
				logger.debug("LinkEntity Post response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting LinkEntity Post Api. {}", e.getMessage());
		}
		return responseStr;
	}

}