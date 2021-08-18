package com.sirionlabs.test.duplicateData;

import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestSLDuplicateData {

    private static Logger logger = LoggerFactory.getLogger(TestSLDuplicateData.class);
    int slEntityTypeId = 14;
    private String slConfigFilePath;
    private String slConfigFileName;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    int clientId = 1002;

    @BeforeClass
    public void beforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");
    }

    @Test
    public void Test_DupData_NextCreationDate(){

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        try{

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            String PCQ = "";
            String DCQ = "";
            int serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest,PCQ,DCQ,customAssert);

            if (serviceLevelId != -1) {

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, "", customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                ArrayList<String> childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);
                HashMap<String, String> cslIdMapAccToServiceDate = new HashMap<>();
                HashMap<String, Integer> cslIdCreatedMap = new HashMap<>();

                for (int i = 0; i < childServiceLevelIds.size(); i++) {

                    if (cslIdCreatedMap.containsKey(childServiceLevelIds.get(i))) {
                        customAssert.assertTrue(false, "Duplicate CSL ID Exists while creation");
                    } else {
                        cslIdCreatedMap.put(childServiceLevelIds.get(i), 1);
                    }
                }

                addCSLToDelete(childServiceLevelIds);
                slToDelete.add(serviceLevelId);
                String currentDate = postgreSQLJDBC.doSelect("SELECT CURRENT_DATE").get(0).get(0);

                String currentTimeStamp = postgreSQLJDBC.doSelect("select now()").get(0).get(0);
                postgreSQLJDBC.updateDBEntry("update sla SET next_creation_date = '" + currentDate + "' where id = '" + serviceLevelId + "' ");

                Long timeOut = 300000L;
                Long timeElapsed = 0L;
                List<List<String>> sqlOutput = new ArrayList<>();
                while (timeElapsed < timeOut) {

                    sqlOutput = postgreSQLJDBC.doSelect("select status from task where entity_id = '" + serviceLevelId + "' and entity_type_id = " + slEntityTypeId +
                            " and scheduled_job_id = (select id from scheduled_job where job_id = 5 and client_id = " + clientId + ") and date_modified > '" + currentTimeStamp + "' order by id desc ;");

                    if(sqlOutput.size() > 0){
                        break;
                    }
                    Thread.sleep(5000);
                    timeElapsed = timeElapsed + 5000;
                }
                if(sqlOutput.size() > 0) {
                    String status = sqlOutput.get(0).get(0);
                    if (!status.equals("4")) {
                        customAssert.assertTrue(false,"Next Creation Date child creation job failed");
                    }else {
                        TabListData tabListData = new TabListData();
                        String payload = "{\"filterMap\":{\"entityTypeId\":15,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
                        String tabListResponse = tabListData.hitTabListDataV2(7,slEntityTypeId,serviceLevelId,payload);
                        JSONObject tabListResponseJson = new JSONObject(tabListResponse);
                        JSONArray dataArray = tabListResponseJson.getJSONArray("data");
                        String key;
                        for(int i = 0;i<dataArray.length();i++){
                            Iterator<String> keys = dataArray.getJSONObject(i).keys();

                            while (keys.hasNext()){
                                key = keys.next();
                                if(dataArray.getJSONObject(i).getJSONObject(key).get("columnName").equals("duedate")){
                                    String dueDate = dataArray.getJSONObject(i).getJSONObject(key).get("value").toString();

                                    if(cslIdMapAccToServiceDate.containsKey(dueDate)){
                                        customAssert.assertTrue(false,"More than one child exists for the same service Date for the due Date " + dueDate + " and Service Level Id " + serviceLevelId);
                                    }else {
                                        cslIdMapAccToServiceDate.put(dueDate,"1");
                                    }
                                }

                            }
                        }

                    }
                }else {
                    customAssert.assertTrue(false,"No task generated for next creation date");
                }

            }

        }catch (Exception e){
            logger.error("Exception in main test method " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception in main test method " + e.getStackTrace());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        customAssert.assertAll();
    }


    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate csl case 1
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CSL_Case1(){
        CustomAssert customAssert = new CustomAssert();

        for(Integer SL:slToDelete) {
            List<String> DuplicateCSLs = DuplicateCSLCase1ForNewlyCreatedSL(String.valueOf(SL));
            List<String> DuplicateCSL = DuplicateCSLCase1AtSystemLevel();

            if (DuplicateCSLs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 \n" + DuplicateCSLs);
            } else if (DuplicateCSL.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 at system level\n" + DuplicateCSLs);
            } else {
                customAssert.assertFalse(true, "There are duplicate CSL as per case 1 \n" + DuplicateCSL);
            }
            customAssert.assertAll();
        }
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate csl case 2
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CSL_Case2(){
        CustomAssert customAssert = new CustomAssert();

        for(Integer SL:slToDelete) {
            List<String> DuplicateCSLs = DuplicateCSLCase2ForNewlyCreatedSL(String.valueOf(SL));
            List<String> DuplicateCSL = DuplicateCSLCase2AtSystemLevel();

            if (DuplicateCSLs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 \n" + DuplicateCSLs);
            } else if (DuplicateCSL.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 at system level\n" + DuplicateCSLs);
            } else {
                customAssert.assertFalse(true, "There are duplicate CSL as per case 1 \n" + DuplicateCSL);
            }
            customAssert.assertAll();
        }
    }


    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate csl case 3
     */
    @Test
    public void Test_Duplicate_DB_level_Check_CSL_Case3(){
        CustomAssert customAssert = new CustomAssert();

        for(Integer SL:slToDelete) {
            List<String> DuplicateCSLs = DuplicateCSLCase3ForNewlyCreatedSL(String.valueOf(SL));
            List<String> DuplicateCSL = DuplicateCSLCase3AtSystemLevel();

            if (DuplicateCSLs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 \n" + DuplicateCSLs);
            } else if (DuplicateCSL.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate CSL as per case 1 at system level\n" + DuplicateCSLs);
            } else {
                customAssert.assertFalse(true, "There are duplicate CSL as per case 1 \n" + DuplicateCSL);
            }
            customAssert.assertAll();
        }
    }

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }

    private void addCSLToDelete(ArrayList<String> cslToDeleteList){

        try {
            for (String cslIDToDelete : cslToDeleteList) {
                cslToDelete.add(Integer.parseInt(cslIDToDelete));
            }
        }catch (Exception e){
            logger.error("Error while adding child service level to deleted list");
        }
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate csl case 1 for Newly created childs
     */
    private static List<String> DuplicateCSLCase1ForNewlyCreatedSL(String SLID) {
        String strOutput = new String();
        String query= "select main_data.slaid, main_data.\"CSL_DB_ID\", main_data.\"CSL Name\", main_data.\"Service Date\", main_data.\"Date Created\", main_data.performance_status, main_data.\"MSL Status\",main_data.\"Client_ID\" from\n" +
                "-- 2 Get all the data inlcuding count of CSLs grouped by slaid, due_date that are part of #1 query result and only have status as overdue/upcoming\n" +
                "    (select count(1)                                    as main_data_count,\n" +
                "    csl.slaid                                           as slaid,\n" +
                "    concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\",\n" +
                "    array_agg( csl.id)                                  as \"CSL_DB_ID\",\n" +
                "    array_agg(csl.name)                                 as \"CSL Name\",\n" +
                "    csl.due_date                                        as \"Service Date\",\n" +
                "    array_agg(csl.date_created)                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"performance_status\",\n" +
                "    slwfs.description                                   as \"MSL Status\",\n" +
                "    csl.client_id                                       as \"Client_ID\"\n" +
                "        from child_sla csl\n" +
                "            left join sla sla on (csl.slaid = sla.id)\n" +
                "            left join work_flow_status cwft on (csl.status_id = cwft.id)\n" +
                "            left join work_flow_status slwfs on (sla.status_id = slwfs.id)\n" +
                "            left join app_user au on csl.last_modified_by_user_id = au.id\n" +
                "\t\t\t\twhere (csl.due_date, csl.slaid) in\n" +
                "                   (\n" +
                "                 -- 1 Get all due_date, slaid combination that have duplicates\n" +
                "                         select due_date, slaid\n" +
                "                         from child_sla\n" +
                "                         where not deleted\n" +
                "                         group by due_date, slaid\n" +
                "                         having count(id) > 1\n" +
                "                    )\n" +
                "               and sla.status_id = 5 -- sla is active\n" +
                "               and csl.status_id in (4, 21)\n" +
                "               and not csl.deleted\n" +
                "             group by csl.due_date, csl.slaid, sla.client_entity_seq_id, sla.name, sla.status_id,\n" +
                "              slwfs.description,csl.client_id\n" +
                "            ) as main_data\n" +
                "        -- 3 This table returns all due_date, slaid combination that have duplicates\n" +
                "        left join (select due_date, slaid, count(1) as count_data\n" +
                "                   from child_sla\n" +
                "                   where not deleted\n" +
                "                   group by due_date, slaid\n" +
                "                   having count(id) > 1) as data on ((main_data.\"Service Date\", main_data.\"slaid\") = (data.due_date, data.slaid))\n" +
                "-- 4 filter only results that have table 2 count equal to table 3 count\n" +
                "where main_data.main_data_count = data.count_data and main_data.slaid ="+SLID;

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try{
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
     * Created on 05 August 2020
     * To check duplidate csl case 1 at system levels
     */
    private static List<String> DuplicateCSLCase2ForNewlyCreatedSL(String SLID) {
        String strOutput = new String();
        String query= "WITH temp as ((select slaid, due_date from (select count(1), slaid, due_date, case when status_id = 21 then 4 else status_id end as statusID\n" +
                "                                             from child_sla\n" +
                "                                             where (due_date, slaid) in\n" +
                "                                                   (select due_date, slaid\n" +
                "                                                       from child_sla\n" +
                "                                                       where client_id = 1002\n" +
                "                                                         and not deleted\n" +
                "                                                       group by due_date, slaid\n" +
                "                                                       having count(id) > 1)\n" +
                "                                             group by slaid, due_date, statusID) \n" +
                "                                             as data group by slaid, due_date having count(1) > 1 order by slaid, due_date))\n" +
                "select" +
                "            csl.slaid as slaid," +
                "            concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\"," +
                "            array_agg(concat('CSL', csl.client_entity_seq_id))  as \"CSL ID\"," +
                "            csl.due_date                                        as \"Service Date\"," +
                "            array_agg(csl.date_created)                         as \"Date Created\"," +
                "            array_agg(cwft.description)                         as \"performance_status\"," +
                "            slwfs.description                                   as \"MSL Status\"" +
                "     from child_sla csl" +
                "              left join sla sla on (csl.slaid = sla.id)" +
                "              left join work_flow_status cwft on (csl.status_id = cwft.id)" +
                "              left join work_flow_status slwfs on (sla.status_id = slwfs.id)" +
                "              left join app_user au on csl.last_modified_by_user_id = au.id" +
                "     where (csl.due_date, csl.slaid) in" +
                "(select bucketOverdue.due_date, bucketOverdue.slaid from" +
                "    (select count(1) as countOverdue, slaid, due_date from child_sla " +
                "        where \n" +
                "        (slaid, due_date) in (select slaid, due_date from temp) and status_id in (4, 21) group by slaid, due_date) as bucketOverdue\n" +
                "        left join\n" +
                "    (select count(1) as totalCount, slaid, due_date from child_sla " +
                "    where (slaid, due_date) in (select slaid, due_date from temp) group by slaid, due_date) as allBucket" +
                "    on ((allBucket.slaid, allBucket.due_date) = (bucketOverdue.slaid, bucketOverdue.due_date))" +
                "where bucketOverdue.countOverdue = allBucket.totalCount - 1) and csl.slaid = " + SLID +
                "group by csl.due_date, csl.slaid, sla.client_entity_seq_id, sla.name, sla.status_id,slwfs.description";

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
     * Created on 05 August 2020
     * To check duplidate csl case 2 for Newly created childs
     */
    private static List<String> DuplicateCSLCase3ForNewlyCreatedSL(String SLID) {
        String strOutput = new String();
        String query= "select" +
                "    csl.slaid as slaid," +
                "    concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\"," +
                "    array_agg(concat('CSL', csl.client_entity_seq_id))  as \"CSL ID\"," +
                "    csl.due_date                                        as \"Service Date\"," +
                "    array_agg(csl.date_created)                         as \"Date Created\"," +
                "    array_agg(cwft.description)                         as \"performance_status\"," +
                "    slwfs.description                                   as \"MSL Status\"" +
                "from child_sla csl" +
                "         left join sla sla on (csl.slaid = sla.id)" +
                "         left join work_flow_status cwft on (csl.status_id = cwft.id)" +
                "         left join work_flow_status slwfs on (sla.status_id = slwfs.id)" +
                "         left join app_user au on csl.last_modified_by_user_id = au.id" +
                "where (csl.due_date, csl.slaid) in" +
                "(select due_date, slaid from child_sla where status_id not in (4, 21) and not deleted and not is_archive group by slaid, due_date having\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "        count(1) > 1) and not csl.deleted and not csl.is_archive and csl.slaid= "+SLID +
                "        group by due_date, slaid, sla.client_entity_seq_id, slwfs.description";

        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        List<String> tempdata= new ArrayList<String>();

        try{
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
     * Created on 05 August 2020
     * To check duplidate csl case 2 at system level
     */
    private static List<String> DuplicateCSLCase1AtSystemLevel() {
        String strOutput = new String();
        String query= "select main_data.slaid, main_data.\"CSL_DB_ID\", main_data.\"CSL Name\", main_data.\"Service Date\", main_data.\"Date Created\", main_data.performance_status, main_data.\"MSL Status\",main_data.\"Client_ID\" from\n" +
                "-- 2 Get all the data inlcuding count of CSLs grouped by slaid, due_date that are part of #1 query result and only have status as overdue/upcoming\n" +
                "    (select count(1)                                    as main_data_count,\n" +
                "    csl.slaid                                           as slaid,\n" +
                "    concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\",\n" +
                "    array_agg( csl.id)                                  as \"CSL_DB_ID\",\n" +
                "    array_agg(csl.name)                                 as \"CSL Name\",\n" +
                "    csl.due_date                                        as \"Service Date\",\n" +
                "    array_agg(csl.date_created)                         as \"Date Created\",\n" +
                "    array_agg(cwft.description)                         as \"performance_status\",\n" +
                "    slwfs.description                                   as \"MSL Status\",\n" +
                "    csl.client_id                                       as \"Client_ID\"\n" +
                "        from child_sla csl\n" +
                "            left join sla sla on (csl.slaid = sla.id)\n" +
                "            left join work_flow_status cwft on (csl.status_id = cwft.id)\n" +
                "            left join work_flow_status slwfs on (sla.status_id = slwfs.id)\n" +
                "            left join app_user au on csl.last_modified_by_user_id = au.id\n" +
                "\t\t\t\twhere (csl.due_date, csl.slaid) in\n" +
                "                   (\n" +
                "                 -- 1 Get all due_date, slaid combination that have duplicates\n" +
                "                         select due_date, slaid\n" +
                "                         from child_sla\n" +
                "                         where not deleted\n" +
                "                         group by due_date, slaid\n" +
                "                         having count(id) > 1\n" +
                "                    )\n" +
                "               and sla.status_id = 5 -- sla is active\n" +
                "               and csl.status_id in (4, 21)\n" +
                "               and not csl.deleted\n" +
                "             group by csl.due_date, csl.slaid, sla.client_entity_seq_id, sla.name, sla.status_id,\n" +
                "              slwfs.description,csl.client_id\n" +
                "            ) as main_data\n" +
                "        -- 3 This table returns all due_date, slaid combination that have duplicates\n" +
                "        left join (select due_date, slaid, count(1) as count_data\n" +
                "                   from child_sla\n" +
                "                   where not deleted\n" +
                "                   group by due_date, slaid\n" +
                "                   having count(id) > 1) as data on ((main_data.\"Service Date\", main_data.\"slaid\") = (data.due_date, data.slaid))\n" +
                "-- 4 filter only results that have table 2 count equal to table 3 count\n" +
                "where main_data.main_data_count = data.count_data";

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
     * Created on 05 August 2020
     * To check duplidate csl case 3 for Newly created childs
     */
    private static List<String> DuplicateCSLCase2AtSystemLevel() {
        String strOutput = new String();
        String query= "WITH temp as ((select slaid, due_date from (select count(1), slaid, due_date, case when status_id = 21 then 4 else status_id end as statusID\n" +
                "                                             from child_sla\n" +
                "                                             where (due_date, slaid) in\n" +
                "                                                   (select due_date, slaid\n" +
                "                                                       from child_sla\n" +
                "                                                       where client_id = 1002\n" +
                "                                                         and not deleted\n" +
                "                                                       group by due_date, slaid\n" +
                "                                                       having count(id) > 1)\n" +
                "                                             group by slaid, due_date, statusID) \n" +
                "                                             as data group by slaid, due_date having count(1) > 1 order by slaid, due_date))\n" +
                "select" +
                "            csl.slaid as slaid," +
                "            concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\"," +
                "            array_agg(concat('CSL', csl.client_entity_seq_id))  as \"CSL ID\"," +
                "            csl.due_date                                        as \"Service Date\"," +
                "            array_agg(csl.date_created)                         as \"Date Created\"," +
                "            array_agg(cwft.description)                         as \"performance_status\"," +
                "            slwfs.description                                   as \"MSL Status\"" +
                "     from child_sla csl" +
                "              left join sla sla on (csl.slaid = sla.id)" +
                "              left join work_flow_status cwft on (csl.status_id = cwft.id)" +
                "              left join work_flow_status slwfs on (sla.status_id = slwfs.id)" +
                "              left join app_user au on csl.last_modified_by_user_id = au.id" +
                "     where (csl.due_date, csl.slaid) in" +
                "(select bucketOverdue.due_date, bucketOverdue.slaid from" +
                "    (select count(1) as countOverdue, slaid, due_date from child_sla " +
                "        where \n" +
                "        (slaid, due_date) in (select slaid, due_date from temp) and status_id in (4, 21) group by slaid, due_date) as bucketOverdue\n" +
                "        left join\n" +
                "    (select count(1) as totalCount, slaid, due_date from child_sla " +
                "    where (slaid, due_date) in (select slaid, due_date from temp) group by slaid, due_date) as allBucket" +
                "    on ((allBucket.slaid, allBucket.due_date) = (bucketOverdue.slaid, bucketOverdue.due_date))" +
                "where bucketOverdue.countOverdue = allBucket.totalCount - 1)" +
                "group by csl.due_date, csl.slaid, sla.client_entity_seq_id, sla.name, sla.status_id,slwfs.description";

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
     * Created on 05 August 2020
     * To check duplidate csl case 3 at system levels
     */
    private static List<String> DuplicateCSLCase3AtSystemLevel() {
        String strOutput = new String();
        String query= "select" +
                "    csl.slaid as slaid," +
                "    concat('SL0', sla.client_entity_seq_id)             as \"Master SL ID\"," +
                "    array_agg(concat('CSL', csl.client_entity_seq_id))  as \"CSL ID\"," +
                "    csl.due_date                                        as \"Service Date\"," +
                "    array_agg(csl.date_created)                         as \"Date Created\"," +
                "    array_agg(cwft.description)                         as \"performance_status\"," +
                "    slwfs.description                                   as \"MSL Status\"" +
                "from child_sla csl" +
                "         left join sla sla on (csl.slaid = sla.id)" +
                "         left join work_flow_status cwft on (csl.status_id = cwft.id)" +
                "         left join work_flow_status slwfs on (sla.status_id = slwfs.id)" +
                "         left join app_user au on csl.last_modified_by_user_id = au.id" +
                "where (csl.due_date, csl.slaid) in" +
                "(select due_date, slaid from child_sla where status_id not in (4, 21) and not deleted and not is_archive group by slaid, due_date having\t\t\t\t\t\t\t\t\t\t\t\t\n" +
                "        count(1) > 1) and not csl.deleted and not csl.is_archive" +
                "        group by due_date, slaid, sla.client_entity_seq_id, slwfs.description";

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


}
