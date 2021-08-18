package com.sirionlabs.api.snowIntegration;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
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

import java.util.Base64;
import java.util.HashMap;

/**
 * Created by shivashish on 13/9/17.
 */
public class GetExcelRowCount {

	private final static Logger logger = LoggerFactory.getLogger(GetExcelRowCount.class);
	String apiResponse = null;
	String apiStatusCode = null;
	int totalRowCount = -1;

	public int getTotalRowCount() {
		return totalRowCount;
	}

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponse() {
		return apiResponse;
	}

	public HttpResponse hitGetExcelRowCount(HashMap<String, String> queryStringParams) throws Exception {
		CookieStore httpCookieStore = null;
		HttpClientBuilder builder = null;
		HttpClient httpClient = null;
		HttpResponse response = null;
		String userNamePassword = ConfigureConstantFields.getConstantFieldsProperty("serviceNowUserName") + ":" + ConfigureConstantFields.getConstantFieldsProperty("serviceNowPassword");
		String encoding = "Basic " + Base64.getEncoder().encodeToString(userNamePassword.getBytes());
		logger.debug("Encoding is : [{}]", encoding);


		//specify the host, protocol, and port
		HttpHost target = new HttpHost(ConfigureConstantFields.getConstantFieldsProperty("serviceNowHostName"),
				Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("serviceNowHostPort")), ConfigureConstantFields.getConstantFieldsProperty("serviceNowHostScheme"));
		httpCookieStore = new BasicCookieStore();
		builder = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

		// Read Proxy config from File and control HttpClient
		if (ConfigureEnvironment.isProxyEnabled) {
			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			httpClient = builder.setProxy(proxy).build();
		} else {
			httpClient = builder.build();
		}


		String queryString = "/api/now/table/incident";
		if (queryStringParams != null) {
			String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
			queryString += "?" + urlParams.replace("%28","(").replace("%27","'").replace("%29",")").replace("%3D","=");

		}
		logger.debug("Query string url formed is {}", queryString);


		//specify the Get request
		HttpGet getRequest = new HttpGet(queryString);
		getRequest.addHeader("Authorization", encoding);
		getRequest.addHeader("Accept", "application/json;charset=UTF-8");
		getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
		response = httpClient.execute(target, getRequest);

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();
		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug(headers[i].toString());
			if ((headers[i].toString().contains("X-Total-Count"))) {
				totalRowCount = Integer.parseInt((headers[i].toString()).split(" ")[1]);
			}
		}

		apiResponse = EntityUtils.toString(response.getEntity());
		return response;
	}


}
