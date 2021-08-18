package com.sirionlabs.api.clientAdmin.supplierAccessVendorLevel;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.utils.commonUtils.APIUtils;

import java.util.HashMap;

public class AdminSupplierAccessVendorLevelCreate extends APIUtils {

	public static String getApiPath() {
		return "/tblsupplieraccessvendorlevel/create";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
	}
}