package com.sirionlabs.helper.DownloadTemplates;

import com.sirionlabs.api.invoice.InvoiceConsumptionTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by shivashish on 26/3/18.
 */
public class DownloadConsumptionTemplate {

	private final static Logger logger = LoggerFactory.getLogger(DownloadConsumptionTemplate.class);

	String DownloadConsumptionDataConfigFilePath = "";
	String DownloadConsumptionDataConfigFileName = "";

	String ConsumptionEntityType = "";

	/**
	 * Constructor For Initializing the Config File
	 */

	public DownloadConsumptionTemplate() {
		DownloadConsumptionDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadTemplatesConfigFilePath");
		DownloadConsumptionDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DownloadConsumptionDataConfigFileName");
		ConsumptionEntityType = ConfigureConstantFields.getConstantFieldsProperty("ConsumptionEntityTypeId");
	}

	/**
	 * this function is to download consumption template for the given consumption Ids
	 *
	 * @param sOutputFile  is path where the template of consumption will be downloaded
	 * @param entityIDList is the list DB id of Consumption
	 * @param entityTypeId is type id of Consumption
	 */
	private boolean downloadConsumptionTemplate(String sOutputFile, String entityTypeId, List<String> entityIDList) {
		InvoiceConsumptionTemplateDownload obj = new InvoiceConsumptionTemplateDownload();
		try {
			logger.info("downloading Consumption template. File location : {}", sOutputFile);
			obj.downloadInvoiceConsumptionTemplateFile(sOutputFile, entityTypeId, entityIDList);
			return true;
		} catch (Exception e) {
			logger.error("Exception while downloading consumption template. error message = {}", e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param consumptionIds is the ids of Consumption for which we need to download template
	 * @return
	 */
	public boolean downloadConsumptionDataTemplate(List<String> consumptionIds) {
		logger.info("Started downloading the Consumption templates  for consumptionId's --> [ {} ] ", consumptionIds.toString());
		if (!consumptionIds.isEmpty()) {
			String sXLSOutputFile = DownloadConsumptionDataConfigFilePath + "/" + "Invoice_Consumption_Data.xlsm";
			return downloadConsumptionTemplate(sXLSOutputFile, ConsumptionEntityType, consumptionIds);

		} else {
			logger.warn("Please Provide the Consumption Id for Which Template need to be downloaded");
			return false;

		}
	}


}
