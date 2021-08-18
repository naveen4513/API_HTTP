package com.sirionlabs.api.documentViewer;

import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class DocumentViewerShow extends TestAPIBase {

	public static String getApiPath(int documentId) {
		return "/documentviewer/show/" + documentId;
	}

	public static String getApiPathDocViewStream(String docViewer) {
		return docViewer;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultLegacyHeaders();
	}

	public static String getDocumentViewerShowResponse(int documentId) {
		return executor.get(getApiPath(documentId), getHeaders()).getResponse().getResponseBody();
	}

	public static APIValidator getDocumentViewerResponse(String apiLink) {
		return executor.get(getApiPathDocViewStream(apiLink), getHeaders());
	}
}