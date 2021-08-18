package com.sirionlabs.test.common;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.searchLayout.SearchLayoutEntityTypes;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.search.MetadataSearchHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestVHSuffixForSupplierUsers extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestVHSuffixForSupplierUsers.class);

	private AdminHelper adminHelperObj = new AdminHelper();
	private Map<Integer, Map<String, String>> allSupplierTypeUsersMap;

	private Integer listDataSize = 10;

	@BeforeClass(groups = { "minor" })
	public void beforeClass() {
		allSupplierTypeUsersMap = adminHelperObj.getAllSupplierTypeUsers();
	}


	/*
	TC-C46290: Verify VH Suffix is displayed for all Supplier Type Users at Entity Listing and Download.
	 */
	@Test(enabled = false)
	public void testVHSuffixInEntityListing() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (allSupplierTypeUsersMap == null) {
				throw new SkipException("Couldn't get All Supplier Type Users Map.");
			}

			String[] allEntitiesArr = {
					"suppliers",
					"contracts",
					"contract draft request",
					"obligations",
					"service levels",
					"interpretations",
					"issues",
					"actions",
					"disputes",
					"invoices",
					"work order requests",
					"governance body",
					"purchase orders",
					"service data"
			};

			List<String> allEntitiesToTestList = new ArrayList<>();
			allEntitiesToTestList.addAll(Arrays.asList(allEntitiesArr));

			logger.info("Starting Test: Verify VH Suffix is displayed for all Supplier Type Users at Entity Listing and Download.");

			ListRendererDefaultUserListMetaData listDefaultObj = new ListRendererDefaultUserListMetaData();
			ListRendererListData listDataObj = new ListRendererListData();
			Show showObj = new Show();

			for (String entityToTest : allEntitiesToTestList) {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityToTest);
				int listId = ConfigureConstantFields.getListIdForEntity(entityToTest);
				String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\"," +
						"\"orderDirection\":\"asc\",\"filterJson\":{}},\"selectedColumns\":[]}";

				logger.info("Hitting DefaultUserList Metadata API for Entity {}", entityToTest);
				listDefaultObj.hitListRendererDefaultUserListMetadata(listId);
				String defaultUserListResponse = listDefaultObj.getListRendererDefaultUserListMetaDataJsonStr();

				logger.info("Hitting ListData API for Entity {}", entityToTest);
				listDataObj.hitListRendererListData(listId, payload);
				String listDataResponse = listDataObj.getListDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
					int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
					JSONObject jsonObj = new JSONObject(listDataResponse);
					JSONArray jsonArr = jsonObj.getJSONArray("data");

					for (int i = 0; i < jsonArr.length(); i++) {
						String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
						int recordId = ListDataHelper.getRecordIdFromValue(idValue);

						logger.info("Hitting Show API for Record Id {} of Entity {}", recordId, entityToTest);
						showObj.hitShow(entityTypeId, recordId);
						String showResponse = showObj.getShowJsonStr();

						if (ParseJsonResponse.validJsonResponse(showResponse)) {
							if (ShowHelper.isShowPageAccessible(showResponse)) {
								logger.info("Getting All Supplier Type Users from Show API Response for Record Id {} of Entity {}", recordId, entityToTest);
								List<Map<String, String>> allSupplierTypeUsersInShowPage = ShowHelper.getAllSupplierTypeUsersFromShowResponse(showResponse);

								if (allSupplierTypeUsersInShowPage != null) {
									if (!allSupplierTypeUsersInShowPage.isEmpty()) {
										for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInShowPage) {
											Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
											String rgLabel = supplierTypeUserMap.get("rgLabel");

											logger.info("Getting Query Name for Field Label {} from DefaultUserListMetadata API Response.", rgLabel);
											String queryName = ListDataHelper.getFieldQueryNameFromName(defaultUserListResponse, rgLabel);

											if (queryName != null) {
												int columnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, queryName);
												String value = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(columnNo)).getString("value");

												Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);
												String showPageUserName = supplierTypeUserMap.get("name");

												logger.info("Validating VH Suffix for User [{}] of Record Id {} of Entity {} in List Data API Response.",
														showPageUserName, recordId, entityToTest);

												String[] valuesArr = value.split(Pattern.quote(","));

												boolean userFoundInListData = false;

												for (String listDataUserName : valuesArr) {
													if (listDataUserName.contains(supplierTypeUserMap.get("name"))) {
														userFoundInListData = true;
														String vhName = expectedUserMap.get("Vendor Hierarchy");

														if (!listDataUserName.contains("(" + vhName + ")")) {
															csAssert.assertTrue(false, "ListData User Name Value: [" + listDataUserName +
																	"] doesn't contain VH Suffix for Record Id " + recordId + " of Entity " + entityToTest);
														}

														if (!showPageUserName.contains("(" + vhName + ")")) {
															csAssert.assertTrue(false, "Show Page User Name Value: [" + listDataUserName +
																	"] doesn't contain VH Suffix for Record Id " + recordId + " of Entity " + entityToTest);
														}
													}
												}

												if (!userFoundInListData) {
													csAssert.assertTrue(false, "User Name " + showPageUserName +
															" not found in ListData Response for Record Id " + recordId + " of Entity " + entityToTest);
												}
											}
										}

										validateVHSuffixOnCreatePage(showResponse, "Record Id " + recordId + " of Entity " + entityToTest, csAssert);

										validateVHSuffixOnEditPage(recordId, entityToTest, csAssert);

										break;
									}
								} else {
									csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from Show API Response of Record Id " +
											recordId + " of Entity " + entityToTest);
								}
							} else {
								csAssert.assertTrue(false, "Show Page is not accessible for Record Id " + recordId + " of Entity " + entityToTest);
							}
						} else {
							csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Entity " + entityToTest +
									" is an Invalid JSON.");
						}
					}
				} else {
					csAssert.assertTrue(false, "ListData API Response for Entity " + entityToTest + " is an Invalid JSON.");
				}
			}

		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix for all Supplier Type Users at Entity Listing. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//ValidateVHSuffixOnCreatePage covers part of TC-C46291
	private void validateVHSuffixOnCreatePage(String showResponse, String additionalInfo, CustomAssert csAssert) {
		try {
			logger.info("Getting All Create Links from Show API Response. {}", additionalInfo);
			Map<Integer, String> allCreateLinks = ShowHelper.getAllCreateLinksMap(showResponse);

			if (allCreateLinks != null) {
				for (Map.Entry<Integer, String> createLink : allCreateLinks.entrySet()) {
					Integer entityTypeId = createLink.getKey();
					String apiPath = createLink.getValue();

					logger.info("Hitting New API for Entity Type Id {} and Path [{}]. {}", entityTypeId, apiPath, additionalInfo);
					String newResponse = executor.get(apiPath, New.getHeaders()).getResponse().getResponseBody();

					if (ParseJsonResponse.successfulResponse(newResponse)) {
						List<Map<String, String>> allSupplierTypeUsersInCreatePage = New.getAllSupplierTypeUsersFromNewResponse(newResponse);

						if (allSupplierTypeUsersInCreatePage != null) {
							if (!allSupplierTypeUsersInCreatePage.isEmpty()) {
								for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInCreatePage) {
									Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
									Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);

									String createPageUserName = supplierTypeUserMap.get("name");

									logger.info("Validating VH Suffix for User [{}] in New API Response. {}", createPageUserName, additionalInfo);
									String vhName = expectedUserMap.get("Vendor Hierarchy");

									if (!createPageUserName.contains("(" + vhName + ")")) {
										csAssert.assertTrue(false, "Create Page User Name Value: [" + createPageUserName +
												"] doesn't contain VH Suffix. " + additionalInfo);
									}
								}
							}
						} else {
							csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from New API Response using ApiPath " +
									apiPath + ". " + additionalInfo);
						}
					} else {
						if (!ParseJsonResponse.containsApplicationError(newResponse)) {
							csAssert.assertTrue(false, "Error in Response of New API using ApiPath " + apiPath + ". " + additionalInfo);
						}
					}
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix on Create Page. " + additionalInfo + ". " + e.getMessage());
		}
	}

	//ValidateVHSuffixOnEditPage covers part of TC-C46291
	private void validateVHSuffixOnEditPage(int recordId, String entityName, CustomAssert csAssert) {
		try {
			logger.info("Hitting Edit Get API for Entity {} and Record Id {}.", entityName, recordId);
			String editResponse = executor.get(Edit.getEditGetApiPath(entityName, recordId), Edit.getHeaders()).getResponse().getResponseBody();

			if (ParseJsonResponse.successfulResponse(editResponse)) {
				EntityEditHelper editHelperObj = new EntityEditHelper();

				List<Map<String, String>> allSupplierTypeUsersInEditPage = editHelperObj.getAllSupplierTypeUsersInEditPage(editResponse);

				if (allSupplierTypeUsersInEditPage != null) {
					if (!allSupplierTypeUsersInEditPage.isEmpty()) {
						for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInEditPage) {
							Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
							Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);

							String editPageUserName = supplierTypeUserMap.get("name");

							logger.info("Validating VH Suffix for User [{}] in Edit API Response for Record Id {} of Entity {}", editPageUserName, recordId, entityName);
							String vhName = expectedUserMap.get("Vendor Hierarchy");

							if (!editPageUserName.contains("(" + vhName + ")")) {
								csAssert.assertTrue(false, "Edit Page User Name Value: [" + editPageUserName +
										"] doesn't contain VH Suffix in Record Id " + recordId + " of Entity " + entityName);
							}
						}
					}
				} else {
					csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from Edit API Response for Entity " +
							entityName + " and Record Id " + recordId + ". ");
				}
			} else {
				csAssert.assertTrue(false, "Error in Response of New API of Record Id " + recordId + " of Entity " + entityName);
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix on Edit Page for Record Id " + recordId + " of Entity " + entityName);
		}
	}


	@Test(enabled = false)
	public void testVHSuffixInListingFilters() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (allSupplierTypeUsersMap == null) {
				throw new SkipException("Couldn't get All Supplier Type Users Map.");
			}

			String[] allEntitiesArr = {
					"suppliers",
					"contracts",
					"obligations",
					"service levels",
					"interpretations",
					"issues",
					"actions",
					"disputes",
					"invoices",
					"work order requests",
					"governance body",
					"purchase orders",
					"service data",
					"child obligations",
					"child service levels",
					"governance body meetings",
					"invoice line item",
					"consumptions",
					"clauses",
					"definition",
					"contract templates",
					"contract template structure",
					"change requests"
			};

			List<String> allEntitiesToTestList = new ArrayList<>();
			allEntitiesToTestList.addAll(Arrays.asList(allEntitiesArr));

			logger.info("Starting Test: Verify VH Suffix is displayed for all Supplier Type Users at Entity Listing Filters.");

			ListRendererFilterData filterDataObj = new ListRendererFilterData();

			for (String entityToTest : allEntitiesToTestList) {
				int listId = ConfigureConstantFields.getListIdForEntity(entityToTest);

				logger.info("Hitting Filter Data API for Entity {}", entityToTest);
				filterDataObj.hitListRendererFilterData(listId);
				String filterDataResponse = filterDataObj.getListRendererFilterDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
					List<Map<String, String>> allSupplierTypeUsersInFilter = ListRendererFilterDataHelper.getAllSupplierTypeUsersFromFilterResponse(filterDataResponse);

					if (allSupplierTypeUsersInFilter != null) {
						if (!allSupplierTypeUsersInFilter.isEmpty()) {
							for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInFilter) {
								Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
								Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);

								String filterUserName = supplierTypeUserMap.get("name");

								logger.info("Validating VH Suffix for User [{}] in Filter Data API Response for Entity {}", filterUserName, entityToTest);
								String vhName = expectedUserMap.get("Vendor Hierarchy");

								if (!filterUserName.contains("(" + vhName + ")")) {
									csAssert.assertTrue(false, "Filter Data User Name Value: [" + filterUserName +
											"] doesn't contain VH Suffix in Filter Data Response of Entity " + entityToTest);
								}
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from Filter Data API Response for Entity " + entityToTest);
					}
				} else {
					csAssert.assertTrue(false, "Filter Data API Response for Entity " + entityToTest + " is an Invalid JSON.");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix in Listing Filters. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-C46292: Verify VH Suffix for all Supplier Type Users in Reports for all Entities.
	 */
	@Test(groups = { "minor" })
	public void testVHSuffixInReportFilters() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (allSupplierTypeUsersMap == null) {
				throw new SkipException("Couldn't get All Supplier Type Users Map.");
			}

			logger.info("Starting Test: Verify VH Suffix is displayed for all Supplier Type Users at Report Listing Filters.");
			ReportsListHelper reportObj = new ReportsListHelper();
			Map<Integer, List<Map<String, String>>> allEntityWiseReportsMap = reportObj.getAllEntityWiseReportsMap();

			ReportRendererFilterData filterDataObj = new ReportRendererFilterData();

			for (Map.Entry<Integer, List<Map<String, String>>> entityWiseReportsMap : allEntityWiseReportsMap.entrySet()) {
				Integer entityTypeId = entityWiseReportsMap.getKey();
				String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);

				for (Map<String, String> reportMap : entityWiseReportsMap.getValue()) {
					Integer reportId = Integer.parseInt(reportMap.get("id"));
					String isManualReport = reportMap.get("isManualReport");
					String reportName = reportMap.get("name");

					if (isManualReport.equalsIgnoreCase("true")) {
						continue;
					}

					logger.info("Hitting Report Filter Data API for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
					filterDataObj.hitReportRendererFilterData(reportId);
					String filterDataResponse = filterDataObj.getReportRendererFilterDataJsonStr();

					if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
						List<Map<String, String>> allSupplierTypeUsersInFilter = ListRendererFilterDataHelper.getAllSupplierTypeUsersFromFilterResponse(filterDataResponse);

						if (allSupplierTypeUsersInFilter != null) {
							if (!allSupplierTypeUsersInFilter.isEmpty()) {
								for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInFilter) {
									Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
									Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);

									String filterUserName = supplierTypeUserMap.get("name");

									logger.info("Validating VH Suffix for User [{}] in Filter Data API Response of Report [{}] having Id {}", filterUserName, reportName,
											reportId);
									String vhName = expectedUserMap.get("Vendor Hierarchy");
									vhName = vhName.substring(0,vhName.indexOf("(")-1);

									if (!filterUserName.contains("(" + vhName + ")")) {
										csAssert.assertTrue(false, "Filter Data User Name Value: [" + filterUserName +
												"] doesn't contain VH Suffix in Filter Data Response of Report [" + reportName + "] having Id " + reportId);
									}
								}

								break;
							}
						} else {
							csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from Filter Data API Response of Report [" +
									reportName + "] having Id " + reportId);
						}
					} else {
						csAssert.assertTrue(false, "Filter Data API Response of Report [" + reportName + " having Id " + reportId + " is an Invalid JSON.");
					}
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix in Report Filters. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	@Test(groups = { "minor" })
	public void testVHSuffixInSearch() {
		CustomAssert csAssert = new CustomAssert();

		try {
			SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
			String entityTypesResponse = entityTypesObj.hitSearchLayoutEntityTypes();
			List<Map<String, String>> allEntitiesList = SearchLayoutEntityTypes.getMetadataEntityTypes(entityTypesResponse);

			MetadataSearch searchObj = new MetadataSearch();
			MetadataSearchHelper searchHelperObj = new MetadataSearchHelper();

			for (Map<String, String> entityMap : allEntitiesList) {
				String entityName = entityMap.get("name");
				int entityTypeId = Integer.parseInt(entityMap.get("id"));

				logger.info("Hitting Search API for Entity {}", entityName);
				String searchResponse = searchObj.hitMetadataSearch(entityTypeId);

				if (ParseJsonResponse.successfulResponse(searchResponse)) {
					List<Map<String, String>> allSupplierTypeUsersInEditPage = searchHelperObj.getAllSupplierTypeUsersFromMetadataSearchResponse(searchResponse);

					if (allSupplierTypeUsersInEditPage != null) {
						if (!allSupplierTypeUsersInEditPage.isEmpty()) {
							for (Map<String, String> supplierTypeUserMap : allSupplierTypeUsersInEditPage) {
								Integer userId = Integer.parseInt(supplierTypeUserMap.get("id"));
								Map<String, String> expectedUserMap = allSupplierTypeUsersMap.get(userId);

								String editPageUserName = supplierTypeUserMap.get("name");

								logger.info("Validating VH Suffix for User [{}] in Search API Response for Entity {}", editPageUserName, entityName);
								String vhName = expectedUserMap.get("Vendor Hierarchy");
								vhName = vhName.substring(0,vhName.indexOf("(")-1);

								if (!editPageUserName.contains("(" + vhName + ")")) {
									csAssert.assertTrue(false, "Search Page User Name Value: [" + editPageUserName +
											"] doesn't contain VH Suffix of Entity " + entityName);
								}
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't get All Supplier Type Users from Search API Response for Entity " + entityName);
					}
				} else {
					csAssert.assertTrue(false, "Error in Response of Search API of Entity " + entityName);
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating VH Suffix in Search");
		}

		csAssert.assertAll();
	}
}