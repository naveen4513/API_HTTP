package com.sirionlabs.test.common;

import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.searchLayout.SearchLayoutEntityTypes;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class TestFunctionAndServiceProgressiveFilter extends TestAPIBase {

	private final static Logger logger = LoggerFactory.getLogger(TestFunctionAndServiceProgressiveFilter.class);

	private MetadataSearch metadataSearchObj = new MetadataSearch();
	private List<String> allEntitiesToTestList = new ArrayList<>();
	private Map<Integer, Map<String, String>> allServicesMap;
	private Boolean isServicesFilterOfAutoCompleteType = null;
	private List<String> dbSortedServicesList;
	private List<String> dbSortedFunctionsList;
	private AdminHelper adminObj = new AdminHelper();

	@BeforeClass(groups = { "minor" })
	public void beforeClass() {
		String[] entitiesToTestArr = {
				"contract draft request",
				"contracts",
				"suppliers",
				"disputes",
				"service levels",
				"work order requests",
				"issues",
				"actions",
				"obligations",
				"invoice line item",
				"service data",
				"consumptions",
				"interpretations",
				"vendors",
				"child service levels",
				"child obligations",
				"change requests",
				"governance body",
				"governance body meetings"};

		allEntitiesToTestList.addAll(Arrays.asList(entitiesToTestArr));
		Integer clientId = adminObj.getClientId();
		allServicesMap = adminObj.getAllServicesMap();

		dbSortedFunctionsList = adminObj.getDbSortedFunctionsList(clientId);
		dbSortedServicesList = adminObj.getDbSortedServicesList(clientId);
	}

	/*
	TC-C42420: Check Function & Service Progressive filters consistency across all applicable entities.
	 */
	@Test(groups = { "minor" })
	public void testFunctionAndServiceFiltersInFilterDataAPI() {
		CustomAssert csAssert = new CustomAssert();
		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		try {
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			logger.info("Starting Test TC-C42420: Validating Function & Service Progressive Filters Consistency across all applicable Entities.");

			if (allServicesMap.isEmpty()) {
				throw new SkipException("Pre-Requisite Failed. Couldn't get All Services Map. Hence skipping test.");
			}

			for (String entityToTest : allEntitiesToTestList) {
				logger.info("Hitting Filter Data API for Entity {}", entityToTest);

				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityToTest);
				int listId = ConfigureConstantFields.getListIdForEntity(entityToTest);

				filterDataObj.hitListRendererFilterData(listId);
				String filterDataResponse = filterDataObj.getListRendererFilterDataJsonStr();

				if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
					validateMappingOfAllServiceOptions(filterDataResponse, entityTypeId, csAssert, "Filter Data API of Entity " + entityToTest);
				} else {
					csAssert.assertTrue(false, "Filter Data API Response for Entity " + entityToTest + " is an Invalid JSON.");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Function & Service Filters in Filter Data API. " + e.getMessage());
		}
		csAssert.assertAll();
	}


	/*
	TC-42433: Check Function & Service Progressive Filters in all Reports.
	 */
	@Test(groups = { "minor" })
	public void testFunctionAndServiceFiltersInReportFilterDataAPI() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-42433: Validating Function & Service Progressive Filters in all Reports.");

			if (allServicesMap.isEmpty()) {
				throw new SkipException("Pre-Requisite Failed. Couldn't get All Services Map. Hence skipping test.");
			}

			ReportsListHelper reportHelperObj = new ReportsListHelper();
			Map<Integer, List<Map<String, String>>> entityWiseReportsMap = reportHelperObj.getAllEntityWiseReportsMap();

			ReportRendererFilterData filterDataObj = new ReportRendererFilterData();

			for (String entityToTest : allEntitiesToTestList) {
				int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityToTest);

				if (!entityWiseReportsMap.containsKey(entityTypeId)) {
					continue;
				}

				List<Map<String, String>> allReportsOfEntity = entityWiseReportsMap.get(entityTypeId);

				for (Map<String, String> reportMap : allReportsOfEntity) {
					String isManualReport = reportMap.get("isManualReport");

					//Ignore Manual Reports.
					if (isManualReport.equalsIgnoreCase("true")) {
						continue;
					}

					Integer reportId = Integer.parseInt(reportMap.get("id"));
					String reportName = reportMap.get("name");

					logger.info("Hitting Filter Data API for Report [{}] having Id {} of Entity {}", reportName, reportId, entityToTest);
					filterDataObj.hitReportRendererFilterData(reportId);
					String filterDataResponse = filterDataObj.getReportRendererFilterDataJsonStr();

					if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
						validateMappingOfAllServiceOptions(filterDataResponse, entityTypeId, csAssert,
								"Filter Data API of Report [" + reportName + "] having Id " + reportId + " of Entity " + entityToTest);
					} else {
						csAssert.assertTrue(false, "Filter Data API Response for Report [" + reportName + "] having Id " + reportId +
								" of Entity " + entityToTest + " is an Invalid JSON.");
					}
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Function & Service Filters in Reports. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	/*
	TC-C46399: Verify that Service Field Under Metadata Search depends on the Function field.
	 */
	@Test(groups = { "minor" })
	public void testFunctionsAndServicesFiltersInMetadataSearchAPI() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Starting Test TC-C46399: Validating Functions and Services Progressive Filters in MetadataSearch API");
			logger.info("Setting All Entities to Test in Search");
			SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
			String entityTypesResponse = entityTypesObj.hitSearchLayoutEntityTypes();
			List<Map<String, String>> allEntitiesToTest = SearchLayoutEntityTypes.getMetadataEntityTypes(entityTypesResponse);

			for (Map<String, String> entityTypeMap : allEntitiesToTest) {
				String entityName = entityTypeMap.get("name");
				int entityTypeId = Integer.parseInt(entityTypeMap.get("id"));

				//Ignore Invoices Entity as it doesn't have Functions & Services fields.
				if (entityTypeId == 67) {
					continue;
				}

				logger.info("Hitting MetadataSearch API for Entity {} having EntityTypeId {}", entityName, entityTypeId);
				String metadataSearchResponse = metadataSearchObj.hitMetadataSearch(entityTypeId);

				if (ParseJsonResponse.validJsonResponse(metadataSearchResponse)) {
					JSONObject jsonObj = new JSONObject(metadataSearchResponse);
					jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("services").getJSONObject("options");

					JSONArray jsonArr = jsonObj.getJSONArray("data");

					for (int i = 0; i < jsonArr.length(); i++) {
						jsonObj = jsonArr.getJSONObject(i);

						if (!jsonObj.has("parentName") || jsonObj.isNull("parentName") ||
								jsonObj.getString("parentName").trim().equalsIgnoreCase("")) {
							csAssert.assertTrue(false, "Parent Name missing for Service Option " + jsonObj.getString("name") + " having Id " +
									jsonObj.getInt("id") + " of Entity " + entityName + " having EntityTypeId " + entityTypeId);
						}
					}
				} else {
					csAssert.assertTrue(false, "MetadataSearch API Response for Entity " + entityName + " having EntityTypeId " + entityTypeId +
							" is an Invalid JSON.");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Functions and Services Progressive Filters in MetadataSearch API." + e.getMessage());
		}

		csAssert.assertAll();
	}

	private void validateMappingOfAllServiceOptions(String filterDataResponse, int entityTypeId, CustomAssert csAssert, String additionalInfo) {
		try {
			String filterName = "services";
			List<Map<String, String>> allOptionsOfServicesFilter;

			if (isServicesFilterOfAutoCompleteType == null) {
				isServicesFilterOfAutoCompleteType = ListRendererFilterDataHelper.isFilterOfAutoCompleteType(filterDataResponse, filterName);
			}

			if (isServicesFilterOfAutoCompleteType != null && isServicesFilterOfAutoCompleteType) {
				allOptionsOfServicesFilter = ListRendererFilterDataHelper.getAllOptionsOfAutoCompleteFilter(filterName, entityTypeId);
			} else {
				allOptionsOfServicesFilter = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
			}

			List<String> actualServicesList = new ArrayList<>();

			for (Map<String, String> serviceMap : allOptionsOfServicesFilter) {
				String serviceName = serviceMap.get("name");
				actualServicesList.add(serviceName);
				logger.info("Validating Mapping of Service Option [{}]. {}", serviceName, additionalInfo);

				Integer serviceId = Integer.parseInt(serviceMap.get("id"));

				if (!allServicesMap.containsKey(serviceId)) {
					csAssert.assertTrue(false, "Service [" + serviceName + "] having Id " + serviceId +
							" is not found in All Services Map. " + additionalInfo);
					return;
				}

				Map<String, String> expectedServiceMap = allServicesMap.get(serviceId);

				String expectedGroupName = expectedServiceMap.get("functionName");
				String actualGroupName = serviceMap.get("group");

				if (!expectedGroupName.equalsIgnoreCase(actualGroupName)) {
					csAssert.assertTrue(false, "Expected Function Name: " + expectedGroupName + " and Actual Function Name: " + actualGroupName + ". " +
							additionalInfo);
				}
			}

			if (!actualServicesList.isEmpty()) {
				validateSortingOrder(actualServicesList, dbSortedServicesList, additionalInfo + ", Services Field", csAssert);
			}

			List<Map<String, String>> allOptionsOfFunctionsFilter = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, "functions");
			List<String> actualFunctionsList = new ArrayList<>();

			for (Map<String, String> functionMap : allOptionsOfFunctionsFilter) {
				String functionName = functionMap.get("name");
				actualFunctionsList.add(functionName);
			}

			if (!actualFunctionsList.isEmpty()) {
				validateSortingOrder(actualFunctionsList, dbSortedFunctionsList, additionalInfo + ", Functions Field", csAssert);
			}

		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating mapping of All Service Options. " + additionalInfo + ". " + e.getMessage());
		}
	}

	//This method covers TC-C46414
	private void validateSortingOrder(List<String> actualOrderList, List<String> expectedOrderList, String additionalInfo, CustomAssert csAssert) {
		try {
			if (actualOrderList.size() != expectedOrderList.size()) {
				csAssert.assertTrue(false, "Actual Order List Size: " + actualOrderList.size() + " and Expected Order List Size: " +
						expectedOrderList.size() + ". " + additionalInfo);
				return;
			}

			for (int i = 0; i < actualOrderList.size(); i++) {
				if (!actualOrderList.get(i).equalsIgnoreCase(expectedOrderList.get(i))) {
					csAssert.assertTrue(false, "Sorting Order doesn't match in Actual Order List and Expected Order List. " + additionalInfo);
					break;
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Sorting Order. " + additionalInfo);
		}
	}
}