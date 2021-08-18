package com.sirionlabs.test.microservice.workflowTwo;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.workflowTwoAPI.NodeDataDetailsAPI;
import com.sirionlabs.api.workflowTwoAPI.TaskRequestDataAPI;
import com.sirionlabs.api.workflowTwoAPI.TimeZoneDataAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestTimezoneDataAPI extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestNodeDataDetailsAPI.class);
    String domain;
    Map<String, String> confmap;
    NodeDataDetailsAPI nodeDataDetailsAPI;
    TimeZoneDataAPI timezoneDataAPI;
    String jwtAuthToken;
    String issuer;
    String secretKey;
    String tokenExpiryTime;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = TimeZoneDataAPI.getAllConfigForGetTimezoneDataAPI("envinfo");
        domain = confmap.get("domain");
        issuer = confmap.get("jwtissuer");
        secretKey = confmap.get("jwtsecretkey");
        tokenExpiryTime = confmap.get("jwtexpirytime");
        jwtAuthToken = TaskRequestDataAPI.generateToken(secretKey, issuer, Integer.parseInt(tokenExpiryTime));
        nodeDataDetailsAPI = new NodeDataDetailsAPI();
        timezoneDataAPI = new TimeZoneDataAPI();
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
    }

    @DataProvider()
    public Object[][] dataProviderForTimezoneDataAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(description = "C153237")
    public void fetchTimezoneDataAPITest() {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        List<List<String>> listTimezoneIds;
        List<List<String>> listTimezone;
        String timezoneId;
        String timezoneFromDB;
        String timezoneFromAPIResponse;

        try {
            flowmap = TimeZoneDataAPI.getAllConfigForGetTimezoneDataAPI("flow");
            String authToken = jwtAuthToken;
            listTimezoneIds = db.doSelect("select id from time_zone");

            for (int i = 0; i < listTimezoneIds.size(); i++) {
                timezoneId = listTimezoneIds.get(i).get(0);
                listTimezone = db.doSelect("select time_zone from time_zone where id=" + listTimezoneIds.get(i).get(0));
                validator = timezoneDataAPI.hitGetTimezoneDataAPICall(executor, domain, timezoneId, authToken, flowmap.get("authorization"));

                timezoneFromAPIResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity");
                timezoneFromDB = listTimezone.get(0).get(0);

                logger.info("Validating timezone from Database and API response wrt ID:" + timezoneId);
                csAssert.assertTrue(timezoneFromAPIResponse.equalsIgnoreCase(timezoneFromDB), "Timezone from DB and API are not equal:" + timezoneId);
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "status");
                csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");
            }
            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            logger.info("closing the DB conncetion");
            db.closeConnection();
        }
    }

    @Test(dataProvider = "dataProviderForTimezoneDataAPI", description = "C153238,C153239")
    public void fetchTimezoneDataAPIAuthenticationCasesTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        try {
            flowmap = TimeZoneDataAPI.getAllConfigForGetTimezoneDataAPI("auth" + flow);
            String authToken = jwtAuthToken;
            if (flowmap.get("authorization").equalsIgnoreCase("yes")) {
                authToken = authToken + "invalid";
            }

            validator = timezoneDataAPI.hitGetTimezoneDataAPICall(executor, domain, flowmap.get("timezoneid"), authToken, flowmap.get("authorization"));

            errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessage");

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 401");
            csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be unauthorized");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(dataProvider = "dataProviderForTimezoneDataAPI", description = "C153240,C153241")
    public void fetchTimezoneAPINegativeTest(String flow) {

        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        String errorMessage;
        boolean statusFromResponse;
        try {
            flowmap = TimeZoneDataAPI.getAllConfigForGetTimezoneDataAPI("negative" + flow);
            String authToken = jwtAuthToken;

            validator = timezoneDataAPI.hitGetTimezoneDataAPICall(executor, domain, flowmap.get("timezoneid"), authToken, flowmap.get("authorization"));


            if (flowmap.get("stringtimezoneid").equalsIgnoreCase("true")) {
                logger.info("Validating test case for String request Id");
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "header.response.status");
                csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be: applicationError");
            } else {
                logger.info("Validating test case with non existing timezone ID");
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "messages.[0].message").toString();
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "status");
                csAssert.assertEquals(errorMessage.toLowerCase(), flowmap.get("errormessage"), "Error message should be: System Error");
                csAssert.assertEquals(Boolean.toString(statusFromResponse), flowmap.get("statusfromresponse"));
            }

            csAssert.assertEquals(String.valueOf(validator.getResponse().getResponseCode()), flowmap.get("statuscode"), "status code should be 200");

            csAssert.assertAll();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
