package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;


/**
 * Created by akshay.rohilla on 6/30/2017.
 */
public class TestUserAccess {

	private final static Logger logger = LoggerFactory.getLogger(TestUserAccess.class);
	private static String configFileName = null;
	private static String configFilePath = null;
	private static String fieldsToTestFilePath = null;
	private static String fieldsToTestFileName = null;
	private static String entityIdMappingConfigFilePath = null;
	private static String entityIdMappingConfigFileName = null;
	private static String csvFilePath = null;
	private static String csvFileName = null;
	private String delimiterForListDataValue = ":;";
	private DumpResultsIntoCSV dumpResultsObj;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("UserAccessConfigFileName");
		fieldsToTestFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "FieldsToTestConfigFilePath");
		fieldsToTestFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "FieldsToTestConfigFileName");
		entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
		entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		csvFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "csvfilepath");
		csvFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "csvfilename") + ".csv";
		dumpResultsObj = new DumpResultsIntoCSV(csvFilePath, csvFileName, setHeadersInCSVFile());
	}

	@AfterClass
	public void afterClass() throws IOException, ConfigurationException {
		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "convertcsvtoxlsx").trim().equalsIgnoreCase("yes")) {
			String csvDelimiter = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVDelimiter");
			XLSUtils.convertCSVToXLSX(csvFilePath, csvFileName, csvFilePath, csvFileName, csvDelimiter);
			File csvFile = new File(csvFilePath + "/" + csvFileName);
			csvFile.delete();
		}
	}

	@DataProvider(parallel = false)
	public Object[][] dataProviderForUserAccess() throws ConfigurationException {
		logger.info("Setting all Entities to Test.");
		List<Object[]> allTestData = new ArrayList<>();
		String[] entities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestotest").split(Pattern.quote(","));
		for (String entity : entities) {
			allTestData.add(new Object[]{entity.trim()});
		}
		logger.info("Total Entities to Test: {}", allTestData.size());
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForUserAccess", enabled = true)
	public void testUserDataAccess(String entityName) {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> resultsMap = new HashMap<>();
		try {
			resultsMap.put("entityName", entityName);
			String delimiterForFields = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delimiterforfields");
			String includedItem = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "IncludedItem").trim();
			String excludedItems[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "ExcludedItems").trim().split(delimiterForFields);

			logger.info("Setting Fields to be Tested for Entity {}", entityName);
			List<String> fieldsToTest = new ArrayList<>();
			List<String> fields = ParseConfigFile.getAllPropertiesOfSection(fieldsToTestFilePath, fieldsToTestFileName, entityName);
			for (String fieldName : fields)
				fieldsToTest.add(fieldName.trim());

			logger.info("Total Fields to be Tested for Entity {} are {}", entityName, fieldsToTest.size());
			int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), null, entityName));
			if (entityTypeId == -1) {
				logger.info("Couldn't find Entity Type Id for {}. Hence skipping Validation", entityName);
				csAssert.assertTrue(false, "Couldn't find Entity Type Id for " + entityName + ". Hence skipping Validation");
				resultsMap.put("result", "fail");
				resultsMap.put("comments", "Couldn't find Entity Type Id for " + entityName + ". Hence skipping Validation");
				dumpResultsIntoCSV(resultsMap);
				return;
			}
			resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
			logger.info("Hitting ListRendererListData for Entity {}", entityName);
			ListRendererListData listDataObj = new ListRendererListData();
			String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
					ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "ListDataOffset") + ",\"size\":" +
					ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "MaxRecordsForListData") + ",\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			resultsMap.put("listDataPayload", listDataPayload);
			int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName,
					"entity_url_id"));
			listDataObj.hitListRendererListData(listId, false, listDataPayload, null);

			String listDataJsonStr = listDataObj.getListDataJsonStr();
			logger.info("Setting List Data for Entity {}", entityName);
			listDataObj.setListData(listDataJsonStr);
			List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();

			if (listData.size() > 0) {
				this.validateListData(listDataObj, listData, entityName, fieldsToTest, includedItem, excludedItems, csAssert);
				this.validateShowPage(listData, entityName, entityTypeId, fieldsToTest, includedItem, excludedItems, csAssert);
				this.validateFilterData(entityName, entityTypeId, fieldsToTest, excludedItems, csAssert);
			} else {
				logger.info("No Data Found for Entity {}", entityName);
				resultsMap.put("comments", "No Data Found for Entity " + entityName);
				dumpResultsIntoCSV(resultsMap);
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying User Access for Entity {}. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying User Access for Entity " + entityName + ". " + e.getMessage());
			resultsMap.put("result", "fail");
			resultsMap.put("exception", e.getMessage());
			dumpResultsIntoCSV(resultsMap);
		}
		csAssert.assertAll();
	}

	private void validateShowPage(List<Map<Integer, Map<String, String>>> listData, String entityName, int entityTypeId, List<String> fieldsToTest, String includedItem,
	                              String[] excludedItems, CustomAssert csAssert) throws Exception {
		logger.info("Verifying Show Page for Entity {}", entityName);
		logger.info("Hitting ListRendererDefaultUserListMetaData Api for Entity {}", entityName);
		ListRendererDefaultUserListMetaData listMetadatObj = new ListRendererDefaultUserListMetaData();
		int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName,
				"entity_url_id"));
		listMetadatObj.hitListRendererDefaultUserListMetadata(listId, null, "{}");
		listMetadatObj.setFilterMetadatas(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());
		listMetadatObj.setColumns(listMetadatObj.getListRendererDefaultUserListMetaDataJsonStr());

		int randomNumbersForShowPage[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, listData.size() - 1,
				Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "MaxNoOfRecordsToTestOnShowPage")));
		int columnId = listMetadatObj.getIdFromQueryName("id");
		logger.info("Total Records to be validated on Show Page for Entity {} are {}", entityName, randomNumbersForShowPage.length);
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		for (int i = 0; i < randomNumbersForShowPage.length; i++) {
			final int outerIndex = i;

			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					String entityId = null;
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
						entityId = listData.get(randomNumbersForShowPage[outerIndex]).get(columnId).get("valueId").trim();
						resultsMap.put("entityId", entityId);
						resultsMap.put("includedItem", includedItem);
						resultsMap.put("showPagePass", "true");
						resultsMap.put("excludedItem", Arrays.toString(excludedItems));
						logger.info("Verifying Show Page for Entity {} and Record #{}", entityName, (outerIndex + 1));
						logger.info("Hitting Show Api for Entity {} having Record Id {}", entityName, entityId);
						Show showObj = new Show();
						showObj.hitShow(entityTypeId, Integer.parseInt(entityId));
						String showJsonStr = showObj.getShowJsonStr();

						if (fieldsToTest.size() == 0) {
							logger.info("No Fields defined to Test for Entity {} and Id {}. Hence skipping Validation", entityName, entityId);
							dumpResultsIntoCSV(resultsMap);
						}

						for (int j = 0; j < fieldsToTest.size(); j++) {
							resultsMap.put("showPagePass", "true");
							resultsMap.put("fieldName", fieldsToTest.get(j));
							String queryName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
									ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFileName"), entityName, fieldsToTest.get(j));

							logger.info("Getting Field Type for Field #{} {}", (j + 1), fieldsToTest.get(j));
							String fieldType = listMetadatObj.getFieldTypeFromQueryName(queryName);
							if (fieldType == null) {
								logger.error("Couldn't find Field Type for Field #{} {}. Hence skipping Show Page validation for this Field", (j + 1), fieldsToTest.get(j));
								csAssert.assertTrue(false, "Couldn't find Field Type for Field " + fieldsToTest.get(j));
								resultsMap.put("result", "fail");
								resultsMap.put("comments", "Couldn't find Field Type for Field " + fieldsToTest.get(j));
								resultsMap.put("showPagePass", "false");
								dumpResultsIntoCSV(resultsMap);
								continue;
							}
							logger.info("Verifying that Field #{} {} of Entity {} has Included Item {}", (j + 1), fieldsToTest.get(j), entityName, includedItem);

							//Code to Get Field Name in Show Page from FieldsToTestInUserAccess Config File and then pass that fieldName in verifyShow Method
							String field = ParseConfigFile.getValueFromConfigFile(fieldsToTestFilePath, fieldsToTestFileName, entityName, fieldsToTest.get(j));
							boolean showFieldPass = showObj.verifyShowField(showJsonStr, field, includedItem, entityTypeId, fieldType, false, false);
							String failureMsg;
							String comments = "";
							if (!showFieldPass) {
								failureMsg = "Field #" + (j + 1) + " " + fieldsToTest.get(j) + " of Record #" + (outerIndex + 1) + " having Id " + entityId + " of Entity " +
										entityName + " failed on Show Page for Inclusion";
								resultsMap.put("result", "fail");
								resultsMap.put("showPagePass", "false");
								csAssert.assertTrue(false, failureMsg);
								comments += failureMsg + ", ";
							}

							logger.info("Verifying that Field #{} {} of Entity {} doesn't have Excluded Items", (j + 1), fieldsToTest.get(j), entityName);
							showFieldPass = showObj.verifyShowFieldForExclusion(showJsonStr, field, includedItem, entityTypeId, fieldType, false, false,
									excludedItems);
							if (!showFieldPass) {
								failureMsg = "Field #" + (j + 1) + " " + fieldsToTest.get(j) + " of Record #" + (outerIndex + 1) + "having Id " + entityId + " of Entity " +
										entityName + " failed on Show Page for Exclusion";
								resultsMap.put("result", "fail");
								resultsMap.put("showPagePass", "false");
								csAssert.assertTrue(false, failureMsg);
								comments += failureMsg;
							}
							resultsMap.put("comments", comments);
							dumpResultsIntoCSV(resultsMap);
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Show Page for Entity {} and Record Id {}. {}", entityName, entityId, e.getStackTrace());
						resultsMap.put("exception", e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("showPagePass", "false");
						dumpResultsIntoCSV(resultsMap);
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

	private void validateListData(ListRendererListData listDataObj, List<Map<Integer, Map<String, String>>> listData, String entityName, List<String> fieldsToTest,
	                              String includedItem, String[] excludedItems, CustomAssert csAssert) throws ExecutionException, InterruptedException, ConfigurationException {
		Map<String, Integer> fieldColumnIdMap = new HashMap<>();
		for (int i = 0; i < fieldsToTest.size(); i++) {
			logger.info("Getting Column Id for Field #{} {}", (i + 1), fieldsToTest.get(i));
			String columnName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFileName"), entityName, fieldsToTest.get(i));
			if (columnName == null) {
				logger.info("Couldn't find Entity/Field {}/{} in Config file. Hence not validating Field in List Data", entityName, fieldsToTest.get(i));
				continue;
			}
			fieldColumnIdMap.put(fieldsToTest.get(i), listDataObj.getColumnIdFromColumnName(columnName));
		}

		logger.info("Total List Data Records are {}", listData.size());
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		for (int i = 0; i < listData.size(); i++) {
			final int outerIndex = i;
			logger.info("Verifying List Data Record #{} for Entity {}", (outerIndex + 1), entityName);

			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(ConfigureConstantFields.getEntityIdByName(entityName)));
						resultsMap.put("listDataPass", "true");
						resultsMap.put("includedItem", includedItem);
						for (int j = 0; j < fieldsToTest.size(); j++) {
							logger.info("Verifying Field #{} {} of List Data Record #{}", (j + 1), fieldsToTest.get(j), (outerIndex + 1));
							resultsMap.put("fieldName", fieldsToTest.get(j));
							if (fieldColumnIdMap.get(fieldsToTest.get(j)) == -1) {
								logger.info("No Such Field {} for Entity {}", fieldsToTest.get(j), entityName);
								resultsMap.put("result", "fail");
								resultsMap.put("listDataPass", "false");
								resultsMap.put("comments", "No Such Field " + fieldsToTest.get(j) + " for Entity " + entityName);
								dumpResultsIntoCSV(resultsMap);
								continue;
							}

							String value[] = listData.get(outerIndex).get(fieldColumnIdMap.get(fieldsToTest.get(j))).get("value").split(delimiterForListDataValue);
							logger.info("Validating that Field #{} {} of List Data Record #{} has Included Item {}", (j + 1), fieldsToTest.get(j), (outerIndex + 1),
									includedItem);
							boolean casePass = value[0].trim().toLowerCase().contains(includedItem.toLowerCase());
							String failureMsg;
							String comments = "";
							if (!casePass) {
								failureMsg = "Field #" + (j + 1) + " " + fieldsToTest.get(j) + " of List Data Record #" + (outerIndex + 1) + " doesn't have Included Item "
										+ includedItem + " and has value " + value[0].trim();
								csAssert.assertTrue(false, failureMsg);
								comments += failureMsg + ", ";
								resultsMap.put("result", "fail");
								resultsMap.put("listDataPass", "false");
							}

							for (String excludedItem : excludedItems) {
								resultsMap.put("listDataPass", "true");
								resultsMap.put("excludedItem", excludedItem);
								logger.info("Validating that Field #{} {} of List Data Record #{} doesn't have Excluded Item {}", (j + 1), fieldsToTest.get(j),
										(outerIndex + 1), excludedItem);
								casePass = value[0].trim().toLowerCase().contains(excludedItem.toLowerCase());
								if (!casePass) {
									failureMsg = "Field #" + (j + 1) + " " + fieldsToTest.get(j) + " of List Data Record #" + (outerIndex + 1) + "has Excluded Item "
											+ excludedItem + " and has value " + value[0].trim();
									csAssert.assertFalse(false, failureMsg);
									comments += failureMsg;
									resultsMap.put("result", "fail");
									resultsMap.put("listDataPass", "false");
								}
							}
							resultsMap.put("comments", comments);
							dumpResultsIntoCSV(resultsMap);
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying List Data Record #{} for Entity {}. {}", (outerIndex + 1), entityName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying List Data Record #" + (outerIndex + 1) + " for Entity " + entityName +
								". " + e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("listDataPass", "false");
						resultsMap.put("exception", e.getMessage());
						dumpResultsIntoCSV(resultsMap);
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

	private void validateFilterData(String entityName, int entityTypeId, List<String> fieldsToTest, String[] excludedItems, CustomAssert csAssert) {
		Map<String, String> resultsMap = new HashMap<>();
		try {
			resultsMap.put("entityName", entityName);
			resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
			logger.info("Verifying Filter Data Part for Entity {}", entityName);
			logger.info("Hitting ListRendererFilterData API for Entity {}", entityName);
			ListRendererFilterData filterDataObj = new ListRendererFilterData();
			int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName,
					"entity_url_id"));
			filterDataObj.hitListRendererFilterData(listId, "{}", null);
			String filterDataJsonStr = filterDataObj.getListRendererFilterDataJsonStr();
			String temp = null;

			for (String testField : fieldsToTest) {
				resultsMap.put("filterDataPass", "true");
				resultsMap.put("fieldName", testField);
				String filterName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
						ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFileName"), entityName, testField);
				List<Map<String, String>> filterData;

				if (!filterDataObj.isFilterAutoComplete(filterDataJsonStr, filterName)) {
					//For Drop Down Type Filters
					filterData = filterDataObj.setFilter(filterDataJsonStr, filterName);

					if (filterData.size() > 0) {
						resultsMap.put("excludedItem", Arrays.toString(excludedItems));
						for (String excludedItem : excludedItems) {
							logger.info("Verifying that Field {} doesn't have excluded item {}", testField, excludedItem);
							for (Map<String, String> filterMap : filterData) {
								if (filterMap.get("name").toLowerCase().contains(excludedItem.toLowerCase())) {
									temp = excludedItem;
									break;
								}
							}
						}
						if (temp != null) {
							csAssert.assertTrue(false, "Field " + testField + " has Excluded Item " + temp);
							resultsMap.put("result", "fail");
							resultsMap.put("comments", "Field " + testField + " has Excluded Item " + temp);
							resultsMap.put("filterDataPass", "false");
						}
					} else {
						logger.info("No Data found for Filter {} and Entity {}", filterName, entityName);
						resultsMap.put("comments", "No Data Found for Filter " + filterName + " and Entity " + entityName);
					}
					dumpResultsIntoCSV(resultsMap);
				} else {
					//For AutoComplete Type Filters
					logger.info("Field {} is of Auto Complete Type", testField);

					for (String excludedItem : excludedItems) {
						resultsMap.put("excludedItem", excludedItem);
						Options optionObj = new Options();
						Map<String, String> parameters = new HashMap<>();
						parameters.put("pageType", ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
								ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata"));
						parameters.put("entityTpeId", Integer.toString(filterDataObj.getFilterId(filterDataJsonStr, filterName)));
						parameters.put("pageEntityTypeId", Integer.toString(entityTypeId));
						parameters.put("query", excludedItem);

						int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
								ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", testField));

						logger.info("Hitting Options Api for Field {} and Excluded Item {}", testField, excludedItem);
						optionObj.hitOptions(dropDownType, parameters);

						logger.info("Verifying that Options Api returns null for Field {} and Excluded Item {}", testField, excludedItem);
						if (optionObj.getIds() != null) {
							csAssert.assertTrue(false, "Options Api has returned IDs " + optionObj.getIds());
							resultsMap.put("result", "fail");
							resultsMap.put("filterDataPass", "false");
							resultsMap.put("comments", "Options Api has returned IDs " + optionObj.getIds());
						}
						dumpResultsIntoCSV(resultsMap);
					}
				}

			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Filter Data for Entity {}. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying Filter Data for Entity " + entityName + ". " + e.getMessage());
			resultsMap.put("result", "fail");
			resultsMap.put("exception", e.getMessage());
			resultsMap.put("filterDataPass", "false");
			dumpResultsIntoCSV(resultsMap);
		}
	}

	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<>();
		String allColumns[] = {"entityTypeId", "entityName", "entityId", "listDataPayload", "listDataPass", "fieldName", "showPagePass", "filterDataPass", "includedItem",
				"excludedItem", "result", "exception", "comments"};
		headers.addAll(Arrays.asList(allColumns));
		return headers;
	}

	private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
		if (!resultsMap.containsKey("result"))
			resultsMap.put("result", "pass");
		dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
	}
}