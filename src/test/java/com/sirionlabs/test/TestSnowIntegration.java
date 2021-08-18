package com.sirionlabs.test;

import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.snowIntegration.ChildServiceLevelPerformenceDataExcelDownload;
import com.sirionlabs.api.snowIntegration.GetExcelRowCount;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceLevel;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by shivashish on 13/9/17.
 */
public class TestSnowIntegration extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestSnowIntegration.class);
	CustomAssert csAssert;


	private String  snowIntegrationConfigFilePath;
	private String snowIntegrationConfigFileName;
	private int totalRowCount;
	private int excelRowCountFromServiceLevelPerformanceDataTab = -1;

	private String dateparam_query;
	private String sysparm_query;
	private String sysparm_display_value;
	private String sysparm_exclude_reference_link;
	private String sysparm_fields;
	private String sysparm_limit;
	private String masterServiceLevelId = null;
	private String childServiceLevelId = null;
	private String downloadDirectory;
	private String offset;
	private String size;
	private String dateFormate;

	private String entityIdMappingFileName;
	private String baseFilePath;
	private String serviceLevelSectionName = "service levels";
	private String serviceLevelTypeId;
	private String childServiceLevelSectionName = "child service levels";
	private String childServiceLevelTypeId;
	private String childServiceLevelTabId;
	private String structredPerformanceDataTabId;
	private String valueSplitter = ":;";

	Boolean isIntegrationJarExecuted = false;

	TabListData tabListData;
	ChildServiceLevelPerformenceDataExcelDownload childServiceLevelPerforamceDataExcelDownload;


	DumpResultsIntoCSV dumpResultsObj;
	String TestResultCSVFilePath;
	int globalIndex = 0;
	int serviceLevelId = -1;
	String delay = "";
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer slEntityTypeId;
	private static String performance_data_format_filepath=null;
	private static String performance_data_format_filename=null;

	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "TestMethodName", "TestMethodResult", "Comments", "ErrorMessage"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}


	/**
	 * beforeClass
	 *
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		tabListData = new TabListData();
		childServiceLevelPerforamceDataExcelDownload = new ChildServiceLevelPerformenceDataExcelDownload();

		///// SL creation
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");
		extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
		extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty( "ServiceLevelsExtraFieldsFileName");
		slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");

		//////
		snowIntegrationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("snowIntegrationConfigFilePath");
		snowIntegrationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("snowIntegrationConfigFileName");

		try {
			dateparam_query = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "dateparam_query");
			sysparm_query = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "sysparm_query");
			sysparm_display_value = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "sysparm_display_value");
			sysparm_exclude_reference_link = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "sysparm_exclude_reference_link");
			sysparm_fields = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "sysparm_fields");
			sysparm_limit = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "sysparm_limit");
			masterServiceLevelId = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "msl_id");
			offset = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "offset");
			size = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "size");
			dateFormate = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "dateformate");
			isIntegrationJarExecuted = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "integration_jar_executed"));
			downloadDirectory = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "downloaddirectory");
			delay = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "delay");
			performance_data_format_filepath = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "performance_data_format_filepath");
			performance_data_format_filename = ParseConfigFile.getValueFromConfigFile(snowIntegrationConfigFilePath, snowIntegrationConfigFileName, "performance_data_format_filename");


			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
			serviceLevelTypeId = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceLevelSectionName, "entity_type_id");
			childServiceLevelTypeId = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, childServiceLevelSectionName, "entity_type_id");
			childServiceLevelTabId = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceLevelSectionName, "child_service_level_tab_id");
			structredPerformanceDataTabId = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, childServiceLevelSectionName, "structured_performance_data_tab_id");

			// for Storing the result of Sorting
			int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
			String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
			TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className + "/";
			logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
			dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());

		} catch (Exception e) {
			logger.info("Error : Issue in getting the query param from config file SnowIntegration.cfg");
			logger.error(e.getMessage());

		}

		testCasesMap = getTestCasesMapping();
	}

	/**
	 * beforeMethod
	 *
	 * @param method
	 */
	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}


	/**
	 * API to Get X-Total Count from Service Now "servicecafe.service-now.com/api/now/table/incident_sla" API
	 *
	 * @throws Exception
	 */
	@Test(priority = 1)
	public void testServiceNowGetXtotalCountGetAPI() throws Exception {
		csAssert = new CustomAssert();
		GetExcelRowCount getExcelRowCount = new GetExcelRowCount();
		HashMap<String, String> queryStringParams = new HashMap<String, String>();
		queryStringParams.put("sysparm_query", dateparam_query+sysparm_query);
//		queryStringParams.put("sysparm_display_value", sysparm_display_value);
//		queryStringParams.put("sysparm_exclude_reference_link", sysparm_exclude_reference_link);
//		queryStringParams.put("sysparm_fields", sysparm_fields);
//		queryStringParams.put("sysparm_limit", sysparm_limit);


		getExcelRowCount.hitGetExcelRowCount(queryStringParams);
		csAssert.assertTrue(getExcelRowCount.getApiStatusCode().contains("200"), "Status Code of Service Now API Response for getting X-Total Count is Incorrect");
		csAssert.assertTrue(APIUtils.validJsonResponse(getExcelRowCount.getApiResponse()), "Response of Service Now API Response for getting X-Total Count is not valid Json");

		if (getExcelRowCount.getTotalRowCount() != -1) {
			totalRowCount = getExcelRowCount.getTotalRowCount();
			logger.info("totalRowCount is : {}", totalRowCount);

		} else {
			csAssert.assertTrue(false, "Service Now API Response for Getting X-Total Count , don't have X-Total Count header in Response");
		}
		addTestResult(getTestCaseIdForMethodName("testServiceNowGetXtotalCountGetAPI"), csAssert);
		csAssert.assertAll();

	}


	@Test(priority = 2, dependsOnMethods = "testServiceNowGetXtotalCountGetAPI")
	public void testChildServiceLevelTabForGivenMSL() {

		csAssert = new CustomAssert();

		try {
			if (isIntegrationJarExecuted) {
				String payload = "{\"filterMap\":{\"entityTypeId\":" + childServiceLevelTypeId + ",\"offset\":" +
						offset + ",\"size\":" +
						size + ",\"orderByColumnName\":\"id\"," +
						"\"orderDirection\":\"asc\",\"filterJson\":{}}}";

				String reportingDate;

				tabListData.hitTabListData(childServiceLevelTabId, true, childServiceLevelTypeId, serviceLevelTypeId, String.valueOf(serviceLevelId), payload);
				csAssert.assertTrue(tabListData.getApiStatusCode().contains("200"), "tabListData API Response Code for Getting Child Service Data API is incorrect");
				csAssert.assertTrue(APIUtils.validJsonResponse(tabListData.getTabListDataResponseStr()), "tabListData API for Getting Child Service Data ,  Response is not valid Json");


				DateFormat dateFormat = new SimpleDateFormat(dateFormate);
				Date presentdate = new Date();
				logger.debug("Current Date is : [{}]", dateFormat.format(presentdate)); //2016/11/16 12:08:43


				JSONObject tabListDataResponseObj = new JSONObject(tabListData.getTabListDataResponseStr());
				int noOfRecords = tabListDataResponseObj.getJSONArray("data").length();
				if (noOfRecords > 0) {
					for (int i = 0; i < noOfRecords; i++) {
						JSONObject jsonObject = tabListDataResponseObj.getJSONArray("data").getJSONObject(i);
						if (JSONUtility.getValueByEmbeddedKey("reporting_date", jsonObject, "value") != null) {
							reportingDate = JSONUtility.getValueByEmbeddedKey("reporting_date", jsonObject, "value");
							DateFormat df = new SimpleDateFormat(dateFormate);
							Date cslReportingDate;
							try {
								cslReportingDate = df.parse(reportingDate);
								logger.debug("cslReportingDate is : [{}]", df.format(cslReportingDate));
								if (presentdate.getMonth() - cslReportingDate.getMonth() == 1) {
									childServiceLevelId = JSONUtility.getValueByEmbeddedKey("id", jsonObject, "value").split(valueSplitter)[1];
									logger.info("CSL DB ID For Last Month is : [{}]", childServiceLevelId);
									break;
								}

							} catch (ParseException e) {
								logger.error("Error: in Parsing the reporting dates from CSL tab of MSL");
								csAssert.assertTrue(false, "Error in Prasing the reporting dates from CSL tab of MSL");
								e.printStackTrace();
							}

						}

					}
				}
			} else {
				logger.info("Intergration Jar Not Executed Yet , Skipping this test : testChildServiceLevelTabForGivenMSL ");
			}
		} catch (Exception e) {
			logger.warn("Error:Child Service Level Tab is Empty for Given MSA ");
			csAssert.assertTrue(false, "Error child service level tab is empty for given msa");
			e.printStackTrace();
		}

		addTestResult(getTestCaseIdForMethodName("testChildServiceLevelTabForGivenMSL"), csAssert);
		csAssert.assertAll();
	}

	@Test(priority = 3, dependsOnMethods = "testChildServiceLevelTabForGivenMSL")
	public void testGetExcelRowCountFromServiceLevelPerformanceDataTab() throws Exception {

		csAssert = new CustomAssert();
		if (isIntegrationJarExecuted) {
			if (childServiceLevelId != null) {
				String payload = "{\"filterMap\":{\"entityTypeId\":" + childServiceLevelTypeId + ",\"offset\":" +
						offset + ",\"size\":" +
						size + ",\"orderByColumnName\":\"id\"," +
						"\"orderDirection\":\"asc\",\"filterJson\":{}}}";

				tabListData.hitTabListData(structredPerformanceDataTabId, true, childServiceLevelTypeId, childServiceLevelTypeId, childServiceLevelId, payload);
				Assert.assertTrue(tabListData.getApiStatusCode().contains("200"), "tabListData API Response Code for Getting structured performance Data API is incorrect");
				Assert.assertTrue(APIUtils.validJsonResponse(tabListData.getTabListDataResponseStr()), "tabListData API for Getting structured performance Data ,  Response is not valid Json");


				if (tabListData.getStructuredPerformanceExcelIds() != null) {
					String[] excelIds = tabListData.getStructuredPerformanceExcelIds();
					logger.info("excelIds : [{}]", excelIds);


					String excelName = excelIds[0].split(valueSplitter)[0];
					String excelId = excelIds[0].split(valueSplitter)[1];
					childServiceLevelPerforamceDataExcelDownload.downloadServiceDataTemplateFile(downloadDirectory + "//" + excelName, excelId);
					csAssert.assertTrue(childServiceLevelPerforamceDataExcelDownload.getApiStatusCode().contains("200"), "childServiceLevelPerforamceDataExcelDownload API Response Code is incorrect");


					XLSUtils xlsReader = new XLSUtils(downloadDirectory, excelName); // Initialize the XLS Reader Utils for XLS file
					List<String> Sheets = xlsReader.getSheetNames();
					if (Sheets.size() > 0) {
						excelRowCountFromServiceLevelPerformanceDataTab = xlsReader.getRowCount(Sheets.get(0));
						logger.info("excelRowCountFromServiceLevelPerformanceDataTab : [{}]", excelRowCountFromServiceLevelPerformanceDataTab);
						XLSUtils.delete_xls(downloadDirectory,excelName);
						logger.info(excelName+" is deleted successfully");
					} else {
						Assert.fail("Error : there is not any sheet in filePath :" + downloadDirectory + "having fileName:" + excelName);
					}


				} else {
					logger.error("There is no excel sheet in Child Service Level Performance Data Tab");
					csAssert.assertTrue(false, "There is no excel sheet in Child Service Level Performance Data Tab");
				}

			} else {
				logger.error("Child Service Level for Last Month hasn't been created for Master Service Level");
				csAssert.assertTrue(false, "Child Service Level for Last Month hasn't been created for Master Service Level");
			}
		} else {
			logger.info("Intergration Jar Not Executed Yet , Skipping this test : testGetExcelRowCountFromServiceLevelPerformanceDataTab");
		}
		csAssert.assertAll();

	}


	/**
	 * this test will verify whether Row Count from Structure data peformance to X-Row-Count from Service Now API
	 *
	 * @throws Exception
	 */
	@Test(priority = 4, dependsOnMethods = "testGetExcelRowCountFromServiceLevelPerformanceDataTab")
	public void testVerifyXLRowCountWithServiceNowAPIResponse() throws Exception {
		csAssert = new CustomAssert();
		if (isIntegrationJarExecuted) {
			if (excelRowCountFromServiceLevelPerformanceDataTab != -1)
				csAssert.assertTrue((excelRowCountFromServiceLevelPerformanceDataTab-1) == totalRowCount, "excelRowCountFromServiceLevelPerformanceDataTab is not Matching with API Response of Service Now");
			else
				csAssert.assertTrue(false, "not being able to get excelRowCountFromServiceLevelPerformanceDataTab for Given MSL");
		} else
			logger.info("Intergration Jar Not Executed Yet , Skipping this test : testVerifyXLRowCountWithServiceNowAPIResponse");

		csAssert.assertAll();
	}



	@Test(priority = 0, description = "testSLCreate")
	public void TestSLCreate() throws Exception {

		CustomAssert csassert = new CustomAssert();
	String[] query_params = null;

		String createResponse = ServiceLevel.createServiceLevel(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "flow with serview now query",
				true);
		if (ParseJsonResponse.validJsonResponse(createResponse)) {
			JSONObject jsonObj = new JSONObject(createResponse);
			String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
			String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flow with serview now query", "expectedResult");
			logger.info("Create Status for Flow [{}]: {}", "flow with serview now query", createStatus);

			if (createStatus.equalsIgnoreCase("success")){
				serviceLevelId = CreateEntity.getNewEntityId(createResponse, "service levels");
			logger.info("service level created successfully with id --> "+ serviceLevelId);}
			else{
				csassert.assertTrue(false,"MSL is not created successfully");
			}
		}
		else{
			csassert.assertTrue(false,"MSL create api does not return valiod json");
			logger.error("SL is not created successfully");
		}

		logger.info("workflow steps started");

		EntityWorkflowActionHelper helper = new EntityWorkflowActionHelper();
		helper.hitWorkflowAction("SL",14,serviceLevelId,"Send For Peer Review");

		helper.hitWorkflowAction("SL",14,serviceLevelId,"Peer Review Complete");
		helper.hitWorkflowAction("SL",14,serviceLevelId,"Send For Internal Review");
	 	helper.hitWorkflowAction("SL",14,serviceLevelId,"Internal Review Complete");
		helper.hitWorkflowAction("SL",14,serviceLevelId,"Send For Client Review");
		helper.hitWorkflowAction("SL",14,serviceLevelId,"Approve");
		helper.hitWorkflowAction("SL",14,serviceLevelId,"Publish");

		logger.info("uploading the template in the performance data format tab");

		String showApiResponse = ShowHelper.getShowResponse(14,serviceLevelId);
		if (showApiResponse != null) {
			//  Get action URI PATH
			JSONArray actionArray = (JSONArray) JSONUtility.parseJson(showApiResponse, "$.body.layoutInfo.actions[?(@.label == \"Upload New Format\")]");
			Object uriObj = JSONUtility.parseJson(actionArray.toJSONString(), "$.[*].api");
			String queryString = ((List<String>) uriObj).get(0);
			 query_params = queryString.split("/");
		}else{
			csassert.assertTrue(false,"Show Api resonse is null");
              logger.error("show api response is  null");
		}

		Map<String, String> payloadMap = new HashMap<>();
		payloadMap.put("parentEntityTypeId", Integer.toString(14));
		payloadMap.put("parentEntityId", Integer.toString(serviceLevelId));
		payloadMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
		UploadBulkData upload = new UploadBulkData();
		upload.hitUploadBulkData(Integer.parseInt(query_params[query_params.length-2]),Integer.parseInt(query_params[query_params.length-1]),performance_data_format_filepath,performance_data_format_filename, payloadMap);
		String upload_response = upload.getUploadBulkDataJsonStr();
		csassert.assertTrue(upload_response.contains("Your request has been successfully submitted"),"template formate is not uploaded successfully");
        logger.info("template uploaded successfully");
		logger.info("start waiting after child creation for "+delay+"seconds");
				Thread.sleep(1000*Integer.valueOf(delay));
		logger.info("delay completed");
		csassert.assertAll();
	}

	/**
	 * afterMethod
	 *
	 * @param result
	 */
	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");

	}


	/**
	 * afterClass
	 */
	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}
}
