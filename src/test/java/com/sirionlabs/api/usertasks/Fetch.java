package com.sirionlabs.api.usertasks;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fetch extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Fetch.class);
	private String fetchJsonStr;

	public HttpResponse hitFetch() {
		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String queryString = "/usertasks/fetch";

			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = APIUtils.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());

			this.fetchJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Fetch response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Fetch Api. {}", e.getMessage());
		}
		return response;
	}

	public String getFetchJsonStr() {
		return fetchJsonStr;
	}
}