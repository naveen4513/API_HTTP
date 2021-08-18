package com.sirionlabs.test.contract;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.CreditEarnBack;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestCreditEarnBackCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestCreditEarnBackCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer creditEarnBackEntityTypeId;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CreditEarnBackCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CreditEarnBackCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		creditEarnBackEntityTypeId = ConfigureConstantFields.getEntityIdByName("creditearnbacks");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestCreditEarnBackCreation() throws ConfigurationException {
		logger.info("Setting all Credit EarnBack Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Credit EarnBack Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestCreditEarnBackCreation")
	public void testCreditEarnBackCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer creditEarnBackId = -1;

		try {
			logger.info("Validating Credit EarnBack Creation Flow [{}]", flowToTest);

			//Validate Credit EarnBack Creation
			logger.info("Creating Credit EarnBack for Flow [{}]", flowToTest);
			String createResponse = CreditEarnBack.createCreditEarnBack(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					creditEarnBackId = CreateEntity.getNewEntityId(createResponse, "creditearnbacks");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (creditEarnBackId != -1) {
							logger.info("Credit EarnBack Created Successfully with Id {}: ", creditEarnBackId);
							logger.info("Hitting Show API for Credit EarnBack Id {}", creditEarnBackId);
							Show showObj = new Show();
							showObj.hitShow(creditEarnBackEntityTypeId, creditEarnBackId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Credit EarnBack Id " + creditEarnBackId);
								}

								logger.info("Validating that Fields Contract and Supplier contains URL for Credit EarnBack Id {} and Flow [{}]", creditEarnBackId,
										flowToTest);
								//Verify Contract and Supplier Fields contain URL
								jsonObj = new JSONObject(showResponse);
								jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

								String url = jsonObj.getJSONObject("contract").getJSONObject("values").getString("url");
								if (url == null || url.trim().equalsIgnoreCase("")) {
									csAssert.assertTrue(false, "Contract doesn't contain URL for Credit EarnBack Id " + creditEarnBackId +
											" in Show Page API Response");
								}

								url = jsonObj.getJSONObject("supplier").getJSONObject("values").getString("url");
								if (url == null || url.trim().equalsIgnoreCase("")) {
									csAssert.assertTrue(false, "Supplier doesn't contain URL for Credit EarnBack Id " + creditEarnBackId +
											" in Show Page API Response");
								}

								logger.info("Validating that Fields Contract and Supplier are Non-Editable for Credit EarnBack Id {} and Flow [{}]", creditEarnBackId,
										flowToTest);
								//Verify Contract and Supplier Fields are non-editable
								Map<String, String> field = ParseJsonResponse.getFieldByName(showResponse, "contract");
								if (field.isEmpty()) {
									throw new SkipException("Couldn't get Attributes of Field Contract from Show Page API Response of Credit EarnBack Id " + creditEarnBackId);
								}

								if (field.get("displayMode") == null || !field.get("displayMode").trim().equalsIgnoreCase("display")) {
									csAssert.assertTrue(false, "Contract Field DisplayMode doesn't match Display in Show Page API Response " +
											"for Credit EarnBack Id " + creditEarnBackId);
								}

								field = ParseJsonResponse.getFieldByName(showResponse, "supplier");
								if (field.isEmpty()) {
									throw new SkipException("Couldn't get Attributes of Field Supplier from Show Page API Response of Credit EarnBack Id " + creditEarnBackId);
								}

								if (field.get("displayMode") == null || !field.get("displayMode").trim().equalsIgnoreCase("display")) {
									csAssert.assertTrue(false, "Supplier Field DisplayMode doesn't match Display in Show Page API Response " +
											"for Credit EarnBack Id " + creditEarnBackId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Credit EarnBack Id " + creditEarnBackId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Credit EarnBack for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Credit EarnBack Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Credit EarnBack Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Credit EarnBack Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && creditEarnBackId != -1) {
				logger.info("Deleting Credit EarnBack having Id {}", creditEarnBackId);
				EntityOperationsHelper.deleteEntityRecord("creditearnbacks", creditEarnBackId);
			}
			csAssert.assertAll();
		}
	}
}