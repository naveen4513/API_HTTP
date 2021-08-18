package com.sirionlabs.test.api.EmailTokenAuthentication;
import com.sirionlabs.api.EmailTokenAuthentication.EmailTokenAuthentication;
import com.sirionlabs.dto.EmailTokenAuthentication.EmailTokenAuthenticationAPIDto;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Listeners(value = MyTestListenerAdapter.class)
public class TestEmailTokenAuthenticationAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestEmailTokenAuthenticationAPI.class);
    private String testingType;
    private String dataFilePath = "src/test/resources/TestConfig/APITestData/EmailTokenAuthentication";
    private String dataFileName = "EmailTokenAuthenticationAPIData.json";

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        List<EmailTokenAuthenticationAPIDto> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    EmailTokenAuthenticationAPIDto dtoObject = getEmailTokenAuthenticationDtoObjectFromJson(jsonObj);
                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (EmailTokenAuthenticationAPIDto dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }
        return allTestData.toArray(new Object[0][]);
    }

    private EmailTokenAuthenticationAPIDto getEmailTokenAuthenticationDtoObjectFromJson(JSONObject jsonObj) {
        EmailTokenAuthenticationAPIDto dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            String AuthToken = jsonObj.getString("authToken");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("errorMessage");

            dtoObject = new EmailTokenAuthenticationAPIDto(testCaseId, description, AuthToken, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting EmailTokenAuthentication DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(EmailTokenAuthenticationAPIDto dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            APIResponse apiResponse = EmailTokenAuthentication.getResponse(dtoObject.getAuthToken());
            int actualStatusCode = apiResponse.getResponseCode();
            int expectedStatusCode = Integer.parseInt(dtoObject.getExpectedStatusCode());
            csAssert.assertTrue(actualStatusCode == expectedStatusCode, "Actual Status Code {" + actualStatusCode + "} and Expected Status{" + expectedStatusCode + "} are Different");
             String responseBody=apiResponse.getResponseBody();
            String  expectedErrorMessage=dtoObject.getExpectedResponseMessage();
            if (!expectedErrorMessage.isEmpty()){
                String actualErrorMessage=new JSONObject(responseBody).getString("errorMessage");
                csAssert.assertTrue(actualErrorMessage.equalsIgnoreCase(expectedErrorMessage),"actual error message "+actualErrorMessage+"and expected error message"+expectedErrorMessage+" are different");
            }
        }  catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC: " + testCaseId + ". " + e.getMessage());
        }
        csAssert.assertAll();
    }
}
