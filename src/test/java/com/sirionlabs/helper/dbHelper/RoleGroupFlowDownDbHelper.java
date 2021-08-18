package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RoleGroupFlowDownDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(RoleGroupFlowDownDbHelper.class);

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

    public static List<String> getRoleGroupFlowDownDataFromDB(String selectedColumns, String parentEntityTypeId, String childEntityTypeId, String roleGroupId, String clientId) {
        String query = "select " + selectedColumns + " from rolegroup_flowdown where parent_entity_type = " + parentEntityTypeId + " and child_entity_type = " +
                childEntityTypeId + " and role_group_id = " + roleGroupId + " and client_id = " + clientId;
        return getOneRowDataFromQuery(query);
    }

    public static int getLatestRoleGroupFlowDownIdFromDB() {
        String query = "select id from rolegroup_flowdown order by id desc limit 1";
        List<String> data = getOneRowDataFromQuery(query);

        if (data != null && !data.isEmpty()) {
            return Integer.parseInt(data.get(0));
        }

        return -1;
    }

    public static boolean isRoleGroupFlowDownPresentInDb(String parentEntityTypeId, String childEntityTypeId, String roleGroupId, String clientId) {
        String query = "select * from rolegroup_flowdown where parent_entity_type = " + parentEntityTypeId + " and child_entity_type = " + childEntityTypeId +
                " and role_group_id = " + roleGroupId + " and client_id = " + clientId;
        List<String> data = getOneRowDataFromQuery(query);

        return !(data == null || data.isEmpty());
    }

    public static void deleteRoleGroupFlowDownDataInDb(int roleGroupFlowDownId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select child_entity_type, role_group_id from rolegroup_flowdown where id = " + roleGroupFlowDownId;
            List<String> data = getOneRowDataFromQuery(query);

            String roleGroupId = data.get(1);
            String childEntityTypeId = data.get(0);

            query = "delete from rolegroup_flowdown where id = " + roleGroupFlowDownId;
            sqlObj.deleteDBEntry(query);

            query = "select short_name from role_group where flowdown_rolegroup = " + roleGroupId + " and entity_type_id = " + childEntityTypeId;
            data = getOneRowDataFromQuery(query);

            String shortName = data.get(0);
            query = "delete from role_group where flowdown_rolegroup = " + roleGroupId + " and entity_type_id = " + childEntityTypeId;
            sqlObj.deleteDBEntry(query);

            query = "select id from entity_field where api_name= '" + shortName + "'";
            data = getOneRowDataFromQuery(query);

            String fieldId = data.get(0);
            query = "delete from link_fields_groups where field_id = " + fieldId;
            sqlObj.deleteDBEntry(query);

            query = "delete from request_field_mapping where field_id = " + fieldId;
            sqlObj.deleteDBEntry(query);

            query = "delete from entity_field where id = " + fieldId;
            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Rule Group Flow Down Data from DB having Id " + roleGroupFlowDownId + ". " + e.getMessage());
        }
    }

    public static List<List<String>> getAllChildEntityTypeIdsForParentEntityTypeIdAndRoleGroupId(int parentEntityTypeId, int roleGroupId, int clientId) {
        String query = "select distinct child_entity_type from rolegroup_flowdown where parent_entity_type = " + parentEntityTypeId + " and role_group_id = " + roleGroupId +
                " and deleted = false and client_id = " + clientId;

        try {
            return new PostgreSQLJDBC().doSelect(query);
        } catch (Exception e) {
            logger.error("Exception while Getting All Child Entity Type Ids for Parent Entity Type Id " + parentEntityTypeId + " and Role Group Id " + roleGroupId +
                    ". " + e.getMessage());
        }

        return null;
    }
}