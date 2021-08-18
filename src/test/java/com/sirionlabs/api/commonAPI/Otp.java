package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class Otp {

	private static String apiPath = "/otp";

	public static String getAPIPath() {
		return apiPath;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = ApiHeaders.getDefaultLegacyHeaders();
		headers.put("Accept", "*/*");
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		headers.put("Cookie", "Authorization=" + Check.getAuthorization() + ";targetUrl=" + ConfigureEnvironment.getCompleteHostUrl() + "/welcome");

		return headers;
	}

	public static String getPayload(String passCode, String userId, Integer clientId) {
		return "passcode=" + passCode + "&userId=" + userId + "&authenticationTypeId=1&totalAllowedAttempts=10&clientId=" +clientId;
	}
}