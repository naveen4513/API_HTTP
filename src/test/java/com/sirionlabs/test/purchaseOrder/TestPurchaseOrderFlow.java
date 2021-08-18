package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.GetEntityId;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestPurchaseOrderFlow {

	private final static Logger logger = LoggerFactory.getLogger(TestPurchaseOrderFlow.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer poListId;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFlowTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFlowTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		poListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "purchase orders", "entity_url_id"));
	}

	@DataProvider
	public Object[][] dataProviderForTestPurchaseOrderFlow() throws ConfigurationException {
		logger.info("Setting all Purchase Order Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Purchase Order Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestPurchaseOrderFlow")
	public void testPurchaseOrderFlow(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Purchase Order Flow [{}]", flowToTest);

			//Validate PO Creation
			logger.info("Creating Purchase Order for Flow [{}]", flowToTest);
			String createResponse = PurchaseOrder.createPurchaseOrder(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

				if (createStatus.trim().equalsIgnoreCase("success")) {
					Integer purchaseOrderId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

					if (purchaseOrderId != -1) {
						//Validate Purchase Order on Listing Page
						if (!validatePurchaseOrderOnListingPage(flowToTest, purchaseOrderId)) {
							logger.error("Purchase Order Listing Page validation failed for Flow [{}].", flowToTest);
							csAssert.assertTrue(false, "Purchase Order Listing Page validation failed for Flow [" + flowToTest + "].");
						}

						logger.info("Hitting Show API for Purchase Order Id {} and Flow [{}]", purchaseOrderId, flowToTest);
						Show showObj = new Show();
						int entityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");
						showObj.hitShow(entityTypeId, purchaseOrderId);
						String showResponse = showObj.getShowJsonStr();

						//Validate Purchaser Order on Show Page
						if (!validatePurchaseOrderOnShowPage(flowToTest, purchaseOrderId, showResponse)) {
							logger.error("Purchase Order Show Page validation failed for Flow [{}].", flowToTest);
							csAssert.assertTrue(false, "Purchase Order Show Page validation failed for Flow [" + flowToTest + "].");
						}

						//Validate Purchase Order Quick Search
						logger.info("Getting Short Code for Purchase Order Id {} and Flow [{}]", purchaseOrderId, flowToTest);
						String shortCodeId = ShowHelper.getValueOfField(entityTypeId, "short code id", showResponse);

						if (!validatePurchaseOrderQuickSearch(flowToTest, purchaseOrderId, shortCodeId)) {
							logger.error("Purchase Order Quick Search validation failed for Flow [{}].", flowToTest);
							csAssert.assertTrue(false, "Purchase Order Quick Search validation failed for Flow [" + flowToTest + "].");
						}

						//Delete Purchase Order
						String deleteResponse = EntityOperationsHelper.getDeleteEntityResponse("purchase orders", purchaseOrderId);
						if (ParseJsonResponse.validJsonResponse(deleteResponse)) {
							JSONObject deleteObj = new JSONObject(deleteResponse);
							deleteObj = deleteObj.getJSONObject("header").getJSONObject("response");

							if (deleteObj.getString("status").trim().equalsIgnoreCase("success")) {
								//Validate on Deleting PO, it redirects to PO Listing Page
								if (!deleteObj.has("type") || !deleteObj.has("properties")) {
									csAssert.assertTrue(false, "Couldn't find Redirect Url in Delete API Response for Purchase Order Id " +
											purchaseOrderId + " and Flow [" + flowToTest + "]");
								} else {
									String expectedRedirectUrl = "/listRenderer/list/" + poListId;
									if (!deleteObj.getString("type").trim().equalsIgnoreCase("redirect") ||
											!deleteObj.getJSONObject("properties").getString("redirectUrl").trim().toLowerCase()
													.contains(expectedRedirectUrl.toLowerCase())) {
										csAssert.assertTrue(false, "Couldn't find Redirect Url of Purchase Order Listing Page in Delete API Response " +
												"for Purchase Order Id " + purchaseOrderId + " and Flow [" + flowToTest + "]");
									}
								}

								//Validate after Deleting PO, Show Page is not accessible.
								logger.info("Validating that Show Page of Purchase Order having Id {} is not accessible");
								showObj.hitShow(entityTypeId, purchaseOrderId);
								showResponse = showObj.getShowJsonStr();

								if (!ShowHelper.verifyShowPageOfDeletedRecord(showResponse)) {
									csAssert.assertTrue(false, "Show Page Validation of Deleted Purchase Order Id " + purchaseOrderId + " and Flow [" +
											flowToTest + "] failed.");
								}

								//Validate Purchase Order on Listing Page after the Entity has been deleted.
								if (validatePurchaseOrderOnListingPage(flowToTest, purchaseOrderId)) {
									logger.error("Purchase Order Listing Page Validation after deleting Entity has failed for Flow [{}]", flowToTest);
									csAssert.assertTrue(false, "Purchase Order Listing Page Validation after deleting Entity has failed for Flow [" +
											flowToTest + "]");
								}

								//Validate Purchase Order Quick Search after the Entity has been deleted.
								if (validatePurchaseOrderQuickSearch(flowToTest, purchaseOrderId, shortCodeId)) {
									logger.error("Purchase Order Quick Search validation after deleting Entity has failed for Flow [{}].", flowToTest);
									csAssert.assertTrue(false, "Purchase Order Quick Search validation after deleting Entity has failed for Flow [" +
											flowToTest + "]");
								}
							} else {
								throw new SkipException("Purchase Order having Id " + purchaseOrderId + " couldn't be deleted for Flow [" + flowToTest +
										"]. Hence skipping remaining tests.");
							}
						} else {
							logger.error("Delete API Response for Purchase Order having Id {} and Flow [{}] is an Invalid JSON.", purchaseOrderId, flowToTest);
							csAssert.assertTrue(false, "Delete API Response for Purchase Order having Id " + purchaseOrderId + " and Flow [" +
									flowToTest + "] is an Invalid JSON.");
						}
					} else {
						throw new SkipException("Couldn't get Id of Newly Created Purchase Order for Flow [" + flowToTest + "]. Hence skipping test.");
					}
				} else {
					logger.error("Couldn't create Purchase Order for Flow [{}] due to {}", flowToTest, createStatus);
					csAssert.assertTrue(false, "Couldn't create Purchase Order for Flow [" + flowToTest + "] due to " + createStatus);
				}
			} else {
				logger.error("Purchase Order Create API Response for Flow [{}] is an Invalid JSON.", flowToTest);
				csAssert.assertTrue(false, "Purchase Order Create API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Purchase Order Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			csAssert.assertAll();
		}
	}

	private Boolean validatePurchaseOrderOnListingPage(String flowToTest, Integer purchaseOrderId) {
		boolean listingPageTestResult = false;

		try {
			//Test PO on Listing Page
			logger.info("Validating Purchase Order Record having Id {} on Listing Page for Flow [{}].", purchaseOrderId, flowToTest);
			ListRendererListData listDataObj = new ListRendererListData();
			Map<String, String> params = new HashMap<>();
			String sourceEntity = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceEntity");
			if (sourceEntity.trim().equalsIgnoreCase("contracts")) {
				String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceId");
				params.put("contractId", contractId);
			} else if (sourceEntity.trim().equalsIgnoreCase("suppliers")) {
				String supplierId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceId");
				params.put("relationId", supplierId);
			}

			int entityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");
			String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":" +
					"\"desc\",\"filterJson\":{}}}";

			logger.info("Hitting List Data API for Purchase Order Flow [{}]", flowToTest);
			listDataObj.hitListRendererListData(poListId, payload, params);
			String listDataJsonStr = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
				listDataObj.setListData(listDataJsonStr);

				List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
				if (listData.size() > 0) {
					int columnId = listDataObj.getColumnIdFromColumnName("name");

					for (Map<Integer, Map<String, String>> oneData : listData) {
						if (oneData.get(columnId).get("valueId").trim().equalsIgnoreCase(String.valueOf(purchaseOrderId))) {
							listingPageTestResult = true;
							break;
						}
					}

					if (!listingPageTestResult) {
						logger.error("Couldn't find newly created Purchase Order having Id {} on Listing Page for Flow [{}]", purchaseOrderId, flowToTest);
					}
				} else {
					logger.error("No Record found in Invoice Listing for Flow [{}]", flowToTest);
				}
			} else {
				logger.error("Purchase Order List Data API Response for Flow [{}] is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order on Listing Page for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return listingPageTestResult;
	}

	private Boolean validatePurchaseOrderOnShowPage(String flowToTest, Integer purchaseOrderId, String showResponse) {
		Boolean showPageTestResult = true;

		try {
			//Test PO on Show Page
			logger.info("Validating Purchase Order Record having Id {} on Show Page for Flow [{}].", purchaseOrderId, flowToTest);
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

			if (ParseJsonResponse.validJsonResponse(showResponse)) {
				//Validate Id
				String idOnShowPage = ShowHelper.getValueOfField("id", showResponse);
				logger.info("Expected Id on Show Page: {} and Actual Id on Show Page: {}", purchaseOrderId, idOnShowPage);

				if (!idOnShowPage.trim().equals(purchaseOrderId.toString().trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field ID.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}

				//Validate Name
				String valueOnShowPage = ShowHelper.getValueOfField(entityTypeId, "name", showResponse);
				String expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedNameOnShowPage");
				logger.info("Expected Name on Show Page: {} and Actual Name on Show Page: {}", expectedValue, valueOnShowPage);

				if (!valueOnShowPage.trim().equalsIgnoreCase(expectedValue.trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field Name.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}

				//Validate PONumber
				valueOnShowPage = ShowHelper.getValueOfField(entityTypeId, "poNumber", showResponse);
				expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedPoNumberOnShowPage");
				logger.info("Expected PO Number on Show Page: {} and Actual PO Number on Show Page: {}", expectedValue, valueOnShowPage);

				if (!valueOnShowPage.trim().equalsIgnoreCase(expectedValue.trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field PO Number.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}

				//Validate Status
				valueOnShowPage = ShowHelper.getValueOfField(entityTypeId, "status", showResponse);
				expectedValue = "Newly Created";
				logger.info("Expected Status on Show Page: {} and Actual Status on Show Page: {}", expectedValue, valueOnShowPage);

				if (!valueOnShowPage.trim().equalsIgnoreCase(expectedValue.trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field Status.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}

				//Validate Source/Parent Entity Name
				valueOnShowPage = ShowHelper.getValueOfField(entityTypeId, "parent entity name", showResponse);
				expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceEntity");
				logger.info("Expected Parent Entity Name on Show Page: {} and Actual Parent Entity Name on Show Page: {}", expectedValue, valueOnShowPage);

				if (!valueOnShowPage.trim().equalsIgnoreCase(expectedValue.trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field Parent Entity Name.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}

				//Validate Source/Parent Entity Id
				valueOnShowPage = ShowHelper.getValueOfField(entityTypeId, "parent entity id", showResponse);
				expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "sourceId");
				logger.info("Expected Parent Entity Id on Show Page: {} and Actual Parent Entity Id on Show Page: {}", expectedValue, valueOnShowPage);

				if (!valueOnShowPage.trim().equalsIgnoreCase(expectedValue.trim())) {
					logger.error("Purchase Order Show Page validation for Flow [{}] and Id {} failed for Field Parent Entity Id.", flowToTest, purchaseOrderId);
					showPageTestResult = false;
				}
			} else {
				logger.error("Purchase Order Show Page API Response for Id {} and Flow [{}] is an Invalid JSON.", purchaseOrderId, flowToTest);
				showPageTestResult = false;
			}
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order on Show Page for Flow [{}]. {}", flowToTest, e.getStackTrace());
			showPageTestResult = false;
		}
		return showPageTestResult;
	}

	private Boolean validatePurchaseOrderQuickSearch(String flowToTest, Integer purchaseOrderId, String shortCodeId) {
		Boolean quickSearchTestResult = false;

		try {
			//Test PO Quick Search
			logger.info("Validating Purchase Order Quick Search for Id {} and Flow [{}]", purchaseOrderId, flowToTest);
			logger.info("Hitting GetEntityId API for Purchase Order with Short Code Id {} and Flow [{}]", shortCodeId, flowToTest);
			GetEntityId obj = new GetEntityId();
			String entityIdResponse = obj.hitGetEntityId("purchase orders", shortCodeId);

			if (entityIdResponse != null && NumberUtils.isParsable(entityIdResponse)) {
				if (entityIdResponse.trim().equalsIgnoreCase(purchaseOrderId.toString().trim()))
					quickSearchTestResult = true;
			} else {
				logger.error("Couldn't get Purchase Order Id from GetEntityId API for Short Code {} and Flow [{}].", shortCodeId, flowToTest);
			}
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order Quick Search for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return quickSearchTestResult;
	}
}
