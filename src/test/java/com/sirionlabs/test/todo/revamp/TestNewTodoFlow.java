package com.sirionlabs.test.todo.revamp;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sirionlabs.api.todo.revamp.TodoStatusWiseCount;
import com.sirionlabs.api.todo.revamp.TodoTotalCount;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static com.sirionlabs.api.todo.revamp.TodoTotalCount.*;
import static com.sirionlabs.api.todo.revamp.TodoStatusWiseCount.*;


public class TestNewTodoFlow {


    private static String configFilePath = null;
    private static String configFileName = null;
    public static int entityTypeId = 18;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestNewTodoFlow.class);
    private HashMap<String, Integer> afterStatusWithId = new HashMap<>();
    private HashMap<String, Integer> afterStatusWithCount = new HashMap<>();
    private HashMap<String, Integer> beforeStatusWithId = new HashMap<>();
    private HashMap<String, Integer> beforeStatusWithCount = new HashMap<>();
    private String expectedResponseBody;
    private JSONObject responseObject;
    private String payload, actualStatus;
    private static int totalCountBefore, totalCountAfter = 0;
    private String occurrence, entitycreation, status, status2, status3, status4, url;
    private int entityId;
    private static JSONObject obj;

    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
    }


    @Test(groups = { "minor" }, priority = 1, description = "ToDo flow for action entity for daily count", enabled = true)
    public void testToDoFlowForTodayAndUpcomingCount() {

        int k;
        for ( k = 1; k < 3; k++) {

            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");
            entitycreation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "entitycreation");
            status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status");
            status2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status2");
            status3 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status3");
            status4 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status4");
            CustomAssert csAssert = new CustomAssert();
            APIResponse response = null;
            HashMap<String, String> map = new HashMap<>();
            map.put(status2, "Approved");
            map.put(status3, "Submitted");

            logger.info("Executing test for entityType : {} and occurence : {}", entityTypeId, occurrence);

            logger.info("Hitting the API /pending-actions/{}/count before entity creation for entity :{}",occurrence, entityTypeId);
            JSONObject obj = new JSONObject(getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeaders()).getResponseBody());
            totalCountBefore = Integer.parseInt(obj.get("count").toString());

            logger.info("Hitting the API /pending-actions/{}/{} before the entity creation", occurrence, entityTypeId);
            String responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            JSONArray temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                beforeStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                beforeStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Creating a new entity of type : {}", entityTypeId);
            if(entityTypeId==18) {
                String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", entitycreation, true);
                entityId = CreateEntity.getNewEntityId(actionResponseString, "action");
            }

            logger.info("Verifying if the total count increased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==1,"Total count didn't increase by 1");

            logger.info("Hitting the API  /pending-actions/{}/{} after entity creation", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }
            logger.info("Verifying if the count increased status wise for status:{} occurence:{} entityId:{}",status, occurrence, entityTypeId);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount,status , csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Hitting the Archive API for entityType:{}", entityTypeId);
            payload = createPayload(entityId);

            if(entityTypeId == 18) {
                response = executor.post("/actionitemmgmts/archive", getHeaders(), payload).getResponse();
            }

            expectedResponseBody = response.getResponseBody();
            responseObject = new JSONObject(expectedResponseBody);
            actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
            csAssert.assertEquals(actualStatus, "success");

            logger.info("Hitting the API  /pending-actions/{}/{} ", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Verifying if the count decreased status wise for status:{} occurence:{} entityId:{}",status, occurrence, entityTypeId);
            matchStatusCountsOnDecrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Verifying if the total count decreased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==-1,"Total count didn't increase by 1");

            logger.info("Hitting the Restore API for entityType:{}", entityTypeId);
            if(entityTypeId==18) {
                response = executor.post("/actionitemmgmts/restore", getHeaders(), payload).getResponse();
            }
            expectedResponseBody = response.getResponseBody();
            responseObject = new JSONObject(expectedResponseBody);
            actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
            csAssert.assertEquals(actualStatus, "success");

            logger.info("Hitting the API  /pending-actions/{}/{}", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Verifying if the count increased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Verifying if the total count increased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==1,"Total count didn't increase by 1");

            logger.info("Changing the WF till it ends");
            // Submitting the entity
            // Changing the Workflow status
            if(entityTypeId==18) {
                changeWFStep(entityId, status2, csAssert);
            }
            // Create the status wise count afterMap for Entity
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            // Validating the count after Submission
            matchStatusCountsOnDecrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount, map.get(status2), csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            // Close the entity
            // Changing the Workflow status
            if(entityTypeId==18) {
                changeWFStep(entityId, status3, csAssert);
            }
            // Create the status wise count afterMap for Entity
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            // Validating the count after Closing the entity
            matchStatusCountsOnDecrease(beforeStatusWithCount, afterStatusWithCount, map.get(status2), csAssert);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount, map.get(status3), csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            // Approve the entity
            // Changing the Workflow status
            if(entityTypeId==18) {
                changeWFStep(entityId, status4, csAssert);
            }
            // Create the status wise count afterMap for Entity
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            // Validating the total count decrease after entity has no workflow action left
            matchStatusCountsOnDecrease(beforeStatusWithCount, afterStatusWithCount, map.get(status3), csAssert);

            logger.info("Verifying if the total count decreased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==-1,"Total count didn't increase by 1");

            if(entityId!= -1) {
                EntityOperationsHelper.deleteEntityRecord("actions", entityId);
            }
            csAssert.assertAll();
        }
    }

    @Test(priority = 2, description = "Testing /pending-actions/occurrence/entityTypeId", enabled = true)
    public void testStatusWiseCountAPIValidation () {

        logger.info("Executing testStatusWiseCountAPIValidation()");

        CustomAssert csAssert = new CustomAssert();
        APIResponse response = null;

        int k;
        for ( k = 1; k < 3; k++) {
            status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status");
            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");

            logger.info("Testing with Bad Auth token for occurrence: {}",occurrence);
            response = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeadersWithInvalidAuth());
            csAssert.assertTrue(response.getResponseCode() == 400, "testStatusWiseCountAPI Failed for Bad Auth Token scenario");

            logger.info("Testing with Bad entityTypeId token for occurrence: {}",occurrence);
            response = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, 121212121), TodoStatusWiseCount.getHeaders());
            if (response.getResponseCode() == 200) {
                JSONObject obj = new JSONObject(response.getResponseBody());
                String responseStatus = obj.getJSONObject("header").getJSONObject("response").get("status").toString();
                csAssert.assertTrue(responseStatus.equals("applicationError"), "testStatusWiseCountAPI Failed for Bad entityTypeId scenario");
            }

            // Check with Invalid occurrence Occurrence
            response = getStatusWiseCount(TodoStatusWiseCount.getApiPath("test", 18), TodoStatusWiseCount.getHeaders());
            csAssert.assertTrue(response.getResponseCode() == 400, "testStatusWiseCountAPIValidation Failed for Invalid occurrence scenario");
        }
        csAssert.assertAll();
    }

    @Test(priority = 3, description = "Testing /pending-actions/occurrence/count", enabled = true)
    public void testTotalCountAPIValidation () {

        logger.info("Executing testTotalCountAPIValidation()");

        CustomAssert csAssert = new CustomAssert();
        APIResponse response = null;

        int k;
        for ( k = 1; k < 3; k++) {
            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");

            // Check with Bad Auth Token For occurrence
            logger.info("Testing with Bad Auth toke for occurrence: {}",occurrence);
            response = getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeadersWithInvalidAuth());
            csAssert.assertTrue(response.getResponseCode() == 400, "testTotalCountAPI Failed for Bad Auth Token scenario");

            // Check with Invalid occurrence
            response = getTotalCount(TodoTotalCount.getApiPath("test"), TodoTotalCount.getHeaders());
            csAssert.assertTrue(response.getResponseCode() == 400, "testTotalCountAPI Failed for Invalid occurrence scenario");

        }
        csAssert.assertAll();

    }

    private void matchStatusCountsOnIncrease
            (HashMap < String, Integer > beforeMap, HashMap < String, Integer > afterMap, String status, CustomAssert
                    csAssert){

        if (beforeMap.containsKey(status)) {
            logger.info("{}'s Before Count : {} and After count : {}", status, beforeMap.get(status), afterMap.get(status));
            csAssert.assertTrue(afterMap.get(status) == beforeMap.get(status) + 1, "Status Count Increase test failed for status: " + status);
        } else if ((!beforeMap.containsKey(status))) {
            logger.info("{}'s Before Count : 0 and After count : {}", status, afterMap.get(status));
            csAssert.assertTrue(afterMap.get(status) == 1, "First entry failed for status: " + status);
        }

    }

    private void matchStatusCountsOnDecrease
            (HashMap<String, Integer> beforeMap, HashMap<String, Integer> afterMap, String status, CustomAssert
                    csAssert){

        if (beforeMap.containsKey(status) && beforeMap.get(status) == 1) {
            logger.info("{}'s Before Count : {} and After count : 0", status, beforeMap.get(status));
            csAssert.assertTrue(!afterMap.containsKey(status), "Status Count Decrease test failed for status: " + status);
        } else if (beforeMap.containsKey(status)) {
            logger.info("{}'s Before Count : {} and After count : {}", status, beforeMap.get(status), afterMap.get(status));
            csAssert.assertTrue(afterMap.get(status) == beforeMap.get(status)-1, "Status Count didn't decrease for status: " + status);
        }
    }

    // Create Payload
    private String createPayload ( int entityId){
        // Hit show page page of the newly created CDR
        String showResponse = ShowHelper.getShowResponse(entityTypeId, entityId);
        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        String payload = "{ \"body\": { \"data\": " + obj.getJSONObject("body").getJSONObject("data").toString() + "  }  }";
        return payload;
    }

    // payload For filling Action Taken and Process Taken
    private String createPayloadForActionSubmission ( int entityId){
        // Hit show page page of the newly created CDR
        String showResponse = ShowHelper.getShowResponse(entityTypeId, entityId);

        // Create payload from the response
        JSONObject obj = new JSONObject(showResponse);
        JSONObject obj1 = obj.getJSONObject("body").getJSONObject("data");

        // Fill Action Taken
        Map<String, Object> obj2 = obj1.getJSONObject("actionTaken").toMap();
        obj2.put("values", "Action Taken");
        obj1.put("actionTaken", obj2);

        // Fill Process Taken
        obj2 = obj1.getJSONObject("processAreaImpacted").toMap();
        obj2.put("values", "Process Taken");
        obj1.put("processAreaImpacted", obj2);

        // Return main object
        obj1.toString();
        String payload = "{ \"body\": { \"data\": " + obj1.toString() + "  }  }";
        return payload;
    }

    // Create Headers
    private Map<String, String> getHeaders () {
        Map<String, String> headers = new HashMap<>();
        try {
            headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Content-Type", "application/json;charset=UTF-8");
        } catch (Exception e) {
            logger.info("Exception occurred in creating headers for request");
        }
        return headers;
    }

    // Get the workflow state of the contract
    private String getNextWorkflowURL ( int entityId, String name){

        String actionsResponse = Actions.getActionsV3Response(entityTypeId, entityId);
        JSONObject obj = new JSONObject(actionsResponse);
        String actionURL = Actions.getAPIForActionV3(actionsResponse, name);

        return actionURL;

    }

    // Change the workflow state of the contract
    private void changeWFStep ( int entityId, String status , CustomAssert csAssert){

        url = getNextWorkflowURL(entityId, status);
        payload = createPayload(entityId);
        APIResponse response = executor.post(url, getHeaders(), payload).getResponse();
        expectedResponseBody = response.getResponseBody();
        responseObject = new JSONObject(expectedResponseBody);
        actualStatus = responseObject.getJSONObject("header").getJSONObject("response").get("status").toString();
        csAssert.assertEquals(actualStatus, "success");

    }

    private static int verifyTotalCountChange(String occurrence, CustomAssert csAssert ) {
        obj = new JSONObject(getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeaders()).getResponseBody());
        totalCountAfter = Integer.parseInt(obj.get("count").toString());
        logger.info("Before Total Count: {} and After Total Count : {}",totalCountBefore, totalCountAfter);
        if(totalCountAfter>=totalCountBefore+1) {
            totalCountBefore = new Integer(totalCountAfter);
            return 1;
        } else if(totalCountAfter==totalCountBefore-1) {
            totalCountBefore = new Integer(totalCountAfter);
            return -1;
        }
        else {return 0;}
    }
}