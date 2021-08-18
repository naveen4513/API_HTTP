/*
package com.sirionlabs.test.invoiceOld;

import com.sirionlabs.api.invoice.InvoicePricingDataUpload;
import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.api.invoice.InvoiceTabListData;
import com.sirionlabs.api.servicedata.ServiceDataContractsShowPage;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.test.servicedata.ServiceDataOperations;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

*/
/**
 * @author manoj.upreti
 * @implNote <b>This class is implementing the helping methods used to test the Invoice.xml</b>
 *//*

public class InvoiceDataOperations {
	private final static Logger logger = LoggerFactory.getLogger(InvoiceDataOperations.class);

	*/
/**
	 * @param parentEntityId this is the ID of subcontract
	 * @return Map<String   ,       String> Change Requests Map if the values are proper otherwise returns null
	 * @throws Exception
	 * @apiNote this method is for creating the CR map for subcontract ID provided in the parameter
	 *//*

	public Map<String, String> getServiceDataCCRMap(String parentEntityId) throws Exception {
		logger.debug("Getting the Service Data Change Request ID List for subcontract ID : [ {} ]", parentEntityId);
		ServiceDataContractsShowPage oServiceDataContractsShowPage = new ServiceDataContractsShowPage();
		oServiceDataContractsShowPage.hitServiceDataContractShowPage(parentEntityId);
		String serviceDataContractShowPageResult = oServiceDataContractsShowPage.getServiceDataContractShowPageResponseStr();
		if (!APIUtils.validJsonResponse(serviceDataContractShowPageResult)) {
			logger.error("The Service Data Contract : [ {} ] show page is not a valid Json", parentEntityId);
			return null;
		}
		JSONObject jsonObjectTillContractShowPage = new JSONObject(serviceDataContractShowPageResult);
		JSONObject jsonObjectTillServiceDataCCROptions = jsonObjectTillContractShowPage.getJSONObject("body").getJSONObject("data").getJSONObject("serviceDataCCR").getJSONObject("options");

		JSONArray jsonObjectTillServiceDataCCROptionsDataArray = jsonObjectTillServiceDataCCROptions.getJSONArray("data");
		int CROptionsDataArrayLength = jsonObjectTillServiceDataCCROptionsDataArray.length();

		if (CROptionsDataArrayLength == 0) {
			logger.debug("The CR options Array length is 0 for sub contract : [ {} ]", parentEntityId);
			return null;
		}

		Map<String, String> crOptionsMap = new HashMap<>();
		for (int count = 0; count < CROptionsDataArrayLength; count++) {
			JSONObject jsonObjectcCCROptions = jsonObjectTillServiceDataCCROptionsDataArray.getJSONObject(count);
			JSONUtility jsonUtilityObjTillCROption = new JSONUtility(jsonObjectcCCROptions);
			String crID = jsonUtilityObjTillCROption.getStringJsonValue("id");
			String crName = jsonUtilityObjTillCROption.getStringJsonValue("name");
			crOptionsMap.put(crID, crName);
		}
		logger.debug("The CR options Map : [ {} ] , is formed for sub contract : [ {} ]", crOptionsMap, parentEntityId);
		return crOptionsMap;
	}


	*/
/**
	 * @param oXLSUtils                 is the Object of {@link XLSUtils} class
	 * @param jsonObjectTillInvoiceType is the {@link JSONObject} till Invoice.xml type from the pricing Data Json file
	 * @return true if the XLS sheet is updated successfully otherwise returns false
	 * @throws IOException
	 * @apiNote <b>This method is to update the Pricing Data XLS file according to the input JSON file</b>
	 *//*

	public boolean updateXlsFileFor_Invoice(XLSUtils oXLSUtils, JSONObject jsonObjectTillInvoiceType) throws Exception {
		logger.debug("Started Editing the Pricing Data XLS file , according to the JSON file : [ {} ] for Invoice.xml", jsonObjectTillInvoiceType.toString());
		boolean writeResult = true;

		JSONUtility jsonUtilityObjTillTemplateInfo = new JSONUtility(jsonObjectTillInvoiceType.getJSONObject("templateInfo"));
		String sheetNameToBeEdited = jsonUtilityObjTillTemplateInfo.getStringJsonValue("dataSheetName");
		int startRow = jsonUtilityObjTillTemplateInfo.getIntegerJsonValue("startRow");
		int numberOfRowsToBeEdited = jsonUtilityObjTillTemplateInfo.getIntegerJsonValue("numberOfRowsToBeEdited");

		logger.debug("The Column List in the JSON is : [ {} ]", jsonObjectTillInvoiceType);
		logger.debug("The number of rows to be created is : [ {} ]", numberOfRowsToBeEdited);
		*/
/*for (int rowNumber = startRow; rowNumber < startRow + numberOfRowsToBeEdited; rowNumber++) {
			Map<Integer, Object> rowDataMap = generateRowDataMap(jsonObjectTillInvoiceType);
			boolean rowResult = oXLSUtils.editRowData(sheetNameToBeEdited, rowNumber, rowDataMap);
			if (!rowResult) {
				writeResult = false;
			}
		}*//*

		return writeResult;
	}


	*/
/**
	 * @param jsonObjectTillInvoiceType is the Json Object till Invoice.xml Type from the Config JSON file
	 * @return Map<Integer   ,       Object> which contains column ID as Key , and Object as Value
	 * @apiNote <b>This method is to create the Map for a row , whill be used to edit the Pricing List XML file</b>
	 *//*

	private Map<Integer, Object> generateRowDataMap(JSONObject jsonObjectTillInvoiceType) {
		logger.debug("Started Generating the Map for row using JSON  : [ {} ]", jsonObjectTillInvoiceType);
		Map<Integer, Object> rowDataMap = new HashMap<>();
		JSONArray columnNamesArray = jsonObjectTillInvoiceType.names();
		int length = columnNamesArray.length();
		for (int index = 0; index < length; index++) {
			String columnNumber = columnNamesArray.getString(index);
			if (!columnNumber.equalsIgnoreCase("templateInfo")) {
				JSONObject jsonObjectTillColumnID = jsonObjectTillInvoiceType.getJSONObject(columnNumber);
				JSONUtility jsonUtilityObjTillColumnID = new JSONUtility(jsonObjectTillColumnID);
				String nameOfCell = jsonUtilityObjTillColumnID.getStringJsonValue("name");
				String typeOfData = jsonUtilityObjTillColumnID.getStringJsonValue("type");
				JSONArray optionsJsonArray = jsonUtilityObjTillColumnID.getArrayJsonValue("options");

				if (typeOfData.equalsIgnoreCase("singleselect")) {
					if (nameOfCell.equalsIgnoreCase("Type")) {
						String randomData = optionsJsonArray.getString(ThreadLocalRandom.current().nextInt(0, optionsJsonArray.length()));
						rowDataMap.put(Integer.parseInt(columnNumber), randomData);
					} else {
						String randomData = optionsJsonArray.getString(ThreadLocalRandom.current().nextInt(0, optionsJsonArray.length()));
						rowDataMap.put(Integer.parseInt(columnNumber), Integer.parseInt(randomData));
					}
				} else {
					logger.error("The type of data is not available for column : [{}] , type : [ {} ]", columnNumber, typeOfData);
				}
			}
		}
		logger.debug("Generation of the Map is completed for row, using JSON Array : [ {} ] is completed.", columnNamesArray);
		return rowDataMap;
	}


	*/
/**
	 * @param oServiceDataOperations is an Object of {@link ServiceDataOperations} class
	 * @param parentEntityTypeId     is the ID of Contract entity
	 * @param parentEntityId         is ID of the sub contract to be edited
	 * @param entityTypeId           is the ID if Service Data Entity
	 * @param sXLSOutputFile         is the XLS file which need to be downloaded , modified and upload for creating service Data
	 * @param csAssertion          is an Object of {@link CustomAssert}
	 * @return true if all download template , edit , upload operations are completed successfully otherwise returns false
	 * @apiNote <b>This method is to create the Service Data</b>
	 *//*

	public boolean createServiceData(ServiceDataOperations oServiceDataOperations, String parentEntityTypeId, String parentEntityId, String entityTypeId, String sXLSOutputFile, CustomAssert csAssertion) {
		logger.info("Current Test Is running Against entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
		boolean serviceDataCreationResult = true;
		//call test for Download XLS Template
		boolean downloadResult = oServiceDataOperations.testBulkActionsDownloadTemplateXLSFile(parentEntityTypeId, parentEntityId, sXLSOutputFile, csAssertion);
		if (!downloadResult) {
			csAssertion.assertTrue(false, "Failed to Download XLS Template for entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] , parentEntityId : [ " + parentEntityId + " ] , and download File Location  : [ " + sXLSOutputFile + " ]\n\n");
			serviceDataCreationResult = false;
		}

		//call test for Edit XLS Template
		boolean editResult = oServiceDataOperations.testBulkActionsEditXLSFile(parentEntityTypeId, parentEntityId, sXLSOutputFile, csAssertion);
		if (!editResult) {
			csAssertion.assertTrue(false, "Write updates is failed to the XLS [ " + sXLSOutputFile + " ] , for entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] , parentEntityId : [ " + parentEntityId + " ] \n\n");
			serviceDataCreationResult = false;
		}

		//call test for Upload XLS Template
		boolean uploadResult = oServiceDataOperations.testBulkActionsUploadXLSFile(entityTypeId, parentEntityTypeId, parentEntityId, sXLSOutputFile, csAssertion);
		if (!uploadResult) {
			csAssertion.assertTrue(false, "The Upload is failed for Uploading of Service Data for entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ], parentEntityId : [ " + parentEntityId + " ], file to Upload : [ " + sXLSOutputFile + " ] \n\n");
			serviceDataCreationResult = false;
		}

		//call the Blocking Message check Method
		boolean blockingMessageResult = oServiceDataOperations.testServiceDataShowPageVerificationSoonAfterUploading(parentEntityId, csAssertion);
		if (!blockingMessageResult) {
			csAssertion.assertTrue(false, "The Blocking message verification failed , for Uploading of Service Data of entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ], parentEntityId : [ " + parentEntityId + " ], file to Upload : [ " + sXLSOutputFile + " ] \n\n");
			serviceDataCreationResult = false;
		}
		return serviceDataCreationResult;
	}

	*/
/**
	 * @param tabSpecificId                          is the id of Tab , getting from config file
	 * @param invoiceServiceDataVerificationJsonFile is the service data result verification json file, which is being generated at the time of service data template XLS modification
	 * @param entityTypeId                           is the ID if Service Data Entity
	 * @param parentEntityTypeId                     is the ID of Contract entity
	 * @param parentEntityId                         is ID of the sub contract to be edited
	 * @return List<String> rRecordIDList
	 * @throws Exception
	 * @apiNote <b>This method is for generating the record IDs list of service data created for invoice type</b>
	 *//*

	public List<String> getRecordIDList(String tabSpecificId, String invoiceServiceDataVerificationJsonFile, String entityTypeId, String parentEntityTypeId, String parentEntityId) throws Exception {
		logger.debug("The Tab Specific ID for Service Data tab is : [ {} ]", tabSpecificId);
		List<String> rRecordIDList = new ArrayList<>();
		TabListData oTabListData = new TabListData();
		oTabListData.hitTabListData(tabSpecificId, true, entityTypeId, parentEntityTypeId, parentEntityId);
		String serviceDataTabResponce = oTabListData.getTabListDataResponseStr();
		ServiceDataOperations oServiceDataOperations = new ServiceDataOperations();
		JSONObject jsonObjectInvoiceServiceDataVerificationJsonFile = oServiceDataOperations.getDataJsonObjForVerification(invoiceServiceDataVerificationJsonFile);
		JSONArray invoiceServiceDataVerificationJsonArray = jsonObjectInvoiceServiceDataVerificationJsonFile.names();
		int jsonArrayLenght = invoiceServiceDataVerificationJsonArray.length();
		for (int linecount = 0; linecount < jsonArrayLenght; linecount++) {
			String lineName = invoiceServiceDataVerificationJsonArray.getString(linecount);
			JSONObject lineJsonObjectInvoiceServiceDataVerification = jsonObjectInvoiceServiceDataVerificationJsonFile.getJSONObject(lineName);
			String serviceClientName = lineJsonObjectInvoiceServiceDataVerification.getString("serviceclient");

			String recordId = oServiceDataOperations.getRecordIDFromServiceDataTabAPIResponse(serviceDataTabResponce, serviceClientName);
			if (!recordId.equalsIgnoreCase("")) {
				rRecordIDList.add(recordId);
			} else {
				logger.error("The ID is returned as empty string for invoiceServiceDataVerificationJsonFile : [ {} ] , entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ]", invoiceServiceDataVerificationJsonFile, entityTypeId, parentEntityTypeId, parentEntityId);
			}

		}
		return rRecordIDList;
	}

	*/
/**
	 * @param sXLSPricingdataFile is the XLS file location where the Template to be downloaded
	 * @param parentEntityTypeId  is the ID of Contract entity
	 * @param parentEntityId      is ID of the sub contract to be edited
	 * @param entityTypeId        is the ID if Service Data Entity
	 * @param entityIDList        is the recocrd IDs list , which are to be extracted from tablistdata Api response
	 * @return true if template is downloaded without exception otherwise returns false
	 * @apiNote <b>This method is for downloading the pricing data template for an invoice type</b>
	 *//*

	public boolean downloadPricingDataTemplate(String sXLSPricingdataFile, String parentEntityTypeId, String parentEntityId, String entityTypeId, List<String> entityIDList) {
		logger.debug("Starting pricing Data Template Download for sXLSPricingdataFile : [ {} ] , entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ],  entityIDList : [ {} ]", sXLSPricingdataFile, entityTypeId, parentEntityTypeId, parentEntityId, entityIDList);
		InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
		try {
			invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId, entityIDList);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Pricing Data Template Download failed for sXLSPricingdataFile : [ {} ] , entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ],  entityIDList : [ {} ], Cause : [ {} ], Exception : [ {} ]", sXLSPricingdataFile, entityTypeId, parentEntityTypeId, parentEntityId, entityIDList, e.getMessage(), e.getStackTrace());
			return false;
		}

		return true;
	}

	*/
/**
	 * @param sXLSPricingdataFile is the XLS file to be uploaded
	 * @param parentEntityTypeId  is the ID of Contract entity
	 * @param parentEntityId      is ID of the sub contract to be edited
	 * @param entityTypeId        is the ID if Service Data Entity
	 * @return upload response string
	 * @apiNote <b>This method is for uploading the pricing data for an invoice type</b>
	 *//*

	public String uploadPricingDataXlsFile(String sXLSPricingdataFile, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.debug("Starting pricing Data upload for sXLSPricingdataFile : [ {} ] , entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ].", sXLSPricingdataFile, entityTypeId, parentEntityTypeId, parentEntityId);
		InvoicePricingDataUpload invoicePricingDataUpload = new InvoicePricingDataUpload();

		try {
			Map<String, String> CRMap = getServiceDataCCRMap(parentEntityId);
			logger.debug("The CR Map is  : [ {} ]", CRMap);
			//get Randon CR number
			Random random = new Random();
			List<String> keys = new ArrayList<String>(CRMap.keySet());
			String serviceDataCCR = keys.get(random.nextInt(keys.size()));
			//String serviceDataCCR = CRMap.get(randomKey);

			String uploadedResponse = invoicePricingDataUpload.uploadPricingData(parentEntityTypeId, parentEntityId, entityTypeId, sXLSPricingdataFile, serviceDataCCR);
			logger.info("The upload Response is  : [ {} ]", uploadedResponse);
			return uploadedResponse;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Pricing Data xls upload failed for sXLSPricingdataFile : [ {} ] , entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ], Cause : [ {} ], Exception : [ {} ]", sXLSPricingdataFile, entityTypeId, parentEntityTypeId, parentEntityId, e.getMessage(), e.getStackTrace());
			return null;
		}
	}


	*/
/**
	 * @param oServiceDataOperations is an Object of {@link ServiceDataOperations} class
	 * @param parentEntityTypeId     is the ID of Contract entity
	 * @param parentEntityId         is ID of the sub contract to be edited
	 * @param entityTypeId           is the ID if Service Data Entity
	 * @param csAssertion          is an Object of {@link CustomAssert}
	 * @return true if all service data tab and audit lof tab is verified successfully otherwise returns false
	 * @apiNote <b>This method is to Verify the Service Data</b>
	 *//*

	public boolean verifyServiceData(ServiceDataOperations oServiceDataOperations, String parentEntityTypeId, String parentEntityId, String entityTypeId, CustomAssert csAssertion) {
		boolean serviceDataverificationResult = true;
		//call test for Download XLS Template
		logger.info("The run configuration is mentioned as verification so Only verification will be started.");
		logger.info("Current Upload Result Is running Against entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);

		boolean verificationSetviceDataTabResult = oServiceDataOperations.testServiceDataTabVerification(entityTypeId, parentEntityTypeId, parentEntityId, csAssertion);
		if (verificationSetviceDataTabResult) {
			logger.info("Service Data Tab Verification is successful for entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
		} else {
			logger.error("Service Data Tab Verification is failed for entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
			serviceDataverificationResult = false;
			csAssertion.assertTrue(false, "Service Data Tab Verification is failed for entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] , parentEntityId : [ " + parentEntityId + " ] \n\n");
		}

		boolean verificationAuditLogTabResult = oServiceDataOperations.testAuditLogTabVerification(entityTypeId, parentEntityTypeId, parentEntityId, csAssertion);
		if (verificationAuditLogTabResult) {
			logger.info("Audit Log Tab Verification is successful for entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
		} else {
			logger.error("Audit Log Tab Verification is failed for entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
			serviceDataverificationResult = false;
			csAssertion.assertTrue(false, "Audit Log Tab Verification is failed for entityTypeId : [ " + entityTypeId + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] , parentEntityId : [ " + parentEntityId + " ] \n\n");
		}
		return serviceDataverificationResult;
	}

	*/
/**
	 * @param oServiceDataOperations is an Object of {@link ServiceDataOperations} class
	 * @param parentEntityId         is ID of the sub contract to be edited
	 * @param csAssertion          is an Object of {@link CustomAssert}
	 * @return true if the blocking message is available on the showPage otherwise returns false
	 * @apiNote <b>This method is to get Show page Blocking error message</b>
	 *//*

	public boolean verifyContractShowPageBlockedMessage(ServiceDataOperations oServiceDataOperations, String parentEntityId, CustomAssert csAssertion) {
		//call the Blocking Message check Method
		return oServiceDataOperations.testServiceDataShowPageVerificationSoonAfterUploading(parentEntityId, csAssertion);
	}


	*/
/**
	 * <b>This method is for verifying the Charges Tab in Invoice.xml tablist data</b>
	 *
	 * @param entityTypeId              is the ID if Service Data Entity
	 * @param serviceDataRecordId       is the id of service data record
	 * @param jsonObjectTillInvoiceType is the {@link JSONObject} till Invoice.xml type from the pricing Data Json file
	 * @return true if number of records , pricing data volume , rate are matching , otherwise return false
	 * @throws Exception
	 *//*

	public boolean verifyChargesTab(String entityTypeId, String serviceDataRecordId, JSONObject jsonObjectTillInvoiceType) throws Exception {
		logger.debug("Started verifying Charges Tab according to the JSON file : [ {} ] for Invoice.xml record ID : [ {} ]", jsonObjectTillInvoiceType.toString(), serviceDataRecordId);
		boolean chargesTabVerificationResult = true;
		int numberOfRowsToBeEdited = -1;
		int pricingDataVolume = -1;
		int pricingDataRate = -1;
		//Get the Volume and Rate from the Invoice.xml Privcing data Json file
		//in this method we are asuming that we will test only for a single data and always take the 0th index of option Array
		JSONArray namesJsonArrayInInvoicePricingDataJson = jsonObjectTillInvoiceType.names();
		for (int nameCount = 0; nameCount < namesJsonArrayInInvoicePricingDataJson.length(); nameCount++) {
			JSONObject jsonObjectTillName = jsonObjectTillInvoiceType.getJSONObject(namesJsonArrayInInvoicePricingDataJson.getString(nameCount));
			if (namesJsonArrayInInvoicePricingDataJson.getString(nameCount).equalsIgnoreCase("templateInfo")) {
				numberOfRowsToBeEdited = jsonObjectTillName.getInt("numberOfRowsToBeEdited");
			}
			if (jsonObjectTillName.toString().contains("Volume")) {
				pricingDataVolume = jsonObjectTillName.getJSONArray("options").getInt(0);
			}
			if (jsonObjectTillName.toString().contains("Rate")) {
				pricingDataRate = jsonObjectTillName.getJSONArray("options").getInt(0);
			}
		}

		logger.debug("Hit TabList data fot Charges Tab and get Response");
		InvoiceTabListData oInvoiceTabListData = new InvoiceTabListData();
		oInvoiceTabListData.hitInvoiceTablistDataAPI(entityTypeId, serviceDataRecordId);
		String chargesTabListDataResponseJson = oInvoiceTabListData.getTablistDataResponseJsonStr();

		//Get data JSONArray from tablist data response
		JSONArray jsonArrayTillDataInTablistDataResponse = new JSONObject(chargesTabListDataResponseJson).getJSONArray("data");
		int jsonArrayLength = jsonArrayTillDataInTablistDataResponse.length();

		// Verify Number of Records
		if (jsonArrayLength != numberOfRowsToBeEdited) {
			logger.error("The number of Records are not matching");
			chargesTabVerificationResult = false;
		}


		for (int count = 0; count < jsonArrayTillDataInTablistDataResponse.length(); count++) {
			JSONObject jsonObjectTillRowInTablistDataResponse = jsonArrayTillDataInTablistDataResponse.getJSONObject(count);

			JSONArray jsonArrayOfNamesInTablistDataResponse = jsonObjectTillRowInTablistDataResponse.names();
			for (int nameCounet = 0; nameCounet < jsonArrayOfNamesInTablistDataResponse.length(); nameCounet++) {
				String columnNameString = jsonArrayOfNamesInTablistDataResponse.getString(nameCounet);
				JSONObject jsonObjectTillCloumnNameInTablistDataResponse = jsonObjectTillRowInTablistDataResponse.getJSONObject(columnNameString);

				String columnName = jsonObjectTillCloumnNameInTablistDataResponse.getString("columnName");

				if (columnName.equalsIgnoreCase("volume")) {
					String columnValue = jsonObjectTillCloumnNameInTablistDataResponse.getString("value");
					double priceDataDouble = Double.parseDouble(columnValue);
					int priceDta = (int) priceDataDouble;
					if (priceDta != pricingDataVolume) {
						logger.error("The pricing data Volume is not matching , Actual Value : [ {} ], required Value : [ {} ]", priceDta, pricingDataVolume);
						chargesTabVerificationResult = false;
					}
				}

				if (columnName.equalsIgnoreCase("unitrate")) {
					String columnValue = jsonObjectTillCloumnNameInTablistDataResponse.getString("value");
					double priceDataDouble = Double.parseDouble(columnValue);
					int priceDta = (int) priceDataDouble;
					if (priceDta != pricingDataRate) {
						logger.error("The pricing data Rate is not matching , Actual Value : [ {} ], required Value : [ {} ]", priceDta, pricingDataRate);
						chargesTabVerificationResult = false;
					}
				}

			}
		}


		return chargesTabVerificationResult;
	}
}
*/
