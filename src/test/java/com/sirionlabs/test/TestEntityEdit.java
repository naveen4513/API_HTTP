package com.sirionlabs.test;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestEntityEdit {

	private final static Logger logger = LoggerFactory.getLogger(TestEntityEdit.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String extraFieldsConfigFilePath;
	private String extraFieldsConfigFileName;
	private Integer defaultNoOfFieldsToEdit = 1;

//	private EntityEditHelper editHelperObj = new EntityEditHelper();

	private Map<String, String> entityCommonExtraFieldsMap;
	private Map<String, String> defaultProperties;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityEditConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityEditConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultNoOfFieldsToEdit");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			defaultNoOfFieldsToEdit = Integer.parseInt(temp.trim());

		entityCommonExtraFieldsMap = ParseConfigFile.getAllConstantProperties(extraFieldsConfigFilePath, extraFieldsConfigFileName,
				"entity common extra fields map");

		defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);
	}

	@DataProvider(parallel = true)
	public Object[][] dataProviderForEntityEdit() {
		List<Object[]> allTestData = new ArrayList<>();
		try {
			List<String> flowsToTest = new ArrayList<>();

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
			if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
				logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
				flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
			} else {
				String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
				for (String flow : allFlows) {
					if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
						flowsToTest.add(flow.trim());
					} else {
						logger.info("Flow having name [{}] not found in Entity Edit Config File.", flow.trim());
					}
				}
			}

			flowsToTest.remove("fieldstoignoremapping");
			flowsToTest.remove("default");

			for (String flow : flowsToTest) {
				allTestData.add(new Object[]{flow.trim()});
			}

			logger.info("Total Flows to Test : {}", allTestData.size());
		} catch (Exception e) {
			logger.error("Exception while Setting all flows to test for Entity Edit. {}", e.getMessage());
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForEntityEdit")
	public void testEntityEdit(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			EntityEditHelper editHelperObj = new EntityEditHelper();

			logger.info("Validating Entity Edit for Flow [{}]", flowToTest);
			Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

			String entityName = flowProperties.get("entity");
			int recordId = Integer.parseInt(flowProperties.get("entityid"));
			String expectedResult = flowProperties.get("expectedresult");

			Map<String, String> extraFields = getExtraFields(entityName, flowToTest);
			Map<String, String> fieldsToIgnoreMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "fieldsToIgnoreMapping");

			boolean negativeCase = expectedResult.trim().equalsIgnoreCase("failure");

			editHelperObj.validateEntityEdit(entityName, recordId, defaultProperties, extraFields, fieldsToIgnoreMap, defaultNoOfFieldsToEdit, negativeCase, csAssert);
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Entity Edit. " + e.getMessage());
		}

		csAssert.assertAll();
	}

	private Map<String, String> getExtraFields(String entityName, String flowToTest) {
		Map<String, String> extraFields = new HashMap<>();
		try {
			if (entityCommonExtraFieldsMap.containsKey(entityName.trim())) {
				logger.info("Getting Common Extra Fields for Entity {}", entityName);
				String commonExtraFieldsSectionName = entityCommonExtraFieldsMap.get(entityName.trim());
				Map<String, String> commonExtraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName,
						commonExtraFieldsSectionName);

				extraFields.putAll(commonExtraFields);
			}

			logger.info("Getting Extra Fields for Flow [{}].", flowToTest);
			Map<String, String> flowSpecificExtraFields = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);
			extraFields.putAll(flowSpecificExtraFields);

		} catch (Exception e) {
			logger.error("Exception while getting Extra Fields for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return extraFields;
	}
}