package com.sirionlabs.api.integration;

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
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CreateRoute extends APIUtils {
	private final static Logger logger = LoggerFactory.getLogger(CreateRoute.class);

	public String createRouteJsonStr;
	public String apiStatusCode;

	public HttpResponse hitCreateRoute(Map<String, String> params, Map<String,File> fileParam) {
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
			String queryString = "/integration/route";

			if (params != null) {
				String urlParams = UrlEncodedString.getUrlEncodedString(params);
				queryString += "?" + urlParams;
			}
			logger.debug("Query string url formed is {}", queryString);
			postRequest = new HttpPost(queryString);

			postRequest.addHeader("Accept", "*/*");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			String hostName = ConfigureEnvironment.getEnvironmentProperty("Host");
			Integer portNumber = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port"));
			String protocolScheme = ConfigureEnvironment.getEnvironmentProperty("Scheme");

			logger.debug("--------------------------------------------------------------------------\n");
			logger.debug("Making HttpPost Request with hostName: {} , portNumber:  {} , protocolScheme: {}", hostName, portNumber, protocolScheme);
			HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

			postRequest.addHeader("X-Requested-With", "XMLHttpRequest");

			HttpEntity postEntity = super.multiPartFormData(new HashMap<>(),fileParam);
			postRequest.setEntity(postEntity);

			response = httpClient.execute(target, postRequest);

			logger.debug("Response status is {}", response.getStatusLine().toString());
			createRouteJsonStr = EntityUtils.toString(response.getEntity());
			apiStatusCode = String.valueOf(response.getStatusLine().getStatusCode());

			Header[] headers = response.getAllHeaders();
			for (int i = 0; i < headers.length; i++) {
				logger.debug("Create Route response header {}", headers[i].toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Create Route Api. {}", e.getMessage());
		}
		return response;
	}

	public String getCreateRouteResponseStatusCode() {
		return apiStatusCode;
	}

	public String getCreateRouteJsonStr() {
		return createRouteJsonStr;
	}
}
