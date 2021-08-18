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

public class SearchContractDoc extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(SearchContractDoc.class);
	private String contractDocJsonStr = null;

	public HttpResponse hitSearchContractDoc(String queryText) {
		return hitSearchContractDoc(queryText, 5, 0, null);
	}

	public HttpResponse hitSearchContractDoc(String queryText, int limit, int offset) {
		return hitSearchContractDoc(queryText, limit, offset, null);
	}

	public HttpResponse hitSearchContractDoc(String queryText, int limit, int offset, Map<String, String> filtersMap) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/search/contractDoc";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("queryText", queryText);
			parameters.put("documentTypes", "1");
			parameters.put("limit", Integer.toString(limit));
			parameters.put("offset", Integer.toString(offset));

			if (filtersMap != null) {
				for (String filterKey : filtersMap.keySet()) {
					parameters.put(filterKey, filtersMap.get(filterKey));
				}
			}

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			response = super.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			this.contractDocJsonStr = EntityUtils.toString(response.getEntity());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers)
				logger.debug("SearchContractDoc response header {}", oneHeader.toString());
		} catch (Exception e) {
			logger.error("Exception while hitting SearchContractDoc Api. {}", e.getMessage());
		}
		return response;
	}

	public String getContractDocJsonStr() {
		return contractDocJsonStr;
	}
}
