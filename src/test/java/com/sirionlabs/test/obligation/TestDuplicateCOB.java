package com.sirionlabs.test.obligation;

import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestDuplicateCOB {

    private final static Logger logger = LoggerFactory.getLogger(TestDuplicateCOB.class);

    private static String configFilePath = null;
    private static String configFileName = null;
    private static String extraFieldsConfigFilePath = null;
    private static String extraFieldsConfigFileName = null;
    private static Integer obligationEntityTypeId;
    private int obligationEntityId;
    private ListRendererTabListData tabData;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        tabData = new ListRendererTabListData();

    }

    @DataProvider()
    public Object[][] dataProviderForGBSchedularTask() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"duplicate_cob"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForGBSchedularTask")
    public void TestGBTabs(String flow) throws InterruptedException, ParseException {
        CustomAssert csassert = new CustomAssert();

        //Create obligation
        obligationEntityId = createOB(csassert,flow);
        if (!( obligationEntityId==-1))
        {
            logger.info("Obligation Created with Entity id: " + obligationEntityId);
            //performing workflow action on Obligation
            logger.info("Perform Entity Workflow Action For Created Obligation");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send for Owner Review", "Review Complete", "Approve", "Activate"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("obligations", obligationEntityTypeId, obligationEntityId, actionLabel);
            }
            //wait for COB creation
            waitForCGBCreation(60000,obligationEntityId);
            //get COB's with due Date
            HashMap<String, String> COBEntityIds = validateCOBCreation(obligationEntityId);
            if(COBEntityIds.size()!=0){
                //validate COB duplicate due date
                if(validateDuplicateValue(COBEntityIds)){
                    csassert.assertFalse(true,"duplicate COB's created "+ COBEntityIds.toString());
                }
                //validate maxOccuranceDate equal to nextStartDate
                List<String> dbData = getNextCreationAndStartDate(obligationEntityId);
                String nextStartDate = dbData.get(1).split(" ")[0];
                String maxOccuranceDate =  getMaxOccuranceDateCOB(COBEntityIds);
                csassert.assertEquals(nextStartDate,maxOccuranceDate,
                        "next start date "+ nextStartDate + " is not equal to max occurance date"+ maxOccuranceDate);
                //update next creation Date creation  in DB
                updateCreationDate(obligationEntityId);
                //wait for COB creation task to be executed again
                int taskexecuted =  waitForTaskCreation(900000,obligationEntityId);
                HashMap<String, String> updatedCOBEntityIds;
                if(taskexecuted==2){
                    //get COB's with due Date
                    updatedCOBEntityIds = validateCOBCreation(obligationEntityId);
                    //validate Duplicate COB creation
                    if(validateDuplicateValue(updatedCOBEntityIds)){
                        csassert.assertFalse(true,"duplicate CGB's created after next creation and start date updation "+ updatedCOBEntityIds.toString());
                    }
                }else{
                    csassert.assertFalse(true,"child creation task not after updation" +
                            "in next creation date and next start date");
                }
                //delete CGB's and GB's
                for ( Map.Entry<String, String> cob: validateCOBCreation(obligationEntityId).entrySet()) {
                    EntityOperationsHelper.deleteEntityRecord("child obligations",
                            Integer.valueOf(cob.getKey()));
                }
            }else{
                csassert.assertFalse(true, "child not created for OB "+ obligationEntityId);
            }
          EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);
        }else {
            csassert.assertTrue(false,"OB is not Created");
        }
        csassert.assertAll();
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate cob case 1
     */
    @Test
    public void Test_Duplicate_DB_level_Check_COB_Case1(){
        CustomAssert customAssert = new CustomAssert();

            List<String> DuplicateCOBs = DuplicateCOBCase1ForNewlyCreatedDNO(String.valueOf(obligationEntityId));
            List<String> DuplicateCOB = DuplicateCOBCase1AtSystemLevel();

            if (DuplicateCOBs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 1 \n" + DuplicateCOBs);
            } else if (DuplicateCOB.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 1 at system level\n" + DuplicateCOBs);
            } else {
                customAssert.assertFalse(true, "There are duplicate COB as per case 1 \n" + DuplicateCOB);
            }
            customAssert.assertAll();
    }

   /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate cob case 2/3/4
     */
    @Test
    public void Test_Duplicate_DB_level_Check_COB_Case2(){
        CustomAssert customAssert = new CustomAssert();

            List<String> DuplicateCOBs = DuplicateCOBCase2ForNewlyCreatedDNO(String.valueOf(obligationEntityId));
            List<String> DuplicateCOB = DuplicateCOBCase2AtSystemLevel();

            if (DuplicateCOBs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 2/3/4 \n" + DuplicateCOBs);
            } else if (DuplicateCOB.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 2/3/4 at system level\n" + DuplicateCOBs);
            } else {
                customAssert.assertFalse(true, "There are duplicate COB as per case 2/3/4 \n" + DuplicateCOB);
            }
            customAssert.assertAll();
    }


    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate cob case 5
     */
    @Test
    public void Test_Duplicate_DB_level_Check_COB_Case3(){
        CustomAssert customAssert = new CustomAssert();

            List<String> DuplicateCOBs = DuplicateCOBCase3ForNewlyCreatedDNO(String.valueOf(obligationEntityId));
            List<String> DuplicateCOB = DuplicateCOBCase3AtSystemLevel();

            if (DuplicateCOBs.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 5 \n" + DuplicateCOBs);
            } else if (DuplicateCOB.get(0).contains("[]")) {
                customAssert.assertFalse(false, "There is no duplicate COB as per case 5 at system level\n" + DuplicateCOBs);
            } else {
                customAssert.assertFalse(true, "There are duplicate COB as per case 5 \n" + DuplicateCOB);
            }
            customAssert.assertAll();
    }

    private List<String> getNextCreationAndStartDate(int obligationEntityId){
        List<String> data = new ArrayList<>();
        String query="select next_creation_date,next_start_date,* from dno where id = "+obligationEntityId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting OB entity Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    private List<List<String>>  getGBTask(int obligationEntityId){
        String query="select * from TASK where scheduled_job_id = 1138 and client_id = 1002 and entity_type_id = 12 and status = 4 and entity_id = "+obligationEntityId;
        List<List<String>> results = null;
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
             results = sqlObj.doSelect(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Task data of OB from DB using query [{}]. {}", query, e.getMessage());
        }

        return results;
    }

    private boolean updateCreationDate(int obligationEntityId){
        boolean results = false;
        String query="update dno set next_creation_date =now() where id = "+obligationEntityId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
             results = sqlObj.updateDBEntry(query);


            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while updating OB entity Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return results;
    }

    private int createOB(CustomAssert customAssert, String flow) {
        logger.info("***********************************creating OB*************************************");
        try {

            String createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flow,
                    true);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                int OBEntityId = CreateEntity.getNewEntityId(createResponse);
                logger.info("OB successfully created with ID ->" + OBEntityId);
                return OBEntityId;
            }
        } catch (Exception e) {
            logger.error("OB is not creating");
            customAssert.assertTrue(false, "Exception while OB is creating");
        }
        return  -1;
    }

    private  HashMap<String,String> validateCOBCreation(int obligationEntityId) throws ParseException {
        int childObligationTabId = 60;
        HashMap<String, String> cob = new HashMap<>();
        String payload = "{\"filterMap\":{\"entityTypeId\":13,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
        tabData.hitListRendererTabListData(childObligationTabId,obligationEntityTypeId,obligationEntityId,payload);
        String  COBDataResponse = tabData.getTabListDataJsonStr();


        JSONArray occurrencedate = (JSONArray) JSONUtility.parseJson(COBDataResponse, "$.data[*].[*][?(@.columnName=='due_date')].value");
        JSONArray id = (JSONArray) JSONUtility.parseJson(COBDataResponse, "$.data[*].[*][?(@.columnName=='id')].value");

        if (id.size() != 0) {
            for (int i = 0; i < id.size(); i++) {
                String occuranceDate =  DateUtils.converDateToAnyFormat((String) occurrencedate.get(i),"MMM-dd-yyyy","yyyy-MM-dd");
                cob.put(((String) id.get(i)).split(":;")[1], occuranceDate);
            }

        }
            return  cob;
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

    private void waitForCGBCreation(int maxTime, int obEntityId ) throws InterruptedException, ParseException {
        int time = 0;
        while (time < maxTime && validateCOBCreation(obEntityId).size()==0){
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

    private String getMaxOccuranceDateCOB(HashMap<String,String> map){
        TreeSet<String> valuesSet = new TreeSet<>(map.values());
        return valuesSet.last();
    }

    /**
     * @author: Naveen Kumar Gupta
     * Created on 05 August 2020
     * To check duplidate cob case 1 for Newly created childs
     */
    private static List<String> DuplicateCOBCase1ForNewlyCreatedDNO(String DNOID) {
        String strOutput = new String();
        String query= "SELECT\tid, dnoid, due_date, status_id\n" +
                "FROM child_dno\t\n" +
                "WHERE id IN (\n" +
                "SELECT unnest(deleted_ids) FROM (SELECT \n" +
                "cd.dnoid,min(cd.id),\n" +
                "array_agg(cd.id),\n" +
                "array_remove(array_agg(cd.id), \n" +
                "min(cd.id)) AS deleted_ids,cd.due_date,\n" +
                "count(cd.id) AS num,\n" +
                "array_agg(DISTINCT (cd.status_id)) AS status_arr\n" +
                "\n" +
                "FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id\n" +
                "JOIN work_flow_status wfs ON cd.status_id = wfs.id\n" +
                "WHERE cd.deleted = FALSE\n" +
                "AND D.deleted = FALSE\n" +
                "AND D.status_id = 5\n" +
                "AND cd.dnoid = " + DNOID +
                "GROUP BY cd.dnoid, cd.due_date\n" +
                "HAVING count(cd.id) > 1\n" +
                "AND array_agg(DISTINCT (cd.status_id)) IN\n" +
                "(ARRAY [4], ARRAY [21], ARRAY [4, 21], ARRAY [21, 4])) AS foo)";

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
     * Created on 05 August 2020
     * To check duplidate cob case 1 at system levels
     */
    private static List<String> DuplicateCOBCase2ForNewlyCreatedDNO(String DNOID) {
        String strOutput = new String();
        String query= "SELECT id, dnoid, due_date,status_id\n" +
                "FROM child_dno\n" +
                "WHERE id IN (SELECT unnest(all_ids)\n" +
                "  FROM (SELECT\n" +
                "cd.dnoid,\n" +
                "array_agg(cd.id) AS all_ids,\n" +
                "cd.due_date,\n" +
                "count(cd.id) AS num,\n" +
                "array_agg(DISTINCT (cd.status_id)) AS status_arr\n" +
                "FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id\n" +
                "JOIN work_flow_status wfs ON cd.status_id = wfs.id\n" +
                "WHERE cd.deleted = FALSE\n" +
                "AND D.deleted = FALSE\n" +
                "AND D.status_id = 5\n" +
                "AND cd.dnoid= " + DNOID +"\n" +
                "GROUP BY cd.dnoid, cd.due_date\n" +
                "HAVING count(cd.id) > 1 \n" +
                "AND(array_agg(DISTINCT (cd.status_id)) && ARRAY [21, 4] \n" +
                "AND array_agg(DISTINCT (cd.status_id)) NOT IN (ARRAY [4], ARRAY [21], ARRAY [4, 21], ARRAY [21, 4])))AS foo)\n" +
                "AND status_id IN (4, 21)\n" +
                "ORDER BY dnoid, due_date\n";

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
     * Created on 05 August 2020
     * To check duplidate cob case 2 for Newly created childs
     */
    private static List<String> DuplicateCOBCase3ForNewlyCreatedDNO(String DNOID) {
        String strOutput = new String();
        String query= "SELECT\tid, dnoid,due_date, status_id\n" +
                "FROM child_dno\n" +
                "WHERE id IN (SELECT unnest(all_ids)\n" +
                "FROM (SELECT\n" +
                "cd.dnoid,\n" +
                "array_agg(cd.id) AS all_ids,\n" +
                "cd.due_date,\n" +
                "count(cd.id) AS num,\n" +
                "array_agg(DISTINCT (cd.status_id)) AS status_arr\n" +
                "FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id\n" +
                "JOIN work_flow_status wfs ON cd.status_id = wfs.id\n" +
                "WHERE cd.deleted = FALSE\n" +
                "AND D.deleted = FALSE\n" +
                "AND D.status_id = 5\n" +
                "AND cd.dnoid= " + DNOID + "\n"+
                "GROUP BY cd.dnoid, cd.due_date\n" +
                "HAVING count(cd.id) > 1\n" +
                "AND (array_agg(DISTINCT (cd.status_id)) && ARRAY [21, 4]) = FALSE) AS foo)\n" +
                "ORDER BY dnoid, due_date\n";

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
     * Created on 05 August 2020
     * To check duplidate cob case 2 at system level
     */
    private static List<String> DuplicateCOBCase1AtSystemLevel() {
        String strOutput = new String();
        String query= "SELECT id, dnoid, due_date, status_id \n" +
                "                FROM child_dno \n" +
                "                WHERE id IN ( \n" +
                "                SELECT unnest(deleted_ids) FROM (SELECT  \n" +
                "                cd.dnoid,min(cd.id), \n" +
                "                array_agg(cd.id), \n" +
                "                array_remove(array_agg(cd.id),  \n" +
                "                min(cd.id)) AS deleted_ids,cd.due_date, \n" +
                "                count(cd.id) AS num, \n" +
                "                array_agg(DISTINCT (cd.status_id)) AS status_arr \n" +
                "                FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id \n" +
                "                JOIN work_flow_status wfs ON cd.status_id = wfs.id \n" +
                "                WHERE cd.deleted = FALSE \n" +
                "                AND D.deleted = FALSE \n" +
                "                AND D.status_id = 5\n" +
                "                GROUP BY cd.dnoid, cd.due_date \n" +
                "                HAVING count(cd.id) > 1 \n" +
                "                AND array_agg(DISTINCT (cd.status_id)) IN \n" +
                "                (ARRAY [4], ARRAY [21], ARRAY [4, 21], ARRAY [21, 4])) AS foo)";

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
     * Created on 05 August 2020
     * To check duplidate cob case 3 for Newly created childs
     */
    private static List<String> DuplicateCOBCase2AtSystemLevel() {
        String strOutput = new String();
        String query= "SELECT id, dnoid, due_date,status_id\n" +
                "FROM child_dno\n" +
                "WHERE id IN (SELECT unnest(all_ids)\n" +
                "  FROM (SELECT\n" +
                "cd.dnoid,\n" +
                "array_agg(cd.id) AS all_ids,\n" +
                "cd.due_date,\n" +
                "count(cd.id) AS num,\n" +
                "array_agg(DISTINCT (cd.status_id)) AS status_arr\n" +
                "FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id\n" +
                "JOIN work_flow_status wfs ON cd.status_id = wfs.id\n" +
                "WHERE cd.deleted = FALSE\n" +
                "AND D.deleted = FALSE\n" +
                "AND D.status_id = 5\n" +
                "GROUP BY cd.dnoid, cd.due_date\n" +
                "HAVING count(cd.id) > 1 \n" +
                "AND(array_agg(DISTINCT (cd.status_id)) && ARRAY [21, 4] \n" +
                "AND array_agg(DISTINCT (cd.status_id)) NOT IN (ARRAY [4], ARRAY [21], ARRAY [4, 21], ARRAY [21, 4])))AS foo)\n" +
                "AND status_id IN (4, 21)\n" +
                "ORDER BY dnoid, due_date\n";

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
     * To check duplidate cob case 3 at system levels
     */
    private static List<String> DuplicateCOBCase3AtSystemLevel() {
        String strOutput = new String();
        String query= "SELECT\tid, dnoid,due_date, status_id\n" +
                "FROM child_dno\n" +
                "WHERE id IN (SELECT unnest(all_ids)\n" +
                "FROM (SELECT\n" +
                "cd.dnoid,\n" +
                "array_agg(cd.id) AS all_ids,\n" +
                "cd.due_date,\n" +
                "count(cd.id) AS num,\n" +
                "array_agg(DISTINCT (cd.status_id)) AS status_arr\n" +
                "FROM child_dno cd LEFT JOIN dno D ON cd.dnoid = D.id\n" +
                "JOIN work_flow_status wfs ON cd.status_id = wfs.id\n" +
                "WHERE cd.deleted = FALSE\n" +
                "AND D.deleted = FALSE\n" +
                "AND D.status_id = 5\n" +
                "GROUP BY cd.dnoid, cd.due_date\n" +
                "HAVING count(cd.id) > 1\n" +
                "AND (array_agg(DISTINCT (cd.status_id)) && ARRAY [21, 4]) = FALSE) AS foo)\n" +
                "ORDER BY dnoid, due_date\n";

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
