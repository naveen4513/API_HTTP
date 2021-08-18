package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkflowButtonsDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowButtonsDbHelper.class);

    private static List<String> getOneRowDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }

        return data;
    }

    public static List<String> getButtonDataFromDB(int buttonId) {
        String query = "select * from workflow_button where id = " + buttonId;
        return getOneRowDataFromQuery(query);
    }

    public static List<String> getLatestButtonCreateDataFromDB() {
        int latestButtonId = getLatestButtonIdFromDB();
        return getButtonDataFromDB(latestButtonId);
    }

    public static int getLatestButtonIdFromDB() {
        String query = "select id from workflow_button order by id desc limit 1";
        List<String> data = getOneRowDataFromQuery(query);

        if (data != null && !data.isEmpty()) {
            return Integer.parseInt(data.get(0));
        }

        return -1;
    }

    public static Boolean isButtonNamePresentInDb(String buttonName) {
        String query = "select * from workflow_button where name = '" + buttonName + "'";
        List<String> data = getOneRowDataFromQuery(query);

        return !(data == null || data.isEmpty());
    }

    public static void deleteButtonDataInDb(int buttonId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "delete from workflow_button where id = " + buttonId;

            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Button Data from DB having Id " + buttonId + ". " + e.getMessage());
        }
    }

    public boolean updateWorkflowDateForCustomFieldHelpWithFlow(String flowToTest){

        String contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        String contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");

        String relationId = ParseConfigFile.getValueFromConfigFile(contractConfigFilePath,contractConfigFileName,flowToTest,"sourceid");

        return updateWorkflowDateForCustomFieldHelpWithRelation(relationId);
    }

    public boolean updateWorkflowDateForCustomFieldHelpWithRelation(String relationId){
        boolean jobDOne = false;

        String query = "update work_flow set date_created = now() where relation_id="+relationId+" and client_id = "+ new AdminHelper().getClientId();

        logger.info("Workflow update query formed is [{}]",query);
        try{
            jobDOne = new PostgreSQLJDBC().updateDBEntry(query);
        }
        catch (Exception e){
            logger.error("Exception occurred in updating workflow in DB");
            //customAssert.assertTrue(false,"Exception occurred in updating workflow in DB");
        }
        return jobDOne;
    }
}