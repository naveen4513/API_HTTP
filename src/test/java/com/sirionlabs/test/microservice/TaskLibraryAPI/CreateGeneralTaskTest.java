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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateGeneralTaskTest extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    CreateTaskAPI createTaskAPI;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("envinfo");
        domain = confmap.get("domain");
        createTaskAPI = new CreateTaskAPI();
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
    }

    @DataProvider()
    public Object[][] dataProviderForGeneralTask() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test
    public void createGeneralTaskAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String status;
        String taskName;
        String entityName;
        String errorMessage;
        String taskId;
        List<List<String>> baseTaskDeletionStatusList;
        String taskDeletionStatus;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("flow1");
            status = flowmap.get("status");
            entityName = flowmap.get("entityname");

            taskName = flowmap.get("taskname") + createTaskAPI.randomNumberGenerator();
            String resolved_payload = createTaskAPI.getPayloadCreateGeneralTask(status, taskName, flowmap.get("entitytypeid"), entityName);
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
            logger.info("General task is not created successfully");
            e.printStackTrace();
        }
    }

    @Test
    public void createGeneralTaskSameNameAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String status;
        String taskName;
        String entityName;
        String errorMessage;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("flow2");
            status = flowmap.get("status");
            entityName = flowmap.get("entityname");
            taskName = flowmap.get("taskname");

            String resolved_payload = createTaskAPI.getPayloadCreateGeneralTask(status, taskName, flowmap.get("entitytypeid"), entityName);

            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages[0].errorMessage");

            logger.info("validating for same name already exist case");
            csAssert.assertFalse(statusFromResponse, "Response should be false in case of null entity");
            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error Message is incprrect");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createGeneralTaskWithoutBodyAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;

        try {
            flowmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("flow3");

            String resolved_payload = "";

            validator = createTaskAPI.hitPostCreateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");

            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is incorrect");


            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
