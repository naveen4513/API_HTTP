package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class Login extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Login.class);

	private HttpResponse hitLogin() {
		HttpResponse response = null;
		try {
			// Read Proxy config from File and control HttpClient
			HttpClient httpClient;
			if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
				httpClient = super.getHttpsClient();
			} else {
				if (ConfigureEnvironment.isProxyEnabled) {
					HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
					httpClient = HttpClients.custom().setProxy(proxy).build();
				} else {
					httpClient = HttpClientBuilder.create().build();
				}
			}

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
					Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the get request
			HttpGet getRequest = new HttpGet("/login");
			getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			logger.info("TestLogin method {}", getRequest.getURI().toString());

			if (ConfigureEnvironment.useCSRFToken)
				getRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

			response = httpClient.execute(target, getRequest);
			logger.debug(response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();

			for (Header oneHeader : headers) {
				logger.debug(oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while Hitting Login API. {}", e.getMessage());
			System.exit(0);
		}
		return response;
	}

	@Test()
	public void testLogin() {
		if (ConfigureEnvironment.useCookies)
			throw new SkipException("Skipping Login");

		this.hitLogin();
	}
}
