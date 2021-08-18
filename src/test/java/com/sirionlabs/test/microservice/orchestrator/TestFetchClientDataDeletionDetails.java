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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestFetchClientDataDeletionDetails extends TestAPIBase {

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
        confmap = OrchestratorMicroService.getAllConfigForGetClientDataAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = OrchestratorMicroService.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        clientId = confmap.get("clientid");
        orchAPI = new OrchestratorMicroService();
    }

    @DataProvider()
    public Object[][] dataProviderForFetchDeletionDetails() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C152164")
    public void postFetchClientDeletionDetails() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String apiStatus;
        int priority = 1;
        int countNumberofSuccess = 0;

        try {
            flowmap = OrchestratorMicroService.getAllPropertiesOfSection("flow1");

            String authToken = jwtAuthToken;

            validator = orchAPI.hitGetClientDataDeletionDetailsAPI(executor, domain, flowmap.get("clientid"), authToken);
            List<HashMap<String, ?>> dataList = JsonPath.read(validator.getResponse().getResponseBody(), "entity.executionDetails");
            apiStatus = JsonPath.read(validator.getResponse().getResponseBody(), "entity.status");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"));

            if (apiStatus.equalsIgnoreCase("Success")) {
                for (HashMap dataMap : dataList) {
                    csAssert.assertEquals(dataMap.get("status"), "Success");
                    csAssert.assertEquals(String.valueOf(dataMap.get("priority")), String.valueOf(priority));
                    priority++;
                }
            } else {
                for (HashMap dataMap : dataList) {
                    if (dataMap.get("status").equals("Success")) {
                        countNumberofSuccess++;
                    }
                }
                csAssert.assertTrue(dataList.size() != countNumberofSuccess, "Number of APIs success should not be equal API count(Negative)");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test(description = "C152165", dataProvider = "postFetchClientDeletionDetailsNegativeCases")
    public void postFetchClientDeletionDetailsNegativeCases(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;

        try {
            flowmap = OrchestratorMicroService.getAllConfigForGetClientDataAPI("negative" + flow);
            String authToken = jwtAuthToken;

            validator = orchAPI.hitGetClientDataDeletionDetailsAPI(executor, domain, flowmap.get("clientid"), authToken);

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code is not correct");
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "status");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("status"), "status in response is not correct");

            String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "messages[*].message").toString().toLowerCase();

            if (flowmap.get("testdesc").equalsIgnoreCase("invalidclient")) {
                csAssert.assertEquals(errorMessageFromResponse, "[\"" + flowmap.get("errormessage") + ": " + flowmap.get("clientid") + "\"]", "error message is not correct");
            } else {
                csAssert.assertEquals(errorMessageFromResponse, flowmap.get("errormessage"), "error message is not correct");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test(description = "C152388", dataProvider = "postFetchClientDeletionDetailsNegativeCases")
    public void postFetchClientDeletionDetailsAuthNegativeCases(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;

        try {
            flowmap = OrchestratorMicroService.getAllConfigForGetClientDataAPI("auth" + flow);
            String authToken = jwtAuthToken;

            if (flowmap.get("authorization").equalsIgnoreCase("invalid")) {
                authToken = authToken + "invalid";
                validator = orchAPI.hitGetClientDataDeletionDetailsAPI(executor, domain, flowmap.get("clientid"), authToken);
            } else {
                validator = orchAPI.hitGetClientDataDeletionDetailswithoutAuth(executor, domain, clientId);
            }


            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code is not correct");
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "status");
            csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("status"), "status in response is not correct");

            String errorMessageFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "messages[*].message").toString().toLowerCase();

            csAssert.assertEquals(errorMessageFromResponse, flowmap.get("errormessage"), "error message is not correct");


            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @DataProvider()
    public Object[][] postFetchClientDeletionDetailsNegativeCases() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }
}
