package com.sirionlabs.test.invoice;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
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
import org.apache.http.Header;
import org.apache.http.HttpResponse;
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
public class TestInvoiceBulkCreate {

	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceBulkCreate.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String bulkCreateTemplateFilePath = null;
	private Integer bulkCreateTemplateId = -1;
	private static int invoiceEntityTypeId = -1;
	private static int contractEntityTypeId = -1;
	private static int invoiceListId = -1;
	private static Long schedulerJobTimeOut = 600000L;
	private static Long schedulerJobPollingTime = 5000L;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceBulkCreateTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceBulkCreateTestConfigFileName");
		bulkCreateTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateFilePath");
		bulkCreateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "templateId"));
		invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
		contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerJobPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerJobPollingTime = Long.parseLong(temp.trim());

		invoiceListId = ConfigureConstantFields.getListIdForEntity("invoices");
	}

	//TC-100109
	@Test (enabled = false)
	public void testInvoiceBulkCreateLink() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Invoice Bulk Create Link present in Contract Show Page API Response.");
			int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create link validation",
					"contractId"));

			logger.info("Hitting Show Page API for Contract Id {}", contractId);
			Show showObj = new Show();
			showObj.hitShow(contractEntityTypeId, contractId);
			String showResponse = showObj.getShowJsonStr();

			validateInvoiceBulkCreateLinkInShowResponse(showResponse, "Contract", contractId, csAssert);

			logger.info("Validating Invoice Bulk Create Link present in Supplier Show Page API Response.");
			int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulk create link validation",
					"supplierId"));

			logger.info("Hitting Show Page API for Supplier Id {}", supplierId);
			int supplierEntityTypeId = ConfigureConstantFields.getEntityIdByName("suppliers");
			showObj.hitShow(supplierEntityTypeId, supplierId);
			showResponse = showObj.getShowJsonStr();

			validateInvoiceBulkCreateLinkInShowResponse(showResponse, "Supplier", supplierId, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Invoice Bulk Create Link. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void validateInvoiceBulkCreateLinkInShowResponse(String showResponse, String entityName, int entityId, CustomAssert csAssert) {
		if (ParseJsonResponse.validJsonResponse(showResponse)) {
			JSONObject jsonObj = new JSONObject(showResponse);
			jsonObj = jsonObj.getJSONObject("createLinks");
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			if (jsonArr.length() > 0) {
				for (int i = 0; i < jsonArr.length(); i++) {
					if (jsonArr.getJSONObject(i).getString("label").trim().equalsIgnoreCase("Invoice")) {
						JSONArray fieldsArr = jsonArr.getJSONObject(i).getJSONArray("fields");

						if (fieldsArr.length() > 0) {
							Boolean bulkLinkFound = false;

							for (int j = 0; j < fieldsArr.length(); j++) {
								if (fieldsArr.getJSONObject(j).getString("label").trim().equalsIgnoreCase("Bulk")) {
									bulkLinkFound = true;
									break;
								}
							}

							if (!bulkLinkFound) {
								csAssert.assertTrue(false, "Bulk Link not Present in Fields JSONArray of Invoice in CreateLink Object " +
										"of Show Page API for " + entityName + " Id " + entityId);
							}
						} else {
							csAssert.assertTrue(false, "No Link present in Fields JSONArray of Invoice in CreateLink Object of Show Page API" +
									" for " + entityName + " Id " + entityId);
						}
						break;
					}
				}
			} else {
				csAssert.assertTrue(false, "Empty Fields JSONArray in CreateLinks Object in Show Page API Response for " + entityName + " Id " + entityId);
			}
		} else {
			csAssert.assertTrue(false, "Show Page API Response for " + entityName + " Id " + entityId + " is an Invalid JSON.");
		}
	}

	//TC-100112
	@Test(priority = 0)
	public void validateBulkCreateTemplateNameAndExtension() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Invoice Bulk Create Template");
			String sectionName = "download template validation";
			int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "contractid"));

			logger.info("Hitting Download API.");
			Download downloadObj = new Download();
			HttpResponse downloadResponse = downloadObj.hitDownload(bulkCreateTemplateId, contractEntityTypeId, contractId);

			if (downloadResponse == null)
				throw new SkipException("Unexpected Error at Downloading Invoice Bulk Create Template. Hence skipping test.");

			String expectedTemplateName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "expectedTemplateName");

			Header[] headers = downloadResponse.getAllHeaders();
			for (Header oneHeader : headers) {
				if (oneHeader.toString().trim().contains("Content-Disposition")) {
					String[] temp = oneHeader.toString().trim().split(Pattern.quote("filename=\""));
					String actualFileName = temp[1].trim();
					actualFileName = actualFileName.substring(0, actualFileName.length() - 1);

					if (!oneHeader.toString().trim().contains(expectedTemplateName)) {
						csAssert.assertTrue(false, "Expected Template Name: [" + expectedTemplateName + "] and Actual Template Name: [" +
								actualFileName + "]");
					}

					if (!actualFileName.endsWith(".xlsm")) {
						csAssert.assertTrue(false, "Invoice Bulk Create Template doesn't have Extension as xlsm.");
					}
					break;
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Bulk Create Template Name & Extension Validation. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100110
	@Test(priority = 1)
	public void validateSheetsInBulkCreateTemplate() {
		CustomAssert csAssert = new CustomAssert();

		try {
			String sectionName = "download template validation";
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "templateFileName");
			int contractId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "contractid"));
			logger.info("Downloading Invoice Bulk Create Template.");
			Boolean templateDownloaded = BulkTemplate.downloadBulkCreateTemplate(bulkCreateTemplateFilePath, templateFileName, bulkCreateTemplateId,
					contractEntityTypeId, contractId);

			if (!templateDownloaded)
				throw new SkipException("Couldn't download Invoice Bulk Create Template. Hence Skipping test.");

			XLSUtils xlsObj = new XLSUtils(bulkCreateTemplateFilePath, templateFileName);

			logger.info("Getting all Sheet Names in Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);
			List<String> allSheetNames = xlsObj.getSheetNames();

			if (allSheetNames.isEmpty()) {
				throw new SkipException("Couldn't get any sheet in Invoice Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" +
						templateFileName + "]");
			}

			String[] expectedSheetNamesArr = {"Instructions", "Information", "Invoice", "Invoice Line Item"};
			List<String> allExpectedSheetNames = new ArrayList<>();
			allExpectedSheetNames.addAll(Arrays.asList(expectedSheetNamesArr));

			for (String expectedSheetName : allExpectedSheetNames) {
				if (!allSheetNames.contains(expectedSheetName.trim())) {
					csAssert.assertTrue(false, "Invoice Bulk Create Template doesn't contain Sheet " + expectedSheetName);
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Sheets in Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100116
	@Test(priority = 2)
	public void validateInvoicesSheet() {
		CustomAssert csAssert = new CustomAssert();

		try {
			//Validate Fields in Invoices Sheet
			String sectionName = "invoices sheet validation";
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
					"templateFileName");
			logger.info("Validating Fields in Invoices Sheet of Invoice Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);

			if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
				throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
						"]. Hence skipping test");
			}

			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, sectionName);

			String expectedInvoicesFields = properties.get("expectedfieldsininvoicessheet");
			expectedInvoicesFields = expectedInvoicesFields.toLowerCase();
			String[] expectedFieldsInInvoicesSheet = expectedInvoicesFields.split(Pattern.quote(","));
			List<String> allExpectedFields = new ArrayList<>();
			allExpectedFields.addAll(Arrays.asList(expectedFieldsInInvoicesSheet));

			List<String> allHeaders = XLSUtils.getHeaders(bulkCreateTemplateFilePath, templateFileName, "Invoice");
			if (allHeaders.isEmpty()) {
				throw new SkipException("Couldn't get Headers in Invoices Sheet of Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" +
						templateFileName + "]");
			}


			for(int i=0;i<allHeaders.size();i++){
				String string = allHeaders.get(i);
				allHeaders.remove(i);
				allHeaders.add(i,string.toLowerCase());
			}

			for (String expectedField : allExpectedFields) {
				if (!allHeaders.contains(expectedField.trim())) {
					csAssert.assertTrue(false, "Invoices Sheet doesn't contain field [" + expectedField + "].");
				}
			}

			//Validate All Validation Rule
			logger.info("Validating that all Validation Rules are present in Invoices Sheet of Bulk Create Template.");
			List<String> allFields = XLSUtils.getExcelDataOfOneRow(bulkCreateTemplateFilePath, templateFileName, "Invoice", 5);
			if (allFields.isEmpty())
				throw new SkipException("Couldn't get Data at Row No 5 in Invoices Sheet of Bulk Create Template. Hence skipping test.");

			String[] expectedValidationRules = properties.get("expectedrulesininvoicessheet").split(Pattern.quote(","));
			List<String> uniqueValidationRules = new ArrayList<>();

			for (String validationRule : allFields) {
				if (!uniqueValidationRules.contains(validationRule.trim()))
					uniqueValidationRules.add(validationRule.trim());
			}

			for (String expectedRule : expectedValidationRules) {
				if (!uniqueValidationRules.contains(expectedRule.trim())) {
					csAssert.assertTrue(false, "Row No 5 in Invoices Sheet of Bulk Create Template doesn't contain Validation Rule " + expectedRule);
				}
			}

			//Validate Mandatory Fields
			logger.info("Validating that Mandatory rule is Present for all Mandatory Fields.");
			String[] expectedMandatoryFields = properties.get("expectedmandatoryfields").toLowerCase().split(Pattern.quote(","));
			List<String> allExpectedMandatoryFieldsInInvoicesSheet = new ArrayList<>();
			allExpectedMandatoryFieldsInInvoicesSheet.addAll(Arrays.asList(expectedMandatoryFields));

			for (String expectedMandatoryField : allExpectedMandatoryFieldsInInvoicesSheet) {
				expectedMandatoryField = expectedMandatoryField.trim().toLowerCase();

				if (!allHeaders.contains(expectedMandatoryField)) {
					csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not present in Invoices Sheet of Bulk Create Template");
				} else {
					if (!allFields.get(allHeaders.indexOf(expectedMandatoryField)).trim().contains("Mandatory")) {
						csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not marked as Mandatory in Invoices Sheet of " +
								"Bulk Create Template");
					}
				}
			}

			//Validate Missing Fields
			logger.info("Validating that Missing/Hidden Fields are not Present in Invoices Sheet.");
			String[] missingFields = properties.get("missingfieldsininvoicessheet").split(Pattern.quote(","));
			List<String> allExpectedMissingFieldsInInvoicesSheet = new ArrayList<>();
			allExpectedMissingFieldsInInvoicesSheet.addAll(Arrays.asList(missingFields));

			for (String expectedMissingField : allExpectedMissingFieldsInInvoicesSheet) {
				if (allHeaders.contains(expectedMissingField.trim())) {
					csAssert.assertTrue(false, "Field " + expectedMissingField + " is Present in Invoices Sheet of Bulk Create Template");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Invoices Sheet of Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100128
	@Test(priority = 4)
	public void validateInvoiceLineItemsSheet() {
		CustomAssert csAssert = new CustomAssert();

		try {
			//Validate Fields in Invoice Line Items Sheet
			String sectionName = "invoice line items sheet validation";
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
					"templateFileName");
			logger.info("Validating Fields in Invoice Line Items Sheet of Invoice Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);

			if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
				throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
						"]. Hence skipping test");
			}

			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, sectionName);

			String expectedInvoicesFields = properties.get("expectedfieldsininvoicelineitemssheet");
			expectedInvoicesFields = expectedInvoicesFields.toLowerCase();
			String[] expectedFieldsInInvoicesSheet = expectedInvoicesFields.split(Pattern.quote(","));
			List<String> allExpectedFields = new ArrayList<>();
			allExpectedFields.addAll(Arrays.asList(expectedFieldsInInvoicesSheet));

			List<String> allHeaders = XLSUtils.getHeaders(bulkCreateTemplateFilePath, templateFileName, "Invoice Line Item");
			if (allHeaders.isEmpty()) {
				throw new SkipException("Couldn't get Headers in Invoice Line Items Sheet of Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" +
						templateFileName + "]");
			}
			for(int i=0;i<allHeaders.size();i++){
				String string = allHeaders.get(i);
				allHeaders.remove(i);
				allHeaders.add(i,string.toLowerCase());
			}


			for (String expectedField : allExpectedFields) {
				if (!allHeaders.contains(expectedField.trim())) {
					csAssert.assertTrue(false, "Invoice Line Items Sheet doesn't contain field [" + expectedField + "].");
				}
			}

			//Validate All Validation Rule
			logger.info("Validating that all Validation Rules are present in Invoice Line Items Sheet of Bulk Create Template.");
			List<String> allFields = XLSUtils.getExcelDataOfOneRow(bulkCreateTemplateFilePath, templateFileName, "Invoice Line Item", 5);
			if (allFields.isEmpty())
				throw new SkipException("Couldn't get Data at Row No 5 in Invoice Line Items Sheet of Bulk Create Template. Hence skipping test.");

			String[] expectedValidationRules = properties.get("expectedrulesininvoicelineitemssheet").split(Pattern.quote("|"));
			List<String> uniqueValidationRules = new ArrayList<>();

			for (String validationRule : allFields) {
				if (!uniqueValidationRules.contains(validationRule.trim()))
					uniqueValidationRules.add(validationRule.toLowerCase().trim());
			}

			for (String expectedRule : expectedValidationRules) {
				if (!uniqueValidationRules.contains(expectedRule.toLowerCase().trim())) {
					csAssert.assertTrue(false, "Row No 5 in Invoice Line Items Sheet of Bulk Create Template doesn't contain Validation Rule " +
							expectedRule);
				}
			}

			//Validate Mandatory Fields
			logger.info("Validating that Mandatory rule is Present for all Mandatory Fields.");
			String[] expectedMandatoryFields = properties.get("expectedmandatoryfields").toLowerCase().split(Pattern.quote(","));
			List<String> allExpectedMandatoryFieldsInInvoicesSheet = new ArrayList<>();
			allExpectedMandatoryFieldsInInvoicesSheet.addAll(Arrays.asList(expectedMandatoryFields));

			for (String expectedMandatoryField : allExpectedMandatoryFieldsInInvoicesSheet) {
				expectedMandatoryField = expectedMandatoryField.trim();

				if (!allHeaders.contains(expectedMandatoryField)) {
					csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not present in Invoice Line Items Sheet of Bulk Create Template");
				} else {
					if (!allFields.get(allHeaders.indexOf(expectedMandatoryField)).trim().contains("Mandatory")) {
						csAssert.assertTrue(false, "Field " + expectedMandatoryField + " is not marked as Mandatory in Invoice Line Items Sheet of " +
								"Bulk Create Template");
					}
				}
			}

			//Validate Process Column is not present in Invoice Line Items Sheet
			logger.info("Validating that Process Field is not Present in Invoice Line Items Sheet.");
			if (allHeaders.contains("Process")) {
				csAssert.assertTrue(false, "Field Process is Present in Invoice Line Items Sheet of Bulk Create Template");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Invoice Line Items Sheet of Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100134
	@Test(priority = 5,enabled =false)
	public void validateMasterData() {
		CustomAssert csAssert = new CustomAssert();

		try {
			//Validate Master Data
			logger.info("Validating Master Data for Invoice Bulk Create Template.");
			String sectionName = "master data validation";
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
					"templateFileName");

			if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
				throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
						"]. Hence skipping test");
			}

			Map<String, String> properties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, sectionName);

			List<String> allHeaders = XLSUtils.getHeaders(bulkCreateTemplateFilePath, templateFileName, "Master Data");
			if (allHeaders.isEmpty()) {
				throw new SkipException("Couldn't get Headers in Invoice Line Items Sheet of Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" +
						templateFileName + "]");
			}

			for (Map.Entry<String, String> entry : properties.entrySet()) {
				if (allHeaders.contains(entry.getKey().trim())) {
					String[] expectedValues = entry.getValue().split(Pattern.quote(","));
					List<String> expectedValuesList = new ArrayList<>();

					for (String expectedValue : expectedValues) {
						expectedValuesList.add(expectedValue.trim());
					}

					int columnNo = allHeaders.indexOf(entry.getKey().trim());
					if (columnNo == -1) {
						throw new SkipException("Couldn't get Column No for Field " + entry.getKey() + " in Master Data Sheet. Hence Skipping test.");
					}

					logger.info("Getting all Values Present in Master Data for Field {} in Invoice Bulk Create Template.", entry.getKey());
					List<String> allValuesInMasterData = XLSUtils.getOneColumnDataFromMultipleRows(bulkCreateTemplateFilePath, templateFileName, "Master Data",
							columnNo, 3, 10);

					if (allValuesInMasterData.isEmpty()) {
						throw new SkipException("Couldn't get Values for Field " + entry.getKey() + " in Master Data Sheet of Invoice Bulk Create Template.");
					}

					for (String value : allValuesInMasterData) {
						if (!expectedValuesList.contains(value.trim())) {
							csAssert.assertTrue(false, "Value [" + value + "] present in Master Data Sheet for Field " + entry.getKey() +
									" is not Expected");
						}
					}
				} else {
					csAssert.assertTrue(false, "Field " + entry.getKey() + " not present in Master Data Sheet of Invoice Bulk Create Template.");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Master Data for Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100118
	@Test(priority = 6)
	public void validateInstructionsSheet() {
		CustomAssert csAssert = new CustomAssert();

		try {
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
					"templateFileName");
			logger.info("Validating Instructions Sheet of Invoice Bulk Create Template located at [{}]", bulkCreateTemplateFilePath + "/" + templateFileName);

			if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
				throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
						"]. Hence skipping test");
			}

			List<String> allInstructionKeys = XLSUtils.getOneColumnDataFromMultipleRows(bulkCreateTemplateFilePath, templateFileName, "Instructions",
					5, 0, 3);
			List<String> allInstructionValues = XLSUtils.getOneColumnDataFromMultipleRows(bulkCreateTemplateFilePath, templateFileName, "Instructions",
					6, 0, 3);

			if (allInstructionKeys.isEmpty() || allInstructionValues.isEmpty()) {
				throw new SkipException("Couldn't find all the Expected Instruction Keys and Values in Instructions Sheet of Bulk Create Template. Hence skipping test.");
			}

			String[] expectedKeys = {"Template Type", "Template Version", "Feature Instructions"};
			for (int i = 0; i < allInstructionKeys.size(); i++) {
				if (!allInstructionKeys.get(i).trim().equalsIgnoreCase(expectedKeys[i].trim())) {
					csAssert.assertTrue(false, "Expected Instruction Key at Row #" + (i + 1) + ": [" + expectedKeys[i].trim() +
							"] and Actual Key found is: " + allInstructionKeys.get(i).trim());
				}
			}

			String[] expectedValues = {"Bulk Create", "1.2", "1. The template is used to create entities in bulk.\n" +
					"\n" +
					"2. It is advisable that user always downloads latest template and use.\n" +
					"\n" +
					"3. A bulk create template consists atleast 3 sheets 1) Instructions 2) Information 3) Entity data sheet(1 or more). Entity data sheets contain data related to the Entity, Child Entities, Tables part of the Entity and/or Child Entity.\n" +
					"\n" +
					"4. \"Information\" sheet captured details of the template e.g. Download date, Downloaded by etc.\n" +
					"\n" +
					"5. Content of Entity data sheets is dynamic and depends on the configuration set by admin.\n" +
					"\n" +
					"6. Entity data sheet has following initial columns 1) Header (1st row) 2) Instructions(5th row) 3) Reference Data(6th row). User should populate data from 7th row ownwards.\n" +
					"\n" +
					"7. Header: Contains name of metadata.\n" +
					"8. Instruction: Captures information related to metadata validation e.g. limit restrictions, mandatory or not etc.\n" +
					"\n" +
					"9. Reference Data: Contains information related to master data available against the metadata e.g. master list of functions & services.\n" +
					"\n"};

			for (int i = 0; i < allInstructionValues.size(); i++) {
				if (!allInstructionValues.get(i).trim().equalsIgnoreCase(expectedValues[i].trim())) {
					csAssert.assertTrue(false, "Expected Instruction Value at Row #" + (i + 1) + ": [" + expectedValues[i].trim() +
							"] and Actual Value found is: " + allInstructionValues.get(i).trim());
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Instructions Sheet of Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	//TC-100120, TC-100121, TC-100122, TC-100137
	@Test(priority = 7)
	public void validateInformationSheet() {
		CustomAssert csAssert = new CustomAssert();

		try {
			String templateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "download template validation",
					"templateFileName");
			logger.info("Validating Information Sheet of Invoice Bulk Create Template located at [" + bulkCreateTemplateFilePath + "/" + templateFileName + "]");

			if (!new File(bulkCreateTemplateFilePath + "/" + templateFileName).exists()) {
				throw new SkipException("Couldn't find Bulk Create Template file at Location [" + bulkCreateTemplateFilePath + "/" + templateFileName +
						"]. Hence skipping test");
			}

			List<String> allInformationKeys = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(bulkCreateTemplateFilePath, templateFileName,
					"Information", 0, 0, 8);

			if (allInformationKeys.isEmpty()) {
				throw new SkipException("Couldn't get all Keys from Information Sheet of Bulk Create Template. Hence skipping test.");
			}

			String[] expectedKeys = {"Bulk Create - Invoice", "", "Downloaded By", "Download date & time", "Parent entity", "",
					"CONFIDENTIALITY AND DISCLAIMER\r\n" +
							"The information in this document is proprietary and confidential and is provided upon the recipient's promise to keep such information " +
							"confidential. In no event may this information be supplied to third parties without <Client's Name>'s prior written consent.\r\n" +
							"The following notice shall be reproduced on any copies permitted to be made:\r\n" +
							"<Client's Name> Confidential & Proprietary. All rights reserved.",""};

			for (int i = 0; i < allInformationKeys.size(); i++) {
				if (!allInformationKeys.get(i).trim().contains(expectedKeys[i].trim())) {
					csAssert.assertTrue(false, "Expected Information Key at Row #" + (i + 1) + ": [" + expectedKeys[i].trim() +
							"] and Actual Key found is: " + allInformationKeys.get(i).trim());
				}
			}

			List<String> allInformationValues = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(bulkCreateTemplateFilePath, templateFileName,
					"Information", 1, 0, 7);
			if (allInformationValues.isEmpty()) {
				throw new SkipException("Couldn't get all Values from Information Sheet of Bulk Create Template. Hence skipping test.");
			}

			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "information sheet validation");

			String expectedDownloadedBy = properties.get("expecteddownloadedby").trim();
			String expectedSupplier = properties.get("expectedsupplier").trim();
			String expectedParentEntity = properties.get("expectedparententity").trim();

			if (!allInformationValues.get(2).trim().equalsIgnoreCase(expectedDownloadedBy)) {
				csAssert.assertTrue(false, "Expected Downloaded By: " + expectedDownloadedBy + " and Actual Downloaded By: " +
						allInformationValues.get(2));
			}

			//due to multi supplier template change
//			if (!allInformationValues.get(4).trim().equalsIgnoreCase(expectedSupplier)) {
//				csAssert.assertTrue(false, "Expected Supplier: " + expectedSupplier + " and Actual Supplier: " + allInformationValues.get(4));
//			}

			if (!allInformationValues.get(4).trim().equalsIgnoreCase(expectedParentEntity)) {
				csAssert.assertTrue(false, "Expected Parent Entity: " + expectedParentEntity + " and Actual Parent Entity: " +
						allInformationValues.get(4));
			}

			//Deleting Bulk Create Template
			new File(bulkCreateTemplateFilePath + "/" + templateFileName).delete();
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Information Sheet of Invoice Bulk Create Template. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForBulkCreateTemplateUpload() throws ConfigurationException {
		logger.info("Setting all Bulk Create Upload Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Invoice Bulk Create Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForBulkCreateTemplateUpload", priority = 8)
	public void testBulkCreateUpload(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Invoice Bulk Create Template Upload Flow [{}]", flowToTest);
			logger.info("Uploading Invoice Bulk Create Template for Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

			String bulkCreateTemplateFileName = properties.get("bulkcreatetemplatefilename");

			if (!(new File(bulkCreateTemplateFilePath + "/" + bulkCreateTemplateFileName).exists())) {
				throw new SkipException("Couldn't find Bulk Create Template File at Location: " + bulkCreateTemplateFilePath + "/" + bulkCreateTemplateFileName);
			}

			int contractId = Integer.parseInt(properties.get("contractid"));

			String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, bulkCreateTemplateFileName, contractEntityTypeId, contractId,
					invoiceEntityTypeId, bulkCreateTemplateId);
			String expectedMessage = properties.get("expectedmessage").trim();

			logger.info("Actual Bulk Create Template Upload API Response: {} and Expected Result: {}", uploadResponse, expectedMessage);
			if (uploadResponse == null || !uploadResponse.trim().toLowerCase().contains(expectedMessage.toLowerCase())) {
					csAssert.assertTrue(false, "Bulk Create Template Upload Response doesn't match with Expected Response for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Bulk Create Template Upload for Flow [" + flowToTest +
					"]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForBulkCreateTemplateProcessing() throws ConfigurationException {
		logger.info("Setting all Bulk Create Template Processing Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "processingFlowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Invoice Bulk Create Template Bulk Upload Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForBulkCreateTemplateProcessing", priority = 9)
	public void testBulkCreateTemplateProcessing(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Invoice Bulk Create Template Processing Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);
			String templateFileName = properties.get("bulkcreatetemplatefilename");
			UserTasksHelper.removeAllTasks();

			logger.info("Uploading Bulk Create Template for Flow [{}]", flowToTest);
			int contractId = Integer.parseInt(properties.get("contractid"));

			logger.info("Hitting ListRendererListData API for Invoice and Contract Id {} to check if any invoice already exists.");
			ListRendererListData listDataObj = new ListRendererListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":" + invoiceEntityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			Map<String, String> params = new HashMap<>();
			params.put("contractId", String.valueOf(contractId));
			listDataObj.hitListRendererListData(invoiceListId, payload, params);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				int initialInvoiceCount = ListDataHelper.getFilteredListDataCount(listDataResponse);

				String uploadResponse = BulkTemplate.uploadBulkCreateTemplate(bulkCreateTemplateFilePath, templateFileName, contractEntityTypeId, contractId,
						invoiceEntityTypeId, bulkCreateTemplateId);

				if (uploadResponse != null && uploadResponse.trim().contains("Your request has been successfully submitted")) {
					Fetch fetchObj = new Fetch();
					fetchObj.hitFetch();
					String fetchResponse = fetchObj.getFetchJsonStr();

					if (ParseJsonResponse.validJsonResponse(fetchResponse)) {
						int newTaskId = UserTasksHelper.getNewTaskId(fetchResponse, null);

						if (newTaskId == -1) {
							throw new SkipException("Couldn't get Task Id for Bulk Create Template Scheduler Job for Flow [" + flowToTest + "]");
						}

						Map<String, String> schedulerJob = UserTasksHelper.waitForScheduler(schedulerJobTimeOut, schedulerJobPollingTime, newTaskId);
						String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
						logger.info("Expected Result of Bulk Create Template Processing for Flow [{}] is {}", flowToTest, expectedResult);
						String jobStatus = schedulerJob.get("jobPassed").trim();

						if (jobStatus.equalsIgnoreCase("skip")) {
							throw new SkipException(schedulerJob.get("errorMessage") + ". Hence skipping test.");
						}

						if (expectedResult.equalsIgnoreCase("success")) {
							if (jobStatus.equalsIgnoreCase("false")) {
								csAssert.assertTrue(false, "Bulk Create Template Processing failed for Flow [" + flowToTest +
										"] whereas it was expected to process successfully");
							} else {
								logger.info("Bulk Create Template Processed successfully for Flow [" + flowToTest + "]");
								logger.info("Hitting ListRendererListData API for Invoice and Contract Id {} to check if New Invoice Created.", contractId);
								listDataObj.hitListRendererListData(invoiceListId, payload, params);
								listDataResponse = listDataObj.getListDataJsonStr();

								if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
									if (ListDataHelper.getFilteredListDataCount(listDataResponse) <= initialInvoiceCount) {
										csAssert.assertTrue(false, "Invoice not created whereas the Processing Flow [" + flowToTest + "] passed.");
									} else {
										if (properties.containsKey("createlineitem") && properties.get("createlineitem").trim().equalsIgnoreCase("true")) {
											//Check if Line Item is created.
											List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(listDataResponse);

											if (listData.isEmpty()) {
												deleteInvoiceOfContract(flowToTest, contractId, csAssert);
												throw new SkipException("Couldn't get ListData from ListData API Response for Invoice and Contract Id " + contractId);
											}

											int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
											if (idColumnNo == -1) {
												deleteInvoiceOfContract(flowToTest, contractId, csAssert);
												throw new SkipException("Couldn't get Column No for Id from ListData API Response for Invoice and Contract Id " + contractId);
											}

											int newInvoiceId = Integer.parseInt(listData.get(0).get(idColumnNo).get("valueId"));
											logger.info("Hitting TabListData API for Invoice {}", newInvoiceId);

											TabListData tabListObj = new TabListData();
											int tabId = TabListDataHelper.getIdForTab("invoice details");

											if (tabId == -1) {
												deleteInvoiceOfContract(flowToTest, contractId, csAssert);
												throw new SkipException("Couldn't get Id for Tab Invoice Details.");
											}

											String tabListDataResponse = tabListObj.hitTabListData(tabId, invoiceEntityTypeId, newInvoiceId);

											if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
												JSONObject jsonObj = new JSONObject(tabListDataResponse);

												if (jsonObj.getJSONArray("data").length() == 0) {
													csAssert.assertTrue(false, "Invoice Created Successfully but Line Item not created.");
												}
											} else {
												csAssert.assertTrue(false, "TabListData API Response for Invoice Id " + newInvoiceId +
														" and Details Tab is an Invalid JSON.");
											}
										}
										deleteInvoiceOfContract(flowToTest, contractId, csAssert);
									}
								} else {
									csAssert.assertTrue(false, "ListData API Response for Invoice and Contract Id " + contractId +
											" is an Invalid JSON. (After Invoice Processing Flow [" + flowToTest + "] passed.)");
								}
							}
						} else {
							if (jobStatus.equalsIgnoreCase("true")) {
								csAssert.assertTrue(false, "Bulk Create Template Processed successfully for Flow [" + flowToTest +
										"] whereas it was expected to fail.");
								deleteInvoiceOfContract(flowToTest, contractId, csAssert);
							} else {
								logger.info("Bulk Create Template Processing failed for Flow [" + flowToTest + "]");
								logger.info("Hitting ListRendererListData API for Invoice and Contract Id {} to check if no New Invoice is Created.", contractId);
								listDataObj.hitListRendererListData(invoiceListId, payload, params);
								listDataResponse = listDataObj.getListDataJsonStr();

								if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
									if (ListDataHelper.getFilteredListDataCount(listDataResponse) > initialInvoiceCount) {
										csAssert.assertTrue(false, "Invoice created whereas the Processing Flow [" + flowToTest + "] failed.");
										deleteInvoiceOfContract(flowToTest, contractId, csAssert);
									}
								} else {
									csAssert.assertTrue(false, "ListData API Response for Invoice and Contract Id " + contractId +
											" is an Invalid JSON. (After Invoice Processing Flow [" + flowToTest + "] passed.)");
								}
							}
						}
					} else {
						csAssert.assertTrue(false, "Fetch API Response for Flow [" + flowToTest + "] is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't upload Bulk Create Template Successfully for Flow [" + flowToTest + "]");
				}
			} else {
				csAssert.assertTrue(false, "List Data API Response for Invoice and Contract Id " + contractId + " is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Invoice Bulk Create Template Processing for Flow [" + flowToTest + "]. " +
					e.getMessage());
		}
		csAssert.assertAll();
	}

	private void deleteInvoiceOfContract(String flowToTest, int contractId, CustomAssert csAssert) {
		try {
			logger.info("Hitting ListRendererListData API for Invoice to delete Invoice for Contract Id {}", contractId);
			ListRendererListData listDataObj = new ListRendererListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":" + invoiceEntityTypeId + ",\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\"," +
					"\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
			Map<String, String> params = new HashMap<>();
			params.put("contractId", String.valueOf(contractId));

			listDataObj.hitListRendererListData(invoiceListId, payload, params);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(listDataResponse);

				if (listData.size() > 0) {
					int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");

					if (idColumnNo != -1) {
						for (Map<Integer, Map<String, String>> oneListData : listData) {
							int serviceDataId = Integer.parseInt(oneListData.get(idColumnNo).get("valueId"));
							logger.info("Deleting Invoice Id {}", serviceDataId);
							EntityOperationsHelper.deleteEntityRecord("invoices", serviceDataId);
						}
					} else {
						throw new SkipException("Couldn't get Column No for Id and Flow [" + flowToTest + "]");
					}
				} else {
					throw new SkipException("Couldn't get List Data from ListDataAPI Response for Flow [" + flowToTest + "]");
				}
			} else {
				csAssert.assertTrue(false, "List Data API Response for Invoice is an Invalid JSON.");
			}
		} catch (Exception e) {
			throw new SkipException("Exception while deleting Invoice(s) of Contract Id " + contractId + ". [" + e.getMessage() + "]");
		}
	}
}