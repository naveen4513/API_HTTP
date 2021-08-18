package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.localmongo.LocalMongoHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestServiceDataPricingTemplateBulkUpload {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceDataPricingTemplateBulkUpload.class);
	private PostgreSQLJDBC postgreSQLJDBC;
	private String configFilePath = null;
	private String configFileName = null;
	private String pricingTemplateFilePath = null;
	private Integer pricingTemplateId = -1;
	private int serviceDataEntityTypeId = -1;
	private Long schedulerJobTimeOut = 600000L;
	private Long schedulerJobPollingTime = 5000L;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataPricingTemplateBulkUploadTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataPricingTemplateBulkUploadTestConfigFileName");
		pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingTemplateFilePath");
		pricingTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingTemplateId"));
		serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobPollingTime = Long.parseLong(temp.trim());


		postgreSQLJDBC = new PostgreSQLJDBC();

	}


	@DataProvider
	public Object[][] dataProviderForTestServiceDataPricingTemplateUpload() throws ConfigurationException {
		logger.info("Setting all Pricing Template Upload Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();
		List<ExcelData> filesList = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				filesList.add(new ExcelData(Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "entityid")), ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "pricingtemplatefilename")));
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Service Data Pricing Template Bulk Upload Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}

		LocalMongoHelper localMongoHelper = new LocalMongoHelper();
		List<String> response = localMongoHelper.getExcelProperties("pricing");


		String[] sheets = new String[]{"Pricing","ARCRRC"};

		for (String str : response) {
			if (ParseJsonResponse.validJsonResponse(str)) {
				JSONObject jsonObject = new JSONObject(str);

				String fileName = jsonObject.getString("_id");
				if (filesList.contains(new ExcelData(-1, fileName))) {

					Download download = new Download();
					download.hitDownload(pricingTemplateFilePath, fileName, 1010, serviceDataEntityTypeId, String.valueOf(findEntityIdFromExcelData(fileName, filesList)));


					for(String sheet : sheets){
						Set<String> set = jsonObject.getJSONObject(sheet).keySet();
						for (String rowNumber : set) {

							List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));
							Map<String, Object> dataMap;

							dataMap = new HashMap<>();

							//for (Map.Entry<String, String> entry : map.entrySet()) {
							for (String internalKey : jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).keySet()) {
								if (NumberUtils.isParsable(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))) {
									if (dateColumnIds.contains(internalKey)) {
										dataMap.put(internalKey, DateUtil.getJavaDate(Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))));
									} else
										dataMap.put(internalKey, Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey)));
								} else {
									dataMap.put(internalKey, jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey));
								}
							}

							boolean editDone = XLSUtils.editRowDataUsingColumnId(pricingTemplateFilePath, fileName, sheet, Integer.parseInt(rowNumber), dataMap);
							System.out.println(editDone);

						}
					}
				}


			}
		}


		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestServiceDataPricingTemplateUpload",enabled = false)
	public void testServiceDataPricingTemplateUpload(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Service Data Pricing Template Upload Flow [{}]", flowToTest);
			logger.info("Uploading Service Data Pricing Template for Flow [{}]", flowToTest);
			String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingTemplateFileName");

			if (!(new File(pricingTemplateFilePath + "/" + pricingTemplateFileName).exists())) {
				throw new SkipException("Couldn't find Pricing Template File at Location: " + pricingTemplateFilePath + "/" + pricingTemplateFileName);
			}

			String uploadResponse = BulkTemplate.uploadPricingTemplate(pricingTemplateFilePath, pricingTemplateFileName, serviceDataEntityTypeId, pricingTemplateId);
//
			String expectedMessage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedMessage");

			logger.info("Actual Pricing Template Upload API Response: {} and Expected Result: {}", uploadResponse, expectedMessage);
			if (uploadResponse == null || !uploadResponse.trim().toLowerCase().contains(expectedMessage.trim().toLowerCase())) {
				csAssert.assertTrue(false, "Pricing Template Upload Response doesn't match with Expected Response for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Service Data Pricing Template Upload for Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-98277, TC-98278
	@Test(priority = 1)
	public void testServiceDataPricingTemplateColumns() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Service Data Pricing Template Columns.");
			logger.info("Hitting ListData API for Service Data records for which Pricing is Available.");

			ListRendererListData listDataObj = new ListRendererListData();
			int serviceDataListId = ConfigureConstantFields.getListIdForEntity("service data");

			if (serviceDataListId == -1) {
				throw new SkipException("Couldn't get List Id for Service Data. Hence skipping test.");
			}

			String payload = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
					"\"filterJson\":{\"244\":{\"filterId\":\"244\",\"filterName\":\"pricingAvailable\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"true\",\"name\":\"Yes\"}]}}}}}";
			listDataObj.hitListRendererListData(serviceDataListId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				listDataObj.setListData(listDataResponse);
				List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
				int serviceDataId = Integer.parseInt(listData.get(0).get(idColumnNo).get("valueId"));

				logger.info("Downloading Pricing Template for Service Data Id {}", serviceDataId);
				Boolean fileDownloaded = BulkTemplate.downloadPricingTemplate(pricingTemplateFilePath, "testPricingColumns.xlsm", pricingTemplateId,
						serviceDataEntityTypeId, String.valueOf(serviceDataId));

				if (!fileDownloaded) {
					throw new SkipException("Couldn't download Pricing Template for Service Data Id " + serviceDataId + ". Hence skipping test.");
				}

				XLSUtils xlsObj = new XLSUtils(pricingTemplateFilePath, "testPricingColumns.xlsm");
				logger.info("Getting All Sheet Names in Pricing Template.");
				List<String> allSheetNames = xlsObj.getSheetNames();
				boolean pricingSheetFound = false;
				boolean arcSheetFound = false;

				//Validate Pricing, ARCRRC Sheets are present.
				logger.info("Validating Sheets Pricing and ARCRRC are present in Pricing Template");
				for (String sheetName : allSheetNames) {
					if (sheetName != null) {
						if (sheetName.trim().equalsIgnoreCase("Pricing")) {
							pricingSheetFound = true;
						} else if (sheetName.trim().equalsIgnoreCase("ARCRRC")) {
							arcSheetFound = true;
						}
					}
				}

				if (pricingSheetFound) {
					//Validate Below Columns are present in Pricing Sheet.
					logger.info("Getting All Header/Column Names present in Pricing Sheet.");
					List<String> allColumnNames = XLSUtils.getHeaders(pricingTemplateFilePath, "testPricingColumns.xlsm", "Pricing");

					if (allColumnNames.size() > 0) {
						String[] columnNamesToValidate = {"Service Start Date", "Service End Date", "Volume", "Rate"};

						for (String columnName : columnNamesToValidate) {
							if (!allColumnNames.contains(columnName)) {
								csAssert.assertTrue(false, columnName + " Column not found in Pricing Sheet of Service Data Id " + serviceDataId);
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't find Header/Column Names in Pricing Sheet of Service Data Id " + serviceDataId);
					}
				} else {
					csAssert.assertTrue(false, "Pricing Sheet not found in Pricing Template for Service Data Id " + serviceDataId);
				}

				if (arcSheetFound) {
					//Validate Below Columns are present in ARCRRC Sheet.
					logger.info("Getting All Header/Column Names present in ARCRRC Sheet.");
					List<String> allColumnNames = XLSUtils.getHeaders(pricingTemplateFilePath, "testPricingColumns.xlsm", "ARCRRC");

					if (allColumnNames.size() > 0) {
						String[] columnNamesToValidate = {"Service Start Date", "Service End Date", "Upper Volume", "Lower Volume", "Type", "Rate"};

						for (String columnName : columnNamesToValidate) {
							if (!allColumnNames.contains(columnName)) {
								csAssert.assertTrue(false, columnName + " Column not found in ARCRRC Sheet of Service Data Id " + serviceDataId);
							}
						}
					} else {
						csAssert.assertTrue(false, "Couldn't find Header/Column Names in ARCRRC Sheet of Service Data Id " + serviceDataId);
					}
				} else {
					csAssert.assertTrue(false, "ARC RRC Sheet not found in Pricing Template for Service Data Id " + serviceDataId);
				}

				//Delete Downloaded Pricing Template File.
				if (new File(pricingTemplateFilePath + "/testPricingColumns.xlsm").delete())
					logger.info("Pricing Template File testPricingColumns.xlsx deleted Successfully.");
			} else {
				csAssert.assertTrue(false, "List Data API Response for Service Data is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Service Data Pricing Template Columns. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForTestServiceDataPricingTemplateProcessing() throws ConfigurationException {
		logger.info("Setting all Pricing Template Processing Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();
		List<ExcelData> filesList = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "processingFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				filesList.add(new ExcelData(Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "entityid")), ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "pricingtemplatefilename")));
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Service Data Pricing Template Bulk Upload Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}

		LocalMongoHelper localMongoHelper = new LocalMongoHelper();
		List<String> response = localMongoHelper.getExcelProperties("pricing");


		String[] sheets = new String[]{"Pricing","ARCRRC"};

		for (String str : response) {
			if (ParseJsonResponse.validJsonResponse(str)) {
				JSONObject jsonObject = new JSONObject(str);

				String fileName = jsonObject.getString("_id");
				if (filesList.contains(new ExcelData(-1, fileName))) {

					Download download = new Download();
					download.hitDownload(pricingTemplateFilePath, fileName, 1010, serviceDataEntityTypeId, String.valueOf(findEntityIdFromExcelData(fileName, filesList)));


					for(String sheet : sheets){
						Set<String> set = jsonObject.getJSONObject(sheet).keySet();
					for (String rowNumber : set) {

						List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));
						Map<String, Object> dataMap;

						dataMap = new HashMap<>();

						//for (Map.Entry<String, String> entry : map.entrySet()) {
						for (String internalKey : jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).keySet()) {
							if (NumberUtils.isParsable(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))) {
								if (dateColumnIds.contains(internalKey)) {
									dataMap.put(internalKey, DateUtil.getJavaDate(Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))));
								} else
									dataMap.put(internalKey, Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey)));
							} else {
								dataMap.put(internalKey, jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey));
							}
						}

						boolean editDone = XLSUtils.editRowDataUsingColumnId(pricingTemplateFilePath, fileName, sheet, Integer.parseInt(rowNumber), dataMap);
						System.out.println(editDone);

					}
				}
				}


			}
		}

		return allTestData.toArray(new Object[0][]);

	}

	private int findEntityIdFromExcelData(String find, List<ExcelData> list) {

		for (ExcelData excelData : list) {
			if (excelData.equals(new ExcelData(-1, find))) {
				return excelData.getEntityId();
			}
		}
		return -1;
	}

	@Test(dataProvider = "dataProviderForTestServiceDataPricingTemplateProcessing", priority = 2,enabled = false)
	public void testServiceDataPricingTemplateProcessing(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Service Data Pricing Template Processing Flow [{}]", flowToTest);
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingTemplateFileName");
			UserTasksHelper.removeAllTasks();

			logger.info("Uploading Pricing Template for Flow [{}]", flowToTest);
			String uploadResponse = BulkTemplate.uploadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataEntityTypeId, pricingTemplateId);

			if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				String fetchResponse = fetchObj.getFetchJsonStr();

				if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
					int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

					if (newTaskId == -1) {
						throw new SkipException("Couldn't get Task Id for Pricing Template Scheduler Job for Flow [" + flowToTest + "]");
					}

					Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);
					String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
					logger.info("Expected Result of Pricing Template Processing for Flow [{}] is {}", flowToTest, expectedResult);
					String jobStatus = schedulerJob.get("jobPassed").trim();

					if (jobStatus.equalsIgnoreCase("skip")) {
						throw new SkipException(schedulerJob.get("errorMessage") + ". Hence skipping test.");
					}

					if (expectedResult.equalsIgnoreCase("success")) {
						if (jobStatus.equalsIgnoreCase("false")) {
							csAssert.assertTrue(false, "Pricing Template Processing failed for Flow [" + flowToTest +
									"] whereas it was expected to process successfully");
						} else {
							logger.info("Pricing Template Processing processed successfully for Flow [" + flowToTest + "]");
						}
					} else {
						if (jobStatus.equalsIgnoreCase("true")) {
							csAssert.assertTrue(false, "Pricing Template Processing processed successfully for Flow [" + flowToTest +
									"] whereas it was expected to fail.");
						} else {
							logger.info("Pricing Template Processing failed for Flow [" + flowToTest + "]");
						}
					}
				} else {
					csAssert.assertTrue(false, "Fetch API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
				}
			} else {
				throw new SkipException("Couldn't upload Pricing Template Successfully for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Service Data Pricing Template Processing for Flow [" + flowToTest + "]. " +
					e.getMessage());
		}
		csAssert.assertAll();
	}


	@DataProvider
	public Object[][] dataProviderForPricingTemplateUploadNegativeTestCases() throws ConfigurationException {
		logger.info("Setting all Pricing Template Processing Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();
		List<ExcelData> filesList = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "negativeflowstovalidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				filesList.add(new ExcelData(Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "entityid")), ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flow.trim(), "pricingtemplatefilename")));
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Service Data Pricing Template Bulk Upload Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}


		LocalMongoHelper localMongoHelper = new LocalMongoHelper();
		List<String> response = localMongoHelper.getExcelProperties("pricing");


		String[] sheets = new String[]{"Pricing","ARCRRC"};

		for (String str : response) {
			if (ParseJsonResponse.validJsonResponse(str)) {
				JSONObject jsonObject = new JSONObject(str);

				String fileName = jsonObject.getString("_id");
				if (filesList.contains(new ExcelData(-1, fileName))) {

					Download download = new Download();
					download.hitDownload(pricingTemplateFilePath, fileName, 1010, serviceDataEntityTypeId, String.valueOf(findEntityIdFromExcelData(fileName, filesList)));


					for(String sheet : sheets){
						Set<String> set = jsonObject.getJSONObject(sheet).keySet();
						for (String rowNumber : set) {

							List dateColumnIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "datecolumnids").split(","));
							Map<String, Object> dataMap;

							dataMap = new HashMap<>();

							//for (Map.Entry<String, String> entry : map.entrySet()) {
							for (String internalKey : jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).keySet()) {
								if (NumberUtils.isParsable(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))) {
									if (dateColumnIds.contains(internalKey)) {
										dataMap.put(internalKey, DateUtil.getJavaDate(Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey))));
									} else
										dataMap.put(internalKey, Double.parseDouble(jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey)));
								} else {
									dataMap.put(internalKey, jsonObject.getJSONObject(sheet).getJSONObject(rowNumber).getString(internalKey));
								}
							}

							boolean editDone = XLSUtils.editRowDataUsingColumnId(pricingTemplateFilePath, fileName, sheet, Integer.parseInt(rowNumber), dataMap);
							System.out.println(editDone);

						}
					}
				}


			}
		}

		return allTestData.toArray(new Object[0][]);
	}

	private boolean isAllExpectedErrorMessagesArethere(ArrayList<String> errorMessagesFromDb, String[] errorMessagesToValidate) {

		for (String errorMessageToValidate : errorMessagesToValidate) {
			if (!errorMessagesFromDb.contains(errorMessageToValidate.trim().toLowerCase())) {
				logger.error("Error Message [{}] should be there in for this case ", errorMessageToValidate);
				return false;
			}
		}

		return true;
	}

	@Test(dataProvider = "dataProviderForPricingTemplateUploadNegativeTestCases", priority = 3,enabled = true)
	public void testServiceDataPricingTemplateUploadNegativeTestCases(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Service Data Pricing Template Processing Flow [{}]", flowToTest);
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "pricingTemplateFileName");
			String[] expectedErrorMessages = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "failuremessages").split(",");


			UserTasksHelper.removeAllTasks();

			logger.info("Uploading Pricing Template for Flow [{}]", flowToTest);
			String uploadResponse = BulkTemplate.uploadPricingTemplate(pricingTemplateFilePath, templateFileName, serviceDataEntityTypeId, pricingTemplateId);

			if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {


				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				String fetchResponse = fetchObj.getFetchJsonStr();

				if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
					int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

					if (newTaskId == -1) {
						throw new SkipException("Couldn't get Task Id for Pricing Template Scheduler Job for Flow [" + flowToTest + "]");
					}

					// Random Sleep for DB to process record
					Thread.sleep(schedulerJobPollingTime * 25);

					String bulkEditRequestTableName = "bulk_edit_request";
					String entityBulkEditPendingTableName = "entity_bulk_edit_pending";

					String query = "select id from " + bulkEditRequestTableName + " where file_name  = '" + templateFileName + "'  and request_date >= now()::date  order by id desc limit 1";
					logger.debug("query is {}", query);
					List<List<String>> result = postgreSQLJDBC.doSelect(query);

					if (result.isEmpty()) {
						csAssert.assertTrue(false, "There is no such entry in the bulk_edit_request for the uploaded file ");
					} else {

						int idForEntityBulkEditPendingTableName = Integer.parseInt(result.get(0).get(0)); // hardcoded 0 zero since we will validate only one record

						query = "select error_data from " + entityBulkEditPendingTableName + " where  request_date >= now()::date  and bulk_edit_request_id = " + idForEntityBulkEditPendingTableName;
						logger.debug("query is {}", query);
						result = postgreSQLJDBC.doSelect(query);

						String errorMessage = result.get(0).get(0);
						JSONArray errors = new JSONArray(errorMessage);
						ArrayList<String> allErrorMessages = new ArrayList<>();

						for (int i = 0; i < errors.length(); i++) {
							JSONObject error = errors.getJSONObject(i);
							if (error.has("messages")) {
								allErrorMessages.add(error.get("messages").toString().trim().toLowerCase());
							}
						}
						csAssert.assertTrue(isAllExpectedErrorMessagesArethere(allErrorMessages, expectedErrorMessages), "Not Appropriate Error Messages While Uploading the file " + templateFileName + " for flow " + flowToTest);
					}
				}
			} else {
				throw new SkipException("Couldn't upload Pricing Template Successfully for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Service Data Pricing Template Negative Flow [" + flowToTest + "]. " +
					e.getMessage());
		}
		csAssert.assertAll();
	}

	class ExcelData {

		private String filename;
		private int entityId;

		ExcelData(int entityId, String fileName) {
			this.entityId = entityId;
			this.filename = fileName;
		}

		public String getFilename() {
			return filename;
		}

		public int getEntityId() {
			return entityId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ExcelData excelData = (ExcelData) o;
			return filename.equals(excelData.getFilename());
		}

		@Override
		public int hashCode() {
			return Objects.hash(filename);
		}
	}

}

