package com.sirionlabs.test.microservice.workflowTwo;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.workflowTwoAPI.TaskRequestDataAPI;
import com.sirionlabs.api.workflowTwoAPI.WorkflowRequestDataAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestWorkflowRequestDataAPI extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    WorkflowRequestDataAPI workflowRequestDataAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = workflowRequestDataAPI.getAllConfigForWorkflowRequestDataAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = TaskRequestDataAPI.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        workflowRequestDataAPI = new WorkflowRequestDataAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForWorkflowRequestDataAuthenticationAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C153173")
    public void workflowDataRequestDataAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String workflowType;
        ArrayList<Integer> nextNodeIdsArray;
        ArrayList<Integer> listIds;
        ArrayList<Integer> nextNodesForCountArray;
        HashMap<Integer, Integer> nodeCountMap = new HashMap<>();

        try {
            flowmap = WorkflowRequestDataAPI.getAllConfigForWorkflowRequestDataAPI("flow1");
            String authToken = jwtAuthToken;
            String resolved_payload = workflowRequestDataAPI.getPayloadWorkflowRequestData(flowmap.get("workflowid"), flowmap.get("clientid"), flowmap.get("entitytypeid"));

            validator = workflowRequestDataAPI.hitPostWorkflowRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            logger.debug("API response is:", validator.getResponse().getResponseBody());

            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");

            listIds = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodes[*].id");

            for (int i = 0; i < listIds.size(); i++) {
                nextNodesForCountArray = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodes[" + i + "].nextNodeIds");

                for (int j = 0; j < nextNodesForCountArray.size(); j++) {

                    if (nodeCountMap.containsKey(nextNodesForCountArray.get(j))) {
                        int value = nodeCountMap.get(nextNodesForCountArray.get(j));

                        nodeCountMap.put(nextNodesForCountArray.get(j), value + 1);
                    } else {
                        nodeCountMap.put(nextNodesForCountArray.get(j), 1);
                    }
                    System.out.println(nodeCountMap);
                }
            }

            for (int i = 0; i < listIds.size(); i++) {

                workflowType = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodes[" + i + "].type");
                nextNodeIdsArray = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodes[" + i + "].nextNodeIds");

                if (workflowType.equalsIgnoreCase("fork")) {
                    csAssert.assertTrue(nextNodeIdsArray.size() > 1, "Fork node size must be more than 1");
                } else if (workflowType.equalsIgnoreCase("simple")) {
                    csAssert.assertTrue(nextNodeIdsArray.size() == 1, "Simple node size must be equals to 1");
                } else {
                    csAssert.assertTrue(nodeCountMap.get(listIds.get(i)) > 1, "If a node is Join, its occurence must be more than 1");
                }
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForWorkflowRequestDataAuthenticationAPI", description = "C153174,C153175")
    public void workflowRequestDataAPIAuthenticationCasesTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = WorkflowRequestDataAPI.getAllConfigForWorkflowRequestDataAPI("auth" + flow);
            String authToken = jwtAuthToken;
            if (flowmap.get("authorization").equalsIgnoreCase("yes")) {
                authToken = authToken + "invalid";
            }

            String resolved_payload = workflowRequestDataAPI.getPayloadWorkflowRequestData(flowmap.get("workflowid"), flowmap.get("clientid"), flowmap.get("entitytypeid"));

            validator = workflowRequestDataAPI.hitPostWorkflowRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessage");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be unauthorized");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(description = "C153176")
    public void workflowRequestDataAPIWithoutRequestBodyTest() {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = WorkflowRequestDataAPI.getAllConfigForWorkflowRequestDataAPI("negativeflow");
            String authToken = jwtAuthToken;
            String resolved_payload = "";
            validator = workflowRequestDataAPI.hitPostWorkflowRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is not correct");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForWorkflowRequestDataAuthenticationAPI", description = "C153176")
    public void workflowRequestDataAPINegativeTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        boolean statusFromResponse;
        ArrayList<Integer> nodeLength;
        try {
            flowmap = WorkflowRequestDataAPI.getAllConfigForWorkflowRequestDataAPI("negative" + flow);
            String authToken = jwtAuthToken;
            String resolved_payload = workflowRequestDataAPI.getPayloadWorkflowRequestData(flowmap.get("workflowid"), flowmap.get("clientid"), flowmap.get("entitytypeid"));

            if (flowmap.get("isremovekey").equalsIgnoreCase("true")) {
                resolved_payload = workflowRequestDataAPI.removeKeyFromBody(resolved_payload, "workflowId");
            }

            validator = workflowRequestDataAPI.hitPostWorkflowRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");

            if (flowmap.get("isremovekey").equalsIgnoreCase("true")) {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages[*].message").toString();
                csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be: Workflow ID cannot be null");
            } else {
                nodeLength = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodes");
                csAssert.assertTrue(nodeLength.size() == 0, "Entity node should be empty");
            }
            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 200");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("statusfromresponse"));

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}