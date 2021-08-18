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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestUpdateWorkflowAPI extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;
    FetchWorkflowDetailsAPI fetchWorkflowDetailsAPI;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForUpdateTaskAPI("envinfo");
        domain = confmap.get("domain");
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
        fetchWorkflowDetailsAPI = new FetchWorkflowDetailsAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForUpdateWorkflowAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C153237", dataProvider = "dataProviderForUpdateWorkflowAPI")
    public void updateWorkflowDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        Random rand = new Random();
        APIValidator validator;
        boolean statusFromResponse;
        String workflowName;
        String errorMessage;
        String workflowId;
        String nameAfterUpdationFromResponse;
        String resolved_payload;

        try {
            flowmap = FetchWorkflowDetailsAPI.getAllConfigForUpdateWorkflowAPI(flow);
            workflowName = "Updated Workflow atm ";
            int randomNumbForName = rand.nextInt(9999999);
            workflowName = workflowName + randomNumbForName;

            workflowId = flowmap.get("workflowid");

            validator = fetchWorkflowDetailsAPI.hitFetchWorkflowDataAPICall(executor, domain, workflowId);

            if (flowmap.get("testtype").equalsIgnoreCase("negative")) {
                resolved_payload = "";
            } else {
                resolved_payload = fetchWorkflowDetailsAPI.createPayloadForUpdateWorkflow(validator.getResponse().getResponseBody(), workflowName);
            }
            validator = fetchWorkflowDetailsAPI.hitUpdateWorkflowAPICall(executor, domain, resolved_payload);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));

            if (flowmap.get("testtype").equalsIgnoreCase("positive")) {
                logger.info("Validating for valid response");
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
                csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
                csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

                logger.info("validating Updation from Fetch call");
                validator = fetchWorkflowDetailsAPI.hitFetchWorkflowDataAPICall(executor, domain, workflowId);

                nameAfterUpdationFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.name");
                Assert.assertTrue(nameAfterUpdationFromResponse.equalsIgnoreCase(workflowName), "Name updating unsuccessful");


            } else {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase("applicationError"), "Error message is not correct");
            }
            csAssert.assertAll();
        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail(flow + " test case failed");
        }
    }

}
