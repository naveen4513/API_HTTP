package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

public class TestPurchaseOrderMisc {


	private final static Logger logger = LoggerFactory.getLogger(TestPurchaseOrderMisc.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static Integer poEntityTypeId = -1;
	private static Integer poListId = -1;
	private static String purchaseOrderEntityName = "purchase orders";
	private static String contractsEntityName = "contracts";
	private static int contractIdForCreatingPurchaseOrder = -1;

	public boolean failedInConfigReading = false;


	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		try {
			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderMiscTestConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderMiscTestConfigFileName");

			poListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "purchase orders", "entity_url_id"));
			poEntityTypeId = ConfigureConstantFields.getEntityIdByName("purchase orders");

			contractIdForCreatingPurchaseOrder = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractid"));

		} catch (Exception e) {
			logger.error("Error in before Class method while reading config file : [{}]", e.getLocalizedMessage());
			failedInConfigReading = true;
		}

	}

	@BeforeMethod
	public void beforeMethod(Method method) {

		if (failedInConfigReading) {
			throw new SkipException("Skipping tests because Some Error while fetching Configuration detail at beforeClass Level");
		} else {

			logger.info("In Before Method");
			logger.info("method name is: {} ", method.getName());
			logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");
		}


	}


	// it will give the purchase Order Entity Id on which Edit API would be called
	public int getPurchaseOrderEntityIdToTest() {

		int entityIdToTest = -1;
		try {

			logger.info("Hitting Purchase Order Listing API.");
			ListRendererListData listDataObj = new ListRendererListData();
			listDataObj.hitListRendererListData(poListId);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				List<Integer> allDBIds;
				JSONObject listDataResponseObj = new JSONObject(listDataResponse);

				int noOfRecords = listDataResponseObj.getJSONArray("data").length();

				if (noOfRecords > 0) {
					listDataObj.setListData(listDataResponse);
//					int columnId = listDataObj.getColumnIdFromColumnName("id");
					int columnId = listDataObj.getColumnIdFromColumnName("name");
					allDBIds = listDataObj.getAllRecordDbId(columnId, listDataResponse);
					int index = (int) (Math.random() * allDBIds.size());
					return allDBIds.get(index);
				}

			} else {
				logger.error("Purchase Order List Data API Response is an Invalid JSON.");
			}

		} catch (Exception e) {
			logger.error("Error While Getting Entity Id to Test From Listing Page of Purchase Order");
			return entityIdToTest;

		}

		return entityIdToTest;
	}

	public boolean verifyIfCountriesOrStatesFieldisMultiSelectOrNot(JSONObject response, String fieldName) {

		boolean result = false;
		try {
			JSONArray tabsDetails = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");

			for (int i = 0; i < tabsDetails.length(); i++) {

				JSONObject tabDetail = tabsDetails.getJSONObject(i);
				if (tabDetail.has("label") && tabDetail.getString("label").toLowerCase().contentEquals("GENERAL".toLowerCase())) {
					JSONArray fields = tabDetail.getJSONArray("fields");

					for (int j = 0; j < fields.length(); j++) {

						JSONObject field = fields.getJSONObject(j);
						if (field.has("label") && Pattern.compile("GEOGRAPHY", Pattern.CASE_INSENSITIVE).matcher(field.getString("label").toLowerCase()).find()) {

							JSONArray innerFields = field.getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
							for (int k = 0; k < innerFields.length(); k++) {

								JSONObject innerField = innerFields.getJSONObject(k);
								if (innerField.has("label") && innerField.getString("label").toLowerCase().contentEquals(fieldName.toLowerCase())) {
									if (innerField.has("type") && innerField.getString("type").toLowerCase().contentEquals("select") &&
											innerField.has("properties") && innerField.getJSONObject("properties").has("multiple") &&
											innerField.getJSONObject("properties").getBoolean("multiple") == true) {
										return true;
									}

								}
							}
						} else
							continue;
					}
				} else
					continue;

			}
		} catch (Exception e) {
			logger.error("Error While Parsing Response for verifying If Countries Field is MultiSelect Or Not");
		}
		return result;

	}

	//This test covers TC-97285,TC-97287
	@Test
	public void testValidateStatusAndCountriesFieldInEditPage() {
		CustomAssert csAssert = new CustomAssert();

		try {

			int entityIdToTest = getPurchaseOrderEntityIdToTest();
			logger.info("entityIdToTest : [{}]", entityIdToTest);
			if (entityIdToTest != -1) {
				Edit edit = new Edit();
				String editPageResponse = edit.hitEdit(purchaseOrderEntityName, entityIdToTest);

				if (ParseJsonResponse.validJsonResponse(editPageResponse)) {

					JSONObject editPageJsonResponse = new JSONObject(editPageResponse);
					csAssert.assertTrue(verifyIfCountriesOrStatesFieldisMultiSelectOrNot(editPageJsonResponse, "Countries"), "Countries Field is not MultiSelect in Edit Page of Purchase Order ");
					csAssert.assertTrue(verifyIfCountriesOrStatesFieldisMultiSelectOrNot(editPageJsonResponse, "States"), "States Field is not MultiSelect in Edit Page of Purchase Order ");

				} else {
					csAssert.assertTrue(false, "Purchase Order edit Page(Get) API Response is an Invalid JSON.");
				}

			} else {
				csAssert.assertTrue(false, "Not Being able to Fetch any Purchase Order Entity Id for Testing");
			}

		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order Show Page Tabs. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while validating Purchase Order Show Page Tabs. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//This test covers TC-97284,TC-97286
	@Test
	public void testValidateStatusAndCountriesFieldInCreatePage() {
		CustomAssert csAssert = new CustomAssert();

		try {
			New newObj = new New();
			newObj.hitNew(purchaseOrderEntityName, contractsEntityName, contractIdForCreatingPurchaseOrder);
			String newPageResponse = newObj.getNewJsonStr();

			if (ParseJsonResponse.validJsonResponse(newPageResponse)) {
				JSONObject newPageJSONResponse = new JSONObject(newPageResponse);
				csAssert.assertTrue(verifyIfCountriesOrStatesFieldisMultiSelectOrNot(newPageJSONResponse, "Countries"), "Countries Field is not MultiSelect in Create Page of Purchase Order ");
				csAssert.assertTrue(verifyIfCountriesOrStatesFieldisMultiSelectOrNot(newPageJSONResponse, "States"), "States Field is not MultiSelect in Create Page of Purchase Order ");

			} else {
				csAssert.assertTrue(false, "Purchase Order Create Page(new get) API Response is an Invalid JSON.");
			}


		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Purchase Order Show Page Tabs. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while validating Purchase Order Show Page Tabs. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");


	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}
}
