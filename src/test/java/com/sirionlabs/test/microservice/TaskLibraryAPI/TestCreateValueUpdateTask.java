package com.sirionlabs.test.microservice.TaskLibraryAPI;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.TaskLibraryAPI.CreateTaskAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestWorkflowRequestDataAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class TestCreateValueUpdateTask extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    CreateTaskAPI createTaskAPI;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForCreateValueUpdateTaskAPI("envinfo");
        domain = confmap.get("domain");
        createTaskAPI = new CreateTaskAPI();
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
    }

    @Test
    public void createWFValueUpdateTaskAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskName;
        String entityName;
        String fieldId;
        String fieldName;
        String errorMessage;
        String taskId;
        List<List<String>> baseTaskDeletionStatusList;
        String taskDeletionStatus;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateValueUpdateTaskAPI("flow1");
            fieldId = flowmap.get("fieldid");
            entityName = flowmap.get("entityname");
            fieldName = flowmap.get("fieldname");

            taskName = flowmap.get("taskname") + createTaskAPI.randomNumberGenerator();
            String resolved_payload = createTaskAPI.getPayloadCreateValueUpdateTask(fieldId, fieldName, taskName, flowmap.get("entitytypeid"), entityName);
            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            logger.info("Validating for valid response");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
            csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

            logger.info("Validating Deletion API");

            taskId = JsonPath.read(validator.getResponse().getResponseBody(), "entity").toString();
            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is false before hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("f"), " Deletion Status for newly created task should be false");

            logger.info("hitting Deletion API");
            createTaskAPI.hitDeleteTaskAPICall(executor, domain, taskId);

            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is true After hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("t"), " Deletion Status after hitting deletion API should be true");
            logger.info("Task has been deleted successfully");

            csAssert.assertAll();

        } catch (Exception e) {
            logger.error("Value Update task is not created successfully");
            e.printStackTrace();
        }
    }

    @Test
    public void createValueUpdateTaskSameNameAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskName;
        String entityName;
        String fieldId;
        String fieldName;
        String errorMessage;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateValueUpdateTaskAPI("flow2");
            fieldId = flowmap.get("fieldid");
            entityName = flowmap.get("entityname");
            fieldName = flowmap.get("fieldname");
            taskName = flowmap.get("taskname");

            String resolved_payload = createTaskAPI.getPayloadCreateValueUpdateTask(fieldId, fieldName, taskName, flowmap.get("entitytypeid"), entityName);

            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages[0].errorMessage");

            logger.info("validating for same name already exist case");
            csAssert.assertFalse(statusFromResponse, "Response should be false in case of null entity");
            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error Message is incorrect");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(description = "C153480")
    public void createWFValueUpdateTaskWithLargeChainingRulesTest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskName;
        String entityName;
        String fieldId;
        String fieldName;
        String errorMessage;
        String taskId;
        List<List<String>> baseTaskDeletionStatusList;
        String taskDeletionStatus;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateValueUpdateTaskAPI("flow1");
            fieldId = flowmap.get("fieldid");
            entityName = flowmap.get("entityname");
            fieldName = flowmap.get("fieldname");
            String rule = createTaskAPI.createRuleForValueUpdateChainingTask(3);

            taskName = flowmap.get("taskname") + createTaskAPI.randomNumberGenerator();
            String resolved_payload = createTaskAPI.getPayloadCreateValueUpdateLargeChainingValTask(fieldId, fieldName, taskName, flowmap.get("entitytypeid"), entityName, rule);
            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            logger.info("Validating for valid response");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
            csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

            logger.info("Validating Deletion API");

            taskId = JsonPath.read(validator.getResponse().getResponseBody(), "entity").toString();
            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is false before hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("f"), " Deletion Status for newly created task should be false");

            logger.info("hitting Deletion API");
            createTaskAPI.hitDeleteTaskAPICall(executor, domain, taskId);

            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is true After hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("t"), " Deletion Status after hitting deletion API should be true");
            logger.info("Task has been deleted successfully");

            csAssert.assertAll();

        } catch (Exception e) {
            logger.error("Value Update task is not created successfully");
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test(description = "C153480")
    public void createWFValueUpdateTaskWithLargeRulesTest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskName;
        String entityName;
        String errorMessage;
        String taskId;
        List<List<String>> baseTaskDeletionStatusList;
        String taskDeletionStatus;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateValueUpdateTaskAPI("flow1");
            entityName = flowmap.get("entityname");
            String rule = createTaskAPI.createRuleForValueUpdateTask(100);

            taskName = flowmap.get("taskname") + createTaskAPI.randomNumberGenerator();
            String resolved_payload = createTaskAPI.getPayloadCreateValueUpdateLargeValTask(taskName, flowmap.get("entitytypeid"), entityName, rule);
            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            logger.info("Validating for valid response");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
            csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

            logger.info("Validating Deletion API");

            taskId = JsonPath.read(validator.getResponse().getResponseBody(), "entity").toString();
            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is false before hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("f"), " Deletion Status for newly created task should be false");

            logger.info("hitting Deletion API");
            createTaskAPI.hitDeleteTaskAPICall(executor, domain, taskId);

            baseTaskDeletionStatusList = db.doSelect("select deleted from wf_base_task where id=" + taskId);
            taskDeletionStatus = baseTaskDeletionStatusList.get(0).get(0);

            logger.info("Assertion for checking Delete status is true After hitting delete API");
            csAssert.assertTrue(taskDeletionStatus.equalsIgnoreCase("t"), " Deletion Status after hitting deletion API should be true");
            logger.info("Task has been deleted successfully");

            csAssert.assertAll();

        } catch (Exception e) {
            logger.error("Value Update task is not created successfully");
            e.printStackTrace();
            Assert.fail();
        }
    }
}