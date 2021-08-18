package com.sirionlabs.test.dispute;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Dispute;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
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
public class TestDisputeCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestDisputeCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer disputeEntityTypeId;
	private static Boolean deleteEntity = true;

	private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DisputeCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("DisputeCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		disputeEntityTypeId = ConfigureConstantFields.getEntityIdByName("disputes");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestDisputeCreation() throws ConfigurationException {
		logger.info("Setting all Dispute Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Dispute Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestDisputeCreation")
	public void testDisputeCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		int disputeId = -1;
		String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
		String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
		String uniqueString = DateUtils.getCurrentTimeStamp();

		uniqueString = uniqueString.replaceAll("_", "");
		uniqueString = uniqueString.replaceAll(" ", "");
		uniqueString = uniqueString.replaceAll("0", "1");
		uniqueString = uniqueString.substring(10);
		String min = "1";
		String max = "100000";

		try {
			logger.info("Validating Dispute Creation Flow [{}]", flowToTest);

			//Validate Dispute Creation
			logger.info("Creating Dispute for Flow [{}]", flowToTest);
			String createResponse;
			if(filter_name != null) {
				String dynamicField = "dyn" + filter_name;

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

			}

			createResponse = Dispute.createDispute(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					disputeId = CreateEntity.getNewEntityId(createResponse, "disputes");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (disputeId != -1) {
							logger.info("Dispute Created Successfully with Id {}: ", disputeId);
							logger.info("Hitting Show API for Dispute Id {}", disputeId);
							Show showObj = new Show();
							showObj.hitShow(disputeEntityTypeId, disputeId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Dispute Id " + disputeId);
								}

								Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
								String sourceEntity = flowProperties.get("sourceentity");
								int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
								int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

								//Validate Source Reference Tab.
								tabListDataHelperObj.validateSourceReferenceTab("disputes", 28, disputeId, parentEntityTypeId, parentRecordId, csAssert);

								//Validate Forward Reference Tab.
								tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 28, disputeId, csAssert);

								if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
									//Validate Supplier on Show Page
									String expectedSupplierId = flowProperties.get("multiparentsupplierid");
									ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 28, disputeId, csAssert);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Dispute Id " + disputeId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Dispute for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Dispute Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Dispute Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Dispute Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && disputeId != -1) {
				logger.info("Deleting Dispute having Id {}", disputeId);
				EntityOperationsHelper.deleteEntityRecord("disputes", disputeId);
			}
			csAssert.assertAll();
		}
	}
}