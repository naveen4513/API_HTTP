package com.sirionlabs.api.clientAdmin.dynamicMetadata;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;


public class DynamicMetadataCreate extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(DynamicMetadataCreate.class);

	public static String getAPIPath() {
		return "/dynamicMetadata/create";
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Accept-Encoding", "gzip, deflate");

		return headers;
	}

	public static HashMap<String, String> getParameters(String fieldName, String fieldLabel, int fieldOrder, int entityTypeId, int headerId, int htmlTypeId, int listReportId
			, int _listReportId) {
		HashMap<String, String> params = new HashMap<>();

		params.put("name", fieldName);
		params.put("displayName", fieldLabel);
		params.put("orderSeq", String.valueOf(fieldOrder));
		params.put("entityType.id", String.valueOf(entityTypeId));
		params.put("subHeader.id", String.valueOf(headerId));
		params.put("htmlType.id", String.valueOf(htmlTypeId));

		if (listReportId != 1) {
			params.put("listReportId", String.valueOf(listReportId));
			params.put("listReportExcelId", String.valueOf(listReportId));
			params.put("filterListReportIds", String.valueOf(listReportId));
		}

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
		params.put("_enableHtmlParsing", "on");
		params.put("_createClauseTag", "on");
		params.put("masterList.id", "");

		return params;
	}

	public static boolean createDynamicField(String fieldName, String fieldLabel, int fieldOrder, int entityTypeId, int headerId, int htmlTypeId, int listReportId,
	                                         int _listReportId) {
		try {
			AdminHelper adminObj = new AdminHelper();
			String lastUserName = Check.lastLoggedInUserName;
			String lastUserPassword = Check.lastLoggedInUserPassword;

			adminObj.loginWithClientAdminUser();

			HashMap<String, String> params = getParameters(fieldName, fieldLabel, fieldOrder, entityTypeId, headerId, htmlTypeId, listReportId, _listReportId);

			logger.info("Hitting Create Dynamic Field API for Field Name {} and EntityTypeId {}", fieldName, entityTypeId);
			Integer responseCode = executor.postMultiPartFormData(getAPIPath(), getHeaders(), params).getResponse().getResponseCode();

			adminObj.loginWithUser(lastUserName, lastUserPassword);

			return (responseCode == 302);
		} catch (Exception e) {
			logger.error("Exception while Creating Dynamic Field having Label: {} for EntityTypeId {}. {}", fieldLabel, entityTypeId, e.getStackTrace());
		}

		return false;
	}

	public static int getFieldId(String fieldName) {
		try {
			PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
			String query = "select id from entity_field where alias ='" + fieldName.trim() + "'";
			List<List<String>> results = sqlObj.doSelect(query);

			if (!results.isEmpty()) {
				return Integer.parseInt(results.get(0).get(0));
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Id for Field {}. {}", fieldName, e.getStackTrace());
		}
		return -1;
	}

	public static boolean deleteDynamicField(String fieldName, int fieldId) {
		try {
			PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
			String query = "select id from entity_field where alias ='" + fieldName.trim() + "'";
			List<List<String>> results = sqlObj.doSelect(query);

			if (!results.isEmpty()) {
				query = "delete from client_field_provisioning_data where field_id =" + fieldId;
				sqlObj.deleteDBEntry(query);

				query = "delete from excel_columns where entity_field_id=" + fieldId;
				sqlObj.deleteDBEntry(query);

				query = "delete from  entity_client_field  where field_id=" + fieldId;
				sqlObj.deleteDBEntry(query);

				query = "delete from link_fields_groups where field_id=" + fieldId;
				sqlObj.deleteDBEntry(query);

				query = "delete from request_field_mapping where field_id=" + fieldId;
				sqlObj.deleteDBEntry(query);

				query = "delete from client_field_provisioningv2_data where field_id = (select id from entity_field where alias = '" + fieldName.trim() + "')";
				sqlObj.deleteDBEntry(query);

				query = "delete from entity_field where alias ='" + fieldName.trim() + "'";
				sqlObj.deleteDBEntry(query);
			}

			sqlObj.closeConnection();
			return true;
		} catch (Exception e) {
			logger.error("Exception while Deleting Dynamic Field {}. {}", fieldName, e.getStackTrace());
		}

		return false;
	}
}
