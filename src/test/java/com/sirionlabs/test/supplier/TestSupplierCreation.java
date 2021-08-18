package com.sirionlabs.test.supplier;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.UpdateFile;
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
public class TestSupplierCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestSupplierCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer supplierEntityTypeId;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SupplierCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		supplierEntityTypeId = ConfigureConstantFields.getEntityIdByName("suppliers");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestSupplierCreation() throws ConfigurationException {
		logger.info("Setting all Supplier Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Supplier Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestSupplierCreation")
	public void testSupplierCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer supplierId = -1;

		try {
			logger.info("Validating Supplier Creation Flow [{}]", flowToTest);

			//Validate Supplier Creation
			logger.info("Creating Supplier for Flow [{}]", flowToTest);
			String createResponse = Supplier.createSupplier(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					supplierId = CreateEntity.getNewEntityId(createResponse, "suppliers");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (supplierId != -1) {
							logger.info("Supplier Created Successfully with Id {}: ", supplierId);
							logger.info("Hitting Show API for Supplier Id {}", supplierId);
							Show showObj = new Show();
							showObj.hitShow(supplierEntityTypeId, supplierId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Supplier Id " + supplierId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Supplier Id " + supplierId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Supplier for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Supplier Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Supplier Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Supplier Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && supplierId != -1) {
				logger.info("Deleting Supplier having Id {}", supplierId);
				EntityOperationsHelper.deleteEntityRecord("suppliers", supplierId);
			}
			csAssert.assertAll();
		}
	}

}
