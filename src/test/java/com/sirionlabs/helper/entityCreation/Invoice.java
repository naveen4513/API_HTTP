package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Invoice {
	private final static Logger logger = LoggerFactory.getLogger(Invoice.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createInvoice() {
		return createInvoice("default", false);
	}

	public static String createInvoice(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Invoice. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");
			return createInvoice(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Invoice Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createInvoice(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createInvoice(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createInvoice(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                   String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Invoice using Section {}", sectionName);

			createResponse = createEntityObj.create("invoices", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Invoice using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
