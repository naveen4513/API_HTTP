package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.config.ConfigureConstantFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractTemplate {
	private final static Logger logger = LoggerFactory.getLogger(ContractTemplate.class);

	/*
	If Config Files are not specified then Default Config Files will be used with Default Section.
	 */
	public static String createContractTemplate() {
		return createContractTemplate("default", false);
	}

	public static String createContractTemplate(String sectionName, Boolean isLocalEntity) {
		try {
			logger.info("Config files not specified for Entity ContractTemplate. Hence using default config files.");
			String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateFilePath");
			String configFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateFileName");
			String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateExtraFieldsFileName");
			return createContractTemplate(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while fetching ContractTemplate Details from Config files. {}", e.getMessage());
			return null;
		}
	}

	/*
	If Section Name and isLocalEntity is not defined then it will create Global Entity with Default Values
	 */
	public static String createContractTemplate(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		return createContractTemplate(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, "default", false);
	}

	/*
	SectionName: Section Name in the Config file from where the Entity Properties will be picked.
	IsLocalEntity: Whether to create Entity Locally or Globally. If set to true then will create Locally otherwise Globally.
	 */
	public static String createContractTemplate(String configFilePath, String configFileName, String extraFieldsConfigFilePath, String extraFieldsConfigFileName,
	                                  String sectionName, Boolean isLocalEntity) {
		String createResponse = null;
		try {
			CreateEntity createEntityObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, sectionName);
			logger.info("Creating Entity ContractTemplate using Section {}", sectionName);

			createResponse = createEntityObj.create("contract templates", isLocalEntity);
		} catch (Exception e) {
			logger.error("Exception while Creating ContractTemplate using Section {}. {}", sectionName, e.getStackTrace());
		}
		return createResponse;
	}
}