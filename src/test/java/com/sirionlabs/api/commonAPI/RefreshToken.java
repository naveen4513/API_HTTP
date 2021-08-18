package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshToken extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(RefreshToken.class);
	private int statusCode = -1;
	private HttpResponse httpResponse = null;

	public String hitRefreshTokenAPI() {
		String response = null;

		try {
			HttpGet getRequest;
			String queryString = "/refresh";

			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			getRequest.addHeader("Content-Type", "application/json");
			getRequest.addHeader("Cookie", "refreshToken=" + Check.refreshToken + ";accessToken=" + APIUtils.accessToken + ";");

			httpResponse = APIUtils.getRequest(getRequest, false);
			statusCode = httpResponse.getStatusLine().getStatusCode();
			logger.debug("Response status is {}", httpResponse.getStatusLine().toString());
			response = EntityUtils.toString(httpResponse.getEntity());

			Header[] headers = httpResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Refresh Token API response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Refresh Token Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse getHttpResponse() {
		return httpResponse;
	}

	public int getStatusCode() {
		return statusCode;
	}
}