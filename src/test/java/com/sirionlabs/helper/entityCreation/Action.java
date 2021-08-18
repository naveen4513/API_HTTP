package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Action {

	private final static Logger logger = LoggerFactory.getLogger(Action.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createAction() {
		return createAction("default", false);
	}

	public static String createAction(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity Actions. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ActionFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionExtraFieldsFileName");
			return createAction(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching Action Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createAction(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createAction(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createAction(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                  String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Actions using Section {}", sectionName);

			createResponse = createEntityObj.create("actions", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating Action using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
