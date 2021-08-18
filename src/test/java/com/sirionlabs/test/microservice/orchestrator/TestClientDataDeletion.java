package com.sirionlabs.test.microservice.orchestrator;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.microservices.Email.OrchestratorMicroService;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestClientDataDeletion extends TestAPIBase {

    String domain;
    String clientId;
    Map<String, String> confmap;
    OrchestratorMicroService orchAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = OrchestratorMicroService.getAllPropertiesOfSection("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = OrchestratorMicroService.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        clientId = confmap.get("clientid");
        orchAPI = new OrchestratorMicroService();
    }

    @DataProvider()
    public Object[][] dataProviderForClientDeletion() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForClientDeletion", description = "C152139")
    public void postClientDataDeletionTest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;

        try {
            flowmap = OrchestratorMicroService.getAllPropertiesOfSection(flow);

            String authToken = jwtAuthToken;
            String resolved_payload = orchAPI.getPayloadPostDeletionRequest(clientId, RandomNumbers.getRandomNumberWithinRange(1, 1000));
            validator = orchAPI.hitPostClientDataDeletion(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");

            if (validator.getResponse().getResponseCode().toString().equalsIgnoreCase(confmap.get("forbiddenstatuscode"))) {

                csAssert.assertEquals(Boolean.toString(statusFromResponse), "false");
                String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.messages[*].message").toString().toLowerCase();
                csAssert.assertEquals(errorMessageFromResponse, confmap.get("errormessage"));
            } else {
                csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("status"));
                csAssert.assertTrue(JsonPath.read(validator.getResponse().getResponseBody(), "$.entity.clientId").toString().equalsIgnoreCase(clientId), "Error in POST request");
                csAssert.assertTrue(JsonPath.read(validator.getResponse().getResponseBody(), "$.entity.status").toString().toLowerCase().equalsIgnoreCase(flowmap.get("entitystatus")), "Error in POST request");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

   /* @Test(dataProvider = "dataProviderForClientDeletion", description = "C152154")
    public void postClientDataDeletionTestNegative(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;

        try {
            String sectionName = "negative" + flow;
            flowmap = OrchestratorMicroService.getAllPropertiesOfSection(sectionName);

            String authToken = jwtAuthToken;
            String resolved_payload = orchAPI.getPayloadPostDeletionRequest(flowmap.get("clientid"), RandomNumbers.getRandomNumberWithinRange(1, 10000));
            validator = orchAPI.hitPostClientDataDeletion(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"));
            String responseStatus = Boolean.toString(JsonPath.read(validator.getResponse().getResponseBody(), "$.status"));
            csAssert.assertEquals(responseStatus, flowmap.get("status"));
            String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.messages[*].message").toString().toLowerCase();
            csAssert.assertEquals(errorMessageFromResponse, flowmap.get("message"));

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test(dataProvider = "dataProviderForClientDeletion", description = "C152158")
    public void postClientDataDeletionTestNegativeAuth(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;

        try {
            String sectionName = "authcase" + flow;
            flowmap = OrchestratorMicroService.getAllPropertiesOfSection(sectionName);

            String authToken = jwtAuthToken;

            if (flowmap.get("authorization").equalsIgnoreCase("invalid")) {
                authToken = authToken + "invalid";
            }

            String resolved_payload = orchAPI.getPayloadPostDeletionRequest(flowmap.get("clientid"), RandomNumbers.getRandomNumberWithinRange(1, 10000));
            validator = orchAPI.hitPostClientDataDeletion(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"));

            String responseStatus = Boolean.toString(JsonPath.read(validator.getResponse().getResponseBody(), "$.status"));
            csAssert.assertEquals(responseStatus, flowmap.get("status"));
            String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.messages[*].message").toString().toLowerCase();
            csAssert.assertEquals(errorMessageFromResponse, flowmap.get("message"));


            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
*/
}
