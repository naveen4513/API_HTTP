package com.sirionlabs.test.email;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.email.EmailActionDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sirionlabs.api.email.EmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestEmailAction {

    private final static Logger logger = LoggerFactory.getLogger(TestEmailAction.class);
    String testingType;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @BeforeClass
    public static String getAuthorizationKey() {
        Check checkObj = new Check();
        checkObj.hitCheck("naveen_admin", "admin123");
        return Check.getAuthorization();
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/Email";
        String dataFileName = "EmailActionTestData.json";

        List<EmailActionDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    EmailActionDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (EmailActionDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private EmailActionDTO getDTOObjectFromJson(JSONObject jsonObj) {
        EmailActionDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityTypeId = jsonObj.get("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String responseBody = jsonObj.getString("responseBody");

            dtoObject = new EmailActionDTO(testCaseId, description, expectedStatusCode, entityTypeId, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting Email Action DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testEmailActionAPI(EmailActionDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityTypeId = dtoObject.getEntityTypeId();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();
        String expectedresponseBody = dtoObject.getResponseBody();


        String payload = "{}";

        //  Hitting the API
        APIResponse response = postEmailActionAPI(getNewApiPath(entityTypeId.toString()), getHeaders(), payload);

        // Verification of the response
        String actualresponseBody = response.getResponseBody();
        int actualResponseCode = response.getResponseCode();

        // Verifying valid JSON
        if (expectedResponseCode!=405) {
            csAssert.assertTrue(ParseJsonResponse.validJsonResponse(actualresponseBody), "Get api response");
        }

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode, "Status code doesn't match");
        csAssert.assertEquals(actualresponseBody, expectedresponseBody, "JSON Response doesn't match");

        csAssert.assertAll();
    }


}
