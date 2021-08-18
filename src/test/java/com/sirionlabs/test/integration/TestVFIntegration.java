package com.sirionlabs.test.integration;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.integration.ContractPortingForm;
import com.sirionlabs.api.integration.Create;
import com.sirionlabs.api.integration.IntegrationListData;
import com.sirionlabs.api.integration.ReprocessXml;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestVFIntegration {

	private final static Logger logger = LoggerFactory.getLogger(TestVFIntegration.class);
	private String vfIntegrationConfigFilePath;
	private String vfIntegrationConfigFileName;
	private String vfIntegrationFieldMappingFilePath;
	private String vfIntegrationFieldMappingFileName;
	private String vfIntegrationFieldXPathMappingFilePath;
	private String vfIntegrationFieldXPathMappingFileName;
	private List<String> allSection;
	private Boolean validateAllSections;
	private Boolean isAutoPortingEnabled;
	private String[] sectionsToValidate;
	private String stagingEntityListIdForContract;
	private String sftpHost;
	private Integer sftpPort;
	private String sftpUsername;
	private String sftpPassword;
	private String sftpTargetDir;
	Integer waitBeforeStaging;
	Set<String> processedContractIdList;
	List<File> uploadedXmlFiles;
	List<String> multiSelectFields;
	List<String> indirectMappedFields;
	List<String> multiSelectDynamicFields;
	List<String> singleSelectDynamicFields;
	List<String> dynamicTableFields;
	List<String> stakeholderFields;
	List<String> dateFields;
	List<String> fieldsWithXpathDefined;
	private List<String> expectedStakeholderValues;
	private String dateFormat;
	private List<String> dynamicFields;
	private String parentContractIdXpathSectionName;
	private String parentContractId;
	private String reprocessingContractId;
	private boolean runOnDifferentEnv;
	private String environmentFileName;
	private String oldEnvironmentFileName;


	@BeforeClass
	public void setConfig() {
		try {
			vfIntegrationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationConfigFilePath");
			vfIntegrationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationConfigFileName");

			vfIntegrationFieldMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationFieldMappingFilePath");
			vfIntegrationFieldMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationFieldMappingFileName");

			vfIntegrationFieldXPathMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationFieldXPathMappingFilePath");
			vfIntegrationFieldXPathMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("VFIntegrationFieldXPathMappingFileName");
			fieldsWithXpathDefined = ParseConfigFile.getAllSectionNames(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName);

			validateAllSections = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "validateallsections"));
			allSection = ParseConfigFile.getAllSectionNames(vfIntegrationConfigFilePath, vfIntegrationConfigFileName);
			sectionsToValidate = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sectionstovalidate").split(",");

			isAutoPortingEnabled = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "isautoportingenabled"));
			stagingEntityListIdForContract = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "stagingEntityListIdForContract");
			waitBeforeStaging = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "waitBeforeStaging"));
			dateFormat = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "dateFormat");

			sftpHost = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sftphost");
			sftpPort = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sftpport"));
			sftpUsername = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sftpusername");
			sftpPassword = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sftppassword");
			sftpTargetDir = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "sftptargetdir");

			parentContractIdXpathSectionName = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "parentcontractidxpathsectionname");
			parentContractId = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "parentcontractid");

			multiSelectFields = getPropertyList("multiselectfields");
			indirectMappedFields = getPropertyList("indirectMappedFields");
			dynamicFields = getPropertyList("dynamicFields");
			multiSelectDynamicFields = getPropertyList("multiSelectDynamicFields");
			singleSelectDynamicFields = getPropertyList("singleSelectDynamicFields");
			dynamicTableFields = getPropertyList("dynamicTableFields");
			stakeholderFields = getPropertyList("stakeholderFields");
			dateFields = getPropertyList("datefields");
			expectedStakeholderValues = getPropertyList("expectedstakeholdervalue");

			runOnDifferentEnv = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "runondifferentenv"));
			environmentFileName = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "separateEnvName");

			if (runOnDifferentEnv) {
				loggingOnSeparateEnv(environmentFileName);
			}

		} catch (Exception e) {
			logger.error("Exception occurred while setting config properties for vf integration test. {}", e.getMessage());
			e.printStackTrace();
		}
	}

	@DataProvider(name = "getAllIntegrationSection", parallel = false)
	public Object[][] getAllIntegrationSection() throws ConfigurationException {

		int i = 0;
		Object[][] groupArray = null;
		List<String> sections = new ArrayList<>();
		if (validateAllSections) {
			groupArray = new Object[allSection.size()][];
			sections = allSection;
		} else {
			groupArray = new Object[sectionsToValidate.length][];
			for (String temp : sectionsToValidate) {
				sections.add(temp);
			}
		}

		for (String entry : sections) {
			groupArray[i] = new Object[1];
			groupArray[i][0] = entry; // sectionName
			i++;
		}

		return groupArray;
	}

	@Test(dataProvider = "getAllIntegrationSection", enabled = true)
	public void testVFIntegrationFlow(String integrationFlow) {
		CustomAssert csAssert = new CustomAssert();
		processedContractIdList = new LinkedHashSet<>();
		uploadedXmlFiles = new ArrayList<>();
		reprocessingContractId = null;

		try {
			logger.info("***************########## VALIDATION STARTED FOR INTEGRATION FLOW : {} ###############*************", integrationFlow);

			if (integrationFlow.contains("reprocessing") && isAutoPortingEnabled) {
				logger.info("contract reprocessing is scheduler dependent. Scheduler dependency is for 6 hours in case of auto-porting enabled. Hence skipping the flow : {}", integrationFlow);
			} else {
				Boolean isTestDataUpdatedSuccessfully = updateTestData(integrationFlow, csAssert);
				if (isTestDataUpdatedSuccessfully) {
					logger.info("******************  Test Data updated successfully. Processed contracts client primary key in xml : {}.  Now Uploading on SFTP server. ************", processedContractIdList);

					Boolean isTestDataUploadedSuccessfully = uploadOnSFTPServer(integrationFlow, csAssert);
					if (isTestDataUploadedSuccessfully) {
						logger.info("******************  Test Data uploaded successfully on SFTP server.  Now Validating on staging. ************");

						waitForSchedulerToProcessFiles();

						Boolean isContractAvailableOnStaging = validateContractCreationOnStaging();
						if (isContractAvailableOnStaging) {
							logger.info("********************  All Contracts are available on staging. Contracts id : {}", processedContractIdList);

							if (integrationFlow.contains("parked") || integrationFlow.contains("parentmissing")) {
								String expectedStatus = null;

								if (integrationFlow.contains("parked"))
									expectedStatus = "Parked";
								else
									expectedStatus = "Missing Parent";

								Boolean isStatusPassed = validateContractForStatus(integrationFlow, expectedStatus);
								if (!isStatusPassed) {
									logger.error("Contract staging status is not matched with the expected status : {}, processedContractIdList : {}", expectedStatus, processedContractIdList);
									csAssert.assertTrue(false, "Contract staging status is not matched with the expected status : " + expectedStatus + ", processedContractIdList : " + processedContractIdList + " , flow : " + integrationFlow);
								} else
									logger.info("Contract staging status is matched successfully with the expected status : {}, processedContractIdList : {}", expectedStatus, processedContractIdList);
							} else {
								Boolean isPortingCompleted = true;
							/*main application validation with auto porting enable/disable feature*/
								if (!isAutoPortingEnabled) {
									logger.info("***************  AutoPorting is disabled. Hence porting the contracts exclusively ******************");
									String stagingIdOfReprocessingContract = null;
									Map<String, String> contractIdAndStagingIdMap = getContractIdAndStagingIdMap();
									for (Map.Entry<String, String> entryMap : contractIdAndStagingIdMap.entrySet()) {
										if (reprocessingContractId != null && entryMap.getKey().equalsIgnoreCase(reprocessingContractId)) {
											stagingIdOfReprocessingContract = entryMap.getValue();
											continue;
										}
										isPortingCompleted = portContractsToMainApp(entryMap.getValue());
										if (!isPortingCompleted) {
											logger.error("porting failed for contract Id : {}, stagingId : {}", entryMap.getKey(), entryMap.getValue());
											break;
										}
									}
									if (isPortingCompleted && integrationFlow.contains("reprocessing")) {
									/*reprocessing and porting contract to main app*/
										ReprocessXml reprocessXml = new ReprocessXml();
										reprocessXml.hitGetReprocessXml(stagingIdOfReprocessingContract);
										String reprocessXmlResponse = reprocessXml.getReprocessXmlJsonStr();

										if (APIUtils.validJsonResponse(reprocessXmlResponse, "reprocessXmlResponse")) {
											JSONObject jsonObject = new JSONObject(reprocessXmlResponse);
											String message = jsonObject.getString("message");
											if (message.equalsIgnoreCase("updated successfully")) {
												logger.info("contract reprocessed successfully. contract id : {}", reprocessingContractId);
												portContractsToMainApp(stagingIdOfReprocessingContract);
											} else {
												isPortingCompleted = false;
												logger.error("contract reprocessing failed. reprocessXml api response : {}", reprocessXmlResponse);
												csAssert.assertTrue(false, "contract reprocessing failed. reprocessXml api response : " + reprocessXmlResponse);
											}

										} else {
											isPortingCompleted = false;
											logger.error("reprocessXml api response is not valid json. response : {}", reprocessXmlResponse);
											csAssert.assertTrue(false, "reprocessXml api response is not valid json. contractId : " + reprocessingContractId);
										}
									}
								}

								if (isPortingCompleted) {
									String expectedStatus = "Porting Completed";
									Boolean isStatusPassed = validateContractForStatus(integrationFlow, expectedStatus);
									if (!isStatusPassed) {
										logger.error("Contract staging status is not matched with the expected status : {}, processedContractIdList : {}", expectedStatus, processedContractIdList);
										csAssert.assertTrue(false, "Contract staging status is not matched with the expected status : " + expectedStatus + ", processedContractIdList : " + processedContractIdList + ", flow : " + integrationFlow);
									} else {
										List<String> contractDbIdsToBeValidated = getContractDbIdsToBeValidated();
										logger.info("*********************  Porting is completed. Validation started on Main Application. Contracts dbIds to be validated : {} ******************", contractDbIdsToBeValidated);

										for (int i = 0; i < contractDbIdsToBeValidated.size(); i++) {
											validateContractOnMainApplication(contractDbIdsToBeValidated.get(i), uploadedXmlFiles.get(contractDbIdsToBeValidated.size() - (i + 1)), csAssert, integrationFlow);
										}
									}
								} else {
									logger.error("Porting not completed for flow : {}", integrationFlow);
									csAssert.assertTrue(false, "Porting not completed for flow : " + integrationFlow);
								}
							}
						} else {
							logger.error("Migrated Contracts are not found on staging. processedContractIdList = {}", processedContractIdList);
							csAssert.assertTrue(false, "Migrated Contracts are not found on staging. processedContractIdList = " + processedContractIdList + ", " + integrationFlow);
						}
					} else {
						logger.error("Unable to upload test data for flow : {}. Hence terminating validation for this flow.", integrationFlow);
						csAssert.assertTrue(false, "Unable to upload test data for flow : " + integrationFlow + ". Hence terminating validation for this flow.");
					}
				} else {
					logger.error("Unable to update test data for flow : {}. Hence terminating validation for this flow.", integrationFlow);
					csAssert.assertTrue(false, "Unable to update test data for flow : " + integrationFlow + ". Hence terminating validation for this flow.");
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception occurred while validating vf integration flow: " + integrationFlow + ", error: " + e.getMessage());
			logger.error("Exception occurred while validating vf integration flow: {}, error: {}", integrationFlow, e.getMessage());
			e.printStackTrace();
		}
		csAssert.assertAll();
	}

	private Boolean validateContractForStatus(String integrationFlow, String expectedStatus) {
		Boolean isStatusPassed = true;
		logger.info("validating contract status on staging. Flow : {}, expected Status : {}", integrationFlow, expectedStatus);
		try {
			String payload = "{\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response, "integration listing")) {
				Integer clientPrimaryKeyColumnId = integrationListData.getColumnIdFromColumnName("stagingClientPrimaryKey");
				List<String> stagedContractIdList = integrationListData.getAllRecordForParticularColumns(clientPrimaryKeyColumnId);

				Integer stagingStatusColumnId = integrationListData.getColumnIdFromColumnName("stagingStatus");
				List<String> stagingStatusValueList = integrationListData.getAllRecordForParticularColumns(stagingStatusColumnId);

				Map<String, String> contractIdAndStatusMap = new HashMap<>();
				for (int i = 0; i < stagedContractIdList.size(); i++) {
					contractIdAndStatusMap.put(stagedContractIdList.get(i), stagingStatusValueList.get(i));
				}

				for (String contractId : processedContractIdList) {
					if (!contractIdAndStatusMap.get(contractId).equalsIgnoreCase(expectedStatus)) {
						logger.error("Status not matched for contract id : {}. Actual : {} but expected : {}", contractId, contractIdAndStatusMap.get(contractId), expectedStatus);
						isStatusPassed = false;
						break;
					}
				}
			} else {
				logger.error("Integration listing api response is not valid json");
				isStatusPassed = false;
			}
		} catch (Exception e) {
			isStatusPassed = false;
			logger.error("Exception while validating contract status on staging. expected status : {}, error : {}", expectedStatus, e.getMessage());
		}
		return isStatusPassed;
	}

	private List<String> getContractDbIdsToBeValidated() {
		List<String> contractDbIdsToBeValidated = new ArrayList<>();
		try {
			Map<String, String> stagingContractIdAndUiIdMap = getStagingContractIdAndUiIdMap();
			Map<String, String> mainAppContractUiIdAndDbIdMap = getMainAppContractUiIdAndDbIdMap();

			for (Map.Entry<String, String> entry : stagingContractIdAndUiIdMap.entrySet()) {
				if (mainAppContractUiIdAndDbIdMap.containsKey(entry.getValue())) {
					contractDbIdsToBeValidated.add(mainAppContractUiIdAndDbIdMap.get(entry.getValue()));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting ContractDbIdsToBeValidated. error :{} ", e.getMessage());
			e.printStackTrace();
		}
		return contractDbIdsToBeValidated;
	}

	private Boolean portContractsToMainApp(String contractStagingId) {
		Boolean isPortingSuccess = true;

		ContractPortingForm contractPortingForm = new ContractPortingForm();
		contractPortingForm.hitGetContractPortingForm(contractStagingId);
		String response = contractPortingForm.getContractPortingFormJsonStr();

		if (APIUtils.validJsonResponse(response, "contractPortingForm")) {
			JSONObject jsonResponse = new JSONObject(response);
			JSONObject data = jsonResponse.getJSONObject("body").getJSONObject("data");
			String payloadForCreateApi = "{\"body\":{\"data\":" + data.toString() + "}}";

			Create create = new Create();
			create.hitCreate("contracts", payloadForCreateApi);
			String createResponse = create.getCreateJsonStr();

			if (APIUtils.validJsonResponse(createResponse, "integration create api")) {
				JSONObject jsonObject = new JSONObject(createResponse);
				String status = jsonObject.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success"))
					logger.info("Contract with staging id : {} is ported successfully.", contractStagingId);
				else {
					isPortingSuccess = false;
					logger.error("Contract with staging id : {} is not ported successfully.", contractStagingId);
				}
			} else {
				isPortingSuccess = false;
				logger.error("create api response for contract with staging id : {} is not valid json. response : {}", contractStagingId, createResponse);
			}
		} else {
			isPortingSuccess = false;
			logger.error("GetContractPortingForm api response for contract with staging id : {} is not valid json. response : {}", contractStagingId, response);
		}
		return isPortingSuccess;
	}

	private Boolean validateContractOnMainApplication(String contractDbId, File xmlFile, CustomAssert csAssert, String integrationFlow) {

		Boolean isValidationPassed = true;
		try {
			logger.info("*************************************************  Validating fields for flow : {} , contract(DB-ID) : {} with xml : {} ****************************************", integrationFlow, contractDbId, xmlFile.getName());

			/*Getting show page mapping*/
			String showPageResponse = getShowResponse(contractDbId);
			Map<String, String> appPropertiesMapping = getAppPropertiesMapping();

			int count = 0;
			for (Map.Entry<String, String> entry : appPropertiesMapping.entrySet()) {

				count++;
				Boolean fieldValidation = true;
				String fieldName = entry.getKey();
				String fieldMappingOnShowPage = entry.getValue().split("::")[1].trim();
				String uiLabel = entry.getValue().split("::")[0].trim();

				logger.info("Validating field name : {}, fieldName mapping on show page : {}, UI label : {}, integrationFlow : {}", fieldName, fieldMappingOnShowPage, uiLabel, integrationFlow);

				if (fieldsWithXpathDefined.contains(fieldName.toLowerCase())) {

					/*Getting actual values from show page response*/
					List<String> actualFieldValues = getActualFieldValues(showPageResponse, contractDbId, fieldName, fieldMappingOnShowPage);
					logger.info("Actual values for the field : {},showPageMappingName : {}  ==> {}", fieldName, fieldMappingOnShowPage, actualFieldValues);

					/*Getting expected values from xml data file*/
					List<String> expectedFieldValues = getExpectedFieldValues(fieldName, fieldMappingOnShowPage, xmlFile);
					logger.info("Expected values for the field : {},showPageMappingName : {}  ==> {}", fieldName, fieldMappingOnShowPage, expectedFieldValues);

					if (dateFields.contains(fieldName) || dateFields.contains(fieldMappingOnShowPage)) {
						SimpleDateFormat actualValueFormat = new SimpleDateFormat(dateFormat);
						SimpleDateFormat expectedValueFormat = new SimpleDateFormat("yyyy-MM-dd");

						List<String> temp = new ArrayList<>();
						for (int i = 0; i < actualFieldValues.size(); i++) {
							String actualDate = null;
							if (dateFormat.equalsIgnoreCase("mmm-dd-yyyy")) {
								actualDate = actualFieldValues.get(i).substring(0, 11);
							} else
								actualDate = actualFieldValues.get(i).substring(0, 10); // date format = 02-27-2012 00:00:00 or 08-24-2016

							temp.add(actualDate);
						}
						actualFieldValues = temp; // getting all the dates in given date format

						List<String> expDateList = new ArrayList<>();
						for (int i = 0; i < expectedFieldValues.size(); i++) {
							String expDate = expectedFieldValues.get(i).substring(0, 10); // date format = 2016-08-24T00:00:00
							Date date = expectedValueFormat.parse(expDate);
							expDateList.add(actualValueFormat.format(date).toString());
						}
						expectedFieldValues = expDateList; // getting all the dates in given date format
					}

					/*field data validation with actual and expected values*/
					for (String expectedValue : expectedFieldValues) {
						if (stakeholderFields.contains(fieldName) || stakeholderFields.contains(fieldMappingOnShowPage)) {
							if (actualFieldValues.size() == 0) {
								isValidationPassed = false;
								fieldValidation = false;
							} else {
								for (String actualValue : actualFieldValues) {
									if (!expectedFieldValues.contains(actualValue)) {
										isValidationPassed = false;
										fieldValidation = false;
									}
								}
							}
						} else {
							if (!actualFieldValues.contains(expectedValue)) {
								isValidationPassed = false;
								fieldValidation = false;
							}
						}
					}
					if (fieldValidation) {
						logger.info("*********************** fieldCount : {}, Field validation passed for integrationFlow : {} , fieldName : {}[{}], actualValue : {} and expected : {} **********************", count, integrationFlow, fieldName, fieldMappingOnShowPage, actualFieldValues, expectedFieldValues);
					} else {
						logger.error("*********************** fieldCount : {}, Field validation failed for integrationFlow : {} ,  fieldName : {}[{}], actualValue : {} but expected : {} **********************", count, integrationFlow, fieldName, fieldMappingOnShowPage, actualFieldValues, expectedFieldValues);
						csAssert.assertTrue(false, "** fieldCount : " + count + ", Field validation failed for flow : " + integrationFlow + " , fieldName : " + fieldName + "[{" + fieldMappingOnShowPage + "}], actualValue : " + actualFieldValues + " but expected : " + expectedFieldValues + " **\n");
					}
				} else {
					logger.error("Xpath is not found for field : {}, showPageMappedFieldName : {}", fieldName, fieldMappingOnShowPage);
					csAssert.assertTrue(false, "Xpath is not found for field : " + fieldName + ", showPageMappedFieldName : " + fieldMappingOnShowPage);
					isValidationPassed = false;
				}
			}
			logger.info("########### Validation completed. Total fields validated = {} for dbId : {} ############", appPropertiesMapping.size(), contractDbId);
		} catch (Exception e) {
			isValidationPassed = false;
			csAssert.assertTrue(false, "Exception occurred while validating fields on show page for flow : " + integrationFlow + ", error : " + e.getMessage());
			logger.error("Exception while validating show page. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return isValidationPassed;
	}

	private Map<String, String> getAppPropertiesMapping() {
		Map<String, String> properties = new LinkedHashMap<>();

		try {
			Boolean validateAllAppPropertiesFields = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "validateallapppropertiesfields"));
			List<String> fieldsToValidate = getPropertyList("propertyfieldstovalidate");

			if (validateAllAppPropertiesFields)
				properties = ParseConfigFile.getAllConstantProperties(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "default");
			else {
				for (String fieldName : fieldsToValidate) {
					if (ParseConfigFile.hasProperty(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "default", fieldName.toLowerCase().trim())) {
						String value = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "default", fieldName.toLowerCase().trim());

						properties.put(fieldName, value);
					}
				}
			}
		} catch (ConfigurationException e) {
			logger.error("Exception while getting appProperties. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return properties;
	}

	private List<String> getExpectedFieldValues(String fieldName, String fieldMappingOnShowPage, File xmlFile) {
		List<String> expectedFieldValues = new ArrayList<>();

		try {
			if (indirectMappedFields.contains(fieldName) || indirectMappedFields.contains(fieldMappingOnShowPage)) {

				if (ParseConfigFile.hasProperty(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "logical fields", fieldMappingOnShowPage)) {
					String dependentFields = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "logical fields", fieldMappingOnShowPage);
					String[] temp = dependentFields.split(",");

					String expectedValue = "";
					int fieldCount = 1;
					for (String dependentField : temp) {
						String xPath = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, dependentField, "xpath");
						String elementName = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, dependentField, "elementName");
						String fieldXpath = xPath + "/" + elementName;
						Node fieldNode = XMLUtils.getNode(xmlFile, fieldXpath);

						String fieldValueInXml = XMLUtils.getValue(fieldNode);

						if (fieldMappingOnShowPage.equalsIgnoreCase("documentType")) {
							String[] msaMapping = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "documenttype", "msa").split(",");
							String[] sowMapping = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "documenttype", "sow").split(",");

							List msaMappingList = Arrays.asList(msaMapping);
							List sowMappingList = Arrays.asList(sowMapping);

							if (msaMappingList.contains(fieldValueInXml))
								expectedValue = "MSA";
							else if (sowMappingList.contains(fieldValueInXml))
								expectedValue = "SOW";
							else
								expectedValue = "MSA";
						} else if (fieldMappingOnShowPage.equalsIgnoreCase("productWarrantiesPresent")) {
							if (fieldCount == temp.length)
								expectedValue += fieldValueInXml;
							else
								expectedValue += fieldValueInXml + " ";
						} else if (fieldName.contains("attachment")) {
							if (fieldName.equalsIgnoreCase("contractdocumentattachment")) {
									/* logic ==> contract-document-name[attachment-name -version- date-added];  eg. DocumentTree1[msat -2- 20121213]*/
								if (fieldCount == 1)
									expectedValue += fieldValueInXml;
								if (fieldCount == 2)
									expectedValue = expectedValue + "[" + fieldValueInXml + " -";
								if (fieldCount == 3)
									expectedValue = expectedValue + fieldValueInXml + "- ";
								if (fieldCount == 4) {
									fieldValueInXml = fieldValueInXml.replaceAll("-", "");
									int startingIndexOfTimeStamp = fieldValueInXml.indexOf("T");
									fieldValueInXml = fieldValueInXml.substring(0, startingIndexOfTimeStamp);
									expectedValue = expectedValue + fieldValueInXml + "];";
								}

							} else {
										/*logic ==> attachment-name [date-added] eg. pba [20120913] ;*/
								if (fieldCount == 1)
									expectedValue += fieldValueInXml + " ";
								if (fieldCount == 2) {
									fieldValueInXml = fieldValueInXml.replaceAll("-", "");
									int startingIndexOfTimeStamp = fieldValueInXml.indexOf("T");
									fieldValueInXml = fieldValueInXml.substring(0, startingIndexOfTimeStamp);
									expectedValue = expectedValue + "[" + fieldValueInXml + "] ;";
								}
							}
						} else if (fieldMappingOnShowPage.equalsIgnoreCase("termType")) {
							if (elementName.equalsIgnoreCase("auto-renewal")) {
								if (fieldValueInXml.equalsIgnoreCase("true")) {
									String value = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "valuewhenautorenewalistrue");
									expectedFieldValues.add(value);
									return expectedFieldValues;
								} else
									continue;
							}
							if (elementName.equalsIgnoreCase("perpetual-term")) {
								if (fieldValueInXml.equalsIgnoreCase("true")) {
									String value = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "valuewhenperpetualtermistrue");
									expectedFieldValues.add(value);
									return expectedFieldValues;
								} else {
									/*when autorenewal = false and perpetualterm = false*/
									String value = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, "valuewhenbotharefalse");
									expectedFieldValues.add(value);
									return expectedFieldValues;
								}
							}
						}
						fieldCount++;
					}
					expectedFieldValues.add(expectedValue);
				}

			} else if (stakeholderFields.contains(fieldName) || stakeholderFields.contains(fieldMappingOnShowPage)) {
				expectedFieldValues = expectedStakeholderValues;
			} else {
						/*Direct mapped fields*/
				String xPath = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, fieldName, "xpath");
				String elementName = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, fieldName, "elementName");

				if (fieldName.equalsIgnoreCase("batchId")) {
					xPath = xPath.split(",")[0].trim();
				}
				String fieldXpath = xPath + "/" + elementName;
				List<Node> fieldNodes = XMLUtils.getNodes(xmlFile, fieldXpath);

				for (Node fieldNode : fieldNodes) {
					String fieldValueInXml = XMLUtils.getValue(fieldNode);
					expectedFieldValues.add(fieldValueInXml);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting expected value for the field : {}, showPageMapping : {}", fieldName, fieldMappingOnShowPage);
		}
		return expectedFieldValues;
	}

	private String getShowResponse(String contractDbId) {
		String response = null;

		Show show = new Show();
		show.hitShow(61, Integer.parseInt(contractDbId));
		response = show.getShowJsonStr();

		return response;

	}

	private List<String> getActualFieldValues(String showPageResponse, String contractDbId, String fieldName, String fieldMappingOnShowPage) {
		List<String> actualValues = new ArrayList<>();

		try {
			Show show = new Show();
			if (APIUtils.validJsonResponse(showPageResponse, "show response for dbId : " + contractDbId)) {

				/*getting actual value for dynamic fields*/
				if (fieldMappingOnShowPage.substring(0, 3).equalsIgnoreCase("dyn") || dynamicFields.contains(fieldName) || dynamicFields.contains(fieldMappingOnShowPage)) {

					if (singleSelectDynamicFields.contains(fieldName) || singleSelectDynamicFields.contains(fieldMappingOnShowPage)) {
						String value = show.getValueOfDynamicFieldWhenValueFieldIsObject(showPageResponse, fieldMappingOnShowPage);
						actualValues.add(value);
					} else if (multiSelectDynamicFields.contains(fieldName) || multiSelectDynamicFields.contains(fieldMappingOnShowPage)) {
						actualValues = show.getValueOfDynamicFieldWhenValueFieldIsArray(showPageResponse, fieldMappingOnShowPage);
					} else if (dynamicTableFields.contains(fieldName) || dynamicTableFields.contains(fieldMappingOnShowPage)) {
						if (ParseConfigFile.hasProperty(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "dynamic table fields", fieldMappingOnShowPage)) {
							String dynamicTableFieldName = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldMappingFilePath, vfIntegrationFieldMappingFileName, "dynamic table fields", fieldMappingOnShowPage);
							actualValues = show.getValueOfDynamicTableField(showPageResponse, dynamicTableFieldName, fieldMappingOnShowPage);
						}
					} else {
						/*default hierarchy for dynamic field. i.e., body->data->dynamicMetadata->values*/
						String value = show.getValueOfDynamicField(showPageResponse, fieldMappingOnShowPage);
						if (value != null) {
							actualValues.add(value);
						}
					}
					return actualValues;
				} else if (stakeholderFields.contains(fieldName) || stakeholderFields.contains(fieldMappingOnShowPage)) {
					actualValues = show.getValueOfStakeholderField(showPageResponse, fieldMappingOnShowPage);
					return actualValues;
				} else {
					String hierarchy = ShowHelper.getShowFieldHierarchy(fieldMappingOnShowPage, 61);

					if (multiSelectFields.contains(fieldName) || multiSelectFields.contains(fieldMappingOnShowPage)) {
						logger.info("fieldName : {} is multi-select field. Getting value.", fieldName);

						if (hierarchy != null)
							actualValues = ShowHelper.getActualValues(showPageResponse, hierarchy);
						else
							logger.error("Hierarchy not found for field :{}", fieldName);
					} else {
						String actualValue = ShowHelper.getActualValue(showPageResponse, hierarchy);
						actualValues.add(actualValue);
					}
				}
			} else {
				logger.error("show api response is not valid json for contract dbid : {}", contractDbId);
			}
		} catch (Exception e) {
			logger.error("Exception while getting actual field value for dbId : {}, fieldName : {}. error : {}", contractDbId, fieldName, e.getMessage());
			e.printStackTrace();
		}
		return actualValues;
	}

	private Map<String, String> getMainAppContractUiIdAndDbIdMap() {
		Map<String, String> map = new LinkedHashMap<>();
		String listDataPayload = "{\"filterMap\":{}}";

		try {
			logger.info("Hitting ListRendererListData");
			ListRendererListData listDataObj = new ListRendererListData();
			listDataObj.hitListRendererListData(2, true, listDataPayload, null);
			String response = listDataObj.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response, "listData")) {
				listDataObj.setListData(response);

				Integer columnId = listDataObj.getColumnIdFromColumnName("id");
				List<String> contractUiIdList = listDataObj.getAllRecordForParticularColumns(columnId);
				List<Integer> contractDbIdList = listDataObj.getAllRecordDbId(columnId, response);

				int count = 0;
				for (String record : contractUiIdList) {
					map.put("CO" + record, contractDbIdList.get(count).toString());
					count++;
				}
			} else {
				logger.error("List Data response is not valid json.");
			}

		} catch (Exception e) {
			logger.error("Exception while getting MainAppContractDbIdAndUiIdMap. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return map;
	}

	private Map<String, String> getStagingContractIdAndUiIdMap() {
		Map<String, String> map = new LinkedHashMap<>();
		String payload = "{\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

		try {
			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response, "integration listing")) {
				Integer clientPrimaryKeyColumnId = integrationListData.getColumnIdFromColumnName("stagingClientPrimaryKey");
				List<String> stagedContractIdList = integrationListData.getAllRecordForParticularColumns(clientPrimaryKeyColumnId);

				Integer stagingSirionKeyColumnId = integrationListData.getColumnIdFromColumnName("stagingSirionKey");
				List<String> stagingSirionKeyIdList = integrationListData.getAllRecordForParticularColumns(stagingSirionKeyColumnId);

				for (int i = 0; i < stagedContractIdList.size(); i++) {
					if (processedContractIdList.contains(stagedContractIdList.get(i))) {
						map.put(stagedContractIdList.get(i), stagingSirionKeyIdList.get(i));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting StagingContractIdAndUiIdMap. error : {}", e.getMessage());
		}
		return map;
	}

	private Map<String, String> getContractIdAndStagingIdMap() {
		Map<String, String> map = new HashMap<>();
		String payload = "{\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

		try {
			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response, "integration listing")) {
				Map<String, String> contractClientPrimaryKeyAndStagingIdMap = integrationListData.getContractClientPrimaryKeyAndStagingIdMap(response);

				for (Map.Entry<String, String> entry : contractClientPrimaryKeyAndStagingIdMap.entrySet()) {
					if (processedContractIdList.contains(entry.getKey())) {
						map.put(entry.getKey(), entry.getValue());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting StagingContractIdAndUiIdMap. error : {}", e.getMessage());
		}
		return map;
	}

	private Boolean validateContractCreationOnStaging() {

		Boolean flag = true;
		String payload = "{\"offset\":0,\"size\":5,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

		try {
			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response, "integration listing")) {
				Integer clientPrimaryKeyColumnId = integrationListData.getColumnIdFromColumnName("stagingClientPrimaryKey");
				List<String> stagedContractIdList = integrationListData.getAllRecordForParticularColumns(clientPrimaryKeyColumnId);

				for (String contractId : processedContractIdList) {
					if (!stagedContractIdList.contains(contractId)) {
						logger.error("contract id : {} is not found on listing api response.", contractId);
						flag = false;
						break;
					}
				}
			} else {
				logger.error("Integration listing api response is not valid json");
				flag = false;
			}

		} catch (Exception e) {
			flag = false;
			logger.error("Exception while getting latest batch id. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private void waitForSchedulerToProcessFiles() {

		try {
			logger.info("Waiting for scheduler to process test data. Wait time : {} seconds", waitBeforeStaging);
			Thread.sleep(waitBeforeStaging * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Boolean uploadOnSFTPServer(String flow, CustomAssert csAssert) {
		Boolean isUploadSuccess = true;
		try {
			String testDataFilePath = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, flow, "testdatafilepath");

			List<File> controllerFile = new ArrayList<>();
			Boolean isXmlUploadSuccess = true;

			File folder = new File(testDataFilePath);
			File[] listOfFiles = folder.listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					File currentFile = listOfFiles[i];
					logger.info("File : {}", currentFile.getName());
					String fileType = FilenameUtils.getExtension(currentFile.getName());

					if (fileType.equalsIgnoreCase("xml")) {
						Boolean temp = FileUtils.uploadFileOnSFTPServer(sftpHost, sftpPort, sftpUsername, sftpPassword, sftpTargetDir, currentFile);

						if (temp == false) {
							logger.error("Unable to upload file : {}", currentFile);
							isXmlUploadSuccess = false;
						} else
							uploadedXmlFiles.add(currentFile);
					} else if (fileType.equalsIgnoreCase("txt")) {
						controllerFile.add(currentFile);
						continue;
					}
				} else {
					logger.info("Directory found : {}", listOfFiles[i].getName());
					if (flow.contains("reprocessing")) {
						File[] innerFiles = listOfFiles[i].listFiles();
						for (int j = 0; j < innerFiles.length; j++) {
							File currentFile = innerFiles[j];
							logger.info("File : {}", currentFile.getName());
							String fileType = FilenameUtils.getExtension(currentFile.getName());

							if (fileType.equalsIgnoreCase("xml")) {
								Boolean temp = FileUtils.uploadFileOnSFTPServer(sftpHost, sftpPort, sftpUsername, sftpPassword, sftpTargetDir, currentFile);

								if (temp == false) {
									logger.error("Unable to upload file : {}", currentFile);
									isXmlUploadSuccess = false;
								} else
									uploadedXmlFiles.add(currentFile);
							} else if (fileType.equalsIgnoreCase("txt")) {
								controllerFile.add(currentFile);
								continue;
							}
						}
					}
				}
			}
			/*Uploading controller file*/
			if (isXmlUploadSuccess) {
				Boolean flag = true;
				for (File controller : controllerFile) {
					Boolean temp = FileUtils.uploadFileOnSFTPServer(sftpHost, sftpPort, sftpUsername, sftpPassword, sftpTargetDir, controller);
					if (!temp)
						flag = false;

					waitForSchedulerToProcessFiles();
				}

				if (!flag) {
					logger.error("Controller file upload failed. Hence validation terminated. flow : {}", flow);
					csAssert.assertTrue(false, "Controller file upload failed. Hence validation terminated. flow : " + flow);
					return false;
				}
			} else {
				logger.error("XML file upload failed. Hence validation terminated. flow : {}", flow);
				csAssert.assertTrue(false, "XML file upload failed. Hence validation terminated. flow : " + flow);
				return false;
			}


		} catch (Exception e) {
			isUploadSuccess = false;
			csAssert.assertTrue(false, "Exception while uploading files on sftp server. flow : " + flow);
			logger.error("Exception while uploading files on sftp server. flow : {}", flow);
			e.printStackTrace();
		}
		return isUploadSuccess;
	}

	private Boolean updateTestData(String flow, CustomAssert csAssert) {

		Boolean isUpdateSuccess = true;
		try {
			String testDataFilePath = ParseConfigFile.getValueFromConfigFile(vfIntegrationConfigFilePath, vfIntegrationConfigFileName, flow, "testdatafilepath");

			Integer latestBatchId = getLatestClientPrimaryKeyId();
			Integer latestClientPrimaryKeyId = getLatestClientPrimaryKeyId();

			if (latestBatchId == null || latestClientPrimaryKeyId == null) {

				logger.error("Unable to fetch latest batchId/clientPrimaryKeyId. Hence terminating the validation for flow : {}", flow);
				csAssert.assertTrue(false, "Unable to fetch latest batchId/clientPrimaryKeyId. Flow : " + flow);
				return false;
			}
			Integer newBatchId = latestBatchId + 1;
			Integer newClientPrimaryKeyId = latestClientPrimaryKeyId + 1;

			File folder = new File(testDataFilePath);
			File[] listOfFiles = folder.listFiles();

			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					File currentFile = listOfFiles[i];
					logger.info("File : {}", currentFile.getName());
					String fileType = FilenameUtils.getExtension(currentFile.getName());

					String batchIdPresentInTestData = null;
					if (fileType.equalsIgnoreCase("txt")) {
						batchIdPresentInTestData = currentFile.getName().split("-")[1];
					} else if (fileType.equalsIgnoreCase("xml")) {
						batchIdPresentInTestData = currentFile.getName().split("-")[0];
					}

					if (batchIdPresentInTestData.equalsIgnoreCase(newBatchId.toString())) {
						logger.warn("files are already present with the latest batch id. Hence skipping updation.");
						processedContractIdList.add(newClientPrimaryKeyId.toString());
						return true;
					}

					if (fileType.equalsIgnoreCase("txt")) {
						BufferedReader br = new BufferedReader(new FileReader(currentFile));

						String newName = testDataFilePath + "/MA-" + newBatchId + ".txt";
						File newFile = new File(newName);
						/*File tempFile = new File(testDataFilePath + "/tempFile.txt");*/
						PrintWriter pw = new PrintWriter(new FileWriter(newFile));

						String line = null;

						int count = 0;
						while ((line = br.readLine()) != null) {
							line = (newBatchId) + "-MA-" + (newClientPrimaryKeyId + count) + ".xml";
							pw.println(line);
							pw.flush();
							count++;
						}
						pw.close();
						br.close();

						// Delete the original file
						if (!currentFile.delete()) {
							logger.error("Could not delete file: {}", currentFile.getName());
							return false;
						}


					} else {
						//updating xml file
						latestClientPrimaryKeyId++;
						processedContractIdList.add(latestClientPrimaryKeyId.toString());

						String parentXpathForId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "id", "xpath");
						String elementNameForId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "id", "elementname");
						String[] parentXpathForBatchId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "batchid", "xpath").split(",");
						String elementNameForBatchId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "batchid", "elementname");

						Document modifiedDocument = XMLUtils.modifyElement(currentFile, parentXpathForId.trim(), elementNameForId.trim(), latestClientPrimaryKeyId.toString());

						for (String xpath : parentXpathForBatchId) {
							modifiedDocument = XMLUtils.modifyElement(modifiedDocument, xpath.trim(), elementNameForBatchId, newBatchId.toString());
						}

						if (flow.contains("parentmissing")) {
							String parentXpathForParentContractId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, parentContractIdXpathSectionName, "xpath");
							String elementNameForParentContractId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, parentContractIdXpathSectionName, "elementname");

							modifiedDocument = XMLUtils.modifyElement(modifiedDocument, parentXpathForParentContractId.trim(), elementNameForParentContractId.trim(), parentContractId);
						}

						String newName = testDataFilePath + "/" + newBatchId + "-MA-" + (latestClientPrimaryKeyId) + ".xml";
						File newFileName = new File(newName);

						XMLUtils.dumpXmlDocumentIntoFile(modifiedDocument, newFileName);
						// Delete the original file
						if (!currentFile.delete()) {
							logger.error("Could not delete file: {}", currentFile.getName());
							return false;
						}
					}

				} else if (listOfFiles[i].isDirectory()) {
					logger.info("Directory found : {} ", listOfFiles[i].getName());
					String directoryName = listOfFiles[i].getName();
					if (flow.contains("reprocessing")) {
						File[] innerFiles = listOfFiles[i].listFiles();
						Integer tempBatchId = (newBatchId + i);

						for (int j = 0; j < innerFiles.length; j++) {
							File currentFile = innerFiles[j];
							logger.info("File : {}", currentFile.getName());
							String fileType = FilenameUtils.getExtension(currentFile.getName());

							String batchIdPresentInTestData = null;
							if (fileType.equalsIgnoreCase("txt")) {
								batchIdPresentInTestData = currentFile.getName().split("-")[1];
							} else if (fileType.equalsIgnoreCase("xml")) {
								batchIdPresentInTestData = currentFile.getName().split("-")[0];
							}
							if (batchIdPresentInTestData.equalsIgnoreCase(tempBatchId.toString())) {
								logger.warn("files are already present with the latest batch id. Hence skipping updation.");
								Integer contractId = newClientPrimaryKeyId + i;
								processedContractIdList.add(contractId.toString());
								continue;
							}

							if (fileType.equalsIgnoreCase("txt")) {
								BufferedReader br = new BufferedReader(new FileReader(currentFile));

								String newName = (testDataFilePath + "/" + directoryName) + "/MA-" + (newBatchId + i) + ".txt";
								File newFile = new File(newName);
								PrintWriter pw = new PrintWriter(new FileWriter(newFile));

								String line = null;

								while ((line = br.readLine()) != null) {
									line = (newBatchId + i) + "-MA-" + (newClientPrimaryKeyId + i) + ".xml";
									pw.println(line);
									pw.flush();
								}
								pw.close();
								br.close();

								// Delete the original file
								if (!currentFile.delete()) {
									logger.error("Could not delete file: {}", currentFile.getName());
									return false;
								}

							} else {
								//updating xml file
								latestClientPrimaryKeyId++;
								processedContractIdList.add(latestClientPrimaryKeyId.toString());

								Integer batchId = (newBatchId + i);

								String parentXpathForId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "id", "xpath");
								String elementNameForId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "id", "elementname");
								String[] parentXpathForBatchId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "batchid", "xpath").split(",");
								String elementNameForBatchId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, "batchid", "elementname");

								Document modifiedDocument = XMLUtils.modifyElement(currentFile, parentXpathForId.trim(), elementNameForId.trim(), latestClientPrimaryKeyId.toString());

								if (directoryName.contains("Reprocessing")) {
									reprocessingContractId = latestClientPrimaryKeyId.toString();
									String parentXpathForParentContractId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, parentContractIdXpathSectionName, "xpath");
									String elementNameForParentContractId = ParseConfigFile.getValueFromConfigFile(vfIntegrationFieldXPathMappingFilePath, vfIntegrationFieldXPathMappingFileName, parentContractIdXpathSectionName, "elementname");

									Integer nextContractId = newClientPrimaryKeyId + 1;
									modifiedDocument = XMLUtils.modifyElement(modifiedDocument, parentXpathForParentContractId.trim(), elementNameForParentContractId.trim(), nextContractId.toString());
								}

								for (String xpath : parentXpathForBatchId) {
									modifiedDocument = XMLUtils.modifyElement(modifiedDocument, xpath.trim(), elementNameForBatchId, batchId.toString());
								}
								String newName = (testDataFilePath + "/" + directoryName) + "/" + (newBatchId + i) + "-MA-" + (latestClientPrimaryKeyId) + ".xml";
								File newFileName = new File(newName);

								XMLUtils.dumpXmlDocumentIntoFile(modifiedDocument, newFileName);
								// Delete the original file
								if (!currentFile.delete()) {
									logger.error("Could not delete file: {}", currentFile.getName());
									return false;
								}
							}
						}
					}
				}
			}

		} catch (Exception e) {
			isUpdateSuccess = false;
			csAssert.assertTrue(false, "Exception while updating Test Data for vf integration flow : " + flow + ", error: " + e.getMessage());
			logger.error("Exception while updating Test Data for vf integration flow :{}, error: {}", flow, e.getMessage());
			e.printStackTrace();
		}
		return isUpdateSuccess;
	}

	private Integer getLatestBatchId() {
		Integer latestBatchId = null;
		String payload = "{\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

		try {
			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response)) {
				Integer batchIdColumnId = integrationListData.getColumnIdFromColumnName("batchId");
				latestBatchId = Integer.parseInt(integrationListData.getAllRecordForParticularColumns(batchIdColumnId).get(0));
			} else
				logger.error("IntegrationListData API is not valid json. response : {}", response);

		} catch (Exception e) {
			logger.error("Exception while getting latest batch id. error : {}", e.getMessage());
			e.printStackTrace();
		}

		return latestBatchId;

	}

	private Integer getLatestClientPrimaryKeyId() {
		Integer latestClientPrimaryKeyId = null;
		String payload = "{\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

		try {
			IntegrationListData integrationListData = new IntegrationListData();
			integrationListData.hitIntegrationListData(stagingEntityListIdForContract, payload);
			String response = integrationListData.getListDataJsonStr();

			if (APIUtils.validJsonResponse(response)) {
				Integer clientPrimaryKeyColumnId = integrationListData.getColumnIdFromColumnName("stagingClientPrimaryKey");
				latestClientPrimaryKeyId = Integer.parseInt(integrationListData.getAllRecordForParticularColumns(clientPrimaryKeyColumnId).get(0));
			} else
				logger.error("IntegrationListData API response is not valid json. response : {}", response);

		} catch (Exception e) {
			logger.error("Exception while getting latest ClientPrimaryKey id. error : {}", e.getMessage());
			e.printStackTrace();
		}

		return latestClientPrimaryKeyId;
	}

	private List<String> getPropertyList(String propertyName) throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(this.vfIntegrationConfigFilePath, this.vfIntegrationConfigFileName, propertyName);
		List<String> list = new ArrayList<String>();

		if (!value.trim().equalsIgnoreCase("")) {
			String properties[] = ParseConfigFile.getValueFromConfigFile(this.vfIntegrationConfigFilePath, this.vfIntegrationConfigFileName, propertyName).split(",");

			for (int i = 0; i < properties.length; i++)
				list.add(properties[i].trim());
		}
		return list;
	}

	private void loggingOnSeparateEnv(String environment) {
		try {
			logger.info("Logging on separate environment. env file name : {}", environment);
			String specificEnv = environment;
			oldEnvironmentFileName = ConfigureEnvironment.environment;

			ConfigureEnvironment.configureProperties(specificEnv, true);

			Check checkObj = new Check();
			//Login on different env.
			checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
			Assert.assertTrue(Check.getAuthorization() != null, "Login for vf integration Suite. Authorization not set by Check Api. Environment = " + specificEnv);

		} catch (Exception e) {
			logger.error("Exception occurred while logging into alternate environment for vf integration. Error = {}", e.getMessage());
			e.printStackTrace();
		}
	}

	@AfterClass
	public void loggingOnOriginalEnv() {
		try {
			logger.info("In After Class.");
			if (runOnDifferentEnv) {
				logger.info("Setting the configuration to original environment : {}", oldEnvironmentFileName);
				ConfigureEnvironment.configureProperties(oldEnvironmentFileName, true);

				Check checkObj = new Check();
				//Login on different env.
				checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
				Assert.assertTrue(Check.getAuthorization() != null, "Login for master Suite. Authorization not set by Check Api. Environment = " + oldEnvironmentFileName);

			}
		} catch (Exception e) {
			logger.error("Exception occurred while rolling back the environment configuration. {}", e.getMessage());
			e.printStackTrace();
		}

	}
}
