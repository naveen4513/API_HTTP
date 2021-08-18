package com.sirionlabs.test.integration;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.integration.CreateRoute;
import com.sirionlabs.api.integration.DeleteRoute;
import com.sirionlabs.api.integration.GetRoute;
import com.sirionlabs.api.integration.TriggerRouteWithClientAndEntityId;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EnvironmentHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TestGenericIntegration {
	private final static Logger logger = LoggerFactory.getLogger(TestGenericIntegration.class);

	String genericIntegrationConfigFilePath;
	String genericIntegrationConfigFileName;
	String vendorXsltFilePath;
	String vendorXsltFileName;
	String vendorXsdFilePath;
	String vendorXsdFileName;
	String supplierXsltFilePath;
	String supplierXsltFileName;
	String supplierXsdFilePath;
	String supplierXsdFileName;
	String sftpHost;
	String sftpPort;
	String sftpUsername;
	String sftpPassword;
	String sftpTargetDir;
	String clientId;
	String entityTypeId;
	String parentEntityTypeId;
	String dbHostAddress;
	String dbPortName;
	String stagingDbName;
	String integrationDbName;
	String dbUserName;
	String dbPassword;
	String mainAppDbHostAddress;
	String mainAppDbPortName;
	String mainAppDbName;
	String mainAppDbUserName;
	String mainAppDbPassword;
	Boolean isRouteTriggeringAutomated;
	List<String> showPageFieldsToValidateForSupplier;
	List<String> showPageFieldsToValidateForVendor;
	List<String> multiSelectFields;
	String integrationEnvFileName;
	String masterSuiteEnvFileName;
	String mainAppEnvFileName;
	String cronExpression;
	String timeZone;
	Integer firstTriggerDelayTime;
	Boolean isAlternateLoginRequired;
	Boolean isRouteDeletionRequired;
	Boolean isUniqueValueUpdateRequired;
	char csvSeparator;
	Integer supplierOldSystemIdColumnNum;
	Integer vendorOldSystemIdColumnNum;
	Integer waitBeforeStaging;
	Long statusPollRefreshTimeout;
	Long timeSpent = 0L;
	Boolean isAllUniqueVendorRequired;
	String successfullyCreatedVendorExternalId;
	private static final AtomicLong LAST_TIME_MS = new AtomicLong();
	CustomAssert csAssert = new CustomAssert();


	@BeforeClass
	public void setConfig() {
		try {
			logger.info("In Before Class....");
			genericIntegrationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GenericIntegrationConfigFilePath");
			genericIntegrationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("GenericIntegrationConfigFileName");

			vendorXsltFilePath = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendorxlstfilepath");
			vendorXsltFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendorxlstfilename");
			vendorXsdFilePath = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendorxsdfilepath");
			vendorXsdFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendorxsdfilename");

			supplierXsltFilePath = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "supplierxlstfilepath");
			supplierXsltFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "supplierxlstfilename");
			supplierXsdFilePath = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "supplierxsdfilepath");
			supplierXsdFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "supplierxsdfilename");


			sftpHost = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "sftphost");
			sftpPort = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "sftpport");
			sftpUsername = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "sftpusername");
			sftpPassword = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "sftppassword");
			sftpTargetDir = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "sftptargetdir");

			clientId = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "clientid");
			entityTypeId = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "entitytypeid");
			parentEntityTypeId = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "parententitytypeid");

			integrationEnvFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "integrationenvfilename");
			mainAppEnvFileName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappenvfilename");
			isAlternateLoginRequired = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "isalternateloginrequired"));

			dbHostAddress = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dbhostaddress");
			dbPortName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dbportname");
			dbUserName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dbusername");
			dbPassword = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dbpassword");
			stagingDbName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "stagingdbname");
			integrationDbName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "integrationdbname");

			mainAppDbHostAddress = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappdbhostaddress");
			mainAppDbPortName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappdbportname");
			mainAppDbUserName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappdbusername");
			mainAppDbPassword = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappdbpassword");
			mainAppDbName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "mainappdbname");

			isRouteTriggeringAutomated = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "isroutetriggeringautomated"));
			isRouteDeletionRequired = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "isRouteDeletionRequired"));
			isUniqueValueUpdateRequired = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "isuniquevalueupdaterequired"));
			isAllUniqueVendorRequired = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "isalluniquevendorrequired"));
			showPageFieldsToValidateForSupplier = getPropertyList("fieldstovalidateforsupplier");
			showPageFieldsToValidateForVendor = getPropertyList("fieldstovalidateforvendor");
			multiSelectFields = getPropertyList("multiselectfields");

			cronExpression = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "cronexpression");
			timeZone = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "timezone");
			firstTriggerDelayTime = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "firstTriggerDelayTime"));
			supplierOldSystemIdColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "supplieroldsystemidcolumnnum"));
			vendorOldSystemIdColumnNum = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendoroldsystemidcolumnnum"));
			csvSeparator = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "csvSeparator").trim().charAt(0);
			waitBeforeStaging = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "waitbeforestaging"));
			statusPollRefreshTimeout = Long.parseLong(ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "statuspollrefreshtimeout"));

			masterSuiteEnvFileName = ConfigureEnvironment.environment;
			if(isAlternateLoginRequired){
				newEnvironmentLogin(mainAppEnvFileName);
			}

			if (isRouteDeletionRequired) {
				newEnvironmentPropertySetup(integrationEnvFileName);
				deleteRoute(clientId, parentEntityTypeId);
				deleteRoute(clientId, entityTypeId);
			}
		} catch (ConfigurationException e) {
			logger.error("Exception occurred while setting config properties for integration test. {}", e.getMessage());
			e.printStackTrace();
		}

	}

	@AfterClass
	public void loggingBackToMasterSuiteEnv() {
		logger.info("In After Class........");
		if (isRouteDeletionRequired) {
			newEnvironmentPropertySetup(integrationEnvFileName);
			deleteRoute(clientId, parentEntityTypeId);
			deleteRoute(clientId, entityTypeId);
		}
		if (masterSuiteEnvFileName != null) {
			logger.info("logging back to master suite env config. envFile :{}", masterSuiteEnvFileName);
			newEnvironmentLogin(masterSuiteEnvFileName);
		} else
			logger.info("New environment setup not required. Master suite env file :{}", ConfigureEnvironment.environment);
	}

	@Test
	public void testIntegrationFlow() {
		try {
			List<File> csvFiles = getCsvFilesToUpload();
			logger.info("total no. of files to upload : {}", csvFiles.size());

			int count = 0;
			for (File fileToUpload : csvFiles) {
				logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!! File Processing : {} !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!", fileToUpload.getName());
				count++;

				/*updating old system id for uniqueness*/
				if (isUniqueValueUpdateRequired) {
					if (count > 1) {
						updateCSV(fileToUpload, supplierOldSystemIdColumnNum, vendorOldSystemIdColumnNum, successfullyCreatedVendorExternalId);
					} else
						updateCSV(fileToUpload, supplierOldSystemIdColumnNum, vendorOldSystemIdColumnNum, null);
				}

				logger.info("uploading file : {} to sftp dir : {}", fileToUpload.getName(), sftpTargetDir);
				Boolean isUploadSuccess = FileUtils.uploadFileOnSFTPServer(sftpHost, Integer.parseInt(sftpPort), sftpUsername, sftpPassword, sftpTargetDir, fileToUpload);

				csAssert.assertTrue(isUploadSuccess, "Unable to upload file on sftp server : " + sftpHost + ", path : " + sftpTargetDir + ", user :" + sftpUsername);
				if (isUploadSuccess) {
					logger.info("File uploaded successfully. file: {}", fileToUpload.getName());

					if (count == 1) {
						/*setting environment properties for integration server*/
						logger.info("setting environment properties for integration server. {}", integrationEnvFileName);
						newEnvironmentPropertySetup(integrationEnvFileName);

						logger.info("Creating and triggering route explicitly.");
						Boolean isRouteTriggered = createAndTriggerRoute(clientId, parentEntityTypeId, entityTypeId);
						if (isRouteTriggered) {
							logger.info("Waiting for {} minutes", firstTriggerDelayTime + waitBeforeStaging);
							Thread.sleep((firstTriggerDelayTime + waitBeforeStaging) * 60000);
						} else {
							csAssert.assertTrue(false, "Unable to trigger route for clientId :" + clientId + " and entityTypeId :" + entityTypeId);
							logger.error("Unable to trigger route for clientId :{} and entityTypeId :{}. Hence skipping.", clientId, entityTypeId);
							csAssert.assertAll();
							return;
						}
					} else {
						logger.info("Waiting for scheduler to pick next csv to process. waiting time : {}minute", firstTriggerDelayTime);
						Thread.sleep(firstTriggerDelayTime * 60000);
					}

					List<Map<String, String>> csvDataList = getCsvDataList(fileToUpload);
					logger.info("Number of rows found in csv = {}", csvDataList.size());

					Set<String> vendorExternalIds = getVHExternalIds(csvDataList, "H_ATTRIBUTE3");

					/*Login into alternate environment*/
					if (isAlternateLoginRequired) {
						logger.info("Separate login is required from master suite. new env :{}", mainAppEnvFileName);
						newEnvironmentLogin(mainAppEnvFileName);
					}else{
						logger.info("Separate login is not required. Hence logging into master suite env :{}", masterSuiteEnvFileName);
						newEnvironmentLogin(masterSuiteEnvFileName);
					}

					List<String> vendorCreationFailedExternalIds = getVendorCreationIdList(clientId, vendorExternalIds).get("vhCreationFailedList");
					List<String> vendorDbIds = getVendorCreationIdList(clientId, vendorExternalIds).get("vhCreationSuccessList");
					Map<String, String> vhExternalIdAndDbIdMap = getVhExternalIdAndDbIdMap(vendorDbIds);

					/*********************** Processing csv file ***************/
					for (int i = 0; i < csvDataList.size(); i++) {
						Map<String, String> csvRowData = csvDataList.get(i);

						String vendorExternalId = csvDataList.get(i).get("H_ATTRIBUTE3");
						if (vendorCreationFailedExternalIds.size() > 0) {
							if (vendorCreationFailedExternalIds.contains(vendorExternalId)) {
								logger.error("vendor creation failed. Hence skipping Supplier validation. Failed external ids = {}", vendorCreationFailedExternalIds.toString());
								csAssert.assertTrue(false, "vendor creation failed. Hence skipping Supplier validation for this vendor. fileName : " + fileToUpload.getName() + "Failed vendor external id = " + vendorExternalId);
								continue;
							}
						}

						if (vhExternalIdAndDbIdMap.containsKey(vendorExternalId)) {
							logger.info("Validating Show page for Entity : VH and externalId : {}", vendorExternalId);
							successfullyCreatedVendorExternalId = vendorExternalId;
							validationOnMainApplicationForVendor(vendorExternalId, vhExternalIdAndDbIdMap.get(vendorExternalId), clientId, parentEntityTypeId, csvRowData);
							logger.info("**************** Show Page validation for Vendor is Completed for external Id :{}, row num :{}*************", vendorExternalId, i + 1);
						}

							/*################# supplier validation starts here #####################################*/
						String supplierExternalId = csvDataList.get(i).get("H_SEGMENT1");
						logger.info("********************** Validation started for Supplier for externalId :{}, row no. :{} ***********************************", supplierExternalId, i + 1);

						String status = getStatus(supplierExternalId, clientId, entityTypeId);

						if (status == null) {
							csAssert.assertTrue(false, "No entry found for Supplier, external id : " + supplierExternalId + " in entity_reference table.");
							logger.error("No entry found for Supplier, external id :{} in entity_reference table.", supplierExternalId);

						} else {
							pollStatusRefresh(supplierExternalId, clientId, entityTypeId);

							if (status.equalsIgnoreCase("created")) {
								logger.info("Entity created successfully. entity : Supplier, externalId :{}", supplierExternalId);
								String vendorDbIdLinkedWithSupp = getVendorDbIdLinkedWithSupplier(clientId, supplierExternalId);
								if (vhExternalIdAndDbIdMap.containsValue(vendorDbIdLinkedWithSupp)) {
									Map<String, String> dbIdAndExternalIdMap = vhExternalIdAndDbIdMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

									String vhLinkedExternalId = dbIdAndExternalIdMap.get(vendorDbIdLinkedWithSupp);
									if (vhLinkedExternalId.equalsIgnoreCase(vendorExternalId)) {
										logger.info("Supplier with external id :{} is successfully linked with required vendor external id : {}", supplierExternalId, vendorExternalId);
									} else {
										logger.error("Supplier with external id :{} is not linked with required vendor external id : {}", supplierExternalId, vendorExternalId);
										csAssert.assertTrue(false, "Supplier with external id : " + supplierExternalId + " is not linked with required vendor external id : " + vendorExternalId);
									}
								}
								logger.info("Validating show page for supplier. External id : {}", supplierExternalId);
								validationOnMainApplicationForSupplier(supplierExternalId, clientId, entityTypeId, csvRowData);
							} else if (status.equalsIgnoreCase("error")) {
								String errorMessage = getErrorMessage(supplierExternalId, clientId, entityTypeId);
								csAssert.assertTrue(false, "Entity: Supplier creation failed for externalId : " + supplierExternalId + " , row number : " + (i + 1) + ". error message : " + errorMessage);
								logger.error("Error occurred while creating supplier for ecc id : {} and name : {}, error : {}", supplierExternalId, csvDataList.get(i).get("H_VENDOR_NAME"), errorMessage);

							} else if (status.equalsIgnoreCase("blocked-on-parent")) {
								csAssert.assertTrue(false, "Entity: Supplier creation failed for externalId : " + supplierExternalId + " , row number : " + (i + 1) + ". status : " + "blocked-on-parent");
								logger.error("Error occurred while creating supplier for ecc id : {} and name : {}, error : blocked-on-parent", supplierExternalId, csvDataList.get(i).get("H_VENDOR_NAME"));

							}
						}
					}
				} else {
					logger.error("File uploading failed on sftp server. Hence validation skipped. fileName :{}", fileToUpload.getName());
					csAssert.assertTrue(false, "File uploading failed on sftp server. Hence validation skipped. fileName = " + fileToUpload.getName());
				}
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception occurred while validating integration flow. error : " + e.getMessage());
			logger.error("Exception while validating integration flow. error : {}", e.getMessage());
			e.printStackTrace();
		}
		csAssert.assertAll();
	}

	private List<File> getCsvFilesToUpload() {

		List<File> allCsvFiles = new ArrayList<>();
		Boolean isProcessAllCsvFiles = false;
		try {
			String csvFilePath = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "csvfilepath");
			if (ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "processallcsvfiles").equalsIgnoreCase("true"))
				isProcessAllCsvFiles = true;

			if (isProcessAllCsvFiles) {
				File folder = new File(csvFilePath);
				File[] listOfFiles = folder.listFiles();

				for (int i = 0; i < listOfFiles.length; i++) {
					if (listOfFiles[i].isFile()) {
						logger.info("File : {}", listOfFiles[i].getName());

						String newName = csvFilePath + "/automationSupplier" + uniqueCurrentTimeMS() + ".csv";
						File newFileName = new File(newName);
						if (listOfFiles[i].renameTo(newFileName))
							logger.info("file name updated successfully. new file name {}", newFileName.getName());
						else
							logger.info("Unable to rename file name.");
						allCsvFiles.add(newFileName);
					} else if (listOfFiles[i].isDirectory()) {
						logger.info("Directory found : {} ", listOfFiles[i].getName());
					}
				}
			} else {
				String[] csvFiles = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "csvfilenames").split(",");

				for (String csvFileName : csvFiles) {
					String fileName = csvFilePath + "/" + csvFileName + ".csv";
					allCsvFiles.add(new File(fileName));
				}
			}

		} catch (Exception e) {
			logger.error("Exception in getting all the csv files. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return allCsvFiles;
	}

	private Boolean createRoute(String clientId, String entityTypeId) {
		Boolean flag = false;

		//String startDateTime = "18/05/2018 19:20:02 +0530"; // format : DD/MM/YYYY HH:MM:SS +TimeZone
		String startDateTime = getStartDateTime();

		Map<String, String> queryParam = new HashMap<>();
		Map<String, File> fileParam = new HashMap<>();
		File validationFile = null;
		File transformFile = null;
		String routeMapping = null;
		String branchName = "automationSupplier";

		if (entityTypeId.equals("1")) {
			routeMapping = "{\"clientId\":" + clientId + ",\"entityTypeId\":" + entityTypeId + ",\"routeConfig\":{\"routeId\":\"" + clientId + "-" + entityTypeId + "\",\"sourceUri\":{\"protocol\":\"seda\",\"parameters\":{\"name\":\"" + branchName + "\"}},\"targetUri\":{\"protocol\":\"rest\",\"parameters\":{\"hostname\":\"192.168.2.174\",\"port\":8080,\"url\":\"/api/entity/v2/upload\"}}}}";
			logger.info("Route mapping for entityType id :{} is : {}", routeMapping);

			validationFile = new File(supplierXsdFilePath + "/" + supplierXsdFileName);
			transformFile = new File(supplierXsltFilePath + "/" + supplierXsltFileName);
		} else if (entityTypeId.equals("3")) {
			routeMapping = "{\"clientId\":" + clientId + ",\"entityTypeId\":" + entityTypeId + ",\"routeConfig\":{\"routeId\":\"" + clientId + "-" + entityTypeId + "\",\"sourceUri\":{\"protocol\":\"sftp\",\"parameters\":{\"hostname\":\"" + sftpHost + "\",\"port\":" + sftpPort + ",\"directory\":\"" + sftpTargetDir + "\",\"username\":\"" + sftpUsername + "\",\"password\":\"" + sftpPassword + "\"}},\"targetUri\":{\"protocol\":\"rest\",\"parameters\":{\"hostname\":\"192.168.2.174\",\"port\":8080,\"url\":\"/api/entity/v2/upload\"}},\"branchName\":\"" + branchName + "\"},\"routeSchedule\":{\"startDateTime\":\"" + startDateTime + "\",\"recurrenceExpression\":\"" + cronExpression + "\"}}";
			logger.info("Route mapping for entityType id :{} is : {}", routeMapping);

			validationFile = new File(vendorXsdFilePath + "/" + vendorXsdFileName);
			transformFile = new File(vendorXsltFilePath + "/" + vendorXsltFileName);
		}
		queryParam.put("clientRoute", routeMapping);


		fileParam.put("validationFile", validationFile);
		fileParam.put("transformFile", transformFile);

		CreateRoute createRouteObj = new CreateRoute();
		createRouteObj.hitCreateRoute(queryParam, fileParam);

		if (createRouteObj.getCreateRouteResponseStatusCode().equals("201")) {
			flag = true;
			logger.info("Route created successfully for clientId : {} and entityTypeId : {}", clientId, entityTypeId);
		} else {
			logger.error("Unable to create route. API Status Code : {} , API response : {}", createRouteObj.apiStatusCode, createRouteObj.getCreateRouteJsonStr());
		}
		return flag;
	}

	private Boolean deleteRoute(String clientId, String entityTypeId) {
		Boolean flag = false;

		DeleteRoute deleteRouteObj = new DeleteRoute();
		deleteRouteObj.hitDeleteRoute(clientId, entityTypeId);
		String apiStatusCode = deleteRouteObj.getDeleteRouteResponseStatusCode();

		if (apiStatusCode.equals("204")) {
			flag = true;
			logger.info("route deleted successfully for clientId : {} and entityTypeId : {}", clientId, entityTypeId);
		} else {
			logger.error("unable to delete route for clientId : {} and entityTypeId : {}. API Status Code : {} , API response : {}", clientId, entityTypeId, apiStatusCode, deleteRouteObj.getDeleteRouteJsonStr());
		}
		return flag;
	}

	private Boolean getRoute(String clientId, String entityTypeId) {
		Boolean flag = false;

		GetRoute getRouteObj = new GetRoute();
		getRouteObj.hitGetRoute(clientId, entityTypeId);
		String apiStatusCode = getRouteObj.getRouteStatusCode();

		if (apiStatusCode.equals("302")) {
			flag = true;
			logger.info("route found for clientId : {} and entityTypeId : {}, API response : {}", clientId, entityTypeId, getRouteObj.getRouteJsonStr());
		} else {
			logger.error("unable to find route for clientId : {} and entityTypeId : {}. API Status Code : {} , API response : {}", clientId, entityTypeId, apiStatusCode, getRouteObj.getRouteJsonStr());
		}
		return flag;
	}

	private Boolean triggerRoute(String clientId, String entityTypeId) {
		Boolean flag = false;

		TriggerRouteWithClientAndEntityId triggerRouteWithClientAndEntityIdObj = new TriggerRouteWithClientAndEntityId();
		triggerRouteWithClientAndEntityIdObj.hitTriggerRouteWithClientAndEntityId(clientId, entityTypeId);
		String apiStatusCode = triggerRouteWithClientAndEntityIdObj.getApiStatusCode();

		if (apiStatusCode.equals("202")) {
			flag = true;
			logger.info("route triggered successfully for clientId : {} and entityTypeId : {}, API response : {}", clientId, entityTypeId);
		} else {
			logger.error("unable to trigger route for clientId : {} and entityTypeId : {}. API Status Code : {} , API response : {}", clientId, entityTypeId, apiStatusCode, triggerRouteWithClientAndEntityIdObj.getTriggerRouteJsonStr());
		}
		return flag;
	}

	private Boolean createAndTriggerRoute(String clientId, String parentEntityTypeId, String entityTypeId) {
		Boolean flag = false;

		try {
			Boolean isParentRouteAlreadyExist = getRoute(clientId, parentEntityTypeId);
			Boolean isChildRouteAlreadyExist = getRoute(clientId, entityTypeId);
			if (isParentRouteAlreadyExist || isChildRouteAlreadyExist) {
				Boolean deleteExistingParentRoute = deleteRoute(clientId, parentEntityTypeId);
				Boolean deleteExistingChildRoute = deleteRoute(clientId, entityTypeId);
				if (deleteExistingParentRoute && deleteExistingChildRoute) {
					Boolean createNewParentRoute = createRoute(clientId, parentEntityTypeId);
					Boolean createNewChildRoute = createRoute(clientId, entityTypeId);
					if (createNewParentRoute && createNewChildRoute) {
						Boolean isParentRouteFound = getRoute(clientId, parentEntityTypeId);
						Boolean isChildRouteFound = getRoute(clientId, entityTypeId);
						if (isParentRouteFound && isChildRouteFound) {
							if (!isRouteTriggeringAutomated) {
								Boolean triggerParentRouteStatus = triggerRoute(clientId, parentEntityTypeId);
								Boolean triggerChildRouteStatus = triggerRoute(clientId, entityTypeId);
								if (triggerParentRouteStatus && triggerChildRouteStatus) {
									flag = true;
									logger.info("Route triggered successfully for clientId : {}, entityTypeId : {}", clientId, entityTypeId);
								}
							} else {
								logger.info("Route triggering is automated.");
								flag = true;
							}

						}
					}
				}
			} else {
				Boolean createNewParentRoute = createRoute(clientId, parentEntityTypeId);
				Boolean createNewChildRoute = createRoute(clientId, entityTypeId);
				if (createNewParentRoute && createNewChildRoute) {
					Boolean isParentRouteFound = getRoute(clientId, parentEntityTypeId);
					Boolean isChildRouteFound = getRoute(clientId, entityTypeId);
					if (isParentRouteFound && isChildRouteFound) {
						if (!isRouteTriggeringAutomated) {
							Boolean triggerParentRouteStatus = triggerRoute(clientId, parentEntityTypeId);
							Boolean triggerChildRouteStatus = triggerRoute(clientId, entityTypeId);
							if (triggerParentRouteStatus && triggerChildRouteStatus) {
								flag = true;
								logger.info("Route triggered successfully for clientId : {}, entityTypeId : {}", clientId, entityTypeId);
							}
						} else {
							logger.info("Route triggering is automated.");
							flag = true;
						}

					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while creating/triggering route. error {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private List<List<String>> executeQuery(String host, String port, String dbName, String userName, String password, String query) {
		List<List<String>> result = null;
		try {
			PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(host, port, dbName, userName, password);
			result = postgreSQLJDBC.doSelect(query);
			postgreSQLJDBC.closeConnection();
		} catch (SQLException e) {
			logger.error("Exception while executing db query : {}, error : {}", query, e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	private List<Map<String, String>> getCsvDataList(File file) throws IOException {
		List<Map<String, String>> response = new LinkedList<Map<String, String>>();
		CsvMapper mapper = new CsvMapper();

		CsvSchema schema = mapper.schemaFor(TestGenericIntegration.class);
		schema = schema.withColumnSeparator(csvSeparator).withHeader();
		//CsvSchema schema = CsvSchema.emptySchema().withHeader();
		MappingIterator<Map<String, String>> iterator = mapper.readerFor(Map.class)
				.with(schema)
				.readValues(file);
		while (iterator.hasNext()) {
			response.add(iterator.next());
		}
		return response;
	}

	private String getShowPageResponse(Integer entityTypeId, String dbId) {
		Show showObj = new Show();
		showObj.hitShow(entityTypeId, Integer.parseInt(dbId));
		return showObj.getShowJsonStr();
	}

	private void validationOnMainApplicationForSupplier(String externalId, String clientId, String entityTypeId, Map<String, String> csvRowData) {

		try {
			String queryForNewlyGeneratedSupplier = "select id from relation where client_id =" + clientId + " and old_system_id = '" + externalId + "'";
			List<List<String>> supplierResult = executeQuery(mainAppDbHostAddress, mainAppDbPortName, mainAppDbName, mainAppDbUserName, mainAppDbPassword, queryForNewlyGeneratedSupplier);
			String dbId = supplierResult.get(0).get(0);

			logger.info("Newly created entity : Supplier, dbId : {}", dbId);
			String showPageResponse = getShowPageResponse(Integer.parseInt(entityTypeId), dbId);

			if (APIUtils.validJsonResponse(showPageResponse, "show api")) {
				for (String fieldName : showPageFieldsToValidateForSupplier) {
					logger.info("************** validating field name : {} *************", fieldName);
					String hierarchy = ShowHelper.getShowFieldHierarchy(fieldName, Integer.parseInt(entityTypeId));
					String expectedValue = getExpectedValueOfField(fieldName, csvRowData, entityTypeId);

					if (multiSelectFields.contains(fieldName)) {
						logger.info("fieldName : {} is multi-select field. Getting value value.", fieldName);
						List<String> actualValues = null;

						if (hierarchy != null)
							actualValues = ShowHelper.getActualValues(showPageResponse, hierarchy);
						else
							logger.error("Hierarchy not found for field :{}", fieldName);

						if (expectedValue != null && actualValues != null) {
							if (actualValues.contains(expectedValue)) {
								logger.info("Validation passed for field name : {}", fieldName);
							} else {
								csAssert.assertTrue(false, "Validation failed for field name : " + fieldName + ", expected : " + expectedValue + " but got : " + actualValues);
								logger.error("Validation failed for field name : {}, expected : {} but got : {}", fieldName, expectedValue, actualValues);
							}
						} else {
							csAssert.assertTrue(false, "Expected/Actual value found to be null for fieldName :" + fieldName + ". expected :" + expectedValue + ", actual:" + actualValues);
							logger.error("Expected/Actual value found to be null for fieldName :{}. expected :{}, actual:{}", fieldName, expectedValue, actualValues);
						}

					} else {
						String actualValue = null;
						if (hierarchy != null) {
							actualValue = ShowHelper.getActualValue(showPageResponse, hierarchy);
							if (fieldName.equalsIgnoreCase("alias")) {
								actualValue = actualValue.substring(0, 3);
							}
						} else
							logger.error("Hierarchy not found for field :{}", fieldName);

						if (actualValue != null && expectedValue != null) {
							if (actualValue.equalsIgnoreCase(expectedValue)) {
								logger.info("Validation passed for field name : {}", fieldName);
							} else {
								csAssert.assertTrue(false, "Validation failed for field name : " + fieldName + ", expected : " + expectedValue + " but got : " + actualValue);
								logger.error("Validation failed for field name : {}, expected : {} but got : {}", fieldName, expectedValue, actualValue);
							}
						} else {
							csAssert.assertTrue(false, "Expected/Actual value found to be null for fieldName :" + fieldName + ". expected :" + expectedValue + ", actual:" + actualValue);
							logger.error("Expected/Actual value found to be null for fieldName :{}. expected :{}, actual:{}", fieldName, expectedValue, actualValue);
						}
					}

				}
			} else {
				csAssert.assertTrue(false, "show api response is not valid json. Hence validation skipped. entity DbId : " + dbId + " , env : " + mainAppEnvFileName);
				logger.error("show api response is not valid json for dbId : {} and env : {}. Hence validation skipped.", dbId, mainAppEnvFileName);
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating fields on show page for externalId : " + externalId + ". error : " + e.getMessage());
			logger.error("Exception while validating fields on show page for externalId : {}. error :{}", externalId, e.getMessage());
			e.printStackTrace();
		}
		logger.info("################### Show page validation completed for externalId = {} ######################################", externalId);
	}

	private void validationOnMainApplicationForVendor(String externalId, String dbId, String clientId, String entityTypeId, Map<String, String> csvRowData) {

		try {
			logger.info("Getting show page response for entity : VH, dbId : {}", dbId);
			String showPageResponse = getShowPageResponse(Integer.parseInt(entityTypeId), dbId);

			if (APIUtils.validJsonResponse(showPageResponse, "show api")) {
				for (String fieldName : showPageFieldsToValidateForVendor) {
					logger.info("************** validating field name : {} *************", fieldName);
					String hierarchy = ShowHelper.getShowFieldHierarchy(fieldName, Integer.parseInt(entityTypeId));
					String expectedValue = getExpectedValueOfField(fieldName, csvRowData, entityTypeId);

					if (multiSelectFields.contains(fieldName)) {
						List<String> actualValues = null;

						if (hierarchy != null)
							actualValues = ShowHelper.getActualValues(showPageResponse, hierarchy);
						else
							logger.error("Hierarchy not found for field :{}", fieldName);

						if (expectedValue != null && actualValues != null) {
							if (actualValues.contains(expectedValue)) {
								logger.info("Validation passed for field name : {}", fieldName);
							} else {
								csAssert.assertTrue(false, "Validation failed for field name : " + fieldName + ", expected : " + expectedValue + " but got : " + actualValues +", for externalId : "+externalId+ " and entityTypeId : "+entityTypeId);
								logger.error("Validation failed for field name : {}, expected : {} but got : {}", fieldName, expectedValue, actualValues);
							}
						} else {
							csAssert.assertTrue(false, "Expected/Actual value found to be null for fieldName :" + fieldName + ". expected :" + expectedValue + ", actual:" + actualValues);
							logger.error("Expected/Actual value found to be null for fieldName :{}. expected :{}, actual:{}", fieldName, expectedValue, actualValues);
						}

					} else {
						String actualValue = null;
						if (hierarchy != null) {
							actualValue = ShowHelper.getActualValue(showPageResponse, hierarchy);
							if (fieldName.equalsIgnoreCase("alias")) {
								actualValue = actualValue.substring(0, 3);
							}
						} else
							logger.error("Hierarchy not found for field :{}", fieldName);

						if (actualValue != null && expectedValue != null) {
							if (actualValue.equalsIgnoreCase(expectedValue)) {
								logger.info("Validation passed for field name : {}", fieldName);
							} else {
								csAssert.assertTrue(false, "Validation failed for field name : " + fieldName + ", expected : " + expectedValue + " but got : " + actualValue);
								logger.error("Validation failed for field name : {}, expected : {} but got : {}", fieldName, expectedValue, actualValue);
							}
						} else {
							csAssert.assertTrue(false, "Expected/Actual value found to be null for fieldName :" + fieldName + ". expected :" + expectedValue + ", actual:" + actualValue);
							logger.error("Expected/Actual value found to be null for fieldName :{}. expected :{}, actual:{}", fieldName, expectedValue, actualValue);
						}
					}

				}
			} else {
				csAssert.assertTrue(false, "show api response is not valid json. Hence validation skipped. entity DbId : " + dbId + " , env : " + mainAppEnvFileName);
				logger.error("show api response is not valid json for dbId : {} and env : {}. Hence validation skipped.", dbId, mainAppEnvFileName);
			}
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating fields on show page for externalId : " + externalId + ". error : " + e.getMessage());
			logger.error("Exception while validating fields on show page for externalId : {}. error :{}", externalId, e.getMessage());
			e.printStackTrace();
		}
		logger.info("################### Show page validation completed for externalId = {} ######################################", externalId);
	}

	private String getStatus(String externalId, String clientId, String entityTypeId) {

		String status = null;
		try {
			String query = "select status from entity_reference where client_id =" + clientId + " and entity_type_id =" + entityTypeId + " and external_id = '" + externalId + "'";
			List<List<String>> result = executeQuery(dbHostAddress, dbPortName, stagingDbName, dbUserName, dbPassword, query);
			status = result.get(0).get(0);
		} catch (Exception e) {
			logger.error("Exception occurred while getting status value from entity_reference table. error :{}", e.getMessage());
			e.printStackTrace();
		}
		return status;
	}

	private String getErrorMessage(String externalId, String clientId, String entityTypeId) {

		String query = "select errors from entity_reference where client_id =" + clientId + " and entity_type_id =" + entityTypeId + " and external_id = '" + externalId + "'";
		List<List<String>> result = executeQuery(dbHostAddress, dbPortName, stagingDbName, dbUserName, dbPassword, query);
		return result.get(0).get(0);
	}

	private List<String> getPropertyList(String propertyName) throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(this.genericIntegrationConfigFilePath, this.genericIntegrationConfigFileName, propertyName);
		List<String> list = new ArrayList<String>();

		if (!value.trim().equalsIgnoreCase("")) {
			String properties[] = ParseConfigFile.getValueFromConfigFile(this.genericIntegrationConfigFilePath, this.genericIntegrationConfigFileName, propertyName).split(",");

			for (int i = 0; i < properties.length; i++)
				list.add(properties[i].trim());
		}
		return list;
	}

	private String getExpectedValueOfField(String fieldName, Map<String, String> csvRowData, String entityTypeId) {
		String expectedValue = null;

		try {
			if (entityTypeId.equals("3")) {
				if (fieldName.equalsIgnoreCase("currency")) {
					expectedValue = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendor mapping", fieldName);
				} else if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendor mapping", fieldName)) {
					String header = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "vendor mapping", fieldName);
					if (fieldName.equalsIgnoreCase("alias")) {
						String value = csvRowData.get(header);
						expectedValue = value.substring(0, 3);
					} else
						expectedValue = csvRowData.get(header);
				}
			}
			if (expectedValue != null) {
				return expectedValue;
			}
			/*direct mapping field validation. eg. name*/
			if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "direct mapping", fieldName)) {
				String header = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "direct mapping", fieldName);
				expectedValue = csvRowData.get(header);
			}

			/*logical mapping field validation. eg. address, tiers, currency etc*/
			else if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "logical mapping", fieldName)) {

				/*handling dynamic field value which are under logical mapping*/
				if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dynamic fields", fieldName)) {
					String dynamicFieldName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "dynamic fields", fieldName);

					String header = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "logical mapping", fieldName);
					String value = csvRowData.get(header);

					if (value == null || value.equalsIgnoreCase("")) {
						logger.warn("Empty value found for the header :{}. Hence setting expected value for the field :{} as \"\" ", header, fieldName);
						expectedValue = "";
					} else if (dynamicFieldName.equalsIgnoreCase("Supplier Status")) {
						Date fieldValue = new SimpleDateFormat("dd.MM.yyyy").parse(value);
						Date currDate = new SimpleDateFormat("dd.MM.yyyy").parse(getCurrentDate());

						if (fieldValue.before(currDate)) {
							expectedValue = "Inactive";
						} else {
							expectedValue = "Active";
						}
					}
				} else if (fieldName.equalsIgnoreCase("address")) {
					String headers[] = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "logical mapping", fieldName).split(",");
					String address = "";
					for (String header : headers) {
						String value = csvRowData.get(header.trim());
						if (value == null)
							value = "";

						address += value;
					}
					expectedValue = address;
				} else if (fieldName.equalsIgnoreCase("parentShortCodeId")) {
					String header = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "logical mapping", fieldName);
					String value = csvRowData.get(header);


				} else {
					String header = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "logical mapping", fieldName);
					String value = csvRowData.get(header);

					if (value == null || value.equalsIgnoreCase("")) {
						logger.warn("Empty value found for the header :{}. Hence setting expected value for the field :{} as \"\" ", header, fieldName);
						expectedValue = "";
					} else {
						if (fieldName.equalsIgnoreCase("tiers")) {
							if (value.equalsIgnoreCase("vendor")) {
								expectedValue = "Tier - 3";
							} else if (value.equalsIgnoreCase("creditor")) {
								expectedValue = "Tier 4";
							} else {
								logger.error("invalid value found for field :{}", fieldName);
							}
						} else if (fieldName.equalsIgnoreCase("alias")) {
							expectedValue = value.substring(0, 3);
						} else if (fieldName.equalsIgnoreCase("globalRegions")) {
							if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "globalRegions mapping", value)) {
								String regionName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "globalRegions mapping", value);
								expectedValue = regionName;
							} else
								logger.error("country mapping not found for value : {}. Hence validation failed for field name: {}", value, fieldName);
						} else if (fieldName.equalsIgnoreCase("globalCountries")) {
							if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "globalCountries mapping", value)) {
								String countryName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "globalCountries mapping", value);
								expectedValue = countryName;
							} else
								logger.error("country mapping not found for value : {}. Hence validation failed for field name: {}", value, fieldName);
						} else if (fieldName.equalsIgnoreCase("currency")) {
							if (ParseConfigFile.hasProperty(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "currency mapping", value)) {
								String currencyName = ParseConfigFile.getValueFromConfigFile(genericIntegrationConfigFilePath, genericIntegrationConfigFileName, "currency mapping", value);
								expectedValue = currencyName;
							} else
								logger.error("currency mapping not found for value : {}. Hence validation failed for field name: {}", value, fieldName);
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Exception while getting expected value of field :{}, error :{}", fieldName, e.getMessage());
			e.printStackTrace();
		}
		logger.info("Expected value for fieldName : {} is {}", fieldName, expectedValue);
		return expectedValue;
	}

	private void newEnvironmentLogin(String envFileName) {
		EnvironmentHelper.loggingOnEnvironment(envFileName);
	}

	private void newEnvironmentPropertySetup(String newEnv) {
		EnvironmentHelper.setEnvironmentProperties(newEnv);
	}

	private String getStartDateTime() {
		String startDateTime = null;
		try {
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			Calendar calobj = Calendar.getInstance();
			String dateTime = df.format(calobj.getTime()).toString();

			String[] temp = dateTime.split(":");
			int minute = Integer.parseInt(temp[1]) + firstTriggerDelayTime;

			if (minute < 10)
				dateTime = temp[0] + ":0" + minute + ":" + temp[2];
			else
				dateTime = temp[0] + ":" + minute + ":" + temp[2];

			startDateTime = dateTime + " " + timeZone;
		} catch (Exception e) {
			logger.error("Exception occurred while getting start date time. error :{}", e.getMessage());
			e.printStackTrace();
		}

		return startDateTime;
	}

	private String getCurrentDate() {
		DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
		Calendar calobj = Calendar.getInstance();

		return df.format(calobj.getTime()).toString();
	}

	private void pollStatusRefresh(String externalId, String clientId, String entityTypeId) {
		try {
			String status = getStatus(externalId, clientId, entityTypeId);
			if (timeSpent >= statusPollRefreshTimeout) {
				csAssert.assertTrue(false, "Max time out occurred for status refresh,entityTypeId = " + entityTypeId + ", externalId = " + externalId + ". Status : " + status);
				timeSpent = 0L;
				return;
			}

			logger.info("pollStatusRefresh ==> status : {}", status);
			if (status.equalsIgnoreCase("staged") || status.equalsIgnoreCase("received")) {
				Thread.sleep(5000);
				timeSpent += 5000;
				pollStatusRefresh(externalId, clientId, entityTypeId);
			} else {
				timeSpent = 0L;
				return;
			}

		} catch (InterruptedException e) {
			logger.error("Exception occurred while polling status refresh. error :{}", e.getMessage());
			e.printStackTrace();
		}
	}

	private Long uniqueCurrentTimeMS() {
		Long now = System.currentTimeMillis();
		while (true) {
			long lastTime = LAST_TIME_MS.get();
			if (lastTime >= now)
				now = lastTime + 1;
			if (LAST_TIME_MS.compareAndSet(lastTime, now))
				return now;
		}
	}

	private void updateCSV(File fileToUpdate, int suppColumnNum, int vhColumnNum, String vhExternalId) throws IOException {

		try {
			logger.info("Updating csv : {} for the suppColumnNum :{} and vhColumnNum={}", fileToUpdate.getName(), suppColumnNum, vhColumnNum);
			// Read existing file
			CSVReader reader = new CSVReader(new FileReader(fileToUpdate), csvSeparator);
			List<String[]> csvBody = reader.readAll();

			// get CSV row column  and replace with by using row and column
			String vendorExternalId = null;
			for (int i = 1; i < csvBody.size(); i++) {
				String uniqueNum = uniqueCurrentTimeMS().toString();

				if (vhExternalId == null) {
					if (isAllUniqueVendorRequired) {
						vendorExternalId = "VH" + uniqueNum;
					} else {
						if (i % 2 != 0) {
							vendorExternalId = "VH" + uniqueNum;
						}
					}
				} else
					vendorExternalId = vhExternalId;

				csvBody.get(i)[vhColumnNum] = vendorExternalId;
				csvBody.get(i)[suppColumnNum] = "SP" + uniqueNum;
			}

			reader.close();

			// Write to CSV file which is open
			CSVWriter writer = new CSVWriter(new FileWriter(fileToUpdate), csvSeparator);
			writer.writeAll(csvBody);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			logger.error("Exception occurred while updating the csv file for old system id. error :{}", e.getMessage());
			e.printStackTrace();
		}
	}

	private Set<String> getVHExternalIds(List<Map<String, String>> csvDataList, String vendorExternalIdHeaderName) {
		Set<String> vendorExternalIds = new HashSet<>();

		for (int i = 0; i < csvDataList.size(); i++) {
			vendorExternalIds.add(csvDataList.get(i).get(vendorExternalIdHeaderName));
		}
		return vendorExternalIds;
	}

	private Map<String, List<String>> getVendorCreationIdList(String clientId, Set<String> vhExternalIds) {

		List<String> failedVHOldSystemIds = new ArrayList<>();
		List<String> passedVHDbIds = new ArrayList<>();
		Map<String, List<String>> resultMap = new HashMap<>();

		for (String externalId : vhExternalIds) {
			String queryForNewlyGeneratedVH = "select id from vendor where client_id =" + clientId + " and old_system_id = '" + externalId + "'";
			List<List<String>> vendorResult = executeQuery(mainAppDbHostAddress, mainAppDbPortName, mainAppDbName, mainAppDbUserName, mainAppDbPassword, queryForNewlyGeneratedVH);
			try {
				String dbId = vendorResult.get(0).get(0);
				passedVHDbIds.add(dbId);
			} catch (Exception e) {
				failedVHOldSystemIds.add(externalId);
				logger.warn("No vendor found for old_system_id = {}", externalId);
				e.printStackTrace();
			}
		}

		resultMap.put("vhCreationSuccessList", passedVHDbIds);
		resultMap.put("vhCreationFailedList", failedVHOldSystemIds);
		return resultMap;
	}

	private Map<String, String> getVhExternalIdAndDbIdMap(List<String> vendorDBIds) {

		Map<String, String> resultMap = new HashMap<>();
		for (String dbId : vendorDBIds) {
			String query = "select old_system_id from vendor where client_id =" + clientId + " and id = " + dbId;
			List<List<String>> vendorResult = executeQuery(mainAppDbHostAddress, mainAppDbPortName, mainAppDbName, mainAppDbUserName, mainAppDbPassword, query);

			String externalId = vendorResult.get(0).get(0);
			resultMap.put(externalId, dbId);
		}
		return resultMap;
	}

	private String getVendorDbIdLinkedWithSupplier(String clientId, String supplierExternalId) {
		String vendorId = null;
		String query = "select vendor_id from relation where client_id =" + clientId + " and old_system_id = '" + supplierExternalId + "'";
		List<List<String>> result = executeQuery(mainAppDbHostAddress, mainAppDbPortName, mainAppDbName, mainAppDbUserName, mainAppDbPassword, query);

		try {
			vendorId = result.get(0).get(0);
		} catch (Exception e) {
			logger.warn("No vendor id found for supplier with external id = {}", supplierExternalId);
			e.printStackTrace();
		}
		return vendorId;
	}
}
