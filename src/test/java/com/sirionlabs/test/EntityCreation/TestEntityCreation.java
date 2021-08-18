package com.sirionlabs.test.EntityCreation;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
@Listeners(value = MyTestListenerAdapter.class)
public class TestEntityCreation extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestEntityCreation.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String delimiterForEntities = null;
	private static boolean createLocalEntity = false;
	private static boolean createGlobalEntity = false;
	private static String excelFilePath = null;
	private static String excelFileName = null;
	private static boolean useExcelSheet = false;
	private static List<String> excelHeaders = null;
	private static String dumpFilePath = null;
	private static String dumpFileName = null;
	private static Integer auditLogTabId = -1;
	private boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFileName");
		delimiterForEntities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "delimiterforentities");
		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "useexcelsheet");
		if (temp != null && temp.trim().equalsIgnoreCase("true")) {
			useExcelSheet = true;
			excelFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelfilepath");
			excelFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelfilename");
		} else {
			createLocalEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "localentitycreation"));
			createGlobalEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "globalentitycreation"));
		}

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteentity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		dumpFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dumpfilepath");
		dumpFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "dumpfileName");
		auditLogTabId = TabListDataHelper.getIdForTab("audit log");

		testCasesMap = getTestCasesMapping();
	}

	/*@DataProvider
	public Object[][] dataProviderForEntityCreation() throws ConfigurationException {
		List<Object[]> allTestData = new ArrayList<>();
		if (!useExcelSheet) {
			logger.info("Setting Entities to be Created");
			List<String> entitiesToCreate = new ArrayList<>();
			String entities[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestocreate")
					.split(Pattern.quote(delimiterForEntities));
			for (String entity : entities)
				entitiesToCreate.add(entity.trim());
			logger.info("Total Entities to be Created: {}", entitiesToCreate.size());

			if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "localentitycreation").equalsIgnoreCase("false") &&
					ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "globalentitycreation").equalsIgnoreCase("false")) {
				logger.info("Both Local Entity Creation and Global Entity Creation disabled. Hence skipping Entity Creation.");
				return null;
			}
			for (String entity : entitiesToCreate)
				allTestData.add(new Object[]{entity, null});
			return allTestData.toArray(new Object[0][]);
		} else {
			logger.info("Setting Entity to Create");
			String entities[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestocreate")
					.split(Pattern.quote(delimiterForEntities));
			String entityName = entities[0].trim();
			String excelSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelsheetname", entityName);
			excelHeaders = XLSUtils.getHeaders(excelFilePath, excelFileName, excelSheetName);
			if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "updateexcelheaders").trim().equalsIgnoreCase("true"))
				updateExcelHeaders(excelHeaders, entityName);
			List<List<String>> allExcelData = XLSUtils.getAllExcelData(excelFilePath, excelFileName, excelSheetName);
			logger.info("Total Records found for Entity {} in Excel Sheet: {}", entityName, allExcelData.size());
			List<List<String>> enabledData = this.getOnlyEnabledData(excelHeaders, allExcelData);
			for (List<String> testData : enabledData)
				allTestData.add(new Object[]{entityName, testData});
			return allTestData.toArray(new Object[0][]);
		}
	}*/

	@Test
	public void testEntityCreation() {
		CustomAssert csAssert = new CustomAssert();

		try {
			if (!useExcelSheet) {
				logger.info("Setting Entities to be Created");
				List<String> entitiesToCreate = new ArrayList<>();
				String entities[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestocreate")
						.split(Pattern.quote(delimiterForEntities));
				for (String entity : entities)
					entitiesToCreate.add(entity.trim());
				logger.info("Total Entities to be Created: {}", entitiesToCreate.size());

				if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "localentitycreation").equalsIgnoreCase("false") &&
						ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "globalentitycreation").equalsIgnoreCase("false")) {
					logger.info("Both Local Entity Creation and Global Entity Creation disabled. Hence skipping Entity Creation.");
					return;
				}

				for (String entityName : entitiesToCreate) {
					testEntityCreationOld(entityName, csAssert);
				}
			} else {
				logger.info("Setting Entity to Create");
				String entities[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestocreate")
						.split(Pattern.quote(delimiterForEntities));
				String entityName = entities[0].trim();
				String excelSheetName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelsheetname", entityName);
				excelHeaders = XLSUtils.getHeaders(excelFilePath, excelFileName, excelSheetName);
				if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "updateexcelheaders").trim().equalsIgnoreCase("true"))
					updateExcelHeaders(excelHeaders, entityName);
				List<List<String>> allExcelData = XLSUtils.getAllExcelData(excelFilePath, excelFileName, excelSheetName);
				logger.info("Total Records found for Entity {} in Excel Sheet: {}", entityName, allExcelData.size());
				List<List<String>> enabledData = this.getOnlyEnabledData(excelHeaders, allExcelData);

				for (List<String> testData : enabledData) {
					testEntityCreationExcel(entityName, testData, csAssert);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Validating Entity Creation. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Entity Creation. " + e.getMessage());
		}

		addTestResult(getTestCaseIdForMethodName("testEntityCreation"), csAssert);
		csAssert.assertAll();
	}

	private void testEntityCreationOld(String entityName, CustomAssert csAssert) {
		try {
			CreateEntity createEntityObj = new CreateEntity();
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, entityName);
			logger.info("Creating Payload for Entity {}", entityName);
			String payloadForCreate = createEntityObj.getCreatePayload(entityName, properties, createLocalEntity, createGlobalEntity);
			if (payloadForCreate != null) {
				logger.info("Hitting Create Api for Entity {}", entityName);
				Create createObj = new Create();
				createObj.hitCreate(entityName, payloadForCreate);

				String createResponse = createObj.getCreateJsonStr();

				if (!ParseJsonResponse.validJsonResponse(createResponse)) {
					FileUtils.saveResponseInFile(entityName + " Create API HTML.txt", createResponse);
				}

				JSONObject jsonObj = new JSONObject(createResponse);
				String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
				String createErrors;
				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity {} created successfully.", entityName);
					logger.info("Getting Id of the Newly Created Entity {}", entityName);
					int newEntityId = getNewEntityId(createObj.getCreateJsonStr(), entityName);

					//Verify Audit Log Entry
//					verifyAuditLogEntry(entityName, newEntityId, csAssert);

					if (deleteEntity) {
						logger.info("Getting Url Name of Entity {}", entityName);
						String urlName = EntityOperationsHelper.getUrlNameOfEntity(entityName);

						EntityOperationsHelper.deleteEntityRecord(entityName, newEntityId, urlName);
					} else {
						logger.info("Dumping Data for Entity {} and Id {}.", entityName, newEntityId);
						dumpEntityData(entityName, newEntityId);
					}
				} else {
					logger.info("Entity Creation failed due to {}", status);

					if (status.trim().equalsIgnoreCase("validationError")) {
						createErrors = jsonObj.getJSONObject("body").getJSONObject("errors").toString();
						csAssert.assertTrue(false, "Entity " + entityName + " could not be created. Errors : " + createErrors);
					} else if (status.trim().equalsIgnoreCase("applicationError")) {
						csAssert.assertTrue(false, "Entity " + entityName + " couldn't be created due to Application Error");
					}
				}
			} else {
				csAssert.assertTrue(false, "Couldn't Proceed with Creation of Entity " + entityName + " as the Payload is null.");
			}
		} catch (Exception e) {
			logger.error("Exception while Creating Entity {} using Old Approach. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Creating Entity " + entityName + " using Old Approach. " + e.getMessage());
		}
	}

	private void testEntityCreationExcel(String entityName, List<String> enabledData, CustomAssert csAssert) {
		try {
			logger.info("*******************************************************************************************************************");
			String prefix = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "columnnameprefix", entityName);
			if (prefix == null || prefix.trim().equalsIgnoreCase(""))
				prefix = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath"),
						ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "short_code");

			List<String> allHeaders = new ArrayList<>();
			allHeaders.addAll(excelHeaders);
			Map<String, String> properties = this.getPropertiesMap(entityName, allHeaders, enabledData, prefix);
			CreateEntity createEntityObj = new CreateEntity();
			logger.info("Removing Unwanted Columns from Enabled Data");
			this.removeUnwantedColumns(allHeaders, enabledData, entityName);

			logger.info("Setting ExcelFields Map for Entity {}", entityName);
			Map<String, String> excelFields = new HashMap<>();
			for (int j = 0; j < allHeaders.size(); j++)
				excelFields.put(allHeaders.get(j), enabledData.get(j));

			logger.info("Creating Payload for Entity {}", entityName);
			String payloadForCreate;
			if (entityName.trim().equalsIgnoreCase("contracts") || entityName.trim().equalsIgnoreCase("suppliers")
					|| entityName.trim().equalsIgnoreCase("purchase orders")) {
				payloadForCreate = createEntityObj.getCreatePayload(entityName, properties, true, false, excelFields, prefix);
			} else {
				payloadForCreate = createEntityObj.getCreatePayload(entityName, properties, false, true, excelFields, prefix);
			}
			if (payloadForCreate != null) {
				logger.info("Hitting Create Api for Entity {}", entityName);
				Create createObj = new Create();
				createObj.hitCreate(entityName, payloadForCreate);
				JSONObject jsonObj = new JSONObject(createObj.getCreateJsonStr());
				String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
				String createErrors;
				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity {} created successfully.", entityName);
					createErrors = "null";
				} else {
					logger.info("Entity Creation failed.");
					createErrors = jsonObj.getJSONObject("body").getJSONObject("errors").toString();
				}
				if (!status.toLowerCase().contains("success")) {
					csAssert.assertTrue(false, "Entity " + entityName + " could not be created. Errors : " + createErrors);
				}
			} else {
				csAssert.assertTrue(false, "Couldn't Proceed with Creation of Entity " + entityName + " as the Payload is null.");
			}
		} catch (Exception e) {
			logger.error("Exception while Creating Entity {} using Excel Approach. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Creating Entity " + entityName + " using Excel Approach. " + e.getMessage());
		}
	}

	private Map<String, String> getPropertiesMap(String entityName, List<String> allHeaders, List<String> enabledData, String prefix) {
		Map<String, String> properties = new HashMap<>();
		int dropDownType;
		String parentType;
		String sourceId;
		Map<String, String> optionsParametersMap = null;
		try {
			switch (entityName.trim().toLowerCase()) {
				case "contracts":
					int contractsParentTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "parentType");
					parentType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "parenttype", enabledData.get(contractsParentTypeColNo));
					properties.put("sourceentity", parentType);
					int contractsParentNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "parent");
					properties.put("sourcename", enabledData.get(contractsParentNameColNo));
					int parentEntityTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "documentType");
					properties.put("parententitytype", enabledData.get(parentEntityTypeColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(contractsParentNameColNo));
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "governance body":
					parentType = "contracts";
					properties.put("sourceentity", parentType);
					int gbContractsColNo = this.getColumnNoOfHeader(allHeaders, prefix + "contracts");
					properties.put("contractname", enabledData.get(gbContractsColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(gbContractsColNo));
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "suppliers":
					parentType = "vendors";
					properties.put("sourceentity", parentType);
					int suppliersParentNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "Parent");
					String suppliersParentName = enabledData.get(suppliersParentNameColNo);
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					optionsParametersMap = this.getOptionsParametersMap(suppliersParentName);
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "vendors":
					parentType = "client";
					properties.put("sourceentity", parentType);
					String listIdStr = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("FieldNamesConfigFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "entity_url_id");
					String clientId = null;
					ListRendererDefaultUserListMetaData defaultUserListMetaDataObj = new ListRendererDefaultUserListMetaData();
					defaultUserListMetaDataObj.hitListRendererDefaultUserListMetadata(Integer.parseInt(listIdStr), null, "{}");
					JSONObject jsonObj = new JSONObject(defaultUserListMetaDataObj.getListRendererDefaultUserListMetaDataJsonStr());
					if (jsonObj.has("popupUrl") && jsonObj.getString("popupUrl") != null) {
						String popUrl = jsonObj.getString("popupUrl");
						String tokens[] = popUrl.split("clientId=");
						if (tokens[1].contains("&")) {
							String temp[] = tokens[1].trim().split(Pattern.quote("&"));
							clientId = temp[0].trim();
						} else
							clientId = tokens[1].trim();
					}
					properties.put("sourceid", clientId);
					break;

				case "obligations":
					parentType = "contracts";
					properties.put("sourceentity", parentType);
					int obligationsSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "SourceName");
					properties.put("sourcename", enabledData.get(obligationsSourceNameColNo));
					int obligationsSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "source");
					properties.put("parententitytype", enabledData.get(obligationsSourceTypeColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(obligationsSourceNameColNo));
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "service levels":
					parentType = "contracts";
					properties.put("sourceentity", parentType);
					int slSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "SourceName");
					properties.put("sourcename", enabledData.get(slSourceNameColNo));
					int slSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "source");
					properties.put("parententitytype", enabledData.get(slSourceTypeColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(slSourceNameColNo));
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "actions":
					int actionsParentTypeColumnNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceType");
					if (enabledData.get(actionsParentTypeColumnNo).trim().equalsIgnoreCase("supplier")) {
						parentType = "suppliers";
						int actionsSupplierColNo = this.getColumnNoOfHeader(allHeaders, prefix + "supplier");
						properties.put("sourcename", enabledData.get(actionsSupplierColNo));
						properties.put("parententitytype", "supplier");
						optionsParametersMap = this.getOptionsParametersMap(enabledData.get(actionsSupplierColNo));
					} else {
						parentType = "contracts";
						int actionsSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceName");
						properties.put("sourcename", enabledData.get(actionsSourceNameColNo));
						properties.put("parententitytype", enabledData.get(actionsParentTypeColumnNo));
						optionsParametersMap = this.getOptionsParametersMap(enabledData.get(actionsSourceNameColNo));
					}
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					properties.put("sourceentity", parentType);
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "contract draft request":
					parentType = "suppliers";
					properties.put("sourceentity", parentType);
					int cdrSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "suppliers");
					properties.put("sourcename", enabledData.get(cdrSourceNameColNo));
					properties.put("sourceid", "-1");
					break;

				case "issues":
					int issuesParentTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceType");
					parentType = enabledData.get(issuesParentTypeColNo);
					if (parentType.trim().equalsIgnoreCase("supplier")) {
						parentType = "suppliers";
						int issuesSupplierColNo = this.getColumnNoOfHeader(allHeaders, prefix + "supplier");
						properties.put("sourcename", enabledData.get(issuesSupplierColNo));
						properties.put("parententitytype", "supplier");
						optionsParametersMap = this.getOptionsParametersMap(enabledData.get(issuesSupplierColNo));
					} else {
						int issuesSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceName");
						if (parentType.trim().equalsIgnoreCase("msa") || parentType.trim().equalsIgnoreCase("psa") ||
								parentType.trim().equalsIgnoreCase("sow") || parentType.trim().equalsIgnoreCase("work order")) {
							parentType = "contracts";
							properties.put("sourcename", enabledData.get(issuesSourceNameColNo));
							properties.put("parententitytype", enabledData.get(issuesParentTypeColNo));
							optionsParametersMap = this.getOptionsParametersMap(enabledData.get(issuesSourceNameColNo));
						}
					}
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					properties.put("sourceentity", parentType);
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "purchase orders":
					int poParentTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceType");
					if (enabledData.get(poParentTypeColNo).trim().equalsIgnoreCase("Supplier") ||
							enabledData.get(poParentTypeColNo).trim().equalsIgnoreCase("suppliers"))
						parentType = "suppliers";
					else
						parentType = "contracts";
					int poSouceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceName");
					properties.put("sourcename", enabledData.get(poSouceNameColNo));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(poSouceNameColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					properties.put("sourceentity", parentType);
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "change requests":
					int crParentTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceType");
					parentType = enabledData.get(crParentTypeColNo);
					if (parentType.trim().equalsIgnoreCase("supplier")) {
						parentType = "suppliers";
						int crSupplierColNo = this.getColumnNoOfHeader(allHeaders, prefix + "supplier");
						properties.put("sourcename", enabledData.get(crSupplierColNo));
						properties.put("parententitytype", "supplier");
						optionsParametersMap = this.getOptionsParametersMap(enabledData.get(crSupplierColNo));
					} else {
						int crSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceName");
						if (parentType.trim().equalsIgnoreCase("msa") || parentType.trim().equalsIgnoreCase("psa") ||
								parentType.trim().equalsIgnoreCase("sow") || parentType.trim().equalsIgnoreCase("work order")) {
							parentType = "contracts";
							properties.put("sourcename", enabledData.get(crSourceNameColNo));
							properties.put("parententitytype", enabledData.get(crParentTypeColNo));
							optionsParametersMap = this.getOptionsParametersMap(enabledData.get(crSourceNameColNo));
						}
					}
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					properties.put("sourceentity", parentType);
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;

				case "work order requests":
					parentType = "contracts";
					properties.put("sourceentity", parentType);
					int worSourceNameColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceName");
					properties.put("sourcename", enabledData.get(worSourceNameColNo));
					int worSourceTypeColNo = this.getColumnNoOfHeader(allHeaders, prefix + "sourceType");
					properties.put("parententitytype", enabledData.get(worSourceTypeColNo));
					optionsParametersMap = this.getOptionsParametersMap(enabledData.get(worSourceNameColNo));
					dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
							ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", parentType));
					sourceId = this.getSourceId(dropDownType, optionsParametersMap, parentType);
					properties.put("sourceid", sourceId);
					break;
			}
		} catch (Exception e) {
			logger.error("Exception while setting Properties Map for Entity {}. {}", entityName, e.getStackTrace());
		}
		return properties;
	}

	private Map<String, String> getOptionsParametersMap(String query) throws ConfigurationException {
		Map<String, String> optionsParameters = new HashMap<>();
		String pageType = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "pagetype", "listdata");
		optionsParameters.put("pageType", pageType);
		optionsParameters.put("query", query);
		return optionsParameters;
	}

	//Removes all the Unwanted Columns/Fields from the Enabled Data
	private void removeUnwantedColumns(List<String> excelHeaders, List<String> enabledData, String entityName) throws ConfigurationException {
		String unwantedColumns[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelcolumnsignorelist",
				entityName).split(Pattern.quote(","));

		for (String column : unwantedColumns) {
			for (int j = 0; j < excelHeaders.size(); j++) {
				if (excelHeaders.get(j).trim().equalsIgnoreCase(column.trim())) {
					excelHeaders.remove(j);
					enabledData.remove(j);
					break;
				}
			}
		}
	}

	private List<List<String>> getOnlyEnabledData(List<String> excelHeaders, List<List<String>> allExcelData) throws ConfigurationException {
		List<List<String>> enabledData = new ArrayList<>();
		try {
			String enableColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "enabletestcolumnname");
			if (enableColumnName == null)
				enableColumnName = "Runmode";
			int columnNo = -1;

			for (int i = 0; i < excelHeaders.size(); i++) {
				if (excelHeaders.get(i).trim().equalsIgnoreCase(enableColumnName.trim())) {
					columnNo = i;
					break;
				}
			}
			for (List<String> recordData : allExcelData) {
				if (recordData.get(columnNo).trim().equalsIgnoreCase("y"))
					enabledData.add(recordData);
			}
		} catch (Exception e) {
			logger.error("Exception while getting only enabled data. {}", e.getMessage());
		}
		return enabledData;
	}

	private int getColumnNoOfHeader(List<String> excelHeaders, String columnName) {
		int columnNo = -1;
		for (int i = 0; i < excelHeaders.size(); i++) {
			if (excelHeaders.get(i).trim().equalsIgnoreCase(columnName)) {
				columnNo = i;
				break;
			}
		}
		return columnNo;
	}

	private String getSourceId(int dropDownType, Map<String, String> optionsParametersMap, String parentType) {
		String sourceId;
		Options optionObj = new Options();
		optionObj.hitOptions(dropDownType, optionsParametersMap);
		sourceId = optionObj.getIds();
		if (sourceId != null && sourceId.contains(",")) {
			String ids[] = sourceId.split(Pattern.quote(","));
			sourceId = ids[0].trim();
			String name = Options.getNameFromId(optionObj.getOptionsJsonStr(), Integer.parseInt(ids[0].trim()));
			logger.warn("Multiple records found for Parent Entity {}. Hence selecting first record having Name \"{}\"", parentType, name);
		}
		return sourceId;
	}

	private void updateExcelHeaders(List<String> excelHeaders, String entityName) throws ConfigurationException {
		String excelHeadersMappingFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelheadersmappingfilepath");
		String excelHeadersMappingFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "excelheadersmappingfilename");
		Map<String, String> allHeadersMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(excelHeadersMappingFilePath, excelHeadersMappingFileName, entityName);
		List<String> allHeadersKeys = new ArrayList<>(allHeadersMap.keySet());
		for (String key : allHeadersKeys) {
			if (excelHeaders.contains(key)) {
				int keyIndex = excelHeaders.indexOf(key);
				excelHeaders.set(keyIndex, allHeadersMap.get(key));
			}
		}
	}

	private int getNewEntityId(String createJsonStr, String entityName) {
		int newEntityId = -1;
		try {
			JSONObject jsonObj = new JSONObject(createJsonStr);
			String notificationStr = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");

			String temp[] = notificationStr.trim().split(Pattern.quote("show/"));
			if (temp.length > 1) {
				String temp2 = temp[1];
				String temp3[] = temp2.trim().split(Pattern.quote("\""));
				if (temp3.length > 1) {
					String temp4 = temp3[0];
					String temp5[] = temp4.trim().split(Pattern.quote("/"));
					if (temp5.length > 1)
						newEntityId = Integer.parseInt(temp5[1]);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Entity Id of Newly Created Entity {}. {}", entityName, e.getMessage());
		}
		return newEntityId;
	}

	private void dumpEntityData(String entityName, int entityId) {
		try {
			FileWriter writer = new FileWriter(dumpFilePath + "/" + dumpFileName, true);
			BufferedWriter bufferedWriter = new BufferedWriter(writer);

			bufferedWriter.write(entityName + " -> " + entityId);
			bufferedWriter.newLine();
			bufferedWriter.close();
		} catch (Exception e) {
			logger.error("Exception while dumping data for Entity {} and Id {}. {}", entityName, entityId, e.getStackTrace());
		}
	}

	private void verifyAuditLogEntry(String entityName, int recordId, CustomAssert csAssert) {
		try {
			logger.info("Verifying Audit Log Entry for Entity {} and Record Id {}.", entityName, recordId);
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			TabListData tabListObj = new TabListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
			String tabListDataResponse = tabListObj.hitTabListData(auditLogTabId, entityTypeId, recordId, payload);

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				JSONObject jsonObj = new JSONObject(tabListDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				if (jsonArr.length() > 0) {
					jsonObj = jsonArr.getJSONObject(0);
					String columnIdForActionName = TabListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "action_name");
					String actualValue = jsonObj.getJSONObject(columnIdForActionName).getString("value");
					String expectedValue = "Newly Created";

					if (entityName.trim().equalsIgnoreCase("change requests"))
						expectedValue = "Change Request Initiated";
					else if (entityName.trim().equalsIgnoreCase("vendors"))
						expectedValue = "Saved";

					logger.info("Audit Log Entry for Entity {}, Record Id {} and Audit Log Tab. Actual Value: {} and Expected Value: {}", entityName,
							recordId, actualValue, expectedValue);

					if (actualValue == null || !actualValue.trim().equalsIgnoreCase(expectedValue)) {
						csAssert.assertTrue(false, "Couldn't find " + expectedValue + " Entry in Audit Log Tab API Response for Entity " + entityName +
								" and Record " + recordId);
					}
				} else {
					csAssert.assertTrue(false, "Couldn't find any data in TabListData API Response for Entity " + entityName + ", Record Id " +
							recordId + " and Audit Log Tab");
				}
			} else {
				csAssert.assertTrue(false, "TabListData API Response for Entity " + entityName + ", Record Id " + recordId +
						" and Audit log Tab is an Invalid JSON.");
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Audit Log Entry for Entity " + entityName + " and Record Id " + recordId + ". "
					+ e.getMessage());
		}
	}
}