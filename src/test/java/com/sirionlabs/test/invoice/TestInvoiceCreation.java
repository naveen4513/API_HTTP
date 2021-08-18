package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Invoice;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
public class TestInvoiceCreation {

	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer invoiceEntityTypeId;
	private static Integer invoiceListId;
	private static List<Integer> entityIdsToDelete;
	private static Boolean deleteEntity = true;
	private String invoice = "invoice";
	String listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");
	String listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreationTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceCreationTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilepath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extrafieldsconfigfilename");
		invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
		invoiceListId = ConfigureConstantFields.getListIdForEntity("invoices");

		listingPayloadConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFileName");
		listingPayloadConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListingPayloadConfigFilePath");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		entityIdsToDelete = new ArrayList<>();
	}



	@Test()
	public void testInvoiceCreation() {
		CustomAssert csAssert = new CustomAssert();
		Integer invoiceId = -1;
		String flowToTest = "fixed fee flow 1";

		String filter_name = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter name");
		String filter_id = ParseConfigFile.getValueFromConfigFile(extraFieldsConfigFilePath,extraFieldsConfigFileName,"dynamic filter id");
		String uniqueString = DateUtils.getCurrentTimeStamp();

		String min = "1";
		String max = "100000";

		try {

			//Validate Invoice Line Item Creation


			uniqueString = uniqueString.replaceAll("_", "");
			uniqueString = uniqueString.replaceAll(" ", "");

			uniqueString = uniqueString.substring(10);
			uniqueString = uniqueString.replace("0","2");

			String createResponse = "";
			ListRendererListData listRendererListData = new ListRendererListData();

			if(filter_name != null) {
				String dynamicField = "dyn" + filter_name;

				min = new BigDecimal(uniqueString).subtract(new BigDecimal("5")).toString();
				max = new BigDecimal(uniqueString).add(new BigDecimal("5")).toString();

				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, "unqString", uniqueString);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", "unqString", uniqueString);
				createResponse = Invoice.createInvoice(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", dynamicField, uniqueString,"unqString");
				UpdateFile.updateConfigFileProperty(extraFieldsConfigFilePath, extraFieldsConfigFileName, "common extra fields", "dynamicMetadata", uniqueString,"unqString");

			}else {
				createResponse = InvoiceLineItem.createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
						true);
			}


			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult");
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (createStatus.equalsIgnoreCase("success"))
					invoiceId = CreateEntity.getNewEntityId(createResponse, "invoices");

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						if (invoiceId != -1) {
							logger.info("Invoice Created Successfully with Id {}: ", invoiceId);

							if (deleteEntity) {
								//Add Newly Created Invoice Line Item to Delete Ids List
								entityIdsToDelete.add(invoiceId);
							}

							int colId = 203;
							String generalPayload = "{\"filterMap\":{\"entityTypeId\":67,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
									"\"filterJson\":{}},\"selectedColumns\":" +
									"[{\"columnId\":14556,\"columnQueryName\":\"bulkcheckbox\"}," +
									"{\"columnId\":" + colId + ",\"columnQueryName\":\"id\"}]}";
//							listRendererListData.checkRecFoundOnListPage(invoiceListId,colId,invoiceId,generalPayload,csAssert);

							if(filter_name !=null) {
								if (!filter_name.equals("")) {
									listRendererListData.checkRecFoundOnListPage(invoiceListId,colId,invoiceId,generalPayload,csAssert);
								}
							}
							//Validate Show Page is Accessible for Newly Created Invoice Line Item
							logger.info("Hitting Show API for Invoice Id {}", invoiceId);
							Show showObj = new Show();
							showObj.hitShow(invoiceEntityTypeId, invoiceId);
							String showResponse = showObj.getShowJsonStr();

							if (ParseJsonResponse.validJsonResponse(showResponse)) {
								if (!showObj.isShowPageAccessible(showResponse)) {
									csAssert.assertTrue(false, "Show Page is Not Accessible for Invoice Id " + invoiceId);
								}
							} else {
								csAssert.assertTrue(false, "Show API Response for Invoice Id " + invoiceId + " is an Invalid JSON.");
							}

							//Test Invoice Line Item in Listing
							logger.info("Validating Invoice Record having Id {} on Listing Page for Flow [{}].", invoiceId, flowToTest);
							ListRendererListData listDataObj = new ListRendererListData();
							Map<String, String> params = new HashMap<>();
							String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractId");
							params.put("contractId", contractId);
							int entityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
							String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
									"\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

							logger.info("Hitting List Data API for invoices Flow [{}]", flowToTest);
							listDataObj.hitListRendererListData(invoiceListId, payload, params);
							String listDataJsonStr = listDataObj.getListDataJsonStr();
							listDataObj.setListData(listDataJsonStr);

							List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
							if (listData.size() > 0) {
								int columnId = listDataObj.getColumnIdFromColumnName("id");
								Boolean recordFound = false;

								for (Map<Integer, Map<String, String>> oneData : listData) {
									if (oneData.get(columnId).get("valueId").trim().equalsIgnoreCase(String.valueOf(invoiceId))) {
										recordFound = true;
										break;
									}
								}

								if (!recordFound) {
									logger.error("Couldn't find newly created Invoice having Id {} on Listing Page for Flow [{}]", invoiceId, flowToTest);
									csAssert.assertTrue(false, "Couldn't find newly created Invoice having Id " + invoiceId +
											" on Listing Page for Flow [" + flowToTest + "]");
								}
							} else {
								logger.error("No Record found in Invoice Listing for Flow [{}]", flowToTest);
								csAssert.assertTrue(false, "No Record found in Invoice Listing for Flow [" + flowToTest + "]");
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Invoice for Flow [" + flowToTest + "] due to " + createStatus);
					}
				} else {
					if (createStatus.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Invoice Created for Flow [" + flowToTest + "] whereas it was expected not to create.");

						if (deleteEntity) {
							//Add Newly Created Invoice to Delete Ids List
							entityIdsToDelete.add(invoiceId);
						}
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Invoice Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Creation Flow [" + flowToTest + "]. " + e.getStackTrace());
		}finally {

			if (deleteEntity && invoiceId != -1) {
				EntityOperationsHelper.deleteEntityRecord("invoices", invoiceId);

				if (filter_name != null) {
					if (!filter_name.equals("")) {
						ListRendererListData listRendererListData = new ListRendererListData();

						String payload = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "invoice", "payload");
						String columnIdsToIgnore = ParseConfigFile.getValueFromConfigFile(listingPayloadConfigFilePath, listingPayloadConfigFileName, "invoice", "columnidstoignore");
						payload = listRendererListData.createPayloadForColStr(payload, columnIdsToIgnore);

						Map<String, String> listColumnValuesMap = listRendererListData.getListRespToChkPartRecIsPresent(invoiceEntityTypeId, filter_id,
								filter_name, min, max, payload, csAssert);

						String entityId = "";
						try {
							entityId = listColumnValuesMap.get("id").split(":;")[1];

						} catch (Exception e) {

						}

						if (entityId.equalsIgnoreCase(String.valueOf(invoiceId))) {
							csAssert.assertTrue(false, "On Listing page invoices entity " + invoiceId + "  Found After Deletion");
						} else {
							logger.info("On Listing page invoices entity " + invoiceId + " Not Found After Deletion");
						}
					}
				}
			}

		}
		csAssert.assertAll();
	}
}
