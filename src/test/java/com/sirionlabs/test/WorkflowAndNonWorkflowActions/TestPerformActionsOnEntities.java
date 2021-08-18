package com.sirionlabs.test.WorkflowAndNonWorkflowActions;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class TestPerformActionsOnEntities extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestPerformActionsOnEntities.class);
	String entityIdMappingFileName;
	String baseFilePath;
	String performActionsOnEntitiesCfgFilePath;
	String performActionsOnEntitiesCfgFileName;

	String performActionsOnEntitiesDbIdFilePath;
	String performActionsOnEntitiesDbIdFileName;


	String entityCreationConfigFilePath;
	String entityCreationConfigFileName;


	ListRendererListData listRendererListData;

	List<String> allEntitySection;
	HashMap<String, String> entityNameIdMap;
	String entitySectionSplitter = ";";
	Boolean testForAllEntities = false;
	Show show;
	Integer entityIdToBeTested;
	//this flag will disable the worlflow action test cases , by default it will be considered as true if not there is config file
	String isworkflowActionToBePerformed;

	int pageSize;
	int maxEntitiesCount;
	String parser;

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getDataEntityClickableActions();
		testCasesMap = getTestCasesMapping();
	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}

	public void getDataEntityClickableActions() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data for EntityClickable Actions api");
		show = new Show();
		listRendererListData = new ListRendererListData();

		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

		performActionsOnEntitiesCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFilePath");
		performActionsOnEntitiesCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFileName");


		entityCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFilePath");
		entityCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityCreationConfigFileName");


		performActionsOnEntitiesDbIdFilePath = ParseConfigFile.getValueFromConfigFile(entityCreationConfigFilePath, entityCreationConfigFileName, "dumpfilepath");
		performActionsOnEntitiesDbIdFileName = ParseConfigFile.getValueFromConfigFile(entityCreationConfigFilePath, entityCreationConfigFileName, "dumpfileName");

		entityNameIdMap = new HashMap<>();

		/** old method **/
		//performActionsOnEntitiesDbIdFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesDbIdFilePath");
		//performActionsOnEntitiesDbIdFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesDbIdFileName");

		try {

			File file = new File(performActionsOnEntitiesDbIdFilePath + "/" + performActionsOnEntitiesDbIdFileName);
			Scanner sc = new Scanner(file);

			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				logger.debug("Line is : [{}]", line);
				String[] map = line.split("->");
				entityNameIdMap.put(map[0].trim(), map[1].trim());
			}

		} catch (Exception e) {
			logger.info("Error in reading or Parsing dump.txt generated from Entity Creation Job :[{}]", e.getMessage());
		}

		logger.info("EntityIdName HashMap is : [{}]", entityNameIdMap);


	}

	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	//@DataProvider(name = "TestEntityClickableActionsData")
//	public Object[][] getTestEntityClickableActionsData(ITestContext c) throws ConfigurationException {
	public Object[][] getTestEntityClickableActionsData() {

		logger.info("In the Data Provider");


		entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "entitysectionsplitter");
		pageSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "pagesize"));
		maxEntitiesCount = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "maxentitiescount"));
		parser = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "parser");

		isworkflowActionToBePerformed = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "isworkflowactiontobeperformed");

		// for getting all section
		if (!ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName,
				"testforallentities").trim().equalsIgnoreCase(""))
			testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "testforallentities"));


		if (!testForAllEntities) {
			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "entitytotest").split(entitySectionSplitter));
		} else {
			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "allentitytotest").split(entitySectionSplitter));
		}
		logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size() - 1);


		int i = 0;
		Object[][] groupArray = new Object[allEntitySection.size()][];
		for (String entitySection : allEntitySection) {
			logger.debug("entitySection :{}", entitySection);
			HashMap<String, String> hashMapforEntityConfigProperties = new HashMap<String, String>();
			groupArray[i] = new Object[5];
			int dbId = -1;

			groupArray[i][4] = dbId; // putting default invalid value to verify in case something went from configuration file
			groupArray[i][0] = null; // putting default invalid value to verify in case something went from configuration file

			try {
				List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entitySection);
				//dbId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesDbIdFilePath, performActionsOnEntitiesDbIdFileName, entitySection, "entityid"));
				dbId = Integer.parseInt(entityNameIdMap.get(entitySection));


				if (allProperties.isEmpty()) {
					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
					Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

					groupArray[i][1] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
					groupArray[i][2] = entitySectionTypeId; // Entity type Id
					groupArray[i][0] = entitySection; // EntityName
					groupArray[i][3] = entitySectionUrlId; // Entity Url Id
					groupArray[i][4] = dbId; // Entity Url Id
					i++;
					continue;
				} else {

					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
					Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

					for (String entitySpecificProperty : allProperties) {
						hashMapforEntityConfigProperties.put(entitySpecificProperty, ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entitySection, entitySpecificProperty));
					}

					groupArray[i][1] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
					groupArray[i][2] = entitySectionTypeId; // Entity type Id
					groupArray[i][0] = entitySection; // EntityName
					groupArray[i][3] = entitySectionUrlId; // Entity Url Id
					groupArray[i][4] = dbId; // Entity Url Id
					logger.info("hashMapforEntityConfigProperties: {} , entitySectionTypeId : {} , entitySection : {} , entitySectionUrlId : {} ", hashMapforEntityConfigProperties, entitySectionTypeId, entitySection, entitySectionUrlId);
					logger.info("Db Id for Testing is : {}", dbId);
					i++;
				}
			} catch (Exception e) {
				logger.error("Something Went Wrong While building Configuration for this entity {} and error is : {}", entitySection, e.getLocalizedMessage());
				i++;
			}

		}

		return groupArray;
	}


	// Test Entity Clickable Workflow Actions API Verification
//	@Test(dataProvider = "TestEntityClickableActionsData", priority = 0)
//	public void testEntityClickWorkflowActionsAPIs(String entityName, HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, Integer entitySectionUrlId, int dbId) throws Exception {
	@Test(priority = 0)
	public void testEntityClickWorkflowActionsAPIs() throws Exception {

		Object[][] grouparray = getTestEntityClickableActionsData();

		String entityName;
		HashMap<String, String> hashMapforEntityConfigProperties;
		Integer entityTypeId;
		Integer entitySectionUrlId;
		int dbId;

		//CustomCustomAssert csAssertion = new CustomCustomAssert();
		CustomAssert customAssert = new CustomAssert();
		for (int i = 0; i < grouparray.length; i++) {
			try {
				entityName = (String) grouparray[i][0];
				hashMapforEntityConfigProperties = (HashMap<String, String>) grouparray[i][1];
				entityTypeId = (Integer) grouparray[i][2];
				entitySectionUrlId = (Integer) grouparray[i][3];
				dbId = (Integer) grouparray[i][4];

				entityIdToBeTested = null;
				String entitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "url_name");

				if (isworkflowActionToBePerformed == null || Boolean.parseBoolean(isworkflowActionToBePerformed) == true) {


					if (hashMapforEntityConfigProperties != null && hashMapforEntityConfigProperties.get("validworkflowaction") != null && hashMapforEntityConfigProperties.size() != 0 && dbId != -1) {
						logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

						///String[] validActions = hashMapforEntityConfigProperties.get("validactions").split(parser);
						String validWorkFlowAction = hashMapforEntityConfigProperties.get("validworkflowaction");
						logger.debug("Valid Actions for entity [{}] are as follows ", entityName);
						//for (String validAction : validActions)
						//logger.debug("{}", validAction);

						logger.info("Workflow Action mentioned in config file is [{}] for entity [{}] ", validWorkFlowAction, entityName);


						logger.info("Performing Workflow [{}] Action on [{}] for dbId [{}] ", validWorkFlowAction, entityName, dbId);

						boolean result = EntityWorkFlowActionsHelper.performAction(validWorkFlowAction, dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
						customAssert.assertTrue(result, "Not Being able to Perform  " + validWorkFlowAction + "Action on " + entityName);

						//csAssertion.assertAll();

						logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
					} else {

						if (dbId != -1) {
							logger.info("User Needs to Specify Entity Clickables Workflow Actions  , Skipping this entity : [{}]", entityName);
							throw new SkipException("Skipping this Entity " + entityName);
						} else {
							logger.info("Dd Id is not updated in config File   , Skipping this entity : [{}]", entityName);
							throw new SkipException("Skipping this Entity " + entityName);
						}

						//logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
					}
				} else {

					logger.info("Worlfow Actions are disabled for all entity in the config File   , Skipping this entity : [{}]", entityName);
					throw new SkipException("Skipping this Entity " + entityName);


				}
			} catch (SkipException e) {

				addTestResultAsSkip(getTestCaseIdForMethodName("testEntityClickWorkflowActionsAPIs"), customAssert);
			} catch (Exception e) {
				logger.error("Exception while performing action on entity method " + e.getStackTrace());
				customAssert.assertTrue(false, "Exception while performing action on entity method ");
			}
		}
		addTestResult(getTestCaseIdForMethodName("testEntityClickWorkflowActionsAPIs"), customAssert);
		customAssert.assertAll();
	}


	// Test Entity Clickable Non Workflow Actions API Verification
//	@Test(dataProvider = "TestEntityClickableActionsData", priority = 1)
//	public void testEntityClickNonWorkflowActionsAPIs(String entityName, HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, Integer entitySectionUrlId, int dbId) throws Exception {
	@Test()
	public void testEntityClickNonWorkflowActionsAPIs() throws Exception {

		Object[][] groupArray = getTestEntityClickableActionsData();

		CustomAssert customAssert = new CustomAssert();
		String entityName = "";
		HashMap<String, String> hashMapforEntityConfigProperties;
		Integer entityTypeId;
		Integer entitySectionUrlId;
		int dbId;

		for (int i = 0; i < groupArray.length; i++) {
			try {
				entityName = (String) groupArray[i][0];
				hashMapforEntityConfigProperties = (HashMap<String, String>) groupArray[i][1];
				entityTypeId = (Integer) groupArray[i][2];
				entitySectionUrlId = (Integer) groupArray[i][3];
				dbId = (Integer) groupArray[i][4];

				entityIdToBeTested = null;
				String entitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "url_name");

				if (hashMapforEntityConfigProperties != null && hashMapforEntityConfigProperties.get("validnonworkflowaction") != null && hashMapforEntityConfigProperties.size() != 0 && dbId != -1) {
					logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

					String[] validActions = hashMapforEntityConfigProperties.get("validactions").split(parser);
					String validNonWorkFlowAction = hashMapforEntityConfigProperties.get("validnonworkflowaction");
					logger.debug("Valid Actions for entity [{}] are as follows ", entityName);
					for (String validAction : validActions)
						logger.debug("{}", validAction);

					logger.info("Non Workflow Action mentioned in config file is [{}] for entity [{}] ", validNonWorkFlowAction, entityName);


					logger.info("Performing non-Workflow [{}] Action on [{}] for dbId [{}] ", validNonWorkFlowAction, entityName, dbId);


					if (validNonWorkFlowAction.toLowerCase().contentEquals("archive")) {

						boolean result = EntityWorkFlowActionsHelper.performAction(validNonWorkFlowAction, dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
						customAssert.assertTrue(result, "Not Being able to Perform  " + validNonWorkFlowAction + " Action on " + entityName);

						if (result) {
							// will do counter action now
							result = EntityWorkFlowActionsHelper.performAction("restore", dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
							customAssert.assertTrue(result, "Not Being able to Perform  " + "restore" + " Action on " + entityName);

						}

					} else if (validNonWorkFlowAction.toLowerCase().contentEquals("onhold")) {

						boolean result = EntityWorkFlowActionsHelper.performAction(validNonWorkFlowAction, dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
						customAssert.assertTrue(result, "Not Being able to Perform  " + validNonWorkFlowAction + " Action on " + entityName);

						if (result) {
							result = EntityWorkFlowActionsHelper.performAction("nonworkflowactivate", dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
							customAssert.assertTrue(result, "Not Being able to Perform  " + "activate" + " Action on " + entityName);
						}

					} else {

						boolean result = EntityWorkFlowActionsHelper.performAction(validNonWorkFlowAction, dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);
						customAssert.assertTrue(result, "Not Being able to Perform  " + validNonWorkFlowAction + " Action on " + entityName);


					}


					// finally deleting the entry using the logic written in TestEntityCreation
					EntityOperationsHelper.deleteEntityRecord(entityName, dbId);

					//csAssertion.assertAll();

					logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
				} else {
					if (dbId != -1) {
						logger.info("User Needs to Specify Entity Clickables Non Workflow Actions  , Skipping this entity : [{}]", entityName);
						EntityOperationsHelper.deleteEntityRecord(entityName, dbId);
						throw new SkipException("Skipping this Entity " + entityName);
					} else {
						logger.info("Dd Id is not updated in config File   , Skipping this entity : [{}]", entityName);
						throw new SkipException("Skipping this Entity " + entityName);
					}
					//logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
				}
			} catch (SkipException e) {

				addTestResultAsSkip(getTestCaseIdForMethodName("testEntityClickNonWorkflowActionsAPIs"), customAssert);
			} catch (Exception e) {
				logger.error("Exception while testing for entity {} : {} ", entityName, e.getStackTrace());
				customAssert.assertTrue(false, "Exception while testing for entity " + entityName + e.getStackTrace());
			}
		}
		addTestResult(getTestCaseIdForMethodName("testEntityClickNonWorkflowActionsAPIs"), customAssert);
		customAssert.assertAll();
	}


	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");
	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
		logger.info("Deleting Dump.txt");
		FileUtils.deleteFile(performActionsOnEntitiesDbIdFilePath + "/" + performActionsOnEntitiesDbIdFileName);

	}

}
