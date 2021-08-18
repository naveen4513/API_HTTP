package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.*;
import java.util.regex.Pattern;

//@Listeners(value = MyTestListenerAdapter.class)
public class TestInvoiceLineItemBulkCreate {

	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemBulkCreate.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String bulkCreateTemplateFilePath = null;
	private Integer bulkCreateTemplateId = -1;
	private static int lineItemEntityTypeId = -1;
	private static int invoiceEntityTypeId = -1;
	private static int invoiceDetailsTabId = -1;
	private static Long schedulerJobTimeOut = 600000L;
	private static Long schedulerJobPollingTime = 5000L;
	private int invoiceId;
	private String fileName;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemBulkCreateConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemBulkCreateConfigFileName");
		bulkCreateTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateFilePath");
		bulkCreateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateId"));
		lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");
		invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
		invoiceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"invoiceid"));
		fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkcreatetemplatefilename");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobPollingTime = Long.parseLong(temp.trim());

		invoiceDetailsTabId = TabListDataHelper.getIdForTab("invoice details");
	}

	@DataProvider
	public Object[][] dataProviderForLineItemBulkCreate() throws ConfigurationException {
		logger.info("Setting all Invoice Line Item Bulk Create Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
		} else {
			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Invoice Line Item Bulk Create Flows Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForLineItemBulkCreate")
	public void testLineItemBulkCreate(String flowToTest) throws ConfigurationException {
		CustomAssert csAssert = new CustomAssert();

		downloadFile();
		try {
			logger.info("Validating Invoice Line Item Bulk Create Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

			if (properties != null) {
				UserTasksHelper.removeAllTasks();

				logger.info("Uploading Bulk Create Template for Flow [{}]", flowToTest);

				String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, fileName, invoiceEntityTypeId, invoiceId,
						lineItemEntityTypeId, bulkCreateTemplateId);

				if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
					Fetch fetchObj = new Fetch();
					fetchObj.hitFetch();
					String fetchResponse = fetchObj.getFetchJsonStr();

					if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
						int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

						if (newTaskId == -1) {
							logger.warn("Couldn't get Task Id for Bulk Create Template Scheduler Job for Flow [{}]", flowToTest);
							throw new SkipException("Couldn't get Task Id for Bulk Create Template Scheduler Job for Flow [" + flowToTest + "]");
						}

						Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);
						String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedresult").trim();
						logger.info("Expected Result of Bulk Create Template Processing for Flow [{}] is {}", flowToTest, expectedResult);
						String jobStatus = schedulerJob.get("jobPassed").trim();

						if (jobStatus.equalsIgnoreCase("skip")) {
							logger.warn(schedulerJob.get("errorMessage") + ". Hence skipping test.");
							throw new SkipException(schedulerJob.get("errorMessage") + ". Hence skipping test.");
						}

						if (expectedResult.equalsIgnoreCase("success")) {
							if (jobStatus.equalsIgnoreCase("false")) {
								csAssert.assertTrue(false, "Bulk Create Template Processing failed for Flow [" + flowToTest +
										"] whereas it was expected to process successfully");
							} else {
								logger.info("Bulk Create Template Processed successfully for Flow [" + flowToTest + "]");
							}
						} else {
							if (jobStatus.equalsIgnoreCase("true")) {
								csAssert.assertTrue(false, "Bulk Create Template Processed successfully for Flow [" + flowToTest +
										"] whereas it was expected to fail.");
							} else {
								logger.info("Bulk Create Template Processing failed for Flow [" + flowToTest + "]");
							}
						}

						if (jobStatus.equalsIgnoreCase("true")) {
							//Delete Newly Created Invoice Line Item
							deleteInvoiceLineItem(flowToTest, invoiceId, csAssert);
						}
					} else {
						csAssert.assertTrue(false, "Fetch API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
					}
				} else {
					logger.warn("Couldn't upload Bulk Create Template Successfully for Flow [" + flowToTest + "]");
					throw new SkipException("Couldn't upload Bulk Create Template Successfully for Flow [" + flowToTest + "]");
				}
			} else {
				logger.warn("Couldn't get Properties from Config File. Hence skipping test.");
				throw new SkipException("Couldn't get Properties from Config. Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Invoice Line Item Bulk Create Template Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void deleteInvoiceLineItem(String flowToTest, int invoiceId, CustomAssert csAssert) {
		try {
			logger.info("Hitting TabListData API for Invoice");
			TabListData tabListObj = new TabListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":" + invoiceEntityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"asc\",\"filterJson\":{}}}";

			String tabListDataResponse = tabListObj.hitTabListData(invoiceDetailsTabId, invoiceEntityTypeId, invoiceId, payload);

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

				if (listData.size() > 0) {
					int idColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "id");

					if (idColumnNo != -1) {
						for (Map<Integer, Map<String, String>> oneListData : listData) {
							int serviceDataId = Integer.parseInt(oneListData.get(idColumnNo).get("valueId"));
							logger.info("Deleting Invoice Line Item Id {}", serviceDataId);
							EntityOperationsHelper.deleteEntityRecord("invoice line item", serviceDataId);
						}
					} else {
						logger.warn("Couldn't get Column No for Id and Flow [" + flowToTest + "]");
						throw new SkipException("Couldn't get Column No for Id and Flow [" + flowToTest + "]");
					}
				} else {
					logger.warn("Couldn't get List Data from TabListData API Response for Flow [" + flowToTest + "]");
					throw new SkipException("Couldn't get List Data from TabListData API Response for Flow [" + flowToTest + "]");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Invoice Id " + invoiceId + " is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.warn("Exception while deleting Invoice Line Item of Invoice Id " + invoiceId + ". [" + e.getMessage() + "]");
			throw new SkipException("Exception while deleting Invoice Line Item of Invoice Id " + invoiceId + ". [" + e.getMessage() + "]");
		}
	}

	private void downloadFile() throws ConfigurationException {
		String filePath = "src/test/resources/TestConfig/InvoiceLineItem/BulkCreate/DataFiles";
		//String configFilePath = "src/test/resources/TestConfig/InvoiceLineItem/BulkCreate";
		//String configFileName = "TestLineItemBulkCreate.cfg";
		Download download = new Download();
		download.hitDownload(filePath,fileName,1004,67,invoiceId);
		System.out.println("Checking for downloaded file");

		String[] stringArray = {"98023","98024","98033","98034","98035","98043","98044"};

		String[] dateColumnIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"datecolumnids").split(",");
		List list = Arrays.asList(dateColumnIds);

		Map<String,Object> dataMap = new HashMap<>();

		for(String s : stringArray) {
			Map<String, String> map = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, s);
			for(Map.Entry<String,String> entry : map.entrySet()){
				if(NumberUtils.isParsable(entry.getValue())){
					if(list.contains(entry.getKey())){
						dataMap.put(entry.getKey(), DateUtil.getJavaDate(Double.parseDouble(entry.getValue())));
					}
					else
						dataMap.put(entry.getKey(),Double.parseDouble(entry.getValue()));
				}
				else{
					dataMap.put(entry.getKey(),entry.getValue());
				}
			}

			XLSUtils.editRowDataUsingColumnId(filePath,fileName,"Invoice Line Item",6,dataMap);
		}
	}
}