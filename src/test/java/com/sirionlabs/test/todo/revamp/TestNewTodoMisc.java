package com.sirionlabs.test.todo.revamp;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.todo.revamp.TodoStatusWiseCount;
import com.sirionlabs.api.todo.revamp.TodoTotalCount;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.dbHelper.EntityDbHelper;
import com.sirionlabs.helper.entityCreation.Action;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sirionlabs.api.todo.revamp.TodoStatusWiseCount.getStatusWiseCount;
import static com.sirionlabs.api.todo.revamp.TodoTotalCount.executor;
import static com.sirionlabs.api.todo.revamp.TodoTotalCount.getTotalCount;


public class TestNewTodoMisc {


    private static String configFilePath = null;
    private static String configFileName = null;
    public static int entityTypeId = 18;
    private String testingType;
    private final static Logger logger = LoggerFactory.getLogger(TestNewTodoMisc.class);
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
    private static String responseBody;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
    }

    //C140810 - Count should decrease in the response of API /pending-actions/{occurrence}/{entityTypeId} API when the entity under test is deleted
    @Test(priority = 1, enabled = true)
    public void testC140810() {

        int k;
        for ( k = 1; k < 3; k++) {

            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");
            entitycreation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "entitycreation");
            status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status");
            CustomAssert csAssert = new CustomAssert();
            HashMap<String, String> map = new HashMap<>();
            map.put(status2, "Approved");
            map.put(status3, "Submitted");

            logger.info("Executing test for entityType : {} and occurence : {}", entityTypeId, occurrence);

            logger.info("Hitting the API /pending-actions/{}/count before entity creation for entity :{}", occurrence, entityTypeId);
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

            if(entityTypeId== 18) {
                EntityOperationsHelper.deleteEntityRecord("actions", entityId);
            }

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

            csAssert.assertAll();
        }

    }

    //C140805 - Count should decrease in the response of API /pending-actions/{occurrence}/{entityTypeId} API when workflow_task = true for the entity under test
    @Test(priority = 2, enabled = true)
    public void testC140805() {
        CustomAssert csAssert = new CustomAssert();

        EntityDbHelper dbHelper =  new EntityDbHelper();
        int k;
        for ( k = 1; k < 3; k++) {

            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");
            entitycreation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "entitycreation");
            status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status");
            HashMap<String, String> map = new HashMap<>();
            map.put(status2, "Approved");
            map.put(status3, "Submitted");

            logger.info("Executing test for entityType : {} and occurence : {}", entityTypeId, occurrence);

            logger.info("Hitting the API /pending-actions/{}/count before entity creation for entity :{}", occurrence, entityTypeId);
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

            logger.info("Hitting the API  /pending-actions/{}/{} ", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Verifying if the count increased status wise for status:{} occurrence:{} entityId:{}",status, occurrence, entityTypeId);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Verifying if the total count increased for occurrence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==1,"Total count didn't increase by 1");

            logger.info("Setting the workflow_close = true for entityId : {}", entityId);
            EntityDbHelper.toggleWFCloseFlag(entityId, "true");

            logger.info("Hitting the API  /pending-actions/{}/{} ", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Verifying if the count decreased status wise for status:{} occurrence:{} entityId:{}",status, occurrence, entityTypeId);
            matchStatusCountsOnDecrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Verifying if the total count decreased for occurence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==-1,"Total count didn't increase by 1");

            logger.info("Setting the workflow_close = false for entityId : {}", entityId);
            EntityDbHelper.toggleWFCloseFlag(entityId, "false");

            logger.info("Hitting the API  /pending-actions/{}/{} ", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            temparr = new JSONArray(responseBody);
            for (int i = 0; i < temparr.length(); i++) {
                afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
            }

            logger.info("Verifying if the count increased status wise for status:{} occurrence:{} entityId:{}",status, occurrence, entityTypeId);
            matchStatusCountsOnIncrease(beforeStatusWithCount, afterStatusWithCount, status, csAssert);
            beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
            afterStatusWithCount = new HashMap<>();

            logger.info("Verifying if the total count increased for occurrence:{} and entityId:{}",occurrence, entityTypeId);
            csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==1,"Total count didn't increase by 1");


        }
        csAssert.assertAll();
    }

    //C140801 - Count should not increase if the tier of the user doesn't match the tier of the entity in the response of API /pending-actions/{occurrence}/{entityTypeId} API
    // C140800 - Count should only increase if the tier of the user matches the tier of the entity in the response of API /pending-actions/{occurrence}/{entityTypeId} API
    @Test(priority = 3, enabled = true)
    public void testC140801() {

        String entitycreationTier2= "";
        CustomAssert csAssert = new CustomAssert();

        AppUserDbHelper dbHelper =  new AppUserDbHelper();
        int k;
        for ( k = 1; k < 3; k++) {

            logger.info("Changing the tier of the user to Tier 1");
            AppUserDbHelper.toggleTierId(1044, 1006);
            try {
                occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");
                entitycreation = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "entitycreation");
                status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "status");
                entitycreationTier2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "entitycreationtier2");

                HashMap<String, String> map = new HashMap<>();
                map.put(status2, "Approved");
                map.put(status3, "Submitted");

                logger.info("Executing test for entityType : {} and occurence : {}", entityTypeId, occurrence);

                logger.info("Hitting the API /pending-actions/{}/count before entity creation for entity :{}", occurrence, entityTypeId);
                JSONObject obj = new JSONObject(getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeaders()).getResponseBody());
                totalCountBefore = Integer.parseInt(obj.get("count").toString());

                logger.info("Hitting the API /pending-actions/{}/{} before the entity creation", occurrence, entityTypeId);
                String responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
                JSONArray temparr = new JSONArray(responseBody);
                for (int i = 0; i < temparr.length(); i++) {
                    beforeStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                    beforeStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
                }

                logger.info("Creating a new entity of type : {} and Tier 1", entityTypeId);
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

                logger.info("Creating a new entity of type : {} and Tier 2", entityTypeId);
                if(entityTypeId==18) {
                    String actionResponseString = Action.createAction("src/test/resources/Helper/EntityCreation/Action", "action.cfg", "src/test/resources/Helper/EntityCreation/Action", "actionExtraFields.cfg", entitycreationTier2, true);
                    entityId = CreateEntity.getNewEntityId(actionResponseString, "action");
                }

                logger.info("Hitting the API  /pending-actions/{}/{} ", occurrence, entityTypeId);
                responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
                temparr = new JSONArray(responseBody);
                for (int i = 0; i < temparr.length(); i++) {
                    afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
                    afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
                }

                logger.info("Verifying if the count remain the same status wise for status:{} occurrence:{} entityId:{}",status, occurrence, entityTypeId);
                csAssert.assertTrue(afterStatusWithCount.get(status).equals(beforeStatusWithCount.get(status)), "Status wise count changed but it shouldn't have.");

                logger.info("Verifying if the total count didn't change for occurrence:{} and entityId:{}",occurrence, entityTypeId);
                csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==2,"Total count changed but it shouldn't have changed");

                if(entityTypeId== 18) {
                    EntityOperationsHelper.deleteEntityRecord("actions", entityId);
                }
            } finally {
                AppUserDbHelper.toggleTierId(1044, 0);
            }

            csAssert.assertAll();
        }

    }

    //C140798 - API /pending-actions/weekly/{entityTypeId} should not work for few entities which don't have due date
    @Test(priority = 4, enabled = true)
    public void testC140798() {

        CustomAssert csAssert = new CustomAssert();
        occurrence = "weekly";
        int[] entitiesToTest= {1, 12,14, 61, 86, 138, 140};
        for (int i = 0; i < entitiesToTest.length; i++) {

            entityTypeId = entitiesToTest[i];
            logger.info("Hitting the API  /pending-actions/{}/{} after entity creation", occurrence, entityTypeId);
            responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
            String expectedResponse= responseBody;
            csAssert.assertEquals("[]",expectedResponse,"Test C140798 failed for : "+entityTypeId);
        }
        csAssert.assertAll();
    }

    //C140799 - 'Other' type of contract should not add count in ToDo API /pending-actions/daily/61
    @Test(priority = 5, enabled = true)
    public void testC140799() {

        status = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_2", "status");
        occurrence = "daily";
        entityTypeId = 61;
        CustomAssert csAssert = new CustomAssert();

        logger.info("Executing test for entityType : {} and occurence : {}", entityTypeId, occurrence);

        logger.info("Hitting the API /pending-actions/{}/count before entity creation for entity :{}", occurrence, entityTypeId);
        JSONObject obj = new JSONObject(getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeaders()).getResponseBody());
        totalCountBefore = Integer.parseInt(obj.get("count").toString());

        logger.info("Hitting the API /pending-actions/{}/{} before the entity creation", occurrence, entityTypeId);
        String responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
        JSONArray temparr = new JSONArray(responseBody);
        for (int i = 0; i < temparr.length(); i++) {
            beforeStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
            beforeStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
        }

        logger.info("Creating a new contract of type : Other");
        if(entityTypeId==61) {
            logger.info("Creating Contract for Flow [{}]", "contract flow inbound email action");
            String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "other type contract",
                    true);
            entityId = CreateEntity.getNewEntityId(contractResponse, "contracts");
        }

        logger.info("Verify that  the total count didn't for occurrence:{} and entityId:{}",occurrence, entityTypeId);
        csAssert.assertTrue(verifyTotalCountChange(occurrence,csAssert)==2,"Total count didn't increase by 1");

        logger.info("Hitting the API  /pending-actions/{}/{} after entity creation", occurrence, entityTypeId);
        responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
        temparr = new JSONArray(responseBody);
        for (int i = 0; i < temparr.length(); i++) {
            afterStatusWithCount.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("count").toString()));
            afterStatusWithId.put(temparr.getJSONObject(i).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(i).get("statusId").toString()));
        }

        logger.info("Verifying if the count remained same for status:{} occurence:{} entityId:{}",status, occurrence, entityTypeId);
        csAssert.assertEquals(afterStatusWithCount.get(status), beforeStatusWithCount.get(status), "Count shouldn't have changed.");
        beforeStatusWithCount = new HashMap<>(afterStatusWithCount);
        afterStatusWithCount = new HashMap<>();

        // Create a contract of MSA type
        if(entityTypeId==61) {
            logger.info("Creating Contract for Flow [{}]", "contract flow inbound email action");
            String contractResponse = Contract.createContract("src/test/resources/Helper/EntityCreation/Contract", "contract.cfg", "src/test/resources/Helper/EntityCreation/Contract", "contractExtraFields.cfg", "mic 80 flow 1 for inbound email action",
                    true);
            entityId = CreateEntity.getNewEntityId(contractResponse, "contracts");
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

        csAssert.assertAll();

    }

    //C141083 - Total count from API /pending-actions/{occurrence}/count should be equal to the sum of total count of /pending-actions/{occurrence}/{entityTypeId} of all entities
    @Test(priority = 6, enabled = true)
    public void testC141083() {

        CustomAssert csAssert = new CustomAssert();

        String[] entitiesForToDo = {"actions", "contracts", "issues", "contract draft request", "suppliers", "obligations", "child obligations", "service levels", "child service levels", "interpretations", "disputes",  "change requests", "invoices", "work order requests", "governance body", "governance body meetings", "clauses", "contract templates", "consumptions"};
        int k;
        int actualTotalCount=0;
        for ( k = 1; k < 3; k++) {
            totalCountBefore = 0;
            occurrence = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "todo_" + k + "", "occurrence");

            logger.info("Hitting the API /pending-actions/{}/count for entity :{}", occurrence, entityTypeId);
            JSONObject obj = new JSONObject(getTotalCount(TodoTotalCount.getApiPath(occurrence), TodoTotalCount.getHeaders()).getResponseBody());
            totalCountBefore = Integer.parseInt(obj.get("count").toString());

            for (int i = 0; i < entitiesForToDo.length; i++) {
                beforeStatusWithCount.clear();
                int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile("src/test/resources/CommonConfigFiles", "EntityIdMapping.cfg", entitiesForToDo[i], "entity_type_id"));

                logger.info("Hitting the API /pending-actions/{}/{}", occurrence, entityTypeId);
                String responseBody = getStatusWiseCount(TodoStatusWiseCount.getApiPath(occurrence, entityTypeId), TodoStatusWiseCount.getHeaders()).getResponseBody();
                if(!responseBody.equals("[]")) {
                    JSONArray temparr = new JSONArray(responseBody);
                    for (int l = 0; l < temparr.length(); l++) {
                        beforeStatusWithCount.put(temparr.getJSONObject(l).get("statusName").toString(), Integer.parseInt(temparr.getJSONObject(l).get("count").toString()));
                    }


                    logger.info("Working on entity : {}", entitiesForToDo[i]);
                    Set<String> set = beforeStatusWithCount.keySet();
                    Object[] statusArray = set.toArray();
                    for (int j = 0; j < set.size(); j++) {
                        int temp = beforeStatusWithCount.get(statusArray[j].toString());
                        logger.info("Count for {} : {}", statusArray[j].toString(), temp);
                        actualTotalCount = actualTotalCount + temp;
                    }
                }

            }
            logger.info("Expected Total Count : {} || Actual Total Count : {} for occurrence : {}", totalCountBefore,actualTotalCount, occurrence);
            csAssert.assertEquals(actualTotalCount, totalCountBefore, "Total count is not same");
            actualTotalCount = 0;
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
        else if(totalCountAfter==totalCountBefore){
            return 2;
        }
        else {return 0;}
    }

}