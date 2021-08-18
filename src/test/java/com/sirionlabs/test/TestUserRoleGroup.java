package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.util.List;

/**
 * Created by pradeep on 5/7/17.
 */
public class TestUserRoleGroup {
	private final static Logger logger = LoggerFactory.getLogger(TestUserRoleGroup.class);

	CustomAssert csAssertion  = new CustomAssert();
	String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
	String getEntityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
	String userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
	String userRoleGroupFileName = ConfigureConstantFields.getConstantFieldsProperty("UserRoleGroupConfigFileName");
	String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
	String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

	ListRendererListData listDataObj =new ListRendererListData();


	public void testUserRoleGroup(String entitySection) {
		try {
			List<String> allEntitySection = ParseConfigFile.getAllSectionNames(userRoleGroupFilePath, userRoleGroupFileName);
			//List<String> entityRequestPayloadProperties = ParseConfigFile.getAllPropertiesOfSection(userRoleGroupFilePath, userRoleGroupFileName, "request_payload");

			//for (String entitySection : allEntitySection) {
				// Iterate over each entitySection
				/*if (entitySection.equalsIgnoreCase("request_payload")) {
					continue;
				}*/
				List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(userRoleGroupFilePath, userRoleGroupFileName, entitySection);
				for (String property : allProperties) {
					Boolean testStatus = false;
					Boolean propertyValue = false;
					switch (property.trim().toLowerCase()) {
						case "listing":
							logger.info("Call the listing for Entity [ {} ]", entitySection);
							testStatus = verifyEntityListing(entitySection);
							propertyValue = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, entitySection, property));
							csAssertion.assertNotEquals(testStatus, propertyValue, "Failed for entitySection [ " + entitySection + " ] , The test status for the property [ " + property + " ] is [ " + !testStatus + " ] but required is [" + propertyValue + " ]\n\n");
							break;
						case "show":
							logger.info("Call the showpage for Entity [ {} ]", entitySection);
							testStatus = verifyShowListingForAllRecords(entitySection);
							propertyValue = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, entitySection, property));
							csAssertion.assertNotEquals(testStatus, propertyValue, "Failed for entitySection [ " + entitySection + " ] , The test status for the property [ " + property + " ] is [ " + !testStatus + " ] but required is [" + propertyValue + " ]\n\n");
							break;
						case "edit":
							logger.info("Call the updatepage for Entity [ {} ]",entitySection);
							testStatus = verifyEditListingForAllRecords(entitySection);
							propertyValue = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, entitySection, property));
							csAssertion.assertNotEquals(testStatus, propertyValue, "Failed for entitySection [ " + entitySection + " ] , The test status for the property [ " + property + " ] is [ " + !testStatus + " ] but required is [" + propertyValue + " ]\n\n");
							break;
						default:
							logger.warn("No Test is enabled for the property [ {} ]", property);
							break;
					}
				}
			//}
		} catch (Exception e) {
			logger.error("Exception while running testUserRoleGroup [ {} ]", e.getMessage());
		}
		csAssertion.assertAll();
	}

	public Boolean verifyEntityListing(String entityName) {
		Boolean testStatus = false;

		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String pageSize = ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, "request_payload", "pagesize");
			String offset = ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, "request_payload", "offset");
			String orderBy = ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, "request_payload", "orderby");
			String orderDirection = ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, "request_payload",
					"orderdirection");
			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "entity_url_id"));
			String requestPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
					offset + ",\"size\":" + pageSize + ",\"orderByColumnName\":\"" + orderBy + "\"," +
					"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";
			listDataObj.hitListRendererListData(urlId, false, requestPayload, null);
			String listDataResponse = listDataObj.getListDataJsonStr();
			testStatus = APIUtils.isPermissionDeniedInResponse(listDataResponse);
		} catch (Exception e) {
			logger.error("Exception while processing verifyEntityListing [ {} ]", e.getMessage());
		}
		return testStatus;
	}

	/**
	 * @param entityName
	 * @return boolean value of show entity permission
	 */
	private boolean verifyShowListingForAllRecords(String entityName) {
		boolean testStatus = false;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String listDataResponse = listDataResponse(entityTypeId, entityName);
			csAssertion.assertTrue(APIUtils.validJsonResponse(listDataResponse), "Response is not a valid JSON for entity Name [ " + entityName + " ]");

			JSONObject listDataResponseObj = new JSONObject(listDataResponse);
			int noOfRecords = listDataResponseObj.getJSONArray("data").length();
			logger.info("--------- Number of records are [ {} ] for the entity [ {} ] -----------------", noOfRecords, entityName);

			if (noOfRecords > 0) {
				listDataObj.setListData(listDataResponse);
				int columnId = listDataObj.getColumnIdFromColumnName("id");
				List<Integer> allDBid = listDataObj.getAllRecordDbId(columnId, listDataResponse);

				for (Integer dbId : allDBid) {
					boolean errorResponce = validateShowPermissionForId(entityTypeId, dbId);
					if (errorResponce) {
						testStatus = true;
						csAssertion.assertTrue(false, "The dbId [ " + dbId + " ] for entity Name [ " + entityName + " ] is having ErrorMessage ");
					}
				}
			} else
				logger.warn("no records found for entity : [ {} ]", entityName);
		} catch (Exception e) {
			logger.error("Exception while processing verifyShowListing [ {} ]", e.getMessage());
		}
		return testStatus;
	}

	public Boolean validateShowPermissionForId(int entityTypeId, int dbId) {
		Boolean errorResponceStatus = true;
		try {
			logger.info(" Checking the show permission for entityTypeId [ {} ] , and dbId [ {} ]", entityTypeId, dbId);
			Show showObj = new Show();
			showObj.hitShow(entityTypeId, dbId);
			String showJsonStr = showObj.getShowJsonStr();
			errorResponceStatus = APIUtils.isPermissionDeniedInResponse(showJsonStr);
			logger.info(" The test status for entity [ {} ] , and DBId [ {} ] , if the responce contains permission error [ {} ]", entityTypeId, dbId, errorResponceStatus);
		} catch (Exception e) {
			logger.error("Exception while processing  validateShowPermissionForId [ {} ]", e.getMessage());
		}
		return errorResponceStatus;
	}

	public String listDataResponse(int entityTypeId, String entitySection) throws Exception {

		//hardcoded values for offset and size , need to change to make it proper.
		int offset = 0;
		int size = 10;
		Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

		String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" + size + ",\"orderByColumnName\":\"id\"," +
				"\"orderDirection\":\"desc\",\"filterJson\":{}}}";

		logger.info("Hitting ListRendererListData");

		listDataObj.hitListRendererListData(urlId, false,
				listDataPayload, null);

		String listDataJsonStr = listDataObj.getListDataJsonStr();
		return listDataJsonStr;
	}

	public Boolean validateEditPermissionForId(String entityName, int dbId) {
		Boolean errorResponceStatus = true;
		try {
			logger.info(" Checking the show permission for entityTypeId [ {} ] , and dbId [ {} ]", entityName, dbId);
			Edit editObj = new Edit();
			String showJsonStr = editObj.hitEdit(entityName,dbId);
			errorResponceStatus = APIUtils.isPermissionDeniedInResponse(showJsonStr);
			logger.info(" The test status for entity [ {} ] , and DBId [ {} ] , if the responce contains permission error [ {} ]", entityName, dbId, errorResponceStatus);
		} catch (Exception e) {
			logger.error("Exception while processing  validateShowPermissionForId [ {} ]", e.getMessage());
		}
		return errorResponceStatus;
	}

	private boolean verifyEditListingForAllRecords(String entityName) {
		boolean testStatus = false;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String listDataResponse = listDataResponse(entityTypeId, entityName);
			csAssertion.assertTrue(APIUtils.validJsonResponse(listDataResponse), "Response is not a valid JSON for entity Name [ " + entityName + " ]");

			JSONObject listDataResponseObj = new JSONObject(listDataResponse);
			int noOfRecords = listDataResponseObj.getJSONArray("data").length();
			logger.info("--------- Number of records are [ {} ] for the entity [ {} ] -----------------", noOfRecords, entityName);

			if (noOfRecords > 0) {
				listDataObj.setListData(listDataResponse);
				int columnId = listDataObj.getColumnIdFromColumnName("id");
				List<Integer> allDBid = listDataObj.getAllRecordDbId(columnId, listDataResponse);

				for (Integer dbId : allDBid) {
					boolean errorResponce = validateEditPermissionForId(entityName, dbId);
					if (errorResponce) {
						testStatus = true;
						csAssertion.assertTrue(false, "The dbId [ " + dbId + " ] for entity Name [ " + entityName + " ] is having ErrorMessage ");
					}
				}
			} else
				logger.warn("no records found for entity : [ {} ]", entityName);
		} catch (Exception e) {
			logger.error("Exception while processing verifyShowListing [ {} ]", e.getMessage());
		}
		return testStatus;
	}

}
