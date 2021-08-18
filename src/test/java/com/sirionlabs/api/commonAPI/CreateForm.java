package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CreateForm extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(CreateForm.class);

	public static String getApiPath(int clientId, String entityName) {
		String searchUrl = ConfigureConstantFields.getSearchUrlForEntity(entityName);
		return "/" + searchUrl + "/createForm/rest?clientId=" + clientId + "&ajax=true&version=2.0";
	}

	public static HashMap<String, String> getHeaders() {
		return ApiHeaders.getDefaultLegacyHeaders();
	}

	public static String getCreateFormV2Response(int clientId, String entityName) {
		logger.info("Hitting CreateForm V2 API for Client Id {} and Entity {}", clientId, entityName);
		return executor.get(getApiPath(clientId, entityName), getHeaders()).getResponse().getResponseBody();
	}

	private static List<String> setAllRequiredFieldNames(List<String> allRequiredFieldNames, String createFormV2Response) {
		try {
			JSONObject jsonObj = new JSONObject(createFormV2Response);
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			for (int i = 0; i < jsonArr.length(); i++) {
				jsonObj = new JSONObject(jsonArr.getJSONObject(i).toString());
				if (jsonObj.has("fields"))
					allRequiredFieldNames = setAllRequiredFieldNames(allRequiredFieldNames, jsonArr.get(i).toString());
				else {
					if (jsonObj.has("validations")) {
						String fieldName = jsonObj.getString("name");
						JSONArray validationsArray = jsonObj.getJSONArray("validations");

						for (int j = 0; j < validationsArray.length(); j++) {
							JSONObject validationsObj = validationsArray.getJSONObject(j);

							if (validationsObj.has("type") && validationsObj.getString("type").equalsIgnoreCase("required")) {
								allRequiredFieldNames.add(fieldName);
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Setting All Required Fields. " + e.getMessage());
		}

		return allRequiredFieldNames;
	}

	public static List<String> getAllRequiredFieldNames(String createFormV2Response) {
		List<String> allRequiredFieldNames = new ArrayList<>();

		try {
			JSONObject jsonObj = new JSONObject(createFormV2Response);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			allRequiredFieldNames = setAllRequiredFieldNames(allRequiredFieldNames, jsonArr.get(0).toString());
		} catch (Exception e) {
			logger.error("Exception while Getting All Required Fields. {}", e.getMessage());
			return null;
		}

		return allRequiredFieldNames;
	}
}