package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestDuplicateCGB {
        private final static Logger logger = LoggerFactory.getLogger(TestGBSchedularTask.class);
        private int gbEntityTypeID;
        private int cgbEntityTypeId;
        private String meetingTabID;
        public  int gbEntityId;
        public HashMap<String, String> cgbEntityIds;
        public HashMap<String, String> updatedCGBEntityIds ;
        public  int gbEntityIdCase2;
        public HashMap<String, String> cgbEntityIdsCase2;
        public HashMap<String, String> updatedCGBEntityIdsCase2 ;

        private Show show = new Show();
        private Edit edit = new Edit();

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");

    }

    @DataProvider()
    public Object[][] dataProviderCGB() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"duplicate_cgb"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderCGB")
    public void TestCGBduplicateOnNextCreationdate(String flow) throws InterruptedException, ParseException {

        CustomAssert csassert = new CustomAssert();
        gbEntityId = createGB(csassert,flow);
        if (!(gbEntityId==-1))
        {
            logger.info("Governance Body Created with Entity id: " + gbEntityId);

            //performing workflow action on Gb

            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel);
            }

            //validate CGB duplicate creation
            waitForCGBCreation(60000,gbEntityId);
            cgbEntityIds = validateMeetingCreation(gbEntityId);

            if(cgbEntityIds.size()!=0){

                if(validateDuplicateValue(cgbEntityIds)){
                    csassert.assertFalse(true,"duplicate CGB's created "+ cgbEntityIds.toString());
                }


                //validate maxOccuranceDate equal to nextStartDate
                List<String> dbData = getNextCreationAndStartDate(gbEntityId);
                String nextStartDate = dbData.get(1).split(" ")[0];
                String maxOccuranceDate =  getMaxOccuranceDateCGB(cgbEntityIds);
                csassert.assertEquals(nextStartDate,maxOccuranceDate,
                        "next start date "+ nextStartDate + " is not equal to max occurance date"+ maxOccuranceDate);


                //update creation and start date in DB
                updateCreationAndStartDate(gbEntityId);
                int taskexecuted =  waitForTaskCreation(900000,gbEntityId);
                if(taskexecuted==2){
                    //validating next creation and start date
                    updatedCGBEntityIds = validateMeetingCreation(gbEntityId);
                    if(validateDuplicateValue(updatedCGBEntityIds)){
                        csassert.assertFalse(true,"duplicate CGB's created after next creation and start date updation "+ updatedCGBEntityIds.toString());
                    }

                }else{
                    csassert.assertFalse(true,"child creation task not after updation" +
                            "in next creation date and next start date");
                }


                //delete CGB's and GB's

                for (Map.Entry<String, String> cgb: validateMeetingCreation(gbEntityId).entrySet()) {
                   /* EntityOperationsHelper.deleteEntityRecord("governance body meetings",
                            Integer.valueOf(cgb.getKey()));*/
                }

            }else{
                csassert.assertFalse(true, "child not created for GB "+ gbEntityId);
            }

         //   EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);

        }else {
            csassert.assertTrue(false,"GB is not Created");
        }

        csassert.assertAll();
    }

    @Test(dataProvider = "dataProviderCGB")
    public void TestCGBduplicateOnDelete(String flow) throws Exception {

        CustomAssert csassert = new CustomAssert();
        gbEntityIdCase2 = createGB(csassert,flow);
        if (!(gbEntityIdCase2==-1))
        {
            logger.info("Governance Body Created with Entity id: " + gbEntityIdCase2);

            //performing workflow action on Gb

            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityIdCase2, actionLabel);
            }

            //validate CGB duplicate creation
            waitForCGBCreation(60000,gbEntityIdCase2);
             cgbEntityIdsCase2 = validateMeetingCreation(gbEntityIdCase2);

            if(cgbEntityIdsCase2.size()!=0){

                if(validateDuplicateValue(cgbEntityIdsCase2)){
                    csassert.assertFalse(true,"duplicate CGB's created "+ cgbEntityIdsCase2.toString());
                }


                //validate maxOccuranceDate equal to nextStartDate
                List<String> dbData = getNextCreationAndStartDate(gbEntityIdCase2);
                String nextStartDate = dbData.get(1).split(" ")[0];
                String maxOccuranceDate =  getMaxOccuranceDateCGB(cgbEntityIdsCase2);
                csassert.assertEquals(nextStartDate,maxOccuranceDate,
                        "next start date "+ nextStartDate + " is not equal to max occurance date"+ maxOccuranceDate);


                //update creation and start date in DB
                editEntity(gbEntityTypeID,gbEntityIdCase2,csassert);

                int taskexecuted =  waitForTaskCreation(900000,gbEntityIdCase2);
                if(taskexecuted==2){
                    //validating meeting deletion after end date update-
                    validateMeetingdeletion(cgbEntityIdsCase2.keySet(),csassert);

                    updatedCGBEntityIdsCase2 = validateMeetingCreation(gbEntityIdCase2);
                    if(validateDuplicateValue(updatedCGBEntityIdsCase2)){
                        csassert.assertFalse(true,"duplicate CGB's created after next creation and start date updation "+ updatedCGBEntityIdsCase2.toString());
                    }

                }else{
                    csassert.assertFalse(true,"child creation task not after updation" +
                            "in next creation date and next start date");
                }


                //delete CGB's and GB's

                for (Map.Entry<String, String> cgb: validateMeetingCreation(gbEntityIdCase2).entrySet()) {
                   /* EntityOperationsHelper.deleteEntityRecord("governance body meetings",
                            Integer.valueOf(cgb.getKey()));*/
                }

            }else{
                csassert.assertFalse(true, "child not created for GB "+ gbEntityIdCase2);
            }

            //   EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);

        }else {
            csassert.assertTrue(false,"GB is not Created");
        }

        csassert.assertAll();
    }


    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 1
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CGB_Case1(){
        CustomAssert customAssert = new CustomAssert();

        List<String> DuplicateCGBs = DuplicateCGBCase1ForNewlyCreatedGB(String.valueOf(gbEntityId));
        List<String> DuplicateCGB = DuplicateCGBCase1AtSystemLevel();

        if (DuplicateCGBs.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 1 \n" + DuplicateCGBs);
        } else if (DuplicateCGB.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 1 at system level\n" + DuplicateCGBs);
        } else {
            customAssert.assertFalse(true, "There are duplicate CGB as per case 1 \n" + DuplicateCGB);
        }
        customAssert.assertAll();
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 2/3/4
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CGB_Case2(){
        CustomAssert customAssert = new CustomAssert();

        List<String> DuplicateCGBs = DuplicateCGBCase2ForNewlyCreatedGB(String.valueOf(gbEntityId));
        List<String> DuplicateCGB = DuplicateCGBCase2AtSystemLevel();

        if (DuplicateCGBs.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 2/3/4 \n" + DuplicateCGBs);
        } else if (DuplicateCGB.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 2/3/4 at system level\n" + DuplicateCGBs);
        } else {
            customAssert.assertFalse(true, "There are duplicate CGB as per case 2/3/4 \n" + DuplicateCGB);
        }
        customAssert.assertAll();
    }


    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 5
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CGB_Case3(){
        CustomAssert customAssert = new CustomAssert();

        List<String> DuplicateCGBs = DuplicateCGBCase3ForNewlyCreatedGB(String.valueOf(gbEntityId));
        List<String> DuplicateCGB = DuplicateCGBCase3AtSystemLevel();

        if (DuplicateCGBs.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 5 \n" + DuplicateCGBs);
        } else if (DuplicateCGB.get(0).contains("[]")) {
            customAssert.assertFalse(false, "There is no duplicate CGB as per case 5 at system level\n" + DuplicateCGBs);
        } else {
            customAssert.assertFalse(true, "There are duplicate CGB as per case 5 \n" + DuplicateCGB);
        }
        customAssert.assertAll();
    }

     private List<String>  validateMeetingdeletion(Set<String> cgbids, CustomAssert csAssert){
         PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
         List<String> data = new ArrayList<>();
         Iterator<String> it = cgbids.iterator();
         while(it.hasNext()){
             String cgbId= it.next();
             String query="select deleted,* from governance_body_child where  id =  "+cgbId;
             try {

                 List<List<String>> results = sqlObj.doSelect(query);

                 if (!results.isEmpty()) {
                     data = results.get(0);
                     if(!data.get(0).equals("t")){
                        csAssert.assertFalse(true, cgbId+" CGB is not deleted after end change changed");
                     }
                 }

             } catch (SQLException e) {
                 logger.error("Exception while Getting GB entity Data from DB using query [{}]. {}", query, e.getMessage());
             }

         }
         sqlObj.closeConnection();
         return data;


     }

     private void editEntity(int entityTypeId,int entityId, CustomAssert csAssert) throws Exception {
        show.hitShowGetAPI(entityTypeId,entityId);
        String showResponse = show.getShowJsonStr();
        JSONObject data = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");
        data.put("expDate", new JSONObject("{\"name\":\"expDate\",\"id\":4709,\"values\":\"08-21-2021\",\"displayValues\":\"Aug-21-2021\",\"multiEntitySupport\":false}"));
        data.remove("history");

        JSONObject payload = new JSONObject();
        JSONObject body = new JSONObject();
        body.put("data",data);
        payload.put("body",body);
        String editResponce = edit.hitEdit("governance body",payload.toString());
        csAssert.assertEquals(JSONUtility.parseJson(editResponce,"$.header.response.status"),
                "success", "governance body "+ entityId+" end date is not updated");

    }

    private List<String> getNextCreationAndStartDate(int gbId){
        List<String> data = new ArrayList<>();
        String query="select next_creation_date,next_start_date,* from governance_body where id = "+gbId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting GB entity Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    private List<List<String>>  getClientJobId(int jobId){

        String query="select * from scheduled_job  where job_id  = "+jobId+" and client_id = "+ConfigureEnvironment.getEnvironmentProperty("client_id");
        List<List<String>> results = null;
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            results = sqlObj.doSelect(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting client Job ID data from DB using query [{}]. {}", query, e.getMessage());
        }

        return results;
    }

    private List<List<String>>  getGBTask(int gbId){

        String query="select * from TASK where scheduled_job_id = "+getClientJobId(14).get(0).get(0)+" and client_id = "+ConfigureEnvironment.getEnvironmentProperty("client_id")+" and entity_type_id = 86 and status = 4 and entity_id = "+gbId;
        List<List<String>> results = null;
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
             results = sqlObj.doSelect(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Task data of GB from DB using query [{}]. {}", query, e.getMessage());
        }

        return results;
    }

    private boolean updateCreationAndStartDate(int gbId){
        boolean results = false;
        //String query="update governance_body set next_creation_date =now() , next_start_date = '2020-07-21 00:00:00' where id = "+gbId;
        String query="update governance_body set next_creation_date =now() where id = "+gbId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
             results = sqlObj.updateDBEntry(query);


            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while updating GB entity Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return results;
    }

    private int createGB(CustomAssert customAssert, String flow) {
        logger.info("***********************************creating GB*************************************");
        try {

            String gbResponse = GovernanceBody.createGB(flow, true);
            if (ParseJsonResponse.validJsonResponse(gbResponse)) {
                int gbEntityId = CreateEntity.getNewEntityId(gbResponse);
                logger.info("Gb successfully created with ID ->" + gbEntityId);
                return gbEntityId;
            }
        } catch (Exception e) {
            logger.error("GB is not creating");
            customAssert.assertTrue(false, "Exception while GB is creating");
        }
        return  -1;
    }

    private  HashMap<String,String> validateMeetingCreation(int gbEntityId) throws ParseException {
        HashMap<String, String> cgb = new HashMap<>();
        TabListData listData = new TabListData();
        String gb_res = listData.hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);


        JSONArray occurrencedate = (JSONArray) JSONUtility.parseJson(gb_res, "$.data[*].[*][?(@.columnName=='occurrencedate')].value");
        JSONArray id = (JSONArray) JSONUtility.parseJson(gb_res, "$.data[*].[*][?(@.columnName=='id')].value");

        if (id.size() != 0) {
            for (int i = 0; i < id.size(); i++) {
                String occuranceDate =  DateUtils.converDateToAnyFormat((String) occurrencedate.get(i),"MMM-dd-yyyy","yyyy-MM-dd");
                cgb.put(((String) id.get(i)).split(":;")[1], occuranceDate);
            }

        }
            return  cgb;
    }

    private  boolean validateDuplicateValue(HashMap<String,String> map){
        boolean duplicate = true;
        List<String> valuesList = new ArrayList<>(map.values());
        Set<String> valuesSet = new HashSet<>(valuesList);
        if(valuesList.size()==valuesSet.size()){
         duplicate = false;
        }
        return duplicate;
    }

    private void waitForCGBCreation(int maxTime, int gbEntityId ) throws InterruptedException, ParseException {
        int time = 0;
        while (time < maxTime && validateMeetingCreation(gbEntityId).size()==0){
            Thread.sleep(5000);
            time = time +5000;

        }
    }

    private int  waitForTaskCreation(int maxTime, int gbEntityId ) throws InterruptedException, ParseException {
        int time = 0;
        while (time < maxTime && getGBTask(gbEntityId).size()!=2){
            Thread.sleep(60000);
            time = time +60000;
           logger.info("wait for millisecond 60000");
        }
        return  getGBTask(gbEntityId).size();
    }

    private String getMaxOccuranceDateCGB(HashMap<String,String> map){
        TreeSet<String> valuesSet = new TreeSet<>(map.values());
        return  valuesSet.last();
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 1 for Newly created childs
     */
    private static List<String> DuplicateCGBCase1ForNewlyCreatedGB(String GBID) {
        String strOutput = new String();
        String query= "select main_data.gb_id,main_data.\"Client ID\",main_data.\"Master GB ID\", main_data.\"GB Status\",main_data.\"GB Title\", \n" +
                "\t   main_data.\"CGB ID\", main_data.\"CGB Name\", main_data.\"CGB Due Date\",\n" +
                "       main_data.\"Date Created\", main_data.\"CGB Status\"\n" +
                "from\n" +
                "    -- 2 Get all the data inlcuding count of CSLs grouped by slaid, due_date that are part of #1 query result and only have status as overdue/upcoming\n" +
                "    (select count(1) as main_data_count,\n" +
                "               gbc.governance_body_id as gb_id,\n" +
                "               gbc.client_id                                       as \"Client ID\",\n" +
                "               concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "               slwfs.description                                   as \"GB Status\",\n" +
                "               gb.title                                         as \"GB Title\",\n" +
                "               array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "               array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "               gbc.occurrence_date                                 as \"CGB Due Date\",\n" +
                "               array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "               array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "--                concat(au.first_name,au.last_name)               as \"Last Modified By\"\n" +
                "        from governance_body_child gbc\n" +
                "                 left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "                 left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "                 left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "                 left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "        where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "              (-- 1 Get all due_date, slaid combination that have duplicates\n" +
                "                  select occurrence_date, governance_body_id\n" +
                "                  from governance_body_child\n" +
                "                  where client_id in (select id from client where deleted=false)\n" +
                "                    and not deleted\n" +
                "                  group by occurrence_date, governance_body_id\n" +
                "                  having count(id) > 1\n" +
                "              )\n" +
                "\n" +
                "=\t\tgbc.status_id in (4, 21)\n" +
                "          and not gbc.deleted\n" +
                "        group by gbc.occurrence_date, gbc.governance_body_id, gb.client_entity_seq_id, gb.title, gb.status_id,\n" +
                "                 slwfs.description,gbc.client_id\n" +
                "    ) as main_data\n" +
                "\n" +
                "        -- 3 This table returns all due_date, slaid combination that have duplicates\n" +
                "        left join (select occurrence_date, governance_body_id, count(1) as count_data\n" +
                "                   from governance_body_child\n" +
                "                   where client_id in (select id from client where deleted=false)\n" +
                "                     and not deleted\n" +
                "                   group by occurrence_date, governance_body_id\n" +
                "                   having count(id) > 1) as data on ((main_data.\"CGB Due Date\", main_data.gb_id) = (data.occurrence_date, data.governance_body_id))\n" +
                "-- 4 filter only results that have table 2 count equal to table 3 count\n" +
                "where main_data.main_data_count = data.count_data and main_data.gb_id = "+ GBID;

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try{
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(0));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 1 at system levels
     */
    private static List<String> DuplicateCGBCase2ForNewlyCreatedGB(String GBID) {
        String strOutput = new String();
        String query= "WITH temp as ( (select governance_body_id, occurrence_date from (select count(1), governance_body_id, occurrence_date, case when status_id = 21 then 4 else status_id end as statusID\n" +
                "from governance_body_child\n" +
                "where (occurrence_date, governance_body_id) in\n" +
                "(select occurrence_date, governance_body_id\n" +
                "from governance_body_child\n" +
                "where client_id in (select id from client where deleted=false)\n" +
                "and not deleted\n" +
                "group by occurrence_date, governance_body_id\n" +
                "having count(id) > 1)\n" +
                "group by governance_body_id, occurrence_date, statusID\n" +
                ") as data group by governance_body_id, occurrence_date having count(1) > 1 order by governance_body_id, occurrence_date))\n" +
                "\n" +
                "select\n" +
                "    gbc.governance_body_id as gb_id,\n" +
                "    gbc.client_id                                       as \"Client ID\",\n" +
                "    concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "    slwfs.description                                   as \"GB Status\",\n" +
                "    gb.title                                         as \"GB Title\",\n" +
                "    array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "    array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "    gbc.occurrence_date                                 as \"CGB Due Date\",\n" +
                "    array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "from governance_body_child gbc\n" +
                "         left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "         left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "         left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "         left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "(select bucketOverdue.occurrence_date, bucketOverdue.governance_body_id from\n" +
                "          (select count(1) as countOverdue, governance_body_id, occurrence_date from governance_body_child where (governance_body_id, occurrence_date) in\n" +
                "\t\t  (select governance_body_id, occurrence_date from temp)\n" +
                "\t\t\tand status_id in (4, 21) group by governance_body_id, occurrence_date) as bucketOverdue\n" +
                "\t\tleft join\n" +
                "          -- allBucket gives the size of all csls irrespective of status_id grouped by slaid, due_date\n" +
                "              (select count(1) as totalCount, governance_body_id, occurrence_date from governance_body_child where (governance_body_id, occurrence_date) in\n" +
                "      (select governance_body_id, occurrence_date from temp) group by governance_body_id, occurrence_date) as allBucket\n" +
                "          on ((allBucket.governance_body_id, allBucket.occurrence_date) = (bucketOverdue.governance_body_id, bucketOverdue.occurrence_date))\n" +
                "       where bucketOverdue.countOverdue = allBucket.totalCount - 1) and gbc.governance_body_id= "+GBID +"\n" +
                "\t\tgroup by gbc.occurrence_date, gbc.governance_body_id, gb.client_entity_seq_id, gb.title, gb.status_id,\n" +
                "         slwfs.description,gb.title,gbc.client_id";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try {
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(0));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }

        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 2 for Newly created childs
     */
    private static List<String> DuplicateCGBCase3ForNewlyCreatedGB(String GBID) {
        String strOutput = new String();
        String query= "select\n" +
                "    gbc.governance_body_id as gb_id,\n" +
                "    gbc.client_id                                       as \"Client ID\",\n" +
                "    concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "    slwfs.description                                   as \"GB Status\",\n" +
                "    gb.title                                         as \"GB Title\",\n" +
                "    array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "    array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "    gbc.occurrence_date                                        as \"CGB Due Date\",\n" +
                "    array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "from governance_body_child gbc\n" +
                "         left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "         left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "         left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "         left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "      (select occurrence_date, governance_body_id from governance_body_child where status_id not in (4, 21) and not deleted and not is_archive\n" +
                "       group by occurrence_date, governance_body_id having\n" +
                "               count(1) > 1) and not gbc.deleted and not gbc.is_archive and gbc.governance_body_id= "+GBID+ "\n"+
                "group by occurrence_date, gb_id, gb.client_entity_seq_id, slwfs.description,gbc.client_id,gb.title;\n";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try{
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(0));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 2 at system level
     */
    private static List<String> DuplicateCGBCase1AtSystemLevel() {
        String strOutput = new String();
        String query= "select main_data.gb_id,main_data.\"Client ID\",main_data.\"Master GB ID\", main_data.\"GB Status\",main_data.\"GB Title\", \n" +
                "\t   main_data.\"CGB ID\", main_data.\"CGB Name\", main_data.\"CGB Due Date\",\n" +
                "       main_data.\"Date Created\", main_data.\"CGB Status\"\n" +
                "from\n" +
                "    -- 2 Get all the data inlcuding count of CSLs grouped by slaid, due_date that are part of #1 query result and only have status as overdue/upcoming\n" +
                "    (select count(1) as main_data_count,\n" +
                "               gbc.governance_body_id as gb_id,\n" +
                "               gbc.client_id                                       as \"Client ID\",\n" +
                "               concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "               slwfs.description                                   as \"GB Status\",\n" +
                "               gb.title                                         as \"GB Title\",\n" +
                "               array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "               array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "               gbc.occurrence_date                                 as \"CGB Due Date\",\n" +
                "               array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "               array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "--                concat(au.first_name,au.last_name)               as \"Last Modified By\"\n" +
                "        from governance_body_child gbc\n" +
                "                 left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "                 left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "                 left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "                 left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "        where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "              (-- 1 Get all due_date, slaid combination that have duplicates\n" +
                "                  select occurrence_date, governance_body_id\n" +
                "                  from governance_body_child\n" +
                "                  where client_id in (select id from client where deleted=false)\n" +
                "                    and not deleted\n" +
                "                  group by occurrence_date, governance_body_id\n" +
                "                  having count(id) > 1\n" +
                "              )\n" +
                "\n" +
                "=\t\tgbc.status_id in (4, 21)\n" +
                "          and not gbc.deleted\n" +
                "        group by gbc.occurrence_date, gbc.governance_body_id, gb.client_entity_seq_id, gb.title, gb.status_id,\n" +
                "                 slwfs.description,gbc.client_id\n" +
                "    ) as main_data\n" +
                "\n" +
                "        -- 3 This table returns all due_date, slaid combination that have duplicates\n" +
                "        left join (select occurrence_date, governance_body_id, count(1) as count_data\n" +
                "                   from governance_body_child\n" +
                "                   where client_id in (select id from client where deleted=false)\n" +
                "                     and not deleted\n" +
                "                   group by occurrence_date, governance_body_id\n" +
                "                   having count(id) > 1) as data on ((main_data.\"CGB Due Date\", main_data.gb_id) = (data.occurrence_date, data.governance_body_id))\n" +
                "-- 4 filter only results that have table 2 count equal to table 3 count\n" +
                "where main_data.main_data_count = data.count_data";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try {
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(0));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 3 for Newly created childs
     */
    private static List<String> DuplicateCGBCase2AtSystemLevel() {
        String strOutput = new String();
        String query= "WITH temp as ( (select governance_body_id, occurrence_date from (select count(1), governance_body_id, occurrence_date, case when status_id = 21 then 4 else status_id end as statusID\n" +
                "from governance_body_child\n" +
                "where (occurrence_date, governance_body_id) in\n" +
                "(select occurrence_date, governance_body_id\n" +
                "from governance_body_child\n" +
                "where client_id in (select id from client where deleted=false)\n" +
                "and not deleted\n" +
                "group by occurrence_date, governance_body_id\n" +
                "having count(id) > 1)\n" +
                "group by governance_body_id, occurrence_date, statusID\n" +
                ") as data group by governance_body_id, occurrence_date having count(1) > 1 order by governance_body_id, occurrence_date))\n" +
                "\n" +
                "select\n" +
                "    gbc.governance_body_id as gb_id,\n" +
                "    gbc.client_id                                       as \"Client ID\",\n" +
                "    concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "    slwfs.description                                   as \"GB Status\",\n" +
                "    gb.title                                         as \"GB Title\",\n" +
                "    array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "    array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "    gbc.occurrence_date                                 as \"CGB Due Date\",\n" +
                "    array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "from governance_body_child gbc\n" +
                "         left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "         left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "         left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "         left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "(select bucketOverdue.occurrence_date, bucketOverdue.governance_body_id from\n" +
                "          (select count(1) as countOverdue, governance_body_id, occurrence_date from governance_body_child where (governance_body_id, occurrence_date) in\n" +
                "\t\t  (select governance_body_id, occurrence_date from temp)\n" +
                "\t\t\tand status_id in (4, 21) group by governance_body_id, occurrence_date) as bucketOverdue\n" +
                "\t\tleft join\n" +
                "          -- allBucket gives the size of all csls irrespective of status_id grouped by slaid, due_date\n" +
                "              (select count(1) as totalCount, governance_body_id, occurrence_date from governance_body_child where (governance_body_id, occurrence_date) in\n" +
                "      (select governance_body_id, occurrence_date from temp) group by governance_body_id, occurrence_date) as allBucket\n" +
                "          on ((allBucket.governance_body_id, allBucket.occurrence_date) = (bucketOverdue.governance_body_id, bucketOverdue.occurrence_date))\n" +
                "       where bucketOverdue.countOverdue = allBucket.totalCount - 1) \n" +
                "\t\tgroup by gbc.occurrence_date, gbc.governance_body_id, gb.client_entity_seq_id, gb.title, gb.status_id,\n" +
                "         slwfs.description,gb.title,gbc.client_id";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();
        try {
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(1));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 10 August 2020
     * To check duplidate cgb case 3 at system levels
     */
    private static List<String> DuplicateCGBCase3AtSystemLevel() {
        String strOutput = new String();
        String query= "select\n" +
                "    gbc.governance_body_id as gb_id,\n" +
                "    gbc.client_id                                       as \"Client ID\",\n" +
                "    concat('GB0', gb.client_entity_seq_id)             as \"Master GB ID\",\n" +
                "    slwfs.description                                   as \"GB Status\",\n" +
                "    gb.title                                         as \"GB Title\",\n" +
                "    array_agg(concat('CGB0', gbc.client_entity_seq_id))  as \"CGB ID\",\n" +
                "    array_agg(gbc.title)                                 as \"CGB Name\",\n" +
                "    gbc.occurrence_date                                        as \"CGB Due Date\",\n" +
                "    array_agg(date(gbc.date_created))                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"CGB Status\"\n" +
                "from governance_body_child gbc\n" +
                "         left join governance_body gb on (gbc.governance_body_id = gb.id)\n" +
                "         left join work_flow_status cwft on (gbc.status_id = cwft.id)\n" +
                "         left join work_flow_status slwfs on (gb.status_id = slwfs.id)\n" +
                "         left join app_user au on gbc.last_modified_by_user_id = au.id\n" +
                "where (gbc.occurrence_date, gbc.governance_body_id) in\n" +
                "      (select occurrence_date, governance_body_id from governance_body_child where status_id not in (4, 21) and not deleted and not is_archive\n" +
                "       group by occurrence_date, governance_body_id having\n" +
                "               count(1) > 1) and not gbc.deleted and not gbc.is_archive" +
                "group by occurrence_date, gb_id, gb.client_entity_seq_id, slwfs.description,gbc.client_id,gb.title;\n";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try {
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                for(List<String> temp:results){
                    tempdata.add(temp.get(0));
                }
            }
            Set<String> hashsetList = new HashSet<String>(tempdata);
            String[][] replacements = {{"{", ""}, {"}", ""}, {" ", ""}};
            //loop over the array and replace
            strOutput = hashsetList.toString();
            for(String[] replacement: replacements) {
                strOutput = strOutput.replace(replacement[0], replacement[1]);
            }
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }
        }
        return Arrays.asList(strOutput);
    }
}
