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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestFetchTaskAPI extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    CreateTaskAPI createTaskAPI;
    PostgreSQLJDBC db = null;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForFetchTaskAPI("envinfo");
        domain = confmap.get("domain");
        createTaskAPI = new CreateTaskAPI();
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
    }

    @DataProvider()
    public Object[][] dataProviderForFetchTaskDetailsCase() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow0", "flow1", "flow2", "flow3"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C153437,C153438,C153439", dataProvider = "dataProviderForFetchTaskDetailsCase")
    public void fetchTaskAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String errorMessage;
        String taskIdTobeFetched;
        List<List<String>> baseTaskDeletionStatusList;

        try {
            flowmap = CreateTaskAPI.getAllConfigForFetchTaskAPI(flow);

            if (flowmap.get("testtype").equalsIgnoreCase("deletedtask")) {
                baseTaskDeletionStatusList = db.doSelect("select id from wf_base_task where deleted='true' order by id desc limit 1");
                taskIdTobeFetched = baseTaskDeletionStatusList.get(0).get(0);
            } else {
                taskIdTobeFetched = flowmap.get("taskid");
            }
            logger.info("hitting Fetch API");
            validator = createTaskAPI.hitFetchTaskAPICall(executor, domain, taskIdTobeFetched);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));

            if (flowmap.get("testtype").equalsIgnoreCase("stringtaskid")) {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is incorrect for:" + flow);
            } else if (flowmap.get("testtype").equalsIgnoreCase("valid")) {
                String taskIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.task.id").toString();
                csAssert.assertEquals(flowmap.get("taskid"), taskIdFromResponse, "TaskIds should be equal");
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
                csAssert.assertTrue(statusFromResponse, "Response is False,it should be true for " + flow);
            } else {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages.[0].errorMessage");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is incorrect for:" + flow);
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
                csAssert.assertFalse(statusFromResponse, "Response is true,it should be false");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public void afterClass() {
        logger.info("in After Class");
        db.closeConnection();
    }
}