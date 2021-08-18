package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.CSVResults;
import com.sirionlabs.utils.csvutils.ReportResultInfoClass;
import com.sirionlabs.utils.csvutils.ResultInfoClass;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author manoj.upreti
 * @owner shiv.ashish
 */
public class TestReportRender extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestReportRender.class);
	ReportRenderListReportJson reportRenderListReportJsonObj;
	ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData;
	ReportRendererFilterData reportRendererFilterData;
	ReportRendererListData reportRendererListData;
	FilterUtils filterUtils;
	int filterCountToTestForSmoke = 1; // eventually we can override this from config file by default it will be 1 for cut down smoke suit time for Sandbox and Prod Environment

	CSVResults oCSVResults;

	String reportRendererConfigFilePath;
	String reportRendererConfigFileName;
	String reportRenderCSVDelemeter;
	String reportRenderCSVFile;
	List<String> allEntitySection;

	String baseFilePath;
	String entityIdMappingFileName;
	String dateFormat;

	String ReportsResultFileName;

	Map<String, Map<String, String>> reportsMap;

	String testFiltersRandomData;
	String filtersRandomDataSize;


	List<String> entitiesTOBeTested = new ArrayList<>();

	List<String> skipReportIdsList = new ArrayList<>();


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getReportRenderConfigData();
		testCasesMap = getTestCasesMapping();
	}

	public void getReportRenderConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Initializing Test Data for Report Render");
		reportRenderListReportJsonObj = new ReportRenderListReportJson();

		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

		reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
		reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");

		allEntitySection = ParseConfigFile.getAllSectionNames(reportRendererConfigFilePath, reportRendererConfigFileName);

		reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
		reportRendererFilterData = new ReportRendererFilterData();
		reportRendererListData = new ReportRendererListData();


		filterUtils = new FilterUtils();
		reportRenderCSVFile = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVFile");
		reportRenderCSVDelemeter = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVDelimiter");
		dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");


		reportRenderListReportJsonObj.hitReportRender();
		String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
		logger.debug("The Report Response for reportRenderListReportJson API is : [ {} ]", reportRenderJsonStr);
		JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
		reportsMap = reportRenderListReportJsonObj.generateReportsMap(reportJsonArrayForEntities);


		ReportsResultFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderResultsCSVFile");
		oCSVResults = new CSVResults();
		ResultInfoClass resultInfoClass = new ReportResultInfoClass();
		oCSVResults.initializeResultCsvFile(ReportsResultFileName, resultInfoClass);

		testFiltersRandomData = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "testrandondata");
		filtersRandomDataSize = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "randomcount");

		String skipReportIds = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "skipreportids");
		skipReportIdsList = Arrays.asList(skipReportIds.trim().split(Pattern.quote(",")));


		if (ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "filtercounttotestforsmoke") != null) {
			filterCountToTestForSmoke = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "filtercounttotestforsmoke"));
		}

	}


	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.debug("In Before Method");
		logger.debug("method name is: {} ", method.getName());
		logger.debug("***********************************************************************************************************************");

	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
//	@DataProvider(name = "TestReportRendererData", parallel = false)
//	public Object[][] getTestReportRendererData(ITestContext c) throws ConfigurationException {
//
//		logger.info("In the Data Provider");
//
//		String testableReports = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "testreportsforallentitiesallreports");
//		if (testableReports != null && testableReports.equalsIgnoreCase("true")) {
//			return getDefaultReportsList();
//		} else {
//
//			int i = 0;
//			Object[][] groupArray;
//
//			String testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "testreportsforallentities");
//
//			if (testReportsForAllEntities == null) // for backward compatibility
//				testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName,"default", "testreportsforentities");
//
//			if (testReportsForAllEntities != null &&  (testReportsForAllEntities.equalsIgnoreCase("true") || testReportsForAllEntities.equalsIgnoreCase("specified"))) {
//				logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
//				groupArray = new Object[allEntitySection.size() - 1][];
//			} else {
//				String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "entitiestotest").split(",");
//				logger.debug("entitiesToTest :{} , entitiesToTest.size() : {}", entitiesToTest, entitiesToTest.length);
//				groupArray = new Object[entitiesToTest.length][];
//				allEntitySection = Arrays.asList(entitiesToTest);
//			}
//
//			for (String entitySection : allEntitySection) {
//				if (entitySection.equalsIgnoreCase("default")) {
//					continue;
//				} else {
//					logger.debug("entitySection :{}", entitySection);
//					Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
//					groupArray[i] = new Object[2];
//					List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection);
//					for (String entitySpecificProperty : allProperties) {
//						if (entitySpecificProperty.contentEquals("test")) {
//							String reportsIds = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection, entitySpecificProperty);
//							logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIds);
//							List<String> reportsIdsArray = Arrays.asList(reportsIds.split(","));
//							groupArray[i][0] = entityTypeId;
//							groupArray[i][1] = reportsIdsArray;
//						}
//					}
//					i++;
//				}
//			}
//			logger.info("Formed the paramter Array to be tested");
//			return groupArray;
//
//
//		}
//	}

//	@Test(dataProvider = "TestReportRendererData")
//	public void testReportRenderData(Integer entityTypeId, List<String> reportsIds) {
	@Test()
	public void testReportRenderData() {
		CustomAssert csAssert = new CustomAssert();
		//Getting dataprovider details
		logger.info("Getting dataprovider details");
		Object[][] groupArray;
		Integer entityTypeId;
		ArrayList<String> reportsIds;
		try {
			String testableReports = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "testreportsforallentitiesallreports");
			if (testableReports != null && testableReports.equalsIgnoreCase("true")) {
				groupArray = getDefaultReportsList();
			} else {

				int i = 0;


				String testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "testreportsforallentities");

				if (testReportsForAllEntities == null) // for backward compatibility
					testReportsForAllEntities = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "testreportsforentities");

				if (testReportsForAllEntities != null && (testReportsForAllEntities.equalsIgnoreCase("true") || testReportsForAllEntities.equalsIgnoreCase("specified"))) {
					logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
					groupArray = new Object[allEntitySection.size() - 1][];
				} else {
					String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "entitiestotest").split(",");
					logger.debug("entitiesToTest :{} , entitiesToTest.size() : {}", entitiesToTest, entitiesToTest.length);
					groupArray = new Object[entitiesToTest.length][];
					allEntitySection = Arrays.asList(entitiesToTest);
				}

				for (String entitySection : allEntitySection) {
					if (entitySection.equalsIgnoreCase("default")) {
						continue;
					} else {
						logger.debug("entitySection :{}", entitySection);
						Integer entityTypeIddataProvider = ConfigureConstantFields.getEntityIdByName(entitySection);
						groupArray[i] = new Object[2];
						List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection);
						for (String entitySpecificProperty : allProperties) {
							if (entitySpecificProperty.contentEquals("test")) {
								String reportsIdsdataProvider = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection, entitySpecificProperty);
								logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIdsdataProvider);
								List<String> reportsIdsArray = Arrays.asList(reportsIdsdataProvider.split(","));
								groupArray[i][0] = entityTypeIddataProvider;
								groupArray[i][1] = reportsIdsArray;
							}
						}
						i++;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while creating dataProvider for the test testReportRenderData");
			addTestResultAsSkip(getTestCaseIdForMethodName("testReportRenderData"), csAssert);
			throw new SkipException("Exception while creating dataProvider for the test testReportRenderData. " + e.getMessage());
		}
		logger.info("Formed the paramter Array to be tested");

//		CustomAssert csAssertion = new CustomAssert();
		try {
			String entityName = "";
			for (int i = 0; i < groupArray.length; i++) {
				entityTypeId = Integer.parseInt(groupArray[i][0].toString());

				reportsIds = (ArrayList<String>) groupArray[i][1];
				entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
				logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

				for (String reportId : reportsIds) {

					logger.info("###################################################:Tests Starting for ReportId:{}##################################################################", reportId);


					if (skipReportIdsList.contains(reportId)) {
						logger.info("No need to test report : [ {} ] , as it is mentioned in Config file", reportId);
					} else {

						reportRendererFilterData(entityName, Integer.parseInt(reportId.trim()), csAssert);
					}


					logger.info("Validating Default UserList MetaData API Call for Report Id : [{}]", reportId);
					verifyReportRendererDefaultUserListMetaDataAPIStatusCode(Integer.parseInt(reportId.trim()), csAssert);
					validateReportRendererDefaultUserListMetaData(Integer.parseInt(reportId.trim()), csAssert);
					logger.info("Done : Validating Default UserList MetaData API Call for Report Id : [{}]", reportId);

					logger.info("Validating FilterData API Call for Report Id : [{}]", reportId);
					verifyReportRendererFilterDataAPIStatusCode(Integer.parseInt(reportId.trim()), csAssert);
					validateReportRendererFilterData(Integer.parseInt(reportId.trim()), csAssert);
					logger.info("Done : Validating Default UserList MetaData API Call for Report Id : [{}]", reportId);

					logger.info("Validating ListData API Call for Report Id : [{}]", reportId);
					verifyReportRendererListDataAPIStatusCode(Integer.parseInt(reportId.trim()), csAssert);
					validateReportRendererListData(Integer.parseInt(reportId.trim()));
					logger.info("Done : Validating Default UserList MetaData API Call for Report Id : [{}]", reportId);
					logger.info("###################################################:Tests Ending for ReportId:{}##################################################################", reportId);


				}
			}
		} catch (Exception e) {
			logger.error("Got Exception. {}", e.getMessage());
			csAssert.assertTrue(false, "Got Exception. " + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testReportRenderData"), csAssert);
		csAssert.assertAll();
	}

	public void reportRendererFilterData(String entityName, Integer reportId, CustomAssert csAssertion) {
		try {
			logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
			logger.info("Checking the reports for entity [ {} ] , report Id [ {} ]", entityName, reportId);
			reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportId);
			String defaultMetadata = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
			logger.debug("The default Metadata API response is  : {}", defaultMetadata);
			Map<Integer, Map<String, String>> filters = filterUtils.getFilters(defaultMetadata);
			for (Map.Entry<Integer, Map<String, String>> filter : filters.entrySet()) {
				for (Map.Entry<String, String> detail : filter.getValue().entrySet()) {
					logger.debug("Key [ {} ] , Value [ {} ]", detail.getKey(), detail.getValue());
				}
			}
			logger.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			// Filter Data API
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			reportRendererFilterData.hitReportRendererFilterData(reportId);
			String responseFilterData = reportRendererFilterData.getReportRendererFilterDataJsonStr();
			logger.debug("The Filter Data API Response is [ {} ]", responseFilterData);
			List<FilterData> filteredDataList = filterUtils.getFiltersData(responseFilterData, reportId.toString(), entityTypeId.toString());

			// randomfilteredDataList is for filteredDataList id on which validation would be perform
			List<FilterData> randomfilteredDataList = filteredDataList;
			// we will pick random filterCountToTestForSmoke filter or less in case of smoke testing
			if (ConfigureEnvironment.getTestingType().toLowerCase().contains("smoke") && filterCountToTestForSmoke < randomfilteredDataList.size()) {
				randomfilteredDataList = new ArrayList<>();
				int[] randomIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filteredDataList.size(), filterCountToTestForSmoke);
				for (int i = 0; i < randomIndex.length; i++) {
					randomfilteredDataList.add(filteredDataList.get(randomIndex[i]));
				}
			}

			for (FilterData filterDataClass : randomfilteredDataList) {
				if (filters.containsKey(filterDataClass.getFilterId())) {
					String filterUiType = filters.get(filterDataClass.getFilterId()).get("uiType");
					Integer filterId = filterDataClass.getFilterId();
					String filterQueryName = filters.get(filterDataClass.getFilterId()).get("queryName");
					logger.debug("The Filter UI Type is [ {} ]", filterUiType);
					logger.debug("The Filter filterId is [ {} ]", filterId);
					logger.info("The filterQueryName is [ {} ]", filterQueryName);

					if (filterUiType.equalsIgnoreCase("STATUS") || filterUiType.equalsIgnoreCase("MULTISELECT")) {
						boolean testResultForSTATUSFilter = testFilter(entityName, reportId, filterId, filterUiType, filterQueryName, filterDataClass);
						csAssertion.assertTrue(testResultForSTATUSFilter, "Testing STATUS Filter failed ,for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] . \n\n");
					} else if (filterUiType.equalsIgnoreCase("DATE")) {
						boolean testResultForDATEFilter = testDATEFilter(entityName, reportId, filterId, filterUiType, filterQueryName);
						csAssertion.assertTrue(testResultForDATEFilter, "Testing DATE Filter failed , for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
					} else if (filterUiType.equalsIgnoreCase("SLIDER")) {
						boolean testResultForSLIDERFilter = testSLIDERFilter(entityName, reportId, filterId, filterUiType, filterQueryName);
						csAssertion.assertTrue(testResultForSLIDERFilter, "Testing SLIDER Filter failed , for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
					} else if (filterUiType.equalsIgnoreCase("STAKEHOLDER")) {
						boolean testResultForSTAKEHOLDERFilter = testSTAKEHOLDERFilter(entityName, reportId, filterId, filterUiType, filterQueryName, filterDataClass);
						csAssertion.assertTrue(testResultForSTAKEHOLDERFilter, "Testing STAKEHOLDER Filter failed ,for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
					}
				} else {
					logger.error("No FIlter Found in Metadata API Response for [ {} ] and , ID : [ {} ]", filterDataClass.getFilterName(), filterDataClass.getFilterId());
				}

			}
		} catch (Exception e) {
			logger.error("Got Exception while Testing the report : [ {} ], for entity : [ {} ], Cause : [ {} ], Exception : [ {} ]", reportId, entityName, e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
	}

	public boolean testSTAKEHOLDERFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass) {

		boolean testSTAKEHOLDERFilterResponse = true;
		Integer entityTypeId = -1;
		try {
			entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String stakeHolder_Config_Vaues = "";
			Map<String, List<String>> stakeGroupsMapFromConfig = new HashMap<>();
			String StakeHolderGroupsDelemeter = "";
			String StakeHolderOneGroupDelemeter = "";
			String StakeHolderGroupEntryID = "";

			try {
				stakeHolder_Config_Vaues = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "stakeholders");
				StakeHolderGroupsDelemeter = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "stakeholdergroupsdelemeter");
				StakeHolderOneGroupDelemeter = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "stakeholderonegroupdelemeter");
				StakeHolderGroupEntryID = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "stakeholdergroupentityid");

			} catch (Exception e) {
				logger.error("Got exception while getting config values for Entity : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityName, e.getMessage(), e.getStackTrace());
				pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, false, "Not Formed, Got exception while getting config values", e.getMessage() + " and " + e.getStackTrace());
				e.printStackTrace();
				return false;
			}
			if ((stakeHolder_Config_Vaues == null || stakeHolder_Config_Vaues.equalsIgnoreCase(""))) {
				logger.debug("No Specification found in the TestReports.cfg file , testing for all Data");
				if (filterDataClass.getDataClassList().size() > 0) {

					List<FilterData.DataClass> dataClassList;
					if (testFiltersRandomData.equalsIgnoreCase("true")) {
						dataClassList = getNRandomList(filterDataClass.getDataClassList(), Integer.parseInt(filtersRandomDataSize));
					} else {
						dataClassList = filterDataClass.getDataClassList();
					}


					for (FilterData.DataClass dataClassObj : dataClassList) {

						Map<String, String> stakeHoldersMapToBetested;
						if (testFiltersRandomData.equalsIgnoreCase("true")) {
							stakeHoldersMapToBetested = getRandomMap(dataClassObj.getMapOfData(), Integer.parseInt(filtersRandomDataSize));
						} else {
							stakeHoldersMapToBetested = dataClassObj.getMapOfData();
						}

						for (Map.Entry<String, String> StakeHolderDataToBeSetected : stakeHoldersMapToBetested.entrySet()) {
							String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, StakeHolderDataToBeSetected.getKey());
							logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ] , filterQueryName : [ {} ]", payLoad, entityTypeId, filterQueryName);
							HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
							logger.debug(" The response is [ {} ] , for payload [ {} ] of reportId : [ {} ] ", reportRendererListData.getListDataJsonStr(), payLoad, reportId);
							boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
							if (!isResponseValidJson) {
								testSTAKEHOLDERFilterResponse = false;
								logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
								pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Not a Valid Json");
							} else {
								pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Valid Json");
							}
						}
					}

				} else {
					logger.error("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterQueryName);
				}
			} else {
				String stakeGroups[] = stakeHolder_Config_Vaues.split(Pattern.quote(StakeHolderGroupsDelemeter.trim()));
				for (String group : stakeGroups) {
					String groupEntry[] = group.split(Pattern.quote(StakeHolderOneGroupDelemeter.trim()));
					String groupIDs[] = groupEntry[1].split(Pattern.quote(StakeHolderGroupEntryID.trim()));
					List<String> groupIDsList = new ArrayList<>();
					for (String id : groupIDs) {
						groupIDsList.add(id);
					}
					stakeGroupsMapFromConfig.put(groupEntry[0], groupIDsList);
				}


				if (filterDataClass.getDataClassList().size() > 0) {

					List<FilterData.DataClass> dataClassList;
					if (testFiltersRandomData.equalsIgnoreCase("true")) {
						dataClassList = getNRandomList(filterDataClass.getDataClassList(), Integer.parseInt(filtersRandomDataSize));
					} else {
						dataClassList = filterDataClass.getDataClassList();
					}

					for (FilterData.DataClass dataClassObj : dataClassList) {
						if (stakeGroupsMapFromConfig.containsKey(dataClassObj.getDataName())) {
							for (String stakeHolderGroupIDToBeSelected : stakeGroupsMapFromConfig.get(dataClassObj.getDataName())) {
								String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, stakeHolderGroupIDToBeSelected);
								logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
								HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
								logger.debug(" The response is : [ {} ], for payload : [ {} ] of reportId : [ {} ]", reportRendererListData.getListDataJsonStr(), payLoad, reportId);
								boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
								if (!isResponseValidJson) {
									testSTAKEHOLDERFilterResponse = false;
									logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
									pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Not a Valid Json");
								} else {
									pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Valid Json");
								}
							}
						} else {
							logger.debug("The Group ID is not available in Config File , [ {} ]", dataClassObj.getDataName());
						}

					}
				} else {
					logger.error("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterQueryName);
				}
			}
		} catch (Exception e) {
			logger.error("Got Exception while testing StakeHolder for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Cause : [ {} ], Exception is : [ {} ]", entityName, reportId, filterQueryName, e.getMessage(), e.getMessage());
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, false, "Not Formed, Got Exception while testing StakeHolder", e.getMessage() + " and " + e.getStackTrace());
			e.printStackTrace();
			return false;
		}
		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterQueryName, testSTAKEHOLDERFilterResponse);
		return testSTAKEHOLDERFilterResponse;
	}

	public boolean testSLIDERFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName) {

		boolean testSLIDERFilterResponse = true;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String slider_min = "";
		String slider_max = "";
		try {
			String slider_min_fromEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "slider_min");
			if (slider_min_fromEntitySection == null || slider_min_fromEntitySection.equalsIgnoreCase("")) {
				slider_min = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "slider_min");
			} else {
				slider_min = slider_min_fromEntitySection;
			}

			String slider_max_toEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "slider_max");
			if (slider_max_toEntitySection == null || slider_max_toEntitySection.equalsIgnoreCase("")) {
				slider_max = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "slider_max");
			} else {
				slider_max = slider_max_toEntitySection;
			}
		} catch (Exception e) {
			logger.error("Got Exception while fetching Slider Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, reportId, filterQueryName, e.getMessage());
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, false, "Not Formed, Got Exception while fetching Slider Values from config files", e.getMessage() + " and " + e.getStackTrace());
			e.printStackTrace();
			return false;
		}


		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(slider_min);
		dataClassObj.setDataValue(slider_max);


		String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
		logger.debug(" The response is : [ {} ], for payload : [ {} ] of reportId : [ {} ]", reportRendererListData.getListDataJsonStr(), payLoad, reportId);
		boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
		if (!isResponseValidJson) {
			testSLIDERFilterResponse = false;
			logger.error("The SLIDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Not a Valid Json");
		} else {
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Valid Json");
		}

		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterQueryName, testSLIDERFilterResponse);
		return testSLIDERFilterResponse;
	}

	public boolean testDATEFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName) {

		boolean testDATEFilterResponse = true;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		String date_from = "";
		String date_to = "";
		String startDate = "";
		String endDate = "";
		try {
			String duedate_fromEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "date_from");
			if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
				date_from = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "date_from");
			} else {
				date_from = duedate_fromEntitySection;
			}

			String duedate_toEntitySection = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, "date_to");
			if (duedate_fromEntitySection == null || duedate_fromEntitySection.equalsIgnoreCase("")) {
				date_to = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "date_to");
			} else {
				date_to = duedate_toEntitySection;
			}


			String actualDate = DateUtils.getDateFromEpoch(System.currentTimeMillis(), dateFormat);
			startDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_from), dateFormat);
			endDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_to), dateFormat);

		} catch (Exception e) {
			logger.error("Got Exception while fetching Date Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Cause : [ {} ],  Exception is [ {} ]", entityName, reportId, filterQueryName, e.getMessage(), e.getStackTrace());
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, false, "Payload not formed, Got Exception while fetching Date Values from config files", e.getMessage() + " and " + e.getStackTrace());
			e.printStackTrace();
			return false;
		}

		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(startDate);
		dataClassObj.setDataValue(endDate);


		String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
		logger.debug(" The response is : [ {} ], for payload : [ {} ] of reportId : [ {} ]", reportRendererListData.getListDataJsonStr(), payLoad, reportId);
		boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
		if (!isResponseValidJson) {
			testDATEFilterResponse = false;
			logger.error("The DATE filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Not a Valid Json");
		} else {
			pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Valid Json");
		}

		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterQueryName, testDATEFilterResponse);
		return testDATEFilterResponse;
	}

	public boolean testFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass) {
		boolean testFilterResponse = true;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		if (filterDataClass.getDataClassList().size() > 0) {

			List<FilterData.DataClass> dataClassList;
			if (testFiltersRandomData.equalsIgnoreCase("true")) {
				dataClassList = getNRandomList(filterDataClass.getDataClassList(), Integer.parseInt(filtersRandomDataSize));
			} else {
				dataClassList = filterDataClass.getDataClassList();
			}

			for (FilterData.DataClass dataClassObj : dataClassList) {
				String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
				logger.debug(" The  payload is [ {} ] for entityTypeId : [ {} ], filterUiType : [ {} ], filterQueryName : [ {} ], filterId : [ {} ]", payLoad, entityTypeId, filterUiType, filterQueryName, filterId);
				reportRendererListData.hitReportRendererListData(reportId, payLoad);
				logger.debug(" The response is : [ {} ], for payload : [ {} ] of reportId : [ {} ]", reportRendererListData.getListDataJsonStr(), payLoad, reportId);
				boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
				if (!isResponseValidJson) {
					testFilterResponse = false;
					logger.error("The Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName  [ {} ], filterUiType : [ {} ],  payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, filterUiType, payLoad);
					pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Not a Valid Json");
				} else {
					pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad, "The API Response is a Valid Json");
				}

			}
		} else {
			logger.warn("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterQueryName);
		}

		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterQueryName, testFilterResponse);
		return testFilterResponse;
	}

	public void pushReportsResultToCSV(Integer entityTypeId, String entityName, Integer reportId, Integer filterId, String filterQueryName, boolean isResponseValidJson, String payLoad, String remark) {
		logger.debug("Generating CSV File");
		if (reportsMap.containsKey(entityTypeId.toString())) {
			if (reportsMap.get(entityTypeId.toString()).containsKey(reportId.toString())) {
				String reportName = reportsMap.get(entityTypeId.toString()).get(reportId.toString());
				String offset = ConfigureConstantFields.getConstantFieldsProperty("offset");
				String pageSize = ConfigureConstantFields.getConstantFieldsProperty("pagesize");
				String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
				String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");
				ResultInfoClass oReportResultInfoClass = new ReportResultInfoClass(entityTypeId.toString(), entityName, reportId.toString(), reportName, filterId.toString(), filterQueryName, payLoad, Boolean.toString(isResponseValidJson), remark, pageSize, offset, orderByColumnName, orderDirection);
				oCSVResults.writeReportsToCSVFile(ReportsResultFileName, oReportResultInfoClass);
			} else {
				logger.error("The report ID [{}] is not available for Entity : [{}]", reportId, entityTypeId);
			}
		} else {
			logger.error("The entity TypeId is not available in Map [{}]", entityTypeId);
		}
	}

	//Verification of List Rederer Default List Meta Data API Response Status Code
	public void verifyReportRendererDefaultUserListMetaDataAPIStatusCode(Integer reportId, CustomAssert csAssertion) throws Exception {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportId);
		csAssertion.assertTrue(reportRendererDefaultUserListMetaData.getStatusCodeFrom(response).contains("200"), "reportRendererDefaultUserListMetaData Status Code is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Validation of List Rederer Default List Meta Data
	public void validateReportRendererDefaultUserListMetaData(Integer reportId, CustomAssert csAssertion) throws Exception {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		csAssertion.assertTrue(!reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr().contains("errorMessage"), "reportRendererDefaultUserListMetaData Response is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Verification of List Renderer Filter Data API Response Status Code
	public void verifyReportRendererFilterDataAPIStatusCode(Integer reportId, CustomAssert csAssertion) throws IOException, ConfigurationException {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererFilterData.hitReportRendererFilterData(reportId);
		csAssertion.assertTrue(reportRendererFilterData.getStatusCodeFrom(response).contains("200"), "reportRendererFilterData Status Code is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Validation of List Renderer Filter Data
	public void validateReportRendererFilterData(Integer reportId, CustomAssert csAssertion) throws IOException, ConfigurationException {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		csAssertion.assertTrue(!reportRendererFilterData.getReportRendererFilterDataJsonStr().contains("errorMessage"), "reportRendererFilterData Response is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Verification of List Renderer List Data API Response Status Code
	public void verifyReportRendererListDataAPIStatusCode(Integer reportId, CustomAssert csAssertion) throws IOException, ConfigurationException {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId);
		csAssertion.assertTrue(reportRendererListData.getStatusCodeFrom(response).contains("200"), "reportRendererListData Status Code is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//validation of List Renderer List Data
	public void validateReportRendererListData(Integer reportId) throws IOException, ConfigurationException {

		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		Assert.assertTrue(!reportRendererListData.getListDataJsonStr().contains("errorMessage"), "reportRendererListData Response is not correct for reportId : " + reportId);
		logger.debug("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	@Test
	public void testDumpAllReportsDataToCSV() {
		CustomAssert csAssert = new CustomAssert();
		try {
			HttpResponse response = reportRenderListReportJsonObj.hitReportRender();
			String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
			logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
			JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
			logger.info("Getting the Entities to be tested from config file");
			entitiesTOBeTested = ParseConfigFile.getAllSectionNames(reportRendererConfigFilePath, reportRendererConfigFileName);
			for (String entityToBeTested : entitiesTOBeTested) {
				logger.info("Entity Name : [ {} ]", entityToBeTested);
			}

			reportRenderListReportJsonObj.dumpReportMetadataInCsvFromJson(reportJsonArrayForEntities, reportRenderCSVFile, reportRenderCSVDelemeter);

		} catch (Exception e) {
			logger.error("Exception while running testReportRenderData , cause : [ {} ] , Exception : [ {} ] ", e.getMessage(), e.getStackTrace());
			csAssert.assertTrue(false, "Exception while running testReportRenderData ");
		}
		addTestResult(getTestCaseIdForMethodName("testDumpAllReportsDataToCSV"), csAssert);
		csAssert.assertAll();
	}

	//This method will return the reports mentioned in Confige File TestReports.cfg
	public List<String> getReportsToBeTestedForEntity(String entityName) throws ConfigurationException {
		logger.info("Getting the Reports to be tested for Entity [ {} ] , from Config File", entityName);
		List<String> reportsToBeTestedForEntity = new ArrayList<>();
		List<String> reportsForEntity = ParseConfigFile.getAllPropertiesOfSection(reportRendererConfigFilePath, reportRendererConfigFileName, entityName);
		logger.info("{}", reportsForEntity);
		for (String reportName : reportsForEntity) {
			boolean reportTobeTested = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entityName, reportName));
			if (reportTobeTested) {
				logger.info("{}", reportTobeTested);
				reportsToBeTestedForEntity.add(reportName);
			}
		}
		return reportsToBeTestedForEntity;
	}

	public Object[][] getDefaultReportsList() {
		Object[][] groupArray = null;
		try {
			reportRenderListReportJsonObj.hitReportRender();
			String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
			logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
			JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
			int noOfEntitiesInReport = reportJsonArrayForEntities.length();
			logger.info("Number of Records [ {} ]", noOfEntitiesInReport);

			Map<Integer, List<String>> dataProvidermap = new HashMap<>();


			if (allEntitySection.size() > noOfEntitiesInReport) {
				logger.debug("The entries in Config are more than the API Response");
				dataProvidermap = getdataProviderMapIfConfigHasMoreEntries(reportJsonArrayForEntities);
			} else {
				logger.debug("The entries in the API Response are more than Config");
				dataProvidermap = getdataProviderMapIfAPIResponseHasMoreEntries(reportJsonArrayForEntities);
			}

			groupArray = new Object[dataProvidermap.size()][2];
			int count = 0;

			for (Map.Entry<Integer, List<String>> entry : dataProvidermap.entrySet()) {
				groupArray[count][0] = entry.getKey();
				groupArray[count][1] = entry.getValue();//.toArray(new Object[entry.getValue().size()]);
				count++;
			}
		} catch (Exception e) {
			logger.error("Got Exception while creating Data Provider for All Reports , Cause : [ {} ] , Stack : [ {} ]", e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}

		return groupArray;
	}

	public Map<Integer, List<String>> getdataProviderMapIfConfigHasMoreEntries(JSONArray reportJsonArrayForEntities) {
		logger.info("Creating Data Provider Map as Config has more entries than API Response");
		Map<Integer, List<String>> dataProvidermap = new HashMap<>();
		for (int rc = 0; rc < allEntitySection.size(); rc++) {
			String entityNameInConfig = allEntitySection.get(rc);
			int entityIdInConfig = ConfigureConstantFields.getEntityIdByName(entityNameInConfig);

			for (int count = 0; count < reportJsonArrayForEntities.length(); count++) {
				JSONObject obj = reportJsonArrayForEntities.getJSONObject(count);
				JSONUtility jsonUtilObj = new JSONUtility(obj);
				int entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
				if (entityTypeId == entityIdInConfig) {
					logger.info("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityNameInConfig, entityTypeId);
					JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
					int sizeOfSubReports = subReportsJsonArray.length();
					List<String> reportsIdsArray = new ArrayList<>();
					for (int sc = 0; sc < sizeOfSubReports; sc++) {
						JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
						JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
						String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
						String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
						if (isManualReport.equalsIgnoreCase("false")) {
							reportsIdsArray.add(reportId);
						}
					}
					dataProvidermap.put(entityTypeId, reportsIdsArray);
				} else {
					continue;
				}
			}
		}
		return dataProvidermap;
	}

	public Map<Integer, List<String>> getdataProviderMapIfAPIResponseHasMoreEntries(JSONArray reportJsonArrayForEntities) {
		logger.info("Creating Data Provider Map as API Response has more entries than Config");
		Map<Integer, List<String>> dataProvidermap = new HashMap<>();
		for (int rc = 0; rc < reportJsonArrayForEntities.length(); rc++) {
			JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
			JSONUtility jsonUtilObj = new JSONUtility(obj);
			Integer entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			if (entityName != null) {
				if (allEntitySection.contains(entityName.toLowerCase())) {
					logger.info("Currently creating for entityName [ {} ] , and  entityTypeId [ {} ] ", entityName, entityTypeId);
					JSONArray subReportsJsonArray = jsonUtilObj.getArrayJsonValue("listMetaDataJsons");
					int sizeOfSubReports = subReportsJsonArray.length();
					List<String> reportsIdsArray = new ArrayList<>();
					for (int sc = 0; sc < sizeOfSubReports; sc++) {
						JSONObject subReportJsonObj = subReportsJsonArray.getJSONObject(sc);
						JSONUtility jsonUtilObjForSubReport = new JSONUtility(subReportJsonObj);
						String reportId = jsonUtilObjForSubReport.getStringJsonValue("id");
						String isManualReport = jsonUtilObjForSubReport.getStringJsonValue("isManualReport");
						if (isManualReport.equalsIgnoreCase("false")) {
							reportsIdsArray.add(reportId);
						}
					}
					dataProvidermap.put(entityTypeId, reportsIdsArray);
				} else {
					logger.warn("The Entity Name [ {} ] is not available in the Config file.", entityName);
				}
			}
		}
		return dataProvidermap;
	}

	private Map<String, String> getRandomMap(Map<String, String> originalMap, int randomDataCounts) {
		if (originalMap.size() <= randomDataCounts) {
			return originalMap;
		} else {
			Map<String, String> randomDataMap = new HashMap<>();
			int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, originalMap.keySet().size() - 1, randomDataCounts);
			List<Object> keySetList = Arrays.asList(originalMap.keySet().toArray());
			for (int i = 0; i < randomNumbers.length; i++) {
				String key = (String) keySetList.get(randomNumbers[i]);
				String value = originalMap.get(key);

				randomDataMap.put(key, value);

			}
			return randomDataMap;
		}
	}

	private List<FilterData.DataClass> getNRandomList(List<FilterData.DataClass> originalList, int randomDataCounts) {
		if (originalList.size() <= randomDataCounts) {
			return originalList;
		} else {
			List<FilterData.DataClass> copy = new ArrayList<>(originalList);
			Collections.shuffle(copy);
			return copy.subList(0, randomDataCounts);
		}
	}

	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.debug("In After Method");
		logger.debug("method name is: {}", result.getMethod().getMethodName());
		logger.debug("***********************************************************************************************************************");
	}

	@AfterClass
	public void afterClass() {
		logger.debug("In After Class method");
	}


}
