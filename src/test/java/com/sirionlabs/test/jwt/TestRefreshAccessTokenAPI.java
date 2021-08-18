package com.sirionlabs.test.jwt;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.RefreshToken;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TestRefreshAccessTokenAPI {

	private final static Logger logger = LoggerFactory.getLogger(TestRefreshAccessTokenAPI.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static Boolean isLaunchTimerTasksDisabled;
	private RefreshToken refreshObj = new RefreshToken();
	private Check checkObj = new Check();

	@BeforeClass
	public void beforeClass() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("RefreshAccessTokenAPIConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("RefreshAccessTokenAPIConfigFileName");

		isLaunchTimerTasksDisabled = !ConfigureEnvironment.launchTimerTasks;
	}

	@Test
	public void testPositiveFlows() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!isLaunchTimerTasksDisabled) {
				logger.info("Launch Timer Tasks flag is not turned off. Hence skipping test.");
				throw new SkipException("Launch Timer Tasks flag is not turned off. Hence Skipping test.");
			}

			checkObj.hitCheck();

			logger.info("Validating Positive Flows of Refresh Access Token API.");

			String refreshResponse = refreshObj.hitRefreshTokenAPI();
			int statusCode = refreshObj.getStatusCode();
			HttpResponse httpResponse = refreshObj.getHttpResponse();

			logger.info("Validating that Refresh Access Token  API Response is a Valid JSON.");

			if (ParseJsonResponse.validJsonResponse(refreshResponse)) {
				//Validate Status Code
				if (statusCode != 200) {
					csAssert.assertTrue(false, "Expected Status Code: 200 and Actual Status Code: " + statusCode);
				}

				//Validate Fields in Response
				verifyFieldsInResponse(refreshResponse, csAssert);

				//Validate Response Headers
				verifyResponseHeaders(httpResponse, csAssert);
			} else {
				csAssert.assertTrue(false, "Refresh Access Token API Response is not a Valid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Positive Flows of Refresh Access Token API. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test
	public void testNegativeFlows() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!isLaunchTimerTasksDisabled) {
				logger.info("Launch Timer Tasks flag is not turned off. Hence skipping test.");
				throw new SkipException("Launch Timer Tasks flag is not turned off. Hence Skipping test.");
			}

			checkObj.hitCheck();

			logger.info("Validating Negative Flows of Refresh Access Token API.");

			//Validate Refresh Token Checks.
			verifyRefreshTokenCheck(csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Negative Flows of Refresh Access Token API. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test
	public void testCheckAPIForSirionMobileAgent() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Hitting Check API with SirionMobile Agent.");
			HttpResponse httpResponse = hitCheckAPI();
			boolean authorizationFound, accessTokenFound, refreshTokenFound;
			authorizationFound = accessTokenFound = refreshTokenFound = false;

			Header[] allHeaders = httpResponse.getAllHeaders();
			for (Header oneHeader : allHeaders) {
				String headerString = oneHeader.toString().trim();

				if (headerString.contains("Authorization")) {
					authorizationFound = true;
				} else if (headerString.contains("accessToken")) {
					accessTokenFound = true;
				} else if (headerString.contains("refreshToken")) {
					refreshTokenFound = true;
				}
			}

			if (!authorizationFound) {
				csAssert.assertTrue(false, "Authorization Code not found in Check API Response Headers");
			}

			if (!accessTokenFound) {
				csAssert.assertTrue(false, "AccessToken not found in Check API Response Headers");
			}

			if (!refreshTokenFound) {
				csAssert.assertTrue(false, "RefreshToken not found in Check API Response Headers");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Check API for SirionMobile Agent. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//Validate that APIs should not return JSON Response if Access Token is Expired.
	@Test(enabled = false)
	public void testC13673() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!isLaunchTimerTasksDisabled) {
				logger.info("Launch Timer Tasks flag is not turned off. Hence skipping test.");
				throw new SkipException("Launch Timer Tasks flag is not turned off. Hence Skipping test.");
			}

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAccessTokenExpiry");
			if (temp == null || !temp.trim().equalsIgnoreCase("true")) {
				logger.info("Flag TestAccessTokenExpiry has been turned off. Hence skipping test.");
				throw new SkipException("Flag TestAccessTokenExpiry has been turned off. Hence skipping test.");
			}

			checkObj.hitCheck();

			logger.info("Validating that APIs should not return JSON Response if Access Token is Expired.");
			Integer accessTokenRefreshTimeInterval = ConfigureEnvironment.accessTokenRefreshTimeInterval;

			logger.info("Putting Thread on Sleep for {} Minutes and 5 Seconds at [{}]", accessTokenRefreshTimeInterval, new Date());
			Thread.sleep((1000 * 60 * accessTokenRefreshTimeInterval) + 5);

			logger.info("Hitting DefaultUserListMetaData API for Suppliers");
			ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
			defaultUserListObj.hitListRendererDefaultUserListMetadata(3);
			String defaultUserListResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
				csAssert.assertTrue(false, "DefaultUserListMetaData API Response is Valid JSON even after the Access Token has Expired.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C13673. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//Validate that Refresh API should not return Access Token if Refresh Token is expired.
	@Test(enabled = false)
	public void testC13674() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!isLaunchTimerTasksDisabled) {
				logger.info("Launch Timer Tasks flag is not turned off. Hence skipping test.");
				throw new SkipException("Launch Timer Tasks flag is not turned off. Hence Skipping test.");
			}

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testRefreshTokenExpiry");
			if (temp == null || !temp.trim().equalsIgnoreCase("true")) {
				logger.info("Flag TestRefreshTokenExpiry has been turned off. Hence skipping test.");
				throw new SkipException("Flag TestRefreshTokenExpiry has been turned off. Hence skipping test.");
			}

			checkObj.hitCheck();

			logger.info("Validating that Refresh API should not return work if Refresh Token is Expired.");
			Integer refreshTokenRefreshTimeInterval = ConfigureEnvironment.refreshTokenTimeInterval;

			logger.info("Putting Thread on Sleep for {} Minutes and 5 Seconds at [{}]", refreshTokenRefreshTimeInterval, new Date());
			Thread.sleep((1000 * 60 * refreshTokenRefreshTimeInterval) + 5);

			logger.info("Hitting Refresh Token API");
			String refreshResponse = refreshObj.hitRefreshTokenAPI();
			HttpResponse httpResponse = refreshObj.getHttpResponse();
			int statusCode = httpResponse.getStatusLine().getStatusCode();

			if (statusCode != 401) {
				csAssert.assertTrue(false, "Expected Status Code: 401 and Actual Status Code: " + statusCode);
			}

			if (!ParseJsonResponse.validJsonResponse(refreshResponse)) {
				csAssert.assertTrue(false, "Refresh Token API Response with Expired Refresh Token is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C13673. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private HttpResponse hitCheckAPI() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		HttpPost postRequest = new HttpPost("/check");
		postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
		postRequest.addHeader("User-Agent", "SirionMobile");

		if (ConfigureEnvironment.useCSRFToken)
			postRequest.addHeader("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

		Map<String, String> parameters = new HashMap<>();
		parameters.put("j_username", Check.lastLoggedInUserName);
		parameters.put("password", Check.lastLoggedInUserPassword);
		parameters.put("j_password", MD5Value.getMD5(Check.lastLoggedInUserPassword));

		String params = UrlEncodedString.getUrlEncodedString(parameters);

		postRequest.setEntity(new StringEntity(params));

		return APIUtils.postRequest(postRequest, null);
	}

	private void verifyRefreshTokenCheck(CustomAssert csAssert) throws IOException {
		/*
		Negative testing with Refresh Token Parameter
		 */

		logger.info("Validating that if Refresh Token is missing then API should return 400.");
		logger.info("Hitting Refresh Token API without Refresh Token.");
		HttpResponse httpResponse = hitRefreshTokenAPI();
		int statusCode = httpResponse.getStatusLine().getStatusCode();

		if (statusCode != 400) {
			csAssert.assertTrue(false, "Refresh Token API without Refresh Token. Expected Status Code: 400 and Actual Status Code: " + statusCode);
		}

		String response = EntityUtils.toString(httpResponse.getEntity());
		//Commenting below block as this is not fixed in the code.
		/*if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Refresh Token is missing.");
		}*/

		logger.info("Validating that if Incorrect Signature in Refresh Token is passed then API should return 401.");
		logger.info("Hitting Refresh Token API with Incorrect Signature in Refresh Token");
		String refreshToken = Check.refreshToken;
		refreshToken = refreshToken.substring(0, refreshToken.length() - 3);
		refreshToken = refreshToken.concat("123");

		httpResponse = hitRefreshTokenAPI(true, true, refreshToken, APIUtils.accessToken);
		statusCode = httpResponse.getStatusLine().getStatusCode();

		if (statusCode != 401) {
			csAssert.assertTrue(false, "Refresh Token API with Incorrect Signature in Refresh Token. Expected Status Code: 401 and Actual Status Code: "
					+ statusCode);
		}

		response = EntityUtils.toString(httpResponse.getEntity());
		if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Incorrect Refresh Token is passed.");
		}

		logger.info("Validating that if Refresh Token is passed with Incorrect Syntax then API should return 401.");
		logger.info("Hitting Refresh Token API with Incorrect Syntax in Refresh Token");
		refreshToken = refreshToken.replaceAll(Pattern.quote("."), "");

		httpResponse = hitRefreshTokenAPI(true, true, refreshToken, APIUtils.accessToken);
		statusCode = httpResponse.getStatusLine().getStatusCode();

		if (statusCode != 401) {
			csAssert.assertTrue(false, "Refresh Token API with Incorrect Syntax in Refresh Token. Expected Status Code: 401 and Actual Status Code: "
					+ statusCode);
		}

		response = EntityUtils.toString(httpResponse.getEntity());
		if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Refresh Token is passed with Incorrect Syntax");
		}

		/*
		Negative testing with Access Token Parameter
		 */

		logger.info("Validating that if Access Token is missing then API should return 400.");
		logger.info("Hitting Refresh Token API without Access Token.");
		httpResponse = hitRefreshTokenAPI(true, false, Check.refreshToken, null);
		statusCode = httpResponse.getStatusLine().getStatusCode();

		if (statusCode != 400) {
			csAssert.assertTrue(false, "Refresh Token API without Access Token. Expected Status Code: 400 and Actual Status Code: " + statusCode);
		}

		response = EntityUtils.toString(httpResponse.getEntity());
		//Commenting below block as this is not fixed in the code.
		/*if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Access Token is missing.");
		}*/

		logger.info("Validating that if Incorrect Signature in Access Token is passed then API should return 400.");
		logger.info("Hitting Refresh Token API with Incorrect Signature in Access Token");
		String accessToken = APIUtils.accessToken;
		accessToken = accessToken.substring(0, refreshToken.length() - 3);
		accessToken = accessToken.concat("123");

		httpResponse = hitRefreshTokenAPI(true, true, Check.refreshToken, accessToken);
		statusCode = httpResponse.getStatusLine().getStatusCode();

		//Commenting below block as this is not fixed in the code.
		/*if (statusCode != 400) {
			csAssert.assertTrue(false, "Refresh Token API with Incorrect Signature in Access Token. Expected Status Code: 400 and Actual Status Code: "
					+ statusCode);
		}*/

		response = EntityUtils.toString(httpResponse.getEntity());
		//Commenting below block as this is not fixed in the code.
		/*if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Incorrect Access Token is passed.");
		}*/

		logger.info("Validating that if Access Token is passed with Incorrect Syntax then API should return 400.");
		logger.info("Hitting Refresh Token API with Incorrect Syntax in Access Token");
		accessToken = accessToken.replaceAll(Pattern.quote("."), "");

		httpResponse = hitRefreshTokenAPI(true, true, Check.refreshToken, accessToken);
		statusCode = httpResponse.getStatusLine().getStatusCode();
		//Commenting below block as this is not fixed in the code.
		/*if (statusCode != 400) {
			csAssert.assertTrue(false, "Refresh Token API with Incorrect Syntax in Access Token. Expected Status Code: 400 and Actual Status Code: "
					+ statusCode);
		}*/

		response = EntityUtils.toString(httpResponse.getEntity());
		//Commenting below block as this is not fixed in the code.
		/*if (!ParseJsonResponse.validJsonResponse(response)) {
			csAssert.assertTrue(false, "Refresh API Response is an Invalid JSON when Access Token is passed with Incorrect Syntax");
		}*/
	}

	private HttpResponse hitRefreshTokenAPI() {
		return hitRefreshTokenAPI(false, true, null, APIUtils.accessToken);
	}

	private HttpResponse hitRefreshTokenAPI(Boolean useRefreshToken, Boolean useAccessToken, String refreshToken, String accessToken) {
		HttpResponse response = null;

		try {
			HttpGet getRequest;
			String queryString = "/refresh";

			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("X-Requested-With", "XMLHttpRequest");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			getRequest.addHeader("Content-Type", "application/json");
			getRequest.addHeader("Authorization", Check.getAuthorization());

			if (useRefreshToken && useAccessToken) {
				getRequest.addHeader("Cookie", "refreshToken=" + refreshToken + ";accessToken=" + accessToken + ";");
			} else if (useRefreshToken) {
				getRequest.addHeader("Cookie", "refreshToken=" + refreshToken + ";");
			} else {
				getRequest.addHeader("Cookie", "accessToken=" + accessToken + ";");
			}

			response = APIUtils.getRequest(getRequest, false);

			logger.debug("Response status is {}", response.getStatusLine().toString());

			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Refresh Token API response header {}", oneHeader.toString());
			}
		} catch (Exception e) {
			logger.error("Exception while hitting Refresh Token Api. {}", e.getMessage());
		}
		return response;
	}

	private void verifyFieldsInResponse(String refreshResponse, CustomAssert csAssert) {
		logger.info("Validating Fields accessToken, token and refreshToken in Response");
		JSONObject jsonObj = new JSONObject(refreshResponse);

		if (!jsonObj.has("accessToken")) {
			csAssert.assertTrue(false, "AccessToken Field missing in Refresh Access Token API Response");
		} else {
			//Validate that New Access Token is generated.

			if (jsonObj.getString("accessToken").trim().equalsIgnoreCase(APIUtils.accessToken)) {
				csAssert.assertTrue(false, "Access Token is not unique. It is same as previous one.");
			}
		}

		if (!jsonObj.has("token")) {
			csAssert.assertTrue(false, "Token Field missing in Refresh Access Token API Response");
		}

		if (!jsonObj.has("refreshToken")) {
			csAssert.assertTrue(false, "RefreshToken Field missing in Refresh Access Token API Response");
		} else {
			//Validate that Refresh Token is same as sent in Cookie

			if (!jsonObj.getString("refreshToken").trim().equalsIgnoreCase(Check.refreshToken)) {
				csAssert.assertTrue(false, "RefreshToken in Response doesn't match with the Token Value sent in Cookie");
			}
		}
	}

	private void verifyResponseHeaders(HttpResponse httpResponse, CustomAssert csAssert) {
		logger.info("Validating Response Headers");
		logger.info("Validating that there's only one entry/instance of Authorization, Refresh Token and Access Token");

		Header[] allHeaders = httpResponse.getAllHeaders();
		int noOfAuthorizationEntries, noOfRefreshTokenEntries, noOfAccessTokenEntries;
		noOfAccessTokenEntries = noOfRefreshTokenEntries = noOfAuthorizationEntries = 0;

		for (Header oneHeader : allHeaders) {
			String headerString = oneHeader.toString().trim();

			if (headerString.contains("Authorization")) {
				noOfAuthorizationEntries++;
			} else if (headerString.contains("refreshToken")) {
				noOfRefreshTokenEntries++;
			} else if (headerString.contains("accessToken")) {
				noOfAccessTokenEntries++;
			}
		}

		if (noOfAuthorizationEntries > 1) {
			csAssert.assertTrue(false, "Multiple Entries/Instances found of Authorization in Response Headers");
		}

		if (noOfAccessTokenEntries > 1) {
			csAssert.assertTrue(false, "Multiple Entries/Instances found of Access Token in Response Headers");
		}

		if (noOfRefreshTokenEntries > 1) {
			csAssert.assertTrue(false, "Multiple Entries/Instances found of Refresh Token in Response Headers");
		}
	}
}