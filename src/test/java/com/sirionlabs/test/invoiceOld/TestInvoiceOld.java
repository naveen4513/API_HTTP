/*
package com.sirionlabs.test.invoiceOld;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.servicedata.ServiceDataOperations;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.IOException;
import java.util.*;


*/
/**
 * @author manoj.upreti
 *//*

public class TestInvoiceOld {
	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceOld.class);


	String InvoiceDataConfigFilePath = "";
	String InvoiceDataConfigFileName = "";
	String InvoicePricingDataJSONFileName = "";
	String ARC_RRC_Invoice_ServiceDataXLSFileName = "";
	String ARC_RRC_Invoice_ServiceData_Config_JSONFileName = "";
	String ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName = "";
	String Fixed_Fee_Invoice_ServiceDataXLSFileName = "";
	String Fixed_Fee_Invoice_ServiceData_Config_JSONFileName = "";
	String Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName = "";
	String Forecast_Invoice_ServiceDataXLSFileName = "";
	String Forecast_Invoice_ServiceData_Config_JSONFileName = "";
	String Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName = "";
	String ServiceDataResultVerification_Config = "";
	InvoiceDataOperations oInvoiceDataOperations;
	CustomAssert csAssertion;
	String ARC_RRC_Invoice_Pricing_DataXLSFileName = "";
	String Fixed_Fee_Invoice_Pricing_DataXLSFileName = "";
	String Forecast_Invoice_Pricing_DataXLSFileName = "";
	String pricingDataUploadRsultRequiredString = "";

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.debug("In Before Class method");
		initializeInvoiceConfig();
	}

	private void initializeInvoiceConfig() {
		InvoiceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceDataConfigFilePath");
		InvoiceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceDataConfigFileName");
		InvoicePricingDataJSONFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingDataJSONFileName");
		ARC_RRC_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_ServiceDataXLSFileName");
		ARC_RRC_Invoice_ServiceData_Config_JSONFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_ServiceData_Config_JSONFileName");
		ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName");
		Fixed_Fee_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_ServiceDataXLSFileName");
		Fixed_Fee_Invoice_ServiceData_Config_JSONFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_ServiceData_Config_JSONFileName");
		Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName");
		Forecast_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_ServiceDataXLSFileName");
		Forecast_Invoice_ServiceData_Config_JSONFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_ServiceData_Config_JSONFileName");
		Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName");

		ServiceDataResultVerification_Config = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataResultVerification_Config");
		oInvoiceDataOperations = new InvoiceDataOperations();
		csAssertion = new CustomAssert();

		ARC_RRC_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_Pricing_DataXLSFileName");
		Fixed_Fee_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_Pricing_DataXLSFileName");
		Forecast_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_Pricing_DataXLSFileName");

		pricingDataUploadRsultRequiredString = ConfigureConstantFields.getConstantFieldsProperty("InvoicePricingDataUploadResultString");
	}


	*/
/**
	 * @param c
	 * @return DataProvider Object[][]
	 * @throws ConfigurationException
	 * @apiNote <b>This is the data provider method which will return the Object 2D array , for each entity , it will contains the invoices types to be tested</b>
	 *//*

	@DataProvider(name = "InvoiceTestData", parallel = true)
	public Object[][] getInvoiceTestData(ITestContext c) throws ConfigurationException {
		logger.info("In the Data Provider for InvoiceTestData ");
		List<String> allEntitySection = ParseConfigFile.getAllSectionNames(InvoiceDataConfigFilePath, InvoiceDataConfigFileName);

		int i = 0;
		logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size());
		Object[][] groupArray = new Object[allEntitySection.size() - 1][];

		for (String entitySection : allEntitySection) {
			if (entitySection.equalsIgnoreCase("default")) {
				continue;
			}
			logger.debug("entitySection :{}", entitySection);
			Map<Integer, List<String>> dataMap = new HashMap<>();
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
			groupArray[i] = new Object[2];
			List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, entitySection);
			List<String> invoiceTypesList = new ArrayList<>();
			int dataentitynametobetestedID = -1;
			for (String entitySpecificProperty : allProperties) {
				if (entitySpecificProperty.contentEquals("dataentitynametobetested")) {
					String dataentitynametobetested = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, entitySection, entitySpecificProperty);
					logger.debug("entitySection :{} ,dataentitynametobetested :{}", entitySection, dataentitynametobetested);
					dataentitynametobetestedID = ConfigureConstantFields.getEntityIdByName(dataentitynametobetested);
				} else if (entitySpecificProperty.contentEquals("invoice_types")) {
					String invoiceTypesLine = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, entitySection, entitySpecificProperty);
					logger.debug("entitySection :{} ,invoiceTypesList :{}", entitySection, invoiceTypesLine);
					invoiceTypesList = Arrays.asList(invoiceTypesLine.split(","));
				}
			}
			dataMap.put(dataentitynametobetestedID, invoiceTypesList);
			groupArray[i][0] = entityTypeId;
			groupArray[i][1] = dataMap;
			i++;
		}
		logger.debug("Formed the Data Provider to be tested : [ {} ]", groupArray);
		return groupArray;
	}


	@Test(dataProvider = "InvoiceTestData")
	public void testReportRenderData(Integer parentEntityType, Map<Integer, List<String>> dataMap) {
		logger.debug("Current test is started for parentEntityTypeId (Contract Entity ID ): [ {} ] and dataMap is : [ {} ]", parentEntityType, dataMap);
		if (parentEntityType == 0) {
			logger.error("The Entity ID is not available , please check the configs");
		} else {
			for (Map.Entry<Integer, List<String>> entry : dataMap.entrySet()) {
				if (entry.getKey() == -1) {
					logger.error("The entityTypeId (service data) is not available , please check the config.... ");
				} else {
					for (String invoiceType : entry.getValue()) {
						String entityTypeId = String.valueOf(entry.getKey());
						String parentEntityTypeId = String.valueOf(parentEntityType);
						String parentEntityTypeName = ConfigureConstantFields.getEntityNameById(parentEntityType);

						List<String> contractShowPageIDsForInnvoice = new ArrayList<>();
						String resultVerificationJsonFileName = "";
						try {
							if (invoiceType.equalsIgnoreCase("arc_rrc")) {
								// Getting the show page IDs for a contract type
								String showpageIdsLineForInvoiceInConfig = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, parentEntityTypeName, "contract_ids_at_showpage_for_arc_rrc");
								contractShowPageIDsForInnvoice = Arrays.asList(showpageIdsLineForInvoiceInConfig.split(","));

							} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
								String showpageIdsLineForInvoiceInConfig = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, parentEntityTypeName, "contract_ids_at_showpage_for_fixed_fee");
								contractShowPageIDsForInnvoice = Arrays.asList(showpageIdsLineForInvoiceInConfig.split(","));
							} else if (invoiceType.equalsIgnoreCase("forecast")) {
								String showpageIdsLineForInvoiceInConfig = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, parentEntityTypeName, "contract_ids_at_showpage_for_forecast");
								contractShowPageIDsForInnvoice = Arrays.asList(showpageIdsLineForInvoiceInConfig.split(","));
							} else {
								logger.error("The Invoice.xml type is not matching in the config file , please check and rerun");
								Assert.fail("The Invoice.xml type is not matching in the config file , please check and rerun \n");
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.error("Got Error while getting contract show page ids for invoice type : [ {} ], cause : [ {} ], Exception : [ {} ]", invoiceType, e.getMessage(), e.getStackTrace());
						}


						if (contractShowPageIDsForInnvoice.size() == 0) {
							logger.error("The Contract show pages IDs are not available for the Invoice.xml Type : [ {} ]", invoiceType);
							csAssertion.assertTrue(false, "The Contract show pages IDs are not available for the Invoice.xml Type : [ " + invoiceType + " ] \n");
						} else {
							for (String showpageID : contractShowPageIDsForInnvoice) {
								String parentEntityId = showpageID;
								String runexecutionpart = "";
								try {
									runexecutionpart = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "runexecutionpart");
								} catch (ConfigurationException e) {
									e.printStackTrace();
								}
								if (runexecutionpart.equalsIgnoreCase("servicedataupload")) {
									logger.info("Current Test Is running Against entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);
									boolean servicedataCreation = createInvoiceServiceData(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The service Data Creation Result is : [ {} ]", servicedataCreation);
									if (!servicedataCreation) {
										csAssertion.assertTrue(false, "The service Data Creation is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}
								} else if (runexecutionpart.equalsIgnoreCase("pricingdataupload")) {
									logger.info("Current Test Is running Against entityTypeId : [ {} ], parentEntityTypeId : [ {} ],  parentEntityId : [ {} ], ", entityTypeId, parentEntityTypeId, parentEntityId);

									//Verify If the Blocking message is still available
									boolean blockinngMessageAvailable = verifyBlockingMessage(invoiceType, parentEntityId);
									logger.info("The blocking message Verification Result is : [ {} ]", blockinngMessageAvailable);
									if (blockinngMessageAvailable) {
										Assert.assertTrue(false, "The Blocking message is still available for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

									//Verify Uploaded service Data
									boolean servicedataCreation = verifyInvoiceServiceData(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The service Data Verification Result is : [ {} ]", servicedataCreation);
									if (!servicedataCreation) {
										csAssertion.assertTrue(false, "The service Data Verification is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

									//Download Pricing Data Template
									boolean pricingdataTemplateDownload = invoicePricingDataTemplateDownload(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The pricing data template download Result is : [ {} ] for Invoice.xml Type : [ {} ]", pricingdataTemplateDownload, invoiceType);
									if (!pricingdataTemplateDownload) {
										csAssertion.assertTrue(false, "The pricing data template download is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

									//Update Pricing Data Template
									boolean updatePricingDataXls = updateInvoicePricingDataXlsFile(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The pricing data updation Result is : [ {} ] for Invoice.xml Type : [ {} ]", updatePricingDataXls, invoiceType);
									if (!updatePricingDataXls) {
										csAssertion.assertTrue(false, "The pricing data Xls Updation is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

									//Upload Pricing Data
									boolean uploadPricingData = uploadInvoicePricingDataXlsFile(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The pricing data upload Result is : [ {} ] for Invoice.xml Type : [ {} ]", uploadPricingData, invoiceType);
									if (!uploadPricingData) {
										csAssertion.assertTrue(false, "The pricing data Xls uploading is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

								} else if (runexecutionpart.equalsIgnoreCase("pricingdataverification")) {

									//Verify If the Blocking message is still available
									boolean blockinngMessageAvailable = verifyBlockingMessage(invoiceType, parentEntityId);
									logger.info("The blocking message Verification Result is : [ {} ]", blockinngMessageAvailable);
									if (blockinngMessageAvailable) {
										Assert.assertTrue(false, "The Blocking message is still available for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

									//verify Charges Tab
									boolean chargesTabVerification = verifypricingDataForChargesTab(invoiceType, parentEntityTypeId, parentEntityId, entityTypeId);
									logger.info("The Charges tab Result is : [ {} ] for Invoice.xml Type : [ {} ]", chargesTabVerification, invoiceType);
									if (!chargesTabVerification) {
										csAssertion.assertTrue(false, "The Charges Tab Verification is failed for invoice Type : [ " + invoiceType + " ], parentEntityTypeId : [ " + parentEntityTypeId + " ] entityTypeId: [ " + entityTypeId + " ] \n");
									}

								} else {
									try {
										resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName;
										String tabSpecificId = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "tabspecificidforservicedatatab");
										List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
										logger.info("Record ID list : [ {} ]", recordIdsList);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}

					}
				}
			}
			csAssertion.assertAll();
		}
	}

	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true is the service Data is uploaded , otherwise returns false
	 * @apiNote <b>This method is for creating service Data for invoice type provided in parameter</b>
	 *//*

	private boolean createInvoiceServiceData(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started Creating the Service Data for [ {} ] type Invoice.xml", invoiceType);
		if (invoiceType.equalsIgnoreCase("arc_rrc")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			String sXLSOutputFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_ServiceDataXLSFileName;
			return oInvoiceDataOperations.createServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, sXLSOutputFile, csAssertion);

		} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			String sXLSOutputFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_ServiceDataXLSFileName;
			return oInvoiceDataOperations.createServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, sXLSOutputFile, csAssertion);

		} else if (invoiceType.equalsIgnoreCase("forecast")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Forecast_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Forecast_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			String sXLSOutputFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_ServiceDataXLSFileName;
			return oInvoiceDataOperations.createServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, sXLSOutputFile, csAssertion);

		} else {
			logger.warn("The Invoice.xml Type is not matching , please check");
			return false;
		}
	}


	*/
/**
	 * @param invoiceType    is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityId is ID of the sub contract to be edited
	 * @return true is the service Data is uploaded , otherwise returns false
	 * @apiNote <b>This method is for creating service Data for invoice type provided in parameter</b>
	 *//*

	private boolean verifyBlockingMessage(String invoiceType, String parentEntityId) {
		logger.info("Started verify Blocking Message for [ {} ] type Invoice.xml", invoiceType);
		if (invoiceType.equalsIgnoreCase("arc_rrc")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyContractShowPageBlockedMessage(oServiceDataOperations, parentEntityId, new CustomAssert());

		} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyContractShowPageBlockedMessage(oServiceDataOperations, parentEntityId, new CustomAssert());

		} else if (invoiceType.equalsIgnoreCase("forecast")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Forecast_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Forecast_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyContractShowPageBlockedMessage(oServiceDataOperations, parentEntityId, new CustomAssert());

		} else {
			logger.warn("The Invoice.xml Type is not matching , please check");
			return false;
		}
	}


	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true if the service data tab and audit log tab has the entries  otherwise returns false
	 * @apiNote <b>This method is for verifying the service data uploaded for invoice type</b>
	 *//*

	private boolean verifyInvoiceServiceData(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started Verifying the Service Data for [ {} ] type Invoice.xml", invoiceType);
		if (invoiceType.equalsIgnoreCase("arc_rrc")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, ARC_RRC_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, csAssertion);

		} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Fixed_Fee_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, csAssertion);

		} else if (invoiceType.equalsIgnoreCase("forecast")) {
			int sirialNumber = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("BulkActionServiceDataStartingSirialNumber"));
			ServiceDataOperations oServiceDataOperations = new ServiceDataOperations(InvoiceDataConfigFilePath, Forecast_Invoice_ServiceDataXLSFileName, InvoiceDataConfigFilePath, Forecast_Invoice_ServiceData_Config_JSONFileName, InvoiceDataConfigFilePath, InvoiceDataConfigFileName, Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName, ServiceDataResultVerification_Config, sirialNumber);
			return oInvoiceDataOperations.verifyServiceData(oServiceDataOperations, parentEntityTypeId, parentEntityId, entityTypeId, csAssertion);

		} else {
			logger.warn("The Invoice.xml Type is not matching , please check");
			return false;
		}
	}

	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true is the pricing data template is downloaded , otherwise returns false
	 * @apiNote <b>This method is for downloading the Pricing data template</b>
	 *//*

	private boolean invoicePricingDataTemplateDownload(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started Pricing Data template download for [ {} ] type Invoice.xml", invoiceType);
		try {
			if (invoiceType.equalsIgnoreCase("arc_rrc")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_Pricing_DataXLSFileName;
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName;
				String tabSpecificId = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "tabspecificidforservicedatatab");
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);
				return oInvoiceDataOperations.downloadPricingDataTemplate(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId, recordIdsList);

			} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_Pricing_DataXLSFileName;
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName;
				String tabSpecificId = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "tabspecificidforservicedatatab");
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);
				return oInvoiceDataOperations.downloadPricingDataTemplate(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId, recordIdsList);

			} else if (invoiceType.equalsIgnoreCase("forecast")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_Pricing_DataXLSFileName;
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName;
				String tabSpecificId = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "tabspecificidforservicedatatab");
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);
				return oInvoiceDataOperations.downloadPricingDataTemplate(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId, recordIdsList);

			} else {
				logger.warn("The Invoice.xml Type is not matching , please check");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Got Exception while Pricing Data Template Download for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityTypeId, parentEntityTypeId, parentEntityId, e.getMessage(), e.getStackTrace());
			return false;
		}
	}

	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true is the editing is successful otherwise returns false
	 * @apiNote <b>This method is for editing the pricing data template according to the config json file</b>
	 *//*

	private boolean updateInvoicePricingDataXlsFile(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started Pricing Data file Updation for [ {} ] type Invoice.xml", invoiceType);
		try {
			String invoicePricingDataJsonFile = InvoiceDataConfigFilePath + "/" + InvoicePricingDataJSONFileName;
			if (invoiceType.equalsIgnoreCase("arc_rrc")) {
				ServiceDataOperations oServiceDataOperations = new ServiceDataOperations();
				JSONObject jsonObjectPricingDataJsonFile = oServiceDataOperations.getDataJsonObjForVerification(invoicePricingDataJsonFile);
				JSONObject jsonObjectTillInvoiceTypeInPricingDataJsonFile = jsonObjectPricingDataJsonFile.getJSONObject("ARCRRC");
				XLSUtils oXlsUtils = new XLSUtils(InvoiceDataConfigFilePath, parentEntityId + "_" + ARC_RRC_Invoice_Pricing_DataXLSFileName);
				boolean arc_rrcEditXmlResult = oInvoiceDataOperations.updateXlsFileFor_Invoice(oXlsUtils, jsonObjectTillInvoiceTypeInPricingDataJsonFile);

				JSONObject jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData = jsonObjectPricingDataJsonFile.getJSONObject("Fixed Fee");
				//XLSUtils oXlsUtils = new XLSUtils(InvoiceDataConfigFilePath, parentEntityId + "_" + ARC_RRC_Invoice_Pricing_DataXLSFileName);
				boolean pricingEditXmlResult = oInvoiceDataOperations.updateXlsFileFor_Invoice(oXlsUtils, jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData);

				if (arc_rrcEditXmlResult && pricingEditXmlResult) {
					return true;
				} else {
					return false;
				}

			} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
				ServiceDataOperations oServiceDataOperations = new ServiceDataOperations();
				JSONObject jsonObjectPricingDataJsonFile = oServiceDataOperations.getDataJsonObjForVerification(invoicePricingDataJsonFile);
				JSONObject jsonObjectTillInvoiceTypeInPricingDataJsonFile = jsonObjectPricingDataJsonFile.getJSONObject("Fixed Fee");
				XLSUtils oXlsUtils = new XLSUtils(InvoiceDataConfigFilePath, parentEntityId + "_" + Fixed_Fee_Invoice_Pricing_DataXLSFileName);
				return oInvoiceDataOperations.updateXlsFileFor_Invoice(oXlsUtils, jsonObjectTillInvoiceTypeInPricingDataJsonFile);

			} else if (invoiceType.equalsIgnoreCase("forecast")) {
				ServiceDataOperations oServiceDataOperations = new ServiceDataOperations();
				JSONObject jsonObjectPricingDataJsonFile = oServiceDataOperations.getDataJsonObjForVerification(invoicePricingDataJsonFile);
				JSONObject jsonObjectTillInvoiceTypeInPricingDataJsonFile = jsonObjectPricingDataJsonFile.getJSONObject("Fixed Fee");
				XLSUtils oXlsUtils = new XLSUtils(InvoiceDataConfigFilePath, parentEntityId + "_" + Forecast_Invoice_Pricing_DataXLSFileName);
				return oInvoiceDataOperations.updateXlsFileFor_Invoice(oXlsUtils, jsonObjectTillInvoiceTypeInPricingDataJsonFile);

			} else {
				logger.warn("The Invoice.xml Type is not matching , please check");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Got Exception while Updating Pricing Data Template for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityTypeId, parentEntityTypeId, parentEntityId, e.getMessage(), e.getStackTrace());
			return false;
		}
	}


	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true if the upload is successful otherwise returns false
	 * @apiNote <b>This method is for uploading the pricing data template</b>
	 *//*

	private boolean uploadInvoicePricingDataXlsFile(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started uploading Pricing Data file for [ {} ] type Invoice.xml", invoiceType);
		String pricingDataUploadResult = "";
		try {
			if (invoiceType.equalsIgnoreCase("arc_rrc")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_Pricing_DataXLSFileName;
				pricingDataUploadResult = oInvoiceDataOperations.uploadPricingDataXlsFile(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId);

			} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_Pricing_DataXLSFileName;
				pricingDataUploadResult = oInvoiceDataOperations.uploadPricingDataXlsFile(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId);

			} else if (invoiceType.equalsIgnoreCase("forecast")) {
				String sXLSPricingdataFile = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_Pricing_DataXLSFileName;
				pricingDataUploadResult = oInvoiceDataOperations.uploadPricingDataXlsFile(sXLSPricingdataFile, parentEntityTypeId, parentEntityId, entityTypeId);

			} else {
				logger.warn("The Invoice.xml Type is not matching , please check");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Got Exception while Uploading Pricing Data xls file for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityTypeId, parentEntityTypeId, parentEntityId, e.getMessage(), e.getStackTrace());
			return false;
		}
		logger.debug("The Pricing Data Upload Result String is : [ {} ]", pricingDataUploadResult);
		if ((pricingDataUploadResult != null) && (!pricingDataUploadResult.equalsIgnoreCase(""))) {
			if (pricingDataUploadResult.contains(pricingDataUploadRsultRequiredString)) {
				logger.debug("The upload result String : [ {} ] contains required String : [ {} ] ,for for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ]", pricingDataUploadResult, pricingDataUploadRsultRequiredString, entityTypeId, parentEntityTypeId, parentEntityId);
				return true;
			} else {
				logger.error("The upload result String : [ {} ] does not contains the required String : [ {} ] for for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ]", pricingDataUploadResult, pricingDataUploadRsultRequiredString, entityTypeId, parentEntityTypeId, parentEntityId);
				return false;
			}

		} else {
			logger.error("The upload result String : [ {} ] is not proper for for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ]", pricingDataUploadResult, entityTypeId, parentEntityTypeId, parentEntityId);
			return false;
		}
	}


	*/
/**
	 * @param invoiceType        is invoice type , it can be either arc_rrc or fixed_fee or forecast
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param entityTypeId       is the ID if Service Data Entity
	 * @return true if the charges tab verification is successful otherwise returns false
	 * @apiNote <b>This method is for verifying the charges tab</b>
	 *//*

	private boolean verifypricingDataForChargesTab(String invoiceType, String parentEntityTypeId, String parentEntityId, String entityTypeId) {
		logger.info("Started verify Pricing data for [ {} ] type Invoice.xml", invoiceType);

		String invoicePricingDataJsonFile = InvoiceDataConfigFilePath + "/" + InvoicePricingDataJSONFileName;
		ServiceDataOperations oServiceDataOperations = new ServiceDataOperations();
		JSONObject jsonObjectPricingDataJsonFile = oServiceDataOperations.getDataJsonObjForVerification(invoicePricingDataJsonFile);
		JSONObject jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData = jsonObjectPricingDataJsonFile.getJSONObject("Fixed Fee");
		try {
			String tabSpecificId = ParseConfigFile.getValueFromConfigFile(InvoiceDataConfigFilePath, InvoiceDataConfigFileName, "default", "tabspecificidforservicedatatab");
			if (invoiceType.equalsIgnoreCase("arc_rrc")) {
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_ServiceDataResultVerificationDataJSONFileName;
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);

				//Get First entry , as we assume that only one row is being edited
				String recordId = recordIdsList.get(0);
				return oInvoiceDataOperations.verifyChargesTab(entityTypeId, recordId, jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData);

			} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_ServiceDataResultVerificationDataJSONFileName;
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);

				//Get First entry , as we assume that only one row is being edited
				String recordId = recordIdsList.get(0);
				return oInvoiceDataOperations.verifyChargesTab(entityTypeId, recordId, jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData);

			} else if (invoiceType.equalsIgnoreCase("forecast")) {
				String resultVerificationJsonFileName = InvoiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_ServiceDataResultVerificationDataJSONFileName;
				List<String> recordIdsList = oInvoiceDataOperations.getRecordIDList(tabSpecificId, resultVerificationJsonFileName, entityTypeId, parentEntityTypeId, parentEntityId);
				logger.debug("Record ID list : [ {} ]", recordIdsList);

				//Get First entry , as we assume that only one row is being edited
				String recordId = recordIdsList.get(0);
				return oInvoiceDataOperations.verifyChargesTab(entityTypeId, recordId, jsonObjectTillInvoiceTypeInPricingDataJsonFilePricingData);

			} else {
				logger.warn("The Invoice.xml Type is not matching , please check");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Got Exception while verifying  Charges Tab  for entityTypeId : [ {} ], parentEntityTypeId : [ {} ], parentEntityId : [ {} ], Cause : [ {} ], Exception : [ {} ]", entityTypeId, parentEntityTypeId, parentEntityId, e.getMessage(), e.getStackTrace());
			return false;
		}
	}


}
*/
