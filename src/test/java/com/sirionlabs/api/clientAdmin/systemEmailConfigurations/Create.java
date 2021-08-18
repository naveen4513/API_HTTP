package com.sirionlabs.api.clientAdmin.systemEmailConfigurations;

import java.util.HashMap;

public class Create {

	public static String getAPIPath() {
		return "/tblsystemEmailConfigurations/create";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Accept-Encoding", "gzip, deflate");

		return headers;
	}

	public static HashMap<String, String> getParameters(int entityTypeId, String emailName, String subjectDataString, String bodyDataString, String bulkSubjectDataString,
	                                                    String bulkBodyDataString) {
		HashMap<String, String> params = new HashMap<>();

		params.put("entityTypeId", String.valueOf(entityTypeId));
		params.put("name", emailName);
		params.put("subjectDataString", subjectDataString);
		params.put("bodyDataString", bodyDataString);
		params.put("bodyDataString_textarea", "true");
		params.put("bulkSubjectDataString", bulkSubjectDataString);
		params.put("bulkBodyDataString", bulkBodyDataString);
		params.put("bulkBodyDataString_textarea", "true");

		return params;
	}
}