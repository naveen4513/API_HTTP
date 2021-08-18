package com.sirionlabs.api.docusignService;

import com.sirionlabs.config.ConfigureEnvironment;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 12/4/18.
 */
public class AdminRedirectUrl {

	private final static Logger logger = LoggerFactory.getLogger(AdminRedirectUrl.class);

	String apiStatusCode = null;
	String adminRedirectUrlAPIRepsonse = null;
	String uri = "/documentService/v1/admin/redirectUrl/";


	public String getApiResponse() {
		return adminRedirectUrlAPIRepsonse;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}


	public HttpResponse hitGetAdminRedirectUrlAPI(String clientId) throws Exception {
		CookieStore httpCookieStore = null;
		HttpClientBuilder builder = null;
		HttpClient httpClient = null;
		HttpResponse response = null;

		//specify the host, protocol, and port
		HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("docusignHost"),
				Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("docusignPort")), ConfigureEnvironment.getEnvironmentProperty("docusignScheme"));

		httpCookieStore = new BasicCookieStore();
		builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);
		// Read Proxy config from File and control HttpClient
		if (ConfigureEnvironment.isProxyEnabled) {
			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			httpClient = builder.setProxy(proxy).build();
		} else {
			httpClient = builder.build();
		}

		//specify the get request
		HttpGet getRequest = new HttpGet(uri + "?clientId=" + clientId);


		response = httpClient.execute(target, getRequest);

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());

		apiStatusCode = response.getStatusLine().toString();


		Header[] headers = response.getAllHeaders();

		for (int i = 0; i < headers.length; i++) {
			logger.debug(headers[i].toString());
		}

		adminRedirectUrlAPIRepsonse = EntityUtils.toString(response.getEntity());
		return response;
	}


}
