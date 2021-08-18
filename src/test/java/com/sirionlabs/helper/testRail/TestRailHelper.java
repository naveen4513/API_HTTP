package com.sirionlabs.helper.testRail;

import com.codepine.api.testrail.TestRail;
import com.codepine.api.testrail.model.Result;
import com.codepine.api.testrail.model.ResultField;
import com.codepine.api.testrail.model.Run;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TestRailHelper {

	private final static Logger logger = LoggerFactory.getLogger(TestRailHelper.class);

	private static String configFilePath = "src/test/resources/CommonConfigFiles";
	private static String configFileName = "TestRailConfig.cfg";

	private static Boolean updateTestsInTestRail = null;

	private static Integer projectId;
	private static Integer suiteId = -1;
	private static Integer testRunId = -1;
	private static Integer milestoneId = -1;

	private static List<Integer> caseIds = new ArrayList<>();
	private static List<Result> allResults = new ArrayList<>();

	private static TestRail testRailObj;

	TestRailHelper() {
		if (testRailObj == null) {
			testRailObj = getInstance();
		}
	}

	private static TestRail getInstance() {
		logger.info("Creating Instance of TestRail.");

		String hostAddress = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "host");
		String userName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "username");
		String userPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "password");

		return TestRail.builder(hostAddress, userName, userPassword).build();
	}

	public static void initializeTestRail() {
		try {
			logger.info("Initializing TestRail.");

			projectId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "projectId"));

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "updateTests");
			updateTestsInTestRail = temp != null && temp.trim().equalsIgnoreCase("true");

			if (updateTestsInTestRail) {
				/*temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createSuite");
				Boolean createNewSuite = temp != null && temp.trim().equalsIgnoreCase("true");

				suiteId = createNewSuite ? createSuite() : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "suiteId"));*/

				suiteId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "suiteId"));

				temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "milestoneId");
				if (temp != null && !temp.trim().equalsIgnoreCase(""))
					milestoneId = Integer.parseInt(temp.trim());

				temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createTestRun");
				Boolean createNewTestRun = temp != null && temp.trim().equalsIgnoreCase("true");

				testRunId = createNewTestRun ? createTestRun() : Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
						"testRunId"));

				if (testRunId == -1) {
					logger.info("Couldn't get Test Run Id. Hence Turning the Flag UpdateTests Off.");
					updateTestsInTestRail = false;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Initializing TestRail. {}", e.getMessage());
			updateTestsInTestRail = false;
		}
	}

	/*
	Creates New Suite in TestRail and returns ID.
	 */
	/*private static int createSuite() {
		String suiteName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "suiteName");
		logger.info("Creating New Suite [{}]", suiteName);

		return testRailObj.suites().add(projectId, new Suite().setName(suiteName)).execute().getId();
	}*/

	/*
	Creates New Test Run in TestRail and returns ID.
	 */
	private static int createTestRun() {
		String testRunName = getNewTestRunName();
		logger.info("Creating New Test Run [{}]", testRunName);

		if (milestoneId != -1) {
			return testRailObj.runs().add(projectId, new Run().setSuiteId(suiteId).setName(testRunName)).execute().getId();
		} else {
			return testRailObj.runs().add(projectId, new Run().setSuiteId(suiteId).setName(testRunName).setMilestoneId(milestoneId)).execute().getId();
		}
	}

	void addTestResultToMap(Integer testCaseId, CustomAssert csAssert) {
		if (updateTestsInTestRail) {
			addTestCaseToTestRun(testCaseId);

			int statusId = csAssert.getAllAssertionMessages().equalsIgnoreCase("") ? 1 : 5;
			addResultForCase(testCaseId, statusId, csAssert.getAllAssertionMessages());
		}
	}

	void addTestResultAsSkipToMap(Integer testCaseId, CustomAssert csAssert) {
		if (updateTestsInTestRail) {
			addTestCaseToTestRun(testCaseId);

			addResultForCase(testCaseId, 4, csAssert.getAllAssertionMessages());
		}
	}

	private void addTestCaseToTestRun(Integer testCaseId) {
		caseIds.add(testCaseId);
	}

	void updateTestsStatusInTestRail() {
		if (updateTestsInTestRail) {
			updateTestRun();

			logger.info("Updating Test Case Results in TestRail.");
			updateTestResults();
		} else {
			logger.info("Not Updating Tests in TestRail as the Flag [UpdateTests] is Off.");
		}
	}

	private void updateTestRun() {
		logger.info("Updating Test Run having Id [R{}]", testRunId);

		Run testRun = testRailObj.runs().get(testRunId).execute();
		testRailObj.runs().update(testRun.setIncludeAll(false).setCaseIds(caseIds)).execute();
	}

	private void addResultForCase(Integer testCaseId, int statusId, String comment) {
		if (testCaseId != null) {
			Result result = new Result();
			result.setCaseId(testCaseId).setStatusId(statusId).setComment(comment);
			allResults.add(result);
		}
	}

	private void updateTestResults() {
		List<ResultField> customResultFields = testRailObj.resultFields().list().execute();
		testRailObj.results().addForCases(testRunId, allResults, customResultFields).execute();
	}

	private static String getNewTestRunName() {
		String runNamePrefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testRunNamePrefix");

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();

		return runNamePrefix + " " + dtf.format(now);
	}
}