package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.jwt.JWTAccessTokenHelper;
import com.sirionlabs.helper.jwt.JWTRefreshTokenHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.MD5Value;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Check extends APIUtils {

	private final static Logger logger = LoggerFactory.getLogger(Check.class);
	public static String authorization = null;
	private static String jSessionId = null;
	public static String refreshToken = null;
	public static String lastLoggedInUserName = null;
	public static String lastLoggedInUserPassword = null;

	public static String getAuthorization() {
		return authorization;
	}

	public static String getJSessionId() {
		return jSessionId;
	}

	public HttpResponse hitCheck() {
		return hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
	}

	public HttpResponse hitCheck(String username, String password) {
		HttpResponse response = null;

		try {
			lastLoggedInUserName = username;
			lastLoggedInUserPassword = password;

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
					Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the post request
			HttpPost postRequest = new HttpPost("/api/v1/check");
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

			if (ConfigureEnvironment.useCSRFToken)
				postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

			Map<String, String> parameters = new HashMap<>();
			parameters.put("j_username", username);
			parameters.put("password", password);
			parameters.put("j_password", MD5Value.getMD5(password));

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			logger.info("Testing environment {} with username/password {} {}", target, username, password);
			logger.debug("parameters: {}", params);

			postRequest.setEntity(new StringEntity(params));

			response = APIUtils.postRequest(postRequest, null);
			boolean loginSuccessful = true;
			if(response.getStatusLine().getStatusCode()!=200){
				loginSuccessful = false;
			}

			Header[] headers = response.getAllHeaders();

			if (!loginSuccessful) {
				logger.error("Login Unsuccessful. Exiting the Suite.");
				System.exit(0);
			}

			Check.authorization = this.getProperty(headers, "authorization=");
			Check.jSessionId = this.getProperty(headers, "jsessionid");

			if (ConfigureEnvironment.jwtEnabled) {
				APIUtils.accessToken = this.getProperty(headers, "accessToken");
				Check.refreshToken = this.getProperty(headers, "refreshToken");
			}

		} catch (Exception e) {
			logger.error("Exception while hitting Check Api. {}", e.getMessage());
		}
		return response;
	}

	public Boolean hitCheckforceReset(String username, String password) {
		HttpResponse response = null;
		boolean status = false;

		try {
			lastLoggedInUserName = username;
			lastLoggedInUserPassword = password;

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
					Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the post request
			HttpPost postRequest = new HttpPost("/api/v1/check");
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

			if (ConfigureEnvironment.useCSRFToken)
				postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

			Map<String, String> parameters = new HashMap<>();
			parameters.put("j_username", username);
			parameters.put("password", password);
			parameters.put("j_password", MD5Value.getMD5(password));

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			logger.info("Testing environment {} with username/password {} {}", target, username, password);
			logger.debug("parameters: {}", params);

			postRequest.setEntity(new StringEntity(params));

			response = APIUtils.postRequest(postRequest, null);

			if(response.getStatusLine().getStatusCode()==200){
				status = true;
			}

			Header[] headers = response.getAllHeaders();

			boolean loginSuccessful = true;




			Check.authorization = this.getProperty(headers, "authorization=");
			Check.jSessionId = this.getProperty(headers, "jsessionid");

			if (ConfigureEnvironment.jwtEnabled) {
				APIUtils.accessToken = this.getProperty(headers, "accessToken");
				Check.refreshToken = this.getProperty(headers, "refreshToken");
			}

		} catch (Exception e) {
			logger.error("Exception while hitting Check Api. {}", e.getMessage());
		}
		return status;
	}

	public Integer hitCheckwithInvalidCredentials(String username, String password) {
		HttpResponse response = null;
		boolean status = false;

		try {
			lastLoggedInUserName = username;
			lastLoggedInUserPassword = password;

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
					Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the post request
			HttpPost postRequest = new HttpPost("/api/v1/check");
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

			if (ConfigureEnvironment.useCSRFToken)
				postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

			Map<String, String> parameters = new HashMap<>();
			parameters.put("j_username", username);
			parameters.put("password", password);
			parameters.put("j_password", MD5Value.getMD5(password));

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			logger.info("Testing environment {} with username/password {} {}", target, username, password);
			logger.debug("parameters: {}", params);

			postRequest.setEntity(new StringEntity(params));

			response = APIUtils.postRequest(postRequest, null);

			Header[] headers = response.getAllHeaders();

			Check.authorization = this.getProperty(headers, "authorization=");
			Check.jSessionId = this.getProperty(headers, "jsessionid");

			if (ConfigureEnvironment.jwtEnabled) {
				APIUtils.accessToken = this.getProperty(headers, "accessToken");
				Check.refreshToken = this.getProperty(headers, "refreshToken");
			}

		} catch (Exception e) {
			logger.error("Exception while hitting Check Api. {}", e.getMessage());
		}
		return response.getStatusLine().getStatusCode();
	}


	public void hitCheckForClientSetup(String username, String password) {
		try {
			lastLoggedInUserName = username;
			lastLoggedInUserPassword = password;

			// Read Proxy config from File and control HttpClient
			HttpClient httpClient = HttpClientBuilder.create().build();
			String host = ConfigureEnvironment.getEnvironmentProperty("Host");
			host = host.replace(host.substring(0, host.indexOf(".")), "sirion");

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(host, Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")),
					ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the post request
			HttpPost postRequest = new HttpPost("/api/v1/check");
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

			Map<String, String> parameters = new HashMap<>();
			parameters.put("j_username", username);
			parameters.put("j_password", MD5Value.getMD5(password));

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			logger.info("Testing environment {} with username/password {} {}", target, username, password);
			logger.debug("parameters: {}", params);

			postRequest.setEntity(new StringEntity(params));
			HttpResponse response = httpClient.execute(target, postRequest);

			Header[] headers = response.getAllHeaders();
			Check.authorization = this.getProperty(headers, "authorization=");
			Check.jSessionId = this.getProperty(headers, "jsessionid");

			if (ConfigureEnvironment.jwtEnabled) {
				APIUtils.accessToken = this.getProperty(headers, "accessToken");
				Check.refreshToken = this.getProperty(headers, "refreshToken");
			}

		} catch (Exception e) {
			logger.error("Exception while hitting Check Api for Client Setup User. {}", e.getMessage());
		}
	}

	public HttpResponse hitCheck(String username, String password, Boolean proxyEnabled) {
		HttpResponse response = null;

		try {
			// Read Proxy config from File and control HttpClient
			HttpClient httpClient;

			HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
			httpClient = HttpClients.custom().setProxy(proxy).build();

			//specify the host, protocol, and port
			HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"),
					Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));

			//specify the post request
			HttpPost postRequest = new HttpPost("/api/v1/check");
			postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

			if (ConfigureEnvironment.useCSRFToken)
				postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

			Map<String, String> parameters = new HashMap<>();
			parameters.put("j_username", username);
			parameters.put("password", password);
			parameters.put("j_password", MD5Value.getMD5(password));

			String params = UrlEncodedString.getUrlEncodedString(parameters);
			logger.info("Testing environment {} with username/password {} {}", target, username, password);
			logger.debug("parameters: {}", params);

			postRequest.setEntity(new StringEntity(params));

			response = httpClient.execute(target, postRequest);

			Header[] headers = response.getAllHeaders();
			Check.authorization = this.getProperty(headers, "authorization=");
			Check.jSessionId = this.getProperty(headers, "jsessionid");
		} catch (Exception e) {
			logger.error("Exception while hitting Check Api. {}", e.getMessage());
		}
		return response;
	}

	@Test()
	public void testCheck() {
		if (ConfigureEnvironment.useCookies)
			throw new SkipException("Skipping Check");

		try {
			this.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

			if (ConfigureEnvironment.jwtEnabled && ConfigureEnvironment.launchTimerTasks) {
				TimerTask accessTokenTimerTask = new JWTAccessTokenHelper();
				Timer timer = new Timer(true);

				timer.scheduleAtFixedRate(accessTokenTimerTask, 1000 * 60 * (ConfigureEnvironment.accessTokenRefreshTimeInterval - 1),
						1000 * 60 * (ConfigureEnvironment.accessTokenRefreshTimeInterval - 1));

				TimerTask refreshTokenTimerTask = new JWTRefreshTokenHelper();
				timer.scheduleAtFixedRate(refreshTokenTimerTask, 1000 * 60 * (ConfigureEnvironment.refreshTokenTimeInterval - 5),
						1000 * 60 * (ConfigureEnvironment.refreshTokenTimeInterval - 5));
			}

			if (Check.getAuthorization() == null) {
				logger.error("Authorization not set by Check API");
				System.exit(0);
			}

			if (Check.getJSessionId() == null) {
				logger.error("JSession Id not set by Check API");
				System.exit(0);
			}
		} catch (Exception e) {
			logger.error("Exception while Hitting Check API. {}", e.getMessage());
			System.exit(0);
		}
	}

	private String getProperty(Header[] headers, String propertyName) {
		String property = null;

		for (Header oneHeader : headers) {
			if (oneHeader.toString().toLowerCase().contains(propertyName.toLowerCase())) {
				String words[] = oneHeader.toString().split(";");
				for (String word : words) {
					if (word.toLowerCase().contains(propertyName.toLowerCase())) {
						String wordAuth[] = word.split("=");
						String auth[] = wordAuth[1].split("\"");

						if (auth.length > 1)
							property = auth[1];
						else
							property = wordAuth[1];
						break;
					}
				}
				break;
			}
		}
		return property;
	}
}
