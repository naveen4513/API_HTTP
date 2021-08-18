package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.invoice.InvoiceLineItemsCreationTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 2/4/18.
 */
public class DownloadInvoiceLineItemsCreationTemplate {

	private final static Logger logger = LoggerFactory.getLogger(DownloadInvoiceLineItemsCreationTemplate.class);

	String DownloadInvoiceLineItemsCreationConfigFilePath = "";
	String DownloadInvoiceLineItemsCreationConfigFileName = "";


	/**
	 * Constructor For Initializing the Config File
	 */

	public DownloadInvoiceLineItemsCreationTemplate() {
		DownloadInvoiceLineItemsCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadTemplatesConfigFilePath");
		DownloadInvoiceLineItemsCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DownloadInvoiceLineItemsCreationConfigFileName");

	}


	/**
	 * @param parentEntityTypeId is the type ID of invoice entity
	 * @param parentEntityId     is ID of invoice
	 * @param sOutputFile        is the XLS file location where the Template need to be downloaded
	 * @return true if the Download template is successful otherwise returns false
	 * @apiNote <b>This method is for Downloading of Invoice Creation Template</b>
	 */
	private boolean testInvoiceLineItemsCreationTemplateDownload(String sOutputFile, String parentEntityTypeId, String parentEntityId) {
		try {
			InvoiceLineItemsCreationTemplateDownload invoiceCreationTemplateDownload = new InvoiceLineItemsCreationTemplateDownload();
			invoiceCreationTemplateDownload.downloadInvoiceLineItemsCreationTemplateFile(sOutputFile, parentEntityTypeId, parentEntityId);
			logger.info("Invoice Line Items Creation XLS Template download is successful for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception while downloading Invoice Line Items Creation XLS Template {}", e.getStackTrace());
			logger.error("Failed to Download Invoice Line Items Creation XLS Template for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return false;
		}
	}


	/**
	 * @param parentEntityTypeId is the type id of invoice
	 * @param parentEntityId     is the id of invoice on which invoice line items creation template is being downloaded
	 * @return
	 */
	public boolean downloadInvoiceLineItemsCreationTemplate(String parentEntityTypeId, String parentEntityId) {
		logger.info("Started downloading the Invoice Creation template  for Contract Id --> [ {} ] ", parentEntityId);
		try {
			int id = Integer.parseInt(parentEntityId);
			String sXLSOutputFile = DownloadInvoiceLineItemsCreationConfigFilePath + "/" + id + "_Invoice_LineItems_Creation_Template.xlsm";
			return testInvoiceLineItemsCreationTemplateDownload(sXLSOutputFile, parentEntityTypeId, parentEntityId);

		} catch (Exception e) {
			logger.warn("Please Provide the Correct Contract Id  " + e.getStackTrace());
			return false;

		}
	}

}
