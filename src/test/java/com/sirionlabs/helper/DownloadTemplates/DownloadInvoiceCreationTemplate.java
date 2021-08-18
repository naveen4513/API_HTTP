package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.invoice.InvoiceCreationTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 2/4/18.
 */
public class DownloadInvoiceCreationTemplate {


	private final static Logger logger = LoggerFactory.getLogger(DownloadInvoiceCreationTemplate.class);

	String DownloadInvoiceCreationConfigFilePath = "";
	String DownloadInvoiceCreationConfigFileName = "";


	/**
	 * Constructor For Initializing the Config File
	 */

	public DownloadInvoiceCreationTemplate() {
		DownloadInvoiceCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadTemplatesConfigFilePath");
		DownloadInvoiceCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DownloadInvoiceCreationConfigFileName");

	}


	/**
	 * @param parentEntityTypeId is the type ID of Contract entity
	 * @param parentEntityId     is ID of contracts
	 * @param sOutputFile        is the XLS file location where the Template need to be downloaded
	 * @return true if the Download template is successful otherwise returns false
	 * @apiNote <b>This method is for Downloading of Invoice Creation Template</b>
	 */
	private boolean testInvoiceCreationTemplateDownload(String sOutputFile, String parentEntityTypeId, String parentEntityId) {
		try {
			InvoiceCreationTemplateDownload invoiceCreationTemplateDownload = new InvoiceCreationTemplateDownload();
			invoiceCreationTemplateDownload.downloadInvoiceCreationTemplateFile(sOutputFile, parentEntityTypeId, parentEntityId);
			logger.info("Invoice Creation XLS Template download is successful for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception while downloading Invoice Creation XLS Template {}", e.getStackTrace());
			logger.error("Failed to Download Invoice Creation XLS Template for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return false;
		}
	}


	/**
	 * @param parentEntityTypeId is the type id of contracts
	 * @param parentEntityId     is the id of contract on which invoice creation template is being downloaded
	 * @return
	 */
	public boolean downloadInvoiceCreationTemplate(String parentEntityTypeId, String parentEntityId) {
		logger.info("Started downloading the Invoice Creation template  for Contract Id --> [ {} ] ", parentEntityId);
		try {
			int id = Integer.parseInt(parentEntityId);
			String sXLSOutputFile = DownloadInvoiceCreationConfigFilePath + "/" + id + "_Invoice_Creation_Template.xlsm";
			return testInvoiceCreationTemplateDownload(sXLSOutputFile, parentEntityTypeId, parentEntityId);

		} catch (Exception e) {
			logger.warn("Please Provide the Correct Contract Id  " + e.getStackTrace());
			return false;

		}
	}

}
