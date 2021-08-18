package com.sirionlabs.api.search;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ContractDocDownload extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(ContractDocDownload.class);

	public HttpResponse hitContractDocDownload(String queryText, int limit) {
		return hitContractDocDownload(queryText, limit, null);
	}

	public HttpResponse hitContractDocDownload(String queryText, int limit, Map<String, String> filtersMap) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/search/contractDoc/download";
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
			postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			Map<String, String> parameters = new HashMap<>();
			parameters.put("queryText", queryText);
			parameters.put("documentTypes", "1");
			parameters.put("limit", Integer.toString(limit));

			if (filtersMap != null) {
				for (String filterKey : filtersMap.keySet()) {
					parameters.put(filterKey, filtersMap.get(filterKey));
				}
			}
			String params = UrlEncodedString.getUrlEncodedString(parameters);
			response = super.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers)
				logger.debug("ContractDocDownload response header {}", oneHeader.toString());
		} catch (Exception e) {
			logger.error("Exception while hitting ContractDocDownload Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse downloadContractDocResultsFile(String file, String queryText, int limit) {
		return downloadContractDocResultsFile(file, queryText, limit, null);
	}

	public HttpResponse downloadContractDocResultsFile(String file, String queryText, int limit, Map<String, String> filtersMap) {
		try {
			HttpHost target = generateHttpTargetHost();
			String queryString = "/search/contractDoc/download";
			String acceptsHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
			String contentTypeHeader = "application/x-www-form-urlencoded";
			Map<String, String> parameters = new HashMap<>();
			parameters.put("queryText", queryText);
			parameters.put("documentTypes", "1");
			parameters.put("limit", Integer.toString(limit));

			if (filtersMap != null) {
				for (String filterKey : filtersMap.keySet()) {
					parameters.put(filterKey, filtersMap.get(filterKey));
				}
			}
			String params = UrlEncodedString.getUrlEncodedString(parameters);
			HttpPost httpPostRequest = generateHttpPostRequestWithQueryStringAndPayload(queryString, acceptsHeader, contentTypeHeader, params);
			return downloadAPIResponseFile(file, target, httpPostRequest);
		} catch (Exception e) {
			logger.error("Exception while Downloading ContractDoc File {}. {}", file, e.getStackTrace());
			return null;
		}
	}
}
