package com.sirionlabs.test.contractTemplateStructure;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.ContractTemplateStructure;
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
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestContractTemplateStructureCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestContractTemplateStructureCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer contractTemplateStructureEntityTypeId;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateStructureCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateStructureCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		contractTemplateStructureEntityTypeId = ConfigureConstantFields.getEntityIdByName("contract template structure");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestContractTemplateStructureCreation() throws ConfigurationException {
		logger.info("Setting all Contract Template Structure Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Contract Template Structure Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestContractTemplateStructureCreation")
	public void testContractTemplateStructureCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer ctsId = -1;

		try {
			logger.info("Validating Contract Template Structure Creation Flow [{}]", flowToTest);

			//Validate Contract Template Structure Creation
			logger.info("Creating Contract Template Structure for Flow [{}]", flowToTest);
			String createResponse = ContractTemplateStructure.createContractTemplateStructure(configFilePath, configFileName, extraFieldsConfigFilePath,
					extraFieldsConfigFileName, flowToTest, false);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					ctsId = CreateEntity.getNewEntityId(createResponse, "contract template structure");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (ctsId != -1) {
							logger.info("Contract Template Structure Created Successfully with Id {}: ", ctsId);
							logger.info("Hitting Show API for Contract Template Structure Id {}", ctsId);
							Show showObj = new Show();
							showObj.hitShow(contractTemplateStructureEntityTypeId, ctsId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Contract Template Structure Id " + ctsId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Contract Template Structure Id " + ctsId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Contract Template Structure for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Contract Template Structure Created for Flow [" + flowToTest +
								"] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Contract Template Structure Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Contract Template Structure Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && ctsId != -1) {
				logger.info("Deleting Contract Template Structure having Id {}", ctsId);
				EntityOperationsHelper.deleteEntityRecord("contract template structure", ctsId);
			}
			csAssert.assertAll();
		}
	}
}