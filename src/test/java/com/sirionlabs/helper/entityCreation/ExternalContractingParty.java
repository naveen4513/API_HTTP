package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalContractingParty {

	private final static Logger logger = LoggerFactory.getLogger(ExternalContractingParty.class);

	public static String createECP(String sectionName) {
		try {
			logger.info("Config files not specified for Entity Actions. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ECPFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ECPFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ECPExtraFieldsFileName");
			return createECP(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName);
		} catch (Exception e) {
			logger.error("Exception while fetching Action Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createECP(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                               String sectionName) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity External Contracting Party using Section {}", sectionName);

			createResponse = createEntityObj.create("externalcontractingparty", true);
		} catch (Exception e) {
			logger.error("Exception while Creating External Contracting Party using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}