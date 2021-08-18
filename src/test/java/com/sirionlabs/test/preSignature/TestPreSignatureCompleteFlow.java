package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.LinkEntity;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.documentFlow.DocumentFlowSave;
import com.sirionlabs.api.documentFlow.MoveToTreeSave;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.presignature.ClausePageData;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sirionlabs.helper.EntityWorkFlowActionsHelper.performAction;

public class TestPreSignatureCompleteFlow {
	private final static Logger logger = LoggerFactory.getLogger(TestPreSignatureCompleteFlow.class);

	private String configFilePath;
	private String configFileName;
	private String preSignatureSectionName;
	private Boolean deleteEntity;
	private Boolean createLocalCdr;
	private Map<Integer, String> newlyCreatedEntityIdAndEntityNameMap = new HashMap<>();
	private Map<Integer, String> newlyCreatedClauseIdAndNameMap = new HashMap<>();
	private Map<Integer, String> newlyCreatedCTIdAndNameMap = new HashMap<>();
	private Map<Integer, String> newlyCreatedCDRIdAndNameMap = new HashMap<>();
	private Map<Integer, String> newlyCreatedContractIdAndContractNameMap = new HashMap<>();
	private List<String> documentIds;
	private String documentStatusIdForFinal;
	private Long schedulerWaitingTime;
	private List<String> documentNames;
	private Boolean killAllTasks;
	private Long schedulerPollingTime;

	@BeforeClass
	public void setConfigProperties() {
		try {
			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("PreSignatureFlowConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("PreSignatureFlowConfigFileName");

			preSignatureSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sectionname");
			deleteEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteentity"));
			createLocalCdr = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createlocalcdr"));
			documentStatusIdForFinal = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "documentstatusidforstatusfinal");
			schedulerWaitingTime = Long.parseLong(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitingTime"));
			schedulerPollingTime = Long.parseLong(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime"));
			killAllTasks = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killAllTasks"));

		} catch (Exception e) {
			logger.error("Exception occurred while setting config properties. {}", e.getMessage());
		}
	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class........");
		/*deleting newly created entities*/
		if (deleteEntity) {
			for (Map.Entry<Integer, String> entry : newlyCreatedEntityIdAndEntityNameMap.entrySet()) {
				logger.info("Deleting newly created entity : {}, DbId {}", entry.getValue(), entry.getKey());
				deleteNewEntity(entry.getValue(), entry.getKey());
			}
		}
	}

	@Test(enabled = true)
	public void testClauseEntity() {
		CustomAssert csAssert = new CustomAssert();
		Boolean createLocalClause = false;
		String entityName = "clauses";
		String action = "publish";
		try {
			String createResponse = Clause.createClause(preSignatureSectionName, createLocalClause);

			if (APIUtils.validJsonResponse(createResponse, "create api for clause entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for Clauses. response : {}", createResponse);
					Integer clauseDbId = CreateEntity.getNewEntityId(createResponse, "clauses");
					String clauseName = getNewEntityName(entityName, clauseDbId);
					newlyCreatedClauseIdAndNameMap.put(clauseDbId, clauseName);
					newlyCreatedEntityIdAndEntityNameMap.put(clauseDbId, entityName);
					logger.info("new clause id : {}", clauseDbId);

					logger.info("Performing work flow action : {} on entity : {}, DbId : {}", action, entityName, clauseDbId);
					Boolean actionPerformed = performWorkflowAction(entityName, clauseDbId.toString(), action);
					if (!actionPerformed) {
						logger.error("Unable to perform workflow action for entity : {}, dbId : {}, Action : {}", entityName, clauseDbId, action);
						csAssert.assertTrue(false, "Unable to perform workflow action for entity : " + entityName + ", dbId : " + clauseDbId + ", Action : " + action);
						csAssert.assertAll();
						return;
					}

				} else {
					logger.error("Entity creation failed for Clauses. Response : {}", createResponse);
					csAssert.assertTrue(false, "Entity creation failed for Clauses. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testClauseEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception occurred while creating clause entity. error = {}" + e.getMessage());
		}
		csAssert.assertAll();
	}

	/*Tag validation on wizard*/
	@Test(dependsOnMethods = "testClauseEntity", enabled = true)
	public void validateTagOnWizard() {
		CustomAssert csAssert = new CustomAssert();
		Integer clauseDbId = null;
		try {
			for (Map.Entry<Integer, String> entry : newlyCreatedEntityIdAndEntityNameMap.entrySet()) {
				if (entry.getValue().equals("clauses")) {
					clauseDbId = entry.getKey();
				}
			}
			Boolean isTagValidationSuccessful = validateTagOnShowPage(clauseDbId);
			if (isTagValidationSuccessful) {
				logger.info("Tag validation passed on show page for clause dbId : {}", clauseDbId);
			} else {
				logger.error("Tag validation failed on show page for clause dbId : {}", clauseDbId);
				csAssert.assertTrue(false, "Tag validation failed on show page for clause dbId : " + clauseDbId);
			}
		} catch (Exception e) {
			logger.error("Exception while validating clause tag on Wizard. error : {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception while validating clause tag on Wizard. error : " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "testClauseEntity")
	public void testContractTemplateEntity() {

		CustomAssert csAssert = new CustomAssert();
		Boolean createLocalContractTemplate = false;
		String entityName = "contract templates";
		String action = "publish";
		try {
			/*Updating clause name and id in extra fields*/
			updateContractTemplateExtraField();
			String createResponse = ContractTemplate.createContractTemplate(preSignatureSectionName, createLocalContractTemplate);

			if (APIUtils.validJsonResponse(createResponse, "create api for Contract Template entity")) {
				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					logger.info("Entity creation passed for Contract Template. response : {}", createResponse);
					Integer contractTemplateDbId = CreateEntity.getNewEntityId(createResponse, "contract templates");
					logger.info("new contract template id : {}", contractTemplateDbId);
					String contractTemplateName = getNewEntityName(entityName, contractTemplateDbId);
					newlyCreatedEntityIdAndEntityNameMap.put(contractTemplateDbId, entityName);
					newlyCreatedCTIdAndNameMap.put(contractTemplateDbId, contractTemplateName);

					logger.info("Performing work flow action : {} on entity : {}, DbId : {}", action, entityName, contractTemplateDbId);
					Boolean actionPerformed = performWorkflowAction(entityName, contractTemplateDbId.toString(), action);
					if (!actionPerformed) {
						logger.error("Unable to perform workflow action for entity : {}, dbId : {}, Action : {}", entityName, contractTemplateDbId, action);
						csAssert.assertTrue(false, "Unable to perform workflow action for entity : " + entityName + ", dbId : " + contractTemplateDbId + ", Action : " + action);
						csAssert.assertAll();
						return;
					}
				} else {
					logger.error("Entity creation failed for Contract Template. Response : {}", createResponse);
					csAssert.assertTrue(false, "Entity creation failed for Contract Template. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testContractTemplateEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception occurred while creating ContractTemplate entity. error = {}" + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "testContractTemplateEntity")
	public void testCDREntity() {
		CustomAssert csAssert = new CustomAssert();
		String entityName = "contract draft request";
		String action = "sendforclientreview";
		try {
			String createResponse = ContractDraftRequest.createCDR(preSignatureSectionName, createLocalCdr);

			if (APIUtils.validJsonResponse(createResponse, "create api for cdr")) {

				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					Integer cdrDbId = CreateEntity.getNewEntityId(createResponse, "contract draft request");
					String cdrName = getNewEntityName(entityName, cdrDbId);
					logger.info("new cdr id : {}", cdrDbId);
					newlyCreatedEntityIdAndEntityNameMap.put(cdrDbId, entityName);
					newlyCreatedCDRIdAndNameMap.put(cdrDbId, cdrName);

					logger.info("Performing work flow action : {} on entity : {}, DbId : {}", action, entityName, cdrDbId);
					Boolean actionPerformed = performWorkflowAction(entityName, cdrDbId.toString(), action); //CDR approve workflow is not present currently

					if (!actionPerformed) {
						logger.error("Unable to perform workflow action for entity : {}, dbId : {}, Action : {}", entityName, cdrDbId, action);
						csAssert.assertTrue(false, "Unable to perform workflow action for entity : " + entityName + ", dbId : " + cdrDbId + ", Action : " + action);
						csAssert.assertAll();
						return;
					}
					Boolean isTemplateLinkedSuccessfully = linkTemplateToCDR(entityName, cdrDbId);

					if (isTemplateLinkedSuccessfully) {
						logger.info("Contract template successfully linked with CDR. cdr dbId : {}", cdrDbId);

					} else {
						logger.error("Contract template failed to linked with CDR. cdr dbId : {}", cdrDbId);
						csAssert.assertTrue(false, "Contract template failed to linked with CDR. cdr dbId : " + cdrDbId);
					}
				} else {
					logger.error("CDR entity creation failed. response : {}", createResponse);
					csAssert.assertTrue(false, "CDR entity creation failed.");
				}
			} else {
				logger.error("create api response is not valid json for entity : CDR");
				csAssert.assertTrue(false, "create api response is not valid json for entity : CDR");
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testCDREntity test method. error = {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception occurred in testCDREntity test method. error = {}" + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "testCDREntity")
	public void testUploadFeatureOnCDR() {
		CustomAssert csAssert = new CustomAssert();

		try {

			String filePath = "src/test/resources/TestConfig/PreSignature/TestData";
			String fileName = "Actions_Aging.png";
			Integer cdrDbId = null;

			for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
				cdrDbId = entry.getKey();
			}

			Long uniqueKey = NumberUtils.getUniqueCurrentTimeMS();

			Map paramMap = new HashMap<>();
			paramMap.put("name", "Actions_Aging");
			paramMap.put("extension", "png");
			paramMap.put("key", uniqueKey.toString());
			paramMap.put("entityTypeId", "160");
			paramMap.put("entityId", cdrDbId.toString());

			FileUploadDraft fileUploadDraft = new FileUploadDraft();
			String fileUploadResponse = fileUploadDraft.hitFileUpload(filePath, fileName, paramMap);

			if (APIUtils.validJsonResponse(fileUploadResponse)) {

				String payload = getPayloadForSubmitDraft(cdrDbId, uniqueKey.toString());
				SubmitDraft submitDraft = new SubmitDraft();
				submitDraft.hitSubmitDraft(payload);
				String submitDraftResponse = submitDraft.getSubmitDraftJsonStr();

				if (APIUtils.validJsonResponse(submitDraftResponse)) {
					String tabListDataPayload = getTabListDataPayload();
					ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
					listRendererTabListData.hitListRendererTabListData(367, 160, cdrDbId, tabListDataPayload);
					String response = listRendererTabListData.getTabListDataJsonStr();

					if (APIUtils.validJsonResponse(response)) {
						JSONObject jsonObject = new JSONObject(response);
						Integer totalCount = jsonObject.getInt("totalCount");

						if (totalCount < 1) {
							csAssert.assertTrue(false, "Document is not successfully uploaded to cdr : " + cdrDbId);
							logger.error("Document is not successfully uploaded to cdr : {}", cdrDbId);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating document upload on CDR entity. error : {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while validating document upload on CDR entity. error : " + e.getMessage());
		}

		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "testCDREntity")
	public void validationOnCDREntityForContractLinking() {
		CustomAssert csAssert = new CustomAssert();
		String entityName = "contracts";
		Boolean createLocalContract = true;

		try {
			String createResponse = Contract.createContract(preSignatureSectionName, createLocalContract);

			if (APIUtils.validJsonResponse(createResponse, "create api for contract")) {

				JSONObject response = new JSONObject(createResponse);
				String status = response.getJSONObject("header").getJSONObject("response").getString("status");

				if (status.equalsIgnoreCase("success")) {
					Integer contractDbId = CreateEntity.getNewEntityId(createResponse, "contracts");
					String contractName = getNewEntityName(entityName, contractDbId);
					logger.info("new cdr id : {}", contractDbId);
					newlyCreatedEntityIdAndEntityNameMap.put(contractDbId, entityName);
					newlyCreatedContractIdAndContractNameMap.put(contractDbId, contractName);


					Boolean isCDRLinkedSuccessfully = linkContractWithCDR(entityName, contractDbId);

					if (isCDRLinkedSuccessfully) {
						logger.info("Contract successfully linked with CDR. contract dbId : {}", contractDbId);

					} else {
						logger.error("Contract failed to linked with CDR. contract dbId : {}", contractDbId);
						csAssert.assertTrue(false, "Contract failed to linked with CDR. contract dbId : " + contractDbId);
					}
				} else {
					logger.error("Contract entity creation failed. response : {}", createResponse);
					csAssert.assertTrue(false, "Contract entity creation failed.");
				}
			} else {
				logger.error("create api response is not valid json for entity : {}", entityName);
				csAssert.assertTrue(false, "create api response is not valid json for entity : " + entityName);
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testPreSignatureLinkingWithContractEntity test method. error = {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception occurred in testPreSignatureLinkingWithContractEntity test method. error = {}" + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "validationOnCDREntityForContractLinking")
	public void validationOnContractEntityForCDRLinking() {
		CustomAssert csAssert = new CustomAssert();
		String entityName = "contracts";
		Integer contractId = null;
		String contractName = null;

		try {
			for (Map.Entry<Integer, String> entry : newlyCreatedContractIdAndContractNameMap.entrySet()) {
				contractId = entry.getKey();
				contractName = entry.getValue();
			}
			String tabListDataResponse = getTabListDataResponse(entityName, contractId, "linkedentitiestablistid");
			if (APIUtils.validJsonResponse(tabListDataResponse, "Linked Entities TabListData response")) {
				String expLinkedEntityName = null;
				String expLinkedEntityId = null;

				for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
					expLinkedEntityName = entry.getValue();
					expLinkedEntityId = entry.getKey().toString();
				}
				Boolean isCDRLinkedSuccessfully = validateTabListDataResponseForLinkedCDREntity(tabListDataResponse, expLinkedEntityName, expLinkedEntityId);
				if (isCDRLinkedSuccessfully) {
					logger.info("validation On Contract Entity For CDR Linking is passed successfully. Liked CDR is validated on Contract- Linked Entities Tab. contractId : {}", contractId);

				} else {
					logger.error("Validation failed for CDR on Contract- Linked Entities Tab. contractId : {}", contractId);
					csAssert.assertTrue(false, "Validation failed for CDR on Contract- Linked Entities Tab. contractId " + contractId);
				}

			} else {
				logger.error("Linked Entities TabListData response is not valid json. response : {}", tabListDataResponse);
				csAssert.assertTrue(false, "Linked Entities TabListData response is not valid json.");
			}
		} catch (Exception e) {
			logger.error("Exception occur while validating Contract entity for PreSignature linking. error : {}", e.getMessage());
			e.printStackTrace();
		}
		csAssert.assertAll();
	}

	@Test(enabled = true, dependsOnMethods = "validationOnContractEntityForCDRLinking")
	public void testDocumentMovement() {
		CustomAssert csAssert = new CustomAssert();
		String entityName = "contracts";
		Integer contractId = null;
		String contractName = null;

		try {
			for (Map.Entry<Integer, String> entry : newlyCreatedContractIdAndContractNameMap.entrySet()) {
				contractId = entry.getKey();
				contractName = entry.getValue();
			}

			String linkedCDREntityName = null;
			String linkedCDREntityId = null;
			for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
				linkedCDREntityName = entry.getValue();
				linkedCDREntityId = entry.getKey().toString();
			}

			logger.info("Document Movement Pre-Requisite : Marking document as final on CDR entity. cdr id : {}, name : {}", linkedCDREntityId, linkedCDREntityName);
			Boolean isDocumentMarkedSuccessfully = markDocumentAsFinalOnCDR(Integer.parseInt(linkedCDREntityId));
			if (isDocumentMarkedSuccessfully) {
				List<Integer> allTaskIdsBeforeSubmittingRequestToInheritDoc = getAllTaskIds();

				if (killAllTasks) {
					logger.info("KillAllTasks flag is Turned On. Killing All Tasks.");
					UserTasksHelper.removeAllTasks();
				}
				Boolean isInheritFileRequestSubmitted = sendRequestToInheritFile(contractId, entityName, documentIds);
				if (isInheritFileRequestSubmitted) {

					Integer newTaskId = getNewTaskId(allTaskIdsBeforeSubmittingRequestToInheritDoc);

					logger.info("Wait For Scheduler to finish Document Inheriting Task");
					Map<String, String> docInheritingJob = UserTasksHelper.waitForScheduler(schedulerWaitingTime, schedulerPollingTime, newTaskId);

					if (docInheritingJob.get("jobPassed").trim().equalsIgnoreCase("true")) {
						logger.info("docInheritingJob Passed for Contract Id : {}.", contractId);
					} else {
						logger.error("document inheriting failed for contract id : {}, errorMessage : {}", contractId, docInheritingJob.get("errorMessage"));
						csAssert.assertTrue(false, "document inheriting failed for contract id : " + contractId + ", errorMessage : " + docInheritingJob.get("errorMessage"));
						csAssert.assertAll();
						return;
					}

					Boolean isDocumentInheritedSuccessfully = getDocumentsInheritedStatus(entityName, contractId);
					if (isDocumentInheritedSuccessfully) {
						logger.info("Documents inherited successfully for contract : {}, id : {}", contractName, contractId);
						Boolean isValidationPassed = validateInheritedDocumentsOnShowPage(entityName, contractId);

						if (isValidationPassed) {
							logger.info("Document metadata is successfully validated on Contract show page [Contract Document Tab].");

							List<Integer> allTaskIdsBeforeSubmittingRequestToMoveToTree = getAllTaskIds();

							if (killAllTasks) {
								logger.info("KillAllTasks flag is Turned On. Killing All Tasks.");
								UserTasksHelper.removeAllTasks();
							}

							Boolean isRequestSubmitted = submitRequestForMovingDocumentToContractTree(contractId, entityName, documentIds);

							if (isRequestSubmitted) {

								Integer newTaskIdForMovingDocToContractTree = getNewTaskId(allTaskIdsBeforeSubmittingRequestToMoveToTree);

								logger.info("Wait For Scheduler to finish Document Movement to Contract Tree Task");
								Map<String, String> docMovementToContractTreeJob = UserTasksHelper.waitForScheduler(schedulerWaitingTime, schedulerPollingTime, newTaskIdForMovingDocToContractTree);

								if (docMovementToContractTreeJob.get("jobPassed").trim().equalsIgnoreCase("true")) {
									logger.info("docMovementToContractTreeJob Passed for Contract Id : {}.", contractId);
								} else {
									logger.error("document Movement To Contract Tree Job failed for contract id : {}, errorMessage : {}", contractId, docInheritingJob.get("errorMessage"));
									csAssert.assertTrue(false, "doc Movement To Contract Tree Job failed for contract id : " + contractId + ", errorMessage : " + docInheritingJob.get("errorMessage"));
									csAssert.assertAll();
									return;
								}

								Boolean isValidationOfContractTreePassed = validateDocumentOnContractTree(entityName, contractId);

								if (isValidationOfContractTreePassed) {
									logger.info("Validation of Documents in Contract tree is passed successfully.");
									logger.info("##############################################   Complete Flow For Pre-Signature is successfully passed. #############################");
								} else {
									logger.error("Validation of Documents in Contract tree is failed.");
									csAssert.assertTrue(false, "Validation of Documents in Contract tree is failed.");
								}
							} else {
								logger.error("Unable to submit request for moving documents to Contract Tree. Contract name : {}, id : {}", contractName, contractId);
								csAssert.assertTrue(false, "Unable to submit request for moving documents to Contract Tree. Contract name : " + contractName + ", id : " + contractId);
							}
						} else {
							logger.info("Validation failed for Document metadata on Contract show page [Contract Document Tab].");
							csAssert.assertTrue(false, "Validation failed for Document metadata on Contract show page [Contract Document Tab]");
						}
					} else {
						logger.error("Documents not inherited successfully for contract : {}, id : {}", contractName, contractId);
						csAssert.assertTrue(false, "Documents not inherited successfully for contract : " + contractName + ", id : " + contractId);
					}

				} else {
					logger.error("Unable to submit request for inheriting file from CDR to Contract. Contract name : {}, id : {}", contractName, contractId);
					csAssert.assertTrue(false, "Unable to submit request for inheriting file from CDR to Contract. Contract name : " + contractName + ", id : " + contractId);
				}

			} else {
				logger.error("Unable to mark documents as Final. linkedCDR name : {}, id : {}", linkedCDREntityName, linkedCDREntityId);
				csAssert.assertTrue(false, "Unable to mark documents as Final. linkedCDR name : " + linkedCDREntityName + ", id : " + linkedCDREntityId);
			}

		} catch (Exception e) {
			logger.error("Exception while validating document movement from CDR to Contract Tree. error : {}", e.getMessage());
			e.printStackTrace();
		}

		csAssert.assertAll();
	}

	@Test(enabled = false)
	public void testDefinitionEntity(String testingType, String environment) {

		if (testingType.contains("smoke") && environment.contains("Sandbox/VF")) {
			throw new SkipException("Skipping this test for the sandbox");
		}
		CustomAssert csAssert = new CustomAssert();
		Boolean createLocalDefinition = false;
		try {
			String createResponse = Definition.createDefinition(preSignatureSectionName, createLocalDefinition);

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
					csAssert.assertTrue(false, "Entity creation failed for Definition. Response : {}" + createResponse);
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred in testDefinitionEntity method. error = {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception occurred while creating definition entity. error = {}" + e.getMessage());
		}
		csAssert.assertAll();
	}

	private String getTabListDataPayload() {
		String payload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

		return payload;
	}

	private String getPayloadForSubmitDraft(Integer cdrDbId, String uniqueKey) {
		String payload = null;

		try {
			Show show = new Show();
			show.hitShow(160, cdrDbId);
			String showPageResponse = show.getShowJsonStr();

			if (APIUtils.validJsonResponse(showPageResponse)) {

				String comment = "{ \"requestedBy\":{ \"name\":\"requestedBy\", \"id\":12244 }, \"comments\":{ \"name\":\"comments\", \"id\":86, \"values\":\"Documentpropertyupdated\" }, \"draft\":{ \"name\":\"draft\", \"values\":true }, \"actualDate\":{ \"name\":\"actualDate\", \"id\":12243 }, \"privateCommunication\":{ \"name\":\"privateCommunication\", \"id\":12242 }, \"changeRequest\":{ \"name\":\"changeRequest\", \"id\":12246 }, \"workOrderRequest\":{ \"name\":\"workOrderRequest\", \"id\":12247 }, \"commentDocuments\":{ \"values\":[ { \"templateTypeId\":1001, \"documentFileId\":null, \"key\":\"" + uniqueKey + "\", \"documentStatusId\":1, \"permissions\":{ \"financial\":false, \"legal\":false, \"businessCase\":false }, \"performanceData\":false, \"searchable\":false, \"legal\":true } ] } }";
				JSONObject jsonObject = new JSONObject(showPageResponse);
				JSONObject jsonData = jsonObject.getJSONObject("body").getJSONObject("data");
				JSONObject commentObj = new JSONObject(comment);
				jsonData.put("comment", commentObj);

				JSONObject finalPayload = new JSONObject();
				JSONObject body = new JSONObject();
				body.put("data", jsonData);
				finalPayload.put("body", body);

				payload = finalPayload.toString();
			}

		} catch (Exception e) {
			logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}

	private Boolean validateTagOnShowPage(Integer clauseDbId) {
		Boolean isTagValidationPassed = false;

		try {
			ClausePageData clausePageData = new ClausePageData();
			clausePageData.hitClausePageData(clauseDbId);
			String response = clausePageData.getClausePageDataResponseStr();

			if (APIUtils.validJsonResponse(response)) {

				JSONObject jsonObject = new JSONObject(response);
				JSONArray tagArray = jsonObject.getJSONArray("clauseTags");
				if (tagArray.length() > 0)
					isTagValidationPassed = true;
				else
					logger.error("No Tags are linked to clause id : {}", clauseDbId);

			} else
				logger.error("clausePageData response is not valid json. response : {}", response);
		} catch (Exception e) {
			logger.error("Exception while validating tags on clause show page. Clause DbId : {}, error : {}" + clauseDbId, e.getMessage());
			e.printStackTrace();
		}
		return isTagValidationPassed;
	}

	private Integer getNewTaskId(List<Integer> allTaskIds) {
		Integer newTaskId = -1;
		try {
			logger.info("Hitting Fetch API to get new Task Id");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();

			newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
		} catch (Exception e) {
			logger.error("Exception while getting new Task id. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return newTaskId;
	}

	private List<Integer> getAllTaskIds() {
		List<Integer> allTaskIds = new ArrayList<>();
		try {
			logger.info("Hitting Fetch API");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

		} catch (Exception e) {
			logger.error("Exception while getting all task ids from user task api response. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return allTaskIds;
	}

	private Boolean validateDocumentOnContractTree(String entityName, Integer entityId) {

		Boolean flag = false;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			ContractTreeData contractTreeData = new ContractTreeData();
			contractTreeData.hitContractTreeDataListAPI(entityTypeId, entityId);
			String response = contractTreeData.getResponseContractTreeData();

			if (APIUtils.validJsonResponse(response, "ContractTreeData api response")) {
				JSONObject jsonObject = new JSONObject(response);
				JSONObject data = jsonObject.getJSONObject("body").getJSONObject("data");

				if (data.has("children")) {
					JSONArray childrenArray = data.getJSONArray("children");
					List<String> actualDocumentNames = new ArrayList<>();
					for (int i = 0; i < childrenArray.length(); i++) {
						String docName = childrenArray.getJSONObject(i).getString("text");
						actualDocumentNames.add(docName);
					}

					/*comparing actual document names found in contract tree with expected document names*/
					Boolean temp = true;
					for (String docName : documentNames) {
						if (!actualDocumentNames.contains(docName)) {
							logger.error("Document name not matched in contract tree. expected : {} but got : {}", documentNames, actualDocumentNames);
							temp = false;
							break;
						}
					}
					if (temp) {
						flag = true;
					}
				} else {
					logger.error("No children found in contract tree for contractId : {}", entityId);
				}
			} else
				logger.error("ContractTreeData api response is not valid json. response : {}", response);

		} catch (Exception e) {
			logger.error("Exception while validating document on contract tree. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private Boolean submitRequestForMovingDocumentToContractTree(Integer contractId, String entityName, List<String> documentIds) {
		Boolean flag = false;
		try {
			String payload = getPayloadForMovingDocumentToTree(contractId, entityName, documentIds);
			MoveToTreeSave moveToTreeSave = new MoveToTreeSave();
			moveToTreeSave.hitMoveToTreeSave(payload);
			String response = moveToTreeSave.getMoveToTreeSaveJsonStr();

			if (APIUtils.validJsonResponse(response, "MoveToTreeSave api response")) {
				JSONObject jsonObject = new JSONObject(response);
				Boolean success = jsonObject.getBoolean("success");
				if (success) {
					logger.info("Document successfully submitted for moving to Contract tree.");
					flag = true;
				}
			}

		} catch (Exception e) {
			logger.error("Exception while submitting request for moving documents to tree. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private Boolean validateInheritedDocumentsOnShowPage(String entityName, Integer entityId) {
		Boolean flag = false;
		try {
			String tabListDataResponse = getTabListDataResponse(entityName, entityId, "contractdocumenttablistid", true);
			if (APIUtils.validJsonResponse(tabListDataResponse)) {
				JSONObject jsonObject = new JSONObject(tabListDataResponse);
				JSONArray data = jsonObject.getJSONArray("data");

				for (int i = 0; i < data.length(); i++) {
					JSONArray innerDataArray = JSONUtility.convertJsonOnjectToJsonArray(data.getJSONObject(i));

					for (int j = 0; j < innerDataArray.length(); j++) {
						String columnName = innerDataArray.getJSONObject(j).getString("columnName");
						if ("documentname".equalsIgnoreCase(columnName)) {
							String documentName = innerDataArray.getJSONObject(j).getString("value").split("\\.")[0].trim();
							if (!documentNames.contains(documentName)) {
								logger.error("Document name not matched. Expected Document name : {}, but got : {}", documentNames, documentName);
								return false;
							} else
								flag = true;
						}
						if ("checkbox".equalsIgnoreCase(columnName)) {
							String documentId = innerDataArray.getJSONObject(j).getString("value").split(":;")[0].trim();
							if (!documentIds.contains(documentId)) {
								logger.error("Document id not matched. Expected Document id : {}, but got : {}", documentIds, documentId);
								return false;
							} else
								flag = true;
						}
					}
				}
			} else {
				logger.error("tab list data response is not valid json. Tab : Contract Document");
			}

		} catch (Exception e) {
			logger.error("Exception while validating inherited documents on show page- Contract Document Tab. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private Boolean getDocumentsInheritedStatus(String entityName, Integer entityId) {
		Boolean flag = false;
		try {
			String tabListDataResponse = getTabListDataResponse(entityName, entityId, "contractdocumenttablistid", true);
			if (APIUtils.validJsonResponse(tabListDataResponse)) {
				JSONObject jsonObject = new JSONObject(tabListDataResponse);
				JSONArray data = jsonObject.getJSONArray("data");

				if (data.length() > 0)
					flag = true;
			}
		} catch (Exception e) {
			logger.error("Exception while getting ");
		}
		return flag;
	}

	private Boolean sendRequestToInheritFile(Integer contractId, String entityName, List<String> documentIds) {
		Boolean flag = false;
		try {
			String payload = getPayloadForInheritingFile(contractId, entityName, documentIds);

			DocumentFlowSave documentFlowSave = new DocumentFlowSave();
			documentFlowSave.hitDocumentFlowSave(payload);
			String response = documentFlowSave.getDocumentFlowSaveJsonStr();

			if (APIUtils.validJsonResponse(response, "documentFlowSave api response")) {
				JSONObject jsonObject = new JSONObject(response);
				Boolean isSuccess = jsonObject.getBoolean("success");
				if (isSuccess) {
					logger.info("Documents successfully submitted for inheriting. ContractId : {}, DocumentIds : {}", contractId, documentIds);
					flag = true;
				}
			}

		} catch (Exception e) {
			logger.error("Exception while submitting request for inheriting file from CDR to Contract. ContractId : {}", contractId);
			e.printStackTrace();
		}
		return flag;
	}

	private String getPayloadForInheritingFile(Integer contractId, String entityName, List<String> documentIds) {
		String payload = null;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			Integer sourceTabId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sourceTabId"));
			Integer statusId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "statusid"));

			JSONObject jsonPayload = new JSONObject();
			jsonPayload.put("entityId", contractId);
			jsonPayload.put("entityTypeId", entityTypeId);
			jsonPayload.put("sourceTabId", sourceTabId);
			jsonPayload.put("statusId", statusId);

			JSONArray auditLogDocIds = new JSONArray();
			for (String docId : documentIds) {
				auditLogDocIds.put(docId);
			}
			jsonPayload.put("auditLogDocIds", auditLogDocIds);

			payload = jsonPayload.toString();

		} catch (Exception e) {
			logger.error("Exception while forming payload for inheriting file. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}

	private String getPayloadForMovingDocumentToTree(Integer contractId, String entityName, List<String> documentIds) {
		String payload = null;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			Integer sourceTabId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sourceTabId"));
			Integer statusId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "statusid"));

			JSONObject jsonPayload = new JSONObject();
			jsonPayload.put("baseEntityId", contractId);
			jsonPayload.put("baseEntityTypeId", entityTypeId);
			jsonPayload.put("sourceEntityTypeId", contractId);
			jsonPayload.put("sourceEntityId", entityTypeId);

			jsonPayload.put("entityId", contractId);
			jsonPayload.put("entityTypeId", entityTypeId);
			jsonPayload.put("sourceTabId", sourceTabId);
			jsonPayload.put("statusId", statusId);

			JSONArray auditLogDocIds = new JSONArray();
			for (String docId : documentIds) {
				auditLogDocIds.put(docId);
			}
			jsonPayload.put("auditLogDocIds", auditLogDocIds);

			payload = jsonPayload.toString();

		} catch (Exception e) {
			logger.error("Exception while forming payload for moving document to contract tree. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}

	private Boolean markDocumentAsFinalOnCDR(Integer expLinkedEntityId) {
		Boolean flag = false;
		String entityName = "contract draft request";

		try {
			String response = getTabListDataResponse(entityName, expLinkedEntityId, "cdrcontractdocumenttablistid");
			if (!APIUtils.validJsonResponse(response, "cdr contract document tabListData api response")) {
				logger.error("cdr contract document tabListData api response is not valid json.");
				return false;
			}
			documentIds = getDocumentIds(response);
			documentNames = getDocumentNames(response);
			String cdrEntityName = null;
			Integer cdrEntityId = null;

			for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
				cdrEntityName = entry.getValue();
				cdrEntityId = entry.getKey();
			}
			String payloadForCDRUpdate = getPayloadForMarkingDocumentAsFinal(entityName, cdrEntityId, documentIds);
			Edit edit = new Edit();
			String updateCDRResponse = edit.hitEdit(entityName, payloadForCDRUpdate);

			if (APIUtils.validJsonResponse(updateCDRResponse, "updateCDR api response")) {
				JSONObject jsonObject = new JSONObject(updateCDRResponse);
				String status = jsonObject.getJSONObject("header").getJSONObject("response").getString("status");
				if ("success".equalsIgnoreCase(status)) {
					String tabListDataResponse = getTabListDataResponse(entityName, cdrEntityId, "cdrcontractdocumenttablistid");

					if (APIUtils.validJsonResponse(tabListDataResponse)) {
						List<String> docStatus = getDocumentStatus(tabListDataResponse);
						for (String docStatusId : docStatus) {
							if (docStatusId.equals(documentStatusIdForFinal)) {
								logger.info("Document successfully marked as final.");
								flag = true;
							} else {
								logger.error("Document is not marked as final. CDR id : {}, name : {}, document name : {}", cdrEntityId, cdrEntityName, documentNames);
								return false;
							}

						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Exception while marking documents as final on CDR. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private String getPayloadForMarkingDocumentAsFinal(String entityName, Integer entityId, List<String> documentIds) {
		String payload = null;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			Show show = new Show();
			show.hitShow(entityTypeId, entityId);
			String showResponse = show.getShowJsonStr();

			if (APIUtils.validJsonResponse(showResponse, "show api for cdr")) {
				JSONObject jsonResponse = new JSONObject(showResponse);
				jsonResponse.remove("header");
				jsonResponse.remove("session");
				jsonResponse.remove("createLinks");
				jsonResponse.getJSONObject("body").remove("layoutInfo");
				jsonResponse.getJSONObject("body").remove("globalData");
				jsonResponse.getJSONObject("body").remove("errors");
				jsonResponse.getJSONObject("body").remove("actions");

				JSONArray valuesArray = new JSONArray();
				for (int i = 0; i < documentIds.size(); i++) {
					JSONObject documentStatus = new JSONObject();
					documentStatus.put("id", documentStatusIdForFinal);
					documentStatus.put("name", "");

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("documentFileId", documentIds.get(i));
					jsonObject.put("editable", true);
					jsonObject.put("documentStatus", documentStatus);

					valuesArray.put(jsonObject);
				}

				jsonResponse.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", valuesArray);
				payload = jsonResponse.toString();
			}

		} catch (Exception e) {
			logger.error("Exception while forming payload for marking document as final. [cdr update api]. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}

	private List<String> getDocumentIds(String response) {
		List<String> docIds = new ArrayList<>();
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONArray data = jsonObject.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONArray innerDataArray = JSONUtility.convertJsonOnjectToJsonArray(data.getJSONObject(i));

				for (int j = 0; j < innerDataArray.length(); j++) {
					String columnName = innerDataArray.getJSONObject(j).getString("columnName");
					if ("documentstatus".equalsIgnoreCase(columnName)) {
						String value = innerDataArray.getJSONObject(j).getString("value").split(":;")[1].trim();
						docIds.add(value);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting document ids to mark as final. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return docIds;
	}

	private List<String> getDocumentNames(String response) {
		List<String> docNames = new ArrayList<>();
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONArray data = jsonObject.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONArray innerDataArray = JSONUtility.convertJsonOnjectToJsonArray(data.getJSONObject(i));

				for (int j = 0; j < innerDataArray.length(); j++) {
					String columnName = innerDataArray.getJSONObject(j).getString("columnName");
					if ("documentname".equalsIgnoreCase(columnName)) {
						String value = innerDataArray.getJSONObject(j).getString("value").split(":;")[1].trim();
						docNames.add(value);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting document ids to mark as final. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return docNames;
	}

	private List<String> getDocumentStatus(String response) {
		List<String> docStatus = new ArrayList<>();
		try {
			JSONObject jsonObject = new JSONObject(response);
			JSONArray data = jsonObject.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONArray innerDataArray = JSONUtility.convertJsonOnjectToJsonArray(data.getJSONObject(i));

				for (int j = 0; j < innerDataArray.length(); j++) {
					String columnName = innerDataArray.getJSONObject(j).getString("columnName");
					if ("documentstatus".equalsIgnoreCase(columnName)) {
						String value = innerDataArray.getJSONObject(j).getString("value").split(":;")[0].trim();
						docStatus.add(value);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting document status to mark as final. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return docStatus;
	}

	private Boolean validateTabListDataResponseForLinkedCDREntity(String response, String expLinkedEntityName, String expLinkedEntityId) {
		Boolean flag = false;
		String actualLinkedEntityName = null;
		String actualLinkedEntityId = null;

		try {
			JSONObject jsonResponse = new JSONObject(response);
			JSONArray data = jsonResponse.getJSONArray("data");

			for (int i = 0; i < data.length(); i++) {
				JSONArray internalDataArray = JSONUtility.convertJsonOnjectToJsonArray(data.getJSONObject(i));

				for (int j = 0; j < internalDataArray.length(); j++) {
					String columnName = internalDataArray.getJSONObject(j).getString("columnName");
					if ("linkedentityname".equalsIgnoreCase(columnName)) {
						actualLinkedEntityName = internalDataArray.getJSONObject(j).getString("value");
					}
					if ("linkedentityid".equalsIgnoreCase(columnName)) {
						actualLinkedEntityId = internalDataArray.getJSONObject(j).getString("value").split(":;")[1].trim();
					}

					if (actualLinkedEntityName != null && actualLinkedEntityId != null) {
						if (expLinkedEntityId.equalsIgnoreCase(actualLinkedEntityId) && expLinkedEntityName.equalsIgnoreCase(actualLinkedEntityName)) {
							logger.info("CDR linked successfully with contract entity and validated on Contract- Linked Entities tabListData response.");
							flag = true;
							break;
						}
					}
				}
				if (flag)
					break;
			}
		} catch (Exception e) {
			logger.error("Exception while validating TabListDataResponseForLinkedCDREntity. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private String getTabListDataResponse(String entityName, Integer entityId, String propertyName) {
		return getTabListDataResponse(entityName, entityId, propertyName, false);
	}

	private String getTabListDataResponse(String entityName, Integer entityId, String propertyName, Boolean isDefaultParametersRequired) {
		String response = null;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			Integer tabListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, propertyName));

			String payload = null;

			if (isDefaultParametersRequired) {
				payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}},\"defaultParameters\":{\"targetEntityTypeId\":" + entityTypeId + ",\"targetEntityId\":" + entityId + ",\"docFlowType\":\"moveToTree\",\"baseEntityId\":" + entityId + ",\"baseEntityTypeId\":" + entityTypeId + "}}";
			} else
				payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

			ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
			listRendererTabListData.hitListRendererTabListData(tabListId, entityTypeId, entityId, payload);
			response = listRendererTabListData.getTabListDataJsonStr();

		} catch (Exception e) {
			logger.error("Exception while getting ContractLinkedEntitiesTabResponse. error : {}", e.getMessage());
			e.printStackTrace();
		}

		return response;
	}

	private Boolean linkContractWithCDR(String entityName, Integer contractDbId) {
		Boolean flag = false;
		try {
			Integer sourceEntityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String payload = getPayloadForLinkEntity(entityName, contractDbId);
			LinkEntity linkEntity = new LinkEntity();
			String response = linkEntity.hitLinkEntity(payload);

			if (APIUtils.validJsonResponse(response, "linkEntity api")) {

				String entityLinkedResponse = linkEntity.hitLinkEntity(sourceEntityTypeId, contractDbId);

				if (APIUtils.validJsonResponse(entityLinkedResponse, "linkEntity api")) {
					JSONObject jsonObject = new JSONObject(entityLinkedResponse);
					JSONObject data = jsonObject.getJSONObject("data");
					if (data.has("linkedEntities") && data.getJSONArray("linkedEntities").length() > 0) {
						String linkedEntityName = data.getJSONArray("linkedEntities").getJSONObject(0).getString("name");
						Integer linkedEntityDbId = data.getJSONArray("linkedEntities").getJSONObject(0).getInt("entityId");

						for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
							if (entry.getKey().equals(linkedEntityDbId) && entry.getValue().equalsIgnoreCase(linkedEntityName)) {
								flag = true;
								logger.info("CDR successfully linked with Contract. contract DbId : {}, CDR DbId : {}, CDR name : {}", contractDbId, linkedEntityDbId, linkedEntityName);
							} else
								logger.error("linked entity does not matched with required CDR entity. Expected [CDR DbId : {}, CDR name : {}] but Actual[CDR DbId : {}, CDR name : {}]", entry.getKey(), entry.getValue(), linkedEntityDbId, linkedEntityName);
						}
					} else {
						logger.error("failed to link entity CDR with Contract. linkEntity[GET] response : {}", entityLinkedResponse);
					}
				} else
					logger.error("linkEntity[GET] response is not valid json. response : {}", response);

			} else {
				logger.error("linkEntity[POST] response is not valid json. response : {}", response);
			}

		} catch (Exception e) {
			logger.error("Exception while linking Contract with CDR. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private String getPayloadForLinkEntity(String sourceEntityName, Integer sourceDbId) {
		String payload = null;
		Integer sourceEntityTypeId = ConfigureConstantFields.getEntityIdByName(sourceEntityName);
		Integer linkedEntityTypeId = ConfigureConstantFields.getEntityIdByName("contract draft request");
		for (Map.Entry<Integer, String> entry : newlyCreatedCDRIdAndNameMap.entrySet()) {
			payload = "{\"entityId\":" + sourceDbId + ",\"entityTypeId\":" + sourceEntityTypeId + ",\"linkEntities\":[{\"entityId\":" + entry.getKey() + ",\"entityTypeId\":" + linkedEntityTypeId + "}]}";
		}

		return payload;
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

	private Boolean performWorkflowAction(String entityName, String dbId, String actionName) {
		Boolean isPassed = true;

		try {
			String entityIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
			String entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String entitySectionUrlName = ParseConfigFile.getValueFromConfigFile(entityIdMappingFilePath, entityIdMappingFileName, entityName, "url_name");

			isPassed = performAction(actionName, Integer.parseInt(dbId), entityName, entitySectionUrlName);
		} catch (Exception e) {
			logger.error("Exception while performing workflow action for Supplier DbId : {}, Status : {}", dbId, actionName);
			e.printStackTrace();
		}

		return isPassed;
	}

	private String getNewEntityName(String entityName, Integer dbId) {
		String newEntityName = null;
		try {
			Integer entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

			Show show = new Show();
			show.hitShow(entityTypeId, dbId);
			String response = show.getShowJsonStr();
			if (APIUtils.validJsonResponse(response, "show api response")) {
				JSONObject responseJson = new JSONObject(response);
				newEntityName = responseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
			}

		} catch (Exception e) {
			logger.error("Exception while getting new entity name for : {}", entityName, dbId);
			e.printStackTrace();
		}
		return newEntityName;
	}

	private void updateContractTemplateExtraField() {
		try {
			String contractTemplateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateFilePath");
			String contractTemplateExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTemplateExtraFieldsFileName");

			String extraField = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extra fields", "clauses");
			JSONObject clauseField = new JSONObject(extraField);

			for (Map.Entry<Integer, String> entry : newlyCreatedClauseIdAndNameMap.entrySet()) {
				clauseField.getJSONArray("values").getJSONObject(0).getJSONObject("clause").put("name", entry.getValue());
				clauseField.getJSONArray("values").getJSONObject(0).getJSONObject("clause").put("id", entry.getKey());
			}

			String updatedPropertyValue = clauseField.toString();

			UpdateFile.updateConfigFileProperty(contractTemplateConfigFilePath, contractTemplateExtraFieldsConfigFileName, preSignatureSectionName, "clauses", updatedPropertyValue);

		} catch (Exception e) {
			logger.error("Exception while updating Contract Template Extra Field. error : {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private Boolean linkTemplateToCDR(String entityName, Integer cdrDbId) {
		Boolean flag = false;
		try {
			Edit edit = new Edit();
			String editEntityResponse = edit.hitEdit(entityName, cdrDbId);

			if (APIUtils.validJsonResponse(editEntityResponse)) {
				String payloadForUpdate = getPayloadForUpdateCDR(editEntityResponse);
				String updateResponse = edit.hitEdit(entityName, payloadForUpdate);

				if (APIUtils.validJsonResponse(updateResponse)) {
					JSONObject response = new JSONObject(updateResponse);
					String status = response.getJSONObject("header").getJSONObject("response").getString("status");
					if ("success".equalsIgnoreCase(status)) {
						flag = true;
					} else {
						logger.error("CDR linking with contract template failed. edit/update api response : {}", updateResponse);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while linking contract template with CDR. cdr dbId : {}, error : {}", cdrDbId, e.getMessage());
			e.printStackTrace();
		}
		return flag;
	}

	private String getPayloadForUpdateCDR(String editEntityResponse) {
		String payload = null;
		try {
			JSONObject response = new JSONObject(editEntityResponse);
			response.remove("header");
			response.remove("session");
			response.remove("actions");
			response.remove("createLinks");
			response.getJSONObject("body").remove("layoutInfo");
			response.getJSONObject("body").remove("globalData");
			response.getJSONObject("body").remove("errors");

			JSONObject mappedContractTemplates = response.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates");

			String templateId = null;
			String templateName = null;
			for (Map.Entry<Integer, String> entry : newlyCreatedCTIdAndNameMap.entrySet()) {
				templateId = entry.getKey().toString();
				templateName = entry.getValue();
			}

			String templateTypeId = null;
			String innerMappedContractTemplates = null;
			String uniqueIdentifier = NumberUtils.getUniqueCurrentTimeMS().toString();

			JSONArray values = new JSONArray();
			JSONObject value = new JSONObject();
			value.put("id", templateId);
			value.put("name", templateName);
			value.put("hasChildren", "false");
			value.put("templateTypeId", templateTypeId);
			value.put("checked", 1);
			value.put("mappedContractTemplates", innerMappedContractTemplates);
			value.put("uniqueIdentifier", uniqueIdentifier);
			//value.put("$$hashKey", "object:212");
			JSONObject mappedTags = new JSONObject();
			value.put("mappedTags", mappedTags);

			values.put(value);
			mappedContractTemplates.put("values", values);

			response.getJSONObject("body").getJSONObject("data").put("mappedContractTemplates", mappedContractTemplates);
			payload = response.toString();
		} catch (Exception e) {
			logger.error("Exception while getting payload for updating cdr. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return payload;
	}

}