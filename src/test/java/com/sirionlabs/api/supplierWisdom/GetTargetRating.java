package com.sirionlabs.api.supplierWisdom;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Created by shivashish on 21/8/17.
 */
public class GetTargetRating extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(GetTargetRating.class);
	String apiResponse = null;
	String apiStatusCode = null;

	public String getApiStatusCode() {
		return apiStatusCode;
	}

	public String getApiResponse() {
		return apiResponse;
	}

	public HttpResponse hitGetTargetRatingAPI(String authToken, HashMap<String, String> queryStringParams) throws Exception {
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


		String queryString = "/api/risk/targetratings/";
		if (queryStringParams != null) {
			String urlParams = UrlEncodedString.getUrlEncodedString(queryStringParams);
			queryString += "?" + urlParams;
		}
		logger.debug("Query string url formed is {}", queryString);


		//specify the Get request
		HttpGet getRequest = new HttpGet(queryString);
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


	// This Method Will Return the hashMap of  <Supplier Id , JsonObjectHavingRatingsDetailsForSupplier>
	public HashMap<Integer, JSONObject> getMapOfRatingsForSupplier() throws Exception {

		HashMap<Integer, JSONObject> hashMapOfTargetRatings = null;
		if (apiResponse != null && !apiResponse.isEmpty()) {
			hashMapOfTargetRatings = new HashMap<Integer, JSONObject>();
			JSONObject jsonObject = new JSONObject(apiResponse);
			JSONArray jsonArray = jsonObject.getJSONArray("results");
			logger.debug("length of hashMapOfTargetRatings  is : {}", jsonArray.length());

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObjectInternal = jsonArray.getJSONObject(i);
				hashMapOfTargetRatings.put(Integer.parseInt(jsonObjectInternal.get("id").toString()), jsonObjectInternal);
			}

		} else {
			logger.debug("Can't Construct hashMapOfTargetRatings from Get TargetRatings API Response: either response in not valid json or empty");
		}

		logger.debug("hashMapOfTargetRatings is [{}]", hashMapOfTargetRatings);
		return hashMapOfTargetRatings;

	}
}
