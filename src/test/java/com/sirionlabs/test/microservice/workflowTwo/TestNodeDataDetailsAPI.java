package com.sirionlabs.test.microservice.workflowTwo;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.workflowTwoAPI.NodeDataDetailsAPI;
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
import java.util.List;
import java.util.Map;

public class TestNodeDataDetailsAPI extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestNodeDataDetailsAPI.class);
    String domain;
    Map<String, String> confmap;
    NodeDataDetailsAPI nodeDataDetailsAPI;
    WorkflowRequestDataAPI workflowRequestDataAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = nodeDataDetailsAPI.getAllConfigForNodeDataDetailsAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = TaskRequestDataAPI.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        nodeDataDetailsAPI = new NodeDataDetailsAPI();
        workflowRequestDataAPI = new WorkflowRequestDataAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForNodeDataDetailsAPI() {
        List<Object[]> allTestData = new ArrayList<>();
        String[] flows = {"flow1", "flow2", "flow3", "flow4"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForNodeDataDetailsAuthenticationAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProviderForNodeDataDetailsAPI", description = "C153173")
    public void workflowDataRequestDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String generalTaskIdFromResponse;
        String nodeIdFromResponse;
        ArrayList<Integer> listTaskIds;
        int[] taskIds;

        try {
            flowmap = NodeDataDetailsAPI.getAllConfigForNodeDataDetailsAPI(flow);
            String authToken = jwtAuthToken;
            String resolved_payload = nodeDataDetailsAPI.getPayloadNodeDataDetails(flowmap.get("nodeid"), flowmap.get("clientid"), flowmap.get("entitytypeid"), flowmap.get("action").toUpperCase());

            validator = nodeDataDetailsAPI.hitPostNodeDataDetailsAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));

            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");
            logger.info(validator.getResponse().getResponseBody());

            generalTaskIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodeDetails.[*].id").toString();
            nodeIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodeDetails.[*].id").toString();

            csAssert.assertTrue(nodeIdFromResponse.equalsIgnoreCase(flowmap.get("nodeid")), "Fetched node id is incorrect");
            csAssert.assertTrue(generalTaskIdFromResponse.equalsIgnoreCase(flowmap.get("generaltaskid")), " General taskId is wrong for the node");

            if (flow.equalsIgnoreCase("flow4")) {
                listTaskIds = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodeDetails.[*].generalAutoTasks[*].taskId");
                csAssert.assertTrue(listTaskIds.size() == 0, "General AUto taskId should be empty");
            } else {
                listTaskIds = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodeDetails.[*].generalAutoTasks[*].taskId");
                taskIds = NodeDataDetailsAPI.stringToIntArray(flowmap.get("generalautotaskids"));

                for (int i = 0; i < listTaskIds.size(); i++) {
                    csAssert.assertTrue(listTaskIds.get(i) == taskIds[i], " list task ids are equal");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForNodeDataDetailsAuthenticationAPI", description = "C153174,C153175")
    public void nodeDataDetailsAPIAuthenticationCasesTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = NodeDataDetailsAPI.getAllConfigForNodeDataDetailsAPI("auth" + flow);
            String authToken = jwtAuthToken;
            if (flowmap.get("authorization").equalsIgnoreCase("yes")) {
                authToken = authToken + "invalid";
            }

            String resolved_payload = workflowRequestDataAPI.getPayloadWorkflowRequestData(flowmap.get("workflowid"), flowmap.get("clientid"), flowmap.get("entitytypeid"));

            validator = workflowRequestDataAPI.hitPostWorkflowRequestDataAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            logger.info("API Response:" + validator.getResponse().getResponseBody());
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessage");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be unauthorized");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForNodeDataDetailsAuthenticationAPI", description = "C153176")
    public void nodeDataDetailsAPINegativeTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        boolean statusFromResponse;
        ArrayList<Integer> nodeDetailsList;
        try {
            flowmap = NodeDataDetailsAPI.getAllConfigForNodeDataDetailsAPI("negative" + flow);
            String authToken = jwtAuthToken;
            String resolved_payload = nodeDataDetailsAPI.getPayloadNodeDataDetails(flowmap.get("nodeid"), flowmap.get("clientid"), flowmap.get("entitytypeid"), flowmap.get("action").toUpperCase());

            if (flowmap.get("isremovekey").equalsIgnoreCase("true")) {
                resolved_payload = workflowRequestDataAPI.removeKeyFromBody(resolved_payload, "nodeIds");
            }

            validator = nodeDataDetailsAPI.hitPostNodeDataDetailsAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            logger.info(validator.getResponse().getResponseBody());
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");

            if (flowmap.get("isremovekey").equalsIgnoreCase("true")) {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages[*].message").toString();
                csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be: NodeId cannot be null");
            } else {
                nodeDetailsList = JsonPath.read(validator.getResponse().getResponseBody(), "entity.nodeDetails");
                csAssert.assertTrue(nodeDetailsList.size() == 0, "Entity node should be empty");
            }
            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 200");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("statusfromresponse"));

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(description = "C153176")
    public void nodeDataDetailsAPIWithoutRequestBodyTest() {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = NodeDataDetailsAPI.getAllConfigForNodeDataDetailsAPI("negativeflow");
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

}
