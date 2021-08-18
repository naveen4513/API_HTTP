package com.sirionlabs.helper.clientAdmin;

import com.sirionlabs.api.clientAdmin.field.Provisioning;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class FieldProvisioningHelper {

	private final static Logger logger = LoggerFactory.getLogger(FieldProvisioningHelper.class);
	private Provisioning fieldProObj = new Provisioning();

	public List<String> getAllHiddenFieldsOfSupplierTypeUser(int entityTypeId) {
		try {
			logger.info("Hitting Field Provisioning GET API for Supplier Type User and EntityTypeId {}", entityTypeId);
			String fieldProvisioningResponse = fieldProObj.hitFieldProvisioning(4, entityTypeId);

			if (ParseJsonResponse.validJsonResponse(fieldProvisioningResponse)) {
				return getAllHiddenFields(fieldProvisioningResponse);
			} else {
				logger.error("Field Provisioning Response for Supplier Type User and EntityTypeId {} is an Invalid JSON.", entityTypeId);
			}

			return null;
		} catch (Exception e) {
			logger.error("Exception while Getting all Hidden Fields of Supplier Type User. {}", e.getMessage());
			return null;
		}
	}

	public List<String> getAllHiddenFields(String fieldProvisioningResponse) {
		List<String> allHiddenFields = new ArrayList<>();

		try {
			logger.info("Getting All Hidden Fields from Field Provisioning Response.");
			JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			for (int i = 0; i < jsonArr.length(); i++) {
				JSONObject fieldJsonObj = jsonArr.getJSONObject(i);

				if (fieldJsonObj.getBoolean("hidden")) {
					allHiddenFields.add(fieldJsonObj.getString("name").trim());
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Hidden Fields from Field Provisioning Response. {}", e.getMessage());
			return null;
		}
		return allHiddenFields;
	}
}