package com.sirionlabs.test.invoice;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestForecastBulkUpload {

	private final static Logger logger = LoggerFactory.getLogger(TestForecastBulkUpload.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String forecastTemplateFilePath = null;
	private Integer forecastEntityTypeId = 180;
	private Integer forecastUploadTemplateId = -1;
	private Integer contractEntityTypeId = -1;
	private Integer serviceDataEntityTypeId = -1;
	private Integer auditLogTabId = -1;
	private Integer auditLogActionNameColumnId = -1;
	private Integer auditLogCommentColumnId = -1;
	private Integer auditLogDocumentColumnId = -1;
	private Integer auditLogHistoryColumnId = -1;
	private static Long schedulerJobTimeOut = 600000L;
	private static Long schedulerJobPollingTime = 5000L;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ForecastBulkUploadConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ForecastBulkUploadConfigFileName");
		forecastTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateFilePath");
		forecastUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateId"));
		contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
		serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobPollingTime = Long.parseLong(temp.trim());

		auditLogTabId = TabListDataHelper.getIdForTab("Audit Log");
	}

	@DataProvider
	public Object[][] dataProviderForForecastUploadFlows() throws ConfigurationException {
		logger.info("Setting all Forecast Upload Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Forecast Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForForecastUploadFlows")
	public void testForecastUploadFlow(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Forecast Template Upload Flow [{}]", flowToTest);
			logger.info("Uploading Forecast Template for Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

			String forecastTemplateFileName = properties.get("forecasttemplatefilename");

			if (!(new File(forecastTemplateFilePath + "/" + forecastTemplateFileName).exists())) {
				throw new SkipException("Couldn't find Forecast Template File at Location: " + forecastTemplateFilePath + "/" + forecastTemplateFileName);
			}

			int contractId = Integer.parseInt(properties.get("contractid"));

			String uploadResponse = BulkTemplate.uploadForecastTemplate(forecastTemplateFilePath, forecastTemplateFileName, contractEntityTypeId, contractId,
					forecastEntityTypeId, forecastUploadTemplateId);
			String expectedMessage = properties.get("expectedresult").trim();

			logger.info("Actual Forecast Template Upload API Response: {} and Expected Result: {}", uploadResponse, expectedMessage);
			if (uploadResponse == null || !uploadResponse.trim().toLowerCase().contains(expectedMessage.toLowerCase())) {
				csAssert.assertTrue(false, "Forecast Template Upload Response doesn't match with Expected Response for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception whiForecast Template Upload Response doesn't match with Expected Response for Flowle validating Forecast Template Upload for Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForForecastProcessingFlows() throws ConfigurationException {
		logger.info("Setting all Forecast Template Processing Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "processingFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Forecast Upload Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForForecastProcessingFlows", priority = 1)
	public void testForecastProcessingFlow(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Forecast Template Processing Flow [{}]", flowToTest);
			logger.info("Uploading Forecast Template for Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

			String forecastTemplateFileName = properties.get("forecasttemplatefilename");

			if (!(new File(forecastTemplateFilePath + "/" + forecastTemplateFileName).exists())) {
				throw new SkipException("Couldn't find Forecast Template File at Location: " + forecastTemplateFilePath + "/" + forecastTemplateFileName);
			}

			int contractId = Integer.parseInt(properties.get("contractid"));

			Fetch fetchObj = new Fetch();
			logger.info("Hitting Fetch API for Processing Flow [{}]", flowToTest);
			fetchObj.hitFetch();
			String fetchResponse = fetchObj.getFetchJsonStr();

			if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
				UserTasksHelper.removeAllTasks(fetchResponse);

				List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchResponse);

				TabListData tabListObj = new TabListData();
				logger.info("Hitting TabListData API for Audit Log Tab of Contract Id {} and Flow [{}] to get Initial Total Logs Count", contractId, flowToTest);
				String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
				String contractAuditLogTabListDataResponse = tabListObj.hitTabListData(auditLogTabId, contractEntityTypeId, contractId, payload);
				int contractAuditLogInitialCount = ListDataHelper.getTotalListDataCount(contractAuditLogTabListDataResponse);

				int serviceDataId = Integer.parseInt(properties.get("servicedataid"));
				logger.info("Hitting TabListData API for Audit Log Tab of Service Data Id {} and Flow [{}] to get Initial Total Logs Count", serviceDataId, flowToTest);
				String serviceDataAuditLogTabListDataResponse = tabListObj.hitTabListData(auditLogTabId, serviceDataEntityTypeId, serviceDataId, payload);
				int serviceDataAuditLogInitialCount = ListDataHelper.getTotalListDataCount(serviceDataAuditLogTabListDataResponse);

				String uploadResponse = BulkTemplate.uploadForecastTemplate(forecastTemplateFilePath, forecastTemplateFileName, contractEntityTypeId, contractId,
						forecastEntityTypeId, forecastUploadTemplateId);

				if (!uploadResponse.trim().contains("200:;")) {
					throw new SkipException("Forecast Template Upload failed for Processing Flow [" + flowToTest + "]");
				}

				fetchObj.hitFetch();
				fetchResponse = fetchObj.getFetchJsonStr();

				Integer newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, allTaskIds);
				Map<String, String> schedulerJobStatus = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);

				if (schedulerJobStatus.get("jobPassed").trim().equalsIgnoreCase("skip")) {
					throw new SkipException(schedulerJobStatus.get("errorMessage") + " and Processing Flow [" + flowToTest + "]");
				}

				String expectedResult = properties.get("expectedresult");

				if (schedulerJobStatus.get("jobPassed").trim().equalsIgnoreCase("true")) {
					if (expectedResult.trim().equalsIgnoreCase("failure")) {
						csAssert.assertTrue(false, "Forecast Template Upload Passed for Processing Flow [" + flowToTest +
								"] whereas it was expected to fail");
					} else {
						contractAuditLogTabListDataResponse = tabListObj.hitTabListData(auditLogTabId, contractEntityTypeId, contractId, payload);
						int contractAuditLogTotalCount = ListDataHelper.getTotalListDataCount(contractAuditLogTabListDataResponse);

						if (contractAuditLogTotalCount <= contractAuditLogInitialCount) {
							csAssert.assertTrue(false, "Audit Log Entry not found in Contract Id " + contractId + " for Forecast Processing Flow [" +
									flowToTest + "]");
						} else {
							//Verify Contract Audit Log
							verifyAuditLogs(flowToTest, contractAuditLogTabListDataResponse, "Contract", csAssert);

							//Verify Contract Audit Log History
							verifyContractAuditLogHistory(flowToTest, contractAuditLogTabListDataResponse, forecastTemplateFileName, csAssert);

							//Verify Service Data Audit Log Entry
							serviceDataAuditLogTabListDataResponse = tabListObj.hitTabListData(auditLogTabId, serviceDataEntityTypeId, serviceDataId, payload);
							int serviceDataAuditLogTotalCount = ListDataHelper.getTotalListDataCount(serviceDataAuditLogTabListDataResponse);

							if (serviceDataAuditLogTotalCount <= serviceDataAuditLogInitialCount) {
								csAssert.assertTrue(false, "Audit Log Entry not found in Service Data Id " + serviceDataId +
										" for Forecast Processing Flow " + flowToTest + "]");
							} else {
								//Verify Service Data Audit Log Data
								verifyAuditLogs(flowToTest, serviceDataAuditLogTabListDataResponse, "Service Data", csAssert);

								//Verify Service Data Audit Log History
								verifyServiceDataAuditLogHistory(flowToTest, serviceDataAuditLogTabListDataResponse, csAssert);
							}
						}
					}
				} else {
					if (expectedResult.trim().equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Forecast Template Upload Failed for Processing Flow [" + flowToTest +
								"] whereas it was expected to Pass");
					}
				}
			} else {
				csAssert.assertTrue(false, "Fetch API Response for Forecast Processing Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Forecast Template Processing for Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void verifyAuditLogs(String flowToTest, String tabListDataResponse, String entityName, CustomAssert csAssert) {
		try {
			logger.info("Verifying Audit Log Data for Entity {} and Flow [{}]", entityName, flowToTest);
			List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

			//Verify Action Name
			if (auditLogActionNameColumnId == -1)
				auditLogActionNameColumnId = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "action_name");

			if (auditLogActionNameColumnId == -1) {
				throw new SkipException("Couldn't get Id for Column action_name from TabListData Response for Entity " + entityName + " and Flow [" + flowToTest + "]");
			}

			String actualActionName = listData.get(0).get(auditLogActionNameColumnId).get("value").trim();
			if (!actualActionName.equalsIgnoreCase("Forecast Uploaded")) {
				csAssert.assertTrue(false, "Expected Action Name: [Forecast Uploaded] and Actual Action Name: " + actualActionName);
			}

			//Verify Comment
			if (auditLogCommentColumnId == -1)
				auditLogCommentColumnId = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "comment");

			if (auditLogCommentColumnId == -1) {
				throw new SkipException("Couldn't get Id for Column comment from TabListData Response for Entity " + entityName + " and Flow [" + flowToTest + "]");
			}

			String actualCommentValue = listData.get(0).get(auditLogCommentColumnId).get("value").trim();
			if (!actualCommentValue.equalsIgnoreCase("No")) {
				csAssert.assertTrue(false, "Expected Comment Value: No and Actual Comment Value: " + actualCommentValue);
			}

			//Verify Document
			if (auditLogDocumentColumnId == -1)
				auditLogDocumentColumnId = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "document");

			if (auditLogDocumentColumnId == -1) {
				throw new SkipException("Couldn't get Id for Column document from TabListData Response for Entity " + entityName + " and Flow [" + flowToTest + "]");
			}

			String actualDocumentValue = listData.get(0).get(auditLogDocumentColumnId).get("value").trim();
			if (!actualDocumentValue.equalsIgnoreCase("No")) {
				csAssert.assertTrue(false, "Expected Document Value: No and Actual Document Value: " + actualDocumentValue);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Audit Logs for Entity " + entityName + " and Forecast Processing Flow [" +
					flowToTest + "]. " + e.getMessage());
		}
	}

	private void verifyContractAuditLogHistory(String flowToTest, String tabListDataResponse, String templateFileName, CustomAssert csAssert) {
		try {
			logger.info("Verifying Contract Audit Log History for Flow [{}]", flowToTest);
			List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

			//Verify History
			String historyValue = getHistoryValue(flowToTest, tabListDataResponse, listData);
			Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

			if (historyId == -1L) {
				throw new SkipException("Couldn't get History Id from History Value for Flow [" + flowToTest + "]");
			}

			logger.info("Hitting FieldHistory API for Flow [{}]", flowToTest);
			FieldHistory historyObj = new FieldHistory();
			String historyResponse = historyObj.hitFieldHistory(historyId, contractEntityTypeId);

			JSONObject jsonObj = new JSONObject(historyResponse);
			jsonObj = jsonObj.getJSONArray("value").getJSONObject(0);

			//Verify History New Value
			if (!jsonObj.getString("newValue").trim().equalsIgnoreCase(templateFileName.trim())) {
				csAssert.assertTrue(false, "Expected History New Value: [" + templateFileName + "] and Actual New Value: [" +
						jsonObj.getString("newValue") + "]");
			}

			//Verify History Property
			if (!jsonObj.getString("property").trim().equalsIgnoreCase("Forecast Uploaded")) {
				csAssert.assertTrue(false, "Expected History Property Value: [Forecast Uploaded] and Actual Property Value: [" +
						jsonObj.getString("property") + "]");
			}

			//Verify History State
			if (!jsonObj.getString("state").trim().equalsIgnoreCase("ADDED")) {
				csAssert.assertTrue(false, "Expected History State Value: ADDED and Actual State Value: " + jsonObj.getString("state"));
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Contract Audit Log History for Forecast Processing Flow [" + flowToTest +
					"]. " + e.getMessage());
		}
	}

	private void verifyServiceDataAuditLogHistory(String flowToTest, String tabListDataResponse, CustomAssert csAssert) {
		try {
			logger.info("Verifying Service Data Audit Log History for Flow [{}]", flowToTest);
			List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

			//Verify History
			String historyValue = getHistoryValue(flowToTest, tabListDataResponse, listData);
			Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

			if (historyId == -1L) {
				throw new SkipException("Couldn't get History Id from History Value for Flow [" + flowToTest + "]");
			}

			logger.info("Hitting FieldHistory API for Flow [{}]", flowToTest);
			FieldHistory historyObj = new FieldHistory();
			String historyResponse = historyObj.hitFieldHistory(historyId, serviceDataEntityTypeId);

			JSONObject jsonObj = new JSONObject(historyResponse);
			JSONArray jsonArr = jsonObj.getJSONArray("value");

			if (jsonArr.length() != 0) {
				csAssert.assertTrue(false, "JSONArray [value] is not Empty in Service Data Field History API Response for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Contract Audit Log History for Forecast Processing Flow [" + flowToTest +
					"]. " + e.getMessage());
		}
	}

	private String getHistoryValue(String flowToTest, String tabListDataResponse, List<Map<Integer, Map<String, String>>> listData) {
		String historyValue = null;

		try {
			if (auditLogHistoryColumnId == -1)
				auditLogHistoryColumnId = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "history");

			if (auditLogHistoryColumnId == -1) {
				throw new SkipException("Couldn't get Id for Column history from TabListData Response for Flow [" + flowToTest + "]");
			}

			historyValue = listData.get(0).get(auditLogHistoryColumnId).get("value");
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while getting History Value from TabListDataResponse for Flow [{}]");
		}
		return historyValue;
	}
}