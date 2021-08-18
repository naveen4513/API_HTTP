package com.sirionlabs.test.WorkflowAndNonWorkflowActions;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by shivashish on 17/7/17.
 */
public class TestEntityClickableActions extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestEntityClickableActions.class);
	String entityIdMappingFileName;
	String baseFilePath;
	String entityClickableActionsCfgFilePath;
	String entityClickableActionsCfgFileName;
	String entityIdtoBeDeleted = null;
	String entityCloneAPIResponse;
	ListRendererListData listRendererListData;
	HashMap<String, HashMap<String, String>> hashMapForClickableActions;
	List<String> allEntitySection;
	String entitySectionSplitter = ";";
	Boolean testForAllEntities = false;
	Show show;
	String showAPIResponse;
	Integer entityIdToBeTested;


	DumpResultsIntoCSV dumpResultsObj;
	String TestResultCSVFilePath;
	int globalIndex = 0;


	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "TestMethodName", "TestMethodResult", "Comments", "Message"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}


	@BeforeClass
	public void beforeClass() throws IOException {
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

	public void getDataEntityClickableActions() throws ParseException, IOException {
		logger.info("Getting Test Data for EntityClickable Actions api");
		show = new Show();
		listRendererListData = new ListRendererListData();

		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

		entityClickableActionsCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("entityClickableActionsCfgFilePath");
		entityClickableActionsCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("entityClickableActionsCfgFileName");


		// for Storing the result of Sorting
		int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
		String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
		TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className;
		logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
		dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());


	}

	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
//	@DataProvider(name = "TestEntityClickableActionsData")
//	public Object[][] getTestEntityClickableActionsData(ITestContext c) throws ConfigurationException {
//
//		logger.info("In the Data Provider");
//		int i = 0;
//
//
//		entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "entitysectionsplitter");
//		// for getting all section
//		if (!ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName,
//				"testforallentities").trim().equalsIgnoreCase(""))
//			testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "testforallentities"));
//
//
//		if (!testForAllEntities) {
//			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "entitytotest").split(entitySectionSplitter));
//		} else {
//			allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "allentitytotest").split(entitySectionSplitter));
//		}
//		logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size() - 1);
//
//
//		Object[][] groupArray = new Object[allEntitySection.size()][];
//		for (String entitySection : allEntitySection) {
//			logger.debug("entitySection :{}", entitySection);
//			HashMap<String, String> hashMapforEntityConfigProperties = new HashMap<String, String>();
//			groupArray[i] = new Object[3];
//
//			List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, entitySection);
//			if (allProperties.isEmpty()) {
//				Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
//				groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
//				groupArray[i][1] = entitySectionTypeId; // Entity type Id
//				groupArray[i][2] = entitySection; // EntityName
//				i++;
//				continue;
//			} else {
//
//				Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
//
//				for (String entitySpecificProperty : allProperties) {
//					hashMapforEntityConfigProperties.put(entitySpecificProperty, ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, entitySection, entitySpecificProperty));
//				}
//
//				groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
//				groupArray[i][1] = entitySectionTypeId; // Entity type Id
//				groupArray[i][2] = entitySection; // EntityName
//				logger.info("hashMapforEntityConfigProperties: {} , entityShowPageUrlId : {} , entitySection : {}  ", hashMapforEntityConfigProperties, entitySectionTypeId, entitySection);
//				i++;
//			}
//		}
//
//		return groupArray;
//	}


	// Test Entity Clickable Actions APIs
//	@Test(dataProvider = "TestEntityClickableActionsData")
//	public void testEnityClickActionsAPIs(HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, String entityName) throws Exception {//	@Test(dataProvider = "TestEntityClickableActionsData")
	@Test()
	public void testEnityClickActionsAPIs() throws Exception {

		logger.info("In the Data Provider");
		int i = 0;

		CustomAssert customAssert = new CustomAssert();

		try {
			entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "entitysectionsplitter");
			// for getting all section
			if (!ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName,
					"testforallentities").trim().equalsIgnoreCase(""))
				testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "testforallentities"));


			if (!testForAllEntities) {
				allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "entitytotest").split(entitySectionSplitter));
			} else {
				allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, "allentitytotest").split(entitySectionSplitter));
			}
			logger.info("allEntitySection :{} , allEntitySection.size() : {}", allEntitySection, allEntitySection.size() - 1);


			Object[][] groupArray = new Object[allEntitySection.size()][];
			for (String entitySection : allEntitySection) {
				logger.debug("entitySection :{}", entitySection);
				HashMap<String, String> hashMapforEntityConfigProperties = new HashMap<String, String>();
				groupArray[i] = new Object[3];

				List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, entitySection);
				if (allProperties.isEmpty()) {
					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
					groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
					groupArray[i][1] = entitySectionTypeId; // Entity type Id
					groupArray[i][2] = entitySection; // EntityName
					i++;
					continue;
				} else {

					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));

					for (String entitySpecificProperty : allProperties) {
						hashMapforEntityConfigProperties.put(entitySpecificProperty, ParseConfigFile.getValueFromConfigFile(entityClickableActionsCfgFilePath, entityClickableActionsCfgFileName, entitySection, entitySpecificProperty));
					}

					groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
					groupArray[i][1] = entitySectionTypeId; // Entity type Id
					groupArray[i][2] = entitySection; // EntityName
					logger.info("hashMapforEntityConfigProperties: {} , entityShowPageUrlId : {} , entitySection : {}  ", hashMapforEntityConfigProperties, entitySectionTypeId, entitySection);
					i++;
				}
			}


			entityIdToBeTested = null;

			HashMap<String, String> hashMapforEntityConfigProperties;
			Integer entityTypeId;
			String entityName;

			for (i = 0; i < groupArray.length; i++) {
				hashMapforEntityConfigProperties = (HashMap<String, String>) groupArray[i][0];
				entityTypeId = (Integer) groupArray[i][1];
				entityName = (String) groupArray[i][2];

				if (hashMapforEntityConfigProperties.size() != 0) {
					logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
					verifyEntityClickableActionsStatusCode(hashMapforEntityConfigProperties, entityTypeId, entityName, customAssert);
					verifyEntityClickableActionsExistence(hashMapforEntityConfigProperties, customAssert);
					verifyEntityClickableActionsPermission(hashMapforEntityConfigProperties, entityTypeId, customAssert);

					logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
				} else {
					logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
					customAssert.assertTrue(false, "User Needs to Specify Entity Clickables Actions with Permission in Config file , Skipping this entity :" + entityName);

					logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
				}
			}
		} catch (Exception e) {
			logger.error("Exception. " + e.getMessage());
			customAssert.assertTrue(false, "Exception. " + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testEnityClickActionsAPIs"), customAssert);
		customAssert.assertAll();
	}


	//  this function will return all the Entities Ids from their List Page
	public HashSet<Integer> getHashSetOfEntityIds(String entitySection) throws Exception {
		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
		List<Integer> allDBIds;
		HashSet<Integer> hashSetofEntityId = new HashSet<>();
		String listDataResponse = listRendererListData.listDataResponse(entityTypeId, entitySection);
		logger.debug("List Data API Response : entity={} , response={}", entitySection, listDataResponse);

		boolean isListDataValidJson = APIUtils.validJsonResponse(listDataResponse);
		Assert.assertTrue(isListDataValidJson, "List Entity API Response is Not Valid");

		JSONObject listDataResponseObj = new JSONObject(listDataResponse);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		if (noOfRecords > 0) {
			listRendererListData.setListData(listDataResponse);
			int columnId = listRendererListData.getColumnIdFromColumnName("id");
			allDBIds = listRendererListData.getAllRecordDbId(columnId, listDataResponse);
			for (Integer dbId : allDBIds) {
				hashSetofEntityId.add(dbId);
			}
		}
		return hashSetofEntityId;
	}

	//Verification of Status Code  for All Entity
	public void verifyEntityClickableActionsStatusCode(HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, String entityName, CustomAssert csAssertion) throws Exception {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse responseShowPage = null;
		if (hashMapforEntityConfigProperties.containsKey("entityid")) {
			entityIdToBeTested = Integer.parseInt(hashMapforEntityConfigProperties.get("entityid"));
			responseShowPage = show.hitShowGetAPI(entityTypeId, entityIdToBeTested);
			csAssertion.assertTrue(responseShowPage.getStatusLine().toString().contains("200"), "Error :Show Page API Response is Incorrect");
		} else {
			// todo: get the entityId from Vijay's List RenderedListData Class Method - done
			HashSet<Integer> hashSetofEntityId = getHashSetOfEntityIds(entityName);
			if (hashSetofEntityId.isEmpty()) {
				//  CSV generation Code Starts Here
				Map<String, String> resultsMap = new HashMap<String, String>();
				resultsMap.put("Index", String.valueOf(++globalIndex));
				resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
				resultsMap.put("TestMethodResult", "Fail");
				resultsMap.put("Comments", "Neither List Page of Entity : " + entityName + "  has anything nor Config File has any Entity Id");
				resultsMap.put("Message", "NA");
				dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
				//  CSV generation Code Ends Here

				Assert.fail("Neither List Page of Entity : " + entityName + "  has anything nor Config File has any Entity Id");
			} else {

				// randomly picking id to for testing workflow action
				int size = hashSetofEntityId.size();
				int item = new Random().nextInt(size); // In real life, the Random object should be rather more shared than this
				int i = 0;
				for (Integer id : hashSetofEntityId) {
					if (i == item)
						entityIdToBeTested = id;
					i++;
				}

				//entityIdToBeTested = hashSetofEntityId.iterator().next(); // Will Test Only for First Id of Show Page
				responseShowPage = show.hitShowGetAPI(entityTypeId, entityIdToBeTested);
				csAssertion.assertTrue(responseShowPage.getStatusLine().toString().contains("200"), "Error :Show Page API Response is Incorrect");
			}
		}

		logger.info("enitityIdTobeTested is : [{}]", entityIdToBeTested);
		showAPIResponse = show.getShowJsonStr();

		logger.debug("Response Status Line is :{}", responseShowPage.getStatusLine().toString());
		logger.debug("Response Locale Line is :{}", responseShowPage.getLocale().toString());
		logger.info("Response Payload is :{}", showAPIResponse);

		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");

	}

	//Verification of Existence of All Clickable Action specify in Config file
	public void verifyEntityClickableActionsExistence(HashMap<String, String> hashMapforEntityConfigProperties, CustomAssert csAssertion) {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		hashMapForClickableActions = show.getLayOutInfoActionsHashMap(showAPIResponse);
		logger.info("hashMapForClickableActions :{}", hashMapForClickableActions);

		for (String key : hashMapforEntityConfigProperties.keySet()) {
			logger.info("key to validate is :{}", key);
			if (key.contentEquals("entityid"))
				continue;
			if (hashMapforEntityConfigProperties.get(key).contentEquals("true")) {
				if (!hashMapForClickableActions.containsKey(key)) {
					//  CSV generation Code Starts Here
					Map<String, String> resultsMap = new HashMap<String, String>();
					resultsMap.put("Index", String.valueOf(++globalIndex));
					resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
					resultsMap.put("TestMethodResult", "Fail");
					resultsMap.put("Comments", "Error: Layout Info Doesn't has Clickable Action : " + key + " as specified in Config File");
					resultsMap.put("Message", "NA");
					dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
					//  CSV generation Code Ends Here
				}

				Assert.assertTrue(hashMapForClickableActions.containsKey(key), "Error: Layout Info Doesn't has Clickable Action : " + key + " as specified in Config File");
			}
		}
		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");


	}

	//Verification of Clickable Actions Permissions based on Config File
	public void verifyEntityClickableActionsPermission(HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, CustomAssert csAssertion) throws Exception {

		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
		String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
		String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		String urlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName, "url_name");


		for (String key : hashMapforEntityConfigProperties.keySet()) {
			logger.info("key to validate is :{}", key);
			if (key.contentEquals("entityid"))
				continue;

			// if Client admin has given the permission for perform clickable action
			if (hashMapforEntityConfigProperties.get(key).contentEquals("true")) {

				if (hashMapForClickableActions.containsKey(key)
						&& hashMapForClickableActions.get(key).containsKey("type")
						&& hashMapForClickableActions.get(key).get("type").toLowerCase().contentEquals("button")) {

					// this will update the payload for each actions
					show.hitShowGetAPI(entityTypeId, entityIdToBeTested);


					JSONObject showPageResponse = new JSONObject(show.getShowJsonStr());
					int historyFieldLength = showPageResponse.getJSONObject("body").getJSONObject("data").getJSONObject("history").getJSONArray("status").length();
					if (historyFieldLength > 75)  // hack to avoid failure in case of SIR-134318
					{
						logger.info("Here bread-crum size has become too long for  this entity : [{}] so skipping it beacause of tech-debt SIR-134318", entityName);
						throw new SkipException("Skipping this Entity " + entityName);
					}


					// if Action Name is Archive then we need to do Restore
					if (hashMapForClickableActions.get(key).get("name").toLowerCase().contentEquals("archive")) {
						logger.info("------------------------------------------------ Archive Action Start From Here -------------------------------------------------");
						String uRI = hashMapForClickableActions.get(key).get("api");
						String counterURI = hashMapForClickableActions.get(key).get("api").replace("archive", "restore");

						boolean result = validateArchiveActionPermission(true, uRI, hashMapForClickableActions.get(key).get("requestType"));
						csAssertion.assertTrue(result, "Error : Not being able to perform action : " + hashMapForClickableActions.get(key).get("name"));
						show.hitShowGetAPI(entityTypeId, entityIdToBeTested);  // this will update the payload for each actions
						if (result == true)
							csAssertion.assertTrue(validateRestoreActionPermission(true, counterURI, hashMapForClickableActions.get(key).get("requestType")), "Error : Not being able to perform action : " + "restore");
					}

					// if Action Name is onHold then we need to do Resume
					if (hashMapForClickableActions.get(key).get("name").toLowerCase().contentEquals("onhold")) {
						logger.info("------------------------------------------------ OnHold Action Start From Here -------------------------------------------------");
						String uRI = hashMapForClickableActions.get(key).get("api");
						String counterURI = hashMapForClickableActions.get(key).get("api").replace("onhold", "activate");

						boolean result = validateOnHoldActionPermission(true, uRI, hashMapForClickableActions.get(key).get("requestType"));
						csAssertion.assertTrue(result, "Error : Not being able to perform action : " + hashMapForClickableActions.get(key).get("name"));
						show.hitShowGetAPI(entityTypeId, entityIdToBeTested);  // this will update the payload for each actions
						if (result == true)
							csAssertion.assertTrue(validateActivateActionPermission(true, counterURI, hashMapForClickableActions.get(key).get("requestType")), "Error : Not being able to perform action : " + "activate");
					}

					// if Action Name is clone then we need to do Create
					if (hashMapForClickableActions.get(key).get("name").toLowerCase().contentEquals("clone")) {
						logger.info("------------------------------------------------ Clone Action Start From Here -------------------------------------------------");
						int endIndex = hashMapForClickableActions.get(key).get("api").replace("clone", "create").lastIndexOf("/");
						String uRI = hashMapForClickableActions.get(key).get("api");
						String counterURI = hashMapForClickableActions.get(key).get("api").replace("clone", "create").substring(0, endIndex);

						boolean result = validateCloneActionPermission(true, uRI, hashMapForClickableActions.get(key).get("requestType"));
						csAssertion.assertTrue(result, "Error : Not being able to perform action : " + hashMapForClickableActions.get(key).get("name"));
						if (result == true)
							csAssertion.assertTrue(validateCreateActionPermission(true, counterURI, "post"), "Error : Not being able to perform action : " + "Create even though Clone is Enable");
					}

				}
			}

			// if Client admin has not given the permission for perform clickable action
			if (hashMapforEntityConfigProperties.get(key).contentEquals("false")) {

				// this will update the payload for each actions
				show.hitShowGetAPI(entityTypeId, entityIdToBeTested);

				// if Action Name is Archive
				if (key.contentEquals("archive")) {
					logger.info("------------------------------------------------ Archive Action Start From Here -------------------------------------------------");
					String uRI = "/" + urlName + "/archive";
					csAssertion.assertTrue(validateArchiveActionPermission(false, uRI, "post"), "Error : Should Not be able to perform action : " + key);
				}

				// if Action Name is onHold
				if (key.contentEquals("onhold")) {
					logger.info("------------------------------------------------ OnHold Action Start From Here -------------------------------------------------");
					String uRI = "/" + urlName + "/onhold";
					csAssertion.assertTrue(validateOnHoldActionPermission(false, uRI, "post"), "Error : Should Not be able to perform action : " + key);
				}

				// if Action Name is clone
				if (key.contentEquals("clone")) {
					logger.info("------------------------------------------------ Clone Action Start From Here -------------------------------------------------");
					String uRI = "/" + urlName + "/clone/" + hashMapforEntityConfigProperties.get("entityid");
					csAssertion.assertTrue(validateCloneActionPermission(false, uRI, "get"), "Error : Should Not be able to perform action : " + key);
				}

			}


		}

		// Special Case for Delete Actions Since It should be perform after every actions
		if (hashMapforEntityConfigProperties.keySet().contains("delete")) {
			if (hashMapforEntityConfigProperties.get("delete").contentEquals("true")) {

				if (hashMapForClickableActions.containsKey("delete")
						&& hashMapForClickableActions.get("delete").containsKey("type")
						&& hashMapForClickableActions.get("delete").get("type").toLowerCase().contentEquals("button")) {
					String uRI = hashMapForClickableActions.get("delete").get("api");
					csAssertion.assertTrue(validateDeleteActionPermission(true, entityTypeId, uRI, hashMapForClickableActions.get("delete").get("requestType"), entityIdToBeTested.toString()), "Error : Not being able to perform action : " + hashMapForClickableActions.get("delete").get("name"));// this will update the payload for each actions


				}

			}
			// if Client admin has not given the permission for perform delete action
			if (hashMapforEntityConfigProperties.get("delete").contentEquals("false")) {
				String uRI = "/" + urlName + "/delete";
				csAssertion.assertTrue(validateDeleteActionPermission(false, entityTypeId, uRI, "post", entityIdToBeTested.toString()), "Error : Not being able to perform action : " + "delete");// this will update the payload for each actions

			}
		}

		logger.info("------------------------------------------------ Method Ends Here -------------------------------------------------");


	}


	// Validate Archive Action
	public boolean validateArchiveActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = show.getShowJsonStr();
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("Archive Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		// if we have permission to perform actions , API response should have status=success
		if (permissionToExecute && status.contentEquals("success")) {
			return true;
		}

		// if we do have permission to perform actions , API response should have status=applicationError
		if (!permissionToExecute && status.contentEquals("applicationError")) {
			return true;
		}


		return false;
	}

	// Validate Restore Action
	public boolean validateRestoreActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = show.getShowJsonStr();
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("Restore Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		if (permissionToExecute && status.contentEquals("success")) {
			return true;
		}


		return false;
	}

	// Validate OnHold Action
	public boolean validateOnHoldActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = show.getShowJsonStr();
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("OnHold Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		// if we have permission to perform actions , API response should have status=success
		if (permissionToExecute && status.contentEquals("success")) {
			return true;
		}

		// if we do have permission to perform actions , API response should have status=applicationError
		if (!permissionToExecute && status.contentEquals("applicationError")) {
			return true;
		}


		return false;
	}

	// Validate Activate Action
	public boolean validateActivateActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = show.getShowJsonStr();
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("Activate Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		if (permissionToExecute && status.contentEquals("success")) {
			return true;
		}

		return false;
	}

	// Validate Clone Action
	public boolean validateCloneActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = show.getShowJsonStr();
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("Clone Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		// if we have permission to perform actions , API response should have status=success
		if (permissionToExecute && status.contentEquals("success")) {


			if (uriForClickableActions.contains("clone")) {
				entityCloneAPIResponse = responseShowPageActionApi;
				logger.info("entityCloneAPIResponse:{}", entityCloneAPIResponse);
			}
			return true;
		}


		// if we do have permission to perform actions , API response should have status=applicationError
		if (!permissionToExecute && status.contentEquals("applicationError")) {
			return true;
		}

		return false;
	}

	// Validate Create Action
	public boolean validateCreateActionPermission(boolean permissionToExecute, String uriForClickableActions, String requestType) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());

		String payload = entityCloneAPIResponse;
		logger.info("Create API payload is : {}", payload);
		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload);
		logger.info("Create Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		if (permissionToExecute && status.contentEquals("success")) {
			if (uriForClickableActions.contains("create")) {
				String StringForIdtoBeDeleted = jsonObj.getJSONObject("header").getJSONObject("response").getJSONObject("properties").get("notification").toString();
				entityIdtoBeDeleted = StringForIdtoBeDeleted.split("/")[3].split("\"")[0];
				logger.info("entityIdtoBeDeleted:{}", entityIdtoBeDeleted);
			}
			return true;
		}

		return false;
	}

	// Validate Delete Action
	public boolean validateDeleteActionPermission(boolean permissionToExecute, Integer entityTypeId, String uriForClickableActions, String requestType, String entityId) throws Exception {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());


		String payloadString = "{\n" +
				"  \"body\": {\n" +
				"    \"data\": {\n" +
				"       \"entityTypeId\": {\n" +
				"        \"name\": " + entityTypeId + " ,\n" +
				"        \"values\": " + entityTypeId + "\n" +
				"      },\n" +
				"      \"id\": {\n" +
				"        \"name\": \"id\",\n" +
				"      }\n" +
				"    }\n" +
				"  }\n" +
				"}";
		JSONObject payload = new JSONObject(payloadString);
		if (entityIdtoBeDeleted != null)
			payload.getJSONObject("body").getJSONObject("data").getJSONObject("id").put("values", Integer.parseInt(entityIdtoBeDeleted));
		else
			payload.getJSONObject("body").getJSONObject("data").getJSONObject("id").put("values", Integer.parseInt(entityId));

		String responseShowPageActionApi = show.hitEntityClickableActionsAPIs(uriForClickableActions, requestType, payload.toString());
		logger.info("Delete Action API Response is:{}", responseShowPageActionApi);

		JSONObject jsonObj = new JSONObject(responseShowPageActionApi);
		String status = jsonObj.getJSONObject("header").getJSONObject("response").get("status").toString();
		logger.info("status:{}", status);

		// if we have permission to perform actions , API response should have status=success
		if (permissionToExecute && status.contentEquals("success")) {
			return true;
		}

		// if we do have permission to perform actions , API response should have status=applicationError
		if (!permissionToExecute && status.contentEquals("applicationError")) {
			return true;
		}

		return false;
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
	}


}
