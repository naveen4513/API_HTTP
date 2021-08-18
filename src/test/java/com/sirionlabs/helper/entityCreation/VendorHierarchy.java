package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VendorHierarchy {

	private final static Logger logger = LoggerFactory.getLogger(VendorHierarchy.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createVendorHierarchy() {
		return createVendorHierarchy("default", false);
	}

	public static String createVendorHierarchy(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Vendor Hierarchy. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("VendorHierarchyFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("VendorHierarchyFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("VendorHierarchyExtraFieldsFileName");
			return createVendorHierarchy(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Vendor Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createVendorHierarchy(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createVendorHierarchy(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createVendorHierarchy(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                           String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Vendor Hierarchy using Section {}", sectionName);

			createResponse = createEntityObj.create("vendors", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Vendor Hierarchy using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
