package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


public class TestPreSignature extends TestRailBase {
	private final static Logger logger = LoggerFactory.getLogger(TestPreSignature.class);

	private String configFilePath;
	private String configFileName;
	private String cdrConfigFilePath;
	private String cdrConfigFileName;
	private String cdrExtraFieldsConfigFileName;
	private String cdrSectionName;
	private String clauseConfigFilePath;
	private String clauseConfigFileName;
	private String clauseExtraFieldsConfigFileName;
	private String clauseSectionName;
	private String contractTemplateSectionName;
	private Boolean deleteEntity;
	private Boolean createLocalCdr;
	private String definitionSectionName;
	private String ctsSectionName;

	@BeforeClass
	public void setConfigProperties() {
		try {
			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PreSignatureConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("PreSignatureConfigFileName");

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cdrsectionname");
			if (temp != null)
				cdrSectionName = temp.trim();

			String temp2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clausesectionname");
			if (temp2 != null)
				clauseSectionName = temp2.trim();

			String temp3 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contracttemplatesectionname");
			if (temp3 != null)
				contractTemplateSectionName = temp3.trim();

			String temp4 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "definitionsectionname");
			if (temp4 != null)
				definitionSectionName = temp4.trim();

			String temp5 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "ctssectionname");
			if (temp5 != null)
				ctsSectionName = temp5.trim();

			//CDR Config files
			cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
			cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");
			cdrExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRExtraFieldsFileName");

			//Clause Config files
			clauseConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ClauseFilePath");
			clauseConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ClauseFileName");
			clauseExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ClauseExtraFieldsFileName");

			deleteEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteentity"));
			createLocalCdr = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createlocalcdr"));

		} catch (Exception e) {
			logger.error("Exception occurred while setting config properties. {}", e.getMessage());
		}
		testCasesMap = getTestCasesMapping();
	}

	@Test(enabled = true)
	public void testCDRCreation() {
		CustomAssert customAssert = new CustomAssert();
//		CustomAssert csAssert = new CustomAssert();
		try {
			String createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrConfigFilePath,
					cdrExtraFieldsConfigFileName, cdrSectionName, createLocalCdr);

			if (APIUtils.validJsonResponse(createResponse, "create api for cdr")) {

				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					Integer cdrDbId = CreateEntity.getNewEntityId(createResponse, "contract draft request");
					logger.info("new cdr id : {}", cdrDbId);

					Boolean isListingPassed = verifyOnListing("contract draft request", cdrDbId);
					if (isListingPassed) {
						logger.info("Listing validation for CDR entity creation is passed.");
					} else {
						logger.error("Listing validation for CDR entity creation is failed.");
						customAssert.assertTrue(false, "Listing validation for CDR entity creation is failed.");
					}

					/*deleting newly created entity*/
					if (deleteEntity) {
						logger.info("Deleting newly created entity. DbId {}", cdrDbId);
						deleteNewEntity("contract draft request", cdrDbId);
					}
				} else {
					logger.error("CDR entity creation failed. response : {}", createResponse);
					customAssert.assertTrue(false, "CDR entity creation failed.");
				}
			} else {
				logger.error("create api response is not valid json for entity : CDR");
				customAssert.assertTrue(false, "create api response is not valid json for entity : CDR");
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testCDRCreation test method. error = {}", e.getMessage());
			e.printStackTrace();
			customAssert.assertTrue(false, "Exception occurred in testCDRCreation test method. error = {}" + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testCDRCreation"), customAssert);
		customAssert.assertAll();
	}

	@Test(enabled = true)
	@Parameters({"TestingType", "Environment"})
	public void testClauseEntity(String testingType,String environment) {

		CustomAssert customAssert = new CustomAssert();

		if(testingType.contains("smoke") && environment.contains("Sandbox/VF")){
			addTestResultAsSkip(getTestCaseIdForMethodName("testClauseEntity"), customAssert);
			throw new SkipException("Skipping this test for the sandbox");
		}
		//CustomAssert csAssert = new CustomAssert();
		Boolean createLocalClause = false;
		try {
			String createResponse = Clause.createClause(clauseConfigFilePath, clauseConfigFileName, clauseConfigFilePath,
					clauseExtraFieldsConfigFileName, clauseSectionName, createLocalClause);

			if (APIUtils.validJsonResponse(createResponse, "create api for clause entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for Clauses. response : {}", createResponse);
					Integer clauseDbId = CreateEntity.getNewEntityId(createResponse, "clauses");
					logger.info("new clause id : {}", clauseDbId);

					/*deleting newly created entity*/
					if (deleteEntity) {
						logger.info("Deleting newly created entity. DbId {}", clauseDbId);
						deleteNewEntity("clauses", clauseDbId);
					}
				} else {
					logger.error("Entity creation failed for Clauses. Response : {}", createResponse);
					customAssert.assertTrue(false, "Entity creation failed for Clauses. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testClauseEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			customAssert.assertTrue(false, "Exception occurred while creting clause entity. error = {}" + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testClauseEntity"), customAssert);
		customAssert.assertAll();
	}

	@Test(enabled = true)
	@Parameters({"TestingType", "Environment"})
	public void testContractTemplateEntity(String testingType,String environment) {

		CustomAssert customAssert = new CustomAssert();

		if(testingType.contains("smoke") && environment.contains("Sandbox/VF")){
			addTestResultAsSkip(getTestCaseIdForMethodName("testContractTemplateEntity"), customAssert);
			throw new SkipException("Skipping this test for the sandbox");
		}
		//CustomAssert csAssert = new CustomAssert();
		Boolean createLocalContractTemplate = false;
		try {
			String createResponse = ContractTemplate.createContractTemplate(contractTemplateSectionName, createLocalContractTemplate);

			if (APIUtils.validJsonResponse(createResponse, "create api for Contract Template entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for Contract Template. response : {}", createResponse);
					Integer contractTemplateDbId = CreateEntity.getNewEntityId(createResponse, "contract templates");
					logger.info("new contract template id : {}", contractTemplateDbId);

					/*deleting newly created entity*/
					if (deleteEntity) {
						logger.info("Deleting newly created entity. DbId {}", contractTemplateDbId);
						deleteNewEntity("contract templates", contractTemplateDbId);
					}
				} else {
					logger.error("Entity creation failed for Contract Template. Response : {}", createResponse);
					customAssert.assertTrue(false, "Entity creation failed for Contract Template. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testContractTemplateEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			customAssert.assertTrue(false, "Exception occurred while creating ContractTemplate entity. error = {}" + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testContractTemplateEntity"), customAssert);
		customAssert.assertAll();
	}

	@Test(enabled = true)
	@Parameters({"TestingType", "Environment"})
	public void testDefinitionEntity(String testingType,String environment) {

		CustomAssert customAssert = new CustomAssert();

		if(testingType.contains("smoke") && environment.contains("Sandbox/VF")){
			addTestResult(getTestCaseIdForMethodName("testDefinitionEntity"), customAssert);
			throw new SkipException("Skipping this test for the sandbox");
		}
//		CustomAssert csAssert = new CustomAssert();
		Boolean createLocalDefinition = false;
		try {
			String createResponse = Definition.createDefinition(definitionSectionName, createLocalDefinition);

			if (APIUtils.validJsonResponse(createResponse, "create api for definition entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for Definition. response : {}", createResponse);
					Integer definitionDbId = CreateEntity.getNewEntityId(createResponse, "definition");
					logger.info("new definition id : {}", definitionDbId);

					/*deleting newly created entity*/
					if (deleteEntity) {
						logger.info("Deleting newly created entity. DbId {}", definitionDbId);
						deleteNewEntity("definition", definitionDbId);
					}
				} else {
					logger.error("Entity creation failed for Definition. Response : {}", createResponse);
					customAssert.assertTrue(false, "Entity creation failed for Definition. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testDefinitionEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			customAssert.assertTrue(false, "Exception occurred while creating definition entity. error = {}" + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testDefinitionEntity"), customAssert);
		customAssert.assertAll();
	}

	@Test(enabled = true)
	@Parameters({"TestingType", "Environment"})
	public void testContractTemplateStructureEntity(String testingType,String environment) {

		CustomAssert customAssert = new CustomAssert();

		if(testingType.contains("smoke") && (environment.contains("Sandbox/VF") || environment.contains("Sandbox/EU"))){
			addTestResult(getTestCaseIdForMethodName("testContractTemplateStructureEntity"), customAssert);
			throw new SkipException("Skipping this test for the sandbox");
		}
		//CustomAssert csAssert = new CustomAssert();
		Boolean createLocalCTS = false;
		try {
			String createResponse = ContractTemplateStructure.createContractTemplateStructure(ctsSectionName, createLocalCTS);

			if (APIUtils.validJsonResponse(createResponse, "create api for contract template structure entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for contract template structure. response : {}", createResponse);
					Integer newDbId = CreateEntity.getNewEntityId(createResponse, "contract template structure");
					logger.info("new contract template structure id : {}", newDbId);

					/*deleting newly created entity*/
					if (deleteEntity) {
						logger.info("Deleting newly created entity. DbId {}", newDbId);
						deleteNewEntity("contract template structure", newDbId);
					}
				} else {
					logger.error("Entity creation failed for contract template structure. Response : {}", createResponse);
					customAssert.assertTrue(false, "Entity creation failed for contract template structure. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testContractTemplateStructureEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			customAssert.assertTrue(false, "Exception occurred while creating contract template structure entity. error = {}" + e.getMessage());
		}
		addTestResult(getTestCaseIdForMethodName("testContractTemplateStructureEntity"), customAssert);
		customAssert.assertAll();
	}

	private Boolean verifyOnListing(String entityName, Integer cdrDbId) {
		Boolean isSuccess = false;

		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "urlId"));
			Integer size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));
			Integer offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));
			String orderByColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderByColumnName");
			String orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderDirection");

			String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
					"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}}";

			ListRendererListData listDataObj = new ListRendererListData();
			listDataObj.hitListRendererListData(urlId, listDataPayload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (APIUtils.validJsonResponse(listDataResponse, "list data api")) {
				JSONObject response = new JSONObject(listDataResponse);
				Integer totalCount = response.getInt("totalCount");
				if (totalCount > 0) {
					JSONArray dataArray = response.getJSONArray("data");
					for (int i = 0; i < dataArray.length(); i++) {
						JSONArray innerDataArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));
						for (int j = 0; j < innerDataArray.length(); j++) {
							if (innerDataArray.getJSONObject(j).getString("columnName").equalsIgnoreCase("id")) {
								if (innerDataArray.getJSONObject(j).getString("value").contains(cdrDbId.toString())) {
									isSuccess = true;
								}
								break;
							}
						}
						if (isSuccess == true)
							break;

					}
				}
			} else {
				isSuccess = false;
				logger.error("list data api response is not valid json for entity : {}, payload : {}", entityName, listDataPayload);
			}

		} catch (Exception e) {
			isSuccess = false;
			logger.error("Exception occurred while validating listing page for entity = {}", entityName);
			e.printStackTrace();
		}


		return isSuccess;
	}

	private void deleteNewEntity(String entityName, int entityId) {
		try {
			String urlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath"),
					ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "url_name");
			logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
			Show showObj = new Show();
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			showObj.hitShow(entityTypeId, entityId);
			if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
				JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
				String prefix = "{\"body\":{\"data\":";
				String suffix = "}}";
				String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
				String deletePayload = prefix + showBodyStr + suffix;

				logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
				Delete deleteObj = new Delete();
				deleteObj.hitDelete(entityName, deletePayload, urlName);
				String deleteJsonStr = deleteObj.getDeleteJsonStr();
				jsonObj = new JSONObject(deleteJsonStr);
				String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
				if (status.trim().equalsIgnoreCase("success"))
					logger.info("Entity having Id {} is deleted Successfully.", entityId);
			}
		} catch (Exception e) {
			logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
		}
	}
}
