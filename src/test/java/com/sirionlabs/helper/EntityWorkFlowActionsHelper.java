package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.entityWorkflowActions.*;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class EntityWorkFlowActionsHelper {

	private final static Logger logger = LoggerFactory.getLogger(EntityWorkFlowActionsHelper.class);

	private static String getPayload(String entityName, int entityId) {
		return getPayload(entityName, -1, entityId);
	}

	private static String getPayload(String entityName, int entityTypeId, int entityId) {
		String payload = null;
		try {
			if (entityTypeId == -1) {
				entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			}

			if (entityTypeId != 0) {
				logger.info("Hitting Show Api for Entity : {} and Id [{}]", entityName, entityId);
				Show showObj = new Show();
				showObj.hitShow(entityTypeId, entityId);
				String showJsonStr = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
					payload = "{\"body\":{\"data\":{";
					JSONObject jsonObj = new JSONObject(showJsonStr);
					jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

					JSONObject tempObj = jsonObj.getJSONObject("id");
					payload += "\"id\": " + tempObj.toString() + ",";

					tempObj = jsonObj.getJSONObject("stakeHolders");
					if (tempObj.has("options"))
						tempObj.remove("options");
					payload += "\"stakeHolders\": " + tempObj.toString() + ",";

					tempObj = jsonObj.getJSONObject("searchParam");
					payload += "\"searchParam\": " + tempObj.toString() + ",";

					tempObj = jsonObj.getJSONObject("entityTypeId");
					payload += "\"entityTypeId\": " + tempObj.toString();

					payload += "}}}";
				} else {
					logger.error("Invalid JSON Show Response for Entity : {} and Id [{}] . Hence couldn't create Payload.", entityName, entityId);
				}
			} else {
				logger.error("Couldn't get Entity Type Id of Entity : {}. Hence couldn't create Payload.", entityName);
			}
		} catch (Exception e) {
			logger.error("Exception while creating payload for Entity : {} and Id [{}]  {}", entityName, entityId, e.getStackTrace());
		}
		return payload;
	}

	private static String getFullPayload(String entityName, int entityTypeId, int entityId) {
		String payload = null;
		try {
			if (entityTypeId == -1) {
				entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			}

			if (entityTypeId != 0) {
				logger.info("Hitting Show Api for Entity : {} and Id [{}]", entityName, entityId);
				Show showObj = new Show();
				showObj.hitShow(entityTypeId, entityId);
				String showJsonStr = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
					payload = showJsonStr;
				} else {
					logger.error("Invalid JSON Show Response for Entity : {} and Id [{}] . Hence couldn't create Payload.", entityName, entityId);
				}
			} else {
				logger.error("Couldn't get Entity Type Id of Entity : {}. Hence couldn't create Payload.", entityName);
			}
		} catch (Exception e) {
			logger.error("Exception while creating payload for Entity : {} and Id [{}] . {}", entityName, entityId, e.getStackTrace());
		}
		return payload;
	}


	// this method will perform the action on the given entityDbId : Wrapper Funcion

	public static boolean performAction(String actionName, Integer entityDbId, String entityName, String entitySectionUrlName) {

		String performActionsOnEntitiesCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFilePath");
		String performActionsOnEntitiesCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFileName");


		logger.info("****************************************************************************************************************************");


		if (actionName.toLowerCase().contentEquals("onhold")) {
			return EntityWorkFlowActionsHelper.onHoldEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("archive")) {
			return EntityWorkFlowActionsHelper.archiveEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("restore")) {
			return EntityWorkFlowActionsHelper.restoreEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("nonworkflowactivate")) { // this activate is different from workflow activate it's counter part of on-hold
			return EntityWorkFlowActionsHelper.activateEntity(entityName, entityDbId, entitySectionUrlName);
		} else {
			String workFlowOrderSequence = null;
			try {
				workFlowOrderSequence = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entityName, actionName);
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
					{
						logger.error("Failed in Performing [{}] action on entityid  [{}] of entity [{}] because pre-requisite action got failed", actionName, entityDbId, entityName);
						return false;
					} else {
						logger.info("Hitting workflow action :{} on entity : {}", actionName, entityName);
						return EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, actionName);
					}
				} else {
					logger.warn("workFlowOrderSequence not found for action : {} and entity : {} ", actionName, entityName);
					return false;
				}

			} catch (Exception e) {
				logger.error("Error [{}] in Reading workFlowOrderSequence for actions [{}]", e.getLocalizedMessage(), actionName);
				return false;
			}

		}
	}

	public static boolean performAction(String actionName, Integer entityDbId, String entityName, Integer entityTypeId, Integer entitySectionUrlId, String entitySectionUrlName) {

		String performActionsOnEntitiesCfgFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFilePath");
		String performActionsOnEntitiesCfgFileName = ConfigureConstantFields.getConstantFieldsProperty("PerformActionsOnEntitiesCfgFileName");


		logger.info("****************************************************************************************************************************");


		if (actionName.toLowerCase().contentEquals("onhold")) {
			return EntityWorkFlowActionsHelper.onHoldEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("archive")) {
			return EntityWorkFlowActionsHelper.archiveEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("restore")) {
			return EntityWorkFlowActionsHelper.restoreEntity(entityName, entityDbId, entitySectionUrlName);
		} else if (actionName.toLowerCase().contentEquals("nonworkflowactivate")) { // this activate is different from workflow activate it's counter part of on-hold
			return EntityWorkFlowActionsHelper.activateEntity(entityName, entityDbId, entitySectionUrlName);
		} else {
			String workFlowOrderSequence = null;
			try {
				workFlowOrderSequence = ParseConfigFile.getValueFromConfigFile(performActionsOnEntitiesCfgFilePath, performActionsOnEntitiesCfgFileName, entityName, actionName);
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
					{
						logger.error("Failed in Performing [{}] action on entityid  [{}] of entity [{}] because pre-requisite action got failed", actionName, entityDbId, entityName);
						return false;
					} else {
						logger.info("Hitting workflow action :{} on entity : {}", actionName, entityName);
						return EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, actionName);
					}
				} else {
					logger.warn("workFlowOrderSequence not found for action : {} and entity : {} ", actionName, entityName);
					return false;
				}

			} catch (Exception e) {
				logger.error("Error [{}] in Reading workFlowOrderSequence for actions [{}]", e.getLocalizedMessage(), actionName);
				return false;
			}

		}
	}

	// Wrapper Function Ends here

	// Non  Workflow Actions starts

	public static Boolean archiveEntity(String entityName, int entityId, String uri) {

		Boolean result = false;
		try {
			String uriName = uri;

			logger.info("Creating Payload for archive (/archive) API for Entity : {} and Id [{}]", entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);


			if (payload != null && !payload.isEmpty()) {
				logger.info("Hitting archive (/archive) API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
				Archive Obj = new Archive();
				Obj.hitArchive(entityName, uriName, payload);
				String response = Obj.getArchiveJsonStr();

				logger.info("API Response is : [{}] ", response);
				JSONObject jsonObj = new JSONObject(response);
				if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
					if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
						logger.info("Entity : {} having Id [{}] has been Archived Successfully.", entityName, entityId);
						result = true;
					}
				}
			} else {
				logger.error("Error In Creating Payload for Archive Action");
			}

			if (!result)
				logger.error("Couldn't Archive Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while Archive Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
		}


		return result;

	}

	public static Boolean restoreEntity(String entityName, int entityId, String uri) {

		Boolean result = false;
		try {
			String uriName = uri;

			logger.info("Creating Payload for restore (/restore) API for Entity : {} and Id [{}]", entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);


			if (payload != null && !payload.isEmpty()) {
				logger.info("Hitting restore (/restore) API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
				Restore Obj = new Restore();
				Obj.hitRestore(entityName, uriName, payload);
				String response = Obj.getRestoreJsonStr();

				logger.info("API Response is : [{}] ", response);
				JSONObject jsonObj = new JSONObject(response);
				if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
					if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
						logger.info("Entity : {} having Id [{}] has been Restore Successfully.", entityName, entityId);
						result = true;
					}
				}
			} else {
				logger.error("Error In Creating Payload for Restore Action");
			}

			if (!result)
				logger.error("Couldn't Restore Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while Restore Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
		}


		return result;

	}

	public static Boolean onHoldEntity(String entityName, int entityId, String uri) {

		Boolean result = false;
		try {
			String uriName = uri;

			logger.info("Creating Payload for OnHold(/onhold) API for Entity : {} and Id [{}]", entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);

			if (payload != null && !payload.isEmpty()) {
				logger.info("Hitting OnHold(/onhold) API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
				OnHold Obj = new OnHold();
				Obj.hitOnHold(entityName, uriName, payload);
				String response = Obj.getOnHoldJsonStr();

				logger.info("API Response is : [{}] ", response);
				JSONObject jsonObj = new JSONObject(response);
				if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
					if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
						logger.info("Entity : {} having Id [{}] has been Onhold Successfully.", entityName, entityId);
						result = true;
					}
				}
			} else {
				logger.error("Error In Creating Payload for OnHold Action");
			}

			if (!result)
				logger.error("Couldn't Onhold Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while Onhold Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
		}
		return result;

	}

	public static Boolean activateEntity(String entityName, int entityId, String uri) {
		Boolean result = false;
		try {

			String uriName = uri;

			logger.info("Creating Payload for Activate (/activate) API for Entity : {} and Id [{}]", entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);

			logger.info("Hitting Activate (/activate) API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
			Activate actObj = new Activate();
			actObj.hitActivate(entityName, uriName, payload);
			String response = actObj.getActivateJsonStr();

			logger.info("API Response is : [{}] ", response);
			JSONObject jsonObj = new JSONObject(response);
			if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
				if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
					logger.info("Entity : {} having Id [{}] has been Activated Successfully.", entityName, entityId);
					result = true;
				}
			}

			if (!result)
				logger.error("Couldn't Activate Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while Activating Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
		}
		return result;
	}

	// Non Workflow Actions Ends HEre

	public static Boolean sendForPeerReviewEntity(String entityName, int entityId, String uri) {

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		try {
			String uriName = uri;


			logger.info("Creating Payload for sendForPeerReview API for Entity : {} and Id [{}]", entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);

			if (payload != null && !payload.isEmpty()) {
				logger.info("Hitting sendForPeerReview API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
				SendForPeerReview Obj = new SendForPeerReview();
				Obj.hitSendForPeerReview(entityName, uriName, payload);
				String response = Obj.getSendForPeerReviewJsonStr();

				JSONObject jsonObj = new JSONObject(response);
				if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
					if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
						logger.info("Entity : {} having Id [{}] has been sendForPeerReview Successfully.", entityName, entityId);
						result = true;
					}
				}
			} else {
				logger.error("Error In Creating Payload for sendForPeerReview Action");
			}

			if (!result)
				logger.error("Couldn't sendForPeerReview Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while sendForPeerReview Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean peerReviewCompleteEntity(String entityName, int entityId, String uri) {

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (sendForPeerReviewEntity(entityName, entityId, uri)) { // PreRequisite
			try {
				String uriName = uri;

				logger.info("Creating Payload for peerReviewComplete API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting peerReviewComplete API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					PeerReviewComplete Obj = new PeerReviewComplete();
					Obj.hitPeerReviewComplete(entityName, uriName, payload);
					String response = Obj.getPeerReviewCompleteJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been peerReviewComplete Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for peerReviewComplete Action");
				}

				if (!result)
					logger.error("Couldn't peerReviewComplete Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while peerReviewComplete Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't peerReviewComplete Entity : {} having Id [{}] Since it failed in pre-requisite operation sendForPeerReview", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean sendForInternalReviewEntity(String entityName, int entityId, String uri) {
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;

		if (peerReviewCompleteEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for sendForInternalReview API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting sendForInternalReview API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					SendForInternalReview Obj = new SendForInternalReview();
					Obj.hitSendForInternalReview(entityName, uriName, payload);
					String response = Obj.getSendForInternalReviewJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been sendForInternalReview Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for sendForInternalReview Action");
				}

				if (!result)
					logger.error("Couldn't sendForInternalReview Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while sendForInternalReview Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't sendForInternalReview Entity : {} having Id [{}] Since it failed in pre-requisite operation peerReviewComplete", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean internalReviewCompleteEntity(String entityName, int entityId, String uri) {

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (sendForInternalReviewEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for internalReviewComplete API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting internalReviewComplete API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					InternalReviewComplete Obj = new InternalReviewComplete();
					Obj.hitInternalReviewComplete(entityName, uriName, payload);
					String response = Obj.getInternalReviewCompleteJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been internalReviewComplete Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for internalReviewComplete Action");
				}

				if (!result)
					logger.error("Couldn't internalReviewComplete Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while internalReviewComplete Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't internalReviewComplete Entity : {} having Id [{}] Since it failed in pre-requisite operation sendForInternalReview", entityName.toUpperCase(), entityId);

		}

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean sendForClientReviewEntity(String entityName, int entityId, String uri) {
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (internalReviewCompleteEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for sendForClientReview API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting sendForClientReview API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					SendForClientReview Obj = new SendForClientReview();
					Obj.hitSendForClientReview(entityName, uriName, payload);
					String response = Obj.getSendForClientReviewJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been sendForClientReview Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for sendForClientReview Action");
				}

				if (!result)
					logger.error("Couldn't sendForClientReview Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while sendForClientReview Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't sendForClientReview Entity : {} having Id [{}] Since it failed in pre-requisite operation internalReviewComplete", entityName.toUpperCase(), entityId);

		}

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");


		return result;

	}

	public static Boolean rejectEntity(String entityName, int entityId, String uri) {
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (sendForClientReviewEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for reject API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting reject API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					Reject Obj = new Reject();
					Obj.hitReject(entityName, uriName, payload);
					String response = Obj.getRejectJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been reject Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for reject Action");
				}

				if (!result)
					logger.error("Couldn't reject Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while reject Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't reject Entity : {} having Id [{}] Since it failed in pre-requisite operation sendForClientReview", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean approveEntity(String entityName, int entityId, String uri) {

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (sendForClientReviewEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for approve API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting approve API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					Approve Obj = new Approve();
					Obj.hitApprove(entityName, uriName, payload);
					String response = Obj.getApproveJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been approve Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for OnHold Action");
				}

				if (!result)
					logger.error("Couldn't approve Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while approve Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't approve Entity : {} having Id [{}] Since it failed in pre-requisite operation sendForClientReview", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean publishEntity(String entityName, int entityId, String uri) {

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (approveEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for publish API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting publish API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					Publish Obj = new Publish();
					Obj.hitPublish(entityName, uriName, payload);
					String response = Obj.getPublishJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been publish Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for publish Action");
				}

				if (!result)
					logger.error("Couldn't publish Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while publish Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't publish Entity : {} having Id [{}] Since it failed in pre-requisite operation approve", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		return result;

	}

	public static Boolean inActivateEntity(String entityName, int entityId, String uri) {
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		if (publishEntity(entityName, entityId, uri)) {
			try {
				String uriName = uri;

				logger.info("Creating Payload for inActivate API for Entity : {} and Id [{}]", entityName, entityId);
				String payload = getFullPayload(entityName, -1, entityId);

				if (payload != null && !payload.isEmpty()) {
					logger.info("Hitting inActivate API for Entity : {} and Id [{}]", entityName.toUpperCase(), entityId);
					InActivate Obj = new InActivate();
					Obj.hitInactivate(entityName, uriName, payload);
					String response = Obj.getInactivateJsonStr();

					JSONObject jsonObj = new JSONObject(response);
					if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
						if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
							logger.info("Entity : {} having Id [{}] has been inActivate Successfully.", entityName, entityId);
							result = true;
						}
					}
				} else {
					logger.error("Error In Creating Payload for inActivate Action");
				}

				if (!result)
					logger.error("Couldn't inActivate Entity : {} having Id [{}]", entityName.toUpperCase(), entityId);
			} catch (Exception e) {
				logger.error("Exception while inActivate Entity : {} and Id [{}]. {}", entityName.toUpperCase(), entityId, e.getStackTrace());
			}
		} else {
			logger.error("Couldn't inActivate Entity : {} having Id [{}] Since it failed in pre-requisite operation publish", entityName.toUpperCase(), entityId);

		}
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");


		return result;

	}

	// Function to Peform Workflow Action : it's Generic Function

	/**
	 * @param entityName : it's name of the entity
	 * @param entityId   : it's db id on which action need to be performed
	 * @param uri        : it uri string value basically entitySectionUrlName (ex : disputemgmts,actionitemmgmts etc)
	 * @param actionName : it's actionName that need to performed on entityId of EntityName (ex : sendforpeerreview , reviewcomplete etc)
	 * @return
	 */
	public static Boolean performActionGeneric(String entityName, int entityId, String uri, String actionName) {
		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");

		Boolean result = false;
		try {
			String uriName = uri;

			logger.info("Creating Payload for [{}] API for Entity : {} and Id [{}]", actionName.toUpperCase(), entityName, entityId);
			String payload = getFullPayload(entityName, -1, entityId);


			// special case when we need to modify the show page response in order to perform worflow action Submit for Action Entity
			if (entityName.toLowerCase().contentEquals("actions") &&
					actionName.toLowerCase().contentEquals("submit")) {

				JSONObject jsonPayload = new JSONObject(payload);
				jsonPayload.getJSONObject("body").getJSONObject("data").getJSONObject("actionTaken").put("values", "This is for Automation Testing");
				jsonPayload.getJSONObject("body").getJSONObject("data").getJSONObject("processAreaImpacted").put("values", "This is for Automation Testing");

				payload = jsonPayload.toString();
			}

			// special case when we need to modify the show page response in order to perform worflow action Submit for Issues Entity
			if ((entityName.toLowerCase().contentEquals("issues") || entityName.toLowerCase().contentEquals("disputes")) &&
					actionName.toLowerCase().contentEquals("submit")) {

				JSONObject jsonPayload = new JSONObject(payload);
				jsonPayload.getJSONObject("body").getJSONObject("data").getJSONObject("actionTaken").put("values", "This is for Automation Testing");
				jsonPayload.getJSONObject("body").getJSONObject("data").getJSONObject("processAreaImpacted").put("values", "This is for Automation Testing");
				jsonPayload.getJSONObject("body").getJSONObject("data").getJSONObject("resolutionRemarks").put("values", "This is for Automation Testing");


				payload = jsonPayload.toString();
			}
			

			logger.debug("Payload is : [{}]", payload);

			if (payload != null && !payload.isEmpty()) {
				logger.info("Hitting [{}] API for Entity : {} and Id [{}]", actionName.toUpperCase(), entityName.toUpperCase(), entityId);
				EntityWorkFlowAction Obj = new EntityWorkFlowAction();
				Obj.hitWorkFlowAction(entityName, uriName, payload, actionName);
				String response = Obj.getResponseJsonStr();

				logger.info("API Response is : [{}] ", response);
				JSONObject jsonObj = new JSONObject(response);
				if (jsonObj.has("header") && jsonObj.getJSONObject("header").has("response") && jsonObj.getJSONObject("header").getJSONObject("response").has("status")) {
					if (jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim().equalsIgnoreCase("success")) {
						logger.info("Entity : {} having Id [{}] has been [{}] Successfully.", entityName, entityId, actionName.toUpperCase());
						result = true;
					}
				}
			} else {
				logger.error("Error In Creating Payload for [{}] Action", actionName.toUpperCase());
			}

			if (!result)
				logger.error("Couldn't perform [{}] Action Entity : {} having Id [{}]", actionName.toUpperCase(), entityName.toUpperCase(), entityId);
		} catch (Exception e) {
			logger.error("Exception while performing [{}] Action Entity : {} and Id [{}]. {}", actionName.toUpperCase(), entityName.toUpperCase(), entityId, e.getStackTrace());
		}

		logger.info("-----------------------------------------------------------------------------------------------------------------------------------");


		return result;

	}


}
