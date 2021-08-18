package com.sirionlabs.test.dashboard;

import com.sirionlabs.api.dashboard.DashboardSLExecutiveData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.csvutils.CSVResults;
import com.sirionlabs.utils.csvutils.DashboardSLExecutiveDataResultInfo;
import com.sirionlabs.utils.csvutils.ResultInfoClass;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDashboardSLExecutiveData {
	private final static Logger logger = LoggerFactory.getLogger(TestDashboardSLExecutiveData.class);
	static String dashboardSLExecutiveDataReportFilePath;
	static String dashboardSLExecutiveDataReportFileName;
	static String dashboardSlExecutiveDataConfigFilePath;
	static String dashboardSlExecutiveDataConfigFileName;
	static Integer slExecutiveChartId;
	static Boolean applyRandomizationOnFilters = true;
	static Integer maxRandomOptions = 3;
	static Boolean testSpecificFilters = false;
	static Boolean validateClickableCharts = true;
	static Boolean validateAxisCombinations = true;
	static String delimiterForValues;
	CSVResults csvResultsObjForFilters = null;
	Map<String, String> yAxisValuesWithMandatoryFilter = new HashMap<>();

	@BeforeClass(alwaysRun = true)
	public void settingStaticFields() {
		logger.debug("In BeforeClass");
		try {
			dashboardSlExecutiveDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("dashboardSlExecutiveDataConfigFilePath");
			dashboardSlExecutiveDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardSlExecutiveDataConfigFileName");

			delimiterForValues = ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "delimiterForValues");
			slExecutiveChartId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "slExecutiveChartId"));
			applyRandomizationOnFilters = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "applyRandomizationOnFilters"));
			maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "maxRandomOptions"));
			testSpecificFilters = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "testSpecificFilters"));
			validateClickableCharts = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "validateclickablecharts"));
			validateAxisCombinations = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "validateaxiscombination"));

			//Setting dashboard- Executive view CSV report generation
			dashboardSLExecutiveDataReportFilePath = ConfigureConstantFields.getConstantFieldsProperty("dashboardSLExecutiveDataReportFilePath");
			dashboardSLExecutiveDataReportFileName = ConfigureConstantFields.getConstantFieldsProperty("dashboardSLExecutiveDataReportFileName");
			csvResultsObjForFilters = new CSVResults();
			ResultInfoClass resultInfoClass = new DashboardSLExecutiveDataResultInfo();
			csvResultsObjForFilters.initializeResultCsvFile(dashboardSLExecutiveDataReportFilePath + "/" + dashboardSLExecutiveDataReportFileName, resultInfoClass);

		} catch (Exception e) {
			logger.error("Exception in Before class while setting static fields. {}", e.getMessage());
		}
	}

	@Test(enabled = true, groups = {"smoke", "regression"})
	@Parameters({"TestingType", "Environment"})
	public void testSLExecutiveDataWithoutFilter(String testingType,String environment) {

		if(testingType.contains("smoke") && (environment.contains("Sandbox/AUS") || environment.contains("Prod/EU"))){
			logger.info("For environment" + environment + "skipping the test");
			throw new SkipException("Skipping this test for the sandbox");
		}
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardSLExecutiveReportMap = new HashMap<>();
		dashboardSLExecutiveReportMap.put("testMethodName", "testSLExecutiveDataWithoutFilter");
		try {
			//validating slExecutiveData response when no filter is applied.
			String chartObj = getSLExecutiveDataPayload(slExecutiveChartId);
			dashboardSLExecutiveReportMap.put("payload", chartObj);
			DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
			slExecutiveDataObj.hitDashboardSLExecutiveData(chartObj);
			String slExecutiveDataJsonStr = slExecutiveDataObj.getDashboardSLExecutiveDataJsonStr();
			Boolean isValidJson = APIUtils.validJsonResponse(slExecutiveDataJsonStr, "[DashboardSLExecutiveData response]");
			dashboardSLExecutiveReportMap.put("isSLExecutiveDataResValidJson", isValidJson.toString());
			if (isValidJson) {
				if (validateAxisCombinations) {
					logger.info("validating different combination of X-axis and Y-axis values along with clickable charts data and comparison feature for valid json when no filter is applied.");
					// verifying api response for different combination of X-axis and Y-axis values along with clickable chart data and its line comparison feature
					validateSLExecutiveDataResponse(slExecutiveDataJsonStr, null, csAssert, dashboardSLExecutiveReportMap);
				}
			} else {
				logger.error("slExecutiveData is not valid json. payload={}", chartObj);
				csAssert.assertTrue(false, "slExecutiveData is not valid json. payload = " + chartObj);
				dashboardSLExecutiveReportMap.put("testStatus", "failed");
				pushReportsResultToCSV(dashboardSLExecutiveReportMap);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testSLExecutiveDataFilters {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while validating Dashboard- SLExecutiveData : " + e.getMessage());
			dashboardSLExecutiveReportMap.put("testStatus", "Failed[Exception : " + e.getMessage());
			pushReportsResultToCSV(dashboardSLExecutiveReportMap);
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, groups = {"regression"})
	public void testSLExecutiveDataWithFilters() {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardSLExecutiveReportMap = new HashMap<>();
		dashboardSLExecutiveReportMap.put("testMethodName", "testSLExecutiveDataWithFilters");
		try {
			//validating slExecutiveData response when no filter is applied.
			String chartObj = getSLExecutiveDataPayload(slExecutiveChartId);
			DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
			slExecutiveDataObj.hitDashboardSLExecutiveData(chartObj);
			String slExecutiveDataJsonStr = slExecutiveDataObj.getDashboardSLExecutiveDataJsonStr();
			Boolean isValidJson = APIUtils.validJsonResponse(slExecutiveDataJsonStr, "[DashboardSLExecutiveData response]");
			if (isValidJson) {
				logger.info("validating slExecutiveData response for valid json when filter is applied.");
				verifyDashboardExecutiveViewFilters(slExecutiveDataJsonStr, csAssert, dashboardSLExecutiveReportMap);     // verifying filters along with clickable data and chart comparison
			} else {
				logger.error("slExecutiveData is not valid json. payload={}", chartObj);
				csAssert.assertTrue(false, "slExecutiveData is not valid json. payload = " + chartObj);
				dashboardSLExecutiveReportMap.put("testStatus", "failed[slExecutiveData is not valid json(without filter)]");
				pushReportsResultToCSV(dashboardSLExecutiveReportMap);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testSLExecutiveDataFilters {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while validating Dashboard- SLExecutiveData : " + e.getMessage());
			dashboardSLExecutiveReportMap.put("testStatus", "Failed[Exception : " + e.getMessage() + "]");
			pushReportsResultToCSV(dashboardSLExecutiveReportMap);
		}
		csAssert.assertAll();
	}

	private String getSLExecutiveDataPayload(Integer chartId) {
		return this.getSLExecutiveDataPayload(chartId, null, null);
	}

	private String getSLExecutiveDataPayload(Integer chartId, String filterParam) {
		return this.getSLExecutiveDataPayload(chartId, filterParam, null);
	}

	private String getSLExecutiveDataPayload(Integer chartId, String filterParam, String additionalParams) {
		String payload = null;

		Calendar cal = Calendar.getInstance();

		Integer currentYear = cal.get(Calendar.YEAR);
		// month start from 0 to 11
		Integer currentMonth = cal.get(Calendar.MONTH) + 1;
		//Integer lastDate = cal.getActualMaximum(Calendar.DATE);

		/* setting date range for 1 year*/
		String startDate = (currentYear - 1) + "-" + currentMonth + "-" + "01";
		String endDate = currentYear + "-" + (currentMonth - 1) + "-" + "28";

		if (filterParam == null && additionalParams == null) {
			payload = "{\"overrideSessionData\":false,\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":[],\"userSpecificView\":false,\"newFilterApplied\":true,\"additionalParams\":{},\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}";
		} else if (additionalParams == null) {
			payload = "{\"overrideSessionData\":false,\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":" + filterParam + ",\"userSpecificView\":false,\"newFilterApplied\":true,\"additionalParams\":{},\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}";
		} else if (filterParam == null) {
			payload = "{\"overrideSessionData\":false,\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":[],\"userSpecificView\":false,\"newFilterApplied\":true,\"additionalParams\":" + additionalParams + ",\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}";
		} else {
			payload = "{\"overrideSessionData\":false,\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":" + filterParam + ",\"userSpecificView\":false,\"newFilterApplied\":true,\"additionalParams\":" + additionalParams + ",\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}";
		}

		return payload;
	}

	private void verifyDashboardExecutiveViewFilters(String slExecutiveDataJsonStr, CustomAssert csAssert, Map<String, String> dashboardSLExecutiveReportMap) {
		try {
			if (testSpecificFilters) {
				logger.info("Running script for specific filters only.");
				List<String> filtersToTest = getSpecificFiltersToTestFromConfig(slExecutiveDataJsonStr);
				for (int i = 0; i < filtersToTest.size(); i++) {
					List<String> filterOptionsToTest = getSpecificFilterOptionsToTestFromConfig(filtersToTest.get(i), slExecutiveDataJsonStr);

					for (int j = 0; j < filterOptionsToTest.size(); j++) {
						Map<String, String> dashboardSLExecutiveReportMapLocal = new HashMap<>(dashboardSLExecutiveReportMap);
						logger.info("Validating slExecutiveData response for filter {}-->{}", filtersToTest.get(i), filterOptionsToTest.get(j));
						this.validateSLExecutiveDataResponseWithFilters(filtersToTest.get(i), filterOptionsToTest.get(j), csAssert, dashboardSLExecutiveReportMapLocal);

					}
				}
			} else {
				logger.info("Running script for all the filters.");
				DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
				List<String> filtersList = slExecutiveDataObj.getAllAttributeNames(slExecutiveDataJsonStr);

				for (int i = 0; i < filtersList.size(); i++) {
					List<String> filterOptionsList = slExecutiveDataObj.getFilterData(slExecutiveDataJsonStr, filtersList.get(i));
					List<String> randomOptionName = new ArrayList<>();

					if (applyRandomizationOnFilters) {
						if (filterOptionsList.size() > 0) {
							logger.info("Applying Randomization on Filter {}.", filtersList.get(i));
							int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filterOptionsList.size() - 1, maxRandomOptions);
							for (int k = 0; k < randomNumbers.length; k++) {
								String filterOption = filterOptionsList.get(randomNumbers[k]);
								randomOptionName.add(filterOption);
							}
							for (int l = 0; l < randomOptionName.size(); l++) {
								Map<String, String> dashboardSLExecutiveReportMapLocal = new HashMap<>(dashboardSLExecutiveReportMap);
								logger.info("Validating slExecutiveData response for filter {}-->{}", filtersList.get(i), randomOptionName.get(l));
								this.validateSLExecutiveDataResponseWithFilters(filtersList.get(i), randomOptionName.get(l), csAssert, dashboardSLExecutiveReportMapLocal);
							}
						} else {
							logger.warn("No options found for filter {}.", filtersList.get(i));
						}
					} else {
						for (int j = 0; j < filterOptionsList.size(); j++) {
							Map<String, String> dashboardSLExecutiveReportMapLocal = new HashMap<>(dashboardSLExecutiveReportMap);
							logger.info("Validating slExecutiveData response for filter {}-->{}", filtersList.get(i), filterOptionsList.get(j));
							this.validateSLExecutiveDataResponseWithFilters(filtersList.get(i), filterOptionsList.get(j), csAssert, dashboardSLExecutiveReportMapLocal);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in verifyDashboardExecutiveViewFilters method. {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			dashboardSLExecutiveReportMap.put("testStatus", "failed[Exception : " + e.getMessage() + "]");
			pushReportsResultToCSV(dashboardSLExecutiveReportMap);
		}
	}

	private List<String> getSpecificFiltersToTestFromConfig(String slExecutiveDataJsonStr) throws ConfigurationException {
		DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
		List<String> filtersList = slExecutiveDataObj.getAllAttributeNames(slExecutiveDataJsonStr);
		List<String> filtersToTest = new ArrayList<>();

		filtersToTest = ParseConfigFile.getAllPropertiesOfSection(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "specificFilters");
		if (filtersToTest.size() == 0) {
			filtersToTest = filtersList;
		} else {
			String[] allAttributeLabels = slExecutiveDataObj.getAllAttributeLabels(slExecutiveDataJsonStr);
			for (String attribute : filtersToTest) {
				for (String attributeLabel : allAttributeLabels) {
					if (attribute.equalsIgnoreCase(attributeLabel)) {
						filtersToTest.remove(attribute);
						filtersToTest.add(attributeLabel);
						break;
					}
				}
			}

		}
		return filtersToTest;
	}

	private List<String> getSpecificFilterOptionsToTestFromConfig(String filterToTest, String slExecutiveDataJsonStr) throws ConfigurationException {

		DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
		List<String> filterOptionsToTest = new ArrayList<>();

		String filterOptionsFromConfig[] = ParseConfigFile.getValueFromConfigFile(dashboardSlExecutiveDataConfigFilePath, dashboardSlExecutiveDataConfigFileName, "specificFilters", filterToTest).split(Pattern.quote(delimiterForValues));
		List<String> filterOptions = slExecutiveDataObj.getFilterData(slExecutiveDataJsonStr, filterToTest);

		if (filterOptionsFromConfig.length == 0) {
			logger.info("Values not provided for Specific Filter {}. Hence picking all Available options.", filterToTest);
			filterOptionsToTest = filterOptions;
		} else {
			for (int j = 0; j < filterOptionsFromConfig.length; j++) {
				if (filterOptions.contains(filterOptionsFromConfig[j])) {
					filterOptionsToTest.add(filterOptionsFromConfig[j]);
				} else {
					logger.error("No such filter option(={}) found under filter = {}", filterOptionsFromConfig[j], filterToTest);
				}
			}
		}
		return filterOptionsToTest;
	}

	private void validateSLExecutiveDataResponseWithFilters(String filterName, String filterOptionValue, CustomAssert csAssert, Map<String, String> dashboardSLExecutiveReportMap) {

		DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();

		dashboardSLExecutiveReportMap.put("filterName", filterName);
		dashboardSLExecutiveReportMap.put("filterOption", filterOptionValue);

		String filterParam = "[{\"key\":\"" + filterName + "\",\"values\":[\"" + filterOptionValue + "\"]}]";
		String chartObj = getSLExecutiveDataPayload(slExecutiveChartId, filterParam);
		dashboardSLExecutiveReportMap.put("payload", chartObj);
		slExecutiveDataObj.hitDashboardSLExecutiveData(chartObj);
		String slExecutiveDataJsonStrOnApplyingFilters = slExecutiveDataObj.getDashboardSLExecutiveDataJsonStr();
		Boolean isValidJson = APIUtils.validJsonResponse(slExecutiveDataJsonStrOnApplyingFilters, "[DashboardSLExecutiveData response after applying filter]");
		dashboardSLExecutiveReportMap.put("isSLExecutiveDataResValidJson", isValidJson.toString());
		csAssert.assertTrue(isValidJson, "SL ExecutiveData response is not valid json for the filter {}" + filterName + "-->{}" + filterOptionValue);
		if (isValidJson) {
			logger.debug("SL Executive response is valid json for filter {}-->{}, payload {}", filterName, filterOptionValue, chartObj);

			if (validateClickableCharts) {
				logger.info("Validating clickable charts data and charts comparison feature for valid json");
				String xAxisName = null;
				String yAxisName = null;
				Map<String, List<String>> axisMapOptions = getYaxisAndXaxisOptionMap(slExecutiveDataJsonStrOnApplyingFilters);
				for (Map.Entry<String, List<String>> entryMap : axisMapOptions.entrySet()) {
					yAxisName = entryMap.getKey();
					if (yAxisValuesWithMandatoryFilter.containsKey(yAxisName)) {
						continue;
					}
					xAxisName = entryMap.getValue().get(0);
					dashboardSLExecutiveReportMap.put("yAxisValue", yAxisName);
					dashboardSLExecutiveReportMap.put("xAxisValue", xAxisName);
					break;
				}
				validateClickableChartAndComparisonFeature(slExecutiveDataJsonStrOnApplyingFilters, filterParam, xAxisName, yAxisName, csAssert, dashboardSLExecutiveReportMap);
			}
		} else {
			logger.error("SL ExecutiveData response is not valid json for the filter {}-->{} , payload {}", filterName, filterOptionValue, chartObj);
			dashboardSLExecutiveReportMap.put("testStatus", "failed");
			pushReportsResultToCSV(dashboardSLExecutiveReportMap);
		}
	}

	private void validateClickableChartAndComparisonFeature(String slExecutiveDataJsonStr, String filterParam, String xAxisName, String yAxisName, CustomAssert csAssert, Map<String, String> csvReportMap) {

		Map<String, String> csvReportMapLocal = new HashMap<>(csvReportMap);
		try {
			//Validate clickable charts data for valid json
			logger.info("Validating clickable charts for filter : {}, x-axis : {}, y-axis : {}", filterParam, xAxisName, yAxisName);
			JSONObject responseJsonObj = new JSONObject(slExecutiveDataJsonStr);
			Boolean isClickableChartDataFound = false;
			if (responseJsonObj.getInt("totalPivots") > 0) {
				JSONArray dataSetArray = responseJsonObj.getJSONObject("chartData").getJSONArray("dataset");
				for (int i = 0; i < dataSetArray.length(); i++) {
					JSONArray dataArray = dataSetArray.getJSONObject(i).getJSONArray("data");
					for (int j = 0; j < dataArray.length(); j++) {
						Boolean isClickable = dataArray.getJSONObject(j).getBoolean("isClickable");
						if (isClickable) {
							isClickableChartDataFound = true;
							Map<String, String> reportMapForEachClickableChart = new HashMap<>(csvReportMap);
							reportMapForEachClickableChart.put("isClickableChartsFound", "true");
							String rowId = dataArray.getJSONObject(j).getString("rowId");
							String columnId = dataArray.getJSONObject(j).getString("columnId");
							String additionalParam = "{\"pivot\":\"" + xAxisName + "\",\"split\":\"" + yAxisName + "\",\"lineChartMap\":\"true\",\"lineSplitVal\":\"" + rowId + "\",\"linePivotVal\":\"" + columnId + "\"}";
							reportMapForEachClickableChart.put("additionalParamForClickableCharts", additionalParam);
							String payloadForClickableChartData = getSLExecutiveDataPayload(slExecutiveChartId, filterParam, additionalParam);
							reportMapForEachClickableChart.put("payloadForClickableChartData", payloadForClickableChartData);
							DashboardSLExecutiveData slExecutiveData = new DashboardSLExecutiveData();
							slExecutiveData.hitDashboardSLExecutiveData(payloadForClickableChartData);
							String slExeDataResForClickableChartData = slExecutiveData.getDashboardSLExecutiveDataJsonStr();
							Boolean isSlExeDataResForClickableChartDataValidJson = APIUtils.validJsonResponse(slExeDataResForClickableChartData, "slExecutive chartData api response for clickable chart data.");
							reportMapForEachClickableChart.put("isClickableChartValidJson", isSlExeDataResForClickableChartDataValidJson.toString());
							if (isSlExeDataResForClickableChartDataValidJson) {
								//validating chart comparison tab/feature
								additionalParam = additionalParam.replace("}", ",");
								additionalParam = additionalParam + "\"isCompareLineChart\":\"true\"}";
								String payloadForComparisonTab = getSLExecutiveDataPayload(slExecutiveChartId, filterParam, additionalParam);
								reportMapForEachClickableChart.put("payloadForComparisonTab", payloadForComparisonTab);
								DashboardSLExecutiveData slExeDataObj = new DashboardSLExecutiveData();
								slExeDataObj.hitDashboardSLExecutiveData(payloadForComparisonTab);
								String slExeDataResForComparisonTab = slExecutiveData.getDashboardSLExecutiveDataJsonStr();
								Boolean isSlExeDataResForComparisonTabValidJson = APIUtils.validJsonResponse(slExeDataResForComparisonTab, "slExecutive chartData api response for comparison tab.");

								reportMapForEachClickableChart.put("isComparisonTabResValidJson", isSlExeDataResForComparisonTabValidJson.toString());
								if (isSlExeDataResForComparisonTabValidJson) {
									logger.info("test validation passed for comparison tab for {}", additionalParam);
									reportMapForEachClickableChart.put("testStatus", "passed");
									pushReportsResultToCSV(reportMapForEachClickableChart);
								} else {
									logger.error("slExecutiveData response is not valid json for compare line chart. payload = {}", additionalParam);
									csAssert.assertTrue(false, "slExecutiveData response is not valid json for compare line chart. payload = " + additionalParam);
									reportMapForEachClickableChart.put("testStatus", "failed");
									pushReportsResultToCSV(reportMapForEachClickableChart);
								}
							} else {
								logger.error("slExecutiveData response is not valid json for chart data with rowId = {} and columnId = {}", rowId, columnId);
								csAssert.assertTrue(false, "slExecutiveData response is not valid json for chart data with rowId = " + rowId + " and columnId = " + columnId);
								reportMapForEachClickableChart.put("testStatus", "failed");
								pushReportsResultToCSV(reportMapForEachClickableChart);
							}
						}
					}
				}
				if (!isClickableChartDataFound) {
					logger.warn("No Clickable chart data found in response of X-axis  = {}, Y-axis = {}", xAxisName, yAxisName);
					csvReportMapLocal.put("isClickableChartsFound", "false");
					csvReportMapLocal.put("testStatus", "passed[No Clickable chart Data]");
					pushReportsResultToCSV(csvReportMapLocal);
				}
			} else {
				logger.warn("Empty dataSet found for filterParam = {}", filterParam);
				csvReportMapLocal.put("testStatus", "passed[empty data]");
				pushReportsResultToCSV(csvReportMapLocal);
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating clickable chart data and comparison feature. {}", e.getMessage());
			csvReportMapLocal.put("testStatus", "failed[Exception in validateClickableChartAndComparisonFeature method = " + e.getMessage() + "]");
			pushReportsResultToCSV(csvReportMapLocal);
		}
	}

	private Map<String, List<String>> getYaxisAndXaxisOptionMap(String slExecutiveDataResponseStr) {
		Map<String, List<String>> yAxisAndXaxisOtionMap = new HashMap<>();

		try {
			JSONObject responseJsonObj = new JSONObject(slExecutiveDataResponseStr);
			JSONArray optionsArray = responseJsonObj.getJSONArray("options");

			for (int i = 0; i < optionsArray.length(); i++) {
				List<String> xAxisOptionList = new ArrayList<>();
				JSONObject jsonObject = optionsArray.getJSONObject(i);
				String yAxisName = jsonObject.getString("name");
				if (jsonObject.has("mandatoryFilter")) {
					this.yAxisValuesWithMandatoryFilter.put(yAxisName, jsonObject.getString("mandatoryFilter"));
				}
				JSONArray xAxisArray = jsonObject.getJSONArray("xaxis");
				for (int j = 0; j < xAxisArray.length(); j++) {
					String xAxisName = xAxisArray.getJSONObject(j).getString("name");
					xAxisOptionList.add(xAxisName);
				}
				yAxisAndXaxisOtionMap.put(yAxisName, xAxisOptionList);
			}
		} catch (Exception e) {
			logger.error("Exception occurred while getting yAxisAndXaxisOtionMap. {}", e.getMessage());
		}

		return yAxisAndXaxisOtionMap;
	}

	/*
	* Below method is for validating SLExecutiveData response For Different X and Y axis Values With ClickableCharts And Comparison feature
	* */
	private void validateSLExecutiveDataResponse(String slExecutiveDataRes, String filterParamForClickableCharts, CustomAssert csAssert, Map<String, String> dashboardSLExecutiveReportMap) {
		Map<String, List<String>> axisOptionsMap = getYaxisAndXaxisOptionMap(slExecutiveDataRes);

		try {
			for (Map.Entry<String, List<String>> entryMap : axisOptionsMap.entrySet()) {
				Map<String, String> reportMapForEachYaxisValue = new HashMap<>(dashboardSLExecutiveReportMap);
				String yAxisName = entryMap.getKey();
				reportMapForEachYaxisValue.put("yAxisValue", yAxisName);
				String filterParam = filterParamForClickableCharts;
				if (yAxisValuesWithMandatoryFilter.containsKey(yAxisName)) {
					String mandatoryFilter = yAxisValuesWithMandatoryFilter.get(yAxisName);
					DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
					List<String> filterOptionsList = slExecutiveDataObj.getFilterData(slExecutiveDataRes, mandatoryFilter);
					if (filterOptionsList.size() > 0) {
						logger.info("Applying Randomization on mandatory Filter = {} for Y-axis = {}.", mandatoryFilter, yAxisName);
						int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filterOptionsList.size() - 1, 1);
						String filterOption = filterOptionsList.get(randomNumbers[0]);

						filterParam = "[{\"key\":\"" + mandatoryFilter + "\",\"values\":[\"" + filterOption + "\"]}]";
						reportMapForEachYaxisValue.put("mandatoryParam", mandatoryFilter + "[" + filterOption + "]");
					}
				}
				for (int i = 0; i < entryMap.getValue().size(); i++) {
					Map<String, String> reportMapForEachXaxisValue = new HashMap<>(reportMapForEachYaxisValue);
					String xAxisName = entryMap.getValue().get(i);
					reportMapForEachXaxisValue.put("xAxisValue", xAxisName);
					String additionalParam = "{\"pivot\":\"" + xAxisName + "\",\"split\":\"" + yAxisName + "\"}";
					String payload = getSLExecutiveDataPayload(slExecutiveChartId, filterParam, additionalParam);
					reportMapForEachXaxisValue.put("payloadForDifferentAxisValues", payload);
					DashboardSLExecutiveData slExecutiveDataObj = new DashboardSLExecutiveData();
					slExecutiveDataObj.hitDashboardSLExecutiveData(payload);
					String slExecutiveDataResStr = slExecutiveDataObj.getDashboardSLExecutiveDataJsonStr();
					Boolean isValidJson = APIUtils.validJsonResponse(slExecutiveDataResStr);
					reportMapForEachXaxisValue.put("isSlExecutiveDataValidJson", isValidJson.toString());
					if (isValidJson) {
						logger.info("SlExecutiveData is ValidJson for X-axis = {} and Y-axis = {}", xAxisName, yAxisName);
						//validating that chart should load with the given x-axis and y-axis values.
						JSONObject jsonObject = new JSONObject(slExecutiveDataResStr);
						JSONObject selectedAxisValues = jsonObject.getJSONObject("selected");
						String selectedYaxisValue = selectedAxisValues.getJSONObject("yaxis").getString("name");
						String selectedXaxisValue = selectedAxisValues.getJSONObject("xaxis").getString("name");

						if (selectedYaxisValue.equalsIgnoreCase(yAxisName) && selectedXaxisValue.equalsIgnoreCase(xAxisName)) {
							logger.info("Validation passed for x-axis and y-axis values. Expected X-axis = {}, Y-axis = {}", xAxisName, yAxisName);
						} else {
							logger.error("Validation failed. Different X-axis or Y-axis values found. Expected X-axis = {}, Y-axis = {}.  Actual X-axis = {}, Y-axis = {}", xAxisName, yAxisName, selectedXaxisValue, selectedYaxisValue);
							csAssert.assertTrue(false, "Validation failed. Different X-axis or Y-axis values found. Expected X-axis = " + xAxisName + ", Y-axis = " + yAxisName + ".  Actual X-axis = " + selectedXaxisValue + ", Y-axis = " + selectedYaxisValue);
						}
						//Validating clickable chartData and chartComparison line feature
						if (validateClickableCharts)
							validateClickableChartAndComparisonFeature(slExecutiveDataResStr, filterParam, xAxisName, yAxisName, csAssert, reportMapForEachXaxisValue);

					} else {
						logger.error("slExecutiveData api response is not valid json for X-axis = {} and Y-axis = {}", xAxisName, yAxisName);
						csAssert.assertTrue(false, "slExecutiveData api response is not valid json for X-axis = " + xAxisName + " and Y-axis = " + yAxisName);
						reportMapForEachXaxisValue.put("testStatus", "failed");
						pushReportsResultToCSV(reportMapForEachXaxisValue);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating SLExecutiveDataResponseForDifferentXandYaxisValues. {}", e.getMessage());
			dashboardSLExecutiveReportMap.put("testStatus", "failed[Exception in validateSLExecutiveDataResponse method : " + e.getMessage() + "]");
			pushReportsResultToCSV(dashboardSLExecutiveReportMap);
		}
	}

	private void pushReportsResultToCSV(Map<String, String> dashboardSlReportMap) {
		logger.debug("Generating CSV File");
		Map<String, String> reportMap = new HashMap<>();

		reportMap.put("testMethodName", "null");
		reportMap.put("filterName", "null");
		reportMap.put("filterOption", "null");
		reportMap.put("payload", "null");
		reportMap.put("isSLExecutiveDataResValidJson", "null");
		reportMap.put("yAxisValue", "null");
		reportMap.put("mandatoryParam", "null");
		reportMap.put("xAxisValue", "null");
		reportMap.put("payloadForDifferentAxisValues", "null");
		reportMap.put("isSlExecutiveDataValidJson", "null");
		reportMap.put("isClickableChartsFound", "null");
		reportMap.put("additionalParamForClickableCharts", "null");
		reportMap.put("payloadForClickableChartData", "null");
		reportMap.put("isClickableChartValidJson", "null");
		reportMap.put("payloadForComparisonTab", "null");
		reportMap.put("isComparisonTabResValidJson", "null");
		reportMap.put("testStatus", "null");

		for (Map.Entry<String, String> entryMap : reportMap.entrySet()) {
			if (dashboardSlReportMap.containsKey(entryMap.getKey()))
				reportMap.put(entryMap.getKey(), dashboardSlReportMap.get(entryMap.getKey()));
		}
		ResultInfoClass dashboardResultInfoObj = new DashboardSLExecutiveDataResultInfo(reportMap.get("testMethodName"), reportMap.get("filterName"), reportMap.get("filterOption"), reportMap.get("payload"),
				reportMap.get("isSLExecutiveDataResValidJson"), reportMap.get("yAxisValue"), reportMap.get("mandatoryParam"), reportMap.get("xAxisValue"), reportMap.get("payloadForDifferentAxisValues"), reportMap.get("isSlExecutiveDataValidJson"), reportMap.get("isClickableChartsFound"), reportMap.get("additionalParamForClickableCharts"),
				reportMap.get("payloadForClickableChartData"), reportMap.get("isClickableChartValidJson"), reportMap.get("payloadForComparisonTab"),
				reportMap.get("isComparisonTabResValidJson"), reportMap.get("testStatus"));
		csvResultsObjForFilters.writeReportsToCSVFile(dashboardSLExecutiveDataReportFilePath + "/" + dashboardSLExecutiveDataReportFileName, dashboardResultInfoObj);
	}
}
