package com.sirionlabs.test.bulkAction;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsList;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.clientAdmin.supplierAccess.SupplierAccessUpdate;
import com.sirionlabs.api.clientAdmin.supplierAccessVendorLevel.AdminSupplierAccessVendorLevelCreate;
import com.sirionlabs.api.clientAdmin.supplierAccessVendorLevel.AdminVendorSupplierAccess;
import com.sirionlabs.api.clientAdmin.systemEmailConfigurations.Create;
import com.sirionlabs.api.clientAdmin.systemEmailConfigurations.EmailDataForCreate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientAdmin.AdminSupplierAccessHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBulkActionClientAdmin extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestBulkActionClientAdmin.class);

	private boolean loginSuccessfulWithClientAdmin = false;

	@BeforeClass
	public void beforeClass() {
		AdminHelper helperObj = new AdminHelper();

		loginSuccessfulWithClientAdmin = helperObj.loginWithClientAdminUser();
	}

	@AfterClass
	public void afterClass() {
		logger.info("Logging back with End User.");
		String endUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
		String endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

		Check checkObj = new Check();
		checkObj.hitCheck(endUserName, endUserPassword);
	}

	/*
	TC-C9091: Verify that Bulk Email can be configured from Client Admin and Created mail is displayed in email name drop down.
	 */
	@Test(enabled = false)
	public void testBulkActionEmailConfiguration() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C9091: Verify Bulk Email Configuration from Client Admin");
			String[] allEntitiesToTest = {
					"obligations",
					"child obligations",
					"service levels",
					"child service levels",
					"consumptions",
					"invoices",
					"service data",
					"clauses",
					"governance body"
			};

			if (!loginSuccessfulWithClientAdmin) {
				throw new SkipException("Couldn't login with Client Admin User.");
			}

			BulkOperationsHelper bulkHelperObj = new BulkOperationsHelper();

			HashMap<String, String> headersForEmailData = EmailDataForCreate.getHeaders();

			for (String entity : allEntitiesToTest) {
				logger.info("Validating Bulk Email Configuration for Entity {}", entity);

				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entity);
				String apiPath = EmailDataForCreate.getAPIPath(entityTypeId);

				logger.info("Hitting EmailDataForCreate API for Entity {} having EntityTypeId {}", entity, entityTypeId);
				String emailDataResponse = executor.post(apiPath, headersForEmailData, null).getResponse().getResponseBody();

				if (ParseJsonResponse.validJsonResponse(emailDataResponse)) {
					JSONObject jsonObj = new JSONObject(emailDataResponse);

					csAssert.assertTrue(jsonObj.has("bulkBodyData"), "BulkBodyData Object not found in EmailDataForCreate API Response for Entity " +
							entity + " having EntityTypeId " + entityTypeId);
					csAssert.assertTrue(jsonObj.has("bulkSubjectData"), "BulkSubjectData Object not found in EmailDataForCreate API Response for Entity " +
							entity + " having EntityTypeId " + entityTypeId);

					logger.info("Hitting System Email Configuration Create API for Entity {} having EntityTypeId {}", entity, entityTypeId);

					HashMap<String, String> headersForCreate = Create.getHeaders();
					apiPath = Create.getAPIPath();
					String emailName = "TestEmailCreationFor" + entity;
					HashMap<String, String> params = Create.getParameters(entityTypeId, emailName, "${vendorName}," + entity + "(#${id}) has been approved",
							"", "${vendorName}," + entity + "have been approved", "");
					Integer responseCode = executor.postMultiPartFormData(apiPath, headersForCreate, params).getResponse().getResponseCode();

					if (responseCode == 302) {
						logger.info("Checking if Email is created for Entity {} or not.", entity);
						boolean emailCreated = bulkHelperObj.entityActionEmailCreated(emailName);

						if (emailCreated) {
							logger.info("Deleting Email Entries from DB for Entity {}", entity);

							//Delete Email from DB
							boolean deleteEmailInDb = bulkHelperObj.deleteAllEntriesInEntityActionEmailForName(emailName);
							csAssert.assertTrue(deleteEmailInDb, "Couldn't delete Email Creation Entries in DB for Entity " + entity);
						} else {
							csAssert.assertTrue(false, "Couldn't create Email for Entity " + entity);
						}
					} else {
						csAssert.assertTrue(false, "Email Creation failed for Entity " + entity + ". Response Code received: " + responseCode);
					}

				} else {
					csAssert.assertTrue(false, "EmailDataForCreate API Response for Entity " + entity + " having EntityTypeId " + entityTypeId +
							" is an Invalid JSON.");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Bulk Email Configuration from Client Admin TC-C9091. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9099: Verify Bulk Action Check Permission is present in all related entities permission page.
	TC-C9110: Bulk Action Checkbox is selectable at User Role Group.
	 */
	@Test
	public void testBulkActionPermissionInUserRoleGroup() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C9099: Verify Bulk Action Permission.");

			String[] entitiesToTest = {
					"Obligation",
					"Child Obligation",
					"Service Levels",
					"Child Service Level",
					"Consumption",
					"Invoice",
					"Contract Service Data",
					"Clause",
					"Governance Body"
			};

			if (!loginSuccessfulWithClientAdmin) {
				throw new SkipException("Couldn't login with Client Admin User.");
			}

			String roleGroupsListApiPath = MasterUserRoleGroupsList.getAPIPath();
			HashMap<String, String> headers = MasterUserRoleGroupsList.getHeaders();

			logger.info("Hitting MasterUserRoleGroups List API.");
			String userRoleGroupsListResponse = executor.get(roleGroupsListApiPath, headers).getResponse().getResponseBody();
			Integer roleGroupId = MasterUserRoleGroupsList.getUserRoleGroupId(userRoleGroupsListResponse, "Admin");

			if (roleGroupId == null) {
				throw new SkipException("Couldn't get Id for Role Group Admin.");
			}

			String roleGroupsUpdateApiPath = MasterUserRoleGroupsUpdate.getApiPath(roleGroupId);
			headers = MasterUserRoleGroupsUpdate.getHeaders();

			logger.info("Hitting MasterUserRoleGroups Update API for Role Group Id {}", roleGroupId);
			String roleGroupUpdateResponse = executor.get(roleGroupsUpdateApiPath, headers).getResponse().getResponseBody();

			Document html = Jsoup.parse(roleGroupUpdateResponse);
			Element div = html.getElementsByClass("accordion user_permission").get(0);
			Elements allSubDivs = div.children();

			for (String entity : entitiesToTest) {
				logger.info("Validating Bulk Action Permission for Entity {}", entity);
				boolean bulkActionFound = false;

				for (int i = 0; i < allSubDivs.size(); i = i + 2) {
					Element subDiv = allSubDivs.get(i);
					String sectionName = subDiv.child(0).child(0).childNode(0).toString().trim();

					if (sectionName.equalsIgnoreCase(entity + ":")) {
						Elements allChildDivsOfEntity = allSubDivs.get(i + 1).child(0).child(0).child(0).child(0).children();

						for (Element childDiv : allChildDivsOfEntity) {
							String propertyName = childDiv.childNode(5).toString().trim();

							if (propertyName.equalsIgnoreCase("Bulk Action")) {
								bulkActionFound = true;
								break;
							}
						}
						break;
					}
				}

				csAssert.assertTrue(bulkActionFound, "Bulk Action CheckBox not found in User Role Groups Update API Response for Entity " + entity);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Bulk Action Permission TC-C9099. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9109: Verify Bulk Action Checkbox is present or not in Supplier Access.
	 */
	@Test
	public void testBulkActionPermissionInSupplierAccess() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C9109: Verify Bulk Action Checkbox in Supplier Access.");

			String[] entitiesToTest = {
					"Obligation",
					"Child Obligation",
					"Service Levels",
					"Child Service Level",
					"Consumption",
					"Invoice",
					"Contract Service Data",
					"Clause",
					"Governance Body"
			};

			if (!loginSuccessfulWithClientAdmin) {
				throw new SkipException("Couldn't login with Client Admin User.");
			}

			String apiPath = SupplierAccessUpdate.getApiPath();
			HashMap<String, String> headers = SupplierAccessUpdate.getHeaders();

			logger.info("Hitting Supplier Access Update API.");
			String supplierAccessUpdateResponse = executor.get(apiPath, headers).getResponse().getResponseBody();

			Document html = Jsoup.parse(supplierAccessUpdateResponse);
			Element div = html.getElementById("permission").child(1).child(0);
			Elements allSubDivs = div.children();

			for (String entity : entitiesToTest) {
				logger.info("Validating Bulk Action Permission on Supplier Access Page for Entity {}", entity);
				boolean bulkActionFound = false;

				for (int i = 0; i < allSubDivs.size(); i = i + 2) {
					Element subDiv = allSubDivs.get(i);
					String sectionName = subDiv.child(0).child(0).childNode(0).toString().trim();

					if (sectionName.equalsIgnoreCase(entity + ":")) {
						Elements allChildDivsOfEntity = allSubDivs.get(i + 1).child(0).child(0).child(0).child(0).children();

						for (Element childDiv : allChildDivsOfEntity) {
							String propertyName = childDiv.childNode(2).toString().trim();

							if (propertyName.equalsIgnoreCase("Bulk Action")) {
								bulkActionFound = true;
								break;
							}
						}
						break;
					}
				}

				csAssert.assertTrue(bulkActionFound, "Bulk Action CheckBox not found in Supplier Access Update API Response for Entity " + entity);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C9109 Bulk Action Checkbox in Supplier Access. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9112: Verify Bulk Action Checkbox is selectable at Supplier Access at Vendor Hierarchy Level.
	 */
	@Test
	public void testBulkActionPermissionInSupplierAccessAtVendorHierarchyLevel() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C9112: Verify Bulk Action Checkbox in Supplier Access at VH Level.");

			String[] entitiesToTest = {
					"Obligation",
					"Child Obligation",
					"Service Levels",
					"Child Service Level",
					"Consumption",
					"Invoice",
					"Contract Service Data",
					"Clause",
					"Governance Body"
			};

			if (!loginSuccessfulWithClientAdmin) {
				throw new SkipException("Couldn't login with Client Admin User.");
			}

			String apiPath = AdminSupplierAccessVendorLevelCreate.getApiPath();
			HashMap<String, String> headers = AdminSupplierAccessVendorLevelCreate.getHeaders();

			logger.info("Hitting Supplier Access Vendor Level Create API.");
			String supplierAccessVHLevelCreateResponse = executor.get(apiPath, headers).getResponse().getResponseBody();

			AdminSupplierAccessHelper helperObj = new AdminSupplierAccessHelper();
			List<Map<String, String>> allVendors = helperObj.getAllVendorsPresentUnderSupplierAccessAtVendorLevel(supplierAccessVHLevelCreateResponse);

			if (allVendors == null || allVendors.isEmpty()) {
				logger.error("Couldn't get all Vendor Options from Supplier Access Vendor Level Create API. Hence skipping test.");
				throw new SkipException("Couldn't get all Vendor Options from Supplier Access Vendor Level Create API. Hence skipping test.");
			}

			int randomNumberForVendor = RandomNumbers.getRandomNumberWithinRangeIndex(0, allVendors.size() - 1);

			String vendorId = allVendors.get(randomNumberForVendor).get("id");
			String vendorName = allVendors.get(randomNumberForVendor).get("name");

			logger.info("Hitting Vendor Supplier Access API for Vendor Id {} and Vendor Name {}", vendorId, vendorName);
			String vendorResponse = executor.get(AdminVendorSupplierAccess.getApiPath(vendorId, vendorName), AdminVendorSupplierAccess.getHeaders())
					.getResponse().getResponseBody();

			Document html = Jsoup.parse(vendorResponse);
			Element div = html.getElementById("permission").child(1).child(0);
			Elements allSubDivs = div.children();

			for (String entity : entitiesToTest) {
				logger.info("Validating Bulk Action Permission on Supplier Access Page for Entity {}", entity);
				boolean bulkActionFound = false;

				for (int i = 0; i < allSubDivs.size(); i = i + 2) {
					Element subDiv = allSubDivs.get(i);
					String sectionName = subDiv.child(0).child(0).childNode(0).toString().trim();

					if (sectionName.equalsIgnoreCase(entity + ":")) {
						Elements allChildDivsOfEntity = allSubDivs.get(i + 1).child(0).child(0).child(0).child(0).children();

						for (Element childDiv : allChildDivsOfEntity) {
							String propertyName = childDiv.childNode(2).toString().trim();

							if (propertyName.equalsIgnoreCase("Bulk Action")) {
								bulkActionFound = true;
								break;
							}
						}
						break;
					}
				}

				csAssert.assertTrue(bulkActionFound, "Bulk Action CheckBox not found in Supplier Access at VH Level for Entity " + entity);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C9112 Bulk Action Checkbox in Supplier Access at VH Level. " + e.getMessage());
		}
		csAssert.assertAll();
	}
}
