package com.sirionlabs.test.microservice.orchestrator;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.microservices.Email.OrchestratorMicroService;
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

public class TestDeleteAPICall extends TestAPIBase {
    String domain;
    String clientId;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;

    Map<String, String> confmap;
    OrchestratorMicroService orchAPI;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = OrchestratorMicroService.getAllPropertiesOfSection("envinfo");
        domain = confmap.get("domain");
        clientId = confmap.get("clientid");
        orchAPI = new OrchestratorMicroService();
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = OrchestratorMicroService.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
    }

    @Test(dataProvider = "dataProviderForDeleteAPICall", description = "C152155")
    public void postDeleteAPICall(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;

        try {
            flowmap = OrchestratorMicroService.getAllConfigForDeleteAPI(flow);

            String authToken = jwtAuthToken;
            Integer serviceIdFromResponse;
            Integer clientIdFromResponse;
            boolean statusFromResponse;
            String status = OrchestratorMicroService.capitalize(flowmap.get("status"));
            String resolved_payload = orchAPI.getPayloadPostDeleteAPICall(clientId, flowmap.get("serviceid"), status);
            validator = orchAPI.hitPostDeleteAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"));

            clientIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.entity.clientId");
            csAssert.assertEquals(String.valueOf(clientIdFromResponse), clientId);

            serviceIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.entity.serviceId");
            csAssert.assertEquals(String.valueOf(serviceIdFromResponse), flowmap.get("serviceid"));

            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("responsestatus"));

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForDeletionAPICallNegativecases", description = "C152156")
    public void postDeleteAPINegativeCase(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;

        try {
            flowmap = OrchestratorMicroService.getAllConfigForDeleteAPI(flow);
            boolean statusFromResponse;

            String status = OrchestratorMicroService.capitalize(flowmap.get("statusforrequest"));
            String authToken = jwtAuthToken;
            String resolved_payload = orchAPI.getPayloadPostDeleteAPICall(flowmap.get("clientid"), flowmap.get("serviceid"), status);

            if (flowmap.get("casetype").equalsIgnoreCase("removekeyfrombody")) {
                String key = flowmap.get("keytoremove");
                String value = flowmap.get("valuetoremove");
                resolved_payload = OrchestratorMicroService.removeKeyFromBody(resolved_payload, "\"" + key + "\": " + value + ",");
            } else if (flowmap.get("casetype").equalsIgnoreCase("removebody")) {
                resolved_payload = "";
            }

            validator = orchAPI.hitPostDeleteAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"));

            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("status"));

            String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.messages[*].message").toString().toLowerCase();
            csAssert.assertEquals(errorMessageFromResponse, flowmap.get("errormessage"));

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @DataProvider(name = "dataProviderForDeletionAPCcallNegativecases")
    public Object[][] dataProviderForDeletionAPICallNegativecases() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"negativeflow1", "negativeflow2", "negativeflow3", "negativeflow4", "negativeflow5", "negativeflow6", "negativeflow7", "negativeflow8", "negativeflow9"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @DataProvider(name = "dataProviderForDeleteAPICall")
    public Object[][] dataProviderForDeleteAPICall() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2", "flow3", "flow4"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

}
