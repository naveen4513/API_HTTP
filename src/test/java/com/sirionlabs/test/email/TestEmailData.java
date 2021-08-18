package com.sirionlabs.test.email;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.email.EmailDataDTO;
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
import java.util.Arrays;
import java.util.List;
import static com.sirionlabs.api.email.EmailData.*;


@Listeners(value = MyTestListenerAdapter.class)
public class TestEmailData {

    private final static Logger logger = LoggerFactory.getLogger(TestEmailData.class);
    String testingType;

    @Parameters({"TestingType"})
    @BeforeClass(groups = { "minor" })
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @BeforeClass(groups = { "minor" })
    public static String getAuthorizationKey() {
        Check checkObj = new Check();
        checkObj.hitCheck("naveen_admin", "admin123");
        return Check.getAuthorization();
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/Email";
        String dataFileName = "EmailDataTestData.json";

        List<EmailDataDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    EmailDataDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (EmailDataDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private EmailDataDTO getDTOObjectFromJson(JSONObject jsonObj) {
        EmailDataDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityTypeId = jsonObj.get("entityTypeId");
            Object emailAction = jsonObj.get("emailAction");
            Object languageId = jsonObj.get("languageId");
            String subjectData = jsonObj.getString("subjectData");
            String bodyData = jsonObj.getString("bodyData");
            String isNonWorkflowEmail = jsonObj.getString("isNonWorkflowEmail");
            String workFlowActionAdded = jsonObj.getString("workFlowActionAdded");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");

            dtoObject = new EmailDataDTO(testCaseId, description, expectedStatusCode, entityTypeId, emailAction, languageId, subjectData, bodyData, isNonWorkflowEmail, workFlowActionAdded);
        } catch (Exception e) {
            logger.error("Exception while Getting Email Action DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(groups = { "minor" }, dataProvider = "dataProviderJson", priority = 1)
    public void testEmailDataAPI(EmailDataDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityTypeId = dtoObject.getEntityTypeId();
        Object emailAction = dtoObject.getEmailAction();
        Object  languageId = dtoObject.getLanguageId();
        String expectedSubjectData = dtoObject.getSubjectData();
        String expectedBodyData = dtoObject.getBodyData();
        String expectedIsNonWorkflowEmail = dtoObject.getIsNonWorkflowEmail();
        String expectedWorkFlowActionAdded = dtoObject.getWorkFlowActionAdded();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();


        String payload = "{}";

        //  Hitting the API
        APIResponse response = postEmailActionAPI(getNewApiPath(entityTypeId.toString(), emailAction.toString(), languageId.toString()), getHeaders(), payload);

        // Verification of the response
        int actualResponseCode = response.getResponseCode();
        String actualresponseBody = response.getResponseBody();
        JSONObject jsonObj=null;
        String actualBodyData="", actualSubjectData="", actualIsNonWorkflowEmail="", actualWorkFlowActionAdded = "";

        // Verifying valid JSON
        if (expectedResponseCode!=405 ) {
            if (!actualresponseBody.contains("<!DOCTYPE HTML SYSTEM") ) {
                csAssert.assertTrue(ParseJsonResponse.validJsonResponse(actualresponseBody), "Get api response");
                jsonObj = new JSONObject(actualresponseBody);
                actualBodyData = jsonObj.getString("bodyData");
                actualSubjectData = jsonObj.getString("subjectData");
                if(!dtoObject.getTestCaseId().equals("5")) {
                    actualIsNonWorkflowEmail = jsonObj.get("isNonWorkflowEmail").toString();
                    actualWorkFlowActionAdded = jsonObj.get("workFlowActionAdded").toString();
                }
            }
        }

        // Verifying To, CC and BCC
        if(dtoObject.getDescription().contains("non workflow") || dtoObject.getDescription().contains("nonworkflow")) {
            JSONObject recipientsList = new JSONObject(jsonObj.get("toCcData").toString());
            List<String> lst = Arrays.asList("toGroupRole", "ccGroupRole", "bccGroupRole");
            for(String item: lst) {
                JSONArray arr = recipientsList.getJSONArray(item);
                csAssert.assertTrue(arr.length()>0, "The entry in "+item+" is empty.");
            }
        }

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode);

        if(!actualresponseBody.contains("<!DOCTYPE HTML SYSTEM")) {
            csAssert.assertEquals(actualSubjectData.trim(), expectedSubjectData);
            csAssert.assertTrue(actualBodyData.contains(expectedBodyData), "Expected bodyData is different than actual bodyData");
            if(!dtoObject.getTestCaseId().equals("5")) {
                csAssert.assertEquals(actualIsNonWorkflowEmail, expectedIsNonWorkflowEmail);
                csAssert.assertEquals(actualWorkFlowActionAdded, expectedWorkFlowActionAdded);
            }
        }

        csAssert.assertAll();
    }


}
