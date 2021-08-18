package com.sirionlabs.helper.search;

import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataSearchHelper {

	private final static Logger logger = LoggerFactory.getLogger(SearchAttachmentHelper.class);

	public List<Map<String, String>> getAllOptionsOfField(String searchResponse, String fieldShowPageObjectName) {
		List<Map<String, String>> allOptions = new ArrayList<>();

		try {
			if (ParseJsonResponse.validJsonResponse(searchResponse)) {
				JSONObject jsonObj = new JSONObject(searchResponse).getJSONObject("body").getJSONObject("data").getJSONObject(fieldShowPageObjectName).getJSONObject("options");
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				for (int i = 0; i < jsonArr.length(); i++) {
					JSONObject tempObj = jsonArr.getJSONObject(i);

					Map<String, String> optionMap = new HashMap<>();
					optionMap.put("name", tempObj.getString("name"));
					optionMap.put("id", String.valueOf(tempObj.getInt("id")));

					if (tempObj.has("parentName"))
						optionMap.put("parentName", tempObj.getString("parentName"));

					allOptions.add(optionMap);
				}
			} else {
				logger.error("Search Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Options of Field [{}]. {}", fieldShowPageObjectName, e.getStackTrace());
		}

		return allOptions;
	}

	public List<Map<String, String>> getAllSupplierTypeUsersFromMetadataSearchResponse(String searchResponse) {
		List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

		try {
			if (ParseJsonResponse.validJsonResponse(searchResponse)) {
				JSONObject jsonObj = new JSONObject(searchResponse);
				jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("options");

				Boolean isAutoComplete = jsonObj.getBoolean("autoComplete");

				if (isAutoComplete) {
					OptionsHelper optionObj = new OptionsHelper();
					String optionResponse = optionObj.hitOptionsForAllStakeholders();
					allSupplierTypeUsers = optionObj.getAllSupplierTypeUsersFromOptionsResponse(optionResponse);

					return allSupplierTypeUsers;
				}

				JSONArray jsonArr = jsonObj.getJSONArray("data");

				for (int i = 0; i < jsonArr.length(); i++) {
					int idType = jsonArr.getJSONObject(i).getInt("idType");

					if (idType == 4) {
						Map<String, String> userMap = new HashMap<>();

						String userName = jsonArr.getJSONObject(i).getString("name");
						int userId = jsonArr.getJSONObject(i).getInt("id");

						userMap.put("name", userName);
						userMap.put("id", String.valueOf(userId));

						allSupplierTypeUsers.add(userMap);
					}
				}

				return allSupplierTypeUsers;
			} else {
				logger.error("Search Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Supplier Type Users from Search Response. {}", e.getMessage());
		}

		return null;
	}
}