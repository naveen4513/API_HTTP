package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class UserConfigurationList extends TestAPIBase {

	private static String apiPath = "/tblusers/list";

	public static String getApiPath() {
		return apiPath;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String getUserConfigurationListResponseBody() {
		return executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
	}
}