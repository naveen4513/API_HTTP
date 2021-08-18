package com.sirionlabs.helper.PerformanceData;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class PerformActionsOnEntities {

	private final static Logger logger = LoggerFactory.getLogger(PerformActionsOnEntities.class);
	String entityIdMappingFileName;
	String baseFilePath;
	String performActionsOnEntitiesCfgFilePath;
	String performActionsOnEntitiesCfgFileName;
	ListRendererListData listRendererListData;

	List<String> allEntitySection;
	String entitySectionSplitter = ";";
	Boolean testForAllEntities = false;
	Show show;
	Integer entityIdToBeTested;

	int pageSize;
	int maxEntitiesCount;
	String parser;

	@BeforeClass
	public void beforeClass() throws IOException {
		logger.info("In Before Class method");
		getDataEntityClickableActions();
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

		performActionsOnEntitiesCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFilePath");
		performActionsOnEntitiesCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFileName");

	}

	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "TestEntityClickableActionsData")
	public Object[][] getTestEntityClickableActionsData(ITestContext c) throws ConfigurationException {

		logger.info("In the Data Provider");
		int i = 0;


		entitySectionSplitter = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "entitysectionsplitter");
		pageSize = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "pagesize"));
		maxEntitiesCount = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "maxentitiescount"));
		parser = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, "parser");


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


		Object[][] groupArray = new Object[allEntitySection.size()][];
		for (String entitySection : allEntitySection) {
			logger.debug("entitySection :{}", entitySection);
			HashMap<String, String> hashMapforEntityConfigProperties = new HashMap<String, String>();
			groupArray[i] = new Object[4];

			List<String> allProperties = ParseConfigFile.getAllPropertiesOfSection(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entitySection);
			if (allProperties.isEmpty()) {
				Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
				Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

				groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
				groupArray[i][1] = entitySectionTypeId; // Entity type Id
				groupArray[i][2] = entitySection; // EntityName
				groupArray[i][3] = entitySectionUrlId; // Entity Url Id
				i++;
				continue;
			} else {

				Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
				Integer entitySectionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

				for (String entitySpecificProperty : allProperties) {
					hashMapforEntityConfigProperties.put(entitySpecificProperty, ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entitySection, entitySpecificProperty));
				}

				groupArray[i][0] = hashMapforEntityConfigProperties; // HashMap with Config Values for Entity Clickable Actions
				groupArray[i][1] = entitySectionTypeId; // Entity type Id
				groupArray[i][2] = entitySection; // EntityName
				groupArray[i][3] = entitySectionUrlId; // Entity Url Id
				logger.info("hashMapforEntityConfigProperties: {} , entitySectionTypeId : {} , entitySection : {} , entitySectionUrlId : {} ", hashMapforEntityConfigProperties, entitySectionTypeId, entitySection, entitySectionUrlId);
				i++;
			}
		}

		return groupArray;
	}

	//  this function will return all the Entities Ids from their List Page
	public HashSet<Integer> getHashSetOfEntityIds(String entitySection, int offset, int size, int listId) throws Exception {
		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
		List<Integer> allDBIds;
		HashSet<Integer> hashSetofEntityId = new HashSet<>();
		String orderByColumnName = "id";
		String orderDirection = "desc";
		HttpResponse httplistDataResponse = listRendererListData.hitListRendererListData(entityTypeId, offset, size, orderByColumnName, orderDirection, listId);
		String listDataResponse = listRendererListData.getListDataJsonStr();
		logger.debug("List Data API Response : entity={} , response={}", entitySection, listDataResponse);

		boolean isListDataValidJson = APIUtils.validJsonResponse(listRendererListData.getListDataJsonStr());
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


	// Test Entity Clickable Actions APIs
	@Test(dataProvider = "TestEntityClickableActionsData")
	public void testEnityClickActionsAPIs(HashMap<String, String> hashMapforEntityConfigProperties, Integer entityTypeId, String entityName, Integer entitySectionUrlId) throws Exception {
		CustomAssert csAssertion = new CustomAssert();
		entityIdToBeTested = null;
		String entitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "url_name");

		if (hashMapforEntityConfigProperties.size() != 0) {
			logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);

			String[] validActions = hashMapforEntityConfigProperties.get("validactions").split(parser);
			int validActionsSize = validActions.length;
			logger.info("Valid Actions for entity [{}] are as follows ", entityName);
			for (String validAction : validActions)
				logger.info("{}", validAction);

			int offset = 0;
			int size = pageSize;
			int actionindex; // index for validActions array
			while (offset < maxEntitiesCount) {

				HashSet<Integer> dbIds = getHashSetOfEntityIds(entityName, offset, size, entitySectionUrlId);
				logger.info("Db Ids are : {}", dbIds);
				if (dbIds.isEmpty()) {
					logger.info("there is no record for given offset:{} and size : {}", offset, size);
					break;
				}

				actionindex = 0;
				int dbIdIndex = 0;
				logger.info("-----------------------------------------------------------------------------------------------------------------------------------");
				for (Integer dbId : dbIds) {

					if (dbIdIndex % (validActionsSize + 1) != 0) {  // this logic is for escaping one dbId after one set of valid option get performed
						logger.info("Performing [{}] Action On EntityId [{}] of EntityType [{}] ", validActions[actionindex % (validActions.length)], dbId, entityName);

						performAction(validActions[actionindex % validActionsSize], dbId, entityName, entityTypeId, entitySectionUrlId, entitySectionUrlName);

						logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

						actionindex++;
					}

					dbIdIndex++;

				}
				offset += size;

			}


			logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
		} else {
			logger.info("###################################################:Tests Starting for Entity:{}##################################################################", entityName);
			csAssertion.assertTrue(false, "User Needs to Specify Entity Clickables Actions  , Skipping this entity :" + entityName);


			logger.info("###################################################:Tests Ending for Entity:{}##################################################################", entityName);
		}
		csAssertion.assertAll();
	}

	// this method will perfrom the action on the given entityDbId
	void performAction(String actionName, Integer entityDbId, String entityName, Integer entityTypeId, Integer entitySectionUrlId, String entitySectionUrlName) {

		logger.info("****************************************************************************************************************************");


		try {
			//if (!entityName.equalsIgnoreCase("contract")) {

			if (actionName.toLowerCase().contentEquals("onhold")) {
				EntityWorkFlowActionsHelper.onHoldEntity(entityName, entityDbId, entitySectionUrlName);
			} else if (actionName.toLowerCase().contentEquals("archive")) {
				EntityWorkFlowActionsHelper.archiveEntity(entityName, entityDbId, entitySectionUrlName);
			} else {
				String workFlowOrderSequence = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entityName, actionName);
				Boolean result = true;
				if (workFlowOrderSequence != null) {

					if (!workFlowOrderSequence.equalsIgnoreCase("independent")) {

						String actions[] = workFlowOrderSequence.trim().split("->");

						logger.info("Setting pre-requisite for workFlow action : {} and entity : {}, workflow order sequence : {}", actionName, entityName, Arrays.asList(actions));
						for (String action : actions) {

							result = EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, action);
							if (!result)
								break;
						}
					}


					if (!result) // if any of the pre-requisite action got failed
						logger.info("Failed in Performing [{}] action on entityid  [{}] of entity [{}] because pre-requisite action got failed", actionName, entityDbId, entityName);

					else {
						logger.info("Hitting workflow action :{} on entity : {}", actionName, entityName);
						EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, actionName);
					}
				} else {
					logger.warn("workFlowOrderSequence not found for action : {} and entity : {} ", actionName, entityName);
				}
			}
//			} else {
//				if (actionName.toLowerCase().contentEquals("onhold")) {
//					EntityWorkFlowActionsHelper.onHoldEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("archive")) {
//					EntityWorkFlowActionsHelper.archiveEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("activate")) {
//					EntityWorkFlowActionsHelper.activateEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("sendforpeerreview")) {
//					EntityWorkFlowActionsHelper.sendForPeerReviewEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("peerreviewcomplete")) {
//					EntityWorkFlowActionsHelper.peerReviewCompleteEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("sendforinternalreview")) {
//					EntityWorkFlowActionsHelper.sendForClientReviewEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("internalreviewcomplete")) {
//					EntityWorkFlowActionsHelper.internalReviewCompleteEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("sendforclientreview")) {
//					EntityWorkFlowActionsHelper.sendForClientReviewEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("reject")) {
//					EntityWorkFlowActionsHelper.rejectEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("approve")) {
//					EntityWorkFlowActionsHelper.approveEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("publish")) {
//					EntityWorkFlowActionsHelper.publishEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//				if (actionName.toLowerCase().contentEquals("inactivate")) {
//					EntityWorkFlowActionsHelper.inActivateEntity(entityName, entityDbId, entitySectionUrlName);
//				}
//			}

		} catch (Exception e) {
			logger.error("Exception occurred in performAction method. error = {}", e.getMessage());
			e.printStackTrace();
		}
		logger.info("****************************************************************************************************************************");


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


