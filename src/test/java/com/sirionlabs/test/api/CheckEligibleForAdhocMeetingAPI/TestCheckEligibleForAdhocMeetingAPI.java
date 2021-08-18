package com.sirionlabs.test.api.CheckEligibleForAdhocMeetingAPI;
import com.sirionlabs.api.CheckEligibleForAdhocMeeting.CheckEligibleForAdhocMeeting;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.dto.CheckEligibleForAdhocMeeting.CheckEligibleForAdhocMeetingAPIDto;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestCheckEligibleForAdhocMeetingAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestCheckEligibleForAdhocMeetingAPI.class);
    private String testingType;
    private String dataFilePath = "src/test/resources/TestConfig/APITestData/CheckEligibleForAdhocMeeting";
    private String dataFileName = "CheckEligibleForAdhocMeetingAPIData.json";
    private Integer gbEntityTypeID;
    private String meetingTabID;
    private Integer cgbEntityTypeId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        List<CheckEligibleForAdhocMeetingAPIDto> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    CheckEligibleForAdhocMeetingAPIDto dtoObject = getCheckEligibleForAdhocMeetingAPIFromJson(jsonObj);
                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }

        }
        for (CheckEligibleForAdhocMeetingAPIDto dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }
        return allTestData.toArray(new Object[0][]);
    }
    private CheckEligibleForAdhocMeetingAPIDto getCheckEligibleForAdhocMeetingAPIFromJson(JSONObject jsonObj) {
        CheckEligibleForAdhocMeetingAPIDto dtoObject = null;
        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            String gbStatus = jsonObj.getString("gbStatus");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");
            dtoObject = new CheckEligibleForAdhocMeetingAPIDto(testCaseId, description, gbStatus, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting CheckEligibleForAdhocMeetingAPI DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(CheckEligibleForAdhocMeetingAPIDto dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        int gbEntityId = 0;
        try {
            String description = dtoObject.getDescription();
            logger.info("Starting TC Id: {}. {}", testCaseId, description);
            logger.info("************Create Gb****************");
            String governanceBodiesResponse = GovernanceBody.createGB("governance_bodies_aid", true);
            if (ParseJsonResponse.validJsonResponse(governanceBodiesResponse))
                gbEntityId = CreateEntity.getNewEntityId(governanceBodiesResponse);
            if (gbEntityId == -1) {
                csAssert.assertTrue(false, "GB IS not Creating");
                throw new SkipException("GB is not creating");
            }
            logger.info("Governance Body Created with Entity id: " + gbEntityId);
            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = dtoObject.getGbStatus().split(",");
            if (!workFlowStep[0].equalsIgnoreCase("Delete Case")) {
                for (String actionLabel : workFlowStep) {
                    logger.info(actionLabel);
                    entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel.trim());
                }
                verifyCheckEligibleForAdhocMeetingAPI(gbEntityId, dtoObject, csAssert);
            } else {
                int gbId = gbEntityId + 1;
                verifyCheckEligibleForAdhocMeetingAPI(gbId, dtoObject, csAssert);
            }
        } catch (Exception e) {
            logger.error("Exception while verifying CheckEligibleForAdhocMeetingAPI Response {}", e.getMessage());
            csAssert.assertTrue(false, e.getMessage());
        } finally {
            logger.info("governance body delete for entity type id {}and entity id {}", gbEntityTypeID, gbEntityId);
            String tabListData = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
            List<String> meetingIds = ListDataHelper.getColumnIds(tabListData);
            for (String meetingId : meetingIds) {
                ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(meetingId));
            }
            ShowHelper.deleteEntity("governance body", gbEntityTypeID, gbEntityId);
        }
        csAssert.assertAll();
    }

    public void verifyCheckEligibleForAdhocMeetingAPI(int gbEntityId, CheckEligibleForAdhocMeetingAPIDto dtoObject, CustomAssert csAssert) {
        try {
            APIResponse apiResponse = CheckEligibleForAdhocMeeting.getResponse(gbEntityId);
            int actualStatusCode = apiResponse.getResponseCode();
            String actualResponseBody = apiResponse.getResponseBody();
//            if (!ParseJsonResponse.validJsonResponse(actualResponseBody))
//                 csAssert.assertTrue(false, "Invalid Json Response");
            String expectedStatusCode = dtoObject.getExpectedStatusCode();
            String expectedResponseMessage = dtoObject.getExpectedResponseMessage();
            csAssert.assertTrue(actualStatusCode == Integer.parseInt(expectedStatusCode), "Actual Status code" + actualStatusCode + "And Expected Status Code" + expectedStatusCode + "Are Different");
            csAssert.assertTrue(actualResponseBody.equalsIgnoreCase(expectedResponseMessage), "Actual Response Body" + actualResponseBody + "And Expected Response Body" + expectedResponseMessage + "Are Different");
            csAssert.assertAll();
        } catch (Exception e) {
            logger.error("Exception while verifying CheckEligibleForAdhocMeetingAPI Response {}", e.getMessage());
             csAssert.assertTrue(false, e.getMessage());
        }
    }

}
