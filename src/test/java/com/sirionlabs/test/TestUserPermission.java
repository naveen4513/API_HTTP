package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.EntityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

/**
 * Created by akshay.rohilla on 7/5/2017.
 */
public class TestUserPermission {

	private final static Logger logger = LoggerFactory.getLogger(TestUserPermission.class);
	private static String delimiterForFieldsToTest = null;
	private static String configFilePath;
	private static String configFileName;
	private static String csvFilePath = null;
	private static String csvFileName = null;
	private DumpResultsIntoCSV dumpResultsObj;

	@BeforeClass
	public void setStaticFields() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestPropertiesFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("UserPermissionConfigFileName");
		delimiterForFieldsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DelimiterForFields").trim();
		csvFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "csvfilepath");
		csvFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "csvfilename") + ".csv";
		dumpResultsObj = new DumpResultsIntoCSV(csvFilePath, csvFileName, setHeadersInCSVFile());
	}

	@AfterClass
	public void afterClass() throws IOException, ConfigurationException {
		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "convertcsvtoxlsx").trim().equalsIgnoreCase("yes")) {
			String csvDelimiter = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVDelimiter");
			XLSUtils.convertCSVToXLSX(csvFilePath, csvFileName, csvFilePath, csvFileName, csvDelimiter);
			File csvFile = new File(csvFilePath + "/" + csvFileName);
			csvFile.delete();
		}
	}

	@DataProvider(parallel = false)
	public Object[][] dataProviderForUserPermission() throws ConfigurationException {
		logger.info("Setting all Entities to Test.");
		List<Object[]> allTestData = new ArrayList<>();
		String[] entities = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiestotest")
				.split(Pattern.quote(delimiterForFieldsToTest));
		for (String entity : entities) {
			allTestData.add(new Object[]{entity.trim()});
		}
		logger.info("Total Entities to Test: {}", allTestData.size());
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForUserPermission", enabled = true)
	public void testUserPermission(String entityName) {
		CustomAssert csAssert = new CustomAssert();
		Map<String, String> resultsMap = new HashMap<>();
		try {
			resultsMap.put("entityName", entityName);
			logger.info("Verifying User Permission for Entity {}", entityName);
			int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFileName"), entityName));
			resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
			Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, entityName);
			this.verifyReadInclusion(entityName, entityTypeId, properties, csAssert);
			this.verifyReadExclusion(entityName, entityTypeId, properties, csAssert);
			this.verifyWriteInclusion(entityName, properties, csAssert);
			this.verifyWriteExclusion(entityName, properties, csAssert);
		} catch (Exception e) {
			logger.error("Exception while Verifying User Permission for Entity {}. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying User Permission for Entity " + entityName + ". " + e.getMessage());
			resultsMap.put("result", "fail");
			resultsMap.put("exception", e.getMessage());
			dumpResultsIntoCSV(resultsMap);
		}
		csAssert.assertAll();
	}

	private void verifyReadInclusion(String entityName, int entityTypeId, Map<String, String> properties, CustomAssert csAssert) throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();
		logger.info("Setting Read Included Items for Entity {}", entityName);
		String includedItems[] = properties.get("readincludeditems").split(Pattern.quote(delimiterForFieldsToTest));

		for (int i = 0; i < includedItems.length; i++) {
			final int index = i;
			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() {
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
						int entityId = Integer.parseInt(includedItems[index].trim());
						resultsMap.put("includedItemId", Integer.toString(entityId));
						resultsMap.put("readInclusionPass", "true");
						logger.info("Hitting Show Api for Record #{} of Entity {} having Id {}", (index + 1), entityName, entityId);
						Show showObj = new Show();
						showObj.hitShow(entityTypeId, entityId);
						String showJsonStr = showObj.getShowJsonStr();
						logger.info("Verifying that Show Page is accessible for Record #{} of Entity {} having Id {}", (index + 1), entityName, entityId);
						boolean casePass = showObj.isShowPageAccessible(showJsonStr);
						if (!casePass) {
							csAssert.assertTrue(false, "Show Page is not accessible for Record #" + (index + 1) + " of Entity " + entityName +
									" having Id " + entityId);
							resultsMap.put("result", "fail");
							resultsMap.put("comments", "Show Page is not accessible for Record #" + (index + 1) + " of Entity " + entityName + " having Id " + entityId);
							resultsMap.put("readInclusionPass", "false");
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Read Inclusion for Entity {}. {}", entityName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying Read Inclusion for Entity " + entityName + ". " + e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("exception", e.getMessage());
						resultsMap.put("readInclusionPass", "false");
					}
					dumpResultsIntoCSV(resultsMap);
					return true;
				}
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();
	}

	private void verifyReadExclusion(String entityName, int entityTypeId, Map<String, String> properties, CustomAssert csAssert) throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();
		logger.info("Setting Read Excluded Items for Entity {}", entityName);
		String excludedItems[] = properties.get("readexcludeditems").split(Pattern.quote(delimiterForFieldsToTest));

		for (int i = 0; i < excludedItems.length; i++) {
			final int index = i;

			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(entityTypeId));
						int entityId = Integer.parseInt(excludedItems[index].trim());
						resultsMap.put("excludedItemId", Integer.toString(entityId));
						resultsMap.put("readExclusionPass", "true");
						logger.info("Hitting Show Api for Record #{} of Entity {} having Id {}", (index + 1), entityName, entityId);
						Show showObj = new Show();
						showObj.hitShow(entityTypeId, entityId);
						String showJsonStr = showObj.getShowJsonStr();
						logger.info("Verifying that Show Page is not accessible for Record #{} of Entity {} having Id {}", (index + 1), entityName, entityId);
						boolean casePass = showObj.isShowPageBlocked(showJsonStr);
						if (!casePass) {
							csAssert.assertTrue(false, "Show Page is Accessible for Record #" + (index + 1) + " of Entity " + entityName +
									" having Id " + entityId);
							resultsMap.put("result", "fail");
							resultsMap.put("comments", "Show Page is Accessible for Record #" + (index + 1) + " of Entity " + entityName + " having Id " + entityId);
							resultsMap.put("readExclusionPass", "false");
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Read Exclusion for Entity {}. {}", entityName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying Read Exclusion for Entity " + entityName + ". " + e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("exception", e.getMessage());
						resultsMap.put("readExclusionPass", "false");
					}
					dumpResultsIntoCSV(resultsMap);
					return true;
				}
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();
	}

	private void verifyWriteInclusion(String entityName, Map<String, String> properties, CustomAssert csAssert) throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();
		logger.info("Setting Write Included Items for Entity {}", entityName);
		String includedItems[] = properties.get("writeincludeditems").split(Pattern.quote(delimiterForFieldsToTest));

		for (int i = 0; i < includedItems.length; i++) {
			final int index = i;

			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(ConfigureConstantFields.getEntityIdByName(entityName)));
						int entityId = Integer.parseInt(includedItems[index].trim());
						resultsMap.put("includedItemId", Integer.toString(entityId));
						resultsMap.put("writeInclusionPass", "true");
						logger.info("Hitting Edit Api for Record #{} of Entity {} having Id {}", index, entityName, entityId);
						Edit editObj = new Edit();
						boolean casePass = ParseJsonResponse.successfulResponse(editObj.hitEdit(entityName, entityId));
						if (!casePass) {
							csAssert.assertTrue(false, "Edit Permission Denied for Record #" + (index + 1) + " of Entity " + entityName +
									" having Id " + entityId);
							resultsMap.put("result", "fail");
							resultsMap.put("comments", "Edit Permission Denied for Record #" + (index + 1) + " of Entity " + entityName + " having Id " + entityId);
							resultsMap.put("writeInclusionPass", "false");
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Write Inclusion for Entity {}. {}", entityName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying Write Inclusion for Entity " + entityName + ". " + e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("exception", e.getMessage());
						resultsMap.put("writeInclusionPass", "false");
					}
					dumpResultsIntoCSV(resultsMap);
					return true;
				}
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();

		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "createpermission").equalsIgnoreCase("yes")) {
			Map<String, String> resultsMap = new HashMap<>();
			resultsMap.put("entityName", entityName);
			resultsMap.put("entityTypeId", Integer.toString(ConfigureConstantFields.getEntityIdByName(entityName)));
			resultsMap.put("createPermissionPass", "true");
			logger.info("Creating Payload for Entity {}", entityName);
			CreateEntity createEntityObj = new CreateEntity();
			String createPayload = createEntityObj.getCreatePayload(entityName, properties);
			logger.info("Hitting Create Api for Entity {}", entityName);
			Create createObj = new Create();
			createObj.hitCreate(entityName, createPayload);
			if (!ParseJsonResponse.successfulResponse(createObj.getCreateJsonStr())) {
				csAssert.assertTrue(false, "Entity Creation Failed for Entity " + entityName);
				resultsMap.put("result", "fail");
				resultsMap.put("comments", "Entity Creation Failed for Entity " + entityName);
				resultsMap.put("createPermissionPass", "false");
			}
			dumpResultsIntoCSV(resultsMap);
		}
	}

	private void verifyWriteExclusion(String entityName, Map<String, String> properties, CustomAssert csAssert) throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
		List<FutureTask<Boolean>> taskList = new ArrayList<>();
		logger.info("Setting Write Excluded Items for Entity {}", entityName);
		String excludedItems[] = properties.get("writeexcludeditems").split(TestUserPermission.delimiterForFieldsToTest);

		for (int i = 0; i < excludedItems.length; i++) {
			final int index = i;

			FutureTask<Boolean> result = new FutureTask<>(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					Map<String, String> resultsMap = new HashMap<>();
					try {
						resultsMap.put("entityName", entityName);
						resultsMap.put("entityTypeId", Integer.toString(ConfigureConstantFields.getEntityIdByName(entityName)));
						int entityId = Integer.parseInt(excludedItems[index].trim());
						resultsMap.put("excludedItemId", Integer.toString(entityId));
						resultsMap.put("writeExclusionPass", "true");
						logger.info("Hitting Edit Api for Record #{} of Entity {} having Id {}", index, entityName, entityId);
						Edit editObj = new Edit();
						if (!ParseJsonResponse.hasPermissionError(editObj.hitEdit(entityName, entityId))) {
							csAssert.assertTrue(false, "Edit Permission Allowed for Record #" + (index + 1) + " of Entity " + entityName +
									" having Id " + entityId);
							resultsMap.put("result", "fail");
							resultsMap.put("comments", "Edit Permission Allowed for Record #" + (index + 1) + " of Entity " + entityName + " having Id " + entityId);
							resultsMap.put("writeExclusionPass", "false");
						}
					} catch (Exception e) {
						logger.error("Exception while Verifying Write Exclusion for Entity {}. {}", entityName, e.getStackTrace());
						csAssert.assertTrue(false, "Exception while Verifying Write Exclusion for Entity " + entityName + ". " + e.getMessage());
						resultsMap.put("result", "fail");
						resultsMap.put("exception", e.getMessage());
						resultsMap.put("writeExclusionPass", "false");
					}
					dumpResultsIntoCSV(resultsMap);
					return true;
				}
			});
			taskList.add(result);
			executor.execute(result);
		}
		for (FutureTask<Boolean> task : taskList)
			task.get();

		if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "createpermission").equalsIgnoreCase("yes")) {
			Map<String, String> resultsMap = new HashMap<>();
			resultsMap.put("entityName", entityName);
			resultsMap.put("entityTypeId", Integer.toString(ConfigureConstantFields.getEntityIdByName(entityName)));
			logger.info("Creating Payload for Entity {}", entityName);
			CreateEntity createEntityObj = new CreateEntity();
			String createPayload = createEntityObj.getCreatePayload(entityName, properties);
			logger.info("Hitting Create Api for Entity {}", entityName);
			Create createObj = new Create();
			createObj.hitCreate(entityName, createPayload);
			if (!ParseJsonResponse.hasPermissionError(createObj.getCreateJsonStr())) {
				csAssert.assertTrue(false, "Entity Creation Allowed for Entity " + entityName);
				resultsMap.put("result", "fail");
				resultsMap.put("comments", "Entity Creation Allowed for Entity " + entityName);
				resultsMap.put("createPermissionPass", "false");
			}
			dumpResultsIntoCSV(resultsMap);
		}
	}

	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<>();
		String allColumns[] = {"entityTypeId", "entityName", "includedItemId", "excludedItemId", "readInclusionPass", "readExclusionPass", "writeInclusionPass",
				"writeExclusionPass", "createPermissionPass", "result", "exception", "comments"};
		headers.addAll(Arrays.asList(allColumns));
		return headers;
	}

	private void dumpResultsIntoCSV(Map<String, String> resultsMap) {
		if (!resultsMap.containsKey("result"))
			resultsMap.put("result", "pass");
		dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
	}
}