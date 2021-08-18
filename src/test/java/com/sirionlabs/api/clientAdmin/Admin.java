package com.sirionlabs.api.clientAdmin;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class Admin extends TestAPIBase {

	private static String apiPath = "/admin";

	public static String getApiPath() {
		return apiPath;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String getAdminResponseBody() {
		return executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
	}
}