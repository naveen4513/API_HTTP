package com.sirionlabs.test;

import com.sirionlabs.api.listRenderer.TabDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.SortingHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class TestShowPageTabColumnSorting {

	private final static Logger logger = LoggerFactory.getLogger(TestShowPageTabColumnSorting.class);
	static String orderDirectionDesc;
	static String orderDirectionAsc;
	static int pageSize;
	List<String> allEntitySection;
	String entitySectionSplitter = ";";
	boolean failedInConfigReading = false;
	int randomShowPageIdsToBeValidated;
	HashMap<String, String> hashMapforPayloadProperties;
	HashMap<String, Integer> hashMapforTabsUrlId;
	List<Map<String, String>> allColumnQueryName;
	DumpResultsIntoCSV dumpResultsObj;
	int globalIndex = 0;
	private String baseFilePath;
	private String entityIdMappingFileName;
	private String showPageTabColumnsSortingCfgFilePath;
	private String showPageTabColumnsSortingCfgFileName;
	private String sortingColumnResultCSVFilePath;
	List<String> columnsToAvoid;

	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "EntityName", "TabName", "DbId", "ColumnName", "ColumnType", "OffSet", "PageSize", "SortingOrder", "StatusCode",
				"IsResponseJson", "IsResultSorted", "Comment", "AllRecords"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}

	private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
		String allColumns[] = {"Index", "EntityName", "TabName", "DbId", "ColumnName", "ColumnType", "OffSet", "PageSize", "SortingOrder", "StatusCode",
				"IsResponseJson", "IsResultSorted", "Comment", "AllRecords"};
		for (String column : allColumns) {
			if (!resultsMap.containsKey(column))
				resultsMap.put(column, "null");
		}
		dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
	}


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getShowPageTabColumnSortingConfigDetail();

	}

	public void getShowPageTabColumnSortingConfigDetail() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data");

		try {

			sortingColumnResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("SortingColumnResultCSVFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			showPageTabColumnsSortingCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("ShowPageTabsColumnSortingConfigFilePath");
			showPageTabColumnsSortingCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("ShowPageTabsColumnSortingConfigFileName");
			dumpResultsObj = new DumpResultsIntoCSV(sortingColumnResultCSVFilePath, "ShowPageTabColumnSorting.csv", setHeadersInCSVFile());

		} catch (Exception e) {
			logger.error("Exception While Reading Config File at before Suit Level : [{}]", e.getLocalizedMessage());
			failedInConfigReading = true;
		}

	}


	@BeforeMethod
	public void beforeMethod(Method method) {

		if (failedInConfigReading) {
			throw new SkipException("Skipping tests because Some Error while fetching Configuration detail at beforeClass Level");
		} else {
			logger.info("In Before Method");
			logger.info("method name is: {} ", method.getName());
			logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

		}
	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "TestShowPageTabsColumnSortingData", parallel = false)
	public Object[][] getTestShowPageTabsColumnSortingData(ITestContext c) {

		Object[][] groupArray = new Object[][]{};
		try {
			Boolean testForAllEntities = false;
			int i = 0;

			hashMapforPayloadProperties = new HashMap<String, String>();
			hashMapforTabsUrlId = new HashMap<String, Integer>();
			allEntitySection = ParseConfigFile.getAllSectionNames(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName);
			entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "entitysectionsplitter");
			randomShowPageIdsToBeValidated = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "randomshowpageidtovalidate"));

			pageSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "pagesizeforpagination"));

			orderDirectionDesc = ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName,
					"orderdirectiondesc");
			orderDirectionAsc = ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName,
					"orderdirectionasc");

			columnsToAvoid = Arrays.asList(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "columnstoavoid").split(","));

			List<String> entityRequestPayloadProperties = ParseConfigFile.getAllPropertiesOfSection(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "request_payload");
			for (String payloadProperties : entityRequestPayloadProperties) {
				hashMapforPayloadProperties.put(payloadProperties, ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "request_payload", payloadProperties));
			}

			List<String> allTabsName = ParseConfigFile.getAllPropertiesOfSection(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "tab layout urls");
			for (String tabName : allTabsName) {
				hashMapforTabsUrlId.put(tabName, Integer.parseInt(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "tab layout urls", tabName)));
			}


			// for getting all section
			if (!ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName,
					"testforallentities").trim().equalsIgnoreCase(""))
				testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "testforallentities"));


			if (!testForAllEntities) {
				allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "entitytotest").split(entitySectionSplitter));
			} else {
				allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, "allentitytotest").split(entitySectionSplitter));
			}

			groupArray = new Object[allEntitySection.size()][];


			// getting all section Ends Here
			for (String entitySection : allEntitySection) {
				groupArray[i] = new Object[4];
				if (entitySection.equalsIgnoreCase("request_payload") || entitySection.equalsIgnoreCase("tab layout urls")) {
					continue;
				}
				List<String> tabsToValidate = Arrays.asList(ParseConfigFile.getValueFromConfigFile(showPageTabColumnsSortingCfgFilePath, showPageTabColumnsSortingCfgFileName, entitySection, "tabstovalidate").split(entitySectionSplitter));

				Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));
				Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));

				groupArray[i][0] = entitySection; // EntityName
				groupArray[i][1] = tabsToValidate; // All the tabs that needed to be validated
				groupArray[i][2] = entitySectionUrlId; // entity section url id
				groupArray[i][3] = entitySectionTypeId; // entity section type id

				logger.info("entitySection : {} , tabsToValidate: {}", entitySection, tabsToValidate);
				i++;
			}
		} catch (Exception e) {
			logger.error("Exception While Reading Config File in Data Provider : [{}]", e.getLocalizedMessage());
			failedInConfigReading = true;

		}


		logger.info("hashMapforPayloadProperties : [{}]", hashMapforPayloadProperties);

		logger.info("hashMapforTabsUrlId : [{}]", hashMapforTabsUrlId);


		return groupArray;
	}


	@Test(dataProvider = "TestShowPageTabsColumnSortingData")
	public void testTabsColumnSortingFunctionality(String entityName, List<String> tabsToValidate, int entitySectionUrlId, int entitySectionTypeId) {

		logger.info("entityName : {} , tabsToValidate: {} , entitySectionUrlId : {} , entitySectionTypeId: {}", entityName, tabsToValidate, entitySectionUrlId, entitySectionTypeId);

		List<Integer> dbIds = ListDataHelper.getListOfEntityIds(entityName);

		List<Integer> dbIdsToValidate = dbIds;

		if (randomShowPageIdsToBeValidated != -1) {
			int[] randomIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, dbIds.size(), randomShowPageIdsToBeValidated);

			dbIdsToValidate = new ArrayList<>();
			for (int i = 0; i < randomIndex.length; i++) {
				dbIdsToValidate.add(dbIds.get(randomIndex[i]));
			}
		}

		logger.info("dbIdsToValidate : [{}]", dbIdsToValidate);


		for (String tabName : tabsToValidate) {

			CustomAssert csAssertion = new CustomAssert();

			if (hashMapforTabsUrlId.containsKey(tabName)) {
				int tabId = hashMapforTabsUrlId.get(tabName);

				logger.info("********************************************************************************************************");


				allColumnQueryName = getAllColumnQueryNameForSorting(entityName, tabId, entitySectionTypeId);
				logger.info("allColumnQueryName [{}]", allColumnQueryName);

				if (!allColumnQueryName.isEmpty()) {


					for (int dbIdToValidate : dbIdsToValidate) {

						logger.info("--------------------------------------------------------------------------------------------------------------");
						logger.info("Validating tabName [{}] , for EntityName [{}] , for Show Page Id [{}]", tabName, entityName, dbIdToValidate);


						String commonString = " tabName : [" + tabName + "] belong to EntityName : [" + entityName + "] having Show Page Id: [" + dbIdToValidate + "] ";


						boolean statusCodeVerification;
						boolean isResponseJson;
						int getTotalRecordsinTab = getTotalRecordsCountInTab(tabId, entitySectionTypeId, dbIdToValidate);
						if (getTotalRecordsinTab <= 0) {
							logger.warn("There is no record for :  tabName [{}] , for EntityName [{}] , for Show Page Id [{}]", tabName, entityName, dbIdToValidate);
							continue;
						} else if (getTotalRecordsinTab == -1) {
							logger.warn("There is Only One record for :  tabName [{}] , for EntityName [{}] , for Show Page Id [{}] . no need to validate sorting here", tabName, entityName, dbIdToValidate);
							continue;
						}

						int numberOfPages = getNumberOfPages(getTotalRecordsinTab, pageSize);
						logger.info("numberOfPages [{}]", numberOfPages);
						TabListData tabListData = new TabListData();


						for (Map<String, String> columnQueryNameHashMap : allColumnQueryName) {
							// if List Page Can Be Sorted with this Particular Column and it's not a dynamic field or checkbox field
							if (columnQueryNameHashMap.get("isSortable").toLowerCase().contentEquals("true") &&
									!columnsToAvoid.contains(columnQueryNameHashMap.get("queryName").toLowerCase()) &&
									!columnQueryNameHashMap.get("queryName").toLowerCase().contains("dyn")) {


								String lastValueOfPrevPageAscOrder = null;
								String lastValueOfPrevPageDescOrder = null;
								boolean isFirstPage;


								logger.debug("Sorting Verification Started for Column name : {} ", columnQueryNameHashMap.get("name"));
								for (int i = 0; i < numberOfPages; i++) {
									isFirstPage = false;
									if (i == 0)
										isFirstPage = true;

									int offsetForAPICall = i * pageSize;
									logger.info("Validating Asc Sorting  for Column name : {} , and offset : {} ", columnQueryNameHashMap.get("name"), offsetForAPICall);
									// Sorting Column by Asc Order
									tabListData.hitTabListData(tabId, entitySectionTypeId, dbIdToValidate, offsetForAPICall, pageSize, columnQueryNameHashMap.get("queryName"), orderDirectionAsc);
									statusCodeVerification = tabListData.getApiStatusCode().contains("200");
									isResponseJson = APIUtils.validJsonResponse(tabListData.getTabListDataResponseStr());


									if (!statusCodeVerification) {

										logger.error("tabListData API Response Code is not proper for " + commonString + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
										csAssertion.assertTrue(statusCodeVerification, "tabListData API Response Code for " + commonString + " is incorrect " + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");

										Map<String, String> resultsMap = new HashMap<String, String>();
										resultsMap.put("Index", String.valueOf(++globalIndex));
										resultsMap.put("EntityName", entityName);
										resultsMap.put("TabName", tabName);
										resultsMap.put("DbId", String.valueOf(dbIdToValidate));
										resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
										resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
										resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
										resultsMap.put("PageSize", String.valueOf(pageSize));
										resultsMap.put("SortingOrder", orderDirectionAsc);
										resultsMap.put("StatusCode", tabListData.getApiStatusCode());
										resultsMap.put("IsResponseJson", "NA");
										resultsMap.put("IsResultSorted", "false");
										resultsMap.put("Comment", "NA");
										resultsMap.put("AllRecords", "NA");
										dumpResultsIntoCSV(resultsMap);


									} else if (!isResponseJson) {


										logger.error("tabListData API Response is not proper for " + commonString + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
										csAssertion.assertTrue(isResponseJson, "tabListData API Response for " + commonString + " is not valid Json" + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");

										Map<String, String> resultsMap = new HashMap<String, String>();
										resultsMap.put("Index", String.valueOf(++globalIndex));
										resultsMap.put("EntityName", entityName);
										resultsMap.put("TabName", tabName);
										resultsMap.put("DbId", String.valueOf(dbIdToValidate));
										resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
										resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
										resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
										resultsMap.put("PageSize", String.valueOf(pageSize));
										resultsMap.put("SortingOrder", orderDirectionAsc);
										resultsMap.put("StatusCode", tabListData.getApiStatusCode());
										resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
										resultsMap.put("IsResultSorted", "false");
										resultsMap.put("Comment", "NA");
										resultsMap.put("AllRecords", "NA");
										dumpResultsIntoCSV(resultsMap);


									} else {
										logger.debug("tabListData API Response Code and Response is proper for " + commonString + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Asc Direction ");
										List<String> allRecords = SortingHelper.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")), tabListData.getTabListDataResponseStr());
										logger.debug("All Records List is : --> {}", allRecords);

										String valueOfLastRecord = allRecords.get(allRecords.size() - 1);
										String valueOfFirstRecord = allRecords.get(0);
										boolean isRecordsSorted = SortingHelper.isRecordsSortedProperly(allRecords, columnQueryNameHashMap.get("type"), columnQueryNameHashMap.get("name"), orderDirectionAsc);
										csAssertion.assertTrue(isRecordsSorted, "Records are not Properly Sorted even after applying sorting (asc nulls first) for column " + columnQueryNameHashMap.get("queryName"));
										Map<String, String> resultsMap = new HashMap<String, String>();
										if (isRecordsSorted) {
											if (numberOfPages == 1) {
												resultsMap.put("Index", String.valueOf(++globalIndex));
												resultsMap.put("EntityName", entityName);
												resultsMap.put("TabName", tabName);
												resultsMap.put("DbId", String.valueOf(dbIdToValidate));
												resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
												resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
												resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
												resultsMap.put("PageSize", String.valueOf(pageSize));
												resultsMap.put("SortingOrder", orderDirectionAsc);
												resultsMap.put("StatusCode", tabListData.getApiStatusCode());
												resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
												resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
												resultsMap.put("Comment", "NA");
												resultsMap.put("AllRecords", "NA");

											} else if (isFirstPage) {
												lastValueOfPrevPageAscOrder = valueOfLastRecord;
												resultsMap.put("Index", String.valueOf(++globalIndex));
												resultsMap.put("EntityName", entityName);
												resultsMap.put("TabName", tabName);
												resultsMap.put("DbId", String.valueOf(dbIdToValidate));
												resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
												resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
												resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
												resultsMap.put("PageSize", String.valueOf(pageSize));
												resultsMap.put("SortingOrder", orderDirectionAsc);
												resultsMap.put("StatusCode", tabListData.getApiStatusCode());
												resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
												resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
												resultsMap.put("Comment", "NA");
												resultsMap.put("AllRecords", "NA");
											} else {

												boolean isPaginationCorrect = SortingHelper.isPaginationCorrect(lastValueOfPrevPageAscOrder, valueOfFirstRecord, columnQueryNameHashMap.get("name"), columnQueryNameHashMap.get("type"), orderDirectionAsc);

												if (isPaginationCorrect) {
													//columnSortingResultInfo = new ColumnSortingResultInfo(String.valueOf(++index), entitySection, columnQueryNameHashMap.get("name"), columnQueryNameHashMap.get("type"), String.valueOf(offsetForAPICall), String.valueOf(pageSize), orderDirectionAsc, tabListData.getApiStatusCode(), String.valueOf(isResponseJson), String.valueOf(isRecordsSorted), "NA", "");
													resultsMap.put("Index", String.valueOf(++globalIndex));
													resultsMap.put("EntityName", entityName);
													resultsMap.put("TabName", tabName);
													resultsMap.put("DbId", String.valueOf(dbIdToValidate));
													resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
													resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
													resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
													resultsMap.put("PageSize", String.valueOf(pageSize));
													resultsMap.put("SortingOrder", orderDirectionAsc);
													resultsMap.put("StatusCode", tabListData.getApiStatusCode());
													resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
													resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
													resultsMap.put("Comment", "NA");
													resultsMap.put("AllRecords", "NA");

												} else {
													logger.error("lastValueOfPrevPageAscOrder :{}", lastValueOfPrevPageAscOrder);
													logger.error("valueOfFirstRecord :{}", valueOfFirstRecord);
													csAssertion.assertTrue(false, "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageAscOrder + "] is not Less than or equal the first value of Current Page [" + valueOfFirstRecord + "]" + " for column " + columnQueryNameHashMap.get("queryName"));
													resultsMap.put("Index", String.valueOf(++globalIndex));
													resultsMap.put("EntityName", entityName);
													resultsMap.put("TabName", tabName);
													resultsMap.put("DbId", String.valueOf(dbIdToValidate));
													resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
													resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
													resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
													resultsMap.put("PageSize", String.valueOf(pageSize));
													resultsMap.put("SortingOrder", orderDirectionAsc);
													resultsMap.put("StatusCode", tabListData.getApiStatusCode());
													resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
													resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
													resultsMap.put("Comment", "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageAscOrder + "] is not Less than or equal the first value of Current Page [" + valueOfFirstRecord + "]");
													resultsMap.put("AllRecords", "NA");

												}

												lastValueOfPrevPageAscOrder = valueOfLastRecord;

											}

										} else {

											lastValueOfPrevPageAscOrder = valueOfLastRecord;
											resultsMap.put("Index", String.valueOf(++globalIndex));
											resultsMap.put("EntityName", entityName);
											resultsMap.put("TabName", tabName);
											resultsMap.put("DbId", String.valueOf(dbIdToValidate));
											resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
											resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
											resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
											resultsMap.put("PageSize", String.valueOf(pageSize));
											resultsMap.put("SortingOrder", orderDirectionAsc);
											resultsMap.put("StatusCode", tabListData.getApiStatusCode());
											resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
											resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
											resultsMap.put("Comment", "Column is not sorted properly please check Logs");
											resultsMap.put("AllRecords", "NA");

										}

										// dumping the result in CSV file;
										dumpResultsIntoCSV(resultsMap);

									}


									logger.info("Validating Desc Sorting  for Column name : {} , and offset : {} ", columnQueryNameHashMap.get("name"), offsetForAPICall);


									// Sorting Column by Desc Order
									tabListData.hitTabListData(tabId, entitySectionTypeId, dbIdToValidate, offsetForAPICall, pageSize, columnQueryNameHashMap.get("queryName"), orderDirectionDesc);
									statusCodeVerification = tabListData.getApiStatusCode().contains("200");
									isResponseJson = APIUtils.validJsonResponse(tabListData.getTabListDataResponseStr());

									if (!statusCodeVerification) {
										logger.error("tabListData API Response Code is not proper for " + commonString + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
										csAssertion.assertTrue(statusCodeVerification, "tabListData API Response Code for " + commonString + " is incorrect " + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
										Map<String, String> resultsMap = new HashMap<String, String>();
										resultsMap.put("Index", String.valueOf(++globalIndex));
										resultsMap.put("EntityName", entityName);
										resultsMap.put("TabName", tabName);
										resultsMap.put("DbId", String.valueOf(dbIdToValidate));
										resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
										resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
										resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
										resultsMap.put("PageSize", String.valueOf(pageSize));
										resultsMap.put("SortingOrder", orderDirectionDesc);
										resultsMap.put("StatusCode", tabListData.getApiStatusCode());
										resultsMap.put("IsResponseJson", "NA");
										resultsMap.put("IsResultSorted", "false");
										resultsMap.put("Comment", "NA");
										resultsMap.put("AllRecords", "NA");
										dumpResultsIntoCSV(resultsMap);
									} else if (!isResponseJson) {
										logger.error("tabListData API Response is not proper for " + commonString + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
										csAssertion.assertTrue(isResponseJson, "tabListData API Response for " + commonString + " is not valid Json" + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
										Map<String, String> resultsMap = new HashMap<String, String>();
										resultsMap.put("Index", String.valueOf(++globalIndex));
										resultsMap.put("EntityName", entityName);
										resultsMap.put("TabName", tabName);
										resultsMap.put("DbId", String.valueOf(dbIdToValidate));
										resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
										resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
										resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
										resultsMap.put("PageSize", String.valueOf(pageSize));
										resultsMap.put("SortingOrder", orderDirectionDesc);
										resultsMap.put("StatusCode", tabListData.getApiStatusCode());
										resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
										resultsMap.put("IsResultSorted", "false");
										resultsMap.put("Comment", "NA");
										resultsMap.put("AllRecords", "NA");
										dumpResultsIntoCSV(resultsMap);
									} else {
										logger.debug("tabListData API Response Code and Response is proper for Entity: " + entityName + " While Sorting with Column : " + columnQueryNameHashMap.get("name") + " In Desc Direction ");
										List<String> allRecords = SortingHelper.getAllRecordForParticularColumns(Integer.parseInt(columnQueryNameHashMap.get("id")), tabListData.getTabListDataResponseStr());
										logger.debug("All Records List is : --> {}", allRecords);

										String valueOfLastRecord = allRecords.get(allRecords.size() - 1);
										String valueOfFirstRecord = allRecords.get(0);
										boolean isRecordsSorted = SortingHelper.isRecordsSortedProperly(allRecords, columnQueryNameHashMap.get("type"), columnQueryNameHashMap.get("name"), orderDirectionDesc);
										csAssertion.assertTrue(isRecordsSorted, "Records are not Properly Sorted even after applying sorting (desc nulls first) for column " + columnQueryNameHashMap.get("queryName"));
										Map<String, String> resultsMap = new HashMap<String, String>();
										if (isRecordsSorted) {

											if (numberOfPages == 1) {
												resultsMap.put("Index", String.valueOf(++globalIndex));
												resultsMap.put("EntityName", entityName);
												resultsMap.put("TabName", tabName);
												resultsMap.put("DbId", String.valueOf(dbIdToValidate));
												resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
												resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
												resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
												resultsMap.put("PageSize", String.valueOf(pageSize));
												resultsMap.put("SortingOrder", orderDirectionDesc);
												resultsMap.put("StatusCode", tabListData.getApiStatusCode());
												resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
												resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
												resultsMap.put("Comment", "NA");
												resultsMap.put("AllRecords", "NA");
											} else if (isFirstPage) {
												lastValueOfPrevPageDescOrder = valueOfLastRecord;
												//lastValueOfPrevPageAscOrder = valueOfLastRecord;
												resultsMap.put("Index", String.valueOf(++globalIndex));
												resultsMap.put("EntityName", entityName);
												resultsMap.put("TabName", tabName);
												resultsMap.put("DbId", String.valueOf(dbIdToValidate));
												resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
												resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
												resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
												resultsMap.put("PageSize", String.valueOf(pageSize));
												resultsMap.put("SortingOrder", orderDirectionDesc);
												resultsMap.put("StatusCode", tabListData.getApiStatusCode());
												resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
												resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
												resultsMap.put("Comment", "NA");
												resultsMap.put("AllRecords", "NA");
											} else {

												boolean isPaginationCorrect = SortingHelper.isPaginationCorrect(lastValueOfPrevPageDescOrder, valueOfFirstRecord, columnQueryNameHashMap.get("name"), columnQueryNameHashMap.get("type"), orderDirectionDesc);

												if (isPaginationCorrect) {
													resultsMap.put("Index", String.valueOf(++globalIndex));
													resultsMap.put("EntityName", entityName);
													resultsMap.put("TabName", tabName);
													resultsMap.put("DbId", String.valueOf(dbIdToValidate));
													resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
													resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
													resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
													resultsMap.put("PageSize", String.valueOf(pageSize));
													resultsMap.put("SortingOrder", orderDirectionDesc);
													resultsMap.put("StatusCode", tabListData.getApiStatusCode());
													resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
													resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
													resultsMap.put("Comment", "NA");
													resultsMap.put("AllRecords", "NA");
												} else {
													logger.error("lastValueOfPrevPageDescOrder :{}", lastValueOfPrevPageDescOrder);
													logger.error("valueOfFirstRecord :{}", valueOfFirstRecord);
													csAssertion.assertTrue(false, "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageDescOrder + "] is not greater than or equal the first value of Current Page [" + valueOfFirstRecord + "]" + " for column " + columnQueryNameHashMap.get("queryName"));
													resultsMap.put("Index", String.valueOf(++globalIndex));
													resultsMap.put("EntityName", entityName);
													resultsMap.put("TabName", tabName);
													resultsMap.put("DbId", String.valueOf(dbIdToValidate));
													resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
													resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
													resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
													resultsMap.put("PageSize", String.valueOf(pageSize));
													resultsMap.put("SortingOrder", orderDirectionDesc);
													resultsMap.put("StatusCode", tabListData.getApiStatusCode());
													resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
													resultsMap.put("IsResultSorted", String.valueOf(isPaginationCorrect));
													resultsMap.put("Comment", "Although Result is Sorted but Last Value of Prev Page [" + lastValueOfPrevPageDescOrder + "] is not greater than or equal the first value of Current Page [" + valueOfFirstRecord + "]");
													resultsMap.put("AllRecords", "NA");

												}

												lastValueOfPrevPageDescOrder = valueOfLastRecord;

											}


										} else {
											lastValueOfPrevPageDescOrder = valueOfLastRecord;
											resultsMap.put("Index", String.valueOf(++globalIndex));
											resultsMap.put("EntityName", entityName);
											resultsMap.put("TabName", tabName);
											resultsMap.put("DbId", String.valueOf(dbIdToValidate));
											resultsMap.put("ColumnName", columnQueryNameHashMap.get("name"));
											resultsMap.put("ColumnType", columnQueryNameHashMap.get("type"));
											resultsMap.put("OffSet", String.valueOf(offsetForAPICall));
											resultsMap.put("PageSize", String.valueOf(pageSize));
											resultsMap.put("SortingOrder", orderDirectionDesc);
											resultsMap.put("StatusCode", tabListData.getApiStatusCode());
											resultsMap.put("IsResponseJson", String.valueOf(isResponseJson));
											resultsMap.put("IsResultSorted", String.valueOf(isRecordsSorted));
											resultsMap.put("Comment", "Column is not sorted properly please check Logs");
											resultsMap.put("AllRecords", "NA");
										}

										// dumping the result in CSV file;
										dumpResultsIntoCSV(resultsMap);

									}

									logger.debug("Sorting Verification Ended for Column name : {} ", columnQueryNameHashMap.get("name"));

								}

							}

						}


					}
					logger.info("--------------------------------------------------------------------------------------------------------------");


				} else {
					logger.error("Not being able to get All Column Detail for :  tabName [{}] , for EntityName [{}] ", tabName, entityName);
					csAssertion.assertTrue(false, "Not being able to get All Column Detail for :  tabName [" + tabName + "]  , for EntityName [" + entityName + "]  ");
				}
			} else {
				logger.error("tab layout url id is not defined for this tab [{}]", tabName);
			}


			logger.info("********************************************************************************************************");

			csAssertion.assertAll();

		}


	}

	/* helper method of Getting All Column Detail For validating sorting in the tab */
	public List<Map<String, String>> getAllColumnQueryNameForSorting(String entityName, Integer tabId, Integer entityTypeId) {

		List<Map<String, String>> allColumnQueryDetailInMap = new ArrayList<>();
		TabDefaultUserListMetaData tabDefaultUserListMetaData = new TabDefaultUserListMetaData();
		Map<String, String> queryParams = new LinkedHashMap<>();
		queryParams.put("entityTypeId", entityTypeId.toString());

		tabDefaultUserListMetaData.hitTabDefaultUserListMetadata(tabId, queryParams);

		if (APIUtils.validJsonResponse(tabDefaultUserListMetaData.getTabDefaultUserListMetaDataJsonStr())) {
			allColumnQueryDetailInMap = tabDefaultUserListMetaData.getAllQueryName();
			logger.debug("allColumnQueryDetailInMap is : {}", allColumnQueryDetailInMap);
		}


		return allColumnQueryDetailInMap;


	}


	/* helper method of getting total number of Pages*/
	public int getNumberOfPages(int numberOfRecords, int pageSize) {

		int numberOfPages = 0;

		if (numberOfRecords % pageSize == 0)
			numberOfPages = numberOfRecords / pageSize;
		else
			numberOfPages = ((numberOfRecords / pageSize) + 1);

		return numberOfPages;

	}

	/* helper method of getting total number of Records from TabListRendered API Response */
	public int getTotalRecordsCountInTab(Integer tabId, Integer entityTypeId, Integer entityId) {


		TabListData tabListData = new TabListData();
		int numberOfRecords = -1;
		String tabListDataResponse = tabListData.hitTabListData(tabId, entityTypeId, entityId);
		if (APIUtils.validJsonResponse(tabListDataResponse)) {
			numberOfRecords = TabListDataHelper.getFilteredCount(tabListDataResponse);
		}

		return numberOfRecords;

	}


	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");


	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}

}
