package com.sirionlabs.helper.PerformanceData;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CreateEntities {

	private final static Logger logger = LoggerFactory.getLogger(CreateEntities.class);
	private String configFilePath;
	private String configFileName;
	private String extraFieldsConfigFilePath;
	private String extraFieldsConfigFileName;
	private int noOfRecordsToProcessAtOnce = 10000;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformanceDataEntityCreationConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformanceDataEntityCreationConfigFileName");
		extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformanceDataEntityCreationExtraFieldsForExcelConfigFilePath");
		extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformanceDataEntityCreationExtraFieldsForExcelConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "NoOfRecordsToProcessAtOnce");
		if (temp != null && NumberUtils.isParsable(temp))
			noOfRecordsToProcessAtOnce = Integer.parseInt(temp);
	}

	@DataProvider
	public Object[][] dataProviderForCreateEntity() {
		List<Object[]> allTestData = new ArrayList<>();
		try {
			logger.info("Setting Entities to be Created");
			String entities[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiesToCreate")
					.split(Pattern.quote(","));
			for (String entityToCreate : entities) {
				String entityName = entityToCreate.trim();
				String excelFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "excelFilePath");
				String excelSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityToCreate, "excelSheetName");
				String excelFileNameStr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityToCreate, "excelFileName");
				String allExcelFileNames[] = excelFileNameStr.split(Pattern.quote(","));

				for (String excelFileName : allExcelFileNames) {
					allTestData.add(new Object[]{entityName, excelFilePath.trim(), excelFileName.trim(), excelSheetName.trim()});
				}
			}
		} catch (Exception e) {
			logger.error("Exception in Data Provider for Performance Data Entity Creation. {}", e.getMessage());
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForCreateEntity")
	public void createEntity(String entityName, String excelFilePath, String excelFileName, String excelSheetName) {
		try {
			CreateEntity createObj = new CreateEntity(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, entityName,
					true);

			List<String> fieldsToHaveMultipleValues = new ArrayList<>();
			String fieldsStr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "multipleValuesEntityFieldsMapping", entityName);
			if (fieldsStr != null && !fieldsStr.trim().equalsIgnoreCase("")) {
				String fields[] = fieldsStr.split(Pattern.quote(","));
				for (String field : fields) {
					fieldsToHaveMultipleValues.add(field.trim().toLowerCase());
				}
			}

			int maxNoOfMultipleValues = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfMultipleValues"));
			int randomNumber = RandomNumbers.getRandomNumberWithinRange(1, maxNoOfMultipleValues);

			logger.info("*************************************************************************************************");
			logger.info("Creating Entity {} using Excel File {} and Sheet {}.", entityName, excelFilePath + "/" + excelFileName, excelSheetName);
			logger.info("Max No. of Multiple Values for this Sheet is: {}", randomNumber);

			List<String> ignoreRequiredFields = new ArrayList<>();

			if (ParseConfigFile.hasProperty(configFilePath, configFileName, "ignoreRequiredFields", entityName)) {
				String ignoreFieldsStr = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "ignoreRequiredFields", entityName);
				String allIgnoreFields[] = ignoreFieldsStr.split(Pattern.quote(","));
				for (String field : allIgnoreFields) {
					ignoreRequiredFields.add(field.trim());
				}
			}

			if (ParseConfigFile.hasProperty(configFilePath, configFileName, entityName, "stakeHolderColumnName")) {
				String stakeHolderColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "stakeHolderColumnName").trim();
				createObj.createFromExcel(excelFilePath, excelFileName, excelSheetName, entityName, noOfRecordsToProcessAtOnce, fieldsToHaveMultipleValues, randomNumber,
						ignoreRequiredFields, stakeHolderColumnName);
			} else {
				createObj.createFromExcel(excelFilePath, excelFileName, excelSheetName, entityName, noOfRecordsToProcessAtOnce, fieldsToHaveMultipleValues, randomNumber,
						ignoreRequiredFields);
			}
			logger.info("*************************************************************************************************");
		} catch (Exception e) {
			logger.error("Exception while Creating Entity {}. {}", entityName, e.getStackTrace());
		}
	}
}
