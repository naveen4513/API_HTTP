package com.sirionlabs.api.clientAdmin.systemEmailConfigurations;

import java.util.HashMap;

public class EmailDataForCreate {

	public static String getAPIPath(int entityTypeId) {
		return "/tblsystemEmailConfigurations/emaildataForCreate?entityTypeId=" + entityTypeId;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html, */*; q=0.01");

		return headers;
	}
}