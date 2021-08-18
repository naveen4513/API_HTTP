package com.sirionlabs.test.inboundEmailAction;

import com.sirionlabs.dto.inbound.WFActionDTO;
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
import static com.sirionlabs.api.inboundEmailAction.WFAction.*;


public class TestWFAction {

    private final static Logger logger = LoggerFactory.getLogger(TestWFAction.class);
    String testingType;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/InboundEmail";
        String dataFileName = "WFActionTestData.json";

        List<WFActionDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    WFActionDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (WFActionDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private WFActionDTO getDTOObjectFromJson(JSONObject jsonObj) {
        WFActionDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            Object entityTypeId = jsonObj.get("entityTypeId");
            Object entityId = jsonObj.get("entityId");
            String responseBody = jsonObj.getString("responseBody");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");

            dtoObject = new WFActionDTO(testCaseId, description, expectedStatusCode, entityTypeId, entityId, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting NotifAlertConditionCheck DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testWFActionAPI(WFActionDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();
        Object entityTypeId = dtoObject.getEntityTypeId();
        Object entityId = dtoObject.getEntityId();
        String expectedResponseBody = dtoObject.getResponseBody();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();
        String description = dtoObject.getDescription();
        String testCaseId = dtoObject.getTestCaseId();


        if (description.equalsIgnoreCase("Invalid Auth Code")) {

            // Hitting the API with Bad Auth Token
            APIResponse response = getWFActionAPI(getNewApiPath(entityTypeId.toString(), entityId.toString()), getHeadersWithBadAuth());
            int actualResponseCode = response.getResponseCode();
            csAssert.assertTrue(actualResponseCode == expectedResponseCode, "Error 400 did not appear for test case: Bad Auth");
        }
        else {
            //  Hitting the API
            APIResponse response = getWFActionAPI(getNewApiPath(entityTypeId.toString(), entityId.toString()), getHeaders());

            // Verification of the response
            int actualResponseCode = response.getResponseCode();
            String actualresponseBody = response.getResponseBody();

            // Actual verification
            csAssert.assertEquals(actualResponseCode, expectedResponseCode);
            csAssert.assertTrue(actualresponseBody.contains(expectedResponseBody), "Assert Failed for Negative scenarios");

        }
        csAssert.assertAll();
    }
}
