package com.sirionlabs.test.microservice.Preprocessor;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.microservices.PreprocessorMicroService.PreprocessorMicroservice;
import com.sirionlabs.api.workflowTwoAPI.TaskRequestDataAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestWorkflowRequestDataAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
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

public class TestCreatePreprocessorForGeneralTask extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    PreprocessorMicroservice preprocessorMicroservice;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = PreprocessorMicroservice.getAllConfigForCreatePreprocessorForGeneralTaskAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = TaskRequestDataAPI.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        preprocessorMicroservice = new PreprocessorMicroservice();
    }

    @DataProvider()
    public Object[][] dataProviderForCreatePreprocessorForGeneralTask() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider()
    public Object[][] dataProviderForPreprocessorsForGeneralTaskAPITest() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow3", "flow4", "flow5", "flow6"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForCreatePreprocessorForGeneralTask", description = "")
    public void createPreprocessorForGeneralTaskAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String errorMessage;
        String resolved_payload;
        String entityTypeFromResponse;
        String entityNameFromResponse;

        try {
            flowmap = PreprocessorMicroservice.getAllConfigForCreatePreprocessorForGeneralTaskAPI(flow);
            String authToken = jwtAuthToken;

            if (flowmap.get("testcasetype").equalsIgnoreCase("positive")) {
                resolved_payload = preprocessorMicroservice.getPayloadCreateGeneralTaskPreprocessor(flowmap.get("entitytype"), flowmap.get("entityname"), flowmap.get("clientid"), flowmap.get("action"));
            } else {
                resolved_payload = "";
            }

            validator = preprocessorMicroservice.hitPostPreprocessorAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"), "CREATE");
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));

            if (flowmap.get("testcasetype").equalsIgnoreCase("positive")) {
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
                csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages");
                csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

                entityTypeFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.entityType");
                entityNameFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.name.value");
                csAssert.assertTrue(entityTypeFromResponse.equalsIgnoreCase(flowmap.get("entitytype")), "EntityType is not correct");
                csAssert.assertTrue(entityNameFromResponse.equalsIgnoreCase(flowmap.get("entityname")), "EntityName is not correct");
            } else {
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "error");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is not correct");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(flow + " test case failed");
        }
    }

    @Test(dataProvider = "dataProviderForPreprocessorsForGeneralTaskAPITest", description = "")
    public void preprocessorsForGeneralTaskAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String errorMessage;
        String resolved_payload;
        boolean keyFromResponse;
        int statusIdFromResponse;

        try {
            flowmap = PreprocessorMicroservice.getAllConfigForCreatePreprocessorForGeneralTaskAPI(flow);
            String authToken = jwtAuthToken;

            resolved_payload = preprocessorMicroservice.getPayloadCreateGeneralTaskPreprocessor(flowmap.get("entitytype"), flowmap.get("entityname"), flowmap.get("clientid"), flowmap.get("action"));
            resolved_payload = preprocessorMicroservice.updatePayloadId(resolved_payload, Integer.parseInt(flowmap.get("entityid")));

            validator = preprocessorMicroservice.hitPostPreprocessorAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"), flowmap.get("apipath"));
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "$.status");
            csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");
            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages");
            csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");

            if (flowmap.get("apipath").equalsIgnoreCase("ONHOLD")) {

                keyFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.isOnHold.value");
                statusIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.status.value.id");
                csAssert.assertTrue(keyFromResponse, "isOnHold key should be true for ONHOLD CASE");
                csAssert.assertTrue(statusIdFromResponse == 2, "statusid for onHold is incorrect");
            } else if (flowmap.get("apipath").equalsIgnoreCase("ARCHIVE")) {

                keyFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.isArchive.value");
                statusIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.status.value.id");
                csAssert.assertTrue(keyFromResponse, "isArchive key should be true for ARCHIVE CASE");
                csAssert.assertTrue(statusIdFromResponse == 1, "statusid for ARCHIVE is incorrect");
            } else if (flowmap.get("apipath").equalsIgnoreCase("RESTORE")) {

                keyFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.isArchive.value");
                csAssert.assertFalse(keyFromResponse, "isArchive key should be false for RESTORE CASE");
            } else if (flowmap.get("apipath").equalsIgnoreCase("ACTIVATE")) {

                keyFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entity.value.isOnHold.value");
                csAssert.assertFalse(keyFromResponse, "isOnHold key should be true for ACTIVATE CASE");
            }

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(flow + " test case failed");
        }
    }
}
