package com.sirionlabs.helper.entityCreation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationGroup {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationGroup.class);

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createApplicationGroup(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createApplicationGroup(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default");
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createApplicationGroup(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                            String sectionName) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity Application Group using Section {}", sectionName);

			createResponse = createEntityObj.create("applicationgroups", true);
		} catch (Exception e) {
			logger.error("Exception while Creating Application Group using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}
