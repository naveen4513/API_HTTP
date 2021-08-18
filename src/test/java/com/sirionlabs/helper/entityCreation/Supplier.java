package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Supplier {
	private final static Logger logger = LoggerFactory.getLogger(Supplier.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createSupplier() {
		return createSupplier("default", true);
	}

	public static String createSupplier(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Suppliers. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SupplierFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierExtraFieldsFileName");
			return createSupplier(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Supplier Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Local Entity with Default Values
	 */
	public static String createSupplier(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createSupplier(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", true);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createSupplier(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                    String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Suppliers using Section {}", sectionName);

			createResponse = createEntityObj.create("suppliers", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Supplier using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
