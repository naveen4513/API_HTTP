package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.serviceData.TestServiceDataMisc;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemMisc {


	private final static Logger logger = LoggerFactory.getLogger(TestServiceDataMisc.class);
	CustomAssert csAssertion;
	boolean failedInConfigReading = false;
	private String configFilePath;
	private String configFileName;
	private String entityIdMappingFileName;
	private String baseFilePath;
	private String serviceDataEntity = "service data";
	private String consumptionEntity = "consumptions";
	private String invoiceEntity = "invoices";
	private String invoiceLineItemEntity = "invoice line item";
	private int invoiceId = -1;
	private int invoiceLineItemId = -1;
	private int invoiceLineItemEntityTypeId;
	private int invoiceEntityTypeId;

	private int fixedFeeInvoiceLineItemTypeId;
	private int arcRrcInvoiceLineItemTypeId;
	private int forecastInvoiceLineItemTypeId;

	private List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates", "Service Data",
			"Purchase Order", "Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");


	private List<String> createGroupToValidate = Arrays.asList("Basic Information", "Important Dates", "Service Data",
			"Purchase Order", "Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

	public void getConfigData() {
		logger.info("Getting Test Data");


		try {

			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemMiscConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemMiscConfigFileName");


			fixedFeeInvoiceLineItemTypeId = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("FixedFeeInvoiceLineItemTypeId"));
			arcRrcInvoiceLineItemTypeId = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("ARCRRCInvoiceLineItemTypeId"));
			forecastInvoiceLineItemTypeId = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("ForecastInvoiceLineItemTypeId"));


			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			invoiceLineItemEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceLineItemEntity, "entity_type_id"));
			invoiceEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, invoiceEntity, "entity_type_id"));


			invoiceLineItemId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoicelineitemid"));
			invoiceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceid"));


		} catch (Exception e) {
			logger.error("Error while fetching Configuration detail at beforeClass Level " + e.getLocalizedMessage());
			failedInConfigReading = true;
		}


	}


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getConfigData();

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


	boolean isAllGroupsAvailable(String stringResponse,List<String> groupToValidate) {

		boolean result = false;

		try {
			JSONObject response = new JSONObject(stringResponse);
			JSONArray fields = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");


			for (int i = 0; i < fields.length(); i++) {

				JSONObject field = fields.getJSONObject(i);
				if (field.has("label") && field.get("label").toString().contentEquals("General")) {

					JSONArray innerfields = field.getJSONArray("fields");

					List<String> allLevels = new ArrayList<>();


					for (int j = 0; j < innerfields.length(); j++) {
						if (innerfields.getJSONObject(j).has("label")) {

							allLevels.add(innerfields.getJSONObject(j).getString("label"));
						}
					}

					logger.info("Groups to validate  in Show Page Resposne are  : [{}]", groupToValidate);
					logger.info("All Avaliable Groups in Show Page Resposne are  : [{}]", allLevels);
					return allLevels.containsAll(groupToValidate) && groupToValidate.containsAll(allLevels);

				}


			}


		} catch (Exception e) {
			logger.error("Error While Parsing show page response for invoice line item [{}]", invoiceLineItemId);
			return result;
		}


		return false;


	}

	boolean isFieldsAreNonEditable(String stringResponse, String groupName) {

		try {
			JSONObject response = new JSONObject(stringResponse);
			JSONArray fields = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");


			for (int i = 0; i < fields.length(); i++) {
				JSONObject field = fields.getJSONObject(i);

				if (field.has("label") && field.get("label").toString().contentEquals("General")) {
					JSONArray innerfields = field.getJSONArray("fields");

					for (int j = 0; j < innerfields.length(); j++) {
						if (innerfields.getJSONObject(j).has("label") &&
								innerfields.getJSONObject(j).getString("label").toLowerCase().contentEquals(groupName.toLowerCase())) {

							JSONArray innerinnerfields = innerfields.getJSONObject(j).getJSONArray("fields");

							for (int k = 0; k < innerinnerfields.length(); k++) {
								if (innerinnerfields.getJSONObject(k).has("displayMode")) {
									String displayModeValue = innerinnerfields.getJSONObject(k).getString("displayMode");
									if (displayModeValue.contentEquals("editable")) {
										logger.error("[{}] should not be editable", innerinnerfields.getJSONObject(k).getString("name"));
										return false;
									}
								}
							}
						}
					}
				}

			}


		} catch (Exception e) {
			logger.error("Error While Parsing show page response for invoice line item [{}]", invoiceLineItemId);
			return false;
		}


		return true;


	}

	@Test
	public void tc100498() {
		csAssertion = new CustomAssert();
		try {
			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates", "Service Data",
					"Purchase Order", "Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

			if (invoiceLineItemId != -1) {
				Show showObj = new Show();
				showObj.hitShow(invoiceLineItemEntityTypeId, invoiceLineItemId);
				String showPageRespose = showObj.getShowJsonStr();

				logger.debug("Show Page Response is : [{}]", showPageRespose);

				boolean isResponseValidJson = APIUtils.validJsonResponse(showPageRespose, "[Show Page response for Invoice Line Item]");
				csAssertion.assertTrue(isResponseValidJson, "Show Page Response is not valid for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isAllGroupsAvailable(showPageRespose,groupToValidate), "Show Page Response doesn't have required groups in it " + invoiceLineItemId);

				csAssertion.assertTrue(isFieldsAreNonEditable(showPageRespose, "SERVICE DATA"), "Fields in SERVICE DATA Should not editable for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isFieldsAreNonEditable(showPageRespose, "PURCHASE ORDER"), "Fields in PURCHASE ORDER Should not editable for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isFieldsAreNonEditable(showPageRespose, "FUNCTIONS"), "Fields in FUNCTIONS Should not editable for invoice line item id " + invoiceLineItemId);


			} else {
				throw new SkipException("Skipping tests invoice line item is not created from Test Invoice Flow Class");
			}

		} catch (Exception e) {
			logger.error("Exception While Testing tc100498 , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	@Test
	public void tc100499() {
		csAssertion = new CustomAssert();
		try {

			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates","Service Data",
					"Purchase Order","Functions","Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

			if (invoiceLineItemId != -1 && invoiceId != -1) {

				New create = new New();
				create.hitNew(invoiceLineItemEntity, invoiceEntity, invoiceId, null, String.valueOf(fixedFeeInvoiceLineItemTypeId));
				String createPageResponse = create.getNewJsonStr();

				logger.debug("Edit Page Response is : [{}]", createPageResponse);

				boolean isResponseValidJson = APIUtils.validJsonResponse(createPageResponse, "[Create Page response(Get) for Invoice Line Item]");
				csAssertion.assertTrue(isResponseValidJson, "Create Page Response(Get) is not valid for invoice line item id " + invoiceLineItemId + " For Fixed Fee Type");
				csAssertion.assertTrue(isAllGroupsAvailable(createPageResponse,groupToValidate), "Create Page Response(Get) doesn't have required groups in it " + invoiceLineItemId + " For Fixed Fee Type");
				csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "SERVICE DATA"), "Fields in SERVICE DATA Should not editable for invoice line item id " + invoiceLineItemId);
				//csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "PURCHASE ORDER"), "Fields in PURCHASE ORDER Should not editable for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "FUNCTIONS"), "Fields in FUNCTIONS Should not editable for invoice line item id " + invoiceLineItemId);


			} else {
				throw new SkipException("Skipping tests invoice line item or invoice is not created from Test Invoice Flow Class");
			}

		} catch (Exception e) {
			logger.error("Exception While Testing tc100499 , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	@Test(dependsOnMethods = "tc100499")
	public void tc100499a() {
		csAssertion = new CustomAssert();
		try {

			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates","Service Data",
					"Purchase Order", "Functions","Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

			New create = new New();
			create.hitNew(invoiceLineItemEntity, invoiceEntity, invoiceId, null, String.valueOf(arcRrcInvoiceLineItemTypeId));
			String createPageResponse = create.getNewJsonStr();

			logger.debug("Edit Page Response is : [{}]", createPageResponse);

			boolean isResponseValidJson = APIUtils.validJsonResponse(createPageResponse, "[Create Page response(Get) for Invoice Line Item]");
			csAssertion.assertTrue(isResponseValidJson, "Create Page Response(Get) is not valid for invoice line item id " + invoiceLineItemId + " For ARC/RRC Type");
			csAssertion.assertTrue(isAllGroupsAvailable(createPageResponse,groupToValidate), "Create Page Response(Get) doesn't have required groups in it " + invoiceLineItemId + " For ARC/RRC Type");
			csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "SERVICE DATA"), "Fields in SERVICE DATA Should not editable for invoice line item id " + invoiceLineItemId);
			//csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "PURCHASE ORDER"), "Fields in PURCHASE ORDER Should not editable for invoice line item id " + invoiceLineItemId);
			csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "FUNCTIONS"), "Fields in FUNCTIONS Should not editable for invoice line item id " + invoiceLineItemId);


		} catch (Exception e) {
			logger.error("Exception While Testing tc100499a , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	@Test(dependsOnMethods = "tc100499a")
	public void tc100499b() {
		csAssertion = new CustomAssert();
		try {

			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates","Service Data",
					"Purchase Order","Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

			New create = new New();
			create.hitNew(invoiceLineItemEntity, invoiceEntity, invoiceId, null, String.valueOf(forecastInvoiceLineItemTypeId));
			String createPageResponse = create.getNewJsonStr();

			logger.debug("Edit Page Response is : [{}]", createPageResponse);

			boolean isResponseValidJson = APIUtils.validJsonResponse(createPageResponse, "[Create Page response(Get) for Invoice Line Item]");
			csAssertion.assertTrue(isResponseValidJson, "Create Page Response(Get) is not valid for invoice line item id " + invoiceLineItemId + " For forcast Type");
			csAssertion.assertTrue(isAllGroupsAvailable(createPageResponse,groupToValidate), "Create Page Response(Get) doesn't have required groups in it " + invoiceLineItemId + " For forecast Type");
			//csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "SERVICE DATA"), "Fields in SERVICE DATA Should not editable for invoice line item id " + invoiceLineItemId);
			//csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "PURCHASE ORDER"), "Fields in PURCHASE ORDER Should not editable for invoice line item id " + invoiceLineItemId);
			csAssertion.assertTrue(isFieldsAreNonEditable(createPageResponse, "FUNCTIONS"), "Fields in FUNCTIONS Should not editable for invoice line item id " + invoiceLineItemId);


		} catch (Exception e) {
			logger.error("Exception While Testing tc100499b , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	@Test
	public void tc100500() {
		csAssertion = new CustomAssert();
		try {

			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates", "Service Data",
					"Purchase Order", "Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");

			if (invoiceLineItemId != -1) {

				Edit edit = new Edit();
				String editPageReponse = edit.hitEdit(invoiceLineItemEntity, invoiceLineItemId);

				logger.debug("Edit Page Response is : [{}]", editPageReponse);

				boolean isResponseValidJson = APIUtils.validJsonResponse(editPageReponse, "[Edit Page response for Invoice Line Item]");
				csAssertion.assertTrue(isResponseValidJson, "Edit Page Response is not valid for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isAllGroupsAvailable(editPageReponse,groupToValidate), "Edit Page Response doesn't have required groups in it " + invoiceLineItemId);
				csAssertion.assertTrue(isFieldsAreNonEditable(editPageReponse, "SERVICE DATA"), "Fields in SERVICE DATA Should not editable for invoice line item id " + invoiceLineItemId);
				//csAssertion.assertTrue(isFieldsAreNonEditable(editPageReponse, "PURCHASE ORDER"), "Fields in PURCHASE ORDER Should not editable for invoice line item id " + invoiceLineItemId);
				csAssertion.assertTrue(isFieldsAreNonEditable(editPageReponse, "FUNCTIONS"), "Fields in FUNCTIONS Should not editable for invoice line item id " + invoiceLineItemId);

			} else {
				throw new SkipException("Skipping tests invoice line item or invoice is not created from Test Invoice Flow Class");
			}

		} catch (Exception e) {
			logger.error("Exception While Testing tc100500 , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	// it will create payload for Invoice line item update
	private JSONObject getPayloadForUpdateLineItem(JSONObject editGetPageJsonResponse) {

		try {
			JSONObject editPostRequestpayload = editGetPageJsonResponse;
			editPostRequestpayload.remove("header");
			editPostRequestpayload.remove("session");
			editPostRequestpayload.remove("actions");
			editPostRequestpayload.remove("createLinks");

			editPostRequestpayload.getJSONObject("body").remove("layoutInfo");
			editPostRequestpayload.getJSONObject("body").remove("globalData");
			editPostRequestpayload.getJSONObject("body").remove("errors");
			// only comment updation
			editPostRequestpayload.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", "for update Testing");
			logger.debug("Payload is : [{}]", editPostRequestpayload);
			return editPostRequestpayload;
		} catch (Exception e) {
			logger.error("Error While Build payload for  Update Line Item ");
			return null;
		}

	}


	@Test(dependsOnMethods = "tc100500")
	public void tc100501() {
		csAssertion = new CustomAssert();
		try {
			List<String> groupToValidate = Arrays.asList("Basic Information", "Important Dates", "Service Data",
					"Purchase Order", "Functions", "Stakeholders", "Validation", "COMMENTS AND ATTACHMENTS");
			if (invoiceLineItemId != -1) {
				Edit edit = new Edit();
				String editPageReponse = edit.hitEdit(invoiceLineItemEntity, invoiceLineItemId);
				logger.debug("Edit Page Response is : [{}]", editPageReponse);


				JSONObject editGetPageJson = new JSONObject(editPageReponse);
				JSONObject editPostRequestpayload = getPayloadForUpdateLineItem(editGetPageJson);

				String editPostAPIReponse = edit.hitEdit(invoiceLineItemEntity, editPostRequestpayload.toString());
				logger.debug("Edit Page(Post) Response is : [{}]", editPostAPIReponse);
				csAssertion.assertTrue(edit.getStatusCodeFrom(edit.getEditAPIResponse()).contains("200"), "Edit Page (Post)-> Update API Status Code is Incorrect");
				boolean isResponseValidJson = APIUtils.validJsonResponse(editPostAPIReponse, "[Edit Page (Post)-> Update API response for Invoice Line Item]");
				csAssertion.assertTrue(isResponseValidJson, "Edit Page (Post)-> Update is not valid for invoice line item id " + invoiceLineItemId);


			} else {
				throw new SkipException("Skipping tests invoice line item or invoice is not created from Test Invoice Flow Class");
			}

		} catch (Exception e) {
			logger.error("Exception While Testing tc100501 , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();


	}

	boolean isInvoiceLineItemAvailableInInvoiceDetailsTabListing(String response, int invoiceLineItemId) {
		try {
			JSONObject jsonResponse = new JSONObject(response);
			JSONArray dataArray = jsonResponse.getJSONArray("data");

			for (int i = 0; i < dataArray.length(); i++) {
				JSONObject data = dataArray.getJSONObject(i);
				Set<String> keys = data.keySet();
				for (String key : keys) {
					if (data.getJSONObject(key).has("columnName")
							&& data.getJSONObject(key).get("columnName").toString().contentEquals("id")
							&& data.getJSONObject(key).has("value")) {

						String value = data.getJSONObject(key).getString("value");
						if (value.contains(String.valueOf(invoiceLineItemId))) {
							return true;
						}
					}

				}
			}
		} catch (Exception e) {
			logger.error("Error While Parsing Details tab response for invoice : [{}]", e.getMessage());
		}
		return false;

	}

	@Test
	public void tc100534() {


		int invoiceDetailsTabId = 357;
		csAssertion = new CustomAssert();
		try {

			if (invoiceLineItemId != -1 && invoiceId != -1) {
				GetTabListData getTabListData = new GetTabListData(invoiceEntityTypeId, invoiceDetailsTabId, invoiceId);
				getTabListData.hitGetTabListData();
				String detailsTabListDataResponse = getTabListData.getTabListDataResponse();
				boolean isTabListDataValidJson = APIUtils.validJsonResponse(detailsTabListDataResponse, "[Details tab list data response for Invoice]");

				csAssertion.assertTrue(isTabListDataValidJson, "Details Tab List Data Response is not valid for invoice id " + invoiceId);
				csAssertion.assertTrue(isInvoiceLineItemAvailableInInvoiceDetailsTabListing(detailsTabListDataResponse, invoiceLineItemId), " Error : invoice line item detail under invoice show page details not found ");
			} else {
				throw new SkipException("Skipping tests invoice line item is not created from Test Invoice Flow Class");
			}

		} catch (Exception e) {
			logger.error("Exception While Testing tc100534 , [{}]", e.getLocalizedMessage());
		}

		csAssertion.assertAll();

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
