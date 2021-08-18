package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DownloadGraphWithData extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DownloadGraphWithData.class);

	public HttpResponse hitDownloadGraphWithData(Map<String, String> formParam, Integer reportId) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/reportRenderer/download/" + reportId + "/data/";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);
			String params = UrlEncodedString.getUrlEncodedString(formParam);
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = APIUtils.postRequest(postRequest, params);
			logger.debug("Response status is {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("DownloadGraphWithData response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting DownloadGraphWithData Api. {}", e.getMessage());
		}
		return response;
	}
}
