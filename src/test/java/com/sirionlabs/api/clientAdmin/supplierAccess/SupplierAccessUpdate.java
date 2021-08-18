package com.sirionlabs.api.clientAdmin.supplierAccess;

import java.util.HashMap;

public class SupplierAccessUpdate {

	public static String getApiPath() {
		return "/tblsupplieraccess/update";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html, */*; q=0.01");

		return headers;
	}
}