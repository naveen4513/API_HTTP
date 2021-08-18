package com.sirionlabs.test.api.BulkEditAttendanceAPI;

import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.governancebody.UpdateBulkCGBAttendance;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.dto.BulkEditAttendance.BulkEditAttendanceDTO;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestBulkEditAttendanceAPI {
    private final static Logger logger = LoggerFactory.getLogger(TestBulkEditAttendanceAPI.class);
    private String dataFilePath = "src/test/resources/TestConfig/APITestData/BulkEditAttendance";
    private String dataFileName = "BulkEditAttendanceAPIData.json";
    private String testingType;
    private String meetingTabID;
    private Integer cgbEntityTypeId;
    private int cgbEntityId;
    private Integer gbEntityTypeId;
    private String dateFormat;
    private int gbEntityId;

    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {
        this.testingType = testingType;
        gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        String listTabConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
        String listTabConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(listTabConfigFilePath, listTabConfigFileName, "tabs mapping", "meetings");
        String gbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        String adhocConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName");
        dateFormat = ParseConfigFile.getValueFromConfigFile(gbConfigFilePath, adhocConfigFileName, "dateformat");
        getCGBId();
    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();
        List<BulkEditAttendanceDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);
        JSONArray jsonArr = new JSONArray(allJsonData);
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    BulkEditAttendanceDTO dtoObject = getCheckEligibleForAdhocMeetingAPIFromJson(jsonObj);
                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }

        }
        for (BulkEditAttendanceDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }
        return allTestData.toArray(new Object[0][]);
    }

    private BulkEditAttendanceDTO getCheckEligibleForAdhocMeetingAPIFromJson(JSONObject jsonObj) {
        BulkEditAttendanceDTO dtoObject = null;
        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            String payload = jsonObj.getString("payload");
            String expectedStatusCode = jsonObj.getString("expectedStatusCode");
            String expectedResponseMessage = jsonObj.getString("expectedResponseMessage");
            dtoObject = new BulkEditAttendanceDTO(testCaseId, description, payload, expectedStatusCode, expectedResponseMessage);
        } catch (Exception e) {
            logger.error("Exception while Getting BulkEditAttendanceDTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }

    private int getCGBId() {
        int gbEntityId = createGB();
        logger.info("------------GB Id---------");
        logger.info("GB Id {}",gbEntityId);
        if (gbEntityId != 0)
            cgbEntityId = createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeId);
        logger.info("------------CGB Id---------");
        logger.info("CGB Id {}",cgbEntityId);
        return cgbEntityId;
    }

    private int createGB() {
        logger.info("***********************************creating GB*************************************");
        try {
            String section = "communicationtab";
            boolean isLocal = true;
            String gbResponse = GovernanceBody.createGB(section, isLocal);
            if (ParseJsonResponse.validJsonResponse(gbResponse)) {
                gbEntityId = CreateEntity.getNewEntityId(gbResponse);
                logger.info("Gb successfully created with ID ->" + gbEntityId);
                return gbEntityId;
            }
        } catch (Exception e) {
            logger.error("GB is not creating");
        }
        return 0;
    }

    private int createAdhocCGB(int gbEntityId, String dateFormat, int gbEntityTypeId) {
        logger.info("****creating CGB**********");
        try {
            AdhocMeeting meeting = new AdhocMeeting();
            String adhocMeetingResponse = meeting.hitAdhocMeetingApi(String.valueOf(gbEntityId), DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(), -3, dateFormat), "21:00", "Asia/Kolkata (GMT +05:30)", "30 Min", "delhi");

            if (adhocMeetingResponse.contains("Meeting Scheduled")) {
                logger.info("Adhoc meeting created");
                // getting meeting id
                TabListData listData = new TabListData();
                String gb_res = listData.hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeId, gbEntityId);
                List<String> meetingIds = ListDataHelper.getColumnIds(gb_res);
                logger.info("meeting created is :  " + meetingIds.get(0));
                return Integer.parseInt(meetingIds.get(0));
            } else {
                logger.error("Adhoc meeting not created");
            }
        } catch (Exception e) {
            logger.error("CGB is not creating");
        }
        return 0;
    }

    @Test(dataProvider = "dataProviderJson")
    public void testAPIFromJson(BulkEditAttendanceDTO dtoObject) {
        CustomAssert csAssert = new CustomAssert();
        String testCaseId = dtoObject.getTestCaseId();
        try {
            String description = dtoObject.getDescription();
            logger.info("Test Case Id {} description {}", testCaseId, description);
            String expectedResponseMessage = dtoObject.getExpectedResponseMessage();
            UpdateBulkCGBAttendance updateBulkCGBAttendance = new UpdateBulkCGBAttendance();
            APIResponse apiResponse = updateBulkCGBAttendance.getUpdateAttendanceAPIResponse(cgbEntityId, dtoObject.getPayload());
            int actualStatusCode = apiResponse.getResponseCode();
            int expectedStatusCode = Integer.parseInt(dtoObject.getExpectedStatusCode());
            csAssert.assertTrue(expectedStatusCode == actualStatusCode, "Actual And Expected Status Code Found Different");
            String responseMessage = apiResponse.getResponseBody();
            if (expectedResponseMessage.equalsIgnoreCase("Attendance Updated")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Attendance Updated"), "Attendance did not updated successfully");
            } else if (expectedResponseMessage.equalsIgnoreCase("Governance Body Child Participants cannot be blank.")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Governance Body Child Participants cannot be blank."), "Governance Body Child Participants blank");
            } else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'Meeting attended'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'Meeting attended'"), "Please Enter 'Meeting attended'");
            } else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'Presence Type'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'Presence Type'"), "Please Enter 'Presence Type'");
            } else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'Duration'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'Duration'"), "Please Enter 'Duration'");
            } else if (expectedResponseMessage.equalsIgnoreCase("Required request parameter data is either missing or wrong.")) {
                csAssert.assertTrue(new JSONObject(responseMessage).getString("error").equalsIgnoreCase("Required request parameter data is either missing or wrong."), "Required request parameter data is either missing or wrong.");
            }
            else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'valid Presence Type'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'valid Presence Type'"), "Please Enter 'valid Presence Type'");
            }
            else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'valid Duration'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'valid Duration'"), "Please Enter 'valid Duration'");
            }
            else if (expectedResponseMessage.equalsIgnoreCase("Please Enter 'Participant User Id or Email'")) {
                csAssert.assertTrue(responseMessage.equalsIgnoreCase("Please Enter 'Participant User Id or Email'"), "Please Enter 'Participant User Id or Email'");
            } else {
                csAssert.assertTrue(false, expectedResponseMessage);
            }
        } catch (Exception e) {
            logger.error("Exception while verifying CheckEligibleForAdhocMeetingAPI Response {}", e.getMessage());
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @AfterClass
    public void deleteTestData() {
        logger.info("governance body delete for entity type id {}and entity id {}", gbEntityTypeId, gbEntityId);
        String tabListData = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeId, gbEntityId);
        List<String> meetingIds = ListDataHelper.getColumnIds(tabListData);
        for (String meetingId : meetingIds) {
            ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(meetingId));
        }
        ShowHelper.deleteEntity("governance body", gbEntityTypeId, gbEntityId);
    }

}
