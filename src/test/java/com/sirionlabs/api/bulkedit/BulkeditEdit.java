package com.sirionlabs.api.bulkedit;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulkeditEdit extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(BulkeditEdit.class);
	String bulkeditEditJsonStr = null;

	public HttpResponse hitBulkeditEdit(int entityTypeId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/bulkedit/edit/" + entityTypeId + "?version=2.0";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.bulkeditEditJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Edit response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Bulkedit Edit Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitBulkEditCreate(int entityTypeId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/bulkedit/create/" + entityTypeId + "?version=2.0";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.bulkeditEditJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Edit response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Bulkedit Edit Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitBulkeditEdit(int entityTypeId,int listId, String payload) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/bulkedit/edit/" + entityTypeId + "?version=2.0&listId=" + listId;
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = super.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.bulkeditEditJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Edit response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Bulkedit Edit Api. {}", e.getMessage());
		}
		return response;
	}

	public String getBulkeditEditJsonStr() {
		return this.bulkeditEditJsonStr;
	}
}
