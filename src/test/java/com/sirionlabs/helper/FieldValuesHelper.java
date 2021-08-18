package com.sirionlabs.helper;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldValuesHelper {

	private final static Logger logger = LoggerFactory.getLogger(FieldValuesHelper.class);
	private String commonConfigFilePath = "src/test/resources/TestConfig/RBI";
	private String commonConfigFileName = "FieldOptionsMapping.cfg";
	private String configFilePath;
	private String configFileName;

	public FieldValuesHelper() {
		configFilePath = commonConfigFilePath;
		configFileName = commonConfigFileName;
	}

	public FieldValuesHelper(String configFilePath, String configFileName) {
		this.commonConfigFilePath = null;
		this.commonConfigFileName = null;
		this.configFilePath = configFilePath;
		this.configFileName = configFileName;
	}

	public FieldValuesHelper(String commonConfigFilePath, String commonConfigFileName, String configFilePath, String configFileName) {
		this.commonConfigFilePath = commonConfigFilePath;
		this.commonConfigFileName = commonConfigFileName;
		this.configFilePath = configFilePath;
		this.configFileName = configFileName;
	}

	public void validateFieldValues(List<String> allFieldsToTest, String entityName, String jsonResponse, CustomAssert csAssert) {
		if (!allFieldsToTest.isEmpty()) {
			for (String fieldToTest : allFieldsToTest) {
				try {
					fieldToTest = fieldToTest.trim();
					logger.info("Validating Field {} of Entity {}", fieldToTest, entityName);
					logger.info("Getting all Possible Values Map for Field {}", fieldToTest);
					Map<String, String> allValuesMap = getAllPossibleValuesMapForField(fieldToTest);
					logger.info("Getting all Available Options for Field {}", fieldToTest);
					List<Map<String, String>> allOptions = ParseJsonResponse.getAllOptionsForField(jsonResponse, fieldToTest, false);

					if (allOptions != null) {
						for (Map<String, String> optionMap : allOptions) {
							String optionId = optionMap.get("id");
							String optionName = optionMap.get("name");

							if (allValuesMap.containsKey(optionId)) {
								String expectedOptionName = allValuesMap.get(optionId);

								if (!optionName.trim().equalsIgnoreCase(expectedOptionName.trim())) {
									csAssert.assertTrue(false, "Expected Option Value: [" + expectedOptionName.trim() +
											"] and Actual Option Value: [" + optionName + "] for Field " + fieldToTest + " for Entity " + entityName);
								}
							} else {
								logger.error("Option Id " + optionId + " is not present in All Values Map for Field " + fieldToTest + " for Entity " + entityName);
								csAssert.assertTrue(false, "Option Id " + optionId + " is not present in All Values Map for Field " +
										fieldToTest + " for Entity " + entityName);
							}
						}
					} else {
						logger.error("Couldn't get Options for Field " + fieldToTest + " for Entity " + entityName);
						csAssert.assertTrue(false, "Couldn't get Options for Field " + fieldToTest + " for Entity " + entityName);
					}
				} catch (Exception e) {
					logger.error("Exception while Validating Field {} of  Entity {}. {}", fieldToTest, entityName, e.getStackTrace());
					csAssert.assertTrue(false, "Exception while Validating Field " + fieldToTest + " of Entity " + entityName + ". " +
							e.getMessage());
				}
			}
		} else {
			csAssert.assertTrue(false, "Couldn't get Fields to Test for Entity " + entityName);
		}
	}

	private Map<String, String> getAllPossibleValuesMapForField(String fieldName) {
		Map<String, String> valuesMap = new HashMap<>();

		try {
			valuesMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, fieldName);
		} catch (Exception e) {
			logger.error("Exception while Getting All Possible Values Map for Field {}. {}", fieldName, e.getStackTrace());
		}
		return valuesMap;
	}

	public void validateFieldLabels(String entityName, String jsonResponse, CustomAssert csAssert) {
		List<String> ignoreLabelsList = getAllFieldLabelsToIgnore(entityName);
		List<String> allLabels = getAllFieldLabels(jsonResponse);

		validateFieldLabels(entityName, allLabels, ignoreLabelsList, csAssert);
	}

	public void validateFieldLabels(String entityName, String jsonResponse, List<String> ignoreLabelsList, CustomAssert csAssert) {
		List<String> allLabels = getAllFieldLabels(jsonResponse);

		validateFieldLabels(entityName, allLabels, ignoreLabelsList, csAssert);
	}

	public void validateFieldLabels(String entityName, List<String> allLabels, CustomAssert csAssert) {
		List<String> ignoreLabelsList = getAllFieldLabelsToIgnore(entityName);
		validateFieldLabels(entityName, allLabels, ignoreLabelsList, csAssert);
	}

	public void validateFieldLabels(String entityName, List<String> allLabels, List<String> ignoreLabelsList, CustomAssert csAssert) {
		try {
			if (allLabels == null) {
				csAssert.assertTrue(false, "Couldn't get All Labels to Test.");
			} else {
				logger.info("Validating All Field Labels of Entity {}", entityName);
				String specialChars = "/*!@#$%^&*()\"{}_[]|\\?/<>,. ";

				for (String label : allLabels) {
					if (ignoreLabelsList.contains(label)) {
						continue;
					}

					Boolean flag = false;
					List<Character> nonRussianChars = new ArrayList<>();

					for (int i = 0; i < label.trim().length(); i++) {
						if (!Character.UnicodeBlock.of(label.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
							if (!specialChars.contains(label.subSequence(i, i + 1))) {
								flag = true;
								nonRussianChars.add(label.charAt(i));
							}
						}
					}
					if (flag) {
						logger.error("Field Label [{}] contains Non-Russian characters {}", label, nonRussianChars);
						csAssert.assertTrue(false, "Field Label [" + label + "] contains Non-Russian Characters " + nonRussianChars);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Validating Field Labels for Entity {}. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Field Labels for Entity " + entityName + ". " + e.getMessage());
		}
	}

	private List<String> getAllFieldLabelsToIgnore(String entityName) {
		List<String> labelsIgnoreList = new ArrayList<>();

		try {
			String fieldLabelsConfigFilePath = "src/test/resources/TestConfig/RBI";
			String fieldLabelsConfigFileName = "FieldLabelsMapping.cfg";

			labelsIgnoreList = ParseConfigFile.getAllPropertiesOfSection(fieldLabelsConfigFilePath, fieldLabelsConfigFileName, "labels to ignore");
		} catch (Exception e) {
			logger.error("Exception while Getting Labels to Ignore List for Entity {}. {}", entityName, e.getStackTrace());
		}
		return labelsIgnoreList;
	}

	private List<String> getAllFieldLabels(String jsonResponse) {
		List<String> allLabels;

		try {
			allLabels = ParseJsonResponse.getAllFieldLabelsOfAllTabs(jsonResponse);
		} catch (Exception e) {
			logger.error("Exception while Getting all Labels. {}", e.getMessage());
			return null;
		}
		return allLabels;
	}
}
