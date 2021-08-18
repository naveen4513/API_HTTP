package com.sirionlabs.test.calendar;

import com.sirionlabs.api.calendar.CalendarData;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.sirionlabs.config.ConfigureEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;


/**
 * Created by akshay.rohilla on 7/11/2017.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestCalendarData extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestCalendarData.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String dateFormat = null;
	private int bufferBeforeDate;
	private int bufferAfterDate;
	private String calendarA = "false";
	private boolean applyRandomization = false;
	private int maxRecordsToValidate = 3;

	@BeforeClass(groups = { "minor" })
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CalendarConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CalendarDataConfigFileName");
		dateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dateformat");
		bufferBeforeDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferbefore"));
		bufferAfterDate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datevalidationbufferafter"));
		calendarA = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "calendarA");
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
		if (temp != null && !temp.trim().equalsIgnoreCase("") && org.apache.commons.lang3.math.NumberUtils.isParsable(temp))
			maxRecordsToValidate = Integer.parseInt(temp);

		testCasesMap = getTestCasesMapping();
	}

	/*@DataProvider
	public Object[][] dataProviderForCalendarDataAPIResponse() throws ConfigurationException {
		List<Object[]> allTestData = new ArrayList<>();
		logger.info("Setting all Calendar Months to Test Calendar Data API.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		int totalMonths = getNoOfMonthsAfter() + 1;
		logger.info("Total Calendar Months to be Tested are: {}", totalMonths);
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
	public void testCalendarDataAPIResponse() {
		CustomAssert csAssert = new CustomAssert();

		logger.info("Setting all Calendar Months to Test Calendar Data API.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));

		try {
			int totalMonths = getNoOfMonthsAfter() + 1;
			logger.info("Total Calendar Months to be Tested are: {}", totalMonths);

			while (totalMonths > 0) {
				logger.info("Verifying Calendar Data API Response for Month {} and Year {}", month, year);
				logger.info("Hitting Calendar Data API for Month {} and Year {}", month, year);
				CalendarData calDataObj = new CalendarData();
				calDataObj.hitCalendarData(month, year, calendarA);

				if (!ParseJsonResponse.validJsonResponse(calDataObj.getCalendarDataJsonStr())) {
					logger.error("Calendar Data API Response for Month {} and Year {} is not a valid JSON.", month, year);
					csAssert.assertTrue(false, "Calendar Data API Response for Month " + month + " and Year " + year + " is not a valid JSON.");
				}

				month++;
				if (month > 11) {
					month = 0;
					year++;
				}
				totalMonths--;
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Data API for Month {} and Year {}. {}", month, year, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Calendar Data API for Month " + month + " and Year " + year + ". " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarDataAPIResponse"), csAssert);
		csAssert.assertAll();
	}

	/*@DataProvider
	public Object[][] dataProviderForCalendarData() throws ConfigurationException {
		logger.info("Setting all Calendar Months to Test.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		int totalMonths = getNoOfMonthsAfter() + 1;
		logger.info("Total Calendar Months to be Tested are: {}", totalMonths);
		List<Object[]> allTestData = new ArrayList<>();
		while (totalMonths > 0) {
			List<CalendarData> recordsToTest = getRecordsToTest(month, year);
			for (CalendarData record : recordsToTest) {
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

	private List<CalendarData> getRecordsToTest(int month, int year) {
		List<CalendarData> recordsToTest = new ArrayList<>();
		try {
			logger.info("Hitting Calendar Data for Month {} and Year {}", month, year);
			CalendarData calDataObj = new CalendarData();
			calDataObj.hitCalendarData(month, year, calendarA);
			logger.info("Setting Calendar Records for Month {} and Year {}", month, year);
			if (ParseJsonResponse.validJsonResponse(calDataObj.getCalendarDataJsonStr())) {
				calDataObj.setRecords(calDataObj.getCalendarDataJsonStr());
				List<CalendarData> calendarRecords = calDataObj.getRecords();
				logger.info("Total Records found: {}", calendarRecords.size());

				if (calendarRecords.size() > 0) {
					if (applyRandomization) {
						logger.info("Maximum No of Records to Validate: {}", maxRecordsToValidate);
						int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, calendarRecords.size() - 1, maxRecordsToValidate);
						for (int randomNumber : randomNumbersForRecords) {
							recordsToTest.add(calendarRecords.get(randomNumber));
						}
					} else {
						recordsToTest.addAll(calendarRecords);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Calendar Records for Month {} and Year {}. {}", month, year, e.getStackTrace());
		}
		return recordsToTest;
	}

	@Test(groups = { "minor" }, dependsOnMethods = "testCalendarDataAPIResponse")
	public void testCalendarData() throws Exception {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Setting all Calendar Months to Test.");
		int month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		int year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));

		try {
			int totalMonths = getNoOfMonthsAfter() + 1;
			logger.info("Total Calendar Months to be Tested are: {}", totalMonths);

			while (totalMonths > 0) {
				List<CalendarData> recordsToTest = getRecordsToTest(month, year);

				ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
				List<FutureTask<Boolean>> taskList = new ArrayList<>();

				for (CalendarData record : recordsToTest) {
					FutureTask<Boolean> result = new FutureTask<>(() -> {
					int entityTypeId = record.getEntityTypeId();
					int entityId = record.getId();
					logger.info("Verifying Calendar Data Record having EntityTypeId {}, Title {} and Id {}.", record.getEntityTypeId(), record.getTitle(), record.getId());

					Show show = new Show();
					logger.info("Hitting Show Api for Record having EntityTypeId {}, Title {} and Id {}", record.getEntityTypeId(), record.getTitle(), record.getId());
					show.hitShow(record.getEntityTypeId(), entityId);
					String showJsonStr = show.getShowJsonStr();

					if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
						logger.error("Invalid Show Json Response for Record having EntityTypeId {}, Title {} and Id {}", record.getEntityTypeId(), record.getTitle(),
								record.getId());
						csAssert.assertTrue(false, "Invalid Show Json Response for Record having EntityTypeId " + entityTypeId + ", Title " +
								record.getTitle() + " and Id " + entityId);
					} else if (ParseJsonResponse.hasPermissionError(showJsonStr)) {
						logger.info("Doesn't have Permission to access Show Page for Record having EntityTypeId {}, Title {} and Id {}", record.getEntityTypeId(),
								record.getTitle(), record.getId());
					} else {
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

						boolean resultPass;
						String failureMsg;

						//Verify Supplier
						if (record.getSupplier() != null && !record.getSupplier().trim().equalsIgnoreCase("")) {
							logger.info("Verifying Supplier");
							resultPass = show.verifyShowField(showJsonStr, "supplier", record.getSupplier(), entityTypeId, "text");

							if (!resultPass) {
								failureMsg = "Record having EntityTypeId " + entityTypeId + ", Supplier " + record.getSupplier() + " and Id " + entityId +
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
							failureMsg = "Record having EntityTypeId " + entityTypeId + ", Status " + record.getEntityStatus() + " and Id " + entityId +
									" failed on Show Page for Field Status";
							csAssert.assertTrue(false, failureMsg);
						}

						//Verify Expiration Date
				/*logger.info("Verifying Expiration Date");
				String actualDate = DateUtils.getDateFromEpoch(Long.parseLong(Long.toString(record.getStart()) + "000"), dateFormat);
				String beforeDate = DateUtils.getDateOfXDaysFromYDate(actualDate, (-bufferBeforeDate), dateFormat);
				String afterDate = DateUtils.getDateOfXDaysFromYDate(actualDate, bufferAfterDate, dateFormat);
				String expectedDateRange = beforeDate + ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter") + afterDate;

				resultPass = show.verifyShowField(showJsonStr, "expirationdate", expectedDateRange, entityTypeId, "date",
						dateFormat);
				if (!resultPass) {
					failureMsg = "Record having EntityTypeId " + entityTypeId + ", Expiration Date " + actualDate + " and Id " + entityId +
							" failed on Show Page for Field Expiration Date";
					csAssert.assertTrue(false, failureMsg);
				}*/
					}
					return true;
				});
				taskList.add(result);
				executor.execute(result);


			}

			for (FutureTask<Boolean> task : taskList)
				task.get();

				month++;
				if (month > 11) {
					month = 0;
					year++;
				}
				totalMonths--;
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Data for Month {}, Year {}. {}", month, year, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Calendar Data for Month " + month + ", Year " + year + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarData"), csAssert);
		csAssert.assertAll();
	}

	private int getNoOfMonthsAfter() throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "NoOfMonthsAfter");
		return (value != null && !value.equalsIgnoreCase("")) ? Integer.parseInt(value)
				: Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DefaultMonthsAfter"));
	}
}