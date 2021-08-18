package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(EntityDbHelper.class);

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

    public static String getEntityAliasName(int entityId) {
        String query = "select alias from entity_field where id= " + entityId;
        List<String> data = getOneRowDataFromQuery(query);

        return (data != null) ? data.get(0) : null;
    }

    public static String getEntityTabLabel(int tabId) {
        String query = "select label from entity_field_groups where id = " + tabId;
        List<String> data = getOneRowDataFromQuery(query);

        return (data != null) ? data.get(0) : null;
    }

    public static Boolean toggleWFCloseFlag(int entityId, String flag) {

        String query = "update action_item_mgmt set workflow_close = "+flag+" where id = "+entityId+"";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        Boolean result = sqlObj.updateDBEntry(query);
        return result;
    }
}