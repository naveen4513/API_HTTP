package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.servicedata.ServiceDataTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shivashish on 20/3/18.
 */
public class DownloadServiceDataTemplate {


	private final static Logger logger = LoggerFactory.getLogger(DownloadServiceDataTemplate.class);

	String DownloadServiceDataConfigFilePath = "";
	String DownloadServiceDataConfigFileName = "";

	String ARC_RRC_Invoice_ServiceDataXLSFileName = "";
	String Fixed_Fee_Invoice_ServiceDataXLSFileName = "";
	String Forecast_Invoice_ServiceDataXLSFileName = "";


	/**
	 * Constructor For Initializing the Config File
	 */

	public DownloadServiceDataTemplate() {
		DownloadServiceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadTemplatesConfigFilePath");
		DownloadServiceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DownloadServiceDataConfigFileName");


		ARC_RRC_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("ARC_RRC_Invoice_Pricing_DataXLSFileName");
		Fixed_Fee_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Fixed_Fee_Invoice_ServiceDataXLSFileName");
		Forecast_Invoice_ServiceDataXLSFileName = ConfigureConstantFields.getConstantFieldsProperty("Forecast_Invoice_ServiceDataXLSFileName");
	}


	/**
	 * @param parentEntityTypeId is the ID of Contract entity
	 * @param parentEntityId     is ID of the sub contract to be edited
	 * @param sOutputFile        is the XLS file location where the Template need to be downloaded
	 * @return true if the Download template is successful otherwise returns false
	 * @apiNote <b>This method is for Downloading of Service Data Template</b>
	 */
	private boolean testBulkActionsDownloadTemplateXLSFile(String parentEntityTypeId, String parentEntityId, String sOutputFile) {
		try {
			ServiceDataTemplateDownload oServiceDataTemplateDownload = new ServiceDataTemplateDownload();
			oServiceDataTemplateDownload.downloadServiceDataTemplateFile(sOutputFile, parentEntityTypeId, parentEntityId);
			logger.info("Download XLS Template download is successful for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception while running testBulkActionsDownloadTemplateXLSFile {}", e.getStackTrace());
			logger.error("Failed to Download XLS Template for parentEntityTypeId : [ {} ] , parentEntityId : [ {} ] , and download File Location  : [ {} ]", parentEntityTypeId, parentEntityId, sOutputFile);
			return false;
		}
	}


	/**
	 * @param invoiceType        is the type of invoice
	 * @param parentEntityTypeId is the type id of contracts
	 * @param parentEntityId     is the id of contract on which service data template is being downloaded
	 * @return
	 */
	public boolean downloadServiceDataTemplate(String invoiceType, String parentEntityTypeId, String parentEntityId) {
		logger.info("Started Downloading the template of the Service Data for [ {} ] type ", invoiceType);
		if (invoiceType.equalsIgnoreCase("arc_rrc")) {
			String sXLSOutputFile = DownloadServiceDataConfigFilePath + "/" + parentEntityId + "_" + ARC_RRC_Invoice_ServiceDataXLSFileName;
			return testBulkActionsDownloadTemplateXLSFile(parentEntityTypeId, parentEntityId, sXLSOutputFile);

		} else if (invoiceType.equalsIgnoreCase("fixed_fee")) {
			String sXLSOutputFile = DownloadServiceDataConfigFilePath + "/" + parentEntityId + "_" + Fixed_Fee_Invoice_ServiceDataXLSFileName;
			return testBulkActionsDownloadTemplateXLSFile(parentEntityTypeId, parentEntityId, sXLSOutputFile);

		} else if (invoiceType.equalsIgnoreCase("forecast")) {
			String sXLSOutputFile = DownloadServiceDataConfigFilePath + "/" + parentEntityId + "_" + Forecast_Invoice_ServiceDataXLSFileName;
			return testBulkActionsDownloadTemplateXLSFile(parentEntityTypeId, parentEntityId, sXLSOutputFile);

		} else {
			logger.warn("The Invoice Type mentioned in not valid , please check");
			return false;
		}
	}

}
