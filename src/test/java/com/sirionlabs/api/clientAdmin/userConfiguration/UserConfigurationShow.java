package com.sirionlabs.api.clientAdmin.userConfiguration;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;

import java.util.HashMap;

public class UserConfigurationShow extends TestAPIBase {

	public static String getApiPath(String userId) {
		return "/tblusers/show/" + userId;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}
	public static String getUserConfigurationShowResponseBody(String userId) {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;
		new ClientSetupHelper().loginWithUserAdmin();
		String response= executor.get(getApiPath(userId), getHeaders()).getResponse().getResponseBody();
		new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
		return response;
	}
}