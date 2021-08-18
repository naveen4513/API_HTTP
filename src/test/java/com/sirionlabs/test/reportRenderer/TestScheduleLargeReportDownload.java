package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.scheduleReport.ScheduleLargeReport;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaurav on 7/1/2021.
 * this Test File is created as a pre-requisite for [ts-80698]
 */
public class TestScheduleLargeReportDownload{


	private final static Logger logger = LoggerFactory.getLogger(TestScheduleLargeReportDownload.class);

	String scheduleLargeReportConfigFilePath;
	String scheduleLargeReportConfigFileName;

	String reportIdMappingFilePath;
	String reportIdMappingFileName;

	List<String> allEntitySection;
	ScheduleLargeReport scheduleLargeReport;
	String clientId;
	String sheetName;
	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getScheduleLargeReportConfigData();
		sheetName = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"sheet name");
		clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");
	}


	public void getScheduleLargeReportConfigData() throws ParseException, ConfigurationException {
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
						allTestData.add(new Object[]{entityTypeId,reportId,reportName});
					}
				}

			}
		}

		return allTestData.toArray(new Object[0][]);

	}


	@Test(dataProvider = "TestReportRendererData", priority = 0)
//	@Test()
	public void testScheduleLargeReportDownload(Integer entityTypeId,String reportId,String reportName) {

		CustomAssert customAssert = new CustomAssert();

		try {

			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			logger.info("###########:Tests Starting for Entity:{}#########", entityName);

			String task_id = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"schedule task id " + reportId);
			String sqlQueryToFetchTaskDetails = "select id,file_Path,status,status_id,email_sent from schedule_large_report where " +
					"id = " +  task_id;

			PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
			List<List<String>> sqlOutput = postgreSQLJDBC.doSelect(sqlQueryToFetchTaskDetails);

			if(sqlOutput.size() == 0){
				customAssert.assertTrue(false,"SQL Query To Fetch task details for Schedule Large Report has zero records");
			}else {
				String id = sqlOutput.get(0).get(0);
				String file_Path = sqlOutput.get(0).get(1);
				String status = sqlOutput.get(0).get(2);
				String status_id = sqlOutput.get(0).get(3);
				String email_sent= sqlOutput.get(0).get(4);

				//Status id =1 means Active Status
				if(file_Path == null){
					logger.error("Report Id" + reportId + " has file_Path null in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " has file_Path null in the database for last run of schedule Report for task id " + task_id);
				}else {
					//File To be read from here
					String remoteFilePath = "/data/" + file_Path;

					String localDir = "src\\test\\resources\\TestConfig\\Reports\\ScheduleLargeReports\\";
					String localFileName = remoteFilePath.split("/")[remoteFilePath.split("/").length -1];

					SCPUtils scpUtils = new SCPUtils();
					Boolean scpStatus = scpUtils.getFileFromRemoteServerToLocalServer(remoteFilePath,localDir,localFileName);

					if(scpStatus) {
						String expectedNoRows = ParseConfigFile.getValueFromConfigFile(scheduleLargeReportConfigFilePath,scheduleLargeReportConfigFileName,"exp num of records " + reportId);
						int nowOfRows = XLSUtils.getNoOfRowsStream(localDir,localFileName,sheetName);

						if(!String.valueOf(nowOfRows).equals(expectedNoRows)){
							logger.error("Expected and Actual Number Of records in schedule report not matched Expected : " + expectedNoRows + " Actual : " + nowOfRows);
							customAssert.assertTrue(false,"Expected and Actual Number Of records in schedule report not matched Expected :" + expectedNoRows + " Actual :" + nowOfRows);
						}
						FileUtils.deleteFile(localDir,localFileName);
					}else {
						customAssert.assertTrue(false,"Schedule Report File SCP Done unsuccessfully" +
								"Either File Not generated or there is some issue while copying file from server");
					}

				}

				//Status id =1 means Active Status
				if(status == null){
					logger.error("Report Id" + reportId + " has status null in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " has status null in the database for last run of schedule Report for task id " + task_id);
				}else if(status.equals("active")){
					logger.info("Report Id" + reportId + " has been scheduled successfully");
				}else {
					logger.error("Report Id" + reportId + " does not have status active in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " does not have status active in the database for last run of schedule Report for task id " + task_id);
				}

				//Status id =1 means Active Status
				if(status_id == null){
					logger.error("Report Id" + reportId + " has status null in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " has status null in the database for last run of schedule Report for task id " + task_id);
				}else if(status_id.equals("1")){
					logger.info("Report Id" + reportId + " has been scheduled successfully");
				}else {
					logger.error("Report Id" + reportId + " does not have status active in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " does not have status active in the database for last run of schedule Report for task id " + task_id);
				}

				//Status id =1 means Active Status
				if(email_sent == null){
					logger.error("Report Id" + reportId + " has email_sent null in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " has status null in the database for last run of schedule Report for task id " + task_id);
				}else if(email_sent.equals("t")){
					logger.info("Report Id" + reportId + " has been scheduled successfully");
				}else {
					logger.error("Report Id" + reportId + " does not have email_sent as true in the database for last run of schedule Report for task id " + task_id);
					customAssert.assertTrue(false,"Report Id" + reportId + " does not have email_sent as true in the database for last run of schedule Report for task id " + task_id);
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
