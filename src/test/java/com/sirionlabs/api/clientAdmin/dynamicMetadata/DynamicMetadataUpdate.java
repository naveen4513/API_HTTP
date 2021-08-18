package com.sirionlabs.api.clientAdmin.dynamicMetadata;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DynamicMetadataUpdate extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(DynamicMetadataUpdate.class);

	public static String getAPIPath() {
		return "/dynamicMetadata/update";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html, */*; q=0.01");
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		headers.put("Accept-Encoding", "gzip, deflate");

		return headers;
	}

	public static HashMap<String, String> getParameters(String fieldName, String fieldLabel, int fieldOrder, int entityTypeId, int headerId, int htmlTypeId, int fieldId
			, int _listReportId) {
		HashMap<String, String> params = new HashMap<>();

		params.put("name", fieldName);
		params.put("id", String.valueOf(fieldId));
		params.put("displayName", fieldLabel);
		params.put("orderSeq", String.valueOf(fieldOrder));
		params.put("entityType.id", String.valueOf(entityTypeId));
		params.put("subHeader.id", String.valueOf(headerId));
		params.put("htmlType.id", String.valueOf(htmlTypeId));
		params.put("_listReportId", String.valueOf(_listReportId));
		params.put("_listReportExcelId", String.valueOf(_listReportId));
		params.put("_filterListReportIds", String.valueOf(_listReportId));
		params.put("searchable", "true");
		params.put("_searchable", "on");
		params.put("active", "true");
		params.put("_active", "on");
		params.put("_bulkEditable", "on");
		params.put("_sirionBI", "on");
		params.put("_autocomplete", "on");
		params.put("optionsSize", "");
		params.put("_enableListView", "on");
		params.put("_alphabeticalSort", "on");
		params.put("_enableListView", "on");
		params.put("_createClauseTag", "on");
		params.put("history", "{\"2089\":\"" + fieldName + "\",\"2090\":\"" + fieldLabel + "\",\"2091\":\"\"}");

		return params;
	}

	public static boolean updateDynamicField(String fieldName, int entityTypeId, HashMap<String, String> params) {
		try {
			AdminHelper adminObj = new AdminHelper();
			String lastUserName = Check.lastLoggedInUserName;
			String lastUserPassword = Check.lastLoggedInUserPassword;

			adminObj.loginWithClientAdminUser();

			logger.info("Hitting Update Dynamic Field API for Field Name {} and EntityTypeId {}", fieldName, entityTypeId);
			Integer responseCode = executor.postMultiPartFormData(getAPIPath(), getHeaders(), params).getResponse().getResponseCode();

			adminObj.loginWithUser(lastUserName, lastUserPassword);

			return (responseCode == 302);
		} catch (Exception e) {
			logger.error("Exception while Update Dynamic Field having Name: {} for EntityTypeId {}. {}", fieldName, entityTypeId, e.getStackTrace());
		}

		return false;
	}
}