package com.sirionlabs.test.changeRequest;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
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
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestChangeRequestCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestChangeRequestCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer crEntityTypeId;
	private static Boolean deleteEntity = true;

	private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CRCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CRCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		crEntityTypeId = ConfigureConstantFields.getEntityIdByName("change requests");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestCRCreation() throws ConfigurationException {
		logger.info("Setting all CR Creation Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
		} else {
			String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in CR Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestCRCreation")
	public void testCRCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		int crId = -1;

		try {
			logger.info("Validating CR Creation Flow [{}]", flowToTest);

			//Validate CR Creation
			logger.info("Creating CR for Flow [{}]", flowToTest);
			String createResponse = ChangeRequest.createChangeRequest(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					crId = CreateEntity.getNewEntityId(createResponse, "change requests");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (crId != -1) {
							logger.info("CR Created Successfully with Id {}: ", crId);
							logger.info("Hitting Show API for CR Id {}", crId);
							Show showObj = new Show();
							showObj.hitShow(crEntityTypeId, crId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + crId);
								}

								Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
								String sourceEntity = flowProperties.get("sourceentity");
								int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
								int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

								//Validate Source Reference Tab.
								tabListDataHelperObj.validateSourceReferenceTab("change requests", 63, crId, parentEntityTypeId,
										parentRecordId, csAssert);

								//Validate Forward Reference Tab.
								tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 63, crId, csAssert);

								if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
									//Validate Supplier on Show Page
									String expectedSupplierId = flowProperties.get("multiparentsupplierid");
									ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 63, crId, csAssert);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for CR Id " + crId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create CR for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "CR Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating CR Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && crId != -1) {
				logger.info("Deleting CR having Id {}", crId);
				EntityOperationsHelper.deleteEntityRecord("change requests", crId);
			}
			csAssert.assertAll();
		}
	}
}
