package com.sirionlabs.test.calendar;

import com.sirionlabs.api.calendar.CalendarData;
import com.sirionlabs.api.calendar.CalendarFilterData;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestCalendarFilterData extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestCalendarFilterData.class);
	private static String statusColorsMappingFilePath = null;
	private static String statusColorsMappingFileName = null;
	private String configFilePath;
	private String configFileName;
	private int month;
	private int year;
	private boolean applyRandomization = false;
	private int maxRecordsToValidate = 3;
	private int maxFiltersToValidate = 3;
	private int maxFilterOptionsToValidate = 3;
	private static List<String> filtersToIgnore;

	@BeforeClass(groups = { "minor" })
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("CalendarConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("CalendarFilterDataConfigFileName");
		statusColorsMappingFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "statuscolorsmappingfilepath");
		statusColorsMappingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "statuscolorsmappingfilename");
		month = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "month"));
		year = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "year"));
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyrandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		if (applyRandomization) {
			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxrecordstovalidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxRecordsToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxnooffilterstovalidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFiltersToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxnooffilteroptionstovalidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFilterOptionsToValidate = Integer.parseInt(temp);
		}

		filtersToIgnore = new ArrayList<>();
		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore") != null &&
				!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore").trim().equalsIgnoreCase("")) {
			String ignoreFiltersStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
					"filtersToIgnore").trim().split(Pattern.quote(","));

			for (String filter : ignoreFiltersStr) {
				filtersToIgnore.add(filter.trim());
			}
		}

		testCasesMap = getTestCasesMapping();
	}

	@Test(groups = { "minor" })
	public void testCalendarFilterDataAPIResponse() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Hitting Calendar Filter Data Api to verify API Response.");
			CalendarFilterData filterDataObj = new CalendarFilterData();
			filterDataObj.hitCalendarFilterData();

			if (!ParseJsonResponse.validJsonResponse(filterDataObj.getCalendarFilterDataJsonStr())) {
				logger.error("Calendar Filter Data API Response is an invalid JSON.");
				csAssert.assertTrue(false, "Calendar Filter Data API Response is an invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Filter Data API Response. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying Calendar Filter Data API Response. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarFilterDataAPIResponse"), csAssert);
		csAssert.assertAll();
	}

	/*@DataProvider
	public Object[][] dataProviderForCalendarFilterData() throws Exception {
		List<Object[]> filtersToTest = new ArrayList<>();
		logger.info("Setting All Filters to Test");
		logger.info("Hitting Calendar Filter Data Api.");
		CalendarFilterData filterDataObj = new CalendarFilterData();
		filterDataObj.hitCalendarFilterData();
		String filterDataJsonStr = filterDataObj.getCalendarFilterDataJsonStr();

		if (ParseJsonResponse.validJsonResponse(filterDataJsonStr)) {
			List<Map<String, String>> allFilterMetadata = CalendarFilterData.getAllFilterMetadata(filterDataJsonStr);
			logger.info("Total Filters found: {}", allFilterMetadata.size());
			logger.info("Removing Filters (if any) that are to be Ignored.");

			List<Map<String, String>> eligibleFilterMetadata = new ArrayList<>();
			eligibleFilterMetadata.addAll(allFilterMetadata);

			for (Map<String, String> filterMetadata : allFilterMetadata) {
				String queryName = filterMetadata.get("queryName").trim().toLowerCase();

				if (filtersToIgnore.contains(queryName))
					eligibleFilterMetadata.remove(filterMetadata);
			}

			List<Map<String, String>> allFiltersToValidate = new ArrayList<>();
			if (applyRandomization) {
				logger.info("Maximum No of Filters to Validate: {}", maxFiltersToValidate);
				int[] randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, eligibleFilterMetadata.size() - 1, maxFiltersToValidate);
				for (int randomNumber : randomNumbersForFilters) {
					allFiltersToValidate.add(eligibleFilterMetadata.get(randomNumber));
				}
			} else {
				allFiltersToValidate.addAll(eligibleFilterMetadata);
			}

			for (Map<String, String> filter : allFiltersToValidate) {
				filtersToTest.add(new Object[]{filter.get("queryName"), filter, filterDataJsonStr});
			}
		}
		return filtersToTest.toArray(new Object[0][]);
	}*/

	@Test(groups = { "minor" }, dependsOnMethods = "testCalendarFilterDataAPIResponse")
	public void testCalendarFilterData() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Setting All Filters to Test");
			logger.info("Hitting Calendar Filter Data Api.");
			CalendarFilterData filterDataObj = new CalendarFilterData();
			filterDataObj.hitCalendarFilterData();
			String filterDataJsonStr = filterDataObj.getCalendarFilterDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(filterDataJsonStr)) {
				List<Map<String, String>> allFilterMetadata = CalendarFilterData.getAllFilterMetadata(filterDataJsonStr);
				logger.info("Total Filters found: {}", allFilterMetadata.size());
				logger.info("Removing Filters (if any) that are to be Ignored.");

				List<Map<String, String>> eligibleFilterMetadata = new ArrayList<>();
				eligibleFilterMetadata.addAll(allFilterMetadata);

				for (Map<String, String> filterMetadata : allFilterMetadata) {
					String queryName = filterMetadata.get("queryName").trim().toLowerCase();

					if (filtersToIgnore.contains(queryName))
						eligibleFilterMetadata.remove(filterMetadata);
				}

				List<Map<String, String>> allFiltersToValidate = new ArrayList<>();
				if (applyRandomization) {
					logger.info("Maximum No of Filters to Validate: {}", maxFiltersToValidate);
					int[] randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, eligibleFilterMetadata.size() - 1,
							maxFiltersToValidate);
					for (int randomNumber : randomNumbersForFilters) {
						allFiltersToValidate.add(eligibleFilterMetadata.get(randomNumber));
					}
				} else {
					allFiltersToValidate.addAll(eligibleFilterMetadata);
				}
				ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
				List<FutureTask<Boolean>> taskList = new ArrayList<>();

				for (Map<String, String> filter : allFiltersToValidate) {
					logger.info("************************************************");

					FutureTask<Boolean> result = new FutureTask<>(() -> {

						String filterName = filter.get("queryName");

					logger.info("Verifying Filter [{}]", filterName);
					String filterType = "select";
					if (CalendarFilterData.isFilterAutoComplete(filterDataJsonStr, filterName, filter.get("queryName"))) {
						filterType = "autoComplete";
					}
					String payloadKeyName = filter.get("queryName");
					if (payloadKeyName.endsWith("s")) {
						payloadKeyName = payloadKeyName.substring(0, payloadKeyName.length() - 1);
					}
					String showPageObjectName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filternameshowpageobjectmapping", filterName);

					if (showPageObjectName == null || showPageObjectName.trim().equalsIgnoreCase("")) {
						csAssert.assertTrue(false, "Show Page Object Name Mapping not found for Filter " + filterName +
								". Hence Can't validate records on Show Page.");
						csAssert.assertAll();
						return true;
					}

					if (filterType.trim().equalsIgnoreCase("select") || filterType.trim().equalsIgnoreCase("multiselect")) {
						List<Map<String, String>> allOptions = CalendarFilterData.getAllFilterOptionsFromQueryName(filterDataJsonStr, filter.get("queryName"));
						logger.info("Total Options found for Filter [{}]: {}", filterName, allOptions.size());

						List<Map<String, String>> allOptionsToValidate = new ArrayList<>();
						if (applyRandomization) {
							logger.info("Maximum No of Options to Validate for Filter [{}]: {}", filterName, maxFilterOptionsToValidate);
							int[] randomNumbersForOptions = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allOptions.size() - 1, maxFilterOptionsToValidate);
							for (int randomNumber : randomNumbersForOptions) {
								allOptionsToValidate.add(allOptions.get(randomNumber));
							}
						} else {
							allOptionsToValidate.addAll(allOptions);
						}

						for (Map<String, String> optionsMap : allOptionsToValidate) {
							logger.info("------------------------------------------");
							String optionId = optionsMap.get("id");
							CalendarData calDataObj = new CalendarData();
							String payload = "{\"" + payloadKeyName + "Ids\":[\"" + optionId + "\"]}";
							logger.info("Hitting Calendar Data Api for Filter [{}], Option [{}]", filterName, optionsMap.get("name"));
							calDataObj.hitCalendarData(month, year, "false", payload);

							if (ParseJsonResponse.validJsonResponse(calDataObj.getCalendarDataJsonStr())) {
								calDataObj.setRecords(calDataObj.getCalendarDataJsonStr());
								List<CalendarData> records = calDataObj.getRecords();
								String expectedValue = optionsMap.get("name");
								String colorName = null;
								if (filter.get("queryName").trim().equalsIgnoreCase("StatusColors")) {
									colorName = expectedValue;
									expectedValue = "statusColor";
								} else if (filter.get("queryName").trim().equalsIgnoreCase("entityTypes")) {
									if (!ParseConfigFile.hasProperty(configFilePath, configFileName, "entityTypeNameIdMapping", expectedValue)) {
										csAssert.assertTrue(false, "Entity Type Id not found for Entity " + expectedValue +
												". Hence Can't validate records on Show Page.");
										continue;
									} else
										expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityTypeNameIdMapping", expectedValue);
								}
								verifyFilterDataRecords(records, filterName, showPageObjectName, expectedValue, colorName, csAssert);
							} else {
								logger.error("Calendar Data API Response for Filter [{}], Option [{}] is an Invalid JSON.", filterName, optionsMap.get("name"));
								csAssert.assertTrue(false, "Calendar Data API Response for Filter [" + filterName + "], Option [" + optionsMap.get("name") +
										"] is an Invalid JSON.");
							}
						}
					} else if (filterType.trim().equalsIgnoreCase("autoComplete")) {
						String optionsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFilePath");
						String optionsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName");
						int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "dropdowntype",
								filterName));
						Map<String, String> optionsParams = new HashMap<>();
						String pageType = ParseConfigFile.getValueFromConfigFile(optionsConfigFilePath, optionsConfigFileName, "pagetype", "filterdata");
						optionsParams.put("pageType", pageType);
						String entityTypeId = null;
						if (filterName.trim().equalsIgnoreCase("relations") || filterName.trim().equalsIgnoreCase("suppliers"))
							entityTypeId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
									ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), "suppliers");
						else if (filterName.trim().equalsIgnoreCase("contract") || filterName.trim().equalsIgnoreCase("contracts"))
							entityTypeId = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
									ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), "contracts");
						optionsParams.put("entityTpeId", entityTypeId);
						String query = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultfiltertext");
						optionsParams.put("query", query);
						Options optionObj = new Options();
						logger.info("Hitting Options Api for Query Text: {}", query);
						optionObj.hitOptions(dropDownType, optionsParams);
						String id = optionObj.getIds();
						if (id.contains(",")) {
							String[] allIds = id.split(Pattern.quote(","));
							logger.info("Multiple data available for Query Text: {}. Considering first record i.e. {}", query, allIds[0].trim());
							id = allIds[0].trim();
						}
						Map<String, String> optionsMap = new HashMap<>();
						String expectedValue = Options.getNameFromId(optionObj.getOptionsJsonStr(), Integer.parseInt(id));
						optionsMap.put("name", expectedValue);
						optionsMap.put("id", id);

						CalendarData calDataObj = new CalendarData();
						String payload = "{\"" + payloadKeyName + "Ids\":[\"" + id + "\"]}";
						logger.info("Hitting Calendar Data Api for Filter [{}], Option {}", filterName, optionsMap.get("name"));
						calDataObj.hitCalendarData(month, year, "false", payload);
						calDataObj.setRecords(calDataObj.getCalendarDataJsonStr());
						List<CalendarData> records = calDataObj.getRecords();
						String colorName = null;
						if (filter.get("queryName").trim().equalsIgnoreCase("StatusColors")) {
							expectedValue = "statusColor";
							colorName = optionsMap.get("name");
						} else if (filter.get("queryName").trim().equalsIgnoreCase("entityTypes")) {
							if (!ParseConfigFile.hasProperty(configFilePath, configFileName, "entityTypeNameIdMapping", expectedValue)) {
								csAssert.assertTrue(false, "Entity Type Id not found for Entity " + expectedValue +
										". Hence Can't validate records on Show Page.");
								return true;
							} else
								expectedValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entityTypeNameIdMapping", expectedValue);
						}
						verifyFilterDataRecords(records, filterName, showPageObjectName, expectedValue, colorName, csAssert);
					} else {
						logger.info("Currently Filters of Type other than Select/Multiselect and AutoComplete are not supported.");
					}

						return true;
					});

					taskList.add(result);
					executor.execute(result);
				}

				for (FutureTask<Boolean> task : taskList)
					task.get();
				}
		} catch (Exception e) {
			logger.error("Exception while Verifying Calendar Filter Data. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Verifying Calendar Filter Data. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testCalendarFilterData"), csAssert);
		csAssert.assertAll();
	}

	private void verifyFilterDataRecords(List<CalendarData> records, String filterName, String fieldName, String expectedValue, String colorName,
	                                     CustomAssert csAssert) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		logger.info("Verifying Records for Filter [{}] and Option [{}]", filterName, expectedValue);
		logger.info("Total Records for Filter [{}] and Option [{}]: {}", filterName, expectedValue, records.size());
		if (records.size() == 0) {
			return;
		}
		List<CalendarData> recordsToValidate = new ArrayList<>();
		if (applyRandomization) {
			logger.info("Maximum Records to Validate for Filter [{}] and Option [{}]: {}", filterName, expectedValue, maxRecordsToValidate);
			int[] randomNumbersForRecords = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, records.size() - 1, maxRecordsToValidate);
			for (int randomNumber : randomNumbersForRecords) {
				recordsToValidate.add(records.get(randomNumber));
			}
		} else {
			recordsToValidate.addAll(records);
		}

		for (int j = 0; j < recordsToValidate.size(); j++) {
			final int index = j;
			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					try {
						String entityName;
						String finalExpectedValue;
						entityName = ConfigureConstantFields.getEntityNameById(recordsToValidate.get(index).getEntityTypeId());

						if (expectedValue != null && expectedValue.trim().equalsIgnoreCase("statusColor"))
							finalExpectedValue = getStatusColorMapping(entityName, colorName);
						else
							finalExpectedValue = expectedValue;

						Show show = new Show();
						logger.info("Hitting Show Api for Record #{} having EntityTypeId {}, Title {} and Id {}",
								(index + 1), recordsToValidate.get(index).getEntityTypeId(), recordsToValidate.get(index).getTitle(), recordsToValidate.get(index).getId());
						show.hitShow(recordsToValidate.get(index).getEntityTypeId(), recordsToValidate.get(index).getId());
						String showJsonStr = show.getShowJsonStr();

						if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
							logger.error("Invalid Show Json Response while Verifying Field {} for Record #{} having EntityTypeId {}, Title {} and Id {}", fieldName,
									(index + 1), recordsToValidate.get(index).getEntityTypeId(), recordsToValidate.get(index).getTitle(), recordsToValidate.get(index).getId());
							csAssert.assertTrue(false, "Invalid Show Json Response while Verifying Field " + fieldName + " for Record #" + (index + 1) +
									" having EntityTypeId " + recordsToValidate.get(index).getEntityTypeId() + ", Title " + recordsToValidate.get(index).getTitle() +
									" and Id " + recordsToValidate.get(index).getId());
						} else {
							logger.info("Verifying Record #{} of Field {} having Expected Value {}", (index + 1), fieldName, finalExpectedValue);
							boolean resultPass = show.verifyShowField(showJsonStr, fieldName, finalExpectedValue, recordsToValidate.get(index).getEntityTypeId());
							if (!resultPass) {
								csAssert.assertTrue(false, "Record #" + (index + 1) + " having EntityTypeId " +
										recordsToValidate.get(index).getEntityTypeId() + ", Title " + recordsToValidate.get(index).getTitle() + " and Id " +
										recordsToValidate.get(index).getId() + " failed on Show Page for Field " + fieldName + " and Expected Value " + finalExpectedValue);
							}

						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Records for Filter [{}]. {}", filterName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying Records for Filter [" + filterName + "]. " + e.getMessage());
					}
					return true;
				}
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();
	}

	private String getStatusColorMapping(String entityName, String statusColor) {
		String colorMapping = null;
		try {
			logger.info("Checking if Entity {} has Mapping for Status Color {}.", entityName, statusColor);
			if (ParseConfigFile.hasProperty(statusColorsMappingFilePath, statusColorsMappingFileName, entityName, statusColor)) {
				colorMapping = ParseConfigFile.getValueFromConfigFile(statusColorsMappingFilePath, statusColorsMappingFileName, entityName, statusColor).trim();
			} else {
				logger.info("Couldn't find mapping for Status Color {} and Entity {}.", statusColor, entityName);
				logger.info("Checking if Default Mapping for Status Color {} is available.", statusColor);
				if (ParseConfigFile.hasProperty(statusColorsMappingFilePath, statusColorsMappingFileName, "default", statusColor)) {
					logger.info("Found default Mapping for Status Color {}.", statusColor);
					colorMapping = ParseConfigFile.getValueFromConfigFile(statusColorsMappingFilePath, statusColorsMappingFileName, "default", statusColor).trim();
				} else {
					logger.info("No Mapping found for Status Color {}.", statusColor);
					return null;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Status Color Mapping for Entity {} and Status Color {}. {}", entityName, statusColor, e.getStackTrace());
		}
		return colorMapping;
	}
}