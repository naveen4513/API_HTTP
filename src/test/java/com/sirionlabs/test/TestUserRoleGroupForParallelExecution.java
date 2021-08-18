package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.executerserviceutil.TaskExecuter;
import com.sirionlabs.utils.executerserviceutil.TaskExecutionImpl;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;
import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;
import com.sirionlabs.utils.taskutil.EntityTaskImpl;
import com.sirionlabs.utils.taskutil.ShowPageTaskImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author manoj.upreti
 */
public class TestUserRoleGroupForParallelExecution {
	private final static Logger logger = LoggerFactory.getLogger(TestUserRoleGroupForParallelExecution.class);
	CustomAssert csAssertion;
	String entityIdMappingFilePath;
	String getEntityIdMappingFileName;
	String userRoleGroupFilePath;
	String userRoleGroupFileName;
	String entityIdMappingFileName;
	String baseFilePath;
	ListRendererListData listDataObj;

	public TestUserRoleGroupForParallelExecution() {
		listDataObj = new ListRendererListData();
		csAssertion = new CustomAssert();
		entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		getEntityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		userRoleGroupFileName = ConfigureConstantFields.getConstantFieldsProperty("UserRoleGroupConfigFileName");
		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
	}

	@BeforeClass
	public void beforeClass() throws IOException {
		listDataObj = new ListRendererListData();
		csAssertion = new CustomAssert();
		entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		getEntityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		userRoleGroupFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		userRoleGroupFileName = ConfigureConstantFields.getConstantFieldsProperty("UserRoleGroupConfigFileName");
		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = "src//test//resources";
	}

	@Test
	public void testUserRoleGroup() {
		try {
			List<String> allEntitySection = ParseConfigFile.getAllSectionNames(userRoleGroupFilePath, userRoleGroupFileName);
			List<String> entityRequestPayloadProperties = ParseConfigFile.getAllPropertiesOfSection(userRoleGroupFilePath, userRoleGroupFileName, "request_payload");
			List<TaskImpl> taskListForEntityListing = new ArrayList<>();
			//List<TaskImpl> taskListForShowListing = new ArrayList<>();
			List<String> entityListForShowPageListing = new ArrayList<>();
			for (String entitySection : allEntitySection) {
				// Iterate over each entitySection
				if (entitySection.equalsIgnoreCase("request_payload")) {
					continue;
				}
				List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(userRoleGroupFilePath, userRoleGroupFileName, entitySection);
				for (String property : allProperties) {
					switch (property.trim().toLowerCase()) {
						case "listing":
							logger.info("Call the listing for Entity [ {} ]", entitySection);
							TaskImpl taskCreationObj = new EntityTaskImpl("verifyEntityListing", entitySection + "", "", "entityTask");
							taskListForEntityListing.add(taskCreationObj);
							break;
						case "show":
							logger.info("Call the showpage Listing for Entity [ {} ]", entitySection);
							entityListForShowPageListing.add(entitySection);
							break;
						default:
							logger.warn("No Test is enabled for the property [ {} ]", property);
							break;
					}
				}
			}
			//get the task results
			List<TaskReturnObject> taskReturnObjectsForEntityListing;
			taskReturnObjectsForEntityListing = TaskExecuter.executeParallely(taskListForEntityListing);
			Map<String, List<TaskReturnObject>> verifyShowListingForAllEntities = verifyShowListingForAllEntities(entityListForShowPageListing);

			//check All Entity Listing
			if (taskReturnObjectsForEntityListing != null) {
				for (TaskReturnObject entityListing : taskReturnObjectsForEntityListing) {
					if (entityListing != null) {
						boolean propertyValue = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, entityListing.entityName, "listing"));
						csAssertion.assertNotEquals(entityListing.SUCCESS, propertyValue, "Failed for entitySection [ " + entityListing.entityName + " ] , The test status for the property [ listing ] is [ " + !entityListing.SUCCESS + " ] but required is [" + propertyValue + " ]\n\n");
						break;
					} else {
						csAssertion.assertTrue(false, "The responce is null , please check error messages");
					}
				}
			} else {
				csAssertion.assertTrue(false, "The responce is null , please check error messages");
			}


			// check Result for Alll Show Page Listing
			if (verifyShowListingForAllEntities != null) {
				for (Map.Entry<String, List<TaskReturnObject>> verifyShowListingForAllEntiy : verifyShowListingForAllEntities.entrySet()) {
					if (verifyShowListingForAllEntiy != null) {
						logger.info("Checking the show Page result for Entity [ {} ] ", verifyShowListingForAllEntiy.getKey());
						boolean propertyValue = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(userRoleGroupFilePath, userRoleGroupFileName, verifyShowListingForAllEntiy.getKey(), "show"));
						boolean testStatusOFShowForEntity = false;
						for (TaskReturnObject showPageTaskRet : verifyShowListingForAllEntiy.getValue()) {
							if (verifyShowListingForAllEntiy != null) {
								if (showPageTaskRet.SUCCESS) {
									testStatusOFShowForEntity = true;
									csAssertion.assertTrue(false, "The dbId [ " + showPageTaskRet.dbID + " ] for entity Name [ " + showPageTaskRet.entityName + " ] is having ErrorMessage ");
								}
							} else {
								csAssertion.assertTrue(false, "The responce is null , please check error messages");
							}
						}
						csAssertion.assertNotEquals(testStatusOFShowForEntity, propertyValue, "Failed for entitySection [ " + verifyShowListingForAllEntiy.getKey() + " ] , The test status for the property [ show ] is [ " + !testStatusOFShowForEntity + " ] but required is [" + propertyValue + " ]\n\n");
					} else {
						csAssertion.assertTrue(false, "The responce is null , please check error messages");
					}
				}
			} else {
				csAssertion.assertTrue(false, "The responce is null , please check error messages");
			}

		} catch (Exception e) {
			logger.error("Exception while running testUserRoleGroup [ {} ]", e.getMessage());
		}


		csAssertion.assertAll();
	}

	public boolean verifyEntityListing(String entityName) {
		boolean testStatus = false;

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


	private Map<String, List<TaskReturnObject>> verifyShowListingForAllEntities(List<String> entityNameList) {
		List<TaskImpl> taskList = new ArrayList<>();
		Map<String, List<TaskReturnObject>> showPageListingForAllEntityMap = new HashMap<>();
		for (String entityName : entityNameList) {
			TaskImpl taskCreationObj = new ShowPageTaskImpl("validateShowPermissionMultipleEntitiesParallel", entityName + "", "", "showpage");
			taskList.add(taskCreationObj);
		}
		List<TaskReturnObject> taskResults = TaskExecuter.executeParallely(taskList);

		for (TaskReturnObject taskResult : taskResults) {
			showPageListingForAllEntityMap.put(taskResult.entityName, taskResult.taskReturnObjectList);
		}

		return showPageListingForAllEntityMap;
	}

	/**
	 * @param entityName
	 * @return boolean value of show entity permission
	 */
	public List<TaskReturnObject> verifyShowListingForAllRecords(String entityName) {
		List<TaskReturnObject> taskResults = null;
		try {

			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String listDataResponse = listDataResponse(entityTypeId, entityName);
			if (!APIUtils.validJsonResponse(listDataResponse)) {
				logger.error("The responce for entity [ {} ] is not a valid Jason for ShowListing in [ verifyShowListingForAllRecords ] method", entityName);
				return null;
			}

			JSONObject listDataResponseObj = new JSONObject(listDataResponse);
			int noOfRecords = listDataResponseObj.getJSONArray("data").length();
			logger.info("--------- Number of records are [ {} ] for the entity [ {} ] -----------------", noOfRecords, entityName);

			if (noOfRecords > 0) {
				listDataObj.setListData(listDataResponse);
				int columnId = listDataObj.getColumnIdFromColumnName("id");

				List<TaskImpl> taskList = new ArrayList<>();
				List<Integer> allDBid = listDataObj.getAllRecordDbId(columnId, listDataResponse);

				for (Integer dbId : allDBid) {
					TaskImpl taskCreationObj = new ShowPageTaskImpl("validateShowPermissionForId", entityTypeId + "", dbId + "", "showpage");
					taskList.add(taskCreationObj);
				}
				taskResults = TaskExecutionImpl.executeAnyTask(taskList);
			} else
				logger.warn("no records found for entity : [ {} ]", entityName);
		} catch (Exception e) {
			logger.error("Exception while processing verifyShowListingForAllRecords [ {} ]", e.getMessage());
			return null;
		}
		return taskResults;
	}

	public boolean validateShowPermissionForId(int entityTypeId, int dbId) {
		boolean errorResponceStatus = true;
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
}
