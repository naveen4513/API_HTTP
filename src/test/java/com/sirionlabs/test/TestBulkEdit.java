package com.sirionlabs.test;

import com.sirionlabs.api.bulkedit.BulkeditCreate;
import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestBulkEdit extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestBulkEdit.class);
	private static String configFilePath;
	private static String configFileName;
	private static int maxRecordsForListData = 20;
	private static int listDataOffset = 0;
	private static int maxRecordsForBulkEdit = 3;
	private static int maxRecordsForShow = 3;
	private static Boolean killAllSchedulerTasks = false;
	private static String flowsConfigFileName = null;
	private static Boolean waitForScheduler = true;
	private static Boolean checkShowPageIsBlocked = true;
	private static Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("BulkEditConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("BulkEditConfigFileName");
		flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordsforlistdata");
		if (temp != null && !temp.equalsIgnoreCase("") && Integer.parseInt(temp) != 0)
			maxRecordsForListData = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listdataoffset");
		if (temp != null && !temp.equalsIgnoreCase(""))
			listDataOffset = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordsforbulkedit");
		if (temp != null && !temp.equalsIgnoreCase("") && Integer.parseInt(temp) != 0)
			maxRecordsForBulkEdit = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordsforshow");
		if (temp != null && !temp.equalsIgnoreCase("") && Integer.parseInt(temp) != 0)
			maxRecordsForShow = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killallschedulertasks");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			killAllSchedulerTasks = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforscheduler");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			waitForScheduler = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			failTestIfJobNotCompletedWithinSchedulerTimeOut = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "checkShowPageIsBlocked");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			checkShowPageIsBlocked = false;

		testCasesMap = getTestCasesMapping();
	}

	/*@DataProvider
	public Object[][] dataProviderForBulkEdit() throws ConfigurationException {
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
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
			if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
				logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
				flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, flowsConfigFileName);
			} else {
				String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(Pattern.quote(","));
				for (String flow : allFlows) {
					if (ParseConfigFile.containsSection(configFilePath, flowsConfigFileName, flow.trim())) {
						flowsToTest.add(flow.trim());
					} else {
						logger.info("Flow having name [{}] not found in Bulk Edit Flows Config File.", flow.trim());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Flows to Test for Bulk Edit Validation. {}", e.getMessage());
		}
		return flowsToTest;
	}

	@Test
	public void testBulkEdit() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Setting all Flows to Test.");
		List<String> allFlowsToTest = getFlowsToTest();
		for (String flowToTest : allFlowsToTest) {
			String entityName = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "entity");
			Map<Integer, String> originalPayloadMap = new HashMap<>();

			try {
				logger.info("Verifying Bulk Edit for Flow [{}]", flowToTest);
				logger.info("Getting Entity Type Id of Entity {} for Flow [{}]", entityName, flowToTest);
				int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
						ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), entityName));
				int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
						ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "entity_url_id"));

				String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + maxRecordsForListData +
						",\"orderByColumnName\":\"id\"," + "\"orderDirection\":\"desc\",\"filterJson\":{}}}";
				ListRendererListData listDataObj = new ListRendererListData();
				logger.info("Hitting ListRendererListData Api for EntityTypeId {} and Flow [{}]", entityTypeId, flowToTest);
				listDataObj.hitListRendererListData(listId, false, payloadForListData, null);
				listDataObj.setListData(listDataObj.getListDataJsonStr());
				List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();

				if (listData.size() > 0) {
					logger.info("Getting Column Id for BulkCheckBox Column");
					int columnIdForBulkCheckBox = listDataObj.getColumnIdFromColumnName("bulkcheckbox");
					logger.info("Getting Column Id for Status Column");
					int columnIdForStatus = listDataObj.getColumnIdFromColumnName("status");
					logger.info("Filtering List Data Records. i.e. Removing records which are already locked for Bulk Action.");
					List<Map<Integer, Map<String, String>>> filteredRecords = this.filterListDataRecords(listData, columnIdForBulkCheckBox, columnIdForStatus);

					if (filteredRecords.size() > 0) {
						//Kill All Scheduler Tasks if Flag is On.
						if (killAllSchedulerTasks) {
							logger.info("Killing All Scheduler Tasks for Flow [{}].", flowToTest);
							killAllSchedulerTasks();
						}

						logger.info("Hitting Fetch API.");
						Fetch fetchObj = new Fetch();
						fetchObj.hitFetch();
						List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

						String testField = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "field");

						ListRendererDefaultUserListMetaData listMetadatObj = new ListRendererDefaultUserListMetaData();
						logger.info("Hitting ListRendererDefaultUserListMetaData Api for Entity {} and Flow [{}]", entityName, flowToTest);
						listMetadatObj.hitListRendererDefaultUserListMetadata(listId, null, "{}");
						listMetadatObj.setFilterMetadatas(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());
						listMetadatObj.setColumns(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());

						logger.info("Proceeding for Field: {}", testField);
						logger.info("Getting Random Records from List Data for Entity {}", entityName);
						int[] randomNumbersForBulkEdit = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (filteredRecords.size() - 1), maxRecordsForBulkEdit);
						logger.info("Total Records selected from List Data {} for Entity {}", randomNumbersForBulkEdit.length, entityName);
						logger.info("Getting Ids for the Selected Records from List Data for Entity {}", entityName);
						String recordIds = this.setRecordIds(filteredRecords, randomNumbersForBulkEdit, listMetadatObj.getIdFromQueryName("id"));
						logger.info("Ids for the Selected  Records from List Data for Entity {} are : {}", entityName, recordIds);

						String idsForShow[] = recordIds.split(Pattern.quote(","));
						originalPayloadMap = getOriginalPayloadForRecords(flowToTest, entityName, idsForShow);

						String payloadForCreate = BulkeditCreate.getPayload(recordIds);
						BulkeditCreate createObj = new BulkeditCreate();
						String createJsonStr = createObj.hitBulkeditCreate(entityTypeId, payloadForCreate);

						Map<String, String> field = ParseJsonResponse.getFieldByLabel(createJsonStr, testField);
						if (field.size() == 0) {
							throw new SkipException("Couldn't find Details for Field " + testField + ". Hence skipping Validation for Entity " + entityName +
									" and Flow [" + flowToTest + "]");
						}

						String fieldValue = null;
						String payloadForEdit = null;
						String fieldType = null;

						if (field.containsKey("type") && field.get("type") != null)
							fieldType = field.get("type").trim().toLowerCase();

						String fieldLabel = field.get("label");
						String fieldName = field.get("name");
						Boolean isMultiple = Boolean.parseBoolean(field.get("multiple"));
						Boolean isDynamic = Boolean.parseBoolean(field.get("dynamicField"));

						String fieldIdsForPayload = PayloadUtils.getFieldIdsForBulkEditPayload(createJsonStr, field.get("id"));
						if (fieldIdsForPayload.trim().equalsIgnoreCase("")) {
							throw new SkipException("Couldn't get Field Ids for Bulk Edit Payload for Flow [" + flowToTest + "]. Hence skipping validation.");
						}

						if (fieldType != null) {
							switch (fieldType) {
								default:
									if (field.get("model") != null && field.get("model").equalsIgnoreCase("stakeHolders.values")) {
										fieldValue = getValueForField(flowToTest, fieldLabel, "stakeholder");
										int stakeholderId = -1;

										logger.info("Hitting Options API to get Id for Stakeholder {}.", fieldValue);
										Options optionObj = new Options();
										int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
												ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", "stakeholders"));
										Map<String, String> params = new HashMap<>();
										params.put("pageType", ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
												ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata"));
										params.put("query", fieldValue);
										optionObj.hitOptions(dropDownType, params);
										String ids = optionObj.getIds();

										if (ids == null) {
											throw new SkipException("Couldn't get Stakeholder Id for Stakeholder " + fieldValue + " and Flow [" + flowToTest +
													"]. Hence skipping further validation.");
										}

										if (ids.trim().contains(",")) {
											String allIds[] = ids.split(Pattern.quote(","));
											for (String id : allIds) {
												stakeholderId = Integer.parseInt(id.trim());
												if (Options.getNameFromId(optionObj.getOptionsJsonStr(), stakeholderId).trim().equalsIgnoreCase(fieldValue)) {
													break;
												}
											}
										} else {
											stakeholderId = Integer.parseInt(ids);
										}

										payloadForEdit = getPayloadForEdit(fieldValue, entityTypeId, fieldName, stakeholderId, recordIds, fieldIdsForPayload);
										fieldName = "stakeholders";
										fieldType = "stakeholder";
									} else {
										fieldValue = getValueForField(flowToTest, fieldLabel, fieldType);
										payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, fieldIdsForPayload);
									}
									break;

								case "select":
									//Get all available options for Field
									List<Map<String, String>> allOptions = ParseJsonResponse.getAllOptionsForField(createJsonStr, fieldName,
											Boolean.getBoolean(field.get("dynamicField")));

									String value = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "value");
									if (value != null && !value.equalsIgnoreCase("")) {
										fieldValue = value.trim();

										for (Map<String, String> optionsMap : allOptions) {
											if (optionsMap.get("name").trim().equalsIgnoreCase(fieldValue)) {
												payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, optionsMap.get("id"), fieldIdsForPayload);
												break;
											}
										}
										break;
									}

									logger.info("Total Available Options for Field {} are {}", fieldLabel, allOptions.size());

									if (allOptions.size() > 0) {
										int randomNumbersForSelect = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
										fieldValue = allOptions.get(randomNumbersForSelect).get("name");
										payloadForEdit = getPayloadForEdit(field, fieldValue, entityTypeId, recordIds, allOptions.get(randomNumbersForSelect).get("id"),
												fieldIdsForPayload);
										logger.info("Verifying BulkEdit For Field {} having Value {}", fieldLabel, fieldValue);
									} else
										logger.info("No Option found for Field {} and Entity {}", fieldLabel, entityName);
									break;
							}
						} else {
							throw new SkipException("Null Field Type for Field " + fieldLabel + " of Entity " + entityName + " and Flow [" + flowToTest +
									"]. Hence Skipping Validation.");
						}

						if(payloadForEdit == null){
							csAssert.assertTrue(false,"Payload for bulk edit is null for the flow " + flowToTest);
							continue;
						}

						logger.info("Hitting BulkEdit Api for Field {} and Entity {} having Value {}", fieldLabel, entityName, fieldValue);
						BulkeditEdit editObj = new BulkeditEdit();

						if(payloadForEdit == null){
							csAssert.assertTrue(false,"Payload for bulk edit is null for the flow " + flowToTest);
							continue;
						}
						editObj.hitBulkeditEdit(entityTypeId, payloadForEdit);
						String editJsonStr = editObj.getBulkeditEditJsonStr();
						boolean isValidJson = ParseJsonResponse.validJsonResponse(editJsonStr);

						if (isValidJson) {
							//Verify Response
							JSONObject jsonObj = new JSONObject(editJsonStr);
							String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().toLowerCase();
							csAssert.assertTrue(status.equalsIgnoreCase("success"), "Expected Status: success and Actual Status: " + status);

							if (status.equalsIgnoreCase("success")) {
								String expectedNotificationMessage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
										"notificationmessage").trim().toLowerCase();
								jsonObj = new JSONObject(editJsonStr);
								String actualNotificationMessage = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties")
										.getString("notification").trim().toLowerCase();

								if (!actualNotificationMessage.contains(expectedNotificationMessage)) {
									csAssert.assertTrue(false, "Expected Notification Message: " + expectedNotificationMessage +
											" and Actual Notification Message: " + actualNotificationMessage + " doesn't match. Hence skipping further validation for Flow [" +
											flowToTest + "]");
									return;
								} else if (waitForScheduler) {
									//Validate Show Page
									int randomNumbersForShow[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, (idsForShow.length - 1), maxRecordsForShow);
									int entityIdsForShow[] = new int[randomNumbersForShow.length];
									for (int j = 0; j < randomNumbersForShow.length; j++)
										entityIdsForShow[j] = Integer.parseInt(idsForShow[randomNumbersForShow[j]]);

									logger.info("Hitting Fetch API to Get Bulk Edit Job Task Id");
									fetchObj.hitFetch();
									logger.info("Getting Task Id of Bulk Edit Job for Flow [{}]", flowToTest);
									int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

									waitForScheduler(flowToTest, entityTypeId, entityIdsForShow, newTaskId, csAssert);

									logger.info("Hitting Fetch API to get Status of Bulk Edit Job");
									fetchObj.hitFetch();
									String bulkEditJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

									if (bulkEditJobStatus != null && bulkEditJobStatus.trim().equalsIgnoreCase("Completed")) {
										//Check Status on Show Page
										this.verifyDataOnShowPage(flowToTest, entityTypeId, entityIdsForShow, fieldName, fieldValue, fieldType, isMultiple,
												isDynamic, originalPayloadMap, csAssert);
									} else {
										if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
											logger.error("Failing Test as Bulk Edit Job for Flow [{}] is not completed yet and Flag " +
													"\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True.", flowToTest);
											csAssert.assertTrue(false, "Failing Test as Bulk Edit Job for Flow [{}] is not completed yet and Flag \'" +
													"FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to True");
										} else {
											throw new SkipException("Bulk Edit Job for Flow [" + flowToTest + "] is not completed yet and Flag " +
													"\'FailTestIfJobNotCompletedWithinSchedulerTimeOut\' is set to False. Hence not Checking Status on Show Page.");
										}
									}
								} else {
									logger.info("Wait for Scheduler Flag is Turned Off. Hence not checking further for Flow [{}]", flowToTest);
								}
							} else {
								csAssert.assertTrue(false, "BulkEdit Response for Field " + fieldLabel + " and Entity " + entityName + " having Value " +
										fieldValue + " is unsuccessful.");
							}
						} else {
							csAssert.assertTrue(false, "Invalid Bulk Edit Json Response for Field " + fieldLabel + " and Entity " + entityName);
						}
					} else {
						throw new SkipException("No Record found after Filtering Records for Flow [" + flowToTest + "] and Entity " + entityName);
					}
				} else {
					throw new SkipException("No Record found in List Data for Flow [" + flowToTest + "] and Entity " + entityName);
				}
			} catch (SkipException e) {
				addTestResultAsSkip(getTestCaseIdForMethodName("testBulkEdit"), csAssert);
				throw new SkipException(e.getMessage());
			} catch (Exception e) {
				logger.error("Exception while Verifying BulkEdit for Flow [{}]. {}", flowToTest, e.getStackTrace());
				csAssert.assertFalse(true, "Exception while Verifying BulkEdit for Flow [" + flowToTest + "]. " + e.getMessage());
			} finally {
				restoreRecords(flowToTest, entityName, originalPayloadMap);
			}
		}

		addTestResult(getTestCaseIdForMethodName("testBulkEdit"), csAssert);
		csAssert.assertAll();
	}

	private void waitForScheduler(String flowToTest, int entityTypeId, int entityIdsForShow[], int newTaskId, CustomAssert csAssert) {
		logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
		try {
			long timeOut = 1200000;
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerwaittimeout").trim();
			if (NumberUtils.isParsable(temp))
				timeOut = Long.parseLong(temp);

			logger.info("Time Out for Scheduler is {} milliseconds", timeOut);
			long timeSpent = 0;

			if (newTaskId != -1) {
				logger.info("Checking if Bulk Edit Task has completed or not for Flow [{}]", flowToTest);
				long pollingTime = 5000;
				temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerpollingtime").trim();
				if (NumberUtils.isParsable(temp))
					pollingTime = Long.parseLong(temp);

				while (timeSpent < timeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
					Thread.sleep(pollingTime);

					logger.info("Hitting Fetch API.");
					Fetch fetchObj = new Fetch();
					fetchObj.hitFetch();

					logger.info("Getting Status of Bulk Edit Task for Flow [{}]", flowToTest);
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						logger.info("Bulk Edit Task Completed for Flow [{}]", flowToTest);
						break;
					} else {
						timeSpent += pollingTime;
						logger.info("Bulk Edit Task is not finished yet for Flow [{}]", flowToTest);
					}

					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
						if (!UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId) &&
								!UserTasksHelper.anyRecordProcessedInTask(fetchObj.getFetchJsonStr(), newTaskId)) {

							//Verify that Show Page is not accessible for entities
							if (checkShowPageIsBlocked)
								this.checkShowPageIsBlocked(flowToTest, entityTypeId, entityIdsForShow, csAssert);
						} else {
							logger.info("Bulk Edit Task for Flow [{}] is In Progress but At-least One record has been processed or failed. " +
									"Hence Not Checking if Show Page is Blocked or not.", flowToTest);
						}
					} else {
						logger.info("Bulk Edit Task for Flow [{}] has not been picked by Scheduler yet.", flowToTest);
					}
				}
			} else {
				logger.info("Couldn't get Bulk Edit Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, timeOut);
				Thread.sleep(timeOut);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}

	private String setRecordIds(List<Map<Integer, Map<String, String>>> listData, int[] indexArray, int columnId) {
		String entityIds = "";
		boolean first = true;
		try {
			for (int index : indexArray) {
				if (first) {
					entityIds = entityIds.concat(listData.get(index).get(columnId).get("valueId"));
					first = false;
				} else
					entityIds = entityIds.concat("," + listData.get(index).get(columnId).get("valueId"));
			}
		} catch (Exception e) {
			logger.error("Exception while setting Entity Ids in TestBulkEdit. {}", e.getMessage());
		}
		return entityIds;
	}

	private String getValueForField(String flowToTest, String fieldLabel, String fieldType) throws ConfigurationException {
		String fieldValue = null;

		if (ParseConfigFile.hasProperty(configFilePath, flowsConfigFileName, flowToTest, "value")) {
			String value = ParseConfigFile.getValueFromConfigFile(configFilePath, flowsConfigFileName, flowToTest, "value");
			if (value != null && !value.equalsIgnoreCase(""))
				return value;
		}

		switch (fieldType) {
			case "text":
			case "textarea":
				String basePrefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultvalues", "text");
				String currTime = Long.toString(System.currentTimeMillis());
				currTime = currTime.substring(currTime.length() - 3, currTime.length());
				fieldValue = basePrefix + currTime;
				break;

			case "date":
				fieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultvalues", "date");
				break;

			case "stakeholder":
				fieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultvalues", "stakeholder");
				break;

			case "number":
				fieldValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultValues", "number");
				break;

			default:
				logger.warn("Field Type {} not defined. Hence returning Null Value", fieldType);
				break;
		}
		if (fieldValue == null)
			logger.warn("Couldn't get Value for Field {}", fieldLabel);
		return fieldValue;
	}

	private String getPayloadForEdit(Map<String, String> field, String expectedValue, int entityTypeId, String entityIds, String fieldIdsForPayload)
			throws ConfigurationException {
		return getPayloadForEdit(field, expectedValue, entityTypeId, entityIds, null, fieldIdsForPayload);
	}

	private String getPayloadForEdit(Map<String, String> field, String expectedValue, int entityTypeId, String entityIds, String fieldId, String fieldIdsForPayload)
			throws ConfigurationException {
		String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
				+ "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":0}},";
		String temp;

		switch (field.get("type").trim().toLowerCase()) {
			default:
				temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":\"" + expectedValue + "\"}";
				break;

			case "select":
				if (field.get("multiple").equalsIgnoreCase("true"))
					temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":[{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}]}";
				else
					temp = "\"" + field.get("name") + "\":{\"name\":\"" + field.get("name") + "\",\"values\":{\"name\":\"" + expectedValue + "\",\"id\":" + fieldId + "}}";

				break;
		}
		if (field.get("dynamicField").equalsIgnoreCase("true"))
			payload += "\"dynamicMetadata\":{" + temp + "}";
		else
			payload += temp;
		payload += "},\"globalData\":{\"entityIds\":[" + entityIds + "],\"fieldIds\": [" + fieldIdsForPayload + "],\"isGlobalBulk\":true}}}";

		return payload;
	}

	//For Stakeholder Field
	private String getPayloadForEdit(String expectedValue, int entityTypeId, String roleGroup, int id, String entityIds, String fieldIdsForPayload) {
		String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + entityTypeId
				+ "},\"searchParam\":{\"offset\":{\"name\":\"offset\",\"values\":0},\"size\":{\"name\":\"size\",\"values\":0}},";
		String temp = "\"stakeHolders\":{\"name\":\"stakeHolders\",\"values\":{\"" + roleGroup + "\":{\"values\":[{\"id\":" + id + ",\"name\":\"" + expectedValue + "\"}]}}}";
		payload += temp + "},\"globalData\":{\"entityIds\":[" + entityIds + "],\"fieldIds\": [" + fieldIdsForPayload + "],\"isGlobalBulk\":true}}}";
		return payload;
	}

	private void checkShowPageIsBlocked(String flowToTest, int entityTypeId, int entityIds[], CustomAssert csAssert) {
		try {
			logger.info("Verifying that Show Page is blocked for Flow [{}], Entity Type Id {} and Entity Ids {}", flowToTest, entityTypeId, Arrays.toString(entityIds));
			ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
			List<FutureTask<Boolean>> taskList = new ArrayList<>();

			logger.info("Total Records for EntityTypeId {} are {}", entityTypeId, entityIds.length);
			for (int i = 0; i < entityIds.length; i++) {
				final int index = i;

				FutureTask<Boolean> result = new FutureTask<>(() -> {
					Show showObj = new Show();
					logger.info("Hitting Show Api for Record #{} for Flow [{}] having EntityTypeId {} and Id {}", (index + 1), flowToTest, entityTypeId, entityIds[index]);
					showObj.hitShow(entityTypeId, entityIds[index]);
					String showJsonStr = showObj.getShowJsonStr();
					boolean showPageBlocked = showObj.isShowPageBlockedForBulkAction(showJsonStr);
					if (!showPageBlocked) {
						logger.error("Show Page is accessible for Record #{} for Flow [{}] having EntityTypeId {} and Id {}", (index + 1), flowToTest, entityTypeId,
								entityIds[index]);
						csAssert.assertTrue(false, "Show Page is accessible for Record #" + (index + 1) + " for Flow [" + flowToTest +
								"] having EntityTypeId " + entityTypeId + " and Id " + entityIds[index]);
					}
					return true;
				});
				taskList.add(result);
				executor.execute(result);
			}
			for (FutureTask<Boolean> task : taskList)
				task.get();
		} catch (Exception e) {
			logger.error("Exception while Checking if Show Page is Blocked for Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Checking if Show Page is Blocked for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}

	private void verifyDataOnShowPage(String flowToTest, int entityTypeId, int recordIds[], String fieldName, String fieldValue, String fieldType, Boolean isMultiple,
	                                  Boolean isDynamic, Map<Integer, String> originalPayloadMap, CustomAssert csAssert) throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		logger.info("Total Records to validate for Flow [{}] and EntityTypeId {} : {}", flowToTest, entityTypeId, recordIds.length);

		for (int j = 0; j < recordIds.length; j++) {
			final int index = j;
			logger.info("Verifying that Show Page is Accessible for Flow [{}], EntityTypeId {} and EntityId {}", flowToTest, entityTypeId, recordIds[index]);

			FutureTask<Boolean> result = new FutureTask<>(() -> {
				try {
					/*
					Special Handling for Select type fields. Verify Data on Show Page only if that Record has the field option on Edit Page.
					Otherwise the field will get updated but wouldn't fail in Bulk Edit Feature.
					 */

					if (fieldType.trim().equalsIgnoreCase("select")) {
						String editGetResponse = originalPayloadMap.get(recordIds[index]);
						List<Map<String, String>> allOptions = ParseJsonResponse.getAllOptionsForField(editGetResponse, fieldName, isDynamic);

						Boolean optionPresentOnEditPage = false;
						if (allOptions != null) {
							for (Map<String, String> optionMap : allOptions) {
								if (optionMap.get("name").trim().equalsIgnoreCase(fieldValue.trim())) {
									optionPresentOnEditPage = true;
									break;
								}
							}
						}

						if (!optionPresentOnEditPage) {
							logger.info("Record having Id {} doesn't have the Option {} for Field {} on Edit Page. Hence no need to validate data on Show Page.",
									recordIds[index], fieldValue, fieldName);
							return true;
						}
					}

					Show showObj = new Show();
					logger.info("Hitting Show Api for Record #{} for Flow [{}] having EntityTypeId {} and Id {}", (index + 1), flowToTest, entityTypeId, recordIds[index]);
					showObj.hitShow(entityTypeId, recordIds[index]);
					String showJsonStr = showObj.getShowJsonStr();
					boolean isShowPageAccessible = showObj.isShowPageAccessibleForBulkEdit(showJsonStr);

					if (isShowPageAccessible) {
						//Verify Field Value
						logger.info("Verifying Field {} for Record #{} for Flow [{}]", fieldName, (index + 1), flowToTest);
						boolean casePass;
						casePass = showObj.verifyShowField(showJsonStr, fieldName, fieldValue, entityTypeId, fieldType, isMultiple, isDynamic);
						if (!casePass) {
							logger.error("Record #{} for Flow [{}] having EntityTypeId {} and Id {} failed on Show Page for Field {}", (index + 1), flowToTest,
									entityTypeId, recordIds[index], fieldName);
							csAssert.assertTrue(false, "Record #" + (index + 1) + " for Flow [" + flowToTest + "] having EntityTypeId " +
									entityTypeId + " and Id " + recordIds[index] + " failed on Show Page for Field " + fieldName);
						}
					} else {
						logger.error("Show Page is not accessible for Record #{} for Flow [{}] having EntityTypeId {} and Id {}", (index + 1), flowToTest,
								entityTypeId, recordIds[index]);
						csAssert.assertTrue(false, "Show Page is not accessible for Record #" + (index + 1) + " for Flow [" + flowToTest +
								"] having EntityTypeId " + entityTypeId + " and Id " + recordIds[index]);
					}
				} catch (Exception e) {
					logger.error("Exception while Verifying Show Page for Record for Flow [{}] having EntityTypeId {} and Id {}. {}", flowToTest, entityTypeId,
							recordIds[index], e.getStackTrace());
					csAssert.assertTrue(false, "Exception while Verifying Show Page for Record having EntityTypeId " + entityTypeId + " and Id " +
							recordIds[index] + ". " + e.getMessage());
				}
				return true;
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();
	}

	private List<Map<Integer, Map<String, String>>> filterListDataRecords(List<Map<Integer, Map<String, String>>> listDataRecords, int columnIdForBulkCheckBox,
	                                                                      int columnIdForStatus) {
		List<Map<Integer, Map<String, String>>> filteredRecords = new ArrayList<>();
		filteredRecords.addAll(listDataRecords);

		try {
			for (int i = 0; i < filteredRecords.size(); ) {
				Map<Integer, Map<String, String>> record = filteredRecords.get(i);
				if (record.get(columnIdForBulkCheckBox).get("value").toLowerCase().contains("true") ||
						record.get(columnIdForStatus).get("value").trim().equalsIgnoreCase("Archived"))
					filteredRecords.remove(i);
				else
					i++;
			}
		} catch (Exception e) {
			logger.error("Exception while filtering List Data Records. {}", e.getMessage());
		}
		return filteredRecords;
	}

	private void killAllSchedulerTasks() {
		UserTasksHelper.removeAllTasks();
	}

	private Map<Integer, String> getOriginalPayloadForRecords(String flowToTest, String entityName, String[] recordIds) {
		Map<Integer, String> recordsPayloadMap = new HashMap<>();

		try {
			Edit editObj = new Edit();

			for (String id : recordIds) {
				Integer recordId = Integer.parseInt(id);

				logger.info("Hitting Edit Get API for Record Id {}, Entity {} and Flow [{}]", recordId, entityName, flowToTest);
				String editGetResponse = editObj.hitEdit(entityName, recordId);

				if (!ParseJsonResponse.validJsonResponse(editGetResponse))
					throw new SkipException("Edit Get API Response for Record Id " + recordId + ", Entity " + entityName + " and Flow [" + flowToTest + "] is an Invalid JSON.");

				recordsPayloadMap.put(recordId, editGetResponse);
			}
		} catch (Exception e) {
			throw new SkipException("Exception occurred while creating Original Payload Map for Records and Flow [" + flowToTest + "]. " + e.getMessage());
		}
		return recordsPayloadMap;
	}

	private void restoreRecords(String flowToTest, String entityName, Map<Integer, String> originalPayloadMap) {
		try {
			logger.info("Restoring all records to its original state for Flow [{}] and Entity {}", flowToTest, entityName);

			for (Map.Entry<Integer, String> record : originalPayloadMap.entrySet()) {
				EntityOperationsHelper.restoreRecord(entityName, record.getKey(), originalPayloadMap.get(record.getKey()));
			}
		} catch (Exception e) {
			logger.error("Exception while Restoring Records for Flow [{}] and Entity {}. {}", flowToTest, entityName, e.getStackTrace());
		}
	}
}
