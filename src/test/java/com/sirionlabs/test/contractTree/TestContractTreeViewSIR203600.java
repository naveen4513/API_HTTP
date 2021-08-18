package com.sirionlabs.test.contractTree;

import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.UserListMetaData;
import com.sirionlabs.api.userPreference.UserPreferenceData;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestContractTreeViewSIR203600 {

	private final static Logger logger = LoggerFactory.getLogger(TestContractTreeViewSIR203600.class);
	private int privateViewId = -1;
	private int publicViewId = -1;

	private UserPreferenceData userPreferenceObj = new UserPreferenceData();

	/*
	TC-C63284: Contract Document Tree | Save View Private/Public
	 */
	@Test
	public void testSaveViewC63284() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C63284: Verify Saving View (Private/Public) in Contract Tree");
			logger.info("Hitting List Data API Version 2 for Suppliers.");

			ListRendererListData listDataObj = new ListRendererListData();
			String listDataResponse = listDataObj.listDataResponseV2(1, "suppliers", 50, null);

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				for (int i = 0; i < jsonArr.length(); i++) {
					String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
					int supplierRecordId = ListDataHelper.getRecordIdFromValue(idValue);

					ContractTreeData contractTreeObj = new ContractTreeData();
					String contractTreeResponse = contractTreeObj.hitContractTreeListAPIV1(1, supplierRecordId);

					if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
						List<Map<String, String>> allChildrenMap = contractTreeObj.getAllChildrenMapIncludingParent(contractTreeResponse);

						if (allChildrenMap == null) {
							throw new SkipException("Couldn't Get All Children Map from Contract Tree V1 Response.");
						}

						if (allChildrenMap.size() == 0) {
							continue;
						}

						validatePrivateViewCreation(csAssert);

						validatePublicViewCreation(csAssert);
						break;
					} else {
						csAssert.assertTrue(false, "Contract Tree API V1 Response is an Invalid JSON for Supplier Id " + supplierRecordId);
					}
				}
			} else {
				csAssert.assertTrue(false, "ListData API Version 2 Response for Suppliers is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Contract Tree Save View. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C63285: Contract Document Tree | List Private/Public View
	 */
	@Test(priority = 1)
	public void testListViewC63285() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Listing Private/Public Views.");
			logger.info("Hitting User Preference List API.");
			userPreferenceObj.hitUserPreferenceListAPI(434);
			String listResponse = userPreferenceObj.getResponselistUserPreference();

			if (ParseJsonResponse.validJsonResponse(listResponse)) {
				JSONArray jsonArr = new JSONArray(listResponse);

				if (privateViewId != -1) {
					boolean privateViewFound = false;

					for (int i = 0; i < jsonArr.length(); i++) {
						int id = jsonArr.getJSONObject(i).getInt("id");

						if (id == privateViewId) {
							privateViewFound = true;
							break;
						}
					}

					csAssert.assertTrue(privateViewFound, "Private View having Id " + privateViewId + " is not found in User Preference List API Response.");
				}

				if (publicViewId != -1) {
					boolean publicViewFound = false;

					for (int i = 0; i < jsonArr.length(); i++) {
						int id = jsonArr.getJSONObject(i).getInt("id");

						if (id == publicViewId) {
							publicViewFound = true;
							break;
						}
					}

					csAssert.assertTrue(publicViewFound, "Public View having Id " + publicViewId + " is not found in User Preference List API Response.");
				}
			} else {
				csAssert.assertTrue(false, "User Preference List API Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating List Views TC-C63285. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C63286: Contract Document Tree | Edit/Delete Views
	 */
	@Test(priority = 2)
	public void testEditDeleteViewC63286() {
		CustomAssert csAssert = new CustomAssert();

		try {
			//Edit and Delete Private View.
			if (privateViewId != -1) {
				logger.info("Editing Private View having Id: {}", privateViewId);
				String privateViewUpdatedName = "Test API Automation Contract Tree Private View Updated";
				String payloadForPrivateViewUpdate = getPayloadForViewUpdate(privateViewUpdatedName, privateViewId, false);

				logger.info("Hitting User Preference Update API for Private View having Id {}", privateViewId);
				userPreferenceObj.hitUserPreferenceUpdateAPI(434, payloadForPrivateViewUpdate);

				String updateResponse = userPreferenceObj.getResponseUpdateUserPreference();

				if (ParseJsonResponse.validJsonResponse(updateResponse)) {
					JSONArray jsonArr = new JSONArray(updateResponse);
					boolean privateViewFound = false;

					for (int i = 0; i < jsonArr.length(); i++) {
						int id = jsonArr.getJSONObject(i).getInt("id");

						if (id == privateViewId) {
							privateViewFound = true;
							String viewName = jsonArr.getJSONObject(i).getString("name");

							if (!viewName.equalsIgnoreCase(privateViewUpdatedName)) {
								csAssert.assertTrue(false, "Expected Private View Name after Update: [" + privateViewUpdatedName +
										"] and Actual View Name: [" + viewName + "] for View having Id " + privateViewId);
							}

							break;
						}
					}

					csAssert.assertTrue(privateViewFound, "Private View having Id " + privateViewId + " not found after Updating it.");
				} else {
					csAssert.assertTrue(false, "User Preference Update API Response for Private View having Id " + privateViewId + " is an Invalid JSON.");
				}

				//Delete Private View
				logger.info("Deleting Private View having Id: {}", privateViewId);
				logger.info("Hitting User Preference Delete API for Private View having Id {}", privateViewId);
				userPreferenceObj.hitUserPreferenceDeleteAPI(434, privateViewId, false);
				String deleteViewResponse = userPreferenceObj.getResponseDeleteUserPreference();

				if (ParseJsonResponse.validJsonResponse(deleteViewResponse)) {
					JSONArray jsonArr = new JSONArray(deleteViewResponse);

					for (int i = 0; i < jsonArr.length(); i++) {
						int viewId = jsonArr.getJSONObject(i).getInt("id");

						if (viewId == privateViewId) {
							csAssert.assertTrue(false, "Couldn't Delete Private View having Id " + privateViewId);
						}
					}
				} else {
					csAssert.assertTrue(false, "Delete View API Response for Private View having Id " + privateViewId + " is an Invalid JSON.");
				}
			}

			//Edit and Delete Private View.
			if (publicViewId != -1) {
				logger.info("Editing Public View having Id: {}", publicViewId);
				String publicViewUpdatedName = "Test API Automation Contract Tree Public View Updated";
				String payloadForPublicViewUpdate = getPayloadForViewUpdate(publicViewUpdatedName, publicViewId, true);

				logger.info("Hitting User Preference Update API for Public View having Id {}", publicViewId);
				userPreferenceObj.hitUserPreferenceUpdateAPI(434, payloadForPublicViewUpdate);

				String updateResponse = userPreferenceObj.getResponseUpdateUserPreference();

				if (ParseJsonResponse.validJsonResponse(updateResponse)) {
					JSONArray jsonArr = new JSONArray(updateResponse);
					boolean publicViewFound = false;

					for (int i = 0; i < jsonArr.length(); i++) {
						int id = jsonArr.getJSONObject(i).getInt("id");

						if (id == publicViewId) {
							publicViewFound = true;
							String viewName = jsonArr.getJSONObject(i).getString("name");

							if (!viewName.equalsIgnoreCase(publicViewUpdatedName)) {
								csAssert.assertTrue(false, "Expected Public View Name after Update: [" + publicViewUpdatedName +
										"] and Actual View Name: [" + viewName + "] for View having Id " + publicViewId);
							}

							break;
						}
					}

					csAssert.assertTrue(publicViewFound, "Public View having Id " + publicViewId + " not found after Updating it.");
				} else {
					csAssert.assertTrue(false, "User Preference Update API Response for Public View having Id " + publicViewId + " is an Invalid JSON.");
				}

				//Delete Public View
				logger.info("Deleting Public View having Id: {}", publicViewId);
				userPreferenceObj.hitUserPreferenceDeleteAPI(434, publicViewId, true);
				String deleteViewResponse = userPreferenceObj.getResponseDeleteUserPreference();

				if (ParseJsonResponse.validJsonResponse(deleteViewResponse)) {
					JSONArray jsonArr = new JSONArray(deleteViewResponse);

					for (int i = 0; i < jsonArr.length(); i++) {
						int viewId = jsonArr.getJSONObject(i).getInt("id");

						if (viewId == publicViewId) {
							csAssert.assertTrue(false, "Couldn't Delete Public View having Id " + privateViewId);
						}
					}
				} else {
					csAssert.assertTrue(false, "Delete View API Response for Public View having Id " + publicViewId + " is an Invalid JSON.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Edit/Delete View TC-C63286. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private String getPayloadForViewCreation(String viewName, Boolean isPublic) {
		return "{\"filterJson\":\"{\\\"filterMap\\\":{\\\"entityTypeId\\\":61,\\\"orderByColumnName\\\":\\\"id\\\",\\\"orderDirection\\\":\\\"desc nulls last\\\"," +
				"\\\"filterJson\\\":{\\\"6\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":6,\\\"filterName\\\":\\\"status\\\"," +
				"\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"11\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":11," +
				"\\\"filterName\\\":\\\"documentType\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"15\\\":{\\\"filterId\\\":\\\"15\\\"," +
				"\\\"filterName\\\":\\\"expirationDate\\\",\\\"entityFieldId\\\":null,\\\"entityFieldHtmlType\\\":null,\\\"dueDateRange\\\":\\\"true\\\"," +
				"\\\"dayOffset\\\":null,\\\"duration\\\":null},\\\"16\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[{\\\"id\\\":\\\"1012\\\"," +
				"\\\"name\\\":\\\"APAC\\\"}]},\\\"filterId\\\":16,\\\"filterName\\\":\\\"regions\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null}," +
				"\\\"17\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":17,\\\"filterName\\\":\\\"functions\\\",\\\"entityFieldHtmlType\\\":null," +
				"\\\"entityFieldId\\\":null},\\\"18\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":18,\\\"filterName\\\":\\\"services\\\"," +
				"\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"344\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":344," +
				"\\\"filterName\\\":\\\"customer\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null}}},\\\"selectedColumns\\\":[]}\",\"maxNumberOfColumns\":0," +
				"\"listId\":434,\"name\":\"" + viewName + "\",\"columns\":[],\"listViewType\":{},\"publicVisibility\":" + isPublic.toString() + "}";
	}

	private String getPayloadForViewUpdate(String viewName, int viewId, Boolean isPublic) {
		return "{\"filterJson\":\"{\\\"filterMap\\\":{\\\"entityTypeId\\\":61,\\\"orderByColumnName\\\":\\\"id\\\",\\\"orderDirection\\\":\\\"desc nulls last\\\"," +
				"\\\"filterJson\\\":{\\\"6\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":6,\\\"filterName\\\":\\\"status\\\"," +
				"\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"11\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":11," +
				"\\\"filterName\\\":\\\"documentType\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"15\\\":{\\\"filterId\\\":\\\"15\\\"," +
				"\\\"filterName\\\":\\\"expirationDate\\\",\\\"entityFieldId\\\":null,\\\"entityFieldHtmlType\\\":null,\\\"dueDateRange\\\":\\\"true\\\"," +
				"\\\"dayOffset\\\":null,\\\"duration\\\":null},\\\"16\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[{\\\"id\\\":\\\"1012\\\"," +
				"\\\"name\\\":\\\"APAC\\\"}]},\\\"filterId\\\":16,\\\"filterName\\\":\\\"regions\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null}," +
				"\\\"17\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":17,\\\"filterName\\\":\\\"functions\\\",\\\"entityFieldHtmlType\\\":null," +
				"\\\"entityFieldId\\\":null},\\\"18\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":18,\\\"filterName\\\":\\\"services\\\"," +
				"\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null},\\\"344\\\":{\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[]},\\\"filterId\\\":344," +
				"\\\"filterName\\\":\\\"customer\\\",\\\"entityFieldHtmlType\\\":null,\\\"entityFieldId\\\":null}}},\\\"selectedColumns\\\":[]}\",\"maxNumberOfColumns\":0," +
				"\"listId\":434,\"id\":\"" + viewId + "\",\"name\":\"" + viewName + "\",\"columns\":[],\"listViewType\":{},\"publicVisibility\":" + isPublic.toString() + "}";
	}

	private void validatePrivateViewCreation(CustomAssert csAssert) {
		try {
			String privateViewName = "Test API Automation Contract Tree Private View";
			String payload = getPayloadForViewCreation(privateViewName, false);

			userPreferenceObj.hitUserPreferenceCreateAPI(434, payload);
			String saveViewResponse = userPreferenceObj.getResponseCreateUserPreference();

			if (ParseJsonResponse.validJsonResponse(saveViewResponse)) {
				JSONArray preferenceJsonArr = new JSONArray(saveViewResponse);
				boolean privateViewCreatedSuccessfully = false;

				for (int j = 0; j < preferenceJsonArr.length(); j++) {
					JSONObject preferenceJsonObj = preferenceJsonArr.getJSONObject(j);

					if (preferenceJsonObj.getString("name").equalsIgnoreCase(privateViewName)) {
						privateViewCreatedSuccessfully = true;

						String visibility = preferenceJsonObj.getString("visibility");

						if (!visibility.equalsIgnoreCase("My View")) {
							csAssert.assertTrue(false, "Created Private View: [" + privateViewName + "] but Visibility is not Private");
						}

						privateViewId = preferenceJsonObj.getInt("id");
						break;
					}
				}

				csAssert.assertTrue(privateViewCreatedSuccessfully, "Couldn't Save Private View with Name: [" + privateViewName + "]");

				UserListMetaData metaDataObj = new UserListMetaData();

				Map<String, String> params = new HashMap<>();
				params.put("preferenceId", String.valueOf(privateViewId));
				params.put("contractId", "");
				params.put("publicVisibility", "false");

				String userListMetaDataResponseCode = metaDataObj.hitUserListMetaData(434, params);

				if (!userListMetaDataResponseCode.equalsIgnoreCase("200")) {
					csAssert.assertTrue(false, "Couldn't Apply Private View: [" + privateViewName + "] on Contract Tree.");
				}
			} else {
				csAssert.assertTrue(false, "Couldn't Save Private View with Name: [" + privateViewName + "].");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Private View Creation.");
		}
	}

	private void validatePublicViewCreation(CustomAssert csAssert) {
		try {
			String publicViewName = "Test API Automation Contract Tree Public View";
			String payload = getPayloadForViewCreation(publicViewName, true);

			userPreferenceObj.hitUserPreferenceCreateAPI(434, payload);
			String saveViewResponse = userPreferenceObj.getResponseCreateUserPreference();

			if (ParseJsonResponse.validJsonResponse(saveViewResponse)) {
				JSONArray preferenceJsonArr = new JSONArray(saveViewResponse);
				boolean publicViewCreatedSuccessfully = false;

				for (int j = 0; j < preferenceJsonArr.length(); j++) {
					JSONObject preferenceJsonObj = preferenceJsonArr.getJSONObject(j);

					if (preferenceJsonObj.getString("name").equalsIgnoreCase(publicViewName)) {
						publicViewCreatedSuccessfully = true;

						String visibility = preferenceJsonObj.getString("visibility");

						if (!visibility.equalsIgnoreCase("Shared By Me")) {
							csAssert.assertTrue(false, "Created Public View: [" + publicViewName + "] but Visibility is not Public");
						}

						publicViewId = preferenceJsonObj.getInt("id");
						break;
					}
				}

				csAssert.assertTrue(publicViewCreatedSuccessfully, "Couldn't Save Public View with Name: [" + publicViewName + "]");

				UserListMetaData metaDataObj = new UserListMetaData();

				Map<String, String> params = new HashMap<>();
				params.put("preferenceId", String.valueOf(publicViewId));
				params.put("contractId", "");
				params.put("publicVisibility", "true");

				String userListMetaDataResponseCode = metaDataObj.hitUserListMetaData(434, params);

				if (!userListMetaDataResponseCode.equalsIgnoreCase("200")) {
					csAssert.assertTrue(false, "Couldn't Apply Public View: [" + publicViewName + "] on Contract Tree.");
				}
			} else {
				csAssert.assertTrue(false, "Couldn't Save Public View with Name: [" + publicViewName + "].");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Public View Creation.");
		}
	}
}