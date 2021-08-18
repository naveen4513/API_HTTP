package com.sirionlabs.test.bulkAction;

import com.sirionlabs.api.bulkaction.BulkActionList;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestBulkActionEndUser extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestBulkActionEndUser.class);

	private List<String> entitiesToTestList = new ArrayList<>();

	@BeforeClass
	public void beforeClass() {
		String[] entitiesToTest = {
				"obligations",
				"child obligations",
				"service levels",
				"child service levels",
				"consumptions",
				"invoices",
				"service data",
				"clauses",
				"definition",
				"governance body"
		};

		entitiesToTestList.addAll(Arrays.asList(entitiesToTest));
	}

	/*
	TC-C9076: Verify that Bulk Action is present.
	 */
	@Test
	public void testC9076() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test C9076: Verify that Bulk Action is present.");

			for (String entityName : entitiesToTestList) {
				int listId = ConfigureConstantFields.getListIdForEntity(entityName);

				logger.info("Hitting DefaultUserListMetadata API for Entity {}", entityName);
				ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
				defaultUserListObj.hitListRendererDefaultUserListMetadata(listId);
				String defaultUserListMetadataResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
					logger.info("Validating that Bulk Action is Available for Entity {}", entityName);
					JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);

					if (!jsonObj.has("bulkAction") || !jsonObj.getBoolean("bulkAction")) {
						csAssert.assertTrue(false, "Bulk Action option not Available in DefaultUserListMetadata API for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C9076. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9098: Verify that Status and Next Action is Drop-Down.
	 */
	@Test
	public void testC9098() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test C9098: Verify that Status and Next Action is Drop-Down.");

			for (String entityName : entitiesToTestList) {
				int listId = ConfigureConstantFields.getListIdForEntity(entityName);

				logger.info("Hitting BulkAction List API for Entity {}", entityName);
				String bulkActionListResponse = executor.get(BulkActionList.getApiPath(listId), BulkActionList.getHeaders()).getResponse().getResponseBody();

				if (ParseJsonResponse.validJsonResponse(bulkActionListResponse)) {
					logger.info("Validating Status and Next Action DropDowns of Bulk Action for Entity {}", entityName);

					JSONObject jsonObj = new JSONObject(bulkActionListResponse);
					jsonObj = jsonObj.getJSONObject("taskToNextTaskMap");

					JSONArray jsonArr = jsonObj.names();

					for (int i = 0; i < jsonArr.length(); i++) {
						String statusName = jsonArr.get(i).toString();
						JSONArray statusJsonArr = jsonObj.getJSONArray(statusName);

						if (statusJsonArr.length() < 1) {
							csAssert.assertTrue(false, "Status " + statusName + " of Entity " + entityName + " doesn't have any option for Next Action");
						}
					}
				} else {
					csAssert.assertTrue(false, "Bulk Action List API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C9098. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9104: Verify that Bulk Checkbox is displayed in all Rows and as 1st Column for all Entities.
	 */
	@Test
	public void testC9104() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test C9104: Verify that Bulk Checkbox is displayed in all rows and as 1st Column.");
			DefaultUserListMetadataHelper defaultHelperObj = new DefaultUserListMetadataHelper();

			for (String entityName : entitiesToTestList) {
				int listId = ConfigureConstantFields.getListIdForEntity(entityName);

				logger.info("Hitting DefaultUserListMetadata API for Entity {}", entityName);
				ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
				defaultUserListObj.hitListRendererDefaultUserListMetadata(listId);
				String defaultUserListMetadataResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
					logger.info("Validating that Bulk Checkbox is 1st Column for Entity {}", entityName);
					String orderId = defaultHelperObj.getColumnPropertyValueFromQueryName(defaultUserListMetadataResponse, "bulkcheckbox",
							"order");

					if (orderId == null) {
						csAssert.assertTrue(false, "Couldn't get Order Id for BulkCheckBox column from DefaultUserListMetadata API Response for Entity " +
								entityName);
					} else if (!orderId.equalsIgnoreCase("0")) {
						csAssert.assertTrue(false, "Bulk Checkbox is not at 1st Column for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C9104. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C9140: Verify that After clicking on apply button, only those records should be shown which match as per the selected status.
	TC-9143: Verify that no record returned if status not matched.
	 */
	@Test
	public void testC9140() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test C9140: Verify that After clicking on Apply Button, only those records are shown which match as per the selected status.");

			ListRendererListData listDataObj = new ListRendererListData();

			for (String entityName : entitiesToTestList) {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
				int listId = ConfigureConstantFields.getListIdForEntity(entityName);

				logger.info("Hitting BulkAction List API for Entity {}", entityName);
				String bulkActionListResponse = executor.get(BulkActionList.getApiPath(listId), BulkActionList.getHeaders()).getResponse().getResponseBody();

				if (ParseJsonResponse.validJsonResponse(bulkActionListResponse)) {
					JSONObject jsonObj = new JSONObject(bulkActionListResponse);
					jsonObj = jsonObj.getJSONObject("taskToNextTaskMap");

					JSONArray jsonArr = jsonObj.names();

					for (int i = 0; i < jsonArr.length(); i++) {
						String statusName = jsonArr.get(i).toString();
						String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\"," +
								"\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"currentTask\":\"" + statusName + "\"}}}";

						logger.info("Hitting ListData API for Entity {} and Status {}", entityName, statusName);
						listDataObj.hitListRendererListData(listId, payloadForListData);
						String listDataResponse = listDataObj.getListDataJsonStr();

						if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
							if (ListDataHelper.getFilteredListDataCount(listDataResponse) > 0) {
								String columnName = "status";

								if (entityName.equalsIgnoreCase("child obligations") || entityName.equalsIgnoreCase("child service levels")) {
									columnName = "performancestatus";
								}

								int statusColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, columnName);
								int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

								JSONObject listDataJsonObj = new JSONObject(listDataResponse);
								JSONArray listDataJsonArr = listDataJsonObj.getJSONArray("data");

								for (int j = 0; j < listDataJsonArr.length(); j++) {
									String statusValue = listDataJsonArr.getJSONObject(j).getJSONObject(String.valueOf(statusColumnNo)).getString("value");

									if (!statusValue.equalsIgnoreCase(statusName)) {
										boolean validationFailed = false;

										if (statusName.equalsIgnoreCase("Archive")) {
											//Special handling for Archive Status

											if (!statusValue.equalsIgnoreCase("Archived")) {
												validationFailed = true;
											}
										} else if (statusName.equalsIgnoreCase("Upcoming") || statusName.equalsIgnoreCase("Overdue")) {
											//Special handling for Upcoming/Overdue Status

											if (!(statusValue.equalsIgnoreCase("Upcoming") || statusValue.equalsIgnoreCase("Overdue"))) {
												validationFailed = true;
											}
										}

										if (validationFailed) {
											String idValue = listDataJsonArr.getJSONObject(j).getJSONObject(String.valueOf(idColumnNo)).getString("value");
											int recordId = ListDataHelper.getRecordIdFromValue(idValue);

											csAssert.assertTrue(false, "Expected Status Value: " + statusName + " and Actual Status Value: " + statusValue +
													" for Record Id " + recordId + " of Entity " + entityName);
											break;
										}
									}
								}
							}
						} else {
							csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " and Status " + statusName +
									" is an Invalid JSON.");
						}
					}
				} else {
					csAssert.assertTrue(false, "Bulk Action List API Response for Entity " + entityName + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C9140. " + e.getMessage());
		}

		csAssert.assertAll();
	}
}
