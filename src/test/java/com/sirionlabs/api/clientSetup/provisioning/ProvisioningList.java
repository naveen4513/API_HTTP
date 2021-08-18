package com.sirionlabs.api.clientSetup.provisioning;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class ProvisioningList extends TestAPIBase {

	public static String getApiPath() {
		return "/provisioning/list";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}

	public static String getProvisioningListResponseBody() {
		return executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
	}
}