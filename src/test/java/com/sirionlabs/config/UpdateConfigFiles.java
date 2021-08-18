package com.sirionlabs.config;

import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateConfigFiles {

	private final static Logger logger = LoggerFactory.getLogger(UpdateConfigFiles.class);

	@BeforeSuite
	@Parameters({"TestingType", "Environment"})
	public void updateConfigFiles(String testingType, String environment) {
		try {
			if(System.getProperty("Environment") != null) {
				environment = System.getProperty("Environment");
			}

			String mappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFilesMappingPath");
			String mappingFileName = ConfigureConstantFields.getConstantFieldsProperty("ConfigFilesMappingName");
			String testingTypeBaseLocation = ParseConfigFile.getValueFromConfigFileCaseSensitive(mappingFilePath, mappingFileName, "default",
					testingType + "SuitePath");

			logger.info("Updating all Testing Type Based config files.");
			Map<String, String> testingTypeBasedConfigFilesMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(mappingFilePath, mappingFileName,
					"TestingTypeConfigFilesMapping");
			List<String> allTestingTypeBasedConfigFilesNames = new ArrayList<>(testingTypeBasedConfigFilesMap.keySet());

			for (String testingTypeBasedConfigFile : allTestingTypeBasedConfigFilesNames) {
				String sourceLocation = testingTypeBaseLocation + "/" + testingTypeBasedConfigFile;
				if (new File(sourceLocation).exists()) {
					logger.info("Replacing Testing Type Based file [{}] with [{}]", testingTypeBasedConfigFilesMap.get(testingTypeBasedConfigFile),
							testingTypeBaseLocation + "/" + testingTypeBasedConfigFile);
					FileUtils.copyFile(new File(sourceLocation), new File(testingTypeBasedConfigFilesMap.get(testingTypeBasedConfigFile)));
				}
			}

			String environmentBaseLocation = ParseConfigFile.getValueFromConfigFileCaseSensitive(mappingFilePath, mappingFileName, "EnvironmentMapping", environment);
			String configFilesLocation = testingTypeBaseLocation + "/" + environmentBaseLocation;

			logger.info("Updating All Config Files for Testing Type {} and Environment {}", testingType, environment);
			Map<String, String> configFilesMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(mappingFilePath, mappingFileName, "ConfigFilesMapping");
			List<String> allConfigFilesNames = new ArrayList<>(configFilesMap.keySet());
			for (String configFile : allConfigFilesNames) {
				String destinationLocation = configFilesMap.get(configFile);
				String sourceLocation = configFilesLocation + "/" + configFile;

				if (new File(sourceLocation).exists()) {
					logger.info("Replacing file [{}] with [{}]", destinationLocation, sourceLocation);
					FileUtils.copyFile(new File(sourceLocation), new File(destinationLocation));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Updating Config Files for Testing Type {} and Environment {}. {}", testingType, environment, e.getStackTrace());
		}
	}
}
