package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

public class WorkflowActionsHelper {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowActionsHelper.class);

    public String getWorkflowIdForActionName(int entityTypeId, int recordId, String actionName) {
        String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

        return getWorkflowIdForActionName(showResponse, entityTypeId, recordId, actionName);
    }

    public String getWorkflowIdForActionName(String showResponse, int entityTypeId, int recordId, String actionName) {
        try {
            logger.info("Getting Workflow Id for Action [{}] of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");

                JSONArray jsonArr = jsonObj.getJSONArray("actions");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String name = jsonArr.getJSONObject(i).getString("name");

                    if (name.trim().equalsIgnoreCase(actionName)) {
                        String apiValue = jsonArr.getJSONObject(i).getString("api");
                        String[] apiValueArr = apiValue.split(Pattern.quote("/"));
                        return apiValueArr[apiValueArr.length - 1].trim();
                    }
                }

                logger.info("Couldn't find Action [{}] in Show Response of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
            } else {
                logger.error("Show Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Workflow Id for Action [{}] of Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public String getWorkflowAPIForActionName(String showResponse, int entityTypeId, int recordId, String actionName) {
        try {
            logger.info("Getting Workflow API for Action [{}] of Record Id {} of Entity Type Id {}", actionName, recordId, entityTypeId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");

                JSONArray jsonArr = jsonObj.getJSONArray("actions");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String name = jsonArr.getJSONObject(i).getString("name");

                    if (name.trim().equalsIgnoreCase(actionName)) {
                        return jsonArr.getJSONObject(i).getString("api").trim();
                    }
                }
            } else {
                logger.error("Show Response is an Invalid JSON.");
            }

            logger.info("Couldn't find Action [{}] in Show Response of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
        } catch (Exception e) {
            logger.error("Exception while Getting Workflow API for Action [{}] of Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public String getWorkflowAPIForActionNameV2(String actionsResponseV2, int entityTypeId, int recordId, String actionName) {
        try {
            logger.info("Getting Workflow API for Action [{}] of Record Id {} of Entity Type Id {}", actionName, recordId, entityTypeId);

            if (ParseJsonResponse.validJsonResponse(actionsResponseV2)) {
                JSONArray jsonArr = new JSONArray(actionsResponseV2);

                for (int i = 0; i < jsonArr.length(); i++) {
                    String name = jsonArr.getJSONObject(i).getString("name");

                    if (name.trim().equalsIgnoreCase(actionName)) {
                        return jsonArr.getJSONObject(i).getString("api").trim();
                    }
                }
            } else {
                logger.error("Actions Response V2 is an Invalid JSON.");
            }

            logger.info("Couldn't find Action [{}] in actions Response V2 of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
        } catch (Exception e) {
            logger.error("Exception while Getting Workflow API for Action [{}] of Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public Boolean performWorkflowAction(int entityTypeId, int recordId, String actionName) {
        String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

        return performWorkflowAction(showResponse, entityTypeId, recordId, actionName);
    }

    public Boolean performWorkflowAction(String showResponse, int entityTypeId, int recordId, String actionName) {
        try {
            String workflowAPI = getWorkflowAPIForActionName(showResponse, entityTypeId, recordId, actionName);

            if (workflowAPI == null) {
                logger.error("Couldn't get Workflow API for Action [{}] of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
                return false;
            }

            logger.info("Hitting Show API for Workflow Action [{}] on Record Id {} of Entity Type Id {}.", actionName, recordId, entityTypeId);
            Show showObj = new Show();
            String response = showObj.hitShowForWorkflowAction(workflowAPI, showResponse, actionName);

            return ParseJsonResponse.successfulResponse(response);
        } catch (Exception e) {
            logger.error("Exception while Performing Workflow Action [{}] on Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public Boolean performWorkflowActionV2(String actionsResponseV2, int entityTypeId, int recordId, String actionName) {
        try {
            String workflowAPI = getWorkflowAPIForActionNameV2(actionsResponseV2, entityTypeId, recordId, actionName);

            if (workflowAPI == null) {
                logger.error("Couldn't get Workflow API for Action [{}] of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
                return false;
            }

            logger.info("Hitting Show API for Workflow Action [{}] on Record Id {} of Entity Type Id {}.", actionName, recordId, entityTypeId);
            Show showObj = new Show();
            showObj.hitShowVersion2(entityTypeId,recordId);
            String showResponse = showObj.getShowJsonStr();

            String response = showObj.hitShowForWorkflowActionV2(workflowAPI, showResponse, actionName);

            return ParseJsonResponse.successfulResponse(response);

        } catch (Exception e) {
            logger.error("Exception while Performing Workflow Action [{}] on Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public String performWorkflowActionV2ResponseString(int entityTypeId, int recordId, String actionName) {
        try {
            String actionApiResponse = Actions.getActionsV2Response(entityTypeId,recordId);

            String workflowAPI = getWorkflowAPIForActionNameV2(actionApiResponse, entityTypeId, recordId, actionName);

            if (workflowAPI == null) {
                logger.error("Couldn't get Workflow API for Action [{}] of Record Id {} of EntityTypeId {}", actionName, recordId, entityTypeId);
                return null;
            }

            logger.info("Hitting Show API for Workflow Action [{}] on Record Id {} of Entity Type Id {}.", actionName, recordId, entityTypeId);
            Show showObj = new Show();
            showObj.hitShowVersion2(entityTypeId,recordId);
            String showResponse = showObj.getShowJsonStr();

            String response = showObj.hitShowForWorkflowActionV2(workflowAPI, showResponse, actionName);

            return response;

        } catch (Exception e) {
            logger.error("Exception while Performing Workflow Action [{}] on Record Id {} of EntityTypeId {}. {}", actionName, recordId, entityTypeId, e.getStackTrace());
        }

        return null;
    }

    public String getLatestWorkflowIdForEntity(int entityTypeId) {
        String latestWorkflowId = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select id from work_flow where entity_type_id = " + entityTypeId + " order by id desc limit 1";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                latestWorkflowId = results.get(0).get(0);
            }

            sqlObj.closeConnection();
            return latestWorkflowId;
        } catch (Exception e) {
            logger.error("Exception while Getting Latest Workflow Id for EntityTypeId " + entityTypeId + ". " + e.getMessage());
        }

        return latestWorkflowId;
    }

    public void deleteWorkflow(String entityName, String workflowId) {
        deleteWorkflow(entityName, -1, workflowId);
    }

    public void deleteWorkflow(String entityName, int cdrRecordId, String workflowId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            logger.info("Deleting Workflow having Id {} for Entity {}", workflowId, entityName);

            //Delete entries from work_flow_task_condition table.
            String query = "delete from work_flow_task_condition where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
            sqlObj.deleteDBEntry(query);

            //Delete entries from work_flow_task_configuration table.
            query = "delete from work_flow_task_configuration where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
            sqlObj.deleteDBEntry(query);

            //Delete entries from work_flow_field_config table.
            query = "delete from work_flow_field_config where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ") ";
            sqlObj.deleteDBEntry(query);

            //Delete entries from work_flow_task_validation table.
            query = "delete from work_flow_task_validation where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
            sqlObj.deleteDBEntry(query);

            //Delete entries from work_flow_task_email table.
            query = "delete from work_flow_task_email where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
            sqlObj.deleteDBEntry(query);

            //Delete entries from work_flow_task_lead_time table.
            query = "delete from work_flow_task_lead_time  where task_id in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
            sqlObj.deleteDBEntry(query);

            if (entityName.equalsIgnoreCase("contract draft request")) {
                //Delete entries from cdr_contract_entity_link table.
                query = "delete from cdr_contract_entity_link where cdr_id = " + cdrRecordId;
                sqlObj.deleteDBEntry(query);

                //Delete entries from contract_draft_request_relation_link table.
                query = "delete from contract_draft_request_relation_link where contract_draft_request_id = " + cdrRecordId;
                sqlObj.deleteDBEntry(query);

                //Delete entries from contract_draft_request table.
                query = "delete from contract_draft_request where work_flow_task_id  in (select id from work_flow_task where work_flow_id = " + workflowId + ")";
                sqlObj.deleteDBEntry(query);
            }

            //Delete entries from work_flow_task table.
            query = "delete from work_flow_task where work_flow_id = " + workflowId + "";
            sqlObj.deleteDBEntry(query);

            //Delete entry from work_flow table.
            query = "delete from work_flow where id = " + workflowId;
            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Workflow Id {} for Entity {}. {}", workflowId, entityName, e.getMessage());
        }
    }

    public String getWorkflowValueUpdateFromTaskName(String workflowId, String taskName) {
        String valueUpdate = null;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select value_update from work_flow_task where work_flow_id = " + workflowId + " and name ='" + taskName + "'";

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                valueUpdate = results.get(0).get(0);
            }

            sqlObj.closeConnection();
            return valueUpdate;
        } catch (Exception e) {
            logger.error("Exception while Getting Value Update for Workflow Id " + workflowId + " and Task Name " + taskName + ". " + e.getMessage());
        }

        return valueUpdate;

    }

    public boolean updateValueUpdateForTaskName(String workflowId, String taskName, String valueUpdate) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "update work_flow_task set value_update ='" + valueUpdate + "' where work_flow_id = " + workflowId + " and name ='" + taskName + "'";

            return sqlObj.updateDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Updating Value Update for Workflow Id " + workflowId + " and Task Name " + taskName + ". " + e.getMessage());
        }

        return false;
    }

    public Boolean performWorkFlowStepsV2(int entityTypeId, int entityId, List<String> workFlowSteps, CustomAssert customAssert){

        Boolean approveStatus = true;
        Boolean workFlowStatus;

        try{
            for(String workFlowStep : workFlowSteps) {

                String actionApiResponse = Actions.getActionsV2Response(entityTypeId,entityId);

                workFlowStatus = performWorkflowActionV2(actionApiResponse, entityTypeId, entityId, workFlowStep);

                if(!workFlowStatus){
                    customAssert.assertTrue(false,"Unable to perform action " + workFlowStep + " on the entity Id " + entityId);
                    approveStatus = false;
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing workFlowStep on the entity Id " + entityId);
            approveStatus = false;
        }
        return approveStatus;
    }

    public Boolean performWorkFlowStepV2(int entityTypeId, int entityId, String workFlowStepName, CustomAssert customAssert){

        Boolean approveStatus = true;
        Boolean workFlowStatus;

        String actionApiResponse = Actions.getActionsV2Response(entityTypeId,entityId);

        try{

            workFlowStatus = performWorkflowActionV2(actionApiResponse, entityTypeId, entityId, workFlowStepName);

            if(!workFlowStatus){
                customAssert.assertTrue(false,"Unable to perform action " + workFlowStepName + " on the entity " + entityId);
                approveStatus = false;
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing workFlowStep on the entity " + entityId);
            approveStatus = false;
        }
        return approveStatus;
    }

    public Boolean performWorkFlowStepV2(int entityTypeId, int entityId, String workFlowStepName){

        Boolean approveStatus = true;
        Boolean workFlowStatus;

        String actionApiResponse = Actions.getActionsV2Response(entityTypeId,entityId);

        try{

            workFlowStatus = performWorkflowActionV2(actionApiResponse, entityTypeId, entityId, workFlowStepName);

            if(!workFlowStatus){

                approveStatus = false;
            }
        }catch (Exception e){

            approveStatus = false;
        }
        return approveStatus;
    }


    public void workFlowStepsToPerform(int entityTypeId, int entityId, List<String> workFlowSteps, CustomAssert customAssert){

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStep : workFlowSteps) {
                if(!workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStep,customAssert)){
                    logger.error("Error while performing workflow Step " + workFlowStep + " on entity type id " + entityTypeId + " entity Id " + entityId);
                    customAssert.assertTrue(false,"Error while performing workflow Step " + workFlowStep + " on entity type id " + entityTypeId + " entity Id " + entityId);
                }
            }
        }catch (Exception e){
            logger.error("Exception while performing workflow steps " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while performing workflow steps " + e.getStackTrace());
        }

    }
}