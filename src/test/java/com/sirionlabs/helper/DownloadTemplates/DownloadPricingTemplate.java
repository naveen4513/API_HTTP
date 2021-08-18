package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shivashish on 26/3/18.
 */
public class DownloadPricingTemplate {

	private final static Logger logger = LoggerFactory.getLogger(DownloadPricingTemplate.class);

	String DownloadPricingDataConfigFilePath = "";
	String DownloadPricingDataConfigFileName = "";

	String Fixed_Fee_Invoice_Pricing_DataXLSFileName = "";
	String ARC_RRC_Invoice_Pricing_DataXLSFileName = "";
	String Forecast_Invoice_Pricing_DataXLSFileName = "";

	/**
	 * Constructor For Initializing the Config File
	 */

	public DownloadPricingTemplate() {
		DownloadPricingDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadTemplatesConfigFilePath");
		DownloadPricingDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DownloadServiceDataConfigFileName");


		Fixed_Fee_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_Pricing_DataXLSFileName");
		ARC_RRC_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_Pricing_DataXLSFileName");
		Forecast_Invoice_Pricing_DataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_Pricing_DataXLSFileName");
	}

	/**
	 * this function is to download pricing template for the given service data list
	 *
	 * @param outputFile        is path where the template of pricing will be downloaded
	 * @param serviceDataIDList is the list DB id of Service Data
	 */
	private boolean downloadPricingTemplate(String outputFile, List<String> serviceDataIDList) {
		InvoicePricingTemplateDownload obj = new InvoicePricingTemplateDownload();
		try {
			logger.info("downloading pricing template. File location : {}", outputFile);
			obj.downloadInvoicePricingTemplateFile(outputFile, serviceDataIDList);
			return true;
		} catch (Exception e) {
			logger.error("Exception while downloading pricing template. error message = {}", e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param invoiceType    is the type of invoice
	 * @param parentEntityId is the id of contract on which service data template is being downloaded
	 * @return
	 */
	public boolean downloadPricingDataTemplate(String invoiceType, String parentEntityId, List<String> serviceDataIDList) {
		logger.info("Started dowloading the template for uploading pricing for [ {} ] type ", invoiceType);
		if (invoiceType.equalsIgnoreCase("arc_rrc")) {
			String sXLSOutputFile = DownloadPricingDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_Pricing_DataXLSFileName;
			return downloadPricingTemplate(sXLSOutputFile, serviceDataIDList);

		} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
			String sXLSOutputFile = DownloadPricingDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_Pricing_DataXLSFileName;
			return downloadPricingTemplate(sXLSOutputFile, serviceDataIDList);

		} else if (invoiceType.equalsIgnoreCase("forecast")) {
			String sXLSOutputFile = DownloadPricingDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_Pricing_DataXLSFileName;
			return downloadPricingTemplate(sXLSOutputFile, serviceDataIDList);

		} else {
			logger.warn("The Invoice Type mentioned in config file is incorrect , please check");
			return false;

		}
	}


}
