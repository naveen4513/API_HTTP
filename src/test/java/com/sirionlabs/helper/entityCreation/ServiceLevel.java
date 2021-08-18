package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceLevel {

	private final static Logger logger = LoggerFactory.getLogger(ServiceLevel.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createServiceLevel() {
		return createServiceLevel("default", false);
	}

	public static String createServiceLevel(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Service Levels. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");
			return createServiceLevel(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Service Level Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createServiceLevel(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createServiceLevel(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createServiceLevel(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                        String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Service Levels using Section {}", sectionName);

			createResponse = createEntityObj.create("service levels", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Service Level using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
