package com.sirionlabs.api.bulk;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.HashMap;

public class BulkFetch extends TestAPIBase {

	private static String entityIdMappingFilePath = "src\\test\\resources\\CommonConfigFiles";
	private static String entityIdMappingFileName = "EntityIdMapping.cfg";

	public static String getApiPath(String entityName) {

		String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath,entityIdMappingFileName,entityName,"url_name");
		return "/" + urlName + "/bulk/fetchAllIds/";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultLegacyHeaders();
	}

	public static String getBulkFetchResponse(String entityName,String payload) {
		return executor.post(getApiPath(entityName), getHeaders(),payload).getResponse().getResponseBody();
	}


}