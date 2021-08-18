package com.sirionlabs.test.notificationAlert;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.dto.notificationAlert.NotifAlertConditionCheckDTO;
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
import static com.sirionlabs.api.notificationAlert.NotifAlertConditionCheck.*;


public class TestNotifAlertConditionCheck {

    private final static Logger logger = LoggerFactory.getLogger(TestNotifAlertConditionCheck.class);
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
        String dataFileName = "NotifAlertConditionCheckTestData.json";

        List<NotifAlertConditionCheckDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    NotifAlertConditionCheckDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (NotifAlertConditionCheckDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private NotifAlertConditionCheckDTO getDTOObjectFromJson(JSONObject jsonObj) {
        NotifAlertConditionCheckDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityId = jsonObj.get("entityId");
            Object ruleId = jsonObj.get("ruleId");
            Object currentDate = jsonObj.get("currentDate");
            String responseBody = jsonObj.getString("responseBody");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");

            dtoObject = new NotifAlertConditionCheckDTO(testCaseId, description, expectedStatusCode, entityId, ruleId, currentDate, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting NotifAlertConditionCheck DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testNotifAlertConditionCheckAPI(NotifAlertConditionCheckDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityId = dtoObject.getEntityId();
        Object ruleId = dtoObject.getRuleId();
        Object currentDate = dtoObject.getCurrentDate();
        String expectedResponseBody = dtoObject.getResponseBody();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();


        String payload = "{}";

        //  Hitting the API
        APIResponse response = postNotifAlertConditionCheckAPI(getNewApiPath(entityId.toString(), ruleId.toString(), currentDate.toString()), getHeaders(), payload);

        // Verification of the response
        int actualResponseCode = response.getResponseCode();
        String actualresponseBody = response.getResponseBody();

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode);

        if (ParseJsonResponse.validJsonResponse(actualresponseBody)) {
            csAssert.assertTrue(actualresponseBody.contains(expectedResponseBody), "Assert failed for :" + dtoObject.getDescription());
        } else {
            csAssert.assertEquals(actualresponseBody, expectedResponseBody);

        }

        csAssert.assertAll();
    }


}
