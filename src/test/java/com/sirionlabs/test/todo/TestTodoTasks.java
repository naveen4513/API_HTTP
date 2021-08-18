package com.sirionlabs.test.todo;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.todo.TodoDaily;
import com.sirionlabs.api.todo.TodoWeekly;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTodoTasks extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestTodoTasks.class);
	private static String configFilePath;
	private static String configFileName;
	private int bufferBeforeDate;
	private int bufferAfterDate;
	private String dateFormat;
	private boolean applyRandomization = false;
	private int maxRecordsToValidate = 3;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TodoConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("TodoTasksConfigFileName");
		bufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferbefore"));
		bufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferafter"));
		dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
		if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
			maxRecordsToValidate = Integer.parseInt(temp);

		testCasesMap = getTestCasesMapping();
	}

	/*@DataProvider
	public Object[][] dataProviderForDailyTasks() throws ConfigurationException {
		List<Object[]> dailyTasksToTest = new ArrayList<>();
		logger.info("Hitting Todo Daily Api");
		TodoDaily todoDailyObj = new TodoDaily();
		todoDailyObj.hitTodoDaily();

		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {
			todoDailyObj.setAllEntities(todoDailyObj.getTodoDailyJsonStr());
			List<Map<String, String>> dailyTasks = todoDailyObj.getTasks();
			logger.info("Total Daily Tasks found: {}", dailyTasks.size());

			if (dailyTasks.size() > 0) {
				List<Map<String, String>> tasksToValidate = new ArrayList<>();
				if (applyRandomization) {
					logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
					int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, dailyTasks.size() - 1, maxRecordsToValidate);
					for (int randomNumber : randomNumbersForRecords) {
						tasksToValidate.add(dailyTasks.get(randomNumber));
					}
				} else {
					tasksToValidate.addAll(dailyTasks);
				}

				for (Map<String, String> approval : tasksToValidate) {
					dailyTasksToTest.add(new Object[]{approval});
				}
			}
		}
		return dailyTasksToTest.toArray(new Object[0][]);
	}*/

	@Test
	public void testDailyTasks() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Hitting Todo Daily Api");
		TodoDaily todoDailyObj = new TodoDaily();
		todoDailyObj.hitTodoDaily();

		if (ParseJsonResponse.validJsonResponse(todoDailyObj.getTodoDailyJsonStr())) {
			todoDailyObj.setAllEntities(todoDailyObj.getTodoDailyJsonStr());
			List<Map<String, String>> dailyTasks = todoDailyObj.getTasks();
			logger.info("Total Daily Tasks found: {}", dailyTasks.size());

			if (dailyTasks.size() > 0) {
				List<Map<String, String>> tasksToValidate = new ArrayList<>();
				if (applyRandomization) {
					logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
					int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, dailyTasks.size() - 1, maxRecordsToValidate);
					for (int randomNumber : randomNumbersForRecords) {
						tasksToValidate.add(dailyTasks.get(randomNumber));
					}
				} else {
					tasksToValidate.addAll(dailyTasks);
				}

				for (Map<String, String> task : tasksToValidate) {
					verifyTodoTasks(task, csAssert);
				}
			}
		}

		addTestResult(getTestCaseIdForMethodName("testDailyTasks"), csAssert);
		csAssert.assertAll();
	}

	/*@DataProvider
	public Object[][] dataProviderForWeeklyTasks() throws ConfigurationException {
		List<Object[]> weeklyTasksToTest = new ArrayList<>();
		logger.info("Hitting Todo Weekly Api");
		TodoWeekly todoWeeklyObj = new TodoWeekly();
		todoWeeklyObj.hitTodoWeekly();

		if (ParseJsonResponse.validJsonResponse(todoWeeklyObj.getTodoWeeklyJsonStr())) {
			todoWeeklyObj.setAllEntities(todoWeeklyObj.getTodoWeeklyJsonStr());
			List<Map<String, String>> weeklyTasks = todoWeeklyObj.getTasks();
			logger.info("Total Weekly Tasks found: {}", weeklyTasks.size());

			if (weeklyTasks.size() > 0) {
				List<Map<String, String>> tasksToValidate = new ArrayList<>();
				if (applyRandomization) {
					logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
					int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, weeklyTasks.size() - 1, maxRecordsToValidate);
					for (int randomNumber : randomNumbersForRecords) {
						tasksToValidate.add(weeklyTasks.get(randomNumber));
					}
				} else {
					tasksToValidate.addAll(weeklyTasks);
				}

				for (Map<String, String> task : tasksToValidate) {
					weeklyTasksToTest.add(new Object[]{task});
				}
			}
		}
		return weeklyTasksToTest.toArray(new Object[0][]);
	}*/

	@Test
	public void testWeeklyTasks() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Hitting Todo Weekly Api");
		TodoWeekly todoWeeklyObj = new TodoWeekly();
		todoWeeklyObj.hitTodoWeekly();

		if (ParseJsonResponse.validJsonResponse(todoWeeklyObj.getTodoWeeklyJsonStr())) {
			todoWeeklyObj.setAllEntities(todoWeeklyObj.getTodoWeeklyJsonStr());
			List<Map<String, String>> weeklyTasks = todoWeeklyObj.getTasks();
			logger.info("Total Weekly Tasks found: {}", weeklyTasks.size());

			if (weeklyTasks.size() > 0) {
				List<Map<String, String>> tasksToValidate = new ArrayList<>();
				if (applyRandomization) {
					logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
					int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, weeklyTasks.size() - 1, maxRecordsToValidate);
					for (int randomNumber : randomNumbersForRecords) {
						tasksToValidate.add(weeklyTasks.get(randomNumber));
					}
				} else {
					tasksToValidate.addAll(weeklyTasks);
				}

				for (Map<String, String> task : tasksToValidate) {
					verifyTodoTasks(task, csAssert);
				}
			}
		}

		addTestResult(getTestCaseIdForMethodName("testWeeklyTasks"), csAssert);
		csAssert.assertAll();
	}

	private void verifyTodoTasks(Map<String, String> record, CustomAssert csAssert) {
		try {
			int entityTypeId = Integer.parseInt(record.get("entityTypeId"));

			Show showObj = new Show();
			logger.info("Hitting Show Api for Record having EntityTypeId {} and Id {}", record.get("entityTypeId"), record.get("id"));
			showObj.hitShow(Integer.parseInt(record.get("entityTypeId")), Integer.parseInt(record.get("id")));
			String showJsonStr = showObj.getShowJsonStr();

			if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
				logger.error("Invalid Show Json Response for Record having EntityTypeId {} and Id {}", record.get("entityTypeId"), record.get("id"));
				csAssert.assertTrue(false, "Invalid Show Json Response for Record having EntityTypeId " + record.get("entityTypeId") + " and Id " +
						record.get("id"));
			} else {
				logger.info("Verifying Record");

				//Verify if No Error
				logger.info("Verifying if No Error in response");
				JSONObject jsonObj = new JSONObject(showJsonStr);
				jsonObj = jsonObj.getJSONObject("header").getJSONObject("response");

				if (jsonObj.getString("status").equalsIgnoreCase("applicationError")) {
					logger.error("Application Error in Show Response for Record having EntityId {} and Id {}.", entityTypeId, record.get("id"));
					csAssert.assertTrue(false, "Application Error in Show Response for Record having EntityTypeId " + entityTypeId + " and Id " +
							record.get("id"));
				} else {
					String failureMsg;
					boolean resultPass;

					//Verify Id
					logger.info("Verifying Id");
					resultPass = showObj.verifyShowField(showJsonStr, "id", record.get("id"), entityTypeId, "int");
					if (!resultPass) {
						failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field Id";
						csAssert.assertTrue(false, failureMsg);
					}

					//Verify EntityTypeId
					logger.info("Verifying EntityTypeId");
					resultPass = showObj.verifyShowField(showJsonStr, "entitytypeid", Integer.toString(entityTypeId), entityTypeId, "int");
					if (!resultPass) {
						failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field EntityTypeId";
						csAssert.assertTrue(false, failureMsg);
					}

					//Verify Supplier
					if (record.get("supplier") != null && !record.get("supplier").trim().equalsIgnoreCase("")) {
						logger.info("Verifying Supplier");
						resultPass = showObj.verifyShowField(showJsonStr, "supplier", record.get("supplier"), entityTypeId, "text");
						if (!resultPass) {
							failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Field Supplier";
							csAssert.assertTrue(false, failureMsg);
						}
					} else {
						logger.info("Couldn't get Supplier for Record having Id {}. Hence not verifying Supplier.", record.get("id"));
					}

					//Verify Due Date
					/*long dueDateTimeStamp = Long.parseLong(record.get("dueDateTimeStamp"));
					if (dueDateTimeStamp != 0) {
						logger.info("Verifying Due Date");
						Date date = new Date(dueDateTimeStamp);
						SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
						String actualDate = sdf.format(date);
						String beforeDate = DateUtils.getDateOfXDaysFromYDate(actualDate, (-bufferBeforeDate), dateFormat);
						String afterDate = DateUtils.getDateOfXDaysFromYDate(actualDate, bufferAfterDate, dateFormat);
						String expectedDateRange = beforeDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + afterDate;

						String fieldName = null;
						if (ParseConfigFile.hasProperty(configFilePath, configFileName, "todoshowpageduedatemapping", String.valueOf(entityTypeId)))
							fieldName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todoshowpageduedatemapping",
									String.valueOf(entityTypeId));

						else if (ParseConfigFile.hasProperty(configFilePath, configFileName, "todoshowpageduedatemapping", "0"))
							fieldName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todoshowpageduedatemapping", "0");

						else {
							logger.info("Todo Show Page Due Date Mapping not found for Entity Type Id {} and also Default Mapping is not present. " +
									"Hence skipping Due Date Validation");
						}

						if (fieldName != null) {
							resultPass = showObj.verifyShowField(showJsonStr, fieldName.trim().toLowerCase(), expectedDateRange, entityTypeId, "date",
									dateFormat);
							if (!resultPass) {
								failureMsg = "Record having EntityTypeId " + entityTypeId + " and Id " + record.get("id") + " failed on Show Page for Due Date";
								csAssert.assertTrue(false, failureMsg);
							}
						}
					}*/
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Tasks. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying Tasks. " + e.getMessage());
		}
	}
}