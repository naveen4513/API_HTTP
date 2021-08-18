package com.sirionlabs.test.common;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDateFieldRangeSIR199471 {

	private final static Logger logger = LoggerFactory.getLogger(TestDateFieldRangeSIR199471.class);
	private String configFilePath = null;
	private String configFileName = null;
	private static String extraFieldsConfigFileName = null;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SIR199471ConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("SIR199471ConfigFileName");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	/*
	TC-C63032: Verify that Selected date range up-to 500+ years is getting reflected on Show Page.
	TC-C63033: Verify that Selected date range up-to 500+ years is getting reflecting on entity listing page.
	 */
	@Test
	public void testDateFieldRangeOnShowPageAndListingPage() {
		CustomAssert csAssert = new CustomAssert();
		Map<Integer, Integer> recordsToDeleteMap = new HashMap<>();

		try {
			logger.info("Setting all Flows to Validate");
			List<String> allFlowsToTest = new ArrayList<>();

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
			if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
				logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
				allFlowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
			} else {
				String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
				for (String flow : allFlows) {
					if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
						allFlowsToTest.add(flow.trim());
					} else {
						logger.info("Flow having name [{}] not found in Config File.", flow.trim());
					}
				}
			}

			for (String flowToTest : allFlowsToTest) {
				Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
				String entityName = flowProperties.get("entity");
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

				String createResponse = createNewRecord(entityName, flowToTest);

				if (createResponse == null || !ParseJsonResponse.successfulResponse(createResponse)) {
					csAssert.assertTrue(false, "Couldn't create New Record of Entity " + entityName);
					continue;
				}

				int newRecordId = CreateEntity.getNewEntityId(createResponse, entityName);
				recordsToDeleteMap.put(entityTypeId, newRecordId);

				//Validate on Show Page
				String fieldToValidate = flowProperties.get("fieldtovalidate");
				String showPageHierarchy = ShowHelper.getShowFieldHierarchy(fieldToValidate, entityTypeId);
				String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, newRecordId);
				String valueOnShowPage = ShowHelper.getActualValue(showResponse, showPageHierarchy);

				if (valueOnShowPage == null) {
					csAssert.assertTrue(false, "Couldn't get Actual Value on Show Page for Record Id " + newRecordId + " of Entity " + entityName);
					continue;
				}

				String expectedYearValue = getExpectedYearValue(fieldToValidate, flowToTest);
				if (expectedYearValue == null) {
					csAssert.assertTrue(false, "Couldn't get Expected Year Value for Flow " + flowToTest);
					continue;
				}

				if (!valueOnShowPage.contains(expectedYearValue)) {
					csAssert.assertTrue(false, "Entity " + entityName + " Expected Year Value: [" + expectedYearValue +
							"] and Actual Value: [" + valueOnShowPage + "] on Show Page");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Date Field Range on Show Page and Listing Page. " + e.getMessage());
		} finally {
			if (deleteEntity) {
				for (Map.Entry<Integer, Integer> recordMap : recordsToDeleteMap.entrySet()) {
					int recordId = recordMap.getValue();
					String entityName = ConfigureConstantFields.getEntityNameById(recordMap.getKey());
					logger.info("Deleting Record having Id {} of Entity {}", recordId, entityName);
					EntityOperationsHelper.deleteEntityRecord(entityName, recordId);
				}

				csAssert.assertAll();
			}
		}
	}

	private String createNewRecord(String entityName, String flowToTest) {
		try {
			String createResponse = null;

			switch (entityName.trim().toLowerCase()) {
				case "actions":
					createResponse = Action.createAction(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, flowToTest, true);
					break;

				case "contracts":
					createResponse = Contract.createContract(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, flowToTest, true);
					break;

				case "obligations":
					createResponse = Obligations.createObligation(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, flowToTest, true);
					break;
			}

			return createResponse;
		} catch (Exception e) {
			logger.error("Exception while Creating New Record of Entity {} and Flow {}. {}", entityName, flowToTest, e.getStackTrace());
		}

		return null;
	}

	private String getExpectedYearValue(String field, String flowToTest) {
		String fieldValue = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, extraFieldsConfigFileName, flowToTest, field);
		String[] temp = fieldValue.split(Pattern.quote(","));

		for (String temp2 : temp) {
			if (temp2.trim().contains("values")) {
				temp2 = temp2.replace("}", "").replaceAll("\"", "");

				String[] temp3 = temp2.split(Pattern.quote("-"));
				String[] temp4 = temp3[temp3.length - 1].split(Pattern.quote(" "));
				return temp4[0];
			}
		}

		return null;
	}
}