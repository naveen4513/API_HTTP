package com.sirionlabs.api.clientAdmin.fieldProvisioning;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class EntityTypes extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(EntityTypes.class);

	public static String getAPIPath(int supplierId) {
		return "/fieldprovisioning/entitytypes/" + supplierId;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Content-Type", "application/json; charset=utf-8");

		return headers;
	}

	public static String getFieldProvisioningEntityTypesResponse(int supplierId) {
		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		AdminHelper adminHelperObj = new AdminHelper();

		//Logging with Client Admin User
		if (!adminHelperObj.loginWithClientAdminUser()) {
			return null;
		}

		logger.info("Hitting Field Provisioning EntityTypes API.");
		String entityTypesResponse = executor.get(getAPIPath(supplierId), getHeaders()).getResponse().getResponseBody();

		//Logging back with End User
		adminHelperObj.loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);

		return entityTypesResponse;
	}

	public static Boolean hasEntityTypeId(String entityTypesResponse, int entityTypeId) {
		try {
			logger.info("Checking whether Entity Types Response has EntityTypeId {} present or not.", entityTypeId);
			JSONArray jsonArr = new JSONArray(entityTypesResponse);

			for (int i = 0; i < jsonArr.length(); i++) {
				if (jsonArr.getJSONObject(i).getInt("id") == entityTypeId) {
					return true;
				}
			}

			return false;
		} catch (Exception e) {
			logger.error("Exception while Checking if EntityTypes Response has Entity Type Id {} or not. {}", entityTypeId, e.getMessage());
			return null;
		}
	}
}