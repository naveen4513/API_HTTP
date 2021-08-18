package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class TestBulkUpdate extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestBulkUpdate.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String flowsConfigFileName = null;
	private Boolean killAllSchedulerTasks = false;
	private Boolean waitForScheduler = true;
	private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
	private String bulkUpdateExcelFilePath;
	private static Long schedulerWaitTimeout = 1200000L;
	private static Long schedulerPollingTime = 5000L;
	private static Map<String, String> templateShowPageFieldsMap = new HashMap<>();

	@BeforeClass
	public void beforeClass() throws ConfigurationException, IOException, ExecutionException, InterruptedException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkUpdateConfigFileName");
		flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsConfigFileName");
		bulkUpdateExcelFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelFilePath");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killAllSchedulerTasks");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			killAllSchedulerTasks = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitForScheduler");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			waitForScheduler = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitTimeout");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerWaitTimeout = Long.parseLong(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerPollingTime = Long.parseLong(temp);

		templateShowPageFieldsMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "templateFieldShowPageFieldMapping");

		testCasesMap = getTestCasesMapping();
	}

	@Test
	public void testBulkUpdateDownloadTemplate() {
		CustomAssert csAssert1 = new CustomAssert();

		try {
			logger.info("Validating Bulk Update Download Template.");
			String templateDownloadPath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkUpdateTemplateDownload",
					"templatePath");
			String templateDownloadName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkUpdateTemplateDownload",
					"templateName");
			int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkUpdateTemplateDownload",
					"templateId"));
			int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkUpdateTemplateDownload",
					"entityTypeId"));
			String entityIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkUpdateTemplateDownload",
					"entityIds");
			Boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(templateDownloadPath, templateDownloadName, templateId, entityTypeId, entityIds);

			if (!templateDownloaded) {
				logger.info("Bulk Update Template Download failed using Template Id {}, EntityTypeId {} and EntityIds {}.", templateId, entityTypeId,
						entityIds);
				csAssert1.assertTrue(templateDownloaded, "Bulk Update Template Download failed using Template Id " + templateId + ", EntityTypeId " +
						entityTypeId + " and EntityIds " + entityIds);
			} else {
				File templateFile = new File(templateDownloadPath + "/" + templateDownloadName);
				logger.info("Deleting downloaded template file for Bulk Update. File location: [{}]", templateDownloadPath + "/" + templateDownloadName);
				Boolean templateDeleted = templateFile.delete();
				if (templateDeleted) {
					logger.info("Bulk Update Template file at Location: [{}] deleted successfully.", templateDownloadPath + "/" + templateDownloadName);
				} else {
					logger.error("Couldn't delete Bulk Update Template file at Location: [{}]", templateDownloadPath + "/" + templateDownloadName);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating Bulk Update Download Template. {}", e.getMessage());
			csAssert1.assertTrue(false, "Exception while validating Bulk Update Download Template. " + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testBulkUpdateDownloadTemplate"), csAssert1);
		csAssert1.assertAll();
	}

	/*@DataProvider
	public Object[][] dataProviderForBulkUpdateFlows() throws ConfigurationException {
		List<Object[]> allTestData = new ArrayList<>();

		logger.info("Setting all Flows to Test.");
		List<String> allFlowsToTest = getFlowsToTest();
		for (String flowToTest : allFlowsToTest) {
			allTestData.add(new Object[]{flowToTest.trim()});
		}
		logger.info("Total Flows to Test : {}", allTestData.size());
		return allTestData.toArray(new Object[0][]);
	}*/

	private List<String> getFlowsToTest() {
		List<String> flowsToTest = new ArrayList<>();

		try {
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
			if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
				logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
				flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, flowsConfigFileName);
			} else {
				String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
				for (String flow : allFlows) {
					if (ParseConfigFile.containsSection(configFilePath, flowsConfigFileName, flow.trim())) {
						flowsToTest.add(flow.trim());
					} else {
						logger.info("Flow having name [{}] not found in Bulk Update Flows Config File.", flow.trim());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Flows to Test for Bulk Update Validation. {}", e.getMessage());
		}
		return flowsToTest;
	}

	@Test(priority = 1)
	public void testBulkUpdateFlows() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Setting all Flows to Test.");
			List<String> allFlowsToTest = getFlowsToTest();

			for (String flowToTest : allFlowsToTest) {
				String entityName = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "entity");
				logger.info("Verifying Bulk Update for Flow: [{}]", flowToTest);
				logger.info("Getting EntityTypeId for Entity: {}", entityName);
				int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
						ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), entityName));

				//Kill All Scheduler Tasks if Flag is On.
				if (killAllSchedulerTasks) {
					logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
					UserTasksHelper.removeAllTasks();
				}

				logger.info("Hitting Fetch API.");
				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

				//Upload Bulk Update Template
				String bulkTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "excelFileName");
				int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "templateId"));
				String bulkUpdateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(bulkUpdateExcelFilePath, bulkTemplateFileName, entityTypeId, templateId);
				String expectedBulkUpdateUploadResponse = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "uploadMessage");
				String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "expectedResult");
				if (expectedResult == null || !(expectedResult.trim().equalsIgnoreCase("failure") || expectedResult.trim().equalsIgnoreCase("failAtUpload")
						|| expectedResult.trim().equalsIgnoreCase("recordsProcessFalse")))
					expectedResult = "success";

				logger.info("Actual Upload Bulk Data Response: [{}] and Expected Upload Bulk Data Response: [{}]", bulkUpdateUploadResponse,
						expectedBulkUpdateUploadResponse);

				if (bulkUpdateUploadResponse == null) {
					logger.error("Upload Bulk Data Response for Bulk Update, Flow [{}] and Entity {} is Null.", flowToTest, entityName);
					csAssert.assertTrue(false, "Upload Bulk Data Response for Bulk Update, Flow [" + flowToTest + "] and Entity " + entityName + " is Null.");
				} else if (!bulkUpdateUploadResponse.trim().toLowerCase().contains(expectedBulkUpdateUploadResponse.trim().toLowerCase())) {
					logger.error("Upload Bulk Data Response received for Bulk Update, Flow [{}] and Entity {} does not match with required response. " +
							"Hence not proceeding further.", flowToTest, entityName);
					csAssert.assertTrue(false, "Upload Bulk Task Response received for Bulk Update, Flow [" + flowToTest + "] and Entity " + entityName +
							" does not match with required response. Hence not proceeding further.");
				} else if (expectedResult.trim().equalsIgnoreCase("success") || expectedResult.trim().equalsIgnoreCase("failure") ||
						expectedResult.trim().equalsIgnoreCase("recordsProcessFalse")) {
					if (waitForScheduler) {
						logger.info("Hitting Fetch API to Get Bulk Update Job Task Id for Flow [{}]", flowToTest);
						fetchObj.hitFetch();
						logger.info("Getting Task Id of Bulk Update Job for Flow [{}]", flowToTest);
						int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

						waitForScheduler(flowToTest, newTaskId, csAssert);

						logger.info("Hitting Fetch API to get Status of Bulk Update Job for Flow [{}]", flowToTest);
						fetchObj.hitFetch();
						String bulkUpdateJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

						if (bulkUpdateJobStatus != null && bulkUpdateJobStatus.trim().equalsIgnoreCase("Completed")) {

							if (expectedResult.trim().equalsIgnoreCase("success")) {
								if (UserTasksHelper.ifAllRecordsPassedInTask(newTaskId)) {
									List<Integer> entityIdsList = new ArrayList<>();
									String[] recordIds = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest,
											"recordIds").trim().split(Pattern.quote(","));

									for (String id : recordIds) {
										entityIdsList.add(Integer.parseInt(id.trim()));
									}

									Map<Integer, String> showPageJsonMap = getEntityShowPageJsonMap(entityIdsList, entityTypeId, flowToTest);

									//Verify Updated fields on Show Page
									List<String> fieldsUpdated = new ArrayList<>();
									String[] fieldsToUpdateStr = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest,
											"fieldsToUpdate").trim().split(Pattern.quote(","));

									for (String field : fieldsToUpdateStr) {
										fieldsUpdated.add(field.trim().toLowerCase());
									}

									logger.info("Total fields updated for Flow [{}] are {}", flowToTest, fieldsUpdated.size());
									List<Map<String, String>> updatedFieldsMap = getUpdatedFieldsMap(bulkTemplateFileName, fieldsUpdated, flowToTest);
									List<Map<String, String>> showPageFields = new ArrayList<>();

									for (Map<String, String> oneRecordUpdatedFieldsMap : updatedFieldsMap) {
										Map<String, String> showPageMap = new HashMap<>();
										for (String field : fieldsUpdated) {
											if (templateShowPageFieldsMap.containsKey(field.trim())) {
												showPageMap.put(templateShowPageFieldsMap.get(field.trim()), oneRecordUpdatedFieldsMap.get(field.trim()));
											} else {
												logger.info("Mapping of Field {} not found in TemplateFieldShowPageFieldMapping. Hence not validating this field for Flow [{}]",
														field.trim(), flowToTest);
												oneRecordUpdatedFieldsMap.remove(field.trim());
											}
										}
										showPageFields.add(showPageMap);
									}

									validateRecordsOnShowPage(flowToTest, entityName, entityTypeId, entityIdsList, showPageFields, showPageJsonMap, csAssert);
								} else {
									logger.error("Some records failed in Scheduler Task for Entity {} and Flow [{}]. Hence skipping further validation.", entityName, flowToTest);
									csAssert.assertTrue(false, "Some records failed in Scheduler Task for Entity " + entityName + " and Flow [" + flowToTest +
											"]. Hence skipping further validation.");
								}
							} else if (expectedResult.trim().equalsIgnoreCase("recordsProcessFalse")) {
								Integer noOfRecordsSubmitted = UserTasksHelper.getNoOfSubmittedRecordsForTask(fetchObj.getFetchJsonStr(), newTaskId);

								if (noOfRecordsSubmitted == -1) {
									logger.info("Couldn't get No of Records Submitted for Flow [{}]", flowToTest);
								} else if (noOfRecordsSubmitted != 0) {
									logger.error("Process Column is False for all Records and Still No of Records Submitted is {} for Flow [{}]", noOfRecordsSubmitted, flowToTest);
									csAssert.assertTrue(false, "Process Column is False for all Records and Still No of Records Submitted is " +
											noOfRecordsSubmitted + " for Flow [" + flowToTest + "]");
								}
							} else {
								//Verify that Bulk Update Job Failed.
								logger.info("Verifying that Bulk Update Job failed for Flow [{}]", flowToTest);
								csAssert.assertTrue(UserTasksHelper.ifAllRecordsFailedInTask(newTaskId), "All Records in Bulk Update Job Id didn't fail.");
							}
						} else {
							if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
								logger.error("Failing Test as Bulk Update Job for Flow [{}] is not completed yet and Flag \'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' " +
										"is set to True.", flowToTest);
								csAssert.assertTrue(false, "Failing Test as Bulk Update Job for Flow [{}] is not completed yet and Flag \'" +
										"FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True");
							} else {
								logger.info("Bulk Update Job for Flow [{}] is not completed yet and Flag \'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to False." +
										" Hence not Checking Status on Show Page.", flowToTest);
							}
						}
					} else {
						logger.info("Wait for Scheduler Flag is Turned Off. Hence not checking further for Flow [{}]", flowToTest);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying BulkUpdate. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying BulkUpdate. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testBulkUpdateFlows"), csAssert);
		csAssert.assertAll();
	}

	private void waitForScheduler(String flowToTest, int newTaskId, CustomAssert csAssert) {
		logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
		try {
			logger.info("Time Out for Scheduler is {} milliseconds", schedulerWaitTimeout);
			long timeSpent = 0;

			if (newTaskId != -1) {
				logger.info("Checking if Bulk Update Task has completed or not for Flow [{}]", flowToTest);

				while (timeSpent < schedulerWaitTimeout) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", schedulerPollingTime);
					Thread.sleep(schedulerPollingTime);

					logger.info("Hitting Fetch API.");
					Fetch fetchObj = new Fetch();
					fetchObj.hitFetch();

					logger.info("Getting Status of Bulk Update Task for Flow [{}]", flowToTest);
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						logger.info("Bulk Update Task Completed for Flow [{}]", flowToTest);
						break;
					} else {
						timeSpent += schedulerPollingTime;
						logger.info("Bulk Update Task is not finished yet for Flow [{}]", flowToTest);
					}
				}
			} else {
				logger.info("Couldn't get Bulk Update Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, schedulerWaitTimeout);
				Thread.sleep(schedulerWaitTimeout);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}

	private void validateRecordsOnShowPage(String flowToTest, String entityName, int entityTypeId, List<Integer> entityIdsList, List<Map<String, String>> showPageFields,
	                                       Map<Integer, String> showPageJsonMap, CustomAssert csAssert) {
		logger.info("Validating Records on Show Page for Flow [{}]", flowToTest);
		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		try {
			List<String> showPageFieldNames = new ArrayList<>(showPageFields.get(0).keySet());
			for (int i = 0; i < entityIdsList.size(); i++) {
				final int outerIndex = i;

				FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						Show showObj = new Show();
						logger.info("Hitting Show Api for Record #{} for Flow [{}] having EntityTypeId {} and Id {}", (outerIndex + 1), flowToTest, entityTypeId,
								entityIdsList.get(outerIndex));
						showObj.hitShow(entityTypeId, entityIdsList.get(outerIndex));
						String showJsonStr = showObj.getShowJsonStr();

						if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
							for (int j = 0; j < showPageFields.get(outerIndex).size(); j++) {
								String fieldName = showPageFieldNames.get(j);
								logger.info("Validating Field {} of Record #{} for Flow [{}] having EntityTypeId {} and Id {}", fieldName, (outerIndex + 1),
										flowToTest, entityTypeId, entityIdsList.get(outerIndex));

								String expectedValue = showPageFields.get(outerIndex).get(fieldName);
								Boolean result = showObj.verifyShowField(showJsonStr, fieldName, expectedValue, entityTypeId);

								if (!result) {
									logger.error("Field {} of Record #{} for Flow [{}] having EntityTypeId {} and Id {} didn't match on Show Page.", fieldName,
											(outerIndex + 1), flowToTest, entityTypeId, entityIdsList.get(outerIndex));
									csAssert.assertTrue(false, "Field " + fieldName + " of Record #" + (outerIndex + 1) + " for Flow [" + flowToTest +
											"] having EntityTypeId " + entityTypeId + " and Id " + entityIdsList.get(outerIndex) + " didn't match on Show Page.");
								}
							}
						} else {
							logger.error("Show API Response for Record #{} for Flow [{}] having EntityTypeId {} and Id {} is not a valid JSON.", (outerIndex + 1),
									flowToTest, entityTypeId, entityIdsList.get(outerIndex));
							csAssert.assertTrue(false, "Show API Response for Record #" + (outerIndex + 1) + " for Flow [" + flowToTest +
									"] having EntityTypeId " + entityTypeId + " and Id " + entityIdsList.get(outerIndex) + " is not a valid JSON.");
						}

						//Restore Record to Original State.
						EntityOperationsHelper.restoreRecord(entityName, entityIdsList.get(outerIndex), showPageJsonMap.get(entityIdsList.get(outerIndex)));

						return true;
					}
				});
				taskList.add(result);
				executor.execute(result);
			}
			for (FutureTask<Boolean> task : taskList)
				task.get();
		} catch (Exception e) {
			logger.error("Exception while Verifying Bulk Update Records on Show Page for Flow [{}] and Entity {}. {}", flowToTest, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Bulk Update Records on Show Page for Flow [" + flowToTest + "] and Entity " +
					entityName + ". " + e.getMessage());
		}
	}

	private List<Map<String, String>> getUpdatedFieldsMap(String bulkTemplateFileName, List<String> updatedFieldsLabels, String flowToTest) {
		List<Map<String, String>> updatedFields = new ArrayList<>();
		try {
			String sheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "sheetName");
			int noOfRecords = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "noOfRecords"));

			logger.info("Getting all Excel Headers for Flow [{}]", flowToTest);
			List<String> allHeaders = XLSUtils.getHeaders(bulkUpdateExcelFilePath, bulkTemplateFileName, sheetName);
			logger.info("Getting Excel Data of all the Records for Flow [{}]", flowToTest);
			List<List<String>> excelData = XLSUtils.getExcelDataOfMultipleRows(bulkUpdateExcelFilePath, bulkTemplateFileName, sheetName, 6, noOfRecords);

			for (int i = 0; i < noOfRecords; i++) {
				logger.info("Mapping Field Values for Record #{} of Flow [{}]", (i + 1), flowToTest);
				Map<String, String> oneRecordMap = new HashMap<>();
				for (int j = 0; j < allHeaders.size(); j++) {
					if (updatedFieldsLabels.contains(allHeaders.get(j).trim().toLowerCase())) {
						oneRecordMap.put(allHeaders.get(j).trim().toLowerCase(), excelData.get(i).get(j).trim());
					}
				}
				updatedFields.add(oneRecordMap);
			}
		} catch (Exception e) {
			logger.error("Exception while getting all updated values for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return updatedFields;
	}

	private Map<Integer, String> getEntityShowPageJsonMap(List<Integer> entityIdsList, int entityTypeId, String flowToTest) {
		Map<Integer, String> entityShowJsonMap = new HashMap<>();
		try {
			Show showObj = new Show();

			for (Integer entityId : entityIdsList) {
				logger.info("Hitting Show API for Entity Id {}, Record Id {} and Flow [{}]", entityTypeId, entityId, flowToTest);
				showObj.hitShow(entityTypeId, entityId);
				String showJsonStr = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
					entityShowJsonMap.put(entityId, showJsonStr);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Entity Show Page Json Map for EntityTypeId {} and Flow [{}]. {}", entityTypeId, flowToTest, e.getStackTrace());
		}
		return entityShowJsonMap;
	}
}
