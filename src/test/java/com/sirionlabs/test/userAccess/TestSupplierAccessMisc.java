package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientAdmin.AdminSupplierAccessHelper;
import com.sirionlabs.helper.clientAdmin.FieldProvisioningHelper;
import com.sirionlabs.helper.search.SearchAttachmentHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestSupplierAccessMisc extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestSupplierAccessMisc.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String endUserName = null;
	private static String endUserPassword = null;
	private static String supplierTypeUserName = "naveen.supplier";
	private static String supplierTypeUserPassword = "admin123";
	private static String clientTypeUserName = "naveen_user";
	private static String clientTypeUserPassword = "admin123";

	private AdminSupplierAccessHelper supplierAccessHelperObj = new AdminSupplierAccessHelper();
	private AdminHelper adminHelperObj = new AdminHelper();

	@BeforeClass
	public void beforeClass() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SupplierAccessMiscConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierAccessMiscConfigFileName");

		endUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client end user details", "username");
		endUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "client end user details", "password");

		/*supplierTypeUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type user details", "username");
		supplierTypeUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplier type user details", "password");*/
	}

	private Boolean loginWithEndUser(String userName, String userPassword) {
		logger.info("Logging with UserName [{}] and Password [{}]", userName, userPassword);
		Check checkObj = new Check();
		checkObj.hitCheck(userName, userPassword);

		return (Check.getAuthorization() != null);
	}

	@AfterClass
	public void afterClass() {
		//Login with Original User for Rest of the Automation Suite
		logger.info("Logging back with Original Environment Configuration.");

		String originalUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
		String originalPassword = ConfigureEnvironment.getEnvironmentProperty("password");

		if (!loginWithEndUser(originalUserName, originalPassword)) {
			logger.info("Couldn't Login back with Original UserName [{}] and Password [{}]. Hence aborting Automation Suite.", originalUserName, originalPassword);
			System.exit(0);
		}
	}

	//To Verify Contract Entity should be present under Supplier access in client admin.
/*	@Test(priority=1)
	public void testC151358() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Verifying that Contract Entity is present under Supplier Access in Client Admin.");
			logger.info("Hitting Admin Supplier Access Show API");

			AdminSupplierAccessShow showObj = new AdminSupplierAccessShow();
			String showResponse = showObj.hitSupplierAccessShow();

			Boolean isContractPresent = supplierAccessHelperObj.isEntityPresentInSupplierAccessConfigurationShowAPIResponse(showResponse, "CONTRACTS");

			if (isContractPresent == null) {
				logger.error("Couldn't check if Contract is present under Supplier Access in Client Admin or not. Hence skipping test.");
				throw new SkipException("Couldn't check if Contract is present under Supplier Access in Client Admin or not. Hence skipping test.");
			}

			if (!isContractPresent) {
				csAssert.assertTrue(false, "Couldn't find Contract Entity under Supplier Access in Client Admin.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//To Verify Contract Entity should be present under Supplier Access at Vendor Hierarchy Level in Client Admin.
	//To Verify Contract Entity Drop Down has values: Full Access, No Access, Entity Level Access.
	@Test(priority=2)
	public void testC151359() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Verifying that Contract Entity is present under Supplier Access at Vendor Hierarchy Level in Client Admin. TC-C151359");
			logger.info("Hitting Admin Supplier Access Vendor Level Create API");

			String createResponse = executor.get(AdminSupplierAccessVendorLevelCreate.getApiPath(), AdminSupplierAccessVendorLevelCreate.getHeaders())
					.getResponse().getResponseBody();

			List<Map<String, String>> allVendors = supplierAccessHelperObj.getAllVendorsPresentUnderSupplierAccessAtVendorLevel(createResponse);

			if (allVendors == null || allVendors.isEmpty()) {
				logger.error("Couldn't get all Vendor Options from Supplier Access Vendor Level Create API. Hence skipping test.");
				throw new SkipException("Couldn't get all Vendor Options from Supplier Access Vendor Level Create API. Hence skipping test.");
			}

			logger.info("Total Vendor Options Available are: {}", allVendors.size());
			logger.info("Selecting One Vendor Randomly.");

			int randomNumberForVendor = RandomNumbers.getRandomNumberWithinRangeIndex(0, allVendors.size() - 1);

			String vendorId = allVendors.get(randomNumberForVendor).get("id");
			String vendorName = allVendors.get(randomNumberForVendor).get("name");

			logger.info("Hitting Admin Supplier Access Vendor Level Vendor Supplier Access API for Vendor Id {} and Vendor Name {}", vendorId, vendorName);
			String vendorResponse = executor.get(AdminVendorSupplierAccess.getApiPath(vendorId, vendorName), AdminVendorSupplierAccess.getHeaders())
					.getResponse().getResponseBody();

			Boolean isContractPresent = supplierAccessHelperObj.isEntityPresentInVendorSupplierAccessAPIResponse(vendorResponse, "CONTRACTS");

			if (isContractPresent == null) {
				logger.error("Couldn't check if Contract is present under Supplier Access in Client Admin or not. Hence skipping test.");
				throw new SkipException("Couldn't check if Contract is present under Supplier Access in Client Admin or not. Hence skipping test.");
			}

			if (isContractPresent) {
				logger.info("Getting All Supplier Access Options at Vendor Level for Contract.");
				List<String> allSupplierAccessOptions = supplierAccessHelperObj.getAllSupplierAccessOptionsAtVendorHierarchyLevelForEntity(vendorResponse,
						"CONTRACTS");

				if (allSupplierAccessOptions == null || allSupplierAccessOptions.isEmpty()) {
					logger.error("Couldn't get All Options of Supplier Access at Vendor Level for Contract. Hence skipping test.");
					throw new SkipException("Couldn't get All Options of Supplier Access at Vendor Level for Contract. Hence skipping test.");
				}

				String[] allExpectedOptions = {"Full Access", "No access", "Enable Access At Entity Level"};
				logger.info("Matching All Expected Options of Supplier Access at Vendor Level for Contract.");

				for (String expectedOption : allExpectedOptions) {
					if (!allSupplierAccessOptions.contains(expectedOption.trim())) {
						csAssert.assertTrue(false, "Option [" + expectedOption + "] is not present under Supplier Access at Vendor Level for Contract.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Couldn't find Contract Entity under Supplier Access in Client Admin.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//To Verify Contract Entity should be available in User Level Field Access
	@Test(priority=3)
	public void testC8314() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Verifying that Contract Entity is present in User Level Field Access in Client Admin. TC-C8314");
			logger.info("Hitting Admin Field Provisioning API");

			Provisioning provisioningObj = new Provisioning();
			String fieldProvisioningResponse = provisioningObj.hitFieldProvisioning();

			UserAccessHelper accessObj = new UserAccessHelper();
			List<String> allEntitiesPresent = accessObj.getAllEntitiesFromFieldProvisioningAPI(fieldProvisioningResponse);

			if (allEntitiesPresent == null || allEntitiesPresent.isEmpty()) {
				logger.error("Couldn't get All Entities present in User Level Field Access in Client Admin. Hence skipping test");
				throw new SkipException("Couldn't get All Entities present in User Level Field Access in Client Admin. Hence skipping test");
			}

			if (!allEntitiesPresent.contains("CONTRACTS")) {
				csAssert.assertTrue(false, "Contract Entity not found in Field Provisioning API Response. Contract not present in User Level Field Access");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Entity Contract is present under Supplier Access in Client Admin. " + e.getMessage());
		}
		csAssert.assertAll();
	}*/

	//To Verify Communication Shared with Supplier Tab in Contract Show Page for Supplier Type User.
	//To Verify Supplier Type User doesn't have access to BreadCrumb in Contract Show Page. TC-C8713
	//To Verify Supplier Type User doesn't have Audit Log Tab. TC-C8712
	/*@Test(priority=4)
	public void verifyTabsForSupplierUserInContractShow() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!loginWithEndUser(supplierTypeUserName, supplierTypeUserPassword)) {
				throw new SkipException("Couldn't login with Supplier Type UserName [" + supplierTypeUserName + "] and Password [" + supplierTypeUserPassword +
						"]. Hence skipping test.");
			}

			logger.info("Hitting ListData API for Contract");
			ListRendererListData listDataObj = new ListRendererListData();
			int listId = ConfigureConstantFields.getListIdForEntity("contracts");
			String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			listDataObj.hitListRendererListData(listId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				String value = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(columnId)).getString("value");
				int recordId = ListDataHelper.getRecordIdFromValue(value);

				logger.info("Hitting Show API for Record Id {} of Contract.", recordId);
				Show showObj = new Show();
				showObj.hitShow(61, recordId);
				String showResponse = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showResponse)) {
					//Verify Tab Communication-Shared With Supplier
					logger.info("Getting All Tab Labels from Show API Response.");
					List<String> allTabLabels = ParseJsonResponse.getAllTabLabels(showResponse);
					*//*logger.info("Verifying that Show Response contains [Communication-Shared With Supplier] Tab");

					if (!allTabLabels.contains("Communication-Shared With Supplier")) {
						csAssert.assertTrue(false, "Tab [Communication-Shared With Supplier] not found in Show API Response for Contract Record Id " +
								recordId);
					}*//*

					//Verify Tab Audit Log is not present
					if (allTabLabels.contains("Audit Log")) {
						csAssert.assertTrue(false, "Tab AUDIT LOG is present in Show API Response for Contract Record Id " + recordId);
					}

					//Verify BreadCrumb is not Available
					logger.info("Verifying that Show Response doesn't have BreadCrumb i.e. history json object.");
					jsonObj = new JSONObject(showResponse);
					jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

					if (jsonObj.has("history")) {
						csAssert.assertTrue(false, "Show API Response contains history object i.e. It has Access to BreadCrumb for Contract Record Id " +
								recordId);
					}
				} else {
					csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Contract is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Contract is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying Communication Shared with Supplier Tab in Contract Show Page. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying Communication  Shared with Supplier Tab in Contract Show Page. " + e.getMessage());
		}
		csAssert.assertAll();
	}
*/
	/*To Verify that on Create Page of Contract Entity, Supplier Access Checkbox should be:
			     * checked (non editable) when Full Access is given (TC-C8342)
	             * unchecked (non editable) when No Access is given (TC-C8345)
	             * unchecked (editable) when Enable Access at Entity Level is given (TC-C8349)
	*/
	/*@Test(priority=5)
	public void testC8342() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Hitting Admin Supplier Access Show API");

			AdminSupplierAccessShow showObj = new AdminSupplierAccessShow();
			String showResponse = showObj.hitSupplierAccessShow();

			String accessValue = supplierAccessHelperObj.getSupplierAccessConfigurationForEntityFromShowAPI(showResponse, "Contract");

			if (accessValue == null) {
				logger.error("Couldn't get Access Value for Contract at Supplier Level Access Configuration. Hence skipping test.");
				throw new SkipException("Couldn't get Access Value for Contract at Supplier Level Access Configuration. Hence skipping test.");
			}

			if (!loginWithEndUser(clientTypeUserName, clientTypeUserPassword)) {
				logger.error("Couldn't login with Supplier Type User [{}/{}]. Hence skipping test", supplierTypeUserName, supplierTypeUserPassword);
				throw new SkipException("Couldn't login with Supplier Type User [" + supplierTypeUserName + "/" + supplierTypeUserPassword + "]. Hence skipping test.");
			}

			logger.info("Hitting ListData API for supplier");
			ListRendererListData listDataObj = new ListRendererListData();
			int listId = ConfigureConstantFields.getListIdForEntity("suppliers");
			String payload = "{\"filterMap\":{\"entityTypeId\":1,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			listDataObj.hitListRendererListData(listId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				String value = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(columnId)).getString("value");
				int recordId = ListDataHelper.getRecordIdFromValue(value);

				logger.info("Hitting Show API for Record Id {} of Contract.", recordId);
				Show showPageObj = new Show();
				showPageObj.hitShow(1, recordId);
				showResponse = showPageObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showResponse)) {

					logger.info("Hitting New API for Contract");
					New newObj = new New();
					newObj.hitNew("contracts", "suppliers", recordId,"msa");
					String newResponse = newObj.getNewJsonStr();

					if (ParseJsonResponse.validJsonResponse(newResponse)) {
						jsonObj = new JSONObject(newResponse);

						logger.info("Getting Actual Value of Supplier Access from New API Response");
						Boolean actualValue = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("supplierAccess").getBoolean("values");

						logger.info("Getting Display Mode of Supplier Access from New API Response");
						Map<String, String> supplierAccessField = ParseJsonResponse.getFieldByName(newResponse, "supplierAccess");

						if (supplierAccessField == null || supplierAccessField.isEmpty() || supplierAccessField.get("displayMode").trim().equalsIgnoreCase("")) {
							logger.error("Couldn't get Actual Display Mode for Supplier Access field from New API Response. Hence skipping test.");
							throw new SkipException("Couldn't get Actual Display Mode for Supplier Access field from New API Response. Hence skipping test.");
						}

						String actualDisplayMode = supplierAccessField.get("displayMode").trim();
						String expectedSectionName = null;

						switch (accessValue.trim().toLowerCase()) {
							case "full access":
								expectedSectionName = "tc-c8342";
								break;

							case "no access":
								expectedSectionName = "tc-c8345";
								break;

							case "enable access at entity level":
								expectedSectionName = "tc-c8349";
								break;
						}

						String expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, expectedSectionName, "checked");
						String expectedDisplayMode = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, expectedSectionName, "displayMode");

						logger.info("Supplier Access Value configured is: {}. Expected Supplier Access Checked Value: {} and Expected Supplier Access Display Mode: {}",
								accessValue.trim(), expectedValue, expectedDisplayMode);

						if (!expectedValue.trim().equalsIgnoreCase(actualValue.toString().trim())) {
							csAssert.assertTrue(false, "Expected Supplier Access Checked Value: " + expectedValue +
									" and Actual Supplier Access Checked Value: " + actualValue);
						}

						if (!expectedDisplayMode.trim().equalsIgnoreCase(actualDisplayMode.trim())) {
							csAssert.assertTrue(false, "Expected Supplier Access Display Mode: " + expectedDisplayMode +
									" and Actual Supplier Access Display Mode: " + actualDisplayMode);
						}
					} else {
						csAssert.assertTrue(false, "New API Response for Contract is an Invalid JSON.");
					}
				}else {
					csAssert.assertTrue(false, "Supplier Show API Response  is an Invalid JSON.");
				}

			}else {
				csAssert.assertTrue(false, "ListData API Response for Supplier is an Invalid JSON.");
			}




		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying Supplier Access field on Create Page of Contract. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying Supplier Access field on Create Page of Contract. " + e.getMessage());
		}
		csAssert.assertAll();
	}
*/
	//To Verify that Supplier Access checkbox is not visible on Show Page of Contract Entity
	@Test(priority=6)
	public void testC8423() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: Validating that Supplier Access Checkbox is not visible on Show Page of Contract Entity.");
			if (!loginWithEndUser(supplierTypeUserName, supplierTypeUserPassword)) {
				throw new SkipException("Couldn't login with Supplier Type UserName [" + supplierTypeUserName + "] and Password [" + supplierTypeUserPassword +
						"]. Hence skipping test.");
			}

			logger.info("Hitting ListData API for Contract");
			ListRendererListData listDataObj = new ListRendererListData();
			int listId = ConfigureConstantFields.getListIdForEntity("contracts");
			String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			listDataObj.hitListRendererListData(listId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (ListDataHelper.getFilteredListDataCount(listDataResponse) == 0) {
					logger.info("Couldn't get any records in List Data API Response for Contract. Hence skipping test.");
					throw new SkipException("Couldn't get any records in List Data API Response for Contract. Hence skipping test.");
				}

				int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				String value = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(columnId)).getString("value");
				int recordId = ListDataHelper.getRecordIdFromValue(value);

				logger.info("Hitting Show API for Record Id {} of Contract.", recordId);
				Show showObj = new Show();
				showObj.hitShow(61, recordId);
				String showResponse = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showResponse)) {
					Map<String, String> supplierAccessMap = ParseJsonResponse.getFieldByName(showResponse, "supplierAccess");

					if (supplierAccessMap == null || supplierAccessMap.isEmpty()) {
						logger.error("Couldn't get attributes of Field SupplierAccess from Show API Response having Record Id {}. Hence skipping test.", recordId);
						throw new SkipException("Couldn't get attributes of Field SupplierAccess from Show API Response having Record Id " + recordId +
								". Hence skipping test");
					}

					String displayMode = supplierAccessMap.get("displayMode");
					if (displayMode == null || !displayMode.trim().equalsIgnoreCase("display")) {
						csAssert.assertTrue(false, "Expected Supplier Access Display Mode: display and Actual Display Mode: " + displayMode);
					}
				} else {
					csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Contract is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Contract is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Supplier Access Checkbox is not visible in Contract Show Page. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Supplier Access Checkbox is not visible in Contract Show Page. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//To Verify that the supplier user sees only comments and attachments shared with him.
	@Test(priority=7)
	public void testC8632() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: Validating that the Supplier User Sees only those Comments and Attachments shared with him.");
			if (!loginWithEndUser(endUserName, endUserPassword)) {
				throw new SkipException("Couldn't login with Client Type End UserName [" + endUserName + "] and Password [" + endUserPassword +
						"]. Hence skipping test.");
			}

			logger.info("Hitting ListData API for Contract");
			ListRendererListData listDataObj = new ListRendererListData();
			int listId = ConfigureConstantFields.getListIdForEntity("contract");
			String payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			listDataObj.hitListRendererListData(listId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (ListDataHelper.getFilteredListDataCount(listDataResponse) == 0) {
					logger.info("Couldn't get any records in List Data API Response for Contract. Hence skipping test.");
					throw new SkipException("Couldn't get any records in List Data API Response for Contract. Hence skipping test.");
				}

				int columnId = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				boolean recordFoundWithCommunication = false;
				int recordToMatchId = -1;
				List<Long> communicationIdsSharedWithSupplier = new ArrayList<>();
				TabListData tabObj = new TabListData();
				String tabListPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
						"\"orderDirection\":\"asc\",\"filterJson\":{}}}";
				String tabListResponse;
				JSONObject tabListJsonObj;
				JSONArray tabListJsonArr;

				//Find any record having at-least one communication entry.
				for (int i = 0; i < jsonArr.length(); i++) {
					String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnId)).getString("value");
					int recordId = ListDataHelper.getRecordIdFromValue(value);

					logger.info("Hitting TabListData API for Record Id {} of Contract.", recordId);
					tabListResponse = tabObj.hitTabListData(65, 61, recordId, tabListPayload);

					if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
						if (TabListDataHelper.getFilteredCount(tabListResponse) > 0) {
							recordFoundWithCommunication = true;
							recordToMatchId = recordId;
							int shareWithSupplierColumnId = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "share_with_supplier");
							int privateCommunicationColumnId = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "private_communication");

							tabListJsonObj = new JSONObject(tabListResponse);
							tabListJsonArr = tabListJsonObj.getJSONArray("data");

							for (int j = 0; j < tabListJsonArr.length(); j++) {
								String sharedWithSupplierValue = tabListJsonArr.getJSONObject(j).getJSONObject(String.valueOf(shareWithSupplierColumnId))
										.getString("value");

								if (sharedWithSupplierValue != null && sharedWithSupplierValue.trim().equalsIgnoreCase("true")) {
									String communicationValue = tabListJsonArr.getJSONObject(j).getJSONObject(String.valueOf(privateCommunicationColumnId))
											.getString("value");

									Long communicationId = TabListDataHelper.getCommunicationIdFromValue(communicationValue);

									if (communicationId != null) {
										communicationIdsSharedWithSupplier.add(communicationId);
									} else {
										logger.error("Couldn't get Communication Id from Value: {}.", value);
										csAssert.assertTrue(false, "Couldn't get Communication Id from Value: " + value);
										break;
									}
								}
							}
							break;
						}
					} else {
						csAssert.assertTrue(false, "TabListData API Response for Tab Id 65 of Record Id " + recordId + " of Contract is an Invalid JSON.");
					}
				}

				if (!recordFoundWithCommunication) {
					logger.info("Couldn't find Record with any Communication Present in it. Hence skipping test.");
					throw new SkipException("Couldn't find Record with any Communication Present in it. Hence skipping test.");
				}

				//Validate Communication Entries.
				if (!loginWithEndUser(supplierTypeUserName, supplierTypeUserPassword)) {
					throw new SkipException("Couldn't login with Supplier Type UserName [" + supplierTypeUserName + "] and Password [" + supplierTypeUserPassword +
							"]. Hence skipping test.");
				}

				tabListResponse = tabObj.hitTabListData(427, 61, recordToMatchId);

				if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
					tabListJsonObj = new JSONObject(tabListResponse);
					tabListJsonArr = tabListJsonObj.getJSONArray("data");

					int privateCommunicationColumnId = ListDataHelper.getColumnIdFromColumnName(tabListResponse, "private_communication");

					for (int k = 0; k < tabListJsonArr.length(); k++) {
						String communicationValue = tabListJsonArr.getJSONObject(k).getJSONObject(String.valueOf(privateCommunicationColumnId))
								.getString("value");
						Long communicationId = TabListDataHelper.getCommunicationIdFromValue(communicationValue);

						if (!communicationIdsSharedWithSupplier.contains(communicationId)) {
							csAssert.assertTrue(false, "Communication having Id " + communicationId +
									" is Not Shared with Supplier but still visible to Supplier Type User. Record Id " + recordToMatchId);
						}
					}
				} else {
					csAssert.assertTrue(false, "TabListData API Response for Tab Id 427 of Record Id " + recordToMatchId + " of Contract is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Contract is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Supplier User sees only those Communication Shared with him.. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Supplier User sees only those Communication Shared with him. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//To Verify that Supplier User should be able to Search only those documents that are shared with him in Search Attachment Feature.
	@Test(priority=8)
	public void testC8714() {
		CustomAssert csAssert = new CustomAssert();
		TabListData tabObj = new TabListData();
		try {
			logger.info("Starting Test: Verifying that Supplier User should be able to Search only those documents that are shared with him in Search Attachment Feature.");
			if (!loginWithEndUser(supplierTypeUserName, supplierTypeUserPassword)) {
				throw new SkipException("Couldn't login with Supplier Type UserName [" + supplierTypeUserName + "] and Password [" + supplierTypeUserPassword +
						"]. Hence skipping test.");
			}

			SearchAttachmentHelper attachmentHelperObj = new SearchAttachmentHelper();
			String attachmentResponse = attachmentHelperObj.getSearchAttachmentResponse("a", 61, 100, 0);

			if (ParseJsonResponse.validJsonResponse(attachmentResponse)) {
				Integer filteredCount = attachmentHelperObj.getFilteredCount(attachmentResponse);

				if (filteredCount == null || filteredCount == 0) {
					logger.error("Couldn't get Filtered Count from Search Attachment Response. Hence skipping test");
					throw new SkipException("Couldn't get Filtered Count from Search Attachment Response. Hence skipping test.");
				}

				int noOfRandomRecordsToTest = 10;

				JSONObject jsonObj = new JSONObject(attachmentResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("searchResults");

				logger.info("Selecting {} Random Results to Verify.", noOfRandomRecordsToTest);
				int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, jsonArr.length() - 1, noOfRandomRecordsToTest);

				List<Map<String, String>> documentRecordList = new ArrayList<>();

				for (int randomNumber : randomNumbers) {
					JSONObject resultJsonObj = jsonArr.getJSONObject(randomNumber);
					Integer recordId = resultJsonObj.getInt("entityId");
					Boolean isDraft = resultJsonObj.getBoolean("draft");

					Map<String, String> documentRecordMap = new HashMap<>();
					documentRecordMap.put("documentFileId", String.valueOf(resultJsonObj.getLong("documentFileId")));
					documentRecordMap.put("recordId", recordId.toString());
					documentRecordMap.put("documentName", resultJsonObj.getString("documentName"));
					documentRecordMap.put("isDraft", isDraft.toString());

					documentRecordList.add(documentRecordMap);
				}

				if (!loginWithEndUser(endUserName, endUserPassword)) {
					throw new SkipException("Couldn't login with Client Type End UserName [" + endUserName + "] and Password [" + endUserPassword +
							"]. Hence skipping test.");
				}

				int contractDocumentTabDocumentNameColumnId, contractDocumentTabShareWithSupplierColumnId, communicationTabDocumentNameColumnId,
						communicationTabShareWithSupplierColumnId, contractDocumentTabVersionColumnId;
				contractDocumentTabDocumentNameColumnId = contractDocumentTabShareWithSupplierColumnId = communicationTabShareWithSupplierColumnId =
						communicationTabDocumentNameColumnId = contractDocumentTabVersionColumnId = -1;

				for (Map<String, String> documentRecordMap : documentRecordList) {
					boolean documentFound = false;
					Long documentFileId = Long.parseLong(documentRecordMap.get("documentFileId"));
					Integer recordId = Integer.parseInt(documentRecordMap.get("recordId"));
					String documentName = documentRecordMap.get("documentName");
					Boolean isDraft = Boolean.parseBoolean(documentRecordMap.get("isDraft"));

					logger.info("Verifying that Document having DocumentFileId {} is shared with Supplier Type User or not.", documentFileId);
					logger.info("Hitting TabListData API for Contract Contract Document Tab and Record Id {}", recordId);

					if (isDraft) {
						String attachmentResultVersionNo = attachmentHelperObj.getVersionNoFromDocumentNameValue(documentName);
						String contractDocumentTabListResponse = tabObj.hitTabListData(65,61,recordId);

						if (contractDocumentTabDocumentNameColumnId == -1) {
							contractDocumentTabDocumentNameColumnId = ListDataHelper.getColumnIdFromColumnName(contractDocumentTabListResponse, "documentname");
						}

						if (contractDocumentTabShareWithSupplierColumnId == -1) {
							contractDocumentTabShareWithSupplierColumnId = ListDataHelper.getColumnIdFromColumnName(contractDocumentTabListResponse,
									"sharewithsupplierflag");
						}

						if (contractDocumentTabVersionColumnId == -1) {
							contractDocumentTabVersionColumnId = ListDataHelper.getColumnIdFromColumnName(contractDocumentTabListResponse, "version");
						}

						JSONObject contractDocTabJsonObj = new JSONObject(contractDocumentTabListResponse);
						JSONArray contractDocTabJsonArr = contractDocTabJsonObj.getJSONArray("data");

						for (int i = 0; i < contractDocTabJsonArr.length(); i++) {
							String tabListDocumentNameValue = contractDocTabJsonArr.getJSONObject(i).getJSONObject(String.valueOf(contractDocumentTabDocumentNameColumnId))
									.getString("value");

							Long actualDocumentFileId = TabListDataHelper.getDocumentFileIdFromDocumentNameValue(tabListDocumentNameValue, documentName);

							if (actualDocumentFileId != null && documentFileId.equals(actualDocumentFileId)) {
								String tabListVersion = contractDocTabJsonArr.getJSONObject(i).getJSONObject(String.valueOf(contractDocumentTabVersionColumnId))
										.getString("value");

								if (tabListVersion.trim().equalsIgnoreCase(attachmentResultVersionNo)) {
									documentFound = true;
									String shareWithSupplierFlagValue = contractDocTabJsonArr.getJSONObject(i)
											.getJSONObject(String.valueOf(contractDocumentTabShareWithSupplierColumnId)).getString("value");

									if (shareWithSupplierFlagValue != null && !shareWithSupplierFlagValue.trim().equalsIgnoreCase("yes")) {
										csAssert.assertTrue(false, "Document having DocumentFileId " + documentFileId + " and Document Name " +
												documentName + " is not Shared with Supplier but still is Visible and Searchable. Record Id " + documentFileId);
									}
									break;
								}
							}
						}
					} else {
						//If Document not found in Contract Contract Document Tab then Check with Communication Tab
						logger.info("Hitting TabListData API for Contract Communication Tab and Record Id {}", recordId);
						String payloadForCommunicationTabResponse = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":500," +
								"\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

						String communicationTabResponse = tabObj.hitTabListData(65, 61, recordId, payloadForCommunicationTabResponse);
						if (communicationTabDocumentNameColumnId == -1) {
							communicationTabDocumentNameColumnId = ListDataHelper.getColumnIdFromColumnName(communicationTabResponse, "document");
						}

						if (communicationTabShareWithSupplierColumnId == -1) {
							communicationTabShareWithSupplierColumnId = ListDataHelper.getColumnIdFromColumnName(communicationTabResponse, "share_with_supplier");
						}

						JSONObject communicationTabJsonObj = new JSONObject(communicationTabResponse);
						JSONArray communicationTabJsonArr = communicationTabJsonObj.getJSONArray("data");

						for (int i = 0; i < communicationTabJsonArr.length(); i++) {
							String documentValue = communicationTabJsonArr.getJSONObject(i).getJSONObject(String.valueOf(communicationTabDocumentNameColumnId))
									.getString("value");

							Long actualDocumentFileId = TabListDataHelper.getDocumentFileIdFromDocumentNameValue(documentValue, documentName);

							if (actualDocumentFileId != null && documentFileId.equals(actualDocumentFileId)) {
								documentFound = true;
								String shareWithSupplierFlagValue = communicationTabJsonArr.getJSONObject(i)
										.getJSONObject(String.valueOf(communicationTabShareWithSupplierColumnId)).getString("value");

								if (shareWithSupplierFlagValue != null && !shareWithSupplierFlagValue.trim().equalsIgnoreCase("true")) {
									csAssert.assertTrue(false, "Document having DocumentFileId " + documentFileId +
											" is not Shared with Supplier but still is Visible and Searchable. Record Id " + recordId);
								}
								break;
							}
						}
					}

					if (!documentFound) {
						csAssert.assertTrue(false, "Document having DocumentFileId " + documentFileId + " and Document Name " + documentName +
								" not found in both Contract Document Tab and Communication Tab of Contract Record Id " + recordId);
					}
				}
			} else {
				csAssert.assertTrue(false, "Search Attachment API Response for QueryText [a], Limit 100 and Offset 0 is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying that Supplier User should be able to Search only those documents that are shared with him in Search Attachment Feature. " +
					"{}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying that Supplier User should be able to Search only those documents that are shared " +
					"with him in Search Attachment Feature. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//To Verify that If Certain fields are restricted from Supplier View, then User should not be able to Search on those fields using MetaDataSearch.
	@Test(priority=9)
	public void testC8715() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Verify that If Certain fields are restricted from Supplier View then User should not be able to Search " +
					"on those fields using MetaDataSearch");
			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			FieldProvisioningHelper fieldProHelperObj = new FieldProvisioningHelper();
			List<String> allHiddenFields = fieldProHelperObj.getAllHiddenFieldsOfSupplierTypeUser(61);

			if (allHiddenFields == null) {
				logger.error("Couldn't get All Hidden Fields for Supplier Type User from Field Provisioning Response. Hence skipping test.");
				throw new SkipException("Couldn't get All Hidden Fields for Supplier Type User from Field Provisioning Response. Hence skipping test.");
			}

			if (!loginWithEndUser(supplierTypeUserName, supplierTypeUserPassword)) {
				throw new SkipException("Couldn't login with Supplier Type UserName [" + supplierTypeUserName + "] and Password [" + supplierTypeUserPassword +
						"]. Hence skipping test.");
			}

			ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
			defaultUserListObj.hitListRendererDefaultUserListMetadata(279);
			String defaultUserListResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
				DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();

				for (String hiddenField : allHiddenFields) {
					String displayFormat = defaultUserListHelperObj.getFilterMetadataPropertyFromName(defaultUserListResponse, hiddenField, "displayFormat");

					if (displayFormat != null && !displayFormat.trim().equalsIgnoreCase("{}")) {
						csAssert.assertTrue(false, "Expected DisplayFormat Property of FilterMetadata having Default Name [" + hiddenField +
								"]: {} and Actual DisplayFormat: [" + displayFormat.trim() + "]. Hence Field " + hiddenField + " is Searchable through MetaData Search.");
					}
				}
			} else {
				csAssert.assertTrue(false, "ListRenderer DefaultUserListMetadata API Response for Contract is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating that if Certain fields are restricted from Supplier View " +
					"then User should not be able to Search on those fields using MetaDataSearch. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	/*@Test(priority =10)
	public void deleteAllData() throws SQLException {

		int lastUserId = AppUserDbHelper.getLatestUserId(2);
		int lastUACId = AccessCriteriaDbHelper.getLatestUacId();

		AccessCriteriaDbHelper.deleteUserDetails(lastUserId);
		AccessCriteriaDbHelper.deleteAccessCriteria(lastUACId);
	}*/
}