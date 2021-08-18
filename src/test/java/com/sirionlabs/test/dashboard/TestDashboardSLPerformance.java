package com.sirionlabs.test.dashboard;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.dashboard.DashboardSlaChart;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.SlaSpecificGraph;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.reportRenderer.FilterData;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.CSVResults;
import com.sirionlabs.utils.csvutils.DashboardSLPerformanceResultInfo;
import com.sirionlabs.utils.csvutils.ResultInfoClass;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDashboardSLPerformance {
	private final static Logger logger = LoggerFactory.getLogger(TestDashboardSLPerformance.class);
	static String dashboardSLPerformanceReportFilePath;
	static String dashboardSLPerformanceReportFileName;
	static String dashboardSLPerformanceSortingOrderReportFileName;
	static Integer limit = 20;
	static Integer offset = 0;
	static String orderByColumnName = "alphabatic";
	static String orderDirection = "ASC";
	static Boolean useRandomizationOnFilters = true;
	static Boolean useRandomizationOnAdvancedFilters = true;
	static String dashboardSlaChartConfigFilePath;
	static String dashboardSlaChartConfigFileName;
	static Integer dashboardSLPerformanceListId;
	static Integer maxRandomOptions = 3;
	static Integer maxRandomOptionsOfPrimaryFilters = 1;
	static String[] columnNameValuesForSorting = {"alphabetic"};
	static String[] orderDirectionForSorting = {"ASC"};
	static String delimiterForValues;
	static Boolean validateChartData = false;
	static Integer tabUrlId;
	CSVResults csvResultsObj = null;
	CSVResults csvResultsObjForSortingValidation = null;

	@BeforeClass(alwaysRun = true)
	public void settingStaticFields() {
		logger.debug("In BeforeClass");
		try {
			dashboardSlaChartConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("dashboardSlaChartConfigFilePath");
			dashboardSlaChartConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardSlaChartConfigFileName");

			offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "offset"));
			limit = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "limit"));
			orderByColumnName = ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "orderByColumnName");
			orderDirection = ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "orderDirection");
			dashboardSLPerformanceListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "dashboardSLPerformanceListId"));
			useRandomizationOnFilters = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "applyrandomizationonprimaryfilters"));
			useRandomizationOnAdvancedFilters = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "applyrandomizationonadvancedfilters"));
			maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "maxrandomoptions"));
			tabUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "tabUrlId"));

			delimiterForValues = ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "delimiterForValues");
			orderDirectionForSorting = ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "orderDirectionForSorting").trim().split(Pattern.quote(delimiterForValues));
			columnNameValuesForSorting = ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "orderByColumnNameForSorting").trim().split(Pattern.quote(delimiterForValues));
			if (ParseConfigFile.getValueFromConfigFile(dashboardSlaChartConfigFilePath, dashboardSlaChartConfigFileName, "validateChartData").equalsIgnoreCase("true"))
				validateChartData = true;

			//Setting dashboard- SL Performance CSV report generation for Primary and Advanced filters
			dashboardSLPerformanceReportFilePath = ConfigureConstantFields.getConstantFieldsProperty("dashboardSLPerformanceReportFilePath");
			dashboardSLPerformanceReportFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardSLPerformanceReportFileName");
			csvResultsObj = new CSVResults();
			ResultInfoClass resultInfoClass = new DashboardSLPerformanceResultInfo();
			csvResultsObj.initializeResultCsvFile(dashboardSLPerformanceReportFilePath + "/" + dashboardSLPerformanceReportFileName, resultInfoClass);

			//Setting dashboard- SL Performance CSV report generation For Sorting Validation
			dashboardSLPerformanceSortingOrderReportFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardSLPerformanceSortingOrderReportFileName");
			csvResultsObjForSortingValidation = new CSVResults();
			ResultInfoClass resultInfoClassObj = new DashboardSLPerformanceResultInfo("testSortingOrder");
			csvResultsObjForSortingValidation.initializeResultCsvFile(dashboardSLPerformanceReportFilePath + "/" + dashboardSLPerformanceSortingOrderReportFileName, resultInfoClassObj);
		} catch (Exception e) {
			logger.error("Exception in Before class while setting static fields. {}", e.getMessage());
		}
	}

	@Test(enabled = true, groups = {"smoke","regression"})
	public void testSLPerformancePrimaryFilters() {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardSLPerformanceReportMap = new HashMap<>();
		try {
			//getting metaData response for dashboard SL Performance
			ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
			metaDataObj.hitListRendererDefaultUserListMetadata(dashboardSLPerformanceListId);
			String metaDataResponseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

			//getting filterData response for dashboard global filters
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			String filterDataPayload = getFilterDataPayloadForDashboardSLPerformance();
			filterDataObj.hitListRendererFilterData(dashboardSLPerformanceListId, filterDataPayload);
			String filterDataResponseStr = filterDataObj.getListRendererFilterDataJsonStr();

			//validating metaData and FilterData API response
			Boolean isMetaDataValidJson = APIUtils.validJsonResponse(metaDataResponseStr, "[metaData API response for dashboard- SL Performance]");
			Boolean isFilterDataValidJson = APIUtils.validJsonResponse(filterDataResponseStr, "[FilterData response for dashboard- SL Performance]");

			dashboardSLPerformanceReportMap.put("isMetaDataValidJson", isMetaDataValidJson.toString());
			dashboardSLPerformanceReportMap.put("isFilterDataValidJson", isFilterDataValidJson.toString());
			if (isMetaDataValidJson && isFilterDataValidJson) {
				/*
				* Validating Dashboard- SL Performance
				* */
				validateDashboardSLPerformance(filterDataResponseStr, metaDataResponseStr, csAssert, dashboardSLPerformanceReportMap);
			} else {
				logger.error("metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				csAssert.assertTrue(false, "metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				dashboardSLPerformanceReportMap.put("TestStatus", "Failed");
				pushReportsResultToCSV(dashboardSLPerformanceReportMap);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testSLPerformance {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while validating Dashboard- SLPerformance : " + e.getMessage());
			dashboardSLPerformanceReportMap.put("TestStatus", "Failed[Exception while getting metaData/FilterData api response]");
			pushReportsResultToCSV(dashboardSLPerformanceReportMap);
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, groups = {"regression"})
	public void testSLPerformanceAdvancedFilters() {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardSLPerformanceReportMap = new HashMap<>();
		try {
			//getting metaData response for dashboard SL Performance
			ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
			metaDataObj.hitListRendererDefaultUserListMetadata(dashboardSLPerformanceListId);
			String metaDataResponseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

			//getting filterData response for dashboard global filters
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			String filterDataPayload = getFilterDataPayloadForDashboardSLPerformance();
			filterDataObj.hitListRendererFilterData(dashboardSLPerformanceListId, filterDataPayload);
			String filterDataResponseStr = filterDataObj.getListRendererFilterDataJsonStr();

			//validating metaData and FilterData API response
			Boolean isMetaDataValidJson = APIUtils.validJsonResponse(metaDataResponseStr, "[metaData API response for dashboard- SL Performance]");
			Boolean isFilterDataValidJson = APIUtils.validJsonResponse(filterDataResponseStr, "[FilterData response for dashboard- SL Performance]");
			dashboardSLPerformanceReportMap.put("isMetaDataValidJson", isMetaDataValidJson.toString());
			dashboardSLPerformanceReportMap.put("isFilterDataValidJson", isFilterDataValidJson.toString());

			if (isMetaDataValidJson && isFilterDataValidJson) {
				/*
				* Validating Dashboard- SL Performance
				* */
				validateDashboardSLPerformanceForAdvancedFilters(filterDataResponseStr, metaDataResponseStr, csAssert, dashboardSLPerformanceReportMap);
			} else {
				logger.error("metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				csAssert.assertTrue(false, "metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				dashboardSLPerformanceReportMap.put("TestStatus", "Failed[metaData/FilterData response in not valid json]");
				pushReportsResultToCSV(dashboardSLPerformanceReportMap);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testSLPerformance {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while validating Dashboard- SLPerformance : " + e.getMessage());
			dashboardSLPerformanceReportMap.put("TestStatus", "Failed");
			pushReportsResultToCSV(dashboardSLPerformanceReportMap);
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, groups = {"regression"})
	public void testSLPerformanceSorting() {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardReportMap = new HashMap<>();
		try {
			//getting metaData response for dashboard SL Performance
			ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
			metaDataObj.hitListRendererDefaultUserListMetadata(dashboardSLPerformanceListId);
			String metaDataResponseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

			//getting filterData response for dashboard global filters
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			String filterDataPayload = getFilterDataPayloadForDashboardSLPerformance();
			filterDataObj.hitListRendererFilterData(dashboardSLPerformanceListId, filterDataPayload);
			String filterDataResponseStr = filterDataObj.getListRendererFilterDataJsonStr();

			//validating metaData and FilterData API response
			Boolean isMetaDataValidJson = APIUtils.validJsonResponse(metaDataResponseStr, "[metaData API response for dashboard- SL Performance]");
			Boolean isFilterDataValidJson = APIUtils.validJsonResponse(filterDataResponseStr, "[FilterData response for dashboard- SL Performance]");

			dashboardReportMap.put("isMetaDataValidJson", isMetaDataValidJson.toString());
			dashboardReportMap.put("isFilterDataValidJson", isFilterDataValidJson.toString());

			if (isMetaDataValidJson && isFilterDataValidJson) {
				/*
				* Validating Dashboard- SL Performance for sorting order
				* */
				validateDashboardSLPerformanceChartsSortingOrder(filterDataResponseStr, metaDataResponseStr, csAssert, dashboardReportMap);
			} else {
				logger.error("metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				csAssert.assertTrue(false, "metaData or Filter Data API response for dashboard is not valid json for dashboard- SL Performance.");
				dashboardReportMap.put("TestStatus", "Failed");
				pushChartsSortingReportsResultToCSV(dashboardReportMap);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testSLPerformance Sorting order{}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while validating Dashboard- SLPerformance Sorting Order : " + e.getMessage());
			dashboardReportMap.put("TestStatus", "Failed[Exception in sorting order validation]");
			pushChartsSortingReportsResultToCSV(dashboardReportMap);
		}
		csAssert.assertAll();
	}

	private void validateDataForSLPerformanceCharts(String slaChartJsonStr, CustomAssert csAssert, Map<String, String> dashboardReportMap) {
		logger.info("Validating SL Performance chart data.");
		try {
			JSONObject slaChartResJsonObj = new JSONObject(slaChartJsonStr);
			Integer totalChartsCount = slaChartResJsonObj.getInt("totalCount");
			JSONArray chartJsonArray = slaChartResJsonObj.getJSONArray("chartJson");
			if (totalChartsCount == 0) {
				dashboardReportMap.put("isDataValidationPassed", "True[Empty chart Json Array]");
				dashboardReportMap.put("TestStatus", "Passed");
				pushReportsResultToCSV(dashboardReportMap);
			} else {
				for (int i = 0; i < chartJsonArray.length(); i++) {

					Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
					Integer chartId = chartJsonArray.getJSONObject(i).getInt("chartId");
					String chartName = chartJsonArray.getJSONObject(i).getString("chartName");

					SlaSpecificGraph slaSpecificGraphObj = new SlaSpecificGraph();
					slaSpecificGraphObj.hitSlaSpecificGraph(tabUrlId.toString(), chartId.toString());
					String slaSpecificGraphResponseStr = slaSpecificGraphObj.getSlaSpecificGraphJsonStr();
					Boolean isValidJson = APIUtils.validJsonResponse(slaSpecificGraphResponseStr, "slaSpecificGraph response");
					csAssert.assertTrue(isValidJson, "slaSpecificGraph api response is not valid json for chartName = " + chartName + ", chartId = " + chartId);
					if (isValidJson) {
						JSONArray jsonArray = new JSONArray(slaSpecificGraphResponseStr);
						JSONObject slaChartJsonObj = jsonArray.getJSONObject(0);

						if (chartJsonArray.getJSONObject(i).toString().equalsIgnoreCase(slaChartJsonObj.toString())) {
							logger.info("Data validation passed for slaChart name = {}, id = {}", chartName, chartId);
							dashboardReportMapLocal.put("isDataValidationPassed", "True[ " + chartName + " ]");
							dashboardReportMapLocal.put("TestStatus", "Passed");
							pushReportsResultToCSV(dashboardReportMapLocal);

						} else {
							logger.error("Data validation failed for slaChart name = {}, id = {}", chartName, chartId);
							csAssert.assertTrue(false, "Data validation failed for chartName = " + chartName);
							dashboardReportMapLocal.put("isDataValidationPassed", "False[ " + chartName + " ]");
							dashboardReportMapLocal.put("TestStatus", "Failed");
							pushReportsResultToCSV(dashboardReportMapLocal);
						}
					} else {
						logger.error("slaSpecificGraph api response is not valid json for tab Child Service Level.");
						dashboardReportMapLocal.put("isDataValidationPassed", "False[slaSpecificGraph is not a valid json on show page]");
						dashboardReportMapLocal.put("TestStatus", "Failed");
						pushReportsResultToCSV(dashboardReportMapLocal);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while data validation of SL Performance chart. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception occurred while data validation of SL Performance chart. " + e.getMessage());
			dashboardReportMap.put("isDataValidationPassed", "False[Exception in Data validation]");
			dashboardReportMap.put("TestStatus", "Failed");
			pushReportsResultToCSV(dashboardReportMap);
		}
	}

	private void validateDashboardSLPerformanceChartsSortingOrder(String filterDataResponseStr, String metaDataResponseStr, CustomAssert csAssert, Map<String, String> dashboardReportMap) {
		Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
		FilterUtils filterObj = new FilterUtils();
		Map<Integer, Map<String, String>> filterMap = filterObj.getSpecificFilters(metaDataResponseStr, "primary");
		Map<String, List<String>> primaryFilterNameAndOptionsIdMap = new HashMap<>();

		for (Map.Entry<Integer, Map<String, String>> entryMap : filterMap.entrySet()) {
			List<String> optionIdList = new ArrayList<>();

			Integer filterId = entryMap.getKey();
			String filterName = entryMap.getValue().get("queryName");
			dashboardReportMapLocal.put("PrimaryFilterName", filterName);
			Map<String, String> filterOptionsIdNameMap = getFilterOptionsIdNameMap(filterId, filterName, filterDataResponseStr);
			if (filterOptionsIdNameMap.size() > 0) {

				for (Map.Entry<String, String> entry : filterOptionsIdNameMap.entrySet()) {
					String optionId = entry.getKey();
					optionIdList.add(optionId);
				}
				primaryFilterNameAndOptionsIdMap.put(filterName, optionIdList);

				List<String> columnNameList = Arrays.asList(columnNameValuesForSorting);
				List<String> orderDirectionList = Arrays.asList(orderDirectionForSorting);

				for (int i = 0; i < columnNameList.size(); i++) {
					for (int j = 0; j < orderDirectionList.size(); j++) {

						Map<String, String> dashboardReportMapSortingOrder = new HashMap<>(dashboardReportMapLocal);

						dashboardReportMapSortingOrder.put("sortingCriteria", columnNameList.get(i));
						dashboardReportMapSortingOrder.put("sortingOrder", orderDirectionList.get(j));

						//forming payload with all the options in a primary filter attribute so as to cover maximum possible charts
						String payload = getPayloadForDashboardSLPerformanceForSorting(primaryFilterNameAndOptionsIdMap, columnNameList.get(i), orderDirectionList.get(j));
						dashboardReportMapSortingOrder.put("slaChartPayload", payload);
						logger.debug("Hitting dashboard- SL Performance API with Payload {}", payload);
						DashboardSlaChart slaChartObj = new DashboardSlaChart();
						slaChartObj.hitDashboardSlaChart(payload);
						String slaChartResponseStr = slaChartObj.getDashboardSlaChartJsonStr();
						Boolean isValidJson = APIUtils.validJsonResponse(slaChartResponseStr);
						csAssert.assertTrue(isValidJson, "slaChart api response is not valid json for the payload = " + payload);
						dashboardReportMapSortingOrder.put("isSlaChartValidJsonResponse", isValidJson.toString());
						if (isValidJson) {
							Boolean isResponseInSortedOrder = verifySorting(slaChartResponseStr, columnNameList.get(i), orderDirectionList.get(j));
							csAssert.assertTrue(isResponseInSortedOrder, "slaChart api Response is not in sorted order for the filter #" + filterName + " sortingCriteria #" + columnNameList.get(i) + " sortingOrder #" + orderDirectionList.get(j));
							dashboardReportMapSortingOrder.put("isResponseInSortedOrder", isResponseInSortedOrder.toString());
							if (!isResponseInSortedOrder) {
								logger.error("Sorting Order Validation failed. {} --> {}", columnNameList.get(i), orderDirectionList.get(j));
								dashboardReportMapSortingOrder.put("TestStatus", "Failed");
								pushChartsSortingReportsResultToCSV(dashboardReportMapSortingOrder);
							} else {
								logger.info("Sorting order validation passed for filter : {}", filterName);
								dashboardReportMapSortingOrder.put("TestStatus", "Passed");
								pushChartsSortingReportsResultToCSV(dashboardReportMapSortingOrder);
							}

						} else {
							logger.error("dashboard- SL Performance response is not valid json for the payload = {}.", payload);
							csAssert.assertTrue(false, "dashboard- SL Performance response is not valid json. [validating sorting order]");
							dashboardReportMapSortingOrder.put("TestStatus", "Failed");
							pushChartsSortingReportsResultToCSV(dashboardReportMapSortingOrder);
						}
					}
				}
			} else {
				logger.warn("No options found for the filter {}.", filterName);
				dashboardReportMapLocal.put("TestStatus", "Passed[Empty Filter]");
				pushChartsSortingReportsResultToCSV(dashboardReportMapLocal);
			}
		}
	}

	private Boolean verifySorting(String slaChartResponseStr, String sortingColumnName, String sortingOrder) {
		Boolean isChartsInExpectedOrder = true;

		try {
			JSONObject slaChartJson = new JSONObject(slaChartResponseStr);
			JSONArray chartJsonArray = slaChartJson.getJSONArray("chartJson");
			Map<Integer, String> chartIdNameMap = new LinkedHashMap<>();

			for (int i = 0; i < chartJsonArray.length(); i++) {
				String chartName = chartJsonArray.getJSONObject(i).getString("chartName");
				Integer chartId = chartJsonArray.getJSONObject(i).getInt("chartId");
				chartIdNameMap.put(chartId, chartName);
			}
			if (sortingColumnName.equalsIgnoreCase("priority")) {
				Map<Integer, String> chartIdPriorityMap = getChartsIdPriorityMap(slaChartResponseStr);
				Boolean flag = false;

				for (Map.Entry<Integer, String> entryMap : chartIdNameMap.entrySet()) {
					if (sortingOrder.equalsIgnoreCase("asc")) {
						if (chartIdPriorityMap.get(entryMap.getKey()) == null) {
							logger.warn("priority is not set for chartId = {}[{}]", entryMap.getKey(), entryMap.getValue());
							continue;
						}
						if (chartIdPriorityMap.get(entryMap.getKey()).equalsIgnoreCase("key")) {
							flag = true;
							continue;
						}
						if (flag == true && chartIdPriorityMap.get(entryMap.getKey()).equalsIgnoreCase("critical")) {
							logger.error("Charts are not sorted on priority-asc.[critical to key]");
							isChartsInExpectedOrder = false;
						}
					} else if (sortingOrder.equalsIgnoreCase("desc")) {
						if (chartIdPriorityMap.get(entryMap.getKey()) == null) {
							logger.warn("priority is not set for chartId = {}[{}]", entryMap.getKey(), entryMap.getValue());
							continue;
						}
						if (chartIdPriorityMap.get(entryMap.getKey()).equalsIgnoreCase("critical")) {
							flag = true;
							continue;
						}
						if (flag == true && chartIdPriorityMap.get(entryMap.getKey()).equalsIgnoreCase("key")) {
							logger.error("Charts are not sorted on priority-desc.[key to critical]");
							isChartsInExpectedOrder = false;
						}
					} else
						logger.error("sorting order is neither asc nor desc.");
				}


			} else if (sortingColumnName.equalsIgnoreCase("alphabatic")) {
				List<String> chartNameList = new ArrayList<>(chartIdNameMap.values());
				PostgreSQLJDBC jdbcObj = new PostgreSQLJDBC();

				for (int i = 0; i < chartNameList.size() - 1; i++) {
					if (sortingOrder.equalsIgnoreCase("asc")) {
						isChartsInExpectedOrder = jdbcObj.compareTwoRecordsForAscOrEqual(chartNameList.get(i), chartNameList.get(i + 1));
						if (isChartsInExpectedOrder == false) {
							logger.error("Charts are not in sorted order(ASC). chart {} should come before chart {}", chartNameList.get(i + 1), chartNameList.get(i));
							break;
						}
					} else if (sortingOrder.equalsIgnoreCase("desc")) {
						isChartsInExpectedOrder = jdbcObj.compareTwoRecordsForDescOrEqual(chartNameList.get(i), chartNameList.get(i + 1));
						if (isChartsInExpectedOrder == false) {
							logger.error("Charts are not in sorted order(DESC). chart {} should come before chart {}", chartNameList.get(i + 1), chartNameList.get(i));
							break;
						}
					} else {
						logger.error("sorting order provided is neither asc or desc.");
					}
				}

			} else {
				logger.error("Sorting order provided is neither alphabatic nor priority");
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating SLPerformance Sorting order{}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
		return isChartsInExpectedOrder;
	}

	private Map<Integer, String> getChartsIdPriorityMap(String slaChartResponseStr) {
		Map<Integer, String> chartsIdPriorityMap = new HashMap<>();
		try {
			JSONObject slaChartJson = new JSONObject(slaChartResponseStr);
			JSONArray chartJsonArray = slaChartJson.getJSONArray("chartJson");

			for (int i = 0; i < chartJsonArray.length(); i++) {
				Integer chartId = chartJsonArray.getJSONObject(i).getInt("chartId");

				Show showObj = new Show();
				showObj.hitslasShow(chartId);
				String slaShowPageRes = showObj.getSlaShowJsonStr();
				if (APIUtils.validJsonResponse(slaShowPageRes, "[sla show api response]")) {
					JSONObject jsonObj = new JSONObject(slaShowPageRes);
					JSONObject data = jsonObj.getJSONObject("body").getJSONObject("data");
					if (data.has("priority")) {
						if (data.getJSONObject("priority").has("values")) {
							String priority = data.getJSONObject("priority").getJSONObject("values").getString("name");
							chartsIdPriorityMap.put(chartId, priority);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting charts Id and Priority mapping. {}", e.getMessage());
		}

		return chartsIdPriorityMap;
	}

	private void validateDashboardSLPerformanceForAdvancedFilters(String filterDataResponseStr, String metaDataResponseStr, CustomAssert csAssert, Map<String, String> dashboardSLPerformanceReportMap) {
		FilterUtils filterObj = new FilterUtils();
		Map<Integer, Map<String, String>> filterMap = filterObj.getSpecificFilters(metaDataResponseStr, "primary");
		List<Map<String, String>> filterOptionIdNameMappingList = new ArrayList<>();

		for (Map.Entry<Integer, Map<String, String>> entryMap : filterMap.entrySet()) {
			Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardSLPerformanceReportMap);
			Integer filterId = entryMap.getKey();
			String filterName = entryMap.getValue().get("queryName");
			dashboardReportMapLocal.put("PrimaryFilterName", filterName);
			Map<String, String> filterOptionsIdNameMap = getFilterOptionsIdNameMap(filterId, filterName, filterDataResponseStr);
			if (filterOptionsIdNameMap.size() > 0) {
				for (Map.Entry<String, String> entry : filterOptionsIdNameMap.entrySet()) {
					Map<String, String> tempMap = new HashMap<>();
					tempMap.put(entry.getKey(), entry.getValue());
					filterOptionIdNameMappingList.add(tempMap);
				}
				int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filterOptionsIdNameMap.size() - 1, maxRandomOptionsOfPrimaryFilters);
				for (int i = 0; i < randomNumbers.length; i++) {
					Map<String, String> innerMap = filterOptionIdNameMappingList.get(randomNumbers[i]);
					for (Map.Entry<String, String> entry : innerMap.entrySet()) {
						String primaryFilterOptionId = entry.getKey();
						String primaryFilterOptionName = entry.getValue();

						Map<Integer, Map<String, String>> advancedFilterMap = filterObj.getSpecificFilters(metaDataResponseStr, "advanced");

						for (Map.Entry<Integer, Map<String, String>> entryMapAdvancedFilter : advancedFilterMap.entrySet()) {
							Map<String, String> dashboardReportMapAdvancedFilters = new HashMap<>(dashboardReportMapLocal);
							Integer advancedFilterId = entryMapAdvancedFilter.getKey();
							String advancedFilterName = entryMapAdvancedFilter.getValue().get("queryName");

							dashboardReportMapAdvancedFilters.put("PrimaryFilterOptionName", primaryFilterOptionName);
							dashboardReportMapAdvancedFilters.put("AdvancedFilterName", advancedFilterName);

							Map<String, String> advancedFilterOptionsIdNameMap = getFilterOptionsIdNameMap(advancedFilterId, advancedFilterName, filterDataResponseStr);

							if (advancedFilterOptionsIdNameMap.size() > 0) {

								if (!useRandomizationOnAdvancedFilters) {
									for (Map.Entry<String, String> entryMapLocal : advancedFilterOptionsIdNameMap.entrySet()) {
										Map<String, String> dashboardReportMap = new HashMap<>(dashboardReportMapAdvancedFilters);
										String advancedFilterOptionId = entryMapLocal.getKey();
										String advancedFilterOptionName = entryMapLocal.getValue();

										dashboardReportMap.put("AdvancedFilterOptionName", advancedFilterOptionName);
										String payload = getPayloadForDashboardSLPerformanceWithAdvancedFilter(filterName, primaryFilterOptionId, advancedFilterName, advancedFilterOptionId);
										logger.debug("Hitting dashboard- SL Performance API with Advanced Filter. Payload {}", payload);
										dashboardReportMap.put("slachartPayload", payload);
										DashboardSlaChart slaChartObj = new DashboardSlaChart();
										slaChartObj.hitDashboardSlaChart(payload);
										String slaChartResponseStr = slaChartObj.getDashboardSlaChartJsonStr();
										Boolean isValidJson = APIUtils.validJsonResponse(slaChartResponseStr);
										csAssert.assertTrue(isValidJson, "slaChart api response is not valid json for the payload = " + payload);
										dashboardReportMap.put("isSlaChartValidJsonResponse", isValidJson.toString());

										if (isValidJson) {
											logger.info("slaChart validation passed for PrimaryFilter #{} --> optionId {}(optionName={}). AdvancedFilter #{} --> optionId {}(optionName={})", filterName, primaryFilterOptionId, primaryFilterOptionName, advancedFilterName, advancedFilterOptionId, advancedFilterOptionName);
											if (validateChartData) {
												validateDataForSLPerformanceCharts(slaChartResponseStr, csAssert, dashboardReportMap);
											} else {
												dashboardReportMap.put("isDataValidationPassed", "NA");
												dashboardReportMap.put("TestStatus", "Passed");
												pushReportsResultToCSV(dashboardReportMap);
											}

										} else {
											logger.error("dashboard- SL Performance response is not valid json for PrimaryFilter #{} --> optionId {}(optionName={}). AdvancedFilter #{} --> optionId {}(optionName={})", filterName, primaryFilterOptionId, primaryFilterOptionName, advancedFilterName, advancedFilterOptionId, advancedFilterOptionName);
											csAssert.assertTrue(false, "dashboard- SL Performance response is not valid json for  PrimaryFilter #{}" + filterName + " --> optionId {}" + primaryFilterOptionId + "(optionName={}" + primaryFilterOptionName + "). AdvancedFilter #{}" + advancedFilterName + " --> optionId {}" + advancedFilterOptionId + "(optionName={}" + advancedFilterOptionName + ")");
											dashboardReportMap.put("TestStatus", "Failed[slaChart response is not valid json]");
											pushReportsResultToCSV(dashboardReportMap);
										}
									}
								} else {
									List<Map<String, String>> advancedFilterOptionList = new ArrayList<>();
									for (Map.Entry<String, String> entryM : advancedFilterOptionsIdNameMap.entrySet()) {
										Map<String, String> tempMap = new HashMap<>();
										tempMap.put(entryM.getKey(), entryM.getValue());
										advancedFilterOptionList.add(tempMap);
									}
									List<String> randomOptionId = new ArrayList<>();
									List<String> randomOptionName = new ArrayList<>();
									logger.debug("Applying Randomization on Filter {}.", advancedFilterName);
									int randomNumber[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, advancedFilterOptionsIdNameMap.size() - 1, maxRandomOptions);
									for (int k = 0; k < randomNumber.length; k++) {
										Map<String, String> innerMaps = advancedFilterOptionList.get(randomNumber[k]);
										for (Map.Entry<String, String> entries : innerMaps.entrySet()) {
											randomOptionId.add(entries.getKey());
											randomOptionName.add(entries.getValue());
										}
									}
									for (int j = 0; j < randomOptionId.size(); j++) {
										Map<String, String> dashboardReportMap = new HashMap<>(dashboardReportMapAdvancedFilters);
										dashboardReportMap.put("AdvancedFilterOptionName", randomOptionName.get(j));
										DashboardSlaChart slaChartObj = new DashboardSlaChart();
										String dashboardSLPerformancePayload = getPayloadForDashboardSLPerformanceWithAdvancedFilter(filterName, primaryFilterOptionId, advancedFilterName, randomOptionId.get(j));
										logger.debug("Hitting dashboard SL Performance API for advanced filter validation with Payload {}", dashboardSLPerformancePayload);
										dashboardReportMap.put("slachartPayload", dashboardSLPerformancePayload);
										slaChartObj.hitDashboardSlaChart(dashboardSLPerformancePayload);
										String dashboardSLPerformanceResponseStr = slaChartObj.getDashboardSlaChartJsonStr();
										Boolean isValidJson = APIUtils.validJsonResponse(dashboardSLPerformanceResponseStr, "[dashboard SL Performance response]");
										dashboardReportMap.put("isSlaChartValidJsonResponse", isValidJson.toString());

										if (isValidJson) {
											logger.info("slaChart validation passed for PrimaryFilter #{} --> optionId {}(optionName={}). AdvancedFilter #{} --> optionId {}(optionName={})", filterName, primaryFilterOptionId, primaryFilterOptionName, advancedFilterName, randomOptionId.get(j), randomOptionName.get(j));
											if (validateChartData) {
												validateDataForSLPerformanceCharts(dashboardSLPerformanceResponseStr, csAssert, dashboardReportMap);
											} else {
												dashboardReportMap.put("isDataValidationPassed", "NA");
												dashboardReportMap.put("TestStatus", "Passed");
												pushReportsResultToCSV(dashboardReportMap);
											}
										} else {
											logger.error("dashboard SL Performance response is not valid json for PrimaryFilter #{} --> optionId {}(optionName={}). AdvancedFilter #{} --> optionId {}(optionName={})", filterName, primaryFilterOptionId, primaryFilterOptionName, advancedFilterName, randomOptionId.get(j), randomOptionName.get(j));
											csAssert.assertTrue(false, "dashboard- SL Performance response is not valid json for  PrimaryFilter #{}" + filterName + " --> optionId {}" + primaryFilterOptionId + "(optionName={}" + primaryFilterOptionName + "). AdvancedFilter #{}" + advancedFilterName + " --> optionId {}" + randomOptionId.get(j) + "(optionName={}" + randomOptionName.get(j) + ")");
											dashboardReportMap.put("TestStatus", "Failed[slaChart response is not valid json]");
											pushReportsResultToCSV(dashboardReportMap);
										}
									}
								}
							} else {
								logger.warn("No options found for Advanced filter = {}", advancedFilterName);
							}
						}
					}
				}
			} else {
				logger.warn("No options found for filter = {}", filterName);
			}
		}
	}

	private void validateDashboardSLPerformance(String filterDataResponseStr, String metaDataResponseStr, CustomAssert csAssert, Map<String, String> dashboardSLPerformanceReportMap) {
		FilterUtils filterObj = new FilterUtils();
		Map<Integer, Map<String, String>> filterMap = filterObj.getSpecificFilters(metaDataResponseStr, "primary");

		for (Map.Entry<Integer, Map<String, String>> entryMap : filterMap.entrySet()) {
			List<Map<String, String>> completeOptionList = new ArrayList<Map<String, String>>();
			Map<String, String> dashboardReportMap = new HashMap<>(dashboardSLPerformanceReportMap);

			Integer filterId = entryMap.getKey();
			String filterName = entryMap.getValue().get("queryName");
			dashboardReportMap.put("PrimaryFilterName", filterName);
			Map<String, String> filterOptionsIdNameMap = getFilterOptionsIdNameMap(filterId, filterName, filterDataResponseStr);
			if (filterOptionsIdNameMap.size() > 0) {

				if (!useRandomizationOnFilters) {
					for (Map.Entry<String, String> entry : filterOptionsIdNameMap.entrySet()) {
						Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
						String optionId = entry.getKey();
						String optionName = entry.getValue();

						dashboardReportMapLocal.put("PrimaryFilterOptionName", optionName);
						String payload = getPayloadForDashboardSLPerformance(filterName, Integer.parseInt(optionId));
						dashboardReportMapLocal.put("slachartPayload", payload);
						logger.debug("Hitting dashboard- SL Performance API with Payload {}", payload);
						DashboardSlaChart slaChartObj = new DashboardSlaChart();
						slaChartObj.hitDashboardSlaChart(payload);
						String slaChartResponseStr = slaChartObj.getDashboardSlaChartJsonStr();
						Boolean isValidJson = APIUtils.validJsonResponse(slaChartResponseStr);
						csAssert.assertTrue(isValidJson, "slaChart api response is not valid json for the payload = " + payload);
						dashboardReportMapLocal.put("isSlaChartValidJsonResponse", isValidJson.toString());
						if (isValidJson) {
							logger.debug("slaChart validation for valid json is passed for filter #{} --> optionId {}(optionName={})", filterName, optionId, optionName);
							if (validateChartData) {
								validateDataForSLPerformanceCharts(slaChartResponseStr, csAssert, dashboardReportMapLocal);
							} else {
								dashboardReportMapLocal.put("isDataValidationPassed", "NA");
								dashboardReportMapLocal.put("TestStatus", "Passed");
								pushReportsResultToCSV(dashboardReportMapLocal);
							}
						} else {
							logger.error("dashboard- SL Performance response is not valid json for filter = {} and optionId = {} optionName = {}", filterName,
									optionId, optionName);
							csAssert.assertTrue(false, "dashboard- SL Performance response is not valid json for  filter = " + filterName
									+ " and optionId(name) =" + optionId + "(" + optionName + ")]");
							dashboardReportMapLocal.put("TestStatus", "Failed");
							pushReportsResultToCSV(dashboardReportMapLocal);
						}
					}
				} else {
					for (Map.Entry<String, String> entry : filterOptionsIdNameMap.entrySet()) {
						Map<String, String> tempMap = new HashMap<>();
						tempMap.put(entry.getKey(), entry.getValue());
						completeOptionList.add(tempMap);
					}
					List<String> randomOptionId = new ArrayList<>();
					List<String> randomOptionName = new ArrayList<>();
					logger.info("Applying Randomization on Filter {}.", filterName);
					int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filterOptionsIdNameMap.size() - 1, maxRandomOptions);
					for (int i = 0; i < randomNumbers.length; i++) {
						Map<String, String> innerMap = completeOptionList.get(randomNumbers[i]);
						for (Map.Entry<String, String> entry : innerMap.entrySet()) {
							randomOptionId.add(entry.getKey());
							randomOptionName.add(entry.getValue());
						}
					}
					for (int j = 0; j < randomOptionId.size(); j++) {
						Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
						dashboardReportMapLocal.put("PrimaryFilterOptionName", randomOptionName.get(j));
						DashboardSlaChart slaChartObj = new DashboardSlaChart();
						String dashboardSLPerformancePayload = getPayloadForDashboardSLPerformance(filterName, Integer.parseInt(randomOptionId.get(j)));
						dashboardReportMapLocal.put("slachartPayload", dashboardSLPerformancePayload);
						logger.debug("Hitting dashboard Data API with Payload {}", dashboardSLPerformancePayload);
						slaChartObj.hitDashboardSlaChart(dashboardSLPerformancePayload);
						String dashboardSLPerformanceResponseStr = slaChartObj.getDashboardSlaChartJsonStr();
						Boolean isValidJson = APIUtils.validJsonResponse(dashboardSLPerformanceResponseStr, "[dashboard SL Performance response]");
						dashboardReportMapLocal.put("isSlaChartValidJsonResponse", isValidJson.toString());

						if (isValidJson) {
							logger.info("slaChart validation passed for filter #{} --> optionId {}(optionName={})", filterName,
									randomOptionId.get(j), randomOptionName.get(j));
							if (validateChartData) {
								validateDataForSLPerformanceCharts(dashboardSLPerformanceResponseStr, csAssert, dashboardReportMapLocal);
							} else {
								dashboardReportMapLocal.put("isDataValidationPassed", "NA");
								dashboardReportMapLocal.put("TestStatus", "Passed");
								pushReportsResultToCSV(dashboardReportMapLocal);
							}

						} else {
							logger.error("dashboard SL Performance response is not valid json for filter = {} and optionId = {} optionName = {}", filterName,
									randomOptionId.get(j), randomOptionName.get(j));
							csAssert.assertTrue(false, "dashboard SL Performance response is not valid json for filter = " + filterName
									+ " and optionId(name) =" + randomOptionId.get(j) + "(" + randomOptionName.get(j) + ")]");
							dashboardReportMapLocal.put("TestStatus", "Failed");
							pushReportsResultToCSV(dashboardReportMapLocal);
						}
					}
				}
			} else {
				logger.warn("No filter options found for primary filter = {}", filterName);
			}
		}
	}

	private String getPayloadForDashboardSLPerformance(String filterName, Integer filterId) {
		String payload = null;
		payload = "{\"filters\":{\"" + filterName + "\":[\"" + filterId + "\"]},\"limit\":" + limit + ",\"offset\":" + offset + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\"}";

		return payload;
	}

	private String getPayloadForDashboardSLPerformanceWithAdvancedFilter(String primaryFilter, String primaryFilterId, String advancedFilter, String advancedFilterId) {
		String payload = null;
		payload = "{\"filters\":{\"" + primaryFilter + "\":[\"" + primaryFilterId + "\"],\"" + advancedFilter + "\":[\"" + advancedFilterId + "\"]},\"limit\":" + limit + ",\"offset\":" + offset + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\"}";

		return payload;
	}

	private String getPayloadForDashboardSLPerformanceForSorting(Map<String, List<String>> primaryFilterNameAndOptionsIdMap, String sortingColumnName, String sortingOrder) {
		String payload = null;
		String filters = "{";

		for (Map.Entry<String, List<String>> entryMap : primaryFilterNameAndOptionsIdMap.entrySet()) {
			String filterName = entryMap.getKey();
			List<String> optionIdList = entryMap.getValue();
			String optionIdArray = "[";

			for (int i = 0; i < optionIdList.size(); i++) {

				optionIdArray += "\"" + optionIdList.get(i) + "\",";
			}
			int lastComma = optionIdArray.trim().lastIndexOf(",");
			optionIdArray = optionIdArray.substring(0, lastComma);
			optionIdArray = optionIdArray + "]";

			filters += "\"" + filterName + "\":" + optionIdArray + ",";
		}
		int lastComma = filters.trim().lastIndexOf(",");
		filters = filters.substring(0, lastComma);
		filters = filters + "}";

		payload = "{\"filters\":" + filters + ",\"limit\":" + limit + ",\"offset\":" + offset + ",\"orderByColumnName\":\"" + sortingColumnName + "\",\"orderDirection\":\"" + sortingOrder + "\"}";

		return payload;
	}

	private Map<String, String> getFilterOptionsIdNameMap(Integer filterId, String filterName, String filterDataResponseStr) {
		Map<String, String> allOptionsNameIdMap = new LinkedHashMap<String, String>();

		JSONObject filterDataJson = new JSONObject(filterDataResponseStr);
		FilterUtils filterUtilObj = new FilterUtils();

		JSONUtility jsonObjectTillOptions = new JSONUtility(filterDataJson.getJSONObject(filterId.toString()).getJSONObject("multiselectValues").
				getJSONObject("OPTIONS"));
		boolean autoComplete = jsonObjectTillOptions.getBooleanJsonValue("autoComplete");
		if (autoComplete) {
			{
				logger.info("This filter is autocomplete enabled ,getting the Values From Config File. filter = {}", filterName);
				allOptionsNameIdMap = filterUtilObj.getMapsOfNameIDForAutoComplete(filterName, "dashboard");
			}
		} else {
			List<FilterData.DataClass> optionList = filterUtilObj.getFilterDataList(jsonObjectTillOptions, filterId);
			for (int i = 0; i < optionList.size(); i++) {
				String optionId = optionList.get(i).getDataName();
				String optionName = optionList.get(i).getDataValue();
				allOptionsNameIdMap.put(optionId, optionName);
			}
		}
		return allOptionsNameIdMap;
	}

	private String getFilterDataPayloadForDashboardSLPerformance() {
		String payload = "{\"pageType\":\"globalFilters\"}";
		return payload;
	}

	private void pushReportsResultToCSV(Map<String, String> dashboardSLPerformanceReportMap) {
		logger.debug("Generating CSV File for Dashboard- SL Performance");
		Map<String, String> reportMap = new LinkedHashMap<>();

		reportMap.put("isMetaDataValidJson", "null");
		reportMap.put("isFilterDataValidJson", "null");
		reportMap.put("PrimaryFilterName", "null");
		reportMap.put("PrimaryFilterOptionName", "null");
		reportMap.put("AdvancedFilterName", "null");
		reportMap.put("AdvancedFilterOptionName", "null");
		reportMap.put("slachartPayload", "null");
		reportMap.put("isSlaChartValidJsonResponse", "null");
		reportMap.put("isDataValidationPassed", "null");
		reportMap.put("TestStatus", "null");

		for (Map.Entry<String, String> entryMap : reportMap.entrySet()) {
			if (dashboardSLPerformanceReportMap.containsKey(entryMap.getKey()) && dashboardSLPerformanceReportMap.get(entryMap.getKey()) != null)
				reportMap.put(entryMap.getKey(), dashboardSLPerformanceReportMap.get(entryMap.getKey()));
		}

		ResultInfoClass dashboardResultInfoObj = new DashboardSLPerformanceResultInfo(reportMap.get("isMetaDataValidJson"), reportMap.get("isFilterDataValidJson"),
				reportMap.get("PrimaryFilterName"), reportMap.get("PrimaryFilterOptionName"), reportMap.get("AdvancedFilterName"), reportMap.get("AdvancedFilterOptionName"),
				reportMap.get("slachartPayload"), reportMap.get("isSlaChartValidJsonResponse"), reportMap.get("isDataValidationPassed"), reportMap.get("TestStatus"));
		csvResultsObj.writeReportsToCSVFile(dashboardSLPerformanceReportFilePath + "/" + dashboardSLPerformanceReportFileName, dashboardResultInfoObj);
	}

	private void pushChartsSortingReportsResultToCSV(Map<String, String> dashboardSLPerformanceReportMap) {
		logger.debug("Generating CSV File for Dashboard- SL Performance- Sorting Order Validation");
		Map<String, String> reportMap = new LinkedHashMap<>();

		reportMap.put("isMetaDataValidJson", "null");
		reportMap.put("isFilterDataValidJson", "null");
		reportMap.put("PrimaryFilterName", "null");
		reportMap.put("sortingCriteria", "null");
		reportMap.put("sortingOrder", "null");
		reportMap.put("slaChartPayload", "null");
		reportMap.put("isSlaChartValidJsonResponse", "null");
		reportMap.put("isResponseInSortedOrder", "null");
		reportMap.put("TestStatus", "null");

		for (Map.Entry<String, String> entryMap : reportMap.entrySet()) {
			if (dashboardSLPerformanceReportMap.containsKey(entryMap.getKey()) && dashboardSLPerformanceReportMap.get(entryMap.getKey()) != null)
				reportMap.put(entryMap.getKey(), dashboardSLPerformanceReportMap.get(entryMap.getKey()));
		}

		ResultInfoClass dashboardResultInfoObj = new DashboardSLPerformanceResultInfo(reportMap.get("isMetaDataValidJson"), reportMap.get("isFilterDataValidJson"),
				reportMap.get("PrimaryFilterName"), reportMap.get("sortingCriteria"), reportMap.get("sortingOrder"), reportMap.get("slaChartPayload"),
				reportMap.get("isSlaChartValidJsonResponse"), reportMap.get("isResponseInSortedOrder"), reportMap.get("TestStatus"));
		csvResultsObjForSortingValidation.writeReportsToCSVFile(dashboardSLPerformanceReportFilePath + "/" + dashboardSLPerformanceSortingOrderReportFileName, dashboardResultInfoObj);
	}
}
