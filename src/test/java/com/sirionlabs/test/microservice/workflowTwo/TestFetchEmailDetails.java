package com.sirionlabs.test.microservice.workflowTwo;

import com.sirionlabs.api.workflowTwoAPI.FetchEmailDetails;
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

public class TestFetchEmailDetails extends TestAPIBase {
    String domain;

    Map<String, String> confmap;
    FetchEmailDetails emailDetailsAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = FetchEmailDetails.getAllConfigForFetchEmailDetailsAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = FetchEmailDetails.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        emailDetailsAPI = new FetchEmailDetails();
    }

    @DataProvider()
    public Object[][] dataProviderForEmailDetails() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForEmailDetails", description = "C152139")
    public void fetchEmailDetailsTest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;

        try {
            flowmap = FetchEmailDetails.getAllConfigForFetchEmailDetailsAPI(flow);
            String authToken = jwtAuthToken;
            String resolved_payload = emailDetailsAPI.getEmailDetailsRequestBody();
            validator = emailDetailsAPI.hitPostEmailDetailsAPICall(executor, domain, resolved_payload, authToken, flowmap.get("authorization"));
            System.out.println(validator.getResponse().getResponseBody());

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
