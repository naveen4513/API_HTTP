package com.sirionlabs.test.WorkflowLibraryAPI;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.TaskLibraryAPI.CreateTaskAPI;
import com.sirionlabs.api.WorkflowLibraryAPI.FetchWorkflowDetailsAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestWorkflowRequestDataAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
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
import java.util.Random;

public class TestCreateWorkflowAPI extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;
    FetchWorkflowDetailsAPI fetchWorkflowDetailsAPI;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("envinfo");
        domain = confmap.get("domain");
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
        fetchWorkflowDetailsAPI = new FetchWorkflowDetailsAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForCreateWorkflowAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(description = "C153237", dataProvider = "dataProviderForCreateWorkflowAPI")
    public void createWorkflowDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        Random rand = new Random();
        APIValidator validator;
        boolean statusFromResponse;
        String workflowName;
        String errorMessage;
        String workflowId;
        List<List<String>> wf_baseDeletionStatusList;
        String workflowDeletionStatus;

        try {
            flowmap = FetchWorkflowDetailsAPI.getAllConfigForCreateWorkflowAPI(flow);
            workflowName = flowmap.get("workflowname");
            int randomNumbForName = rand.nextInt(9999999);

            if (flowmap.get("testtype").equalsIgnoreCase("positive")) {
                workflowName = workflowName + randomNumbForName;
            }

            String resolved_payload = fetchWorkflowDetailsAPI.createRequestBodyForCreateWorkflowAPI(1, workflowName);
            validator = fetchWorkflowDetailsAPI.hitPostCreateWorkflowAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            if (statusFromResponse) {
                logger.info("Validating for valid response");
                csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
                csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

                logger.info("hitting Deletion API");

                workflowId = JsonPath.read(validator.getResponse().getResponseBody(), "entity").toString();

                fetchWorkflowDetailsAPI.hitDeleteWorkflowAPICall(executor, domain, workflowId);

                wf_baseDeletionStatusList = db.doSelect("select deleted from wf_base where id=" + workflowId);
                workflowDeletionStatus = wf_baseDeletionStatusList.get(0).get(0);

                logger.info("Assertion for checking Delete status is true After hitting delete API");
                csAssert.assertTrue(workflowDeletionStatus.equalsIgnoreCase("t"), " Deletion Status after hitting deletion API should be true");
                logger.info("Workflow has been deleted successfully");

            } else {
                csAssert.assertFalse(statusFromResponse, "Response is true,it should be false");
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages.[0].errorMessage");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase("Workflow with same name already exist"), "Error message is not correct");
            }
            csAssert.assertAll();
        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail(flow + " test case failed");
        }
    }

    @Test(description = "C153237")
    public void createWorkflowWithEmptyRequestBodyTest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;

        try {
            flowmap = FetchWorkflowDetailsAPI.getAllConfigForCreateWorkflowAPI("flow3");

            String resolved_payload = "";
            validator = fetchWorkflowDetailsAPI.hitPostCreateWorkflowAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");

            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is incorrect");

            csAssert.assertAll();
        } catch (Exception exception) {
            exception.printStackTrace();
            logger.debug("Create workflow without body test failed");
            Assert.fail();
        }
    }

    @AfterClass
    public void afterClass() {
        logger.debug("in After Class");
        db.closeConnection();
    }

}