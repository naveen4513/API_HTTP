package com.sirionlabs.test.clause;

import com.sirionlabs.api.clientAdmin.fieldLabel.CreateForm;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldLabel.MessagesList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientAdmin.FieldLabelHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TestClauseUsageStatistics extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestClauseUsageStatistics.class);

	private static String endUserName = null;
	private static String endUserPassword = null;

	private AdminHelper adminHelperObj = new AdminHelper();

	@BeforeClass
	public void beforeClass() {
		endUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
		endUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

		testCasesMap = getTestCasesMapping();
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

		if (!loginWithEndUser(endUserName, endUserPassword)) {
			logger.info("Couldn't Login back with Original UserName [{}] and Password [{}]. Hence aborting Automation Suite.", endUserName, endUserPassword);
			System.exit(0);
		}
	}

	//To Validate that 'Used in X Templates' section should be there on Clause Listing Page and Grid View.
	@Test
	public void testC10819() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Validate that 'Used in X Templates' section should be present on Clause Listing Page and Grid View.");
			logger.info("Hitting ListData API for Clause and Grid View");

			String listDataResponse = getListDataResponseForEntityGridView("clauses");

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (!listDataResponseContainsContractTemplateCountField(listDataResponse)) {
					csAssert.assertTrue(false, "Contract Template Count field not found in ListData Response for Clause and Grid View.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Clauses and Grid View is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating that 'Used in X Templates' section should be present on Clause Listing Page " +
					"and Grid View. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC10819"), csAssert);
		csAssert.assertAll();
	}

	//To Validate that 'Used in X Templates' section should be there on Clause Listing Page and List View.
	@Test
	public void testC10820() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Validate that 'Used in X Templates' section should be present on Clause Listing Page and List View.");
			logger.info("Hitting ListData API for Clause and List View");

			String listDataResponse = getListDataResponseForEntityListView("clauses");

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (!listDataResponseContainsContractTemplateCountField(listDataResponse)) {
					csAssert.assertTrue(false, "Contract Template Count field not found in ListData Response for Clause and List View.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Clauses and List View is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating that 'Used in X Templates' section should be present on Clause Listing Page " +
					"and List View. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC10820"), csAssert);
		csAssert.assertAll();
	}

	//To Validate that 'Used in X Templates' section should be there on Definition Listing Page and Grid View.
	@Test
	public void testC10834() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Validate that 'Used in X Templates' section should be present on Definition Listing Page and Grid View.");
			logger.info("Hitting ListData API for Definition and Grid View");

			String listDataResponse = getListDataResponseForEntityGridView("definition");

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (!listDataResponseContainsContractTemplateCountField(listDataResponse)) {
					csAssert.assertTrue(false, "Contract Template Count field not found in ListData Response for Definition and Grid View.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Definition and Grid View is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating that 'Used in X Templates' section should be present on Definition Listing Page " +
					"and Grid View. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC10834"), csAssert);
		csAssert.assertAll();
	}

	//To Validate that 'Used in X Templates' section should be there on Definition Listing Page and List View.
	@Test
	public void testC13419() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Validate that 'Used in X Templates' section should be present on Definition Listing Page and List View.");
			logger.info("Hitting ListData API for Clause and List View");

			String listDataResponse = getListDataResponseForEntityListView("definition");

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				if (!listDataResponseContainsContractTemplateCountField(listDataResponse)) {
					csAssert.assertTrue(false, "Contract Template Count field not found in ListData Response for Definition and List View.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Definition and List View is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating that 'Used in X Templates' section should be present on Definition Listing Page " +
					"and List View. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC13419"), csAssert);
		csAssert.assertAll();
	}

	//TC-C10821: To Verify renaming of new field should be available on Client Admin and should rename and reflect the Metadata field name "Used in <X> templates".
	@Test
	public void testC10821() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Verify Renaming of New Field should be available on Client Admin and should rename and reflect the Metadata Field Name " +
					"[Used in <X> Templates].");

			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Hitting FieldLabel CreateForm API.");
			CreateForm createFormObj = new CreateForm();
			String createFormResponse = createFormObj.hitFieldLabelCreateForm();
			FieldLabelHelper fieldLabelHelperObj = new FieldLabelHelper();

			logger.info("Getting GroupId Value for Dialogue Messages");
			Integer groupIdValue = fieldLabelHelperObj.getFieldLabelGroupValueFromCreateFormAPI(createFormResponse, "Dialogue Messages");

			if (groupIdValue == null) {
				logger.error("Couldn't get GroupIdValue for Group [Dialogue Messages] from CreateForm API Response. Hence skipping test.");
				throw new SkipException("Couldn't get GroupIdValue for Group [Dialogue Messages] from CreateForm API Response. Hence skipping test.");
			}

			logger.info("Getting LanguageId for English (English)");
			Integer languageId = fieldLabelHelperObj.getFieldLabelLanguageIdFromCreateFormAPI(createFormResponse, "English (English)");

			if (languageId == null) {
				logger.error("Couldn't get LanguageId for Language [English (English)] from CreateForm API Response. Hence skipping test.");
				throw new SkipException("Couldn't get LanguageId for Language [English (English)] from CreateForm API Response. Hence skipping test.");
			}

			logger.info("Hitting FieldRenaming API for Language Id {} and Group Id {}", languageId, groupIdValue);
			FieldRenaming fieldRenamingObj = new FieldRenaming();
			String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(languageId, groupIdValue);
			String payloadForFieldRenamingUpdate = null;

			if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
				JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("childGroups");

				boolean fieldLabelFound = false;
				String originalFieldLabelValue;
				String newFieldLabelValue = "New Value Used in %s template(s)";
				Long fieldId = null;

				for (int i = 0; i < jsonArr.length(); i++) {
					if (jsonArr.getJSONObject(i).getString("name").trim().equalsIgnoreCase("dialogue Messages : Clause")) {
						JSONArray fieldLabelsArr = jsonArr.getJSONObject(i).getJSONArray("fieldLabels");

						for (int j = 0; j < fieldLabelsArr.length(); j++) {
							if (fieldLabelsArr.getJSONObject(j).getString("name").trim().equalsIgnoreCase("Used in %s template(s)")) {
								fieldLabelFound = true;
								fieldId = fieldLabelsArr.getJSONObject(j).getLong("id");
								originalFieldLabelValue = fieldLabelsArr.getJSONObject(j).getString("clientFieldName").trim();
								payloadForFieldRenamingUpdate = fieldRenamingResponse.replace("\"clientFieldName\":\"" + originalFieldLabelValue + "\"",
										"\"clientFieldName\":\"" + newFieldLabelValue + "\"");
								break;
							}
						}
						break;
					}
				}

				if (!fieldLabelFound) {
					csAssert.assertTrue(false, "Couldn't find FieldLabel by Name [Used in %s template(s)] in FieldRenaming API Response.");
				} else {
					logger.info("Hitting Field Renaming Update API.");
					String fieldRenamingUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

					if (ParseJsonResponse.validJsonResponse(fieldRenamingUpdateResponse)) {
						JSONObject updateJsonObj = new JSONObject(fieldRenamingUpdateResponse);

						if (updateJsonObj.getBoolean("isSuccess")) {
							if (!loginWithEndUser(endUserName, endUserPassword)) {
								logger.error("Couldn't login with End User [{}] and Password [{}]. Hence skipping test", endUserName, endUserPassword);
								throw new SkipException("Couldn't login with End User [" + endUserName + "] and Password [" + endUserPassword + "]. Hence skipping test.");
							}

							logger.info("Hitting Field Label Messages List API.");

							MessagesList listObj = new MessagesList();
							String payload = "[" + fieldId + "]";
							String messagesListResponse = listObj.hitFieldLabelMessagesList(payload);

							if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
								JSONObject listJsonObj = new JSONObject(messagesListResponse);
								listJsonObj = listJsonObj.getJSONObject(fieldId.toString());

								if (!listJsonObj.getString("name").trim().equalsIgnoreCase(newFieldLabelValue)) {
									csAssert.assertTrue(false, "Expected Field Label Value: " + newFieldLabelValue + " and Actual Field Label Value: " +
											listJsonObj.getString("name"));
								}

								logger.info("Reverting Field Labels to Original Values.");

								if (!adminHelperObj.loginWithClientAdminUser()) {
									logger.error("Couldn't login with Client Admin User. Hence skipping test");
									throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
								}

								fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);

								logger.info("Logging Back with End User");
								loginWithEndUser(endUserName, endUserPassword);
							} else {
								csAssert.assertTrue(false, "Field Label Messages List API Response is an Invalid JSON.");
							}
						} else {
							logger.error("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
							throw new SkipException("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
						}
					} else {
						csAssert.assertTrue(false, "Field Renaming Update API Response is an Invalid JSON.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Field Renaming API Response for Language Id " + languageId + " and Group Id " + groupIdValue +
						" is an Invalid JSON.");
			}
		} catch (SkipException e) {
			addTestResultAsSkip(getTestCaseIdForMethodName("testC10821"), csAssert);
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Test C10821. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Test C10821. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC10821"), csAssert);
		csAssert.assertAll();
	}

	//TC-C10835: To Verify the Column Contract Template Count data under Definition list view to show number of templates associated to particular definition.
	@Test(dependsOnMethods = "testC13419")
	public void testC10835() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Verify the Contract Template Count data under Definition List View to Show No of Templates associated.");
			logger.info("Hitting ListData API for Clause and List View");

			String listDataResponse = getListDataResponseForEntityListView("definition");
			int entityTypeId = ConfigureConstantFields.getEntityIdByName("definition");

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, jsonArr.length() - 1, 3);
				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
				int contractTemplateCountColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "contract_template_count");
				int definitionForwardReferenceAffectedTemplatesTabId = TabListDataHelper.getIdForTab("definitions forward reference affected templates");
				TabListData tabListDataObj = new TabListData();
				String tabListDataPayload = TabListDataHelper.getDefaultTabListDataPayload(entityTypeId, 1000, 0);

				for (int randomNumber : randomNumbers) {
					String idValue = jsonArr.getJSONObject(randomNumber).getJSONObject(String.valueOf(idColumnNo)).getString("value");
					int recordId = ListDataHelper.getRecordIdFromValue(idValue);

					if (recordId != -1) {
						logger.info("Validating Contract Template Count for Definition Record Id {}", recordId);
						int actualContractTemplateCount = 0;

						if (!jsonArr.getJSONObject(randomNumber).getJSONObject(String.valueOf(contractTemplateCountColumnNo)).isNull("value")) {
							actualContractTemplateCount = Integer.parseInt(jsonArr.getJSONObject(randomNumber).getJSONObject(String.valueOf(contractTemplateCountColumnNo))
									.getString("value"));
						}

						logger.info("Hitting TabListData API for Tab [Definitions Forward Reference Affected Templates], Tab Id {} and Record Id {}",
								definitionForwardReferenceAffectedTemplatesTabId, recordId);

						String tabListDataResponse = tabListDataObj.hitTabListData(definitionForwardReferenceAffectedTemplatesTabId, entityTypeId, recordId,
								tabListDataPayload);

						if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
							int expectedContractTemplateCount = getExpectedContractTemplateCount(tabListDataResponse);

							if (actualContractTemplateCount != expectedContractTemplateCount) {
								csAssert.assertTrue(false, "Expected Contract Template Count in TabListData API for Record Id is: " +
										expectedContractTemplateCount + " and Actual Count is: " + actualContractTemplateCount);
							}
						} else {
							csAssert.assertTrue(false, "TabListData API for Tab [Definitions Forward Reference Affected Templates], Tab Id " +
									definitionForwardReferenceAffectedTemplatesTabId + " and Record Id " + recordId + " is an Invalid JSON.");
						}
					}
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for Definition and List View is an Invalid JSON.");
			}
		} catch (SkipException e) {
			addTestResultAsSkip(getTestCaseIdForMethodName("testC10835"), csAssert);
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Test C10835 i.e. " +
					"'Verify Contract Template Count data under Definition List View to Show No of Templates associated.' " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC10835"), csAssert);
		csAssert.assertAll();
	}

	//TC-C13417: To Verify renaming of new field should be available on Client Admin and should rename and reflect the Metadata field name "Contract Template Count".
	@Test
	public void testC13417() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test: To Verify Renaming of New Field should be available on Client Admin and should rename and reflect the Metadata Field Name " +
					"[Contract Template Count].");

			if (!adminHelperObj.loginWithClientAdminUser()) {
				logger.error("Couldn't login with Client Admin User. Hence skipping test");
				throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
			}

			logger.info("Hitting FieldLabel CreateForm API.");
			CreateForm createFormObj = new CreateForm();
			String createFormResponse = createFormObj.hitFieldLabelCreateForm();
			FieldLabelHelper fieldLabelHelperObj = new FieldLabelHelper();

			logger.info("Getting GroupId Value for Clause");
			Integer groupIdValue = fieldLabelHelperObj.getFieldLabelGroupValueFromCreateFormAPI(createFormResponse, "Entity", "Clause");

			if (groupIdValue == null) {
				logger.error("Couldn't get GroupIdValue for ParentGroup [Entity] and  Group [Clause] from CreateForm API Response. Hence skipping test.");
				throw new SkipException("Couldn't get GroupIdValue for ParentGroup [Entity] and Group [Clause] from CreateForm API Response. Hence skipping test.");
			}

			logger.info("Getting LanguageId for English (English)");
			Integer languageId = fieldLabelHelperObj.getFieldLabelLanguageIdFromCreateFormAPI(createFormResponse, "English (English)");

			if (languageId == null) {
				logger.error("Couldn't get LanguageId for Language [English (English)] from CreateForm API Response. Hence skipping test.");
				throw new SkipException("Couldn't get LanguageId for Language [English (English)] from CreateForm API Response. Hence skipping test.");
			}

			logger.info("Hitting FieldRenaming API for Language Id {} and Group Id {}", languageId, groupIdValue);
			FieldRenaming fieldRenamingObj = new FieldRenaming();
			String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(languageId, groupIdValue);
			String payloadForFieldRenamingUpdate = null;

			if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
				JSONObject jsonObj = new JSONObject(fieldRenamingResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("childGroups");

				boolean fieldLabelFound = false;
				String originalFieldLabelValue;
				String newFieldLabelValue = "New Value Contract Template Count";

				for (int i = 0; i < jsonArr.length(); i++) {
					if (jsonArr.getJSONObject(i).getString("name").trim().equalsIgnoreCase("List Specific Columns")) {
						JSONArray fieldLabelsArr = jsonArr.getJSONObject(i).getJSONArray("fieldLabels");

						for (int j = 0; j < fieldLabelsArr.length(); j++) {
							if (fieldLabelsArr.getJSONObject(j).getString("name").trim().equalsIgnoreCase("CONTRACT TEMPLATE COUNT")) {
								fieldLabelFound = true;
								originalFieldLabelValue = fieldLabelsArr.getJSONObject(j).getString("clientFieldName").trim();
								payloadForFieldRenamingUpdate = fieldRenamingResponse.replace("\"clientFieldName\":\"" + originalFieldLabelValue + "\"",
										"\"clientFieldName\":\"" + newFieldLabelValue + "\"");
								break;
							}
						}
						break;
					}
				}

				if (!fieldLabelFound) {
					csAssert.assertTrue(false, "Couldn't find FieldLabel by Name [CONTRACT TEMPLATE COUNT] in FieldRenaming API Response.");
				} else {
					logger.info("Hitting Field Renaming Update API.");
					String fieldRenamingUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

					if (ParseJsonResponse.validJsonResponse(fieldRenamingUpdateResponse)) {
						JSONObject updateJsonObj = new JSONObject(fieldRenamingUpdateResponse);

						if (updateJsonObj.getBoolean("isSuccess")) {
							if (!loginWithEndUser(endUserName, endUserPassword)) {
								logger.error("Couldn't login with End User [{}] and Password [{}]. Hence skipping test", endUserName, endUserPassword);
								throw new SkipException("Couldn't login with End User [" + endUserName + "] and Password [" + endUserPassword + "]. Hence skipping test.");
							}

							logger.info("Hitting DefaultUserListMetadata API for Clause");
							ListRendererDefaultUserListMetaData defaultUserListObj = new ListRendererDefaultUserListMetaData();
							int listId = ConfigureConstantFields.getListIdForEntity("clauses");
							defaultUserListObj.hitListRendererDefaultUserListMetadata(listId);
							String defaultUserListResponse = defaultUserListObj.getListRendererDefaultUserListMetaDataJsonStr();

							if (ParseJsonResponse.validJsonResponse(defaultUserListResponse)) {
								JSONObject defaultUserListJsonObj = new JSONObject(defaultUserListResponse);
								JSONArray columnsJsonArr = defaultUserListJsonObj.getJSONArray("columns");

								for (int j = 0; j < columnsJsonArr.length(); j++) {
									if (columnsJsonArr.getJSONObject(j).getString("queryName").trim().equalsIgnoreCase("contract_template_count")) {
										if (!columnsJsonArr.getJSONObject(j).getString("name").trim().equalsIgnoreCase(newFieldLabelValue)) {
											csAssert.assertTrue(false, "Expected Field Label Value: " + newFieldLabelValue +
													" and Actual Field Label Value: " + columnsJsonArr.getJSONObject(j).getString("name"));
										}

										break;
									}
								}

								logger.info("Reverting Field Labels to Original Values.");

								if (!adminHelperObj.loginWithClientAdminUser()) {
									logger.error("Couldn't login with Client Admin User. Hence skipping test");
									throw new SkipException("Couldn't login with Client Admin User. Hence skipping test.");
								}

								fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);

								logger.info("Logging Back with End User");
								loginWithEndUser(endUserName, endUserPassword);
							} else {
								csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Clause is an Invalid JSON.");
							}
						} else {
							logger.error("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
							throw new SkipException("Couldn't update Field Labels using FieldRenaming Update API. Hence skipping test.");
						}
					} else {
						csAssert.assertTrue(false, "Field Renaming Update API Response is an Invalid JSON.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Field Renaming API Response for Language Id " + languageId + " and Group Id " + groupIdValue +
						" is an Invalid JSON.");
			}
		} catch (SkipException e) {
			addTestResultAsSkip(getTestCaseIdForMethodName("testC13417"), csAssert);
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Test C13417. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Test C13417. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testC13417"), csAssert);
		csAssert.assertAll();
	}

	private Boolean listDataResponseContainsContractTemplateCountField(String listDataResponse) {
		try {
			JSONObject jsonObj = new JSONObject(listDataResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("data");

			int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, jsonArr.length() - 1);
			jsonObj = jsonArr.getJSONObject(randomNumber);
			jsonArr = jsonObj.names();

			for (int i = 0; i < jsonArr.length(); i++) {
				String columnName = jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("columnName");

				if (columnName.trim().equalsIgnoreCase("id")) {
					String[] value = jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("value").trim().split(Pattern.quote(":;"));
					logger.info("Checking ListData API Response for Record Id " + value[1]);
					continue;
				}

				if (columnName.trim().equalsIgnoreCase("contract_template_count")) {
					return true;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Checking whether ListData Response contains Contract Template Count Field or not. " + e.getMessage());
		}
		return false;
	}

	private int getExpectedContractTemplateCount(String tabListDataResponse) {
		try {
			JSONObject jsonObj = new JSONObject(tabListDataResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("data");
			int filteredCount = TabListDataHelper.getFilteredCount(tabListDataResponse);

			if (filteredCount > 0) {
				String statusColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "status");

				for (int i = 0; i < jsonArr.length(); i++) {
					String statusValue = jsonArr.getJSONObject(i).getJSONObject(statusColumnId).getString("value");

					if (statusValue != null && (statusValue.trim().equalsIgnoreCase("Archived") || statusValue.trim().equalsIgnoreCase("On Hold"))) {
						filteredCount--;
					}
				}

				return filteredCount;
			}

			return 0;
		} catch (Exception e) {
			logger.error("Exception while Getting Expected Contract Template Count from TabListData API Response. {}", e.getMessage());
			return -1;
		}
	}

	private String getListDataResponseForEntityListView(String entityName) {
		ListRendererListData listDataObj = new ListRendererListData();
		int listId = ConfigureConstantFields.getListIdForEntity(entityName);
		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String payload = ListDataHelper.getDefaultPayloadForListData(entityTypeId, 20);

		Map<String, String> params = new HashMap<>();
		params.put("version", "2.0");

		listDataObj.hitListRendererListData(listId, payload, params);
		return listDataObj.getListDataJsonStr();
	}

	private String getListDataResponseForEntityGridView(String entityName) {
		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String payload = "{\"filterMap\":{\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null," + "\"entityTypeId\":" + entityTypeId +
				",\"hasDefinitionCategoryIds\":true,\"filterJson\":{}}}";

		return ListDataHelper.getListDataResponse(entityName, payload);
	}
}