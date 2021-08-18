package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class DocumentSave {

	public static String getAPIPath() {
		return "/autoExtraction/documentSave";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getContentTypeAsJsonOnlyHeader();
	}

	public static String getPayload(String docId, String clientId, String fileName, String documentData) {
		return "{\"docId\": " + docId + ",\"clientId\": " + clientId + ",\"metadata\": {\"fileName\": \"" + fileName + "\"},\"fileInfo\": {\"documentData\": \"" +
				documentData + "\",\"filePath\": " + "\"\"" + "}," + "\"status\" :" + "\"\"" +"}";
	}
}