package com.sirionlabs.test.microservice.workflowTwo;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.workflowTwoAPI.TaskRequestDataAPI;
import com.sirionlabs.api.workflowTwoAPI.WorkflowRequestDataAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTaskRequestDataAPI extends TestAPIBase {
    String domain;
    Map<String, String> confmap;
    TaskRequestDataAPI taskRequestDataAPI;
    WorkflowRequestDataAPI workflowRequestDataAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = TaskRequestDataAPI.getAllConfigForTaskRequesrDataAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = TaskRequestDataAPI.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        taskRequestDataAPI = new TaskRequestDataAPI();
        workflowRequestDataAPI = new WorkflowRequestDataAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForTaskRequestDataAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2", "flow3", "flow4", "flow5", "flow6", "flow7"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForTaskRequestDataAuthenticationAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"authflow1", "authflow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForTaskRequestDataAPI", description = "C152139")
    public void taskRequestDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String taskType;
        String preProcessorPostValidation;
        String preProcessorType;
        String postProcessorType;

        try {
            flowmap = TaskRequestDataAPI.getAllConfigForTaskRequesrDataAPI(flow);
            String authToken = jwtAuthToken;
            String resolved_payload = taskRequestDataAPI.getPayloadTaskRequestData(flowmap.get("taskid"), flowmap.get("clientid"), flowmap.get("entitytypeid"), flowmap.get("action").toUpperCase());

            validator = taskRequestDataAPI.hitPostTaskRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            System.out.println(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            taskType = JsonPath.read(validator.getResponse().getResponseBody(), "entity.task.type").toString();
            csAssert.assertEquals(taskType.toLowerCase(), flowmap.get("tasktype"),"Task type is not correct for: "+flow);

            preProcessorPostValidation = JsonPath.read(validator.getResponse().getResponseBody(), "entity.preProcessors.[*].postValidation").toString();
            preProcessorType = JsonPath.read(validator.getResponse().getResponseBody(), "entity.preProcessors.[*].type").toString();
            postProcessorType = JsonPath.read(validator.getResponse().getResponseBody(), "entity.postProcessors.[*].type").toString();

            csAssert.assertEquals(preProcessorPostValidation, flowmap.get("preprocessorpostvalidation"), "preProcessorPostValidation value is not correct");
            csAssert.assertEquals(preProcessorType, flowmap.get("preprocessortype").toUpperCase(), "preprocessortype value is not correct");
            csAssert.assertEquals(postProcessorType, flowmap.get("postprocessortype").toUpperCase(), "postprocessortype value is not correct");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
            csAssert.assertTrue(false,"Exception caught, task not created successfully for:"+flow);
            csAssert.assertAll();
        }
    }

    @Test(dataProvider = "dataProviderForTaskRequestDataAuthenticationAPI", description = "C152139")
    public void taskRequestDataAPIAuthenticationCasesTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = TaskRequestDataAPI.getAllConfigForTaskRequesrDataAPI(flow);
            String authToken = jwtAuthToken;
            if (flowmap.get("authorization").equalsIgnoreCase("yes")) {
                authToken = authToken + "invalid";
            }

            String resolved_payload = taskRequestDataAPI.getPayloadTaskRequestData(flowmap.get("taskid"), flowmap.get("clientid"), flowmap.get("entitytypeid"), flowmap.get("action").toUpperCase());

            validator = taskRequestDataAPI.hitPostTaskRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessage");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be unauthorized");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(description = "C152139")
    public void taskRequestDataAPIWithoutRequestBodyTest() {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = TaskRequestDataAPI.getAllConfigForTaskRequesrDataAPI("negativeflow1");
            String authToken = jwtAuthToken;

            String resolved_payload = "";

            validator = taskRequestDataAPI.hitPostTaskRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is not correct");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(description = "C152139")
    public void taskRequestDataAPIWithoutTaskIdBodyTest() {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        boolean statusFromResponse;
        try {
            flowmap = TaskRequestDataAPI.getAllConfigForTaskRequesrDataAPI("negativeflow2");
            String authToken = jwtAuthToken;
            String resolved_payload = workflowRequestDataAPI.getPayloadWorkflowRequestData(flowmap.get("taskid"), flowmap.get("clientid"), flowmap.get("entitytypeid"));
            resolved_payload = workflowRequestDataAPI.removeKeyFromBody(resolved_payload, "taskIds");

            validator = taskRequestDataAPI.hitPostTaskRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages[*].message").toString();
            csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be: Task Ids cannot be null");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
