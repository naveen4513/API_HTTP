package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SpecialEntityFieldPropertyDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(SpecialEntityFieldPropertyDbHelper.class);

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

    public static List<String> getSpecialEntityFieldDataFromDB(String selectedColumns, int fieldId) {
        String query = "select " + selectedColumns + " from special_entity_field_property_mapping where id = " + fieldId;
        return getOneRowDataFromQuery(query);
    }

    public static List<String> getLatestFieldIdDataFromDB(String selectedColumns) {
        int latestFieldId = getLatestFieldIdFromDB();
        return getSpecialEntityFieldDataFromDB(selectedColumns, latestFieldId);
    }

    public static int getLatestFieldIdFromDB() {
        String query = "select id from special_entity_field_property_mapping order by id desc limit 1";
        List<String> data = getOneRowDataFromQuery(query);

        if (data != null && !data.isEmpty()) {
            return Integer.parseInt(data.get(0));
        }

        return -1;
    }

    public static void deleteFieldDataInDb(int fieldId) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "delete from special_entity_field_property_mapping where id = " + fieldId;

            sqlObj.deleteDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Deleting Special Entity Field Data from DB having Id " + fieldId + ". " + e.getMessage());
        }
    }
}