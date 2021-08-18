package com.sirionlabs.test.contractPriceBook;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ContractPriceBookHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestContractPriceBook {

	private final static Logger logger = LoggerFactory.getLogger(TestContractPriceBook.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static String templateDownloadFilePath = null;
	private static String templateDownloadFileName = null;
	private static String invoicePricingHelperConfigFilePath = null;
	private static String invoicePricingHelperConfigFileName = null;
	private static Integer contractPriceBookTabId = -1;
	private static Integer contractPriceBookPivotalColumnTabId = -1;
	private static boolean applyRandomization = false;
	private static int maxRecordsToValidate = 3;
	private static int maxFiltersToValidate = 3;
	private static int maxFilterOptionsToValidate = 3;
	private static List<String> filtersToIgnoreList;
	private static Integer tabListDataOffset = 0;
	private static Integer tabListDataSize = 20;
	private static Long schedulerWaitTimeOut = 1200000L;
	private static Long schedulerPollingTime = 10000L;
	private static Boolean killAllTasks = true;
	private static Integer serviceDataEntityTypeId = -1;
	private static Integer contractEntityTypeId;
	private static List<Integer> allServiceDataIdsList = new ArrayList<>();
	private static String filterDataResponse = null;

	@BeforeClass
	public void beforeClass() throws Exception {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractPriceBookTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractPriceBookTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		templateDownloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingFileDownloadPath");
		templateDownloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingFileDownloadName");

		invoicePricingHelperConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFilePath");
		invoicePricingHelperConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingHelperConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "contracts price book");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			contractPriceBookTabId = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping",
				"contracts price book pivotal");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			contractPriceBookPivotalColumnTabId = Integer.parseInt(temp);

		serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
		contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerWaitTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerPollingTime = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killAllTasks");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			killAllTasks = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyRandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			tabListDataOffset = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			tabListDataSize = Integer.parseInt(temp);

		if (applyRandomization) {
			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordsToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxRecordsToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfFiltersToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFiltersToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfFilterOptionsToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFilterOptionsToValidate = Integer.parseInt(temp);
		}

		filtersToIgnoreList = new ArrayList<>();
		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore") != null &&
				!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore").trim().equalsIgnoreCase("")) {
			String ignoreFiltersStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
					"filtersToIgnore").trim().split(Pattern.quote(","));

			for (String filter : ignoreFiltersStr) {
				filtersToIgnoreList.add(filter.trim());
			}
		}
	}

	@DataProvider(name = "dataProviderForContractPriceBookGroupByAndVersion", parallel = true)
	public Object[][] dataProviderForContractPriceBookGroupByAndVersion() throws Exception {
		logger.info("Setting all Contract Price Book Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> allFlowsToTest = new ArrayList<>();

		if (contractPriceBookTabId != -1) {
			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					allFlowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Contract Price Book Config File.", flow.trim());
				}
			}

			for (String flowToTest : allFlowsToTest) {
				Map<Integer, List<Map<String, String>>> allServiceDataPricingMap = new HashMap<>();
				Boolean preRequisitePass = true;

				try {
					setAllPreRequisiteForTests(flowToTest, allServiceDataPricingMap);
				} catch (Exception e) {
					logger.error("Pre-Requisite failed for Flow [{}]. [{}]", flowToTest, e.getMessage());
					preRequisitePass = false;
				}

				int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "contractId"));

				logger.info("Hitting Contract Price Book Filter");
				ListRendererFilterData filterObj = new ListRendererFilterData();
				String payloadForFilterData = "{\"entityId\":" + contractId + "}";
				filterObj.hitListRendererFilterData(contractPriceBookTabId, payloadForFilterData);
				String filterResponse = filterObj.getListRendererFilterDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(filterResponse)) {
					logger.info("Setting all Versions for Contract Id {}", contractId);
					List<Map<String, String>> allVersionOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterResponse, "priceVersion");

					logger.info("Setting all Group By Options for Contract Id {}", contractId);
					List<Map<String, String>> allGroupByOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterResponse, "serviceDataPricingBookPivot");

					String[] allSubTabTypes = {"base charges summary", "baseline summary", "rates"};

					if (allVersionOptions.size() > 0) {
						if (allGroupByOptions.size() > 0) {
							for (Map<String, String> versionOption : allVersionOptions) {
								Map<Integer, String> serviceDataPricingMap = getServiceDataPricingMapForVersion(allServiceDataPricingMap, versionOption.get("id"));

								for (Map<String, String> groupByOption : allGroupByOptions) {
									for (String subTabType : allSubTabTypes) {
										allTestData.add(new Object[]{flowToTest, versionOption.get("name"), Integer.parseInt(versionOption.get("id")),
												groupByOption.get("name"), Integer.parseInt(groupByOption.get("id")), contractId, serviceDataPricingMap,
												subTabType, preRequisitePass});
									}
								}
							}
						} else {
							logger.error("Couldn't get Group By Options for Contract Id {}", contractId);
						}
					} else {
						logger.error("Couldn't get Version Options for Contract Id {}", contractId);
					}
				}
			}
		} else {
			logger.error("Couldn't get Contract Price Book Tab Id. Hence skipping all tests.");
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForContractPriceBookGroupByAndVersion")
	public void testContractPriceBookGroupByAndVersion(String flowToTest, String versionName, Integer versionId, String groupByCategoryName, Integer groupByCategoryId,
	                                                   int contractId, Map<Integer, String> serviceDataPricingMap, String subTabType, Boolean preRequisitePass) {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!preRequisitePass) {
				throw new SkipException("Pre-Requisite State failed for Flow " + flowToTest + ". Hence skipping test.");
			}

			logger.info("Validating Contract Price Book Data for Flow [{}] having Contract Id {}, Service Data Version [{}], Group By Category [{}] and SubTabType [{}]",
					flowToTest, contractId, versionName, groupByCategoryName, subTabType);
			Set<Integer> allUniqueParentIds = new HashSet<>();
			Map<Integer, Set<Integer>> serviceDataIdParentIdMap = new HashMap<>();
			List<Integer> serviceDataIdsList = new ArrayList<>(serviceDataPricingMap.keySet());

			for (Integer serviceDataId : serviceDataIdsList) {
				logger.info("Getting all Parent Ids of Group By Category {} in Service Data Id {}", groupByCategoryName, serviceDataId);
				Set<Integer> allParentIdsOfServiceData = ContractPriceBookHelper.getAllParentIdsOfGroupByCategoryForServiceData(groupByCategoryName, serviceDataId);

				if (allParentIdsOfServiceData.size() > 0) {
					serviceDataIdParentIdMap.put(serviceDataId, allParentIdsOfServiceData);
					allUniqueParentIds.addAll(allParentIdsOfServiceData);
				} else {
					throw new SkipException("Couldn't get Id of Group By Category " + groupByCategoryName + " in Service Data Id " + serviceDataId + ". Hence skipping test.");
				}
			}

			logger.info("Hitting TabListData API for Contract Id {} and Pivotal Column Tab.", contractId);
			String payload = getPayloadForTabListData(groupByCategoryName, groupByCategoryId, subTabType, serviceDataIdsList, versionName, versionId);
			TabListData tabListObj = new TabListData();
			String tabListResponse = tabListObj.hitTabListData(contractPriceBookPivotalColumnTabId, contractEntityTypeId, contractId, payload);

			if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
				//Validate No of Group By Results in Pivotal Column Tab API.
				JSONObject jsonObj = new JSONObject(tabListResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");
				int noOfResultsInPivotalColumnTabResponse = jsonArr.length();

				if (allUniqueParentIds.size() != noOfResultsInPivotalColumnTabResponse) {
					csAssert.assertTrue(false, "Expected No of Records in Pivotal Column Tab Response: " + allUniqueParentIds.size() +
							" and Actual No of Records: " + noOfResultsInPivotalColumnTabResponse);
				}

				//Validate that every Parent Id is present in Pivotal Column Tab Response.
				List<Integer> allParentIdsNotFoundInPivotalColumnResponse = new ArrayList<>();
				allParentIdsNotFoundInPivotalColumnResponse.addAll(allUniqueParentIds);
				String pivotalColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "pivotalcolumn");

				if (pivotalColumnId != null) {
					for (int i = 0; i < jsonArr.length(); i++) {
						jsonObj = jsonArr.getJSONObject(i);
						Integer parentId = Integer.parseInt(jsonObj.getJSONObject(pivotalColumnId).getString("value"));

						if (allParentIdsNotFoundInPivotalColumnResponse.contains(parentId)) {
							logger.info("Parent Id {} found in Pivotal Column Tab Response for Group By Category {} and Contract Id {}", parentId,
									groupByCategoryName, contractId);
							allParentIdsNotFoundInPivotalColumnResponse.remove(parentId);
						}
					}

					if (allParentIdsNotFoundInPivotalColumnResponse.size() > 0) {
						for (Integer parentIdNotFound : allParentIdsNotFoundInPivotalColumnResponse) {
							csAssert.assertTrue(false, "Parent Id " + parentIdNotFound + " not found in Pivotal Column Tab Response for Group By Category "
									+ groupByCategoryName + " and Contract Id " + contractId);
						}
					}

				} else {
					throw new SkipException("Couldn't get Id for Pivotal Column in Pivotal Column Tab API Response for Group By Category " + groupByCategoryName +
							" and Contract Id " + contractId + ". Hence skipping test.");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Group By Category " + groupByCategoryName +
						", Pivotal Column Tab and Contract Id " + contractId + " is an Invalid JSON.");
			}

			logger.info("Hitting TabListData API for Group By Category {}, Contract Id {} and Price Book Tab.", groupByCategoryName, contractId);
			tabListResponse = tabListObj.hitTabListData(contractPriceBookTabId, contractEntityTypeId, contractId, payload);

			//Validate the Parent Id of every Service Data in Price Book Tab API Response.
			if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
				JSONObject jsonObj = new JSONObject(tabListResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");
				String parentColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "parentId");

				if (parentColumnId != null) {
					for (int i = 0; i < jsonArr.length(); i++) {
						Integer serviceDataIdInPriceBook = ContractPriceBookHelper.getServiceDataIdFromSeqNoInContractPriceBookTab(tabListResponse, i);

						if (serviceDataIdInPriceBook != -1) {
							String parentValues = jsonArr.getJSONObject(i).getJSONObject(parentColumnId).getString("value").trim();
							Set<Integer> allExpectedParentIds = new HashSet<>();
							allExpectedParentIds.addAll(serviceDataIdParentIdMap.get(serviceDataIdInPriceBook));

							for (Integer expectedId : serviceDataIdParentIdMap.get(serviceDataIdInPriceBook)) {
								if (parentValues.contains(expectedId.toString())) {
									logger.info("Parent Id {} found in Price Book Tab Response for Group By Category {} and Service Data Id {}", expectedId,
											groupByCategoryName, serviceDataIdInPriceBook);
									allExpectedParentIds.remove(expectedId);
								}
							}

							if (allExpectedParentIds.size() > 0) {
								for (Integer id : allExpectedParentIds) {
									csAssert.assertTrue(false, "Parent Id " + id + " not found in Price Book Tab Response for Group By Category " +
											groupByCategoryName + " and Service Data Id " + serviceDataIdInPriceBook);
								}
							}
						} else {
							throw new SkipException("Couldn't get Service Data in Price Book Response for Group By Category " + groupByCategoryName + " and Contract Id " +
									contractId + ". Hence skipping test.");
						}
					}
				} else {
					throw new SkipException("Couldn't get Id of Parent Id Column for Group By Category " + groupByCategoryName + " and Contract Id " + contractId +
							". Hence skipping test.");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Group By Category " + groupByCategoryName +
						", Price Book Tab and Contract Id " + contractId + " is an Invalid JSON.");
			}

			//Verify Contract Price Book Data Fields for every Service Data.
			for (Integer serviceDataId : serviceDataIdsList) {
				String pricingSectionName = serviceDataPricingMap.get(serviceDataId);
				verifyContractPriceBookFields(tabListResponse, serviceDataId, pricingSectionName, contractId, "Group By Category " + groupByCategoryName,
						csAssert, subTabType);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Contract Price Book Group By for Field {}, Contract Id {}. {}", groupByCategoryName, contractId, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Contract Price Book Group By for Field " + groupByCategoryName + ", Contract Id " +
					contractId + ". " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider(name = "dataProviderForContractPriceBookFilters", parallel = true)
	public Object[][] dataProviderForContractPriceBookFilters() throws Exception {
		List<Object[]> filtersToTest = new ArrayList<>();

		Integer contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filters validation",
				"contractId"));

		logger.info("Setting All Filters to Test");
		logger.info("Hitting Contract Price Book Filter Data Api.");
		ListRendererFilterData filterObj = new ListRendererFilterData();
		String payloadForFilterData = "{\"entityId\":" + contractId + "}";
		filterObj.hitListRendererFilterData(contractPriceBookTabId, payloadForFilterData);
		String filterResponse = filterObj.getListRendererFilterDataJsonStr();

		if (ParseJsonResponse.validJsonResponse(filterResponse)) {
			filterDataResponse = filterResponse;
			List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterResponse);
			if (allFilterNames.size() > 0) {
				logger.info("Total Filters found: {}", allFilterNames.size());
				logger.info("Removing Filters (if any) that are to be Ignored.");

				List<String> eligibleFilters = new ArrayList<>();
				eligibleFilters.addAll(allFilterNames);

				for (String filterToIgnore : filtersToIgnoreList) {
					if (allFilterNames.contains(filterToIgnore))
						eligibleFilters.remove(filterToIgnore);
				}

				if (eligibleFilters.size() > 0) {
					List<String> allFiltersToValidate = new ArrayList<>();
					if (applyRandomization) {
						logger.info("Maximum No of Filters to Validate: {}", maxFiltersToValidate);
						int[] randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, eligibleFilters.size() - 1, maxFiltersToValidate);
						for (int randomNumber : randomNumbersForFilters) {
							allFiltersToValidate.add(eligibleFilters.get(randomNumber));
						}
					} else {
						allFiltersToValidate.addAll(eligibleFilters);
					}

					for (String filter : allFiltersToValidate) {
						filtersToTest.add(new Object[]{filter, contractId});
					}
				} else {
					logger.error("No Eligible Filter found for Contract Price Book.");
				}
			} else {
				logger.error("No Filter found for Contract Price Book.");
			}
		}
		return filtersToTest.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForContractPriceBookFilters", priority = 1)
	public void testContractPriceBookFilters(String filterName, Integer contractId) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("************************************************");
			logger.info("Verifying Filter: {}", filterName);
			logger.info("Getting Id for Filter: {}", filterName);
			Integer filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);

			if (filterId != -1) {
				logger.info("Getting Show Page Object Name Mapping of Filter {}", filterName);
				String showPageObjectName = getShowPageObjectNameMapping(filterName);

				if (showPageObjectName != null) {
					logger.info("Getting UI Type of Filter {}", filterName);
					String uiType = ListRendererFilterDataHelper.getFilterUiType(filterDataResponse, filterName);

					if (uiType == null || uiType.trim().equalsIgnoreCase("MultiSelect") || uiType.trim().equalsIgnoreCase("Select")) {
						validateFiltersOfSelectType(filterName, filterId, showPageObjectName, contractId, csAssert);
					} else {
						throw new SkipException("Filter of Type " + uiType + " is not supported. Hence skipping test.");
					}
				} else {
					throw new SkipException("Couldn't get Show Page Object Name of Filter " + filterName + ". Hence skipping test.");
				}
			} else {
				throw new SkipException("Couldn't get Id for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Contract Price Book Filters. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Contract Price Book Filters. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(priority = 2)
	public void deleteServiceData() throws ConfigurationException {
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteServiceData");

		if (temp != null && temp.trim().equalsIgnoreCase("true") && allServiceDataIdsList.size() > 0) {
			for (Integer serviceDataId : allServiceDataIdsList)
				EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId);
		}
	}

	private Map<Integer, String> getServiceDataPricingMapForVersion(Map<Integer, List<Map<String, String>>> allServiceDataPricingMap, String ccrId) {
		Map<Integer, String> serviceDataPricingMap = new HashMap<>();

		try {
			for (Map.Entry<Integer, List<Map<String, String>>> oneServiceDataPricingMap : allServiceDataPricingMap.entrySet()) {
				Integer serviceDataId = oneServiceDataPricingMap.getKey();

				for (int i = 0; i < oneServiceDataPricingMap.getValue().size(); i++) {
					if (oneServiceDataPricingMap.getValue().get(i).get("ccrId").equalsIgnoreCase(ccrId)) {
						String pricingSectionName = oneServiceDataPricingMap.getValue().get(i).get("pricingSection");
						serviceDataPricingMap.put(serviceDataId, pricingSectionName);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Service Data Pricing Map for CCR Id {}. {}", ccrId, e.getStackTrace());
		}
		return serviceDataPricingMap;
	}

	private void verifyContractPriceBookFields(String tabListResponse, int serviceDataId, String pricingSectionName, int contractId, String flowToTest,
	                                           CustomAssert csAssert, String subTabType) {
		try {
			if (ParseJsonResponse.validJsonResponse(tabListResponse)) {

				int serviceDataSeqNo = TabListDataHelper.getServiceDataSeqNo(tabListResponse, serviceDataId);
				if (serviceDataSeqNo != -1) {
					logger.info("Hitting Show API for Service Data Id {} and Flow [{}].", serviceDataId, flowToTest);
					Show showObj = new Show();
					showObj.hitShow(serviceDataEntityTypeId, serviceDataId);
					String serviceDataShowResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(serviceDataShowResponse)) {
						//Validate Service Data
						if (!validateContractPriceBookServiceData(flowToTest, serviceDataId, serviceDataShowResponse, tabListResponse, serviceDataSeqNo)) {
							logger.error("Contract Price Book validation for Contract Id {}, Service Data Id {} and Flow [{}] failed for Field Service Data.",
									contractId, serviceDataId, flowToTest);
							csAssert.assertTrue(false, "Contract Price Book validation for Contract Id " + contractId + ", Service Data Id " +
									serviceDataId + " and Flow [" + flowToTest + "] failed for Field Service Data.");
						}

						//Validate Unit Type
						if (!validateContractPriceBookUnitType(flowToTest, serviceDataShowResponse, tabListResponse, serviceDataSeqNo)) {
							logger.error("Contract Price Book validation for Contract Id {}, Service Data Id {} and Flow [{}] failed for Field Unit Type.",
									contractId, serviceDataId, flowToTest);
							csAssert.assertTrue(false, "Contract Price Book validation for Contract Id " + contractId + ", Service Data Id " +
									serviceDataId + " and Flow [" + flowToTest + "] failed for Field Unit Type.");
						}

						//Validate Pricing Data
						List<Map<String, String>> pricingDataExpectedValues = getContractPriceBookPricingDataMapList(flowToTest, pricingSectionName, subTabType);
						if (pricingDataExpectedValues.size() > 0) {
							if (!validateContractPriceBookPricingData(tabListResponse, pricingDataExpectedValues, serviceDataSeqNo)) {
								logger.error("Contract Price Book validation for Contract Id {}, Service Data Id {} and Flow [{}] failed for Field Pricing Data.",
										contractId, serviceDataId, flowToTest);
								csAssert.assertTrue(false, "Contract Price Book validation for Contract Id " + contractId + ", Service Data Id " +
										serviceDataId + " and Flow [" + flowToTest + "] failed for Field Pricing Data.");
							}
						} else {
							throw new SkipException("Couldn't get Pricing Data Expected Values Map List for Contract Id " + contractId + ", Service Data Id " +
									serviceDataId + " and Flow [" + flowToTest + "]. Hence skipping test.");
						}
					} else {
						logger.error("Show API Response for Service Data Id {} and Flow [{}] is an Invalid JSON.", serviceDataId, flowToTest);
						csAssert.assertTrue(false, "Show API Response for Service Data Id " + serviceDataId + " and Flow [" + flowToTest +
								"] is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get Seq No for Service Data Id " + serviceDataId + ", Contract Id " + contractId + " and Flow [" + flowToTest +
							"]. Hence skipping test.");
				}
			} else {
				logger.error("TabListData API Response for Price Book of Contract Id {} and Flow [{}] is an Invalid JSON.", contractId, flowToTest);
				csAssert.assertTrue(false, "TabListData API Response for Price Book of Contract Id " + contractId + " and Flow [" + flowToTest +
						"] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Contract Price Book Fields Data for Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Contract Price Book Sub-Tabs for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}

	private void setAllPreRequisiteForTests(String flowToTest, Map<Integer, List<Map<String, String>>> allServiceDataPricingMap) {
		try {
			List<String> preRequisiteSectionsList = getAllPreRequisiteSectionsList(flowToTest);

			if (preRequisiteSectionsList.size() > 0) {
				for (String preRequisiteSection : preRequisiteSectionsList) {
					preRequisiteSection = preRequisiteSection.trim();
					Map<String, String> allPropertiesOfPreRequisiteSection = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, preRequisiteSection);

					//Create Service Data
					String creationSectionName = allPropertiesOfPreRequisiteSection.get("creationsection").trim();
					logger.info("Creating Service Data using Create Section [{}] for Flow [{}].", creationSectionName, flowToTest);

					String jsonStr = ServiceData.createServiceData(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName,
							creationSectionName, true);

					if (ParseJsonResponse.validJsonResponse(jsonStr)) {
						String responseStatus = ParseJsonResponse.getStatusFromResponse(jsonStr);

						if (responseStatus.trim().equalsIgnoreCase("success")) {
							Integer serviceDataId = CreateEntity.getNewEntityId(jsonStr);

							if (serviceDataId != -1) {
								logger.info("Service Data created successfully using Create Section [{}] for Flow [{}] with Id {}", creationSectionName,
										flowToTest, serviceDataId);

								allServiceDataIdsList.add(serviceDataId);
								String serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, creationSectionName,
										"serviceDataType").trim();

								List<Map<String, String>> serviceDataMapList = new ArrayList<>();

								if (allPropertiesOfPreRequisiteSection.get("defaultpricingenabled").trim().equalsIgnoreCase("true")) {
									String defaultPricingSectionName = allPropertiesOfPreRequisiteSection.get("defaultpricingsection").trim();
									Map<String, String> serviceDataMap = new HashMap<>();
									serviceDataMap.put("pricingSection", defaultPricingSectionName);
									serviceDataMap.put("ccrId", "1");
									serviceDataMapList.add(serviceDataMap);

									//Upload Default/Original Pricing Sheet.
									uploadPricingSheet(serviceDataId, serviceDataType, defaultPricingSectionName);
								}

								if (allPropertiesOfPreRequisiteSection.get("versionpricingenabled").trim().equalsIgnoreCase("true")) {
									List<String> allVersionPricingSectionsList = getAllVersionPricingSectionsList(preRequisiteSection);

									for (String versionPricingSectionName : allVersionPricingSectionsList) {
										String newPricingSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, versionPricingSectionName,
												"pricing section");
										String ccrSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, versionPricingSectionName,
												"ccr section");
										String ccrId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, ccrSectionName, "ccrId");
										String ccrValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, ccrSectionName, "ccrValue");

										Map<String, String> serviceDataMap = new HashMap<>();
										serviceDataMap.put("pricingSection", newPricingSectionName);
										serviceDataMap.put("ccrId", ccrId);
										serviceDataMapList.add(serviceDataMap);

										//Upload Pricing Sheet for CCR Version.
										uploadPricingSheet(serviceDataId, serviceDataType, newPricingSectionName, true, ccrValue);
									}
								}

								allServiceDataPricingMap.put(serviceDataId, serviceDataMapList);
							} else {
								throw new SkipException("Couldn't get Id of Newly Created Service Data using Section " + creationSectionName);
							}
						} else {
							throw new SkipException("Couldn't create Service Data using Create Section [" + creationSectionName + "] due to " + responseStatus);
						}
					} else {
						throw new SkipException("Service Data Creation API Response is an Invalid JSON for Service Data using Create Section " + creationSectionName);
					}
				}
			} else {
				throw new SkipException("Couldn't get Pre-Requisite Sections List for Flow [" + flowToTest + "]. Pre-Requisite Stage failed.");
			}
			logger.info("***************************************************");
		} catch (Exception e) {
			throw new SkipException(e.getMessage());
		}
	}

	private void uploadPricingSheet(Integer serviceDataId, String serviceDataType, String pricingSectionName) {
		uploadPricingSheet(serviceDataId, serviceDataType, pricingSectionName, false, null);
	}

	private void uploadPricingSheet(Integer serviceDataId, String serviceDataType, String pricingSectionName, Boolean editDataSheet, String ccrValue) {
		try {
			InvoicePricingHelper pricingObj = new InvoicePricingHelper();

			//Download Pricing Template
			Boolean downloadPricingTemplate = pricingObj.downloadPricingTemplate(templateDownloadFilePath, templateDownloadFileName, serviceDataId);

			if (downloadPricingTemplate) {
				Map<Integer, Map<Integer, Object>> valuesMap = getValuesMapForPricingSheet(pricingSectionName);

				//Edit Pricing Template
				Boolean editTemplate = pricingObj.editPricingTemplateMultipleRows(templateDownloadFilePath, templateDownloadFileName,
						"Pricing", valuesMap);

				if (editTemplate) {
					if (serviceDataType.trim().equalsIgnoreCase("arc") || serviceDataType.trim().equalsIgnoreCase("rrc")) {
						Map<Integer, Object> arcValuesMap = getValuesMapForArcRrcSheet(pricingSectionName);
						editTemplate = pricingObj.editPricingTemplate(templateDownloadFilePath, templateDownloadFileName, "arc rrc sheet",
								arcValuesMap);
					}

					if (editTemplate) {
						if (editDataSheet) {
							Map<Integer, Object> dataValuesMap = getValuesMapForDataSheet(ccrValue);
							editTemplate = pricingObj.editPricingTemplate(templateDownloadFilePath, templateDownloadFileName, "data sheet",
									dataValuesMap);
						}

						if (editTemplate) {
							logger.info("Hitting Fetch API");
							Fetch fetchObj = new Fetch();
							fetchObj.hitFetch();
							List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

							if (killAllTasks) {
								logger.info("KillAllTasks flag is Turned On. Killing All Tasks.");
								UserTasksHelper.removeAllTasks();
							}

							//Upload Pricing Template
							String uploadResponse = pricingObj.uploadPricing(templateDownloadFilePath, templateDownloadFileName);

							if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
								logger.info("Hitting Fetch API to get Pricing Task Id");
								fetchObj.hitFetch();

								int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

								//Wait For Scheduler to finish Pricing Consumption Task
								Map<String, String> pricingJob = UserTasksHelper.waitForScheduler(schedulerWaitTimeOut, schedulerPollingTime, newTaskId);

								if (pricingJob.get("jobPassed").trim().equalsIgnoreCase("true")) {
									logger.info("Pricing Job Passed for Service Data Id {} using Pricing Section [{}].", serviceDataId, pricingSectionName);
								} else {
									throw new SkipException(pricingJob.get("errorMessage"));
								}
							} else {
								throw new SkipException("Pricing Template Failed due to " + uploadResponse + " for Service Data Id " + serviceDataId);
							}
						} else {
							throw new SkipException("Couldn't edit Data Sheet for Service Data Id " + serviceDataId);
						}
					} else {
						throw new SkipException("Couldn't edit ARC RRC Sheet for Service Data Id " + serviceDataId);
					}
				} else {
					throw new SkipException("Couldn't edit Pricing Template Sheet for Service Data Id " + serviceDataId);
				}
			} else {
				throw new SkipException("Couldn't download Pricing Template File at Location: [" + templateDownloadFilePath + "/" + templateDownloadFileName +
						"] for Service Data Id " + serviceDataId);
			}
		} catch (Exception e) {
			throw new SkipException(e.getMessage());
		}
	}

	private String getPayloadForTabListData(String groupByCategoryName, Integer groupByCategoryId, String subTabType, List<Integer> serviceDataIdsList, String versionName,
	                                        int versionId) {
		String payload = null;

		try {
			payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":" +
					"{" + getResourceUnitPayload(serviceDataIdsList) + "," + getServiceDataPricingBookPivotPayload(groupByCategoryName, groupByCategoryId) + "," +
					getPricingDataViewTypePayload(subTabType) + "," + getPriceVersionPayload(versionName, versionId) + "}," +
					"\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"}}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Group By Field {}. {}", groupByCategoryName, e.getStackTrace());
		}
		return payload;
	}

	private String getResourceUnitPayload(List<Integer> serviceDataIdsList) {
		String selectedIdsSubPayload = "";
		for (Integer serviceDataId : serviceDataIdsList) {
			selectedIdsSubPayload += "{\"id\":\"" + serviceDataId + "\"},";
		}

		selectedIdsSubPayload = selectedIdsSubPayload.substring(0, selectedIdsSubPayload.length() - 1);
		return "\"179\":{\"filterId\":179,\"filterName\":\"resourceUnit\",\"multiselectValues\":{\"SELECTEDDATA\":[" + selectedIdsSubPayload + "]}}";
	}

	private String getServiceDataPricingBookPivotPayload(String groupByCategoryName, Integer groupByCategoryId) {
		return "\"211\":{\"filterId\":211,\"filterName\":\"serviceDataPricingBookPivot\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" +
				groupByCategoryId + "\",\"name\":\"" + groupByCategoryName + "\"}]}}";
	}

	private String getPricingDataViewTypePayload(String subTabType) {
		String tabName;
		int tabId;

		if (subTabType.trim().equalsIgnoreCase("base charges summary")) {
			tabName = "Base Charges Summary";
			tabId = 1;
		} else if (subTabType.trim().equalsIgnoreCase("baseline summary")) {
			tabName = "Baseline Summary";
			tabId = 2;
		} else if (subTabType.trim().equalsIgnoreCase("rates")) {
			tabName = "Rates";
			tabId = 3;
		} else {
			logger.info("Unknown SubTabType: {}. Hence returning default Payload.", subTabType);
			tabName = "Base Charges Summary";
			tabId = 1;
		}

		return "\"212\":{\"filterId\":212,\"filterName\":\"pricingDataViewType\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + tabId +
				"\",\"name\":\"" + tabName + "\"}]}}";
	}

	private String getPriceVersionPayload(String versionName, int versionId) {
		if (versionName == null || versionId == -1)
			return "\"215\":{\"filterId\":215,\"filterName\":\"priceVersion\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Original\"}]}}";

		return "\"215\":{\"filterId\":215,\"filterName\":\"priceVersion\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + versionId +
				"\",\"name\":\"" + versionName + "\"}]}}";
	}

	private void validateFiltersOfSelectType(String filterName, Integer filterId, String showPageObjectName, Integer contractId, CustomAssert csAssert) {
		try {
			List<Map<String, String>> allOptions = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);

			if (allOptions.size() > 0) {
				List<Map<String, String>> optionsToTest = getFilterOptionsToTest(allOptions, filterName);

				for (Map<String, String> filterOption : optionsToTest) {
					String optionName = filterOption.get("name");
					String payload = getPayloadForTabListDataFilter(filterName, filterId, filterOption, "select");

					if (filterName.trim().equalsIgnoreCase("resourceUnit"))
						optionName = filterOption.get("id");

					if (payload != null) {
						hitListDataAndVerifyResults(filterName, optionName, payload, showPageObjectName, contractId, csAssert, "select");
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

	private void hitListDataAndVerifyResults(String filterName, String optionName, String payload, String showPageObjectName, Integer contractId,
	                                         CustomAssert csAssert, String filterType) {
		try {
			logger.info("Hitting Tab List Data API for Filter {} and Option {}", filterName, optionName);
			ListRendererTabListData tabListDataObj = new ListRendererTabListData();
			tabListDataObj.hitListRendererTabListData(contractPriceBookTabId, contractEntityTypeId, contractId, payload);
			String tabListDataResponse = tabListDataObj.getTabListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				List<Integer> allServiceDataIds = ContractPriceBookHelper.getAllServiceDataIdsOfContractFromPriceBookTab(tabListDataResponse, contractId);
				logger.info("Total Results found for Filter {} and Option {}: {}", filterName, optionName, allServiceDataIds.size());

				if (allServiceDataIds.size() > 0) {
					List<Integer> listDataToValidate = getTabListDataResultsToValidate(allServiceDataIds, filterName, optionName);
					logger.info("Total Results to Validate for Filter {} and Option {}: {}", filterName, optionName, listDataToValidate.size());

					for (Integer serviceDataId : listDataToValidate) {
						logger.info("Hitting Show API for Record Id {} of Filter {} and Option {}", serviceDataId, filterName, optionName);
						Show showObj = new Show();
						showObj.hitShow(serviceDataEntityTypeId, serviceDataId);

						String showResponse = showObj.getShowJsonStr();
						if (ParseJsonResponse.validJsonResponse(showResponse)) {
							Boolean result = showObj.verifyShowField(showResponse, showPageObjectName, optionName, serviceDataEntityTypeId, filterType);

							if (!result) {
								logger.error("Show Page validation failed for Record Id {} of Filter {} and Option {}", serviceDataId, filterName, optionName);
								csAssert.assertTrue(false, "Show Page validation failed for Record Id " + serviceDataId +
										" of Filter " + filterName + " and Option " + optionName);
							}
						} else {
							logger.error("Show API Response for Record Id {} of Filter {} and Option {} is an Invalid JSON.", serviceDataId, filterName, optionName);
							csAssert.assertTrue(false, "Show API Response for Record Id " + serviceDataId + " of Filter " +
									filterName + " and Option " + optionName + " is an Invalid JSON.");
						}
					}
				}
			} else {
				logger.error("Tab List Data API Response for Filter {} and Option {} is an Invalid JSON.", filterName, optionName);
				csAssert.assertTrue(false, "Tab List Data API Response for Filter " + filterName + " and Option " + optionName + " is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while validating Filter {}. {}", filterName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Filter " + filterName + ". " + e.getMessage());
		}
	}

	private List<Map<String, String>> getFilterOptionsToTest(List<Map<String, String>> allOptions, String filterName) {
		if (!applyRandomization)
			return allOptions;

		List<Map<String, String>> optionsToTest = new ArrayList<>();

		try {
			int randomNumbersArr[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, maxFilterOptionsToValidate);

			for (int randomNumber : randomNumbersArr) {
				optionsToTest.add(allOptions.get(randomNumber));
			}
		} catch (Exception e) {
			logger.error("Exception while getting Options to test for Filter {}. {}", filterName, e.getStackTrace());
		}
		return optionsToTest;
	}

	private String getPayloadForTabListDataFilter(String filterName, Integer filterId, Map<String, String> optionMap, String filterType) {
		String payload = null;

		try {
			logger.info("Creating Payload for Filter {}", filterName);
			String optionId = optionMap.get("id");
			String optionName = optionMap.get("name");

			switch (filterType.trim().toLowerCase()) {
				case "select":
					payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":" + tabListDataOffset + ",\"size\":" + tabListDataSize + "," +
							"\"filterJson\":{\"180\":{\"filterId\":\"" + filterId + "\"," + "\"filterName\":\"" + filterName + "\",\"entityFieldId\":null," +
							"\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + optionId + "\",\"name\":\"" + optionName + "\"}]}}," +
							"\"211\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"filterId\":211,\"filterName\":\"serviceDataPricingBookPivot\"," +
							"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Service Category\"}]}},\"212\":{\"entityFieldHtmlType\":null," +
							"\"entityFieldId\":null,\"filterId\":212,\"filterName\":\"pricingDataViewType\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\"," +
							"\"name\":\"Base Charges Summary\"}]}}},\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"}}";
					break;
			}
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Tab List Data API for Filter {} and Option {}. {}", filterName, optionMap.get("name"), e.getStackTrace());
		}
		return payload;
	}

	private List<Integer> getTabListDataResultsToValidate(List<Integer> allResults, String filterName, String optionName) {
		if (!applyRandomization)
			return allResults;

		List<Integer> listDataResultsToValidate = new ArrayList<>();

		try {
			int randomNumbersArr[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allResults.size() - 1, maxRecordsToValidate);

			for (int randomNumber : randomNumbersArr) {
				listDataResultsToValidate.add(allResults.get(randomNumber));
			}
		} catch (Exception e) {
			logger.error("Exception while getting Results to Validate for Filter {} and Option {}. {}", filterName, optionName, e.getStackTrace());
		}
		return listDataResultsToValidate;
	}

	private Map<Integer, Map<Integer, Object>> getValuesMapForPricingSheet(String pricingSectionName) {
		Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

		try {
			Integer noOfRows = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName, "noOfRows"));
			Integer startingRowNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"pricing sheet", "startingRowNum"));
			Integer rateColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"pricing sheet", "rateColumnNum"));
			Integer volumeColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"pricing sheet", "volumeColumnNum"));

			String[] volumeValuesArr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
					"volumeValue").trim().split(Pattern.quote(","));
			String[] rateValuesArr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
					"rateValue").trim().split(Pattern.quote(","));

			for (int i = 0; i < noOfRows; i++) {
				Map<Integer, Object> innerValuesMap = new HashMap<>();

				innerValuesMap.put(volumeColumnNo, volumeValuesArr[i].trim());
				innerValuesMap.put(rateColumnNo, rateValuesArr[i].trim());
				valuesMap.put(startingRowNo + i, innerValuesMap);
			}
		} catch (Exception e) {
			logger.error("Exception while getting Values Map for Pricing Sheet. {}", e.getMessage());
		}
		return valuesMap;
	}

	private Map<Integer, Object> getValuesMapForArcRrcSheet(String pricingSectionName) {
		Map<Integer, Object> valuesMap = new HashMap<>();

		try {
			Integer lowerLevelColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"arc rrc sheet", "lowerLevelColumnNum"));
			Integer upperLevelColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"arc rrc sheet", "upperLevelColumnNum"));
			Integer rateColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"arc rrc sheet", "rateColumnNum"));
			Integer typeColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"arc rrc sheet", "typeColumnNum"));

			Double lowerLevelValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
					"arcLowerLevelValue"));
			Double upperLevelValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
					"arcUpperLevelValue"));
			Double rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
					"arcRateValue"));
			String typeValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName, "arcTypeValue");

			valuesMap.put(lowerLevelColumnNo, lowerLevelValue);
			valuesMap.put(upperLevelColumnNo, upperLevelValue);
			valuesMap.put(rateColumnNo, rateValue);
			valuesMap.put(typeColumnNo, typeValue);
		} catch (Exception e) {
			logger.error("Exception while getting Values Map for ARC RRC Sheet. {}", e.getMessage());
		}
		return valuesMap;
	}

	private Map<Integer, Object> getValuesMapForDataSheet(String ccrValue) {
		Map<Integer, Object> valuesMap = new HashMap<>();

		try {
			Integer changeRequestColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(invoicePricingHelperConfigFilePath, invoicePricingHelperConfigFileName,
					"data sheet", "changeRequestColumnNum"));

			valuesMap.put(changeRequestColumnNo, ccrValue);
		} catch (Exception e) {
			logger.error("Exception while getting Values Map for Pricing Sheet. {}", e.getMessage());
		}
		return valuesMap;
	}

	private List<String> getAllPreRequisiteSectionsList(String flowToTest) {
		List<String> allPreRequisiteSectionsList = new ArrayList<>();

		try {
			String[] allSectionsStr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"preRequisiteSections").trim().split(Pattern.quote(","));

			allPreRequisiteSectionsList.addAll(Arrays.asList(allSectionsStr));
		} catch (Exception e) {
			logger.error("Exception while getting all Pre-Requisite Sections List for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return allPreRequisiteSectionsList;
	}


	private List<String> getAllVersionPricingSectionsList(String preRequisiteSectionName) {
		List<String> versionPricingSectionsList = new ArrayList<>();

		try {
			String[] versionPricingSectionsStr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, preRequisiteSectionName,
					"versionPricingSections").trim().split(Pattern.quote(","));

			for (String versionPricingSection : versionPricingSectionsStr) {
				versionPricingSectionsList.add(versionPricingSection.trim());
			}
		} catch (Exception e) {
			logger.error("Exception while getting All Version Pricing Sections List for Pre-Requisite Section [{}]. {}", preRequisiteSectionName, e.getStackTrace());
		}
		return versionPricingSectionsList;
	}

	private Boolean validateContractPriceBookServiceData(String flowToTest, int serviceDataId, String serviceDataShowResponse, String tabListDataResponse,
	                                                     int serviceDataSeqNo) {
		Boolean serviceDataMatched = false;

		try {
			String expectedServiceDataName = ShowHelper.getValueOfField("name", serviceDataShowResponse);
			serviceDataMatched = TabListDataHelper.verifyContractPriceBookServiceData(tabListDataResponse, expectedServiceDataName, serviceDataId, serviceDataSeqNo);
		} catch (Exception e) {
			logger.error("Exception while validating Service Data for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return serviceDataMatched;
	}

	private Boolean validateContractPriceBookUnitType(String flowToTest, String serviceDataShowResponse, String tabListDataResponse, int serviceDataSeqNo) {
		Boolean unitTypeMatched = false;

		try {
			String unitValue = ShowHelper.getValueOfField("unit", serviceDataShowResponse);
			unitTypeMatched = TabListDataHelper.verifyContractPriceBookUnitType(tabListDataResponse, unitValue, serviceDataSeqNo);
		} catch (Exception e) {
			logger.error("Exception while validating Unit Type for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return unitTypeMatched;
	}

	private Boolean validateContractPriceBookPricingData(String tabListDataResponse, List<Map<String, String>> expectedValues, int serviceDataSeqNo) {
		return TabListDataHelper.verifyContractPriceBookPricingData(tabListDataResponse, expectedValues, serviceDataSeqNo);
	}

	private List<Map<String, String>> getContractPriceBookPricingDataMapList(String flowToTest, String pricingSectionName, String subTabType) {
		List<Map<String, String>> expectedValues = new ArrayList<>();

		try {
			switch (subTabType.trim().toLowerCase()) {
				default:
				case "base charges summary":
					String expectedBaseChargesVolumesStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
							"volumeValue").trim().split(Pattern.quote(","));
					String expectedBaseChargesRatesStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
							"rateValue").trim().split(Pattern.quote(","));

					for (int i = 0; i < expectedBaseChargesVolumesStr.length; i++) {
						Map<String, String> valuesMap = new HashMap<>();
						Double volume = Double.parseDouble(expectedBaseChargesVolumesStr[i]) * Double.parseDouble(expectedBaseChargesRatesStr[i]);
						valuesMap.put("volume", volume.toString());

						expectedValues.add(valuesMap);
					}
					break;

				case "baseline summary":
					String expectedBaseLineVolumesStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
							"volumeValue").trim().split(Pattern.quote(","));

					for (String baseLineVolume : expectedBaseLineVolumesStr) {
						Map<String, String> valuesMap = new HashMap<>();
						Double expectedVolume = Double.parseDouble(baseLineVolume);
						valuesMap.put("volume", expectedVolume.toString());

						expectedValues.add(valuesMap);
					}
					break;

				case "rates":
					String expectedRatesStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, pricingSectionName,
							"rateValue").trim().split(Pattern.quote(","));

					for (String rate : expectedRatesStr) {
						Map<String, String> valuesMap = new HashMap<>();
						Double expectedRate = Double.parseDouble(rate);
						valuesMap.put("volume", expectedRate.toString());

						expectedValues.add(valuesMap);
					}
					break;
			}
		} catch (Exception e) {
			logger.error("Exception while getting Contract Price Book Pricing Data Map List for Sub-Tab {}, Flow [{}]. {}", subTabType, flowToTest, e.getStackTrace());
		}
		return expectedValues;
	}

	private String getShowPageObjectNameMapping(String filterName) {
		String showPageObjectName = null;

		try {
			showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "filterNameShowPageObjectMapping", filterName);
		} catch (Exception e) {
			logger.error("Exception while getting Show Page Object Name Mapping of Filter {}. {}", filterName, e.getStackTrace());
		}
		return showPageObjectName;
	}
}
