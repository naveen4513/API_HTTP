package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class FirstOtp {

	public static String getAPIPath(Integer userId) {
		return "/firstotp?authType=1&userId=" + userId+"&?&emailMobileAuthenticatorConfig=false";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = ApiHeaders.getDefaultAcceptEncodingHeader();
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

		return headers;
	}
}