package com.sirionlabs.test.contractTree;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestContractTreeFiltersSIR197141 {

	private final static Logger logger = LoggerFactory.getLogger(TestContractTreeFiltersSIR197141.class);

	private String entityFilterMappingConfigFilePath;
	private String entityFilterMappingConfigFileName;
	private String defaultStartDate = "01-01-2017";
	private String defaultEndDate = "03-03-2020";
	private String defaultMinSliderValue = "5";
	private String defaultMaxSliderValue = "5000";

	private ContractTreeData contractTreeObj = new ContractTreeData();


	@BeforeClass
	public void beforeClass() {
		entityFilterMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityFilterShowPageObjectNameMappingFilePath");
		entityFilterMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityFilterShowPageObjectNameMappingFile");
	}


	/*
	TC-C63010: Verify results after applying filters on Contract Tree.
	 */
	@Test
	public void testFiltersC63010() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C63010: Verify Filters on Contract Tree.");
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

					String contractTreeResponse = contractTreeObj.hitContractTreeListAPIV1(1, supplierRecordId);

					if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
						List<Map<String, String>> allChildrenMap = contractTreeObj.getAllChildrenMapIncludingParent(contractTreeResponse);

						if (allChildrenMap == null) {
							throw new SkipException("Couldn't Get All Children Map from Contract Tree V1 Response.");
						}

						if (allChildrenMap.size() == 0) {
							continue;
						}

						for (Map<String, String> childrenMap : allChildrenMap) {
							int entityTypeId = Integer.parseInt(childrenMap.get("entityTypeId"));
							int entityId = Integer.parseInt(childrenMap.get("entityId"));

							if (entityTypeId != 1 && entityTypeId != 61) {
								continue;
							}

							logger.info("Hitting Filter Data API for Contract Tree of Record Id {} of EntityTypeId {}", entityId, entityTypeId);
							ListRendererFilterData filterDataObj = new ListRendererFilterData();

							Map<String, String> params = new HashMap<>();
							if (entityTypeId == 1) {
								params.put("relationId", String.valueOf(entityId));
								params.put("contractId", "");
							} else {
								params.put("relationId", "");
								params.put("contractId", String.valueOf(entityId));
							}

							filterDataObj.hitListRendererFilterData(434, params);
							String filterDataResponse = filterDataObj.getListRendererFilterDataJsonStr();

							if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
								List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterDataResponse);

								for (String filterName : allFilterNames) {
									logger.info("Validating Filter {} of Contract Tree for Record Id {} of EntityTypeId {}", filterName, entityId, entityTypeId);
									Integer filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);
									String filterType = ListRendererFilterDataHelper.getFilterType(filterDataResponse, filterName, filterId).trim().toLowerCase();

									if (filterId != -1) {
										logger.info("Getting Show Page Object Name Mapping of Filter {} of Contract", filterName);
										String showPageObjectName = getShowPageObjectNameMapping(filterName);

										if (showPageObjectName != null && !showPageObjectName.trim().equalsIgnoreCase("")) {
											switch (filterType) {
												case "select":
												case "singleselect":
												case "multiselect":
													validateFiltersOfSelectType(filterDataResponse, filterName, filterId, showPageObjectName, entityTypeId,
															entityId, csAssert);
													break;

												case "date":
													validateFiltersOfDateType(filterName, filterId, showPageObjectName, entityTypeId, entityId,
															defaultStartDate, defaultEndDate, csAssert);
													break;

												case "slider":
													validateFiltersOfSliderType(filterName, filterId, showPageObjectName, entityTypeId, entityId,
															defaultMinSliderValue, defaultMaxSliderValue, csAssert);
													break;

												case "autocomplete":
													validateFiltersOfAutoCompleteType(filterName, filterId, showPageObjectName, entityTypeId, entityId,
															csAssert);
													break;

												case "checkbox":
													validateFiltersOfCheckBoxType(filterDataResponse, filterName, filterId, showPageObjectName, entityTypeId,
															entityId, csAssert);
													break;

												default:
													throw new SkipException("Filter of Type " + filterType + " is not supported. Hence skipping test.");
											}
										} else {
											logger.warn("Couldn't get Show Page Object Name of Filter {} of Contract. Hence skipping test.", filterName);
											throw new SkipException("Couldn't get Show Page Object Name of Filter " + filterName + " of Contract. Hence skipping test.");
										}
									} else {
										throw new SkipException("Couldn't get Id for Filter " + filterName + " of Contract. Hence skipping test.");
									}
								}
							} else {
								csAssert.assertTrue(false, "Filter Data API Response for Contract Tree of Supplier Id " + supplierRecordId +
										" is an Invalid JSON.");
							}
						}

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
			csAssert.assertTrue(false, "Exception while Validating Test TC-C63010. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private String getShowPageObjectNameMapping(String filterName) {
		String showPageObjectName = null;

		try {
			if (ParseConfigFile.hasPropertyCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
					"contracts filter name show page object mapping", filterName)) {
				showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
						"contracts filter name show page object mapping", filterName);
			} else if (ParseConfigFile.hasPropertyCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
					"default filter name show page object mapping", filterName)) {
				showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(entityFilterMappingConfigFilePath, entityFilterMappingConfigFileName,
						"default filter name show page object mapping", filterName);
			} else {
				logger.info("Show Page Object Mapping not available for Filter {} of Contracts", filterName);
			}
		} catch (Exception e) {
			logger.error("Exception while getting Show Page Object Name Mapping of Filter {}. {}", filterName, e.getStackTrace());
		}
		return showPageObjectName;
	}

	private void validateFiltersOfSelectType(String filterDataResponse, String filterName, Integer filterId, String showPageObjectName, int entityTypeId,
	                                         int recordId, CustomAssert csAssert) {
		try {
			List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

			if (allOptions.size() > 0) {
				for (Map<String, String> filterOption : allOptions) {
					String optionName = filterOption.get("name");

					String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId);

					if (payload != null) {
						hitContractTreeAndVerifyResults(filterName, optionName, payload, showPageObjectName, entityTypeId, recordId, csAssert,
								"select");
					} else {
						throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
					}
				}
			} else {
				throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
		}
	}

	private void validateFiltersOfDateType(String filterName, Integer filterId, String showPageObjectName, int entityTypeId, int recordId, String startDate,
	                                       String endDate, CustomAssert csAssert) {
		try {
			String payload = getPayloadForDateType(filterName, filterId, entityTypeId);

			if (payload != null) {
				hitListDataAndVerifyResultsForDateType(filterName, recordId, startDate, endDate, payload, showPageObjectName, entityTypeId, csAssert);
			} else {
				throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Date Range " + startDate + " - " + endDate + ". Hence skipping test");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Date. " + e.getMessage());
		}
	}

	private void validateFiltersOfSliderType(String filterName, Integer filterId, String showPageObjectName, int entityTypeId, int recordId,
	                                         String minSliderValue, String maxSliderValue, CustomAssert csAssert) {
		try {
			String payload = getPayloadForSliderType(filterName, filterId, entityTypeId);
			String expectedValue = minSliderValue + ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter") + maxSliderValue;

			if (payload != null) {
				hitContractTreeAndVerifyResults(filterName, expectedValue, payload, showPageObjectName, entityTypeId, recordId, csAssert, "slider");
			} else {
				throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Slider Range " + expectedValue + ". Hence skipping test");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Slider. " + e.getMessage());
		}
	}

	private void validateFiltersOfAutoCompleteType(String filterName, Integer filterId, String showPageObjectName, int entityTypeId, int recordId,
	                                               CustomAssert csAssert) {
		try {
			List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfAutoCompleteFilter(filterName, entityTypeId);

			if (allOptions.size() > 0) {
				for (Map<String, String> filterOption : allOptions) {
					String optionName = filterOption.get("name");

					String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId);

					if (payload != null) {
						hitContractTreeAndVerifyResults(filterName, optionName, payload, showPageObjectName, entityTypeId, recordId, csAssert,
								"select");
					} else {
						throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
					}
				}
			} else {
				throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
		}
	}

	private void validateFiltersOfCheckBoxType(String filterDataResponse, String filterName, Integer filterId, String showPageObjectName, int entityTypeId,
	                                           int recordId, CustomAssert csAssert) {
		try {
			List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

			if (allOptions.size() > 0) {
				for (Map<String, String> filterOption : allOptions) {
					String optionName = filterOption.get("name");
					String payload = getPayloadForSelectType(filterName, filterId, filterOption, entityTypeId);

					if (payload != null) {
						optionName = optionName.trim().equalsIgnoreCase("yes") ? "true" : optionName;
						optionName = optionName.trim().equalsIgnoreCase("no") ? "false" : optionName;

						hitContractTreeAndVerifyResults(filterName, optionName, payload, showPageObjectName, entityTypeId, recordId, csAssert,
								"checkbox");
					} else {
						throw new SkipException("Couldn't get Payload for Filter " + filterName + " and Option " + optionName + ". Hence skipping test");
					}
				}
			} else {
				throw new SkipException("No Option found for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + " of Type Select. " + e.getMessage());
		}
	}

	private String getPayloadForSelectType(String filterName, Integer filterId, Map<String, String> optionMap, int entityTypeId) {
		String payload = null;

		try {
			logger.info("Creating Payload for Filter {} and Option {}.", filterName, optionMap.get("name"));

			payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 50 +
					",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
					"\",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionMap.get("id") +
					"\",\"name\":\"" + optionMap.get("name") + "\"}]}}}}}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
		}
		return payload;
	}

	private String getPayloadForDateType(String filterName, Integer filterId, int entityTypeId) {
		String payload = null;

		try {
			logger.info("Creating Payload for Filter {}", filterName);

			payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 50 +
					",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
					"\",\"filterName\":\"" + filterName + "\",\"start\":\"" + defaultStartDate + "\",\"end\":\"" + defaultEndDate +
					"\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Date\"}]}}}}}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
		}
		return payload;
	}

	private String getPayloadForSliderType(String filterName, Integer filterId, int entityTypeId) {
		String payload = null;

		try {
			logger.info("Creating Payload for Filter {}", filterName);

			payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 50 +
					",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{\"" + filterId + "\":{\"filterId\":\"" + filterId +
					"\",\"filterName\":\"" + filterName + "\",\"min\":\"" + defaultMinSliderValue + "\",\"max\":\"" + defaultMaxSliderValue + "\"}}}}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for List Data API for Filter {}. {}", filterName, e.getStackTrace());
		}
		return payload;
	}

	private void hitListDataAndVerifyResultsForDateType(String filterName, int recordId, String startDate, String endDate, String payload, String showPageObjectName,
	                                                    int entityTypeId, CustomAssert csAssert) {
		try {
			String optionName = startDate + " - " + endDate;

			String expectedDateRange = null;
			String expectedDateFormat = null;

			String fieldHierarchy = ShowHelper.getShowFieldHierarchy(showPageObjectName, entityTypeId);
			String lastObjectName = ShowHelper.getLastObjectNameFromHierarchy(fieldHierarchy);

			String contractTreeResponse = contractTreeObj.hitContractTreeListAPIV1(entityTypeId, recordId, payload);

			if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
				List<Map<String, String>> allChildrenMap = contractTreeObj.getAllChildrenMap(contractTreeResponse);

				Show showObj = new Show();

				for (Map<String, String> childrenMap : allChildrenMap) {
					String lightColor = childrenMap.get("lightColor");
					int entityId = Integer.parseInt(childrenMap.get("entityId"));

					if (childrenMap.get("entityTypeId").equalsIgnoreCase("23")) {
						continue;
					}

					if (lightColor.equalsIgnoreCase("true")) {
						continue;
					}

					logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", entityId, filterName, optionName);
					showObj.hitShow(61, entityId);
					String showResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(showResponse)) {
						Boolean result = false;

						JSONObject jsonObj = new JSONObject(showResponse);
						String showStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

						if (showStatus.trim().equalsIgnoreCase("success")) {
							if (expectedDateFormat == null) {
								expectedDateFormat = ShowHelper.getExpectedDateFormat(showResponse, lastObjectName, fieldHierarchy);
							}

							if (expectedDateFormat != null) {

								if (expectedDateRange == null) {
									String fromDate = startDate;
									Date date = new SimpleDateFormat("MM-dd-yyyy").parse(fromDate);
									fromDate = new SimpleDateFormat(expectedDateFormat).format(date);

									String toDate = endDate;
									date = new SimpleDateFormat("MM-dd-yyyy").parse(toDate);
									toDate = new SimpleDateFormat(expectedDateFormat).format(date);

									expectedDateRange = fromDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + toDate;
								}

								result = showObj.verifyShowField(showResponse, showPageObjectName, expectedDateRange, entityTypeId, "date", expectedDateFormat);
							} else {
								logger.error("Couldn't get Expected Date Format for Filter {}, Option {} and Record Id {}", filterName, optionName, entityId);
							}

							if (!result) {
								csAssert.assertTrue(false, "Show Page validation failed for Record Id " + entityId +
										" of Filter " + filterName + " and Option " + optionName);
							}
						} else {
							csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + entityId + " of Filter " + filterName +
									" and Option " + optionName);
						}
					} else {
						csAssert.assertTrue(false, "Show API Response for Record Id " + entityId + " of Filter " +
								filterName + " and Option " + optionName + " is an Invalid JSON.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Contract Tree V1 API Response for Filter Name " + filterName + " and Option " + optionName +
						" for Record Id " + recordId + " of EntityTypeId " + entityTypeId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
		}
	}

	private void hitContractTreeAndVerifyResults(String filterName, String optionName, String payload, String showPageObjectName,
	                                             int entityTypeId, int recordId, CustomAssert csAssert, String filterType) {
		try {
			String contractTreeResponse = contractTreeObj.hitContractTreeListAPIV1(entityTypeId, recordId, payload);

			if (ParseJsonResponse.validJsonResponse(contractTreeResponse)) {
				List<Map<String, String>> allChildrenMap = contractTreeObj.getAllChildrenMap(contractTreeResponse);

				Show showObj = new Show();

				for (Map<String, String> childrenMap : allChildrenMap) {
					String lightColor = childrenMap.get("lightColor");
					int entityId = Integer.parseInt(childrenMap.get("entityId"));

					if (childrenMap.get("entityTypeId").equalsIgnoreCase("23")) {
						continue;
					}

					if (lightColor.equalsIgnoreCase("true")) {
						continue;
					}

					logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", entityId, filterName, optionName);

					showObj.hitShow(61, entityId);
					String showResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(showResponse)) {
						Boolean result;

						if (ShowHelper.isShowPageAccessible(showResponse)) {
							result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, 61, filterType.trim());

							if (!result) {
								//Special code for Regions field.
								if (showPageObjectName.trim().equalsIgnoreCase("globalRegions") ||
										showPageObjectName.trim().equalsIgnoreCase("contractRegions")) {
									showPageObjectName = showPageObjectName.trim().equalsIgnoreCase("globalRegions") ? "contractregions" : "globalregions";

									result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, 61, filterType.trim());
								} else if (showPageObjectName.trim().equalsIgnoreCase("aging")) {
									//Special code for Aging field.
									showPageObjectName = "agingvalue";
									result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, 61, filterType.trim());
								}
							}

							if (!result) {
								logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
								csAssert.assertTrue(false, "Show Page validation failed for Record Id " + recordId +
										" of Filter " + filterName + " and Option " + optionName);
							}
						} else {
							logger.warn("Show Page Not Accessible for Record Id {} of Filter {} and Option {}", recordId, filterName, optionName);
							csAssert.assertTrue(false, "Show Page Not Accessible for Record Id " + recordId + " of Filter " + filterName + " and Option "
									+ optionName);
						}
					} else {
						csAssert.assertTrue(false, "Show API Response for Record Id " + recordId + " of Filter " +
								filterName + " and Option " + optionName + " of Entity Contracts is an Invalid JSON.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Contract Tree V1 API Response for Filter Name " + filterName + " and Option " + optionName +
						" for Record Id " + recordId + " of EntityTypeId " + entityTypeId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
		}
	}
}