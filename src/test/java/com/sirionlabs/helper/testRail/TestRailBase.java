package com.sirionlabs.helper.testRail;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;

import java.util.Map;

public class TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestRailBase.class);

	private TestRailHelper railHelperObj = new TestRailHelper();
	public Map<String, String> testCasesMap;

	public Map<String, String> getTestCasesMapping() {
		String testCasesMappingConfigFilePath = "src/test/resources/CommonConfigFiles";
		String testCasesMappingConfigFileName = "TestRailCasesMapping.cfg";

		return ParseConfigFile.getAllConstantPropertiesCaseSensitive(testCasesMappingConfigFilePath, testCasesMappingConfigFileName, this.getClass().getSimpleName());
	}

	public Integer getTestCaseIdForMethodName(String methodName) {
		try {
			if (testCasesMap != null && !testCasesMap.isEmpty()) {
				return Integer.parseInt(testCasesMap.get(methodName));
			} else {
				logger.error("No Value Present in TestCases Map.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting TestCase Id for MethodName [{}] from TestCase Map. []", methodName, e.getStackTrace());
		}

		return null;
	}

	public void addTestResultAsSkip(Integer testCaseId, CustomAssert csAssert) {
		if (testCaseId != null) {
			railHelperObj.addTestResultAsSkipToMap(testCaseId, csAssert);
		}
	}

	public void addTestResult(Integer testCaseId, CustomAssert csAssert) {
		if (testCaseId != null) {
			railHelperObj.addTestResultToMap(testCaseId, csAssert);
		}
	}

	@AfterClass
	public void baseAfterClass() {
		//Update Test Results in TestRail.
		railHelperObj.updateTestsStatusInTestRail();
	}
}