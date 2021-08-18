package com.sirionlabs.api.clientAdmin.supplierAccessVendorLevel;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;

import java.util.HashMap;
import java.util.Map;

public class AdminVendorSupplierAccess extends APIUtils {

	public static String getApiPath(String vendorId, String vendorName) {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("vendorId", vendorId);
		parameters.put("vendorName", vendorName);

		String params = UrlEncodedString.getUrlEncodedString(parameters);
		return "/tblsupplieraccessvendorlevel/vendorsupplieraccess?" + params;
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}
}