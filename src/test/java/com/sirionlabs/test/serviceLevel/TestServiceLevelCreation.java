package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityCreation.ServiceLevel;
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


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestServiceLevelCreation {
	private final static Logger logger = LoggerFactory.getLogger(TestServiceLevelCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer slEntityTypeId;
	private static Boolean deleteEntity = true;

	private TabListDataHelper tabListDataHelperObj = new TabListDataHelper();

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;
	}

	@DataProvider
	public Object[][] dataProviderForTestSLCreation() throws ConfigurationException {
		logger.info("Setting all Service Level Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Service Level Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestSLCreation")
	public void testServiceLevelCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		int serviceLevelId = -1;

		String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
		String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
		String uniqueString = DateUtils.getCurrentTimeStamp();

		uniqueString = uniqueString.replaceAll("_", "");
		uniqueString = uniqueString.replaceAll(" ", "");
		uniqueString = uniqueString.replaceAll("0", "1");

		uniqueString = uniqueString.substring(10);

		try {
			logger.info("Validating Service Level Creation Flow [{}]", flowToTest);

			//Validate Service Level Creation
			logger.info("Creating Service Level for Flow [{}]", flowToTest);
			String createResponse = "";

			if(filter_name != null) {
				String dynamicField = "dyn" + filter_name;

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

				createResponse = ServiceLevel.createServiceLevel(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString,"unqString");
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString,"unqString");

			}else {
				createResponse = ServiceLevel.createServiceLevel(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);
			}


			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					serviceLevelId = CreateEntity.getNewEntityId(createResponse, "service levels");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (serviceLevelId != -1) {
							logger.info("Service Level Created Successfully with Id {}: ", serviceLevelId);
							logger.info("Hitting Show API for Service Level Id {}", serviceLevelId);

							if(filter_name!=null) {
								ListRendererListData listRendererListData = new ListRendererListData();
								String min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
								String max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();
								String payload = "";

								Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(14, filter_id,
										filter_name, min, max, payload, csAssert);

								String entityId = "";
								try {
									entityId = listColumnValuesMap.get("id").split(":;")[1];

								} catch (Exception e) {

								}

								if (!entityId.equalsIgnoreCase(String.valueOf(serviceLevelId))) {
									csAssert.assertTrue(false, "On Listing page serviceLevelId entity " + serviceLevelId + " Not Found After Applying Automation Numeric Filter with values " + min + " and "+ max);
								} else {
									logger.info("On Listing page Service Level entity " + serviceLevelId + "  Found");
								}

							}
							Show showObj = new Show();
							showObj.hitShow(slEntityTypeId, serviceLevelId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Service Level Id " + serviceLevelId);
								}

								Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
								String sourceEntity = flowProperties.get("sourceentity");
								int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntity);
								int parentRecordId = Integer.parseInt(flowProperties.get("sourceid"));

								//Validate Source Reference Tab.
								tabListDataHelperObj.validateSourceReferenceTab("service levels", 14, serviceLevelId, parentEntityTypeId,
										parentRecordId, csAssert);

								//Validate Forward Reference Tab.
								tabListDataHelperObj.validateForwardReferenceTab(sourceEntity, parentEntityTypeId, parentRecordId, 14,
										serviceLevelId, csAssert);

								if (flowProperties.containsKey("multisupplier") && flowProperties.get("multisupplier").trim().equalsIgnoreCase("true")) {
									//Validate Supplier on Show Page
									String expectedSupplierId = flowProperties.get("multiparentsupplierid");
									ShowHelper.verifyShowField(showResponse, "supplier id", expectedSupplierId, 14, serviceLevelId, csAssert);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Service Level Id " + serviceLevelId + " is an Invalid JSON.");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Service Level for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Service Level Created for Flow [" + flowToTest + "] whereas it was expected not to create.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Service Level Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Service Level Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && serviceLevelId != -1) {
				logger.info("Deleting Service Level having Id {}", serviceLevelId);
				Boolean deletionFlag = EntityOperationsHelper.deleteEntityRecord("service levels", serviceLevelId);

				if(deletionFlag) {

					if(filter_name !=null) {
						ListRendererListData listRendererListData = new ListRendererListData();
						String min = new BigDecimal(uniqueString).subtract(new BigDecimal("2")).toString();
						String max = new BigDecimal(uniqueString).add(new BigDecimal("1")).toString();
						String payload = "";
						Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(14, filter_id,
								filter_name, min, max, payload, csAssert);

						String entityId = "";
						try {
							entityId = listColumnValuesMap.get("id").split(":;")[1];

						} catch (Exception e) {

						}

						if (entityId.equalsIgnoreCase(String.valueOf(serviceLevelId))) {
							csAssert.assertTrue(false, "On Listing page serviceLevelId entity " + serviceLevelId + "  Found after deletion After Applying Automation Numeric Filter with values " + min + " and "+ max);
						} else {
							logger.info("On Listing page Service Level entity " + serviceLevelId + "  not Found after deletion");
						}
					}
				}else {
					csAssert.assertTrue(false,"Error while entity deletion");
				}

			}
			csAssert.assertAll();
		}
	}
}
