package com.sirionlabs.test.dashboard;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.dashboard.*;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.test.reportRenderer.FilterData;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDashboard {
	private final static Logger logger = LoggerFactory.getLogger(TestDashboard.class);
	public static String[] doughnut2dCharts = null;
	static String dashboardConfigFilePath;
	static String dashboardConfigFileName;
	static String dashboardCSVFilePath;
	static String dashboardCSVFileName;
	static String delimiterForValues;
	static int offset;
	static int size;
	static List<Integer> newFrameworkChartsId = new ArrayList<Integer>();
	static List<Integer> oldFrameworkChartsId = new ArrayList<Integer>();
	static int maxRandomOptions = 3;
	static Map<String, String> chartTypeMap = new HashMap<>();
	static boolean useRandomizationOnLocalFilters = true;
	static boolean useRandomizationOnGlobalFilters = true;
	static boolean testAllFilters = true;
	static boolean validateRecordsApi = true;
	static boolean validateRecordsData = false;
	static String dashboardShowPageConfigFilePath;
	static String dashboardShowPageConfigFileName;
	static Integer dashboardListId = null;
	static String[] chartsWithLocalFilterApi = null;
	static String[] chartsWithDependentFilters = null;
	static String minSliderValue = "";
	static String maxSliderValue = "";
	String userRequestId = null;
	DumpResultsIntoCSV dumpResultsObj;
	static boolean runOnSeparateEnv = false;
	static String environmentFileName = "";
	static String oldEnvironmentFileName = "";

	@BeforeTest(alwaysRun = true)
	public void setStaticFields() throws ConfigurationException {
		TestDashboard.dashboardConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DashboardConfigFilePath");
		TestDashboard.dashboardConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DashboardConfigFileName");
		TestDashboard.delimiterForValues = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"delimiterforvalues");
		offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "offset"));
		size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "size"));

		newFrameworkChartsId = this.getChartsId("chartsidwithnewframework");
		oldFrameworkChartsId = this.getChartsId("chartsidwitholdframework");
		chartTypeMap = ParseConfigFile.getAllConstantProperties(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "chartType");
		dashboardListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"dashboardlistid"));

		if (!ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"minSliderValue").equalsIgnoreCase(""))
			minSliderValue = (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "minSliderValue"));

		if (!ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"maxSliderValue").equalsIgnoreCase(""))
			maxSliderValue = (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "maxSliderValue"));

		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"testallfilters").equalsIgnoreCase("false"))
			testAllFilters = false;

		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"applyrandomizationonlocalfilters").equalsIgnoreCase("false"))
			useRandomizationOnLocalFilters = false;

		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"applyrandomizationonglobalfilters").equalsIgnoreCase("false"))
			useRandomizationOnGlobalFilters = false;

		if (!ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"maxrandomoptions").trim().equalsIgnoreCase(""))
			maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
					"maxrandomoptions").trim());

		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"validatedata").equalsIgnoreCase("true"))
			validateRecordsData = true;

		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"validatedata").equalsIgnoreCase("false"))
			validateRecordsApi = false;

		//Setting dashboard CSV report generation
		dashboardCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("DashboardCSVFilePath");
		dashboardCSVFileName = ConfigureConstantFields.getConstantFieldsProperty("DashboardCSVFileName");
		dumpResultsObj = new DumpResultsIntoCSV(dashboardCSVFilePath, dashboardCSVFileName, setHeadersInCSVFile());

		dashboardShowPageConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DashboardShowPageMappingFilePath");
		dashboardShowPageConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DashboardShowPageMappingFileName");

		doughnut2dCharts = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "doughnut2dcharts").split(
				Pattern.quote(TestDashboard.delimiterForValues));

		chartsWithLocalFilterApi = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "chartswithlocalfilterapi").split(
				Pattern.quote(TestDashboard.delimiterForValues));

		chartsWithDependentFilters = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, "chartsWithDependentFilters").split(
				Pattern.quote(TestDashboard.delimiterForValues));

		/* Setting different environment for running dashboard suite*/
		if (ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"testondifferentenv").equalsIgnoreCase("true"))
			runOnSeparateEnv = true;

		environmentFileName = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
				"envfilename");

		if (runOnSeparateEnv) {
			loggingOnSeparateEnv(environmentFileName);
		}

	}

	@AfterTest(alwaysRun = true)
	public void loggingOnOriginalEnv() {
		try {
			logger.info("In After Class.");
			if (runOnSeparateEnv) {
				logger.info("Setting the configuration to original environment");
				ConfigureEnvironment.configureProperties(oldEnvironmentFileName, true);

				Check checkObj = new Check();
				//Login on different env.
				checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
				Assert.assertTrue(Check.getAuthorization() != null, "Login for Dashboard Suite. Authorization not set by Check Api. Environment = " + oldEnvironmentFileName);

			}
		} catch (Exception e) {
			logger.error("Exception occurred while rolling back the environment configuration. {}", e.getMessage());
			e.printStackTrace();
		}

	}

	private List<Integer> getChartsId(String propertyName) throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, propertyName);
		List<Integer> idList = new ArrayList<Integer>();

		if (!value.trim().equalsIgnoreCase("")) {
			String chartsId[] = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, propertyName).split(
					Pattern.quote(TestDashboard.delimiterForValues));

			for (int i = 0; i < chartsId.length; i++)
				idList.add(Integer.parseInt(chartsId[i].trim()));
		}
		return idList;
	}

	@DataProvider(parallel = false)
	public Object[][] getAllChartIds() {
		logger.info("Hitting Dashboard Charts Api for All DashboardCharts");
		List<Integer> allChartsId = new ArrayList<>();
		DashboardCharts chartObj = new DashboardCharts();
		chartObj.hitDashboardCharts();
		String chartResponse = chartObj.getDashboardChartsJsonStr();
		logger.debug("chart response {}", chartResponse);
		allChartsId = chartObj.getAllChartIdsExcludingManualCharts(chartResponse);

		List<Object[]> allTestData = new ArrayList<Object[]>();
		for (int i = 0; i < allChartsId.size(); i++) {
			allTestData.add(new Object[]{allChartsId.get(i)});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@DataProvider(parallel = false)
	public Object[][] getChartIdsFromConfigFile() {
		logger.info("Getting Dashboard Chart Ids from config file");

		Integer totalChartsCount = newFrameworkChartsId.size() + oldFrameworkChartsId.size();
		Object[][] chartIds = new Object[totalChartsCount][];

		List<Integer> tempList1 = new ArrayList(newFrameworkChartsId);
		List<Integer> tempList2 = new ArrayList(oldFrameworkChartsId);
		tempList1.addAll(tempList2);
		Collections.sort(tempList1);

		logger.info("chart ids in config file : total count ={}, ids ={}", tempList1.size(), tempList1);

		int i = 0;
		for (Integer chartId : tempList1) {

			chartIds[i] = new Object[1];
			chartIds[i][0] = chartId;
			i++;
		}
		return chartIds;
	}

	@Test(dataProvider = "getChartIdsFromConfigFile", enabled = true, groups = {"smoke", "regression"})
	public void testDashboard(Integer chartId) {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardReportMap = new HashMap<String, String>();
		dashboardReportMap.put("TestType", "TestDashboardLocalFilters");

		try {
			if (TestDashboard.newFrameworkChartsId.contains(chartId) || TestDashboard.oldFrameworkChartsId.contains(chartId)) {

				logger.info("validation started for chart id : {}", chartId);
				dashboardReportMap.put("ChartID", chartId.toString());
				DashboardData dataObj = new DashboardData();
				String dashboardDataPayload = PayloadUtils.getPayloadForDashboardData(chartId);
				dashboardReportMap.put("DashboardDataRequestPayload", dashboardDataPayload);
				dataObj.hitDashboardData(dashboardDataPayload);
				Boolean isValidJson = APIUtils.validJsonResponse(dataObj.getDashboardDataJsonStr(), "[dashboardData response]");
				dashboardReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString());
				if (isValidJson) {
					if (dataObj.getDashboardDataJsonStr().equals("[]") || dataObj.getDashboardDataJsonStr().equals("{}")) {
						logger.warn("DashboardData response for chartId {} is empty [response = {}]. Hence skipping further test for this chart", chartId,
								dataObj.getDashboardDataJsonStr());
						dashboardReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString() + "[empty response]");
						dumpResultsIntoCSV(dashboardReportMap);
					} else {

						List<String> outputGroups = getDashboardOutputGroups(dataObj.getDashboardDataJsonStr());
						if (outputGroups.size() == 0) {
							dashboardReportMap.put("CurrentOutputGroup", "NULL[No output group in response]");
							if (TestDashboard.newFrameworkChartsId.contains(chartId)) {
								this.verifyDashboardCharts(chartId, "new", null, dashboardReportMap, csAssert);
							} else if (TestDashboard.oldFrameworkChartsId.contains(chartId)) {
								this.verifyDashboardCharts(chartId, "old", null, dashboardReportMap, csAssert);
							}

						} else {
							/*iterate over different outputGroups like Supplier(default), TCV Range*/
							for (int i = 0; i < outputGroups.size(); i++) {
								/*clearing csv report data for next output group*/
								Map<String, String> dashboardReportMapForOutputGrp = new HashMap<>(dashboardReportMap);
								dashboardReportMapForOutputGrp.put("testStatus", "passed");

								logger.info("verifying currentOutputGroup : {}", outputGroups.get(i));
								dashboardReportMapForOutputGrp.put("currentOutputGroup", outputGroups.get(i));
								if (TestDashboard.newFrameworkChartsId.contains(chartId)) {
									this.verifyDashboardCharts(chartId, "new", outputGroups.get(i), dashboardReportMapForOutputGrp, csAssert);
								} else if (TestDashboard.oldFrameworkChartsId.contains(chartId)) {
									this.verifyDashboardCharts(chartId, "old", outputGroups.get(i), dashboardReportMapForOutputGrp, csAssert);
								}
							}
						}
					}
				} else {
					logger.error("Dashboard Data response is not valid json for chartId {}", chartId);
					dashboardReportMap.put("TestStatus", "failed");
					dumpResultsIntoCSV(dashboardReportMap);
					csAssert.assertTrue(false, "Dashboard Data response is not valid json for chartId =" + chartId);
				}
			} else {
				logger.info("Since ChartId is not provided in config file. Validation skipped for chartId : {}", chartId);
				throw new SkipException("Since ChartId is not provided in config file. Hence Validation skipped for chartId : {}" + chartId);
			}
		} catch (SkipException e) {
			//Test Case Skipped
			logger.warn("chart id not present in  config file. Hence skipped.");

		} catch (Exception e) {
			//Test Case Failed
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "TestDashboard Exception\n" + errors.toString());
			dashboardReportMap.put("TestStatus", "Failed[Exception occurred in test Dashboard method]");
			dumpResultsIntoCSV(dashboardReportMap);
		}
		logger.info("Validation Completed for chart Id {}", chartId);
		csAssert.assertAll();
	}

	@Test(dataProvider = "getChartIdsFromConfigFile", enabled = true, groups = {"smoke", "regression"})
	public void testDashboardGlobalFilters(Integer chartId) {
		CustomAssert csAssert = new CustomAssert();
		/*
		 * Map to store the dashboard Global Filter report attributes.Key = chartId,isMetaDataFilterDataResponseValidJson,filterName,optionId,optionName,
		 * dashboardDataRequestPayload,isDashboardDataResponseValidJson,testStatus
		 * */
		Map<String, String> dashboardGlobalFilterReportMap = new HashMap<String, String>();
		dashboardGlobalFilterReportMap.put("TestType", "TestDashboardGlobalFilters");

		try {
			//if (TestDashboard.newFrameworkChartsId.contains(chartId) || TestDashboard.oldFrameworkChartsId.contains(chartId)) {
			logger.info("********** Validating Global Filters for Chart Id {} ************", chartId);
			dashboardGlobalFilterReportMap.put("ChartId", chartId.toString());

			//getting metaData response for dashboard filters
			ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
			metaDataObj.hitListRendererDefaultUserListMetadata(dashboardListId);
			String metaDataResponseStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

			//getting filterData response for dashboard global filters
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			String filterDataPayload = getFilterDataPayloadForDashboardGlobalFilters();
			filterDataObj.hitListRendererFilterData(dashboardListId, filterDataPayload);
			String filterDataResponseStr = filterDataObj.getListRendererFilterDataJsonStr();

			//validating metaData and FilterData API response
			Boolean isMetaDataValidJson = APIUtils.validJsonResponse(metaDataResponseStr, "[metaData API response for dashboard]");
			Boolean isFilterDataValidJson = APIUtils.validJsonResponse(filterDataResponseStr, "[FilterData response for Global filters]");

			dashboardGlobalFilterReportMap.put("isMetaDataResponseValidJson", isMetaDataValidJson.toString());
			dashboardGlobalFilterReportMap.put("isFilterDataResponseValidJson", isFilterDataValidJson.toString());

			if (isMetaDataValidJson && isFilterDataValidJson) {
				validateDashboardChartsOnGlobalFilters(chartId, filterDataResponseStr, metaDataResponseStr, dashboardGlobalFilterReportMap, csAssert);
			} else {
				logger.error("metaData or Filter Data API response for dashboard is not valid json for chartId {}", chartId);

				dashboardGlobalFilterReportMap.put("TestStatus", "failed");
				dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
				csAssert.assertTrue(false, "metaData or Filter Data API response for dashboard is not valid json for chartId " + chartId);
				//}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "TestDashboard Global filter Exception\n" + e.getStackTrace());
			logger.error("Exception occurred while testing global filters on dashboard for chartId {}", chartId);

			dashboardGlobalFilterReportMap.put("TestStatus", "failed[Exception]");
			dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "getAllChartIds", enabled = false)
	public void testBugSIR_100152_dashboardCaptionShowingNull(Integer chartId) {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> dashboardReportMap = new HashMap<String, String>();
		dashboardReportMap.put("TestType", "TestProductionBug_CaptionShowingNull");
		try {
			if (TestDashboard.newFrameworkChartsId.contains(chartId) || TestDashboard.oldFrameworkChartsId.contains(chartId)) {
				logger.info("Validating BugSIR_100152_dashboardCaptionShowingNull for chartId = {}", chartId);
				dashboardReportMap.put("ChartID", chartId.toString());
				DashboardData dataObj = new DashboardData();
				String dashboardDataPayload = PayloadUtils.getPayloadForDashboardData(chartId);
				dashboardReportMap.put("DashboardDataRequestPayload", dashboardDataPayload);
				dataObj.hitDashboardData(dashboardDataPayload);
				Boolean isValidJson = APIUtils.validJsonResponse(dataObj.getDashboardDataJsonStr(), "[dashboardData response]");
				dashboardReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString());
				if (isValidJson) {
					if (dataObj.getDashboardDataJsonStr().equals("[]") || dataObj.getDashboardDataJsonStr().equals("{}")) {
						logger.warn("DashboardData response for chartId {} is empty [response = {}]. Hence skipping further test for this chart", chartId,
								dataObj.getDashboardDataJsonStr());
						dashboardReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString() + "[empty response]");
						dumpResultsIntoCSV(dashboardReportMap);
					} else {
						String basicPayload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":[],\"userSpecificView\":false,\"drillDown\":false}";
						String initialAnalysisJsonStr = setInitialAnalysisJsonStr(chartId);
						Boolean isDashboardAnalysisValidJson = APIUtils.validJsonResponse(initialAnalysisJsonStr, "dashboardAnalysis response");

						dashboardReportMap.put("DashboardAnalysisRequestPayload", basicPayload);
						dashboardReportMap.put("isDashboardAnalysisResponseValidJson", isDashboardAnalysisValidJson.toString());

						if (isDashboardAnalysisValidJson) {
							Boolean bugStatus = testBugSIR_100152_dashboardCaptionShowingNull(initialAnalysisJsonStr);
							if (bugStatus) {
								logger.error("BugSIR_100152_dashboardCaptionShowingNull still exist for chart id {}", chartId);
								csAssert.assertTrue(false, "BugSIR_100152_dashboardCaptionShowingNull still exist for chartId " + chartId);
								dashboardReportMap.put("TestStatus", "failed[BugSIR_100152 persist]");
								dumpResultsIntoCSV(dashboardReportMap);
							} else {
								logger.info("BugSIR_100152_dashboardCaptionShowingNull does not exist for chartId {}", chartId);
								dashboardReportMap.put("TestStatus", "passed");
								dumpResultsIntoCSV(dashboardReportMap);
							}
						}
					}
				} else {
					logger.error("Dashboard Data response is not valid json for chartId {}", chartId);
					dashboardReportMap.put("TestStatus", "failed");
					dumpResultsIntoCSV(dashboardReportMap);
					csAssert.assertTrue(false, "Dashboard Data response is not valid json for chartId =" + chartId);
				}
				logger.info("Validation Completed for chart Id {}", chartId);
			} else {
				logger.info("Validation for BugSIR_100152_dashboardCaptionShowingNull is skipped for chartId {}", chartId);
			}
		} catch (Exception e) {
			//Test Case Failed
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "TestDashboard Exception\n" + errors.toString());
			dashboardReportMap.put("TestStatus", "Failed[Exception occurred in test Dashboard method]");
			dumpResultsIntoCSV(dashboardReportMap);
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "getAllChartIds", enabled = false)
	public void testDashboardChartsWithDependentFilters(Integer chartId) {
		CustomAssert csAssert = new CustomAssert();

		try {
			List<String> chartsWithDependentFiltersList = Arrays.asList(chartsWithDependentFilters);
			if (chartsWithDependentFiltersList.contains(chartId.toString())) {
				logger.info("validating dependent filters on chart id = {}", chartId.toString());
				DashboardData dataObj = new DashboardData();
				String dashboardDataPayload = PayloadUtils.getPayloadForDashboardData(chartId);
				dataObj.hitDashboardData(dashboardDataPayload);
				String dashboardDataJsonStr = dataObj.getDashboardDataJsonStr();
				Boolean isValidJson = APIUtils.validJsonResponse(dashboardDataJsonStr, "[dashboardData response]");

				if (isValidJson) {
					if (dataObj.getDashboardDataJsonStr().equals("[]") || dataObj.getDashboardDataJsonStr().equals("{}")) {
						logger.warn("DashboardData response for chartId {} is empty [response = {}]. Hence skipping further test for this chart", chartId,
								dataObj.getDashboardDataJsonStr());
					} else {
						JSONArray jsonResponseArray = new JSONArray(dashboardDataJsonStr);
						JSONObject dependentAttributeObj = jsonResponseArray.getJSONObject(0).getJSONObject("dependentAttributes");
						String dependentAttributes[] = JSONObject.getNames(dependentAttributeObj);
						Map<String, String> attributeParentChildMap = new HashMap<>();
						Map<String, List<String>> attributeNameAndValueListMap = new HashMap<>();

						for (int i = 0; i < dependentAttributeObj.length(); i++) {
							String parent = dependentAttributeObj.getJSONObject(dependentAttributes[i]).getString("parent");
							String name = dependentAttributeObj.getJSONObject(dependentAttributes[i]).getString("name");
							JSONArray valueArray = dependentAttributeObj.getJSONObject(dependentAttributes[i]).getJSONArray("values");
							List<String> valuesList = new ArrayList<>();
							for (int j = 0; j < valueArray.length(); j++)
								valuesList.add(valueArray.getString(j));

							attributeNameAndValueListMap.put(name, valuesList);
							attributeParentChildMap.put(parent, name);
						}
						String rootNode = attributeParentChildMap.get("");
						String payload = getDependentFilterPayload(chartId, rootNode);

						DashboardDependentFilters dependentFiltersObj = new DashboardDependentFilters();
						dependentFiltersObj.hitDashboardDependentFilters(payload);
						String dependentFilterJsonStr = dependentFiltersObj.getDashboardDependentFiltersJsonStr();
						Boolean isResponseValidJson = APIUtils.validJsonResponse(dependentFilterJsonStr, "[dependentFilters response for currentFilterKey =" + rootNode + "]");
						if (isResponseValidJson) {
							logger.info("dependentFilters response is valid json for filterKey {}", rootNode);

							String parentNode = rootNode;
							Boolean isDependentFilterResponseValidJason = true;
							Map<String, List<String>> parentAttributeNameValueMap = new LinkedHashMap<>();
							parentAttributeNameValueMap.put(rootNode, attributeNameAndValueListMap.get(rootNode));
							for (int i = 0; i < attributeParentChildMap.size() - 1; i++) {
								String childNode = attributeParentChildMap.get(parentNode);
								String childNodePayload = getDependentFilterPayload(chartId, childNode, parentAttributeNameValueMap);

								DashboardDependentFilters dependentFiltersObject = new DashboardDependentFilters();
								dependentFiltersObject.hitDashboardDependentFilters(childNodePayload);
								String dependentFilterResponseStr = dependentFiltersObject.getDashboardDependentFiltersJsonStr();
								Boolean isValidJsn = APIUtils.validJsonResponse(dependentFilterResponseStr, "[dependentFilter response for currentFilter " + childNode + "]");
								List<String> attributeValueList = new ArrayList<>();
								if (!isValidJsn) {
									isDependentFilterResponseValidJason = false;
									logger.error("dependent Filters api response is not valid json for current filter key {}", childNode);
									csAssert.assertTrue(false, "dependent Filters api response is not valid json for current filter key = " + childNode);
								} else {
									JSONObject resJsonObj = new JSONObject(dependentFilterResponseStr);
									JSONObject dependentAttr = resJsonObj.getJSONObject("dependentAttributes");
									String attributeName[] = JSONObject.getNames(dependentAttr);
									JSONArray attributeValues = dependentAttr.getJSONObject(attributeName[0]).getJSONArray("values");
									for (int j = 0; j < attributeValues.length(); j++) {
										attributeValueList.add(attributeValues.getString(j));
									}
								}
								parentNode = childNode;
								parentAttributeNameValueMap.put(parentNode, attributeValueList);
							}
							if (isDependentFilterResponseValidJason) {
								logger.info("Hitting dashboard analysis api with dependent filters.");
								String analysisPayload = getDashboardAnalysisPayloadWithDependentFilters(chartId, parentAttributeNameValueMap);
								DashboardAnalysis analysisObj = new DashboardAnalysis();
								analysisObj.hitDashboardAnalysis(analysisPayload);
								String analysisResponseStr = analysisObj.getDashboardAnalysisJsonStr();
								Boolean isAnalysisResValidJson = APIUtils.validJsonResponse(analysisResponseStr, "[Dashboard analysis response with dependent filters]");
								if (!isAnalysisResValidJson) {
									logger.error("Dashboard analysis response is not valid json while hitting with dependent Filters.");
									csAssert.assertTrue(false, "Dashboard analysis response is not valid json while hitting with dependent Filters.");
								}
							}
						} else {
							csAssert.assertTrue(false, "dashboard dependent filters api response is not valid json for root filter = {}" + rootNode);
							logger.error("dashboard dependent filters api response is not valid json for root filter = {}", rootNode);
						}
					}
				} else {
					csAssert.assertTrue(false, "dashboard data is not valid json for chartId {}" + chartId);
					logger.error("dashboard data is not valid json for chartId {}", chartId.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while validating dependent filters {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while validating dependent filters " + e.getMessage());
		}
	}

	private String getDashboardAnalysisPayloadWithDependentFilters(Integer chartId, Map<String, List<String>> parentAttributeNameValueMap) {
		String payload = null;
		String filterParam = "[";

		for (Map.Entry<String, List<String>> entryMap : parentAttributeNameValueMap.entrySet()) {
			List<String> attributeValues = entryMap.getValue();
			int randomValue[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, attributeValues.size() - 1, 1);   //dependent filters support Single selected values only

			filterParam += "{\"key\":" + entryMap.getKey() + ",\"values\":\"" + attributeValues.get(randomValue[0]) + "\"},";
		}
		filterParam = filterParam.trim().substring(0, filterParam.length() - 1); // removing last comma[,]

		payload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":" + filterParam + "],\"userSpecificView\":false,\"newFilterApplied\":true}";

		return payload;
	}

	private String getDependentFilterPayload(Integer chartId, String currentFilterKey) {
		return this.getDependentFilterPayload(chartId, currentFilterKey, null);
	}

	private String getDependentFilterPayload(Integer chartId, String currentFilterKey, Map<String, List<String>> filterParamMap) {
		String payload = null;
		String filterParam = "[";

		if (filterParamMap == null)
			payload = "{\"filterParam\":[],\"currentFilterKey\":\"" + currentFilterKey + "\",\"chartId\":\"" + chartId.toString() + "\"}";
		else {
			for (Map.Entry<String, List<String>> entryMap : filterParamMap.entrySet()) {
				List<String> attributeValues = entryMap.getValue();
				int randomValue[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, attributeValues.size() - 1, 1);   //dependent filters support Single selected values only

				filterParam += "{\"key\":" + entryMap.getKey() + ",\"values\":\"" + attributeValues.get(randomValue[0]) + "\"},";
			}
			filterParam = filterParam.trim().substring(0, filterParam.length() - 1); // removing last comma[,]
			payload = "{\"filterParam\":" + filterParam + "],\"currentFilterKey\":\"" + currentFilterKey + "\",\"chartId\":\"" + chartId.toString() + "\"}";
		}

		return payload;
	}

	/*chartFramework is a special keyword that decides how to Create payload for Records Api.
	Possible options (case-insensitive) are:    New, Old
	 */
	private void verifyDashboardCharts(Integer chartId, String chartFramework, String currentOutputGroup, Map<String, String> dashboardReportMap, CustomAssert csAssert) {


		ExecutorService executorOuter = Executors.newFixedThreadPool(1);
		List<FutureTask<Boolean>> taskListOuter = new ArrayList<FutureTask<Boolean>>();

		try {
			String dashboardLocalFiltersResponseStr;
			//List<String> chartsListWithLocalFilterApiCall = Arrays.asList(chartsWithLocalFilterApi);

			String initialAnalysisJsonStr = setInitialAnalysisJsonStr(chartId);
			JSONObject dashboardAnalysisRes = new JSONObject(initialAnalysisJsonStr);
			if (dashboardAnalysisRes.has("newLocalFilterCall") && dashboardAnalysisRes.getBoolean("newLocalFilterCall")) {

				//Charts with new framework where local filters are fetched from dashboard local filters api response
				String dashboardLocalFiltersPayload = "{\"chartId\":" + chartId + "}";
				DashboardLocalFilters dashboardLocalFiltersObj = new DashboardLocalFilters();
				dashboardLocalFiltersObj.hitDashboardLocalFilters(dashboardLocalFiltersPayload);
				String dashboardLocalFiltersRes = dashboardLocalFiltersObj.getDashboardLocalFiltersJsonStr();
				dashboardLocalFiltersResponseStr = dashboardLocalFiltersRes;
			} else {
				//Charts with old framework where local filters are fetched from dashboard analysis api response
				dashboardLocalFiltersResponseStr = initialAnalysisJsonStr;
			}

			logger.debug("dashboardLocalFiltersResponseStr = {}", dashboardLocalFiltersResponseStr);
			if (APIUtils.validJsonResponse(dashboardLocalFiltersResponseStr, "[dashboardLocalFiltersResponseStr]")) {
				Map<String, String> recordsPageFieldsMapTemp = new HashMap<>();
				if (validateRecordsData)
					recordsPageFieldsMapTemp = setRecordsPageFieldsMapping(chartId, dashboardLocalFiltersResponseStr);

				Map<String, String> recordsPageFieldsMap = new HashMap<>(recordsPageFieldsMapTemp);
				logger.info("Getting all Attributes to be Validated for Chart Id: {}", chartId);
				List<String> allAttributes = this.getAllAttributesToTest(chartId, dashboardLocalFiltersResponseStr);

				logger.info("Total Attributes to be Validated for Chart Id {}: {}", chartId, allAttributes.size());
				for (int i = 0; i < allAttributes.size(); i++) {
					Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
					final int outerIndex = i;

					FutureTask<Boolean> resultOuter = new FutureTask<Boolean>(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {

							if (!allAttributes.get(outerIndex).trim().toLowerCase().contains("stakeholder")) {
								verifyNonStakeholderFields(dashboardLocalFiltersResponseStr, allAttributes.get(outerIndex), chartId, chartFramework, currentOutputGroup,
										dashboardReportMapLocal, recordsPageFieldsMap, csAssert);
							} else {
								verifyStakeholderField(dashboardLocalFiltersResponseStr, allAttributes.get(outerIndex), chartId, chartFramework, currentOutputGroup,
										dashboardReportMapLocal, recordsPageFieldsMap, csAssert);
							}
							return true;
						}
					});
					taskListOuter.add(resultOuter);
					executorOuter.execute(resultOuter);
				}
				for (int m = 0; m < taskListOuter.size(); m++)
					taskListOuter.get(m).get();
			} else {
				logger.error("API response containing local filters is not valid json for chart Id {}", chartId);
				csAssert.assertTrue(false, "Dashboard Analysis response[initial response without any filter] is not valid json for chart Id = " + chartId);
				Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
				dashboardReportMapLocal.put("isDashboardAnalysisResponseValidJson", "False[initial response without applying any filter]");
				dashboardReportMapLocal.put("TestStatus", "failed");
				dumpResultsIntoCSV(dashboardReportMapLocal);
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "TestDashboard Exception while Verifying Chart having Id: " + chartId + " and outputGroup : " +
					currentOutputGroup + "\n" + errors.toString());
			executorOuter.shutdownNow();
		}
	}

	private void verifyNonStakeholderFields(String dashboardLocalFiltersResponseStr, String attribute, int chartId, String chartFramework, String currentOutputGroup,
	                                        Map<String, String> dashboardReportMap, Map<String, String> recordsPageFieldsMap, CustomAssert csAssert)
			throws ExecutionException, InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		List<FutureTask<Boolean>> taskList = new ArrayList<FutureTask<Boolean>>();
		try {
			logger.info("Getting Filter Data Options of Attribute {} for Chart Id {}", attribute, chartId);
			dashboardReportMap.put("LocalFilterName", attribute);
			List<String> options = this.getOptionsForAttribute(chartId, attribute, dashboardLocalFiltersResponseStr);

			logger.info("Total Options to be Validated for Attribute {} and Chart Id {}: {}", attribute, chartId, options.size());
			for (int i = 0; i < options.size(); i++) {
				final int innerIndex = i;
				Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);

				FutureTask<Boolean> result = new FutureTask<Boolean>(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						Map<String, String> params = new HashMap<String, String>();
						params.put(attribute, options.get(innerIndex));

						dashboardReportMapLocal.put("LocalFilterOption", options.get(innerIndex));
						logger.info("Creating Payload of Dashboard Analysis for Option #{} of Attribute {} having Value {} and Chart Id {}", (innerIndex + 1), attribute,
								options.get(innerIndex), chartId);
						String payloadForDashboardAnalysis = PayloadUtils.getPayloadForDashboardAnalysis(chartId, params, dashboardLocalFiltersResponseStr, currentOutputGroup);
						dashboardReportMapLocal.put("DashboardAnalysisRequestPayload", payloadForDashboardAnalysis);
						logger.info("Hitting dashboard analysis api for Option #{} having filter {} --> {} ", (innerIndex + 1), attribute, options.get(innerIndex));
						DashboardAnalysis analysisObj = new DashboardAnalysis();
						analysisObj.hitDashboardAnalysis(payloadForDashboardAnalysis);
						String analysisJsonStrFilterApplied = analysisObj.getDashboardAnalysisJsonStr();

						Boolean isValidJson = APIUtils.validJsonResponse(analysisJsonStrFilterApplied, "dashboard analysis response");
						dashboardReportMapLocal.put("isDashboardAnalysisResponseValidJson", isValidJson.toString());
						if (isValidJson) {
							if (validateRecordsApi) {
								logger.info("validating dashboard records api response for Option #{} having filter {} --> {}", (innerIndex + 1), attribute,
										options.get(innerIndex));
								validateDashboardRecords(analysisJsonStrFilterApplied, chartId, params, chartFramework, attribute, options.get(innerIndex),
										dashboardReportMapLocal, dashboardLocalFiltersResponseStr, recordsPageFieldsMap, csAssert);
							} else
								logger.warn("validateRecordsApi flag is false. Hence Skipping validation of dashboard records api.");
						} else {
							logger.error("dashboard analysis response is not valid json for Attribute {} having Value {} and chartId {}. ", attribute, options.get(innerIndex),
									chartId);
							csAssert.assertTrue(false, "dashboard analysis response is not valid json for Attribute " + attribute + " having Value " +
									options.get(innerIndex) + "  ChartId " + chartId + " and outputGroup : " + currentOutputGroup);
							dashboardReportMapLocal.put("TestStatus", "failed");
							dumpResultsIntoCSV(dashboardReportMapLocal);
						}
						return true;
					}
				});
				taskList.add(result);
				executor.execute(result);
			}
			for (int j = 0; j < taskList.size(); j++)
				taskList.get(j).get();
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while Verifying Non-Stakeholder Field " + attribute + " of Chart Id: " + chartId + " and outputGroup :"
					+ currentOutputGroup + "\n" + errors.toString());
			executor.shutdownNow();
		}
	}

	private void verifyStakeholderField(String dashboardLocalFiltersResponseStr, String attribute, int chartId, String chartFramework, String currentOutputGroup,
	                                    Map<String, String> dashboardReportMap, Map<String, String> recordsPageFieldsMap, CustomAssert csAssert)
			throws ExecutionException, InterruptedException, ConfigurationException {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		List<FutureTask<Boolean>> taskList = new ArrayList<FutureTask<Boolean>>();
		boolean specialFilter;

		try {
			List<Map<String, String>> roleGroups = new ArrayList<Map<String, String>>();
			String stakeholderObjectName = DashboardLocalFilters.getStakeholderObjectName(dashboardLocalFiltersResponseStr);
			String delimiterForRoleGroups = ParseConfigFile.getValueFromConfigFile(dashboardConfigFilePath, dashboardConfigFileName, "delimiterforrolegroups");
			String delimiterForOwners = ParseConfigFile.getValueFromConfigFile(dashboardConfigFilePath, dashboardConfigFileName, "delimiterforowners");

			if (isSpecialFilter(chartId, attribute)) {
				specialFilter = true;
				logger.info("{} is a Special Filter. ", attribute);

				String stakeholders[] = ParseConfigFile.getValueFromConfigFileCaseSensitive(dashboardConfigFilePath, dashboardConfigFileName, Integer.toString(chartId),
						attribute).split(Pattern.quote(delimiterForValues));

				for (String stakeholder : stakeholders) {
					String roleGroup[] = stakeholder.trim().split(Pattern.quote(delimiterForRoleGroups));
					Map<String, String> roleGroupData = DashboardLocalFilters.getRoleGroupDataFromLabel(dashboardLocalFiltersResponseStr, stakeholderObjectName, roleGroup[0].trim());
					roleGroups.add(roleGroupData);
				}
			} else {
				specialFilter = false;
				logger.info("Getting RoleGroups Data of Stakeholder for Chart Id {}", chartId);
				roleGroups = DashboardLocalFilters.getRoleGroupsForStakeholder(dashboardLocalFiltersResponseStr, stakeholderObjectName);
			}

			logger.info("Total RoleGroups for Attribute {} of Chart Id {} are: {}", attribute, chartId, roleGroups.size());
			dashboardReportMap.put("LocalFilterName", attribute);
			Boolean isOptionsAvailableForStakeholder = false;

			for (int i = 0; i < roleGroups.size(); i++) {
				final int outerIndex = i;
				int roleGroupId = -1;
				List<Map<String, String>> allOwners = new ArrayList<Map<String, String>>();

				if (specialFilter) {
					String stakeholders[] = ParseConfigFile.getValueFromConfigFileCaseSensitive(dashboardConfigFilePath, dashboardConfigFileName, Integer.toString(chartId),
							attribute).split(Pattern.quote(delimiterForValues));
					for (String stakeholder : stakeholders) {
						String roleGroup[] = stakeholder.trim().split(Pattern.quote(delimiterForRoleGroups));
						String ownerNames[] = roleGroup[1].trim().split(Pattern.quote(delimiterForOwners));

						for (String owner : ownerNames) {
							roleGroupId = Integer.parseInt(roleGroups.get(outerIndex).get("id"));
							Map<String, String> ownerData = DashboardLocalFilters.getOwnerDetailsFromLabel(dashboardLocalFiltersResponseStr, roleGroupId, stakeholderObjectName,
									owner.trim());
							allOwners.add(ownerData);
						}
					}
				} else {
					roleGroupId = Integer.parseInt(roleGroups.get(outerIndex).get("id"));
					logger.info("Getting all Options of RoleGroup {} for Stakeholder of Chart Id {}", roleGroupId, chartId);
					List<Map<String, String>> allOptions = DashboardLocalFilters.getOptionsForStakeholderRoleGroup(dashboardLocalFiltersResponseStr, roleGroupId, stakeholderObjectName);
					logger.info("Total Options for RoleGroup {} for Stakeholder of Chart Id {}: {}", roleGroupId, chartId, allOptions.size());
					if (allOptions.size() > 0) {
						int randomUsers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, maxRandomOptions);
						for (int randomUser : randomUsers)
							allOwners.add(allOptions.get(randomUser));
					} else {
						logger.info("No options found for RoleGroup {}. Hence skipping.", roleGroupId);
						continue;
					}
				}

				for (int j = 0; j < allOwners.size(); j++) {
					isOptionsAvailableForStakeholder = true;
					final int innerIndex = j;
					List<Map<String, String>> finalRoleGroups = roleGroups;
					Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);

					FutureTask<Boolean> result = new FutureTask<Boolean>(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							dashboardReportMapLocal.put("LocalFilterOption", " [" + finalRoleGroups.get(outerIndex).get("name") + "] [" +
									allOwners.get(innerIndex).get("name") + "]");
							String payloadForDashboardAnalysis = PayloadUtils.getPayloadForDashboardAnalysis(chartId, finalRoleGroups.get(outerIndex),
									allOwners.get(innerIndex), dashboardLocalFiltersResponseStr, stakeholderObjectName, currentOutputGroup);
							dashboardReportMapLocal.put("DashboardAnalysisRequestPayload", payloadForDashboardAnalysis);
							logger.info("Hitting dashboard analysis api for User #{} of RoleGroup {} for Chart Id {}. payload = {}", (innerIndex + 1),
									finalRoleGroups.get(outerIndex).get("name"), chartId, payloadForDashboardAnalysis);
							DashboardAnalysis analysisObj = new DashboardAnalysis();
							analysisObj.hitDashboardAnalysis(payloadForDashboardAnalysis);
							String analysisJsonStrFilterApplied = analysisObj.getDashboardAnalysisJsonStr();

							Boolean isValidJson = APIUtils.validJsonResponse(analysisJsonStrFilterApplied, "[dashboard analysis response]");
							dashboardReportMapLocal.put("isDashboardAnalysisResponseValidJson", isValidJson.toString());
							if (!isValidJson) {
								dashboardReportMapLocal.put("TestStatus", "failed");
								dumpResultsIntoCSV(dashboardReportMapLocal);
							} else {
								if (validateRecordsApi) {
									logger.info("validating dashboard records api response for User #{} of RoleGroup {} for Chart Id {}. --> {}", (innerIndex + 1),
											finalRoleGroups.get(outerIndex).get("name"), chartId, allOwners.get(innerIndex));
									validateDashboardRecords(analysisJsonStrFilterApplied, chartId, finalRoleGroups.get(outerIndex), allOwners.get(innerIndex),
											chartFramework, attribute + " -> " + finalRoleGroups.get(outerIndex).get("name"),
											allOwners.get(innerIndex).get("name"), stakeholderObjectName, dashboardReportMapLocal, dashboardLocalFiltersResponseStr, recordsPageFieldsMap, csAssert);
								} else
									logger.warn("validateRecordsApi flag is false. Hence Skipping validation of dashboard records api.");
							}
							return true;
						}
					});
					taskList.add(result);
					executor.execute(result);
				}
				for (int k = 0; k < taskList.size(); k++)
					taskList.get(k).get();
			}
			if (!isOptionsAvailableForStakeholder) {
				logger.warn("No options found for Attribute = Stakeholder.");
				dashboardReportMap.put("TestStatus", "passed");
				dumpResultsIntoCSV(dashboardReportMap);
			}
		} catch (Exception e) {
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "Exception while Verifying Stakeholder Fields of Chart Id: " + chartId + " and outputGroup : " + currentOutputGroup
					+ "\n" + errors.toString());
			executor.shutdownNow();
		}
	}

	private void validateDashboardRecords(String analysisJsonStr, int chartId, Map<String, String> params, String chartFramework, String attributeName, String attributeValue,
	                                      Map<String, String> dashboardReportMap, String dashboardLocalFiltersResponseStr, Map<String, String> recordsPageFieldsMap, CustomAssert csAssert) {
		this.validateDashboardRecords(analysisJsonStr, chartId, params, null, chartFramework, attributeName, attributeValue, null,
				dashboardReportMap, dashboardLocalFiltersResponseStr, recordsPageFieldsMap, csAssert);
	}

	private void validateDashboardRecords(String analysisJsonStr, int chartId, Map<String, String> params, Map<String, String> childParam, String chartFramework,
	                                      String attributeName, String attributeValue, String stakeholderObjectName, Map<String, String> dashboardReportMap,
	                                      String dashboardLocalFiltersResponseStr, Map<String, String> recordsPageFieldsMap, CustomAssert csAssert) {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		List<FutureTask<Boolean>> taskList = new ArrayList<FutureTask<Boolean>>();

		try {
			List<String> allLinks = this.getAllLinks(analysisJsonStr);

			if (allLinks.size() > 0) {
				logger.info("Total Links for Attribute {} having Value {} and Chart Id {} = {}", attributeName, attributeValue, chartId, allLinks.size());
				for (int k = 0; k < allLinks.size(); k++) {
					final int index = k;
					Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMap);
					String link = allLinks.get(index);
					FutureTask<Boolean> result = new FutureTask<Boolean>(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							String payload = null;
							DashboardRecords recordsObj = new DashboardRecords();

							//Process only those links whose length is > 5. Otherwise link would be treated as Not sufficient
							if (link.length() > 5) {
								dashboardReportMapLocal.put("Link", link);
								if (childParam == null)
									payload = PayloadUtils.getPayloadForDashboardRecords(chartId, params, dashboardLocalFiltersResponseStr, link, offset, size, chartFramework, userRequestId);
								else
									// For Stakeholder Filter
									payload = PayloadUtils.getPayloadForDashboardRecords(chartId, params, childParam, dashboardLocalFiltersResponseStr, link, offset, size,
											chartFramework, stakeholderObjectName, userRequestId);

								dashboardReportMapLocal.put("DashboardRecordsRequestPayload", payload);
								recordsObj.hitDashboardRecords(payload);
								String dashboardRecordJsonStr = recordsObj.getDashboardRecordsJsonStr();
								/*logger.info("dashboard record API response for Link #{} of Attribute {} having Value {},  and chartId = {} response = {}", (index + 1),
										attributeName, attributeValue, chartId, dashboardRecordJsonStr);*/

								//Validate that Records Response is Valid Json.
								Boolean validJsonResponse = APIUtils.validJsonResponse(dashboardRecordJsonStr, "[dashboardRecords response]");
								dashboardReportMapLocal.put("isDashboardRecordsResponseValidJson", validJsonResponse.toString());
								if (!validJsonResponse)
									dashboardReportMapLocal.put("TestStatus", "failed");
								//pushReportsResultToCSV(dashboardReportMapLocal);
								csAssert.assertTrue(validJsonResponse, "Dashboard Record response is not valid json. Link #" +
										(index + 1) + " Attribute = " + attributeName + ", Value = " + attributeValue + ", chartId = " + chartId + " payload = " + payload);

								if (validJsonResponse && validateRecordsData) {
									if (childParam == null)
										validateDashboardRecordsData(dashboardRecordJsonStr, attributeName, attributeValue, chartId, false,
												dashboardLocalFiltersResponseStr, recordsPageFieldsMap, csAssert, dashboardReportMapLocal);
									else
										validateDashboardRecordsData(dashboardRecordJsonStr, attributeName, attributeValue, chartId, true,
												dashboardLocalFiltersResponseStr, recordsPageFieldsMap, csAssert, dashboardReportMapLocal);
								} else {
									dumpResultsIntoCSV(dashboardReportMapLocal);
								}
							} else {
								logger.warn("Link #{} of Attribute {} having Value {} and Chart Id {} doesn't meet length requirements. Hence skipping Validation",
										(index + 1), attributeName, attributeValue, chartId);
							}
							return true;
						}
					});
					taskList.add(result);
					executor.execute(result);
				}
			} else {
				logger.info("Empty data set in dashboard analysis API response for Attribute {} having Value {} and Chart Id {}, dashboardAnalysis response ={}. " +
						"Hence skipping hitting records api", attributeName, attributeValue, chartId, analysisJsonStr);
				dashboardReportMap.put("TestStatus", "passed");
				dumpResultsIntoCSV(dashboardReportMap);
			}
			for (int n = 0; n < taskList.size(); n++)
				taskList.get(n).get();

		} catch (Exception e) {
			logger.error("Exception while validating records API for Attribute {} having Value {}, link {} and Chart Id {}. {}", attributeName, attributeValue, chartId,
					e.getMessage());
			csAssert.assertTrue(false, "Exception while validating records API for Attribute " + attributeName + " having Value " + attributeValue +
					" and Chart Id " + chartId + ": " + e.getMessage());
			executor.shutdownNow();
		}
	}

	private String setInitialAnalysisJsonStr(int chartId) {
		String initialAnalysisJsonStr = null;
		try {
			String initialPayloadForDashboardAnalysis = PayloadUtils.getPayloadForDashboardAnalysis(chartId);
			logger.info("Hitting Dashboard Analysis Api for Chart Id: {}", chartId);
			DashboardAnalysis analysisObj = new DashboardAnalysis();
			analysisObj.hitDashboardAnalysis(initialPayloadForDashboardAnalysis);
			initialAnalysisJsonStr = analysisObj.getDashboardAnalysisJsonStr();
		} catch (Exception e) {
			logger.error("Exception while getting all Attributes for Chart Id: {}. {}", chartId, e.getMessage());
		}
		return initialAnalysisJsonStr;
	}

	private Map<String, String> setRecordsPageFieldsMapping(int chartId, String dashboardLocalFiltersResponseStr) {
		Map<String, String> recordsPageFieldsMap = new HashMap<String, String>();
		try {
			logger.info("Getting payload for Dashboard Records Api (headers only) for Chart Id: {}", chartId);
			String payloadForRecords = PayloadUtils.getPayloadForDashboardRecords(chartId);
			logger.info("Hitting Dashboard Records Api (headers only) for Chart Id: {}", chartId);
			DashboardRecords recordsObj = new DashboardRecords();
			recordsObj.hitDashboardRecords(true, payloadForRecords);
			String recordsJsonStr = recordsObj.getDashboardRecordsJsonStr();

			logger.info("Getting All Attribute Names for Chart Id: {}", chartId);
			List<String> allAttributeNames = DashboardLocalFilters.getAllAttributeNames(dashboardLocalFiltersResponseStr);

			JSONObject jsonObj = new JSONObject(recordsJsonStr);
			jsonObj = jsonObj.getJSONObject("chartListHeader");

			logger.info("Setting Records Page Fields Mapping for Chart Id: {}", chartId);
			for (String attributeName : allAttributeNames) {
				if (jsonObj.has(attributeName))
					recordsPageFieldsMap.put(attributeName, jsonObj.getString(attributeName));
				else
					recordsPageFieldsMap.put(attributeName, null);
			}

			String stakeholderObjectName = DashboardLocalFilters.getStakeholderObjectName(dashboardLocalFiltersResponseStr);
			if (stakeholderObjectName != null) {
				List<Map<String, String>> allRoleGroups = DashboardLocalFilters.getRoleGroupsForStakeholder(dashboardLocalFiltersResponseStr, stakeholderObjectName);

				for (Map<String, String> roleGroup : allRoleGroups) {
					if (jsonObj.has(roleGroup.get("name")))
						recordsPageFieldsMap.put(roleGroup.get("name"), jsonObj.getString(roleGroup.get("name")));
					else
						recordsPageFieldsMap.put(roleGroup.get("name"), null);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Setting Records Page Fields Mapping for Chart Id: {}. {}", chartId, e.getMessage());
		}
		return recordsPageFieldsMap;
	}

	private void validateDashboardRecordsData(String recordsJsonStr, String attribute, String expectedValue, int chartId, boolean isStakeholderField,
	                                          String dashboardLocalFiltersResponseStr, Map<String, String> recordsPageFieldsMap, CustomAssert csAssert, Map<String, String> dashboardReportMapLocal) {
		ExecutorService executor = Executors.newFixedThreadPool(1);
		List<FutureTask<Boolean>> taskList = new ArrayList<FutureTask<Boolean>>();
		logger.info("Validating Dashboard Records Data of Attribute {} for Chart Id {}", attribute, chartId);

		Map<String, String> dashboardReportMapWithDataValidation = new HashMap<>(dashboardReportMapLocal);
		try {
			logger.info("Getting Attribute Name for Attribute {} and Chart Id {}", attribute, chartId);
			final String attributeName;

			if (isStakeholderField) {
				String attributeNameFields[] = attribute.split(Pattern.quote("->"));
				attributeName = attributeNameFields[1].trim();
				String expectedValueFields[] = expectedValue.split("\\(");
				expectedValue = expectedValueFields[0].trim();
			} else
				attributeName = DashboardLocalFilters.getAttributeNameFromLabel(dashboardLocalFiltersResponseStr, attribute);
			String attributeType = DashboardLocalFilters.getAttributeType(dashboardLocalFiltersResponseStr, attribute);

			JSONObject jsonObj = new JSONObject(recordsJsonStr);
			JSONArray recordsJsonArray = jsonObj.getJSONArray("records");

			logger.info("Total Records found for Filter {} and Chart Id {}: {}", attribute, chartId, recordsJsonArray.length());
			for (int i = 0; i < recordsJsonArray.length(); i++) {
				final int index = i;
				final String finalExpectedValue = expectedValue;

				FutureTask<Boolean> result = new FutureTask<Boolean>(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						String actualValue = null;
						logger.info("Validating Data of Record #{} for Filter {} and ChartId {}", (index + 1), attribute, chartId);
						JSONObject recordJsonObj = new JSONObject(recordsJsonArray.get(index).toString());

						if (recordsPageFieldsMap.get(attributeName) != null) {
							actualValue = recordJsonObj.getString(attributeName);

							if (attributeType.equalsIgnoreCase("slider")) {       //Data validation for Slider type filters
								Double actualVal = Double.parseDouble(actualValue);

								if (minSliderValue == "" && maxSliderValue == "") {
									logger.warn("record data validation for filter {} is skipped due to no empty value provided in min-max values.", attribute);
								} else if (minSliderValue == "" || maxSliderValue == "") {
									if (minSliderValue == "") {
										if (actualVal <= Double.parseDouble(maxSliderValue)) {
											logger.info("record data validation passed for Record #{} and filter {}. Actual value = {} and Expected Value is between null to {} ", (index + 1), attribute, actualVal, maxSliderValue);
										} else {
											logger.error("record data validation failed for Record #{} and filter {}. Actual value = {} and Expected Value is between null to {}", (index + 1), attribute, actualVal, maxSliderValue);
										}
									} else {
										if (actualVal >= Double.parseDouble(minSliderValue)) {
											logger.info("record data validation passed for Record #{} and filter {}. Actual value = {} and Expected Value is between {} to null ", (index + 1), attribute, actualVal, minSliderValue);
										} else {
											logger.error("record data validation failed for Record #{} and filter {}. Actual value = {} and Expected Value is between {} to null ", (index + 1), attribute, actualVal, minSliderValue);
										}
									}
								} else if (actualVal >= Double.parseDouble(minSliderValue) && actualVal <= Double.parseDouble(maxSliderValue)) {
									logger.info("record data validation passed for Record #{} and filter {}. Actual value = {} and Expected Value is between {} to {} ", (index + 1), attribute, actualVal, minSliderValue, maxSliderValue);
								} else {
									logger.error("record data validation failed for Record #{} and filter {}. Actual value = {} and Expected Value is between {} to {}", (index + 1), attribute, actualVal, minSliderValue, maxSliderValue);
									csAssert.assertTrue(false, "record data validation failed for Record #" + (index + 1) + " and filter " + attribute + ". Actual value = " + actualValue + " and Expected Value is between " + minSliderValue + " to " + maxSliderValue);
								}
							} else if (!StringUtils.isNumericRangeValue(actualValue)) {

								csAssert.assertTrue(actualValue.trim().toLowerCase().contains(finalExpectedValue.trim().toLowerCase()), "Dashboard Record Data " +
										"failed " + "for Record #" + (index + 1) + " for Filter " + attribute + " and ChartId " + chartId + " having Actual Value as " +
										actualValue + " and Expected Value as " + finalExpectedValue);
								if (!actualValue.trim().toLowerCase().contains(finalExpectedValue.trim().toLowerCase())) {
									dashboardReportMapWithDataValidation.put("DataValidationStatus", "Failed");
									dashboardReportMapWithDataValidation.put("TestStatus", "Failed");
									dumpResultsIntoCSV(dashboardReportMapWithDataValidation);
								} else {
									dashboardReportMapWithDataValidation.put("DataValidationStatus", "passed");
									dashboardReportMapWithDataValidation.put("TestStatus", "passed");
									dumpResultsIntoCSV(dashboardReportMapWithDataValidation);
								}
							} else {
								csAssert.assertTrue(NumberUtils.verifyNumericRangeValue(actualValue, finalExpectedValue.trim()), "Dashboard Record Data " +
										"failed " + "for Record #" + (index + 1) + " for Filter " + attribute + " and ChartId " + chartId + " having Actual Value as " +
										actualValue + " and Expected Value as " + finalExpectedValue);
								if (!NumberUtils.verifyNumericRangeValue(actualValue, finalExpectedValue.trim())) {
									dashboardReportMapWithDataValidation.put("DataValidationStatus", "Failed");
									dashboardReportMapWithDataValidation.put("TestStatus", "Failed");
									dumpResultsIntoCSV(dashboardReportMapWithDataValidation);
								} else {
									dashboardReportMapWithDataValidation.put("DataValidationStatus", "Failed");
									dashboardReportMapWithDataValidation.put("TestStatus", "Failed");
									dumpResultsIntoCSV(dashboardReportMapWithDataValidation);
								}
							}
						} else {
							logger.info("Dashboard Records Page doesn't have Attribute {}. Hence Validating data on Show Page for Chart Id {}", attribute, chartId);
							int entityTypeId = recordJsonObj.getInt("entityTypeId");
							int id = recordJsonObj.getInt("id");
							verifyRecordsDataOnShowPage(entityTypeId, id, attribute, finalExpectedValue, chartId, csAssert, dashboardReportMapWithDataValidation);
						}
						return true;
					}
				});
				taskList.add(result);
				executor.execute(result);
			}
			for (int j = 0; j < taskList.size(); j++)
				taskList.get(j).get();
		} catch (Exception e) {
			logger.error("Exception while Validating Dashboard Record Data for Filter {} having Expected Value {}. {}", attribute, expectedValue, e.getStackTrace());
			dashboardReportMapWithDataValidation.put("DataValidationStatus", "Failed[Exception Occurred]");
			dashboardReportMapWithDataValidation.put("TestStatus", "Failed");
			dumpResultsIntoCSV(dashboardReportMapWithDataValidation);
			executor.shutdownNow();
		}
	}

	private List<String> getAllLinks(String analysisJsonStr) {
		String linkPath = null;
		List<String> allLinks = new ArrayList<>();
		try {

			JSONObject jsonObject = new JSONObject(analysisJsonStr);
			String chartType = jsonObject.getString("chartType").toLowerCase();
			if (chartTypeMap.containsKey(chartType))
				linkPath = chartTypeMap.get(chartType);
			else
				linkPath = chartTypeMap.get("default");

			if (chartType.equalsIgnoreCase("world8")) {
				String linkString = JsonPath.parse(analysisJsonStr).read(linkPath);
				allLinks.add(linkString);
			} else
				allLinks = JsonPath.parse(analysisJsonStr).read(linkPath);
		} catch (Exception e) {
			logger.error("Exception while getting all Links from Analysis Response. {}", e.getMessage());
		}

		List<String> allLinksWithoutNull = new ArrayList<>();
		for (int i = 0; i < allLinks.size(); i++) {
			if (allLinks.get(i) != null) {
				allLinksWithoutNull.add(allLinks.get(i));
			}
		}
		return allLinksWithoutNull;
	}

	private boolean isSpecialFilter(int chartId, String attribute) throws ConfigurationException {
		boolean specialFilter = false;
		if (!testAllFilters && ParseConfigFile.hasProperty(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName, Integer.toString(chartId), attribute)
				&& !ParseConfigFile.getValueFromConfigFile(dashboardConfigFilePath, dashboardConfigFileName, Integer.toString(chartId),
				attribute).equalsIgnoreCase(""))
			specialFilter = true;
		return specialFilter;
	}

	private List<String> getOptionsForAttribute(int chartId, String attribute, String dashboardLocalFiltersResponseStr) {
		List<String> options = new ArrayList<String>();
		try {

			List<String> filterData = new ArrayList<>();
			String attributeType = DashboardLocalFilters.getAttributeType(dashboardLocalFiltersResponseStr, attribute);
			if (attributeType.equalsIgnoreCase("SLIDER")) {

				String sliderOptionValue = "{\"min\":\"" + minSliderValue + "\",\"max\":\"" + maxSliderValue + "\"}";
				filterData.add(sliderOptionValue);
				return filterData;
			} else {
				filterData = DashboardLocalFilters.getFilterData(dashboardLocalFiltersResponseStr, attribute);
			}
			logger.info("Total Options Available for Attribute {} for Chart Id {}: {}", attribute, chartId, filterData.size());

			if (isSpecialFilter(chartId, attribute)) {
				logger.info("Attribute {} of Chart Id {} is a Special Filter. ", attribute, chartId);
				String value[] = null;
				String filterValue = ParseConfigFile.getValueFromConfigFile(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
						Integer.toString(chartId), attribute);

				if (!filterValue.trim().equalsIgnoreCase("")) {
					value = filterValue.split(Pattern.quote(TestDashboard.delimiterForValues));
					for (String option : value)
						options.add(option.trim());
				}

				if (options.size() == 0) {
					logger.info("Values not provided for Special Filter {} and Chart Id {}. Hence picking all Available options.", attribute, chartId);
					options = filterData;
				}
			} else if (useRandomizationOnLocalFilters) {
				logger.info("Applying Randomization on Attribute {} of Chart Id {}.", attribute, chartId);
				if (filterData.size() > 0) {
					int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filterData.size() - 1, maxRandomOptions);

					for (int i = 0; i < randomNumbers.length; i++)
						options.add(filterData.get(randomNumbers[i]));
				} else {
					logger.warn("No options found for the attribute {}", attribute);
				}
			} else
				options = filterData;

		} catch (Exception e) {
			logger.error("Exception while getting Options for Attribute {} and ChartId {}. {}", attribute, chartId, e.getMessage());
		}
		return options;
	}

	private List<String> getAllAttributesToTest(int chartId, String dashboardLocalFiltersResponseStr) {
		List<String> attributesToTest = new ArrayList<String>();
		try {
			logger.info("Getting all Attributes Available for Chart Id: {}", chartId);
			List<String> allAttributes = DashboardLocalFilters.getAllAttributes(dashboardLocalFiltersResponseStr);

			/*skipping validation for Sl Met attribute. */
			if (allAttributes.contains("Sl Met"))
				allAttributes.remove("Sl Met");

			if (!TestDashboard.testAllFilters) {
				attributesToTest = ParseConfigFile.getAllPropertiesOfSection(TestDashboard.dashboardConfigFilePath, TestDashboard.dashboardConfigFileName,
						Integer.toString(chartId));

				if (attributesToTest.size() == 0)
					attributesToTest = allAttributes;

				else {
					String[] allAttributeLabels = DashboardLocalFilters.getAllAttributeLabels(dashboardLocalFiltersResponseStr);
					for (String attribute : attributesToTest) {
						for (String attributeLabel : allAttributeLabels) {
							if (attribute.equalsIgnoreCase(attributeLabel)) {
								attributesToTest.remove(attribute);
								attributesToTest.add(attributeLabel);
								break;
							}
						}
					}
				}
			} else
				attributesToTest = allAttributes;
		} catch (Exception e) {
			logger.error("Exception while getting All Attributes to Test for ChartId {}. {}", chartId, e.getMessage());
		}
		return attributesToTest;
	}

	private String getFilterDataPayloadForDashboardGlobalFilters() {
		String payload = "{\"pageType\":\"globalFilters\"}";
		return payload;
	}

	private void validateDashboardChartsOnGlobalFilters(Integer chartId, String filterDataResponseStr, String metaDataResponseStr,
	                                                    Map<String, String> dashboardGlobalFilterReportMap, CustomAssert csAssert) {
		int filterCount = 0;
		try {
			JSONObject filterDataJson = new JSONObject(filterDataResponseStr);
			FilterUtils filterObj = new FilterUtils();
			Map<Integer, Map<String, String>> filterMap = filterObj.getFilters(metaDataResponseStr);
			logger.info(" ChartId = {} , Total Filters = {}", chartId, filterMap.size());
			for (Map.Entry<Integer, Map<String, String>> entryMap : filterMap.entrySet()) {
				Map<String, String> allOptionsNameIdMap = new LinkedHashMap<String, String>();
				List<Map<String, String>> autoCompleteOptionList = new ArrayList<Map<String, String>>();
				filterCount++;
				Integer filterId = entryMap.getKey();
				String filterName = entryMap.getValue().get("queryName");
				dashboardGlobalFilterReportMap.put("GlobalFilterName", filterName);
				JSONUtility jsonObjectTillOptions = new JSONUtility(filterDataJson.getJSONObject(filterId.toString()).getJSONObject("multiselectValues").
						getJSONObject("OPTIONS"));
				boolean autoComplete = jsonObjectTillOptions.getBooleanJsonValue("autoComplete");
				if (autoComplete) {
					{
						logger.info("This filter is autocomplete enabled ,getting the Values From Config File. filter = {}", filterName);
						allOptionsNameIdMap = filterObj.getMapsOfNameIDForAutoComplete(filterName, "dashboard");
					}
				} else {
					List<FilterData.DataClass> optionList = filterObj.getFilterDataList(jsonObjectTillOptions, filterId);
					for (int i = 0; i < optionList.size(); i++) {
						String optionId = optionList.get(i).getDataName();
						String optionName = optionList.get(i).getDataValue();
						allOptionsNameIdMap.put(optionId, optionName);
					}
				}
				logger.info("Total options for the filter #{}. {} = {} and chartId = {}", filterCount, filterName, allOptionsNameIdMap.size(), chartId);
				if (!useRandomizationOnGlobalFilters) {
					for (Map.Entry<String, String> entry : allOptionsNameIdMap.entrySet()) {
						String optionId = entry.getKey();
						String optionName = entry.getValue();

						dashboardGlobalFilterReportMap.put("GlobalFilterOption", optionName + "[" + optionId + "]");
						DashboardData dataObj = new DashboardData();
						String dashboardDataPayload = PayloadUtils.getPayloadForDashboardDataWithFilter(chartId, filterName, optionId);
						dashboardGlobalFilterReportMap.put("DashboardDataRequestPayload", dashboardDataPayload);
						logger.debug("Hitting dashboard Data API with Payload {}", dashboardDataPayload);
						dataObj.hitDashboardData(dashboardDataPayload);
						String dashboardDataResponseStr = dataObj.getDashboardDataJsonStr();
						Boolean isValidJson = APIUtils.validJsonResponse(dashboardDataResponseStr, "[Dashboard Data response for Global Filters]");
						dashboardGlobalFilterReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString());
						if (isValidJson) {
							dashboardGlobalFilterReportMap.put("TestStatus", "passed");
							dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
							logger.info("validation passed for chartId ={} filter #{}. {} --> optionId {}(optionName={})", chartId, filterCount, filterName, optionId,
									optionName);
						} else {
							dashboardGlobalFilterReportMap.put("TestStatus", "failed");
							dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
							logger.error("dashboard Data response is not valid json for chartId = {} filter = {} and optionId = {} optionName = {}", chartId, filterName,
									optionId, optionName);
							csAssert.assertTrue(false, "dashboard Data response is not valid json for chartId =" + chartId + " filter = " + filterName
									+ " and optionId(name) =" + optionId + "(" + optionName + ")]");
						}
					}
				} else {
					if (allOptionsNameIdMap.size() > 0) {
						for (Map.Entry<String, String> entry : allOptionsNameIdMap.entrySet()) {
							Map<String, String> tempMap = new HashMap<>();
							tempMap.put(entry.getKey(), entry.getValue());
							autoCompleteOptionList.add(tempMap);
						}
						List<String> randomOptionId = new ArrayList<>();
						List<String> randomOptionName = new ArrayList<>();
						logger.info("Applying Randomization on Filter {} of Chart Id {}.", filterName, chartId);
						int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptionsNameIdMap.size() - 1, maxRandomOptions);
						for (int i = 0; i < randomNumbers.length; i++) {
							Map<String, String> innerMap = autoCompleteOptionList.get(randomNumbers[i]);
							for (Map.Entry<String, String> entry : innerMap.entrySet()) {
								randomOptionId.add(entry.getKey());
								randomOptionName.add(entry.getValue());
							}
						}

						for (int j = 0; j < randomOptionId.size(); j++) {

							dashboardGlobalFilterReportMap.put("GlobalFilterOption", randomOptionName.get(j) + "[" + randomOptionId + "]");
							DashboardData dataObj = new DashboardData();
							String dashboardDataPayload = PayloadUtils.getPayloadForDashboardDataWithFilter(chartId, filterName, randomOptionId.get(j));
							dashboardGlobalFilterReportMap.put("DashboardDataRequestPayload", dashboardDataPayload);
							logger.debug("Hitting dashboard Data API with Payload {}", dashboardDataPayload);
							dataObj.hitDashboardData(dashboardDataPayload);
							String dashboardDataResponseStr = dataObj.getDashboardDataJsonStr();
							Boolean isValidJson = APIUtils.validJsonResponse(dashboardDataResponseStr, "[dashboard Data response for Global filters]");
							dashboardGlobalFilterReportMap.put("isDashboardDataResponseValidJson", isValidJson.toString());
							if (isValidJson) {
								dashboardGlobalFilterReportMap.put("TestStatus", "passed");
								dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
								logger.info("validation passed for chartId ={} filter #{}. {} --> optionId {}(optionName={})", chartId, filterCount, filterName,
										randomOptionId.get(j), randomOptionName.get(j));
							} else {
								dashboardGlobalFilterReportMap.put("TestStatus", "failed");
								dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
								logger.error("dashboard Data response is not valid json for chartId = {} filter = {} and optionId = {} optionName = {}", chartId, filterName,
										randomOptionId.get(j), randomOptionName.get(j));
								csAssert.assertTrue(false, "dashboard Data response is not valid json for chartId =" + chartId + " filter = " + filterName
										+ " and optionId(name) =" + randomOptionId.get(j) + "(" + randomOptionName.get(j) + ")]");
							}
						}
					} else {
						logger.warn("No options found for the Global filter {}", filterName);
					}
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "TestDashboard Global filter Exception\n" + e.getStackTrace());
			logger.error("Exception occurred while validating global filters on dashboard for chartId {}", chartId);
			dashboardGlobalFilterReportMap.put("isDashboardDataResponseValidJson", "FALSE[Exception Occurred]");
			dashboardGlobalFilterReportMap.put("TestStatus", "failed");
			dumpResultsIntoCSV(dashboardGlobalFilterReportMap);
		}
	}

	private void verifyRecordsDataOnShowPage(int entityTypeId, int id, String attributeLabel, String expectedValue, int chartId, CustomAssert csAssert, Map<String, String> dashboardReportMapWithDataValidation) {
		Map<String, String> dashboardReportMapLocal = new HashMap<>(dashboardReportMapWithDataValidation);
		try {
			String fieldNameForShowPage = null;
			logger.info("Getting Dashboard Show Mapping for Attribute {} and Chart {}.", attributeLabel, chartId);
			if (ParseConfigFile.hasProperty(dashboardShowPageConfigFilePath, dashboardShowPageConfigFileName, Integer.toString(chartId), attributeLabel))
				fieldNameForShowPage = ParseConfigFile.getValueFromConfigFile(dashboardShowPageConfigFilePath, dashboardShowPageConfigFileName, Integer.toString(chartId),
						attributeLabel);
			else
				fieldNameForShowPage = ParseConfigFile.getValueFromConfigFile(dashboardShowPageConfigFilePath, dashboardShowPageConfigFileName, attributeLabel);

			if (fieldNameForShowPage != null) {
				Show showObj = new Show();
				logger.info("Hitting Show Api for Attribute {} and Chart Id {} having Expected Value {}, EntityTypeId {} and Id {}", attributeLabel, chartId, expectedValue,
						entityTypeId, id);
				showObj.hitShow(entityTypeId, id);
				String showJsonStr = showObj.getShowJsonStr();

				if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
					logger.error("Invalid Show Json Response for Attribute {} and ChartId {} having ExpectedValue {}, EntityTypeId {}, Id {}", attributeLabel, chartId,
							expectedValue, entityTypeId, id);
					csAssert.assertFalse(true, "Invalid Show Json Response for Attribute " + attributeLabel + "and ChartId " + chartId +
							" having ExpectedValue " + expectedValue + ", EntityTypeId " + entityTypeId + " and Id " + id);
					dashboardReportMapLocal.put("DataValidationStatus", "Failed[show api response is not valid json]");
					dashboardReportMapLocal.put("TestStatus", "Failed");
					dumpResultsIntoCSV(dashboardReportMapLocal);

				} else {
					boolean showFieldPass;
					showFieldPass = showObj.verifyShowField(showJsonStr, fieldNameForShowPage, expectedValue, entityTypeId);
					csAssert.assertTrue(showFieldPass, "Attribute " + attributeLabel + " and ChartId " + chartId + " having ExpectedValue " + expectedValue +
							", EntityTypeId " + entityTypeId + ", Id " + id + " failed on Show Page");
					if (!showFieldPass) {
						dashboardReportMapLocal.put("DataValidationStatus", "Failed");
						dashboardReportMapLocal.put("TestStatus", "Failed");
						dumpResultsIntoCSV(dashboardReportMapLocal);
					} else {
						dashboardReportMapLocal.put("DataValidationStatus", "passed");
						dashboardReportMapLocal.put("TestStatus", "passed");
						dumpResultsIntoCSV(dashboardReportMapLocal);
					}
				}
			} else {
				logger.warn("Mapping not found in Dashboard Show Mapping Config File for Attribute {} and Chart Id {}. Hence Skipping Data Validation on Show Page.",
						attributeLabel, chartId);
				dashboardReportMapLocal.put("DataValidationStatus", "passed[skipped due to mapping not found in dashboardShowMapping file]");
				dashboardReportMapLocal.put("TestStatus", "passed");
				dumpResultsIntoCSV(dashboardReportMapLocal);
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Records Data on Show Page for Attribute {} with ExpectedValue {} and ChartId {}. {}", attributeLabel, expectedValue,
					chartId, e.getStackTrace());
			dashboardReportMapLocal.put("DataValidationStatus", "Failed[Exception]");
			dashboardReportMapLocal.put("TestStatus", "Failed");
			dumpResultsIntoCSV(dashboardReportMapLocal);
		}
	}

	private List<String> getDashboardOutputGroups(String dashboardDataJsonStr) {
		List<String> outputGroups = new ArrayList<>();
		JSONArray dashboardDataResponse = new JSONArray(dashboardDataJsonStr);
		if (dashboardDataResponse.getJSONObject(0).has("outputs")) {
			JSONArray outputs = dashboardDataResponse.getJSONObject(0).getJSONArray("outputs");
			for (int i = 0; i < outputs.length(); i++) {
				outputGroups.add(outputs.get(i).toString());
			}
		}

		return outputGroups;
	}

	private Boolean testBugSIR_100152_dashboardCaptionShowingNull(String dashboardAnalysisResponseStr) {
		Boolean isBugFound = false;
		try {
			JSONObject jsonResponse = new JSONObject(dashboardAnalysisResponseStr);

			if (jsonResponse.has("colorrange")) {
				JSONArray colorArray = jsonResponse.getJSONObject("colorrange").getJSONArray("color");

				for (int i = 0; i < colorArray.length(); i++) {
					String displayVal = colorArray.getJSONObject(i).getString("displayValue");
					if (displayVal.contains("null")) {
						isBugFound = true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating BugSIR_100152_dashboardCaptionShowingNull. {}", e.getMessage());
		}
		return isBugFound;
	}

	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"TestType", "ChartID", "isMetaDataResponseValidJson", "isFilterDataResponseValidJson", "GlobalFilterName", "GlobalFilterOption", "DashboardDataRequestPayload", "isDashboardDataResponseValidJson", "CurrentOutputGroup", "LocalFilterName", "LocalFilterOption", "DashboardAnalysisRequestPayload", "isDashboardAnalysisResponseValidJson", "Link", "DashboardRecordsRequestPayload", "isDashboardRecordsResponseValidJson", "DataValidationStatus", "TestStatus"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}

	private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
		String allColumns[] = {"TestType", "ChartID", "isMetaDataResponseValidJson", "isFilterDataResponseValidJson", "GlobalFilterName", "GlobalFilterOption", "DashboardDataRequestPayload", "isDashboardDataResponseValidJson", "CurrentOutputGroup", "LocalFilterName", "LocalFilterOption", "DashboardAnalysisRequestPayload", "isDashboardAnalysisResponseValidJson", "Link", "DashboardRecordsRequestPayload", "isDashboardRecordsResponseValidJson", "DataValidationStatus", "TestStatus"};
		for (String column : allColumns) {
			if (!resultsMap.containsKey(column))
				resultsMap.put(column, "null");
		}
		dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
	}

	private void loggingOnSeparateEnv(String environment) {
		try {
			String dashboardSpecificEnv = environment;
			oldEnvironmentFileName = ConfigureEnvironment.environment;

			ConfigureEnvironment.configureProperties(dashboardSpecificEnv, true);

			Check checkObj = new Check();
			//Login on different env.
			checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
			Assert.assertTrue(Check.getAuthorization() != null, "Login for Dashboard Suite. Authorization not set by Check Api. Environment = " + dashboardSpecificEnv);

		} catch (Exception e) {
			logger.error("Exception occurred while logging into alternate environment for Dashboard. Error = {}", e.getMessage());
			e.printStackTrace();
		}
	}
}
