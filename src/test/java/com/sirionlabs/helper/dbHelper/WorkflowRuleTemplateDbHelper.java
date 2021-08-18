package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkflowRuleTemplateDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowRuleTemplateDbHelper.class);

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

    public static List<String> getRuleTemplateDataFromDB(String selectedColumns, int ruleTemplateId) {
        String query = "select " + selectedColumns + " from workflow_rule_template where id = " + ruleTemplateId;
        return getOneRowDataFromQuery(query);
    }

    public static List<String> getLatestRuleTemplateDataFromDB(String selectedColumns) {
        int latestRuleTemplateId = getLatestRuleTemplateIdFromDB();
        return getRuleTemplateDataFromDB(selectedColumns, latestRuleTemplateId);
    }

    public static int getLatestRuleTemplateIdFromDB() {
        String query = "select id from workflow_rule_template order by id desc limit 1";
        List<String> data = getOneRowDataFromQuery(query);

        if (data != null && !data.isEmpty()) {
            return Integer.parseInt(data.get(0));
        }

        return -1;
    }

    public static Boolean isRuleTemplateNamePresentInDb(String ruleTemplateName) {
        String query = "select * from workflow_rule_template where rule_template_name = '" + ruleTemplateName + "'";
        List<String> data = getOneRowDataFromQuery(query);

        return !(data == null || data.isEmpty());
    }

    public static void deleteRuleTemplateDataInDb(int ruleTemplateId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "delete from workflow_rule_template where id = " + ruleTemplateId;

            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Rule Template Data from DB having Id " + ruleTemplateId + ". " + e.getMessage());
        }
    }
}