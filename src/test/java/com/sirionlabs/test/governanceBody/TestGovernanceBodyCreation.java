package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGovernanceBodyCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestGovernanceBodyCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer gbEntityTypeId;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("GovernanceBodyCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("GovernanceBodyCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestGBCreation() throws ConfigurationException {
		logger.info("Setting all Governance Body Creation Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
		} else {
			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Governance Body Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestGBCreation")
	public void testGBCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer gbId = -1;

		try {
			logger.info("Validating GB Creation Flow [{}]", flowToTest);

			//Validate GB Creation
			logger.info("Creating GB for Flow [{}]", flowToTest);
			String createResponse = GovernanceBody.createGB(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					gbId = CreateEntity.getNewEntityId(createResponse, "governance body");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (gbId != -1) {
							logger.info("Governance Body Created Successfully with Id {}: ", gbId);
							logger.info("Hitting Show API for Governance Body Id {}", gbId);
							Show showObj = new Show();
							showObj.hitShow(gbEntityTypeId, gbId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Governance Body Id " + gbId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Governance Body Id " + gbId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Governance Body for Flow [" + flowToTest + "] due to " + createStatus);
						if(createStatus.equals("validationError"))
						logger.info(JSONUtility.parseJson(createResponse,"$.body.errors").toString());
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Governance Body Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Governance Body Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Governance Body Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && gbId != -1) {
				logger.info("Deleting Governance Body having Id {}", gbId);
				EntityOperationsHelper.deleteEntityRecord("governance body", gbId);
			}
			csAssert.assertAll();
		}
	}
}