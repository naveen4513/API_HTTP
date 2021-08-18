package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.governancebody.ShareMOM;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestShareMOM {
    private final static Logger logger = LoggerFactory.getLogger(TestShareMOM.class);
    private int gbEntityTypeId;
    private int cgbEntityTypeId;
    private int supplierEntityTypeId;
    private String meetingTabID;
    private String dateFormat;
    private Show show;
    private PostgreSQLJDBC db=null;


    @DataProvider()
    public Object[][] dataProviderForShareMOM() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"communicationtab"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        gbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        supplierEntityTypeId = ConfigureConstantFields.getEntityIdByName("suppliers");
        String listTabConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath");
        String listTabConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName");
        String gbConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        String adhocConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(listTabConfigFilePath, listTabConfigFileName, "tabs mapping", "meetings");
        dateFormat = ParseConfigFile.getValueFromConfigFile(gbConfigFilePath, adhocConfigFileName, "dateformat");
        show = new Show();
//        db = new PostgreSQLJDBC(ConfigureEnvironment.getEnvironmentProperty("dbHostAddress"),
//                ConfigureEnvironment.getEnvironmentProperty("dbPortName"), ConfigureEnvironment.getEnvironmentProperty("dbName"),
//                ConfigureEnvironment.getEnvironmentProperty("dbUserName"), ConfigureEnvironment.getEnvironmentProperty("dbPassword"));
 }



    @Test(dataProvider = "dataProviderForShareMOM")
    public void TestShareMOMUsers(String flow) {
        PostgreSQLJDBC db=new PostgreSQLJDBC();

       CustomAssert customAssert = new CustomAssert();
        int gbEntityId = 0;
        int cgbEntityId = 0;
        try {

            logger.info("create Gb and Adhoc meeting");
            gbEntityId = createGB(customAssert, flow);
            cgbEntityId = createAdhocCGB(gbEntityId, dateFormat, gbEntityTypeId, customAssert);

            //get suppliers ID's from GB
            show.hitShowGetAPI(cgbEntityTypeId,cgbEntityId);
            String cgbShow = show.getShowJsonStr();
            List relations =  (List)JSONUtility.parseJson(cgbShow,"$.body.data.relations.values[*].id");
            logger.info("suppliers in GB " + relations);

            //get vendor ID's from suppliers in CGB
          ArrayList<Integer> vendorsinGB = new ArrayList<>();
            for (int i=0;i<relations.size();i++){
                show.hitShowGetAPI(supplierEntityTypeId,(int)relations.get(i));
                String supplierShow = show.getShowJsonStr();
                Integer vendorId =  (Integer) JSONUtility.parseJson(supplierShow,"$.body.data.vendor.values.id");
                vendorsinGB.add(vendorId);
            }
            logger.info("vendors in GB "+ vendorsinGB);

            //Hit share MOM API and get vendor id of allUsers
            String getResponse = ShareMOM.getShareMom(cgbEntityId).getResponseBody();
           ArrayList<Integer> vendors =  (ArrayList<Integer>)JSONUtility.parseJson(getResponse,"$.allUsers[*].vendorId");
            Set vendorsSet = new HashSet<Integer>(vendors);
            logger.info("vendors in Users in ShareMOM api"+ vendorsSet);

            //Assert vendors in GB and vendors of users of share MOM API
            boolean result =  vendorsinGB.containsAll(vendorsSet);
            customAssert.assertEquals(result,true, "users with vendorId" + vendors+ "are present while vendors in gb are" + vendorsinGB );


            //validate post shareMOM api status code
            String payload = createPayloadPostMOM(cgbShow,getResponse);
           int statusCode = ShareMOM.postShareMom(cgbEntityId,payload).getResponseCode();
            logger.info("statusCode "+ statusCode);
            customAssert.assertEquals(statusCode,200,"Post share mom api throwing unexpected status code");



            //validate post ShareMOM email in DB
            String query = "select * from system_emails where subject ilike '%Meeting Minutes Shared%'order by id desc limit 10";
             int wait =1;
             a:while(wait<5){
                 List<List<String>> emailData = db.doSelect(query);
                 for (int i=0; i<emailData.size();i++){

                     if ((emailData.get(i)).get(10).equals((String)JSONUtility.parseJson(payload,"$.attachmentName")+".pdf")){
                         String expected_to = emailData.get(i).get(1);
                         JSONArray to_arr = (JSONArray)JSONUtility.parseJson(payload,"$.to[*].email");
                         JSONArray toExternal_arr = (JSONArray)JSONUtility.parseJson(payload,"$.toExternal[*]");
                         customAssert.assertEquals(expected_to.split(",").length ,to_arr.size()+toExternal_arr.size(),"No of recipient in to section is incorrect");
                         customAssert.assertTrue(expected_to.contains(toExternal_arr.get(0).toString()),"external user "+toExternal_arr.get(0)+" is not present in the recipient");
                         for (int j = 0; j < to_arr.size(); j++) {
                             customAssert.assertTrue(expected_to.contains(to_arr.get(j).toString()),"to_user "+to_arr.get(j).toString()+" is not present in the recipient");
                         }
                         break a;
                     }
                 }
                 Thread.sleep(10000);
                 wait++;
             }
             customAssert.assertTrue(wait<5,"shared MOM email is not triggered");


        }catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            customAssert.assertTrue(false, "error in shareMOM " + e.getMessage());
        } finally {
            logger.info("******************GB and CGB Deleted***********************************");
            EntityOperationsHelper.deleteEntityRecord("governance body meetings", cgbEntityId);
            EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);
        }
        customAssert.assertAll();
    }





       private String createPayloadPostMOM(String cgbResponse, String shareMOMResponse){
        String temp = ShareMOM.getPostShareMOMPayloadTemplate();

            //date
           Date date = new Date(System.currentTimeMillis());
           DateFormat utcFormat = new SimpleDateFormat("ddMMMyyyy");
           utcFormat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
           String sharedate = utcFormat.format(date);

           //get CGB shortcode and name
           String shortCodeId =  (String)JSONUtility.parseJson(cgbResponse,"$.body.data.shortCodeId.values");
           String name =  (String)JSONUtility.parseJson(cgbResponse,"$.body.data.name.values");

           String subject = "Meeting Minutes - "+shortCodeId+" -"+name;
           String attachmentname = shortCodeId+" - "+sharedate+" - "+name;

           //get To list
           JSONArray to =  (JSONArray)JSONUtility.parseJson(shareMOMResponse,"$.allUsers[0,1]");

           //get toExternal
           JSONArray toExternal = new JSONArray();
          toExternal.add("sarthak.garg@sirionlabs.com");

           //create payload
           JSONObject template = new JSONObject(temp);
           template.put("shareDate",sharedate);
           template.put("subject",subject);
           template.put("attachmentName",attachmentname);
           template.put("toExternal",toExternal);
           template.put("to",to);
           template.put("selectedUsers",to);

           return template.toString();

       }



        private int createGB(CustomAssert customAssert, String flow) {
            logger.info("***********************************creating GB*************************************");
            try {
                boolean isLocal = true;
                String gbResponse = GovernanceBody.createGB(flow, isLocal);
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



    @AfterClass
    public void after(){
        //    db.closeConnection();
                }

        }
