package com.sirionlabs.api.search;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SearchAttachment extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(SearchAttachment.class);
	private String attachmentJsonStr = null;

	public HttpResponse hitAttachment(String queryText, int entityTypeId) {
		return hitAttachment(queryText, entityTypeId, 5, 0, null);
	}

	public HttpResponse hitAttachment(String queryText, int entityTypeId, int limit, int offset) {
		return hitAttachment(queryText, entityTypeId, limit, offset, null);
	}

	public HttpResponse hitAttachment(String queryText, int entityTypeId, int limit, int offset, Map<String, String> filtersMap) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/search/attachment";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			Map<String, String> parameters = new HashMap<>();
			parameters.put("queryText", queryText);
			parameters.put("documentTypes", "2");
			parameters.put("limit", Integer.toString(limit));
			parameters.put("offset", Integer.toString(offset));
			parameters.put("entityTypeId", Integer.toString(entityTypeId));

			if (filtersMap != null) {
				for (String filterKey : filtersMap.keySet()) {
					parameters.put(filterKey, filtersMap.get(filterKey));
				}
			}

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			response = super.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.attachmentJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers)
				logger.debug("SearchAttachment response header {}", oneHeader.toString());
		} catch (Exception e) {
			logger.error("Exception while hitting SearchAttachment Api. {}", e.getMessage());
		}
		return response;
	}

	public String getAttachmentJsonStr() {
		return attachmentJsonStr;
	}
}
