package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.api.scheduleReport.ScheduleLargeReport;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by shivashish on 10/4/18.
 * this Test File is created as a pre-requisite for [ts-80698]
 */
public class TestScheduleLargeReport extends TestRailBase {


	private final static Logger logger = LoggerFactory.getLogger(TestScheduleLargeReport.class);

	String scheduleLargeReportConfigFilePath;
	String scheduleLargeReportConfigFileName;

	String reportIdMappingFilePath;
	String reportIdMappingFileName;

	List<String> allEntitySection;
	ScheduleLargeReport scheduleLargeReport;
	String clientId;

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getScheduleLargeReportConfigData();
		testCasesMap = getTestCasesMapping();

		clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");
	}


	public void getScheduleLargeReportConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Initializing Test Data for Report Render");

		scheduleLargeReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ScheduleLargeReportConfigFilePath");
		scheduleLargeReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ScheduleLargeReportConfigFileName");

		reportIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportIdMappingFilePath");
		reportIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportIdMappingFileName");

		allEntitySection = ParseConfigFile.getAllSectionNames(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName);

	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "TestReportRendererData")
	public Object[][] getTestReportRendererData() throws ConfigurationException {

		logger.info("In the Data Provider");
		int i = 0;
		logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());

		List<Object[]> allTestData = new ArrayList<>();


		for (String entitySection : allEntitySection) {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
			List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, entitySection);
			for (String entitySpecificProperty : allProperties) {
				if (entitySpecificProperty.contentEquals("test")) {
					String[] reportsIds = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath, scheduleLargeReportConfigFileName, entitySection, entitySpecificProperty).split(",");
					for(String reportId : reportsIds){
						String reportName = "";
						reportName = ParseConfigFile.getValueFromConfigFile(reportIdMappingFilePath,reportIdMappingFileName,reportId);
						allTestData.add(new Object[]{reportName,entityTypeId,reportId});
					}
				}

			}
		}

		return allTestData.toArray(new Object[0][]);

	}


	@Test(dataProvider = "TestReportRendererData", priority = 0)
//	@Test()
	public void testScheduleLargeReport(String reportName,Integer entityTypeId,String reportId) {

		CustomAssert customAssert = new CustomAssert();

		try {

			String entityName = "";

			ReportRendererListData reportRendererListData = new ReportRendererListData();
			reportRendererListData.hitReportRendererListData(Integer.parseInt(reportId), true);
			String reportListResponse = reportRendererListData.getListDataJsonStr();

			if(!JSONUtility.validjson(reportListResponse)){
				logger.error("Report List Response is not a valid json for report " + reportName);
				customAssert.assertTrue(false,"Report List Response is not a valid json for report " + reportName);
			}else {
				JSONObject reportListResponseJson = new JSONObject(reportListResponse);
				String reportCount = reportListResponseJson.get("totalCount").toString();

				UpdateFile.updateConfigFileProperty(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"exp num of records " + reportId,reportCount);
			}

			entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			logger.info("###########:Tests Starting for Entity:{}#########", entityName);

			scheduleLargeReport = new ScheduleLargeReport(reportId, String.valueOf(entityTypeId));
			String currentTimeStamp = DateUtils.getDBTimeStamp();
			scheduleLargeReport.hitCreateScheduleLargeReportAPI();

			Boolean isResponseJson = APIUtils.validJsonResponse(scheduleLargeReport.getResponseScheduleLargeReportAPI());
			customAssert.assertTrue(scheduleLargeReport.getApiStatusCode().contains("200"), "API status code is not correct While hitting scheduling Large Report API for Report Id " + reportId);

			customAssert.assertTrue(isResponseJson, "ScheduleLargeReport API Response in not a valid Json");

			// Json Response Validation
			if (isResponseJson) {
				JSONObject apiResponse = new JSONObject(scheduleLargeReport.getResponseScheduleLargeReportAPI());
				customAssert.assertTrue(apiResponse.get("success").toString().contentEquals("true"), "ScheduleLargeReport API is failing since success flag in response json in not true for Report Id " + reportId);
				customAssert.assertTrue(apiResponse.get("errors").toString().contentEquals(""), "ScheduleLargeReport API is failing since there is Error Message in response Json for Report Id " + reportId);

				PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
				String sqlQueryToFetchTaskDetails = "select id,status,status_id from schedule_large_report where " +
						"client_id = " +  clientId + " and " +
						"entity_type_id = "+ entityTypeId + " and " +
						"list_id = " + reportId  + "and " +
						"date_created > '" + currentTimeStamp + "'";

				List<List<String>> sqlOutput = postgreSQLJDBC.doSelect(sqlQueryToFetchTaskDetails);

				if(sqlOutput.size() == 0){
					customAssert.assertTrue(false,"SQL Query To Fetch task details for Schedule Large Report has zero records");
				}else {
					String id = sqlOutput.get(0).get(0);
					String status_id = sqlOutput.get(0).get(2);

					//Status id =3 means Scheduled Status
					if(status_id.equals("3")){
						logger.info("Report Id " + reportId + " has been scheduled successfully");
					}else {
						logger.error("Report Id " + reportId + " does not have status scheduled in the database");
						customAssert.assertTrue(false,"Report Id " + reportId + " does not have status scheduled in the database");
					}

					UpdateFile.updateConfigFileProperty(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"schedule task id " + reportId,id);
					UpdateFile.updateConfigFileProperty(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"exp num of records " + reportId,id);

				}

			}


		} catch (Exception e) {
			logger.error("Exception. " + e.getMessage());
			customAssert.assertTrue(false, "Exception. " + e.getMessage());
		}

		customAssert.assertAll();
	}


	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "TestMethodName", "reportId", "createScheduleReportFormAPI", "createScheduleReportAPI", "scheduleByMeReportAPI", "isScheduleReportIsInActiveStatus", "TestMethodResult", "Comments", "ErrorMessage"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("***********************************************************************************************************************");

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
