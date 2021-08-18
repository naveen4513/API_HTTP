package com.sirionlabs.test.invoice.flow;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

//@Listeners(value = MyTestListenerAdapter.class)
public class TestServiceData extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceData.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static int serviceDataListId;
	private static List<Integer> entityIdsToDelete;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		serviceDataListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "service data", "entity_url_id"));

		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity").trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		entityIdsToDelete = new ArrayList<>();
		testCasesMap = getTestCasesMapping();
	}

	@AfterClass
	public void afterClass() {
		//Delete All Newly Created Service Data
		for (Integer serviceDataId : entityIdsToDelete) {
			logger.info("Deleting Service Data having Id {}.", serviceDataId);
			Boolean entityDeleted = EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId);
			if (entityDeleted) {
				logger.info("Service Data Record having Id {} is deleted Successfully.", serviceDataId);
			} else {
				logger.error("Couldn't delete Service Data Record having Id {}.", serviceDataId);
			}
		}
	}

//	@DataProvider
//	public Object[][] dataProviderForTestServiceData() throws ConfigurationException {
//		logger.info("Setting all Service Data Flows to Validate");
//		List<Object[]> allTestData = new ArrayList<>();
//		List<String> flowsToTest = new ArrayList<>();
//
//		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
//		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
//			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
//			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
//		} else {
//			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
//			for (String flow : allFlows) {
//				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
//					flowsToTest.add(flow.trim());
//				} else {
//					logger.info("Flow having name [{}] not found in Service Data Config File.", flow.trim());
//				}
//			}
//		}
//
//		for (String flowToTest : flowsToTest) {
//			allTestData.add(new Object[]{flowToTest});
//		}
//		return allTestData.toArray(new Object[0][]);
//	}

//	@Test(dataProvider = "dataProviderForTestServiceData")
//	public void testServiceData(String flowToTest) {
	@Test(enabled = false)
	public void testServiceData() {
		//CustomAssert csAssert = new CustomAssert();
		CustomAssert csAssert = new CustomAssert();
		List<String> flowsToTest = new ArrayList<>();
		try {
			logger.info("Setting all Service Data Flows to Validate");
			List<Object[]> allTestData = new ArrayList<>();


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
						logger.info("Flow having name [{}] not found in Service Data Config File.", flow.trim());
					}
				}
			}
		}catch (Exception e) {
			logger.error("Exception while creating data provider");
			addTestResult(getTestCaseIdForMethodName("testDocumentsAPI"), csAssert);
			return;
		}


		for (String flowToTest : flowsToTest) {

			try {
				logger.info("Validating Service Data for Flow [{}].", flowToTest);
				logger.info("Creating Service Data for Flow [{}].", flowToTest);
				String jsonStr = InvoiceHelper.getServiceDataCreateResponseForExistingContract(configFilePath, configFileName, extraFieldsConfigFileName,flowToTest);

				if (ParseJsonResponse.validJsonResponse(jsonStr)) {
					JSONObject jsonObj = new JSONObject(jsonStr);
					String responseStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
					String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");

					if (expectedResult.trim().equalsIgnoreCase("Success")) {
						if (responseStatus.equalsIgnoreCase("success")) {
							int serviceDataId = CreateEntity.getNewEntityId(jsonStr);
							if (serviceDataId != -1) {
								//Test Service Data in Listing
								logger.info("Validating Service Data Record having Id {} on Listing Page for Flow [{}].", serviceDataId, flowToTest);
								ListRendererListData listDataObj = new ListRendererListData();
								Map<String, String> params = new HashMap<>();
								String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceId");
								params.put("contractId", contractId);
								int entityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
								String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":" +
										"\"desc nulls last\",\"filterJson\":{}}}";

								logger.info("Hitting List Data API for Service Data Flow [{}]", flowToTest);
								listDataObj.hitListRendererListData(serviceDataListId, payload, params);
								String listDataJsonStr = listDataObj.getListDataJsonStr();
								listDataObj.setListData(listDataJsonStr);

								List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
								if (listData.size() > 0) {
									int columnId = listDataObj.getColumnIdFromColumnName("id");
									Boolean recordFound = false;

									for (Map<Integer, Map<String, String>> oneData : listData) {
										if (oneData.get(columnId).get("valueId").trim().equalsIgnoreCase(String.valueOf(serviceDataId))) {
											recordFound = true;
											break;
										}
									}

									if (!recordFound) {
										logger.error("Couldn't find newly created Service Data having Id {} on Listing Page for Flow [{}]", serviceDataId, flowToTest);
										csAssert.assertTrue(false, "Couldn't find newly created Service Data having Id " + serviceDataId +
												" on Listing Page for Flow [" + flowToTest + "]");
									}
								} else {
									logger.error("No Record found in Service Data Listing for Flow [{}]", flowToTest);
									csAssert.assertTrue(false, "No Record found in Service Data Listing for Flow [" + flowToTest + "]");
								}

								if (deleteEntity) {
									//Add Newly Created Service Data to Delete Ids List
									entityIdsToDelete.add(serviceDataId);
								}
							} else {
								logger.error("Couldn't get Id of newly created Service Data for Flow [{}]", flowToTest);
								csAssert.assertTrue(false, "Couldn't get Id of newly created Service Data for Flow [" + flowToTest + "]");
							}
						} else {
							logger.error("Couldn't create Service Data for Flow [{}].", flowToTest);
							csAssert.assertTrue(false, "Couldn't create Service Data for Flow [" + flowToTest + "]");
						}
					} else {
						//Check Negative cases
						if (!responseStatus.equalsIgnoreCase("success")) {
							logger.info("Couldn't create Service Data for Flow [{}] due to {}", flowToTest, responseStatus);
						} else {
							logger.error("Expected Result is Failure and Still Service Data Created for Flow [{}].", flowToTest);
							csAssert.assertTrue(false, "Expected Result is Failure and Still Service Data Created for Flow [" + flowToTest + "]");
						}
					}
				} else {
					logger.error("Service Data Creation API Response for Flow [{}] is an Invalid JSON.", flowToTest);
					csAssert.assertTrue(false, "Service Data Creation API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
				}
			} catch (Exception e) {
				logger.error("Exception while Validating Service Data for Flow [{}]. {}", flowToTest, e.getStackTrace());
//			csAssert.assertTrue(false, "Exception while Validating Service Data for Flow [" + flowToTest + "]. " + e.getMessage());
				csAssert.assertTrue(false, "Exception while Validating Service Data for Flow [" + flowToTest + "]. " + e.getMessage());
			}
		}
		addTestResult(getTestCaseIdForMethodName("testServiceData"), csAssert);
		csAssert.assertAll();
	}
}
