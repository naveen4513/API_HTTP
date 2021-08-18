package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGBMISC {
    private final static Logger logger = LoggerFactory.getLogger(TestGBMISC.class);
    private String entityIdConfigFilePath;
    private int gbEntityId;
    private int gbEntityTypeID;
    private String meetingTabID;
    private int cgbEntityTypeId;
    private Show show;
    private String dateFormat;
    private ListRendererListData listObj;
    private int cgbEntityId;
    private  AdhocMeeting meeting = new AdhocMeeting();
    PostgreSQLJDBC db = new PostgreSQLJDBC();
    @BeforeClass
    public void beforeClass(){
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");
        String adhocConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName");
        String gbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        dateFormat = ParseConfigFile.getValueFromConfigFile(gbConfigFilePath, adhocConfigFileName, "dateformat");
        show = new Show();
        listObj = new ListRendererListData();
    }


    @DataProvider()
    public Object[][] dataProviderForGBMISC() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"governance_bodies_aid","gb_multisupplier"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


   @Test(dataProvider = "dataProviderForGBMISC")
    public void TestGBTabs(String flow) throws InterruptedException {

       CustomAssert csassert = new CustomAssert();

        gbEntityId = createGB(csassert,flow);

        if (gbEntityId==-1)
        {
            csassert.assertTrue(false,"GB IS not Creating");
        }else {
            logger.info("Governance Body Created with Entity id: " + gbEntityId);

            logger.info("validate the Gb page tabs before publish");
            //validate the Gb page tabs before publish
            show.hitShowGetAPI(gbEntityTypeID,gbEntityId);
            String gbShow = show.getShowJsonStr();
            JSONArray before_tabs= (JSONArray) JSONUtility.parseJson(gbShow,"$.body.layoutInfo.layoutComponent.fields[*].label");
            if(before_tabs.size()==3 && before_tabs.toString().toUpperCase().contains("GENERAL")
                    && before_tabs.toString().toUpperCase().contains("COMMUNICATION")
                    &&before_tabs.toString().toUpperCase().contains("AUDIT LOG") ){
            }else{
                csassert.assertTrue(false,"GENERAL,COMMUNICATION,AUDIT LOG are not visible in GB: "+ gbEntityId );
            }

            //validate adhoc meeting at my profile grovernance schedule tab list
            cgbEntityId =createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeID, csassert);
            String listRendererJsonStr =  listObj.listDataResponseV2OrderDirection(87,"adhoc meeting",20,"desc nulls last",null);
            JSONArray idArray = (JSONArray) JSONUtility.parseJson(listRendererJsonStr, "$.data[*].[*][?(@.columnName=='id')].value");
            int i = 0;
            for (i = 0; i <idArray.size() ; i++) {
                String[]  id =  idArray.get(0).toString().split(":;");
                if(id[1].equals(String.valueOf(cgbEntityId))){
                    break;
                }
            }
            csassert.assertFalse(i==(idArray.size()-1),"created adhoc "+ cgbEntityId + " is not present in adhoc listing in myprofile page");


            //performing workflow action on Gb

            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel);
            }

            logger.info("validate the Gb page tabs after publish");
            //validate the Gb page tabs After publish
            show.hitShowGetAPI(gbEntityTypeID,gbEntityId);
            String gbafterShow = show.getShowJsonStr();
            JSONArray after_tabs= (JSONArray) JSONUtility.parseJson(gbafterShow,"$.body.layoutInfo.layoutComponent.fields[*].label");

            if(after_tabs.size()==5 && after_tabs.toString().toUpperCase().contains("GENERAL") && after_tabs.toString().toUpperCase().contains("COMMUNICATION")
                    &&after_tabs.toString().toUpperCase().contains("AUDIT LOG")
                                && after_tabs.toString().toUpperCase().contains("MEETINGS") &&
                    after_tabs.toString().toUpperCase().contains("MEETING OUTCOMES")){
            }else{
                csassert.assertTrue(false,"GENERAL,COMMUNICATION,AUDIT LOG,MEETINGS,MEETING OUTCOMES\n are not visible in GB: "+ gbEntityId );
            }
        }

        List<String> cgbEntityIds = validateMeetingCreation(gbEntityId);
        for (int i = 0; i <cgbEntityIds.size() ; i++) {
            EntityOperationsHelper.deleteEntityRecord("governance body meetings", Integer.valueOf(cgbEntityIds.get(i)));

        }

        EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);

        csassert.assertAll();




    }


    @Test(description = "C151300")
    public void TestAdhocDefaultValue() throws SQLException {
        CustomAssert csAssert = new CustomAssert();

        //hit ahdoc scheduletab default value api
        String defaultResponse = meeting.hitDefaultValue();

        // get Start time and timezone from api respionse
       String timeZone = (String)JSONUtility.parseJson(defaultResponse,"$.userTimeZone.timeZone");
       String startTime = (String)JSONUtility.parseJson(defaultResponse,"$.userStartTime.name");
        JSONArray governanceBodyOptions = (JSONArray)JSONUtility.parseJson(defaultResponse,"$.governanceBodyOptions");

        int size = -1;
        if(governanceBodyOptions.size()>3) size=3;
        else size = governanceBodyOptions.size();

        for (int i =0; i<size;i++){
          String gbStartTime =   (String)((LinkedHashMap)((LinkedHashMap)
                                    governanceBodyOptions.get(i)).get("startTime")).get("name");
            String gbDuration =  (String)((LinkedHashMap)((LinkedHashMap)
                                    governanceBodyOptions.get(i)).get("duration")).get("name");
            String gbTimeZone =  (String)((LinkedHashMap)((LinkedHashMap)
                                    governanceBodyOptions.get(i)).get("timeZone")).get("name");
            Integer gbId    =     (Integer)((LinkedHashMap) governanceBodyOptions.get(i)).get("id");
            String location =  ((String)((LinkedHashMap) governanceBodyOptions.get(i)).get("location"));
            show.hitShowVersion2(gbEntityTypeID,gbId);
            String expectedTimeZone =   (String)JSONUtility.parseJson(show.getShowJsonStr(),
                    "$.body.data.timeZone.values.name");
            String  expectedDuration = (String)JSONUtility.parseJson(show.getShowJsonStr(),
                "$.body.data.duration.values.name");
            String  expectedStartTime = (String)JSONUtility.parseJson(show.getShowJsonStr(),
                "$.body.data.startTime.values.name");
            String  expectedLocation=null;
            try{
              expectedLocation = (String)JSONUtility.parseJson(show.getShowJsonStr(),
                    "$.body.data.location.values");
            }catch (Exception e){
                expectedLocation = null;
            }

            csAssert.assertEquals(gbStartTime,expectedStartTime,
                    "start time is not correct on schedule adhoc metting tab for gb "+gbId );
            csAssert.assertEquals(gbDuration,expectedDuration,
                    "duration is not correct on schedule adhoc metting tab for gb "+gbId );
            csAssert.assertEquals(gbTimeZone,expectedTimeZone,
                    "timezone is not correct on schedule adhoc metting tab for gb "+gbId );
            csAssert.assertEquals(location,expectedLocation,
                    "location is not correct on schedule adhoc metting tab for gb "+gbId );
        }


        //hit user acccount info api and get user's timezone and starttime.
        String info = new AccountInfo().getUserAccountInfoResponse().getResponse().getResponseBody();
        String userTimeZone = (String)JSONUtility.parseJson(info,"$.user.timeZone.timeZone");
        String userStartTime = DateUtils.getCurrentDateInAnyFormat("HH:mm",timeZone);

        csAssert.assertEquals(timeZone,userTimeZone,
                "default timezone: "+timeZone+" is not same as usertimezone: "+userTimeZone);

       String result =  db.doSelect("SELECT '"+ userStartTime +"' <= '"+ startTime+"'").get(0).get(0);

       csAssert.assertEquals(result,"t","default time is "+ startTime +"and usertime is "
       + userStartTime);

       csAssert.assertAll();
    }

    private  List<String> validateMeetingCreation(int gbEntityId){
        TabListData listData = new TabListData();
        String gb_res = listData.hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
        List<String> meetingIds = ListDataHelper.getColumnIds(gb_res);
        if(meetingIds.size()==0){
            return null;
        }else{
            logger.info("meeting created is :  " + meetingIds);
            return meetingIds;
        }
    }



    private int createGB(CustomAssert customAssert, String flow) {
        logger.info("***********************************creating GB*************************************");
        try {
            String section = flow;
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
        return -1;
    }

    private int createAdhocCGB(int gbEntityId, String dateFormat, int gbEntityTypeId, CustomAssert customAssert) {
        logger.info("****creating CGB**********");
        String adhocMeetingResponse="";
        try {
            adhocMeetingResponse = meeting.hitAdhocMeetingApi(String.valueOf(gbEntityId), DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(), -1, dateFormat), "21:00", "Asia/Kolkata (GMT +05:30)", "30 Min", "delhi");

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
        logger.info(adhocMeetingResponse);
        return -1;
    }



    private int validatemeetinginGBTab(int gbEntityId) throws InterruptedException {
        Thread.sleep(60000);
        TabListData listData = new TabListData();
        String gb_res = listData.hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
        List<String> meetingIds = ListDataHelper.getColumnIds(gb_res);
        if(meetingIds.size()==0){
           return  -1;
        }else{
        logger.info("meeting created is :  " + meetingIds.get(0));
        return Integer.parseInt(meetingIds.get(0));
        }
    }

}
