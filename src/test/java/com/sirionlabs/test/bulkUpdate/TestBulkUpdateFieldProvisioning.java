package com.sirionlabs.test.bulkUpdate;

import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataCreate;
import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataUpdate;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestBulkUpdateFieldProvisioning {

	private final static Logger logger = LoggerFactory.getLogger(TestBulkUpdateFieldProvisioning.class);

	private List<String> entitiesToTest;

	private FieldProvisioning fieldProvisioningObj = new FieldProvisioning();
	private BulkOperationsHelper bulkOperationsHelperObj = new BulkOperationsHelper();
	private AdminHelper adminHelperObj = new AdminHelper();
	private String endUserName;
	private String endUserPassword;


	@BeforeClass
	public void beforeClass() {
		entitiesToTest = getEntitiesToTest();

		endUserName = Check.lastLoggedInUserName;
		endUserPassword = Check.lastLoggedInUserPassword;
	}


	@AfterClass
	public void afterClass() {
		new Check().hitCheck(endUserName, endUserPassword);
	}

	public List<String> getEntitiesToTest() {
		String[] entitiesArr = {"contracts", "child service levels", "disputes", "child obligations", "obligations", "consumptions"};
		return Arrays.asList(entitiesArr);
	}


	/*
	TC-C3352: Verify Bulk Update (Excel) columns in field provisioning as Global Supplier.
	TC-C3381: Verify fields (Privacy, Comments, Actual Date, Requested By and Change Request) are supported for Bulk Update.
	 */
	@Test
	public void testC3352() {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Starting Test TC-C3352: Verify Bulk Update (Excel) columns in field provisioning as Global Supplier.");


		for (String entityName : entitiesToTest) {
			logger.info("Verifying Bulk Update (Excel) columns for Entity {}", entityName);

			try {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);

				if (ParseJsonResponse.validJsonResponse(fieldProvisioningResponse)) {
					//Verify Part 1. Bulk Update Fields should be available.
					List<Map<String, String>> bulkUpdateAvailableFields = fieldProvisioningObj.getAllBulkUpdateAvailableFields(fieldProvisioningResponse);

					if (bulkUpdateAvailableFields == null) {
						throw new SkipException("Couldn't get Bulk Update Available Fields for Entity " + entityName);
					}

					if (bulkUpdateAvailableFields.size() == 0) {
						csAssert.assertTrue(false, "No Bulk Update Available Field found for Entity " + entityName);
					}

					if (!entityName.equalsIgnoreCase("consumptions")) {
						//Verify C3381: Fields (Privacy, Comments, Actual Date, Requested By and Change Request) should be present.
						boolean privacyFieldFound = false;
						boolean commentsFieldFound = false;
						boolean actualDateFieldFound = false;
						boolean requestedByFieldFound = false;
						boolean changeRequestFieldFound = false;

						for (Map<String, String> fieldMap : bulkUpdateAvailableFields) {
							String id = fieldMap.get("id");

							if (id.equalsIgnoreCase("11860")) {
								privacyFieldFound = true;
							} else if (id.equalsIgnoreCase("86")) {
								commentsFieldFound = true;
							} else if (id.equalsIgnoreCase("87")) {
								actualDateFieldFound = true;
							} else if (id.equalsIgnoreCase("88")) {
								requestedByFieldFound = true;
							} else if (id.equalsIgnoreCase("89")) {
								changeRequestFieldFound = true;
							}
						}

						csAssert.assertTrue(privacyFieldFound,
								"Comments and Attachments - Privacy Field not found in All Bulk Update Available Fields for Entity " + entityName);
						csAssert.assertTrue(commentsFieldFound,
								"Comments and Attachments - Comments Field not found in All Bulk Update Available Fields for Entity " + entityName);
						csAssert.assertTrue(actualDateFieldFound,
								"Comments and Attachments - Actual Date Field not found in All Bulk Update Available Fields for Entity " + entityName);
						csAssert.assertTrue(requestedByFieldFound,
								"Comments and Attachments - Requested By Field not found in All Bulk Update Available Fields for Entity " + entityName);
						csAssert.assertTrue(changeRequestFieldFound,
								"Comments and Attachments - Change Request Field not found in All Bulk Update Available Fields for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "Field Provisioning API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			} catch (SkipException e) {
				throw new SkipException(e.getMessage());
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating Bulk Update (Excel) Columns for Entity " + entityName + ". " + e.getMessage());
			}
		}
		csAssert.assertAll();
	}


	/*
	TC-C3372: Verify that Client Admin Dynamic Fields should come in Field Provisioning.
	TC-C3377: Verify that user should not be able to edit and update Bulk Update Fields for Inactive Dynamic Fields.
	TC-C3378: Verify that user should not able to edit and update Bulk Update Fields for Active Dynamic Fields.
	 */
	@Test
	public void testC3372() {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Starting Test TC-C3372: Verify that all the Client Admin Dynamic Fields should come in Field Provisioning Page.");

		for (String entityName : entitiesToTest) {
			logger.info("Verifying Client Admin Dynamic Fields for Entity {}", entityName);
			int newlyCreatedDynamicFieldId = -1;
			String fieldName = entityName + "dynamicField1";

			try {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);

				if (ParseJsonResponse.validJsonResponse(fieldProvisioningResponse)) {
					//Verify Part 1. Dynamic Fields should be visible in Bulk Update.
					List<Map<String, String>> allFields = fieldProvisioningObj.getAllFields(fieldProvisioningResponse);

					if (allFields == null) {
						throw new SkipException("Couldn't get All Provisioning Fields for Entity " + entityName);
					}

					List<Map<String, String>> allDynamicFields = bulkOperationsHelperObj.getAllDynamicFieldsOfEntity(entityTypeId);

					if (allDynamicFields == null) {
						throw new SkipException("Couldn't get All Dynamic Fields of Entity " + entityName);
					}

					for (Map<String, String> dynamicFieldMap : allDynamicFields) {
						String id = dynamicFieldMap.get("id");
						String active = dynamicFieldMap.get("active");
						String apiName = dynamicFieldMap.get("apiName");
						boolean dynamicFieldFound = false;

						for (Map<String, String> bulkUpdateField : allFields) {
							if (bulkUpdateField.get("id").equalsIgnoreCase(id)) {
								dynamicFieldFound = true;

								if (!bulkUpdateField.get("active").equalsIgnoreCase(active)) {
									csAssert.assertTrue(false, "Expected Active Status: [" + active + "] and Actual Status: [" +
											bulkUpdateField.get("active") + "] for Field having Id " + id + " and ApiName " + apiName);
								}
								break;
							}
						}

						csAssert.assertTrue(dynamicFieldFound, "Couldn't find Dynamic Field having Id " + id + ", ApiName: " + apiName + " of Entity " + entityName);
					}

					//Verify Part 2. Create Dynamic Field.

					boolean dynamicFieldCreated = DynamicMetadataCreate.createDynamicField(fieldName, fieldName, 89800 + entityTypeId, entityTypeId,
							getHeaderIdForEntity(entityName), 20, 1, 1);

					if (!dynamicFieldCreated) {
						throw new SkipException("Couldn't create Dynamic Field for Entity " + entityName);
					}

					fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);

					allFields = fieldProvisioningObj.getAllFields(fieldProvisioningResponse);

					if (allFields == null) {
						throw new SkipException("Couldn't get All Provisioning Fields for Entity " + entityName);
					}

					newlyCreatedDynamicFieldId = DynamicMetadataCreate.getFieldId(fieldName);

					if (newlyCreatedDynamicFieldId == -1) {
						throw new SkipException("Couldn't get Id of Newly Created Field " + fieldName + " of Entity " + entityName);
					}

					Map<String, String> fieldMap = fieldProvisioningObj.getFieldPropertiesFromFieldId(fieldProvisioningResponse, newlyCreatedDynamicFieldId);

					if (fieldMap == null || fieldMap.isEmpty()) {
						throw new SkipException("Couldn't get Properties of Newly Created Field " + fieldName + " of Entity " + entityName);
					}

					csAssert.assertTrue(fieldMap.get("bulkUpdateAvailable").equalsIgnoreCase("true"),
							"Expected BulkUpdateAvailable Value: [true] and Actual Value: " + fieldMap.get("bulkUpdateAvailable") + " for Entity " + entityName);

					csAssert.assertTrue(fieldMap.get("bulkUpdate").equalsIgnoreCase("false"),
							"Expected bulkUpdate Value: [false] and Actual Value: " + fieldMap.get("bulkUpdate") + " for Entity " + entityName);
				} else {
					csAssert.assertTrue(false, "Field Provisioning API Response for Entity " + entityName + " is an Invalid JSON.");
				}

				validateC3378(entityName, entityTypeId, fieldProvisioningResponse, fieldName, newlyCreatedDynamicFieldId, csAssert);

				validateC3377(entityName, entityTypeId, fieldName, 89800 + entityTypeId, getHeaderIdForEntity(entityName), 20,
						newlyCreatedDynamicFieldId, csAssert);
			} catch (SkipException e) {
				throw new SkipException(e.getMessage());
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating Client Admin Dynamic Fields for Entity " + entityName + ". " + e.getMessage());
			} finally {
				logger.info("Deleting Newly Created Dynamic Field {} of Entity {}", fieldName, entityName);
				if (!DynamicMetadataCreate.deleteDynamicField(fieldName, newlyCreatedDynamicFieldId)) {
					logger.warn("Couldn't delete Newly Created Dynamic Field {} of Entity {}", fieldName, entityName);
				}
			}
		}
		csAssert.assertAll();
	}

	private int getHeaderIdForEntity(String entityName) {
		int headerId = -1;

		switch (entityName) {
			case "contracts":
				headerId = 4;
				break;

			case "child service levels":
				headerId = 1107;
				break;

			case "disputes":
				headerId = 3534;
				break;

			case "child obligations":
				headerId = 1007;
				break;

			case "obligations":
				headerId = 303;
				break;

			case "consumptions":
				headerId = 3917;
				break;
		}

		return headerId;
	}

	private void validateC3378(String entityName, int entityTypeId, String fieldProvisioningResponse, String fieldName, int newlyCreatedFieldId, CustomAssert csAssert) {
		try {
			logger.info("Validating TC-C3378 i.e. Verify user should be able to edit and update the Bulk Update Columns for Active Custom Field for Entity {}", entityName);
			JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("data");

			String payload = jsonArr.toString();

			for (int i = 0; i < jsonArr.length(); i++) {
				JSONObject internalJsonObj = jsonArr.getJSONObject(i);
				String oldJsonStr = internalJsonObj.toString();

				if (internalJsonObj.getJSONObject("field").getInt("id") == newlyCreatedFieldId) {
					internalJsonObj.put("bulkUpdate", true);

					payload = payload.replace(oldJsonStr, internalJsonObj.toString());
					break;
				}
			}

			if (fieldProvisioningObj.hitFieldProvisioning(entityTypeId, payload) != 200) {
				csAssert.assertTrue(false, "Couldn't update Bulk Update Provisioning for Field " + fieldName + " of Entity " + entityName);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C3378 for Entity " + entityName + ". " + e.getMessage());
		}
	}

	private void validateC3377(String entityName, int entityTypeId, String fieldName, int fieldOrder, int headerId, int htmlTypeId, int newlyCreatedFieldId,
	                           CustomAssert csAssert) {
		try {
			logger.info("Validating TC-C3377 i.e. Verify user should not be able to edit and update the Bulk Update Columns for Inactive Custom Field for Entity {}",
					entityName);
			logger.info("Inactivating the Custom Field {} of Entity {}", fieldName, entityName);

			HashMap<String, String> params = DynamicMetadataUpdate.getParameters(fieldName, fieldName, fieldOrder, entityTypeId, headerId, htmlTypeId,
					newlyCreatedFieldId, 1);

			params.remove("active");

			if (!DynamicMetadataUpdate.updateDynamicField(fieldName, entityTypeId, params)) {
				throw new SkipException("Couldn't Inactivate Custom Field " + fieldName + " of Entity " + entityName);
			}

			String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);

			Map<String, String> fieldMap = fieldProvisioningObj.getFieldPropertiesFromFieldId(fieldProvisioningResponse, newlyCreatedFieldId);

			csAssert.assertTrue(fieldMap.get("bulkUpdateAvailable").equalsIgnoreCase("false"),
					"Expected BulkUpdateAvailable Value: [false] and Actual Value: " + fieldMap.get("bulkUpdateAvailable") + " for Entity " + entityName);

			csAssert.assertTrue(fieldMap.get("bulkUpdate").equalsIgnoreCase("false"),
					"Expected bulkUpdate Value: [false] and Actual Value: " + fieldMap.get("bulkUpdate") + " for Entity " + entityName);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating TC-C3378 for Entity " + entityName + ". " + e.getMessage());
		}
	}


	/*
	TC-C4241: Verify that Bulk Update Setting is not available in entities not supported for Bulk Update.
	 */
	@Test
	public void testC4241() {
		CustomAssert csAssert = new CustomAssert();

		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		logger.info("Starting Test TC-C4241: Verify that Bulk Update Setting is not available in entities not supported for Bulk Update.");
		String[] entitiesNotSupportingBulkUpdateArr = {"actions", "change requests", "clauses", "contract draft request", "contract templates",
				"contract template structure", "governance body", "governance body meetings", "interpretations", "invoices", "issues",
				"purchase orders", "suppliers", "work order requests"};

		List<String> entitiesList = new ArrayList<>();
		entitiesList.addAll(Arrays.asList(entitiesNotSupportingBulkUpdateArr));

		adminHelperObj.loginWithClientAdminUser();

		for (String entityName : entitiesList) {
			try {
				logger.info("Validating Test for Entity {}", entityName);

				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				String fieldProvisioningResponse = fieldProvisioningObj.hitFieldProvisioning(entityTypeId);

				if (ParseJsonResponse.validJsonResponse(fieldProvisioningResponse)) {
					List<Map<String, String>> bulkUpdateEnabledFields = fieldProvisioningObj.getAllBulkUpdateEnabledFields(fieldProvisioningResponse);

					if (bulkUpdateEnabledFields == null) {
						csAssert.assertTrue(false, "Couldn't Get All Bulk Update Enabled Fields for Entity " + entityName);
					} else if (bulkUpdateEnabledFields.size() > 0) {
						csAssert.assertTrue(false, "Bulk Update Enabled Fields present in Field Provisioning for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "Field Provisioning Response for Entity " + entityName + " is an Invalid JSON.");
				}
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating TC-C4241 for Entity " + entityName + ". " + e.getMessage());
			}
		}

		csAssert.assertAll();

		new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
	}


	/*
	TC-C4310: Verify that Entity Id should not come in Field Provisioning.
	 */
	@Test
	public void testC4310() {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Starting Test TC-C4310: Verify that Entity Id should not come in Field Provisioning.");

		String lastLoggedInUserName = Check.lastLoggedInUserName;
		String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

		adminHelperObj.loginWithClientAdminUser();

		for (String entityName : entitiesToTest) {
			logger.info("Verifying that Entity Id should not come in Field Provisioning for Entity {}", entityName);

			try {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				String fieldProvisioningResponse = fieldProvisioningObj.hitFieldProvisioning(entityTypeId);

				if (ParseJsonResponse.validJsonResponse(fieldProvisioningResponse)) {
					List<Integer> allFieldIds = fieldProvisioningObj.getAllFieldIds(fieldProvisioningResponse);

					if (allFieldIds == null) {
						throw new SkipException("Couldn't get All Field Ids from Field Provisioning Response for Entity " + entityName);
					}

					int entityIdFieldId = BulkTemplate.getBulkUpdateHeaderIdForEntityId(entityName);

					if (allFieldIds.contains(entityIdFieldId)) {
						csAssert.assertTrue(false, "Entity Id Field is present in Field Provisioning Response for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "Field Provisioning API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			} catch (SkipException e) {
				throw new SkipException(e.getMessage());
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating TC-C4310 for Entity " + entityName + ". " + e.getMessage());
			}
		}
		csAssert.assertAll();

		new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
	}
}