package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AuditLogsDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(AuditLogsDbHelper.class);

    private static List<List<String>> getAllOtherAuditLogsDataFromQuery(String query) {
        List<List<String>> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            data = sqlObj.doSelect(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public static List<List<String>> getAllOtherAuditLogsForEntityIdAndEntityTypeId(String selectedColumns, int entityTypeId, int entityId, int clientId,
                                                                                    String orderByColumnName, String orderDirection) {
        String query = "select " + selectedColumns + " from other_audit_log where client_id = " + clientId + " and entity_type_id = " +
                entityTypeId + " and entity_id = " + entityId + " order by " + orderByColumnName + " " + orderDirection;
        return getAllOtherAuditLogsDataFromQuery(query);
    }

    public static List<List<String>> getAllOtherAuditLogsForEntityIdAndEntityTypeId(int entityTypeId, int entityId, int clientId) {
        return getAllOtherAuditLogsForEntityIdAndEntityTypeId("*", entityTypeId, entityId, clientId, "id", "desc");
    }

    public static int getLatestAuditLogIdForClientAndEntityTypeIdFromDb(int clientId, int entityTypeId) {
        String query = "select id from other_audit_log where client_id = " + clientId + " and entity_type_id = " + entityTypeId + " order by id desc limit 1";

        List<List<String>> results = getAllOtherAuditLogsDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0).get(0));
    }

    public static List<String> getOneAuditLogDataFromId(String selectedColumns, int logId) {
        String query = "select " + selectedColumns + " from other_audit_log where id = " + logId;
        return getAllOtherAuditLogsDataFromQuery(query).get(0);
    }
}