package com.sirionlabs.test;


import com.sirionlabs.api.bulkaction.BulkActionSave;
import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class test_SIR190442 extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(test_SIR190442.class);
	private String configFilePath;
	private String configFileName;
	private String bulkCreateTemplateFilePath;
	private String bulkUploadTemplateFilePath;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testSIR190442ConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("testSIR190442ConfigFileName");
		bulkCreateTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreate", "bulkcreatefilepath");
		bulkUploadTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkupload", "bulkuploadfilepath");

	}

	@DataProvider
	public Object[][] dataProviderForBulkCreate() {

		logger.info("Setting flow for Bulk Create ");

		List<Object[]> allTestData = new ArrayList<>();

		String parentEntityName;
		String[] childBulkEntities;
		String[] templateIds;
		String[] parentEntitiesForBulkCreate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreate", "entitiestotest").split(",");
		Integer parentId;

		for (String entity : parentEntitiesForBulkCreate) {

			parentEntityName = entity;
			childBulkEntities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, parentEntityName, "bulkcreateentities").split(",");
			templateIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, parentEntityName, "templateids").split(",");
			parentId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, parentEntityName, "parentid"));

			for (int j = 0; j < childBulkEntities.length; j++) {
				allTestData.add(new Object[]{parentEntityName, childBulkEntities[j], Integer.parseInt(templateIds[j]), parentId});
			}
		}

		return allTestData.toArray(new Object[0][]);

	}

	@DataProvider
	public Object[][] dataProviderForBulkEdit() {

		logger.info("Setting flow for Bulk Edit ");

		List<Object[]> allTestData = new ArrayList<>();

		String parentEntityName;
		Integer entityId;
		String[] entitiesForBulkEdit = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkedit", "entitiestotest").split(",");

		for (String entity : entitiesForBulkEdit) {

			parentEntityName = entity;
			entityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, parentEntityName, "bulkeditentityid"));
			allTestData.add(new Object[]{parentEntityName, entityId});
		}

		return allTestData.toArray(new Object[0][]);

	}

	@DataProvider
	public Object[][] dataProviderForBulkAction() {

		logger.info("Setting flow for Bulk Action ");

		List<Object[]> allTestData = new ArrayList<>();

		String[] entitiesForBulkAction = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkaction", "entitiestotest").split(",");

		for (String entity : entitiesForBulkAction) {

			allTestData.add(new Object[]{entity});
		}

		return allTestData.toArray(new Object[0][]);
	}

	@DataProvider
	public Object[][] dataProviderListDataFilterAPI() {

		logger.info("Setting flow for List Data API for Job Scheduler ");

		List<Object[]> allTestData = new ArrayList<>();

		String[] entitiesForListDataJobScheduler = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listrender job scheduler", "entitiestotest").split(",");

		for (String entity : entitiesForListDataJobScheduler) {

			allTestData.add(new Object[]{entity});
		}

		return allTestData.toArray(new Object[0][]);

	}

	@Test(dataProvider = "dataProviderForBulkCreate", priority = 0)
	public void testFetchForBulkCreateSuccess(String parentEntityName, String bulkCreateEntity, Integer templateId, Integer parentId) {

		CustomAssert csAssert = new CustomAssert();
		killAllSchedulerTasks();

		String templateFilePath = bulkCreateTemplateFilePath + parentEntityName;
		String templateFileName = bulkCreateEntity + "_Pass.xlsm";
		int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
		int bulkCreateEntityTypeId = ConfigureConstantFields.getEntityIdByName(bulkCreateEntity);

		killAllSchedulerTasks();

		String bulkUploadResponse = BulkTemplate.uploadBulkCreateTemplate(templateFilePath, templateFileName, parentEntityTypeId, parentId, bulkCreateEntityTypeId, templateId);

		if (bulkUploadResponse.contains("Template is not correct")) {
			csAssert.assertTrue(false, "Template is not correct");
			csAssert.assertAll();
			return;

		} else {
			csAssert.assertTrue(true, "Template is correct");
		}

		if (bulkUploadResponse.contains("200")) {
			logger.info("Bulk create File uploaded successfully");
		} else {
			logger.error("Bulk create File uploaded unsuccessfully");
			csAssert.assertTrue(false, "Bulk create File uploaded unsuccessfully");
			csAssert.assertAll();
			return;
		}

		String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("pass entities", csAssert);
		if (!(resultDetails == null)) {
			csAssert.assertTrue(true, "Fetch Response Validation successful");
			validateFetchResponseWithListData(resultDetails, parentEntityTypeId, parentEntityName, csAssert);

		} else {
			logger.error("Fetch Response Validation unsuccessful");
			csAssert.assertTrue(false, "Fetch Response Validation unsuccessful");
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForBulkCreate")
	public void testFetchForBulkCreateFail(String parentEntityName, String bulkCreateEntity, Integer templateId, Integer parentId) {

		CustomAssert csAssert = new CustomAssert();

		killAllSchedulerTasks();

		String templateFilePath = bulkCreateTemplateFilePath + parentEntityName;
		String templateFileName = bulkCreateEntity + "_Fail.xlsm";
		int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
		int bulkCreateEntityTypeId = ConfigureConstantFields.getEntityIdByName(bulkCreateEntity);

		killAllSchedulerTasks();

		String bulkUploadResponse = BulkTemplate.uploadBulkCreateTemplate(templateFilePath, templateFileName, parentEntityTypeId, parentId, bulkCreateEntityTypeId, templateId);
		if (bulkUploadResponse.contains("Template is not correct")) {
			logger.error("Template is not correct");
			csAssert.assertTrue(false, "Template is not correct");
			csAssert.assertAll();
			return;
		} else {
			csAssert.assertTrue(true, "Template is correct");
		}

		if (bulkUploadResponse.contains("200")) {
			logger.info("Bulk create File uploaded successfully");
			csAssert.assertTrue(true, "Bulk create File uploaded successful");
		} else {
			logger.error("Bulk create File uploaded unsuccessfully");
			csAssert.assertTrue(false, "Bulk create File uploaded unsuccessfully " + bulkUploadResponse);
			csAssert.assertAll();
			return;
		}
		String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("createFailCase", csAssert);
		if (!(resultDetails == null)) {
			csAssert.assertTrue(true, "Fetch Response Validation successful");
			resultDetails[2] = "0";
			validateFetchResponseWithListData(resultDetails, parentEntityTypeId, parentEntityName, csAssert);

		} else {
			logger.error("Fetch Response Validation unsuccessful");
			csAssert.assertTrue(false, "Fetch Response Validation unsuccessful");
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForBulkEdit")
	public void testFetchForBulkEditSuccess(String entityName, Integer entityId) {

		CustomAssert csAssert = new CustomAssert();

		JSONObject valuesJson = new JSONObject();
		JSONArray valuesJsonArray = new JSONArray();
		JSONObject globalDataJson = new JSONObject();
		JSONArray entityIds = new JSONArray();
		JSONArray fieldIds = new JSONArray();

		killAllSchedulerTasks();

		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

		killAllSchedulerTasks();

		String bulkEditPayload = "{\"entityIds\":[" + entityId + "]}";

		BulkeditEdit bulkeditEdit = new BulkeditEdit();
//		bulkeditEdit.hitBulkEditCreate(entityTypeId, bulkEditPayload);
		bulkeditEdit.hitBulkEditCreate(entityTypeId, bulkEditPayload);
		String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

		JSONObject bulkEditResponseJson = new JSONObject(bulkEditResponse);

		try {
			bulkEditResponseJson.remove("header");
			bulkEditResponseJson.remove("session");
			bulkEditResponseJson.remove("actions");
			bulkEditResponseJson.remove("createLinks");
			bulkEditResponseJson.getJSONObject("body").remove("layoutInfo");
			bulkEditResponseJson.getJSONObject("body").remove("globalData");
			bulkEditResponseJson.getJSONObject("body").remove("errors");

			JSONObject dataJson = bulkEditResponseJson.getJSONObject("body").getJSONObject("data");

			valuesJson.put("name", "Human Resources");
			valuesJson.put("id", "1003");
			valuesJsonArray.put(0, valuesJson);

			dataJson.getJSONObject("functions").put("options", "null");
			dataJson.getJSONObject("functions").put("values", valuesJsonArray);

			entityIds.put(0, entityId);
			fieldIds.put(0, 508);

			globalDataJson.put("entityIds", entityIds);
			globalDataJson.put("fieldIds", fieldIds);
			globalDataJson.put("isGlobalBulk", true);

			bulkEditResponseJson.getJSONObject("body").put("data", dataJson);
			bulkEditResponseJson.getJSONObject("body").put("globalData", globalDataJson);

		} catch (Exception e) {
			logger.error("Exception while creating payload for bulk edit");
			csAssert.assertTrue(false, "Exception while creating payload for bulk edit");
		}

		bulkeditEdit.hitBulkeditEdit(entityTypeId, bulkEditResponseJson.toString());
		bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

		if (bulkEditResponse.contains("success")) {
			logger.info("Bulk Edit successful");
			csAssert.assertTrue(true, "Bulk Edit successful");
		} else {
			logger.error("Bulk Edit unsuccessful");
			csAssert.assertTrue(false, "Bulk Edit unsuccessful");
		}

		String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("pass entities", csAssert);
		if (!(resultDetails == null)) {
			csAssert.assertTrue(true, "Fetch Response Validation successful");
			validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);
		} else {
			logger.error("Fetch Response validated unsuccessfully");
			csAssert.assertTrue(false, "Fetch Response validated unsuccessfully");
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForBulkEdit")
	public void testFetchForBulkEditFailure(String entityName, Integer entityId) {

		CustomAssert csAssert = new CustomAssert();

		JSONObject valuesJson = new JSONObject();
		JSONArray valuesJsonArray = new JSONArray();
		JSONObject globalDataJson = new JSONObject();
		JSONArray entityIds = new JSONArray();
		JSONArray fieldIds = new JSONArray();

		killAllSchedulerTasks();

		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

		killAllSchedulerTasks();

		String bulkEditPayload = "{\"entityIds\":[" + entityId + "]}";

		BulkeditEdit bulkeditEdit = new BulkeditEdit();
		bulkeditEdit.hitBulkEditCreate(entityTypeId, bulkEditPayload);
		String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

		JSONObject bulkEditResponseJson = new JSONObject(bulkEditResponse);

		try {
			bulkEditResponseJson.remove("header");
			bulkEditResponseJson.remove("session");
			bulkEditResponseJson.remove("actions");
			bulkEditResponseJson.remove("createLinks");
			bulkEditResponseJson.getJSONObject("body").remove("layoutInfo");
			bulkEditResponseJson.getJSONObject("body").remove("globalData");
			bulkEditResponseJson.getJSONObject("body").remove("errors");

			JSONObject dataJson = bulkEditResponseJson.getJSONObject("body").getJSONObject("data");

			valuesJson.put("name", "Human Resources");
			valuesJson.put("id", "1003");
			valuesJsonArray.put(0, valuesJson);

			dataJson.getJSONObject("functions").put("options", "null");
			dataJson.getJSONObject("functions").put("values", valuesJsonArray);
			dataJson.getJSONObject("email").put("values", 11111);
			entityIds.put(0, entityId);
			fieldIds.put(0, 7004);

			globalDataJson.put("entityIds", entityIds);
			globalDataJson.put("fieldIds", fieldIds);
			globalDataJson.put("isGlobalBulk", true);

			bulkEditResponseJson.getJSONObject("body").put("data", dataJson);
			bulkEditResponseJson.getJSONObject("body").put("globalData", globalDataJson);

		} catch (Exception e) {
			logger.error("Exception while creating payload for bulk edit");
			csAssert.assertTrue(false, "Exception while creating payload for bulk edit");
		}

		bulkeditEdit.hitBulkeditEdit(entityTypeId, bulkEditResponseJson.toString());
		bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

		bulkEditResponseJson = new JSONObject(bulkEditResponse);

		if (bulkEditResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success")) {
			logger.info("Bulk Edit done successfully");
			csAssert.assertTrue(true, "Bulk Edit done successfully");
		} else {
			logger.error("Bulk Edit done unsuccessfully");
			csAssert.assertTrue(false, "Bulk Edit done unsuccessfully");
		}

		String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("failed entities", csAssert);
		if (!(resultDetails == null)) {
			csAssert.assertTrue(true, "Fetch Response Validation successful");
			validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);

		} else {
			logger.error("Fetch Response validated unsuccessfully");
			csAssert.assertTrue(false, "Fetch Response validated unsuccessfully");
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForBulkAction")
	public void testFetchForBulkActionSuccess(String entityName) {

		CustomAssert csAssert = new CustomAssert();

		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		int listId = ConfigureConstantFields.getListIdForEntity(entityName);

		ListRendererListData listRendererListData = new ListRendererListData();

		killAllSchedulerTasks();
//        Filtering the records based on newly created status
		String filterMap = "{\"filterMap\": {\"entityTypeId\": " + entityTypeId + ",\"offset\": 0,\"size\": 2000,\"orderByColumnName\": \"id\",\"orderDirection\": \"desc nulls last\",\"filterJson\": {\"currentTask\": \"Newly Created\",\"nextTaskForBulk\": \"On Hold\"}}}";

		listRendererListData.hitListRendererListDataV2(listId, filterMap);
		String listDataResponse = listRendererListData.getListDataJsonStr();

		JSONObject listDataResponseJson = new JSONObject(listDataResponse);
		JSONObject indDataJson = listDataResponseJson.getJSONArray("data").getJSONObject(0);
		JSONArray indDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indDataJson);
		JSONObject columnJson;
		String entityId = "";
		for (int i = 0; i < indDataJsonArray.length(); i++) {

			columnJson = indDataJsonArray.getJSONObject(i);
			if (columnJson.get("columnName").equals("id")) {
				entityId = columnJson.get("value").toString().split(":;")[1];
				break;
			}
		}
		if (entityId.equals("")) {
			logger.error("Can't find entity id with newly created status");
			csAssert.assertTrue(false, "Can't find entity id with newly created status");
			csAssert.assertAll();
			return;
		}
		String payloadForBulkSave = createPayloadForBulkSaveSuccess(entityId, entityTypeId, listId);

		try {
			executor.post(BulkActionSave.getApiPath(), BulkActionSave.getHeaders(), payloadForBulkSave);

            /*bulkactionSave.hitBulkActionSave(payloadForBulkSave);
            bulkactionSave.getBulkActionSaveJsonStr();*/

			String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("pass entities", csAssert);
			if (!(resultDetails == null)) {
				csAssert.assertTrue(true, "Fetch Response Validation successful");
				validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);

			} else {
				logger.error("Fetch Response validated unsuccessfully");
				csAssert.assertTrue(false, "Fetch Response validated unsuccessfully");
			}
		} catch (Exception e) {
			logger.error("Exception while hitting bulk action");
			csAssert.assertTrue(false, "Exception while hitting bulk action");
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "dataProviderForBulkAction")
	public void testFetchForBulkActionFail(String entityName) {

		CustomAssert csAssert = new CustomAssert();

		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		int listId = ConfigureConstantFields.getListIdForEntity(entityName);

		ListRendererListData listRendererListData = new ListRendererListData();

		killAllSchedulerTasks();
//        Filtering the records based on newly created status

		String filterMap = "{\"filterMap\": {\"entityTypeId\": " + entityTypeId + ",\"offset\": 0,\"size\": 2000,\"orderByColumnName\": \"id\",\"orderDirection\": \"desc nulls last\",\"filterJson\": {\"currentTask\": \"Newly Created\",\"nextTaskForBulk\": \"On Hold\"}}}";

		listRendererListData.hitListRendererListDataV2(listId, filterMap);
		String listDataResponse = listRendererListData.getListDataJsonStr();

		JSONObject listDataResponseJson = new JSONObject(listDataResponse);
		JSONObject indDataJson = listDataResponseJson.getJSONArray("data").getJSONObject(0);
		JSONArray indDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(indDataJson);
		JSONObject columnJson;
		String entityId = "";
		for (int i = 0; i < indDataJsonArray.length(); i++) {

			columnJson = indDataJsonArray.getJSONObject(i);
			if (columnJson.get("columnName").equals("id")) {
				entityId = columnJson.get("value").toString().split(":;")[1];
				break;
			}
		}

		if (entityId.equals("")) {
			logger.error("Can't find entity id with newly created status");
			csAssert.assertTrue(false, "Can't find entity id with newly created status");
			return;
		}

		String payloadForBulkSave = createPayloadForBulkSaveFail(entityId, entityTypeId, listId);

		try {
			executor.post(BulkActionSave.getApiPath(), BulkActionSave.getHeaders(), payloadForBulkSave);

            /*bulkactionSave.hitBulkActionSave(payloadForBulkSave);
            bulkactionSave.getBulkActionSaveJsonStr();*/
			String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("failed entities", csAssert);
			if (!(resultDetails == null)) {
				csAssert.assertTrue(true, "Fetch Response Validation successful");
				validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);

				logger.info("Fetch Response validated successfully");
				csAssert.assertTrue(true, "Fetch Response validated successfully");
			} else {
				logger.error("Fetch Response validated unsuccessfully");
				csAssert.assertTrue(false, "Fetch Response validated unsuccessfully");
				csAssert.assertTrue(false, "Either the job has not been picked up or resultDetails is null");
			}
		} catch (Exception e) {
			logger.error("Exception while hitting bulk save");
			csAssert.assertTrue(false, "Exception while hitting bulk save");
		}
		csAssert.assertAll();
	}

	@Test()
	public void testFetchForPricingUploadSuccess() {

		CustomAssert csAssert = new CustomAssert();

		String pricingFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricing upload", "pricinguploadfilepath");
		String pricingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricing upload", "pricinguploadfilenamepass");
		int entityTypeId = 176;
		String entityName = "consumptions";
		killAllSchedulerTasks();
//        Filtering the records based on newly created status

		try {
			InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
			String pricingUploadResponse = invoicePricingHelper.uploadPricing(pricingFilePath, pricingFileName);

			if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {

				logger.info("Pricing file uploaded successfully");
				csAssert.assertTrue(true, "Pricing file uploaded successfully");
			} else {
				logger.error("Pricing file uploaded unsuccessfully");
				csAssert.assertTrue(false, "Pricing file uploaded unsuccessfully");

			}
			Thread.sleep(10000);

			String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("pass entities", csAssert);
			if (!(resultDetails == null)) {
				csAssert.assertTrue(true, "Fetch Response Validation successful");
				validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);

			} else {
				logger.error("Fetch Response validated unsuccessfully");
				csAssert.assertTrue(false, "Fetch Response does not contain pass fail entities");

			}
		} catch (Exception e) {
			logger.error("Exception while pricing upload");
			csAssert.assertTrue(false, "Exception while pricing upload");
		}
		csAssert.assertAll();
	}

	@Test()
	public void testFetchForPricingUploadFail() {

		CustomAssert csAssert = new CustomAssert();

		String pricingFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricing upload", "pricinguploadfilepath");
		String pricingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricing upload", "pricinguploadfilenamefail");

		int entityTypeId = 176;
		String entityName = "consumptions";

		killAllSchedulerTasks();
//        Filtering the records based on newly created status

		try {
			InvoicePricingHelper invoicePricingHelper = new InvoicePricingHelper();
			String pricingUploadResponse = invoicePricingHelper.uploadPricing(pricingFilePath, pricingFileName);

			if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {

				logger.info("pricing file uploaded successfully");
			}
			Thread.sleep(10000);

			String[] resultDetails = checkFetchResponseIfFailedPassEntitiesExist("failed entities", csAssert);
			if (!(resultDetails == null)) {

				csAssert.assertTrue(true, "Fetch Response Validation successful");
				validateFetchResponseWithListData(resultDetails, entityTypeId, entityName, csAssert);

			} else {
				logger.error("Fetch Response doesn't contains failedEntities");
				csAssert.assertTrue(false, "Fetch Response does not contain pass fail entities");
			}
		} catch (Exception e) {
			logger.error("Exception while pricing upload");
			csAssert.assertTrue(false, "Exception while pricing upload");
		}
		csAssert.assertAll();
	}

	private void killAllSchedulerTasks() {
		UserTasksHelper.removeAllTasks();
	}

	private String[] checkFetchResponseIfFailedPassEntitiesExist(String whatToTestFor, CustomAssert csAssert) {

		String[] resultDetails = new String[3];
		Fetch fetch = new Fetch();
		try {

			if (!(waitForScheduler().equalsIgnoreCase("pass"))) {
				logger.error("Job didn't completed in specific time so skipping the test");
				csAssert.assertTrue(false, "Job didn't completed in specific time so skipping the test");
				return resultDetails;
			}

			fetch.hitFetch();
			String fetchResponse = fetch.getFetchJsonStr();

			JSONObject fetchResponseJson = new JSONObject(fetchResponse);
			JSONArray currentDayUserTasksJsonArray = fetchResponseJson.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");

			String requestId = currentDayUserTasksJsonArray.getJSONObject(0).get("requestId").toString();
			String successList = currentDayUserTasksJsonArray.getJSONObject(0).get("successList").toString();
			String failedList = currentDayUserTasksJsonArray.getJSONObject(0).get("failedList").toString();

			String successfullyProcessedRecordsCount = currentDayUserTasksJsonArray.getJSONObject(0).get("successfullyProcessedRecordsCount").toString();
			String failedRecordsCount = currentDayUserTasksJsonArray.getJSONObject(0).get("failedRecordsCount").toString();
			switch (whatToTestFor) {
				case "pass entities":
					if (successList.equalsIgnoreCase("true") && failedList.equalsIgnoreCase("false")) {
						logger.info("Fetch Response contain successList as true and failedList as false");


						resultDetails[0] = requestId;
						resultDetails[1] = successfullyProcessedRecordsCount;
						resultDetails[2] = failedRecordsCount;

					} else {
						logger.error("Fetch Response contain either of successList as false or failedList as true");
						csAssert.assertTrue(false, "Fetch Response contain either of successList as false or failedList as true");
						return null;
					}
					break;

				case "failed entities":
					if (successList.equalsIgnoreCase("false") && failedList.equalsIgnoreCase("true")) {
						logger.info("Fetch Response contain successList as false and failedList as true");


						resultDetails[0] = requestId;
						resultDetails[1] = successfullyProcessedRecordsCount;
						resultDetails[2] = failedRecordsCount;
					} else {
						logger.info("Fetch Response contain successList as true or failedList as false");
						csAssert.assertTrue(false, "Fetch Response contain successList as true or failedList as false");
						return null;
					}
					break;

				case "both":
					if (successList.equalsIgnoreCase("true") && failedList.equalsIgnoreCase("true")) {
						logger.info("Fetch Response contain both success and failed flags true");

						resultDetails[0] = requestId;
						resultDetails[1] = successfullyProcessedRecordsCount;
						resultDetails[2] = failedRecordsCount;
					} else {
						logger.error("Fetch Response contain either of success and failed flags as false");
						return null;
					}
					break;
				case "createFailCase":
					if (successList.equalsIgnoreCase("false") && failedList.equalsIgnoreCase("false")) {
						logger.info("Fetch Response contain both success and failed flags true");

						resultDetails[0] = requestId;
						resultDetails[1] = successfullyProcessedRecordsCount;
						resultDetails[2] = failedRecordsCount;
					} else {
						logger.error("Fetch Response contain either of success and failed flags as false");
						return null;
					}
					break;
			}


		} catch (Exception e) {
			logger.error("Exception while getting passed or failed entities ");
			resultDetails = null;
		}
		return resultDetails;
	}

	private String createPayloadForBulkSaveSuccess(String entityId, int entityTypeId, int listId) {

		String payloadForSave = "{\"entityIds\": [" + entityId + "],\"entityTypeId\": " + entityTypeId + ",\"listId\": \"" + listId + "\",\"fromTask\": \"Newly Created\",\"toTask\": \"On Hold\",\"toBeIgnoredEntityIds\": [],\"fromStatusId\": \"1611\",\"toActionId\": \"26877\",\"isGlobalBulk\": true,\"comment\": {},\"isSelectAll\": false,\"filterMap\": {}}";

		return payloadForSave;
	}

	private String createPayloadForBulkSaveFail(String entityId, int entityTypeId, int listId) {

		String payloadForSave = "{\"entityIds\": [" + entityId + "],\"entityTypeId\": " + entityTypeId + ",\"listId\": \"" + listId + "\",\"fromTask\": \"On Hold\",\"toTask\": \"Newly Created\",\"toBeIgnoredEntityIds\": [],\"fromStatusId\": \"26877\",\"toActionId\": \"1611\",\"isGlobalBulk\": true,\"comment\": {},\"isSelectAll\": false,\"filterMap\": {}}";

		return payloadForSave;
	}

	private String waitForScheduler() {

		String result = "pass";
		Long schedulerTimeOut = 120000L;
		Long pollingTime = 5000L;
		try {
			logger.info("Time Out for Pricing Scheduler is {} milliseconds", schedulerTimeOut);
			long timeSpent = 0;
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			logger.info("Getting Task Id of Current Job");
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), null);

			if (newTaskId != -1) {
				Boolean taskCompleted = false;
				logger.info("Checking if Scheduler Task has completed or not.");

				while (timeSpent < schedulerTimeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
					Thread.sleep(pollingTime);

					logger.info("Hitting Fetch API.");
					fetchObj.hitFetch();
					logger.info("Getting Status of Scheduler Task.");
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						taskCompleted = true;
						result = "pass";

						break;
					} else {
						timeSpent += pollingTime;
						logger.info("Pricing Upload Task is not finished yet.");
					}
				}
				if (!taskCompleted && timeSpent >= schedulerTimeOut) {
					//Task didn't complete within given time.
					result = "skip";
				}
			} else {
				logger.info("Couldn't get Pricing Upload Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
				Thread.sleep(schedulerTimeOut);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Pricing Scheduler to Finish");
			result = "fail";
		}
		return result;
	}

	private void validateFetchResponseWithListData(String[] resultDetails, int entityTypeId, String entityName, CustomAssert csAssert) {

		String bulkRequestId = resultDetails[0];
		int successEntries = Integer.parseInt(resultDetails[1]);
		int failEntries = Integer.parseInt(resultDetails[2]);
		int maxNoRecords = 10000;

		ListRendererListData listRendererListData = new ListRendererListData();

		Map<String, String> params = new HashMap<>();
		params.put("bulkRequestId", bulkRequestId);
		params.put("isSuccess", "true");

		String listResponse = listRendererListData.listDataResponseV2(entityTypeId, entityName, maxNoRecords, params);

		JSONObject listResponseJson = new JSONObject(listResponse);
		int filteredCount = Integer.parseInt(listResponseJson.get("filteredCount").toString());

		if (!(filteredCount == successEntries)) {
			logger.error("List Data Response contain unequal number of successful entries for bulk request id " + bulkRequestId + " as compared to fetch response ");
			csAssert.assertTrue(false, "List Data Response contain unequal number of successful entries for bulk request id " + bulkRequestId + " as compared to fetch response. List Data Response contains " + filteredCount + " entries. Fetch Response contains " + successEntries);
		}

		params.put("isSuccess", "false");

		listResponse = listRendererListData.listDataResponseV2(entityTypeId, entityName, maxNoRecords, params);

		listResponseJson = new JSONObject(listResponse);
		filteredCount = Integer.parseInt(listResponseJson.get("filteredCount").toString());

		if (!(filteredCount == failEntries)) {
			logger.error("List Data Response contain unequal number of failure entries for bulk request id " + bulkRequestId + " as compared to fetch response ");
			csAssert.assertTrue(false, "List Data Response contain unequal number of failure entries for bulk request id " + bulkRequestId + " as compared to fetch response. List Data Response contains " + filteredCount + " entries. Fetch Response contains " + failEntries);
		}
	}
}
