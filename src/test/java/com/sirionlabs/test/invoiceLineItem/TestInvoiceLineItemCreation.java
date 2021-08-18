package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.UpdateConfigFiles;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer lineItemEntityTypeId;
	private static Integer lineItemListId;
	private static List<Integer> entityIdsToDelete;
	private static Boolean deleteEntity = true;
	private String listingPayloadConfigFilePath;
	private String listingPayloadConfigFileName;
	private String invoiceLineItem = "invoice line item";

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("LineItemCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("LineItemCreationTestConfigFileName");

		listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");
		listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");

		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");
		lineItemListId = ConfigureConstantFields.getListIdForEntity("invoice line item");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		entityIdsToDelete = new ArrayList<>();
	}

	@DataProvider
	public Object[][] dataProviderForTestInvoiceLineItemCreation() throws ConfigurationException {
		logger.info("Setting all Invoice Line Item Creation Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Invoice Line Item Creation Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestInvoiceLineItemCreation")
	public void testInvoiceLineItemCreation(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer lineItemId = -1;
		ListRendererListData listRendererListData = new ListRendererListData();

		String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, "dynamic filter name");
		String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath, extraFieldsConfigFileName, "dynamic filter id");
		String uniqueString = DateUtils.getCurrentTimeStamp();

		String min = "1";
		String max = "100000";
		/*{

			String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "payload");
			String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "columnidstoignore");
			payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);

			Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(lineItemEntityTypeId, filter_id,
					filter_name, min, max, payload, csAssert);
			System.out.println(" ");
		}*/
		try {

			logger.info("Validating Invoice Line Item Creation Flow [{}]", flowToTest);

			//Validate Invoice Line Item Creation
			logger.info("Creating Invoice Line Item for Flow [{}]", flowToTest);

			uniqueString = uniqueString.replaceAll("_", "");
			uniqueString = uniqueString.replaceAll(" ", "");

			uniqueString = uniqueString.substring(10);
			uniqueString = uniqueString.replaceAll("0", "1");

			String createResponse = "";

			if (filter_name != null) {
				String dynamicField = "dyn" + filter_name;

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);

				createResponse = InvoiceLineItem.createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);


				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString, "unqString");
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString, "unqString");

			} else {
				createResponse = InvoiceLineItem.createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);
			}


			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					lineItemId = CreateEntity.getNewEntityId(createResponse, "invoice line item");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (lineItemId != -1) {
							logger.info("Invoice Line Item Created Successfully with Id {}: ", lineItemId);

							if (deleteEntity) {
								//Add Newly Created Invoice Line Item to Delete Ids List
								entityIdsToDelete.add(lineItemId);
							}
							if (filter_name != null) {

								min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
								max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();

								String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "payload");
								String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "columnidstoignore");
								payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);

								Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(lineItemEntityTypeId, filter_id,
										filter_name, min, max, payload, csAssert);

								String entityId = "";
								try {
									entityId = listColumnValuesMap.get("id").split(":;")[1];

								} catch (Exception e) {

								}

								if (!entityId.equalsIgnoreCase(String.valueOf(lineItemId))) {
									csAssert.assertTrue(false, "On Listing page Invoice Line Item entity " + lineItemId + "  Not Found After Creation");
								} else {
									logger.info("On Listing page Invoice Line Item entity " + lineItemId + /*Not*/" Found After Creation");
								}

								String recordPresent = listRendererListData.chkPartRecIsPresentForDiffUser(lineItemEntityTypeId, filter_id,
										filter_name, min, max, payload, csAssert);

								if(recordPresent.equalsIgnoreCase("Yes") ){
									csAssert.assertTrue(false,"Line Item ID Record present for different user where is it is not supposed to present for different user");
								}
							}

							//Validate Show Page is Accessible for Newly Created Invoice Line Item
							logger.info("Hitting Show API for Invoice Line Item Id {}", lineItemId);
							Show showObj = new Show();
							showObj.hitShow(lineItemEntityTypeId, lineItemId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Invoice Line Item Id " + lineItemId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Invoice Line Item Id " + lineItemId + " is an Invalid JSON.");
							}

							//Test Invoice Line Item in Listing
							logger.info("Validating Invoice Line Item Record having Id {} on Listing Page for Flow [{}].", lineItemId, flowToTest);
							ListRendererListData listDataObj = new ListRendererListData();
							Map<String, String> params = new HashMap<>();
							String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractId");
							params.put("contractId", contractId);
							int entityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");
							String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
									"\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

							logger.info("Hitting List Data API for Invoice Line Item Flow [{}]", flowToTest);
							listDataObj.hitListRendererListData(lineItemListId, payload, params);
							String listDataJsonStr = listDataObj.getListDataJsonStr();
							listDataObj.setListData(listDataJsonStr);

							List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
							if (listData.size() > 0) {
								int columnId = listDataObj.getColumnIdFromColumnName("id");
								Boolean recordFound = false;

								for (Map<Integer, Map<String, String>> oneData : listData) {
									if (oneData.get(columnId).get("valueId").trim().equalsIgnoreCase(String.valueOf(lineItemId))) {
										recordFound = true;
										break;
									}
								}

								if (!recordFound) {
									logger.error("Couldn't find newly created Invoice Line Item having Id {} on Listing Page for Flow [{}]", lineItemId, flowToTest);
									csAssert.assertTrue(false, "Couldn't find newly created Invoice Line Item having Id " + lineItemId +
											" on Listing Page for Flow [" + flowToTest + "]");
								}
							} else {
								logger.error("No Record found in Invoice Line Item Listing for Flow [{}]", flowToTest);
								csAssert.assertTrue(false, "No Record found in Invoice Line Item Listing for Flow [" + flowToTest + "]");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Invoice Line Item for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Invoice Line Item Created for Flow [" + flowToTest + "] whereas it was expected not to create.");

//						if (deleteEntity) {
//							//Add Newly Created Invoice Line Item to Delete Ids List
//							entityIdsToDelete.add(lineItemId);
//						}
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Invoice Line Item Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Line Item Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {

			//Deletion Operation
			for (Integer invoiceLineItemId : entityIdsToDelete){
				if (deleteEntity && invoiceLineItemId != -1) {
					EntityOperationsHelper.deleteEntityRecord("invoice line item", invoiceLineItemId);
				}
			}


			if (filter_name != null) {
				String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "payload");
				String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, invoiceLineItem, "columnidstoignore");
				payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);
                //payload = "";
				Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(lineItemEntityTypeId, filter_id,
						filter_name, uniqueString, uniqueString, payload, csAssert);

				String entityId = "";
				try {
					entityId = listColumnValuesMap.get("id").split(":;")[1];

				} catch (Exception e) {

				}

				if (entityId.equalsIgnoreCase(String.valueOf(lineItemId))) {
					csAssert.assertTrue(false, "On Listing page Line Item entity " + lineItemId + "  Found After Deletion");
				} else {
					logger.info("On Listing page PO entity " + lineItemId + " Not Found After Deletion");
				}
			}
			csAssert.assertAll();
		}
	}
}
