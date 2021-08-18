package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkflowLayoutDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(WorkflowLayoutDbHelper.class);

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

    public static List<String> getLayoutDataFromDB(String selectedColumns, int layoutId) {
        String query = "select " + selectedColumns + " from work_flow_layout where id = " + layoutId;
        return getOneRowDataFromQuery(query);
    }

    public static List<String> getLatestLayoutDataFromDB(String selectedColumns) {
        int latestLayoutId = getLatestLayoutIdFromDB();
        return getLayoutDataFromDB(selectedColumns, latestLayoutId);
    }

    public static int getLatestLayoutIdFromDB() {
        String query = "select id from work_flow_layout order by id desc limit 1";
        List<String> data = getOneRowDataFromQuery(query);

        if (data != null && !data.isEmpty()) {
            return Integer.parseInt(data.get(0));
        }

        return -1;
    }

    public static Boolean isLayoutNamePresentInDb(String layoutName) {
        String query = "select * from work_flow_layout where name = '" + layoutName + "'";
        List<String> data = getOneRowDataFromQuery(query);

        return !(data == null || data.isEmpty());
    }

    public static void deleteLayoutDataInDb(int layoutId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "delete from work_flow_layout where id = " + layoutId;

            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Layout Data from DB having Id " + layoutId + ". " + e.getMessage());
        }
    }
}