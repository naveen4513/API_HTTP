package com.sirionlabs.test.microservice.TaskLibraryAPI;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.TaskLibraryAPI.CreateTaskAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestWorkflowRequestDataAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestUpdateTaskAPI extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    CreateTaskAPI createTaskAPI;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForUpdateTaskAPI("envinfo");
        domain = confmap.get("domain");
        createTaskAPI = new CreateTaskAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForUpdateTaskDetailsCase() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow0", "flow1"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C153437,C153438,C153439", dataProvider = "dataProviderForUpdateTaskDetailsCase")
    public void updateTaskAPITest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskName;
        String entityName;
        String errorMessage;
        String taskId;
        String taskNameFromResponse;
        try {
            flowmap = CreateTaskAPI.getAllConfigForUpdateTaskAPI(flow);
            taskId = flowmap.get("taskid");
            entityName = flowmap.get("entityname");


            taskName = flowmap.get("taskname") + createTaskAPI.randomNumberGenerator();
            String resolved_payload = createTaskAPI.getPayloadForUpdateTask(taskName, flowmap.get("entitytypeid"), entityName);
            validator = createTaskAPI.hitUpdateTaskAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            logger.info("Validating for valid response");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
            csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

            logger.info("Validating Updates from fetch API");
            validator = createTaskAPI.hitFetchTaskAPICall(executor, domain, taskId);

            logger.info("*************************************************************");
            logger.info(validator.getResponse().getResponseBody());

            taskNameFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.task.name");
            csAssert.assertTrue(taskName.equalsIgnoreCase(taskNameFromResponse), "Updation not working properly");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void updateTaskWithoutBodyAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;

        try {
            flowmap = CreateTaskAPI.getAllConfigForUpdateTaskAPI("flow2");

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

    @AfterClass
    public void afterClass() {
        logger.info("in After Class");
    }
}
