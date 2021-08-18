package com.sirionlabs.api.bulkaction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class BulkActionList {

	public static String getApiPath(int listId) {
		return "/bulkaction/list/" + listId;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultLegacyHeaders();
	}
}