package com.sirionlabs.test.calendar;

import com.sirionlabs.api.calendar.CalendarV1Data;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by akshay.rohilla on 7/11/2017.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestCalendarV1Data extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestCalendarV1Data.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String queryUserName = null;
	private int bufferBeforeDate;
	private int bufferAfterDate;
	private String dateFormat;
	private String calendarA;
	private String userId;
	private boolean applyRandomization = false;
	private int maxRecordsToValidate = 3;

	@BeforeClass(groups = { "minor" })
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CalendarConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CalendarV1DataConfigFileName");
		calendarA = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendarA");
		queryUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "queryUsername");
		bufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateValidationBufferBefore"));
		bufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateValidationBufferAfter"));
		dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateFormat");
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyRandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordsToValidate");
		if (temp != null && !temp.trim().equalsIgnoreCase("") && org.apache.commons.lang3.math.NumberUtils.isParsable(temp))
			maxRecordsToValidate = Integer.parseInt(temp);

		setUserId();

		testCasesMap = getTestCasesMapping();
	}

	/*@DataProvider
	public Object[][] dataProviderForCalendarV1DataAPIResponse() throws ConfigurationException {
		List<Object[]> allTestData = new ArrayList<>();
		logger.info("Setting all Calendar Months to Test for Calendar V1 Data API Response.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		int totalMonths = getNoOfMonthsAfter() + 1;
		logger.info("Total Calendar Months to be Tested for Calendar V1 Data API Response are: {}", totalMonths);
		while (totalMonths > 0) {
			allTestData.add(new Object[]{month, year});
			month++;
			if (month > 11) {
				month = 0;
				year++;
			}
			totalMonths--;
		}
		return allTestData.toArray(new Object[0][]);
	}*/

	@Test(groups = { "minor" })
	public void testCalendarV1DataAPIResponse() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Setting all Calendar Months to Test for Calendar V1 Data API Response.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));

		try {

			int totalMonths = getNoOfMonthsAfter() + 1;
			logger.info("Total Calendar Months to be Tested for Calendar V1 Data API Response are: {}", totalMonths);
			while (totalMonths > 0) {

				logger.info("Verifying Calendar V1 Data API Response for Month {}, Year {} and UserId {}.", month, year, userId);
				logger.info("Hitting Calendar V1 Data API to verify Response for Month {}, Year {} and UserId {}", month, year, userId);
				CalendarV1Data calV1Obj = new CalendarV1Data();
				calV1Obj.hitCalendarV1Data(month, year, calendarA, "{\"userIds\":[" + userId + "]}");

				if (!ParseJsonResponse.validJsonResponse(calV1Obj.getCalendarV1DataJsonStr())) {
					logger.error("Calendar V1 Data API Response for Month {}, Year {} and UserId {} is Invalid JSON Response.", month, year, userId);
					csAssert.assertTrue(false, "Calendar V1 Data API Response for Month " + month + ", Year " + year + " and UserId " + userId +
							" is invalid JSON Response.");
				}

				month++;
				if (month > 11) {
					month = 0;
					year++;
				}
				totalMonths--;
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar V1 Data API Response for Month {} and Year {}. {}", month, year, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Calendar V1 Data API Response for Month " + month + " and Year " + year + ". " +
					e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarV1DataAPIResponse"), csAssert);
		csAssert.assertAll();
	}

	/*@DataProvider
	public Object[][] dataProviderForCalendarV1Data() throws ConfigurationException {
		logger.info("Setting all Calendar Months to Test.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		int totalMonths = getNoOfMonthsAfter() + 1;
		logger.info("Total Calendar Months to be Tested are: {}", totalMonths);

		List<Object[]> allTestData = new ArrayList<>();
		while (totalMonths > 0) {
			List<CalendarV1Data> recordsToTest = getRecordsToTest(month, year);
			for (CalendarV1Data record : recordsToTest) {
				allTestData.add(new Object[]{month, year, record});
			}
			month++;
			if (month > 11) {
				month = 0;
				year++;
			}
			totalMonths--;
		}
		return allTestData.toArray(new Object[0][]);
	}*/

	private List<CalendarV1Data> getRecordsToTest(int month, int year) {
		List<CalendarV1Data> recordsToTest = new ArrayList<>();
		try {
			logger.info("Hitting Calendar V1 Data Api for Month {}, Year {} and UserId {}", month, year, userId);
			CalendarV1Data calV1Obj = new CalendarV1Data();
			calV1Obj.hitCalendarV1Data(month, year, calendarA, "{\"userIds\":[" + userId + "]}");

			if (ParseJsonResponse.validJsonResponse(calV1Obj.getCalendarV1DataJsonStr())) {
				calV1Obj.setRecords(calV1Obj.getCalendarV1DataJsonStr());
				List<CalendarV1Data> records = calV1Obj.getRecords();
				logger.info("Total Records found: {}", records.size());

				if (records.size() > 0) {
					if (applyRandomization) {
						logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
						int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, records.size() - 1, maxRecordsToValidate);
						for (int randomNumber : randomNumbersForRecords) {
							recordsToTest.add(records.get(randomNumber));
						}
					} else {
						recordsToTest.addAll(records);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Records to Test for Month {} and Year {}. {}", month, year, e.getStackTrace());
		}
		return recordsToTest;
	}

	@Test(groups = { "minor" }, dependsOnMethods = "testCalendarV1DataAPIResponse")
	public void testCalendarV1Data() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Setting all Calendar Months to Test.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));

		try {

			int totalMonths = getNoOfMonthsAfter() + 1;
			logger.info("Total Calendar Months to be Tested are: {}", totalMonths);

			while (totalMonths > 0) {
				List<CalendarV1Data> recordsToTest = getRecordsToTest(month, year);

				for (CalendarV1Data record : recordsToTest) {
					int entityTypeId = record.getEntityTypeId();
					int entityId = record.getId();

					logger.info("Verifying Calendar V1 Record having EntityTypeId {} and Id {}.", entityTypeId, entityId);
					logger.info("Hitting Show Api for Record having EntityTypeId {}, Title {} and Id {}", entityTypeId, record.getTitle(), entityId);
					Show show = new Show();
					show.hitShow(record.getEntityTypeId(), entityId);
					String showJsonStr = show.getShowJsonStr();

					if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
						logger.error("Invalid Show Json Response for Record having EntityTypeId {}, Title {} and Id {}. Hence Skipping data validation", entityTypeId,
								record.getTitle(), entityId);
						csAssert.assertTrue(false, "Invalid Show Json Response for Record having EntityTypeId " + entityTypeId + ", Title " +
								record.getTitle() + " and Id " + entityId);
					} else if (ParseJsonResponse.hasPermissionError(showJsonStr)) {
						logger.info("Doesn't have Permission to access Show Page for Record having EntityTypeId {}, Title {} and Id {}. Hence Skipping data validation",
								entityTypeId, record.getTitle(), entityId);
					} else {
						String failureMsg;
						boolean resultPass;

						//Verify Title
						logger.info("Verifying Title");
						String titleOnShowPage = ShowHelper.getValueOfField(entityTypeId, entityId, "title", false, showJsonStr);
						if (titleOnShowPage == null || !titleOnShowPage.trim().equalsIgnoreCase(record.getTitle())) {
							String nameOnShowPage = ShowHelper.getValueOfField(entityTypeId, entityId, "name", false, showJsonStr);
							if (nameOnShowPage == null || !nameOnShowPage.trim().equalsIgnoreCase(record.getTitle())) {
								logger.error("Record having EntityTypeId {}, Title {} and Id {} failed on Show Page for Field Title.", entityTypeId, record.getTitle(),
										entityId);
								csAssert.assertTrue(false, "Record having EntityTypeId " + entityTypeId + ", Title " + record.getTitle() + " and Id " + entityId +
										" failed on Show Page for Field Title.");
							}
						}

						//Verify Supplier
						if (record.getSupplier() != null && !record.getSupplier().trim().equalsIgnoreCase("")) {
							logger.info("Verifying Supplier");
							resultPass = show.verifyShowField(showJsonStr, "supplier", record.getSupplier(), entityTypeId, "text");
							if (!resultPass) {
								failureMsg = "Record having EntityTypeId " + entityTypeId + " ,Supplier " + record.getSupplier() + " and Id " + entityId +
										" failed on Show Page for Field Supplier";
								csAssert.assertTrue(false, failureMsg);
							}
						} else {
							logger.info("Couldn't get Supplier for Record having Id {}. Hence not verifying Supplier.", record.getId());
						}

						//Verify Status
						logger.info("Verifying Status");
						resultPass = show.verifyShowField(showJsonStr, "calendar status", record.getEntityStatus(), entityTypeId, "text");
						if (!resultPass) {
							failureMsg = "Record having EntityTypeId " + entityTypeId + " , Status " + record.getEntityStatus() + " and Id " + entityId +
									" failed on Show Page for Field Status";
							csAssert.assertTrue(false, failureMsg);
						}

				/*//Verify Expiration Date
				logger.info("Verifying Expiration Date");
				String actualDate = DateUtils.getDateFromEpoch(Long.parseLong(Long.toString(record.getStart()) + "000"), dateFormat);
				String beforeDate = DateUtils.getDateOfXDaysFromYDate(actualDate, (-bufferBeforeDate), dateFormat);
				String afterDate = DateUtils.getDateOfXDaysFromYDate(actualDate, bufferAfterDate, dateFormat);
				String expectedDateRange = beforeDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + afterDate;

				resultPass = show.verifyShowField(showJsonStr, "expirationdate", expectedDateRange, entityTypeId, "date", dateFormat);
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + " , Expiration Date " + actualDate + " and Id " + entityId +
							" failed on Show Page for Field Expiration Date";
					csAssert.assertTrue(false, failureMsg);
				}*/

						//Verify Stakeholder
						logger.info("Verifying Stakeholder");
						String stakeHolderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "stakeHolderName");

						JSONObject jsonObj = new JSONObject(showJsonStr);
						String stakeHolderJsonStr = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").toString();

						if (!stakeHolderJsonStr.trim().equalsIgnoreCase("{}")) {
							resultPass = show.verifyShowField(showJsonStr, "stakeholders", stakeHolderName, entityTypeId, "stakeholder");
							if (!resultPass) {
								failureMsg = "Record having EntityTypeId " + entityTypeId + " , Stakeholder " + queryUserName + " and Id " + entityId +
										" failed on Show Page for Field Stakeholders";
								csAssert.assertTrue(false, failureMsg);
							}
						} else {
							logger.error("Stakeholder not defined for Record having EntityTypeId {} and Id {}.", entityTypeId, entityId);
						}
					}
				}

				month++;
				if (month > 11) {
					month = 0;
					year++;
				}
				totalMonths--;
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying CalendarV1 Data for Month {} and Year {}. {}", month, year, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying CalendarV1 Data for Month " + month + " and Year " + year + ". " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarV1Data"), csAssert);
		csAssert.assertAll();
	}

	private int getNoOfMonthsAfter() throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "NoOfMonthsAfter");
		return (value != null && !value.equalsIgnoreCase("")) ? Integer.parseInt(value)
				: Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DefaultMonthsAfter"));
	}

	private void setUserId() {
		try {
			logger.info("Setting Id for User: {}", queryUserName);
			Map<String, String> optionsParameters = new HashMap<>();
			String pageType = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pageType", "calendar");
			optionsParameters.put("pageType", pageType);
			optionsParameters.put("query", queryUserName);
			int optionsDropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropDownType", "stakeholders"));
			logger.info("Hitting Options Api.");
			Options opObj = new Options();
			opObj.hitOptions(optionsDropDownType, optionsParameters);
			if (opObj.getIds().contains(",")) {
				String[] ids = opObj.getIds().split(Pattern.quote(","));
				userId = ids[0].trim();
			} else {
				userId = opObj.getIds();
			}
		} catch (Exception e) {
			logger.error("Exception while Setting Id for User {}. {}", queryUserName, e.getStackTrace());
		}
	}
}