package com.sirionlabs.api.supplierWisdom;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by shivashish on 21/8/17.
 */
public class GetTargetType extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(GetTargetType.class);
	String apiResponse = null;
	String apiStatusCode = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponse() {
		return apiResponse;
	}

	public HttpResponse hitTargetTypeAPI(String authToken) throws Exception {
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
		HttpGet getRequest = new HttpGet("/api/v1/targettypes/");
		getRequest.addHeader("Authorization", authToken);
		response = httpClient.execute(target, getRequest);

		logger.debug("API Status Code is : {}", response.getStatusLine().toString());
		apiStatusCode = response.getStatusLine().toString();


		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			logger.debug(headers[i].toString());
		}

		apiResponse = EntityUtils.toString(response.getEntity());
		return response;
	}


	// This Method Will Return the hashMap of Target Types <Id , TargetNames>
	public HashMap<Integer, String> getMapOfTargetTypes() throws Exception {

		HashMap<Integer, String> hashMapOfTargetTypes = null;
		if (apiResponse != null && !apiResponse.isEmpty()) {
			hashMapOfTargetTypes = new HashMap<Integer, String>();
			JSONObject jsonObject = new JSONObject(apiResponse);
			JSONArray jsonArray = jsonObject.getJSONArray("results");
			logger.debug("length of hashMapOfTargetTypes is : {}", jsonArray.length());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObjectInternal = jsonArray.getJSONObject(i);
				hashMapOfTargetTypes.put(Integer.parseInt(jsonObjectInternal.get("id").toString()), jsonObjectInternal.get("name").toString());
			}

		} else {
			logger.debug("Can't Construct hashMapOfTargetTypes from GetTargetType API Response because either API response is not Json or empty");
		}

		logger.debug("hashMapOfTargetTypes is [{}]", hashMapOfTargetTypes);
		return hashMapOfTargetTypes;

	}

}
