package com.sirionlabs.test.notificationAlert;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.notificationAlert.NotifAlertExecutionStatusDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.sirionlabs.api.notificationAlert.NotifAlertExecutionStatus.*;


public class TestNotifAlertExecutionStatus {

    private final static Logger logger = LoggerFactory.getLogger(TestNotifAlertExecutionStatus.class);
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

        String dataFilePath = "src/test/resources/TestConfig/NotificationAlert";
        String dataFileName = "NotifAlertExecutionStatusTestData.json";

        List<NotifAlertExecutionStatusDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    NotifAlertExecutionStatusDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (NotifAlertExecutionStatusDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private NotifAlertExecutionStatusDTO getDTOObjectFromJson(JSONObject jsonObj) {
        NotifAlertExecutionStatusDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityId = jsonObj.get("entityId");
            Object ruleId = jsonObj.get("ruleId");
            String responseBody = jsonObj.getString("responseBody");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");

            dtoObject = new NotifAlertExecutionStatusDTO(testCaseId, description, expectedStatusCode, entityId, ruleId, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting NotifAlertConditionCheck DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testLanguagesForEmailActionAPI(NotifAlertExecutionStatusDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityId = dtoObject.getEntityId();
        Object ruleId = dtoObject.getRuleId();
        String expectedResponseBody = dtoObject.getResponseBody();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();


        String payload = "{}";

        //  Hitting the API
        APIResponse response = getNotifAlertExecutionStatusAPI(getNewApiPath(entityId.toString(), ruleId.toString()), getHeaders(), payload);

        // Verification of the response
        int actualResponseCode = response.getResponseCode();
        String actualresponseBody = response.getResponseBody();

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode);
        csAssert.assertTrue(actualresponseBody.contains(expectedResponseBody), "Assert failed for :" + dtoObject.getDescription());

        csAssert.assertAll();
    }


}