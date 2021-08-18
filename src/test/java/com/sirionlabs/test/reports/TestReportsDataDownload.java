package com.sirionlabs.test.reports;

import com.sirionlabs.api.reportRenderer.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.*;

public class TestReportsDataDownload {

	private final static Logger logger = LoggerFactory.getLogger(TestReportsDataDownload.class);

	private String configFilePath = null;
	private String configFileName = null;
	private String csrfToken;

	private String outputFilePath = "src/test/output";
	private ReportsListHelper listHelperObj = new ReportsListHelper();

	@BeforeClass
	public void beforeClass() {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestReportDataDownloadConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestReportDataDownloadConfigFileName");
		csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
	}

	@DataProvider(parallel = false)
	public Object[][] dataProviderForReportDataDownload() throws Exception {
		logger.info("Setting All Reports to Test");
		List<Object[]> allTestData = new ArrayList<>();

		logger.info("Getting All Report Ids Map.");
		Map<Integer, List<Map<String, String>>> allEntityWiseReportsMap = listHelperObj.getAllEntityWiseReportsMap();

		List<Integer> reportIdsToIgnore = getReportsToIgnore();
		String reportIdsToTest =  ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"report ids to test");
		List<String> reportIdsToTestList = new ArrayList<>();

		if (reportIdsToTest != null && !reportIdsToTest.trim().equalsIgnoreCase("")) {
			reportIdsToTestList =Arrays.asList(reportIdsToTest.split(","));

		}
		outerLoop:
		for (Map.Entry<Integer, List<Map<String, String>>> entityWiseReportMap : allEntityWiseReportsMap.entrySet()) {
			Integer entityTypeId = entityWiseReportMap.getKey();
			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			List<Map<String, String>> allReportsListOfEntity = entityWiseReportMap.getValue();

			for (Map<String, String> reportMap : allReportsListOfEntity) {
				Integer reportId = Integer.parseInt(reportMap.get("id"));

				if (reportIdsToIgnore.contains(reportId)) {
					continue;
				}

				String reportName = reportMap.get("name").trim();

				if(reportIdsToTestList.size() !=0) {
					if (reportIdsToTestList.contains(String.valueOf(reportId))) {
						allTestData.add(new Object[]{reportId, reportName, entityTypeId, entityName});
						continue;
					}else {
						continue;
					}
				}
				allTestData.add(new Object[]{reportId, reportName, entityTypeId, entityName});

			}
		}

		return allTestData.toArray(new Object[0][]);
	}

	private List<Integer> getReportsToIgnore() {
		List<Integer> reportsToIgnore = new ArrayList<>();

		String reportsToIgnoreStr = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "default", "reportIdsToIgnore");
		if (reportsToIgnoreStr != null && !reportsToIgnoreStr.trim().equalsIgnoreCase("")) {
			String[] reportIdsArr = reportsToIgnoreStr.trim().split(",");

			for (String reportId : reportIdsArr) {
				reportsToIgnore.add(Integer.parseInt(reportId.trim()));
			}
		}

		return reportsToIgnore;
	}

	@Test(dataProvider = "dataProviderForReportDataDownload")
	public void testReportGraphDownload(Integer reportId, String reportName, int entityTypeId, String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Downloading Graph for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
			String reportRendererChartDataPayload = getReportRendererChartDataPayload(entityTypeId);
			String metaDataResponseStr = getMetaDataResponseStr(reportId, entityName);

			if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
				Boolean isChartAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isChart");

				if (isChartAvailable) {
					ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
					reportRendererListDataObj.hitReportRendererListData(reportId);
					String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
					if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
						JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
						if (reportListDataJson.getInt("totalCount") == 0)
							logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraph image file for this report.", reportName);

						else {
							ReportRendererChartData chartDataObj = new ReportRendererChartData();
							chartDataObj.hitReportRendererChartData(reportRendererChartDataPayload, reportId);
							String reportRendererChartDataResponseStr = chartDataObj.getReportRendererChartDataJsonStr();

							if (APIUtils.validJsonResponse(reportRendererChartDataResponseStr, "[reportRendererChartDataResponse]")) {

								Map<String, String> formParam = getDownloadGraphPayload(reportRendererChartDataResponseStr);
								DownloadGraph downloadGraphObj = new DownloadGraph();
								HttpResponse response = downloadGraphObj.hitDownloadGraph(formParam);

								dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraph", entityName, reportName,
										csAssert);
							} else {
								csAssert.assertTrue(false, "reportRendererChartDataResponseStr is not valid json for report = {}" + reportName);
								logger.error("reportRendererChartDataResponseStr is not valid json for entity = {} and report = {}. Hence skipping download graph validation.",
										entityName, reportName);
							}
						}
					} else {
						csAssert.assertTrue(false, "ReportRendererListData is not valid json for report = {}" + reportName);
						logger.error("ReportRendererListData response is not valid json for report = {}", reportName);
					}
				} else
					logger.warn("Chart is not available(isChart = false) for entity {} , report {}. Hence skipping download graph for this report.", entityName,
							reportName);
			} else {
				csAssert.assertTrue(false, "MetaData response is not valid json for report = {}" + reportName);
				logger.error("Meta Data response for report = {} is not valid json. downloadGraph validation skipped.", reportName);
			}
		} catch (Exception e) {
			logger.error("Exception while Downloading Graph for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Downloading Graph for Report [" + reportName + "] having Id " + reportId + " of Entity " +
					entityName);
		}
		csAssert.assertAll();
	}

	private String getReportRendererChartDataPayload(Integer entityTypeId) {
		return "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
				",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
	}

	private String getMetaDataResponseStr(Integer reportId, String entityName) {
		String responseStr = null;
		try {
			ReportRendererDefaultUserListMetaData metaDataObj = new ReportRendererDefaultUserListMetaData();
			metaDataObj.hitReportRendererDefaultUserListMetadata(reportId);
			responseStr = metaDataObj.getReportRendererDefaultUserListMetaDataJsonStr();
		} catch (Exception e) {
			logger.error("Exception occurred while hitting metaData API for entity {}", entityName);
		}
		return responseStr;
	}

	private Map<String, String> getDownloadGraphPayload(String reportRendererChartDataResponseStr) {
		Map<String, String> formParam = new HashMap<>();

		formParam.put("chartJson", reportRendererChartDataResponseStr);
		formParam.put("_csrf_token", csrfToken);

		return formParam;
	}

	private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName,
	                                                       String reportName, CustomAssert csAssert) {
		this.dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, featureName, entityName, reportName, null, csAssert);
	}

	private void dumpDownloadGraphWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName,
	                                                       String reportName, String columnStatus, CustomAssert csAssert) {
		String outputFile;
		String outputImageFormatForDownloadGraph = ".png";
		String outputFileFormatForDownloadGraphWithData = ".xlsx";
		reportName = removeSpecialCharactersFromFileName(reportName); //replacing special characters
		FileUtils fileUtil = new FileUtils();
		Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
		Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
		if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
			if (columnStatus != null)
				outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + "_" + columnStatus + outputFileFormatForDownloadGraphWithData;
			else
				outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + reportName + outputImageFormatForDownloadGraph;

			Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);

			if (status)
				logger.info("DownloadGraphWithData file generated at {}", outputFile);
			else {
				csAssert.assertTrue(false, "File not generated at [" + outputFile + "] for Report [" + reportName + "]");
			}
		}
	}

	private String removeSpecialCharactersFromFileName(String fileName) {
		String newFileName;
		logger.info("fileName before removing special character : {}", fileName);
		newFileName = fileName.replaceAll("[+^/*<:>?|]*", "");
		newFileName = newFileName.replaceAll("\\\\", "");
		newFileName = newFileName.replaceAll("\"", "");
		logger.info("fileName after removing special character : {}", newFileName);

		return newFileName;
	}

	@Test(dataProvider = "dataProviderForReportDataDownload")
	public void testReportGraphDownloadWithData(Integer reportId, String reportName, int entityTypeId, String entityName) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Downloading Graph with Data for Report [{}] having Id {} of Entity {}", reportName, reportId, entityName);
			String metaDataResponseStr = getMetaDataResponseStr(reportId, entityName);

			if (APIUtils.validJsonResponse(metaDataResponseStr, "[metaData Response]")) {
				Boolean isListingAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isListing");
				Boolean isDownloadAvailable = new JSONObject(metaDataResponseStr).getJSONObject("reportMetadataJson").getBoolean("isDownload");

				if (isListingAvailable && isDownloadAvailable) {
					ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
					reportRendererListDataObj.hitReportRendererListData(reportId);
					String reportListDataResponseStr = reportRendererListDataObj.getListDataJsonStr();
					if (APIUtils.validJsonResponse(reportListDataResponseStr, "[ReportRendererListData response]")) {
						JSONObject reportListDataJson = new JSONObject(reportListDataResponseStr);
						if (reportListDataJson.getInt("totalCount") == 0)
							logger.warn("ReportName = {} doesn't have any data. Hence skipping generation of downloadGraphWithData file for this report.",
									reportName);

						else {
							downloadGraphDataForAllColumns(entityTypeId, entityName, reportId, reportName, csAssert);
							downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, reportId, reportName, true,
									csAssert);
							downloadGraphDataForSelectedColumns(metaDataResponseStr, entityTypeId, entityName, reportId, reportName, false,
									csAssert);
						}
					} else {
						csAssert.assertTrue(false, "reportListData is not valid json for report = " + reportName);
						logger.error("reportListData is not valid json for report = {}. Hence generation of downloadGraphWithData file is skipped.", reportName);
					}
				} else {
					logger.warn("download is not available(isDownload = false) for entity {} and report {}. Hence skipping download data for this report.",
							entityName, reportName);
				}
			} else {
				csAssert.assertTrue(false, "Meta Data response is not valid json for report = " + reportName);
				logger.error("MetaData response is not valid json for report = {}. DownloadGraphWithData file generation skipped.", reportName);
			}
		} catch (Exception e) {
			logger.error("Exception while Downloading Graph with Data for Report [{}] having Id {} of Entity {}. {}", reportName, reportId, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Downloading Graph with Data for Report [" + reportName + "] having Id " + reportId + " of Entity " +
					entityName);
		}
		csAssert.assertAll();
	}

	private void downloadGraphDataForAllColumns(Integer entityTypeId, String entityName, Integer reportId, String reportName, CustomAssert csAssert) {
		Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId);

		DownloadGraphWithData downloadGraphWithDataObj = new DownloadGraphWithData();
		logger.info("Hitting DownloadGraphWithData for entity {} [id = {}] and reportName {} [id = {}]", entityName, entityTypeId, reportName, reportId);
		HttpResponse response = downloadGraphWithDataObj.hitDownloadGraphWithData(formParam, reportId);

		/*
		 * dumping response into file
		 * */
		dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraphWithData", entityName, reportName, "AllColumn", csAssert);
	}

	private void downloadGraphDataForSelectedColumns(String metaDataResponseStr, Integer entityTypeId, String entityName, Integer reportId, String reportName, Boolean isRandomizationRequiredOnColumn, CustomAssert csAssert) throws Exception {

		ReportRendererDefaultUserListMetaData metaDataObj = new ReportRendererDefaultUserListMetaData();
		Map<Integer, String> columnIdNameMap = metaDataObj.getColumnIdNameMap(metaDataResponseStr, isRandomizationRequiredOnColumn, 5);

		Map<String, String> formParam = getDownloadGraphWithDataPayload(entityTypeId, columnIdNameMap);
		DownloadGraphWithData downloadGraphWithDataObj = new DownloadGraphWithData();
		logger.info("Hitting DownloadGraphWithData for entity {} [id = {}] and reportName {} [id = {}]", entityName, entityTypeId, reportName, reportId);
		HttpResponse response = downloadGraphWithDataObj.hitDownloadGraphWithData(formParam, reportId);

		String columnStatus = "DefaultColumn";
		if (isRandomizationRequiredOnColumn)
			columnStatus = "RandomizedColumn";

		/*
		 * dumping response into file
		 * */
		dumpDownloadGraphWithDataResponseIntoFile(response, outputFilePath, "DownloadGraphWithData", entityName, reportName, columnStatus, csAssert);
	}

	private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId) {
		return this.getDownloadGraphWithDataPayload(entityTypeId, null);
	}

	private Map<String, String> getDownloadGraphWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap) {
		Map<String, String> formParam = new HashMap<String, String>();
		String jsonData = null;

		if (selectedColumnMap == null) {
			jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
					",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
		} else {
			String selectedColumnArray = "\"selectedColumns\":[";
			for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
				selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
			}
			selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
			selectedColumnArray += "]";

			jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + 0 + ",\"size\":" + 20 +
					",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}," + selectedColumnArray + "}";
		}

		formParam.put("jsonData", jsonData);
		formParam.put("_csrf_token", csrfToken);

		return formParam;
	}
}