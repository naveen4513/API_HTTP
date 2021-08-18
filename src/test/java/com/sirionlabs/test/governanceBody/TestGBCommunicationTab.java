package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.*;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.meetingnotedelete.MeetingNoteDelete;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGBCommunicationTab {

    private final static Logger logger = LoggerFactory.getLogger(TestGBCommunicationTab.class);
    private String cgbConfigFilePath;
    private String cgbConfigFileName;
    private int gbEntityTypeId;
    private int cgbEntityTypeId;
    private String noOfFile;
    private ArrayList<String> fileNameList = new ArrayList<>();
    private String dateFormat;
    private String meetingTabID;
    private int meetingMinutesTabId;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        cgbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("cgbConfigFilePath");
        cgbConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("cgbConfigFileName");
        gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        noOfFile = ParseConfigFile.getValueFromConfigFile(cgbConfigFilePath, cgbConfigFileName, "document file upload", "nooffiletoupload");
        String listTabConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
        String listTabConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(listTabConfigFilePath, listTabConfigFileName, "tabs mapping", "meetings");
        meetingMinutesTabId = TabListDataHelper.getIdForTab("governance body meetingminutes");
        String gbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        String adhocConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName");
        dateFormat = ParseConfigFile.getValueFromConfigFile(gbConfigFilePath, adhocConfigFileName, "dateformat");
    }

    @Test
    public void testCaseAttendanceMark() {
        CustomAssert customAssert = new CustomAssert();
        String publishReportTitle = null;
        int clientEntitySeqId = 0;
        int gbEntityId = 0;
        int cgbEntityId = 0;
        try {
            logger.info("create Gb");
            //String GBANDCGBConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestGBCGBEntityConfigFilePath");
            //String  GBANDCGBConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestGBCGBEntityConfigFileName");
            gbEntityId = createGB(customAssert);//Integer.parseInt(ParseConfigFile.getValueFromConfigFile(GBANDCGBConfigFilePath,GBANDCGBConfigFileName,"gb","gbentityid").trim());//
            cgbEntityId =createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeId, customAssert);//Integer.parseInt(ParseConfigFile.getValueFromConfigFile(GBANDCGBConfigFilePath,GBANDCGBConfigFileName,"cgb","cgbentityid").trim());//

           // upload file and and delete file
            uploadDocumentFileInCreateNote(cgbEntityId,cgbEntityTypeId, customAssert);
           // get all document id

            logger.info("Hitting get document list API");
            APIResponse apiResponse=new MeetingNoteGetDocumentListAPI().getResponse(cgbEntityTypeId,cgbEntityId);
            String responseBody=apiResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(responseBody))
            {
                JSONArray jsonArray=new JSONObject(responseBody).getJSONArray("documentLinkData");
                for (int i=0; i<jsonArray.length(); i++)
                {
                      String[] documentDta =jsonArray.get(i).toString().split(":;");
                      isDeleteDocument(String.valueOf(cgbEntityTypeId),String.valueOf(cgbEntityId),documentDta[0],customAssert);
                }
            }
            //create meeting note
            logger.info("*****************Hitting create note API*******************");
            MeetingNoteCreateAPI meetingNoteCreateAPI = new MeetingNoteCreateAPI();
            String createMeetingNoteAPIPayload = "{\"noteText\":\"attendance\"}";
            APIResponse meetingNoteCreateAPIResponse = meetingNoteCreateAPI.getMeetingNoteCreateAPIResponse(cgbEntityTypeId, cgbEntityId, createMeetingNoteAPIPayload);
            String meetingNoteCreateAPIResponseBody = meetingNoteCreateAPIResponse.getResponseBody();
            if (ParseJsonResponse.validJsonResponse(meetingNoteCreateAPIResponseBody)) {
                boolean isMeetingNoteCreated = new JSONObject(meetingNoteCreateAPIResponseBody).getBoolean("status");
                if (!isMeetingNoteCreated) {
                    customAssert.assertTrue(false, "meetingNote could not be created");
                }
            }


            // mark attendance
            logger.info("*******************mark attendance************************************");
            JSONObject jsonObject = new JSONObject();
            JSONArray saveAttendancePayload = new JSONArray();

            DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
            String defaultUserListMetadataResponse = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse(meetingMinutesTabId, null);

            Map<Integer, Map<String, String>> stakeholderData = getStakeholderData(cgbEntityId);

            //check all stake holder present in attendance tab
            checkStakeholderInMeetingTab(stakeholderData, cgbEntityId, customAssert);

            for (Map.Entry<Integer, Map<String, String>> stakeholderMetaData : stakeholderData.entrySet()) {
                jsonObject.put("governanceBodyChildId", cgbEntityId);
                jsonObject.put("userId", stakeholderMetaData.getKey());
                jsonObject.put("mandatory", false);
                jsonObject.put("meetingAttended", getAttendanceData("attended", defaultUserListMetadataResponse));
                jsonObject.put("presenceType", getAttendanceData("presenceType", defaultUserListMetadataResponse));
                jsonObject.put("duration", getAttendanceData("duration", defaultUserListMetadataResponse));
                jsonObject.put("email", stakeholderMetaData.getValue().get("email"));
                jsonObject.put("name", stakeholderMetaData.getValue().get("name"));
                jsonObject.put("external", false);
                saveAttendancePayload.put(jsonObject);
            }

            logger.info("********************Hitting save attendance API for mark attendance*******************");
            SaveAttendance saveAttendance = new SaveAttendance();
            APIResponse saveAttendanceAPIResponse = saveAttendance.getSaveAttendance(cgbEntityId, saveAttendancePayload.toString());
            if (saveAttendanceAPIResponse.getResponseBody().equalsIgnoreCase("Attendance Updated")) {
                logger.info("Attendance Updated");

                // now publish mark attendance
                logger.info("**********************Hitting attendance publish API**********************************");
                PublishMOM publishMOM = new PublishMOM();
                String publishAttendanceAPIPayload = "{\"notify\":false,\"meetingMinutesHtml\":\"<html></html>\"}";
                APIResponse publishAttendanceAPIResponse = publishMOM.getPublishMOM(cgbEntityId, publishAttendanceAPIPayload);
                String publishAttendanceAPIResponseBody = publishAttendanceAPIResponse.getResponseBody();
                if (ParseJsonResponse.validJsonResponse(publishAttendanceAPIResponseBody)) {
                    boolean isPublishedAttendance = new JSONObject(publishAttendanceAPIResponseBody).getBoolean("published");
                    publishReportTitle = new JSONObject(publishAttendanceAPIResponseBody).getString("title");
                    clientEntitySeqId = new JSONObject(publishAttendanceAPIResponseBody).getInt("cliententityseqid");
                    if (!isPublishedAttendance) {
                        customAssert.assertTrue(false, "Attendance report could not be published");
                    }
                }
            } else {
                customAssert.assertTrue(false, "Attendance could  not be updated");
            }



            //check  attendance report in communication tab
            logger.info("*********************check attendance report create in communication tab******************************** ");
            int communicationTabId = TabListDataHelper.getIdForTab("governance body communication");
            if (communicationTabId != -1) {
                logger.info("Hitting TabListData API for Governance Body Communication Tab.");
                TabListData tabListObj = new TabListData();
                String tabListDataResponse = tabListObj.hitTabListData(communicationTabId, cgbEntityTypeId, cgbEntityId);
                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    int time = 60000;
                    int timeTemp = 0;
                    while(timeTemp<time){
                        net.minidev.json.JSONArray data =  (net.minidev.json.JSONArray)
                                JSONUtility.parseJson(tabListDataResponse,"$.data");
                        if(data.size()!=0){
                            break;
                        }
                        timeTemp = timeTemp+5000;
                        System.out.println( "waiting for millisecond"+ timeTemp);
                        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
                        String password = ConfigureEnvironment.getEnvironmentProperty("password");
                        new Check().hitCheck(userName,password);
                        tabListDataResponse = tabListObj.hitTabListData(communicationTabId, cgbEntityTypeId, cgbEntityId);
                    }
                    verifyAttendancePublishedReportAndUploadFileInCommunicationTab(tabListDataResponse, publishReportTitle, clientEntitySeqId, customAssert);
                }
            }
            //upload file and check in communication tab
            uploadDocumentFileInCreateNote(cgbEntityId, cgbEntityTypeId, customAssert);
            logger.info("*************************check upload note report in communication tab ************************************");
            if (communicationTabId != -1) {
                logger.info("Hitting TabListData API for Governance Body Communication Tab.");
                TabListData tabListObj = new TabListData();
                String tabListDataResponse = tabListObj.hitTabListData(communicationTabId, cgbEntityTypeId, cgbEntityId);
                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    int time = 60000;
                    int timeTemp = 0;
                    while(timeTemp<time){
                        net.minidev.json.JSONArray data =  (net.minidev.json.JSONArray)
                                JSONUtility.parseJson(tabListDataResponse,"$.data");
                        if(data.size()==2){
                            break ;
                        }
                        timeTemp = timeTemp+5000;
                        System.out.println( "waiting for millisecond"+ timeTemp);
                        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
                        String password = ConfigureEnvironment.getEnvironmentProperty("password");
                        new Check().hitCheck(userName,password);
                        tabListDataResponse = tabListObj.hitTabListData(communicationTabId, cgbEntityTypeId, cgbEntityId);
                    }
                   verifyAttendancePublishedReportAndUploadFileInCommunicationTab(tabListDataResponse, publishReportTitle, clientEntitySeqId, customAssert);
                }
            }
        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while Mark Attendance" + e.getMessage());
        } finally {
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

    private void verifyAttendancePublishedReportAndUploadFileInCommunicationTab(String tabListDataResponse, String publishTitleReport, int clientEntitySeqId, CustomAssert customAssert) {
        try {
            AccountInfo accountInfo = new AccountInfo();
            Date date = new Date(System.currentTimeMillis());
            JSONArray jsonObjectName = new JSONObject(tabListDataResponse).getJSONArray("data").getJSONObject(0).names();
            for (int i = 0; i < Integer.valueOf(noOfFile); i++) {
                JSONObject jsonObject = new JSONObject(tabListDataResponse).getJSONArray("data").getJSONObject(0).getJSONObject(jsonObjectName.getString(i));
                switch (jsonObject.getString("columnName")) {
                    case "comment":
                        if (!jsonObject.get("value").toString().equalsIgnoreCase("null")) {
                            customAssert.assertTrue(false, "comment should be null");
                        }
                        break;
                    case "completed_by":
                        if (!jsonObject.getString("value").equalsIgnoreCase(accountInfo.getUserName())) {
                            customAssert.assertTrue(false, "name should not be found");
                        }
                        break;
                    case "user_date":
                        DateFormat utcFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
                        utcFormat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
                     logger.info(jsonObject.getString("value")+" "+utcFormat.format(date));

                        if (!jsonObject.getString("value").substring(0, 10).equalsIgnoreCase(utcFormat.format(date).substring(0, 10))) {
                            customAssert.assertTrue(false, "user date not match");
                        }
                        break;
                    case "status_name":
                        if (!(jsonObject.getString("value").equalsIgnoreCase("Published") || jsonObject.getString("value").equalsIgnoreCase("Documents Uploaded"))) {
                            customAssert.assertTrue(false, jsonObject.getString("value") + "is not found");
                        }
                        break;
                    case "document":
                        List<String> documents = new ArrayList<>();
                        if (jsonObject.getString("value").contains("###")) {
                            String[] arr = jsonObject.getString("value").split("###");
                            for (int j = 0; j < arr.length; j++) {
                                String[] documentArray = arr[j].split(":;");
                                documents.add(documentArray[1] + "." + documentArray[2]);
                            }
                            if (documents.size() == Integer.parseInt(noOfFile)) {
                                for (int j = 0; j < fileNameList.size(); j++) {
                                    if (!documents.contains(fileNameList.get(j))) {
                                        customAssert.assertTrue(false, "Uploaded Document " + fileNameList.get(i) + " is not present in Communication Tab Response.");
                                    }
                                }
                            } else {
                                customAssert.assertTrue(false, "no of file uploaded is incorrect");
                            }

                        } else {
                            String[] arr = jsonObject.getString("value").split(":;");
                            SimpleDateFormat simpleDateFormatUpload=new SimpleDateFormat("ddMMMyyyy");
                            simpleDateFormatUpload.setTimeZone(TimeZone.getTimeZone("GMT"));
                            documents.add(arr[1]);
                            String documentName;
                            if (String.valueOf(clientEntitySeqId).length() > 4) {
                                documentName = "CGB" + clientEntitySeqId + "-" + simpleDateFormatUpload.format(date) + "-" + publishTitleReport;
                            } else {
                                documentName = "CGB0" + clientEntitySeqId + "-" +simpleDateFormatUpload.format(date)+ "-" + publishTitleReport;
                            }
                            if (!documents.contains(documentName)) {
                                customAssert.assertTrue(false, "publish report not found");
                            }
                        }
                        break;
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating publish report");
        }
    }

    private void uploadDocumentFileInCreateNote(int cgbEntityId, int cgbEntityTypeId, CustomAssert customAssert) {

        ArrayList<String> randomList = new ArrayList<>();
        try {
            logger.info("*********************** upload document file in create note tab ***********************");

            String filePath = ParseConfigFile.getValueFromConfigFile(cgbConfigFilePath, cgbConfigFileName, "document file upload", "uploadFilePath");
            for (int i = 1; i <= Integer.parseInt(noOfFile); i++) {

                String fileName = ParseConfigFile.getValueFromConfigFile(cgbConfigFilePath, cgbConfigFileName, "document file upload", "uploadFileName" + i);
                String randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
                String uploadResponse = DocumentHelper.uploadDocumentFile(filePath, fileName, randomKeyForDocumentFile);

                if (!(uploadResponse != null && uploadResponse.trim().startsWith("/data/") && uploadResponse.trim().contains(fileName))) {
                    throw new SkipException("Couldn't upload Document File located at [" + filePath + "/" + fileName + "]. Hence skipping test.");
                }
                randomList.add(randomKeyForDocumentFile);
                fileNameList.add(fileName);
            }

            String createNoteUploadFilePayload = getCreateNoteUploadFilePayload(randomList);
            if (ParseJsonResponse.validJsonResponse(createNoteUploadFilePayload)) {
                MeetingNoteFileUpload meetingNoteFileUpload = new MeetingNoteFileUpload();
                APIResponse meetingNoteFileUploadAPIResponse = meetingNoteFileUpload.getMeetingNoteFileUploadAPIResponse(cgbEntityTypeId, cgbEntityId, createNoteUploadFilePayload);
                String meetingNoteFileUploadResponseBody = meetingNoteFileUploadAPIResponse.getResponseBody();
                if (ParseJsonResponse.validJsonResponse(meetingNoteFileUploadResponseBody)) {
                    boolean statusOfFile = new JSONObject(meetingNoteFileUploadResponseBody).getBoolean("status");
                    if (!statusOfFile) {
                        customAssert.assertTrue(false, "File is not uploading");
                    }
                }
            }

        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception while file is uploading");
        }
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

    private String getCreateNoteUploadFilePayload(List<String> keyList) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < keyList.size(); i++) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key", keyList.get(i));
            jsonArray.put(i, jsonObject);
        }
        return jsonArray.toString();
    }

    private String getAttendanceData(String data, String defaultUserListMetadataResponse) {
        logger.info("Get {} value", data);
        try {
            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {

                JSONArray jsonArray = new JSONObject(defaultUserListMetadataResponse).getJSONArray("columns");
                for (int i = 0; i < jsonArray.length(); i++) {
                    if (jsonArray.getJSONObject(i).get("queryName").toString().equalsIgnoreCase(data)) {
                        jsonArray = new JSONObject(jsonArray.getJSONObject(i).getString("displayFormat")).getJSONArray("optionValues");
                        return jsonArray.getString((int) (Math.random() * jsonArray.length()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting attendance data {}", data);
        }
        return null;
    }

    private Map<Integer, Map<String, String>> getStakeholderData(int cgbEntityId) {
        logger.info("Get Stakeholder name, userID and email");

        try {
            Map<Integer, Map<String, String>> stakeholderData = new HashMap<>();

            logger.info("Hitting listRendererTabListData API");

            ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
            String listRendererDataPayload = "{\"filterMap\":{\"entityTypeId\":" + cgbEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null,\"filterJson\":{}}}";

            listRendererTabListData.hitListRendererTabListData(meetingMinutesTabId, cgbEntityTypeId, cgbEntityId, listRendererDataPayload);

            String tabListDataJsonStr = listRendererTabListData.getTabListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(tabListDataJsonStr)) {

                JSONArray jsonArray = new JSONObject(tabListDataJsonStr).getJSONArray("data");

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONArray jsonObjectName = jsonArray.getJSONObject(i).names();
                    Map<String, String> nameAndEmail = new HashMap<>();
                    int userId = 0;

                    for (int j = 0; j < jsonObjectName.length(); j++) {

                        if (jsonArray.getJSONObject(i).getJSONObject(jsonObjectName.getString(j)).getString("columnName").equalsIgnoreCase("name")) {
                            String[] userIdAndName = jsonArray.getJSONObject(i).getJSONObject(jsonObjectName.getString(j)).getString("value").split(":;");
                            userId = Integer.parseInt(userIdAndName[0].trim());
                            nameAndEmail.put("name", userIdAndName[1].trim());
                        } else if (jsonArray.getJSONObject(i).getJSONObject(jsonObjectName.getString(j)).getString("columnName").equalsIgnoreCase("email")) {
                            nameAndEmail.put("email", jsonArray.getJSONObject(i).getJSONObject(jsonObjectName.getString(j)).getString("value").trim());
                        }
                    }

                    stakeholderData.put(userId, nameAndEmail);
                }
                return stakeholderData;
            }
        } catch (Exception e) {
            logger.error("Exception while getting userName,email and userId");
        }
        return null;
    }

    private void checkStakeholderInMeetingTab(Map<Integer, Map<String, String>> stakeholderData, int cgbEntityId, CustomAssert customAssert) {
        Show show = new Show();
        logger.info("Hitting show page API for entityId{} and entityTypeId{}", cgbEntityId, cgbEntityTypeId);
        show.hitShow(cgbEntityTypeId, cgbEntityId);

        String responseOfShowPage = show.getShowJsonStr();
        if (ParseJsonResponse.validJsonResponse(responseOfShowPage)) {
            JSONObject jsonObject = new JSONObject(responseOfShowPage);
            jsonObject = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
            JSONArray stakeholderArray = jsonObject.getJSONObject(jsonObject.names().getString(0)).getJSONArray("values");
            for (int i = 0; i < stakeholderArray.length(); i++) {
                if (stakeholderData.containsKey(stakeholderArray.getJSONObject(i).getInt("id"))) {
                    logger.info("stakeholder present in meeting tab {} ", stakeholderArray.getJSONObject(i).getInt("id"));
                } else {
                    customAssert.assertTrue(false, "stakeholder not found in meeting tab");
                }
            }
        }
    }
    private void isDeleteDocument(String cgbEntityTypeId,String cgbEntityId,String documentId,CustomAssert customAssert)
    {
             try {
                 logger.info("Hitting Document Delete API");
                 APIResponse apiResponse = MeetingNoteDelete.getResponse(cgbEntityTypeId, cgbEntityId, documentId);
                 int responseCode = apiResponse.getResponseCode();
                 customAssert.assertTrue(responseCode == 200, "actual response code" + responseCode + "and expected response code 200 are different");
                 String responseBody = apiResponse.getResponseBody();
                 if (ParseJsonResponse.validJsonResponse(responseBody)) {
                     customAssert.assertTrue(responseBody.contains("Deleted Successfully"),"Document not deleted");
                 }
             }
             catch (Exception e)
             {
                 customAssert.assertTrue(false,"Exception while verifying document deleted successfully in notes section");
             }
    }
}
