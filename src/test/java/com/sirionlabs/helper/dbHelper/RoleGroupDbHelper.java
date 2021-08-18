package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoleGroupDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(RoleGroupDbHelper.class);

    private static List<String> getRoleGroupDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Role Group Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public static List<String> getRoleGroupDataFromId(int rgId) {
        return getRoleGroupDataFromId("*", rgId);
    }

    public static List<String> getRoleGroupDataFromId(String selectedColumns, int rgId) {
        String query = "select " + selectedColumns + " from role_group where id = " + rgId;
        return getRoleGroupDataFromQuery(query);
    }

    public static int getLatestRoleGroupId() {
        String query = "select id from role_group order by id desc limit 1";

        List<String> results = getRoleGroupDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

    public static void deleteRoleGroup(int rgId) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "delete from link_group_role where group_id =  " + rgId;
        sqlObj.deleteDBEntry(query);

        query = "delete from role_group where id = " + rgId;
        sqlObj.deleteDBEntry(query);

        sqlObj.closeConnection();
    }

    public static int getFlowDownRoleGroupId(String roleGroup, String entity) {
        String query = "select id from role_group where name ilike '%"+roleGroup+" "+entity+"%'";

        List<String> results = getRoleGroupDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

}