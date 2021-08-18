package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRenderListReportJson;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.CSVResults;
import com.sirionlabs.utils.csvutils.ReportResultInfoClass;
import com.sirionlabs.utils.csvutils.ResultInfoClass;
import com.sirionlabs.utils.executerserviceutil.TaskExecuter;
import com.sirionlabs.utils.executerserviceutil.TaskImpl;
import com.sirionlabs.utils.executerserviceutil.TaskReturnObject;
import com.sirionlabs.utils.taskutil.ReportsTaskImpl;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author manoj.upreti
 */
public class TestReportRenderParallelExecution {
	private final static Logger logger = LoggerFactory.getLogger(TestReportRenderParallelExecution.class);
	ReportRenderListReportJson reportRenderListReportJsonObj;
	ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData;
	ReportRendererFilterData reportRendererFilterData;
	ReportRendererListData reportRendererListData;
	FilterUtils filterUtils;
	CustomAssert csAssertion;

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


	List<String> entitiesTOBeTested = new ArrayList<>();


	public TestReportRenderParallelExecution() {
		try {
			logger.info("Calling Report Renderer Constructor");
			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
			reportRendererConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFilePath");
			reportRendererConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRendererConfigFileName");
			allEntitySection = ParseConfigFile.getAllSectionNames(reportRendererConfigFilePath, reportRendererConfigFileName);
			reportRenderCSVFile = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVFile");
			reportRenderCSVDelemeter = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderCSVDelimiter");
			dateFormat = ConfigureConstantFields.getConstantFieldsProperty("DateFormatForReports");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getReportRenderConfigData();
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
		logger.debug("The Report Response is : [ {} ]", reportRenderJsonStr);
		JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
		reportsMap = reportRenderListReportJsonObj.generateReportsMap(reportJsonArrayForEntities);


		ReportsResultFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportRenderResultsCSVFile");
		oCSVResults = new CSVResults();
		ResultInfoClass resultInfoClass = new ReportResultInfoClass();
		oCSVResults.initializeResultCsvFile(ReportsResultFileName, resultInfoClass);
	}


	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("***********************************************************************************************************************");

	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "TestReportRendererData", parallel = true)
	public Object[][] getTestReportRendererData(ITestContext c) throws ConfigurationException {

		logger.info("In the Data Provider");

		String testableReports = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, "default", "testreportsforentities");
		if (testableReports.equalsIgnoreCase("all")) {
			return getDefaultReportsList();
		} else {
			int i = 0;
			logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
			Object[][] groupArray = new Object[allEntitySection.size() - 1][];

			for (String entitySection : allEntitySection) {
				if (entitySection.equalsIgnoreCase("default")) {
					continue;
				} else {
					logger.debug("entitySection :{}", entitySection);
					Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
					groupArray[i] = new Object[2];
					List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection);
					for (String entitySpecificProperty : allProperties) {
						if (entitySpecificProperty.contentEquals("test")) {
							String reportsIds = ParseConfigFile.getValueFromConfigFile(reportRendererConfigFilePath, reportRendererConfigFileName, entitySection, entitySpecificProperty);
							logger.debug("entitySection :{} ,reportsIds :{}", entitySection, reportsIds);
							List<String> reportsIdsArray = Arrays.asList(reportsIds.split(","));
							groupArray[i][0] = entityTypeId;
							groupArray[i][1] = reportsIdsArray;
						}
					}
					i++;
				}
			}
			logger.info("Formed the paramter Array to be tested");
			return groupArray;
		}
	}


	@Test(dataProvider = "TestReportRendererData")
	public void testReportRenderData(Integer entityTypeId, List<String> reportsIds) throws Exception {
		csAssertion = new CustomAssert();
		String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
		logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

		for (String reportId : reportsIds) {

			logger.info("###################################################:Tests Starting for ReportId:{}##################################################################", reportId);

			reportRendererFilterData(entityName, Integer.parseInt(reportId.trim()));

			verifyReportRendererDefaultUserListMetaDataAPIStatusCode(Integer.parseInt(reportId.trim()));
			validateReportRendererDefaultUserListMetaData(Integer.parseInt(reportId.trim()));

			verifyReportRendererFilterDataAPIStatusCode(Integer.parseInt(reportId.trim()));
			validateReportRendererFilterData(Integer.parseInt(reportId.trim()));

			verifyReportRendererListDataAPIStatusCode(Integer.parseInt(reportId.trim()));
			validateReportRendererListData(Integer.parseInt(reportId.trim()));
			logger.info("###################################################:Tests Ending for ReportId:{}##################################################################", reportId);

		}

		logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
		csAssertion.assertAll();
	}


	public void reportRendererFilterData(String entityName, Integer reportId) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
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


		for (FilterData filterDataClass : filteredDataList) {
			if (filters.containsKey(filterDataClass.getFilterId())) {
				String filterUiType = filters.get(filterDataClass.getFilterId()).get("uiType");
				Integer filterId = filterDataClass.getFilterId();
				String filterQueryName = filters.get(filterDataClass.getFilterId()).get("queryName");
				logger.debug("The Filter UI Type is [ {} ]", filterUiType);
				logger.debug("The Filter filterId is [ {} ]", filterId);
				logger.debug("The filterQueryName is [ {} ]", filterQueryName);

				if (filterUiType.equalsIgnoreCase("STATUS")) {
					boolean testResultForSTATUSFilter = testFilter(entityName, reportId, filterId, filterUiType, filterQueryName, filterDataClass);
					csAssertion.assertTrue(testResultForSTATUSFilter, "Testing STATUS Filter failed ,for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] . \n\n");
				} else if (filterUiType.equalsIgnoreCase("MULTISELECT")) {
					boolean testResultForMULTISELECTFilter = testFilter(entityName, reportId, filterId, filterUiType, filterQueryName, filterDataClass);
					csAssertion.assertTrue(testResultForMULTISELECTFilter, "Testing MULTISELECT Filter failed , for entityName : [" + entityName + "] ,reportId : [" + reportId + "] ,filterQueryName : [" + filterQueryName + "] .\n\n");
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
	}


	public boolean testSTAKEHOLDERFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass) {
		List<TaskImpl> taskList = new ArrayList<>();
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		boolean testSTAKEHOLDERFilterResponse = true;
		try {
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
				logger.error("Gor exception while getting config values for Entity : [ {} ]", entityName);
			}
			if ((stakeHolder_Config_Vaues == null || stakeHolder_Config_Vaues.equalsIgnoreCase(""))) {
				logger.debug("No Specification found in the TestReports.cfg file , testing for all Data");
				if (filterDataClass.getDataClassList().size() > 0) {
					for (FilterData.DataClass dataClassObj : filterDataClass.getDataClassList()) {
						for (Map.Entry<String, String> StakeHolderDataToBeSetected : dataClassObj.getMapOfData().entrySet()) {
							String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, StakeHolderDataToBeSetected.getKey());
							logger.debug(" The  payload is [ {} ] ", payLoad);
							/*HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
							logger.debug(" The response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
							boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
							if (!isResponseValidJson) {
								testSTAKEHOLDERFilterResponse = false;
								logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterUiType [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterUiType, payLoad);
							}
							pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad);*/
							TaskImpl objTaskCreation = new ReportsTaskImpl("validatereport", entityName, reportId.toString(), "Report Task", String.valueOf(filterId), filterQueryName, payLoad);
							taskList.add(objTaskCreation);
						}
					}

				} else {
					logger.error("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterUiType);
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
					for (FilterData.DataClass dataClassObj : filterDataClass.getDataClassList()) {
						if (stakeGroupsMapFromConfig.containsKey(dataClassObj.getDataName())) {
							for (String stakeHolderGroupIDToBeSelected : stakeGroupsMapFromConfig.get(dataClassObj.getDataName())) {
								String payLoad = filterUtils.getPayloadForSTAKEHOLDERFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj, stakeHolderGroupIDToBeSelected);
								logger.debug(" The  payload is [ {} ] ", payLoad);
								/*HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
								logger.info(" The response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
								boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
								if (!isResponseValidJson) {
									testSTAKEHOLDERFilterResponse = false;
									logger.error("The STAKEHOLDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterQueryName [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
								}
								pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad);*/
								TaskImpl objTaskCreation = new ReportsTaskImpl("validatereport", entityName, reportId.toString(), "Report Task", String.valueOf(filterId), filterQueryName, payLoad);
								taskList.add(objTaskCreation);
							}
						} else {
							logger.debug("The Group ID is not available in Config File , [ {} ]", dataClassObj.getDataName());
						}

					}
				} else {
					logger.error("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterUiType);
				}
			}
		} catch (Exception e) {
			logger.error("Got Exception while testing StakeHolder for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is : [ {} ]", entityName, reportId, filterQueryName, e.getStackTrace());
		}

		List<TaskReturnObject> taskResults = TaskExecuter.executeParallely(taskList);
		for (TaskReturnObject taskResult : taskResults) {
			if (!taskResult.SUCCESS) {
				testSTAKEHOLDERFilterResponse = false;
			}

			pushReportsResultToCSV(entityTypeId, entityName, Integer.parseInt(taskResult.dbID), Integer.parseInt(taskResult.filterID), taskResult.filterQueryName, taskResult.SUCCESS, taskResult.requestedPayload);
		}

		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterUiType, testSTAKEHOLDERFilterResponse);
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
			logger.error("Got Error while fetching Slider Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, reportId, filterUiType, e.getStackTrace());
		}


		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(slider_min);
		dataClassObj.setDataValue(slider_max);


		String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is [ {} ] ", payLoad);
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
		logger.debug(" The response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
		boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
		if (!isResponseValidJson) {
			testSLIDERFilterResponse = false;
			logger.error("The SLIDER filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterUiType [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterUiType, payLoad);
		}
		pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad);

		logger.info("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterUiType, testSLIDERFilterResponse);
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
		} catch (Exception e) {
			logger.error("Got Error while fetching Date Values from config files for Entity [ {} ], Report [ {} ] , filterName [ {} ] , Exception is [ {} ]", entityName, reportId, filterUiType, e.getStackTrace());
		}


		String actualDate = DateUtils.getDateFromEpoch(System.currentTimeMillis(), dateFormat);

		try {
			startDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_from), dateFormat);
			endDate = DateUtils.getDateOfXDaysFromYDate(actualDate, Integer.parseInt(date_to), dateFormat);
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}

		FilterData.DataClass dataClassObj = new FilterData.DataClass();
		dataClassObj.setDataName(startDate);
		dataClassObj.setDataValue(endDate);


		String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
		logger.debug(" The  payload is  :  {}  ", payLoad);
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId, payLoad);
		logger.debug(" The response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
		boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
		if (!isResponseValidJson) {
			testDATEFilterResponse = false;
			logger.error("The DATE filter Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterUiType [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterUiType, payLoad);
		}
		pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad);

		logger.info("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterUiType, testDATEFilterResponse);
		return testDATEFilterResponse;
	}

	public boolean testFilter(String entityName, Integer reportId, Integer filterId, String filterUiType, String filterQueryName, FilterData filterDataClass) {
		boolean testFilterResponse = true;
		Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		List<TaskImpl> taskList = new ArrayList<>();

		if (filterDataClass.getDataClassList().size() > 0) {
			for (FilterData.DataClass dataClassObj : filterDataClass.getDataClassList()) {
				String payLoad = filterUtils.getPayloadForFilter(entityTypeId + "", filterUiType, filterId + "", filterQueryName, dataClassObj);
				logger.debug("The Payload is : {}", payLoad);
				/*
				//Adding Multithreading
				reportRendererListData.hitReportRendererListData(reportId, payLoad);
				logger.debug(" the response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
				boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
				if (!isResponseValidJson) {
					testFilterResponse = false;
					logger.error("The Json response is not a valid Json for entity [ {} ] , reportId [ {} ] , filterUiType [ {} ], payLoad [ {} ] \n\n", entityName, reportId, filterUiType, payLoad);
				}
				pushReportsResultToCSV(entityTypeId, entityName, reportId, filterId, filterQueryName, isResponseValidJson, payLoad);*/

				//String taskName, String entityName, String dbID, String taskType,String filterID, String filterQueryName, String requestedPayload
				TaskImpl objTaskCreation = new ReportsTaskImpl("validatereport", entityName, reportId.toString(), "Report Task", String.valueOf(filterId), filterQueryName, payLoad);
				taskList.add(objTaskCreation);
			}
		} else {
			logger.error("The filterData List Size is 0 for Entity [ {} ], Report [ {} ] , filterName [ {} ]", entityName, reportId, filterUiType);
		}

		List<TaskReturnObject> taskResults = TaskExecuter.executeParallely(taskList);
		for (TaskReturnObject taskResult : taskResults) {
			if (!taskResult.SUCCESS) {
				testFilterResponse = false;
			}
			pushReportsResultToCSV(entityTypeId, entityName, Integer.parseInt(taskResult.dbID), Integer.parseInt(taskResult.filterID), taskResult.filterQueryName, taskResult.SUCCESS, taskResult.requestedPayload);
		}

		logger.debug("The Response for Entity [ {} ], Report [ {} ] , filterName [ {} ] is [ {} ]", entityName, reportId, filterUiType, testFilterResponse);
		return testFilterResponse;
	}

	public boolean validateResponceParallelly(String entityName, int reportId, int filterID, String filterQueryName, String payLoad) {
		logger.debug("The Payload is : {}", payLoad);
		reportRendererListData.hitReportRendererListData(reportId, payLoad);
		logger.debug(" the response for payload [ {} ] ", reportRendererListData.getListDataJsonStr());
		boolean isResponseValidJson = APIUtils.validJsonResponse(reportRendererListData.getListDataJsonStr());
		if (!isResponseValidJson) {
			logger.error("The Json response is not a valid Json for entity : [ {} ] , reportId : [ {} ] , filterID : [ {} ] , filterUiType : [ {} ], payLoad : [ {} ] \n\n", entityName, reportId, filterQueryName, payLoad);
		}
		//Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
		//pushReportsResultToCSV(entityTypeId,entityName,reportId,filterID,filterQueryName,isResponseValidJson,payLoad);
		return isResponseValidJson;
	}

	public void pushReportsResultToCSV(Integer entityTypeId, String entityName, Integer reportId, Integer filterId, String filterQueryName, boolean isResponseValidJson, String payLoad) {
		logger.debug("Generating CSV File");
		if (reportsMap.containsKey(entityTypeId.toString())) {
			if (reportsMap.get(entityTypeId.toString()).containsKey(reportId.toString())) {
				String reportName = reportsMap.get(entityTypeId.toString()).get(reportId.toString());
				String remark = "Remark";
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
	public void verifyReportRendererDefaultUserListMetaDataAPIStatusCode(Integer reportId) throws Exception {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportId);
		csAssertion.assertTrue(reportRendererDefaultUserListMetaData.getStatusCodeFrom(response).contains("200"), "reportRendererDefaultUserListMetaData Status Code is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Validation of List Rederer Default List Meta Data
	public void validateReportRendererDefaultUserListMetaData(Integer reportId) throws Exception {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		csAssertion.assertTrue(!reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr().contains("errorMessage"), "reportRendererDefaultUserListMetaData Response is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}


	//Verification of List Renderer Filter Data API Response Status Code
	public void verifyReportRendererFilterDataAPIStatusCode(Integer reportId) throws IOException, ConfigurationException {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererFilterData.hitReportRendererFilterData(reportId);
		csAssertion.assertTrue(reportRendererFilterData.getStatusCodeFrom(response).contains("200"), "reportRendererFilterData Status Code is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}


	//Validation of List Renderer Filter Data
	public void validateReportRendererFilterData(Integer reportId) throws IOException, ConfigurationException {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		csAssertion.assertTrue(!reportRendererFilterData.getReportRendererFilterDataJsonStr().contains("errorMessage"), "reportRendererFilterData Response is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Verification of List Renderer List Data API Response Status Code
	public void verifyReportRendererListDataAPIStatusCode(Integer reportId) throws IOException, ConfigurationException {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response = reportRendererListData.hitReportRendererListData(reportId);
		csAssertion.assertTrue(reportRendererListData.getStatusCodeFrom(response).contains("200"), "reportRendererListData Status Code is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//validation of List Renderer List Data
	public void validateReportRendererListData(Integer reportId) throws IOException, ConfigurationException {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		Assert.assertTrue(!reportRendererListData.getListDataJsonStr().contains("errorMessage"), "reportRendererListData Response is not correct for reportId : " + reportId);
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	@Test
	public void testReportRenderData() {
		csAssertion = new CustomAssert();
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
			List<String> propertiesToBeTestedForEntity = getReportsToBeTestedForEntity(entitiesTOBeTested.get(1));
			for (String report : propertiesToBeTestedForEntity) {
				logger.info("Report Name : [ {} ]", report);
			}

		} catch (Exception e) {
			logger.error("Exception while running testReportRenderData {}", e.getStackTrace());
			csAssertion.assertTrue(false, "Exception while running testReportRenderData ");
		}
		csAssertion.assertAll();
	}

	//This method will return the reports mentioned in Confige File TestReports.cfg
	public List<String> getReportsToBeTestedForEntity(String entityName) throws ConfigurationException {
		logger.info("Getting the Reports to be tested for Entity [ {} ] , from Config File", entityName);
		List<String> reportsToBeTestedForEntity = new ArrayList<>();
		List<String> reportsForEntity = new ArrayList<>();
		reportsForEntity = ParseConfigFile.getAllPropertiesOfSection(reportRendererConfigFilePath, reportRendererConfigFileName, entityName);
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
		reportRenderListReportJsonObj.hitReportRender();
		String reportRenderJsonStr = reportRenderListReportJsonObj.getReportRendorJsonStr();
		logger.info("The Report Response is : [ {} ]", reportRenderJsonStr);
		JSONArray reportJsonArrayForEntities = new JSONArray(reportRenderJsonStr);
		int noOfEntitiesInReport = reportJsonArrayForEntities.length();
		logger.info("Number of Records [ {} ]", noOfEntitiesInReport);

		Map<Integer, List<String>> dataProvidermap = new HashMap<>();

		int i = 0;
		for (int rc = 0; rc < allEntitySection.size(); rc++) {

			JSONObject obj = reportJsonArrayForEntities.getJSONObject(rc);
			JSONUtility jsonUtilObj = new JSONUtility(obj);
			Integer entityTypeId = jsonUtilObj.getIntegerJsonValue("entityTypeId");
			//String entityName = jsonUtilObj.getStringJsonValue("name");
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

		Object[][] groupArray = new Object[dataProvidermap.size()][2];
		int count = 0;
		try {
			for (Map.Entry<Integer, List<String>> entry : dataProvidermap.entrySet()) {
				groupArray[count][0] = entry.getKey();
				groupArray[count][1] = entry.getValue();//.toArray(new Object[entry.getValue().size()]);
				count++;
			}
		} catch (Exception e) {
			logger.error("Got Exception : [ {} ]", e.getStackTrace());
			//e.printStackTrace();
		}

		return groupArray;
	}


	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");
	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}


}
