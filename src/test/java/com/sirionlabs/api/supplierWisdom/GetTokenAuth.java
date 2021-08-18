package com.sirionlabs.api.supplierWisdom;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GetTokenAuth extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(GetTokenAuth.class);
	String tokenAuth = null;
	String apiStatusCode = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getTokenAuth() {
		return tokenAuth;
	}

	public HttpResponse hitTokenAuth() throws Exception {
		CookieStore httpCookieStore = null;
		HttpClientBuilder builder = null;
		HttpClient httpClient = null;
		HttpResponse response = null;

		//specify the host, protocol, and port
		HttpHost target = new HttpHost(ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomHost"),
				Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomPort")), ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomScheme"));
		httpCookieStore = new BasicCookieStore();
		builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

		// Read Proxy config from File and control HttpClient
		if (ConfigureEnvironment.isProxyEnabled) {
			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			httpClient = builder.setProxy(proxy).build();
		} else {
			httpClient = builder.build();
		}


		//specify the post request
		HttpPost postRequest = new HttpPost("/api-token-auth/");
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("username", ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomUserName")));
		nvps.add(new BasicNameValuePair("password", ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomPassword")));
		postRequest.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		response = httpClient.execute(target, postRequest);

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();

		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug(headers[i].toString());
		}

		tokenAuth = EntityUtils.toString(response.getEntity());
		return response;
	}


}
