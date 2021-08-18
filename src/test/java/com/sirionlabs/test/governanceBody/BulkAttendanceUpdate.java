package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.governancebody.UpdateBulkCGBAttendance;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class BulkAttendanceUpdate {
    private final static Logger logger = LoggerFactory.getLogger(TestGBCommunicationTab.class);
    private String cgbConfigFilePath;
    private String cgbConfigFileName;
    private int gbEntityTypeId;
    private int cgbEntityTypeId;
    private String dateFormat;
    private String meetingTabID;
    private int meetingMinutesTabId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        cgbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("cgbConfigFilePath");
        cgbConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("cgbConfigFileName");
        gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        String listTabConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
        String listTabConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(listTabConfigFilePath, listTabConfigFileName, "tabs mapping", "meetings");
        meetingMinutesTabId = TabListDataHelper.getIdForTab("governance body meetingminutes");
        String gbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        String adhocConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName");
        dateFormat = ParseConfigFile.getValueFromConfigFile(gbConfigFilePath, adhocConfigFileName, "dateformat");
    }


    @Test
    public void testCaseBulkAttendanceUpadate() {
        CustomAssert customAssert = new CustomAssert();
        int gbEntityId = 0;
        int cgbEntityId = 0;
        try {
            logger.info("*******************************Create GB*****************************************");
            gbEntityId = createGB(customAssert);

            logger.info("*******************************Create CGB*****************************************");
            cgbEntityId = createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeId, customAssert);

            // mark attendance
            logger.info("*************************Mark Bulk Attendance************************************");

            DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
            String defaultUserListMetadataResponse = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse(meetingMinutesTabId, null);

            String duration = getRandomElement(Objects.requireNonNull(getDurationPresenceTypeAttended(defaultUserListMetadataResponse, "duration")));
            String presenceType = getRandomElement(Objects.requireNonNull(getDurationPresenceTypeAttended(defaultUserListMetadataResponse, "presencetype")));
            String attended = getRandomElement(Objects.requireNonNull(getDurationPresenceTypeAttended(defaultUserListMetadataResponse, "attended")));

            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            listRendererTabListData.hitListRendererTabListData(meetingMinutesTabId, cgbEntityTypeId, cgbEntityId, "{\"filterMap\":{\"entityTypeId\":87,\"offset\":0,\"size\":50,\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}");
            String listDataJsonStr = listRendererTabListData.getTabListDataJsonStr();
            JSONArray listDataJsonArray = new JSONObject(listDataJsonStr).getJSONArray("data");

            StringBuilder payloadBulkAttendance = new StringBuilder("{\"governanceBodyChildParticipants\":[");
            for (int i = 0; i < listDataJsonArray.length(); i++) {
                JSONArray jsonArray = listDataJsonArray.getJSONObject(i).names();
                HashMap<String, String> attendanceData = new HashMap<>();
                for (int j = 0; j < jsonArray.length(); j++) {
                    String columnName = listDataJsonArray.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("columnName");
                    if (columnName.equalsIgnoreCase("name")) {
                        String[] ar = listDataJsonArray.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("value").split(":;");
                        attendanceData.put("userId", ar[0].trim());
                        attendanceData.put("name", ar[1].trim());
                    } else if (columnName.equalsIgnoreCase("email")) {
                        attendanceData.put("email", listDataJsonArray.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("value").trim());
                    }
                }
                payloadBulkAttendance.append("{\"governanceBodyChildId\":").append(cgbEntityId).append(",\"userId\":").append(attendanceData.get("userId")).append(",\"mandatory\":false,\"meetingAttended\":\"true\",\"presenceType\":\"Telephonic\",\"email\":\"").append(attendanceData.get("email")).append("\",\"name\":\"").append(attendanceData.get("name")).append("\",\"external\":false,\"duration\":\"7 Hour\"}");
                if (i < listDataJsonArray.length() - 1) {
                    payloadBulkAttendance.append(",");
                } else {
                    if (attended.equalsIgnoreCase("true")) {
                        payloadBulkAttendance.append("],\"meetingAttended\":\"").append(attended).append("\",\"duration\":\"").append(duration).append("\",\"presenceType\":\"").append(presenceType).append("\"}");
                    } else {
                        payloadBulkAttendance.append("],\"meetingAttended\":\"").append(attended).append("\"}");
                    }
                }
            }

            logger.info("Bulk Attendance Payload {}",payloadBulkAttendance);

            APIResponse apiResponse = new UpdateBulkCGBAttendance().getUpdateAttendanceAPIResponse(cgbEntityId, payloadBulkAttendance.toString());
            if (apiResponse.getResponseCode() == HttpStatus.SC_OK && ParseJsonResponse.validJsonResponse(apiResponse.getResponseBody())) {
                if (!new JSONObject(apiResponse.getResponseBody()).getString("message").equalsIgnoreCase("Attendance Updated")) {
                    customAssert.assertTrue(false, "Attendance not Updated");
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
        finally
        {
            logger.info("******************GB and CGB Deleted***********************************");

            logger.info("governance body meeting delete for entity type id {}and entity id {}", cgbEntityTypeId, cgbEntityId);
            ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId,cgbEntityId);
            logger.info("governance body delete for entity type id {}and entity id {}", gbEntityTypeId, gbEntityId);
            String tabListData = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeId, gbEntityId);
            List<String> meetingIds = ListDataHelper.getColumnIds(tabListData);
            for (String meetingId : meetingIds) {
                ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(meetingId));
            }
            ShowHelper.deleteEntity("governance body", gbEntityTypeId, gbEntityId);
        }
        customAssert.assertAll();
    }


    private String getRandomElement(List<Object> list) {
        Random rand = new Random();
        return list.get(rand.nextInt(list.size())).toString();
    }

    private List<Object> getDurationPresenceTypeAttended(String defaultUserListMetadataResponse, String columnQueryName) {
        if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
            JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("columns");

            for (int i = 0; i < jsonArr.length(); i++) {
                String queryName = jsonArr.getJSONObject(i).getString("queryName");
                if (queryName.trim().equalsIgnoreCase(columnQueryName)) {
                    return new JSONObject(jsonArr.getJSONObject(i).getString("displayFormat")).getJSONArray("optionValues").toList();
                }

            }
        } else {
            logger.error("DefaultUserList Metadata Response is an Invalid JSON.");
        }
        return null;
    }

    private int createGB(CustomAssert customAssert) {
        logger.info("***********************************creating GB*************************************");
        try {
            String section = "communicationtab";
            boolean isLocal = true;
            String gbResponse = GovernanceBody.createGB(section, isLocal);
            if (ParseJsonResponse.validJsonResponse(gbResponse)) {
                int gbEntityId = CreateEntity.getNewEntityId(gbResponse);
                logger.info("Gb successfully created with ID ->" + gbEntityId);
                return gbEntityId;
            }
        } catch (Exception e) {
            logger.error("GB is not creating");
            customAssert.assertTrue(false, "Exception while GB is creating");
        }
        return 0;
    }

    private int createAdhocCGB(int gbEntityId, String dateFormat, int gbEntityTypeId, CustomAssert customAssert) {
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
                customAssert.assertTrue(false, "Adhoc meeting not created");
            }
        } catch (Exception e) {
            logger.error("CGB is not creating");
            customAssert.assertTrue(false, "CGB is not creating");
        }
        return 0;
    }

}
