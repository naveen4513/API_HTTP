package com.sirionlabs.api.reportRenderer;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DownloadGraph extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(DownloadGraph.class);

	public HttpResponse hitDownloadGraphOld(Map<String, String> formParam) {
		HttpResponse response = null;

		// Read Proxy config from File and control HttpClient
		HttpClient httpClient;
		if (ConfigureEnvironment.isProxyEnabled) {
			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			httpClient = HttpClients.custom().setProxy(proxy).build();
		} else {
			httpClient = HttpClientBuilder.create().build();
		}

		try {
			HttpPost postRequest;
			String queryString = "/tblcommontask/download/image/";

			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);

			//postRequest.addHeader("Content-Type", "multipart/form-data; boundary=" + boundaryValue);
			postRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
			Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
			String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

			logger.debug("--------------------------------------------------------------------------\n");
			logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
			HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

			if (ConfigureEnvironment.useCookies) {
				logger.debug("--------------------Cookies Flag is True------------------------------");
				postRequest.addHeader("Cookie", ConfigureEnvironment.getEnvironmentProperty("Cookie"));
			} else {
				logger.debug("--------------------Cookies Flag is False------------------------------");
				logger.debug("Authorization is : {}", Check.getAuthorization());
				logger.debug("JSession Id is : {}", Check.getJSessionId());
				postRequest.addHeader("Authorization", Check.getAuthorization());
			}
			if (ConfigureEnvironment.useCSRFToken) {
				logger.debug("--------------------useCSRFToken Flag is true------------------------------");
				logger.debug("X-CSRF-TOKEN is : {}", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
				postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
			}
			postRequest.addHeader("X-Requested-With", "XMLHttpRequest");

			HttpEntity postEntity = super.multiPartFormData(formParam);
			postRequest.setEntity(postEntity);

			response = httpClient.execute(target, postRequest);

			logger.debug("Response status is {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("download graph response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting download/image Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitDownloadGraph(Map<String, String> formParam) {
		HttpResponse response = null;
		try {
			HttpPost postRequest;
			String queryString = "/tblcommontask/download/image/";

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
				logger.debug("DownloadGraph response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting DownloadGraph Api. {}", e.getMessage());
		}
		return response;
	}
}
