package com.sirionlabs.helper.api;

import com.jcraft.jsch.MAC;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

public class APIExecutor {

	private final static Logger logger = LoggerFactory.getLogger(APIExecutor.class);

	private HttpClient client;
	private String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
			ConfigureEnvironment.getEnvironmentProperty("Port");

	APIExecutor() {
		if (ConfigureEnvironment.isProxyEnabled) {
			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			client = HttpClients.custom().setProxy(proxy).build();
		} else {
			if (ConfigureEnvironment.getEnvironmentProperty("Scheme").equalsIgnoreCase("https")) {
				APIUtils apiUtils = new APIUtils();
				client = apiUtils.getHttpsClient();
			} else
			client = HttpClientBuilder.create().build();
		}
	}

	public APIValidator get(String path, Map<String, String> headers) {
		return get(hostUrl, path, headers);
	}

	public APIValidator getWithoutAuthorization(String hostUrl, String path, Map<String, String> headers) {
		HttpGet getRequest = new HttpGet(hostUrl + path);

		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				getRequest.addHeader(key, headers.get(key));
			}
		}

		return hitGetAPIWithoutAuth(getRequest);
	}

	public APIValidator get(String path, Map<String, String> requestParams, boolean isRequestParamsAvailable) {

		URIBuilder builder = new URIBuilder();
		for(Map.Entry<String,String> entry : requestParams.entrySet())
			builder.setParameter(entry.getKey(), entry.getValue());

		builder.setScheme(ConfigureEnvironment.getEnvironmentProperty("Scheme")).setHost(ConfigureEnvironment.getEnvironmentProperty("Host")).setPath(path);
		URI uri = null;
		try {
			uri = builder.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		HttpGet getRequest = new HttpGet(uri);

		return hitGetAPI(getRequest);
	}

	public APIValidator get(String hostUrl, String path, Map<String, String> headers) {
		HttpGet getRequest = new HttpGet(hostUrl + path);

		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				getRequest.addHeader(key, headers.get(key));
			}
		}

		return hitGetAPI(getRequest);
	}
	public  APIValidator delete(String path, Map<String, String> headers)
	{
		return delete(hostUrl, path, headers);
	}
	public APIValidator delete(String hostUrl, String path, Map<String, String> headers)
	{
		HttpDelete deleteRequest=new HttpDelete(hostUrl + path);
		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				deleteRequest.addHeader(key, headers.get(key));
			}
		}
          return hitDeleteAPI(deleteRequest);
	}

	private APIValidator hitDeleteAPI(HttpDelete deleteRequest) {
		APIResponse responseObj = new APIResponse();
		try {
			addMandatoryDefaultHeadersToRequest(deleteRequest);

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(deleteRequest);
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

		return new APIValidator(responseObj);
	}

	public APIValidator put(String path, Map<String, String> headers, String payload)
	 {
	 	return put(hostUrl, path, headers, payload, null);
	 }
	 public APIValidator put(String hostUrl, String path, Map<String, String> headers, String payload)
	 {
	 	return put(hostUrl, path, headers, payload, null);
	 }
	 public APIValidator put(String hostUrl, String path, Map<String, String> headers, String payload, Map<String, String> parameters)
	 {
		 HttpPut putRequest = new HttpPut(hostUrl + path);

		 if (payload != null) {
			 putRequest.setEntity(new StringEntity(payload, "UTF-8"));
		 } else if (parameters != null) {
			 String params = UrlEncodedString.getUrlEncodedString(parameters);
			 putRequest.setEntity(new StringEntity(params, "UTF-8"));
		 }

		 for (Map.Entry<String, String> headersMap : headers.entrySet()) {
			 putRequest.addHeader(headersMap.getKey(), headersMap.getValue());
		 }

		 return hitPutAPI(putRequest);
	 }

	 public  APIValidator hitPutAPI(HttpPut putRequest)
	 {
		 APIResponse responseObj = new APIResponse();
		 try {
			 addMandatoryDefaultHeadersToRequest(putRequest);

			 logger.info("API Counter: {}", ++APIUtils.apiCounter);
			 HttpResponse httpResponse = client.execute(putRequest);

			 if(httpResponse.getEntity() != null) {
				 responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			 }

			 responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			 responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			 Header[] allHeaders = httpResponse.getAllHeaders();
			 for (Header header : allHeaders) {
				 responseObj.setHeader(header.getName(), header.getValue());
			 }
		 } catch (Exception e) {
			 e.getStackTrace();
		 }

		 return new APIValidator(responseObj);
	 }
	public APIValidator post(String path, Map<String, String> headers, String payload, Map<String,String> parameters) {
		return post(hostUrl, path, headers, payload,parameters);
	}
	public APIValidator post(String path, Map<String, String> headers, String payload) {
		return post(hostUrl, path, headers, payload, null);
	}

	public APIValidator post(String hostUrl, String path, Map<String, String> headers, String payload) {
		return post(hostUrl, path, headers, payload, null);
	}

	public APIValidator post(String hostUrl, String path, Map<String, String> headers, String payload, Map<String, String> parameters) {
		HttpPost postRequest = new HttpPost(hostUrl + path);

		if (payload != null) {
			postRequest.setEntity(new StringEntity(payload, "UTF-8"));
		} else if (parameters != null) {
			String params = UrlEncodedString.getUrlEncodedString(parameters);
			postRequest.setEntity(new StringEntity(params, "UTF-8"));
		}

		for (Map.Entry<String, String> headersMap : headers.entrySet()) {
			postRequest.addHeader(headersMap.getKey(), headersMap.getValue());
		}

		return hitPostAPI(postRequest);
	}

	public APIValidator postWithoutMandatoryHeaders(String path, Map<String, String> headers, String payload) {
		return postWithoutMandatoryHeaders(hostUrl, path, headers, payload, null);
	}

	public APIValidator postWithoutMandatoryHeaders(String hostUrl, String path, Map<String, String> headers, String payload, Map<String, String> parameters) {
		HttpPost postRequest = new HttpPost(hostUrl + path);

		if (payload != null) {
			postRequest.setEntity(new StringEntity(payload, "UTF-8"));
		} else if (parameters != null) {
			String params = UrlEncodedString.getUrlEncodedString(parameters);
			postRequest.setEntity(new StringEntity(params, "UTF-8"));
		}

		for (Map.Entry<String, String> headersMap : headers.entrySet()) {
			postRequest.addHeader(headersMap.getKey(), headersMap.getValue());
		}

		return hitPostAPIWithoutMandatoryHeaders(postRequest);
	}

	public APIValidator postMultiPartFormData(String path, Map<String, String> headers, Map<String, String> parameters) throws UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost(hostUrl + path);

		String params = UrlEncodedString.getUrlEncodedString(parameters);
		postRequest.setEntity(new StringEntity(params));

		for (Map.Entry<String, String> headersMap : headers.entrySet()) {
			postRequest.addHeader(headersMap.getKey(), headersMap.getValue());
		}

		return hitPostAPI(postRequest);
	}

	public APIValidator postMultiPartFormData(String hostUrl,String path, Map<String, String> headers, Map<String, String> parameters) throws UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost(hostUrl + path);

		String params = UrlEncodedString.getUrlEncodedString(parameters);
		postRequest.setEntity(new StringEntity(params));

		for (Map.Entry<String, String> headersMap : headers.entrySet()) {
			postRequest.addHeader(headersMap.getKey(), headersMap.getValue());
		}

		return hitPostAPI(postRequest);
	}

	private APIValidator hitPostAPIWithoutMandatoryHeaders(HttpPost postRequest) {
		APIResponse responseObj = new APIResponse();
		try {

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(postRequest);
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

		return new APIValidator(responseObj);
	}

	private APIValidator hitPostAPI(HttpPost postRequest) {
		APIResponse responseObj = new APIResponse();
		try {
			addMandatoryDefaultHeadersToRequest(postRequest);

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(postRequest);
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

		return new APIValidator(responseObj);
	}
  public APIValidator getAPIWithoutMandatoryDefaultHeaders(String path, Map<String, String> headers)
  {
	  HttpGet getRequest = new HttpGet(hostUrl + path);

	  if (headers != null) {
		  Set<String> keys = headers.keySet();
		  for (String key : keys) {
			  getRequest.addHeader(key, headers.get(key));
		  }
	  }
	  APIResponse responseObj = new APIResponse();
	  try {

		  logger.info("API Counter: {}", ++APIUtils.apiCounter);
		  HttpResponse httpResponse = client.execute(getRequest);
		  responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
		  responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
		  responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

		  Header[] allHeaders = httpResponse.getAllHeaders();
		  for (Header header : allHeaders) {
			  responseObj.setHeader(header.getName(), header.getValue());
		  }
	  } catch (Exception e) {
		  e.getStackTrace();
	  }

	  return new APIValidator(responseObj);
  }

    public HttpResponse GetHttpResponseForGetAPI(String path, Map<String, String> headers)
	{
		HttpGet getRequest = new HttpGet(hostUrl + path);
		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				getRequest.addHeader(key, headers.get(key));
			}
		}
		try {
			addMandatoryDefaultHeadersToRequest(getRequest);
			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			return client.execute(getRequest);
		} catch (Exception e) {
			e.getStackTrace();
		}
		return null;
	}

	private APIValidator hitGetAPIWithoutAuth(HttpGet getRequest) {
		APIResponse responseObj = new APIResponse();
		try {

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(getRequest);
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}
		return new APIValidator(responseObj);
	}

	private APIValidator hitGetAPI(HttpGet getRequest) {
		APIResponse responseObj = new APIResponse();
		try {
			addMandatoryDefaultHeadersToRequest(getRequest);

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(getRequest);
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

		return new APIValidator(responseObj);
	}

	private void addMandatoryDefaultHeadersToRequest(HttpRequest request) {
		request.addHeader("Authorization", Check.getAuthorization());
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		request.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
	}

	public  APIValidator patch(String path, Map<String, String> headers)
	{
		return patch(hostUrl, path, headers);
	}

	public APIValidator patch(String hostUrl, String path, Map<String, String> headers)
	{
		HttpPatch patchRequest=new HttpPatch(hostUrl + path);
		if (headers != null) {
			Set<String> keys = headers.keySet();
			for (String key : keys) {
				patchRequest.addHeader(key, headers.get(key));
			}
		}
		return hitPatchAPI(patchRequest);
	}

	private APIValidator hitPatchAPI(HttpPatch patchRequest) {
		APIResponse responseObj = new APIResponse();
		try {
			addMandatoryDefaultHeadersToRequest(patchRequest);

			logger.info("API Counter: {}", ++APIUtils.apiCounter);
			HttpResponse httpResponse = client.execute(patchRequest);
			responseObj.setResponseBody(EntityUtils.toString(httpResponse.getEntity()));
			responseObj.setResponseCode(httpResponse.getStatusLine().getStatusCode());
			responseObj.setResponseMessage(httpResponse.getStatusLine().getReasonPhrase());

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header header : allHeaders) {
				responseObj.setHeader(header.getName(), header.getValue());
			}
		} catch (Exception e) {
			e.getStackTrace();
		}

		return new APIValidator(responseObj);
	}

}
