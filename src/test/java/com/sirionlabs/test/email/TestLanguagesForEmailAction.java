package com.sirionlabs.test.email;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.email.LanguagesForEmailActionDTO;
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
import static com.sirionlabs.api.email.LanguagesForEmailAction.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestLanguagesForEmailAction {

    private final static Logger logger = LoggerFactory.getLogger(TestLanguagesForEmailAction.class);
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
        String dataFileName = "LanguagesForEmailActionTestData.json";

        List<LanguagesForEmailActionDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    LanguagesForEmailActionDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (LanguagesForEmailActionDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private LanguagesForEmailActionDTO getDTOObjectFromJson(JSONObject jsonObj) {
        LanguagesForEmailActionDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityTypeId = jsonObj.get("entityTypeId");
            Object emailId = jsonObj.get("emailId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String responseBody = jsonObj.getString("responseBody");

            dtoObject = new LanguagesForEmailActionDTO(testCaseId, description, expectedStatusCode, entityTypeId, emailId, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting Email Action DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testLanguagesForEmailActionAPI(LanguagesForEmailActionDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityTypeId = dtoObject.getEntityTypeId();
        Object emailId = dtoObject.getEmailId();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();
        String expectedresponseBody = dtoObject.getResponseBody();


        String payload = "{}";

        //  Hitting the API
        APIResponse response = postEmailActionAPI(getNewApiPath(entityTypeId.toString(), emailId.toString()), getHeaders(), payload);

        // Verification of the response
        String actualresponseBody = response.getResponseBody();
        int actualResponseCode = response.getResponseCode();

        // Verifying valid JSON
        if (expectedResponseCode!=405 ) {
            if (!actualresponseBody.contains("<!DOCTYPE HTML SYSTEM") ) {
                csAssert.assertTrue(ParseJsonResponse.validJsonResponse(actualresponseBody), "Get api response");
            }
        }

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode);

        if(!actualresponseBody.contains("<!DOCTYPE HTML SYSTEM"))
            csAssert.assertEquals(actualresponseBody, expectedresponseBody);

        csAssert.assertAll();
    }


}
