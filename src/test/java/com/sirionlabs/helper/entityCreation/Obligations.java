package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Obligations {

	private final static Logger logger = LoggerFactory.getLogger(Obligations.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createObligation() {
		return createObligation("default", false);
	}

	public static String createObligation(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Obligations. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationsFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationsFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationsExtraFieldsFileName");
			return createObligation(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Obligations Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createObligation(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createObligation(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                      String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Obligations using Section {}", sectionName);

			createResponse = createEntityObj.create("obligations", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Obligation using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
