package com.sirionlabs.test.reports;

import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.Reports.ReportsDefaultUserListMetadataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestReportListingData {

	private final static Logger logger = LoggerFactory.getLogger(TestReportListingData.class);
	private String configFilePath = null;
	private String configFileName = null;
	private int listDataOffset = 0;
	private int listDataSize = 20;
	private int maxNoOfRecordsToValidate = 200;
	private String listDataExpectedDateFormat;

	private ReportsListHelper listHelperObj = new ReportsListHelper();
	private ReportsDefaultUserListMetadataHelper defaultUserListHelperObj = new ReportsDefaultUserListMetadataHelper();

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportListingDataConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportListingDataConfigFileName");

		Map<String, String> defaultPropertiesMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "default");

		if (defaultPropertiesMap != null) {
			if (defaultPropertiesMap.containsKey("offset") && NumberUtils.isParsable(defaultPropertiesMap.get("offset")))
				listDataOffset = Integer.parseInt(defaultPropertiesMap.get("offset"));

			if (defaultPropertiesMap.containsKey("size") && NumberUtils.isParsable(defaultPropertiesMap.get("size")))
				listDataSize = Integer.parseInt(defaultPropertiesMap.get("size"));

			if (defaultPropertiesMap.containsKey("maxrecordstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxrecordstovalidate")))
				maxNoOfRecordsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxrecordstovalidate"));

			listDataExpectedDateFormat = defaultPropertiesMap.get("listdataexpecteddateformat");
		} else {
			logger.error("Couldn't get Default Properties from Config File [{}]", configFilePath + "/" + configFileName);
		}
	}

	@DataProvider
	public Object[][] dataProviderForListingData() throws Exception {
		logger.info("Setting All Reports to Test Listing Data.");
		List<Object[]> allTestData = new ArrayList<>();

		String reportListJsonResponse = listHelperObj.getReportListJsonResponse();

		List<Integer> reportIdsToIgnore = getReportIdsToIgnore();

		List<Integer> reportIdsToTest = getReportIdsToTest();

		logger.info("Getting All Report Ids Map.");
		Map<Integer, List<Map<String, String>>> allEntityWiseReportsMap = listHelperObj.getAllEntityWiseReportsMap(reportListJsonResponse);

		for (Map.Entry<Integer, List<Map<String, String>>> entityWiseReportMap : allEntityWiseReportsMap.entrySet()) {
			Integer entityTypeId = entityWiseReportMap.getKey();
			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			List<Map<String, String>> allReportsListOfEntity = entityWiseReportMap.getValue();

			if (reportIdsToTest.size() > 0) {

				for (Map<String, String> reportMap : allReportsListOfEntity) {
					Integer reportId = Integer.parseInt(reportMap.get("id"));
					String reportName = reportMap.get("name").trim();
					Boolean isManualReport = Boolean.parseBoolean(reportMap.get("isManualReport"));
					Boolean isListing = isListingReport(reportId);

					//Filtering out Manual Reports
					if (reportIdsToTest.contains(reportId) && !isManualReport && isListing != null && isListing) {
						allTestData.add(new Object[]{reportId, reportName, entityName});
					}
				}

			} else {
				for (Map<String, String> reportMap : allReportsListOfEntity) {
					Integer reportId = Integer.parseInt(reportMap.get("id"));
					String reportName = reportMap.get("name").trim();
					Boolean isManualReport = Boolean.parseBoolean(reportMap.get("isManualReport"));
					Boolean isListing = isListingReport(reportId);

					//Filtering out Manual Reports
					if (!reportIdsToIgnore.contains(reportId) && !isManualReport && isListing != null && isListing) {
						allTestData.add(new Object[]{reportId, reportName, entityName});
					}
				}
			}
		}
		return allTestData.toArray(new Object[0][]);
	}

	private Boolean isListingReport(Integer reportId) {
		ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData=new ReportRendererDefaultUserListMetaData();
		try {
			reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportId);
			String reportRendererDefaultUserListMetaDataJsonStr =reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
			if (ParseJsonResponse.validJsonResponse(reportRendererDefaultUserListMetaDataJsonStr))
			{
				return new JSONObject(reportRendererDefaultUserListMetaDataJsonStr).getJSONObject("reportMetadataJson").getBoolean("isListing");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test(dataProvider = "dataProviderForListingData", enabled = false)
	public void testReportListData(Integer reportId, String reportName, String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Listing for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName.trim());

			//Validate Listing Records Data
			testListingRecordsData(reportName, reportId, entityName, entityTypeId, csAssert);

		} catch (Exception e) {
			logger.error("Exception while Validating Listing Data for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Listing Data for Report [" + reportName + "] having Id " + reportId +
					" of Entity " + entityName + ". " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void testListingRecordsData(String reportName, Integer reportId, String entityName, int entityTypeId, CustomAssert csAssert) {
		try {
			//Get Fields to Test for Entity
			List<String> fieldsToTest = getFieldsToTestForReport(reportId, entityName);

			if (fieldsToTest == null || fieldsToTest.isEmpty()) {
				csAssert.assertTrue(false, "Couldn't get Fields to Test for Report Id " + reportId + " of Entity " + entityName);
			} else {
				Map<String, String> fieldsShowPageObjectMap = getFieldShowPageObjectMapForEntity(entityName, fieldsToTest);

				if (fieldsToTest.isEmpty() || fieldsShowPageObjectMap.isEmpty()) {
					csAssert.assertTrue(false, "Couldn't get Fields to Test with Show Page Object Mapping for Report Id " + reportId + " of Entity " +
							entityName);
				} else {
					String showPageExpectedDateFormat = getShowPageExpectedDateFormatForEntity(entityName);

					listHelperObj.verifyListingRecordsData(entityTypeId, reportId, listDataOffset, listDataSize, maxNoOfRecordsToValidate, fieldsToTest,
							fieldsShowPageObjectMap, showPageExpectedDateFormat, listDataExpectedDateFormat, csAssert);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Validating Listing Records Data for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Listing Records Data for Report [" + reportName + "] having Id " + reportId +
					" Entity " + entityName + ". " + e.getMessage());
		}
	}

	private List<Integer> getReportIdsToIgnore() {
		List<Integer> reportIdsToIgnore = new ArrayList<>();

		String[] idsToIgnore = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
				"reportIdsToIgnore").split(Pattern.quote(","));

		for (String id : idsToIgnore) {
			reportIdsToIgnore.add(Integer.parseInt(id.trim()));
		}

		return reportIdsToIgnore;
	}

	private List<Integer> getReportIdsToTest() {
		List<Integer> reportIdsToIgnore = new ArrayList<>();

		try {
			String[] idsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
					"reportIdsToTest").split(Pattern.quote(","));

			for (String id : idsToTest) {
				reportIdsToIgnore.add(Integer.parseInt(id.trim()));
			}
		}catch (Exception e){
			logger.debug("Exception while getting report Ids To Test");
		}
		return reportIdsToIgnore;
	}

	private List<String> getFieldsToTestForReport(Integer reportId, String entityName) {
		String entityFieldsToIgnoreMappingSectionName = "entity fields to ignore mapping";
		List<String> allFieldsToTest = new ArrayList<>();

		try {
			logger.info("Getting Listing Data Fields to Test for Reports of Entity {}", entityName);
			String defaultUserListResponse = defaultUserListHelperObj.hitDefaultUserListMetadataAPIForReportId(reportId);

			logger.info("Getting All Fields Query Name of Report Id {}", reportId);
			List<String> allFieldsQueryName = defaultUserListHelperObj.getAllListDataFieldsQueryName(defaultUserListResponse);

			if (allFieldsQueryName != null) {
//				allFieldsToTest.addAll(allFieldsQueryName);

				//Temporary Code to ignore dynamic fields.
				for (String queryName : allFieldsQueryName) {
					if (!queryName.startsWith("dyn")) {
						allFieldsToTest.add(queryName);
					}
				}

				if (ParseConfigFile.hasProperty(configFilePath, configFileName, entityFieldsToIgnoreMappingSectionName, entityName)) {
					String allFieldsToIgnore[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityFieldsToIgnoreMappingSectionName,
							entityName).trim().split(Pattern.quote(","));

					for (String fieldToIgnore : allFieldsToIgnore) {
						if (allFieldsToTest.contains(fieldToIgnore.trim()))
							allFieldsToTest.remove(fieldToIgnore.trim());
					}

					if (!allFieldsToTest.contains("id")) {
						allFieldsToTest.add("id");
					}
				} else {
					logger.info("Couldn't find Fields to Ignore Mapping for Report Id {} of Entity {} in Config Section [{}]", reportId, entityName,
							entityFieldsToIgnoreMappingSectionName);
				}
			} else {
				logger.info("Couldn't get All Fields Query Name of Report Id {}", reportId);
			}

			return allFieldsToTest;
		} catch (Exception e) {
			logger.error("Exception while getting List Data Fields to test for Report Id {} of Entity {}. {}", reportId, entityName, e.getStackTrace());
		}
		return null;
	}

	private Map<String, String> getFieldShowPageObjectMapForEntity(String entityName, List<String> fieldsToTest) {
		String masterEntityShowPageObjectSectionMapName = "entity show page object section mapping";
		String defaultFieldsShowPageObjectMappingSectionName = "default fields show page object mapping section";
		Map<String, String> fieldShowPageObjectMap = new HashMap<>();

		try {
			logger.info("Creating Field and Show Page Object Mapping for Entity {}", entityName);

			List<String> allFieldsToTest = new ArrayList<>();
			allFieldsToTest.addAll(fieldsToTest);

			String entityFieldsShowPageObjectMappingSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
					masterEntityShowPageObjectSectionMapName, entityName);

			if (entityFieldsShowPageObjectMappingSectionName == null || entityFieldsShowPageObjectMappingSectionName.trim().equalsIgnoreCase("")) {
				logger.warn("Couldn't find Mapping of Fields Show Page Object Section for Entity {}. Proceeding with Default Mapping", entityName);
				entityFieldsShowPageObjectMappingSectionName = defaultFieldsShowPageObjectMappingSectionName;
			}

			Map<String, String> entityShowPageObjectMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
					entityFieldsShowPageObjectMappingSectionName);
			Map<String, String> defaultShowPageObjectMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
					defaultFieldsShowPageObjectMappingSectionName);

			for (String field : allFieldsToTest) {
				if (entityShowPageObjectMap != null && defaultShowPageObjectMap != null) {
					if (entityShowPageObjectMap.containsKey(field.trim())) {
						fieldShowPageObjectMap.put(field, entityShowPageObjectMap.get(field.trim()));
					} else if (defaultShowPageObjectMap.containsKey(field.trim())) {
//						logger.warn("Couldn't find Show Page Object Mapping for Field {} of Entity {} in Section [{}]. Hence Proceeding with Default Mapping.", field.trim(),
//								entityName, entityFieldsShowPageObjectMappingSectionName);
						fieldShowPageObjectMap.put(field, defaultShowPageObjectMap.get(field.trim()));
					} else {
						if (!field.trim().equalsIgnoreCase("sourceId")) {
//							logger.warn("Couldn't find Show Page Object Mapping for Field {} of Entity {}. Hence removing it.", field, entityName);
							System.out.println(field.trim());
							fieldsToTest.remove(field);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting List Data Fields and Show Page Object Map. {}", e.getMessage());
		}
		return fieldShowPageObjectMap;
	}

	private String getShowPageExpectedDateFormatForEntity(String entityName) {
		String showPageExpectedDateFormat = null;
		String showPageExpectedDateFormatMappingSectionName = "entity show page expected date format mapping";

		try {
			logger.info("Getting Show Page Expected Date Format for Entity {}", entityName);
			Map<String, String> allShowPageExpectedDateFormatsMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName,
					showPageExpectedDateFormatMappingSectionName);

			if (allShowPageExpectedDateFormatsMap != null) {
				if (allShowPageExpectedDateFormatsMap.containsKey(entityName.trim())) {
					showPageExpectedDateFormat = allShowPageExpectedDateFormatsMap.get(entityName.trim());
				} else {
					logger.warn("Show Page Expected Date Format Mapping not available for Entity {} under Section [{}]. Hence considering Default Date Format", entityName,
							showPageExpectedDateFormatMappingSectionName);
					showPageExpectedDateFormat = allShowPageExpectedDateFormatsMap.get("default");
				}
			} else {
				logger.error("Couldn't get Show Page Expected Date Format Map from Config File [{}]", configFilePath + "/" + configFileName);
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Show Page Expected Date Format for Entity {}. {}", entityName, e.getStackTrace());
		}
		return showPageExpectedDateFormat;
	}

	@Test(dataProvider = "dataProviderForListingData")
	public void testReportListingPagination(Integer reportId, String reportName, String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Listing Pagination for Report [{}] having Id {} of Entity {}.", reportName, reportId, entityName);

			String reportListDataResponse = listHelperObj.hitListDataAPIForReportId(reportId);
			int[] limitArr = {20, 50, 100};


			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

			int totalRecords = ListDataHelper.getTotalListDataCount(reportListDataResponse);

			if (totalRecords == -1) {
				throw new SkipException("Couldn't get Total No of Records in Listing of Report [" + reportName + "] having Id " + reportId + " of Entity " + entityName);
			}

			if (totalRecords == 0) {
				logger.info("No Record found in Listing of Report [" + reportName + "] having Id " + reportId + " of Entity {}", entityName);
				return;
			}

			logger.info("Total Records present for Report [" + reportName + "] having Id " + reportId + " of Entity {} are: {}", entityName, totalRecords);

			for (int size : limitArr) {
				int offset = 0;

				do {
					logger.info("Hitting ListData API for Report [" + reportName + "] having Id " + reportId + " of Entity {} with Size: {} and Offset: {}",
							entityName, size, offset);

					String payloadForListData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size +
							",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";

					reportListDataResponse = listHelperObj.hitListDataAPIForReportId(reportId, payloadForListData);

					if (ParseJsonResponse.validJsonResponse(reportListDataResponse)) {
						JSONObject jsonObj = new JSONObject(reportListDataResponse);
						JSONArray jsonArr = jsonObj.getJSONArray("data");

						int noOfData = jsonArr.length();
						int expectedNoOfData = (size + offset) > totalRecords ? (totalRecords % size) : size;

						csAssert.assertTrue(expectedNoOfData == noOfData, "Pagination failed for Listing of Report [" + reportName + "] having Id " +
								reportId + " of Entity " + entityName + ", size " + size + " and offset " + offset + ". Expected No of Records: " + expectedNoOfData +
								" and Actual No of Records: " + noOfData);
					} else {
						csAssert.assertTrue(false, "ListData API Response for Report [" + reportName + "] having Id " + reportId + " of Entity " +
								entityName + ",size " + size + " and offset " + offset + " is an Invalid JSON.");
					}

					offset += size;
				} while (offset <= totalRecords);
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Listing Pagination for Report [" + reportName + "] having Id " + reportId +
					" of Entity " + entityName + ". " + e.getMessage());
		}
		csAssert.assertAll();
	}
}