package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.scheduleReport.CreateScheduleReportForm;
import com.sirionlabs.api.scheduleReport.ScheduleByMeReportAPI;
import com.sirionlabs.api.scheduleReport.ScheduleJobsAPI;
import com.sirionlabs.api.scheduleReport.SharedWithMeReportAPI;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class TestScheduleReportNegativeCases {


	private final static Logger logger = LoggerFactory.getLogger(TestScheduleReportNegativeCases.class);
	int reportId = 30; // as of now hardcoded @todo
	String reportNameExpected = "Actions - Aging"; // as of now hardcoded @todo
	String entityName = "actions";


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");

	}


	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("***********************************************************************************************************************");

	}


	@Test(priority = 0)
	//TC-93168:By default Subject should be report name
	public void testTC93168() throws Exception {
		CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
		HashMap<String, String> queryStringParams = new HashMap<String, String>();
		queryStringParams.put("id", String.valueOf(reportId));
		createScheduleReportForm.hitCreateReportFormAPI(queryStringParams);

		Assert.assertTrue(createScheduleReportForm.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReportForm API for Report Id " + reportId);
		Assert.assertTrue(APIUtils.validJsonResponse(createScheduleReportForm.getResponseCreateReportFormAPI()), "CreateScheduleReportFrom API Response in not a valid Json");

		JSONObject response = new JSONObject(createScheduleReportForm.getResponseCreateReportFormAPI());

		String reportNameActual = null;
		if (response.has("subject"))
			reportNameActual = response.getString("subject");

		if (reportNameActual == null || !reportNameActual.contentEquals(reportNameExpected)) {

			Assert.fail("Schedule Report Form Either don't have report Name as default subject or it's incorrect");

		}
		logger.info("Default Subject of the Report is same as the Report name");


	}

	@Test(priority = 1)
	//TC-93167:CC field shouldnt be present
	public void testTC93167() throws Exception {
		CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
		HashMap<String, String> queryStringParams = new HashMap<String, String>();
		queryStringParams.put("id", String.valueOf(reportId));
		createScheduleReportForm.hitCreateReportFormAPI(queryStringParams);

		Assert.assertTrue(createScheduleReportForm.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReportForm API for Report Id " + reportId);
		Assert.assertTrue(APIUtils.validJsonResponse(createScheduleReportForm.getResponseCreateReportFormAPI()), "CreateScheduleReportFrom API Response in not a valid Json");

		JSONObject response = new JSONObject(createScheduleReportForm.getResponseCreateReportFormAPI());


		if (JSONUtility.checkKey(response, "cc")) {
			Assert.fail("ERROR : Schedule Report is not supposed to have cc field ");
		}


		logger.info("Schedule Report don't have CC Field - As Expected");


	}

	@Test(priority = 2)
	//TC-93254:Schedule Listing should include schedule names , frequency details , date , time , report name, last
	public void testTC93254() throws Exception {

		CustomAssert csAssert = new CustomAssert();

		// Hitting the ScheduleByMeReport API
		ScheduleByMeReportAPI scheduleByMeReportAPI = new ScheduleByMeReportAPI();
		scheduleByMeReportAPI.hitScheduleByMeReportAPI();

		Assert.assertTrue(scheduleByMeReportAPI.getApiStatusCode().contains("200"), "API status code is not correct While hitting ScheduleByMeReport API");
		Assert.assertTrue(APIUtils.validJsonResponse(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI()), "ScheduleByMeReport API Response in not a valid Json");


		JSONObject response = new JSONObject(scheduleByMeReportAPI.getResponseScheduleByMeReportAPI());
		ArrayList<String> columns = new ArrayList();

		JSONArray dataArray = response.getJSONArray("data");
		if (dataArray.length() > 0) {
			JSONObject data = dataArray.getJSONObject(0);
			Set<String> keys = data.keySet();
			for (String key : keys) {
				columns.add(data.getJSONObject(key).getString("columnName"));
			}

			csAssert.assertTrue(columns.contains("name"), "Schedule by Me Section don't have column name : [name]");
			csAssert.assertTrue(columns.contains("frequencytype"), "Schedule by Me Section don't have column name : frequencytype ");
			csAssert.assertTrue(columns.contains("schedulename"), "Schedule by Me Section don't have column name : schedulename ");
			csAssert.assertTrue(columns.contains("nextsend"), "Schedule by Me Section don't have column name : nextsend");
			csAssert.assertTrue(columns.contains("lastsend"), "Schedule by Me Section don't have column name : lastsend");
			csAssert.assertTrue(columns.contains("status"), "Schedule by Me Section don't have column name : status");
			csAssert.assertTrue(columns.contains("actions"), "Schedule by Me Section don't have column name : actions");
			csAssert.assertTrue(columns.contains("calendartype"), "Schedule by Me Section don't have column name : calendartype");


			csAssert.assertAll();


		} else {

			logger.info("the schedule by me Section Don't have any record so skipping this test");
			throw new SkipException("Skipping this test");

		}


	}

	@Test(priority = 3)
	//TC-93271:Shared with me should have following options : Report Name (Reflects the name of the report shred
	public void testTC93271() throws Exception {

		CustomAssert csAssert = new CustomAssert();

		// Hitting the SharedWithMe API
		SharedWithMeReportAPI sharedWithMeReportAPI = new SharedWithMeReportAPI();
		sharedWithMeReportAPI.hitSharedWithMeReportAPI();

		Assert.assertTrue(sharedWithMeReportAPI.getApiStatusCode().contains("200"), "API status code is not correct While hitting SharedWithMeReport API ");
		Assert.assertTrue(APIUtils.validJsonResponse(sharedWithMeReportAPI.getResponseSharedWithMeReportAPI()), "SharedWithMeReport API Response in not a valid Json");


		JSONObject response = new JSONObject(sharedWithMeReportAPI.getResponseSharedWithMeReportAPI());
		ArrayList<String> columns = new ArrayList();

		JSONArray dataArray = response.getJSONArray("data");
		if (dataArray.length() > 0) {
			JSONObject data = dataArray.getJSONObject(0);
			Set<String> keys = data.keySet();
			for (String key : keys) {
				columns.add(data.getJSONObject(key).getString("columnName"));
			}

			csAssert.assertTrue(columns.contains("name"), "Shared With Me Report Section don't have column name : [name]");
			csAssert.assertTrue(columns.contains("frequencytype"), "Shared With Me Report Section don't have column name : frequencytype ");
			csAssert.assertTrue(columns.contains("sharedby"), "Shared With Me Report  Section don't have column name : sharedby ");
			csAssert.assertTrue(columns.contains("lastrecieved"), "Shared With Me Report Section don't have column name : lastrecieved");
			csAssert.assertTrue(columns.contains("upcoming"), "Shared With Me Report Section don't have column name : upcoming");
			csAssert.assertTrue(columns.contains("status"), "Shared With Me Report Section don't have column name : status");
			csAssert.assertTrue(columns.contains("actions"), "Shared With Me Report Section don't have column name : actions");
			csAssert.assertTrue(columns.contains("calendartype"), "Shared With Me Report ection don't have column name : calendartype");


			csAssert.assertAll();


		} else {

			logger.info("the Shared by me Section Don't have any record so skipping this test");
			throw new SkipException("Skipping this test");

		}


	}

	@Test(priority = 4)
	// these is no test cases associate with this
	public void testSheduleJobsAPICall() throws Exception {

		CustomAssert csAssert = new CustomAssert();

		// Hitting the ScheduleJobs API
		ScheduleJobsAPI scheduleJobsAPI = new ScheduleJobsAPI();
		scheduleJobsAPI.hitScheduleJobsAPI();

		Assert.assertTrue(scheduleJobsAPI.getApiStatusCode().contains("200"), "API status code is not correct While hitting ScheduleJobs API ");
		Assert.assertTrue(APIUtils.validJsonResponse(scheduleJobsAPI.getResponseScheduleJobsAPI()), "ScheduleJobs API Response in not a valid Json");


		JSONObject response = new JSONObject(scheduleJobsAPI.getResponseScheduleJobsAPI());
		ArrayList<String> columns = new ArrayList();

		JSONArray dataArray = response.getJSONArray("data");
		if (dataArray.length() > 0) {
			JSONObject data = dataArray.getJSONObject(0);
			Set<String> keys = data.keySet();
			for (String key : keys) {
				columns.add(data.getJSONObject(key).getString("columnName"));
			}

			csAssert.assertTrue(columns.contains("reportname"), "Schedule Jobs Section don't have column name : [reportname]");
			csAssert.assertTrue(columns.contains("datereportgenerated"), "Schedule Jobs Section don't have column name : datereportgenerated ");
			csAssert.assertTrue(columns.contains("datecreated"), "Schedule Jobs Section don't have column name : datecreated ");
			csAssert.assertTrue(columns.contains("download"), "Schedule Jobs Section don't have column name : download");
			csAssert.assertTrue(columns.contains("status"), "Schedule Jobs Section don't have column name : status");


			csAssert.assertAll();


		} else {

			logger.info(" the Schedule Jobs Section Don't have any record so skipping this test");
			throw new SkipException("Skipping this test");

		}


	}

	@Test(priority = 5)
	//TC-93185:verify TimeZone field is avalaible in mail
	public void testTC93185() throws Exception {
		CreateScheduleReportForm createScheduleReportForm = new CreateScheduleReportForm();
		HashMap<String, String> queryStringParams = new HashMap<String, String>();
		queryStringParams.put("id", String.valueOf(reportId));
		createScheduleReportForm.hitCreateReportFormAPI(queryStringParams);

		Assert.assertTrue(createScheduleReportForm.getApiStatusCode().contains("200"), "API status code is not correct While hitting CreateScheduleReportForm API for Report Id " + reportId);
		Assert.assertTrue(APIUtils.validJsonResponse(createScheduleReportForm.getResponseCreateReportFormAPI()), "CreateScheduleReportFrom API Response in not a valid Json");

		JSONObject response = new JSONObject(createScheduleReportForm.getResponseCreateReportFormAPI());


		if (response.has("timeZones") || (response.get("timeZones") instanceof JSONArray)) {
			logger.info("schedule Report Form has timeZones Field Array in it ");
		} else {
			Assert.fail("ERROR : Schedule Report form don't have timezone field in it");
		}


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
