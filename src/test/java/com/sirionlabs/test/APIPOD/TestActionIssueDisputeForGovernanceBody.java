package com.sirionlabs.test.APIPOD;


import com.sirionlabs.api.governancebody.ActionIssueDisputeGraph;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.DownloadGraphWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestActionIssueDisputeForGovernanceBody {
    private final static Logger logger = LoggerFactory.getLogger(TestActionIssueDisputeForGovernanceBody.class);
    private int gbEntityTypeID;
    private String dateFormat;
    private String issueConfigFilePath;
    private String issueConfigFileName;
    private String issueExtraFieldsConfigFileName;
    private int issueEntityTypeId;
    private String meetingTabID;
    private int cgbEntityTypeId;
    private String disputeConfigFilePath;
    private String disputeConfigFileName;
    private String disputeExtraFieldsConfigFileName;
    private int disputeEntityTypeId;
    private String actionConfigFilePath;
    private String actionConfigFileName;
    private String actionExtraFieldsConfigFileName;
    private int actionEntityTypeId;
    private int disputeUrlId;
    private int actionUrlId;
    private int issueUrlId;
    private String cgbEntityId;

    @BeforeClass
    public void beforeClass() {
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        dateFormat = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("GBFilePath"), ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName"), "dateformat");

        issueConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("IssueFilePath");
        issueConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueFileName");
        issueExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueExtraFieldsFileName");
        issueEntityTypeId = ConfigureConstantFields.getEntityIdByName("issues");

        disputeConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DisputeFilePath");
        disputeConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DisputeFileName");
        disputeExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DisputeExtraFieldsFileName");
        disputeEntityTypeId = ConfigureConstantFields.getEntityIdByName("disputes");

        actionConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ActionFilePath");
        actionConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionFileName");
        actionExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ActionExtraFieldsFileName");
        actionEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

        disputeUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile("CommonConfigFiles", "EntityIdMapping.cfg", "disputes", "entity_url_id"));
        actionUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile("CommonConfigFiles", "EntityIdMapping.cfg", "actions", "entity_url_id"));
        issueUrlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile("CommonConfigFiles", "EntityIdMapping.cfg", "issues", "entity_url_id"));

    }


    @Test
    public void testActionIssueDispute() {
        CustomAssert customAssert = new CustomAssert();
        int gbEntityId = 0;
        try {
            logger.info("************Create Gb****************");
            String governanceBodiesResponse = GovernanceBody.createGB("governance_bodies_aid", true);
            if (ParseJsonResponse.validJsonResponse(governanceBodiesResponse))
                gbEntityId = CreateEntity.getNewEntityId(governanceBodiesResponse);

            if (gbEntityId == -1) {
                customAssert.assertTrue(false, "GB IS not Creating");
                throw new SkipException("GB is not creating");
            }

            logger.info("Governance Body Created with Entity id: " + gbEntityId);
            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel);
            }


            logger.info("Create AdhocMeeting for Governance Body Entity ID {}", gbEntityId);
            String adhocMeetingAPIResponse = new AdhocMeeting().hitAdhocMeetingApi(String.valueOf(gbEntityId), DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(), -1, dateFormat), "21:00", "Asia/Kolkata (GMT +05:30)", "30 Min", "delhi");
            if (!adhocMeetingAPIResponse.contains("Meeting Scheduled"))
                customAssert.assertTrue(false, "adhoc meeting not created");
            else {
                logger.info(adhocMeetingAPIResponse);
                String cgbName = "";
                String gbMeetingTabListDataResponse = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
                if (!ParseJsonResponse.validJsonResponse(gbMeetingTabListDataResponse))
                    customAssert.assertTrue(false, "Invalid meeting tab list data response in governance body");

                JSONObject tabListDataJsonObject = new JSONObject(gbMeetingTabListDataResponse).getJSONArray("data").getJSONObject(0);
                JSONArray jsonObjectName = tabListDataJsonObject.names();
                for (int i = 0; i < jsonObjectName.length(); i++) {
                    if (tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("columnName").equalsIgnoreCase("id")) {
                        String[] value = tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("value").trim().split(":;");
                        cgbEntityId = value[1];
                    } else if (tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("columnName").equalsIgnoreCase("name"))
                        cgbName = tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("value").trim();
                }


                //check action, issue , graph before creation
                logger.info("check action,issue,dispute graph before creation of action,issue,dispute");

                customAssert.assertEquals(ActionIssueDisputeGraph.actionGraph(gbEntityId), "{}", "action graph api should be \"{}\"");
                customAssert.assertEquals(ActionIssueDisputeGraph.issueGraph(gbEntityId), "{}", "issue graph api should be \"{}\"");
                customAssert.assertEquals(ActionIssueDisputeGraph.disputeGraph(gbEntityId), "{}", "dispute graph api should be \"{}\"");


                //create dispute
                int disputeId = createActionIssueAndDispute(disputeConfigFilePath, disputeConfigFileName, "governance_bodies_dispute", disputeExtraFieldsConfigFileName, customAssert, cgbEntityId, "disputes");
                logger.info("Dispute Entity Created with entity id : " + disputeId);

                if (disputeId == 0) {
                    customAssert.assertTrue(false, "Dispute IS not Creating");
                    throw new SkipException("Dispute is not created");
                }

                logger.info("check action,issue,dispute graph after creation of only dispute");
                customAssert.assertEquals(ActionIssueDisputeGraph.actionGraph(gbEntityId), "{}", "action graph api should be \"{}\"");
                customAssert.assertEquals(ActionIssueDisputeGraph.issueGraph(gbEntityId), "{}", "issue graph api should be \"{}\"");
                String dispute_response = ActionIssueDisputeGraph.disputeGraph(gbEntityId);
                String dispute_graph = (String) JSONUtility.parseJson(dispute_response, "$.chartName");
                customAssert.assertEquals(dispute_graph, "Disputes", "dispute graph data is not visible");


                // create issue
                int issueId = createActionIssueAndDispute(issueConfigFilePath, issueConfigFileName, "governance_bodies_issue", issueExtraFieldsConfigFileName, customAssert, cgbEntityId, "issues");
                logger.info("Issue Entity Created with entity id : " + issueId);
                if (issueId == 0) {
                    customAssert.assertTrue(false, "Issue IS not Creating");
                    throw new SkipException("Issue is not created");
                }

                logger.info("check action,issue,dispute graph after creation of dispute,issue");
                customAssert.assertEquals(ActionIssueDisputeGraph.actionGraph(gbEntityId), "{}", "action graph api should be \"{}\"");
                String issue_response = ActionIssueDisputeGraph.issueGraph(gbEntityId);
                String issue_graph = (String) JSONUtility.parseJson(issue_response, "$.chartName");
                customAssert.assertEquals(issue_graph, "Issues", "issue graph data is not visible");
                customAssert.assertEquals(dispute_graph, "Disputes", "dispute graph data is not visible");

                //create action
                int actionId = createActionIssueAndDispute(actionConfigFilePath, actionConfigFileName, "governance_bodies_action", actionExtraFieldsConfigFileName, customAssert, cgbEntityId, "actions");
                logger.info("Action Entity Created with entity id : " + actionId);

                if (actionId == 0) {
                    customAssert.assertTrue(false, "Action IS not Creating");
                    throw new SkipException("action is not created");
                }

                logger.info("check action,issue,dispute graph after creation of dispute,issue and action");
                String action_response = ActionIssueDisputeGraph.actionGraph(gbEntityId);
                String action_graph = (String) JSONUtility.parseJson(action_response, "$.chartName");
                customAssert.assertEquals(action_graph, "Actions", "action graph data is not visible");
                customAssert.assertEquals(issue_graph, "Issues", "issue graph data is not visible");
                customAssert.assertEquals(dispute_graph, "Disputes", "dispute graph data is not visible");

                //checking for show page
                String issueShowApiResponse = CheckActionIssueDisputeShowPage(issueEntityTypeId, issueId, cgbName, customAssert);
                String disputeShowApiResponse = CheckActionIssueDisputeShowPage(disputeEntityTypeId, disputeId, cgbName, customAssert);
                String actionShowApiResponse = CheckActionIssueDisputeShowPage(actionEntityTypeId, actionId, cgbName, customAssert);

                // check meeting minute default user list mata data in cgb

                checkActionIssueDisputeMeetingMinutesDefaultUserListMetaData(362, customAssert);

                checkActionIssueDisputeMeetingMinutesDefaultUserListMetaData(363, customAssert);

                checkActionIssueDisputeMeetingMinutesDefaultUserListMetaData(487, customAssert);


                // check meeting minute list mata data in cgb
                checkActionIssueDisputeInMeetingMinuteTab(customAssert, 362);

                checkActionIssueDisputeInMeetingMinuteTab(customAssert, 363);

                checkActionIssueDisputeInMeetingMinuteTab(customAssert, 487);


                //checking dispute in meeting outcome table for CGB
                checkActionIssueDisputeInMeetingOutcomesTab(disputeShowApiResponse, customAssert, 489, cgbEntityTypeId, Integer.parseInt(cgbEntityId));

                //checking action in meeting outcome table for CGB
                checkActionIssueDisputeInMeetingOutcomesTab(actionShowApiResponse, customAssert, 215, cgbEntityTypeId, Integer.parseInt(cgbEntityId));

                //checking issue in meeting outcome table for CGB
                checkActionIssueDisputeInMeetingOutcomesTab(issueShowApiResponse, customAssert, 216, cgbEntityTypeId, Integer.parseInt(cgbEntityId));

                //checking dispute in meeting outcome table for GB
                checkActionIssueDisputeInMeetingOutcomesTab(disputeShowApiResponse, customAssert, 491, gbEntityTypeID, gbEntityId);

                //checking action in meeting outcome table for GB
                checkActionIssueDisputeInMeetingOutcomesTab(actionShowApiResponse, customAssert, 227, gbEntityTypeID, gbEntityId);

                //checking issue in meeting outcome table for GB
                checkActionIssueDisputeInMeetingOutcomesTab(issueShowApiResponse, customAssert, 228, gbEntityTypeID, gbEntityId);

                //checking issue in meeting minute table for CGB
                checkActionIssueDisputeInGBMeetingMinutes(actionShowApiResponse, 362, customAssert, cgbEntityId);

                // checking action in meeting minute table for CGB
                checkActionIssueDisputeInGBMeetingMinutes(issueShowApiResponse, 363, customAssert, cgbEntityId);

                // checking dispute in meeting minute table for CGB
                checkActionIssueDisputeInGBMeetingMinutes(disputeShowApiResponse, 491, customAssert, cgbEntityId);
                String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + issueEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":" +
                        "\"desc\",\"filterJson\":{\"12\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + gbEntityId + "\",\"name\":\"" + cgbName + "\"}]},\"filterId\":12," +
                        "\"filterName\":\"governanceBody\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\": 14486,\"columnQueryName\": \"" +
                        "bulkcheckbox\"},{ \"columnId\": 155, \"columnQueryName\": \"id\"}, {\"columnId\": 18705,\"columnQueryName\": \"governancebody_meeting\"}]}";

                //checking for Listing UI
                checkActionIssueDisputeOnListing(listDataPayload, customAssert, cgbName, issueId, issueUrlId);
                //Checking ListingDownload
                checkActionIssueDisputeOnListingDownload(listDataPayload, issueEntityTypeId, cgbName, customAssert, issueUrlId);
                //ReportDownload
                checkActionIssueDisputeOnReportDownload(listDataPayload, "Issues - Tracker", cgbName, customAssert);
                //listing download
                checkActionIssueDisputeOnReportListing(listDataPayload, issueId, cgbName, customAssert, "Issues - Tracker");
                //delete issue
                logger.info("delete issue for issue id {}", issueId);
                ShowHelper.deleteEntity("issues", issueEntityTypeId, issueId);

                listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + disputeEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":" +
                        "\"desc\",\"filterJson\":{\"12\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + gbEntityId + "\",\"name\":\"" + cgbName + "\"}]},\"filterId\":12," +
                        "\"filterName\":\"governanceBody\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\": 14486,\"columnQueryName\": \"" +
                        "bulkcheckbox\"},{ \"columnId\": 155, \"columnQueryName\": \"id\"}, {\"columnId\": 18705,\"columnQueryName\": \"governancebody_meeting\"}]}";

                //checking for Listing UI
                checkActionIssueDisputeOnListing(listDataPayload, customAssert, cgbName, disputeId, disputeUrlId);
                //Checking ListingDownload
                checkActionIssueDisputeOnListingDownload(listDataPayload, disputeEntityTypeId, cgbName, customAssert, disputeUrlId);
                //ReportDownload
                checkActionIssueDisputeOnReportDownload(listDataPayload, "Dispute - Tracker", cgbName, customAssert);
                //listing download
                checkActionIssueDisputeOnReportListing(listDataPayload, disputeId, cgbName, customAssert, "Dispute - Tracker");
                //delete dispute
                logger.info("delete dispute for dispute id {}", disputeId);
                ShowHelper.deleteEntity("disputes", disputeEntityTypeId, disputeId);

                listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + actionEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":" +
                        "\"desc\",\"filterJson\":{\"12\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + gbEntityId + "\",\"name\":\"" + cgbName + "\"}]},\"filterId\":12," +
                        "\"filterName\":\"governanceBody\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":1557,\"columnQueryName\":\"id\"},{\"columnId\":18707,\"columnQueryName\":\"governancebody_meeting\"}]}";

                //checking for Listing UI
                checkActionIssueDisputeOnListing(listDataPayload, customAssert, cgbName, actionId, actionUrlId);
                //Checking ListingDownload
                checkActionIssueDisputeOnListingDownload(listDataPayload, actionEntityTypeId, cgbName, customAssert, actionUrlId);
                //ReportDownload
                checkActionIssueDisputeOnReportDownload(listDataPayload, "Actions - Tracker", cgbName, customAssert);
                //listing download
                checkActionIssueDisputeOnReportListing(listDataPayload, actionId, cgbName, customAssert, "Actions - Tracker");
                //delete action
                logger.info("delete action for action id {}", actionId);
                ShowHelper.deleteEntity("actions", actionEntityTypeId, actionId);
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Action Issue And Dispute{}", e.getMessage());

        } finally {
            logger.info("governance body meeting delete for entity type id {}and entity id {}", cgbEntityTypeId, cgbEntityId);
            ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(cgbEntityId));
            logger.info("governance body delete for entity type id {}and entity id {}", gbEntityTypeID, gbEntityId);
            String tabListData = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
            List<String> meetingIds = ListDataHelper.getColumnIds(tabListData);
            for (String meetingId : meetingIds) {
                ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(meetingId));
            }
            ShowHelper.deleteEntity("governance body", gbEntityTypeID, gbEntityId);
//           String GBANDCGBConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestGBCGBEntityConfigFilePath");
//           String  GBANDCGBConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestGBCGBEntityConfigFileName");
//
//            UpdateFile.updateConfigFileProperty(GBANDCGBConfigFilePath,GBANDCGBConfigFileName,"gb","gbentityid",String.valueOf(gbEntityId));
//            UpdateFile.updateConfigFileProperty(GBANDCGBConfigFilePath,GBANDCGBConfigFileName,"cgb","cgbentityid",cgbEntityId);
        }
        customAssert.assertAll();

    }

    private void checkActionIssueDisputeInMeetingMinuteTab(CustomAssert customAssert, int listId) {
        ListRendererListData listRendererListData = new ListRendererListData();
        String payload = "{\"filterMap\":{\"filterJson\":{\"271\":{\"filterId\":271,\"filterName\":\"note\",\"multiselectValues\":{\"SELECTEDDATA\":[]}},\"272\":{\"filterId\":272,\"filterName\":\"meeting\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + cgbEntityId + "\"}]}}},\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"},\"offset\":0,\"size\":20}";
        listRendererListData.hitListRendererListData(listId, payload);
        String listDataJsonStr = listRendererListData.getListDataJsonStr();
        if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
            JSONObject jsonObj = new JSONObject(listDataJsonStr);
            JSONArray dataArr = jsonObj.getJSONArray("data");

            jsonObj = dataArr.getJSONObject(0);
            JSONArray jsonNamesArr = jsonObj.names();

            for (Object objectName : jsonNamesArr) {
                if (jsonObj.getJSONObject(objectName.toString().trim()).getString("columnName").trim().equalsIgnoreCase("owner")) {
                    customAssert.assertTrue(jsonObj.getJSONObject(objectName.toString().trim()).getString("value").equalsIgnoreCase("Anay User"), "Owner name Found Wrong");
                    break;
                }
            }
        }
    }

    private void checkActionIssueDisputeMeetingMinutesDefaultUserListMetaData(int listId, CustomAssert customAssert) {
        try {
            DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
            TabDefaultUserListMetaData tabDefaultUserListMetaData = new TabDefaultUserListMetaData();
            tabDefaultUserListMetaData.hitTabDefaultUserListMetadata(listId);
            String tabDefaultUserListMetaDataJsonStr = tabDefaultUserListMetaData.getTabDefaultUserListMetaDataJsonStr();
            if (ParseJsonResponse.validJsonResponse(tabDefaultUserListMetaDataJsonStr)) {
                List<String> allColumnNames = defaultUserListMetadataHelper.getAllColumnNames(tabDefaultUserListMetaDataJsonStr);
                if (!allColumnNames.contains("Owner")) {
                    customAssert.assertTrue(false, "Owner name not found in Column Section");
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
    }

    private String CheckActionIssueDisputeShowPage(int entityTypeId, int id, String cgbName, CustomAssert customAssert) {
        logger.info("Checking show page for entity type id {} and entity id {}", entityTypeId, id);
        String showApiResponse = ShowHelper.getShowResponse(entityTypeId, id);
        try {
            if (!ParseJsonResponse.validJsonResponse(showApiResponse))
                customAssert.assertTrue(false, "Invalid create issue response");
            else {
                String source_title = ShowHelper.getValueOfField(entityTypeId, "governancebody", showApiResponse);
                customAssert.assertTrue(cgbName.equalsIgnoreCase(source_title), "governance body meeting name different in show page response for entity type id {" + entityTypeId + "} and entity id" + id);
            }
        } catch (Exception e) {
            logger.error("Exception while verifying show page for entity type id {}and entity id {}{}", entityTypeId, id, e.getMessage());

        }
        return showApiResponse;
    }

    private int createActionIssueAndDispute(String configFilePath, String configFileName, String sectionName, String extraFieldsConfigFileName, CustomAssert customAssert, String cgbId, String entity) {
        logger.info("*****create*****" + entity);
        try {
            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, sectionName, "sourceid", cgbId);
            String entityResponse = null;
            switch (entity) {
                case "issues":
                    entityResponse = Issue.createIssue(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
                case "disputes":
                    entityResponse = Dispute.createDispute(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
                case "actions":
                    entityResponse = Action.createAction(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName, sectionName, true);
                    break;
            }

            if (!ParseJsonResponse.validJsonResponse(entityResponse))
                customAssert.assertTrue(false, "create " + entity + "response Invalid");
            return (int) JSONUtility.parseJson(entityResponse, "$.header.response.entityId");
        } catch (Exception e) {
            logger.error("Exception while verifying  Create{},{}", entity, e.getMessage());
        }
        return 0;
    }


    private void checkActionIssueDisputeOnListing(String listDataPayload, CustomAssert customAssert, String cgbName, int id, int listId) {
        logger.info("checking listing page for entity id {}", id);
        ListRendererListData listRendererListData = new ListRendererListData();
        try {
            listRendererListData.hitListRendererListDataV2(listId, listDataPayload);
            String listResponse = listRendererListData.getListDataJsonStr();

            if (!ParseJsonResponse.validJsonResponse(listResponse))
                customAssert.assertTrue(false, "Invalid listRendererListData response");
            JSONObject jsonObject = new JSONObject(listResponse).getJSONArray("data").getJSONObject(0);
            JSONArray jsonArray = jsonObject.names();
            for (int i = 0; i < jsonArray.length(); i++) {
                if (jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").equalsIgnoreCase("governancebody_meeting")) {
                    String[] name = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                    customAssert.assertTrue(cgbName.equalsIgnoreCase(name[0]), "governance body meeting name different on listing page for entity id" + id);
                } else if (jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").equalsIgnoreCase("id")) {
                    String[] ids = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                    customAssert.assertTrue(id == Integer.parseInt(ids[1]), "governance body meeting id different on listing page for entity id");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while verifying data on listing page{}", e.getMessage());
        }
    }

    private void checkActionIssueDisputeOnListingDownload(String listDataPayload, int entityTypeId, String cgbName, CustomAssert customAssert, int entityURLId) {

        logger.info("checking list download");

        String outputFilePath = "src/test/output/DefaultColumn.xlsx";
        try {
            DownloadListWithData downloadListwithData = new DownloadListWithData();
            Map<String, String> formParam = downloadListwithData.getDownloadListWithDataPayload(entityTypeId, null, listDataPayload);
            HttpResponse downloadResponse = downloadListwithData.hitDownloadListWithData(formParam, entityURLId);
            Boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, outputFilePath);

            if (!fileDownloaded) {
                throw new SkipException("Couldn't Download Data ");
            }
            XSSFSheet myExcelSheet = new XSSFWorkbook(new FileInputStream(outputFilePath)).getSheet("data");
            XSSFRow rowHeader = myExcelSheet.getRow(3);
            XSSFRow rowData = myExcelSheet.getRow(4);
            for (int i = 0; i < 2; i++) {
                if (rowHeader.getCell(i).getStringCellValue().equalsIgnoreCase("GOVERNANCE BODY MEETING"))
                    customAssert.assertTrue(rowData.getCell(i).getStringCellValue().equalsIgnoreCase(cgbName), "Governance body meeting name different in listing download");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying List Download{}", e.getMessage());

        } finally {
            FileUtils.deleteFile(outputFilePath);
        }
    }


    private void checkActionIssueDisputeOnReportDownload(String listDataPayload, String reportName, String cgbName, CustomAssert customAssert) {
        logger.info("report download {}", reportName);
        String reportOutputFilePath = "src/test/output/" + reportName + ".xlsx";
        try {
            Map<String, String> params = new HashMap<>();
            params.put("jsonData", listDataPayload);
            params.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            HttpResponse reportDownloadResponse = new DownloadGraphWithData().hitDownloadGraphWithData(params, getReportIdByReportName(reportName));
            Boolean trackerFileDownload = new FileUtils().writeResponseIntoFile(reportDownloadResponse, reportOutputFilePath);
            if (!trackerFileDownload) {
                throw new SkipException("Couldn't Download Data ");
            }
            ZipSecureFile.setMinInflateRatio(-1.0d);
            XSSFSheet myReportExcelSheet = new XSSFWorkbook(new FileInputStream(reportOutputFilePath)).getSheet("data");
            XSSFRow reportRowHeader = myReportExcelSheet.getRow(3);
            XSSFRow reportRowData = myReportExcelSheet.getRow(4);
            for (int i = 0; i < 2; i++) {
                if (reportRowHeader.getCell(i).getStringCellValue().equalsIgnoreCase("GOVERNANCE BODY MEETING")) {
                    customAssert.assertTrue(reportRowData.getCell(i).getStringCellValue().trim().equalsIgnoreCase(cgbName.trim()), "governance body meeting name different in downloaded report---" + reportRowData.getCell(i).getStringCellValue().trim() + "---" + cgbName + "----" + reportName);
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Exception while verifying Report Download  for report name{}{}", reportName, e.getMessage());
        } finally {
            FileUtils.deleteFile(reportOutputFilePath);
        }
    }

    private void checkActionIssueDisputeOnReportListing(String listDataPayload, int id, String cgbName, CustomAssert customAssert, String reportName) {
        logger.info("checking report listing for report name{}", reportName);
        try {
            ReportRendererListData reportRendererListDataObj = new ReportRendererListData();
            reportRendererListDataObj.hitReportRendererListData(getReportIdByReportName(reportName), listDataPayload);
            String reportRendererListDataObjListDataJsonStr = reportRendererListDataObj.getListDataJsonStr();
            JSONObject reportJsonObject = new JSONObject(reportRendererListDataObjListDataJsonStr).getJSONArray("data").getJSONObject(0);
            JSONArray reportObjects = reportJsonObject.names();
            for (int i = 0; i < reportObjects.length(); i++) {
                if (reportJsonObject.getJSONObject(reportObjects.getString(i)).getString("columnName").equalsIgnoreCase("id")) {
                    String[] ids = reportJsonObject.getJSONObject(reportObjects.getString(i)).getString("value").split(":;");
                    customAssert.assertTrue(Integer.parseInt(ids[1]) == id, "governance body meeting id different in report listing");
                } else if (reportJsonObject.getJSONObject(reportObjects.getString(i)).getString("columnName").equalsIgnoreCase("governancebody_meeting")) {
                    String[] name = reportJsonObject.getJSONObject(reportObjects.getString(i)).getString("value").split(":;");
                    customAssert.assertTrue(cgbName.equalsIgnoreCase(name[0]), "governance body meeting name different in report listing");
                }
            }
        } catch (Exception e) {
            logger.error("Exception while verifying report listing for report name{}{}", reportName, e.getMessage());
        }
    }

    private Integer getReportIdByReportName(String reportName) {
        ReportsListHelper reportHelper = new ReportsListHelper();
        Map<Integer, List<Map<String, String>>> allEntityWiseReportsMap = reportHelper.getAllEntityWiseReportsMap();
        for (Map.Entry<Integer, List<Map<String, String>>> entityWiseReportMap : allEntityWiseReportsMap.entrySet()) {
            List<Map<String, String>> allReportsListOfEntity = entityWiseReportMap.getValue();
            for (Map<String, String> reportMap : allReportsListOfEntity) {
                int reportNo = Integer.parseInt(reportMap.get("id"));
                String report = reportMap.get("name").trim();
                if (report.equals(reportName)) return reportNo;
            }
        }
        return null;
    }

    private void checkActionIssueDisputeInGBMeetingMinutes(String entityResponse, int listId, CustomAssert customAssert, String cgbEntityId) {
        try {
            String payload = "{\"filterMap\":{\"filterJson\":{\"271\":{\"filterId\":271,\"filterName\":\"note\",\"multiselectValues\":{\"SELECTEDDATA\":[]}},\"272\":{\"filterId\":272,\"filterName\":\"meeting\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + cgbEntityId + "\"}]}}},\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"},\"offset\":0,\"size\":20}";
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId, payload);
            String listRendererListDataJsonResponse = listRendererListData.getListDataJsonStr();
            Map<String, String> tabListDataAccordingToColumnName = new HashMap<>();
            if (ParseJsonResponse.validJsonResponse(listRendererListDataJsonResponse)) {
                JSONObject jsonObject = new JSONObject(listRendererListDataJsonResponse).getJSONArray("data").getJSONObject(0);
                JSONArray jsonArray = jsonObject.names();
                for (int i = 0; i < jsonArray.length(); i++) {
                    switch (jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toUpperCase()) {
                        case "ID":
                            String[] id = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                            tabListDataAccordingToColumnName.put("id", id[1]);
                            break;
                        case "NAME":
                        case "STATUS":
                            String[] name = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                            tabListDataAccordingToColumnName.put(jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toLowerCase(), name[0]);
                            break;
                        case "PLANNEDCOMPETITIONDATE":
                        case "PLANNEDCOMPLETIONDATE":
                            tabListDataAccordingToColumnName.put("plannedcompletiondate", jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").substring(0, 11));
                            break;

                    }
                }

            }
            JSONObject disputeShowPageResponse = new JSONObject(entityResponse.toLowerCase()).getJSONObject("body").getJSONObject("data");
            for (Map.Entry<String, String> tabList : tabListDataAccordingToColumnName.entrySet()) {
                Object object = disputeShowPageResponse.getJSONObject(tabList.getKey()).get("values");
                if (object instanceof JSONObject)
                    customAssert.assertTrue(((JSONObject) object).getString("name").equalsIgnoreCase(tabList.getValue()), "");
                else if (object instanceof String) {
                    if (tabList.getKey().equalsIgnoreCase("plannedcompletiondate"))
                        customAssert.assertTrue(new SimpleDateFormat("MMM-dd-yyyy").parse(tabList.getValue()).compareTo(new SimpleDateFormat("MM-dd-yyyy").parse(object.toString())) == 0, "planned completion date different in meeting outcomes");
                    else
                        customAssert.assertTrue(object.toString().equalsIgnoreCase(tabList.getValue()), "column data different in meeting minutes table");
                } else
                    customAssert.assertTrue(object.toString().equalsIgnoreCase(tabList.getValue()), "column data different in meeting minutes table");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying meeting minutes table{}", e.getMessage());
        }
    }

    private void checkActionIssueDisputeInMeetingOutcomesTab(String disputeResponse, CustomAssert customAssert, int tabId, int cgbEntityTypeID, int cgbEntityID) {
        logger.info("Hitting tab list data API for listId {},entityTypeId {},entityId {}", tabId, cgbEntityTypeID, cgbEntityID);
        try {
            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(tabId, cgbEntityTypeID, cgbEntityID);
            String tabListDataResponse = tabListData.getTabListDataResponseStr();
            Map<String, String> tabListDataAccordingToColumnName = new HashMap<>();
            if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                JSONObject jsonObject = new JSONObject(tabListDataResponse).getJSONArray("data").getJSONObject(0);
                JSONArray jsonArray = jsonObject.names();
                for (int i = 0; i < jsonArray.length(); i++) {
                    switch (jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toUpperCase()) {
                        case "ID":
                            String[] id = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                            tabListDataAccordingToColumnName.put("id", id[1]);
                            break;
                        case "NAME":
                        case "STATUS":
                        case "AGEING":
                        case "SUPPLIER":
                            String[] name = jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").split(":;");
                            tabListDataAccordingToColumnName.put(jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toLowerCase(), name[0]);
                            break;
                        case "PLANNEDCOMPLETIONDATE":
                            tabListDataAccordingToColumnName.put(jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toLowerCase(), jsonObject.getJSONObject(jsonArray.getString(i)).getString("value").substring(0, 11));
                            break;
                        case "CYCLETIME":
                        case "TYPE":
                            tabListDataAccordingToColumnName.put(jsonObject.getJSONObject(jsonArray.getString(i)).getString("columnName").toLowerCase(), "");
                            break;
                    }
                }
            }
            JSONObject disputeShowPageResponse = new JSONObject(disputeResponse.toLowerCase()).getJSONObject("body").getJSONObject("data");
            for (Map.Entry<String, String> tabList : tabListDataAccordingToColumnName.entrySet()) {
                Object object = disputeShowPageResponse.getJSONObject(tabList.getKey()).get("values");
                if (object instanceof JSONObject)
                    customAssert.assertTrue(((JSONObject) object).getString("name").equalsIgnoreCase(tabList.getValue()), "");
                else if (object instanceof String) {
                    if (tabList.getKey().equalsIgnoreCase("plannedcompletiondate"))
                        customAssert.assertTrue(new SimpleDateFormat("yyyy-MM-dd").parse(tabList.getValue()).compareTo(new SimpleDateFormat("MM-dd-yyyy").parse(object.toString())) == 0, "planned completion date different in meeting outcomes");
                    else
                        customAssert.assertTrue(object.toString().equalsIgnoreCase(tabList.getValue()), "column data different in meeting outcomes table");
                } else if (object instanceof Double) {
                    customAssert.assertTrue(Integer.parseInt(object.toString()) == Integer.parseInt(tabList.getValue()), "column data different in meeting outcomes table");
                } else
                    customAssert.assertTrue(object.toString().equalsIgnoreCase(tabList.getValue()), "column data different in meeting outcomes table");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying meeting outcomes table {}", e.getMessage());
        }

    }
}
